/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-manager
 */

package app.morphe.manager.patcher.runtime

import android.content.res.Resources
import android.os.Build
import app.morphe.manager.patcher.DeviceConfig

/** System settings, unaffected by the manager's in-app language or scaling. */
internal fun deviceConfig(): DeviceConfig {
    val system = Resources.getSystem()
    val locales = system.configuration.locales
    return DeviceConfig(
        supportedAbis = Build.SUPPORTED_ABIS.filter { it.isNotBlank() },
        localeTags = (0 until locales.size()).map { locales[it].toLanguageTag() },
        densityDpi = system.displayMetrics.densityDpi
    )
}
