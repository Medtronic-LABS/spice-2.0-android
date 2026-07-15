package org.medtroniclabs.uhis.ui.patient.fragment

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DateUtils.DATE_FORMAT_ddMMMyyyy
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.medicalreview.ReqBPBGLogList
import org.medtroniclabs.uhis.data.registration.BPResponse
import org.medtroniclabs.uhis.data.registration.BpLog
import org.medtroniclabs.uhis.data.registration.BpLogDetails
import org.medtroniclabs.uhis.databinding.FragmentBloodPressureBinding
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams
import org.medtroniclabs.uhis.formgeneration.extension.capitalizeFirstChar
import org.medtroniclabs.uhis.formgeneration.extension.markMandatory
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.MenuConstants
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.UpperLimitDiastolic
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.UpperLimitSystolic
import org.medtroniclabs.uhis.ui.common.GeneralInfoDialog
import org.medtroniclabs.uhis.ui.patient.viewmodel.MedicalReviewBaseViewModel
import org.medtroniclabs.uhis.ui.patient.viewmodel.NurseMedicalReviewViewModel
import org.medtroniclabs.uhis.ui.patient.viewmodel.PatientDetailViewModel

class BloodPressureFragment : BaseFragment(), View.OnClickListener {
    private lateinit var binding: FragmentBloodPressureBinding
    private val viewModel: PatientDetailViewModel by activityViewModels()
    private val medicalReviewBaseViewModel: MedicalReviewBaseViewModel by activityViewModels()
    private val nurseMedicalReviewViewModel: NurseMedicalReviewViewModel by activityViewModels()

    companion object {
        const val TAG = "BloodPressureFragment"

        fun newInstance(): BloodPressureFragment = BloodPressureFragment()

        private const val MIN_VALUE = 30
        private const val MAX_VALUE = 300
        private const val VISIBLE_ITEM_COUNT = 5
    }

