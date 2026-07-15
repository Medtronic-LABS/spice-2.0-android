package org.medtroniclabs.uhis.formgeneration.utility

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.db.entity.SignsAndSymptomsEntity
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams.OTHER_METHOD_SPECIFY
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener

class CheckboxDialogAdapter(
    private val dialogList: List<SignsAndSymptomsEntity>,
    val translate: Boolean = false,
    val callback: (() -> Unit)? = null,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
    }

    class DialogHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val separator: View = itemView.findViewById(R.id.seperatorView)
        val checkBoxHeader: TextView = itemView.findViewById(R.id.checkboxItemHeader)
    }

    class DialogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val separator: View = itemView.findViewById(R.id.seperatorView)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkboxItem)
    }

    override fun getItemViewType(position: Int): Int =
        if (dialogList[position].isTitle) {
            VIEW_TYPE_HEADER
        } else {
            VIEW_TYPE_ITEM
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder =
        if (viewType == VIEW_TYPE_HEADER) {
            val view = LayoutInflater
                .from(parent.context)
                .inflate(R.layout.checkbox_dialog_header, parent, false)
            DialogHeaderViewHolder(view)
        } else {
            val view = LayoutInflater
                .from(parent.context)
                .inflate(R.layout.checkbox_dialog_items, parent, false)
            DialogViewHolder(view)
        }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        val item = dialogList[position]
        if (holder is DialogHeaderViewHolder) {
            if (item.displayOrder != 1) {
                holder.separator.visible()
            } else {
                holder.separator.gone()
            }
            holder.checkBoxHeader.text =
                if (translate) item.displayValue ?: item.symptom else item.symptom
        } else if (holder is DialogViewHolder) {
            if (item.symptom == OTHER_METHOD_SPECIFY) {
                holder.separator.visible()
            } else {
                holder.separator.gone()
            }
            holder.checkBox.text =
                if (translate) item.displayValue ?: item.symptom else item.symptom
            holder.checkBox.isChecked = item.isSelected
            holder.checkBox.isEnabled = item.isEnabled
            holder.checkBox.safeClickListener {
                if (item.isEnabled) {
                    checkDataAndUpdate(item, dialogList)
                }
            }
        }
    }

    private fun checkDataAndUpdate(
        item: SignsAndSymptomsEntity,
        dialogList: List<SignsAndSymptomsEntity>,
    ) {
        if (item.isNone()
        ) {
            updateNoSymptomSelection(item, dialogList)
        } else {
            val model = dialogList.find {
                it.isNone()
            }
            model?.isSelected = false
            item.isSelected = !item.isSelected
        }
        notifyItemRangeChanged(0, dialogList.size)
        callback?.invoke()
    }

    private fun updateNoSymptomSelection(
        item: SignsAndSymptomsEntity,
        dialogList: List<SignsAndSymptomsEntity>,
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
                map[DefinedParams.CULTURE_VALUE] = cultureValue
            }
            it.value?.let { value ->
                map[DefinedParams.VALUE] = value
            }
            selectedItemList.add(map)
        }
        return selectedItemList
    }
}
