package com.medtroniclabs.opensource.ncd.followup.activity

import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.addTextChangedListener
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.databinding.ActivityNcdFollowUpBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.mappingkey.Screening
import com.medtroniclabs.opensource.ncd.followup.NCDFollowUpUtils
import com.medtroniclabs.opensource.ncd.followup.NCDFollowUpUtils.Assessment_Type
import com.medtroniclabs.opensource.ncd.followup.NCDFollowUpUtils.Defaulters_Type
import com.medtroniclabs.opensource.ncd.followup.NCDFollowUpUtils.LTFU_Type
import com.medtroniclabs.opensource.ncd.followup.NCDFollowUpUtils.SCREENED
import com.medtroniclabs.opensource.ncd.followup.adapter.NCDFollowUpAdapter
import com.medtroniclabs.opensource.ncd.followup.fragment.NCDCallResultBottomDialog
import com.medtroniclabs.opensource.ncd.followup.fragment.NCDFollowUpFilterDialog
import com.medtroniclabs.opensource.ncd.followup.viewmodel.NCDFollowUpAssessmentViewModel
import com.medtroniclabs.opensource.ncd.followup.viewmodel.NCDFollowUpLostViewModel
import com.medtroniclabs.opensource.ncd.followup.viewmodel.NCDFollowUpMRViewModel
import com.medtroniclabs.opensource.ncd.followup.viewmodel.NCDFollowUpScreenedViewModel
import com.medtroniclabs.opensource.ncd.followup.viewmodel.NCDFollowUpViewModel
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NCDFollowUpActivity : BaseActivity() {
    private lateinit var binding: ActivityNcdFollowUpBinding
    private val viewModel: NCDFollowUpViewModel by viewModels()
    private val ncdFollowUpSearchViewModel: NCDFollowUpScreenedViewModel by viewModels()
    private val ncdFollowUpAssessmentViewModel: NCDFollowUpAssessmentViewModel by viewModels()
    private val ncdFollowUpMrViewModel: NCDFollowUpMRViewModel by viewModels()
    private val ncdFollowUpLostViewModel: NCDFollowUpLostViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = if (CommonUtils.checkIsTablet(this)) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        binding = ActivityNcdFollowUpBinding.inflate(layoutInflater)
        setMainContentView(
            binding.root,
            title = getString(R.string.search_patient),
            isToolbarVisible = true,
            homeAndBackVisibility = Pair(true, true)
        )
        initView()
        setTabLayout()
        attachObservers()
    }

    private fun initView() {
        with(binding) {
            binding.llFilter.btnFilter.text = getString(R.string.filter)
            binding.tvPatientNoFound.visible()
            binding.llFilter.btnFilter.visible()
            binding.llFilter.btnSort.gone()
            llFilter.btnFilter.safeClickListener {
                val fragment = supportFragmentManager.findFragmentByTag(NCDFollowUpFilterDialog.TAG)
                if (fragment == null) {
                    NCDFollowUpFilterDialog.newInstance()
                        .show(supportFragmentManager, NCDFollowUpFilterDialog.TAG)
                }
            }
            llExactSearch.etSearchTerm.addTextChangedListener {
                val search = it?.trim().toString()
                llExactSearch.btnSearch.isEnabled = !search.isNullOrEmpty()
                if (search.isNullOrEmpty() && !isInitial) {
                    withNetworkAvailability(
                        online = {
                            viewModel.searchText = ""
                            when (viewModel.type) {
                                SCREENED -> {
                                    ncdFollowUpSearchViewModel.searchLiveData()
                                }

                                Assessment_Type -> {
                                    ncdFollowUpAssessmentViewModel.searchLiveData()
                                }

                                Defaulters_Type -> {
                                    ncdFollowUpMrViewModel.searchLiveData()
                                }

                                LTFU_Type -> {
                                    ncdFollowUpLostViewModel.searchLiveData()
                                }

                                else -> {
                                    ncdFollowUpSearchViewModel.searchLiveData()
                                }
                            }
                        },
                        isErrorShow = false
                    )
                }
                isInitial = false
            }
            llExactSearch.btnSearch.safeClickListener {
                withNetworkAvailability(online = {
                    viewModel.searchText =
                        llExactSearch.etSearchTerm.text?.trim().toString()
                    when (viewModel.type) {
                        SCREENED -> {
                            ncdFollowUpSearchViewModel.searchLiveData()
                        }

                        Assessment_Type -> {
                            ncdFollowUpAssessmentViewModel.searchLiveData()
                        }

                        Defaulters_Type -> {
                            ncdFollowUpMrViewModel.searchLiveData()
                        }

                        LTFU_Type -> {
                            ncdFollowUpLostViewModel.searchLiveData()
                        }

                        else -> {
                            ncdFollowUpSearchViewModel.searchLiveData()
                        }
                    }
                })
            }
        }
    }

    fun attachObservers() {
        viewModel.getPatientRegisterResponse.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {}

                ResourceState.SUCCESS -> {
                    if (resourceState.data?.id != null) {
                        hideLoading()
                        val fragment =
                            supportFragmentManager.findFragmentByTag(NCDCallResultBottomDialog.TAG)
                        if (fragment == null) {
                            NCDCallResultBottomDialog.newInstance()
                                .show(supportFragmentManager, NCDCallResultBottomDialog.TAG)
                        }
                    } else {
                        withNetworkAvailability(online = {
                            when (viewModel.type) {
                                SCREENED -> ncdFollowUpSearchViewModel.triggerGetStatus()
                                Assessment_Type -> ncdFollowUpAssessmentViewModel.triggerGetStatus()
                                Defaulters_Type -> ncdFollowUpMrViewModel.triggerGetStatus()
                                LTFU_Type -> ncdFollowUpLostViewModel.triggerGetStatus()
                                else -> ncdFollowUpSearchViewModel.triggerGetStatus()
                            }
                        })
                    }
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }
            }
        }
        viewModel.statusUpdateResponse.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {}

                ResourceState.SUCCESS -> {
                    hideLoading()
                    (supportFragmentManager.findFragmentByTag(NCDCallResultBottomDialog.TAG) as? NCDCallResultBottomDialog)?.dismiss()

                    val dataMap = resourceState.data
                    val isInitiated =
                        dataMap?.get(NCDFollowUpUtils.isInitiated) as? Boolean ?: false

                    if (!isInitiated) {
                        when (viewModel.type) {
                            SCREENED -> ncdFollowUpSearchViewModel.triggerGetStatus()
                            Assessment_Type -> ncdFollowUpAssessmentViewModel.triggerGetStatus()
                            Defaulters_Type -> ncdFollowUpMrViewModel.triggerGetStatus()
                            LTFU_Type -> ncdFollowUpLostViewModel.triggerGetStatus()
                            else -> ncdFollowUpSearchViewModel.triggerGetStatus()
                        }
                    } else {
                        val phoneNumber = dataMap?.get(Screening.phoneNumber) as? String
                        if (!phoneNumber.isNullOrBlank()) {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:$phoneNumber")
                            }
                            startActivity(intent)
                        }
                    }
                }

                ResourceState.ERROR -> {
                    hideLoading()
                    resourceState.message?.let { message ->
                        showErrorDialogue(
                            getString(R.string.error),
                            message,
                            isNegativeButtonNeed = false
                        ) {}
                    }
                }
            }
        }

        viewModel.totalPatientCount.observe(this) {
            it?.let { count ->
                if (count > 0) {
                    binding.tvHPatientCount.apply {
                        text = if (count == 1) getString(R.string.patient_found) else getString(
                            R.string.patients_found,
                            count
                        )
                        visibility = View.VISIBLE
                    }
                    binding.tvPatientNoFound.gone()
                    binding.llFilter.btnFilter.visible()
                } else {
                    viewModel.filterCount.value?.let { count ->
                        if (count == 0) {
                            binding.llFilter.btnFilter.gone()
                        } else {
                            binding.llFilter.btnFilter.visible()
                        }
                    } ?: binding.llFilter.btnFilter.gone()
                    binding.tvHPatientCount.text = getString(R.string.no_patients_found)
                    binding.tvPatientNoFound.visible()
                }
            } ?: run {
                binding.llFilter.btnFilter.gone()
                binding.tvPatientNoFound.gone()
                binding.tvHPatientCount.text = getString(R.string.no_patients_found)
            }
        }
        viewModel.filterCount.observe(this) { count ->
            if (count > 0) {
                binding.llFilter.btnFilter.text = this.getString(R.string.filter_count, count)
            } else {
                binding.llFilter.btnFilter.text = getString(R.string.filter)
            }
        }
        viewModel.filterSet.observe(this) {
            when (viewModel.type) {
                SCREENED -> ncdFollowUpSearchViewModel.triggerGetStatus()
                Assessment_Type -> ncdFollowUpAssessmentViewModel.triggerGetStatus()
                Defaulters_Type -> ncdFollowUpMrViewModel.triggerGetStatus()
                LTFU_Type -> ncdFollowUpLostViewModel.triggerGetStatus()
                else -> ncdFollowUpSearchViewModel.triggerGetStatus()
            }
        }
    }

    var isInitial = false
    private fun setTabLayout() {
        binding.llExactSearch.etSearchTerm.hint = getString(R.string.search_by_national_id)
        binding.viewPager.adapter = NCDFollowUpAdapter(supportFragmentManager, lifecycle)
        TabLayoutMediator(binding.llExactSearch.tabLayout, binding.viewPager) { tab, position ->
            val tabTitles = listOf(
                getString(R.string.referred),
                getString(R.string.assessment),
                getString(R.string.defaulters),
                getString(R.string.LTFU)
            )
            tab.text = tabTitles.getOrNull(position) ?: ""
        }.attach()
        binding.llExactSearch.tabLayout.getTabAt(0)?.view?.setBackgroundResource(R.drawable.left_mh_view_selector)
        binding.llExactSearch.tabLayout.getTabAt(1)?.view?.setBackgroundResource(R.drawable.mental_health_button_bg)
        binding.llExactSearch.tabLayout.getTabAt(2)?.view?.setBackgroundResource(R.drawable.mental_health_button_bg)
        binding.llExactSearch.tabLayout.getTabAt(3)?.view?.setBackgroundResource(R.drawable.right_mh_view_selector)
        val tabView1 = (binding.llExactSearch.tabLayout.getChildAt(0) as? ViewGroup)?.getChildAt(0)
        tabView1?.setPadding(0, 0, 0, 0)
        val tabView2 = (binding.llExactSearch.tabLayout.getChildAt(0) as? ViewGroup)?.getChildAt(1)
        tabView2?.setPadding(0, 0, 0, 0)
        val tabView3 = (binding.llExactSearch.tabLayout.getChildAt(0) as? ViewGroup)?.getChildAt(2)
        tabView3?.setPadding(0, 0, 0, 0)
        viewModel.type = SCREENED
        binding.llExactSearch.tabLayout.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    isInitial = true
                    binding.llExactSearch.etSearchTerm.setText("")
                    viewModel.searchText = ""
                    viewModel.customDate = null
                    viewModel.dateRange = null
                    viewModel.remainingAttempts = listOf()
                    binding.llFilter.btnFilter.gone()
                    binding.tvPatientNoFound.gone()
                    binding.tvHPatientCount.text = getString(R.string.no_patients_found)
                    viewModel.filterCount.postValue(0)
                    viewModel.type = when (it.position) {
                        0 -> SCREENED
                        1 -> Assessment_Type
                        2 -> Defaulters_Type
                        3 -> LTFU_Type
                        else -> SCREENED
                    }
                    when (viewModel.type) {
                        SCREENED -> ncdFollowUpSearchViewModel.triggerGetStatus()
                        Assessment_Type -> ncdFollowUpAssessmentViewModel.triggerGetStatus()
                        Defaulters_Type -> ncdFollowUpMrViewModel.triggerGetStatus()
                        LTFU_Type -> ncdFollowUpLostViewModel.triggerGetStatus()
                        else -> ncdFollowUpSearchViewModel.triggerGetStatus()
                    }
                    binding.viewPager.currentItem = it.position

                    setTabTypeface(
                        it,
                        ResourcesCompat.getFont(applicationContext, R.font.inter_bold)
                    )
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                setTabTypeface(
                    tab,
                    ResourcesCompat.getFont(applicationContext, R.font.inter_regular)
                )
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                // No action needed
            }
        })

        binding.llExactSearch.tabLayout.getTabAt(0)?.let {
            setTabTypeface(it, ResourcesCompat.getFont(applicationContext, R.font.inter_bold))
        }
    }


    private fun setTabTypeface(tab: TabLayout.Tab?, typeface: Typeface?) {
        val count = tab?.view?.childCount ?: 0
        for (i in 0 until count) {
            val tabViewChild = tab?.view?.getChildAt(i)
            if (tabViewChild != null && tabViewChild is TextView) {
                tabViewChild.typeface = typeface
            }
        }
    }

    override fun onResume() {
        super.onResume()
        withNetworkAvailability(online = {
            viewModel.getPatientCallRegister()
        })
    }
}