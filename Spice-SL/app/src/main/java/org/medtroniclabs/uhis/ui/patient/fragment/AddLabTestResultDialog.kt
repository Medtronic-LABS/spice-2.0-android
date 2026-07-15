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
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.postError
import org.medtroniclabs.uhis.appextensions.setError
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.CodeDetailsObject
import org.medtroniclabs.uhis.data.EncounterDetails
import org.medtroniclabs.uhis.data.offlinesync.model.ProvanceDto
import org.medtroniclabs.uhis.data.registration.LabTestModel
import org.medtroniclabs.uhis.data.registration.LabTestResult
import org.medtroniclabs.uhis.databinding.AddLabTestResultBinding
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams
import org.medtroniclabs.uhis.formgeneration.extension.markMandatory
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.model.LabTestCreateRequest
import org.medtroniclabs.uhis.model.LabTestDetails
import org.medtroniclabs.uhis.model.LabTestResultObject
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
        val labResults = resultsAdapter.getResultsList()
        val testedOn = DateUtils.convertDateTimeToDate(
            binding.tvTestedOn.text.toString(),
            DateUtils.DATE_FORMAT_ddMMMyyyy,
            DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
            inUserTimeZone = true,
        )
        val performedBy = SecuredPreference.getUserFhirId()
        val resultObjects = ArrayList<LabTestResultObject>()
        labResults.forEach { row ->
            val fieldId = (row[LabTestViewModel.FHIR_FIELD_ID] as? String)
                ?: (row[DefinedParams.NAME] as? String).orEmpty()
            val code = row[LabTestViewModel.FHIR_CODE] as? String
            val url = row[LabTestViewModel.FHIR_URL] as? String
            resultObjects.add(
                LabTestResultObject(
                    name = fieldId,
                    value = row[DefinedParams.RESULT_VALUE],
                    performedBy = performedBy,
                    codeDetails = if (code != null && url != null) CodeDetailsObject(code, url) else null,
                    testedOn = testedOn,
                    resource = row[LabTestViewModel.FHIR_RESOURCE] as? String,
                    unit = row[DefinedParams.UNIT] as? String,
                ),
            )
        }
        viewModel.createLabTestResultFhir(buildLabTestCreateRequest(resultObjects))
    }

    private fun buildLabTestCreateRequest(resultObjects: ArrayList<LabTestResultObject>): LabTestCreateRequest {
        val nurseMr = nurseViewModel.nurseMrRequestModel
        val patient = nurseViewModel.patientDetailsValue
        val detail = LabTestDetails(
            testName = labTestModel.labTestName.orEmpty(),
            labTestId = labTestModel.labTestId,
            recommendedBy = labTestModel.recommendedById ?: SecuredPreference.getUserFhirId(),
            recommendedName = labTestModel.referredByDisplay,
            recommendedOn = labTestModel.referredDate.orEmpty(),
            labTestResults = resultObjects,
            id = labTestModel.fhirId ?: labTestModel._id?.toString(),
        )
        val encounter = EncounterDetails(
            id = nurseMr.encounterReference,
            patientReference = nurseMr.patientReference,
            patientId = patient?.patientId,
            memberId = nurseMr.memberReference,
            provenance = ProvanceDto(),
            visitId = nurseMr.patientVisitId?.takeIf { it > 0L }?.toString(),
        )
        return LabTestCreateRequest(
            encounter = encounter,
            labTests = arrayListOf(detail),
            identityValue = patient?.identityValue,
        )
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
