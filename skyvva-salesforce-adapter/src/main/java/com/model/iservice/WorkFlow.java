package com.model.iservice;

import com.model.iservice.base.PersistentDTO;


public class WorkFlow extends PersistentDTO{
	/**
	 * 
	 */
	private static final long serialVersionUID = 9098330109418093689L;
	private String description;
	private String status;
	private String task;
	private String schedule;
	
	private Integration integration ; //which work flow relates to
		
	public Integration getIntegration() {
		return integration;
	}
	public void setIntegration(Integration integration) {
		this.integration = integration;
	}
	public String getSchedule() {
		return schedule;
	}
	public void setSchedule(String schedule) {
		this.schedule = schedule;
	}
	public String getTask() {
		return task;
	}
	public void setTask(String task) {
		this.task = task;
	}
	
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	
	
}
