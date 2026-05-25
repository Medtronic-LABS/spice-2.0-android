package org.medtroniclabs.uhis.ncd.followup.fragment

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.model.UserDetail
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.appextensions.convertToUtcDateTime
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.CommonUtils.getOptionMap
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.ShortageReasonEntity
import org.medtroniclabs.uhis.data.model.ChipViewItemModel
import org.medtroniclabs.uhis.data.offlinesync.model.FollowUpCallReason
import org.medtroniclabs.uhis.data.offlinesync.model.FollowUpCallStatus
import org.medtroniclabs.uhis.data.offlinesync.model.ProvanceDto
import org.medtroniclabs.uhis.databinding.FragmentNcdCallResultBottomDialogBinding
import org.medtroniclabs.uhis.db.entity.HealthFacilityEntity
import org.medtroniclabs.uhis.db.entity.NCDCallDetails
import org.medtroniclabs.uhis.db.entity.NCDFollowUp
import org.medtroniclabs.uhis.formgeneration.extension.markMandatory
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.model.FormLayout
import org.medtroniclabs.uhis.formgeneration.ui.SingleSelectionCustomView
import org.medtroniclabs.uhis.formgeneration.utility.CustomSpinnerAdapter
import org.medtroniclabs.uhis.ncd.data.CallDetails
import org.medtroniclabs.uhis.ncd.data.FollowUpUpdateRequest
import org.medtroniclabs.uhis.ncd.followup.NCDFollowUpUtils.visited_facility
import org.medtroniclabs.uhis.ncd.followup.NCDFollowUpUtils.will_visit_facility
import org.medtroniclabs.uhis.ncd.followup.NCDFollowUpUtils.wont_visit_facility
import org.medtroniclabs.uhis.ncd.followup.viewmodel.NCDCallResultViewModel
import org.medtroniclabs.uhis.ncd.followup.viewmodel.NCDFollowUpViewModel
import org.medtroniclabs.uhis.ui.TagListCustomView
import org.medtroniclabs.uhis.ui.followup.fragment.CallResultDialogFragment

@AndroidEntryPoint
class NCDCallResultBottomDialog : BottomSheetDialogFragment(), View.OnClickListener {
    private lateinit var binding: FragmentNcdCallResultBottomDialogBinding
    private val viewModel: NCDFollowUpViewModel by activityViewModels()
    private val callResultViewModel: NCDCallResultViewModel by viewModels()
    var data: NCDFollowUp? = null
    private lateinit var reasonListCustomView: TagListCustomView
    val adapter by lazy { CustomSpinnerAdapter(requireContext()) }

    fun setFollowUpData(value: NCDFollowUp) {
        this.data = value
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentNcdCallResultBottomDialogBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return binding.root
    }

    override fun getTheme(): Int = R.style.DialogStyle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false

