package com.iservice.database;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.iservice.gui.helper.PasswordProtector;
import com.iservice.model.Prefs;
import com.iservice.sforce.MapSFConnInfo;
import com.iservice.task.GenericSFTask;
import com.iservice.utils.DateUtils;

public class PropertySettingDao extends BaseDao {
	
	// SF kies from SQLite
	public static final String PROPERTYFILE = "propertyfile";
	public static final String USERNAME = "username";
	public static final String VALUE = "value";
	public static final String KEY = "key";
	public static final String SF_ID = "sfid";
	
	public static final String TABLE_NAME = "propertysetting";
	public static final String SF_ACCESS_TOKEN = "access_token";	
	public static String LOG_FILES = "log_files";
	
	private static ThreadLocal<PropertySettingDao> instance = new ThreadLocal<>();
	protected String filename;
	
	private Map<String,Prefs> mapPrefs = new HashMap<String, Prefs>();
	
	public static PropertySettingDao getInstance() {
		if (instance.get() == null) {
			createInstance();
		}
		return instance.get();
	}

	public static PropertySettingDao createInstance() {
		instance.set(new PropertySettingDao());
		return instance.get();
	}
	
	@Override
	public List<String> getFields() {
		return new ArrayList<String>(Arrays.asList(KEY, VALUE,USERNAME,PROPERTYFILE,SF_ID));
	}

	public String getUserName() {
		return getValue(MapSFConnInfo.USERNAME_P);
	}
	
	public void setUsername(String username){
		mapKey.put(MapSFConnInfo.USERNAME_P, username);
	}

	public String getPassword() {
		return getValue(MapSFConnInfo.PASSWORD_P);
	}
	public boolean getValueBool(String key) {
		return Boolean.parseBoolean(getValue(key));
	}

	public int getValueInt(String key) {
		return Integer.parseInt(getValue(key));
	}
	
	public MapSFConnInfo getAllValues(String username) {
		filename = username;
		try {
			mapKey = new MapSFConnInfo();
			addPrefToMap(CacheSqlite.getInstance().getPropertySetting().get(username));
		} catch (Exception e) {
			return new MapSFConnInfo();
		}
		return mapKey;
	}
	
	public MapSFConnInfo getAllValueByFileName(String fileName) {
		filename = fileName;
		try {
			mapKey = new MapSFConnInfo();
			Map<String,Object> param = new HashMap<>();
			param.put(PROPERTYFILE, fileName);
			List<Prefs> prefs = getDb().query("select "+KEY+","+VALUE+","+SF_ID+","+USERNAME+","+CREATED_DATE+","+CREATED_BY+" from "+ getTableName() +" where "+PROPERTYFILE+"=:"+PROPERTYFILE,Prefs.class,param);
			addPrefToMap(prefs);
		} catch (Exception e) {
			return new MapSFConnInfo();
		}
		return mapKey;
	}

	protected void addPrefToMap(List<Prefs> prefs) {
		if (prefs.size() > 0) {
			for (Prefs pref : prefs) {
				String value = pref.value;
				if(isEncrypt(pref.key) && StringUtils.isNotBlank(value)){
					try {
						value = PasswordProtector.decrypt(value);
					} catch (Exception e) {
						//nothing to do
					}
				}
				mapKey.put(pref.key, value);
				mapPrefs.put(pref.key, pref);
			}
		}
		
	}
	
	protected MapSFConnInfo mapKey = null;

	public PropertySettingDao() {
		mapKey = new MapSFConnInfo();
	}
	
	public MapSFConnInfo getSelectedProperties() {
		return mapKey;
	}
	
