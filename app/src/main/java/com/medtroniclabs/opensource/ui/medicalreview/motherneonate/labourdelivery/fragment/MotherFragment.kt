package com.medtroniclabs.opensource.ui.medicalreview.motherneonate.labourdelivery.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.DefinedParams.Episiotomy
import com.medtroniclabs.opensource.common.DefinedParams.None
import com.medtroniclabs.opensource.common.DefinedParams.StateOfPerineum
import com.medtroniclabs.opensource.common.DefinedParams.Tear
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.data.LabourDeliveryMetaEntity
import com.medtroniclabs.opensource.data.model.ChipViewItemModel
import com.medtroniclabs.opensource.databinding.FragmentMotherBinding
import com.medtroniclabs.opensource.formgeneration.model.FormLayout
import com.medtroniclabs.opensource.formgeneration.ui.SingleSelectionCustomView
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseFragment
import com.medtroniclabs.opensource.ui.TagListCustomView
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.labourdelivery.viewmodel.LabourDeliveryViewModel
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewTypeEnums

class MotherFragment : BaseFragment() {

    private lateinit var binding: FragmentMotherBinding
    private lateinit var cgGeneralConditionOfMother: TagListCustomView
    private lateinit var cgSignSymptomsObserved: TagListCustomView
    private lateinit var cgRiskFactors: TagListCustomView
    private lateinit var cgStatus: TagListCustomView
    private val viewModel: LabourDeliveryViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMotherBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListeners()
        initializeChipItem()
        attachObserver()
    }

    private fun initListeners() {
        binding.tvNumber.doAfterTextChanged {
            val ttNoOfDosage = it?.trim().toString()
            if(ttNoOfDosage.isNotEmpty()) {
                viewModel.motherTTDosageSoFar = ttNoOfDosage
                viewModel.validateSubmitButtonState()
            }else {
                viewModel.motherTTDosageSoFar = null
                viewModel.validateSubmitButtonState()
            }
        }
    }

    private fun attachObserver() {
        viewModel.labourDeliveryMetaList.observe(viewLifecycleOwner) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.SUCCESS -> {
                    resource.data?.let { listItems ->
                        initializeSignsSymptomsItems(listItems)
                        initializeMotherConditionItems(listItems)
                        initializeRiskFactorsItems(listItems)
                        initializeToMotherStatusItems(listItems)
                        initializeStateOfPerineumLabel(listItems)
                    }
                    hideProgress()
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }
            }
        }
    }

    private fun initializeSignsSymptomsItems(listItems: List<LabourDeliveryMetaEntity>) {
        val chipItemList = ArrayList<ChipViewItemModel>()
        listItems.filter { it.type == MedicalReviewTypeEnums.Mother.name }.forEach {
            chipItemList.add(
                ChipViewItemModel(
                    id = it.id,
                    name = it.name,
                    type = it.type,
                    value = it.value
                )
            )
        }
        cgSignSymptomsObserved.addChipItemList(chipItemList)
    }

    private fun initializeToMotherStatusItems(listItems: List<LabourDeliveryMetaEntity>) {
        val chipItemList = ArrayList<ChipViewItemModel>()
        listItems.filter { it.category == MedicalReviewTypeEnums.MotherDeliveryStatus.name }.forEach {
            chipItemList.add(
                ChipViewItemModel(
                    id = it.id,
                    name = it.name,
                    type = it.type,
                    value = it.value
                )
            )
        }
        cgStatus.addChipItemList(chipItemList)
    }

    private fun initializeMotherConditionItems(listItems: List<LabourDeliveryMetaEntity>) {
        val chipItemList = ArrayList<ChipViewItemModel>()
        listItems.filter { it.category == MedicalReviewTypeEnums.ConditionOfMother.name }.forEach {
            chipItemList.add(
                ChipViewItemModel(
                    id = it.id,
                    name = it.name,
                    type = it.type,
                    value = it.value
                )
            )
        }
        cgGeneralConditionOfMother.addChipItemList(chipItemList)
    }

    private fun initializeRiskFactorsItems(listItems: List<LabourDeliveryMetaEntity>) {
        val chipItemList = ArrayList<ChipViewItemModel>()
        listItems.filter { it.category == MedicalReviewTypeEnums.RiskFactors.name }.forEach {
            chipItemList.add(
                ChipViewItemModel(
                    id = it.id,
                    name = it.name,
                    type = it.type,
                    value = it.value
                )
            )
        }
        cgRiskFactors.addChipItemList(chipItemList)
    }


    private fun initializeChipItem() {
        cgGeneralConditionOfMother =
            TagListCustomView(binding.root.context, binding.cgGeneralConditionOfMother) { name, _, _ ->
                cgGeneralConditionOfMother.getSelectedTags().let {
                    if (it.isNotEmpty()) {
                        viewModel.motherGeneralCondition = it[0].value
                    } else {
                        viewModel.motherGeneralCondition = null
                    }
                }
                viewModel.validateSubmitButtonState()
            }

        cgRiskFactors = TagListCustomView(binding.root.context, binding.cgRiskFactors) { _, _, _ ->
            viewModel.motherRiskFactors = cgRiskFactors.getSelectedTags()
            viewModel.validateSubmitButtonState()
        }
        cgStatus =
            TagListCustomView(binding.root.context, binding.cgStatus) { _, _, _ ->
                viewModel.motherStatus = cgStatus.getSelectedTags()
                viewModel.validateSubmitButtonState()
            }
        cgSignSymptomsObserved =
            TagListCustomView(binding.root.context, binding.cgSignsSymptomsObserved) { _, _, _ ->
                viewModel.motherSignsAndSymptoms = cgSignSymptomsObserved.getSelectedTags()
                viewModel.validateSubmitButtonState()
            }
    }

    companion object {
        const val TAG = "MotherFragment"

        fun newInstance(): MotherFragment {
            return MotherFragment()
        }
    }


    private fun initializeStateOfPerineumLabel(listItems: List<LabourDeliveryMetaEntity>) {
    val chipItemList = ArrayList<ChipViewItemModel>()
        listItems.filter { it.category == MedicalReviewTypeEnums.StateOfPerineum.name }.forEach {
            chipItemList.add(
                ChipViewItemModel(
                    id = it.id,
                    name = it.name,
                    type = it.type,
                    value = it.value
                )
            )
        }
        getMotherFlowData(chipItemList).let {
            val view = SingleSelectionCustomView(binding.root.context)
            view.tag = TAG
            view.addViewElements(
                it,
                SecuredPreference.getIsTranslationEnabled(),
                viewModel.perineumStateMap,
                Pair(StateOfPerineum, null),
                FormLayout(viewType = "", id = "", title = "", visibility = "", optionsList = null),
                singleSelectionCallback
            )
            binding.selectionGroup.addView(view)
        }
    }

    private fun initializeTearLabel() {
        getTearFlowData().let {
            val view = SingleSelectionCustomView(binding.root.context)
            view.tag = TAG
            view.addViewElements(
                it,
                SecuredPreference.getIsTranslationEnabled(),
                viewModel.perineumStateMap,
                Pair(Tear, null),
                FormLayout(viewType = "", id = "", title = "", visibility = "", optionsList = null),
                singleSelectionCallback
            )
            binding.tearlayout.removeAllViews()
            binding.tearlayout.addView(view)
        }
    }

    private var singleSelectionCallback: ((selectedID: Any?, elementId: Pair<String, String?>, serverViewModel: FormLayout, name: String?) -> Unit)? =
        { selectedID, _, _, _ ->
            saveSelectedOptionValue(selectedID)
            viewModel.validateSubmitButtonState()
        }

    private fun saveSelectedOptionValue(selectedID: Any?) {
        viewModel.perineumStateMap[StateOfPerineum] = selectedID as String
        if (selectedID.toString() == DefinedParams.Tear) {
            binding.groupTear.isVisible = true
            initializeTearLabel()
        } else if (selectedID.toString() ==Episiotomy || selectedID.toString()== None) {
            binding.groupTear.isVisible = false
            viewModel.perineumStateMap[Tear]=""
        } else {
            viewModel.perineumStateMap[Tear] = selectedID
        }
    }

    private fun getTearFlowData(): ArrayList<Map<String, Any>> {
        val flowList = ArrayList<Map<String, Any>>()
        flowList.add(CommonUtils.getOptionMap(getString(R.string.deg_1), getString(R.string.deg_1)))
        flowList.add(CommonUtils.getOptionMap(getString(R.string.deg_2), getString(R.string.deg_2)))
        flowList.add(CommonUtils.getOptionMap(getString(R.string.deg_3), getString(R.string.deg_3)))
        flowList.add(CommonUtils.getOptionMap(getString(R.string.deg_4), getString(R.string.deg_4)))
        return flowList
    }

    private fun getMotherFlowData(chipItemList: ArrayList<ChipViewItemModel>): ArrayList<Map<String, Any>> {
        val flowList = ArrayList<Map<String, Any>>()
        chipItemList.forEach {
            it.value?.let {value -> CommonUtils.getOptionMap(value, it.name) }
                ?.let { itemList -> flowList.add(itemList) }
        }
        return flowList
    }

    fun validateInput(): Boolean {
        var isValid = false
        if (validateTagView(
                cgGeneralConditionOfMother,
                binding.cgGeneralConditionOfMotherError
            ) && validateTagView(cgStatus, binding.cgStatusError)
        ) {
            isValid = true
        }
        return isValid
    }

    private fun validateTagView(
        tagView: TagListCustomView,
        textView: AppCompatTextView
    ): Boolean {
        var isValidOrNot = true
        if (tagView.getSelectedTags().isEmpty()) {
            textView.isVisible = true
            isValidOrNot = false
        } else {
            textView.isVisible = false
        }
        return isValidOrNot
    }
}