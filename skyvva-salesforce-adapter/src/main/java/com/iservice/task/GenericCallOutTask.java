package com.iservice.task;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiLoggingDecorator;
import org.apache.commons.lang3.StringUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.iservice.gui.data.IChained_Interfaces__c;
import com.iservice.gui.data.Integration__c;
import com.iservice.gui.data.Interfaces__c;
import com.iservice.sforce.SFIntegrationService;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.soap.schemas._class.IServices.IBean;

/**
 * GenericCallOutTask
 * invokecallout from sf side
 * @author SOKDET
 *
 */
public class GenericCallOutTask extends GenericSFTask{

    private static final CpiLoggingDecorator LOGGER = CpiLoggingDecorator.getLogger(GenericCallOutTask.class);
	
	public String myPackage() {
		return GenericSFTask.DEMO32?"":"skyvvasolutions__";
	}
	
	IBean[][] records;
//	IDBConnection adapter;
	SFIntegrationService sfIntegrationService;
	
	List<SObject> lstSObjMessage;
	JsonArray lstMessage;
	Interfaces__c interfacee;
	Integration__c integration; 

	public GenericCallOutTask() throws Exception{
		super();
	}
	
	public GenericCallOutTask(Map<String, Object> mapObj) throws Exception{
		doInit(mapObj);
	}
	
	private void doInit(Map<String, Object> mapObj) throws Exception {
//		// get interface
//		this.interfacee = getInterfacee(mapObj);
//		
//		// after get intf will have integration and RoutingLogFileName
//		GenericSFTask.RoutingLogFileName(this.integration.getName()+"_"+this.integration.getId());
//		
//		log.trace("GenericCallOutTask() >doInit() ...");
//		
//		// get records
//		List<Map<String,Object>> lstRecords = (List<Map<String, Object>>) mapObj.get("records");
//		this.records =  recordsToIBean(lstRecords);
//		
//		// get message
//		this.lstSObjMessage = getListSObjectMessage(mapObj); 
//		
//		// get message
//		this.lstMessage = getListMessage(mapObj);
//		
//		// get adapter
//		Map<String,Object> mapAdp = (Map<String, Object>) mapObj.get("mapAdp");
//		Map<String,String> mAdp = (Map<String, String>) mapAdp.get("adp"); // adp
//		Map<String,String> mapProp = (Map<String, String>) mapAdp.get("mapProp"); // mapProp
//		mapProp.put("adpName", mAdp.get(myPackage()+"Name__c"));
//		mapProp.put(PropertyNameHelper.ADAPTER_TYPE, mAdp.get(myPackage()+"Type__c"));
//		this.adapter = new CoreIntegration().getAdapter(mapProp);
//		this.adapter.setTableName(interfacee.getSource_Name__c());
//		
//		// create sf service
//		String fileName = mapProp.get("agent_instance")!=null ? mapProp.get("agent_instance") : mapProp.get("adapter_instance");
//		if(fileName==null || fileName.isEmpty()) throw new Exception("There is no adapter instance.");
//		MapSFConnInfo mapConInfo = BaseDao.getDao(PropertySettingDao.class).getAllValueByFileName(fileName);
//		sfIntegrationService = new SFIntegrationService(mapConInfo);
//		sfIntegrationService.clearAllMap();
	}
	
	// getInterfacee
	private Interfaces__c getInterfacee(Map<String, Object> mapObj) throws Exception {
		Map<String, Object> mapIntf = (Map<String, Object>) mapObj.get("mapIntf");
		Interfaces__c intf = new Interfaces__c(myPackage());
		for(String key : mapIntf.keySet()) {
			if(StringUtils.contains(key, myPackage()+"Parent_IChained_Interfaces__r")) {
				Map<String,Object> mapCh = (Map<String, Object>) mapIntf.get(key);
				List<Map<String,Object>> lstMapCh = (List<Map<String, Object>>) mapCh.get("records");
				List<IChained_Interfaces__c> lstCh = new ArrayList<IChained_Interfaces__c>();
				for(Map<String,Object> map : lstMapCh) {
					IChained_Interfaces__c ch = new IChained_Interfaces__c(myPackage());
					for(String str : map.keySet()) {
						ch.put(str.replaceAll(myPackage(),""), map.get(str));
					}
					lstCh.add(ch);
				}
				intf.put(key.replaceAll(myPackage(),""), lstCh);
			}else if (StringUtils.contains(key, myPackage()+"Integration__r")) {
				Map<String,Object> mapIntg = (Map<String, Object>) mapIntf.get(key);
				Integration__c integration = new Integration__c(myPackage());
				integration.setId(SFIntegrationService.getIntegrationId15(String.valueOf(mapIntg.get("Id"))));
				integration.setName(String.valueOf(mapIntg.get("Name")));
				this.integration = integration;
			}else {
				intf.put(key.replaceAll(myPackage(),""), mapIntf.get(key));
			}
		}
		return intf;
	}
	
