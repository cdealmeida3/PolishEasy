package com.polisheasy.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.polisheasy.app.ui.theme.Scaled
import androidx.compose.foundation.layout.size

/** One destination of the navigation bar: its [label] and its [icon]. */
class NavigationTab(val label: String, @DrawableRes val icon: Int)

/**
 * A screen outside a Daily Set with the navigation bar under it. The tab at [selected] is marked
 * by its pill and by its colour, and is announced as selected. The bar keeps clear of the system's
 * own navigation, so the [content] above it does not have to.
 */
@Composable
fun NavigationFrame(
    tabs: List<NavigationTab>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .consumeWindowInsets(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
        ) {
            content()
        }
        NavigationBar(containerColor = scheme.surfaceContainer, contentColor = scheme.onSurfaceVariant, tonalElevation = 0.dp) {
            tabs.forEachIndexed { index, tab ->
                NavigationBarItem(
                    selected = index == selected,
                    onClick = { onSelect(index) },
                    icon = { Icon(painterResource(tab.icon), contentDescription = null, modifier = Modifier.size(Scaled.icon())) },
                    label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = scheme.primary,
                        selectedTextColor = scheme.onSurface,
                        indicatorColor = scheme.primaryContainer,
                        unselectedIconColor = scheme.onSurfaceVariant,
                        unselectedTextColor = scheme.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}
