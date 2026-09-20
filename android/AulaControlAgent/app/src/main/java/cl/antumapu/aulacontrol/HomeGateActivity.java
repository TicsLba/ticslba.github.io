package cl.antumapu.aulacontrol;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserManager;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Synchronous owner-only boot/access guard.
 *
 * Android must always have a HOME target. To guarantee that the institutional
 * gate is the first usable surface after reboot, the Device Owner sets this
 * tiny activity as HOME only for the owner user. It is not an application
 * launcher: it exposes no apps and immediately forwards to Tablet Escolar.
 *
 * Managed student/teacher users never enable this component and therefore use
 * the manufacturer's ordinary Android launcher once their session is ACTIVE.
 */
public class HomeGateActivity extends Activity {
    private final Handler ui=new Handler(Looper.getMainLooper());
    private boolean forwarded;

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(18,35,67));
        getWindow().setNavigationBarColor(Color.rgb(18,35,67));
        render();
        Managed.enterGate(this);
        Managed.homeGuardOn(this);
        waitUntilUnlocked();
    }

    @Override protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        setIntent(intent);
        forwarded=false;
        Managed.enterGate(this);
        waitUntilUnlocked();
    }

    @Override protected void onResume(){
        super.onResume();
        if(!forwarded)waitUntilUnlocked();
    }

    @Override public void onBackPressed(){}

    private void waitUntilUnlocked(){
        if(isFinishing()||forwarded)return;
        UserManager um=(UserManager)getSystemService(USER_SERVICE);
        boolean unlocked=um==null||um.isUserUnlocked();
        if(!unlocked){
            ui.postDelayed(this::waitUntilUnlocked,250);
            return;
        }

        if(!Managed.owner(this)){
            Managed.homeGuardOff(this);
            finish();
            return;
        }

        // A reboot always closes maintenance mode and returns to the protected gate.
        Core.adminMode(this,false);
        SessionState.ownerGate(this);
        Managed.applyOwner(this);
        startAgent();
        forwardToGate();
    }

    private void startAgent(){
        try{
            Intent s=new Intent(this,AgentService.class);
            if(android.os.Build.VERSION.SDK_INT>=26)startForegroundService(s);else startService(s);
        }catch(Exception ignored){}
    }

    private void forwardToGate(){
        if(forwarded||isFinishing())return;
        forwarded=true;
        try{
            startActivity(new Intent(this,SplashActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));
        }catch(Exception ignored){
            forwarded=false;
            ui.postDelayed(this::forwardToGate,350);
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
        tile.setColor(Color.WHITE);tile.setCornerRadius(dp(25));
        mark.setBackground(tile);
        ImageView logo=new ImageView(this);
        logo.setImageResource(R.drawable.ic_tablet_school_mark);
        mark.addView(logo,new LinearLayout.LayoutParams(dp(72),dp(72)));
        root.addView(mark,new LinearLayout.LayoutParams(dp(98),dp(98)));

        TextView title=text("Tablet Escolar",30,true,Color.WHITE);
        LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-2,-2);tp.topMargin=dp(22);
        root.addView(title,tp);

        TextView line=text("Protegiendo el acceso institucional…",15,true,Color.rgb(221,231,255));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.topMargin=dp(9);
        root.addView(line,lp);

        TextView sub=text("El dispositivo se habilitará después de identificar al usuario.",13,false,Color.rgb(199,214,247));
        sub.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);
        sp.topMargin=dp(17);sp.leftMargin=dp(16);sp.rightMargin=dp(16);
        root.addView(sub,sp);

        setContentView(root);
        mark.setAlpha(0f);mark.setScaleX(.84f);mark.setScaleY(.84f);
        title.setAlpha(0f);line.setAlpha(0f);sub.setAlpha(0f);
        mark.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(300).start();
        title.animate().alpha(1f).setStartDelay(120).setDuration(260).start();
        line.animate().alpha(1f).setStartDelay(210).setDuration(260).start();
        sub.animate().alpha(1f).setStartDelay(290).setDuration(260).start();
    }

    private TextView text(String s,int size,boolean bold,int color){
        TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setGravity(Gravity.CENTER);
        if(bold)v.setTypeface(null,1);return v;
    }
    private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
}
