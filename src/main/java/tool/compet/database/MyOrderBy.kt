/*
 * Copyright (c) 2017-2021 DarkCompet. All rights reserved.
 */
package tool.compet.database

class MyOrderBy(
	private val grammar: OwnGrammar, var type: String, // column or raw sql
	var name: String, var direction: String
) {
	fun compile(): String {
		return when (type) {
			OwnConst.K_BASIC -> grammar.safeName(name) + ' ' + direction
			OwnConst.K_RAW -> name
			else -> throw RuntimeException("Invalid type: " + type)
		}
	}
}
