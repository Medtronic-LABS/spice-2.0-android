package org.medtroniclabs.uhis.ui.patient.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.data.registration.VisitDateModel
import org.medtroniclabs.uhis.databinding.LayoutDateListAdapterBinding
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.ui.patient.util.DateSelectionListener

class DateListAdapter(
    val list: ArrayList<VisitDateModel>,
    var selectedPatientId: Long?,
    val listener: DateSelectionListener,
) :
    RecyclerView.Adapter<DateListAdapter.DateViewHolder>() {
    class DateViewHolder(val binding: LayoutDateListAdapterBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): DateViewHolder =
        DateViewHolder(
            LayoutDateListAdapterBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            ),
        )

    override fun onBindViewHolder(
        holder: DateViewHolder,
        position: Int,
    ) {
        val model = list[position]
        holder.binding.tvDate.text = DateUtils.convertDateTimeToDate(
            model.visitDate,
            DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
            DateUtils.DATE_FORMAT_ddMMMyyyy,
        )
        if (selectedPatientId == model._id) {
            holder.binding.ivSelected.visibility = View.VISIBLE
        } else {
            holder.binding.ivSelected.visibility = View.GONE
        }
        holder.binding.root.safeClickListener {
            listener.onDateSelected(model._id)
        }
    }

    override fun getItemCount(): Int = list.size
}
