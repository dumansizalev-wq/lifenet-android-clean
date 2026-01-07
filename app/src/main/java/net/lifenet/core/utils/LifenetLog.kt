package net.lifenet.core.utils

import android.util.Log

/**
 * LifenetLog: Centralized logging wrapper to handle production sanitization.
 */
object LifenetLog {
    private const val DEFAULT_TAG = "LIFENET"
    
    // In a real app, BuildConfig.DEBUG would be used.
    // We'll use a hardcoded toggle for this demo.
    private var isDebug = true 

    fun d(tag: String = DEFAULT_TAG, message: String) {
        if (isDebug) Log.d(tag, message)
    }

    fun i(tag: String = DEFAULT_TAG, message: String) {
        Log.i(tag, message)
    }

    fun w(tag: String = DEFAULT_TAG, message: String) {
        Log.w(tag, message)
    }

    fun e(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
    }
    
    fun setDebugMode(enabled: Boolean) {
        isDebug = enabled
    }
}
