package org.medtroniclabs.uhis.network.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import java.io.IOException
import java.net.InetSocketAddress
import javax.net.SocketFactory

/**
 * Send a ping to googles primary DNS.
 * If successful, that means we have internet.
 */
object DoesNetworkHaveInternet {
    private const val HOST_NAME = "8.8.8.8"
    private const val PORT = 53
    private const val TIMEOUT = 1500
    private const val ERROR_MESSAGE = "Socket is null."

    // Make sure to execute this on a background thread.
    fun execute(socketFactory: SocketFactory): Boolean =
        try {
            val socket = socketFactory.createSocket() ?: throw IOException(ERROR_MESSAGE)
            socket.connect(InetSocketAddress(HOST_NAME, PORT), TIMEOUT)
            socket.close()
            true
        } catch (e: IOException) {
            false
        }

    fun hasInternetConnection(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnected
        }
    }
}
