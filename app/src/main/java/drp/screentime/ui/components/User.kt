package drp.screentime.ui.components

import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import drp.screentime.firestore.FirestoreManager
import drp.screentime.firestore.User
import drp.screentime.storage.DataStoreManager
import drp.screentime.usage.UsageStatsProcessor
import drp.screentime.util.generateUserName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserView() {
  val sheetState = rememberModalBottomSheetState()
  val showBottomSheet = remember { mutableStateOf(false) }
  val context = LocalContext.current
  val dataStoreManager = remember { DataStoreManager(context) }
  val firestoreManager = remember { FirestoreManager() }
  val scope = rememberCoroutineScope()
  var userId by remember { mutableStateOf<String?>(null) }

  val usageStatsProcessor =
      UsageStatsProcessor(
          context.packageManager,
          context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager)

  LaunchedEffect(Unit) {
    // Load the user ID from DataStore
    dataStoreManager.userIdFlow.collect { storedUserId ->
      if (storedUserId == null) {
        createUser(firestoreManager, scope, dataStoreManager)
      } else {
        // Verify the user exists in Firestore
        firestoreManager.getUser(storedUserId) { user ->
          if (user == null) {
            // User doesn't exist, create a new user
            createUser(firestoreManager, scope, dataStoreManager)
          } else {
            userId = storedUserId
            postScreenTimeToDb(storedUserId, usageStatsProcessor)
          }
        }
      }
    }
  }

  if (userId == null) {
    Scaffold { contentPadding ->
      Box(
          modifier = Modifier.padding(contentPadding).fillMaxSize(),
          contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
          }
    }
  } else {
    Scaffold(
        topBar = {
          LargeTopAppBar(
              title = {
                Text(
                    "Leaderboard",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 0.dp),
                )
              })
        },
    ) { contentPadding ->
      CompetitionView(
          modifier = Modifier.padding(contentPadding),
          userId = userId!!,
          showBottomSheet = showBottomSheet)

      if (showBottomSheet.value) {
        EditNameView(sheetState, showBottomSheet, userId!!)
      }
    }
  }
}

private fun createUser(
    firestoreManager: FirestoreManager,
    scope: CoroutineScope,
    dataStoreManager: DataStoreManager
) {
  val name = generateUserName()
  firestoreManager.addUser(name) { newUserId ->
    newUserId?.let {
      scope.launch {
        dataStoreManager.saveUserId(it)
        dataStoreManager.saveUserName(name)
      }
    }
  }
}

fun postScreenTimeToDb(userId: String, usageStatsProcessor: UsageStatsProcessor) {
  FirestoreManager().setUserScore(userId, usageStatsProcessor.getTotalUsage()) {}

  val usageStats = usageStatsProcessor.getApplicationUsageStats()
  FirestoreManager().uploadUsageData(userId, usageStats) {}
}
