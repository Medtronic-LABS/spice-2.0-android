package com.medtroniclabs.opensource.ui.patientTransfer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.databinding.AutoCompleteAdapterBinding
import com.medtroniclabs.opensource.ncd.data.NCDSiteRoleResponse

class NCDRoleUserAutoCompleteAdapter(context: Context): ArrayAdapter<String>(context, R.layout.spinner_drop_down_item)
{
    private var itemList = ArrayList<NCDSiteRoleResponse>()

    override fun getCount(): Int = itemList.size

    override fun getItem(position: Int): String =
        "${itemList[position].firstName} ${itemList[position].lastName} (${itemList[position].username})"


    override fun getItemId(position: Int): Long = position.toLong()

    fun setData(itemList: ArrayList<NCDSiteRoleResponse>) {
        this.itemList = itemList
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val myHolder: ViewHolder

        if (convertView == null) {
            val binding = AutoCompleteAdapterBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            view = binding.root
            myHolder = ViewHolder(binding)
            myHolder.bind(getItem(position))
            view.tag = myHolder
        } else{
            view = convertView
            myHolder = convertView.tag as ViewHolder
            myHolder.bind(getItem(position))
        }

        return view
    }

    class ViewHolder(val binding: AutoCompleteAdapterBinding) {
        fun bind(item: String) {
            binding.tvTitle.text = item
        }
    }
}