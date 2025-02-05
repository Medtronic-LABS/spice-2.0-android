package com.medtroniclabs.opensource.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.cancelAllWorker
import com.medtroniclabs.opensource.common.AppConstants
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.GeneralErrorDialog
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.network.utils.ConnectivityManager
import com.medtroniclabs.opensource.ui.boarding.LoginActivity
import com.medtroniclabs.opensource.ui.dialog.GeneralSuccessDialog
import javax.inject.Inject
import kotlin.system.exitProcess

open class SpiceRootActivity : AppCompatActivity() {

    private lateinit var sessionExpiredBroadcastReceiver: SessionExpiredBroadcastReceiver

    @Inject
    lateinit var connectivityManager: ConnectivityManager

    private lateinit var appUpdateManager: AppUpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionExpiredBroadcastReceiver = SessionExpiredBroadcastReceiver()
        checkInAppUpdate()
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            sessionExpiredBroadcastReceiver,
            IntentFilter(
                DefinedParams.ACTION_SESSION_EXPIRED
            )
        )
        if (this::appUpdateManager.isInitialized) {
            appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        activityResultLauncher,
                        AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                    )
                }
            }
        }
    }

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result: androidx.activity.result.ActivityResult ->
            if ((result.resultCode == ActivityResult.RESULT_IN_APP_UPDATE_FAILED || result.resultCode == RESULT_CANCELED) && result.resultCode != RESULT_OK) {
                finishAffinity()
                exitProcess(0)
            }
        }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(
            sessionExpiredBroadcastReceiver
        )
    }

    inner class SessionExpiredBroadcastReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val sessionExpired = intent.getBooleanExtra(DefinedParams.SL_SESSION, false)
            val className = this.javaClass.simpleName
            if (sessionExpired && !AppConstants.exemptionList.contains(className)) {
                showErrorDialogue(
                    getString(R.string.alert),
                    getString(R.string.session_expired),
                    isNegativeButtonNeed = false
                ) { status ->
                    if (status) {
                        cancelAllWorker()
                        SecuredPreference.clear(this@SpiceRootActivity)
                        val i = Intent(context, LoginActivity::class.java)
                        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        context.startActivity(i)
                    }
                }
            }
        }
    }

    fun showSuccessDialogue(
        title: String,
        message: String,
        positiveButtonName: String? = null,
        callback: () -> Unit
    ) {
        val generalSuccessDialog =
            GeneralSuccessDialog.newInstance(
                title,
                message = message,
                okayButton = positiveButtonName ?: getString(R.string.done),
                callback
            )
        val successFragment = supportFragmentManager.findFragmentByTag(GeneralSuccessDialog.TAG)
        if (successFragment == null)
            generalSuccessDialog.show(supportFragmentManager, GeneralSuccessDialog.TAG)
    }

    fun showErrorDialogue(
        title: String = getString(R.string.error),
        message: String,
        isNegativeButtonNeed: Boolean? = null,
        positiveButtonName: String? = null,
        okayBtnEnable: Boolean? = null,
        cancelBtnName: String? = null,
        callback: ((isPositiveResult: Boolean) -> Unit)
    ) {
        val generalErrorDialog =
            GeneralErrorDialog.newInstance(
                title,
                callback,
                this,
                isNegativeButtonNeed ?: false,
                okayButton = positiveButtonName ?: getString(R.string.ok),
                cancelButton = cancelBtnName ?: getString(R.string.cancel),
                messageBtnData = Pair(message, okayBtnEnable ?: true)
            )
        val errorFragment = supportFragmentManager.findFragmentByTag(GeneralErrorDialog.TAG)
        if (errorFragment == null)
            generalErrorDialog.show(supportFragmentManager, GeneralErrorDialog.TAG)
    }

    private fun checkInAppUpdate() {
        if (connectivityManager.isNetworkAvailable()) {
            appUpdateManager = AppUpdateManagerFactory.create(this)

            val appUpdateInfoTask = appUpdateManager.appUpdateInfo

            appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(
                        AppUpdateType.IMMEDIATE
                    )
                ) {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        activityResultLauncher,
                        AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                    )
                }
            }
        }
    }

}