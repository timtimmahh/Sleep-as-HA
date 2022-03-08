package com.timmahh.sleepasha

import android.app.Application
import com.timmahh.sleepasha.di.AppModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.ksp.generated.module


class SleepAsHAApp : Application() {

    /*private val mqttManager by inject<MqttManager> {
        fun MqttClientDsl.builder() {
            serverHost = "condo-homeassistant.duckdns.org"
            serverPort = 8883
            sslConfig { }
        }
        parametersOf(MqttClientDsl::builder)
    }*/

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@SleepAsHAApp)
            modules(AppModule().module)
        }
        /*ProcessLifecycleOwner.get().lifecycleScope.launchWhenResumed {
            val result = mqttManager.connect {
                this.simpleAuth = "timtimmahh" to "Unknown1"
            }
        }*/
    }


    override fun onTerminate() {
        super.onTerminate()
    }
}