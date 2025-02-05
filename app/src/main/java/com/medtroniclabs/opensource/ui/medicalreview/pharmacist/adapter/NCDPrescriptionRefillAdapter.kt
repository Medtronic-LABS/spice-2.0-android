package com.medtroniclabs.opensource.ui.medicalreview.pharmacist.adapter

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.textOrHyphen
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.data.DispensePrescriptionResponse
import com.medtroniclabs.opensource.databinding.LayoutPrescriptionRefillAdapterBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil

class NCDPrescriptionRefillAdapter :
    RecyclerView.Adapter<NCDPrescriptionRefillAdapter.PrescriptionRefillViewHolder>() {

    var list = ArrayList<DispensePrescriptionResponse>()

    inner class PrescriptionRefillViewHolder(val binding: LayoutPrescriptionRefillAdapterBinding) :
        RecyclerView.ViewHolder(binding.root) {
            private val rootContext: Context = binding.root.context
        fun bind(pos: Int, model: DispensePrescriptionResponse) {
            binding.tvMedicationName.text = model.medicationName.textOrHyphen()
            binding.tvDosage.text = model.dosageUnitValue?.let { getDosageValue(it, model.dosageUnitName) }
            binding.tvFrequency.text = model.dosageFrequencyName.textOrHyphen()
            binding.tvMedicationPrescribedDays.text = model.dispenseRemainingDays.toString()
            binding.ivFormType.setImageDrawable(
                model.dosageFormName?.let {
                    getFormDosage(
                        it,
                        rootContext
                    )
                }
            )
            binding.ivDosageFrom.setImageDrawable(
                model.dosageFormName?.let {
                    getFormDosage(
                        it,
                        rootContext
                    )
                }
            )

            val daysFilled =
                if (model.prescriptionFilledDays == 0 || model.prescriptionFilledDays == null) rootContext.getString(R.string.empty) else "${model.prescriptionFilledDays}"
            binding.tvDaysFilled.setText(daysFilled)
            binding.ivDropDown.safeClickListener {
                list[layoutPosition].let {
                    it.isSelected = !it.isSelected
                }
                notifyItemChanged(layoutPosition)
            }

            binding.tvDaysFilled.addTextChangedListener { editable ->
                list[layoutPosition].let {
                    if (editable.isNullOrBlank()) {
                        it.prescriptionFilledDays = null
                    } else {
                        it.prescriptionFilledDays = editable.toString().toIntOrNull()
                    }
                }
            }

            if (model.isSelected) {
                binding.ivDropDown.setImageDrawable(
                    ContextCompat.getDrawable(
                        rootContext,
                        R.drawable.ic_drop_down_medium_blue
                    )
                )
                binding.prescriptionDropDown.visible()
                binding.tvMedicationName.setTextColor(
                    ContextCompat.getColor(
                        rootContext,
                        R.color.blue
                    )
                )
            } else {
                binding.ivDropDown.setImageDrawable(
                    ContextCompat.getDrawable(
                        rootContext,
                        R.drawable.ic_drop_down_grey
                    )
                )
                binding.prescriptionDropDown.gone()
                binding.tvMedicationName.setTextColor(
                    ContextCompat.getColor(
                        rootContext,
                        R.color.black
                    )
                )
            }

            binding.tvPrescribedDate.text =
                DateUtils.convertDateTimeToDate(
                model.prescribedSince,
                DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                DateUtils.DATE_FORMAT_ddMMMyyyy
            )

            binding.tvDosageForm.text = model.dosageFormName.textOrHyphen()
            binding.tvBrand.text = model.brandName.textOrHyphen()
            binding.tvClassification.text = model.classificationName.textOrHyphen()
            binding.etInstruction.setText(model.instructionNote)
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PrescriptionRefillViewHolder {
        return PrescriptionRefillViewHolder(
            LayoutPrescriptionRefillAdapterBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: PrescriptionRefillViewHolder, position: Int) {
        list.let {
            holder.bind(position, it[position])
        }
    }

    fun getFormDosage(dosageFormName: String, context: Context): Drawable? {

        when (dosageFormName) {
            NCDMRUtil.Injection_Injectable_Solution -> {
                return ContextCompat.getDrawable(context, R.drawable.ic_injection_form)
            }

            NCDMRUtil.Liquid_Oral -> {
                return ContextCompat.getDrawable(context, R.drawable.ic_syrup)
            }

            NCDMRUtil.Tablet -> {
                return ContextCompat.getDrawable(context, R.drawable.ic_tablet)
            }

            NCDMRUtil.Capsule -> {
                return ContextCompat.getDrawable(context, R.drawable.ic_capsule)
            }
        }
        return null
    }
    fun getDosageValue(dosageUnitValue: String, dosageUnitName: String?) = dosageUnitValue + (dosageUnitName ?: "")

    override fun getItemCount(): Int {
        return list.size
    }

    fun setData(list: ArrayList<DispensePrescriptionResponse>) {
        this.list = list
    }
}