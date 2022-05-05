/*
 * Copyright (c) 2017-2020 DarkCompet. All rights reserved.
 */
package tool.compet.database

import tool.compet.core.DkCollections
import tool.compet.core.DkConst
import tool.compet.core.DkStrings
import tool.compet.core.DkUtils
import java.util.*
import kotlin.collections.ArrayList

/**
 * Base grammar for making sql sentence.
 */
abstract class OwnGrammar {
	protected abstract fun availableOperators(): Set<String?>
	fun fixGrammar(expression: MyExpression) {
		val myOperator = expression.operator?.lowercase(Locale.getDefault())
		if (!availableOperators().contains(myOperator)) {
			throw RuntimeException("Invalid operator: " + expression.operator)
		}
		// Fix null comparasion, for eg,. `user`.`name` = null -> `user`.`name` is null
		if (expression.value == null) {
			when (myOperator) {
				"=" -> {
					expression.operator = OwnConst.Companion.K_IS_NULL
				}
				"!=", "<>" -> {
					expression.operator = OwnConst.Companion.K_IS_NOT_NULL
				}
				else -> {
					DkUtils.complain("Invalid operator `%s` for null value", expression.operator)
				}
			}
		}
	}

	/**
	 * Prevent confliction between name and keyword.
	 *
	 * @param name db column name or table name, for eg,. "user.id as user_id", "user.*"
	 * @return String for eg,. "`user`.`id` as `user_id`", "`user`.*"
	 */
	fun safeName(name: String): String {
		return OwnGrammarHelper.safeName(name)
	}

	/**
	 * Wrap value to make it can valid when comparasion and prevent injection.
	 *
	 * @param value must primitive value, for eg,. "leo leo", 1.23
	 */
	fun safeValue(value: Any?): String? {
		return OwnGrammarHelper.safeValue(value)
	}

	fun safeValues(values: Iterable<*>?): List<String?> {
		return OwnGrammarHelper.safeValues(values)
	}

	/**
	 * Check valid of operator, for eg,. =, <, >, !=, ...
	 */
	fun invalidOperator(operator: String?): Boolean {
		return !availableOperators().contains(operator!!.lowercase(Locale.ROOT))
	}

	/**
	 * Generate select list.
	 *
	 * @param selections Select list
	 * @return for eg,. "*", "`id`, `name`, count(id) as `count_id`"
	 */
	fun compileSelects(selections: List<MySelection?>?): String {
		// Consider caller wanna get all fields if no selection was specified
		if (DkCollections.empty(selections)) {
			return "*"
		}
		val items: MutableList<String?> = ArrayList()
		for (selection in selections!!) {
			items.add(selection!!.compile())
		}
		return DkStrings.join(", ", items)
	}

	/**
	 * Generate insert clause.
	 *
	 * @param params Insert params
	 * @return for eg,. "insert into `user` (`id`, `name`) values ('1', 'leo leo')"
	 */
	fun compileInsertQuery(tableName: String, params: Map<String, Any?>?): String {
		if (params == null || params.size == 0) {
			return DkConst.EMPTY_STRING
		}
		val names: MutableList<String?> = ArrayList()
		val values: MutableList<String?> = ArrayList()
		for ((key, value) in params) {
			names.add(safeName(key))
			values.add(safeValue(value))
		}
		val flatten_names = DkStrings.join(", ", names)
		val flatten_values = DkStrings.join(", ", values)
		val insertSet = DkStrings.format("(%s) values (%s)", flatten_names, flatten_values)
		val query = DkStrings.format(
			"insert into %s %s",
			safeName(tableName),
			insertSet
		)
		return query.trim { it <= ' ' }
	}

	/**
	 * Generate `update` clause.
	 *
	 * @param params Update params
	 * @return for eg,. "update `user` set `id` = 1, `name` = 'leo leo'"
	 */
	fun compileUpdateQuery(tableName: String, params: Map<String, Any?>, whereClause: String?): String {
		val updateSetBuilder = StringBuilder()
		var isFirst = true
		for ((key, value1) in params) {
			if (isFirst) {
				isFirst = false
			}
			else {
				updateSetBuilder.append(", ")
			}
			val name = safeName(key)
			val value = safeValue(value1)
			updateSetBuilder.append(name).append(" = ").append(value)
		}
		val query = DkStrings.format(
			"update %s set %s %s",
			safeName(tableName),
			updateSetBuilder.toString(),
			whereClause
		)
		return query.trim { it <= ' ' }
	}

	/**
	 * Generate `where` clause.
	 *
	 * @param wheres Where condition list
	 * @return for eg,. "where id = 1 and name is null"
	 */
	fun compileWheres(wheres: List<MyExpression?>?): String {
		return compileBoolExpression("where", wheres)
	}

