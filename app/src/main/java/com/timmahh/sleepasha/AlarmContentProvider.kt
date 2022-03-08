package com.timmahh.sleepasha

import android.app.Application
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.provider.BaseColumns
import androidx.lifecycle.MutableLiveData
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import kotlinx.serialization.Serializable

object SleepRecord {
    const val AUTHORITY = "com.urbandroid.sleep.history"
    const val RECORDS_TABLE = "records"

    object Column : BaseColumns {
        val CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + RECORDS_TABLE)
        const val CONTENT_TYPE = "vnd.android.cursor.dir/com.urbandroid.sleep.history"
        const val RECORD_ID = "_id"
        const val START_TIME = "startTime"
        const val LATEST_TO_TIME = "latestToTime"
        const val TO_TIME = "toTime"
        const val FRAMERATE = "framerate"
        const val RATING = "rating"
        const val COMMENT = "comment"
        const val RECORD_DATA = "recordData"
        const val TIMEZONE = "timezone"
        const val LEN_ADJUST = "lenAdjust"
        const val QUALITY = "quality"
        const val SNORE = "snore"
        const val CYCLES = "cycles"
        const val EVENT_LABELS = "eventLabels"
        const val EVENTS = "events"
        const val RECORD_FULL_DATA = "recordFullData"
        const val RECORD_NOISE_DATA = "recordNoiseData"
        const val NOISE_LEVEL = "noiseLevel"
        const val FINISHED = "finished"
        const val GEO = "geo"
        const val LENGTH = "length"
    }
}

object Alarm {
    val CONTENT_URI = Uri.parse("content://com.urbandroid.sleep.alarmclock/alarm")
    val DEFAULT_SORT_ORDER = arrayOf(OrderEntry(Columns.hour), OrderEntry(Columns.minutes))

    enum class Columns {
        _id, hour, minutes, daysofweek, alarmtime, enabled, vibrate, message, alert, suspendtime, ndswakeupwindow
    }
}

fun Alarm.Columns.getColumnIndex(cursor: Cursor) =
    cursor.getColumnIndex(name).takeUnless { it < 0 } ?: ordinal

fun Alarm.Columns.getColumnType(cursor: Cursor) =
    name to when (cursor.getType(getColumnIndex(cursor))) {
        Cursor.FIELD_TYPE_NULL -> null
        Cursor.FIELD_TYPE_BLOB -> "blob"
        Cursor.FIELD_TYPE_INTEGER -> "int"
        Cursor.FIELD_TYPE_FLOAT -> "float"
        Cursor.FIELD_TYPE_STRING -> "string"
        else -> "unknown"
    }

@Serializable
data class AlarmModel(
    val hour: Int,
    val minutes: Int,
    val daysOfWeek: Int,
    val alarmTime: Int,
    val enabled: Int,
    val vibrate: Int,
    val message: String,
    val alert: String,
    val suspendTime: Int,
    val nonDeepSleepWakeupWindow: Int
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readInt(),
        parcel.readInt()
    )

    constructor(cursor: Cursor) : this(
        hour = cursor.getInt(Alarm.Columns.hour.getColumnIndex(cursor)),
        minutes = cursor.getInt(Alarm.Columns.minutes.getColumnIndex(cursor)),
        daysOfWeek = cursor.getInt(Alarm.Columns.daysofweek.getColumnIndex(cursor)),
        alarmTime = cursor.getInt(Alarm.Columns.alarmtime.getColumnIndex(cursor)),
        enabled = cursor.getInt(Alarm.Columns.enabled.getColumnIndex(cursor)),
        vibrate = cursor.getInt(Alarm.Columns.vibrate.getColumnIndex(cursor)),
        message = cursor.getString(Alarm.Columns.message.getColumnIndex(cursor)),
        alert = cursor.getString(Alarm.Columns.alert.getColumnIndex(cursor)),
        suspendTime = cursor.getInt(Alarm.Columns.suspendtime.getColumnIndex(cursor)),
        nonDeepSleepWakeupWindow = cursor.getInt(Alarm.Columns.ndswakeupwindow.getColumnIndex(cursor))
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(hour)
        parcel.writeInt(minutes)
        parcel.writeInt(daysOfWeek)
        parcel.writeInt(alarmTime)
        parcel.writeInt(enabled)
        parcel.writeInt(vibrate)
        parcel.writeString(message)
        parcel.writeString(alert)
        parcel.writeInt(suspendTime)
        parcel.writeInt(nonDeepSleepWakeupWindow)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<AlarmModel> {
        override fun createFromParcel(parcel: Parcel): AlarmModel {
            return AlarmModel(parcel)
        }

        override fun newArray(size: Int): Array<AlarmModel?> {
            return arrayOfNulls(size)
        }
    }
}

class SelectionBuilder {

    inner class Selection internal constructor(
        val column: String,
        val op: String,
        val arg: Any,
        val combiner: String
    ) {
        override fun toString(): String {
            return "$combiner $column $op $arg"
        }
    }

