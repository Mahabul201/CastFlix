package com.example.data.model

/**
 * Helpers for the 480p / 720p / 1080p variants an admin can attach to a movie or episode.
 * When no variants exist the plain streamUrl is the only option.
 */
const val DEFAULT_QUALITY_LABEL = "720p"

private fun heightOf(label: String): Int =
  label.filter { it.isDigit() }.toIntOrNull() ?: Int.MAX_VALUE

/** Options sorted low -> high. Falls back to a single "Auto" entry built from [streamUrl]. */
fun buildQualityOptions(qualities: List<VideoQuality>, streamUrl: String): List<VideoQuality> {
  val valid = qualities
    .filter { it.label.isNotBlank() && it.url.isNotBlank() }
    .distinctBy { it.label }
    .sortedBy { heightOf(it.label) }
  if (valid.isNotEmpty()) return valid
  return if (streamUrl.isNotBlank()) listOf(VideoQuality("Auto", streamUrl)) else emptyList()
}

/** True when the user actually has a choice to make. */
fun hasQualityChoice(options: List<VideoQuality>): Boolean = options.size > 1

/**
 * Picks the option to use: the preferred label if present, otherwise the closest lower
 * quality, otherwise the lowest higher one. With no preference it uses 720p when available.
 */
fun pickQuality(options: List<VideoQuality>, preferredLabel: String?): VideoQuality? {
  if (options.isEmpty()) return null
  val wanted = preferredLabel?.takeIf { it.isNotBlank() && !it.equals(AUTO_QUALITY, ignoreCase = true) }
    ?: DEFAULT_QUALITY_LABEL
  options.firstOrNull { it.label == wanted }?.let { return it }
  val h = heightOf(wanted)
  return options.lastOrNull { heightOf(it.label) <= h } ?: options.first()
}

// ---------------------------------------------------------------------------------------------
// Auto quality: follows the network speed
// ---------------------------------------------------------------------------------------------
const val AUTO_QUALITY = "Auto"

/** No saved choice (or "Auto") means the app picks the quality from the network speed. */
fun isAutoQuality(preferred: String?): Boolean =
  preferred.isNullOrBlank() || preferred.equals(AUTO_QUALITY, ignoreCase = true)

/** Rough bitrate (bits/s) a stream of this quality needs to play smoothly. */
private fun neededBps(label: String): Long = when (heightOf(label)) {
  in 0..360 -> 700_000L
  in 361..480 -> 1_200_000L
  in 481..720 -> 2_800_000L
  in 721..1080 -> 5_500_000L
  else -> 12_000_000L
}

/**
 * Best quality the current connection can carry. Goes down at once when the network gets slow,
 * but only goes up when there is clear headroom, so the quality does not flip back and forth.
 */
fun pickAutoQuality(options: List<VideoQuality>, bandwidthBps: Long, currentLabel: String? = null): VideoQuality? {
  if (options.isEmpty()) return null
  if (options.size == 1) return options.first()
  if (bandwidthBps <= 0L) return pickQuality(options, DEFAULT_QUALITY_LABEL)
  val usable = (bandwidthBps * 0.75).toLong()
  val best = options.lastOrNull { neededBps(it.label) <= usable } ?: options.first()
  val current = options.firstOrNull { it.label == currentLabel } ?: return best
  return if (heightOf(best.label) > heightOf(current.label) && usable < neededBps(best.label) * 13 / 10) current else best
}
