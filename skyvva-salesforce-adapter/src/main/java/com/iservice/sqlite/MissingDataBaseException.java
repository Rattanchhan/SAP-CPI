package com.iservice.sqlite;

public class MissingDataBaseException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2581007296827585020L;

	public MissingDataBaseException() {
		super();
		
	}

	public MissingDataBaseException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		
	}

	public MissingDataBaseException(String message, Throwable cause) {
		super(message, cause);
		
	}

	public MissingDataBaseException(String message) {
		super(message);
		
	}

	public MissingDataBaseException(Throwable cause) {
		super(cause);
		
	}

}
