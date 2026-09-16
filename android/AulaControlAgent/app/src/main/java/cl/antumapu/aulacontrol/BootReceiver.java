package cl.antumapu.aulacontrol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i){
        try{Intent s=new Intent(c,AgentService.class);if(Build.VERSION.SDK_INT>=26)c.startForegroundService(s);else c.startService(s);}catch(Exception ignored){}
        if(Managed.owner(c)&&Core.ready(c)&&!Core.adminMode(c)){SessionState.ownerGate(c);Managed.applyOwner(c);launch(c);}
        else if(Managed.profileOwner(c)&&Core.ready(c)){if(!SessionState.isActive(c))SessionState.guestSetup(c);Managed.applyGuest(c);launch(c);}
    }
    private void launch(Context c){try{c.startActivity(new Intent(c,SplashActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));}catch(Exception ignored){}}
}
