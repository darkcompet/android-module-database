/*
 * Copyright (c) 2017-2021 DarkCompet. All rights reserved.
 */
package tool.compet.database

class TheSqliteColumnDetail internal constructor(
	private val name: String,
	private val type: String
) {
	private var nullable = true
	private var hasDefaultValue = false
	private var defaultValue: String? = null
	fun nullable(nullable: Boolean): TheSqliteColumnDetail {
		this.nullable = nullable
		return this
	}

	fun defaultValue(value: String?): TheSqliteColumnDetail {
		hasDefaultValue = true
		defaultValue = value
		return this
	}

	fun build(): String {
		var def = OwnGrammarHelper.safeName(name) + " " + type
		if (!nullable) {
			def += " not null"
		}
		if (hasDefaultValue) {
			def += " '$defaultValue'"
		}
		return def.trim { it <= ' ' }
	}
}
