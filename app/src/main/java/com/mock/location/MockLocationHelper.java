package com.mock.location;

import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;
import android.content.Context;

public class MockLocationHelper {

    private static final String MOCK_PROVIDER = "gps"; // 也可以自定义，如 "mock"

    public static void startMockingLocation(Context context, double latitude, double longitude) {
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        try {
            // 1. 移除可能已存在的测试 Provider（避免重复）
            lm.removeTestProvider(MOCK_PROVIDER);
        } catch (Exception ignored) {}

        try {
            // 2. 添加测试 Provider
            lm.addTestProvider(
                MOCK_PROVIDER,
                false, // requiresNetwork
                false, // requiresSatellite
                false, // requiresCell
                false, // hasMonetaryCost
                true,  // supportsAltitude
                true,  // supportsSpeed
                true,  // supportsBearing
                android.location.Criteria.POWER_HIGH,
                android.location.Criteria.ACCURACY_FINE
            );

            lm.setTestProviderEnabled(MOCK_PROVIDER, true);

            // 3. 构造假位置
            Location mockLocation = new Location(MOCK_PROVIDER);
            mockLocation.setLatitude(latitude);
            mockLocation.setLongitude(longitude);
            mockLocation.setAccuracy(3.0f); // 精度（米）
            mockLocation.setTime(System.currentTimeMillis());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                mockLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
            }

            // 4. 推送位置
            lm.setTestProviderLocation(MOCK_PROVIDER, mockLocation);

            android.util.Log.d("MockLocation", "✅ Mock location set: " + latitude + ", " + longitude);

        } catch (SecurityException e) {
            android.util.Log.e("MockLocation", "❌ 没有被设为 Mock App！请去「开发者选项」选择本应用", e);
        } catch (Exception e) {
            android.util.Log.e("MockLocation", "❌ 设置 Mock 位置失败", e);
        }
    }

    public static void stopMockingLocation(Context context) {
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        try {
            lm.removeTestProvider(MOCK_PROVIDER);
        } catch (Exception e) {
            // ignore
        }
    }
}