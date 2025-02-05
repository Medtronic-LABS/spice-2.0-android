package com.medtroniclabs.opensource.formgeneration

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.widget.NestedScrollView
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.invisible
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.data.DiseaseCategoryItems
import com.medtroniclabs.opensource.data.DiseaseConditionItems
import com.medtroniclabs.opensource.data.model.ChipViewItemModel
import com.medtroniclabs.opensource.databinding.DiagnosisAccordionLayoutBinding
import com.medtroniclabs.opensource.formgeneration.config.DefinedParams.AccordionGroup
import com.medtroniclabs.opensource.ui.TagListCustomView

class DiagnosisGenerator(
    val context: Context,
    private val parentLayout: LinearLayout,
    val listener: DiagnosisListener,
    var scrollView: NestedScrollView? = null,
    val translate: Boolean = false
) {
    private val countSuffix = "countSuffix"
    private val tagViews = mutableListOf<TagListCustomView>()
    private val selectedTagsMap = hashMapOf<String, List<ChipViewItemModel>>()
    private var diagnosisCallback: DiagnosisListener? = null

    fun populateDiagnosisView(
        diagnosis: List<DiseaseCategoryItems>,
        selectedItemList: List<ChipViewItemModel>?
    ) {
        diagnosis.forEach {
            val binding = DiagnosisAccordionLayoutBinding.inflate(LayoutInflater.from(context))
            binding.tvDiagnosisName.text = it.name
            binding.llFamilyRoot.tag = it.name
            binding.accordionGroup.tag = it.name + AccordionGroup
            binding.tvCount.tag = it.name + countSuffix
            binding.tvDiagnosisNameHolder.setOnClickListener {
                if (binding.llFamilyRoot.visibility != View.VISIBLE) {
                    binding.llFamilyRoot.visibility = View.VISIBLE
                    binding.ivDropDown.setImageDrawable(
                        getDrawable(
                            context,
                            R.drawable.ic_arrow_purple
                        )
                    )
                } else {
                    binding.llFamilyRoot.visibility = View.GONE
                    binding.ivDropDown.setImageDrawable(
                        getDrawable(
                            context,
                            R.drawable.ic_arrow_forward
                        )
                    )
                }
            }
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            //layoutParams.setMargins(10, 10, 10, 10)
            binding.root.layoutParams = layoutParams
            val tagView =
                TagListCustomView(
                    context, binding.diagnosisHolder
                ) { _, _, _ ->
                    //updateSelectedCount()
                    diagnosisCallback?.onDiagnosisSelection(isAccordionNotEmpty())
                }
            renderDiagnosisAccordionView(
                tagView,
                it.name,
                it.diseaseCondition,
                selectedItemList,
                binding.tvCount
            )
            tagViews.add(tagView)
            parentLayout.addView(binding.root)
        }
    }

    private fun updateSelectedCount() {
        val selectedAccordionCountMap = HashMap<String, Int>()
        tagViews.forEach { tagView ->
            val accordionView = tagView.chipGroup.parent as? View
            val accordionName = accordionView?.tag as? String
            accordionName?.let {
                val selectedChipCount = tagView.getSelectedTags().size
                selectedAccordionCountMap[it] = selectedChipCount
                getViewByTag(accordionName + countSuffix)?.let { view ->
                    if (view is TextView) {
                        if (selectedChipCount > 0) {
                            view.visible()
                            view.text = selectedChipCount.toString()
                        } else {
                            view.invisible()
                        }
                    }
                }
            }
        }
    }

    fun getViewByTag(tag: Any): View? {
        return parentLayout.findViewWithTag(tag)
    }

    private fun renderDiagnosisAccordionView(
        tagView: TagListCustomView,
        name: String,
        diseaseCondition: ArrayList<DiseaseConditionItems>,
        selectedItemList: List<ChipViewItemModel>?,
        tvCount: AppCompatTextView
    ) {
        diseaseCondition.let {
            val chipItemList = ArrayList<ChipViewItemModel>()
            diseaseCondition.forEach {
                chipItemList.add(
                    ChipViewItemModel(
                        id = it.id,
                        name = it.name,
                        value = it.value
                    )
                )
            }
            tagView.addChipItemList(
                chipItemList,
                selectedItemList
            )
        }
        val count = tagView.getSelectedTags().size
        tvCount.text = count.toString()
    }

    fun isAccordionNotEmpty(): Boolean {
        return getSelectedTagsForAccordions().values.any { it.isNotEmpty() }
    }

    fun isEmptyAccordion(): Boolean {
        return getSelectedTagsForAccordions().values.all { it.isEmpty() }
    }

    fun getSelectedTagsForAccordions(): HashMap<String, List<ChipViewItemModel>> {
        selectedTagsMap.clear()
        tagViews.forEach { tagView ->
            val accordionView = tagView.chipGroup.parent as? View
            val accordionName = accordionView?.tag as? String
            accordionName?.let {
                selectedTagsMap[accordionName] = tagView.getSelectedTags()
            }
        }
        return selectedTagsMap
    }

    fun modifyMap(name: String) {
        selectedTagsMap.remove(name)
    }

    fun removeViewByTag(name: String) {
        val iterator = tagViews.iterator()
        while (iterator.hasNext()) {
            val tagView = iterator.next()
            val accordionChipGroupView = tagView.chipGroup.parent as? View
            val accordionName = accordionChipGroupView?.tag as? String
            if (accordionName == name) {
                iterator.remove()
                val parent = accordionChipGroupView.parent as? ViewGroup
                parent?.removeView(accordionChipGroupView)
                removeAccordionView(name)
                break
            }
        }
        modifyMap(name)
    }

    private fun removeAccordionView(name: String) {
        getViewByTag(name + AccordionGroup)?.let { view ->
            val parent = view.parent as? ViewGroup
            parent?.removeView(view)
        }
    }

    fun setDiagnosisCallback(callback: DiagnosisListener) {
        diagnosisCallback = callback
    }
}