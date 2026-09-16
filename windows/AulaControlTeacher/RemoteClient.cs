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
    long LocationTs
);

public sealed class RemoteClient : IDisposable
{
    readonly string key,tag,url;
    readonly HttpClient http = new(){Timeout=TimeSpan.FromSeconds(8)};
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
                e.TryGetProperty("deviceName",out var dn)?dn.GetString()??id:id,
                e.TryGetProperty("user",out var u)?u.GetString()??"":"",
                e.TryGetProperty("course",out var co)?co.GetString()??"":"",
                e.TryGetProperty("role",out var ro)?ro.GetString()??"":"",
                e.TryGetProperty("model",out var mo)?mo.GetString()??"":"",
                e.TryGetProperty("android",out var an)?an.GetString()??"":"",
                e.TryGetProperty("battery",out var ba)&&ba.TryGetInt32(out var b)?b:-1,
                e.TryGetProperty("managed",out var ma)&&ma.GetBoolean(),
                e.TryGetProperty("screen",out var sc)&&sc.GetBoolean(),
                e.TryGetProperty("lost",out var ls)&&ls.GetBoolean(),
                e.TryGetProperty("lat",out var la)?la.GetString()??"":"",
                e.TryGetProperty("lon",out var lo)?lo.GetString()??"":"",
                e.TryGetProperty("accuracy",out var ac)?ac.GetString()??"":"",
                e.TryGetProperty("locationTs",out var lt)&&lt.TryGetInt64(out var ltv)?ltv:0));
        }catch{}
        return result;
    }

    public async Task<(bool,string)> SendCommandAsync(string deviceId,string action,string value="")
    {
        if(!Enabled)return(false,"Relay no configurado");
        long ts=DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();string enc=AcCrypto.SealText(key,value);string commandSig=AcCrypto.Hmac(key,$"{action}\n{ts}\n{enc}");string reqSig=AcCrypto.Hmac(key,$"command\n{tag}\n{deviceId}\n{action}\n{ts}\n{enc}\n{commandSig}");
        using var doc=await Post(new{op="command",schoolTag=tag,deviceId,action,ts,enc,sig=commandSig,reqSig});if(doc==null)return(false,"Sin respuesta del relay");
        bool ok=doc.RootElement.TryGetProperty("ok",out var o)&&o.GetBoolean();return(ok,ok?"OK":doc.RootElement.TryGetProperty("error",out var e)?e.GetString()??"Error":"Error");
    }

    public void Dispose()=>http.Dispose();
}
