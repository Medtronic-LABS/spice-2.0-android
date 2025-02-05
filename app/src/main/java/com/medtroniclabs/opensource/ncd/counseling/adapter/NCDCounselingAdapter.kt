package com.medtroniclabs.opensource.ncd.counseling.adapter

import android.animation.ObjectAnimator
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.setVisible
import com.medtroniclabs.opensource.appextensions.textOrHyphen
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.databinding.AdapterNcdCounselingBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.ncd.data.NCDCounselingModel
import com.medtroniclabs.opensource.ncd.counseling.utils.CounselingInterface
import com.medtroniclabs.opensource.ui.BaseActivity

class NCDCounselingAdapter(private val counselingInterface: CounselingInterface) :
    RecyclerView.Adapter<NCDCounselingAdapter.ViewHolder>() {
    private var adapterList = ArrayList<NCDCounselingModel>()

    inner class ViewHolder(val binding: AdapterNcdCounselingBinding) :
        RecyclerView.ViewHolder(binding.root), View.OnClickListener {
        val context: Context = binding.root.context

        fun bind(item: NCDCounselingModel) {
            with(item) {
                binding.apply {
                    val assessed = !assessedBy.isNullOrBlank()

                    if (isExpanded) {
                        resultsLayout.visibility = View.VISIBLE
                        rotateArrow180f(ivDropDown)
                    } else {
                        resultsLayout.visibility = View.GONE
                        rotateArrow0f(ivDropDown)
                    }

                    counselorNotes.apply {
                        tvKey.text = context.getString(R.string.clinician_notes)
                        tvValue.text = clinicianNote.textOrHyphen()
                    }

                    if (assessed) {
                        counselorNotes.apply {
                            root.visibility = View.VISIBLE
                            tvKey.text = context.getString(R.string.counselor_notes)
                            tvValue.text = counselorAssessment.textOrHyphen()
                        }
                    } else
                        counselorNotes.root.visibility = View.GONE

                    tvReferredFor.text = clinicianNotes?.joinToString(separator = ", ")
                        ?: clinicianNote.textOrHyphen()

                    tvRefDate.text = referredDate?.let {
                        DateUtils.convertDateTimeToDate(
                            it,
                            DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                            DateUtils.DATE_FORMAT_ddMMMyyyy
                        )
                    }.textOrHyphen()

                    tvRefBy.text = referredByDisplay.textOrHyphen()

                    tvAssessedDate.apply {
                        text = assessedDate?.let {
                            DateUtils.convertDateTimeToDate(
                                it,
                                DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                                DateUtils.DATE_FORMAT_ddMMMyyyy
                            )
                        } ?: run {
                            context.getString(R.string.not_assessed)
                        }
                        setTextColor(getTextColor(context, assessedDate))
                    }

                    tvAssessedBy.apply {
                        text = assessedByDisplay ?: run { context.getString(R.string.not_available) }
                        setTextColor(getTextColor(context, assessedByDisplay))
                    }

                    if (id.isNullOrBlank()) {
                        ivRemove.setVisible(!assessed)
                        ivDelete.gone()
                    } else {
                        ivRemove.gone()
                        ivDelete.setVisible(!assessed)
                    }

                    ivDropDown.visibility =
                        if (assessed) View.VISIBLE else View.INVISIBLE

                    ivRemove.safeClickListener(this@ViewHolder)
                    ivDelete.safeClickListener(this@ViewHolder)
                    root.safeClickListener(this@ViewHolder)
                }
            }
        }

        override fun onClick(mView: View?) {
            when (mView?.id) {
                binding.ivRemove.id, binding.ivDelete.id -> {
                    if (layoutPosition < adapterList.size) {
                        adapterList[layoutPosition].let { item ->
                            if (item.id == null)
                                counselingInterface.removeElement(item)
                            else
                                showMessage(context, item)
                        }
                    }
                }

                binding.root.id -> {
                    if (layoutPosition < adapterList.size) {
                        val item = adapterList[layoutPosition]
                        if (item.id.isNullOrBlank() || item.counselorAssessment.isNullOrBlank())
                            return
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

    private fun showMessage(context: Context, item: NCDCounselingModel) {
        if ((context as BaseActivity).connectivityManager.isNetworkAvailable()) {
            context.showErrorDialogue(
                title = context.getString(R.string.confirmation),
                message = context.getString(R.string.delete_confirmation),
                isNegativeButtonNeed = true,
                positiveButtonName = context.getString(R.string.yes)
            ) { isPositiveResult ->
                if (isPositiveResult)
                    counselingInterface.removeElement(item)
            }
        } else {
            context.showErrorDialogue(
                context.getString(R.string.error),
                context.getString(R.string.no_internet_error),
                false,
            ) {}
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
            AdapterNcdCounselingBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
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