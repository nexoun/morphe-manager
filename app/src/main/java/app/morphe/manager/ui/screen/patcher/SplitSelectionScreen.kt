package app.morphe.manager.ui.screen.patcher

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.morphe.manager.R
import app.morphe.manager.patcher.split.SplitApkPreparer
import app.morphe.manager.ui.screen.shared.AppDialog
import app.morphe.manager.ui.screen.shared.DialogPadding
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
    val recommendedAlreadySelected = state.modules.all { module ->
        (module.recommended || module.mandatory) == (module.name in state.selectedNames)
    }
    val allAlreadySelected = state.modules.isNotEmpty() && state.modules.all { module ->
        module.mandatory || module.name in state.selectedNames
    }

    AppDialog(
        onDismissRequest = onCancel,
        title = stringResource(R.string.split_selection_title),
        description = stringResource(R.string.split_selection_source, state.sourceName),
        footer = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.split_selection_cancel))
                }
                Button(
                    onClick = onConfirm,
                    enabled = state.selectedNames.isNotEmpty() || state.modules.any { it.mandatory }
                ) {
                    Text(stringResource(R.string.split_selection_continue))
                }
            }
        },
        scrollable = false,
        padding = DialogPadding.Compact,
        contentArrangement = Arrangement.Top,
        fillContentHeight = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.split_selection_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Match the patch selector's compact, icon-led bulk actions.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onRecommended,
                    enabled = !recommendedAlreadySelected,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ThumbUp,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.split_selection_recommended),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                OutlinedButton(
                    onClick = onSelectAll,
                    enabled = !allAlreadySelected,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DoneAll,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.split_selection_all),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(
                        R.string.split_selection_count,
                        state.selectedNames.size,
                        state.modules.size
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(state.modules, key = { it.name }) { module ->
                    val selected = module.name in state.selectedNames || module.mandatory
                    val itemAlpha by animateFloatAsState(
                        targetValue = if (selected) 1f else 0.43f,
                        animationSpec = tween(durationMillis = 180),
                        label = "split-module-selection-alpha"
                    )
                    val shape = RoundedCornerShape(12.dp)

                    // Like Morphe's patch cards, selection is indicated by emphasis rather
                    // than a checkbox. Unselected modules remain tappable, but are subdued.
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { alpha = itemAlpha }
                            .toggleable(
                                value = selected,
                                enabled = !module.mandatory,
                                role = Role.Checkbox,
                                onValueChange = { checked -> onToggle(module.name, checked) }
                            ),
                        shape = shape,
                        color = if (selected) {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.46f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f)
                        },
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.52f)
                            } else {
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = module.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = stringResource(moduleKindLabel(module.kind)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (module.mandatory) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                ) {
                                    Text(
                                        text = stringResource(R.string.split_selection_required),
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Text(
                text = stringResource(R.string.split_selection_warning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
