package com.timmahh.sleepasha.net

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.MqttClientBuilder
import com.hivemq.client.mqtt.lifecycle.MqttClientAutoReconnect
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import com.hivemq.client.mqtt.mqtt5.Mqtt5ClientBuilder
import com.hivemq.client.mqtt.mqtt5.message.connect.Mqtt5Connect
import com.hivemq.client.mqtt.mqtt5.message.connect.Mqtt5ConnectBuilder
import com.hivemq.client.mqtt.mqtt5.message.disconnect.Mqtt5Disconnect
import com.hivemq.client.mqtt.mqtt5.message.disconnect.Mqtt5DisconnectBuilder
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishBuilder
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishBuilderBase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

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

class MqttManager(
    lifecycleOwner: LifecycleOwner,
    builder: Mqtt5ClientBuilder.() -> Unit
) : DefaultLifecycleObserver {
    private val client by lazy {
        Mqtt5Client.builder()
            .automaticReconnect(
                MqttClientAutoReconnect.builder()
                    .initialDelay(2, TimeUnit.SECONDS)
                    .maxDelay(10, TimeUnit.SECONDS)
                    .build()
            ).addConnectedListener {
                currentState.value = MqttState.CONNECTED
            }.addDisconnectedListener {
                currentState.value = MqttState.CONNECTED
            }.apply(builder).buildBlocking()
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

    suspend fun connect(builder: (Mqtt5ConnectBuilder.() -> Unit)? = null) =
        coroutineScope {
            async {
                builder?.let { client.connect(Mqtt5Connect.builder().apply(it).build()) }
                    ?: client.connect()
            }
        }

    suspend fun disconnect(builder: (Mqtt5DisconnectBuilder.() -> Unit)? = null) =
        coroutineScope {
            async {
                builder?.let { client.disconnect(Mqtt5Disconnect.builder().apply(it).build()) }
                    ?: client.disconnect()
            }
        }

    suspend fun publish(builder: (Mqtt5PublishBuilderBase.Complete<*>.() -> Unit)? = null) =
        coroutineScope {
            async {
                builder?.let { client.publish(Mqtt5Publish.builder().topic("")) }
            }
        }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)

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