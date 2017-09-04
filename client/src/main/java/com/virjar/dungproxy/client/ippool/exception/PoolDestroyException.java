package com.virjar.dungproxy.client.ippool.exception;

/**
 * Created by virjar on 16/10/4.
 */
public class PoolDestroyException extends IllegalStateException {

	private static final long serialVersionUID = 1854587781742117563L;

	public PoolDestroyException() {
	}

	public PoolDestroyException(Throwable cause) {
		super(cause);
	}

	public PoolDestroyException(String message, Throwable cause) {
		super(message, cause);
	}

	public PoolDestroyException(String s) {
		super(s);
	}
}
