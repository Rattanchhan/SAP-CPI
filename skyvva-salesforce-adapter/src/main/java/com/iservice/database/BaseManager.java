package com.iservice.database;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.JsonObject;
import com.iservice.gui.helper.ISchedulerSettingHelper;
import com.iservice.sqlite.SQLite;
import com.iservice.utils.DateUtils;

public abstract class BaseManager {

	// require field in database
	public static final String MODIFIED_BY = "modifiedby";
	public static final String MODIFIED_DATE = "modifieddate";
	public static final String CREATED_BY = "createdby";
	public static final String CREATED_DATE = "createddate";

	public abstract List<String> getFields();

	public abstract String getTableName();
	public abstract String[] getIndexes();
	public abstract String[] getUniques();
	public static final String AGENT_ID = "agent_id";
	
	public BaseManager() {}

	public boolean checkUpdateDeleted(JsonObject item) {
		return false;
	}

	public boolean isDeletedRecord(JsonObject item) {
		return false;
	}

	public SQLite getDb() {
		return SQLite.getInstance();
	}
	
	public static <C extends BaseManager> C getDao(Class<C> cls) {
		if(cls==PropertySettingDao.class){
			//do not create instance of setting
			return (C)PropertySettingDao.getInstance();
		}
		return SQLite.getDao(cls);
	}

	protected void execSQL(String query) {
		execSQL(query, null);
	}

	/**
	 *
	 * @param query
	 * @param critria
	 *            key is criteria name
	 */
	protected void execSQL(String query, Map<String, Object> criteria) {
		getDb().executeStatment(query, criteria);
	}

	public List<Map<String, Object>> query(String query) {
		return query(query, (Map<String, Object>)null);
	}

	protected List<Map<String, Object>> query(String query, Map<String, Object> criteria) {
		return getDb().query(query, criteria);
	}
	
	public <T> List<T> query(String q, Class<T> c) {
		return query(q, c,null);
	}

	// TODO: change params order: String,Map,Class
	protected <T> List<T> query(String query, Class<T> c, Map<String, Object> criteria) {
		List<T> records = getDb().query(query, c, criteria);
		return new ArrayList<T>(records);
	}

	protected void dropTable(String table) {
		execSQL("DROP TABLE " + table);
	}

	public Map<String, String> initCols() {
		Map<String, String> cols = new HashMap<String, String>();

		for (String col : getFields()) {
			cols.put(StringUtils.trim(col), "TEXT");
		}

		// Add custom fields for BaseDao instances
		if (this instanceof BaseDao) {
			//all table must have a primary key
			cols.put(AGENT_ID, "INTEGER PRIMARY KEY AUTOINCREMENT");
			cols.put(CREATED_DATE, "TEXT");
			cols.put(CREATED_BY, "TEXT");
			cols.put(MODIFIED_DATE, "TEXT");
			cols.put(MODIFIED_BY, "TEXT");
		}
		return cols;
	}

	public void deleteByCriteria(Map<String, Object> criteria) {
		if (criteria != null && !criteria.isEmpty()) {
			String query = "DELETE FROM " + getTableName() + " WHERE ";
			boolean first = true;
			for (String col : criteria.keySet()) {
				if (!first) {
					query += " AND ";
				}
				first = false;
				query += col + "= :" + col;
			}
			execSQL(query, criteria);
		}
	}
	
	public void update(JsonObject jsondata) {
		jsondata.addProperty(ISchedulerSettingHelper.SYNC_STATUS, ISchedulerSettingHelper.SFSYNC);
		jsondata.addProperty(ISchedulerSettingHelper.MODIFIED_DATE, DateUtils.format(new Date()));
		JsonObject wherecondition = new JsonObject();
		wherecondition.addProperty(ISchedulerSettingHelper.SCHEDULER_TYPE, com.iservice.utils.StringUtils.getString(jsondata, ISchedulerSettingHelper.SCHEDULER_TYPE));
		getDb().update(getTableName(), jsondata, wherecondition);
	}
	
	public void bulkUpsert(JsonObject item) {
		getDb().bulkUpsert(this, item);
	}
	
	public int bulkUpsert(List<JsonObject> items) {
		return getDb().bulkUpsert(this, items);
	}
	
	public void insert(BaseManager dao,Map<String, Object> item) {
		String fields = "";
		String setValue = "VALUES (";
		String comma = "";
		boolean isComa = false;

		Map<String, Object> params = new HashMap<String, Object>();
		
		for (String field : dao.getTableFields()) {

			if (isComa) {
				comma = ",";
			} else {
				isComa = true;
			}
			fields += comma + field;
			setValue += comma + ":" + field;
			params.put(field, item.get(field));

		}

		setValue += ")";
		String sql = "insert into " + dao.getTableName() + " (" + fields + ") " + setValue;

		getDb().executeStatment(sql, params);

	}

	protected void delete(String deletStmt) {
		execSQL(deletStmt);
	}

	protected void delete(String deletStmt, Map<String, Object> criterias) {
		execSQL(deletStmt, criterias);
	}

	public List<String> getTableFields() {
		return new ArrayList<String>(getFields());
	}

	protected void update(Map<String, Object> item, Map<String, Object> criterias) {
		BaseManager dao = getDao(getClass());
		StringBuilder sb = new StringBuilder();
		sb.append("UPDATE " + dao.getTableName() + " SET ");

		Map<String, Object> params = new HashMap<>();
		boolean first = true;

		for (String field : dao.getTableFields()) {
			// right now update only field in the rec.
			if (item.containsKey(field)) {
				// String field = dtoField.getElement_name();
				if (first) {
					first = false;
				} else {
					sb.append(", ");
				}
				sb.append(field + " = :" + field);
				params.put(field, item.get(field));
			}
		}

		sb.append(getWhere(criterias, params));

		execSQL(sb.toString(), params);
	}
	
	protected String getWhere(Map<String, Object> criterias,
			Map<String, Object> params) {

		if (criterias != null) {
			StringBuffer sb = new StringBuffer(" WHERE ");
			boolean first = true;
			for (String f : criterias.keySet()) {
				if (!first) {
					sb.append(" AND ");
				}
				String pname = f + "_c";
				sb.append(f + "=:" + pname);
				first = false;
				params.put(pname, criterias.get(f));
			}
			return sb.toString();
		} else {
			return "";
		}
	}
	
	protected void update(Map<String, Object> item,
			String column, String value) {
		Map<String, Object> criterias = new HashMap<>();
		criterias.put(column, value);
		update( item, criterias);
	}

	public void deleteAll() {
		delete("DELETE FROM " + getTableName());
	}

	public String ensureColName(String colname) {
		Set<String> cols = initCols().keySet();
		for (String origName : cols) {
			if (StringUtils.equalsIgnoreCase(colname, origName)) {
				return origName;
			}
		}
		return colname;
	}
}