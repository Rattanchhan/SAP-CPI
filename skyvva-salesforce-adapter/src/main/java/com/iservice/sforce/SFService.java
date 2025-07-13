package com.iservice.sforce;

import java.io.ByteArrayOutputStream;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiLoggingDecorator;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.iservice.database.PropertySettingDao;
import com.iservice.gui.data.Integration__c;
import com.iservice.gui.data.Interfaces__c;
import com.iservice.gui.helper.PropertyNameHelper;
import com.iservice.model.IMessageTree;
import com.iservice.task.GenericSFTask;
import com.iservice.task.MapIntegrationInfo;
import com.sforce.soap.ProfessionalOrgIntegration;
import com.sforce.soap.SFConnectorConfig;
import com.sforce.soap.SalesForceHttpTransport;
import com.sforce.soap.partner.Connector;
import com.sforce.soap.partner.LoginResult;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.fault.ApiFault;
import com.sforce.soap.partner.fault.ExceptionCode;
import com.sforce.soap.schemas._class.IServices.IBean;
import com.sforce.soap.schemas._class.IServices.IIntegration;
import com.sforce.soap.schemas._class.IServices.IntegrationService;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;

public class SFService implements ISFService {

	public static final String API_VERSION = "53.0";
	protected SFConnectorConfig sfConnectorConfig;
	protected PartnerConnection partnerConnection;
	protected SFServiceCache cache;
	private MapIntegrationInfo mapIntgInfo;
	//http://wiki.developerforce.com/index.php/Security_Review_FAQ
	//API token when passed the security review
	public static final String CLIENT_ID = "Skyvva/IntegrationSuite/.";
	public static final Integer _BATCH_SIZE = 2000;

	protected String sessionId;
	protected MapSFConnInfo mapConInfo;
    private static final CpiLoggingDecorator LOGGER = CpiLoggingDecorator.getLogger(SFService.class);
	private int timeRetry = 0;
	private int maxNumberOfRetry;
	private int retryTimeInterval;
	//20111006 increaSe timeout to 15 minutes
	public static int TIMEOUT = 900000; //300000; //default 60000
	private static String URL_V4 = "services/apexrest/skyvvasolutions/V4/IntegrateWithIMessage";
//	private static String URL_V3_RawContent = "services/apexrest/skyvvasolutions/V3/integrate";
	private static final JSONParser PARSER = new JSONParser();

	public SFService(MapSFConnInfo mapProps, SFServiceCache cache) {
		if(mapProps==null){
			throw new RuntimeException("Connection map cannot null!");
		}
		this.mapConInfo = mapProps;
		this.cache = cache;
	}
	
	@Override
	public String getServiceUrl() {
		return this.mapConInfo.getServiceUrl();
	}
	
	@Override
	public String getIntegrationUrl() {
		return this.getEndPoint();
	}

	/*
	 * 06012010
    Q: I understand that reviewed solutions are able to work with PE orgs. How does this work? 
	A: All partner applications are assigned an API token (Client ID) once they pass the security review. This token is to be used for all API calls for that specific application. Subsequent to the security review, we allow API transactions identified with your API token to operate with Professional Edition orgs. This token does not support API access in Group Edition or Contact Manager Edition orgs.

	Upon passing or provisionally passing the security review, a detailed email will be sent to you giving instructions on how to add the code containing the Client ID to the SOAP Header. Here are some sample code snippets:

	// Use the following in your login method before you make
	// your login call to the service; Set the call option for client ID

    Java Example:
	String clientID = "your_clientid_is_case_sensitive"; 
	CallOptions co = new CallOptions();
	co.setClient(clientID);


	// bind it to the current soap session
	binding.setHeader("SforceService", "CallOptions", co); 
	 */
	@Override
	public boolean login() throws ConnectionException {
		partnerConnection = newPartnerConnection(mapConInfo);
		ConnectorConfig config = partnerConnection.getConfig();
		if (StringUtils.isBlank(config.getSessionId())) {
			return false;
		}
		this.sessionId = config.getSessionId();
		return true;
	}

