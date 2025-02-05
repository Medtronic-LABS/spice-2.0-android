package com.medtroniclabs.opensource.ui.medicalreview.pharmacist.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.data.DispensePrescriptionResponse
import com.medtroniclabs.opensource.databinding.PrescriptionHistoryAdapterBinding

class NCDPrescriptionHistoryAdapter(
    private val prescriptionList: ArrayList<DispensePrescriptionResponse>
) :
    RecyclerView.Adapter<NCDPrescriptionHistoryAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: PrescriptionHistoryAdapterBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val context: Context = binding.root.context

        fun bind(position: Int, item: DispensePrescriptionResponse) {
            binding.apply {
                with(item) {
                    tvMedicationName.text = getValidValue(context, medicationName)
                    tvDosage.text = getValidValue(context, dosageUnitValue)
                    tvUnit.text = getValidValue(context, dosageUnitName)
                    tvForm.text = getValidValue(context, dosageFormName)
                    tvFrequency.text = getValidValue(context, dosageFrequencyName)
                    tvPrescribedDays.text = getValidValue(context, prescribedDays)
                    tvFilledDays.text = getValidValue(context, prescriptionFilledDays)
                }
            }
        }
    }

    private fun getValidValue(context: Context, modelValue: Any?): String {
        var value = context.getString(R.string.separator_hyphen)
        modelValue?.let { text ->
            text.toString().isNotBlank().let {
                if (it)
                    value = text.toString()
            }
        }
        return value
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): NCDPrescriptionHistoryAdapter.ViewHolder {
        return ViewHolder(
            PrescriptionHistoryAdapterBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: NCDPrescriptionHistoryAdapter.ViewHolder, position: Int) {
        prescriptionList.let {
            holder.bind(position, it[position])
        }
    }

    override fun getItemCount(): Int = prescriptionList.size
}