package org.medtroniclabs.uhis.ui.services

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.hideKeyboard
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.common.qrscanner.QRScanContract
import org.medtroniclabs.uhis.common.qrscanner.QRScanResult
import org.medtroniclabs.uhis.common.qrscanner.QRScannerActivity
import org.medtroniclabs.uhis.data.model.ChipViewItemModel
import org.medtroniclabs.uhis.data.offlinesync.model.HouseholdMemberWithTb
import org.medtroniclabs.uhis.databinding.ActivityServicesBinding
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.utility.CustomSpinnerAdapter
import org.medtroniclabs.uhis.model.services.ServiceStaticFilter
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.dashboard.ncd.DashboardConstants
import org.medtroniclabs.uhis.ui.externalmember.ExternalMemberRegistrationActivity
import org.medtroniclabs.uhis.ui.household.MemberSelectionListener
import org.medtroniclabs.uhis.ui.household.summary.MemberSummaryActivity
import org.medtroniclabs.uhis.ui.patient.UIConstants
import org.medtroniclabs.uhis.ui.services.viewmodel.ServicesViewModel
import org.medtroniclabs.uhis.common.DefinedParams as CommonDefinedParams

/**
 * Activity responsible to display members based on services
 */
@AndroidEntryPoint
class ServicesActivity : BaseActivity(), View.OnClickListener, MemberSelectionListener {
    private lateinit var binding: ActivityServicesBinding

    private val servicesViewModel: ServicesViewModel by viewModels()

    private lateinit var adapter: ServiceMembersAdapter

    /**
     * Flag to indicate if this is external member mode
     */
    private var isExternalMember = false
    private var preSelectedSsIds: LongArray = longArrayOf()
    private var preSelectedSubVillageIds: LongArray = longArrayOf()
    private var preSelectedStaticFilter: ServiceStaticFilter? = null

    private var isPreselectedFilterAlreadySet = false

    /**
     * Spinner adapter holding filters
     */
    private lateinit var spinnerAdapter: CustomSpinnerAdapter

    private var lastPosition = -1

    private lateinit var textChange: TextWatcher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityServicesBinding.inflate(layoutInflater)

        // Check if this is external member mode
        isExternalMember = intent.getBooleanExtra(IS_EXTERNAL_MEMBER, false)

        servicesViewModel.initializeAllowedDropdown(isExternalMember)

        preSelectedSsIds = intent.getLongArrayExtra(DashboardConstants.EXTRA_DASHBOARD_SS_IDS) ?: longArrayOf()
        preSelectedSubVillageIds = intent.getLongArrayExtra(DashboardConstants.EXTRA_DASHBOARD_SUB_VILLAGE_IDS) ?: longArrayOf()
        preSelectedStaticFilter = intent.getStringExtra(DashboardConstants.EXTRA_DASHBOARD_STATIC_FILTER)?.let {
            runCatching { ServiceStaticFilter.valueOf(it) }.getOrNull()
        }
        if (servicesViewModel.isFoPo && !isExternalMember) {
            if (preSelectedStaticFilter != null && preSelectedStaticFilter !in servicesViewModel.getStaticFilters()) {
                preSelectedStaticFilter = null
            }
        }
        servicesViewModel.initializeFilter(
            isExternalMember = isExternalMember,
            ssFilter = preSelectedSsIds.map { ChipViewItemModel(id = it, name = "") },
            subVillagesFilter = preSelectedSubVillageIds.map { ChipViewItemModel(id = it, name = "") },
            staticFilter = preSelectedStaticFilter,
        )

        val title =
            when {
                isExternalMember -> getString(R.string.external_member)
                else -> getString(R.string.service_recipient_list)
            }

