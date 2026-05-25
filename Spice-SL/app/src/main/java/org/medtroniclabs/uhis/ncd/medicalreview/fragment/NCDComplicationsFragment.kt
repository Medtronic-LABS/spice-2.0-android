package org.medtroniclabs.uhis.ncd.medicalreview.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatEditText
import androidx.fragment.app.activityViewModels
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.data.model.ChipViewItemModel
import org.medtroniclabs.uhis.databinding.FragmentSystemicExaminationsBinding
import org.medtroniclabs.uhis.formgeneration.extension.markMandatory
import org.medtroniclabs.uhis.ncd.medicalreview.NCDMRUtil
import org.medtroniclabs.uhis.ncd.medicalreview.viewmodel.NCDComplicationsViewModel
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.TagListCustomView
import org.medtroniclabs.uhis.ui.medicalreview.motherneonate.anc.MotherNeonateUtil

class NCDComplicationsFragment : BaseFragment() {
    private val viewModel: NCDComplicationsViewModel by activityViewModels()
    private lateinit var binding: FragmentSystemicExaminationsBinding
    private lateinit var tagView: TagListCustomView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentSystemicExaminationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        const val TAG = "NCDComplicationsFragment"

        fun newInstance(): NCDComplicationsFragment = NCDComplicationsFragment()
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
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
                    value = item.value,
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

    private fun initView(complaintList: ArrayList<ChipViewItemModel>) {
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
                    },
                )
            tagView.addChipItemList(complaintList, viewModel.chips)
        }
    }

    private fun showNotes() {
        if (viewModel.chips.firstOrNull {
                it.name.equals(
                    DefinedParams.OTHER,
                    ignoreCase = true,
                )
            } != null
        ) {
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
            binding.tvErrorMessage,
        )
        return Pair(isValid, binding.etPhysicalExaminationComments)
    }
}
