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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                Box(modifier = Modifier.weight(1f)) {
                    CompetitionList(competitions, firestoreManager, userId)
                }
                Column(modifier = Modifier.padding(16.dp)) {
                    Row {
                        MainScreenButton(
                            modifier = modifier.weight(1f),
                            onClick = {},
                            icon = Icons.Filled.Info,
                            text = "Insights"
                        )
                        Spacer(Modifier.width(16.dp))
                        MainScreenButton(
                            modifier = modifier.weight(1f),
                            onClick = {
                                // Add a new competition
                                firestoreManager.createCompetitionAndAddUser(userId, "Test") { }
                                loading = true
                                fetchCompetitions()
                            },
                            icon = Icons.Filled.AddCircle,
                            text = "Add Competition"
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Row {
                        MainScreenButton(
                            modifier = modifier.weight(1f),
                            onClick = {
                                showJoinCompetitionDialog = true
                            },
                            icon = Icons.Filled.Person,
                            text = "Join Competition"
                        )
                        Spacer(Modifier.width(16.dp))
                        MainScreenButton(
                            modifier = modifier.weight(1f),
                            onClick = {},
                            icon = Icons.Filled.Face,
                            text = "Edit Profile"
                        )
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
fun MainScreenButton(modifier: Modifier, onClick: () -> Unit, icon: ImageVector, text: String) {
    Button(
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(12.dp),
        modifier = modifier,
        onClick = onClick
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = text, modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(4.dp))
            Text(text, fontSize = 16.sp)
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
