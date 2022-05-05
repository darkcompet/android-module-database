/*
 * Copyright (c) 2017-2021 DarkCompet. All rights reserved.
 */
package tool.compet.database

/**
 * Sqlite model query builder.
 */
class OwnSqliteModelQuery<M : OwnModel<M>>(
	model: DkSqliteModel<M>,
	private val query: OwnSqliteQuery<M>
) : TheModelQuery<M>(model, query) {
	/**
	 * Sqlite can perform upsert with one query.
	 */
	fun upsert() {
		query.upsert(model.calcInsertParams(), model.calcUpdateParams(), model.primaryKeys!!)
	}
}
