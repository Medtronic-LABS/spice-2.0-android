package org.medtroniclabs.uhis.ui.followup.fragment

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
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.setDialogPercent
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.data.offlinesync.model.FollowUpCallStatus
import org.medtroniclabs.uhis.databinding.CallResultsDialogBinding
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.utility.CustomSpinnerAdapter
import org.medtroniclabs.uhis.ui.followup.viewmodel.FollowUpViewModel

/**
 * In followup after a call completes, this dialog gets displayed
 * Where, we do capture below details
 * 1. Whether call is successful or not.
 * 2. If call is successful, whether the patient willing to visit health facility or not.
 * 3. If patient not willing to visit health facility, what is the reason.
 * 4. If call is unsuccessful, why it is unsuccessful.
 */
@AndroidEntryPoint
class CallResultDialogFragment : DialogFragment(), View.OnClickListener {
    lateinit var binding: CallResultsDialogBinding
    private val callViewModel: FollowUpViewModel by activityViewModels()
    private var notWillingToVisitSpinner: CustomSpinnerAdapter? = null
    private var spUnsuccessfulReasonAdapter: CustomSpinnerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
        arguments?.let { bundle ->
            callViewModel.apply {
                callType = bundle.getString(DefinedParams.CALL_TYPE)
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
        // Reset data when again showing the popup
        callViewModel.isSuccessful = null
        callViewModel.isWillingToVisitUHC = null
        callViewModel.visitRejectReason = null
        callViewModel.otherVisitRejectReason = null
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
        binding.btnViewPatientDetail.gone()
    }

    private fun setObserver() {
    }

    private fun prefillData() {
        binding.labelHeader.titleView.text = getString(R.string.call_result)
        binding.tvMobileNumberValue.text = callViewModel.selectedFollowUpDetail?.phoneNumber ?: getString(R.string.hyphen_symbol)
        val name = callViewModel.selectedFollowUpDetail?.name
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

        notWillingToVisitSpinner = CustomSpinnerAdapter(requireContext())
        notWillingToVisitSpinner?.setData(reasonsList)
        binding.spReason.adapter = notWillingToVisitSpinner
        binding.spReason.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long,
            ) {
                val reasonsArray = notWillingToVisitReasonArray

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
//        binding.btnViewPatientDetail.setOnClickListener {
//            patientHistoryDetail?.let {
//                val type = object : TypeToken<ArrayList<PatientHistoryData>>() {}.type
//                val historyList: List<PatientHistoryData> = Gson().fromJson(it, type)
//                val dialog = PatientDetailHistoryDialogFragment(historyList)
//                dialog.show(childFragmentManager, "Test_Tag")
//            }
//        }

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
            if (CommonUtils.isHealthScreener()) {
                if (callViewModel.callResultStatus == FollowUpCallStatus.SUCCESSFUL) {
                    updateValueForSuccessful()
                }

                updateStatus()
            } else {
                if (callViewModel.isSuccessful == true) {
                    updateValueForSuccessful()
                } else {
                    callViewModel.callResultStatus = FollowUpCallStatus.UN_SUCCESSFUL
                    callViewModel.visitRejectReason = null
                    callViewModel.otherVisitRejectReason = null
                    val reasonsArray = arrayOf(
                        getString(R.string.please_select),
                        DefinedParams.WRONG_NUMBER,
                        DefinedParams.UNREACHABLE,
                    )
                    val selectedReasonPosition = binding.spUnsuccessfulReason.selectedItemPosition

                    val unsuccessfulReason = if (selectedReasonPosition != 0) {
                        reasonsArray[selectedReasonPosition]
                    } else {
                        "" // "--Select--" selected
                    }

                    callViewModel.unSuccessfulCallReason = unsuccessfulReason
                }
                updateStatus()
            }
        }
    }

