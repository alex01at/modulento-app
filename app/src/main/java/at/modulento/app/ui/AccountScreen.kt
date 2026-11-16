package at.modulento.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import at.modulento.app.R
import at.modulento.app.data.api.Account

@Composable
fun AccountScreen(
    title: String,
    account: Account,
    serverUrl: String,
    onSettings: () -> Unit,
    onLogout: () -> Unit,
) {
    Scaffold(
        topBar = {
            BrandTopBar(
                title = title,
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, stringResource(R.string.action_settings))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            val none = stringResource(R.string.value_none)
            Field(R.string.account_display_name, account.displayName?.takeIf { it.isNotBlank() } ?: none)
            Field(R.string.account_email, account.email)
            Field(R.string.account_id, account.id.toString())
            Field(R.string.account_locale, account.locale.ifBlank { none })
            Field(R.string.account_created_at, account.createdAt.ifBlank { none })
            Field(
                R.string.account_demo_mode,
                stringResource(if (account.demoMode) R.string.value_yes else R.string.value_no),
            )
            HorizontalDivider()
            Field(R.string.account_server, serverUrl)
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                Text(stringResource(R.string.action_logout))
            }
        }
    }
}

@Composable
private fun Field(label: Int, value: String) {
    ListItem(
        overlineContent = { Text(stringResource(label)) },
        headlineContent = { Text(value) },
    )
}
