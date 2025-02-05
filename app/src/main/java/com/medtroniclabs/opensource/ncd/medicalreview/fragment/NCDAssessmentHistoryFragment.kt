package com.medtroniclabs.opensource.ncd.medicalreview.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.google.gson.Gson
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.setExpandableText
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.CommonUtils.getGlucoseUnit
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.databinding.FragmentNcdAssessmentHistoryBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.formgeneration.utility.CustomSpinnerAdapter
import com.medtroniclabs.opensource.ncd.data.BPBGListModel
import com.medtroniclabs.opensource.ncd.data.BPLogList
import com.medtroniclabs.opensource.ncd.data.GlucoseLogList
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil.BP_TAG
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil.FBS
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil.RBS
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil.RBS_FBS
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMedicalReviewCMRActivity
import com.medtroniclabs.opensource.ncd.medicalreview.dialog.NCDBloodGlucoseReadingDialog
import com.medtroniclabs.opensource.ncd.medicalreview.dialog.NCDBloodPressureVitalsDialog
import com.medtroniclabs.opensource.ncd.medicalreview.viewmodel.NCDBpAndBgViewModel
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.BaseFragment
import com.medtroniclabs.opensource.ui.dialog.GeneralSuccessDialog
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.PatientDetailViewModel

class NCDAssessmentHistoryFragment : BaseFragment(), View.OnClickListener {

