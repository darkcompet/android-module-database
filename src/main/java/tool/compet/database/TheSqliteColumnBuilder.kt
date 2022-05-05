/*
 * Copyright (c) 2017-2021 DarkCompet. All rights reserved.
 */
package tool.compet.database

class TheSqliteColumnBuilder {
	private val colDetails: MutableList<TheSqliteColumnDetail> = ArrayList()

	/**
	 * Add id column without autoincrement feature.
	 * We should turn off this feature for re-use rowid.
	 */
	@JvmOverloads
	fun id(colName: String, autoincrement: Boolean = false): TheSqliteColumnDetail {
		var type = "integer primary key"
		if (autoincrement) {
			type += " autoincrement"
		}
		return addColumn(colName, type)
	}

	fun integer(colName: String): TheSqliteColumnDetail {
		return addColumn(colName, "integer")
	}

	fun string(colName: String): TheSqliteColumnDetail {
		return addColumn(colName, "text")
	}

	fun real(colName: String): TheSqliteColumnDetail {
		return addColumn(colName, "real")
	}

	fun blob(colName: String): TheSqliteColumnDetail {
		return addColumn(colName, "blob")
	}

	private fun addColumn(colName: String, colType: String): TheSqliteColumnDetail {
		val colDetail = TheSqliteColumnDetail(colName, colType)
		colDetails.add(colDetail)
		return colDetail
	}

	fun build(): List<String> {
		val colDefs: MutableList<String> = ArrayList()
		for (detail in colDetails) {
			val colDef = detail.build()
			if (colDef != null && colDef.length > 0) {
				colDefs.add(colDef)
			}
		}
		return colDefs
	}
}
