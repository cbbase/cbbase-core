package com.cbbase.core.assist.code;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.cbbase.core.assist.database.DataBaseTable;
import com.cbbase.core.tools.CommonUtil;
import com.cbbase.core.tools.StringUtil;

/**
 * 	仅适用于单线程
 * @author changbo
 *
 */
public class CodeAssist {
	
	//基本参数
	public static String basePackage = "com.cbbase";
	public static String modelName = "admin";
	public static String table = "t_sys_login_log";//表名
	public static String table_schema = null;//schema(暂时用不到)
	public static String table_profix = "t_";//表名前缀(生成的代码中将去除)
	
	public static String jdbc_name = "jdbc";//application.properties中数据源配置的前缀
	public static String root_path = CommonUtil.getProjectPath()+"/";//项目路径
	public static String java_path = "src/main/java/";//java代码根目录
	public static String jsp_path = "src/main/WEB-INF/webapp/pages/";//jsp代码根目录
	
	//创建的代码
	public static boolean createXml = true;//是否创建实体类的xml文件(mybatis的xml文件)
	public static boolean createJsp = true;//是否创建jsp文件
	public static boolean createEntity = true;//是否创建实体类java文件
	public static boolean createController = true;//是否创建Controller类java文件
	public static boolean createService = true;//是否创建Service类java文件
	public static boolean createDao = true;//是否创建Dao类java文件
	
	public static boolean toJsp = true;
	public static boolean pageXml = true;//xml文件里是否要包含分页查询语句
	public static boolean commentAsTitle = true;//表里面的注释作为字段标题
	public static boolean addAuth = true;//是否将生成的代码.直接写成文件
	public static boolean coverCore = false;
	
	public static boolean showContent = false;//是否在控制台输出生成的内容
	public static boolean writeFile = true;//是否将生成的代码.直接写成文件
	
	public static boolean extendBaseEntity = true;//实体类是否继承BaseEntity
	public static String[] baseEntityField = {"id", "createTime", "createId", "createName", "updateTime", "updateId", "updateName"};
	
	
	//中间变量
	protected static List<Map<String, Object>> columns = null;
	protected static String package_name = null;
	protected static String package_folder = null;
	protected static String entity_name = null;
	protected static String entity_var = null;
	protected static String auth_name = null;
	protected static String table_comment = null;
	
	
	protected static void init(){
		columns = new DataBaseTable(jdbc_name).queryColumns(table, table_schema);
		table_comment = new DataBaseTable(jdbc_name).getTableComment(table);
		package_name = getPackageName();
		package_folder = getPackageFolder();
		entity_name = getEntityName();
		entity_var = StringUtil.lowerFirst(entity_name);
		auth_name = getAuthName();
		if(!root_path.endsWith("/")){
			root_path = root_path+"/";
		}
	}
	
	protected static String getPackageName(){
		return basePackage + "." + modelName;
	}

	protected static String getPackageFolder(){
		return getPackageName().replaceAll("\\.", "\\\\");
	}
	
	protected static String getEntityName(){
		String name = table;
		if(table.startsWith(table_profix)){
			name = table.substring(table_profix.length());
		}
		name = StringUtil.formatCamel(name);
		name = StringUtil.upperFirst(name);
		return name;
	}
	
	protected static String getAuthName(){
		String name = table;
		if(table.startsWith(table_profix)){
			name = table.substring(table_profix.length());
		}
		name = name.replaceAll("_", ".");
		return name;
	}
	
	protected static boolean hasColumn(String col){
		Vector<String> v = new Vector<String>();
		for(Map<String, Object> map : columns){
			v.add(map.get("column_name").toString());
		}
		return v.contains(col);
	}
	
	protected static boolean isBaseEntityField(String field) {
		List<String> list = Arrays.asList(baseEntityField);
		return list.contains(field);
	}
	
	protected static boolean isSelectField(String field) {
		if(field.toLowerCase().endsWith("type") 
				|| field.toLowerCase().endsWith("status")
				|| field.toLowerCase().endsWith("id")) {
			return true;
		}
		return false;
	}

	protected static String getFieldTitle(int i) {
		String title = StringUtil.formatCamel(columns.get(i).get("column_name").toString());
		String comments = StringUtil.getValue(columns.get(i).get("comments"));
		if(commentAsTitle && StringUtil.hasValue(comments)) {
			String split = null;
			int index = 10;
			String[] splitArray = {":", "：", ",", "，", ".", "。"};
			for(String s : splitArray) {
				int p = comments.indexOf(s);
				if(p > 0 && p < index) {
					index = p;
					split = s;
				}
			}
			if(split != null) {
				title = comments.split(split)[0];
			}else {
				title = comments;
			}
			if(title.length() > 10) {
				title = title.substring(0, 10);
			}
			if(title.toLowerCase().indexOf("id") >= 0) {
				title = title.toLowerCase().replaceAll("id", "");
			}
		}
		return title;
	}
	
	protected static String getFieldConmment(int i) {
		return StringUtil.getValue(columns.get(i).get("comments"));
	}
	
	protected static void executeForAllTables(String tablePrefix){
		DataBaseTable db = new DataBaseTable(null);
		List<Map<String,Object>> tables = db.queryTables();
		for(Map<String, Object> map : tables){
			String tableName = map.get("table_name").toString();
			if(tableName.startsWith(tablePrefix)){
				table = tableName;
				execute();
			}
		}
	}
	
	public static void execute(){
		System.out.println("table:"+table);
		init();
		if(createXml){
			CodeXml.create();
		}
		if(createJsp){
			CodeJsp.create();
		}
		if(createEntity){
			CodeEntity.create();
		}
		if(createController){
			CodeController.create();
		}
		if(createService){
			CodeService.create();
		}
		if(createDao){
			CodeDao.create();
		}
	}

}
