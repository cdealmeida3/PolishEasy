package com.polisheasy.app

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
        setContent {
            PolishEasyTheme {
                PolishEasyApp()
            }
        }
    }
}
