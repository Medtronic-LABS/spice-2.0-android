package com.medtroniclabs.opensource.ui

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.common.DefinedParams.Other
import com.medtroniclabs.opensource.common.DefinedParams.ENABLED
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.data.model.ChipViewItemModel
import com.medtroniclabs.opensource.databinding.CustomLayoutTagviewComponentBinding
import com.medtroniclabs.opensource.databinding.OtherChipLayoutBinding
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil

class TagListCustomView(
    val context: Context,
    val chipGroup: ChipGroup,
    val otherSingleSelect: Boolean? = null,
    val isSelectionRequired: Boolean? = null,
    val otherCallBack: ((name: String, isChecked: Boolean) -> Unit)? = null,
    val callBack: ((name: String?, isEmpty: Boolean, isChecked: Boolean) -> Unit)? = null
) {
    fun addChipItemList(
        chipItemList: List<ChipViewItemModel>,
        selectedChipItemList: List<ChipViewItemModel>? = null,
        singleSelectionTypeMap: HashMap<String, MutableList<ChipViewItemModel>>? = null
    ) {
        chipGroup.removeAllViews()
        chipItemList.forEach { data ->
            getChipText(data)?.let { chipData ->
                if (chipData.second.startsWith(Other, ignoreCase = true))
                    otherChipBinding(data, chipData, selectedChipItemList)
                else
                    chipBinding(data, chipData, selectedChipItemList, singleSelectionTypeMap)
            }
        }
    }

    private fun chipBinding(
        data: Any,
        chipData: Pair<String?, String>,
        selectedChipItemList: List<ChipViewItemModel>?,
        singleSelectionTypeMap: HashMap<String, MutableList<ChipViewItemModel>>?
    ) {
        val binding = CustomLayoutTagviewComponentBinding.inflate(LayoutInflater.from(context))
        val chip = binding.root
        chip.chipBackgroundColor =
            getColorStateList(
                context.getColor(R.color.primary_medium_blue),
                context.getColor(R.color.white)
            )
        chip.tag = data
        chip.text = getChipViewText(chipData)
        chip.setChipBackgroundColorResource(R.color.diagnosis_confirmation_selector)
        chip.typeface = ResourcesCompat.getFont(context, R.font.inter_regular)
        chip.chipStrokeWidth = 2f
        chip.setTextColor(
            getColorStateList(
                context.getColor(R.color.white),
                context.getColor(R.color.grey_black)
            )
        )
        chip.chipStrokeColor = getColorStateList(
            context.getColor(R.color.primary_medium_blue),
            context.getColor(R.color.mild_gray)
        )
        chip.setOnCheckedChangeListener { _, isChecked ->
            validateOtherSingleSelection(isChecked, chipData)
            if (isChecked) {
                singleSelectionTypeMap?.let {
                    groupItemsEnableDisable(
                        singleSelectionTypeMap,
                        chipGroup,
                        chipData.second,
                        arrayListOf(
                            NCDMRUtil.HYPERTENSION,
                            NCDMRUtil.DIABETES,
                            NCDMRUtil.HIV,
                            NCDMRUtil.PREGNANCY
                        )
                    )
                }
                chip.typeface = ResourcesCompat.getFont(context, R.font.inter_bold)
                chip.chipStrokeWidth = 0f
            } else {
                validateSelectionRequired(chip)
            }
            if (otherCallBack != null)
                otherCallBack.invoke(chipData.second, isChecked)
            else
                callBack?.invoke(chipData.second, chipGroup.checkedChipIds.isEmpty(), isChecked)

            if (chipData.second.equals(context.getString(R.string.none), ignoreCase = true)) {
                if (isChecked)
                    onNoneSelected()
            } else if (isChecked)
                resetNone()
        }

        if (otherSingleSelect == true)
            chipGroup.isSingleSelection = true
        chipGroup.addView(binding.root)

        if (isSelectionRequired == true)
            chipGroup.isSelectionRequired = isSelectionRequired

        autoPopulateChip(selectedChipItemList, data, chip)
    }

    private fun getChipViewText(chipData: Pair<String?, String>): CharSequence? {
        return if(SecuredPreference.getIsTranslationEnabled()) chipData.first else chipData.second
    }

    private fun autoPopulateChip(selectedChipItemList: List<ChipViewItemModel>?, data: Any, chip: Chip) {
        selectedChipItemList?.let { chipItemList ->
            val dataValue = getChipText(data)
            dataValue?.second?.let { chipItem ->
                val isAlreadySelected = chipItemList.any { it.name == chipItem }
                if (isAlreadySelected) {
                    chip.isChecked = true
                }
            }
        }
    }

    fun populateChipByName(
        chipGroup: ChipGroup,
        chipList: List<ChipViewItemModel>,
        data: String,
        callBack: () -> Unit
    ) {
        val tag = chipList.firstOrNull { it.name.equals(data, true) }
        chipGroup.findViewWithTag<Chip>(tag)?.let {
            it.isChecked = true
            callBack.invoke()
        }
    }

    private fun onNoneSelected() {
        for (chipId in chipGroup.checkedChipIds) {
            chipGroup.findViewById<Chip>(chipId)?.let { chip ->
                val actualText = getActualNameOfChip(chip.tag)
                if (!actualText.equals(context.getString(R.string.none), true)) {
                    chip.isChecked = false
                }
            }
        }
        chipGroup.findViewWithTag<LinearLayout>(Other)?.let { chipLayout ->
            chipLayout.getChildAt(1)?.tag?.let {
                chipLayout.getChildAt(0)?.let { view ->
                    (view as AppCompatTextView).performClick()
                }
            }
        }
    }

    private fun resetNone() {
        for (chipId in chipGroup.checkedChipIds) {
            val chip = chipGroup.findViewById<Chip>(chipId)
            var chipActualText: String? = getActualNameOfChip(chip.tag)
            if (chip != null && chipActualText.equals(
                    context.getString(R.string.none),
                    true
                ) && chip.isChecked
            ) {
                chip.isChecked = false
                break
            }
        }
    }

    private fun validateSelectionRequired(chip: Chip) {
        if (isSelectionRequired == true) {
            if (!chipGroup.checkedChipIds.contains(chip.id)) {
                chip.typeface = ResourcesCompat.getFont(context, R.font.inter_regular)
                chip.chipStrokeWidth = 2f
            }
        } else {
            chip.typeface = ResourcesCompat.getFont(context, R.font.inter_regular)
            chip.chipStrokeWidth = 2f
        }
    }

    fun groupItemsEnableDisable(
        singleSelectionTypeMap: HashMap<String, MutableList<ChipViewItemModel>>,
        chipGroup: ChipGroup,
        selectedChip: String,
        keys: List<String>? = null
    ) {
        var groupKey: String? = null
        singleSelectionTypeMap.forEach { (key, value) ->
            val filteredValue = value.find { it.name.equals(selectedChip, true) }
            if (filteredValue != null) {
                groupKey = key
                return@forEach
            }
        }

        val groupValues = groupKey?.let { singleSelectionTypeMap[it] }
        keys?.let {
            if (groupKey in keys && !(groupValues.isNullOrEmpty())) {
                applyChip(groupValues, chipGroup, selectedChip)
            }
        }
    }

    private fun applyChip(
        groupValues: MutableList<ChipViewItemModel>,
        chipGroup: ChipGroup,
        selectedChip: String
    ) {
        for (j in 0 until chipGroup.childCount) {
            for (i in groupValues.indices) {
                val chip = chipGroup.getChildAt(j)
                val tag = chip.tag
                val actualChipName: String? = getActualNameOfChip(tag)
                if (chip is Chip && groupValues[i].name == actualChipName && groupValues[i].name != selectedChip && chip.isChecked) {
                    chip.isChecked = false
                    break
                }
            }
        }
    }

    private fun validateOtherSingleSelection(isChecked: Boolean, chipData: Pair<String?, String>) {
        if (otherSingleSelect == true && isChecked)
            uncheckOtherChip(chipData)
    }

    private fun uncheckOtherChip(chipData: Pair<String?, String>) {
        chipGroup.findViewWithTag<LinearLayout>(Other)?.let { chipLayout ->
            val tvOther = chipLayout.getChildAt(0)
            val tagView = chipLayout.getChildAt(1)
            tagView?.tag?.let {
                if (tvOther != null && tvOther is AppCompatTextView) {
                    otherOnClick(tvOther, tagView, chipData)
                }
            }
        }
    }

    private fun otherChipBinding(
        data: Any,
        chipData: Pair<String?, String>,
        selectedChipItemList: List<ChipViewItemModel>?
    ) {
        val binding = OtherChipLayoutBinding.inflate(LayoutInflater.from(context))
        binding.root.tag = Other
        binding.tvOther.text = getChipViewText(chipData)
        binding.tvOther.tag = data
        binding.tvOther.setOnClickListener { //Don't change it to safeClickListener
            otherOnClick(binding.tvOther, binding.tagView, chipData)
        }
        chipGroup.addView(binding.root)
        /*selectedChipItemList?.let { selectedChipItemList ->
            val isAlreadySelected = selectedChipItemList.any { it.name.startsWith(Other)}
            if (isAlreadySelected) {
                binding.tvOther.performClick()
            }
        }*/
        selectedChipItemList?.let { chipItemList ->
            val dataValue = getChipText(data)
            dataValue?.second?.let { chipItem ->
                if (chipItem.startsWith(Other)){
                    val isAlreadySelected = chipItemList.any { it.name == chipItem }
                    if (isAlreadySelected) {
                        binding.tvOther.performClick()
                    }
                }
            }
        }
    }

    private fun otherOnClick(
        tvOther: AppCompatTextView,
        tagView: View,
        chipData: Pair<String?, String>
    ) {
        if (tagView.tag == null) {
            tagView.tag = ENABLED
            tvOther.typeface =
                ResourcesCompat.getFont(context, R.font.inter_bold)
            tvOther.background =
                ContextCompat.getDrawable(context, R.drawable.other_button_enabled)
            tvOther.setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.white
                )
            )
            resetNone()
            if(otherSingleSelect == true)
                resetOtherChip()
        } else {
            tagView.tag = null
            tvOther.typeface =
                ResourcesCompat.getFont(context, R.font.inter_regular)
            tvOther.background =
                ContextCompat.getDrawable(context, R.drawable.other_view_background)
            tvOther.setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.primary_medium_blue
                )
            )
        }
        if (otherCallBack != null)
            otherCallBack.invoke(Other, tagView.tag != null)
        else
            callBack?.invoke(chipData.second ,chipGroup.checkedChipIds.isEmpty(), tagView.tag != null)
    }

    private fun getColorStateList(
        selectedColor: Int,
        unSelectedColor: Int
    ): ColorStateList {
        val states = arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf(-android.R.attr.state_checked),
            intArrayOf(android.R.attr.state_selected),
            intArrayOf(-android.R.attr.state_selected),
        )
        val colors = intArrayOf(
            selectedColor,
            unSelectedColor,
            selectedColor,
            unSelectedColor
        )
        return ColorStateList(states, colors)
    }

    fun getSelectedTags(): List<ChipViewItemModel> {
        val tags = arrayListOf<ChipViewItemModel>()
        for (chipId in 0..chipGroup.childCount) {
            chipGroup.getChildAt(chipId)?.let {
                if (it is Chip && it.isChecked) {
                    val tagModel = it.tag as? ChipViewItemModel
                    tagModel?.let {
                        tags.add(tagModel)
                    }
                }
            }
        }
        chipGroup.findViewWithTag<LinearLayout>(Other)?.let { layout ->
            layout.getChildAt(1)?.tag?.let {
                layout.getChildAt(0)?.tag?.let { tag ->
                    val tagModel = tag as? ChipViewItemModel
                    tagModel?.let {
                        tags.add(tagModel)
                    }
                }
            }
        }
        return tags
    }

    fun clearSelection() {
        chipGroup.clearCheck()
    }

    private fun getChipText(data: Any): Pair<String?, String>? {
        return when (data) {
            is ChipViewItemModel -> {
                Pair(data.cultureValue ?: data.name, data.name)
            }
            else -> null
        }
    }

    private fun getActualNameOfChip(data: Any): String? {
        return when (data) {
            is ChipViewItemModel -> {
                return data.name
            }
            else -> null
        }
    }

    private fun resetOtherChip() {
        chipGroup.clearCheck()
    }

    fun clearOtherChip() {
        onNoneSelected()
    }

}