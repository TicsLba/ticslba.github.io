package cl.antumapu.aulacontrol;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Owner-user boot guard.
 *
 * This component is enabled only in the Device Owner user. Its sole purpose is
 * to be Android's synchronous HOME target during institutional access, so the
 * manufacturer launcher cannot become interactive before Tablet Escolar.
 *
 * Student and teacher temporary users do not use this HOME component: they keep
 * the manufacturer's ordinary launcher, which becomes available only after the
 * session has been prepared and supervision has been confirmed.
 */
public class HomeGateActivity extends Activity {
    private boolean forwarded;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(18,35,67));
        getWindow().setNavigationBarColor(Color.rgb(18,35,67));

        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(28),dp(28),dp(28),dp(28));
        root.setBackgroundColor(Color.rgb(18,35,67));

        TextView title=text("Tablet Escolar",28,true,Color.WHITE);
        root.addView(title);

        TextView status=text("Protegiendo el acceso al dispositivo…",15,true,Color.rgb(221,230,255));
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-2,-2);
        sp.topMargin=dp(12);
        root.addView(status,sp);

        TextView privacy=text("Android se habilitará después de identificar al usuario. Tablet Escolar no captura, guarda ni entrega contraseñas personales, PIN ni claves secretas de otras aplicaciones.",12,false,Color.rgb(197,211,242));
        privacy.setGravity(Gravity.CENTER);
        privacy.setLineSpacing(0,1.10f);
        LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,-2);
        pp.topMargin=dp(18);
        pp.leftMargin=dp(18);
        pp.rightMargin=dp(18);
        root.addView(privacy,pp);

        setContentView(root);

        if(!Managed.owner(this)||Core.adminMode(this)){
            Managed.homeGuardOff(this);
            finish();
            return;
        }

        SessionState.ownerGate(this);
        Managed.enterGate(this);
        root.post(this::forwardToGate);
    }

    @Override protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        setIntent(intent);
        forwarded=false;
        if(Managed.owner(this)&&!Core.adminMode(this)){
            SessionState.ownerGate(this);
            Managed.enterGate(this);
            forwardToGate();
        }else{
            Managed.homeGuardOff(this);
            finish();
        }
    }

    private void forwardToGate(){
        if(forwarded||isFinishing())return;
        forwarded=true;
        try{
            startActivity(new Intent(this,SplashActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));
        }catch(Exception ignored){}
        finish();
    }

    private TextView text(String s,int size,boolean bold,int color){
        TextView v=new TextView(this);
        v.setText(s);v.setTextSize(size);v.setTextColor(color);
        if(bold)v.setTypeface(null,1);
        return v;
    }

    private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
}
