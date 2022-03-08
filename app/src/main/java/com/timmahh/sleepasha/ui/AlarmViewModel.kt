package com.timmahh.sleepasha.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
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
    val repo: AlarmRepository,
    val alarmContentProvider: AlarmContentProvider
) : AndroidViewModel(application) {

    val alarmsLiveData = MediatorLiveData<List<AlarmModel>>()

    init {
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
    }

}