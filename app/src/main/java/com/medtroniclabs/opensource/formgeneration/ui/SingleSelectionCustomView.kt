package com.medtroniclabs.opensource.formgeneration.ui

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
import com.medtroniclabs.opensource.formgeneration.model.FormLayout

class SingleSelectionCustomView : LinearLayout {

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
        optionList: ArrayList<Map<String, Any>>,
        translate: Boolean,
        resultMap: HashMap<String, Any>?,
        elementID: Pair<String, String?>,
        serverViewModel: FormLayout,
        callback: ((SelectedID: Any?, elementID: Pair<String,String?>, serverViewModel: FormLayout, name:String?) -> Unit?)?
    ) {
        removeAllViews()
        this.optionList = optionList
        this.optionList?.forEachIndexed { index, optionValue ->
            val name = optionValue[DefinedParams.NAME]
            val idValue = optionValue[DefinedParams.ID]
            val translatedName = optionValue[DefinedParams.cultureValue]
            val textView = TextView(viewContext, null, 0, R.style.Form_MH_Style_with_padding)
            val param = LayoutParams(
                0,
                LayoutParams.MATCH_PARENT,
                1.0f
            )
            val selectedValue = if (resultMap!= null)  resultMap[elementID.first] else null
            textView.tag = "${idValue}_${elementID.first}"
            textView.isSelected = selectedValue != null && selectedValue == idValue
            textView.layoutParams = param
            if (translate && translatedName != null && translatedName is String) {
                textView.text = translatedName
            } else if (name != null && name is String) {
                textView.text = name
            } else {
                textView.text = ""
            }
            getBackgroundDrawable(index, optionList)?.let {
                textView.background = it
            }
            serverViewModel.enableSingleSelection?.let { enableStatus ->
                textView.isEnabled = enableStatus
            }

            textView.safeClickListener {
                callback?.invoke(optionValue[DefinedParams.ID], elementID,serverViewModel,(name as String?))
                addViewElements(
                    optionList,
                    translate,
                    resultMap,
                    elementID,
                    serverViewModel,
                    callback
                )
            }
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

    fun singleSelectionChildViewsOption(string: String) {
        forEach {
            if (it is TextView && it.text.toString().equals(string, ignoreCase = true)) {
                it.performClick()
                it.isEnabled = false
                return@forEach
            }
        }
    }

    fun singleSelectionAutofill(id: String) {
        forEach {
            if (it is TextView && it.tag.toString().equals(id, true)) {
                it.performClick()
                it.isEnabled = false
                return@forEach
            }
        }
    }
}