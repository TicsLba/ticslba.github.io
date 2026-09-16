using System.Security.Cryptography;using System.Net;using System.Net.Sockets;using System.Text;using System.Text.Json;using System.IO;
namespace AulaControlTeacher;
public sealed class Discovery:IDisposable{
 readonly string key,tag;UdpClient? udp;CancellationTokenSource? cts;public event Action<Device>? Seen;public Discovery(string k){key=k;tag=AcCrypto.Sha(k)[..12];}
 public void Start(){cts=new();udp=new UdpClient();udp.Client.SetSocketOption(SocketOptionLevel.Socket,SocketOptionName.ReuseAddress,true);udp.Client.Bind(new IPEndPoint(IPAddress.Any,45888));_=Loop(cts.Token);} 
 async Task Loop(CancellationToken t){while(!t.IsCancellationRequested)try{
  var r=await udp!.ReceiveAsync(t);using var doc=JsonDocument.Parse(Encoding.UTF8.GetString(r.Buffer));var x=doc.RootElement;
  if(x.GetProperty("type").GetString()!="AULACONTROL_BEACON"||x.GetProperty("schoolTag").GetString()!=tag)continue;
  int ver=x.TryGetProperty("version",out var ve)?ve.GetInt32():3;
  string id=x.GetProperty("deviceId").GetString()??"",dn=x.GetProperty("deviceName").GetString()??"",ip=x.GetProperty("ip").GetString()??r.RemoteEndPoint.Address.ToString();if(ip=="0.0.0.0")ip=r.RemoteEndPoint.Address.ToString();
  int cp=x.GetProperty("commandPort").GetInt32(),sp=x.GetProperty("screenPort").GetInt32(),bat=x.GetProperty("battery").GetInt32();bool sh=x.GetProperty("screenSharing").GetBoolean();
  bool managed=ver>=4&&x.TryGetProperty("managed",out var ma)&&ma.GetBoolean();long frame=ver>=4&&x.TryGetProperty("frameAge",out var fa)?fa.GetInt64():-1;long ts=x.GetProperty("ts").GetInt64();string sig=x.GetProperty("sig").GetString()??"";
  if(Math.Abs(DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()-ts)>30000)continue;
  string u="",co="",role="",wireU="",wireC="",wireR="";
  if(ver>=5){wireU=x.GetProperty("userEnc").GetString()??"";wireC=x.GetProperty("courseEnc").GetString()??"";if(ver>=6)wireR=x.GetProperty("roleEnc").GetString()??"";}else{u=x.GetProperty("userName").GetString()??"";co=x.GetProperty("course").GetString()??"";wireU=u;wireC=co;}
  string lat="",lon="",acc="";long locTs=0;bool lost=false;
  if(ver>=8){lat=x.TryGetProperty("lat",out var la)?la.GetString()??"":"";lon=x.TryGetProperty("lon",out var lo)?lo.GetString()??"":"";acc=x.TryGetProperty("accuracy",out var ac)?ac.GetString()??"":"";locTs=x.TryGetProperty("locationTs",out var lt)?lt.GetInt64():0;lost=x.TryGetProperty("lost",out var ls)&&ls.GetBoolean();}
  string d;
  if(ver>=8)d=$"{id}\n{dn}\n{wireU}\n{wireC}\n{wireR}\n{ip}\n{cp}\n{sp}\n{sh.ToString().ToLowerInvariant()}\n{managed.ToString().ToLowerInvariant()}\n{frame}\n{bat}\n{lat}\n{lon}\n{acc}\n{locTs}\n{lost.ToString().ToLowerInvariant()}\n{ts}";
  else if(ver>=6)d=$"{id}\n{dn}\n{wireU}\n{wireC}\n{wireR}\n{ip}\n{cp}\n{sp}\n{sh.ToString().ToLowerInvariant()}\n{managed.ToString().ToLowerInvariant()}\n{frame}\n{bat}\n{ts}";
  else if(ver>=4)d=$"{id}\n{dn}\n{wireU}\n{wireC}\n{ip}\n{cp}\n{sp}\n{sh.ToString().ToLowerInvariant()}\n{managed.ToString().ToLowerInvariant()}\n{frame}\n{bat}\n{ts}";
  else d=$"{id}\n{dn}\n{wireU}\n{wireC}\n{ip}\n{cp}\n{sp}\n{sh.ToString().ToLowerInvariant()}\n{bat}\n{ts}";
  if(!CryptographicOperations.FixedTimeEquals(Convert.FromHexString(AcCrypto.Hmac(key,d)),Convert.FromHexString(sig)))continue;
  if(ver>=5){u=AcCrypto.OpenText(key,wireU);co=AcCrypto.OpenText(key,wireC);}if(ver>=6&&!string.IsNullOrWhiteSpace(wireR))role=AcCrypto.OpenText(key,wireR);
  if(ver>=8)LiveRecovery.Update(id,lat,lon,acc,locTs,lost);
  Seen?.Invoke(new Device{Id=id,DeviceName=dn,User=u,Course=co,Role=role,Model=x.GetProperty("model").GetString()??"",Android=x.GetProperty("android").GetString()??"",Ip=ip,CommandPort=cp,ScreenPort=sp,Battery=bat,Screen=sh,Managed=managed,FrameAgeMs=frame,Seen=DateTime.Now});
 }catch(OperationCanceledException){break;}catch{try{await Task.Delay(150,t);}catch{}}}
 public void Dispose(){try{cts?.Cancel();udp?.Dispose();}catch{}}
}
public class Commands(string key){public async Task<(bool,string)> Send(Device d,string a,string v=""){try{long ts=DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();string enc=AcCrypto.SealText(key,v);string sig=AcCrypto.Hmac(key,$"{a}\n{ts}\n{enc}");string q=JsonSerializer.Serialize(new{version=2,action=a,ts,enc,sig})+"\n";using var tcp=new TcpClient();using var c=new CancellationTokenSource(5000);await tcp.ConnectAsync(d.Ip,d.CommandPort,c.Token);await tcp.GetStream().WriteAsync(Encoding.UTF8.GetBytes(q),c.Token);using var sr=new StreamReader(tcp.GetStream(),Encoding.UTF8);string? line=await sr.ReadLineAsync(c.Token);if(string.IsNullOrWhiteSpace(line))return(false,"Sin respuesta");using var doc=JsonDocument.Parse(line);bool ok=doc.RootElement.GetProperty("ok").GetBoolean();return(ok,ok?"OK":doc.RootElement.TryGetProperty("error",out var e)?e.GetString()??"Error":"Error");}catch(Exception e){return(false,e.Message);}}}
