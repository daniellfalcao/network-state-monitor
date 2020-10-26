package com.falcon.sample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.falcon.library.NetworkStateMonitor

class MainActivity : AppCompatActivity() {

    private lateinit var networkStateMonitor: NetworkStateMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        networkStateMonitor = NetworkStateMonitor(application, lifecycle)
        networkStateMonitor.addNetworkStateListener { networkState, networkTransport ->
            println("state -> $networkState, transport -> $networkTransport")
        }
    }
}