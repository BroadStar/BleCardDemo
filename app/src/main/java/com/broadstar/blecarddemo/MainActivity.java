package com.broadstar.blecarddemo;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

	private static final String SERVICE_UUID = "0000FF92-0000-1000-8000-00805F9B34FB";

	private static final long SCAN_PERIOD = 10000;
	private static final int REQUEST_ENABLE_BT = 1;
	private static final int REQUEST_ACCESS_COARSE_LOCATION = 2;

	private ListView lv_deviceList;

	private SwipeRefreshLayout refreshLayout;

	private BluetoothAdapter bluetoothAdapter;

	private Handler mHandler = null;
	private boolean mScanning = false;

	List<Map<String, Object>> deviceList = new ArrayList<>();
	SimpleAdapter deviceListAdapter;

	// Device scan callback.
	private BluetoothAdapter.LeScanCallback mLeScanCallback =
			new BluetoothAdapter.LeScanCallback() {

				@Override
				public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
					for (Map<String, Object> oldDevice : deviceList) {
						BluetoothDevice bluetoothDevice = (BluetoothDevice) oldDevice.get("device");
						if (bluetoothDevice.getAddress().equals(device.getAddress())) {
							oldDevice.put("deviceName", device.getName() + " ( " + device.getAddress() + " )" + " ( " + rssi + "dB" + " )");
							oldDevice.put("deviceRssi", getSignalResource(rssi));
							notifyDeviceChanged();
							return;
						}
					}
					Map<String, Object> deviceItem = new HashMap<>();
					deviceItem.put("deviceName", device.getName() + " ( " + device.getAddress() + " )" + " ( " + rssi + "dB" + " )");
					deviceItem.put("deviceRssi", getSignalResource(rssi));
					deviceItem.put("device", device);
					deviceList.add(deviceItem);
					notifyDeviceChanged();
					Log.d(device.getName(), ": " + device.getAddress());
				}
			};

	private void notifyDeviceChanged() {
		runOnUiThread(new Runnable() {
				@Override
				public void run() {
					deviceListAdapter.notifyDataSetChanged();
				}
			});
	}

	private int getSignalResource(int rssi) {
		if (rssi >= -40) {
			return R.drawable.signal_wifi_4;
		} else if (rssi >= -50) {
			return R.drawable.signal_wifi_3;
		} else if (rssi >= -60) {
			return R.drawable.signal_wifi_2;
		} else{
			return R.drawable.signal_wifi_1;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		initViews();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_ENABLE_BT) {
			if (resultCode == Activity.RESULT_OK) {
				Log.d("onActivityResult", "蓝牙开启成功");
				scanLeDevice(true);
			} else {
				Log.w("onActivityResult", "蓝牙开启失败");
			}
		}
	}

	@Override
	public void onDestroy() {
		if (mScanning)
			scanLeDevice(false);
		super.onDestroy();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (requestCode == REQUEST_ACCESS_COARSE_LOCATION) {
			if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				UUID[] servicesUuids = new UUID[1];
				servicesUuids[0] = UUID.fromString(SERVICE_UUID);
				bluetoothAdapter.startLeScan(servicesUuids, mLeScanCallback);
			}
		}
	}

	private void initViews() {

		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		mHandler = new Handler();

		lv_deviceList = findView(R.id.lv_device_list);

		deviceListAdapter = new SimpleAdapter(MainActivity.this, deviceList, R.layout.item_device_list, new String[] { "deviceName", "deviceRssi" }, new int[] { R.id.device_name, R.id.device_rssi });
		lv_deviceList.setAdapter(deviceListAdapter);

		refreshLayout = findView(R.id.refreshLayout);
		refreshLayout.setColorSchemeColors(Color.RED);
		refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				if (bluetoothAdapter != null) {
					if (!bluetoothAdapter.isEnabled()) {
						Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
						startActivityForResult(enableBTIntent, REQUEST_ENABLE_BT);
					} else {
						scanLeDevice(true);
					}
				} else {
					Log.d("bluetoothAdapter", "bluetoothAdapter == null");
				}
			}
		});

		lv_deviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (mScanning)
					scanLeDevice(false);
				Intent intent = new Intent(MainActivity.this, DeviceActivity.class);
				intent.putExtra("BluetoothDevice", (BluetoothDevice) deviceList.get(position).get("device"));
				startActivity(intent);
			}
		});

	}

	private void scanLeDevice(final boolean enable) {
		if (enable) {
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					if (mScanning)
						scanLeDevice(false);
				}
			}, SCAN_PERIOD);

			deviceList.clear();
			notifyDeviceChanged();
			mScanning = true;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
						!= PackageManager.PERMISSION_GRANTED) {

					requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
							REQUEST_ACCESS_COARSE_LOCATION);
				} else {
					bluetoothAdapter.startLeScan(mLeScanCallback);
				}
			} else {
				bluetoothAdapter.startLeScan(mLeScanCallback);
			}
		} else {
			mScanning = false;
			bluetoothAdapter.stopLeScan(mLeScanCallback);
			refreshLayout.setRefreshing(false);
		}
	}

	private <T extends View> T findView(int id) {
		return (T) findViewById(id);
	}

}
