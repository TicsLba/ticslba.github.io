package cl.antumapu.aulacontrol;

import android.app.*;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import android.graphics.drawable.GradientDrawable;

public class LostModeActivity extends Activity {
    static final int NAVY=Color.rgb(23,43,77),BLUE=Color.rgb(49,89,255),WHITE=Color.WHITE,MUTED=Color.rgb(102,112,133),BG=Color.rgb(244,247,251);
    int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}    
    GradientDrawable shape(int fill,int radius){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(radius));return g;}
    TextView t(String s,int z,boolean b,int c){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(c);if(b)v.setTypeface(null,1);return v;}

    @Override public void onCreate(Bundle b){super.onCreate(b);showUi();}
    @Override protected void onResume(){super.onResume();if(!RecoveryPrefs.lost(this)){try{stopLockTask();}catch(Exception ignored){}finish();return;}showUi();try{startLockTask();}catch(Exception ignored){}}
    @Override public void onBackPressed(){}

    void showUi(){
        getWindow().setStatusBarColor(NAVY);getWindow().setNavigationBarColor(NAVY);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER);root.setPadding(dp(28),dp(28),dp(28),dp(28));root.setBackgroundColor(BG);
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setGravity(Gravity.CENTER_HORIZONTAL);card.setPadding(dp(26),dp(28),dp(26),dp(28));card.setBackground(shape(WHITE,24));
        TextView badge=t("TABLET ESCOLAR",12,true,BLUE);badge.setLetterSpacing(.12f);card.addView(badge);
        TextView title=t("Dispositivo institucional bloqueado",28,true,NAVY);title.setGravity(Gravity.CENTER);title.setPadding(0,dp(16),0,dp(12));card.addView(title);
        TextView msg=t("Este equipo fue marcado para recuperación por su establecimiento. No continúes utilizándolo. Entrégalo a personal autorizado.",16,false,MUTED);msg.setGravity(Gravity.CENTER);msg.setLineSpacing(0,1.15f);card.addView(msg);
        TextView id=t(Core.dn(this)+"\n"+Core.id(this),16,true,NAVY);id.setGravity(Gravity.CENTER);id.setPadding(0,dp(22),0,dp(10));card.addView(id);
        TextView foot=t("La ubicación institucional puede reportarse mientras el equipo tenga servicios de ubicación e Internet disponibles.",12,false,MUTED);foot.setGravity(Gravity.CENTER);foot.setPadding(0,dp(12),0,0);card.addView(foot);
        root.addView(card,new LinearLayout.LayoutParams(-1,-2));setContentView(root);
    }
}
