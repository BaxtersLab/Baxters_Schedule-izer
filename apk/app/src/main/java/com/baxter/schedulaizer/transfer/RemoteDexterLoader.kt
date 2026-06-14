package com.baxter.schedulaizer.transfer

import android.util.Log

object RemoteDexterLoader {
    private const val TAG = "RemoteDexterLoader"

    fun loadNativeLibrary() {
        try {
            System.loadLibrary("remotedexter")
            RemoteDexterNative.libraryLoaded = true
            Log.i(TAG, "remotedexter native library loaded")
        } catch (t: Throwable) {
            RemoteDexterNative.libraryLoaded = false
            Log.w(TAG, "Failed to load remotedexter native library", t)
        }
    }
}