	// getMessage
	private List<SObject> getListSObjectMessage(Map<String, Object> mapObj){
		List<SObject> lstSObj = new ArrayList<SObject>();
		Map<String,Object> mapMr = (Map<String, Object>) mapObj.get("mapMr");
		List<Map<String, String>> lstMsg = (List<Map<String, String>>) mapMr.get("listMessage"); // listMessage
		for(Map<String,String> mapMsg : lstMsg) {
			SObject sobj = new SObject();
			sobj.setType(myPackage() + "IMessage__c");
			sobj.setField("Id", mapMsg.get("Id"));
			sobj.setField(myPackage() + "External_Id2__c", mapMsg.get(myPackage() + "External_Id2__c"));
			sobj.setField(myPackage() + "Status__c", mapMsg.get(myPackage() + "Status__c"));
			sobj.setField(myPackage() + "Comment2__c", "");
			lstSObj.add(sobj);
		}
		return lstSObj;
	}
	
	// getJsonArray
	private JsonArray getListMessage(Map<String, Object> mapObj) {
		JsonArray arrResult = new JsonArray();
		Map<String,Object> mapMr = (Map<String, Object>) mapObj.get("mapMr");
		List<Map<String, String>> lstMsg = (List<Map<String, String>>) mapMr.get("listMessage"); // listMessage
		for(Map<String,String> mapMsg : lstMsg) {
			JsonObject obj = new JsonObject();
			obj.addProperty("status", mapMsg.get(myPackage() + "Status__c"));
			obj.addProperty("comment", mapMsg.get(myPackage() + "Comment2__c"));
			arrResult.add(obj);
		}
		return arrResult;
	}
	
	private IBean[][] recordsToIBean(List<Map<String,Object>> lstRecords){
		// in case no data in the message list
		if (lstRecords==null || lstRecords.size()==0) {			
			return null;
		}
		IBean[][] records = new IBean[lstRecords.size()][];
		int rec=0,col=0;
		for(Map<String,Object> mapRec: lstRecords) {
			List<Map<String,Object>> lstRec = (List<Map<String, Object>>) mapRec.get("oneRecord");
			records[rec] = new IBean[lstRec.size()];
			for(Map<String,Object> oneRec : lstRec) {
				IBean bean = new IBean(String.valueOf(oneRec.get("name")),oneRec.get("value")==null?"":String.valueOf(oneRec.get("value")));
				records[rec][col] = bean;
				col++;
			}
			col=0;
			rec++;
		}
		
		return records;
	};
	
	/**
	 * doCallOut call from AgentServiceController
	 * @return
	 * @throws Exception
	 */
	public JsonArray doCallOut() throws Exception {
//		log.trace("GenericCallOutTask() >doCallOut() > start.");
//		
//		// login service
//		sfIntegrationService.login();
//		
//		// check intf mode
//		String processingMode = interfacee.getInterface_Mode__c();
//		// Synchronous
//		if(StringUtils.equalsIgnoreCase(processingMode, "Synchronous")) {
//			log.trace("GenericCallOutTask() >doCallOut() >doSynIntegrateSF2Agent() > start.");
//			return doSynIntegrateSF2Agent();
//		}
//		
//		log.trace("GenericCallOutTask() >doCallOut() >CalloutThread() > start.");
//		// Asynchronous
//		CalloutThread callout = new CalloutThread(records, adapter, lstSObjMessage, sfIntegrationService);
//		new Thread(callout, Thread.currentThread().getName()).start();
//		
//		if(lstMessage.size()>0) {
//			for(int i=0; i<lstMessage.size(); i++) {
//				lstMessage.get(i).getAsJsonObject().addProperty("status", "Completed");
//				lstMessage.get(i).getAsJsonObject().addProperty("comment", "Data is arrived Agent.");
//			}
//		}
//		
//		log.trace("GenericCallOutTask() >doCallOut() > end.");
//		return lstMessage;
		return null;
	}
	
