package com.iservice.sforce;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.iservice.gui.helper.ServerHelper;
import com.iservice.task.GenericSFTask;

public class MapSFConnInfo extends HashMap<String, String> {
	
	public static final String SERVICE_URL = "serviceUrl";
	public static final String KEY_P = "key";
	public static final String ENDPOINT_P="endpoint";

	public static final String USERNAME_P = "username";
	public static final String PASSWORD_P = "password";
	public static final String TOKEN_P = "token";
	public static final String SERVER_ENVIRONMENT = "serverEnvironment";
    public static final String LOGIN_URL = "loginUrl";
	
	public static final String PACKAGE_P = "package";
	public static final String PUSH_LOGS2SF = "pushLogs2SF";
	
	public static final String PROXY_USED = "proxyUse";	
	public static final String PROXY_PORT = "proxyPort";
	public static final String PROXY_HOST = "proxyHost";
	public static final String PROXY_TYPE = "proxyType";
	public static final String PROXY_USERNAME = "proxyUsername";
	public static final String PROXY_PASSWORD = "proxyPassword";
	
	public static final String AGENT_USERNAME = "agentUsername";
	public static final String AGENT_PASSWORD = "agentPassword";
	public static final String HOST_NAME = "hostName";
	public static final String PORT_FORWARD = "portForward";
	
	private static final long serialVersionUID = -2501427624218376340L;
	public static final String DEFAULT_PACKAGE = (GenericSFTask.DEMO32)?"/":"skyvvasolutions/";
	
	public MapSFConnInfo() {
		super();
	
	}
	public MapSFConnInfo(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
		
	}
	public MapSFConnInfo(int initialCapacity) {
		super(initialCapacity);
		
	}
	public MapSFConnInfo(Map<? extends String, ? extends String> m) {
		super(m);
		
	}
	public String getServerEnvironment() {
		return StringUtils.trimToEmpty(get(SERVER_ENVIRONMENT));
	}
	public String getUsername() {
		return StringUtils.trimToEmpty(get(USERNAME_P));
	}
	public String getPassword() {
		return StringUtils.trimToEmpty(get(PASSWORD_P));
	}
	public String getSkyvvaPackage() {
		return StringUtils.trimToEmpty(StringUtils.isBlank(get(PACKAGE_P))?DEFAULT_PACKAGE:get(PACKAGE_P));
	}
	public static String clearDoubleSlashes(String thePackage) {
	   if(StringUtils.isNotBlank(thePackage)) {
			if(!thePackage.contains("/")){
				thePackage += "/";
				return thePackage;
			}else{
				while (thePackage.contains("//")){
					thePackage = thePackage.replace("//", "/");
				}
				return thePackage;
			}
	   }
	   return "";
	}
	public String getToken(){
		return StringUtils.trimToEmpty(get(TOKEN_P));
	}
	public String getEndpoint(){
		return StringUtils.trimToEmpty(get(ENDPOINT_P));
	}
	public String getSFLoginUrl() {
        return StringUtils.trimToEmpty(get(LOGIN_URL));
	}
	public void setServerEnvironment(String evn) {
		put(SERVER_ENVIRONMENT,evn);
	}
	public void setUsername(String username) {
		put(USERNAME_P,username);
	}
	public void setPassword(String password) {
		put(PASSWORD_P,password);
	}
	public void setSkyvvaPackage(String skyvvaPackage) {
		put(PACKAGE_P,skyvvaPackage);
	}
	public void setKey(String key) {
		put(KEY_P,key);
	}
	public void setToken(String token) {
		put(TOKEN_P,token);
	}
    public void setLoginUrl(String loginUrl) {
        put(LOGIN_URL, loginUrl);
    }
	public void setEndPoint(String endpoint) {
		put(ENDPOINT_P,endpoint);
	}
	public String getPushLogs2SF() {
		return StringUtils.isBlank(get(PUSH_LOGS2SF))?"false":StringUtils.trimToEmpty(get(PUSH_LOGS2SF));
	}
	public void setPushLogs2SF(String pushLogs2SF) {
		put(PUSH_LOGS2SF,pushLogs2SF);
	}
	public String getProxyUse() {
		return StringUtils.isBlank(get(PUSH_LOGS2SF))?"false":StringUtils.trimToEmpty(get(PROXY_USED));
	}
	public void setProxyUse(String proxyUse) {
		put(PROXY_USED,proxyUse);
	}
	public String getProxyPort() {
		return StringUtils.trimToEmpty(get(PROXY_PORT));
	}
	public int getProxyPortAsInteger() {
		try{
			return Integer.parseInt( getProxyPort());
		}catch(NumberFormatException e){
			try{
				double d = Double.parseDouble(getProxyPort());
				return (int)d;
			}catch(NumberFormatException e2){
				//nothing todo
			}
		}
		
		return -1;
	}
	public void setServiceUrl(String url) {
		put(SERVICE_URL, url);
	}
	public String getServiceUrl() {
		return StringUtils.trimToEmpty(get(SERVICE_URL));
	}
	public void setProxyPort(String proxyPort) {
		put(PROXY_PORT,proxyPort);
	}
	public String getProxyHost() {
		return StringUtils.trimToEmpty(get(PROXY_HOST));
	}
	public void setProxyHost(String proxyHost) {
		put(PROXY_HOST,proxyHost);
	}
	public String getProxyType() {
		return StringUtils.trimToEmpty(get(PROXY_TYPE));
	}
	public void setProxyType(String proxyType) {
		put(PROXY_TYPE,proxyType);
	}
	public String getProxyUsername() {
		return StringUtils.trimToEmpty(get(PROXY_USERNAME));
	}
	public void setProxyUsername(String proxyUsername) {
		put(PROXY_USERNAME,proxyUsername);
	}
	public String getProxyPassword() {
		return StringUtils.trimToEmpty(get(PROXY_PASSWORD));
	}
	public void setProxyPassword(String proxyPassword) {
		put(PROXY_PASSWORD,proxyPassword);
	}
	public boolean isUseProxy(){
		if(StringUtils.isNotBlank(getProxyUse())){
			return Boolean.parseBoolean(StringUtils.lowerCase(getProxyUse()));
		}
		return false;
	}
	public String getAgentUsername() {
		return StringUtils.isBlank(get(AGENT_USERNAME))?"admin":StringUtils.trimToEmpty(get(AGENT_USERNAME));
	}
	public void setAgentUsername(String agentusername) {
		put(AGENT_USERNAME,agentusername);
	}
	public String getAgentPassword() {
		return StringUtils.isBlank(get(AGENT_PASSWORD))?"12345":StringUtils.trimToEmpty(get(AGENT_PASSWORD));
	}
	public void setAgentPassword(String agentpassword) {
		put(AGENT_PASSWORD,agentpassword);
	}
	public String getHostName() {
		return StringUtils.isBlank(get(HOST_NAME))?"http://":StringUtils.trimToEmpty(get(HOST_NAME));
	}
	public void setHostName(String hostname) {
		put(HOST_NAME,hostname);
	}
	public String getPortForward() {
		return StringUtils.isBlank(get(PORT_FORWARD))?"9091":StringUtils.trimToEmpty(get(PORT_FORWARD));
	}
	public void setPortForward(String protforward) {
		put(PORT_FORWARD,protforward);
	}
	
}