    private var bpReadings: List<Triple<String, String, Boolean>> = listOf()
    private var isExpanded = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentBloodPressureBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        attachObserver()
    }

    private fun initView() {
        medicalReviewBaseViewModel.fetchWorkFlow(MenuConstants.NCD_MENU_ID)
        binding.instructionsLayout.safeClickListener(this)
        binding.tvViewLess.safeClickListener(this)
        binding.tvViewMore.safeClickListener(this)
        binding.ivDownArrow.safeClickListener(this)
        binding.tvSystolic.markMandatory()
        binding.tvDiastolic.markMandatory()
        binding.tvViewMore.safeClickListener {
            if (!isExpanded) {
                renderItems(bpReadings)
                toggleView(true)
            }
        }

        binding.tvViewLess.safeClickListener {
            if (isExpanded) {
                renderItems(bpReadings.take(VISIBLE_ITEM_COUNT))
                toggleView(false)
            }
        }
        nurseMedicalReviewViewModel.nurseMrRequestModel.bpLog = nurseMedicalReviewViewModel.nurseMrRequestModel.bpLog
            ?: BpLog() // Initialize bpLog if it's null

        nurseMedicalReviewViewModel.nurseMrRequestModel.bpLog?.bpLogDetails =
            nurseMedicalReviewViewModel.nurseMrRequestModel.bpLog?.bpLogDetails
                ?: arrayListOf() // Initialize bpLogDetails as an empty list if it's null

        binding.etSystolic.addTextChangedListener {
            validateSystolicOnTextChange()
        }
        binding.etDiastolicOne.addTextChangedListener {
            validateDiastolicOnTextChange()
            validateSystolicOnTextChange()
        }
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.instructionsLayout -> {
                GeneralInfoDialog
                    .newInstance(
                        getString(R.string.blood_pressure),
                        getString(R.string.while_messauring),
                        medicalReviewBaseViewModel.instructionsList,
                    ).show(childFragmentManager, GeneralInfoDialog.TAG)
            }
        }
    }

    private fun validateSystolicOnTextChange() {
        val systolic = binding.etSystolic.text
            .toString()
            .takeIf { it.isNotBlank() }
            ?.toDoubleOrNull()
        val diastolic = binding.etDiastolicOne.text
            .toString()
            .takeIf { it.isNotBlank() }
            ?.toDoubleOrNull()
        when {
            systolic == null -> {
                binding.tvSystolicErrorMessage.visible()
                binding.tvSystolicErrorMessage.setText(R.string.default_user_input_error)
            }

            systolic <= MIN_VALUE -> {
                binding.tvSystolicErrorMessage.visible()
                binding.tvSystolicErrorMessage.setText(R.string.systolic_error_min)
            }

            systolic >= MAX_VALUE -> {
                binding.tvSystolicErrorMessage.visible()
                binding.tvSystolicErrorMessage.setText(R.string.systolic_error_max)
            }

            diastolic != null && systolic < diastolic -> {
                binding.tvSystolicErrorMessage.visible()
                binding.tvSystolicErrorMessage.setText(R.string.systolic_greater_than_diastolic)
            }

            else -> {
                binding.tvSystolicErrorMessage.gone()
            }
        }
    }

    private fun validateDiastolicOnTextChange() {
        val systolic = binding.etSystolic.text
            .toString()
            .takeIf { it.isNotBlank() }
            ?.toDoubleOrNull()
        val diastolic = binding.etDiastolicOne.text
            .toString()
            .takeIf { it.isNotBlank() }
            ?.toDoubleOrNull()
        when {
            diastolic == null -> {
                binding.tvDiastolicErrorMessage.visible()
                binding.tvDiastolicErrorMessage.setText(R.string.default_user_input_error)
            }

            diastolic <= MIN_VALUE -> {
                binding.tvDiastolicErrorMessage.visible()
                binding.tvDiastolicErrorMessage.setText(R.string.diastolic_error_min)
            }

            diastolic >= MAX_VALUE -> {
                binding.tvDiastolicErrorMessage.visible()
                binding.tvDiastolicErrorMessage.setText(R.string.diastolic_error_max)
            }

            systolic != null && systolic < diastolic -> {
                binding.tvDiastolicErrorMessage.gone()
                binding.tvSystolicErrorMessage.visible()
                binding.tvSystolicErrorMessage.setText(R.string.systolic_greater_than_diastolic)
            }

            else -> {
                binding.tvDiastolicErrorMessage.gone()
            }
        }
    }

    private fun attachObserver() {
        medicalReviewBaseViewModel.formResponseLiveData.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    resourceState.data
                        ?.formLayout
                        ?.find { it.id == DefinedParams.BP_LOG_DETAILS }
                        ?.let { instruction ->
                            val instructions = if (SecuredPreference.getIsTranslationEnabled()) {
                                instruction.instructionsCulture
                            } else {
                                instruction.instructions
                            }
                            instructions?.let { medicalReviewBaseViewModel.instructionsList.addAll(it) }
                        }
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }
            }
        }

        nurseMedicalReviewViewModel.patientDetailsResponse.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.SUCCESS -> {
                    resourceState.data?.memberId?.let { id ->
                        val request = ReqBPBGLogList(memberId = id)
                        viewModel.getPatientBPLogList(requireContext(), request)
                    }
                }

                else -> {
                    // Invoked if response state is not success
                }
            }
        }

        viewModel.patientBPLogListResponse.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    resourceState.data?.let { bpLogResponse ->
                        bpLogResponse.bpLogList?.let {
                            renderBPResponse(it)
                        }
                    }
                }
            }
        }
    }

    private fun renderBPResponse(bpResponses: List<BPResponse>) {
        if (bpResponses.isNotEmpty()) {
            binding.clBPReadingHistory.visibility = View.VISIBLE
            binding.gViewMoreLess.visibility = View.VISIBLE
            this.bpReadings = processBPResponse(bpResponses)
            renderItems(bpReadings.take(VISIBLE_ITEM_COUNT))
        } else {
            binding.clBPReadingHistory.visibility = View.GONE
        }
    }

    private fun processBPResponse(bpResponses: List<BPResponse>): List<Triple<String, String, Boolean>> =
        bpResponses
            .sortedByDescending {
                DateUtils.getLastMenstrualDate(it.bpTakenOn ?: "").timeInMillis
            }.map { response ->
                val formattedDate = response.bpTakenOn
                val bpReading = "${response.avgSystolic.toInt()}/${response.avgDiastolic.toInt()}"
                val isHighBP =
                    response.avgSystolic > UpperLimitSystolic || response.avgDiastolic > UpperLimitDiastolic
                Triple(bpReading, formattedDate, isHighBP)
            }

    private fun renderItems(items: List<Triple<String, String, Boolean>>) {
        binding.llBpReading.removeAllViews()
        for (item in items) {
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(10, 10, 30, 10)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                )
                setVerticalGravity(Gravity.CENTER_VERTICAL)
            }

            val imageView = ImageView(requireContext()).apply {
                val imageRes =
                    if (item.third) R.drawable.ic_bp_reading_orange else R.drawable.ic_bp_reading_green
                setImageResource(imageRes)
                layoutParams = LinearLayout.LayoutParams(40, 40).apply {
                    marginEnd = 20
                }
            }

            val textViewBP = TextView(requireContext()).apply {
                text = item.first
                textSize = 16f
                setTextColor(requireContext().getColor(R.color.secondary_black))
                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f,
                    )
            }

            val separatorTextView = TextView(requireContext()).apply {
                text = " - "
                textSize = 16f
                setTextColor(requireContext().getColor(R.color.secondary_black))
                layoutParams =
                    LinearLayout.LayoutParams(50, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        marginStart = 20
                        marginEnd = 20
                    }
            }

            val textViewDate = TextView(requireContext()).apply {
                val visitDateMillis = DateUtils.getLastMenstrualDate(item.second ?: "").timeInMillis
                val displayDate = DateUtils.formatDateToDisplayFormat(visitDateMillis, DATE_FORMAT_ddMMMyyyy) ?: ""
                text = displayDate
                textSize = 16f
                setTextColor(requireContext().getColor(R.color.secondary_black))
                layoutParams =
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            row.addView(imageView)
            row.addView(textViewBP)
            row.addView(separatorTextView)
            row.addView(textViewDate)
            binding.llBpReading.addView(row)
        }
    }

    private fun toggleView(expand: Boolean) {
        isExpanded = expand
        binding.tvViewMore.visibility = if (expand) View.GONE else View.VISIBLE
        binding.ivDownArrow.visibility = if (expand) View.GONE else View.VISIBLE
        binding.tvViewLess.visibility = if (expand) View.VISIBLE else View.GONE
        binding.ivUpArrow.visibility = if (expand) View.VISIBLE else View.GONE
    }

    fun validation(): Boolean {
        var isValid = true

        val systolicString = binding.etSystolic.text
            .toString()
            .takeIf { it.isNotBlank() }
        val diastolicString = binding.etDiastolicOne.text
            .toString()
            .takeIf { it.isNotBlank() }
        val systolic = systolicString?.toDoubleOrNull()
        val diastolic = diastolicString?.toDoubleOrNull()

        nurseMedicalReviewViewModel.nurseMrRequestModel.bpLog
            ?.bpLogDetails
            ?.clear()
        nurseMedicalReviewViewModel.nurseMrRequestModel.bpLog?.apply {
            avgSystolic = binding.etSystolic.text
                .toString()
                .trim()
                .takeIf { it.isNotBlank() }
                ?.toDoubleOrNull()
            avgDiastolic = binding.etDiastolicOne.text
                .toString()
                .trim()
                .takeIf { it.isNotBlank() }
                ?.toDoubleOrNull()
            bpTakenOn = DateUtils.getCurrentDateTime(DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ)

            type = DefinedParams.MEDICAL_REVIEW
            avgBloodPressure = "$systolic/$diastolic"
            unitMeasurement = DefinedParams.UNIT_MEASUREMENT_METRIC_TYPE.capitalizeFirstChar()
            bpLogDetails?.add(BpLogDetails(diastolic = diastolic, systolic = systolic))
        }

        nurseMedicalReviewViewModel.systolic = systolicString
        nurseMedicalReviewViewModel.diastolic = diastolicString

        if (nurseMedicalReviewViewModel.systolic.isNullOrEmpty()) {
            isValid = false
            binding.tvSystolicErrorMessage.visibility = View.VISIBLE
        } else if (systolic != null && diastolic != null) {
            if (systolic < diastolic) {
                isValid = false
                binding.tvSystolicErrorMessage.visibility = View.VISIBLE
                binding.tvSystolicErrorMessage.text = getString(R.string.systolic_greater_than_diastolic)
            } else if (systolic >= MAX_VALUE && diastolic >= MAX_VALUE) {
                isValid = false
                binding.tvSystolicErrorMessage.visibility = View.VISIBLE
                binding.tvSystolicErrorMessage.text = getString(R.string.systolic_diastolic_max_validation, "300")
            } else if (systolic <= MIN_VALUE && diastolic <= MIN_VALUE) {
                isValid = false
                binding.tvSystolicErrorMessage.visibility = View.VISIBLE
                binding.tvSystolicErrorMessage.text = getString(R.string.systolic_diastolic_min_validation, "30")
            } else {
                binding.tvSystolicErrorMessage.visibility = View.GONE
            }
        }
        if (nurseMedicalReviewViewModel.diastolic.isNullOrEmpty()) {
            isValid = false
            binding.tvDiastolicErrorMessage.visibility = View.VISIBLE
        } else {
            binding.tvDiastolicErrorMessage.visibility = View.GONE
        }
        return isValid
    }
}
