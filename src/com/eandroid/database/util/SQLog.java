/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-5-25
 * @version 0.1
 */
package com.eandroid.database.util;

import android.content.ContentValues;

import com.eandroid.database.SQL;
import com.eandroid.util.EALog;
import com.eandroid.util.StringUtils;

public class SQLog {

	public static void d(String tag,SQL sql){
		if(sql == null || sql.getSql() == null)
			return;
		StringBuilder sqlString = new StringBuilder("sql:");
		sqlString.append(sql.getSql());
		if(sql.getBindArgs() != null && !sql.getBindArgs().isEmpty()){
			sqlString.append(" args:[")
				.append(sql.getBindArgsAsArray().toString())
				.append("]");
		}
		EALog.d(tag, sqlString.toString());
	}
	
	public static void d(String tag,String sql){
		if(StringUtils.isEmpty(sql))
			return;
		EALog.d(tag, sql);
	}
	
	public static void insert(String tag,String tableName,ContentValues cvs){
		if(StringUtils.isEmpty(tableName))
			return;
		StringBuilder sqlString = new StringBuilder("sql:");
		sqlString.append("insert into ").append(tableName);
		if(cvs != null && cvs.size() > 0){
			sqlString.append(" args:[")
				.append(cvs.toString())
				.append("]");
		}
		EALog.d(tag, sqlString.toString());
	}
	
}
