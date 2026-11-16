package com.polisheasy.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.Point
import com.polisheasy.app.ui.components.PrimaryButton
import com.polisheasy.app.ui.components.ScreenFrame
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.app.ui.theme.inPolish

/**
 * The welcome a new Learner sees once: a first word of Polish, the three things that make the app
 * what it is, and one button that goes straight into the first Daily Set.
 */
@Composable
internal fun WelcomeScreen(onStart: () -> Unit) {
    ScreenFrame {
        // Scrolls at a large font size; the button stays on screen.
        Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(Space.xxl))
            Text(
                stringResource(R.string.welcome_greeting),
                style = MaterialTheme.typography.displayLarge.inPolish(),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                stringResource(R.string.welcome_greeting_meaning),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Space.l))
            Text(
                stringResource(R.string.welcome_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(Space.xl))
            Point(R.drawable.ic_check_circle, stringResource(R.string.welcome_point_daily), stringResource(R.string.welcome_point_daily_note))
            Spacer(Modifier.height(Space.l))
            Point(R.drawable.ic_schedule, stringResource(R.string.welcome_point_minutes), stringResource(R.string.welcome_point_minutes_note))
            Spacer(Modifier.height(Space.l))
            Point(R.drawable.ic_flame, stringResource(R.string.welcome_point_tomorrow), stringResource(R.string.welcome_point_tomorrow_note))
            Spacer(Modifier.height(Space.m))
        }
        Spacer(Modifier.height(Space.m))
        PrimaryButton(stringResource(R.string.welcome_start), onStart)
    }
}
