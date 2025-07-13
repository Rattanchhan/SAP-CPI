package com.model.iservice;

import java.util.ArrayList;
import java.util.List;

import com.model.iservice.base.PersistentDTO;


public class Integration extends PersistentDTO implements IIntegration {
	
	protected String businessId;
	protected String name ; 
	protected String description;
		
	protected List<Source> source = new ArrayList<Source>(0);
	protected List<Target> target = new ArrayList<Target>(0);
	
	// store a list of scheduled tasks
	protected List<WorkFlow> workflow = new ArrayList<WorkFlow>(0);
	protected List<Map> map = new ArrayList<Map>(0);
	

	public List<WorkFlow> getWorkflow() {
		return workflow;
	}

	public void setWorkflow(List<WorkFlow> workflow) {
		this.workflow = workflow;
	}

	
	public List<Source> getSource() {
		return source;
	}

	public void setSource(List<Source> source) {
		this.source = source;
	}

	public List<Target> getTarget() {
		return target;
	}

	public void setTarget(List<Target> target) {
		this.target = target;
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

	public String getBusinessId() {
		return businessId;
	}

	public void setBusinessId(String businessId) {
		this.businessId = businessId;
	}

	public List<Map> getMap() {
		return map;
	}

	public void setMap(List<Map> map) {
		this.map = map;
	}
	
}
