package com.medtroniclabs.opensource.ui.mypatients.fragment

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.setExpandableText
import com.medtroniclabs.opensource.appextensions.takeIfNotNull
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.databinding.PatientInfoItemBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.mappingkey.Screening
import com.medtroniclabs.opensource.toggle.OnToggledListener
import com.medtroniclabs.opensource.toggle.ToggleableView
import com.medtroniclabs.opensource.ui.BaseActivity

class PatientInfoAdapter(
    private val data: List<Map<String, Any?>?> = emptyList(),
    private val fragmentBg: Int,
    private val activity: BaseActivity,
    private val mentalHealthAssessment: (mhPair: Pair<String?, Boolean>) -> Unit,
    private val onItemPregnantDialog: () -> Unit,
    private val onItemToggle: (isChecked: Boolean) -> Unit
) :
    RecyclerView.Adapter<PatientInfoAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: PatientInfoItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        val context: Context = binding.root.context
        fun bind(label: Map<String, Any?>) {
            with(binding) {
                val empty = context.getString(R.string.hyphen_symbol)
                tvLabel.text = (label[DefinedParams.label] as? String).takeIfNotNull(empty)
                tvValue.setExpandableText(
                    (label[DefinedParams.Value] as? String).takeIfNotNull(empty),
                    title = (label[DefinedParams.label] as? String).takeIfNotNull(empty),
                    maxLength = 35,
                    activity = activity
                )
                if (label.containsKey(DefinedParams.color)) {
                    label[DefinedParams.color]?.let {
                        tvValue.setTextColor(label[DefinedParams.color] as Int)
                    }
                }
                val mentalHealthLabels = listOf(
                    context.getString(R.string.phq4_score),
                    context.getString(R.string.suicidal_ideation),
                    context.getString(R.string.cage_aid)
                )

                if (label[DefinedParams.label] != null && mentalHealthLabels.contains(label[DefinedParams.label])) {
                    tvMentalHealth.visible()
                    tvMentalHealth.text = (label[Screening.type] as? String).takeIfNotNull()
                } else {
                    tvMentalHealth.text = context.getString(R.string.empty)
                    tvMentalHealth.gone()
                }
                tvMentalHealth.safeClickListener {
                    val isEdit = tvMentalHealth.text.toString()
                        .equals(context.getString(R.string.edit_assessment), true)
                    mentalHealthAssessment.invoke(
                        Pair(
                            label[DefinedParams.label] as? String?,
                            isEdit
                        )
                    )
                }
                if (label[DefinedParams.label]?.equals(context.getString(R.string.high_risk)) == true) {
                    tvValue.gone()
                    viewToggle.visible()
                    smHighRisk.visible()
                    tvHighRiskPregnancyCriteria.visible()
                    smHighRisk.isOn = label[DefinedParams.Value] as Boolean == true
                    viewToggle.safeClickListener {
                        if (!binding.smHighRisk.isOn) {
                            userConfirms()
                            smHighRisk.setOnToggledListener(toggledListener)
                        }
                    }
                    tvHighRiskPregnancyCriteria.safeClickListener {
                        onItemPregnantDialog.invoke()
                    }
                } else {
                    tvValue.visible()
                    smHighRisk.gone()
                    viewToggle.gone()
                    tvHighRiskPregnancyCriteria.gone()
                }
                root.background = ContextCompat.getDrawable(context, fragmentBg)
            }
        }

        private fun userConfirms() {
            (activity as? BaseActivity?)?.showErrorDialogue(
                context.getString(R.string.alert),
                context.getString(R.string.high_risk_confirmation),
                positiveButtonName = context.getString(R.string.yes),
                isNegativeButtonNeed = true,
                cancelBtnName = context.getString(R.string.no),
                okayBtnEnable = true
            ) {
                if (it)
                    binding.smHighRisk.performClick()
            }
        }
    }

    private val toggledListener = object : OnToggledListener {
        override fun onSwitched(toggleableView: ToggleableView?, isOn: Boolean) {
            onItemToggle.invoke(isOn)
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            PatientInfoItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        data[position]?.let { holder.bind(it) }
    }

}