package org.medtroniclabs.uhis.ui.medicalreview.tb.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.data.model.ChipViewItemModel
import org.medtroniclabs.uhis.databinding.FragmentSystemicExaminationsBinding
import org.medtroniclabs.uhis.formgeneration.extension.markMandatory
import org.medtroniclabs.uhis.ncd.medicalreview.NCDMRUtil
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.TagListCustomView
import org.medtroniclabs.uhis.ui.medicalreview.motherneonate.anc.MotherNeonateUtil
import org.medtroniclabs.uhis.ui.medicalreview.tb.viewmodel.ComorbiditiesViewModel
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewTypeEnums

@AndroidEntryPoint
class ComorbiditiesFragment : BaseFragment() {
    private val viewModel: ComorbiditiesViewModel by activityViewModels()
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
        const val TAG = "ComorbiditiesFragment"

        fun newInstance(type: String) =
            ComorbiditiesFragment().apply {
                arguments = Bundle().apply {
                    putString(DefinedParams.type, type)
                }
            }
    }

    fun getType(): String = arguments?.getString(DefinedParams.type, "") ?: ""

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.getChips(getType())
        setObserver()
        attachObserver()
    }

    private fun attachObserver() {
        viewModel.getChipItems.observe(viewLifecycleOwner) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.SUCCESS -> {
                    resource.data?.let { listItems ->
                        val chipItemList =
                            listItems
                                .filter { it.category == MedicalReviewTypeEnums.comorbidities.name }
                                .map {
                                    ChipViewItemModel(
                                        id = it.id,
                                        name = it.name,
                                        value = it.value,
                                    )
                                } as ArrayList<ChipViewItemModel>
                        initView(chipItemList)
                    }
                    hideProgress()
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }
            }
        }
    }

    private fun setObserver() {
        MotherNeonateUtil.initTextWatcherForString(binding.etPhysicalExaminationComments) {
            viewModel.comments = it
            setFragmentResult(
                MedicalReviewDefinedParams.CF_ITEM,
                bundleOf(
                    MedicalReviewDefinedParams.CHIP_ITEMS to true,
                ),
            )
        }
    }

    private fun initView(complaintList: ArrayList<ChipViewItemModel>) {
        with(binding) {
            binding.tvCommentsTitle.text = getString(R.string.please_specify_the_comorbidity)
            binding.tvCommentsTitle.markMandatory()
            binding.tvCommentsTitle.gone()
            binding.etPhysicalExaminationComments.gone()
            binding.tvCommentsTitle.gone()
            tvSystemicExaminationTitle.text =
                if (getType().equals(
                        MedicalReviewTypeEnums.HIV.name,
                        true,
                    )
                ) {
                    getString(R.string.comorbidities_coinfections)
                } else {
                    getString(R.string.comorbidities)
                }
            tagView =
                TagListCustomView(
                    root.context,
                    tagPhysicalExamination,
                    callBack = { _, _, _ ->
                        viewModel.chips.clear()
                        viewModel.chips =
                            ArrayList(tagView.getSelectedTags())
                        showNotes()
                        setFragmentResult(
                            MedicalReviewDefinedParams.CF_ITEM,
                            bundleOf(
                                MedicalReviewDefinedParams.CHIP_ITEMS to true,
                            ),
                        )
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
            binding.tvCommentsTitle.gone()
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
        )
        return Pair(isValid, binding.etPhysicalExaminationComments)
    }
}
