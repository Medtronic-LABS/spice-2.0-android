package org.medtroniclabs.uhis.ui.household.summary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.AdapterView
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams.MemberDeceased
import org.medtroniclabs.uhis.appextensions.getLongTime
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.startBackgroundOfflineSync
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.databinding.FragmentMemberDeceasedDialogBinding
import org.medtroniclabs.uhis.db.entity.SignsAndSymptomsEntity
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.utility.CheckboxDialogAdapter
import org.medtroniclabs.uhis.formgeneration.utility.CustomSpinnerAdapter
import org.medtroniclabs.uhis.model.MemberDetailsSpinnerModel
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH
import org.medtroniclabs.uhis.ui.household.viewmodel.HouseHoldSummaryViewModel

class MemberDeceasedDialogFragment : DialogFragment(), View.OnClickListener {
    private lateinit var binding: FragmentMemberDeceasedDialogBinding
    private val householdSummaryViewModel: HouseHoldSummaryViewModel by activityViewModels()

    /**
     * Neonatal type-of-death options with a UI-specific default select item.
     */
    private val neonateDeathTypeOptions: ArrayList<Map<String, Any>> by lazy {
        withDefaultTypeOption(RMNCH.neonatalDeathTypeOptions)
    }

    /**
     * Maternal type-of-death options with a UI-specific default select item.
     */
    private val motherDeathTypeOptions: ArrayList<Map<String, Any>> by lazy {
        withDefaultTypeOption(RMNCH.maternalDeathTypeOptions)
    }

    /** Spinner adapter used to bind type-of-death options dynamically. */
    private var typeOfDeathAdapter: CustomSpinnerAdapter? = null

    /** Checkbox adapter used for maternal/neonatal cause selections. */
    private var causeOfDeathAdapter: CheckboxDialogAdapter? = null

