package cl.antumapu.pangi.v205;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class PangiReadyService extends Service {
    private final Binder binder=new Binder();

    @Override public IBinder onBind(Intent intent){
        if(!Managed.profileOwner(this))return null;
        try{
            Managed.applyGuest(this);
            AppPolicy.applyGuest(this);
            Managed.homeGuardOn(this);
            if(!ImeGuard.ensure(this))return null;
            return binder;
        }catch(Exception e){
            return null;
        }
    }
}