    private val selections = mutableListOf<Selection>()

    private fun addSelection(
        column: Alarm.Columns,
        op: String,
        value: Any,
        combiner: String? = null,
        negate: Boolean
    ) {
        selections += Selection(
            column.name,
            op,
            value,
            "${if (negate) "NOT " else ""}${combiner ?: if (selections.isNotEmpty()) "AND" else ""}"
        )
    }

    protected fun eq(
        column: Alarm.Columns,
        value: Any,
        combiner: String? = null,
        negate: Boolean = false
    ) = addSelection(column, "=", value, combiner, negate)

    protected fun gt(
        column: Alarm.Columns,
        value: Any,
        combiner: String? = null,
        negate: Boolean = false
    ) = addSelection(column, ">", value, combiner, negate)

    protected fun lt(
        column: Alarm.Columns,
        value: Any,
        combiner: String? = null,
        negate: Boolean = false
    ) = addSelection(column, "<", value, combiner, negate)

    protected fun gte(
        column: Alarm.Columns,
        value: Any,
        combiner: String? = null,
        negate: Boolean = false
    ) = addSelection(column, ">=", value, combiner, negate)

    protected fun lte(
        column: Alarm.Columns,
        value: Any,
        combiner: String? = null,
        negate: Boolean = false
    ) = addSelection(column, "<=", value, combiner, negate)

    protected fun neq(
        column: Alarm.Columns,
        value: Any,
        combiner: String? = null,
        negate: Boolean = false
    ) = addSelection(column, "<>", value, combiner, negate)

    protected fun between(
        column: Alarm.Columns,
        value: Pair<*, *>,
        combiner: String? = null,
        negate: Boolean = false
    ) = addSelection(column, "BETWEEN", value, combiner, negate)

    protected fun like(
        column: Alarm.Columns,
        value: String,
        combiner: String? = null,
        negate: Boolean = false
    ) = addSelection(column, "LIKE", value, combiner, negate)

    protected fun contains(
        column: Alarm.Columns,
        value: Array<*>,
        combiner: String? = null,
        negate: Boolean = false
    ) = addSelection(column, "IN", value, combiner, negate)

    fun build(): Pair<String, Array<String>> =
        selections.joinToString(" ") { it.toString() } to selections.map { if (it.arg is String) it.arg else it.arg.toString() }
            .toTypedArray()
}

fun selection(block: SelectionBuilder.() -> Unit) = SelectionBuilder().apply(block).build()

enum class OrderDirection {
    ASC, DESC
}

data class OrderEntry(
    val column: Alarm.Columns,
    val direction: OrderDirection = OrderDirection.ASC
) {
    override fun toString(): String = "${column.name} ${direction.name}"
}


//@Single
//@Named("AlarmContentProvider")
class AlarmContentProvider(application: Application) : LoaderManager.LoaderCallbacks<Cursor> {

    private val cursorLoader by lazy { application.cursorBuilder {} }
    val alarms: MutableLiveData<List<AlarmModel>> = MutableLiveData(emptyList())

    override fun onCreateLoader(
        id: Int,
        args: Bundle?
    ): Loader<Cursor> = cursorLoader

    override fun onLoadFinished(
        loader: Loader<Cursor>,
        data: Cursor?
    ) {
        var counter = 0
        val loadedAlarms = mutableListOf<AlarmModel>()
        if (data?.moveToFirst() == true) {
            do {
                print("Index=$counter")
                loadedAlarms.add(AlarmModel(data))
                print("Finished index=$counter")
                counter++
            } while (data.moveToNext())
        }
        alarms.value = loadedAlarms
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        alarms.value = emptyList()
    }
}

fun Context.cursorBuilder(block: CursorBuilder.() -> Unit) = CursorBuilder(this, block).build()

class CursorBuilder(private val context: Context, block: CursorBuilder.() -> Unit) {

    init {
        apply(block)
    }

    internal var projections: Set<Alarm.Columns> = setOf(
        Alarm.Columns.hour,
        Alarm.Columns.minutes,
        Alarm.Columns.daysofweek,
        Alarm.Columns.alarmtime,
        Alarm.Columns.enabled,
        Alarm.Columns.vibrate,
        Alarm.Columns.message,
        Alarm.Columns.alert,
        Alarm.Columns.suspendtime,
        Alarm.Columns.ndswakeupwindow
    )
    internal var selections: Pair<String?, Array<String>> = null to emptyArray()
    internal var orderBy: Array<OrderEntry> = Alarm.DEFAULT_SORT_ORDER

    fun build() = CursorLoader(
        context,
        Alarm.CONTENT_URI,
        projections.map(Alarm.Columns::name).toTypedArray(),
        selections.first,
        selections.second,
        orderBy.joinToString(", ", transform = OrderEntry::toString)
    )
}