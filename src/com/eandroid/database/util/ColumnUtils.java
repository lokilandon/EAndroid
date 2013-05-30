/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-5-16
 * @version 1.0.0
 */
package com.eandroid.database.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Date;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.eandroid.database.AssociateProperty;
import com.eandroid.database.Column;
import com.eandroid.database.annotation.Associate;
import com.eandroid.database.annotation.DefaultValue;
import com.eandroid.database.annotation.ForeignKey;
import com.eandroid.database.annotation.NotNull;
import com.eandroid.database.annotation.PrimaryKey;
import com.eandroid.database.annotation.Transient;
import com.eandroid.database.annotation.Unique;
import com.eandroid.database.annotation.Associate.TYPE;
import com.eandroid.util.StringUtils;



public class ColumnUtils {

	public static boolean isPrimaryKey(Field field){
		if(field == null)
			return false;
		return field.isAnnotationPresent(PrimaryKey.class);
	}

	public static boolean isForeignKey(Field field){
		if(field == null)
			return false;
		return field.isAnnotationPresent(ForeignKey.class);
	}

	public static boolean isTransient(Field field){
		if(field == null)
			return false;
		return field.isAnnotationPresent(Transient.class);
	}

	public static boolean isUnique(Field field){
		if(field == null)
			return false;
		return field.isAnnotationPresent(Unique.class);
	}

	public static boolean isNotNull(Field field){
		if(field == null)
			return false;
		return field.isAnnotationPresent(NotNull.class);
	}

	public static boolean hasDefaultValue(Field field){
		if(field == null)
			return false;
		return field.isAnnotationPresent(DefaultValue.class);
	}

	public static boolean isAssociate(Field field){
		if(field == null)
			return false;
		return field.isAnnotationPresent(Associate.class);
	}

	public static Column getColumn(Field field,Class<?> clazz) throws NoSuchMethodException{
		if(field == null)
			return null;

		if(!field.isAccessible())
			field.setAccessible(true);
		Column column = new Column(field.getName(),
				ColumnUtils.isUnique(field),
				ColumnUtils.reflectGetMethod(field, clazz),
				ColumnUtils.reflectSetMethod(field, clazz),
				field.getType());
		if(isNotNull(field)){
			column.setNotNull(true);
		}
		if(hasDefaultValue(field)){
			DefaultValue defaultValue = field.getAnnotation(DefaultValue.class);
			column.setDefaultValue(defaultValue.value());
		}
		return column;
	}

	public static com.eandroid.database.PrimaryKey getPrimaryKey(Field field,Class<?> clazz)
			throws NoSuchMethodException{
		if(field == null)
			return null;
		
		boolean autoIncrement = true;
		PrimaryKey pkAnnotation = field.getAnnotation(PrimaryKey.class);
		if(pkAnnotation != null){
			autoIncrement = pkAnnotation.autoincrement();
		}
		if(!isValidColumnDataType(field)){
			throw new IllegalStateException("ColumnUtils getPrimaryKey - fail,illegal data type");
		}
		if(!field.isAccessible())
			field.setAccessible(true);
		com.eandroid.database.PrimaryKey	pk = new com.eandroid.database.PrimaryKey(field.getName(),
				true,
				ColumnUtils.reflectGetMethod(field, clazz),
				ColumnUtils.reflectSetMethod(field, clazz),
				field.getType(),
				autoIncrement);
		pk.setNotNull(true);
		pk.setDefaultValue(null);
		return pk;
	}
	
	public static com.eandroid.database.ForeignKey getForeignKey(Field field)
			throws NoSuchMethodException{
		if(field == null){
			return null;
		}
		ForeignKey annotation = field.getAnnotation(ForeignKey.class);
		if(annotation == null)
			return null;
		if(annotation.maintable() == null){
			throw new IllegalStateException("ColumnUtils - getForeignKey() failed," +
					" maintable is empty.");
		}
		if(!isValidAssociateTableClazz(annotation.maintable())){
			throw new IllegalStateException("ColumnUtils - getForeignKey() failed," +
					" foreign key's maintable is not table class.");
		}
		if(!ColumnUtils.isValidColumnDataType(field)){
			throw new IllegalStateException("ColumnUtils - getForeignKey() failed,illegal data type");
		}
		if(!field.isAccessible())
			field.setAccessible(true);
		com.eandroid.database.ForeignKey fk =
				new com.eandroid.database.ForeignKey(field.getName(),
						isUnique(field), 
						reflectGetMethod(field, field.getDeclaringClass()),
						reflectSetMethod(field, field.getDeclaringClass()),
						field.getType(), 
						annotation.maintable());
		if(isNotNull(field)){
			fk.setNotNull(true);
		}
		if(hasDefaultValue(field)){
			DefaultValue defaultValue = field.getAnnotation(DefaultValue.class);
			fk.setDefaultValue(defaultValue.value());
		}
		return fk;
	}

