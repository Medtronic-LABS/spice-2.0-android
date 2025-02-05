package com.medtroniclabs.opensource.ui.followup.fragment

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.DefinedParams.CallResult
import com.medtroniclabs.opensource.common.DefinedParams.PatientStatus
import com.medtroniclabs.opensource.common.DefinedParams.UnSuccessful
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.data.offlinesync.model.FollowUpCallReason
import com.medtroniclabs.opensource.data.offlinesync.model.FollowUpCallStatus
import com.medtroniclabs.opensource.databinding.FragmentBottomCallResultDialogBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.formgeneration.model.FormLayout
import com.medtroniclabs.opensource.formgeneration.ui.SingleSelectionCustomView
import com.medtroniclabs.opensource.ui.assessment.referrallogic.utils.ReferralStatus
import com.medtroniclabs.opensource.ui.followup.viewmodel.FollowUpViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CallResultDialogFragment : BottomSheetDialogFragment(), View.OnClickListener {

    private lateinit var binding: FragmentBottomCallResultDialogBinding
    private val viewModel: FollowUpViewModel by activityViewModels()

    companion object {
        const val TAG = "CallResultDialogFragment"
        fun newInstance(): CallResultDialogFragment {
            return CallResultDialogFragment()
        }
    }

    override fun getTheme(): Int {
        return R.style.DialogStyle
    }

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
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentBottomCallResultDialogBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    private fun initView() {
        viewModel.callResultHashMap[CallResult] = FollowUpCallStatus.SUCCESSFUL.name
        getCallResultData().let {
            val view = SingleSelectionCustomView(binding.root.context)
            view.tag = TAG
            view.addViewElements(
                it,
                SecuredPreference.getIsTranslationEnabled(),
                viewModel.callResultHashMap,
                Pair(CallResult,null),
                FormLayout(viewType = "", id = "", title = "", visibility = "", optionsList = null),
                callResultSelectionCallback
            )
            binding.selectionCallResult.addView(view)
        }

        showPatientStatus()
        enableForSuccessFul()

        binding.btnDone.safeClickListener(this)
    }

    private var callResultSelectionCallback: ((selectedID: Any?, elementId: Pair<String,String?>, serverViewModel: FormLayout, name: String?) -> Unit)? =
        { selectedID, _, _, _ ->
            val newSelection = selectedID as String
            val lastSelection = viewModel.callResultHashMap[CallResult]

            if (lastSelection != null) {
                viewModel.patientStatusHashMap.clear()
                viewModel.unSuccessfulHashMap.clear()
            }

            if (lastSelection != newSelection) {
                viewModel.callResultHashMap[CallResult] = newSelection
                if (newSelection == FollowUpCallStatus.UNSUCCESSFUL.name) {
                    showUnsuccessfulReason()
                    enableForUnSuccessful()
                } else {
                    showPatientStatus()
                    enableForSuccessFul()
                }
            }
        }

    private var unsuccessfulSelectionCallback: ((selectedID: Any?, elementId: Pair<String,String?>, serverViewModel: FormLayout, name: String?) -> Unit)? =
        { selectedID, _, _, _ ->
            viewModel.unSuccessfulHashMap[UnSuccessful] = selectedID as String
            enableForUnSuccessful()
        }

    private var patientStatusSelectionCallback: ((selectedID: Any?, elementId: Pair<String,String?>, serverViewModel: FormLayout, name: String?) -> Unit)? =
        { selectedID, _, _, _ ->
            viewModel.patientStatusHashMap[PatientStatus] = selectedID as String
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
                getString(R.string.successful)
            )
        )
        flowList.add(
            getOptionMap(
                FollowUpCallStatus.UNSUCCESSFUL.name,
                getString(R.string.un_successful)
            )
        )
        return flowList
    }

    private fun getPatientStatusData(): ArrayList<Map<String, Any>> {
        val flowList = ArrayList<Map<String, Any>>()
        flowList.add(getOptionMap(ReferralStatus.Recovered.name, getString(R.string.recovered)))
        if (viewModel.selectedFollowUpDetail?.type?.equals(
                ReferralStatus.Referred.name,
                true
            ) != true
        ) {
            flowList.add(
                getOptionMap(
                    ReferralStatus.OnTreatment.name,
                    getString(R.string.on_treatment)
                )
            )
        }

        flowList.add(getOptionMap(ReferralStatus.Referred.name, getString(R.string.referred)))
        return flowList
    }

    private fun getUnsuccessfulData(): ArrayList<Map<String, Any>> {
        val flowList = ArrayList<Map<String, Any>>()
        flowList.add(
            getOptionMap(
                FollowUpCallReason.UNREACHABLE.name,
                getString(R.string.un_reachable)
            )
        )
        flowList.add(
            getOptionMap(
                FollowUpCallReason.WRONG_NUMBER.name,
                getString(R.string.wrong_number)
            )
        )
        return flowList
    }

    private fun getOptionMap(id: String, value: String): Map<String, Any> {
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
            }
        }
    }

    private fun showPatientStatus() {
        if (viewModel.selectedFollowUpDetail?.type?.equals(ReferralStatus.Referred.name, true) == true) {
            viewModel.patientStatusHashMap[PatientStatus] = ReferralStatus.Referred.name
        } else {
            viewModel.patientStatusHashMap[PatientStatus] = ReferralStatus.OnTreatment.name
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
                Pair(PatientStatus,null),
                FormLayout(viewType = "", id = "", title = "", visibility = "", optionsList = null),
                patientStatusSelectionCallback
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
                Pair(UnSuccessful,null),
                FormLayout(
                    viewType = "",
                    id = "",
                    title = "",
                    visibility = "",
                    optionsList = null
                ),
                unsuccessfulSelectionCallback
            )
            binding.selectionPatientStatus.addView(view)
        }
    }
}