package tool.compet.database

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import androidx.collection.ArrayMap
import tool.compet.core.DkLogcats
import tool.compet.core.DkTypeHelper
import tool.compet.core.DkUtils
import tool.compet.core.isEmptyDk
import tool.compet.core.parseBooleanDk
import tool.compet.core.parseDoubleDk
import tool.compet.core.parseFloatDk
import tool.compet.core.parseIntDk
import tool.compet.core.parseLongDk
import tool.compet.reflection.DkReflectionFinder
import java.lang.reflect.Field

abstract class TheSqliteConnection : TheConnection {
	/**
	 * Requires readable database. Normally, this return read/write database instance,
	 * but for some problem (full disk...), the read only database was returned.
	 * So call this is maybe same with `getWritableDatabase()` in general.
	 *
	 * @throws SQLiteException If cannot open database.
	 */
	@get:Throws(SQLiteException::class)
	abstract val readableDatabase: SQLiteDatabase

	/**
	 * Requires read/write database. This attempts to get write-database instance,
	 * but sometimes maybe failed since some problems (full disk...), so exception
	 * maybe raised.
	 *
	 * @throws SQLiteException If cannot open database for writing.
	 */
	@get:Throws(SQLiteException::class)
	abstract val writableDatabase: SQLiteDatabase

	fun rawQuery(sql: String): Cursor {
		if (BuildConfig.DEBUG) {
			DkLogcats.info(this, "Query: $sql")
		}
		return readableDatabase.rawQuery(sql, null)
	}

	override fun <T> query(sql: String, rowType: Class<T>): List<T> {
		rawQuery(sql).use { cursor ->  // Autoclose
			val rows: MutableList<T> = ArrayList()

			if (cursor.moveToFirst()) {
				// Cache fields since it is called usually
				var fields = rowFieldCache[rowType.name]
				if (fields == null) {
					fields = DkReflectionFinder.getIns().findFields(rowType, DkColumnInfo::class.java)
					rowFieldCache[rowType.name] = fields
				}
				if (fields.isEmptyDk()) {
					DkUtils.complainAt(
						this,
						"Must annotate some fields with `@DkColumnInfo` in model `%s`",
						rowType.name
					)
				}
				do {
					rows.add(row2obj(cursor, rowType, fields))
				}
				while (cursor.moveToNext())
			}

			return rows
		}
	}

	override fun execute(sql: String) {
		if (BuildConfig.DEBUG) {
			DkLogcats.info(this, "Execute: $sql")
		}
		writableDatabase.execSQL(sql)
	}

	/**
	 * Convert row data in given `cursor` to object which has type is given model class.
	 *
	 * @throws RuntimeException When something happen
	 */
	// ignore nullable when obtain annotation
	private fun <T> row2obj(cursor: Cursor, modelClass: Class<T>, fields: List<Field>): T {
		val model: T = try {
			modelClass.newInstance()
		}
		catch (e: Exception) {
			throw RuntimeException(e)
		}

		// Each field has annotated with DkColumnInfo
		// so don't need to require the annotation when get
		for (field in fields) {
			try {
				val columnInfo = field.getAnnotation(DkColumnInfo::class.java)
				val colName: String = columnInfo!!.name
				val colIndex = cursor.getColumnIndex(colName)

				// Field is in model but NOT found in db -> caller does not query it
				if (colIndex < 0) {
					continue
				}
				val value = cursor.getString(colIndex)
				val fieldType = field.type

				when (DkTypeHelper.typeMasked(fieldType)) {
					DkTypeHelper.TYPE_BOOLEAN_MASKED -> {
						field[model] = value.parseBooleanDk()
					}
					DkTypeHelper.TYPE_SHORT_MASKED -> {
						field[model] = value.parseIntDk()
					}
					DkTypeHelper.TYPE_INTEGER_MASKED -> {
						field[model] = value.parseIntDk()
					}
					DkTypeHelper.TYPE_LONG_MASKED -> {
						field[model] = value.parseLongDk()
					}
					DkTypeHelper.TYPE_FLOAT_MASKED -> {
						field[model] = value.parseFloatDk()
					}
					DkTypeHelper.TYPE_DOUBLE_MASKED -> {
						field[model] = value.parseDoubleDk()
					}
					DkTypeHelper.TYPE_STRING_MASKED -> {
						field[model] = value
					}
					else -> {
						throw RuntimeException("Invalid type: " + fieldType.name)
					}
				}
			}
			catch (e: Exception) {
				DkLogcats.error(this, e)
			}
		}
		return model
	}

	companion object {
		// For faster convert DB-row vs POJO, we cache model fields after reflection
		private val rowFieldCache = ArrayMap<String, List<Field>>()
	}
}
