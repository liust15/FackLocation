// 文件: com.mock.location.MockLocationInfo.java
package com.mock.location.model;

public class MockLocationInfo implements  java.io.Serializable{
    private double lat;
    private double lng;

    public MockLocationInfo() {}

    public MockLocationInfo(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    // Getters & Setters
    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }
    public double getLng() { return lng; }
    public void setLng(double lng) { this.lng = lng; }

    @Override
    public String toString() {
        return "MockLocationInfo{lat='" + lat + "', lng='" + lng + "'}";
    }
    private  static final MockLocationInfo  DEFAULT_VALUE = new MockLocationInfo(39.9042,116.4074);
    public static MockLocationInfo DefaultValue(){
        return  DEFAULT_VALUE;
    }
}