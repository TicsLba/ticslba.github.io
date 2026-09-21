using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using System.Text.Json.Nodes;

var builder=WebApplication.CreateBuilder(args);
builder.Services.AddSingleton<RelayStore>();
builder.Services.AddSingleton<RelaySecurity>();
var app=builder.Build();

app.MapGet("/",()=>Results.Json(new{ok=true,service="Aula Móvil Relay",version="11.0.0",time=DateTimeOffset.UtcNow}));
app.MapGet("/health",()=>Results.Json(new{ok=true,time=DateTimeOffset.UtcNow}));

app.MapPost("/relay",async(HttpRequest req,RelayStore store,RelaySecurity sec)=>{
    JsonObject? b;
    try{b=await JsonSerializer.DeserializeAsync<JsonObject>(req.Body);}catch{return Results.Json(new{ok=false,error="invalid-json"},statusCode:400);}
    if(b is null)return Results.Json(new{ok=false,error="invalid-json"},statusCode:400);
    var tag=S(b,"schoolTag"); if(!sec.ValidTag(tag))return Results.Json(new{ok=false,error="school-tag"},statusCode:403);
    var op=S(b,"op");
    try{
        return op switch{
            "telemetry"=>Telemetry(b,store,sec),
            "devices"=>Devices(b,store,sec),
            "command"=>Command(b,store,sec),
            "ack"=>Ack(b,store,sec),
            "cancel"=>Cancel(b,store,sec),
            "commands"=>Commands(b,store,sec),
            "screen"=>ScreenPut(b,store,sec),
            "screenGet"=>ScreenGet(b,store,sec),
            "event"=>EventPut(b,store,sec),
            "history"=>History(b,store,sec),
            _=>Results.Json(new{ok=false,error="unknown-op"},statusCode:400)
        };
    }catch(Exception ex){
        return Results.Json(new{ok=false,error="server",detail=ex.Message},statusCode:500);
    }
});

app.MapPost("/api/file",async(HttpRequest req,RelayStore store,RelaySecurity sec)=>{
    var tag=req.Headers["X-Aula-Tag"].ToString();var sig=req.Headers["X-Signature"].ToString();var sha=req.Headers["X-SHA256"].ToString().ToLowerInvariant();
    if(!long.TryParse(req.Headers["X-Timestamp"],out var ts)||!Fresh(ts,120_000)||!sec.ValidTag(tag))return Results.StatusCode(403);
    long len=req.ContentLength??-1;if(len<0||len>250L*1024*1024||sha.Length!=64)return Results.BadRequest(new{ok=false,error="file-size"});
    if(!sec.Eq(sig,sec.Hmac($"fileUpload\n{tag}\n{ts}\n{sha}\n{len}")))return Results.StatusCode(403);
    using var ms=new MemoryStream();await req.Body.CopyToAsync(ms);var bytes=ms.ToArray();
    var got=Convert.ToHexString(SHA256.HashData(bytes)).ToLowerInvariant();if(got!=sha)return Results.BadRequest(new{ok=false,error="sha256"});
    var name=req.Headers["X-File-Name"].ToString();var token=store.SaveFile(bytes,name,sha);
    return Results.Json(new{ok=true,token,path="/files/"+token,sha256=sha,size=bytes.Length});
});
app.MapGet("/files/{token}",(string token,RelayStore store)=>{
    var f=store.GetFile(token);return f is null?Results.NotFound():Results.File(f.Value.Bytes,"application/octet-stream",f.Value.Name,enableRangeProcessing:true);
});

app.Run();

