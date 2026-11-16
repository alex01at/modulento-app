package at.modulento.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import at.modulento.app.R

/** One site page, opened with a logged-in web session obtained through the handoff. */
@Composable
fun WebHandoffScreen(
    title: String,
    state: HandoffState,
    onRetry: () -> Unit,
    onSessionRejected: () -> Unit,
    onClose: () -> Unit,
) {
    LaunchedEffect(state) {
        if (state is HandoffState.SessionRejected) onSessionRejected()
    }

    Scaffold(topBar = { BrandTopBar(title, onBack = onClose) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
            when (state) {
                HandoffState.Loading, HandoffState.SessionRejected -> LoadingBox()
                is HandoffState.Ready -> SiteWebView(url = state.url)
                is HandoffState.Failed -> MessageBox(
                    title = stringResource(R.string.error_page_title),
                    text = stringResource(state.error.text),
                    primaryLabel = stringResource(R.string.action_retry),
                    onPrimary = onRetry,
                    secondaryLabel = stringResource(R.string.action_close),
                    onSecondary = onClose,
                )
            }
        }
    }
}
