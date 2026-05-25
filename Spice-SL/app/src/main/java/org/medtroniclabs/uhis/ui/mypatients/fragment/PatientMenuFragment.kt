package org.medtroniclabs.uhis.ui.mypatients.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.DefinedParams.CHILD_PATIENT_ID
import org.medtroniclabs.uhis.common.DefinedParams.DOB
import org.medtroniclabs.uhis.common.DefinedParams.DateOfDelivery
import org.medtroniclabs.uhis.common.DefinedParams.GENDER
import org.medtroniclabs.uhis.common.DefinedParams.GENDER_FEMALE
import org.medtroniclabs.uhis.common.DefinedParams.GENDER_MALE
import org.medtroniclabs.uhis.common.DefinedParams.ID
import org.medtroniclabs.uhis.common.DefinedParams.MEMBER_ID
import org.medtroniclabs.uhis.common.DefinedParams.NEONATE_OUTCOME
import org.medtroniclabs.uhis.common.DefinedParams.PatientId
import org.medtroniclabs.uhis.common.DefinedParams.villageId
import org.medtroniclabs.uhis.databinding.FragmentPatientMenuBinding
import org.medtroniclabs.uhis.db.entity.MenuEntity
import org.medtroniclabs.uhis.ncd.medicalreview.NCDMedicalReviewActivity
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.MenuConstants
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH.PREGNANCY_MAX_AGE
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH.PREGNANCY_MIN_AGE
import org.medtroniclabs.uhis.ui.home.MenuSelectionListener
import org.medtroniclabs.uhis.ui.home.ToolsViewModel
import org.medtroniclabs.uhis.ui.home.adapter.DashboardMenuItemsAdapter
import org.medtroniclabs.uhis.ui.medicalreview.abovefiveyears.AboveFiveYearsBaseActivity
import org.medtroniclabs.uhis.ui.medicalreview.epi.ImmunizationActivity
import org.medtroniclabs.uhis.ui.medicalreview.familyplan.activity.FamilyPlanMedicalReviewActivity
import org.medtroniclabs.uhis.ui.medicalreview.hiv.activity.HivImrAndCmrActivity
import org.medtroniclabs.uhis.ui.medicalreview.motherneonate.anc.fragment.SelectFlowDialog
import org.medtroniclabs.uhis.ui.medicalreview.tb.activity.TBMedicalReviewActivity
import org.medtroniclabs.uhis.ui.medicalreview.underfiveyears.UnderFiveYearsBaseActivity
import org.medtroniclabs.uhis.ui.medicalreview.undertwomonths.activity.UnderTwoMonthsBaseActivity

