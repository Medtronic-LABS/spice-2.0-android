package org.medtroniclabs.uhis.ui.membersearch

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams.HOUSEHOLDFILTER
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.data.model.ChipViewItemModel
import org.medtroniclabs.uhis.databinding.FragmentFilterBottomSheetDialogBinding
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.utility.CustomSpinnerAdapter
import org.medtroniclabs.uhis.model.household.HouseHoldFilterUiData
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.TagListCustomView
import org.medtroniclabs.uhis.ui.membersearch.viewmodel.MemberSearchViewModel

/**
 * FO/PO filter sheet: Shasthya Kormi → SS → sub-village.
 */
class FilterBottomSheetDialogFragment : BottomSheetDialogFragment(), View.OnClickListener {
    private lateinit var binding: FragmentFilterBottomSheetDialogBinding
    private lateinit var ssListTagView: TagListCustomView
    private lateinit var subVillageListTagView: TagListCustomView
    private var skSpinnerAdapter: CustomSpinnerAdapter? = null
    private var suppressSkSpinnerSelection: Boolean = false
    private val viewModel: MemberSearchViewModel by activityViewModels()

    private var spinnerDataSet = false

    /** CHCP-only: Union (village) selector and the sub-villages loaded for the selected Union. */
    private val isChcp: Boolean by lazy { CommonUtils.isCHCP() }
    private lateinit var unionListTagView: TagListCustomView
    private var lastChcpSubVillages: List<ChipViewItemModel> = emptyList()

    companion object {
        const val TAG = "MemberSearchFilterBottomSheetDialogFragment"
        private const val SK_SPINNER_PLACEHOLDER_ID = -1L

        fun newInstance(): FilterBottomSheetDialogFragment = FilterBottomSheetDialogFragment()
    }

    override fun getTheme(): Int = R.style.DialogStyle

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        BottomSheetDialog(requireContext(), theme).apply {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentFilterBottomSheetDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initializeListeners()
        attachObservers()
        if (isChcp) {
            viewModel.loadChcpUnions()
        } else {
            viewModel.getFilterUiData()
        }
    }

    private fun hasValidSkSelected(): Boolean {
        val adapter = skSpinnerAdapter ?: return false
        val position = binding.spShasthyaKormi.selectedItemPosition
        if (position < 0) return false
        val id = adapter.getData(position)?.get(DefinedParams.ID) as? Long ?: return false
        return id != SK_SPINNER_PLACEHOLDER_ID
    }

    private fun enableConfirm() {
        if (isChcp) {
            val unionSelected = unionListTagView.getSelectedTags().isNotEmpty()
            val subVillageSelected = subVillageListTagView.getSelectedTags().isNotEmpty()
            binding.btnApply.isEnabled = unionSelected || subVillageSelected
            return
        }
        val isSsValid = ssListTagView.getSelectedTags().isNotEmpty()
        val isSubVillageValid = subVillageListTagView.getSelectedTags().isNotEmpty()
        val skOnlyValid = hasValidSkSelected()

        binding.btnApply.isEnabled = isSsValid || isSubVillageValid || skOnlyValid
    }

    private fun initializeListeners() {
        binding.btnApply.safeClickListener(this)
        binding.btnCancel.safeClickListener(this)
    }

