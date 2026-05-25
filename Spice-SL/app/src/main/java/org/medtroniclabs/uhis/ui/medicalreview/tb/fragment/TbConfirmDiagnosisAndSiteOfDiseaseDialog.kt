package org.medtroniclabs.uhis.ui.medicalreview.tb.fragment

import android.content.res.Configuration
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.isGone
import org.medtroniclabs.uhis.appextensions.isVisible
import org.medtroniclabs.uhis.appextensions.setDialogPercent
import org.medtroniclabs.uhis.appextensions.setError
import org.medtroniclabs.uhis.appextensions.setVisible
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.DefinedParams.DrugSensitiveTB
import org.medtroniclabs.uhis.common.DefinedParams.ExtraPulmonary
import org.medtroniclabs.uhis.common.DefinedParams.OrganAffected
import org.medtroniclabs.uhis.common.DefinedParams.SITE_OF_DISEASE
import org.medtroniclabs.uhis.common.DefinedParams.TB
import org.medtroniclabs.uhis.data.DiagnosisDiseaseModel
import org.medtroniclabs.uhis.data.DiagnosisSaveUpdateRequest
import org.medtroniclabs.uhis.data.model.ChipViewItemModel
import org.medtroniclabs.uhis.data.offlinesync.model.ProvanceDto
import org.medtroniclabs.uhis.databinding.FragmentPatientStatusDialogBinding
import org.medtroniclabs.uhis.formgeneration.extension.markMandatory
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.model.medicalreview.CreateUnderTwoMonthsResponse
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.network.utils.ConnectivityManager
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.MenuConstants
import org.medtroniclabs.uhis.ui.TagListCustomView
import org.medtroniclabs.uhis.ui.medicalreview.motherneonate.anc.DialogDismissListenerForTb
import org.medtroniclabs.uhis.ui.medicalreview.motherneonate.anc.MotherNeonateUtil.TB_ORGAN_AFFECTED
import org.medtroniclabs.uhis.ui.medicalreview.motherneonate.anc.MotherNeonateUtil.TB_SITE_OF_DISEASE
import org.medtroniclabs.uhis.ui.medicalreview.tb.viewmodel.TbConfirmDiagnosisAndSiteOfDiseaseViewModel
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewTypeEnums
import org.medtroniclabs.uhis.ui.mypatients.viewmodel.PatientDetailViewModel
import javax.inject.Inject

@AndroidEntryPoint
class TbConfirmDiagnosisAndSiteOfDiseaseDialog : DialogFragment(), View.OnClickListener {
    var listener: DialogDismissListenerForTb? = null
    private lateinit var binding: FragmentPatientStatusDialogBinding
    private val diagnosisViewModel: TbConfirmDiagnosisAndSiteOfDiseaseViewModel by activityViewModels()
    private val patientViewModel: PatientDetailViewModel by activityViewModels()
    private lateinit var diseaseConfirmCategoryTagView: TagListCustomView
    private lateinit var siteDiseaseCategoryTagView: TagListCustomView
    private lateinit var organAffectedTagView: TagListCustomView

