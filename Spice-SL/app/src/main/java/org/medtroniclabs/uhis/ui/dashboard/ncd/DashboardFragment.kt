package org.medtroniclabs.uhis.ui.dashboard.ncd

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ
import org.medtroniclabs.uhis.common.DateUtils.DATE_ddMMyyyy
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.common.ViewUtils
import org.medtroniclabs.uhis.data.CustomDateModel
import org.medtroniclabs.uhis.data.NCDUserDashboardRequest
import org.medtroniclabs.uhis.data.NCDUserDashboardResponse
import org.medtroniclabs.uhis.databinding.FragmentDashboardBinding
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.MenuConstants
import org.medtroniclabs.uhis.ui.dashboard.ncd.DashboardConstants.CARD_ANC
import org.medtroniclabs.uhis.ui.dashboard.ncd.DashboardConstants.CARD_ANC_3_PLUS
import org.medtroniclabs.uhis.ui.dashboard.ncd.DashboardConstants.CARD_CHILD_VISIT
import org.medtroniclabs.uhis.ui.dashboard.ncd.DashboardConstants.CARD_FAMILY_PLANNING
import org.medtroniclabs.uhis.ui.dashboard.ncd.DashboardConstants.CARD_HIGH_RISK_PREGNANT_WOMEN
import org.medtroniclabs.uhis.ui.dashboard.ncd.DashboardConstants.CARD_HOUSEHOLD_REGISTERED
import org.medtroniclabs.uhis.ui.dashboard.ncd.DashboardConstants.CARD_NCD_FOLLOW_UP_ASSESSMENT
import org.medtroniclabs.uhis.ui.dashboard.ncd.DashboardConstants.CARD_NCD_IN_CATARACT_CAMP
import org.medtroniclabs.uhis.ui.dashboard.ncd.DashboardConstants.CARD_NCD_REFERRED_FOLLOWUP
import org.medtroniclabs.uhis.ui.dashboard.ncd.DashboardConstants.CARD_NCD_SCREENING
import org.medtroniclabs.uhis.ui.dashboard.ncd.DashboardConstants.CARD_PNC
import org.medtroniclabs.uhis.ui.dashboard.ncd.DashboardConstants.CARD_PREGNANCY_OUTCOME
import org.medtroniclabs.uhis.ui.dashboard.ncd.DashboardConstants.CARD_PREGNANT_WOMEN_REGISTRATION
import org.medtroniclabs.uhis.ui.dashboard.ncd.DashboardConstants.CARD_PW_IDENTIFIED_4_MONTHS_ANC
import org.medtroniclabs.uhis.ui.dashboard.ncd.DashboardConstants.CARD_REFERRED_FOR_OPERATION
import org.medtroniclabs.uhis.ui.dashboard.ncd.DashboardConstants.CARD_TOTAL_EYE_SCREENING
import org.medtroniclabs.uhis.ui.dashboard.ncd.DashboardConstants.CARD_TOTAL_NCD_SERVICES
import org.medtroniclabs.uhis.ui.dashboard.ncd.DashboardConstants.CARD_CATARACT_SCREENING
import org.medtroniclabs.uhis.ui.dashboard.ncd.DashboardConstants.CARD_GLASSES_SOLD
import org.medtroniclabs.uhis.ui.dashboard.ncd.adapter.DashboardCardItem
import org.medtroniclabs.uhis.ui.dashboard.ncd.adapter.UserDashboardAdapter
import org.medtroniclabs.uhis.ui.dashboard.ncd.viewmodel.NCDDashBoardViewModel

@AndroidEntryPoint
class DashboardFragment : BaseFragment(), View.OnClickListener {
    private lateinit var binding: FragmentDashboardBinding
    private val viewModel: NCDDashBoardViewModel by activityViewModels()
    private var dashboardFilterCount: Int = 0

    /** Lowercased clinical workflow slugs from forms sync; gates NCD / eye / cataract tiles. */
    private var clinicalWorkflowNamesLower: Set<String> = emptySet()

    private fun workflowSlugsContain(vararg slug: String): Boolean =
        slug.any { candidate -> clinicalWorkflowNamesLower.contains(candidate.lowercase()) }

