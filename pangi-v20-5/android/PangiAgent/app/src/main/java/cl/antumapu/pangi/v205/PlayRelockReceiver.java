package cl.antumapu.pangi.v205;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PlayRelockReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent){
        PlayStoreGuard.lock(context);
    }
}
