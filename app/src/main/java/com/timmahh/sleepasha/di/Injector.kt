package com.timmahh.sleepasha.di

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.timmahh.sleepasha.AlarmContentProvider
import com.timmahh.sleepasha.net.MqttManager
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan("com.timmahh.sleepasha")
class AppModule {
    @Single
    fun alarmContentProvider(application: Application) = AlarmContentProvider(application)

    @Single
    fun mqttManager() = MqttManager(ProcessLifecycleOwner.get()) {
        serverHost = "condo-homeassistant.duckdns.org"
        serverPort = 8883
        sslConfig { }
    }
}