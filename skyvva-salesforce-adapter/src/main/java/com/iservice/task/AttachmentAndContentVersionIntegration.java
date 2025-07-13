package com.iservice.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiLoggingDecorator;

import com.iservice.gui.data.IMapping__c;
import com.sforce.soap.schemas._class.IServices.IBean;

public class AttachmentAndContentVersionIntegration extends
		FileIntegrationEvent {
    private static final CpiLoggingDecorator LOGGER = CpiLoggingDecorator.getLogger(AttachmentAndContentVersionIntegration.class);
	protected String attachmentBinaryFileFieldName;
	protected String folder;
	public AttachmentAndContentVersionIntegration(long totalRecord, MapIntegrationInfo intInfo) {
		super(totalRecord, intInfo); 
		//only FileMapAdapter can do content version with attachment
		// get reference field		
//		attachmentBinaryFileFieldName =adapter.getAdapter().getPropertyValue(PropertyNameHelper.REF_FIELD);
	}
	
	protected String getFolder() {
//		if (org.apache.commons.lang.StringUtils.isEmpty(folder)) {
//			// get attachment folder (BINARY_FOLDER) and assign to folder
//			folder = this.mapAdapter.getAdapter().getPropertyValue(PropertyNameHelper.BINARY_FOLDER);
//
//			// if folder of binary file is empty use folder of description file
//			// (CSV|XML) folder
//			if (StringUtils.isEmpty(folder)) {
//				folder = this.mapAdapter.getAdapter().getPropertyValue(PropertyNameHelper.FOLDER);
//			}
//		}
//		return folder;
		return null;
	}
	@Override
	public boolean processRecord(List<IBean> record)throws Exception {
//		if(StringUtils.isEmpty(attachmentBinaryFileFieldName))throw new Exception("No reference field define in adapter");
//		
//		if(isForceStop()) return false;
//		for(IBean field: record) { // loop field		
//			if(attachmentBinaryFileFieldName.equalsIgnoreCase(field.getName())) {		
//				String attachmentName = field.getValue();
//				String folder = getFolder();
//				List<IBean> attachment = new ArrayList<IBean>();
//				try {
//					attachment = reader.readContentFile(folder, attachmentName);
//					if(attachment.isEmpty()) {		
//						LOGGER.warn("Cannot find "+attachmentName +" in folder " + folder);		
//					}else {		
//						return attachFile(attachment, record);
//					}
//				}catch(IOException ex) {
//					LOGGER.warn("Cannot find "+attachmentName +" in folder " + folder);
//					return attachFile(attachment, record);
//				}
//				break;
//			}		
//		}	
		return true;

	}
	/**
	 * #3089
	 * @return SKYVVA integration agent limit: Integration Files size limit in agent <<< Salesforce attachment limit
	 * @param attachment		
	 * @param record : this is for other data read from attachment/contversion csv information file		
	 * @param attachmentOrContentVersion : this param is a flag for upload contentversion or attachment, (null mean attachment, "contentversion" mean upload contentversion)		
	 * @throws Exception
	 */
	protected  boolean attachFile(List<IBean> attachment, List<IBean> record) throws Exception{

		if(attachment.isEmpty())
			LOGGER.trace(">GenericSFTask>Start create failed message..");
		else 
			LOGGER.trace(">GenericSFTask>Start upload attachment..");
		
		String parentId="";
		List<IMapping__c> iMapping = getMapping();
		if (iMapping == null) {
			iMapping = new ArrayList<>();			
		}

		Map<String, String> data = new HashMap<>();
		//fileName, fileType
		
			if(record==null)record = new ArrayList<>();
			Map<String, String> mapData = GenericSFTask.convertListBeanToMap(record);
			String fileName="";
			if("ContentVersion".equalsIgnoreCase(iMapping.get(0).getTarget_Object__c())) {
				
				for(IMapping__c m : iMapping){					
					if(m.getTarget__c().equals("Title")) {
						
						fileName = mapData.get(m.getSource_Long__c());
					}else {
						if(!m.getTarget__c().equalsIgnoreCase("body"))data.put(m.getTarget__c(), mapData.get(m.getSource_Long__c()));
					}
				}
			}else {
				for(IMapping__c m : iMapping){					
					if(m.getTarget__c().equals("ParentId")){
						String formula = m.getSource_Long__c();//VLOOKUP....
						for(String key : mapData.keySet()) {
							if(formula.contains(key)) {
								formula = GenericSFTask.replaceLast(formula, key,  "\'"+mapData.get(key)+"\'");
								break;
							}
						}
						parentId = formula;
					}else if(m.getTarget__c().equals("Name")) {
						
						fileName = mapData.get(m.getSource_Long__c());
					}else {
						if(!m.getTarget__c().equalsIgnoreCase("body"))data.put(m.getTarget__c(), mapData.get(m.getSource_Long__c()));
					}
				}
			}
		
			 doAttachFile(attachment, data, iMapping,  parentId,fileName);
			 return true;
		
	}

}
