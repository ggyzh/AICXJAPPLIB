package com.aic.xj.app.ble;

import com.aic.xj.app.util.HexUtil;

public class BluetoothRequest {
	public int _requestType = 0;
	public int _responseLength = 0;
	public byte[] _responseHeader = null;
	public boolean _headerCheck = false;
	public byte[] _requestData = null;
	public int _timeoutMilliseconds = 5000;
	public int _packageReceiveCount = 0;

	public BluetoothRequest(int requestType,String requestData, int responseLength, byte[] responseHeader, int timeoutMilliseconds) {
		this._requestType = requestType;
		String r = requestData;
		if (r.startsWith("0x")) {
			r = r.substring(2);
		}
		if (r.contains(" ")) {
			r = r.replace(" ", "");
		}
		this._requestData = HexUtil.hexStringToBytes(r);
		this._responseLength = responseLength;
		this._responseHeader = responseHeader;
		this._timeoutMilliseconds = timeoutMilliseconds;
	}
	
	public BluetoothRequest(int requestType,byte[] requestData, int responseLength, byte[] responseHeader, int timeoutMilliseconds) {
		this._requestType = requestType;
		this._requestData = requestData;
		this._responseLength = responseLength;
		this._responseHeader = responseHeader;
		this._timeoutMilliseconds = timeoutMilliseconds;
	}
}
