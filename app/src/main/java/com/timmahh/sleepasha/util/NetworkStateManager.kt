package com.timmahh.sleepasha.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import androidx.lifecycle.*

enum class NetworkState {
    CONNECTED, WILL_DISCONNECT, DISCONNECTED, UNAVAILABLE
}

class NetworkStateManager(applicationContext: Context, lifecycleOwner: LifecycleOwner, vararg networkObservers: Array<Observer<NetworkState>>) : DefaultLifecycleObserver, NetworkCallback() {

    private val conManager =
        applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val networkStateLiveData = MutableLiveData(NetworkState.UNAVAILABLE).apply {
        observe(lifecycleOwner, Observer {

        })
    }
    private val stateObservers = networkObservers

    fun observeNetworkState(destData: MediatorLiveData<NetworkState>) {

    }


    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)

        conManager.registerDefaultNetworkCallback(this)
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)

        conManager.unregisterNetworkCallback(this)
    }

    override fun onAvailable(network: Network) {
        super.onAvailable(network)
    }

    override fun onLosing(network: Network, maxMsToLive: Int) {
        super.onLosing(network, maxMsToLive)
    }

    override fun onLost(network: Network) {
        super.onLost(network)
    }

    override fun onUnavailable() {
        super.onUnavailable()

    }
}