/*
 * Copyright (c) 2017-2021 DarkCompet. All rights reserved.
 */
package tool.compet.database

/**
 * Manage selection clause.
 */
class MySelection {
	private val grammar: OwnGrammar

	// Common
	var type: String // basic, raw

	// A. For basic column select
	var name: String? = null // columns, alias, ...

	// B. For raw select (sub query, function...)
	var raw: String? = null // function, sub query, ...
	var alias: String? = null // user_id, user_name, ...

	// A
	constructor(grammar: OwnGrammar, type: String, name: String?) {
		this.grammar = grammar
		this.type = type
		this.name = name
	}

	// B
	constructor(grammar: OwnGrammar, type: String, raw: String?, alias: String?) {
		this.grammar = grammar
		this.type = type
		this.raw = raw
		this.alias = alias
	}

	fun compile(): String? {
		return grammar.compileSelect(this)
	}
}
