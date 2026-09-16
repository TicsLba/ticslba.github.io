package cl.antumapu.aulacontrol;

import android.os.Build;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

final class RemoteRelay {
    private RemoteRelay(){}

    static void pulse(AgentService service){
        String endpoint=RelayPrefs.url(service);
        if(endpoint.isEmpty()||!endpoint.toLowerCase().startsWith("https://"))return;
        String key=Core.key(service);if(key.isEmpty())return;
        try{
            JSONObject detail=new JSONObject();
            detail.put("deviceName",Core.dn(service));
            detail.put("user",Core.user(service));
            detail.put("course",Core.course(service));
            detail.put("role",Core.role(service));
            detail.put("model",Build.MANUFACTURER+" "+Build.MODEL);
            detail.put("android",Build.VERSION.RELEASE);
            detail.put("battery",service.battery());
            detail.put("managed",Managed.managed(service));
            detail.put("screen",ScreenCaptureService.active);
            detail.put("lost",RecoveryPrefs.lost(service));
            detail.put("lat",LocationTracker.lat(service));
            detail.put("lon",LocationTracker.lon(service));
            detail.put("accuracy",LocationTracker.accuracy(service));
            detail.put("locationTs",LocationTracker.time(service));

            String op="telemetry",tag=Core.sha(key).substring(0,12),id=Core.id(service),payload=Core.sealText(key,detail.toString());
            long ts=System.currentTimeMillis();
            String sig=Core.hmac(key,op+"\n"+tag+"\n"+id+"\n"+ts+"\n"+payload);
            JSONObject req=new JSONObject();req.put("op",op);req.put("schoolTag",tag);req.put("deviceId",id);req.put("ts",ts);req.put("payload",payload);req.put("sig",sig);
            JSONObject res=post(endpoint,req);
            if(res==null||!res.optBoolean("ok",false))return;
            JSONArray commands=res.optJSONArray("commands");if(commands==null)return;
            for(int i=0;i<commands.length();i++){
                JSONObject c=commands.optJSONObject(i);if(c==null)continue;
                String action=c.optString("action"),enc=c.optString("enc"),csig=c.optString("sig");long cts=c.optLong("ts");
                if(action.isEmpty()||enc.isEmpty()||csig.isEmpty())continue;
                if(Math.abs(System.currentTimeMillis()-cts)>15*60*1000L)continue;
                if(!Core.eq(Core.hmac(key,action+"\n"+cts+"\n"+enc),csig))continue;
                String value=Core.openText(key,enc);
                service.action(action,value);
            }
        }catch(Exception ignored){}
    }

    static JSONObject post(String endpoint,JSONObject body){
        HttpURLConnection c=null;
        try{
            URL u=new URL(endpoint);c=(HttpURLConnection)u.openConnection();c.setConnectTimeout(7000);c.setReadTimeout(7000);c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json; charset=utf-8");
            byte[] b=body.toString().getBytes(StandardCharsets.UTF_8);c.setFixedLengthStreamingMode(b.length);try(OutputStream o=c.getOutputStream()){o.write(b);}int code=c.getResponseCode();if(code<200||code>=300)return null;
            try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buf=new byte[4096];int n;while((n=in.read(buf))>0)out.write(buf,0,n);return new JSONObject(out.toString(StandardCharsets.UTF_8.name()));}
        }catch(Exception e){return null;}finally{if(c!=null)c.disconnect();}
    }
}