@AndroidEntryPoint
class PatientMenuFragment : BaseFragment(), MenuSelectionListener {
    lateinit var binding: FragmentPatientMenuBinding
    private val viewModel: ToolsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentPatientMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.getMyPatientsMenuItemsList()
        attachObservers()
    }

    private fun attachObservers() {
        viewModel.menuListLiveData.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    (activity as BaseActivity).showLoading()
                }

                ResourceState.SUCCESS -> {
                    (activity as BaseActivity).hideLoading()
                    resourceState.data?.let {
                        setAdapterViews(it)
                    }
                }

                ResourceState.ERROR -> {
                    (activity as BaseActivity).hideLoading()
                }
            }
        }
    }

    private fun setAdapterViews(menuItemsList: List<MenuEntity>) {
        viewModel.isMenutypeHiv = menuItemsList.any { it.name.equals(MenuConstants.HIV) }
        if (CommonUtils.checkIsTablet(requireContext())) {
            val layoutManager = FlexboxLayoutManager(context)
            layoutManager.flexDirection = FlexDirection.ROW
            layoutManager.justifyContent = JustifyContent.CENTER
            binding.rvActivitiesList.layoutManager = layoutManager
        } else {
            val layoutManager = GridLayoutManager(context, 2)
            binding.rvActivitiesList.layoutManager = layoutManager
        }
        val gender = arguments?.getString(GENDER, "")
        val dob = arguments?.getString(DOB, "")
        // Get the menu items list

        // Check and set isDisable property based on gender
        menuItemsList.forEach { menuItem ->
            when (menuItem.name) {
                (MenuConstants.MOTHER_AND_NEONATE_ID) -> {
                    menuItem.isDisabled = when {
                        gender.equals(GENDER_MALE, true) -> true
                        gender.equals(GENDER_FEMALE, true) && !dob.isNullOrBlank() -> {
                            val ageAndWeek = DateUtils.getV2YearMonthAndWeek(dob)
                            val ageYears = ageAndWeek.years
                            val ageMonths = ageAndWeek.months
                            val ageWeeks = ageAndWeek.weeks
                            val ageDays = ageAndWeek.days

                            (ageYears !in PREGNANCY_MIN_AGE..PREGNANCY_MAX_AGE) || (ageYears == PREGNANCY_MAX_AGE && (ageMonths + ageWeeks + ageDays) != 0)
                        }

                        (
                            gender.equals(GENDER_FEMALE, true) ||
                                gender.equals(
                                    GENDER_MALE,
                                    true,
                                )
                        ) &&
                            dob.isNullOrBlank() -> true

                        else -> false
                    }
                }

                MenuConstants.GENERAL_ID -> menuItem.isDisabled = isGeneralDisabled(dob)
                MenuConstants.UNDER_AGE_FIVE_TO_TWO_MONTHS_ID ->
                    menuItem.isDisabled =
                        isUnderFiveToTwoMonthsDisabled(dob)

                MenuConstants.UNDER_AGE_ABOVE_FIVE_YEAR_ID ->
                    menuItem.isDisabled =
                        isUnderAgeAboveFiveYearsDisabled(dob)

                MenuConstants.EPI_ID -> menuItem.isDisabled = !isUnderSixtyMonths(dob)
                MenuConstants.TB_MENU_ID -> menuItem.isDisabled = false
                MenuConstants.FP_MENU_MR ->
                    menuItem.isDisabled =
                        isUnderAgeFamilyPlanningDisabled(dob)

                else -> {}
            }
        }
        binding.rvActivitiesList.adapter =
            DashboardMenuItemsAdapter(menuItemsList.filter { !it.isDisabled }, this)
    }

    companion object {
        const val TAG = "PatientMenuFragment"

        fun newInstance() = PatientMenuFragment()

        fun newInstance(
            patientId: String?,
            id: String?,
            memberId: String?,
            gender: String?,
            dob: String?,
            childPatientId: String?,
            dateOfDelivery: String?,
            neonateOutcome: String?,
            householdId: String?,
            villageId: String?,
            isPregnant: Boolean,
            isEMTCTFlow: Boolean,
            hivTestedPositive: Boolean,
        ): PatientMenuFragment {
            val fragment = PatientMenuFragment()
            val bundle = Bundle()
            bundle.putString(PatientId, patientId)
            bundle.putString(ID, id)
            bundle.putString(MEMBER_ID, memberId)
            bundle.putString(GENDER, gender)
            bundle.putString(DOB, dob)
            bundle.putString(CHILD_PATIENT_ID, childPatientId)
            bundle.putString(DateOfDelivery, dateOfDelivery)
            bundle.putString(NEONATE_OUTCOME, neonateOutcome)
            bundle.putString(DefinedParams.householdId, householdId)
            bundle.putString(DefinedParams.villageId, villageId)
            bundle.putBoolean(DefinedParams.isPregnant, isPregnant)
            bundle.putBoolean(DefinedParams.EMTCT, isEMTCTFlow)
            bundle.putBoolean(DefinedParams.HIV_TESTED_POSITIVE, hivTestedPositive)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onMenuSelected(
        menuId: String,
        subModule: String?,
    ) {
        startAssessmentToolsActivity(menuId)
    }

    private fun startAssessmentToolsActivity(menuId: String) {
        when (menuId) {
            MenuConstants.TB_MENU_ID -> {
                val intent = Intent(requireContext(), TBMedicalReviewActivity::class.java)
                intent.putExtra(PatientId, arguments?.getString(PatientId))
                intent.putExtra(ID, arguments?.getString(ID))
                startActivity(intent)
            }

            MenuConstants.GENERAL_ID -> {
                val intent = Intent(requireContext(), AboveFiveYearsBaseActivity::class.java)
                intent.putExtra(PatientId, arguments?.getString(PatientId))
                intent.putExtra(ID, arguments?.getString(ID))
                startActivity(intent)
            }

            MenuConstants.MOTHER_AND_NEONATE_ID -> {
                withNetworkAvailability(online = {
                    val patientId = arguments?.getString(PatientId, "")
                    val id = arguments?.getString(ID, "")
                    val childPatientId = arguments?.getString(CHILD_PATIENT_ID, "")
                    val dateOfDelivery = arguments?.getString(DateOfDelivery, "")
                    val neonateOutcome = arguments?.getString(NEONATE_OUTCOME, "")
                    val isEmtctFlow = arguments?.getBoolean(DefinedParams.EMTCT, false)
                    val hivTestedPositive =
                        arguments?.getBoolean(DefinedParams.HIV_TESTED_POSITIVE, false)
                    if (patientId?.isNotBlank() == true) {
                        SelectFlowDialog
                            .newInstance(
                                patientId,
                                id,
                                childPatientId,
                                dateOfDelivery,
                                neonateOutcome,
                                arguments?.getString(MEMBER_ID),
                                isEmtctFlow,
                                hivTestedPositive,
                                viewModel.isMenutypeHiv,
                            ).show(childFragmentManager, SelectFlowDialog.TAG)
                    }
                })
            }

            MenuConstants.UNDER_AGE_FIVE_TO_TWO_MONTHS_ID -> {
                val intent = Intent(requireContext(), UnderTwoMonthsBaseActivity::class.java)
                intent.putExtra(PatientId, arguments?.getString(PatientId))
                intent.putExtra(ID, arguments?.getString(ID))
                startActivity(intent)
            }

            MenuConstants.UNDER_AGE_ABOVE_FIVE_YEAR_ID -> {
                val intent = Intent(requireContext(), UnderFiveYearsBaseActivity::class.java)
                intent.putExtra(PatientId, arguments?.getString(PatientId))
                intent.putExtra(ID, arguments?.getString(ID))
                startActivity(intent)
            }

            MenuConstants.EPI_ID -> {
                val intent = Intent(requireContext(), ImmunizationActivity::class.java)
                intent.putExtra(PatientId, arguments?.getString(PatientId))
                intent.putExtra(DOB, arguments?.getString(DOB))
                intent.putExtra(ID, arguments?.getString(ID))
                intent.putExtra(MEMBER_ID, arguments?.getString(MEMBER_ID))
                intent.putExtra(
                    DefinedParams.householdId,
                    arguments?.getString(DefinedParams.householdId),
                )
                intent.putExtra(
                    DefinedParams.villageId,
                    arguments?.getString(DefinedParams.villageId),
                )
                startActivity(intent)
            }

            MenuConstants.FP_MENU_MR -> {
                val intent = Intent(requireContext(), FamilyPlanMedicalReviewActivity::class.java)
                intent.putExtra(PatientId, arguments?.getString(PatientId))
                intent.putExtra(DOB, arguments?.getString(DOB))
                intent.putExtra(ID, arguments?.getString(ID))
                intent.putExtra(MEMBER_ID, arguments?.getString(MEMBER_ID))
                startActivity(intent)
            }

            MenuConstants.HIV -> {
                if (arguments?.getBoolean(DefinedParams.HIV_TESTED_POSITIVE, false) == true) {
                    val patientId = arguments?.getString(PatientId, "")
                    val id = arguments?.getString(ID, "")
                    val intent =
                        Intent(requireContext(), HivImrAndCmrActivity::class.java)
                    if (patientId?.isNotBlank() == true) {
                        intent.putExtra(PatientId, patientId)
                        intent.putExtra(ID, id)
                    }
                    startActivity(intent)
                } else {
                    val isPregnant = arguments?.getBoolean(DefinedParams.isPregnant, false)
                    arguments?.getString(villageId)?.let { villageId ->
                        SelectFlowDialog
                            .newInstanceHiv(
                                arguments?.getString(PatientId),
                                arguments?.getString(ID),
                                true,
                                isPregnant == true,
                                arguments?.getString(MEMBER_ID),
                                villageId,
                            ).show(childFragmentManager, SelectFlowDialog.TAG)
                    }
                }
            }

            else -> {
                startAssessmentActivity()
            }
        }
    }

    private fun startAssessmentActivity() {
        if (connectivityManager.isNetworkAvailable()) {
            val intent = Intent(requireContext(), NCDMedicalReviewActivity::class.java)
            val patientId = arguments?.getString(PatientId, "")
            if (patientId?.isNotBlank() == true) {
                intent.putExtra(PatientId, patientId)
            }
            startActivity(intent)
        } else {
            showErrorDialog(getString(R.string.error), getString(R.string.no_internet_error))
        }
    }

    private fun isGeneralDisabled(dob: String?): Boolean {
        if (dob.isNullOrBlank()) return false
        val age = DateUtils.getV2YearMonthAndWeek(dob)
        return when {
            age.years > 5 -> false
            age.years == 5 && (age.months > 0 || age.weeks > 0 || age.days > 0) -> false
            else -> true
        }
    }

    private fun isGeneralDisabledForTB(dob: String?): Boolean {
        if (dob.isNullOrBlank()) return false
        val age = DateUtils.getV2YearMonthAndWeek(dob)
        return when {
            age.years >= 5 -> false // Allow exactly 5 and more
            else -> true
        }
    }

    private fun isUnderFiveToTwoMonthsDisabled(dob: String?): Boolean {
        if (dob.isNullOrBlank()) return false
        val age = DateUtils.getV2YearMonthAndWeek(dob)
        return when {
            // Age is under 2 months
            age.years == 0 && age.months < 2 -> false
            // Age is exactly 2 months but with any week or day deviation
            // age.years == 0 && age.months == 2 && age.weeks == 0 && age.days== 0 -> false
            else -> true
        }
    }

    private fun isUnderAgeAboveFiveYearsDisabled(dob: String?): Boolean {
        if (dob.isNullOrBlank()) return false
        val age = DateUtils.getV2YearMonthAndWeek(dob)
        return when {
            // Age is less than 2 months
            age.years == 0 && age.months < 2 -> true

            age.years == 0 && (age.months == 2 && age.weeks == 0 && age.days == 0) -> false
            // Age is exactly 5 years
            age.years == 5 && (age.months == 0 && age.weeks == 0 && age.days == 0) -> false
            // Age is more than 2 months but less than 5 years
            age.years < 5 -> false
            else -> true
        }
    }

    private fun isUnderAgeFamilyPlanningDisabled(dob: String?): Boolean {
        if (dob.isNullOrBlank()) return false
        val age = DateUtils.getV2YearMonthAndWeek(dob)
        return when {
            age.years == 13 && (age.months == 0 && age.weeks == 0 && age.days == 0) -> true
            age.years < 13 -> true
            else -> false
        }
    }

    private fun isUnderSixtyMonths(dob: String?): Boolean {
        val maxMonthAgeForEPI = 60
        if (dob.isNullOrBlank()) return false
        val age = DateUtils.getV2YearMonthAndWeek(dob)
        val ageInMonths = (age.years * 12) + age.months

        return ageInMonths < maxMonthAgeForEPI
    }
}
