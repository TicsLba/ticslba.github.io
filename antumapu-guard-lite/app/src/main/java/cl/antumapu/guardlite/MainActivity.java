package cl.antumapu.guardlite;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int GREEN = Color.rgb(14, 47, 32);
    private static final int GREEN_2 = Color.rgb(25, 96, 61);
    private static final int ORANGE = Color.rgb(229, 138, 43);
    private static final int BG = Color.rgb(244, 247, 245);
    private static final int TEXT = Color.rgb(28, 51, 37);
    private static final int MUTED = Color.rgb(93, 108, 98);

    private boolean adminAuthenticated = false;
    private TextView protectionStatus;
    private TextView serviceStatus;
    private TextView timeoutStatus;
    private TextView uninstallStatus;
    private Button maintenanceButton;
    private Button deviceAdminButton;

    private int dp(int n) {
        return (int)(n * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);

        if (!Prefs.hasPin(this)) {
            buildWelcome();
            showPinSetup(true);
        } else {
            buildAdminGate();
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (adminAuthenticated) refreshStatus();
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radius));
        return g;
    }

    private GradientDrawable outline(int fill, int stroke, int radius) {
        GradientDrawable g = round(fill, radius);
        g.setStroke(dp(1), stroke);
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

    private ImageView logo(int sizeDp) {
        ImageView i = new ImageView(this);
        i.setImageResource(R.drawable.ic_pangi);
        i.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        i.setContentDescription("Pangi");
        i.setLayoutParams(new LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp)));
        return i;
    }

    private void buildWelcome() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(34), dp(36), dp(34), dp(36));
        root.setBackgroundColor(GREEN);

        root.addView(logo(104));

        TextView title = text("Pangi", 38, Color.WHITE, true);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, -2);
        tp.setMargins(0, dp(18), 0, dp(5));
        root.addView(title, tp);

        TextView sub = text("Protección de acceso", 17, Color.rgb(214, 228, 219), false);
        sub.setGravity(Gravity.CENTER);
        root.addView(sub);

        TextView note = text(
                "Configura un PIN administrativo para comenzar.",
                14, Color.rgb(170, 194, 179), false);
        note.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, -2);
        np.setMargins(0, dp(22), 0, 0);
        root.addView(note, np);

        setContentView(root);
    }

    private void buildAdminGate() {
        adminAuthenticated = false;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(34), dp(34), dp(34), dp(34));
        root.setBackgroundColor(GREEN);

        root.addView(logo(96));

        TextView brand = text("PANGI", 13, ORANGE, true);
        brand.setLetterSpacing(0.16f);
        brand.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2);
        bp.setMargins(0, dp(16), 0, dp(8));
        root.addView(brand, bp);

        TextView title = text("Administración protegida", 27, Color.WHITE, true);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView subtitle = text(
                "Ingresa tu PIN para administrar la protección del dispositivo.",
                14, Color.rgb(197, 216, 204), false);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.setMargins(dp(6), dp(9), dp(6), dp(24));
        root.addView(subtitle, sp);

        final EditText pin = new EditText(this);
        pin.setHint("PIN administrativo");
        pin.setSingleLine(true);
        pin.setGravity(Gravity.CENTER);
        pin.setTextSize(19);
        pin.setTextColor(TEXT);
        pin.setHintTextColor(Color.rgb(127, 139, 131));
        pin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        pin.setBackground(round(Color.WHITE, 15));
        pin.setPadding(dp(16), dp(14), dp(16), dp(14));
        root.addView(pin, new LinearLayout.LayoutParams(-1, dp(58)));

        Button enter = new Button(this);
        enter.setText("INGRESAR");
        enter.setTextSize(14);
        enter.setTextColor(Color.WHITE);
        enter.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        enter.setBackground(round(ORANGE, 15));
        enter.setOnClickListener(v -> {
            String value = pin.getText() == null ? "" : pin.getText().toString();
            if (Prefs.checkPin(this, value)) {
                adminAuthenticated = true;
                buildDashboard();
            } else {
                pin.setText("");
                Toast.makeText(this, "PIN incorrecto", Toast.LENGTH_SHORT).show();
            }
        });
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(-1, dp(56));
        ep.setMargins(0, dp(16), 0, 0);
        root.addView(enter, ep);

        TextView foot = text(
                "Áreas sensibles protegidas mediante PIN",
                12, Color.rgb(143, 172, 154), false);
        foot.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(-1, -2);
        fp.setMargins(0, dp(20), 0, 0);
        root.addView(foot, fp);

        setContentView(root);
        pin.requestFocus();
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(19), dp(18), dp(19), dp(18));
        c.setBackground(round(Color.WHITE, 18));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, 0, 0, dp(14));
        c.setLayoutParams(p);
        return c;
    }

    private TextView sectionLabel(String label) {
        TextView t = text(label, 11, Color.rgb(91, 108, 97), true);
        t.setLetterSpacing(0.09f);
        return t;
    }

    private void buildDashboard() {
        if (!adminAuthenticated) {
            buildAdminGate();
            return;
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(page);

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.HORIZONTAL);
        hero.setGravity(Gravity.CENTER_VERTICAL);
        hero.setPadding(dp(22), dp(24), dp(22), dp(24));
        hero.setBackgroundColor(GREEN);

        ImageView icon = logo(66);
        hero.addView(icon);

        LinearLayout heroText = new LinearLayout(this);
        heroText.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams htp = new LinearLayout.LayoutParams(0, -2, 1f);
        htp.setMargins(dp(16), 0, 0, 0);

        TextView brand = text("PANGI", 12, ORANGE, true);
        brand.setLetterSpacing(0.15f);
        heroText.addView(brand);
        heroText.addView(text("Protección de acceso", 25, Color.WHITE, true));

        TextView desc = text("Panel de administración", 13, Color.rgb(190, 211, 198), false);
        LinearLayout.LayoutParams dpv = new LinearLayout.LayoutParams(-1, -2);
        dpv.setMargins(0, dp(4), 0, 0);
        heroText.addView(desc, dpv);

        hero.addView(heroText, htp);
        page.addView(hero);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(18), dp(18), dp(28));
        page.addView(content);

        LinearLayout status = card();
        status.addView(sectionLabel("ESTADO DEL DISPOSITIVO"));

        protectionStatus = text("", 20, GREEN_2, true);
        LinearLayout.LayoutParams ps = new LinearLayout.LayoutParams(-1, -2);
        ps.setMargins(0, dp(10), 0, dp(5));
        status.addView(protectionStatus, ps);

        serviceStatus = text("", 14, MUTED, false);
        status.addView(serviceStatus);

        timeoutStatus = text("", 14, MUTED, false);
        LinearLayout.LayoutParams ts = new LinearLayout.LayoutParams(-1, -2);
        ts.setMargins(0, dp(3), 0, 0);
        status.addView(timeoutStatus, ts);

        maintenanceButton = actionButton("ACTIVAR MODO MANTENIMIENTO", true);
        maintenanceButton.setOnClickListener(v -> {
            if (Prefs.isUnlocked(this)) {
                Prefs.lock(this);
                Toast.makeText(this, "Protección restablecida", Toast.LENGTH_SHORT).show();
            } else {
                Prefs.unlock(this);
                Toast.makeText(this, "Modo mantenimiento activado", Toast.LENGTH_SHORT).show();
            }
            refreshStatus();
        });
        status.addView(maintenanceButton);

        Button lockNow = actionButton("BLOQUEAR AHORA", false);
        lockNow.setOnClickListener(v -> {
            Prefs.lock(this);
            Toast.makeText(this, "Áreas sensibles bloqueadas", Toast.LENGTH_SHORT).show();
            refreshStatus();
        });
        status.addView(lockNow);
        content.addView(status);

        LinearLayout protectedAreas = card();
        protectedAreas.addView(sectionLabel("PROTECCIONES"));

        TextView intro = text(
                "Elige qué áreas requieren PIN. Los ajustes cotidianos siguen disponibles.",
                13, MUTED, false);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(-1, -2);
        ip.setMargins(0, dp(7), 0, dp(9));
        protectedAreas.addView(intro, ip);

        protectedAreas.addView(toggleRow(
                "Google Play Store",
                "Solicita PIN antes de abrir la tienda.",
                Prefs.protectPlay(this), 1));

        protectedAreas.addView(toggleRow(
                "Instalación de APK",
                "Protege Package Installer y fuentes desconocidas.",
                Prefs.protectInstall(this), 2));

        protectedAreas.addView(toggleRow(
                "Aplicaciones y desinstalación",
                "Protege Aplicaciones, App info, desinstalar e inhabilitar.",
                Prefs.protectApps(this), 3));

        protectedAreas.addView(toggleRow(
                "Accesibilidad",
                "Evita que se desactive Pangi sin autorización.",
                Prefs.protectAccessibility(this), 4));

        protectedAreas.addView(toggleRow(
                "Opciones de desarrollador",
                "Protección opcional para configuración avanzada.",
                Prefs.protectDeveloper(this), 5));

        content.addView(protectedAreas);

        LinearLayout time = card();
        time.addView(sectionLabel("ACCESO TEMPORAL"));

        TextView timeDesc = text(
                "Al autorizar un área protegida, Pangi puede mantenerla desbloqueada durante:",
                13, MUTED, false);
        LinearLayout.LayoutParams td = new LinearLayout.LayoutParams(-1, -2);
        td.setMargins(0, dp(7), 0, dp(10));
        time.addView(timeDesc, td);

        LinearLayout timeRow = new LinearLayout(this);
        timeRow.setOrientation(LinearLayout.HORIZONTAL);
        timeRow.addView(timeButton("1 MIN", 60), new LinearLayout.LayoutParams(0, dp(48), 1f));
        timeRow.addView(timeButton("5 MIN", 300), new LinearLayout.LayoutParams(0, dp(48), 1f));
        timeRow.addView(timeButton("15 MIN", 900), new LinearLayout.LayoutParams(0, dp(48), 1f));
        time.addView(timeRow);
        content.addView(time);

        LinearLayout everyday = card();
        everyday.addView(sectionLabel("AJUSTES COTIDIANOS"));
        TextView everydayText = text(
                "Wi‑Fi · Bluetooth · Sonido · Pantalla · Brillo · Idioma · Fecha y hora · Batería · Almacenamiento",
                14, TEXT, false);
        LinearLayout.LayoutParams ed = new LinearLayout.LayoutParams(-1, -2);
        ed.setMargins(0, dp(8), 0, 0);
        everyday.addView(everydayText, ed);
        content.addView(everyday);

        LinearLayout service = card();
        service.addView(sectionLabel("SERVICIO DE PROTECCIÓN"));
        TextView serviceText = text(
                "Pangi usa Accesibilidad para reconocer la pantalla activa. No requiere root ni modifica Android.",
                13, MUTED, false);
        LinearLayout.LayoutParams st = new LinearLayout.LayoutParams(-1, -2);
        st.setMargins(0, dp(7), 0, dp(10));
        service.addView(serviceText, st);

        Button accessibility = actionButton("REVISAR ACCESIBILIDAD", true);
        accessibility.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        service.addView(accessibility);

        Button test = actionButton("PROBAR BLOQUEO DE PLAY STORE", false);
        test.setOnClickListener(v -> openPlayStore());
        service.addView(test);
        content.addView(service);

        LinearLayout uninstall = card();
        uninstall.addView(sectionLabel("PROTECCIÓN CONTRA DESINSTALACIÓN"));

        TextView uninstallText = text(
                "Activa la protección administrativa de Android. Mientras esté activa, Pangi no puede desinstalarse desde el acceso directo, la pantalla de inicio ni Ajustes.",
                13, MUTED, false);
        LinearLayout.LayoutParams ut = new LinearLayout.LayoutParams(-1, -2);
        ut.setMargins(0, dp(7), 0, dp(8));
        uninstall.addView(uninstallText, ut);

        uninstallStatus = text("", 15, TEXT, true);
        uninstall.addView(uninstallStatus);

        deviceAdminButton = actionButton("ACTIVAR PROTECCIÓN DE DESINSTALACIÓN", true);
        deviceAdminButton.setOnClickListener(v -> requestDeviceAdmin());
        uninstall.addView(deviceAdminButton);

        content.addView(uninstall);

        LinearLayout admin = card();
        admin.addView(sectionLabel("ADMINISTRACIÓN"));

        Button changePin = actionButton("CAMBIAR PIN", false);
        changePin.setOnClickListener(v -> showPinSetup(false));
        admin.addView(changePin);

        Button logout = actionButton("CERRAR SESIÓN ADMINISTRATIVA", false);
        logout.setOnClickListener(v -> {
            adminAuthenticated = false;
            buildAdminGate();
        });
        admin.addView(logout);
        content.addView(admin);

        TextView legal = text(
                "Pangi combina Accesibilidad con la protección administrativa de Android. Un restablecimiento de fábrica sigue siendo un mecanismo externo de recuperación.",
                11, Color.rgb(115, 128, 119), false);
        legal.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(dp(10), dp(2), dp(10), dp(12));
        content.addView(legal, lp);

        TextView foot = text("Pangi · Protección de acceso · 1.3", 11,
                Color.rgb(123, 136, 127), false);
        foot.setGravity(Gravity.CENTER_HORIZONTAL);
        content.addView(foot);

        setContentView(scroll);
        refreshStatus();
    }

    private LinearLayout toggleRow(String title, String subtitle, boolean checked, final int which) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), 0, dp(10));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);

        labels.addView(text(title, 16, TEXT, true));
        TextView sub = text(subtitle, 12, MUTED, false);
        LinearLayout.LayoutParams subP = new LinearLayout.LayoutParams(-1, -2);
        subP.setMargins(0, dp(3), dp(10), 0);
        labels.addView(sub, subP);

        row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1f));

        Switch sw = new Switch(this);
        sw.setChecked(checked);
        sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!adminAuthenticated) {
                buttonView.setChecked(!isChecked);
                buildAdminGate();
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
        b.setTextColor(TEXT);
        b.setBackground(outline(Color.WHITE, Color.rgb(210, 219, 213), 12));
        b.setOnClickListener(v -> {
            if (!adminAuthenticated) {
                buildAdminGate();
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
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(primary ? Color.WHITE : GREEN_2);
        b.setBackground(primary
                ? round(GREEN_2, 13)
                : outline(Color.WHITE, Color.rgb(207, 220, 211), 13));

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(52));
        p.setMargins(0, dp(10), 0, 0);
        b.setLayoutParams(p);
        return b;
    }

    private void refreshStatus() {
        if (!adminAuthenticated || protectionStatus == null) return;

        boolean enabled = isServiceEnabled();
        boolean unlocked = Prefs.isUnlocked(this);

        if (!enabled) {
            protectionStatus.setText("● Configuración pendiente");
            protectionStatus.setTextColor(Color.rgb(194, 93, 31));
        } else if (unlocked) {
            protectionStatus.setText("● Modo mantenimiento");
            protectionStatus.setTextColor(ORANGE);
        } else {
            protectionStatus.setText("● Protección activa");
            protectionStatus.setTextColor(Color.rgb(25, 125, 67));
        }

        serviceStatus.setText(enabled
                ? "Accesibilidad · activa"
                : "Accesibilidad · requiere activación");

        int sec = Prefs.getUnlockSeconds(this);
        String value = sec == 60 ? "1 minuto" : sec == 900 ? "15 minutos" : "5 minutos";
        timeoutStatus.setText("Acceso temporal · " + value);

        if (maintenanceButton != null) {
            maintenanceButton.setText(unlocked
                    ? "FINALIZAR MODO MANTENIMIENTO"
                    : "ACTIVAR MODO MANTENIMIENTO");
        }

        boolean adminActive = isDeviceAdminActive();
        if (uninstallStatus != null) {
            uninstallStatus.setText(adminActive
                    ? "● Protección del sistema activa"
                    : "● Aún puede desinstalarse");
            uninstallStatus.setTextColor(adminActive
                    ? Color.rgb(25, 125, 67)
                    : Color.rgb(194, 93, 31));
        }
        if (deviceAdminButton != null) {
            deviceAdminButton.setEnabled(!adminActive);
            deviceAdminButton.setText(adminActive
                    ? "PROTECCIÓN ACTIVA"
                    : "ACTIVAR PROTECCIÓN DE DESINSTALACIÓN");
        }
    }

    private boolean isDeviceAdminActive() {
        DevicePolicyManager dpm =
                (DevicePolicyManager)getSystemService(DEVICE_POLICY_SERVICE);
        ComponentName admin = new ComponentName(this, PangiDeviceAdminReceiver.class);
        return dpm != null && dpm.isAdminActive(admin);
    }

    private void requestDeviceAdmin() {
        if (isDeviceAdminActive()) {
            Toast.makeText(this, "La protección contra desinstalación ya está activa", Toast.LENGTH_SHORT).show();
            refreshStatus();
            return;
        }

        ComponentName admin = new ComponentName(this, PangiDeviceAdminReceiver.class);
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Activa esta protección para impedir que Pangi sea desinstalado sin autorización administrativa.");
        startActivity(intent);
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

    private void showPinSetup(final boolean first) {
        LinearLayout fields = new LinearLayout(this);
        fields.setOrientation(LinearLayout.VERTICAL);
        fields.setPadding(dp(22), dp(8), dp(22), 0);

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
                .setTitle(first ? "Crear PIN administrativo" : "Cambiar PIN")
                .setMessage(first
                        ? "Este PIN protegerá Pangi y todas las áreas sensibles."
                        : "Define una nueva clave administrativa.")
                .setView(fields)
                .setCancelable(!first)
                .setNegativeButton(first ? null : "Cancelar", null)
                .setPositiveButton("Guardar", null)
                .create();

        dialog.setOnShowListener(d ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String a = one.getText() == null ? "" : one.getText().toString();
                    String b = two.getText() == null ? "" : two.getText().toString();

                    if (a.length() < 4 || a.length() > 12 || !a.equals(b)) {
                        Toast.makeText(this,
                                "El PIN debe tener 4 a 12 dígitos y coincidir",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    Prefs.setPin(this, a);
                    Prefs.lock(this);
                    dialog.dismiss();

                    if (first) {
                        Toast.makeText(this,
                                "PIN creado. Ingresa el PIN para acceder a Pangi.",
                                Toast.LENGTH_LONG).show();
                        buildAdminGate();
                        requestDeviceAdmin();
                    } else {
                        Toast.makeText(this, "PIN actualizado", Toast.LENGTH_SHORT).show();
                    }
                }));

        dialog.show();
    }

    @Override public void onBackPressed() {
        finish();
    }
}
