package com.saints.ble;

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
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.saints.Thread.InputThread;

import java.util.UUID;


public class BlueTeen {
    private Context context;
    private BluetoothManager manager;
    private BluetoothAdapter adapter;
    private BluetoothDevice klbDevice;
    private BluetoothGatt gatt;
    private Handler handler = new Handler();
    private byte[] buffer = new byte[0];


    private static final UUID UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public BlueTeen(Context ctx) {
        context = ctx;
        manager =(BluetoothManager)context.getSystemService(Context.BLUETOOTH_SERVICE);
        adapter = manager.getAdapter();
    }

    public String connect() {
        if(!adapter.isEnabled()) {
            return "not open";
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                adapter.stopLeScan(scanCallback);
                if(klbDevice == null) {
                    Toast.makeText(context, "未搜索到相关设备", Toast.LENGTH_SHORT).show();
                }
            }
        }, 1000 * 30);
        Toast.makeText(context, "设备搜索中...", Toast.LENGTH_SHORT).show();
        adapter.startLeScan(scanCallback);
        return "";
    }

    public boolean unconnect() {
        klbDevice = null;
        if (gatt != null) {
            gatt.disconnect();
            gatt.close();
            gatt = null;
            return true;
        }
        return false;
    }

    private boolean enableNotifyCharacteristic(BluetoothGattCharacteristic characteristic, boolean enable) {
        //根据通知UUID找到通知特征
        boolean success = gatt.setCharacteristicNotification(characteristic, enable);
        if(!success) {
            return false;
        }
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR);
        if(descriptor == null) {
            return false;
        }
        descriptor.setValue(enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        return gatt.writeDescriptor(descriptor);
    }

    private BluetoothAdapter.LeScanCallback scanCallback = new BluetoothAdapter.LeScanCallback(){
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            if(klbDevice == null) {
                String name = device.getName();
                if (name !=null && name.indexOf("KLB") == 0) { //name
                    klbDevice = device;
                    Toast.makeText(context, "设备" + name + "连接中...", Toast.LENGTH_SHORT).show();
                    gatt = klbDevice.connectGatt(context, false, gattCallback);
                }
            }
        }
    };

    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,int newState){
            super.onConnectionStateChange(gatt, status, newState);
            final String name = klbDevice.getName();
            final int cur_status = status;
            if (newState == BluetoothProfile.STATE_CONNECTED) {

                Toast.makeText(context, name + "服务搜索中...", Toast.LENGTH_SHORT).show();
                // 连接成功 发现服务
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                unconnect();
                connect();
            } else {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, name + "连接出错(" + cur_status + ")", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //发现设备，遍历服务，初始化特征
                for (BluetoothGattService gattService : gatt.getServices()) {
                    for (BluetoothGattCharacteristic gattCharacteristic : gattService.getCharacteristics()) {
                        int charaProp = gattCharacteristic.getProperties();
                        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            //根据通知UUID找到通知特征
                            boolean success = enableNotifyCharacteristic(gattCharacteristic, true);
                            final String name = klbDevice.getName();
                            if(!success) {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(context, "设备" + name + "连接失败", Toast.LENGTH_SHORT).show();
                                    }
                                });

                            } else {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(context, "设备" + name + "连接成功", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                    }
                }
                String address = klbDevice.getAddress();
            } else {
                final int cur_status = status;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "服务搜索失败(" + cur_status + ")", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status){
            super.onCharacteristicRead(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                final byte[] data = characteristic.getValue();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, bytes2HexString(data), Toast.LENGTH_SHORT).show();
                    }
                });
                receiveData(data);
            }else{
                final int cur_status = status;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "读取失败(" + cur_status + ")", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,BluetoothGattCharacteristic characteristic){
            super.onCharacteristicChanged(gatt, characteristic);

            final byte[] data = characteristic.getValue();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, bytes2HexString(data), Toast.LENGTH_SHORT).show();
                }
            });
            receiveData(data);
        }

        /**
         /**
         * 收到BLE终端写入数据回调
         * @param gatt
         * @param characteristic
         * @param status
         */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // 发送成功

            } else {
                // 发送失败
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {

            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {

            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {

            }
        }
    };

    private void receiveData(byte[] data) {
        int keyCode = -1;
        if(data.length>=3 && ((int)data[0]) == -6 && ((int)data[2]) == -5) {
            int num = (int)data[1];
            if(num>=0x30 && num<=0x39) {
                keyCode = num - 41;
            } else if(num>=0x41 && num<=0x5A) {
                keyCode = num - 36;
            }
        }
        Log.d("TAG", "receive-->" + data.length);
        if(keyCode >= 0) {
            Thread thread = new InputThread(keyCode);
//            Thread thread = new InputThreadnum(keyCode);
            thread.start();
        }
    }

    private static String bytes2HexString(byte[] b) {
        String r = "";

        for (int i = 0; i < b.length; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            r += hex.toUpperCase();
        }

        return r;
    }
}
