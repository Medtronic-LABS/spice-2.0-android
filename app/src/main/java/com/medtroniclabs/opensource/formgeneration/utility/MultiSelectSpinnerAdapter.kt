package com.medtroniclabs.opensource.formgeneration.utility

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.TextView
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.data.model.MultiSelectDropDownModel

class MultiSelectSpinnerAdapter(
    context: Context,
    private val items: List<MultiSelectDropDownModel>,
    private val selectedItems: ArrayList<MultiSelectDropDownModel>
) : ArrayAdapter<MultiSelectDropDownModel>(context, 0, items) {

    private val checkedItems = BooleanArray(items.size)
    private var onItemSelectedListener: OnItemSelectedListener? = null

    interface OnItemSelectedListener {
        fun onItemSelected(
            selectedItems: List<MultiSelectDropDownModel>,
            pos: Int
        )
    }

    init {
        for (i in items.indices) {
            checkedItems[i] = selectedItems.contains(items[i])
        }
    }

    fun setOnItemSelectedListener(listener: OnItemSelectedListener?) {
        this.onItemSelectedListener = listener
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
                if (isDropDown) R.layout.custom_spinner_dropdown_item else R.layout.custom_non_select_dropdown,
                parent,
                false
            )

        val textView = view.findViewById<TextView>(R.id.spin_txt)

        if (isDropDown) {
            val checkBox = view.findViewById<CheckBox>(R.id.spinnerCheckbox)
            val itemName = view.findViewById<TextView>(R.id.itemName)

            itemName.text = items[position].displayValue ?: items[position].name
            checkBox.setOnCheckedChangeListener(null)
            checkBox.isChecked = checkedItems[position]

            checkBox.setOnCheckedChangeListener { _, isChecked ->
                checkedItems[position] = isChecked
                if (isChecked) {
                    if (!selectedItems.contains(items[position])) {
                        selectedItems.add(items[position])
                    }
                } else {
                    selectedItems.remove(items[position])
                }
                notifyDataSetChanged()
                onItemSelectedListener?.onItemSelected(selectedItems, position)
            }

            itemName.setOnClickListener {
                val newCheckedState = !checkBox.isChecked
                checkBox.isChecked = newCheckedState
            }

        } else {
            if (selectedItems.isEmpty()) {
                textView.text = context.getString(R.string.please_select)
            } else {
                val names = ArrayList<String>()
                for (i in getSelectedItems().indices) {
                    names.add(getSelectedItems()[i].name)
                }
                textView.text = if (names.size > 1) {
                    "${names[0]} and ${names.size - 1} more"
                } else {
                    names[0]
                }
            }
        }

        return view
    }

    private fun getSelectedItems(): List<MultiSelectDropDownModel> {
        return selectedItems
    }

    fun reset() {
        selectedItems.clear()
        checkedItems.fill(false)

        //Update adapter
        notifyDataSetChanged()
        onItemSelectedListener?.onItemSelected(selectedItems, -1)
    }
}
