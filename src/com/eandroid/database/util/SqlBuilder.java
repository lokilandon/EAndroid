/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-5-21
 * @version 1.0.0
 */
package com.eandroid.database.util;

import java.util.Iterator;
import java.util.List;

import android.content.ContentValues;

import com.eandroid.database.Column;
import com.eandroid.database.ForeignKey;
import com.eandroid.database.MainTable;
import com.eandroid.database.PrimaryKey;
import com.eandroid.database.SQL;
import com.eandroid.database.Table;
import com.eandroid.database.TableFactory;
import com.eandroid.database.MainTable.SubTable;
import com.eandroid.util.StringUtils;
import com.eandroid.util.SystemUtils;

public class SqlBuilder {

	public static String buildCreateTableSql(Table t){
		String name = t.getName();
		PrimaryKey pk = t.getPk();
		List<ForeignKey> fks = t.getFks();
		List<Column> columns = t.getColumns();
		if(StringUtils.isEmpty(name))
			throw new IllegalStateException("Table create() - failed, table name is empty!");

		StringBuilder createTableBuilder = new StringBuilder();
		createTableBuilder.append("CREATE TABLE IF NOT EXISTS ").append(name).append(" (");
		if(pk != null){
			String columnName = pk.getName();
			String columnType = "TEXT";
			boolean dataTypeCanAutoIncrement = false;
			if(pk.isIntegerDataType()){
				columnType = "INTEGER";
				dataTypeCanAutoIncrement = true;
			}
			createTableBuilder.append(columnName).append(" ").append(columnType)
			.append(" PRIMARY KEY ");
			//主键为int字段时，不能设置autoincrement
			if(dataTypeCanAutoIncrement && pk.isAutoIncrement()){
				createTableBuilder.append(" AUTOINCREMENT ");
			}else{
				//sqlite允许主键为空字段，如果是integer类型的主键，当插入空值时，会自动取当前列最大的值进行插入（设置和不设置autoincrement时，规则会有不同）
				//但如果是text类型的主键，插入空值就会导致表中有一条主键为空的记录，并且允许插入多条主键为空的记录，因此这里判断如果是text类型，增加NOT NULL关键字
				createTableBuilder.append(" NOT NULL ");
			}
			createTableBuilder.append(",");
		}
		if(columns != null && columns.size() > 0 ){
			for(Column column : columns){
				String columnName = column.getName();
				String columnType = " TEXT";
				createTableBuilder.append(columnName).append(columnType);
				if(column.isUnique()){
					//sqlite中，虽然对某一列设置了unique（语义上分析应该不允许插入空值），但还是允许插入空值，并且不会对空值进行唯一性判断（和上面主键问题一样）
					//因此这里如果设置了unique，同时设置上not null，解决此问题
					createTableBuilder.append(" UNIQUE NOT NULL");
				}else if(column.isNotNull()){
					createTableBuilder.append(" NOT NULL");
				}
				if(column.hasDefaultValue()){
					createTableBuilder.append(" DEFAULT ").append(column.getDefaultValue());
				}
				createTableBuilder.append(",");
			}
		}
		if(fks != null && !fks.isEmpty()){
			for(ForeignKey fk:fks){
				String columnName = fk.getName();
				Table mainTable = fk.getMainTable();
				String mainTableName = mainTable.getName();
				String columnType = "TEXT";
				createTableBuilder.append(columnName).append(" ").append(columnType);
				if(fk.isUnique()){
					createTableBuilder.append(" UNIQUE NOT NULL");
				}else if(fk.isNotNull()){
					createTableBuilder.append(" NOT NULL");
				}
				if(fk.hasDefaultValue()){
					createTableBuilder.append(" DEFAULT ").append(fk.getDefaultValue());
				}

				if(SystemUtils.hasJellyBean()){
					createTableBuilder.append(" REFERENCES ").append(mainTableName)
					.append(" (").append(fk.getMainTable().getPk().getName()).append(")");
				}
				createTableBuilder.append(",");
			}
		}
		createTableBuilder.deleteCharAt(createTableBuilder.length() - 1);
		createTableBuilder.append(" )");
		return createTableBuilder.toString();
	}

