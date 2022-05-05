/*
 * Copyright (c) 2017-2021 DarkCompet. All rights reserved.
 */
package tool.compet.database

/**
 * This annotation is mainly used for mapping between DB-column and POJO-field.
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class DkColumnInfo(
	/**
	 * Name of associated mapping column in database.
	 * Column name can be: table real column OR selected alias name.
	 */
	val name: String,
	/**
	 * Indicate the mapping column is table-column or not.
	 *
	 * @return TRUE iff this is table-column. Otherwise, it is maybe alias name when select.
	 */
	val isTableColumn: Boolean = false
)
