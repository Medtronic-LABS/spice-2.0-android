package com.medtroniclabs.opensource.ui.medicalreview.abovefiveyears

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.common.CommonUtils.combineText
import com.medtroniclabs.opensource.common.CommonUtils.convertListToString
import com.medtroniclabs.opensource.common.CommonUtils.createInvestigation
import com.medtroniclabs.opensource.common.CommonUtils.createPrescription
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.DefinedParams.OtherNotes
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.common.ViewUtils.showDatePicker
import com.medtroniclabs.opensource.data.AboveFiveYearsSummaryDetails
import com.medtroniclabs.opensource.data.MedicalReviewMetaItems
import com.medtroniclabs.opensource.data.history.PatientStatus
import com.medtroniclabs.opensource.data.model.MultiSelectDropDownModel
import com.medtroniclabs.opensource.databinding.FragmentMedicalReviewTreatmentPlanSummaryBinding
import com.medtroniclabs.opensource.formgeneration.extension.markMandatory
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.formgeneration.utility.CustomSpinnerAdapter
import com.medtroniclabs.opensource.formgeneration.utility.MultiSelectSpinnerAdapter
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseFragment
import com.medtroniclabs.opensource.ui.assessment.referrallogic.utils.ReferralStatus
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewDefinedParams
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewTypeEnums
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.PatientDetailViewModel

class AboveFiveYearsTreatmentSummaryFragment : BaseFragment(), View.OnClickListener {
    companion object {
        const val TAG = "AboveFiveYearsTreatmentSummaryFragment"
        fun newInstance(): AboveFiveYearsTreatmentSummaryFragment {
            return AboveFiveYearsTreatmentSummaryFragment()
        }
    }

