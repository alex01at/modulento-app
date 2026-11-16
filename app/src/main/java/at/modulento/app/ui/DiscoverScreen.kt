package at.modulento.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import at.modulento.app.R
import at.modulento.app.data.ServerUrl

/** The public website; the button opens the account area with a logged-in web session. */
@Composable
fun DiscoverScreen(title: String, serverUrl: String, onOpenAccount: () -> Unit) {
    Scaffold(topBar = { BrandTopBar(title) }) { padding ->
        Column(Modifier.padding(padding).consumeWindowInsets(padding).imePadding()) {
            FilledTonalButton(
                onClick = onOpenAccount,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(stringResource(R.string.discover_open_account))
            }
            SiteWebView(url = ServerUrl.page(serverUrl, "/"), modifier = Modifier.weight(1f))
        }
    }
}
