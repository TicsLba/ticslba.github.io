package cl.antumapu.aulacontrol;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.*;
import android.util.DisplayMetrics;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

public class CaptureService extends Service {
    static final int PORT=45902,NOTIFICATION_ID=3002;
    static volatile boolean active=false;
    static volatile long firstFrameAt=0,lastFrameAt=0;
    static volatile boolean intentionalStop=false;
    MediaProjection projection;VirtualDisplay display;ImageReader reader;HandlerThread captureThread;Handler captureHandler;ServerSocket server;volatile boolean run;AtomicReference<byte[]> latest=new AtomicReference<>();long lastEncoded;

    @Override public int onStartCommand(Intent i,int flags,int id){
        startForeground(NOTIFICATION_ID,notification());intentionalStop=false;
        int rc=i==null?0:i.getIntExtra("resultCode",0);Intent data=null;
        if(i!=null){if(Build.VERSION.SDK_INT>=33)data=i.getParcelableExtra("data",Intent.class);else data=(Intent)i.getParcelableExtra("data");}
        if(rc!=0&&data!=null)startProjection(rc,data);else stopSelf();return START_NOT_STICKY;
    }
    @Override public IBinder onBind(Intent i){return null;}
    static void stop(Context c,boolean intentional){intentionalStop=intentional;try{c.stopService(new Intent(c,CaptureService.class));}catch(Exception ignored){}}

    void startProjection(int resultCode,Intent data){
        if(active)return;
        try{
            MediaProjectionManager m=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);projection=m.getMediaProjection(resultCode,data);if(projection==null){stopSelf();return;}
            active=true;run=true;firstFrameAt=0;lastFrameAt=0;
            captureThread=new HandlerThread("TabletEscolarCapture",android.os.Process.THREAD_PRIORITY_BACKGROUND);captureThread.start();captureHandler=new Handler(captureThread.getLooper());
            projection.registerCallback(new MediaProjection.Callback(){@Override public void onStop(){active=false;run=false;firstFrameAt=0;lastFrameAt=0;if(!intentionalStop)failClosed();stopSelf();}},new Handler(Looper.getMainLooper()));
            DisplayMetrics dm=getResources().getDisplayMetrics();int w=Math.min(dm.widthPixels,720);int h=Math.max(1,(int)(dm.heightPixels*(w/(double)Math.max(1,dm.widthPixels))));reader=ImageReader.newInstance(w,h,PixelFormat.RGBA_8888,2);
            reader.setOnImageAvailableListener(r->encodeLatest(r,w,h),captureHandler);
            display=projection.createVirtualDisplay("TabletEscolarLive",w,h,dm.densityDpi,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader.getSurface(),null,captureHandler);
            new Thread(this::serve,"TabletEscolarScreenServer").start();
        }catch(Exception e){active=false;run=false;stopSelf();}
    }

    void encodeLatest(ImageReader r,int w,int h){
        Image im=null;try{im=r.acquireLatestImage();if(im==null)return;long now=System.currentTimeMillis();if(now-lastEncoded<450)return;lastEncoded=now;Image.Plane p=im.getPlanes()[0];ByteBuffer b=p.getBuffer();int ps=p.getPixelStride(),rs=p.getRowStride(),pad=Math.max(0,rs-ps*w);Bitmap wide=Bitmap.createBitmap(w+pad/ps,h,Bitmap.Config.ARGB_8888);wide.copyPixelsFromBuffer(b);Bitmap crop=Bitmap.createBitmap(wide,0,0,w,h);ByteArrayOutputStream out=new ByteArrayOutputStream(160*1024);crop.compress(Bitmap.CompressFormat.JPEG,58,out);latest.set(out.toByteArray());lastFrameAt=System.currentTimeMillis();if(firstFrameAt==0)firstFrameAt=lastFrameAt;wide.recycle();crop.recycle();}catch(Exception ignored){}finally{if(im!=null)im.close();}
    }

    void failClosed(){
        if(!PolicyManager.profileOwner(this)||!Store.supervisionStarted(this))return;
        Store.state(this,Store.STATE_CLOSING);Store.clearGuestIdentity(this);SessionUsers.logoutGuest(this);
    }

    void serve(){
        try{server=new ServerSocket(PORT);server.setReuseAddress(true);while(run)try(Socket c=server.accept()){c.setSoTimeout(3000);serveOne(c);}catch(Exception ignored){}}catch(Exception ignored){}finally{server=null;}
    }
    void serveOne(Socket c)throws Exception{
        BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream(),StandardCharsets.US_ASCII));if(r.readLine()==null)return;String ts="",sig="",line;while((line=r.readLine())!=null&&!line.isEmpty()){int x=line.indexOf(':');if(x>0){String k=line.substring(0,x).trim(),v=line.substring(x+1).trim();if(k.equalsIgnoreCase("X-Timestamp"))ts=v;if(k.equalsIgnoreCase("X-Signature"))sig=v;}}
        boolean ok=false;String key=Store.technicalKey(this);try{long t=Long.parseLong(ts);ok=Math.abs(System.currentTimeMillis()-t)<60000&&Crypto.equal(Crypto.hmac(key,"SCREEN\n"+t),sig);}catch(Exception ignored){}
        byte[] b=latest.get();if(!ok){write(c,403,"text/plain","forbidden".getBytes(StandardCharsets.UTF_8));return;}if(!active||b==null){write(c,503,"text/plain","waiting".getBytes(StandardCharsets.UTF_8));return;}byte[] sealed=Crypto.seal(key,b);if(sealed.length==0){write(c,500,"text/plain","encryption".getBytes(StandardCharsets.UTF_8));return;}write(c,200,"application/octet-stream",sealed);
    }
    void write(Socket c,int code,String type,byte[] b)throws IOException{OutputStream o=c.getOutputStream();String h="HTTP/1.1 "+code+" OK\r\nContent-Type: "+type+"\r\nX-TabletEscolar-Encryption: aes-256-gcm\r\nContent-Length: "+b.length+"\r\nConnection: close\r\nCache-Control: no-store\r\n\r\n";o.write(h.getBytes(StandardCharsets.US_ASCII));o.write(b);o.flush();}

    @Override public void onDestroy(){
        run=false;active=false;firstFrameAt=0;lastFrameAt=0;try{if(server!=null)server.close();}catch(Exception ignored){}try{if(display!=null)display.release();}catch(Exception ignored){}try{if(reader!=null)reader.close();}catch(Exception ignored){}try{if(projection!=null)projection.stop();}catch(Exception ignored){}if(captureThread!=null)captureThread.quitSafely();boolean fail=!intentionalStop&&Store.supervisionStarted(this);intentionalStop=false;if(fail)failClosed();super.onDestroy();
    }

    Notification notification(){NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);String ch="tablet_escolar_capture";if(Build.VERSION.SDK_INT>=26&&nm.getNotificationChannel(ch)==null)nm.createNotificationChannel(new NotificationChannel(ch,"Tablet Escolar · supervisión",NotificationManager.IMPORTANCE_LOW));Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,ch):new Notification.Builder(this);Intent open=new Intent(this,SessionSetupActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);PendingIntent pi=PendingIntent.getActivity(this,401,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);return b.setContentTitle("Tablet Escolar · supervisión activa").setContentText("Pantalla en vivo durante la sesión temporal").setSmallIcon(android.R.drawable.presence_online).setContentIntent(pi).setOngoing(true).setOnlyAlertOnce(true).build();}
}
