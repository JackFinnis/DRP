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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import drp.screentime.firestore.Competition
import drp.screentime.firestore.FirestoreManager
import drp.screentime.util.formatDuration

@ExperimentalMaterial3Api
@Composable
fun UserCompetitionsScreen(
    modifier: Modifier = Modifier,
    userId: String,
    firestoreManager: FirestoreManager = FirestoreManager(),
    showBottomSheet: MutableState<Boolean>
) {
    var competitions by remember { mutableStateOf<List<Competition>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var fullLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val pullRefreshState = rememberPullToRefreshState()
    var showJoinCompetitionDialog by remember { mutableStateOf(false) }
    var inviteCode by remember { mutableStateOf("") }
    var showInviteDialog by remember { mutableStateOf(false) }

    fun fetchCompetitions() {
        firestoreManager.getEnrolledCompetitions(userId) { result ->
            competitions = result.ifEmpty {
                emptyList()
            }
            loading = false
            fullLoading = false
        }
    }

    // Initial fetch
    LaunchedEffect(userId) { fetchCompetitions() }

    if (showJoinCompetitionDialog) {
        AlertDialog(
            onDismissRequest = { showJoinCompetitionDialog = false },
            confirmButton = { TextButton(onClick = {
            // Handle submit action
                firestoreManager.enrollWithInviteCode(userId, inviteCode) {
                    fullLoading = true
                    loading = true
                    fetchCompetitions()
                }
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

    if (!(loading && fullLoading) && error == null) {
        Column(modifier = modifier) {
            Box(modifier = Modifier.weight(1f)) {
                if (!loading) {
                    PullToRefreshBox(
                        isRefreshing = loading, onRefresh = {
                            loading = true
                            fetchCompetitions()
                        }, modifier = Modifier.fillMaxSize(), state = pullRefreshState
                    ) {
                        Box(modifier = Modifier) {
                            CompetitionList(competitions, firestoreManager, userId)
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            if (competitions.isEmpty()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row {
                        MainScreenButton(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                // Add a new competition
                                firestoreManager.createCompetitionAndAddUser(userId, "Test") {
                                    fullLoading = true
                                    loading = true
                                    fetchCompetitions()
                                }
                            },
                            icon = Icons.Filled.AddCircle,
                            text = "Add Competition",
                            tonal = false
                        )
                        Spacer(Modifier.width(16.dp))
                        MainScreenButton(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                showJoinCompetitionDialog = true
                            },
                            icon = Icons.Filled.Person,
                            text = "Join Competition",
                            tonal = false
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Row {
                        MainScreenButton(
                            modifier = Modifier.weight(1f),
                            onClick = { showBottomSheet.value = true },
                            icon = Icons.Filled.Face,
                            text = "Edit Profile",
                            tonal = true
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row {
                        MainScreenButton(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                showInviteDialog = true
                            },
                            icon = Icons.Filled.AddCircle,
                            text = "Invite People",
                            tonal = false
                        )
                        Spacer(Modifier.width(16.dp))
                        MainScreenButton(
                            modifier = Modifier.weight(1f),
                            onClick = { showBottomSheet.value = true },
                            icon = Icons.Filled.Face,
                            text = "Edit Profile",
                            tonal = true
                        )
                    }
                }
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when {
                loading -> CircularProgressIndicator()
                error != null -> Text(text = error!!)
            }
        }
    }

    if (showInviteDialog) {
        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            title = { Text("Invite code") },
            text = { Text(if (competitions.isEmpty()) "" else competitions[0].inviteCode) },
            confirmButton = { TextButton(onClick = { showInviteDialog = false }) { Text("OK") } })
    }
}

@Composable
fun MainScreenButton(
    modifier: Modifier,
    onClick: () -> Unit,
    icon: ImageVector,
    text: String,
    tonal: Boolean
) {
    if (tonal)
        FilledTonalButton(
            shape = RoundedCornerShape(24.dp),
            contentPadding = PaddingValues(16.dp),
            modifier = modifier,
            onClick = onClick
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(icon, contentDescription = text, modifier = Modifier.size(36.dp))
                Spacer(Modifier.height(8.dp))
                Text(text, style = MaterialTheme.typography.labelLarge)
            }
        }
    else
        Button(
            shape = RoundedCornerShape(24.dp),
            contentPadding = PaddingValues(16.dp),
            modifier = modifier,
            onClick = onClick
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(icon, contentDescription = text, modifier = Modifier.size(36.dp))
                Spacer(Modifier.height(8.dp))
                Text(text, style = MaterialTheme.typography.labelLarge)
            }
        }
}

@Composable
fun CompetitionList(competitions: List<Competition>, firestoreManager: FirestoreManager, userId: String) {
    if (competitions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Not enrolled in any competitions")
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(competitions) { competition ->
                CompetitionItem(competition, firestoreManager, userId)
            }
        }
    }
}

@Composable
fun CompetitionItem(competition: Competition, firestoreManager: FirestoreManager, userId: String) {
    Box(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = competition.name,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(0.dp, 8.dp, 0.dp, 0.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (competition.leaderboard.isEmpty()) {
                Text(text = "No leaderboard data available")
            } else {
                competition.leaderboard.toList().sortedBy { pair: Pair<String, Int> -> pair.second }
                    .forEachIndexed { index, (user, score) ->
                    LeaderboardEntry(index + 1, user, score, user == userId)
                }
            }
        }
    }
}

@Composable
fun LeaderboardEntry(
    place: Int,
    userId: String,
    score: Int,
    isMe: Boolean
) {
    val firestoreManager = FirestoreManager()
    var userName by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var currentApp by remember { mutableStateOf<String?>(null)}
    var time by remember { mutableStateOf<Long>(0)}

    LaunchedEffect(userId) {
        firestoreManager.getUserData(userId) { user ->
            userName = user?.name ?: "Unknown User"
            loading = false
            currentApp = user?.currentApp
            time = Timestamp.now().seconds - (user?.currentAppSince?.seconds ?: 0)
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor =
            if (isMe) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.secondaryContainer
        ),
        onClick = {},
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp, 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                place.toString(),
                modifier = Modifier.width(36.dp),
                style = MaterialTheme.typography.labelMedium,
                color = if (isMe) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.secondary
            )
            if (currentApp == null)
                Text(
                    if (loading) "Loading..." else userName,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isMe) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSecondaryContainer
                )
            else
                Column {
                    Text(
                        if (loading) "Loading..." else userName,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isMe) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "⦿",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isMe) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Using $currentApp for ${formatDuration(time ?: 0)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isMe) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            Spacer(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            Text(
                text = formatDuration(score.toLong()),
                style = MaterialTheme.typography.labelMedium,
                color = if (isMe) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}
