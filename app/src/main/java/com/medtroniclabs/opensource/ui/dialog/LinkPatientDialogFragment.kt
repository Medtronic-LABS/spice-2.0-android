package com.medtroniclabs.opensource.ui.dialog

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.databinding.FragmentLinkpatientDialogBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.ui.household.HouseholdActivity
import com.medtroniclabs.opensource.ui.household.HouseholdDefinedParams
import com.medtroniclabs.opensource.ui.landing.OnDialogDismissListener
import com.medtroniclabs.opensource.ui.phuwalkins.listener.LinkSuccessListener

class LinkPatientDialogFragment : DialogFragment(), View.OnClickListener {

    private lateinit var binding: FragmentLinkpatientDialogBinding
    private var onDismissListener: OnDialogDismissListener? = null
    private var onLinkSuccessListener: LinkSuccessListener? = null


    override fun onAttach(context: Context) {
        super.onAttach(context)
        onDismissListener = context as OnDialogDismissListener
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLinkpatientDialogBinding.inflate(layoutInflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        isCancelable = false
        return binding.root
    }

    companion object {
        const val TAG = "LinkPatientDialogFragment"

        fun newInstance(houseHoldID: Long, memberID: Long, fhirMemberId: Long): LinkPatientDialogFragment {
            val bundle = Bundle().apply {
                putLong(HouseholdDefinedParams.ID, houseHoldID)
                putLong(DefinedParams.MemberID, memberID)
                putLong(DefinedParams.FhirMemberID, fhirMemberId)
            } // Assuming memberID is passed as argument}
            val fragment = LinkPatientDialogFragment()
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
    }

    private fun setListener() {
        binding.btnLink.safeClickListener(this)
        binding.btnCancel.safeClickListener(this)
    }


    override fun onClick(view: View) {
        when (view.id) {
            binding.btnLink.id -> {
                val intent = Intent(requireActivity(), HouseholdActivity::class.java)
                intent.putExtra(
                    HouseholdDefinedParams.isPhuWalkInsFlow,
                    true
                )
                intent.putExtra(DefinedParams.MemberID,  arguments?.getLong(DefinedParams.MemberID, -1L))
                intent.putExtra(DefinedParams.FhirMemberID,  arguments?.getLong(DefinedParams.FhirMemberID, -1L))
                intent.putExtra(
                    HouseholdDefinedParams.ID,
                    arguments?.getLong(HouseholdDefinedParams.ID, -1L)
                )
                startActivity(intent)
                dismiss()
            }

            binding.btnCancel.id -> {
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        onDismissListener = null
    }
}