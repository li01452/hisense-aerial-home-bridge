# Hisense Aerial Home Bridge

一个面向旧款海信 Android TV 的轻量开机桥接程序。

## 当前版本

- 版本：0.1.0-test1
- applicationId：com.li01452.hisenseaerialhomebridge
- 最低 Android：4.4 / API 19
- targetSdk：22
- 不需要 Root，不修改系统分区，不自动禁用任何系统应用

## 设计目标

1. 监听海信私有开机广播 android.intent.action.HISENSE_BOOT_COMPLETED。
2. 同时监听 Android 标准 BOOT_COMPLETED，作为备用入口。
3. 收到开机广播后，优先尝试显示全屏 Hisense Logo 遮罩。
4. 短暂显示 Logo 后启动 Aerial（包名 app.orchard.tv）。
5. 如果系统不允许悬浮窗，则退回全屏 Activity 方案。

LOCKED_BOOT_COMPLETED 只尝试显示 Logo，不在用户解锁前启动 Aerial。随后收到 BOOT_COMPLETED 时再启动 Aerial。

## 重要边界

这个 APK 只能在 Android 系统开始运行后执行。它不能替换 Bootloader、内核或 Android framework 之前的真正开机 Logo，也不能保证海信固件一定向第三方应用发送 HISENSE_BOOT_COMPLETED。

如果两种开机广播都没有送达，普通 APK 无法凭空在开机时运行。届时需要重新研究固件启动链，不能靠修改 APK 名称或增加权限解决。

## 安装测试

先在电视上安装 Aerial。Android 5.0 及以上优先使用 Aerial Standard；Android 4.x 需要使用 Aerial Lite 或其他兼容版本。

将 APK 安装到电视：

    adb install -r hisense-aerial-home-bridge-v0.1.0-test1.apk

首次安装后必须手动打开一次桥接程序，解除 Android 对新安装应用的 stopped 状态：

    adb shell monkey -p com.li01452.hisenseaerialhomebridge 1

如果电视是 Android 6.0 或更新版本，需要允许“显示在其他应用上层”。可以从桥接程序中的权限按钮进入设置。

Android 5.1 / SDK 22 可尝试通过 ADB 授权悬浮窗：

    adb shell appops set com.li01452.hisenseaerialhomebridge SYSTEM_ALERT_WINDOW allow

如果 appops 命令不支持或提示权限错误，记录结果即可。程序仍会尝试使用全屏 Activity 作为回退。

## 验证步骤

第一轮不要禁用聚好看，也不要禁用海信原厂桌面。

1. 安装 Aerial 和本桥接 APK。
2. 手动打开桥接 APK 一次。
3. 在桥接 APK 内点击“测试 Logo 覆盖层”，确认 Logo 能压在当前界面上方显示约 4 秒。
4. 保持原厂桌面启用，重启电视。
5. 连续测试至少三次。
6. 观察是否出现 Hisense Logo，随后是否自动进入 Aerial。

可在电脑上查看日志：

    adb logcat -s HisenseBootBridge:I

重点关注以下日志：

- Received boot broadcast
- Overlay permission unavailable
- Logo overlay displayed
- Launching Aerial
- Aerial launch activity not found

## 当前版本不会做的事

- 不会自动禁用聚好看。
- 不会自动禁用海信原厂桌面。
- 不会修改 Home 默认应用。
- 不会修改 bootanimation.zip、bootloader 或系统分区。
- 不会安装 Home Guard 或无障碍守护。
- 不会 Root。

只有桥接启动稳定后，才单独确认原厂桌面包名并设计可恢复的停用步骤。

## 恢复方式

本版本不修改系统包状态，因此正常情况下卸载桥接 APK 即可恢复原状：

    adb uninstall com.li01452.hisenseaerialhomebridge

如果后续手动停用了某个原厂包，必须使用现场确认过的包名恢复：

    adb shell pm enable --user 0 已确认的包名

## 构建

项目使用 Android Gradle Plugin 8.7.3、Gradle 8.9、JDK 17。

GitHub Actions 会在 main 分支更新后自动编译 APK，并发布测试版 Release。Release APK 使用调试证书签名，只用于侧载测试，不用于应用商店发布。
