using System.Net.Http;
using System.Text;
using System.Text.Json;

namespace AulaControlTeacher;

public sealed record RemoteTelemetry(
    string DeviceId,
    DateTime Seen,
    string DeviceName,
    string User,
    string Course,
    string Role,
    string Model,
    string Android,
    int Battery,
    bool Managed,
    bool Screen,
    bool Lost,
    string Lat,
    string Lon,
    string Accuracy,
    long LocationTs,
    string AppVersion="",
    bool Charging=false,
    string Wifi="",
    long StorageFreeMb=-1,
    long StorageTotalMb=-1,
    string State="",
    double Fps=2,
    int IdleStudentMin=10,
    int IdleTeacherMin=30
);

public sealed class RemoteClient : IDisposable
{
    readonly string key,tag,url;
    readonly HttpClient http = new(){Timeout=TimeSpan.FromSeconds(9)};
    public RemoteClient(string k,string endpoint){key=k;tag=AcCrypto.Sha(k)[..12];url=endpoint.Trim();}
    public bool Enabled => Uri.TryCreate(url,UriKind.Absolute,out var u) && u.Scheme.Equals("https",StringComparison.OrdinalIgnoreCase);

    async Task<JsonDocument?> Post(object body)
    {
        if(!Enabled)return null;
        try{
            var json=JsonSerializer.Serialize(body);using var c=new StringContent(json,Encoding.UTF8,"application/json");var r=await http.PostAsync(url,c);if(!r.IsSuccessStatusCode)return null;return JsonDocument.Parse(await r.Content.ReadAsStringAsync());
        }catch{return null;}
    }

    public async Task<List<RemoteTelemetry>> FetchDevicesAsync()
    {
        var result=new List<RemoteTelemetry>();if(!Enabled)return result;
        long ts=DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();string sig=AcCrypto.Hmac(key,$"devices\n{tag}\n{ts}");
        using var doc=await Post(new{op="devices",schoolTag=tag,ts,sig});if(doc==null||!doc.RootElement.TryGetProperty("ok",out var ok)||!ok.GetBoolean())return result;
        if(!doc.RootElement.TryGetProperty("devices",out var ds)||ds.ValueKind!=JsonValueKind.Array)return result;
        foreach(var x in ds.EnumerateArray())try{
            string id=x.GetProperty("deviceId").GetString()??"",payload=x.GetProperty("payload").GetString()??"";long seen=x.GetProperty("ts").GetInt64();
            string plain=AcCrypto.OpenText(key,payload);using var p=JsonDocument.Parse(plain);var e=p.RootElement;
            result.Add(new RemoteTelemetry(id,DateTimeOffset.FromUnixTimeMilliseconds(seen).LocalDateTime,
                S(e,"deviceName",id),S(e,"user"),S(e,"course"),S(e,"role"),S(e,"model"),S(e,"android"),
                I(e,"battery",-1),B(e,"managed"),B(e,"screen"),B(e,"lost"),S(e,"lat"),S(e,"lon"),S(e,"accuracy"),L(e,"locationTs"),
                S(e,"appVersion"),B(e,"charging"),S(e,"wifi"),L(e,"storageFreeMb",-1),L(e,"storageTotalMb",-1),S(e,"state"),
                D(e,"fps",2),I(e,"idleStudentMin",10),I(e,"idleTeacherMin",30)));
        }catch{}
        return result;
    }

    public async Task<(bool,string)> SendCommandAsync(string deviceId,string action,string value="")
    {
        if(!Enabled)return(false,"Relay no configurado");
        long ts=DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();string enc=AcCrypto.SealText(key,value);string commandSig=AcCrypto.Hmac(key,$"{action}\n{ts}\n{enc}");string reqSig=AcCrypto.Hmac(key,$"command\n{tag}\n{deviceId}\n{action}\n{ts}\n{enc}\n{commandSig}");
        using var doc=await Post(new{op="command",schoolTag=tag,deviceId,action,ts,enc,sig=commandSig,reqSig});if(doc==null)return(false,"Sin respuesta del relay");
        bool ok=doc.RootElement.TryGetProperty("ok",out var o)&&o.GetBoolean();
        string id=doc.RootElement.TryGetProperty("commandId",out var ci)?ci.GetString()??"":"";
        return(ok,ok?(string.IsNullOrWhiteSpace(id)?"OK":"OK · "+id):(doc.RootElement.TryGetProperty("error",out var e)?e.GetString()??"Error":"Error"));
    }

    public async Task<byte[]?> FetchScreenAsync(string deviceId)
    {
        if(!Enabled)return null;
        long ts=DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        string sig=AcCrypto.Hmac(key,$"screenGet\n{tag}\n{deviceId}\n{ts}");
        using var doc=await Post(new{op="screenGet",schoolTag=tag,deviceId,ts,sig});
        if(doc==null||!doc.RootElement.TryGetProperty("ok",out var ok)||!ok.GetBoolean())return null;
        string payload=doc.RootElement.TryGetProperty("payload",out var p)?p.GetString()??"":"";
        if(string.IsNullOrWhiteSpace(payload))return null;
        try{return AcCrypto.Open(key,Convert.FromBase64String(payload));}catch{return null;}
    }

    public async Task<(bool,string)> CancelCommandAsync(string commandId)
    {
        if(!Enabled)return(false,"Relay no configurado");
        long ts=DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        string sig=AcCrypto.Hmac(key,$"cancel\n{tag}\n{commandId}\n{ts}");
        using var doc=await Post(new{op="cancel",schoolTag=tag,commandId,ts,sig});
        bool ok=doc!=null&&doc.RootElement.TryGetProperty("ok",out var o)&&o.GetBoolean();
        return(ok,ok?"Cancelado":"No se pudo cancelar");
    }

    static string S(JsonElement e,string n,string d="")=>e.TryGetProperty(n,out var x)&&x.ValueKind==JsonValueKind.String?x.GetString()??d:d;
    static bool B(JsonElement e,string n)=>e.TryGetProperty(n,out var x)&&x.ValueKind is JsonValueKind.True or JsonValueKind.False&&x.GetBoolean();
    static int I(JsonElement e,string n,int d=0)=>e.TryGetProperty(n,out var x)&&x.TryGetInt32(out var v)?v:d;
    static long L(JsonElement e,string n,long d=0)=>e.TryGetProperty(n,out var x)&&x.TryGetInt64(out var v)?v:d;
    static double D(JsonElement e,string n,double d=0)=>e.TryGetProperty(n,out var x)&&x.TryGetDouble(out var v)?v:d;

    public void Dispose()=>http.Dispose();
}
