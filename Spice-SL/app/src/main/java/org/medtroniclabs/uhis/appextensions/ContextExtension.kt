package org.medtroniclabs.uhis.appextensions

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.location.LocationManager
import android.os.Build
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import org.medtroniclabs.uhis.BuildConfig
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.offlinesync.GetSyncStatusWorker
import org.medtroniclabs.uhis.offlinesync.ScheduledSyncWork
import org.medtroniclabs.uhis.ui.assessment.referrallogic.utils.ReferralStatus
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val NOTIFICATION_ID = 99 // Unique ID for the notification
const val SIGNATURE_FOLDER = "signatures"
const val IMG_FILE_NAME_EXTENSION = "JPEG"

const val WORKER_UNIQUE_NAME = "spicePostWorker"
const val WORKER_UNIQUE_NAME_FOR_NCD = "spicePostWorkerForNCD"

fun Context.isGpsEnabled(): Boolean {
    val locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
}

fun Context.isFineAndCoarseLocationPermissionGranted(): Boolean =
    ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

fun Context.getPatientStatus(status: String?): String? =
    status?.let {
        when (status) {
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
                null
            }
        }
    }

fun Context.changePatientStatus(input: String): String {
    val onTreatmentString = getString(R.string.on_treatment)
    val regex = Regex(ReferralStatus.OnTreatment.name, RegexOption.IGNORE_CASE)
    return input.replace(regex, onTreatmentString)
}

fun Context.triggerOneTimeWorker() {
    val workManager = WorkManager.getInstance(this)
    // Only work that is in a terminal state (SUCCEEDED, FAILED, or CANCELLED) and has no unfinished dependent work will be pruned.
    workManager.pruneWork()
    val constraints = Constraints
        .Builder()
        .setRequiresBatteryNotLow(false) // Requires battery to not be low
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
    val workRequest = OneTimeWorkRequestBuilder<GetSyncStatusWorker>()
        .setConstraints(constraints)
        .build()
    workManager.enqueueUniqueWork(WORKER_UNIQUE_NAME_FOR_NCD, ExistingWorkPolicy.KEEP, workRequest)
}

fun Context.cancelAllWorker() {
    val workManager = WorkManager.getInstance(this)
    workManager.cancelUniqueWork(WORKER_UNIQUE_NAME) // For Automatic sync worker
}

fun Context.showNotification(
    title: String = "Background Task",
    message: String = "Syncing Offline data",
) {
    val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelId = "worker_channel"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(channelId, "Worker Notifications", NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)
    }

    val notification = NotificationCompat
        .Builder(this, channelId)
        .setContentTitle(title)
        .setContentText(message)
        .setSmallIcon(R.drawable.ic_splash_icon)
        .build()

    notificationManager.notify(NOTIFICATION_ID, notification)
}

fun Context.hideNotification() {
    val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.cancel(NOTIFICATION_ID)
}

fun Context.saveBitmapAsJpeg(
    bitmap: Bitmap,
    fileName: String,
): Boolean {
    // Get the directory for the app's private files directory
    val directory = File(this.filesDir, SIGNATURE_FOLDER)
    if (!directory.exists()) {
        directory.mkdir()
    }

    val fileNameWithExtension = "$fileName.$IMG_FILE_NAME_EXTENSION"

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
    // Only work that is in a terminal state (SUCCEEDED, FAILED, or CANCELLED) and has no unfinished dependent work will be pruned.
    workManager.pruneWork()

    val constraint = Constraints
        .Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    val postWorker = OneTimeWorkRequestBuilder<ScheduledSyncWork>()
        .setInitialDelay(0, TimeUnit.SECONDS)
        .setConstraints(constraint)
        .build()
    workManager.enqueueUniqueWork(WORKER_UNIQUE_NAME, ExistingWorkPolicy.KEEP, postWorker)
}

fun Double.toCleanString(): String = if (this % 1.0 == 0.0) this.toInt().toString() else this.toString()

/**
 * Returns a localized string resource for the specified [locale],
 * regardless of the app’s currently active language configuration.
 *
 * This creates a temporary [Context] with an overridden [Configuration]
 * so the requested string is resolved using the provided locale only,
 * without changing the app-wide locale or UI language.
 *
 * Useful for:
 * - Fetching base/default language strings (e.g. English) while app is in another language
 * - Comparing translated vs canonical values
 * - Logging / analytics with consistent language output
 * - Accessing strings for a specific locale on demand
 *
 * Example:
 * ```
 * val englishTitle = context.getStringForLocale(
 *     R.string.title,
 *     Locale.ENGLISH
 * )
 * ```
 *
 * @param resId The string resource ID to resolve.
 * @param locale The target locale used to fetch the resource.
 *
 * @return The localized string for the given resource in the requested locale.
 *
 * @see Configuration.setLocale
 * @see Context.createConfigurationContext
 *
 * Note:
 * - Does not update the app’s global language setting
 * - Does not recreate UI components
 * - Requires the target locale resources to exist
 *   (falls back to default resources if unavailable)
 */
fun Context.getStringForLocale(
    @StringRes resId: Int,
    locale: Locale,
): String {
    val config = Configuration(resources.configuration)
    config.setLocale(locale)

    return createConfigurationContext(config)
        .resources
        .getString(resId)
}

/**
 * Opens play store app with the package name, returns true if able to open
 */
fun Context.openPlayStore(): Boolean =
    try {
        startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                data =
                    ("https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID).toUri()
                setPackage("com.android.vending")
            },
        )
        true
    } catch (_: Exception) {
        false
    }
