package com.medtroniclabs.opensource.ui.household.summary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.AdapterView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.databinding.FragmentMemberEditDialogBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.model.MemberDetailsSpinnerModel
import com.medtroniclabs.opensource.ui.household.MemberSelectionListener
import com.medtroniclabs.opensource.ui.household.viewmodel.HouseHoldSummaryViewModel

class MemberEditDialogFragment() : DialogFragment(), View.OnClickListener {

    private lateinit var binding: FragmentMemberEditDialogBinding
    private var listener: MemberSelectionListener? = null
    private val householdSummaryViewModel: HouseHoldSummaryViewModel by activityViewModels()

    constructor(listener: MemberSelectionListener) : this() {
        this.listener = listener
    }

    companion object{
        val TAG = "MemberEditDialogFragment"
        fun newInstance(listener: MemberSelectionListener):MemberEditDialogFragment{
            return MemberEditDialogFragment(listener)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMemberEditDialogBinding.inflate(inflater, container, false)
        val window: Window? = dialog?.window
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = false
        attachListeners()
        loadSpinnerData()
    }

    private fun loadSpinnerData() {
        val memberAdapter = EditMemberSpinnerAdapter(
            requireContext(),
            translate = SecuredPreference.getIsTranslationEnabled()
        )
        val dropDownList = ArrayList<MemberDetailsSpinnerModel>()
        dropDownList.add(
            MemberDetailsSpinnerModel(
                id = DefinedParams.DefaultSelectID,
                name = DefinedParams.DefaultIDLabel
            )
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
                            dob = item.dateOfBirth
                        )
                    )
                }
            }
        }
        memberAdapter.setData(dropDownList)
        binding.etMemberInput.adapter = memberAdapter
        binding.etMemberInput.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
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
        when(view.id){
            binding.btnOkay.id -> {
                //Navigate to Member Edit Fragment
                listener?.onMemberSelected(
                    householdSummaryViewModel.selectedMemberId,
                    true,
                    householdSummaryViewModel.selectedMemberDob
                )
                dismiss()
            }
            binding.imgClose.id -> {
                dismiss()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }
}