static IResult Telemetry(JsonObject b,RelayStore store,RelaySecurity sec){
    var id=S(b,"deviceId");var payload=S(b,"payload");var ts=L(b,"ts");var sig=S(b,"sig");var tag=S(b,"schoolTag");
    if(string.IsNullOrWhiteSpace(id)||string.IsNullOrWhiteSpace(payload)||!Fresh(ts,120_000))return Results.Json(new{ok=false,error="bad-telemetry"},statusCode:400);
    if(!sec.Eq(sig,sec.Hmac($"telemetry\n{tag}\n{id}\n{ts}\n{payload}")))return Results.Json(new{ok=false,error="auth"},statusCode:403);
    store.PutLatest(id,ts,payload);store.AppendTelemetry(id,ts,payload);store.CleanupIfNeeded();
    var commands=store.Pending(id).Select(x=>new{id=x.Id,action=x.Action,ts=x.Ts,enc=x.Enc,sig=x.Sig}).ToArray();
    return Results.Json(new{ok=true,commands});
}
static IResult Devices(JsonObject b,RelayStore store,RelaySecurity sec){
    var ts=L(b,"ts");var tag=S(b,"schoolTag");var sig=S(b,"sig");
    if(!Fresh(ts,120_000)||!sec.Eq(sig,sec.Hmac($"devices\n{tag}\n{ts}")))return Results.Json(new{ok=false,error="auth"},statusCode:403);
    return Results.Json(new{ok=true,devices=store.Devices().Select(x=>new{deviceId=x.DeviceId,ts=x.Ts,payload=x.Payload})});
}
static IResult Command(JsonObject b,RelayStore store,RelaySecurity sec){
    var id=S(b,"deviceId");var action=S(b,"action");var enc=S(b,"enc");var ts=L(b,"ts");var sig=S(b,"sig");var reqSig=S(b,"reqSig");var tag=S(b,"schoolTag");
    if(string.IsNullOrWhiteSpace(id)||string.IsNullOrWhiteSpace(action)||string.IsNullOrWhiteSpace(enc)||!Fresh(ts,120_000))return Results.Json(new{ok=false,error="bad-command"},statusCode:400);
    if(!sec.Eq(sig,sec.Hmac($"{action}\n{ts}\n{enc}")))return Results.Json(new{ok=false,error="command-auth"},statusCode:403);
    if(!sec.Eq(reqSig,sec.Hmac($"command\n{tag}\n{id}\n{action}\n{ts}\n{enc}\n{sig}")))return Results.Json(new{ok=false,error="request-auth"},statusCode:403);
    var cmd=store.CreateCommand(id,action,ts,enc,sig);
    store.AppendAudit("command",id,new{commandId=cmd.Id,action});
    return Results.Json(new{ok=true,commandId=cmd.Id});
}
static IResult Ack(JsonObject b,RelayStore store,RelaySecurity sec){
    var id=S(b,"deviceId");var cid=S(b,"commandId");var ts=L(b,"ts");var ok=B(b,"ok");var enc=S(b,"enc");var tag=S(b,"schoolTag");var sig=S(b,"sig");
    if(!Fresh(ts,120_000)||string.IsNullOrWhiteSpace(id)||string.IsNullOrWhiteSpace(cid))return Results.Json(new{ok=false,error="bad-ack"},statusCode:400);
    if(!sec.Eq(sig,sec.Hmac($"ack\n{tag}\n{id}\n{cid}\n{ts}\n{ok.ToString().ToLowerInvariant()}\n{enc}")) &&
       !sec.Eq(sig,sec.Hmac($"ack\n{tag}\n{id}\n{cid}\n{ts}\n{ok}\n{enc}")))
        return Results.Json(new{ok=false,error="auth"},statusCode:403);
    var changed=store.Ack(cid,id,ok,enc);store.AppendAudit("ack",id,new{commandId=cid,ok});
    return Results.Json(new{ok=changed});
}
static IResult Cancel(JsonObject b,RelayStore store,RelaySecurity sec){
    var cid=S(b,"commandId");var ts=L(b,"ts");var tag=S(b,"schoolTag");var sig=S(b,"sig");
    if(!Fresh(ts,120_000)||!sec.Eq(sig,sec.Hmac($"cancel\n{tag}\n{cid}\n{ts}")))return Results.Json(new{ok=false,error="auth"},statusCode:403);
    return Results.Json(new{ok=store.Cancel(cid)});
}
static IResult Commands(JsonObject b,RelayStore store,RelaySecurity sec){
    var ts=L(b,"ts");var tag=S(b,"schoolTag");var sig=S(b,"sig");var id=S(b,"deviceId");
    if(!Fresh(ts,120_000)||!sec.Eq(sig,sec.Hmac($"commands\n{tag}\n{id}\n{ts}")))return Results.Json(new{ok=false,error="auth"},statusCode:403);
    return Results.Json(new{ok=true,commands=store.CommandStatus(id)});
}
static IResult ScreenPut(JsonObject b,RelayStore store,RelaySecurity sec){
    var id=S(b,"deviceId");var payload=S(b,"payload");var ts=L(b,"ts");var fts=L(b,"frameTs");var tag=S(b,"schoolTag");var sig=S(b,"sig");
    if(!Fresh(ts,120_000)||payload.Length>2_500_000)return Results.Json(new{ok=false,error="bad-screen"},statusCode:400);
    if(!sec.Eq(sig,sec.Hmac($"screen\n{tag}\n{id}\n{ts}\n{fts}\n{payload}")))return Results.Json(new{ok=false,error="auth"},statusCode:403);
    store.PutScreen(id,fts,payload);return Results.Json(new{ok=true});
}
static IResult ScreenGet(JsonObject b,RelayStore store,RelaySecurity sec){
    var id=S(b,"deviceId");var ts=L(b,"ts");var tag=S(b,"schoolTag");var sig=S(b,"sig");
    if(!Fresh(ts,120_000)||!sec.Eq(sig,sec.Hmac($"screenGet\n{tag}\n{id}\n{ts}")))return Results.Json(new{ok=false,error="auth"},statusCode:403);
    var s=store.GetScreen(id);return s is null?Results.Json(new{ok=false,error="no-frame"}):Results.Json(new{ok=true,frameTs=s.FrameTs,payload=s.Payload});
}
static IResult EventPut(JsonObject b,RelayStore store,RelaySecurity sec){
    var id=S(b,"deviceId");var payload=S(b,"payload");var ts=L(b,"ts");var tag=S(b,"schoolTag");var sig=S(b,"sig");
    if(!Fresh(ts,120_000)||!sec.Eq(sig,sec.Hmac($"event\n{tag}\n{id}\n{ts}\n{payload}")))return Results.Json(new{ok=false,error="auth"},statusCode:403);
    store.AppendEvent(id,ts,payload);return Results.Json(new{ok=true});
}
static IResult History(JsonObject b,RelayStore store,RelaySecurity sec){
    var id=S(b,"deviceId");var ts=L(b,"ts");var tag=S(b,"schoolTag");var sig=S(b,"sig");var days=Math.Clamp(I(b,"days",30),1,365);
    if(!Fresh(ts,120_000)||!sec.Eq(sig,sec.Hmac($"history\n{tag}\n{id}\n{days}\n{ts}")))return Results.Json(new{ok=false,error="auth"},statusCode:403);
    return Results.Json(new{ok=true,events=store.ReadEvents(id,days),telemetry=store.ReadTelemetry(id,days)});
}

