package cl.antumapu.aulacontrol;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

public class AdminReceiver extends DeviceAdminReceiver {
    @Override public void onEnabled(Context c,Intent i){super.onEnabled(c,i);if(PolicyManager.profileOwner(c))Store.setupGuest(c,i);launch(c);}
    @Override public void onProfileProvisioningComplete(Context c,Intent i){super.onProfileProvisioningComplete(c,i);if(PolicyManager.profileOwner(c))Store.setupGuest(c,i);launch(c);}
    private void launch(Context c){
        if(PolicyManager.profileOwner(c)){
            new Handler(Looper.getMainLooper()).postDelayed(()->open(c,SessionSetupActivity.class),250);
            new Handler(Looper.getMainLooper()).postDelayed(()->{if(!Store.supervisionStarted(c))open(c,SessionSetupActivity.class);},1100);
        }else open(c,SplashActivity.class);
    }
    private void open(Context c,Class<?> target){try{c.startActivity(new Intent(c,target).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));}catch(Exception ignored){}}
}
