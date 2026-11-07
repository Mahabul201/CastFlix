package com.example.util

/**
 * Shared flags for automatic picture-in-picture.
 * The players set [videoActive] while a video is playing; MainActivity reads it when the user
 * presses Home / switches apps and moves the video into a small floating window.
 */
object PipState {
  @Volatile var videoActive: Boolean = false
  @Volatile var pipEnabled: Boolean = true
}
