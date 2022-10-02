/*
 * Copyright (c) 2017-2021 DarkCompet. All rights reserved.
 */
package tool.compet.database

class MyGroupBy(
	private val grammar: OwnGrammar,
	var type: String, // column or raw sql
	var name: String
) {
	fun compile(): String? {
		return when (type) {
			"basic" -> grammar.safeName(name)
			"raw" -> name
			else -> throw RuntimeException("Invalid type: $type")
		}
	}
}
