package org.medtroniclabs.uhis.ui.household.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.setPadding
import androidx.recyclerview.widget.RecyclerView
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.databinding.SummaryListItemBinding
import org.medtroniclabs.uhis.db.entity.MemberAssessmentHistoryEntity
import org.medtroniclabs.uhis.formgeneration.extension.px
import org.medtroniclabs.uhis.ui.assessment.utils.AssessmentUtil

/**
 * Adapter for showing member assessment history
 */
class MemberAssessmentHistoryAdapter(
    val historyList: List<MemberAssessmentHistoryEntity>,
) : RecyclerView.Adapter<MemberAssessmentHistoryAdapter.ViewHolder>() {
    override fun onCreateViewHolder(
        viewGroup: ViewGroup,
        itemType: Int,
    ): ViewHolder =
        ViewHolder(
            LinearLayout(viewGroup.context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
                setPadding(10.px)
            },
        )

    override fun onBindViewHolder(
        viewHolder: ViewHolder,
        position: Int,
    ) {
        viewHolder.bindData(historyList[position])
    }

    override fun getItemCount() = historyList.size

    class ViewHolder(private val view: LinearLayout) : RecyclerView.ViewHolder(view) {
        fun bindData(history: MemberAssessmentHistoryEntity) {
            val context = view.context
            view.removeAllViews()
            addSummaryView(
                context.getString(R.string.service_name),
                AssessmentUtil.mapServiceToServiceName(history.serviceProvided ?: "", context),
            )
            addSummaryView(
                context.getString(R.string.service_provided_by),
                AssessmentUtil.formatServiceProviderDisplay(
                    context,
                    history.serviceProvidedByName,
                    history.serviceProvidedByRole,
                ),
            )
            val visitDateMillis = DateUtils.getLastMenstrualDate(history.visitDate ?: "").timeInMillis
            addSummaryView(
                context.getString(R.string.service_date),
                DateUtils.formatDateToDisplayFormat(visitDateMillis) ?: "",
            )   
            val service = history.serviceProvided ?: ""
            if (AssessmentUtil.shouldShowServiceObservations(service)) {
                addSummaryView(
                    context.getString(R.string.blood_pressure),
                    AssessmentUtil.formatServiceHistoryBloodPressure(
                        context,
                        history.observations?.bp,
                    ),
                )
                addSummaryView(
                    context.getString(R.string.blood_glucose),
                    AssessmentUtil.formatServiceHistoryBloodGlucose(
                        context,
                        history.observations?.bg,
                    ),
                )
            }
            val currentStatus = AssessmentUtil.formatServiceHistoryCurrentStatus(
                history.customStatus,
                context,
                history.serviceProvided,
                history.referralStatus,
            )
            addSummaryView(
                context.getString(R.string.current_status),
                CommonUtils.getStringElse(currentStatus, context.getString(R.string.separator_double_hyphen)),
            )
            if (AssessmentUtil.shouldShowServiceObservations(service)) {
                addSummaryView(
                    context.getString(R.string.blood_pressure),
                    AssessmentUtil.formatServiceHistoryBloodPressure(
                        context,
                        history.observations?.bp,
                    ),
                )
                addSummaryView(
                    context.getString(R.string.blood_glucose),
                    AssessmentUtil.formatServiceHistoryBloodGlucose(
                        context,
                        history.observations?.bg,
                    ),
                )
            }
            val referralStatus = AssessmentUtil.getReferralStatus(
                context,
                history.serviceProvided ?: "",
                history.referralStatus,
            )
            addSummaryView(
                context.getString(R.string.referral_status),
                referralStatus,
            )
            if (AssessmentUtil.shouldShowNextFollowUpDate(service)) {
                val nextFollowUpDate = AssessmentUtil.getNextFollowUpDate(
                    context,
                    service,
                    history.nextFollowUpDate,
                )
                addSummaryView(
                    context.getString(R.string.next_follow_up_date),
                    nextFollowUpDate,
                )
            }
        }

        private fun addSummaryView(
            name: String,
            value: String,
        ) {
            val binding = SummaryListItemBinding.inflate(LayoutInflater.from(view.context))
            binding.tvLabel.text = name
            binding.tvValue.text = value
            view.addView(binding.root)
        }
    }
}
