package com.medtroniclabs.opensource.ncd.medicalreview.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatEditText
import androidx.fragment.app.activityViewModels
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.data.model.ChipViewItemModel
import com.medtroniclabs.opensource.databinding.FragmentSystemicExaminationsBinding
import com.medtroniclabs.opensource.formgeneration.extension.markMandatory
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil
import com.medtroniclabs.opensource.ncd.medicalreview.viewmodel.NCDComplicationsViewModel
import com.medtroniclabs.opensource.ui.BaseFragment
import com.medtroniclabs.opensource.ui.TagListCustomView
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.MotherNeonateUtil

class NCDComplicationsFragment : BaseFragment() {
    private val viewModel: NCDComplicationsViewModel by activityViewModels()
    private lateinit var binding: FragmentSystemicExaminationsBinding
    private lateinit var tagView: TagListCustomView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSystemicExaminationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        const val TAG = "NCDComplicationsFragment"
        fun newInstance(): NCDComplicationsFragment {
            return NCDComplicationsFragment()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.getChips()
        setObserver()
        attachObserver()
    }

    private fun attachObserver() {
        viewModel.getChipItems.observe(viewLifecycleOwner) {
            val complaintList = it.map { item ->
                ChipViewItemModel(
                    id = item.id,
                    name = item.name,
                    cultureValue = item.displayValue,
                    type = item.type,
                    value = item.value
                )
            } as ArrayList<ChipViewItemModel>
            initView(complaintList)
        }
    }

    private fun setObserver() {
        MotherNeonateUtil.initTextWatcherForString(binding.etPhysicalExaminationComments) {
            viewModel.comments = it
        }
    }


    private fun initView(
        complaintList: ArrayList<ChipViewItemModel>
    ) {
        with(binding) {
            binding.tvSystemicExaminationTitle.text = getString(R.string.complications)
            binding.tvCommentsTitle.text = getString(R.string.please_specify_the_complication)
            binding.tvCommentsTitle.markMandatory()
            binding.etPhysicalExaminationComments.gone()
            binding.tvCommentsTitle.gone()
            tagView =
                TagListCustomView(
                    root.context,
                    tagPhysicalExamination,
                    callBack = { _, _, _ ->
                        viewModel.chips.clear()
                        viewModel.chips =
                            ArrayList(tagView.getSelectedTags())
                        showNotes()
                    }
                )
            tagView.addChipItemList(complaintList, viewModel.chips)
        }
    }

    private fun showNotes() {
        if (viewModel.chips.firstOrNull {
                it.name.equals(
                    DefinedParams.Other,
                    ignoreCase = true
                )
            } != null) {
            binding.etPhysicalExaminationComments.visible()
            binding.tvCommentsTitle.visible()
        } else {
            binding.etPhysicalExaminationComments.gone()
            binding.tvCommentsTitle.gone()
            viewModel.comments = ""
            binding.etPhysicalExaminationComments.setText(getText(R.string.empty))
            binding.tvErrorMessage.gone()
        }
    }

    fun validateInput(isMandatory: Boolean = false): Pair<Boolean, AppCompatEditText> {
        val isValid = NCDMRUtil.validateInput(
            isMandatory,
            viewModel.chips,
            binding.etPhysicalExaminationComments,
            binding.tvErrorMessage
        )
        return Pair(isValid, binding.etPhysicalExaminationComments)
    }
}