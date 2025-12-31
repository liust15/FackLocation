// app/src/main/java/com/mock/location/model/SerializableCellInfo.java
package com.mock.location.model;

import java.io.Serializable;

public class SerializableCellInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    public String networkType;
    public int mcc = -1;
    public int mnc = -1;
    public int lac = -1;
    public int cid = -1;

    @Override
    public String toString() {
        return networkType + " MCC:" + mcc + " MNC:" + mnc + " LAC/TAC:" + lac + " CID:" + cid;
    }
}