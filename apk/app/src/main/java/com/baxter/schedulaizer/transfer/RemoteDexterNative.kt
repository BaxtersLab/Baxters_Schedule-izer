package com.baxter.schedulaizer.transfer

object RemoteDexterNative {
    // Do NOT auto-load native library. Loading should be done explicitly
    // in controlled environments. This flag indicates whether native
    // methods are available. Default is false to avoid accidental JNI calls.
    @Volatile
    var libraryLoaded: Boolean = false

    @JvmStatic
    external fun init(): Boolean

    @JvmStatic
    external fun connect(host: String, port: Int): Boolean

    @JvmStatic
    external fun sendFile(localPath: String, remotePath: String): Boolean

    @JvmStatic
    external fun sendBytes(data: ByteArray, remotePath: String): Boolean

    @JvmStatic
    external fun close()
}
