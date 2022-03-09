package com.timmahh.sleepasha.data

import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.hivemq.client.internal.mqtt.message.publish.MqttPublishResult
import com.hivemq.client.mqtt.MqttClientState
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAck
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PayloadFormatIndicator
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishResult
import com.timmahh.sleepasha.AlarmContentProvider
import com.timmahh.sleepasha.AlarmModel
import com.timmahh.sleepasha.net.MqttManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

@Single
class AlarmRepository(
    private val mqttManager: MqttManager,
    private val alarmContentProvider: AlarmContentProvider
) {

    val mqttState: MutableLiveData<MqttClientState>
        get() = mqttManager.currentState

    suspend fun connectMqtt() =
        mqttManager.connect {
            simpleAuth = "timtimmahh" to "Unknown1"
        }

    suspend fun publishAlarms(alarms: List<AlarmModel>, topicName: String) : Mqtt5PublishResult {
        Log.d("AlarmRepository", "publishAlarms: (publishing to " +
                "SleepAsAndroid/$topicName/alarms")
        return mqttManager.publish {
            topic = "SleepAsAndroid/${topicName}/alarms"
            payload = Json.encodeToString(alarms).encodeToByteArray()
            qos = MqttQos.AT_LEAST_ONCE
            payloadFormatIndicator = Mqtt5PayloadFormatIndicator.UTF_8
            contentType = "application/json"
            retain = true
        }
    }

    suspend fun fetchAlarms(dispatcher: CoroutineDispatcher = Dispatchers.IO): List<AlarmModel> {
        Log.d("AlarmRepository", "fetching alarms")
        return withContext(dispatcher) {
            alarmContentProvider.fetchAllAlarms {  }
        }
    }

    fun linkLifecycleToMqtt(owner: LifecycleOwner) = owner.lifecycle.addObserver(mqttManager)
}