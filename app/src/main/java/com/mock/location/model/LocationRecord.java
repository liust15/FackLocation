// com.mock.location.model.LocationRecord
package com.mock.location.model;

import java.util.List;

public class LocationRecord {
    public String name;               // 如 "记录1"
    public double lat;
    public double lng;
    public long timestamp;           // 记录时间
    public List<String> wifiBssids;  // 可选：WiFi BSSID 列表（用于高级伪装）
    public CellInfo cellInfo;        // 可选：基站信息

    public static class CellInfo {
        public int cid;   // Cell ID
        public int lac;   // Location Area Code
        public int mcc;   // Mobile Country Code
        public int mnc;   // Mobile Network Code
    }
}