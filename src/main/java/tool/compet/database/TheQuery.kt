/*
 * Copyright (c) 2017-2020 DarkCompet. All rights reserved.
 */
package tool.compet.database

import tool.compet.core.DkConst
import tool.compet.core.DkRunner1
import tool.compet.core.DkStrings

/**
 * This is base query builder for various query language as sqlite, mysql, postgresql...
 * It receives a database connection, provides a query execution.
 * Caller can build and execute a query from this instead of manual sql.
 *
 * @param <M> Query result model.
 *
 * @author darkcompet
 */
// Package privated (this class is open for usage, not for create)
abstract class TheQuery<M>(
	protected var connection: TheConnection, // db connection for CRUD
	protected var grammar: OwnGrammar, // grammar for the sql
	protected var tableName: String, // db table name
	protected var rowType: Class<M> // query result model
) {
	// endregion CRUD
	// Enable this to reduce mistake when query
	var isEnableStrictMode = true
		protected set

	//
	// For query builder
	//
	protected var nullableSelects: MutableList<MySelection>? = null
	protected var distinct = false
	protected var nullableJoins: MutableList<MyJoin>? = null
	protected var nullableWheres: MutableList<MyExpression>? = null
	protected var nullableOrderBys: MutableList<MyOrderBy>? = null
	protected var nullableGroupBys: MutableList<MyGroupBy>? = null
	protected var nullableHavings: MutableList<MyExpression>? = null
	protected var limit = Long.MIN_VALUE
	protected var offset = Long.MIN_VALUE

	// region Build query
	fun table(tableName: String): TheQuery<M> {
		this.tableName = tableName
		return this
	}

	fun <MM> rowType(rowType: Class<MM>): TheQuery<MM> {
		this.rowType = rowType as Class<M>
		return this as TheQuery<MM>
	}

	protected fun selects(): MutableList<MySelection> {
		return if (nullableSelects != null) nullableSelects!!
		else ArrayList<MySelection>().also {
			nullableSelects = it
		}
	}

	protected fun joins(): MutableList<MyJoin> {
		return if (nullableJoins != null) nullableJoins!! else ArrayList<MyJoin>().also { nullableJoins = it }
	}

	protected fun wheres(): MutableList<MyExpression> {
		return if (nullableWheres != null) nullableWheres!! else ArrayList<MyExpression>().also { nullableWheres = it }
	}

	protected fun orderBys(): MutableList<MyOrderBy> {
		return if (nullableOrderBys != null) nullableOrderBys!!
		else ArrayList<MyOrderBy>().also {
			nullableOrderBys = it
		}
	}

	protected fun groupBys(): MutableList<MyGroupBy> {
		return if (nullableGroupBys != null) nullableGroupBys!!
		else ArrayList<MyGroupBy>().also {
			nullableGroupBys = it
		}
	}

	protected fun havings(): MutableList<MyExpression> {
		return if (nullableHavings != null) nullableHavings!!
		else ArrayList<MyExpression>().also {
			nullableHavings = it
		}
	}

	/**
	 * Select one or multiple columns.
	 */
	fun select(vararg names: String?): TheQuery<M> {
		for (name in names) {
			selects().add(MySelection(grammar, OwnConst.Companion.K_BASIC, name))
		}
		return this
	}

	/**
	 * @param subQuery String for eg,. "count(id) as user_id"
	 * @return this
	 */
	@JvmOverloads
	fun selectRaw(subQuery: String?, alias: String? = null): TheQuery<M> {
		selects().add(MySelection(grammar, OwnConst.Companion.K_RAW, subQuery, alias))
		return this
	}

	fun distinct(): TheQuery<M> {
		distinct = true
		return this
	}

	/**
	 * Left join with another table on `first = second` condition.
	 */
	fun leftJoin(joinTable: String, first: String, second: String): TheQuery<M> {
		return registerSingleJoin(OwnConst.Companion.K_LEFT, joinTable, first, "=", second)
	}

	fun leftJoin(joinTable: String, first: String, operator: String, second: String): TheQuery<M> {
		return registerSingleJoin(OwnConst.Companion.K_LEFT, joinTable, first, operator, second)
	}

	fun rightJoin(joinTable: String, first: String, second: String): TheQuery<M> {
		return registerSingleJoin(OwnConst.Companion.K_RIGHT, joinTable, first, "=", second)
	}

	fun rightJoin(joinTable: String, first: String, operator: String, second: String): TheQuery<M> {
		return registerSingleJoin(OwnConst.Companion.K_RIGHT, joinTable, first, operator, second)
	}

	fun join(joinTable: String, first: String, second: String): TheQuery<M> {
		return registerSingleJoin(OwnConst.Companion.K_INNER, joinTable, first, "=", second)
	}

	fun join(joinTable: String, first: String, operator: String, second: String): TheQuery<M> {
		return registerSingleJoin(OwnConst.Companion.K_INNER, joinTable, first, operator, second)
	}

	fun leftJoin(joinTable: String, joinerCallback: DkRunner1<MyJoiner>): TheQuery<M> {
		return registerMultipleJoin(OwnConst.Companion.K_LEFT, joinTable, joinerCallback)
	}

	fun rightJoin(joinTable: String, joinerCallback: DkRunner1<MyJoiner>): TheQuery<M> {
		return registerMultipleJoin(OwnConst.Companion.K_RIGHT, joinTable, joinerCallback)
	}

	fun join(joinTable: String, joinerCallback: DkRunner1<MyJoiner>): TheQuery<M> {
		return registerMultipleJoin(OwnConst.Companion.K_INNER, joinTable, joinerCallback)
	}

	private fun registerMultipleJoin(
		joinType: String,
		joinTable: String,
		joinerCallback: DkRunner1<MyJoiner>
	): TheQuery<M> {
		// Send joiner to callback and receive condition from callbacker
		val joiner = MyJoiner(grammar)
		joinerCallback.run(joiner)
		joins().add(MyJoin(grammar, joinType, joinTable, joiner))
		return this
	}

	private fun registerSingleJoin(
		joinType: String,
		joinTable: String,
		first: String,
		operator: String,
		second: String
	): TheQuery<M> {
		joins().add(MyJoin(grammar, joinType, joinTable, first, operator, second))
		return this
	}

	/**
	 * This is equal where, short where for equal.
	 *
	 * @param name  String table column name
	 * @param value Object target value which matches with value of the field
	 */
	fun where(name: String, value: Any?): TheQuery<M> {
		return registerExpression(
			MyExpression(
				grammar,
				OwnConst.Companion.K_AND,
				OwnConst.Companion.K_BASIC,
				name,
				OwnConst.Companion.K_EQ,
				value
			), wheres()
		)
	}

	fun orWhere(name: String, value: Any?): TheQuery<M> {
		return registerExpression(
			MyExpression(
				grammar,
				OwnConst.Companion.K_OR,
				OwnConst.Companion.K_BASIC,
				name,
				OwnConst.Companion.K_EQ,
				value
			), wheres()
		)
	}

	fun where(name: String, operator: String, value: Any?): TheQuery<M> {
		return registerExpression(
			MyExpression(
				grammar,
				OwnConst.Companion.K_AND,
				OwnConst.Companion.K_BASIC,
				name,
				operator,
				value
			), wheres()
		)
	}

	fun orWhere(name: String, operator: String, value: Any?): TheQuery<M> {
		return registerExpression(
			MyExpression(
				grammar,
				OwnConst.Companion.K_OR,
				OwnConst.Companion.K_BASIC,
				name,
				operator,
				value
			), wheres()
		)
	}

	fun whereNull(name: String): TheQuery<M> {
		return registerExpression(
			MyExpression(
				grammar,
				OwnConst.Companion.K_AND,
				OwnConst.Companion.K_NULL,
				name,
				OwnConst.Companion.K_IS_NULL
			), wheres()
		)
	}

	fun orWhereNull(name: String): TheQuery<M> {
		return registerExpression(
			MyExpression(
				grammar,
				OwnConst.Companion.K_OR,
				OwnConst.Companion.K_NULL,
				name,
				OwnConst.Companion.K_IS_NULL
			), wheres()
		)
	}

	fun whereNotNull(name: String): TheQuery<M> {
		return registerExpression(
			MyExpression(
				grammar,
				OwnConst.Companion.K_AND,
				OwnConst.Companion.K_NOT_NULL,
				name,
				OwnConst.Companion.K_IS_NOT_NULL
			), wheres()
		)
	}

	fun orWhereNotNull(name: String): TheQuery<M> {
		return registerExpression(
			MyExpression(
				grammar,
				OwnConst.Companion.K_OR,
				OwnConst.Companion.K_NOT_NULL,
				name,
				OwnConst.Companion.K_IS_NOT_NULL
			), wheres()
		)
	}

	fun whereIn(name: String, values: Iterable<*>?): TheQuery<M> {
		return registerExpression(
			MyExpression(
				grammar,
				OwnConst.Companion.K_AND,
				OwnConst.Companion.K_IN,
				name,
				OwnConst.Companion.K_IN,
				values
			), wheres()
		)
	}

	fun orWhereIn(name: String, values: Iterable<*>?): TheQuery<M> {
		return registerExpression(
			MyExpression(
				grammar,
				OwnConst.Companion.K_OR,
				OwnConst.Companion.K_IN,
				name,
				OwnConst.Companion.K_IN,
				values
			), wheres()
		)
	}

	fun whereNotIn(name: String, values: Iterable<*>?): TheQuery<M> {
		return registerExpression(
			MyExpression(
				grammar,
				OwnConst.Companion.K_AND,
				OwnConst.Companion.K_NOT_IN,
				name,
				OwnConst.Companion.K_NOT_IN,
				values
			), wheres()
		)
	}

	fun orWhereNotIn(name: String, values: Iterable<*>?): TheQuery<M> {
		return registerExpression(
			MyExpression(
				grammar,
				OwnConst.Companion.K_OR,
				OwnConst.Companion.K_NOT_IN,
				name,
				OwnConst.Companion.K_NOT_IN,
				values
			), wheres()
		)
	}

	fun whereRaw(sql: String): TheQuery<M> {
		return registerExpression(
			MyExpression(grammar, OwnConst.Companion.K_AND, OwnConst.Companion.K_RAW, sql),
			wheres()
		)
	}

	fun orWhereRaw(sql: String): TheQuery<M> {
		return registerExpression(
			MyExpression(grammar, OwnConst.Companion.K_OR, OwnConst.Companion.K_RAW, sql),
			wheres()
		)
	}

	/**
	 * Register expression for where, join...
	 *
	 * @param exp Where condition
	 * @return query builder
	 */
	private fun registerExpression(exp: MyExpression, expressions: MutableList<MyExpression>): TheQuery<M> {
		// Trim passing params
		exp.name = exp.name.trim { it <= ' ' }
		exp.operator = exp.operator!!.trim { it <= ' ' }

		// Validation
		if (grammar.invalidOperator(exp.operator)) {
			throw RuntimeException("Invalid operator: " + exp.operator)
		}

		// Fix grammar
		grammar.fixGrammar(exp)

		// Register expression
		expressions.add(exp)
		return this
	}

	fun groupBy(vararg names: String): TheQuery<M> {
		for (name in names) {
			groupBys().add(MyGroupBy(grammar, OwnConst.Companion.K_BASIC, name))
		}
		return this
	}

	fun groupByRaw(sql: String): TheQuery<M> {
		groupBys().add(MyGroupBy(grammar, OwnConst.Companion.K_RAW, sql))
		return this
	}

	fun orderBy(name: String): TheQuery<M> {
		return orderBy(OwnConst.Companion.K_BASIC, name, OwnConst.Companion.K_ASC)
	}

	fun orderBy(name: String, direction: String): TheQuery<M> {
		return orderBy(OwnConst.Companion.K_BASIC, name, direction)
	}

	fun orderByAsc(name: String): TheQuery<M> {
		return orderBy(OwnConst.Companion.K_BASIC, name, OwnConst.Companion.K_ASC)
	}

	fun orderByDesc(name: String): TheQuery<M> {
		return orderBy(OwnConst.Companion.K_BASIC, name, OwnConst.Companion.K_DESC)
	}

	fun orderByRaw(sql: String): TheQuery<M> {
		return orderBy(OwnConst.Companion.K_RAW, sql, OwnConst.Companion.K_ASC)
	}

	fun orderByRaw(sql: String, direction: String): TheQuery<M> {
		return orderBy(OwnConst.Companion.K_RAW, sql, direction)
	}

	fun orderByRawAsc(sql: String): TheQuery<M> {
		return orderBy(OwnConst.Companion.K_RAW, sql, OwnConst.Companion.K_ASC)
	}

	fun orderByRawDesc(sql: String): TheQuery<M> {
		return orderBy(OwnConst.Companion.K_RAW, sql, OwnConst.Companion.K_DESC)
	}

	private fun orderBy(type: String, name: String, direction: String): TheQuery<M> {
		orderBys().add(MyOrderBy(grammar, type, name, direction))
		return this
	}

	fun having(name: String, operator: String, value: Any?): TheQuery<M> {
		return registerExpression(
			MyExpression(
				grammar,
				OwnConst.Companion.K_AND,
				OwnConst.Companion.K_BASIC,
				name,
				operator,
				value
			), havings()
		)
	}

	fun orHaving(name: String, operator: String, value: Any?): TheQuery<M> {
		return registerExpression(
			MyExpression(
				grammar,
				OwnConst.Companion.K_OR,
				OwnConst.Companion.K_BASIC,
				name,
				operator,
				value
			), havings()
		)
	}

	fun havingRaw(sql: String): TheQuery<M> {
		return registerExpression(
			MyExpression(grammar, OwnConst.Companion.K_AND, OwnConst.Companion.K_RAW, sql),
			havings()
		)
	}

	fun orHavingRaw(sql: String): TheQuery<M> {
		return registerExpression(
			MyExpression(grammar, OwnConst.Companion.K_OR, OwnConst.Companion.K_RAW, sql),
			havings()
		)
	}

	fun offset(offset: Long): TheQuery<M> {
		this.offset = offset
		return this
	}

	fun limit(limit: Long): TheQuery<M> {
		this.limit = limit
		return this
	}

	// endregion Build query
	// region CRUD
	fun first(): M? {
		limit = 1
		val rows = this.get()
		return if (rows == null || rows.size == 0) null else rows[0]
	}

	fun get(): List<M> {
		val grammar = grammar
		val all = arrayOf(
			"select",
			grammar.compileDistinct(distinct),
			grammar.compileSelects(nullableSelects),
			"from",
			grammar.safeName(tableName),
			grammar.compileJoins(nullableJoins),
			grammar.compileWheres(nullableWheres),
			grammar.compileGroupBys(nullableGroupBys),
			grammar.compileHaving(nullableHavings),
			grammar.compileOrderBys(nullableOrderBys),
			grammar.compileLimit(limit),
			grammar.compileOffset(offset)
		)
		val items: MutableList<String> = ArrayList()
		for (s in all) {
			if (s != null && s.length > 0) {
				items.add(s)
			}
		}

		// Maybe should validate the query in future
		// for now, just run the query
		val query = DkStrings.join(DkConst.SPACE_CHAR, items)
		return connection.query(query, rowType)
	}

	/**
	 * This will build query from given params, and perform insert.
	 */
	fun insert(insertParams: Map<String, Any?>?) {
		val query = grammar.compileInsertQuery(tableName, insertParams)
		connection.execute(query!!)
	}

	/**
	 * This will build query from given params, and perform update.
	 */
	fun update(updateParams: Map<String, Any?>) {
		val whereClause = grammar.compileWheres(wheres())
		if (isEnableStrictMode && wheres().size == 0) {
			throw RuntimeException("Failed since perform update without any condition in strict mode")
		}
		val query = grammar.compileUpdateQuery(tableName, updateParams, whereClause)
		connection.execute(query!!)
	}

	/**
	 * This will build delete-query, and perform delete.
	 */
	fun delete() {
		val whereClause = grammar.compileWheres(wheres())
		val query = grammar.compileDeleteQuery(tableName, whereClause)
		connection.execute(query!!)
	}

	/**
	 * Validate the correctness of sql query.
	 */
	fun validateSql(): TheQuery<M> {
		throw RuntimeException("Invalid sql")
	}

	/**
	 * Validate the correctness of build query.
	 */
	fun validateQuery(): TheQuery<M> {
		throw RuntimeException("Invalid sql")
	}

	fun setEnableStrictMode(enableStrictMode: Boolean): TheQuery<M> {
		isEnableStrictMode = enableStrictMode
		return this
	}
}
