package org.medtroniclabs.uhis.ui.household

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams.HOUSEHOLDFILTER
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.databinding.FragmentFilterBottomSheetDialogBinding
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.ui.TagListCustomView
import org.medtroniclabs.uhis.ui.household.viewmodel.HouseholdListViewModel

class FilterBottomSheetDialogFragment : BottomSheetDialogFragment(), View.OnClickListener {
    private lateinit var binding: FragmentFilterBottomSheetDialogBinding
    private lateinit var ssListTagView: TagListCustomView
    private lateinit var subVillageListTagView: TagListCustomView
    private val householdListViewModel: HouseholdListViewModel by activityViewModels()

    companion object {
        const val TAG = "FilterBottomSheetDialogFragment"

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
        householdListViewModel.getShashtyaShebikas()
    }

    private fun enableConfirm() {
        val isSsValid = ssListTagView.getSelectedTags().isNotEmpty()
        val isSubVillageValid = subVillageListTagView.getSelectedTags().isNotEmpty()

        binding.btnApply.isEnabled = isSsValid || isSubVillageValid
    }

    private fun initializeListeners() {
        binding.btnApply.safeClickListener(this)
        binding.btnCancel.safeClickListener(this)
    }

    private fun attachObservers() {
        householdListViewModel.shashthyaShebikasLiveData.observe(viewLifecycleOwner) {
            ssListTagView.addChipItemList(
                it,
                householdListViewModel.getFilterLiveData().value?.filterBySs,
            )
        }
        householdListViewModel.subVillagesLiveData.observe(viewLifecycleOwner) {
            binding.subVillageChipGroup.clearCheck()
            if (it.isEmpty()) {
                hideVillage()
            } else {
                binding.tvSubVillage.visible()
                binding.subVillageChipGroup.visible()
            }
            subVillageListTagView.addChipItemList(
                it,
                householdListViewModel.getFilterLiveData().value?.filterBySubVillages,
            )
        }
    }

    private fun initView() {
        householdListViewModel.setUserJourney(HOUSEHOLDFILTER)
        // Hide Household location
        binding.tvVillageTitle.gone()
        binding.villageChipGroup.gone()
        // Show SS
        binding.tvSsTitle.visible()
        binding.ssChipGroup.visible()
        // Hide registration status
        binding.tvRegistrationStatus.gone()
        binding.registrationStatusChipGroup.gone()

        // Hide village(sub-village)
        hideVillage()

        ssListTagView = TagListCustomView(binding.root.context, binding.ssChipGroup, true) { _, _, _ ->
            val selectedTags = ssListTagView.getSelectedTags()
            if (selectedTags.isEmpty()) {
                hideVillage()
            } else {
                householdListViewModel.onShashtyaShebikaSelected(ssListTagView.getSelectedTags())
            }
            enableConfirm()
        }
        subVillageListTagView = TagListCustomView(binding.root.context, binding.subVillageChipGroup) { _, _, _ ->
            enableConfirm()
        }

        binding.etFromDate.safeClickListener(this)
        binding.etToDate.safeClickListener(this)
    }

    private fun hideVillage() {
        binding.tvSubVillage.gone()
        binding.subVillageChipGroup.gone()
        binding.subVillageChipGroup.clearCheck()
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.btnApply -> {
                applyFilter()
            }

            R.id.btnCancel -> {
                householdListViewModel.setUserJourney(AnalyticsDefinedParams.HOUSEHOLDFILTERCANCELTRIGGERED)
                householdListViewModel.setFilterLiveData(
                    ssFilter = listOf(),
                    subVillagesFilter = listOf(),
                )
                ssListTagView.clearSelection()
                subVillageListTagView.clearSelection()
                dismiss()
            }
        }
    }

    private fun applyFilter() {
        householdListViewModel.setUserJourney(AnalyticsDefinedParams.HOUSEHOLDFILTERAPPLYTRIGGERED)
        householdListViewModel.setFilterLiveData(
            ssFilter = ssListTagView.getSelectedTags(),
            subVillagesFilter = subVillageListTagView.getSelectedTags(),
        )
        dismiss()
    }
}
