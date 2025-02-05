package com.medtroniclabs.opensource.ui.mypatients.fragment

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.GridLayoutManager
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.app.analytics.model.UserDetail
import com.medtroniclabs.opensource.app.analytics.utils.AnalyticsDefinedParams
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.hideKeyboard
import com.medtroniclabs.opensource.appextensions.invisible
import com.medtroniclabs.opensource.appextensions.loadAsGif
import com.medtroniclabs.opensource.appextensions.resetImageView
import com.medtroniclabs.opensource.appextensions.setVisible
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto
import com.medtroniclabs.opensource.databinding.FragmentPatientSearchBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.model.PatientListRespModel
import com.medtroniclabs.opensource.ncd.counseling.activity.NCDCounselorActivity
import com.medtroniclabs.opensource.ncd.counseling.activity.NCDNutritionistActivity
import com.medtroniclabs.opensource.ncd.data.PatientVisitRequest
import com.medtroniclabs.opensource.ncd.medicalreview.NCDHrioBaseActivity
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMedicalReviewCMRActivity
import com.medtroniclabs.opensource.ncd.medicalreview.dialog.SortDialogFragment
import com.medtroniclabs.opensource.ncd.screening.ui.ScreeningActivity
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.BaseFragment
import com.medtroniclabs.opensource.ui.MenuConstants
import com.medtroniclabs.opensource.ui.home.AssessmentToolsActivity
import com.medtroniclabs.opensource.ui.medicalreview.addnewmember.AddNewMemberActivity
import com.medtroniclabs.opensource.ui.medicalreview.labTechnician.NCDLabTestListActivity
import com.medtroniclabs.opensource.ui.medicalreview.pharmacist.activity.NCDPharmacistActivity
import com.medtroniclabs.opensource.ui.mypatients.PatientSelectionListener
import com.medtroniclabs.opensource.ui.mypatients.PatientsListAdapter
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.PatientListViewModel
import com.medtroniclabs.opensource.ui.referralhistory.activity.ReferralHistoryActivity
import com.medtroniclabs.opensource.ncd.registration.ui.RegistrationActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PatientSearchFragment : BaseFragment(), PatientSelectionListener, View.OnClickListener {
    lateinit var binding: FragmentPatientSearchBinding
    private val patientListViewModel: PatientListViewModel by viewModels()
    private lateinit var patientsListAdapter: PatientsListAdapter
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPatientSearchBinding.inflate(layoutInflater,container,false)
        return binding.root
    }

    companion object {
        const val TAG = "PatientSearchFragment"
        fun newInstance(): PatientSearchFragment {
            return PatientSearchFragment()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        setAdapterViews()
        attachObservers()
        getPatientList()
        patientListViewModel.setUserJourney(AnalyticsDefinedParams.PatientSearch)
        binding.refreshLayout.setOnRefreshListener {
            swipeRefresh()
        }
    }

    private fun swipeRefresh() {
        withNetworkAvailability(online = {
            getPatientList()
            scrollTop()
        }, offline = {
            if (binding.refreshLayout.isRefreshing) {
                binding.refreshLayout.isRefreshing = false
            }
        })
    }

    private fun attachObservers() {
        patientListViewModel.totalPatientCount.observe(viewLifecycleOwner) { count ->
            if (!count.isNullOrBlank()) {
                val patientCount = count.toLong()
                if (patientCount > 0) {
                    binding.tvPatientCount.apply {
                        text = if(patientCount == 1L) getString(R.string.patient_found) else getString(R.string.patients_found, patientCount)
                        visibility = View.VISIBLE
                    }
                    binding.tvNoPatientsFound.gone()
                    binding.btnRegister.gone()
                } else {
                    binding.tvPatientCount.gone()
                    binding.tvNoPatientsFound.text = getString(R.string.no_patients_found)
                    val isNotShown = patientListViewModel.origin?.lowercase() == MenuConstants.ASSESSMENT.lowercase() || patientListViewModel.origin?.lowercase() == MenuConstants.REGISTRATION.lowercase()
                    if (patientListViewModel.searchText.isBlank()) {
                        binding.tvNoPatientsFound.visible()
                        if (isNotShown) {
                            binding.tvNoPatientsFound.gone()
                        }
                        binding.btnRegister.gone()
                    } else {
                        binding.tvNoPatientsFound.visible()
                        binding.btnRegister.text = when (patientListViewModel.origin?.lowercase()) {
                            MenuConstants.REGISTRATION.lowercase() -> {
                                getString(R.string.register)
                            }

                            MenuConstants.ASSESSMENT.lowercase() -> {
                                binding.tvNoPatientsFound.text =
                                    getString(R.string.no_patients_found_perform_screening)
                                getString(R.string.start_screening)
                            }

                            MenuConstants.MY_PATIENTS_MENU_ID.lowercase() -> {
                                binding.tvNoPatientsFound.text =
                                    getString(R.string.no_patients_found)
                                ""
                            }

                            else -> {
                                ""
                            }
                        }
                        binding.btnRegister.setVisible(
                            patientListViewModel.origin.equals(
                                MenuConstants.REGISTRATION,
                                true
                            ) || patientListViewModel.origin.equals(MenuConstants.ASSESSMENT, true)
                        )
                    }
                }
                binding.llSortFilter.btnSort.apply {
                    if (CommonUtils.canShowSort(patientListViewModel.origin)) {
                        visible()

                        val sortCount = patientListViewModel.sortCount()
                        text = if (sortCount > 0)
                            getString(R.string.sort_count, sortCount)
                        else
                            getString(R.string.sort)
                    } else
                        invisible()
                }
                binding.llSortFilter.btnFilter.apply {
                    if (CommonUtils.canShowFilter(patientListViewModel.origin)) {
                        visible()

                        val filterCount = patientListViewModel.filterCount()
                        text = if (filterCount > 0)
                            getString(R.string.filter_count, filterCount)
                        else
                            getString(R.string.filter)
                    } else
                        invisible()
                }
            }
            if (binding.refreshLayout.isRefreshing) {
                binding.refreshLayout.isRefreshing = false
            }
        }
        patientListViewModel.filterLiveData.observe(viewLifecycleOwner) {
            withNetworkAvailability(online = {
                if (it) {
                    getPatientList()
                    scrollTop()
                    patientListViewModel.setFilter(false)
                }
            })
        }
        patientListViewModel.sortLiveData.observe(viewLifecycleOwner) {
            withNetworkAvailability(online = {
                if (it) {
                    getPatientList()
                    scrollTop()
                    patientListViewModel.setSort(false)
                }
            })
        }
        patientListViewModel.patientVisitLiveData.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    (activity as BaseActivity).showLoading()
                }

                ResourceState.SUCCESS -> {
                    (activity as BaseActivity).hideLoading()
                    resourceState.data?.let {
                        val destinationIntent = when (patientListViewModel.origin?.lowercase()) {
                            MenuConstants.DISPENSE.lowercase() -> NCDPharmacistActivity::class.java
                            MenuConstants.LIFESTYLE.lowercase() -> NCDNutritionistActivity::class.java
                            MenuConstants.PSYCHOLOGICAL.lowercase() -> NCDCounselorActivity::class.java
                            MenuConstants.INVESTIGATION.lowercase() -> NCDLabTestListActivity::class.java
                            MenuConstants.MY_PATIENTS_MENU_ID.lowercase() -> if (it.initialReviewed == true || CommonUtils.isNutritionist()) NCDMedicalReviewCMRActivity::class.java else AssessmentToolsActivity::class.java
                            else -> null
                        }
                        destinationIntent?.let { destIntent ->
                            val intent =
                                Intent(requireContext(), destIntent).apply {
                                    putExtra(NCDMRUtil.EncounterReference, it.encounterReference)
                                    putExtra(
                                        DefinedParams.FhirId,
                                        patientListViewModel.selectedPatientDetails?.id
                                    )
                                    putExtra(
                                        DefinedParams.PatientId,
                                        patientListViewModel.selectedPatientDetails?.patientId
                                    )
                                    putExtra(DefinedParams.ORIGIN, patientListViewModel.origin)
                                    putExtra(
                                        DefinedParams.Gender,
                                        patientListViewModel.selectedPatientDetails?.gender
                                    )
                                }
                            startActivity(intent)
                        }
                    }
                }

                ResourceState.ERROR -> {
                    (activity as BaseActivity).hideLoading()
                    showErrorDialog(getString(R.string.error), getString(R.string.something_went_wrong_try_later))
                }
            }
        }
    }
    private fun initViews() {
        patientListViewModel.origin = arguments?.getString(DefinedParams.ORIGIN)
        binding.bottomCardView.gone()
        binding.llSortFilter.btnFilter.text = getString(R.string.filters)
        binding.llSortFilter.btnSort.text = getString(R.string.sort)
        val tabletSize =
            resources.getBoolean(R.bool.isLargeTablet) || resources.getBoolean(R.bool.isTablet)
        if (tabletSize) {
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            patientListViewModel.spanCount = DefinedParams.span_count_3
        } else {
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            patientListViewModel.spanCount = DefinedParams.span_count_1
        }
        binding.llExactSearch.etPatientSearch.addTextChangedListener(searchListener)
        binding.llExactSearch.btnSearch.safeClickListener(this)
        binding.btnRegister.safeClickListener(this)
        binding.btnScreening.safeClickListener(this)
        binding.llSortFilter.btnFilter.safeClickListener(this)
        binding.llSortFilter.btnSort.safeClickListener(this)
        binding.loadingProgress.safeClickListener(this)
        binding.btnAddNewMember.safeClickListener(this)
        binding.btnAddNewMember.visibility = if (CommonUtils.isCommunity()) View.VISIBLE else View.GONE

        binding.llExactSearch.etPatientSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                if (binding.llExactSearch.btnSearch.isEnabled)
                    binding.llExactSearch.btnSearch.performClick()
                true
            } else
                false
        }
    }

    private val searchListener = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            /**
             * this method is not used
             */
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            /**
             * this method is not used
             */
        }

        override fun afterTextChanged(s: Editable?) {
            val enteredChar = s?.toString() ?: ""
            val trimmedChar = enteredChar?.trim()
            val hasString = !trimmedChar.isNullOrBlank()
            binding.llExactSearch.btnSearch.isEnabled = hasString
            if (enteredChar.isEmpty() && !hasString) {
                handleSearchBarAfterTextRemove()
            }
        }
    }

    private fun handleSearchBarAfterTextRemove() {
        withNetworkAvailability(online = {
            patientListViewModel.searchText = ""
            getPatientList()
        })
    }

    private fun setAdapterViews() {
        patientsListAdapter = PatientsListAdapter(this)
        binding.rvPatientsList.apply {
            layoutManager =
                GridLayoutManager(requireContext(), patientListViewModel.spanCount)
            adapter = patientsListAdapter
        }
        patientsListAdapter.addLoadStateListener {
            val isLoading = it.refresh is LoadState.Loading
            if (isLoading) showLoading()
            else hideLoading()
            if (it.append is LoadState.Loading) {
                binding.pageProgress.visible()
            } else {
                binding.pageProgress.gone()
            }
        }
    }

    override fun onSelectedPatient(item: PatientListRespModel) {
        withNetworkAvailability(online = {
            if (CommonUtils.isCommunity()) {
                val intent = Intent(requireActivity(), ReferralHistoryActivity::class.java)
                intent.putExtra(DefinedParams.PatientId, item.patientId)
                intent.putExtra(DefinedParams.Gender, item.gender)
                intent.putExtra(DefinedParams.DOB, item.birthDate)
                intent.putExtra(DefinedParams.FhirId, item.id)
                intent.putExtra(DefinedParams.ORIGIN, patientListViewModel.origin)
                startActivity(intent)
            } else {
                val origin = patientListViewModel.origin?.lowercase()
                val destinationIntent = when (origin) {
                    MenuConstants.REGISTRATION.lowercase() -> RegistrationActivity::class.java
                    MenuConstants.ASSESSMENT.lowercase() -> AssessmentToolsActivity::class.java
                    MenuConstants.INVESTIGATION.lowercase(),
                    MenuConstants.LIFESTYLE.lowercase(),
                    MenuConstants.PSYCHOLOGICAL.lowercase(),
                    MenuConstants.MY_PATIENTS_MENU_ID.lowercase(),
                    MenuConstants.DISPENSE.lowercase() -> {
                        if (MenuConstants.MY_PATIENTS_MENU_ID.lowercase() == origin && (CommonUtils.isNonCommunity() && CommonUtils.isNURSE())) {
                            NCDMedicalReviewCMRActivity::class.java
                        } else if (MenuConstants.MY_PATIENTS_MENU_ID.lowercase() == origin && (CommonUtils.isNonCommunity() && CommonUtils.isHRIO())) {
                             NCDHrioBaseActivity::class.java
                        } else {
                            patientListViewModel.selectedPatientDetails = item
                            withNetworkAvailability(online = {
                                patientListViewModel.createPatientVisit(
                                    PatientVisitRequest(
                                        patientReference = item.patientId,
                                        provenance = ProvanceDto(
                                        ),
                                        memberReference = item.id
                                    )
                                )
                            })
                            null
                        }
                    }

                    else -> null
                }
                destinationIntent?.let { destIntent ->
                    val intent = Intent(requireContext(), destIntent)
                    intent.putExtra(DefinedParams.FhirId, item.id)
                    intent.putExtra(DefinedParams.PatientId, item.patientId)
                    intent.putExtra(DefinedParams.ORIGIN, patientListViewModel.origin)
                    intent.putExtra(DefinedParams.Gender,item.gender)
                    startActivity(intent)
                }
            }
        })
    }

    private fun getPatientList() {
        viewLifecycleOwner.lifecycleScope.launch {
            patientListViewModel.patientsDataSource.collectLatest { pagedData ->
                patientsListAdapter.submitData(pagedData)
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.llExactSearch.btnSearch.id -> {
                requireContext().hideKeyboard(v)
                networkAvailability()
            }

            binding.llSortFilter.btnFilter.id -> {
                requireContext().hideKeyboard(v)
                handleFilterClick()
            }
            binding.llSortFilter.btnSort.id -> {
                requireContext().hideKeyboard(v)
                handleSortClick()
            }
            binding.loadingProgress.id -> {}

            binding.btnAddNewMember.id ->{
                val intent = Intent(requireContext(), AddNewMemberActivity::class.java)
                startActivity(intent)
            }
            binding.btnRegister.id -> {
                if (patientListViewModel.origin.equals(MenuConstants.REGISTRATION, true)) {
                    val intent = Intent(requireContext(), RegistrationActivity::class.java)
                    startActivity(intent)
                } else if (patientListViewModel.origin.equals(MenuConstants.ASSESSMENT, true)) {
                    val intent = Intent(requireContext(), ScreeningActivity::class.java)
                    startActivity(intent)
                }
            }
        }
    }

    private fun networkAvailability() {
        withNetworkAvailability(online = {
            patientListViewModel.setAnalyticsData(
                UserDetail.startDateTime,
                eventName = AnalyticsDefinedParams.NCDSearchPatient + " " + patientListViewModel.origin.takeIf { !it.isNullOrBlank() },
                isCompleted = true
            )
            patientListViewModel.searchText =
                binding.llExactSearch.etPatientSearch.text?.trim().toString()
            getPatientList()
            scrollTop()
        })
    }

    private fun scrollTop() {
        binding.rvPatientsList.scrollToPosition(0)
    }

    private fun handleFilterClick() {
        val existingFragment =
            childFragmentManager.findFragmentByTag(PatientSearchFilterDialog.TAG) as? PatientSearchFilterDialog
        if (existingFragment == null) {
            PatientSearchFilterDialog.newInstance(patientListViewModel.origin)
                .show(childFragmentManager, PatientSearchFilterDialog.TAG)
        } else {
            existingFragment.show(childFragmentManager, PatientSearchFilterDialog.TAG)
        }
    }

    private fun handleSortClick() {
        val existingFragment =
            childFragmentManager.findFragmentByTag(SortDialogFragment.TAG) as? SortDialogFragment
        if (existingFragment == null) {
            SortDialogFragment.newInstance()
                .show(childFragmentManager, SortDialogFragment.TAG)
        } else {
            existingFragment.show(childFragmentManager, SortDialogFragment.TAG)
        }
    }

    override fun onResume() {
        super.onResume()
        withNetworkAvailability(online = {
            getPatientList()
        })
    }

    private fun showLoading() {
        binding.loadingProgress.visible()
        binding.loaderImage.apply {
            loadAsGif(R.drawable.loader_spice)
        }
    }

    private fun hideLoading() {
        binding.loadingProgress.gone()
        binding.loaderImage.apply {
            resetImageView()
        }
    }
}