	private JsonArray doSynIntegrateSF2Agent() throws Exception {
//		// check folder 
//		if(adapter instanceof FileMapAdapter || adapter instanceof FTPAdapter) {
//			if(org.apache.commons.lang3.StringUtils.isBlank(adapter.getAdapter().getPropertyValue(PropertyNameHelper.FOLDER))) {
//				throw new Exception("InBound processing with adapter name: "+adapter.getAdapter().getPropertyValue("adpName")+" specify the adapter property 'Folder'.");
//			}	
//		}
//
//		if(records!=null && records.length>0) {
//			
//			//List<IChained_Interfaces__c> Parent_IChained_Interfaces = sfIntegrationService.getIChained_Interfaces__c(interfacee.getId());
//			List<IChained_Interfaces__c> Parent_IChained_Interfaces = interfacee.getParent_IChained_Interfaces();
//			ResponseInterfaceHandler responseInterfaceHandler = null;
//			List<List<IBean>> lsResponseRecords = new ArrayList<List<IBean>>(); 
//			//has child interface
//			List<BatchExecutorResponseItem> response = null;
//			if(Parent_IChained_Interfaces!=null && Parent_IChained_Interfaces.size()>0){
//				response = adapter.updateChainData(sfIntegrationService, interfacee, records);
//			}else{
//				if(adapter instanceof com.iservice.adapter.XmlAdapter) {
//					response = adapter.update2(sfIntegrationService, interfacee, records);
//				}else {
//					response = adapter.update(records, interfacee.getOperationType__c(), sfIntegrationService, interfacee);	
//				}
//			}
//			
//			// add messages status to update on SF
//			if(lstMessage.size()>0) {
//				for(int i=0; i<response.size(); i++) {
//					BatchExecutorResponseItem res = response.get(i);
//					lstMessage.get(i).getAsJsonObject().addProperty("status", res.getStatus());
//					lstMessage.get(i).getAsJsonObject().addProperty("comment", res.getMessage());
//					if(res.getResponseRecords().size()>0 && interfacee.getResponseInterface__c()!=null) {
//						lsResponseRecords.addAll(res.getResponseRecords());
//					}
//				}
//			}
//			if(lsResponseRecords.size()>0 && interfacee.getResponseInterface__c()!=null) {
//				responseInterfaceHandler = ResponseInterfaceFactory(interfacee.getResponseInterface__c(), sfIntegrationService, lsResponseRecords);
//				if(isHandleResponseInterface(interfacee, responseInterfaceHandler)) responseInterfaceHandler.process();
//			}
//		}
//		log.trace("GenericCallOutTask() >doCallOut() >doSynIntegrateSF2Agent() > end.");
//		log.trace("GenericCallOutTask() >doCallOut() > end.");
//		return lstMessage;
			return null;
	}
	
