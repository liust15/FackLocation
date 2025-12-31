package com.mock.location;

import android.net.wifi.ScanResult;

import com.mock.location.model.LocationRecord;
import com.mock.location.model.SerializableCellInfo;
import com.mock.location.util.ConfigFileUtil;
import com.mock.location.util.JsonUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    // ==================== 屏蔽/注入网络定位（WiFi/基站）====================
    private void hookNetworkLocation(LoadPackageParam lpparam) {
        try {
            final String packageName = lpparam.packageName;
            final ClassLoader classLoader = lpparam.classLoader;

            // WiFi 扫描结果注入：使用保存的 BSSID 构造 List<ScanResult>
            XposedHelpers.findAndHookMethod(
                    "android.net.wifi.WifiManager",
                    classLoader,
                    "getScanResults",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            try {
                                LocationRecord record = readMockLocation();
                                if (record == null) {
                                    return;
                                }
                                List<String> bssids = record.getWifiBssids();
                                if (bssids == null || bssids.isEmpty()) {
                                    // 没有保存的 WiFi 列表，保留原始结果
                                    return;
                                }

                                Class<?> scanResultClass = XposedHelpers.findClass(
                                        "android.net.wifi.ScanResult",
                                        classLoader
                                );

                                List<ScanResult> mockedList = new ArrayList<>();
                                for (String bssid : bssids) {
                                    if (bssid == null || bssid.isEmpty()) {
                                        continue;
                                    }
                                    Object obj = XposedHelpers.newInstance(scanResultClass);
                                    ScanResult sr = (ScanResult) obj;

                                    // 通过反射安全设置字段，任一失败则整体回退到原始结果
                                    XposedHelpers.setObjectField(sr, "BSSID", bssid);
                                    XposedHelpers.setObjectField(sr, "SSID", "mock");
                                    try {
                                        XposedHelpers.setIntField(sr, "level", -50);
                                    } catch (Throwable ignored) {
                                        // 某些版本字段名可能变化，忽略
                                    }
                                    try {
                                        XposedHelpers.setIntField(sr, "frequency", 2412);
                                    } catch (Throwable ignored) {
                                    }

                                    mockedList.add(sr);
                                }

                                param.setResult(mockedList);
                                XposedBridge.log(TAG + ": WifiManager.getScanResults() -> injected "
                                        + mockedList.size() + " BSSID(s), pkg=" + packageName);
                            } catch (Throwable t) {
                                // 任何反射错误都回退到原始结果，避免崩溃
                                XposedBridge.log(TAG + ": WifiManager.getScanResults inject error: " + t.getMessage());
                            }
                        }
                    }
            );

            // 基站定位注入：仅对 GSM/WCDMA 构造 GsmCellLocation
            XposedHelpers.findAndHookMethod(
                    "android.telephony.TelephonyManager",
                    classLoader,
                    "getCellLocation",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            try {
                                LocationRecord record = readMockLocation();
                                if (record == null) {
                                    return;
                                }
                                SerializableCellInfo cellInfo = record.getCellInfo();
                                if (cellInfo == null) {
                                    return;
                                }

                                String nt = cellInfo.networkType != null
                                        ? cellInfo.networkType.toUpperCase()
                                        : "";
                                if (!"GSM".equals(nt) && !"WCDMA".equals(nt)) {
                                    // LTE/NR 等保持原始结果
                                    return;
                                }

                                Class<?> gsmCellLocationClass = XposedHelpers.findClass(
                                        "android.telephony.gsm.GsmCellLocation",
                                        classLoader
                                );
                                Object gsmCellLocation = XposedHelpers.newInstance(gsmCellLocationClass);

                                boolean applied = false;
                                try {
                                    XposedHelpers.callMethod(gsmCellLocation, "setLacAndCid",
                                            cellInfo.lac, cellInfo.cid);
                                    applied = true;
                                } catch (Throwable methodError) {
                                    try {
                                        XposedHelpers.setIntField(gsmCellLocation, "mLac", cellInfo.lac);
                                        XposedHelpers.setIntField(gsmCellLocation, "mCid", cellInfo.cid);
                                        applied = true;
                                    } catch (Throwable fieldError) {
                                        XposedBridge.log(TAG + ": GsmCellLocation setLacAndCid/mLac/mCid failed: "
                                                + fieldError.getMessage());
                                    }
                                }

                                if (!applied) {
                                    // 无法安全设置 LAC/CID，保留原始结果
                                    return;
                                }

                                param.setResult(gsmCellLocation);
                                XposedBridge.log(TAG + ": TelephonyManager.getCellLocation() -> injected LAC="
                                        + cellInfo.lac + ", CID=" + cellInfo.cid + ", pkg=" + packageName);
                            } catch (Throwable t) {
                                XposedBridge.log(TAG + ": getCellLocation inject error: " + t.getMessage());
                            }
                        }
                    }
            );

            // 注意：不再 Hook getAllCellInfo，避免脆弱的 CellInfo 模拟
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
