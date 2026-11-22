package com.li01452.hisenseaerialhomebridge;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

public final class LogoActivity extends Activity {
    private static final String TAG = "HisenseBootBridge";
    private static final String AERIAL_PACKAGE = "app.orchard.tv";
    private static final long LOGO_DURATION_MS = 1800L;
    private static final long TEST_DURATION_MS = 3000L;

    public static final String EXTRA_AUTO_LAUNCH = "auto_launch_aerial";
    public static final String EXTRA_SOURCE = "boot_source";

    private final Handler handler = new Handler();
    private boolean autoLaunch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        autoLaunch = getIntent().getBooleanExtra(EXTRA_AUTO_LAUNCH, false);
        String source = getIntent().getStringExtra(EXTRA_SOURCE);
        Log.i(TAG, "LogoActivity displayed. source=" + source
                + ", autoLaunch=" + autoLaunch);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        TextView logo = new TextView(this);
        logo.setText("Hisense");
        logo.setTextColor(Color.WHITE);
        logo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 58);
        logo.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        logo.setGravity(Gravity.CENTER);
        logo.setBackgroundColor(Color.BLACK);
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            logo.setLetterSpacing(0.015f);
        }
        logo.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        setContentView(logo);

        long duration = autoLaunch ? LOGO_DURATION_MS : TEST_DURATION_MS;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (autoLaunch) {
                    launchAerialOrHome();
                } else {
                    finish();
                }
            }
        }, duration);
    }

    private void launchAerialOrHome() {
        try {
            Intent launch = getPackageManager()
                    .getLaunchIntentForPackage(AERIAL_PACKAGE);
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                startActivity(launch);
                Log.i(TAG, "Fallback activity launching Aerial");
            } else {
                Intent home = new Intent(Intent.ACTION_MAIN);
                home.addCategory(Intent.CATEGORY_HOME);
                home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(home);
                Log.w(TAG, "Aerial not found; opened default Home");
            }
        } catch (Exception error) {
            Log.e(TAG, "Fallback launch failed", error);
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
