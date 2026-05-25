package org.medtroniclabs.uhis.ui.followup

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Typeface
import android.os.Bundle
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doOnTextChanged
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams.COUNTER_REFERRAL_TAB
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams.HH_VISIT_TAB
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams.MYFOLLOWUPPATIENTS
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams.ONMOREBUTTONTRIGGERED
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams.REFERRED_TAB
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.startBackgroundOfflineSync
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.databinding.ActivityFollowUpMyPatientBinding
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.extension.safePopupMenuClickListener
import org.medtroniclabs.uhis.ncd.followup.NCDFollowUpUtils
import org.medtroniclabs.uhis.ncd.followup.adapter.NCDFollowUpAdapter
import org.medtroniclabs.uhis.ncd.followup.fragment.NCDCallResultBottomDialog
import org.medtroniclabs.uhis.ncd.followup.fragment.NCDFollowUpOfflineBottomDialogFilter
import org.medtroniclabs.uhis.ncd.followup.fragment.NCDFollowUpSortDialog
import org.medtroniclabs.uhis.ncd.followup.viewmodel.NCDFollowUpViewModel
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.followup.adapter.FollowUpPatientListAdapter
import org.medtroniclabs.uhis.ui.followup.viewmodel.FollowUpViewModel
import org.medtroniclabs.uhis.ui.phuwalkins.activity.PhuWalkInsActivity
import timber.log.Timber

class FollowUpMyPatientActivity : BaseActivity() {
    private lateinit var binding: ActivityFollowUpMyPatientBinding
    private val viewModel: FollowUpViewModel by viewModels()
    private val ncdFollowUpViewModel: NCDFollowUpViewModel by viewModels()