	protected  PartnerConnection newPartnerConnection(MapSFConnInfo mapConInfo) throws ConnectionException {
		
		sfConnectorConfig = new SFConnectorConfig();
		String username = mapConInfo.getUsername();
		String password = mapConInfo.getPassword()+mapConInfo.getToken();
		String url = mapConInfo.getSFLoginUrl();
		sfConnectorConfig.setUsername(username);
		sfConnectorConfig.setPassword(password);
		sfConnectorConfig.setAuthEndpoint(url);
		sfConnectorConfig.setServiceEndpoint(sfConnectorConfig.getAuthEndpoint());
		
		if(mapConInfo.isUseProxy()){
			sfConnectorConfig.setProxy(mapConInfo.getProxyHost(),mapConInfo.getProxyPortAsInteger());
			sfConnectorConfig.setProxyUsername(mapConInfo.getProxyUsername());
			sfConnectorConfig.setProxyPassword(mapConInfo.getProxyPassword());
		}
		sfConnectorConfig.setTransport(SalesForceHttpTransport.class);
		sfConnectorConfig.setManualLogin(true);
		sfConnectorConfig.setConnectionTimeout(TIMEOUT);
		PartnerConnection partnerConnection = Connector.newConnection(sfConnectorConfig);
		partnerConnection.setCallOptions(CLIENT_ID, null);
		LoginResult result = partnerConnection.login(username, password);

		partnerConnection.setQueryOptions(_BATCH_SIZE);
		if (!result.isPasswordExpired()) {
			sfConnectorConfig.setSessionId(result.getSessionId());
			partnerConnection.setSessionHeader(result.getSessionId());
			sfConnectorConfig.setServiceEndpoint(result.getServerUrl());
			String soapEndpoint = result.getServerUrl();
			String apiVersion = getApiVersion(soapEndpoint);
			//bulk v1 endpoint
		    String restEndpoint = StringUtils.substringBefore(soapEndpoint, "Soap/") + "async/" +apiVersion ;
		    sfConnectorConfig.setRestEndpoint(restEndpoint);
		    //bulk v2 endpoint
		    String bulkV2EndPoint = StringUtils.substringBefore(soapEndpoint, "Soap/")+"data/v"+apiVersion+"/jobs/ingest";
		    sfConnectorConfig.setBulkV2EndPoint(bulkV2EndPoint);
			String endPoint=null;
			String svrURL = sfConnectorConfig.getServiceEndpoint();
		    if(GenericSFTask.DEMO32||StringUtils.isBlank(getPackage())){
		    	endPoint = svrURL.substring(0, svrURL.indexOf(".com/")) + ".com/services/Soap/class/IServices";
		    }else{
		    	endPoint = svrURL.substring(0, svrURL.indexOf(".com/")) + ".com/services/Soap/class/"+getPackage()+"/IServices";
		    }
		    mapConInfo.setEndPoint(endPoint);
		    mapConInfo.setServiceUrl(svrURL);
		}else{
			throw new ConnectionException("Password is expired.");
		}
		return partnerConnection;
	}
	
	public static String getApiVersion(String serviceUrl){
		try {
			// String url =
			// "https://na5.salesforce.com/services/Soap/u/28.0/00D70000000Jysr";
			serviceUrl = StringUtils.substringBeforeLast(serviceUrl, "/");
			String version = StringUtils.substringAfterLast(serviceUrl, "/");
			if (StringUtils.isNotBlank(serviceUrl)) {
				double v=Double.parseDouble(version);//make user it is valid version
				if(v<Double.parseDouble(API_VERSION)){
					return API_VERSION;
				}
				return version;
			}
		} catch (Exception e) {
			// nothing todo
		}
		// default version is 30
		return API_VERSION;
	}

	@Override
	public String getSessionId() throws ConnectionException {
		if (StringUtils.isBlank(sessionId)) {
			login();
		}
		return sessionId;
	}

	@Override
	public void integrate(IIntegration integration) throws Exception {
		timeRetry = 0;
		if(cache!=null) {
			maxNumberOfRetry = getMaxRetryNumber(cache.getIntegration(integration.getFromSystem()), cache.getInterfaceByName(integration.getTargetObject()));
			retryTimeInterval = getRetryTimeInterval(cache.getIntegration(integration.getFromSystem()), cache.getInterfaceByName(integration.getTargetObject()));
		}
		else {
			maxNumberOfRetry = MAX_NUMBER_OF_RETRY_DEFAULT;
			retryTimeInterval = RETRY_TIME_INTERVAL_DEFAULT;
		}
		integrateService(integration);
	}
	
