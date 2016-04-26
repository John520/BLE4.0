package com.john.ble;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;





import com.john.bean.bleData;
import com.john.service.BluetoothLeService;

import android.os.Bundle;
import android.os.IBinder;
import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private final static String TAG = MainActivity.class.getSimpleName();
	public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
	public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
	private BluetoothLeService mmBluetoothLeService;
	private boolean mmConnected = false;
	private String mmDeviceName;
	private String mmDeviceAddress;
	private TextView mmConnectionState;
	public TextView tempture;
	public TextView speed;
	public TextView current;
	public TextView voltage;
	public TextView oretation1;
	public TextView oretation2;
	public TextView oretation3;
	private final String LIST_NAME = "NAME";
	private final String LIST_UUID = "UUID";
	private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
	private final ServiceConnection mmServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName componentName,
				IBinder service) {
			mmBluetoothLeService = ((BluetoothLeService.LocalBinder) service)
					.getService();
			if (!mmBluetoothLeService.initialize()) {
				Log.e(TAG, "Unable to initialize Bluetooth");
				finish();
			}
			// Automatically connects to the device upon successful start-up
			// initialization.
			mmBluetoothLeService.connect(mmDeviceAddress);
			System.out.println("service is connected");
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			mmBluetoothLeService = null;
			System.out.println("service is disconnected");
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		tempture = (TextView) findViewById(R.id.tep);
		speed = (TextView) findViewById(R.id.spe);
		current = (TextView) findViewById(R.id.cur);
		voltage = (TextView) findViewById(R.id.vol);
		oretation1 = (TextView) findViewById(R.id.deg1);
		oretation2 = (TextView) findViewById(R.id.deg2);
		oretation3 = (TextView) findViewById(R.id.deg3);
		final Intent intent = getIntent();
		mmDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
		System.out.println("intent:" + intent.toString());
		mmDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
		getActionBar().setTitle(mmDeviceName);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
		bindService(gattServiceIntent, mmServiceConnection, BIND_AUTO_CREATE);

	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(mmGattUpdateReceiver);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(mmServiceConnection);
		mmBluetoothLeService = null;
	}

	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver(mmGattUpdateReceiver, makeGattUpdateIntentFilter());
		if (mmBluetoothLeService != null) {
			final boolean result = mmBluetoothLeService
					.connect(mmDeviceAddress);
			Log.d(TAG, "Connect request result=" + result);
		}
	}

	private final BroadcastReceiver mmGattUpdateReceiver = new BroadcastReceiver() {
		// 接收广播
		@Override
		public void onReceive(Context context, Intent intent) {
			// 在onReceive方法内，我们可以获取随广播而来的Intent中的数据
			final String action = intent.getAction();
			if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
				mmConnected = true;
				Toast.makeText(context, "ACTION_GATT_CONNECTED",
						Toast.LENGTH_LONG);
				invalidateOptionsMenu();
				System.out.println("ACTION_GATT_CONNECTED");// 使菜单无效
			} else if (BluetoothLeService.ACTION_GATT_DISCONNECTED
					.equals(action)) {
				// //在@+id/connection_state空间设置为未连接状态
				mmConnected = false;
				Toast.makeText(context, "ACTION_GATT_DISCONNECTED",
						Toast.LENGTH_LONG);
				// updateConnectionState(R.string.disconnected);
				invalidateOptionsMenu();
				System.out.println("ACTION_GATT_DISCONNECTED");
			} else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED
					.equals(action)) {
				Toast.makeText(context, "ACTION_GATT_SERVICES_DISCOVERED",
						Toast.LENGTH_LONG);
				System.out.println("ACTION_GATT_SERVICES_DISCOVERED");
				setNotification(mmBluetoothLeService.getSupportedGattServices());
				System.out.println("setNotification successfully");
			} else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
			
				System.out.println("ACTION_DATA_AVAILABLE");
			   byte[]	data1 = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
				bleData data = bufferToObjiect(data1);
				System.out.println("进入show函数");
				
				System.out.println("进入show函数");
				System.out.println(""+data.getTmp());
				tempture.setText(""+data.getTmp());
				current.setText(""+data.getCur());
				voltage.setText(""+data.getVol());
				speed.setText(""+data.getSpd());
				oretation1.setText(""+data.getOri1());
				oretation2.setText(""+data.getOri2());
				oretation3.setText(""+data.getOri3());
		}
		}

		private void setNotification(List<BluetoothGattService> gattServices) {
			
			for (BluetoothGattService gattService : gattServices) {
				List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
				
				// Loops through available Characteristics.
				for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
				
					if (gattCharacteristic.getUuid().toString()
							.equals(BluetoothLeService.BleData.toString())) {
						mmBluetoothLeService.setCharacteristicNotification(
								gattCharacteristic, true);
					}
				}
				
			}
			
		}

		
	};

	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
		intentFilter
				.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
		return intentFilter;
	}
	public void show(bleData data) {
		this.tempture.setText(""+data.getTmp());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu（增加拆单）; this adds items to the action bar（操作栏） if it
		// is present.
		 getMenuInflater().inflate(R.menu.main, menu);
		 if (mmConnected) {
	             menu.findItem(R.id.menu_refresh).setVisible(false);
	             menu.findItem(R.id.menu_scan).setVisible(false);
	             menu.findItem(R.id.menu_stop).setVisible(false);
				 menu.findItem(R.id.menu_connect).setVisible(false);
				 menu.findItem(R.id.menu_disconnect).setVisible(true);
		 } else {
	             menu.findItem(R.id.menu_refresh).setVisible(false);
	             menu.findItem(R.id.menu_scan).setVisible(false);
	             menu.findItem(R.id.menu_stop).setVisible(false);
			     menu.findItem(R.id.menu_connect).setVisible(true);
				 menu.findItem(R.id.menu_disconnect).setVisible(false);
		 }
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case R.id.menu_connect:
			mmBluetoothLeService.connect(mmDeviceAddress);
			return true;
		case R.id.menu_disconnect:
			mmBluetoothLeService.disconnect();
			Toast.makeText(MainActivity.this, "连接已断开", Toast.LENGTH_LONG);
			return true;
		case android.R.id.home:
			onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public bleData bufferToObjiect(byte[] buffer) {
		System.out.println("进入bufferToObjiect");
		bleData data = new bleData();
		int i = -1;
		if(buffer.length==13){
				i = ((buffer[0] << 8) & 0xff00 | (buffer[1]) & 0xff);
				System.out.println(	""+buffer.length);
				data.setTmp(i/10);
		i = 0;
		i = (buffer[2] & 0xff);
		data.setCur(i - 50);
		i = 0;
		i = (buffer[3] & 0xff);
		data.setVol(i);
		i = 0;
		i = ((buffer[4] << 8) & 0xff00 | (buffer[5]) & 0xff);
		data.setSpd(i);
		i = 0;
		i = ((buffer[6] << 8) & 0xff00 | (buffer[7]) & 0xff);
		data.setOri1(i / 10-360);
		i = 0;
		i = ((buffer[8] << 8) & 0xff00 | (buffer[9]) & 0xff);
		data.setOri2(i  / 10-360);
		i = 0;
		i = ((buffer[10] << 8) & 0xff00 | (buffer[11]) & 0xff);
		data.setOri3(i  / 10-360);
		String s1= ""+data.getTmp();
		System.out.println(""+data.getTmp());
		String s2= ""+data.getCur();
		System.out.println(""+data.getCur());
		String s3= ""+data.getSpd();
		System.out.println(""+data.getSpd());
		String s4= ""+data.getOri1();
		System.out.println(""+data.getOri1());
		String s5= ""+data.getOri2();
		System.out.println(""+data.getOri2());
		String s6= ""+data.getOri3();
		System.out.println(""+data.getOri3());

		}else{
			System.out.println("数据格式不正确");
			
		}
	
		return data;
	}

}