	/**
	 * Express bool operation from bool keywords (and, or), name, value and operator.
	 *
	 * @param prefix      `where` or `on`
	 * @param expressions List of bool operation
	 * @return for eg,. "where `name` = 'hehe' and `age` = '20'", "on `id` = '100'"
	 */
	protected fun compileBoolExpression(prefix: String, expressions: List<MyExpression?>?): String {
		// Nothing is compiled, so return empty string
		if (DkCollections.empty(expressions)) {
			return DkConst.EMPTY_STRING
		}
		val items: MutableList<String?> = ArrayList()
		for (expression in expressions!!) {
			items.add(expression!!.compile())
		}
		val clause = DkStrings.join(' ', items).trim { it <= ' ' }
		return prefix + ' ' + clause.replaceFirst("^(and|or)".toRegex(), DkConst.EMPTY_STRING).trim { it <= ' ' }
	}

	/**
	 * Generate `join` clause.
	 *
	 * @param joinInfos List of join info.
	 * @return for eg,. left join `user_detail` on `user`.`id` = `user_detail`.`user_id`
	 * right join `user_city` on `user`.`id` = `user_city`.`user_id`
	 * inner join `user_main` on `user`.`id` = `user_main`.`user_id`
	 */
	fun compileJoins(joinInfos: List<MyJoin?>?): String {
		if (DkCollections.empty(joinInfos)) {
			return DkConst.EMPTY_STRING
		}
		val joinClauses: MutableList<String?> = ArrayList()
		for (joinInfo in joinInfos!!) {
			joinClauses.add(joinInfo!!.compile())
		}
		return DkStrings.join(' ', joinClauses)
	}

	/**
	 * Generate `group by` clause.
	 *
	 * @param groupBys List of group by
	 * @return for eg,. "group by `id`, `name`, `age`"
	 */
	fun compileGroupBys(groupBys: List<MyGroupBy?>?): String {
		if (DkCollections.empty(groupBys)) {
			return DkConst.EMPTY_STRING
		}
		val groupByList: MutableList<String?> = ArrayList()
		for (groupBy in groupBys!!) {
			groupByList.add(groupBy!!.compile())
		}
		return "group by " + DkStrings.join(", ", groupByList)
	}

	/**
	 * Generate `having` clause.
	 *
	 * @param havings Having where condition list
	 * @return for eg,. "having count(id) < 10"
	 */
	fun compileHaving(havings: List<MyExpression?>?): String {
		return compileBoolExpression("having", havings)
	}

	/**
	 * Generate `order by` clause.
	 *
	 * @param orderBys List of orderby obj info
	 * @return for eg,. "order by `id` asc, `name` desc"
	 */
	fun compileOrderBys(orderBys: List<MyOrderBy?>?): String {
		if (DkCollections.empty(orderBys)) {
			return DkConst.EMPTY_STRING
		}
		val orderByList: MutableList<String?> = ArrayList()
		for (orderBy in orderBys!!) {
			orderByList.add(orderBy!!.compile())
		}
		return "order by " + DkStrings.join(", ", orderByList)
	}

	/**
	 * Generate `limit` clause.
	 *
	 * @param limit Max item count to get
	 * @return for eg,. "limit 10"
	 */
	fun compileLimit(limit: Long): String {
		return if (limit == Long.MIN_VALUE) {
			DkConst.EMPTY_STRING
		}
		else "limit $limit"
	}

	/**
	 * Generate `offset` clause.
	 *
	 * @param offset Position where take it
	 * @return for eg,. "offset 12"
	 */
	fun compileOffset(offset: Long): String {
		return if (offset == Long.MIN_VALUE) {
			DkConst.EMPTY_STRING
		}
		else "offset $offset"
	}

	/**
	 * Generate `distinct` clause.
	 *
	 * @param distinct Indicate the query has distinct
	 * @return for eg,. "distinct"
	 */
	fun compileDistinct(distinct: Boolean): String {
		return if (distinct) "distinct" else DkConst.EMPTY_STRING
	}

	/**
	 * Compile delete query.
	 */
	fun compileDeleteQuery(tableName: String, whereClause: String?): String {
		return DkStrings.format(
			"delete from %s %s",
			safeName(tableName),
			whereClause
		)
	}

	fun compileSelect(selection: MySelection): String? {
		return when (selection.type) {
			OwnConst.Companion.K_BASIC -> {
				safeName(selection.name!!)
			}
			OwnConst.Companion.K_RAW -> {
				var raw = selection.raw
				if (selection.alias != null) {
					raw = "(" + raw + ") as " + safeName(selection.alias!!)
				}
				raw
			}
			else -> {
				throw RuntimeException("Invalid type: " + selection.type)
			}
		}
	}
}
