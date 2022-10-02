/*
 * Copyright (c) 2017-2021 DarkCompet. All rights reserved.
 */
package tool.compet.database

import tool.compet.core.DkStrings

class MyJoin {
	private val grammar: OwnGrammar
	private val joinType: String // left, inner, right
	private val joinTable: String // user_detail

	// A. Single join on
	private var first: String? = null
	private var operator: String? = null
	private var second: String? = null

	// B. Multiple join on
	private var joiner: MyJoiner? = null

	// A
	constructor(
		grammar: OwnGrammar,
		joinType: String,
		joinTable: String,
		first: String?,
		operator: String?,
		second: String?
	) {
		this.grammar = grammar
		this.joinType = joinType
		this.joinTable = joinTable
		this.first = first
		this.operator = operator
		this.second = second
	}

	// B
	constructor(grammar: OwnGrammar, joinType: String, joinTable: String, joiner: MyJoiner?) {
		this.grammar = grammar
		this.joinType = joinType
		this.joinTable = joinTable
		this.joiner = joiner
	}

	/**
	 * For eg,. this builds below clauses:
	 * 1. left join `event` on `user`.`id` = `event`.`user_id` and `user`.`rank` >= `event`.`level`
	 * 2. right join `event` on `user`.`id` = `event`.`user_id` or `user`.`rank` <= `event`.`level`
	 * 3. inner join `event` on `user`.`id` = `event`.`user_id` and `user`.`rank` != `event`.`level`
	 */
	fun compile(): String {
		val tableName = grammar.safeName(joinTable)
		val onCondition = makeCondition()
		return DkStrings.format("%s join %s on %s", joinType, tableName, onCondition)
	}

	private fun makeCondition(): String? {
		return if (joiner != null) {
			joiner!!.compile()
		}
		else grammar.safeName(first!!) + ' ' + operator + ' ' + grammar.safeName(second!!)
	}
}
