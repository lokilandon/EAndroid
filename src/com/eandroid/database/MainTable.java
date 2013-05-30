/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-5-15
 * @version 0.1
 */
package com.eandroid.database;

import java.util.ArrayList;
import java.util.List;

import android.database.sqlite.SQLiteDatabase;

import com.eandroid.database.util.SqlBuilder;
import com.eandroid.util.DatabaseUtils;

public class MainTable extends Table {

	MainTable(String name, List<Column> columns, PrimaryKey pk) {
		super(name, columns, pk);
	}

	private SubTable[] subTables;

	public SubTable[] getSubTables() {
		return subTables;
	}

	public void setSubTables(SubTable[] subTables) {
		this.subTables = subTables;
	}

	@Override
	protected boolean doCreate(SQLiteDatabase db) {
		if(DatabaseUtils.isTableExist(name, db)){
			exist = true;
			return true;
		}
		super.doCreate(db);
		List<String> subTableTriggers = new ArrayList<String>();
		if(subTables != null && subTables.length > 0){
			for(SubTable subTable : subTables){
				if(!subTable.getTable().create(db)){
					return false;
				}
				//				if(!SystemUtils.hasJellyBean()){
				subTableTriggers.add(SqlBuilder.buildFKDeleteTriggers(this, subTable));
				//				}
			}
			for(String triggerSql : subTableTriggers){
				db.execSQL(triggerSql);
			}
		}
		return true;
	}

	public static class SubTable{

		private String associatedColumnName;
		private Table table;

		public SubTable(Table table, String associatedColumnName) {
			this.table = table;
			this.associatedColumnName = associatedColumnName;
		}

		public Column getAssociatedColumn() {
			if(table == null)
				return null;

			Column column = table.getColumnByFieldName(associatedColumnName);
			if(column == null){
				throw new IllegalStateException("SubTable - getAssociatedColumn() failed,subtable's column is not exists");
			}
			return column;
		}

		public Table getTable() {
			return table;
		}
	}


}
