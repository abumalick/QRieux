package net.hilson.qrieux.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.hilson.qrieux.PlatformContext
import net.hilson.qrieux.currentTimeMillis
import net.hilson.qrieux.history.HistoryEntry
import net.hilson.qrieux.history.HistoryEntryType
import net.hilson.qrieux.history.clearHistory
import net.hilson.qrieux.history.deleteHistoryEntry
import net.hilson.qrieux.history.displayLabel
import net.hilson.qrieux.history.loadHistory
import net.hilson.qrieux.ui.theme.QRieuxUiConfig
import org.jetbrains.compose.resources.stringResource
import qr_scanner.composeapp.generated.resources.Res
import qr_scanner.composeapp.generated.resources.history_clear_all
import qr_scanner.composeapp.generated.resources.history_clear_cancel
import qr_scanner.composeapp.generated.resources.history_clear_confirm
import qr_scanner.composeapp.generated.resources.history_clear_yes
import qr_scanner.composeapp.generated.resources.history_created
import qr_scanner.composeapp.generated.resources.history_days_ago
import qr_scanner.composeapp.generated.resources.history_empty
import qr_scanner.composeapp.generated.resources.history_empty_desc
import qr_scanner.composeapp.generated.resources.history_hours_ago
import qr_scanner.composeapp.generated.resources.history_just_now
import qr_scanner.composeapp.generated.resources.history_minutes_ago
import qr_scanner.composeapp.generated.resources.history_scanned
import qr_scanner.composeapp.generated.resources.history_title
import qr_scanner.composeapp.generated.resources.history_yesterday

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    platformContext: PlatformContext,
    onEntryClick: (HistoryEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    var refreshKey by remember { mutableStateOf(0) }
    var entries by remember(refreshKey) { mutableStateOf(loadHistory(platformContext)) }
    var showClearDialog by remember { mutableStateOf(false) }

    fun refresh() { refreshKey++ }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.history_title)) },
                actions = {
                    if (entries.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = stringResource(Res.string.history_clear_all)
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (entries.isEmpty()) {
            EmptyState(modifier = Modifier.fillMaxSize().padding(paddingValues))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(entries, key = { _, e -> e.id }) { _, entry ->
                    SwipeableHistoryItem(
                        entry = entry,
                        onClick = { onEntryClick(entry) },
                        onDelete = {
                            deleteHistoryEntry(platformContext, entry.id)
                            refresh()
                        }
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(Res.string.history_clear_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    clearHistory(platformContext)
                    refresh()
                    showClearDialog = false
                }) {
                    Text(stringResource(Res.string.history_clear_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(Res.string.history_clear_cancel))
                }
            }
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(Res.string.history_empty),
            style = MaterialTheme.typography.titleMedium.copy(fontSize = QRieuxUiConfig.bodySize),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.history_empty_desc),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = QRieuxUiConfig.supportingSize),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableHistoryItem(
    entry: HistoryEntry,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    @Suppress("DEPRECATION")
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color by animateColorAsState(
                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart)
                    MaterialTheme.colorScheme.error
                else Color.Transparent
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, RoundedCornerShape(12.dp))
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        enableDismissFromStartToEnd = false
    ) {
        HistoryItemCard(entry = entry, onClick = onClick)
    }
}

@Composable
private fun HistoryItemCard(
    entry: HistoryEntry,
    onClick: () -> Unit
) {
    val isScan = entry.type == HistoryEntryType.SCAN
    val scannedLabel = stringResource(Res.string.history_scanned)
    val createdLabel = stringResource(Res.string.history_created)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isScan) Icons.Default.QrCodeScanner else Icons.Default.QrCode2,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayLabel(entry),
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = QRieuxUiConfig.bodySize),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isScan) scannedLabel else createdLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = QRieuxUiConfig.supportingSize),
                        color = if (isScan) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = relativeTime(entry.timestamp),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = QRieuxUiConfig.supportingSize),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun relativeTime(timestamp: Long): String {
    val now = currentTimeMillis()
    val diff = now - timestamp
    val minutes = diff / 60_000
    val hours = diff / 3_600_000
    val days = diff / 86_400_000

    return when {
        minutes < 1 -> stringResource(Res.string.history_just_now)
        minutes < 60 -> stringResource(Res.string.history_minutes_ago, minutes.toInt())
        hours < 24 -> stringResource(Res.string.history_hours_ago, hours.toInt())
        days < 2 -> stringResource(Res.string.history_yesterday)
        else -> stringResource(Res.string.history_days_ago, days.toInt())
    }
}
