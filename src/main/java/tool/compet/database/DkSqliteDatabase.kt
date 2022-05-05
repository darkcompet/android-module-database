/*
 * Copyright (c) 2017-2021 DarkCompet. All rights reserved.
 */
package tool.compet.database

import android.content.Context
import android.database.sqlite.SQLiteOpenHelper
import tool.compet.core.DkStrings

/**
 * This handles Sqlite database.
 */
abstract class DkSqliteDatabase protected constructor(
	context: Context,
	protected val name: String,
	protected val version: Int
) : SQLiteOpenHelper(context, name, null, version), TheDatabase {
	/**
	 * Delete current app database.
	 */
	fun deleteDatabase(context: Context) {
		context.deleteDatabase(name)
	}

	/**
	 * Delete a table from current app database.
	 */
	fun dropTable(tableName: String) {
		writableDatabase.execSQL("drop table if exists " + OwnGrammarHelper.safeName(tableName))
	}

	/**
	 * @return Number of table inside current app database.
	 */
	fun tableCount(): Int {
		val query =
			"select count(*) from `sqlite_master` where `type` = 'table' and `name` != 'android_metadata' and `name` != 'sqlite_sequence'"
		readableDatabase.use { db ->
			db.rawQuery(query, null).use { cursor ->
				return if (cursor != null && cursor.moveToFirst()) {
					cursor.getInt(0)
				}
				else 0
			}
		}
	}

	/**
	 * Check existence of given table in current app database.
	 */
	fun hasTable(tableName: String): Boolean {
		val tabName = OwnGrammarHelper.safeName(tableName)
		val query = DkStrings.format(
			"select count(`_rowid_`) from `sqlite_master` where `name` = %s and `type` = 'table'",
			tabName
		)
		readableDatabase.use { db ->
			db.rawQuery(query, null)
				.use { cursor -> return cursor != null && cursor.moveToFirst() && cursor.getInt(0) > 0 }
		}
	}
}
