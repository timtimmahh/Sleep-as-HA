package com.timmahh.sleepasha.net

import androidx.lifecycle.*
import com.hivemq.client.internal.mqtt.message.connect.connack.MqttConnAck
import com.hivemq.client.internal.mqtt.util.MqttChecks.publish
import com.hivemq.client.mqtt.MqttClientState
import com.hivemq.client.mqtt.datatypes.MqttClientIdentifier
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAck
import com.hivemq.client.mqtt.mqtt5.message.disconnect.Mqtt5Disconnect
import com.hivemq.client.mqtt.mqtt5.message.disconnect.Mqtt5DisconnectBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.TimeUnit

sealed class MqttEvent {
    object START : MqttEvent()
    object END : MqttEvent()
}

class MqttManager(
//    lifecycleOwner: LifecycleOwner = ProcessLifecycleOwner.get(),
    builder: MqttClientDsl.() -> Unit
) : DefaultLifecycleObserver {
    private val client by lazy {
        mqttClient {
            automaticReconnect {
                initialDelay = 2L to TimeUnit.SECONDS
                maxDelay = 10L to TimeUnit.SECONDS
            }
            onConnected { currentState.postValue(it) }
            onDisconnect { currentState.postValue(it) }
            apply(builder)
        }
    }
    val identifier: Optional<MqttClientIdentifier>
        get() = client.config.clientIdentifier

    val state: MqttClientState
        get() = client.state

    private var lastConnect: (MqttConnectDsl.() -> Unit)? = null
    private var lastConnectAck: Mqtt5ConnAck? = null
    private var lastDisconnect: (Mqtt5DisconnectBuilder.() -> Unit)? = null

//    private val stateChanges = mutableListOf<(MqttClientState) -> Unit>()

    val currentState = MutableLiveData(MqttClientState.DISCONNECTED)
/*
    fun watchState(stateChange: (MqttClientState) -> Unit) {
        stateChanges += stateChange
    }

    fun LifecycleOwner.watchState(stateChanged: (MqttClientState) -> Unit) {
        currentState.observe(this) {
            stateChanged(it)
        }
    }*/

    suspend fun connect(builder: (MqttConnectDsl.() -> Unit)? = null) =
        when (state) {
            MqttClientState.DISCONNECTED -> {
                lastConnectAck = withContext(Dispatchers.Default) {
                    lastConnect = builder
                    builder?.let { client.connect(mqttConnect(it)) }
                        ?: client.connect()
                }
                lastConnectAck
            }
            else -> lastConnectAck
        }


    suspend fun disconnect(builder: (Mqtt5DisconnectBuilder.() -> Unit)? = null) =
        withContext(Dispatchers.Default) {
            lastDisconnect = builder
            builder?.let { client.disconnect(Mqtt5Disconnect.builder().apply(it).build()) }
                ?: client.disconnect()
        }

    suspend fun publish(builder: MqttPublishDsl.() -> Unit) =
        withContext(Dispatchers.Default) {
            client.publish(mqttPublish(builder))
        }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        if (lastConnect != null)
            owner.lifecycleScope.launchWhenResumed {
                connect(lastConnect)
            }
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        if (lastDisconnect != null)
            owner.lifecycleScope.launch {
                disconnect(lastDisconnect)
            }
    }
}