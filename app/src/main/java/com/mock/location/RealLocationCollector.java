// com.mock.location.util.RealLocationCollector
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
import android.telephony.CellInfo;
import android.telephony.TelephonyManager;
import androidx.core.app.ActivityCompat;
import com.mock.location.model.LocationRecord;
import java.util.ArrayList;
import java.util.List;

public class RealLocationCollector {

    public interface OnLocationCollectedCallback {
        void onCollected(LocationRecord record);
        void onError(String error);
    }

    public static void collectCurrentLocation(Context context, OnLocationCollectedCallback callback) {
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean hasGps = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean hasNetwork = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!hasGps && !hasNetwork) {
            callback.onError("定位服务未开启");
            return;
        }

        LocationListener listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                LocationRecord record = new LocationRecord();
                record.lat = location.getLatitude();
                record.lng = location.getLongitude();

                // 收集 WiFi
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED) {
                    WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                    List<ScanResult> results = wifi.getScanResults();
                    record.wifiBssids = new ArrayList<>();
                    for (ScanResult r : results) {
                        record.wifiBssids.add(r.BSSID);
                    }
                }

                // 收集基站（简化版）
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                    TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                    List<CellInfo> cellInfos = tm.getAllCellInfo();
                    if (cellInfos != null && !cellInfos.isEmpty()) {
                        // 这里简化处理，实际需区分 GSM/LTE 等
                        record.cellInfo = new LocationRecord.CellInfo();
                        // 示例：取第一个
                        // 实际需用 CellInfoGsm / CellInfoLte 等 cast
                    }
                }

                lm.removeUpdates(this);
                callback.onCollected(record);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}
            @Override
            public void onProviderEnabled(String provider) {}
            @Override
            public void onProviderDisabled(String provider) {}
        };

        try {
            if (hasGps) {
                lm.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, null);
            } else if (hasNetwork) {
                lm.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener, null);
            }
        } catch (SecurityException e) {
            callback.onError("缺少定位权限");
        }
    }
}