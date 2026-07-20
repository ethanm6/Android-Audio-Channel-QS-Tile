package com.varuns2002.audio_channel_qs_tile

import android.graphics.drawable.Icon
import android.os.Build
import android.os.CountDownTimer
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
        if (!AudioSwitch.isReady(this)) {
            AudioSwitch.requestAccess(this)
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
        // Render from the session bookkeeping alone: a session is active iff a duration is
        // set. The live "master_mono" setting is not consulted — in the privileged flavor it
        // is written asynchronously and lags a tap, which previously left the tile stuck on a
        // stale "Mono" render after a session ended (Stereo was never shown).
        if (MonoTimer.durationIndex(this) != -1) {
            tile.state = Tile.STATE_ACTIVE
            tile.icon = Icon.createWithResource(this, R.drawable.ic_toggle_mono)
            // remainingSeconds() returns -1 for infinite sessions, which formatRemaining
            // renders as "∞".
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
        // remainingSeconds is -1 when no session is active or the session is infinite.
        val remaining = MonoTimer.remainingSeconds(this)
        if (remaining < 0) return

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
