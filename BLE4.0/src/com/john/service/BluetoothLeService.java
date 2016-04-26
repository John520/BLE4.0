/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.john.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import java.util.List;
import java.util.UUID;


/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given BluetoothLE device.
 */
@SuppressLint("NewApi")
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;		//用于后面初始化蓝牙
    private BluetoothAdapter mBluetoothAdapter;		//初始化蓝牙和其他接口
    private String mBluetoothDeviceAddress;			//在connect函数连接蓝牙时候
    private BluetoothGatt mBluetoothGatt;			//继承BluetoothProfile，通过BluetoothGatt可以连接设备（connect）,发现服务（discoverServices），并把相应地属性返回到BluetoothGattCallback 
    private int mConnectionState = STATE_DISCONNECTED;
   
    private BluetoothGattService  DataService;
    private BluetoothGattService  DeviceInfoService;
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED           = "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED        = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE           = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA                      = "com.example.bluetooth.le.EXTRA_DATA";

    public final static UUID     BleData   = UUID.fromString(SampleGattAttributes.Ble_Data);
    public  final static UUID    DeviceUUID     = UUID.fromString(SampleGattAttributes.Dev_UUID);
    public  final static UUID    DataUUID       =UUID.fromString(SampleGattAttributes.Data_UUID);
    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    //接口返回函数，当有APP事件发生时候调用，例如连接状态和发现服务时候调用
    //**************************************************************************************//
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
            	 System.out.println("BluetoothProfile.STATE_CONNECTED");
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");//连接GATT服务//在 控制台输出日志.
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +//尝试开始service搜索
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            	System.out.println("BluetoothProfile.STATE_DISCONNECTED");
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");//断开GATT server连接
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
            	System.out.println("onServicesDiscovered");
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
        	System.out.println("onCharacteristicRead");
            if (status == BluetoothGatt.GATT_SUCCESS) {
            	System.out.println("onCharacteristicReadsucess");
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
        	System.out.println("onCharacteristicChanged");
        	System.out.println(characteristic.getValue().toString());
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };
//*****************************************************************************************//
    
    
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

	private void broadcastUpdate(final String action,// 广播更新
			final BluetoothGattCharacteristic characteristic) {
		System.out.println("进入broadcastUpdate");
		final Intent intent = new Intent(action);
		final byte[] data = characteristic.getValue();
		  System.out.println((intent.getAction().toString()));
     System.out.println((data.toString()));
      //放入byte[]数据
		intent.putExtra(EXTRA_DATA, data);
		System.out.println(characteristic.getUuid().toString());
		sendBroadcast(intent);
	}

    public class LocalBinder extends Binder {
            public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {//解放**********************************************
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked(召唤) when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through     初始化蓝牙适配器************
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {                              //连接远程设备。
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {										//断开与远程设备的GATT连接。
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {											//关闭GATT Client端。
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously（异步） through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {//读取指定的characteristic。
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification（通告，通知） on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        List<BluetoothGattDescriptor> descriptors=characteristic.getDescriptors();
        
        for(BluetoothGattDescriptor dp:descriptors){
        	
        	dp.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        	mBluetoothGatt.writeDescriptor(dp);}
    }

    /**
     * Retrieves（恢复） a list of supported GATT services on the connected device. This should be
     * invoked（召唤） only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {//*****************************在device =controlactivity 中有调用
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();					//获取远程设备所支持的services。
    }
    public BluetoothGattService getDataService(){
    	
    	 if (mBluetoothGatt == null) return null;
    	 if (mBluetoothGatt.getService(DataUUID)==null)return null;
      	 else return mBluetoothGatt.getService(DataUUID);
    }
    public BluetoothGattService getDeviceInfoService(){
    	
    	 if (mBluetoothGatt == null) return null;
    	 if (mBluetoothGatt.getService(DeviceUUID)==null)return null;
      	 else return mBluetoothGatt.getService(DeviceUUID);
    }
}
