package org.medtroniclabs.uhis.ui.patient.fragment

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.AdapterView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.setDialogPercent
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.data.model.UpdatePatientCallRegister
import org.medtroniclabs.uhis.data.offlinesync.model.FollowUpCallStatus
import org.medtroniclabs.uhis.data.servicerecipient.PatientHistoryData
import org.medtroniclabs.uhis.databinding.CallResultsDialogBinding
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.utility.CustomSpinnerAdapter
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.followup.fragment.PatientDetailHistoryDialogFragment
import org.medtroniclabs.uhis.ui.patient.CallRegisterInterface
import org.medtroniclabs.uhis.ui.patient.viewmodel.PatientListViewModel

@AndroidEntryPoint
class CallResultsDialogFragment : DialogFragment(), View.OnClickListener {
    lateinit var binding: CallResultsDialogBinding
    private val callViewModel: PatientListViewModel by viewModels()
    private var callRegisterInterface: CallRegisterInterface? = null
    private var ccSpinnerAdapter: CustomSpinnerAdapter? = null
    private var spUnsuccessfulReasonAdapter: CustomSpinnerAdapter? = null
    private var callStartTime: Long? = null
    private var callEndTime: Long? = null
    private var patientHistoryDetail: String? = null

    companion object {
        const val TAG = "CallResultsDialogFragment"
        private const val ARG_SELECTED_TAB = "selected_tab"

        fun newInstance(selectedTab: String): CallResultsDialogFragment {
            val fragment = CallResultsDialogFragment()
            val args = Bundle()
            args.putString(ARG_SELECTED_TAB, selectedTab)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val parent = parentFragment
        callRegisterInterface = if (parent is CallRegisterInterface) {
            parent
        } else if (context is CallRegisterInterface) {
            context
        } else {
            throw RuntimeException()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
        arguments?.let { bundle ->
            callViewModel.apply {
                name = bundle.getString(DefinedParams.NAME)
                age = bundle.getLong(DefinedParams.AGE)
                gender = bundle.getString(DefinedParams.GENDER)
                mobile = bundle.getString(DefinedParams.PHONE_NUMBER)
                callId = bundle.getLong(DefinedParams.id)
                callType = bundle.getString(DefinedParams.CALL_TYPE)
            }
            if (bundle.containsKey(DefinedParams.CALL_START_TIME)) {
                callStartTime = bundle.getLong(DefinedParams.CALL_START_TIME)
            }

            if (bundle.containsKey(DefinedParams.PATIENT_HISTORY)) {
                patientHistoryDetail = bundle.getString(DefinedParams.PATIENT_HISTORY)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = CallResultsDialogBinding.inflate(inflater, container, false)
        val window: Window? = dialog?.window
        window?.setBackgroundDrawableResource(R.color.transparent)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        setObserver()
        prefillData()
        setListeners()
        setupSpinner()
        setupUnsuccessfulSpinner()
        updateButtonStates()
        updateSubmitButtonState()
        binding.etOtherNotes.doOnTextChanged { _, _, _, _ ->
            updateSubmitButtonState()
        }

        if (patientHistoryDetail != null) {
            binding.btnViewPatientDetail.visible()
        } else {
            binding.btnViewPatientDetail.gone()
        }
    }

    private fun setObserver() {
        callViewModel.statusUpdateResponse.observe(viewLifecycleOwner) {
            when (it.state) {
                ResourceState.SUCCESS -> {
                    callRegisterInterface?.callRegisterStatus(true)
                    dismiss()
                }

                ResourceState.LOADING -> {
                }

                ResourceState.ERROR -> {
                }
            }
        }
    }

    private fun prefillData() {
        binding.labelHeader.titleView.text = getString(R.string.call_result)
        binding.tvMobileNumberValue.text = callViewModel.mobile ?: getString(R.string.hyphen_symbol)
        val name = callViewModel.name
        binding.tvNameValue.text =
            if (name != null) CommonUtils.capitalize(name) else getString(R.string.hyphen_symbol)
        binding.tvFollowUpValue.text = getCallType(callViewModel.callType)
    }

    private fun getCallType(callType: String?): String =
        when (callType) {
            DefinedParams.SCREENED -> getString(R.string.screening)
            DefinedParams.OVERDUE -> getString(R.string.overdue)
            DefinedParams.MISSEDVISIT -> getString(R.string.missed_visit)
            DefinedParams.REDRISK -> getString(R.string.red_risk)
            DefinedParams.LTF -> getString(R.string.ltf)
            else -> getString(R.string.hyphen_symbol)
        }

    private fun setupSpinner() {
        val reasonStrings = resources.getStringArray(R.array.call_failure_reason_for_sk)
        val reasonsList = ArrayList<Map<String, Any>>()

        reasonStrings.forEachIndexed { index, reason ->
            reasonsList.add(mapOf(DefinedParams.id to index, DefinedParams.NAME to reason))
        }

        ccSpinnerAdapter = CustomSpinnerAdapter(requireContext())
        ccSpinnerAdapter?.setData(reasonsList)
        binding.spReason.adapter = ccSpinnerAdapter
        binding.spReason.tag = null
        binding.spReason.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long,
            ) {
                val reasonsArray = getSuccessfulCallReasons()

                val selectedReason = if (position > 0 && position < reasonsArray.size) {
                    reasonsArray[position]
                } else {
                    null // "--Select--" selected or invalid index
                }

                if (selectedReason.equals(DefinedParams.COMPLIANCE_TYPE_OTHER) &&
                    !CommonUtils.isHealthScreener()
                ) {
                    binding.tvOtherReason.visibility = View.VISIBLE
                    binding.etOtherNotes.visibility = View.VISIBLE
                    binding.dialogScrollView.post {
                        binding.dialogScrollView.fullScroll(View.FOCUS_DOWN)
                    }
                } else {
                    binding.tvOtherReason.visibility = View.GONE
                    binding.etOtherNotes.visibility = View.GONE
                }
                updateSubmitButtonState()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupUnsuccessfulSpinner() {
        val reasonStrings = resources.getStringArray(R.array.call_unsuccessful_reasons)
        val reasonsList = ArrayList<Map<String, Any>>()

        reasonStrings.forEachIndexed { index, reason ->
            reasonsList.add(mapOf(DefinedParams.id to index, DefinedParams.NAME to reason))
        }

        spUnsuccessfulReasonAdapter = CustomSpinnerAdapter(requireContext())
        spUnsuccessfulReasonAdapter?.setData(reasonsList)
        binding.spUnsuccessfulReason.adapter = spUnsuccessfulReasonAdapter
        binding.spUnsuccessfulReason.tag = null
        binding.spUnsuccessfulReason.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    updateSubmitButtonState()
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
    }

    private fun setListeners() {
        binding.btnViewPatientDetail.setOnClickListener {
            patientHistoryDetail?.let {
                val type = object : TypeToken<ArrayList<PatientHistoryData>>() {}.type
                val historyList: List<PatientHistoryData> = Gson().fromJson(it, type)
                val dialog = PatientDetailHistoryDialogFragment.newInstance(historyList)
                dialog.show(childFragmentManager, PatientDetailHistoryDialogFragment::class.simpleName)
            }
        }

        binding.labelHeader.ivClose.visibility = View.INVISIBLE
        binding.tvSuccessful.safeClickListener(this)
        binding.tvUnsuccessful.safeClickListener(this)
        binding.tvWrongNumber.safeClickListener(this)
        val heightInPx = TypedValue
            .applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                65f, // 65sdp
                resources.displayMetrics,
            ).toInt()

        if (!CommonUtils.isHealthScreener()) {
            binding.tvUnsuccessful.background =
                ContextCompat.getDrawable(requireContext(), R.drawable.right_mh_view_selector)
            binding.tvWrongNumber.visibility = View.GONE
        } else {
            val paramsSuccess = binding.tvSuccessful.layoutParams
            paramsSuccess.height = heightInPx
            binding.tvSuccessful.layoutParams = paramsSuccess

            val paramsUn = binding.tvUnsuccessful.layoutParams
            paramsUn.height = heightInPx
            binding.tvUnsuccessful.layoutParams = paramsUn

            val paramsWn = binding.tvWrongNumber.layoutParams
            paramsWn.height = heightInPx
            binding.tvWrongNumber.layoutParams = paramsWn
        }
        binding.tvYes.safeClickListener(this)
        binding.tvNo.safeClickListener(this)
        binding.btnSubmit.setOnClickListener {
            val totalTimeTaken = calculateTotalTimeTaken()

            if (CommonUtils.isHealthScreener()) {
                if (callViewModel.callResultStatus == FollowUpCallStatus.SUCCESSFUL.name) {
                    updateValueForSuccessful()
                }

                updateStatus(
                    callViewModel.callResultStatus,
                    callViewModel.isWillingToVisit,
                    callViewModel.reason,
                    callViewModel.otherReason,
                    null,
                    totalTimeTaken = totalTimeTaken,
                )
            } else {
                if (callViewModel.isCalled == true) {
                    updateValueForSuccessful()
                } else {
                    callViewModel.callResultStatus = FollowUpCallStatus.UNSUCCESSFUL.name
                    callViewModel.reason = null
                    callViewModel.otherReason = null
                    val reasonsArray = arrayOf(
                        getString(R.string.please_select),
                        DefinedParams.WRONG_NUMBER,
                        DefinedParams.UNREACHABLE,
                    )
                    val selectedReasonPosition = binding.spUnsuccessfulReason.selectedItemPosition

                    val unsuccessfulReason = if (selectedReasonPosition != 0) {
                        reasonsArray[selectedReasonPosition]
                    } else {
                        null // "--Select--" selected
                    }

                    callViewModel.unsuccessfulReason = unsuccessfulReason
                }
                updateStatus(
                    callViewModel.callResultStatus,
                    callViewModel.isWillingToVisit,
                    callViewModel.reason,
                    callViewModel.otherReason,
                    callViewModel.unsuccessfulReason,
                    totalTimeTaken = totalTimeTaken,
                )
            }
        }
        updateButtonStates()
    }

    private fun updateValueForSuccessful() {
        callViewModel.callResultStatus = FollowUpCallStatus.SUCCESSFUL.name
        if (callViewModel.isWillingToVisit == false) {
            val reasonsArray = getSuccessfulCallReasons()
            val selectedReasonPosition = binding.spReason.selectedItemPosition

            val selectedReason = if (selectedReasonPosition != 0) {
                reasonsArray[selectedReasonPosition]
            } else {
                null // "--Select--" selected
            }

            callViewModel.reason = selectedReason
            callViewModel.otherReason = if (binding.etOtherNotes.isVisible) {
                binding.etOtherNotes.text
                    ?.toString()
                    ?.takeIf { it.isNotBlank() }
            } else {
                null
            }
        } else {
            callViewModel.reason = null
            callViewModel.otherReason = null
        }
        callViewModel.unsuccessfulReason = null
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.tvSuccessful -> {
                callViewModel.isCalled = true
                callViewModel.isWillingToVisit = null
                callStartTime?.let {
                    callEndTime = System.currentTimeMillis()
                }
                // Reset spinner to '--select--'
                binding.spReason.setSelection(0)
                // Clear other reason EditText
                binding.etOtherNotes.setText("")
                updateButtonStates()
                binding.dialogScrollView.post {
                    binding.dialogScrollView.smoothScrollTo(0, binding.tvYes.top)
                }

                if (CommonUtils.isHealthScreener()) {
                    callViewModel.callResultStatus = FollowUpCallStatus.SUCCESSFUL.name
                    binding.tvSuccessful.isSelected = true
                    binding.tvUnsuccessful.isSelected = false
                    binding.tvWrongNumber.isSelected = false
                    // binding.tvUHC.visibility = View.GONE
                    // binding.visitBtn.visibility = View.GONE
                    binding.spUnsuccessfulReason.visibility = View.GONE
                    binding.tvReasonUnsuccess.visibility = View.GONE
                    //  binding.btnSubmit.isEnabled = true
                }
            }

            R.id.tvUnsuccessful -> {
                callViewModel.isCalled = false
                callViewModel.isWillingToVisit = null
                // Reset spinner to '--select--'
                binding.spReason.setSelection(0)
                // Clear other reason EditText
                binding.etOtherNotes.setText("")
                updateButtonStates()
                binding.dialogScrollView.post {
                    binding.dialogScrollView.smoothScrollTo(0, binding.spUnsuccessfulReason.top)
                }

                if (CommonUtils.isHealthScreener()) {
                    callViewModel.callResultStatus = FollowUpCallStatus.UNSUCCESSFUL.name
                    binding.tvSuccessful.isSelected = false
                    binding.tvUnsuccessful.isSelected = true
                    binding.tvWrongNumber.isSelected = false
                    binding.tvUHC.visibility = View.GONE
                    binding.visitBtn.visibility = View.GONE
                    binding.spUnsuccessfulReason.visibility = View.GONE
                    binding.tvReasonUnsuccess.visibility = View.GONE
                    binding.btnSubmit.isEnabled = true
                }
            }

            R.id.tvWrongNumber -> {
                callViewModel.isWillingToVisit = null
                if (CommonUtils.isHealthScreener()) {
                    callViewModel.callResultStatus = FollowUpCallStatus.WRONG_NUMBER.name
                    binding.tvSuccessful.isSelected = false
                    binding.tvUnsuccessful.isSelected = false
                    binding.tvWrongNumber.isSelected = true
                    binding.tvUHC.visibility = View.GONE
                    binding.visitBtn.visibility = View.GONE
                    binding.spUnsuccessfulReason.visibility = View.GONE
                    binding.tvReasonUnsuccess.visibility = View.GONE
                    binding.btnSubmit.isEnabled = true
                }
            }

            R.id.tvYes -> {
                callViewModel.isWillingToVisit = true
                // Reset spinner to '--select--'
                binding.spReason.setSelection(0)
                // Clear other reason EditText
                binding.etOtherNotes.setText("")
                updateButtonStates()
            }

            R.id.tvNo -> {
                callViewModel.isWillingToVisit = false
                updateButtonStates()
                binding.dialogScrollView.post {
                    binding.dialogScrollView.smoothScrollTo(0, binding.spReason.top)
                }
            }
        }
    }

    private fun updateButtonStates() {
        binding.tvYes.isSelected = callViewModel.isWillingToVisit == true
        binding.tvNo.isSelected = callViewModel.isWillingToVisit == false
        binding.tvSuccessful.isSelected = callViewModel.isCalled == true
        binding.tvUnsuccessful.isSelected = callViewModel.isCalled == false

        when (callViewModel.isCalled) {
            true -> {
                binding.tvUHC.visibility = View.VISIBLE
                binding.visitBtn.visibility = View.VISIBLE
                binding.spUnsuccessfulReason.visibility = View.GONE
                binding.tvReasonUnsuccess.visibility = View.GONE
                binding.spUnsuccessfulReason.setSelection(0)
            }
            false -> {
                binding.tvUHC.visibility = View.GONE
                binding.visitBtn.visibility = View.GONE
                binding.tvReasonUnsuccess.visibility = View.VISIBLE
                binding.spUnsuccessfulReason.visibility = View.VISIBLE
            }
            else -> {
                binding.tvUHC.visibility = View.GONE
                binding.visitBtn.visibility = View.GONE
                binding.spUnsuccessfulReason.visibility = View.GONE
                binding.tvReasonUnsuccess.visibility = View.GONE
            }
        }

        if (callViewModel.isWillingToVisit == false && callViewModel.callType != DefinedParams.SCREENED) {
            binding.reasonGroup.visibility = View.VISIBLE
        } else {
            binding.reasonGroup.visibility = View.GONE
            binding.tvOtherReason.visibility = View.GONE
            binding.etOtherNotes.visibility = View.GONE
        }
        updateSubmitButtonState()
    }

    private fun updateSubmitButtonState() {
        val isSuccessful = callViewModel.isCalled == true
        val isYes = callViewModel.isWillingToVisit == true
        val isNo = callViewModel.isWillingToVisit == false
        val reasonsArray = getSuccessfulCallReasons()
        val selectedReasonPosition = binding.spReason.selectedItemPosition

        val selectedReason =
            if (selectedReasonPosition > 0 && selectedReasonPosition < reasonsArray.size) {
                reasonsArray[selectedReasonPosition]
            } else {
                null // "--Select--" selected or invalid index
            }
        val selectedUnsuccessfulReason = binding.spUnsuccessfulReason.selectedItem?.toString()
            ?: getString(R.string.please_select)

        val canSubmit =
            (isSuccessful && isYes) ||
                (isSuccessful && isNo && canAllowForNo(selectedReason)) ||
                (
                    callViewModel.isCalled == false &&
                        selectedUnsuccessfulReason != getString(
                            R.string.please_select,
                        )
                )

        val canEnable = if (selectedReason.equals(DefinedParams.COMPLIANCE_TYPE_OTHER) &&
            binding.etOtherNotes.isVisible &&
            binding.etOtherNotes.text?.isEmpty() == true
        ) {
            false
        } else {
            canSubmit
        }

        binding.btnSubmit.isEnabled = canEnable
    }

    private fun canAllowForNo(selectedReason: String?): Boolean =
        if (binding.spReason.isVisible) {
            selectedReason != null && selectedReason != getString(R.string.please_select)
        } else {
            true
        }

    private fun calculateTotalTimeTaken(): Double? =
        callStartTime?.let { startTime ->
            val endTime = callEndTime ?: System.currentTimeMillis()
            val durationInMillis = endTime - startTime
            durationInMillis / 60000.0
        }

    private fun updateStatus(
        status: String,
        willingToVisit: Boolean?,
        reason: String?,
        otherReason: String?,
        unsuccessfulReason: String?,
        totalTimeTaken: Double? = null,
    ) {
        callViewModel.callId?.let {
            callViewModel.updateCallStatus(
                UpdatePatientCallRegister(
                    it,
                    status = status,
                    callType = CommonUtils.getCallType(callViewModel.callType) ?: DefinedParams.SCREENED,
                    isWillingToVisitUHC = willingToVisit,
                    visitRejectReason = reason,
                    otherVisitRejectReason = otherReason,
                    unSuccessfulCallReason = unsuccessfulReason,
                    wrongNumber = if (!CommonUtils.isHealthScreener()) {
                        unsuccessfulReason?.equals(
                            DefinedParams.WRONG_NUMBER,
                            ignoreCase = true,
                        ) == true
                    } else {
                        status.equals(FollowUpCallStatus.WRONG_NUMBER.name, ignoreCase = true)
                    },
                    duration = totalTimeTaken,
                ),
            )
        }
    }

    override fun onStart() {
        super.onStart()
        if (CommonUtils.checkIsLargeTablet(requireContext())) {
            setDialogPercent(75, getHeightPercentage())
        } else if (CommonUtils.checkIsTablet(requireContext())) {
            setDialogPercent(75, getHeightPercentage())
        } else {
            setDialogPercent(95, getHeightPercentage())
        }
    }

    private fun getHeightPercentage(): Int {
        val orientation = resources.configuration.orientation
        return if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            85
        } else {
            70
        }
    }

    private fun getSuccessfulCallReasons(): Array<String> =
        arrayOf(
            getString(R.string.please_select),
            DefinedParams.TREATMENT_FROM_OTHER_FACILITY,
            DefinedParams.NO_MEDICINE,
            DefinedParams.LONG_DISTANCE,
            DefinedParams.TRANSPORTATION_AND_UNSUPPLIED_MEDICINE_COST,
            DefinedParams.LONG_WAITING_QUEUE,
            DefinedParams.MIGRATED_TO_OTHER_PLACE,
            DefinedParams.DIED,
            DefinedParams.COMPLIANCE_TYPE_OTHER,
        )
}
