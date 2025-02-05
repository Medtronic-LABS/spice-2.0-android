package com.medtroniclabs.opensource.network.utils

import java.io.IOException
import java.net.InetSocketAddress
import javax.net.SocketFactory


/**
 * Send a ping to googles primary DNS.
 * If successful, that means we have internet.
 */
object DoesNetworkHaveInternet {

    private const val hostName = "8.8.8.8"
    private const val port = 53
    private const val timeout = 1500
    private const val errorMessage = "Socket is null."
    // Make sure to execute this on a background thread.
    fun execute(socketFactory: SocketFactory): Boolean {
        return try {
            val socket = socketFactory.createSocket() ?: throw IOException(errorMessage)
            socket.connect(InetSocketAddress(hostName, port), timeout)
            socket.close()
            true
        } catch (e: IOException) {
            false
        }
    }
}