/*
 * Copyright (c) 2017-2021 DarkCompet. All rights reserved.
 */
package tool.compet.database

import androidx.collection.ArrayMap
import tool.compet.core.DkArrays
import tool.compet.core.DkLogcats
import tool.compet.core.DkRuntimeException
import tool.compet.reflection.DkReflectionFinder
import java.lang.reflect.Field

/**
 * Query model for associated table.
 * This model is mapping of Java object with Database table.
 * Dao can use query-builder from this model or raw query-builder to query.
 *
 * By default, we enable below properties:
 * - Auto-increment with primary key `id`.
 * - Primary keys: [`id`] only.
 */
abstract class OwnModel<M : OwnModel<M>>(
	// DB connection to perform query
	var connection: TheConnection,

	// Name of table which mapping with this model
	// Subclass must provide it, for eg,. at subclass constructor
	protected var tableName: String,

	// Query result row-type
	//fixme maybe not need for model, if we need extended selection, just use subclass of this
	protected var rowType: Class<M>
) {
	/**
	 * Before call this, should check `autoIncrement` property.
	 * By default, we name rowid as `id`. Subclass can override to change rowid name if want.
	 *
	 * @return Name of rowid (default name is `id`).
	 */
	protected fun rowIdName(): String {
		if (autoIncrement) {
			return DEFAULT_ROWID
		}
		throw DkRuntimeException(this, "Row id is provided only when auto-increment enabled.")
	}

	/**
	 * Before call this, should check `autoIncrement` property.
	 */
	protected abstract fun lastInsertRowId(): Long

	/**
	 * Check whether this model is existing in database.
	 * Subclass can answer by checking value of primary-keys is set or not.
	 * This method is used to judge action `update` or `insert` when call `save()`.
	 */
	protected abstract fun exists(): Boolean

	/**
	 * Make new model query builder.
	 */
	abstract fun newQuery(): TheModelQuery<M>

	/**
	 * Triggered before building params of insert-query.
	 */
	protected abstract fun onInsert()

	/**
	 * Triggered before building params of update-query.
	 */
	protected abstract fun onUpdate()

	// Enable strict-mode to avoid mistake of config or query...
	protected var enableStrictMode = true

	// By default, we assume all models has auto-incremented rowid.
	protected var autoIncrement = true

	// By default, we set pks with default `id`
	// Subclass can change it if pks is different with default value
	var primaryKeys: List<String>? = DkArrays.asList(DEFAULT_ROWID)

	// Fields should be included at upsert-query
	// This is composite of `guarded`
	protected var fillableColumns: List<String>? = null

	// Fields should be ignored at upsert-query
	// This is composite of `fillable`, by default we allow fill all columns
	protected var guardedColumns: List<String>? = null

	// Fields should be ignored at select-query
	// By default, no column is hidden for select
	protected var hiddenColumns: List<String>? = null

	// Mapping of column-name vs field-value for this model
	protected val attributes = ArrayMap<String, Any>()

//	fun setConnection(connection: TheConnection) {
//		this.connection = connection
//	}

//	fun setEnableStrictMode(enableStrictMode: Boolean) {
//		this.enableStrictMode = enableStrictMode
//	}

	fun setFillableColumns(vararg columns: String) {
		fillableColumns = DkArrays.asList(*columns)
	}

	fun setGuardedColumns(vararg columns: String) {
		guardedColumns = DkArrays.asList(*columns)
	}

	fun setHiddenColumns(vararg columns: String) {
		hiddenColumns = DkArrays.asList(*columns)
	}

	/**
	 * Find model of record by row id.
	 *
	 * @param rowid Is PK, normally has type of Integer or String.
	 */
	fun find(rowid: Any): M? {
		return newQuery().where(rowIdName(), rowid).first()
	}

	/**
	 * Insert new record with take care about config of this model.
	 *
	 * Note: We should use `save()` instead as possible to avoid missing between insert and update.
	 */
	fun insert() {
		// Trigger pre-insert to tell subclass prepare some extra info
		onInsert()

		// Perform insert by model-query-builder
		val insertParams = calcInsertParams()
		newQuery().insert(insertParams)

		// Set back inserted-rowid to this model's rowid
		if (autoIncrement) {
			setAttribute(rowIdName(), lastInsertRowId())
		}
	}

	/**
	 * Update target record with take care about config of this model.
	 *
	 * Note: We should use `save()` instead as possible to avoid missing between insert and update.
	 */
	fun update() {
		// Trigger pre-update to tell subclass prepare some extra info
		onUpdate()

		val modelQuery = newQuery()
		val conditions = calcUpdateConditions()
		for (index in conditions.size - 1 downTo 0) {
			modelQuery.where(conditions.keyAt(index), conditions.valueAt(index))
		}

		// Perform update by model-query-builder
		modelQuery.update(calcUpdateParams())
	}

	/**
	 * Save (insert or update changes) model to database.
	 * Other name: upsert, replace.
	 *
	 * Note: Use this method instead of `insert(), update()` as possible.
	 */
	fun save() {
		if (exists()) {
			update()
		}
		else {
			insert()
		}
	}

	/**
	 * Calculate insert-params from this model.
	 */
	fun calcInsertParams(): ArrayMap<String, Any?> {
		val insertParams = ArrayMap<String, Any?>()

		try {
			val modelClass: Class<*> = javaClass
			val name2field = findAndCacheFields(modelClass)
			for (index in name2field!!.size - 1 downTo 0) {
				val colName = name2field.keyAt(index)
				val field = name2field.valueAt(index)
				val colInfo = field.getAnnotation(DkColumnInfo::class.java)
					?: throw DkRuntimeException(
						this,
						"Field %s.%s must be annotated with DkColumnInfo",
						javaClass.name,
						field.name
					)
				// Requires real table-column
				if (colInfo.isTableColumn) {
					// For safe, we also check column is assignable.
					// When table is auto-increment, we ignore insert for rowid
					var insertable = isAssignable(colName)
					if (insertable && autoIncrement && colName == rowIdName()) {
						insertable = false
					}
					if (insertable) {
						insertParams[colName] = OwnGrammarHelper.makeDbValue(field[this])
					}
				}
			}
		}
		catch (e: Exception) {
			throw DkRuntimeException(this, e, "Failed to calculate insert params")
		}
		if (enableStrictMode && insertParams.size == 0) {
			throw DkRuntimeException(this, "Must provide params which can be inserted at strict-mode")
		}
		return insertParams
	}

	/**
	 * Calculate update-condition from this model.
	 */
	protected fun calcUpdateConditions(): ArrayMap<String, Any> {
		if (primaryKeys == null) {
			throw DkRuntimeException(this, "Primary keys must not be empty")
		}
		val conditions = ArrayMap<String, Any>()
		try {
			val modelClass: Class<*> = javaClass
			val name2field = findAndCacheFields(modelClass)
			for (pk in primaryKeys!!) {
				val field =
					name2field!![pk] ?: throw DkRuntimeException(this, "Not found field for pk: %s, pls review !", pk)
				val colInfo = field.getAnnotation(DkColumnInfo::class.java)
					?: throw DkRuntimeException(
						this,
						"Field %s.%s must be annotated with DkColumnInfo",
						javaClass.name,
						field.name
					)
				if (colInfo.isTableColumn) {
					conditions[pk] = field[this]
				}
			}
		}
		catch (e: Exception) {
			throw DkRuntimeException(this, e, "Failed to calculate update condition")
		}
		if (enableStrictMode && conditions.size == 0) {
			throw DkRuntimeException(this, "Must provide condition for update at strict-mode")
		}
		return conditions
	}

	/**
	 * Calculate update-params from this model.
	 */
	fun calcUpdateParams(): ArrayMap<String, Any?> {
		if (primaryKeys == null || primaryKeys!!.isEmpty()) {
			throw DkRuntimeException(this, "Primary keys must not be empty")
		}
		val updateParams = ArrayMap<String, Any?>()
		try {
			val modelClass: Class<*> = javaClass
			val name2field = findAndCacheFields(modelClass)
			for (index in name2field!!.size - 1 downTo 0) {
				val colName = name2field.keyAt(index)
				val field = name2field.valueAt(index)
				val colInfo = field.getAnnotation(DkColumnInfo::class.java)
					?: throw DkRuntimeException(this, "Oops, colInfo cannot be null !")
				// Requires real table-column
				if (colInfo.isTableColumn) {
					// Check column is assignable for update.
					// And in general, we don't allow update model's pk column
					var updatable = isAssignable(colName)
					if (updatable && primaryKeys!!.contains(colName)) {
						updatable = false
					}
					if (updatable) {
						updateParams[colName] = OwnGrammarHelper.makeDbValue(field[this])
					}
				}
			}
		}
		catch (e: Exception) {
			throw DkRuntimeException(this, e, "Failed to calculate update params")
		}
		if (enableStrictMode && updateParams.size == 0) {
			throw DkRuntimeException(this, "Must provide params which can be updated at strict-mode")
		}
		return updateParams
	}

	/**
	 * Check given column is writable by model-config (fillableColumns, guardedColumns).
	 */
	protected fun isAssignable(colName: String): Boolean {
		// Can set
		return if (fillableColumns != null) {
			fillableColumns!!.contains(colName)
		}
		else guardedColumns == null || !guardedColumns!!.contains(colName)
		// Not be protected
	}

	/**
	 * Set value for the field which has name match with given `name` via reflection in this model.
	 *
	 * @param name Name of column (field).
	 * @param value Value for column (field).
	 */
	protected fun setAttribute(name: String, value: Any?) {
		try {
			val name2field = findAndCacheFields(javaClass)
			val field = name2field!![name]
			if (field != null) {
				field[this] = value
			}
			else {
				DkLogcats.warning(this, "Field not found for given name: $name")
			}
		}
		catch (e: Exception) {
			throw DkRuntimeException(this, e, "Could not setAttribute, name: $name")
		}
	}

	private fun findAndCacheFields(modelClass: Class<*>): ArrayMap<String, Field>? {
		val cacheKey = modelClass.name
		var name2field: ArrayMap<String, Field>?
		synchronized(fieldCache) {
			name2field = fieldCache[cacheKey]
			if (name2field == null) {
				name2field = ArrayMap()
				val fields = DkReflectionFinder.getIns().findFields(javaClass, DkColumnInfo::class.java)
				for (field in fields) {
					val colInfo = field.getAnnotation(DkColumnInfo::class.java)
						?: throw DkRuntimeException(this, "Oops, colInfo cannot be null !")
					name2field!![colInfo.name] = field
				}
				fieldCache[cacheKey] = name2field
			}
		}
		return name2field
	}

	companion object {
		// We recommend each model should contain a rowid named with `id` as primary key (pk).
		const val DEFAULT_ROWID = "id"

		// Reflection-cache for all models
		protected val fieldCache = ArrayMap<String, ArrayMap<String, Field>>()
	}
}
