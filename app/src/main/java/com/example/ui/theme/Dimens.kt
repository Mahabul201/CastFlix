package com.example.ui.theme

import androidx.compose.ui.unit.dp

/** One shared set of spacing values so every page lines up. */
object Dimens {
  /** Left/right margin of every page. */
  val ScreenHorizontal = 16.dp

  /** Space between the status bar (or the page title area) and the first element. */
  val ScreenTop = 16.dp

  /** Extra space at the end of a scrolling list (the bottom bar is already inset by Scaffold). */
  val ScreenBottom = 24.dp

  /** Gap between cards in a row / column / grid. */
  val GridGap = 12.dp
  val ListGap = 12.dp

  /** Space between sections on a page. */
  val Section = 16.dp
}
