package cl.antumapu.aulacontrol;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;

final class RingController {
    private static final Handler H=new Handler(Looper.getMainLooper());
    private static ToneGenerator tone;
    private static boolean active;
    private static final Runnable LOOP=new Runnable(){
        @Override public void run(){
            synchronized(RingController.class){
                if(!active)return;
                try{
                    if(tone==null)tone=new ToneGenerator(AudioManager.STREAM_ALARM,100);
                    tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD,2200);
                }catch(Exception ignored){}
                H.postDelayed(this,2600);
            }
        }
    };
    private RingController(){}

    static synchronized void on(Context c){
        try{
            AudioManager a=(AudioManager)c.getSystemService(Context.AUDIO_SERVICE);
            if(a!=null)a.setStreamVolume(AudioManager.STREAM_ALARM,a.getStreamMaxVolume(AudioManager.STREAM_ALARM),0);
        }catch(Exception ignored){}
        if(active)return;
        active=true;
        H.removeCallbacks(LOOP);
        H.post(LOOP);
    }

    static synchronized void off(){
        active=false;
        H.removeCallbacks(LOOP);
        try{if(tone!=null){tone.stopTone();tone.release();}}catch(Exception ignored){}
        tone=null;
    }
}