    /** Backing list for currently visible cause-of-death options. */
    private var causeOfDeathItems = mutableListOf<SignsAndSymptomsEntity>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentMemberDeceasedDialogBinding.inflate(inflater, container, false)
        val window: Window? = dialog?.window
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = false
        householdSummaryViewModel.setUserJourney(MemberDeceased)
        binding.etTypeOfDeath.setBackgroundResource(R.drawable.edittext_background)
        typeOfDeathAdapter = CustomSpinnerAdapter(requireContext(), SecuredPreference.getIsTranslationEnabled())
        binding.etTypeOfDeath.adapter = typeOfDeathAdapter
        binding.rvItems.layoutManager = LinearLayoutManager(requireContext())
        attachListeners()
        loadSpinnerData()
        onEnterReason()
        showInitialState()
    }

    /**
     * Enables submit button only when free-text reason is visible and entered.
     */
    private fun onEnterReason() {
        binding.etReason.addTextChangedListener {
            updateSubmitState()
        }
    }

    /**
     * Populates member spinner and drives branch selection for deceased reason flow.
     */
    private fun loadSpinnerData() {
        val memberAdapter = EditMemberSpinnerAdapter(requireContext())
        val dropDownList = ArrayList<MemberDetailsSpinnerModel>()
        dropDownList.add(
            MemberDetailsSpinnerModel(
                id = DefinedParams.DEFAULT_SELECT_ID,
                name = DefinedParams.DEFAULT_ID_LABEL,
            ),
        )
        householdSummaryViewModel.householdMembersLiveData.value?.let { data ->
            data.forEach { item ->
                if (item.isActive) {
                    dropDownList.add(
                        MemberDetailsSpinnerModel(
                            id = item.id,
                            name = item.name,
                            age = "",
                            gender = item.gender,
                            dob = item.dateOfBirth,
                        ),
                    )
                }
            }
        }
        memberAdapter.setData(dropDownList)
        binding.etMemberInput.adapter = memberAdapter
        binding.etMemberInput.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    val selectedItem = memberAdapter.getData(position = position)
                    resetDynamicSelectionViews()
                    selectedItem?.let {
                        if (it.id != -1L) {
                            householdSummaryViewModel.selectedMemberId = it.id
                            householdSummaryViewModel.selectedMemberDob = it.dob
                            evaluateSelectedMemberFlow(it)
                        } else {
                            showInitialState()
                        }
                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    /**
                     * this method is not used
                     */
                }
            }
    }

    private fun attachListeners() {
        binding.btnOkay.safeClickListener(this)
        binding.imgClose.safeClickListener(this)
        binding.etTypeOfDeath.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                adapterView: AdapterView<*>?,
                view: View?,
                pos: Int,
                itemId: Long,
            ) {
                val typeOfDeath = typeOfDeathAdapter?.getData(pos)
                typeOfDeath?.let {
                    householdSummaryViewModel.selectedTypeOfDeath = typeOfDeath
                }
                val typeId = typeOfDeath?.get(DefinedParams.ID)?.toString().orEmpty()
                onTypeOfDeathSelected(typeId)
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {
                // Do Nothing
            }
        }
    }

    /**
     * Handles visibility state after type-of-death selection.
     */
    private fun onTypeOfDeathSelected(typeId: String) {
        when {
            typeId.isBlank() || typeId == DefinedParams.DEFAULT_ID -> {
                binding.tvReason.gone()
                binding.tvReasonHint.gone()
                binding.rvItems.gone()
                binding.etReason.gone()
            }

            typeId.equals(RMNCH.DEATH_TYPE_OTHER, true) -> showReasonInput()

            typeId.equals(RMNCH.DEATH_TYPE_NEONATAL, true) -> showCauseOfDeathOptions(RMNCH.neonatalDeathCauseOptions, RMNCH.DEATH_TYPE_NEONATAL)

            typeId.equals(RMNCH.DEATH_TYPE_MOTHER, true) -> showCauseOfDeathOptions(RMNCH.maternalDeathCauseOptions, RMNCH.DEATH_TYPE_MOTHER)

            else -> {
                binding.tvReason.gone()
                binding.tvReasonHint.gone()
                binding.rvItems.gone()
                binding.etReason.gone()
            }
        }
        updateSubmitState()
    }

    /**
     * Resolves whether selected member should use neonatal, maternal, or direct-reason flow.
     */
    private fun evaluateSelectedMemberFlow(item: MemberDetailsSpinnerModel) {
        lifecycleScope.launch {
            val selectedMemberId = item.id
            val isNeonate = isWithinPastDays(item.dob, NEONATE_DAYS_LIMIT)
            val isRecentDeliveryCase =
                !isNeonate &&
                    item.gender.equals(DefinedParams.GENDER_FEMALE, true) &&
                    isLatestDeliveryWithinDays(selectedMemberId, MOTHER_DELIVERY_DAYS_LIMIT)

            // Prevent stale UI updates when user changes member quickly.
            if (!isAdded || householdSummaryViewModel.selectedMemberId != selectedMemberId) {
                return@launch
            }
            when {
                isNeonate -> showTypeOfDeathSelection(neonateDeathTypeOptions)
                isRecentDeliveryCase -> showTypeOfDeathSelection(motherDeathTypeOptions)
                else -> showReasonInputDirectly()
            }
        }
    }

    /**
     * Checks whether latest pregnancy delivery is within a day window for member.
     */
    private suspend fun isLatestDeliveryWithinDays(
        memberId: Long,
        daysLimit: Long,
    ): Boolean {
        val deliveryDate = householdSummaryViewModel.getLatestPregnancyDetail(memberId)?.dateOfDelivery
        return isWithinPastDays(deliveryDate, daysLimit)
    }

    /**
     * Checks if a date string is within [daysLimit] days from today.
     */
    private fun isWithinPastDays(
        date: String?,
        daysLimit: Long,
    ): Boolean {
        val localDate = DateUtils.parseDate(date) ?: return false
        val localDateMilli = localDate.getLongTime()
        val days = DateUtils.getDaysDifference(localDateMilli) ?: return false
        return days in 0..daysLimit
    }

    /**
     * Shows type-of-death controls and hides downstream reason/cause input until selection.
     */
    private fun showTypeOfDeathSelection(options: ArrayList<Map<String, Any>>) {
        binding.tvTypeOfDeath.visible()
        binding.etTypeOfDeath.visible()
        binding.tvReason.gone()
        binding.tvReasonHint.gone()
        binding.etReason.gone()
        binding.rvItems.gone()
        binding.etReason.setText("")
        causeOfDeathItems.clear()
        causeOfDeathAdapter = null
        binding.rvItems.adapter = null
        typeOfDeathAdapter?.setData(options)
        binding.etTypeOfDeath.setSelection(0)
        updateSubmitState()
    }

    /**
     * Adds default "Please select" entry required by spinner UI.
     */
    private fun withDefaultTypeOption(options: List<Map<String, Any>>): ArrayList<Map<String, Any>> =
        arrayListOf<Map<String, Any>>(
            mapOf(
                DefinedParams.ID to DefinedParams.DEFAULT_ID,
                DefinedParams.NAME to getString(R.string.please_select),
            ),
        ).apply {
            addAll(options)
        }

    /**
     * Shows reason input mode and keeps hint hidden for text-based entry.
     */
    private fun showReasonInput() {
        binding.tvReason.visible()
        binding.tvReasonHint.gone()
        binding.rvItems.gone()
        binding.etReason.visible()
        causeOfDeathItems.clear()
        causeOfDeathAdapter = null
        binding.rvItems.adapter = null
    }

    /**
     * Shows direct reason input without type-of-death selection.
     */
    private fun showReasonInputDirectly() {
        binding.tvTypeOfDeath.gone()
        binding.etTypeOfDeath.gone()
        binding.tvReason.visible()
        binding.tvReasonHint.gone()
        binding.rvItems.gone()
        binding.etReason.visible()
        binding.etReason.setText("")
        causeOfDeathItems.clear()
        causeOfDeathAdapter = null
        binding.rvItems.adapter = null
        updateSubmitState()
    }

    /**
     * Binds cause-of-death checkbox adapter for neonatal/maternal branches.
     */
    private fun showCauseOfDeathOptions(
        options: List<Map<String, Any>>,
        type: String,
    ) {
        binding.tvReason.visible()
        binding.tvReasonHint.visible()
        binding.etReason.gone()
        binding.rvItems.visible()
        binding.etReason.setText("")
        causeOfDeathItems = buildCauseItems(options, type).toMutableList()
        causeOfDeathAdapter = CheckboxDialogAdapter(
            dialogList = causeOfDeathItems,
            translate = SecuredPreference.getIsTranslationEnabled(),
        ) {
            updateSubmitState()
        }
        binding.rvItems.adapter = causeOfDeathAdapter
    }

    /**
     * Converts option maps to checkbox entities for recycler adapter.
     */
    private fun buildCauseItems(
        options: List<Map<String, Any>>,
        type: String,
    ): List<SignsAndSymptomsEntity> =
        options.mapIndexed { index, option ->
            SignsAndSymptomsEntity(
                _id = index.toLong(),
                symptom = option[DefinedParams.NAME]?.toString().orEmpty(),
                type = type,
                value = option[DefinedParams.ID]?.toString(),
                displayValue = option[DefinedParams.CULTURE_VALUE]?.toString(),
            )
        }

    /**
     * Resets dynamic reason/cause views when member selection changes.
     */
    private fun resetDynamicSelectionViews() {
        binding.etReason.setText("")
        binding.tvReason.gone()
        binding.tvReasonHint.gone()
        binding.etReason.gone()
        binding.rvItems.gone()
        causeOfDeathItems.clear()
        causeOfDeathAdapter = null
        binding.rvItems.adapter = null
        typeOfDeathAdapter?.setData(arrayListOf())
        binding.btnOkay.isEnabled = false
    }

    /**
     * Shows initial dialog state with only member selection visible.
     */
    private fun showInitialState() {
        binding.tvTypeOfDeath.gone()
        binding.etTypeOfDeath.gone()
        resetDynamicSelectionViews()
    }

    /**
     * Enables submit for either text reason input or selected causes.
     */
    private fun updateSubmitState() {
        val hasTextReason = binding.etReason.isVisible &&
            binding.etReason.text
                ?.toString()
                ?.isNotBlank() == true
        val hasCauseSelection = binding.rvItems.isVisible && causeOfDeathItems.any { it.isSelected }
        binding.btnOkay.isEnabled = hasTextReason || hasCauseSelection
    }

    /**
     * Builds final reason payload from visible input mode.
     */
    private fun getDeceasedReasonPayload(): String {
        if (binding.etReason.isVisible) {
            return binding.etReason.text
                ?.toString()
                ?.trim()
                .orEmpty()
        }
        val selectedCauseValues =
            causeOfDeathItems
                .filter { it.isSelected }
                .joinToString(",") { it.value ?: it.symptom }
        val selectedTypeId = householdSummaryViewModel.selectedTypeOfDeath[DefinedParams.ID]?.toString().orEmpty()
        return if (
            selectedTypeId.equals(RMNCH.DEATH_TYPE_NEONATAL, true) ||
            selectedTypeId.equals(RMNCH.DEATH_TYPE_MOTHER, true)
        ) {
            val typePrefix =
                if (selectedTypeId.equals(RMNCH.DEATH_TYPE_NEONATAL, true)) {
                    RMNCH.DECEASED_REASON_PREFIX_NEONATAL
                } else {
                    RMNCH.DECEASED_REASON_PREFIX_MOTHER
                }
            "$typePrefix:$selectedCauseValues"
        } else {
            selectedCauseValues
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.btnOkay.id -> {
                householdSummaryViewModel.updateMemberDeceasedReason(
                    householdSummaryViewModel.selectedMemberId,
                    false,
                    getDeceasedReasonPayload(),
                )
                requireActivity().startBackgroundOfflineSync()
                householdSummaryViewModel.setUserJourney(AnalyticsDefinedParams.MEMBERDECEASEDSUMBITTRIGGERED)
                dismiss()
            }

            binding.imgClose.id -> {
                householdSummaryViewModel.setUserJourney(AnalyticsDefinedParams.MEMBERDECEASEDCLOSETRIGGERED)
                dismiss()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
        )
    }

    companion object {
        const val TAG = "MemberEditDialogFragment"

        /** Maximum age in days to classify selected member as neonate. */
        private const val NEONATE_DAYS_LIMIT = 30L

        /** Maximum days since latest delivery to classify as maternal case. */
        private const val MOTHER_DELIVERY_DAYS_LIMIT = 50L

        fun newInstance(): MemberDeceasedDialogFragment = MemberDeceasedDialogFragment()
    }
}
