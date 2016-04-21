package com.cht.iot.demo.pi;

public class Future<T> {
	T object;
	
	public Future() {		
	}
	
	public synchronized T get() throws InterruptedException {
		if (object == null) {
			wait();
		}
		
		return object;
	}
	
	public synchronized void set(T object) {
		this.object = object;
		
		notify();
	}
}
