package cl.antumapu.pangi.v205;

import android.app.*;import android.content.*;import android.graphics.*;import android.graphics.drawable.GradientDrawable;import android.os.*;import android.view.*;import android.widget.*;

public class AttentionActivity extends Activity{
 static final int NAVY=Color.rgb(23,43,77),BLUE=Color.rgb(49,89,255),TEAL=Color.rgb(32,199,164),MUTED=Color.rgb(194,207,231);
 BroadcastReceiver off=new BroadcastReceiver(){public void onReceive(Context c,Intent i){finish();}};
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);getWindow().setStatusBarColor(NAVY);getWindow().setNavigationBarColor(NAVY);LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setGravity(Gravity.CENTER);r.setPadding(dp(34),dp(34),dp(34),dp(34));r.setBackgroundColor(NAVY);
  LinearLayout mark=new LinearLayout(this);mark.setGravity(Gravity.CENTER);mark.setBackground(shape(BLUE,22));ImageView icon=new ImageView(this);icon.setImageResource(R.drawable.pangi_mark);mark.addView(icon,new LinearLayout.LayoutParams(dp(62),dp(62)));r.addView(mark,new LinearLayout.LayoutParams(dp(78),dp(78)));
  TextView a=t("PANGI",18,true);a.setTextColor(MUTED);a.setPadding(0,dp(18),0,0);r.addView(a);
  TextView h=t("Atención al docente",38,true);h.setTextColor(Color.WHITE);h.setGravity(Gravity.CENTER);h.setPadding(0,dp(10),0,dp(12));r.addView(h);
  String m=getIntent().getStringExtra("message");TextView x=t(m==null||m.isBlank()?"Mira al profesor y espera indicaciones.":m,21,false);x.setTextColor(Color.WHITE);x.setGravity(Gravity.CENTER);x.setLineSpacing(0,1.08f);r.addView(x);
  TextView n=t("Esta pausa fue activada desde la consola docente.",13,false);n.setTextColor(MUTED);n.setPadding(0,dp(28),0,0);r.addView(n);setContentView(r);registerReceiver(off,new IntentFilter("cl.antumapu.pangi.v205.ATTENTION_OFF"),RECEIVER_NOT_EXPORTED);}
 @Override protected void onDestroy(){try{unregisterReceiver(off);}catch(Exception ignored){}super.onDestroy();}
 @Override public void onBackPressed(){}
 TextView t(String s,int z,boolean b){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);if(b)v.setTypeface(null,1);return v;}int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}GradientDrawable shape(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));return g;}
}
