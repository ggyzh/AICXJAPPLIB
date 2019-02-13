package com.aic.xj.app.sdk;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.content.Context;
import android.os.Handler;
import com.aic.xj.app.ble.BluetoothLeAdapter;
import com.aic.xj.app.ble.BluetoothLeAdvertisedData;
import com.aic.xj.app.ble.BluetoothRequest;
import com.aic.xj.app.resource.GBMap;
import com.aic.xj.app.util.BleUtil;
import com.aic.xj.app.util.StringUtil;

@SuppressLint("HandlerLeak")
public class SensorOperator {
	private BluetoothLeAdapter _le_adapter = null;
	private BluetoothAdapter _adapter = null;
	private Handler _handler = null;
	private Context _ctx = null;
	private Thread _scanThread = null;

	public Handler get_handler() {
		return _handler;
	}

	public Context get_Context() {
		return _ctx;
	}

	public boolean isOpen() {
		synchronized (this) {
			return _le_adapter != null;
		}
	}
	
	public boolean isDiscovering()
	{
		synchronized (this) {
			return _scanThread != null;
		}
	}

	private LeScanCallback _leCallback = new LeScanCallback() {
		@Override
		public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
			final BluetoothLeAdvertisedData badata = BleUtil.parseAdertisedData(scanRecord);
			String deviceName = device.getName();
			if (deviceName == null) {
				deviceName = badata.getName();
			}
			Sensor sensor = new Sensor();
			sensor.MAC = device.getAddress();
			if (!StringUtil.isNullOrWhiteSpace(deviceName)) {
				sensor.Name = GBMap.getInstance().decoding(deviceName);
			}
			sensor.Device = device;
			_handler.obtainMessage(HandlerConstants.HANDLER_NEW_DEVICE_DISCOVERED, 0, 0, sensor).sendToTarget();
		}
	};

	public SensorOperator(Handler handler, Context ctx) throws Exception {
		if (handler != null && ctx != null) {
			this._handler = handler;
			this._ctx = ctx;
			this._adapter = ((BluetoothManager) _ctx.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
		} else
			throw new Exception("handler and ctx is null");
	}

	//如果返回#OK，则连接成功，否则返回的是错误信息
	public String open(String mac) throws Exception {
		synchronized (this) {
			return this.open(mac, 15000);
		}
	}

	//如果返回#OK，则连接成功，否则返回的是错误信息
	public String open(String mac, int timeoutMilliseconds) throws Exception {
		synchronized (this) {
			this.close();
			this._le_adapter = new BluetoothLeAdapter(mac, _ctx, _handler, "00002a18-0000-1000-8000-00805f9b34fb", "00002a52-0000-1000-8000-00805f9b34fb");
			return this._le_adapter.connect(timeoutMilliseconds);
		}
	}

	//如果返回#OK，则连接成功，否则返回的是错误信息
	public String open(String mac, int timeoutMilliseconds, int connectWaitMilliseconds) throws Exception {
		synchronized (this) {
			this.close();
			this._le_adapter = new BluetoothLeAdapter(mac, _ctx, _handler, "00002a18-0000-1000-8000-00805f9b34fb", "00002a52-0000-1000-8000-00805f9b34fb", connectWaitMilliseconds);
			return this._le_adapter.connect(timeoutMilliseconds);
		}
	}

	// return null if timeout
	public byte[] ioControl(int requestType, String requestData, int timeout, int responseLength) throws Exception {
		synchronized (this) {
			if (_le_adapter != null) {
				BluetoothRequest br = new BluetoothRequest(requestType, requestData, responseLength, null, timeout);
				return _le_adapter.call(br);
			} else
				return null;
		}
	}

	public byte[] ioControl(int requestType, byte[] requestData, int timeout, int responseLength) throws Exception {
		synchronized (this) {
			if (_le_adapter != null) {
				BluetoothRequest br = new BluetoothRequest(requestType, requestData, responseLength, null, timeout);
				return _le_adapter.call(br);
			} else
				return null;
		}
	}

	public void startDiscovery(final int timeout) {
		synchronized (this) {
			if (!_adapter.isEnabled()) {
				_adapter.enable();
			}

			if (_scanThread == null) {
				_scanThread = new Thread() {
					@Override
					public void run() {
						_adapter.startLeScan(_leCallback);
						try {
							Thread.sleep(timeout);
						} catch (InterruptedException e) {
						}
						stopDiscovery();
						_handler.obtainMessage(HandlerConstants.HANDLER_DEVICE_DISCOVER_COMPLETED, 0, 0, null).sendToTarget();
					}
				};
				_scanThread.start();
			}
		}
	}

	public void stopDiscovery() {
		Thread temp = null;
		synchronized (this) {
			if (_scanThread != null) {
				_adapter.stopLeScan(_leCallback);
				temp = _scanThread;
				_scanThread = null;
			}
		}
		if (temp != null) {
			temp.interrupt();
		}
	}

	// failed code :
	// #IsNullOrWhiteSpace
	// #ContentTooLong
	// #UnsupportChar
	public String encodingDeviceName(String name) {
		return GBMap.getInstance().encoding(name);
	}

	public String decodingDeviceName(String name) {
		return GBMap.getInstance().decoding(name);
	}

	public synchronized void close() {
		synchronized (this) {
			if (_le_adapter != null) {
				_le_adapter.disconnect();
				_le_adapter.close();
				_le_adapter = null;
			}
		}
	}
}
