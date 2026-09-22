package cl.antumapu.pangi.v16;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PangiHomeActivity extends Activity {
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(17,24,32));
        getWindow().setNavigationBarColor(Color.rgb(17,24,32));

        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(28),dp(28),dp(28),dp(28));
        root.setBackgroundColor(Color.rgb(17,24,32));

        TextView mark=new TextView(this);
        mark.setText("◢  PANGI  ◣");
        mark.setTextSize(28);
        mark.setTextColor(Color.rgb(24,167,124));
        mark.setTypeface(null,1);
        mark.setGravity(Gravity.CENTER);
        root.addView(mark);

        TextView version=new TextView(this);
        version.setText("V16");
        version.setTextSize(13);
        version.setTextColor(Color.rgb(90,216,177));
        version.setGravity(Gravity.CENTER);
        version.setPadding(0,dp(8),0,dp(18));
        root.addView(version);

        TextView line=new TextView(this);
        line.setText("Preparando acceso institucional…");
        line.setTextSize(15);
        line.setTextColor(Color.rgb(245,247,247));
        line.setGravity(Gravity.CENTER);
        root.addView(line);
        setContentView(root);

        if(PangiPolicy.isManaged(this)){
            PangiPolicy.startGate(this);
            getWindow().getDecorView().postDelayed(this::forward,120);
        }else{
            getWindow().getDecorView().postDelayed(this::forward,120);
        }
    }

    private void forward(){
        try{
            startActivity(new Intent(this,MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));
        }catch(Exception ignored){}
        finish();
    }

    @Override public void onBackPressed(){}

    private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
}
