package cl.antumapu.guardlite;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class LockActivity extends Activity {
    private EditText pin;

    private int dp(int n) {
        return (int)(n * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (!Prefs.hasPin(this)) {
            finish();
            return;
        }
        buildUi();
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radius));
        return g;
    }

    private TextView label(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setGravity(Gravity.CENTER_HORIZONTAL);
        return t;
    }

    private void buildUi() {
        String reason = getIntent().getStringExtra("reason");
        if (reason == null || reason.trim().length() == 0) reason = "Área protegida";

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(30), dp(42), dp(30), dp(42));
        root.setBackgroundColor(Color.rgb(15, 43, 29));

        TextView brand = label("ANTUMAPU GUARD LITE", 12, Color.rgb(229,138,43), true);
        brand.setLetterSpacing(0.08f);
        root.addView(brand);

        TextView title = label("Acceso protegido", 28, Color.WHITE, true);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, -2);
        tp.setMargins(0, dp(14), 0, dp(8));
        root.addView(title, tp);

        TextView area = label(reason, 16, Color.rgb(219,230,222), true);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-1, -2);
        ap.setMargins(0, 0, 0, dp(8));
        root.addView(area, ap);

        TextView sub = label("Ingresa el PIN administrativo para continuar.", 14,
                Color.rgb(180,201,187), false);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.setMargins(0, 0, 0, dp(24));
        root.addView(sub, sp);

        pin = new EditText(this);
        pin.setHint("PIN administrativo");
        pin.setSingleLine(true);
        pin.setGravity(Gravity.CENTER);
        pin.setTextSize(20);
        pin.setTextColor(Color.rgb(24,38,29));
        pin.setHintTextColor(Color.rgb(120,128,122));
        pin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        pin.setBackground(round(Color.WHITE, 14));
        pin.setPadding(dp(14), dp(14), dp(14), dp(14));
        root.addView(pin, new LinearLayout.LayoutParams(-1, -2));

        Button unlock = new Button(this);
        unlock.setText("DESBLOQUEAR");
        unlock.setTextColor(Color.WHITE);
        unlock.setTextSize(15);
        unlock.setBackground(round(Color.rgb(229,138,43), 14));
        unlock.setOnClickListener(v -> verify());
        LinearLayout.LayoutParams up = new LinearLayout.LayoutParams(-1, dp(54));
        up.setMargins(0, dp(18), 0, dp(6));
        root.addView(unlock, up);

        Button cancel = new Button(this);
        cancel.setText("VOLVER");
        cancel.setTextColor(Color.rgb(210,225,214));
        cancel.setBackgroundColor(Color.TRANSPARENT);
        cancel.setOnClickListener(v -> leaveProtectedArea());
        root.addView(cancel, new LinearLayout.LayoutParams(-1, dp(48)));

        TextView foot = label("El permiso será temporal y se cerrará automáticamente.", 12,
                Color.rgb(145,171,153), false);
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(-1, -2);
        fp.setMargins(0, dp(14), 0, 0);
        root.addView(foot, fp);

        setContentView(root);

        pin.requestFocus();
        pin.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(pin, InputMethodManager.SHOW_IMPLICIT);
        }, 180);
    }

    private void verify() {
        String value = pin.getText() == null ? "" : pin.getText().toString();
        if (Prefs.checkPin(this, value)) {
            Prefs.unlock(this);
            Toast.makeText(this, "Acceso autorizado temporalmente", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            pin.setText("");
            Toast.makeText(this, "PIN incorrecto", Toast.LENGTH_SHORT).show();
        }
    }

    private void leaveProtectedArea() {
        Prefs.lock(this);
        Intent home = new Intent(Intent.ACTION_MAIN);
        home.addCategory(Intent.CATEGORY_HOME);
        home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(home);
        finish();
    }

    @Override public void onBackPressed() {
        leaveProtectedArea();
    }
}
