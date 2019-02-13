package com.aic.xj.app.ble;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;

import com.aic.xj.app.ble.BluetoothLeBaseClass.OnDataAvailableListener;
import com.aic.xj.app.ble.BluetoothLeBaseClass.OnDisconnectListener;
import com.aic.xj.app.ble.BluetoothLeBaseClass.OnServiceDiscoverListener;
import com.aic.xj.app.sdk.HandlerConstants;
import com.aic.xj.app.util.SyncUtil;

public class BluetoothLeAdapter {

	private BluetoothLeBaseClass _le = null;
	private Context _ctx = null;
	private String _readUUID = null;

	private ByteArrayOutputStream _readBuffer = null;
	private BluetoothGattCharacteristic _readCharacteristic = null;

	private SyncUtil _event = null;

	private BluetoothRequest _request = null;
	private byte[] _response = null;

	private String _writeUUID = null;
	private BluetoothGattCharacteristic _writeCharacteristic = null;

	private String _address = null;
	private Handler _handler = null;

	private int _connectWaitMilliseconds = 10000;

	private boolean _connected = false;

	public BluetoothLeAdapter(String address, Context ctx, Handler handler, String readUUID, String writeUUID) throws Exception {
		this(address, ctx, handler, readUUID, writeUUID, 7100);
	}

	public BluetoothLeAdapter(String address, Context ctx, Handler handler, String readUUID, String writeUUID, int connectWaitMilliseconds) throws Exception {
		this._address = address;
		this._handler = handler;
		this._ctx = ctx;
		this._readUUID = readUUID;
		this._readBuffer = new ByteArrayOutputStream();
		this._writeUUID = writeUUID;
		this._connectWaitMilliseconds = connectWaitMilliseconds;

		if (!_ctx.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			throw new Exception("unsupport bluetooth le");
		}

		_le = new BluetoothLeBaseClass(_ctx);
		if (!_le.initialize()) {
			throw new Exception("bluetooth le initialize failed");
		}
		_le.setOnServiceDiscoverListener(_onServiceDiscover);
		_le.setOnDataAvailableListener(_onDataAvailable);
		_le.setOnDisconnectListener(_onDisconnect);
	}

	private BluetoothLeBaseClass.OnServiceDiscoverListener _onServiceDiscover = new OnServiceDiscoverListener() {
		@Override
		public void onServiceDiscover(BluetoothGatt gatt) {
			List<BluetoothGattService> gattservices = _le.getSupportedGattServices();
			if (gattservices == null)
				return;

			for (BluetoothGattService service : gattservices) {
				List<BluetoothGattCharacteristic> gattCharacteristics = service.getCharacteristics();
				for (BluetoothGattCharacteristic characteristic : gattCharacteristics) {
					if (characteristic.getUuid().toString().equals(_writeUUID)) {
						_writeCharacteristic = characteristic;
					} else if (characteristic.getUuid().toString().equals(_readUUID)) {
						_readCharacteristic = characteristic;
					}
				}
			}

			if (_readCharacteristic != null && _writeCharacteristic != null) {
				try {
					Thread.sleep(_connectWaitMilliseconds);
				} catch (InterruptedException e) {
				}

				synchronized (this) {
					_connected = true;
				}
			}
			if (_event != null)
				_event.set();
		}
	};

	private BluetoothLeBaseClass.OnDataAvailableListener _onDataAvailable = new OnDataAvailableListener() {

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			if (characteristic.getUuid().toString().equals(_readUUID)) {
				synchronized (this) {
					byte[] value = characteristic.getValue();
					if (_request != null) {
						_request._packageReceiveCount++;
						try {
							_readBuffer.write(value);
						} catch (IOException e) {
						}
						if (_request._responseHeader != null && _request._responseHeader.length != 0) {
							if (!_request._headerCheck) {
								if (_readBuffer.size() >= _request._responseHeader.length) {
									byte[] temp = _readBuffer.toByteArray();
									for (int i = 0; i < _request._responseHeader.length; i++) {
										if (temp[i] != _request._responseHeader[i]) {
											if (_event != null)
												_event.set();
											break;
										}
									}
									_request._headerCheck = true;
								}
							}
						}

						if (_request._responseLength <= _readBuffer.size()) {
							_request = null;
							_response = _readBuffer.toByteArray();
							_readBuffer.reset();
							if (_event != null)
								_event.set();
						}
					}
				}
			}
		}
	};

	private BluetoothLeBaseClass.OnDisconnectListener _onDisconnect = new OnDisconnectListener() {

		@Override
		public void onDisconnect(BluetoothGatt gatt) {
			if (_address.equals(gatt.getDevice().getAddress())) {
				if (_handler != null) {
					_handler.obtainMessage(HandlerConstants.HANDLER_DEVICE_DISCONNECT, 0, 0, null).sendToTarget();
				}
			}
		}
	};

	public void startDiscovery() {
		_le.startDiscovery();
	}

	public void stopDiscovery() {
		_le.stopDiscovery();
	}

	public void enableBLE() {
		_le.enableBLE();
	}

	public void disableBLE() {
		_le.disableBLE();
	}

	public Set<BluetoothDevice> getBondedDevices() {
		return _le.getBondedDevices();
	}

	public int getBufferLength() {
		return _readBuffer.size();
	}

	public boolean isConnected() {
		return _connected;
	}

	public String connect(int timeoutMilliseconds) {
		synchronized (this) {
			if (!_connected) {
				String result = _le.connect(_address);
				if (result.equals("#OK")) {
					_event = new SyncUtil();
					try {
						_event.wait(timeoutMilliseconds);
					} catch (InterruptedException e) {
					}
				} else
					return result;
			}
			if (_connected) {
				return "#OK";
			} else {
				return "Failed on service discover";
			}
		}
	}

	public void disconnect() {
		synchronized (this) {
			_readBuffer.reset();
			_le.disconnect();
			_connected = false;
		}
	}

	public void close() {
		synchronized (this) {
			_readBuffer.reset();
			_readCharacteristic = null;
			_writeCharacteristic = null;
			_le.close();
			_connected = false;
		}
	}

	public byte[] call(BluetoothRequest request) {
		synchronized (this) {
			if (_connected && _readCharacteristic != null && _writeCharacteristic != null) {
				_readBuffer.reset();
				_request = request;
				_response = null;
				_le.setCharacteristicNotification(_writeCharacteristic, true);
				_writeCharacteristic.setValue(_request._requestData);
				_le.writeCharacteristic(_writeCharacteristic);
				_le.setCharacteristicNotification(_readCharacteristic, true);
				_le.readCharacteristic(_readCharacteristic);
				if (request._responseLength != 0) {
					_event = new SyncUtil();
					try {
						_event.wait(request._timeoutMilliseconds);
					} catch (InterruptedException e) {
					}
					if (_event.status) {
						return _response;
					} else
						return null;
				}
			}
		}
		return null;
	}
}
