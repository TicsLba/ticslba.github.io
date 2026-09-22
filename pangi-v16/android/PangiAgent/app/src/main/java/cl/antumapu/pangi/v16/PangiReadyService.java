package cl.antumapu.pangi.v16;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class PangiReadyService extends Service {
    private final Binder binder=new Binder();

    @Override public IBinder onBind(Intent intent){
        if(!PangiPolicy.isProfileOwner(this))return null;
        boolean ready=PangiStore.guestReady(this);
        if(!ready){
            ready=PangiPolicy.prepareGuest(this);
            PangiStore.guestReady(this,ready);
        }
        if(!ready||!PangiPolicy.ensureIme(this))return null;
        return binder;
    }
}
