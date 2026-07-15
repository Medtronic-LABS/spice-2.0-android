package org.medtroniclabs.uhis.ui.patient.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.data.registration.FillMedicineResponse
import org.medtroniclabs.uhis.databinding.RowQualiPharmBinding

class QualiPharmAdapter(val list: ArrayList<FillMedicineResponse>) : RecyclerView.Adapter<QualiPharmAdapter.QualiPharmViewHolder>() {
    class QualiPharmViewHolder(val binding: RowQualiPharmBinding) : RecyclerView.ViewHolder(binding.root) {
        val context = binding.root.context
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): QualiPharmViewHolder =
        QualiPharmViewHolder(
            RowQualiPharmBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            ),
        )

    override fun onBindViewHolder(
        holder: QualiPharmViewHolder,
        position: Int,
    ) {
        val model = list[position]
        val pos = position + 1
        holder.binding.tvBullet.text = "$pos."
        holder.binding.tvTitle.text = getMessage(model, holder.context)
    }

    private fun getMessage(
        model: FillMedicineResponse,
        context: Context,
    ): String =
        if ((model.requestedDays > model.dispensedDays) && model.dispensedDays > 0) {
            context.getString(
                R.string.quantity_difference_message,
                model.medicationName,
                model.requestedDays,
                model.dispensedDays,
            )
        } else if (model.requestedDays == model.dispensedDays) {
            context.getString(
                R.string.quantity_success_message,
                model.medicationName,
                model.dispensedDays,
            )
        } else {
            context.getString(R.string.out_of_stock_message, model.medicationName)
        }

    override fun getItemCount(): Int = list.size
}
