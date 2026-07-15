package org.medtroniclabs.uhis.ncd.medicalreview.dialog

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.invisible
import org.medtroniclabs.uhis.appextensions.loadAsGif
import org.medtroniclabs.uhis.appextensions.resetImageView
import org.medtroniclabs.uhis.appextensions.setDialogPercent
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.data.model.ChipViewItemModel
import org.medtroniclabs.uhis.data.offlinesync.model.ProvanceDto
import org.medtroniclabs.uhis.databinding.DialogNcdDiagnosisBinding
import org.medtroniclabs.uhis.db.entity.NCDDiagnosisEntity
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.mappingkey.Screening
import org.medtroniclabs.uhis.ncd.data.NCDDiagnosisGetRequest
import org.medtroniclabs.uhis.ncd.data.NCDDiagnosisItem
import org.medtroniclabs.uhis.ncd.data.NCDDiagnosisRequestResponse
import org.medtroniclabs.uhis.ncd.medicalreview.NCDDialogDismissListener
import org.medtroniclabs.uhis.ncd.medicalreview.NCDMRUtil
import org.medtroniclabs.uhis.ncd.medicalreview.NCDMRUtil.CONFIRM_DIAGNOSIS_TYPE
import org.medtroniclabs.uhis.ncd.medicalreview.NCDMRUtil.CONFIRM_DIAGNOSIS_TYPE_GET
import org.medtroniclabs.uhis.ncd.medicalreview.NCDMRUtil.IsPregnant
import org.medtroniclabs.uhis.ncd.medicalreview.NCDMRUtil.MENU_Name
import org.medtroniclabs.uhis.ncd.medicalreview.viewmodel.NCDDiagnosisViewModel
import org.medtroniclabs.uhis.ncd.medicalreview.viewmodel.NCDMedicalReviewViewModel
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.TagListCustomView
import org.medtroniclabs.uhis.ui.medicalreview.motherneonate.anc.MotherNeonateUtil

@AndroidEntryPoint
class NCDDiagnosisDialogFragment : DialogFragment(), View.OnClickListener {
    private lateinit var binding: DialogNcdDiagnosisBinding
    private val viewModel: NCDDiagnosisViewModel by viewModels()
    private val ncdMedicalReviewViewModel: NCDMedicalReviewViewModel by viewModels()
    private lateinit var tagListCustomView: TagListCustomView
    var listener: NCDDialogDismissListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
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
            menuName: String? = null,
        ) = NCDDiagnosisDialogFragment().apply {
            arguments = Bundle().apply {
                putString(DefinedParams.PatientId, patientId)
                putString(DefinedParams.MEMBER_ID, memberId)
                putStringArrayList(CONFIRM_DIAGNOSIS_TYPE, types)
                putStringArrayList(CONFIRM_DIAGNOSIS_TYPE_GET, getTypes)
                putBoolean(NCDMRUtil.IS_FEMALE, isFemale)
                putBoolean(NCDMRUtil.IS_DIAGNOSIS_MISMATCH, isDiagnosisMismatch)
                putBoolean(IsPregnant, isPregnant)
                putString(Screening.Type, type)
                putString(MENU_Name, menuName)
            }
        }
    }

    private fun getTypes(): ArrayList<String> = arguments?.getStringArrayList(CONFIRM_DIAGNOSIS_TYPE) ?: arrayListOf()

    private fun getConfirmDiagnosisTypes(): ArrayList<String> = arguments?.getStringArrayList(CONFIRM_DIAGNOSIS_TYPE_GET) ?: arrayListOf()

    private fun isPregnant(): Boolean = arguments?.getBoolean(IsPregnant) ?: false

    private fun isDiagnosisMismatch(): Boolean = arguments?.getBoolean(NCDMRUtil.IS_DIAGNOSIS_MISMATCH) ?: false

    private fun getGender(): String =
        if (arguments?.getBoolean(NCDMRUtil.IS_FEMALE) == true) {
            Screening.Female.lowercase()
        } else {
            Screening.Male.lowercase()
        }

    fun getPatientId(): String? = arguments?.getString(DefinedParams.PatientId)

    fun getMemberId(): String? = arguments?.getString(DefinedParams.MEMBER_ID)

    private fun getTypeForRequest(): String? = NCDMRUtil.requestTypeForConfirmDiagnoses(arguments?.getString(Screening.Type))

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
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
                                                if (hyp.equals(NCDMRUtil.KnownPatient, true)) {
                                                    patientDiagnosis.add(NCDMRUtil.HYPERTENSION.lowercase())
                                                }
                                            }
                                            if (values.contains(DefinedParams.OTHER.lowercase())) {
                                                patientDiagnosis.add(DefinedParams.OTHER.lowercase())
                                            }
                                            liveData
                                                .filter { db ->
                                                    patientDiagnosis.any { db.value.equals(it, true) }
                                                }.map { item ->
                                                    ChipViewItemModel(
                                                        id = item.id,
                                                        name = item.name,
                                                        cultureValue = item.displayValue,
                                                        type = item.type,
                                                        value = item.value,
                                                    )
                                                }
                                        }
                                    }
                                } else {
                                    liveData
                                        .filter { db ->
                                            values.any { db.value.equals(it, true) }
                                        }.map { item ->
                                            ChipViewItemModel(
                                                id = item.id,
                                                name = item.name,
                                                cultureValue = item.displayValue,
                                                type = item.type,
                                                value = item.value,
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
                ncdMedicalReviewViewModel.ncdPatientDiagnosisStatus(
                    HashMap<String, Any>().apply {
                        put(
                            DefinedParams.PatientReference,
                            patientReference,
                        )
                    },
                )
            }
        } else {
            confirmDiagnosis()
        }
    }

    private fun confirmDiagnosis() {
        viewModel.getConfirmDiagonsis(
            NCDDiagnosisGetRequest(
                patientReference = getPatientId(),
                memberReference = getMemberId(),
                diagnosisType = getConfirmDiagnosisTypes(),
            ),
        )
    }

    private fun setChipItems(ncdDiagnosisEntities: List<NCDDiagnosisEntity>) {
        val complaintList = ncdDiagnosisEntities.map { item ->
            ChipViewItemModel(
                id = item.id,
                name = item.name,
                cultureValue = item.displayValue,
                type = item.type,
                value = item.value,
            )
        } as ArrayList<ChipViewItemModel>
        addChip(complaintList)
    }

    private fun addChip(complaintList: ArrayList<ChipViewItemModel>) {
        tagListCustomView.addChipItemList(
            complaintList,
            viewModel.selectedChips,
            diagnosisGrouping(complaintList),
        )
    }

    private fun diagnosisGrouping(list: List<ChipViewItemModel>?): HashMap<String, MutableList<ChipViewItemModel>>? =
        list?.groupByTo(HashMap(), {
            it.type.toString()
        }, { it })

    private fun initView() {
        viewModel.getChip(getTypes().map { it.lowercase() }, getGender(), isPregnant())
        tagListCustomView =
            TagListCustomView(
                binding.root.context,
                binding.cgDiagnosis,
                callBack = { _, _, _ ->
                    viewModel.selectedChips =
                        ArrayList(tagListCustomView.getSelectedTags())
                },
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
            type = getTypeForRequest(),
        )
        viewModel.createConfirmDiagonsis(request, arguments?.getString(MENU_Name))
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
                loadAsGif(R.drawable.ic_rotating_uhis_logo)
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
