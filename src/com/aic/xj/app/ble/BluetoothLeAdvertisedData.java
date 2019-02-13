package com.aic.xj.app.ble;

import java.util.List;
import java.util.UUID;

public class BluetoothLeAdvertisedData {
    private List<UUID> mUuids;
    private String mName;
    public BluetoothLeAdvertisedData(List<UUID> uuids, String name){
        mUuids = uuids;
        mName = name;
    }

    public List<UUID> getUuids(){
        return mUuids;
    }

    public String getName(){
        return mName;
    }
}