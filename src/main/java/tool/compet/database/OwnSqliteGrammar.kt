/*
 * Copyright (c) 2017-2021 DarkCompet. All rights reserved.
 */
package tool.compet.database

import androidx.collection.ArraySet
import java.util.*

/**
 * Sqlite grammar.
 */
class OwnSqliteGrammar : OwnGrammar() {
	private val availableOperators: Set<String> = ArraySet(
		listOf(
			"=", "<", ">", "<=", ">=", "<>", "!=",
			"is null", "is not null",
			"in", "not in",
			"like", "not like", "ilike",
			"&", "|", "<<", ">>"
		)
	)

	override fun availableOperators(): Set<String> {
		return availableOperators
	}
}
