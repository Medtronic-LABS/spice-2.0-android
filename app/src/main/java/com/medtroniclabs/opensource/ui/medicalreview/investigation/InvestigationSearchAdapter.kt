package com.medtroniclabs.opensource.ui.medicalreview.investigation

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.databinding.SearchLayoutLabtestBinding
import com.medtroniclabs.opensource.model.medicalreview.SearchLabTestResponse

class InvestigationSearchAdapter(context: Context) :
    ArrayAdapter<SearchLabTestResponse>(context, R.layout.spinner_drop_down_item) {

    private var investigationList = ArrayList<SearchLabTestResponse>()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val viewHolder: ViewHolder
        if (convertView == null) {
            val binding = SearchLayoutLabtestBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            view = binding.root
            viewHolder = ViewHolder(binding)
            viewHolder.bind(getItem(position))
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = convertView.tag as ViewHolder
            viewHolder.bind(getItem(position))
        }
        return view
    }

    class ViewHolder(val binding: SearchLayoutLabtestBinding) {
        fun bind(item: SearchLabTestResponse) {
            binding.tvTitle.text = item.testName
        }
    }


    override fun getCount(): Int = investigationList.size

    override fun getItem(position: Int): SearchLabTestResponse = investigationList[position]

    override fun getItemId(position: Int): Long = position.toLong()

    fun setData(itemList: ArrayList<SearchLabTestResponse>) {
        this.investigationList = itemList
    }


}