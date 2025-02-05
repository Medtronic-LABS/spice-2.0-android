package com.medtroniclabs.opensource.formgeneration

import android.app.DatePickerDialog
import android.content.Context
import android.content.ContextWrapper
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Point
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextPaint
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.widget.AdapterView
import android.widget.DatePicker
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatSpinner
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.children
import androidx.core.view.forEach
import androidx.core.widget.NestedScrollView
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.isGone
import com.medtroniclabs.opensource.appextensions.isVisible
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.CommonUtils.displayAge
import com.medtroniclabs.opensource.common.CommonUtils.getMaxDateLimit
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ
import com.medtroniclabs.opensource.common.DateUtils.DATE_ddMMyyyy
import com.medtroniclabs.opensource.common.DateUtils.convertDateFormat
import com.medtroniclabs.opensource.common.DateUtils.convertDateToStringWithUTC
import com.medtroniclabs.opensource.common.DefinedParams.female
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.common.StringConverter
import com.medtroniclabs.opensource.common.Validator
import com.medtroniclabs.opensource.data.LocalSpinnerResponse
import com.medtroniclabs.opensource.databinding.AgeDobLayoutBinding
import com.medtroniclabs.opensource.databinding.BpReadingLayoutBinding
import com.medtroniclabs.opensource.databinding.CardLayoutBinding
import com.medtroniclabs.opensource.databinding.CheckboxDialogSpinnerLayoutBinding
import com.medtroniclabs.opensource.databinding.CustomSpinnerBinding
import com.medtroniclabs.opensource.databinding.DatepickerLayoutBinding
import com.medtroniclabs.opensource.databinding.EdittextAreaLayoutBinding
import com.medtroniclabs.opensource.databinding.EdittextLayoutBinding
import com.medtroniclabs.opensource.databinding.InstructionLayoutBinding
import com.medtroniclabs.opensource.databinding.LayoutInformationLabelBinding
import com.medtroniclabs.opensource.databinding.LayoutSingleSelectionBinding
import com.medtroniclabs.opensource.databinding.MentalHealthLayoutBinding
import com.medtroniclabs.opensource.databinding.NoOfDaysLayoutBinding
import com.medtroniclabs.opensource.databinding.RadioGroupLayoutBinding
import com.medtroniclabs.opensource.databinding.TextLabelLayoutBinding
import com.medtroniclabs.opensource.databinding.TimeViewLayoutBinding
import com.medtroniclabs.opensource.db.entity.MentalHealthEntity
import com.medtroniclabs.opensource.formgeneration.FormSupport.getSpannableString
import com.medtroniclabs.opensource.formgeneration.FormSupport.isTranslatedOrNot
import com.medtroniclabs.opensource.formgeneration.FormSupport.translateTitle
import com.medtroniclabs.opensource.formgeneration.FormSupport.updateTitle
import com.medtroniclabs.opensource.formgeneration.config.DefinedParams
import com.medtroniclabs.opensource.formgeneration.config.DefinedParams.GONE
import com.medtroniclabs.opensource.formgeneration.config.DefinedParams.INVISIBLE
import com.medtroniclabs.opensource.formgeneration.config.DefinedParams.Month
import com.medtroniclabs.opensource.formgeneration.config.DefinedParams.SSP16
import com.medtroniclabs.opensource.formgeneration.config.DefinedParams.VISIBLE
import com.medtroniclabs.opensource.formgeneration.config.DefinedParams.Week
import com.medtroniclabs.opensource.formgeneration.config.DefinedParams.Year
import com.medtroniclabs.opensource.formgeneration.config.DefinedParams.value
import com.medtroniclabs.opensource.common.DefinedParams.BOLD
import com.medtroniclabs.opensource.common.DefinedParams.BOLD_ITALIC
import com.medtroniclabs.opensource.common.DefinedParams.DefaultID
import com.medtroniclabs.opensource.common.DefinedParams.ITALIC
import com.medtroniclabs.opensource.formgeneration.config.ViewType.VIEW_INFORMATION_LABEL
import com.medtroniclabs.opensource.formgeneration.config.ViewType.VIEW_TYPE_DIALOG_CHECKBOX
import com.medtroniclabs.opensource.formgeneration.config.ViewType.VIEW_TYPE_FORM_AGE
import com.medtroniclabs.opensource.formgeneration.config.ViewType.VIEW_TYPE_FORM_BP
import com.medtroniclabs.opensource.formgeneration.config.ViewType.VIEW_TYPE_FORM_CARD_FAMILY
import com.medtroniclabs.opensource.formgeneration.config.ViewType.VIEW_TYPE_FORM_DATEPICKER
import com.medtroniclabs.opensource.formgeneration.config.ViewType.VIEW_TYPE_FORM_EDITTEXT
import com.medtroniclabs.opensource.formgeneration.config.ViewType.VIEW_TYPE_FORM_EDITTEXT_AREA
import com.medtroniclabs.opensource.formgeneration.config.ViewType.VIEW_TYPE_FORM_RADIOGROUP
import com.medtroniclabs.opensource.formgeneration.config.ViewType.VIEW_TYPE_FORM_SPINNER
import com.medtroniclabs.opensource.formgeneration.config.ViewType.VIEW_TYPE_FORM_TEXTLABEL
import com.medtroniclabs.opensource.formgeneration.config.ViewType.VIEW_TYPE_INSTRUCTION
import com.medtroniclabs.opensource.formgeneration.config.ViewType.VIEW_TYPE_METAL_HEALTH
import com.medtroniclabs.opensource.formgeneration.config.ViewType.VIEW_TYPE_NO_OF_DAYS
import com.medtroniclabs.opensource.formgeneration.config.ViewType.VIEW_TYPE_SINGLE_SELECTION
import com.medtroniclabs.opensource.formgeneration.config.ViewType.VIEW_TYPE_TIME
import com.medtroniclabs.opensource.formgeneration.extension.DecimalDigitsInputFilter
import com.medtroniclabs.opensource.formgeneration.extension.dp
import com.medtroniclabs.opensource.formgeneration.extension.hideKeyboard
import com.medtroniclabs.opensource.formgeneration.extension.markMandatory
import com.medtroniclabs.opensource.formgeneration.extension.markNonMandatory
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.formgeneration.extension.textSizeSsp
import com.medtroniclabs.opensource.formgeneration.listener.FormEventListener
import com.medtroniclabs.opensource.formgeneration.model.BPModel
import com.medtroniclabs.opensource.formgeneration.model.ConditionModelConfig
import com.medtroniclabs.opensource.formgeneration.model.ConditionalModel
import com.medtroniclabs.opensource.formgeneration.model.FormLayout
import com.medtroniclabs.opensource.formgeneration.model.MentalHealthOption
import com.medtroniclabs.opensource.formgeneration.ui.SingleSelectionCustomView
import com.medtroniclabs.opensource.formgeneration.utility.CustomSpinnerAdapterCustomLayout
import com.medtroniclabs.opensource.formgeneration.utility.CustomSpinnerAdapter
import com.medtroniclabs.opensource.formgeneration.utility.DecimalInputFilter
import com.medtroniclabs.opensource.formgeneration.utility.DigitsInputFilter
import com.medtroniclabs.opensource.formgeneration.utility.FormFieldValidator
import com.medtroniclabs.opensource.mappingkey.HouseHoldRegistration.headPhoneNumber
import com.medtroniclabs.opensource.mappingkey.MemberRegistration
import com.medtroniclabs.opensource.mappingkey.MemberRegistration.dateOfBirth
import com.medtroniclabs.opensource.mappingkey.MemberRegistration.gender
import com.medtroniclabs.opensource.mappingkey.MemberRegistration.phoneNumber
import com.medtroniclabs.opensource.mappingkey.Screening
import com.medtroniclabs.opensource.mappingkey.Screening.DateOfBirth
import com.medtroniclabs.opensource.mappingkey.Screening.Hour
import com.medtroniclabs.opensource.mappingkey.Screening.Minute
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.muacCode
import com.medtroniclabs.opensource.ui.assessment.rmnch.RMNCH.PREGNANCY_MAX_AGE
import com.medtroniclabs.opensource.ui.assessment.rmnch.RMNCH.PREGNANCY_MIN_AGE
import java.util.Calendar
import java.util.Date

