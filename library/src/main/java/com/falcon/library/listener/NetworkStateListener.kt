package com.falcon.library.listener

import com.falcon.library.data.NetworkState
import com.falcon.library.data.NetworkTransport

interface NetworkStateListener {
    fun onNetworkStateChanged(networkState: NetworkState, networkTransport: NetworkTransport)
}