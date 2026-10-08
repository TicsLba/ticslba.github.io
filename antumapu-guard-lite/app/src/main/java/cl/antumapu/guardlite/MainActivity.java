package cl.antumapu.guardlite;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
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
    private TextView protectionStatus;
    private TextView serviceStatus;
    private TextView timeoutStatus;
    private boolean authDialogShowing = false;

    private int dp(int n) {
        return (int)(n * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();

        if (!Prefs.hasPin(this)) {
            showPinSetup(true);
        } else if (!Prefs.isUnlocked(this)) {
            showAdminLogin();
        }
    }

    @Override protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radius));
        return g;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(18), dp(17), dp(18), dp(17));
        c.setBackground(round(Color.WHITE, 16));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, 0, 0, dp(14));
        c.setLayoutParams(p);
        return c;
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(243,247,244));

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(page);

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(24), dp(30), dp(24), dp(27));
        hero.setBackgroundColor(Color.rgb(16,55,35));

        TextView kicker = text("CONTROL DE ACCESO · SIN ROOT", 11, Color.rgb(229,138,43), true);
        kicker.setLetterSpacing(0.08f);
        hero.addView(kicker);

        TextView title = text("Antumapu Guard Lite", 29, Color.WHITE, true);
        LinearLayout.LayoutParams titleP = new LinearLayout.LayoutParams(-1, -2);
        titleP.setMargins(0, dp(8), 0, dp(6));
        hero.addView(title, titleP);

        hero.addView(text(
                "Protege solo las áreas sensibles. El resto de Ajustes permanece disponible.",
                15, Color.rgb(207,224,213), false));

        page.addView(hero);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(18), dp(18), dp(28));
        page.addView(content);

        LinearLayout status = card();
        status.addView(sectionLabel("ESTADO"));
        protectionStatus = text("", 19, Color.rgb(27,92,56), true);
        LinearLayout.LayoutParams ps = new LinearLayout.LayoutParams(-1, -2);
        ps.setMargins(0, dp(8), 0, dp(2));
        status.addView(protectionStatus, ps);
        serviceStatus = text("", 14, Color.rgb(82,99,88), false);
        status.addView(serviceStatus);
        timeoutStatus = text("", 14, Color.rgb(82,99,88), false);
        status.addView(timeoutStatus);
        content.addView(status);

        LinearLayout protectedAreas = card();
        protectedAreas.addView(sectionLabel("ÁREAS PROTEGIDAS"));

        TextView intro = text(
                "Activa o desactiva cada protección. Ninguna de estas opciones bloquea Ajustes completo.",
                13, Color.rgb(88,104,93), false);
        LinearLayout.LayoutParams introP = new LinearLayout.LayoutParams(-1, -2);
        introP.setMargins(0, dp(6), 0, dp(8));
        protectedAreas.addView(intro, introP);

        protectedAreas.addView(toggleRow(
                "Google Play Store",
                "Pide PIN cada vez que se intenta abrir Play Store.",
                Prefs.protectPlay(this), 1));

        protectedAreas.addView(toggleRow(
                "Instalación de APK",
                "Protege Package Installer e Instalar apps desconocidas.",
                Prefs.protectInstall(this), 2));

        protectedAreas.addView(toggleRow(
                "Apps y desinstalación",
                "Protege Ajustes > Aplicaciones, App info, desinstalar e inhabilitar.",
                Prefs.protectApps(this), 3));

        protectedAreas.addView(toggleRow(
                "Accesibilidad",
                "Protege el lugar desde donde podrían apagar Antumapu Guard Lite.",
                Prefs.protectAccessibility(this), 4));

        protectedAreas.addView(toggleRow(
                "Opciones de desarrollador",
                "Opcional. Déjalo apagado mientras configuras la tablet.",
                Prefs.protectDeveloper(this), 5));

        content.addView(protectedAreas);

        LinearLayout free = card();
        free.addView(sectionLabel("AJUSTES QUE SIGUEN LIBRES"));
        TextView freeText = text(
                "Wi‑Fi · Bluetooth · Sonido · Pantalla · Brillo · Idioma · Fecha y hora · Batería · Almacenamiento y demás ajustes cotidianos.",
                14, Color.rgb(57,80,65), false);
        LinearLayout.LayoutParams freeP = new LinearLayout.LayoutParams(-1, -2);
        freeP.setMargins(0, dp(8), 0, 0);
        free.addView(freeText, freeP);
        content.addView(free);

        LinearLayout time = card();
        time.addView(sectionLabel("TIEMPO DE DESBLOQUEO"));
        TextView timeDesc = text(
                "Después de ingresar el PIN, el permiso dura el tiempo elegido y luego se vuelve a bloquear.",
                13, Color.rgb(88,104,93), false);
        LinearLayout.LayoutParams td = new LinearLayout.LayoutParams(-1, -2);
        td.setMargins(0, dp(6), 0, dp(10));
        time.addView(timeDesc, td);

        LinearLayout timeRow = new LinearLayout(this);
        timeRow.setOrientation(LinearLayout.HORIZONTAL);
        timeRow.addView(timeButton("1 MIN", 60), new LinearLayout.LayoutParams(0, dp(48), 1f));
        timeRow.addView(timeButton("5 MIN", 300), new LinearLayout.LayoutParams(0, dp(48), 1f));
        timeRow.addView(timeButton("15 MIN", 900), new LinearLayout.LayoutParams(0, dp(48), 1f));
        time.addView(timeRow);
        content.addView(time);

        LinearLayout service = card();
        service.addView(sectionLabel("SERVICIO DE PROTECCIÓN"));

        TextView serviceText = text(
                "Antumapu Guard Lite necesita Accesibilidad para detectar qué pantalla está abierta. No requiere root ni modifica Android.",
                13, Color.rgb(88,104,93), false);
        LinearLayout.LayoutParams st = new LinearLayout.LayoutParams(-1, -2);
        st.setMargins(0, dp(6), 0, dp(10));
        service.addView(serviceText, st);

        Button accessibility = actionButton("ACTIVAR / REVISAR ACCESIBILIDAD", true);
        accessibility.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        service.addView(accessibility);

        Button test = actionButton("PROBAR CON PLAY STORE", false);
        test.setOnClickListener(v -> openPlayStore());
        service.addView(test);

        content.addView(service);

        LinearLayout admin = card();
        admin.addView(sectionLabel("ADMINISTRACIÓN"));

        Button lock = actionButton("BLOQUEAR AHORA", false);
        lock.setOnClickListener(v -> {
            Prefs.lock(this);
            Toast.makeText(this, "Protección bloqueada nuevamente", Toast.LENGTH_SHORT).show();
            refreshStatus();
            showAdminLogin();
        });
        admin.addView(lock);

        Button pin = actionButton("CAMBIAR PIN ADMINISTRATIVO", false);
        pin.setOnClickListener(v -> showPinSetup(false));
        admin.addView(pin);

        content.addView(admin);

        TextView warning = text(
                "Protección local mediante Accesibilidad. No equivale a MDM o Device Owner y puede ser anulada mediante restablecimiento de fábrica o Modo seguro.",
                12, Color.rgb(112,126,117), false);
        warning.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(-1, -2);
        wp.setMargins(dp(10), dp(2), dp(10), dp(10));
        content.addView(warning, wp);

        TextView foot = text("Antumapu Guard Lite · 1.0", 12,
                Color.rgb(118,132,122), false);
        foot.setGravity(Gravity.CENTER_HORIZONTAL);
        content.addView(foot);

        setContentView(scroll);
    }

    private TextView sectionLabel(String label) {
        TextView t = text(label, 11, Color.rgb(87,105,92), true);
        t.setLetterSpacing(0.07f);
        return t;
    }

    private View toggleRow(String title, String subtitle, boolean checked, final int which) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);

        labels.addView(text(title, 16, Color.rgb(25,58,38), true));
        TextView sub = text(subtitle, 12, Color.rgb(95,111,100), false);
        LinearLayout.LayoutParams subP = new LinearLayout.LayoutParams(-1, -2);
        subP.setMargins(0, dp(2), dp(8), 0);
        labels.addView(sub, subP);

        row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1f));

        Switch sw = new Switch(this);
        sw.setChecked(checked);
        sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!Prefs.hasPin(this) || !Prefs.isUnlocked(this)) {
                buttonView.setChecked(!isChecked);
                showAdminLogin();
                return;
            }
            if (which == 1) Prefs.setProtectPlay(this, isChecked);
            if (which == 2) Prefs.setProtectInstall(this, isChecked);
            if (which == 3) Prefs.setProtectApps(this, isChecked);
            if (which == 4) Prefs.setProtectAccessibility(this, isChecked);
            if (which == 5) Prefs.setProtectDeveloper(this, isChecked);
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
            if (!Prefs.isUnlocked(this)) {
                showAdminLogin();
                return;
            }
            Prefs.setUnlockSeconds(this, seconds);
            Toast.makeText(this, "Tiempo configurado: " + label.toLowerCase(), Toast.LENGTH_SHORT).show();
            refreshStatus();
        });
        return b;
    }

    private Button actionButton(String label, boolean primary) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(13);
        b.setTextColor(primary ? Color.WHITE : Color.rgb(26,70,45));
        if (primary) {
            b.setBackground(round(Color.rgb(28,105,65), 12));
        }
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(52));
        p.setMargins(0, dp(9), 0, 0);
        b.setLayoutParams(p);
        return b;
    }

    private void refreshStatus() {
        if (protectionStatus == null) return;

        boolean enabled = isServiceEnabled();
        boolean unlocked = Prefs.isUnlocked(this);

        if (!enabled) {
            protectionStatus.setText("● Servicio pendiente");
            protectionStatus.setTextColor(Color.rgb(194,93,31));
        } else if (unlocked) {
            protectionStatus.setText("● Modo mantenimiento");
            protectionStatus.setTextColor(Color.rgb(201,117,24));
        } else {
            protectionStatus.setText("● Protección activa");
            protectionStatus.setTextColor(Color.rgb(25,125,67));
        }

        serviceStatus.setText(enabled
                ? "Accesibilidad · activada"
                : "Accesibilidad · debes activarla una vez");

        int sec = Prefs.getUnlockSeconds(this);
        String value = sec == 60 ? "1 minuto" : sec == 900 ? "15 minutos" : "5 minutos";
        timeoutStatus.setText("Desbloqueo temporal · " + value);
    }

    private boolean isServiceEnabled() {
        String expected = new ComponentName(this, GuardAccessibilityService.class).flattenToString();
        String enabled = Settings.Secure.getString(
                getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabled == null) return false;

        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
        splitter.setString(enabled);
        while (splitter.hasNext()) {
            String item = splitter.next();
            if (item.equalsIgnoreCase(expected)) return true;
        }
        return false;
    }

    private void openPlayStore() {
        Intent launch = getPackageManager().getLaunchIntentForPackage("com.android.vending");
        if (launch == null) {
            Toast.makeText(this, "Play Store no está disponible", Toast.LENGTH_SHORT).show();
            return;
        }
        Prefs.lock(this);
        startActivity(launch);
    }

    private void showAdminLogin() {
        if (!Prefs.hasPin(this) || authDialogShowing) return;
        authDialogShowing = true;

        final EditText input = new EditText(this);
        input.setHint("PIN administrativo");
        input.setGravity(Gravity.CENTER);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Administración protegida")
                .setMessage("Ingresa el PIN para modificar Antumapu Guard Lite.")
                .setView(input)
                .setCancelable(false)
                .setNegativeButton("Salir", (d, w) -> {
                    authDialogShowing = false;
                    finish();
                })
                .setPositiveButton("Entrar", null)
                .create();

        dialog.setOnShowListener(d ->
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String value = input.getText() == null ? "" : input.getText().toString();
                    if (Prefs.checkPin(this, value)) {
                        Prefs.unlock(this);
                        authDialogShowing = false;
                        dialog.dismiss();
                        refreshStatus();
                    } else {
                        input.setText("");
                        Toast.makeText(this, "PIN incorrecto", Toast.LENGTH_SHORT).show();
                    }
                }));

        dialog.setOnDismissListener(d -> authDialogShowing = false);
        dialog.show();
    }

    private void showPinSetup(final boolean first) {
        LinearLayout fields = new LinearLayout(this);
        fields.setOrientation(LinearLayout.VERTICAL);
        fields.setPadding(dp(22), dp(6), dp(22), 0);

        final EditText one = new EditText(this);
        one.setHint(first ? "Crea un PIN de 4 a 12 dígitos" : "Nuevo PIN");
        one.setSingleLine(true);
        one.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);

        final EditText two = new EditText(this);
        two.setHint("Repite el PIN");
        two.setSingleLine(true);
        two.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);

        fields.addView(one);
        fields.addView(two);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(first ? "Configurar protección" : "Cambiar PIN")
                .setMessage(first
                        ? "Elige la clave administrativa que protegerá las áreas sensibles."
                        : "Define una nueva clave administrativa.")
                .setView(fields)
                .setCancelable(!first)
                .setNegativeButton(first ? null : "Cancelar", null)
                .setPositiveButton("Guardar", null)
                .create();

        dialog.setOnShowListener(d ->
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String a = one.getText() == null ? "" : one.getText().toString();
                    String b = two.getText() == null ? "" : two.getText().toString();

                    if (a.length() < 4 || a.length() > 12 || !a.equals(b)) {
                        Toast.makeText(this,
                                "El PIN debe tener 4 a 12 dígitos y coincidir",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    Prefs.setPin(this, a);
                    Prefs.unlock(this);
                    dialog.dismiss();
                    Toast.makeText(this,
                            first ? "PIN creado. Ahora activa Accesibilidad." : "PIN actualizado",
                            Toast.LENGTH_LONG).show();
                    refreshStatus();
                }));

        dialog.show();
    }
}
