package cl.antumapu.aulacontrol;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.hardware.display.*;
import android.media.*;
import android.media.projection.*;
import android.os.*;
import android.util.DisplayMetrics;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

public class ScreenCaptureService extends Service {
    public static final int PORT=45902;
    public static volatile boolean active=false;
    public static volatile long lastFrameAt=0,lastVisualChangeAt=0;
    MediaProjection mp;VirtualDisplay vd;ImageReader ir;volatile boolean run;
    AtomicReference<byte[]> frame=new AtomicReference<>();long last;long visualSig=Long.MIN_VALUE;
    ServerSocket screenSocket;HandlerThread captureThread;Handler captureHandler;

    @Override public int onStartCommand(Intent i,int f,int id){
        startForeground(2001,note());int rc=i==null?0:i.getIntExtra("rc",0);Intent data=null;
        if(i!=null)data=Build.VERSION.SDK_INT>=33?i.getParcelableExtra("data",Intent.class):(Intent)i.getParcelableExtra("data");
        if(rc!=0&&data!=null)start(rc,data);return START_NOT_STICKY;
    }
    @Override public IBinder onBind(Intent i){return null;}
    @Override public void onDestroy(){
        run=false;active=false;lastFrameAt=0;try{if(screenSocket!=null)screenSocket.close();}catch(Exception ignored){}
        try{if(vd!=null)vd.release();}catch(Exception ignored){}try{if(ir!=null)ir.close();}catch(Exception ignored){}try{if(mp!=null)mp.stop();}catch(Exception ignored){}if(captureThread!=null)captureThread.quitSafely();
        failClosed();super.onDestroy();
    }

    void start(int rc,Intent data){
        if(active)return;MediaProjectionManager m=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);mp=m.getMediaProjection(rc,data);if(mp==null){stopSelf();return;}
        active=run=true;lastFrameAt=0;lastVisualChangeAt=System.currentTimeMillis();
        captureThread=new HandlerThread("AulaMovilCapture",android.os.Process.THREAD_PRIORITY_BACKGROUND);captureThread.start();captureHandler=new Handler(captureThread.getLooper());
        mp.registerCallback(new MediaProjection.Callback(){@Override public void onStop(){active=false;lastFrameAt=0;failClosed();stopSelf();}},new Handler(Looper.getMainLooper()));
        DisplayMetrics dm=getResources().getDisplayMetrics();int target=dm.widthPixels>=1200?720:640;int w=Math.min(target,dm.widthPixels),h=Math.max(1,(int)(dm.heightPixels*(w/(double)dm.widthPixels)));
        ir=ImageReader.newInstance(w,h,PixelFormat.RGBA_8888,2);
        ir.setOnImageAvailableListener(r->{Image im=null;try{im=r.acquireLatestImage();if(im==null)return;long n=System.currentTimeMillis();if(n-last<Core.frameIntervalMs(this))return;last=n;Image.Plane p=im.getPlanes()[0];ByteBuffer b=p.getBuffer();int ps=p.getPixelStride(),rs=p.getRowStride(),pad=Math.max(0,rs-ps*w);Bitmap wide=Bitmap.createBitmap(w+pad/ps,h,Bitmap.Config.ARGB_8888);wide.copyPixelsFromBuffer(b);Bitmap crop=Bitmap.createBitmap(wide,0,0,w,h);long sig=signature(crop);if(sig!=visualSig){visualSig=sig;lastVisualChangeAt=n;}ByteArrayOutputStream out=new ByteArrayOutputStream(128*1024);crop.compress(Bitmap.CompressFormat.JPEG,60,out);byte[] jpg=out.toByteArray();frame.set(jpg);lastFrameAt=n;if(Core.remoteScreenRequested(this))RemoteRelay.uploadScreenAsync(this,jpg,n);wide.recycle();crop.recycle();}catch(Exception ignored){}finally{if(im!=null)im.close();}},captureHandler);
        vd=mp.createVirtualDisplay("AulaMovil",w,h,dm.densityDpi,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,ir.getSurface(),null,captureHandler);new Thread(this::server,"AulaMovilScreen").start();
    }

    long signature(Bitmap b){long s=1125899906842597L;int gx=6,gy=6;for(int y=0;y<gy;y++){int py=Math.min(b.getHeight()-1,(y*b.getHeight())/gy);for(int x=0;x<gx;x++){int px=Math.min(b.getWidth()-1,(x*b.getWidth())/gx);s=31*s+b.getPixel(px,py);}}return s;}

    void failClosed(){
        // Una interrupción de MediaProjection nunca destruye ni bloquea una
        // sesión ya activa. La consola la marcará "sin supervisión" y podrá
        // volver a solicitar el consentimiento oficial de Android.
        if(!Core.guest(this)||Core.user(this).isEmpty())return;
        Core.supervisionStarted(this,false);
    }

    void server(){try{screenSocket=new ServerSocket(PORT);screenSocket.setReuseAddress(true);while(run)try(Socket c=screenSocket.accept()){c.setSoTimeout(3000);serve(c);}catch(Exception ignored){}}catch(Exception ignored){}finally{screenSocket=null;}}
    void serve(Socket c)throws Exception{BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream(),StandardCharsets.US_ASCII));if(r.readLine()==null)return;String ts="",sig="",l;while((l=r.readLine())!=null&&!l.isEmpty()){int x=l.indexOf(':');if(x>0){String k=l.substring(0,x).trim(),v=l.substring(x+1).trim();if(k.equalsIgnoreCase("X-Timestamp"))ts=v;if(k.equalsIgnoreCase("X-Signature"))sig=v;}}boolean ok=false;String key=Core.key(this);try{long t=Long.parseLong(ts);ok=Math.abs(System.currentTimeMillis()-t)<60000&&Core.eq(Core.hmac(key,"SCREEN\n"+t),sig);}catch(Exception ignored){}byte[] b=frame.get();if(!ok){write(c,403,"text/plain","forbidden".getBytes(StandardCharsets.UTF_8));return;}if(!active||b==null){write(c,503,"text/plain","waiting".getBytes(StandardCharsets.UTF_8));return;}byte[] sealed=Core.seal(key,b);if(sealed.length==0){write(c,500,"text/plain","encryption".getBytes(StandardCharsets.UTF_8));return;}write(c,200,"application/octet-stream",sealed);}
    void write(Socket c,int code,String type,byte[] b)throws IOException{OutputStream o=c.getOutputStream();String h="HTTP/1.1 "+code+" OK\r\nContent-Type: "+type+"\r\nX-AulaControl-Encryption: aes-256-gcm\r\nContent-Length: "+b.length+"\r\nConnection: close\r\nCache-Control: no-store, no-cache, must-revalidate\r\nPragma: no-cache\r\n\r\n";o.write(h.getBytes(StandardCharsets.US_ASCII));o.write(b);o.flush();}
    Notification note(){NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);String c="capture";if(Build.VERSION.SDK_INT>=26&&nm.getNotificationChannel(c)==null)nm.createNotificationChannel(new NotificationChannel(c,"Aula Móvil · supervisión",NotificationManager.IMPORTANCE_LOW));Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,c):new Notification.Builder(this);Intent open=new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);PendingIntent pi=PendingIntent.getActivity(this,301,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);return b.setContentTitle("Aula Móvil · supervisión activa").setContentText("Supervisión en tiempo real, no vigilancia histórica").setSmallIcon(android.R.drawable.presence_online).setContentIntent(pi).setOngoing(true).setOnlyAlertOnce(true).build();}
}
