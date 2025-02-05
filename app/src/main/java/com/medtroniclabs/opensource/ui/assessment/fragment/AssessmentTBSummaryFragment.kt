package com.medtroniclabs.opensource.ui.assessment.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.app.analytics.utils.AnalyticsDefinedParams
import com.medtroniclabs.opensource.common.CommonUtils.convertListToString
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.DefinedParams.DefaultID
import com.medtroniclabs.opensource.common.DefinedParams.TB
import com.medtroniclabs.opensource.common.StringConverter
import com.medtroniclabs.opensource.databinding.FragmentAssessmentTBSummaryBinding
import com.medtroniclabs.opensource.formgeneration.config.DefinedParams.DefaultIDLabel
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.formgeneration.utility.CustomSpinnerAdapter
import com.medtroniclabs.opensource.model.AssessmentSummaryModel
import com.medtroniclabs.opensource.ui.assessment.AssessmentCommonUtils
import com.medtroniclabs.opensource.ui.assessment.AssessmentCommonUtils.getListItemValue
import com.medtroniclabs.opensource.ui.assessment.AssessmentCommonUtils.getValueOfKeyFromMap
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.CoughTBSummary
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.DrenchingNightSweats
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.hasCough
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.HasNightSweats
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.PHUSite1
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.PHUSite2
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.PHUSite3
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.ReferredPHUSite
import com.medtroniclabs.opensource.ui.assessment.viewmodel.AssessmentViewModel

class AssessmentTBSummaryFragment : Fragment(), View.OnClickListener {
    private val viewModel: AssessmentViewModel by activityViewModels()
    lateinit var binding: FragmentAssessmentTBSummaryBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAssessmentTBSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        attachObservers()
        loadPhuSitesList()
        viewModel.setUserJourney(AnalyticsDefinedParams.TBSummaryAssessement)
    }


    private fun initView() {
        binding.btnDone.safeClickListener(this)
    }

    override fun onClick(view: View) {
        when(view.id) {
            binding.btnDone.id -> {
                viewModel.updateOtherAssessmentDetails()
            }
        }
    }

    private fun attachObservers() {
        viewModel.assessmentStringLiveData.value?.let {
            createSummaryView(createTBListSummaryData(it))
        }
    }

    private fun createTBListSummaryData(data: String): MutableList<AssessmentSummaryModel>? {
        return viewModel.formLayout?.filter { it.isSummary == true }?.map { formLayout ->
            AssessmentSummaryModel(
                title = formLayout.title,
                id = formLayout.id,
                cultureValue = formLayout.titleCulture,
                value = getValueOfKeyFromMap(
                    StringConverter.stringToMap(data),
                    formLayout.id,
                    TB
                )
            )
        }?.toMutableList()
    }

    private fun createSummaryView(
        listSummaryData: MutableList<AssessmentSummaryModel>?
    ) {
        listSummaryData?.let {summaryData ->
            binding.emptyErrorMessage.visibility = View.GONE
            binding.parentLayout.visibility = View.VISIBLE
            binding.parentLayout.removeAllViews()
            composeTbSummaryView(summaryData)
        } ?: kotlin.run {
            showErrorInSummary()
        }
    }

    private fun composeTbSummaryView(listSummaryData: MutableList<AssessmentSummaryModel>) {
        signsAndSymptoms(listSummaryData)
        bindTbSummaryView(getString(R.string.presumptive_tb_no), getString(R.string.seperator_hyphen))
    }

    private fun bindTbSummaryView(title: String?, value: String?, valueTextColor: Int? = null) {
        binding.parentLayout.addView(
            AssessmentCommonUtils.addViewSummaryLayout(
                title,
                value,
                valueTextColor,
                requireContext()
            )
        )
    }

    private fun signsAndSymptoms(listSummaryData: MutableList<AssessmentSummaryModel>) {
        val signsList = ArrayList<String>()
        getListItemValue(hasCough, listSummaryData)?.let {
            if (it.value?.lowercase() == DefinedParams.Yes.lowercase())
                signsList.add(CoughTBSummary)
        }
        getListItemValue(HasNightSweats, listSummaryData)?.let {
            if (it.value?.lowercase() == DefinedParams.Yes.lowercase())
                signsList.add(DrenchingNightSweats)
        }
        if (signsList.isNotEmpty()) {
            bindTbSummaryView(
                getString(R.string.tb_signs_symptoms),
                convertListToString(signsList)
            )
        }
    }

    private fun loadPhuSitesList() {
        binding.etPhuChange.setSelection(0, true)
        val dropDownList = ArrayList<Map<String, Any>>()
        dropDownList.add(
            hashMapOf<String, Any>(
                DefinedParams.NAME to DefaultIDLabel,
                DefinedParams.ID to DefaultID
            )
        )
        dropDownList.add(
            hashMapOf<String, Any>(
                DefinedParams.NAME to PHUSite1,
                DefinedParams.ID to PHUSite1
            )
        )
        dropDownList.add(
            hashMapOf<String, Any>(
                DefinedParams.NAME to PHUSite2,
                DefinedParams.ID to PHUSite2
            )
        )
        dropDownList.add(
            hashMapOf<String, Any>(
                DefinedParams.NAME to PHUSite3,
                DefinedParams.ID to PHUSite3
            )
        )
        val adapter = CustomSpinnerAdapter(requireContext())
        adapter.setData(dropDownList)
        binding.etPhuChange.adapter = adapter
        binding.etPhuChange.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    view: View?,
                    pos: Int,
                    itemId: Long
                ) {
                    val selectedItem = adapter.getData(position = pos)
                    selectedItem?.let {
                        val selectedId = it[DefinedParams.id] as String?
                        if (selectedId != DefaultID) {
                            viewModel.otherAssessmentDetails[ReferredPHUSite] = selectedId.toString()
                        } else {
                            if (viewModel.otherAssessmentDetails.containsKey(ReferredPHUSite))
                                viewModel.otherAssessmentDetails.remove(ReferredPHUSite)
                        }
                    }
                }
                override fun onNothingSelected(p0: AdapterView<*>?) {
                    /**
                     * this method is not used
                     */
                }
            }
    }

    companion object {
        const val TAG = "AssessmentTBSummaryFragment"
        fun newInstance(): AssessmentTBSummaryFragment {
            return AssessmentTBSummaryFragment()
        }
    }

    private fun showErrorInSummary() {
        binding.emptyErrorMessage.visibility = View.VISIBLE
        binding.parentLayout.visibility = View.GONE
    }
}