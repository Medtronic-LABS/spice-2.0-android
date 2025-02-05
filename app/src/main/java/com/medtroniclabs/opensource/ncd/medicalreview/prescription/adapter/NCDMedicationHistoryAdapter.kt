package com.medtroniclabs.opensource.ncd.medicalreview.prescription.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.data.PatientPrescriptionHistoryResponse
import com.medtroniclabs.opensource.databinding.NcdMedicationHistoryAdapterBinding

class NCDMedicationHistoryAdapter(
    private val medicationLists: ArrayList<PatientPrescriptionHistoryResponse>
) :
    RecyclerView.Adapter<NCDMedicationHistoryAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: NcdMedicationHistoryAdapterBinding) :
        RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        override fun onClick(mView: View?) {
            //View - OnClickListener
        }

        fun bind(position: Int, item: PatientPrescriptionHistoryResponse) {
            val context : Context = binding.root.context
            binding.apply {
                with(item) {
                    tvPrescribedDate.text = createdAt?.let { prescribedAt ->
                        DateUtils.convertDateTimeToDate(
                            getTime(prescribedAt),
                            DateUtils.DATE_FORMAT_yyyyMMddHHmmss,
                            DateUtils.DATE_FORMAT_ddMMMyyyy
                        )
                    } ?: run { context.getString(R.string.hyphen_symbol) }
                    tvDosage.text = validateString(context, dosageUnitValue)
                    tvUnit.text = getDosageUnit(context, dosageUnitName)
                    tvForm.text = validateString(context, dosageFormName)
                    tvFrequency.text = validateString(context, dosageFrequencyName)
                    tvPrescribedDays.text = validateInt(context, prescribedDays)
                    tvInformation.text = validateString(context, instructionNote)
                }
            }
        }
    }

    fun validateString(context: Context, value: String?): String {
        return if (value.isNullOrBlank()) context.getString(R.string.hyphen_symbol) else value
    }

    fun validateInt(context: Context, value: Long?): String {
        return value?.toString() ?: context.getString(R.string.hyphen_symbol)
    }

    private fun getTime(dateFormat: String): String {
        return dateFormat.split("+")[0]
    }

    private fun getDosageUnit(context: Context, dosageUnit: String?): String {
        return dosageUnit?.let {
            var dosage = it
            val str = it.split(" ")
            if (str.isNotEmpty())
                dosage = str[0].trim()
            dosage
        } ?: run { context.getString(R.string.hyphen_symbol) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            NcdMedicationHistoryAdapterBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        medicationLists.let {
            holder.bind(position, it[position])
        }
    }

    override fun getItemCount(): Int = medicationLists.size
}