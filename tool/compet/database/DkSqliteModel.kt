/*
 * Copyright (c) 2017-2021 DarkCompet. All rights reserved.
 */
package tool.compet.database

/**
 * Subclass should extends this to listen trigger upsert from DAO.
 *
 * Lets talk about this name, why we call it as `model`...
 * As we known, model is considered for shape, figure...
 * So we take this name for database's table since they are structured one.
 */
abstract class DkSqliteModel<M : OwnModel<M>>(
	connection: TheSqliteConnection,
	tableName: String,
	rowType: Class<M>
) : OwnModel<M>(connection, tableName, rowType) {

	// For all query builder instances
	protected val grammar = OwnSqliteGrammar()

	override fun newQuery(): TheModelQuery<M> {
		val connection = connection as TheSqliteConnection
		val query = OwnSqliteQuery(connection, grammar, tableName, rowType)
		return OwnSqliteModelQuery(this, query)
	}

	public override fun lastInsertRowId(): Long {
		val connection = connection as TheSqliteConnection
		val sql = "select last_insert_rowid()"

		connection.rawQuery(sql).use { cursor ->  // Autoclose
			if (cursor.moveToFirst()) {
				return cursor.getLong(0)
			}
		}
		return -1
	}
}