	private void integrateService(IIntegration integration) throws Exception {
		//LOGGER.trace(">SFService.integrate> Time to Disconect!");
		//Thread.sleep(5000);
		try {
			new IntegrationService(this).integrate(integration);
		} catch (Exception ex) {
			if(ex instanceof ApiFault) ex = new Exception(((ApiFault) ex).getExceptionMessage(),ex);
			//In case Professional Edition, API disabled for this org
			if(ex.getMessage().contains(ExceptionCode.API_DISABLED_FOR_ORG.toString())){
				ProfessionalOrgIntegration apex = new ProfessionalOrgIntegration(this);
				try {
					apex.integrate(integration);
					}catch(Exception ex2) {
						if(ex2 instanceof ApiFault) ex2 = new Exception(((ApiFault) ex2).getExceptionMessage(),ex2);
						throw ex2;
					}
			}
			else if(isErrorConnection(ex)) {
				if(maxNumberOfRetry>0) {
					timeRetry++;
					maxNumberOfRetry--;
					LOGGER.trace(">SFService.integrate> ERROR> Problem with Connection...!");
					LOGGER.trace(">SFService.integrate> Retry Integration>("+timeRetry+ (timeRetry<=1?" time":" times")+") Retry Time Interval="+retryTimeInterval+"ms!");
					Thread.sleep(retryTimeInterval);
					integrateService(integration);
				}
				else
					throw ex;
			}
			else if(ex.getMessage().contains("System.LimitException")) {
				LOGGER.error(">SFService.integrate> ERROR> "+ex.getMessage().substring(0, ex.getMessage().indexOf("\n")));
				throw ex;
			}
			else {
				LOGGER.error("SFService.integrate> ERROR> "+ex.getMessage(), ex);
				throw ex;
			}
		}
	}
	
	@Override
	public IBean[][] search(IBean[] filter, MapIntegrationInfo mapIntgInfor) throws Exception {
		timeRetry = 0;
		if(mapIntgInfor!=null) {
			maxNumberOfRetry = getMaxRetryNumber(mapIntgInfor.getIntegration(), mapIntgInfor.getInterfaces());
			retryTimeInterval = getRetryTimeInterval(mapIntgInfor.getIntegration(), mapIntgInfor.getInterfaces());
		}
		else {
			maxNumberOfRetry = MAX_NUMBER_OF_RETRY_DEFAULT;
			retryTimeInterval = RETRY_TIME_INTERVAL_DEFAULT;
		}
		return searchService(filter);
	}
	
	@Override
	public IBean[][] search(IBean[] filter) throws Exception {
		timeRetry = 0;
		if(filter[0].getValue().equals(ISFService.OPERATION_SF_DATA) && cache!=null) {
			maxNumberOfRetry = getMaxRetryNumber(cache.getIntegration(filter[1].getValue()), cache.getInterface(filter[2].getValue()));
			retryTimeInterval = getRetryTimeInterval(cache.getIntegration(filter[1].getValue()), cache.getInterface(filter[2].getValue()));
		}
		else {
			maxNumberOfRetry = MAX_NUMBER_OF_RETRY_DEFAULT;
			retryTimeInterval = RETRY_TIME_INTERVAL_DEFAULT;
		}
		return searchService(filter);
	}
	
	private IBean[][] searchService(IBean[] filter) throws Exception{
		try {
			return new IntegrationService(this).search(filter);
		} catch (Exception ex) {
			if(ex instanceof ApiFault) ex = new Exception(((ApiFault) ex).getExceptionMessage(),ex);
			//LOGGER.error("SFService.search> ERROR> "+ex.getMessage(), ex);
			//In case Professional Edition, API disabled for this org
			if(ex.getMessage().contains(ExceptionCode.API_DISABLED_FOR_ORG.toString())){
				ProfessionalOrgIntegration apex = new ProfessionalOrgIntegration(this);
				try {
					return apex.search(filter);
				}catch(Exception ex2) {
					if(ex2 instanceof ApiFault) ex2 = new Exception(((ApiFault) ex2).getExceptionMessage(),ex2);
					throw ex2;
				}
			}else if(isErrorConnection(ex)) {
				if (maxNumberOfRetry>0) {
					timeRetry++;
					maxNumberOfRetry--;
					//System.out.println("Retry Integrate"+timeRetry);
					LOGGER.trace(">SFService.Search> ERROR> Problem with Connection...!");
					LOGGER.trace(">SFService.Search> Retry Integration> ("+timeRetry+ (timeRetry<=1?" time":" times")+") Retry Time Interval="+retryTimeInterval+"ms!");
					Thread.sleep(retryTimeInterval);
					return searchService(filter);
				}
				else
					throw ex;
			}
			else {
				throw ex;
			}
		}
	}
	
