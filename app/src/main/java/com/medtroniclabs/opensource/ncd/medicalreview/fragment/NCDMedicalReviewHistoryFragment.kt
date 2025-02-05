package com.medtroniclabs.opensource.ncd.medicalreview.fragment

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
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.data.history.NCDMedicalReviewHistory
import com.medtroniclabs.opensource.data.history.PhysicalExaminations
import com.medtroniclabs.opensource.databinding.FragmentReferralTicketBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.model.ReferredDate
import com.medtroniclabs.opensource.ncd.medicalreview.viewmodel.NCDMedicalReviewCMRViewModel
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseFragment
import com.medtroniclabs.opensource.ui.mypatients.adapter.DateListAdapter
import com.medtroniclabs.opensource.ui.referralhistory.adapter.ReferralHistoryAdapter
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class NCDMedicalReviewHistoryFragment : BaseFragment(), View.OnClickListener {
    lateinit var binding: FragmentReferralTicketBinding
    private var listPopupWindow: PopupWindow? = null
    private lateinit var dateListAdapter: DateListAdapter
    private lateinit var adapters: ReferralHistoryAdapter
    val viewModel: NCDMedicalReviewCMRViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentReferralTicketBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        const val TAG = "NCDMedicalReviewHistoryFragment"
        fun newInstance(): NCDMedicalReviewHistoryFragment {
            return NCDMedicalReviewHistoryFragment()
        }

        fun newInstance(patientId: String?): NCDMedicalReviewHistoryFragment {
            val fragment = NCDMedicalReviewHistoryFragment()
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
            viewModel.medicalVisitId = null
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
                        medicalVisitId = referred.id
                    )
                    viewModel.medicalVisitId = referred.id
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
            ivReload.safeClickListener(this@NCDMedicalReviewHistoryFragment)
            ivNext.safeClickListener(this@NCDMedicalReviewHistoryFragment)
            ivPrevious.safeClickListener(this@NCDMedicalReviewHistoryFragment)
        }
    }

    private fun attachObservers() {
        viewModel.medicalReviewTicketLiveData.observe(viewLifecycleOwner) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    if (resource.data?.history.isNullOrEmpty() && viewModel.medicalVisitId.isNullOrBlank()) {
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

    private fun setReferralTicket(medicalReviewHistory: NCDMedicalReviewHistory) {
        binding.tvNoHistory.gone()
        adjustGuideline()
        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = adapters
        setReferralDates(medicalReviewHistory.history, medicalReviewHistory.patientVisitId)
        adapters.updateList(createMedicalReview(medicalReviewHistory))
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
            if (viewModel.medicalVisitId == null) {
                viewModel.medicalVisitId = id
                viewModel.medicalReferralDates.value = referredDates
            }
            viewModel.medicalReferralDates.value?.let { list ->
                if (referredDates.size > list.size) {
                    viewModel.medicalVisitId = id
                    viewModel.medicalReferralDates.value = referredDates
                }
            }
            viewModel.medicalReferralDates.value?.let {
                dateListAdapter.submitData(it, viewModel.medicalVisitId)
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
            if (!viewModel.medicalVisitId.isNullOrBlank()) {
                getPatientId()?.takeIf { it.isNotBlank() }
                    ?.let {
                        viewModel.getMedicalReviewHistory(
                            patientId = it,
                            medicalVisitId = viewModel.medicalVisitId
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
                if (referredDate.id == viewModel.medicalVisitId) {
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
                if ((referredDate.id == viewModel.medicalVisitId) && ((index + 1) < it.size)) {
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
                    medicalVisitId = it
                )
                viewModel.medicalVisitId = it
            }
        }
    }

    private fun getNextItemToCurrent() {
        val selectedIndex = checkNextItem()
        if (selectedIndex != -1) {
            viewModel.medicalReferralDates.value?.get(selectedIndex)?.id?.let {
                viewModel.getMedicalReviewHistory(
                    patientId = getPatientId(),
                    medicalVisitId = it
                )
                viewModel.medicalVisitId = it
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


    private fun createMedicalReview(medicalReviewHistory: NCDMedicalReviewHistory): List<Map<String, String?>> {
        val commonFields = listOf(
            mapOf(
                DefinedParams.label to requireContext().getString(R.string.date_of_review),
                DefinedParams.Value to (viewModel.medicalReferralDates.value?.firstOrNull { it.id == viewModel.medicalVisitId }?.date?.let {
                    DateUtils.convertDateFormat(
                        it,
                        DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                        DateUtils.DATE_ddMMyyyy
                    )
                } ?: getString(R.string.separator_double_hyphen))
            ),
            mapOf(
                DefinedParams.label to requireContext().getString(R.string.chief_complaints),
                DefinedParams.Value to CommonUtils.combineText(
                    CommonUtils.convertAnyToListOfString(medicalReviewHistory.medicalReview?.complaints),
                    "",
                    getString(R.string.separator_double_hyphen)
                )
            ),
            mapOf(
                DefinedParams.label to requireContext().getString(R.string.clinical_notes),
                DefinedParams.Value to CommonUtils.combineText(
                    CommonUtils.convertAnyToListOfString(medicalReviewHistory.medicalReview?.notes),
                    "",
                    getString(R.string.separator_double_hyphen)
                )
            ),
            mapOf(
                DefinedParams.label to requireContext().getString(R.string.examination),
                DefinedParams.Value to getPhysicalExamination(medicalReviewHistory.medicalReview?.physicalExams)
            )
        )
        return commonFields
    }

    private fun getPhysicalExamination(physicalExams: List<PhysicalExaminations>?): String {
        val returnStr = StringBuilder()
        physicalExams?.forEachIndexed { index, element ->
            val items = element.physicalExaminations?.joinToString(", ")
            if (!items.isNullOrBlank()) {
                returnStr.append(items)
            }
            if (!element.physicalExaminationsNote.isNullOrBlank())
                returnStr.append(" [${element.physicalExaminationsNote}]")
            if (index < physicalExams.size - 1)
                returnStr.append(", ")
        }

        return returnStr.toString()
    }
}