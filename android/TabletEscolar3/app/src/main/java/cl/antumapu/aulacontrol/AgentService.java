package cl.antumapu.aulacontrol;

import android.app.*;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.widget.Toast;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class AgentService extends Service {
    static final int UDP_PORT=45888,CMD_PORT=45901,NOTIFICATION_ID=3001;
    static final long WARNING_MS=30_000L;
    static final String ACTION_KEEP="TE3_KEEP",ACTION_LOGOUT="TE3_LOGOUT";
    volatile boolean run,warning;
    ScheduledExecutorService jobs;ServerSocket commandSocket;Handler ui=new Handler(Looper.getMainLooper());long screenOffSince=0,lastLocationRequest=0;
    final Runnable autoLogout=this::logout;

    @Override public void onCreate(){super.onCreate();startForeground(NOTIFICATION_ID,note());run=true;jobs=Executors.newScheduledThreadPool(4);jobs.scheduleAtFixedRate(this::beacon,0,3,TimeUnit.SECONDS);jobs.scheduleAtFixedRate(this::idleTick,5,5,TimeUnit.SECONDS);jobs.scheduleAtFixedRate(this::locationTick,5,60,TimeUnit.SECONDS);jobs.scheduleAtFixedRate(()->RelayClient.pulse(this),10,30,TimeUnit.SECONDS);new Thread(this::commandServer,"TabletEscolarCommands").start();if(Store.lost(this))ui.post(this::openLostMode);}
    @Override public int onStartCommand(Intent i,int flags,int id){if(i!=null&&ACTION_KEEP.equals(i.getAction()))keep();else if(i!=null&&ACTION_LOGOUT.equals(i.getAction()))logout();else refreshNotification();if(Store.lost(this))ui.post(this::openLostMode);return START_STICKY;}
    @Override public IBinder onBind(Intent i){return null;}
    @Override public void onDestroy(){run=false;if(jobs!=null)jobs.shutdownNow();ui.removeCallbacks(autoLogout);try{if(commandSocket!=null)commandSocket.close();}catch(Exception ignored){}super.onDestroy();}

    void idleTick(){
        if(!PolicyManager.profileOwner(this)||!Store.STATE_ACTIVE.equals(Store.state(this))||Store.user(this).isEmpty()){cancelWarning();return;}
        if(Store.lost(this)){cancelWarning();return;}
        if(!CaptureService.active){if(Store.supervisionStarted(this))ui.post(this::logout);return;}
        long now=System.currentTimeMillis(),last;
        if(hasUsageAccess()){
            last=Math.max(Store.lastActivity(this),latestUsageInteraction());if(last>Store.lastActivity(this))Store.sp(this).edit().putLong("last_activity",last).apply();
            if(now-last>=Store.idleLimit(this)&&!warning)ui.post(this::showWarning);
        }else{
            PowerManager p=(PowerManager)getSystemService(POWER_SERVICE);boolean interactive=p!=null&&p.isInteractive();
            if(interactive){screenOffSince=0;cancelWarning();return;}
            if(screenOffSince==0)screenOffSince=now;
            if(now-screenOffSince>=Store.idleLimit(this)&&!warning)ui.post(this::showWarning);
        }
    }
    boolean hasUsageAccess(){try{AppOpsManager a=(AppOpsManager)getSystemService(APP_OPS_SERVICE);return a.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,android.os.Process.myUid(),getPackageName())==AppOpsManager.MODE_ALLOWED;}catch(Exception e){return false;}}
    long latestUsageInteraction(){long latest=Store.lastActivity(this);try{UsageStatsManager m=(UsageStatsManager)getSystemService(USAGE_STATS_SERVICE);long now=System.currentTimeMillis();UsageEvents es=m.queryEvents(Math.max(0,now-35*60_000L),now);UsageEvents.Event e=new UsageEvents.Event();while(es!=null&&es.hasNextEvent()){es.getNextEvent(e);int t=e.getEventType();if(t==UsageEvents.Event.USER_INTERACTION||t==UsageEvents.Event.ACTIVITY_RESUMED||t==UsageEvents.Event.SCREEN_INTERACTIVE||t==UsageEvents.Event.KEYGUARD_HIDDEN)latest=Math.max(latest,e.getTimeStamp());}}catch(Exception ignored){}return latest;}

    void showWarning(){
        if(warning||Store.user(this).isEmpty())return;warning=true;long deadline=System.currentTimeMillis()+WARNING_MS;String mins=Store.ROLE_TEACHER.equals(Store.role(this))?"30 minutos":"10 minutos";
        Intent keep=new Intent(this,AgentService.class).setAction(ACTION_KEEP),logout=new Intent(this,AgentService.class).setAction(ACTION_LOGOUT);PendingIntent kp=PendingIntent.getService(this,501,keep,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE),lp=PendingIntent.getService(this,502,logout,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b=builder("session_alert",NotificationManager.IMPORTANCE_HIGH).setContentTitle("¿Sigues usando la tablet?").setContentText("Sin actividad durante "+mins+" · cierre en 30 s").setSmallIcon(android.R.drawable.ic_dialog_alert).setOngoing(true).setOnlyAlertOnce(true).setWhen(deadline).setUsesChronometer(true).addAction(new Notification.Action.Builder(null,"Seguiré usando",kp).build()).addAction(new Notification.Action.Builder(null,"Cerrar sesión",lp).build());if(Build.VERSION.SDK_INT>=24)b.setChronometerCountDown(true);((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID,b.build());
        try{startActivity(new Intent(this,AttentionActivity.class).putExtra("deadline",deadline).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));}catch(Exception ignored){}
        ui.removeCallbacks(autoLogout);ui.postDelayed(autoLogout,WARNING_MS);
    }
    void keep(){Store.touch(this);screenOffSince=0;cancelWarning();Toast.makeText(this,"La sesión continúa activa",Toast.LENGTH_SHORT).show();}
    void cancelWarning(){if(!warning)return;warning=false;ui.removeCallbacks(autoLogout);sendBroadcast(new Intent("cl.antumapu.aulacontrol.WARNING_CLOSED").setPackage(getPackageName()));refreshNotification();}

    void logout(){
        if(!PolicyManager.profileOwner(this))return;warning=false;ui.removeCallbacks(autoLogout);Store.state(this,Store.STATE_CLOSING);CaptureService.stop(this,true);Store.clearGuestIdentity(this);beacon();SessionUsers.logoutGuest(this);
    }

    void locationTick(){if(!PolicyManager.managed(this))return;long now=System.currentTimeMillis(),period=Store.lost(this)?60_000L:300_000L;if(now-lastLocationRequest<period)return;lastLocationRequest=now;LocationTracker.request(this);}

    void beacon(){
        try{
            String key=Store.technicalKey(this);if(key.isEmpty())return;String ip=NetUtil.ip();long ts=System.currentTimeMillis();int bat=battery();long frameAge=CaptureService.lastFrameAt>0?Math.max(0,ts-CaptureService.lastFrameAt):-1;String ue=Crypto.sealText(key,Store.user(this)),ce=Crypto.sealText(key,Store.course(this)),re=Crypto.sealText(key,Store.role(this));String lat=LocationTracker.lat(this),lon=LocationTracker.lon(this),acc=LocationTracker.accuracy(this);long locTs=LocationTracker.time(this);
            JSONObject j=new JSONObject();j.put("type","AULACONTROL_BEACON");j.put("version",9);j.put("schoolTag",Crypto.sha256(key).substring(0,12));j.put("deviceId",Store.deviceId(this));j.put("deviceName",Store.deviceName(this));j.put("userEnc",ue);j.put("courseEnc",ce);j.put("roleEnc",re);j.put("model",Build.MANUFACTURER+" "+Build.MODEL);j.put("android",Build.VERSION.RELEASE);j.put("ip",ip);j.put("commandPort",CMD_PORT);j.put("screenPort",CaptureService.PORT);j.put("screenSharing",CaptureService.active);j.put("managed",PolicyManager.managed(this));j.put("frameAge",frameAge);j.put("battery",bat);j.put("lat",lat);j.put("lon",lon);j.put("accuracy",acc);j.put("locationTs",locTs);j.put("lost",Store.lost(this));j.put("state",Store.state(this));j.put("ts",ts);
            String wire=Store.deviceId(this)+"\n"+Store.deviceName(this)+"\n"+ue+"\n"+ce+"\n"+re+"\n"+ip+"\n"+CMD_PORT+"\n"+CaptureService.PORT+"\n"+CaptureService.active+"\n"+PolicyManager.managed(this)+"\n"+frameAge+"\n"+bat+"\n"+lat+"\n"+lon+"\n"+acc+"\n"+locTs+"\n"+Store.lost(this)+"\n"+ts;j.put("sig",Crypto.hmac(key,wire));byte[] data=j.toString().getBytes(StandardCharsets.UTF_8);DatagramSocket s=new DatagramSocket();s.setBroadcast(true);for(InetAddress a:NetUtil.broadcasts())try{s.send(new DatagramPacket(data,data.length,a,UDP_PORT));}catch(Exception ignored){}s.close();
        }catch(Exception ignored){}
    }

    int battery(){Intent i=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));if(i==null)return-1;int l=i.getIntExtra("level",-1),s=i.getIntExtra("scale",100);return s>0?(int)(100d*l/s):-1;}

    void commandServer(){try{commandSocket=new ServerSocket(CMD_PORT);commandSocket.setReuseAddress(true);while(run)try(Socket c=commandSocket.accept()){c.setSoTimeout(4000);handle(c);}catch(Exception ignored){}}catch(Exception ignored){}finally{commandSocket=null;}}
    void handle(Socket c)throws Exception{BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream(),StandardCharsets.UTF_8));String line=r.readLine();if(line==null)return;JSONObject q=new JSONObject(line);String a=q.optString("action"),sig=q.optString("sig"),enc=q.optString("enc"),key=Store.technicalKey(this);long ts=q.optLong("ts");boolean ok=Math.abs(System.currentTimeMillis()-ts)<60_000&&Crypto.equal(Crypto.hmac(key,a+"\n"+ts+"\n"+enc),sig);String value=ok?Crypto.openText(key,enc):"";String err=ok?action(a,value):"auth";JSONObject out=new JSONObject();out.put("ok",err==null);if(err!=null)out.put("error",err);c.getOutputStream().write((out.toString()+"\n").getBytes(StandardCharsets.UTF_8));}

    String action(String action,String value){switch(action){
        case "PING":return null;
        case "MESSAGE":message(value);return null;
        case "OPEN_URL":openUrl(value);return null;
        case "FORCE_LOGOUT":ui.post(this::logout);return null;
        case "ATTENTION_ON":attention(value);return null;
        case "ATTENTION_OFF":sendBroadcast(new Intent("cl.antumapu.aulacontrol.ATTENTION_OFF").setPackage(getPackageName()));return null;
        case "LAUNCH_APP":launchApp(value);return null;
        case "REQUEST_LOCATION":LocationTracker.request(this);beacon();return null;
        case "LOCK_NOW":ui.post(()->PolicyManager.lockNow(this));return null;
        case "LOST_MODE_ON":ui.post(this::lostOn);return null;
        case "LOST_MODE_OFF":ui.post(this::lostOff);return null;
        case "SET_RELAY_URL":if(value==null||value.isEmpty()||value.toLowerCase().startsWith("https://")){Store.relay(this,value);return null;}return "relay-must-use-https";
        default:return "unknown";
    }}
    void lostOn(){Store.lost(this,true);LocationTracker.request(this);openLostMode();refreshNotification();beacon();ui.postDelayed(()->PolicyManager.lockNow(this),700);}
    void lostOff(){Store.lost(this,false);refreshNotification();beacon();sendBroadcast(new Intent("cl.antumapu.aulacontrol.LOST_MODE_OFF").setPackage(getPackageName()));if(PolicyManager.profileOwner(this))try{startActivity(new Intent(this,SessionSetupActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP));}catch(Exception ignored){}}
    void openLostMode(){try{startActivity(new Intent(this,LostModeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));}catch(Exception ignored){}}
    void attention(String v){try{startActivity(new Intent(this,AttentionActivity.class).putExtra("message",v).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));}catch(Exception ignored){}}
    void launchApp(String pkg){try{Intent i=getPackageManager().getLaunchIntentForPackage(pkg);if(i!=null){i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);}}catch(Exception ignored){}}
    void message(String v){Notification n=builder("messages",NotificationManager.IMPORTANCE_HIGH).setContentTitle("Mensaje del docente · Tablet Escolar").setContentText(v).setStyle(new Notification.BigTextStyle().bigText(v)).setSmallIcon(android.R.drawable.ic_dialog_info).build();((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(3003,n);}
    void openUrl(String v){try{Uri u=Uri.parse(v);if(!"http".equalsIgnoreCase(u.getScheme())&&!"https".equalsIgnoreCase(u.getScheme()))return;Intent i=new Intent(Intent.ACTION_VIEW,u).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);}catch(Exception ignored){}}

    void refreshNotification(){try{((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID,note());}catch(Exception ignored){}}
    PendingIntent panelIntent(){Class<?> k=PolicyManager.profileOwner(this)?SessionSetupActivity.class:GateActivity.class;Intent i=new Intent(this,k).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);return PendingIntent.getActivity(this,504,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
    Notification note(){boolean lost=Store.lost(this);String who=Store.user(this).isEmpty()?Store.deviceName(this):Store.user(this)+" · "+Store.roleLabel(this);Notification.Builder b=builder("agent",lost?NotificationManager.IMPORTANCE_HIGH:NotificationManager.IMPORTANCE_LOW).setContentTitle(lost?"Tablet Escolar · MODO PÉRDIDA":"Tablet Escolar · dispositivo protegido").setContentText(lost?Store.deviceName(this)+" · recuperación activa":who).setSmallIcon(lost?android.R.drawable.ic_lock_lock:android.R.drawable.presence_online).setContentIntent(panelIntent()).setOngoing(true).setOnlyAlertOnce(true);if(PolicyManager.profileOwner(this)&&!Store.user(this).isEmpty()){Intent out=new Intent(this,AgentService.class).setAction(ACTION_LOGOUT);PendingIntent p=PendingIntent.getService(this,502,out,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);b.addAction(new Notification.Action.Builder(null,"Cerrar sesión",p).build());}return b.build();}
    Notification.Builder builder(String ch,int importance){NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);if(Build.VERSION.SDK_INT>=26&&nm.getNotificationChannel(ch)==null)nm.createNotificationChannel(new NotificationChannel(ch,"Tablet Escolar",importance));return Build.VERSION.SDK_INT>=26?new Notification.Builder(this,ch):new Notification.Builder(this);}
}
