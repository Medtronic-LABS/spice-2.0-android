package com.medtroniclabs.opensource.formgeneration

import android.content.Context
import android.content.ContextWrapper
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.widget.NestedScrollView
import androidx.core.widget.addTextChangedListener
import com.google.android.flexbox.FlexboxLayout
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.data.ExaminationModel
import com.medtroniclabs.opensource.databinding.AccordionLayoutExaminationBinding
import com.medtroniclabs.opensource.databinding.CheckboxDialogSpinnerLayoutBinding
import com.medtroniclabs.opensource.databinding.EdittextLayoutBinding
import com.medtroniclabs.opensource.databinding.LayoutSingleSelectionBinding
import com.medtroniclabs.opensource.formgeneration.config.DefinedParams
import com.medtroniclabs.opensource.formgeneration.config.ViewType
import com.medtroniclabs.opensource.formgeneration.extension.markMandatory
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.formgeneration.model.FormLayout
import com.medtroniclabs.opensource.formgeneration.ui.SingleSelectionCustomView
import com.medtroniclabs.opensource.formgeneration.utility.DigitsInputFilter
import com.medtroniclabs.opensource.mappingkey.HouseHoldRegistration
import com.medtroniclabs.opensource.mappingkey.MemberRegistration

class ExaminationGenerator(
    val context: Context,
    private val parentLayout: LinearLayout,
    val listener: ExaminationListener,
    var scrollView: NestedScrollView? = null,
    val translate: Boolean = false
) : ContextWrapper(context) {

    private val rootSuffix = "rootView"
    private val titleSuffix = "titleTextView"
    private val errorSuffix = "errorMessageView"
    private val tvKey = "summaryKey"
    private val tvValue = "summaryValue"
    private val rootSummary = "summaryRoot"
    private val resultHashMap = HashMap<String, Any>()

    fun populateExaminationView(examinations: ArrayList<ExaminationModel>) {
        parentLayout.removeAllViews()
        examinations.forEach {
            val binding = AccordionLayoutExaminationBinding.inflate(LayoutInflater.from(context))
            binding.tvDiseaseName.text = it.diseaseName
            binding.tvDiseaseNameHolder.setOnClickListener {
                if (binding.llFamilyRoot.visibility != View.VISIBLE) {
                    binding.llFamilyRoot.visibility = View.VISIBLE
                    binding.ivDropDown.setImageDrawable(getDrawable(R.drawable.ic_arrow_up))
                } else {
                    binding.llFamilyRoot.visibility = View.GONE
                    binding.ivDropDown.setImageDrawable(getDrawable(R.drawable.ic_arrow_down))
                }
            }
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams.setMargins(10, 10, 10, 10)
            binding.root.layoutParams = layoutParams
            renderAccordionView(it.questionnaires, binding.questionnairesHolder, it.diseaseName)
            parentLayout.addView(binding.root)
        }
    }

    private fun renderAccordionView(
        questionnaires: ArrayList<FormLayout>,
        questionnairesHolderLayout: FlexboxLayout,
        diseaseName: String
    ) {
        questionnaires.forEach { formLayout ->
            when (formLayout.viewType) {
                ViewType.VIEW_TYPE_SINGLE_SELECTION -> createSingleSelectionView(
                    formLayout,
                    questionnairesHolderLayout,
                    diseaseName
                )

                ViewType.VIEW_TYPE_FORM_EDITTEXT -> createEditText(
                    formLayout,
                    questionnairesHolderLayout,
                    diseaseName
                )

                ViewType.VIEW_TYPE_DIALOG_CHECKBOX -> createCheckboxDialogView(
                    formLayout,
                    questionnairesHolderLayout,
                    diseaseName
                )
            }
        }
    }

    private fun createCheckboxDialogView(
        formLayout: FormLayout,
        questionnairesHolderLayout: FlexboxLayout,
        diseaseName: String
    ) {
        val binding = CheckboxDialogSpinnerLayoutBinding.inflate(LayoutInflater.from(context))
        formLayout.apply {
            binding.root.tag = id + rootSuffix
            binding.etUserInput.tag = id
            binding.tvTitle.tag = id + titleSuffix
            binding.tvErrorMessage.tag = id + errorSuffix
            binding.tvTitle.text = FormSupport.translateTitle(titleCulture, title, false)
            binding.parentLayout.minimumWidth =  dpToPx(520)
            hint?.let {
                binding.etUserInput.hint = it
            }

            if (isMandatory) {
                binding.tvTitle.markMandatory()
            }

            binding.etUserInput.safeClickListener {
                listener.onDialogueCheckboxListener(
                    id,
                    formLayout,
                    getCategorizedMap(resultHashMap, diseaseName)[id],
                    diseaseName
                )
            }
            questionnairesHolderLayout.addView(binding.root)
        }

    }

    private fun createEditText(
        serverViewModel: FormLayout,
        questionnairesHolderLayout: FlexboxLayout,
        diseaseName: String
    ) {
        val binding = EdittextLayoutBinding.inflate(LayoutInflater.from(context))
        serverViewModel.apply {
            binding.root.tag = id + rootSuffix
            binding.tvTitle.tag = id + titleSuffix
            binding.etUserInput.tag = id
            binding.tvErrorMessage.tag = id + errorSuffix
            binding.tvNationalIdAction.visibility = View.GONE
            binding.tvKey.tag = id + tvKey
            binding.tvValue.tag = id + tvValue
            val leftRightPadding = dpToPx(16)
            val topBottomPadding = dpToPx(0)
            binding.root.setPadding(
                leftRightPadding,
                topBottomPadding,
                leftRightPadding,
                topBottomPadding
            )
            binding.bgLastMeal.tag = id + rootSummary
            binding.tvTitle.text =
                FormSupport.updateTitle(title, translate, titleCulture, unitMeasurement)

            if (serverViewModel.id.contains(MemberRegistration.phoneNumber) || serverViewModel.id.contains(
                    HouseHoldRegistration.headPhoneNumber
                )
            ) {
                SecuredPreference.getPhoneNumberCode()?.let { phoneNumberCode ->
                    binding.llCountryCode.visibility = View.VISIBLE
                    binding.tvCountryCode.text = phoneNumberCode
                }
            }

            maxLines?.let { binding.etUserInput.setLines(it) }
                ?: binding.etUserInput.setSingleLine()

            if (isMandatory) {
                binding.tvTitle.markMandatory()
            }

            defaultValue?.let {
                binding.etUserInput.setText(it)
                getCategorizedMap(resultHashMap, diseaseName)[id] = it
            }

            hint?.let {
                if (translate) {
                    binding.etUserInput.hint = hintCulture ?: it
                } else {
                    binding.etUserInput.hint = it
                }
            }


            isEnabled?.let {
                binding.etUserInput.isEnabled = it
            }

            val inputFilter = arrayListOf<InputFilter>()
            maxLength?.let {
                inputFilter.add(InputFilter.LengthFilter(it))
            }

            contentLength?.let {
                inputFilter.add(InputFilter.LengthFilter(it))
            }

            if (applyDecimalFilter == true)
                inputFilter.add(DigitsInputFilter())

            if (id == DefinedParams.NationalId) {
                inputFilter.add(InputFilter.AllCaps())
            }

            if (inputFilter.isNotEmpty()) {
                try {
                    binding.etUserInput.filters = inputFilter.toTypedArray()
                } catch (_: Exception) {
                    //Exception - Catch block
                }
            }

            inputType?.let {
                when (it) {
                    InputType.TYPE_CLASS_PHONE, InputType.TYPE_CLASS_NUMBER -> binding.etUserInput.inputType =
                        InputType.TYPE_CLASS_NUMBER

                    InputType.TYPE_NUMBER_FLAG_DECIMAL -> binding.etUserInput.inputType =
                        InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL

                    else -> {
                        binding.etUserInput.inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                    }
                }
            }

            questionnairesHolderLayout.addView(binding.root)

            binding.etUserInput.addTextChangedListener { editable: Editable? ->
                when {
                    editable.isNullOrBlank() -> {
                        getCategorizedMap(resultHashMap, diseaseName).remove(id)
                    }

                    else -> {
                        if ((inputType != null && (inputType == InputType.TYPE_CLASS_NUMBER ||
                                    inputType == InputType.TYPE_NUMBER_FLAG_DECIMAL))
                        ) {
                            val resultValue = editable.trim().toString().toDoubleOrNull()
                            resultValue?.let {
                                getCategorizedMap(resultHashMap, diseaseName)[id] = resultValue
                            }
                        } else
                            getCategorizedMap(resultHashMap, diseaseName)[id] =
                                editable.trim().toString()
                    }
                }

                listener.setResultHashMap(resultHashMap)
            }
        }
    }


    private fun createSingleSelectionView(
        serverViewModel: FormLayout,
        questionnairesHolderLayout: FlexboxLayout,
        diseaseName: String
    ) {
        serverViewModel.apply {
            val binding = LayoutSingleSelectionBinding.inflate(LayoutInflater.from(context))
            binding.root.tag = id + rootSuffix
            binding.tvTitle.tag = id + titleSuffix
            binding.tvErrorMessage.tag = id + errorSuffix
            binding.tvTitle.text = FormSupport.translateTitle(titleCulture, title, translate)
            optionsList?.let {
                val view = SingleSelectionCustomView(context)
                view.tag = id
                view.addViewElements(
                    it,
                    translate,
                    getCategorizedMap(resultHashMap, diseaseName),
                    Pair(id, diseaseName),
                    serverViewModel,
                    singleSelectionCallback
                )
                binding.selectionGroup.addView(view)
            }
            if (isMandatory)
                binding.tvTitle.markMandatory()

            questionnairesHolderLayout.addView(binding.root)
        }
    }

    private fun getCategorizedMap(
        resultHashMap: HashMap<String, Any>,
        diseaseName: String
    ): HashMap<String, Any> {
        if (resultHashMap.containsKey(diseaseName) && resultHashMap[diseaseName] is Map<*, *>) {
            return resultHashMap[diseaseName] as HashMap<String, Any>
        } else {
            val map = HashMap<String, Any>()
            resultHashMap[diseaseName] = map
            return resultHashMap[diseaseName] as HashMap<String, Any>
        }
    }

    private var singleSelectionCallback: ((selectedID: Any?, elementId: Pair<String, String?>, serverViewModel: FormLayout, name: String?) -> Unit)? =
        { selectedId, elementID, _, _ ->
            saveSelectedOptionValue(elementID, selectedId)
        }

    private fun saveSelectedOptionValue(
        elementId: Pair<String, String?>,
        idValue: Any?
    ) {
        idValue?.let {
            if (resultHashMap.containsKey(elementId.second) && resultHashMap[elementId.second] is Map<*, *>) {
                val map = resultHashMap[elementId.second] as HashMap<String, Any>
                map[elementId.first] = it
            } else {
                val map = HashMap<String, Any>()
                map[elementId.first] = it
                elementId.second?.let { second ->
                    resultHashMap[second] = map
                }
            }

            listener.setResultHashMap(resultHashMap)
        }
    }


    fun validateCheckboxDialogue(
        id: String,
        resultMap: ArrayList<HashMap<String, Any>>,
        diseaseName: String
    ) {
        if (resultMap.isEmpty()) {
            if (getCategorizedMap(resultHashMap, diseaseName).containsKey(id)) {
                getCategorizedMap(resultHashMap, diseaseName).remove(id)
            }
        } else {
            getCategorizedMap(resultHashMap, diseaseName)[id] = resultMap
        }
        getViewByTag(id)?.let { view ->
            if (view is AppCompatTextView) {
                view.text = setCheckBoxDialogText(getCategorizedMap(resultHashMap,diseaseName), id)
            }
        }
        listener.setResultHashMap(resultHashMap)
    }

    private fun setCheckBoxDialogText(
        resultHashMap: HashMap<String, Any>,
        id: String
    ): String {
        var text = ""
        if (resultHashMap.containsKey(id)) {
            val mapList = resultHashMap[id]
            if (mapList is java.util.ArrayList<*>) {
                if (mapList.size == 1) {
                    text = setDialogText(mapList)
                } else if (mapList.size > 1) {
                    text = if (isContainsOther(mapList)) {
                        "${mapList.size - 1} and ${DefinedParams.Other} ${
                            getString(R.string.symptoms_selected)
                        }"
                    } else {
                        "${mapList.size} ${getString(R.string.symptoms_selected)}"
                    }
                } else {
                    text = ""
                }
            }
        }
        return text
    }

    private fun setDialogText(mapList: java.util.ArrayList<*>): String {
        return if (isContainsOther(mapList)) {
            "${DefinedParams.Other} ${
                getString(R.string.symptoms_selected)
            }"
        } else if (isNoSymptomContain(mapList)) {
            getString(R.string.no_symptom_selected)
        } else {
            "${mapList.size} ${getString(R.string.symptoms_selected)}"
        }
    }

    private fun isContainsOther(mapList: ArrayList<*>): Boolean {
        var status = false
        mapList.forEach { map ->
            if (map is HashMap<*, *>) {
                val name = map[DefinedParams.NAME]
                if (name is String && name.equals(DefinedParams.Other, true)) {
                    status = true
                    return@forEach
                }
            }
        }
        return status
    }

    private fun isNoSymptomContain(mapList: java.util.ArrayList<*>): Boolean {
        var status = false
        mapList.forEach { map ->
            if (map is HashMap<*, *>) {
                val name = map[DefinedParams.NAME]
                if (name is String && name.startsWith(DefinedParams.NoSymptoms, true)) {
                    status = true
                    return@forEach
                }
            }
        }
        return status
    }

    private fun getViewByTag(tag: Any): View? {
        return parentLayout.findViewWithTag(tag)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}