    @Inject
    lateinit var connectivityManager: ConnectivityManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentPatientStatusDialogBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        isCancelable = false
        return binding.root
    }

    companion object {
        const val TAG = "TbConfirmDiagnosisAndSiteOfDiseaseDialog"

        fun newInstance() = TbConfirmDiagnosisAndSiteOfDiseaseDialog()
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        attachObservers()
        setListener()
        initTag()
    }

    private fun setListener() {
        binding.etOtherDiagnosisNotes.doAfterTextChanged {
            enableBtn()
        }
    }

    private fun initTag() {
        diseaseConfirmCategoryTagView = TagListCustomView(
            binding.root.context,
            binding.diagnosisChip,
        ) { _, _, _ ->
            enableBtn()
        }

        siteDiseaseCategoryTagView = TagListCustomView(
            binding.root.context,
            binding.siteChip,
        ) { _, _, _ ->
            enableBtn()
        }
        organAffectedTagView = TagListCustomView(
            binding.root.context,
            binding.organChip,
        ) { _, _, _ ->
            val selectedTags = organAffectedTagView.getSelectedTags()
            val hasOther = selectedTags.any { it.value.equals(DefinedParams.OTHER, ignoreCase = true) }

            enableBtn()
            showOtherNotes(hasOther)
            if (!hasOther && binding.etOtherDiagnosisNotes.isGone()) {
                setOtherDiagnosisTextSafely("")
            }
        }
    }

    private fun showOtherNotes(isShow: Boolean) {
        binding.etOtherDiagnosisNotes.setVisible(isShow)
    }

    private fun enableBtn() {
        val siteTags = siteDiseaseCategoryTagView.getSelectedTags()
        val confirmTags = diseaseConfirmCategoryTagView.getSelectedTags()
        val organTags = organAffectedTagView.getSelectedTags()

        val hasSiteTags = siteTags.isNotEmpty()
        val hasConfirmTags = confirmTags.isNotEmpty()
        val hasOrganTags = organTags.isNotEmpty()
        val hasDrugSensitiveTB = confirmTags.any { it.value == DrugSensitiveTB }
        val hasExtraPulmonary = siteTags.any { it.value == ExtraPulmonary }
        val hasOtherOrgan = organTags.any { it.value.equals(DefinedParams.OTHER, true) }
        val otherNotesFilled = binding.etOtherDiagnosisNotes.text
            ?.toString()
            ?.trim()
            ?.isNotBlank() == true

        val validOrganSelection = hasOrganTags &&
            (!hasOtherOrgan || (hasOtherOrgan && otherNotesFilled))

        val validDrugSensitiveTB = (!hasExtraPulmonary || (hasExtraPulmonary && validOrganSelection))

        if (hasSiteTags && hasExtraPulmonary) {
            if (binding.organChip.isGone()) {
                binding.organChip.visible()
                binding.tvOrganLbl.visible()
                binding.tvOrganError.gone()
            }
        } else {
            if (binding.organChip.isVisible()) {
                binding.organChip.gone()
                binding.tvOrganLbl.gone()
                binding.tvOrganError.gone()
                setOtherDiagnosisTextSafely("")

                binding.etOtherDiagnosisNotes.gone()
                organAffectedTagView.clearOtherChip()
                organAffectedTagView.clearSelection()
            }
        }
        binding.btnOkay.isEnabled = hasSiteTags && hasConfirmTags && validDrugSensitiveTB
    }

    private fun setOtherDiagnosisTextSafely(text: String) {
        with(binding.etOtherDiagnosisNotes) {
            removeTextChangedListener(defaultOtherDiagnosisWatcher)
            setText(text)
            addTextChangedListener(defaultOtherDiagnosisWatcher)
        }
    }

    private val defaultOtherDiagnosisWatcher = object : TextWatcher {
        override fun beforeTextChanged(
            s: CharSequence?,
            start: Int,
            count: Int,
            after: Int,
        ) {}

        override fun onTextChanged(
            s: CharSequence?,
            start: Int,
            before: Int,
            count: Int,
        ) {
            enableBtn()
        }

        override fun afterTextChanged(s: Editable?) {}
    }

    private fun attachObservers() {
        diagnosisViewModel.diagnosisSaveUpdateResponse.observe(viewLifecycleOwner) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    binding.loader.visible()
                }

                ResourceState.SUCCESS -> {
                    binding.loader.gone()
                    diagnosisViewModel.diagnosisSaveUpdateResponse.setError()
                    listener?.onDialogDismissedForTb(isPatientType = false)
                    dismiss()
                }

                ResourceState.ERROR -> {
                    binding.loader.gone()
                }
            }
        }
        diagnosisViewModel.diagnosisMetaList.observe(viewLifecycleOwner) { resource ->
            if (resource.state == ResourceState.SUCCESS) {
                diagnosisViewModel.getSiteOfDiseaseMetaList(TB_SITE_OF_DISEASE)
            }
        }
        diagnosisViewModel.organAffectedMetaList.observe(viewLifecycleOwner) { resource ->
            if (resource.state == ResourceState.SUCCESS) {
                val listItems = resource.data ?: return@observe
                val patientId = patientViewModel.patientDetailsLiveData.value
                    ?.data
                    ?.id
                if (patientId != null) {
                    diagnosisViewModel.getDiagnosisDetails(
                        CreateUnderTwoMonthsResponse(
                            patientReference = patientId,
                            type = MenuConstants.TB_MENU_ID,
                        ),
                    )
                } else {
                    val organMetaChipItems = listItems.map {
                        ChipViewItemModel(it.id, it.name, value = it.value)
                    }

                    val diagnosisMetaChipItems =
                        diagnosisViewModel.diagnosisMetaList.value?.data?.map {
                            ChipViewItemModel(it.id, it.name, value = it.value)
                        } ?: emptyList()
                    val siteMetaChipItems =
                        diagnosisViewModel.siteOfDiseaseMetaList.value?.data?.map {
                            ChipViewItemModel(it.id, it.name, value = it.value)
                        } ?: emptyList()
                    siteDiseaseCategoryTagView.addChipItemList(siteMetaChipItems, null)
                    organAffectedTagView.addChipItemList(organMetaChipItems, null)
                    diseaseConfirmCategoryTagView.addChipItemList(diagnosisMetaChipItems, null)
                }
            }
        }

        diagnosisViewModel.siteOfDiseaseMetaList.observe(viewLifecycleOwner) { resource ->
            if (resource.state == ResourceState.SUCCESS) {
                diagnosisViewModel.getOrganAffectedMetaList(TB_ORGAN_AFFECTED)
            }
        }

        diagnosisViewModel.diagnosisDetailsList.observe(viewLifecycleOwner) { resource ->
            binding.loader.visible()
            if (resource.state == ResourceState.SUCCESS) {
                binding.loader.gone()
                val listItems = resource.data ?: return@observe

                val confirmMetaList = diagnosisViewModel.diagnosisMetaList.value?.data?.map {
                    ChipViewItemModel(it.id, it.name, value = it.value)
                } ?: emptyList()

                val siteMetaList = diagnosisViewModel.siteOfDiseaseMetaList.value?.data?.map {
                    ChipViewItemModel(it.id, it.name, value = it.value)
                } ?: emptyList()

                val organMetaList = diagnosisViewModel.organAffectedMetaList.value?.data?.map {
                    ChipViewItemModel(it.id, it.name, value = it.value)
                } ?: emptyList()

                val siteGetItem = listItems.filter { it.type == SITE_OF_DISEASE }
                val confirmGetItem = listItems.filter { it.type.isNullOrBlank() || it.type == TB.uppercase() }
                val organGetItem = listItems.filter { it.type == OrganAffected }

                val filteredConfirmList = confirmMetaList.filter { diagnosis ->
                    confirmGetItem.any { it.diseaseCategory == diagnosis.value }
                }

                val filteredSiteList = siteMetaList.filter { diagnosis ->
                    siteGetItem.any { it.diseaseCategory == diagnosis.value }
                }

                val filteredOrganList = organMetaList.filter { diagnosis ->
                    organGetItem.any { it.diseaseCategory == diagnosis.value }
                }
                siteDiseaseCategoryTagView.addChipItemList(siteMetaList, filteredSiteList)
                organAffectedTagView.addChipItemList(organMetaList, filteredOrganList)
                diseaseConfirmCategoryTagView.addChipItemList(confirmMetaList, filteredConfirmList)

                if (filteredOrganList.isNotEmpty()) {
                    if (binding.organChip.isGone()) {
                        binding.apply {
                            organChip.visible()
                            tvOrganLbl.visible()
                            tvOrganError.gone()
                        }
                    }
                    val hasOther = filteredOrganList.any { it.value.equals(DefinedParams.OTHER, ignoreCase = true) }
                    val otherNotesItem = listItems.firstOrNull { it.diseaseCategory.equals(DefinedParams.OTHER_NOTES, ignoreCase = true) }

                    if (hasOther && otherNotesItem?.diseaseCondition?.isNotBlank() == true) {
                        binding.etOtherDiagnosisNotes.setText(otherNotesItem.diseaseCondition)
                    }
                } else {
                    if (binding.organChip.isVisible()) {
                        binding.apply {
                            organChip.gone()
                            tvOrganLbl.gone()
                            tvOrganError.gone()
                            etOtherDiagnosisNotes.setText("")
                            etOtherDiagnosisNotes.gone()
                        }
                    }
                }
            } else {
                binding.loader.gone()
                val siteMetaChipItems = diagnosisViewModel.siteOfDiseaseMetaList.value?.data?.map {
                    ChipViewItemModel(it.id, it.name, value = it.value)
                } ?: emptyList()

                val diagnosisMetaChipItems = diagnosisViewModel.diagnosisMetaList.value?.data?.map {
                    ChipViewItemModel(it.id, it.name, value = it.value)
                } ?: emptyList()

                val organMetaChipItems = diagnosisViewModel.organAffectedMetaList.value?.data?.map {
                    ChipViewItemModel(it.id, it.name, value = it.value)
                } ?: emptyList()

                siteDiseaseCategoryTagView.addChipItemList(siteMetaChipItems, null)
                organAffectedTagView.addChipItemList(organMetaChipItems, null)
                diseaseConfirmCategoryTagView.addChipItemList(diagnosisMetaChipItems, null)
            }
        }
    }

    private fun initView() {
        diagnosisViewModel.getDiagnosisMetaList(MedicalReviewTypeEnums.TB.name)
        with(binding) {
            tvDiagnosisLbl.visible()
            tvSiteError.gone()
            tvSiteLbl.text = getString(R.string.site_of_disease)
            tvSiteLbl.markMandatory()
            tvOrganLbl.markMandatory()
            tvSiteLbl.visible()
            siteChip.visible()
            tvDiagnosisError.gone()
            tvTitle.text = getString(R.string.confirm_diagnoses)
            tvDiagnosisLbl.text = getString(R.string.diagnosis_tb)
            tvDiagnosisLbl.markMandatory()
            btnOkay.text = getString(R.string.save).uppercase()
            btnCancel.safeClickListener(this@TbConfirmDiagnosisAndSiteOfDiseaseDialog)
            btnOkay.safeClickListener(this@TbConfirmDiagnosisAndSiteOfDiseaseDialog)
            ivClose.safeClickListener(this@TbConfirmDiagnosisAndSiteOfDiseaseDialog)
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.btnOkay.id -> handleOkayClick()
            binding.btnCancel.id, binding.ivClose.id -> dismiss()
        }
    }

    private fun handleOkayClick() {
        if (connectivityManager.isNetworkAvailable()) {
            patientViewModel.patientDetailsLiveData.value?.data?.let { details ->
                details.patientId?.let { patientId ->
                    val request = DiagnosisSaveUpdateRequest(
                        patientId = patientId,
                        patientReference = details.id,
                        diseases = getDiagnosisDiseaseList().toCollection(arrayListOf()),
                        provenance = ProvanceDto(),
                        type = MenuConstants.TB_MENU_ID.uppercase(),
                        otherNotes = binding.etOtherDiagnosisNotes.text
                            .toString()
                            .ifBlank { null },
                    )
                    diagnosisViewModel.diagnosisCreate(request)
                }
            }
        } else {
            (activity as? BaseActivity)?.showErrorDialogue(
                getString(R.string.error),
                getString(R.string.no_internet_error),
                isNegativeButtonNeed = false,
            ) {
            }
        }
    }

    private fun getDiagnosisDiseaseList(): List<DiagnosisDiseaseModel> =
        (
            siteDiseaseCategoryTagView.getSelectedTags().map {
                DiagnosisDiseaseModel(
                    diseaseCategoryId = it.id ?: -1L,
                    diseaseCategory = it.value ?: "",
                    diseaseConditionId = null,
                    diseaseCondition = null,
                    type = SITE_OF_DISEASE, // Set true for siteDiseaseCategoryTagView
                )
            } + diseaseConfirmCategoryTagView.getSelectedTags().map {
                DiagnosisDiseaseModel(
                    diseaseCategoryId = it.id ?: -1L,
                    diseaseCategory = it.value ?: "",
                    diseaseConditionId = null,
                    diseaseCondition = null,
                    type = null, // Set false for diseaseConfirmCategoryTagView
                )
            } + organAffectedTagView.getSelectedTags().map {
                DiagnosisDiseaseModel(
                    diseaseCategoryId = it.id ?: -1L,
                    diseaseCategory = it.value ?: "",
                    diseaseConditionId = null,
                    diseaseCondition = null,
                    type = OrganAffected, // Set false for diseaseConfirmCategoryTagView
                )
            }
        )

    private fun handleDialogSize() {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val width = if (CommonUtils.checkIsTablet(requireContext())) {
            if (isLandscape) 65 else 90
        } else {
            if (isLandscape) 65 else 90
        }
        setDialogPercent(width)
    }

    override fun onStart() {
        super.onStart()
        handleDialogSize()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        handleDialogSize()
    }
}
