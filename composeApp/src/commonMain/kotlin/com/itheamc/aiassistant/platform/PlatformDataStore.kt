package com.itheamc.aiassistant.platform

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

internal const val dataStoreFileName = "ai_assistant.preferences_pb"

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class PlatformDataStore {
    fun get(): DataStore<Preferences>
}