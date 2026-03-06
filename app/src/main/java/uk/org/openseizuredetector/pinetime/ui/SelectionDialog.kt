package uk.org.openseizuredetector.pinetime.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class SelectionDialogItem(
    val id: String,
    val title: String,
    val supportingText: String? = null
)

@Composable
fun SingleChoiceSelectionDialog(
    title: String,
    hint: String,
    items: List<SelectionDialogItem>,
    selectedItemId: String?,
    onItemSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmText: String,
    dismissText: String,
    confirmEnabled: Boolean,
    emptyStateContent: (@Composable () -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                if (items.isEmpty()) {
                    emptyStateContent?.invoke()
                } else {
                    LazyColumn {
                        items(items, key = { it.id }) { item ->
                            ListItem(
                                headlineContent = { Text(item.title) },
                                supportingContent = {
                                    item.supportingText?.let { Text(it) }
                                },
                                trailingContent = {
                                    RadioButton(
                                        selected = selectedItemId == item.id,
                                        onClick = { onItemSelected(item.id) }
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onItemSelected(item.id) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = confirmEnabled, onClick = onConfirm) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        }
    )
}

