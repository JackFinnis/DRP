package drp.screentime.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun LoadingView() {
  Scaffold { contentPadding ->
    Box(
        modifier = Modifier.padding(contentPadding).fillMaxSize(),
        contentAlignment = Alignment.Center) {
          CircularProgressIndicator()
        }
  }
}
