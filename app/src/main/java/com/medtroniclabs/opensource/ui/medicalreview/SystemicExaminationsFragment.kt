package com.medtroniclabs.opensource.ui.medicalreview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.data.model.ChipViewItemModel
import com.medtroniclabs.opensource.databinding.FragmentSystemicExaminationsBinding
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseFragment
import com.medtroniclabs.opensource.ui.TagListCustomView
import com.medtroniclabs.opensource.ui.medicalreview.abovefiveyears.SystemicExaminationViewModel
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.MotherNeonateUtil.isDataValid
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewDefinedParams
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewTypeEnums
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SystemicExaminationsFragment : BaseFragment() {

    private lateinit var binding: FragmentSystemicExaminationsBinding
    private lateinit var examinationsTagView: TagListCustomView
    private val viewModel: SystemicExaminationViewModel by activityViewModels()

    companion object {
        const val TAG = "SystemicExaminationsFragment"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            viewModel.systemicExaminationsType =
                it.getString(MedicalReviewTypeEnums.SystemicExaminations.name) ?: ""
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSystemicExaminationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews()
        attachObserver()
        setListener()
    }

    private fun setListener() {
        binding.etPhysicalExaminationComments.addTextChangedListener {
            it?.let {
                viewModel.enteredExaminationNotes = it.trim().toString()
                setFragmentResult(
                    MedicalReviewDefinedParams.SE_ITEM, bundleOf(
                        MedicalReviewDefinedParams.CHIP_ITEMS to true
                    )
                )
            }
        }
        binding.etFundalHeight.addTextChangedListener {
            it?.let {
                val value = it.trim().toString()
                viewModel.fundalHeight = if (value.isNotBlank()) value.toDoubleOrNull() else null
                setFragmentResult(
                    MedicalReviewDefinedParams.SE_ITEM, bundleOf(
                        MedicalReviewDefinedParams.CHIP_ITEMS to true
                    )
                )
            }
        }

        binding.etFetalHeartRate.addTextChangedListener {
            it?.let {
                val value = it.trim().toString()
                viewModel.fetalHeartRate = if (value.isNotBlank()) value.toDoubleOrNull() else null
                setFragmentResult(
                    MedicalReviewDefinedParams.SE_ITEM, bundleOf(
                        MedicalReviewDefinedParams.CHIP_ITEMS to true
                    )
                )
            }
        }
    }

    private fun attachObserver() {
        viewModel.systemicExaminationList.observe(viewLifecycleOwner) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.SUCCESS -> {
                    resource.data?.let { listItems ->
                        val chipItemList = ArrayList<ChipViewItemModel>()
                        val category =
                            if (viewModel.systemicExaminationsType == MedicalReviewTypeEnums.ANC_REVIEW.name) MedicalReviewTypeEnums.ObstetricExaminations.name else MedicalReviewTypeEnums.SystemicExaminations.name
                        listItems.filter { it.category == category }.forEach {
                            chipItemList.add(
                                ChipViewItemModel(
                                    id = it.id,
                                    name = it.name,
                                    value = it.value
                                )
                            )
                        }
                        examinationsTagView.addChipItemList(
                            chipItemList,
                            viewModel.selectedSystemicExaminations
                        )
                    }
                    hideProgress()
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }
            }
        }
    }

    private fun initializeViews() {
        val (titleResId, showObstetricGroup) = when (viewModel.systemicExaminationsType) {
            MedicalReviewTypeEnums.PNC_MOTHER_REVIEW.name -> {Pair(R.string.systemic_examinations, true)}
            MedicalReviewTypeEnums.ANC_REVIEW.name -> Pair(R.string.obstetric_examination, true)
            MedicalReviewTypeEnums.UNDER_FIVE_YEARS.name -> Pair(
                R.string.systemic_examinations,
                false
            )

            MedicalReviewTypeEnums.ABOVE_FIVE_YEARS.name -> Pair(
                R.string.systemic_examinations,
                false
            )

            else -> return // Handle other cases or provide a default behavior
        }

        with(binding) {
            tvSystemicExaminationTitle.text = getString(titleResId)
            obstetricGroup.visibility = if (showObstetricGroup) View.VISIBLE else View.GONE
            tvFundalHeightError.gone()
            tvFetalHeartRateError.gone()
        }
        initTag()
        if (viewModel.enteredExaminationNotes.isNotBlank()) {
            binding.etPhysicalExaminationComments.setText(viewModel.enteredExaminationNotes)
        }
    }

    private fun initTag() {
        examinationsTagView =
            TagListCustomView(binding.root.context, binding.tagPhysicalExamination) { _, _, _ ->
                viewModel.selectedSystemicExaminations =
                    ArrayList(examinationsTagView.getSelectedTags())
                setFragmentResult(
                    MedicalReviewDefinedParams.SE_ITEM, bundleOf(
                        MedicalReviewDefinedParams.CHIP_ITEMS to true
                    )
                )
            }
        viewModel.getSystemicExaminationList(viewModel.systemicExaminationsType)
    }

    fun validateInput(): Boolean {
        val isFundalHeightValid =
            isDataValid(
                viewModel.fundalHeight,
                binding.tvFundalHeightError,
                50,
                binding.etFundalHeight,
                requireContext()
            )
        val isFetalHeartRateValid =
            isDataValid(
                viewModel.fetalHeartRate,
                binding.tvFetalHeartRateError,
                200,
                binding.etFetalHeartRate,
                requireContext()
            )
        return isFundalHeightValid && isFetalHeartRateValid
    }
    fun refreshFragment() {
        examinationsTagView.clearSelection()
        examinationsTagView.clearOtherChip()
        binding.etPhysicalExaminationComments.text?.clear()
    }

}