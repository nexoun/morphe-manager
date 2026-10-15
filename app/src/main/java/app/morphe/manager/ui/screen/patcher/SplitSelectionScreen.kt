package app.morphe.manager.ui.screen.patcher

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import app.morphe.manager.R
import app.morphe.manager.patcher.split.SplitApkPreparer
import app.morphe.manager.ui.viewmodel.SplitSelectionUiState

@Composable
fun SplitSelectionScreen(
    state: SplitSelectionUiState,
    onToggle: (String, Boolean) -> Unit,
    onRecommended: () -> Unit,
    onSelectAll: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.split_selection_title),
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = stringResource(R.string.split_selection_source, state.sourceName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.split_selection_description),
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onRecommended) {
                        Text(stringResource(R.string.split_selection_recommended))
                    }
                    TextButton(onClick = onSelectAll) {
                        Text(stringResource(R.string.split_selection_all))
                    }
                }
                Text(
                    text = stringResource(
                        R.string.split_selection_count,
                        state.selectedNames.size,
                        state.modules.size
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(state.modules, key = { it.name }) { module ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable(enabled = !module.mandatory) {
                                    onToggle(module.name, module.name !in state.selectedNames)
                                }
                                .padding(horizontal = 4.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = module.name in state.selectedNames || module.mandatory,
                                onCheckedChange = { onToggle(module.name, it) },
                                enabled = !module.mandatory
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = module.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = stringResource(moduleKindLabel(module.kind)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (module.mandatory) {
                                Text(
                                    text = stringResource(R.string.split_selection_required),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }

                Text(
                    text = stringResource(R.string.split_selection_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onCancel) {
                        Text(stringResource(R.string.split_selection_cancel))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(onClick = onConfirm, enabled = state.selectedNames.isNotEmpty()) {
                        Text(stringResource(R.string.split_selection_continue))
                    }
                }
            }
        }
    }
}

private fun moduleKindLabel(kind: SplitApkPreparer.ModuleKind): Int = when (kind) {
    SplitApkPreparer.ModuleKind.BASE -> R.string.split_selection_kind_base
    SplitApkPreparer.ModuleKind.ARCHITECTURE -> R.string.split_selection_kind_architecture
    SplitApkPreparer.ModuleKind.LOCALE -> R.string.split_selection_kind_locale
    SplitApkPreparer.ModuleKind.DENSITY -> R.string.split_selection_kind_density
    SplitApkPreparer.ModuleKind.FEATURE -> R.string.split_selection_kind_feature
    SplitApkPreparer.ModuleKind.OTHER -> R.string.split_selection_kind_other
}
