package drp.screentime.storage

import android.content.Context

class StorageManager(context: Context) {
  enum class Key {
    APPS
  }

  val preferences =
      context.getSharedPreferences("drp.screentime.prefs", Context.MODE_PRIVATE)
}
