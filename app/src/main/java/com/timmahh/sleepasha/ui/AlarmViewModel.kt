package com.timmahh.sleepasha.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import com.hivemq.client.mqtt.MqttClientState
import com.timmahh.sleepasha.AlarmContentProvider
import com.timmahh.sleepasha.AlarmModel
import com.timmahh.sleepasha.data.AlarmRepository
import com.timmahh.sleepasha.util.MQTT_PREFERENCES
import com.timmahh.sleepasha.util.MQTT_PREF_TOPIC
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class AlarmViewModel(
    application: Application,
    private val repo: AlarmRepository
) : AndroidViewModel(application) {

    val alarmsLiveData: LiveData<List<AlarmModel>> = liveData {
        val alarms = repo.fetchAlarms()
        /*if (repo.mqttState.value == MqttClientState.DISCONNECTED)
            repo.connectMqtt()
        else if (repo.mqttState.value?.isConnected == true)
            */
        repo.publishAlarms(
            alarms, application.getSharedPreferences(
                MQTT_PREFERENCES, Context
                    .MODE_PRIVATE
            ).getString(MQTT_PREF_TOPIC, null) ?: ""
        )
        emit(alarms)
    }
    val mqttStateLiveData: LiveData<MqttClientState> = repo.mqttState

    fun linkLifecycle(owner: LifecycleOwner) = repo.linkLifecycleToMqtt(owner)

    fun mqttConnect() = viewModelScope.launch { repo.connectMqtt() }
//    var mqttConnectReady: Boolean = false

    /*init {
        mqttStateLiveData.addSource(repo.mqttState) {
            mqttConnectReady = it.isConnected
        }
        viewModelScope.launch {
            repo.connectMqtt()
            alarmsLiveData.addSource(alarmContentProvider.alarms) {
                viewModelScope.launch {
                    repo.publishAlarms(
                        it, application.getSharedPreferences(
                            MQTT_PREFERENCES, Context.MODE_PRIVATE
                        ).getString(MQTT_PREF_TOPIC, "") ?: ""
                    )
                }
            }
        }
    }*/

}