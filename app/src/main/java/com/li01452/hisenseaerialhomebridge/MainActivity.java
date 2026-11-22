package com.li01452.hisenseaerialhomebridge;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private static final String AERIAL_PACKAGE = "app.orchard.tv";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        int padding = dp(28);
        root.setPadding(padding, padding, padding, padding);
        root.setBackgroundColor(0xFF101010);

        TextView title = new TextView(this);
        title.setText("Hisense Boot Bridge");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView detail = new TextView(this);
        detail.setText("桥接程序已安装。首次打开用于启用开机接收器。\n\n开机时：显示 Hisense Logo → 启动 Aerial。\n\n请先保持聚好看和原厂桌面启用，完成三次重启测试后再处理原厂桌面。");
        detail.setTextColor(0xFFCCCCCC);
        detail.setTextSize(16);
        detail.setGravity(Gravity.CENTER);
        detail.setPadding(0, dp(18), 0, dp(18));
        root.addView(detail, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        Button testButton = new Button(this);
        testButton.setText("测试 Logo 覆盖层");
        testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent test = new Intent(MainActivity.this, BootCoverService.class);
                test.putExtra(BootCoverService.EXTRA_ACTION, "MANUAL_TEST");
                test.putExtra(BootCoverService.EXTRA_LAUNCH_AERIAL, false);
                test.putExtra(BootCoverService.EXTRA_MANUAL_TEST, true);
                startService(test);
            }
        });
        root.addView(testButton, buttonLayout());

        Button aerialButton = new Button(this);
        aerialButton.setText("立即启动 Aerial");
        aerialButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchAerial();
            }
        });
        root.addView(aerialButton, buttonLayout());

        if (Build.VERSION.SDK_INT >= 23) {
            Button permissionButton = new Button(this);
            permissionButton.setText("检查 / 开启悬浮窗权限");
            permissionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (Settings.canDrawOverlays(MainActivity.this)) {
                        Toast.makeText(MainActivity.this,
                                "悬浮窗权限已开启", Toast.LENGTH_SHORT).show();
                    } else {
                        Intent settings = new Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + getPackageName()));
                        try {
                            startActivity(settings);
                        } catch (Exception error) {
                            Toast.makeText(MainActivity.this,
                                    "请在系统设置中允许显示在其他应用上层",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                }
            });
            root.addView(permissionButton, buttonLayout());
        }

        setContentView(root);
    }

    private void launchAerial() {
        Intent launch = getPackageManager()
                .getLaunchIntentForPackage(AERIAL_PACKAGE);
        if (launch == null) {
            Toast.makeText(this,
                    "没有找到 Aerial。请先安装 Aerial APK。",
                    Toast.LENGTH_LONG).show();
            return;
        }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(launch);
    }

    private LinearLayout.LayoutParams buttonLayout() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(8);
        return params;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
