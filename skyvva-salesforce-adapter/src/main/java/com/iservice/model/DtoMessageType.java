package com.iservice.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DtoMessageType {
	
	private String id;
	private String name;
	private List<DtoMessageType> sub;
	private Map<String,DtoMessageType> mSub;
	
	public DtoMessageType(String id, String name) {
		this.id = id;
		this.name = name;
		this.sub = new ArrayList<DtoMessageType>();
		this.mSub = new HashMap<String,DtoMessageType>();
	}

	public void addSub(DtoMessageType sub) {
		this.sub.add(sub);
		this.mSub.put(sub.name, sub);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<DtoMessageType> getSub() {
		return sub;
	}

	public void setSub(List<DtoMessageType> sub) {
		this.sub = sub;
	}

	public Map<String, DtoMessageType> getmSub() {
		return mSub;
	}

	public void setmSub(Map<String, DtoMessageType> mSub) {
		this.mSub = mSub;
	}
	
}