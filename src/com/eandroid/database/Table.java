/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-5-15
 * @version 1.0.0
 */
package com.eandroid.database;

import java.util.ArrayList;
import java.util.List;

import android.database.sqlite.SQLiteDatabase;

import com.eandroid.database.annotation.Associate;
import com.eandroid.database.util.SQLog;
import com.eandroid.database.util.SqlBuilder;
import com.eandroid.util.DatabaseUtils;
import com.eandroid.util.EALog;
import com.eandroid.util.StringUtils;


public class Table {

	private static String TAG = "EasyDB";

	protected String name;

	protected List<Column> columns;

	protected PrimaryKey pk;

	protected List<ForeignKey> fks;

	protected List<AssociateProperty> associates;

	protected volatile boolean exist;

	public boolean create(SQLiteDatabase db){
		if(exist){
			return true;
		}
		if(DatabaseUtils.isTableExist(name, db)){
			exist = true;
			return true;
		}
		db.beginTransaction();
		try{
			if(doCreate(db)){
				db.setTransactionSuccessful();
				exist = true;
				return true;
			}else{
				return false;
			}
		}catch (Exception e) {
			EALog.e(TAG, e.toString());
			return false;
		}finally{
			db.endTransaction();
		}
	}

	protected boolean doCreate(SQLiteDatabase db){
		if(DatabaseUtils.isTableExist(name, db)){
			exist = true;
			return true;
		}
		String sql = SqlBuilder.buildCreateTableSql(this);
		SQLog.d(TAG, sql);
		db.execSQL(sql);

		List<String> triggers = new ArrayList<String>();
		if(fks != null && !fks.isEmpty()){
			for(ForeignKey fk:fks){
				if(!fk.getMainTable().create(db)){
					return false;
				}
				//				if(!SystemUtils.hasJellyBean()){
				triggers.add(SqlBuilder.buildFKInsertTriggers(fk, this));
				triggers.add(SqlBuilder.buildFKUpdateTriggers(fk, this));
				//				}
			}
			if(!triggers.isEmpty()){
				for(String triggerSql : triggers){
					db.execSQL(triggerSql);
				}
			}
		}
		return true;
	}

	public boolean drop(SQLiteDatabase db){
		String sql = SqlBuilder.buildDropTableSql(this);
		SQLog.d(TAG, sql);
		try {
			db.execSQL(sql);
		} catch (Exception e) {
			return false;
		}
		exist = false;
		return true;
	}


	Table(String name,List<Column> columns,PrimaryKey pk){
		this.name = name;
		this.columns = columns;
		this.pk = pk;
	}

	public AssociateProperty getAssociateByForeignKey(ForeignKey fk){
		if(associates == null || associates.isEmpty()){
			return null;
		}
		if(fk == null)
			return null;

		for(AssociateProperty ap: associates){
			if(ap.getType() == Associate.TYPE.ONE_TO_MANY){
				continue;
			}
			if(ap.getPkNameOfOneInMany().equals(fk.getName()))
				return ap;
		}
		return null;
	}

	public Column getColumnByFieldName(String fieldName){
		if(StringUtils.isEmpty(fieldName)){
			return null;
		}

		for(Column column: columns){
			if(column.getName().equals(fieldName))
				return column;
		}
		for(ForeignKey column: fks){
			if(column.getName().equals(fieldName))
				return column;
		}
		if(pk.getName().equals(fieldName))
			return pk;
		return null;

	}

	public Column getColumnByAssociate(AssociateProperty associateProperty){
		if(associateProperty == null){
			return null;
		}

		for(Column column: columns){
			if(column.getName().equals(associateProperty.getPkNameOfOneInMany()))
				return column;
		}
		for(ForeignKey column: fks){
			if(column.getName().equals(associateProperty.getPkNameOfOneInMany()))
				return column;
		}
		if(pk.getName().equals(associateProperty.getPkNameOfOneInMany()))
			return pk;
		return null;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Column> getColumns() {
		return columns;
	}

	public void setColumns(List<Column> columns) {
		this.columns = columns;
	}

	public PrimaryKey getPk() {
		return pk;
	}

	public void setPk(PrimaryKey pk) {
		this.pk = pk;
	}

	public List<ForeignKey> getFks() {
		return fks;
	}

	public void setFk(List<ForeignKey> fks) {
		this.fks = fks;
	}

	public List<AssociateProperty> getAssociates() {
		return associates;
	}

	public void setAssociates(List<AssociateProperty> associates) {
		this.associates = associates;
	}

	public boolean isExist() {
		return exist;
	}

}
