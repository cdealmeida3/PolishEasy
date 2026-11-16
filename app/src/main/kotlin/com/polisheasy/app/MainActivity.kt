package com.polisheasy.app

import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.polisheasy.app.ui.PolishEasyApp
import com.polisheasy.app.ui.PolishEasyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // The volume keys change the media volume even while no Clip is playing.
        volumeControlStream = AudioManager.STREAM_MUSIC
        setContent {
            PolishEasyTheme {
                PolishEasyApp()
            }
        }
    }
}