    private fun hasNcdWorkflow(): Boolean =
        workflowSlugsContain(MenuConstants.NCD_MENU_ID)

    private fun hasEyeCareWorkflow(): Boolean = workflowSlugsContain(MenuConstants.EYE_CARE_MENU_ID)

    private fun hasCataractWorkflow(): Boolean = workflowSlugsContain(MenuConstants.CATARACT_MENU_ID)

    private fun reboundDashboardAfterClinicalWorkflowsLoaded() {
        val resource = viewModel.userDashboardDetails.value
        if (resource?.isSuccess() == true) {
            resource.data?.let { showView(false, it) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        clickListeners()
        applyTodayDefaultDatesToFields()
        attachObservers()
        viewModel.getMenus()
        viewModel.loadDashboardClinicalWorkflowGate()
    }

    private fun attachObservers() {
        viewModel.clinicalWorkflowNamesLowerLiveData.observe(viewLifecycleOwner) { names ->
            clinicalWorkflowNamesLower = names ?: emptySet()
            reboundDashboardAfterClinicalWorkflowsLoaded()
        }
        viewModel.getFilterLiveData().observe(viewLifecycleOwner) { filter ->
            var count = 0
            if (filter.filterBySs.isNotEmpty()) count++
            if (filter.filterBySubVillages.isNotEmpty()) count++
            dashboardFilterCount = count
            updateFilterButtonLabel(dashboardFilterCount)
            if (hasCompleteDateRange()) {
                loadDashboardWithCustomDateRange()
            }
        }
        viewModel.userDashboardDetails.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.SUCCESS -> {
                    hideProgress()
                    resourceState.data?.let { entity ->
                        showView(false, entity)
                    }
                }

                ResourceState.ERROR -> {
                    hideProgress()
                    showErrorDialog(getString(R.string.error), resourceState.message.toString())
                }
            }
        }
    }

    private fun clickListeners() {
        binding.etFromDate.safeClickListener(this)
        binding.etToDate.safeClickListener(this)
        updateFilterButtonLabel(dashboardFilterCount)
        binding.llFilter?.btnFilter?.safeClickListener {
            DashboardFilterBottomSheetDialogFragment
                .newInstance()
                .show(childFragmentManager, DashboardFilterBottomSheetDialogFragment.TAG)
        }
    }

    private fun updateFilterButtonLabel(count: Int) {
        binding.llFilter?.btnFilter?.text =
            if (count > 0) {
                getString(R.string.filter_count, CommonUtils.formatCountForCurrentLocale(count))
            } else {
                getString(R.string.filter)
            }
    }

    private fun applyTodayDefaultDatesToFields() {
        val today = DateUtils.getTodayDateDDMMYYYY(DATE_ddMMyyyy)
        binding.etFromDate.text = today
        binding.etToDate.text = today
    }

    private fun hasCompleteDateRange(): Boolean =
        !binding.etFromDate.text.isNullOrEmpty() &&
            !binding.etToDate.text.isNullOrEmpty()

    /**
     * Returns true if the from-date is after to-date otherwise false.
     */
    private fun shouldResetToDate(newFromDate: String): Boolean {
        val currentToDate = binding.etToDate.text
            ?.toString()
            .orEmpty()
        if (currentToDate.isBlank()) return false

        val fromDateValue = DateUtils.convertStringToDate(newFromDate, DATE_ddMMyyyy)
        val toDateValue = DateUtils.convertStringToDate(currentToDate, DATE_ddMMyyyy)
        return fromDateValue?.after(toDateValue) == true
    }

    private fun resetCounts() {
        viewModel.userDashboardDetails.value
            ?.data
            ?.let { showView(true, it) }
    }

