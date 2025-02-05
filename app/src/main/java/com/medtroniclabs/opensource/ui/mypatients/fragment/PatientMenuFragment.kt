package com.medtroniclabs.opensource.ui.mypatients.fragment

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
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.DefinedParams.ChildPatientId
import com.medtroniclabs.opensource.common.DefinedParams.DOB
import com.medtroniclabs.opensource.common.DefinedParams.DateOfDelivery
import com.medtroniclabs.opensource.common.DefinedParams.Gender
import com.medtroniclabs.opensource.common.DefinedParams.ID
import com.medtroniclabs.opensource.common.DefinedParams.NeonateOutcome
import com.medtroniclabs.opensource.common.DefinedParams.PatientId
import com.medtroniclabs.opensource.common.DefinedParams.female
import com.medtroniclabs.opensource.common.DefinedParams.male
import com.medtroniclabs.opensource.databinding.FragmentPatientMenuBinding
import com.medtroniclabs.opensource.db.entity.MenuEntity
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.BaseFragment
import com.medtroniclabs.opensource.ui.MenuConstants
import com.medtroniclabs.opensource.ui.assessment.rmnch.RMNCH.PREGNANCY_MAX_AGE
import com.medtroniclabs.opensource.ui.assessment.rmnch.RMNCH.PREGNANCY_MIN_AGE
import com.medtroniclabs.opensource.ui.home.MenuSelectionListener
import com.medtroniclabs.opensource.ui.home.ToolsViewModel
import com.medtroniclabs.opensource.ui.home.adapter.DashboardMenuItemsAdapter
import com.medtroniclabs.opensource.ui.medicalreview.abovefiveyears.AboveFiveYearsBaseActivity
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.fragment.SelectFlowDialog
import com.medtroniclabs.opensource.ui.medicalreview.underfiveyears.UnderFiveYearsBaseActivity
import com.medtroniclabs.opensource.ui.medicalreview.undertwomonths.activity.UnderTwoMonthsBaseActivity
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMedicalReviewActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PatientMenuFragment : BaseFragment(), MenuSelectionListener {

    lateinit var binding: FragmentPatientMenuBinding
    private val viewModel: ToolsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPatientMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
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
        if (CommonUtils.checkIsTablet(requireContext())) {
            val layoutManager = FlexboxLayoutManager(context)
            layoutManager.flexDirection = FlexDirection.ROW
            layoutManager.justifyContent = JustifyContent.CENTER
            binding.rvActivitiesList.layoutManager = layoutManager
        } else {
            val layoutManager = GridLayoutManager(context, 2)
            binding.rvActivitiesList.layoutManager = layoutManager
        }
        val gender = arguments?.getString(Gender, "")
        val dob = arguments?.getString(DOB, "")
        // Get the menu items list

        // Check and set isDisable property based on gender
        menuItemsList.forEach { menuItem ->
            if (menuItem.name == MenuConstants.MOTHER_AND_NEONATE_ID) {
                menuItem.isDisabled = when {
                    gender.equals(male, true) -> true
                    gender.equals(female, true) && !dob.isNullOrBlank() -> {
                        val ageAndWeek = DateUtils.getV2YearMonthAndWeek(dob)
                        val ageYears = ageAndWeek.years
                        val ageMonths = ageAndWeek.months
                        val ageWeeks = ageAndWeek.weeks
                        val ageDays = ageAndWeek.days

                        (ageYears !in PREGNANCY_MIN_AGE..PREGNANCY_MAX_AGE) || (ageYears == PREGNANCY_MAX_AGE && (ageMonths + ageWeeks + ageDays) != 0)
                    }

                    (gender.equals(female, true) || gender.equals(
                        male,
                        true
                    )) && dob.isNullOrBlank() -> true

                    else -> false
                }
            }
        }
        binding.rvActivitiesList.adapter =
            DashboardMenuItemsAdapter(menuItemsList.filter { !it.isDisabled }, this)
    }

    companion object {
        const val TAG = "PatientMenuFragment"
        fun newInstance() =
            PatientMenuFragment()

        fun newInstance(
            patientId: String?,
            id: String?,
            gender: String?,
            dob: String?,
            childPatientId: String?,
            dateOfDelivery:String?,
            neonateOutcome: String?
        ): PatientMenuFragment {
            val fragment = PatientMenuFragment()
            val bundle = Bundle()
            bundle.putString(PatientId, patientId)
            bundle.putString(ID, id)
            bundle.putString(Gender, gender)
            bundle.putString(DOB, dob)
            bundle.putString(ChildPatientId, childPatientId)
            bundle.putString(DateOfDelivery, dateOfDelivery)
            bundle.putString(NeonateOutcome,neonateOutcome)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onMenuSelected(menuId: String, subModule: String?) {
        startAssessmentToolsActivity(menuId)
    }

    private fun startAssessmentToolsActivity(menuId: String) {
        when (menuId) {
            MenuConstants.TB_MENU_ID -> {
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
                    val childPatientId=arguments?.getString(ChildPatientId,"")
                    val dateOfDelivery=arguments?.getString(DateOfDelivery,"")
                    val neonateOutcome=arguments?.getString(NeonateOutcome,"")
                    if (patientId?.isNotBlank() == true) {
                        SelectFlowDialog.newInstance(patientId, id,childPatientId,dateOfDelivery,neonateOutcome)
                            .show(childFragmentManager, SelectFlowDialog.TAG)
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
}