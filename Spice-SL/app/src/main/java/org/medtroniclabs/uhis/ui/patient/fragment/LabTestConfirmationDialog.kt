package org.medtroniclabs.uhis.ui.patient.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.registration.LabTestModel
import org.medtroniclabs.uhis.databinding.FragmentLabTestConfirmationDialogBinding
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.network.utils.ConnectivityManager
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.patient.viewmodel.LabTestViewModel
import javax.inject.Inject
import kotlin.getValue

@AndroidEntryPoint
class LabTestConfirmationDialog(private val model: LabTestModel) : DialogFragment(), View.OnClickListener {
    companion object {
        const val TAG = "LabTestConfirmationDialog"
        const val KEY_MODEL = "KEY_MODEL"

        @JvmStatic
        fun newInstance(model: LabTestModel): LabTestConfirmationDialog = LabTestConfirmationDialog(model)
    }

    private lateinit var binding: FragmentLabTestConfirmationDialogBinding
    private val viewModel: LabTestViewModel by activityViewModels()

    @Inject
    lateinit var connectivityManager: ConnectivityManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentLabTestConfirmationDialogBinding.inflate(inflater, container, false)
        val window: Window? = dialog?.window
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
        )
        dialog?.window?.attributes?.windowAnimations = R.style.dialogEnterExitAnimation
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        setListeners()
    }

    private fun setListeners() {
        binding.labelHeader.ivClose.safeClickListener(this)
        binding.btnCancel.safeClickListener(this)
        binding.btnConfirm.safeClickListener(this)
    }

    override fun onClick(mView: View?) {
        when (mView?.id) {
            binding.labelHeader.ivClose.id -> dismiss()

            binding.btnCancel.id -> dismiss()

            binding.btnConfirm.id -> {
                if (connectivityManager.isNetworkAvailable()) {
                    val request = HashMap<String, Any>()
                    request[DefinedParams.COMMENT] = model.resultComments ?: ""
                    request[DefinedParams.TENANT_ID] = SecuredPreference.getTenantId()
                    request[DefinedParams.PATIENT_TRACK_ID] = viewModel.patientTrackId
                    request[DefinedParams.ID] = model._id ?: -1
                    viewModel.reviewLabTestResult(requireContext(), request)
                    dismiss()
                } else {
                    (activity as BaseActivity).showErrorDialogue(getString(R.string.error), getString(R.string.no_internet_error), false) {}
                }
            }
        }
    }
}