    private lateinit var textChange: TextWatcher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFollowUpMyPatientBinding.inflate(layoutInflater)
        setMainContentView(
            binding.root,
            isToolbarVisible = true,
            title = if (CommonUtils.isCommunity()) {
                getString(R.string.my_patients)
            } else {
                getString(R.string.search_patient)
            },
        )
        if (CommonUtils.isCommunity()) {
            initView()
            initObserver()
            setTabLayout()
            showHideVerticalIcon(true)
        } else {
            initViewForNcd()
            setNCDTabLayout()
            attachObserversForNcd()
            showHideVerticalIcon(false)
        }
        setOrientation()
    }

    private fun attachTextWatcher() {
        textChange = binding.llExactSearch.etSearchTerm.doOnTextChanged { text, _, _, _ ->
            viewModel.onTextChange(text?.toString())
        }
    }

    private fun setOrientation() {
        val isTablet =
            resources.getBoolean(R.bool.isLargeTablet) || resources.getBoolean(R.bool.isTablet)
        requestedOrientation = if (isTablet && CommonUtils.isNonCommunity()) {
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
                getString(R.string.LTFU),
            )
            tab.text = tabTitles.getOrNull(position) ?: ""
        }.attach()
        binding.llExactSearch.tabLayout
            .getTabAt(0)
            ?.view
            ?.setBackgroundResource(R.drawable.left_mh_view_selector)
        binding.llExactSearch.tabLayout
            .getTabAt(1)
            ?.view
            ?.setBackgroundResource(R.drawable.mental_health_button_bg)
        binding.llExactSearch.tabLayout
            .getTabAt(2)
            ?.view
            ?.setBackgroundResource(R.drawable.mental_health_button_bg)
        binding.llExactSearch.tabLayout
            .getTabAt(3)
            ?.view
            ?.setBackgroundResource(R.drawable.right_mh_view_selector)
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
                        ResourcesCompat.getFont(applicationContext, R.font.inter_bold),
                    )
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                setTabTypeface(
                    tab,
                    ResourcesCompat.getFont(applicationContext, R.font.inter_regular),
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

    override fun onResume() {
        super.onResume()
        viewModel.setUserJourney(MYFOLLOWUPPATIENTS)
    }

    private fun setTabLayout() {
        binding.viewPager.adapter = FollowUpPatientListAdapter(supportFragmentManager, lifecycle)
        TabLayoutMediator(binding.llExactSearch.tabLayout, binding.viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = getString(R.string.scheduled_visit)
                1 -> tab.text = getString(R.string.referred)
            }
        }.attach()

        binding.llExactSearch.tabLayout
            .getTabAt(0)
            ?.view
            ?.setBackgroundResource(R.drawable.left_mh_view_selector)
        binding.llExactSearch.tabLayout
            .getTabAt(1)
            ?.view
            ?.setBackgroundResource(R.drawable.mental_health_button_bg)
        binding.llExactSearch.tabLayout
            .getTabAt(2)
            ?.view
            ?.setBackgroundResource(R.drawable.right_mh_view_selector)
        val tabView3 = (binding.llExactSearch.tabLayout.getChildAt(0) as? ViewGroup)?.getChildAt(2)
        tabView3?.setPadding(0, 0, 0, 0)

        binding.llExactSearch.tabLayout.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    viewModel.updateFollowUpFilter(pageType = it.position)
                    // binding.llExactSearch.etSearchTerm.setText("")
                    binding.viewPager.currentItem = it.position
                    getTabScreenName(it.position)?.let { tabName ->
                        viewModel.setUserJourney(tabName)
                    }
                    setTabTypeface(
                        it,
                        ResourcesCompat.getFont(applicationContext, R.font.inter_bold),
                    )
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
            viewModel.setUserJourney(HH_VISIT_TAB)
            setTabTypeface(it, ResourcesCompat.getFont(applicationContext, R.font.inter_bold))
        }
    }

    private fun getTabScreenName(position: Int): String? =
        when (position) {
            0 -> HH_VISIT_TAB
            1 -> REFERRED_TAB
            2 -> COUNTER_REFERRAL_TAB
            else -> null
        }

    private fun initView() {
        with(binding) {
            llFilter.btnFilter.safeClickListener {
                FollowUpFilterBottomSheetDialogFragment
                    .newInstance()
                    .show(supportFragmentManager, FollowUpFilterBottomSheetDialogFragment.TAG)
            }
            binding.llExactSearch.btnSearch.gone()
            attachTextWatcher()
        }
    }

    private fun initObserver() {
        viewModel.followUpPatientListLiveData.observe(this) {
            binding.tvHPatientCount.text = getString(R.string.patient_count, CommonUtils.formatCountForCurrentLocale(it.size))
        }

        viewModel.getFilterDataLiveData().observe(this) {
            var count = 0
            if (!it.selectedShashtyaShebikas.isNullOrEmpty()) {
                count++
            }
            if (!it.selectedVillages.isNullOrEmpty()) {
                count++
            }
            if (!it.selectedDateRange.isNullOrEmpty()) {
                count++
            }
            if (!it.selectedReferralReasons.isNullOrEmpty()) {
                count++
            }
            if (!it.ncdSelectedReasons.isNullOrEmpty()) {
                count++
            }
            if (!it.ncdSelectedReferralTo.isNullOrEmpty()) {
                count++
            }

            if (count > 0) {
                binding.llFilter.btnFilter.text = this.getString(R.string.filter_count, CommonUtils.formatCountForCurrentLocale(count))
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
                    if (it.data == true) {
                        startBackgroundOfflineSync()
                    }
                }

                ResourceState.ERROR -> {
                    Timber.w("Something went wrong while saving followup call history")
                }
            }
        }
    }

    private fun setTabTypeface(
        tab: TabLayout.Tab?,
        typeface: Typeface?,
    ) {
        val count = tab?.view?.childCount ?: 0
        for (i in 0 until count) {
            val tabViewChild = tab?.view?.getChildAt(i)
            if (tabViewChild != null && tabViewChild is TextView) {
                tabViewChild.typeface = typeface
            }
        }
    }

    private fun showHideVerticalIcon(visibility: Boolean) {
        showVerticalMoreIcon(visibility) {
            onMoreIconClicked(it)
        }
    }

    private fun onMoreIconClicked(view: View) {
        viewModel.setUserJourney(ONMOREBUTTONTRIGGERED)
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
                    NCDFollowUpOfflineBottomDialogFilter
                        .newInstance()
                        .show(supportFragmentManager, NCDFollowUpOfflineBottomDialogFilter.TAG)
                }
            }

            llFilter.btnSort.safeClickListener {
                val fragment =
                    supportFragmentManager.findFragmentByTag(NCDFollowUpSortDialog.TAG)
                if (fragment == null) {
                    NCDFollowUpSortDialog
                        .newInstance()
                        .show(supportFragmentManager, NCDFollowUpSortDialog.TAG)
                }
            }

            llExactSearch.etSearchTerm.addTextChangedListener {
                val search = it?.trim().toString()
                llExactSearch.btnSearch.isEnabled = search.isNotEmpty()
                if (search.isEmpty()) {
                    ncdFollowUpViewModel.searchLiveDataForOffline("")
                }
            }

            llExactSearch.btnSearch.setOnClickListener {
                ncdFollowUpViewModel.searchLiveDataForOffline(
                    llExactSearch.etSearchTerm.text
                        ?.trim()
                        .toString(),
                )
            }
        }
    }

    private fun attachObserversForNcd() {
        ncdFollowUpViewModel.totalPatientCountOffline.observe(this) { count ->
            if (count > 0) {
                binding.tvHPatientCount.apply {
                    text = if (count == 1) {
                        getString(R.string.patient_found)
                    } else {
                        getString(
                            R.string.patients_found,
                            count,
                        )
                    }
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
                binding.llFilter.btnSort.text = this.getString(R.string.sort_count, CommonUtils.formatCountForCurrentLocale(count))
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
                            NCDCallResultBottomDialog
                                .newInstance(it)
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
                binding.llFilter.btnFilter.text = this.getString(R.string.filter_count, CommonUtils.formatCountForCurrentLocale(count))
            } else {
                binding.llFilter.btnFilter.text = getString(R.string.filter)
            }
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
        setOrientation()
    }
}
