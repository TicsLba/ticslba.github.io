package cl.antumapu.guardlite;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;

public class PangiDeviceAdminReceiver extends DeviceAdminReceiver {
    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        return "Pangi protege este dispositivo contra desinstalaciones no autorizadas. Desactívalo solo durante mantenimiento administrativo.";
    }
}
