package com.mock.location;

import com.mock.location.model.LocationRecord;
import com.mock.location.util.ConfigFileUtil;
import com.mock.location.util.JsonUtils;

import java.util.Collections;

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
        if ("android".equals(lpparam.packageName)
                || "com.mock.location".equals(lpparam.packageName)) {
            return;
        }

        // Hook 基础 Android 定位
        hookLocation(lpparam);

        // 屏蔽 WiFi / 基站定位（防止辅助定位泄露真实位置）
        hookNetworkLocation(lpparam);
    }
    public void handleLoad(LoadPackageParam lpparam) throws Throwable {

        // 使用目标包的 ClassLoader 获取 android.location.Location 类
        Class<?> locationClass = XposedHelpers.findClass("android.location.Location", lpparam.classLoader);

        // Hook getLatitude
        XposedHelpers.findAndHookMethod(
                locationClass,
                "getLatitude",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        LocationRecord LocationRecord = readMockLocation();
                        param.setResult(LocationRecord.getLat());
                        XposedBridge.log(TAG + " locationClass: Location.getLatitude() -> "
                                + LocationRecord.getLat()
                        ); }
                }
        );

        // Hook getLongitude
        XposedHelpers.findAndHookMethod(
                locationClass,
                "getLongitude",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        LocationRecord LocationRecord = readMockLocation();
                        param.setResult(LocationRecord.getLng());
                        XposedBridge.log(TAG + " locationClass: Location.getLongitude() -> "
                                + LocationRecord.getLng()
                              );
                    }
                }
        );
    }
    // ==================== Hook Android 原生 Location ====================
    private void hookLocation(LoadPackageParam lpparam) {
        try {
            final String packageName = lpparam.packageName;

            XposedHelpers.findAndHookMethod(
                    "android.location.Location",
                    lpparam.classLoader,
                    "getLatitude",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            try {
                                LocationRecord LocationRecord = readMockLocation();
                                param.setResult(LocationRecord.getLat());
                                XposedBridge.log(TAG + ": Location.getLatitude() -> "
                                        + LocationRecord.getLat()
                                        + " pkg=" + packageName);
                            } catch (Throwable t) {
                                XposedBridge.log(TAG + ": Location.getLatitude hook error: " + t.getMessage());
                            }
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
                            try {
                                LocationRecord LocationRecord = readMockLocation();
                                param.setResult(LocationRecord.getLng());
                                XposedBridge.log(TAG + ": Location.getLongitude() -> "
                                        + LocationRecord.getLng()
                                        + " pkg=" + packageName);
                            } catch (Throwable t) {
                                XposedBridge.log(TAG + ": Location.getLongitude hook error: " + t.getMessage());
                            }
                        }
                    }
            );
        } catch (Throwable e) {
            XposedBridge.log(TAG + ": Failed to hook Location: " + e.getMessage());
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
                            try {
                                LocationRecord LocationRecord = readMockLocation();
                                // 返回安全的空列表，避免调用方对结果 for-each 时 NPE
                                param.setResult(LocationRecord.getWifiBssids());
                                XposedBridge.log(TAG + ": WifiManager.getScanResults() -> empty list");
                            } catch (Throwable t) {
                                XposedBridge.log(TAG + ": getScanResults hook error: " + t.getMessage());
                            }
                        }
                    }
            );

            // 基站定位：仅记录日志，不强行改为 null，避免未判空的 App 崩溃
            XposedHelpers.findAndHookMethod(
                    "android.telephony.TelephonyManager",
                    lpparam.classLoader,
                    "getCellLocation",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            try {
                                LocationRecord LocationRecord = readMockLocation();
                                Object result = param.getResult();
                                String type = (result != null)
                                        ? result.getClass().getSimpleName()
                                        : "null";
                                param.setResult(LocationRecord.getCellInfo());
                                XposedBridge.log(TAG + ": TelephonyManager.getCellLocation() hooked, original=" + type);
                            } catch (Throwable t) {
                                XposedBridge.log(TAG + ": getCellLocation hook error: " + t.getMessage());
                            }
                        }
                    }
            );
        } catch (Throwable e) {
            XposedBridge.log(TAG + ": Network location hook failed: " + e.getMessage());
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 从 /data/local/tmp/mock_location.loc 读取 JSON，反序列化为 LocationRecord
     */
    private static LocationRecord readMockLocation() {
        String jsonStr = ConfigFileUtil.readString();
        if (jsonStr != null) {
            try {
                LocationRecord info = JsonUtils.fromJson(jsonStr, LocationRecord.class);
                if (info != null) {
                    return info;
                }
            } catch (Throwable e) {
                XposedBridge.log(TAG + ": Parse JSON failed: " + e.getMessage());
            }
        }
        // 兜底：返回默认坐标，保证不为 null
        return LocationRecord.DefaultValue();
    }
}
