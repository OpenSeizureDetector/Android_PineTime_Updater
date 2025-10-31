package uk.org.openseizuredetector.pinetime.model

import kotlinx.serialization.Serializable

@Serializable
data class FirmwareRelease(
    val version: String,
    val description: String,
    val fname: String
)

@Serializable
data class FirmwareIndex(
    val releases: List<FirmwareRelease>,
    val recommendedFw: Int // The index of the recommended firmware
)
