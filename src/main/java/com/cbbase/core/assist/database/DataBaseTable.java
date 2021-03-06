package com.cbbase.core.assist.database;

import java.util.List;
import java.util.Map;

import com.cbbase.core.assist.jdbc.JdbcConnection;
import com.cbbase.core.assist.jdbc.JdbcHelper;
import com.cbbase.core.tools.StringUtil;
 

/**
 * 
 * @author changbo
 *
 */
public class DataBaseTable{
	
	private String database = null;
	
	public DataBaseTable(){
	}
	
	public DataBaseTable(String database){
		this.database = database;
	}
	
	public List<Map<String, Object>> queryColumns(String tableName){
		return queryColumns(tableName, null);
	}
	
	public List<Map<String, Object>> queryColumns(String tableName, String tableSchema){
		String sql = "";
		if(JdbcConnection.isOracle(database)){
			sql = "select col.column_name, col.data_type, col.data_length, col.data_scale, decode(col.column_name, 'ID', 'Y', 'N') is_primary, col.nullable, com.comments  "
			+ "from user_tab_columns col "
			+ "left join user_col_comments com "
			+ "on com.table_name = col.table_name and com.column_name = col.column_name "
			+ "where col.table_name='" + tableName +"' ";
		}else if(JdbcConnection.isMysql(database)){
			sql = "select column_name, data_type, character_maximum_length data_length, numeric_scale data_scale, if(column_key = 'PRI', 'Y', 'N') is_primary, if(is_nullable = 'YES', 'Y', 'N') nullable, column_comment comments  "
				+ "from information_schema.COLUMNS where table_name = '" + tableName +"'";
			if(StringUtil.hasValue(tableSchema)) {
				sql += " AND table_schema = '"+tableSchema+"'";
			}
		}
		List<Map<String, Object>> list = JdbcHelper.getJdbcHelper(database).query(sql);
		
		return list;
	}
	
	public String querySelectColumns(String tableName){
		return querySelectColumns(tableName, null);
	}
	
	public String querySelectColumns(String tableName, String tableSchema){
		List<Map<String, Object>> list = queryColumns(tableName, tableSchema);
		StringBuffer sb = new StringBuffer("\nselect ");
		int i=1;
		for(Map<String, Object> map : list) {
			sb.append(map.get("column_name")+", ");
			if(sb.length() > 120*i) {
				sb.append("\n");
				i++;
			}
		}
		sb.setLength(sb.length()-2);
		sb.append("\nfrom ");
		if(tableSchema != null) {
			sb.append(tableSchema+".");
		}
		sb.append(tableName);
		return sb.toString();
	}
	
	
	public List<Map<String, Object>> queryIndex(String tableName){
		String sql = "";
		if(JdbcConnection.isOracle(database)){
			sql = "select * from user_indexes where table_name='"+tableName+"';";
		}else if(JdbcConnection.isMysql(database)){
			sql = "show index from " + tableName;
		}
		List<Map<String, Object>> list = JdbcHelper.getJdbcHelper(database).query(sql);
		
		return list;
	}
 

	public List<Map<String, Object>> queryTables(){
		String sql = "";
		if(JdbcConnection.isOracle(database)){
			sql = "select table_name from user_tables ";
		}else if(JdbcConnection.isMysql(database)){
			sql = "select table_name, table_comment from information_schema.tables ";
		}
		List<Map<String, Object>> list = JdbcHelper.getJdbcHelper(database).query(sql);
		
		return list;
	}
	
	public String getTableComment(String tableName) {
		List<Map<String, Object>> list = queryTables();
		for(Map<String, Object> map : list) {
			if(StringUtil.isEqualIgnoreCase(tableName, StringUtil.getValue(map.get("table_name")))) {
				if(map.get("table_comment") == null) {
					return null;
				}
				return map.get("table_comment").toString();
			}
		}
		return null;
	}
	
}