	public void savePropertiesToDatabase(MapSFConnInfo mapConInfo) {
		
		String today = DateUtils.format(new Date());
		setUsername(mapConInfo.get(MapSFConnInfo.USERNAME_P));
		
		// Salesforce settings
		setValue(MapSFConnInfo.USERNAME_P, mapConInfo.getUsername(), today);
		setValue(MapSFConnInfo.PASSWORD_P, mapConInfo.getPassword(), today);
		setValue(MapSFConnInfo.TOKEN_P, mapConInfo.getToken(), today);
		setValue(MapSFConnInfo.SERVER_ENVIRONMENT, mapConInfo.getServerEnvironment(), today);
		
		// Agent settings
		setValue(MapSFConnInfo.PACKAGE_P, mapConInfo.getSkyvvaPackage(), today);
		setValue(MapSFConnInfo.PUSH_LOGS2SF, mapConInfo.getPushLogs2SF(), today);
		
		// Proxy settings
		setValue(MapSFConnInfo.PROXY_USED, mapConInfo.getProxyUse(), today);
		setValue(MapSFConnInfo.PROXY_HOST, mapConInfo.getProxyHost(), today);
		setValue(MapSFConnInfo.PROXY_PORT, mapConInfo.getProxyPort(), today);
		setValue(MapSFConnInfo.PROXY_USERNAME, mapConInfo.getProxyUsername(), today);
		setValue(MapSFConnInfo.PROXY_PASSWORD, mapConInfo.getProxyPassword(), today);
		
		// Agent Credential
		setValue(MapSFConnInfo.AGENT_USERNAME, mapConInfo.getAgentUsername(), today);
		setValue(MapSFConnInfo.AGENT_PASSWORD, mapConInfo.getAgentPassword(), today);
		setValue(MapSFConnInfo.HOST_NAME, mapConInfo.getHostName(), today);
		setValue(MapSFConnInfo.PORT_FORWARD, mapConInfo.getPortForward(), today);
		
	}
	
	public String getSFID(String username, String key) {
		Map<String,Object> param = new HashMap<>();
		param.put(USERNAME, username);
		param.put(KEY, key);
		List<Prefs> prefs = getDb().query("select "+KEY+","+VALUE+" from "+ getTableName() +" where "+USERNAME+"=:"+USERNAME+" AND "+KEY+"=:"+KEY,Prefs.class,param);
		if(prefs.size()>0) return prefs.get(0).getSfid();
		return null;
	}

	public String getValue(String key) {
		if(mapKey==null) return null;
		if(!mapKey.containsKey(key)) {
			List<Map<String, Object>> result = getDb().query("SELECT * FROM " + getTableName() + " WHERE "+KEY+"='" + key + "'");
			if (result.size() > 0) {
				Map<String, Object> obj = result.get(0);
				mapKey.put(obj.get(KEY).toString(), obj.get(VALUE).toString());
				if (isEncrypt(key)) {
					try {
						mapKey.put (key,PasswordProtector.decrypt(mapKey.get(key)));
					} catch (Exception e) {
						//nothing to do
					}
				}
			}
		}
		return mapKey.get(key);
	}
	
	public void setValue(String key, String value, String today) {
		String originalVal = value;
		if (isEncrypt(key) && StringUtils.isNotBlank(value)) {
			try {
				value = PasswordProtector.encrypt(value);
			} catch (GeneralSecurityException e) {}//nothing to do 
		}
		Prefs prefs = new Prefs();
		if(mapPrefs==null || mapPrefs.get(key)==null) {
			prefs.setKey(key);
			prefs.setValue("");
			prefs.setCreateddate(today);
			prefs.setCreatedby(getUserName());
		}else {
			prefs=mapPrefs.get(key);
		}
		setCreatedBy(StringUtils.isBlank(prefs.getCreatedby())?getUserName():prefs.getCreatedby());
		setCreatedDate(StringUtils.isBlank(prefs.getCreateddate())?today:prefs.getCreateddate());
		setModifiedDate(today);
		upsertPropertiesToDb(key, value, prefs);
		// in memory we store normal string
		mapKey.put(key, originalVal);
	}

	public static boolean isEncrypt(String key){
		return StringUtils.equalsIgnoreCase(key, MapSFConnInfo.PASSWORD_P)||StringUtils.equalsIgnoreCase(key, MapSFConnInfo.PROXY_PASSWORD) || StringUtils.equalsIgnoreCase(key, MapSFConnInfo.AGENT_PASSWORD);
	}
	
