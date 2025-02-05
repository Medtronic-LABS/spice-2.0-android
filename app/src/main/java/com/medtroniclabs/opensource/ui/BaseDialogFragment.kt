package com.medtroniclabs.opensource.ui

import androidx.fragment.app.DialogFragment
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.network.utils.ConnectivityManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
open class BaseDialogFragment : DialogFragment() {

    @Inject
    lateinit var connectivityManager: ConnectivityManager

    fun withNetworkAvailability(
        online: () -> Unit,
        offline: () -> Unit = {}
    ) {
        connectivityManager.isNullableNetworkAvailable()?.let { isNetworkAvailable ->
            if (isNetworkAvailable) {
                online()
            } else {
                (requireActivity() as BaseActivity).showErrorDialogue(
                    getString(R.string.error),
                    getString(R.string.no_internet_error),
                    isNegativeButtonNeed = false
                ) {
                    if (it) {
                        offline()
                    }
                    (requireActivity() as BaseActivity).hideLoading()
                }
            }
        }
    }

    fun finishFragment() {
        activity?.supportFragmentManager?.beginTransaction()?.remove(this)?.commit()
    }
}