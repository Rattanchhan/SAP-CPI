package com.iservice.model;

public class HttpCredential extends AbstractDataModel{
	
	private String username;
	private String password;
	private int port;
	private boolean isssl;
	private String keystorename;
	private String keystorepassword;
	
	public String getUsername() {
		return username;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}
	
	public String getPassword() {
		return password;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public int getPort() {
		return port;
	}
	
	public void setPort(int port) {
		this.port = port;
	}
	
	public String getKeystorename() {
		return keystorename;
	}
	
	public void setKeystorename(String keystorename) {
		this.keystorename = keystorename;
	}
	
	public String getKeystorepassword() {
		return keystorepassword;
	}
	
	public void setKeystorepassword(String keystorepassword) {
		this.keystorepassword = keystorepassword;
	}

	public boolean isIsssl() {
		return isssl;
	}

	public void setIsssl(boolean isssl) {
		this.isssl = isssl;
	}	
	
}
