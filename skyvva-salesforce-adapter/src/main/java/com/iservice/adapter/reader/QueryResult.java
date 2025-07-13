package com.iservice.adapter.reader;

import java.util.List;

import com.sforce.soap.schemas._class.IServices.IBean;

public class QueryResult {
	protected int totalRec;
	protected boolean differentCol = false;
	protected List<List<IBean>> result;	
	
	public QueryResult(){
		
	}
	public QueryResult(int totalRec, List<List<IBean>> result) {
		super();
		this.totalRec = totalRec;
		this.result = result;
	}
	public int getTotalRec() {
		return totalRec;
	}
	public void setTotalRec(int totalRec) {
		this.totalRec = totalRec;
	}
	public List<List<IBean>> getResult() {
		return result;
	}
	public void setResult(List<List<IBean>> result) {
		this.result = result;
	}
	public boolean isDifferentCol() {
		return differentCol;
	}
	public void setDifferentCol(boolean differentCol) {
		this.differentCol = differentCol;
	}
}
