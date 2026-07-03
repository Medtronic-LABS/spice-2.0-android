package org.medtroniclabs.uhis.ui.patient.fragment

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.recyclerview.widget.GridLayoutManager
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.RoleConstant
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.common.qrscanner.QRScanContract
import org.medtroniclabs.uhis.common.qrscanner.QRScanResult
import org.medtroniclabs.uhis.common.qrscanner.QRScannerActivity
import org.medtroniclabs.uhis.data.model.FilterModel
import org.medtroniclabs.uhis.data.model.PatientDetails
import org.medtroniclabs.uhis.data.model.PatientListResModel
import org.medtroniclabs.uhis.data.model.SortModel
import org.medtroniclabs.uhis.databinding.FragmentPatientListBinding
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.ncd.medicalreview.NCDMRUtil
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.followup.fragment.PatientDetailHistoryDialogFragment
import org.medtroniclabs.uhis.ui.medicalreview.pharmacist.activity.NCDPharmacistActivity
import org.medtroniclabs.uhis.ui.patient.CallRegisterInterface
import org.medtroniclabs.uhis.ui.patient.EnrollmentFormBuilderActivity
import org.medtroniclabs.uhis.ui.patient.FilterSortInterface
import org.medtroniclabs.uhis.ui.patient.IntentConstants
import org.medtroniclabs.uhis.ui.patient.NurseMedicalReviewActivity
import org.medtroniclabs.uhis.ui.patient.PatientSelectionListener
import org.medtroniclabs.uhis.ui.patient.PrescriptionRefillActivity
import org.medtroniclabs.uhis.ui.patient.TermsAndConditionActivity
import org.medtroniclabs.uhis.ui.patient.UIConstants
import org.medtroniclabs.uhis.ui.patient.adapter.PatientsListAdapter
import org.medtroniclabs.uhis.ui.patient.viewmodel.PatientListViewModel
import timber.log.Timber
import org.medtroniclabs.uhis.common.DefinedParams as CommonDefinedParams

@AndroidEntryPoint
class PatientListFragment : BaseFragment(), PatientSelectionListener, CallRegisterInterface, FilterSortInterface {
    private lateinit var binding: FragmentPatientListBinding
    private val activityVm: PatientListViewModel by activityViewModels()
    private val fragmentVm: PatientListViewModel by viewModels()
    private lateinit var patientsListAdapter: PatientsListAdapter
    private var dialerOpened = false
    private var shouldShowPatientHistory = false

