package cl.antumapu.guardlite;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class LockActivity extends Activity {
    private static final int GREEN = Color.rgb(14, 47, 32);
    private static final int ORANGE = Color.rgb(229, 138, 43);
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
        root.setPadding(dp(32), dp(38), dp(32), dp(38));
        root.setBackgroundColor(GREEN);

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_pangi);
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        icon.setContentDescription("Pangi");
        root.addView(icon, new LinearLayout.LayoutParams(dp(92), dp(92)));

        TextView brand = label("PANGI", 12, ORANGE, true);
        brand.setLetterSpacing(0.16f);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2);
        bp.setMargins(0, dp(14), 0, dp(8));
        root.addView(brand, bp);

        TextView title = label("Acceso protegido", 28, Color.WHITE, true);
        root.addView(title);

        TextView area = label(reason, 16, Color.rgb(219, 230, 222), true);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-1, -2);
        ap.setMargins(0, dp(8), 0, dp(7));
        root.addView(area, ap);

        TextView sub = label("Ingresa el PIN administrativo para continuar.", 14,
                Color.rgb(182, 204, 190), false);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.setMargins(0, 0, 0, dp(22));
        root.addView(sub, sp);

        pin = new EditText(this);
        pin.setHint("PIN administrativo");
        pin.setSingleLine(true);
        pin.setGravity(Gravity.CENTER);
        pin.setTextSize(20);
        pin.setTextColor(Color.rgb(24, 38, 29));
        pin.setHintTextColor(Color.rgb(120, 128, 122));
        pin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        pin.setBackground(round(Color.WHITE, 15));
        pin.setPadding(dp(16), dp(14), dp(16), dp(14));
        root.addView(pin, new LinearLayout.LayoutParams(-1, dp(58)));

        Button unlock = new Button(this);
        unlock.setText("AUTORIZAR ACCESO");
        unlock.setTextColor(Color.WHITE);
        unlock.setTextSize(14);
        unlock.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        unlock.setBackground(round(ORANGE, 15));
        unlock.setOnClickListener(v -> verify());
        LinearLayout.LayoutParams up = new LinearLayout.LayoutParams(-1, dp(56));
        up.setMargins(0, dp(16), 0, dp(6));
        root.addView(unlock, up);

        Button cancel = new Button(this);
        cancel.setText("VOLVER");
        cancel.setTextColor(Color.rgb(210, 225, 214));
        cancel.setBackgroundColor(Color.TRANSPARENT);
        cancel.setOnClickListener(v -> leaveProtectedArea());
        root.addView(cancel, new LinearLayout.LayoutParams(-1, dp(48)));

        TextView foot = label(
                "El acceso es temporal y se bloquea nuevamente de forma automática.",
                12, Color.rgb(145, 171, 153), false);
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(-1, -2);
        fp.setMargins(0, dp(12), 0, 0);
        root.addView(foot, fp);

        setContentView(root);

        pin.requestFocus();
        pin.postDelayed(() -> {
            InputMethodManager imm =
                    (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
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