	/**
	 * CalloutThread
	 * run asynchronous mode
	 * @author SOKDET
	 *
	 */
//	class CalloutThread implements Runnable{
//		
//		// check to stop thread
//		private volatile boolean isExit = false;
//		
//		IBean[][] records;
//		IDBConnection adapter;
//		List<SObject> lstSObjMessage;
//		SFIntegrationService sfIntegrationService;
//		
//		public CalloutThread (IBean[][] records,IDBConnection adapter,List<SObject> lstSObjMessage,SFIntegrationService sfIntegrationService) {
//			this.records = records;
//			this.adapter = adapter;
//			this.lstSObjMessage = lstSObjMessage;
//			this.sfIntegrationService = sfIntegrationService;
//		}
//		
//		@Override
//		public void run(){
//			while(!isExit) {
//				doAsynIntegrateSF2Agent();
//			}
//		}
//		
//		public void stop(){
//			isExit = true;
//			//System.out.println(isExit);
//	    }
//		
//		// doAsynIntegrateSF2Agent
//		public void doAsynIntegrateSF2Agent() {
//			MapIntegrationInfo mapIntgInfo = null;
//			try {
//				mapIntgInfo = new MapIntegrationInfo(sfIntegrationService, integration, interfacee, Calendar.getInstance());
//			} catch (Exception e) {
//				log.trace(e.getMessage());
//			}
//			ArrayList<ILogs__c> ilogs = new ArrayList<ILogs__c>();
//			try {
//				
//				// check folder 
//				if(adapter instanceof FileMapAdapter || adapter instanceof FTPAdapter) {
//					if(org.apache.commons.lang3.StringUtils.isBlank(adapter.getAdapter().getPropertyValue(PropertyNameHelper.FOLDER))) {
//						throw new Exception("InBound processing with adapter name: "+adapter.getAdapter().getPropertyValue("adpName")+" specify the adapter property 'Folder'.");
//					}	
//				}
//
//				//16-05-2017 update last data 
//				Map<Interfaces__c, String> mapInterfaceLastData = new HashMap<Interfaces__c, String>();
//				GenericSFTask.dtoLastData = null;
//				
//				try {
//					if(records!=null && records.length>0) {
//						
//						GenericSFTask.getDtoLastData().setTotal(records.length);
//						
//						//List<IChained_Interfaces__c> Parent_IChained_Interfaces = sfIntegrationService.getIChained_Interfaces__c(interfacee.getId());
//						List<IChained_Interfaces__c> Parent_IChained_Interfaces = interfacee.getParent_IChained_Interfaces();
//						
//						//has child interface
//						List<BatchExecutorResponseItem> response = null;
//						
//						if(Parent_IChained_Interfaces!=null && Parent_IChained_Interfaces.size()>0){
//							response = adapter.updateChainData(sfIntegrationService, interfacee, records);
//						}else{
//							if(adapter instanceof com.iservice.adapter.XmlAdapter) {
//								response = adapter.update2(sfIntegrationService, interfacee, records);
//							}else {
//								response = adapter.update(records, interfacee.getOperationType__c(), sfIntegrationService, interfacee);	
//							}
//						}
//						
//						// add messages status to update on SF
//						if(lstSObjMessage.size()>0) {
//							for(int i=0; i<response.size(); i++) {
//								BatchExecutorResponseItem res = response.get(i);
//								lstSObjMessage.get(i).setField(myPackage() + "Status__c", res.getStatus());
//								lstSObjMessage.get(i).setField(myPackage() + "Comment2__c", res.getMessage());
//							}
//							List<SObject> sobjToSave = new ArrayList<SObject>();
//							for(SObject sobj : lstSObjMessage){
//								sobjToSave.add(sobj);
//								if(sobjToSave.size()==1000){
//									updateMsgOnSF(sobjToSave, sfIntegrationService);
//									sobjToSave = new ArrayList<SObject>();
//								}
//							}
//							if(sobjToSave.size()>0){
//								//update message on SF
//								updateMsgOnSF(sobjToSave, sfIntegrationService);
//							}
//						}
//					
//						//prepare this current last data info
//						String status = (GenericSFTask.getDtoLastData().getTotal().intValue() == GenericSFTask.getDtoLastData().getProcessed().intValue() ? "Completed" : "In Progress");
//						String thisLastData = 	GenericSFTask.getDtoLastData().getSys() 
//												+ "," + GenericSFTask.getDtoLastData().getRunTime().getTime()
//												+ "," + GenericSFTask.getDtoLastData().getTotal()
//												+ "," + GenericSFTask.getDtoLastData().getProcessed() 
//												+ "," + GenericSFTask.getDtoLastData().getFailed() 
//												+ "," + GenericSFTask.getDtoLastData().getPending() 
//												+ "," + GenericSFTask.getDtoLastData().getCompleted() 
//												+ "," + status;
//						mapInterfaceLastData.put(interfacee, thisLastData);
//					}
//					
//				} finally {
//					new AgentRestService().upsertInterfaceLastDataOnTable(sfIntegrationService, mapInterfaceLastData);
//					mapInterfaceLastData.clear();
//				}
//			
//			}catch(Exception ex) {
//				ilogs.add(traceError(ex));
//			}
//			
//			try {
//				createLogs(ilogs, (SFService) sfIntegrationService.getISFService(), mapIntgInfo);
//			} catch (Exception ex) {
//				log.error(">doIntegrateSF2Agent> createLogs> Error: " + ex.getMessage());
//			}finally {
//				stop();
//			}
//			log.trace("GenericCallOutTask() >doCallOut() >CalloutThread() > end.");
//		}
//		
//		// updateMsg
//		private void updateMsgOnSF(List<SObject> sobjToSave, SFIntegrationService integrationService) {
//			Integer nb = sobjToSave.size();
//			DtoLastData dto = null;
//			try {
//				//16-05-2017 Last Data
//				GenericSFTask.getDtoLastData().setProcessed(GenericSFTask.getDtoLastData().getProcessed()+nb);
//				//updateMessages
//				dto = updateMessages(sobjToSave, integrationService.getISFService().getSFPartner());
//				//Last Data
//				if(dto!=null) {
//					GenericSFTask.getDtoLastData().setFailed(GenericSFTask.getDtoLastData().getFailed()+dto.getFailed());
//					GenericSFTask.getDtoLastData().setPending(GenericSFTask.getDtoLastData().getPending()+dto.getPending());
//					GenericSFTask.getDtoLastData().setCompleted(GenericSFTask.getDtoLastData().getCompleted()+dto.getCompleted());
//				}
//			}catch(Exception ex) {
//				// updateMessages exception Last Data
//				GenericSFTask.getDtoLastData().setPending(GenericSFTask.getDtoLastData().getPending()+nb);
//			}
//		}
//		
//		// updateMessages
//		private DtoLastData updateMessages(List<SObject> sobjToSave, PartnerConnection binding) throws Exception {
//			Integer failed = 0, pending = 0, completed = 0;
//			SObject[] sobjs = new SObject[sobjToSave.size()];
//			int i=0;
//			for(SObject sobj : sobjToSave){
//				sobjs[i]=sobj;
//				String status = String.valueOf(sobj.getField(myPackage() + "Status__c"));
//				if(status.equalsIgnoreCase("Failed")) {
//					failed++;
//				}
//				else if(status.equalsIgnoreCase("Completed")) {
//					completed++;
//				}
//				else {
//					pending++;
//				}
//				i++;
//			}
//			
//			// isNotPersist
//			if(!interfacee.isIsNotPersistMessage__c()) {
//				//External_Id2__c is unique
//				UpsertResult[] sr = binding.upsert("Id", sobjs);
//				//handle error here if we need
//				if(sr != null) {	
//					for(int k=0; k<sr.length; k++){
//						if(!sr[k].isSuccess() && sr[k].getErrors()!=null){
//							System.out.println(sr[k].getErrors().toString());
//						}
//					}
//				}
//			}
//			
//			return new DtoLastData(null, null, null, null, failed, pending, completed, null);
//		}
//		
//		// create logs in batch
//		private ILogs__c traceError(Throwable e1) {
//
//			ILogs__c ilog = null;
//			try {
//				
//				ilog = new ILogs__c(myPackage());
//				ilog.setIntegration__c(interfacee.getIntegration__c());
//				ilog.setSource__c("ISERVICE");
//				
//				if(e1 instanceof FileMapAdapter.WarningFileNotFoundException){
//					ilog.setError_Level__c("Warning");
//				}
//				else{
//					ilog.setError_Level__c("Error");
//				}
//
//				StringWriter sw = new StringWriter();
//				e1.printStackTrace(new PrintWriter(sw));
//				String stacktrace = sw.toString()+"";
//				
//				if(stacktrace!=null && stacktrace.startsWith("java.lang.Exception: ")) {
//					stacktrace = stacktrace.replaceFirst("java.lang.Exception: ", "");
//				}
//				
//				// handle case line break, bec log desc is Text Area(255)
//				String errMsg = stacktrace; // e1.getMessage();
//				errMsg = errMsg.replaceAll("\r", " ").replaceAll("\n", " ");
//				if (errMsg.length() > 255) {
//					ilog.setDescription__c(errMsg.substring(0, 255));
//				} else {
//					ilog.setDescription__c(errMsg);
//				}
//
//			} catch (Throwable e2) {
//				log.error(">traceError >Error: " + e2.getMessage());
//			}
//			return ilog;
//		}
//		
//		// create logs in batch
//		private void createLogs(List<ILogs__c> ilogs, SFService sfService, MapIntegrationInfo mapIntgInfo) throws Exception {
//
//			try {
//				
//				if (ilogs != null && ilogs.size() > 0) {
//					
//					log.trace(">createLogs >ilogs: " + ilogs);
//					List<JsonObject> lsLog = new ArrayList<>();
//					IBean[] filtererror = new IBean[5 * ilogs.size()];
//					Integer nb = 0;
//					IBean integrationBean = new IBean(ISFService.INTEGRATION_C, interfacee.getIntegration__c());
//					for (int i = 0; i < ilogs.size(); i++) {
//						IBean error = new IBean("LEVEL", ilogs.get(i).getError_Level__c());
//						IBean source = new IBean("SOURCE", ilogs.get(i).getSource__c());
//						IBean logBean = new IBean(ISFService.OPERATION_LOGS, ilogs.get(i).getDescription__c());
//						IBean sourceType = new IBean("DataSourceType", "AGENT");
//						filtererror[nb++] = integrationBean;
//						filtererror[nb++] = error;
//						filtererror[nb++] = logBean;
//						filtererror[nb++] = source;
//						filtererror[nb++] = sourceType;
//						JsonObject json = new JsonObject();
//						json.addProperty("ParentId", integrationBean.getValue());
//						json.addProperty("level", ilogs.get(i).getError_Level__c());
//						json.addProperty("source", ilogs.get(i).getSource__c());
//						json.addProperty("sourceType", "AGENT");
//						json.addProperty("description", ilogs.get(i).getDescription__c());
//						lsLog.add(json);
//					}
//					try {
//						Map<String, Object> criteria = new HashMap<String, Object>();
//						criteria.put("ParentId", integrationBean.getValue());
//						BaseDao.getDao(LogDao.class).deleteByCriteria(criteria);
//						BaseDao.getDao(LogDao.class).bulkUpsert(lsLog);
//					} catch (Exception e) {
//						log.error(e.getMessage());
//					}
//					sfService.search(filtererror, mapIntgInfo);
//					
//					//20110725 release from memory
//					filtererror = null;
//					
//					log.trace(">createLogs >Done");
//
//				}
//			} catch (Throwable e2) {
//				log.error(">createLogs >Error: " + e2.getMessage());
//			}
//		}
//	} // end CalloutThread
	
