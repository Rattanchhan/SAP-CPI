package com.iservice.model.v4response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;


@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobStatus {
    private String ExtValue;
    private String SalesforceId;
    private String MessageComment;
    private String MessageStatus;
    private String MessageId;
	public String getExtValue() {
		return ExtValue;
	}
	public void setExtValue(String extValue) {
		ExtValue = extValue;
	}
	public String getSalesforceId() {
		return SalesforceId;
	}
	public void setSalesforceId(String salesforceId) {
		SalesforceId = salesforceId;
	}
	public String getMessageComment() {
		return MessageComment;
	}
	public void setMessageComment(String messageComment) {
		MessageComment = messageComment;
	}
	public String getMessageStatus() {
		return MessageStatus;
	}
	public void setMessageStatus(String messageStatus) {
		MessageStatus = messageStatus;
	}
	public String getMessageId() {
		return MessageId;
	}
	public void setMessageId(String messageId) {
		MessageId = messageId;
	}
    
    
}
