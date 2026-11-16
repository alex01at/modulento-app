package at.modulento.app.ui

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import at.modulento.app.R
import at.modulento.app.data.ServerUrl

/** Without a login: the public website plus a visible way to sign in. */
@Composable
fun GuestScreen(serverUrl: String, onLogin: () -> Unit, onSettings: () -> Unit) {
    Scaffold(
        topBar = {
            BrandTopBar(
                title = stringResource(R.string.app_name),
                actions = {
                    TextButton(
                        onClick = onLogin,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text(stringResource(R.string.action_login))
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, stringResource(R.string.action_settings))
                    }
                },
            )
        },
    ) { padding ->
        SiteWebView(
            url = ServerUrl.page(serverUrl, "/"),
            modifier = Modifier.padding(padding).consumeWindowInsets(padding).imePadding(),
        )
    }
}
