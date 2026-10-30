package cl.antumapu.guardlite;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;

public class SplashActivity extends Activity {
    private int dp(int n) {
        return (int)(n * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.WHITE);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        content.setPadding(dp(32), dp(24), dp(32), dp(24));
        root.addView(content, new FrameLayout.LayoutParams(-1, -1));

        ImageView logo = new ImageView(this);
        logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        logo.setAlpha(0f);

        File external = getExternalFilesDir(null);
        File logoFile = external == null ? null : new File(external, "logo_antumapu.webp");
        Bitmap bitmap = null;

        if (logoFile != null && logoFile.exists()) {
            bitmap = BitmapFactory.decodeFile(logoFile.getAbsolutePath());
        }

        if (bitmap != null) {
            logo.setImageBitmap(bitmap);
        } else {
            logo.setImageResource(R.drawable.ic_pangi);
        }

        LinearLayout.LayoutParams logoParams =
                new LinearLayout.LayoutParams(dp(360), dp(282));
        content.addView(logo, logoParams);

        TextView institutional = new TextView(this);
        institutional.setText("DISPOSITIVO INSTITUCIONAL");
        institutional.setTextColor(Color.rgb(0, 90, 18));
        institutional.setTextSize(14);
        institutional.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        institutional.setLetterSpacing(0.14f);
        institutional.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams institutionalParams =
                new LinearLayout.LayoutParams(-1, -2);
        institutionalParams.setMargins(0, dp(22), 0, dp(5));
        content.addView(institutional, institutionalParams);

        TextView managed = new TextView(this);
        managed.setText("Gestión protegida por Pangi");
        managed.setTextColor(Color.rgb(86, 96, 89));
        managed.setTextSize(13);
        managed.setGravity(Gravity.CENTER);
        content.addView(managed);

        View accent = new View(this);
        accent.setBackgroundColor(Color.rgb(244, 90, 30));
        FrameLayout.LayoutParams accentParams =
                new FrameLayout.LayoutParams(-1, dp(6), Gravity.BOTTOM);
        root.addView(accent, accentParams);

        setContentView(root);

        logo.animate()
                .alpha(1f)
                .setDuration(650)
                .start();

        new Handler().postDelayed(this::finish, 3200L);
    }

    @Override public void onBackPressed() {
        // La pantalla institucional termina automáticamente.
    }
}
