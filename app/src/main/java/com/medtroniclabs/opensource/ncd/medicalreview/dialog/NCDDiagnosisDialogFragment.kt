package com.medtroniclabs.opensource.ncd.medicalreview.dialog

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.invisible
import com.medtroniclabs.opensource.appextensions.loadAsGif
import com.medtroniclabs.opensource.appextensions.resetImageView
import com.medtroniclabs.opensource.appextensions.setDialogPercent
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.data.model.ChipViewItemModel
import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto
import com.medtroniclabs.opensource.databinding.DialogNcdDiagnosisBinding
import com.medtroniclabs.opensource.db.entity.NCDDiagnosisEntity
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.mappingkey.Screening
import com.medtroniclabs.opensource.ncd.data.NCDDiagnosisGetRequest
import com.medtroniclabs.opensource.ncd.data.NCDDiagnosisItem
import com.medtroniclabs.opensource.ncd.data.NCDDiagnosisRequestResponse
import com.medtroniclabs.opensource.ncd.medicalreview.NCDDialogDismissListener
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil.CONFIRM_DIAGNOSIS_TYPE
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil.CONFIRM_DIAGNOSIS_TYPE_GET
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil.IsPregnant
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil.MENU_Name
import com.medtroniclabs.opensource.ncd.medicalreview.viewmodel.NCDDiagnosisViewModel
import com.medtroniclabs.opensource.ncd.medicalreview.viewmodel.NCDMedicalReviewViewModel
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.TagListCustomView
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.MotherNeonateUtil
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NCDDiagnosisDialogFragment : DialogFragment(), View.OnClickListener {
    private lateinit var binding: DialogNcdDiagnosisBinding
    private val viewModel: NCDDiagnosisViewModel by viewModels()
    private val ncdMedicalReviewViewModel: NCDMedicalReviewViewModel by viewModels()
    private lateinit var tagListCustomView: TagListCustomView
    var listener: NCDDialogDismissListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogNcdDiagnosisBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        isCancelable = false
        return binding.root
    }

    companion object {
        const val TAG = "NCDDiagnosisDialogFragment"
        fun newInstance(
            patientId: String?,
            memberId: String?,
            types: ArrayList<String>,
            isFemale: Boolean,
            getTypes: ArrayList<String>,
            isPregnant: Boolean,
            isDiagnosisMismatch: Boolean,
            type: String? = null,
            menuName:String? = null
        ) = NCDDiagnosisDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(DefinedParams.PatientId, patientId)
                    putString(DefinedParams.MemberID, memberId)
                    putStringArrayList(CONFIRM_DIAGNOSIS_TYPE, types)
                    putStringArrayList(CONFIRM_DIAGNOSIS_TYPE_GET, getTypes)
                    putBoolean(NCDMRUtil.IS_FEMALE, isFemale)
                    putBoolean(NCDMRUtil.IS_DIAGNOSIS_MISMATCH, isDiagnosisMismatch)
                    putBoolean(IsPregnant, isPregnant)
                    putString(Screening.Type,type)
                    putString(MENU_Name, menuName)
                }
            }
    }

    private fun getTypes(): ArrayList<String> {
        return arguments?.getStringArrayList(CONFIRM_DIAGNOSIS_TYPE) ?: arrayListOf()
    }

    private fun getConfirmDiagnosisTypes(): ArrayList<String> {
        return arguments?.getStringArrayList(CONFIRM_DIAGNOSIS_TYPE_GET) ?: arrayListOf()
    }

    private fun isPregnant(): Boolean {
        return arguments?.getBoolean(IsPregnant) ?: false
    }

    private fun isDiagnosisMismatch(): Boolean {
        return arguments?.getBoolean(NCDMRUtil.IS_DIAGNOSIS_MISMATCH) ?: false
    }

    private fun getGender(): String {
        return if (arguments?.getBoolean(NCDMRUtil.IS_FEMALE) == true) {
            Screening.Female.lowercase()
        } else {
            Screening.Male.lowercase()
        }
    }

    fun getPatientId(): String? {
        return arguments?.getString(DefinedParams.PatientId)
    }
    fun getMemberId(): String? {
        return arguments?.getString(DefinedParams.MemberID)
    }
    private fun getTypeForRequest(): String? {
        return NCDMRUtil.requestTypeForConfirmDiagnoses(arguments?.getString(Screening.Type))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        attachObservers()
    }

    private fun attachObservers() {
        ncdMedicalReviewViewModel.ncdPatientDiagnosisStatus.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    confirmDiagnosis()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }
            }
        }
        viewModel.getChipLiveData.observe(viewLifecycleOwner) {
            setChipItems(it)
            getDiagonsis()
        }

        viewModel.createConfirmDiagonsis.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    dismiss()
                    listener?.onDialogDismissed(true)
                }

                ResourceState.ERROR -> {
                    // error dialog
                    hideLoading()
                }
            }
        }

        viewModel.getConfirmDiagonsis.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    resourceState.data?.let { data ->
                        viewModel.getChipLiveData.value?.let { liveData ->
                            data.diagnosis?.mapNotNull { it.value }?.let { values ->
                                val filteredChips = if (isDiagnosisMismatch()) {
                                    ncdMedicalReviewViewModel.ncdPatientDiagnosisStatus.value?.data?.let { patientDiagnosisMap ->
                                        (patientDiagnosisMap[NCDMRUtil.NCDPatientStatus] as? Map<*, *>)?.let { patientStatusMap ->
                                            val patientDiagnosis = ArrayList<String>()
                                            (patientStatusMap[NCDMRUtil.DiabetesControlledType] as? String)?.let { dia ->
                                                patientDiagnosis.add(dia)
                                            }
                                            (patientStatusMap[NCDMRUtil.HypertensionStatus] as? String)?.let { hyp ->
                                                if (hyp.equals(NCDMRUtil.KnownPatient, true))
                                                    patientDiagnosis.add(NCDMRUtil.HYPERTENSION.lowercase())
                                            }
                                            if (values.contains(DefinedParams.Other.lowercase()))
                                                patientDiagnosis.add(DefinedParams.Other.lowercase())
                                            liveData.filter { db ->
                                                patientDiagnosis.any { db.value.equals(it, true) }
                                            }.map { item ->
                                                ChipViewItemModel(
                                                    id = item.id,
                                                    name = item.name,
                                                    cultureValue = item.displayValue,
                                                    type = item.type,
                                                    value = item.value
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    liveData.filter { db ->
                                        values.any { db.value.equals(it, true) }
                                    }.map { item ->
                                        ChipViewItemModel(
                                            id = item.id,
                                            name = item.name,
                                            cultureValue = item.displayValue,
                                            type = item.type,
                                            value = item.value
                                        )
                                    }
                                }

                                if (!data.diagnosisNotes.isNullOrBlank()) {
                                    binding.etCommentDiagnosis.setText(data.diagnosisNotes.takeIf { it.isNotBlank() })
                                }

                                if (!filteredChips.isNullOrEmpty()) {
                                    viewModel.selectedChips.apply {
                                        clear()
                                        addAll(filteredChips)
                                    }
                                    setChipItems(liveData)
                                }
                            }
                        }
                    }
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }
            }
        }
    }

    private fun getDiagonsis() {
        if (isDiagnosisMismatch()) {
            getPatientId()?.let { patientReference ->
                ncdMedicalReviewViewModel.ncdPatientDiagnosisStatus(HashMap<String, Any>().apply {
                    put(
                        DefinedParams.PatientReference,
                        patientReference
                    )
                })
            }
        } else
            confirmDiagnosis()
    }

    private fun confirmDiagnosis() {
        viewModel.getConfirmDiagonsis(
            NCDDiagnosisGetRequest(
                patientReference = getPatientId(),
                memberReference = getMemberId(),
                diagnosisType = getConfirmDiagnosisTypes()
            )
        )
    }

    private fun setChipItems(ncdDiagnosisEntities: List<NCDDiagnosisEntity>) {
        val complaintList = ncdDiagnosisEntities.map { item ->
            ChipViewItemModel(
                id = item.id,
                name = item.name,
                cultureValue = item.displayValue,
                type = item.type,
                value = item.value
            )
        } as ArrayList<ChipViewItemModel>
        addChip(complaintList)
    }

    private fun addChip(complaintList: ArrayList<ChipViewItemModel>) {
        tagListCustomView.addChipItemList(
            complaintList,
            viewModel.selectedChips,
            diagnosisGrouping(complaintList)
        )
    }

    private fun diagnosisGrouping(list: List<ChipViewItemModel>?): HashMap<String, MutableList<ChipViewItemModel>>? {
        return list?.groupByTo(HashMap(), { it.type.toString() }, { it })
    }

    private fun initView() {
        viewModel.getChip(getTypes().map { it.lowercase() }, getGender(),isPregnant())
        tagListCustomView =
            TagListCustomView(
                binding.root.context,
                binding.cgDiagnosis,
                callBack = { _, _, _ ->
                    viewModel.selectedChips =
                        ArrayList(tagListCustomView.getSelectedTags())
                }
            )
        binding.ivClose.safeClickListener(this)
        binding.btnCancel.safeClickListener(this)
        binding.btnConfirm.safeClickListener(this)
        MotherNeonateUtil.initTextWatcherForString(binding.etCommentDiagnosis) {
            viewModel.comments = it
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.ivClose.id, binding.btnCancel.id -> dismiss()
            binding.btnConfirm.id -> {
                if (validateInput()) {
                    createDiagonsis()
                }
            }
        }
    }

    private fun createDiagonsis() {
        val request = NCDDiagnosisRequestResponse(
            ProvanceDto(),
            diagnosisNotes = viewModel.comments.takeIf { it.isNotBlank() },
            confirmDiagnosis = viewModel.selectedChips.map { chip ->
                NCDDiagnosisItem(type = chip.type, value = chip.value)
            },
            patientReference = getPatientId(),
            memberReference = getMemberId(),
            type = getTypeForRequest()
        )
        viewModel.createConfirmDiagonsis(request,arguments?.getString(MENU_Name))
    }

    fun validateInput(): Boolean {
        val isValid = viewModel.selectedChips.isNotEmpty()
        if (isValid) {
            binding.tvErrorMessage.gone()
        } else {
            binding.tvErrorMessage.visible()
        }
        return isValid
    }

    override fun onStart() {
        super.onStart()
        handleOrientation()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        handleOrientation()
    }

    private fun handleOrientation() {
        val isTablet = CommonUtils.checkIsTablet(requireContext())
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val width = when {
            isTablet && isLandscape -> 65
            else -> 100
        }
        val height = when {
            isTablet && isLandscape -> 90
            else -> 100
        }
        setDialogPercent(width, height)
    }

    fun showLoading() {
        binding.apply {
            btnConfirm.invisible()
            btnCancel.invisible()
            loadingProgress.visible()
            loaderImage.apply {
                loadAsGif(R.drawable.loader_spice)
            }
        }
    }

    fun hideLoading() {
        binding.apply {
            btnConfirm.visible()
            btnCancel.visible()
            loadingProgress.gone()
            loaderImage.apply {
                resetImageView()
            }
        }
    }
}