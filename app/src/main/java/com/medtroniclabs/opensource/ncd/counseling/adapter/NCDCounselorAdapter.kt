package com.medtroniclabs.opensource.ncd.counseling.adapter

import android.animation.ObjectAnimator
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.textOrEmpty
import com.medtroniclabs.opensource.appextensions.textOrHyphen
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.databinding.ListItemCounselorBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.ncd.data.NCDCounselingModel
import com.medtroniclabs.opensource.ncd.counseling.utils.ValidationListener

class NCDCounselorAdapter(private val listener: ValidationListener) :
    RecyclerView.Adapter<NCDCounselorAdapter.ViewHolder>() {
    private var adapterList = ArrayList<NCDCounselingModel>()

    inner class ViewHolder(val binding: ListItemCounselorBinding) :
        RecyclerView.ViewHolder(binding.root), View.OnClickListener {
        val context: Context = binding.root.context

        fun bind(item: NCDCounselingModel) {
            with(item) {
                binding.apply {
                    if (isExpanded) {
                        resultsLayout.visible()
                        rotateArrow180f(ivDropDown)
                    } else {
                        resultsLayout.gone()
                        rotateArrow0f(ivDropDown)
                    }

                    tvClinicalNote.text = clinicianNote.textOrHyphen()

                    val refDate = referredDate?.let {
                        DateUtils.convertDateTimeToDate(
                            it,
                            DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                            DateUtils.DATE_FORMAT_ddMMMyyyy
                        )
                    }
                    tvRefDate.text = refDate.textOrHyphen()

                    tvRefBy.text = referredByDisplay.textOrHyphen()

                    etOtherNotes.setText(counselorAssessment.textOrEmpty())
                    etOtherNotes.addTextChangedListener { value ->
                        adapterList[layoutPosition].apply {
                            this.counselorAssessment = if (value.isNullOrBlank())
                                null
                            else
                                value.toString()
                        }
                        listener.validate()
                    }

                    root.safeClickListener(this@ViewHolder)
                }
            }
        }

        override fun onClick(mView: View?) {
            when (mView?.id) {
                binding.root.id -> {
                    if (layoutPosition < adapterList.size) {
                        val item = adapterList[layoutPosition]
                        if (item.id.isNullOrBlank()) return
                        else {
                            adapterList[layoutPosition].let {
                                it.isExpanded = !it.isExpanded
                            }
                            notifyItemChanged(layoutPosition)
                        }
                    }
                }
            }
        }
    }

    private fun rotateArrow180f(view: View) {
        val ivArrow = ObjectAnimator.ofFloat(view, "rotation", 0f, 180f)
        ivArrow.start()
    }

    private fun rotateArrow0f(view: View) {
        val ivArrow = ObjectAnimator.ofFloat(view, "rotation", 180f, 0f)
        ivArrow.start()
    }

    private fun getTextColor(context: Context, enteredBy: Any?): Int {
        return if (enteredBy == null) context.getColor(R.color.disabled_text_color) else context.getColor(
            R.color.navy_blue
        )
    }

    fun getData() = adapterList

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ListItemCounselorBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        adapterList.let {
            holder.bind(it[position])
        }
    }

    override fun getItemCount(): Int = adapterList.size

    fun submitData(list: ArrayList<NCDCounselingModel>) {
        adapterList = ArrayList(list)
        notifyItemRangeChanged(0, adapterList.size)
    }
}