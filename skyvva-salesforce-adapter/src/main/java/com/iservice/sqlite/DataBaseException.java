package com.iservice.sqlite;

public class DataBaseException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2679068981680314290L;

	public DataBaseException() {
		super();
		
	}

	public DataBaseException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		
	}

	public DataBaseException(String message, Throwable cause) {
		super(message, cause);
		
	}

	public DataBaseException(String message) {
		super(message);
		
	}

	public DataBaseException(Throwable cause) {
		super(cause);
		
	}

}
