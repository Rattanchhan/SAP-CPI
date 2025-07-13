package com.iservice.gui.data;



import com.iservice.sforce.ISFService;
import com.sforce.ws.ConnectionException;

public class LoginCache {
	
	private ISFService sfConnection;
	
	public LoginCache(ISFService sfConnection) {
		super();
		this.sfConnection = sfConnection;
	}

	public String getSessionId() {
		try {
			return sfConnection.getSessionId();
		} catch (ConnectionException e) {
			throw new RuntimeException("Cannot login to salesforce, ERR:"+e.getMessage(),e);
		}
	}
	
	public String getServiceURL() {
		return sfConnection.getServiceUrl();
	}
	
	public String getIntegrationUrl() {
		return sfConnection.getIntegrationUrl();
	}

	public ISFService getSfConnection() {
		return sfConnection;
	}

}
