package org.medtroniclabs.uhis.ui.followup.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.convertToLocalDateTime
import org.medtroniclabs.uhis.appextensions.getPatientStatus
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.invisible
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ
import org.medtroniclabs.uhis.common.DateUtils.DATE_TIME_CALL_DISPLAY_FORMAT
import org.medtroniclabs.uhis.common.DateUtils.DATE_ddMMyyyy
import org.medtroniclabs.uhis.data.FollowUpPatientModel
import org.medtroniclabs.uhis.databinding.LayoutItemMyPatientBinding
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.ui.assessment.utils.AssessmentUtil
import org.medtroniclabs.uhis.ui.followup.FollowUpDefinedParams.FU_TYPE_HH_VISIT
import org.medtroniclabs.uhis.ui.followup.FollowUpDefinedParams.FU_TYPE_MEDICAL_REVIEW
import org.medtroniclabs.uhis.ui.followup.FollowUpDefinedParams.FU_TYPE_REFERRED
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
                tvRemainingCount.visible()
                callButton.isEnabled = data.isValidNumber()
                assessmentButton.visible()

                groupReason.visible()
                groupVisitDate.gone()
                groupServiceDate.gone()
                groupPatientStatus.gone()
                groupLastCall.visible()

                tvService.text = AssessmentUtil.mapServiceToServiceName(data.encounterName ?: "", context)

                when (data.type) {
                    FU_TYPE_HH_VISIT -> {
                        groupVisitDate.visible()
                        groupPatientStatus.visible()
                        groupLastCall.gone()
                        tvNextVisitDate.text = data.nextVisitDate?.let {
                            DateUtils.convertDateFormat(
                                data.nextVisitDate,
                                DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                                DATE_ddMMyyyy,
                            )
                        } ?: context.getString(R.string.separator_double_hyphen)
                        tvLastVisitDate.text = data.encounterDate?.let {
                            DateUtils.convertDateFormat(
                                data.encounterDate,
                                DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                                DATE_ddMMyyyy,
                            )
                        } ?: context.getString(R.string.separator_double_hyphen)

                        callButton.gone()
                        tvRemainingCount.gone()
                        setOverDueInfo(context, data.nextVisitDate, tvDueInformation)
                    }

                    FU_TYPE_REFERRED -> {
                        assessmentButton.invisible()
                        groupServiceDate.visible()
                        tvServiceDate.text = data.encounterDate?.let {
                            DateUtils.convertDateFormat(
                                data.encounterDate,
                                DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                                DATE_ddMMyyyy,
                            )
                        } ?: context.getString(R.string.separator_double_hyphen)
                        setOverDueInfo(context, data.encounterDate, tvDueInformation, referralDayLimit)
                    }

                    FU_TYPE_MEDICAL_REVIEW -> {
                        assessmentButton.invisible()
                        groupReason.gone()
                        setOverDueInfo(context, data.nextVisitDate, tvDueInformation)
                    }
                }

                tvReason.text = data.getReason(context.getString(R.string.hyphen_symbol))
                tvPatientStatus.text = context.getPatientStatus(data.patientStatus) ?: context.getString(R.string.hyphen_symbol)
                tvLastCallAtValue.text = data.calledAt?.convertToLocalDateTime(format = DATE_TIME_CALL_DISPLAY_FORMAT) ?: "--"
                tvRemainingCount.text = CommonUtils.formatCountForCurrentLocale(data.remainingAttempts)

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
        gender: String?,
    ): String =
        context.getString(
            R.string.household_summary_member_info,
            name,
            CommonUtils.getAgeFromDOB(
                dob,
                context,
            ),
            CommonUtils.getGenderText(gender, context),
        )

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): PatientViewHolder =
        PatientViewHolder(
            LayoutItemMyPatientBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            ),
        )

    override fun getItemCount(): Int = listOfPatient.size

    override fun onBindViewHolder(
        holder: PatientViewHolder,
        position: Int,
    ) {
        holder.bind(listOfPatient[position])
    }

    private fun setOverDueInfo(
        context: Context,
        dateString: String?,
        tv: TextView,
        adjust: Int = 0,
    ) {
        if (dateString != null) {
            val currentDate = LocalDate.now()
            val dateTime = OffsetDateTime.parse(dateString, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            val date = dateTime.toLocalDate().plusDays(adjust.toLong())
            val daysBetween = ChronoUnit.DAYS.between(currentDate, date)

            when {
                daysBetween < 0L -> {
                    val text = if (daysBetween == -1L) {
                        context.getString(R.string.day_overdue, CommonUtils.formatCountForCurrentLocale(-daysBetween.toInt()))
                    } else {
                        context.getString(R.string.days_overdue, CommonUtils.formatCountForCurrentLocale(-daysBetween.toInt()))
                    }

                    tv.text = text
                    tv.setTextColor("#994242".toColorInt())
                }

                daysBetween == 0L -> {
                    tv.text = context.getString(R.string.today)
                    tv.setTextColor("#EB956A".toColorInt())
                }

                daysBetween == 1L -> {
                    tv.text = context.getString(R.string.tomorrow)
                    tv.setTextColor("#54CC90".toColorInt())
                }

                daysBetween > 1L -> {
                    tv.text = context.getString(R.string.upcoming_in_days, CommonUtils.formatCountForCurrentLocale(daysBetween.toInt()))
                    tv.setTextColor("#54CC90".toColorInt())
                }

                else -> {
                    tv.text = "--"
                    tv.setTextColor("#54CC90".toColorInt())
                }
            }
        } else {
            tv.text = "--"
            tv.setTextColor("#54CC90".toColorInt())
        }
    }
}
