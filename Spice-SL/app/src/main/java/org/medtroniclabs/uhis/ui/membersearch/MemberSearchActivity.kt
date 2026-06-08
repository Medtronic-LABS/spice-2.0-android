package org.medtroniclabs.uhis.ui.membersearch

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.TextWatcher
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.hideKeyboard
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.qrscanner.QRScanContract
import org.medtroniclabs.uhis.common.qrscanner.QRScanResult
import org.medtroniclabs.uhis.common.qrscanner.QRScannerActivity
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
import org.medtroniclabs.uhis.ui.patient.UIConstants
import org.medtroniclabs.uhis.common.DefinedParams as CommonDefinedParams

/**
 * FO/PO member search screen with debounced text search, filters, and hybrid local + remote results.
 */
@AndroidEntryPoint
class MemberSearchActivity : BaseActivity(), View.OnClickListener, MemberSelectionListener {
    private lateinit var binding: ActivityMemberSearchBinding

    private val viewModel: MemberSearchViewModel by viewModels()

    private lateinit var adapter: MemberSearchAdapter

    /**
     * Search text listener being used for adding text based search.
     * We are removing this listener when scanning a valid QR code
     * and adding again as the search text is also getting cleared
     */
    private lateinit var searchTextListener: TextWatcher

    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            startScanning()
        } else {
            // Camera permission denied
        }
    }

    private val qrScanLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(QRScanContract()) { result ->
            val qrCode = result.resultString?.trim().orEmpty()
            if (qrCode.isNotBlank()) {
                viewModel.filterMemberListByQr(qrCode)
                binding.llExactSearch.etSearchTerm.removeTextChangedListener(searchTextListener)
                binding.llExactSearch.etSearchTerm.text
                    ?.clear()
                binding.llExactSearch.etSearchTerm.addTextChangedListener(searchTextListener)
            } else {
                showErrorDialogue(
                    title = getString(R.string.alert),
                    message = getString(R.string.invalid_qr),
                    positiveButtonName = getString(R.string.ok),
                ) { }
            }
        }

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
        binding.llExactSearch.clBtnQrSearch.visible()
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
        adapter.addOnPagesUpdatedListener { updateListState() }
        adapter.registerAdapterDataObserver(
            object : RecyclerView.AdapterDataObserver() {
                override fun onItemRangeInserted(
                    positionStart: Int,
                    itemCount: Int,
                ) {
                    super.onItemRangeInserted(positionStart, itemCount)
                    if (positionStart == 0) {
                        binding.rvMembersList.scrollToPosition(0)
                    }
                }
            },
        )
        showLoading()

        binding.bottomNavigationView.visible()
        binding.btnAddMember.safeClickListener(this)
    }

    private fun setListeners() {
        binding.llFilter.btnFilter.safeClickListener(this)
        binding.llExactSearch.btnQrSearch.safeClickListener(this)
        searchTextListener = binding.llExactSearch.etSearchTerm.doOnTextChanged { text, _, _, _ ->
            viewModel.onTextChange(text?.toString())
        }
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
                        adapter.submitData(lifecycle, pagingData)
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
        if (loadState.refresh is LoadState.Loading) {
            showLoading()
        } else {
            hideLoading()
        }
        if (loadState.append is LoadState.Loading) {
            binding.pageProgress.visible()
        } else {
            binding.pageProgress.gone()
        }
        updateListState()
    }

    private fun updateListState() {
        val count = adapter.itemCount
        val countStr = CommonUtils.formatCountForCurrentLocale(count)
        binding.tvMembersCount.text = resources.getQuantityString(
            R.plurals.plural_member,
            count,
            countStr,
        )
        if (count == 0) {
            binding.tvNoMembersFound.visible()
            binding.rvMembersList.gone()
        } else {
            binding.tvNoMembersFound.gone()
            binding.rvMembersList.visible()
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

            R.id.btnQrSearch -> {
                viewModel.setUserJourney(AnalyticsDefinedParams.SERVICES_QR_SEARCH_TRIGGERED)
                launchQrScanner()
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

    private fun launchQrScanner() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED
            ) {
                cameraPermission.launch(Manifest.permission.CAMERA)
            } else {
                startScanning()
            }
        } catch (_: Exception) {
            // error block
        }
    }

    private fun startScanning() {
        qrScanLauncher.launch(
            Intent(this, QRScannerActivity::class.java).apply {
                putExtra(QRScanResult.REQUEST_FROM, UIConstants.SCREENING_UNIQUE_ID)
            },
        )
    }

    companion object {
        const val ENTRY_POINT_MEMBER_SEARCH = "ENTRY_POINT_MEMBER_SEARCH"
    }
}
