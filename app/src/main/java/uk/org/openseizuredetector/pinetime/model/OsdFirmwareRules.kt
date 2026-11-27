package uk.org.openseizuredetector.pinetime.model

import android.content.Intent
import android.util.Log

/** Data sent by the OpenSeizureDetector app when it starts this updater from its "Update Firmware?" prompt. */
data class OsdLaunchRequest(
    val macAddress: String,        // watch to update; may be blank
    val firmwareVersion: String,   // version OSD saw as "Recommended" (for logging only)
    val installedVersion: String   // version currently on the watch, e.g. "230.0.5"
) {
    companion object {
        const val EXTRA_AUTO_SELECT = "auto_select"
        const val EXTRA_MAC_ADDRESS = "mac_address"
        const val EXTRA_FIRMWARE_VERSION = "firmware_version"
        const val EXTRA_INSTALLED_VERSION = "installed_version"

        /** Null when the updater was started normally (manual button / launcher): behaviour unchanged. */
        fun from(intent: Intent?): OsdLaunchRequest? {
            if (intent == null || !intent.getBooleanExtra(EXTRA_AUTO_SELECT, false)) return null
            return OsdLaunchRequest(
                macAddress = intent.getStringExtra(EXTRA_MAC_ADDRESS).orEmpty(),
                firmwareVersion = intent.getStringExtra(EXTRA_FIRMWARE_VERSION).orEmpty(),
                installedVersion = intent.getStringExtra(EXTRA_INSTALLED_VERSION).orEmpty()
            )
        }
    }
}

/**
 * Rules for the OSD-initiated update. A release qualifies only if BOTH hold:
 *  1. it is the one marked recommended (releases[recommendedFw]), and
 *  2. its version is higher than the version installed on the watch.
 * Versions are compared number by number ("0.10" > "0.9", "1.0" > "0.10").
 * The watch reports "230.0.7" while index.json says "0.7"; the fixed "230." prefix is dropped.
 */
object OsdFirmwareRules {
    private const val TAG = "OsdFirmwareRules"
    private val NUMERIC = Regex("""\d+(\.\d+)*""")

    fun normalise(v: String): String = v.trim().removePrefix("230.")

    private fun parts(v: String): List<Int> =
        normalise(v).filter { it.isDigit() || it == '.' }.split('.').map { it.toIntOrNull() ?: 0 }

    fun compare(a: String, b: String): Int {
        val x = parts(a)
        val y = parts(b)
        for (i in 0 until maxOf(x.size, y.size)) {
            val xi = x.getOrElse(i) { 0 }
            val yi = y.getOrElse(i) { 0 }
            if (xi != yi) return xi.compareTo(yi)
        }
        return 0
    }

    fun qualifyingRelease(index: FirmwareIndex, installedVersion: String): FirmwareRelease? {
        if (installedVersion.isBlank()) return null
        val recommended = index.releases.getOrNull(index.recommendedFw) ?: return null
        // never offer "InfiniTime", "0.8_test" etc.
        if (!NUMERIC.matches(normalise(recommended.version))) return null
        val newer = compare(installedVersion, recommended.version) < 0
        Log.d(TAG, "installed=$installedVersion recommended=${recommended.version} newer=$newer")
        return if (newer) recommended else null
    }
}
