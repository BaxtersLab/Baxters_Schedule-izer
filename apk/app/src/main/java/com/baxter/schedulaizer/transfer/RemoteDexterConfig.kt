package com.baxter.schedulaizer.transfer

data class RemoteDexterConfig(
    val host: String = "192.168.1.100",
    val port: Int = 9000,
    val connectTimeoutMs: Int = 5000,
    val readTimeoutMs: Int = 10000,
    val retryCount: Int = 2
)

object RemoteDexterConfigProvider {
    @Volatile
    private var _config: RemoteDexterConfig = RemoteDexterConfig()

    fun get(): RemoteDexterConfig = _config

    fun set(config: RemoteDexterConfig) {
        _config = config
    }
}
