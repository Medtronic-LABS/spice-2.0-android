package com.medtroniclabs.opensource.ui.followup.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.convertToLocalDateTime
import com.medtroniclabs.opensource.appextensions.getPatientStatus
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.invisible
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.DateUtils.DATE_TIME_CALL_DISPLAY_FORMAT
import com.medtroniclabs.opensource.data.FollowUpPatientModel
import com.medtroniclabs.opensource.databinding.LayoutItemMyPatientBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.ui.followup.FollowUpDefinedParams.FU_TYPE_HH_VISIT
import com.medtroniclabs.opensource.ui.followup.FollowUpDefinedParams.FU_TYPE_MEDICAL_REVIEW
import com.medtroniclabs.opensource.ui.followup.FollowUpDefinedParams.FU_TYPE_REFERRED
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class PatientListAdapter(private val callback: (Int, FollowUpPatientModel) -> Unit) :
    RecyclerView.Adapter<PatientListAdapter.PatientViewHolder>() {

    object ConstantPatientListAdapter {
        const val PATIENT_DETAIL = 0
        const val CALL = 1
        const val ASSESSMENT = 2
    }

    private val listOfPatient = mutableListOf<FollowUpPatientModel>()
    private var referralDayLimit = 2

    fun updateReferralDayLimit(limit: Int) {
        referralDayLimit = limit
    }

    fun updateList(list: List<FollowUpPatientModel>) {
        listOfPatient.clear()
        listOfPatient.addAll(list)
        notifyDataSetChanged()
    }

    inner class PatientViewHolder(val binding: LayoutItemMyPatientBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(data: FollowUpPatientModel) {
            val context = binding.root.context

            with(binding) {
                tvPatientName.text =
                    getPatientName(context, data.name, data.dateOfBirth, data.gender)

                callButton.visible()
                assessmentButton.visible()
                tvLabelReason.visible()
                tvReason.visible()
                tvLabelReasonSeperator.visible()
                callButton.isEnabled = !data.isWrongNumber

                when(data.type) {
                    FU_TYPE_HH_VISIT -> {
                        callButton.gone()
                        setOverDueInfo(data.nextVisitDate, tvDueInformation)
                    }

                    FU_TYPE_REFERRED -> {
                        assessmentButton.invisible()
                        setOverDueInfo(data.encounterDate, tvDueInformation, referralDayLimit)
                    }

                    FU_TYPE_MEDICAL_REVIEW -> {
                        assessmentButton.invisible()
                        tvLabelReason.gone()
                        tvReason.gone()
                        tvLabelReasonSeperator.gone()
                        setOverDueInfo(data.nextVisitDate, tvDueInformation)
                    }
                }

                tvReason.text = data.getReason(context.getString(R.string.hyphen_symbol))
                tvPatientStatus.text = context.getPatientStatus(data.patientStatus) ?: context.getString(R.string.hyphen_symbol)
                tvLastCallAtValue.text = data.calledAt?.convertToLocalDateTime(format = DATE_TIME_CALL_DISPLAY_FORMAT) ?: "--"

                root.safeClickListener {
                    callback(ConstantPatientListAdapter.PATIENT_DETAIL, data)
                }

                callButton.safeClickListener {
                    callback(ConstantPatientListAdapter.CALL, data)
                }
                assessmentButton.safeClickListener {
                    callback(ConstantPatientListAdapter.ASSESSMENT, data)
                }
            }
        }
    }

    private fun getPatientName(
        context: Context,
        name: String?,
        dob: String?,
        gender: String?
    ): String {
        return context.getString(
            R.string.household_summary_member_info,
            name,
            CommonUtils.getAgeFromDOB(
                dob,
                context
            ),
            CommonUtils.getGenderText(gender, context)
        )
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
        return PatientViewHolder(
            LayoutItemMyPatientBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return listOfPatient.size
    }

    override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
        holder.bind(listOfPatient[position])
    }

    private fun setOverDueInfo(dateString: String?, tv: TextView, adjust: Int = 0) {
        if (dateString != null) {
            val currentDate = LocalDate.now()
            val dateTime = OffsetDateTime.parse(dateString, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            val date = dateTime.toLocalDate().plusDays(adjust.toLong())
            val daysBetween = ChronoUnit.DAYS.between(currentDate, date)

            when {
                daysBetween < 0L -> {
                    val suffix = if (daysBetween == -1L)
                        "day, Overdue"
                    else
                        "days, Overdue"

                    tv.text = "${-daysBetween} $suffix"
                    tv.setTextColor(Color.parseColor("#994242"))
                }

                daysBetween == 0L -> {
                    tv.text = "Today"
                    tv.setTextColor(Color.parseColor("#EB956A"))
                }

                daysBetween == 1L -> {
                    tv.text = "Tomorrow"
                    tv.setTextColor(Color.parseColor("#54CC90"))
                }

                daysBetween > 1L -> {
                    tv.text = "Upcoming in $daysBetween days"
                    tv.setTextColor(Color.parseColor("#54CC90"))
                }

                else -> {
                    tv.text = "--"
                    tv.setTextColor(Color.parseColor("#54CC90"))
                }
            }
        } else {
            tv.text = "--"
            tv.setTextColor(Color.parseColor("#54CC90"))
        }
    }
}