    companion object {
        const val TAG = "PatientListFragment"

        private const val ORIGIN = "ORIGIN"
        private const val SELECTED_TAB = "SELECTED_TAB"

        fun newInstance(
            origin: String,
            position: Int,
        ): PatientListFragment {
            val args = Bundle()
            args.putString(ORIGIN, origin)
            val selectedTab =
                buildList {
                    add(DefinedParams.SCREENED)
                    addAll(
                        listOf(
                            DefinedParams.OVERDUE,
                            DefinedParams.MISSED_VISIT,
                            DefinedParams.LTF,
                            DefinedParams.RED_RISK,
                        ),
                    )
                }.toTypedArray()

            if (CommonUtils.isHealthScreener() || CommonUtils.isCHCP()) {
                args.putString(SELECTED_TAB, DefinedParams.SCREENED)
            } else {
                args.putString(SELECTED_TAB, selectedTab[position])
            }

            val fragment = PatientListFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentPatientListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        setListeners()
        setObserver()
        callPatientsAPI()
    }

    fun initView() {
        binding.llExactSearch.btnSearch.isEnabled = false

        fragmentVm.apply {
            requireArguments().let {
                it.getString(ORIGIN)?.let { keyOrigin ->
                    origin = keyOrigin
                }
                it.getString(SELECTED_TAB)?.let { keySelectedTab ->
                    followUpType = if (keySelectedTab == DefinedParams.SCREENING) DefinedParams.SCREENED else keySelectedTab.uppercase().replace(" ", "_")
                }
            }
        }

        binding.btnEnrol.visibility = handleRegistrationBtnVisibility(fragmentVm.origin == UIConstants.ENROLLMENT_UNIQUE_ID)

        binding.llExactSearch.qrSearchGrp.visibility = if (fragmentVm.origin == UIConstants.FOLLOW_UP) View.GONE else View.VISIBLE
    }

    override fun onResume() {
        super.onResume()

        fragmentVm.filter = activityVm.filter

        dialerOpened = false // Reset so next event can open dialer
        if (fragmentVm.isCallRegistered.value == true && isVisible) {
            fragmentVm.isCallRegistered.postValue(false)
            callPatientsAPI()
        } else if (fragmentVm.origin == UIConstants.FOLLOW_UP) {
            getPatients()
        }
        binding.llExactSearch.etPatientSearch.text
            ?.clear()
        fragmentVm.searchPatientId = ""
    }

    private fun setListeners() {
        binding.llExactSearch.apply {
            etPatientSearch.addTextChangedListener(searchListener)
            etPatientSearch.setOnEditorActionListener(searchImeListener)
            ivQRSearch.safeClickListener(clickListener)
            btnSearch.safeClickListener(clickListener)
        }
        binding.llSortFilter.apply {
            btnFilter.safeClickListener(clickListener)
            btnSort.safeClickListener(clickListener)
        }
        binding.apply {
            btnEnrol.safeClickListener(clickListener)
            btnScreening.safeClickListener(clickListener)
        }
        setPatientListAdapter()
    }

    private val searchImeListener = object : OnEditorActionListener {
        override fun onEditorAction(
            v: TextView?,
            actionId: Int,
            event: KeyEvent?,
        ): Boolean {
            if (actionId == EditorInfo.IME_ACTION_SEARCH && binding.llExactSearch.btnSearch.isEnabled) {
                binding.llExactSearch.btnSearch.performClick()
                return true
            }
            return false
        }
    }

    private val searchListener = object : TextWatcher {
        override fun beforeTextChanged(
            s: CharSequence?,
            start: Int,
            count: Int,
            after: Int,
        ) {
            /**
             * this method is not used
             */
        }

        override fun onTextChanged(
            s: CharSequence?,
            start: Int,
            before: Int,
            count: Int,
        ) {
            /**
             * this method is not used
             */
        }

        override fun afterTextChanged(s: Editable?) {
            if (fragmentVm.isOfflineEnabled || fragmentVm.isBangladesh) {
                val hasString = (s?.trim()?.count() ?: 0) > 0
                binding.llExactSearch.btnSearch.isEnabled = hasString
                if (!hasString && fragmentVm.searchPatientId.isNotEmpty()) {
                    fragmentVm.searchPatientId = ""
                    if (fragmentVm.isPatientListRequired()) {
                        networkAvailability()
                    } else {
                        fragmentVm.totalPatientCount.postValue("0")
                        patientsListAdapter.submitData(lifecycle, PagingData.empty())
                    }
                }
            } else {
                binding.llExactSearch.btnSearch.isEnabled = (s?.trim()?.count() ?: 0) > 0
            }
        }
    }

    private fun setPatientListAdapter() {
        patientsListAdapter = PatientsListAdapter(this, fragmentVm.origin, fragmentVm.followUpType)
        Timber.tag("bug_n_bug").d("Followup Type : ${fragmentVm.followUpType}")
        binding.rvPatientsList.apply {
            layoutManager = GridLayoutManager(requireContext(), activityVm.spanCount)
            adapter = patientsListAdapter
        }
        patientsListAdapter.addLoadStateListener {
            val isLoading = it.refresh is LoadState.Loading
            if (isLoading) {
                showLoading()
            } else {
                hideLoading()
            }
            binding.pbLoadMore.visibility =
                if (it.append is LoadState.Loading) View.VISIBLE else View.GONE
        }
    }

    private fun setObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            fragmentVm.patientsDataSource.collectLatest { pagedData ->
                patientsListAdapter.submitData(pagedData)
            }
        }

