package org.medtroniclabs.uhis.ui.referralhistory.fragment

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.internal.LinkedTreeMap
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.changePatientStatus
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.CommonUtils.combineText
import org.medtroniclabs.uhis.common.CommonUtils.toFormattedList
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.DefinedParams.ABOVE_5_MEDICAL_REVIEW
import org.medtroniclabs.uhis.common.DefinedParams.EMTCT_HIV_MEDICAL_REVIEW
import org.medtroniclabs.uhis.common.DefinedParams.EMTCT_HIV_MEDICAL_SCREENING
import org.medtroniclabs.uhis.common.DefinedParams.HIV_MEDICAL_SCREENING
import org.medtroniclabs.uhis.common.DefinedParams.ICCM_ABOVE_2M_5Y
import org.medtroniclabs.uhis.common.DefinedParams.MOTHER_DELIVERY_REVIEW
import org.medtroniclabs.uhis.common.DefinedParams.NAME
import org.medtroniclabs.uhis.common.DefinedParams.NEONATE_BIRTH_REVIEW
import org.medtroniclabs.uhis.common.DefinedParams.OTHER_NOTES
import org.medtroniclabs.uhis.common.DefinedParams.PregnancyAncMedicalReview
import org.medtroniclabs.uhis.common.DefinedParams.TB
import org.medtroniclabs.uhis.common.DefinedParams.Value
import org.medtroniclabs.uhis.data.history.Eligibility
import org.medtroniclabs.uhis.data.history.MedicalReviewHistory
import org.medtroniclabs.uhis.data.model.ChipResponse
import org.medtroniclabs.uhis.databinding.FragmentReferralTicketBinding
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.model.ReferredDate
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewTypeEnums
import org.medtroniclabs.uhis.ui.mypatients.adapter.DateListAdapter
import org.medtroniclabs.uhis.ui.referralhistory.adapter.ReferralHistoryAdapter
import org.medtroniclabs.uhis.ui.referralhistory.viewmodel.ReferralHistoryViewModel
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class MedicalReviewHistoryFragment : BaseFragment(), View.OnClickListener {
    lateinit var binding: FragmentReferralTicketBinding
    private var listPopupWindow: PopupWindow? = null
    private lateinit var dateListAdapter: DateListAdapter
    private lateinit var adapters: ReferralHistoryAdapter
    val viewModel: ReferralHistoryViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentReferralTicketBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        const val TAG = "MedicalReviewHistoryFragment"

        fun newInstance(): MedicalReviewHistoryFragment = MedicalReviewHistoryFragment()

        fun newInstance(patientId: String?): MedicalReviewHistoryFragment {
            val fragment = MedicalReviewHistoryFragment()
            val bundle = Bundle()
            bundle.putString(DefinedParams.FhirId, patientId)
            fragment.arguments = bundle
            return fragment
        }
    }

    private fun getPatientId(): String? = arguments?.getString(DefinedParams.FhirId, "")

    private fun getInitialReferralTickets() {
        getPatientId()
            ?.takeIf { it.isNotBlank() }
            ?.let { viewModel.getMedicalReviewHistory(patientId = it) }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        attachObservers()
    }

    private fun initView() {
        binding.tvNoHistory.text = requireContext().getString(R.string.no_medical_review_history)
        binding.tvReferralTicketTitle.text =
            requireContext().getString(R.string.medical_review_history)
        if (getPatientId().isNullOrBlank()) {
            binding.tvNoHistory.visible()
            binding.llHistoryAction.ivNext.gone()
            binding.llHistoryAction.ivPrevious.gone()
            binding.llHistoryAction.ivReload.gone()
        } else {
            binding.tvNoHistory.gone()
            adapters = ReferralHistoryAdapter()
            viewModel.medicalTicketId = null
            getInitialReferralTickets()
            setupClickListeners()
            binding.retryButtonBp.safeClickListener(this)
            setupPopupWindow()
        }
    }

    private fun setupPopupWindow() {
        val inflater =
            requireActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.layout_popup_window, null)
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvDateList)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.addItemDecoration(
            DividerItemDecoration(
                recyclerView.context,
                DividerItemDecoration.VERTICAL,
            ),
        )
        dateListAdapter =
            DateListAdapter { referred ->
                if (connectivityManager.isNetworkAvailable()) {
                    viewModel.getMedicalReviewHistory(
                        patientId = getPatientId(),
                        medicalTicketId = referred.id,
                    )
                    viewModel.medicalTicketId = referred.id
                } else {
                    showErrorDialog(
                        getString(R.string.error),
                        getString(R.string.no_internet_error),
                    )
                }
                listPopupWindow?.dismiss()
            }
        recyclerView.adapter = dateListAdapter
        listPopupWindow = PopupWindow(
            view,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )
    }

    private fun setupClickListeners() {
        with(binding.llHistoryAction) {
            ivReload.safeClickListener(this@MedicalReviewHistoryFragment)
            ivNext.safeClickListener(this@MedicalReviewHistoryFragment)
            ivPrevious.safeClickListener(this@MedicalReviewHistoryFragment)
        }
    }

    private fun attachObservers() {
        viewModel.medicalReviewTicketLiveData.observe(viewLifecycleOwner) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    if (resource.data?.history.isNullOrEmpty() && viewModel.medicalTicketId.isNullOrBlank()) {
                        binding.groupHistoryList.gone()
                        binding.clLoaderProgress.gone()
                        binding.tvNoHistory.visible()
                    } else {
                        resource.data?.let {
                            handleSuccess()
                            setReferralTicket(it)
                        } ?: kotlin.run {
                            binding.groupHistoryList.gone()
                            binding.clLoaderProgress.gone()
                            binding.tvNoHistory.visible()
                        }
                    }
                }

                ResourceState.ERROR -> {
                    handleError()
                }
            }
        }
    }

    override fun showLoading() {
        binding.clLoaderProgress.visible()
        binding.loaderProgress.visible()
        binding.retryButtonBp.gone()
        binding.tvErrorLabel.gone()
        binding.tvNoHistory.gone()
        binding.groupHistoryList.gone()
    }

    private fun handleSuccess() {
        binding.groupHistoryList.visible()
        binding.clLoaderProgress.gone()
        binding.llHistoryAction.ivNext.visible()
        binding.llHistoryAction.ivPrevious.visible()
        binding.llHistoryAction.ivReload.visible()
    }

    private fun handleError() {
        binding.clLoaderProgress.visible()
        binding.retryButtonBp.visible()
        binding.tvErrorLabel.visible()
        binding.groupHistoryList.gone()
    }

    private fun setReferralTicket(medicalReviewHistory: MedicalReviewHistory) {
        binding.tvNoHistory.gone()
        adapters.updateList(
            createMedicalReview(medicalReviewHistory),
        )
        adjustGuideline()
        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = adapters
        setReferralDates(medicalReviewHistory.history, medicalReviewHistory.id)
    }

    private fun adjustGuideline() {
        val params = binding.centerGuideline.layoutParams as ConstraintLayout.LayoutParams
        params.guidePercent =
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) 0.5f else 0.8f
        binding.centerGuideline.layoutParams = params
    }

    private fun checkNextPreviousVisibility() {
        binding.llHistoryAction.ivPrevious.isEnabled = checkForPreviousItem() != -1
        binding.llHistoryAction.ivNext.isEnabled = checkNextItem() != -1
    }

    private fun setReferralDates(
        referredDates: List<ReferredDate>?,
        id: String?,
    ) {
        if (referredDates != null) {
            if (viewModel.medicalTicketId == null) {
                viewModel.medicalTicketId = id
                viewModel.medicalReferralDates.value = referredDates
            }
            viewModel.medicalReferralDates.value?.let { list ->
                if (referredDates.size > list.size) {
                    viewModel.medicalTicketId = id
                    viewModel.medicalReferralDates.value = referredDates
                }
            }
            viewModel.medicalReferralDates.value?.let {
                dateListAdapter.submitData(it, viewModel.medicalTicketId)
            }
        }

        checkNextPreviousVisibility()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.llHistoryAction.ivReload.id -> {
                listPopupWindow?.isOutsideTouchable = true
                listPopupWindow?.isFocusable = true
                listPopupWindow?.showAsDropDown(binding.llHistoryAction.ivReload)
            }

            binding.llHistoryAction.ivPrevious.id -> {
                if (connectivityManager.isNetworkAvailable()) {
                    getPreviousItemToCurrent()
                } else {
                    showErrorDialog(
                        getString(R.string.error),
                        getString(R.string.no_internet_error),
                    )
                }
            }

            binding.llHistoryAction.ivNext.id -> {
                if (connectivityManager.isNetworkAvailable()) {
                    getNextItemToCurrent()
                } else {
                    showErrorDialog(
                        getString(R.string.error),
                        getString(R.string.no_internet_error),
                    )
                }
            }

            binding.retryButtonBp.id -> {
                handleRetry()
            }
        }
    }

    private fun handleRetry() {
        if (connectivityManager.isNetworkAvailable()) {
            if (!viewModel.medicalTicketId.isNullOrBlank()) {
                getPatientId()
                    ?.takeIf { it.isNotBlank() }
                    ?.let {
                        viewModel.getMedicalReviewHistory(
                            patientId = it,
                            medicalTicketId = viewModel.medicalTicketId,
                        )
                    }
            } else {
                getInitialReferralTickets()
            }
        } else {
            showErrorDialog(getString(R.string.error), getString(R.string.no_internet_error))
        }
    }

    private fun checkForPreviousItem(): Int {
        var selectedIndex = -1
        viewModel.medicalReferralDates.value?.let {
            it.forEachIndexed { index, referredDate ->
                if (referredDate.id == viewModel.medicalTicketId) {
                    selectedIndex = index - 1
                }
            }
        }
        return selectedIndex
    }

    private fun checkNextItem(): Int {
        var selectedIndex = -1
        viewModel.medicalReferralDates.value?.let {
            it.forEachIndexed { index, referredDate ->
                if ((referredDate.id == viewModel.medicalTicketId) && ((index + 1) < it.size)) {
                    selectedIndex = index + 1
                }
            }
        }
        return selectedIndex
    }

    private fun getPreviousItemToCurrent() {
        val selectedIndex = checkForPreviousItem()
        if (selectedIndex != -1) {
            viewModel.medicalReferralDates.value?.get(selectedIndex)?.id?.let {
                viewModel.getMedicalReviewHistory(
                    patientId = getPatientId(),
                    medicalTicketId = it,
                )
                viewModel.medicalTicketId = it
            }
        }
    }

    private fun getNextItemToCurrent() {
        val selectedIndex = checkNextItem()
        if (selectedIndex != -1) {
            viewModel.medicalReferralDates.value?.get(selectedIndex)?.id?.let {
                viewModel.getMedicalReviewHistory(
                    patientId = getPatientId(),
                    medicalTicketId = it,
                )
                viewModel.medicalTicketId = it
            }
        }
    }

    private fun calculateDateTime(
        dateTime: String,
        isDate: Boolean,
    ): String? {
        val inputFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        val dateTime1 = ZonedDateTime.parse(dateTime, inputFormatter)
        val timeFormatter: DateTimeFormatter = if (isDate) {
            DateTimeFormatter.ofPattern(DateUtils.DATE_ddMMyyyy)
        } else {
            DateTimeFormatter.ofPattern(DateUtils.TIME_FORMAT_hhmma)
        }
        return dateTime1.format(timeFormatter)
    }

    private fun formatEligibility(
        eligibility: Eligibility,
        otherPopulationType: String? = null,
    ): String =
        buildString {
            // Handle Symptoms
            eligibility.Symptoms?.takeIf { it.isNotEmpty() }?.let { symptoms ->
                val label = "${getString(R.string.symptoms)} - "
                append(label)
                val padding = " ".repeat(8)
                symptoms.forEach { symptom ->
                    append("\n$padding$symptom")
                }
                append("\n") // Add a line break between sections
            }

            eligibility.hivPopulationType?.takeIf { it.isNotEmpty() }?.let { types ->
                val label = "${getString(R.string.population_type)} - "
                append(label)
                val padding = " ".repeat(8)
                types.forEach { type ->
                    val line = if (type.equals(DefinedParams.OTHER, ignoreCase = true) &&
                        !otherPopulationType.isNullOrBlank()
                    ) {
                        "$type - ${otherPopulationType.trim()}"
                    } else {
                        type
                    }
                    append("\n$padding$line")
                }
            }
        }.trim()

    private fun getEmtctScreening(medicalReviewHistory: MedicalReviewHistory): List<Map<String, Any?>> {
        val entryPoint = medicalReviewHistory.reviewDetails?.entryPoint?.takeIf { it.isNotBlank() }?.let { entry ->
            if (entry.equals(DefinedParams.OTHER, ignoreCase = true) &&
                !medicalReviewHistory.reviewDetails.otherEntryPoint.isNullOrBlank()
            ) {
                "$entry - ${medicalReviewHistory.reviewDetails.otherEntryPoint}"
            } else {
                entry
            }
        } ?: getString(R.string.separator_double_hyphen)

        return listOf(
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.diagnosis_tb),
                Value to combineText(
                    medicalReviewHistory.reviewDetails
                        ?.diagnosis
                        ?.filter {
                            it.diseaseCategory?.lowercase() != OTHER_NOTES.lowercase()
                        }?.map { it.diseaseCategory }
                        ?.distinct(),
                    "",
                    getString(R.string.separator_double_hyphen),
                ),
            ),
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.patient_status),
                Value to (
                    medicalReviewHistory.reviewDetails
                        ?.patientStatus
                        ?.takeIf { it.isNotBlank() }
                        ?.let { requireContext().changePatientStatus(it) }
                        ?: getString(R.string.separator_double_hyphen)
                ),
            ),
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.date_of_review),
                Value to medicalReviewHistory.dateOfReview?.let {
                    DateUtils.convertDateFormat(
                        it,
                        DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                        DateUtils.DATE_ddMMyyyy,
                    )
                },
            ),
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.eligibility),
                Value to (
                    medicalReviewHistory.reviewDetails
                        ?.eligibilities
                        ?.let {
                            formatEligibility(it, otherPopulationType = medicalReviewHistory.reviewDetails.otherPopulationType)
                        }.takeIf { !it.isNullOrBlank() }
                        ?: getString(R.string.separator_double_hyphen)
                ),
            ),
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.hbsag_test),
                Value to (
                    medicalReviewHistory.reviewDetails?.hbsAGTest?.takeIf { it.isNotBlank() }
                        ?: getString(R.string.separator_double_hyphen)
                ),
            ),
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.a1_test_result),
                Value to (
                    medicalReviewHistory.reviewDetails?.a1TestResult?.takeIf { it.isNotBlank() }
                        ?: getString(R.string.separator_double_hyphen)
                ),
            ),
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.a2_test_result),
                Value to (
                    medicalReviewHistory.reviewDetails?.a2TestResult?.takeIf { it.isNotBlank() }
                        ?: getString(R.string.separator_double_hyphen)
                ),
            ),
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.entry_point),
                Value to entryPoint,
            ),
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.clinical_notes),
                Value to (
                    medicalReviewHistory.reviewDetails?.clinicalNotes?.takeIf { it.isNotBlank() }
                        ?: getString(R.string.separator_double_hyphen)
                ),
            ),
        )
    }

    private fun getHivScreening(medicalReviewHistory: MedicalReviewHistory): List<Map<String, Any?>> {
        val entryPoint = medicalReviewHistory.reviewDetails?.entryPoint?.takeIf { it.isNotBlank() }?.let { entry ->
            if (entry.equals(DefinedParams.OTHER, ignoreCase = true) &&
                !medicalReviewHistory.reviewDetails.otherEntryPoint.isNullOrBlank()
            ) {
                "$entry - ${medicalReviewHistory.reviewDetails.otherEntryPoint}"
            } else {
                entry
            }
        } ?: getString(R.string.separator_double_hyphen)
        return listOf(
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.diagnosis_tb),
                Value to combineText(
                    medicalReviewHistory.reviewDetails
                        ?.diagnosis
                        ?.filter {
                            it.diseaseCategory?.lowercase() != OTHER_NOTES.lowercase()
                        }?.map { it.diseaseCategory }
                        ?.distinct(),
                    "",
                    getString(R.string.separator_double_hyphen),
                ),
            ),
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.patient_status),
                Value to (
                    medicalReviewHistory.reviewDetails
                        ?.patientStatus
                        ?.takeIf { it.isNotBlank() }
                        ?.let { requireContext().changePatientStatus(it) }
                        ?: getString(R.string.separator_double_hyphen)
                ),
            ),
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.date_of_review),
                Value to medicalReviewHistory.dateOfReview?.let {
                    DateUtils.convertDateFormat(
                        it,
                        DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                        DateUtils.DATE_ddMMyyyy,
                    )
                },
            ),
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.eligibility),
                Value to (
                    medicalReviewHistory.reviewDetails
                        ?.eligibilities
                        ?.let {
                            formatEligibility(it, otherPopulationType = medicalReviewHistory.reviewDetails.otherPopulationType)
                        }.takeIf { !it.isNullOrBlank() }
                        ?: getString(R.string.separator_double_hyphen)
                ),
            ),
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.a1_test_result),
                Value to (
                    medicalReviewHistory.reviewDetails?.a1TestResult?.takeIf { it.isNotBlank() }
                        ?: getString(R.string.separator_double_hyphen)
                ),
            ),
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.a2_test_result),
                Value to (
                    medicalReviewHistory.reviewDetails?.a2TestResult?.takeIf { it.isNotBlank() }
                        ?: getString(R.string.separator_double_hyphen)
                ),
            ),
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.a3_test_result),
                Value to (
                    medicalReviewHistory.reviewDetails?.a3TestResult?.takeIf { it.isNotBlank() }
                        ?: getString(R.string.separator_double_hyphen)
                ),
            ),
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.entry_point),
                Value to entryPoint,
            ),
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.clinical_notes),
                Value to (
                    medicalReviewHistory.reviewDetails?.clinicalNotes?.takeIf { it.isNotBlank() }
                        ?: getString(R.string.separator_double_hyphen)
                ),
            ),
        )
    }

    private fun getHivImrCmr(medicalReviewHistory: MedicalReviewHistory): List<Map<String, Any?>> {
        val hivSystemicExaminations =
            (medicalReviewHistory.reviewDetails?.systemicExaminations as? Map<String, String>)
                ?.toFormattedList()
                .takeIf { !it.isNullOrEmpty() }
                ?: emptyList()
        return listOf(
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.diagnosis_tb),
                Value to combineText(
                    medicalReviewHistory.reviewDetails
                        ?.diagnosis
                        ?.filter {
                            it.diseaseCategory?.lowercase() != OTHER_NOTES.lowercase()
                        }?.map { it.diseaseCategory }
                        ?.distinct(),
                    "",
                    getString(R.string.separator_double_hyphen),
                ),
            ),
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.patient_status),
                Value to (
                    medicalReviewHistory.reviewDetails
                        ?.patientStatus
                        ?.takeIf { it.isNotBlank() }
                        ?.let { requireContext().changePatientStatus(it) }
                        ?: getString(R.string.separator_double_hyphen)
                ),
            ),
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.date_of_review),
                Value to medicalReviewHistory.dateOfReview?.let {
                    DateUtils.convertDateFormat(
                        it,
                        DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                        DateUtils.DATE_ddMMyyyy,
                    )
                },
            ),
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.presenting_complaints),
                Value to combineText(
                    CommonUtils.convertAnyToListOfString(medicalReviewHistory.reviewDetails?.presentingComplaints),
                    medicalReviewHistory.reviewDetails?.presentingComplaintsNotes,
                    getString(R.string.separator_double_hyphen),
                ),
            ),
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.comorbidities_coinfections),
                Value to combineText(
                    medicalReviewHistory.reviewDetails?.comorbiditiesCoinfections,
                    medicalReviewHistory.reviewDetails?.comorbiditiesCoinfectionsNotes,
                    getString(R.string.separator_double_hyphen),
                ),
            ),
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.general_systemic_examinations),
                Value to combineText(
                    hivSystemicExaminations,
                    null,
                    getString(R.string.separator_double_hyphen),
                ),
            ),
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.clinical_notes),
                Value to (
                    medicalReviewHistory.reviewDetails?.clinicalNotes?.takeIf { it.isNotBlank() }
                        ?: getString(R.string.separator_double_hyphen)
                ),
            ),
        )
    }

    fun getEmtctHivMr(medicalReviewHistory: MedicalReviewHistory): List<Map<String, Any?>> {
        val hivSystemicExaminations =
            (medicalReviewHistory.reviewDetails?.systemicExaminations as? Map<String, String>)
                ?.toFormattedList()
                .takeIf { !it.isNullOrEmpty() }
                ?: emptyList()
        return listOf(
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.diagnosis_tb),
                Value to combineText(
                    medicalReviewHistory.reviewDetails
                        ?.diagnosis
                        ?.filter {
                            it.diseaseCategory?.lowercase() != OTHER_NOTES.lowercase()
                        }?.map { it.diseaseCategory }
                        ?.distinct(),
                    "",
                    getString(R.string.separator_double_hyphen),
                ),
            ),
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.patient_status),
                Value to (
                    medicalReviewHistory.reviewDetails
                        ?.patientStatus
                        ?.takeIf { it.isNotBlank() }
                        ?.let { requireContext().changePatientStatus(it) }
                        ?: getString(R.string.separator_double_hyphen)
                ),
            ),
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.date_of_review),
                Value to medicalReviewHistory.dateOfReview?.let {
                    DateUtils.convertDateFormat(
                        it,
                        DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                        DateUtils.DATE_ddMMyyyy,
                    )
                },
            ),
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.presenting_complaints),
                Value to combineText(
                    CommonUtils.convertAnyToListOfString(medicalReviewHistory.reviewDetails?.presentingComplaints),
                    medicalReviewHistory.reviewDetails?.presentingComplaintsNotes,
                    getString(R.string.separator_double_hyphen),
                ),
            ),
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.obstetric_examination),
                Value to combineText(
                    medicalReviewHistory.reviewDetails?.obstetricExaminations,
                    medicalReviewHistory.reviewDetails?.obstetricExaminationNotes,
                    getString(R.string.separator_double_hyphen),
                ),
            ),
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.general_systemic_examinations),
                Value to combineText(
                    hivSystemicExaminations,
                    null,
                    getString(R.string.separator_double_hyphen),
                ),
            ),
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.clinical_notes),
                Value to (
                    medicalReviewHistory.reviewDetails?.clinicalNotes?.takeIf { it.isNotBlank() }
                        ?: getString(R.string.separator_double_hyphen)
                ),
            ),
        )
    }

    private fun getFamilyPlanning(medicalReviewHistory: MedicalReviewHistory): List<Map<String, Any?>> {
        val progestin = listOfNotNull(
            medicalReviewHistory.reviewDetails
                ?.contraceptive
                ?.progestinOnlyOrals
                ?.takeIf { it.isNotBlank() },
            medicalReviewHistory.reviewDetails
                ?.contraceptive
                ?.otherProgestinOnlyOrals
                ?.takeIf { it.isNotBlank() },
        ).joinToString("-")

        return listOf(
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.patient_status),
                Value to (
                    medicalReviewHistory.reviewDetails
                        ?.patientStatus
                        ?.takeIf { it.isNotBlank() }
                        ?.let { requireContext().changePatientStatus(it) }
                        ?: getString(R.string.separator_double_hyphen)
                ),
            ),
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.date_of_review),
                Value to medicalReviewHistory.dateOfReview?.let {
                    DateUtils.convertDateFormat(
                        it,
                        DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                        DateUtils.DATE_ddMMyyyy,
                    )
                },
            ),
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.client_type),
                Value to (
                    medicalReviewHistory.reviewDetails
                        ?.contraceptive
                        ?.clientType
                        ?.takeIf { it.isNotBlank() }
                        ?: getString(R.string.separator_double_hyphen)
                ),
            ),
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.post_partum),
                Value to (
                    medicalReviewHistory.reviewDetails
                        ?.contraceptive
                        ?.postPartum
                        ?.takeIf { it.isNotBlank() }
                        ?: getString(R.string.separator_double_hyphen)
                ),
            ),
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.progestin_only_orals),
                Value to (
                    progestin.takeIf { it.isNotBlank() }
                        ?: getString(R.string.separator_double_hyphen)
                ),
            ),
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.quantity_microlut),
                Value to (
                    medicalReviewHistory.reviewDetails
                        ?.contraceptive
                        ?.microlutQuantity
                        ?.toString()
                        ?: getString(R.string.separator_double_hyphen)
                ),
            ),
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.clinical_notes),
                Value to (
                    medicalReviewHistory.reviewDetails?.clinicalNotes?.takeIf { it.isNotBlank() }
                        ?: getString(R.string.separator_double_hyphen)
                ),
            ),
        )
    }

    private fun createMedicalReview(medicalReviewHistory: MedicalReviewHistory): List<Map<String, Any?>> {
        if (medicalReviewHistory.type == EMTCT_HIV_MEDICAL_SCREENING) {
            return getEmtctScreening(medicalReviewHistory)
        }
        if (medicalReviewHistory.type == HIV_MEDICAL_SCREENING) {
            return getHivScreening(medicalReviewHistory)
        }
        if (medicalReviewHistory.type == MedicalReviewTypeEnums.HIV_MEDICAL_REVIEW.name) {
            return getHivImrCmr(medicalReviewHistory)
        }
        if (medicalReviewHistory.type == EMTCT_HIV_MEDICAL_REVIEW) {
            return getEmtctHivMr(medicalReviewHistory)
        }
        if (medicalReviewHistory.type == MedicalReviewTypeEnums.FAMILY_PLANNING_REVIEW.name) {
            return getFamilyPlanning(medicalReviewHistory)
        }
        if (medicalReviewHistory.type == DefinedParams.TB_REVIEW) {
            val chipList = (medicalReviewHistory.reviewDetails?.systemicExaminations as? List<*>)
                ?.mapNotNull { item ->
                    (item as? LinkedTreeMap<*, *>)?.let { map ->
                        ChipResponse(
                            name = map[NAME] as? String,
                            value = map[Value] as? String,
                        )
                    }
                }.orEmpty()

            val respiratoryTextList = chipList.mapNotNull { chip ->
                chip.name?.let { name ->
                    if (name.equals(MedicalReviewDefinedParams.respiratory, ignoreCase = true)) {
                        if (!chip.value.isNullOrBlank()) "$name : ${chip.value}" else name
                    } else {
                        name
                    }
                }
            }

            return listOf(
                mapOf(
                    DefinedParams.LABEL to requireContext().getString(R.string.diagnosis_tb),
                    Value to combineText(
                        medicalReviewHistory.reviewDetails
                            ?.diagnosis
                            ?.filter {
                                it.diseaseCategory?.lowercase() != OTHER_NOTES.lowercase() &&
                                    (it.type.equals(TB, true) || it.type.isNullOrBlank())
                            }?.map { it.diseaseCategory }
                            ?.distinct(),
                        "",
                        getString(R.string.separator_double_hyphen),
                    ),
                ),
                mapOf(
                    DefinedParams.LABEL to requireContext().getString(R.string.patient_status),
                    Value to (
                        medicalReviewHistory.reviewDetails
                            ?.patientStatus
                            ?.takeIf { it.isNotBlank() }
                            ?.let { requireContext().changePatientStatus(it) }
                            ?: getString(R.string.separator_double_hyphen)
                    ),
                ),
                mapOf(
                    DefinedParams.LABEL to requireContext().getString(R.string.date_of_review),
                    Value to medicalReviewHistory.dateOfReview?.let {
                        DateUtils.convertDateFormat(
                            it,
                            DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                            DateUtils.DATE_ddMMyyyy,
                        )
                    },
                ),
                mapOf(
                    DefinedParams.LABEL to requireContext().getString(R.string.presenting_complaints),
                    Value to combineText(
                        CommonUtils.convertAnyToListOfString(medicalReviewHistory.reviewDetails?.presentingComplaints),
                        medicalReviewHistory.reviewDetails?.presentingComplaintsNotes,
                        getString(R.string.separator_double_hyphen),
                    ),
                ),
                mapOf(
                    DefinedParams.LABEL to requireContext().getString(R.string.comorbidities),
                    Value to combineText(
                        medicalReviewHistory.reviewDetails?.comorbidities,
                        medicalReviewHistory.reviewDetails?.comorbiditiesNotes,
                        getString(R.string.separator_double_hyphen),
                    ),
                ),
                mapOf(
                    DefinedParams.LABEL to requireContext().getString(R.string.general_systemic_examinations),
                    Value to combineText(
                        respiratoryTextList,
                        medicalReviewHistory.reviewDetails?.systemicExaminationsNotes,
                        getString(R.string.separator_double_hyphen),
                    ),
                ),
                mapOf(
                    DefinedParams.LABEL to requireContext().getString(R.string.clinical_notes),
                    Value to (
                        medicalReviewHistory.reviewDetails?.clinicalNotes?.takeIf { it.isNotBlank() }
                            ?: getString(R.string.separator_double_hyphen)
                    ),
                ),
            )
        }

        if (medicalReviewHistory.type == DefinedParams.IMMUNIZATION) {
            val epiFields = listOf(
                mapOf(
                    DefinedParams.LABEL to requireContext().getString(R.string.date_of_review),
                    Value to medicalReviewHistory.dateOfReview?.let {
                        DateUtils.convertDateFormat(
                            it,
                            DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                            DateUtils.DATE_ddMMyyyy,
                        )
                    },
                ),
                mapOf(
                    DefinedParams.LABEL to requireContext().getString(R.string.vaccination_taken),
                    Value to (
                        medicalReviewHistory.reviewDetails?.vaccinated?.joinToString(separator = ", ")
                            ?: getString(R.string.separator_double_hyphen)
                    ),
                ),
                mapOf(
                    DefinedParams.LABEL to requireContext().getString(R.string.scheduled_date),
                    Value to getVaccineScheduledDateValues(
                        medicalReviewHistory.reviewDetails?.lastScheduledDate,
                        medicalReviewHistory.reviewDetails?.lastScheduledDateReason,
                    ),
                ),
                mapOf(
                    DefinedParams.LABEL to requireContext().getString(R.string.next_vaccination_duration),
                    Value to (
                        medicalReviewHistory.reviewDetails?.nextVaccinationDuration
                            ?: getString(R.string.separator_double_hyphen)
                    ),
                ),
                mapOf(
                    DefinedParams.LABEL to requireContext().getString(R.string.next_vaccination_dose),
                    Value to (
                        medicalReviewHistory.reviewDetails?.nextVaccinationDose?.joinToString(separator = ", ")
                            ?: getString(R.string.separator_double_hyphen)
                    ),
                ),
                mapOf(
                    DefinedParams.LABEL to requireContext().getString(R.string.next_vaccination_date),
                    Value to (
                        medicalReviewHistory.reviewDetails?.nextVaccinationDate?.let {
                            DateUtils.convertDateFormat(
                                it,
                                DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                                DateUtils.DATE_ddMMyyyy,
                            )
                        } ?: getString(R.string.separator_double_hyphen)
                    ),
                ),
            )
            return epiFields
        } else {
            val commonFields = listOf(
                mapOf(
                    DefinedParams.LABEL to requireContext().getString(R.string.diagnosis),
                    Value to combineText(
                        medicalReviewHistory.reviewDetails
                            ?.diagnosis
                            ?.filter { it.diseaseCategory?.lowercase() != OTHER_NOTES.lowercase() }
                            ?.map { it.diseaseCategory }
                            ?.distinct(),
                        "",
                        getString(R.string.separator_double_hyphen),
                    ),
                ),
                mapOf(
                    DefinedParams.LABEL to requireContext().getString(R.string.patient_status),
                    Value to (
                        medicalReviewHistory.reviewDetails
                            ?.patientStatus
                            ?.takeIf { it.isNotBlank() }
                            ?.let { requireContext().changePatientStatus(it) }
                            ?: getString(R.string.separator_double_hyphen)
                    ),
                ),
                mapOf(
                    DefinedParams.LABEL to requireContext().getString(R.string.date_of_review),
                    Value to medicalReviewHistory.dateOfReview?.let {
                        DateUtils.convertDateFormat(
                            it,
                            DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                            DateUtils.DATE_ddMMyyyy,
                        )
                    },
                ),
                mapOf(
                    DefinedParams.LABEL to requireContext().getString(R.string.presenting_complaints),
                    Value to combineText(
                        CommonUtils.convertAnyToListOfString(medicalReviewHistory.reviewDetails?.presentingComplaints),
                        medicalReviewHistory.reviewDetails?.presentingComplaintsNotes,
                        getString(R.string.separator_double_hyphen),
                    ),
                ),
                mapOf(
                    DefinedParams.LABEL to requireContext().getString(R.string.clinical_notes),
                    Value to (
                        medicalReviewHistory.reviewDetails?.clinicalNotes?.takeIf { it.isNotBlank() }
                            ?: getString(R.string.separator_double_hyphen)
                    ),
                ),
            )
            val labourDeliveryNeonate = when (medicalReviewHistory.type?.lowercase()) {
                MOTHER_DELIVERY_REVIEW.lowercase() -> {
                    listOf(
                        mapOf(
                            DefinedParams.LABEL to requireContext().getString(R.string.patient_status),
                            Value to (
                                medicalReviewHistory.reviewDetails
                                    ?.patientStatus
                                    ?.takeIf { it.isNotBlank() }
                                    ?.let { requireContext().changePatientStatus(it) }
                                    ?: getString(R.string.separator_double_hyphen)
                            ),
                        ),
                        mapOf(
                            DefinedParams.LABEL to requireContext().getString(R.string.date_of_review),
                            Value to medicalReviewHistory.dateOfReview?.let {
                                DateUtils.convertDateFormat(
                                    it,
                                    DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                                    DateUtils.DATE_ddMMyyyy,
                                )
                            },
                        ),
                        mapOf(
                            DefinedParams.LABEL to requireContext().getString(R.string.date_of_delivery),
                            Value to (
                                medicalReviewHistory.reviewDetails?.labourDTO?.dateAndTimeOfDelivery?.let {
                                    calculateDateTime(
                                        it,
                                        true,
                                    )
                                } ?: getString(R.string.separator_double_hyphen)
                            ),
                        ),
                        mapOf(
                            DefinedParams.LABEL to requireContext().getString(R.string.date_of_labour_onset),
                            Value to (
                                medicalReviewHistory.reviewDetails?.labourDTO?.dateAndTimeOfLabourOnset?.let {
                                    calculateDateTime(
                                        it,
                                        true,
                                    )
                                } ?: getString(R.string.separator_double_hyphen)
                            ),
                        ),
                        mapOf(
                            DefinedParams.LABEL to requireContext().getString(R.string.delivery_by),
                            Value to (
                                medicalReviewHistory.reviewDetails
                                    ?.labourDTO
                                    ?.deliveryBy
                                    ?.takeIf { it.isNotBlank() } ?: getString(R.string.separator_double_hyphen)
                            ),
                        ),
                        mapOf(
                            DefinedParams.LABEL to requireContext().getString(R.string.delivery_type),
                            Value to (
                                medicalReviewHistory.reviewDetails
                                    ?.labourDTO
                                    ?.deliveryType
                                    ?.takeIf { it.isNotBlank() } ?: getString(R.string.separator_double_hyphen)
                            ),
                        ),
                        mapOf(
                            DefinedParams.LABEL to requireContext().getString(R.string.delivery_at),
                            Value to (
                                medicalReviewHistory.reviewDetails
                                    ?.labourDTO
                                    ?.deliveryAt
                                    ?.takeIf { it.isNotBlank() } ?: getString(R.string.separator_double_hyphen)
                            ),
                        ),
                        mapOf(
                            DefinedParams.LABEL to requireContext().getString(R.string.delivery_status),
                            Value to (
                                medicalReviewHistory.reviewDetails
                                    ?.labourDTO
                                    ?.deliveryStatus
                                    ?.takeIf { it.isNotBlank() } ?: getString(R.string.separator_double_hyphen)
                            ),
                        ),
                    )
                }
                NEONATE_BIRTH_REVIEW.lowercase() -> {
                    listOf(
                        mapOf(
                            DefinedParams.LABEL to requireContext().getString(R.string.patient_status),
                            Value to (
                                medicalReviewHistory.reviewDetails
                                    ?.patientStatus
                                    ?.takeIf { it.isNotBlank() }
                                    ?.let { requireContext().changePatientStatus(it) }
                                    ?: getString(R.string.separator_double_hyphen)
                            ),
                        ),
                        mapOf(
                            DefinedParams.LABEL to requireContext().getString(R.string.date_of_review),
                            Value to medicalReviewHistory.dateOfReview?.let {
                                DateUtils.convertDateFormat(
                                    it,
                                    DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                                    DateUtils.DATE_ddMMyyyy,
                                )
                            },
                        ),
                        mapOf(
                            DefinedParams.LABEL to requireContext().getString(R.string.neonateOutcome),
                            Value to (
                                medicalReviewHistory.reviewDetails
                                    ?.neonateOutcome
                                    ?.takeIf { it.isNotBlank() } ?: getString(R.string.separator_double_hyphen)
                            ),
                        ),
                        mapOf(
                            DefinedParams.LABEL to requireContext().getString(R.string.stateOfBaby),
                            Value to
                                (medicalReviewHistory.reviewDetails?.stateOfBaby ?.takeIf { it.isNotBlank() } ?: getString(R.string.separator_double_hyphen)),
                        ),
                        mapOf(
                            DefinedParams.LABEL to requireContext().getString(R.string.signs_Symptoms_observed),
                            Value to combineText(
                                medicalReviewHistory.reviewDetails?.signs,
                                null,
                                getString(R.string.separator_double_hyphen),
                            ),
                        ),
                    )
                }
                else -> {
                    commonFields
                }
            }

            // TODO Please note: Change the spelling of 'systemicExaminations'
            //  consistently throughout the project. This may require effort
            //  and could affect past data fetching, as it currently varies in some requests and responses
            // after backend change this we need to change variables name

            val additionalFields = when (medicalReviewHistory.type?.lowercase()) {
                ABOVE_5_MEDICAL_REVIEW.lowercase() -> mapOf(
                    DefinedParams.LABEL to requireContext().getString(R.string.systemic_examinations),
                    Value to combineText(
                        (medicalReviewHistory.reviewDetails?.systemicExaminations as? List<*>)?.filterIsInstance<String?>(),
                        medicalReviewHistory.reviewDetails?.systemicExaminationsNotes,
                        getString(R.string.separator_double_hyphen),
                    ),
                )

                ICCM_ABOVE_2M_5Y.lowercase() -> mapOf(
                    DefinedParams.LABEL to requireContext().getString(R.string.systemic_examinations),
                    Value to combineText(
                        medicalReviewHistory.reviewDetails?.systemicExamination,
                        medicalReviewHistory.reviewDetails?.systemicExaminationNotes,
                        getString(R.string.separator_double_hyphen),
                    ),
                )

                PregnancyAncMedicalReview.lowercase() -> mapOf(
                    DefinedParams.LABEL to requireContext().getString(R.string.obstetric_examination),
                    Value to combineText(
                        medicalReviewHistory.reviewDetails?.obstetricExaminations,
                        medicalReviewHistory.reviewDetails?.obstetricExaminationNotes,
                        getString(R.string.separator_double_hyphen),
                    ),
                )
                else -> null
            }
            return labourDeliveryNeonate + listOfNotNull(additionalFields)
        }
    }

    private fun getVaccineScheduledDateValues(
        lastScheduledDate: String?,
        statusReason: String?,
    ): Spannable {
        val onTime = "On Time"
        lastScheduledDate?.let { dateNotNull ->
            val date = DateUtils.convertDateFormat(
                dateNotNull,
                DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                DateUtils.DATE_ddMMyyyy,
            )

            statusReason?.let { status ->
                val fullText = "$date, ($status)"
                val spannable = SpannableString(fullText)

                val color = if (status.equals(onTime, true)) {
                    ContextCompat.getColor(requireContext(), R.color.green_attention_color)
                } else {
                    ContextCompat.getColor(requireContext(), R.color.epi_missed_primary)
                }

                val startIndex = fullText.indexOf("($status)")
                val endIndex = startIndex + status.length + 2 // +2 for parentheses

                if (startIndex != -1) {
                    spannable.setSpan(ForegroundColorSpan(color), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }

                return spannable
            } ?: kotlin.run {
                return SpannableString(date)
            }
        }

        return SpannableString(getString(R.string.separator_double_hyphen))
    }
}
