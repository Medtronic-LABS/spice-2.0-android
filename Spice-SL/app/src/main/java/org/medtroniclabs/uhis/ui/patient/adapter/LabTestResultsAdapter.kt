package org.medtroniclabs.uhis.ui.patient.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.internal.LinkedTreeMap
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.data.UnitMetricEntity
import org.medtroniclabs.uhis.databinding.LabTestResultItemBinding
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams
import org.medtroniclabs.uhis.formgeneration.extension.capitalizeFirstChar
import org.medtroniclabs.uhis.formgeneration.extension.fetchString
import org.medtroniclabs.uhis.formgeneration.extension.markMandatory
import org.medtroniclabs.uhis.formgeneration.utility.CustomSpinnerAdapter

class LabTestResultsAdapter(private val labTestName: String?) :
    RecyclerView.Adapter<LabTestResultsAdapter.ActivitiesViewHolder>() {
    private val resultsList: ArrayList<HashMap<String, Any>> = ArrayList()
    private val defaultUnitList: ArrayList<UnitMetricEntity> = ArrayList()

    class ActivitiesViewHolder(val binding: LabTestResultItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val context: Context = binding.root.context
    }

    fun setData(
        inputList: ArrayList<HashMap<String, Any>>,
        unitList: ArrayList<UnitMetricEntity>?,
    ) {
        resultsList.addAll(inputList)
        if (unitList != null) {
            defaultUnitList.addAll(unitList)
        }
        notifyItemRangeChanged(0, resultsList.size)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ActivitiesViewHolder =
        ActivitiesViewHolder(
            LabTestResultItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            ),
        )

    fun getResultsList() = resultsList

    override fun onBindViewHolder(
        holder: ActivitiesViewHolder,
        position: Int,
    ) {
        holder.setIsRecyclable(false)
        val data = resultsList[position]
        val resultName = data[DefinedParams.NAME] as String? ?: ""
        if (resultName.isBlank() || resultName.equals("Other", ignoreCase = true)) {
            data[DefinedParams.NAME] = labTestName ?: ""
        }
        holder.binding.tvTitleLbl.text = data[DefinedParams.NAME] as String? ?: ""
        holder.binding.tvTitleLbl.capitalizeFirstChar()
        holder.binding.tvTitleLbl.markMandatory()
        holder.binding.tvUnitLbl.text = holder.context.getString(R.string.unit_text)
        holder.binding.tvUnitLbl.markMandatory()

        val adapter = CustomSpinnerAdapter(holder.context)
        adapter.setData(getUnitsMap(resultsList[position]))
        holder.binding.etUnit.adapter = adapter

        handleValidation(data, holder)

        handleResultValue(data, holder)

        handleUnitDropDown(data, holder, adapter, position)
    }

    private fun handleValidation(
        data: java.util.HashMap<String, Any>,
        holder: ActivitiesViewHolder,
    ) {
        if (data.containsKey(DefinedParams.IS_RESULT_VALUE_VALID)) {
            val isValid = data[DefinedParams.IS_RESULT_VALUE_VALID]
            if (isValid is Boolean && isValid) {
                holder.binding.tvError.visibility = View.GONE
            } else {
                holder.binding.tvError.visibility = View.VISIBLE
                holder.binding.tvError.text =
                    holder.context.getString(R.string.default_user_input_error)
            }
        } else {
            holder.binding.tvError.visibility = View.GONE
        }

        if (data.containsKey(DefinedParams.IS_UNIT_VALID)) {
            val isValid = data[DefinedParams.IS_UNIT_VALID]
            if (isValid is Boolean && isValid) {
                holder.binding.tvUnitError.visibility = View.GONE
            } else {
                holder.binding.tvUnitError.visibility = View.VISIBLE
            }
        } else {
            holder.binding.tvUnitError.visibility = View.GONE
        }
    }

    private fun handleResultValue(
        data: java.util.HashMap<String, Any>,
        holder: ActivitiesViewHolder,
    ) {
        if (data.containsKey(DefinedParams.RESULT_VALUE)) {
            holder.binding.etValue.setText(data[DefinedParams.RESULT_VALUE] as String)
        } else {
            holder.binding.etValue.setText("")
        }

        holder.binding.etValue.addTextChangedListener { editable ->
            if (editable.isNullOrBlank()) {
                if (data.containsKey(DefinedParams.RESULT_VALUE)) {
                    data.remove(DefinedParams.RESULT_VALUE)
                    data[DefinedParams.IS_RESULT_VALUE_VALID] = false
                    // holder.binding.tvError.visibility = View.VISIBLE
                }
            } else {
                data[DefinedParams.RESULT_VALUE] = editable.fetchString()
                // holder.binding.tvError.visibility = View.GONE
                data[DefinedParams.IS_RESULT_VALUE_VALID] = true
            }
        }
    }

    private fun handleUnitDropDown(
        data: java.util.HashMap<String, Any>,
        holder: ActivitiesViewHolder,
        adapter: CustomSpinnerAdapter,
        position: Int,
    ) {
        if (data.containsKey(DefinedParams.UNIT)) {
            val selectedIndex = getIndex(resultsList[position], data[DefinedParams.UNIT])
            if (selectedIndex >= 0) {
                holder.binding.etUnit.setSelection(selectedIndex, true)
            } else {
                holder.binding.etUnit.setSelection(0, true)
            }
        } else {
            holder.binding.etUnit.setSelection(0, true)
        }

        holder.binding.etUnit.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                p0: AdapterView<*>?,
                p1: View?,
                pos: Int,
                p3: Long,
            ) {
                val selectedItem = adapter.getData(position = pos)
                selectedItem?.let {
                    val selectedId = it[DefinedParams.ID]
                    if ((selectedId is Long && selectedId <= 0)) {
                        if (data.containsKey(DefinedParams.UNIT)) {
                            data[DefinedParams.IS_UNIT_VALID] = false
                            data.remove(DefinedParams.UNIT)
                        }
                    } else {
                        data[DefinedParams.UNIT] =
                            it[DefinedParams.NAME] as Any
                        data[DefinedParams.IS_UNIT_VALID] = true
                    }
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                /**
                 * this method is not used
                 */
            }
        }
    }

    private fun getIndex(
        currentItem: HashMap<String, Any>,
        unitValue: Any?,
    ): Int {
        var selectedIndex = -1
        if (currentItem.containsKey(DefinedParams.LAB_RESULT_RANGE)) {
            val list = currentItem[DefinedParams.LAB_RESULT_RANGE]
            if (list is ArrayList<*>) {
                list.forEachIndexed { index, range ->
                    if (range is LinkedTreeMap<*, *> && range.containsKey(DefinedParams.UNIT)) {
                        val spinnerVal = range[DefinedParams.UNIT] as String
                        getSelectedIndex(index, spinnerVal, unitValue)?.let {
                            selectedIndex = it
                        }
                        return@forEachIndexed
                    }
                }
            }
        }
        if (selectedIndex == -1 && defaultUnitList.isNotEmpty()) {
            selectedIndex = defaultUnitList.indexOfFirst {
                it.unit == if (unitValue is String) unitValue else ""
            }
        }

        // +1 is included because we have added '--Select--' at the 0th index
        return if (selectedIndex != -1) selectedIndex + 1 else 0
    }

    private fun getSelectedIndex(
        index: Int,
        spinnerVal: String,
        unitValue: Any?,
    ): Int? = if (spinnerVal == fetchChosenVal(unitValue)) index else null

    private fun fetchChosenVal(unitValue: Any?): String =
        if (unitValue is String) {
            unitValue
        } else {
            ""
        }

    private fun getUnitsMap(currentItem: HashMap<String, Any>): ArrayList<Map<String, Any>> {
        val unitList = ArrayList<Map<String, Any>>()

        if (currentItem.containsKey(DefinedParams.LAB_RESULT_RANGE)) {
            val list = currentItem[DefinedParams.LAB_RESULT_RANGE]
            if (list is ArrayList<*>) {
                list.forEach { range ->
                    if (range is LinkedTreeMap<*, *> && range.containsKey(DefinedParams.UNIT)) {
                        val map = HashMap<String, Any>()
                        map[DefinedParams.ID] = (range[DefinedParams.ID] as Double).toLong()
                        map[DefinedParams.NAME] = range[DefinedParams.UNIT] as String
                        unitList.add(map)
                    }
                }
            }
        }

        if (unitList.isEmpty() && defaultUnitList.isNotEmpty()) {
            defaultUnitList.forEach { unit ->
                val unitMap = HashMap<String, Any>()
                unitMap[DefinedParams.ID] = unit.id
                unitMap[DefinedParams.NAME] = unit.unit
                unitList.add(unitMap)
            }
        }

        val hashMap = HashMap<String, Any>()
        hashMap[DefinedParams.ID] = DefinedParams.DEFAULT_SELECT_ID
        hashMap[DefinedParams.NAME] = DefinedParams.DEFAULT_ID_LABEL
        unitList.add(0, hashMap)

        return unitList
    }

    override fun getItemCount(): Int = resultsList.size

    fun validateInputs(): Boolean {
        var isValid = true
        resultsList.forEachIndexed { _, hashMap ->
            validateResultValue(hashMap)?.let {
                isValid = it
            }

            validateUnitValue(hashMap)?.let {
                isValid = it
            }
            if (hashMap.containsKey(DefinedParams.ID) && hashMap[DefinedParams.ID] is Number) {
                hashMap[DefinedParams.ID] = (hashMap[DefinedParams.ID] as Number).toLong()
            }
        }
        return isValid
    }

    private fun validateResultValue(hashMap: java.util.HashMap<String, Any>): Boolean? {
        var isValid: Boolean? = null
        if (hashMap.containsKey(DefinedParams.RESULT_VALUE)) {
            if ((hashMap[DefinedParams.RESULT_VALUE] as String?).isNullOrBlank()) {
                hashMap[DefinedParams.IS_RESULT_VALUE_VALID] = false
                isValid = false
            } else {
                hashMap[DefinedParams.IS_RESULT_VALUE_VALID] = true
            }
        } else {
            hashMap[DefinedParams.IS_RESULT_VALUE_VALID] = false
            isValid = false
        }
        return isValid
    }

    private fun validateUnitValue(hashMap: java.util.HashMap<String, Any>): Boolean? {
        var isValid: Boolean? = null
        if (hashMap.containsKey(DefinedParams.UNIT)) {
            if ((hashMap[DefinedParams.UNIT] as String?).isNullOrBlank()) {
                hashMap[DefinedParams.IS_UNIT_VALID] = false
                isValid = false
            } else {
                if ((hashMap[DefinedParams.UNIT] as String) == DefinedParams.DEFAULT_ID_LABEL) {
                    hashMap[DefinedParams.IS_UNIT_VALID] = false
                    isValid = false
                } else {
                    hashMap[DefinedParams.IS_UNIT_VALID] = true
                }
            }
        } else {
            hashMap[DefinedParams.IS_UNIT_VALID] = false
            isValid = false
        }
        return isValid
    }
}
