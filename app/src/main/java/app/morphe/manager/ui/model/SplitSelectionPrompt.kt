/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-manager
 */

package app.morphe.manager.ui.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.morphe.manager.patcher.runtime.deviceConfig
import app.morphe.manager.patcher.split.SplitApkPreparer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** A suspended preflight question shared by single-app and batch patching. */
class SplitSelectionPrompt {
    data class Request(
        val archiveName: String,
        val modules: List<SplitApkPreparer.Module>,
        val error: String? = null
    )

    var request by mutableStateOf<Request?>(null)
        private set
    private var answer: CompletableDeferred<Set<String>?>? = null

    /** Null means canceled, an empty set means base only. Inspection errors never start a run. */
    suspend fun select(file: File): Set<String>? = withContext(Dispatchers.Main.immediate) {
        check(answer == null)
        val pending = CompletableDeferred<Set<String>?>()
        answer = pending
        try {
            request = try {
                val modules = withContext(Dispatchers.IO) {
                    SplitApkPreparer.selectableModules(file, deviceConfig())
                }
                Request(file.name, modules)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Request(file.name, emptyList(), e.message ?: e.javaClass.simpleName)
            }
            pending.await()
        } finally {
            request = null
            answer = null
            pending.cancel()
        }
    }

    fun confirm(names: Set<String>) {
        val current = request ?: return
        if (current.error != null) return
        val selectable = current.modules.filterNot { it.isBase }.mapTo(hashSetOf()) { it.name }
        if (selectable.containsAll(names)) answer?.complete(names.toSet())
    }

    fun cancel() {
        answer?.complete(null)
    }
}
