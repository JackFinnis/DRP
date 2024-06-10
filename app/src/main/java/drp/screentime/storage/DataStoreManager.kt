package drp.screentime.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "user_preferences")

class DataStoreManager(private val context: Context) {
  enum class Key {
    USER_ID
  }

  suspend fun set(key: Key, value: String) {
    context.dataStore.edit { preferences -> preferences[stringPreferencesKey(key.name)] = value }
  }

  fun get(key: Key): Flow<String?> =
      context.dataStore.data.map { preferences -> preferences[stringPreferencesKey(key.name)] }
}
