package cl.antumapu.guardlite;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends Activity {
    private static final int GREEN = Color.rgb(0, 90, 18);
    private static final int GREEN_DARK = Color.rgb(0, 58, 12);
    private static final int GREEN_SOFT = Color.rgb(232, 244, 234);
    private static final int ORANGE = Color.rgb(244, 90, 30);
    private static final int ORANGE_SOFT = Color.rgb(255, 239, 233);
    private static final int BG = Color.rgb(247, 249, 247);
    private static final int TEXT = Color.rgb(25, 48, 31);
    private static final int MUTED = Color.rgb(101, 116, 106);
    private static final int LINE = Color.rgb(224, 231, 225);
    private static final int GOOD = Color.rgb(20, 133, 60);
    private static final int WARN = Color.rgb(196, 101, 25);

    private boolean adminAuthenticated = false;

    private int dp(int n) {
        return (int)(n * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);

        if (PangiPolicyManager.isDeviceOwner(this)) {
            PangiPolicyManager.clearLegacyLauncherOverride(this);
        }

        if (!Prefs.hasPin(this)) {
            buildWelcome();
            showPinSetup();
        } else {
            buildAdminGate();
        }
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
        t.setLineSpacing(0f, 1.08f);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private Bitmap loadInstitutionalLogo() {
        try {
            File dir = getExternalFilesDir(null);
            if (dir == null) return null;
            File png = new File(dir, "logo_antumapu.png");
            if (png.exists()) return BitmapFactory.decodeFile(png.getAbsolutePath());
        } catch (Exception ignored) {
        }
        return null;
    }

    private ImageView logo(int sizeDp) {
        ImageView i = new ImageView(this);
        Bitmap bitmap = loadInstitutionalLogo();
        if (bitmap != null) i.setImageBitmap(bitmap);
        else i.setImageResource(R.drawable.ic_pangi);
        i.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        i.setContentDescription("Liceo Bicentenario Antumapu");
        i.setLayoutParams(new LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp)));
        return i;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(20), dp(19), dp(20), dp(19));
        c.setBackground(outline(Color.WHITE, LINE, 20));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, 0, 0, dp(14));
        c.setLayoutParams(p);
        return c;
    }

    private TextView sectionLabel(String label) {
        TextView t = text(label, 11, MUTED, true);
        t.setLetterSpacing(0.10f);
        return t;
    }

    private TextView pill(String value, boolean good) {
        TextView p = text(value, 10, good ? GREEN_DARK : WARN, true);
        p.setLetterSpacing(0.08f);
        p.setGravity(Gravity.CENTER);
        p.setPadding(dp(10), dp(5), dp(10), dp(5));
        p.setBackground(round(good ? GREEN_SOFT : ORANGE_SOFT, 50));
        return p;
    }

    private void buildWelcome() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(34), dp(36), dp(34), dp(36));
        root.setBackgroundColor(BG);

        root.addView(logo(150));

        TextView title = text("Pangi", 42, GREEN, true);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, -2);
        tp.setMargins(0, dp(18), 0, dp(4));
        root.addView(title, tp);

        TextView v = text("GESTIÓN INSTITUCIONAL · 3.1", 11, ORANGE, true);
        v.setLetterSpacing(0.12f);
        v.setGravity(Gravity.CENTER);
        root.addView(v);

        TextView note = text(
                "Configura el PIN administrativo. Pangi protegerá la tablet sin reemplazar el launcher del dispositivo.",
                14, MUTED, false);
        note.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, -2);
        np.setMargins(dp(6), dp(24), dp(6), 0);
        root.addView(note, np);

        setContentView(root);
    }

    private void buildAdminGate() {
        adminAuthenticated = false;

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(30), dp(34), dp(30), dp(34));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -1));

        root.addView(logo(126));

        TextView product = text("Pangi", 38, GREEN, true);
        product.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-1, -2);
        pp.setMargins(0, dp(14), 0, 0);
        root.addView(product, pp);

        TextView brand = text("ADMINISTRACIÓN · 3.1", 11, ORANGE, true);
        brand.setLetterSpacing(0.13f);
        brand.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2);
        bp.setMargins(0, dp(3), 0, dp(22));
        root.addView(brand, bp);

        LinearLayout gate = card();
        gate.setPadding(dp(22), dp(22), dp(22), dp(22));

        TextView title = text("Panel protegido", 23, TEXT, true);
        gate.addView(title);

        TextView subtitle = text(
                "Ingresa el PIN para administrar las políticas institucionales.",
                13, MUTED, false);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.setMargins(0, dp(5), 0, dp(18));
        gate.addView(subtitle, sp);

        final EditText pin = new EditText(this);
        pin.setHint("PIN administrativo");
        pin.setSingleLine(true);
        pin.setGravity(Gravity.CENTER);
        pin.setTextSize(18);
        pin.setTextColor(TEXT);
        pin.setHintTextColor(Color.rgb(140, 151, 143));
        pin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        pin.setBackground(outline(Color.WHITE, LINE, 14));
        pin.setPadding(dp(14), dp(13), dp(14), dp(13));
        gate.addView(pin, new LinearLayout.LayoutParams(-1, dp(56)));

        Button enter = actionButton("INGRESAR", true);
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
        gate.addView(enter);
        root.addView(gate, new LinearLayout.LayoutParams(-1, -2));

        TextView footer = text(
                "Desarrollado por la Coordinación Informática del Liceo Bicentenario Antumapu\n© Eduardo Pérez González",
                10, MUTED, false);
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(-1, -2);
        fp.setMargins(0, dp(8), 0, 0);
        root.addView(footer, fp);

        setContentView(scroll);
        pin.requestFocus();
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
        hero.setPadding(dp(20), dp(18), dp(20), dp(18));
        hero.setBackgroundColor(Color.WHITE);

        hero.addView(logo(64));

        LinearLayout heroText = new LinearLayout(this);
        heroText.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams htp = new LinearLayout.LayoutParams(0, -2, 1f);
        htp.setMargins(dp(14), 0, 0, 0);

        TextView kicker = text("PANGI 3.1", 11, ORANGE, true);
        kicker.setLetterSpacing(0.13f);
        heroText.addView(kicker);
        heroText.addView(text("Gestión institucional", 24, TEXT, true));
        heroText.addView(text("Device Owner · Launcher OEM intacto", 12, MUTED, false));
        hero.addView(heroText, htp);

        boolean owner = PangiPolicyManager.isDeviceOwner(this);
        hero.addView(pill(owner ? "ACTIVO" : "PENDIENTE", owner));

        page.addView(hero);

        View heroLine = new View(this);
        heroLine.setBackgroundColor(ORANGE);
        page.addView(heroLine, new LinearLayout.LayoutParams(-1, dp(3)));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(18), dp(16), dp(30));
        page.addView(content);

        LinearLayout status = card();
        status.addView(sectionLabel("ESTADO DEL DISPOSITIVO"));

        TextView state = text(
                owner ? "Protección institucional activa" : "Device Owner pendiente",
                21, owner ? GREEN : WARN, true);
        LinearLayout.LayoutParams stp = new LinearLayout.LayoutParams(-1, -2);
        stp.setMargins(0, dp(8), 0, dp(4));
        status.addView(state, stp);

        status.addView(text(
                owner
                        ? "Las políticas son aplicadas directamente por Android."
                        : "Instala y provisiona Pangi como Device Owner para activar las protecciones.",
                13, MUTED, false));

        LinearLayout deviceRow = new LinearLayout(this);
        deviceRow.setOrientation(LinearLayout.HORIZONTAL);
        deviceRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams drp = new LinearLayout.LayoutParams(-1, -2);
        drp.setMargins(0, dp(16), 0, 0);
        status.addView(deviceRow, drp);

        LinearLayout deviceText = new LinearLayout(this);
        deviceText.setOrientation(LinearLayout.VERTICAL);
        deviceText.addView(text(Build.MANUFACTURER + " " + Build.MODEL, 14, TEXT, true));
        deviceText.addView(text("Android " + Build.VERSION.RELEASE + " · API " + Build.VERSION.SDK_INT,
                12, MUTED, false));
        deviceRow.addView(deviceText, new LinearLayout.LayoutParams(0, -2, 1f));

        if (owner) deviceRow.addView(pill("PROTEGIDO", true));
        content.addView(status);

        LinearLayout launcher = card();
        launcher.addView(sectionLabel("INTEGRACIÓN CON ANDROID"));

        boolean nativeHome = PangiPolicyManager.isNativeHomeIntact(this);
        String homePkg = PangiPolicyManager.getCurrentHomePackage(this);

        LinearLayout homeTitle = new LinearLayout(this);
        homeTitle.setOrientation(LinearLayout.HORIZONTAL);
        homeTitle.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.setMargins(0, dp(9), 0, dp(5));
        launcher.addView(homeTitle, hp);

        TextView homeText = text("Launcher del dispositivo", 17, TEXT, true);
        homeTitle.addView(homeText, new LinearLayout.LayoutParams(0, -2, 1f));
        homeTitle.addView(pill(nativeHome ? "INTACTO" : "REVISAR", nativeHome));

        launcher.addView(text(
                "Pangi 3.1 no declara CATEGORY_HOME, no reemplaza el escritorio y no intercepta el botón Inicio.",
                13, MUTED, false));

        TextView pkg = text(
                homePkg.length() > 0 ? "HOME actual: " + homePkg : "HOME actual: no identificado",
                11, GREEN_DARK, true);
        LinearLayout.LayoutParams pkgp = new LinearLayout.LayoutParams(-1, -2);
        pkgp.setMargins(0, dp(10), 0, 0);
        launcher.addView(pkg, pkgp);

        if (owner) {
            Button cleanLegacy = actionButton("LIMPIAR CONFIGURACIÓN HOME DE PANGI 2.x", false);
            cleanLegacy.setOnClickListener(v -> {
                PangiPolicyManager.clearLegacyLauncherOverride(this);
                Toast.makeText(this, "Preferencias HOME antiguas eliminadas", Toast.LENGTH_SHORT).show();
                buildDashboard();
            });
            launcher.addView(cleanLegacy);
        }

        content.addView(launcher);

        LinearLayout fleet = card();
        fleet.addView(sectionLabel("CONSOLA DE FLOTA"));

        boolean fleetConfigured = Prefs.consoleConfigured(this);
        boolean fleetOn = Prefs.fleetEnabled(this);

        LinearLayout fleetTitle = new LinearLayout(this);
        fleetTitle.setOrientation(LinearLayout.HORIZONTAL);
        fleetTitle.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams ftp = new LinearLayout.LayoutParams(-1, -2);
        ftp.setMargins(0, dp(9), 0, dp(5));
        fleet.addView(fleetTitle, ftp);

        TextView fleetName = text("Monitoreo institucional", 17, TEXT, true);
        fleetTitle.addView(fleetName, new LinearLayout.LayoutParams(0, -2, 1f));
        fleetTitle.addView(pill(fleetOn ? "EN LÍNEA" : (fleetConfigured ? "LISTO" : "SIN CONFIGURAR"),
                fleetOn || fleetConfigured));

        fleet.addView(text(
                "Reporta estado de la tablet, batería y ubicación del dispositivo institucional. " +
                        "La ubicación se muestra de forma transparente mediante una notificación persistente.",
                13, MUTED, false));

        TextView deviceId = text("ID: " + Prefs.deviceId(this), 11, GREEN_DARK, true);
        LinearLayout.LayoutParams dip = new LinearLayout.LayoutParams(-1, -2);
        dip.setMargins(0, dp(9), 0, 0);
        fleet.addView(deviceId, dip);

        long hb = Prefs.lastHeartbeat(this);
        String hbText = hb == 0L ? "Sin heartbeat registrado"
                : "Último heartbeat: " +
                new java.text.SimpleDateFormat("dd/MM HH:mm:ss", java.util.Locale.getDefault())
                        .format(new java.util.Date(hb));
        TextView heartbeatInfo = text(hbText, 12, MUTED, false);
        LinearLayout.LayoutParams hip = new LinearLayout.LayoutParams(-1, -2);
        hip.setMargins(0, dp(5), 0, 0);
        fleet.addView(heartbeatInfo, hip);

        if (Prefs.hasLocation(this)) {
            long locAt = Prefs.lastLocationAt(this);
            String locText = "Última ubicación: " +
                    String.format(java.util.Locale.US, "%.5f, %.5f", Prefs.lastLat(this), Prefs.lastLon(this)) +
                    " · ±" + Math.round(Prefs.lastAccuracy(this)) + " m";
            if (locAt > 0L) {
                locText += " · " +
                        new java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault())
                                .format(new java.util.Date(locAt));
            }
            TextView locationInfo = text(locText, 12, MUTED, false);
            LinearLayout.LayoutParams lip = new LinearLayout.LayoutParams(-1, -2);
            lip.setMargins(0, dp(4), 0, 0);
            fleet.addView(locationInfo, lip);
        }

        LinearLayout monitorRow = new LinearLayout(this);
        monitorRow.setOrientation(LinearLayout.HORIZONTAL);
        monitorRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams mrp = new LinearLayout.LayoutParams(-1, -2);
        mrp.setMargins(0, dp(13), 0, 0);
        fleet.addView(monitorRow, mrp);

        LinearLayout monitorLabels = new LinearLayout(this);
        monitorLabels.setOrientation(LinearLayout.VERTICAL);
        monitorLabels.addView(text("Monitoreo remoto", 15, TEXT, true));
        monitorLabels.addView(text("Heartbeat cada 60 s · GPS periódico · comandos remotos",
                11, MUTED, false));
        monitorRow.addView(monitorLabels, new LinearLayout.LayoutParams(0, -2, 1f));

        Switch fleetSwitch = new Switch(this);
        fleetSwitch.setChecked(fleetOn);
        fleetSwitch.setEnabled(owner && fleetConfigured);
        fleetSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !Prefs.consoleConfigured(this)) {
                buttonView.setChecked(false);
                showFleetSetup();
                return;
            }
            PangiPolicyManager.setFleetMonitoring(this, isChecked);
            Toast.makeText(this,
                    isChecked ? "Monitoreo institucional activado" : "Monitoreo detenido",
                    Toast.LENGTH_SHORT).show();
            buildDashboard();
        });
        monitorRow.addView(fleetSwitch);

        Button configureFleet = actionButton(
                fleetConfigured ? "EDITAR CONSOLA" : "CONFIGURAR CONSOLA", false);
        configureFleet.setOnClickListener(v -> showFleetSetup());
        fleet.addView(configureFleet);

        Button testFleet = actionButton("PROBAR CONEXIÓN", false);
        testFleet.setEnabled(fleetConfigured);
        testFleet.setOnClickListener(v -> {
            Toast.makeText(this, "Probando conexión…", Toast.LENGTH_SHORT).show();
            new Thread(() -> {
                boolean ok = FleetClient.test(MainActivity.this);
                runOnUiThread(() -> Toast.makeText(
                        MainActivity.this,
                        ok ? "Consola conectada correctamente" : "No fue posible conectar con la consola",
                        Toast.LENGTH_LONG).show());
            }, "PangiFleetTest").start();
        });
        fleet.addView(testFleet);

        TextView privacy = text(
                "Privacidad: Pangi identifica la tablet, no al estudiante. No registra micrófono, cámara, " +
                        "mensajes ni contenido personal.",
                11, MUTED, false);
        LinearLayout.LayoutParams prp = new LinearLayout.LayoutParams(-1, -2);
        prp.setMargins(0, dp(12), 0, 0);
        fleet.addView(privacy, prp);

        content.addView(fleet);

        LinearLayout policies = card();
        policies.addView(sectionLabel("POLÍTICAS DE PROTECCIÓN"));

        policies.addView(policyToggle(
                "Play Store",
                "Oculta la tienda institucionalmente. Las apps instaladas continúan funcionando.",
                Prefs.blockPlayStore(this), 1, owner));

        policies.addView(divider());

        policies.addView(policyToggle(
                "Instalación externa",
                "Bloquea APK y fuentes desconocidas mediante políticas de Android.",
                Prefs.blockUnknownSources(this), 2, owner));

        policies.addView(divider());

        policies.addView(policyToggle(
                "Control de aplicaciones",
                "Bloquea desinstalación, inhabilitación y cambios desde Ajustes.",
                Prefs.blockAppsControl(this), 3, owner));

        content.addView(policies);

        LinearLayout identity = card();
        identity.addView(sectionLabel("IDENTIDAD INSTITUCIONAL"));
        TextView identityTitle = text("Presentación Pangi", 18, TEXT, true);
        LinearLayout.LayoutParams itp = new LinearLayout.LayoutParams(-1, -2);
        itp.setMargins(0, dp(8), 0, dp(4));
        identity.addView(identityTitle, itp);

        identity.addView(text(
                Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                        ? "En esta versión de Android, Pangi puede mostrar una presentación breve al completar el arranque sin cambiar el launcher."
                        : "Android 10+ restringe las Activities iniciadas desde segundo plano. Pangi no fuerza una pantalla tardía ni reemplaza el launcher.",
                13, MUTED, false));

        Button preview = actionButton("VER PRESENTACIÓN", false);
        preview.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, SplashActivity.class)));
        identity.addView(preview);
        content.addView(identity);

        LinearLayout free = card();
        free.addView(sectionLabel("USO COTIDIANO"));
        TextView freeText = text(
                "Wi‑Fi · Bluetooth · Sonido · Pantalla · Brillo · Idioma · Fecha y hora · Batería · Almacenamiento",
                13, TEXT, false);
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(-1, -2);
        fp.setMargins(0, dp(8), 0, 0);
        free.addView(freeText, fp);
        content.addView(free);

        LinearLayout admin = card();
        admin.addView(sectionLabel("ADMINISTRACIÓN"));

        Button reapply = actionButton("REAPLICAR POLÍTICAS", true);
        reapply.setEnabled(owner);
        reapply.setOnClickListener(v -> {
            PangiPolicyManager.applyDefaults(this);
            Toast.makeText(this, "Políticas reaplicadas", Toast.LENGTH_SHORT).show();
            buildDashboard();
        });
        admin.addView(reapply);

        Button changePin = actionButton("CAMBIAR PIN", false);
        changePin.setOnClickListener(v -> showPinSetup());
        admin.addView(changePin);

        Button logout = actionButton("BLOQUEAR PANEL", false);
        logout.setOnClickListener(v -> buildAdminGate());
        admin.addView(logout);

        content.addView(admin);

        TextView foot = text(
                "Pangi 3.1 · Gestión institucional · Launcher OEM intacto · Sin root\n" +
                        "Desarrollado por la Coordinación Informática del Liceo Bicentenario Antumapu\n" +
                        "© Eduardo Pérez González",
                10, Color.rgb(119, 131, 122), false);
        foot.setGravity(Gravity.CENTER_HORIZONTAL);
        content.addView(foot);

        setContentView(scroll);
    }

    private View divider() {
        View v = new View(this);
        v.setBackgroundColor(LINE);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(1));
        p.setMargins(0, dp(2), 0, dp(2));
        v.setLayoutParams(p);
        return v;
    }

    private LinearLayout policyToggle(String title, String subtitle, boolean checked,
                                      final int which, boolean enabled) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), 0, dp(10));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text(title, 15, TEXT, true));

        TextView sub = text(subtitle, 12, MUTED, false);
        LinearLayout.LayoutParams subP = new LinearLayout.LayoutParams(-1, -2);
        subP.setMargins(0, dp(3), dp(12), 0);
        labels.addView(sub, subP);

        row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1f));

        Switch sw = new Switch(this);
        sw.setChecked(checked);
        sw.setEnabled(enabled);
        sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!PangiPolicyManager.isDeviceOwner(this)) {
                Toast.makeText(this, "Pangi debe ser Device Owner", Toast.LENGTH_SHORT).show();
                return;
            }

            if (which == 1) PangiPolicyManager.setPlayStoreBlocked(this, isChecked);
            if (which == 2) PangiPolicyManager.setUnknownSourcesBlocked(this, isChecked);
            if (which == 3) PangiPolicyManager.setAppsControlBlocked(this, isChecked);
        });
        row.addView(sw);
        return row;
    }

    private Button actionButton(String label, boolean primary) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(12);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(primary ? Color.WHITE : GREEN);
        b.setAllCaps(false);
        b.setBackground(primary
                ? round(GREEN, 14)
                : outline(Color.WHITE, Color.rgb(196, 213, 199), 14));

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(52));
        p.setMargins(0, dp(11), 0, 0);
        b.setLayoutParams(p);
        return b;
    }

    private void showFleetSetup() {
        LinearLayout fields = new LinearLayout(this);
        fields.setOrientation(LinearLayout.VERTICAL);
        fields.setPadding(dp(20), dp(6), dp(20), 0);

        final EditText url = new EditText(this);
        url.setHint("URL HTTPS de Pangi Console");
        url.setSingleLine(true);
        url.setText(Prefs.consoleUrl(this));
        url.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_VARIATION_URI);

        final EditText token = new EditText(this);
        token.setHint("Token de dispositivos");
        token.setSingleLine(true);
        token.setText(Prefs.consoleToken(this));
        token.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        final EditText label = new EditText(this);
        label.setHint("Alias, por ejemplo TAB-SAMSUNG-01");
        label.setSingleLine(true);
        label.setText(Prefs.deviceLabel(this));

        fields.addView(url);
        fields.addView(token);
        fields.addView(label);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Pangi Console")
                .setMessage("Vincula esta tablet a la consola institucional. La URL debe usar HTTPS.")
                .setView(fields)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Guardar", null)
                .create();

        dialog.setOnShowListener(d ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String u = url.getText() == null ? "" : url.getText().toString().trim();
                    String t = token.getText() == null ? "" : token.getText().toString().trim();
                    String l = label.getText() == null ? "" : label.getText().toString().trim();

                    if (!u.startsWith("https://")) {
                        Toast.makeText(this, "La consola debe usar HTTPS", Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (t.length() < 12) {
                        Toast.makeText(this, "Token de consola inválido", Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (l.length() == 0) l = Build.MANUFACTURER + " " + Build.MODEL;

                    Prefs.setConsoleConfig(this, u, t, l);
                    dialog.dismiss();

                    if (Prefs.fleetEnabled(this)) {
                        PangiPolicyManager.setFleetMonitoring(this, true);
                    }

                    Toast.makeText(this, "Consola guardada", Toast.LENGTH_SHORT).show();
                    buildDashboard();
                }));

        dialog.show();
    }

    private void showPinSetup() {
        LinearLayout fields = new LinearLayout(this);
        fields.setOrientation(LinearLayout.VERTICAL);
        fields.setPadding(dp(20), dp(6), dp(20), 0);

        final EditText one = new EditText(this);
        one.setHint(Prefs.hasPin(this) ? "Nuevo PIN" : "Crea un PIN de 4 a 12 dígitos");
        one.setSingleLine(true);
        one.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);

        final EditText two = new EditText(this);
        two.setHint("Repite el PIN");
        two.setSingleLine(true);
        two.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);

        fields.addView(one);
        fields.addView(two);

        boolean first = !Prefs.hasPin(this);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(first ? "Crear PIN administrativo" : "Cambiar PIN")
                .setMessage(first
                        ? "Este PIN protege el panel administrativo de Pangi."
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
                    dialog.dismiss();

                    if (PangiPolicyManager.isDeviceOwner(this)) {
                        PangiPolicyManager.applyDefaults(this);
                    }

                    Toast.makeText(this,
                            first ? "PIN creado" : "PIN actualizado",
                            Toast.LENGTH_SHORT).show();
                    buildAdminGate();
                }));

        dialog.show();
    }

    @Override public void onBackPressed() {
        finish();
    }
}
