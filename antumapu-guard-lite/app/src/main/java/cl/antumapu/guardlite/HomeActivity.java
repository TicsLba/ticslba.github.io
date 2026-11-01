package cl.antumapu.guardlite;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;

public class HomeActivity extends Activity {
    private final Handler handler = new Handler();
    private boolean routed = false;

    private int dp(int n) {
        return (int)(n * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        routeHome();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        routed = false;
        routeHome();
    }

    private void routeHome() {
        if (routed) return;
        routed = true;

        PangiPolicyManager.applyCriticalStartup(this);

        final int bootCount = readBootCount();
        final boolean shouldShow = Prefs.shouldShowSplashForBoot(this, bootCount);

        if (!shouldShow) {
            if (!PangiPolicyManager.launchOriginalHome(this)) {
                showRecoveryScreen();
            } else {
                finish();
            }
            return;
        }

        Prefs.markSplashShownForBoot(this, bootCount);
        showInstitutionalSplash();

        handler.postDelayed(() -> {
            if (!PangiPolicyManager.launchOriginalHome(HomeActivity.this)) {
                showRecoveryScreen();
            } else {
                finish();
            }
        }, 1550L);
    }

    private int readBootCount() {
        try {
            return Settings.Global.getInt(getContentResolver(), Settings.Global.BOOT_COUNT, -1);
        } catch (Exception ignored) {
            return -1;
        }
    }

    private Bitmap loadInstitutionalLogo() {
        try {
            File dir = getExternalFilesDir(null);
            if (dir == null) return null;

            File png = new File(dir, "logo_antumapu.png");
            if (png.exists()) {
                Bitmap b = BitmapFactory.decodeFile(png.getAbsolutePath());
                if (b != null) return b;
            }

            File webp = new File(dir, "logo_antumapu.webp");
            if (webp.exists()) {
                return BitmapFactory.decodeFile(webp.getAbsolutePath());
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void configureFullscreen() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    private void showInstitutionalSplash() {
        configureFullscreen();

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.WHITE);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(dp(28), dp(34), dp(28), dp(28));

        FrameLayout.LayoutParams contentParams =
                new FrameLayout.LayoutParams(-1, -1);
        root.addView(content, contentParams);

        ImageView logo = new ImageView(this);
        logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        logo.setContentDescription("Liceo Bicentenario Antumapu");
        logo.setAlpha(0f);

        Bitmap bitmap = loadInstitutionalLogo();
        if (bitmap != null) {
            logo.setImageBitmap(bitmap);
        } else {
            logo.setImageResource(R.drawable.ic_pangi);
        }

        LinearLayout.LayoutParams logoParams =
                new LinearLayout.LayoutParams(-1, 0, 0.43f);
        logoParams.setMargins(dp(22), 0, dp(22), dp(4));
        content.addView(logo, logoParams);

        TextView name = new TextView(this);
        name.setText("Pangi");
        name.setTextColor(Color.rgb(0, 90, 18));
        name.setTextSize(40);
        name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        name.setGravity(Gravity.CENTER);
        name.setAlpha(0f);
        content.addView(name, new LinearLayout.LayoutParams(-1, -2));

        TextView descriptor = new TextView(this);
        descriptor.setText("GESTIÓN INSTITUCIONAL");
        descriptor.setTextColor(Color.rgb(244, 90, 30));
        descriptor.setTextSize(12);
        descriptor.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        descriptor.setLetterSpacing(0.16f);
        descriptor.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams descriptorParams =
                new LinearLayout.LayoutParams(-1, -2);
        descriptorParams.setMargins(0, dp(6), 0, 0);
        content.addView(descriptor, descriptorParams);

        View spacer = new View(this);
        content.addView(spacer, new LinearLayout.LayoutParams(1, 0, 0.27f));

        TextView developed = new TextView(this);
        developed.setText("Desarrollado por la Coordinación Informática del Liceo Bicentenario Antumapu");
        developed.setTextColor(Color.rgb(75, 86, 78));
        developed.setTextSize(12);
        developed.setGravity(Gravity.CENTER);
        developed.setLineSpacing(0f, 1.12f);
        content.addView(developed, new LinearLayout.LayoutParams(-1, -2));

        TextView author = new TextView(this);
        author.setText("© Eduardo Pérez González");
        author.setTextColor(Color.rgb(0, 90, 18));
        author.setTextSize(12);
        author.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        author.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams authorParams =
                new LinearLayout.LayoutParams(-1, -2);
        authorParams.setMargins(0, dp(8), 0, dp(4));
        content.addView(author, authorParams);

        View accent = new View(this);
        accent.setBackgroundColor(Color.rgb(244, 90, 30));
        FrameLayout.LayoutParams accentParams =
                new FrameLayout.LayoutParams(-1, dp(5), Gravity.BOTTOM);
        root.addView(accent, accentParams);

        setContentView(root);

        logo.animate().alpha(1f).setDuration(260).start();
        name.animate().alpha(1f).setStartDelay(90).setDuration(260).start();
    }

    private void showRecoveryScreen() {
        handler.removeCallbacksAndMessages(null);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.WHITE);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        content.setPadding(dp(30), dp(30), dp(30), dp(30));
        root.addView(content, new FrameLayout.LayoutParams(-1, -1));

        TextView title = new TextView(this);
        title.setText("Pangi");
        title.setTextSize(34);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(Color.rgb(0, 90, 18));
        title.setGravity(Gravity.CENTER);
        content.addView(title);

        TextView msg = new TextView(this);
        msg.setText("No se encontró un launcher del sistema disponible.\nAbre la administración de Pangi para recuperar el entorno.");
        msg.setTextSize(14);
        msg.setTextColor(Color.rgb(75, 86, 78));
        msg.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(-1, -2);
        mp.setMargins(0, dp(16), 0, dp(20));
        content.addView(msg, mp);

        TextView open = new TextView(this);
        open.setText("ABRIR ADMINISTRACIÓN");
        open.setTextSize(14);
        open.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        open.setTextColor(Color.WHITE);
        open.setGravity(Gravity.CENTER);
        open.setPadding(dp(20), dp(16), dp(20), dp(16));
        android.graphics.drawable.GradientDrawable bg =
                new android.graphics.drawable.GradientDrawable();
        bg.setColor(Color.rgb(0, 90, 18));
        bg.setCornerRadius(dp(14));
        open.setBackground(bg);
        open.setOnClickListener(v -> {
            Intent admin = new Intent(HomeActivity.this, MainActivity.class);
            startActivity(admin);
        });
        content.addView(open, new LinearLayout.LayoutParams(-1, -2));

        setContentView(root);
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override public void onBackPressed() {
        // HOME no se abandona con Atrás.
    }
}
