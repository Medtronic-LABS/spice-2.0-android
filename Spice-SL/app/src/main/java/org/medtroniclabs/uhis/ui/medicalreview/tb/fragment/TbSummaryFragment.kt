package org.medtroniclabs.uhis.ui.medicalreview.tb.fragment

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.invisible
import org.medtroniclabs.uhis.appextensions.setExpandableText
import org.medtroniclabs.uhis.appextensions.setVisible
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.DefinedParams.SITE_OF_DISEASE
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.common.ViewUtils
import org.medtroniclabs.uhis.data.model.TbHistory
import org.medtroniclabs.uhis.databinding.FragmentTbSummaryBinding
import org.medtroniclabs.uhis.formgeneration.extension.markMandatory
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.utility.CustomSpinnerAdapter
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.assessment.referrallogic.utils.ReferralStatus
import org.medtroniclabs.uhis.ui.medicalreview.tb.activity.TBMedicalReviewActivity
import org.medtroniclabs.uhis.ui.medicalreview.tb.viewmodel.TbSummaryViewModel
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.respiratory
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewTypeEnums
import org.medtroniclabs.uhis.ui.mypatients.viewmodel.PatientDetailViewModel

@AndroidEntryPoint
class TbSummaryFragment : BaseFragment(), View.OnClickListener {
    private lateinit var binding: FragmentTbSummaryBinding
    val adapter: CustomSpinnerAdapter by lazy { CustomSpinnerAdapter(requireContext()) }
    val adapterForTreatmentOutCome: CustomSpinnerAdapter by lazy { CustomSpinnerAdapter(requireContext()) }
    private var datePickerDialog: DatePickerDialog? = null
    val viewModel: TbSummaryViewModel by activityViewModels()
    val patientViewModel: PatientDetailViewModel by activityViewModels()
    private var encounterId: String? = null
    private var fhirId: String? = null

