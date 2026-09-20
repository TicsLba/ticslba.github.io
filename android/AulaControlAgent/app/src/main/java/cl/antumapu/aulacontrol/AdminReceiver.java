package cl.antumapu.aulacontrol;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

public class AdminReceiver extends DeviceAdminReceiver {
    @Override public void onEnabled(Context c,Intent i){super.onEnabled(c,i);configure(c,i);}
    @Override public void onProfileProvisioningComplete(Context c,Intent i){super.onProfileProvisioningComplete(c,i);configure(c,i);}

    private void configure(Context c,Intent i){
        if(Managed.profileOwner(c)){
            Core.setupGuest(c,i);
            RelayPrefs.setupGuest(c,i);
            SessionState.guestSetup(c);
            Managed.applyGuest(c);
            startAgent(c);
            launchGuest(c);
            new Handler(Looper.getMainLooper()).postDelayed(()->launchGuest(c),650);
            return;
        }

        if(Managed.owner(c)){
            SessionState.ownerGate(c);
            Managed.applyBootOwner(c);
            launchOwner(c);
            new Handler(Looper.getMainLooper()).postDelayed(()->launchOwner(c),350);
        }
    }

    private void startAgent(Context c){
        try{
            Intent s=new Intent(c,AgentService.class);
            if(Build.VERSION.SDK_INT>=26)c.startForegroundService(s);else c.startService(s);
        }catch(Exception ignored){}
    }

    private void launchOwner(Context c){
        try{c.startActivity(new Intent(c,BootGuardActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));}
        catch(Exception ignored){}
    }

    private void launchGuest(Context c){
        try{c.startActivity(new Intent(c,SplashActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));}
        catch(Exception ignored){}
    }
}
