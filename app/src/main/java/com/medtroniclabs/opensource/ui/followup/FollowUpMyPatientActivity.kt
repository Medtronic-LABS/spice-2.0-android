package com.medtroniclabs.opensource.ui.followup

import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Typeface
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.addTextChangedListener
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.startBackgroundOfflineSync
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.databinding.ActivityFollowUpMyPatientBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.formgeneration.extension.safePopupMenuClickListener
import com.medtroniclabs.opensource.ncd.followup.NCDFollowUpUtils
import com.medtroniclabs.opensource.ncd.followup.adapter.NCDFollowUpAdapter
import com.medtroniclabs.opensource.ncd.followup.fragment.NCDCallResultBottomDialog
import com.medtroniclabs.opensource.ncd.followup.fragment.NCDFollowUpOfflineBottomDialogFilter
import com.medtroniclabs.opensource.ncd.followup.fragment.NCDFollowUpSortDialog
import com.medtroniclabs.opensource.ncd.followup.viewmodel.NCDFollowUpViewModel
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.MenuConstants
import com.medtroniclabs.opensource.ui.followup.adapter.FollowUpPatientListAdapter
import com.medtroniclabs.opensource.ui.followup.viewmodel.FollowUpViewModel
import com.medtroniclabs.opensource.ui.phuwalkins.activity.PhuWalkInsActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

