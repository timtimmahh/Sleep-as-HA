package com.timmahh.sleepasha.net

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.MqttClientBuilder
import com.hivemq.client.mqtt.lifecycle.MqttClientAutoReconnect
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import java.util.concurrent.TimeUnit

enum class MqttState {
    STARTING,
    STARTED,
    CONNECTED,
    DISCONNECTED,
    STOPPING,
    STOPPED
}

@FunctionalInterface
interface OnStateChanged {
    fun onStateChanged(newState: MqttState, oldState: MqttState)
}

class MqttManager<Mqtt : MqttClient>(
    lifecycleOwner: LifecycleOwner,
    build: MqttClientBuilder.() -> Mqtt
) : DefaultLifecycleObserver {
    private val client: Mqtt by lazy {
        MqttClient.builder()
            .automaticReconnect(
                MqttClientAutoReconnect.builder()
                    .initialDelay(2, TimeUnit.SECONDS)
                    .maxDelay(10, TimeUnit.SECONDS)
                    .build()
            ).addConnectedListener {
                currentState.value = MqttState.CONNECTED
            }.addDisconnectedListener {
                currentState.value = MqttState.CONNECTED
            }.build()
    }

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }

    private val currentState = MutableLiveData(MqttState.STARTING to MqttState.STARTING).apply {
        observe(lifecycleOwner) {

        }
    }

    fun LifecycleOwner.watchState(stateChanged: OnStateChanged) {
        currentState.observe(this) {
             stateChanged.onStateChanged(it.first, it.second)
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        (client as Mqtt5Client).pub
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
    }

    /*private val client = Mqtt5Client.builder().identifier(clientId)
        .serverHost(host)
        .serverPort(port).apply {
            if (sslEnabled)
                sslWithDefaultConfig()
            if (useWebSocket)
                webSocketConfig()
        }
        .sslWithDefaultConfig()*/
}