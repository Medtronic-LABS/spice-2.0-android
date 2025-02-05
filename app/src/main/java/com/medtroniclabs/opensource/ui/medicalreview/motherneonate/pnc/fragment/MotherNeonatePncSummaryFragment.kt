package com.medtroniclabs.opensource.ui.medicalreview.motherneonate.pnc.fragment

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
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.setExpandableText
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.CommonUtils.combineText
import com.medtroniclabs.opensource.common.CommonUtils.convertListToString
import com.medtroniclabs.opensource.common.CommonUtils.createInvestigation
import com.medtroniclabs.opensource.common.CommonUtils.getBooleanAsString
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.common.ViewUtils
import com.medtroniclabs.opensource.data.MotherNeonatePncSummaryResponse
import com.medtroniclabs.opensource.data.history.PatientStatus
import com.medtroniclabs.opensource.databinding.FragmentMotherNeonarePncSummaryBinding
import com.medtroniclabs.opensource.databinding.MotherNeonatePncSummaryLayoutBinding
import com.medtroniclabs.opensource.formgeneration.extension.capitalizeFirstChar
import com.medtroniclabs.opensource.formgeneration.extension.markMandatory
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.formgeneration.utility.CustomSpinnerAdapter
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.BaseFragment
import com.medtroniclabs.opensource.ui.assessment.referrallogic.utils.ReferralStatus
import com.medtroniclabs.opensource.ui.assessment.rmnch.RMNCH
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.pnc.viewmodel.MotherNeonatePncSummaryViewModel
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewDefinedParams
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewTypeEnums
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.PatientDetailViewModel


class MotherNeonatePncSummaryFragment(var isShowNeonate: Boolean) : BaseFragment(), View.OnClickListener {
    private lateinit var binding: FragmentMotherNeonarePncSummaryBinding
    var adapter: CustomSpinnerAdapter? = null
    private var datePickerDialog: DatePickerDialog? = null
    val viewModel: MotherNeonatePncSummaryViewModel by activityViewModels()
    private val patientDetailViewModel: PatientDetailViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMotherNeonarePncSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        const val TAG = "MotherNeonateSummary"
        fun newInstance(isShowNeonate: Boolean): MotherNeonatePncSummaryFragment {
            return MotherNeonatePncSummaryFragment(isShowNeonate)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getPncSummaryDetails()
        getPncPatientStatus(MedicalReviewTypeEnums.PNC_MOTHER_REVIEW.name)
        clickListener()
        attachObservers()
        showNeonateSummary()
    }

    private fun showNeonateSummary() {
        if (isShowNeonate) {
            if (patientDetailViewModel.patientDetailsNeonateLiveData.value?.data?.isActive==false)
            {
                binding.neonateSummary.cardSummary.gone()
            }else{
                binding.neonateSummary.cardSummary.visible()
            }
        } else {
            binding.neonateSummary.cardSummary.gone()
        }
    }
    private fun getPncSummaryDetails() = viewModel.getPncSummaryDetails()
    private fun getPncPatientStatus(category: String) =
        viewModel.setPncReqToGetMetaForPatientStatus(category = category)

    private fun clickListener() {
        binding.motherSummary.tvPatientStatus.markMandatory()
        binding.neonateSummary.tvPatientStatus.markMandatory()
        binding.motherSummary.tvNextMedicalReviewLabel.markMandatory()
        binding.motherSummary.tvNextMedicalReviewLabelText.safeClickListener(this)
    }

    private fun attachObservers() {

        viewModel.pncSummaryResponse.observe(viewLifecycleOwner) { resource ->
            handleResourceState(resource) {
                initializeMotherSummaryDetails(resource.data)
                initializeNeonateSummaryDetails(resource.data)
            }
        }
    }

    private fun patientStatusBasedOnType(resource: List<PatientStatus>?) {
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
        if (viewModel.motherNeonateAlive) {
            viewModel.pncMotherPatientStatus = resource
            viewModel.pncMotherPatientStatus?.let {
                initializePatientStatus(it, binding.motherSummary)
            }
            getPncPatientStatus(MedicalReviewTypeEnums.PNC_CHILD_REVIEW.name)
        } else {
            binding.motherSummary.tvNextMedicalReviewLabel.gone()
            binding.motherSummary.tvNextMedicalReviewSeparator.gone()
            binding.motherSummary.tvNextMedicalReviewLabelText.gone()
            binding.motherSummary.tvPatientStatus.gone()
            binding.motherSummary.tvPatientSeparator.gone()
            binding.motherSummary.tvPatientStatusSpinner.gone()
            notAliveFlow()
        }
    }


