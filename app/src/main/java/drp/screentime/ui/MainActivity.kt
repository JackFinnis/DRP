package drp.screentime.ui

import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import drp.screentime.firestore.FirestoreManager
import drp.screentime.storage.DataStoreManager
import drp.screentime.ui.components.SaveNameBottomSheet
import drp.screentime.ui.components.UserCompetitionsScreen
import drp.screentime.ui.theme.ScreenTimeTheme
import drp.screentime.usage.UsageStatsProcessor
import drp.screentime.util.generateFirstName
import drp.screentime.util.generateLastName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScreenTimeTheme {
                MainScreen()
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
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
                        postScreenTimeToDb(storedUserId, usageStatsProcessor)
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
    } else Scaffold(
        topBar = {
            TopAppBar(title = { Text("Screen Time") }, colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.primary,
            ), actions = {
                IconButton(onClick = {
                    postScreenTimeToDb(userId!!, usageStatsProcessor)
                }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh screen time")
                }
                IconButton(onClick = { showBottomSheet.value = true }) {
                    Icon(Icons.Filled.Settings, contentDescription = "Configure user settings")
                }
            })
        }
    ) { contentPadding ->
        Box(modifier = Modifier.padding(contentPadding)) {
            UserCompetitionsScreen(userId = userId!!)
        }

        if (showBottomSheet.value) {
            SaveNameBottomSheet(sheetState, showBottomSheet, userId!!)
        }
    }
}

private fun createUser(
    firestoreManager: FirestoreManager, scope: CoroutineScope, dataStoreManager: DataStoreManager
) {
    val firstName = generateFirstName()
    val lastName = generateLastName()
    firestoreManager.addUser(firstName, lastName) { newUserId ->
        newUserId?.let {
            scope.launch {
                dataStoreManager.saveUserId(it)
                dataStoreManager.saveUserName("$firstName $lastName")
            }
        }

        // Enroll in the single competition
        val competition = "QUQYodR01IUu1iaj4qKn"
        firestoreManager.enrollInCompetition(newUserId!!, competition) { }
        firestoreManager.addUserToCompetition(newUserId, competition) { }
    }
}

fun postScreenTimeToDb(userId: String, usageStatsProcessor: UsageStatsProcessor) {
    FirestoreManager().updateScore(
        "QUQYodR01IUu1iaj4qKn", userId, usageStatsProcessor.getTotalUsage().toInt()
    ) {}
}
