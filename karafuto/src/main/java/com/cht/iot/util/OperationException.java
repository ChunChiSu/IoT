package com.cht.iot.util;

public class OperationException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public OperationException(String message, Throwable cause) {
		super(message, cause);
	}
}
