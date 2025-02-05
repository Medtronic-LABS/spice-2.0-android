package com.medtroniclabs.opensource.appextensions

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.offlinesync.GetSyncStatusWorker
import com.medtroniclabs.opensource.offlinesync.ScheduledSyncWork
import com.medtroniclabs.opensource.ui.assessment.referrallogic.utils.ReferralStatus
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

const val syncWorkerName = "SyncWorker"
private const val durationGap = 30L // 1 * 60L - 1 hour once. Need to give in minutes
private const val notificationId = 99 // Unique ID for the notification
const val signatureFolder = "signatures"
const val imgFileNameExtension = "JPEG"

const val workerUniqueName = "spicePostWorker"
const val workerUniqueNameForNCD = "spicePostWorkerForNCD"
fun Context.isGpsEnabled(): Boolean {
    val locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
}

fun Context.isFineAndCoarseLocationPermissionGranted(): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

fun Context.getPatientStatus(status: String?): String? {
    status?.let {
        return when (status) {
            ReferralStatus.OnTreatment.name -> {
                this.getString(R.string.on_treatment)
            }

            ReferralStatus.Referred.name -> {
                this.getString(R.string.referred)
            }

            ReferralStatus.Recovered.name -> {
                this.getString(R.string.recovered)
            }

            else -> {
                return null
            }
        }
    } ?: return null
}

fun Context.changePatientStatus(input: String): String {
    val onTreatmentString = getString(R.string.on_treatment)
    val regex = Regex(ReferralStatus.OnTreatment.name, RegexOption.IGNORE_CASE)
    return input.replace(regex, onTreatmentString)
}

fun Context.scheduleSyncWorker() {
    val workManager = WorkManager.getInstance(this)

    val constraint = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    val periodicWorkRequest =
        PeriodicWorkRequestBuilder<ScheduledSyncWork>(durationGap, TimeUnit.MINUTES)
            .setInitialDelay(0, TimeUnit.SECONDS)
            .setConstraints(constraint)
            .build()

    // Enqueue the periodic work request with a unique name and policy
    workManager.enqueueUniquePeriodicWork(
        syncWorkerName,
        ExistingPeriodicWorkPolicy.KEEP,
        periodicWorkRequest
    )

}

fun Context.triggerOneTimeWorker() {
    val workManager = WorkManager.getInstance(this)
    //Only work that is in a terminal state (SUCCEEDED, FAILED, or CANCELLED) and has no unfinished dependent work will be pruned.
    workManager.pruneWork()
    val constraints = Constraints.Builder()
        .setRequiresBatteryNotLow(false) // Requires battery to not be low
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
    val workRequest = OneTimeWorkRequestBuilder<GetSyncStatusWorker>()
        .setConstraints(constraints)
        .build()
    val workerInfos = workManager.getWorkInfosForUniqueWork(workerUniqueNameForNCD).get()
    val noPendingWorker =
        workerInfos?.filter { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.BLOCKED }
            .isNullOrEmpty()
    val existingWorkPolicy =
        if (noPendingWorker) ExistingWorkPolicy.APPEND else ExistingWorkPolicy.KEEP
    workManager.enqueueUniqueWork(workerUniqueNameForNCD, existingWorkPolicy, workRequest)
}

private fun observerStatus(workManager: WorkManager) {
    workManager.getWorkInfosForUniqueWorkLiveData(syncWorkerName).observeForever {
        for (woker in it) {
            Log.e("WORKER_TEST","Id "+woker.id)
            Log.e("WORKER_TEST","Status "+woker.state.name)
        }
    }
}

fun Context.isBackgroundWorkerRunning(): Boolean {
    val workManager = WorkManager.getInstance(this)
    val list = workManager.getWorkInfosForUniqueWork(syncWorkerName).get()
    if (list.isNotEmpty()) {
        return list[0].state == WorkInfo.State.RUNNING
    }
    return false
}

fun Context.cancelAllWorker() {
    val workManager = WorkManager.getInstance(this)
    workManager.cancelUniqueWork(syncWorkerName) // For old Worker
    workManager.cancelUniqueWork(workerUniqueName) // For Automatic sync worker
}

fun Context.showNotification(title: String = "Background Task", message: String = "Syncing Offline data") {
    val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelId = "worker_channel"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(channelId, "Worker Notifications", NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)
    }

    val notification = NotificationCompat.Builder(this, channelId)
        .setContentTitle(title)
        .setContentText(message)
        .setSmallIcon(R.drawable.ic_spice_logo)
        .build()

    notificationManager.notify(notificationId, notification)
}

fun Context.hideNotification() {
    val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.cancel(notificationId)
}

fun Context.saveBitmapAsJpeg(bitmap: Bitmap, fileName: String): Boolean {
    // Get the directory for the app's private files directory
    val directory = File(this.filesDir, signatureFolder)
    if (!directory.exists()) {
        directory.mkdir()
    }

    val fileNameWithExtension = "$fileName.$imgFileNameExtension"

    // Create a file to save the image
    val imageFile = File(directory, fileNameWithExtension)

    var fileOutputStream: FileOutputStream? = null
    return try {
        fileOutputStream = FileOutputStream(imageFile)
        // Compress the bitmap and save it as a JPEG
        bitmap.compress(Bitmap.CompressFormat.JPEG, 30, fileOutputStream)
        true
    } catch (e: IOException) {
        e.printStackTrace()
        false
    } finally {
        try {
            fileOutputStream?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

fun Context.startBackgroundOfflineSync() {
    val workManager = WorkManager.getInstance(this)
    //Only work that is in a terminal state (SUCCEEDED, FAILED, or CANCELLED) and has no unfinished dependent work will be pruned.
    workManager.pruneWork()

    val constraint = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    val postWorker = OneTimeWorkRequestBuilder<ScheduledSyncWork>()
        .setInitialDelay(0, TimeUnit.SECONDS)
        .setConstraints(constraint)
        .build()

    val workerInfos = workManager.getWorkInfosForUniqueWork(workerUniqueName).get()
    val noPendingWorker = workerInfos?.filter { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.BLOCKED }.isNullOrEmpty()

    val existingWorkPolicy =
        if (noPendingWorker) ExistingWorkPolicy.APPEND else ExistingWorkPolicy.KEEP

    workManager.enqueueUniqueWork(workerUniqueName, existingWorkPolicy, postWorker)
}