	public static boolean isErrorConnection(Exception ex) {
		if((ex instanceof UnknownHostException || 
				ex instanceof NoRouteToHostException || 
				ex instanceof HttpHostConnectException ||
				ex instanceof ConnectionException ||
				ex instanceof SocketException) && ex instanceof ApiFault == false) return true;
		return false;
	}
	
	public static boolean isErrorConnection(Throwable ex) {
		if((ex instanceof UnknownHostException || 
				ex instanceof NoRouteToHostException || 
				ex instanceof HttpHostConnectException ||
				ex instanceof ConnectionException ||
				ex instanceof SocketException)&& ex instanceof ApiFault == false) return true;
		return false;
	}
	
//	public void integrateV3Inbound(List<IMessageTree> msgTrees, int numOfTree, String mode) throws Exception{
//		String svrURL = mapIntgInfo.getSFIntegrationService().getEndPoint();
//		String url = svrURL.substring(0, svrURL.indexOf(".com/")) + ".com/" + URL_V4;
//		URL targetURL = new URL(url);
//		HttpPost method = createMethod(targetURL.getPath(), HttpPost.class);
//		HttpHost target = new HttpHost(targetURL.getHost(),targetURL.getPort(),targetURL.getProtocol());
//		JsonObject reqBody = new JsonObject();
//		JsonObject body = new JsonObject();
//		body.addProperty("mode", mode);
//		body.addProperty("integration", mapIntgInfo.getIntegration().getName());
//		body.addProperty("rootInterface", mapIntgInfo.getInterfaceName());
//		body.addProperty("responsePayloadFormat", "json");
//		body.addProperty("numberOfTree", numOfTree);
//		body.add("messages", new JsonParser().parse(new Gson().toJson(msgTrees)));
//		reqBody.add("request", body);
//		method.setEntity(new ByteArrayEntity(reqBody.toString().getBytes("UTF-8")));
//		doSend(method, target);
//	}
	
	@SuppressWarnings("deprecation")
	public void integrateV3Inbound(List<IMessageTree> msgTrees, int numOfTree, String mode) throws Exception{
		String svrURL = mapIntgInfo.getSFIntegrationService().getEndPoint();
		String url = svrURL.substring(0, svrURL.indexOf(".com/")) + ".com/" + URL_V4;
		
		ArrayList<NameValuePair> postParameters = new ArrayList<NameValuePair>();
		postParameters.add(new BasicNameValuePair("integration",mapIntgInfo.getIntegration().getName()));
		postParameters.add(new BasicNameValuePair("interface",mapIntgInfo.getInterfaceName()));
		postParameters.add(new BasicNameValuePair("mode",mode));
		postParameters.add(new BasicNameValuePair("response-format","JSON"));
		postParameters.add(new BasicNameValuePair("numberOfTree",String.valueOf(numOfTree))); 
		
		URI targetURI = new URIBuilder(url).addParameters(postParameters).build(); 		
		HttpPost method = createMethod(targetURI, HttpPost.class);
		HttpHost target = new HttpHost(targetURI.getHost(),targetURI.getPort(),targetURI.getScheme());
		
		JsonObject reqBody = new JsonObject();
		JsonObject body = new JsonObject();
		
		body.add("messages", new JsonParser().parse(new Gson().toJson(msgTrees)));
		reqBody.add("requestBody", body);
		method.setEntity(new ByteArrayEntity(reqBody.toString().getBytes("UTF-8")));
		doSend(method, target);
	}
	
	public void integrateV3Inbound_RawContent(String rawData) throws Exception{/*
		String svrURL = mapIntgInfo.getSFIntegrationService().getEndPoint();
		String url = svrURL.substring(0, svrURL.indexOf(".com/")) + ".com/" + URL_V3_RawContent;
		URL targetURL = new URL(url);
		HttpPost method = createMethod(targetURL.getPath(), HttpPost.class);
		HttpHost target = new HttpHost(targetURL.getHost(),targetURL.getPort(),targetURL.getProtocol());
		JsonObject reqBody = new JsonObject();
		JsonObject body = new JsonObject();
		body.addProperty("mode", "Asynchronous");
		body.addProperty("integration", mapIntgInfo.getIntegration().getName());
		body.addProperty("iinterface", mapIntgInfo.getInterfaceName());
		body.addProperty("transferId", new XmlToImgV3().generateMessageExtId());
		body.addProperty("runtimeParameter", "");
		body.addProperty("responsePayloadFormat", "json");
		body.addProperty("payloadFormat", getFileType(mapIntgInfo));
		body.addProperty("payload", rawData);
		reqBody.add("dtoRequestBody", body);
		method.setEntity(new ByteArrayEntity(reqBody.toString().getBytes("UTF-8")));
		doSend(method, target);*/
	}
	
