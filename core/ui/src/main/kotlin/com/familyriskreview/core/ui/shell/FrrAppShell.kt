package com.familyriskreview.core.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.familyriskreview.core.designsystem.theme.FrrSpacing
import com.familyriskreview.core.designsystem.theme.frrColors
import com.familyriskreview.core.ui.layout.WindowWidthClass
import com.familyriskreview.core.ui.layout.classifyWidth

enum class FrrShellDestination {
    Dashboard,
    Settings,
}

/**
 * Adaptive app shell: centres content to [FrrSpacing.contentMaxWidth] and shows a
 * navigation rail on expanded widths for Dashboard / Settings.
 */
@Composable
fun FrrAppShell(
    selectedDestination: FrrShellDestination,
    onDestinationSelected: (FrrShellDestination) -> Unit,
    modifier: Modifier = Modifier,
    dashboardLabel: String,
    settingsLabel: String,
    content: @Composable () -> Unit,
) {
    val colors = frrColors()
    BoxWithConstraints(
        modifier =
        modifier
            .fillMaxSize()
            .background(colors.surface),
    ) {
        val widthClass = classifyWidth(maxWidth)
        val showRail = widthClass == WindowWidthClass.EXPANDED

        if (showRail) {
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxHeight()) {
                    NavigationRail(
                        containerColor = colors.elevatedSurface,
                        contentColor = colors.onSurface,
                    ) {
                        ShellRailItem(
                            selected = selectedDestination == FrrShellDestination.Dashboard,
                            onClick = { onDestinationSelected(FrrShellDestination.Dashboard) },
                            icon = Icons.Outlined.SpaceDashboard,
                            label = dashboardLabel,
                        )
                        ShellRailItem(
                            selected = selectedDestination == FrrShellDestination.Settings,
                            onClick = { onDestinationSelected(FrrShellDestination.Settings) },
                            icon = Icons.Outlined.Settings,
                            label = settingsLabel,
                        )
                    }
                }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(FrrSpacing.lg),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    FrrContentWidth { content() }
                }
            }
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = FrrSpacing.md, vertical = FrrSpacing.lg),
                contentAlignment = Alignment.TopCenter,
            ) {
                FrrContentWidth { content() }
            }
        }
    }
}

@Composable
fun FrrContentWidth(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
        modifier
            .fillMaxWidth()
            .widthIn(max = FrrSpacing.contentMaxWidth),
    ) {
        content()
    }
}

@Composable
private fun ShellRailItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
) {
    val colors = frrColors()
    NavigationRailItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(imageVector = icon, contentDescription = label) },
        label = { Text(label) },
        colors =
        NavigationRailItemDefaults.colors(
            selectedIconColor = colors.primaryAction,
            selectedTextColor = colors.primaryAction,
            indicatorColor = colors.primaryAction.copy(alpha = 0.14f),
            unselectedIconColor = colors.mutedText,
            unselectedTextColor = colors.mutedText,
        ),
        modifier = Modifier.padding(vertical = 4.dp),
    )
}
