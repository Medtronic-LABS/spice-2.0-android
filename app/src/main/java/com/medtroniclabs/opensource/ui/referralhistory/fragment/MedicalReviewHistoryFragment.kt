package com.medtroniclabs.opensource.ui.referralhistory.fragment

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.changePatientStatus
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.CommonUtils.combineText
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.DefinedParams.Above5MedicalReview
import com.medtroniclabs.opensource.common.DefinedParams.ICCM_ABOVE_2M_5Y
import com.medtroniclabs.opensource.common.DefinedParams.MotherDeliveryReview
import com.medtroniclabs.opensource.common.DefinedParams.Neonate_Birth_Review
import com.medtroniclabs.opensource.common.DefinedParams.OtherNotes
import com.medtroniclabs.opensource.common.DefinedParams.PregnancyAncMedicalReview
import com.medtroniclabs.opensource.data.history.MedicalReviewHistory
import com.medtroniclabs.opensource.databinding.FragmentReferralTicketBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.model.ReferredDate
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseFragment
import com.medtroniclabs.opensource.ui.mypatients.adapter.DateListAdapter
import com.medtroniclabs.opensource.ui.referralhistory.adapter.ReferralHistoryAdapter
import com.medtroniclabs.opensource.ui.referralhistory.viewmodel.ReferralHistoryViewModel
import dagger.hilt.android.AndroidEntryPoint
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
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentReferralTicketBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        const val TAG = "MedicalReviewHistoryFragment"
        fun newInstance(): MedicalReviewHistoryFragment {
            return MedicalReviewHistoryFragment()
        }

        fun newInstance(patientId: String?): MedicalReviewHistoryFragment {
            val fragment = MedicalReviewHistoryFragment()
            val bundle = Bundle()
            bundle.putString(DefinedParams.FhirId, patientId)
            fragment.arguments = bundle
            return fragment
        }
    }

    private fun getPatientId(): String? {
        return arguments?.getString(DefinedParams.FhirId, "")
    }

    private fun getInitialReferralTickets() {
        getPatientId()?.takeIf { it.isNotBlank() }
            ?.let { viewModel.getMedicalReviewHistory(patientId = it) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
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
                DividerItemDecoration.VERTICAL
            )
        )
        dateListAdapter =
            DateListAdapter { referred ->
                if (connectivityManager.isNetworkAvailable()) {
                    viewModel.getMedicalReviewHistory(
                        patientId = getPatientId(),
                        medicalTicketId = referred.id
                    )
                    viewModel.medicalTicketId = referred.id
                } else {
                    showErrorDialog(
                        getString(R.string.error),
                        getString(R.string.no_internet_error)
                    )
                }
                listPopupWindow?.dismiss()
            }
        recyclerView.adapter = dateListAdapter
        listPopupWindow = PopupWindow(
            view,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
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

    private fun showLoading() {
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
            createMedicalReview(medicalReviewHistory)
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

    private fun setReferralDates(referredDates: List<ReferredDate>?, id: String?) {
        if (referredDates != null) {
            if (viewModel.medicalTicketId == null) {
                viewModel.medicalTicketId = id
                viewModel.medicalReferralDates.value = referredDates
            }
            viewModel.medicalReferralDates.value?.let {list ->
                if (referredDates.size > list.size){
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
                        getString(R.string.no_internet_error)
                    )
                }
            }

            binding.llHistoryAction.ivNext.id -> {
                if (connectivityManager.isNetworkAvailable()) {
                    getNextItemToCurrent()
                } else {
                    showErrorDialog(
                        getString(R.string.error),
                        getString(R.string.no_internet_error)
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
                getPatientId()?.takeIf { it.isNotBlank() }
                    ?.let {
                        viewModel.getMedicalReviewHistory(
                            patientId = it,
                            medicalTicketId = viewModel.medicalTicketId
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
                    medicalTicketId = it
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
                    medicalTicketId = it
                )
                viewModel.medicalTicketId = it
            }
        }
    }
    private fun calculateDateTime(dateTime: String, isDate: Boolean): String? {
        val inputFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        val dateTime1 = ZonedDateTime.parse(dateTime, inputFormatter)
        val timeFormatter: DateTimeFormatter = if (isDate) {
            DateTimeFormatter.ofPattern(DateUtils.DATE_ddMMyyyy)
        } else {
            DateTimeFormatter.ofPattern(DateUtils.TIME_FORMAT_hhmma)
        }
        return dateTime1.format(timeFormatter)
    }


    private fun createMedicalReview(medicalReviewHistory: MedicalReviewHistory): List<Map<String, String?>> {
        val commonFields = listOf(
            mapOf(
                DefinedParams.label to requireContext().getString(R.string.diagnosis),
                DefinedParams.Value to combineText(
                    medicalReviewHistory.reviewDetails?.diagnosis?.filter { it.diseaseCategory?.lowercase() != OtherNotes.lowercase() }
                        ?.map { it.diseaseCategory }?.distinct(),
                    "",
                    getString(R.string.separator_double_hyphen)
                )
            ),
            mapOf(
                DefinedParams.label to requireContext().getString(R.string.patient_status),
                DefinedParams.Value to (medicalReviewHistory.reviewDetails?.patientStatus?.takeIf { it.isNotBlank() }
                    ?.let { requireContext().changePatientStatus(it) }
                    ?: getString(R.string.separator_double_hyphen))
            ),
            mapOf(
                DefinedParams.label to requireContext().getString(R.string.date_of_review),
                DefinedParams.Value to medicalReviewHistory.dateOfReview?.let {
                    DateUtils.convertDateFormat(
                        it,
                        DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                        DateUtils.DATE_ddMMyyyy
                    )
                }
            ),
            mapOf(
                DefinedParams.label to requireContext().getString(R.string.presenting_complaints),
                DefinedParams.Value to combineText(
                    CommonUtils.convertAnyToListOfString(medicalReviewHistory.reviewDetails?.presentingComplaints),
                    medicalReviewHistory.reviewDetails?.presentingComplaintsNotes,
                    getString(R.string.separator_double_hyphen)
                )
            ),
            mapOf(
                DefinedParams.label to requireContext().getString(R.string.clinical_notes),
                DefinedParams.Value to (medicalReviewHistory.reviewDetails?.clinicalNotes?.takeIf { it.isNotBlank() }
                    ?: getString(R.string.separator_double_hyphen))
            )
        )
        val labourDeliveryNeonate= when (medicalReviewHistory.type?.lowercase()) {
            MotherDeliveryReview.lowercase() -> {
                listOf(
                    mapOf(
                        DefinedParams.label to requireContext().getString(R.string.patient_status),
                        DefinedParams.Value to (medicalReviewHistory.reviewDetails?.patientStatus?.takeIf { it.isNotBlank() }
                            ?.let { requireContext().changePatientStatus(it) }
                            ?: getString(R.string.separator_double_hyphen))
                    ),
                    mapOf(
                        DefinedParams.label to requireContext().getString(R.string.date_of_review),
                        DefinedParams.Value to medicalReviewHistory.dateOfReview?.let {
                            DateUtils.convertDateFormat(
                                it,
                                DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                                DateUtils.DATE_ddMMyyyy
                            )
                        }
                    ),
                    mapOf(
                        DefinedParams.label to requireContext().getString(R.string.date_of_delivery),
                        DefinedParams.Value to (medicalReviewHistory.reviewDetails?.labourDTO?.dateAndTimeOfDelivery?.let {
                            calculateDateTime(
                                it,
                                true
                            )}?: getString(R.string.separator_double_hyphen))

                    ), mapOf(
                        DefinedParams.label to requireContext().getString(R.string.date_of_labour_onset),
                        DefinedParams.Value to (medicalReviewHistory.reviewDetails?.labourDTO?.dateAndTimeOfLabourOnset?.let {
                            calculateDateTime(
                                it,
                                true
                            )}?: getString(R.string.separator_double_hyphen))
                    ),
                    mapOf(
                        DefinedParams.label to requireContext().getString(R.string.delivery_by),
                        DefinedParams.Value to (medicalReviewHistory.reviewDetails?.labourDTO?.deliveryBy
                            ?.takeIf { it.isNotBlank() }?: getString(R.string.separator_double_hyphen))
                    ),
                    mapOf(
                        DefinedParams.label to requireContext().getString(R.string.delivery_type),
                        DefinedParams.Value to (medicalReviewHistory.reviewDetails?.labourDTO?.deliveryType
                            ?.takeIf { it.isNotBlank() }?: getString(R.string.separator_double_hyphen))
                    ),
                    mapOf(
                        DefinedParams.label to requireContext().getString(R.string.delivery_at),
                        DefinedParams.Value to (medicalReviewHistory.reviewDetails?.labourDTO?.deliveryAt
                            ?.takeIf { it.isNotBlank() }?: getString(R.string.separator_double_hyphen))
                    ),
                    mapOf(
                        DefinedParams.label to requireContext().getString(R.string.delivery_status),
                        DefinedParams.Value to (medicalReviewHistory.reviewDetails?.labourDTO?.deliveryStatus
                            ?.takeIf { it.isNotBlank() }?: getString(R.string.separator_double_hyphen))
                    )
                )
            }
            Neonate_Birth_Review.lowercase()->{
                listOf(
                    mapOf(
                        DefinedParams.label to requireContext().getString(R.string.patient_status),
                        DefinedParams.Value to (medicalReviewHistory.reviewDetails?.patientStatus?.takeIf { it.isNotBlank() }
                            ?.let { requireContext().changePatientStatus(it) }
                            ?: getString(R.string.separator_double_hyphen))
                    ),
                    mapOf(
                        DefinedParams.label to requireContext().getString(R.string.date_of_review),
                        DefinedParams.Value to medicalReviewHistory.dateOfReview?.let {
                            DateUtils.convertDateFormat(
                                it,
                                DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                                DateUtils.DATE_ddMMyyyy
                            )
                        }
                    ),mapOf(
                        DefinedParams.label to requireContext().getString(R.string.neonateOutcome),
                        DefinedParams.Value to (medicalReviewHistory.reviewDetails?.neonateOutcome
                            ?.takeIf { it.isNotBlank() }?: getString(R.string.separator_double_hyphen))
                    )
                    ,mapOf(
                        DefinedParams.label to requireContext().getString(R.string.stateOfBaby),
                        DefinedParams.Value to (medicalReviewHistory.reviewDetails?.stateOfBaby  ?.takeIf { it.isNotBlank() }?: getString(R.string.separator_double_hyphen))

                    )
                    ,mapOf(
                        DefinedParams.label to requireContext().getString(R.string.signs_Symptoms_observed),
                        DefinedParams.Value to combineText(medicalReviewHistory.reviewDetails?.signs,
                            null,getString(R.string.separator_double_hyphen))
                    )
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
            Above5MedicalReview.lowercase() -> mapOf(
                DefinedParams.label to requireContext().getString(R.string.systemic_examinations),
                DefinedParams.Value to combineText(
                    medicalReviewHistory.reviewDetails?.systemicExaminations,
                    medicalReviewHistory.reviewDetails?.systemicExaminationsNotes,
                    getString(R.string.separator_double_hyphen)
                )
            )

            ICCM_ABOVE_2M_5Y.lowercase() -> mapOf(
                DefinedParams.label to requireContext().getString(R.string.systemic_examinations),
                DefinedParams.Value to combineText(
                    medicalReviewHistory.reviewDetails?.systemicExamination,
                    medicalReviewHistory.reviewDetails?.systemicExaminationNotes,
                    getString(R.string.separator_double_hyphen)
                )
            )

            PregnancyAncMedicalReview.lowercase() -> mapOf(
                DefinedParams.label to requireContext().getString(R.string.obstetric_examination),
                DefinedParams.Value to combineText(
                    medicalReviewHistory.reviewDetails?.obstetricExaminations,
                    medicalReviewHistory.reviewDetails?.obstetricExaminationNotes,
                    getString(R.string.separator_double_hyphen)
                )
            )
            else -> null
        }
        return labourDeliveryNeonate + listOfNotNull(additionalFields)
    }
}