	private String getFileType(MapIntegrationInfo mapIntgInfo) throws Exception {
		if(mapIntgInfo.getAdapterProperties().get(PropertyNameHelper.FILE_TYPE)!=null) return mapIntgInfo.getAdapterProperties().get(PropertyNameHelper.FILE_TYPE);
		else if(mapIntgInfo.getAdapterProperties().get(PropertyNameHelper.FTP_FILE_TYPE)!=null) return mapIntgInfo.getAdapterProperties().get(PropertyNameHelper.FTP_FILE_TYPE);
		else throw new Exception("No file type specified!");
	}
	
	protected void doSend(HttpRequestBase method, HttpHost target) throws Exception {
		CloseableHttpClient httpConn = null;
		try {
			httpConn = SalesForceHttpTransport.createHttpClient(method,mapIntgInfo.getSalesforceConfiguration());
			HttpResponse httpResponse = SalesForceHttpTransport.execute(httpConn,target, method);
			HttpEntity r_entity = httpResponse.getEntity();
			BufferedHttpEntity b_entity = new BufferedHttpEntity(r_entity);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			b_entity.writeTo(out);
			byte[] content = out.toByteArray();
			if (content.length > 0) {
				mapIntgInfo.setResponseObject(content);
				Object obj = PARSER.parse(new String(content, "UTF-8"));				
				if (obj instanceof JSONArray || httpResponse.getStatusLine().getStatusCode() >= 400) {
					// when error the response is json array 
					StringBuilder message = new StringBuilder();
					if(obj instanceof JSONArray) {
						JSONArray errs = (JSONArray) obj;
						boolean first = true;
						for(Object err:errs){
							JSONObject jerr = (JSONObject)err;
							if(!first){
								message.append("\n");
							}
							message.append(jerr.get("message"));
							first = false;
						}
					}else if(obj instanceof JSONObject) {
						JSONObject errs = (JSONObject) obj;
						message.append(errs.get("StatusMsg"));	
					}
					throw new Exception(message.toString());
				}else if(obj instanceof JSONObject) {
					JSONObject reponse = (JSONObject) obj;
					JSONObject globalStatus = (JSONObject) reponse.get("GlobalStatus");
					// if we got record error
					if(globalStatus!=null && Integer.parseInt(globalStatus.get("Failed").toString())>0) {
						throw new Exception("Records have not been integrated into Salesforce completely. Please check the Message Monitoring on SKYVVA App.");
					}
				}
			}
		}finally{
			if(httpConn!=null){
				httpConn.close();
			}
		}
	}
	
	protected <M extends HttpRequestBase> M createMethod(URI targetURL, Class<M> type) throws Exception {
		M method = type.getConstructor(String.class).newInstance(targetURL.getPath());
		method.setURI(targetURL);
		SFConnectorConfig sfConn = mapIntgInfo.getSalesforceConfiguration();
		//set cookie
		method.setHeader("Authorization",	"Bearer " + sfConn.getSessionId());
		method.setHeader("Content-Type", "application/json; charset=UTF-8");
		method.setHeader("Accept", "application/json");
		return method;
	}

	@Override
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getEndPoint() {
		return mapConInfo.getEndpoint();
	}

