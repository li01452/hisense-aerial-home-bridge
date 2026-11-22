package com.li01452.hisenseaerialhomebridge;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

public final class BootCoverService extends Service {
    private static final String TAG = "HisenseBootBridge";
    private static final String AERIAL_PACKAGE = "app.orchard.tv";
    private static final long COVER_BEFORE_AERIAL_MS = 1800L;
    private static final long LOCKED_BOOT_TIMEOUT_MS = 25000L;

    public static final String EXTRA_ACTION = "boot_action";
    public static final String EXTRA_LAUNCH_AERIAL = "launch_aerial";

    private static long lastAerialLaunchElapsed = 0L;

    private final Handler handler = new Handler();
    private WindowManager windowManager;
    private View coverView;
    private Runnable pendingLaunch;
    private Runnable pendingRemoval;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null
                ? "unknown"
                : intent.getStringExtra(EXTRA_ACTION);
        boolean launchAerial = intent != null
                && intent.getBooleanExtra(EXTRA_LAUNCH_AERIAL, false);

        Log.i(TAG, "Cover service started. action=" + action
                + ", launchAerial=" + launchAerial);

        if (coverView == null && !showOverlay(action, launchAerial)) {
            if (launchAerial) {
                startFallbackActivity(action);
            }
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        if (pendingLaunch != null) {
            handler.removeCallbacks(pendingLaunch);
        }
        if (pendingRemoval != null) {
            handler.removeCallbacks(pendingRemoval);
        }

        if (launchAerial) {
            pendingLaunch = new Runnable() {
                @Override
                public void run() {
                    launchAerialAndRemove();
                }
            };
            handler.postDelayed(pendingLaunch, COVER_BEFORE_AERIAL_MS);
        } else {
            pendingRemoval = new Runnable() {
                @Override
                public void run() {
                    removeOverlay();
                    stopSelf();
                }
            };
            handler.postDelayed(pendingRemoval, LOCKED_BOOT_TIMEOUT_MS);
        }

        return START_NOT_STICKY;
    }

    private boolean showOverlay(String action, boolean launchAerial) {
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
            Log.w(TAG, "Overlay permission unavailable; using full-screen activity");
            return false;
        }

        try {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            if (windowManager == null) {
                Log.w(TAG, "WindowManager unavailable");
                return false;
            }

            TextView logo = new TextView(this);
            logo.setText("Hisense");
            logo.setTextColor(Color.WHITE);
            logo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 58);
            logo.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            logo.setGravity(Gravity.CENTER);
            logo.setBackgroundColor(Color.BLACK);
            if (Build.VERSION.SDK_INT >= 21) {
                logo.setLetterSpacing(0.015f);
            }
            logo.setContentDescription("Hisense startup logo");

            int windowType;
            if (Build.VERSION.SDK_INT >= 26) {
                windowType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                windowType = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
            }

            int windowFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_FULLSCREEN
                    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;

            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    windowType,
                    windowFlags,
                    PixelFormat.OPAQUE
            );
            params.gravity = Gravity.TOP | Gravity.START;

            windowManager.addView(logo, params);
            coverView = logo;
            Log.i(TAG, "Logo overlay displayed for action=" + action);
            return true;
        } catch (Exception error) {
            Log.e(TAG, "Could not display logo overlay", error);
            removeOverlay();
            return false;
        }
    }

    private void launchAerialAndRemove() {
        long now = SystemClock.elapsedRealtime();
        if (lastAerialLaunchElapsed == 0L
                || now < lastAerialLaunchElapsed
                || now - lastAerialLaunchElapsed > 15000L) {
            lastAerialLaunchElapsed = now;
            boolean launched = launchAerial();
            if (!launched) {
                launchHome();
            }
        } else {
            Log.i(TAG, "Aerial launch already attempted during this boot");
        }

        pendingRemoval = new Runnable() {
            @Override
            public void run() {
                removeOverlay();
                stopSelf();
            }
        };
        handler.postDelayed(pendingRemoval, 650L);
    }

    private boolean launchAerial() {
        try {
            Intent launch = getPackageManager()
                    .getLaunchIntentForPackage(AERIAL_PACKAGE);
            if (launch == null) {
                Log.w(TAG, "Aerial launch activity not found");
                return false;
            }

            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(launch);
            Log.i(TAG, "Launching Aerial");
            return true;
        } catch (Exception error) {
            Log.e(TAG, "Could not launch Aerial", error);
            return false;
        }
    }

    private void launchHome() {
        try {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(home);
            Log.i(TAG, "Aerial unavailable; returned to default Home");
        } catch (Exception error) {
            Log.e(TAG, "Could not return to default Home", error);
        }
    }

    private void startFallbackActivity(String action) {
        Intent fallback = new Intent(this, LogoActivity.class);
        fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        fallback.putExtra(LogoActivity.EXTRA_AUTO_LAUNCH, true);
        fallback.putExtra(LogoActivity.EXTRA_SOURCE, action);
        try {
            startActivity(fallback);
        } catch (Exception error) {
            Log.e(TAG, "Could not start full-screen fallback", error);
        }
    }

    private void removeOverlay() {
        if (windowManager != null && coverView != null) {
            try {
                windowManager.removeViewImmediate(coverView);
            } catch (Exception error) {
                Log.w(TAG, "Overlay was already removed", error);
            }
        }
        coverView = null;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        removeOverlay();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