    private lateinit var binding: FragmentMedicalReviewTreatmentPlanSummaryBinding
    private val viewModel: AboveFiveYearsViewModel by activityViewModels()
    private val chipItemViewModel: ClinicalNotesViewModel by activityViewModels()
    private val presentingComplaintsViewModel: PresentingComplaintsViewModel by activityViewModels()
    private val patientDetailViewModel: PatientDetailViewModel by activityViewModels()
    private var datePickerDialog: DatePickerDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            FragmentMedicalReviewTreatmentPlanSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initializeListener()
        attachObserver()
    }

    private fun initializeListener() {
        binding.tvNextMedicalReviewLabelText.safeClickListener(this)
    }

    private fun attachObserver() {
        viewModel.summaryMetaListItems.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.SUCCESS -> {
                    hideProgress()
                    resourceState.data?.let { list ->
                     initializeCostItem(list.filter { item -> item.category == MedicalReviewTypeEnums.cost.name }
                            .sortedBy { it.displayOrder })
                        initializeMedicalSupplies(list.filter { item -> item.category == MedicalReviewTypeEnums.medical_supplies.name }
                            .sortedBy { it.displayOrder })
                    }
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }
            }
        }

        viewModel.summaryDetailsLiveData.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }

                ResourceState.SUCCESS -> {
                    hideProgress()
                    resourceState.data?.let {
                        renderSummaryDetails(it)
                    }
                }
            }
            val swipeRefresh =
                (activity as AboveFiveYearsBaseActivity).findViewById<SwipeRefreshLayout>(R.id.refreshLayout)
            if (swipeRefresh.isRefreshing) {
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun renderSummaryDetails(details: AboveFiveYearsSummaryDetails) {
        initializePatientStatus(details.summaryStatus)
        binding.tvDiagnosisText.text =
            details.diagnosis?.let {list ->
                if (list.isNotEmpty()){
                    binding.tvDiagnosisText.setTextColor(ContextCompat.getColor(requireContext(), R.color.a_red_error))
                }
                convertListToString(
                    ArrayList(list.filter { it.diseaseCategory.lowercase() != OtherNotes.lowercase() }.map { it.diseaseCategory }.distinct())
                )
            } ?: requireContext().getString(R.string.hyphen_symbol)
        binding.tvPresentingComplaintsText.text = presentingComplaintsViewModel.selectedPresentingComplaints.map { it.name }.let {
            combineText(
                it, details.presentingComplaintsNotes, getString(R.string.hyphen_symbol)
            )
        }
        binding.tvDiseaseCategoryText.text =
            details.diagnosis?.let { list ->
                convertListToString(
                    ArrayList(list.filter { it.diseaseCategory.lowercase() != OtherNotes.lowercase() }
                        .map { it.diseaseCategory }.distinct())
                )
            } ?: requireContext().getString(R.string.hyphen_symbol)
        binding.tvDiseaseConditionText.text =
            details.diagnosis?.let { list ->
                convertListToString(
                    ArrayList(list.filter { it.diseaseCategory.lowercase() != OtherNotes.lowercase() }
                        .mapNotNull { it.diseaseCondition })
                )
            } ?: requireContext().getString(R.string.hyphen_symbol)
        binding.tvClinicalNotesText.text = chipItemViewModel.enteredClinicalNotes
        binding.tvClinicalName.text = requireContext().getString(
            R.string.firstname_lastname,
            SecuredPreference.getUserDetails()?.firstName,
            SecuredPreference.getUserDetails()?.lastName
        )
        binding.tvDateOfReviewValue.text = DateUtils.convertDateTimeToDate(
            DateUtils.getTodayDateDDMMYYYY(),
            DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
            DateUtils.DATE_ddMMyyyy
        )
        binding.tvPrescriptionsText.text = details.prescriptions?.let { createPrescription(it, requireContext()) }?.takeIf { it.isNotEmpty() }
            ?: requireContext().getString(R.string.hyphen_symbol)

        binding.tvInvestigationText.text = details.investigations?.let { createInvestigation(it,requireContext()) }?.takeIf { it.isNotEmpty() }
            ?: requireContext().getString(R.string.hyphen_symbol)
    }

    private fun initView() {
        binding.tvNextMedicalReviewLabel.markMandatory()
        binding.tvPatientStatus.markMandatory()
        viewModel.getSummaryListMetaItems(MedicalReviewTypeEnums.ABOVE_FIVE_YEARS.name)
    }

    private fun initializePatientStatus(patientStatusList: List<PatientStatus>?) {
        val dropDownList = ArrayList<Map<String, Any>>()
        if (patientStatusList != null) {
            for (item in patientStatusList) {
                dropDownList.add(
                    hashMapOf<String, Any>(
                        DefinedParams.NAME to item.name,
                        DefinedParams.Value to item.value
                    )
                )
            }
        }
        val adapter = CustomSpinnerAdapter(requireContext())
        adapter.setData(dropDownList)
        var defaultPosition = 0
        for ((index, patientStatus) in dropDownList.withIndex()) {
            if ((patientStatus[DefinedParams.Value] as? String).equals(
                    ReferralStatus.OnTreatment.name,
                    true
                )
            ) {
                defaultPosition = index
            }
        }
        binding.tvPatientStatusSpinner.post {
                binding.tvPatientStatusSpinner.setSelection(defaultPosition, false)
        }
        binding.tvPatientStatusSpinner.adapter = adapter
        binding.tvPatientStatusSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    view: View?,
                    pos: Int,
                    itemId: Long
                ) {
                    val selectedItem = adapter.getData(position = pos)
                    selectedItem?.let {
                        val selectedName = it[DefinedParams.Value] as String?
                        selectedName?.let { name ->
                            viewModel.selectedPatientStatus = name
                        }
                        updateNextFollowUpDate()
                    }
                    summaryListener()
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    /**
                     * this method is not used
                     */
                }
            }
    }

    private fun updateNextFollowUpDate() {
        if (viewModel.selectedPatientStatus?.equals(ReferralStatus.Recovered.name, true) == true) {
            if (viewModel.nextFollowupDate != null) {
                viewModel.nextFollowupDate = null
                binding.tvNextMedicalReviewLabelText.text = ""
            }
            binding.tvNextMedicalReviewLabelText.isEnabled = false
        } else {
            if(!binding.tvNextMedicalReviewLabelText.isEnabled){
                binding.tvNextMedicalReviewLabelText.isEnabled = true
            }
        }
    }

    private fun initializeCostItem(costList: List<MedicalReviewMetaItems>) {
        val dropDownList = ArrayList<Map<String, Any>>()
        dropDownList.add(
            hashMapOf<String, Any>(
                DefinedParams.NAME to DefinedParams.DefaultIDLabel,
                DefinedParams.id to DefinedParams.DefaultID,
                DefinedParams.Value to DefinedParams.DefaultIDLabel
            )
        )
        for (item in costList) {
            dropDownList.add(
                hashMapOf<String, Any>(
                    DefinedParams.NAME to item.name,
                    DefinedParams.id to item.id.toString(),
                    DefinedParams.Value to (item.value ?: item.name)
                )
            )
        }
        val adapter = CustomSpinnerAdapter(requireContext())
        adapter.setData(dropDownList)
        binding.tvCostSpinner.adapter = adapter
        binding.tvCostSpinner.setSelection(0, false)
        binding.tvCostSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    view: View?,
                    pos: Int,
                    itemId: Long
                ) {
                    val selectedItem = adapter.getData(position = pos)
                    selectedItem?.let {
                        val selectedName = it[DefinedParams.NAME] as String?
                        if (selectedName != DefinedParams.DefaultIDLabel) {
                                viewModel.selectedCostItem = it[DefinedParams.Value] as String
                        } else {
                            viewModel.selectedCostItem = null
                        }
                    }
                    summaryListener()
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    /**
                     * this method is not used
                     */
                }
            }
    }

    private fun initializeMedicalSupplies(supplyList: List<MedicalReviewMetaItems>) {
        val dropDownList = ArrayList<MultiSelectDropDownModel>()
        for (item in supplyList) {
            dropDownList.add(
                MultiSelectDropDownModel(
                    id = item.id,
                    name = item.name,
                    value = item.value
                )
            )
        }
        val adapter = MultiSelectSpinnerAdapter(requireContext(), dropDownList, viewModel.selectedMedicalSupplyListItem)
        binding.tvMedicalSupplySpinner.adapter = adapter
        adapter.setOnItemSelectedListener(object :
            MultiSelectSpinnerAdapter.OnItemSelectedListener {
            override fun onItemSelected(
                selectedItems: List<MultiSelectDropDownModel>,
                pos: Int,
            ) {
                if (selectedItems.isNotEmpty()){
                    viewModel.selectedMedicalSupplyListItem = ArrayList(selectedItems)
                }
                summaryListener()
            }
        }
        )
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.tvNextMedicalReviewLabelText.id -> {
                showDatePickerDialog()
            }
        }
    }

    private fun showDatePickerDialog() {
        var yearMonthDate: Triple<Int?, Int?, Int?>? = null
        if (!binding.tvNextMedicalReviewLabelText.text.isNullOrBlank())
            yearMonthDate =
                DateUtils.convertedMMMToddMM(binding.tvNextMedicalReviewLabelText.text.toString())

        if (datePickerDialog == null) {
            datePickerDialog = showDatePicker(
                context = requireContext(),
                minDate = DateUtils.getTomorrowDate(),
                date = yearMonthDate,
                cancelCallBack = { datePickerDialog = null }
            ) { _, year, month, dayOfMonth ->
                val stringDate = "$dayOfMonth-$month-$year"
                binding.tvNextMedicalReviewLabelText.text =
                    DateUtils.convertDateTimeToDate(
                        stringDate,
                        DateUtils.DATE_FORMAT_ddMMyyyy,
                        DateUtils.DATE_ddMMyyyy
                    )
                viewModel.nextFollowupDate = binding.tvNextMedicalReviewLabelText.text.toString()
                datePickerDialog = null
                summaryListener()
            }
        }
    }

    private fun summaryListener() {
        setFragmentResult(
            MedicalReviewDefinedParams.SUMMARY_ITEM, bundleOf(
                MedicalReviewDefinedParams.SUMMARY_ITEM to true
            )
        )
    }
}