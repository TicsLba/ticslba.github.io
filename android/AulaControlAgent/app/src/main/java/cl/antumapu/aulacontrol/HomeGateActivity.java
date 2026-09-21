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
 * Synchronous owner-only institutional access guard.
 *
 * Aula Móvil makes this the HOME target while access is gated. In the owner
 * user it protects identification and boot; in a temporary student/teacher
 * user it remains HOME until visible supervision is active. Only then is HOME
 * released to the manufacturer's normal launcher.
 */
public class HomeGateActivity extends Activity {
    private final Handler ui=new Handler(Looper.getMainLooper());
    private boolean forwarded;

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(18,35,67));
        getWindow().setNavigationBarColor(Color.rgb(18,35,67));
        render();

        if(Managed.profileOwner(this)){
            Managed.applyGuest(this);
            Managed.homeGuardOn(this);
            Managed.enterGate(this);
            forwardToGate();
            return;
        }
        if(!Managed.owner(this)){
            Managed.homeGuardOff(this);
            finish();
            return;
        }

        // This path is safe before the credential-encrypted user store opens.
        Managed.applyBootOwner(this);
        Managed.enterGate(this);
        waitUntilUnlocked();
    }

    @Override protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        setIntent(intent);
        forwarded=false;
        if(Managed.profileOwner(this)){
            Managed.applyGuest(this);
            Managed.homeGuardOn(this);
            Managed.enterGate(this);
            forwardToGate();
            return;
        }
        if(!Managed.owner(this)){
            Managed.homeGuardOff(this);
            finish();
            return;
        }
        Managed.applyBootOwner(this);
        Managed.enterGate(this);
        waitUntilUnlocked();
    }

    @Override protected void onResume(){
        super.onResume();
        if(forwarded)return;
        if(Managed.profileOwner(this)){
            Managed.applyGuest(this);
            Managed.homeGuardOn(this);
            Managed.enterGate(this);
            forwardToGate();
            return;
        }
        if(Managed.owner(this)){
            Managed.applyBootOwner(this);
            Managed.enterGate(this);
            waitUntilUnlocked();
        }
    }

    @Override protected void onDestroy(){
        ui.removeCallbacksAndMessages(null);
        super.onDestroy();
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
            Managed.homeGuardOff(this);
            finish();
            return;
        }

        // Maintenance never survives a reboot.
        Core.adminMode(this,false);
        SessionState.ownerGate(this);
        Managed.applyOwner(this);

        // A reboot invalidates any old shared session. Remove secondary users
        // outside the UI thread while the owner gate stays visible.
        new Thread(()->SessionUsers.cleanupSecondaryUsers(this),
                "AulaMovilBootCleanup").start();

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
            ui.postDelayed(this::forwardToGate,250);
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
        logo.setImageResource(R.drawable.lba_logo);
        mark.addView(logo,new LinearLayout.LayoutParams(dp(72),dp(72)));
        root.addView(mark,new LinearLayout.LayoutParams(dp(98),dp(98)));

        TextView title=text("Aula Móvil",30,true,Color.WHITE);
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

        TextView boot=text("Arranque protegido · Aula Móvil 10.1",11,true,Color.rgb(173,197,244));
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-2,-2);bp.topMargin=dp(19);
        root.addView(boot,bp);

        setContentView(root);
        mark.setAlpha(0f);mark.setScaleX(.84f);mark.setScaleY(.84f);
        title.setAlpha(0f);line.setAlpha(0f);sub.setAlpha(0f);boot.setAlpha(0f);
        mark.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(260).start();
        title.animate().alpha(1f).setStartDelay(90).setDuration(220).start();
        line.animate().alpha(1f).setStartDelay(160).setDuration(220).start();
        sub.animate().alpha(1f).setStartDelay(220).setDuration(220).start();
        boot.animate().alpha(1f).setStartDelay(300).setDuration(220).start();
    }

    private TextView text(String s,int size,boolean bold,int color){
        TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setGravity(Gravity.CENTER);
        if(bold)v.setTypeface(null,1);return v;
    }
    private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
}
