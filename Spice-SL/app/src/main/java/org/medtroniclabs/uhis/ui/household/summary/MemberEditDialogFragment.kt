package org.medtroniclabs.uhis.ui.household.summary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.AdapterView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.databinding.FragmentMemberEditDialogBinding
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.model.MemberDetailsSpinnerModel
import org.medtroniclabs.uhis.ui.household.MemberSelectionListener
import org.medtroniclabs.uhis.ui.household.viewmodel.HouseHoldSummaryViewModel

class MemberEditDialogFragment() : DialogFragment(), View.OnClickListener {
    private lateinit var binding: FragmentMemberEditDialogBinding
    private var listener: MemberSelectionListener? = null
    private val householdSummaryViewModel: HouseHoldSummaryViewModel by activityViewModels()

    constructor(listener: MemberSelectionListener) : this() {
        this.listener = listener
    }

    companion object {
        val TAG = "MemberEditDialogFragment"

        fun newInstance(listener: MemberSelectionListener): MemberEditDialogFragment = MemberEditDialogFragment(listener)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentMemberEditDialogBinding.inflate(inflater, container, false)
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
        householdSummaryViewModel.setUserJourney(AnalyticsDefinedParams.MEMBEREDITCHOOSERDIALOGUE)
        attachListeners()
        loadSpinnerData()
    }

    private fun loadSpinnerData() {
        val memberAdapter = EditMemberSpinnerAdapter(
            requireContext(),
            translate = SecuredPreference.getIsTranslationEnabled(),
        )
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
                    p0: AdapterView<*>?,
                    p1: View?,
                    p2: Int,
                    p3: Long,
                ) {
                    val selectedItem = memberAdapter.getData(position = p2)
                    selectedItem?.let {
                        if (it.id != -1L) {
                            binding.btnOkay.isEnabled = true
                            householdSummaryViewModel.selectedMemberId = it.id
                            householdSummaryViewModel.selectedMemberDob = it.dob
                        } else {
                            binding.btnOkay.isEnabled = false
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
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.btnOkay.id -> {
                // Navigate to Member Edit Fragment
                listener?.onMemberSelected(
                    householdSummaryViewModel.selectedMemberId,
                    true,
                    householdSummaryViewModel.selectedMemberDob,
                )
                householdSummaryViewModel.setUserJourney(AnalyticsDefinedParams.MEMBEREDITOKAYBUTTONTRIGGERED)
                dismiss()
            }
            binding.imgClose.id -> {
                dismiss()
                householdSummaryViewModel.setUserJourney(AnalyticsDefinedParams.MEMBEREDITCLOSEBUTTONTRIGGERED)
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
}
