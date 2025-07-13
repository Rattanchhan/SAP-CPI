package com.iservice.database;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.iservice.gui.helper.PasswordProtector;
import com.iservice.model.HttpCredential;

public class HTTPSettingDao extends BaseManager {
	
	public static final String PORT_APP = "port";
	public static final String PASSWORD = "password";
	public static final String USERNAME = "username";
	public static final String IS_SSL = "isssl";
	public static final String KEYSTORE_NAME = "keystorename";
	public static final String KEYSTORE_PASSWORD = "keystorepassword";
	
	public static final String TABLE_NAME = "httpsetting";
	@Override
	public List<String> getFields() {
		return new ArrayList<String>(Arrays.asList(USERNAME, PASSWORD, PORT_APP, IS_SSL, KEYSTORE_NAME, KEYSTORE_PASSWORD));
	}

	
	@Override
	public String getTableName() {
		return TABLE_NAME;
	}

	@Override
	public String[] getIndexes() {
		return null;
	}

	@Override
	public String[] getUniques() {
		return null;
	}
	
	/**
	 * getDefaultCredential if no one in database
	 * @return HttpCredential
	 */
	private HttpCredential getDefaultCredential(){
		HttpCredential cre = new HttpCredential();
		cre.setUsername("admin");
		try {
			cre.setPassword(PasswordProtector.encrypt("12345"));
		} catch (GeneralSecurityException e) {
			cre.setPassword("12345");
			e.printStackTrace();
		}
		cre.setPort(9091);
		cre.setIsssl(false);
		cre.setKeystorename("");
		cre.setKeystorepassword("");
		return cre;
	}
	
	/**
	 * getCredential from database
	 * @return HttpCredential
	 * If no one in database
	 * @return DefaultCredential
	 * username = admin
	 * password = 12345
	 * portapp = 9091
	 */
	public HttpCredential getCredential() {
		List<HttpCredential> ls =  BaseDao.getDao(HTTPSettingDao.class).query("SELECT "+ USERNAME +", "+ PASSWORD +", "+ PORT_APP +", "+ IS_SSL +", "+ KEYSTORE_NAME +", "+ KEYSTORE_PASSWORD +" FROM "+ TABLE_NAME + " LIMIT 1",HttpCredential.class,null);
		if(ls != null && ls.size()>0) {
			return ls.get(0);
		}
		return getDefaultCredential();
	}
	
}
