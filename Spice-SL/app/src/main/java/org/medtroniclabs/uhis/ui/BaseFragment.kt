package org.medtroniclabs.uhis.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.isFineAndCoarseLocationPermissionGranted
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.GeneralErrorDialog
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.ncd.data.PatientFollowUpEntity
import org.medtroniclabs.uhis.ncd.followup.fragment.NCDFollowUpDialogFragment
import org.medtroniclabs.uhis.ncd.medicalreview.NCDMRUtil
import org.medtroniclabs.uhis.network.utils.ConnectivityManager
import org.medtroniclabs.uhis.ui.home.AssessmentToolsActivity
import org.medtroniclabs.uhis.ui.mypatients.PatientSelectionListenerForFollowUp
import javax.inject.Inject

@AndroidEntryPoint
open class BaseFragment : Fragment() {
    @Inject
    lateinit var connectivityManager: ConnectivityManager

    /**
     * Boolean variable holding whether translation is enabled or not for the app.
     * If it is true that means user is using the app in second language (other than english)
     */
    protected var isTranslationEnabled: Boolean = false

    open fun showLoading() {
        (requireActivity() as BaseActivity).showLoading()
    }

    open fun hideLoading() {
        (activity as? BaseActivity)?.hideLoading()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isTranslationEnabled = SecuredPreference.getIsTranslationEnabled()
    }

    fun showProgress() {
        if (activity is BaseActivity) {
            (activity as BaseActivity?)?.showLoading()
        }
    }

    fun hideProgress() {
        if (activity is BaseActivity) {
            (activity as BaseActivity?)?.hideLoading()
        }
    }

    inline fun <reified fragment : Fragment> replaceFragmentInId(
        id: Int,
        bundle: Bundle? = null,
        tag: String? = null,
    ) {
        childFragmentManager.commit {
            setReorderingAllowed(true)
            replace<fragment>(
                id,
                args = bundle,
                tag = tag,
            )
        }
    }

    fun setTitle(title: String) {
        (activity as? BaseActivity)?.setTitle(title)
    }

    fun getTitle(): String? = (activity as? BaseActivity)?.getString()

    fun showSuccessDialogue(
        title: String,
        message: String,
    ) {
        (requireActivity() as BaseActivity).showSuccessDialogue(
            title = title,
            message = message,
        ) {}
    }

    fun showErrorDialog(
        title: String,
        message: String,
    ) {
        (requireActivity() as BaseActivity).showErrorDialogue(
            title,
            message,
            isNegativeButtonNeed = false,
        ) {}
    }

    fun withNetworkAvailability(
        online: () -> Unit,
        offline: () -> Unit = {},
        requireErrorDialog: Boolean = true,
    ) {
        connectivityManager.isNullableNetworkAvailable()?.let { isNetworkAvailable ->
            if (isNetworkAvailable) {
                online()
            } else {
                if (requireErrorDialog) {
                    showErrorDialog(getString(R.string.error), getString(R.string.no_internet_error))
                }
                offline()
            }
        }
    }

    inline fun <reified F : Fragment> replaceFragmentIfExists(
        id: Int,
        bundle: Bundle? = null,
        tag: String? = null,
    ) {
        val existingFragment = parentFragmentManager.findFragmentByTag(tag)

        parentFragmentManager.commit {
            setReorderingAllowed(true)
            if (existingFragment != null) {
                // Fragment exists, replace it
                replace(id, existingFragment, tag)
            } else {
                // Fragment does not exist, create a new instance and replace it
                replace<F>(id, args = bundle, tag = tag)
            }
        }
    }

    fun showCallDialError(isActivityFinish: Boolean = true) {
        (activity as BaseActivity).showErrorDialogue(
            message = getString(R.string.device_phone_info),
        ) {
            if (isActivityFinish) {
                requireActivity().finish()
            }
        }
    }

    fun hasTelephonyFeature(context: Context): Boolean {
        val packageManager = context.packageManager
        return packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
    }

    fun launchAssessment(
        item: PatientFollowUpEntity,
        context: Context,
    ) {
        val intent = Intent(requireContext(), AssessmentToolsActivity::class.java)
        intent.putExtra(DefinedParams.FhirId, item.memberId)
        intent.putExtra(DefinedParams.PatientId, item.patientId)
        intent.putExtra(DefinedParams.ORIGIN, MenuConstants.ASSESSMENT.lowercase())
        intent.putExtra(DefinedParams.GENDER, item.gender)
        startActivity(intent)
    }