    private fun initializeMotherSummaryDetails(data: MotherNeonatePncSummaryResponse?) {
        patientStatusBasedOnType(data?.pncMother?.summaryStatus)

        binding.motherSummary.apply {
            tvTitle.text = getString(R.string.pnc_visit_summary_mother)
            tvPncVisitNoText.text =
                ((data?.pncMother?.visitNumber) ?: getString(R.string.empty__)).toString()
            val presentingComplaintsText = combineText(
                data?.pncMother?.presentingComplaints,
                data?.pncMother?.presentingComplaintsNotes,
                getString(R.string.hyphen_symbol)
            )
            tvPresentingComplaintsText.setExpandableText(
                fullText = presentingComplaintsText,
                moreColorResId = R.color.purple_700,
                title = tvPresentingComplaintsLabel.text.toString(),
                activity = (requireActivity() as BaseActivity)
            )

            tvClinicalNotesText.setExpandableText(
                fullText = (data?.pncMother?.clinicalNotes) ?: getString(R.string.hyphen_symbol),
                moreColorResId = R.color.purple_700,
                title = tvClinicalNotesLabel.text.toString(),
                activity = (requireActivity() as BaseActivity)
            )
            val list = mutableListOf<HashMap<String, Pair<String?, Any?>>>()
            data?.pncMother?.breastCondition?.let { breastCondition ->
                data.pncMother.breastConditionNotes.let { breastConditionNotes ->
                    list.add(
                        hashMapOf(
                            getString(R.string.breast_condition) to Pair(
                                breastCondition,
                                breastConditionNotes
                            )
                        )
                    )
                }
            }
            data?.pncMother?.involutionsOfTheUterus?.let { involutionsOfTheUterus ->
                data.pncMother.involutionsOfTheUterusNotes.let { involutionsOfTheUterusNotes ->
                    list.add(
                        hashMapOf(
                            getString(R.string.involutions_of_the_nuterus_summary) to Pair(
                                involutionsOfTheUterus,
                                involutionsOfTheUterusNotes
                            )
                        )
                    )
                }
            }
            tvExaminationsText.text =
                list.let { CommonUtils.createMotherNeonateExamination(it, requireContext(), true) }
                    ?.takeIf { it.isNotEmpty() }
                    ?: requireContext().getString(R.string.hyphen_symbol)


            tvPrescriptionsText.text = data?.pncMother?.prescriptions.let {
                CommonUtils.createPrescription(
                    it,
                    requireContext()
                )
            }?.takeIf { it.isNotEmpty() }
                ?: requireContext().getString(R.string.hyphen_symbol)

            tvInvestigationText.text = data?.pncMother?.investigations?.let { createInvestigation(it,requireContext()) }?.takeIf { it.isNotEmpty() }
                ?: requireContext().getString(R.string.hyphen_symbol)

            tvAncVisitText.text =
                data?.pncMother?.diagnosis?.let { list ->
                    if (list.isNotEmpty()) {
                        binding.motherSummary.tvAncVisitText.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.a_red_error
                            )
                        )
                    }
                    convertListToString(
                        ArrayList(list.filter { it.diseaseCategory.lowercase() != DefinedParams.OtherNotes.lowercase() }.map { it.diseaseCategory }.distinct())
                    )
                } ?: requireContext().getString(R.string.empty__)

        }
        patientDetailViewModel.dateOfDelivery?.let {
            DateUtils.convertStringToDate(
                it,
                DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ
            )?.let { deliveryDate ->
                RMNCH.calculateNextPNCVisitDate(deliveryDate)?.let { visitDate ->
                    binding.motherSummary.tvNextMedicalReviewLabelText.text = DateUtils.getDateStringFromDate(
                        visitDate, DateUtils.DATE_ddMMyyyy
                    )
                    viewModel.nextFollowupDate= binding.motherSummary.tvNextMedicalReviewLabelText.text.toString()
                    summaryListener()
                }
            }
        }

    }


    private fun initializeNeonateSummaryDetails(data: MotherNeonatePncSummaryResponse?) {
        binding.neonateSummary.neonateflow.gone()
        binding.neonateSummary.apply {
            tvTitle.text = getString(R.string.pnc_visit_summary_neonate)
            tvPncVisitNoText.text = (data?.pncChild?.visitNumber.toString())

            val presentingComplaintsText = combineText(
                data?.pncChild?.presentingComplaints,
                data?.pncChild?.presentingComplaintsNotes,
                getString(R.string.hyphen_symbol)
            )
            tvPresentingComplaintsText.setExpandableText(
                fullText = presentingComplaintsText,
                moreColorResId = R.color.purple_700,
                title = tvPresentingComplaintsLabel.text.toString(),
                activity = (requireActivity() as BaseActivity)
            )

            tvClinicalNotesText.setExpandableText(
                fullText = (data?.pncChild?.clinicalNotes) ?: getString(R.string.hyphen_symbol),
                moreColorResId = R.color.purple_700,
                title = tvClinicalNotesLabel.text.toString(),
                activity = (requireActivity() as BaseActivity)
            )
            val list = mutableListOf<HashMap<String, Pair<String?, Any?>>>()
            data?.pncChild?.congenitalDetect?.let { congenitalDetect ->
                list.add(
                    hashMapOf(
                        getString(R.string.congenital_detect) to Pair(
                            getBooleanAsString(congenitalDetect.toBoolean()).capitalizeFirstChar(),
                            null
                        )
                    )
                )
            }
            data?.pncChild?.cordExamination?.let { cordExamination ->
                list.add(
                    hashMapOf(
                        getString(R.string.cord_examination) to Pair(
                            cordExamination,
                            null
                        )
                    )
                )
            }
            tvExaminationsLabel.text = getString(R.string.physical_examinations)

            tvExaminationsText.text = list.let {
                CommonUtils.createMotherNeonateExamination(
                    it,
                    requireContext(),
                    false
                )
            }?.takeIf { it.isNotEmpty() }
                ?: requireContext().getString(R.string.hyphen_symbol)
        }
    }

    private fun initializePatientStatus(
        pncMotherPatientStatus: List<PatientStatus>,
        patientStatusBinding: MotherNeonatePncSummaryLayoutBinding
    ) {
        val dropDownList = ArrayList<Map<String, Any>>()

        for (item in pncMotherPatientStatus) {
            dropDownList.add(
                hashMapOf<String, Any>(
                    DefinedParams.NAME to
                            item.name,
                    DefinedParams.Value to item.value
                )
            )
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
        patientStatusBinding.tvPatientStatusSpinner.post {
            patientStatusBinding.tvPatientStatusSpinner.setSelection(defaultPosition, false)
        }
        patientStatusBinding.tvPatientStatusSpinner.adapter = adapter
        patientStatusBinding.tvPatientStatusSpinner.onItemSelectedListener =
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
                            pncMotherPatientStatus.let { type ->
                                viewModel.patientStatusMother = name
                            }
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

    private fun summaryListener() {
        setFragmentResult(
            MedicalReviewDefinedParams.SUMMARY_ITEM, bundleOf(
                MedicalReviewDefinedParams.SUMMARY_ITEM to true
            )
        )
    }

    private inline fun handleResourceState(resource: Resource<*>, onSuccessBlock: () -> Unit = {}) {
        when (resource.state) {
            ResourceState.LOADING -> showProgress()
            ResourceState.SUCCESS -> {
                hideProgress()
                onSuccessBlock()
            }

            ResourceState.ERROR -> {
                resource.optionalData?.let {
                } ?: showErrorDialog(
                    title = getString(R.string.alert),
                    message = getString(R.string.something_went_wrong_try_later),
                )
            }
        }
    }

    private fun showDatePickerDialog() {
        var yearMonthDate: Triple<Int?, Int?, Int?>? = null
        if (!binding.motherSummary.tvNextMedicalReviewLabelText.text.isNullOrBlank())
            yearMonthDate =
                DateUtils.convertedMMMToddMM(binding.motherSummary.tvNextMedicalReviewLabelText.text.toString())
        if (datePickerDialog == null) {
            datePickerDialog = ViewUtils.showDatePicker(
                context = requireContext(),
                minDate = DateUtils.getTomorrowDate(),
                date = yearMonthDate,
                cancelCallBack = { datePickerDialog = null }
            ) { _, year, month, dayOfMonth ->
                val stringDate = "$dayOfMonth-$month-$year"
                binding.motherSummary.tvNextMedicalReviewLabelText.text =
                    DateUtils.convertDateTimeToDate(
                        stringDate,
                        DateUtils.DATE_FORMAT_ddMMyyyy,
                        DateUtils.DATE_ddMMyyyy
                    )
                viewModel.nextFollowupDate =
                    binding.motherSummary.tvNextMedicalReviewLabelText.text.toString()
                datePickerDialog = null
                summaryListener()
            }
        }
    }


    private fun updateNextFollowUpDate() {
        binding.motherSummary.tvNextMedicalReviewLabelText.isEnabled = true

    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.motherSummary.tvNextMedicalReviewLabelText.id -> {
                showDatePickerDialog()
            }
        }
    }
    private fun notAliveFlow() {
        setFragmentResult(
            MedicalReviewDefinedParams.NOT_ALIVE, bundleOf(
                MedicalReviewDefinedParams.NOT_ALIVE to true
            )
        )
    }
}

