package org.medtroniclabs.uhis.ui.followup.fragment

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams.CallResultDialogue
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.DefinedParams.CALL_RESULT
import org.medtroniclabs.uhis.common.DefinedParams.HIV
import org.medtroniclabs.uhis.common.DefinedParams.PATIENT_STATUS
import org.medtroniclabs.uhis.common.DefinedParams.UNSUCCESSFUL
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.offlinesync.model.FollowUpCallReason
import org.medtroniclabs.uhis.data.offlinesync.model.FollowUpCallStatus
import org.medtroniclabs.uhis.databinding.FragmentBottomCallResultDialogBinding
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams.IMMUNISATION
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams.NCD
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.model.FormLayout
import org.medtroniclabs.uhis.formgeneration.ui.SingleSelectionCustomView
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams
import org.medtroniclabs.uhis.ui.assessment.referrallogic.utils.ReferralStatus
import org.medtroniclabs.uhis.ui.followup.viewmodel.FollowUpViewModel

@AndroidEntryPoint
class CallResultDialogFragment : BottomSheetDialogFragment(), View.OnClickListener {
    private lateinit var binding: FragmentBottomCallResultDialogBinding
    private val viewModel: FollowUpViewModel by activityViewModels()
    val data = setOf(NCD, IMMUNISATION, HIV)
    val familyPlanningData = setOf(AssessmentDefinedParams.Family_Planning.uppercase())

    companion object {
        const val TAG = "CallResultDialogFragment"

        fun newInstance(): CallResultDialogFragment = CallResultDialogFragment()
    }

    override fun getTheme(): Int = R.style.DialogStyle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false

