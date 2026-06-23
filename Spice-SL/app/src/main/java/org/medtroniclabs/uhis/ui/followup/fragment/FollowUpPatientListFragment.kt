package org.medtroniclabs.uhis.ui.followup.fragment

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.fragment.app.activityViewModels
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams.callButtonClicked
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsUtils
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.databinding.FragmentFollowUpMyPatientListBinding
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.MenuConstants
import org.medtroniclabs.uhis.ui.followup.FollowUpDefinedParams.FU_TYPE_HH_VISIT
import org.medtroniclabs.uhis.ui.followup.adapter.PatientListAdapter
import org.medtroniclabs.uhis.ui.followup.viewmodel.FollowUpViewModel
import org.medtroniclabs.uhis.ui.home.AssessmentToolsActivity
import org.medtroniclabs.uhis.ui.household.summary.MemberSummaryActivity

class FollowUpPatientListFragment : BaseFragment() {
    private lateinit var binding: FragmentFollowUpMyPatientListBinding
    private val viewModel: FollowUpViewModel by activityViewModels()
    private lateinit var adapter: PatientListAdapter

    companion object {
        const val TAG = "FollowUpPatientListFragment"
    }

    private val dialerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK || result.resultCode == RESULT_CANCELED) {
                viewModel.callEndTime = System.currentTimeMillis()
                CallResultDialogFragment
                    .newInstance(DefinedParams.SCREENED)
                    .show(childFragmentManager, CallResultDialogFragment.TAG)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentFollowUpMyPatientListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initAdapter()
        attachListener()
    }

    private fun attachListener() {
        viewModel.followUpPatientListLiveData.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                binding.tvPatientNoFound.gone()
                binding.rvPatientList.visible()
                adapter.updateList(it)
            } else {
                binding.tvPatientNoFound.visible()
                binding.rvPatientList.gone()
                adapter.updateList(listOf())
            }
        }

        viewModel.referralDayLimitLiveData.observe(viewLifecycleOwner) {
            adapter.updateReferralDayLimit(it)
        }
        viewModel.triggerCallLiveData.observe(viewLifecycleOwner) {
            if (it) {
                onCallClicked()
                viewModel.callTriggered()
            }
        }
    }

    private fun initAdapter() {
        adapter = PatientListAdapter { index, data ->
            viewModel.selectedFollowUpDetail = data
            when (index) {
                PatientListAdapter.ConstantPatientListAdapter.PATIENT_DETAIL -> {
                    if (data.type == FU_TYPE_HH_VISIT) {
                        val intent = Intent(requireContext(), MemberSummaryActivity::class.java)
                        intent.putExtra(DefinedParams.HOUSEHOLD_ID, data.householdId)
                        intent.putExtra(DefinedParams.MEMBER_ID, data.localPatientId)
                        intent.putExtra(DefinedParams.DOB, data.dateOfBirth)
                        startActivity(intent)
                    } else {
                        data.toPatientHistoryData(requireContext()).takeIf { it.isNotEmpty() }?.let { historyData ->
                            val dialog = PatientDetailHistoryDialogFragment.newInstance(historyData, true)
                            dialog.show(childFragmentManager, PatientDetailHistoryDialogFragment::class.simpleName)
                        }
                    }
                }

                PatientListAdapter.ConstantPatientListAdapter.CALL -> {
                    onCallClicked()
                }

                PatientListAdapter.ConstantPatientListAdapter.ASSESSMENT -> {
                    onLaunchAssessment()
                }
            }
        }

        binding.rvPatientList.adapter = adapter
    }

    fun onCallClicked() {
        viewModel.callStartTime = System.currentTimeMillis()
        SecuredPreference.putString(DefinedParams.FollowUpStartTiming, AnalyticsUtils.getCurrentDateTimeInLocalTime())
        viewModel.selectedFollowUpDetail?.let { data ->
            data.phoneNumber?.let { phoneNumber ->
                val dialIntent = Intent(Intent.ACTION_DIAL)
                dialIntent.data = "tel:$phoneNumber".toUri()
                dialerLauncher.launch(dialIntent)
                viewModel.setUserJourney(callButtonClicked)
            }
        }
    }

    fun onLaunchAssessment() {
        viewModel.selectedFollowUpDetail?.let { data ->
            if (data.householdId != null && data.householdId != 0L) {
                val intent = Intent(requireContext(), AssessmentToolsActivity::class.java)
                intent.putExtra(DefinedParams.MEMBER_ID, data.localPatientId)
                intent.putExtra(DefinedParams.FOLLOW_UP_ID, data.id)
                intent.putExtra(MenuConstants.FOLLOW_UP, true)
                intent.putExtra(DefinedParams.DOB, data.dateOfBirth)
                startActivity(intent)
                viewModel.setUserJourney(AnalyticsDefinedParams.STARTASSESSMENTTRIGGERED)
            } else {
                showErrorDialogue(
                    getString(R.string.alert),
                    getString(R.string.warning_link_to_household),
                    isNegativeButtonNeed = false,
                ) { _ -> }
            }
        }
    }
}
