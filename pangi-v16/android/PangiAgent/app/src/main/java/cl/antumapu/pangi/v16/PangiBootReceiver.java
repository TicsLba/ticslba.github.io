package cl.antumapu.pangi.v16;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PangiBootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i){
        if(!PangiPolicy.isOwner(c))return;
        PangiStore.adminMode(c,false);
        PangiPolicy.prepareOwnerGate(c);

        if(Intent.ACTION_BOOT_COMPLETED.equals(i.getAction())){
            final PendingResult pr=goAsync();
            new Thread(()->{
                try{SessionCoordinator.cleanupSecondaryUsers(c);}
                finally{pr.finish();}
            },"PangiBootCleanup").start();
        }
    }
}
