package cl.antumapu.aulacontrol;

import android.app.*;import android.content.*;import android.graphics.*;import android.graphics.drawable.GradientDrawable;import android.os.*;import android.view.*;import android.widget.*;

public class AttentionActivity extends Activity{
 BroadcastReceiver off=new BroadcastReceiver(){public void onReceive(Context c,Intent i){finish();}};
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setGravity(Gravity.CENTER);r.setPadding(dp(34),dp(34),dp(34),dp(34));r.setBackgroundColor(Color.rgb(8,56,38));TextView a=t("AulaControl",22,true);a.setTextColor(Color.rgb(205,235,219));r.addView(a);TextView h=t("Atención",40,true);h.setTextColor(Color.WHITE);h.setPadding(0,dp(18),0,dp(12));r.addView(h);String m=getIntent().getStringExtra("message");TextView x=t(m==null||m.isBlank()?"Mira al profesor y espera indicaciones.":m,21,false);x.setTextColor(Color.WHITE);x.setGravity(Gravity.CENTER);r.addView(x);TextView n=t("Esta pausa fue activada por la consola docente.",13,false);n.setTextColor(Color.rgb(205,235,219));n.setPadding(0,dp(28),0,0);r.addView(n);setContentView(r);registerReceiver(off,new IntentFilter("cl.antumapu.aulacontrol.ATTENTION_OFF"),RECEIVER_NOT_EXPORTED);}
 @Override protected void onDestroy(){try{unregisterReceiver(off);}catch(Exception ignored){}super.onDestroy();}
 @Override public void onBackPressed(){}
 TextView t(String s,int z,boolean b){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);if(b)v.setTypeface(null,1);return v;}int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
}
