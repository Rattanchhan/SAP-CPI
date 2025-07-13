package com.iservice.task;

import java.util.Map;

import com.iservice.gui.data.Interfaces__c;
import com.iservice.sforce.MapSFConnInfo;
import com.iservice.sforce.SFIntegrationService;

public class AgentRestService {

	/**
	 * Check current user license key
	 * Call Skyvva Salesforce Webservice method from class RESTIntegrationSearchService
	 * @param intService
	 * @throws Exception if invalid license key
	 */ 
	public void checkLicenseKey(SFIntegrationService intService) throws Exception {
		
//		String strPackage = StringUtils.isEmpty(intService.getConnectionInfo().get("package").toString()) ? "" 
//				: intService.getConnectionInfo().get("package").toString();
//		if(strPackage.endsWith("/"))strPackage = strPackage.substring(0, strPackage.length()-1);
//
//		String endPoints[] = intService.getEndPoint().split("/");
//		
//		String url = GenericSFTask.DEMO32
//				? (endPoints[0] + "//" + endPoints[2] + "/" + endPoints[3] + "/apexrest/SearchService/")
//				: (endPoints[0] + "//" + endPoints[2] + "/" + endPoints[3] + "/apexrest/" + strPackage + "/SearchService");
//				
//		CloseableHttpClient httpcon = HttpClients.createDefault();
//		HttpPost httppost = null;
//	
//		URL targetURL = new URL(url);
//		HttpHost target = new HttpHost(targetURL.getHost(),targetURL.getPort(), targetURL.getProtocol());
//		httppost = new HttpPost(targetURL.getPath());
//		httppost.setHeader("Authorization", "Bearer "+intService.getSessionId());
//		httppost.setHeader("Content-Type", "application/json");
//		
//		String body = "{\"AGENT_CHECK_LICENSE\":\"true\"}";
//		httppost.setEntity(new StringEntity(body));
//		
//		try {
//			
//			HttpResponse httpresponse = httpcon.execute(target,httppost);
//			HttpEntity r_entity = httpresponse.getEntity();
//			BufferedHttpEntity b_entity = new BufferedHttpEntity(r_entity);	
//			
//			String content = IOUtils.toString(b_entity.getContent(), "UTF-8").replaceAll("\\\\", "");
//			Object obj =  new JSONParser().parse(content);
//			if(obj instanceof JSONObject) {
//				JSONObject json = (JSONObject)obj;
//				//result.put("message", json.getString("message"));
//				//result.put("status", json.getString("status"));
//				if(json.containsKey("status") && json.get("status").toString().equalsIgnoreCase("failed")) {
//					throw new Exception(json.get("message").toString());
//				}
//			}
//			
//		}catch(Exception exception) {
//			// in case salesforce version below 2.39
//			if(!(exception instanceof FileNotFoundException || exception instanceof IOException))throw exception;
//		} finally {
//			httpcon.close();
//		}
	}
	
	/**
	 * update interface last data with table
	 * Agent1.47 and Skyvva 2.39
	 */
	public void upsertInterfaceLastDataOnTable(SFIntegrationService intService, Map<Interfaces__c, String> mapInterfaceLastData) throws Exception {
		
//		for(Interfaces__c intf : mapInterfaceLastData.keySet()) {
//			// new version 2.39
//			if(intf.isSalesForceField("CreateStatisticalData__c")) {
//				if(intf.getCreateStatisticalData__c()) {
//					
//					String lastData = mapInterfaceLastData.get(intf);
//
//					String strPackage = StringUtils.isEmpty(intService.getConnectionInfo().get("package").toString()) ? "" 
//							: intService.getConnectionInfo().get("package").toString();
//					if(strPackage.endsWith("/"))strPackage = strPackage.substring(0, strPackage.length()-1);
//
//					String endPoints[] = intService.getEndPoint().split("/");
//					
//					String url = GenericSFTask.DEMO32
//							? (endPoints[0] + "//" + endPoints[2] + "/" + endPoints[3] + "/apexrest/AgentRestService/")
//							: (endPoints[0] + "//" + endPoints[2] + "/" + endPoints[3] + "/apexrest/" + strPackage + "/AgentRestService");
//							
//					CloseableHttpClient httpcon = HttpClients.createDefault();
//					HttpPost httppost = null;
//				
//					URL targetURL = new URL(url);
//					HttpHost target = new HttpHost(targetURL.getHost(),targetURL.getPort(), targetURL.getProtocol());
//					httppost = new HttpPost(targetURL.getPath());
//					httppost.setHeader("Authorization", "Bearer "+intService.getSessionId());
//					httppost.setHeader("Content-Type", "application/json");
//					
//					String body = "{\"UPSERT_INTERFACE_LAST_DATA\":\"UPSERT_INTERFACE_LAST_DATA\",\"INTERFACEID\":\"" + intf.getId() + "\",\"LAST_DATA\":\"" + lastData + "\"}";
//					httppost.setEntity(new StringEntity(body));
//					
//					try {
//						
//						HttpResponse httpresponse = httpcon.execute(target,httppost);
//						HttpEntity r_entity = httpresponse.getEntity();
//						BufferedHttpEntity b_entity = new BufferedHttpEntity(r_entity);	
//						String content = IOUtils.toString(b_entity.getContent(), "UTF-8").replaceAll("\\\\", "");
//						Object obj =  new JSONParser().parse(content);
//						if(obj instanceof JSONObject) {
//							JSONObject json = (JSONObject)obj;
//							if(json.containsKey("status") && json.get("status").toString().equalsIgnoreCase("failed")) {
//								throw new Exception(json.get("message").toString());
//							}
//						}else if (obj instanceof JSONArray){
//							JSONArray jsonArr = (JSONArray) obj;
//							JSONObject json = (JSONObject)jsonArr.get(0);
//							if(json.get("errorCode").equals("NOT_FOUND") && json.get("message").toString().contains("Could not find a match for URL")) {
//								GenericSFTask.upsertInterfacesLastData(intService, mapInterfaceLastData);
//								return;
//							}else {
//								throw new Exception(json.get("message").toString());
//							}
//						}
//			
//					}catch(Exception exception) {
//						// in case salesforce version below 2.39
//						if(!(exception instanceof FileNotFoundException || exception instanceof IOException)) {
//							throw exception;
//						}
//					} finally {
//						httpcon.close();
//					}
//				}else {
//					// TODO No need to update Last Data on Table
//				}
//			}
//			// old version under 2.39
//			else {
//				GenericSFTask.upsertInterfacesLastData(intService, mapInterfaceLastData);
//			}
//		}
		
	}
	
	public static void main(String[] args) {
		GenericSFTask.DEMO32 = false;
		MapSFConnInfo mapProps = new MapSFConnInfo();
		String username = "childa@test.com";
		String password = "test12345";
		String token = "UffC1LkDP2yh7afwnlk9kDh6";
		String urlserver = "https://login.salesforce.com/services/Soap/u/19.0";
		String _package = "skyvvasolutions/";
		String endpoint = "https://eu11.salesforce.com/services/Soap/class/skyvvasolutions/IServices";
		mapProps.put("username", username);
		mapProps.put("password", password);
		mapProps.put("token", token);
		mapProps.put("urlserver", urlserver);
		mapProps.put("package", _package);
		mapProps.put("endpoint", endpoint);
		
		
		SFIntegrationService service = new SFIntegrationService(mapProps);
		
		try {
			if(service.login()){
				new AgentRestService().checkLicenseKey(service);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}
