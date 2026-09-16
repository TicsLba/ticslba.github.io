package cl.antumapu.aulacontrol;

import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class RelayClient {
    private RelayClient(){}
    static void pulse(AgentService service){
        String endpoint=Store.relay(service);if(endpoint.isEmpty()||!endpoint.toLowerCase().startsWith("https://"))return;String key=Store.technicalKey(service);if(key.isEmpty())return;
        try{
            JSONObject detail=new JSONObject();detail.put("deviceName",Store.deviceName(service));detail.put("user",Store.user(service));detail.put("course",Store.course(service));detail.put("role",Store.role(service));detail.put("model",Build.MANUFACTURER+" "+Build.MODEL);detail.put("android",Build.VERSION.RELEASE);detail.put("battery",service.battery());detail.put("managed",PolicyManager.managed(service));detail.put("screen",CaptureService.active);detail.put("lost",Store.lost(service));detail.put("lat",LocationTracker.lat(service));detail.put("lon",LocationTracker.lon(service));detail.put("accuracy",LocationTracker.accuracy(service));detail.put("locationTs",LocationTracker.time(service));
            String op="telemetry",tag=Crypto.sha256(key).substring(0,12),id=Store.deviceId(service),payload=Crypto.sealText(key,detail.toString());long ts=System.currentTimeMillis();String sig=Crypto.hmac(key,op+"\n"+tag+"\n"+id+"\n"+ts+"\n"+payload);JSONObject req=new JSONObject();req.put("op",op);req.put("schoolTag",tag);req.put("deviceId",id);req.put("ts",ts);req.put("payload",payload);req.put("sig",sig);
            JSONObject res=post(endpoint,req);if(res==null||!res.optBoolean("ok",false))return;JSONArray cmds=res.optJSONArray("commands");if(cmds==null)return;
            for(int i=0;i<cmds.length();i++){JSONObject c=cmds.optJSONObject(i);if(c==null)continue;String action=c.optString("action"),enc=c.optString("enc"),csig=c.optString("sig");long cts=c.optLong("ts");if(action.isEmpty()||enc.isEmpty()||csig.isEmpty()||Math.abs(System.currentTimeMillis()-cts)>15*60_000L)continue;if(!Crypto.equal(Crypto.hmac(key,action+"\n"+cts+"\n"+enc),csig))continue;service.action(action,Crypto.openText(key,enc));}
        }catch(Exception ignored){}
    }
    static JSONObject post(String endpoint,JSONObject body){HttpURLConnection c=null;try{c=(HttpURLConnection)new URL(endpoint).openConnection();c.setConnectTimeout(7000);c.setReadTimeout(7000);c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json; charset=utf-8");byte[] b=body.toString().getBytes(StandardCharsets.UTF_8);c.setFixedLengthStreamingMode(b.length);try(OutputStream o=c.getOutputStream()){o.write(b);}int code=c.getResponseCode();if(code<200||code>=300)return null;try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buf=new byte[4096];int n;while((n=in.read(buf))>0)out.write(buf,0,n);return new JSONObject(out.toString(StandardCharsets.UTF_8.name()));}}catch(Exception e){return null;}finally{if(c!=null)c.disconnect();}}
}
