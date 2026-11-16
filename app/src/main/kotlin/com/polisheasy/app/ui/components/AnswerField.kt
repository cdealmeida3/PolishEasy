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
) {
    val focusRequester = remember { FocusRequester() }
    // Opens the keyboard as soon as the Exercise appears.
    LaunchedEffect(Unit) { if (enabled) focusRequester.requestFocus() }

    OutlinedTextField(
        value = typed,
        onValueChange = onTypedChange,
        readOnly = !enabled,
        singleLine = true,
        textStyle = MaterialTheme.typography.titleMedium,
        shape = MaterialTheme.shapes.small,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Done,
            hintLocales = LocaleList("pl"),
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
    )
}

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
