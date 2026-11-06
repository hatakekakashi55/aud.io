package com.aud.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.aud.android.data.Store
import com.aud.android.player.PlaybackManager
import com.aud.android.ui.AudApp
import com.aud.android.ui.AudTheme

class MainActivity : ComponentActivity() {

    private lateinit var store: Store
    private lateinit var playback: PlaybackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = Store(applicationContext)
        playback = PlaybackManager(store)
        setContent {
            AudTheme {
                AudApp(player = playback, store = store)
            }
        }
    }

    override fun onDestroy() {
        playback.release()
        super.onDestroy()
    }
}
