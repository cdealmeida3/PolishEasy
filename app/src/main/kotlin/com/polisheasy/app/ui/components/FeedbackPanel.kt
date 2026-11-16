package com.polisheasy.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.polisheasy.app.R
import com.polisheasy.app.ui.PolishEasyTheme
import com.polisheasy.app.ui.theme.Corner
import com.polisheasy.app.ui.theme.Size
import com.polisheasy.app.ui.theme.Space

/** What the feedback is about. Green is right, amber is wrong; red is never used for an answer. */
enum class FeedbackTone {
    /** A right answer: green, with a tick. */
    Correct,

    /** Accepted, with something to notice, such as a missing accent: amber, with a tick. */
    Noted,

    /** A wrong answer: amber, with a cross. */
    Wrong,
}

/**
 * What the Learner is told about an answer, in the place of the main action, with the next main
 * action inside it. [heading] says how it went and [detail] what to take from it, such as the
 * correct answer. [speaker] is the speaker for the Polish, when there is something to play.
 */
@Composable
fun FeedbackPanel(
    tone: FeedbackTone,
    heading: String,
    continueLabel: String,
    onContinue: () -> Unit,
    reportLabel: String,
    onReport: () -> Unit,
    modifier: Modifier = Modifier,
    detail: String? = null,
    speaker: (@Composable () -> Unit)? = null,
) {
    val colors = PolishEasyTheme.colors
    val right = tone == FeedbackTone.Correct
    val container = if (right) colors.successContainer else colors.cautionContainer
    val onContainer = if (right) colors.onSuccessContainer else colors.onCautionContainer
    val accent = if (right) colors.success else colors.caution
    Surface(
        color = container,
        contentColor = onContainer,
        shape = RoundedCornerShape(topStart = Corner.extraLarge, topEnd = Corner.extraLarge),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                .padding(start = Space.screenSide, end = Space.xs, top = Space.m, bottom = Space.screenBottom),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.widthIn(max = Size.contentMax)) {
                Icon(
                    painterResource(if (tone == FeedbackTone.Wrong) R.drawable.ic_cancel else R.drawable.ic_check_circle),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier
                        .size(Size.feedbackIcon)
                        .appearing(),
                )
                Spacer(Modifier.width(Space.s))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = Space.xs)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                ) {
                    Text(heading, style = MaterialTheme.typography.titleLarge, color = accent)
                    if (detail != null) {
                        Text(detail, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                speaker?.invoke()
                IconButton(onClick = onReport, modifier = Modifier.size(Size.touch)) {
                    Icon(painterResource(R.drawable.ic_flag), contentDescription = reportLabel, tint = accent)
                }
            }
            Spacer(Modifier.height(Space.xs))
            PrimaryButton(
                continueLabel,
                onContinue,
                tone = if (right) ButtonTone.Success else ButtonTone.Caution,
                modifier = Modifier
                    .padding(end = Space.m)
                    .widthIn(max = Size.contentMax),
            )
        }
    }
}
