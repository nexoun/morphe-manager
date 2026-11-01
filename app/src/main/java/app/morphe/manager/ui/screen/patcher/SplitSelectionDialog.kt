/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-manager
 */

package app.morphe.manager.ui.screen.patcher

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import app.morphe.manager.R
import app.morphe.manager.ui.model.SplitSelectionPrompt
import app.morphe.manager.ui.screen.shared.*
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.*

/** Uses the same themed shell, selection cards and action pills as the rest of the manager. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DialogHostScope.SplitSelectionDialog(
    request: SplitSelectionPrompt.Request,
    onConfirm: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val optional = request.modules.filterNot { it.isBase }
    val recommended = optional.filter { it.matchesDevice }.map { it.name }
    // ArrayList is Bundle-saveable, including across activity recreation while this is open.
    var selected by rememberSaveable(request) { mutableStateOf(ArrayList(recommended)) }
    AppDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.split_selection_title),
        description = request.archiveName,
        footer = {
            AppDialogButtonRow(
                primaryText = stringResource(R.string.continue_),
                onPrimaryClick = { onConfirm(selected.toSet()) },
                primaryEnabled = request.error == null,
                secondaryText = stringResource(android.R.string.cancel),
                onSecondaryClick = onDismiss
            )
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Defaults.SpaceSmall)) {
            if (request.error != null) {
                Text(
                    text = stringResource(R.string.split_selection_error, request.error),
                    color = dialogTextColor(),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    text = stringResource(R.string.split_selection_description),
                    color = dialogSecondaryTextColor(),
                    style = MaterialTheme.typography.bodyMedium
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Defaults.SpaceSmall),
                    verticalArrangement = Arrangement.spacedBy(Defaults.SpaceSmall)
                ) {
                    val allLabel = stringResource(R.string.split_selection_all)
                    ActionPillButton(
                        onClick = { selected = ArrayList(optional.map { it.name }) },
                        icon = MaterialSymbols.Rounded.Done_all,
                        contentDescription = allLabel,
                        label = allLabel
                    )
                    val deviceLabel = stringResource(R.string.split_selection_device)
                    ActionPillButton(
                        onClick = { selected = ArrayList(recommended) },
                        icon = MaterialSymbols.Rounded.Smartphone,
                        contentDescription = deviceLabel,
                        label = deviceLabel
                    )
                    val noneLabel = stringResource(R.string.split_selection_none)
                    ActionPillButton(
                        onClick = { selected = arrayListOf() },
                        icon = MaterialSymbols.Rounded.Remove_done,
                        contentDescription = noneLabel,
                        label = noneLabel
                    )
                }
                Text(
                    text = stringResource(R.string.split_selection_count, selected.size, optional.size),
                    color = dialogSecondaryTextColor(),
                    style = MaterialTheme.typography.labelMedium
                )
                request.modules.forEach { module ->
                    val checked = module.isBase || module.name in selected
                    RadioSelectionCard(
                        selected = checked,
                        enabled = !module.isBase,
                        role = Role.Checkbox,
                        onSelect = {
                            selected = ArrayList(
                                if (checked) selected - module.name else selected + module.name
                            )
                        },
                        leadingContent = {
                            SelectionCheckIndicator(ToggleableState(checked), enabled = !module.isBase)
                        }
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = module.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = dialogTextColor()
                            )
                            if (module.isBase || module.matchesDevice) {
                                Text(
                                    text = stringResource(
                                        if (module.isBase) R.string.split_selection_base
                                        else R.string.split_selection_recommended
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = dialogSecondaryTextColor()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
