package cl.antumapu.guardlite;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;

public class PangiDeviceAdminReceiver extends DeviceAdminReceiver {
    @Override
    public void onEnabled(Context context, Intent intent) {
        super.onEnabled(context, intent);
        PangiPolicyManager.applyDefaults(context);
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        return "Pangi administra institucionalmente esta tablet. La administración no debe desactivarse fuera de mantenimiento autorizado.";
    }
}
