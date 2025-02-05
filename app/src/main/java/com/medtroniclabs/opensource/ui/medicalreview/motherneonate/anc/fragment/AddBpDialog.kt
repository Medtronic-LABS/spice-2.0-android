package com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.fragment

import android.content.res.Configuration
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.postError
import com.medtroniclabs.opensource.appextensions.setWidth
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.SpiceLocationManager
import com.medtroniclabs.opensource.data.model.BpAndWeightRequestModel
import com.medtroniclabs.opensource.data.model.MedicalReviewEncounter
import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto
import com.medtroniclabs.opensource.databinding.FragmentAddBpDialogBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.network.utils.ConnectivityManager
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.DialogDismissListener
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.viewmodel.AddBpViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class AddBpDialog : DialogFragment(), View.OnClickListener {

    var listener: DialogDismissListener? = null
    private lateinit var binding: FragmentAddBpDialogBinding
    private val viewModel: AddBpViewModel by activityViewModels()

    @Inject
    lateinit var connectivityManager: ConnectivityManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddBpDialogBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        isCancelable = false
        return binding.root
    }

    companion object {
        const val TAG = "AddBpDialog"
        fun newInstance(): AddBpDialog {
            return AddBpDialog()
        }

        fun newInstance(patientId: String?): AddBpDialog {
            val fragment = AddBpDialog()
            fragment.arguments = Bundle().apply {
                putString(DefinedParams.PatientId, patientId)
            }
            return fragment
        }
    }

    private fun handleDialogSize() {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val width = if (CommonUtils.checkIsTablet(requireContext())) {
            if (isLandscape) 65 else 90
        } else {
            if (isLandscape) 65 else 90
        }
        setWidth(width)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        attachObservers()
        getCurrentLocation()
    }

    private fun getCurrentLocation() {
        val locationManager = SpiceLocationManager(requireContext())
        locationManager.getCurrentLocation {
            viewModel.lastLocation = it
        }
    }
    private fun attachObservers() {
        viewModel.saveBloodPressure.observe(viewLifecycleOwner) { resourcesState ->
            when (resourcesState.state) {
                ResourceState.LOADING -> {
                    binding.loader.visible()
                }

                ResourceState.SUCCESS -> {
                    binding.loader.gone()
                    dismiss()
                    listener?.onDialogDismissed(true)
                    viewModel.saveBloodPressure.postError(optionalData = true)
                }

                ResourceState.ERROR -> {
                    binding.loader.gone()

                }
            }
        }
    }


    private fun initView() {
        with(binding) {
            btnOkay.safeClickListener(this@AddBpDialog)
            btnCancel.safeClickListener(this@AddBpDialog)
            ivClose.safeClickListener(this@AddBpDialog)
            etPulse.addTextChangedListener(textWatcher)
            etSystolic.addTextChangedListener(textWatcher)
            etDiastolic.addTextChangedListener(textWatcher)
        }
    }

    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            // Not needed for your use case
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            // Not needed for your use case
        }

        override fun afterTextChanged(s: Editable?) {
            val pulseFilled = binding.etPulse.text?.isNotBlank() == true
            val systolicFilled = binding.etSystolic.text?.isNotBlank() == true
            val diastolicFilled = binding.etDiastolic.text?.isNotBlank() == true

            // Check if only one of the EditText fields is filled
            val onlyOneFilled = pulseFilled || systolicFilled || diastolicFilled

            // Enable the button if only one EditText field is filled
            binding.btnOkay.isEnabled = onlyOneFilled
        }

    }

    override fun onStart() {
        super.onStart()
        handleDialogSize()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        handleDialogSize()
    }

    private fun inputValidate(): Boolean {
        val isSystolicValid = isValidMeasurement(
            binding.etSystolic.text.toString(),
            binding.tvSystolicError,
            30,
            300,
            binding.etDiastolic,
            minErrorMessage = getString(R.string.systolic_error_min),
            maxErrorMessage = getString(R.string.systolic_error_max),
        )
        val isDiastolicValid = isValidMeasurement(
            binding.etDiastolic.text.toString(),
            binding.tvDiastolicError,
            30,
            300,
            binding.etDiastolic,
            getString(R.string.diastolic_error_min),
            maxErrorMessage = getString(R.string.diastolic_error_max)
        )
        val isPulseValid = if ((binding.etPulse.text?.trim()
                ?.isNotEmpty() == true)) {
            isValidMeasurement(
                binding.etPulse.text.toString(),
                binding.tvPulseError,
                35,
                300,
                minErrorMessage = getString(R.string.pulse_error_min),
                maxErrorMessage = getString(R.string.pulse_error_max),
                isPulseRequired = (isDiastolicValid == isSystolicValid)
            )
        } else {
            true
        }
        return isSystolicValid && isDiastolicValid && isPulseValid
    }

    private fun isValidMeasurement(
        valueText: String?,
        errorTextView: TextView,
        minValue: Int,
        maxValue: Int,
        text: AppCompatEditText? = null,
        minErrorMessage: String,
        maxErrorMessage: String,
        isPulseRequired: Boolean = false
    ): Boolean {
        val value = valueText?.toIntOrNull()
        val diastolic = text?.text.toString().toIntOrNull()
        if (value == null) {
            // Invalid input, display error message
            if (isPulseRequired) {
                return true
            }
            errorTextView.text = getText(R.string.error_label)
            errorTextView.visible()
            return false
        }
        if (value < minValue) {
            // Value is less than minimum allowed value, display error message
            errorTextView.text = minErrorMessage
            errorTextView.visible()
            return false
        }
        if (value > maxValue) {
            errorTextView.text = maxErrorMessage
            errorTextView.visible()
            return false
        }

        if (diastolic != null && value < diastolic) {
            errorTextView.text = getText(R.string.systolic_diastolic_error)
            errorTextView.visible()
            return false
        }
        // Valid input
        errorTextView.gone()
        return true
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.btnOkay.id -> handleOkayClick()
            binding.btnCancel.id, binding.ivClose.id -> dismiss()
        }
    }

    private fun handleOkayClick() {
        if (connectivityManager.isNetworkAvailable()) {
            if (inputValidate()) {
                viewModel.saveBloodPressure(createBpAndWeightRequestModel())
            }
        } else {
            (activity as BaseActivity?)?.showErrorDialogue(
                getString(R.string.error),
                getString(R.string.no_internet_error),
                isNegativeButtonNeed = false
            ) {

            }
        }
    }

    private fun createBpAndWeightRequestModel(): BpAndWeightRequestModel {
        return BpAndWeightRequestModel(
            systolic = binding.etSystolic.text?.trim().toString().toDoubleOrNull(),
            diastolic = binding.etDiastolic.text?.trim().toString().toDoubleOrNull(),
            pulse = binding.etPulse.text?.trim().toString().toDoubleOrNull(),
            encounter = createMedicalReviewEncounter()
        )
    }

    private fun createMedicalReviewEncounter(): MedicalReviewEncounter {
        return MedicalReviewEncounter(
            provenance = ProvanceDto(),
            latitude = viewModel.lastLocation?.latitude,
            longitude = viewModel.lastLocation?.longitude,
            patientId = arguments?.getString(DefinedParams.PatientId, ""),
            startTime = DateUtils.getCurrentDateAndTime(DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ),
            endTime = DateUtils.getCurrentDateAndTime(DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ)
        )
    }
}
