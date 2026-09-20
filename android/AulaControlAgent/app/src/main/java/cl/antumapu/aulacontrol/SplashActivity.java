package cl.antumapu.aulacontrol;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SplashActivity extends Activity {
    private static final int NAVY = Color.rgb(18, 35, 67);
    private static final int COBALT = Color.rgb(49, 89, 255);
    private static final int TEAL = Color.rgb(32, 199, 164);
    private static final int SOFT = Color.rgb(221, 230, 255);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        BootGateScheduler.cancel(this);
        getWindow().setStatusBarColor(NAVY);
        getWindow().setNavigationBarColor(NAVY);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(28), dp(28), dp(28), dp(28));
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{NAVY, Color.rgb(31, 55, 111), COBALT});
        root.setBackground(bg);

        TextView chip = text("VERIFICACIÓN DE SEGURIDAD", 11, true, TEAL);
        chip.setLetterSpacing(.10f);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-2, -2);
        cp.bottomMargin = dp(18);
        root.addView(chip, cp);

        LinearLayout mark = new LinearLayout(this);
        mark.setGravity(Gravity.CENTER);
        GradientDrawable circle = new GradientDrawable();
        circle.setColor(Color.WHITE);
        circle.setCornerRadius(dp(28));
        mark.setBackground(circle);
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_tablet_school_mark);
        mark.addView(logo, new LinearLayout.LayoutParams(dp(82), dp(82)));
        root.addView(mark, new LinearLayout.LayoutParams(dp(108), dp(108)));

        TextView title = text("Tablet Escolar", 31, true, Color.WHITE);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-2, -2);
        tp.topMargin = dp(22);
        root.addView(title, tp);

        TextView sub = text("Acceso seguro al dispositivo institucional", 15, true, SOFT);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-2, -2);
        sp.topMargin = dp(8);
        root.addView(sub, sp);

        TextView security = text(
                "Por seguridad, Android se habilita sólo después de identificar al usuario y preparar una sesión protegida.",
                13, false, Color.rgb(236, 241, 255));
        security.setGravity(Gravity.CENTER);
        security.setLineSpacing(0, 1.10f);
        LinearLayout.LayoutParams secp = new LinearLayout.LayoutParams(-1, -2);
        secp.topMargin = dp(18);
        secp.leftMargin = dp(12);
        secp.rightMargin = dp(12);
        root.addView(security, secp);

        TextView privacy = text(
                "No captura, guarda ni entrega contraseñas personales, PIN ni claves secretas de otras aplicaciones.",
                12, false, Color.rgb(207, 220, 249));
        privacy.setGravity(Gravity.CENTER);
        privacy.setLineSpacing(0, 1.08f);
        LinearLayout.LayoutParams prp = new LinearLayout.LayoutParams(-1, -2);
        prp.topMargin = dp(10);
        prp.leftMargin = dp(12);
        prp.rightMargin = dp(12);
        root.addView(privacy, prp);

        TextView pulse = text("●", 20, true, TEAL);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-2, -2);
        pp.topMargin = dp(20);
        root.addView(pulse, pp);

        setContentView(root);

        chip.setAlpha(0f);
        mark.setAlpha(0f); mark.setScaleX(.72f); mark.setScaleY(.72f);
        title.setAlpha(0f); title.setTranslationY(dp(12));
        sub.setAlpha(0f); sub.setTranslationY(dp(12));
        security.setAlpha(0f); security.setTranslationY(dp(10));
        privacy.setAlpha(0f); privacy.setTranslationY(dp(10));
        pulse.setAlpha(.25f);

        chip.animate().alpha(1f).setDuration(280).start();
        AnimatorSet a = new AnimatorSet();
        a.playTogether(
                ObjectAnimator.ofFloat(mark, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(mark, View.SCALE_X, .72f, 1f),
                ObjectAnimator.ofFloat(mark, View.SCALE_Y, .72f, 1f));
        a.setStartDelay(80);
        a.setDuration(500);
        a.start();
        title.animate().alpha(1f).translationY(0).setStartDelay(260).setDuration(380).start();
        sub.animate().alpha(1f).translationY(0).setStartDelay(350).setDuration(380).start();
        security.animate().alpha(1f).translationY(0).setStartDelay(470).setDuration(380).start();
        privacy.animate().alpha(1f).translationY(0).setStartDelay(560).setDuration(380).start();
        pulse.animate().alpha(1f).setStartDelay(650).setDuration(300).withEndAction(() ->
                pulse.animate().alpha(.35f).setDuration(430).start()).start();

        new Handler(Looper.getMainLooper()).postDelayed(this::go, 1750);
    }

    private void go() {
        Intent i = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private TextView text(String s, int size, boolean bold, int color) {
        TextView v = new TextView(this);
        v.setText(s); v.setTextSize(size); v.setTextColor(color); v.setGravity(Gravity.CENTER);
        if (bold) v.setTypeface(null, 1);
        return v;
    }

    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }
}