	protected void upsertPropertiesToDb(String key, String value, Prefs prefs) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put(KEY, key);
		params.put(VALUE, value);
		params.put(USERNAME, getUserName());
		params.put(PROPERTYFILE, getFilename());
		params.put(SF_ID, prefs.getSfid());
		//reference
		params.put(BaseManager.CREATED_BY, getCreatedBy());
		params.put(BaseManager.CREATED_DATE, getCreatedDate());
		params.put(BaseManager.MODIFIED_BY, getUserName());
		params.put(BaseManager.MODIFIED_DATE, getModifiedDate());
		String field = SF_ID + "," + KEY+ "," + VALUE + "," + USERNAME + "," + PROPERTYFILE + "," + CREATED_BY + "," + CREATED_DATE + "," + MODIFIED_BY + "," + MODIFIED_DATE;
		String fieldvalue = ":" + SF_ID +",:" + KEY + ",:" + VALUE + ",:" + USERNAME + ",:" + PROPERTYFILE + ",:" + CREATED_BY + ",:" + CREATED_DATE + ",:" + MODIFIED_BY + ",:" + MODIFIED_DATE;
		getDb().executeStatment("INSERT OR REPLACE INTO "+getTableName()+" (" + field + ") VALUES (" + fieldvalue + ")", params);
	}
	
	public void deletePropertiesFromDb(String filename) {
		getDb().executeStatment("DELETE FROM "+getTableName()+" WHERE " + PROPERTYFILE + " = '" + filename + "'");
	}
	
	@Override
	public String getTableName() {
		return TABLE_NAME;
	}
	
	public void loadByFile(String file){
		getAllValueByFileName(file);
	}
	public void loadByUser(String username){
		getAllValues(username);
	}

	@Override
	public String[] getUniques() {
		return new String[] { "key,propertyfile"};
	}

	public List<String> getAllPropertyName() {
		String query ="select DISTINCT "+PROPERTYFILE+" from "+getTableName();
		
		List<Map<String,Object>> result = query(query);
		List<String> ls = new ArrayList<String>();
		if(result != null && result.size()>0) {
			for(Map<String,Object> obj : result) {
				ls.add((String)obj.get(PROPERTYFILE));
			}
		}
		return ls;
	}
	public Map<String,Map<String,String>> getAllUsers() {
		
		String query ="select * from "+getTableName();
		
		List<Map<String,Object>> result = query(query);
		Map<String,Map<String,String>> map = new HashMap<String,Map<String,String>>();
		if(result != null && result.size()>0) {
			for(Map<String,Object> obj : result) {
				if(StringUtils.isBlank((String)obj.get(USERNAME))) {
					continue;
				}
				Map<String,String> mapPropety = map.get((String)obj.get(USERNAME)) == null ? new HashMap<String,String>() :  map.get((String)obj.get(USERNAME));
				map.put((String)obj.get(USERNAME), mapPropety);
				String key = (String)obj.get(KEY);
				String value = (String) obj.get(VALUE);
				if(("password".equals(key) || "proxyPassword".equals(key)) && StringUtils.isNotBlank(value)) {
					try {
						value = PasswordProtector.decrypt(value);
					} catch (Exception e) {
						//nothing to do
						e.printStackTrace();
					}
				}
				mapPropety.put(key,value);
			}
		}
		
		return map;
	}
	public Map<String,String> getUsernamePropertyFileName() {
		String query ="select * from "+getTableName();
		Map<String,String> mapUsers = new HashMap<>();
		List<Map<String,Object>> result = query(query);
		if(result != null && result.size()>0) {
			for(Map<String,Object> obj : result) {
				if(StringUtils.isBlank((String)obj.get(USERNAME))) continue;
				mapUsers.put((String)obj.get(USERNAME),(String)obj.get(PROPERTYFILE));
			}
		}
		return mapUsers;
	}
	@Override
	public String[] getIndexes() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getString(String key) {
		return getValue(key);
	}
	public String getPackage() {
		if(GenericSFTask.DEMO32){
			return "";
		}
		String thePackage = StringUtils.trimToEmpty("skyvvasolutions/");
		
		return MapSFConnInfo.clearDoubleSlashes(thePackage);
	}
	
	public String getQueryPackage() {
		String thePackage = "skyvvasolutions/";//getPackage();
		thePackage = StringUtils.substringBefore(thePackage, "/");

		if(StringUtils.isNotBlank(thePackage)){
			thePackage+="__";
			thePackage = StringUtils.replace(thePackage, "/", "");
		}
		return StringUtils.trimToEmpty(thePackage);
	}
	public void setFilename(String fileName) {
		this.filename = fileName;
	}
	public String getFilename() {
		return filename;
	}
	
	public String getCreatedBy() {
		return getValue(BaseManager.CREATED_BY);
	}
	public void setCreatedBy(String ceatedby){
		mapKey.put(BaseManager.CREATED_BY, ceatedby);
	}
	public String getCreatedDate() {
		return getValue(BaseManager.CREATED_DATE);
	}
	public void setCreatedDate(String ceateddate){
		mapKey.put(BaseManager.CREATED_DATE, ceateddate);
	}
	public String getModifiedDate() {
		return getValue(BaseManager.MODIFIED_DATE);
	}
	public void setModifiedDate(String modifieddate){
		mapKey.put(BaseManager.MODIFIED_DATE, modifieddate);
	}

	public Map<String, Prefs> getMapPrefs() {
		return mapPrefs;
	}
	public void setMapPrefs(Map<String, Prefs> mapPrefs) {
		this.mapPrefs = mapPrefs;
	}
}