    private fun attachObservers() {
        if (isChcp) {
            attachChcpObservers()
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.filterUiData.collect { resource ->
                        when (resource.state) {
                            ResourceState.LOADING -> showLoading()
                            ResourceState.SUCCESS -> {
                                hideLoading()
                                resource.data?.let { data ->
                                    bindSkSpinner(data)
                                    val hasSsData = data.ssList.isNotEmpty()
                                    if (hasSsData) {
                                        binding.tvSsTitle.visible()
                                        binding.ssChipGroup.visible()
                                        hideVillage()
                                        bindSsChips(data)
                                    } else {
                                        hideSS()
                                        hideVillage()
                                    }
                                    enableConfirm()
                                }
                            }
                            ResourceState.ERROR -> {
                                hideLoading()
                                enableConfirm()
                            }
                        }
                    }
                }
                launch {
                    viewModel.subVillages.collect { villages ->
                        binding.subVillageChipGroup.clearCheck()
                        if (villages.isEmpty()) {
                            hideVillage()
                        } else {
                            binding.tvSubVillage.post {
                                binding.tvSubVillage.visible()
                                binding.subVillageChipGroup.visible()
                            }
                        }
                        subVillageListTagView.addChipItemList(
                            villages,
                            viewModel.searchParams.value.filterBySubVillages,
                        )
                    }
                }
            }
        }
    }

    /**
     * CHCP filter observers: Union (village) list and the sub-villages for the selected Union.
     */
    private fun attachChcpObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.unions.collect { unions ->
                        unionListTagView.addChipItemList(unions)
                        enableConfirm()
                    }
                }
                launch {
                    viewModel.subVillages.collect { villages ->
                        lastChcpSubVillages = villages
                        binding.subVillageChipGroup.clearCheck()
                        if (villages.isEmpty()) {
                            hideVillage()
                        } else {
                            binding.tvSubVillage.post {
                                binding.tvSubVillage.visible()
                                binding.subVillageChipGroup.visible()
                            }
                        }
                        subVillageListTagView.addChipItemList(
                            villages,
                            viewModel.searchParams.value.filterBySubVillages,
                        )
                        enableConfirm()
                    }
                }
            }
        }
    }

    private fun initView() {
        viewModel.setUserJourney(HOUSEHOLDFILTER)

        binding.tvRegistrationStatus.gone()
        binding.registrationStatusChipGroup.gone()
        hideVillage()

        if (isChcp) {
            initChcpLocationViews()
        } else {
            initFoPoLocationViews()
        }

        binding.etFromDate.safeClickListener(this)
        binding.etToDate.safeClickListener(this)
    }

    /**
     * CHCP filter: Union (village) → Village (sub-village). No Shasthya Kormi/Shebika.
     */
    private fun initChcpLocationViews() {
        binding.tvVillageTitle.visible()
        binding.tvVillageTitle.setText(R.string.union_village)
        binding.spShasthyaKormi.gone()
        binding.villageChipGroup.visible()
        hideSS()
        binding.tvSubVillage.setText(R.string.village)

        unionListTagView =
            TagListCustomView(binding.root.context, binding.villageChipGroup, true) { _, _, _ ->
                val selectedUnion = unionListTagView.getSelectedTags().firstOrNull()
                val unionId = selectedUnion?.id
                if (unionId == null) {
                    viewModel.clearChcpSubVillages()
                    hideVillage()
                } else {
                    viewModel.onUnionSelected(unionId)
                }
                enableConfirm()
            }
        subVillageListTagView =
            TagListCustomView(binding.root.context, binding.subVillageChipGroup) { _, _, _ ->
                enableConfirm()
            }
    }

    /**
     * FO/PO filter: Shasthya Kormi → SS → sub-village.
     */
    private fun initFoPoLocationViews() {
        binding.tvVillageTitle.visible()
        binding.tvVillageTitle.setText(R.string.shasthya_kormi_sk)
        binding.villageChipGroup.gone()
        binding.spShasthyaKormi.visible()
        binding.tvSsTitle.gone()
        binding.ssChipGroup.gone()

        skSpinnerAdapter = CustomSpinnerAdapter(requireContext())
        binding.spShasthyaKormi.adapter = skSpinnerAdapter
        binding.spShasthyaKormi.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    itemView: View?,
                    position: Int,
                    id: Long,
                ) {
                    if (suppressSkSpinnerSelection) return
                    val adapter = skSpinnerAdapter ?: run {
                        enableConfirm()
                        return
                    }
                    val kormiId = adapter.getData(position)?.get(DefinedParams.ID) as? Long
                    if (kormiId == null || kormiId == SK_SPINNER_PLACEHOLDER_ID) {
                        hideSS()
                        hideVillage()
                        enableConfirm()
                        return
                    }
                    viewModel.loadSsListForSelectedKormi(kormiId)
                    enableConfirm()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }

        ssListTagView = TagListCustomView(binding.root.context, binding.ssChipGroup, true) { _, _, _ ->
            if (ssListTagView.getSelectedTags().isEmpty()) {
                hideVillage()
            } else {
                viewModel.onSsSelected(ssListTagView.getSelectedTags())
            }
            enableConfirm()
        }
        subVillageListTagView = TagListCustomView(binding.root.context, binding.subVillageChipGroup) { _, _, _ ->
            enableConfirm()
        }
    }

    private fun hideVillage() {
        binding.tvSubVillage.gone()
        binding.subVillageChipGroup.gone()
        binding.subVillageChipGroup.clearCheck()
    }

    private fun hideSS() {
        binding.tvSsTitle.gone()
        binding.ssChipGroup.gone()
        binding.ssChipGroup.clearCheck()
    }

    private fun bindSkSpinner(data: HouseHoldFilterUiData) {
        val adapter = skSpinnerAdapter ?: return
        if (spinnerDataSet) return
        spinnerDataSet = true
        val placeholder =
            linkedMapOf<String, Any>(
                DefinedParams.ID to SK_SPINNER_PLACEHOLDER_ID,
                DefinedParams.NAME to getString(R.string.please_select),
                DefinedParams.CULTURE_VALUE to getString(R.string.please_select),
            )
        val items = arrayListOf<Map<String, Any>>(placeholder)
        data.skList.forEach { k ->
            val label =
                listOf(k.firstName, k.lastName)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                    .trim()
                    .ifEmpty { k.username.orEmpty() }
                    .ifEmpty { k.phoneNumber.orEmpty() }
                    .ifEmpty { k.id.toString() }
            items.add(
                linkedMapOf(
                    DefinedParams.ID to k.id,
                    DefinedParams.NAME to label,
                    DefinedParams.CULTURE_VALUE to label,
                ),
            )
        }
        suppressSkSpinnerSelection = true
        adapter.setData(items)
        binding.spShasthyaKormi.adapter = adapter
        val selectedIdx =
            viewModel.searchParams.value.filterSk.let { kid ->
                if (kid == -1L) -1 else adapter.getIndexOfItem(kid)
            }
        val position = if (selectedIdx >= 0) selectedIdx else 0
        binding.spShasthyaKormi.setSelection(position, false)
        binding.spShasthyaKormi.post {
            suppressSkSpinnerSelection = false
        }
    }

    private fun bindSsChips(data: HouseHoldFilterUiData) {
        val ssList =
            data.ssList.map {
                val name =
                    if (it.ssId.isNullOrBlank()) {
                        it.name
                    } else {
                        "${it.ssId} - ${it.name}"
                    }
                ChipViewItemModel(
                    id = it.id,
                    name = name,
                )
            }
        ssListTagView.addChipItemList(
            ssList,
            viewModel.searchParams.value.filterBySs,
        )
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.btnApply -> applyFilter()
            R.id.btnCancel -> {
                viewModel.setUserJourney(AnalyticsDefinedParams.HOUSEHOLDFILTERCANCELTRIGGERED)
                if (isChcp) {
                    clearChcpFilter()
                } else {
                    viewModel.updateFilter(
                        ssFilter = listOf(),
                        subVillagesFilter = listOf(),
                        filterSk = -1,
                    )
                    ssListTagView.clearSelection()
                    subVillageListTagView.clearSelection()
                    hideVillage()
                    suppressSkSpinnerSelection = true
                    binding.spShasthyaKormi.setSelection(0, false)
                    binding.spShasthyaKormi.post {
                        suppressSkSpinnerSelection = false
                    }
                }
                dismiss()
            }
        }
    }

    private fun clearChcpFilter() {
        viewModel.updateFilter(
            ssFilter = listOf(),
            subVillagesFilter = listOf(),
            filterSk = -1,
        )
        unionListTagView.clearSelection()
        subVillageListTagView.clearSelection()
        viewModel.clearChcpSubVillages()
        hideVillage()
    }

    private fun applyFilter() {
        viewModel.setUserJourney(AnalyticsDefinedParams.HOUSEHOLDFILTERAPPLYTRIGGERED)
        if (isChcp) {
            applyChcpFilter()
            return
        }
        val ssSelection = ssListTagView.getSelectedTags()
        val subSelection = subVillageListTagView.getSelectedTags()
        val filterSk = if (hasValidSkSelected()) {
            val position = binding.spShasthyaKormi.selectedItemPosition
            skSpinnerAdapter?.getData(position)?.get(DefinedParams.ID) as? Long
        } else {
            null
        }
        viewModel.updateFilter(
            ssFilter = ssSelection,
            subVillagesFilter = subSelection,
            filterSk = filterSk,
        )
        dismiss()
    }

    /**
     * CHCP: filter by the chosen sub-villages, or by every sub-village in the selected Union
     * when no specific sub-village is picked.
     */
    private fun applyChcpFilter() {
        val subSelection = subVillageListTagView.getSelectedTags()
        val effectiveSubVillages = subSelection.ifEmpty { lastChcpSubVillages }
        viewModel.updateFilter(
            ssFilter = listOf(),
            subVillagesFilter = effectiveSubVillages,
            filterSk = -1,
        )
        dismiss()
    }

    private fun showLoading() {
        binding.loadingProgress.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        binding.loadingProgress.visibility = View.GONE
    }
}