    private fun updateValueForSuccessful() {
        callViewModel.callResultStatus = FollowUpCallStatus.SUCCESSFUL
        if (callViewModel.isWillingToVisitUHC == false) {
            val reasonsArray = notWillingToVisitReasonArray
            val selectedReasonPosition = binding.spReason.selectedItemPosition

            val selectedReason = if (selectedReasonPosition != 0) {
                reasonsArray[selectedReasonPosition]
            } else {
                null // "--Select--" selected
            }

            callViewModel.visitRejectReason = selectedReason
            callViewModel.otherVisitRejectReason = if (binding.etOtherNotes.isVisible) {
                binding.etOtherNotes.text
                    ?.toString()
                    ?.takeIf { it.isNotBlank() }
            } else {
                null
            }
        } else {
            callViewModel.visitRejectReason = null
            callViewModel.otherVisitRejectReason = null
        }
        callViewModel.unSuccessfulCallReason = null
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.tvSuccessful -> {
                callViewModel.isSuccessful = true
                callViewModel.isWillingToVisitUHC = null
                // Reset spinner to '--select--'
                binding.spReason.setSelection(0)
                // Clear other reason EditText
                binding.etOtherNotes.setText("")
                updateButtonStates()
                binding.dialogScrollView.post {
                    binding.dialogScrollView.smoothScrollTo(0, binding.tvYes.top)
                }

                if (CommonUtils.isHealthScreener()) {
                    callViewModel.callResultStatus = FollowUpCallStatus.SUCCESSFUL
                    binding.tvSuccessful.isSelected = true
                    binding.tvUnsuccessful.isSelected = false
                    binding.tvWrongNumber.isSelected = false
                    binding.spUnsuccessfulReason.visibility = View.GONE
                    binding.tvReasonUnsuccess.visibility = View.GONE
                }
            }

            R.id.tvUnsuccessful -> {
                callViewModel.isSuccessful = false
                callViewModel.isWillingToVisitUHC = null
                // Reset spinner to '--select--'
                binding.spReason.setSelection(0)
                // Clear other reason EditText
                binding.etOtherNotes.setText("")
                updateButtonStates()
                binding.dialogScrollView.post {
                    binding.dialogScrollView.smoothScrollTo(0, binding.spUnsuccessfulReason.top)
                }

                if (CommonUtils.isHealthScreener()) {
                    callViewModel.callResultStatus = FollowUpCallStatus.UN_SUCCESSFUL
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
                callViewModel.isWillingToVisitUHC = null
                if (CommonUtils.isHealthScreener()) {
                    callViewModel.callResultStatus = FollowUpCallStatus.WRONG_NUMBER
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
                callViewModel.isWillingToVisitUHC = true
                // Reset spinner to '--select--'
                binding.spReason.setSelection(0)
                // Clear other reason EditText
                binding.etOtherNotes.setText("")
                updateButtonStates()
            }

            R.id.tvNo -> {
                callViewModel.isWillingToVisitUHC = false
                updateButtonStates()
                binding.dialogScrollView.post {
                    binding.dialogScrollView.smoothScrollTo(0, binding.spReason.top)
                }
            }
        }
    }

    private fun updateButtonStates() {
        binding.tvYes.isSelected = callViewModel.isWillingToVisitUHC == true
        binding.tvNo.isSelected = callViewModel.isWillingToVisitUHC == false
        binding.tvSuccessful.isSelected = callViewModel.isSuccessful == true
        binding.tvUnsuccessful.isSelected = callViewModel.isSuccessful == false

        when (callViewModel.isSuccessful) {
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

        if (callViewModel.isWillingToVisitUHC == false) {
            binding.reasonGroup.visibility = View.VISIBLE
        } else {
            binding.reasonGroup.visibility = View.GONE
            binding.tvOtherReason.visibility = View.GONE
            binding.etOtherNotes.visibility = View.GONE
        }
        updateSubmitButtonState()
    }

    private fun updateSubmitButtonState() {
        val isSuccessful = callViewModel.isSuccessful == true
        val isYes = callViewModel.isWillingToVisitUHC == true
        val isNo = callViewModel.isWillingToVisitUHC == false
        val reasonsArray = notWillingToVisitReasonArray
        val selectedReasonPosition = binding.spReason.selectedItemPosition

        val selectedReason =
            if (selectedReasonPosition > 0 && selectedReasonPosition < reasonsArray.size) {
                reasonsArray[selectedReasonPosition]
            } else {
                null // "--Select--" selected or invalid index
            }
        val selectedUnsuccessfulReason = binding.spUnsuccessfulReason.selectedItem?.toString()
            ?: getString(R.string.please_select)

        val canSubmit = (isSuccessful && isYes) ||
            (isSuccessful && isNo && canAllowForNo()) ||
            (callViewModel.isSuccessful == false && selectedUnsuccessfulReason != getString(R.string.please_select))

        val canEnable =
            if (selectedReason.equals(DefinedParams.COMPLIANCE_TYPE_OTHER) &&
                binding.etOtherNotes.isVisible &&
                binding.etOtherNotes.text?.isEmpty() == true
            ) {
                false
            } else {
                canSubmit
            }

        binding.btnSubmit.isEnabled = canEnable
    }

    private fun canAllowForNo(): Boolean =
        if (binding.spReason.isVisible) {
            binding.spReason.selectedItemPosition > 0
        } else {
            true
        }

    private fun updateStatus() {
        callViewModel.addCallHistory()
        dismiss()
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

    companion object {
        const val TAG = "CallResultDialogFragment"

        fun newInstance(callType: String): CallResultDialogFragment {
            val fragment = CallResultDialogFragment()
            val args = Bundle()
            args.putString(DefinedParams.CALL_TYPE, callType)
            fragment.arguments = args
            return fragment
        }

        private val notWillingToVisitReasonArray = arrayOf(
            DefinedParams.DEFAULT_ID_LABEL,
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
}