class FollowUpMyPatientActivity : BaseActivity() {
    private lateinit var binding: ActivityFollowUpMyPatientBinding
    private val viewModel: FollowUpViewModel by viewModels()
    private val ncdFollowUpViewModel: NCDFollowUpViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFollowUpMyPatientBinding.inflate(layoutInflater)
        setMainContentView(
            binding.root, isToolbarVisible = true, title = if (CommonUtils.isCommunity()) {
                getString(R.string.my_patients)
            } else {
                getString(R.string.search_patient)
            }
        )
        setOrientation()
        if (CommonUtils.isCommunity()) {
            initView()
            initObserver()
            setTabLayout()
            viewModel.setUserJourney(getString(R.string.my_patients))
            showHideVerticalIcon(true)
        } else {
            initViewForNcd()
            setNCDTabLayout()
            attachObserversForNcd()
            showHideVerticalIcon(false)
        }
    }

    private fun setOrientation() {
        val isTablet =
            resources.getBoolean(R.bool.isLargeTablet) || resources.getBoolean(R.bool.isTablet)
        requestedOrientation = if (isTablet) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    private fun setNCDTabLayout() {
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
        ncdFollowUpViewModel.typeOffline = NCDFollowUpUtils.SCREENED
        binding.llExactSearch.tabLayout.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                showLoading()
                tab?.let {
                    ncdFollowUpViewModel.customDate = null
                    ncdFollowUpViewModel.sortModel = null
                    binding.llFilter.btnSort.gone()
                    ncdFollowUpViewModel.filterByDateRange = listOf()
                    ncdFollowUpViewModel.filterByVillage = listOf()
                    binding.llExactSearch.etSearchTerm.setText("")
                    ncdFollowUpViewModel.filterCount.postValue(0)
                    ncdFollowUpViewModel.sortCount.postValue(0)
                    ncdFollowUpViewModel.typeOffline = when (it.position) {
                        0 -> NCDFollowUpUtils.SCREENED
                        1 -> NCDFollowUpUtils.Assessment_Type
                        2 -> NCDFollowUpUtils.Defaulters_Type
                        3 -> NCDFollowUpUtils.LTFU_Type
                        else -> ""
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
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                hideProgressAfterDelay() // Hide loading once transition is complete
            }
        })
        binding.llExactSearch.tabLayout.getTabAt(0)?.let {
            setTabTypeface(it, ResourcesCompat.getFont(applicationContext, R.font.inter_bold))
        }
    }

    private fun hideProgressAfterDelay() {
        CoroutineScope(Dispatchers.Main).launch {
            delay(1500) // Wait for 3 seconds
            hideLoading()
        }
    }


    private fun setTabLayout() {

        binding.viewPager.adapter = FollowUpPatientListAdapter(supportFragmentManager, lifecycle)
        TabLayoutMediator(binding.llExactSearch.tabLayout, binding.viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = getString(R.string.hh_visit)
                1 -> tab.text = getString(R.string.referred)
                2 -> tab.text = getString(R.string.tab_counter_referral)
            }
        }.attach()

        binding.llExactSearch.tabLayout.getTabAt(0)?.view?.setBackgroundResource(R.drawable.left_mh_view_selector)
        binding.llExactSearch.tabLayout.getTabAt(1)?.view?.setBackgroundResource(R.drawable.mental_health_button_bg)
        binding.llExactSearch.tabLayout.getTabAt(2)?.view?.setBackgroundResource(R.drawable.right_mh_view_selector)
        val tabView3 = (binding.llExactSearch.tabLayout.getChildAt(0) as? ViewGroup)?.getChildAt(2)
        tabView3?.setPadding(0, 0, 0, 0)

        binding.llExactSearch.tabLayout.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    viewModel.updateFollowUpFilter(pageType = it.position)
                   // binding.llExactSearch.etSearchTerm.setText("")
                    binding.viewPager.currentItem = it.position

                    setTabTypeface(it, ResourcesCompat.getFont(applicationContext, R.font.inter_bold))
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                setTabTypeface(tab, ResourcesCompat.getFont(applicationContext, R.font.inter_regular))
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                // No action needed
            }
        })

        binding.llExactSearch.tabLayout.getTabAt(0)?.let {
            setTabTypeface(it, ResourcesCompat.getFont(applicationContext, R.font.inter_bold))
        }
    }

    private fun initView() {
        with(binding) {
            val origin = intent.getStringExtra(MenuConstants.MY_PATIENTS_MENU_ID)
            llFilter.btnFilter.safeClickListener {
                FollowUpFilterBottomSheetDialogFragment.newInstance()
                    .show(supportFragmentManager, FollowUpFilterBottomSheetDialogFragment.TAG)
            }

            llExactSearch.etSearchTerm.addTextChangedListener {
                val search = it?.trim().toString()
                llExactSearch.btnSearch.isEnabled = !search.isNullOrEmpty()
                if (search.isNullOrEmpty()) {
                    viewModel.updateFollowUpFilter(search = "")
                }
            }

            llExactSearch.btnSearch.setOnClickListener {
                viewModel.updateFollowUpFilter(
                    search = llExactSearch.etSearchTerm.text?.trim().toString()
                )
            }
        }
    }

    private fun initObserver() {
        viewModel.followUpPatientListLiveData.observe(this) {
            binding.tvHPatientCount.text = getString(R.string.patient_count, it.size)
        }

        viewModel.getFilterDataLiveData().observe(this) {
            var count = 0
            if (!it.selectedVillages.isNullOrEmpty())
                count++
            if (!it.selectedDateRange.isNullOrEmpty())
                count++

            if (count > 0) {
                binding.llFilter.btnFilter.text = this.getString(R.string.filter_count, count)
            } else {
                binding.llFilter.btnFilter.text = getString(R.string.filter)
            }
        }

        viewModel.addCallHistoryLiveData.observe(this) {
            when (it.state) {
                ResourceState.LOADING -> {
                    Timber.i("Saving followup call history in progress")
                }

                ResourceState.SUCCESS -> {
                    if (it.data == true)
                        startBackgroundOfflineSync()

                }

                ResourceState.ERROR -> {
                    Timber.w("Something went wrong while saving followup call history")
                }
            }
        }
    }

    private fun setTabTypeface(tab: TabLayout.Tab?,  typeface: Typeface?) {
        val count = tab?.view?.childCount ?: 0
        for (i in 0 until count) {
            val tabViewChild = tab?.view?.getChildAt(i)
            if (tabViewChild != null && tabViewChild is TextView) {
                (tabViewChild as TextView).typeface = typeface
            }
        }
    }
    private fun showHideVerticalIcon(visibility: Boolean) {
        showVerticalMoreIcon(visibility) {
            onMoreIconClicked(it)
        }
    }


    private fun onMoreIconClicked(view: View) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.menuInflater.inflate(R.menu.mypatient_menu, popupMenu.menu)
        popupMenu.safePopupMenuClickListener(object :
            android.widget.PopupMenu.OnMenuItemClickListener,
            PopupMenu.OnMenuItemClickListener {
            override fun onMenuItemClick(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.et_phuwalkins -> {
                        val intent =
                            Intent(this@FollowUpMyPatientActivity, PhuWalkInsActivity::class.java)
                        startActivity(intent)
                    }
                }
                return true
            }
        })
        popupMenu.setForceShowIcon(true)
        popupMenu.show()
    }

    private fun initViewForNcd() {
        with(binding) {
            llFilter.btnFilter.gone()
            llFilter.btnSort.gone()
            tvPatientNoFound.gone()
            llFilter.btnFilter.text = getString(R.string.filter)
            llFilter.btnSort.text = getString(R.string.sort)

            llFilter.btnFilter.safeClickListener {
                val fragment =
                    supportFragmentManager.findFragmentByTag(NCDFollowUpOfflineBottomDialogFilter.TAG)
                if (fragment == null) {
                    NCDFollowUpOfflineBottomDialogFilter.newInstance()
                        .show(supportFragmentManager, NCDFollowUpOfflineBottomDialogFilter.TAG)
                }
            }

            llFilter.btnSort.safeClickListener {
                val fragment =
                    supportFragmentManager.findFragmentByTag(NCDFollowUpSortDialog.TAG)
                if (fragment == null) {
                    NCDFollowUpSortDialog.newInstance()
                        .show(supportFragmentManager, NCDFollowUpSortDialog.TAG)
                }
            }

            llExactSearch.etSearchTerm.addTextChangedListener {
                val search = it?.trim().toString()
                llExactSearch.btnSearch.isEnabled = !search.isNullOrEmpty()
                if (search.isNullOrEmpty()) {
                    ncdFollowUpViewModel.searchLiveDataForOffline("")
                }
            }

            llExactSearch.btnSearch.setOnClickListener {
                ncdFollowUpViewModel.searchLiveDataForOffline(
                    llExactSearch.etSearchTerm.text?.trim().toString()
                )
            }
        }
    }

    private fun attachObserversForNcd() {
        ncdFollowUpViewModel.totalPatientCountOffline.observe(this) { count ->
            if (count > 0) {
                binding.tvHPatientCount.apply {
                    text = if (count == 1) getString(R.string.patient_found) else getString(
                        R.string.patients_found,
                        count
                    )
                    visible()
                }
                binding.tvPatientNoFound.gone()
                binding.llFilter.btnFilter.visible()
                binding.llFilter.btnSort.visible()
            } else {
                ncdFollowUpViewModel.filterCount.value?.let { count ->
                    if (count == 0) {
                        binding.llFilter.btnFilter.gone()
                    } else {
                        binding.llFilter.btnFilter.visible()
                    }
                } ?: binding.llFilter.btnFilter.gone()
                ncdFollowUpViewModel.sortCount.value?.let { count ->
                    if (count == 0) {
                        binding.llFilter.btnSort.gone()
                    } else {
                        binding.llFilter.btnSort.visible()
                    }
                } ?: binding.llFilter.btnSort.gone()
                binding.tvHPatientCount.text = getString(R.string.no_patients_found)
                binding.tvPatientNoFound.visible()
            }
        }
        ncdFollowUpViewModel.sortCount.observe(this) { count ->
            if (count > 0) {
                binding.llFilter.btnSort.text = this.getString(R.string.sort_count, count)
            } else {
                binding.llFilter.btnSort.text = getString(R.string.sort)
            }
        }
        ncdFollowUpViewModel.getInitialLiveData.observe(this) { resources ->
            when (resources.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    resources.data?.let {
                        val fragment =
                            supportFragmentManager.findFragmentByTag(NCDCallResultBottomDialog.TAG)
                        if (fragment == null) {
                            NCDCallResultBottomDialog.newInstance(it)
                                .show(supportFragmentManager, NCDCallResultBottomDialog.TAG)
                        }
                    }
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }
            }
        }
        ncdFollowUpViewModel.saveCallDetails.observe(this) { resources ->
            when (resources.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    SecuredPreference.putBoolean(SecuredPreference.EnvironmentKey.INITIAL_CALL.name, false)
                    val fragment =
                        supportFragmentManager.findFragmentByTag(NCDCallResultBottomDialog.TAG)
                    fragment?.let { fragment ->
                        (fragment as? NCDCallResultBottomDialog)?.dismiss()
                    }
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }
            }
        }
        ncdFollowUpViewModel.filterCount.observe(this) { count ->
            if (count > 0) {
                binding.llFilter.btnFilter.text = this.getString(R.string.filter_count, count)
            } else {
                binding.llFilter.btnFilter.text = getString(R.string.filter)
            }
        }
    }
}


