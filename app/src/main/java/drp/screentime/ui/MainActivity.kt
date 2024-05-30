package drp.screentime.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import drp.screentime.firestore.FirestoreManager
import drp.screentime.ui.components.SaveNameBottomSheet
import drp.screentime.ui.components.UserCompetitionsScreen
import drp.screentime.ui.theme.ScreenTimeTheme

class MainActivity : ComponentActivity() {

    private val firestoreManager = FirestoreManager();

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
    val userId = "InPUKwlctve6bzgSVgfeK85g4p43"

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Screen Time") }, colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.primary,
            ), actions = {
                IconButton(onClick = {}) {
                    Icon(Icons.Filled.Add, contentDescription = "Create new competition")
                }
                IconButton(onClick = { showBottomSheet.value = true }) {
                    Icon(Icons.Filled.Settings, contentDescription = "Configure user settings")
                }
            })
        }
    ) { contentPadding ->
        Box(modifier = Modifier.padding(contentPadding)) {
            UserCompetitionsScreen(userId = userId)
        }

        if (showBottomSheet.value) {
            SaveNameBottomSheet(sheetState, showBottomSheet)
        }
    }
}