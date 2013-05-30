/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-5-15
 * @version 1.0.0
 */
package com.eandroid.database.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 主表
 * 对关联的从表、与从表中的哪些字段关联进行配置
 * @author Kain
 *
 */
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MainTable {

	public Class<?>[] subtables();//从表
	public String[] columns();//从表中的关联字段，需要subTables一一对应
}
