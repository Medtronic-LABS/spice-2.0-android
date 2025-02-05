package com.medtroniclabs.opensource.formgeneration.utility

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.db.entity.SignsAndSymptomsEntity
import com.medtroniclabs.opensource.formgeneration.config.DefinedParams
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener

class CheckboxDialogAdapter(
    private val dialogList: List<SignsAndSymptomsEntity>,
    val translate: Boolean = false
) :
    RecyclerView.Adapter<CheckboxDialogAdapter.DialogViewHolder>() {
    inner class DialogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkBox: CheckBox = itemView.findViewById(R.id.checkboxItem)
        val Root: LinearLayout = itemView.findViewById(R.id.root)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DialogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.checkbox_dialog_items, parent, false)
        return DialogViewHolder(view)
    }

    override fun onBindViewHolder(holder: DialogViewHolder, position: Int) {
        val item = dialogList[position]
        holder.checkBox.text = if (translate) item.displayValue ?: item.symptom else item.symptom
        holder.checkBox.isChecked = item.isSelected
        holder.Root.safeClickListener {
            checkDataAndUpdate(item, dialogList)
        }
    }

    private fun checkDataAndUpdate(
        item: SignsAndSymptomsEntity,
        dialogList: List<SignsAndSymptomsEntity>
    ) {
        if (item.symptom.startsWith(DefinedParams.NoSymptoms, true)) {
            updateNoSymptomSelection(item, dialogList)
        } else {
            val model = dialogList.find { it.symptom.startsWith(DefinedParams.NoSymptoms, true) }
            model?.isSelected = false
            item.isSelected = !item.isSelected
        }
        notifyDataSetChanged()
    }

    private fun updateNoSymptomSelection(
        item: SignsAndSymptomsEntity,
        dialogList: List<SignsAndSymptomsEntity>
    ) {
        dialogList.forEach {
            if (it._id == item._id) {
                it.isSelected = !it.isSelected
            } else {
                it.isSelected = false
            }
        }
    }

    override fun getItemCount(): Int = dialogList.size
    fun getSelectedItems(): ArrayList<HashMap<String, Any>> {
        val list = dialogList.filter { it.isSelected }
        val selectedItemList = ArrayList<HashMap<String, Any>>()
        list.forEach {
            val map = HashMap<String, Any>()
            map[DefinedParams.ID] = it._id
            map[DefinedParams.NAME] = it.symptom
            it.displayValue?.let { cultureValue ->
                map[DefinedParams.cultureValue] = cultureValue
            }
            it.value?.let { value ->
                map[DefinedParams.value] = value
            }
            selectedItemList.add(map)
        }
        return selectedItemList
    }
}