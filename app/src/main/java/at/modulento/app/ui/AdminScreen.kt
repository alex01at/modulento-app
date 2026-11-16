package at.modulento.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.modulento.app.data.api.AdminGroup
import at.modulento.app.data.api.AdminItem

/** The server's admin menu, group by group; every entry opens in the in-app WebView. */
@Composable
fun AdminScreen(title: String, groups: List<AdminGroup>, onOpen: (AdminItem) -> Unit) {
    Scaffold(topBar = { BrandTopBar(title) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            groups.filter { it.items.isNotEmpty() }.forEach { group ->
                item(key = "group:${group.id}") {
                    Text(
                        group.label.ifBlank { group.id },
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
                    )
                }
                items(group.items) { item ->
                    ListItem(
                        headlineContent = { Text(item.label.ifBlank { item.path }) },
                        trailingContent = {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                        },
                        modifier = Modifier.clickable { onOpen(item) },
                    )
                }
                item { HorizontalDivider() }
            }
        }
    }
}
