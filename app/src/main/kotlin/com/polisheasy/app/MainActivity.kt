package com.polisheasy.app

import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.polisheasy.app.ui.PolishEasyApp
import com.polisheasy.app.ui.PolishEasyTheme

class MainActivity : ComponentActivity() {
    private val viewModel: CourseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // The volume keys change the media volume even while no Clip is playing.
        volumeControlStream = AudioManager.STREAM_MUSIC
        // Covers anything that dropped the alarm while the app was away: a restored install, a force-stop.
        Reminder.arm(this)
        setContent {
            PolishEasyTheme {
                PolishEasyApp(viewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        visible = true
    }

    override fun onResume() {
        super.onResume()
        // The Learner is here, so the Reminder has done its job however the app was opened.
        Reminder.dismiss(this)
    }

    override fun onStop() {
        visible = false
        super.onStop()
    }

    /**
     * The Reminder notification tapped while the app was already running: back to Home. Nothing
     * is lost, since a Daily Set in progress is saved after every answer. A cold start is on Home
     * already.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Reminder.ACTION_OPEN_HOME) viewModel.backToHome()
    }

    companion object {
        /** Whether the app is on screen, where a Reminder would only be in the way. */
        @Volatile
        var visible = false
            private set
    }
}
