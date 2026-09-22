package cl.antumapu.pangi.v16;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;

public class PangiAdminReceiver extends DeviceAdminReceiver {
    @Override public void onEnabled(Context c,Intent i){
        super.onEnabled(c,i);
        if(PangiPolicy.isOwner(c)){
            PangiStore.adminMode(c,false);
            PangiPolicy.prepareOwnerGate(c);
        }
    }

    @Override public void onProfileProvisioningComplete(Context c,Intent i){
        super.onProfileProvisioningComplete(c,i);
        if(!PangiPolicy.isProfileOwner(c))return;
        PangiStore.bootstrapGuest(c,i);
        boolean ok=PangiPolicy.prepareGuest(c);
        PangiStore.guestReady(c,ok);
    }
}
