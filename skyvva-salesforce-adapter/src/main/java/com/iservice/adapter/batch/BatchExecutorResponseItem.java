package com.iservice.adapter.batch;

import java.util.ArrayList;
import java.util.List;

import com.sforce.soap.schemas._class.IServices.IBean;

public class BatchExecutorResponseItem {
	
	private Integer rowNumber;
	private String messageId;
	private String status; //completed or failed
	private String message; //comment
	List<List<IBean>> responseRecords = new ArrayList<List<IBean>>();
	
	/**
	 * @param rowNumber
	 * @param status
	 * @param message
	 */
	public BatchExecutorResponseItem(Integer rowNumber, String messageId, String status,
			String message) {
		super();
		this.rowNumber = rowNumber;
		this.messageId = messageId;
		this.status = status;
		this.message = message;
	}
	
	
	
	public Integer getRowNumber() {
		return rowNumber;
	}
	public void setRowNumber(Integer rowNumber) {
		this.rowNumber = rowNumber;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}

	public String getMessaegId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}
	
	public List<List<IBean>> getResponseRecords() {
		return responseRecords;
	}

	public void setResponseRecords(List<List<IBean>> responseRecords) {
		this.responseRecords = responseRecords;
	}

	public void setResponseRecords(IBean[] beans) {
		for(IBean tmp : beans) {
			if(tmp.getName().equals("response_SObjectId")) {
				IBean oneBean = new IBean(tmp.getName(), tmp.getValue());
				List<IBean> lsBeans = new ArrayList<IBean>();
				lsBeans.add(oneBean);
				List<List<IBean>> responseRec = new ArrayList<List<IBean>>();
				responseRec.add(lsBeans);
				this.setResponseRecords(responseRec);
				break;
			}
		}
	}

	

}
