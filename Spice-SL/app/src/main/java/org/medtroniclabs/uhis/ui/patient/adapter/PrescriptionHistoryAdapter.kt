package org.medtroniclabs.uhis.ui.patient.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.data.registration.PrescriptionRefillHistoryResponse
import org.medtroniclabs.uhis.databinding.AdapterPrescriptionHistoryBinding

class PrescriptionHistoryAdapter(
    private val prescriptionList: ArrayList<PrescriptionRefillHistoryResponse>,
) :
    RecyclerView.Adapter<PrescriptionHistoryAdapter.ViewHolder>() {
    inner class ViewHolder(val binding: AdapterPrescriptionHistoryBinding) :
        RecyclerView.ViewHolder(binding.root), View.OnClickListener {
        val context: Context = binding.root.context

        override fun onClick(mView: View?) {
            // View - OnClickListener
        }

        fun bind(
            position: Int,
            item: PrescriptionRefillHistoryResponse,
        ) {
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

    private fun getValidValue(
        context: Context,
        modelValue: Any?,
    ): String {
        var value = context.getString(R.string.separator_hyphen)
        modelValue?.let { text ->
            text.toString().isNotBlank().let {
                if (it) {
                    value = text.toString()
                }
            }
        }
        return value
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): PrescriptionHistoryAdapter.ViewHolder =
        ViewHolder(
            AdapterPrescriptionHistoryBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            ),
        )

    override fun onBindViewHolder(
        holder: PrescriptionHistoryAdapter.ViewHolder,
        position: Int,
    ) {
        prescriptionList.let {
            holder.bind(position, it[position])
        }
    }

    override fun getItemCount(): Int = prescriptionList.size
}
