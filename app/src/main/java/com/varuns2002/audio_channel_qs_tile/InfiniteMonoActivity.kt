package com.varuns2002.audio_channel_qs_tile

import android.app.Activity
import android.os.Bundle

/**
 * Invisible activity launched by long-pressing the tile (via the QS_TILE_PREFERENCES intent
 * filter). Mirrors Caffeine's handleLongClick: jumps straight to the infinite duration, or
 * does nothing if Mono is already infinite. Finishes immediately without showing any UI.
 */
class InfiniteMonoActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!AudioSwitch.isReady(this)) {
            AudioSwitch.requestAccess(this)
        } else {
            val infiniteIndex = MonoTimer.DURATIONS.size - 1
            if (!(MonoTimer.isMonoOn(this) && MonoTimer.durationIndex(this) == infiniteIndex)) {
                MonoTimer.startDuration(this, infiniteIndex)
            }
            MonoTimer.refreshTile(this)
        }
        finish()
    }
}
