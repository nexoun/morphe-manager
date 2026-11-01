/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-manager
 */

package app.morphe.manager.patcher

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The ART/JVM request must distinguish automatic filtering from a base-only selection. */
class PatchRequestSplitSelectionTest {
    private val request = PatchRequest(
        packageName = "com.example.app",
        inputFile = "input.apks",
        outputFile = "output.apk",
        mergedApkFile = "merged.apk",
        temporaryDir = "temp",
        workspaceDir = "workspace",
        bundles = emptyList(),
        stripUnusedNativeLibs = true,
        device = DeviceConfig(listOf("arm64-v8a"), listOf("en-US"), 420)
    )

    @Test
    fun `requests without a selection retain the automatic default`() {
        val encoded = Json.encodeToString(PatchRequest.serializer(), request)
        assertNull(Json.decodeFromString(PatchRequest.serializer(), encoded).selectedSplitNames)
    }

    @Test
    fun `empty and explicit manual selections survive the runtime boundary`() {
        for (names in listOf(emptySet(), setOf("split_config.fr.apk", "split_config.x86_64.apk"))) {
            val original = request.copy(selectedSplitNames = names)
            val encoded = Json.encodeToString(PatchRequest.serializer(), original)
            assertEquals(original, Json.decodeFromString(PatchRequest.serializer(), encoded))
        }
    }
}
