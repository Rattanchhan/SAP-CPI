package com.model.iservice;

import com.model.iservice.base.IPersistent;


public interface IIService  extends IPersistent {	
		
	public abstract String getDescription();	
	public abstract void setDescription(String description);
	
	public abstract String getName();
	public abstract void setName(String name);	

}
