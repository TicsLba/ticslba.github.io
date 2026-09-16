package cl.antumapu.aulacontrol;

import android.app.admin.*;
import android.content.*;
import android.os.*;

public class AdminReceiver extends DeviceAdminReceiver {
    @Override
    public void onEnabled(Context c, Intent i) {
        super.onEnabled(c, i);
        if (Managed.profileOwner(c)) {
            Core.setupGuest(c, i);
            RelayPrefs.setupGuest(c, i);
        }
        Managed.apply(c);
        start(c);
    }

    @Override
    public void onProfileProvisioningComplete(Context c, Intent i) {
        super.onProfileProvisioningComplete(c, i);
        if (Managed.profileOwner(c)) {
            Core.setupGuest(c, i);
            RelayPrefs.setupGuest(c, i);
        }
        Managed.apply(c);
        start(c);
    }

    private void start(Context c) {
        try {
            Intent s = new Intent(c, AgentService.class);
            if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(s);
            else c.startService(s);
        } catch (Exception ignored) {}

        try {
            c.startActivity(new Intent(c, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TOP |
                            Intent.FLAG_ACTIVITY_SINGLE_TOP));
        } catch (Exception ignored) {}
    }
}
