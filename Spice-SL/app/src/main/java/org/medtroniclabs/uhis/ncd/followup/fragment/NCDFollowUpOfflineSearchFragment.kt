package org.medtroniclabs.uhis.ncd.followup.fragment

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.postError
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.databinding.FragmentNcdFollowUpOfflineSearchBinding
import org.medtroniclabs.uhis.db.entity.NCDFollowUp
import org.medtroniclabs.uhis.mappingkey.Screening
import org.medtroniclabs.uhis.ncd.followup.NCDFollowUpUtils.setOrientationAndSpanCount
import org.medtroniclabs.uhis.ncd.followup.adapter.NCDFollowUpOfflineListAdapter
import org.medtroniclabs.uhis.ncd.followup.viewmodel.NCDFollowUpViewModel
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.MenuConstants
import org.medtroniclabs.uhis.ui.home.AssessmentToolsActivity
import org.medtroniclabs.uhis.ui.mypatients.PatientSelectionListenerForFollowUpOffline

class NCDFollowUpOfflineSearchFragment :
    BaseFragment(),
    PatientSelectionListenerForFollowUpOffline {
    private lateinit var binding: FragmentNcdFollowUpOfflineSearchBinding
    private val viewModel: NCDFollowUpViewModel by activityViewModels()
    private val followUpAdapter: NCDFollowUpOfflineListAdapter by lazy {
        NCDFollowUpOfflineListAdapter(
            this,
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
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

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
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
            val villages = viewModel.filterByVillage
                .map { it.id?.toString().orEmpty() }
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
                                    true,
                                )
                                this.data = Uri.parse("tel:$phoneNumber")
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
            followUpAdapter = followUpAdapter,
        )
    }

    override fun onSelectedPatientForCall(item: NCDFollowUp) {
        if (hasTelephonyFeature()) {
            viewModel.updateInitial(
                item.copy(
                    isInitiated = true,
                    retryAttempts = item.retryAttempts?.minus(1) ?: 0,
                    isCompleted = item.retryAttempts == 1L,
                ),
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
        intent.putExtra(DefinedParams.GENDER, item.gender)
        startActivity(intent)
    }

    override fun onSelectedPatientCard(item: NCDFollowUp) {
        viewModel.selectedFollowUpPatient = item
        val fragment = childFragmentManager.findFragmentByTag(NCDFollowUpDialogFragment.TAG)
        if (fragment == null) {
            NCDFollowUpDialogFragment
                .newInstance(this)
                .show(childFragmentManager, NCDFollowUpDialogFragment.TAG)
        }
    }

    override fun onResume() {
        super.onResume()
        showProgress()
        followUpAdapter.submitData(listOf())
        viewModel.searchLiveDataForOffline(
            if (viewModel.searchTextOffline.isNotBlank()) viewModel.searchTextOffline else "",
        )
        if (SecuredPreference.getBoolean(SecuredPreference.EnvironmentKey.INITIAL_CALL.name)) {
            viewModel.getInitial()
        }
    }
}