	public static String buildFKInsertTriggers(ForeignKey fk,Table table){
		StringBuilder triggersBuilder = new StringBuilder();
		String mainTableName = fk.getMainTable().getName();
		PrimaryKey mainTablePk = fk.getMainTable().getPk();
		String fkName = fk.getName();
		String tableName = table.getName();

		triggersBuilder.append("CREATE TRIGGER FK_INSERT_")
		.append(fk.getName()).append("_").append(tableName)
		.append(" BEFORE INSERT ON ").append(tableName)
		.append(" FOR EACH ROW WHEN NEW.").append(fkName).append(" IS NOT NULL")
		.append(" BEGIN")
		.append(" SELECT RAISE(ROLLBACK,'fk column ").append(fkName).append(" is null").append(" in ").append(mainTableName).append("')")
		.append(" WHERE (SELECT ").append(mainTablePk.getName()).append(" FROM ").append(mainTableName)
		.append(" WHERE ").append(mainTablePk.getName()).append(" = NEW.").append(fkName)
		.append(") is NULL").append(";")
		.append(" END");
		return triggersBuilder.toString();
	}

	public static String buildFKUpdateTriggers(ForeignKey fk,Table table){
		StringBuilder triggersBuilder = new StringBuilder();
		String mainTableName = fk.getMainTable().getName();
		PrimaryKey mainTablePk = fk.getMainTable().getPk();
		String fkName = fk.getName();
		String tableName = table.getName();

		triggersBuilder.append("CREATE TRIGGER FK_UPDATE_")
		.append(fk.getName()).append("_").append(tableName)
		.append(" BEFORE UPDATE ON ").append(tableName)
		.append(" FOR EACH ROW WHEN NEW.").append(fkName).append(" IS NOT NULL")
		.append(" BEGIN")
		.append(" SELECT RAISE(ROLLBACK,'fk column ").append(fkName).append(" is null").append(" in ").append(mainTableName).append("')")
		.append(" WHERE (SELECT ").append(mainTablePk.getName()).append(" FROM ").append(mainTableName)
		.append(" WHERE ").append(mainTablePk.getName()).append(" = NEW.").append(fkName)
		.append(") is NULL").append(";")
		.append(" END");
		return triggersBuilder.toString();
	}

	public static String buildFKDeleteTriggers(MainTable mainTable,SubTable subTable){
		StringBuilder triggersBuilder = new StringBuilder();
		String mainTableName = mainTable.getName();
		Column associateColumn = subTable.getAssociatedColumn();
		String mainTablePkName = mainTable.getPk().getName();

		triggersBuilder.append("CREATE TRIGGER FK_DELETE_")
		.append(associateColumn.getName()).append("_").append(mainTableName)
		.append(" BEFORE DELETE ON ").append(mainTableName)
		.append(" FOR EACH ROW BEGIN")
		.append(" DELETE FROM ").append(subTable.getTable().getName())
		.append(" WHERE ").append(associateColumn.getName()).append(" = OLD.").append(mainTablePkName).append(";")
		.append(" END");
		return triggersBuilder.toString();
	}

	public static ContentValues generateContentValues(Object entity){
		if(entity == null)
			return null;

		Table table = TableFactory.getTable(entity.getClass());

		ContentValues cv = new ContentValues();

		PrimaryKey pk = table.getPk();
		//如果主键为long或者int 基础类型，无法通过是否为null判断用户是否主动的设置了主键
		//因此判断当为0时，代表用户未主动设置主键
		//只对主键做了判断，外键和普通的列类型，为防止意外不做判断。
		//因此如果其他字段设置为int或者long类型，插入数据库时如果没有设置过任何值，会以0进行插入
		if(!pk.isNullValue(entity)){
			cv.put(pk.getName(), pk.getValue(entity).toString());
		}

		List<ForeignKey> fks = table.getFks();
		if(fks != null && !fks.isEmpty()){
			for(ForeignKey fk : fks){
				if(fk.getValue(entity) != null)
					cv.put(fk.getName(), fk.getValue(entity).toString());
			}
		}

		List<Column> columns = table.getColumns();
		if(columns != null && !columns.isEmpty()){
			Iterator<Column> columnIterator = columns.iterator();
			while (columnIterator.hasNext()) {
				Column column = columnIterator.next();
				if(column.getValue(entity) != null)
					cv.put(column.getName(), column.getValue(entity).toString());
			}
		}
		return cv;
	}

	public static SQL buildUpdateSql(Object entity){
		if(entity == null)
			return null;

		Table table = TableFactory.getTable(entity.getClass());
		PrimaryKey pk = table.getPk();
		String where = pk.getName() + "=?";
		SQL sql = buildUpdateSql(entity, where);
		if(sql != null){
			sql.addBindArg(pk.getValue(entity));
		}
		return sql;
	}