	/**
	 * 获取字段所代表的关联 AssociatteProperty
	 * @param field
	 * @param clazz
	 * @return
	 * @throws NoSuchMethodException
	 */
	public static AssociateProperty getAssociate(Field field) throws NoSuchMethodException {
		Associate annotation = field.getAnnotation(Associate.class);
		if(annotation != null){
			Class<?> clazz = field.getDeclaringClass();
			TYPE type = annotation.type();
			String pkNameOfOneInMany = annotation.pkOfOneInMany();
			Class<?> beAssociatedTableClazz = annotation.table();

			//判断关联的列字段是否为空
			//多对一关系时，该字段应指向关联表在本表中的所对应的主键id字段
			//一对多关系时，该字段应指向本表中的主键id在关联表中所对应的字段
			if(StringUtils.isEmpty(pkNameOfOneInMany)){
				throw new IllegalStateException("ColumnUtils - getAssociate() failed," +
						" associate's column is empty.");
			}

			//判断注解中是否显示设置了关联表，如果设置了则不能与字段类型所表示的关联表类型不同，除非字段类型为Map
			if(beAssociatedTableClazz != null && beAssociatedTableClazz != Object.class){
				if(getBeAssociatedTableClass(field) != beAssociatedTableClazz){
					throw new IllegalStateException("ColumnUtils - getAssociate() failed," +
							" associate's table is not same with the associate field's class.");
				}
			}else{
				//如果注解中未设置关联表，则通过字段反射获取他表示的关联表类型
				beAssociatedTableClazz = getBeAssociatedTableClass(field);
			}

			//对关联表类型进行检查，是否合法
			if(!isValidAssociateTableClazz(beAssociatedTableClazz)){
				throw new IllegalStateException("ColumnUtils - getAssociate() failed," +
						" associate's table is not table class.");
			}

			//对一对多，多对一关系分别进行检查，看参数是否符合要求
			if(type == TYPE.MANY_TO_ONE){
				if(!isValidManyToOneAssociateDataField(field)){
					throw new IllegalStateException("ColumnUtils - getAssociate() failed," +
							" the associate ("+ field.getName()+") is not many to one type.");
				}else{
					Field associateColumnField = null;
					try {
						associateColumnField = clazz.getDeclaredField(pkNameOfOneInMany);
						if(isForeignKey(associateColumnField)){
							ForeignKey fkAnnotation = associateColumnField.getAnnotation(ForeignKey.class);
							if(beAssociatedTableClazz != fkAnnotation.maintable()){
								throw new IllegalStateException("ColumnUtils - getForeignKey() failed," +
										" foreign key's maintable is not same with the associate field's class.");
							}
						}
						if(!isValidColumnDataType(associateColumnField)){
							throw new IllegalStateException("ColumnUtils - getAssociate() failed," +
									" associate's column is not valid column data type.");
						}
					} catch (NoSuchFieldException e) {
						throw new IllegalStateException("ColumnUtils - getAssociate() failed," +
								" can not found associate's column（"+field.getName()+") ,please check it. "+e);
					}
				}
			}else if(type == TYPE.ONE_TO_MANY){
				if(!isValidOneToManyAssociateDataField(field)){
					throw new IllegalStateException("ColumnUtils - getAssociate() failed," +
							" the associate ("+ field.getName()+") is not one to many type.");
				}
			}else{
				throw new IllegalStateException("ColumnUtils - getAssociate() failed," +
						" unknown associate type.");
			}

			if(!field.isAccessible())
				field.setAccessible(true);
			AssociateProperty associate = new AssociateProperty(type,
					pkNameOfOneInMany,
					beAssociatedTableClazz,
					ColumnUtils.reflectGetMethod(field, clazz),
					ColumnUtils.reflectSetMethod(field, clazz),
					field);
			return associate;

		}
		return  null;
	}

