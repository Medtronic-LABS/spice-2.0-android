package org.medtroniclabs.uhis.ui.membersearch

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.hideKeyboard
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.data.offlinesync.model.SavedMemberDetails
import org.medtroniclabs.uhis.databinding.ActivityMemberSearchBinding
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.externalmember.ExternalMemberRegistrationActivity
import org.medtroniclabs.uhis.ui.household.MemberSelectionListener
import org.medtroniclabs.uhis.ui.household.summary.MemberSummaryActivity
import org.medtroniclabs.uhis.ui.membersearch.adapter.MemberSearchAdapter
import org.medtroniclabs.uhis.ui.membersearch.viewmodel.MemberSearchViewModel
import org.medtroniclabs.uhis.common.DefinedParams as CommonDefinedParams

/**
 * FO/PO member search screen with debounced text search, filters, and hybrid local + remote results.
 */
@AndroidEntryPoint
class MemberSearchActivity : BaseActivity(), View.OnClickListener, MemberSelectionListener {
    private lateinit var binding: ActivityMemberSearchBinding

    private val viewModel: MemberSearchViewModel by viewModels()

    private lateinit var adapter: MemberSearchAdapter
    private var isRefreshLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMemberSearchBinding.inflate(layoutInflater)
        setMainContentView(
            binding.root,
            isToolbarVisible = true,
            title = getString(R.string.my_patients),
        )
        initViews()
        setListeners()
        attachObservers()
    }

    override fun onResume() {
        super.onResume()
        viewModel.setUserJourney(AnalyticsDefinedParams.SERVICES)
    }

    private fun initViews() {
        binding.llExactSearch.clBtnQrSearch.gone()
        binding.llExactSearch.btnSearch.gone()
        binding.llFilter.btnFilter.text = getString(R.string.filter)
        binding.llExactSearch.etSearchTerm.hint = getString(R.string.member_name_or_phone)

        adapter = MemberSearchAdapter(
            listener = this,
            onRemotePatientClick = { patient ->
                withNetworkAvailability(online = {
                    viewModel.fetchRemoteMemberDetails(patient)
                })
            },
        )
        binding.rvMembersList.adapter = adapter
        showLoading()

        binding.bottomNavigationView.visible()
        binding.btnAddMember.safeClickListener(this)
    }

    private fun setListeners() {
        binding.llFilter.btnFilter.safeClickListener(this)
        binding.llExactSearch.etSearchTerm.doOnTextChanged { text, _, _, _ ->
            viewModel.onTextChange(text?.toString())
        }
        adapter.registerAdapterDataObserver(
            object : androidx.recyclerview.widget.RecyclerView.AdapterDataObserver() {
                override fun onChanged() = updateListState()

                override fun onItemRangeInserted(
                    positionStart: Int,
                    itemCount: Int,
                ) = updateListState()

                override fun onItemRangeRemoved(
                    positionStart: Int,
                    itemCount: Int,
                ) = updateListState()
            },
        )
    }

    /** Collects paging data, filter badge state, and load-state driven UI updates. */
    private fun attachObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    adapter.loadStateFlow.collect { loadState ->
                        handleLoadState(loadState)
                    }
                }
                launch {
                    viewModel.searchParams.collect { params ->
                        binding.llFilter.btnFilter.text = if (params.activeFilterCount > 0) {
                            getString(
                                R.string.filter_count,
                                CommonUtils.formatCountForCurrentLocale(params.activeFilterCount),
                            )
                        } else {
                            getString(R.string.filter)
                        }
                    }
                }
                launch {
                    viewModel.membersFlow.collectLatest { pagingData ->
                        adapter.submitData(pagingData)
                    }
                }
                launch {
                    viewModel.remoteMemberDetailsState.collect { resource ->
                        when (resource.state) {
                            ResourceState.LOADING -> {
                                showLoading()
                            }

                            ResourceState.SUCCESS -> {
                                resource.data?.let {
                                    hideLoading()
                                    navigateToMemberSummary(it)
                                    viewModel.clearRemoteMemberDetailsState()
                                }
                            }

                            ResourceState.ERROR -> {
                                hideLoading()
                                showErrorDialogue(
                                    title = getString(R.string.alert),
                                    message = getString(R.string.something_went_wrong_try_later),
                                    positiveButtonName = getString(R.string.ok),
                                ) { }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleLoadState(loadState: CombinedLoadStates) {
        isRefreshLoading = loadState.refresh is LoadState.Loading
        if (isRefreshLoading) {
            showLoading()
        } else {
            hideLoading()
        }
        if (loadState.append is LoadState.Loading) {
            binding.pageProgress.visible()
        } else {
            binding.pageProgress.gone()
        }
        if (loadState.refresh is LoadState.NotLoading || loadState.append is LoadState.NotLoading) {
            updateListState()
        }
    }

    private fun updateListState() {
        val count = adapter.itemCount
        val countStr = CommonUtils.formatCountForCurrentLocale(count)
        binding.tvMembersCount.text = resources.getQuantityString(
            R.plurals.plural_member,
            count,
            countStr,
        )
        if (count > 0) {
            binding.tvNoMembersFound.gone()
            binding.rvMembersList.visible()
        } else if (!isRefreshLoading) {
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

            R.id.btnAddMember -> {
                withLocationCheck {
                    viewModel.setUserJourney("ADD_MEMBER_BUTTON_FO_PO")
                    startActivity(Intent(this, ExternalMemberRegistrationActivity::class.java))
                }
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
        navigateToMemberSummary(
            SavedMemberDetails(
                localMemberId = item,
                dateOfBirth = dateOfBirth,
            ),
            houseHoldId = houseHoldId,
        )
    }

    private fun navigateToMemberSummary(
        result: SavedMemberDetails,
        houseHoldId: Long? = null,
    ) {
        val intent = Intent(this, MemberSummaryActivity::class.java)
        intent.putExtra(CommonDefinedParams.HOUSEHOLD_ID, houseHoldId)
        intent.putExtra(CommonDefinedParams.MEMBER_ID, result.localMemberId)
        intent.putExtra(CommonDefinedParams.DOB, result.dateOfBirth)
        intent.putExtra(CommonDefinedParams.ENTRY_POINT, ENTRY_POINT_MEMBER_SEARCH)
        startActivity(intent)
    }

    companion object {
        const val ENTRY_POINT_MEMBER_SEARCH = "ENTRY_POINT_MEMBER_SEARCH"
    }
}
