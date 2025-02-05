package com.medtroniclabs.opensource.ncd.followup.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto
import com.medtroniclabs.opensource.databinding.FragmentFollowUpSearchBinding
import com.medtroniclabs.opensource.mappingkey.Screening
import com.medtroniclabs.opensource.ncd.data.FollowUpUpdateRequest
import com.medtroniclabs.opensource.ncd.data.PatientFollowUpEntity
import com.medtroniclabs.opensource.ncd.followup.NCDFollowUpUtils
import com.medtroniclabs.opensource.ncd.followup.adapter.NCDPatientFollowUPListAdapter
import com.medtroniclabs.opensource.ncd.followup.viewmodel.NCDFollowUpLostViewModel
import com.medtroniclabs.opensource.ncd.followup.viewmodel.NCDFollowUpViewModel
import com.medtroniclabs.opensource.ui.BaseFragment
import com.medtroniclabs.opensource.ui.mypatients.PatientSelectionListenerForFollowUp

class NCDFollowUpLostFragment : BaseFragment(), PatientSelectionListenerForFollowUp {

    companion object {
        const val TAG = "NCDFollowUpLostFragment"
        fun newInstance(type: String) =
            NCDFollowUpLostFragment().apply {
                arguments = Bundle().apply {
                    putString(Screening.type, type)
                }
            }
    }

    private lateinit var binding: FragmentFollowUpSearchBinding
    private val followUpAdapter: NCDPatientFollowUPListAdapter by lazy {
        NCDPatientFollowUPListAdapter(
            this
        )
    }

    private val viewModel: NCDFollowUpViewModel by activityViewModels()
    private val followUpViewModel: NCDFollowUpLostViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentFollowUpSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        attachObservers()
        setAdapterViews()
    }

    private fun attachObservers() {
        followUpViewModel.searchLiveData.observe(viewLifecycleOwner) {
            getPatientList()
        }
        followUpViewModel.triggerGetStatus.observe(viewLifecycleOwner) {
            getPatientList()
        }
    }

    private fun initView() {
        NCDFollowUpUtils.setOrientationAndSpanCountForOffline(
            requireActivity(),
            resources,
            viewModel,
            binding = binding,
            context = requireContext(),
            followUpAdapter = followUpAdapter
        )
    }

    private fun setAdapterViews() {
        NCDFollowUpUtils.setupAdapterLoadStateListener(
            adapter = followUpAdapter,
            requireContext(),
            showProgress = { showProgress() },
            hideProgress = { hideProgress() },
            showPageProgress = { binding.pageProgress.visible() },
            hidePageProgress = { binding.pageProgress.gone() },
            showError = { title, message ->
                showErrorDialog(title = title, message = message)
            },
            postError = { viewModel.totalPatientCount.postValue(null) }
        )
    }

    override fun onSelectedPatientForCall(item: PatientFollowUpEntity) {
        if (NCDFollowUpUtils.hasTelephonyFeature(requireContext())) {
            item.id?.let { id ->
                val request = FollowUpUpdateRequest(
                    id = id,
                    patientId = item.patientId,
                    attempts = null,
                    type = viewModel.type,
                    referredSiteId = item.referredSiteId,
                    villageId = item.villageId,
                    memberId = item.memberId,
                    isInitiated = true,
                    phoneNumber = item.phoneNumber,
                    provenance = ProvanceDto()
                )
                withNetworkAvailability(online = {
                    viewModel.updatePatientCallRegister(request)
                })
            }
        } else {
            showCallDialError()
        }
    }

    override fun onSelectedPatientForAssessment(item: PatientFollowUpEntity) {
        withNetworkAvailability(online = { launchAssessment(item, requireContext()) })
    }

    override fun onSelectedPatientCard(item: PatientFollowUpEntity) {
        viewModel.selectedPatient = item
        launchPatientDetailsDialog(this)
    }

    private fun getPatientList() {
        NCDFollowUpUtils.submitEmptyList(followUpAdapter, viewLifecycleOwner)
        NCDFollowUpUtils.collectPagedData(
            lifecycleOwner = viewLifecycleOwner,
            pagingDataFlow = viewModel.patientsDataSource,
            adapter = followUpAdapter
        )
    }
}