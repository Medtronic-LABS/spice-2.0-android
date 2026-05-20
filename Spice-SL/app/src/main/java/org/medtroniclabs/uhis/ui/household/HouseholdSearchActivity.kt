package org.medtroniclabs.uhis.ui.household

import android.content.Intent
import android.os.Bundle
import android.text.TextWatcher
import android.view.View
import androidx.activity.viewModels
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams.HOUSEHOLDLISTSEARCHTRIGGERED
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams.HOUSEHOLDS
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.hideKeyboard
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.DefinedParams.isHouseHold
import org.medtroniclabs.uhis.databinding.ActivityHouseholdSearchBinding
import org.medtroniclabs.uhis.db.dao.HouseholdSortOrder
import org.medtroniclabs.uhis.db.response.HouseHoldEntityWithLastActivity
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.dashboard.ncd.DashboardConstants
import org.medtroniclabs.uhis.ui.household.HouseholdDefinedParams.IS_FROM_HOUSEHOLD_REGISTRATION
import org.medtroniclabs.uhis.ui.household.adapter.HouseholdListAdapter
import org.medtroniclabs.uhis.ui.household.summary.HouseholdSummaryActivity
import org.medtroniclabs.uhis.ui.household.viewmodel.HouseholdListViewModel

@AndroidEntryPoint
class HouseholdSearchActivity : BaseActivity(), View.OnClickListener {
    private lateinit var binding: ActivityHouseholdSearchBinding
    private val householdListViewModel: HouseholdListViewModel by viewModels()
    private lateinit var householdListAdapter: HouseholdListAdapter
    private var preSelectedSsIds: LongArray = longArrayOf()
    private var preSelectedSubVillageIds: LongArray = longArrayOf()

