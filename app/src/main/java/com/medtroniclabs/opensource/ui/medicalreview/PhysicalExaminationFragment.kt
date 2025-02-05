package com.medtroniclabs.opensource.ui.medicalreview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.data.model.ChipViewItemModel
import com.medtroniclabs.opensource.databinding.FragmentPhysicalExaminationBinding
import com.medtroniclabs.opensource.formgeneration.model.FormLayout
import com.medtroniclabs.opensource.formgeneration.ui.SingleSelectionCustomView
import com.medtroniclabs.opensource.mappingkey.HouseHoldRegistration
import com.medtroniclabs.opensource.ui.BaseFragment
import com.medtroniclabs.opensource.ui.TagListCustomView
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewDefinedParams
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewTypeEnums
import com.medtroniclabs.opensource.ui.medicalreview.viewmodel.PhysicalExaminationViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PhysicalExaminationFragment : BaseFragment() {

    private lateinit var binding: FragmentPhysicalExaminationBinding
    private val viewModel: PhysicalExaminationViewModel by activityViewModels()
    private lateinit var examinationsTagView: TagListCustomView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentPhysicalExaminationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews()
        attachObserver()
        initializeBreastCondition()
        initializeCongenitalDetect()
        initializeCordExamination()
        initializeExclusiveBreastCondition()
    }

    private fun attachObserver() {
        viewModel.systemicExaminationListLiveData.observe(viewLifecycleOwner) { list ->
            val chipItemList = ArrayList<ChipViewItemModel>()
            list.filter { it.category == MedicalReviewTypeEnums.ObstetricExaminations.name }
                .forEach {
                    chipItemList.add(
                        ChipViewItemModel(
                            id = it.id,
                            name = it.name,
                            cultureValue = it.displayValue,
                            type = it.type,
                            value = it.value
                        )
                    )
                }
            examinationsTagView.addChipItemList(chipItemList, null)
        }
    }

    private fun initializeViews() {
        examinationsTagView = TagListCustomView(
            binding.root.context, binding.tagPhysicalExamination
        ) { _, _, _ ->
            viewModel.selectedSystemicExaminations =
                ArrayList(examinationsTagView.getSelectedTags())
            setFragmentResult(
                MedicalReviewDefinedParams.SE_ITEM, bundleOf(
                    MedicalReviewDefinedParams.CHIP_ITEMS to true
                )
            )
        }
        viewModel.setType(MedicalReviewTypeEnums.PNC_CHILD_REVIEW.name)
    }

    private fun initializeCongenitalDetect() {
        getCongenitalDetectFlowData().let {
            val view = SingleSelectionCustomView(binding.root.context)
            view.tag = DefinedParams.CongenitalDetect
            view.addViewElements(
                it,
                SecuredPreference.getIsTranslationEnabled(),
                viewModel.congenitalDefectMap,
                Pair(DefinedParams.CongenitalDetect, null),
                FormLayout(viewType = "", id = "", title = "", visibility = "", optionsList = null),
                congenitalDetectSelectionCallback
            )
            binding.congenitalDetectSelector.addView(view)
        }
    }

    private var congenitalDetectSelectionCallback: ((selectedID: Any?, elementId: Pair<String, String?>, serverViewModel: FormLayout, name: String?) -> Unit)? =
        { selectedID, _, _, _ ->
            viewModel.congenitalDefectMap[DefinedParams.CongenitalDetect] = selectedID as String
            val flowValue =
                viewModel.congenitalDefectMap[DefinedParams.CongenitalDetect] as? String
            viewModel.congenitalDefect =
                flowValue?.equals(HouseHoldRegistration.yes, ignoreCase = true) ?: false
        }

    private fun getCongenitalDetectFlowData(): ArrayList<Map<String, Any>> {
        val flowList = ArrayList<Map<String, Any>>()
        flowList.add(CommonUtils.getOptionMap(getString(R.string.yes), getString(R.string.yes)))
        flowList.add(CommonUtils.getOptionMap(getString(R.string.no), getString(R.string.no)))
        return flowList
    }

    private fun initializeCordExamination() {
        getCordExaminationFlowData().let {
            val view = SingleSelectionCustomView(binding.root.context)
            view.tag = DefinedParams.CordExamination
            view.addViewElements(
                it,
                SecuredPreference.getIsTranslationEnabled(),
                viewModel.cordExaminationMap,
                Pair(DefinedParams.CordExamination, null),
                FormLayout(viewType = "", id = "", title = "", visibility = "", optionsList = null),
                cordExaminationSelectionCallback
            )
            binding.cordExaminationSelector.addView(view)
        }
    }

    private var cordExaminationSelectionCallback: ((selectedID: Any?, elementId: Pair<String, String?>, serverViewModel: FormLayout, name: String?) -> Unit)? =
        { selectedID, _, _, _ ->
            viewModel.cordExaminationMap[DefinedParams.CordExamination] = selectedID as String
        }

    private fun getCordExaminationFlowData(): ArrayList<Map<String, Any>> {
        val flowList = ArrayList<Map<String, Any>>()
        flowList.add(
            CommonUtils.getOptionMap(
                getString(R.string.healing_satisfactorily),
                getString(R.string.healing_satisfactorily)
            )
        )
        flowList.add(
            CommonUtils.getOptionMap(
                getString(R.string.poor_healing), getString(R.string.poor_healing)
            )
        )
        flowList.add(
            CommonUtils.getOptionMap(
                getString(R.string.infected), getString(R.string.infected)
            )
        )
        return flowList
    }


    private fun initializeBreastCondition() {
        getBreastConditionFlowData().let {
            val view = SingleSelectionCustomView(binding.root.context)
            view.tag = DefinedParams.BreastCondition
            view.addViewElements(
                it,
                SecuredPreference.getIsTranslationEnabled(),
                viewModel.breastCondition,
                Pair(DefinedParams.BreastCondition, null),
                FormLayout(viewType = "", id = "", title = "", visibility = "", optionsList = null),
                breastConditionSelectionCallback
            )
            binding.breastFeedingSelector.addView(view)
        }
    }

    private var breastConditionSelectionCallback: ((selectedID: Any?, elementId: Pair<String, String?>, serverViewModel: FormLayout, name: String?) -> Unit)? =
        { selectedID, _, _, _ ->
            viewModel.breastCondition[DefinedParams.BreastCondition] = selectedID as String
            val flowValue =
                viewModel.breastCondition[DefinedParams.BreastCondition] as? String
            viewModel.breastFeeding =
                flowValue?.equals(HouseHoldRegistration.yes, ignoreCase = true) ?: false
            if (selectedID == getString(R.string.yes)) {
                binding.BreastFeedingGroup.visible()
            } else {
                binding.BreastFeedingGroup.gone()
                resetSelectionViews(DefinedParams.ExclusiveBreastCondition)
            }

        }

    private fun getBreastConditionFlowData(): ArrayList<Map<String, Any>> {
        val flowList = ArrayList<Map<String, Any>>()
        flowList.add(CommonUtils.getOptionMap(getString(R.string.yes), getString(R.string.yes)))
        flowList.add(CommonUtils.getOptionMap(getString(R.string.no), getString(R.string.no)))
        return flowList
    }

    private fun initializeExclusiveBreastCondition() {
        getExclusiveBreastConditionFlowData().let {
            val view = SingleSelectionCustomView(binding.root.context)
            view.tag = DefinedParams.ExclusiveBreastCondition
            view.addViewElements(
                it,
                SecuredPreference.getIsTranslationEnabled(),
                viewModel.exclusiveBreastCondition,
                Pair(DefinedParams.ExclusiveBreastCondition, null),
                FormLayout(viewType = "", id = "", title = "", visibility = "", optionsList = null),
                exclusiveBreastConditionSelectionCallback
            )
            binding.exclusiveBreastFeedingSelector.addView(view)
        }
    }

    private var exclusiveBreastConditionSelectionCallback: ((selectedID: Any?, elementId: Pair<String, String?>, serverViewModel: FormLayout, name: String?) -> Unit)? =
        { selectedID, _, _, _ ->
            viewModel.exclusiveBreastCondition[DefinedParams.ExclusiveBreastCondition] =
                selectedID as String
            val flowValue =
                viewModel.exclusiveBreastCondition[DefinedParams.ExclusiveBreastCondition] as? String
            viewModel.exclusiveBreastFeeding =
                flowValue?.equals(HouseHoldRegistration.yes, ignoreCase = true) ?: false
        }

    private fun getExclusiveBreastConditionFlowData(): ArrayList<Map<String, Any>> {
        val flowList = ArrayList<Map<String, Any>>()
        flowList.add(CommonUtils.getOptionMap(getString(R.string.yes), getString(R.string.yes)))
        flowList.add(CommonUtils.getOptionMap(getString(R.string.no), getString(R.string.no)))
        return flowList
    }

    private fun resetSelectionViews(vararg viewTags: String) {
        viewTags.forEach { tag ->
            val view = binding.root.findViewWithTag<SingleSelectionCustomView>(tag)
            view?.resetSingleSelectionChildViews()
        }
    }
    fun refreshFragment() {
        examinationsTagView.clearSelection()
        examinationsTagView.clearOtherChip()
        viewModel.breastFeeding=null
        viewModel.exclusiveBreastFeeding=null
        viewModel.congenitalDefect=null
        viewModel.cordExaminationMap.clear()
        resetSelectionViews(DefinedParams.CongenitalDetect)
        resetSelectionViews(DefinedParams.CordExamination)
        resetSelectionViews(DefinedParams.ExclusiveBreastCondition)
        resetSelectionViews(DefinedParams.BreastCondition)
        binding.BreastFeedingGroup.gone()

    }


}