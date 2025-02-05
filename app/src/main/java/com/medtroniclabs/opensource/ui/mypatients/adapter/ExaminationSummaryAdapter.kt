package com.medtroniclabs.opensource.ui.mypatients.adapter

import android.graphics.Typeface
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.data.resource.ExaminationResult


class ExaminationSummaryAdapter() :
    RecyclerView.Adapter<ExaminationSummaryAdapter.DiseaseViewHolder>() {
    private var examinationResult = mutableListOf<ExaminationResult>()

    class DiseaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val linearLayout: LinearLayout = itemView.findViewById(R.id.examination_disease)
        val titleTextView: AppCompatTextView = itemView.findViewById(R.id.titleTextView)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiseaseViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return DiseaseViewHolder(
            layoutInflater.inflate(
                R.layout.examination_summary,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: DiseaseViewHolder, position: Int) {
        val item = examinationResult[position]
        bindExaminationList(item, holder)
    }

    override fun getItemCount(): Int = examinationResult.size
    fun updateData(newItems: List<ExaminationResult>) {
        this.examinationResult.clear()
        this.examinationResult = newItems.toMutableList()
        notifyDataSetChanged()
    }

    private fun bindExaminationList(diseaseInfo: ExaminationResult, holder: DiseaseViewHolder) {
        val symptoms = diseaseInfo.symptomsTitle
        holder.apply {
            titleTextView.text = symptoms
            titleTextView.setTypeface(null, Typeface.BOLD)
            linearLayout.removeAllViews()  // Clear previous views
            diseaseInfo.description?.forEach { description ->
                val textView = AppCompatTextView(itemView.context).apply {
                    id = View.generateViewId()
                    layoutParams = ConstraintLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, resources.getDimensionPixelSize(R.dimen._4sdp), 0, 0)
                    }
                    setTextColor(ContextCompat.getColor(context, R.color.charcoal_grey))
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen._16ssp))
                    typeface = ResourcesCompat.getFont(context, R.font.inter_medium)
                    text = description
                }
                linearLayout.addView(textView)
            }
        }
    }
}

