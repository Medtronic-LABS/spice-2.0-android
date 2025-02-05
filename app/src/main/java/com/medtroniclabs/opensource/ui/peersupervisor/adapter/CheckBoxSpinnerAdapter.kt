package com.medtroniclabs.opensource.ui.peersupervisor.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.DefinedParams.DefaultIDLabel
import com.medtroniclabs.opensource.data.performance.CheckBoxSpinnerData

class CheckBoxSpinnerAdapter(
    context: Context,
    private val items: MutableList<CheckBoxSpinnerData>,
    private val listener: OnCheckBoxSpinnerListener
) : ArrayAdapter<CheckBoxSpinnerData>(context, 0, items) {

    fun updateData(list: List<CheckBoxSpinnerData>) {
        items.clear()
        items.addAll(list)

        val selectedSize = list.filter { it.isSelected }

        if (items.size > 1)
            items.add(0, CheckBoxSpinnerData(0L, "All", selectedSize.size == list.size))

        notifyDataSetChanged()
    }

    fun updateSelectedItems(list: List<Long?>?) {
        if (list == null) { // if null or empty just select all
            items.forEach { item ->
                item.isSelected = true
            }
        } else {
            // Selected specifi ids
            items.forEach { item ->
                item.isSelected = list.contains(item.id)
            }

            // Select All or Not
            if (items.size > 1) {
                val endIndex = items.size
                val validItems = items.subList(1, endIndex)
                val selectedItems = validItems.filter { it.isSelected }
                items[0].isSelected = (validItems.size == selectedItems.size)
            }
        }

        notifyDataSetChanged()
    }


    override fun getCount(): Int {
        return if (items.isEmpty()) 1 else super.getCount()
    }

    override fun getItem(position: Int): CheckBoxSpinnerData? {
        return if (items.isEmpty()) CheckBoxSpinnerData(-1L, DefaultIDLabel,false) else super.getItem(position)
    }

    override fun isEnabled(position: Int): Boolean {
        return false
    }


    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent, false)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent, true)
    }

    private fun createView(position: Int, convertView: View?, parent: ViewGroup, isDropDown: Boolean): View {
        val view: View = convertView
            ?: LayoutInflater.from(context).inflate(
                if (isDropDown) R.layout.checkbox_spinner_dropdown_item else R.layout.custom_non_select_dropdown,
                parent,
                false
            )

        if (isDropDown) {
            val checkBox = view.findViewById<CheckBox>(R.id.spinnerCheckbox)
            val itemName = view.findViewById<TextView>(R.id.itemName)
            val llRootView = view.findViewById<LinearLayout>(R.id.llRootView)

            val item = items[position]
            itemName.text = item.name
            checkBox.setOnCheckedChangeListener(null)
            checkBox.isChecked = item.isSelected

            llRootView.setOnClickListener {
                item.isSelected = !item.isSelected
                updateList(position, item.isSelected)
                listener.onCheckBoxSpinnerItemClick(items.filter { it.isSelected })
            }
        } else {
            val textView = view.findViewById<TextView>(R.id.spin_txt)
            val selectedItems = getSelectedItems()

            if (selectedItems.isEmpty()) {
                textView.text = DefinedParams.DefaultIDLabel
            } else if (selectedItems[0].id == 0L && selectedItems[0].isSelected) {
                textView.text = selectedItems[0].name
            } else {
                val names = selectedItems.map { it.name }

                textView.text = if (selectedItems.size > 1) {
                    "${names[0]} and ${selectedItems.size - 1} more"
                } else {
                    names[0]
                }
            }
        }

        return view
    }

    fun getSelectedItems(): List<CheckBoxSpinnerData> {
        return items.filter { it.isSelected }.toList()
    }

    private fun updateList(position: Int, status: Boolean) {
        if (items.size > 1) { // With All
            if (position == 0) { // All item clicked
                items.forEach {
                    it.isSelected = status
                }
            } else {
                val endIndex = items.size
                val validItems = items.subList(1, endIndex)
                val selectedItems = validItems.filter { it.isSelected }
                items[0].isSelected = validItems.size == selectedItems.size
            }
        }
        notifyDataSetChanged()
    }

    interface OnCheckBoxSpinnerListener {
        fun onCheckBoxSpinnerItemClick(selectedItems: List<CheckBoxSpinnerData>)
    }

}