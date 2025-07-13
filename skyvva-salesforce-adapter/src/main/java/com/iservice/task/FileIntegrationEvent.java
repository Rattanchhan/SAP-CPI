package com.iservice.task;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiLoggingDecorator;
import org.apache.commons.lang3.StringUtils;

import com.iservice.gui.data.IMapping__c;
import com.iservice.gui.data.Interfaces__c;
import com.iservice.task.GenericSFTask.DtoLastData;
import com.sforce.soap.schemas._class.IServices.IBean;

public class FileIntegrationEvent extends NormalIntegrationEvent {
	protected boolean isUseAttachFile = true;
    private static final CpiLoggingDecorator LOGGER = CpiLoggingDecorator.getLogger(FileIntegrationEvent.class);
	
	protected DtoLastData dtoLastData ;
	public FileIntegrationEvent(long totalRecord, MapIntegrationInfo intInfo) {
		super(totalRecord, intInfo);
		dtoLastData = new DtoLastData(GenericSFTask.SF_AGENT, new Date(), 0, 0, 0, 0, 0, "In Progress");
		dtoLastData.setTotal((int)totalRecord);
	}

	public boolean processRecord(List<IBean> record)throws Exception {
		if(isForceStop()) return false;
		if (isUseAttachFile) {
			try {
				attachFile(record);
				return true;
			} catch (FileNotFoundException e) {
				LOGGER.warn(e.getMessage(), e);
				// use old version
				isUseAttachFile = false;
				return super.processRecord(record);
			}
		} else {
			return super.processRecord(record);
		}

	}
	
	@Override
	public boolean doIntegration(boolean checkPackage)throws Exception{
		if(isUseAttachFile && !checkPackage){
			//at the end we need to sent report to sf when attach file no error
			Map<Interfaces__c, String> mapInterfaceLastData = new HashMap<Interfaces__c, String>();
			//prepare this current last data info
			String statusLastData = (dtoLastData.getTotal().intValue() == dtoLastData.getProcessed().intValue() ? "Completed" : "In Progress");
			String thisLastData = 	/*getDtoLastData().getSys()*/GenericSFTask.AGENT_SF 
									+ "," + dtoLastData.getRunTime().getTime()
									+ "," + dtoLastData.getTotal()
									+ "," + dtoLastData.getProcessed() 
									+ "," + dtoLastData.getFailed() 
									+ "," + dtoLastData.getPending() 
									+ "," + dtoLastData.getCompleted() 
									+ "," + statusLastData;
			mapInterfaceLastData.put(mapIntegrationInfo.getInterfaces(), thisLastData);
			new AgentRestService().upsertInterfaceLastDataOnTable(mapIntegrationInfo.getSFIntegrationService(), mapInterfaceLastData);
			return true;
		}else{
			return super.doIntegration(checkPackage);
		}
	}
	



	/**
	 * #3089
	 * 
	 * @return SKYVVA integration agent limit: Integration Files size limit in
	 *         agent <<< Salesforce attachment limit
	 * @param intService
	 * @param integration
	 * @param intf
	 * @param attachment
	 * @param metadata
	 *            : this is for other data read from attachment/contversion csv
	 *            information file
	 * @param attachmentOrContentVersion
	 *            : this param is a flag for upload contentversion or
	 *            attachment, (null mean attachment, "contentversion" mean
	 *            upload contentversion)
	 * @throws Exception
	 */
	protected  void attachFile( List<IBean> attachment) throws Exception {
		String parentId = "";

		List<IMapping__c> iMapping = getMapping();
		if (iMapping == null) {
			iMapping = new ArrayList<>();			
		}

		Map<String, String> data = new HashMap<>();
		// fileName, fileType
		for (IMapping__c imapping : iMapping) {
			if (StringUtils.equals(imapping.getTarget__c(),"ParentId")) {
				parentId = imapping.getSource_Long__c();
				break;
			}
		}

		doAttachFile(attachment, data, iMapping, parentId,null);
	
	}
	