    private lateinit var binding: FragmentNcdAssessmentHistoryBinding
    private var isBPSummary: Boolean = false
    private val viewModel: NCDBpAndBgViewModel by activityViewModels()
    private val patientDetailViewModel: PatientDetailViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentNcdAssessmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        const val TAG = "NCDAssessmentHistoryFragment"
        fun newInstance() =
            NCDAssessmentHistoryFragment()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isBPSummary = arguments?.getString(NCDMRUtil.TAG) == BP_TAG
        initializeViews()
        attachObservers()
        getValues()
    }

    private fun initializeViews() {
        setupUI(isBPSummary)
    }

    private fun setupUI(isBPSummary: Boolean) {
        // Set titles and labels based on BP summary or Blood Glucose
        binding.apply {
            cardTitle.text =
                getString(if (isBPSummary) R.string.blood_pressure else R.string.blood_glucose)
            llValue.tvKey.text =
                getString(if (isBPSummary) R.string.bp_value else R.string.blood_glucose_value)
            llValue.tvRowSeparator.text = getString(R.string.separator_colon)
            llValue.tvValue.text = getString(R.string.separator_hyphen)

            // Common labels for Last Assessment and Symptoms
            llLastAssessment.apply {
                tvKey.text = getString(R.string.last_assessment_date)
                tvRowSeparator.text = getString(R.string.separator_colon)
                tvValue.text = getString(R.string.separator_hyphen)
            }

            llSymptoms.apply {
                tvKey.text = getString(R.string.symptoms)
                tvRowSeparator.text = getString(R.string.separator_colon)
                tvValue.text = getString(R.string.separator_hyphen)
            }

            // Set up graph navigation buttons
            ivGraphNext.apply {
                tag = isBPSummary
                safeClickListener(this@NCDAssessmentHistoryFragment)
            }

            ivGraphPrevious.apply {
                tag = isBPSummary
                safeClickListener(this@NCDAssessmentHistoryFragment)
            }
            clGraph.apply {
                tag = isBPSummary
            }
            btnAddNewReading.apply {
                tag = isBPSummary
            }
            CenterProgress.apply {
                tag = isBPSummary
            }
            spinnerRbsFbs.visibility = if (isBPSummary) View.INVISIBLE else View.VISIBLE

            if (!isBPSummary) {
                spinnerRbsFbs.apply {
                    adapter = createSpinnerAdapter()
                    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            adapterView: AdapterView<*>?,
                            view: View?,
                            pos: Int,
                            itemId: Long
                        ) {
                            val selectedItem =
                                (adapter as CustomSpinnerAdapter).getData(position = pos)
                            viewModel.selectedBGDropDown.value =
                                selectedItem?.get(DefinedParams.ID) as? Int ?: 3
                        }

                        override fun onNothingSelected(p0: AdapterView<*>?) {
                            // No action needed
                        }
                    }
                }
            }
            // Add new reading button
            btnAddNewReading.safeClickListener(this@NCDAssessmentHistoryFragment)
        }
    }

    private fun createSpinnerAdapter(): CustomSpinnerAdapter {
        val adapter = CustomSpinnerAdapter(requireContext())
        // Explicitly cast HashMap to Map to match the required type
        val dropDownList: ArrayList<Map<String, Any>> = arrayListOf(
            hashMapOf<String, Any>(
                DefinedParams.NAME to RBS_FBS,
                DefinedParams.ID to NCDMRUtil.fbs_rbs_code
            ) as Map<String, Any>,
            hashMapOf<String, Any>(
                DefinedParams.NAME to RBS,
                DefinedParams.ID to NCDMRUtil.rbs_code
            ) as Map<String, Any>,
            hashMapOf<String, Any>(
                DefinedParams.NAME to FBS,
                DefinedParams.ID to NCDMRUtil.fbs_code
            ) as Map<String, Any>
        )
        adapter.setData(dropDownList)
        return adapter
    }


    private fun attachObservers() {
        viewModel.selectedBGDropDown.observe(viewLifecycleOwner) { num ->
            if (!isBPSummary) {
                viewModel.glucoseLogListResponseLiveData.value?.data?.let { response ->
                    response.glucoseLogList?.let { logList ->
                        populateBGDetails(logList, num, response)
                    }
                }
            }
        }
        viewModel.bpLogListResponseLiveData.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showGraphLoader(true)
                }

                ResourceState.ERROR -> {
                    hideGraphLoader(true)
                }

                ResourceState.SUCCESS -> {
                    hideGraphLoader(true)
                    if (isBPSummary)
                        loadBPValues(resourceState.data)
                }
            }
        }

        viewModel.glucoseLogListResponseLiveData.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showGraphLoader(false)
                }

                ResourceState.ERROR -> {
                    hideGraphLoader(false)
                }

                ResourceState.SUCCESS -> {
                    hideGraphLoader(false)
                    if (!isBPSummary)
                        loadBGValues(resourceState.data)
                }
            }
        }
    }

    private fun getValues(forward: Boolean? = null) {
        patientDetailViewModel.getPatientFHIRId()?.let { memberId ->
            val isBP = isBPSummary
            val request = BPBGListModel(
                memberId = memberId,
                latestRequired = true,
                limit = NCDMRUtil.PageLimit,
                sortOrder = -1 //Asc
//                sortField = if (isBP) NCDMRUtil.BPTakenOn else NCDMRUtil.BGTakenOn
            )

            val count = if (isBP) viewModel.totalBPCount else viewModel.totalBGCount
            val updatedCount = when (forward) {
                null -> 0
                true -> count - 1
                false -> count + 1
            }

            if (isBP) {
                viewModel.totalBPCount = updatedCount
                request.skip = updatedCount * NCDMRUtil.PageLimit
                viewModel.bpLogList(request)
            } else {
                viewModel.totalBGCount = updatedCount
                request.skip = updatedCount * NCDMRUtil.PageLimit
                viewModel.glucoseLogList(request, forward)
            }
        }
    }

    private fun loadBPValues(bpLog: BPBGListModel?) {
        bpLog?.let {
            it.total.let {
                viewModel.totalBPTotalCount = it//7
            }
            if (binding.ivGraphNext.tag == isBPSummary && binding.ivGraphPrevious.tag == isBPSummary) {
                binding.ivGraphNext.isEnabled = bpLog.skip != 0
                binding.ivGraphPrevious.isEnabled =
                    viewModel.totalBPTotalCount?.let { count -> if (count != NCDMRUtil.PageLimit) viewModel.totalBPCount < (count / NCDMRUtil.PageLimit) else null }
                        ?: false
            }
            it.latestBpLog?.let {
                viewModel.latestBpLogResponse = it
            }
            renderLatestBPLogDetails(viewModel.latestBpLogResponse)
            renderBPLogsToGraph(it)
        }
    }

    private fun renderBPLogsToGraph(log: BPBGListModel) {
        if (log.bpLogList.isNullOrEmpty()) {
            showNoRecordView()
            return
        }
        hideNoRecordView()
        val bundle = Bundle().apply {
            putString(NCDMRUtil.TAG, BP_TAG)
            putString(
                NCDMRUtil.graphDetails,
                Gson().toJson(log.apply {
                    if (!bpLogList.isNullOrEmpty()) {
                        bpLogList = ArrayList(bpLogList!!.reversed())
                    }
                }
                ))
        }
        binding.tvLineOne.text = getString(R.string.systolic)
        binding.tvLineTwo.text = getString(R.string.diastolic)
        replaceFragmentInId<NCDAssessmentHistoryGraphFragment>(bundle = bundle)
    }

    private inline fun <reified fragment : Fragment> replaceFragmentInId(
        id: Int? = null,
        bundle: Bundle? = null,
        tag: String? = null
    ) {
        childFragmentManager.commit {
            setReorderingAllowed(true)
            replace<fragment>(
                id ?: binding.llGraph.id,
                args = bundle,
                tag = tag
            )
        }
    }

    private fun loadBGValues(data: BPBGListModel?) {
        data?.let { it ->
            if (binding.ivGraphNext.tag == isBPSummary && binding.ivGraphPrevious.tag == isBPSummary) {
                binding.ivGraphNext.isEnabled = it.skip != 0
                binding.ivGraphPrevious.isEnabled =
                    it.total != NCDMRUtil.PageLimit && viewModel.totalBGCount < (it.total / NCDMRUtil.PageLimit)
            }
            data.latestGlucoseLog?.let {
                renderLatestBGLogDetails(it)
            }
            renderBGLogsToGraph(it)
        }
    }

    private fun renderBGLogsToGraph(log: BPBGListModel) {
        log.glucoseLogList?.filterNot { it.glucoseType == null }?.takeIf { it.isNotEmpty() }?.let {
            hideNoRecordView()
            val bundle = Bundle().apply {
                putString(NCDMRUtil.TAG, NCDMRUtil.BG_TAG)
                putString(NCDMRUtil.graphDetails, Gson().toJson(log.apply {
                    if (!glucoseLogList.isNullOrEmpty()) {
                        glucoseLogList = ArrayList(glucoseLogList!!.reversed())
                    }
                }))
            }
            binding.tvLineOne.text = RBS
            binding.tvLineTwo.text = FBS
            replaceFragmentInId<NCDAssessmentHistoryGraphFragment>(bundle = bundle)
        } ?: showNoRecordView()
    }

    private fun showNoRecordView() {
        binding.llGraph.gone()
        binding.tvNoData.visible()
        binding.llSummaryHolder.visibility = View.INVISIBLE
    }

    private fun hideNoRecordView() {
        binding.llGraph.visible()
        binding.tvNoData.gone()
        binding.llSummaryHolder.visible()
    }


    private fun renderLatestBPLogDetails(latestBPLog: BPLogList?) {
        latestBPLog?.let { log ->
            val formattedDate = DateUtils.convertDateTimeToDate(
                log.bpTakenOn,
                DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                DateUtils.DATE_FORMAT_ddMMMyyyy
            ).ifEmpty { getString(R.string.separator_hyphen) }
            binding.llLastAssessment.tvValue.text = formattedDate
            binding.llValue.tvValue.text = getString(
                R.string.average_mmhg_string,
                CommonUtils.getDecimalFormatted(log.avgSystolic),
                CommonUtils.getDecimalFormatted(log.avgDiastolic)
            )
            setSymptoms(
                log.symptoms?.filter { it.isNotBlank() }
                ?.takeIf { it.isNotEmpty() }
                ?.joinToString(separator = ", ")
                ?: getString(R.string.hyphen_symbol),
                binding.llSymptoms.tvValue
            )
        }
    }

    private fun populateBGDetails(
        logList: ArrayList<GlucoseLogList>,
        num: Int?,
        response: BPBGListModel
    ) {
        val type = when (num) {
            NCDMRUtil.fbs_code -> NCDMRUtil.fbs
            NCDMRUtil.rbs_code -> NCDMRUtil.rbs
            else -> null
        }
        type?.let { glucoseType ->
            logList.firstOrNull { it.glucoseType == glucoseType }?.let { response.latestGlucoseLog }
                ?.let {
                    renderLatestBGLogDetails(response.latestGlucoseLog)
                } ?: renderLatestBGLogDetails(null)
        } ?: renderLatestBGLogDetails(response.latestGlucoseLog)
    }

    private fun renderLatestBGLogDetails(latestGlucoseLog: GlucoseLogList?) {
        latestGlucoseLog?.let { log ->
            binding.llLastAssessment.tvValue.text = log.glucoseDateTime?.let {
                DateUtils.convertDateTimeToDate(
                    it,
                    DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                    DateUtils.DATE_FORMAT_ddMMMyyyy
                )
            } ?: getString(R.string.separator_hyphen)

            binding.llValue.tvValue.text = log.glucoseValue?.let {
                "${CommonUtils.getDecimalFormatted(it)} ${getGlucoseUnit(log.glucoseUnit, false)}"
            } ?: getString(R.string.separator_hyphen)
            setSymptoms(log.symptoms?.filter { it.isNotBlank() }
                ?.takeIf { it.isNotEmpty() }
                ?.joinToString(separator = ", ")
                ?: getString(R.string.hyphen_symbol), binding.llSymptoms.tvValue)
        } ?: run {
            listOf(
                binding.llLastAssessment.tvValue,
                binding.llValue.tvValue,
                binding.llSymptoms.tvValue
            ).forEach { it.text = getString(R.string.separator_hyphen) }
        }
    }

    private fun setSymptoms(symptoms: String, tvValue: TextView) {
        // do more
        tvValue.setExpandableText(
            symptoms,
            title = getString(R.string.symptoms),
            maxLength = 35,
            activity = (activity as BaseActivity),
        )
    }

    private fun hideGraphLoader(isBPSummary: Boolean) {
        val viewsToShow = listOf(binding.clGraph, binding.btnAddNewReading)
        val viewsToHide = listOf(binding.CenterProgress)

        viewsToShow.forEach { view ->
            if (view.tag == isBPSummary) {
                view.visible()
            }
        }

        viewsToHide.forEach { view ->
            if (view.tag == isBPSummary) {
                view.gone()
            }
        }

        assessmentBpBg()
    }

    private fun assessmentBpBg() {
        if (CommonUtils.isNurse()) {
            binding.btnAddNewReading.text =
                if (isBPSummary) getString(R.string.add_blood_pressure_readings) else getString(
                    R.string.add_blood_glucose_readings
                )
            binding.btnAddNewReading.visible()
        } else
            binding.btnAddNewReading.gone()
    }

    private fun showGraphLoader(isBPSummary: Boolean) {
        val viewsToHide = listOf(binding.clGraph, binding.btnAddNewReading)
        val viewsToShow = listOf(binding.CenterProgress)

        viewsToHide.forEach { view ->
            if (view.tag == isBPSummary) {
                view.gone()
            }
        }

        viewsToShow.forEach { view ->
            if (view.tag == isBPSummary) {
                view.visible()
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.ivGraphNext -> {
                getValues(true)
            }

            R.id.ivGraphPrevious -> {
                getValues(false)
            }

            R.id.btnAddNewReading -> {
                val tag = if (isBPSummary) {
                    NCDBloodPressureVitalsDialog.TAG
                } else {
                    NCDBloodGlucoseReadingDialog.TAG
                }
                // Check if the fragment already exists
                val existingFragment = childFragmentManager.findFragmentByTag(tag)
                // Show the new instance of the dialog
                if (isBPSummary) {
                    if (existingFragment == null) {
                        NCDBloodPressureVitalsDialog.newInstance(){
                            GeneralSuccessDialog.newInstance(
                                getString(R.string.blood_pressure), getString(
                                    R.string.blood_pressure_saved_successfully
                                ), getString(R.string.okay)
                            ) {
                                (requireActivity() as? NCDMedicalReviewCMRActivity)?.swipeRefresh()
                            }.show(parentFragmentManager, GeneralSuccessDialog.TAG)
                        }.show(
                            childFragmentManager,
                            NCDBloodPressureVitalsDialog.TAG
                        )
                    }
                } else {
                    if (existingFragment == null) {
                        NCDBloodGlucoseReadingDialog.newInstance(){
                            GeneralSuccessDialog.newInstance(
                                getString(R.string.blood_glucose), getString(
                                    R.string.blood_glucose_saved_successfully
                                ), getString(R.string.okay)
                            ) {
                                (requireActivity() as? NCDMedicalReviewCMRActivity)?.swipeRefresh()
                            }.show(parentFragmentManager, GeneralSuccessDialog.TAG)
                        }.show(
                            childFragmentManager,
                            NCDBloodGlucoseReadingDialog.TAG
                        )
                    }
                }
            }
        }
    }
}