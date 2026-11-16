package com.polisheasy.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.polisheasy.app.R
import com.polisheasy.app.ui.components.PrimaryButton
import com.polisheasy.app.ui.components.ScreenFrame
import com.polisheasy.app.ui.components.SecondaryButton
import com.polisheasy.app.ui.theme.Space
import com.polisheasy.engine.Today
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun HomeScreen(
    today: Today,
    streak: Int,
    onStart: () -> Unit,
    onContinue: () -> Unit,
    onOpenPath: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    ScreenFrame(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Space.xs))
        Text(
            pluralStringResource(R.plurals.home_streak, streak, streak),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(Space.m))
        when (today) {
            Today.DailySetAvailable -> {
                Message(stringResource(R.string.home_ready))
                Spacer(Modifier.height(Space.xl))
                PrimaryButton(stringResource(R.string.home_start), onStart)
            }
            Today.ReviewOnlyAvailable -> {
                Message(stringResource(R.string.home_review_only_ready))
                Spacer(Modifier.height(Space.xl))
                PrimaryButton(stringResource(R.string.home_start), onStart)
            }
            is Today.InProgress -> {
                val message = if (today.reviewOnly) R.string.home_review_only_in_progress else R.string.home_in_progress
                Message(stringResource(message, today.progress.correct, today.progress.total))
                Spacer(Modifier.height(Space.xl))
                PrimaryButton(stringResource(R.string.home_continue), onContinue)
            }
            is Today.DoneForToday -> {
                val unlockTime = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(today.nextUnlock)
                val message = if (today.reviewOnlyNext) R.string.home_done_review_only_next else R.string.home_done
                Message(stringResource(message, unlockTime))
            }
            Today.AllCompleted -> Message(stringResource(R.string.home_all_completed))
        }
        Spacer(Modifier.height(Space.l))
        SecondaryButton(stringResource(R.string.home_path), onOpenPath)
        Spacer(Modifier.height(Space.s))
        SecondaryButton(stringResource(R.string.home_settings), onOpenSettings)
    }
}

@Composable
private fun Message(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
}