    private fun showView(
        customize: Boolean,
        entity: NCDUserDashboardResponse,
    ) {
        val userDashboardList = ArrayList<DashboardCardItem>()
        entity.let {
            userDashboardList.add(
                DashboardCardItem(
                    CARD_PREGNANT_WOMEN_REGISTRATION,
                    getString(R.string.pregnant_women_registration),
                    it.pregnantWomenRegistrationCount,
                    R.drawable.ic_rmnch_tool,
                ),
            )
            userDashboardList.add(
                DashboardCardItem(
                    CARD_ANC,
                    getString(R.string.anc_dashboard),
                    it.ancCount,
                    R.drawable.ic_rmnch_tool,
                ),
            )
            userDashboardList.add(
                DashboardCardItem(
                    CARD_PW_IDENTIFIED_4_MONTHS_ANC,
                    getString(R.string.pw_identified_first_4_months_received_anc),
                    it.pwIdentifiedFirst4MonthsWithAncCount,
                    R.drawable.ic_rmnch_tool,
                ),
            )
            userDashboardList.add(
                DashboardCardItem(
                    CARD_ANC_3_PLUS,
                    getString(R.string.anc_3_plus_services),
                    it.anc3PlusCount,
                    R.drawable.ic_rmnch_tool,
                ),
            )
            userDashboardList.add(
                DashboardCardItem(
                    CARD_PREGNANCY_OUTCOME,
                    getString(R.string.pregnancy_outcome_dashboard),
                    it.pregnancyOutcomeCount,
                    R.drawable.ic_rmnch_tool,
                ),
            )
            userDashboardList.add(
                DashboardCardItem(
                    CARD_PNC,
                    getString(R.string.pnc_dashboard),
                    it.pncCount,
                    R.drawable.ic_rmnch_tool,
                ),
            )
            userDashboardList.add(
                DashboardCardItem(
                    CARD_HIGH_RISK_PREGNANT_WOMEN,
                    getString(R.string.highrisk_pregnant_women),
                    it.highRiskPregnantWomenCount,
                    R.drawable.ic_referred,
                ),
            )
            userDashboardList.add(
                DashboardCardItem(
                    CARD_CHILD_VISIT,
                    getString(R.string.child_visit),
                    it.childVisitCount,
                    R.drawable.ic_child_under_5,
                ),
            )
            userDashboardList.add(
                DashboardCardItem(
                    CARD_HOUSEHOLD_REGISTERED,
                    getString(R.string.household_registered),
                    it.householdRegisteredCount,
                    R.drawable.ic_registration,
                ),
            )
            userDashboardList.add(
                DashboardCardItem(
                    CARD_FAMILY_PLANNING,
                    getString(R.string.family_planning_dashboard),
                    it.familyPlanningCount,
                    R.drawable.ic_family_planning,
                ),
            )
            if (hasNcdWorkflow()) {
                userDashboardList.add(
                    DashboardCardItem(
                        CARD_NCD_SCREENING,
                        getString(R.string.dashboard_ncd_screening),
                        it.ncdScreeningFirstServiceCount,
                        R.drawable.ic_ncd_tool,
                    ),
                )
                userDashboardList.add(
                    DashboardCardItem(
                        CARD_NCD_REFERRED_FOLLOWUP,
                        getString(R.string.dashboard_ncd_referred_followup),
                        it.ncdFollowUpReferralCount,
                        R.drawable.ic_referred,
                    ),
                )
                userDashboardList.add(
                    DashboardCardItem(
                        CARD_NCD_FOLLOW_UP_ASSESSMENT,
                        getString(R.string.dashboard_ncd_follow_up_assessment),
                        it.ncdFollowUpAssessmentCount,
                        R.drawable.ic_ncd_tool,
                    ),
                )
                userDashboardList.add(
                    DashboardCardItem(
                        CARD_TOTAL_NCD_SERVICES,
                        getString(R.string.dashboard_total_ncd_services),
                        it.totalNcdServicesCount,
                        R.drawable.ic_ncd_tool,
                    ),
                )
            }
            if (hasEyeCareWorkflow()) {
                userDashboardList.add(
                    DashboardCardItem(
                        CARD_TOTAL_EYE_SCREENING,
                        getString(R.string.dashboard_total_eye_screening),
                        it.eyeCareCount,
                        R.drawable.ic_eye_care,
                    ),
                )
                userDashboardList.add(
                    DashboardCardItem(
                        CARD_GLASSES_SOLD,
                        getString(R.string.dashboard_glasses_sold),
                        it.glassesSoldCustomStatusCount,
                        R.drawable.ic_eye_care,
                    ),
                )
            }
            if (hasCataractWorkflow()) {
                userDashboardList.add(
                    DashboardCardItem(
                        CARD_CATARACT_SCREENING,
                        getString(R.string.dashboard_cataract_screening),
                        it.cataractCount,
                        R.drawable.ic_eye_care,
                    ),
                )
                userDashboardList.add(
                    DashboardCardItem(
                        CARD_NCD_IN_CATARACT_CAMP,
                        getString(R.string.dashboard_ncd_in_cataract_camp),
                        it.ncdServicesInCataractCampCount,
                        R.drawable.ic_ncd_tool,
                    ),
                )
                userDashboardList.add(
                    DashboardCardItem(
                        CARD_REFERRED_FOR_OPERATION,
                        getString(R.string.dashboard_referred_for_operation),
                        it.patientsReferredForOperationCount,
                        R.drawable.ic_referred,
                    ),
                )
            }
        }
        binding.rvActivitiesList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = UserDashboardAdapter(customize, userDashboardList) {
                // Navigation from dashboard cards is intentionally disabled.
            }
        }
    }

    private fun showDatePickerDialog(
        isFromDate: Boolean,
        text: String?,
    ) {
        var date: Triple<Int?, Int?, Int?>? = null
        if (!text.isNullOrBlank()) {
            date = DateUtils.convertedMMMToddMM(text)
        }

        val fromDate = binding.etFromDate.text?.toString()

        val datePickerDialog = ViewUtils.showDatePicker(
            context = requireContext(),
            date = date,
            minDate = if (isFromDate) {
                null
            } else {
                DateUtils.convertDateToLong(
                    fromDate,
                    DATE_ddMMyyyy,
                )
            },
            maxDate = System.currentTimeMillis(),
        ) { _, year, month, dayOfMonth ->
            DateUtils
                .convertDateTimeToDate(
                    "$dayOfMonth-$month-$year",
                    DateUtils.DATE_FORMAT_ddMMyyyy,
                    DATE_ddMMyyyy,
                ).let { stringDate ->
                    if (isFromDate) {
                        resetCounts()
                        binding.etFromDate.text = stringDate
                        if (shouldResetToDate(stringDate)) {
                            binding.etToDate.text = getString(R.string.empty)
                        }
                    } else {
                        binding.etToDate.text = stringDate
                    }
                }
            if (hasCompleteDateRange()) {
                loadDashboardWithCustomDateRange()
            }
        }
        datePickerDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.okay)) { dg, _ ->
            datePickerDialog.onClick(dg, DialogInterface.BUTTON_POSITIVE)
        }

        datePickerDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel)) { dg, _ ->
            datePickerDialog.onClick(dg, DialogInterface.BUTTON_NEGATIVE)
        }
        datePickerDialog.show()
    }

    private fun loadDashboardWithCustomDateRange() {
        if (!hasCompleteDateRange()) return
        val endDate =
            DateUtils.convertStringToDate(binding.etToDate.text.toString(), DATE_ddMMyyyy)
        val request =
            NCDUserDashboardRequest(
                customDate =
                    CustomDateModel(
                        startDate =
                            DateUtils.convertDateTimeToDate(
                                binding.etFromDate.text.toString(),
                                DATE_ddMMyyyy,
                                DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                                inUTC = true,
                            ),
                        endDate =
                            DateUtils.getEndDate(
                                endDate,
                                DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                                inUTC = true,
                            ),
                    ),
                userId = SecuredPreference.getUserFhirId(),
                filterBySs =
                    viewModel
                        .getFilterLiveData()
                        .value
                        ?.filterBySs
                        ?.mapNotNull { it.id },
                filterBySubVillages =
                    viewModel
                        .getFilterLiveData()
                        .value
                        ?.filterBySubVillages
                        ?.mapNotNull { it.id },
            )
        viewModel.getUserDashboardDetails(request)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.etFromDate.id -> {
                showDatePickerDialog(true, binding.etFromDate.text.toString())
            }

            binding.etToDate.id -> {
                if (binding.etFromDate.text
                        .toString()
                        .isNotEmpty()
                ) {
                    showDatePickerDialog(false, binding.etToDate.text.toString())
                }
            }
        }
    }

    companion object {
        const val TAG = "DashboardFragment"

        fun newInstance(): DashboardFragment = DashboardFragment()
    }
}
