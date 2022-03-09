package com.timmahh.sleepasha

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.timmahh.sleepasha.net.MqttEvent
import com.timmahh.sleepasha.net.MqttManager
import com.timmahh.sleepasha.util.ACTION_START_MQTT_SERVICE
import com.timmahh.sleepasha.util.ACTION_STOP_MQTT_SERVICE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.core.annotation.Single


@Single
class MqttService : LifecycleService() {

    private val mqttManager: MqttManager by inject()

    companion object {
        val mqttEvent = MutableLiveData<MqttEvent>(MqttEvent.END)
    }

    override fun onCreate() {
        super.onCreate()
        initValues()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_START_MQTT_SERVICE -> startService()
                ACTION_STOP_MQTT_SERVICE -> stopService()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun initValues() {
        mqttEvent.postValue(MqttEvent.END)
    }

    private fun startService() {
        mqttEvent.postValue(MqttEvent.START)
    }

    private fun stopService() {
        mqttEvent.postValue(MqttEvent.END)
        initValues()
        stopSelf()
    }

    private fun connectMqtt() {
        CoroutineScope(Dispatchers.IO).launch {
            mqttManager.connect {  }
        }
    }
}