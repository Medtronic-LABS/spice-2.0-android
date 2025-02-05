package com.medtroniclabs.opensource.ui.dialog

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.invisible
import com.medtroniclabs.opensource.databinding.FragmentSuccessDialogBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.ui.household.HouseholdDefinedParams.IsHousehold
import com.medtroniclabs.opensource.ui.household.HouseholdDefinedParams.IsHouseholdMember
import com.medtroniclabs.opensource.ui.household.HouseholdDefinedParams.isPhuWalkInsFlow
import com.medtroniclabs.opensource.ui.landing.OnDialogDismissListener
import com.medtroniclabs.opensource.ui.phuwalkins.activity.PhuWalkInsActivity

class SuccessDialogFragment : DialogFragment(), View.OnClickListener {

    private lateinit var binding: FragmentSuccessDialogBinding
    private var onDismissListener: OnDialogDismissListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        onDismissListener = context as OnDialogDismissListener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSuccessDialogBinding.inflate(layoutInflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        isCancelable = false
        return binding.root
    }

    companion object {
        const val TAG = "SuccessDialogFragment"

        fun newInstance(isHousehold : Boolean = false, isMember : Boolean = false,isPhuLink:Boolean=false): SuccessDialogFragment {
            val bundle = Bundle()
            bundle.putBoolean(IsHousehold, isHousehold)
            bundle.putBoolean(IsHouseholdMember, isMember)
            bundle.putBoolean(isPhuWalkInsFlow, isPhuLink)
            val fragment =  SuccessDialogFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setListener()
        attachObserver()
    }

    private fun setListener() {
        binding.btnDone.safeClickListener(this)
    }

    private fun attachObserver() {
        binding.householdNo.invisible()

        if (arguments?.getBoolean(IsHousehold) == true) {
            binding.successMessage.text = getString(R.string.household_successfully)
        }

        if (arguments?.getBoolean(IsHouseholdMember) == true) {
            binding.successMessage.text = getString(R.string.member_registered_successfully)
        }

        if (arguments?.getBoolean(isPhuWalkInsFlow) == true) {
            binding.successMessage.setPadding(50, 0, 50, 0)
            binding.successMessage.text = getString(R.string.member_registered_successfully_linked)
            binding.householdNo.gone()
        }

    }

    override fun onClick(view: View) {
        when(view.id){
            binding.btnDone.id -> {
                if (arguments?.getBoolean(isPhuWalkInsFlow)==true) {
                    val intent= Intent(requireContext(),PhuWalkInsActivity::class.java)
                    startActivity(intent)
                    requireActivity().finish()
                }else {
                    onDismissListener?.onDialogDismissListener(
                        arguments?.getBoolean(IsHousehold) == true
                    )
                    dismiss()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        onDismissListener = null
    }
}