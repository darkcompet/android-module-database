/*
 * Copyright (c) 2017-2021 DarkCompet. All rights reserved.
 */
package tool.compet.database

/**
 * This is convenience for migration database.
 * To use it, subclass must provide methods `migrate_X_to_Y()` for reflection while migrating,
 * in here, `X` is some version, and `Y` is next of X version (X and Y are integers and not leading with zero).
 * For eg,. subclass should implement methods like: `migrate_0_to_1()`, `migrate_1_to_2()`, `migrate_2_to_3()`,...
 */
abstract class DkMigration {
	@Throws(Exception::class)
	abstract fun migrate(connection: TheConnection)
}
