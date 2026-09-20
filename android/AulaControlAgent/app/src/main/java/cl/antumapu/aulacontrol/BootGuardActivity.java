package cl.antumapu.aulacontrol;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserManager;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Full-screen Direct-Boot gate used only while Android finishes booting.
 *
 * This activity is deliberately NOT a HOME/launcher component. The OEM launcher
 * remains Android's HOME at all times. Once the credential-protected user is
 * available, this gate hands off to the normal Tablet Escolar access flow.
 */
public class BootGuardActivity extends Activity {
    private final Handler ui=new Handler(Looper.getMainLooper());
    private boolean forwarded;

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(18,35,67));
        getWindow().setNavigationBarColor(Color.rgb(18,35,67));
        render();
        Managed.enterBootGate(this);
        waitUntilUnlocked();
    }

    @Override protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        setIntent(intent);
        forwarded=false;
        Managed.enterBootGate(this);
        waitUntilUnlocked();
    }

    @Override protected void onResume(){
        super.onResume();
        Managed.enterBootGate(this);
        if(!forwarded)waitUntilUnlocked();
    }

    @Override public void onBackPressed(){}

    private void waitUntilUnlocked(){
        if(isFinishing()||forwarded)return;
        UserManager um=(UserManager)getSystemService(USER_SERVICE);
        boolean unlocked=um==null||um.isUserUnlocked();
        if(!unlocked){
            ui.postDelayed(this::waitUntilUnlocked,180);
            return;
        }

        if(!Managed.owner(this)){
            finish();
            return;
        }

        try{Core.adminMode(this,false);}catch(Exception ignored){}
        try{SessionState.ownerGate(this);}catch(Exception ignored){}
        try{Managed.applyOwner(this);}catch(Exception ignored){}
        startAgent();
        cleanupOldSessions();
        forward();
    }

    private void startAgent(){
        try{
            Intent s=new Intent(this,AgentService.class);
            if(Build.VERSION.SDK_INT>=26)startForegroundService(s);else startService(s);
        }catch(Exception ignored){}
    }

    private void cleanupOldSessions(){
        new Thread(()->{
            try{SessionUsers.cleanupSecondaryUsers(this);}catch(Exception ignored){}
        },"TabletEscolarBootGuardCleanup").start();
    }

    private void forward(){
        if(forwarded||isFinishing())return;
        forwarded=true;
        BootGateScheduler.cancel(this);
        try{
            startActivity(new Intent(this,SplashActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));
        }catch(Exception ignored){
            forwarded=false;
            ui.postDelayed(this::forward,250);
            return;
        }
        finish();
    }

    private void render(){
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(30),dp(30),dp(30),dp(30));
        GradientDrawable bg=new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(14,31,61),Color.rgb(31,57,126),Color.rgb(49,89,255)});
        root.setBackground(bg);

        LinearLayout mark=new LinearLayout(this);
        mark.setGravity(Gravity.CENTER);
        GradientDrawable tile=new GradientDrawable();
        tile.setColor(Color.WHITE);
        tile.setCornerRadius(dp(25));
        mark.setBackground(tile);
        ImageView logo=new ImageView(this);
        logo.setImageResource(R.drawable.ic_tablet_school_mark);
        mark.addView(logo,new LinearLayout.LayoutParams(dp(72),dp(72)));
        root.addView(mark,new LinearLayout.LayoutParams(dp(98),dp(98)));

        TextView title=text("Tablet Escolar",30,true,Color.WHITE);
        LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-2,-2);
        tp.topMargin=dp(22);
        root.addView(title,tp);

        TextView line=text("Protegiendo el acceso institucional…",15,true,Color.rgb(221,231,255));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);
        lp.topMargin=dp(9);
        root.addView(line,lp);

        TextView sub=text("Preparando el dispositivo antes de habilitar Android.",13,false,Color.rgb(199,214,247));
        sub.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);
        sp.topMargin=dp(17);sp.leftMargin=dp(16);sp.rightMargin=dp(16);
        root.addView(sub,sp);

        setContentView(root);
        mark.setAlpha(0f);mark.setScaleX(.84f);mark.setScaleY(.84f);
        title.setAlpha(0f);line.setAlpha(0f);sub.setAlpha(0f);
        mark.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(260).start();
        title.animate().alpha(1f).setStartDelay(90).setDuration(220).start();
        line.animate().alpha(1f).setStartDelay(160).setDuration(220).start();
        sub.animate().alpha(1f).setStartDelay(230).setDuration(220).start();
    }

    private TextView text(String s,int size,boolean bold,int color){
        TextView v=new TextView(this);
        v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setGravity(Gravity.CENTER);
        if(bold)v.setTypeface(null,1);
        return v;
    }

    private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
}
