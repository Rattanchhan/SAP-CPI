package com.sforce.soap;

import com.sforce.ws.ConnectorConfig;

public class SFConnectorConfig extends ConnectorConfig {

	protected String proxyHost;
	protected int proxyPort;
	protected String bulkV2EndPoint;
	
	@Override
	public void setProxy(String host, int port) {
		this.proxyHost = host;
		this.proxyPort=port;
		
		super.setProxy(host, port);
	}

	public String getProxyHost() {
		return proxyHost;
	}

	public void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}

	public int getProxyPort() {
		return proxyPort;
	}

	public void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
	}

	public String getBulkV2EndPoint() {
		return bulkV2EndPoint;
	}

	public void setBulkV2EndPoint(String bulkV2EndPoint) {
		this.bulkV2EndPoint = bulkV2EndPoint;
	}

}
