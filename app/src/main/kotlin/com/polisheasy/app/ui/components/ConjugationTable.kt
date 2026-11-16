package com.polisheasy.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.polisheasy.app.ui.theme.Edge
import com.polisheasy.app.ui.theme.Space
import androidx.compose.ui.Alignment
import com.polisheasy.app.ui.theme.inPolish

/** One row of a [ConjugationTable]: the Polish [pronouns], the verb's [form], and the [speaker] that says the form, if it can be heard. */
class ConjugationRowView(val pronouns: String, val form: String, val speaker: (@Composable () -> Unit)? = null)

/** A verb's forms, one row per person: the Polish pronouns, the form, and its speaker at the end when it has one. */
@Composable
fun ConjugationTable(title: String, rows: List<ConjugationRowView>, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(Edge.hairline, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(Space.m), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            rows.forEach { row ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        row.pronouns,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(row.form, style = MaterialTheme.typography.titleMedium.inPolish(), modifier = Modifier.weight(1f))
                    row.speaker?.invoke()
                }
            }
        }
    }
}
