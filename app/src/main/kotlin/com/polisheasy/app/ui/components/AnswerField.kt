package com.polisheasy.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.intl.LocaleList
import com.polisheasy.app.ui.theme.Size
import com.polisheasy.app.ui.theme.Space
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.dp
import com.polisheasy.app.ui.theme.inPolish
import com.polisheasy.app.ui.theme.Edge

/**
 * The field a Polish answer is typed into. The keyboard opens with it while [enabled], and its
 * "Done" key calls [onDone]. Once answered it shows what was typed and cannot be changed.
 */
@Composable
fun AnswerField(
    typed: TextFieldValue,
    onTypedChange: (TextFieldValue) -> Unit,
    label: String,
    enabled: Boolean,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    /** Whether the keyboard opens as soon as the field appears; otherwise it waits for a tap. */
    focus: Boolean = true,
) {
    val focusRequester = remember { FocusRequester() }
    // Opens the keyboard as soon as the Exercise appears.
    LaunchedEffect(Unit) { if (enabled && focus) focusRequester.requestFocus() }

    OutlinedTextField(
        value = typed,
        onValueChange = onTypedChange,
        readOnly = !enabled,
        singleLine = true,
        textStyle = MaterialTheme.typography.titleMedium,
        shape = MaterialTheme.shapes.small,
        label = { Text(label) },
        keyboardOptions = PolishKeyboard,
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
    )
}

/**
 * A Polish sentence with a gap the Learner types into: the words [before] and [after] the gap at
 * prompt size, and between them a slot that holds what is typed and grows with it. The whole
 * sentence is the text field, so the keyboard opens with it while [enabled], and its "Done" key
 * calls [onDone]. [label] is what a screen reader calls the slot.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SentenceField(
    before: String,
    after: String,
    typed: TextFieldValue,
    onTypedChange: (TextFieldValue) -> Unit,
    label: String,
    enabled: Boolean,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val focusRequester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { if (enabled) focusRequester.requestFocus() }
    val style = MaterialTheme.typography.headlineMedium.inPolish()

    BasicTextField(
        value = typed,
        onValueChange = onTypedChange,
        readOnly = !enabled,
        singleLine = true,
        textStyle = style.copy(color = scheme.onSecondaryContainer),
        cursorBrush = SolidColor(scheme.primary),
        keyboardOptions = PolishKeyboard,
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused }
            .semantics { contentDescription = label },
        decorationBox = { slot ->
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Space.xs),
                verticalArrangement = Arrangement.spacedBy(Space.xs),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                wordsOf(before).forEach { Text(it, style = style, color = scheme.onSurface) }
                Box(
                    modifier = Modifier
                        .width(IntrinsicSize.Min)
                        .widthIn(min = SlotLeast)
                        .background(scheme.secondaryContainer, MaterialTheme.shapes.small)
                        .border(Edge.border, if (focused && enabled) scheme.primary else scheme.outline, MaterialTheme.shapes.small)
                        .padding(horizontal = Space.s, vertical = Space.xxs),
                ) { slot() }
                wordsOf(after).forEach { Text(it, style = style, color = scheme.onSurface) }
            }
        },
    )
}

private fun wordsOf(text: String): List<String> = text.split(' ').filter { it.isNotBlank() }

private val SlotLeast = 96.dp

/** A keyboard for typing Polish: no capitals and no corrections forced on the Learner. */
private val PolishKeyboard = KeyboardOptions(
    capitalization = KeyboardCapitalization.None,
    autoCorrectEnabled = false,
    imeAction = ImeAction.Done,
    hintLocales = LocaleList("pl"),
)

private val polishLetters = listOf("ą", "ć", "ę", "ł", "ń", "ó", "ś", "ź", "ż")

/**
 * The Polish letters a standard keyboard hides behind a long press, one tap each. It sits directly
 * above the keyboard. [keyDescription] gives what a screen reader says for a letter.
 */
@Composable
fun PolishKeyRow(onKey: (String) -> Unit, keyDescription: @Composable (String) -> String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(Space.xxs),
        horizontalArrangement = Arrangement.spacedBy(Space.xxs),
    ) {
        polishLetters.forEach { letter ->
            val description = keyDescription(letter)
            Surface(
                onClick = { onKey(letter) },
                shape = MaterialTheme.shapes.extraSmall,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = Size.tile)
                    // A key must not take focus from the text field, or the keyboard would close.
                    .focusProperties { canFocus = false }
                    .semantics { contentDescription = description },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(letter, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

/** This text with [letter] in place of the selection, or at the cursor, and the cursor after it. */
fun TextFieldValue.inserting(letter: String): TextFieldValue = copy(
    text = text.replaceRange(selection.min, selection.max, letter),
    selection = TextRange(selection.min + letter.length),
)
