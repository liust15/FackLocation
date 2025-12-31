package com.mock.location;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Looper;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityNr;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoNr;
import android.telephony.CellInfoWcdma;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.mock.location.model.LocationRecord;
import com.mock.location.model.SerializableCellInfo;

import java.util.ArrayList;
import java.util.List;

public class RealLocationCollector {

    private static final String TAG = "RealLocationCollector";

    public interface OnLocationCollectedCallback {
        void onCollected(LocationRecord record);
        void onError(String error);
    }

    public static void collectCurrentLocation(Context context, OnLocationCollectedCallback callback) {
        Log.d(TAG, "collectCurrentLocation 被调用");

        Context appContext = context.getApplicationContext();
        LocationManager lm = (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);

        // 检查定位权限
        if (ActivityCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "缺少 ACCESS_FINE_LOCATION 权限");
            callback.onError("缺少定位权限");
            return;
        }

        boolean gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        Log.d(TAG, "GPS enabled: " + gpsEnabled + ", Network enabled: " + networkEnabled);

        if (!gpsEnabled && !networkEnabled) {
            Log.w(TAG, "所有定位提供者均被禁用");
            callback.onError("请在系统设置中开启定位服务");
            return;
        }

        // 创建监听器（只处理第一个有效位置）
        LocationListener listener = new LocationListener() {
            private boolean hasReceived = false;

            @Override
            public void onLocationChanged(Location location) {
                synchronized (this) {
                    if (hasReceived) return;
                    hasReceived = true;
                }

                Log.i(TAG, "成功获取位置: " + location.getLatitude() + ", " + location.getLongitude());
                lm.removeUpdates(this); // 注销监听

                // 过滤无效或陈旧位置
                long age = System.currentTimeMillis() - location.getTime();
                if (age > 5 * 60 * 1000) {
                    Log.w(TAG, "位置过旧（" + (age / 1000) + "秒），忽略");
                    callback.onError("无法获取最新位置，请稍后重试");
                    return;
                }

                if (location.getLatitude() == 0 && location.getLongitude() == 0) {
                    Log.w(TAG, "位置为 (0,0)，无效");
                    callback.onError("获取到无效位置");
                    return;
                }

                // 构建完整记录
                LocationRecord record = new LocationRecord(location.getLatitude(), location.getLongitude());
                record.wifiBssids = getWifiBssids(appContext);
                record.cellInfo = getCellInfo(appContext);
                callback.onCollected(record);
            }

            @Override
            public void onProviderDisabled(String provider) {
                Log.d(TAG, "Provider 被禁用: " + provider);
            }
        };

        try {
            // 尝试使用最近已知位置（快速路径）
            Location lastGps = gpsEnabled ? lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) : null;
            Location lastNetwork = networkEnabled ? lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) : null;

            Location bestLast = null;
            if (lastGps != null && isValidRecent(lastGps)) {
                bestLast = lastGps;
            } else if (lastNetwork != null && isValidRecent(lastNetwork)) {
                bestLast = lastNetwork;
            }

            if (bestLast != null) {
                Log.i(TAG, "使用 getLastKnownLocation 快速返回");
                LocationRecord record = new LocationRecord(bestLast.getLatitude(), bestLast.getLongitude());
                record.wifiBssids = getWifiBssids(appContext);
                record.cellInfo = getCellInfo(appContext);
                callback.onCollected(record);
                return;
            }

            // 启动实时监听
            Log.d(TAG, "开始监听实时位置更新...");
            if (gpsEnabled) {
                lm.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, Looper.getMainLooper());
            }
            if (networkEnabled) {
                lm.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener, Looper.getMainLooper());
            }

        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException", e);
            callback.onError("定位权限异常");
        } catch (Exception e) {
            Log.e(TAG, "定位请求异常", e);
            callback.onError("定位服务异常: " + e.getMessage());
        }
    }

    private static boolean isValidRecent(Location location) {
        if (location == null) return false;
        long age = System.currentTimeMillis() - location.getTime();
        return age >= 0 && age <= 5 * 60 * 1000; // 5分钟内
    }

    private static List<String> getWifiBssids(Context context) {
        List<String> bssids = new ArrayList<>();
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
                return bssids;
            }

            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (wifiManager == null || !wifiManager.isWifiEnabled()) {
                Log.d(TAG, "WiFi 未开启或不可用");
                return bssids;
            }

            List<ScanResult> scanResults = wifiManager.getScanResults();
            if (scanResults != null) {
                for (ScanResult result : scanResults) {
                    if (result.BSSID != null && !result.BSSID.equals("00:00:00:00:00:00") && !result.BSSID.equals("")) {
                        bssids.add(result.BSSID.toLowerCase().replaceAll("-", ":"));
                    }
                }
                Log.d(TAG, "采集到 " + bssids.size() + " 个 WiFi BSSID");
            }
        } catch (Exception e) {
            Log.e(TAG, "获取 WiFi 列表失败", e);
        }
        return bssids;
    }

    private static SerializableCellInfo getCellInfo(Context context) {
        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm == null) return null;

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "缺少 READ_PHONE_STATE 权限，跳过基站信息");
                return null;
            }

            List<CellInfo> allCells = tm.getAllCellInfo();
            if (allCells == null || allCells.isEmpty()) {
                Log.w(TAG, "getAllCellInfo() 返回 null 或空");
                return null;
            }

            CellInfo cell = allCells.get(0); // 主小区
            SerializableCellInfo sci = new SerializableCellInfo();

            if (cell instanceof CellInfoGsm) {
                CellIdentityGsm id = ((CellInfoGsm) cell).getCellIdentity();
                sci.networkType = "GSM";
                sci.mcc = id.getMcc();
                sci.mnc = id.getMnc();
                sci.lac = id.getLac();
                sci.cid = id.getCid();
            } else if (cell instanceof CellInfoWcdma) {
                CellIdentityWcdma id = ((CellInfoWcdma) cell).getCellIdentity();
                sci.networkType = "WCDMA";
                sci.mcc = id.getMcc();
                sci.mnc = id.getMnc();
                sci.lac = id.getLac();
                sci.cid = id.getCid();
            } else if (cell instanceof CellInfoLte) {
                CellIdentityLte id = ((CellInfoLte) cell).getCellIdentity();
                sci.networkType = "LTE";
                sci.mcc = id.getMcc();
                sci.mnc = id.getMnc();
                sci.lac = id.getTac(); // TAC
                sci.cid = id.getCi();
            } else if (cell instanceof CellInfoNr) {
                CellIdentityNr id = ((CellInfoNr) cell).getCellIdentity();
                sci.networkType = "NR"; // 5G
                sci.mcc = id.getMcc();
                sci.mnc = id.getMnc();
                sci.lac = id.getTac();
                sci.cid = (int) (id.getNci() & 0xFFFFFFFFL); // NCI 可能是 long，截断为 int
            } else {
                Log.w(TAG, "不支持的 CellInfo 类型: " + cell.getClass().getSimpleName());
                return null;
            }

            Log.d(TAG, "采集基站信息: " + sci.toString());
            return sci;
        } catch (Exception e) {
            Log.e(TAG, "获取基站信息失败", e);
            return null;
        }
    }
}