package com.iservice.task;

import java.util.ArrayList;
import java.util.List;

import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiLoggingDecorator;
import org.apache.camel.util.json.JsonObject;
import org.apache.camel.util.json.Jsoner;
import org.apache.commons.lang3.StringUtils;

import com.iservice.adapter.IIntegrationProcessListener;
import com.iservice.adapter.MapAdapter;
import com.iservice.adapter.ResultsetMapper;
import com.iservice.gui.data.IMapping__c;
import com.iservice.model.IMessageTree;
import com.sforce.async.AsyncApiException;
import com.sforce.soap.partner.fault.ExceptionCode;
import com.sforce.soap.partner.fault.UnexpectedErrorFault;
import com.sforce.soap.schemas._class.IServices.IBean;

public abstract class AbstractIntegrationEvent {
	protected ResultsetMapper mapper = new ResultsetMapper();
	protected MapAdapter mapAdapter;
    protected static final CpiLoggingDecorator LOGGER = CpiLoggingDecorator.getLogger(AbstractIntegrationEvent.class);
	protected MapIntegrationInfo mapIntegrationInfo;
	protected List<List<IBean>> currentPackage = new ArrayList<List<IBean>>();
	protected List<IMessageTree> iMessage = new ArrayList<IMessageTree>();
	protected int numberOfTree = 0;
	protected List<IMessageTree> remainIMessage = new ArrayList<IMessageTree>();
	protected int remainNumOfTree;
	protected long totalRecord;
	protected List<IMapping__c> mappings;
	protected int preProcess =0;
	protected boolean error = false;
	
	public AbstractIntegrationEvent(long totalRecord,MapIntegrationInfo intInfo){
		this.mapIntegrationInfo = intInfo;
		this.totalRecord = totalRecord;
	}
	
	public abstract boolean doIntegration(boolean checkPackage)throws Exception;
	
	public boolean doTestIntegration(List<List<IBean>> records) throws Exception{
		if(isForceStop()) return false;
		if(currentPackage==null){
			currentPackage= new ArrayList<List<IBean>>();
		}
		if(records!=null){
			for(List<IBean> record:records){
				currentPackage.add(record);
			}
		}
		return doIntegration(false);
	}
	
	public boolean processIMessagesV3(List<IMessageTree> iMessage, int numberOfTree) throws Exception {
		if(isForceStop()) return false;
		
		// SF bulk use IBean to process so we will convert it from IMessage
		if(this instanceof AbstractBulkDirectObjectEvent) {
			for(IMessageTree imsg : iMessage) {
				if(imsg.getMessage().getBoolean("skyvvasolutions__isRoot__c")) {
					JsonObject data = (JsonObject) Jsoner.deserialize(imsg.getMessage().getString("skyvvasolutions__Data__c"));
					List<IBean> record = new ArrayList<IBean>();
					for(String key : data.keySet()) {					
						record.add(new IBean(key, data.getString(key)));
					}
					currentPackage.add(record);
					break;
				}
			}
		}else {
			this.iMessage.addAll(iMessage);
			this.numberOfTree += numberOfTree;
		}
		
		// always check if package is full then sent to salesforce 
		return doIntegration(true);
	}
	
	public boolean processRecords(List<List<IBean>> records) throws Exception{
		if(isForceStop()) return false;
		if(records!=null){
			for(List<IBean> record:records){
				processRecord(record);
			}
			//fix process by package size
//			if(currentPackage!=null && currentPackage.size()>0) doIntegration(false);
		}
		return true;
	}
	public boolean processRecord(List<IBean> record) throws Exception{
		if(isForceStop()) return false;
		if(currentPackage==null){
			currentPackage= new ArrayList<List<IBean>>();
		}
		currentPackage.add(record);
		// always check if package is full then sent to salesforce 
		return doIntegration(true);
	}
	
	public MapAdapter getAdapter() {
		return mapAdapter;
	}

	public MapIntegrationInfo getIntegrationInfo() {
		return mapIntegrationInfo;
	}
	
	protected boolean isForceStop(){
//		if(mapAdapter!=null) return mapAdapter.isForceStop();
		return false;
	}
	
	protected IIntegrationProcessListener getListener(){
		if(mapAdapter!=null) return mapAdapter.getProcessLisenter();
		return null;
	}
	
	/**
	 * send info back to caller
	 * @param numSent
	 */
	protected void onInProgressProcess(int numSent) {
//		if(getListener()!=null){
//			getListener().onProgress(numSent, totalRecord);
//		}
	}
	
	protected List<IMapping__c> getMapping() throws Exception {
		if (mappings == null && !mapIntegrationInfo.getInterfaces().isExternalMapping()) {
			//when an interface select external mapping mean that no mapping for it
			String interfaceId = mapIntegrationInfo.getInterfaceId();
			mappings = mapIntegrationInfo.getSFIntegrationService().getIMapping__c(interfaceId);
		}
		return mappings;
	}
	public void done(){
		//nothing todo
	}

	public boolean isError() {
		return error;
	}

	public void setError(boolean error) {
		this.error = error;
	}
	
	protected void reLoginSF() throws Exception {
		LOGGER.trace("Invalid Session Id> Re Login and Retry...!");
		this.mapIntegrationInfo.getISFService().setSessionId(null);
		this.mapIntegrationInfo.getISFService().login();
	}
	
	protected boolean isSessionExpired(Exception ex) {
		if (ex instanceof UnexpectedErrorFault) {
			UnexpectedErrorFault unexpectedErrorFault = (UnexpectedErrorFault) ex;
            return unexpectedErrorFault.getExceptionCode() == ExceptionCode.INVALID_SESSION_ID;
		} else if (ex instanceof AsyncApiException) {
			AsyncApiException asyncApiException = (AsyncApiException) ex;
            return StringUtils.equals(asyncApiException.getExceptionMessage(), "Invalid session id");
		}
		
		return false;
	}
}
