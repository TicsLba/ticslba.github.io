package cl.antumapu.aulacontrol;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * Owner-user boot guard.
 *
 * This is not the launcher used during student/teacher sessions. It exists only
 * in the Owner user so Android has a synchronous HOME target at boot. Once the
 * temporary managed user becomes active this component remains disabled there,
 * and Android uses the manufacturer's normal launcher.
 */
public class HomeGateActivity extends Activity {
    private boolean forwarded;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(18,35,67));
        getWindow().setNavigationBarColor(Color.rgb(18,35,67));

        FrameLayout root=new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(18,35,67));
        TextView title=new TextView(this);
        title.setText("Tablet Escolar");
        title.setTextColor(Color.WHITE);
        title.setTextSize(25);
        title.setTypeface(null,1);
        title.setGravity(Gravity.CENTER);
        root.addView(title,new FrameLayout.LayoutParams(-1,-1));
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
}
