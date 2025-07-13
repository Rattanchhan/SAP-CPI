package com.model.iservice;

import com.model.iservice.base.PersistentDTO;



public class IService extends PersistentDTO implements IIService{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected String name ;	 
	protected String description;
	protected String data;
	
	protected Integration integration;
	protected Adapter adapter;
	/**
	 * date when this file was uploaded (the last time)
	 */
	

	protected IService() {
	}


	
	


	public String getDescription() {
		return description;
	}
	
	
	
	public String getName() {

		return name;
	}



	


	public void setDescription(String desc) {
		this.description = desc;
	}

	


	public void setName(String name) {
		this.name = name;
		
	}



	



	public Integration getIntegration() {
		return integration;
	}



	public void setIntegration(Integration integration) {
		this.integration = integration;
	}



	public Adapter getAdapter() {
		return adapter;
	}



	public void setAdapter(Adapter adapter) {
		this.adapter = adapter;
	}






	public String getData() {
		return data;
	}






	public void setData(String data) {
		this.data = data;
	}



}
