package com.iservice.gui.helper;

public class RequiredFieldException extends Exception {

	public RequiredFieldException(){
		super();
	}
	
	public RequiredFieldException(String message){
		super(message);
	}
}
