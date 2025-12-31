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

    public LocationRecord() {}

    public LocationRecord(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
        this.timestamp = System.currentTimeMillis();
    }
}