package com.app.scavenger.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class ConnectionDetector(
    val context: Context
) {
    fun connectedToInternet(): Boolean {
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networks = connectivity.allNetworks
        for (network in networks) {
            connectivity.getNetworkCapabilities(network)?.run {
                if (hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) or hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    return true
                }
            }
        }
        return false
    }
}