package com.machiav3lli.backup.items

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.machiav3lli.backup.utils.FileUtils
import com.machiav3lli.backup.utils.GsonUtils.instance
import com.machiav3lli.backup.utils.LogUtils
import org.apache.commons.io.IOUtils
import java.io.FileNotFoundException
import java.io.IOException
import java.time.LocalDateTime

open class LogItem : Parcelable {
    @SerializedName("logDate")
    @Expose
    lateinit var logDate: LocalDateTime
        private set

    @SerializedName("deviceName")
    @Expose
    val deviceName: String?

    @SerializedName("sdkVersion")
    @Expose
    val sdkCodename: String?

    @SerializedName("cpuArch")
    @Expose
    val cpuArch: String?

    @SerializedName("logText")
    @Expose
    val logText: String?

    constructor(text: String, date: LocalDateTime) {
        this.logDate = date
        this.deviceName = android.os.Build.DEVICE
        this.sdkCodename = android.os.Build.VERSION.RELEASE
        this.cpuArch = android.os.Build.SUPPORTED_ABIS[0]
        this.logText = text
    }

    constructor(context: Context, logFile: StorageFile) {
        try {
            FileUtils.openFileForReading(context, logFile.uri).use { reader ->
                val item = fromGson(IOUtils.toString(reader))
                this.logDate = item.logDate
                this.deviceName = item.deviceName
                this.sdkCodename = item.sdkCodename
                this.cpuArch = item.cpuArch
                this.logText = item.logText
            }
        } catch (e: FileNotFoundException) {
            throw BackupItem.BrokenBackupException("Cannot open ${logFile.name} at URI ${logFile.uri}", e)
        } catch (e: IOException) {
            throw BackupItem.BrokenBackupException("Cannot read ${logFile.name} at URI ${logFile.uri}", e)
        } catch (e: Throwable) {
            LogUtils.unhandledException(e, logFile.uri)
            throw BackupItem.BrokenBackupException("Unable to process ${logFile.name} at URI ${logFile.uri}. [${e.javaClass.canonicalName}] $e")
        }
    }

    protected constructor(source: Parcel) {
        deviceName = source.readString()
        sdkCodename = source.readString()
        cpuArch = source.readString()
        logText = source.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(deviceName)
        parcel.writeString(sdkCodename)
        parcel.writeString(cpuArch)
        parcel.writeString(logText)
    }

    override fun describeContents(): Int {
        return 0
    }

    fun toGson(): String {
        return instance!!.toJson(this)
    }

    override fun toString(): String {
        return "LogItem{" +
                "logDate=$logDate" +
                ", deviceName='$deviceName'" +
                ", sdkCodename='$sdkCodename'" +
                ", cpuArch='$cpuArch'" +
                ", logText:\n$logText" +
                '}'
    }

    fun delete(context: Context): Boolean? {
        val logFile = LogUtils(context).getLogFile(this.logDate)
        return logFile?.delete()
    }

    companion object {
        const val LOG_INSTANCE = "%s.log"
        val CREATOR: Parcelable.Creator<LogItem?> = object : Parcelable.Creator<LogItem?> {
            override fun createFromParcel(source: Parcel): LogItem {
                return LogItem(source)
            }

            override fun newArray(size: Int): Array<LogItem?> {
                return arrayOfNulls(size)
            }
        }

        fun fromGson(gson: String?): LogItem {
            return instance!!.fromJson(gson, LogItem::class.java)
        }
    }
}