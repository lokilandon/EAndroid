/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-5-20
 * @version 0.1
 */
package com.eandroid.util;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class DatabaseUtils {

	private static final String TAG = "DatebaseUtils";
	public static boolean isTableExist(String tableName,SQLiteDatabase db){
		Cursor cursor = null;
		try {
			String sql = "SELECT COUNT(*) AS c FROM sqlite_master WHERE type ='table' AND name ='"+tableName+"' ";
			cursor = db.rawQuery(sql, null);
			if(cursor!=null && cursor.moveToNext()){
				int count = cursor.getInt(0);
				if(count>0){
					return true;
				}
			}

		} catch (Exception e) {
			EALog.e(TAG, e.toString());
		}finally{
			if(cursor!=null)
				cursor.close();
			cursor=null;
		}
		return false;
	}

	public static String wrapDBProperty(Object property){
		if(property instanceof String 
				|| property instanceof java.util.Date 
				|| property instanceof java.sql.Date){
			return "\'" + property.toString() + "\'";
		}else {
			return property.toString();
		}
	}

}
