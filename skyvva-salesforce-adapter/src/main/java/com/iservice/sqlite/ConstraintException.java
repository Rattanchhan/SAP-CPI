package com.iservice.sqlite;

public class ConstraintException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public ConstraintException() {
		super();
		
	}

	public ConstraintException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		
	}

	public ConstraintException(String message, Throwable cause) {
		super(message, cause);
		
	}

	public ConstraintException(String message) {
		super(message);
		
	}

	public ConstraintException(Throwable cause) {
		super(cause);
		
	}
	@Override
	public String getMessage() {
		return "A record that contains identical values to the record you are trying to create already exists .  If you would like to enter a new record, please ensure that the field values are unique.";
	}
}
