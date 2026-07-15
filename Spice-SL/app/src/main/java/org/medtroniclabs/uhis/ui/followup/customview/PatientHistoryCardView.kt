package org.medtroniclabs.uhis.ui.followup.customview

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.data.servicerecipient.PatientHistoryData
import org.medtroniclabs.uhis.data.servicerecipient.PatientHistoryDataItem
import org.medtroniclabs.uhis.databinding.ViewPatientHistoryCardBinding

class PatientHistoryCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : CardView(context, attrs, defStyleAttr) {
    private val binding: ViewPatientHistoryCardBinding =
        ViewPatientHistoryCardBinding.inflate(LayoutInflater.from(context), this, true)

    private val adapter = PatientHistoryAdapter()
    private var currentPageIndex = 0
    private lateinit var dataList: List<List<PatientHistoryDataItem>>

    init {
        binding.rvHistoryList.layoutManager = LinearLayoutManager(context)
        binding.rvHistoryList.adapter = adapter

        binding.ivGoToPrevious.setOnClickListener {
            currentPageIndex--
            populateCurrentPage()
        }

        binding.ivGoToNext.setOnClickListener {
            currentPageIndex++
            populateCurrentPage()
        }
    }

    /** Set title text */
    fun setTitle(title: Int) {
        binding.tvTitle.text = resources.getString(title)
    }

    private fun populateCurrentPage() {
        enableOrDisablePrevButton(currentPageIndex != 0)
        enableOrDisableNextButton(currentPageIndex != dataList.lastIndex)
        adapter.submitData(dataList[currentPageIndex])
    }

    /** Set data to RecyclerView */
    fun setData(data: List<List<PatientHistoryDataItem>>) {
        currentPageIndex = 0
        dataList = data
        populateCurrentPage()
    }

    /** Combined function (optional) */
    fun setContent(data: PatientHistoryData) {
        if (data.isPaginationRequired) {
            binding.ivGoToPrevious.visibility = VISIBLE
            binding.ivGoToNext.visibility = VISIBLE
        } else {
            binding.ivGoToPrevious.visibility = GONE
            binding.ivGoToNext.visibility = GONE
        }

        setTitle(data.title)
        setData(data.items)
    }

    private fun enableOrDisablePrevButton(isEnable: Boolean) {
        binding.ivGoToPrevious.isEnabled = isEnable
        if (isEnable) {
            binding.ivGoToPrevious.setImageResource(R.drawable.ic_arrow_prev_enabled)
        } else {
            binding.ivGoToPrevious.setImageResource(R.drawable.ic_arrow_prev_disabled)
        }
    }

    private fun enableOrDisableNextButton(isEnable: Boolean) {
        binding.ivGoToNext.isEnabled = isEnable
        if (isEnable) {
            binding.ivGoToNext.setImageResource(R.drawable.ic_arrow_next_enabled)
        } else {
            binding.ivGoToNext.setImageResource(R.drawable.ic_arrow_next_disabled)
        }
    }
}
