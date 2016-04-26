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
	    private BluetoothAdapter mBluetoothAdapter;//��ȡ��������������
	    private boolean mScanning;
	    private Handler mHandler;
	    
	    private static final int REQUEST_ENABLE_BT = 1;
	    // 10���ֹͣ��������.
	    private static final long SCAN_PERIOD = 10000;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setTitle("�ҵ��豸");//�趨������ΪBLE Device Scan
        mHandler = new Handler();//handler���ᱻ�Լ�����

        // ��鵱ǰ�ֻ��Ƿ�֧��ble ����,�����֧���˳�����
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "��֧������", Toast.LENGTH_SHORT).show();//Toast:��һ����,��Ҫ������Ϣ����ʾ.
            finish();
        }

        // ��ʼ�� Bluetooth adapter, ͨ�������������õ�һ���ο�����������(API����������android4.3�����ϺͰ汾)
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // ����豸���Ƿ�֧������
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, " ���� ", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
		
	}
		@Override
		protected void onResume() {
			  // Ϊ��ȷ���豸��������ʹ��, �����ǰ�����豸û����,�����Ի������û�Ҫ������Ȩ��������
		    super.onResume();

	        // Ϊ��ȷ���豸��������ʹ��, �����ǰ�����豸û����,�����Ի������û�Ҫ������Ȩ��������
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
        intent.putExtra(MainActivity.EXTRAS_DEVICE_NAME, device.getName());//putExtra("A",B)�У�ABΪ��ֵ�ԣ���һ������Ϊ�������ڶ�������Ϊ����Ӧ��ֵ��
        intent.putExtra(MainActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        if (mScanning) {														//�����ɨ�裬��ֹͣɨ�裬����Ѿ�����ɨ�裬�����
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }
        startActivity(intent);
    }
//onPause ������һ��Activityת����һ��Activity���豸��������״̬(��Ļ��ס��)��������dialog����ʱ
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
            mInflator = ScanActivity.this.getLayoutInflater();//�õ���ҳ���xml
        }

        public void addDevice(BluetoothDevice device) {//�Ѿ�����������ӣ�û�д��ڵ�Ҫ�������
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {//��ȡ�豸
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();//����豸
        }

        @Override
        public int getCount() {//���ڻ�ȡ���ݵĳ���
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);//��ȡ�ڼ���item
        }

        @Override
        public long getItemId(int i) {//��ȡitem id
            return i;
        }
 /*������ÿ��ʾ����һ��itemʱ���ͻ���ø÷���,getView()��������������һ��������ʾ��item��Adapter�����������ӿڣ��е�λ�ã�
 * �ڶ���������item��View�����ǻ���listʱ��Ҫ��ʾ�ڽ����ϵ�item�������item����ʾ������ʧ����ʱandroid
 * �Ὣ��ʧ��item���أ���Ϊ��view��Ҳ����˵��ʱ��view��Ϊnull���������������ڼ���xml��ͼ��*/
        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {//������listʱ�����û��item��ʧ����ʱ��������view��û���κ�ָ��ģ�Ϊnull
                view = mInflator.inflate(R.layout.listitem_device, null);//mInflator��LayoutInflater��(���ּ�����)ʵ�����󣬸����Ƕ�̬���ز���
                viewHolder = new ViewHolder();//���ʵ������
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);//�ֱ�ʵ������ʾName��Address��textView�ؼ�
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);//��view����Ӹ���������Ϣ��������Ҳ��������textView����
            } else {
                viewHolder = (ViewHolder) view.getTag();//����оɵ�view���󷵻�(������ǻ���listʱ��item��ʧ)���Ӹ�view����ȡ�����������Ѿ�������textView���󣬴ﵽ����ѭ��ʹ��
            }

            BluetoothDevice device = mLeDevices.get(i);//�����������豸�б��еõ���ʾλ���豸����
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);//�����ʾ�豸SSID
            else
                viewHolder.deviceName.setText("δ֪�豸");
           // viewHolder.deviceAddress.setText(device.getAddress());//�����ʾ�豸��ַ��Ϣ

            return view;
        }
    }
    /*************����******************/
    // Device scan callback.ͨ��uuid�������豸
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
//    viewholder����һ�������ߵ��࣬������һ��û�з�����ֻ�����ԣ����þ���һ����ʱ�Ĵ�����������getView������ÿ�η��ص�View��������
//    �����´����á��������ĺô����ǲ���ÿ�ζ��������ļ���ȥ�õ����View�������Ч�ʡ�
    static class ViewHolder { //���������ݴ�textView��ʵ�������󣬴ﵽѭ��ʹ��
        TextView deviceName;
        TextView deviceAddress;
    }

	
}
