package com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.data.model.ChipViewItemModel
import com.medtroniclabs.opensource.databinding.FragmentPregnancyPastObstetricHistoryBinding
import com.medtroniclabs.opensource.formgeneration.model.FormLayout
import com.medtroniclabs.opensource.formgeneration.ui.SingleSelectionCustomView
import com.medtroniclabs.opensource.mappingkey.HouseHoldRegistration.yes
import com.medtroniclabs.opensource.ui.BaseFragment
import com.medtroniclabs.opensource.ui.TagListCustomView
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewTypeEnums
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.PregnancyPastObstetricHistoryViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PregnancyPastObstetricHistoryFragment : BaseFragment() {

    private lateinit var binding: FragmentPregnancyPastObstetricHistoryBinding
    private lateinit var complaintsTagView: TagListCustomView
    private val viewModel: PregnancyPastObstetricHistoryViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPregnancyPastObstetricHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        const val TAG = "PregnancyPastObstetricHistoryFragment"
        fun newInstance() =
            PregnancyPastObstetricHistoryFragment()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.setAncReqToGetMetaForPregnancyHistory(MedicalReviewTypeEnums.PregnancyHistories.name)
        attachObservers()
    }

    private fun attachObservers() {
        viewModel.ancMetaLiveDataForPregnancyHistory.observe(viewLifecycleOwner) {
            val complaintList = it.map { item ->
                ChipViewItemModel(
                    id = item.id,
                    name = item.name,
                    type = item.type,
                    value = item.value
                )
            } as ArrayList<ChipViewItemModel>
            initView(complaintList) {
                initViewEditText()
                viewModel.checkSubmitBtn()
            }
            initViewEditText()
        }
    }

    private fun initViewEditText() {
        binding.etPregnancyHistory.doAfterTextChanged {
            viewModel.pregnancyHistoryNotes = it?.trim().toString()
            viewModel.checkSubmitBtn()
        }
        if (viewModel.pregnancyHistoryNotes?.isNotBlank() == true) {
            binding.etPregnancyHistory.setText(viewModel.pregnancyHistoryNotes)
        }
    }

    private fun initView(
        complaintList: ArrayList<ChipViewItemModel>,
        updateNextButtonStateCallback: (() -> Unit)? = null
    ) {
        var isOtherChip = false
        complaintsTagView =
            TagListCustomView(
                binding.root.context,
                binding.tagViewPresentingComplaints,
                callBack = { name, _,isChecked ->
                    viewModel.pregnancyHistoryChip =
                        ArrayList(complaintsTagView.getSelectedTags())
                    updateNextButtonStateCallback?.invoke()
                }
            )
        viewModel.pregnancyHistoryOther =
            complaintList.firstOrNull { it.name.equals(DefinedParams.Other, ignoreCase = true) }
        complaintsTagView.addChipItemList(complaintList, viewModel.pregnancyHistoryChip)
        getRMNCHFlowData().let {
            val view = SingleSelectionCustomView(binding.root.context)
            view.tag = TAG
            view.addViewElements(
                it,
                SecuredPreference.getIsTranslationEnabled(),
                viewModel.resultFlowHashMap,
                Pair(TAG, null),
                FormLayout(viewType = "", id = "", title = "", visibility = "", optionsList = null),
                singleSelectionCallback
            )
            binding.btnLayout.addView(view)
        }
        viewModel.checkSubmitBtn()
    }

    private var singleSelectionCallback: ((selectedID: Any?, elementId: Pair<String, String?>, serverViewModel: FormLayout, name: String?) -> Unit)? =
        { selectedID, _, _, _ ->
            viewModel.resultFlowHashMap[TAG] = selectedID as String
            val flowValue = viewModel.resultFlowHashMap[TAG] as? String
            viewModel.deliveryKit = flowValue?.equals(yes, ignoreCase = true) ?: false
            viewModel.checkSubmitBtn()
        }


    private fun getRMNCHFlowData(): ArrayList<Map<String, Any>> {
        val flowList = ArrayList<Map<String, Any>>()
        flowList.add(CommonUtils.getOptionMap(getString(R.string.yes), getString(R.string.yes)))
        flowList.add(CommonUtils.getOptionMap(getString(R.string.no), getString(R.string.no)))
        return flowList
    }

}