package com.timmahh.sleepasha

import android.database.Cursor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.timmahh.sleepasha.ui.theme.SleepAsHATheme
import kotlinx.serialization.json.Json
import org.json.JSONObject

val format = Json { prettyPrint = true }

@ExperimentalPermissionsApi
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SleepAsHATheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val alarmPermissionState = rememberPermissionState("com.urbandroid.sleep.READ")
                    when (alarmPermissionState.status) {
                        PermissionStatus.Granted -> {
                            val alarms = remember { mutableStateListOf<AlarmModel>() }
                            lifecycleScope.launchWhenCreated {
                                LoaderManager.getInstance(this@MainActivity).initLoader(
                                    0,
                                    null,
                                    object : LoaderManager.LoaderCallbacks<Cursor> {
                                        override fun onCreateLoader(
                                            id: Int,
                                            args: Bundle?
                                        ): Loader<Cursor> = loadAlarmsFromProvider()

                                        override fun onLoadFinished(
                                            loader: Loader<Cursor>,
                                            data: Cursor?
                                        ) {
                                            var counter = 0
                                            val loadedAlarms = mutableListOf<AlarmModel>()
                                            if (data?.moveToFirst() == true) {
                                                do {
                                                    print("Index=$counter")
                                                    val types = mutableMapOf<String, String?>()
                                                    enumValues<Alarm.Columns>().map { it.getColumnType(data) }.forEach {
                                                        types += it
                                                    }
                                                    print(JSONObject(types as Map<*, *>?).toString(4))
                                                    loadedAlarms.add(AlarmModel(data))
                                                    print("Finished index=$counter")
                                                    counter++
                                                } while (data.moveToNext())
                                            }
                                            alarms.clear()
                                            alarms.addAll(loadedAlarms)
                                        }

                                        override fun onLoaderReset(loader: Loader<Cursor>) {
                                            alarms.clear()
                                        }
                                    })
                            }
                            AlarmList(alarms)
                        }
                        is PermissionStatus.Denied -> {
                            Column {
                                val textToShow = if (alarmPermissionState.status.shouldShowRationale) {
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