    private lateinit var textChange: TextWatcher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHouseholdSearchBinding.inflate(layoutInflater)
        setMainContentView(
            binding.root,
            isToolbarVisible = true,
            title = getString(R.string.households),
        )
        showLoading()
        preSelectedSsIds = intent.getLongArrayExtra(DashboardConstants.EXTRA_DASHBOARD_SS_IDS) ?: longArrayOf()
        preSelectedSubVillageIds = intent.getLongArrayExtra(DashboardConstants.EXTRA_DASHBOARD_SUB_VILLAGE_IDS) ?: longArrayOf()
        initViews()
        applyPrefiltersFromDashboard()
        setListeners()
        attachObserver()
    }

    private fun applyPrefiltersFromDashboard() {
        if (preSelectedSsIds.isEmpty() && preSelectedSubVillageIds.isEmpty()) return
        val ssFilters = preSelectedSsIds.map {
            org.medtroniclabs.uhis.data.model
                .ChipViewItemModel(id = it, name = "")
        }
        val subVillageFilters = preSelectedSubVillageIds.map {
            org.medtroniclabs.uhis.data.model
                .ChipViewItemModel(id = it, name = "")
        }
        householdListViewModel.setFilterLiveData(
            ssFilter = ssFilters,
            subVillagesFilter = subVillageFilters,
        )
    }

    override fun onResume() {
        super.onResume()
        householdListViewModel.setUserJourney(HOUSEHOLDS)
    }

    private fun initViews() {
        binding.llExactSearch.btnSearch.gone()
        binding.llFilter.btnFilter.text = getString(R.string.filter)
        binding.llFilter.btnSort.visible()
        binding.llFilter.btnSort.setText(R.string.sort)
        binding.llExactSearch.etSearchTerm.hint = getString(R.string.household_name_or_no)

        householdListAdapter = HouseholdListAdapter {
            val intent = Intent(this@HouseholdSearchActivity, HouseholdSummaryActivity::class.java)
            intent.putExtra(DefinedParams.householdId, it)
            intent.putExtra(IS_FROM_HOUSEHOLD_REGISTRATION, false)
            startActivity(intent)
        }

        binding.rvHouseholdList.apply {
            layoutManager = LinearLayoutManager(this@HouseholdSearchActivity)
            adapter = householdListAdapter
        }
    }

    private fun setListeners() {
        binding.llExactSearch.btnSearch.safeClickListener(this)
        binding.btnAddHousehold.safeClickListener(this)
        binding.llFilter.btnFilter.safeClickListener(this)
        binding.llFilter.btnSort.safeClickListener(this)
        textChange = binding.llExactSearch.etSearchTerm.doOnTextChanged { text, _, _, _ ->
            householdListViewModel.onTextChange(text?.toString())
        }
    }

    private fun attachObserver() {
        householdListViewModel.getFilterLiveData().observe(this) {
            var count = 0
            if (it.filterBySs.isNotEmpty()) {
                count++
            }
            if (it.filterBySubVillages.isNotEmpty()) {
                count++
            }

            if (count > 0) {
                binding.llFilter.btnFilter.text =
                    this.getString(R.string.filter_count, CommonUtils.formatCountForCurrentLocale(count))
            } else {
                binding.llFilter.btnFilter.text = getString(R.string.filter)
            }
            val sortCount = if (it.sortOrder == HouseholdSortOrder.DEFAULT) 0 else 1
            binding.llFilter.btnSort.text = if (sortCount > 0) {
                getString(R.string.sort_count, CommonUtils.formatCountForCurrentLocale(sortCount))
            } else {
                getString(R.string.sort)
            }
        }

        householdListViewModel.filteredHouseholdsLiveData.observe(this) {
            hideLoading()
            it?.let {
                setHouseholdListAdapter(it)
            } ?: kotlin.run {
                setHouseholdListAdapter(ArrayList())
            }
        }
    }

    private fun setHouseholdListAdapter(householdList: List<HouseHoldEntityWithLastActivity>) {
        binding.tvHouseHoldCount.text = setLabelValue(householdList.size)
        if (householdList.isNotEmpty()) {
            binding.llFilter.btnFilter.visible()
            binding.tvNoHouseHoldFound.gone()
            binding.rvHouseholdList.visible()
            householdListAdapter.setHouseHoldList(householdList)
        } else {
            binding.tvNoHouseHoldFound.visible()
            binding.rvHouseholdList.gone()
        }
    }

    private fun setLabelValue(size: Int): CharSequence {
        val countStr = CommonUtils.formatCountForCurrentLocale(size)
        return resources.getQuantityString(
            R.plurals.plural_household_found,
            size,
            countStr,
        )
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.btnAddHousehold -> {
                withLocationCheck({
                    householdListViewModel.setUserJourney(AnalyticsDefinedParams.ADDHOUSEHOLDBUTTONTRIGGERED)
                    launchHouseholdActivity()
                })
            }

            R.id.btnSearch -> {
                withLocationCheck({
                    householdListViewModel.setUserJourney(HOUSEHOLDLISTSEARCHTRIGGERED)
                    val searchTerm = binding.llExactSearch.etSearchTerm.text
                        .toString()
                    householdListViewModel.setFilterLiveData(search = searchTerm)
                })
            }

            R.id.btnFilter -> {
                hideKeyboard(view)
                withLocationCheck({
                    FilterBottomSheetDialogFragment
                        .newInstance()
                        .show(supportFragmentManager, FilterBottomSheetDialogFragment.TAG)
                })
            }
            R.id.btnSort -> {
                hideKeyboard(view)
                handleSortClick()
            }
        }
    }

    private fun handleSortClick() {
        val existingFragment =
            supportFragmentManager.findFragmentByTag(SortDialogFragment.TAG) as? SortDialogFragment
        if (existingFragment == null) {
            SortDialogFragment
                .newInstance()
                .show(supportFragmentManager, SortDialogFragment.TAG)
        } else {
            existingFragment.show(supportFragmentManager, SortDialogFragment.TAG)
        }
    }

    private fun launchHouseholdActivity() {
        val intent = Intent(this, ConsentFormActivity::class.java)
        intent.putExtra(isHouseHold, true)
        startActivity(intent)
    }
}
