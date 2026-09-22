package cl.antumapu.pangi.v205;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.media.AudioManager;
import android.content.pm.PackageInfo;
import android.os.StatFs;
import android.os.*;
import android.widget.Toast;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class AgentService extends Service {
    static final int UDP=45888,CMD=45901,SESSION_NOTIFICATION=1001;
    static final long WARN=120_000L;
    static final String KEEP="AC_KEEP",LOGOUT="AC_LOGOUT";
    volatile boolean run,warn;
    ScheduledExecutorService ex;
    ServerSocket commandSocket;
    Handler ui=new Handler(Looper.getMainLooper());
    Runnable autoLogout=this::logout;
    long lastLocRequest;

    @Override public void onCreate(){
        super.onCreate();startForeground(SESSION_NOTIFICATION,note());run=true;
        ex=Executors.newScheduledThreadPool(4);
        ex.scheduleAtFixedRate(this::beacon,0,3,TimeUnit.SECONDS);
        ex.scheduleAtFixedRate(this::guard,3,3,TimeUnit.SECONDS);
        ex.scheduleAtFixedRate(this::idle,5,5,TimeUnit.SECONDS);
        ex.scheduleAtFixedRate(this::locationTick,4,60,TimeUnit.SECONDS);
        ex.scheduleAtFixedRate(()->RemoteRelay.pulse(this),5,15,TimeUnit.SECONDS);
        ex.scheduleAtFixedRate(()->UsageReporter.tick(this),20,60,TimeUnit.SECONDS);
        new Thread(this::server,"AulaMovilCmd").start();
        if(RecoveryPrefs.lost(this))ui.post(this::openLostMode);
    }

    @Override public int onStartCommand(Intent i,int flags,int id){
        if(i!=null&&KEEP.equals(i.getAction()))keep();else if(i!=null&&LOGOUT.equals(i.getAction()))logout();else refreshNotification();
        if(RecoveryPrefs.lost(this))ui.post(this::openLostMode);return START_STICKY;
    }
    @Override public IBinder onBind(Intent i){return null;}
    @Override public void onDestroy(){run=false;if(ex!=null)ex.shutdownNow();ui.removeCallbacks(autoLogout);try{if(commandSocket!=null)commandSocket.close();}catch(Exception ignored){}super.onDestroy();}

    void guard(){
        if(!Core.guest(this)||Core.user(this).isEmpty()||RecoveryPrefs.lost(this))return;
        if(SessionState.isActive(this)&&Core.supervisionStarted(this)&&!ScreenCaptureService.active){
            // La sesión continúa. La consola verá "sin supervisión" y podrá
            // solicitar nuevamente el permiso oficial de MediaProjection.
            Core.supervisionStarted(this,false);beacon();
        }
    }

    void idle(){
  PlayStoreGuard.relockIfExpired(this);
        if(RecoveryPrefs.lost(this)){if(warn)cancelWarning();return;}
        if(!Core.guest(this)||Core.user(this).isEmpty()||!SessionState.isActive(this)){if(warn)cancelWarning();return;}
        long last=Core.last(this);
        if(Core.usageAccess(this))last=Math.max(last,Core.activity(this));
        else if(ScreenCaptureService.active)last=Math.max(last,ScreenCaptureService.lastVisualChangeAt);
        if(last>Core.last(this))Core.sp(this).edit().putLong("last",last).apply();
        if(!warn&&System.currentTimeMillis()-last>=Core.idleLimit(this))ui.post(this::showWarn);
    }

    void showWarn(){
        if(warn||Core.user(this).isEmpty()||!SessionState.isActive(this))return;warn=true;
        Intent ki=new Intent(this,AgentService.class).setAction(KEEP),lo=new Intent(this,AgentService.class).setAction(LOGOUT);
        PendingIntent kp=PendingIntent.getService(this,201,ki,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE),lp=PendingIntent.getService(this,202,lo,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b=builder("agent",NotificationManager.IMPORTANCE_HIGH).setContentTitle("PANGI · ¿sigues ahí?").setContentText("La sesión se cerrará en 2 minutos").setSmallIcon(android.R.drawable.ic_dialog_alert).setOngoing(true).setOnlyAlertOnce(true).addAction(new Notification.Action.Builder(null,"Seguiré usando",kp).build()).addAction(new Notification.Action.Builder(null,"Cerrar sesión",lp).build());
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(SESSION_NOTIFICATION,b.build());
        try{startActivity(new Intent(this,SessionWarningActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));}catch(Exception ignored){}
        ui.removeCallbacks(autoLogout);ui.postDelayed(autoLogout,WARN);
    }

    void keep(){if(Core.user(this).isEmpty()){cancelWarning();return;}Core.touch(this);cancelWarning();beacon();Toast.makeText(this,"La sesión continúa activa",Toast.LENGTH_SHORT).show();}
    void cancelWarning(){warn=false;ui.removeCallbacks(autoLogout);refreshNotification();}
    void refreshNotification(){try{((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(SESSION_NOTIFICATION,note());}catch(Exception ignored){}}

    void logout(){
        if(RecoveryPrefs.lost(this)){openLostMode();return;}warn=false;ui.removeCallbacks(autoLogout);
        if(Core.guest(this)){SessionState.set(this,SessionState.State.CLOSING);Core.supervisionStarted(this,false);Core.remoteScreenUntil(this,0);Managed.homeGuardOn(this);stopService(new Intent(this,ScreenCaptureService.class));ui.post(this::openGate);new Thread(()->SessionUsers.logoutGuest(this),"AulaMovilLogout").start();}
        else ui.post(this::openGate);
    }

    void openGate(){try{startActivity(new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));}catch(Exception ignored){}}

    void locationTick(){if(!Managed.managed(this))return;long now=System.currentTimeMillis(),every=RecoveryPrefs.lost(this)?60_000L:300_000L;if(now-lastLocRequest<every)return;lastLocRequest=now;LocationTracker.request(this);}

    void beacon(){try{
        String k=Core.key(this);if(k.isEmpty())return;String ip=Core.ip();int bat=battery();long ts=System.currentTimeMillis();boolean managed=Managed.managed(this);long frameAge=ScreenCaptureService.lastFrameAt>0?Math.max(0,ts-ScreenCaptureService.lastFrameAt):-1;
        String ue=Core.sealText(k,Core.user(this)),ce=Core.sealText(k,Core.course(this)),re=Core.sealText(k,Core.role(this));String lat=LocationTracker.lat(this),lon=LocationTracker.lon(this),acc=LocationTracker.accuracy(this);long locTs=LocationTracker.time(this);boolean lost=RecoveryPrefs.lost(this);
        JSONObject j=new JSONObject();j.put("type","AULACONTROL_BEACON");j.put("version",11);j.put("schoolTag",Core.sha(k).substring(0,12));j.put("deviceId",Core.id(this));j.put("deviceName",Core.dn(this));j.put("userEnc",ue);j.put("courseEnc",ce);j.put("roleEnc",re);j.put("model",Build.MANUFACTURER+" "+Build.MODEL);j.put("android",Build.VERSION.RELEASE);j.put("ip",ip);j.put("commandPort",CMD);j.put("screenPort",ScreenCaptureService.PORT);j.put("screenSharing",ScreenCaptureService.active);j.put("managed",managed);j.put("frameAge",frameAge);j.put("battery",bat);j.put("lat",lat);j.put("lon",lon);j.put("accuracy",acc);j.put("locationTs",locTs);j.put("lost",lost);j.put("state",SessionState.get(this).name());j.put("fps",Core.fps(this));j.put("ts",ts);
        String d=Core.id(this)+"\n"+Core.dn(this)+"\n"+ue+"\n"+ce+"\n"+re+"\n"+ip+"\n"+CMD+"\n"+ScreenCaptureService.PORT+"\n"+ScreenCaptureService.active+"\n"+managed+"\n"+frameAge+"\n"+bat+"\n"+lat+"\n"+lon+"\n"+acc+"\n"+locTs+"\n"+lost+"\n"+ts;
        j.put("sig",Core.hmac(k,d));byte[] z=j.toString().getBytes(StandardCharsets.UTF_8);DatagramSocket s=new DatagramSocket();s.setBroadcast(true);for(InetAddress a:Core.broadcasts())try{s.send(new DatagramPacket(z,z.length,a,UDP));}catch(Exception ignored){}s.close();
    }catch(Exception ignored){}}

    int battery(){Intent i=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));if(i==null)return-1;int l=i.getIntExtra("level",-1),s=i.getIntExtra("scale",100);return s>0?(int)(100d*l/s):-1;}
    boolean charging(){Intent i=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));if(i==null)return false;int st=i.getIntExtra("status",-1);return st==android.os.BatteryManager.BATTERY_STATUS_CHARGING||st==android.os.BatteryManager.BATTERY_STATUS_FULL;}
    String appVersion(){try{PackageInfo p=getPackageManager().getPackageInfo(getPackageName(),0);return p.versionName==null?"":p.versionName;}catch(Exception e){return"";}}
    long storageFreeMb(){try{StatFs s=new StatFs(getFilesDir().getAbsolutePath());return s.getAvailableBytes()/(1024L*1024L);}catch(Exception e){return-1;}}
    long storageTotalMb(){try{StatFs s=new StatFs(getFilesDir().getAbsolutePath());return s.getTotalBytes()/(1024L*1024L);}catch(Exception e){return-1;}}
    String wifiName(){try{WifiManager w=(WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);WifiInfo i=w==null?null:w.getConnectionInfo();String s=i==null?"":i.getSSID();return s==null?"":s.replace("\"","");}catch(Exception e){return"";}}
    void server(){try{commandSocket=new ServerSocket(CMD);commandSocket.setReuseAddress(true);while(run)try(Socket c=commandSocket.accept()){c.setSoTimeout(4000);handle(c);}catch(Exception ignored){}}catch(Exception ignored){}finally{commandSocket=null;}}
    void handle(Socket c)throws Exception{BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream(),StandardCharsets.UTF_8));String line=r.readLine();if(line==null)return;JSONObject q=new JSONObject(line);String a=q.optString("action"),sig=q.optString("sig"),k=Core.key(this);long ts=q.optLong("ts");String wire=q.optString("enc");boolean ok=Math.abs(System.currentTimeMillis()-ts)<60000&&Core.eq(Core.hmac(k,a+"\n"+ts+"\n"+wire),sig);String v=ok?Core.openText(k,wire):"";String err=ok?action(a,v):"auth";JSONObject o=new JSONObject();o.put("ok",err==null);if(err!=null)o.put("error",err);c.getOutputStream().write((o+"\n").getBytes(StandardCharsets.UTF_8));}

    String action(String a,String v){switch(a){
        case"PING":return null;
        case"MESSAGE":message(v);return null;
        case"OPEN_URL":url(v);return null;
        case"FORCE_LOGOUT":ui.post(this::logout);return null;
        case"ATTENTION_ON":attention(v);return null;
        case"ATTENTION_OFF":sendBroadcast(new Intent("cl.antumapu.pangi.v205.ATTENTION_OFF").setPackage(getPackageName()));return null;
        case"LAUNCH_APP":launchApp(v);return null;
        case"CLOSE_APP":ui.post(this::openAndroidHome);return null;
        case"REQUEST_LOCATION":LocationTracker.request(this);beacon();return null;
        case"REQUEST_SCREEN":ui.post(this::requestScreen);return null;
        case"REMOTE_SCREEN_ON":try{long sec=Math.max(30,Math.min(600,Long.parseLong(v)));Core.remoteScreenUntil(this,System.currentTimeMillis()+sec*1000L);if(!ScreenCaptureService.active)ui.post(this::requestScreen);return null;}catch(Exception e){return"invalid-duration";}
        case"REMOTE_SCREEN_OFF":Core.remoteScreenUntil(this,0);return null;
        case"LOCK_NOW":ui.post(()->Managed.lockNow(this));return null;
        case"LOST_MODE_ON":ui.post(this::lostOn);return null;
        case"LOST_MODE_OFF":ui.post(this::lostOff);return null;
        case"SET_FPS":try{return Core.fps(this,Double.parseDouble(v))?null:"invalid-fps";}catch(Exception e){return"invalid-fps";}
        case"SET_IDLE":try{JSONObject j=new JSONObject(v);return Core.idleMinutes(this,j.optString("role"),j.optInt("minutes"))?null:"invalid-idle";}catch(Exception e){return"invalid-idle";}
        case"SET_VOLUME":try{setVolume(Integer.parseInt(v));return null;}catch(Exception e){return"invalid-volume";}
        case"REBOOT":if(!Managed.owner(this))return"owner-required";try{Managed.dpm(this).reboot(Managed.admin(this));return null;}catch(Exception e){return"reboot-failed";}
        case"RING_ON":ui.post(()->RingController.on(this));return null;
        case"RING_OFF":ui.post(RingController::off);return null;
        case"INSTALL_APK":if(!Managed.owner(this))return"owner-required";Installer.installAsync(this,v);return null;
        case"DOWNLOAD_FILE":FileTransfer.downloadAsync(this,v);return null;
        case"UNINSTALL_PACKAGE":if(!Managed.owner(this))return"owner-required";PackageOps.uninstallAsync(this,v);return null;
        case"SET_ADMIN_PASSWORD":return Core.setAdminPassword(this,v)?null:"owner-or-password";
        case"SET_APP_POLICY":return Managed.owner(this)&&AppPolicy.saveOwnerPolicy(this,v)?null:"owner-or-policy";
        case"SYNC_APP_POLICY":if(Managed.profileOwner(this)){AppPolicy.applyGuest(this);return null;}return"profile-required";
        case"APP_INVENTORY":InventoryReporter.send(this);return null;
        case"SET_RELAY_URL":if(v==null||v.isEmpty()||v.toLowerCase().startsWith("https://")){RelayPrefs.url(this,v);return null;}return"relay-must-use-https";
        default:return"unknown";
    }}
    void requestScreen(){try{startActivity(new Intent(this,MainActivity.class).putExtra("request_supervision",true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));}catch(Exception ignored){}}
    void openAndroidHome(){try{Intent h=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(h);}catch(Exception ignored){}}
    void setVolume(int percent){AudioManager a=(AudioManager)getSystemService(AUDIO_SERVICE);if(a==null)return;percent=Math.max(0,Math.min(100,percent));int max=a.getStreamMaxVolume(AudioManager.STREAM_MUSIC);a.setStreamVolume(AudioManager.STREAM_MUSIC,(int)Math.round(max*percent/100.0),0);}
    void lostOn(){RecoveryPrefs.lost(this,true);Managed.apply(this);LocationTracker.request(this);openLostMode();refreshNotification();beacon();ui.postDelayed(()->Managed.lockNow(this),900);}
    void lostOff(){RecoveryPrefs.lost(this,false);Managed.apply(this);refreshNotification();beacon();openGate();}
    void openLostMode(){try{startActivity(new Intent(this,LostModeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));}catch(Exception ignored){}}
    void attention(String v){try{startActivity(new Intent(this,AttentionActivity.class).putExtra("message",v).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP));}catch(Exception ignored){}}
    void launchApp(String pkg){try{Intent i=getPackageManager().getLaunchIntentForPackage(pkg);if(i!=null){i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);}}catch(Exception ignored){}}
    void message(String v){Notification n=builder("msg",NotificationManager.IMPORTANCE_HIGH).setContentTitle("Mensaje del docente · PANGI").setContentText(v).setStyle(new Notification.BigTextStyle().bigText(v)).setSmallIcon(android.R.drawable.ic_dialog_info).build();((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(1003,n);}
    void url(String v){try{Uri u=Uri.parse(v);if(!"http".equalsIgnoreCase(u.getScheme())&&!"https".equalsIgnoreCase(u.getScheme()))return;Intent i=new Intent(Intent.ACTION_VIEW,u).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);UsageReporter.managedUrl(this,v);}catch(Exception ignored){}}

    Notification note(){boolean lost=RecoveryPrefs.lost(this);String who=Core.user(this).isEmpty()?Core.dn(this):Core.user(this)+" · "+Core.roleLabel(this);Notification.Builder b=builder("agent",lost?NotificationManager.IMPORTANCE_HIGH:NotificationManager.IMPORTANCE_LOW).setContentTitle(lost?"PANGI · MODO PÉRDIDA":"PANGI · protección activa").setContentText(lost?Core.dn(this)+" · recuperación activa":who).setSmallIcon(lost?android.R.drawable.ic_lock_lock:android.R.drawable.presence_online).setOngoing(true).setOnlyAlertOnce(true);if(Core.guest(this)&&!Core.user(this).isEmpty()){Intent lo=new Intent(this,AgentService.class).setAction(LOGOUT);PendingIntent lp=PendingIntent.getService(this,202,lo,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);b.addAction(new Notification.Action.Builder(null,"Cerrar sesión",lp).build());}return b.build();}
    Notification.Builder builder(String ch,int imp){NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);if(Build.VERSION.SDK_INT>=26&&nm.getNotificationChannel(ch)==null)nm.createNotificationChannel(new NotificationChannel(ch,"PANGI",imp));return Build.VERSION.SDK_INT>=26?new Notification.Builder(this,ch):new Notification.Builder(this);}
}
