/*
 * Copyright (c) 2017-2021 DarkCompet. All rights reserved.
 */
package tool.compet.database

import tool.compet.core.DkStrings

/**
 * Hold operation info to express comparasion.
 *
 *
 * For eg,. "`user`.`id` <= 100",
 * "`user`.`name` is not null"
 */
open class MyExpression(
	protected val grammar: OwnGrammar, // and, or
	protected var logic: String, // basic, null, not null, in, not in, raw
	protected var type: String
) {
	// user.id as user_id, or raw query
	lateinit var name: String
	// =, <, >, ...
	var operator: String? = null
	// Primitive or iterable
	var value: Any? = null

	constructor(grammar: OwnGrammar, logic: String, type: String, name: String) : this(grammar, logic, type) {
		this.name = name
	}

	constructor(grammar: OwnGrammar, logic: String, type: String, name: String, operator: String) : this(
		grammar,
		logic,
		type,
		name
	) {
		this.operator = operator
	}

	constructor(grammar: OwnGrammar, logic: String, type: String, name: String, operator: String, value: Any?) : this(
		grammar,
		logic,
		type,
		name,
		operator
	) {
		this.value = value
	}

	/**
	 * Compile to build expression (condition) for given info.
	 *
	 * @return Expression like "and `user`.`name` is not null"
	 */
	open fun compile(): String {
		return logic + ' ' + compileWithoutLogic()
	}

	private fun compileWithoutLogic(): String {
		val name = grammar.safeName(name!!) // user.name as user_name
		val value = OwnGrammarHelper.makeDbValue(value)
		return when (type) {
			OwnConst.K_BASIC -> {
				DkStrings.format("%s %s %s", name, operator, grammar.safeValue(value))
			}
			OwnConst.K_NULL, OwnConst.K_NOT_NULL -> {
				DkStrings.format("%s %s", name, operator)
			}
			OwnConst.K_IN, OwnConst.K_NOT_IN -> {
				val values = grammar.safeValues(value as Iterable<*>)
				DkStrings.format("%s %s (%s)", name, operator, DkStrings.join(", ", values))
			}
			else -> {
				throw RuntimeException("Invalid type: $type")
			}
		}
	}
}
