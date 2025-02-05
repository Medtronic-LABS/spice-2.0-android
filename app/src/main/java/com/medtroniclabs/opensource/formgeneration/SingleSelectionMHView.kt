package com.medtroniclabs.opensource.formgeneration

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.formgeneration.config.DefinedParams
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.mappingkey.Screening
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams

class SingleSelectionMHView : LinearLayout {
    private lateinit var viewContext: Context
    private var optionList: ArrayList<Map<String, Any>>? = null
    constructor(context: Context) : super(context) {
        init(context)
    }
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context)
    }
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context)
    }
    private fun init(context: Context) {
        viewContext = context
        orientation = HORIZONTAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }
    fun addViewElements(
        //Pair(questionId, translate)
        questionIdTranslatePair: Pair<Long, Boolean>,
        optionList: ArrayList<Map<String, Any>>,
        //Pair(selectedId, mapType)
        selectedIdMapTypePair: Pair<Long?, String?>,
        editList: ArrayList<Map<String, Any>>? = null,
        resultMap: HashMap<String, Any>,
        //Triple(isViewOnly, isUnselect, question)
        isViewOnlySelectionPair: Triple<Boolean, Boolean, String?>,
        callback: ((questionId: Long, answerId: Long, score: Double, answerName: String, isClicked: Boolean) -> Unit?)? = null
    ) {
        removeAllViews()
        val editMap = ArrayList<Long>()
        getEditList(
            isViewOnlySelectionPair.first,
            getResultMapType(selectedIdMapTypePair.second),
            resultMap,
            editList,
            isViewOnlySelectionPair.third
        )?.forEach { item ->
            if (item[Screening.Answer_Id] is Long) {
                (item[Screening.Answer_Id] as? Long)?.let {
                    editMap.add(it)
                }
            } else {
                (item[Screening.Answer_Id] as? Double)?.toLong()?.let {
                    editMap.add(it)
                }
            }
        } ?: selectedIdMapTypePair.first?.let { editMap.add(it) }
        this.optionList = optionList
        this.optionList?.forEachIndexed { index, optionValue ->
            val name = (optionValue[Screening.Questions] as? String) ?: ""
            val idValue = (optionValue[DefinedParams.ID] as? Double)?.toLong() ?: -1L
            val score = (optionValue[Screening.Value] as? Double) ?: 0.0
            val translatedName = optionValue[DefinedParams.cultureValue] ?: optionValue[DefinedParams.displayValue]
            val textView = TextView(viewContext, null, 0, R.style.Form_MH_Style_with_padding)
            val param = LayoutParams(
                0,
                LayoutParams.MATCH_PARENT,
                1.0f
            )
            textView.layoutParams = param
            if (questionIdTranslatePair.second && translatedName != null && translatedName is String) {
                textView.text = translatedName
            } else {
                textView.text = name
            }
            getBackgroundDrawable(index, optionList)?.let {
                textView.background = it
            }
            if (optionValue.containsKey(Screening.Value)) {
                (optionValue[Screening.Value] as? Double)?.toInt()?.let {
                    textView.tag = it
                }
            }
            if (!isViewOnlySelectionPair.first) {
                textView.safeClickListener {
                    callback?.invoke(questionIdTranslatePair.first, idValue, score, name, true)
                    addViewElements(
                        Pair(questionIdTranslatePair.first, questionIdTranslatePair.second),
                        optionList,
                        Pair(selectedIdMapTypePair.first, selectedIdMapTypePair.second),
                        null,
                        resultMap,
                        Triple(
                            false,
                            isViewOnlySelectionPair.second,
                            isViewOnlySelectionPair.third
                        ),
                        callback
                    )
                }
            }
            if(editMap.contains(idValue))
            {
                textView.isSelected = true
                callback?.invoke(questionIdTranslatePair.first, idValue, score, name, false)
            }
            else
                textView.isSelected = false
            addView(textView)
        }
    }
    private fun getBackgroundDrawable(
        index: Int,
        list: ArrayList<Map<String, Any>>
    ): Drawable? {
        when (index) {
            0 -> return ContextCompat.getDrawable(viewContext, R.drawable.left_mh_view_selector)
            list.size - 1 -> return ContextCompat.getDrawable(
                viewContext,
                R.drawable.right_mh_view_selector
            )
        }
        return null
    }
    fun resetSingleSelectionChildViews() {
        forEach {
            it.isSelected = false
        }
    }
    private fun getEditList(
        viewOnly: Boolean,
        resultType: String?,
        resultMap: HashMap<String, Any>,
        editList: ArrayList<Map<String, Any>>?,
        question: String?
    ): ArrayList<Map<String, Any>>? {
        return if (!viewOnly && resultMap.containsKey(resultType) && editList==null){
            val itemsList:ArrayList<Map<String, Any>> = ArrayList()
            val resultMapItems = resultMap[resultType] as? HashMap<String,Any>
            resultMapItems?.let {
                if (resultMapItems.containsKey(question)){
                    val questionItems = resultMapItems[question] as? HashMap<String,Any>
                    questionItems?.let {listItems ->
                        for ((key, value) in listItems) {
                            if (key== Screening.Answer_Id){
                                val newMap = HashMap<String, Any>()
                                newMap[key] = value
                                itemsList.add(newMap)
                            }
                        }
                    }
                }
            }
            return itemsList
        }
        else editList
    }
    fun getResultMapType(type: String?): String? {
        return when(type){
            Screening.PHQ4 -> Screening.MentalHealthDetails
            AssessmentDefinedParams.GAD7 -> AssessmentDefinedParams.GAD7_Mental_Health
            AssessmentDefinedParams.PHQ9 -> AssessmentDefinedParams.PHQ9_Mental_Health
            else -> null
        }
    }

    fun prefillSingleSelection(string: String) {
        forEach {
            if (it is TextView && it.text.toString().equals(string, ignoreCase = true)) {
                it.performClick()
                return@forEach
            }
        }
    }
}