	protected void doAttachFile(List<IBean> attachment,Map<String,String> data,List<IMapping__c> iMapping,String parentId,String fileName)throws Exception{
//		if (parentId.equals("")) {
//			throw new Exception("Parent Id is empty.");
//		}
//		//sent info back to caller
//		preProcess++;
//		onInProgressProcess(preProcess);
//		
//		// just create message Failed if attachment is empty
//		if(attachment.isEmpty()) {
//			
//			String message = "", status = "", id = "";
//			
//			status = "Failed";
//			message = "Cannot find attachment name: "+ fileName +" , in binary folder.";
//					
//			// create message
//			GenericSFTask.createMsgToSF(mapIntegrationInfo.getSFIntegrationService(), mapIntegrationInfo.getIntegration(), mapIntegrationInfo.getInterfaces(), status, id, message, fileName);
//			prepareAttachFileLastData(status);
//			
//		}else {
//			byte[] body = null;
//			String   fileType = "";
//			for (IBean ibean : attachment) {
//				if (ibean.getName().equals(FileMapAdapter.ATTACHMENT_FILE_NAME)) {
//					if(org.apache.commons.lang.StringUtils.isBlank(fileName)) {
//						fileName = ibean.getValue();
//					}
//				} else if (ibean.getName().equals(
//						FileMapAdapter.ATTACHMENT_FILE_TYPE)) {
//					fileType = ibean.getValue();
//				} else if (ibean.getName().equals(
//						FileMapAdapter.ATTACHMENT_FILE_BODY)) {
//					body = com.Ostermiller.util.Base64.decodeToBytes(ibean
//							.getValue());
//				}
//			}
//			if (fileName == null || fileName.equals("")) {
//				throw new Exception("File Name is empty.");
//			} else if (fileType == null || fileType.equals("")) {
//				throw new Exception("File FileType is empty.");
//			} else if (fileType == null || body == null) {
//				throw new Exception("File body is null.");
//			} else {
//				String endPoints[] = mapIntegrationInfo.getSFIntegrationService().getEndPoint().split("/");
//				// https://ap2.salesforce.com/services/apexrest/AttachmentFile/0012800000toDe7
//				String strPackage = StringUtils.isEmpty(mapIntegrationInfo.getSFIntegrationService()
//						.getConnectionInfo().get("package").toString()) ? ""
//						: mapIntegrationInfo.getSFIntegrationService().getConnectionInfo().get("package").toString();
//				if (strPackage.endsWith("/"))
//					strPackage = strPackage.substring(0, strPackage.length() - 1);
//				String url = GenericSFTask.DEMO32 ? (endPoints[0] + "//"
//						+ endPoints[2] + "/" + endPoints[3] + "/apexrest/AttachmentRestService/")
//						: (endPoints[0] + "//" + endPoints[2] + "/" + endPoints[3]
//								+ "/apexrest/" + strPackage + "/AttachmentRestService");
//
//				
//				HttpPost httppost = null;
//			
//				URL targetURL = new URL(url);
//				HttpHost target = new HttpHost(targetURL.getHost(),targetURL.getPort(), targetURL.getProtocol());
//				httppost = new HttpPost(targetURL.getPath());
//				PartnerConnection binding = mapIntegrationInfo.getSFIntegrationService().getBinding();
//				httppost.setHeader("Authorization","Bearer " + mapIntegrationInfo.getSFIntegrationService().getSessionId());
//				httppost.setHeader("Content-Type", fileType);
//				CloseableHttpClient httpcon = SalesForceHttpTransport.createHttpClient(httppost,(SFConnectorConfig) binding.getConfig());
//				JSONObject jsonObject = new JSONObject();
//				jsonObject.put("FileName", fileName);
//				jsonObject.put("ContentType", fileType);
//				jsonObject.put("ParentId", parentId);
//				jsonObject.put("IntegrationId", mapIntegrationInfo.getIntegrationId());
//
//			
//				// other data
//				for (Map.Entry<String, String> d : data.entrySet()) {
//					jsonObject.put(d.getKey(), d.getValue());
//				}
//				
//				httppost.setHeader("metadata", jsonObject.toString());
//				
//				httppost.setEntity(new ByteArrayEntity(body));
//
//				HttpResponse httpresponse = httpcon.execute(target,httppost);
//				HttpEntity r_entity = httpresponse.getEntity();
//				BufferedHttpEntity b_entity = new BufferedHttpEntity(r_entity);	
//
//				String message = "", status = "", id = "", content = "";
//				
//				try {
//					
//					content = IOUtils.toString(b_entity.getContent(), "UTF-8").replaceAll("\\\\", "");
//					content = content.substring(1, content.length() - 1);
//
//					if (StringUtils.isEmpty(content)) {
//						throw new Exception("Content response from " + url + "is Empty!");
//					}
//					// Skyvva 2.37
//					if (content.equalsIgnoreCase("modification")) {
//						status = "Modification";
//					} else if (content.equalsIgnoreCase("creation")) {
//						status = "Creation";
//					}
//					// Skyvva 2.38
//					else {
//						JSONObject result = (JSONObject) new JSONParser().parse(content);
//
//						status = result.get("status").toString();
//						id = result.get("id").toString();
//					}
//
//					if (status.equalsIgnoreCase("Creation")) {
//						status = "Completed";
//						message = "Creation of "
//								+ iMapping.get(0).getTarget_Object__c();
//					} else if (status.equalsIgnoreCase("Modification")) {
//						status = "Completed";
//						message = "Modification of "
//								+ iMapping.get(0).getTarget_Object__c();
//					} else {
//						status = "Failed";
//						message = "Failed " + content;
//						
//					}
//
//				} catch (Exception exception) {
//					status = "Failed";
//					message = "Failed " + "\n" + exception.getLocalizedMessage() + "\n" + content;
//					
//				} finally {
//					httpcon.close();
//				}
//				
//				if(status.equals("Failed")) getAdapter().getKeepFiles().add(fileName);
//				
//				// create message
//				GenericSFTask.createMsgToSF(mapIntegrationInfo.getSFIntegrationService(), mapIntegrationInfo.getIntegration(), mapIntegrationInfo.getInterfaces(), status, id, message, fileName);
//				prepareAttachFileLastData(status);
//			}
//		}
//		
//		LOGGER.trace(">GenericSFTask>Finish upload [" + fileName + "]");
	}
	
	protected  void prepareAttachFileLastData( String status){
		//prepare LastData
		Integer failed = 0, pending = 0, completed = 0;
		if(status.equals("Completed")) completed++;
		else if(status.equals("Failed")) failed++;
		else pending++;
		
		dtoLastData.setProcessed(dtoLastData.getProcessed()+1);
		dtoLastData.setFailed(dtoLastData.getFailed()+failed);
		dtoLastData.setPending(dtoLastData.getPending()+pending);
		dtoLastData.setCompleted(dtoLastData.getCompleted()+completed);
	}

	

}
