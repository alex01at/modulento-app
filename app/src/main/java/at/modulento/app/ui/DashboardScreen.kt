package at.modulento.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import at.modulento.app.R
import at.modulento.app.data.api.Meta
import at.modulento.app.data.api.Notice

@Composable
fun DashboardScreen(
    title: String,
    meta: Meta,
    onRefresh: () -> Unit,
    onOpenAdmin: () -> Unit,
    onOpenDiscover: () -> Unit,
    onOpenNotice: (Notice) -> Unit,
) {
    Scaffold(
        topBar = {
            BrandTopBar(
                title = title,
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, stringResource(R.string.action_refresh))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                val name = meta.account.displayName?.takeIf { it.isNotBlank() } ?: meta.account.email
                Text(
                    stringResource(R.string.dashboard_greeting, name),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }

            if (meta.notices.isNotEmpty()) {
                item {
                    Text(
                        stringResource(R.string.dashboard_notices),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                items(meta.notices) { notice ->
                    // Only local paths can go through the handoff; anything else is shown, not linked.
                    val opens = notice.link?.startsWith("/") == true
                    DashboardCard(
                        headline = notice.label,
                        badge = notice.count.takeIf { it > 0 },
                        onClick = if (opens) ({ onOpenNotice(notice) }) else null,
                    )
                }
            }

            if (meta.hasAdminMenu) {
                item {
                    DashboardCard(
                        headline = stringResource(R.string.nav_admin),
                        supporting = stringResource(R.string.dashboard_admin_text, meta.adminMenu.size),
                        onClick = onOpenAdmin,
                    )
                }
            }
            item {
                DashboardCard(
                    headline = stringResource(R.string.nav_discover),
                    supporting = stringResource(R.string.dashboard_discover_text),
                    onClick = onOpenDiscover,
                )
            }
            if (meta.coreVersion.isNotBlank()) {
                item {
                    Text(
                        stringResource(R.string.dashboard_version, meta.coreVersion, meta.apiVersion),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardCard(
    headline: String,
    supporting: String? = null,
    badge: Int? = null,
    onClick: (() -> Unit)?,
) {
    val content: @Composable () -> Unit = {
        ListItem(
            headlineContent = { Text(headline) },
            supportingContent = supporting?.let { { Text(it) } },
            leadingContent = badge?.let {
                { Badge { Text(it.toString(), modifier = Modifier.padding(horizontal = 2.dp)) } }
            },
            trailingContent = if (onClick != null) {
                { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) }
            } else {
                null
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
    if (onClick != null) {
        Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Column { content() } }
    } else {
        Card(modifier = Modifier.fillMaxWidth()) { Column { content() } }
    }
}
