package com.model.iservice;

import com.model.iservice.base.IPersistent;

public interface IIntegration extends IPersistent{
		
	public abstract void setName(String name );
	public abstract void setDescription(String desc );
	
	public abstract String getName();
	public abstract String getDescription();

}
