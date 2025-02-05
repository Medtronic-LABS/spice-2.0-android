package com.medtroniclabs.opensource.ncd.followup.fragment

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.postError
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.databinding.FragmentNcdFollowUpOfflineSearchBinding
import com.medtroniclabs.opensource.db.entity.NCDFollowUp
import com.medtroniclabs.opensource.mappingkey.Screening
import com.medtroniclabs.opensource.ncd.followup.NCDFollowUpUtils.setOrientationAndSpanCount
import com.medtroniclabs.opensource.ncd.followup.adapter.NCDFollowUpOfflineListAdapter
import com.medtroniclabs.opensource.ncd.followup.viewmodel.NCDFollowUpViewModel
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseFragment
import com.medtroniclabs.opensource.ui.MenuConstants
import com.medtroniclabs.opensource.ui.home.AssessmentToolsActivity
import com.medtroniclabs.opensource.ui.mypatients.PatientSelectionListenerForFollowUpOffline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NCDFollowUpOfflineSearchFragment : BaseFragment(),
    PatientSelectionListenerForFollowUpOffline {

    private lateinit var binding: FragmentNcdFollowUpOfflineSearchBinding
    private val viewModel: NCDFollowUpViewModel by activityViewModels()
    private val followUpAdapter: NCDFollowUpOfflineListAdapter by lazy {
        NCDFollowUpOfflineListAdapter(
            this
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNcdFollowUpOfflineSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        fun newInstance(type: String) =
            NCDFollowUpOfflineSearchFragment().apply {
                arguments = Bundle().apply {
                    putString(Screening.type, type)
                }
            }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        attachObserver()
    }


    private fun hideProgressAfterDelay() {
        CoroutineScope(Dispatchers.Main).launch {
            delay(1500) // Wait for 3 seconds
            hideProgress()
        }
    }

    private fun attachObserver() {
        viewModel.getFollowUpData.observe(viewLifecycleOwner) { data ->
            val villages = viewModel.filterByVillage.map { it.id?.toString().orEmpty() }
                .takeIf { it.any(String::isNotBlank) } ?: emptyList()
            val displayData = if (villages.isEmpty()) data else data.filter { it.villageId in villages }

            if (displayData.isEmpty()) {
                binding.rvPatientList.gone()
                binding.tvPatientNoFound.gone()
            } else {
                binding.rvPatientList.visible()
                binding.tvPatientNoFound.gone()
                followUpAdapter.submitData(displayData)
            }
            viewModel.totalPatientCountOffline.postValue(displayData.size)
            hideProgressAfterDelay()
        }
        viewModel.updateCallLiveData.observe(viewLifecycleOwner) { resources ->
            when (resources.state) {
                ResourceState.LOADING -> {
                  showProgress()
                }

                ResourceState.SUCCESS -> {
                    hideProgress()
                    resources.data?.let { data ->
                        data.phoneNumber?.let { phoneNumber ->
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                SecuredPreference.putBoolean(
                                    SecuredPreference.EnvironmentKey.INITIAL_CALL.name,
                                    true
                                )
                                this.data = Uri.parse("tel:${phoneNumber}")
                            }
                            startActivity(intent)
                        }
                    }
                    viewModel.updateCallLiveData.postError()
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }
            }
        }
    }

    private fun initView() {
        setOrientationAndSpanCount(
            requireActivity(),
            resources,
            viewModel,
            binding = binding,
            context = requireContext(),
            followUpAdapter = followUpAdapter
        )
    }

    override fun onSelectedPatientForCall(item: NCDFollowUp) {
        if (hasTelephonyFeature()) {
            viewModel.updateInitial(
                item.copy(
                    isInitiated = true,
                    retryAttempts = item.retryAttempts?.minus(1) ?: 0,
                    isCompleted = item.retryAttempts == 1L
                )
            )
        } else {
            showCallDialError()
        }
    }

    private fun hasTelephonyFeature(): Boolean {
        val packageManager = requireContext().packageManager
        return packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
    }

    override fun onSelectedPatientForAssessment(item: NCDFollowUp) {
        val intent = Intent(requireContext(), AssessmentToolsActivity::class.java)
        intent.putExtra(DefinedParams.FhirId, item.memberId)
        intent.putExtra(DefinedParams.PatientId, item.patientId)
        intent.putExtra(MenuConstants.FOLLOW_UP, true)
        intent.putExtra(DefinedParams.ORIGIN, MenuConstants.ASSESSMENT.lowercase())
        intent.putExtra(DefinedParams.Gender, item.gender)
        startActivity(intent)
    }


    override fun onSelectedPatientCard(item: NCDFollowUp) {
        viewModel.selectedFollowUpPatient = item
        val fragment = childFragmentManager.findFragmentByTag(NCDFollowUpDialogFragment.TAG)
        if (fragment == null) {
            NCDFollowUpDialogFragment.newInstance(this)
                .show(childFragmentManager, NCDFollowUpDialogFragment.TAG)
        }
    }

    override fun onResume() {
        super.onResume()
        showProgress()
        followUpAdapter.submitData(listOf())
        viewModel.searchLiveDataForOffline(
            if (viewModel.searchTextOffline.isNotBlank()) viewModel.searchTextOffline else ""
        )
        if (SecuredPreference.getBoolean(SecuredPreference.EnvironmentKey.INITIAL_CALL.name)) {
            viewModel.getInitial()
        }
    }
}