package com.medtroniclabs.opensource.ui.household

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.data.model.ChipViewItemModel
import com.medtroniclabs.opensource.databinding.FragmentFilterBottomSheetDialogBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.TagListCustomView
import com.medtroniclabs.opensource.ui.household.viewmodel.HouseholdListViewModel

class FilterBottomSheetDialogFragment : BottomSheetDialogFragment(), View.OnClickListener {

    private lateinit var binding: FragmentFilterBottomSheetDialogBinding
    private lateinit var villageListTagView: TagListCustomView
    private lateinit var statusListTagView: TagListCustomView
    private val householdListViewModel: HouseholdListViewModel by activityViewModels()

    companion object {
        const val TAG = "FilterBottomSheetDialogFragment"
        fun newInstance(): FilterBottomSheetDialogFragment {
            return FilterBottomSheetDialogFragment()
        }
    }

    override fun getTheme(): Int {
        return R.style.DialogStyle
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        BottomSheetDialog(requireContext(), theme).apply {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFilterBottomSheetDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initializeListeners()
        attachObservers()
    }

    private fun enableConfirm() {
        val isVillageValid =  villageListTagView.getSelectedTags().isNotEmpty()
        val isStatusListValid = statusListTagView.getSelectedTags().isNotEmpty()

        binding.btnApply.isEnabled = isVillageValid || isStatusListValid
    }

    private fun initializeListeners() {
        binding.btnApply.safeClickListener(this)
        binding.btnCancel.safeClickListener(this)
    }

    private fun attachObservers() {
        householdListViewModel.villageListResponse.observe(viewLifecycleOwner) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    resource.data?.let { listItems ->
                        val chipItemList = ArrayList<ChipViewItemModel>()
                        listItems.forEach {
                            chipItemList.add(
                                ChipViewItemModel(
                                    id = it.id,
                                    name = it.name
                                )
                            )
                        }
                        villageListTagView.addChipItemList(
                            chipItemList,
                            householdListViewModel.getFilterLiveData().value?.filterByVillage
                        )
                    }
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }
            }
        }
    }

    private fun initView() {
        if (CommonUtils.isCommunity()) {
            binding.tvVillageTitle.text = getString(R.string.household_location)
        }
        villageListTagView =
            TagListCustomView(binding.root.context, binding.villageChipGroup) { _, _, _ ->
                enableConfirm()
            }
        statusListTagView =
            TagListCustomView(
                binding.root.context,
                binding.registrationStatusChipGroup
            ) { _, _, _ ->
                enableConfirm()
            }

        householdListViewModel.getAllVillagesName()
        composeStatusListChipView()
        binding.etFromDate.safeClickListener(this)
        binding.etToDate.safeClickListener(this)
    }


    private fun composeStatusListChipView() {
        val itemList = arrayListOf(
            HouseholdDefinedParams.Pending,
            HouseholdDefinedParams.Finished
        )
        val statusList = ArrayList<ChipViewItemModel>()
        itemList.forEach {
            statusList.add(
                ChipViewItemModel(name = it)
            )
        }
        statusListTagView.addChipItemList(
            statusList,
            householdListViewModel.getFilterLiveData().value?.filterByStatus
        )
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.btnApply -> {
                applyFilter()
            }

            R.id.btnCancel -> {
                householdListViewModel.setFilterLiveData(
                    villageFilter = listOf(),
                    statusFilter = listOf()
                )
                villageListTagView.clearSelection()
                statusListTagView.clearSelection()
                dismiss()
            }
        }
    }

    private fun applyFilter() {
        householdListViewModel.setFilterLiveData(
            villageFilter = villageListTagView.getSelectedTags(),
            statusFilter = statusListTagView.getSelectedTags()
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