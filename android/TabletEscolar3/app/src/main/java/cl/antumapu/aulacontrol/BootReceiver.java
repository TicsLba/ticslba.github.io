package cl.antumapu.aulacontrol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i){
        try{
            if(PolicyManager.profileOwner(c)){
                if(Store.supervisionStarted(c)){Store.state(c,Store.STATE_CLOSING);SessionUsers.logoutGuest(c);}else open(c,SessionSetupActivity.class);
                return;
            }
            if(PolicyManager.owner(c)&&Store.configured(c)){
                Store.state(c,Store.STATE_GATE);PolicyManager.applyOwnerGate(c);startAgent(c);open(c,GateActivity.class);
            }else open(c,SplashActivity.class);
        }catch(Exception ignored){}
    }
    private void startAgent(Context c){try{Intent s=new Intent(c,AgentService.class);if(Build.VERSION.SDK_INT>=26)c.startForegroundService(s);else c.startService(s);}catch(Exception ignored){}}
    private void open(Context c,Class<?> k){try{c.startActivity(new Intent(c,k).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));}catch(Exception ignored){}}
}
