package com.iservice.sqlite;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiLoggingDecorator;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;

import com.google.gson.JsonObject;
import com.iservice.database.BaseDao;
import com.iservice.database.BaseManager;
import com.iservice.database.HTTPSettingDao;
import com.iservice.database.LogDao;
import com.iservice.database.PropertySettingDao;
import com.iservice.database.SFProcessingJobDao;
import com.iservice.gui.helper.Helper;
import com.iservice.gui.helper.PasswordProtector;
import com.iservice.gui.helper.ServerHelper;
import com.iservice.helper.FileHelper;
import com.iservice.model.ISchedulerSetting;
import com.iservice.sforce.MapSFConnInfo;

public class SQLite {

	public static final String AGENT_DB = "agent.db";
	public static final String AGENT_CONF = "agent.conf";
	private String dbName = "";
	private String sJdbc = "jdbc:sqlite";
	private String userdir;
	private static final int FIELD = 0;
	private static final int PARAMS = 1;
	private static final int UPDATE = 2;
	private Properties currentSetting;
//	private G2GSql2o sql2o = null;
	protected Set<String> readingAttachment = new HashSet<String>();

//	public G2GSql2o getSql2o() {
//		return sql2o;
//	}

	// make sure all dao refresh
	private  Map<Class<? extends BaseManager>, BaseManager> entity2Dao = new HashMap<>();
	{
		entity2Dao.put(PropertySettingDao.class, new PropertySettingDao());
		entity2Dao.put(LogDao.class, new LogDao());
//		entity2Dao.put(ISchedulerSettingDao.class, new ISchedulerSettingDao());
		entity2Dao.put(HTTPSettingDao.class, new HTTPSettingDao());
		entity2Dao.put(SFProcessingJobDao.class, new SFProcessingJobDao());
//		entity2Dao.put(StreamingApiDao.class, new StreamingApiDao());
		// Main Entity
	}

	private static SQLite db;

    private static final CpiLoggingDecorator LOGGER = CpiLoggingDecorator.getLogger(SQLite.class);

	public synchronized static SQLite getInstance() {
		if (db == null) {
			db = new SQLite();
//			checkDB(db);
		}
		return db;
	}

	public static void resetDb() {
		if (SQLite.db != null) {
//			SQLite.db.sql2o.closeConnection();
		}
		SQLite.db = null;
	}

	public String getDBName() {
		return dbName;
	}

	public static Collection<BaseManager> getAllDaos() {

		return getInstance().entity2Dao.values();
	}

	public static Collection<BaseDao> getAllBaseDaos() {
		List<BaseDao> daos = new ArrayList<BaseDao>();
		for (BaseManager bm : getAllDaos()) {
			if (bm instanceof BaseDao) {
				daos.add((BaseDao) bm);
			}
		}

		return daos;
	}

	public synchronized void storePropertiesToFile(Properties prop) {
		try {
			if (prop != null) {
				File settingFile = new File(userdir, AGENT_CONF);
				FileOutputStream outStream = new FileOutputStream(settingFile);
				prop.store(outStream, null);
				outStream.close();
			}
		} catch (Exception e) {
			LOGGER.error("QLite> storePropertiesToFile()> error: "+e.getMessage(), e);
		}
	}

	protected Properties loadProperties(String propFile) {
		Properties realProp = new Properties();

		try {
			File settingFile = new File(userdir, propFile);
			if (!settingFile.exists()) {
				settingFile.createNewFile();
			}
			FileInputStream prop = new FileInputStream(settingFile);
			realProp.load(prop);
			prop.close();

		}
		catch (Exception e) {
			LOGGER.error("QLite> loadProperties()> error: "+e.getMessage(), e);
		}

		return realProp;
	}

