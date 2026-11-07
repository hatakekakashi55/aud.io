package com.aud.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.aud.android.data.Appearance
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
            // Look & feel lives here so a change repaints the whole app at once.
            var appearance by remember { mutableStateOf(Appearance.load(this)) }

            AudTheme(appearance = appearance) {
                AudApp(
                    player = playback,
                    store = store,
                    appearance = appearance,
                    onAppearanceChange = { next ->
                        appearance = next
                        Appearance.save(this, next)
                    },
                    onFeedReset = {
                        // Wipes learned taste only — likes and recents are kept on purpose.
                        store.resetFeed()
                    },
                )
            }
        }
    }

    override fun onDestroy() {
        playback.release()
        super.onDestroy()
    }
}
