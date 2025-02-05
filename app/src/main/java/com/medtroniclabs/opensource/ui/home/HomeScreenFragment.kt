package com.medtroniclabs.opensource.ui.home

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
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.databinding.FragmentHomeScreenBinding
import com.medtroniclabs.opensource.db.entity.MenuEntity
import com.medtroniclabs.opensource.ncd.followup.activity.NCDFollowUpActivity
import com.medtroniclabs.opensource.ncd.screening.ui.ScreeningActivity
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.BaseFragment
import com.medtroniclabs.opensource.ui.MenuConstants
import com.medtroniclabs.opensource.ui.dashboard.ncd.NCDDashboardViewActivity
import com.medtroniclabs.opensource.ui.common.PatientSearchActivity
import com.medtroniclabs.opensource.ui.followup.FollowUpMyPatientActivity
import com.medtroniclabs.opensource.ui.home.adapter.DashboardMenuItemsAdapter
import com.medtroniclabs.opensource.ui.household.HouseholdSearchActivity
import com.medtroniclabs.opensource.ui.landing.viewmodel.LandingViewModel
import com.medtroniclabs.opensource.ui.peersupervisor.PerformanceMonitoringActivity
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class HomeScreenFragment : BaseFragment(), MenuSelectionListener {

    private lateinit var binding: FragmentHomeScreenBinding

    private val viewModel: LandingViewModel by activityViewModels()

    companion object {
        const val TAG = "HomeScreenFragment"
        fun newInstance(): HomeScreenFragment {
            return HomeScreenFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        attachObservers()
        viewModel.getMenus()
        viewModel.setUserJourney(getString(R.string.home))
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

    private fun setAdapterViews(menuEntity: List<MenuEntity>) {
        if (CommonUtils.checkIsTablet(requireContext())) {
            val layoutManager = FlexboxLayoutManager(context)
            layoutManager.flexDirection = FlexDirection.ROW
            layoutManager.justifyContent = JustifyContent.CENTER
            binding.rvActivitiesList.layoutManager = layoutManager
        } else {
            val layoutManager = GridLayoutManager(context, 2)
            binding.rvActivitiesList.layoutManager = layoutManager
        }
        binding.rvActivitiesList.adapter = DashboardMenuItemsAdapter(menuEntity, this)

    }

    override fun onMenuSelected(menuId: String, subModule: String?) {
        when (menuId) {
            MenuConstants.HOUSEHOLD_MENU_ID -> {
                startActivity(Intent(requireContext(), HouseholdSearchActivity::class.java))
            }

            MenuConstants.MY_PATIENTS_MENU_ID -> {
                val bundle = Bundle().apply {
                    putString(DefinedParams.ORIGIN, MenuConstants.MY_PATIENTS_MENU_ID)
                }
                val intent = if (CommonUtils.isCommunity()) Intent(
                    requireContext(),
                    FollowUpMyPatientActivity::class.java
                ) else Intent(requireContext(), PatientSearchActivity ::class.java)

                intent.putExtras(bundle)
                if (CommonUtils.isCommunity()) {
                    startActivity(intent)
                } else {
                    withNetworkAvailability(online = {
                        startActivity(intent)
                    })
                }
            }

            MenuConstants.PerformanceMonitoring_ID -> {
                if (connectivityManager.isNetworkAvailable()) {
                    val intent = Intent(requireContext(),PerformanceMonitoringActivity::class.java)
                    startActivity(intent)
                } else {
                    (activity as BaseActivity?)?.showErrorDialogue(
                        getString(R.string.title_no_network),
                        getString(R.string.message_no_network),
                        isNegativeButtonNeed = false
                    ) { _ -> }
                }

            }

            /*  NCD WorkFlow                           */
            MenuConstants.SCREENING ->{
                startActivity(Intent(requireContext(), ScreeningActivity::class.java))
            }

            MenuConstants.REGISTRATION -> {
                withNetworkAvailability(online = {
                    val bundle = Bundle().apply {
                        putString(DefinedParams.ORIGIN, MenuConstants.REGISTRATION.lowercase())
                    }
                    val intent = Intent(requireContext(), PatientSearchActivity::class.java)
                    intent.putExtras(bundle)
                    startActivity(intent)
                })
            }

            MenuConstants.ASSESSMENT -> {
                val bundle = Bundle().apply {
                    putString(DefinedParams.ORIGIN, MenuConstants.ASSESSMENT.lowercase())
                }
                val intent = Intent(requireContext(), PatientSearchActivity::class.java)
                intent.putExtras(bundle)
                startActivity(intent)
            }

            MenuConstants.DISPENSE -> {
                val bundle = Bundle().apply {
                    putString(DefinedParams.ORIGIN, MenuConstants.DISPENSE.lowercase())
                }
                val intent = Intent(requireContext(), PatientSearchActivity::class.java)
                intent.putExtras(bundle)
                startActivity(intent)
            }

            MenuConstants.DASHBOARD -> {
                withNetworkAvailability(online = {
                    val intent = Intent(requireContext(), NCDDashboardViewActivity::class.java)
                    startActivity(intent)
                })
            }

            MenuConstants.LIFESTYLE -> {
                val bundle = Bundle().apply {
                    putString(DefinedParams.ORIGIN, MenuConstants.LIFESTYLE.lowercase())
                }
                val intent = Intent(requireContext(), PatientSearchActivity::class.java)
                intent.putExtras(bundle)
                startActivity(intent)
            }

            MenuConstants.PSYCHOLOGICAL -> {
                val bundle = Bundle().apply {
                    putString(DefinedParams.ORIGIN, MenuConstants.PSYCHOLOGICAL.lowercase())
                }
                val intent = Intent(requireContext(), PatientSearchActivity::class.java)
                intent.putExtras(bundle)
                startActivity(intent)
            }

            MenuConstants.INVESTIGATION -> {
                val bundle = Bundle().apply {
                    putString(DefinedParams.ORIGIN, MenuConstants.INVESTIGATION.lowercase())
                }
                val intent = Intent(requireContext(), PatientSearchActivity::class.java)
                intent.putExtras(bundle)
                startActivity(intent)
            }
            MenuConstants.FOLLOW_UP -> {
                if ((CommonUtils.isChp())) {
                    val intent = Intent(requireContext(), FollowUpMyPatientActivity::class.java)
                    startActivity(intent)
                } else {
                    val intent = Intent(requireContext(), NCDFollowUpActivity::class.java)
                    startActivity(intent)
                }
            }
        }
    }
}