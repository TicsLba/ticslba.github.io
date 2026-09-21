package cl.antumapu.aulacontrol;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
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
            AppPolicy.applyGuest(c);
            launchGuest(c);
            new Handler(Looper.getMainLooper()).postDelayed(()->launchGuest(c),600);
            return;
        }

        if(Managed.owner(c)){
            // Arm the synchronous owner guard before doing anything else.
            Managed.applyBootOwner(c);
            SessionState.ownerGate(c);
            Managed.applyOwner(c);
            launchOwner(c);
            new Handler(Looper.getMainLooper()).postDelayed(()->launchOwner(c),300);
        }
    }

    private void launchOwner(Context c){
        try{c.startActivity(new Intent(c,HomeGateActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));}
        catch(Exception ignored){}
    }

    private void launchGuest(Context c){
        try{c.startActivity(new Intent(c,HomeGateActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));}
        catch(Exception ignored){}
    }
}
