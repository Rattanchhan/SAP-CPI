package com.iservice.model;


public abstract class AbstractDataModel {
	protected Long agent_id;
	public Long getAgent_id(){
		return agent_id;
	}
	
	public void setAgent_id(Long id){
		this.agent_id=id;
	}
}
