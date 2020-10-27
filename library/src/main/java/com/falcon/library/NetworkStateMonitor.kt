package com.falcon.library

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.falcon.library.data.NetworkState
import com.falcon.library.data.NetworkTransport
import com.falcon.library.data.NetworkTransport.Companion.toState
import com.falcon.library.extension.networkTransport
import com.falcon.library.listener.NetworkStateListener

class NetworkStateMonitor(
    private val context: Context,
    private val lifecycle: Lifecycle
) : LifecycleObserver {

    var networkState: NetworkState = NetworkState.OFFLINE
    var networkTransport: NetworkTransport = NetworkTransport.NONE

    /** Saves the initial state of the monitor when services start. */
    private var initialNetworkState: NetworkState = NetworkState.OFFLINE
    private var isInitialNetworkStateHandled = false

    /** Request the connectivity system service to track network changes. */
    private val connectivityManagerService: ConnectivityManager? by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    }

    /** Broadcast receiver used to dispatch network changes in api 21 ~ 22. */
    private val networkBroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                onNetworkTransportChanged(connectivityManagerService.networkTransport())
            }
        }
    }

    /** Setup the network request to track connection by connection capabilities. */
    private val networkRequest: NetworkRequest by lazy {
        NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
            .build()
    }

    /** Setup the network callback to bring info about the network status. */
    private val networkCallback: ConnectivityManager.NetworkCallback by lazy {
        object : ConnectivityManager.NetworkCallback() {

            /**
             * When [NetworkCapabilities] has changed the current method is called with the active
             * [network], when there are no more [network] available, this method will not be called
             * and the connection change treatment will be done by [onLost].
             *
             * */
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                // attempt trigger the network changes listener.
                onNetworkTransportChanged(networkCapabilities.networkTransport())
            }

            /** When [network] is no longer available this method is called. */
            override fun onLost(network: Network) {
                // attempt trigger the network changes listener.
                onNetworkTransportChanged(connectivityManagerService.networkTransport())
            }
        }
    }

    /** The list of network listeners to be used in java and kotlin. */
    private val networkStateListenerJava = mutableListOf<NetworkStateListener>()
    private val networkStateListenerKotlin = mutableListOf<(
        networkState: NetworkState,
        networkTransport: NetworkTransport
    ) -> Unit>()

    init {
        // register lifecycle events to this class
        lifecycle.addObserver(this)
    }

    /** Register a listener to network state changes. */
    fun addNetworkStateListener(listener: NetworkStateListener) = apply {
        networkStateListenerJava.add(listener)
    }

    /**
     * Remove a listener that has been registered before. The [listener] must be contained within
     * in [networkStateListenerJava].
     *
     * */
    fun removeNetworkStateListener(listener: NetworkStateListener) = apply {
        networkStateListenerJava.remove(listener)
    }

    /** Register a listener to network state changes. */
    fun addNetworkStateListener(
        listener: (networkState: NetworkState, networkTransport: NetworkTransport) -> Unit
    ) = apply {
        networkStateListenerKotlin.add(listener)
    }

    /**
     * Remove a listener that has been registered before. The [listener] must be contained within
     * in [networkStateListenerKotlin].
     *
     * */
    fun removeNetworkStateListener(
        listener: (networkState: NetworkState, networkTransport: NetworkTransport) -> Unit
    ) = apply {
        networkStateListenerKotlin.remove(listener)
    }

    /**
     * When lifecycle is [Lifecycle.Event.ON_RESUME] the [NetworkStateMonitor] starts observing the
     * network state changes. The service is only paused when the lifecycle is
     * [Lifecycle.Event.ON_PAUSE].
     *
     * */
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun startNetworkObserver() {
        // when services starts, save the initial state
        initialNetworkState = connectivityManagerService.networkTransport().toState()
        isInitialNetworkStateHandled = false
        // start network changes callback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            connectivityManagerService?.registerNetworkCallback(networkRequest, networkCallback)
        } else {
            @Suppress("DEPRECATION")
            IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION).also {
                context.registerReceiver(networkBroadcastReceiver, it)
            }
        }
    }

    /**
     * When lifecycle is [Lifecycle.Event.ON_PAUSE] the [NetworkStateMonitor] stops observing the
     * network state changes. The service is only restarted when the lifecycle is
     * [Lifecycle.Event.ON_RESUME].
     *
     * */
    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    private fun stopNetworkObserver() {
        // stop network changes callback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            connectivityManagerService?.unregisterNetworkCallback(networkCallback)
        } else {
            context.unregisterReceiver(networkBroadcastReceiver)
        }
    }

    /**
     * When lifecycle is [Lifecycle.Event.ON_DESTROY] the [NetworkStateMonitor] destroy all listeners
     * and the lifecycle observe is stopped.
     *
     * */
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun destroyNetworkObserver() {
        // unregister lifecycle events of this class
        lifecycle.removeObserver(this)
        // remove listeners
        networkStateListenerJava.clear()
        networkStateListenerKotlin.clear()
    }

    /**
     * When a [newNetworkTransport] appears, is checked if there has been any change based on the
     * previous state, if there has been any change when the state has occurred a callback is
     * generated for the observers.
     *
     * @param newNetworkTransport the new transport used by network
     *
     * */
    private fun onNetworkTransportChanged(newNetworkTransport: NetworkTransport) {
        // check if state has been changed comparing with the last state
        val networkStateHasChanged = networkState != newNetworkTransport.toState()
        // update connection state and type, with the new values
        networkState = newNetworkTransport.toState()
        networkTransport = newNetworkTransport
        // check if the first network state changed is actually the current state
        if (!isInitialNetworkStateHandled) {
            isInitialNetworkStateHandled = true
            // return to avoid send callback to listeners if the initial state is equals to actual
            if (initialNetworkState == networkState) return
        }
        // if has been detected some change in state, dispatch to observers
        if (networkStateHasChanged) dispatchNetworkStateChanges(networkState, networkTransport)
    }

    /** Notify observers about the network changed state. */
    private fun dispatchNetworkStateChanges(
        networkState: NetworkState,
        networkTransport: NetworkTransport
    ) {
        // change to main thread
        Handler(Looper.getMainLooper()).post {
            networkStateListenerJava.forEach {
                it.onNetworkStateChanged(networkState, networkTransport)
            }
            networkStateListenerKotlin.forEach {
                it.invoke(networkState, networkTransport)
            }
        }
    }
}