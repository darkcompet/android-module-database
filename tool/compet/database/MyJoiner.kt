/*
 * Copyright (c) 2017-2021 DarkCompet. All rights reserved.
 */
package tool.compet.database

import tool.compet.core.DkConst
import tool.compet.core.DkStrings

class MyJoiner(grammar: OwnGrammar) : MyExpression(grammar, OwnConst.Companion.K_AND, OwnConst.Companion.K_BASIC) {
	private var __expressions: MutableList<MyExpression>? = null
	private fun expressions(): MutableList<MyExpression> {
		return if (__expressions != null) __expressions!! else ArrayList<MyExpression>().also { __expressions = it }
	}

	fun on(first: String, second: String?): MyJoiner {
		expressions().add(
			MyExpression(
				grammar,
				OwnConst.Companion.K_AND,
				OwnConst.Companion.K_BASIC,
				first,
				OwnConst.Companion.K_EQ,
				second
			)
		)
		return this
	}

	fun on(first: String, operator: String, second: String?): MyJoiner {
		expressions().add(
			MyExpression(
				grammar,
				OwnConst.Companion.K_AND,
				OwnConst.Companion.K_BASIC,
				first,
				operator,
				second
			)
		)
		return this
	}

	fun where(name: String, value: Any?): MyJoiner {
		expressions().add(
			MyExpression(
				grammar,
				OwnConst.Companion.K_AND,
				OwnConst.Companion.K_BASIC,
				name,
				OwnConst.Companion.K_EQ,
				value
			)
		)
		return this
	}

	fun orWhere(name: String, value: Any?): MyJoiner {
		expressions().add(
			MyExpression(
				grammar,
				OwnConst.Companion.K_OR,
				OwnConst.Companion.K_BASIC,
				name,
				OwnConst.Companion.K_EQ,
				value
			)
		)
		return this
	}

	fun where(name: String, operator: String, value: Any?): MyJoiner {
		expressions().add(
			MyExpression(
				grammar,
				OwnConst.Companion.K_AND,
				OwnConst.Companion.K_BASIC,
				name,
				operator,
				value
			)
		)
		return this
	}

	fun orWhere(name: String, operator: String, value: Any?): MyJoiner {
		expressions().add(
			MyExpression(
				grammar,
				OwnConst.Companion.K_OR,
				OwnConst.Companion.K_BASIC,
				name,
				operator,
				value
			)
		)
		return this
	}

	fun whereNull(name: String): MyJoiner {
		expressions().add(
			MyExpression(
				grammar,
				OwnConst.Companion.K_AND,
				OwnConst.Companion.K_NULL,
				name,
				OwnConst.Companion.K_IS_NULL
			)
		)
		return this
	}

	fun orWhereNull(name: String): MyJoiner {
		expressions().add(
			MyExpression(
				grammar,
				OwnConst.Companion.K_OR,
				OwnConst.Companion.K_NULL,
				name,
				OwnConst.Companion.K_IS_NULL
			)
		)
		return this
	}

	fun whereNotNull(name: String): MyJoiner {
		expressions().add(
			MyExpression(
				grammar,
				OwnConst.Companion.K_AND,
				OwnConst.Companion.K_NOT_NULL,
				name,
				OwnConst.Companion.K_IS_NOT_NULL
			)
		)
		return this
	}

	fun orWhereNotNull(name: String): MyJoiner {
		expressions().add(
			MyExpression(
				grammar,
				OwnConst.Companion.K_OR,
				OwnConst.Companion.K_NOT_NULL,
				name,
				OwnConst.Companion.K_IS_NOT_NULL
			)
		)
		return this
	}

	fun whereIn(name: String, values: Iterable<*>?): MyJoiner {
		expressions().add(
			MyExpression(
				grammar,
				OwnConst.Companion.K_AND,
				OwnConst.Companion.K_IN,
				name,
				OwnConst.Companion.K_IN,
				values
			)
		)
		return this
	}

	fun orWhereIn(name: String, values: Iterable<*>?): MyJoiner {
		expressions().add(
			MyExpression(
				grammar,
				OwnConst.Companion.K_OR,
				OwnConst.Companion.K_IN,
				name,
				OwnConst.Companion.K_IN,
				values
			)
		)
		return this
	}

	fun whereNotIn(name: String, values: Iterable<*>?): MyJoiner {
		expressions().add(
			MyExpression(
				grammar,
				OwnConst.Companion.K_AND,
				OwnConst.Companion.K_NOT_IN,
				name,
				OwnConst.Companion.K_NOT_IN,
				values
			)
		)
		return this
	}

	fun orWhereNotIn(name: String, values: Iterable<*>?): MyJoiner {
		expressions().add(
			MyExpression(
				grammar,
				OwnConst.Companion.K_OR,
				OwnConst.Companion.K_NOT_IN,
				name,
				OwnConst.Companion.K_NOT_IN,
				values
			)
		)
		return this
	}

	fun whereRaw(sql: String): MyJoiner {
		expressions().add(MyExpression(grammar, OwnConst.Companion.K_AND, OwnConst.Companion.K_RAW, sql))
		return this
	}

	fun orWhereRaw(sql: String): MyJoiner {
		expressions().add(MyExpression(grammar, OwnConst.Companion.K_OR, OwnConst.Companion.K_RAW, sql))
		return this
	}

	/**
	 * Compile multiple join conditions.
	 *
	 * @return for eg,. "`user`.`name` is null and `user`.`age` <= '20'"
	 */
	public override fun compile(): String {
		if (expressions().size == 0) {
			return DkConst.EMPTY_STRING
		}
		val clauses: MutableList<String?> = ArrayList()
		for (exp in expressions()) {
			clauses.add(exp.compile())
		}
		var joinedClauses = DkStrings.join(' ', clauses)
		joinedClauses = joinedClauses.replaceFirst("^(and|or)".toRegex(), DkConst.EMPTY_STRING)
		return joinedClauses.trim { it <= ' ' }
	}
}
