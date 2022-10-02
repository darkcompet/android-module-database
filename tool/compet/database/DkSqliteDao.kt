/*
 * Copyright (c) 2017-2021 DarkCompet. All rights reserved.
 */
package tool.compet.database

/**
 * DAO for Android Sqlite.
 *
 * @param <M> Query result model row-type.
 */
abstract class DkSqliteDao<M>(
	connection: TheSqliteConnection,
	tableName: String,
	rowType: Class<M>
) : OwnDao<M>(connection, tableName, rowType) {
	// Auto-increment PK has 4 equivalent names: `rowid`, `_rowid_`, `oid` and `id` (same with our defined PK in model)
	protected val ROWID = "_rowid_"

	// Default sqlite grammar
	protected val grammar = OwnSqliteGrammar()

	public override fun newQuery(): TheQuery<M> {
		val connection = this.connection as TheSqliteConnection
		return OwnSqliteQuery(connection, grammar, tableName, rowType)
	}

	/**
	 * Get target row via its id.
	 */
	fun find(rowid: Long): M? {
		return newQuery().where(ROWID, rowid).first()
	}

	fun get(): List<M> {
		return newQuery().get()
	}

	/**
	 * Load list of row with limited.
	 *
	 * @param limit Max number of row to get. Pass 0 to unlimit.
	 */
	fun get(limit: Long): List<M> {
		return newQuery().limit(limit).get()
	}

	/**
	 * Delete target row via its id.
	 */
	fun delete(rowid: Long) {
		newQuery().where(ROWID, rowid).delete()
	}

	override fun truncate() {
		// In Sqlite, truncate is same with `clear` since
		// autoincrement key will also be reset if clear table.
		clear()
	}

	public override fun empty(): Boolean {
		val connection = connection as TheSqliteConnection
		// Auto-increment PK has 4 equivalent names: `rowid`, `_rowid_`, `oid` and `id` (our curstom pk)
		val sql = "select `_rowid_` from " + OwnGrammarHelper.safeName(tableName) + " limit 1"
		connection.rawQuery(sql).use { cursor ->  // Autoclose
			return cursor.count == 0
		}
	}

	public override fun count(): Long {
		val connection = connection as TheSqliteConnection
		val sql = "select count(`_rowid_`) from " + OwnGrammarHelper.safeName(tableName)
		connection.rawQuery(sql).use { cursor ->  // Autoclose
			if (cursor.moveToFirst()) {
				return cursor.getLong(0)
			}
		}
		return 0
	}

	public override fun lastInsertRowId(): Long {
		val connection = connection as TheSqliteConnection
		val sql = "select last_insert_rowid()"
		connection.rawQuery(sql).use { cursor ->  // use() will autoclose cursor
			if (cursor.moveToFirst()) {
				return cursor.getLong(0)
			}
		}
		return -1
	}
}
