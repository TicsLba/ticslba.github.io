package cl.antumapu.guard;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class LockActivity extends Activity {
    private EditText pin;

    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (!Prefs.hasPin(this)) {
            Toast.makeText(this, "Configura primero una clave administrativa", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        buildUi();
    }

    private GradientDrawable box(int color, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp((int)radius));
        return g;
    }

    private TextView label(String text, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER_HORIZONTAL);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(28), dp(36), dp(28), dp(36));
        root.setBackgroundColor(Color.rgb(15, 43, 29));

        TextView shield = label("ANTUMAPU GUARD", 13, Color.rgb(229,138,43), true);
        shield.setLetterSpacing(0.08f);
        root.addView(shield);

        TextView title = label("Acceso administrativo", 28, Color.WHITE, true);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, -2);
        tp.setMargins(0, dp(14), 0, dp(8));
        root.addView(title, tp);

        TextView sub = label("Esta sección está protegida.\nIngresa la clave para continuar.", 16,
                Color.rgb(211, 224, 214), false);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.setMargins(0, 0, 0, dp(24));
        root.addView(sub, sp);

        pin = new EditText(this);
        pin.setHint("PIN administrativo");
        pin.setSingleLine(true);
        pin.setGravity(Gravity.CENTER);
        pin.setTextSize(20);
        pin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        pin.setTextColor(Color.rgb(25, 40, 30));
        pin.setHintTextColor(Color.rgb(110, 120, 113));
        pin.setBackground(box(Color.WHITE, 14));
        pin.setPadding(dp(16), dp(14), dp(16), dp(14));
        root.addView(pin, new LinearLayout.LayoutParams(-1, -2));

        Button ok = new Button(this);
        ok.setText("DESBLOQUEAR");
        ok.setTextSize(15);
        ok.setTextColor(Color.WHITE);
        ok.setBackground(box(Color.rgb(229,138,43), 14));
        ok.setOnClickListener(v -> verify());
        LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(-1, dp(54));
        op.setMargins(0, dp(18), 0, dp(8));
        root.addView(ok, op);

        Button cancel = new Button(this);
        cancel.setText("CANCELAR");
        cancel.setTextColor(Color.rgb(210,224,214));
        cancel.setBackgroundColor(Color.TRANSPARENT);
        cancel.setOnClickListener(v -> {
            Prefs.lock(this);
            goHome();
            finish();
        });
        root.addView(cancel, new LinearLayout.LayoutParams(-1, dp(50)));

        TextView foot = label("El acceso volverá a bloquearse automáticamente.", 12,
                Color.rgb(153,178,160), false);
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(-1, -2);
        fp.setMargins(0, dp(18), 0, 0);
        root.addView(foot, fp);

        setContentView(root);
        pin.requestFocus();
    }

    private void verify() {
        String value = pin.getText() == null ? "" : pin.getText().toString();
        if (Prefs.checkPin(this, value)) {
            Prefs.unlock(this);
            RootPolicy.applyUnlocked(this);
            RelockReceiver.schedule(this);
            Toast.makeText(this, "Acceso autorizado temporalmente", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            pin.setText("");
            Toast.makeText(this, "Clave incorrecta", Toast.LENGTH_SHORT).show();
        }
    }

    private void goHome() {
        Intent home = new Intent(Intent.ACTION_MAIN);
        home.addCategory(Intent.CATEGORY_HOME);
        home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(home);
    }

    @Override public void onBackPressed() {
        Prefs.lock(this);
        goHome();
        finish();
    }
}
