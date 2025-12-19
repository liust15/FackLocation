package com.mock.location;

import android.location.Location;
import android.os.Build;

import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookLoader implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals("com.mock.location")) return;

        // 强行把 Location 对象的“模拟”标签改为 false
        XposedHelpers.findAndHookMethod(Location.class, "isFromMockProvider", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                param.setResult(false);
            }
        });

        // 兼容 Android 12+ 的新标记
        if (Build.VERSION.SDK_INT >= 31) {
            XposedHelpers.findAndHookMethod(Location.class, "isMock", new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) { param.setResult(false); }
            });
        }
    }
}