        viewModel.callResultHashMap.clear()
        viewModel.patientStatusHashMap.clear()
        viewModel.unSuccessfulHashMap.clear()
    }

    companion object {
        const val TAG = "NCDCallResultBottomDialog"

        fun newInstance() =
            NCDCallResultBottomDialog().apply {
            }

        fun newInstance(data: NCDFollowUp) =
            NCDCallResultBottomDialog().apply {
                setFollowUpData(data)
            }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        BottomSheetDialog(requireContext(), theme).apply {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initializeTagView()
        initView()
        viewModel.getFollowUpReasonList()
        viewModel.getSites(true)
        attachObservers()
        setListeners()
        binding.etOther.doAfterTextChanged {
            enableForSuccessFul()
        }
    }

    private fun attachObservers() {
        callResultViewModel.getAttempts.observe(viewLifecycleOwner) {
            data?.let { value ->
                SecuredPreference.putBoolean(
                    SecuredPreference.EnvironmentKey.INITIAL_CALL.name,
                    false,
                )
                val isSuccess = viewModel.callResultHashMap[DefinedParams.CALL_RESULT] == FollowUpCallStatus.SUCCESSFUL.name

                var otherReason: String? = null
                val reason = when {
                    !isSuccess -> viewModel.unSuccessfulHashMap[DefinedParams.UN_SUCCESSFUL] as? String
                    (viewModel.patientStatusHashMap[DefinedParams.PATIENT_STATUS] as? String)
                        ?.equals(wont_visit_facility, true) == true -> {
                        val selectedTagName = reasonListCustomView.getSelectedTags().firstOrNull()?.name
                        if (selectedTagName.equals(DefinedParams.OTHER, true)) {
                            otherReason = binding.etOther.text
                                ?.trim()
                                .toString()
                            DefinedParams.OTHER
                        } else {
                            selectedTagName
                        }
                    }
                    else -> null
                }

                val patientStatus = viewModel.patientStatusHashMap[DefinedParams.PATIENT_STATUS] as? String
                val visitedFacilityId = if (patientStatus.equals(visited_facility, true)) {
                    viewModel.selectedHealthFacilityId
                } else {
                    null
                }

                val otherVisitedFacilityName = if (viewModel.selectedHealthFacilityName.equals(DefinedParams.OTHER, true)) {
                    binding.etOther.text
                        ?.trim()
                        .toString()
                } else {
                    null
                }
                viewModel.insertNCDCallDetails(
                    NCDCallDetails(
                        id = value.id,
                        villageId = value.villageId,
                        patientId = value.patientId,
                        memberId = value.memberId,
                        referredSiteId = value.referredSiteId,
                        callDate = System.currentTimeMillis().convertToUtcDateTime(),
                        status = (viewModel.callResultHashMap[DefinedParams.CALL_RESULT] as? String)
                            ?: null,
                        reason = reason,
                        otherReason = otherReason,
                        patientStatus = (viewModel.patientStatusHashMap[DefinedParams.PATIENT_STATUS] as? String)
                            ?: null,
                        visitedFacilityId = visitedFacilityId,
                        otherVisitedFacilityName = otherVisitedFacilityName,
                        type = viewModel.typeOffline,
                        attempts = it?.plus(1) ?: 1,
                        createdAt = System.currentTimeMillis(),
                        createdBy = SecuredPreference.getUserId(),
                        updatedAt = System.currentTimeMillis(),
                        updatedBy = SecuredPreference.getUserId(),
                    ),
                )
            }
        }
        viewModel.getFollowUpReasonList.observe(viewLifecycleOwner) { list ->
            list?.let {
                if (list.isNotEmpty()) {
                    loadDeleteReasonList(it)
                }
            }
        }
        viewModel.getSitesLiveData.observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                loadSiteDetails(ArrayList(it))
            }
        }
    }

    private fun initView() {
        viewModel.callResultHashMap[DefinedParams.CALL_RESULT] = FollowUpCallStatus.SUCCESSFUL.name
        getCallResultData().let {
            val view = SingleSelectionCustomView(binding.root.context)
            view.tag = CallResultDialogFragment.TAG
            view.addViewElements(
                it,
                SecuredPreference.getIsTranslationEnabled(),
                viewModel.callResultHashMap,
                Pair(DefinedParams.CALL_RESULT, null),
                FormLayout(viewType = "", id = "", title = "", visibility = "", optionsList = null),
                callResultSelectionCallback,
            )
            binding.selectionCallResult.addView(view)
        }
        enableForSuccessFul()
        showPatientStatusForSuccess()
        binding.btnDone.safeClickListener(this)
        binding.tvHealthFacilityTitle.markMandatory()
    }

    private fun setListeners() {
        binding.tvHealthFacilitySpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    view: View?,
                    pos: Int,
                    itemId: Long,
                ) {
                    adapter.getData(pos)?.let {
                        val selectedItem = adapter.getData(position = pos)
                        selectedItem?.let {
                            val selectedId = (it[DefinedParams.ID] as? Long) ?: null
                            val name = it[DefinedParams.NAME ] as String?
                            if (selectedId != DefinedParams.DEFAULT_SELECT_ID) {
                                viewModel.selectedHealthFacilityId = selectedId
                            } else {
                                viewModel.selectedHealthFacilityId = null
                            }
                            viewModel.selectedHealthFacilityName = name
                            if (name.equals(DefinedParams.OTHER, true)) {
                                binding.OtherGroup.visible()
                                binding.etOther.setText("")
                                binding.tvOtherError.gone()
                                binding.tvOtherTitle.text =
                                    getString(R.string.other_health_facility)
                                binding.etOther.hint = getString(R.string.health_facility_name)
                                binding.tvOtherTitle.markMandatory()
                            } else {
                                binding.OtherGroup.gone()
                                binding.tvOtherError.gone()
                            }
                        }
                        enableForSuccessFul()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    private fun loadSiteDetails(data: ArrayList<HealthFacilityEntity>?) {
        val list = arrayListOf<Map<String, Any>>(
            hashMapOf(
                DefinedParams.NAME to DefinedParams.DEFAULT_ID_LABEL,
                DefinedParams.ID to DefinedParams.DEFAULT_SELECT_ID,
            ),
        )
        data
            ?.map { site ->
                hashMapOf(
                    DefinedParams.ID to site.id,
                    DefinedParams.NAME to site.name,
                    DefinedParams.TENANT_ID to site.tenantId,
                    DefinedParams.FhirId to (site.fhirId ?: 0),
                )
            }?.let { list.addAll(it) }
        list.add(hashMapOf(DefinedParams.NAME to DefinedParams.OTHER))
        adapter.setData(list)
        binding.tvHealthFacilitySpinner.post {
            binding.tvHealthFacilitySpinner.setSelection(0, false)
        }
        binding.tvHealthFacilitySpinner.adapter = adapter
    }

    private fun initializeTagView() {
        reasonListCustomView = TagListCustomView(
            requireContext(),
            binding.cgReason,
            otherSingleSelect = true,
            callBack = { name, _, _ ->
                if (reasonListCustomView
                        .getSelectedTags()
                        .firstOrNull { it.name.equals(DefinedParams.OTHER, true) } != null
                ) {
                    binding.OtherGroup.visible()
                    binding.etOther.setText("")
                    binding.tvOtherError.gone()
                    binding.tvOtherTitle.text = getString(R.string.other_reason)
                    binding.etOther.hint = getString(R.string.other_reason)
                    binding.tvOtherTitle.markMandatory()
                } else {
                    binding.OtherGroup.gone()
                    binding.tvOtherError.gone()
                }
                enableForSuccessFul()
            },
        )
    }

    private fun loadDeleteReasonList(list: List<ShortageReasonEntity>) {
        val chipItems = ArrayList<ChipViewItemModel>()
        list.forEachIndexed { index, element ->
            chipItems.add(
                ChipViewItemModel(
                    id = (index + 1).toLong(),
                    name = element.name,
                    cultureValue = element.displayValue,
                    type = element.type,
                    value = element.value,
                ),
            )
        }
        reasonListCustomView.addChipItemList(chipItems, null, diagnosisGrouping(chipItems))
    }

    private fun diagnosisGrouping(list: List<ChipViewItemModel>?): HashMap<String, MutableList<ChipViewItemModel>>? =
        list?.groupByTo(HashMap(), {
            it.type.toString()
        }, { it })

    private fun enableForSuccessFul() {
        val isCallResultEnabled = viewModel.callResultHashMap.isNotEmpty()
        val isReasonEnabled = viewModel.patientStatusHashMap.isNotEmpty()

        val patientStatus = viewModel.patientStatusHashMap[DefinedParams.PATIENT_STATUS] as? String
        val isWontVisitFacility = patientStatus.equals(wont_visit_facility, true)
        val isVisitFacility = patientStatus.equals(visited_facility, true)

        val selectedTags = reasonListCustomView.getSelectedTags()
        val hasOtherTag = selectedTags.any { it.name.equals(DefinedParams.OTHER, true) }
        val isOtherReasonValid = hasOtherTag &&
            !binding.etOther.text
                ?.trim()
                .isNullOrBlank()

        val isWontVisitConditionValid = !isWontVisitFacility ||
            (isWontVisitFacility && (selectedTags.isEmpty() || !hasOtherTag || isOtherReasonValid))

        val isHealthFacilitySelected = viewModel.selectedHealthFacilityId != null &&
            !viewModel.selectedHealthFacilityName.isNullOrBlank() &&
            !viewModel.selectedHealthFacilityName.equals(DefinedParams.DEFAULT_ID_LABEL, true)

        val isOtherHealthFacility = viewModel.selectedHealthFacilityId == null &&
            !viewModel.selectedHealthFacilityName.isNullOrBlank() &&
            !viewModel.selectedHealthFacilityName.equals(DefinedParams.DEFAULT_ID_LABEL, true)
        val isOtherHealthFacilityValid =
            viewModel.selectedHealthFacilityName.equals(DefinedParams.OTHER, true).not() ||
                (
                    viewModel.selectedHealthFacilityName.equals(
                        DefinedParams.OTHER,
                        true,
                    ) &&
                        !binding.etOther.text
                            ?.trim()
                            .isNullOrBlank()
                )

        val isVisitFacilityConditionValid = !isVisitFacility ||
            (isVisitFacility && ((isHealthFacilitySelected && !isOtherHealthFacility) || (isOtherHealthFacility && isOtherHealthFacilityValid)))

        binding.btnDone.isEnabled = isCallResultEnabled &&
            isReasonEnabled &&
            isWontVisitConditionValid &&
            isVisitFacilityConditionValid
    }

    private var callResultSelectionCallback: ((selectedID: Any?, elementId: Pair<String, String?>, formLayout: FormLayout, name: String?) -> Unit)? =
        { selectedID, _, _, _ ->
            val newSelection = selectedID as String
            viewModel.callResultHashMap[DefinedParams.CALL_RESULT] = newSelection
            viewModel.unSuccessfulHashMap.clear()
            viewModel.patientStatusHashMap.clear()
            if (newSelection == FollowUpCallStatus.UNSUCCESSFUL.name) {
                showUnsuccessfulReason()
                enableForUnSuccessful()
                binding.tvHealthFacilitySpinner.post {
                    binding.tvHealthFacilitySpinner.setSelection(0, false)
                }
                binding.reasonGroup.gone()
                binding.OtherGroup.gone()
                binding.etOther.setText("")
                viewModel.selectedHealthFacilityId = null
                viewModel.selectedHealthFacilityName = null
                binding.healthFacilityGroup.gone()
            } else {
                showPatientStatusForSuccess()
                enableForSuccessFul()
            }
        }

    private fun showUnsuccessfulReason() {
        binding.selectionPatientStatus.removeAllViews()
        binding.tvPatientStatus.text = getString(R.string.reason)
        getUnsuccessfulData().let {
            val view = SingleSelectionCustomView(binding.root.context)
            view.tag = CallResultDialogFragment.TAG
            view.addViewElements(
                it,
                SecuredPreference.getIsTranslationEnabled(),
                viewModel.unSuccessfulHashMap,
                Pair(DefinedParams.UN_SUCCESSFUL, null),
                FormLayout(
                    viewType = "",
                    id = "",
                    title = "",
                    visibility = "",
                    optionsList = null,
                ),
                unsuccessfulSelectionCallback,
            )
            binding.selectionPatientStatus.addView(view)
        }
    }

    private var unsuccessfulSelectionCallback: ((selectedID: Any?, elementId: Pair<String, String?>, formLayout: FormLayout, name: String?) -> Unit)? =
        { selectedID, _, _, _ ->
            viewModel.unSuccessfulHashMap[DefinedParams.UN_SUCCESSFUL] = selectedID as String
            enableForUnSuccessful()
        }

    private fun enableForUnSuccessful() {
        val isCallResultEnabled = viewModel.callResultHashMap.isNotEmpty()
        val isReasonEnabled = viewModel.unSuccessfulHashMap.isNotEmpty()

        binding.btnDone.isEnabled = isCallResultEnabled && isReasonEnabled
    }

    private fun getUnsuccessfulData(): ArrayList<Map<String, Any>> {
        val flowList = ArrayList<Map<String, Any>>()
        flowList.add(
            getOptionMap(
                FollowUpCallReason.UNREACHABLE.name,
                getString(R.string.un_reachable),
            ),
        )
        flowList.add(
            getOptionMap(
                FollowUpCallReason.WRONG_NUMBER.name,
                getString(R.string.wrong_number),
            ),
        )
        return flowList
    }

    private fun getCallResultData(): ArrayList<Map<String, Any>> {
        val flowList = ArrayList<Map<String, Any>>()
        flowList.add(
            getOptionMap(
                FollowUpCallStatus.SUCCESSFUL.name,
                getString(R.string.successful),
            ),
        )
        flowList.add(
            getOptionMap(
                FollowUpCallStatus.UNSUCCESSFUL.name,
                getString(R.string.un_successful),
            ),
        )
        return flowList
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.btnDone.id -> {
                if ((CommonUtils.isChp())) {
                    // do save flow
                    data?.let { value ->
                        callResultViewModel.getAttemptsById(value.id)
                    }
                } else {
                    val isSuccess = viewModel.callResultHashMap[DefinedParams.CALL_RESULT] == FollowUpCallStatus.SUCCESSFUL.name

                    viewModel.getPatientRegisterResponse.value?.data?.let { data ->
                        var otherReason: String? = null
                        val reason = when {
                            !isSuccess -> viewModel.unSuccessfulHashMap[DefinedParams.UN_SUCCESSFUL] as? String
                            (viewModel.patientStatusHashMap[DefinedParams.PATIENT_STATUS] as? String)
                                ?.equals(wont_visit_facility, true) == true -> {
                                val selectedTagName = reasonListCustomView.getSelectedTags().firstOrNull()?.name
                                if (selectedTagName.equals(DefinedParams.OTHER, true)) {
                                    otherReason = binding.etOther.text
                                        ?.trim()
                                        .toString()
                                    DefinedParams.OTHER
                                } else {
                                    selectedTagName
                                }
                            }
                            else -> null
                        }

                        val patientStatus = viewModel.patientStatusHashMap[DefinedParams.PATIENT_STATUS] as? String
                        val visitedFacilityId = if (patientStatus.equals(visited_facility, true)) {
                            viewModel.selectedHealthFacilityId
                        } else {
                            null
                        }

                        val otherVisitedFacilityName = if (viewModel.selectedHealthFacilityName.equals(DefinedParams.OTHER, true)) {
                            binding.etOther.text
                                ?.trim()
                                .toString()
                        } else {
                            null
                        }

                        val callDetails = CallDetails(
                            callDate = System.currentTimeMillis().convertToUtcDateTime(),
                            status = viewModel.callResultHashMap[DefinedParams.CALL_RESULT] as? String,
                            reason = reason,
                            otherReason = otherReason,
                            patientStatus = patientStatus,
                            visitedFacilityId = visitedFacilityId,
                            otherVisitedFacilityName = otherVisitedFacilityName,
                        )

                        val request = FollowUpUpdateRequest(
                            id = data.id,
                            patientId = data.patientId,
                            memberId = data.memberId,
                            type = viewModel.type,
                            villageId = data.villageId,
                            isInitiated = false,
                            provenance = ProvanceDto(),
                            followUpDetails = listOf(callDetails),
                        )
                        // Proceed with the request object
                        viewModel.updatePatientCallRegister(request)
                    }
                }
                viewModel.setAnalyticsData(
                    UserDetail.startDateTime,
                    eventName = AnalyticsDefinedParams.NCDCallResult + " " + viewModel.type,
                    isCompleted = true,
                )
            }
        }
    }

    private fun showPatientStatusForSuccess() {
        binding.selectionPatientStatus.removeAllViews()
        binding.tvPatientStatus.text = getString(R.string.current_status)
        getPatientStatusForSuccessData().let {
            val view = SingleSelectionCustomView(binding.root.context)
            view.tag = CallResultDialogFragment.TAG
            view.addViewElements(
                it,
                SecuredPreference.getIsTranslationEnabled(),
                viewModel.patientStatusHashMap,
                Pair(DefinedParams.PATIENT_STATUS, null),
                FormLayout(viewType = "", id = "", title = "", visibility = "", optionsList = null),
                patientStatusForSuccessSelectionCallback,
            )
            binding.selectionPatientStatus.addView(view)
        }
    }

    private var patientStatusForSuccessSelectionCallback: ((selectedID: Any?, elementId: Pair<String, String?>, formLayout: FormLayout, name: String?) -> Unit)? =
        { selectedID, _, _, _ ->
            viewModel.patientStatusHashMap[DefinedParams.PATIENT_STATUS] = selectedID as String
            enableForSuccessFul()
            showReason()
        }

    private fun showReason() {
        if ((viewModel.patientStatusHashMap[DefinedParams.PATIENT_STATUS] as? String)?.equals(
                wont_visit_facility,
                true,
            ) == true
        ) {
            binding.tvHealthFacilitySpinner.post {
                binding.tvHealthFacilitySpinner.setSelection(0, false)
            }
            viewModel.selectedHealthFacilityId = null
            viewModel.selectedHealthFacilityName = null
            binding.reasonGroup.visible()
            binding.OtherGroup.gone()
            viewModel.getFollowUpReasonList()
            binding.etOther.setText("")
            binding.healthFacilityGroup.gone()
        } else if ((viewModel.patientStatusHashMap[DefinedParams.PATIENT_STATUS] as? String)?.equals(
                visited_facility,
                true,
            ) == true
        ) {
            binding.tvHealthFacilitySpinner.post {
                binding.tvHealthFacilitySpinner.setSelection(0, false)
            }
            binding.reasonGroup.gone()
            binding.OtherGroup.gone()
            binding.etOther.setText("")
            viewModel.getFollowUpReasonList()
            viewModel.selectedHealthFacilityId = null
            viewModel.selectedHealthFacilityName = null
            binding.healthFacilityGroup.visible()
        } else {
            binding.tvHealthFacilitySpinner.post {
                binding.tvHealthFacilitySpinner.setSelection(0, false)
            }
            binding.reasonGroup.gone()
            binding.OtherGroup.gone()
            binding.etOther.setText("")
            viewModel.getFollowUpReasonList()
            viewModel.selectedHealthFacilityId = null
            viewModel.selectedHealthFacilityName = null
            binding.healthFacilityGroup.gone()
        }
    }

    private fun getPatientStatusForSuccessData(): ArrayList<Map<String, Any>> {
        val flowList = ArrayList<Map<String, Any>>()
        flowList.add(
            getOptionMap(
                visited_facility,
                visited_facility,
                getString(R.string.visited_facility),
            ),
        )
        flowList.add(
            getOptionMap(
                will_visit_facility,
                will_visit_facility,
                getString(R.string.will_visit_facility),
            ),
        )
        flowList.add(
            getOptionMap(
                wont_visit_facility,
                wont_visit_facility,
                getString(R.string.wont_visit_facility),
            ),
        )
        return flowList
    }
}
