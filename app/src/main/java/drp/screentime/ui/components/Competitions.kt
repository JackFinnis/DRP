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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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

    fun fetchCompetitions() {
        firestoreManager.getEnrolledCompetitions(userId) { result ->
            competitions = result.ifEmpty {
                error = "Not enrolled in any competitions yet"
                emptyList()
            }
            loading = false
        }
    }

    // Initial fetch
    LaunchedEffect(userId) { fetchCompetitions() }

    if (!loading && error == null) {
        PullToRefreshBox(
            isRefreshing = loading, onRefresh = {
                loading = true
                fetchCompetitions()
            }, modifier = modifier.fillMaxSize(), state = pullRefreshState
        ) {
            CompetitionList(competitions)
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
fun CompetitionList(competitions: List<Competition>) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(competitions) { competition ->
            CompetitionItem(competition)
        }
    }
}

@Composable
fun CompetitionItem(competition: Competition) {
    Card(modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.outlinedCardElevation(),
        onClick = {}) {
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
}

@Composable
fun LeaderboardEntry(place: Int, userId: String, score: Int) {
    val firestoreManager = FirestoreManager()
    var userName by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        firestoreManager.getUserData(userId) { user ->
            userName = user?.let { "${it.firstName} ${it.lastName}" } ?: "Unknown User"
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
            Text(text = score.toString())
        }
    }
}