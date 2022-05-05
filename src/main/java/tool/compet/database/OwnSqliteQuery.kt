/*
 * Copyright (c) 2017-2021 DarkCompet. All rights reserved.
 */
package tool.compet.database

import androidx.collection.ArrayMap
import tool.compet.core.DkStrings

/**
 * Sqlite query builder.
 */
class OwnSqliteQuery<M>(connection: TheSqliteConnection, grammar: OwnGrammar, tableName: String, rowType: Class<M>) :
	TheQuery<M>(connection, grammar, tableName, rowType) {
	/**
	 * This is specific to Sqlite.
	 *
	 * Refer: https://www.sqlitetutorial.net/sqlite-replace-statement/
	 */
	fun upsert(
		insertParams: ArrayMap<String, Any?>,
		updateParams: ArrayMap<String, Any?>,
		primaryKeys: Iterable<String>
	) {
		//	   INSERT INTO tmp (email, username) VALUES ('mail1', 'name1')
		//	   ON CONFLICT (email, username)
		//	   DO UPDATE SET email='kkkk3'
		val _tmpList: MutableList<String?> = ArrayList()

		// Build insert name list: `email`, `telno`
		for (index in insertParams.size - 1 downTo 0) {
			val colName = insertParams.keyAt(index)
			_tmpList.add(grammar.safeName(colName))
		}
		val flatternInsertColumns = DkStrings.join(", ", _tmpList)

		// Build insert value list: 'test@g.c', `0923919232`
		_tmpList.clear()
		for (index in insertParams.size - 1 downTo 0) {
			val colValue = insertParams.valueAt(index)
			_tmpList.add(grammar.safeValue(colValue))
		}
		val flattenInsertValues = DkStrings.join(", ", _tmpList)

		// Build pk list: `username`, `email`
		_tmpList.clear()
		for (pk in primaryKeys) {
			_tmpList.add(grammar.safeName(pk))
		}
		val flattenPKs = DkStrings.join(", ", _tmpList)

		// Build update list: `email` = 'test@g.c', `telno` = '098191020020'
		_tmpList.clear()
		for (index in updateParams.size - 1 downTo 0) {
			val colName = grammar.safeName(updateParams.keyAt(index))
			val colValue: Any? = grammar.safeValue(updateParams.valueAt(index))
			_tmpList.add("$colName = $colValue")
		}
		val flattenUpdateSet = DkStrings.join(", ", _tmpList)
		val upsertQuery = "insert into" +
			" " + grammar.safeName(tableName) +
			" (" + flatternInsertColumns + ")" +
			" values " + flattenInsertValues +
			" on conflict (" + flattenPKs + ")" +
			" do update set " + flattenUpdateSet
		connection.execute(upsertQuery)
	}
}