        fragmentVm.userUnionListResponse.observe(viewLifecycleOwner) {
            fragmentVm.refreshPatients()
        }
        fragmentVm.patientVisitResponse.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                    resourceState?.message?.let { message ->
                        (activity as BaseActivity).showErrorDialogue(
                            getString(R.string.error),
                            message,
                            isNegativeButtonNeed = false,
                        ) {}
                    }
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    resourceState.data?.let { patientDetails ->
                        startNewReviewActivity(patientDetails)
                    }
                }
            }
        }
        fragmentVm.totalPatientCount.observe(viewLifecycleOwner) {
            updatePatientsCount(it)
        }
        fragmentVm.registerCallResponse.observe(viewLifecycleOwner) { resourceState ->
            if (resourceState == null) return@observe
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }
                ResourceState.SUCCESS -> {
                    hideLoading()
                    fragmentVm.isCallRegistered.postValue(true)
                    resourceState.data?.phoneNumber?.let { phoneNumber ->
                        fragmentVm.triggerDial(phoneNumber)
                    }
                }
                ResourceState.ERROR -> {
                    hideLoading()
                    resourceState.message?.let { message ->
                        (activity as BaseActivity).showErrorDialogue(
                            getString(R.string.error),
                            message,
                            isNegativeButtonNeed = false,
                        ) {}
                    }
                }
            }
            fragmentVm.registerCallResponse.value = null
        }
        fragmentVm.dialEvent.observe(viewLifecycleOwner) { event ->
            if (!dialerOpened) {
                event.getContentIfNotHandled()?.let { phoneNumber ->
                    val intent = Intent(Intent.ACTION_DIAL)
                    intent.data = "tel:$phoneNumber".toUri()
                    startActivity(intent)
                    dialerOpened = true
                }
            }
        }
        fragmentVm.getPatientRegisterResponse.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    if (resourceState.data?.id != null) {
                        hideLoading()
                        val existingDialog = childFragmentManager.findFragmentByTag(CallResultsDialogFragment.TAG)
                        if (existingDialog == null) {
                            val callResultsDialogFragment = CallResultsDialogFragment.newInstance(fragmentVm.followUpType)
                            val bundle = Bundle()
                            bundle.putLong(CommonDefinedParams.id, resourceState.data.id)
                            bundle.putString(DefinedParams.NAME, "${resourceState.data.firstName} ${resourceState.data.lastName}")
                            bundle.putLong(CommonDefinedParams.AGE, resourceState.data.age)
                            bundle.putString(CommonDefinedParams.GENDER, resourceState.data.gender)
                            bundle.putString(CommonDefinedParams.PHONE_NUMBER, resourceState.data.phoneNumber)
                            bundle.putString(CommonDefinedParams.CALL_TYPE, resourceState.data.callType)
                            bundle.putString(CommonDefinedParams.PATIENT_HISTORY, getPatientHistoryDetail())
                            fragmentVm.callStartTime?.let { startTime ->
                                bundle.putLong(CommonDefinedParams.CALL_START_TIME, startTime)
                            }
                            callResultsDialogFragment.arguments = bundle
                            callResultsDialogFragment.show(childFragmentManager, CallResultsDialogFragment.TAG)
                        }
                    } else {
                        getPatients()
                    }
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }
            }
        }

        fragmentVm.tcPatientDetailLiveData.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }
                ResourceState.SUCCESS -> {
                    if (shouldShowPatientHistory) {
                        hideLoading()
                        resourceState.data?.let {
                            val dialog = PatientDetailHistoryDialogFragment.newInstance(it)
                            dialog.show(childFragmentManager, PatientDetailHistoryDialogFragment::class.simpleName)
                        }
                    }
                }
                ResourceState.ERROR -> {
                    hideLoading()
                    Toast
                        .makeText(
                            requireContext(),
                            getString(R.string.something_went_wrong),
                            Toast.LENGTH_LONG,
                        ).show()
                }
            }
        }
    }

    private fun getPatientHistoryDetail(): String? {
        fragmentVm.tcPatientDetailLiveData.value?.data?.let {
            val listJson = Gson().toJson(it)
            return listJson
        }

        return null
    }

    private fun updatePatientsCount(count: String) {
        binding.tvPatientCount.apply {
            val totPatients = count.toInt()
            if (totPatients == 0) {
                visibility = View.GONE
                checkListSize(false)
            } else {
                visibility = View.VISIBLE
                text =
                    if (totPatients > 1) {
                        getString(R.string.patients_found, totPatients)
                    } else {
                        getString(
                            R.string.one_patient_found,
                        )
                    }
                checkListSize(true)
            }
        }

        // Sort Count
        if (fragmentVm.sortCount() > 0) {
            binding.llSortFilter.btnSort.text = getString(R.string.sort_with_count, " (1)")
        } else {
            binding.llSortFilter.btnSort.text = getString(R.string.sort_with_count, "")
        }

        // Filter Count
        val filterCount = fragmentVm.filterCount()
        if (filterCount > 0) {
            binding.llSortFilter.btnFilter.text =
                getString(R.string.filter_with_count, " ($filterCount)")
        } else {
            binding.llSortFilter.btnFilter.text = getString(R.string.filter_with_count, "")
        }
    }

    override fun onSelectedPatient(item: PatientListResModel) {
        when (fragmentVm.origin) {
            UIConstants.ENROLLMENT_UNIQUE_ID -> {
                proceedRegistration(item)
            }

            UIConstants.FOLLOW_UP -> {
                shouldShowPatientHistory = true
                fragmentVm.getTCPatientDetails(requireContext(), item.id!!)
            }

            else -> {
                if (CommonUtils.isNurse() && !item.isConfirmDiagnosis) {
                    proceedRegistration(item)
                } else {
                    selectedPatientAction(item)
                }
            }
        }
    }

    private fun proceedRegistration(item: PatientListResModel) {
        if (item.patientStatus.equals(DefinedParams.ENROLLED, true)) {
            val programId = if (item.programId == null) "-" else item.programId.toString()
            val errorMsg: String =
                if (programId.isNotEmpty()) {
                    getString(R.string.patient_already_enrolled_with_program_id) + item.programId
                } else {
                    getString(R.string.patient_already_enrolled)
                }
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle(null)
            builder.setMessage(errorMsg)
            builder.setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                dialog.dismiss()
            }
            builder.show()
        } else {
            confirmDiagnosis(item)
        }
    }

    private fun registerAndConfirmDiagnosis() {
        val intent = Intent(requireContext(), TermsAndConditionActivity::class.java)
        intent.putExtra(IntentConstants.INTENT_ENROLLMENT, true)
        intent.putExtra(IntentConstants.IS_FROM_DIRECT_ENROLLMENT, true)
        startActivity(intent)
    }

    private fun confirmDiagnosis(item: PatientListResModel) {
        startActivity(
            Intent(
                requireContext(),
                EnrollmentFormBuilderActivity::class.java,
            ).apply {
                putExtra(
                    DefinedParams.PATIENT_ID,
                    item.id ?: -1L,
                )
                putExtra(
                    IntentConstants.INTENT_MEMBER_REFERENCE,
                    item.memberReference,
                )
                putExtra(
                    IntentConstants.IS_FROM_DIRECT_ENROLLMENT,
                    true,
                )
                putExtra(
                    DefinedParams.SCREENING_ID,
                    item.id ?: -1L,
                )
                putExtra(
                    DefinedParams.IS_CONFIRM_DIAGNOSIS,
                    true,
                )
            },
        )
    }

    override fun onRegisterPatientCall(item: PatientListResModel) {
        if (hasTelephonyFeature()) {
            item.id?.let {
                shouldShowPatientHistory = false
                fragmentVm.getTCPatientDetails(
                    requireContext(),
                    it,
                    CommonUtils.getCallType(fragmentVm.followUpType),
                )
            }
        } else {
            (activity as BaseActivity).showErrorDialogue(
                message = getString(R.string.device_phone_info),
                callback = {
                    (activity as BaseActivity).finish()
                },
            )
        }
    }

    private fun hasTelephonyFeature(): Boolean {
        val packageManager = (activity as BaseActivity).packageManager
        return packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
    }

    private fun selectedPatientAction(item: PatientListResModel) {
        item.id?.let { patientRef ->
            item.memberReference?.let { memberRef ->
                item.patientId?.let { patientId ->
                    fragmentVm.createPatientVisit(requireContext(), patientRef, memberRef, patientId)
                }
            }
        }

//        if (CommonUtils.isHRIO() && fragmentVm.origin == UIConstants.MY_PATIENTS_UNIQUE_ID) {
//            val intent = Intent(requireContext(), PatientEditActivity::class.java)
//            intent.putExtra(DefinedParams.Patient_Id, item.patientId)
//            intent.putExtra(DefinedParams.PATIENT_TRACK_ID, item.id)
//            refreshPatientList.launch(intent)
//        } else if (fragmentVm.origin == UIConstants.MY_PATIENTS_UNIQUE_ID && CommonUtils.isCHCP()) {
//            val intent = Intent(requireContext(), ContinuousMedicalReviewBaseActivity::class.java)
//            intent.putExtra(IntentConstants.INTENT_PATIENT_ID, item.id)
//            // send a patient Tenant Id
//            intent.putExtra(DefinedParams.Patient_Tenant_Id, item.tenantId)
//            intent.putExtra(DefinedParams.ORIGIN, fragmentVm.origin)
//            refreshPatientList.launch(intent)
//        }
//        else if (fragmentVm.origin == UIConstants.MY_PATIENTS_UNIQUE_ID && CommonUtils.isPsychologist()) {
//            val intent = Intent(requireContext(), SessionEncounterBaseActivity::class.java)
//            intent.putExtra(IntentConstants.INTENT_PATIENT_ID, item.id)
//            intent.putExtra(DefinedParams.ORIGIN, fragmentVm.origin)
//            intent.putExtra(DefinedParams.Age, item.age)
//            patientListLauncher.launch(intent)
//        } else {
//            createPatientVisit(item.initialReview, item.id, item.age)
//        }
    }

    private fun startNewReviewActivity(details: PatientDetails) {
        when (fragmentVm.origin) {
            UIConstants.MY_PATIENTS_UNIQUE_ID -> {
                val intent = Intent(
                    requireContext(),
                    NurseMedicalReviewActivity::class.java,
                )
                intent.putExtra(IntentConstants.INTENT_PATIENT_ID, details.patientID)
                intent.putExtra(IntentConstants.INTENT_PATIENT_ID_STRING, details.patientIdString)
                intent.putExtra(IntentConstants.INTENT_VISIT_ID, details.visitID)
                intent.putExtra(IntentConstants.INTENT_ENCOUNTER_REFERENCE, details.encounterReference)
                intent.putExtra(IntentConstants.INTENT_MEMBER_REFERENCE, details.memberReference)
                intent.putExtra(IntentConstants.INTENT_PATIENT_REFERENCE, details.patientReference)
                intent.putExtra(IntentConstants.INTENT_INITIAL_REVIEW, details.initialReview)
                intent.putExtra(IntentConstants.SHOW_CONTINUOUS_MEDICAL_REVIEW, false)
                intent.putExtra(DefinedParams.ORIGIN, fragmentVm.origin)
                refreshPatientList.launch(intent)
            }

            UIConstants.PRESCRIPTION_UNIQUE_ID -> {
                val intent = Intent(requireContext(), PrescriptionRefillActivity::class.java)
                intent.putExtra(IntentConstants.INTENT_PATIENT_ID, details.patientID)
                intent.putExtra(IntentConstants.INTENT_VISIT_ID, details.visitID)
                startActivity(intent)
            }

            UIConstants.DISPENSE -> {
                val intent = Intent(requireContext(), NCDPharmacistActivity::class.java)
                intent.putExtra(NCDMRUtil.EncounterReference, details.encounterReference)
                intent.putExtra(CommonDefinedParams.FhirId, details.patientID.toString())
                intent.putExtra(CommonDefinedParams.PatientId, details.patientIdString)
                intent.putExtra(CommonDefinedParams.ORIGIN, fragmentVm.origin)
                startActivity(intent)
            }

//            UIConstants.INVESTIGATION -> {
//                val intent = Intent(requireContext(), LabTestListActivity::class.java)
//                intent.putExtra(IntentConstants.INTENT_PATIENT_ID, details.patientID)
//                intent.putExtra(IntentConstants.INTENT_VISIT_ID, details.visitID)
//                refreshPatientList.launch(intent)
//            }
//
//            UIConstants.LIFESTYLE -> {
//                val intent = Intent(requireContext(), NutritionistActivity::class.java)
//                intent.putExtra(IntentConstants.INTENT_PATIENT_ID, details.patientID)
//                intent.putExtra(IntentConstants.INTENT_VISIT_ID, details.visitID)
//                startActivity(intent)
//            }
//
//            UIConstants.PSYCHOLOGICAL -> {
//                val intent = Intent(requireContext(), CounselorActivity::class.java)
//                intent.putExtra(IntentConstants.INTENT_PATIENT_ID, details.patientID)
//                intent.putExtra(IntentConstants.INTENT_VISIT_ID, details.visitID)
//                startActivity(intent)
//            }
//
//            UIConstants.MY_PATIENTS_UNIQUE_ID -> {
//                when {
//                    CommonUtils.isNurse() -> {
//
//                    }
//
//                    CommonUtils.isParaCounselor() -> {
//                        val intent =
//                            Intent(requireContext(), SessionEncounterBaseActivity::class.java)
//                        intent.putExtra(IntentConstants.INTENT_PATIENT_ID, details.patientID)
//                        intent.putExtra(IntentConstants.INTENT_VISIT_ID, details.visitID)
//                        intent.putExtra(DefinedParams.ORIGIN, fragmentVm.origin)
//                        intent.putExtra(DefinedParams.Age, details.age)
//                        patientListLauncher.launch(intent)
//                    }
//
//                    (CommonUtils.isAfricaAndNutritionist() || details.initialReview) -> {
//                        val intent = Intent(
//                            requireContext(), ContinuousMedicalReviewBaseActivity::class.java
//                        )
//                        intent.putExtra(IntentConstants.INTENT_PATIENT_ID, details.patientID)
//                        intent.putExtra(IntentConstants.INTENT_VISIT_ID, details.visitID)
//                        intent.putExtra(DefinedParams.ORIGIN, fragmentVm.origin)
//                        refreshPatientList.launch(intent)
//                    }
//
//                    else -> {
//                        val intent = Intent(requireContext(), MedicalReviewBaseActivity::class.java)
//                        intent.putExtra(IntentConstants.INTENT_PATIENT_ID, details.patientID)
//                        intent.putExtra(IntentConstants.INTENT_VISIT_ID, details.visitID)
//                        intent.putExtra(IntentConstants.SHOW_CONTINUOUS_MEDICAL_REVIEW, false)
//                        intent.putExtra(DefinedParams.ORIGIN, fragmentVm.origin)
//                        refreshPatientList.launch(intent)
//                    }
//                }
//            }
//
//            UIConstants.MEDICAL_REVIEW_UNIQUE_ID -> {
//                val intent = Intent(requireContext(), MedicalReviewBaseActivity::class.java)
//                intent.putExtra(IntentConstants.INTENT_PATIENT_ID, details.patientID)
//                intent.putExtra(IntentConstants.INTENT_VISIT_ID, details.visitID)
//                intent.putExtra(DefinedParams.ORIGIN, fragmentVm.origin)
//                intent.putExtra(IntentConstants.SHOW_CONTINUOUS_MEDICAL_REVIEW, details.initialReview)
//                refreshPatientList.launch(intent)
//            }
        }
    }

    private fun getPatients() {
        fragmentVm.let {
            if (it.origin == UIConstants.FOLLOW_UP) {
                it.fetchUnionOrAccountIdList()
            } else {
                fetchPatientList()
            }
        }
    }

    private fun fetchPatientList() {
        fragmentVm.refreshPatients()
    }

    private fun checkListSize(hasPatients: Boolean) {
        binding.btnEnrol.visibility = handleRegistrationBtnVisibility(false)
        binding.btnScreening.visibility = View.GONE

        binding.tvNoPatientsFound.visibility = if (hasPatients) View.GONE else View.VISIBLE
        binding.rvPatientsList.visibility = if (hasPatients) View.VISIBLE else View.GONE
        if (showSortFilter(hasPatients)) {
            binding.llSortFilter.root.visibility = View.VISIBLE
            binding.llSortFilter.btnSort.visibility = needSort()
            binding.llSortFilter.btnFilter.visibility = View.VISIBLE
        } else if (showSortPharmacistLabTestFilter(hasPatients) ||
            fragmentVm.origin == UIConstants.FOLLOW_UP ||
            CommonUtils.isOnlyParaCounselor() ||
            CommonUtils.isPsychologist()
        ) {
            binding.llSortFilter.root.visibility = View.VISIBLE
            binding.llSortFilter.btnSort.visibility =
                if (fragmentVm.origin == UIConstants.FOLLOW_UP) View.VISIBLE else View.GONE
            binding.llSortFilter.btnFilter.visibility = View.VISIBLE
        } else {
            binding.llSortFilter.root.visibility = View.GONE
        }

        validateCheckList(hasPatients)
    }

    private fun needSort(): Int = if (CommonUtils.isNurse()) View.GONE else View.VISIBLE

    private fun validateCheckList(hasPatients: Boolean) {
        if (hasPatients) {
            // Empty Block
        } else {
            if (fragmentVm.origin == UIConstants.ENROLLMENT_UNIQUE_ID) {
                binding.tvNoPatientsFound.text = getString(R.string.no_patient_found)
                binding.btnEnrol.visibility = handleRegistrationBtnVisibility(true)
                binding.btnScreening.visibility = View.GONE
            } else if (fragmentVm.origin == UIConstants.PSYCHOLOGICAL) {
                binding.tvNoPatientsFound.text = getString(R.string.no_patient_found)
            } else if (fragmentVm.origin == UIConstants.FOLLOW_UP) {
                binding.tvNoPatientsFound.text = getString(R.string.no_patient_found)
            } else if (fragmentVm.origin == UIConstants.MY_PATIENTS_UNIQUE_ID) {
                binding.tvNoPatientsFound.text = getString(R.string.no_patient_found)
                binding.btnEnrol.setText(R.string.register)
                binding.btnEnrol.visibility = handleRegistrationBtnVisibility(true)
            } else {
                binding.tvNoPatientsFound.text = getString(R.string.screening_after_search)
                if (fragmentVm.origin == UIConstants.ASSESSMENT_UNIQUE_ID) {
                    binding.btnScreening.visibility = View.VISIBLE
                    binding.btnEnrol.visibility = handleRegistrationBtnVisibility(false)
                }
            }
        }
    }

    private fun handleRegistrationBtnVisibility(visible: Boolean): Int = if (visible && SecuredPreference.canDoRegistration()) View.VISIBLE else View.GONE

    private fun filterModel(): FilterModel =
        fragmentVm.filter?.copy(origin = fragmentVm.origin, selectedTab = fragmentVm.followUpType) ?: FilterModel(
            origin = fragmentVm.origin,
            selectedTab = fragmentVm.followUpType,
        )

    private fun sortModel(): SortModel =
        fragmentVm.sort?.copy(origin = fragmentVm.origin, followUpType = fragmentVm.followUpType)
            ?: SortModel(origin = fragmentVm.origin, followUpType = fragmentVm.followUpType)

    private val clickListener = View.OnClickListener { v ->
        when (v) {
            binding.llExactSearch.btnSearch -> {
                hideKeyboard(v)
                performExactSearch()
                networkAvailability()
            }

            binding.llExactSearch.ivQRSearch -> {
                try {
                    if (ContextCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.CAMERA,
                        ) == PackageManager.PERMISSION_DENIED
                    ) {
                        ActivityCompat.requestPermissions(
                            activity as BaseActivity,
                            arrayOf(Manifest.permission.CAMERA),
                            QRScanResult.REQUEST_CODE,
                        )
                    } else {
                        startScanning()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            binding.btnEnrol -> {
                registerAndConfirmDiagnosis()
            }

            binding.llSortFilter.btnFilter -> {
                activityVm.filter = filterModel()
                FilterDialogFragment
                    .newInstance()
                    .show(childFragmentManager, FilterDialogFragment.TAG)
            }

            binding.llSortFilter.btnSort -> {
                activityVm.sort = sortModel()
                SortingDialogFragment
                    .newInstance()
                    .show(childFragmentManager, SortingDialogFragment.TAG)
            }
        }
    }

    private fun performExactSearch() {
        fragmentVm.searchPatientId = binding.llExactSearch.etPatientSearch.text
            .toString()
            .trim()
    }

    private fun networkAvailability() {
        if (connectivityManager.isNetworkAvailable()) {
            binding.rvPatientsList.addOnLayoutChangeListener(patientListLayoutListener)
            clearQrSearch()
            getPatients()
        } else {
            (activity as BaseActivity).showErrorDialogue(
                getString(R.string.error),
                getString(R.string.no_internet_error),
                isNegativeButtonNeed = false,
            ) {}
        }
    }

    private fun clearQrSearch() {
        fragmentVm.apply {
            if (isQRSearch) {
                isQRSearch = false
                searchQRValue = null
            }
        }
    }

    private fun displayPatients(): Boolean = fragmentVm.isPatientListRequired() || fragmentVm.isQRSearch

    private fun isFollowUp(): Boolean = fragmentVm.origin == UIConstants.FOLLOW_UP

    private fun callPatientsAPI() {
        dialerOpened = false // Reset only when a new API call is made
        binding.tvPatientCount.visibility = View.GONE
        patientsListAdapter.submitData(lifecycle, PagingData.empty())
        if (displayPatients()) {
            if (isFollowUp()) {
                // TODO : Actual call is getPatientCallRegister(). But, triggering getPatients() for testing
//                getPatientCallRegister()
                getPatients()
            } else {
                getPatients()
            }
        }
    }

    private fun getPatientCallRegister() {
        fragmentVm.getPatientCallRegister(requireContext())
    }

    private fun showSortFilter(hasPatients: Boolean): Boolean {
        val role = SecuredPreference
            .getUserDetails()
            ?.roles
            ?.first()
            ?.name
        val havePermission =
            fragmentVm.origin == UIConstants.MY_PATIENTS_UNIQUE_ID &&
                (role == RoleConstant.PROVIDER || role == RoleConstant.PHYSICIAN_PRESCRIBER || role == RoleConstant.NURSE)
        return if (havePermission) {
            val hasSortFilter = fragmentVm.sortCount() > 0 || fragmentVm.filterCount() > 0
            val isSearch =
                if (fragmentVm.isOfflineEnabled || fragmentVm.isBangladesh) {
                    binding.llExactSearch.btnSearch.isEnabled
                } else {
                    binding.llExactSearch.btnSearch.isEnabled
                }
            val hideSortFilterLayout = isSearch && !hasSortFilter && !hasPatients
            !hideSortFilterLayout
        } else {
            false
        }
    }

    private fun showSortPharmacistLabTestFilter(hasPatients: Boolean): Boolean {
        val role = SecuredPreference
            .getUserDetails()
            ?.roles
            ?.first()
            ?.name
        val havePermission =
            (fragmentVm.origin == UIConstants.PRESCRIPTION_UNIQUE_ID || fragmentVm.origin == UIConstants.INVESTIGATION) &&
                (role == RoleConstant.PHARMACIST || role == RoleConstant.LAB_TECHNICIAN)
        return if (havePermission) {
            val hasSortFilter = fragmentVm.sortCount() > 0 || fragmentVm.filterCount() > 0
            val isSearch =
                if (fragmentVm.isOfflineEnabled ||
                    fragmentVm.isBangladesh
                ) {
                    binding.llExactSearch.btnSearch.isEnabled
                } else {
                    binding.llExactSearch.btnSearch.isEnabled
                }
            val hideSortFilterLayout = isSearch && !hasSortFilter && !hasPatients
            !hideSortFilterLayout
        } else {
            false
        }
    }

    private val qrScanLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(QRScanContract()) { result ->
            if (result.resultString != null) {
                if (result.resultString.isNotBlank()) {
                    binding.llExactSearch.etPatientSearch.text
                        ?.clear()
                    fragmentVm.apply {
                        isQRSearch = true
                        searchQRValue = result.resultString

                        searchPatientId = ""

                        filter = null
                        sort = null

                        callPatientsAPI()
                    }
                } else {
                    (activity as BaseActivity).showErrorDialogue(
                        getString(R.string.invalid_qr),
                        getString(R.string.scan_valid_qr),
                        isNegativeButtonNeed = false,
                    ) {}
                }
            }
        }

    private fun startScanning() {
        qrScanLauncher.launch(
            Intent(requireContext(), QRScannerActivity::class.java).apply {
                putExtra(QRScanResult.REQUEST_FROM, fragmentVm.origin)
            },
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == QRScanResult.REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanning()
            } else {
                // Camera permission Denied
            }
        }
    }

    private val patientListLayoutListener = object : View.OnLayoutChangeListener {
        override fun onLayoutChange(
            v: View?,
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
            oldLeft: Int,
            oldTop: Int,
            oldRight: Int,
            oldBottom: Int,
        ) {
            if (bottom != oldBottom) {
                (binding.rvPatientsList.layoutManager as? GridLayoutManager)?.scrollToPositionWithOffset(
                    0,
                    0,
                )
                binding.rvPatientsList.removeOnLayoutChangeListener(this)
            }
        }
    }

    override fun callRegisterStatus(isSuccess: Boolean) {
        if (isSuccess) callPatientsAPI()
    }

    private val refreshPatientList =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                callPatientsAPI()
            }
        }

    private var patientListLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                getPatients()
            }
        }

    override fun filter(filterModel: FilterModel) {
        fragmentVm.filter = filterModel
        activityVm.filter = filterModel
        binding.rvPatientsList.addOnLayoutChangeListener(patientListLayoutListener)
        clearQrSearch()
        callPatientsAPI()
    }

    override fun sort(sortModel: SortModel?) {
        fragmentVm.sort = sortModel
        binding.rvPatientsList.addOnLayoutChangeListener(patientListLayoutListener)
        clearQrSearch()
        callPatientsAPI()
    }
}
