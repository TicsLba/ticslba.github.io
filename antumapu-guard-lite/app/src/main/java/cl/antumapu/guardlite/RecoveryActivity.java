package cl.antumapu.guardlite;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.UserManager;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class RecoveryActivity extends Activity {
    private int dp(int n) {
        return (int)(n * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(28), dp(28), dp(28), dp(28));
        root.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("Pangi Recovery");
        title.setTextSize(30);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(Color.rgb(0, 90, 18));
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView info = new TextView(this);
        info.setText("Herramienta administrativa para liberar esta tablet de Pangi sin restablecerla de fábrica.\n\nEsta acción quitará Device Owner, liberará el launcher y permitirá desinstalar Pangi.");
        info.setTextSize(15);
        info.setTextColor(Color.rgb(60, 70, 64));
        info.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(-1, -2);
        ip.setMargins(0, dp(18), 0, dp(24));
        root.addView(info, ip);

        Button release = new Button(this);
        release.setText("LIBERAR PANGI DE ESTA TABLET");
        release.setTextColor(Color.WHITE);
        release.setTextSize(14);
        release.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        release.setBackgroundColor(Color.rgb(244, 90, 30));
        release.setOnClickListener(v -> confirmRelease());
        root.addView(release, new LinearLayout.LayoutParams(-1, dp(58)));

        TextView foot = new TextView(this);
        foot.setText("© Eduardo Pérez González · Coordinación Informática Liceo Bicentenario Antumapu");
        foot.setTextSize(11);
        foot.setTextColor(Color.GRAY);
        foot.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(-1, -2);
        fp.setMargins(0, dp(22), 0, 0);
        root.addView(foot, fp);

        setContentView(root);
    }

    private void confirmRelease() {
        new AlertDialog.Builder(this)
                .setTitle("Confirmar liberación")
                .setMessage("Pangi dejará de administrar este dispositivo. Después podrás desinstalar la aplicación con ADB. No se realizará un restablecimiento de fábrica.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Liberar", (dialog, which) -> releaseDevice())
                .show();
    }

    @SuppressWarnings("deprecation")
    private void releaseDevice() {
        DevicePolicyManager dpm =
                (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        ComponentName admin = new ComponentName(this, PangiDeviceAdminReceiver.class);

        if (dpm == null || !dpm.isDeviceOwnerApp(getPackageName())) {
            Toast.makeText(this, "Pangi ya no es Device Owner.", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            try {
                dpm.clearPackagePersistentPreferredActivities(admin, getPackageName());
            } catch (Exception ignored) {}

            try {
                dpm.setUninstallBlocked(admin, getPackageName(), false);
            } catch (Exception ignored) {}

            try {
                dpm.clearUserRestriction(admin, UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES);
            } catch (Exception ignored) {}

            try {
                dpm.clearUserRestriction(admin, UserManager.DISALLOW_APPS_CONTROL);
            } catch (Exception ignored) {}

            try {
                dpm.setApplicationHidden(admin, "com.android.vending", false);
            } catch (Exception ignored) {}

            dpm.clearDeviceOwnerApp(getPackageName());

            new AlertDialog.Builder(this)
                    .setTitle("Pangi liberado")
                    .setMessage("Device Owner fue eliminado. Ahora vuelve a PowerShell y ejecuta:\n\nadb uninstall cl.antumapu.guardlite")
                    .setCancelable(false)
                    .setPositiveButton("Aceptar", null)
                    .show();

        } catch (SecurityException e) {
            new AlertDialog.Builder(this)
                    .setTitle("No fue posible liberar Pangi")
                    .setMessage(e.toString())
                    .setPositiveButton("Aceptar", null)
                    .show();
        } catch (Exception e) {
            new AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage(e.toString())
                    .setPositiveButton("Aceptar", null)
                    .show();
        }
    }
}
