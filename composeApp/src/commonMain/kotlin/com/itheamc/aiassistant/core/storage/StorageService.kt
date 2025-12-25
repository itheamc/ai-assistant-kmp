package com.itheamc.aiassistant.core.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.itheamc.aiassistant.platform.PlatformDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

/**
 * A service class for managing key-value pairs using [DataStore] with [Preferences].
 *
 * Provides methods to read, write, and remove preferences in a type-safe and suspendable way.
 */
class StorageService(
    private val platformDataStore: PlatformDataStore,
) {

    /**
     * Lazily initialized instance of [DataStore] for storing [Preferences].
     *
     * This uses `platformDataStore.get()` to provide the platform-specific DataStore implementation
     * (e.g., Android, iOS, or other supported KMP platforms). The instance is created only when
     * first accessed, ensuring efficient resource usage.
     *
     */
    private val datastore: DataStore<Preferences> by lazy { platformDataStore.get() }

    /**
     * A [Flow] that emits the current [Preferences] object whenever the stored preferences change.
     *
     * Can be collected to observe live updates of all stored preferences.
     */
    val preferences: Flow<Preferences> = datastore.data

    /**
     * Stores a value for the given [key] in the DataStore.
     *
     * @param key The [Preferences.Key] representing the preference to store.
     * @param value The value to be stored for the key.
     * @param onError Optional lambda to handle errors if storing fails. Receives the error message.
     */
    suspend fun <T> set(key: Preferences.Key<T>, value: T, onError: ((String?) -> Unit)? = null) {
        try {
            datastore.edit { preferences ->
                preferences[key] = value
            }
        } catch (e: Exception) {
            onError?.invoke(e.message)
        }
    }

    /**
     * Removes a value associated with the given [key] from the DataStore.
     *
     * @param key The [Preferences.Key] representing the preference to remove.
     * @param onError Optional lambda to handle errors if removal fails. Receives the error message.
     */
    suspend fun <T> remove(key: Preferences.Key<T>, onError: ((String?) -> Unit)? = null) {
        try {
            datastore.edit { preferences ->
                preferences.remove(key)
            }
        } catch (e: Exception) {
            onError?.invoke(e.message)
        }
    }

    /**
     * Retrieves the value associated with the given [key] from the DataStore.
     *
     * This is a suspend function that fetches the current value of the preference.
     * If the key does not exist or an error occurs, returns [defaultValue].
     *
     * @param key The [Preferences.Key] representing the preference to retrieve.
     * @param defaultValue The value to return if the key is not found or an error occurs. Defaults to `null`.
     * @return The value of type [T] associated with the key, or [defaultValue] if not found.
     */
    suspend fun <T> get(key: Preferences.Key<T>, defaultValue: T? = null): T? {
        return try {
            datastore.data
                .map { preferences -> preferences[key] }
                .firstOrNull() ?: defaultValue
        } catch (e: Exception) {
            defaultValue
        }
    }

    /**
     * Clears all preferences stored in the DataStore.
     *
     * This is a suspend function that clears all preferences
     *
     * @param onSuccess Optional lambda to execute after successful clearing.
     * @param onError Optional lambda to handle errors if clearing fails. Receives the error message.
     */
    suspend fun clear(
        onSuccess: (suspend () -> Unit)? = null,
        onError: (suspend (String?) -> Unit)? = null
    ) {
        try {
            // Clear the datastore
            datastore.edit { preferences ->
                preferences.clear()
            }
            onSuccess?.invoke()
        } catch (e: Exception) {
            onError?.invoke(e.message)
        }
    }
}