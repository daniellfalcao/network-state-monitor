package com.falcon.library.data

enum class NetworkTransport {
    NONE,
    MOBILE_DATA,
    WI_FI,
    ETHERNET;

    companion object {

        fun NetworkTransport.toState(): NetworkState {
            return if (this == NONE) NetworkState.OFFLINE else NetworkState.ONLINE
        }
    }
}