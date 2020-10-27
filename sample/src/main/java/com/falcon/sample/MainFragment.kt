package com.falcon.sample

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.falcon.library.NetworkStateMonitor
import kotlinx.android.synthetic.main.fragment_main.*

class MainFragment : Fragment() {

    companion object {
        const val FRAGMENT_NUMBER = "number"
    }

    private lateinit var networkStateMonitor: NetworkStateMonitor
    private val fragmentNumber: Int by lazy { arguments?.getInt(FRAGMENT_NUMBER) ?: 0 }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        networkStateMonitor = NetworkStateMonitor(context, lifecycle)
        networkStateMonitor.addNetworkStateListener { networkState, networkTransport ->
            status.text = "Status = $networkState"
            println("$fragmentNumber state -> $networkState, transport -> $networkTransport")
        }
    }

    override fun onResume() {
        super.onResume()
        println("on fragment resumed! $fragmentNumber")
    }

    override fun onPause() {
        super.onPause()
        println("on fragment paused! $fragmentNumber")
    }

}