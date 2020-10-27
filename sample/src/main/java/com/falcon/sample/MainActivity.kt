package com.falcon.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.falcon.library.NetworkStateMonitor
import com.falcon.sample.MainFragment.Companion.FRAGMENT_NUMBER

class MainActivity : AppCompatActivity() {

    lateinit var networkStateMonitor: NetworkStateMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onAttachFragment(fragment: Fragment) {
        super.onAttachFragment(fragment)
        val fragmentNumber = when (fragment.id) {
            R.id.fragment -> 1
            R.id.fragment2 -> 2
            R.id.fragment3 -> 3
            else -> 0
        }
        Bundle().apply { putInt(FRAGMENT_NUMBER, fragmentNumber) }.also { fragment.arguments = it }
    }

}