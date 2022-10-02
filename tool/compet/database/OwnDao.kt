/*
 * Copyright (c) 2017-2021 DarkCompet. All rights reserved.
 */
package tool.compet.database

/**
 * Data Access Object (DAO) for query with database. This is also called as `repository`.
 * In general, subclass of DAO will use [TheQuery] or [TheModelQuery]
 * to write queries.
 *
 * @param <M> Query result model row-type.
 */
abstract class OwnDao<M> protected constructor(
	protected var connection: TheConnection, // to handle (query, execute...) with database
	protected var tableName: String, // target table
	protected var rowType: Class<M> // query result row-type
) {
	/**
	 * Make new query builder.
	 * Subclass uses this for query.
	 */
	protected abstract fun newQuery(): TheQuery<M>

	/**
	 * @return Number of record in the table.
	 */
	protected abstract fun count(): Long

	/**
	 * @return TRUE iff the table is empty (no record).
	 */
	protected abstract fun empty(): Boolean

	/**
	 * @return ID of last inserted record.
	 */
	protected abstract fun lastInsertRowId(): Long

	/**
	 * Delete all records from the table and Reset auto-increment key.
	 */
	abstract fun truncate()

	/**
	 * Delete all records from the table.
	 *
	 * Some database config may enable primary-key at where clause,
	 * at that case, subclass maybe need override this to re-implement.
	 */
	fun clear() {
		newQuery().delete()
	}

	/**
	 * Each query needs a connection to execute sql.
	 * When caller has own connection, should set to it to avoid cyclic-invocation
	 * inside the connection in some cases.
	 */
//	fun setConnection(connection: TheConnection) {
//		this.connection = connection
//	}
}
