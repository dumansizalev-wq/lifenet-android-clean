package net.lifenet.core.lifecycle

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import net.lifenet.core.mesh.GhostRadioService

/**
 * SentinelBootReceiver: Restores mesh continuity after device reboot.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            
            Log.i("LIFENET", "System reboot detected. Respawning node...")
            val serviceIntent = Intent(context, GhostRadioService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}
