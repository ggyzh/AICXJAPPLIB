package com.aic.xj.app.util;

public class SyncUtil {
	private final Object _monitor = new Object();
	public boolean status = false;

	public void wait(int timeout) throws InterruptedException {
		synchronized (_monitor) {
			_monitor.wait(timeout);
		}
	}

	public void set() {
		synchronized (_monitor) {
			status = true;
			_monitor.notifyAll();
		}
	}
}
