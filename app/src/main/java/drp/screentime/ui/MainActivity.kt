package drp.screentime.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.compose.AppTheme
import drp.screentime.App
import drp.screentime.firestore.FirestoreManager
import drp.screentime.storage.DataStoreManager
import drp.screentime.ui.components.LoadingView
import drp.screentime.ui.components.PermissionsView
import drp.screentime.ui.components.UserView

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent { AppTheme { AuthView() } }
  }
}

@Composable
fun AuthView() {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  var permissionsGranted by remember { mutableStateOf(App.areAllPermissionsGranted(context)) }

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        permissionsGranted = App.areAllPermissionsGranted(context)
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  if (permissionsGranted) {
    RootView()
  } else {
    PermissionsView()
  }
}

@Composable
fun RootView() {
  val context = LocalContext.current
  val dataStoreManager = remember { DataStoreManager(context) }
  var userId by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(Unit) {
    dataStoreManager.get(DataStoreManager.Key.USER_ID).collect { storedUserId ->
      if (storedUserId == null) {
        dataStoreManager.set(DataStoreManager.Key.USER_ID, FirestoreManager.getId())
      } else {
        userId = storedUserId
      }
    }
  }

  if (userId == null) {
    LoadingView()
  } else {
    UserView(userId!!)
  }
}