	public Map<String,List<ISchedulerSetting>> extractCronFile(String fileName) throws Exception {
		List<ISchedulerSetting> items = new ArrayList<ISchedulerSetting>();
		List<String> listEntries = FileHelper.readCrontabFileListEntries(fileName);
		Map<String,List<ISchedulerSetting>> mapCronPerUser = new HashMap<String,List<ISchedulerSetting>>(); 
		for(String entry : listEntries) {
			ISchedulerSetting iSchedulerSetting = new ISchedulerSetting(entry);
			if(StringUtils.isBlank(iSchedulerSetting.getUsername())) continue;
			items = mapCronPerUser.get(iSchedulerSetting.getUsername());
			if(items==null) items = new ArrayList<ISchedulerSetting>();
			items.add(iSchedulerSetting);
			mapCronPerUser.put(iSchedulerSetting.getUsername(), items);
		}
		return mapCronPerUser;
	}
/*
	public void moveCronFileToDB(String fileName) {
		try {
			Map<String,List<ISchedulerSetting>> mapCronPerUser = extractCronFile(fileName);
			Map<String,Map<String,String>> mapAllUser = BaseDao.getDao(PropertySettingDao.class).getAllUsers();
			for(String username : mapCronPerUser.keySet()) {
				if(mapAllUser.get(username)!=null && mapAllUser.get(username).size()>0) {
					List<ISchedulerSetting> items = mapCronPerUser.get(username);
					Map<String,String> mapProps = mapAllUser.get(username);
					SFIntegrationService intService = new SFIntegrationService(new MapSFConnInfo(mapProps));
					intService.login();
					List<JsonObject> listResult = intService.createISchedulerSetting(items);
					BaseDao.getDao(ISchedulerSettingDao.class).bulkUpsert(listResult);
				}else {
					logIService.warn("QLite> moveCronFileToDB()> Warning: not found properties file Username: " + username + " , from cron file. Please import properties file and import crontab file again." );
				}
			}
			currentSetting.put(Helper.CRONTAB_FILENAME, Helper.CRONTAB_FILENAME);
			storePropertiesToFile(currentSetting);
		} catch (Exception e) {
			logIService.error("QLite> moveCronFileToDB()> error: "+e.getMessage(), e);
		} 
	}
*/
	public static String previousProperty;

	public boolean hasSameUsername(String dir, String fileName) throws FileNotFoundException, IOException {
		PropertySettingDao doa = PropertySettingDao.getInstance();
		Properties properties = new Properties();

		properties.load(new FileInputStream(new File(dir, "")));

		for(String pf : doa.getAllPropertyName()) {
			if(fileName.equals(pf)) {
				continue;
			}

			MapSFConnInfo connInfo = doa.getAllValueByFileName(pf);

			if(properties.get(MapSFConnInfo.USERNAME_P).equals(connInfo.get(MapSFConnInfo.USERNAME_P))) {
				previousProperty = pf;
				return true;
			}
		}
		return false;
	}

	public boolean isExistedPropertiesFile(String fileName) {
		PropertySettingDao dao = PropertySettingDao.getInstance();
		for(String pf : dao.getAllPropertyName()) {
			if(fileName.equalsIgnoreCase(pf)) {
				previousProperty = pf;
				return true;
			}
		}
		return false;
	}
	
	public String getExistedFileName(String fileName) {
		PropertySettingDao dao = PropertySettingDao.getInstance();
		for(String pf : dao.getAllPropertyName()) {
			if(fileName.equalsIgnoreCase(pf)) {
				return pf;
			}
		}
		return fileName;
	}
	
	public boolean isExistedUsername(String newUsername) {
		PropertySettingDao dao = PropertySettingDao.getInstance();
		dao.getAllUsers();
		for(String oldUsername : dao.getAllUsers().keySet()) {
			if(newUsername.equals(oldUsername)) {
				return true;
			}
		}
		return false;
	}

	private void moveDefaultPropertyFileToDB() {
		File folder;
		try {
			folder = new File(Helper.getAgentHome() + Helper.getFileSeparator() + "");
			FilenameFilter textFilter = new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.toLowerCase().contains(".properties");
				}
			};

