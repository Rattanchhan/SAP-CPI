package com.iservice.model.v4response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class GlobalStatus {
    private Integer Total;
    private Integer Cancelled;
    private Integer New;
    private Integer Failed;
    private Integer Pending;
	private Integer PartialCompleted;
    private Integer Completed;
	public Integer getTotal() {
		return Total;
	}
	public void setTotal(Integer total) {
		Total = total;
	}
	public Integer getCancelled() {
		return Cancelled;
	}
	public void setCancelled(Integer cancelled) {
		Cancelled = cancelled;
	}
	public Integer getNew() {
		return New;
	}
	public void setNew(Integer new1) {
		New = new1;
	}
	public Integer getFailed() {
		return Failed;
	}
	public void setFailed(Integer failed) {
		Failed = failed;
	}
	public Integer getPending() {
		return Pending;
	}
	public void setPending(Integer pending) {
		Pending = pending;
	}
	public Integer getPartialCompleted() {
		return PartialCompleted;
	}
	public void setPartialCompleted(Integer partialCompleted) {
		PartialCompleted = partialCompleted;
	}
	public Integer getCompleted() {
		return Completed;
	}
	public void setCompleted(Integer completed) {
		Completed = completed;
	}
    
}