    fun launchPatientDetailsDialog(listener: PatientSelectionListenerForFollowUp) {
        val fragment = childFragmentManager.findFragmentByTag(NCDFollowUpDialogFragment.TAG)
        if (fragment == null) {
            NCDFollowUpDialogFragment
                .newInstance(listener)
                .show(childFragmentManager, NCDFollowUpDialogFragment.TAG)
        }
    }

    fun handleChipType(
        type: String?,
        isFemalePregnant: Boolean,
    ): String? =
        if (type.equals(
                DefinedParams.PregnancyANC,
                true,
            ) &&
            !isFemalePregnant
        ) {
            NCDMRUtil.NCD.lowercase()
        } else {
            type
        }

    fun withLocationCheck(
        onLocationAvailable: () -> Unit,
//        onLocationNotAvailable: (() -> Unit)? = null,
        shouldHideProgress: Boolean = false,
    ) {
        when {
//            !requireContext().isGpsEnabled() -> {
//                showTurnOnGPSDialog()
//                onLocationNotAvailable?.invoke()
//            }
            !requireContext().isFineAndCoarseLocationPermissionGranted() -> {
                requestLocationPermissions { permissionsGranted ->
                    if (permissionsGranted) {
                        onLocationAvailable()
//                        showProgress()
//                        val locationManager = SpiceLocationManager(requireContext())
//                        locationManager.getCurrentLocation {
//                            onLocationAvailable()
//                            if (shouldHideProgress) {
//                                hideProgress()
//                            }
//                        }
                    } else {
                        onLocationAvailable()
//                        showErrorDialogue(
//                            title = getString(R.string.gps_disabled_title),
//                            message = getString(R.string.gps_disabled_message),
//                            positiveButtonName = getString(R.string.ok),
//                        ) {
//                            if (it) {
//                                val intent = Intent()
//                                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
//                                val uri = Uri.fromParts(
//                                    "package",
//                                    BuildConfig.APPLICATION_ID,
//                                    null,
//                                )
//                                intent.data = uri
//                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//                                startActivity(intent)
//                                onLocationNotAvailable?.invoke()
//                            }
//                        }
                    }
                }
            }
            else -> onLocationAvailable()
        }
    }

    private var locationPermissionResultCallback: ((Boolean) -> Unit)? = null

    private fun requestLocationPermissions(onResult: (Boolean) -> Unit) {
        locationPermissionResultCallback = onResult
        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
        )
    }

    // Permission launcher
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val finePermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarsePermission = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

            locationPermissionResultCallback?.invoke(finePermission && coarsePermission)
        }

    fun showTurnOnGPSDialog() {
        showErrorDialogue(
            title = getString(R.string.gps_disabled_title),
            message = getString(R.string.gps_disabled_message),
            positiveButtonName = getString(R.string.ok),
        ) {
            if (it) {
                val settingsIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(settingsIntent)
            }
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
                requireContext(),
                isNegativeButtonNeed ?: false,
                okayButton = positiveButtonName ?: getString(R.string.ok),
                cancelButton = cancelBtnName ?: getString(R.string.cancel),
                messageBtnData = Pair(message, okayBtnEnable ?: true),
            )
        val errorFragment = childFragmentManager.findFragmentByTag(GeneralErrorDialog.TAG)
        if (errorFragment == null) {
            generalErrorDialog.show(childFragmentManager, GeneralErrorDialog.TAG)
        }
    }

    fun showHomeIcon() {
        (activity as? BaseActivity)?.hideHomeButton(false)
    }

    fun hideHomeIcon() {
        (activity as? BaseActivity)?.hideHomeButton(true)
    }

    fun hideKeyboard(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    inline fun <reified T : DialogFragment> Fragment.showDialogIfNotPresent(
        tag: String,
        dialogProvider: () -> T,
    ) {
        val existingFragment = childFragmentManager.findFragmentByTag(tag)
        if (existingFragment == null) {
            dialogProvider().show(childFragmentManager, tag)
        }
    }
}
