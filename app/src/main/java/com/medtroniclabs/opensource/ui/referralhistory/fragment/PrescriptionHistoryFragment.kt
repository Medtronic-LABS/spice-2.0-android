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
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.data.history.Prescription
import com.medtroniclabs.opensource.data.history.HistoryEntity
import com.medtroniclabs.opensource.databinding.FragmentReferralTicketBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.model.ReferredDate
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseFragment
import com.medtroniclabs.opensource.ui.mypatients.adapter.DateListAdapter
import com.medtroniclabs.opensource.ui.referralhistory.adapter.ReferralHistoryAdapter
import com.medtroniclabs.opensource.ui.referralhistory.viewmodel.ReferralHistoryViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PrescriptionHistoryFragment : BaseFragment(), View.OnClickListener {

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
        const val TAG = "PrescriptionHistoryFragment"
        fun newInstance(): PrescriptionHistoryFragment {
            return PrescriptionHistoryFragment()
        }

        fun newInstance(patientId: String?): PrescriptionHistoryFragment {
            val fragment = PrescriptionHistoryFragment()
            val bundle = Bundle()
            bundle.putString(DefinedParams.FhirId, patientId)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        attachObservers()
    }

    private fun getPatientId(): String? {
        return arguments?.getString(DefinedParams.FhirId, "")
    }

    private fun getInitialReferralTickets() {
        getPatientId()?.takeIf { it.isNotBlank() }
            ?.let { viewModel.getPrescriptionHistory(patientId = it) }
    }

    private fun initView() {
        binding.tvNoHistory.text = requireContext().getString(R.string.no_prescription_history)
        binding.tvReferralTicketTitle.text = requireContext().getString(R.string.prescription)
        if (getPatientId().isNullOrBlank()) {
            binding.tvNoHistory.visible()
            binding.llHistoryAction.ivNext.gone()
            binding.llHistoryAction.ivPrevious.gone()
            binding.llHistoryAction.ivReload.gone()
        } else {
            binding.tvNoHistory.gone()
            adapters = ReferralHistoryAdapter()
            viewModel.prescriptionTicketId = null
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
                    viewModel.getPrescriptionHistory(
                        patientId = getPatientId(),
                        prescriptionTicketId = referred.id
                    )
                    viewModel.prescriptionTicketId = referred.id
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
            ivReload.safeClickListener(this@PrescriptionHistoryFragment)
            ivNext.safeClickListener(this@PrescriptionHistoryFragment)
            ivPrevious.safeClickListener(this@PrescriptionHistoryFragment)
        }
    }

    private fun attachObservers() {
        viewModel.prescriptionTicketLiveData.observe(viewLifecycleOwner) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    if (resource.data?.history.isNullOrEmpty() && viewModel.prescriptionTicketId.isNullOrBlank()) {
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

    private fun setReferralTicket(prescriptionData: HistoryEntity) {
        binding.tvNoHistory.gone()
        with(DefinedParams) {
            adapters.updateList(
                listOf(
                    mapOf(
                        label to requireContext().getString(R.string.date_of_prescription),
                        this.Value to prescriptionData.dateOfReview?.let {
                            DateUtils.convertDateFormat(
                                it,
                                DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                                DateUtils.DATE_ddMMyyyy
                            )
                        }
                    ),
                    mapOf(
                        label to requireContext().getString(R.string.medication_prescribed),
                        this.Value to createPrescription(prescriptionData.prescriptions)
                    )
                )
            )
        }
        adjustGuideline()
        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = adapters
        setReferralDates(prescriptionData.history, prescriptionData.encounterId)
    }

    private fun createPrescription(prescriptions: List<Prescription>?): List<String>? {
        return prescriptions?.map { prescription ->
            "${prescription.medicationName} / ${prescription.frequencyName} / ${prescription.prescribedDays} ${
                dayPeriod(
                    prescription.prescribedDays
                )
            }"
        }
    }

    private fun dayPeriod(prescribedDays: Int?): String {
        return if (prescribedDays == 1){
            requireContext().getString(R.string.day)
        } else {
            requireContext().getString(R.string.days)
        }
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
            if (viewModel.prescriptionTicketId == null) {
                viewModel.prescriptionTicketId = id
                viewModel.prescriptionReferralDates.value = referredDates
            }
            viewModel.prescriptionReferralDates.value?.let {list ->
                if (referredDates.size > list.size){
                    viewModel.prescriptionTicketId = id
                    viewModel.prescriptionReferralDates.value = referredDates
                }
            }
            viewModel.prescriptionReferralDates.value?.let {
                dateListAdapter.submitData(it, viewModel.prescriptionTicketId)
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
            if (!viewModel.prescriptionTicketId.isNullOrBlank()) {
                getPatientId()?.takeIf { it.isNotBlank() }
                    ?.let {
                        viewModel.getPrescriptionHistory(
                            patientId = it,
                            prescriptionTicketId = viewModel.prescriptionTicketId
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
        viewModel.prescriptionReferralDates.value?.let {
            it.forEachIndexed { index, labTestDateModel ->
                if (labTestDateModel.id == viewModel.prescriptionTicketId) {
                    selectedIndex = index - 1
                }
            }
        }
        return selectedIndex
    }

    private fun checkNextItem(): Int {
        var selectedIndex = -1
        viewModel.prescriptionReferralDates.value?.let {
            it.forEachIndexed { index, labTestDateModel ->
                if ((labTestDateModel.id == viewModel.prescriptionTicketId) && ((index + 1) < it.size)) {
                    selectedIndex = index + 1
                }
            }
        }
        return selectedIndex
    }

    private fun getPreviousItemToCurrent() {
        val selectedIndex = checkForPreviousItem()
        if (selectedIndex != -1) {
            viewModel.prescriptionReferralDates.value?.get(selectedIndex)?.id?.let {
                viewModel.getPrescriptionHistory(
                    patientId = getPatientId(),
                    prescriptionTicketId = it
                )
                viewModel.prescriptionTicketId = it
            }
        }
    }

    private fun getNextItemToCurrent() {
        val selectedIndex = checkNextItem()
        if (selectedIndex != -1) {
            viewModel.prescriptionReferralDates.value?.get(selectedIndex)?.id?.let {
                viewModel.getPrescriptionHistory(
                    patientId = getPatientId(),
                    prescriptionTicketId = it
                )
                viewModel.prescriptionTicketId = it
            }
        }
    }
}