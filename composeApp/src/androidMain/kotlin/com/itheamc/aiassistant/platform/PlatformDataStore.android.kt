package com.itheamc.aiassistant.platform

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

@Suppress(names = ["EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING"])
actual class PlatformDataStore(private val context: Context) {
    // Lazy initialization ensures single instance
    private val dataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.createWithPath(
            produceFile = {
                context.filesDir.resolve(dataStoreFileName).absolutePath.toPath()
            }
        )
    }

    actual fun get(): DataStore<Preferences> = dataStore
}