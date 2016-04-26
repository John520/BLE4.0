package com.john.ble;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
@SuppressLint("NewApi")
public class ScanActivity extends ListActivity {

	    private LeDeviceListAdapter mLeDeviceListAdapter;
	    private BluetoothAdapter mBluetoothAdapter;//获取并开启本地蓝牙
	    private boolean mScanning;
	    private Handler mHandler;
	    
	    private static final int REQUEST_ENABLE_BT = 1;
	    // 10秒后停止查找搜索.
	    private static final long SCAN_PERIOD = 10000;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setTitle("我的设备");//设定操作栏为BLE Device Scan
        mHandler = new Handler();//handler不会被自己调用

        // 检查当前手机是否支持ble 蓝牙,如果不支持退出程序
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "不支持蓝牙", Toast.LENGTH_SHORT).show();//Toast:是一个类,主要管理消息的提示.
            finish();
        }

        // 初始化 Bluetooth adapter, 通过蓝牙管理器得到一个参考蓝牙适配器(API必须在以上android4.3或以上和版本)
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // 检查设备上是否支持蓝牙
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, " 错误 ", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
		
	}
		@Override
		protected void onResume() {
			  // 为了确保设备上蓝牙能使用, 如果当前蓝牙设备没启用,弹出对话框向用户要求授予权限来启用
		    super.onResume();

	        // 为了确保设备上蓝牙能使用, 如果当前蓝牙设备没启用,弹出对话框向用户要求授予权限来启用
	        if (!mBluetoothAdapter.isEnabled()) {
	            if (!mBluetoothAdapter.isEnabled()) {
	                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
	                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
	            }
	        }

	        // Initializes list view adapter.
	        mLeDeviceListAdapter = new LeDeviceListAdapter();
	        setListAdapter(mLeDeviceListAdapter);
	        scanLeDevice(true);
		}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	       getMenuInflater().inflate(R.menu.main, menu);
	        if (!mScanning) {
	            menu.findItem(R.id.menu_stop).setVisible(false);
	            menu.findItem(R.id.menu_scan).setVisible(true);
	            menu.findItem(R.id.menu_connect).setVisible(false);
	            menu.findItem(R.id.menu_disconnect).setVisible(false);
	        } else {
	            menu.findItem(R.id.menu_stop).setVisible(true);
	            menu.findItem(R.id.menu_scan).setVisible(false);
	            menu.findItem(R.id.menu_refresh).setActionView(
	                    R.layout.actionbar_indeterminate_progress);
	            menu.findItem(R.id.menu_connect).setVisible(false);
	            menu.findItem(R.id.menu_disconnect).setVisible(false);
	        }
	        return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {	
        case R.id.menu_scan:
            mLeDeviceListAdapter.clear();
            scanLeDevice(true);
            break;
        case R.id.menu_stop:
            scanLeDevice(false);
            break;
    }
    return true;
		
	}
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
    	System.out.println("==position=="+position);
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
        if (device == null) return;
        final Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRAS_DEVICE_NAME, device.getName());//putExtra("A",B)中，AB为键值对，第一个参数为键名，第二个参数为键对应的值。
        intent.putExtra(MainActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        if (mScanning) {														//如果在扫描，则停止扫描，如果已经不在扫描，则忽略
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }
        startActivity(intent);
    }
//onPause 用于由一个Activity转到另一个Activity、设备进入休眠状态(屏幕锁住了)、或者有dialog弹出时
    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
    }
	/********     *************/
    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = ScanActivity.this.getLayoutInflater();//得到本页面的xml
        }

        public void addDevice(BluetoothDevice device) {//已经存在则不再添加，没有存在的要继续添加
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {//获取设备
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();//清除设备
        }

        @Override
        public int getCount() {//用于获取数据的长度
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);//获取第几个item
        }

        @Override
        public long getItemId(int i) {//获取item id
            return i;
        }
 /*当界面每显示出来一个item时，就会调用该方法,getView()有三个参数，第一个参数表示该item在Adapter（适配器，接口）中的位置；
 * 第二个参数是item的View对象，是滑动list时将要显示在界面上的item，如果有item在显示界面消失，这时android
 * 会将消失的item返回，称为旧view，也就是说此时的view不为null；第三个参数用在加载xml视图。*/
        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {//当滑动list时，如果没有item消失，这时参数对象view是没有任何指向的，为null
                view = mInflator.inflate(R.layout.listitem_device, null);//mInflator是LayoutInflater类(布局加载器)实例对象，该行是动态加载布局
                viewHolder = new ViewHolder();//获得实例对象
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);//分别实例化显示Name和Address的textView控件
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);//向view中添加附加数据信息，在这里也就是两个textView对象
            } else {
                viewHolder = (ViewHolder) view.getTag();//如果有旧的view对象返回(该情况是滑动list时有item消失)，从该view中提取创建的两个已经创建的textView对象，达到对象循环使用
            }

            BluetoothDevice device = mLeDevices.get(i);//从搜索到的设备列表中得到显示位置设备对象。
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);//输出显示设备SSID
            else
                viewHolder.deviceName.setText("未知设备");
           // viewHolder.deviceAddress.setText(device.getAddress());//输出显示设备地址信息

            return view;
        }
    }
    /*************　　******************/
    // Device scan callback.通过uuid来查找设备
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLeDeviceListAdapter.addDevice(device);
                    mLeDeviceListAdapter.notifyDataSetChanged();
                }
            });
        }
    };
//    viewholder就是一个持有者的类，他里面一般没有方法，只有属性，作用就是一个临时的储存器，把你getView方法中每次返回的View存起来，
//    可以下次再用。这样做的好处就是不必每次都到布局文件中去拿到你的View，提高了效率。
    static class ViewHolder { //该类用来暂存textView的实例化对象，达到循环使用
        TextView deviceName;
        TextView deviceAddress;
    }

	
}
