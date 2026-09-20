package cl.antumapu.aulacontrol;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserManager;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Direct-Boot access gate. This is a normal Activity, never a HOME handler.
 *
 * It can appear as soon as Android permits Device Owner UI, waits until
 * credential-protected storage is unlocked, reapplies owner policy once, then
 * hands off to the normal Aula Móvil splash/gate.
 */
public class BootGateActivity extends Activity {
    private static final int NAVY=Color.rgb(32,32,32), COBALT=Color.rgb(0,103,192),
            TEAL=Color.rgb(0,120,212), SOFT=Color.rgb(96,94,92);
    private final Handler ui=new Handler(Looper.getMainLooper());
    private boolean preparing, forwarded;

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        BootGateScheduler.cancel(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(NAVY);
        getWindow().setNavigationBarColor(NAVY);
        setContentView(screen());
        enterBootLock();
        ui.post(this::advance);
    }

    @Override protected void onResume(){
        super.onResume();
        enterBootLock();
        ui.post(this::advance);
    }

    @Override public void onBackPressed(){ /* institutional gate: no back navigation */ }

    private void advance(){
        if(forwarded||isFinishing())return;
        if(!Managed.owner(this)){finish();return;}

        UserManager um=(UserManager)getSystemService(USER_SERVICE);
        boolean unlocked=Build.VERSION.SDK_INT<24||um==null||um.isUserUnlocked();
        if(!unlocked){
            ui.postDelayed(this::advance,180);
            return;
        }
        if(preparing)return;
        preparing=true;

        new Thread(()->{
            try{Core.adminMode(this,false);}catch(Exception ignored){}
            try{SessionState.ownerGate(this);}catch(Exception ignored){}
            try{Managed.applyOwner(this);}catch(Exception ignored){}
            runOnUiThread(this::forward);
        },"AulaMovilBootGate").start();
    }

    private void forward(){
        if(forwarded||isFinishing())return;
        forwarded=true;
        BootGateScheduler.cancel(this);
        try{
            startActivity(new Intent(this,SplashActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));
        }catch(Exception ignored){}
        finish();
        overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
    }

    private void enterBootLock(){
        if(!Managed.owner(this))return;
        try{
            DevicePolicyManager d=Managed.dpm(this);
            ComponentName a=Managed.admin(this);
            d.setLockTaskPackages(a,new String[]{getPackageName()});
            if(d.isLockTaskPermitted(getPackageName()))startLockTask();
        }catch(Exception ignored){}
    }

    private LinearLayout screen(){
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(30),dp(30),dp(30),dp(30));
        GradientDrawable bg=new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(243,243,243),Color.rgb(250,250,250),Color.WHITE});
        root.setBackground(bg);

        TextView mark=text("■",30,true,COBALT);root.addView(mark);
        TextView title=text("Aula Móvil",31,true,NAVY);
        LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-2,-2);tp.topMargin=dp(14);root.addView(title,tp);
        TextView sub=text("Preparando acceso institucional…",15,true,SOFT);
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-2,-2);sp.topMargin=dp(8);root.addView(sub,sp);
        TextView msg=text("La identificación institucional se abrirá antes de habilitar el uso normal de Android.",13,false,Color.rgb(70,70,70));
        msg.setGravity(Gravity.CENTER);msg.setLineSpacing(0,1.10f);
        LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(-1,-2);mp.topMargin=dp(18);mp.leftMargin=dp(16);mp.rightMargin=dp(16);root.addView(msg,mp);

        mark.setAlpha(.35f);
        mark.animate().alpha(1f).setDuration(340).withEndAction(()->
                mark.animate().alpha(.35f).setDuration(430).start()).start();
        return root;
    }

    private TextView text(String s,int size,boolean bold,int color){
        TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setGravity(Gravity.CENTER);
        if(bold)v.setTypeface(null,1);return v;
    }
    private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
}
