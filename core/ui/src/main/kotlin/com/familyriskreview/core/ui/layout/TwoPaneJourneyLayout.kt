package com.familyriskreview.core.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.familyriskreview.core.designsystem.theme.FrrColors

/**
 * Adaptive two-pane shell for early journey screens.
 * Left: visual context. Right: question and interaction.
 *
 * Designed for 10–13" tablets in landscape first; collapses to a single column
 * when width is compact (portrait / split-screen).
 */
@Composable
fun TwoPaneJourneyLayout(
    visualContext: @Composable () -> Unit,
    interaction: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val useTwoPane = maxWidth >= 840.dp
        if (useTwoPane) {
            Row(Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxHeight()
                        .background(FrrColors.WarmMist)
                        .padding(32.dp),
                ) {
                    visualContext()
                }
                Box(
                    modifier = Modifier
                        .weight(0.58f)
                        .fillMaxHeight()
                        .background(FrrColors.CloudWhite)
                        .padding(horizontal = 40.dp, vertical = 32.dp),
                ) {
                    Column(Modifier.widthIn(max = 640.dp)) {
                        interaction()
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(FrrColors.CloudWhite)
                    .padding(24.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.35f, fill = false)
                        .background(FrrColors.WarmMist)
                        .padding(20.dp),
                ) {
                    visualContext()
                }
                Spacer(Modifier.height(16.dp))
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    interaction()
                }
            }
        }
    }
}

enum class WindowWidthClass {
    COMPACT,
    MEDIUM,
    EXPANDED,
}

fun classifyWidth(widthDp: androidx.compose.ui.unit.Dp): WindowWidthClass = when {
    widthDp < 600.dp -> WindowWidthClass.COMPACT
    widthDp < 840.dp -> WindowWidthClass.MEDIUM
    else -> WindowWidthClass.EXPANDED
}
