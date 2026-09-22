package cl.antumapu.pangi.v205;

import android.content.Context;
import android.os.Build;
import android.util.Base64;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Relay HTTPS de PANGI 11.
 * El servidor conserva telemetría/comandos y, cuando una consola solicita
 * supervisión remota, únicamente el último cuadro cifrado. No archiva video.
 */
final class RemoteRelay {
    private static final AtomicBoolean screenBusy=new AtomicBoolean(false);
    private RemoteRelay(){}

    static void pulse(AgentService service){
        String endpoint=RelayPrefs.url(service);
        if(!https(endpoint))return;
        String key=Core.key(service);if(key.isEmpty())return;
        try{
            JSONObject detail=new JSONObject();
            detail.put("deviceName",Core.dn(service));
            detail.put("user",Core.user(service));
            detail.put("course",Core.course(service));
            detail.put("role",Core.role(service));
            detail.put("model",Build.MANUFACTURER+" "+Build.MODEL);
            detail.put("android",Build.VERSION.RELEASE);
            detail.put("appVersion",service.appVersion());
            detail.put("battery",service.battery());
            detail.put("charging",service.charging());
            detail.put("managed",Managed.managed(service));
            detail.put("screen",ScreenCaptureService.active);
            detail.put("lost",RecoveryPrefs.lost(service));
            detail.put("lat",LocationTracker.lat(service));
            detail.put("lon",LocationTracker.lon(service));
            detail.put("accuracy",LocationTracker.accuracy(service));
            detail.put("locationTs",LocationTracker.time(service));
            detail.put("ipLocal",Core.ip());
            detail.put("wifi",service.wifiName());
            detail.put("storageFreeMb",service.storageFreeMb());
            detail.put("storageTotalMb",service.storageTotalMb());
            detail.put("usageAccess",Core.usageAccess(service));
            detail.put("state",SessionState.get(service).name());
            detail.put("fps",Core.fps(service));
            detail.put("idleStudentMin",Core.idleMinutes(service,Core.ROLE_STUDENT));
            detail.put("idleTeacherMin",Core.idleMinutes(service,Core.ROLE_TEACHER));

            String op="telemetry",tag=Core.sha(key).substring(0,12),id=Core.id(service),payload=Core.sealText(key,detail.toString());
            long ts=System.currentTimeMillis();
            String sig=Core.hmac(key,op+"\n"+tag+"\n"+id+"\n"+ts+"\n"+payload);
            JSONObject req=new JSONObject();req.put("op",op);req.put("schoolTag",tag);req.put("deviceId",id);req.put("ts",ts);req.put("payload",payload);req.put("sig",sig);
            JSONObject res=post(endpoint,req);
            if(res==null||!res.optBoolean("ok",false))return;
            JSONArray commands=res.optJSONArray("commands");if(commands==null)return;
            for(int i=0;i<commands.length();i++){
                JSONObject c=commands.optJSONObject(i);if(c==null)continue;
                String commandId=c.optString("id","");
                String action=c.optString("action"),enc=c.optString("enc"),csig=c.optString("sig");long cts=c.optLong("ts");
                if(action.isEmpty()||enc.isEmpty()||csig.isEmpty())continue;
                if(Math.abs(System.currentTimeMillis()-cts)>24*60*60*1000L)continue;
                if(!Core.eq(Core.hmac(key,action+"\n"+cts+"\n"+enc),csig))continue;
                String value=Core.openText(key,enc);
                String error=service.action(action,value);
                ack(endpoint,key,tag,id,commandId,error==null,error==null?"":error);
            }
        }catch(Exception ignored){}
    }

    static void uploadScreenAsync(Context c,byte[] jpg,long frameTs){
        if(c==null||jpg==null||jpg.length==0||!Core.remoteScreenRequested(c))return;
        if(!screenBusy.compareAndSet(false,true))return;
        new Thread(()->{try{uploadScreen(c.getApplicationContext(),jpg,frameTs);}finally{screenBusy.set(false);}},"AulaMovilRemoteFrame").start();
    }

    private static void uploadScreen(Context c,byte[] jpg,long frameTs){
        String endpoint=RelayPrefs.url(c),key=Core.key(c);if(!https(endpoint)||key.isEmpty())return;
        try{
            String tag=Core.sha(key).substring(0,12),id=Core.id(c);
            String payload=Base64.encodeToString(Core.seal(key,jpg),Base64.NO_WRAP);
            long ts=System.currentTimeMillis();
            String sig=Core.hmac(key,"screen\n"+tag+"\n"+id+"\n"+ts+"\n"+frameTs+"\n"+payload);
            JSONObject q=new JSONObject().put("op","screen").put("schoolTag",tag).put("deviceId",id)
                    .put("ts",ts).put("frameTs",frameTs).put("payload",payload).put("sig",sig);
            post(endpoint,q);
        }catch(Exception ignored){}
    }

    static void eventAsync(Context c,String type,JSONObject detail){
        if(c==null||type==null||type.isEmpty())return;
        new Thread(()->event(c.getApplicationContext(),type,detail),"AulaMovilEvent").start();
    }

    private static void event(Context c,String type,JSONObject detail){
        String endpoint=RelayPrefs.url(c),key=Core.key(c);if(!https(endpoint)||key.isEmpty())return;
        try{
            String tag=Core.sha(key).substring(0,12),id=Core.id(c);
            long ts=System.currentTimeMillis();
            String plain=new JSONObject().put("type",type).put("detail",detail==null?new JSONObject():detail).toString();
            String payload=Core.sealText(key,plain);
            String sig=Core.hmac(key,"event\n"+tag+"\n"+id+"\n"+ts+"\n"+payload);
            post(endpoint,new JSONObject().put("op","event").put("schoolTag",tag).put("deviceId",id)
                    .put("ts",ts).put("payload",payload).put("sig",sig));
        }catch(Exception ignored){}
    }

    private static void ack(String endpoint,String key,String tag,String id,String commandId,boolean ok,String error){
        if(commandId==null||commandId.isEmpty())return;
        try{
            long ts=System.currentTimeMillis();
            String enc=Core.sealText(key,error==null?"":error);
            String sig=Core.hmac(key,"ack\n"+tag+"\n"+id+"\n"+commandId+"\n"+ts+"\n"+ok+"\n"+enc);
            post(endpoint,new JSONObject().put("op","ack").put("schoolTag",tag).put("deviceId",id)
                    .put("commandId",commandId).put("ts",ts).put("ok",ok).put("enc",enc).put("sig",sig));
        }catch(Exception ignored){}
    }

    static JSONObject post(String endpoint,JSONObject body){
        HttpURLConnection c=null;
        try{
            URL u=new URL(endpoint);c=(HttpURLConnection)u.openConnection();c.setConnectTimeout(7000);c.setReadTimeout(9000);c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json; charset=utf-8");
            byte[] b=body.toString().getBytes(StandardCharsets.UTF_8);c.setFixedLengthStreamingMode(b.length);try(OutputStream o=c.getOutputStream()){o.write(b);}int code=c.getResponseCode();if(code<200||code>=300)return null;
            try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buf=new byte[4096];int n;while((n=in.read(buf))>0)out.write(buf,0,n);return new JSONObject(out.toString(StandardCharsets.UTF_8.name()));}
        }catch(Exception e){return null;}finally{if(c!=null)c.disconnect();}
    }

    static boolean https(String s){
        return s!=null&&!s.isEmpty()&&s.toLowerCase().startsWith("https://");
    }
}
