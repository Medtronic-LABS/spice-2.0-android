package com.medtroniclabs.opensource.ncd.medicalreview.prescription.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.setWidth
import com.medtroniclabs.opensource.data.PatientPrescriptionHistoryResponse
import com.medtroniclabs.opensource.databinding.FragmentNcdMedicationHistoryDialogBinding
import com.medtroniclabs.opensource.formgeneration.extension.parcelableArrayList
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.ncd.medicalreview.prescription.adapter.NCDMedicationHistoryAdapter


class NCDMedicationHistoryDialog : DialogFragment(),
    View.OnClickListener {

    private lateinit var binding: FragmentNcdMedicationHistoryDialogBinding
    private lateinit var medicationHistoryAdapter: NCDMedicationHistoryAdapter
    private lateinit var prescriptionLists: ArrayList<PatientPrescriptionHistoryResponse>

    companion object {
        const val TAG = "NCDMedicationHistoryDialog"
        const val KEY_LIST = "KEY_LIST"
        fun newInstance(prescriptionList: ArrayList<PatientPrescriptionHistoryResponse>): NCDMedicationHistoryDialog {
            val fragment = NCDMedicationHistoryDialog()
            fragment.arguments = Bundle().apply {
                putParcelableArrayList(KEY_LIST, prescriptionList)
            }
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNcdMedicationHistoryDialogBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = false
        readArguments()
        initializeView()
        setListeners()
    }

    override fun onStart() {
        super.onStart()
        handleDialogSize()
    }

    private fun handleDialogSize() {
        setWidth(90)
    }

    private fun initializeView() {
        isCancelable = false
        if (prescriptionLists.size > 0) {
            medicationHistoryAdapter = NCDMedicationHistoryAdapter(prescriptionLists)
            binding.rvMedicationHistory.layoutManager = LinearLayoutManager(requireContext())
            binding.rvMedicationHistory.adapter = medicationHistoryAdapter
            binding.tvNoRecord.visibility = View.GONE
            binding.rvMedicationHistory.visibility = View.VISIBLE
            val titleCard = getString(R.string.medication_history) + " - " + prescriptionLists[0].medicationName
            binding.tvTitle.text = titleCard
        } else {
            binding.tvNoRecord.visibility = View.VISIBLE
            binding.rvMedicationHistory.visibility = View.GONE
        }
    }

    private fun setListeners() {
        binding.tvTitle.safeClickListener(this)
        binding.btnClose.safeClickListener(this)
    }

    private fun readArguments() {
        arguments?.let { args ->
            if (args.containsKey(KEY_LIST))
                prescriptionLists = args.parcelableArrayList(KEY_LIST) ?: ArrayList()
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.tvTitle.id -> dismiss()
            binding.btnClose.id -> dismiss()
        }
    }
}