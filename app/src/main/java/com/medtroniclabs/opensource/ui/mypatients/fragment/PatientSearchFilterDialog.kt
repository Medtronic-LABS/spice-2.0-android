package com.medtroniclabs.opensource.ui.mypatients.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.app.analytics.model.UserDetail
import com.medtroniclabs.opensource.app.analytics.utils.AnalyticsDefinedParams
import com.medtroniclabs.opensource.appextensions.setDialogPercent
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.data.model.ChipViewItemModel
import com.medtroniclabs.opensource.databinding.FragmentPatientSearchFilterDialogBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.ncd.medicalreview.CommonEnums
import com.medtroniclabs.opensource.ui.TagListCustomView
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.PatientListViewModel

class PatientSearchFilterDialog : DialogFragment(), View.OnClickListener {
    private lateinit var binding: FragmentPatientSearchFilterDialogBinding
    private lateinit var medicalReviewDueTag: TagListCustomView
    private lateinit var patientStatusTag: TagListCustomView

    private lateinit var ncdReferredForTag: TagListCustomView
    private lateinit var ncdMedicalReviewDateTag: TagListCustomView
    private lateinit var ncdRedRiskTag: TagListCustomView
    private lateinit var ncdRegistrationTag: TagListCustomView
    private lateinit var ncdCvdRiskTag: TagListCustomView
    private lateinit var ncdAssessmentTag: TagListCustomView
    private val patientListViewModel: PatientListViewModel by viewModels(
        ownerProducer = { requireParentFragment() }
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPatientSearchFilterDialogBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        isCancelable = false
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    private fun initView() {
        binding.apply {
            if (CommonUtils.isNonCommunity()) {
                if (CommonUtils.isDispenseOrInvestigation(requireArguments().getString(ORIGIN))) {
                    referredForGroup.visible()
                } else {
                    medicalReviewDateGroup.visible()
                    riskGroup.visible()
                    registrationGroup.visible()
                    cvdGroup.visible()
                    assessmentGroup.visible()
                }
            } else {
                spiceFilterGroup.visible()
            }
        }
        binding.btnLayout.btnConfirm.text = requireContext().getString(R.string.apply)
        medicalReviewDueTag =
            TagListCustomView(binding.root.context, binding.medicalReviewDueChipGroup) { _, _, _ ->
                enableConfirm()
            }
        patientStatusTag =
            TagListCustomView(binding.root.context, binding.patientStatusChipGroup) { _, _, _ ->
                enableConfirm()
            }
        ncdReferredForTag =
            TagListCustomView(binding.root.context, binding.ncdReferredForChipGroup) { _, _, _ ->
                enableConfirm()
            }
        ncdMedicalReviewDateTag =
            TagListCustomView(
                binding.root.context,
                binding.ncdMedicalReviewDateChipGroup
            ) { _, _, _ ->
                enableConfirm()
            }
        ncdRedRiskTag =
            TagListCustomView(binding.root.context, binding.riskChipGroup) { _, _, _ ->
                enableConfirm()
            }
        ncdRegistrationTag =
            TagListCustomView(binding.root.context, binding.registrationChipGroup) { _, _, _ ->
                enableConfirm()
            }
        ncdCvdRiskTag =
            TagListCustomView(binding.root.context, binding.cvdChipGroup) { _, _, _ ->
                enableConfirm()
            }
        ncdAssessmentTag =
            TagListCustomView(binding.root.context, binding.assessmentDateChipGroup) { _, _, _ ->
                enableConfirm()
            }
        medicalReviewDueTag.addChipItemList(
            getMedicalReviewDueChip(),
            patientListViewModel.medicalReviewDueTag
        )
        patientStatusTag.addChipItemList(
            getPatientStatusChip(),
            patientListViewModel.patientStatusTag
        )
        ncdReferredForTag.addChipItemList(
            getYesterdayTodayChip(),
            patientListViewModel.ncdReferredForTag
        )
        ncdMedicalReviewDateTag.addChipItemList(
            getTodayTomorrowChip(),
            patientListViewModel.ncdMedicalReviewDateTag
        )
        ncdRedRiskTag.addChipItemList(
            getRedRisks(),
            patientListViewModel.ncdRedRiskTag
        )
        ncdRegistrationTag.addChipItemList(
            getRegistrations(),
            patientListViewModel.ncdRegistrationTag
        )
        ncdCvdRiskTag.addChipItemList(
            getCvdRisks(),
            patientListViewModel.ncdCvdRiskTag
        )
        ncdAssessmentTag.addChipItemList(
            getTodayTomorrowChip(),
            patientListViewModel.ncdAssessmentTag
        )
        binding.btnLayout.btnCancel.safeClickListener(this)
        binding.imgClose.safeClickListener(this)
        binding.btnLayout.btnConfirm.safeClickListener(this)
    }

    private fun enableConfirm() {
        binding.btnLayout.btnConfirm.isEnabled =
            patientStatusTag.getSelectedTags().isNotEmpty() ||
                    medicalReviewDueTag.getSelectedTags().isNotEmpty() ||
                    ncdReferredForTag.getSelectedTags().isNotEmpty() ||
                    ncdMedicalReviewDateTag.getSelectedTags().isNotEmpty() ||
                    ncdRedRiskTag.getSelectedTags().isNotEmpty() ||
                    ncdRegistrationTag.getSelectedTags().isNotEmpty() ||
                    ncdCvdRiskTag.getSelectedTags().isNotEmpty() ||
                    ncdAssessmentTag.getSelectedTags().isNotEmpty()
    }

    override fun onStart() {
        super.onStart()
        if (CommonUtils.checkIsTablet(requireContext())) {
            setDialogPercent(60)
        } else {
            setDialogPercent(90)
        }
    }

    companion object {
        const val TAG = "PatientSearchFilterDialog"
        const val ORIGIN = "origin"

        fun newInstance(origin: String?): PatientSearchFilterDialog {
            val args = Bundle()
            args.putString(ORIGIN, origin)
            val fragment = PatientSearchFilterDialog()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.imgClose.id -> dismiss()
            binding.btnLayout.btnCancel.id -> {
                patientListViewModel.apply {
                    patientStatusTag = null
                    medicalReviewDueTag = null
                    ncdReferredForTag = null
                    ncdMedicalReviewDateTag = null
                    ncdRedRiskTag = null
                    ncdRegistrationTag = null
                    ncdCvdRiskTag = null
                    ncdAssessmentTag = null
                }
                patientListViewModel.setFilter(true)
                dismiss()
            }

            binding.btnLayout.btnConfirm.id -> {
                patientListViewModel.apply {
                    this@PatientSearchFilterDialog.let {
                        medicalReviewDueTag =
                            it.medicalReviewDueTag.getSelectedTags().takeIf { it.isNotEmpty() }
                        patientStatusTag =
                            it.patientStatusTag.getSelectedTags().takeIf { it.isNotEmpty() }
                        ncdReferredForTag =
                            it.ncdReferredForTag.getSelectedTags().takeIf { it.isNotEmpty() }
                        ncdMedicalReviewDateTag =
                            it.ncdMedicalReviewDateTag.getSelectedTags().takeIf { it.isNotEmpty() }
                        ncdRedRiskTag =
                            it.ncdRedRiskTag.getSelectedTags().takeIf { it.isNotEmpty() }
                        ncdRegistrationTag =
                            it.ncdRegistrationTag.getSelectedTags().takeIf { it.isNotEmpty() }
                        ncdCvdRiskTag =
                            it.ncdCvdRiskTag.getSelectedTags().takeIf { it.isNotEmpty() }
                        ncdAssessmentTag =
                            it.ncdAssessmentTag.getSelectedTags().takeIf { it.isNotEmpty() }
                    }
                }
                patientListViewModel.setAnalyticsData(
                    UserDetail.startDateTime,
                    eventName = AnalyticsDefinedParams.NCDPatientFilter,
                    isCompleted = true
                )
                patientListViewModel.setFilter(true)
                dismiss()
            }
        }
    }

    private fun getMedicalReviewDueChip(): ArrayList<ChipViewItemModel> {
        val chipItemList = ArrayList<ChipViewItemModel>()
        chipItemList.add(
            ChipViewItemModel(
                id = 1,
                name = CommonEnums.TODAY.title,
                cultureValue = getString(CommonEnums.TODAY.cultureValue),
                value = CommonEnums.TODAY.value
            )
        )
        chipItemList.add(
            ChipViewItemModel(
                id = 2,
                name = CommonEnums.TOMORROW.title,
                cultureValue = getString(CommonEnums.TOMORROW.cultureValue),
                value = CommonEnums.TOMORROW.value
            )
        )
        return chipItemList
    }

    private fun getPatientStatusChip(): ArrayList<ChipViewItemModel> {
        val chipItemList = ArrayList<ChipViewItemModel>()
        chipItemList.add(
            ChipViewItemModel(
                id = 1,
                name = CommonEnums.REFERRED.title,
                cultureValue = getString(CommonEnums.REFERRED.cultureValue),
                value = CommonEnums.REFERRED.value
            )
        )
        chipItemList.add(
            ChipViewItemModel(
                id = 2,
                name = CommonEnums.ON_TREATMENT.title,
                cultureValue = getString(CommonEnums.ON_TREATMENT.cultureValue),
                value = CommonEnums.ON_TREATMENT.value
            )
        )
        return chipItemList
    }

    private fun getYesterdayTodayChip(): ArrayList<ChipViewItemModel> {
        val chipItemList = ArrayList<ChipViewItemModel>()
        chipItemList.add(
            ChipViewItemModel(
                id = 1,
                name = CommonEnums.YESTERDAY.title,
                cultureValue = getString(CommonEnums.YESTERDAY.cultureValue),
                value = CommonEnums.YESTERDAY.value
            )
        )
        chipItemList.add(
            ChipViewItemModel(
                id = 2,
                name = CommonEnums.TODAY.title,
                cultureValue = getString(CommonEnums.TODAY.cultureValue),
                value = CommonEnums.TODAY.value
            )
        )
        return chipItemList
    }

    private fun getTodayTomorrowChip(): ArrayList<ChipViewItemModel> {
        val chipItemList = ArrayList<ChipViewItemModel>()
        chipItemList.add(
            ChipViewItemModel(
                id = 1,
                name = CommonEnums.TODAY.title,
                cultureValue = getString(CommonEnums.TODAY.cultureValue),
                value = CommonEnums.TODAY.value
            )
        )
        chipItemList.add(
            ChipViewItemModel(
                id = 2,
                name = CommonEnums.TOMORROW.title,
                cultureValue = getString(CommonEnums.TOMORROW.cultureValue),
                value = CommonEnums.TOMORROW.value
            )
        )
        return chipItemList
    }

    private fun getRegistrations(): ArrayList<ChipViewItemModel> {
        val chipItemList = ArrayList<ChipViewItemModel>()
        chipItemList.add(
            ChipViewItemModel(
                id = 1,
                name = CommonEnums.ENROLLED.title,
                cultureValue = getString(CommonEnums.ENROLLED.cultureValue),
                value = CommonEnums.ENROLLED.value
            )
        )
        chipItemList.add(
            ChipViewItemModel(
                id = 2,
                name = CommonEnums.NOT_ENROLLED.title,
                cultureValue = getString(CommonEnums.NOT_ENROLLED.cultureValue),
                value = CommonEnums.NOT_ENROLLED.value
            )
        )
        return chipItemList
    }

    private fun getCvdRisks(): ArrayList<ChipViewItemModel> {
        val chipItemList = ArrayList<ChipViewItemModel>()
        chipItemList.add(
            ChipViewItemModel(
                id = 1,
                name = CommonEnums.HIGH_RISK.title,
                cultureValue = getString(CommonEnums.HIGH_RISK.cultureValue),
                value = CommonEnums.HIGH_RISK.value
            )
        )
        chipItemList.add(
            ChipViewItemModel(
                id = 2,
                name = CommonEnums.MEDIUM_RISK.title,
                cultureValue = getString(CommonEnums.MEDIUM_RISK.cultureValue),
                value = CommonEnums.MEDIUM_RISK.value
            )
        )
        chipItemList.add(
            ChipViewItemModel(
                id = 3,
                name = CommonEnums.LOW_RISK.title,
                cultureValue = getString(CommonEnums.LOW_RISK.cultureValue),
                value = CommonEnums.LOW_RISK.value
            )
        )
        return chipItemList
    }

    private fun getRedRisks(): ArrayList<ChipViewItemModel> {
        val chipItemList = ArrayList<ChipViewItemModel>()
        chipItemList.add(
            ChipViewItemModel(
                id = 1,
                name = CommonEnums.RED_RISK.title,
                cultureValue = getString(CommonEnums.RED_RISK.cultureValue),
                value = CommonEnums.RED_RISK.value
            )
        )
        return chipItemList
    }
}