	public static void main(String[] arg) throws Exception {

		String skyvvaPackage = "skyvvasolutions////////////////";
		skyvvaPackage = MapSFConnInfo.clearDoubleSlashes(skyvvaPackage);
		System.out.println(skyvvaPackage);


		Map<String, String> mapProps =new HashMap<String, String>();
		mapProps.put("package", skyvvaPackage);

		//Production account
		//	   mapProps.put("username", "bernhard.vonberg@skyvva.com");
		//	   mapProps.put("password", "A9iA1lHO");
		//	   mapProps.put("token", "FEH6MEc5oTx5lT4hxq9wYmVZ");
		//	   mapProps.put("url", "https://www.salesforce.com/services/Soap/u/12.0");

		//Developer
		//	   mapProps.put("username", "heng.ngoun@skyvva.com");
		//	   mapProps.put("password", "Odbg092011");
		//	   mapProps.put("token", "619pS9MW3JLgKqlIg3sQdBrN9");
		//	   mapProps.put("url", "https://www.salesforce.com/services/Soap/u/12.0");

		//Professional Edition account
		mapProps.put("username", "xxx");
		mapProps.put("password", "xxx");
		mapProps.put("token", "xxx");
		mapProps.put("url", "https://login.salesforce.com/services/Soap/u/"+API_VERSION);
		mapProps.put("endpoint", "https://ap1-api.salesforce.com/services/Soap/class/skyvvasolutions/IServices");

		SFService sf = new SFService(new MapSFConnInfo(mapProps), null);
		try {
			String s = sf.getSessionId();
			System.out.println(s);
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		try {
			IBean[] filter = new IBean[2];
			IBean b = new IBean(ISFService.OPERATION_SETUP,ISFService.OPERATION_SETUP);
			filter[0]=b;
			b = new IBean(ISFService.INTEGRATION_C, "a0C90000001jfJU");
			filter[1]=b;
			IBean[][] bbs = sf.search(filter);
			System.out.println(bbs);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public PartnerConnection getSFPartner() throws Exception  {
		if(partnerConnection==null){
			partnerConnection = newPartnerConnection(mapConInfo);
		}
		return partnerConnection;
	}

	@Override
	public String getQueryPackage() {
		return PropertySettingDao.getInstance().getQueryPackage();
	}
	
	@Override
	public String getPackage() {
		return PropertySettingDao.getInstance().getPackage();
	}

	public SFConnectorConfig getSfConnectorConfig() {
		return sfConnectorConfig;
	}

	public void setSfConnectorConfig(SFConnectorConfig sfConnectorConfig) {
		this.sfConnectorConfig = sfConnectorConfig;
	}

	public PartnerConnection getPartnerConnection() {
		return partnerConnection;
	}

	public void setPartnerConnection(PartnerConnection partnerConnection) {
		this.partnerConnection = partnerConnection;
	}
	
	public int getMaxRetryNumber(Integration__c intg, Interfaces__c intf){
		try {
			if(intf!=null && intf.getMax_Number_Of_API_Retry__c()!=null) {
				return Integer.parseInt(intf.getMax_Number_Of_API_Retry__c());
			}
			else if(intg.getInterface_groups().size()>0 && intg.getInterface_groups().get(0).getMax_Number_Of_API_Retry__c()!=null){
				return Integer.parseInt(intg.getInterface_groups().get(0).getMax_Number_Of_API_Retry__c());
			}
			else if(intg!=null && intg.getMax_Number_Of_API_Retry__c()!=null){
				return Integer.parseInt(intg.getMax_Number_Of_API_Retry__c());
			}
			return MAX_NUMBER_OF_RETRY_DEFAULT;
		}catch(Exception ex){
			return MAX_NUMBER_OF_RETRY_DEFAULT;
		}
	}
	
	public int getRetryTimeInterval(Integration__c intg, Interfaces__c intf){
		try {
			if(intf!=null && intf.getRetry_Time_Interval__c()!=null) {
				return (RETRY_TIME_INTERVAL_DEFAULT * Integer.parseInt(intf.getRetry_Time_Interval__c()));
			}
			else if(intg.getInterface_groups().size()>0 && intg.getInterface_groups().get(0).getRetry_Time_Interval__c()!=null){
				return (RETRY_TIME_INTERVAL_DEFAULT * Integer.parseInt(intg.getInterface_groups().get(0).getRetry_Time_Interval__c()));
			}
			else if(intg!=null && intg.getRetry_Time_Interval__c()!=null){
				return (RETRY_TIME_INTERVAL_DEFAULT * Integer.parseInt(intg.getRetry_Time_Interval__c()));
			}
			return RETRY_TIME_INTERVAL_DEFAULT;
		}catch(Exception ex){
			return RETRY_TIME_INTERVAL_DEFAULT;
		}
	}
	
	public void setSFServiceCache(SFServiceCache cache) {
		this.cache = cache;
	}
	
	public MapIntegrationInfo getMapIntgInfo() {
		return mapIntgInfo;
	}

	public void setMapIntgInfo(MapIntegrationInfo mapIntgInfo) {
		this.mapIntgInfo = mapIntgInfo;
	}
}
