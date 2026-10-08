package cl.antumapu.guard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private LinearLayout content;
    private TextView statusProtection;
    private TextView statusRoot;
    private TextView statusAccess;

    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        buildUi();
        if (!Prefs.hasPin(this)) showPinDialog(true);
    }

    @Override protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private GradientDrawable bg(int color, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radius));
        return g;
    }

    private TextView text(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(18), dp(16), dp(18), dp(16));
        c.setBackground(bg(Color.WHITE, 16));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, 0, 0, dp(14));
        c.setLayoutParams(p);
        return c;
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.rgb(242,246,243));

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), 0, dp(18), dp(28));
        scroll.addView(content);

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(22), dp(32), dp(22), dp(24));
        hero.setBackgroundColor(Color.rgb(16, 55, 36));

        TextView eyebrow = text("SEGURIDAD DEL DISPOSITIVO", 12, Color.rgb(229,138,43), true);
        eyebrow.setLetterSpacing(0.08f);
        hero.addView(eyebrow);

        TextView title = text("Antumapu Guard", 30, Color.WHITE, true);
        LinearLayout.LayoutParams titleP = new LinearLayout.LayoutParams(-1, -2);
        titleP.setMargins(0, dp(8), 0, dp(6));
        hero.addView(title, titleP);

        TextView sub = text("Control administrativo selectivo para tablets institucionales.", 15,
                Color.rgb(207,224,213), false);
        hero.addView(sub);

        content.addView(hero, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout.LayoutParams spacer = new LinearLayout.LayoutParams(-1, dp(18));
        TextView dummy = new TextView(this);
        content.addView(dummy, spacer);

        LinearLayout status = card();
        status.addView(text("ESTADO", 12, Color.rgb(83,105,91), true));
        statusProtection = text("", 18, Color.rgb(20,65,41), true);
        statusRoot = text("", 14, Color.rgb(80,95,85), false);
        statusAccess = text("", 14, Color.rgb(80,95,85), false);
        LinearLayout.LayoutParams m = new LinearLayout.LayoutParams(-1, -2);
        m.setMargins(0, dp(8), 0, 0);
        status.addView(statusProtection, m);
        status.addView(statusRoot);
        status.addView(statusAccess);
        content.addView(status);

        LinearLayout protectedCard = card();
        protectedCard.addView(text("ÁREAS PROTEGIDAS", 12, Color.rgb(83,105,91), true));
        TextView desc = text("Solo estas áreas pedirán clave. Wi‑Fi, Bluetooth, sonido, pantalla y el resto de Ajustes seguirán disponibles.",
                13, Color.rgb(90,105,95), false);
        LinearLayout.LayoutParams dp1 = new LinearLayout.LayoutParams(-1, -2);
        dp1.setMargins(0, dp(6), 0, dp(10));
        protectedCard.addView(desc, dp1);

        protectedCard.addView(toggle("Google Play Store",
                "Solicita PIN al abrir Play Store.", Prefs.protectPlay(this), 1));
        protectedCard.addView(toggle("Instalación de APK",
                "Protege Package Installer y las instalaciones desde Chrome, Files y terceros.", Prefs.protectInstaller(this), 2));
        protectedCard.addView(toggle("Apps y desinstalación",
                "Protege Ajustes > Apps, información de app, desinstalar e inhabilitar.", Prefs.protectApps(this), 3));
        protectedCard.addView(toggle("Accesibilidad",
                "Evita que se desactive la protección desde Accesibilidad.", Prefs.protectAccessibility(this), 4));
        protectedCard.addView(toggle("Opciones de desarrollador",
                "Opcional. Útil cuando la tablet ya queda entregada.", Prefs.protectDeveloper(this), 5));
        content.addView(protectedCard);

        LinearLayout timeCard = card();
        timeCard.addView(text("TIEMPO DE MANTENIMIENTO", 12, Color.rgb(83,105,91), true));
        TextView timeText = text("Después de ingresar la clave, las áreas protegidas quedan abiertas temporalmente.",
                13, Color.rgb(90,105,95), false);
        LinearLayout.LayoutParams tt = new LinearLayout.LayoutParams(-1, -2);
        tt.setMargins(0, dp(6), 0, dp(10));
        timeCard.addView(timeText, tt);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(timeButton("1 MIN", 60), new LinearLayout.LayoutParams(0, dp(48), 1f));
        row.addView(timeButton("5 MIN", 300), new LinearLayout.LayoutParams(0, dp(48), 1f));
        row.addView(timeButton("15 MIN", 900), new LinearLayout.LayoutParams(0, dp(48), 1f));
        timeCard.addView(row);
        content.addView(timeCard);

        LinearLayout actions = card();
        actions.addView(text("ADMINISTRACIÓN", 12, Color.rgb(83,105,91), true));

        Button apply = action("APLICAR PROTECCIÓN ROOT", true);
        apply.setOnClickListener(v -> {
            if (!Shell.hasRoot()) {
                Toast.makeText(this, "No se detectó root", Toast.LENGTH_LONG).show();
                return;
            }
            boolean ok = RootPolicy.ensureAccessibility(this);
            Prefs.lock(this);
            RelockReceiver.cancel(this);
            RootPolicy.applyLocked(this, true);
            Toast.makeText(this, ok ? "Protección aplicada" : "Root OK, pero revisa Accesibilidad", Toast.LENGTH_LONG).show();
            refreshStatus();
        });
        actions.addView(apply);

        Button lock = action("BLOQUEAR AHORA", false);
        lock.setOnClickListener(v -> {
            Prefs.lock(this);
            RelockReceiver.cancel(this);
            RootPolicy.applyLocked(this, true);
            Toast.makeText(this, "Protección reactivada", Toast.LENGTH_SHORT).show();
            refreshStatus();
        });
        actions.addView(lock);

        Button pin = action("CAMBIAR PIN ADMINISTRATIVO", false);
        pin.setOnClickListener(v -> showPinDialog(false));
        actions.addView(pin);

        Button accessibility = action("REVISAR ACCESIBILIDAD", false);
        accessibility.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        actions.addView(accessibility);

        content.addView(actions);

        TextView foot = text("Antumapu Guard 3.0 · Protección selectiva ROOT", 12,
                Color.rgb(115,130,120), false);
        foot.setGravity(Gravity.CENTER_HORIZONTAL);
        content.addView(foot);

        setContentView(scroll);
    }

    private View toggle(String title, String sub, boolean checked, final int which) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text(title, 16, Color.rgb(25,55,36), true));
        labels.addView(text(sub, 12, Color.rgb(95,110,100), false));
        row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1f));

        Switch sw = new Switch(this);
        sw.setChecked(checked);
        sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (which == 1) Prefs.setProtectPlay(this, isChecked);
            if (which == 2) Prefs.setProtectInstaller(this, isChecked);
            if (which == 3) Prefs.setProtectApps(this, isChecked);
            if (which == 4) Prefs.setProtectAccessibility(this, isChecked);
            if (which == 5) Prefs.setProtectDeveloper(this, isChecked);
            Prefs.lock(this);
            RootPolicy.applyLocked(this, false);
            refreshStatus();
        });
        row.addView(sw);
        return row;
    }

    private Button timeButton(String label, final int seconds) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(12);
        b.setOnClickListener(v -> {
            Prefs.setUnlockSeconds(this, seconds);
            Toast.makeText(this, "Tiempo: " + label.toLowerCase(), Toast.LENGTH_SHORT).show();
        });
        return b;
    }

    private Button action(String label, boolean primary) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(14);
        b.setTextColor(primary ? Color.WHITE : Color.rgb(25,65,42));
        if (primary) b.setBackground(bg(Color.rgb(24,92,58), 12));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(52));
        p.setMargins(0, dp(10), 0, 0);
        b.setLayoutParams(p);
        return b;
    }

    private void refreshStatus() {
        if (statusProtection == null) return;
        boolean open = Prefs.isUnlocked(this);
        statusProtection.setText(open ? "● Modo mantenimiento activo" : "● Protección activa");
        statusProtection.setTextColor(open ? Color.rgb(201,112,26) : Color.rgb(20,120,65));

        boolean root = Shell.hasRoot();
        statusRoot.setText(root ? "ROOT · disponible" : "ROOT · no disponible");

        String enabled = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        String comp = getPackageName() + "/" + GuardAccessibilityService.class.getName();
        boolean acc = enabled != null && enabled.contains(comp);
        statusAccess.setText(acc ? "Accesibilidad · activa" : "Accesibilidad · requiere activación");
    }

    private void showPinDialog(final boolean first) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(6), dp(20), 0);

        final EditText one = new EditText(this);
        one.setHint(first ? "Crea un PIN de 4 a 12 dígitos" : "Nuevo PIN");
        one.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        final EditText two = new EditText(this);
        two.setHint("Repite el PIN");
        two.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        box.addView(one);
        box.addView(two);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(first ? "Configurar Antumapu Guard" : "Cambiar PIN")
                .setMessage(first ? "Esta clave será necesaria para las áreas protegidas." : "Define una nueva clave administrativa.")
                .setView(box)
                .setCancelable(!first)
                .setNegativeButton(first ? null : "Cancelar", null)
                .setPositiveButton("Guardar", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String a = one.getText() == null ? "" : one.getText().toString();
                    String b = two.getText() == null ? "" : two.getText().toString();
                    if (a.length() < 4 || a.length() > 12 || !a.equals(b)) {
                        Toast.makeText(this, "PIN de 4 a 12 dígitos; ambos deben coincidir", Toast.LENGTH_LONG).show();
                        return;
                    }
                    Prefs.setPin(this, a);
                    Prefs.lock(this);
                    RootPolicy.applyLocked(this, false);
                    dialog.dismiss();
                    Toast.makeText(this, "PIN guardado", Toast.LENGTH_SHORT).show();
                    refreshStatus();
                }));
        dialog.show();
    }
}
