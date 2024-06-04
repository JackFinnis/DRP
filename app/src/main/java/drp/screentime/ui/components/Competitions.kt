package drp.screentime.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import drp.screentime.firestore.Competition
import drp.screentime.firestore.FirestoreManager
import drp.screentime.util.formatDuration

@ExperimentalMaterial3Api
@Composable
fun UserCompetitionsScreen(
    modifier: Modifier = Modifier,
    userId: String,
    firestoreManager: FirestoreManager = FirestoreManager()
) {
    var competitions by remember { mutableStateOf<List<Competition>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val pullRefreshState = rememberPullToRefreshState()
    var showJoinCompetitionDialog by remember { mutableStateOf(false) }
    var inviteCode by remember { mutableStateOf("") }

    fun fetchCompetitions() {
        firestoreManager.getEnrolledCompetitions(userId) { result ->
            competitions = result.ifEmpty {
                emptyList()
            }
            loading = false
        }
    }

    // Initial fetch
    LaunchedEffect(userId) { fetchCompetitions() }

    if (showJoinCompetitionDialog) {
        AlertDialog(
            onDismissRequest = { showJoinCompetitionDialog = false },
            confirmButton = { TextButton(onClick = {
            // Handle submit action
            firestoreManager.enrollWithInviteCode(userId, inviteCode) { }
            showJoinCompetitionDialog = false
        }) {
            Text("Join")
        } },
            dismissButton = { TextButton(onClick = {
                showJoinCompetitionDialog = false
            }) {
            Text("Cancel")
        } },
            title = { Text("Enter Join Code") },
            text = {
                TextField(
                    value = inviteCode,
                    onValueChange = { inviteCode = it },
                    label = { Text("Invite Code") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
    }

    if (!loading && error == null) {
        PullToRefreshBox(
            isRefreshing = loading, onRefresh = {
                loading = true
                fetchCompetitions()
            }, modifier = modifier.fillMaxSize(), state = pullRefreshState
        ) {
            Column {
                CompetitionList(competitions, firestoreManager, userId)
                Row {
                    Button(
                        onClick = {
                            // Add a new competition
                            firestoreManager.createCompetitionAndAddUser(userId, "Test") { }
                            loading = true
                            fetchCompetitions()
                        }) {
                        Text("Add Competition")
                    }
                    Button(
                        onClick = {
                            showJoinCompetitionDialog = true
                        }) {
                        Text("Join Competition")
                    }
                }
            }
        }
    } else {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when {
                loading -> CircularProgressIndicator()
                error != null -> Text(text = error!!)
            }
        }
    }
}

@Composable
fun CompetitionList(competitions: List<Competition>, firestoreManager: FirestoreManager, userId: String) {
    if (competitions.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(text = "Not enrolled in any competitions")
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(competitions) { competition ->
                CompetitionItem(competition, firestoreManager, userId)
            }
        }
    }
}

@Composable
fun CompetitionItem(competition: Competition, firestoreManager: FirestoreManager, userId: String) {
    var showDialog by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.outlinedCardElevation(),
        onClick = { showDialog = true}
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = competition.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (competition.leaderboard.isEmpty()) {
                Text(text = "No leaderboard data available")
            } else {
                competition.leaderboard.toList().forEachIndexed { index, (user, score) ->
                    LeaderboardEntry(index + 1, user, score)
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Invite code") },
            text = { Text(competition.inviteCode) },
            confirmButton = { TextButton(onClick = { showDialog = false }) { Text("OK") } })
    }
}

@Composable
fun LeaderboardEntry(place: Int, userId: String, score: Int) {
    val firestoreManager = FirestoreManager()
    var userName by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        firestoreManager.getUserData(userId) { user ->
            userName = user?.name ?: "Unknown User"
            loading = false
        }
    }

    if (loading) {
        Row {
            Text(text = place.toString(), modifier = Modifier.padding(end = 16.dp))
            Text(text = "Loading...")
            Spacer(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            Text(text = score.toString())
        }
    } else {
        Row {
            Text(text = place.toString(), modifier = Modifier.padding(end = 16.dp))
            Text(text = userName)
            Spacer(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            Text(text = formatDuration(score.toLong()))
        }
    }
}
