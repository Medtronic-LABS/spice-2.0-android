package org.medtroniclabs.uhis.ui.patient.fragment

import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.postError
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.data.model.ChipViewItemModel
import org.medtroniclabs.uhis.data.offlinesync.model.ProvanceDto
import org.medtroniclabs.uhis.data.registration.Diagnosis
import org.medtroniclabs.uhis.data.registration.PatientDetailsModel
import org.medtroniclabs.uhis.databinding.FragmentConfirmDiagnosisBinding
import org.medtroniclabs.uhis.db.entity.DiagnosisEntity
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams.HYPERTENSION
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.ncd.data.NCDDiagnosisItem
import org.medtroniclabs.uhis.ncd.data.NCDDiagnosisRequestResponse
import org.medtroniclabs.uhis.ncd.medicalreview.NCDMRUtil
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.TagListCustomView
import org.medtroniclabs.uhis.ui.assessment.utils.EnrollmentObservationFormatter
import org.medtroniclabs.uhis.ui.patient.util.CommonDialogInterface
import org.medtroniclabs.uhis.ui.patient.viewmodel.MedicalReviewBaseViewModel
import org.medtroniclabs.uhis.ui.patient.viewmodel.NurseMedicalReviewViewModel
import org.medtroniclabs.uhis.ui.patient.viewmodel.PatientDetailViewModel
import kotlin.getValue

class ConfirmDiagnosisDialog(val commonDialogInterface: CommonDialogInterface? = null) : DialogFragment(), View.OnClickListener {
    private var isSummary: Boolean = false
    private lateinit var binding: FragmentConfirmDiagnosisBinding
    private lateinit var tagListCustomView: TagListCustomView
    private var patientDetails: PatientDetailsModel? = null
    private val medicalReviewBaseViewModel: MedicalReviewBaseViewModel by activityViewModels()
    private val viewModel: PatientDetailViewModel by activityViewModels()
    private val nurseViewModel: NurseMedicalReviewViewModel by activityViewModels()

    companion object {
        const val TAG = "ConfirmDiagnosis"
        const val IS_SUMMARY = "IS_SUMMARY"

        fun newInstance(
            isSummary: Boolean,
            commonDialogInterface: CommonDialogInterface? = null,
        ): ConfirmDiagnosisDialog {
            val fragment = ConfirmDiagnosisDialog(commonDialogInterface)
            fragment.arguments = Bundle().apply {
                putBoolean(IS_SUMMARY, isSummary)
            }
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentConfirmDiagnosisBinding.inflate(inflater, container, false)

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
        readArguments()
        //  Handle the diagnosis based on the nurse MR
        if (CommonUtils.isNurse()) {
            handleDiagnosisResponse(nurseViewModel.patientDetailsValue)
        } else {
            handleDiagnosisResponse(viewModel.patientDetailsResponse.value?.data)
        }
        setListeners()
        initializeTagView()
        attachObserver()
    }

    private fun readArguments() {
        arguments?.let {
            isSummary = it.getBoolean(IS_SUMMARY, false)
        }
    }

    override fun onStart() {
        super.onStart()
        val window = dialog?.window
        window?.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
        )
    }

    private fun setListeners() {
        binding.labelHeader.ivClose.safeClickListener(this)
        binding.btnCancel.safeClickListener(this)
        binding.btnConfirm.safeClickListener(this)
    }

