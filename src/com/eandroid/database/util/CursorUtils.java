/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-5-26
 * @version 0.1
 */
package com.eandroid.database.util;

import java.util.ArrayList;
import java.util.List;

import android.database.Cursor;

import com.eandroid.database.Column;
import com.eandroid.database.Table;
import com.eandroid.database.TableFactory;
import com.eandroid.util.EALog;

public class CursorUtils {

	private final static String TAG = "EasyDB";

	public static <T> T getEntity(Cursor cursor, Class<T> clazz){
		if(cursor == null || clazz == null)
			return null;

		try {
			Table table = TableFactory.getTable(clazz);
			int columnCount = cursor.getColumnCount();
			int count = cursor.getCount();
			if(columnCount <= 0 || count <= 0)
				return null;

			T  entity = (T) clazz.newInstance();
			cursor.moveToFirst();
			for(int i=0;i<columnCount;i++){
				String columnName = cursor.getColumnName(i);

				Column column = table.getColumnByFieldName(columnName);
				if(column!=null){
					column.setValue(entity, cursor.getString(i));
				}
			}
			return entity;
		} catch (Exception e) {
			EALog.e(TAG, e.toString());
		}
		return null;
	}

	public static <T> List<T> getListEntity(Cursor cursor, Class<T> clazz){
		if(cursor == null || clazz == null)
			return null;

		List<T> list = null;
		try {
			Table table = TableFactory.getTable(clazz);
			int columnCount = cursor.getColumnCount();
			int count = cursor.getCount();
			if(columnCount <= 0 || count <= 0)
				return null;

			list = new ArrayList<T>();
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				T  entity = (T) clazz.newInstance();
				for(int i=0;i<columnCount;i++){
					String columnName = cursor.getColumnName(i);
					Column column = table.getColumnByFieldName(columnName);
					if(column!=null){
						column.setValue(entity, cursor.getString(i));
					}
				}
				list.add(entity);
				cursor.moveToNext();
			}
		} catch (Exception e) {
			EALog.e(TAG, e.toString());
		}
		return list;
	}

}