static string S(JsonObject o,string k)=>o[k]?.GetValue<string>()??"";
static long L(JsonObject o,string k){try{return o[k]?.GetValue<long>()??0;}catch{return 0;}}
static bool B(JsonObject o,string k){try{return o[k]?.GetValue<bool>()??false;}catch{return false;}}
static int I(JsonObject o,string k,int d){try{return o[k]?.GetValue<int>()??d;}catch{return d;}}
static bool Fresh(long ts,long max)=>Math.Abs(DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()-ts)<=max;

public sealed class RelaySecurity{
    readonly string key;
    public string Tag{get;}
    public RelaySecurity(){
        key=Environment.GetEnvironmentVariable("AULAMOVIL_SCHOOL_KEY")??"";
        if(key.Length<10)throw new InvalidOperationException("Define AULAMOVIL_SCHOOL_KEY (10+ caracteres)");
        Tag=Sha(key)[..12];
    }
    public bool ValidTag(string t)=>Eq(t,Tag);
    public string Hmac(string s){using var h=new HMACSHA256(Encoding.UTF8.GetBytes(key));return Convert.ToHexString(h.ComputeHash(Encoding.UTF8.GetBytes(s))).ToLowerInvariant();}
    public static string Sha(string s)=>Convert.ToHexString(SHA256.HashData(Encoding.UTF8.GetBytes(s))).ToLowerInvariant();
    public bool Eq(string a,string b){if(a.Length!=b.Length)return false;return CryptographicOperations.FixedTimeEquals(Encoding.UTF8.GetBytes(a.ToLowerInvariant()),Encoding.UTF8.GetBytes(b.ToLowerInvariant()));}
}

public sealed record Latest(string DeviceId,long Ts,string Payload);
public sealed record ScreenFrame(long FrameTs,string Payload);
public sealed class CommandRecord{
    public string Id{get;set;}="";public string DeviceId{get;set;}="";public string Action{get;set;}="";
    public long Ts{get;set;}public string Enc{get;set;}="";public string Sig{get;set;}="";
    public long CreatedAt{get;set;}public long DeliveredAt{get;set;}public long ExecutedAt{get;set;}
    public string Status{get;set;}="pending";public string ErrorEnc{get;set;}="";
}

