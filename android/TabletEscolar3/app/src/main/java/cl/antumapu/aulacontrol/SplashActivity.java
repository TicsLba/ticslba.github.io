package cl.antumapu.aulacontrol;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SplashActivity extends Activity {
    @Override public void onCreate(Bundle b){
        super.onCreate(b);Ui.immersive(this);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER);root.setPadding(Ui.dp(this,28),Ui.dp(this,28),Ui.dp(this,28),Ui.dp(this,28));
        GradientDrawable bg=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Ui.NAVY,Color.rgb(36,61,122),Ui.COBALT});root.setBackground(bg);
        LinearLayout mark=new LinearLayout(this);mark.setGravity(Gravity.CENTER);mark.setBackground(Ui.shape(this,Color.WHITE,28));ImageView icon=new ImageView(this);icon.setImageResource(R.drawable.ic_tablet_school);mark.addView(icon,new LinearLayout.LayoutParams(Ui.dp(this,96),Ui.dp(this,96)));root.addView(mark,new LinearLayout.LayoutParams(Ui.dp(this,128),Ui.dp(this,128)));
        TextView title=Ui.text(this,"Tablet Escolar",34,true,Color.WHITE);title.setGravity(Gravity.CENTER);title.setPadding(0,Ui.dp(this,26),0,0);root.addView(title);
        TextView sub=Ui.text(this,"Gestión segura de dispositivos institucionales",14,false,Color.rgb(222,231,255));sub.setGravity(Gravity.CENTER);sub.setPadding(Ui.dp(this,18),Ui.dp(this,7),Ui.dp(this,18),0);root.addView(sub);
        TextView version=Ui.text(this,"3.0",12,true,Color.rgb(188,209,255));version.setGravity(Gravity.CENTER);version.setPadding(0,Ui.dp(this,18),0,0);root.addView(version);
        setContentView(root);
        mark.setAlpha(0f);mark.setScaleX(.78f);mark.setScaleY(.78f);title.setAlpha(0f);sub.setAlpha(0f);version.setAlpha(0f);
        mark.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(520).withEndAction(()->Ui.pulse(mark)).start();
        title.animate().alpha(1f).setStartDelay(280).setDuration(360).start();sub.animate().alpha(1f).setStartDelay(480).setDuration(360).start();version.animate().alpha(1f).setStartDelay(650).setDuration(300).start();
        new Handler(Looper.getMainLooper()).postDelayed(this::route,1450);
    }
    void route(){
        Class<?> target;
        if(PolicyManager.profileOwner(this))target=SessionSetupActivity.class;
        else if(!PolicyManager.owner(this)||!Store.configured(this))target=SetupActivity.class;
        else target=GateActivity.class;
        startActivity(new Intent(this,target).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));finish();
    }
}
