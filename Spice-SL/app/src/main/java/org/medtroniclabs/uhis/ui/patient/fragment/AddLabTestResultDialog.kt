package org.medtroniclabs.uhis.ui.patient.fragment

import android.app.DatePickerDialog
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.internal.LinkedTreeMap
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.postError
import org.medtroniclabs.uhis.appextensions.setError
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.registration.LabTestModel
import org.medtroniclabs.uhis.data.registration.LabTestResult
import org.medtroniclabs.uhis.databinding.AddLabTestResultBinding
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams
import org.medtroniclabs.uhis.formgeneration.extension.markMandatory
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.patient.adapter.LabTestResultsAdapter
import org.medtroniclabs.uhis.ui.patient.util.ViewUtil.showDatePicker
import org.medtroniclabs.uhis.ui.patient.viewmodel.LabTestViewModel
import org.medtroniclabs.uhis.ui.patient.viewmodel.NurseMedicalReviewViewModel
import kotlin.getValue

class AddLabTestResultDialog(
    private val labTestModel: LabTestModel,
    private val callback: (isPositiveResult: Boolean) -> Unit,
) : DialogFragment(),
    View.OnClickListener {
    companion object {
        const val TAG = "AddLabTestResultDialog"

        fun newInstance(
            labTestModel: LabTestModel,
            callback: (isPositiveResult: Boolean) -> Unit,
        ): AddLabTestResultDialog = AddLabTestResultDialog(labTestModel, callback)
    }

    private lateinit var binding: AddLabTestResultBinding
    private lateinit var resultsAdapter: LabTestResultsAdapter
    private val viewModel: LabTestViewModel by activityViewModels()
    private var datePickerDialog: DatePickerDialog? = null
    private val nurseViewModel: NurseMedicalReviewViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = AddLabTestResultBinding.inflate(inflater, container, false)
        val window: Window? = dialog?.window
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        window?.attributes?.apply {
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            gravity = Gravity.CENTER
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window?.setDecorFitsSystemWindows(false)
        }
        binding.root.setOnApplyWindowInsetsListener { _, windowInsets ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val imeHeight = windowInsets.getInsets(WindowInsets.Type.ime()).bottom
                binding.root.setPadding(0, 0, 0, imeHeight)
                windowInsets.getInsets(WindowInsets.Type.ime() or WindowInsets.Type.systemGestures())
            }
            windowInsets
        }

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews()
        setListeners()
        setAdapterViews()
        attachObservers()
    }

    private fun attachObservers() {
        viewModel.createResultResponse.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> showLoading()
                ResourceState.ERROR -> {
                    hideLoading()
                    resourceState?.message?.let { message ->
                        (activity as BaseActivity).showErrorDialogue(getString(R.string.error), message, false) {}
                        viewModel.createResultResponse.postError(null)
                    }
                }
                ResourceState.SUCCESS -> {
                    viewModel.createResultResponse.setError()
                    dismiss()
                    callback.invoke(true)
                }
            }
        }
    }

    fun showLoading() {
        binding.loadingProgress.visibility = View.VISIBLE
    }

    fun hideLoading() {
        binding.loadingProgress.visibility = View.GONE
    }

    private fun setListeners() {
        binding.btnSave.safeClickListener(this)
        binding.btnCancel.safeClickListener(this)
        binding.titleCard.ivClose.safeClickListener(this)
        binding.tvTestedOn.safeClickListener(this)
    }

    private fun setAdapterViews() {
        val layoutManager = GridLayoutManager(requireContext(), 1, RecyclerView.VERTICAL, false)
        binding.rvResults.layoutManager = layoutManager
        binding.rvResults.adapter = resultsAdapter
    }

    private fun initializeViews() {
        resultsAdapter = LabTestResultsAdapter(labTestModel.labTestName)
        labTestModel.patientLabtestResults?.let {
            resultsAdapter.setData(it, viewModel.labTestUnitList)
        }
        binding.titleCard.titleView.text = labTestModel.labTestName ?: "-"
        binding.tvTestedOnLbl.markMandatory()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
        )
        dialog?.window?.attributes?.windowAnimations = R.style.dialogEnterExitAnimation
    }

    override fun onClick(mView: View?) {
        when (mView?.id) {
            binding.btnSave.id -> {
                validateInputs()
            }

            binding.btnCancel.id, binding.titleCard.ivClose.id -> {
                nurseViewModel.labTestId = null
                dismiss()
                callback.invoke(false)
            }

            binding.tvTestedOn.id -> showDatePickerDialog()
        }
    }

    private fun validateInputs() {
        var isValid = true

        if (binding.tvTestedOn.text.isNullOrBlank()) {
            isValid = false
            binding.tvTestedOnError.visibility = View.VISIBLE
            binding.tvTestedOnError.text = getString(R.string.select_tested_on_date)
        } else {
            binding.tvTestedOnError.visibility = View.GONE
        }

        if (!resultsAdapter.validateInputs()) {
            isValid = false
        }

        if (isValid) {
            createRequest()
        } else {
            resultsAdapter.notifyDataSetChanged()
        }
    }

    private fun createRequest() {
        val request = HashMap<String, Any>()
        val labResults = resultsAdapter.getResultsList()
        var isEmptyRanges = true
        labResults.forEach { resultMap ->
            // Entered Value
            val result = resultMap[DefinedParams.RESULT_VALUE]
            var enteredValue: Double? = null
            if (result is String) {
                enteredValue = result.toDoubleOrNull()
            }

            // Comparing the selected unit test range with the entered value
            val selectedUnit = resultMap[DefinedParams.UNIT]
            val rangesList = resultMap[DefinedParams.LAB_RESULT_RANGE]
            if (rangesList is ArrayList<*>) {
                isEmptyRanges = rangesList.isEmpty()
                handleRangeList(resultMap, rangesList, selectedUnit, enteredValue)
            }
            request[DefinedParams.IS_EMPTY_RANGES] = isEmptyRanges

            // Removing the Lab Result Ranges list(As we have calculated the value for the is_abnormal key)
            resultMap.remove(DefinedParams.LAB_RESULT_RANGE)
        }
        request[DefinedParams.PATIENT_LABTEST_RESULTS] = labResults
        request[DefinedParams.REFFERED_DATE] = labTestModel.referredDate ?: ""
        request[DefinedParams.PATIENT_LABTEST_ID] = labTestModel._id ?: -1
        request[DefinedParams.TENANT_ID] = SecuredPreference.getTenantId()
        request[DefinedParams.TESTED_ON] = DateUtils.convertDateTimeToDate(
            binding.tvTestedOn.text.toString(),
            DateUtils.DATE_FORMAT_ddMMMyyyy,
            DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
            inUserTimeZone = true,
        )
        request[DefinedParams.COMMENT] = binding.etComment.text?.toString() ?: ""
        val userRole = SecuredPreference.getUserDetails()?.roles?.joinToString { it.name } ?: ""
        request[DefinedParams.ROLE_NAME] = userRole
        request[DefinedParams.IS_REVIEWED] = false
        viewModel.createLabTestResult(requireContext(), request)
    }

    private fun handleRangeList(
        resultMap: HashMap<String, Any>,
        rangesList: ArrayList<*>,
        selectedUnit: Any?,
        enteredValue: Double?,
    ) {
        rangesList.forEach range@{ range ->
            if (range is LinkedTreeMap<*, *>) {
                val unit = range[DefinedParams.UNIT]
                if (unit is String && selectedUnit is String && unit == selectedUnit) {
                    validateRanges(resultMap, range, enteredValue)
                    return@range
                }
            }
        }
    }

    private fun validateRanges(
        resultMap: HashMap<String, Any>,
        range: LinkedTreeMap<*, *>,
        enteredValue: Double?,
    ) {
        val min = range[DefinedParams.MINIMUM_VALUE] ?: 0.0
        val max = range[DefinedParams.MAXIMUM_VALUE]
        max?.let { maximumRange ->
            if (min is Double && maximumRange is Double) {
                enteredValue?.let { value ->
                    val isNormal = value >= min && value <= maximumRange
                    resultMap[DefinedParams.IS_ABNORMAL] = !isNormal
                    resultMap[DefinedParams.RESULT_STATUS] =
                        if (isNormal) DefinedParams.RESULT_NEGATIVE else DefinedParams.RESULT_POSITIVE
                }
            }
        }
        if (!resultMap.containsKey(DefinedParams.IS_ABNORMAL)) {
            resultMap[DefinedParams.IS_ABNORMAL] = false
            resultMap[DefinedParams.RESULT_STATUS] =
                DefinedParams.RESULT_NEGATIVE
        }
        val displayName = range[DefinedParams.DISPLAY_NAME]
        if (displayName is String) {
            resultMap[DefinedParams.DISPLAY_NAME] = displayName
        }
    }

    private fun showDatePickerDialog() {
        var yearMonthDate: Triple<Int?, Int?, Int?>? = null
        if (!binding.tvTestedOn.text.isNullOrBlank()) {
            yearMonthDate = DateUtils.convertddMMMToddMM(binding.tvTestedOn.text.toString())
        }

        if (datePickerDialog == null) {
            datePickerDialog = showDatePicker(
                context = requireContext(),
                disableFutureDate = true,
                date = yearMonthDate,
                cancelCallBack = { datePickerDialog = null },
            ) { _, year, month, dayOfMonth ->
                val stringDate = "$dayOfMonth-$month-$year"
                binding.tvTestedOn.text =
                    DateUtils.convertDateTimeToDate(
                        stringDate,
                        DateUtils.DATE_FORMAT_ddMMyyyy,
                        DateUtils.DATE_FORMAT_ddMMMyyyy,
                    )
                datePickerDialog = null
            }
        }
    }

    private var okayButtonClickListener: DangerSignsClickListener? = null

    fun setDangerSignListener(listener: DangerSignsClickListener) {
        this.okayButtonClickListener = listener
    }

    interface DangerSignsClickListener {
        fun onDangerSignsClicked(b: Pair<Long?, ArrayList<LabTestResult>>)
    }
}