	public static SQL buildUpdateSql(Object entity,String where){
		if(entity == null)
			return null;

		Table table = TableFactory.getTable(entity.getClass());

		SQL sql = new SQL();
		StringBuilder sqlBuider = new StringBuilder();
		sqlBuider.append("UPDATE ").append(table.getName())
		.append(" SET ");

		List<ForeignKey> fks = table.getFks();
		if(fks != null && !fks.isEmpty()){
			for(ForeignKey fk : fks){
				sqlBuider.append(fk.getName()).append(" = ?,");
				sql.addBindArg(fk.getValue(entity));
			}
		}

		List<Column> columns = table.getColumns();
		if(columns != null && !columns.isEmpty()){
			Iterator<Column> columnIterator = columns.iterator();
			while (columnIterator.hasNext()) {
				Column column = columnIterator.next();
				sqlBuider.append(column.getName()).append(" = ?,");
				sql.addBindArg(column.getValue(entity));
			}
		}

		if(sqlBuider.charAt(sqlBuider.length() - 1) == ','){
			sqlBuider.deleteCharAt(sqlBuider.length() - 1);
		}

		sqlBuider.append(" WHERE ").append(where);
		sql.setSql(sqlBuider.toString());
		return sql;
	}
	
	public static SQL buildSelectSql(Class<?> clazz,Object id){
		if(clazz == null || id == null)
			return null;

		Table table = TableFactory.getTable(clazz);
		String where = table.getPk().getName() + "=?";
		SQL sql = buildSelectSql(clazz, where, null, null);
		if(sql != null)
			sql.addBindArg(id.toString());
		return sql;
	}
	
	public static SQL buildSelectSql(Class<?> clazz,String where,String orderBy,String limit){
		if(clazz == null)
			return null;

		Table table = TableFactory.getTable(clazz);

		SQL sql = new SQL();
		StringBuilder sqlBuider = new StringBuilder();
		sqlBuider.append("SELECT ");
		PrimaryKey pk = table.getPk();
		sqlBuider.append(pk.getName()).append(",");
		
		List<ForeignKey> fks = table.getFks();
		if(fks != null && !fks.isEmpty()){
			for(ForeignKey fk : fks){
				sqlBuider.append(fk.getName()).append(",");
			}
		}

		List<Column> columns = table.getColumns();
		if(columns != null && !columns.isEmpty()){
			Iterator<Column> columnIterator = columns.iterator();
			while (columnIterator.hasNext()) {
				Column column = columnIterator.next();
				sqlBuider.append(column.getName()).append(",");
			}
		}
		sqlBuider.deleteCharAt(sqlBuider.length() - 1);
		sqlBuider.append(" FROM ").append(table.getName());
		if(StringUtils.isNotEmpty(where))
			sqlBuider.append(" WHERE ").append(where);
		if(StringUtils.isNotEmpty(orderBy))
			sqlBuider.append(" ORDERBY ").append(orderBy);
		if(StringUtils.isNotEmpty(limit))
			sqlBuider.append(" LIMIT ").append(limit);
		sql.setSql(sqlBuider.toString());
		return sql;
	}


	public static SQL buildDeleteSql(Object entity){
		if(entity == null)
			return null;

		Table table = TableFactory.getTable(entity.getClass());

		PrimaryKey pk = table.getPk();
		String where = pk.getName() + "=?";
		SQL sql = buildDeleteSql(entity.getClass(),where);
		if(sql != null){
			sql.addBindArg(pk.getValue(entity));
		}
		return sql;
	}

	public static SQL buildDeleteSql(Object entity,Object id){
		if(entity == null || id == null)
			return null;

		Table table = TableFactory.getTable(entity.getClass());

		PrimaryKey pk = table.getPk();
		String where = pk.getName() + "=?";
		SQL sql = buildDeleteSql(entity.getClass(),where);
		if(sql != null){
			sql.addBindArg(id);
		}
		return sql;
	}
	
	public static SQL buildDeleteSql(Class<?> clazz,String where){
		if(clazz == null)
			return null;

		Table table = TableFactory.getTable(clazz);

		SQL sql = new SQL();
		StringBuilder sqlBuider = new StringBuilder();
		sqlBuider.append("DELETE FROM ").append(table.getName());
		sqlBuider.append(" WHERE ").append(where);
		sql.setSql(sqlBuider.toString());
		return sql;
	}
	
	public static String buildDropTableSql(Table table){
		if(table == null)
			return null;

		StringBuilder sqlBuider = new StringBuilder();
		sqlBuider.append("DROP TABLE ").append(table.getName());
		return sqlBuider.toString();
	}
}
