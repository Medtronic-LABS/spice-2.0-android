package com.medtroniclabs.opensource.ui.medicalreview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.data.model.ChipViewItemModel
import com.medtroniclabs.opensource.databinding.FragmentGeneralExaminationBinding
import com.medtroniclabs.opensource.formgeneration.model.FormLayout
import com.medtroniclabs.opensource.formgeneration.ui.SingleSelectionCustomView
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseFragment
import com.medtroniclabs.opensource.ui.TagListCustomView
import com.medtroniclabs.opensource.ui.assessment.rmnch.RMNCH.ANC
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewDefinedParams
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewTypeEnums
import com.medtroniclabs.opensource.ui.medicalreview.viewmodel.GeneralExaminationViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GeneralExaminationFragment : BaseFragment() {

    private lateinit var binding: FragmentGeneralExaminationBinding
    private lateinit var examinationsTagView: TagListCustomView
    private val viewModel: GeneralExaminationViewModel by activityViewModels()

    companion object{
        const val TAG = "SystemicExaminationsFragment"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            viewModel.systemicExaminationsType =
                it.getString(MedicalReviewTypeEnums.SystemicExaminations.name) ?: ""
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGeneralExaminationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews()
        attachObserver()
        setListener()
        initializeBreastCondition()
        initializeUterusCondition()
        binding.conditionSelector.addTextChangedListener {
            it?.let {
                viewModel.specifyCondition = it.toString()
                setFragmentResult(
                    MedicalReviewDefinedParams.GENERAL_SE_ITEM, bundleOf(
                        MedicalReviewDefinedParams.Notes to true)
                )
            }
        }
        binding.conditionSelectorUterus.addTextChangedListener {
            it?.let {
                viewModel.specifyConditionUterus = it.toString()
                setFragmentResult(
                    MedicalReviewDefinedParams.GENERAL_SE_ITEM, bundleOf(
                        MedicalReviewDefinedParams.Notes to true)
                )
            }
        }
    }

    private fun setListener() {
        binding.etPhysicalExaminationComments.addTextChangedListener {
            it?.let {
                viewModel.enteredExaminationNotes = it.trim().toString()
                setFragmentResult(
                    MedicalReviewDefinedParams.SE_ITEM, bundleOf(
                        MedicalReviewDefinedParams.CHIP_ITEMS to true
                    )
                )
            }
        }
    }

    private fun attachObserver() {
        viewModel.systemicExaminationList.observe(viewLifecycleOwner) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.SUCCESS -> {
                    resource.data?.let { listItems ->
                        val chipItemList = ArrayList<ChipViewItemModel>()
                        val category =
                            if (viewModel.systemicExaminationsType == ANC.uppercase()) MedicalReviewTypeEnums.ObstetricExaminations.name else MedicalReviewTypeEnums.SystemicExaminations.name
                        listItems.filter { it.category == category }.forEach {
                            chipItemList.add(
                                ChipViewItemModel(
                                    id = it.id,
                                    name = it.name,
                                    value = it.value
                                )
                            )
                        }
                        examinationsTagView.addChipItemList(chipItemList, viewModel.selectedSystemicExaminations)
                    }
                    hideProgress()
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }
            }
        }
    }

    private fun initializeViews() {
        val (titleResId, showObstetricGroup) = when (viewModel.systemicExaminationsType) {
            MedicalReviewTypeEnums.PNC_MOTHER_REVIEW.name -> {Pair(R.string.general_systemic_examinations, true)}
            else -> return // Handle other cases or provide a default behavior
        }

        with(binding) {
            tvSystemicExaminationTitle.text = getString(titleResId)
        }
        initTag()
        if (viewModel.enteredExaminationNotes.isNotBlank()) {
            binding.etPhysicalExaminationComments.setText(viewModel.enteredExaminationNotes)
        }
    }

    private fun initTag() {
        examinationsTagView =
            TagListCustomView(binding.root.context, binding.tagPhysicalExamination) { _, _, _ ->
                viewModel.selectedSystemicExaminations =
                    ArrayList(examinationsTagView.getSelectedTags())
                setFragmentResult(
                    MedicalReviewDefinedParams.SE_ITEM, bundleOf(
                        MedicalReviewDefinedParams.CHIP_ITEMS to true
                    )
                )
            }
        viewModel.getSystemicExaminationList(viewModel.systemicExaminationsType)
    }


    fun refreshFragment() {
        examinationsTagView.clearSelection()
        examinationsTagView.clearOtherChip()
        resetSelectionViews(DefinedParams.BreastCondition)
        resetSelectionViews(DefinedParams.UterusCondition)
        viewModel.breastConditionValue=null
        viewModel.uterusConditionValue=null
        viewModel.specifyConditionUterus=null
        viewModel.specifyCondition=null
        binding.etPhysicalExaminationComments.text?.clear()
        binding.specifyConditionGroupUterus.gone()
        binding.specifyConditionGroup.gone()
        binding.conditionSelector.text?.clear()
        binding.conditionSelectorUterus.text?.clear()
    }
    private fun initializeBreastCondition() {
        getBreastConditionFlowData().let {
            val view = SingleSelectionCustomView(binding.root.context)
            view.tag = DefinedParams.BreastCondition
            view.addViewElements(
                it,
                SecuredPreference.getIsTranslationEnabled(),
                viewModel.breastConditionMap,
                Pair(DefinedParams.BreastCondition, null),
                FormLayout(viewType = "", id = "", title = "", visibility = "", optionsList = null),
                breastConditionSelectionCallback
            )
            binding.breastConditionSelector.addView(view)
        }
    }
    private fun initializeUterusCondition() {
        getBreastConditionFlowData().let {
            val view = SingleSelectionCustomView(binding.root.context)
            view.tag = DefinedParams.UterusCondition
            view.addViewElements(
                it,
                SecuredPreference.getIsTranslationEnabled(),
                viewModel.uterusConditionMap,
                Pair(DefinedParams.UterusCondition, null),
                FormLayout(viewType = "", id = "", title = "", visibility = "", optionsList = null),
                uterusSelectionCallback
            )
            binding.uterusSelector.addView(view)
        }
    }

    private var breastConditionSelectionCallback: ((selectedID: Any?, elementId: Pair<String, String?>, serverViewModel: FormLayout, name: String?) -> Unit)? =
        { selectedID, _, _, _ ->
            viewModel.breastConditionMap[DefinedParams.BreastCondition] = selectedID as String
            resetSelectionViews(DefinedParams.BreastCondition)
            val flowValue =
                viewModel.breastConditionMap[DefinedParams.BreastCondition] as? String
            viewModel.breastConditionValue =selectedID
            if (selectedID == getString(R.string.abnormal)) {
                binding.specifyConditionGroup.visible()
            } else {
                binding.conditionSelector.text?.clear()
                binding.specifyConditionGroup.gone()
            }
        }
    private var uterusSelectionCallback: ((selectedID: Any?, elementId: Pair<String, String?>, serverViewModel: FormLayout, name: String?) -> Unit)? =
        { selectedID, _, _, _ ->
            viewModel.uterusConditionMap[DefinedParams.UterusCondition] = selectedID as String
            resetSelectionViews(DefinedParams.UterusCondition)
            val flowValue =
                viewModel.uterusConditionMap[DefinedParams.UterusCondition] as? String
            viewModel.uterusConditionValue =selectedID
            if (selectedID == getString(R.string.abnormal)) {
                binding.specifyConditionGroupUterus.visible()
            } else {
                binding.conditionSelectorUterus.text?.clear()
                binding.specifyConditionGroupUterus.gone()
            }
        }

    private fun getBreastConditionFlowData(): ArrayList<Map<String, Any>> {
        val flowList = ArrayList<Map<String, Any>>()
        flowList.add(CommonUtils.getOptionMap(getString(R.string.normal), getString(R.string.normal)))
        flowList.add(CommonUtils.getOptionMap(getString(R.string.abnormal), getString(R.string.abnormal)))
        return flowList
    }

    private fun resetSelectionViews(vararg viewTags: String) {
        viewTags.forEach { tag ->
            val view = binding.root.findViewWithTag<SingleSelectionCustomView>(tag)
            view?.resetSingleSelectionChildViews()
        }
    }

}