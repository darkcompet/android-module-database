/*
 * Copyright (c) 2017-2021 DarkCompet. All rights reserved.
 */
package tool.compet.database

import tool.compet.core.DkRunner1
import tool.compet.core.DkStrings

/**
 * Migration helper for SQLite.
 * Caller can use this to edit table schema as:
 * - Create, drop table column.
 * - Add, remove index.
 * ...
 */
object DkSqliteSchema {
	/**
	 * Create new table.
	 */
	fun create(connection: TheConnection, tableName: String, builderCb: DkRunner1<TheSqliteColumnBuilder>) {
		// Get table definition from caller
		val builder = TheSqliteColumnBuilder()
		builderCb.run(builder)

		// Build table definition
		val colsDef = builder.build()

		// Create table
		val sql = DkStrings.format(
			"create table if not exists %s (%s)",
			OwnGrammarHelper.safeName(tableName),
			DkStrings.join(", ", colsDef)
		)
		connection.execute(sql)
	}

	/**
	 * Modifies table attributes like: index (add, drop), column (add, drop)...
	 */
	fun table(connection: TheConnection, tableName: String, modifierCb: DkRunner1<TheSqliteTableModifier>) {
		// Get commands from caller
		val modifier = TheSqliteTableModifier(tableName)
		modifierCb.run(modifier)

		// Build and Run sql collection
		for (sql in modifier.compile()) {
			connection.execute(sql)
		}
	}
}