        setMainContentView(
            binding.root,
            isToolbarVisible = true,
            title = title,
        )
        initViews()
        setListeners()
        attachObserver()
    }

    override fun onResume() {
        super.onResume()
        servicesViewModel.setUserJourney(AnalyticsDefinedParams.SERVICES)
    }

    private fun initViews() {
        binding.llExactSearch.clBtnQrSearch.visible()
        binding.llFilter.btnFilter.text = getString(R.string.filter)

        // Update search hint for external members
        val searchHint = if (isExternalMember) {
            getString(R.string.member_name_or_phone)
        } else {
            getString(R.string.household_name_or_no)
        }
        binding.llExactSearch.etSearchTerm.hint = searchHint

        adapter = ServiceMembersAdapter(this)

        binding.rvMembersList.apply {
            adapter = this@ServicesActivity.adapter
        }

        // Show/hide button and dropdown for external members
        if (isExternalMember) {
            binding.tvMemberTypes.gone()
            binding.viewMemberTypes.gone()
            binding.bottomNavigationView.visible()
            binding.btnAddExternalMember.text = getString(R.string.add_external_member)
            binding.btnAddExternalMember.safeClickListener(this)
        } else if (servicesViewModel.isFoPo) {
            binding.bottomNavigationView.visible()
            binding.btnAddExternalMember.text = getString(R.string.add_new_member_small)
            binding.btnAddExternalMember.safeClickListener(this)
        } else {
            binding.bottomNavigationView.gone()
        }
        binding.llExactSearch.btnSearch.gone()
    }

    /**
     * Listener for member type spinner
     */
    private val dropdownListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(
            adapterView: AdapterView<*>?,
            itemView: View?,
            position: Int,
            itemId: Long,
        ) {
            lastPosition = position
            val item = spinnerAdapter.getData(position)
            val id = item?.get(DefinedParams.ID) as? ServiceStaticFilter
            if (id != null) {
                servicesViewModel.setFilterLiveData(staticFilter = id)
            }
        }

        override fun onNothingSelected(p0: AdapterView<*>?) {
            // Do Nothing
        }
    }

    /**
     * Sets member type spinner data with count for each dropdown element
     */
    private fun setDropDownData(counts: Map<ServiceStaticFilter, Int>) {
        // Remove any existing listener, so that the filter won't get triggered
        binding.tvMemberTypes.onItemSelectedListener = null

        val dropDownList = buildDropDownList(counts)
        spinnerAdapter = CustomSpinnerAdapter(this, SecuredPreference.getIsTranslationEnabled())
        spinnerAdapter.setData(dropDownList)
        binding.tvMemberTypes.adapter = spinnerAdapter

        if (!isPreselectedFilterAlreadySet) {
            isPreselectedFilterAlreadySet = true
            // When opened from Dashboard, make the spinner reflect the same static filter explicitly.
            val initialFilter = preSelectedStaticFilter ?: ServiceStaticFilter.ALL_MEMBERS
            val initialPosition = dropDownList
                .indexOfFirst { item ->
                    (item[DefinedParams.ID] as? ServiceStaticFilter) == initialFilter
                }.takeIf { it >= 0 } ?: 0
            lastPosition = initialPosition
        }
        if (lastPosition != -1) {
            binding.tvMemberTypes.setSelection(lastPosition, false)
        }

        // Set listener after setting adapter, so that the filter works
        binding.tvMemberTypes.post {
            binding.tvMemberTypes.onItemSelectedListener = dropdownListener
        }
    }

    /**
     * Builds list for member type spinner
     */
    private fun buildDropDownList(counts: Map<ServiceStaticFilter, Int>): ArrayList<Map<String, Any>> {
        val dropdownList = arrayListOf<Map<String, Any>>()
        counts.forEach { (filter, filterCount) ->
            dropdownList.add(
                mapOf(
                    DefinedParams.CULTURE_VALUE to filter.culturalValue + " (${CommonUtils.formatCountForCurrentLocale(filterCount)})",
                    DefinedParams.NAME to filter.value + " (${CommonUtils.formatCountForCurrentLocale(filterCount)})",
                    DefinedParams.ID to filter,
                ),
            )
        }
        return dropdownList
    }

    private fun setListeners() {
        binding.llFilter.btnFilter.safeClickListener(this)
        binding.llExactSearch.btnQrSearch.safeClickListener(this)
        textChange = binding.llExactSearch.etSearchTerm.doOnTextChanged { text, _, _, _ ->
            servicesViewModel.onTextChange(text?.toString())
        }
    }

    private fun attachObserver() {
        servicesViewModel.getFilterLiveData().observe(this) {
            var count = 0
            if (it.filterSk != -1L) {
                count++
            }
            if (it.filterBySs.isNotEmpty()) {
                count++
            }
            if (it.filterBySubVillages.isNotEmpty()) {
                count++
            }

            if (count > 0) {
                binding.llFilter.btnFilter.text = this.getString(
                    R.string.filter_count,
                    CommonUtils.formatCountForCurrentLocale(count),
                )
            } else {
                binding.llFilter.btnFilter.text = getString(R.string.filter)
            }
        }
        servicesViewModel.filteredMembersLiveData.observe(this) { filteredMembersResource ->
            when (filteredMembersResource.state) {
                ResourceState.ERROR -> {
                    hideLoading()
                    // Do Nothing
                }
                ResourceState.LOADING -> {
                    showLoading()
                }
                ResourceState.SUCCESS -> {
                    hideLoading()
                    filteredMembersResource.data?.let { filteredMembersUiData ->
                        if (!isExternalMember) {
                            setDropDownData(filteredMembersUiData.counts)
                        }
                        setMembers(filteredMembersUiData.members)
                    }
                }
            }
        }
    }

    private fun setMembers(membersList: List<HouseholdMemberWithTb>) {
        val size = membersList.size
        val countStr = CommonUtils.formatCountForCurrentLocale(size)
        binding.tvMembersCount.text = resources.getQuantityString(R.plurals.plural_member, size, countStr)
        if (membersList.isNotEmpty()) {
            binding.llFilter.btnFilter.visible()
            binding.tvNoMembersFound.gone()
            binding.rvMembersList.visible()
            adapter.setMembersList(membersList)
        } else {
            binding.tvNoMembersFound.visible()
            binding.rvMembersList.gone()
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.btnFilter -> {
                hideKeyboard(view)
                withLocationCheck {
                    FilterBottomSheetDialogFragment
                        .newInstance()
                        .show(supportFragmentManager, FilterBottomSheetDialogFragment.TAG)
                }
            }

            R.id.btnSearch -> {
                withLocationCheck {
                    servicesViewModel.setUserJourney(AnalyticsDefinedParams.SERVICES_SEARCH_TRIGGERED)
                    val searchTerm = binding.llExactSearch.etSearchTerm.text
                        .toString()
                    servicesViewModel.setFilterLiveData(search = searchTerm)
                }
            }

            R.id.btnQrSearch -> {
                withLocationCheck({
                    servicesViewModel.setUserJourney(AnalyticsDefinedParams.SERVICES_QR_SEARCH_TRIGGERED)
                    launchQrScanner()
                })
            }

            R.id.btnAddExternalMember -> {
                withLocationCheck {
                    servicesViewModel.setUserJourney(
                        if (isExternalMember) {
                            "ADD_EXTERNAL_MEMBER_BUTTON_TRIGGERED"
                        } else {
                            "ADD_MEMBER_BUTTON_FO_PO"
                        },
                    )
                    val intent = Intent(this, ExternalMemberRegistrationActivity::class.java)
                    startActivity(intent)
                }
            }
        }
    }

    private fun launchQrScanner() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED
            ) {
                cameraPermission.launch(Manifest.permission.CAMERA)
            } else {
                startScanning()
            }
        } catch (e: Exception) {
            // error block
        }
    }

    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            startScanning()
        } else {
            // Camera permission denied
        }
    }

    private fun startScanning() {
        qrScanLauncher.launch(
            Intent(this, QRScannerActivity::class.java).apply {
                putExtra(QRScanResult.REQUEST_FROM, UIConstants.SCREENING_UNIQUE_ID)
            },
        )
    }

    private val qrScanLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(QRScanContract()) { result ->
            if (result.resultString != null) {
                servicesViewModel.filterMemberListByQr(result.resultString)
            }
        }

    override fun onMemberSelected(
        item: Long,
        isEdit: Boolean,
        dateOfBirth: String?,
        isContactTrace: Boolean,
        houseHoldId: Long?,
    ) {
        val intent = Intent(this, MemberSummaryActivity::class.java)
        intent.putExtra(CommonDefinedParams.HOUSEHOLD_ID, houseHoldId)
        intent.putExtra(CommonDefinedParams.MEMBER_ID, item)
        intent.putExtra(CommonDefinedParams.DOB, dateOfBirth)
        // Add entry point for navigation only if not external member,
        // for external members navigation has been handled differently
        if (!isExternalMember) {
            intent.putExtra(CommonDefinedParams.ENTRY_POINT, ENTRY_POINT_SERVICES)
        }
        startActivity(intent)
    }

    companion object {
        const val ENTRY_POINT_SERVICES = "Services"
        const val IS_EXTERNAL_MEMBER = "isExternalMember"
    }
}
