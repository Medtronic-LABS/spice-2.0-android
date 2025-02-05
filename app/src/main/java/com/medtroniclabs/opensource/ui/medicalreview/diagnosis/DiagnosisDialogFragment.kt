package com.medtroniclabs.opensource.ui.medicalreview.diagnosis

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.setError
import com.medtroniclabs.opensource.appextensions.setWidth
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.DefinedParams.Other
import com.medtroniclabs.opensource.common.DefinedParams.OtherNotes
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.data.DiagnosisDiseaseModel
import com.medtroniclabs.opensource.data.DiagnosisSaveUpdateRequest
import com.medtroniclabs.opensource.data.DiseaseCategoryItems
import com.medtroniclabs.opensource.data.model.ChipViewItemModel
import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto
import com.medtroniclabs.opensource.databinding.FragmentDiagnosisDialogBinding
import com.medtroniclabs.opensource.formgeneration.DiagnosisGenerator
import com.medtroniclabs.opensource.formgeneration.DiagnosisListener
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.model.medicalreview.CreateUnderTwoMonthsResponse
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.network.utils.ConnectivityManager
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.TagListCustomView
import com.medtroniclabs.opensource.ui.medicalreview.diagnosis.viewmodel.DiagnosisViewModel
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewTypeEnums
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.PatientDetailViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DiagnosisDialogFragment : DialogFragment(), View.OnClickListener, DiagnosisListener {

    private lateinit var binding: FragmentDiagnosisDialogBinding
    private lateinit var diagnosisGenerator: DiagnosisGenerator
    private lateinit var diseaseCategoryTagView: TagListCustomView
    private val patientViewModel: PatientDetailViewModel by activityViewModels()
    private val diagnosisViewModel: DiagnosisViewModel by activityViewModels()

    @Inject
    lateinit var connectivityManager: ConnectivityManager

    companion object {
        const val TAG: String = "DiagnosisDialogueFragment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentDiagnosisDialogBinding.inflate(layoutInflater, container, false)
        val window: Window? = dialog?.window
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        setListener()
        attachObserver()
    }

    private fun setListener() {
        binding.btnCancel.safeClickListener(this)
        binding.ivClose.safeClickListener(this)
        binding.btnOkay.safeClickListener(this)
        binding.loadingProgress.safeClickListener(this)

        binding.etOtherDiagnosisNotes.addTextChangedListener {
            if (!it.isNullOrEmpty() && !binding.btnOkay.isEnabled){
                binding.btnOkay.isEnabled = true
            }
        }
    }

    private fun attachObserver() {
        diagnosisViewModel.diagnosisMetaList.observe(viewLifecycleOwner) { resource ->
            when (resource.state) {
                ResourceState.SUCCESS -> {
                    resource.data?.let { listItems ->
                        patientViewModel.patientDetailsLiveData.value?.data?.let { details ->
                            details.id?.let { id ->
                                diagnosisViewModel.getDiagnosisDetails(
                                    CreateUnderTwoMonthsResponse(
                                        patientReference = id,
                                        type = diagnosisViewModel.diagnosisType
                                    )
                                )
                            } ?: kotlin.run {
                                val diagnosisMetaChipItemList = ArrayList<ChipViewItemModel>()
                                listItems.forEach {
                                    diagnosisMetaChipItemList.add(
                                        ChipViewItemModel(
                                            id = it.id, name = it.name, value = it.value
                                        )
                                    )
                                }
                                diseaseCategoryTagView.addChipItemList(
                                    diagnosisMetaChipItemList,
                                    null
                                )
                                diagnosisViewModel.viewDiagnosis = false
                            }
                        }
                    }
                }

                else -> {

                }
            }
        }

        diagnosisViewModel.diagnosisDetailsList.observe(viewLifecycleOwner) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    resource.data?.let { listItems ->
                        if (diagnosisViewModel.viewDiagnosis) {
                            binding.llFamilyRoot.removeAllViews()
                            diagnosisViewModel.diagnosisMetaList.value?.data?.let { diagnosisList ->
                                /**
                                 * From backend diagnosis items respective ID and its FHIR value only be given,
                                 * We need to compare it with our meta items and filter respective items to render
                                 * and auto-populate the respective chip Items
                                 *
                                 * diagnosisAccordionList - Collecting list of accordions which has been already selected
                                 *                          by comparing diseaseCategory and it value
                                 * diagnosisMetaChipItemList - It is to render the Parent Chip item Disease Category
                                 * selectedDiagnosisMetaChipItemList - It is to collect overall selected parent chip item list
                                 * selectedDiseaseConditionItemList - It is to select chips of disease conditions which is inside accordion of respective group
                                 */
                                binding.etOtherDiagnosisNotes.setText(listItems.firstOrNull {
                                    it.diseaseCategory.equals(
                                        OtherNotes,
                                        true
                                    )
                                }?.diseaseCondition.takeIf { !it.isNullOrBlank() })

                                val diagnosisAccordionList = ArrayList<DiseaseCategoryItems>()
                                for (diagnosis in diagnosisList) {
                                    if (listItems.any { it.diseaseCategory == diagnosis.value }) {
                                        diagnosisAccordionList.add(diagnosis)
                                    }
                                }

                                val diagnosisMetaChipItemList = ArrayList<ChipViewItemModel>()
                                diagnosisList.forEach {
                                    diagnosisMetaChipItemList.add(
                                        ChipViewItemModel(
                                            id = it.id, name = it.name, value = it.value
                                        )
                                    )
                                }

                                val selectedDiagnosisMetaChipItemList =
                                    ArrayList<ChipViewItemModel>()
                                diagnosisAccordionList.forEach {
                                    selectedDiagnosisMetaChipItemList.add(
                                            ChipViewItemModel(
                                                id = it.id, name = it.name, value = it.value
                                            )
                                        )
                                }

                                val selectedDiseaseConditionItemList = ArrayList<ChipViewItemModel>()
                                listItems.forEach {
                                    val nameItemValue =
                                        diagnosisList.flatMap { item -> item.diseaseCondition }
                                            .find { conditionItem -> conditionItem.value == it.diseaseCondition }?.name
                                    nameItemValue?.let { value ->
                                        if (value.lowercase() != Other.lowercase() || !it.diseaseCategory.equals(Other, true)) {
                                            selectedDiseaseConditionItemList.add(
                                                ChipViewItemModel(
                                                    id = it.diseaseConditionId, name = value
                                                )
                                            )
                                        }
                                    }
                                }

                                diseaseCategoryTagView.addChipItemList(
                                    diagnosisMetaChipItemList,
                                    selectedDiagnosisMetaChipItemList
                                )
                                diagnosisAccordionList.removeAll{it.name.lowercase() == Other.lowercase()}
                                diagnosisGenerator.populateDiagnosisView(
                                    diagnosisAccordionList,
                                    selectedDiseaseConditionItemList
                                )
                            }
                        }
                    } ?: kotlin.run {
                        diagnosisViewModel.diagnosisMetaList.value?.data?.let { listItems ->
                            val chipItemList = ArrayList<ChipViewItemModel>()
                            listItems.forEach {
                                chipItemList.add(
                                    ChipViewItemModel(
                                        id = it.id, name = it.name, value = it.value
                                    )
                                )
                            }
                            diseaseCategoryTagView.addChipItemList(chipItemList, null)
                        }
                    }
                    diagnosisViewModel.viewDiagnosis = false
                    hideLoading()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }
            }
        }

        diagnosisViewModel.diagnosisSaveUpdateResponse.observe(viewLifecycleOwner) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    diagnosisViewModel.viewDiagnosis = true
                    patientViewModel.patientDetailsLiveData.value?.data?.let {details ->
                        details.id?.let {id ->
                            getDiagnosisDetails(id)
                        } ?: kotlin.run {
                            details.patientId?.let { patientViewModel.getPatients(it, origin = null) }
                        }
                    }
                    diagnosisViewModel.diagnosisSaveUpdateResponse.setError()
                    hideLoading()
                    dismiss()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }
            }
        }
    }

    private fun getDiagnosisDetails(id: String) {
        diagnosisViewModel.getDiagnosisDetails(
            CreateUnderTwoMonthsResponse(
                patientReference = id,
                type = diagnosisViewModel.diagnosisType
            )
        )
    }

    private fun initView() {
        handleVisibility()
        diagnosisGenerator = DiagnosisGenerator(binding.root.context, binding.llFamilyRoot, this, translate = SecuredPreference.getIsTranslationEnabled())
        diseaseCategoryTagView = TagListCustomView(
            binding.root.context, binding.diseaseConditionChipGroup
        ) { name, _, isChecked ->
            if (!diagnosisViewModel.viewDiagnosis && isShowAccordion() && name?.lowercase() != Other.lowercase() ) {
                diagnosisViewModel.diagnosisMetaList.value?.data?.let { listItems ->
                    if (isChecked) {
                        val filteredList =
                            listItems.filter { it.name.lowercase() == name?.lowercase() }
                        diagnosisGenerator.populateDiagnosisView(filteredList, null)
                    } else {
                        name?.let {
                            diagnosisGenerator.removeViewByTag(it)
                        }
                    }
                }
            }
            saveBtnStateHandler()
            handleSelectedDiseaseCategory()
        }
        diagnosisGenerator.setDiagnosisCallback(this)
    }

    private fun handleSelectedDiseaseCategory() {
        when (diagnosisViewModel.diagnosisType) {
            MedicalReviewTypeEnums.ABOVE_FIVE_YEARS.name -> {
                if (diseaseCategoryTagView.getSelectedTags().isEmpty()) {
                    binding.tvSelectedDiseaseConditionLbl.gone()
                } else if (diseaseCategoryTagView.getSelectedTags().size > 1) {
                    binding.tvSelectedDiseaseConditionLbl.visible()
                } else {
                    val list =
                        diseaseCategoryTagView.getSelectedTags()
                            .filter { it.name.equals(Other, true) }
                    if (list.isNotEmpty()) {
                        binding.tvSelectedDiseaseConditionLbl.gone()
                    } else {
                        binding.tvSelectedDiseaseConditionLbl.visible()
                    }
                }
            }
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.btnCancel.id, binding.ivClose.id -> {
                diagnosisViewModel.viewDiagnosis = true
                dismiss()
            }

            binding.btnOkay.id -> {
                if (connectivityManager.isNetworkAvailable()){
                    patientViewModel.patientDetailsLiveData.value?.data?.let { details ->
                        details.patientId?.let { patientId ->
                            val request = DiagnosisSaveUpdateRequest(
                                patientId = patientId,
                                patientReference = details.id,
                                diseases = getDiagnosisDiseaseList(),
                                provenance = ProvanceDto(),
                                otherNotes = binding.etOtherDiagnosisNotes.text?.trim()?.takeIf { it.isNotEmpty() }?.toString(),
                                type = diagnosisViewModel.diagnosisType
                            )
                            diagnosisViewModel.diagnosisCreate(request)
                        }
                    }
                } else {
                    (activity as? BaseActivity)?.showErrorDialogue(
                        getString(R.string.error),
                        getString(R.string.no_internet_error),
                        isNegativeButtonNeed = false
                    ) {

                    }
                }
            }

            binding.loadingProgress.id -> {}
        }
    }

    private fun getDiagnosisDiseaseList(): ArrayList<DiagnosisDiseaseModel> {
        /**
         * This method is to compose request for save and update diagnosis
         */
        val diagnosisList = ArrayList<DiagnosisDiseaseModel>()
        if (isShowAccordion()) {
            diagnosisGenerator.getSelectedTagsForAccordions().let { selectedAccordionMap ->
                for ((diseaseCategoryValue, chipViewItemList) in selectedAccordionMap) {
                    for (chipViewItem in chipViewItemList) {
                        chipViewItem.value?.let { value ->
                            diagnosisViewModel.diagnosisMetaList.value?.data?.filter { item -> item.name == diseaseCategoryValue }
                                ?.let { categoryItem ->
                                    val diagnosisDiseaseModel = DiagnosisDiseaseModel(
                                        diseaseCategoryId = getDiseaseCategoryId(
                                            diseaseCategoryValue
                                        ),
                                        diseaseConditionId = chipViewItem.id ?: -1,
                                        diseaseCategory = categoryItem[0].value,
                                        diseaseCondition = value
                                    )
                                    diagnosisList.add(diagnosisDiseaseModel)
                                }
                        }
                    }
                }
            }
        } else {
            diseaseCategoryTagView.getSelectedTags().let { list ->
                list.forEach { item ->
                    item.id?.let {
                        item.value?.let {
                            diagnosisList.add(
                                DiagnosisDiseaseModel(
                                    diseaseCategoryId = item.id,
                                    diseaseConditionId = null,
                                    diseaseCategory = item.value,
                                    diseaseCondition = null
                                )
                            )
                        }
                    }
                }
            }
        }
        diseaseCategoryTagView.getSelectedTags().let { list ->
            val otherItem = list.filter { item -> item.value?.lowercase() == Other.lowercase() }
            if (otherItem.isNotEmpty() && diagnosisViewModel.diagnosisType == MedicalReviewTypeEnums.ABOVE_FIVE_YEARS.name) {
                otherItem[0].id?.let { otherId ->
                    diagnosisList.add(
                        DiagnosisDiseaseModel(
                            diseaseCategoryId = otherId,
                            diseaseConditionId = null,
                            diseaseCategory = otherItem[0].value ?: otherItem[0].name,
                            diseaseCondition = null
                        )
                    )
                }
            }
        }
        return diagnosisList
    }

    private fun getDiseaseCategoryId(diseaseCategoryName: String): Long {
        return diagnosisViewModel.diagnosisMetaList.value?.data?.let { list ->
            list.filter { it.name == diseaseCategoryName }[0].id
        } ?: -1L
    }

    override fun onStart() {
        super.onStart()
        handleDialogSize()
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

    private fun showLoading() {
        binding.loadingProgress.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        binding.loadingProgress.visibility = View.GONE
    }

    private fun isShowAccordion(): Boolean {
        return when (diagnosisViewModel.diagnosisType) {
            MedicalReviewTypeEnums.ANC_REVIEW.name, MedicalReviewTypeEnums.PNC_MOTHER_REVIEW.name, MedicalReviewTypeEnums.UNDER_TWO_MONTHS.name, MedicalReviewTypeEnums.UNDER_FIVE_YEARS.name -> {
                false
            }

            else -> {
                true
            }
        }
    }

    private fun handleVisibility() {
        when(diagnosisViewModel.diagnosisType) {
            MedicalReviewTypeEnums.ANC_REVIEW.name -> {
                binding.tvSelectedDiseaseCategoryLbl.text = requireContext().getString(R.string.select_diagnosis_found_on_the_patient)
                binding.tvSelectedDiseaseConditionLbl.gone()
                binding.llFamilyRoot.gone()
            }
            MedicalReviewTypeEnums.UNDER_TWO_MONTHS.name, MedicalReviewTypeEnums.UNDER_FIVE_YEARS.name ,MedicalReviewTypeEnums.PNC_MOTHER_REVIEW.name-> {
                binding.tvSelectedDiseaseConditionLbl.gone()
                binding.tvSelectedDiseaseCategoryLbl.gone()
                binding.etOtherDiagnosisNotes.visible()
                binding.llFamilyRoot.gone()
            }
        }
    }

    override fun onDiagnosisSelection(isEmptyOrNot : Boolean) {
        val previousSelectedItems = diagnosisViewModel.diagnosisDetailsList.value?.data?.let {
            it.isNotEmpty()
        }
        val otherSelection = diseaseCategoryTagView.getSelectedTags()
            .any { it.name.lowercase() == Other.lowercase() }
        binding.btnOkay.isEnabled = isEmptyOrNot|| (previousSelectedItems == true && diagnosisGenerator.isEmptyAccordion()) || otherSelection
    }

    private fun saveBtnStateHandler() {
        val otherSelection = diseaseCategoryTagView.getSelectedTags()
            .any { it.name.lowercase() == Other.lowercase() }
        when (diagnosisViewModel.diagnosisType) {
            MedicalReviewTypeEnums.ABOVE_FIVE_YEARS.name -> {
                diagnosisViewModel.diagnosisDetailsList.value?.data?.let {
                    if ((diseaseCategoryTagView.getSelectedTags().size <= 1) || otherSelection) {
                        binding.btnOkay.isEnabled =
                            (it.isNotEmpty() && diseaseCategoryTagView.getSelectedTags()
                                .isEmpty()) || otherSelection || diagnosisGenerator.isAccordionNotEmpty()
                    } else {
                        if (diagnosisViewModel.diagnosisType == MedicalReviewTypeEnums.ABOVE_FIVE_YEARS.name) {
                            binding.btnOkay.isEnabled = diagnosisGenerator.isAccordionNotEmpty()
                        }
                    }
                } ?: kotlin.run {
                    binding.btnOkay.isEnabled = otherSelection
                }
            }

            MedicalReviewTypeEnums.ANC_REVIEW.name, MedicalReviewTypeEnums.UNDER_FIVE_YEARS.name, MedicalReviewTypeEnums.UNDER_TWO_MONTHS.name,
            MedicalReviewTypeEnums.PNC_MOTHER_REVIEW.name -> {
                if (diseaseCategoryTagView.getSelectedTags().isNotEmpty() || otherSelection) {
                    binding.btnOkay.isEnabled = true
                } else {
                    binding.btnOkay.isEnabled =
                        diagnosisViewModel.diagnosisDetailsList.value?.data?.let { list ->
                            list.size > 0
                        } ?: false
                }
            }
        }
    }
}