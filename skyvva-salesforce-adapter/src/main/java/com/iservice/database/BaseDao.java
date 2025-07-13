package com.iservice.database;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;

import com.iservice.utils.StringUtils;



public abstract class BaseDao extends BaseManager {
	
	protected String stmtFindAll;
	public BaseDao() {
		initStatement();
	}

	protected void initStatement() {
	
	}

	public String generateUniqCriterai(JSONObject rec,
			Map<String, Object> params) {
		return "";
	}

	protected String generateServerId(long g2gId) {
		return "#" + g2gId;
	}

	
	public long insert(Map<String, Object> data) {
		
		try {

			insert(this, data);
			
			return getLastRecordId();
		} catch (Exception e) {
			
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	protected String generateFindAllQuery(Collection<String> columns, String filter, int limit, int offset, String order_by,
			 String group_by) {
		String cols = "";
		
		for (String column : columns) {
			cols += ", " + column;
		}
		if (!StringUtils.isEmpty(order_by)) {
			order_by = " ORDER BY " + order_by;
		}
		else {
			order_by = "";
		}
		String hideByType = "";

		String query = "SELECT agent_id"
				+  cols + " FROM " + getTableName() + " WHERE " + (StringUtils.isEmpty(filter) ? "" : filter + " AND ")
				+ "(deleted = 0 OR deleted IS null) " + hideByType + (StringUtils.isEmpty(group_by) ? "" : "GROUP BY " + group_by) + order_by
				+ (limit == 0 ? "" : (" LIMIT " + limit + " OFFSET " + offset));
		return query;
	}
	public List<Map<String, Object>> findAll(Collection<String> columns, String filter, String selectedId, int limit, int offset, String order_by,
			 String group_by) {

		stmtFindAll = generateFindAllQuery(columns, filter, limit, offset, order_by, group_by);

		List<Map<String, Object>> items = query(stmtFindAll);

		// add a specific item when selectedId arg is provided
		// this is usefull when the user follows links to add the target item
		if (!StringUtils.isEmpty(selectedId)) {
			boolean found = false;
			for (Map<String, Object> item : items) {
				if (item.get(AGENT_ID).equals(selectedId)) {
					found = true;
					break;
				}
			}
			if (!found) {
				if (StringUtils.isNotEmpty(filter)) {
					filter = "(" + filter + ") AND agent_id =" + selectedId;
				}
				else {
					filter = "agent_id =" + selectedId;
				}
				stmtFindAll = generateFindAllQuery(columns, filter, 1, 0, "", "");

				items.addAll(query(stmtFindAll));
			}
		}
		// computeCategory(items);
		// Utils.suppressWarning(items);
		return items;
	}
	public long getLastRecordId() {

		try {
			return getDb().executeScalar("SELECT last_insert_rowid()",
					Long.class);
		} catch (Exception e) {
			// nothing todo--cannot happen
		}

		return -1;
	}
	

	public void update(Map<String, Object> object) {
		Map<String, Object> criteria = new HashMap<String, Object>();
		criteria.put(AGENT_ID, object.get(AGENT_ID));
		update( object, criteria);
	}
}
