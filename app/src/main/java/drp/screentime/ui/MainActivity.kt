package drp.screentime.ui

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.compose.AppTheme
import drp.screentime.App
import drp.screentime.firestore.FirestoreManager
import drp.screentime.firestore.User
import drp.screentime.storage.DataStoreManager
import drp.screentime.ui.components.SaveNameBottomSheet
import drp.screentime.ui.components.UserCompetitionsScreen
import drp.screentime.usage.UsageStatsProcessor
import drp.screentime.util.generateUserName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (!App.areAllPermissionsGranted(this)) {
            val intent = Intent(this, SetupActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        setContent {
            AppTheme {
                MainScreen()
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var showAppBar = remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val showBottomSheet = remember { mutableStateOf(false) }
    val context = LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context) }
    val firestoreManager = remember { FirestoreManager() }
    val scope = rememberCoroutineScope()

    var userId by remember { mutableStateOf<String?>(null) }

    val usageStatsProcessor = UsageStatsProcessor(
        context.packageManager,
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    )

    LaunchedEffect(Unit) {
        // Load the user ID from DataStore
        dataStoreManager.userIdFlow.collect { storedUserId ->
            if (storedUserId == null) {
                createUser(firestoreManager, scope, dataStoreManager)
            } else {
                // Verify the user exists in Firestore
                firestoreManager.getUserData(storedUserId) { user ->
                    if (user == null) {
                        // User doesn't exist, create a new user
                        createUser(firestoreManager, scope, dataStoreManager)
                    } else {
                        userId = storedUserId
                        postScreenTimeToDb(storedUserId, usageStatsProcessor, user)
                    }
                }
            }
        }
    }

    if (userId == null) Scaffold {
        Box(
            modifier = Modifier
                .padding(it)
                .fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else Scaffold(topBar = {
        LargeTopAppBar(title = {
            if (showAppBar.value) Text(
                "Leaderboard",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(8.dp, 0.dp),
                fontWeight = FontWeight.Medium
            )
        })
    }) { contentPadding ->
        Box(modifier = Modifier.padding(contentPadding)) {
            UserCompetitionsScreen(
                userId = userId!!,
                showBottomSheet = showBottomSheet,
                showAppBar = showAppBar
            )
        }
        if (showBottomSheet.value) {
            SaveNameBottomSheet(sheetState, showBottomSheet, userId!!)
        }
    }
}

private fun createUser(
    firestoreManager: FirestoreManager, scope: CoroutineScope, dataStoreManager: DataStoreManager
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

fun postScreenTimeToDb(userId: String, usageStatsProcessor: UsageStatsProcessor, user: User) {
    FirestoreManager().updateScore(userId, usageStatsProcessor.getTotalUsage()) {}

    val usageStats = usageStatsProcessor.getApplicationUsageStats()
    FirestoreManager().uploadUsageData(userId, usageStats) {}
}
