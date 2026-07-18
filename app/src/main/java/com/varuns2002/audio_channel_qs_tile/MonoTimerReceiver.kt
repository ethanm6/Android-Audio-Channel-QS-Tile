package com.varuns2002.audio_channel_qs_tile

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Fired by the [MonoTimer] expiry alarm. Reverts Mono to Stereo and refreshes the tile if it
 * happens to be visible. This is the reliable expiry mechanism, independent of whether the
 * Quick Settings panel (and hence the TileService) is currently alive.
 */
class MonoTimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        MonoTimer.turnOff(context)
        MonoTimer.refreshTile(context)
    }
}
