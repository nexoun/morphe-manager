package app.morphe.manager.ui.screen.patcher

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
    val selectedCount = state.modules.count { module ->
        module.mandatory || module.name in state.selectedNames
    }
    val recommendedAlreadySelected = state.modules
        .filterNot { it.mandatory }
        .all { module -> module.recommended == (module.name in state.selectedNames) }
    val allAlreadySelected = state.modules.isNotEmpty() && state.modules.all { module ->
        module.mandatory || module.name in state.selectedNames
    }

    AppDialog(
        onDismissRequest = onCancel,
        title = stringResource(R.string.split_selection_title),
        footer = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(percent = 50)
                ) {
                    Text(stringResource(R.string.split_selection_cancel))
                }
                Button(
                    onClick = onConfirm,
                    enabled = selectedCount > 0,
                    shape = RoundedCornerShape(percent = 50)
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Match the patch picker's app-name/count header with a quiet tonal surface.
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = state.sourceName,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        shape = RoundedCornerShape(percent = 50),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = stringResource(
                                R.string.split_selection_count,
                                selectedCount,
                                state.modules.size
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1
                        )
                    }
                }
            }

            // The patch picker uses a short row of icon-only pill actions. Keep these
            // compact and theme-driven rather than stretching labelled buttons across the dialog.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SplitSelectionActionButton(
                    onClick = onSelectAll,
                    enabled = !allAlreadySelected,
                    label = stringResource(R.string.split_selection_all),
                    icon = Icons.Outlined.DoneAll,
                    iconColor = MaterialTheme.colorScheme.primary
                )
                SplitSelectionActionButton(
                    onClick = onRecommended,
                    enabled = !recommendedAlreadySelected,
                    label = stringResource(R.string.split_selection_recommended),
                    icon = Icons.Outlined.ThumbUp,
                    iconColor = MaterialTheme.colorScheme.tertiary
                )

            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.modules, key = { it.name }) { module ->
                    val selected = module.mandatory || module.name in state.selectedNames
                    val itemAlpha by animateFloatAsState(
                        targetValue = if (selected) 1f else 0.38f,
                        animationSpec = tween(durationMillis = 160),
                        label = "split-module-selection-alpha"
                    )

                    // Use the normal Material card surface, with selection communicated the
                    // same way as the patch list: enabled items stay clear; disabled ones fade.
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(itemAlpha)
                            .toggleable(
                                value = selected,
                                enabled = !module.mandatory,
                                role = Role.Checkbox,
                                onValueChange = { checked -> onToggle(module.name, checked) }
                            ),
                        colors = CardDefaults.cardColors(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = module.name,
                                    style = MaterialTheme.typography.bodyLarge,
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
                                Text(
                                    text = stringResource(R.string.split_selection_required),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1
                                )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SplitSelectionActionButton(
    onClick: () -> Unit,
    enabled: Boolean,
    label: String,
    icon: ImageVector,
    iconColor: Color
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState()
    ) {
        FilledTonalButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(width = 56.dp, height = 40.dp),
            shape = RoundedCornerShape(percent = 50),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                contentColor = iconColor
            )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(18.dp)
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
