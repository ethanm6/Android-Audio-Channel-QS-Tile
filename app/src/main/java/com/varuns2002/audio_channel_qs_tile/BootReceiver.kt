package com.varuns2002.audio_channel_qs_tile

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * A reboot clears any pending [AlarmManager] alarm, so if a tile session was active when the
 * device rebooted the timer would never fire. To avoid Mono getting stuck on, revert to Stereo
 * on boot. (Applies to infinite sessions too — a fresh boot starts at Stereo.)
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        if (MonoTimer.durationIndex(context) != -1) {
            MonoTimer.turnOff(context)
        }
    }
}
