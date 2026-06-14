package com.baxter.schedulaizer.data.db

import androidx.room.TypeConverter
import java.util.Date
import com.baxter.schedulaizer.data.db.entity.TransferState

class AppTypeConverters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time

    @TypeConverter
    fun transferStateToString(state: TransferState?): String? = state?.name

    @TypeConverter
    fun stringToTransferState(value: String?): TransferState? = value?.let { TransferState.valueOf(it) }
}
