/*
 * Copyright (c) 2017-2020 DarkCompet. All rights reserved.
 */
package tool.compet.database

import tool.compet.core.DkTypeHelper

object OwnGrammarHelper {
	fun safeName(name: String): String {
		var name = name
		if ("*" == name) {
			return "*"
		}
		name = name.trim { it <= ' ' }
		if (name.contains(".")) {
			val arr = name.split("\\.").toTypedArray()
			return safeName(arr[0]) + '.' + safeName(arr[1])
		}
		if (name.toLowerCase().contains(" as ")) {
			val arr = name.split("\\s+(?i)as\\s+").toTypedArray() // (?i) for case-insensitive
			return safeName(arr[0]) + " as " + safeName(arr[1])
		}
		return "`$name`"
	}

	fun safeNames(names: Collection<String>?): List<String> {
		val result: MutableList<String> = ArrayList()
		if (names != null) {
			for (name in names) {
				result.add(safeName(name))
			}
		}
		return result
	}

	/**
	 * Safing given value before upsert to database.
	 * Use it can avoid SQL-injection problem.
	 *
	 * @param value For eg,. 1, "darkcompet", 99.991,...
	 *
	 * @return For eg,. '1', 'darkcompet', '99.991',...
	 */
	fun safeValue(value: Any?): String? {
		if (value == null) {
			return null
		}
		val singleQuote = "'"
		val doubleQuote = "''"
		return if (value is String) {
			singleQuote + value.replace(singleQuote, doubleQuote) + singleQuote
		}
		else singleQuote + value + singleQuote
	}

	fun safeValues(values: Iterable<*>?): List<String?> {
		val items: MutableList<String?> = ArrayList()
		for (value in values!!) {
			items.add(safeValue(value))
		}
		return items
	}

	/**
	 * This will convert given value to db value since
	 * some values don't have corresponding type in database, for eg,. boolean...
	 *
	 */
	fun makeDbValue(obj: Any?): Any? {
		if (obj == null) {
			return null
		}
		return if (DkTypeHelper.typeMasked(obj.javaClass) == DkTypeHelper.TYPE_BOOLEAN_MASKED) {
			if (obj as Boolean) 1 else 0
		}
		else obj
	}
}
