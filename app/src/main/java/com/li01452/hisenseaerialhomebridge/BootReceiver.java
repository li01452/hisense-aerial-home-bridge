package com.li01452.hisenseaerialhomebridge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

public final class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "HisenseBootBridge";
    private static final String HISENSE_BOOT =
            "android.intent.action.HISENSE_BOOT_COMPLETED";
    private static final String LOCKED_BOOT =
            "android.intent.action.LOCKED_BOOT_COMPLETED";

    private static long lastHandledElapsed = 0L;

    @Override
    public synchronized void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        final String action = intent.getAction();
        final boolean lockedBoot =
                LOCKED_BOOT.equals(action);

        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !lockedBoot
                && !HISENSE_BOOT.equals(action)) {
            return;
        }

        long now = SystemClock.elapsedRealtime();
        if (lastHandledElapsed > 0L
                && now >= lastHandledElapsed
                && now - lastHandledElapsed < 8000L) {
            Log.i(TAG, "Ignoring duplicate boot broadcast: " + action);
            return;
        }
        lastHandledElapsed = now;

        Log.i(TAG, "Received boot broadcast: " + action);

        Intent service = new Intent(context, BootCoverService.class);
        service.putExtra(BootCoverService.EXTRA_ACTION, action);
        service.putExtra(BootCoverService.EXTRA_LAUNCH_AERIAL, !lockedBoot);

        try {
            context.startService(service);
        } catch (Exception error) {
            Log.e(TAG, "Could not start cover service", error);

            if (!lockedBoot) {
                Intent fallback = new Intent(context, LogoActivity.class);
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                fallback.putExtra(LogoActivity.EXTRA_AUTO_LAUNCH, true);
                fallback.putExtra(LogoActivity.EXTRA_SOURCE, action);
                try {
                    context.startActivity(fallback);
                } catch (Exception activityError) {
                    Log.e(TAG, "Could not start fallback activity", activityError);
                }
            }
        }
    }
}
