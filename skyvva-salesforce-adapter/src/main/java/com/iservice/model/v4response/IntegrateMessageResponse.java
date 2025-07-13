package com.iservice.model.v4response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class IntegrateMessageResponse {
    private GlobalStatus GlobalStatus;
    private List<JobStatus> Cancelled;
    private List<JobStatus> New;
    private List<JobStatus> Failed;
    private List<JobStatus> Pending;
    private List<JobStatus> PartialCompleted;
    private List<JobStatus> Completed;
    private String StatusMsg;
    private String StatusCode;
    
	public GlobalStatus getGlobalStatus() {
		return GlobalStatus;
	}
	public void setGlobalStatus(GlobalStatus globalStatus) {
		GlobalStatus = globalStatus;
	}
	public List<JobStatus> getCancelled() {
		return Cancelled;
	}
	public void setCancelled(List<JobStatus> cancelled) {
		Cancelled = cancelled;
	}
	public List<JobStatus> getNew() {
		return New;
	}
	public void setNew(List<JobStatus> new1) {
		New = new1;
	}
	public List<JobStatus> getFailed() {
		return Failed;
	}
	public void setFailed(List<JobStatus> failed) {
		Failed = failed;
	}
	public List<JobStatus> getPending() {
		return Pending;
	}
	public void setPending(List<JobStatus> pending) {
		Pending = pending;
	}
	public List<JobStatus> getPartialCompleted() {
		return PartialCompleted;
	}
	public void setPartialCompleted(List<JobStatus> partialCompleted) {
		PartialCompleted = partialCompleted;
	}
	public List<JobStatus> getCompleted() {
		return Completed;
	}
	public void setCompleted(List<JobStatus> completed) {
		Completed = completed;
	}
	public String getStatusMsg() {
		return StatusMsg;
	}
	public void setStatusMsg(String statusMsg) {
		StatusMsg = statusMsg;
	}
	public String getStatusCode() {
		return StatusCode;
	}
	public void setStatusCode(String statusCode) {
		StatusCode = statusCode;
	}
    
    
}
