package com.model.iservice;

import com.model.iservice.base.PersistentDTO;


public class Property extends PersistentDTO{
	/**
	 * 
	 */
	private static final long serialVersionUID = 9100512563050329092L;
	private String name;
	private String value;
	private Adapter adapter;
	
	
	
	public Property(String name, String value) {
		super();
		this.name = name;
		this.value = value;
	}
	public Adapter getAdapter() {
		return adapter;
	}
	public void setAdapter(Adapter adapter) {
		this.adapter = adapter;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	
	
}
