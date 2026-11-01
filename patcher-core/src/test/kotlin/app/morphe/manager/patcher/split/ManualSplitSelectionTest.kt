/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-manager
 */

package app.morphe.manager.patcher.split

import app.morphe.manager.patcher.DeviceConfig
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ManualSplitSelectionTest {
    private val workspace = createTempDirectory("manual-splits").toFile()
    private val device = DeviceConfig(listOf("arm64-v8a", "armeabi-v7a"), listOf("en-US"), 420)
    private val base = "com.example.config.fr.apk"
    private val splits = listOf(
        "split_config.arm64_v8a.apk", "split_config.x86_64.apk",
        "split_config.en.apk", "split_config.fr.apk",
        "split_config.xhdpi.apk", "split_config.xxxhdpi.apk", "feature_camera.apk"
    )

    @AfterTest
    fun cleanup() {
        workspace.deleteRecursively()
    }

    private fun module(split: String?): ByteArray {
        val manifest = AndroidManifestBlock().apply {
            packageName = "com.example.app"
            if (split != null) setSplit(split, true)
            refresh()
        }.bytes
        return ByteArrayOutputStream().also { bytes ->
            ZipOutputStream(bytes).use { zip ->
                zip.putNextEntry(ZipEntry("AndroidManifest.xml"))
                zip.write(manifest)
                zip.closeEntry()
            }
        }.toByteArray()
    }

    private fun archive(bases: List<String> = listOf(base), names: List<String> = splits): File =
        File(workspace, "bundle.apks").also { file ->
            ZipOutputStream(file.outputStream()).use { zip ->
                (bases + names).forEach { name ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(module(if (name in bases) null else name.removeSuffix(".apk")))
                    zip.closeEntry()
                }
                // An APK embedded as an asset must never appear as a selectable module.
                zip.putNextEntry(ZipEntry("assets/embedded.apk"))
                zip.write(module(null))
                zip.closeEntry()
            }
        }

    private fun selected(file: File, names: Set<String>?, filter: DeviceConfig? = device): Set<String> =
        ZipFile(file).use { zip ->
            SplitApkPreparer.selectedModules(zip, SplitApkPreparer.splitModuleEntries(zip).toList(), names, filter)
                .mapTo(linkedSetOf()) { it.name }
        }

    @Test
    fun `manual choices preserve foreign ABI and language despite device filtering`() {
        val names = setOf("split_config.x86_64.apk", "split_config.fr.apk")
        assertEquals(names + base, selected(archive(), names))
    }

    @Test
    fun `select all preserves every module`() {
        assertEquals(splits.toSet() + base, selected(archive(), splits.toSet()))
    }

    @Test
    fun `deselect all retains the actual base even when its name looks like an unwanted language`() {
        assertEquals(setOf(base), selected(archive(), emptySet()))
    }

    @Test
    fun `recommendations exactly match automatic filtering including density fallback and features`() {
        val file = archive()
        val modules = SplitApkPreparer.selectableModules(file, device)
        assertEquals(base, modules.first().name)
        assertTrue(modules.first().isBase)
        assertEquals(splits.size + 1, modules.size)
        val recommended = modules.filter { it.matchesDevice }.mapTo(linkedSetOf()) { it.name }
        assertEquals(
            setOf(base, "split_config.arm64_v8a.apk", "split_config.en.apk", "split_config.xhdpi.apk", "feature_camera.apk"),
            recommended
        )
        assertEquals(selected(file, null), recommended)
        assertEquals(recommended, selected(file, recommended - base))
    }

    @Test
    fun `disabled filtering with no manual selection keeps the previous all-modules behavior`() {
        assertEquals(splits.toSet() + base, selected(archive(), null, filter = null))
    }

    @Test
    fun `an unknown selected name fails instead of silently changing the selection`() {
        assertFailsWith<IllegalArgumentException> { selected(archive(), setOf("missing.apk")) }
    }

    @Test
    fun `a missing or ambiguous base blocks manual selection`() {
        for (bases in listOf(emptyList(), listOf("base.apk", "another-base.apk"))) {
            val file = archive(bases = bases)
            assertFailsWith<IllegalArgumentException> { SplitApkPreparer.selectableModules(file, device) }
            assertFailsWith<IllegalArgumentException> { selected(file, emptySet()) }
        }
    }

    @Test
    fun `an archive containing only the base permits an empty selection`() {
        val file = archive(names = emptyList())
        assertEquals(setOf(base), selected(file, emptySet()))
        assertEquals(1, SplitApkPreparer.selectableModules(file, device).size)
    }
}
