package com.polisheasy.app.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.polisheasy.app.R

internal fun appVersion(context: Context): String =
    context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()

/**
 * Opens a draft in the Learner's email app, naming the Exercise when the report is about one.
 * The app itself sends nothing: it has no network permission.
 */
internal fun reportMistake(context: Context, exerciseId: String?) {
    val address = context.getString(R.string.report_email)
    val subject = if (exerciseId == null) {
        context.getString(R.string.report_subject_general)
    } else {
        context.getString(R.string.report_subject_exercise, exerciseId)
    }
    val body = listOfNotNull(
        exerciseId?.let { context.getString(R.string.report_body_exercise, it) },
        context.getString(R.string.report_body_version, appVersion(context)),
        "",
        context.getString(R.string.report_body_prompt),
        "",
    ).joinToString("\n")
    val draft = Uri.parse(
        "mailto:" + Uri.encode(address, "@") +
            "?subject=" + Uri.encode(subject) +
            "&body=" + Uri.encode(body.replace("\n", "\r\n")),
    )
    // Email apps differ in which of the two they read, so the subject and body go in both.
    val intent = Intent(Intent.ACTION_SENDTO, draft)
        .putExtra(Intent.EXTRA_SUBJECT, subject)
        .putExtra(Intent.EXTRA_TEXT, body)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.report_no_email_app, Toast.LENGTH_LONG).show()
    }
}