			File[] files = folder.listFiles(textFilter);
			for (File file : files) {
				if(!currentSetting.contains(file.getName())) {
					upsertPropertiesFile(file);
				}
			}

		} catch (Exception e) {
			LOGGER.error("QLite> movePropertyFileToDB()> error: "+e.getMessage(), e);
		}
	}

	public void upsertPropertiesFile(File file) throws Exception {

		PropertySettingDao dao = PropertySettingDao.getInstance();
		Properties realProp = new Properties();
		FileInputStream prop = new FileInputStream(file);
		realProp.load(prop);
		prop.close();
		dao.loadByFile(file.getName());
		dao.setUsername(realProp.getProperty(MapSFConnInfo.USERNAME_P));
		MapSFConnInfo mapConInfo = new MapSFConnInfo();

		for(Object key : realProp.keySet()) {

			String value = realProp.getProperty((String)key);

			if(org.apache.commons.lang3.StringUtils.equalsIgnoreCase("urlserver", (String)key)) {
				//convert to server environment
				key =  MapSFConnInfo.SERVER_ENVIRONMENT;

				if(org.apache.commons.lang3.StringUtils.contains(value,ServerHelper.getEnvironment(ServerHelper.SANDBOX))){
					value = ServerHelper.SANDBOX;

				}else if(org.apache.commons.lang3.StringUtils.contains(value,ServerHelper.getEnvironment(ServerHelper.PRODUCTION_DEVELOPER))){ 
					value = ServerHelper.PRODUCTION_DEVELOPER;

				}else if(org.apache.commons.lang3.StringUtils.contains(value,ServerHelper.getEnvironment(ServerHelper.WWW))){
					value = ServerHelper.PRODUCTION_DEVELOPER;

				}else{
					value = ServerHelper.PRODUCTION_DEVELOPER;
				}
			}

			if (PropertySettingDao.isEncrypt((String)key) && org.apache.commons.lang3.StringUtils.isNotBlank(value)) {
				try {
					value = PasswordProtector.decrypt(value);
				} catch (GeneralSecurityException e) {}//nothing to do 
			}

			mapConInfo.put((String)key, value);
		}
		
		// save properties local db
		dao.savePropertiesToDatabase(mapConInfo);

		//save setting
		currentSetting.put(file.getName(), file.getName());
		storePropertiesToFile(currentSetting);				
	}
	
	public void upsetNewProperties(String fileName) {
		PropertySettingDao dao = PropertySettingDao.getInstance();
		dao.loadByFile(fileName);
		dao.setUsername("");
		MapSFConnInfo mapConInfo = new MapSFConnInfo();
		dao.getMapPrefs().clear();
		dao.savePropertiesToDatabase(mapConInfo);
		//save setting
		currentSetting.put(fileName, fileName);
		storePropertiesToFile(currentSetting);				
	}

	public static <T extends BaseManager> T getDao(Class<T> cls) {
		return (T)getInstance().entity2Dao.get(cls);
	}

	private SQLite() {
//		this(AGENT_DB);
	}

	private SQLite(String dbName)  {
		this(null, dbName);
	}

	private SQLite(String userdir, String dbName) {

		this.userdir = userdir;
		if(org.apache.commons.lang3.StringUtils.isBlank(this.userdir)){
			try {
				this.userdir = Helper.getAgentHome();
			} catch (IOException e) {
				LOGGER.error(e.getMessage(),e);
				throw new RuntimeException(e.getMessage(),e);
			}
		}
		this.userdir = this.userdir.replaceAll("\\\\", "/");
		this.dbName = dbName;
		currentSetting = loadProperties(AGENT_CONF);
		createDbIfNotExist();
		if (!StringUtils.isEmpty(this.dbName) && isDBFileExist()) {
			init();
		}
		else {
			throw new MissingDataBaseException("Missing database!; dbname=" + dbName);
		}
		// clear cache
		PropertySettingDao.createInstance();

	}

	private boolean isDBFileExist() {
		File settingFile = new File(userdir, dbName);
		return settingFile.exists();
	}

	private void createDbIfNotExist() {
		File settingFile = new File(userdir, dbName);
		if (!settingFile.exists()) {
			try {
				// create new setting when database is created
				settingFile.createNewFile();
				currentSetting = new Properties();
				storePropertiesToFile(currentSetting);
			}
			catch (IOException e) {
				LOGGER.error("QLite> createDbIfNotExist()> error: "+e.getMessage(), e);
			}
		}
		// save db to file
	}

	public String getUserdir() {
		return userdir;
	}

	public void setUserdir(String userdir) {
		this.userdir = userdir;
	}

	private static void checkDB(SQLite db) {
		initDatabase(db);
	}


	public <K> K executeScalar(String query, Class<K> cls) {/*
		try (Connection con = sql2o.open()) {
			Query q = con.createQuery(query);
			return q.executeScalar(cls);
		}
		catch (Exception e) {
			throwException(e);
		}*/
		return null;
	}

	private void init() {
/*
		String sDbUrl = sJdbc + ":/" + userdir + "/" + dbName;
		logIService.trace("SQLite> Init DB> getConnection: " + sDbUrl);
		try {
			sql2o = new G2GSql2o(sDbUrl, "", "") {

				@Override
				public synchronized Connection open() {
					if (!isDBFileExist()) {
						throw new MissingDataBaseException("Missing database!; dbname=" + dbName);
					}
					return super.open();
				}

				@Override
				public synchronized Connection beginTransaction(int isolationLevel) {
					if (!isDBFileExist()) {
						throw new MissingDataBaseException("Missing database!; dbname=" + dbName);
					}
					return super.beginTransaction(isolationLevel);
				}

			};
			sql2o.setDefaultCaseSensitive(true);
		}
		catch (Exception e) {
			throwException(e);
		}*/
	}

	protected static void createTable(SQLite db, String table, Map<String, String> columns) {
		try {
			LOGGER.trace("SQLite> Creating table " + table + "...");

			StringBuilder sb = new StringBuilder();
			sb.append("CREATE TABLE " + table + "(");
			Boolean first = true;
			for (String column : columns.keySet()) {
				if (first) {
					first = false;
				}
				else {
					sb.append(", ");
				}
				sb.append(column + " " + columns.get(column));
			}
			sb.append(")");
			db.executeStatment(sb.toString());
		}
		catch (Exception e) {
			LOGGER.error("QLite> createTable()> error: "+e.getMessage(), e);
		}
	}

	public List<Map<String, Object>> query(String q) {
		return query(q, true);
	}

	/**
	 *
	 * @param q
	 * @return
	 */
	public synchronized List<Map<String, Object>> query(String q, boolean logError) {/*
		try (Connection con = sql2o.open()) {
			//if (con.getJdbcConnection().getAutoCommit()) {
			//	LOGGER.trace(q.toString());
			//}
			return con.createQuery(q, false).executeAndFetchTable().asList();
		}
		catch (Exception e) {
			throwException(e, logError);
		}*/
		return new ArrayList<>();
	}

	/**
	 *
	 * @param q
	 * @param c
	 * @return
	 */
	public <T> List<T> query(String q, Class<T> c) {
		return query(q, c, null);
	}

	/**
	 *
	 * @param query
	 * @param c
	 * @param params
	 * @return
	 */
	public synchronized <T> List<T> query(String query, Class<T> c, Map<String, Object> params) {
/*
		try (Connection con = sql2o.open()) {

			Query q = con.createQuery(query);
			if (params != null) {
				Set<String> fields = params.keySet();
				for (String k : fields) {

					String s = k;
					// remove :
					if (k.indexOf(":") != -1) {
						s = k.substring(1, k.length());
					}
					q.addParameter(s, params.get(k));
				}

			}

			//if (con.getJdbcConnection().getAutoCommit()) {
			//	LOGGER.trace(q.toString());
			//}

			return q.executeAndFetch(c);
		}
		catch (Exception e) {
			throwException(e);
		}*/
		return null;
	}

	public synchronized List<Map<String, Object>> query(String query, Map<String, Object> params) {/*
		try (Connection con = sql2o.open()) {

			Query q = con.createQuery(query, false);
			if (params != null) {
				Set<String> fields = params.keySet();
				for (String k : fields) {

					String s = k;
					// remove :
					if (k.indexOf(":") != -1) {
						s = k.substring(1, k.length());
					}
					q.addParameter(s, params.get(k));
				}

			}

			//if (con.getJdbcConnection().getAutoCommit()) {
			//	LOGGER.trace(q.toString());
			//}

			return q.executeAndFetchTable().asList();
		}
		catch (Exception e) {
			throwException(e);
		}*/
		return null;
	}

	/**
	 *
	 * @param tablename
	 * @param json
	 * @throws SQLException
	 */
	public Integer insert(String tablename, JsonObject json) {/*

		String insertsql = "insert into " + tablename + "(" + buildFields(json, FIELD) + ") values (" + buildFields(json, PARAMS) + ")";
		try (Connection con = sql2o.open()) {
			Query q = con.createQuery(insertsql);
			// bind
			Set<String> fields = json.keySet();
			for (String k : fields) {
				q = q.addParameter("val" + k, json.get(k));
			}
			q.executeUpdate();
		}
		catch (Exception e) {
			throwException(e);
		}*/
		return -1;
	}

	/**
	 *
	 * @param tablename
	 * @param data
	 */
	public void delete(String tablename, JsonObject data) {/*

		String deletesql = "delete from " + tablename + " where " + buildFields(data, UPDATE);
		try (Connection con = sql2o.open()) {
			Query q = con.createQuery(deletesql);
			// bind
			Set<String> fields = data.keySet();
			for (String k : fields) {
				q = q.addParameter("val" + k, data.get(k));
			}
			q.executeUpdate();
		}
		catch (Exception e) {
			throwException(e);
		}*/
	}

	/**
	 *
	 * @param tablename
	 * @param jsondata
	 * @param wherecondition
	 * @throws SQLException
	 */
	public void update(String tablename, JsonObject jsondata, JsonObject wherecondition) {/*
		String filter = buildFields(wherecondition, UPDATE).replaceAll(",", " and ");
		String updatesql = "update " + tablename + " set " + buildFields(jsondata, UPDATE) + " where " + filter;

		try (Connection con = sql2o.open()) {
			Query q = con.createQuery(updatesql);
			// bind
			Set<String> fields = jsondata.keySet();
			for (String k : fields) {
				q = q.addParameter("val" + k, com.iservice.utils.StringUtils.getString(jsondata,k));
			}

			Set<String> where = wherecondition.keySet();
			for (String k : where) {
				q = q.addParameter("val" + k, com.iservice.utils.StringUtils.getString(wherecondition,k));
			}
			q.executeUpdate();
		}
		catch (Exception e) {
			throwException(e);
		}*/
	}

	public void update(String updatesql) {/*

		try (Connection con = sql2o.open()) {
			Query q = con.createQuery(updatesql);
			q.executeUpdate();
		}
		catch (Exception e) {
			throwException(e);
		}
*/
	}

	public static String buildFields(JsonObject json, int type) {

		String s = "";

		Set<String> fields = json.keySet();
		for (String k : fields) {

			String val = "";
			if (type == FIELD) {
				val = k;
			}
			else if (type == PARAMS) {
				val = ":val" + k;
			}
			else if (type == UPDATE) {
				val = k + "=" + ":val" + k;
			}
			s = s + val + " ,";
		}

		// remove last ,
		if (s != "") {
			s = s.substring(0, s.length() - 1);
		}

		return s;
	}

	public static void main(String argv[]) {
		// try {
		// SQLite.login("cp_prod/noakl", "Coloplast1");
		// }
		// catch (Exception e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// // String userdir = System.getProperty("user.home");
		// //
		SQLite.db = SQLite.getInstance();
		// initDatabase(db);
		// // try {
		// // // String res = listmap_to_json_string(a.query("select * from
		// // // user"));
		// //
		// // // System.out.print(res);
		// //
		// // JSONParser parser = new JSONParser();
		// // JSONObject jsondata = (JSONObject) parser
		// // .parse("{\"Name\" : \"Test\", \"Site\" : \"test\", \"staff\" : \"12\" }");
		// // JSONObject condition = (JSONObject)
		// // parser.parse("{\"id\" : \"123\" }");
		// //
		// // // a.update("account", jsondata , condition);
		// // // a.insert("account", jsondata);
		// // a.delete("account", condition);
		// //
		// // } catch (Exception e) {
		// // // TODO Auto-generated catch block
		// // e.printStackTrace();
		// // }
		//
		// // SQLLite.getInstance().importMeta("export.sql");
		// SQLite.getInstance().importLayoutData();
		// // BaseManager.initDatabase();


	}
	protected String generateInsertOrReplaceQuery(List<String> fields, BaseManager dao) {
		StringBuilder strField = new StringBuilder();
		StringBuilder params = new StringBuilder();
		boolean first = true;
		for (String field : fields) {
			if (field != null) {
				if (!first) {
					params.append(",");
					strField.append(",");
				}
				first = false;
				strField.append(field);
				params.append(":" + field);
			}
		}
		String table = dao.getTableName();

		return "INSERT OR REPLACE INTO " + table + " (" + strField + ") values( " + params + ")";
	}
	public synchronized void bulkUpsert(BaseManager dao, JsonObject item) {/*

		StopWatch watch = new StopWatch();
		watch.start();

		if (item != null) {
			Connection con = sql2o.beginTransaction(java.sql.Connection.TRANSACTION_READ_UNCOMMITTED);
			try {
				List<String> tableFields = dao.getTableFields();
				StopWatch watchCreateQuery = new StopWatch();
				watchCreateQuery.start();
				String strQuery = generateInsertOrReplaceQuery(tableFields, dao);
				Query query = con.createQuery(strQuery);
				for (String field : tableFields) {
					query.addParameter(field, com.iservice.utils.StringUtils.getString(item,field));
				}
				query.addToBatch();
				watchCreateQuery.stop();
				//LOGGER.trace("createQuery:" + (watch.getTime() / 1000));
				query.executeBatch();
				con.commit(); 
			}
			catch (Exception e) {
				con.rollback();
				throwException(e);
			}
		}
		watch.stop();
		//LOGGER.trace("save time:" + (watch.getTime() / 1000));*/
	}

	public synchronized int bulkUpsert(BaseManager dao, List<JsonObject> records) {
		StopWatch watch = new StopWatch();
		watch.start();

		int count = 0;/*
		if (records != null && !records.isEmpty()) {
			Connection con = sql2o.beginTransaction(java.sql.Connection.TRANSACTION_READ_UNCOMMITTED);
			try {

				List<String> tableFields = dao.getTableFields();
				StopWatch watchCreateQuery = new StopWatch();
				watchCreateQuery.start();
				String strQuery = generateInsertOrReplaceQuery(tableFields, dao);
				Query query = con.createQuery(strQuery);
				for (JsonObject item : records) {
					for (String field : tableFields) {
						query.addParameter(field, com.iservice.utils.StringUtils.getString(item,field));
					}
					query.addToBatch();
					count++;
				}
				watchCreateQuery.stop();
				//LOGGER.trace("createQuery:" + (watch.getTime() / 1000));
				query.executeBatch();
				con.commit(); // remember to call commit(), else sql2o will
				// automatically rollback.
			}
			catch (Exception e) {
				con.rollback();
				throwException(e);
			}
		}

		watch.stop();
		//LOGGER.trace("save time:" + (watch.getTime() / 1000));*/
		return count;
	}

