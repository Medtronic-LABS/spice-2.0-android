package com.medtroniclabs.opensource.ncd.medicalreview.dialog

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.appcompat.widget.AppCompatTextView
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
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.common.ViewUtils
import com.medtroniclabs.opensource.data.model.ChipViewItemModel
import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto
import com.medtroniclabs.opensource.databinding.FragmentNcdBloodGlucoseReadingDialogBinding
import com.medtroniclabs.opensource.formgeneration.extension.markMandatory
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.formgeneration.model.FormLayout
import com.medtroniclabs.opensource.formgeneration.model.FormResponse
import com.medtroniclabs.opensource.formgeneration.utility.DecimalInputFilter
import com.medtroniclabs.opensource.mappingkey.Screening
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil
import com.medtroniclabs.opensource.ncd.medicalreview.viewmodel.NCDBloodPressureVitalsViewModel
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.network.utils.ConnectivityManager
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.TagListCustomView
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.PatientDetailViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NCDBloodGlucoseReadingDialog(private val callback: () -> Unit) : DialogFragment(), View.OnClickListener {

    private lateinit var binding: FragmentNcdBloodGlucoseReadingDialogBinding
    private lateinit var tagListCustomView: TagListCustomView
    private var datePickerDialog: DatePickerDialog? = null
    private val viewModel: NCDBloodPressureVitalsViewModel by viewModels()
    private val patientViewModel: PatientDetailViewModel by activityViewModels()

    @Inject
    lateinit var connectivityManager: ConnectivityManager
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNcdBloodGlucoseReadingDialogBinding.inflate(inflater, container, false)
        val window: Window? = dialog?.window
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        return binding.root
    }

    companion object {
        const val TAG = "NCDBloodGlucoseReadingDialog"
        fun newInstance(callback: () -> Unit) =
            NCDBloodGlucoseReadingDialog(callback)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = false
        viewModel.getNcdFormData(DefinedParams.Assessment, NCDMRUtil.NCD.lowercase())
        viewModel.getRiskEntityList()
        attachObservers()
        setClickListeners()
        initializeTagView()
    }

    private fun setupView() {
        viewModel.bloodGlucose?.let { bloodGlucose ->
            binding.tvBloodGlucoseTitle.text = setTitle(bloodGlucose)
            binding.etBloodGlucose.hint = setHint(bloodGlucose)
        }
        viewModel.hbA1c?.let { hbA1c ->
            binding.tvHbA1c.text = setTitle(hbA1c)
            binding.etHbA1c.hint = setHint(hbA1c)
        }
        binding.etBloodGlucose.filters = arrayOf<InputFilter>(DecimalInputFilter())
        binding.etHbA1c.filters = arrayOf<InputFilter>(DecimalInputFilter())
        binding.tvAssessmentDate.markMandatory()
        binding.tvBloodGlucoseTitle.markMandatory()
        binding.tvSelectType.markMandatory()
    }

    private fun setHint(formData: FormLayout): CharSequence? {
        return (formData.hint)
    }

    private fun setTitle(formData: FormLayout): CharSequence {
        return (formData.title) + " (${formData.unitMeasurement})"
    }

    private fun initializeTagView() {
        tagListCustomView =
            TagListCustomView(
                requireContext(),
                binding.cgSelectType,
                callBack = { _, _, _ ->
                    viewModel.selectedChips =
                        ArrayList(tagListCustomView.getSelectedTags())
                }
            )
        tagListCustomView.addChipItemList(getChip(), viewModel.selectedChips)
    }

    private fun getChip(): ArrayList<ChipViewItemModel> {
        val chipItemList = ArrayList<ChipViewItemModel>()
        chipItemList.add(
            ChipViewItemModel(
                id = 1,
                name = NCDMRUtil.FBS,
                cultureValue = getString(R.string.fbs),
                value = NCDMRUtil.FBS
            )
        )
        chipItemList.add(
            ChipViewItemModel(
                id = 2,
                name = NCDMRUtil.RBS,
                cultureValue = getString(R.string.rbs),
                value = NCDMRUtil.RBS,
            )
        )
        return chipItemList
    }

    private fun setClickListeners() {
        binding.labelHeader.ivClose.safeClickListener(this)
        binding.btnCancel.safeClickListener(this)
        binding.btnAddReading.safeClickListener(this)
        binding.etAssessmentDate.safeClickListener(this)
    }

    override fun onClick(mView: View?) {
        when (mView?.id) {
            binding.btnAddReading.id -> {
                validateInputs()
            }

            binding.btnCancel.id, binding.labelHeader.ivClose.id -> {
                dismiss()
            }

            binding.etAssessmentDate.id -> showDatePickerDialog()
        }
    }

    private fun showDatePickerDialog() {
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
                } else {
                    hideDateValidationError(stringDate)
                }
                datePickerDialog = null
            }
        }
    }

    private fun hideDateValidationError(stringDate: String) {
        binding.tvAssessmentDateErrorMessage.text = ""
        binding.tvAssessmentDateErrorMessage.gone()
        binding.etAssessmentDate.text =
            DateUtils.convertDateTimeToDate(
                stringDate,
                DateUtils.DATE_FORMAT_ddMMyyyy,
                DateUtils.DATE_ddMMyyyy
            )
    }

    private fun validateInputs() {
        var isValid = true
        val assessmentDate = binding.etAssessmentDate.text
        val bgValue = binding.etBloodGlucose.text
        val hbA1cValue = binding.etHbA1c.text

        if (assessmentDate.isNullOrBlank()) {
            isValid = false
            binding.tvAssessmentDateErrorMessage.visible()
            binding.tvAssessmentDateErrorMessage.text = getString(R.string.valid_assessment_date)
        } else
            binding.tvAssessmentDateErrorMessage.gone()

        if (bgValue.isNullOrBlank()) {
            isValid = false
            binding.tvBloodGlucoseErrorMessage.visible()
            binding.tvBloodGlucoseErrorMessage.text = getString(
                R.string.validation_error, getString(
                    R.string.glucose_value
                )
            )
        } else {
            isValid = setErrorView(
                binding.etBloodGlucose.text.toString().toDouble(),
                viewModel.bloodGlucose?.minValue,
                viewModel.bloodGlucose?.maxValue,
                binding.tvBloodGlucoseErrorMessage,
                isValid
            )
        }

        if (!binding.etHbA1c.text.isNullOrBlank()) {
            isValid = setErrorView(
                binding.etHbA1c.text.toString().toDouble(),
                viewModel.hbA1c?.minValue,
                viewModel.hbA1c?.maxValue,
                binding.tvHbA1cErrorMessage,
                isValid
            )
        }

        if (tagListCustomView.getSelectedTags().isEmpty()) {
            isValid = false
            binding.tvSelectTypeErrorMessage.visible()
            binding.tvSelectTypeErrorMessage.text = getString(R.string.bg_select_type)
        } else
            binding.tvSelectTypeErrorMessage.gone()

        if (isValid) {
            processResultandProceed()
        }
    }

    private fun processResultandProceed() {
        val result = HashMap<String, Any>().apply {
            putAll(viewModel.bgResultHashMap)
        }

        val assessmentDate = DateUtils.getDateStringInFormat(
            binding.etAssessmentDate.text.toString(),
            DateUtils.DATE_ddMMyyyy,
            DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ
        )

        patientViewModel.patientDetailsLiveData.value?.data?.let { patientData ->
            result.apply {
                NCDMRUtil.getBioDataBioMetrics(result, patientData, isGlucose = true)

                putAll(
                    mapOf(
                        AssessmentDefinedParams.encounter to hashMapOf<String, Any?>(DefinedParams.Provenance to ProvanceDto()),
                        AssessmentDefinedParams.assessmentType to DefinedParams.Assessment,
                        DefinedParams.AssessmentOrganizationId to SecuredPreference.getOrganizationFhirId(),
                        AssessmentDefinedParams.assessmentProcessType to CommonUtils.requestFrom(),
                        AssessmentDefinedParams.assessmentTakenOn to assessmentDate
                    )
                )
                patientViewModel.getPatientId()?.let {
                    put(AssessmentDefinedParams.patientReference, it)
                }
                patientViewModel.getPatientFHIRId()?.let {
                    put(AssessmentDefinedParams.memberReference, it)

                }
            }
        }

        val hbA1cValue =
            binding.etHbA1c.text?.trim()?.toString()?.takeIf { it.isNotBlank() }?.toDoubleOrNull()
        val bgValue = binding.etBloodGlucose.text?.trim()?.toString()?.takeIf { it.isNotBlank() }
            ?.toDoubleOrNull()

        if (bgValue != null) {
            result[DefinedParams.GLUCOSE_LOG] = hashMapOf<String, Any?>(
                AssessmentDefinedParams.HBA1CUnit to viewModel.hbA1c?.unitMeasurement,
                Screening.Glucose_Value to bgValue,
                Screening.Glucose_Type to tagListCustomView.getSelectedTags()
                    .firstOrNull()?.name?.lowercase(),
                AssessmentDefinedParams.Glucose_Date_Time to assessmentDate,
                Screening.GlucoseUnit to viewModel.bloodGlucose?.unitMeasurement,
                AssessmentDefinedParams.hba1c to hbA1cValue
            )
        }

        if (connectivityManager.isNetworkAvailable()) {
            // Uncomment to call viewModel method for glucose log creation
            viewModel.glucoseLogCreate(result)
        } else {
            (activity as BaseActivity).showErrorDialogue(
                getString(R.string.error),
                getString(R.string.no_internet_error),
                false
            ) {}
        }
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

    private fun attachObservers() {
        viewModel.getRiskEntityListLiveData.observe(this) {}
        viewModel.formLayoutsNcdLiveData.observe(viewLifecycleOwner) { data ->
            if (data != null) {
                val formFieldsType = object : TypeToken<FormResponse>() {}.type
                val formFields: FormResponse = Gson().fromJson(data, formFieldsType)
                viewModel.bloodGlucose =
                    formFields.formLayout.find { it.id.lowercase() == Screening.BloodGlucoseID.lowercase() }
                viewModel.hbA1c =
                    formFields.formLayout.find { it.id.lowercase() == AssessmentDefinedParams.hba1c.lowercase() }
                setupView()
            }
        }

        viewModel.glucoseLogCreateResponseLiveData.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
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

                ResourceState.SUCCESS -> {
                    hideLoading()
                    dismiss()
                    callback.invoke()
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
}