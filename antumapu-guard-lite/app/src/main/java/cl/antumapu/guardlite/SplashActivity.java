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
        root.setBackgroundColor(Color.WHITE);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(dp(28), dp(34), dp(28), dp(28));
        root.addView(content, new FrameLayout.LayoutParams(-1, -1));

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

        handler.postDelayed(this::finish, 1550L);
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

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override public void onBackPressed() {
        // La presentación termina automáticamente.
    }
}
