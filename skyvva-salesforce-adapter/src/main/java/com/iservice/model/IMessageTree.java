package com.iservice.model;

import org.apache.camel.util.json.JsonObject;

public class IMessageTree {
	private String rootId;
	private String parentId;
	private JsonObject msg;

	public IMessageTree () {}

	public IMessageTree (JsonObject msg,String rootId,String parentId) {
		this.msg = msg;
		this.rootId = rootId;
		this.parentId = parentId;
	}

	public String getRootId() {
		return rootId;
	}
	public void setRootId(String rootId) {
		this.rootId = rootId;
	}
	public String getParentId() {
		return parentId;
	}
	public void setParentId(String parentId) {
		this.parentId = parentId;
	}
	public JsonObject getMessage() {
		return msg;
	}
	public void setMessage(JsonObject message) {
		this.msg = message;
	}
}