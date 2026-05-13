package org.medtroniclabs.uhis.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.model.UserDetail
import org.medtroniclabs.uhis.appextensions.cancelAllWorker
import org.medtroniclabs.uhis.common.AppConstants
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.GeneralErrorDialog
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.network.utils.ConnectivityManager
import org.medtroniclabs.uhis.ui.boarding.LoginActivity
import org.medtroniclabs.uhis.ui.dialog.GeneralSuccessDialog
import java.util.UUID
import javax.inject.Inject

open class SpiceRootActivity : AppCompatActivity() {
    private lateinit var sessionExpiredBroadcastReceiver: SessionExpiredBroadcastReceiver

    @Inject
    lateinit var connectivityManager: ConnectivityManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionExpiredBroadcastReceiver = SessionExpiredBroadcastReceiver()
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            sessionExpiredBroadcastReceiver,
            IntentFilter(
                DefinedParams.ACTION_SESSION_EXPIRED,
            ),
        )
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(
            sessionExpiredBroadcastReceiver,
        )
    }

    /**
     * Receiver for session expired broadcasts from [Retrofit API].
     */
    inner class SessionExpiredBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(
            context: Context,
            intent: Intent,
        ) {
            val sessionExpired = intent.getBooleanExtra(DefinedParams.SL_SESSION, false)
            val className = this.javaClass.simpleName
            if (sessionExpired && !AppConstants.exemptionList.contains(className)) {
                showErrorDialogue(
                    getString(R.string.alert),
                    getString(R.string.session_expired),
                    isNegativeButtonNeed = false,
                ) { status ->
                    if (status) {
                        cancelAllWorker()
                        SecuredPreference.clear(this@SpiceRootActivity)
                        val i = Intent(context, LoginActivity::class.java)
                        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        context.startActivity(i)
                        UserDetail.referenceId = UUID.randomUUID().toString()
                    }
                }
            }
        }
    }

    fun showSuccessDialogue(
        title: String,
        message: String,
        positiveButtonName: String? = null,
        callback: () -> Unit,
    ) {
        val generalSuccessDialog =
            GeneralSuccessDialog.newInstance(
                title,
                message = message,
                okayButton = positiveButtonName ?: getString(R.string.done),
                callback,
            )
        val successFragment = supportFragmentManager.findFragmentByTag(GeneralSuccessDialog.TAG)
        if (successFragment == null) {
            generalSuccessDialog.show(supportFragmentManager, GeneralSuccessDialog.TAG)
        }
    }

    fun showErrorDialogue(
        title: String = getString(R.string.error),
        message: String,
        isNegativeButtonNeed: Boolean? = null,
        positiveButtonName: String? = null,
        okayBtnEnable: Boolean? = null,
        cancelBtnName: String? = null,
        callback: ((isPositiveResult: Boolean) -> Unit),
    ) {
        val generalErrorDialog =
            GeneralErrorDialog.newInstance(
                title,
                callback,
                this,
                isNegativeButtonNeed ?: false,
                okayButton = positiveButtonName ?: getString(R.string.ok),
                cancelButton = cancelBtnName ?: getString(R.string.cancel),
                messageBtnData = Pair(message, okayBtnEnable ?: true),
            )
        val errorFragment = supportFragmentManager.findFragmentByTag(GeneralErrorDialog.TAG)
        if (errorFragment == null) {
            generalErrorDialog.show(supportFragmentManager, GeneralErrorDialog.TAG)
        }
    }
}
