package com.varuns2002.audio_channel_qs_tile

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.SystemClock
import android.provider.Settings
import android.service.quicksettings.TileService

/**
 * Shared logic for the Mono timer, used by [AudioChannelQSTile], [MonoTimerReceiver] and
 * [BootReceiver]. Behaves like LineageOS's Caffeine tile: tapping cycles through a set of
 * durations (within a 5 second window) and Mono automatically reverts to Stereo when the
 * timer expires.
 *
 * The actual audio state lives in the system "master_mono" setting; the timer bookkeeping
 * (which duration, when it expires, when it was last clicked) lives in [SharedPreferences].
 * Expiry is enforced by an [AlarmManager] exact alarm rather than an in-process timer, since
 * a TileService's process is killed shortly after the Quick Settings panel closes.
 */
object MonoTimer {

    /** Durations in seconds; -1 means infinite. Cycle: 1 min, 5 min, 10 min, ∞. */
    val DURATIONS = intArrayOf(1 * 60, 5 * 60, 10 * 60, -1)

    private const val PREFS = "mono_timer"
    private const val KEY_DURATION_INDEX = "duration_index"
    private const val KEY_EXPIRY_ELAPSED = "expiry_elapsed"
    private const val KEY_LAST_CLICK = "last_click_time"

    private const val ALARM_REQUEST_CODE = 1001

    /** Window (ms) within which a tap cycles duration instead of toggling off. */
    private const val CYCLE_WINDOW_MS = 5000L

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // region audio state

    fun isMonoOn(context: Context): Boolean = try {
        Settings.System.getInt(context.contentResolver, "master_mono") == 1
    } catch (exception: Settings.SettingNotFoundException) {
        exception.printStackTrace()
        false
    }

    // endregion

    // region timer state

    fun durationIndex(context: Context): Int = prefs(context).getInt(KEY_DURATION_INDEX, -1)

    /** Seconds left, or -1 if the current session is infinite / has no finite expiry. */
    fun remainingSeconds(context: Context): Int {
        val index = durationIndex(context)
        if (index < 0 || DURATIONS[index] == -1) return -1
        val expiry = prefs(context).getLong(KEY_EXPIRY_ELAPSED, 0L)
        val remainingMs = expiry - SystemClock.elapsedRealtime()
        return if (remainingMs <= 0L) 0 else (remainingMs / 1000L).toInt()
    }

    /** Formats seconds as "MM:SS", or "∞" when infinite (-1). */
    fun formatRemaining(seconds: Int): String {
        if (seconds < 0) return "∞"
        return String.format("%02d:%02d", seconds / 60 % 60, seconds % 60)
    }

    // endregion

    /**
     * The tap state machine, mirroring Caffeine's handleClick. A tap within [CYCLE_WINDOW_MS]
     * of the previous tap cycles to the next duration (turning off after the last one);
     * otherwise it simply toggles Mono on (starting at the first duration) or off.
     */
    fun handleClick(context: Context) {
        val now = SystemClock.elapsedRealtime()
        val lastClick = prefs(context).getLong(KEY_LAST_CLICK, -1L)

        if (isMonoOn(context) && lastClick != -1L && now - lastClick < CYCLE_WINDOW_MS) {
            // cycle to the next duration
            val nextIndex = durationIndex(context) + 1
            if (nextIndex >= DURATIONS.size) {
                turnOff(context)
            } else {
                startDuration(context, nextIndex)
            }
        } else {
            // toggle
            if (isMonoOn(context)) {
                turnOff(context)
            } else {
                startDuration(context, 0)
            }
        }

        prefs(context).edit().putLong(KEY_LAST_CLICK, now).apply()
    }

    /** Enables Mono for DURATIONS[index] and (re)schedules the expiry alarm. */
    fun startDuration(context: Context, index: Int) {
        AudioSwitch.setMono(context, true)
        val seconds = DURATIONS[index]
        val editor = prefs(context).edit().putInt(KEY_DURATION_INDEX, index)
        if (seconds == -1) {
            // infinite: no expiry, no alarm
            editor.remove(KEY_EXPIRY_ELAPSED).apply()
            cancelAlarm(context)
        } else {
            val expiry = SystemClock.elapsedRealtime() + seconds * 1000L
            editor.putLong(KEY_EXPIRY_ELAPSED, expiry).apply()
            scheduleAlarm(context, expiry)
        }
    }

    /** Reverts to Stereo and clears all timer state. */
    fun turnOff(context: Context) {
        AudioSwitch.setMono(context, false)
        cancelAlarm(context)
        prefs(context).edit()
            .putInt(KEY_DURATION_INDEX, -1)
            .remove(KEY_EXPIRY_ELAPSED)
            .apply()
    }

    // region alarm

    private fun alarmPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MonoTimerReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun scheduleAlarm(context: Context, expiryElapsed: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // Exact-alarm restrictions don't apply: the standard flavor's targetSdk 22 predates
        // them, and the privileged flavor declares USE_EXACT_ALARM.
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            expiryElapsed,
            alarmPendingIntent(context)
        )
    }

    private fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(alarmPendingIntent(context))
    }

    // endregion

    /** Asks the system to bind the tile briefly so it re-renders after a background change. */
    fun refreshTile(context: Context) {
        TileService.requestListeningState(
            context,
            ComponentName(context, AudioChannelQSTile::class.java)
        )
    }
}
