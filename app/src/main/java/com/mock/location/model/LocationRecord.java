// app/src/main/java/com/mock/location/model/LocationRecord.java
package com.mock.location.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class LocationRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    public String name;
    public double lat;
    public double lng;
    public long timestamp;
    public List<String> wifiBssids = new ArrayList<>();
    public SerializableCellInfo cellInfo;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public List<String> getWifiBssids() {
        return wifiBssids;
    }

    public void setWifiBssids(List<String> wifiBssids) {
        this.wifiBssids = wifiBssids;
    }

    public SerializableCellInfo getCellInfo() {
        return cellInfo;
    }

    public void setCellInfo(SerializableCellInfo cellInfo) {
        this.cellInfo = cellInfo;
    }

    public LocationRecord() {}

    public LocationRecord(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
        this.timestamp = System.currentTimeMillis();
    }

    public static LocationRecord DefaultValue() {
        return new LocationRecord(39.909, 116.397);
    }
}