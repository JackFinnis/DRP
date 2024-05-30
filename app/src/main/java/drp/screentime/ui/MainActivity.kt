package drp.screentime.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import drp.screentime.firestore.FirestoreManager
import drp.screentime.ui.components.SaveNameBottomSheet
import drp.screentime.ui.components.UsageStatsScreen
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

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("Edit name") },
                icon = { Icon(Icons.Filled.Edit, contentDescription = "Edit my name") },
                onClick = {
                    showBottomSheet.value = true
                }
            )
        }
    ) { contentPadding ->
        Box(modifier = Modifier.padding(contentPadding)) {
            UsageStatsScreen()
        }

        if (showBottomSheet.value) {
            SaveNameBottomSheet(sheetState, showBottomSheet)
        }
    }
}