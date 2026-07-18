package com.varuns2002.audio_channel_qs_tile

import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.CountDownTimer
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class AudioChannelQSTile : TileService() {

    /** Ticks once a second while the panel is open, purely to update the visible countdown. */
    private var countdownTicker: CountDownTimer? = null

    override fun onStartListening() {
        super.onStartListening()
        renderTile()
        startTickerIfNeeded()
    }

    override fun onStopListening() {
        super.onStopListening()
        stopTicker()
    }

    override fun onClick() {
        super.onClick()
        if (!Settings.System.canWrite(this)) {
            val intent = Intent("android.settings.action.MANAGE_WRITE_SETTINGS")
            intent.data = Uri.parse("package:$packageName")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            return
        }
        MonoTimer.handleClick(this)
        renderTile()
        stopTicker()
        startTickerIfNeeded()
    }

    /** Updates the tile's state, icon, and text to reflect the current Mono/timer state. */
    private fun renderTile() {
        val tile = qsTile ?: return
        if (MonoTimer.isMonoOn(this)) {
            tile.state = Tile.STATE_ACTIVE
            tile.icon = Icon.createWithResource(this, R.drawable.ic_toggle_mono)
            // remainingSeconds() returns -1 for infinite sessions (and for Mono enabled
            // outside this tile), which formatRemaining renders as "∞".
            val timeText = MonoTimer.formatRemaining(MonoTimer.remainingSeconds(this))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.label = getString(R.string.label_and_subtitle_mono)
                tile.subtitle = timeText
            } else {
                // No subtitle field before Q: fold the countdown into the label.
                tile.label = "${getString(R.string.label_and_subtitle_mono)} $timeText"
            }
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.icon = Icon.createWithResource(this, R.drawable.ic_toggle_stereo)
            tile.label = getString(R.string.label_and_subtitle_stereo)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = ""
            }
        }
        tile.updateTile()
    }

    /** Starts a 1 s ticker to animate the countdown, only for an active finite timer. */
    private fun startTickerIfNeeded() {
        if (!MonoTimer.isMonoOn(this)) return
        val remaining = MonoTimer.remainingSeconds(this)
        if (remaining < 0) return // infinite: nothing to tick

        countdownTicker = object : CountDownTimer(remaining * 1000L + 500L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                renderTile()
            }

            override fun onFinish() {
                // The alarm reverts Mono independently; do it here too for an instant update
                // when the user is watching the panel.
                MonoTimer.turnOff(this@AudioChannelQSTile)
                renderTile()
            }
        }.start()
    }

    private fun stopTicker() {
        countdownTicker?.cancel()
        countdownTicker = null
    }
}
