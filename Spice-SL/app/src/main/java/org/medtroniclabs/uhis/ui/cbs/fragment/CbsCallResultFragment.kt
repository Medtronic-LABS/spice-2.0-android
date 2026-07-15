package org.medtroniclabs.uhis.ui.cbs.fragment

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.postError
import org.medtroniclabs.uhis.common.CommonUtils.getOptionMap
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.DefinedParams.PEER_SUPERVISOR
import org.medtroniclabs.uhis.common.DefinedParams.PHU
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.CbsCallResult
import org.medtroniclabs.uhis.data.CbsFollowUp
import org.medtroniclabs.uhis.data.offlinesync.model.FollowUpCallStatus
import org.medtroniclabs.uhis.databinding.FragmentBottomCallResultDialogBinding
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.model.FormLayout
import org.medtroniclabs.uhis.formgeneration.ui.SingleSelectionCustomView
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.assessment.viewmodel.AssessmentViewModel

class CbsCallResultFragment : BottomSheetDialogFragment(), View.OnClickListener {
    private lateinit var binding: FragmentBottomCallResultDialogBinding
    private val viewModel: AssessmentViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentBottomCallResultDialogBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return binding.root
    }

    override fun getTheme(): Int = R.style.DialogStyle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
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
        initView()
        attachObservers()
    }

    companion object {
        const val TAG = "CbsCallResultFragment"

        fun newInstance(type: String) =
            CbsCallResultFragment().apply {
                val bundle = Bundle().apply {
                    putString(DefinedParams.type, type)
                }
                arguments = bundle
            }
    }

    private fun attachObservers() {
        viewModel.callResultSaveLiveData.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {}
                ResourceState.SUCCESS -> {
                    resourceState?.data?.let {
                        dismiss()
                        viewModel.callResultSaveLiveData.postError()
                    }
                }
                ResourceState.ERROR -> {}
            }
        }
    }

    private fun initView() {
        val type = arguments?.getString(DefinedParams.type)
        viewModel.callResultHashMap[DefinedParams.CALL_RESULT] =
            if (!type.isNullOrBlank() &&
                type.equals(
                    DefinedParams.ps,
                    true,
                )
            ) {
                getString(R.string.informed_ps)
            } else {
                getString(R.string.informed_phu)
            }
        getCallResultData().let {
            val view = SingleSelectionCustomView(binding.root.context)
            view.tag = TAG
            view.addViewElements(
                it,
                false,
                viewModel.callResultHashMap,
                Pair(DefinedParams.CALL_RESULT, null),
                FormLayout(viewType = "", id = "", title = "", visibility = "", optionsList = null),
                callResultSelectionCallback,
            )
            binding.selectionCallResult.addView(view)
        }
        binding.tvPatientStatus.gone()
        binding.selectionPatientStatus.gone()
        binding.btnDone.isEnabled = true
        binding.btnDone.safeClickListener(this)
    }

    private fun getCallResultData(): ArrayList<Map<String, Any>> {
        val type = arguments?.getString(DefinedParams.type)
        val options = if (!type.isNullOrBlank() && type.equals(DefinedParams.ps, true)) {
            listOf(R.string.informed_ps, R.string.ps_not_reachable)
        } else {
            listOf(R.string.informed_phu, R.string.phu_not_reachable)
        }
        return ArrayList(options.map { getOptionMap(getString(it), getString(it)) })
    }

    private var callResultSelectionCallback: ((selectedID: Any?, elementId: Pair<String, String?>, formLayout: FormLayout, name: String?) -> Unit)? =
        { selectedID, _, _, _ ->
            val newSelection = selectedID as String
            viewModel.callResultHashMap[DefinedParams.CALL_RESULT] = newSelection
        }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.btnDone.id -> {
                saveCallResult()
            }
        }
    }

    private fun saveCallResult() {
        viewModel.assessmentSaveLiveData.value?.data?.let { saveData ->
            val data = saveData.second
            val gson = Gson()
            val type = object : TypeToken<CbsFollowUp>() {}.type

            // Convert callResult JSON to list, handling null safely
            val callResults: CbsFollowUp = data.callResult?.let { json ->
                gson.fromJson(json, type)
            } ?: CbsFollowUp()

            // Determine the type and result status
            val typeValue = arguments?.getString(DefinedParams.type)?.trim()?.lowercase()
            val isPsType = typeValue.equals(DefinedParams.ps, true)

            val selectedResult = viewModel.callResultHashMap[DefinedParams.CALL_RESULT] as? String

            val followUpStatus = when {
                !selectedResult.isNullOrBlank() &&
                    selectedResult.equals(
                        getString(if (isPsType) R.string.informed_ps else R.string.informed_phu),
                        true,
                    ) -> FollowUpCallStatus.SUCCESSFUL
                else -> FollowUpCallStatus.UN_SUCCESSFUL
            }

            val psAttempts = callResults.followUpDetails.filter { it.reason == PEER_SUPERVISOR }.size + 1
            val phuAttempts = callResults.followUpDetails.filter { it.reason == PHU }.size + 1

            val followUpReason = if (isPsType) {
                Pair(PEER_SUPERVISOR, psAttempts)
            } else {
                Pair(PHU, phuAttempts)
            }

            val lat = SecuredPreference.getDouble(SecuredPreference.EnvironmentKey.CURRENT_LATITUDE.name)
            val lng = SecuredPreference.getDouble(SecuredPreference.EnvironmentKey.CURRENT_LONGITUDE.name)

            // Add new CbsCallResult entry
            callResults.followUpDetails.add(
                CbsCallResult(
                    duration = 0,
                    attempts = followUpReason.second,
                    status = followUpStatus.name,
                    reason = followUpReason.first,
                    latitude = lat,
                    longitude = lng,
                ),
            )

            // Update callResult JSON and save
            data.apply { callResult = gson.toJson(callResults) }
            viewModel.saveCallResult(saveData.first, data)
        }
    }
}
