package org.medtroniclabs.uhis.ui.patient.adapter

import android.animation.ObjectAnimator
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.data.registration.LabTestModel
import org.medtroniclabs.uhis.databinding.LabtestInvestigationNurseAdapterBinding
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener

class LabtestHistoryAdapter(var itemRemove: Boolean, var type: Int? = null) :
    RecyclerView.Adapter<LabtestHistoryAdapter.ViewHolder>() {
    var adapterList = ArrayList<LabTestModel>()
    private var selectedItem: Long = -1L

    inner class ViewHolder(val binding: LabtestInvestigationNurseAdapterBinding) :
        RecyclerView.ViewHolder(binding.root), View.OnClickListener {
        val context: Context = binding.root.context

        fun bind(
            position: Int,
            item: LabTestModel,
        ) {
            binding.tvTestName.text = item.labTestName ?: "-"
            binding.tvRefOn.text = "-"
            binding.tvTestedOn.text = "-"

            item.referDateDisplay?.let {
                binding.tvRefOn.text = DateUtils.convertDateTimeToDate(
                    it,
                    DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                    DateUtils.DATE_FORMAT_ddMMMyyyy,
                )
            }

            binding.ivDot.visibility = if (item.isAbnormal == true) View.VISIBLE else View.GONE

            item.referredDate?.let {
                binding.tvRefOn.text = DateUtils.convertDateTimeToDate(
                    it,
                    DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                    DateUtils.DATE_FORMAT_ddMMMyyyy,
                )
            }

            item.resultDate?.let {
                binding.tvTestedOn.text = DateUtils.convertDateTimeToDate(
                    it,
                    DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                    DateUtils.DATE_FORMAT_ddMMMyyyy,
                )
            }
            binding.tvRefBy.text = "-"
//
            binding.tvRefBy.text = getReferredBy(item)

            binding.tvRefBy.setTextColor(getTextColor(context, item.referredBy))

            binding.tvValue.text = context.getString(R.string.not_available)
            getResultUpdatedBy(item)?.let {
                binding.tvValue.text = it
            }

            binding.tvValue.setTextColor(getTextColor(context, item.resultUpdateBy))

            binding.ivDelete.visibility = View.GONE
            binding.ivDropDown.visibility =
                if (item.resultDate.isNullOrBlank()) View.GONE else View.VISIBLE

            binding.ivEdit.visibility =
                if (item.referredDate != null && item.resultDate.isNullOrEmpty()) View.VISIBLE else View.GONE

            binding.ivRemove.visibility =
                if (item.isReviewed == true || item.resultDate != null) View.GONE else View.VISIBLE
            if (itemRemove) {
                binding.ivRemove.visibility = View.GONE
                binding.ivDelete.visibility = View.GONE
                binding.ivEdit.visibility = View.GONE
                binding.tvValue.visibility = View.GONE
            }
            if (type == 2) {
//                binding.ivRemove.visibility = View.GONE
                binding.ivDelete.visibility = View.GONE
                binding.tvValue.visibility = View.GONE
                binding.tvTestedOn.visibility = View.GONE
            }
            if (item._id == null) {
                binding.ivEdit.visibility = View.GONE
            }
            binding.ivEdit.safeClickListener(this)
            binding.ivDropDown.safeClickListener(this)
            binding.tvTestName.safeClickListener(this)
            binding.ivRemove.safeClickListener(this)
            binding.btnReview.safeClickListener(this)

            binding.etComment.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    p0: CharSequence?,
                    p1: Int,
                    p2: Int,
                    p3: Int,
                ) {
                    /**
                     * this method is not used
                     */
                }

                override fun onTextChanged(
                    text: CharSequence?,
                    p1: Int,
                    p2: Int,
                    p3: Int,
                ) {
                    item.resultComments = if (text.isNullOrBlank()) "" else text.toString()
                }

                override fun afterTextChanged(p0: Editable?) {
                    /**
                     * this method is not used
                     */
                }
            })

            binding.ivDropDown.rotation = 0f

            setResultAdapter(context, item, binding)
        }

        override fun onClick(mView: View?) {
            when (mView?.id) {
                binding.ivRemove.id -> {
                    handleRemove(context, layoutPosition)
                }

                binding.ivEdit.id -> {
                    if (layoutPosition < adapterList.size) {
                        adapterList[layoutPosition].let {
                            anInterface?.onItemSelected(
                                it,
                                isRemove = false,
                                loadResult = false,
                            )
                        }
                    }
                }

                binding.tvTestName.id -> {
                    if (binding.ivDropDown.visibility == View.VISIBLE) {
                        binding.ivDropDown.performClick()
                    }
                }

                binding.ivDropDown.id -> {
                    if (binding.resultsLayout.visibility == View.GONE) {
                        if (layoutPosition < adapterList.size) {
                            selectedItem = adapterList[layoutPosition]._id ?: -1L
                            adapterList[layoutPosition].let {
                                anInterface?.onItemSelected(
                                    it,
                                    isRemove = false,
                                    loadResult = true,
                                )
                            }
                        }
                    } else {
                        rotateArrow0f(binding.ivDropDown)
                        binding.resultsLayout.visibility = View.GONE
                    }
                }

                binding.btnReview.id -> {
                    if (layoutPosition < adapterList.size) {
                        selectedItem = adapterList[layoutPosition]._id ?: -1L
                        adapterList[layoutPosition].let {
                            anInterface?.reviewResults(it)
                        }
                    }
                }
            }
        }
    }

    private fun handleRemove(
        context: Context,
        layoutPosition: Int,
    ) {
        if (layoutPosition < adapterList.size) {
            adapterList[layoutPosition].let {
                adapterList.removeAt(layoutPosition)
                notifyItemRemoved(layoutPosition)
                anInterface?.onItemSelected(
                    it,
                    isRemove = true,
                    loadResult = false,
                )
            }
        }
    }

    private fun setResultAdapter(
        context: Context,
        item: LabTestModel,
        binding: LabtestInvestigationNurseAdapterBinding,
    ) {
        if (item._id == selectedItem) {
            binding.resultsLayout.visibility = View.VISIBLE
            rotateArrow180f(binding.ivDropDown)
            if ((item.resultDetails?.size ?: 0) > 0) {
                binding.resultsGrid.visibility = View.VISIBLE
                val resultAdapter = ResultsAdapter(item.resultDetails!!)
                binding.resultsGrid.layoutManager =
                    GridLayoutManager(context, 1, RecyclerView.VERTICAL, false)
                binding.resultsGrid.adapter = resultAdapter

                if (item.isReviewed == true) {
                    binding.reviewGrp.visibility = View.GONE
//                    binding.commentLayout.root.visibility = View.VISIBLE
                    binding.commentLayout.tvKey.text = context.getString(R.string.test_comments)
                    binding.commentLayout.tvValue.text =
                        if (item.resultComments.isNullOrBlank()) "-" else item.resultComments
                } else {
                    binding.commentLayout.root.visibility = View.GONE
                    binding.etComment.setText("")
                }
            } else {
                binding.resultsLayout.visibility = View.GONE
            }
            selectedItem = -1L
        } else {
            binding.resultsLayout.visibility = View.GONE
            item.resultDetails = null
        }
    }

    private fun getResultUpdatedBy(item: LabTestModel): String? {
        item.resultUpdateBy?.let { map ->
            var text = ""
            if (map.containsKey(DefinedParams.FIRST_NAME)) {
                text = map[DefinedParams.FIRST_NAME] as String
            }
            if (map.containsKey(DefinedParams.LAST_NAME)) {
                text = "$text ${map[DefinedParams.LAST_NAME] as String}"
            }
            return text
        }
        return null
    }

    private fun getReferredBy(item: LabTestModel): String {
        var referredBy = ""
        if (item.referredBy != null && item.referredBy is Map<*, *>) {
            (item.referredBy as Map<*, *>).let { map ->
                if (map.containsKey(DefinedParams.FIRST_NAME)) {
                    referredBy = map[DefinedParams.FIRST_NAME] as String
                }
                if (map.containsKey(DefinedParams.LAST_NAME)) {
                    referredBy = "$referredBy ${map[DefinedParams.LAST_NAME] as String}"
                }
            }
        } else {
            referredBy = item.referredByDisplay ?: ""
        }
        return referredBy
    }

    private fun rotateArrow180f(view: View) {
        val ivArrow = ObjectAnimator.ofFloat(view, "rotation", 0f, 180f)
        ivArrow.start()
    }

    private fun rotateArrow0f(view: View) {
        val ivArrow = ObjectAnimator.ofFloat(view, "rotation", 180f, 0f)
        ivArrow.start()
    }

    private fun getTextColor(
        context: Context,
        enteredBy: Any?,
    ): Int =
        if (enteredBy == null) {
            context.getColor(R.color.disabled_text_color)
        } else {
            context.getColor(
                R.color.navy_blue,
            )
        }

    fun getData() = adapterList

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder =
        ViewHolder(
            LabtestInvestigationNurseAdapterBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            ),
        )

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        adapterList.let {
            holder.bind(position, it[position])
        }
    }

    override fun getItemCount(): Int = adapterList.size

    fun submitData(list: ArrayList<LabTestModel>) {
        if (adapterList.isEmpty()) {
            adapterList.clear()
        }
        adapterList = ArrayList(list)
        notifyItemRangeChanged(0, adapterList.size)
    }

    fun showResultDetails(
        resultsData: List<Map<String, Any>>,
        comment: String?,
    ) {
        adapterList.find { it._id == selectedItem }?.let { model ->
            model.resultDetails = ArrayList(resultsData)
            model.resultComments = comment
        }
        notifyItemRangeChanged(0, adapterList.size)
    }

    fun updateReviewCommentsView() {
        adapterList.find { it._id == selectedItem }?.let { model ->
            model.isReviewed = true
            notifyItemRangeChanged(0, adapterList.size)
        }
    }

    private var anInterface: LabTestInterface? = null

    fun setanInterfaceListener(listener: LabTestInterface) {
        this.anInterface = listener
    }

    interface LabTestInterface {
        fun onItemSelected(
            model: LabTestModel,
            isRemove: Boolean,
            loadResult: Boolean,
        )

        fun setButtonEnabled(isEnabled: Boolean)

        fun reviewResults(model: LabTestModel)
    }

    fun removeList(removeName: String) {
        adapterList.removeAll { it.testName == removeName }
        notifyDataSetChanged()
    }
}
