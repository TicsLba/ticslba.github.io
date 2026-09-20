package cl.antumapu.aulacontrol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.UserManager;

/**
 * Boot fallback. The owner HOME guard is the primary mechanism; this receiver
 * reasserts it and launches the guard when Android/OEM boot ordering allows it.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i){
        String action=i==null?"":i.getAction();

        if(Managed.owner(c)){
            Managed.homeGuardOn(c);

            UserManager um=(UserManager)c.getSystemService(Context.USER_SERVICE);
            boolean unlocked=um==null||um.isUserUnlocked();
            if(unlocked){
                try{Core.adminMode(c,false);}catch(Exception ignored){}
                try{SessionState.ownerGate(c);}catch(Exception ignored){}
                try{Managed.applyOwner(c);}catch(Exception ignored){}
            }

            launchOwnerGuard(c);
            return;
        }

        if(Managed.profileOwner(c)){
            if(Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action))return;
            try{
                if(!SessionState.isActive(c))SessionState.guestSetup(c);
                Managed.applyGuest(c);
                c.startActivity(new Intent(c,SplashActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|
                                Intent.FLAG_ACTIVITY_CLEAR_TOP|
                                Intent.FLAG_ACTIVITY_SINGLE_TOP));
            }catch(Exception ignored){}
        }
    }

    private void launchOwnerGuard(Context c){
        try{
            c.startActivity(new Intent(c,HomeGateActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|
                            Intent.FLAG_ACTIVITY_CLEAR_TOP|
                            Intent.FLAG_ACTIVITY_SINGLE_TOP));
        }catch(Exception ignored){}
    }
}