	// test main 
	public static void main(String[] args) throws InterruptedException {
//		String json = "{\"records\":[{\"mD\":null,\"oneRecord\":[{\"name\":\"MESSAGE\",\"value\":null},{\"name\":\"EXTERNAL_FIELD_NAME\",\"value\":\"\"},{\"name\":\"Account.SKYVVA__PARENTID\",\"value\":\"0011v00001zLpvtAAC\"},{\"name\":\"Account.Website\",\"value\":\"\"},{\"name\":\"Account.Type\",\"value\":\"\"},{\"name\":\"Account.Phone\",\"value\":\"070974386\"},{\"name\":\"Account.Name\",\"value\":\"Parent Account\"},{\"name\":\"Account.Id\",\"value\":\"0011v00001zLpvtAAC\"},{\"name\":\"Account.Fax\",\"value\":\"\"},{\"name\":\"Account.BillingCountry\",\"value\":\"Cambodia\"},{\"name\":\"Account.BillingCity\",\"value\":\"Phnom Penh\"},{\"name\":\"Contact.Fax\",\"value\":\"\"},{\"name\":\"Contact.Phone\",\"value\":\"070974386\"},{\"name\":\"Contact.MobilePhone\",\"value\":\"070974386\"},{\"name\":\"Contact.FirstName\",\"value\":\"rayok\"},{\"name\":\"Contact.LastName\",\"value\":\"ChaildTwo\"},{\"name\":\"Contact.Id\",\"value\":\"0031v00001ycgDzAAI\"}]},{\"mD\":null,\"oneRecord\":[{\"name\":\"MESSAGE\",\"value\":null},{\"name\":\"EXTERNAL_FIELD_NAME\",\"value\":\"\"},{\"name\":\"Account.SKYVVA__PARENTID\",\"value\":\"0011v00001zLpvtAAC\"},{\"name\":\"Account.Website\",\"value\":\"\"},{\"name\":\"Account.Type\",\"value\":\"\"},{\"name\":\"Account.Phone\",\"value\":\"070974386\"},{\"name\":\"Account.Name\",\"value\":\"Parent Account\"},{\"name\":\"Account.Id\",\"value\":\"0011v00001zLpvtAAC\"},{\"name\":\"Account.Fax\",\"value\":\"\"},{\"name\":\"Account.BillingCountry\",\"value\":\"Cambodia\"},{\"name\":\"Account.BillingCity\",\"value\":\"Phnom Penh\"},{\"name\":\"Contact.Fax\",\"value\":\"\"},{\"name\":\"Contact.Phone\",\"value\":\"070974386\"},{\"name\":\"Contact.MobilePhone\",\"value\":\"070974386\"},{\"name\":\"Contact.FirstName\",\"value\":\"rayok\"},{\"name\":\"Contact.LastName\",\"value\":\"ChaildOne\"},{\"name\":\"Contact.Id\",\"value\":\"0031v00001ycgDLAAY\"}]}],\"mapIntf\":{\"Initialization__c\":false,\"Asynchronize__c\":false,\"Query__c\":\"SELECT Id,Name,BillingCountry,BillingCity,Fax,Phone,Type,Website FROM Account Where Id='0011v00001zLpvtAAC'\",\"Include_Error_Sync__c\":false,\"Insert_Message__c\":false,\"Custom_Processing__c\":false,\"Include_Technical_Error_Sync__c\":false,\"AdapterId__c\":\"a010Y00000tB2klQAC\",\"Interface_Mode__c\":\"Asynchronous\",\"Name__c\":\"Csv OutBound\",\"Integration__c\":\"a070Y000004FiXTQA0\",\"PI__c\":false,\"Source_Name__c\":\"Account\",\"AdapterId__r\":{\"Type__c\":\"File\",\"attributes\":{\"type\":\"Adapter__c\",\"url\":\"\\/services\\/data\\/v45.0\\/sobjects\\/Adapter__c\\/a010Y00000tB2klQAC\"},\"Id\":\"a010Y00000tB2klQAC\"},\"Parent_IChained_Interfaces__r\":{\"totalSize\":1,\"records\":[{\"ParentInterfaceId__c\":\"a090Y00000Q0BzNQAV\",\"attributes\":{\"type\":\"IChained_Interfaces__c\",\"url\":\"\\/services\\/data\\/v45.0\\/sobjects\\/IChained_Interfaces__c\\/a0J1v00000Gua4qEAB\"},\"Id\":\"a0J1v00000Gua4qEAB\"}],\"done\":true},\"CreateStatisticalData__c\":true,\"Integration__r\":{\"attributes\":{\"type\":\"Integration__c\",\"url\":\"\\/services\\/data\\/v45.0\\/sobjects\\/Integration__c\\/a070Y000004FiXTQA0\"},\"Id\":\"a070Y000004FiXTQA0\",\"Name\":\"SOKDET_TEST\"},\"IsNotPersistMessage__c\":false,\"Type__c\":\"OutBound\",\"R90_File_Separator__c\":\",\",\"Number_of_Records_Per_Batch__c\":50,\"attributes\":{\"type\":\"Interfaces__c\",\"url\":\"\\/services\\/data\\/v45.0\\/sobjects\\/Interfaces__c\\/a090Y00000Q0BzNQAV\"},\"OperationType__c\":\"upsert\",\"Id\":\"a090Y00000Q0BzNQAV\",\"Status__c\":\"Deployed\"},\"mapAdp\":{\"mapProp\":{\"agent_instance\":\"sokdet@takeo.properties\",\"filetype\":\"Xml\",\"xml_type\":\"Attribute\",\"folder\":\"D:\\\\DATA\\\\CSVfiles\\\\Outbound\",\"merger_file\":\"false\",\"file_upload_mode\":\"CONTENT\",\"direction\":\"OutBound\"},\"adp\":{\"Name__c\":\"SokDet Csv OutBound Adapter\",\"Type__c\":\"File\",\"Property__r\":{\"totalSize\":7,\"records\":[{\"Name__c\":\"file_upload_mode\",\"ValueLong__c\":\"CONTENT\",\"Adapter__c\":\"a010Y00000tB2klQAC\",\"attributes\":{\"type\":\"Property__c\",\"url\":\"\\/services\\/data\\/v45.0\\/sobjects\\/Property__c\\/a0d1v00000IIY0DAAX\"},\"Id\":\"a0d1v00000IIY0DAAX\",\"Value2__c\":\"CONTENT\",\"Name\":\"I-00019477\"},{\"Name__c\":\"filetype\",\"ValueLong__c\":\"Xml\",\"Adapter__c\":\"a010Y00000tB2klQAC\",\"attributes\":{\"type\":\"Property__c\",\"url\":\"\\/services\\/data\\/v45.0\\/sobjects\\/Property__c\\/a0d1v00000IIY0EAAX\"},\"Id\":\"a0d1v00000IIY0EAAX\",\"Value2__c\":\"Xml\",\"Name\":\"I-00019478\"},{\"Name__c\":\"direction\",\"ValueLong__c\":\"OutBound\",\"Adapter__c\":\"a010Y00000tB2klQAC\",\"attributes\":{\"type\":\"Property__c\",\"url\":\"\\/services\\/data\\/v45.0\\/sobjects\\/Property__c\\/a0d1v00000IIY0FAAX\"},\"Id\":\"a0d1v00000IIY0FAAX\",\"Value2__c\":\"OutBound\",\"Name\":\"I-00019479\"},{\"Name__c\":\"folder\",\"ValueLong__c\":\"D:\\\\DATA\\\\CSVfiles\\\\Outbound\",\"Adapter__c\":\"a010Y00000tB2klQAC\",\"attributes\":{\"type\":\"Property__c\",\"url\":\"\\/services\\/data\\/v45.0\\/sobjects\\/Property__c\\/a0d1v00000IIY0GAAX\"},\"Id\":\"a0d1v00000IIY0GAAX\",\"Value2__c\":\"D:\\\\DATA\\\\CSVfiles\\\\Outbound\",\"Name\":\"I-00019480\"},{\"Name__c\":\"xml_type\",\"ValueLong__c\":\"Attribute\",\"Adapter__c\":\"a010Y00000tB2klQAC\",\"attributes\":{\"type\":\"Property__c\",\"url\":\"\\/services\\/data\\/v45.0\\/sobjects\\/Property__c\\/a0d1v00000IIY0HAAX\"},\"Id\":\"a0d1v00000IIY0HAAX\",\"Value2__c\":\"Attribute\",\"Name\":\"I-00019481\"},{\"Name__c\":\"merger_file\",\"ValueLong__c\":\"false\",\"Adapter__c\":\"a010Y00000tB2klQAC\",\"attributes\":{\"type\":\"Property__c\",\"url\":\"\\/services\\/data\\/v45.0\\/sobjects\\/Property__c\\/a0d1v00000IIY0IAAX\"},\"Id\":\"a0d1v00000IIY0IAAX\",\"Value2__c\":\"false\",\"Name\":\"I-00019482\"},{\"Name__c\":\"agent_instance\",\"ValueLong__c\":\"sokdet@takeo.properties\",\"Adapter__c\":\"a010Y00000tB2klQAC\",\"attributes\":{\"type\":\"Property__c\",\"url\":\"\\/services\\/data\\/v45.0\\/sobjects\\/Property__c\\/a0d1v00000IIY0JAAX\"},\"Id\":\"a0d1v00000IIY0JAAX\",\"Value2__c\":\"sokdet@takeo.properties\",\"Name\":\"I-00019483\"}],\"done\":true},\"attributes\":{\"type\":\"Adapter__c\",\"url\":\"\\/services\\/data\\/v45.0\\/sobjects\\/Adapter__c\\/a010Y00000tB2klQAC\"},\"Id\":\"a010Y00000tB2klQAC\",\"Name\":\"SokDet Csv OutBound Adapter\"}},\"mapMr\":{\"mappingError\":false,\"listMapRecord\":[{\"BillingCity\":\"Phnom Penh\",\"FirstName\":\"rayok\",\"Website\":null,\"SALESFORCEID\":\"0011v00001zLpvtAAC\",\"MobilePhone\":\"070974386\",\"Name\":\"Parent Account\",\"Type\":null,\"BillingCountry\":\"Cambodia\",\"Phone\":\"070974386\",\"SKYVVA__SALESFORCEID\":\"0011v00001zLpvtAAC\",\"LastName\":\"ChaildOne\",\"Id\":\"0031v00001ycgDLAAY\",\"Fax\":null}],\"listMessage\":[{\"Related_To__c\":\"0011v00001zLpvtAAC\",\"Integration__c\":\"a070Y000004FiXTQA0\",\"RecordLastModifiedDate__c\":\"2019-02-11T10:22:31.000+0000\",\"Is_Alerted__c\":false,\"Modification_Date__c\":\"2019-02-13T04:25:06.103+0000\",\"Comment2__c\":null,\"Comment__c\":null,\"Name\":\"IM#2019-02-13 11:25:06.0541\",\"Interface__c\":\"a090Y00000Q0BzNQAV\",\"Type__c\":\"Outbound\",\"attributes\":{\"type\":\"IMessage__c\"},\"Status__c\":\"Pending\",\"External_Id2__c\":\"a090Y00000Q0BzNQAV0011v00001zLpvtAAC\"}],\"mapObject\":{}}}\r\n" + 
//				"";
//		JSONParser parser = new JSONParser();
//		try {
//			Map<String,Object> mapObj = (Map<String,Object>)parser.parse(json);
//			//System.out.println(mapObj);
//			
//			// GenericCallOutTask
//			GenericCallOutTask callout = new GenericCallOutTask(mapObj);
//			//callout.doIntegrateSF2Agent();
//			
//			ExecutorService service = Executors.newFixedThreadPool(1);
//			
//		}catch(Exception e) {
//			System.err.println(e.getMessage());
//		}
	}
	

}
