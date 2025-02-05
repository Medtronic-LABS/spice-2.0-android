package com.medtroniclabs.opensource.ncd.medicalreview.prescription.adapter

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.data.Prescription
import com.medtroniclabs.opensource.databinding.NcdDiscontinuedMedicationAdapterBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.ncd.medicalreview.MedicationListener

class NCDDiscontinuedMedicationAdapter(
    private val medicationLists: ArrayList<Prescription>,
    private val medicationListener: MedicationListener
) :
    RecyclerView.Adapter<NCDDiscontinuedMedicationAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: NcdDiscontinuedMedicationAdapterBinding) :
        RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        override fun onClick(mView: View?) {
            when (mView) {
                binding.tvDaysPrescribed -> {
                    medicationListener.openMedicalHistory(medicationLists[layoutPosition].prescriptionId?.toLong())
                }
            }
        }

        fun bind(position: Int, item: Prescription) {
            binding.apply {
                binding.tvDaysPrescribed.paintFlags = Paint.UNDERLINE_TEXT_FLAG
                with(item) {
                    tvDMMedicationName.text = medicationName
                    tvDMDosage.text = dosageUnitValue
                    tvDMUnit.text = getDosageUnit(dosageUnitName ?: "")
                    tvDMForm.text = dosageFormName
                    tvDMFrequency.text = dosageFrequencyName
                    prescribedSince?.let { prescribedSince ->
                        tvStartedFrom.text = DateUtils.convertDateTimeToDate(
                            getTime(prescribedSince),
                            DateUtils.DATE_FORMAT_yyyyMMddHHmmss,
                            DateUtils.DATE_FORMAT_ddMMMyyyy
                        )
                    }
                    discontinuedDate?.let { discontinuedOn ->
                        tvDiscontinuedOn.text = DateUtils.convertDateTimeToDate(
                            getTime(discontinuedOn),
                            DateUtils.DATE_FORMAT_yyyyMMddHHmmss,
                            DateUtils.DATE_FORMAT_ddMMMyyyy
                        )
                    }
                    tvDaysPrescribed.text = prescribedDays.toString()
                }

                tvDaysPrescribed.safeClickListener(this@ViewHolder)
            }
        }

        private fun getDosageUnit(dosageUnit: String): String {
            val dosage: String
            val str = dosageUnit.split(" ")
            dosage = str[0].trim()
            return dosageUnit
        }
    }

    private fun getTime(dateFormat: String): String {
        return dateFormat.split("+")[0]
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            NcdDiscontinuedMedicationAdapterBinding.inflate(
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