public sealed class RelayStore{
    readonly object gate=new();
    readonly string root,latestDir,commandDir,screenDir,telemetryDir,eventDir,auditDir,fileDir;
    readonly int retentionDays;
    long lastCleanup;
    readonly JsonSerializerOptions json=new(){WriteIndented=false};

    public RelayStore(){
        root=Environment.GetEnvironmentVariable("AULAMOVIL_DATA")??Path.Combine(AppContext.BaseDirectory,"data");
        retentionDays=Math.Clamp(int.TryParse(Environment.GetEnvironmentVariable("AULAMOVIL_RETENTION_DAYS"),out var d)?d:30,1,3650);
        latestDir=D("latest");commandDir=D("commands");screenDir=D("screens");telemetryDir=D("telemetry");eventDir=D("events");auditDir=D("audit");fileDir=D("files");
    }
    string D(string n){var p=Path.Combine(root,n);Directory.CreateDirectory(p);return p;}
    static string Safe(string s)=>string.Concat((s??"").Select(ch=>char.IsLetterOrDigit(ch)||ch is '-' or '_' or '.'?ch:'_'));

    public void PutLatest(string id,long ts,string payload){lock(gate)File.WriteAllText(Path.Combine(latestDir,Safe(id)+".json"),JsonSerializer.Serialize(new Latest(id,ts,payload),json));}
    public IEnumerable<Latest> Devices(){lock(gate){foreach(var f in Directory.EnumerateFiles(latestDir,"*.json")){Latest? x=null;try{x=JsonSerializer.Deserialize<Latest>(File.ReadAllText(f));}catch{}if(x is not null)yield return x;}}}
    public void AppendTelemetry(string id,long ts,string payload)=>Append(telemetryDir,new{ts,deviceId=id,payload});
    public void AppendEvent(string id,long ts,string payload)=>Append(eventDir,new{ts,deviceId=id,payload});
    public void AppendAudit(string kind,string id,object detail)=>Append(auditDir,new{ts=DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),kind,deviceId=id,detail});

    public CommandRecord CreateCommand(string id,string action,long ts,string enc,string sig){
        var c=new CommandRecord{Id=Guid.NewGuid().ToString("N"),DeviceId=id,Action=action,Ts=ts,Enc=enc,Sig=sig,CreatedAt=DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()};
        SaveCommand(c);return c;
    }
    void SaveCommand(CommandRecord c){lock(gate)File.WriteAllText(Path.Combine(commandDir,c.Id+".json"),JsonSerializer.Serialize(c,json));}
    CommandRecord? LoadCommand(string path){try{return JsonSerializer.Deserialize<CommandRecord>(File.ReadAllText(path));}catch{return null;}}
    public List<CommandRecord> Pending(string id){
        var now=DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();var outp=new List<CommandRecord>();
        lock(gate){
            foreach(var f in Directory.EnumerateFiles(commandDir,"*.json").OrderBy(x=>File.GetCreationTimeUtc(x))){
                var c=LoadCommand(f);if(c is null||c.DeviceId!=id||c.Status!="pending")continue;
                if(now-c.CreatedAt>7L*24*60*60*1000){c.Status="expired";SaveCommand(c);continue;}
                c.DeliveredAt=now;c.Status="delivered";SaveCommand(c);outp.Add(c);if(outp.Count>=20)break;
            }
        }return outp;
    }
    public bool Ack(string cid,string id,bool ok,string errorEnc){
        lock(gate){var p=Path.Combine(commandDir,Safe(cid)+".json");if(!File.Exists(p))return false;var c=LoadCommand(p);if(c is null||c.DeviceId!=id)return false;c.ExecutedAt=DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();c.Status=ok?"executed":"failed";c.ErrorEnc=errorEnc;SaveCommand(c);return true;}
    }
    public bool Cancel(string cid){
        lock(gate){var p=Path.Combine(commandDir,Safe(cid)+".json");if(!File.Exists(p))return false;var c=LoadCommand(p);if(c is null||c.Status is "executed" or "failed")return false;c.Status="cancelled";SaveCommand(c);return true;}
    }
    public object[] CommandStatus(string id){lock(gate)return Directory.EnumerateFiles(commandDir,"*.json").Select(LoadCommand).Where(x=>x is not null&&(string.IsNullOrWhiteSpace(id)||x.DeviceId==id)).OrderByDescending(x=>x!.CreatedAt).Take(500).Cast<object>().ToArray();}

    public string SaveFile(byte[] bytes,string name,string sha){
        var token=Guid.NewGuid().ToString("N");var safe=Safe(string.IsNullOrWhiteSpace(name)?"archivo.bin":Path.GetFileName(name));
        lock(gate){File.WriteAllBytes(Path.Combine(fileDir,token+".bin"),bytes);File.WriteAllText(Path.Combine(fileDir,token+".json"),JsonSerializer.Serialize(new{name=safe,sha,createdAt=DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()},json));}
        return token;
    }
    public (byte[] Bytes,string Name)? GetFile(string token){
        if(string.IsNullOrWhiteSpace(token)||token.Any(ch=>!char.IsLetterOrDigit(ch)))return null;
        lock(gate){var b=Path.Combine(fileDir,token+".bin");var m=Path.Combine(fileDir,token+".json");if(!File.Exists(b)||!File.Exists(m))return null;if(DateTime.UtcNow-File.GetCreationTimeUtc(b)>TimeSpan.FromHours(4))return null;try{var n=JsonNode.Parse(File.ReadAllText(m)) as JsonObject;return(File.ReadAllBytes(b),n?["name"]?.GetValue<string>()??"archivo.bin");}catch{return null;}}
    }

    public void PutScreen(string id,long frameTs,string payload){
        lock(gate)File.WriteAllText(Path.Combine(screenDir,Safe(id)+".json"),JsonSerializer.Serialize(new ScreenFrame(frameTs,payload),json));
    }
    public ScreenFrame? GetScreen(string id){lock(gate){var p=Path.Combine(screenDir,Safe(id)+".json");if(!File.Exists(p))return null;try{return JsonSerializer.Deserialize<ScreenFrame>(File.ReadAllText(p));}catch{return null;}}}

    void Append(string dir,object o){
        lock(gate){var f=Path.Combine(dir,DateTime.UtcNow.ToString("yyyy-MM-dd")+".jsonl");File.AppendAllText(f,JsonSerializer.Serialize(o,json)+Environment.NewLine,Encoding.UTF8);}
    }
    public object[] ReadEvents(string id,int days)=>ReadLog(eventDir,id,days);
    public object[] ReadTelemetry(string id,int days)=>ReadLog(telemetryDir,id,days);
    object[] ReadLog(string dir,string id,int days){
        var cutoff=DateTime.UtcNow.Date.AddDays(-days+1);var list=new List<object>();
        lock(gate){
            foreach(var f in Directory.EnumerateFiles(dir,"*.jsonl")){
                if(File.GetLastWriteTimeUtc(f)<cutoff)continue;
                foreach(var line in File.ReadLines(f)){
                    try{var n=JsonNode.Parse(line) as JsonObject;if(n is null)continue;if(!string.IsNullOrWhiteSpace(id)&&S(n,"deviceId")!=id)continue;list.Add(n);}catch{}
                    if(list.Count>=10000)break;
                }
            }
        }return list.ToArray();
    }
    static string S(JsonObject o,string k)=>o[k]?.GetValue<string>()??"";

    public void CleanupIfNeeded(){
        var now=DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();if(now-lastCleanup<60*60*1000)return;lastCleanup=now;
        lock(gate){
            var cutoff=DateTime.UtcNow.AddDays(-retentionDays);
            foreach(var dir in new[]{telemetryDir,eventDir,auditDir})foreach(var f in Directory.EnumerateFiles(dir,"*.jsonl"))if(File.GetLastWriteTimeUtc(f)<cutoff)try{File.Delete(f);}catch{}
            foreach(var f in Directory.EnumerateFiles(commandDir,"*.json"))if(File.GetLastWriteTimeUtc(f)<cutoff)try{File.Delete(f);}catch{}
            foreach(var f in Directory.EnumerateFiles(fileDir,"*.*"))if(DateTime.UtcNow-File.GetLastWriteTimeUtc(f)>TimeSpan.FromHours(4))try{File.Delete(f);}catch{}
        }
    }
}
