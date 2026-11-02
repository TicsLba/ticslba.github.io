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
    private final Handler handler = new Handler();

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
        root.setBackgroundColor(Color.rgb(250, 251, 250));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(dp(30), dp(32), dp(30), dp(26));
        root.addView(content, new FrameLayout.LayoutParams(-1, -1));

        View topSpace = new View(this);
        content.addView(topSpace, new LinearLayout.LayoutParams(1, 0, 0.12f));

        ImageView emblem = new ImageView(this);
        Bitmap bitmap = loadInstitutionalLogo();
        if (bitmap != null) emblem.setImageBitmap(bitmap);
        else emblem.setImageResource(R.drawable.ic_pangi);
        emblem.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        emblem.setContentDescription("Liceo Bicentenario Antumapu");
        emblem.setAlpha(0f);
        content.addView(emblem, new LinearLayout.LayoutParams(dp(188), dp(150)));

        TextView product = new TextView(this);
        product.setText("Pangi");
        product.setTextColor(Color.rgb(0, 90, 18));
        product.setTextSize(44);
        product.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        product.setGravity(Gravity.CENTER);
        product.setAlpha(0f);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-1, -2);
        pp.setMargins(0, dp(18), 0, 0);
        content.addView(product, pp);

        TextView version = new TextView(this);
        version.setText("GESTIÓN INSTITUCIONAL  ·  3.0");
        version.setTextColor(Color.rgb(244, 90, 30));
        version.setTextSize(11);
        version.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        version.setLetterSpacing(0.12f);
        version.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(-1, -2);
        vp.setMargins(0, dp(6), 0, 0);
        content.addView(version, vp);

        View middleSpace = new View(this);
        content.addView(middleSpace, new LinearLayout.LayoutParams(1, 0, 0.36f));

        TextView managed = new TextView(this);
        managed.setText("Dispositivo administrado institucionalmente");
        managed.setTextColor(Color.rgb(55, 67, 59));
        managed.setTextSize(13);
        managed.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        managed.setGravity(Gravity.CENTER);
        content.addView(managed, new LinearLayout.LayoutParams(-1, -2));

        TextView developed = new TextView(this);
        developed.setText("Desarrollado por la Coordinación Informática del Liceo Bicentenario Antumapu");
        developed.setTextColor(Color.rgb(102, 112, 105));
        developed.setTextSize(11);
        developed.setGravity(Gravity.CENTER);
        developed.setLineSpacing(0f, 1.12f);
        LinearLayout.LayoutParams dpv = new LinearLayout.LayoutParams(-1, -2);
        dpv.setMargins(dp(12), dp(9), dp(12), 0);
        content.addView(developed, dpv);

        TextView author = new TextView(this);
        author.setText("© Eduardo Pérez González");
        author.setTextColor(Color.rgb(0, 90, 18));
        author.setTextSize(11);
        author.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        author.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-1, -2);
        ap.setMargins(0, dp(7), 0, dp(2));
        content.addView(author, ap);

        View accent = new View(this);
        accent.setBackgroundColor(Color.rgb(244, 90, 30));
        FrameLayout.LayoutParams accentParams =
                new FrameLayout.LayoutParams(-1, dp(4), Gravity.BOTTOM);
        root.addView(accent, accentParams);

        setContentView(root);

        emblem.animate().alpha(1f).translationY(0).setDuration(300).start();
        product.animate().alpha(1f).setStartDelay(90).setDuration(280).start();

        handler.postDelayed(this::finish, 1450L);
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

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override public void onBackPressed() {
        // La presentación termina automáticamente.
    }
}
