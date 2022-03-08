package com.timmahh.sleepasha

import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.OutlinedTextField
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.loader.app.LoaderManager
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.timmahh.sleepasha.ui.AlarmViewModel
import com.timmahh.sleepasha.ui.theme.SleepAsHATheme
import com.timmahh.sleepasha.util.MQTT_PREFERENCES
import com.timmahh.sleepasha.util.MQTT_PREF_TOPIC
import org.koin.androidx.compose.get
import org.koin.androidx.compose.getViewModel

@ExperimentalPermissionsApi
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SleepAsHATheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    val alarmPermissionState = rememberPermissionState("com.urbandroid.sleep.READ")
                    when (alarmPermissionState.status) {
                        PermissionStatus.Granted -> {
                            val sharedPref = remember {
                                getSharedPreferences(
                                    MQTT_PREFERENCES, Context.MODE_PRIVATE
                                )
                            }
                            val (prefTopic, setPrefTopic) = remember {
                                mutableStateOf(
                                    sharedPref.getString(MQTT_PREF_TOPIC, null) ?: ""
                                )
                            }
                            val (backupPref, setBackupPref) = remember {
                                mutableStateOf(
                                    sharedPref.getString(MQTT_PREF_TOPIC, null)
                                )
                            }
                            sharedPref.registerOnSharedPreferenceChangeListener { sharedPreferences, key ->
                                sharedPreferences.getString(key, null)?.let {
                                    setBackupPref(it)
                                }
                            }
                            if (backupPref == null) SetupView(
                                sharedPref,
                                prefTopic,
                                setPrefTopic
                            )
                            else AlarmRoot(this@MainActivity)
                        }
                        is PermissionStatus.Denied -> {
                            Column {
                                val textToShow =
                                    if (alarmPermissionState.status.shouldShowRationale) {
                                        "Please grant permission to view/modify the alarms from Sleep as Android."
                                    } else "Read/Write permission required to view/modify alarms from Sleep as Android. Please grant the permission."
                                Text(textToShow)
                                Button(onClick = { alarmPermissionState.launchPermissionRequest() }) {
                                    Text("Request permission")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun SetupView(sharedPrefs: SharedPreferences, text: String, setText: (String) -> Unit) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(text, setText, label = {
                Text("MQTT Topic Name")
            }, singleLine = true, modifier = Modifier.weight(1F, true))
            ElevatedButton(onClick = {
                sharedPrefs.edit { putString(MQTT_PREF_TOPIC, text) }
                Toast.makeText(
                    this@MainActivity, "Updated topic to ${
                        sharedPrefs.getString(MQTT_PREF_TOPIC, null)
                    }", Toast.LENGTH_SHORT
                ).show()
            }, Modifier.weight(1F, true)) {
                Text("Set Topic")
            }
        }
    }

/*
    @Preview(showBackground = true)
    @Composable
    fun SetupPreview() {
        SleepAsHATheme {
            val sharedPrefs = getSharedPreferences(MQTT_PREFERENCES, Context.MODE_PRIVATE)
            val (text, setText) = remember {
                mutableStateOf(
                    sharedPrefs.getString
                        (MQTT_PREF_TOPIC, null) ?: ""
                )
            }
            SetupView(getSharedPreferences(MQTT_PREFERENCES, Context.MODE_PRIVATE), text, setText)
        }
    }*/
}


@Composable
fun <T> AlarmRoot(
    owner: T,
    alarmContentProvider: AlarmContentProvider = get()
) where T : LifecycleOwner, T : ViewModelStoreOwner {

    val alarmViewModel = getViewModel<AlarmViewModel>()
//        val alarms = remember { mutableStateListOf<AlarmModel>() }
    val alarms: List<AlarmModel> by alarmViewModel.alarmsLiveData.observeAsState(listOf())
    owner.lifecycleScope.launchWhenCreated {
        LoaderManager.getInstance(owner).initLoader<Cursor>(
            0, null, alarmContentProvider
        )
    }
    AlarmList(alarms)
}

@Composable
fun AlarmList(alarms: List<AlarmModel> = emptyList()) {
    LazyColumn {
        items(alarms, key = { alarm -> alarm }) { alarm ->
            AlarmItem(alarm)
        }
    }
}

@Composable
fun AlarmItem(alarm: AlarmModel) = Column(modifier = Modifier.padding(16.dp)) {
    Row {
        Text("${alarm.hour}:${alarm.minutes}")
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    SleepAsHATheme {
        AlarmList()
    }
}