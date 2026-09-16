package cl.antumapu.aulacontrol;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SessionWarningActivity extends Activity {
    private static final int NAVY=Color.rgb(23,43,77), COBALT=Color.rgb(49,89,255), CORAL=Color.rgb(244,91,105), MUTED=Color.rgb(102,112,133);
    private CountDownTimer timer;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(NAVY);
        getWindow().setNavigationBarColor(NAVY);
        render();
    }

    private void render(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER);root.setPadding(dp(24),dp(24),dp(24),dp(24));root.setBackgroundColor(Color.rgb(241,245,253));
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(26),dp(25),dp(26),dp(24));card.setBackground(round(Color.WHITE,24));
        TextView chip=t("PROTECCIÓN DE SESIÓN",12,true,CORAL);card.addView(chip);
        TextView title=t("¿Sigues usando la tablet?",27,true,NAVY);title.setPadding(0,dp(13),0,dp(7));card.addView(title);
        TextView body=t("Cerraremos esta sesión para proteger tus datos si no confirmas que sigues aquí.",15,false,MUTED);card.addView(body);
        TextView count=t("30",52,true,COBALT);count.setGravity(Gravity.CENTER);count.setPadding(0,dp(18),0,dp(12));card.addView(count);
        Button keep=button("Seguiré usando",COBALT,Color.WHITE),close=button("Cerrar sesión",Color.rgb(255,239,241),CORAL);card.addView(keep);card.addView(close);
        root.addView(card,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));setContentView(root);
        keep.setOnClickListener(v->{send(IntentAction.KEEP);finish();});
        close.setOnClickListener(v->{send(IntentAction.LOGOUT);finish();});
        timer=new CountDownTimer(30_000,1000){public void onTick(long ms){count.setText(String.valueOf(Math.max(0,(ms+999)/1000)));}public void onFinish(){send(IntentAction.LOGOUT);finish();}};timer.start();
        card.setAlpha(0f);card.setTranslationY(dp(28));card.animate().alpha(1f).translationY(0).setDuration(320).start();
    }

    private void send(String action){try{startService(new Intent(this,AgentService.class).setAction(action));}catch(Exception ignored){}}
    @Override protected void onDestroy(){if(timer!=null)timer.cancel();super.onDestroy();}
    @Override public void onBackPressed(){}

    private static final class IntentAction{static final String KEEP="AC_KEEP",LOGOUT="AC_LOGOUT";}
    private Button button(String s,int fill,int fg){Button b=new Button(this);b.setText(s);b.setTextColor(fg);b.setTextSize(16);b.setTypeface(null,1);b.setAllCaps(false);b.setBackground(round(fill,15));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(6),0,dp(6));b.setLayoutParams(p);return b;}
    private TextView t(String s,int z,boolean bold,int color){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(color);if(bold)v.setTypeface(null,1);return v;}
    private GradientDrawable round(int color,int r){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(r));return g;}
    private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
}
