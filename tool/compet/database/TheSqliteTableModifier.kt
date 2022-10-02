/*
 * Copyright (c) 2017-2021 DarkCompet. All rights reserved.
 */
package tool.compet.database

import tool.compet.core.DkStrings

class TheSqliteTableModifier(private val tableName: String) {
	private val orderedCommands: MutableList<String> = ArrayList()

	fun addIndex(colName: String): TheSqliteTableModifier {
		val tab_col_idx = OwnGrammarHelper.safeName(DkStrings.format("%s_%s_idx", tableName, colName))
		val wrapped_tab_name = OwnGrammarHelper.safeName(tableName)
		val wrapped_col_name = OwnGrammarHelper.safeName(colName)
		orderedCommands.add(
			DkStrings.format(
				"create index %s on %s (%s)",
				tab_col_idx,
				wrapped_tab_name,
				wrapped_col_name
			)
		)
		return this
	}

	fun removeIndex(colName: String?): TheSqliteTableModifier {
		val index_name = OwnGrammarHelper.safeName(DkStrings.format("%s_%s_idx", tableName, colName))
		orderedCommands.add(DkStrings.format("drop index if exists %s", index_name))
		return this
	}

	fun compile(): List<String> {
		return orderedCommands
	}
}
