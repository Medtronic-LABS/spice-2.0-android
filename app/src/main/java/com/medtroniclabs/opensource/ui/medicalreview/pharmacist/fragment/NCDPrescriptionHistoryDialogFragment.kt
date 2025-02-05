package com.medtroniclabs.opensource.ui.medicalreview.pharmacist.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.data.DispensePrescriptionResponse
import com.medtroniclabs.opensource.databinding.NcdPrescriptionHistoryDialogFragmentBinding
import com.medtroniclabs.opensource.formgeneration.extension.parcelableArrayList
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.ui.medicalreview.pharmacist.adapter.NCDPrescriptionHistoryAdapter


class NCDPrescriptionHistoryDialogFragment :
    DialogFragment(),
    View.OnClickListener {

    private lateinit var binding: NcdPrescriptionHistoryDialogFragmentBinding
    private lateinit var prescriptionHistoryAdapter: NCDPrescriptionHistoryAdapter
    private lateinit var prescriptionLists: ArrayList<DispensePrescriptionResponse>

    companion object {
        const val TAG = "PrescriptionSignatureDialogFragment"
        const val PRESCRIPTION_LIST = "PRESCRIPTION_LIST"
        fun newInstance(prescriptionList: ArrayList<DispensePrescriptionResponse>): NCDPrescriptionHistoryDialogFragment {
            val args = Bundle().apply {
                putParcelableArrayList(PRESCRIPTION_LIST, prescriptionList)
            }
            return NCDPrescriptionHistoryDialogFragment().apply {
                arguments = args
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = NcdPrescriptionHistoryDialogFragmentBinding.inflate(inflater, container, false)
        val window: Window? = dialog?.window
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        readArguments()
        initializeView()
        setListeners()
    }

    private fun readArguments() {
        arguments?.parcelableArrayList<DispensePrescriptionResponse>(PRESCRIPTION_LIST)?.let {
            prescriptionLists = it
        }
    }

    private fun setListeners() {
        binding.ivClose.safeClickListener(this)
        binding.btnClose.safeClickListener(this)
    }

    private fun initializeView() {
        isCancelable = false

        if (prescriptionLists.size > 0) {
            prescriptionHistoryAdapter = NCDPrescriptionHistoryAdapter(prescriptionLists)
            binding.rvMedicationHistory.layoutManager = LinearLayoutManager(requireContext())
            binding.rvMedicationHistory.adapter = prescriptionHistoryAdapter
            binding.tvNoRecord.gone()
            binding.rvMedicationHistory.visible()
        } else {
            binding.tvNoRecord.visible()
            binding.rvMedicationHistory.gone()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.ivClose.id -> dismiss()
            binding.btnClose.id -> dismiss()
        }
    }
}