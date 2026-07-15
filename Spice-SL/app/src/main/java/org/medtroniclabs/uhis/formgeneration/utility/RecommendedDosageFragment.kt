package org.medtroniclabs.uhis.formgeneration.utility

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils.convertStringDobToMonths
import org.medtroniclabs.uhis.databinding.FragmentRecommendedDosageBinding
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.ACT
import org.medtroniclabs.uhis.ui.assessment.viewmodel.AssessmentViewModel

class RecommendedDosageFragment : DialogFragment(), View.OnClickListener {
    lateinit var binding: FragmentRecommendedDosageBinding
    private val viewModel: AssessmentViewModel by activityViewModels()

    companion object {
        const val TAG = "RecommendedDosageFragment"

        fun newInstance(
            id: String,
            title: String,
        ): RecommendedDosageFragment {
            val fragment = RecommendedDosageFragment()
            fragment.arguments = Bundle().apply {
                putString(DefinedParams.ID, id)
                putString(DefinedParams.TITLE, title)
            }
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentRecommendedDosageBinding.inflate(inflater, container, false)
        val window: Window? = dialog?.window
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = false
        initializeViews()
        setListeners()
    }

    private fun initializeViews() {
        binding.tvHeaderMessage.gone()
        binding.tvHeader2Message?.gone()
        val ageInMonths =
            viewModel.memberDetailsLiveData.value
                ?.data
                ?.let { convertStringDobToMonths(it.dateOfBirth) }

        val mainTable = viewModel.dosageListModel?.filter { it.tableId == 1 }
        val secondaryTable = viewModel.dosageListModel?.filter { it.tableId == 2 }

        mainTable?.let { tableListItem ->
            if (tableListItem.isNotEmpty()) {
                tableListItem[0].let { dosageItem ->
                    ageInMonths?.let { age ->
                        val dosageTableList =
                            dosageItem.dosageFrequency?.filter { (age >= it.minMonth) && (age <= it.maxMonth) }
                        binding.tvTitle.text = dosageItem.title ?: requireContext().getString(
                            R.string.instructions,
                        )
                        dosageTableList?.let { dosageTableItem ->
                            if (dosageTableItem.isNotEmpty()) {
                                binding.tableRecyclerView.apply {
                                    binding.tableCol1.text = dosageItem.columnName1
                                    binding.tableCol2.text = dosageItem.columnName2
                                    binding.tableCol3.text = dosageItem.columnName3
                                    binding.tvHeader.text = dosageTableItem[0].monthLabel
                                    layoutManager = LinearLayoutManager(requireContext())
                                    dosageTableItem[0].routine?.let {
                                        if (it[0].night == null) {
                                            binding.tableCol3.gone()
                                        }
                                        adapter = TableRowAdapter(
                                            requireContext(),
                                            it,
                                        )
                                    } ?: kotlin.run {
                                        binding.tableRecyclerView.gone()
                                        binding.tableCol1.gone()
                                        binding.tableCol2.gone()
                                        binding.tableCol3.gone()
                                    }
                                }

                                dosageTableItem[0].warning?.let {
                                    binding.tvHeaderMessage.visible()
                                    binding.tvHeaderMessage.text = it
                                }
                            } else {
                                if (arguments?.getString(DefinedParams.ID) != ACT.lowercase()) {
                                    binding.tableGroup1.gone()
                                    //  binding.errorGroup.visible()
                                }
                            }

                            dosageItem.descriptionTitle?.let { text ->
                                binding.tvContentHeader.visible()
                                binding.tvContentHeader.text = text
                            } ?: kotlin.run {
                                binding.tvContentHeader.gone()
                            }

                            dosageItem.descriptionList?.let { list ->
                                binding.rvInfoList.apply {
                                    layoutManager = LinearLayoutManager(requireContext())
                                    adapter = DosageInstructionAdapter(list)
                                }
                            } ?: kotlin.run {
                                binding.rvInfoList.visibility = View.GONE
                            }
                        }
                    }
                }
            }
        }

        secondaryTable?.let { tableListItem ->
            if (tableListItem.isNotEmpty()) {
                tableListItem[0].let { dosageItem ->
                    ageInMonths?.let { age ->
                        val dosageTableList =
                            dosageItem.dosageFrequency?.filter { (age >= it.minMonth) && (age <= it.maxMonth) }
                        dosageTableList?.let { dosageTableItem ->
                            if (dosageTableItem.isNotEmpty()) {
                                binding.tableGroup2.visible()
                                binding.table2Col1.text = dosageItem.columnName1
                                binding.table2Col2.text = dosageItem.columnName2
                                binding.table2Col3.text = dosageItem.columnName3
                                binding.tvHeader2.text = dosageTableItem[0].monthLabel
                                binding.tableRecyclerView2.apply {
                                    layoutManager = LinearLayoutManager(requireContext())
                                    dosageTableItem[0].routine?.let {
                                        if (it[0].night == null) {
                                            binding.table2Col3.gone()
                                        }
                                        adapter = TableRowAdapter(
                                            requireContext(),
                                            it,
                                        )
                                    } ?: kotlin.run {
                                        binding.tableRecyclerView2.gone()
                                        binding.table2Col1.gone()
                                        binding.table2Col2.gone()
                                        binding.table2Col3.gone()
                                    }
                                }

                                dosageTableItem[0].warning?.let {
                                    binding.tvHeader2Message?.visible()
                                    binding.tvHeader2Message?.text = it
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setListeners() {
        binding.ivClose.safeClickListener(this)
        binding.btnClose.safeClickListener(this)
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.ivClose.id, binding.btnClose.id -> {
                dismiss()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
        )
    }
}