class FormGenerator(
    var context: Context,
    private val parentLayout: LinearLayout,
    private val resultLauncher: ActivityResultLauncher<Intent>? = null,
    private val listener: FormEventListener,
    var scrollView: NestedScrollView? = null,
    val translate: Boolean,
    private val callback: ((HashMap<String, Any>,String) -> Unit)? = null
) : ContextWrapper(context) {

    private var serverData: List<FormLayout>? = null
    val rootSuffix = "rootView"
    private val titleSuffix = "titleTextView"
    private val errorSuffix = "errorMessageView"
    private val resultHashMap = HashMap<String, Any>()
    private val tvKey = "summaryKey"
    private val tvValue = "summaryValue"
    private val rootSummary = "summaryRoot"
    private var editScreen: Boolean? = null
    private var focusNeeded: View? = null
    private val infoSuffix = "information"
    private val infoSuffixText = "informationSuffixText"
    private val generateNationalIdSuffix = "generateNationalId"
    private val diastolicSuffix = "DiastolicSuffix"
    private val systolicSuffix = "SystolicSuffix"
    private val pulseSuffix = "PulseSuffix"
    val lastMealTypeMeridiem  = "Meridiem"
    val lastMealTypeDateSuffix  = "Date"
    private var mentalHealthQuestions: HashMap<String, ArrayList<MentalHealthOption>>? = null
    private var mentalHealthEditList: ArrayList<Map<String, Any>>? = null
    private var EDITSCREEN: Boolean? = null

    fun populateViews(
        serverData: List<FormLayout>,
    ) {
        this.serverData = serverData
        parentLayout.removeAllViews()
        serverData.forEach { serverViewModel ->
            when (serverViewModel.viewType) {
                VIEW_TYPE_FORM_CARD_FAMILY -> createCardViewFamily(serverViewModel)
                VIEW_TYPE_FORM_EDITTEXT -> createEditText(serverViewModel)
                VIEW_TYPE_FORM_EDITTEXT_AREA -> createEditTextArea(serverViewModel)
                VIEW_TYPE_FORM_RADIOGROUP -> createRadioGroup(serverViewModel)
                VIEW_TYPE_SINGLE_SELECTION -> createSingleSelectionView(serverViewModel)
                VIEW_TYPE_FORM_SPINNER -> createCustomSpinner(serverViewModel)
                VIEW_TYPE_DIALOG_CHECKBOX -> createCheckboxDialogView(serverViewModel)
                VIEW_INFORMATION_LABEL -> createInformationLabel(serverViewModel)
                VIEW_TYPE_INSTRUCTION -> createInstructionView(serverViewModel)
                VIEW_TYPE_FORM_TEXTLABEL -> createTextLabel(serverViewModel)
                VIEW_TYPE_METAL_HEALTH -> createMentalHealthView(serverViewModel)
                VIEW_TYPE_FORM_AGE -> createAgeView(serverViewModel)
                VIEW_TYPE_NO_OF_DAYS -> createNoOfDaysView(serverViewModel)
                VIEW_TYPE_FORM_DATEPICKER -> createDatePicker(serverViewModel)
                VIEW_TYPE_FORM_BP -> createBPView(serverViewModel)
                VIEW_TYPE_TIME -> createTimeView(serverViewModel)
            }
        }
        listener.onRenderingComplete()
    }

    private fun createCardViewFamily(serverViewModel: FormLayout) {
        val binding = CardLayoutBinding.inflate(LayoutInflater.from(context))
        serverViewModel.apply {
            setViewVisibility(visibility, binding.root)
            binding.root.tag = id + rootSuffix
            binding.llFamilyRoot.tag = id
            if (translate) {
                binding.cardTitle.text = titleCulture ?: title
            } else {
                binding.cardTitle.text = title
            }
            if (parentLayout.findViewWithTag<LinearLayout>(id) == null) {
                parentLayout.addView(binding.root)
            }
        }
    }

    private fun createNoOfDaysView(serverViewModel: FormLayout) {
        val binding = NoOfDaysLayoutBinding.inflate(LayoutInflater.from(context))
        serverViewModel.apply {
            binding.root.tag = id + rootSuffix
            binding.tvTitle.tag = id + titleSuffix
            binding.etUserInput.tag = id
            binding.tvErrorMessage.tag = id + errorSuffix
            binding.tvTitle.text = title
            binding.tvInfo.text = information
            binding.tvInfo.tag = id + infoSuffix
            if (isMandatory) {
                binding.tvTitle.markMandatory()
            }
            isEnabled?.let {
                binding.etUserInput.isEnabled = it
            }
            isInfo?.let {
                binding.ivInfo.visibility = getVisibility(it)
            }

            binding.ivInfo.setOnClickListener {
                listener.onInstructionClicked(
                    id,
                    title,
                    dosageListModel = serverViewModel.dosageListItems
                )
            }

            informationVisibility?.let { value ->
                setViewVisibility(value, binding.tvInfo)
            }
            binding.etUserInput.inputType = InputType.TYPE_CLASS_NUMBER
            hint?.let {
                if (translate) {
                    binding.etUserInput.hint = hintCulture ?: it
                } else {
                    binding.etUserInput.hint = it
                }
            }
            val inputFilter = arrayListOf<InputFilter>()
            maxLength?.let {
                inputFilter.add(InputFilter.LengthFilter(it))
            }
            binding.etUserInput.filters = inputFilter.toTypedArray()
            binding.etUserInput.addTextChangedListener { input ->
                input?.let {
                    val enteredValue = input.trim().toString().toIntOrNull()
                    noOfDays?.let { days ->
                        listener.onInformationHandling(id, days, enteredValue, resultHashMap)
                    }
                    if (enteredValue != null) {
                        resultHashMap[id] = enteredValue
                    } else {
                        if (resultHashMap.containsKey(id))
                            resultHashMap.remove(id)
                    }
                }
            }
            (binding.etUserInput.background as? GradientDrawable)?.apply {
                setStroke(resources.getDimensionPixelSize(R.dimen._1sdp), context.getColor(R.color.edittext_stroke))
            }
            setViewVisibility(visibility, binding.root)
            setViewEnableDisable(isEnabled, binding.root)
            getFamilyView(family)?.addView(binding.root) ?: kotlin.run {
                parentLayout.addView(binding.root)
            }
        }
    }

    private fun getVisibility(visibility: String): Int {
        return when (visibility) {
            VISIBLE -> View.VISIBLE
            INVISIBLE -> View.INVISIBLE
            else -> View.GONE
        }
    }


    private fun createEditText(serverViewModel: FormLayout) {
        val binding = EdittextLayoutBinding.inflate(LayoutInflater.from(context))
        serverViewModel.apply {
            binding.root.tag = id + rootSuffix
            binding.tvTitle.tag = id + titleSuffix
            binding.etUserInput.tag = id
            binding.tvErrorMessage.tag = id + errorSuffix
            binding.tvNationalIdAction.visibility = View.GONE
            binding.tvKey.tag = id + tvKey
            binding.tvValue.tag = id + tvValue
            binding.tvNationalIdAction.tag = id + generateNationalIdSuffix
            binding.tvNationalIdAction.visibility = View.GONE
            binding.bgLastMeal.tag = id + rootSummary
            if (isNeedAction) {
                checkGenerateAction(this, binding)
            }
            binding.tvTitle.text = updateTitle(title, translate, titleCulture, unitMeasurement)

            if (serverViewModel.id.contains(Screening.phoneNumber) || serverViewModel.id.contains(phoneNumber) || serverViewModel.id.contains(
                    headPhoneNumber
                )
            ) {
                SecuredPreference.getPhoneNumberCode()?.let { phoneNumberCode ->
                    binding.llCountryCode.visibility = View.VISIBLE
                    binding.tvCountryCode.text = if (phoneNumberCode.startsWith("+")) {
                        phoneNumberCode
                    } else {
                        "+$phoneNumberCode"
                    }
                }
            }

            maxLines?.let { binding.etUserInput.setLines(it) }
                ?: binding.etUserInput.setSingleLine()

            if (isMandatory) {
                binding.tvTitle.markMandatory()
            }

            defaultValue?.let {
                binding.etUserInput.setText(it)
                resultHashMap[id] = it
            }
            maxDecimalPlaces?.let {
                binding.etUserInput.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter(it))
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

            if (id == Screening.identityValue) {
                inputFilter.add(InputFilter.AllCaps())
            }

            if (applyTwoDigitPrecision == true) {
                inputFilter.add(DecimalInputFilter())
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

            getFamilyView(family)?.addView(binding.root) ?: kotlin.run {
                parentLayout.addView(binding.root)
            }

            binding.etUserInput.addTextChangedListener { editable: Editable? ->
                when {
                    editable.isNullOrBlank() -> {
                        if (editScreen == true) {
                            if ((inputType != null && (inputType == InputType.TYPE_CLASS_NUMBER ||
                                        inputType == InputType.TYPE_NUMBER_FLAG_DECIMAL))
                            ) {
                                resultHashMap.remove(id)
                            } else {
                                resultHashMap[id] = ""
                            }
                        } else {
                            resultHashMap.remove(id)
                        }
                        setConditionalVisibility(serverViewModel, null)
                        resultHashMap.remove(id + Screening.unitMeasurement_KEY)
                    }

                    else -> {
                        if ((inputType != null && (inputType == InputType.TYPE_CLASS_NUMBER ||
                                    inputType == InputType.TYPE_NUMBER_FLAG_DECIMAL))
                        ) {
                            val resultValue = editable.trim().toString().toDoubleOrNull()
                            resultValue?.let {
                                resultHashMap[id] = resultValue
                            }
                        } else
                            resultHashMap[id] = editable.trim().toString()
                        setConditionalVisibility(serverViewModel, editable.trim().toString())
                        unitMeasurement?.let {
                            it.also { resultHashMap[id + Screening.unitMeasurement_KEY] = it }
                        }
                    }
                }
                callback?.invoke(resultHashMap,id)
            }
            (binding.etUserInput.background as? GradientDrawable)?.apply {
                setStroke(resources.getDimensionPixelSize(R.dimen._1sdp), context.getColor(R.color.edittext_stroke))
            }
            setViewVisibility(visibility, binding.root)
            setViewEnableDisable(isEnabled, binding.root)
        }
    }

    private fun generateNationalId() {
        var nationalId = ""
        var errorVisibility = View.GONE
        val firstName = firstName(Screening.firstName)
        firstName.first?.let {
            nationalId = it
        }
        focusNeeded = null
        firstNameError(nationalId, Screening.firstName, firstName.second)
        val lastName = lastName(nationalId)
        lastName.let {
            it.first?.let { nId ->
                nationalId = nId
            }
            it.second?.let { errVisibility ->
                errorVisibility = errVisibility
            }
        }
        lastNameError(errorVisibility, lastName.third)

        phoneNumber(nationalId).let {
            it.first?.let { nId ->
                nationalId = nId
            }
            it.second?.let { errVisibility ->
                errorVisibility = errVisibility
            }
        }
        phoneNumberError(errorVisibility)

        if (nationalId.isNotEmpty()) {
            getViewByTag(Screening.identityValue)?.let { editText ->
                if (editText is AppCompatEditText) {
                    editText.setText(nationalId.uppercase())
                    editText.isEnabled = false
                }
                getViewByTag("${Screening.identityValue}$errorSuffix")?.let { error ->
                    (error as TextView).apply {
                        gone()
                    }
                }
            }
        } else {
            focusNeeded?.let { focusView ->
                scrollView?.let { scrollView ->
                    scrollToView(scrollView, focusView)
                }
            }
        }
    }

    private fun phoneNumberError(errorVisibility: Int) {
        getViewByTag("${Screening.phoneNumber}$errorSuffix")?.let { tvError ->
            (tvError as TextView).apply {
                text = getString(R.string.default_user_input_error)
                visibility = errorVisibility
                if (errorVisibility == View.VISIBLE && focusNeeded == null) focusNeeded =
                    getViewByTag(Screening.phoneNumber + titleSuffix) ?: this
            }
        }
    }

    private fun firstNameError(nationalId: String, id: String, errorMessage: String) {
        getViewByTag(id + errorSuffix)?.let { tvError ->
            (tvError as TextView).apply {
                text = errorMessage
                if (nationalId.isEmpty()) {
                    visibility = View.VISIBLE
                    focusNeeded = getViewByTag(id + titleSuffix) ?: this
                } else visibility = View.GONE
            }
        }
    }

    private fun lastNameError(errorVisibility: Int, errorMessage: String) {
        getViewByTag("${Screening.lastName}$errorSuffix")?.let { tvError ->
            (tvError as TextView).apply {
                text = errorMessage
                visibility = errorVisibility
                if (errorVisibility == View.VISIBLE && focusNeeded == null) focusNeeded =
                    getViewByTag("${Screening.lastName}$titleSuffix") ?: this
            }
        }
    }

    private fun phoneNumber(nationalId: String): Pair<String?, Int?> {
        var nId = nationalId
        var errorVisibility: Int? = null
        try {
            getViewByTag(Screening.phoneNumber)?.let { editText ->
                if (editText is AppCompatEditText && (!editText.text.isNullOrBlank()) && Validator.isValidMobileNumber(
                        editText.text.toString()
                    )
                ) {
                    errorVisibility = View.GONE
                    if (nId.isNotEmpty()) {
                        val input = editText.text!!.trim().replace("\\s".toRegex(), "")
                        nId = if (input.length > 5) {
                            val phnStartIndex = input.length - 5

                            "$nId${
                                input.substring(phnStartIndex)
                            }"
                        } else "$nId$input"
                    }
                } else {
                    nId = ""
                    errorVisibility = View.VISIBLE
                }
            }
        } catch (_: Exception) {
            //Exception - Catch block
        }
        return Pair(nId, errorVisibility)
    }

    private fun validateNationalId(input: String, nId: String): String {
        return if (input.length >= 4) "$nId${
            input.substring(
                0, 4
            )
        }"
        else "$nId$input"
    }

    private fun lastName(nationalId: String): Triple<String?, Int?, String> {
        var nId = nationalId
        var errorVisibility: Int? = null
        var errorMessage: String = getString(R.string.error_label)
        getViewByTag(Screening.lastName)?.let { editText ->
            if (editText is AppCompatEditText && ((!editText.text.isNullOrBlank()) && checkMinLength(
                    Screening.lastName, editText.text?.trim()?.length
                ))
            ) {
                errorVisibility = View.GONE
                if (nId.isNotEmpty()) {
                    val input = editText.text!!.trim().replace("\\s".toRegex(), "")
                    if (onlyAlphabet(Screening.lastName, input)) {
                        nId = validateNationalId(input, nId)
                    } else {
                        nId = ""
                        errorVisibility = View.VISIBLE
                        errorMessage = getString(R.string.only_alphabets_validation)
                    }
                }
            } else {
                nId = ""
                errorVisibility = View.VISIBLE
            }
        }
        return Triple(nId, errorVisibility, errorMessage)
    }

    private fun firstName(
        id: String,
        defaultError: String = getString(R.string.default_user_input_error)
    ): Pair<String?, String> {
        var nationalId: String? = null
        var errorMessage: String = defaultError
        getViewByTag(id)?.let { editText ->
            if (editText is AppCompatEditText && ((!editText.text.isNullOrBlank()) && checkMinLength(
                    id, editText.text?.trim()?.length
                ))
            ) {
                val input = editText.text!!.trim().replace("\\s".toRegex(), "")
                if (onlyAlphabet(id, input)) {
                    nationalId =
                        if (input.length >= 4) input.substring(
                            0, 4
                        )
                        else input
                } else errorMessage = getString(R.string.only_alphabets_validation)
            }
        }
        return Pair(nationalId, errorMessage)
    }


    private fun checkMinLength(name: String, actualLength: Int?): Boolean {
        val minLength = serverData?.first { it.id == name }?.minLength
        if (minLength == null) {
            return true
        } else {
            if (actualLength != null && actualLength < minLength) {
                return false
            }
        }
        return true
    }

    private fun createBPView(serverViewModel: FormLayout) {
        val binding = BpReadingLayoutBinding.inflate(LayoutInflater.from(context))
        serverViewModel.apply {
            binding.root.tag = id + rootSuffix
            binding.tvErrorMessage.tag = id + errorSuffix
            binding.tvInstructionBloodPressure.tag = id + titleSuffix
            "${0}-${diastolicSuffix}".also { binding.etDiastolicOne.tag = it }
            "${0}-${systolicSuffix}".also { binding.etSystolicOne.tag = it }
            "${0}-${pulseSuffix}".also { binding.etPulseOne.tag = it }
            "${1}-${diastolicSuffix}".also { binding.etDiastolicTwo.tag = it }
            "${1}-${systolicSuffix}".also { binding.etSystolicTwo.tag = it }
            "${1}-${pulseSuffix}".also { binding.etPulseTwo.tag = it }
            "${2}-${diastolicSuffix}".also { binding.etDiastolicThree.tag = it }
            "${2}-${systolicSuffix}".also { binding.etSystolicThree.tag = it }
            "${2}-${pulseSuffix}".also { binding.etPulseThree.tag = it }
            binding.tvSnoReadingTwo.isEnabled = false
            binding.etSystolicTwo.isEnabled = false
            binding.etDiastolicTwo.isEnabled = false
            binding.etPulseTwo.isEnabled = false
            binding.separatorRowTwo.isEnabled = false
            binding.tvSnoReadingThree.isEnabled = false
            binding.etSystolicThree.isEnabled = false
            binding.etDiastolicThree.isEnabled = false
            binding.etPulseThree.isEnabled = false
            binding.separatorRowThree.isEnabled = false
            val list = Screening.getEmptyBPReading(totalCount ?: 2)
            resultHashMap[id] = list
            binding.instructionsLayout.safeClickListener {
                instructions?.let {
                    listener.onInstructionClicked(
                        id = id,
                        title = titleCulture ?: title,
                        informationList = it,
                        description = getString(R.string.bp_measure)
                    )
                }
            }
            if (list.size > 2) {
                binding.bpReadingThree.visibility = View.VISIBLE
            } else {
                binding.bpReadingThree.visibility = View.GONE
            }

            val inputFilter = arrayListOf<InputFilter>()
            maxLength?.let {
                inputFilter.add(InputFilter.LengthFilter(it))
            }
            if (inputFilter.isNotEmpty()) {
                binding.etSystolicOne.filters = inputFilter.toTypedArray()
                binding.etDiastolicOne.filters = inputFilter.toTypedArray()
                binding.etPulseOne.filters = inputFilter.toTypedArray()
                binding.etSystolicTwo.filters = inputFilter.toTypedArray()
                binding.etDiastolicTwo.filters = inputFilter.toTypedArray()
                binding.etPulseTwo.filters = inputFilter.toTypedArray()
                binding.etSystolicThree.filters = inputFilter.toTypedArray()
                binding.etDiastolicThree.filters = inputFilter.toTypedArray()
                binding.etPulseThree.filters = inputFilter.toTypedArray()
            }

            binding.etSystolicOne.addTextChangedListener {
                checkInputsAndEnableNextField(binding.tvSnoReading.text, binding, list)
            }
            binding.etDiastolicOne.addTextChangedListener {
                checkInputsAndEnableNextField(binding.tvSnoReading.text, binding, list)
            }
            binding.etPulseOne.addTextChangedListener {
                checkInputsAndEnableNextField(binding.tvSnoReading.text, binding, list)
            }

            binding.etSystolicTwo.addTextChangedListener {
                checkInputsAndEnableNextField(binding.tvSnoReadingTwo.text, binding, list)
            }
            binding.etDiastolicTwo.addTextChangedListener {
                checkInputsAndEnableNextField(binding.tvSnoReadingTwo.text, binding, list)
            }
            binding.etPulseTwo.addTextChangedListener {
                checkInputsAndEnableNextField(binding.tvSnoReadingTwo.text, binding, list)
            }
            binding.etSystolicThree.addTextChangedListener {
                checkInputsAndEnableNextField(binding.tvSnoReadingThree.text, binding, list)
            }
            binding.etDiastolicThree.addTextChangedListener {
                checkInputsAndEnableNextField(binding.tvSnoReadingThree.text, binding, list)
            }
            binding.etPulseThree.addTextChangedListener {
                checkInputsAndEnableNextField(binding.tvSnoReadingThree.text, binding, list)
            }

            getFamilyView(family)?.addView(binding.root) ?: kotlin.run {
                parentLayout.addView(binding.root)
            }
            setViewVisibility(visibility, binding.root)
            // setViewEnableDisable(isEnabled, binding.root)
        }
    }

    private fun checkInputsAndEnableNextField(
        text: CharSequence, binding: BpReadingLayoutBinding, list: ArrayList<BPModel>
    ) {
        if (text == getString(R.string.sno_1)) {
            val systolicReadingOne = binding.etSystolicOne.text?.toString()
            val diastolicReadingOne = binding.etDiastolicOne.text?.toString()
            val pulseReadingOne = binding.etPulseOne.text?.toString()
            if (list.size > 0) {
                val model = list[0]
                model.systolic = systolicReadingOne?.toDoubleOrNull()
                model.diastolic = diastolicReadingOne?.toDoubleOrNull()
                model.pulse = pulseReadingOne?.toDoubleOrNull()
            }
            if (!systolicReadingOne.isNullOrBlank() && !diastolicReadingOne.isNullOrBlank()) {
                binding.tvSnoReadingTwo.isEnabled = true
                binding.etSystolicTwo.isEnabled = true
                binding.etDiastolicTwo.isEnabled = true
                binding.etPulseTwo.isEnabled = true
                binding.separatorRowTwo.isEnabled = true
            }
        } else if (text == getString(R.string.sno_2)) {
            val systolicReading = binding.etSystolicTwo.text?.toString()
            val diastolicReading = binding.etDiastolicTwo.text?.toString()
            val pulseReading = binding.etPulseTwo.text?.toString()
            if (list.size > 1) {
                val model = list[1]
                model.systolic = systolicReading?.toDoubleOrNull()
                model.diastolic = diastolicReading?.toDoubleOrNull()
                model.pulse = pulseReading?.toDoubleOrNull()
            }
            if (!systolicReading.isNullOrBlank() && !diastolicReading.isNullOrBlank()) {
                binding.tvSnoReadingThree.isEnabled = true
                binding.etSystolicThree.isEnabled = true
                binding.etDiastolicThree.isEnabled = true
                binding.etPulseThree.isEnabled = true
                binding.separatorRowThree.isEnabled = true
            }
        } else if (text == getString(R.string.sno_3)) {
            val systolicReading = binding.etSystolicThree.text?.toString()
            val diastolicReading = binding.etDiastolicThree.text?.toString()
            val pulseReading = binding.etPulseThree.text?.toString()
            if (list.size > 2) {
                val model = list[2]
                model.systolic = systolicReading?.toDoubleOrNull()
                model.diastolic = diastolicReading?.toDoubleOrNull()
                model.pulse = pulseReading?.toDoubleOrNull()
            }
        }
    }

    private fun createTimeView(serverViewModel: FormLayout) {
        val binding = TimeViewLayoutBinding.inflate(LayoutInflater.from(context))
        serverViewModel.apply {
            binding.root.tag = id + rootSuffix
            binding.tvTitle.tag = id + titleSuffix
            binding.tvErrorMessage.tag = id + errorSuffix
            binding.etHour.tag = binding.etHour.id
            binding.etMinute.tag = binding.etMinute.id
            binding.tvTitle.text = translateTitle(titleCulture, title, translate)
            binding.llDate.tag = id + lastMealTypeDateSuffix
            binding.llTimeGroup.tag = id + lastMealTypeMeridiem
            dayOptionsList?.let {
                val view = SingleSelectionCustomView(context)
                view.tag = id + lastMealTypeDateSuffix
                view.addViewElements(
                    it,
                    translate,
                    resultHashMap,
                    Pair(id + lastMealTypeDateSuffix, null),
                    serverViewModel,
                    singleSelectionCallbackForDate
                )
                binding.llDate.addView(view)
            }
            timeOptionsList?.let {
                val view = SingleSelectionCustomView(context)
                view.tag = id + lastMealTypeMeridiem
                view.addViewElements(
                    it,
                    translate,
                    resultHashMap,
                    Pair(id + lastMealTypeMeridiem, null),
                    serverViewModel,
                    singleSelectionCallbackForTime
                )
                binding.llTimeGroup.addView(view)
            }
            if (isMandatory) {
                binding.tvTitle.markMandatory()
            }
            binding.etHour.addTextChangedListener {
                storeTimeValue(Hour, it?.toString(), id)
            }
            binding.etMinute.addTextChangedListener {
                storeTimeValue(Minute, it?.toString(), id)
            }
            getFamilyView(family)?.addView(binding.root) ?: kotlin.run {
                parentLayout.addView(binding.root)
            }
            setViewVisibility(visibility, binding.root, true)
            setViewEnableDisable(isEnabled, binding.root, true)
        }
    }

    private var singleSelectionCallbackForDate: ((selectedID: Any?, elementId: Pair<String, String?>, serverViewModel: FormLayout, name: String?) -> Unit)? =
        { selectedId, elementID, serverViewModel, name ->
            saveSelectedOptionValue(elementID, selectedId, serverViewModel, name)
        }

    private var singleSelectionCallbackForTime: ((selectedID: Any?, elementId: Pair<String, String?>, serverViewModel: FormLayout, name: String?) -> Unit)? =
        { selectedId, elementID, serverViewModel, name ->
            saveSelectedOptionValue(elementID, selectedId, serverViewModel, name)
        }

    private fun storeTimeValue(key: String, value: String?, id: String) {
        if (resultHashMap.containsKey(id)) {
            if (resultHashMap[id] is Map<*, *>) {
                if (!value.isNullOrBlank()) {
                    (resultHashMap[id] as java.util.HashMap<String, String>)[key] = value
                } else {
                    (resultHashMap[id] as java.util.HashMap<String, String>).remove(key)
                }
            } else if (resultHashMap[id] is String) {
                resultHashMap.remove(id)
                storeTimeValue(key, value, id)
            }
        } else {
            if (!value.isNullOrBlank()) {
                val map = java.util.HashMap<String, String>()
                map[key] = value
                resultHashMap[id] = map
            }
        }
    }
    private fun createEditTextArea(serverViewModel: FormLayout) {
        val binding = EdittextAreaLayoutBinding.inflate(LayoutInflater.from(context))
        serverViewModel.apply {
            binding.root.tag = id + rootSuffix
            binding.tvTitle.tag = id + titleSuffix
            binding.etUserInput.tag = id
            binding.tvErrorMessage.tag = id + errorSuffix
            binding.tvNationalIdAction.visibility = View.GONE
            binding.tvKey.tag = id + tvKey
            binding.tvValue.tag = id + tvValue
            binding.bgLastMeal.tag = id + rootSummary
           // checkGenerateAction(this, binding)
            binding.tvTitle.text = updateTitle(title, translate, titleCulture, unitMeasurement)

           /* maxLines?.let { binding.etUserInput.setLines(it) }
                ?: binding.etUserInput.setSingleLine()
*/
            if (isMandatory) {
                binding.tvTitle.markMandatory()
            }

            defaultValue?.let {
                binding.etUserInput.setText(it)
                resultHashMap[id] = it
            }
            maxDecimalPlaces?.let {
                binding.etUserInput.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter(it))
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

            if (applyTwoDigitPrecision == true) {
                inputFilter.add(DecimalInputFilter())
            }

            if (inputFilter.isNotEmpty()) {
                try {
                   binding.etUserInput.filters = inputFilter.toTypedArray()
                } catch (_: Exception) {
                    //Exception - Catch block
                }
            }

           /* inputType?.let {
                when (it) {
                    InputType.TYPE_CLASS_PHONE, InputType.TYPE_CLASS_NUMBER -> binding.etUserInput.inputType =
                        InputType.TYPE_CLASS_NUMBER

                    InputType.TYPE_NUMBER_FLAG_DECIMAL -> binding.etUserInput.inputType =
                        InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL

                    else -> {
                        binding.etUserInput.inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                    }
                }
            }*/

            getFamilyView(family)?.addView(binding.root) ?: kotlin.run {
                parentLayout.addView(binding.root)
            }

            binding.etUserInput.addTextChangedListener { editable: Editable? ->
                when {
                    editable.isNullOrBlank() -> {
                        if (editScreen == true) {
                            if ((inputType != null && (inputType == InputType.TYPE_CLASS_NUMBER ||
                                        inputType == InputType.TYPE_NUMBER_FLAG_DECIMAL))
                            ) {
                                resultHashMap.remove(id)
                            } else {
                                resultHashMap[id] = ""
                            }
                        } else {
                            resultHashMap.remove(id)
                        }
                        setConditionalVisibility(serverViewModel, null)
                    }

                    else -> {
                        if ((inputType != null && (inputType == InputType.TYPE_CLASS_NUMBER ||
                                    inputType == InputType.TYPE_NUMBER_FLAG_DECIMAL))
                        ) {
                            val resultValue = editable.trim().toString().toDoubleOrNull()
                            resultValue?.let {
                                resultHashMap[id] = resultValue
                            }
                        } else
                            resultHashMap[id] = editable.trim().toString()
                        setConditionalVisibility(serverViewModel, editable.trim().toString())
                    }
                }
            }
            setViewVisibility(visibility, binding.root)
            setViewEnableDisable(isEnabled, binding.root)
        }
    }

    private fun checkGenerateAction(serverViewModel: FormLayout, binding: EdittextLayoutBinding) {
        serverViewModel.apply {
            if (isNeedAction) {
                when (id) {
                    Screening.identityValue -> {
                        binding.tvNationalIdAction.visibility = View.VISIBLE
                        val clickableSpan = object : ClickableSpan() {
                            override fun onClick(mView: View) {
                                // action click
                                generateNationalId()
                            }

                            override fun updateDrawState(ds: TextPaint) {
                                super.updateDrawState(ds)
                                ds.isUnderlineText = false
                            }
                        }
                        val text = context.getString(R.string.don_t_have_id_generate_id)
                        var index = text.indexOf("?")
                        index = if (index >= 0) index + 1 else 0
                        binding.tvNationalIdAction.text = getSpannableString(
                            clickableSpan,
                            text, index
                        )
                        binding.tvNationalIdAction.movementMethod = LinkMovementMethod.getInstance()
                    }
                }
            }
        }
    }

    private fun checkGenerateAction(serverViewModel: FormLayout, binding: EdittextAreaLayoutBinding) {
        serverViewModel.apply {
            if (isNeedAction) {
                when (id) {
                    Screening.identityValue -> {
                        binding.tvNationalIdAction.visibility = View.VISIBLE
                        val clickableSpan = object : ClickableSpan() {
                            override fun onClick(mView: View) {
                                // action click
                                generateNationalId()
                            }

                            override fun updateDrawState(ds: TextPaint) {
                                super.updateDrawState(ds)
                                ds.isUnderlineText = false
                            }
                        }
                        val text = context.getString(R.string.don_t_have_id_generate_id)
                        var index = text.indexOf("?")
                        index = if (index >= 0) index + 1 else 0
                        binding.tvNationalIdAction.text = getSpannableString(
                            clickableSpan,
                            text, index
                        )
                        binding.tvNationalIdAction.movementMethod = LinkMovementMethod.getInstance()
                    }
                }
            }
        }
    }

    private fun createRadioGroup(serverViewModel: FormLayout) {
        val binding = RadioGroupLayoutBinding.inflate(LayoutInflater.from(context))
        serverViewModel.apply {
            binding.root.tag = id + rootSuffix
            binding.rgGroup.tag = id
            binding.tvTitle.tag = id + titleSuffix
            binding.tvErrorMessage.tag = id + errorSuffix
            binding.tvTitle.text = translateTitle(titleCulture, title, translate)
            optionsList?.forEachIndexed { index, map ->
                val name = map[DefinedParams.NAME]
                if (name != null && name is String) {
                    val radioButton = RadioButton(binding.rgGroup.context)
                    radioButton.id = index
                    radioButton.tag = map[DefinedParams.ID]
                    radioButton.setPadding(20.dp, 0, 20.dp, 0)
                    val colorStateList = ColorStateList(
                        arrayOf(
                            intArrayOf(-android.R.attr.state_enabled),
                            intArrayOf(-android.R.attr.state_checked),
                            intArrayOf(android.R.attr.state_checked)
                        ), intArrayOf(
                            ContextCompat.getColor(
                                context,
                                R.color.navy_blue_20_alpha
                            ),  // disabled
                            ContextCompat.getColor(context, R.color.purple),  // disabled
                            ContextCompat.getColor(context, R.color.purple) // enabled
                        )
                    )
                    val textColorStateList = ColorStateList(
                        arrayOf(
                            intArrayOf(-android.R.attr.state_enabled),
                            intArrayOf(-android.R.attr.state_checked),
                            intArrayOf(android.R.attr.state_checked)
                        ), intArrayOf(
                            ContextCompat.getColor(
                                context,
                                R.color.navy_blue_20_alpha
                            ),  // disabled
                            ContextCompat.getColor(context, R.color.navy_blue), // enabled
                            ContextCompat.getColor(context, R.color.purple)
                        )
                    )
                    radioButton.setTextColor(textColorStateList)
                    radioButton.buttonTintList = colorStateList
                    radioButton.invalidate()
                    radioButton.textSizeSsp = SSP16
                    radioButton.text = isTranslatedOrNot(map, name, translate)
                    val optionVisibility = map[DefinedParams.VISIBILITY]
                    setOptionVisibility(optionVisibility, radioButton)
                    radioButton.layoutParams = RadioGroup.LayoutParams(
                        RadioGroup.LayoutParams.WRAP_CONTENT,
                        RadioGroup.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                    radioButton.typeface = ResourcesCompat.getFont(context, R.font.inter_regular)
                    binding.rgGroup.addView(radioButton)
                }
            }
            binding.rgGroup.orientation = orientation ?: LinearLayout.HORIZONTAL

            if (isMandatory)
                binding.tvTitle.markMandatory()

            getFamilyView(family)?.addView(binding.root) ?: kotlin.run {
                parentLayout.addView(binding.root)
            }

            binding.rgGroup.setOnCheckedChangeListener { _, checkedId ->
                checkRadioGroupId(checkedId, optionsList, id, serverViewModel)
                changeRadioGroupTypeFace(checkedId, binding.rgGroup)
            }
            setViewVisibility(visibility, binding.root)
            setViewEnableDisable(isEnabled, binding.root)
        }
    }

    private fun checkRadioGroupId(
        checkedId: Int,
        optionsList: java.util.ArrayList<Map<String, Any>>?,
        id: String,
        serverViewModel: FormLayout
    ) {
        if (checkedId >= 0) {
            optionsList?.let {
                val map = it[checkedId]
                resultHashMap[id] = map[DefinedParams.ID] as Any
                setConditionalVisibility(
                    serverViewModel,
                    it[checkedId][DefinedParams.NAME] as String? ?: ""
                )
            }
        }
    }

    private fun changeRadioGroupTypeFace(radioButtonId: Int, rootView: RadioGroup) {

        rootView.let {
            rootView.children.iterator().forEach {
                val child = it as RadioButton
                if (child.id == radioButtonId) {
                    child.typeface = ResourcesCompat.getFont(this, R.font.inter_bold)
                } else {
                    child.typeface = ResourcesCompat.getFont(this, R.font.inter_regular)
                }
            }
        }
    }

    private fun setOptionVisibility(optionVisibility: Any?, radioButton: View) {
        if (optionVisibility is String) {
            setViewVisibility(optionVisibility, radioButton)
        }
    }


    private fun createSingleSelectionView(serverViewModel: FormLayout) {
        serverViewModel.apply {
            val binding = LayoutSingleSelectionBinding.inflate(LayoutInflater.from(context))
            binding.root.tag = id + rootSuffix
            binding.tvTitle.tag = id + titleSuffix
            binding.tvErrorMessage.tag = id + errorSuffix
            binding.tvTitle.text = translateTitle(titleCulture, title, translate)
            optionsList?.let {
                val view = SingleSelectionCustomView(context)
                view.tag = id
                view.addViewElements(
                    it,
                    translate,
                    resultHashMap,
                    Pair(id, null),
                    serverViewModel,
                    singleSelectionCallback
                )
                binding.selectionGroup.addView(view)
            }
            if (isMandatory)
                binding.tvTitle.markMandatory()

            isInfo?.let {
                binding.ivInfo.visibility = getVisibility(it)
            }

            binding.ivInfo.setOnClickListener {
                listener.onInstructionClicked(
                    id,
                    title,
                    dosageListModel = serverViewModel.dosageListItems
                )
            }

            getFamilyView(family)?.addView(binding.root) ?: kotlin.run {
                parentLayout.addView(binding.root)
            }
            setViewVisibility(visibility, binding.root)
            setViewEnableDisable(isEnabled, binding.root)
        }
    }

    private var singleSelectionCallback: ((selectedID: Any?, elementId: Pair<String, String?>, serverViewModel: FormLayout, name: String?) -> Unit)? =
        { selectedId, elementID, serverViewModel, name ->
            saveSelectedOptionValue(elementID, selectedId, serverViewModel, name)
            if ((selectedId as? String).equals(female, true)) {
                listener.onAgeCheckForPregnancy()
            }
            callback?.invoke(resultHashMap,elementID.first)
            listener.onUpdateInstruction(elementID.first, selectedId)
        }

    private fun isResultAvailable(key: String, value: String): Boolean {
        return resultHashMap.contains(gender) && resultHashMap[gender] == female
    }

    fun getResultMap(): HashMap<String, Any> {
        return resultHashMap
    }

    fun getServerData(): List<FormLayout>? {
        return serverData
    }

    private fun saveSelectedOptionValue(
        id: Pair<String, String?>,
        idValue: Any?,
        serverViewModel: FormLayout,
        name: String?
    ) {
        idValue?.let {
            resultHashMap[id.first] = it
            setConditionalVisibility(
                serverViewModel,
                name
            )
        } ?: kotlin.run {
            setConditionalVisibility(
                serverViewModel,
                null
            )
        }
    }

    private fun createCustomSpinner(serverViewModel: FormLayout) {
        val binding = CustomSpinnerBinding.inflate(LayoutInflater.from(context))
        serverViewModel.apply {
            listener.handleMandatoryCondition(serverViewModel)
            binding.root.tag = id + rootSuffix
            binding.etUserInput.tag = id
            binding.tvTitle.tag = id + titleSuffix
            binding.tvErrorMessage.tag = id + errorSuffix
            binding.tvTitle.text = translateTitle(titleCulture, title, translate)
            (binding.etUserInput.background as? GradientDrawable)?.apply {
                setStroke(resources.getDimensionPixelSize(R.dimen._1sdp), context.getColor(R.color.edittext_stroke))
            }
            val dropDownList = java.util.ArrayList<Map<String, Any>>()
            dropDownList.add(
                hashMapOf<String, Any>(
                    DefinedParams.NAME to getString(R.string.please_select),
                    DefinedParams.ID to DefaultID
                )
            )
            if (isMandatory) {
                binding.tvTitle.markMandatory()
            }
            isInfo?.let {
                binding.ivInfo.visibility = getVisibility(it)
            }

            binding.ivInfo.setOnClickListener {
                listener.onInstructionClicked(
                    id,
                    title,
                    dosageListModel = serverViewModel.dosageListItems
                )
            }
            if (id==muacCode){
                binding.etUserInput.background= ContextCompat.getDrawable(context,R.drawable.edittext_background)
                val adapter =  CustomSpinnerAdapterCustomLayout(context, translate)
                optionsList?.let { list ->
                    addDropDownList(list, dropDownList)
                }

                adapter.setData(dropDownList)
                binding.etUserInput.adapter = adapter
                binding.etUserInput.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            adapterView: AdapterView<*>?,
                            view: View?,
                            pos: Int,
                            itemId: Long
                        ) {
                            handleSelectedItem(
                                adapter.getData(position = pos),
                                id,
                                dependentID,
                                serverViewModel
                            )
                            onPopulateCondition(condition)
                        }

                        override fun onNothingSelected(p0: AdapterView<*>?) {
                            /**
                             * usage of this method is not required
                             */
                        }
                    }
            } else {
                val adapter=CustomSpinnerAdapter(context, translate)
                optionsList?.let { list ->
                    addDropDownList(list, dropDownList)
                }

                adapter.setData(dropDownList)
                binding.etUserInput.adapter = adapter
                binding.etUserInput.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            adapterView: AdapterView<*>?,
                            view: View?,
                            pos: Int,
                            itemId: Long
                        ) {
                            handleSelectedItem(
                                adapter.getData(position = pos),
                                id,
                                dependentID,
                                serverViewModel
                            )
                            onPopulateCondition(condition)
                        }

                        override fun onNothingSelected(p0: AdapterView<*>?) {
                            /**
                             * usage of this method is not required
                             */
                        }
                    }
            }



            defaultValue?.let { value ->
                val selectedMapIndex =
                    dropDownList.indexOfFirst { it.containsKey(DefinedParams.ID) && it[DefinedParams.ID] != null && it[DefinedParams.ID] == value }

                if (selectedMapIndex > 0) {
                    val selectedMap = dropDownList[selectedMapIndex]
                    selectedMap.let { map ->
                        if (map.isNotEmpty()) {
                            binding.etUserInput.setSelection(selectedMapIndex, true)
                            resultHashMap[id] = value
                        }
                    }
                }
            }

            getFamilyView(family)?.addView(binding.root) ?: kotlin.run {
                parentLayout.addView(binding.root)
            }
            setViewVisibility(visibility, binding.root)
            setViewEnableDisable(isEnabled, binding.root)
            disableSpinner?.let {
                if (it)
                    binding.etUserInput.isEnabled = false
            }
            localDataCache?.let { localCache ->
                listener.loadLocalCache(id, localCache)
            }
        }
    }

    private fun handleSelectedItem(
        selectedItem: Map<String, Any>?,
        id: String,
        dependentID: String?,
        serverViewModel: FormLayout
    ) {
        selectedItem?.let {
            val selectedId = it[DefinedParams.ID]
            val selectedName = it[DefinedParams.NAME]
            if ((selectedId is String && selectedId == "-1")) {
                if (resultHashMap.containsKey(id)) {
                    handleId(id)
                    dependentID?.let { deptId ->
                        resetDependantSpinnerView(deptId)
                    }
                } else {
                    if (editScreen == true)
                        resultHashMap[id] = ""
                }
            } else {
                if (serverViewModel.spinnerAsObject)
                    resultHashMap[id] = it
                else
                    resultHashMap[id] =
                        it[DefinedParams.ID] as Any
                dependentID?.let { deptId ->
                    resetDependantSpinnerView(deptId)
                    listener.loadLocalCache(
                        deptId,
                        deptId,
                        it[DefinedParams.ID] as Long
                    )
                }
            }

            listener.onUpdateInstruction(id, selectedId)
            selectedIdVisibility(selectedId, serverViewModel, selectedName)
        }
    }

    private fun handleId(id: String) {
        if (editScreen == true) {
            resultHashMap[id] = ""
        } else {
            resultHashMap.remove(id)
        }
    }

    private fun selectedIdVisibility(
        selectedId: Any?,
        serverViewModel: FormLayout,
        selectedName: Any?
    ) {
        if (selectedId is String)
            setConditionalVisibility(serverViewModel, selectedId)
        else if (selectedId is Long && selectedName is String)
            setConditionalVisibility(serverViewModel, selectedName)
    }

    private fun onPopulateCondition(condition: java.util.ArrayList<ConditionalModel>?) {
        if (!condition.isNullOrEmpty()) {
            val id = condition[0].targetId
            if (!id.isNullOrBlank())
                listener.onPopulate(id)
        }
    }

    private fun addDropDownList(
        list: ArrayList<Map<String, Any>>,
        dropDownList: ArrayList<Map<String, Any>>
    ) {
        if (list.isNotEmpty()) {
            dropDownList.addAll(list)
        }
    }

    private fun resetDependantSpinnerView(dependentID: String) {
        val view = getViewByTag(dependentID)
        if (view is AppCompatSpinner && view.adapter is CustomSpinnerAdapter) {
            val dropDownList = java.util.ArrayList<Map<String, Any>>()
            dropDownList.add(
                hashMapOf<String, Any>(
                    DefinedParams.NAME to getString(R.string.please_select),
                    DefinedParams.ID to DefaultID
                )
            )
            (view.adapter as CustomSpinnerAdapter).setData(dropDownList)
            view.setSelection(0, true)
        }
    }


    private fun createCheckboxDialogView(serverViewModel: FormLayout) {
        val binding = CheckboxDialogSpinnerLayoutBinding.inflate(LayoutInflater.from(context))
        serverViewModel.apply {
            binding.root.tag = id + rootSuffix
            binding.etUserInput.tag = id
            binding.tvTitle.tag = id + titleSuffix
            binding.tvErrorMessage.tag = id + errorSuffix
            binding.tvTitle.text = translateTitle(titleCulture, title, translate)

            hint?.let {
                binding.etUserInput.hint = it
            }

            if (isMandatory) {
                binding.tvTitle.markMandatory()
            }

            binding.etUserInput.safeClickListener {
                listener.onCheckBoxDialogueClicked(id, serverViewModel, resultHashMap[id])
            }
            getFamilyView(family)?.addView(binding.root) ?: kotlin.run {
                parentLayout.addView(binding.root)
            }
            (binding.etUserInput.background as? GradientDrawable)?.apply {
                setStroke(resources.getDimensionPixelSize(R.dimen._1sdp), context.getColor(R.color.edittext_stroke))
            }
            setViewVisibility(visibility, binding.root)
            setViewEnableDisable(isEnabled, binding.root)
        }
    }

    private fun createInformationLabel(serverViewModel: FormLayout) {
        val binding = LayoutInformationLabelBinding.inflate(LayoutInflater.from(context))
        serverViewModel.apply {
            listener.handleMandatoryCondition(serverViewModel)
            binding.root.tag = id + rootSuffix
            binding.tvValue.tag = id
            binding.tvSubValue.tag = id + infoSuffixText
            binding.tvKey.tag = id + tvKey
            binding.tvValue.text = getString(R.string.hyphen_symbol)
            binding.tvKey.text = updateTitle(title, translate, titleCulture, unitMeasurement)
            backgroundColor?.let { color ->
                if (color.startsWith(getString(R.string.hash_symbol))) {
                    binding.llBase.setBackgroundColor(
                        Color.parseColor(color)
                    )
                }
            }
            getFamilyView(family)?.addView(binding.root) ?: kotlin.run {
                parentLayout.addView(binding.root)
            }
            setViewVisibility(visibility, binding.root)
            setViewEnableDisable(isEnabled, binding.root)
        }
    }


    private fun createInstructionView(serverViewModel: FormLayout) {
        serverViewModel.apply {
            var instructionsList = instructions
            if (translate && !instructionsCulture.isNullOrEmpty())
                instructionsList = instructionsCulture

            val binding = InstructionLayoutBinding.inflate(LayoutInflater.from(context))
            binding.root.tag = id + rootSuffix
            binding.tvTitle.tag = id
            binding.tvTitle.text = translateTitle(titleCulture, title, false)
            binding.clInstructionRoot.safeClickListener {
                instructionsList?.let {
                    listener.onInstructionClicked(
                        id,
                        title,
                        it,
                        dosageListModel = serverViewModel.dosageListItems
                    )
                } ?: kotlin.run {
                    listener.onInstructionClicked(
                        id,
                        title,
                        dosageListModel = serverViewModel.dosageListItems
                    )
                }
            }
            getFamilyView(family)?.addView(binding.root) ?: kotlin.run {
                parentLayout.addView(binding.root)
            }
            setViewVisibility(visibility, binding.root)
            setViewEnableDisable(isEnabled, binding.root)

        }
    }


    private fun createTextLabel(serverViewModel: FormLayout) {
        val binding = TextLabelLayoutBinding.inflate(LayoutInflater.from(context))
        serverViewModel.apply {
            binding.root.tag = id + rootSuffix
            binding.tvTitle.tag = id
            if (translate) {
                binding.tvTitle.text = titleCulture ?: title
            } else {
                binding.tvTitle.text = title
            }
            if (displayAsterisk)
                binding.tvTitle.markMandatory()
            else
                binding.tvTitle.markNonMandatory()
            textLabelColor?.let {
                binding.tvTitle.setTextColor(Color.parseColor(it))
            }
            textLabelStyle?.let {style ->
                binding.tvTitle.apply {
                    when (style) {
                        BOLD -> setTypeface(null, Typeface.BOLD)
                        ITALIC -> setTypeface(null, Typeface.ITALIC)
                        BOLD_ITALIC -> setTypeface(null, Typeface.BOLD_ITALIC)
                        else -> setTypeface(null, Typeface.NORMAL)
                    }
                }
            }
            getFamilyView(family)?.addView(binding.root) ?: kotlin.run {
                parentLayout.addView(binding.root)
            }
            setViewVisibility(visibility, binding.root)
            setViewEnableDisable(isEnabled, binding.root)
        }
    }


    private fun createMentalHealthView(serverViewModel: FormLayout) {
        val binding = MentalHealthLayoutBinding.inflate(LayoutInflater.from(context))
        serverViewModel.apply {
            binding.root.tag = id + rootSuffix
            binding.tvErrorMessage.tag = id + errorSuffix
            binding.rvMentalHealth.tag = id
            binding.rvMentalHealth.layoutManager = LinearLayoutManager(context)

            localDataCache?.let {
                listener.loadLocalCache(id, it)
            }
            getFamilyView(family)?.addView(binding.root) ?: kotlin.run {
                parentLayout.addView(binding.root)
            }
            setViewVisibility(visibility, binding.root)
            setViewEnableDisable(isEnabled, binding.root)
        }
    }

    private fun createDatePicker(serverViewModel: FormLayout) {
        val binding = DatepickerLayoutBinding.inflate(LayoutInflater.from(context))
        serverViewModel.apply {
            binding.root.tag = id + rootSuffix
            binding.etUserInput.tag = id
            hint?.let {
                if (translate) {
                    binding.etUserInput.hint = hintCulture ?: it
                } else {
                    binding.etUserInput.hint = it
                }
            }
            binding.tvErrorMessage.tag = id + errorSuffix
            if (translate) {
                binding.tvTitle.text = titleCulture ?: title
            } else {
                binding.tvTitle.text = title
            }
            binding.tvTitle.tag = id + titleSuffix

            binding.etUserInputHolder.safeClickListener {
                val dateInput = if (binding.etUserInput.text.toString().isNotEmpty())
                    DateUtils.getYearMonthAndDate(binding.etUserInput.text.toString()) else null
                showDatePicker(
                    context = context,
                    disableFutureDate = disableFutureDate ?: false,
                    minDate = getMaxDateLimit(menstrualPeriod, minDays),
                    maxDate = maxDate,
                    date = dateInput
                ) { _, year, month, dayOfMonth ->
                    val stringDate = "$dayOfMonth-$month-$year"
                    val parsedDate = DateUtils.getDatePatternDDMMYYYY().parse(stringDate)
                    parsedDate?.let {
                        binding.etUserInput.text = DateUtils.getDateDDMMYYYY().format(it)
                        resultHashMap[id] = DateUtils.getDateString(
                            parsedDate.time,
                            inputFormat = DateUtils.DATE_FORMAT_yyyyMMdd,
                            outputFormat = DATE_FORMAT_yyyyMMddHHmmssZZZZZ
                        )
                    }
                    callback?.invoke(resultHashMap,id)
                }
            }

            if (isMandatory) {
                binding.tvTitle.markMandatory()
            }

            getFamilyView(family)?.addView(binding.root) ?: kotlin.run {
                parentLayout.addView(binding.root)
            }
            (binding.etUserInput.background as? GradientDrawable)?.apply {
                setStroke(resources.getDimensionPixelSize(R.dimen._1sdp), context.getColor(R.color.edittext_stroke))
            }
            setViewVisibility(visibility, binding.root)
            setViewEnableDisable(isEnabled, binding.root)
        }
    }

    private lateinit var textWatcher: TextWatcher
    private var isDOBUpdated = false
    private fun createAgeView(serverViewModel: FormLayout) {
        val binding = AgeDobLayoutBinding.inflate(LayoutInflater.from(context))
        serverViewModel.apply {
            binding.root.tag = id + rootSuffix
            binding.etDateOfBirth.tag = id
            binding.etYears.inputType = InputType.TYPE_CLASS_NUMBER
            binding.etMonths.inputType = InputType.TYPE_CLASS_NUMBER
            binding.etWeeks.inputType = InputType.TYPE_CLASS_NUMBER
            binding.etYears.tag = id + Year
            binding.etMonths.tag = id + Month
            binding.etWeeks.tag = id + Week
            binding.ageValue.tag = id + value
            binding.tvErrorMessage.tag = id + errorSuffix
            binding.tvDateOfBirth.tag = id + titleSuffix
            textWatcher = object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    val years = binding.etYears.text.toString().toIntOrNull() ?: 0
                    val months = binding.etMonths.text.toString().toIntOrNull() ?: 0
                    val weeks = binding.etWeeks.text.toString().toIntOrNull() ?: 0

                    fillDetailsOnEditText(years, months, weeks, id, binding.etDateOfBirth)

                    listener.onAgeCheckForPregnancy()
                    callback?.invoke(resultHashMap,id)
                }

                override fun afterTextChanged(s: Editable?) {
                }
            }

            addWatcher(binding.etYears, binding.etMonths, binding.etWeeks)
            binding.etDateOfBirth.safeClickListener {
                val yearMonthWeek = if (binding.etDateOfBirth.text.isNotEmpty()) {
                    DateUtils.getYearMonthAndDate(binding.etDateOfBirth.text.toString())
                } else null
                serverViewModel.run {
                    showDatePicker(
                        context = context,
                        disableFutureDate = disableFutureDate ?: false,
                        minDate = minDate,
                        maxDate = maxDate,
                        date = yearMonthWeek
                    ) { _, year, month, dayOfMonth ->
                        val stringDate = "$dayOfMonth-$month-$year"
                        val parsedDate = DateUtils.getDatePatternDDMMYYYY().parse(stringDate)
                        parsedDate?.let {
                            binding.etDateOfBirth.text = DateUtils.getDateDDMMYYYY().format(it)
                            fillDetailsOnDatePickerSet(it, true, id)
                            callback?.invoke(resultHashMap,id)
                        }
                    }
                }
            }
            binding.etDateOfBirth.addTextChangedListener { dob ->
                listener.onAgeUpdateListener(
                    if (dob.isNullOrBlank()) 0 else CommonUtils.getAgeInYearsByDOB(dob.toString()),
                    serverData,
                    resultHashMap
                )
            }

            if (isMandatory) {
                binding.tvYear.markMandatory()
                if (CommonUtils.isCommunity()) {
                    binding.tvWeeks.markMandatory()
                    binding.tvMonths.markMandatory()
                }
                binding.tvDateOfBirth.markMandatory()
            }
            getFamilyView(family)?.addView(binding.root) ?: kotlin.run {
                parentLayout.addView(binding.root)
            }
            setViewVisibility(visibility, binding.root)
            setViewEnableDisable(isEnabled, binding.root)
        }
    }

    fun fillDetailsOnDatePickerSet(date: Date, disable: Boolean, id: String = dateOfBirth) {
        val yearView = getViewByTag(id + Year)
        val monthView = getViewByTag(id + Month)
        val weekView = getViewByTag(id + Week)
        if (yearView is AppCompatEditText && monthView is AppCompatEditText && weekView is AppCompatEditText)
            removeWatcher(yearView, monthView, weekView)

        isDOBUpdated = true

        val dobString = convertDateToStringWithUTC(date, DATE_FORMAT_yyyyMMddHHmmssZZZZZ)

        addOrUpdateDOB(dobString, id)

        val yearMonthWeeks = DateUtils.getV2YearMonthAndWeek(dobString)

        if (yearView is AppCompatEditText && monthView is AppCompatEditText && weekView is AppCompatEditText) {
            yearMonthWeeks.years.let { year ->
                yearView.setText(year.toString())
                yearView.isEnabled = disable
                resultHashMap[Year] = year
            }
            yearMonthWeeks.months.let { month ->
                monthView.setText(month.toString())
                monthView.isEnabled = disable
                resultHashMap[Month] = month
            }
            yearMonthWeeks.weeks.let { week ->
                weekView.setText(week.toString())
                weekView.isEnabled = disable
                resultHashMap[Week] = week
            }
            listener.onAgeCheckForPregnancy()
            updateAgeView(id)
            addWatcher(yearView, monthView, weekView)
        }
    }

    fun removeWatcher(
        etYears: AppCompatEditText,
        etMonths: AppCompatEditText,
        etWeeks: AppCompatEditText
    ) {
        etYears.removeTextChangedListener(textWatcher)
        etMonths.removeTextChangedListener(textWatcher)
        etWeeks.removeTextChangedListener(textWatcher)
    }

    private fun addWatcher(
        etYears: AppCompatEditText,
        etMonths: AppCompatEditText,
        etWeeks: AppCompatEditText
    ) {
        etYears.addTextChangedListener(textWatcher)
        etMonths.addTextChangedListener(textWatcher)
        etWeeks.addTextChangedListener(textWatcher)
        isDOBUpdated = false
    }

    private fun fillDetailsOnEditText(
        years: Int,
        months: Int,
        weeks: Int,
        id: String,
        etDateOfBirth: AppCompatTextView
    ) {
        // removeWatcher(etYears, etMonths, etWeeks)
        if (years == 0 && months == 0 && weeks == 0) {
            etDateOfBirth.text = ""
            removeIfContains(id)
            removeIfContains(Year)
            removeIfContains(Month)
            removeIfContains(Week)
        } else {
            resultHashMap[Year] = years
            resultHashMap[Month] = months
            resultHashMap[Week] = weeks

            val calculatedBirthDate = DateUtils.calculateBirthDate(years, months, weeks)

            etDateOfBirth.text = convertDateFormat(
                calculatedBirthDate,
                DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                DATE_ddMMyyyy
            )
            addOrUpdateDOB(calculatedBirthDate, id)
        }

        //addWatcher(etYears, etMonths, etWeeks)

        updateAgeView(id)
    }

    private fun updateAgeView(id: String) {
        val dobString = resultHashMap[id] as? String
        val ageView = getViewByTag(id + value)
        dobString?.let { dob ->
            val age = displayAge(dob, context)
            ageView?.let { view ->
                (view as? AppCompatTextView)?.text = age
            }
        } ?: run {
            ageView?.let { view ->
                (view as? AppCompatTextView)?.text = "-"
            }
        }
    }

    fun removeIfContains(key: String) {
        if (resultHashMap.containsKey(key)) {
            resultHashMap.remove(key)
        }
    }

    private fun addOrUpdateAgeValue(value: String, key: String) {
        value.toInt().let {
            resultHashMap[key] = it
        }
    }

    private fun removeDOB(
        editText: AppCompatTextView,
        etYears: AppCompatEditText,
        etMonths: AppCompatEditText,
        etWeeks: AppCompatEditText
    ) {
        val year = resultHashMap[Year] as? Int ?: 0
        val month = resultHashMap[Month] as? Int ?: 0
        val weeks = resultHashMap[Week] as? Int ?: 0
        if (year == 0 && month == 0 && weeks == 0) {
            etYears.removeTextChangedListener(textWatcher)
            etMonths.removeTextChangedListener(textWatcher)
            etWeeks.removeTextChangedListener(textWatcher)
            editText.text = ""
            etYears.addTextChangedListener(textWatcher)
            etMonths.addTextChangedListener(textWatcher)
            etWeeks.addTextChangedListener(textWatcher)
        }
    }

    private fun addOrUpdateDOB(dateOfBirth: String, id: String) {
        resultHashMap[id] = dateOfBirth
    }

    private fun showDatePicker(
        context: Context,
        disableFutureDate: Boolean = false,
        minDate: Long? = null,
        maxDate: Long? = null,
        date: Triple<Int?, Int?, Int?>? = null,
        cancelCallBack: (() -> Unit)? = null,
        callBack: (dialog: DatePicker, year: Int, month: Int, dayOfMonth: Int) -> Unit,
    ): DatePickerDialog {

        val calendar = Calendar.getInstance()
        var thisYear = calendar.get(Calendar.YEAR)
        var thisMonth = calendar.get(Calendar.MONTH)
        var thisDay = calendar.get(Calendar.DAY_OF_MONTH)
        val dialog: DatePickerDialog?

        if (date?.first != null && date.second != null && date.third != null) {
            thisYear = date.first!!
            thisMonth = date.second!!
            thisDay = date.third!!
        }

        val dateSetListener =
            DatePickerDialog.OnDateSetListener { datePicker, year, month, dayOfMonth ->
                callBack.invoke(datePicker, year, month + 1, dayOfMonth)
            }

        dialog = DatePickerDialog(
            context,
            dateSetListener,
            thisYear,
            thisMonth,
            thisDay
        )

        if (cancelCallBack != null) {
            dialog.setOnCancelListener {
                cancelCallBack.invoke()
            }
        }

        minDate?.let {
            dialog.datePicker.minDate = it
        }
        maxDate?.let {
            dialog.datePicker.maxDate = it
        }

        dialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.okay)) { dg, _ ->
            dialog.onClick(dg, DialogInterface.BUTTON_POSITIVE)
        }

        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel)) { dg, _ ->
            dialog.onClick(dg, DialogInterface.BUTTON_NEGATIVE)
        }

        if (disableFutureDate) dialog.datePicker.maxDate = System.currentTimeMillis()

        dialog.setCancelable(false)

        dialog.show()

        return dialog

    }

    private fun getFamilyView(family: String?): LinearLayout? {
        family ?: return null
        return parentLayout.findViewWithTag(family)
    }


    private fun setConditionalVisibility(
        model: FormLayout,
        actualValue: String?,
        isCheckBox: Boolean = false
    ) {
        val conditionList = model.condition
        if (conditionList.isNullOrEmpty())
            return

        conditionList.forEach { conditionalModel ->
            validateConditionModel(conditionalModel, actualValue, isCheckBox)
        }
    }


    private fun validateConditionModel(
        conditionalModel: ConditionalModel,
        actualValue: String?,
        isCheckBox: Boolean
    ) {
        conditionalModel.apply {
            if (targetId != null && targetOption != null) {
                val targetedView = parentLayout.findViewWithTag<View>(targetOption)
                if (visibility != null) {
                    checkConditionBasedRendering(
                        conditionalModel,
                        ConditionModelConfig.VISIBILITY,
                        actualValue,
                        targetedView,
                        targetOption,
                        isCheckBox
                    )
                } else if (enabled != null) {
                    checkConditionBasedRendering(
                        conditionalModel,
                        ConditionModelConfig.ENABLED,
                        actualValue,
                        targetedView,
                        targetOption,
                        isCheckBox
                    )
                }
            } else if (targetId != null) {
                val targetedView = parentLayout.findViewWithTag<View>(targetId + rootSuffix)
                visibleEnableConditionRendering(
                    visibility,
                    enabled,
                    conditionalModel,
                    actualValue,
                    targetedView,
                    isCheckBox
                )
            } else {
                return@apply
            }
        }
    }


    private fun checkConditionBasedRendering(
        conditionalModel: ConditionalModel,
        config: ConditionModelConfig,
        actualValue: String?,
        targetedView: View?,
        targetOption: String? = null,
        isCheckBox: Boolean
    ) {
        conditionalModel.apply {
            targetedView ?: return@apply
            if (!eq.isNullOrBlank()) {
                if (eq == actualValue)
                    handleConfig(true, config, visibility, enabled, targetedView)
                else if (!isCheckBox)
                    handleTargetConfig(serverData, targetId, targetedView, config, targetOption)
            } else if (lengthGreaterThan != null) {
                isLengthGreater(
                    conditionalModel,
                    actualValue,
                    targetOption,
                    targetedView,
                    config,
                    isCheckBox
                )
            } else if (!eqList.isNullOrEmpty()) {
                if (eqList!!.contains(actualValue))
                    handleConfig(true, config, visibility, enabled, targetedView)
                else if (!isCheckBox)
                    handleTargetConfig(serverData, targetId, targetedView, config, targetOption)
            }
        }
    }

    private fun isLengthGreater(
        conditionalModel: ConditionalModel,
        actualValue: String?,
        targetOption: String?,
        targetedView: View,
        config: ConditionModelConfig,
        isCheckBox: Boolean
    ) {
        conditionalModel.apply {
            if (actualValue != null && actualValue.length > lengthGreaterThan!!)
                handleConfig(false, config, visibility, enabled, targetedView)
            else if (!isCheckBox)
                handleTargetConfig(serverData, targetId, targetedView, config, targetOption)
        }
    }

    private fun handleConfig(
        resetValue: Boolean,
        config: ConditionModelConfig,
        visibility: String?,
        enabled: Boolean?,
        targetedView: View
    ) {
        if (config == ConditionModelConfig.VISIBILITY)
            setViewVisibility(visibility, targetedView, resetValue)
        else
            setViewEnableDisable(enabled, targetedView, resetValue)
    }

    private fun handleTargetConfig(
        serverData: List<FormLayout>?,
        targetId: String?,
        targetedView: View,
        config: ConditionModelConfig,
        targetOption: String?
    ) {
        val targetModel = serverData?.find { it.id == targetId }
        if (config == ConditionModelConfig.VISIBILITY) {
            var visibility: String? = null
            if (targetOption != null) {
                visibility = targetModelVisibility(targetModel, targetOption, visibility)
                removeTargetId(targetId, visibility, targetedView)
                setViewVisibility(visibility, targetedView, false)
            } else {
                visibility = targetModel?.visibility
                setViewVisibility(visibility, targetedView, true)
            }
        } else {
            var enabled: Boolean? = null
            if (targetOption != null) {
                enabled = targetModelStatus(targetModel, enabled, targetOption)
                setViewEnableDisable(enabled, targetedView, false)
            } else {
                enabled = targetModel?.isEnabled
                setViewEnableDisable(enabled, targetedView, true)
            }
        }
    }

    private fun setViewEnableDisable(
        enabled: Boolean?,
        rootLyt: View,
        resetValue: Boolean = false
    ) {
        recursiveLoopChildren(rootLyt as ViewGroup, { view ->
            view?.apply {
                if (resetValue) {
                    resetChildFormViewComponents(view)
                } else {
                    resetSpecificChildViews(view)
                }
                if (enabled == null) {
                    this.isEnabled = true
                    return@apply
                }
                this.isEnabled = enabled
            }
        }) { viewGroup ->
            if (resetValue) {
                resetChildFormViewGroupComponents(viewGroup)
            }
            viewGroup?.forEach { view ->
                view.apply {
                    if (enabled == null) {
                        this.isEnabled = true
                        return@apply
                    }
                    this.isEnabled = enabled
                }
            }
        }
    }

    private fun targetModelVisibility(
        targetModel: FormLayout?,
        targetOption: String,
        visibility: String?
    ): String? {
        var visibleOrNot = visibility
        targetModel?.optionsList?.forEach { map ->
            if (map[DefinedParams.NAME] == targetOption) {
                val value = map[DefinedParams.VISIBILITY]
                if (value is String?) {
                    visibleOrNot = value
                }
            }
        }
        return visibleOrNot
    }

    private fun targetModelStatus(
        targetModel: FormLayout?,
        status: Boolean?,
        targetOption: String
    ): Boolean? {
        var enableOrNot = status
        targetModel?.optionsList?.forEach { map ->
            if (map[DefinedParams.NAME] == targetOption) {
                val value = map[DefinedParams.isEnabled]
                if (value is Boolean?) {
                    enableOrNot = value
                }
            }
        }
        return enableOrNot
    }

    private fun removeTargetId(targetId: String?, visibility: String?, targetedView: View) {
        if (visibility == GONE && targetId != null) {
            val view = getViewByTag(targetId)
            if (view is RadioGroup && targetedView.isEnabled) {
                view.clearCheck()
                resultHashMap.remove(targetId)
            }
        }
    }

    private fun visibleEnableConditionRendering(
        visibility: String?,
        enabled: Boolean?,
        conditionalModel: ConditionalModel,
        actualValue: String?,
        targetedView: View?,
        isCheckBox: Boolean
    ) {
        if (visibility != null) {
            checkConditionBasedRendering(
                conditionalModel,
                ConditionModelConfig.VISIBILITY,
                actualValue,
                targetedView,
                isCheckBox = isCheckBox
            )
        } else if (enabled != null) {
            checkConditionBasedRendering(
                conditionalModel,
                ConditionModelConfig.ENABLED,
                actualValue,
                targetedView,
                isCheckBox = isCheckBox
            )
        }
    }

    private fun resetSpecificChildViews(view: View?) {
        view?.apply {
            val specialChildView = listOf(VIEW_TYPE_TIME)
            val model = serverData?.find { specialChildView.contains(it.viewType) }
            when (model?.viewType) {
                VIEW_TYPE_TIME -> {
                    if (!view.isEnabled) {
                        (getViewByTag(model.id + errorSuffix) as? TextView)?.gone()
                        getViewByTag(R.id.etMinute)?.let {
                            resetEditTextDatePicker(it, model)
                        }
                        getViewByTag(R.id.etHour)?.let {
                            resetEditTextDatePicker(it, model)
                        }
                        listOf(model.dayOptionsList, model.timeOptionsList).forEach { optionList ->
                            optionList?.forEach { option ->
                                val suffix = if (optionList == model.dayOptionsList) lastMealTypeDateSuffix else lastMealTypeMeridiem
                                getViewByTag(option[DefinedParams.ID] as String + "_" + model.id + suffix)?.let { view ->
                                    resultHashMap.remove(model.id)
                                    resultHashMap.remove(model.id + suffix)
                                    if (view is TextView) {
                                        isSelected = false
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setViewVisibility(visibility: String?, root: View, resetValue: Boolean = false) {

        if (resetValue && visibility != null && visibility != VISIBLE) {
            resetChildViews(root)
        }

        when (visibility) {
            VISIBLE -> {
                root.visibility = View.VISIBLE
            }

            INVISIBLE -> {
                root.visibility = View.INVISIBLE
            }

            GONE -> {
                root.visibility = View.GONE
            }

            else -> {
                root.visibility = View.VISIBLE
            }
        }
    }

    fun resetChildViews(rootLyt: View) {
        recursiveLoopChildren(rootLyt as ViewGroup, { view ->
            resetChildFormViewComponents(view)
        }) { viewGroup ->
            resetChildFormViewGroupComponents(viewGroup)
        }
    }


    private fun recursiveLoopChildren(
        parent: ViewGroup,
        viewItem: (view: View?) -> Unit,
        viewGroupItem: (viewGroup: ViewGroup?) -> Unit
    ) {
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            if (child is ViewGroup) {
                recursiveLoopChildren(child, {
                    viewItem.invoke(it)
                }) { viewGroup ->
                    viewGroupItem.invoke(viewGroup)
                }
                if (child.tag != null) {
                    viewGroupItem.invoke(child)
                }

            } else {
                if (child != null && child.tag != null) {
                    // DO SOMETHING WITH VIEW
                    viewItem.invoke(child)
                }
            }
        }
    }

    private fun resetChildFormViewComponents(view: View?) {
        view?.apply {
            val model = serverData?.find { it.id == tag }
            when (model?.viewType) {
                VIEW_TYPE_FORM_DATEPICKER,
                VIEW_TYPE_FORM_SPINNER,
                VIEW_TYPE_FORM_EDITTEXT, VIEW_TYPE_NO_OF_DAYS -> resetEditTextDatePicker(
                    this,
                    model
                )
                VIEW_TYPE_FORM_AGE -> resetAgeView(this, model)
                VIEW_TYPE_DIALOG_CHECKBOX -> resetCheckBoxDialogView(this, model)
                VIEW_INFORMATION_LABEL -> {
                    resetInformationLabel(this, model)
                }
                else -> {
                    if (view.tag.toString().lowercase().let { it.contains(VIEW_TYPE_TIME) || view.tag in listOf(R.id.etMinute, R.id.etHour) }) {
                        val specialChildView = listOf(VIEW_TYPE_TIME)
                        val model = serverData?.find { specialChildView.contains(it.viewType) }
                        if (VIEW_TYPE_TIME == model?.viewType) {
                            resetTimeView(model)
                        }
                    }
                }
            }
        }
    }

    private fun resetInformationLabel(view: View, model: FormLayout) {
        getViewByTag(model.id + tvKey)?.let {
            if ((view is TextView)) {
                view.text = model.defaultValue ?: getString(R.string.empty)
                resultHashMap.remove(model.id)
            }
        }
    }

    fun disableSingleSelection(id: String) {
        getViewByTag(id)?.let {
            if (it is ViewGroup) {
                it.forEach { view ->
                    if (view is TextView) {
                        view.isEnabled = false
                    }
                }
            }
        }
    }

    fun disableView(view: View, context: Context) {
        view.isEnabled = false
        view.setBackgroundColor(ContextCompat.getColor(context, R.color.border_gray))
    }

    private fun resetChildFormViewGroupComponents(viewGroup: ViewGroup?) {
        viewGroup?.apply {
            val model = serverData?.find { it.id == tag }
            model?.let {
                when (model.viewType) {
                    VIEW_TYPE_FORM_DATEPICKER,
                    VIEW_TYPE_FORM_EDITTEXT, VIEW_TYPE_NO_OF_DAYS -> resetEditTextDatePicker(
                        this,
                        model
                    )
                    VIEW_TYPE_FORM_SPINNER -> resetSpinner(this)
                    VIEW_TYPE_FORM_RADIOGROUP -> resetRadioGroup(this, model)
                    VIEW_TYPE_SINGLE_SELECTION -> {
                        resultHashMap.remove(model.id)
                        if (this is SingleSelectionCustomView) {
                            resetSingleSelectionChildViews()
                        }
                    }
                    //  VIEW_TYPE_FORM_CHECKBOX -> resetCheckbox(this, model)
                }
            }

        }
    }

    private fun resetEditTextDatePicker(view: View, model: FormLayout) {
        if (view is EditText) {
            model.defaultValue?.let {
                view.setText(it)
            } ?: kotlin.run {
                view.text.clear()
            }
        } else if (view is TextView) {
            model.defaultValue?.let {
                view.text = it
            } ?: kotlin.run {
                view.text = ""
            }
        }
    }

    private fun resetTimeView(model: FormLayout) {
        (getViewByTag(model.id + errorSuffix) as? TextView)?.gone()
        getViewByTag(R.id.etMinute)?.let {
            resetEditTextDatePicker(it, model)
        }
        getViewByTag(R.id.etHour)?.let {
            resetEditTextDatePicker(it, model)
        }
        listOf(model.dayOptionsList, model.timeOptionsList).forEach { optionList ->
            optionList?.forEach { option ->
                val suffix = if (optionList == model.dayOptionsList) lastMealTypeDateSuffix else lastMealTypeMeridiem
                getViewByTag(option[DefinedParams.ID] as String + "_" + model.id + suffix)?.let { view ->
                    resultHashMap.remove(model.id)
                    resultHashMap.remove(model.id + suffix)
                    if (view is TextView) {
                        view.isSelected = false
                    }
                }
            }
        }
    }

    fun getViewByTag(tag: Any): View? {
        return parentLayout.findViewWithTag(tag)
    }

    private fun resetRadioGroup(rgGroup: View, model: FormLayout, button: Int? = null) {
        if (rgGroup is RadioGroup) {
            val default = model.defaultValue
            if (default != null || button != null) {
                val count = rgGroup.childCount
                for (i in 0..count) {
                    resetRg(rgGroup, default, button)
                }
            } else {
                rgGroup.clearCheck()
                resultHashMap.remove(model.id)
            }
        }
    }

    private fun resetRg(rgGroup: RadioGroup, default: String?, button: Int?) {
        rgGroup.forEach {
            if (it is RadioButton) {
                when {
                    default != null && it.tag == default -> it.isChecked = true
                    button != null && it.id == button -> it.isChecked = true
                }
            }
        }
    }

    private fun resetAgeView(view: View, model: FormLayout) {
        resetEditTextDatePicker(view, model)
        getViewByTag(R.id.etDateOfBirth)?.let {
            resetEditTextDatePicker(it, model)
        }
        getViewByTag(R.id.etYears)?.let {
            resetEditTextDatePicker(it, model)
        }
        getViewByTag(R.id.etMonths)?.let {
            resetEditTextDatePicker(it, model)
        }
        getViewByTag(R.id.etWeeks)?.let {
            resetEditTextDatePicker(it, model)
        }
    }

    private fun resetCheckBoxDialogView(view: View, model: FormLayout) {
        (view as TextView).text = model.defaultValue ?: ""
        resultHashMap.remove(model.id)
    }

    private fun resetSpinner(spinnerView: View) {
        if (spinnerView is Spinner) {
            spinnerView.setSelection(0, true)
        }
    }

    fun formSubmitAction(view: View): Boolean {
        return if (validateInputs()) {
            hideKeyboard(view)
            listener.onFormSubmit(resultMap = resultHashMap, serverData = serverData)
            true
        } else {
            focusNeeded?.let { focusNeeded ->
                scrollView?.let { scrollView ->
                    scrollToView(scrollView, focusNeeded)
                }
            }
            false
        }
    }


    /**
     * Used to scroll to the given view.
     *
     * @param scrollViewParent Parent ScrollView
     * @param view View to which we need to scroll.
     */
    fun scrollToView(scrollViewParent: NestedScrollView, view: View) {
        // Get deepChild Offset
        val childOffset = Point()
        getDeepChildOffset(scrollViewParent, view.parent, view, childOffset)
        // Scroll to child.
        scrollViewParent.smoothScrollTo(0, childOffset.y)
    }

    /**
     * Used to get deep child offset.
     *
     *
     * 1. We need to scroll to child in scrollview, but the child may not the direct child to scrollview.
     * 2. So to get correct child position to scroll, we need to iterate through all of its parent views till the main parent.
     *
     * @param mainParent        Main Top parent.
     * @param parent            Parent.
     * @param child             Child.
     * @param accumulatedOffset Accumulated Offset.
     */
    private fun getDeepChildOffset(
        mainParent: ViewGroup,
        parent: ViewParent,
        child: View,
        accumulatedOffset: Point
    ) {
        val parentGroup = parent as ViewGroup
        accumulatedOffset.x += child.left
        accumulatedOffset.y += child.top
        if (parentGroup == mainParent) {
            return
        }
        getDeepChildOffset(mainParent, parentGroup.parent, parentGroup, accumulatedOffset)
    }


    fun validateInputs(): Boolean {
        var isValid = true
        focusNeeded = null
        serverData?.forEach { data ->
            data.apply {
                if ((isMandatory && !resultHashMap.containsKey(id)
                            && isViewVisible(id) && isViewEnabled(id))
                    ||
                    (isMandatory && resultHashMap.containsKey(id)
                            && resultHashMap[id] is String && (resultHashMap[id] as String).isEmpty())
                ) {
                    isValid = false
                    requestFocusView(data)
                } else if ((id == Screening.phoneNumber || id == headPhoneNumber || id == phoneNumber) && isMandatory && resultHashMap.containsKey(
                        id
                    )
                ) {
                    val actualValue = resultHashMap[id] as? String
                    actualValue?.let {
                        if (!startsWith.isNullOrEmpty() && !checkPhoneNumberValidOrNot(
                                it,
                                startsWith
                            )
                        ) {
                            isValid = false
                            requestFocusView(
                                data, getString(
                                    R.string.start_with_validation,
                                    startsWith?.joinToString(separator = " ${getString(R.string.or)} ")
                                        ?: ""
                                )
                            )
                        } else if (!phoneNumberConatinMaxLength(
                                contentLength ?: maxLength,
                                it
                            )
                        ) {
                            isValid = false
                            requestFocusView(data)
                        } else if (!FormFieldValidator.isValidMobileNumber(it)) {
                            isValid = false
                            requestFocusView(
                                data, getString(
                                    R.string.phone_number_invalid
                                )
                            )
                        } else {
                            hideValidationField(data)
                        }
                    } ?: run {
                        isValid = false
                        requestFocusView(data)
                    }
                } else if ((id == dateOfBirth || id == DateOfBirth) && isMandatory && resultHashMap.containsKey(id)) {
                    val actualValue = resultHashMap[id] as? String
                    maxAge?.let { ageLimit ->
                        val isValidAge = actualValue?.let {
                            val dob = DateUtils.getV2YearMonthAndWeek(it)
                            dob.years < ageLimit || (dob.years == ageLimit && dob.months == 0 && dob.weeks == 0 && dob.days == 0)
                        } ?: false

                        if (!isValidAge) {
                            isValid = false
                            requestFocusView(data, getString(R.string.dob_invalid, maxAge))
                        } else {
                            hideValidationField(data)
                        }
                    } ?: run {
                        actualValue?.let {
                            hideValidationField(data)
                        } ?: run {
                            isValid = false
                            requestFocusView(data)
                        }
                    }
                } else if (data.viewType.equals(VIEW_TYPE_FORM_BP, true)) {
                    val list = resultHashMap[id] as ArrayList<BPModel>
                    val validationBPResultModel = Validator.checkValidBPInput(
                        context, list, data
                    )
                    if (validationBPResultModel.status) {
                        hideValidationField(data)
                    } else {
                        isValid = false
                        requestFocusView(data, validationBPResultModel.message)
                    }
                } else if (data.viewType.equals(VIEW_TYPE_TIME, true)) {
                    val dateKey = id + lastMealTypeDateSuffix
                    val timeKey = id + lastMealTypeMeridiem
                    if (resultHashMap.containsKey(Screening.BloodGlucoseID) && resultHashMap[Screening.BloodGlucoseID] != null) {
                        if (resultHashMap[dateKey] != null) {
                            val result = resultHashMap[id] as? MutableMap<*, *>
                            val hour = (result?.get(Hour) as? String)?.toIntOrNull()
                            val minute = (result?.get(Minute) as? String)?.toIntOrNull()
                            if (hour != null && minute != null && hour != 0 && resultHashMap[timeKey] != null) {
                                val minHour = data.minValueForHour
                                val maxHour = data.maxValueForHour
                                val minMinute = data.minValueForMinute
                                val maxMinute = data.maxValueForMinute

                                val isValidHour =
                                    minHour != null && maxHour != null && hour in minHour..maxHour
                                val isValidMinute =
                                    minMinute != null && maxMinute != null && minute in minMinute..maxMinute

                                if (((!(minHour != null && maxHour != null)) && (!(minMinute != null && maxMinute != null))) || (isValidHour && isValidMinute)) {
                                    val res = (resultHashMap[dateKey] as? String)?.let { date ->
                                        if (date.equals(
                                                Screening.Today,
                                                ignoreCase = true
                                            ) && hour != null && minute != null && hour != 0 && resultHashMap[timeKey] != null
                                        ) {
                                            DateUtils.isValidTimeForLastMealTime(
                                                hour,
                                                minute,
                                                resultHashMap[timeKey] as String
                                            )
                                        } else {
                                            true
                                        }
                                    } ?: false
                                    if (res) {
                                        hideValidationField(data)
                                    } else {
                                        isValid = false
                                        requestFocusView(data)
                                    }
                                } else {
                                    isValid = false
                                    requestFocusView(
                                        data,
                                        getString(
                                            R.string.time_meal_error,
                                            minHour,
                                            maxHour,
                                            minMinute,
                                            maxMinute
                                        )
                                    )
                                }
                            } else {
                                isValid = false
                                requestFocusView(data)
                            }
                        } else {
                            isValid = false
                            requestFocusView(data)
                        }
                    }
                } else {
                    if (resultHashMap.containsKey(id) && data.viewType.equals(
                            VIEW_TYPE_FORM_EDITTEXT, true
                        )
                    ) {
                        val actualValue = resultHashMap[id]
                        if (actualValue is String && actualValue.isEmpty() && !isMandatory) {
                            hideValidationField(data)
                        } else {
                            isValid = validateMinMaxLength(
                                actualValue,
                                isValid,
                                data
                            )
                            if (isValid && data.onlyAlphabets == true) {
                                isValid = checkOnlyAlphabets(
                                    actualValue,
                                    isValid,
                                    data
                                )
                            }
                        }
                    } else {
                        when (data.viewType) {
                            VIEW_TYPE_METAL_HEALTH -> {
                                if (isViewVisible(id)) {
                                    if (checkValidMentalHealth(this, id)) {
                                        hideValidationField(data)
                                    } else {
                                        isValid = false
                                        requestFocusView(data)
                                    }
                                }
                            }
                            else -> {
                                hideValidationField(data)
                            }
                        }
                    }
                }


                /* if ((id == headPhoneNumber || id == phoneNumber) && isMandatory && resultHashMap.containsKey(
                         id
                     )
                 ) {
                     val actualValue = resultHashMap[id] as? String
                     actualValue?.let {
                         if (!startsWith.isNullOrEmpty() && !checkPhoneNumberValidOrNot(
                                 it,
                                 startsWith
                             )
                         ) {
                             isValid = false
                             requestFocusView(
                                 data, getString(
                                     R.string.start_with_validation,
                                     startsWith?.joinToString(separator = " ${getString(R.string.or)} ")
                                         ?: ""
                                 )
                             )
                         } else if (!phoneNumberConatinMaxLength(
                                 maxLength,
                                 it
                             )
                         ) {
                             isValid = false
                             requestFocusView(data)
                         } else if (!FormFieldValidator.isValidMobileNumber(it)) {
                             isValid = false
                             requestFocusView(
                                 data, getString(
                                     R.string.phone_number_invalid
                                 )
                             )
                         } else {
                             hideValidationField(data)
                         }
                     } ?: run {
                         isValid = false
                         requestFocusView(data)
                     }
                 } else if ((isMandatory && !resultHashMap.containsKey(id)
                             && isViewVisible(id) && isViewEnabled(id))
                     ||
                     (isMandatory && resultHashMap.containsKey(id)
                             && resultHashMap[id] is String && (resultHashMap[id] as String).isEmpty())) {
                     isValid = false
                     requestFocusView(data)
                 } else if (viewType == VIEW_TYPE_FORM_EDITTEXT && isValid) {
                     isValid = validateMinMaxLength(
                         resultHashMap[id],
                         isValid,
                         data
                     )
                     if (isValid && data.onlyAlphabets == true) {
                         isValid = checkOnlyAlphabets(
                             resultHashMap[id],
                             isValid,
                             data
                         )
                     }
                 } else {
                     hideValidationField(data)
                 }*/
            }
        }
        return isValid
    }

    fun isViewVisible(id: String): Boolean {
        val view = getViewByTag(id + rootSuffix)
        return view != null && view.visibility == View.VISIBLE
    }

    private fun isViewEnabled(id: String): Boolean {
        val view = getViewByTag(id)
        return view != null && view.isEnabled
    }

    private fun phoneNumberConatinMaxLength(maxLength: Int?, actualValue: String): Boolean {
        return (maxLength != null && actualValue.length == maxLength)
    }

    private fun checkPhoneNumberValidOrNot(
        actualValue: String,
        startsWithArray: ArrayList<String>?
    ): Boolean {
        var valid = false
        startsWithArray?.forEach { value ->
            if (actualValue.startsWith(value, true)) {
                valid = true
            }
        }
        return valid
    }

    private fun requestFocusView(serverViewModel: FormLayout, message: String? = null) {
        if (focusNeeded == null) {
            focusNeeded = showValidationMessage(serverViewModel, message)
        } else {
            showValidationMessage(serverViewModel, message)
        }
    }

    private fun validateMinMaxLength(
        actualValue: Any?,
        valid: Boolean,
        serverViewModel: FormLayout
    ): Boolean {
        var isValid = valid
        serverViewModel.apply {
            if (minLength != null && viewType == VIEW_TYPE_FORM_EDITTEXT
                && actualValue != null && actualValue is String
                && actualValue.length < minLength!!
            ) {
                isValid = false
                requestFocusView(
                    serverViewModel,
                    getString(
                        R.string.min_char_length_validation,
                        minLength!!.toString()
                    )
                )
            } else if (maxValue != null || minValue != null) {
                if (maxValue != null && minValue != null) {
                    if (actualValue is String) {
                        actualValue.toDoubleOrNull()?.let { value ->
                            if (value < minValue!! || value > maxValue!!) {
                                isValid = false
                                requestFocusView(
                                    serverViewModel,
                                    getString(
                                        R.string.general_min_max_validation,
                                        CommonUtils.getDecimalFormatted(
                                            minValue!!
                                        ),
                                        CommonUtils.getDecimalFormatted(
                                            maxValue!!
                                        )
                                    )
                                )
                            } else {
                                hideValidationField(serverViewModel)
                            }
                        }
                    } else if (actualValue is Number) {
                        actualValue.toDouble().let { value ->
                            if (value < minValue!! || value > maxValue!!) {
                                isValid = false
                                requestFocusView(
                                    serverViewModel,
                                    getString(
                                        R.string.general_min_max_validation,
                                        CommonUtils.getDecimalFormatted(
                                            minValue!!
                                        ),
                                        CommonUtils.getDecimalFormatted(
                                            maxValue!!
                                        )
                                    )
                                )
                            } else {
                                hideValidationField(serverViewModel)
                            }
                        }
                    } else {
                        hideValidationField(serverViewModel)
                    }
                } else if (minValue != null) {
                    if (actualValue is String) {
                        actualValue.toDoubleOrNull()?.let { value ->
                            if (value < minValue!!) {
                                isValid = false
                                requestFocusView(
                                    serverViewModel,
                                    getString(
                                        R.string.general_min_validation,
                                        CommonUtils.getDecimalFormatted(
                                            minValue!!
                                        )
                                    )
                                )
                            } else {
                                hideValidationField(serverViewModel)
                            }
                        }
                    } else if (actualValue is Number) {
                        actualValue.toDouble().let { value ->
                            if (value < minValue!!) {
                                isValid = false
                                requestFocusView(
                                    serverViewModel,
                                    getString(
                                        R.string.general_min_validation,
                                        CommonUtils.getDecimalFormatted(
                                            minValue!!
                                        )
                                    )
                                )
                            } else {
                                hideValidationField(serverViewModel)
                            }
                        }
                    } else {
                        hideValidationField(serverViewModel)
                    }
                } else if (maxValue != null) {
                    if (actualValue is String) {
                        actualValue.toDoubleOrNull()?.let { value ->
                            if (value > maxValue!!.toDouble()) {
                                isValid = false
                                requestFocusView(
                                    serverViewModel,
                                    getString(
                                        R.string.general_max_validation,
                                        CommonUtils.getDecimalFormatted(
                                            maxValue!!
                                        )
                                    )
                                )
                            } else {
                                hideValidationField(serverViewModel)
                            }
                        }
                    } else if (actualValue is Number) {
                        actualValue.toDouble().let { value ->
                            if (value > maxValue!!.toDouble()) {
                                isValid = false
                                requestFocusView(
                                    serverViewModel,
                                    getString(
                                        R.string.general_max_validation,
                                        CommonUtils.getDecimalFormatted(
                                            maxValue!!
                                        )
                                    )
                                )
                            } else {
                                hideValidationField(serverViewModel)
                            }
                        }
                    } else {
                        hideValidationField(serverViewModel)
                    }
                }
            } else if (contentLength != null) {
                if (actualValue is Number) {
                    val actualValueString =
                        CommonUtils.getDecimalFormatted(actualValue)
                    if (contentLength == actualValueString.length) {
                        hideValidationField(serverViewModel)
                    } else {
                        isValid = false
                        requestFocusView(serverViewModel)
                    }
                } else {
                    val actualValueString = actualValue.toString()
                    if (contentLength == actualValueString.length) {
                        hideValidationField(serverViewModel)
                    } else {
                        isValid = false
                        requestFocusView(serverViewModel)
                    }
                }
            } else {
                hideValidationField(serverViewModel)
            }
        }
        return isValid
    }

    private fun showValidationMessage(serverViewModel: FormLayout, message: String? = null): View? {
        serverViewModel.apply {
            val view = getViewByTag(serverViewModel.id + errorSuffix)
            if (view is TextView) {
                view.visibility = View.VISIBLE
                if (message != null) {
                    view.text = message
                } else {
                    view.text = getErrorMessageFromJSON(errorMessage, cultureErrorMessage)
                }
                return getViewByTag(serverViewModel.id + titleSuffix)
            }
        }
        return null
    }

    private fun getErrorMessageFromJSON(
        errorMessage: String?,
        cultureErrorMessage: String?
    ): String {

        return if (translate && !(cultureErrorMessage.isNullOrEmpty() || cultureErrorMessage.isBlank()))
            cultureErrorMessage
        else if (!(errorMessage.isNullOrEmpty() || errorMessage.isBlank()))
            errorMessage
        else getString(R.string.default_user_input_error)
    }

    private fun hideValidationField(serverViewModel: FormLayout) {
        serverViewModel.apply {
            val view = getViewByTag(serverViewModel.id + errorSuffix)
            if (view != null && view is TextView && view.visibility == View.VISIBLE) {
                view.visibility = View.GONE
            }
        }
    }

    fun validateCheckboxDialogue(
        id: String,
        serverViewModel: FormLayout,
        resultMap: ArrayList<HashMap<String, Any>>
    ) {
        if (resultMap.isEmpty()) {
            if (resultHashMap.containsKey(id)) {
                resultHashMap.remove(id)
            }
        } else {
            resultHashMap[id] = resultMap
        }
        getViewByTag(id)?.let { view ->
            if (view is AppCompatTextView) {
                view.text = setCheckBoxDialogText(resultHashMap, id)
            }
        }
        if (isContainsOther(resultMap)) {
            setConditionalVisibility(
                serverViewModel,
                DefinedParams.Other
            )
        } else {
            setConditionalVisibility(
                serverViewModel,
                null
            )
        }
        callback?.invoke(resultHashMap,id)
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
                        "${mapList.size - 1} and ${getString(R.string.other)} ${
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

    fun setValueForView(value: Any?, view: View) {
        if (view is AppCompatEditText) {
            if (value is String) {
                view.setText(value)
            } else if (value is Int) {
                view.setText(value.toString())
            } else {
                view.setText("")
            }
        }

        if (view is AppCompatTextView) {
            when (value) {
                is String -> {
                    view.text = value
                }

                is Int -> {
                    view.text = value.toString()
                }

                else -> {
                    view.text = ""
                }
            }
        }

        if (view is Spinner) {
            val adapter = view.adapter
            if (adapter != null && adapter is CustomSpinnerAdapter && value is Long) {
                val selectedIndex = adapter.getIndexOfItem(value)
                if (selectedIndex != -1)
                    view.setSelection(selectedIndex, true)
            }
            if (adapter != null && adapter is CustomSpinnerAdapter && value is String) {
                val selectedIndex = adapter.getIndexOfItemByName(value)
                if (selectedIndex != -1)
                    view.setSelection(selectedIndex, true)
            }
        }
    }

    fun spinnerDataInjection(data: LocalSpinnerResponse, mapList: ArrayList<Map<String, Any>>) {
        val spinner = getViewByTag(data.tag) as? AppCompatSpinner ?: return
        val mandatory = serverData?.find { it.id == data.tag }?.isMandatory ?: false
        if (spinner.adapter is CustomSpinnerAdapter) {
            if (!mandatory || mapList.size != 1) {
                mapList.add(0, createDefaultMap())
            }
            (spinner.adapter as CustomSpinnerAdapter).setData(mapList)
            spinner.onItemSelectedListener?.onItemSelected(
                spinner,
                spinner.selectedView,
                0,
                spinner.selectedItemId
            )
        }
    }

    private fun createDefaultMap(): Map<String, Any> {
        return hashMapOf(
            DefinedParams.NAME to getString(R.string.please_select),
            DefinedParams.ID to DefaultID
        )
    }

    fun resetSingleSelection(id: String) {
        getViewByTag(id)?.let {
            if (it is ViewGroup) {
                it.forEach { view ->
                    if (view is TextView) {
                        view.isSelected = false
                    }
                }
            }
        }
    }

    fun handlePregnancyCardBasedOnAge() {
        val dateOfBirthView =
            getViewByTag(dateOfBirth) as? AppCompatTextView ?: return
        val dateOfBirth = dateOfBirthView.text?.toString()?.trim() ?: return

        if (DateUtils.calculateAge(
                dateOfBirth,
                DATE_ddMMyyyy
            ) !in PREGNANCY_MIN_AGE..PREGNANCY_MAX_AGE
        ) {
            handleAgeBelowThreshold()
        } else {
            handleAgeAboveThreshold()
        }
    }
    fun handlePregnancyCardBasedOnAgeAndWeeks() {
        val dateOfBirthView =
            getViewByTag(MemberRegistration.dateOfBirth) as? AppCompatTextView ?: return
        val dateOfBirth = dateOfBirthView.text?.toString()?.trim()
        if (!dateOfBirth.isNullOrEmpty()) {
            val ageAndWeek = DateUtils.getV2YearMonthAndWeek(dateOfBirth, DATE_ddMMyyyy)
            val ageYears = ageAndWeek.years
            val ageMonths = ageAndWeek.months
            val ageWeeks = ageAndWeek.weeks
            val ageDays = ageAndWeek.days

            if ((ageYears !in PREGNANCY_MIN_AGE..PREGNANCY_MAX_AGE) || (ageYears == PREGNANCY_MAX_AGE && (ageMonths + ageWeeks + ageDays) != 0)) {
                handleAgeBelowThreshold()
            } else {
                handleAgeAboveThreshold()
            }
        } else {
            handleAgeBelowThreshold()
        }
    }

    private fun handleAgeBelowThreshold() {
        if (isResultAvailable(gender, female)) {
            val isPregnantRootView =
                getViewByTag(MemberRegistration.isPregnant + rootSuffix) as? ViewGroup
                    ?: return
            removeIfContains(MemberRegistration.isPregnant)
            (getViewByTag(MemberRegistration.isPregnant) as? ViewGroup)?.forEach { view ->
                if (view is TextView) {
                    view.isSelected = false
                }
            }
            if (isPregnantRootView.isVisible()) {
                isPregnantRootView.gone()
            }
        }
    }

    private fun handleAgeAboveThreshold() {
        if (isResultAvailable(gender, female)) {
            val isPregnantRootView =
                getViewByTag(MemberRegistration.isPregnant + rootSuffix) as? ViewGroup
                    ?: return
            if (isPregnantRootView.isGone()) {
                isPregnantRootView.visible()
            }
        }
    }

    private fun checkOnlyAlphabets(
        actualValue: Any?,
        valid: Boolean,
        serverViewModel: FormLayout
    ): Boolean {
        var isValid = valid
        serverViewModel.apply {
            if (viewType == VIEW_TYPE_FORM_EDITTEXT && actualValue is String && actualValue.isNotBlank() && !CommonUtils.isAlphabetsWithSpace(
                    actualValue
                )
            ) {
                isValid = false
                requestFocusView(serverViewModel, getString(R.string.only_alphabets_validation))
            } else {
                hideValidationField(serverViewModel)
            }
        }
        return isValid
    }

    private fun onlyAlphabet(itemId: String, input: String): Boolean {
        val serverItem = serverData?.firstOrNull { it.id == itemId }
        return serverItem?.let { it.onlyAlphabets == true && CommonUtils.isAlphabetsWithSpace(input) }
            ?: true
    }

    fun checkValidMentalHealth(formLayout: FormLayout, id: String): Boolean {
        val totalQuestions = mentalHealthQuestions?.get(id)?.size
        formLayout.optionsList?.forEach { option ->
            val isMandatory =
                (option[Screening.isMandatory] as Boolean?) ?: false
            if (isMandatory) {
                if (resultHashMap.containsKey(id)) {
                    val map = resultHashMap[id] as HashMap<String, String>
                    val question = (option[DefinedParams.NAME] as String?) ?: ""
                    if (!map.containsKey(question)) {
                        return false
                    }
                } else {
                    return false
                }
            }
        }
        if (mentalHealthQuestions == null || (mentalHealthQuestions?.containsKey(id) == false))
            listener.loadLocalCache(id, localDataCache = Screening.Fetch_MH_Questions)
        mentalHealthQuestions(id)?.let {
            return it
        }
        return (resultHashMap.containsKey(id) && totalQuestions != null && ((resultHashMap[id] as HashMap<String, String>).size == totalQuestions))
    }

    private fun mentalHealthQuestions(id: String): Boolean? {
        mentalHealthQuestions?.get(id)?.forEach { option ->
            val isMandatory =
                (option.map[Screening.Mandatory] as Boolean?) ?: false
            if (isMandatory) {
                if (resultHashMap.containsKey(id)) {
                    val map = resultHashMap[id] as HashMap<String, String>
                    val question = (option.map[Screening.Questions] as String?) ?: ""
                    if (!map.containsKey(question)) {
                        return false
                    }
                } else {
                    return false
                }
            }
        }
        return null
    }

    fun fetchMHQuestions(id: String, questions: LocalSpinnerResponse?) {
        questions?.let {
            questions.response as MentalHealthEntity
            val map = questions.response.formInput?.let {
                StringConverter.convertStringToListOfMap(it)
            }
            map?.let { list ->
                saveMentalHealthQuestions(id, questions = getUIModel(list))
            }
        }
    }

    fun loadMentalHealthQuestions(
        questions: LocalSpinnerResponse?,
        mentalHealthEditList: java.util.ArrayList<Map<String, Any>>? = null,
        isViewOnly: Boolean = false
    ) {
        val recyclerView = questions?.tag?.let { getViewByTag(it) } ?: return
        if (recyclerView.visibility == View.VISIBLE) {
            this.mentalHealthEditList = mentalHealthEditList?.let {
                java.util.ArrayList(mentalHealthEditList)
            }
            questions?.let { response ->
                questions.response as MentalHealthEntity
                val map = questions.response.formInput?.let {
                    StringConverter.convertStringToListOfMap(it)
                }
                map?.let { list ->
                    val model = getUIModel(list)
                    saveMentalHealthQuestions(response.tag, questions = model)
                    recyclerView.let {
                        if (it is RecyclerView) {
                            it.adapter = MentalHealthAdapter(
                                context,
                                model,
                                response.tag,
                                isViewOnly = isViewOnly,
                                editList = this.mentalHealthEditList,
                                resultMap = resultHashMap,
                                translate = translate
                            ) { id, question, result, isUnselect, isClicked ->
                                processMentalHealthResult(
                                    id,
                                    question,
                                    result,
                                    isUnselect,
                                    isClicked
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun processMentalHealthResult(
        id: String,
        question: String,
        result: HashMap<String, Any>,
        isUnselect: Boolean,
        isClicked: Boolean
    ) {
        if (resultHashMap.containsKey(id)) {
            updateResultMap(resultHashMap, result, question, isUnselect, id, isClicked)
        } else {
            val map = HashMap<String, Map<String, Any>>()
            map[question] = result
            resultHashMap[id] = map
        }
    }

    private fun updateResultMap(
        resultHashMap: HashMap<String, Any>,
        result: HashMap<String, Any>,
        question: String,
        isUnselect: Boolean,
        id: String,
        isClicked: Boolean
    ) {
        val mhResultMap = resultHashMap[id] as? HashMap<String, Any>
        mhResultMap?.let {
            if (isUnselect && areAnswersSame(mhResultMap, result, question) && isClicked) {
                if (resultHashMap.containsKey(id) && mhResultMap.size == 1) {
                    resultHashMap.remove(id)
                } else {
                    mhResultMap.remove(question)
                }
            } else {
                mhResultMap[question] = result
            }
        }
    }

    private fun areAnswersSame(
        mhResultMap: HashMap<String, Any>,
        selectedAnswerMap: HashMap<String, Any>,
        question: String
    ): Boolean {
        if (mhResultMap.containsKey(question)) {
            val questionResultHashMap = mhResultMap[question] as HashMap<String, Any>
            val mhMapQuestionId = questionResultHashMap[Screening.Question_Id]
            val mhMapAnswerId = questionResultHashMap[Screening.Answer_Id]
            val answerMapQuestionId = selectedAnswerMap[Screening.Question_Id]
            val answerMapAnswerId = selectedAnswerMap[Screening.Answer_Id]
            return mhMapQuestionId == answerMapQuestionId && mhMapAnswerId == answerMapAnswerId
        }
        return false
    }

    private fun getUIModel(optionList: ArrayList<Map<String, Any>>): ArrayList<MentalHealthOption> {
        val optionListUI = ArrayList<MentalHealthOption>()
        optionList.forEach {
            optionListUI.add(MentalHealthOption(map = it))
        }
        return optionListUI
    }

    private fun saveMentalHealthQuestions(id: String, questions: java.util.ArrayList<MentalHealthOption>) {
        if (mentalHealthQuestions == null)
            mentalHealthQuestions = HashMap()
        mentalHealthQuestions?.put(id, questions)
    }

    fun checkIfNoSymptomsPresent(diabetes: Any?): Boolean {
        var status = false
        if ((diabetes is java.util.ArrayList<*>) && diabetes.size > 0) {
            if (diabetes.size == 1) {
                diabetes[0]?.let { map ->
                    if (map is Map<*, *>) {
                        status = map[DefinedParams.NAME] != Screening.NoSymptoms
                    }
                }
            } else {
                status = true
            }
        }
        return status
    }

    fun showHideCardFamily(status: Boolean, family: String) {
        serverData?.let { serverDataList ->
            val familyList = serverDataList.filter { it.id == family }
            familyList.forEach { formLayout ->
                val view = getViewByTag(formLayout.id + rootSuffix)
                view?.let { familyView ->
                    if (status) {
                        setViewVisibility(VISIBLE, familyView, true)
                    } else {
                        setViewVisibility(GONE, familyView, true)
                    }
                }
            }
        }
    }

    fun showMHView(showView: Boolean, types: List<String>) {
        types.forEach { type ->
            val mhType = when (type) {
                AssessmentDefinedParams.PHQ9 -> Pair(
                    AssessmentDefinedParams.PHQ9.lowercase(),
                    AssessmentDefinedParams.PHQ9_Mental_Health
                )

                AssessmentDefinedParams.GAD7 -> Pair(
                    AssessmentDefinedParams.GAD7.lowercase(),
                    AssessmentDefinedParams.GAD7_Mental_Health
                )

                else -> Pair(Screening.PHQ4.lowercase(), Screening.MentalHealthDetails)
            }

            getViewByTag(mhType.first + rootSuffix)?.let {
                if (showView) {
                    it.visible()
                } else parentLayout.removeView(it)
            }
        }
    }

    fun populateEditableViews(
        serverData: List<FormLayout>, mentalHealthEditList: java.util.ArrayList<Map<String, Any>>? = null
    ) {
        this.serverData =
            serverData.filter { it.viewType != VIEW_TYPE_FORM_CARD_FAMILY && it.isEditable }
        this.mentalHealthEditList = mentalHealthEditList?.let {
            java.util.ArrayList(mentalHealthEditList)
        }
        EDITSCREEN = true
        parentLayout.removeAllViews()
        addEditableCards(serverData)
        this.serverData?.forEach { serverViewModel ->
            when (serverViewModel.viewType) {
                VIEW_TYPE_FORM_CARD_FAMILY -> createCardViewFamily(serverViewModel)
                VIEW_TYPE_FORM_EDITTEXT -> createEditText(serverViewModel)
                VIEW_TYPE_FORM_RADIOGROUP -> createRadioGroup(serverViewModel)
                VIEW_TYPE_SINGLE_SELECTION -> createSingleSelectionView(serverViewModel)
                VIEW_TYPE_FORM_SPINNER -> createCustomSpinner(serverViewModel)
                VIEW_TYPE_DIALOG_CHECKBOX -> createCheckboxDialogView(serverViewModel)
                VIEW_INFORMATION_LABEL -> createInformationLabel(serverViewModel)
                VIEW_TYPE_INSTRUCTION -> createInstructionView(serverViewModel)
                VIEW_TYPE_FORM_TEXTLABEL -> createTextLabel(serverViewModel)
                VIEW_TYPE_METAL_HEALTH -> createMentalHealthView(serverViewModel)
                VIEW_TYPE_FORM_AGE -> createAgeView(serverViewModel)
                VIEW_TYPE_NO_OF_DAYS -> createNoOfDaysView(serverViewModel)
                VIEW_TYPE_FORM_DATEPICKER -> createDatePicker(serverViewModel)
                VIEW_TYPE_FORM_BP -> createBPView(serverViewModel)
                VIEW_TYPE_TIME -> createTimeView(serverViewModel)
            }
        }
    }

    private fun addEditableCards(serverData: List<FormLayout>) {
        serverData.forEach { data ->
            if (data.isEditable) {
                val list = serverData.find { it.id == data.family }
                if (list != null) {
                    createCardViewFamily(list)
                }
            }
        }
    }

    fun isViewGone(tag: Any): Boolean {
        return getViewByTag(tag)?.visibility == View.GONE
    }

}