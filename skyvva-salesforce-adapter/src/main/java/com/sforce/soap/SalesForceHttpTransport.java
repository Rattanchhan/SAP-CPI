package com.sforce.soap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Proxy;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.sforce.ws.ConnectorConfig;
import com.sforce.ws.transport.Transport;

public class SalesForceHttpTransport implements Transport {
	public static final int MAX_RETRY = 10;
	protected Map<String, String> httpHeaders;
	private boolean successful;
	private SFConnectorConfig config;
	private URL url;
	protected ByteArrayOutputStream content;
	
	public SalesForceHttpTransport() {
	}

	public SalesForceHttpTransport(ConnectorConfig config) {
		setConfig(config);
	}

	@Override
	public void setConfig(ConnectorConfig config) {
		this.config = (SFConnectorConfig)config;
	}

	@Override
	public OutputStream connect(String uri, HashMap<String, String> httpHeaders) throws IOException {
		return connectRaw(uri, httpHeaders);
	}

	@Override
	public OutputStream connect(String uri, HashMap<String, String> httpHeaders, boolean enableCompression)
			throws IOException {
		return connectRaw(uri, httpHeaders);
	}

	@Override
	public OutputStream connect(String uri, String soapAction) throws IOException {
		if (soapAction == null) {
			soapAction = "";
		}

		HashMap<String, String> header = new HashMap<String, String>();

		header.put("SOAPAction", "\"" + soapAction + "\"");
		header.put("Content-Type", "text/xml; charset=UTF-8");
		header.put("Accept", "text/xml");

		return connectRaw(uri, header);
	}

	private OutputStream connectRaw(String uri, HashMap<String, String> httpHeaders)
			throws IOException {
		url = new URL(uri);
		this.httpHeaders = httpHeaders;

		content = new ByteArrayOutputStream();

		return content;
	}

	public static CloseableHttpClient createHttpClient(HttpRequestBase method,SFConnectorConfig config){
		CloseableHttpClient httpClient = null;
		if(config.getProxy()!=Proxy.NO_PROXY){
			CredentialsProvider credsProvider = new BasicCredentialsProvider();
			AuthScope auth = new AuthScope(config.getProxyHost(),config.getProxyPort());
			UsernamePasswordCredentials useCredential = new UsernamePasswordCredentials("","");
			if(StringUtils.isNotBlank(config.getProxyUsername())){
				useCredential=new UsernamePasswordCredentials(config.getProxyUsername(), config.getProxyPassword());
			}
			credsProvider.setCredentials(auth,useCredential);

			httpClient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();
			HttpHost proxy = new HttpHost(config.getProxyHost(),config.getProxyPort());
			RequestConfig httpConfig = RequestConfig.custom().setProxy(proxy).build();
			method.setConfig(httpConfig);
		}else{
			httpClient = HttpClients.createDefault();
		}
		return httpClient;
	}
	
	public static HttpResponse execute(HttpClient client,HttpHost target,HttpRequestBase httpMethod) throws IOException{
		int retry =0;
		while(true){
			try{
				HttpResponse httpresponse = client.execute(target, httpMethod);
	
				return  httpresponse;
				
			}catch(IOException ex){
				if (ex.getMessage().toLowerCase().contains("read timed out")
						|| ex.getMessage().toLowerCase().contains("your request was running for too long, and has been stopped.")
						//30122010
						|| ex.getMessage().toLowerCase().contains("connection was cancelled here")){
					//when has a connection time out we need to retry to send the request
					retry ++;
					if(retry<=MAX_RETRY){
						try {
							Thread.sleep(retry*2000);
						} catch (InterruptedException e) {
							//nothing to to
						}
					}else{
						throw ex;
					}
				}else{
					throw ex;
				}
			}
		}
	}
	
	@Override
	public InputStream getContent() throws IOException {
		CloseableHttpClient httpClient = null;
		try{
			HttpHost target = new HttpHost(url.getHost(),
					url.getPort(), url.getProtocol());
			HttpPost httppost = new HttpPost(url.getPath());
			httpClient = createHttpClient(httppost, config);
			
			if (this.httpHeaders!=null) {

				for (String key_head : this.httpHeaders.keySet()) {
					httppost.setHeader(key_head, httpHeaders.get(key_head));
				}

			}
			
			HttpEntity entity =	EntityBuilder.create().setBinary(content.toByteArray()).gzipCompress().build();
			httppost.setEntity(entity);
			HttpResponse httpresponse =execute(httpClient, target, httppost);
			
			HttpEntity response = httpresponse.getEntity();
			BufferedHttpEntity b_entity = new BufferedHttpEntity(response);
			successful = httpresponse.getStatusLine().getStatusCode() < 400;
			return b_entity.getContent();
		}
		finally{
			content = null;
			if(httpClient!=null){
				httpClient.close();
			}
		}
	}

	@Override
	public boolean isSuccessful() {
		return successful;
	}
}
