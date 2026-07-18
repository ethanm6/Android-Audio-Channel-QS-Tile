package com.varuns2002.audio_channel_qs_tile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings

/**
 * Invisible activity launched by long-pressing the tile (via the QS_TILE_PREFERENCES intent
 * filter). Mirrors Caffeine's handleLongClick: jumps straight to the infinite duration, or
 * does nothing if Mono is already infinite. Finishes immediately without showing any UI.
 */
class InfiniteMonoActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Settings.System.canWrite(this)) {
            val intent = Intent("android.settings.action.MANAGE_WRITE_SETTINGS")
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
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
