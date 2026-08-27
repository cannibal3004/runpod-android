package com.canni.runpod.data.repo

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MigrationStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    fun save(podId: String, migrationId: String) {
        prefs.edit()
            .putString(KEY_POD_ID, podId)
            .putString(KEY_MIGRATION_ID, migrationId)
            .apply()
    }

    fun activeFor(podId: String): String? =
        if (prefs.getString(KEY_POD_ID, null) == podId) {
            prefs.getString(KEY_MIGRATION_ID, null)
        } else {
            null
        }

    fun clear() {
        prefs.edit().remove(KEY_POD_ID).remove(KEY_MIGRATION_ID).apply()
    }

    companion object {
        private const val PREFS_FILE = "runpod_migration"
        private const val KEY_POD_ID = "pod_id"
        private const val KEY_MIGRATION_ID = "migration_id"
    }
}
