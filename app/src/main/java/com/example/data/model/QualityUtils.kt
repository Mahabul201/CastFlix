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
  val wanted = preferredLabel?.takeIf { it.isNotBlank() } ?: DEFAULT_QUALITY_LABEL
  options.firstOrNull { it.label == wanted }?.let { return it }
  val h = heightOf(wanted)
  return options.lastOrNull { heightOf(it.label) <= h } ?: options.first()
}
