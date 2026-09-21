package cl.antumapu.aulacontrol;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.os.UserManager;
import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    private DevicePolicyManager dpm;
    private ComponentName admin;
    private TextView status;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        dpm = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);
        admin = new ComponentName(this, AdminReceiver.class);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(36,36,36,36);
        root.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = new TextView(this);
        title.setText("Aula Móvil · Limpiador");
        title.setTextSize(26);
        title.setTextColor(Color.BLACK);
        root.addView(title,new LinearLayout.LayoutParams(-1,-2));

        TextView info = new TextView(this);
        info.setText("\nEste APK es temporal. Limpia políticas heredadas de Aula Móvil 7.0 y libera Device Owner para poder desinstalar la versión anterior.\n\nNo borra aplicaciones, archivos ni Wi‑Fi de la tablet.");
        info.setTextSize(17);
        info.setTextColor(Color.DKGRAY);
        root.addView(info,new LinearLayout.LayoutParams(-1,-2));

        status = new TextView(this);
        status.setText("\nEstado: comprobando...");
        status.setTextSize(16);
        status.setTextColor(Color.BLACK);
        root.addView(status,new LinearLayout.LayoutParams(-1,-2));

        Button clean = new Button(this);
        clean.setText("LIMPIAR Y LIBERAR DEVICE OWNER");
        clean.setAllCaps(false);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1,-2);
        bp.topMargin=30;
        root.addView(clean,bp);

        clean.setOnClickListener(v -> {
            clean.setEnabled(false);
            new Thread(() -> runCleanup()).start();
        });

        ScrollView sv = new ScrollView(this);
        sv.addView(root,new ScrollView.LayoutParams(-1,-1));
        setContentView(sv);

        boolean owner = dpm.isDeviceOwnerApp(getPackageName());
        status.setText("\nDevice Owner actual: " + (owner ? "SÍ" : "NO") +
                "\nPaquete: " + getPackageName() +
                "\n\nPulsa el botón solo si quieres eliminar por completo la instalación anterior.");
    }

    private void runCleanup() {
        final StringBuilder log = new StringBuilder();
        try {
            if (!dpm.isDeviceOwnerApp(getPackageName())) {
                post("ERROR: esta app no es Device Owner. No se modificó nada.");
                return;
            }

            try {
                Bundle r = dpm.getUserRestrictions(admin);
                for (String key : r.keySet()) {
                    if (r.getBoolean(key, false)) {
                        try { dpm.clearUserRestriction(admin, key); log.append("Restricción eliminada: ").append(key).append("\n"); }
                        catch (Exception e) { log.append("No se pudo limpiar ").append(key).append(": ").append(e.getClass().getSimpleName()).append("\n"); }
                    }
                }
            } catch (Exception e) { log.append("Restricciones: ").append(e.getClass().getSimpleName()).append("\n"); }

            try { dpm.setApplicationHidden(admin, "com.android.vending", false); log.append("Play Store: visible\n"); }
            catch (Exception e) { log.append("Play Store visible: ").append(e.getClass().getSimpleName()).append("\n"); }

            try { dpm.setPackagesSuspended(admin, new String[]{"com.android.vending"}, false); log.append("Play Store: no suspendida\n"); }
            catch (Exception e) { log.append("Play Store suspensión: ").append(e.getClass().getSimpleName()).append("\n"); }

            try { dpm.setScreenCaptureDisabled(admin, false); log.append("Captura de pantalla: habilitada\n"); }
            catch (Exception e) { log.append("Captura: ").append(e.getClass().getSimpleName()).append("\n"); }

            try { dpm.setCameraDisabled(admin, false); log.append("Cámara: habilitada\n"); }
            catch (Exception e) { log.append("Cámara: ").append(e.getClass().getSimpleName()).append("\n"); }

            try { dpm.setBluetoothContactSharingDisabled(admin, false); log.append("Compartir contactos Bluetooth: habilitado\n"); }
            catch (Exception e) { log.append("Bluetooth contactos: ").append(e.getClass().getSimpleName()).append("\n"); }

            try { dpm.setKeyguardDisabledFeatures(admin, DevicePolicyManager.KEYGUARD_DISABLE_FEATURES_NONE); log.append("Keyguard: valores por defecto\n"); }
            catch (Exception e) { log.append("Keyguard: ").append(e.getClass().getSimpleName()).append("\n"); }

            try { dpm.setLockTaskPackages(admin, new String[]{}); log.append("Lock task allowlist: vacía\n"); }
            catch (Exception e) { log.append("Lock task: ").append(e.getClass().getSimpleName()).append("\n"); }

            try { dpm.setStatusBarDisabled(admin, false); log.append("Barra de estado: habilitada\n"); }
            catch (Exception e) { log.append("Barra de estado: ").append(e.getClass().getSimpleName()).append("\n"); }

            try { dpm.setUninstallBlocked(admin, getPackageName(), false); log.append("Desinstalación propia: desbloqueada\n"); }
            catch (Exception e) { log.append("Desinstalación propia: ").append(e.getClass().getSimpleName()).append("\n"); }

            try { dpm.setPermissionPolicy(admin, DevicePolicyManager.PERMISSION_POLICY_PROMPT); log.append("Permisos: política normal\n"); }
            catch (Exception e) { log.append("Permisos: ").append(e.getClass().getSimpleName()).append("\n"); }

            try { dpm.clearPackagePersistentPreferredActivities(admin, getPackageName()); log.append("Preferencias persistentes: limpiadas\n"); }
            catch (Exception e) { log.append("Preferencias: ").append(e.getClass().getSimpleName()).append("\n"); }

            post("Limpieza de políticas terminada. Liberando Device Owner...\n\n" + log);
            Thread.sleep(600);

            dpm.clearDeviceOwnerApp(getPackageName());

            boolean stillOwner = dpm.isDeviceOwnerApp(getPackageName());
            post(log + "\nDevice Owner liberado: " + (!stillOwner ? "SÍ" : "NO") +
                    "\n\nVuelve a PowerShell y verifica con dumpsys device_policy. Después podremos desinstalar el paquete.");
        } catch (Throwable t) {
            post("ERROR durante la limpieza: " + t.getClass().getSimpleName() + ": " + t.getMessage() + "\n\n" + log);
        }
    }

    private void post(String s) {
        runOnUiThread(() -> status.setText("\n" + s));
    }
}
