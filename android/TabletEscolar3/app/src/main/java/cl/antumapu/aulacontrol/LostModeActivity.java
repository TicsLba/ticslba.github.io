package cl.antumapu.aulacontrol;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class LostModeActivity extends Activity {
    boolean registered;BroadcastReceiver off=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){finish();}};
    @Override public void onCreate(Bundle b){super.onCreate(b);Ui.immersive(this);lock();showLost();try{IntentFilter f=new IntentFilter("cl.antumapu.aulacontrol.LOST_MODE_OFF");if(android.os.Build.VERSION.SDK_INT>=33)registerReceiver(off,f,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(off,f);registered=true;}catch(Exception ignored){}}
    void lock(){if(PolicyManager.owner(this))PolicyManager.startGateLock(this);else if(PolicyManager.profileOwner(this))PolicyManager.startSessionLock(this);}
    void showLost(){LinearLayout r=Ui.root(this);r.setGravity(Gravity.CENTER_VERTICAL);Ui.header(this,r,"Dispositivo en modo perdido","Esta tablet institucional fue marcada para recuperación. La ubicación del dispositivo puede reportarse a la administración autorizada.");LinearLayout c=Ui.card(this);c.addView(Ui.chip(this,"RECUPERACIÓN ACTIVA",Ui.CORAL,Color.rgb(255,236,239)));TextView icon=Ui.text(this,"⌖",58,true,Ui.CORAL);icon.setGravity(Gravity.CENTER);icon.setPadding(0,Ui.dp(this,18),0,Ui.dp(this,8));c.addView(icon);TextView dn=Ui.text(this,Store.deviceName(this),24,true,Ui.INK);dn.setGravity(Gravity.CENTER);c.addView(dn);TextView id=Ui.text(this,Store.deviceId(this),14,true,Ui.MUTED);id.setGravity(Gravity.CENTER);id.setPadding(0,Ui.dp(this,5),0,Ui.dp(this,18));c.addView(id);TextView note=Ui.text(this,"Si encontraste este equipo, entrégalo al establecimiento educacional responsable.",14,false,Ui.MUTED);note.setGravity(Gravity.CENTER);c.addView(note);r.addView(c);setContentView(Ui.scroll(this,r));Ui.animateIn(c,60);Ui.pulse(c);}
    @Override public void onBackPressed(){}
    @Override protected void onDestroy(){if(registered)try{unregisterReceiver(off);}catch(Exception ignored){}super.onDestroy();}
}
