package com.timmahh.sleepasha.net

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.hivemq.client.mqtt.MqttClientState
import com.hivemq.client.mqtt.datatypes.MqttClientIdentifier
import com.hivemq.client.mqtt.mqtt5.message.disconnect.Mqtt5Disconnect
import com.hivemq.client.mqtt.mqtt5.message.disconnect.Mqtt5DisconnectBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.TimeUnit

class MqttManager(
//    lifecycleOwner: LifecycleOwner = ProcessLifecycleOwner.get(),
    builder: MqttClientDsl.() -> Unit
)/* : DefaultLifecycleObserver*/ {
    private val client by lazy {
        mqttClient {
            automaticReconnect {
                initialDelay = 2L to TimeUnit.SECONDS
                maxDelay = 10L to TimeUnit.SECONDS
            }
            onConnected { currentState.value = it }
            onDisconnect { currentState.value = it }
            apply(builder)
        }
    }
    val identifier: Optional<MqttClientIdentifier>
        get() = client.config.clientIdentifier

    val state: MqttClientState
        get() = client.state

    /*init {
        lifecycleOwner.lifecycle.addObserver(this)
    }*/

    private val stateChanges = mutableListOf<(MqttClientState) -> Unit>()

    private val currentState = MutableLiveData(MqttClientState.DISCONNECTED).apply {
        observeForever {
            stateChanges.forEach { stateChange -> stateChange(it) }
        }
    }

    fun watchState(stateChange: (MqttClientState) -> Unit) {
        stateChanges += stateChange
    }

    fun LifecycleOwner.watchState(stateChanged: (MqttClientState) -> Unit) {
        currentState.observe(this) {
            stateChanged(it)
        }
    }

    suspend fun connect(builder: (MqttConnectDsl.() -> Unit)? = null) =
        withContext(Dispatchers.Default) {
            builder?.let { client.connect(mqttConnect(it)) }
                ?: client.connect()
        }

    suspend fun disconnect(builder: (Mqtt5DisconnectBuilder.() -> Unit)? = null) =
        withContext(Dispatchers.Default) {
            builder?.let { client.disconnect(Mqtt5Disconnect.builder().apply(it).build()) }
                ?: client.disconnect()
        }

    suspend fun publish(builder: MqttPublishDsl.() -> Unit) =
        withContext(Dispatchers.Default) {
            client.publish(mqttPublish(builder))
        }
}