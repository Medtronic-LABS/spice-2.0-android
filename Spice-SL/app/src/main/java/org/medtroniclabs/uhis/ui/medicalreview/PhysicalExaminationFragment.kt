package org.medtroniclabs.uhis.ui.medicalreview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.model.ChipViewItemModel
import org.medtroniclabs.uhis.databinding.FragmentPhysicalExaminationBinding
import org.medtroniclabs.uhis.formgeneration.model.FormLayout
import org.medtroniclabs.uhis.formgeneration.ui.SingleSelectionCustomView
import org.medtroniclabs.uhis.mappingkey.HouseHoldRegistration
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.TagListCustomView
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewTypeEnums
import org.medtroniclabs.uhis.ui.medicalreview.viewmodel.PhysicalExaminationViewModel

@AndroidEntryPoint
class PhysicalExaminationFragment : BaseFragment() {
    private lateinit var binding: FragmentPhysicalExaminationBinding
    private val viewModel: PhysicalExaminationViewModel by activityViewModels()
    private lateinit var examinationsTagView: TagListCustomView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentPhysicalExaminationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
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
            list
                .filter { it.category == MedicalReviewTypeEnums.ObstetricExaminations.name }
                .forEach {
                    chipItemList.add(
                        ChipViewItemModel(
                            id = it.id,
                            name = it.name,
                            cultureValue = it.displayValue,
                            type = it.type,
                            value = it.value,
                        ),
                    )
                }
            examinationsTagView.addChipItemList(chipItemList, null)
        }
    }

    private fun initializeViews() {
        examinationsTagView = TagListCustomView(
            binding.root.context,
            binding.tagPhysicalExamination,
        ) { _, _, _ ->
            viewModel.selectedSystemicExaminations =
                ArrayList(examinationsTagView.getSelectedTags())
            setFragmentResult(
                MedicalReviewDefinedParams.SE_ITEM,
                bundleOf(
                    MedicalReviewDefinedParams.CHIP_ITEMS to true,
                ),
            )
        }
        viewModel.setType(MedicalReviewTypeEnums.PNC_CHILD_REVIEW.name)
    }

    private fun initializeCongenitalDetect() {
        getCongenitalDetectFlowData().let {
            val view = SingleSelectionCustomView(binding.root.context)
            view.tag = DefinedParams.CONGENITAL_DETECT
            view.addViewElements(
                it,
                SecuredPreference.getIsTranslationEnabled(),
                viewModel.congenitalDefectMap,
                Pair(DefinedParams.CONGENITAL_DETECT, null),
                FormLayout(viewType = "", id = "", title = "", visibility = "", optionsList = null),
                congenitalDetectSelectionCallback,
            )
            binding.congenitalDetectSelector.addView(view)
        }
    }

    private var congenitalDetectSelectionCallback: ((selectedID: Any?, elementId: Pair<String, String?>, formLayout: FormLayout, name: String?) -> Unit)? =
        { selectedID, _, _, _ ->
            viewModel.congenitalDefectMap[DefinedParams.CONGENITAL_DETECT] = selectedID as String
            val flowValue =
                viewModel.congenitalDefectMap[DefinedParams.CONGENITAL_DETECT] as? String
            viewModel.congenitalDefect =
                flowValue?.equals(HouseHoldRegistration.YES, ignoreCase = true) ?: false
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
            view.tag = DefinedParams.CORD_EXAMINATION
            view.addViewElements(
                it,
                SecuredPreference.getIsTranslationEnabled(),
                viewModel.cordExaminationMap,
                Pair(DefinedParams.CORD_EXAMINATION, null),
                FormLayout(viewType = "", id = "", title = "", visibility = "", optionsList = null),
                cordExaminationSelectionCallback,
            )
            binding.cordExaminationSelector.addView(view)
        }
    }

    private var cordExaminationSelectionCallback: ((selectedID: Any?, elementId: Pair<String, String?>, formLayout: FormLayout, name: String?) -> Unit)? =
        { selectedID, _, _, _ ->
            viewModel.cordExaminationMap[DefinedParams.CORD_EXAMINATION] = selectedID as String
        }

    private fun getCordExaminationFlowData(): ArrayList<Map<String, Any>> {
        val flowList = ArrayList<Map<String, Any>>()
        flowList.add(
            CommonUtils.getOptionMap(
                getString(R.string.healing_satisfactorily),
                getString(R.string.healing_satisfactorily),
            ),
        )
        flowList.add(
            CommonUtils.getOptionMap(
                getString(R.string.poor_healing),
                getString(R.string.poor_healing),
            ),
        )
        flowList.add(
            CommonUtils.getOptionMap(
                getString(R.string.infected),
                getString(R.string.infected),
            ),
        )
        return flowList
    }

    private fun initializeBreastCondition() {
        getBreastConditionFlowData().let {
            val view = SingleSelectionCustomView(binding.root.context)
            view.tag = DefinedParams.BREAST_CONDITION
            view.addViewElements(
                it,
                SecuredPreference.getIsTranslationEnabled(),
                viewModel.breastCondition,
                Pair(DefinedParams.BREAST_CONDITION, null),
                FormLayout(viewType = "", id = "", title = "", visibility = "", optionsList = null),
                breastConditionSelectionCallback,
            )
            binding.breastFeedingSelector.addView(view)
        }
    }

    private var breastConditionSelectionCallback: ((selectedID: Any?, elementId: Pair<String, String?>, formLayout: FormLayout, name: String?) -> Unit)? =
        { selectedID, _, _, _ ->
            viewModel.breastCondition[DefinedParams.BREAST_CONDITION] = selectedID as String
            val flowValue =
                viewModel.breastCondition[DefinedParams.BREAST_CONDITION] as? String
            viewModel.breastFeeding =
                flowValue?.equals(HouseHoldRegistration.YES, ignoreCase = true) ?: false
            if (selectedID == getString(R.string.yes)) {
                binding.BreastFeedingGroup.visible()
            } else {
                binding.BreastFeedingGroup.gone()
                resetSelectionViews(DefinedParams.EXCLUSIVE_BREAST_CONDITION)
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
            view.tag = DefinedParams.EXCLUSIVE_BREAST_CONDITION
            view.addViewElements(
                it,
                SecuredPreference.getIsTranslationEnabled(),
                viewModel.exclusiveBreastCondition,
                Pair(DefinedParams.EXCLUSIVE_BREAST_CONDITION, null),
                FormLayout(viewType = "", id = "", title = "", visibility = "", optionsList = null),
                exclusiveBreastConditionSelectionCallback,
            )
            binding.exclusiveBreastFeedingSelector.addView(view)
        }
    }

    private var exclusiveBreastConditionSelectionCallback:
        ((selectedID: Any?, elementId: Pair<String, String?>, formLayout: FormLayout, name: String?) -> Unit)? =
        { selectedID, _, _, _ ->
            viewModel.exclusiveBreastCondition[DefinedParams.EXCLUSIVE_BREAST_CONDITION] =
                selectedID as String
            val flowValue =
                viewModel.exclusiveBreastCondition[DefinedParams.EXCLUSIVE_BREAST_CONDITION] as? String
            viewModel.exclusiveBreastFeeding =
                flowValue?.equals(HouseHoldRegistration.YES, ignoreCase = true) ?: false
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
        viewModel.breastFeeding = null
        viewModel.exclusiveBreastFeeding = null
        viewModel.congenitalDefect = null
        viewModel.cordExaminationMap.clear()
        resetSelectionViews(DefinedParams.CONGENITAL_DETECT)
        resetSelectionViews(DefinedParams.CORD_EXAMINATION)
        resetSelectionViews(DefinedParams.EXCLUSIVE_BREAST_CONDITION)
        resetSelectionViews(DefinedParams.BREAST_CONDITION)
        binding.BreastFeedingGroup.gone()
    }
}
