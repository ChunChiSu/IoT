package com.cht.iot.demo.pi;

public class HoldingTrigger<T> {	
	T data;		
	
	long last;
	boolean updated = false;
	
	long threshold = 100L;	
	
	Callback<T> callback;
	
	Thread thread;
	
	public HoldingTrigger(Callback<T> callback) {
		this.callback = callback;
		
		thread = new Thread(new Runnable() {
			public void run() {
				watch();				
			}
		});
		thread.start();
	}
	
	public void destroy() {
		thread.interrupt();
	}
	
	public void setThreshold(long threshold) {
		this.threshold = threshold;
	}
	
	public synchronized void update(T data) {
		this.data = data;
				
		last = System.currentTimeMillis();
		updated = true;
		
		notify();
	}
	
	protected void watch() {
		try {
			for (;;) {
				synchronized (this) {
					if (updated == false) {
						wait();
					}
				}
				
				while ((System.currentTimeMillis() - last) < threshold) {
					synchronized (this) {
						wait(threshold);
					}
				}
				
				synchronized (this) {				
					callback.onData(data);
					
					updated = false;
				}
			}
		} catch (InterruptedException ie) {
		}
	}
	
	interface Callback<T> {		
		void onData(T data);
	}
}
