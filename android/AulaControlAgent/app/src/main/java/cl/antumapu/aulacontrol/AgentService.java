package cl.antumapu.aulacontrol;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.widget.Toast;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class AgentService extends Service{
 static final int UDP=45888,CMD=45901,SESSION_NOTIFICATION=1001;
 static final long WARN=30_000L;
 static final String KEEP="AC_KEEP",LOGOUT="AC_LOGOUT";
 volatile boolean run,warn;
 ScheduledExecutorService ex;
 ServerSocket commandSocket;
 Handler ui=new Handler(Looper.getMainLooper());
 Runnable autoLogout=()->logout();

 @Override public void onCreate(){
  super.onCreate();
  startForeground(SESSION_NOTIFICATION,note());
  run=true;
  ex=Executors.newScheduledThreadPool(2);
  ex.scheduleAtFixedRate(this::beacon,0,3,TimeUnit.SECONDS);
  ex.scheduleAtFixedRate(this::idle,5,5,TimeUnit.SECONDS);
  new Thread(this::server,"TabletEscolarCmd").start();
 }

 @Override public int onStartCommand(Intent i,int f,int id){
  if(i!=null&&KEEP.equals(i.getAction()))keep();
  else if(i!=null&&LOGOUT.equals(i.getAction()))logout();
  else refreshSessionNotification();
  return START_STICKY;
 }

 @Override public IBinder onBind(Intent i){return null;}

 @Override public void onDestroy(){
  run=false;
  if(ex!=null)ex.shutdownNow();
  ui.removeCallbacks(autoLogout);
  try{if(commandSocket!=null)commandSocket.close();}catch(Exception ignored){}
  super.onDestroy();
 }

 void idle(){
  if(Core.user(this).isEmpty()||Core.adminMode(this)||!Core.guest(this)){if(warn)cancelWarning();return;}

  // Una sesión temporal se crea antes de que Android permita autorizar
  // MediaProjection. La falta de captura sólo es crítica después de haber
  // confirmado al menos un cuadro real.
  if(!ScreenCaptureService.active){
   if(Core.supervisionStarted(this))ui.post(this::logout);
   else if(warn)cancelWarning();
   return;
  }

  // PACKAGE_USAGE_STATS es un permiso especial y puede no estar disponible en
  // cada usuario temporal. Para evitar cierres falsos mientras la tablet está
  // claramente en uso, cuando no hay acceso a UsageStats consideramos actividad
  // mientras la pantalla siga interactiva. Con UsageStats disponible se aplica
  // el límite exacto de 10/30 minutos sin almacenar historial.
  if(!Core.usageAccess(this)){
   try{
    PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE);
    if(pm!=null&&pm.isInteractive()){
     Core.touch(this);
     if(warn)cancelWarning();
     return;
    }
   }catch(Exception ignored){}
  }

  if(warn)return;
  long last=Core.activity(this);
  if(last>Core.last(this))Core.sp(this).edit().putLong("last",last).apply();
  if(System.currentTimeMillis()-last>=Core.idleLimit(this))ui.post(this::showWarn);
 }

 void showWarn(){
  if(warn||Core.user(this).isEmpty())return;
  warn=true;
  long deadline=System.currentTimeMillis()+WARN;
  Intent ki=new Intent(this,AgentService.class).setAction(KEEP),lo=new Intent(this,AgentService.class).setAction(LOGOUT);
  PendingIntent kp=PendingIntent.getService(this,201,ki,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE),
          lp=PendingIntent.getService(this,202,lo,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  String mins=Core.ROLE_TEACHER.equals(Core.role(this))?"30 minutos":"10 minutos";

  Notification.Builder b=builder("agent",NotificationManager.IMPORTANCE_HIGH)
          .setContentTitle("Tablet Escolar · cierre de sesión")
          .setContentText("Sin actividad durante "+mins)
          .setSmallIcon(android.R.drawable.ic_dialog_alert)
          .setOngoing(true)
          .setOnlyAlertOnce(true)
          .setWhen(deadline)
          .setUsesChronometer(true)
          .setContentIntent(openPanelIntent())
          .addAction(new Notification.Action.Builder(null,"Seguiré usando",kp).build())
          .addAction(new Notification.Action.Builder(null,"Cerrar sesión",lp).build());
  if(Build.VERSION.SDK_INT>=24)b.setChronometerCountDown(true);
  ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(SESSION_NOTIFICATION,b.build());
  ui.removeCallbacks(autoLogout);
  ui.postDelayed(autoLogout,WARN);
 }

 void keep(){
  if(Core.user(this).isEmpty()){cancelWarning();return;}
  Core.touch(this);cancelWarning();beacon();
  Toast.makeText(this,"La sesión continúa activa",Toast.LENGTH_SHORT).show();
 }

 void cancelWarning(){warn=false;ui.removeCallbacks(autoLogout);refreshSessionNotification();}
 void refreshSessionNotification(){try{((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(SESSION_NOTIFICATION,note());}catch(Exception ignored){}}

 void logout(){
  warn=false;ui.removeCallbacks(autoLogout);
  boolean guest=Core.guest(this);
  Core.clearIdentity(this);
  stopService(new Intent(this,ScreenCaptureService.class));
  beacon();
  if(guest){
   if(!SessionUsers.logoutGuest(this)){
    try{startActivity(new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP));}catch(Exception ignored){}
   }
  }else try{startActivity(new Intent(this,PrivacyGateActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP));}catch(Exception ignored){}
 }

 void beacon(){try{
  String k=Core.key(this);if(k.isEmpty())return;String ip=Core.ip();int bat=battery();long ts=System.currentTimeMillis();boolean managed=Managed.managed(this);long frameAge=ScreenCaptureService.lastFrameAt>0?Math.max(0,ts-ScreenCaptureService.lastFrameAt):-1;
  String ue=Core.sealText(k,Core.user(this)),ce=Core.sealText(k,Core.course(this)),re=Core.sealText(k,Core.role(this));
  JSONObject j=new JSONObject();j.put("type","AULACONTROL_BEACON");j.put("version",7);j.put("schoolTag",Core.sha(k).substring(0,12));j.put("deviceId",Core.id(this));j.put("deviceName",Core.dn(this));j.put("userEnc",ue);j.put("courseEnc",ce);j.put("roleEnc",re);j.put("model",Build.MANUFACTURER+" "+Build.MODEL);j.put("android",Build.VERSION.RELEASE);j.put("ip",ip);j.put("commandPort",CMD);j.put("screenPort",ScreenCaptureService.PORT);j.put("screenSharing",ScreenCaptureService.active);j.put("managed",managed);j.put("frameAge",frameAge);j.put("battery",bat);j.put("ts",ts);
  String d=Core.id(this)+"\n"+Core.dn(this)+"\n"+ue+"\n"+ce+"\n"+re+"\n"+ip+"\n"+CMD+"\n"+ScreenCaptureService.PORT+"\n"+ScreenCaptureService.active+"\n"+managed+"\n"+frameAge+"\n"+bat+"\n"+ts;
  j.put("sig",Core.hmac(k,d));byte[] z=j.toString().getBytes(StandardCharsets.UTF_8);DatagramSocket s=new DatagramSocket();s.setBroadcast(true);for(InetAddress a:Core.broadcasts())try{s.send(new DatagramPacket(z,z.length,a,UDP));}catch(Exception ignored){}s.close();
 }catch(Exception ignored){}}

 int battery(){Intent i=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));if(i==null)return-1;int l=i.getIntExtra("level",-1),s=i.getIntExtra("scale",100);return s>0?(int)(100d*l/s):-1;}

 void server(){try{commandSocket=new ServerSocket(CMD);commandSocket.setReuseAddress(true);while(run)try(Socket c=commandSocket.accept()){c.setSoTimeout(4000);handle(c);}catch(Exception ignored){}}catch(Exception ignored){}finally{commandSocket=null;}}

 void handle(Socket c)throws Exception{
  BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream(),StandardCharsets.UTF_8));String line=r.readLine();if(line==null)return;JSONObject q=new JSONObject(line);String a=q.optString("action"),sig=q.optString("sig"),k=Core.key(this);long ts=q.optLong("ts");String wire=q.optString("enc");boolean ok=Math.abs(System.currentTimeMillis()-ts)<60000&&Core.eq(Core.hmac(k,a+"\n"+ts+"\n"+wire),sig);String v=ok?Core.openText(k,wire):"";String err=ok?action(a,v):"auth";JSONObject o=new JSONObject();o.put("ok",err==null);if(err!=null)o.put("error",err);c.getOutputStream().write((o+"\n").getBytes(StandardCharsets.UTF_8));
 }

 String action(String a,String v){switch(a){case"PING":return null;case"MESSAGE":message(v);return null;case"OPEN_URL":url(v);return null;case"FORCE_LOGOUT":ui.post(this::logout);return null;case"ATTENTION_ON":attention(v);return null;case"ATTENTION_OFF":sendBroadcast(new Intent("cl.antumapu.aulacontrol.ATTENTION_OFF").setPackage(getPackageName()));return null;case"LAUNCH_APP":launchApp(v);return null;default:return"unknown";}}
 void attention(String v){try{startActivity(new Intent(this,AttentionActivity.class).putExtra("message",v).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP));}catch(Exception ignored){}}
 void launchApp(String pkg){try{Intent i=getPackageManager().getLaunchIntentForPackage(pkg);if(i!=null){i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);}}catch(Exception ignored){}}
 void message(String v){Notification n=builder("msg",NotificationManager.IMPORTANCE_HIGH).setContentTitle("Mensaje del docente · Tablet Escolar").setContentText(v).setStyle(new Notification.BigTextStyle().bigText(v)).setSmallIcon(android.R.drawable.ic_dialog_info).build();((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(1003,n);}
 void url(String v){try{Uri u=Uri.parse(v);if(!"http".equalsIgnoreCase(u.getScheme())&&!"https".equalsIgnoreCase(u.getScheme()))return;Intent i=new Intent(Intent.ACTION_VIEW,u);i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);}catch(Exception ignored){}}

 PendingIntent openPanelIntent(){Intent i=new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);return PendingIntent.getActivity(this,203,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}

 Notification note(){
  String who=Core.user(this).isEmpty()?Core.dn(this):Core.user(this)+" · "+Core.roleLabel(this);
  Notification.Builder b=builder("agent",NotificationManager.IMPORTANCE_LOW)
          .setContentTitle("Tablet Escolar · sesión protegida")
          .setContentText(who+" · toca para abrir el panel")
          .setSmallIcon(android.R.drawable.presence_online)
          .setContentIntent(openPanelIntent())
          .setOngoing(true)
          .setOnlyAlertOnce(true);
  if(Core.guest(this)&&!Core.user(this).isEmpty()){
   Intent lo=new Intent(this,AgentService.class).setAction(LOGOUT);
   PendingIntent lp=PendingIntent.getService(this,202,lo,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   b.addAction(new Notification.Action.Builder(null,"Cerrar sesión",lp).build());
  }
  return b.build();
 }

 Notification.Builder builder(String ch,int imp){NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);if(Build.VERSION.SDK_INT>=26&&nm.getNotificationChannel(ch)==null)nm.createNotificationChannel(new NotificationChannel(ch,"Tablet Escolar",imp));return Build.VERSION.SDK_INT>=26?new Notification.Builder(this,ch):new Notification.Builder(this);}
}