/*	
	public Connection beginTransaction() {
		return sql2o.beginTransaction(java.sql.Connection.TRANSACTION_READ_UNCOMMITTED);
	}

	public void commit(Connection conn) {
		if (conn != null) {
			conn.commit();
		}
	}

	public void rollback(Connection conn) {
		if (conn != null) {
			conn.rollback();
		}
	}

	public long executeStatment(Connection con, String stmt, Map<String, Object> params) {
		return executeStatment(con, stmt, params, true);
	}

	public synchronized long executeStatment(Connection con, String stmt, Map<String, Object> params, boolean logError) {
		try {
			Query q = con.createQuery(stmt);
			if (params != null) {
				Set<String> fields = params.keySet();
				for (String k : fields) {

					String s = k;
					// remove :
					if (k.indexOf(":") != -1) {
						s = k.substring(1, k.length());
					}
					try {
						q.addParameter(s, params.get(k));
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			q.executeUpdate();

		}
		catch (Exception e) {

			throwException(e, logError);

		}
		return -1;
	}
*/
	private void throwException(Throwable e) throws RuntimeException {
		throwException(e, true);
	}

	private void throwException(Throwable e, boolean logerror) throws RuntimeException {
		if (logerror) {
			LOGGER.error(e.getMessage(), e);
		}
		if (!isDBFileExist()) {
			throw new MissingDataBaseException(e.getMessage(), e);
		}
		else if (e.getMessage() != null && e.getMessage().contains("[SQLITE_CONSTRAINT]")) {
			throw new ConstraintException(e.getMessage(), e);
		}
		else {
			throw new DataBaseException(e.getMessage(), e);
		}
	}

	public long executeStatment(String stmt) {
		return executeStatment(stmt, null);

	}

	public long executeStatment(String stmt, Map<String, Object> params) {
		return executeStatment(stmt, params, true);
	}

	public long executeStatment(String stmt, Map<String, Object> params, boolean logError) {
//		return executeStatment(sql2o.open(), stmt, params, logError); 
		return 0;
	}

	private static void checkTable(BaseManager dao, SQLite db) {
		Map<String, String> cols = dao.initCols();
		checkTable(db, dao, dao.getTableName(), cols);
	}

	protected static void checkTable(SQLite db, BaseManager dao, String table, Map<String, String> columns) {
		LOGGER.trace("SQLite> Checking table: " + table + "...");

		String statementSelect = "SELECT name FROM sqlite_master WHERE type='table' AND name like '" + table + "'";
		try {

			List<Map<String, Object>> result = db.query(statementSelect, false);
			if (result == null || result.isEmpty()) {
				createTable(db, table, columns);
				createIndex(dao, db);
			}

		}
		catch (Exception e) {
			createTable(db, table, columns);
			createIndex(dao, db);
		}
	}

	private static void initDatabase(SQLite db) {
		for (BaseManager dao : db.entity2Dao.values()) {
			initTable(dao, db);
		}

		db.moveDefaultPropertyFileToDB();

		LOGGER.trace("SQLite> Database initialization: done");
	}

	private static void initTable(BaseManager dao, SQLite db) {
		// check missing table
		checkTable(dao, db);
	}

	private static void createIndex(BaseManager dao, SQLite db) {
		// create indexes
		String[] indexes = dao.getIndexes();
		if(indexes != null) {
			addIndex(dao, db, indexes, "");
		}
		// create uniques
		if(dao.getUniques() != null) {
			ArrayList<String> uniques = new ArrayList<String>(Arrays.asList(dao.getUniques()));
			addIndex(dao, db, uniques.toArray(new String[] {}), "UNIQUE");
		}

	}

	private static void addIndex(BaseManager dao, SQLite db, String[] indexes, String unique) {
		if (indexes == null || indexes.length == 0) {
			return;
		}
		String table = dao.getTableName();
		for (String index : indexes) {
			String indexName = "i_" + table + "_" + com.iservice.utils.StringUtils.md5hash(table + " " + index + " " + unique);
			String stmtCreateIndex = "CREATE " + unique + " INDEX IF NOT EXISTS " + indexName + " ON " + table + " ( " + index + " );";
			//LOGGER.trace(stmtCreateIndex);

			try {
				db.executeStatment(stmtCreateIndex);
			}
			catch (Exception e) {
				// nothing todo
				//e.printStackTrace();
			}
		}

	}

	public Properties getCurrentSetting() {
		return currentSetting;
	}

}