    fun setIds(
        encounterId: String?,
        fhirId: String?,
    ) {
        this.encounterId = encounterId
        this.fhirId = fhirId
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentTbSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        const val TAG = "TbSummaryFragment"

        fun newInstance() = TbSummaryFragment()

        fun newInstance(
            encounterId: String?,
            fhirId: String?,
        ): TbSummaryFragment {
            val fragment = TbSummaryFragment()
            fragment.setIds(encounterId = encounterId, fhirId = fhirId)
            return fragment
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        attachObserver()
    }

    private fun initView() {
        binding.tvNextMedicalReviewLabel.markMandatory()
        binding.tvPatientStatus.markMandatory()
        binding.tvClinicalName.text = requireContext().getString(
            R.string.firstname_lastname,
            SecuredPreference.getUserDetails()?.firstName,
            SecuredPreference.getUserDetails()?.lastName,
        )
        binding.tvDateOfReviewValue.text = DateUtils.convertDateTimeToDate(
            DateUtils.getTodayDateDDMMYYYY(),
            DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
            DateUtils.DATE_ddMMyyyy,
        )
        binding.tvNextMedicalReviewLabelText.safeClickListener(this)
        viewModel.fetchTbAssessmentDetails(
            encounterId,
            fhirId,
        )
    }

    private fun attachObserver() {
        viewModel.getTreatmentOutComeLiveData.observe(viewLifecycleOwner) { items ->
            val list = arrayListOf<Map<String, Any>>().apply {
                add(
                    mapOf(
                        DefinedParams.NAME to DefinedParams.DEFAULT_ID_LABEL,
                        DefinedParams.ID to -1L,
                    ),
                )

                items?.forEach { item ->
                    if (!item.value.equals(ReferralStatus.Died.name, true)) {
                        add(
                            mapOf(
                                DefinedParams.ID to item.id,
                                DefinedParams.NAME to item.name,
                                DefinedParams.Value to (item.value ?: item.name),
                            ),
                        )
                    }
                }
            }
            setTreatmentOutCome(list)
        }

        viewModel.tbSummary.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.SUCCESS -> {
                    hideProgress()
                    resourceState.data?.let {
                        populate(it)
                    }
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }
            }
        }
    }

    private fun populate(data: TbHistory) {
        with(binding) {
            val views = listOf(
                tvDiagnosesLabel,
                tvDiagnosesText,
                tvDiagnosesSeparator,
                tvSiteLabel,
                tvSiteSeparator,
                tvSiteText,
                tvTreatmentText,
                tvTreatmentLabel,
                tvTreatmentSeparator,
            )
            tvDiagnosesLabel.text = getText(R.string.diagnosis_tb)
            views.forEach { it.setVisible(patientViewModel.getTbMedicalReviewStatus()) }

            // Diagnosis Text
            val diagnosisList = data.diagnosis ?: emptyList()
            if (diagnosisList.isNotEmpty()) {
                tvDiagnosesText.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.a_red_error,
                    ),
                )
            }
            tvDiagnosesText.text = diagnosisList
                .filter {
                    it.diseaseCategory
                        .equals(DefinedParams.OTHER_NOTES, ignoreCase = true)
                        .not() &&
                        (it.type.equals(DefinedParams.TB, true) || it.type.isNullOrBlank())
                }.map { it.diseaseCategory }
                .distinct()
                .takeIf { it.isNotEmpty() }
                ?.let { CommonUtils.convertListToString(ArrayList(it)) }
                ?: getString(R.string.hyphen_symbol)

            // Site Text
            tvSiteText.text = diagnosisList
                .filter {
                    it.diseaseCategory
                        .equals(DefinedParams.OTHER_NOTES, ignoreCase = true)
                        .not() &&
                        it.type.equals(SITE_OF_DISEASE, true)
                }.map { it.diseaseCategory }
                .distinct()
                .takeIf { it.isNotEmpty() }
                ?.let { CommonUtils.convertListToString(ArrayList(it)) }
                ?: getString(R.string.hyphen_symbol)

            // Presenting Complaints
            tvPresentingText.setExpandableText(
                fullText = CommonUtils.combineText(
                    data.presentingComplaints,
                    data.presentingComplaintsNotes,
                    getString(R.string.hyphen_symbol),
                ),
                moreColorResId = R.color.purple_700,
                title = tvPresentingLabel.text.toString(),
                activity = requireActivity() as BaseActivity,
            )

            // Clinical Notes
            tvClinicalNotesText.setExpandableText(
                fullText = data.clinicalNotes?.takeIf { it.isNotEmpty() }
                    ?: getString(R.string.hyphen_symbol),
                moreColorResId = R.color.purple_700,
                title = tvClinicalNotesLabel.text.toString(),
                activity = requireActivity() as BaseActivity,
            )

            // Comorbidities
            tvComborbiditiesText.setExpandableText(
                fullText = CommonUtils.combineText(
                    data.comorbidities,
                    data.comorbiditiesNotes,
                    getString(R.string.hyphen_symbol),
                ),
                moreColorResId = R.color.purple_700,
                title = tvComborbiditiesLabel.text.toString(),
                activity = requireActivity() as BaseActivity,
            )

            // Systematic Examination
            tvGeneralText.setExpandableText(
                fullText = CommonUtils.combineText(
                    data.systemicExaminations?.map {
                        if (it.name.equals(respiratory, ignoreCase = true) && !it.value.isNullOrBlank()) {
                            "${it.name} : ${it.value}"
                        } else {
                            it.name
                        }
                    } ?: emptyList(),
                    "",
                    getString(R.string.hyphen_symbol),
                ),
                moreColorResId = R.color.purple_700,
                title = tvGeneralLabel.text.toString(),
                activity = requireActivity() as BaseActivity,
            )

            // Prescription
            tvPrescrptionText.text = data.prescriptions
                ?.let { CommonUtils.createPrescription(it, requireContext()) }
                ?.takeIf { it.isNotEmpty() }
                ?: getString(R.string.hyphen_symbol)

            // Investigation
            tvInvestigationText.text = data.investigations
                ?.let { CommonUtils.createInvestigation(it, requireContext()) }
                ?.takeIf { it.isNotEmpty() }
                ?: getString(R.string.hyphen_symbol)
        }

        val dropDownList = ArrayList<Map<String, Any>>()
        if (!data.summaryStatus.isNullOrEmpty()) {
            for (item in data.summaryStatus) {
                dropDownList.add(
                    hashMapOf<String, Any>(
                        DefinedParams.NAME to item.name,
                        DefinedParams.Value to item.value,
                    ),
                )
            }
            setSpinner(dropDownList)
            if (patientViewModel.getTbMedicalReviewStatus()) {
                viewModel.getTreatmentOutCome(MedicalReviewTypeEnums.treatment_outcome.name)
            }
        }
    }

    private fun setSpinner(statusList: ArrayList<Map<String, Any>>) {
        adapter.setData(statusList)
        var defaultPosition = 0
        for ((index, patientStatus) in statusList.withIndex()) {
            if ((patientStatus[DefinedParams.Value] as? String).equals(
                    ReferralStatus.OnTreatment.name,
                    true,
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
                    itemId: Long,
                ) {
                    val selectedItem = adapter.getData(position = pos)
                    selectedItem?.let {
                        val selectedPatientStatus = it[DefinedParams.Value] as String?
                        selectedPatientStatus?.let {
                            viewModel.patientStatus = selectedPatientStatus
                        } ?: kotlin.run {
                            viewModel.patientStatus = null
                        }
                        showHideNextVisit()
                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    /**
                     * this method is not used
                     */
                }
            }
    }

    private fun setTreatmentOutCome(treatmentOutCome: ArrayList<Map<String, Any>>) {
        adapterForTreatmentOutCome.setData(treatmentOutCome)
        binding.tvTreatmentText.post {
            binding.tvTreatmentText.setSelection(0, false)
        }
        binding.tvTreatmentText.adapter = adapterForTreatmentOutCome
        binding.tvTreatmentText.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    view: View?,
                    pos: Int,
                    itemId: Long,
                ) {
                    val selectedItem = adapterForTreatmentOutCome.getData(position = pos)
                    selectedItem?.let {
                        val selectedId = (it[DefinedParams.ID] as? Long) ?: -1L
                        val selectedTreatmentOutCome = it[DefinedParams.Value] as String?
                        if (selectedId != -1L) {
                            viewModel.treatmentOutCome = selectedTreatmentOutCome
                            // handleRecoveredState()
                        } else {
                            viewModel.treatmentOutCome = null
                        }
                        showHideNextVisit()
                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    /**
                     * this method is not used
                     */
                }
            }
    }

    private fun showHideNextVisit() {
        // Enable next medical review label unless patient is Recovered or Died
        binding.tvNextMedicalReviewLabelText.isEnabled =
            !(
                viewModel.patientStatus.equals(ReferralStatus.Recovered.name, true) ||
                    viewModel.patientStatus.equals(ReferralStatus.Died.name, true)
            )

        if (viewModel.patientStatus.equals(ReferralStatus.OnTreatment.name, true)) {
            binding.tvNextMedicalReviewLabelText.text = DateUtils.getFormattedDateAfterMonths(1)
            viewModel.nextFollowupDate = binding.tvNextMedicalReviewLabelText.text.toString()
        }

        // Clear label text if patient is Recovered or Died
        if (viewModel.patientStatus.equals(ReferralStatus.Recovered.name, true) ||
            viewModel.patientStatus.equals(ReferralStatus.Died.name, true)
        ) {
            binding.tvNextMedicalReviewLabelText.text = ""
            viewModel.nextFollowupDate = null
            binding.tvNextMedicalReviewError.invisible()
        }

        // Special case: Patient status is Recovered but treatment outcome is Died
        if ((
                viewModel.patientStatus.equals(ReferralStatus.Recovered.name, true) ||
                    viewModel.patientStatus.equals(ReferralStatus.OnTreatment.name, true)
            ) &&
            viewModel.treatmentOutCome.equals(ReferralStatus.Died.name, true)
        ) {
            val treatmentList = arrayListOf<Map<String, Any>>().apply {
                add(
                    mapOf(
                        DefinedParams.NAME to DefinedParams.DEFAULT_ID_LABEL,
                        DefinedParams.ID to -1L,
                    ),
                )

                viewModel.getTreatmentOutComeLiveData.value?.forEach { item ->
                    if (!item.value.equals(ReferralStatus.Died.name, true)) {
                        add(
                            mapOf(
                                DefinedParams.ID to item.id,
                                DefinedParams.NAME to item.name,
                                DefinedParams.Value to (item.value ?: item.name),
                            ),
                        )
                    }
                }
            }
            adapterForTreatmentOutCome.setData(treatmentList)
            binding.tvTreatmentText.post {
                binding.tvTreatmentText.isEnabled = true
                binding.tvTreatmentText.setSelection(0, false)
                viewModel.treatmentOutCome = ""
            }
        }

        // If patient is Died, auto-select the "Died" outcome and disable the field
        if (viewModel.patientStatus.equals(ReferralStatus.Died.name, true)) {
            val treatmentList = arrayListOf<Map<String, Any>>().apply {
                add(
                    mapOf(
                        DefinedParams.NAME to DefinedParams.DEFAULT_ID_LABEL,
                        DefinedParams.ID to -1L,
                    ),
                )
                viewModel.getTreatmentOutComeLiveData.value?.forEach { item ->
                    add(
                        mapOf(
                            DefinedParams.ID to item.id,
                            DefinedParams.NAME to item.name,
                            DefinedParams.Value to (item.value ?: item.name),
                        ),
                    )
                }
            }

            adapterForTreatmentOutCome.setData(treatmentList)

            val outcomeId = viewModel.getTreatmentOutComeLiveData.value
                ?.firstOrNull { it.value.equals(ReferralStatus.Died.name, true) }
                ?.id ?: -1L

            val index = adapterForTreatmentOutCome.getIndexOfItem(outcomeId)

            binding.tvTreatmentText.apply {
                post { setSelection(index, false) }
                isEnabled = false
            }
        }
        (requireActivity() as? TBMedicalReviewActivity)?.enableRefer(
            !viewModel.patientStatus.equals(ReferralStatus.Died.name, true),
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
        if (!binding.tvNextMedicalReviewLabelText.text.isNullOrBlank()) {
            yearMonthDate =
                DateUtils.convertedMMMToddMM(binding.tvNextMedicalReviewLabelText.text.toString())
        }

        if (datePickerDialog == null) {
            datePickerDialog = ViewUtils.showDatePicker(
                context = requireContext(),
                minDate = DateUtils.getTomorrowDate(),
                date = yearMonthDate,
                cancelCallBack = { datePickerDialog = null },
            ) { _, year, month, dayOfMonth ->
                val stringDate = "$dayOfMonth-$month-$year"
                binding.tvNextMedicalReviewLabelText.text = DateUtils.convertDateTimeToDate(
                    stringDate,
                    DateUtils.DATE_FORMAT_ddMMyyyy,
                    DateUtils.DATE_ddMMyyyy,
                )
                viewModel.nextFollowupDate = binding.tvNextMedicalReviewLabelText.text.toString()
                datePickerDialog = null
            }
        }
    }

    fun validateInput(): Boolean {
        val value = binding.tvNextMedicalReviewLabelText.text
            ?.trim()
            .toString()
        if (value.isBlank()) {
            if (viewModel.patientStatus?.equals(ReferralStatus.Recovered.name, true) == true ||
                viewModel.patientStatus?.equals(ReferralStatus.Died.name, true) == true
            ) {
                binding.tvNextMedicalReviewError.invisible()
                return true
            }
            binding.tvNextMedicalReviewError.visible()
            binding.tvNextMedicalReviewLabelText.requestFocus()
            return false
        }
        binding.tvNextMedicalReviewError.invisible()
        return true
    }
}
