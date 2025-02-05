package com.medtroniclabs.opensource.ncd.medicalreview.dialog

import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.InputFilter
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.color
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.invisible
import com.medtroniclabs.opensource.appextensions.loadAsGif
import com.medtroniclabs.opensource.appextensions.resetImageView
import com.medtroniclabs.opensource.appextensions.setDialogPercentForWidth
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.CommonUtils.toDoubleOrEmptyString
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.DefinedParams.Assessment
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.common.Validator
import com.medtroniclabs.opensource.common.ViewUtils
import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto
import com.medtroniclabs.opensource.databinding.FragmentNcdBloodPressureVitalsDialogBinding
import com.medtroniclabs.opensource.db.entity.RiskClassificationModel
import com.medtroniclabs.opensource.formgeneration.extension.dp
import com.medtroniclabs.opensource.formgeneration.extension.markMandatory
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.formgeneration.extension.textSizeSsp
import com.medtroniclabs.opensource.formgeneration.model.BPModel
import com.medtroniclabs.opensource.formgeneration.model.FormLayout
import com.medtroniclabs.opensource.formgeneration.model.FormResponse
import com.medtroniclabs.opensource.mappingkey.MemberRegistration
import com.medtroniclabs.opensource.mappingkey.Screening
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil
import com.medtroniclabs.opensource.ncd.medicalreview.viewmodel.NCDBloodPressureVitalsViewModel
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.network.utils.ConnectivityManager
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.PatientDetailViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class NCDBloodPressureVitalsDialog(private val callback: () -> Unit) : DialogFragment(), View.OnClickListener {

    private lateinit var binding: FragmentNcdBloodPressureVitalsDialogBinding
    private val viewModel: NCDBloodPressureVitalsViewModel by viewModels()
    private val patientViewModel: PatientDetailViewModel by activityViewModels()

    @Inject
    lateinit var connectivityManager: ConnectivityManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentNcdBloodPressureVitalsDialogBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return binding.root
    }

    companion object {
        const val TAG = "NCDBloodPressureVitalsDialog"
        fun newInstance(callback: () -> Unit) =
            NCDBloodPressureVitalsDialog(callback)
    }

    override fun onStart() {
        super.onStart()
        if (!CommonUtils.checkIsTablet(requireContext())) {
            setDialogPercentForWidth(50)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = false
        init()
        attachObserver()
        renderAssessmentForm()
        setClickListeners()
    }

    private fun setClickListeners() {
        binding.ivClose.safeClickListener(this)
        binding.etAssessmentDate.safeClickListener(this)
        binding.btnCancel.safeClickListener(this)
        binding.btnAddReading.safeClickListener(this)
    }

    fun attachObserver() {
        viewModel.getRiskEntityListLiveData.observe(this) {}
        viewModel.formLayoutsNcdLiveData.observe(viewLifecycleOwner) { data ->
            if (data != null) {
                val formFieldsType = object : TypeToken<FormResponse>() {}.type
                val formFields: FormResponse = Gson().fromJson(data, formFieldsType)
                viewModel.weight =
                    formFields.formLayout.find { it.id.lowercase() == Screening.Weight.lowercase() }
                viewModel.height =
                    formFields.formLayout.find { it.id.lowercase() == Screening.Height.lowercase() }
                viewModel.bpLog =
                    formFields.formLayout.find { it.id.lowercase() == Screening.BPLog_Details.lowercase() }
            }
        }
        viewModel.bpLogCreateResponseLiveData.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    dismiss()
                    callback.invoke()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                    resourceState?.message?.let { message ->
                        (activity as BaseActivity).showErrorDialogue(
                            getString(R.string.error),
                            message,
                            false
                        ) {}
                    }
                }
            }
        }
    }

    fun showLoading() {
        binding.apply {
            btnAddReading.invisible()
            btnCancel.invisible()
            loadingProgress.visible()
            loaderImage.apply {
                loadAsGif(R.drawable.loader_spice)
            }
        }
    }

    fun hideLoading() {
        binding.apply {
            btnAddReading.visible()
            btnCancel.visible()
            loadingProgress.gone()
            loaderImage.apply {
                resetImageView()
            }
        }
    }

    private fun init() {
        binding.btnAddReading.safeClickListener(this)
        viewModel.getNcdFormData(DefinedParams.Assessment, NCDMRUtil.NCD.lowercase())
        with(binding.bmiView) {
            llBase.setBackgroundColor(requireContext().getColor(R.color.Background_Color1))
            tvKey.text = getString(R.string.bmi)
            tvValue.text = getString(R.string.hyphen_symbol)
            val horizontalPadding = resources.getDimensionPixelSize(R.dimen._12sdp)
            tvKey.setPadding(
                horizontalPadding,
                tvKey.paddingTop,
                horizontalPadding,
                tvKey.paddingBottom
            )
            if (!CommonUtils.checkIsTablet(requireContext())) {
                val constraintSet = ConstraintSet()
                constraintSet.clone(llBase)
                constraintSet.setHorizontalWeight(tvKey.id, 0.3F)
                constraintSet.applyTo(llBase)
            }
        }

        binding.tvAssessmentDate.markMandatory()
        binding.tvHeight.markMandatory()
        binding.tvWeight.markMandatory()
        binding.tvSmokingLbl.markMandatory()
        binding.llBpReading.instructionsLayout.gone()
        binding.llBpReading.bpReadingThree.visible()
        binding.llBpReading.apply {
            setMaxLength(etSystolicOne)
            setMaxLength(etDiastolicOne)
            setMaxLength(etPulseOne)
            setMaxLength(etSystolicTwo)
            setMaxLength(etDiastolicTwo)
            setMaxLength(etPulseTwo)
            setMaxLength(etSystolicThree)
            setMaxLength(etDiastolicThree)
            setMaxLength(etPulseThree)
            instructionsLayout.gone()
            tvSnoReadingTwo.isEnabled = false
            etSystolicTwo.isEnabled = false
            etDiastolicTwo.isEnabled = false
            etPulseTwo.isEnabled = false
            separatorRowTwo.isEnabled = false
            tvSnoReadingThree.isEnabled = false
            etSystolicThree.isEnabled = false
            etDiastolicThree.isEnabled = false
            etPulseThree.isEnabled = false
            separatorRowThree.isEnabled = false
        }

        val list = Screening.getEmptyBPReading(3)
        viewModel.resultHashMap[Screening.BPLog_Details] = list
        binding.llBpReading.etSystolicOne.addTextChangedListener {
            checkInputsAndEnableNextField(binding.llBpReading.tvSnoReading.text, list)
        }
        binding.llBpReading.etDiastolicOne.addTextChangedListener {
            checkInputsAndEnableNextField(binding.llBpReading.tvSnoReading.text, list)
        }
        binding.llBpReading.etPulseOne.addTextChangedListener {
            checkInputsAndEnableNextField(binding.llBpReading.tvSnoReading.text, list)
        }

        binding.etWeight.addTextChangedListener {
            val weight = it?.trim().toString()
            if (weight.isEmpty()) {
                viewModel.resultHashMap.remove(Screening.Weight)
            } else {
                viewModel.resultHashMap[Screening.Weight] = weight.toDouble()
            }
            displayBMIValue()
        }

        binding.etHeight.addTextChangedListener {
            val height = it?.trim().toString()
            if (height.isEmpty()) {
                viewModel.resultHashMap.remove(Screening.Height)
            } else {
                viewModel.resultHashMap[Screening.Height] = height.toDouble()
            }
            displayBMIValue()
        }

        binding.llBpReading.etSystolicTwo.addTextChangedListener {
            checkInputsAndEnableNextField(binding.llBpReading.tvSnoReadingTwo.text, list)
        }
        binding.llBpReading.etDiastolicTwo.addTextChangedListener {
            checkInputsAndEnableNextField(binding.llBpReading.tvSnoReadingTwo.text, list)
        }
        binding.llBpReading.etPulseTwo.addTextChangedListener {
            checkInputsAndEnableNextField(binding.llBpReading.tvSnoReadingTwo.text, list)
        }
        binding.llBpReading.etSystolicThree.addTextChangedListener {
            checkInputsAndEnableNextField(binding.llBpReading.tvSnoReadingThree.text, list)
        }
        binding.llBpReading.etDiastolicThree.addTextChangedListener {
            checkInputsAndEnableNextField(binding.llBpReading.tvSnoReadingThree.text, list)
        }
        binding.llBpReading.etPulseThree.addTextChangedListener {
            checkInputsAndEnableNextField(binding.llBpReading.tvSnoReadingThree.text, list)
        }
        setupRadioButtons(binding.btnYes)
        setupRadioButtons(binding.btnNo)
        patientViewModel.patientDetailsLiveData.value?.data?.let { patientData ->
            patientData.weight?.takeIf { it > 0 }?.toDoubleOrEmptyString()?.let { binding.etWeight.setText(it) }
            patientData.height?.takeIf { it > 0 }?.toDoubleOrEmptyString()?.let { binding.etHeight.setText(it) }
            binding.smokingGrp.visibility =
                if (patientData.isRegularSmoker == null) View.VISIBLE else View.GONE
        }
    }

    private fun displayBMIValue() {
        with(binding.bmiView) {

            val bmi = CommonUtils.calculateBMI(
                viewModel.resultHashMap
            )
            if (bmi == null) {
                tvValue.text = getString(R.string.hyphen_symbol)
                viewModel.resultHashMap.remove(Screening.BMI)
                viewModel.resultHashMap.remove(Screening.BMI_CATEGORY)
            } else {
                val bmiInfo = CommonUtils.getBMIInformation(requireContext(), bmi)
                if (bmiInfo == null) {
                    tvValue.text = bmi.toString()
                } else {
                    viewModel.resultHashMap[Screening.BMI_CATEGORY] = bmiInfo.first
                    val bmiWithCategoryInfo = SpannableStringBuilder()
                        .append(bmi.toString())
                        .color(requireContext().getColor(bmiInfo.second)) {
                            append(" (${bmiInfo.first})")
                        }
                    tvValue.text = bmiWithCategoryInfo
                }
            }
        }
    }

    private fun setupRadioButtons(radioButton: RadioButton) {
        val mContext = requireContext()
        val colorStateList = ColorStateList(
            arrayOf(
                intArrayOf(-android.R.attr.state_enabled),
                intArrayOf(-android.R.attr.state_checked),
                intArrayOf(android.R.attr.state_checked)
            ), intArrayOf(
                ContextCompat.getColor(
                    mContext,
                    R.color.navy_blue_20_alpha
                ),  // disabled
                ContextCompat.getColor(mContext, R.color.purple),  // disabled
                ContextCompat.getColor(mContext, R.color.purple) // enabled
            )
        )
        val textColorStateList = ColorStateList(
            arrayOf(
                intArrayOf(-android.R.attr.state_enabled),
                intArrayOf(-android.R.attr.state_checked),
                intArrayOf(android.R.attr.state_checked)
            ), intArrayOf(
                ContextCompat.getColor(
                    mContext,
                    R.color.navy_blue_20_alpha
                ),  // disabled
                ContextCompat.getColor(mContext, R.color.navy_blue), // enabled
                ContextCompat.getColor(mContext, R.color.purple)
            )
        )
        radioButton.setPadding(20.dp, 0, 20.dp, 0)
        radioButton.setTextColor(textColorStateList)
        radioButton.buttonTintList = colorStateList
        radioButton.invalidate()
        radioButton.textSizeSsp = Screening.SSP16
        radioButton.typeface = ResourcesCompat.getFont(mContext, R.font.inter_regular)
    }

    private fun checkInputsAndEnableNextField(
        text: CharSequence,
        list: ArrayList<BPModel>
    ) {
        val binding = binding.llBpReading
        if (text == getString(R.string.sno_1)) {
            val systolicReadingOne = binding.etSystolicOne.text?.toString()
            val diastolicReadingOne = binding.etDiastolicOne.text?.toString()
            val pulseReadingOne = binding.etPulseOne.text?.toString()
            if (list.size > 0) {
                val model = list[0]
                model.systolic = systolicReadingOne?.toDoubleOrNull()
                model.diastolic = diastolicReadingOne?.toDoubleOrNull()
                model.pulse = pulseReadingOne?.toDoubleOrNull()
            }
            if (!systolicReadingOne.isNullOrBlank() && !diastolicReadingOne.isNullOrBlank()) {
                binding.tvSnoReadingTwo.isEnabled = true
                binding.etSystolicTwo.isEnabled = true
                binding.etDiastolicTwo.isEnabled = true
                binding.etPulseTwo.isEnabled = true
                binding.separatorRowTwo.isEnabled = true
            }
        } else if (text == getString(R.string.sno_2)) {
            val systolicReading = binding.etSystolicTwo.text?.toString()
            val diastolicReading = binding.etDiastolicTwo.text?.toString()
            val pulseReading = binding.etPulseTwo.text?.toString()
            if (list.size > 1) {
                val model = list[1]
                model.systolic = systolicReading?.toDoubleOrNull()
                model.diastolic = diastolicReading?.toDoubleOrNull()
                model.pulse = pulseReading?.toDoubleOrNull()
            }
            if (!systolicReading.isNullOrBlank() && !diastolicReading.isNullOrBlank()) {
                binding.tvSnoReadingThree.isEnabled = true
                binding.etSystolicThree.isEnabled = true
                binding.etDiastolicThree.isEnabled = true
                binding.etPulseThree.isEnabled = true
                binding.separatorRowThree.isEnabled = true
            }
        } else if (text == getString(R.string.sno_3)) {
            val systolicReading = binding.etSystolicThree.text?.toString()
            val diastolicReading = binding.etDiastolicThree.text?.toString()
            val pulseReading = binding.etPulseThree.text?.toString()
            if (list.size > 2) {
                val model = list[2]
                model.systolic = systolicReading?.toDoubleOrNull()
                model.diastolic = diastolicReading?.toDoubleOrNull()
                model.pulse = pulseReading?.toDoubleOrNull()
            }
        }
    }

    private fun setMaxLength(appCompatEditText: AppCompatEditText) {
        appCompatEditText.filters = arrayOf(InputFilter.LengthFilter(3))
    }


    private fun renderAssessmentForm() {
        viewModel.getRiskEntityList()
    }


    override fun onClick(mView: View?) {
        when (mView?.id) {
            binding.btnAddReading.id -> {
                validateInputs()
            }

            binding.btnCancel.id, binding.ivClose.id -> {
                dismiss()
            }

            binding.etAssessmentDate.id -> showDatePickerDialog()
        }
    }

    private fun validateInputs() {
        var isValid = true
        isValid = validateHeightAndWeight(
            isValid,
            binding.etHeight,
            binding.tvHeightErrorMessage,
            viewModel.height,
            errorMessage = getErrorMessage(
                getString(
                    R.string.validation_error,
                    getString(R.string.height_value)
                )
            )
        )

        isValid = validateHeightAndWeight(
            isValid, binding.etWeight, binding.tvWeightErrorMessage, viewModel.weight,
            errorMessage = getString(R.string.validation_error, getString(R.string.weight_value))
        )

        if (binding.etAssessmentDate.text.isNullOrBlank()) {
            isValid = false
            binding.tvAssessmentDateErrorMessage.visible()
            binding.tvAssessmentDateErrorMessage.text = getString(R.string.valid_assessment_date)
        } else
            binding.tvAssessmentDateErrorMessage.gone()

        if (binding.llBpReading.etSystolicOne.text.isNullOrBlank() ||
            binding.llBpReading.etDiastolicOne.text.isNullOrBlank()
        ) {
            isValid = false
            binding.tvBpLogErrorMessage.text = getErrorMessage(
                viewModel.bpLog?.cultureErrorMessage
                    ?: viewModel.bpLog?.errorMessage
            )
            binding.tvBpLogErrorMessage.visible()
        } else {
            isValid = validBPInputCheck(isValid)
        }

        if (binding.tvAssessmentDateErrorMessage.visibility == View.VISIBLE)
            isValid = false

        if (binding.smokingGrp.visibility == View.VISIBLE) {
            if (!binding.btnYes.isChecked && !binding.btnNo.isChecked) {
                isValid = false
                binding.tvSmokingErr.visible()
            } else
                binding.tvSmokingErr.gone()
        }

        if (isValid) {
            processResultAndProceed()
        }
    }

    private fun processResultAndProceed() {
        // Remove unwanted keys
        val result = HashMap<String, Any>()
        result.putAll(viewModel.resultHashMap)
        patientViewModel.patientDetailsLiveData.value?.data?.let { patientData ->
            with(result) {
                NCDMRUtil.getBioDataBioMetrics(
                    result,
                    patientData,
                    binding.etHeight.text.toString().toDouble(),
                    binding.etWeight.text.toString().toDouble()
                )

                put(MemberRegistration.gender, patientData.gender ?: "")
                // Add individual parameters
                put(Screening.Weight, binding.etWeight.text.toString().toDouble())
                put(Screening.Height, binding.etHeight.text.toString().toDouble())
                put(Screening.UnitMeasurement, Screening.Unit_Measurement_Metric_Type)
                patientViewModel.getPatientId()?.let {
                    put(AssessmentDefinedParams.patientReference, it)
                }
                patientViewModel.getPatientFHIRId()?.let {
                    put(
                        AssessmentDefinedParams.memberReference,
                        it
                    )
                }

                put(
                    AssessmentDefinedParams.assessmentTakenOn, DateUtils.getDateStringInFormat(
                        binding.etAssessmentDate.text.toString(),
                        DateUtils.DATE_ddMMyyyy,
                        DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ
                    )
                )
                put(AssessmentDefinedParams.encounter,hashMapOf<String, Any?>(DefinedParams.Provenance to ProvanceDto()))
                put(Screening.DateOfBirth, patientData.dateOfBirth ?: "")
                put(AssessmentDefinedParams.assessmentType, Assessment)
                put(DefinedParams.AssessmentOrganizationId,SecuredPreference.getOrganizationFhirId())
                put(AssessmentDefinedParams.assessmentProcessType, CommonUtils.requestFrom())
                patientData.isRegularSmoker?.let { put(Screening.is_regular_smoker, it) }
                // Check and set smoking status
                if (binding.smokingGrp.visibility == View.VISIBLE) {
                    put(Screening.is_regular_smoker, binding.btnYes.isChecked)
                }
            }
        }


        // Calculate and update result hash map values
        if (result.containsKey(Screening.BPLog_Details) && result[Screening.BPLog_Details] != null) {
            CommonUtils.calculateAverageBloodPressure(result)
        }
        CommonUtils.calculateBMI(result)


        // Parse and calculate CVD Risk
        viewModel.getRiskEntityListLiveData.value?.firstOrNull()?.nonLabEntity?.let { nonLabEntity ->
            val resultList = Gson().fromJson<ArrayList<RiskClassificationModel>>(
                nonLabEntity,
                object : TypeToken<ArrayList<RiskClassificationModel>>() {}.type
            )
            CommonUtils.calculateCVDRiskFactor(
                result,
                resultList,
                (result[Screening.Avg_Systolic] as? Number)?.toInt()
            )
        }

        // Add BMI to BioMetrics map if calculated
        (result[Screening.BMI] as? Double)?.let { bmi ->
            (result[Screening.BioMetrics] as? HashMap<String, Any>)?.put(
                Screening.BMI,
                bmi
            )
        }


        // Consolidate BP Log details
        result[Screening.bp_log] = hashMapOf(
            Screening.Avg_Systolic to result[Screening.Avg_Systolic],
            Screening.BMI_CATEGORY to viewModel.resultHashMap[Screening.BMI_CATEGORY],
            Screening.Avg_Diastolic to result[Screening.Avg_Diastolic],
            Screening.Avg_Blood_pressure to result[Screening.Avg_Blood_pressure],
            Screening.BPLog_Details to result[Screening.BPLog_Details]
        )
        result.keys.removeAll(
            setOf(
                Screening.Weight, Screening.Height, Screening.BMI, Screening.BPLog_Details,
                Screening.Avg_Blood_pressure, Screening.Avg_Diastolic, Screening.BMI_CATEGORY,
                Screening.Avg_Systolic, MemberRegistration.gender
            )
        )

        // Network check and action
        if (connectivityManager.isNetworkAvailable()) {
            Timber.d("${result}")
            viewModel.createBpLog(result) // Uncomment as needed
        } else {
            (activity as BaseActivity).showErrorDialogue(
                getString(R.string.error),
                getString(R.string.no_internet_error), false
            ) {}
        }
    }


    private fun validateHeightAndWeight(
        validOrNot: Boolean,
        etInput: AppCompatEditText,
        tvErrorMessage: AppCompatTextView,
        formJson: FormLayout?,
        errorMessage: String? = null
    ): Boolean {
        var isValid = validOrNot
        if (etInput.text.isNullOrBlank()) {
            isValid = false
            tvErrorMessage.visible()
            errorMessage?.let {
                tvErrorMessage.text = errorMessage
            }
        } else {
            isValid = setErrorView(
                etInput.text.toString().toDouble(),
                formJson?.minValue,
                formJson?.maxValue,
                tvErrorMessage,
                isValid
            )
        }
        return isValid
    }

    private fun setErrorView(
        value: Double,
        minValue: Double?,
        maxValue: Double?,
        tvErrorMessage: AppCompatTextView,
        validOrNot: Boolean,
    ): Boolean {
        var isValid = validOrNot
        if (minValue != null || maxValue != null) {
            if (minValue != null && maxValue != null) {
                if (value < minValue || value > maxValue) {
                    isValid = false
                    tvErrorMessage.visible()
                    tvErrorMessage.text =
                        getString(
                            R.string.general_min_max_validation,
                            CommonUtils.getDecimalFormatted(minValue),
                            CommonUtils.getDecimalFormatted(maxValue)
                        )
                } else {
                    tvErrorMessage.gone()
                }
            } else if (minValue != null) {
                isValid = minMaxValueCheck(value, minValue, 0, isValid, tvErrorMessage)
            } else if (maxValue != null) {
                isValid = minMaxValueCheck(value, maxValue, 1, isValid, tvErrorMessage)
            }
        } else {
            tvErrorMessage.gone()
        }
        return isValid
    }

    private fun minMaxValueCheck(
        value: Double,
        minOrMaxValue: Double,
        minMaxFlg: Int,
        validOrNot: Boolean,
        tvErrorMessage: AppCompatTextView
    ): Boolean {
        var isValid = validOrNot
        if (minMaxFlg == 0) {
            if (value < minOrMaxValue) {
                isValid = false
                tvErrorMessage.visible()
                tvErrorMessage.text =
                    getString(
                        R.string.general_min_validation,
                        CommonUtils.getDecimalFormatted(minOrMaxValue)
                    )
            } else {
                tvErrorMessage.gone()
            }
        } else {
            if (value > minOrMaxValue) {
                isValid = false
                tvErrorMessage.visible()
                tvErrorMessage.text = getString(
                    R.string.general_max_validation,
                    CommonUtils.getDecimalFormatted(minOrMaxValue)
                )
            } else {
                tvErrorMessage.gone()
            }
        }
        return isValid
    }

    private fun getErrorMessage(errorMessage: String?): String {
        return if (errorMessage.isNullOrBlank()) getString(R.string.default_user_input_error) else errorMessage
    }

    private fun validBPInputCheck(valid: Boolean): Boolean {
        var isValid = valid
        val list = ArrayList<BPModel>()
        (viewModel.resultHashMap[Screening.BPLog_Details] as? List<*>)?.let { data ->
            data.filterIsInstance<BPModel>().forEach { bpModel ->
                if (bpModel.diastolic != null || bpModel.systolic != null) {
                    list.add(bpModel)
                }
            }
        }
        val validationBPResultModel =
            Validator.checkValidBPInput(
                requireContext(),
                list,
                viewModel.bpLog
            )
        if (validationBPResultModel.status) {
            viewModel.resultHashMap[Screening.BPLog_Details] = list
            viewModel.bpLog?.let {
                viewModel.calculateBPValues(
                    formLayout = it,
                    viewModel.resultHashMap
                )
            }
            binding.tvBpLogErrorMessage.gone()
        } else {
            isValid = false
            binding.tvBpLogErrorMessage.text = getErrorMessage(validationBPResultModel.message)
            binding.tvBpLogErrorMessage.visible()
        }
        return isValid
    }

    private fun showDatePickerDialog() {
        var datePickerDialog: DatePickerDialog? = null
        var yearMonthDate: Triple<Int?, Int?, Int?>? = null
        if (!binding.etAssessmentDate.text.isNullOrBlank())
            yearMonthDate = DateUtils.getYearMonthAndDate(binding.etAssessmentDate.text.toString())

        if (datePickerDialog == null) {
            datePickerDialog = ViewUtils.showDatePicker(
                context = requireContext(),
                maxDate = System.currentTimeMillis(),
                date = yearMonthDate,
                cancelCallBack = { datePickerDialog = null }
            ) { _, year, month, dayOfMonth ->
                val stringDate = "$dayOfMonth-$month-$year"
                if (DateUtils.validateForSameDate(year, month, dayOfMonth)) {
                    binding.tvAssessmentDateErrorMessage.visible()
                    binding.etAssessmentDate.text = ""
                    binding.tvAssessmentDateErrorMessage.text =
                        getString(R.string.please_select_past_date)
                } else
                    hideDateValidation(stringDate)
                datePickerDialog = null
            }
        }
    }

    private fun hideDateValidation(stringDate: String) {
        binding.etAssessmentDate.text =
            DateUtils.convertDateTimeToDate(
                stringDate,
                DateUtils.DATE_FORMAT_ddMMyyyy,
                DateUtils.DATE_ddMMyyyy
            )
        binding.tvAssessmentDateErrorMessage.text = ""
        binding.tvAssessmentDateErrorMessage.gone()
    }
}