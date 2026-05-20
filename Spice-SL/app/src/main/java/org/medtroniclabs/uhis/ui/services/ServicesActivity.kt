package org.medtroniclabs.uhis.ui.services

import android.content.Intent
import android.os.Bundle
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import androidx.activity.viewModels
import androidx.core.widget.doOnTextChanged
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.hideKeyboard
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.model.ChipViewItemModel
import org.medtroniclabs.uhis.data.offlinesync.model.HouseholdMemberWithTb
import org.medtroniclabs.uhis.databinding.ActivityServicesBinding
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.utility.CustomSpinnerAdapter
import org.medtroniclabs.uhis.model.services.ServiceMemberCounts
import org.medtroniclabs.uhis.model.services.ServiceStaticFilter
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.dashboard.ncd.DashboardConstants
import org.medtroniclabs.uhis.ui.externalmember.ExternalMemberRegistrationActivity
import org.medtroniclabs.uhis.ui.household.MemberSelectionListener
import org.medtroniclabs.uhis.ui.household.summary.MemberSummaryActivity
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
        isExternalMember = intent.getBooleanExtra("isExternalMember", false)
        preSelectedSsIds = intent.getLongArrayExtra(DashboardConstants.EXTRA_DASHBOARD_SS_IDS) ?: longArrayOf()
        preSelectedSubVillageIds = intent.getLongArrayExtra(DashboardConstants.EXTRA_DASHBOARD_SUB_VILLAGE_IDS) ?: longArrayOf()
        preSelectedStaticFilter = intent.getStringExtra(DashboardConstants.EXTRA_DASHBOARD_STATIC_FILTER)?.let {
            runCatching { ServiceStaticFilter.valueOf(it) }.getOrNull()
        }
        if (CommonUtils.isFoOrPo() && !isExternalMember) {
            val allowedFoPoFilters =
                setOf(
                    ServiceStaticFilter.ALL_MEMBERS,
                    ServiceStaticFilter.NCD_SERVICES,
                    ServiceStaticFilter.CATARACT_SCREENING,
                    ServiceStaticFilter.EYE_SCREENING,
                )
            if (preSelectedStaticFilter != null && preSelectedStaticFilter !in allowedFoPoFilters) {
                preSelectedStaticFilter = null
            }
        }

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
            servicesViewModel.setFilterLiveData(staticFilter = ServiceStaticFilter.EXTERNAL_MEMBERS)
        } else if (CommonUtils.isFoOrPo()) {
            binding.bottomNavigationView.visible()
            binding.btnAddExternalMember.text = getString(R.string.add_new_member_small)
            binding.btnAddExternalMember.safeClickListener(this)
        } else {
            binding.bottomNavigationView.gone()
        }
        applyPrefiltersFromDashboard()
        binding.llExactSearch.btnSearch.gone()
    }

    private fun applyPrefiltersFromDashboard() {
        if (preSelectedSsIds.isEmpty() && preSelectedSubVillageIds.isEmpty() && preSelectedStaticFilter == null) return
        val ssFilters = preSelectedSsIds.map {
            ChipViewItemModel(id = it, name = "")
        }
        val subVillageFilters = preSelectedSubVillageIds.map {
            ChipViewItemModel(id = it, name = "")
        }
        servicesViewModel.setFilterLiveData(
            ssFilter = ssFilters,
            subVillagesFilter = subVillageFilters,
            staticFilter = preSelectedStaticFilter,
        )
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
    private fun setDropDownData(counts: ServiceMemberCounts) {
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
    private fun buildDropDownList(counts: ServiceMemberCounts): ArrayList<Map<String, Any>> {
        val dropdownList = arrayListOf<Map<String, Any>>()
        val isFoOrPoUser = CommonUtils.isFoOrPo()
        val staticFilters =
            if (isFoOrPoUser) {
                linkedMapOf(
                    ServiceStaticFilter.ALL_MEMBERS to counts.allMembers,
                    ServiceStaticFilter.NCD_SERVICES to counts.ncdServices,
                    ServiceStaticFilter.CATARACT_SCREENING to counts.cataractScreening,
                    ServiceStaticFilter.EYE_SCREENING to counts.eyeScreening,
                )
            } else {
                linkedMapOf(
                    ServiceStaticFilter.ALL_MEMBERS to counts.allMembers,
                    ServiceStaticFilter.FAMILY_PLANNING_COUNSELLING_ELIGIBLE to counts.familyPlanning,
                    ServiceStaticFilter.PREGNANT_WOMEN to counts.pregnantWomen,
                    ServiceStaticFilter.HIGH_RISK_PREGNANT_WOMEN to counts.highRiskPregnant,
                    ServiceStaticFilter.POSTNATAL_CARE_MOTHERS to counts.postnatalMothers,
                    ServiceStaticFilter.CHILDREN_UNDER_TWO_YEARS to counts.childrenUnderTwo,
                    ServiceStaticFilter.EXPECTED_DELIVERIES to counts.expectedDeliveries,
                    ServiceStaticFilter.PENDING_DELIVERIES to counts.pendingDeliveries,
                    ServiceStaticFilter.EXTERNAL_MEMBERS to counts.externalMembers,
                    ServiceStaticFilter.EXTERNAL_PREGNANT_WOMEN to counts.externalPregnant,
                )
            }
        staticFilters.forEach { filterEntry ->
            val filter = filterEntry.key
            val filterCount = filterEntry.value
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
                withLocationCheck({
                    FilterBottomSheetDialogFragment
                        .newInstance()
                        .show(supportFragmentManager, FilterBottomSheetDialogFragment.TAG)
                })
            }

            R.id.btnSearch -> {
                withLocationCheck({
                    servicesViewModel.setUserJourney(AnalyticsDefinedParams.SERVICES_SEARCH_TRIGGERED)
                    val searchTerm = binding.llExactSearch.etSearchTerm.text
                        .toString()
                    servicesViewModel.setFilterLiveData(search = searchTerm)
                })
            }

            R.id.btnAddExternalMember -> {
                withLocationCheck({
                    servicesViewModel.setUserJourney(
                        if (isExternalMember) {
                            "ADD_EXTERNAL_MEMBER_BUTTON_TRIGGERED"
                        } else {
                            "ADD_MEMBER_BUTTON_FO_PO"
                        },
                    )
                    val intent = Intent(this, ExternalMemberRegistrationActivity::class.java)
                    startActivity(intent)
                })
            }
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
    }
}
