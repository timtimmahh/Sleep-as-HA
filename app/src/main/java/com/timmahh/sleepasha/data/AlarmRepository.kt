package com.timmahh.sleepasha.data

import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAck
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PayloadFormatIndicator
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishResult
import com.timmahh.sleepasha.AlarmModel
import com.timmahh.sleepasha.net.MqttManager
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single


interface AlarmRepository {
    suspend fun connectMqtt(): Mqtt5ConnAck

    suspend fun publishAlarms(alarms: List<AlarmModel>, topicName: String): Mqtt5PublishResult
}

@Single
class AlarmRepositoryImpl(private val mqttManager: MqttManager) : AlarmRepository {

    override suspend fun connectMqtt() =
        mqttManager.connect {
            simpleAuth = "timtimmahh" to "Unknown1"
        }

    override suspend fun publishAlarms(alarms: List<AlarmModel>, topicName: String) =
        mqttManager.publish {
            topic = "SleepAsAndroid/${topicName}/alarms"
            payload = Json.encodeToString(alarms).encodeToByteArray()
            qos = MqttQos.AT_LEAST_ONCE
            payloadFormatIndicator = Mqtt5PayloadFormatIndicator.UTF_8
            contentType = "application/json"
        }
}