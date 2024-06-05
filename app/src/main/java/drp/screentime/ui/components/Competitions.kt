package drp.screentime.ui.components

import android.content.Intent
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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import drp.screentime.firestore.FirestoreManager
import drp.screentime.firestore.User
import drp.screentime.util.formatDuration
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds


@ExperimentalMaterial3Api
@Composable
fun UserCompetitionsScreen(
    modifier: Modifier = Modifier,
    userId: String,
    firestoreManager: FirestoreManager = FirestoreManager(),
    showBottomSheet: MutableState<Boolean>,
    showAppBar: MutableState<Boolean>
) {
    var competitionId by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var fullLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val pullRefreshState = rememberPullToRefreshState()
    var showJoinCompetitionDialog by remember { mutableStateOf(false) }
    var inviteCode by remember { mutableStateOf("") }
    var showInviteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    fun fetchCompetitions() {
        firestoreManager.listenForUserDataChanges(userId) { user ->
            competitionId = user?.competitionId
            showAppBar.value = competitionId != null
            loading = false
            fullLoading = false

            if (competitionId != null) {
                firestoreManager.listenForCompetitionDataChanges(competitionId!!) { competition ->
                    inviteCode = competition?.inviteCode ?: ""
                }
            }
        }
    }

    // Initial fetch
    LaunchedEffect(userId) { fetchCompetitions() }

    var joinCompetitionCode by remember { mutableStateOf("") }
    if (showJoinCompetitionDialog) {
        AlertDialog(
            onDismissRequest = { showJoinCompetitionDialog = false },
            confirmButton = { TextButton(onClick = {
            // Handle submit action
                firestoreManager.enrollWithInviteCode(userId, joinCompetitionCode) {
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
            title = { Text("Enter invite code") },
            text = {
                TextField(
                    value = joinCompetitionCode,
                    onValueChange = { joinCompetitionCode = it },
                    label = { Text("Invite code") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
    }

    if (!(loading && fullLoading) && error == null) {
        Column(modifier = modifier) {
            Box(modifier = Modifier.weight(1f)) {
                if (loading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (competitionId != null) {
                    PullToRefreshBox(
                        isRefreshing = loading, onRefresh = {
                            loading = true
                            fetchCompetitions()
                        }, modifier = Modifier.fillMaxSize(), state = pullRefreshState
                    ) {
                        Box(modifier = Modifier) {
                            Leaderboard(competitionId!!, userId)
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row {
                    if (competitionId == null) {
                        MainScreenButton(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                // Add a new competition
                                firestoreManager.createCompetitionAndAddUser(userId) {
                                    fullLoading = true
                                    loading = true
                                    fetchCompetitions()
                                }
                            },
                            icon = Icons.Filled.AddCircle,
                            text = "Start Competition",
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
            } else {
                        MainScreenButton(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, inviteCode)
                                    type = "text/plain"
                                }

                                val shareIntent = Intent.createChooser(sendIntent, null)
                                context.startActivity(shareIntent)
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
            title = { Text("Invite code") }, text = { Text(inviteCode) },
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
                Text(text, style = typography.labelLarge)
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
                Text(text, style = typography.labelLarge)
            }
        }
}

@Composable
fun Leaderboard(competitionId: String, userId: String) {
    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    val firestoreManager = FirestoreManager()
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(competitionId) {
        firestoreManager.listenForCompetitionUpdates(competitionId) { newUsers ->
            users = newUsers.sortedBy { it.score }
            loading = false
        }
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
    ) {
        LazyColumn(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(
                users,
                key = { _, user -> user.id }
            ) { index, user ->
                Box(modifier = Modifier.animateItem()) {
                    LeaderboardEntry(
                        place = index + 1,
                        user = user,
                        isMe = user.id == userId
                    )
                }
            }
        }
    }
}

@Composable
fun LeaderboardEntry(
    place: Int,
    user: User,
    isMe: Boolean = false
) {
    val startTime = user.currentAppSince?.seconds ?: 0

    // number of seconds the user has been using the app
    var time by remember {
        mutableStateOf(0L)
    }

    LaunchedEffect(startTime) {
        while (true) { // isActive is true as long as the coroutine is active
            val diffInMillis = Timestamp.now().seconds - startTime
            time = diffInMillis
            delay(1.seconds)
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor =
            if (isMe) colorScheme.primary
            else colorScheme.secondaryContainer
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
                style = typography.labelMedium,
                color = if (isMe) colorScheme.onPrimary
                else colorScheme.secondary
            )
            if (user.currentApp == null)
                Text(
                    user.name,
                    style = typography.labelMedium,
                    color = if (isMe) colorScheme.onPrimary
                    else colorScheme.onSecondaryContainer
                )
            else
                Column {
                    Text(
                        user.name,
                        style = typography.labelMedium,
                        color = if (isMe) colorScheme.onPrimary
                        else colorScheme.onSecondaryContainer
                    )
                    if (!isMe) {
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "⦿",
                                style = typography.labelSmall,
                                color = if (isMe) colorScheme.onPrimary
                                else colorScheme.onSecondaryContainer
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Using ${user.currentApp} for ${formatDuration(time)}",
                                style = typography.labelSmall,
                                color = if (isMe) colorScheme.onPrimary
                                else colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            Spacer(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            Text(
                text = formatDuration(user.score),
                style = typography.labelMedium,
                color = if (isMe) colorScheme.onPrimary
                else colorScheme.onSecondaryContainer
            )
        }
    }
}
