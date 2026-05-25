package org.medtroniclabs.uhis.ui.medicalreview.motherneonate.anc.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.model.ChipViewItemModel
import org.medtroniclabs.uhis.databinding.FragmentPregnancyPastObstetricHistoryBinding
import org.medtroniclabs.uhis.formgeneration.model.FormLayout
import org.medtroniclabs.uhis.formgeneration.ui.SingleSelectionCustomView
import org.medtroniclabs.uhis.mappingkey.HouseHoldRegistration.YES
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.TagListCustomView
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewTypeEnums
import org.medtroniclabs.uhis.ui.mypatients.viewmodel.PregnancyPastObstetricHistoryViewModel

@AndroidEntryPoint
class PregnancyPastObstetricHistoryFragment : BaseFragment() {
    private lateinit var binding: FragmentPregnancyPastObstetricHistoryBinding
    private lateinit var complaintsTagView: TagListCustomView
    private val viewModel: PregnancyPastObstetricHistoryViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentPregnancyPastObstetricHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        const val TAG = "PregnancyPastObstetricHistoryFragment"

        fun newInstance() = PregnancyPastObstetricHistoryFragment()
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
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
                    value = item.value,
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
        updateNextButtonStateCallback: (() -> Unit)? = null,
    ) {
        var isOtherChip = false
        complaintsTagView =
            TagListCustomView(
                binding.root.context,
                binding.tagViewPresentingComplaints,
                callBack = { name, _, isChecked ->
                    viewModel.pregnancyHistoryChip =
                        ArrayList(complaintsTagView.getSelectedTags())
                    updateNextButtonStateCallback?.invoke()
                },
            )
        viewModel.pregnancyHistoryOther =
            complaintList.firstOrNull { it.name.equals(DefinedParams.OTHER, ignoreCase = true) }
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
                singleSelectionCallback,
            )
            binding.btnLayout.addView(view)
        }
        viewModel.checkSubmitBtn()
    }

    private var singleSelectionCallback: ((selectedID: Any?, elementId: Pair<String, String?>, formLayout: FormLayout, name: String?) -> Unit)? =
        { selectedID, _, _, _ ->
            viewModel.resultFlowHashMap[TAG] = selectedID as String
            val flowValue = viewModel.resultFlowHashMap[TAG] as? String
            viewModel.deliveryKit = flowValue?.equals(YES, ignoreCase = true) ?: false
            viewModel.checkSubmitBtn()
        }

    private fun getRMNCHFlowData(): ArrayList<Map<String, Any>> {
        val flowList = ArrayList<Map<String, Any>>()
        flowList.add(CommonUtils.getOptionMap(getString(R.string.yes), getString(R.string.yes)))
        flowList.add(CommonUtils.getOptionMap(getString(R.string.no), getString(R.string.no)))
        return flowList
    }
}
