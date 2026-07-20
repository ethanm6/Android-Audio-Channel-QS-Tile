package com.varuns2002.audio_channel_qs_tile

import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.topjohnwu.superuser.Shell
import java.util.concurrent.Executors
import rikka.shizuku.Shizuku

/**
 * Privileged flavor: writes "master_mono" by running `settings put` as the shell user
 * (Shizuku) or as root (libsu). Both are exempt from the targetSdk restriction that forces
 * the standard flavor to target SDK 22, so this flavor targets the current SDK and installs
 * without workarounds. Shizuku is preferred when it is running; root is the fallback.
 */
object AudioSwitch {

    /**
     * Writes run off the main thread: the first `su` call blocks on the Magisk grant dialog,
     * and Shizuku's remote process wait is also blocking.
     */
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    private fun shizukuRunning(): Boolean = try {
        Shizuku.pingBinder()
    } catch (exception: Throwable) {
        false
    }

    private fun shizukuGranted(): Boolean = try {
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (exception: Throwable) {
        false
    }

    /**
     * Only reports "not ready" when Shizuku is running but not yet authorized — that is the
     * one case with a meaningful grant dialog to show. Root availability can't be checked
     * cheaply on the main thread (the check itself may pop the Magisk dialog), so without
     * Shizuku this returns true and the async write path surfaces failures via toast.
     */
    fun isReady(context: Context): Boolean = !shizukuRunning() || shizukuGranted()

    fun requestAccess(context: Context) {
        Toast.makeText(context, R.string.toast_grant_shizuku, Toast.LENGTH_LONG).show()
        try {
            Shizuku.requestPermission(0)
        } catch (exception: Throwable) {
            exception.printStackTrace()
        }
    }

    fun setMono(context: Context, on: Boolean) {
        val appContext = context.applicationContext
        executor.execute {
            val value = if (on) 1 else 0
            val ok = writeViaShizuku(value) || writeViaRoot(value)
            if (!ok && on) {
                mainHandler.post {
                    Toast.makeText(
                        appContext, R.string.toast_need_shizuku_or_root, Toast.LENGTH_LONG
                    ).show()
                }
                // Roll the timer bookkeeping back; its own setMono(false) will fail silently.
                MonoTimer.turnOff(appContext)
            }
            // Re-render once the write has actually landed.
            MonoTimer.refreshTile(appContext)
        }
    }

    private fun writeViaShizuku(value: Int): Boolean {
        if (!shizukuRunning() || !shizukuGranted()) return false
        return try {
            // Shizuku.newProcess is hidden but stable in practice; kept via proguard-rules.pro.
            val newProcess = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java, Array<String>::class.java, String::class.java
            )
            newProcess.isAccessible = true
            val process = newProcess.invoke(
                null, arrayOf("settings", "put", "system", "master_mono", value.toString()),
                null, null
            ) as Process
            process.waitFor() == 0
        } catch (exception: Throwable) {
            exception.printStackTrace()
            false
        }
    }

    private fun writeViaRoot(value: Int): Boolean = try {
        Shell.getShell().isRoot &&
            Shell.cmd("settings put system master_mono $value").exec().isSuccess
    } catch (exception: Throwable) {
        exception.printStackTrace()
        false
    }
}
