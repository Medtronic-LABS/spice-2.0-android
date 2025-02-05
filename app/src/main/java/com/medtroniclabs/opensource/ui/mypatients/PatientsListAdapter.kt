package com.medtroniclabs.opensource.ui.mypatients

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.databinding.ListItemPatientsBinding
import com.medtroniclabs.opensource.formgeneration.extension.capitalizeFirstChar
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.model.PatientListRespModel

class PatientsListAdapter(
    val listener: PatientSelectionListener
) : PagingDataAdapter<PatientListRespModel, PatientsListAdapter.PatientListViewHolder>(
    PatientListComparator
) {

    inner class PatientListViewHolder(val binding: ListItemPatientsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val context: Context = binding.root.context

        fun bind(item: PatientListRespModel) {
            val context = binding.root.context
            val name = item.name ?: context.getString(R.string.separator_hyphen)
            val gender = item.gender?.lowercase()?.capitalizeFirstChar()
                ?: context.getString(R.string.separator_hyphen)

            val formattedAge = item.birthDate?.let { DateUtils.getAgeDescription(it,context) }
                ?: context.getString(R.string.separator_hyphen)
            val age = item.age ?: formattedAge
            val patientInfo = context.getString(
                R.string.household_summary_member_info,
                name,
                age,
                CommonUtils.translatedGender(context, gender)
            )
            var leftLbl = context.getString(R.string.empty)
            var rightLbl = context.getString(R.string.empty)
            var leftValue = context.getString(R.string.empty)
            var rightValue = context.getString(R.string.empty)

            if (CommonUtils.isCommunity()) {
                leftLbl = context.getString(R.string.patient_id)
                leftValue = item.patientId ?: context.getString(R.string.separator_hyphen)
                rightLbl = context.getString(R.string.village)
                rightValue = item.village ?: context.getString(R.string.separator_hyphen)
            } else if (CommonUtils.isNonCommunity()) {
                leftLbl = context.getString(R.string.national_id)
                leftValue = item.identityValue ?: context.getString(R.string.separator_hyphen)
                rightLbl = context.getString(R.string.patient_id)
                rightValue = item.programId ?: context.getString(R.string.separator_hyphen)
            }

            with(binding) {
                tvCardPatientName.text = patientInfo
                tvLeftLbl.text = leftLbl
                tvRightLbl.text = rightLbl
                tvLeftValue.text = leftValue
                tvRightValue.text = rightValue

                getDrawable(clPatientRoot, item.riskColorCode ?: "#FFFFFFFF")
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientListViewHolder {
        return PatientListViewHolder(
            ListItemPatientsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }


    override fun onBindViewHolder(holder: PatientListViewHolder, position: Int) {
        getItem(position)?.let { item ->
            holder.bind(item)
            holder.binding.cardPatient.safeClickListener {
                listener.onSelectedPatient(item)
            }
        }
    }

    object PatientListComparator : DiffUtil.ItemCallback<PatientListRespModel>() {
        override fun areItemsTheSame(
            oldItem: PatientListRespModel,
            newItem: PatientListRespModel
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: PatientListRespModel,
            newItem: PatientListRespModel
        ): Boolean {
            return oldItem.id == newItem.id
        }
    }

    fun getDrawable(view: ConstraintLayout, colorCode: String) {
        if (view.background != null) {
            val drawable = view.background as GradientDrawable
            drawable.mutate()
            drawable.setStroke(3, Color.parseColor(colorCode))
        }
    }
}