	public static boolean isValidColumnDataType(Field field){
		if(field == null)
			return false;

		return !isTransient(field) 
				&& isValidColumnDataType(field.getType());
	}

	public static boolean isValidColumnDataType(Class<?> clazz){
		if(clazz == null)
			return false;

		if(clazz == Integer.class
				|| clazz == Double.class
				|| clazz == Float.class
				|| clazz == Long.class
				|| clazz == String.class
				|| clazz == Character.class
				|| clazz == Date.class
				|| clazz == java.util.Date.class
				|| clazz == Boolean.class
				|| clazz == Short.class
				|| clazz == Byte.class
				|| clazz.isPrimitive()){
			return true;
		}
		return false;
	}

	public static boolean isValidManyToOneAssociateDataField(Field field){
		if(field == null)
			return false;

		Class<?> clazz = field.getType();
		if(clazz.isInterface()
				|| clazz.isArray()
				|| clazz.isEnum()
				|| clazz.isAnnotation()
				|| isValidColumnDataType(field)
				|| Collection.class.isAssignableFrom(clazz)
				|| Map.class.isAssignableFrom(clazz)){
			return false;
		}	
		return true;
	}

	public static boolean isValidOneToManyAssociateDataField(Field field){
		if(field == null)
			return false;

		if(List.class.isAssignableFrom(field.getType())){
			return true;
		}
		return false;
	}

	public static boolean isValidAssociateTableClazz(Class<?> clazz){
		if(clazz == null)
			return false;

		if(clazz.isInterface()
				|| clazz.isArray()
				|| clazz.isEnum()
				|| clazz.isAnnotation()
				|| isValidColumnDataType(clazz)
				|| Collection.class.isAssignableFrom(clazz)
				|| Map.class.isAssignableFrom(clazz)){
			return false;
		}
		return true;
	}

	public static Class<?> getBeAssociatedTableClass(Field field){
		Type type = field.getGenericType();
		if(type instanceof ParameterizedType){
			ParameterizedType pType = (ParameterizedType)type;
			if(pType.getActualTypeArguments().length == 0){
				return (Class<?>)pType.getRawType();
			}else if(pType.getActualTypeArguments().length == 1){
				return (Class<?>)pType.getActualTypeArguments()[0];
			}else{
				return null;
			}
		}else{
			return field.getType();
		}
	}

	public static boolean isBaseColumnData(Field field){
		if(field == null)
			return false;

		if(isValidColumnDataType(field) 
				&& !isPrimaryKey(field) && !isForeignKey(field)){
			return true;
		}
		return false;
	}

	public static Method reflectGetMethod(Field field,Class<?> clazz) throws NoSuchMethodException{
		String methodName;
		if(field.getType() == Boolean.class || field.getType() == boolean.class){
			methodName = "is"+field.getName().substring(0, 1).toUpperCase()
					+field.getName().substring(1);
		}else{
			methodName = "get"+field.getName().substring(0, 1).toUpperCase()
					+field.getName().substring(1);
		}
		return clazz.getMethod(methodName, (Class<?>[])null);
	}

	public static Method reflectSetMethod(Field field,Class<?> clazz) throws NoSuchMethodException{
		String methodName;
		methodName = "set"+field.getName().substring(0, 1).toUpperCase()
				+field.getName().substring(1);
		return clazz.getMethod(methodName, field.getType());
	}

	public static boolean isInheritedField(Field field,boolean samePackage){
		if(samePackage){
			if(!Modifier.isPrivate(field.getModifiers()))
				return true;
		}else{
			int searchModifers = 0x00000000;
			searchModifers |= Modifier.PUBLIC;
			searchModifers |= Modifier.PROTECTED;
			if((field.getModifiers() & searchModifers) != 0){
				return true;
			}
		}
		return false;
	}

	public static boolean isDefaultModifier(Field field){
		int searchModifers = 0x00000000;
		searchModifers |= Modifier.PUBLIC;
		searchModifers |= Modifier.PROTECTED;
		searchModifers |= Modifier.PRIVATE;
		if((field.getModifiers() & searchModifers) != 0)
			return false;
		return true;
	}
}