        viewModel.callResultHashMap.clear()
        viewModel.patientStatusHashMap.clear()
        viewModel.unSuccessfulHashMap.clear()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        BottomSheetDialog(requireContext(), theme).apply {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentBottomCallResultDialogBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    private fun initView() {
        viewModel.setUserJourney(CallResultDialogue)
        viewModel.callResultHashMap[CALL_RESULT] = FollowUpCallStatus.SUCCESSFUL.name
        getCallResultData().let {
            val view = SingleSelectionCustomView(binding.root.context)
            view.tag = TAG
            view.addViewElements(
                it,
                SecuredPreference.getIsTranslationEnabled(),
                viewModel.callResultHashMap,
                Pair(CALL_RESULT, null),
                FormLayout(viewType = "", id = "", title = "", visibility = "", optionsList = null),
                callResultSelectionCallback,
            )
            binding.selectionCallResult.addView(view)
        }

        showPatientStatus()
        enableForSuccessFul()

        binding.btnDone.safeClickListener(this)
    }

    private var callResultSelectionCallback: ((selectedID: Any?, elementId: Pair<String, String?>, formLayout: FormLayout, name: String?) -> Unit)? =
        { selectedID, _, _, _ ->
            val newSelection = selectedID as String
            val lastSelection = viewModel.callResultHashMap[CALL_RESULT]

            if (lastSelection != null) {
                viewModel.patientStatusHashMap.clear()
                viewModel.unSuccessfulHashMap.clear()
            }

            if (lastSelection != newSelection) {
                viewModel.callResultHashMap[CALL_RESULT] = newSelection
                if (newSelection == FollowUpCallStatus.UNSUCCESSFUL.name) {
                    showUnsuccessfulReason()
                    enableForUnSuccessful()
                } else {
                    showPatientStatus()
                    enableForSuccessFul()
                }
            }
        }

    private var unsuccessfulSelectionCallback: ((selectedID: Any?, elementId: Pair<String, String?>, formLayout: FormLayout, name: String?) -> Unit)? =
        { selectedID, _, _, _ ->
            viewModel.unSuccessfulHashMap[UNSUCCESSFUL] = selectedID as String
            enableForUnSuccessful()
        }

    private var patientStatusSelectionCallback: ((selectedID: Any?, elementId: Pair<String, String?>, formLayout: FormLayout, name: String?) -> Unit)? =
        { selectedID, _, _, _ ->
            viewModel.patientStatusHashMap[PATIENT_STATUS] = selectedID as String
        }

    private fun enableForUnSuccessful() {
        val isCallResultEnabled = viewModel.callResultHashMap.isNotEmpty()
        val isReasonEnabled = viewModel.unSuccessfulHashMap.isNotEmpty()

        binding.btnDone.isEnabled = isCallResultEnabled && isReasonEnabled
    }

    private fun enableForSuccessFul() {
        binding.btnDone.isEnabled = true
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

    private fun getPatientStatusData(): ArrayList<Map<String, Any>> {
        val flowList = ArrayList<Map<String, Any>>()
        val value = viewModel.selectedFollowUpDetail?.encounterType
        if (data.contains(value)) {
            flowList.add(getOptionMap(getString(R.string.inform), getString(R.string.inform)))
            flowList.add(
                getOptionMap(
                    getString(R.string.not_inform),
                    getString(R.string.not_inform),
                ),
            )
        } else if (familyPlanningData.contains(value)) {
            flowList.add(getOptionMap(getString(R.string.visited), getString(R.string.visited)))
            flowList.add(
                getOptionMap(
                    getString(R.string.not_visited),
                    getString(R.string.not_visited),
                ),
            )
        } else {
            flowList.add(getOptionMap(ReferralStatus.Recovered.name, getString(R.string.recovered)))
            if (viewModel.selectedFollowUpDetail?.type?.equals(
                    ReferralStatus.Referred.name,
                    true,
                ) != true
            ) {
                flowList.add(
                    getOptionMap(
                        ReferralStatus.OnTreatment.name,
                        getString(R.string.on_treatment),
                    ),
                )
            }
            flowList.add(getOptionMap(ReferralStatus.Referred.name, getString(R.string.referred)))
        }
        return flowList
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

    private fun getOptionMap(
        id: String,
        value: String,
    ): Map<String, Any> {
        val map = HashMap<String, Any>()
        map[DefinedParams.ID] = id
        map[DefinedParams.NAME] = value
        return map
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.btnDone.id -> {
                viewModel.addCallHistory()
                dismiss()
                viewModel.setUserJourney(AnalyticsDefinedParams.DONEBUTTONTRIGGERED)
            }
        }
    }

    private fun showPatientStatus() {
        if (viewModel.selectedFollowUpDetail?.type?.equals(ReferralStatus.Referred.name, true) == true) {
            viewModel.patientStatusHashMap[PATIENT_STATUS] = ReferralStatus.Referred.name
        } else {
            viewModel.patientStatusHashMap[PATIENT_STATUS] = ReferralStatus.OnTreatment.name
        }
        if (data.contains(viewModel.selectedFollowUpDetail?.encounterType)) {
            viewModel.patientStatusHashMap[PATIENT_STATUS] = getString(R.string.inform)
        }
        if (familyPlanningData.contains(viewModel.selectedFollowUpDetail?.encounterType)) {
            viewModel.patientStatusHashMap[PATIENT_STATUS] = getString(R.string.visited)
        }
        binding.selectionPatientStatus.removeAllViews()
        binding.tvPatientStatus.text = getString(R.string.patient_status)
        getPatientStatusData().let {
            val view = SingleSelectionCustomView(binding.root.context)
            view.tag = TAG
            view.addViewElements(
                it,
                SecuredPreference.getIsTranslationEnabled(),
                viewModel.patientStatusHashMap,
                Pair(PATIENT_STATUS, null),
                FormLayout(viewType = "", id = "", title = "", visibility = "", optionsList = null),
                patientStatusSelectionCallback,
            )
            binding.selectionPatientStatus.addView(view)
        }
    }

    private fun showUnsuccessfulReason() {
        binding.selectionPatientStatus.removeAllViews()
        binding.tvPatientStatus.text = getString(R.string.reason)
        getUnsuccessfulData().let {
            val view = SingleSelectionCustomView(binding.root.context)
            view.tag = TAG
            view.addViewElements(
                it,
                SecuredPreference.getIsTranslationEnabled(),
                viewModel.unSuccessfulHashMap,
                Pair(UNSUCCESSFUL, null),
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
}
