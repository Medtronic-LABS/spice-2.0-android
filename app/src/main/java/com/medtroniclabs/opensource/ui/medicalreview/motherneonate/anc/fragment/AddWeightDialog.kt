package com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.fragment

import android.content.res.Configuration
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.medtroniclabs.opensource.databinding.FragmentAddWeightDialogBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.network.utils.ConnectivityManager
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.DialogDismissListener
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.MotherNeonateUtil.isValidInput
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.viewmodel.AddWeightViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AddWeightDialog : DialogFragment(), View.OnClickListener {
    var listener: DialogDismissListener? = null
    private lateinit var binding: FragmentAddWeightDialogBinding
    private val viewModel: AddWeightViewModel by activityViewModels()

    @Inject
    lateinit var connectivityManager: ConnectivityManager

    companion object {
        const val TAG = "AddWeightDialog"
        fun newInstance(): AddWeightDialog {
            return AddWeightDialog()
        }

        fun newInstance(patientId: String?): AddWeightDialog {
            val fragment = AddWeightDialog()
            fragment.arguments = Bundle().apply {
                putString(DefinedParams.PatientId, patientId)
            }
            return fragment
        }
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
        viewModel.saveWeight.observe(viewLifecycleOwner) { resourcesState ->
            when (resourcesState.state) {
                ResourceState.LOADING -> {
                    binding.loader.visible()
                }

                ResourceState.SUCCESS -> {
                    binding.loader.gone()
                    dismiss()
                    listener?.onDialogDismissed(false)
                    viewModel.saveWeight.postError(optionalData = true)
                }

                ResourceState.ERROR -> {
                    binding.loader.gone()

                }
            }
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddWeightDialogBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        isCancelable = false
        return binding.root
    }

    private fun initView() {
        with(binding) {
            btnCancel.safeClickListener(this@AddWeightDialog)
            etWeight.addTextChangedListener(textWatcher)
            btnOkay.safeClickListener(this@AddWeightDialog)
            ivClose.safeClickListener(this@AddWeightDialog)
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

    override fun onStart() {
        super.onStart()
        handleDialogSize()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        handleDialogSize()
    }

    private fun isWeightValid(): Boolean {
        return isValidInput(
            binding.etWeight.text.toString(),
            binding.etWeight,
            binding.tvWeightErrorLabel,
            10.0..400.0,
            R.string.weight_error,
            false,
            requireContext()
        )
    }
    override fun onClick(v: View?) {
        when (v?.id) {
            binding.btnOkay.id -> handleOkayClick()
            binding.btnCancel.id, binding.ivClose.id -> dismiss()
        }
    }

    private fun handleOkayClick() {
        if (connectivityManager.isNetworkAvailable()) {
            if (isWeightValid()) {
                viewModel.saveWeight(createBpAndWeightRequestModel())
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
            weight = binding.etWeight.text?.trim().toString().toDoubleOrNull(),
            encounter = MedicalReviewEncounter(
                provenance = ProvanceDto(),
                latitude = viewModel.lastLocation?.latitude,
                longitude = viewModel.lastLocation?.longitude,
                patientId = arguments?.getString(DefinedParams.PatientId, ""),
                startTime = DateUtils.getCurrentDateAndTime(DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ),
                endTime = DateUtils.getCurrentDateAndTime(DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ)
            )
        )
    }

    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            // Not needed for your use case
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            // Not needed for your use case
        }

        override fun afterTextChanged(s: Editable?) {
            // Call the method to check if any EditText field is filled
            val hasString = (s?.trim()?.count() ?: 0) > 0
            binding.btnOkay.isEnabled = hasString
        }
    }
}
