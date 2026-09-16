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

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(NAVY);
        getWindow().setNavigationBarColor(NAVY);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(28), dp(28), dp(28), dp(28));
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{NAVY, Color.rgb(31, 55, 111), COBALT});
        root.setBackground(bg);

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

        TextView sub = text("Gestión segura de dispositivos institucionales", 14, false,
                Color.rgb(221, 230, 255));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-2, -2);
        sp.topMargin = dp(7);
        root.addView(sub, sp);

        TextView pulse = text("●", 20, true, TEAL);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-2, -2);
        pp.topMargin = dp(20);
        root.addView(pulse, pp);

        setContentView(root);

        mark.setAlpha(0f); mark.setScaleX(.72f); mark.setScaleY(.72f);
        title.setAlpha(0f); title.setTranslationY(dp(12));
        sub.setAlpha(0f); sub.setTranslationY(dp(12));
        pulse.setAlpha(.25f);

        AnimatorSet a = new AnimatorSet();
        a.playTogether(
                ObjectAnimator.ofFloat(mark, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(mark, View.SCALE_X, .72f, 1f),
                ObjectAnimator.ofFloat(mark, View.SCALE_Y, .72f, 1f));
        a.setDuration(520);
        a.start();
        title.animate().alpha(1f).translationY(0).setStartDelay(260).setDuration(420).start();
        sub.animate().alpha(1f).translationY(0).setStartDelay(380).setDuration(420).start();
        pulse.animate().alpha(1f).setStartDelay(520).setDuration(350).withEndAction(() ->
                pulse.animate().alpha(.35f).setDuration(450).start()).start();

        new Handler(Looper.getMainLooper()).postDelayed(this::go, 1250);
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
