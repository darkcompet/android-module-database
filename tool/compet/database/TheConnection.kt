package tool.compet.database

/**
 * Provides connection to database for querying.
 * This is normally called from query-builders.
 */
interface TheConnection {
	/**
	 * Query with given sql and result to list of given row-type.
	 * This is used for select-query.
	 */
	fun <M> query(sql: String, rowType: Class<M>): List<M>

	/**
	 * Execute given sql. This is used for upsert-query.
	 */
	fun execute(sql: String)
}
