package drp.screentime.ui

import android.app.usage.UsageStatsManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import drp.screentime.firestore.FirestoreManager
import drp.screentime.storage.DataStoreManager
import drp.screentime.ui.theme.ScreenTimeTheme
import drp.screentime.usage.UsageStatsProcessor
import kotlinx.coroutines.flow.firstOrNull
import java.util.Date

class DebugActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScreenTimeTheme {
                Scaffold {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(it)
                    ) {
                        UploadActivityButton()
                    }
                }
            }
        }
    }

    @Composable
    private fun UploadActivityButton() {
        val usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val usageStatsProcessor = UsageStatsProcessor(packageManager, usageStatsManager)
        FirestoreManager()

        // load user ID from local storage
        val dataStoreManager = DataStoreManager(this)
        var userId by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(Unit) {
            dataStoreManager.userIdFlow.firstOrNull()?.let { loaded ->
                userId = loaded
            }
        }

        Button(onClick = {
            if (userId == null) {
                return@Button
            } else {
                usageStatsProcessor.uploadDeviceActivity(userId!!, Date()) {

                }
            }
        }) {
            Text("Upload Activity")
        }
    }
}