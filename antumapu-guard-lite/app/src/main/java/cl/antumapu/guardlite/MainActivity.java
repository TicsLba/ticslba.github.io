package cl.antumapu.guardlite;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
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

    private int dp(int n) {
        return (int)(n * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
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
        TextView t = text(label, 11, Color.rgb(91,108,97), true);
        t.setLetterSpacing(0.09f);
        return t;
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

        TextView sub = text("Gestión institucional", 17, Color.rgb(214,228,219), false);
        sub.setGravity(Gravity.CENTER);
        root.addView(sub);

        TextView note = text("Configura el PIN administrativo para comenzar.", 14,
                Color.rgb(170,194,179), false);
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

        TextView brand = text("PANGI 2.0", 13, ORANGE, true);
        brand.setLetterSpacing(0.15f);
        brand.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2);
        bp.setMargins(0, dp(16), 0, dp(8));
        root.addView(brand, bp);

        TextView title = text("Administración protegida", 27, Color.WHITE, true);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView subtitle = text("Ingresa el PIN para gestionar las políticas de la tablet.",
                14, Color.rgb(197,216,204), false);
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
        pin.setHintTextColor(Color.rgb(127,139,131));
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

        setContentView(root);
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
        hero.setPadding(dp(22), dp(24), dp(22), dp(24));
        hero.setBackgroundColor(GREEN);
        hero.addView(logo(66));

        LinearLayout heroText = new LinearLayout(this);
        heroText.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams htp = new LinearLayout.LayoutParams(0, -2, 1f);
        htp.setMargins(dp(16), 0, 0, 0);
        TextView brand = text("PANGI 2.0", 12, ORANGE, true);
        brand.setLetterSpacing(0.14f);
        heroText.addView(brand);
        heroText.addView(text("Gestión institucional", 25, Color.WHITE, true));
        heroText.addView(text("Device Owner · Android Enterprise", 13,
                Color.rgb(190,211,198), false));
        hero.addView(heroText, htp);
        page.addView(hero);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(18), dp(18), dp(28));
        page.addView(content);

        boolean owner = PangiPolicyManager.isDeviceOwner(this);

        LinearLayout status = card();
        status.addView(sectionLabel("ESTADO"));
        TextView state = text(owner ? "● Gestión institucional activa" : "● Device Owner pendiente",
                20, owner ? Color.rgb(25,125,67) : Color.rgb(194,93,31), true);
        LinearLayout.LayoutParams stp = new LinearLayout.LayoutParams(-1, -2);
        stp.setMargins(0, dp(10), 0, dp(5));
        status.addView(state, stp);

        status.addView(text(owner
                        ? "Android aplica las políticas de Pangi a nivel del sistema."
                        : "La app está instalada, pero debe configurarse como Device Owner mediante el instalador.",
                14, MUTED, false));

        if (owner) {
            TextView anti = text("✓ Pangi no puede ser desinstalado por el usuario", 14, TEXT, true);
            LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-1, -2);
            ap.setMargins(0, dp(10), 0, 0);
            status.addView(anti, ap);
        }
        content.addView(status);

        LinearLayout policies = card();
        policies.addView(sectionLabel("POLÍTICAS"));

        policies.addView(policyToggle(
                "Bloquear Play Store",
                "Oculta la tienda. Las aplicaciones ya instaladas siguen funcionando.",
                Prefs.blockPlayStore(this), 1, owner));

        policies.addView(policyToggle(
                "Bloquear instalación externa",
                "Impide instalar APK desde fuentes desconocidas.",
                Prefs.blockUnknownSources(this), 2, owner));

        policies.addView(policyToggle(
                "Bloquear control de aplicaciones",
                "Impide desinstalar, inhabilitar o modificar aplicaciones desde Ajustes.",
                Prefs.blockAppsControl(this), 3, owner));

        content.addView(policies);

        LinearLayout free = card();
        free.addView(sectionLabel("AJUSTES QUE QUEDAN LIBRES"));
        TextView freeText = text(
                "Wi‑Fi · Bluetooth · Sonido · Pantalla · Brillo · Idioma · Fecha y hora · Batería · Almacenamiento",
                14, TEXT, false);
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
        });
        admin.addView(reapply);

        Button changePin = actionButton("CAMBIAR PIN", false);
        changePin.setOnClickListener(v -> showPinSetup());
        admin.addView(changePin);

        Button logout = actionButton("CERRAR SESIÓN", false);
        logout.setOnClickListener(v -> buildAdminGate());
        admin.addView(logout);

        content.addView(admin);

        TextView foot = text("Pangi 2.0 · Gestión institucional · sin root · sin Accesibilidad",
                11, Color.rgb(123,136,127), false);
        foot.setGravity(Gravity.CENTER_HORIZONTAL);
        content.addView(foot);

        setContentView(scroll);
    }

    private LinearLayout policyToggle(String title, String subtitle, boolean checked,
                                      final int which, boolean enabled) {
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
        sw.setEnabled(enabled);
        sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!PangiPolicyManager.isDeviceOwner(this)) {
                Toast.makeText(this, "Primero configura Pangi como Device Owner", Toast.LENGTH_SHORT).show();
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
        b.setTextSize(13);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(primary ? Color.WHITE : GREEN_2);
        b.setBackground(primary
                ? round(GREEN_2, 13)
                : outline(Color.WHITE, Color.rgb(207,220,211), 13));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(52));
        p.setMargins(0, dp(10), 0, 0);
        b.setLayoutParams(p);
        return b;
    }

    private void showPinSetup() {
        LinearLayout fields = new LinearLayout(this);
        fields.setOrientation(LinearLayout.VERTICAL);
        fields.setPadding(dp(22), dp(8), dp(22), 0);

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
                        ? "Este PIN protege el panel de administración de Pangi."
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
