/*
 * Copyright (c) 2017-2020 DarkCompet. All rights reserved.
 */
package tool.compet.database

import androidx.collection.ArrayMap

/**
 * This is base model query builder for various query language as sqlite, mysql, postgresql...
 * It receives a database connection, provides a query execution.
 * Caller can build and execute a query from this instead of manual sql.
 *
 * Different with [TheQuery], config declared in the model
 * will be applied when query, and user usually handle with model's fields
 * instead of working direct with table column.
 *
 * @author darkcompet
 */
abstract class TheModelQuery<M : OwnModel<M>>(
	protected var model: OwnModel<M>,
	private var query: TheQuery<M>
) {
	fun where(name: String, value: Any?): TheModelQuery<M> {
		query.where(name, value)
		return this
	}

	fun select(vararg names: String): TheModelQuery<M> {
		query.select(*names)
		return this
	}

	fun first(): M? {
		return query.first()
	}

	fun get(): List<M?> {
		return query.get()
	}

	fun insert(insertParams: ArrayMap<String, Any?>) {
		query.insert(insertParams)
	}

	fun update(updateParams: ArrayMap<String, Any?>) {
		query.update(updateParams)
	}
}
