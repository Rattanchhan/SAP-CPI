package com.model.iservice;

import com.model.iservice.base.PersistentDTO;

public class Map extends PersistentDTO{
private String source;
private String target;
private Integration integration;

public Integration getIntegration() {
	return integration;
}
public void setIntegration(Integration integration) {
	this.integration = integration;
}
public String getSource() {
	return source;
}
public void setSource(String source) {
	this.source = source;
}
public String getTarget() {
	return target;
}
public void setTarget(String target) {
	this.target = target;
}

}