    private fun initializeTagView() {
        tagListCustomView = TagListCustomView(requireContext(), binding.cgDiagnosis) { _, _, _ ->
            enableConfirm()
        }
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            binding.labelHeader.ivClose.id -> {
                dismiss()
            }
            R.id.btnCancel -> {
                dismiss()
            }
            R.id.btnConfirm -> {
                saveDiagnosis()
            }
        }
    }

    private fun enableConfirm() {
        val selectedTag = tagListCustomView.getSelectedTags()
        val list: ArrayList<Diagnosis> = ArrayList()
        selectedTag.forEach {
            if (it is ChipViewItemModel) {
                list.add(Diagnosis(it.name))
            }
        }
        viewModel.confirmDiagnosisRequestData.confirmDiagnosis = list
    }

    private fun attachObserver() {
        viewModel.patientDetailsResponse.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.SUCCESS -> {
                    resourceState.data?.let {
                        patientDetails = it
                    }
                }
                else -> {
                    // Invoked if response state is not success
                }
            }
        }

        medicalReviewBaseViewModel.diagnosisListResponse.observe(this) { responseList ->
            val list = validateResponseList(responseList)
            val removeItem: List<DiagnosisEntity> = validateEntityList(list)
            (list as? ArrayList?)?.removeAll(removeItem)

            val chipItems = list.map { entity ->
                ChipViewItemModel(
                    id = entity._id,
                    name = entity.diagnosis,
                    cultureValue = entity.cultureValue,
                    type = entity.type,
                    value = entity.value,
                )
            }
            val diagnosisMap: HashMap<String, MutableList<ChipViewItemModel>> =
                chipItems.groupByTo(HashMap(), { it.type.toString() }, { it })

            // patient/patientDetails returns confirmed diagnoses under patientConfirmDiagnosis
            // (confirmDiagnosis can be null in that payload), and the list may contain null entries.
            // API values may be SNOMED display text (e.g. "Diabetes mellitus type 2 (disorder)")
            // while chips use local master names (e.g. "Diabetes Mellitus Type 2").
            val selectedDiagnosis = ArrayList<String>()
            (patientDetails?.patientConfirmDiagnosis ?: patientDetails?.confirmDiagnosis)
                ?.filterNotNull()
                ?.let { selectedDiagnosis.addAll(it) }
            autoPopulateDialogue(selectedDiagnosis)?.let {
                selectedDiagnosis.addAll(it)
            }

            val selectedChips = chipItems.filter { chip ->
                selectedDiagnosis.any { selected ->
                    EnrollmentObservationFormatter.matchesDiagnosisChip(
                        token = selected,
                        chipValue = chip.value,
                        chipName = chip.name,
                        diagnosisLookup = list,
                    )
                }
            }

            tagListCustomView.addChipItemList(chipItems, selectedChips, diagnosisMap)
            enableConfirm()
        }

        viewModel.confirmDiagnosisRequest.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> showLoading()
                ResourceState.SUCCESS -> {
                    hideLoading()
                    dismiss()
                    val confirmedDiagnoses = viewModel.confirmDiagnosisRequestData.confirmDiagnosis
                        ?.map { it.name }
                        ?.let { ArrayList(it) }
                    val diagnosisNotes = viewModel.confirmDiagnosisRequestData.diagnosisComments
                    commonDialogInterface?.onSuccess(confirmedDiagnoses, diagnosisNotes)
                    if (isSummary) {
                        medicalReviewBaseViewModel.medicalReViewRequest?.let {
                            viewModel.getPatientMedicalReviewSummary(requireContext(), it, true)
                        }
                    } else {
                        viewModel.getPatientDetails(requireContext(), false)
                    }
                    viewModel.confirmDiagnosisRequest.value = Resource(ResourceState.ERROR)
                }
                ResourceState.ERROR -> {
                    hideLoading()
                    resourceState?.message?.let { message ->
                        (activity as BaseActivity).showErrorDialogue(getString(R.string.error), message, false) {}
                        viewModel.confirmDiagnosisRequest.postError(null)
                    }
                }
            }
        }

        binding.etCommentDiagnosis.addTextChangedListener {
            if (it.isNullOrBlank()) {
                viewModel.confirmDiagnosisRequestData.diagnosisComments = null
            } else {
                viewModel.confirmDiagnosisRequestData.diagnosisComments = it.toString()
            }
        }
    }

    private fun validateEntityList(list: List<DiagnosisEntity>): List<DiagnosisEntity> = emptyList()

    private fun validateResponseList(responseList: List<DiagnosisEntity>): List<DiagnosisEntity> =
        if (patientDetails?.isPregnant == false) {
            responseList.filter { !it.diagnosis.contains(DefinedParams.GESTATIONAL_DIABETES) && it.type?.contains(DefinedParams.PREGNANCY_DIABETES) != true }
        } else {
            responseList
        }

    private fun autoPopulateDialogue(selectedDiagnosis: ArrayList<String>): ArrayList<String>? {
        if (isSummary) {
            medicalReviewBaseViewModel.medicalReviewEditModel.initialMedicalReview?.diagnosis?.let { data ->
                if ((data.htnPatientType == DefinedParams.KNOWN)) {
                    selectedDiagnosis.add(HYPERTENSION)
                }
                if (data.diabetesPatientType == DefinedParams.KNOWN) {
                    if (data.diabetesDiagControlledType == DefinedParams.PRE_DIABETIC) {
                        removeIfContainsDiagnosis(selectedDiagnosis, DefinedParams.DMT_ONE, DefinedParams.DMT_TWO, DefinedParams.GESTATIONAL_DIABETES)
                        selectedDiagnosis.add(DefinedParams.PRE_DIABETIC)
                    } else {
                        populateDiabetesDiagnosis(data.diabetesDiagnosis, selectedDiagnosis)
                    }
                }
            }
        }

        if (selectedDiagnosis.size > 0) {
            return selectedDiagnosis
        } else {
            return null
        }
    }

    private fun populateDiabetesDiagnosis(
        diabetesDiagnosis: String?,
        selectedDiagnosis: ArrayList<String>,
    ) {
        when (diabetesDiagnosis) {
            DefinedParams.TYPE_ONE -> {
                removeIfContainsDiagnosis(selectedDiagnosis, DefinedParams.DMT_TWO, DefinedParams.GESTATIONAL_DIABETES, DefinedParams.PRE_DIABETIC)
                selectedDiagnosis.add(DefinedParams.DMT_ONE)
            }
            DefinedParams.TYPE_TWO -> {
                removeIfContainsDiagnosis(selectedDiagnosis, DefinedParams.DMT_ONE, DefinedParams.GESTATIONAL_DIABETES, DefinedParams.PRE_DIABETIC)
                selectedDiagnosis.add(DefinedParams.DMT_TWO)
            }
            DefinedParams.GESTATIONAL_DIABETES -> {
                removeIfContainsDiagnosis(selectedDiagnosis, DefinedParams.DMT_ONE, DefinedParams.DMT_TWO, DefinedParams.PRE_DIABETIC)
                selectedDiagnosis.add(DefinedParams.GESTATIONAL_DIABETES)
            }
            else -> {
                diabetesDiagnosis?.let {
                    selectedDiagnosis.add(it)
                }
            }
        }
    }

    private fun removeIfContainsDiagnosis(
        selectedDiagnosis: ArrayList<String>,
        param1: String,
        param2: String,
        param3: String,
    ) {
        if (selectedDiagnosis.contains(param1)) {
            selectedDiagnosis.remove(param1)
        } else if (selectedDiagnosis.contains(param2)) {
            selectedDiagnosis.remove(param2)
        } else if (selectedDiagnosis.contains(param3)) {
            selectedDiagnosis.remove(param3)
        }
    }

    private fun saveDiagnosis() {
        if (validateInputs()) {
            patientDetails?.let {
                val selectedDiagnoses = tagListCustomView
                    .getSelectedTags()
                    .filterIsInstance<ChipViewItemModel>()
                    .map { chip ->
                        NCDDiagnosisItem(
                            type = chip.type,
                            value = chip.value ?: chip.name,
                            name = chip.name,
                        )
                    }
                val request = NCDDiagnosisRequestResponse(
                    provenanceDTO = ProvanceDto(),
                    confirmDiagnosis = selectedDiagnoses,
                    diagnosisNotes = viewModel.confirmDiagnosisRequestData.diagnosisComments,
                    patientReference = nurseViewModel.nurseMrRequestModel.patientReference,
                    memberReference = nurseViewModel.nurseMrRequestModel.memberReference,
                    type = NCDMRUtil.NCD,
                )
                viewModel.updateConfirmDiagnosis(requireContext(), request)
            }
        }
    }

    private fun validateInputs(): Boolean {
        var isValidInput = true
        if (viewModel.confirmDiagnosisRequestData.confirmDiagnosis == null ||
            (viewModel.confirmDiagnosisRequestData.confirmDiagnosis != null && viewModel.confirmDiagnosisRequestData.confirmDiagnosis!!.size <= 0)
        ) {
            binding.tvErrorMessage.visibility = View.VISIBLE
            isValidInput = false
        }
        return isValidInput
    }

    private fun showLoading() {
        binding.loadingProgress.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        binding.loadingProgress.visibility = View.GONE
    }

    private fun handleDiagnosisResponse(details: PatientDetailsModel?) {
        details?.let {
            patientDetails = it
            val list = arrayListOf<String>().apply {
            }

            val genderList = arrayListOf(it.gender!!.uppercase(), DefinedParams.BOTH.uppercase())
            medicalReviewBaseViewModel.getConfirmDiagnosisList(genderList, list)
            binding.etCommentDiagnosis.setText(it.diagnosisComments ?: "")
        }
    }
}
