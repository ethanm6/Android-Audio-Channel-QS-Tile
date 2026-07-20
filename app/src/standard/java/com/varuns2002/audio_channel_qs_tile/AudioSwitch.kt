package com.varuns2002.audio_channel_qs_tile

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast

/**
 * Standard flavor: writes "master_mono" directly through the public Settings API, gated on
 * the "Modify system settings" permission. This only works because the app targets SDK 22
 * (see build.gradle) — from SDK 23 on, SettingsProvider rejects writes to non-whitelisted
 * Settings.System keys regardless of granted permissions.
 */
object AudioSwitch {

    fun isReady(context: Context): Boolean = Settings.System.canWrite(context)

    /** Explains the redirect with a toast, then opens the "Modify system settings" screen. */
    fun requestAccess(context: Context) {
        Toast.makeText(context, R.string.toast_grant_write_settings, Toast.LENGTH_LONG).show()
        val intent = Intent("android.settings.action.MANAGE_WRITE_SETTINGS")
        intent.data = Uri.parse("package:${context.packageName}")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun setMono(context: Context, on: Boolean) {
        Settings.System.putInt(context.contentResolver, "master_mono", if (on) 1 else 0)
    }
}
