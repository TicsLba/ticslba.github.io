package cl.antumapu.aulacontrol;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AttentionActivity extends Activity {
    Handler ui=new Handler(Looper.getMainLooper());long deadline;TextView countdown;boolean registered;
    BroadcastReceiver close=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){finish();}};
    @Override public void onCreate(Bundle b){super.onCreate(b);Ui.immersive(this);deadline=getIntent().getLongExtra("deadline",0);String message=getIntent().getStringExtra("message");if(deadline>0)warning();else teacherMessage(message==null?"Atención solicitada por el docente":message);register();}
    void register(){try{IntentFilter f=new IntentFilter();f.addAction("cl.antumapu.aulacontrol.WARNING_CLOSED");f.addAction("cl.antumapu.aulacontrol.ATTENTION_OFF");if(android.os.Build.VERSION.SDK_INT>=33)registerReceiver(close,f,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(close,f);registered=true;}catch(Exception ignored){}}
    @Override protected void onDestroy(){ui.removeCallbacksAndMessages(null);if(registered)try{unregisterReceiver(close);}catch(Exception ignored){}super.onDestroy();}
    @Override public void onBackPressed(){}

    void warning(){
        LinearLayout r=Ui.root(this);r.setGravity(Gravity.CENTER_VERTICAL);Ui.header(this,r,"¿Sigues usando la tablet?","Cerraremos esta sesión para proteger tus datos si no confirmas que continúas trabajando.");LinearLayout c=Ui.card(this);c.addView(Ui.chip(this,"CIERRE AUTOMÁTICO",Ui.CORAL,Color.rgb(255,236,239)));countdown=Ui.text(this,"30",52,true,Ui.CORAL);countdown.setGravity(Gravity.CENTER);countdown.setPadding(0,Ui.dp(this,16),0,Ui.dp(this,6));c.addView(countdown);TextView sec=Ui.text(this,"segundos",14,true,Ui.MUTED);sec.setGravity(Gravity.CENTER);c.addView(sec);r.addView(c);Button keep=Ui.primary(this,"Seguiré usando"),out=Ui.danger(this,"Cerrar sesión ahora");keep.setOnClickListener(v->{startService(new Intent(this,AgentService.class).setAction(AgentService.ACTION_KEEP));finish();});out.setOnClickListener(v->{startService(new Intent(this,AgentService.class).setAction(AgentService.ACTION_LOGOUT));finish();});r.addView(keep);r.addView(out);setContentView(Ui.scroll(this,r));Ui.animateIn(c,40);Ui.animateIn(keep,120);Ui.animateIn(out,180);tick();
    }
    void tick(){long left=Math.max(0,(deadline-System.currentTimeMillis()+999)/1000);if(countdown!=null)countdown.setText(Long.toString(left));if(left<=0){finish();return;}ui.postDelayed(this::tick,250);}
    void teacherMessage(String message){LinearLayout r=Ui.root(this);r.setGravity(Gravity.CENTER_VERTICAL);Ui.header(this,r,"Atención del docente","La consola de aula envió un mensaje a este dispositivo.");LinearLayout c=Ui.card(this);c.addView(Ui.chip(this,"MENSAJE",Ui.COBALT,Color.rgb(232,237,255)));TextView m=Ui.text(this,message,24,true,Ui.INK);m.setGravity(Gravity.CENTER);m.setPadding(Ui.dp(this,4),Ui.dp(this,24),Ui.dp(this,4),Ui.dp(this,24));c.addView(m);r.addView(c);Button ok=Ui.primary(this,"Entendido");ok.setOnClickListener(v->finish());r.addView(ok);setContentView(Ui.scroll(this,r));Ui.animateIn(c,50);Ui.animateIn(ok,160);Ui.pulse(c);}
}
