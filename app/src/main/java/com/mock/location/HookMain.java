package com.mock.location;

import android.location.Location;

import com.mock.location.model.MockLocationInfo;
import com.mock.location.util.ConfigFileUtil;
import com.mock.location.util.JsonUtils;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class HookMain implements IXposedHookLoadPackage {
    private static final String TAG = "MockLocation";

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        // 跳过系统进程和自身
        if ("android".equals(lpparam.packageName) ||
                "com.mock.location".equals(lpparam.packageName)) {
            return;
        }

        // Hook 基础 Android 定位
        hookLocation(lpparam);

        // 屏蔽 WiFi / 基站定位（防止辅助定位泄露真实位置）
        hookNetworkLocation(lpparam);
    }

    // ==================== Hook Android 原生 Location ====================
    private void hookLocation(LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(
                    "android.location.Location",
                    lpparam.classLoader,
                    "getLatitude",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            MockLocationInfo mockLocationInfo = readMockLocation();
                            param.setResult(mockLocationInfo.getLat());
                            XposedBridge.log(TAG +"active"+param.getResult()+ ": android.location.Location📍 getLatitude() -> " + mockLocationInfo.getLat() + " (pkg: " + ")");
                        }
                    }
            );

            XposedHelpers.findAndHookMethod(
                    "android.location.Location",
                    lpparam.classLoader,
                    "getLongitude",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            MockLocationInfo mockLocationInfo = readMockLocation();
                            param.setResult(mockLocationInfo.getLng());
                            XposedBridge.log(TAG +"active"+param.getResult()+ ": android.location.Location 📍 getLongitude() -> " + mockLocationInfo.getLng());
                        }
                    }
            );
            XposedHelpers.findAndHookMethod(Location.class, "getLatitude", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    MockLocationInfo mockLocationInfo = readMockLocation();
                    param.setResult(mockLocationInfo.getLat());
                    XposedBridge.log(TAG + "active " + param.getResult() + ":default Location.getLatitude() -> " + mockLocationInfo.getLat());
                }
            });

            XposedHelpers.findAndHookMethod(Location.class, "getLongitude", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    MockLocationInfo mockLocationInfo = readMockLocation();
                    XposedBridge.log(TAG + "active " + param.getResult() + ":default Location.getLongitude() -> " + mockLocationInfo.getLng());
                    param.setResult(mockLocationInfo.getLng());
                }
            });
        } catch (Exception e) {
            XposedBridge.log(TAG + ": ❌ Failed to hook Location: " + e.getMessage());
        }
    }

    // ==================== 屏蔽网络定位（WiFi/基站）====================
    private void hookNetworkLocation(LoadPackageParam lpparam) {
        try {
            // 屏蔽 WiFi 扫描
            XposedHelpers.findAndHookMethod(
                    "android.net.wifi.WifiManager",
                    lpparam.classLoader,
                    "getScanResults",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            XposedBridge.log(TAG +"active: "+JsonUtils.toJson( param.getResult())+ ": ⚠️ getScanResults: ");
                            param.setResult(null); // 返回空列表
                        }
                    }
            );

            // 屏蔽基站定位
            XposedHelpers.findAndHookMethod(
                    "android.telephony.TelephonyManager",
                    lpparam.classLoader,
                    "getCellLocation",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            XposedBridge.log(TAG +"active: "+JsonUtils.toJson( param.getResult())+ ": ⚠️ getCellLocation: ");

                            param.setResult(null);
                        }
                    }
            );
        } catch (Exception e) {
            XposedBridge.log(TAG + ": ⚠️ Network location hook failed: " + e.getMessage());
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 从当前 App 的私有目录读取 mock_location.txt
     * 格式：第一行纬度，第二行经度
     */
    private static MockLocationInfo readMockLocation() {
        // 在 Xposed 的 hook 方法中
        String jsonStr = ConfigFileUtil.readString(); // 你自己实现的 readString()
        if (jsonStr != null) {
            try {
                return JsonUtils.fromJson(jsonStr, MockLocationInfo.class);
            } catch (Exception e) {
                XposedBridge.log("XPOSED: ❌ Parse JSON failed: " + e.getMessage());
            }
        }
        return MockLocationInfo.DefaultValue();
    }
}