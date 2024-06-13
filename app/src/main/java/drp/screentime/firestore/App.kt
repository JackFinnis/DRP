package drp.screentime.firestore

enum class App(val displayName: String, val packageName: String) {
  INSTAGRAM("Instagram", "com.instagram.android"),
  FACEBOOK("Facebook", "com.facebook.katana"),
  SNAPCHAT("Snapchat", "com.snapchat.android"),
  TIKTOK("TikTok", "com.zhiliaoapp.musically"),
  YOUTUBE("YouTube", "com.google.android.youtube"),
}
