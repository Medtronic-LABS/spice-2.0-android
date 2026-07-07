package org.medtroniclabs.uhis.formgeneration

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.content.ContextWrapper
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.Point
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.RelativeSizeSpan
import android.view.LayoutInflater
import android.view.MotionEvent
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
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatSpinner
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.children
import androidx.core.view.forEach
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.medtroniclabs.uhis.BuildConfig
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.invisible
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.AgeOrDobDisplay
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.CommonUtils.displayAge
import org.medtroniclabs.uhis.common.CommonUtils.getMaxDateLimit
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ
import org.medtroniclabs.uhis.common.DateUtils.DATE_ddMMyyyy
import org.medtroniclabs.uhis.common.DateUtils.convertDateFormat
import org.medtroniclabs.uhis.common.DefinedParams.BOLD
import org.medtroniclabs.uhis.common.DefinedParams.BOLD_ITALIC
import org.medtroniclabs.uhis.common.DefinedParams.DEFAULT_ID
import org.medtroniclabs.uhis.common.DefinedParams.GENDER_FEMALE
import org.medtroniclabs.uhis.common.DefinedParams.ITALIC
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.common.StringConverter
import org.medtroniclabs.uhis.common.Validator
import org.medtroniclabs.uhis.data.LocalSpinnerResponse
import org.medtroniclabs.uhis.databinding.AgeDobLayoutBinding
import org.medtroniclabs.uhis.databinding.AgeOrDobLayoutBinding
import org.medtroniclabs.uhis.databinding.AgeYmdDobLayoutBinding
import org.medtroniclabs.uhis.databinding.BpReadingLayoutBinding
import org.medtroniclabs.uhis.databinding.CardLayoutBinding
import org.medtroniclabs.uhis.databinding.CheckboxDialogSpinnerLayoutBinding
import org.medtroniclabs.uhis.databinding.CustomSpinnerBinding
import org.medtroniclabs.uhis.databinding.DatepickerLayoutBinding
import org.medtroniclabs.uhis.databinding.EdittextAreaLayoutBinding
import org.medtroniclabs.uhis.databinding.EdittextLayoutBinding
import org.medtroniclabs.uhis.databinding.FormQrScanViewBinding
import org.medtroniclabs.uhis.databinding.InstructionLayoutBinding
import org.medtroniclabs.uhis.databinding.LayoutInformationLabelBinding
import org.medtroniclabs.uhis.databinding.LayoutSingleSelectionBinding
import org.medtroniclabs.uhis.databinding.MentalHealthLayoutBinding
import org.medtroniclabs.uhis.databinding.NoOfDaysLayoutBinding
import org.medtroniclabs.uhis.databinding.RadioGroupLayoutBinding
import org.medtroniclabs.uhis.databinding.TextLabelLayoutBinding
import org.medtroniclabs.uhis.databinding.TimeViewLayoutBinding
import org.medtroniclabs.uhis.db.entity.MentalHealthEntity
import org.medtroniclabs.uhis.formgeneration.FormSupport.getSpannableString
import org.medtroniclabs.uhis.formgeneration.FormSupport.isTranslatedOrNot
import org.medtroniclabs.uhis.formgeneration.FormSupport.translateTitle
import org.medtroniclabs.uhis.formgeneration.FormSupport.updateTitle
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams.GONE
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams.INVISIBLE
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams.MONTH_KEY
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams.OTHER_METHOD_SPECIFY
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams.SSP16
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams.VALUE
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams.VISIBLE
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams.WEEK_KEY
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams.YEAR_KEY
import org.medtroniclabs.uhis.formgeneration.config.EditTextOptionType
import org.medtroniclabs.uhis.formgeneration.config.ViewType.VIEW_INFORMATION_LABEL
import org.medtroniclabs.uhis.formgeneration.config.ViewType.VIEW_TYPE_DIALOG_CHECKBOX
import org.medtroniclabs.uhis.formgeneration.config.ViewType.VIEW_TYPE_FORM_AGE
import org.medtroniclabs.uhis.formgeneration.config.ViewType.VIEW_TYPE_FORM_AGE_OR_DOB
import org.medtroniclabs.uhis.formgeneration.config.ViewType.VIEW_TYPE_FORM_AGE_YMD
import org.medtroniclabs.uhis.formgeneration.config.ViewType.VIEW_TYPE_FORM_BP
import org.medtroniclabs.uhis.formgeneration.config.ViewType.VIEW_TYPE_FORM_CARD_FAMILY
import org.medtroniclabs.uhis.formgeneration.config.ViewType.VIEW_TYPE_FORM_DATEPICKER
import org.medtroniclabs.uhis.formgeneration.config.ViewType.VIEW_TYPE_FORM_EDITTEXT
import org.medtroniclabs.uhis.formgeneration.config.ViewType.VIEW_TYPE_FORM_EDITTEXT_AREA
import org.medtroniclabs.uhis.formgeneration.config.ViewType.VIEW_TYPE_FORM_MULTISELECT_DATEPICKER
import org.medtroniclabs.uhis.formgeneration.config.ViewType.VIEW_TYPE_FORM_MULTI_SELECT_SPINNER
import org.medtroniclabs.uhis.formgeneration.config.ViewType.VIEW_TYPE_FORM_QR
import org.medtroniclabs.uhis.formgeneration.config.ViewType.VIEW_TYPE_FORM_RADIOGROUP
import org.medtroniclabs.uhis.formgeneration.config.ViewType.VIEW_TYPE_FORM_SPINNER
import org.medtroniclabs.uhis.formgeneration.config.ViewType.VIEW_TYPE_FORM_TEXTLABEL
import org.medtroniclabs.uhis.formgeneration.config.ViewType.VIEW_TYPE_INSTRUCTION
import org.medtroniclabs.uhis.formgeneration.config.ViewType.VIEW_TYPE_MENTAL_HEALTH
import org.medtroniclabs.uhis.formgeneration.config.ViewType.VIEW_TYPE_NO_OF_DAYS
import org.medtroniclabs.uhis.formgeneration.config.ViewType.VIEW_TYPE_SINGLE_SELECTION
import org.medtroniclabs.uhis.formgeneration.config.ViewType.VIEW_TYPE_TIME
import org.medtroniclabs.uhis.formgeneration.extension.DecimalDigitsInputFilter
import org.medtroniclabs.uhis.formgeneration.extension.dp
import org.medtroniclabs.uhis.formgeneration.extension.hideKeyboard
import org.medtroniclabs.uhis.formgeneration.extension.markMandatory
import org.medtroniclabs.uhis.formgeneration.extension.markNonMandatory
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.extension.textSizeSsp
import org.medtroniclabs.uhis.formgeneration.listener.FormEventListener
import org.medtroniclabs.uhis.formgeneration.model.BPModel
import org.medtroniclabs.uhis.formgeneration.model.ConditionModelConfig
import org.medtroniclabs.uhis.formgeneration.model.ConditionalModel
import org.medtroniclabs.uhis.formgeneration.model.FormLayout
import org.medtroniclabs.uhis.formgeneration.model.MentalHealthOption
import org.medtroniclabs.uhis.formgeneration.ui.MultiSelectDatePickerDialog
import org.medtroniclabs.uhis.formgeneration.ui.MultiSelectSpinnerAdapter
import org.medtroniclabs.uhis.formgeneration.ui.SingleSelectionCustomView
import org.medtroniclabs.uhis.formgeneration.utility.CustomSpinnerAdapter
import org.medtroniclabs.uhis.formgeneration.utility.CustomSpinnerAdapterCustomLayout
import org.medtroniclabs.uhis.formgeneration.utility.DecimalInputFilter
import org.medtroniclabs.uhis.formgeneration.utility.DigitsInputFilter
import org.medtroniclabs.uhis.formgeneration.utility.FormFieldValidator
import org.medtroniclabs.uhis.formgeneration.utility.PersonNameFilter
import org.medtroniclabs.uhis.mappingkey.CommunityDetails
import org.medtroniclabs.uhis.mappingkey.MemberRegistration
import org.medtroniclabs.uhis.mappingkey.MemberRegistration.DATE_OF_BIRTH
import org.medtroniclabs.uhis.mappingkey.MemberRegistration.PHONE_NUMBER
import org.medtroniclabs.uhis.mappingkey.RxBuddy
import org.medtroniclabs.uhis.mappingkey.Screening
import org.medtroniclabs.uhis.mappingkey.Screening.DateOfBirth
import org.medtroniclabs.uhis.mappingkey.Screening.Hour
import org.medtroniclabs.uhis.mappingkey.Screening.Minute
import org.medtroniclabs.uhis.mappingkey.Screening.TODAY
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.CAMP_DATE
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.MUAC
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.muacCode
import timber.log.Timber
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FormGenerator(
    var context: Context,
    private val parentLayout: LinearLayout,
    private val listener: FormEventListener,
    var scrollView: NestedScrollView? = null,
    val translate: Boolean = false,
    private val callback: ((HashMap<String, Any>, String) -> Unit)? = null,
) : ContextWrapper(context) {
    private var serverData: List<FormLayout>? = null
    val rootSuffix = AssessmentDefinedParams.rootSuffix
    val titleSuffix = AssessmentDefinedParams.TITLE_SUFFIX
    private val errorSuffix = AssessmentDefinedParams.errorSuffix
    private val resultHashMap = HashMap<String, Any>()
    private val tvKey = AssessmentDefinedParams.summaryKey
    private val tvValue = AssessmentDefinedParams.SUMMARY_VALUE
    private val rootSummary = AssessmentDefinedParams.SUMMARY_ROOT

    private val scanDoneButtonSuffix = "scanDone"
    private val scanImageView = "scanImageView"

    private val innerRootSuffix = "innerRootView"
    private var editScreen: Boolean? = null
    private var focusNeeded: View? = null
    private val infoSuffix = DefinedParams.INFORMATION
    private val infoSuffixText = AssessmentDefinedParams.infoSuffixText
    private val generateNationalIdSuffix = "generateNationalId"
    private val diastolicSuffix = "DiastolicSuffix"
    private val systolicSuffix = "SystolicSuffix"
    private val pulseSuffix = "PulseSuffix"
    val lastMealTypeMeridiem = Screening.lastMealTypeMeridiem
    val lastMealTypeDateSuffix = Screening.lastMealTypeDateSuffix
    private var mentalHealthQuestions: HashMap<String, ArrayList<MentalHealthOption>>? = null
    private var mentalHealthEditList: ArrayList<Map<String, Any>>? = null

    companion object {
        private const val TAG_VALIDATE = "FormValidation"
    }

    private fun logValidationFailure(
        formLayout: FormLayout,
        message: String?,
    ) {
        if (!BuildConfig.DEBUG) return
        val id = formLayout.id
        val current = resultHashMap[id]
        val valueStr =
            when (current) {
                null -> "<no value in map>"
                is String ->
                    if (current.isEmpty()) {
                        "\"\" (empty string)"
                    } else {
                        val t = current.take(100)
                        "\"$t\"${if (current.length > 100) "…" else ""}"
                    }

                else -> "${current.javaClass.simpleName}: ${current.toString().take(120)}"
            }
        val reason = message ?: formLayout.errorMessage ?: formLayout.cultureErrorMessage ?: "validation failed"
        Timber.tag(TAG_VALIDATE).w(
            "Invalid field id=\"$id\" title=\"${formLayout.title}\" viewType=${formLayout.viewType} " +
                "mandatory=${formLayout.isMandatory} visible=${isViewVisible(id)} enabled=${isViewEnabled(id)} " +
                "value=$valueStr reason=$reason",
        )
    }

    fun populateViews(serverData: List<FormLayout>) {
        this.serverData = serverData
        parentLayout.removeAllViews()
        serverData.forEach { formLayout ->
            when (formLayout.viewType) {
                VIEW_TYPE_FORM_CARD_FAMILY -> createCardViewFamily(formLayout)
                VIEW_TYPE_FORM_EDITTEXT -> createEditText(formLayout)
                VIEW_TYPE_FORM_EDITTEXT_AREA -> createEditTextArea(formLayout)
                VIEW_TYPE_FORM_RADIOGROUP -> createRadioGroup(formLayout)
                VIEW_TYPE_SINGLE_SELECTION -> createSingleSelectionView(formLayout)
                VIEW_TYPE_FORM_SPINNER -> createCustomSpinner(formLayout)
                VIEW_TYPE_FORM_MULTI_SELECT_SPINNER -> createMultiSelectSpinner(formLayout)
                VIEW_TYPE_DIALOG_CHECKBOX -> createCheckboxDialogView(formLayout)
                VIEW_INFORMATION_LABEL -> createInformationLabel(formLayout)
                VIEW_TYPE_INSTRUCTION -> createInstructionView(formLayout)
                VIEW_TYPE_FORM_TEXTLABEL -> createTextLabel(formLayout)
                VIEW_TYPE_MENTAL_HEALTH -> createMentalHealthView(formLayout)
                VIEW_TYPE_FORM_AGE -> createAgeView(formLayout)
                VIEW_TYPE_FORM_AGE_YMD -> createAgeYMDView(formLayout)
                VIEW_TYPE_FORM_AGE_OR_DOB -> createAgeOrDobView(formLayout)
                VIEW_TYPE_NO_OF_DAYS -> createNoOfDaysView(formLayout)
                VIEW_TYPE_FORM_DATEPICKER -> createDatePicker(formLayout)
                VIEW_TYPE_FORM_BP -> createBPView(formLayout)
                VIEW_TYPE_TIME -> createTimeView(formLayout)
                VIEW_TYPE_FORM_QR -> addScannerView(formLayout)
                VIEW_TYPE_FORM_MULTISELECT_DATEPICKER -> createMultiSelectDatePicker(formLayout)
            }
        }
        listener.onRenderingComplete()
    }

    @SuppressLint("SetTextI18n")
    private fun createMultiSelectDatePicker(formLayout: FormLayout) {
        val binding = DatepickerLayoutBinding.inflate(LayoutInflater.from(context))
        formLayout.apply {
            binding.root.tag = id + rootSuffix
            binding.etUserInput.tag = id
            hint?.let {
                binding.etUserInput.hint = if (translate) hintCulture ?: it else it
            }
            binding.tvErrorMessage.tag = id + errorSuffix
            binding.tvTitle.text = if (translate) titleCulture ?: title else title
            binding.tvTitle.tag = id + titleSuffix

            binding.etUserInputHolder.safeClickListener {
                val dates = if (resultHashMap.containsKey(id)) resultHashMap[id] as List<Long> else listOf()

                // Open MultiSelectDatePickerDialog with previously selected dates
                MultiSelectDatePickerDialog(
                    context = context,
                    initialSelectedDates = dates,
                    onDateSelected = { selectedDates ->
                        val displayString = if (selectedDates.size > 1) "Days selected" else "Day selected"
                        binding.etUserInput.text = "${selectedDates.size} $displayString"

                        // Update resultHashMap
                        resultHashMap[id] = selectedDates

                        // Trigger callback
                        callback?.invoke(resultHashMap, id)
                    },
                ).show()
            }

            if (isMandatory) {
                binding.tvTitle.markMandatory()
            }

            getFamilyView(family)?.addView(binding.root) ?: parentLayout.addView(binding.root)

            (binding.etUserInput.background as? GradientDrawable)?.apply {
                setStroke(
                    resources.getDimensionPixelSize(R.dimen._1sdp),
                    context.getColor(R.color.edittext_stroke),
                )
            }
            setViewVisibility(visibility, binding.root)
            setViewEnableDisable(isEnabled, binding.root)
        }
    }

    private fun createCardViewFamily(formLayout: FormLayout) {
        val binding = CardLayoutBinding.inflate(LayoutInflater.from(context))
        formLayout.apply {
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

    private fun createNoOfDaysView(formLayout: FormLayout) {
        val binding = NoOfDaysLayoutBinding.inflate(LayoutInflater.from(context))
        formLayout.apply {
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
                binding.ivInfo.text = getInfoTitle(translate, getString(R.string.job_aid))
                binding.ivInfo.visibility = getVisibility(it)
            }

            binding.ivInfo.setOnClickListener {
                listener.onInstructionClicked(
                    id,
                    title,
                    dosageListModel = formLayout.dosageListItems,
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
                        if (resultHashMap.containsKey(id)) {
                            resultHashMap.remove(id)
                        }
                    }
                }
            }
            (binding.etUserInput.background as? GradientDrawable)?.apply {
                setStroke(resources.getDimensionPixelSize(R.dimen._1sdp), context.getColor(R.color.edittext_stroke))
            }
            setViewVisibility(visibility, binding.root)
            setViewEnableDisable(isEnabled, binding.root)
            getFamilyView(family)?.addView(binding.root) ?: run {
                parentLayout.addView(binding.root)
            }
        }
    }

    private fun getVisibility(visibility: String): Int =
        when (visibility) {
            VISIBLE -> View.VISIBLE
            INVISIBLE -> View.INVISIBLE
            else -> View.GONE
        }

    private fun createEditText(formLayout: FormLayout) {
        val binding = EdittextLayoutBinding.inflate(LayoutInflater.from(context))
        formLayout.apply {
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

            if (isPhoneNumberField(formLayout.id)) {
                if (optionType == EditTextOptionType.PHONE_NUMBER_WITHOUT_COUNTRY_CODE) {
                    binding.llCountryCode.gone()
                } else {
                    SecuredPreference.getPhoneNumberCode()?.let { phoneNumberCode ->
                        binding.llCountryCode.visibility = View.VISIBLE
                        binding.tvCountryCode.text = if (phoneNumberCode.startsWith("+")) {
                            phoneNumberCode
                        } else {
                            "+$phoneNumberCode"
                        }
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
            unitMeasurement?.let {
                it.also { resultHashMap[id + Screening.unitMeasurement_KEY] = it }
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

            if (applyDecimalFilter == true) {
                inputFilter.add(DigitsInputFilter())
            }

            if (id == Screening.identityValue) {
                inputFilter.add(InputFilter.AllCaps())
            }

            if (applyTwoDigitPrecision == true) {
                inputFilter.add(DecimalInputFilter())
            }

            if (optionType == EditTextOptionType.PERSON_NAME) {
                inputFilter.add(PersonNameFilter())
            }

            if (inputFilter.isNotEmpty()) {
                try {
                    binding.etUserInput.filters = inputFilter.toTypedArray()
                } catch (_: Exception) {
                    // Exception - Catch block
                }
            }

            isInfo?.let {
                binding.tvInfo.text = getInfoTitle(translate, null)
                binding.tvInfo.visibility = getVisibility(it)
            }

            inputType?.let {
                when (it) {
                    InputType.TYPE_CLASS_PHONE, InputType.TYPE_CLASS_NUMBER ->
                        binding.etUserInput.inputType =
                            InputType.TYPE_CLASS_NUMBER

                    InputType.TYPE_NUMBER_FLAG_DECIMAL ->
                        binding.etUserInput.inputType =
                            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL

                    InputType.TYPE_TEXT_VARIATION_PERSON_NAME ->
                        binding.etUserInput.inputType =
                            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS or InputType.TYPE_TEXT_VARIATION_PERSON_NAME

                    else -> {
                        binding.etUserInput.inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                    }
                }
            }

            getFamilyView(family)?.addView(binding.root) ?: run {
                parentLayout.addView(binding.root)
            }

            binding.etUserInput.addTextChangedListener { editable: Editable? ->
                when {
                    editable.isNullOrBlank() -> {
                        if (editScreen == true) {
                            if ((
                                    inputType != null &&
                                        (
                                            inputType == InputType.TYPE_CLASS_NUMBER ||
                                                inputType == InputType.TYPE_NUMBER_FLAG_DECIMAL
                                        )
                                )
                            ) {
                                resultHashMap.remove(id)
                            } else {
                                resultHashMap[id] = ""
                            }
                        } else {
                            resultHashMap.remove(id)
                        }
                        setConditionalVisibility(formLayout, null)
                        resultHashMap.remove(id + Screening.unitMeasurement_KEY)
                        binding.tvErrorMessage.gone()
                    }

                    else -> {
                        if ((
                                inputType != null &&
                                    (
                                        inputType == InputType.TYPE_CLASS_NUMBER ||
                                            inputType == InputType.TYPE_NUMBER_FLAG_DECIMAL
                                    )
                            )
                        ) {
                            val resultValue = editable.trim().toString().toDoubleOrNull()
                            resultValue?.let {
                                resultHashMap[id] = resultValue
                            }
                        } else {
                            resultHashMap[id] = editable.trim().toString()
                        }
                        setConditionalVisibility(formLayout, editable.trim().toString())
                        unitMeasurement?.let {
                            it.also { resultHashMap[id + Screening.unitMeasurement_KEY] = it }
                        }
                        validateInputs(formLayout.id)
                    }
                }
                callback?.invoke(resultHashMap, id)
            }
            (binding.etUserInput.background as? GradientDrawable)?.apply {
                setStroke(resources.getDimensionPixelSize(R.dimen._1sdp), context.getColor(R.color.edittext_stroke))
            }
            setViewVisibility(visibility, binding.root)
            setViewEnableDisable(isEnabled, binding.root)
        }
    }

    private fun generateNationalId(viewId: String) {
        var nationalId = ""
        var errorVisibility = View.GONE

        // Full Name
        val fullName = firstName(Screening.FULL_NAME)
        fullName.first?.let {
            nationalId = it
        }
        focusNeeded = null
        fullNameError(nationalId, Screening.FULL_NAME, fullName.second)

        // Phone number
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
            getViewByTag(viewId)?.let { editText ->
                if (editText is AppCompatEditText) {
                    editText.setText(nationalId.uppercase())
                    editText.isEnabled = false
                }
                getViewByTag("${viewId}$errorSuffix")?.let { error ->
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
                if (errorVisibility == View.VISIBLE && focusNeeded == null) {
                    focusNeeded =
                        getViewByTag(Screening.phoneNumber + titleSuffix) ?: this
                }
            }
        }
    }

    private fun fullNameError(
        nationalId: String,
        id: String,
        errorMessage: String,
    ) {
        getViewByTag(id + errorSuffix)?.let { tvError ->
            (tvError as TextView).apply {
                text = errorMessage
                if (nationalId.isEmpty()) {
                    visibility = View.VISIBLE
                    focusNeeded = getViewByTag(id + titleSuffix) ?: this
                } else {
                    visibility = View.GONE
                }
            }
        }
    }

    private fun lastNameError(
        errorVisibility: Int,
        errorMessage: String,
    ) {
        getViewByTag("${Screening.lastName}$errorSuffix")?.let { tvError ->
            (tvError as TextView).apply {
                text = errorMessage
                visibility = errorVisibility
                if (errorVisibility == View.VISIBLE && focusNeeded == null) {
                    focusNeeded =
                        getViewByTag("${Screening.lastName}$titleSuffix") ?: this
                }
            }
        }
    }

    private fun phoneNumber(nationalId: String): Pair<String?, Int?> {
        var nId = nationalId
        var errorVisibility: Int? = null
        try {
            getViewByTag(Screening.phoneNumber)?.let { editText ->
                if (editText is AppCompatEditText &&
                    (!editText.text.isNullOrBlank()) &&
                    FormFieldValidator.isValidMobileNumber(
                        editText.text.toString(),
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
                        } else {
                            "$nId$input"
                        }
                    }
                } else {
                    nId = ""
                    errorVisibility = View.VISIBLE
                }
            }
        } catch (_: Exception) {
            // Exception - Catch block
        }
        return Pair(nId, errorVisibility)
    }

    private fun validateNationalId(
        input: String,
        nId: String,
    ): String =
        if (input.length >= 4) {
            "$nId${
                input.substring(
                    0,
                    4,
                )
            }"
        } else {
            "$nId$input"
        }

    private fun lastName(nationalId: String): Triple<String?, Int?, String> {
        var nId = nationalId
        var errorVisibility: Int? = null
        var errorMessage: String = getString(R.string.error_label)
        getViewByTag(Screening.lastName)?.let { editText ->
            if (editText is AppCompatEditText &&
                (
                    (!editText.text.isNullOrBlank()) &&
                        checkMinLength(
                            Screening.lastName,
                            editText.text?.trim()?.length,
                        )
                )
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
        defaultError: String = getString(R.string.default_user_input_error),
    ): Pair<String?, String> {
        var nationalId: String? = null
        var errorMessage: String = defaultError
        getViewByTag(id)?.let { editText ->
            if (editText is AppCompatEditText &&
                (
                    (!editText.text.isNullOrBlank()) &&
                        checkMinLength(
                            id,
                            editText.text?.trim()?.length,
                        )
                )
            ) {
                val input = editText.text!!.trim().replace("\\s".toRegex(), "")
                if (onlyAlphabet(id, input)) {
                    val input = if (input.length >= 4) {
                        input.substring(
                            0,
                            4,
                        )
                    } else {
                        input
                    }
                    nationalId = to4DigitHash(input)
                } else {
                    errorMessage = getString(R.string.only_alphabets_validation)
                }
            }
        }
        return Pair(nationalId, errorMessage)
    }

    private fun to4DigitHash(input: String): String {
        val number = kotlin.math.abs(input.hashCode()) % 10000
        return number.toString().padStart(4, '0')
    }

    private fun checkMinLength(
        name: String,
        actualLength: Int?,
    ): Boolean {
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

    private fun createBPView(formLayout: FormLayout) {
        val binding = BpReadingLayoutBinding.inflate(LayoutInflater.from(context))
        formLayout.apply {
            binding.root.tag = id + rootSuffix
            binding.tvErrorMessage.tag = id + errorSuffix
            binding.tvInstructionBloodPressure.tag = id + titleSuffix
            "${0}-$diastolicSuffix".also { binding.etDiastolicOne.tag = it }
            "${0}-$systolicSuffix".also { binding.etSystolicOne.tag = it }
            "${0}-$pulseSuffix".also { binding.etPulseOne.tag = it }
            "${1}-$diastolicSuffix".also { binding.etDiastolicTwo.tag = it }
            "${1}-$systolicSuffix".also { binding.etSystolicTwo.tag = it }
            "${1}-$pulseSuffix".also { binding.etPulseTwo.tag = it }
            "${2}-$diastolicSuffix".also { binding.etDiastolicThree.tag = it }
            "${2}-$systolicSuffix".also { binding.etSystolicThree.tag = it }
            "${2}-$pulseSuffix".also { binding.etPulseThree.tag = it }
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
                if (translate) {
                    instructionsCulture?.let {
                        listener.onInstructionClicked(
                            id = id,
                            title = titleCulture ?: title,
                            informationList = it,
                            description = getString(R.string.bp_measure),
                        )
                    }
                } else {
                    instructions?.let {
                        listener.onInstructionClicked(
                            id = id,
                            title = title,
                            informationList = it,
                            description = getString(R.string.bp_measure),
                        )
                    }
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

            if (showPulse == false) {
                hidePulseColumn(binding)
            }

            getFamilyView(family)?.addView(binding.root) ?: run {
                parentLayout.addView(binding.root)
            }
            setViewVisibility(visibility, binding.root)
            // setViewEnableDisable(isEnabled, binding.root)
        }
    }

    /**
     * Hides the pulse column for a [VIEW_TYPE_FORM_BP] view when the form config disables it.
     * The BP layout is a weighted constraint chain, so setting the pulse header, spacers, and
     * pulse inputs to GONE lets the remaining columns redistribute and fill the row.
     */
    private fun hidePulseColumn(binding: BpReadingLayoutBinding) {
        binding.textView13.gone()
        binding.space02.gone()
        binding.spaceRow12.gone()
        binding.etPulseOne.gone()
        binding.spaceRow22.gone()
        binding.etPulseTwo.gone()
        binding.spaceRow33.gone()
        binding.etPulseThree.gone()
    }

    private fun checkInputsAndEnableNextField(
        text: CharSequence,
        binding: BpReadingLayoutBinding,
        list: ArrayList<BPModel>,
    ) {
        if (text == getString(R.string.sno_1)) {
            val systolicReadingOne = binding.etSystolicOne.text?.toString()
            val diastolicReadingOne = binding.etDiastolicOne.text?.toString()
            val pulseReadingOne = binding.etPulseOne.text?.toString()
            if (list.isNotEmpty()) {
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

    private fun createTimeView(formLayout: FormLayout) {
        val binding = TimeViewLayoutBinding.inflate(LayoutInflater.from(context))
        formLayout.apply {
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
                    formLayout,
                    singleSelectionCallbackForDate,
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
                    formLayout,
                    singleSelectionCallbackForTime,
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
            getFamilyView(family)?.addView(binding.root) ?: run {
                parentLayout.addView(binding.root)
            }
            setViewVisibility(visibility, binding.root, true)
            setViewEnableDisable(isEnabled, binding.root, true)
        }
    }

    private fun addScannerView(formLayout: FormLayout) {
        formLayout.apply {
            val binding = FormQrScanViewBinding.inflate(LayoutInflater.from(context))
            binding.root.tag = id + rootSuffix
            binding.tvTitle.tag = id + titleSuffix
            binding.tvErrorMessage.tag = id + errorSuffix
            binding.etQRCode.tag = id
            binding.ivScanDone.visibility = View.GONE
            binding.ivScanDone.tag = id + scanDoneButtonSuffix
            binding.ivScan.tag = id + scanImageView
            binding.tvTitle.text = CommonUtils.getTitle(formLayout, translate)
            binding.tvNewScan.visibility = View.GONE
            binding.tvNewScan.tag = id + innerRootSuffix
            if (isMandatory) {
                binding.tvTitle.markMandatory()
            }
            binding.ivScan.safeClickListener { listener.onQRScanRequested() }
            binding.tvTitle.safeClickListener { listener.onQRScanRequested() }
            binding.etQRCode.addTextChangedListener { editable ->
                resultHashMap[id] = editable.toString()
                binding.tvTitle.text = getString(R.string.qr_code_scanned)
                binding.ivScan.safeClickListener(null)
                binding.tvTitle.safeClickListener(null)
                binding.ivScanDone.visibility = View.VISIBLE
                binding.tvNewScan.visibility = View.VISIBLE
            }

            val clickableSpan = object : ClickableSpan() {
                override fun onClick(mView: View) {
                    listener.onQRScanRequested()
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = false
                }
            }
            binding.tvNewScan.text = CommonUtils.getSpannableString(
                clickableSpan,
                getString(R.string.scan_new_qr),
            )
            binding.tvNewScan.movementMethod = LinkMovementMethod.getInstance()

            getFamilyView(family)?.addView(binding.root) ?: kotlin.run {
                parentLayout.addView(binding.root)
            }
        }
    }

    private var singleSelectionCallbackForDate: ((selectedID: Any?, elementId: Pair<String, String?>, formLayout: FormLayout, name: String?) -> Unit)? =
        { selectedId, elementID, formLayout, _ ->
            saveSelectedOptionValue(elementID, selectedId, formLayout)
        }

    private var singleSelectionCallbackForTime: ((selectedID: Any?, elementId: Pair<String, String?>, formLayout: FormLayout, name: String?) -> Unit)? =
        { selectedId, elementID, formLayout, _ ->
            saveSelectedOptionValue(elementID, selectedId, formLayout)
        }

    private fun storeTimeValue(
        key: String,
        value: String?,
        id: String,
    ) {
        if (resultHashMap.containsKey(id)) {
            if (resultHashMap[id] is Map<*, *>) {
                if (!value.isNullOrBlank()) {
                    (resultHashMap[id] as HashMap<String, String>)[key] = value
                } else {
                    (resultHashMap[id] as HashMap<String, String>).remove(key)
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

    @SuppressLint("ClickableViewAccessibility")
    private fun createEditTextArea(formLayout: FormLayout) {
        val binding = EdittextAreaLayoutBinding.inflate(LayoutInflater.from(context))
        formLayout.apply {
            binding.root.tag = id + rootSuffix
            binding.tvTitle.tag = id + titleSuffix
            binding.etUserInput.tag = id
            binding.tvErrorMessage.tag = id + errorSuffix
            binding.tvNationalIdAction.visibility = View.GONE
            binding.tvKey.tag = id + tvKey
            binding.tvValue.tag = id + tvValue
            binding.bgLastMeal.tag = id + rootSummary
            binding.tvTitle.text = updateTitle(title, translate, titleCulture, unitMeasurement)

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

            if (applyDecimalFilter == true) {
                inputFilter.add(DigitsInputFilter())
            }

            if (id == DefinedParams.NATIONAL_ID) {
                inputFilter.add(InputFilter.AllCaps())
            }

            if (applyTwoDigitPrecision == true) {
                inputFilter.add(DecimalInputFilter())
            }

            if (inputFilter.isNotEmpty()) {
                try {
                    binding.etUserInput.filters = inputFilter.toTypedArray()
                } catch (_: Exception) {
                    // Exception - Catch block
                }
            }

            getFamilyView(family)?.addView(binding.root) ?: run {
                parentLayout.addView(binding.root)
            }

            binding.etUserInput.setOnTouchListener { v, event ->
                v.parent?.requestDisallowInterceptTouchEvent(true)
                when (event.action) {
                    MotionEvent.ACTION_UP -> v.parent?.requestDisallowInterceptTouchEvent(false)
                }
                false
            }

            binding.etUserInput.addTextChangedListener { editable: Editable? ->
                when {
                    editable.isNullOrBlank() -> {
                        if (editScreen == true) {
                            if ((
                                    inputType != null &&
                                        (
                                            inputType == InputType.TYPE_CLASS_NUMBER ||
                                                inputType == InputType.TYPE_NUMBER_FLAG_DECIMAL
                                        )
                                )
                            ) {
                                resultHashMap.remove(id)
                            } else {
                                resultHashMap[id] = ""
                            }
                        } else {
                            resultHashMap.remove(id)
                        }
                        setConditionalVisibility(formLayout, null)
                    }

                    else -> {
                        if ((
                                inputType != null &&
                                    (
                                        inputType == InputType.TYPE_CLASS_NUMBER ||
                                            inputType == InputType.TYPE_NUMBER_FLAG_DECIMAL
                                    )
                            )
                        ) {
                            val resultValue = editable.trim().toString().toDoubleOrNull()
                            resultValue?.let {
                                resultHashMap[id] = resultValue
                            }
                        } else {
                            resultHashMap[id] = editable.trim().toString()
                        }
                        setConditionalVisibility(formLayout, editable.trim().toString())

                        val textWatcher = object : TextWatcher {
                            override fun beforeTextChanged(
                                s: CharSequence?,
                                start: Int,
                                count: Int,
                                after: Int,
                            ) {
                            }

                            override fun onTextChanged(
                                s: CharSequence?,
                                start: Int,
                                before: Int,
                                count: Int,
                            ) {
                            }

                            override fun afterTextChanged(s: Editable?) {
                                s?.let {
                                    val inputText = it.toString()
                                    // Capitalize the first letter if it's lowercase
                                    if (inputText.isNotEmpty() && inputText[0].isLowerCase()) {
                                        binding.etUserInput.removeTextChangedListener(this) // Prevent infinite loop
                                        it.replace(
                                            0,
                                            1,
                                            inputText[0].uppercase(),
                                        ) // Capitalize first character
                                        binding.etUserInput.addTextChangedListener(this)
                                    }
                                }
                            }
                        }
                        binding.etUserInput.addTextChangedListener(textWatcher)
                    }
                }
            }
            setViewVisibility(visibility, binding.root)
            setViewEnableDisable(isEnabled, binding.root)
        }
    }

    private fun checkGenerateAction(
        formLayout: FormLayout,
        binding: EdittextLayoutBinding,
    ) {
        formLayout.apply {
            if (isNeedAction) {
                when (id) {
                    Screening.identityValue, Screening.nationalId -> {
                        binding.tvNationalIdAction.visibility = View.VISIBLE
                        val clickableSpan = object : ClickableSpan() {
                            override fun onClick(mView: View) {
                                // action click
                                generateNationalId(id)
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
                            text,
                            index,
                        )
                        binding.tvNationalIdAction.movementMethod = LinkMovementMethod.getInstance()
                    }
                }
            }
        }
    }

    private fun createRadioGroup(formLayout: FormLayout) {
        val binding = RadioGroupLayoutBinding.inflate(LayoutInflater.from(context))
        formLayout.apply {
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
                            intArrayOf(android.R.attr.state_checked),
                        ),
                        intArrayOf(
                            ContextCompat.getColor(
                                context,
                                R.color.navy_blue_20_alpha,
                            ), // disabled
                            ContextCompat.getColor(context, R.color.purple), // disabled
                            ContextCompat.getColor(context, R.color.purple), // enabled
                        ),
                    )
                    val textColorStateList = ColorStateList(
                        arrayOf(
                            intArrayOf(-android.R.attr.state_enabled),
                            intArrayOf(-android.R.attr.state_checked),
                            intArrayOf(android.R.attr.state_checked),
                        ),
                        intArrayOf(
                            ContextCompat.getColor(
                                context,
                                R.color.navy_blue_20_alpha,
                            ), // disabled
                            ContextCompat.getColor(context, R.color.navy_blue), // enabled
                            ContextCompat.getColor(context, R.color.purple),
                        ),
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
                        1f,
                    )
                    radioButton.typeface = ResourcesCompat.getFont(context, R.font.inter_regular)
                    binding.rgGroup.addView(radioButton)
                }
            }
            binding.rgGroup.orientation = orientation ?: LinearLayout.HORIZONTAL

            if (isMandatory) {
                binding.tvTitle.markMandatory()
            }

            getFamilyView(family)?.addView(binding.root) ?: run {
                parentLayout.addView(binding.root)
            }

            binding.rgGroup.setOnCheckedChangeListener { _, checkedId ->
                checkRadioGroupId(checkedId, optionsList, id, formLayout)
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
        formLayout: FormLayout,
    ) {
        if (checkedId >= 0) {
            optionsList?.let {
                val map = it[checkedId]
                resultHashMap[id] = map[DefinedParams.ID] as Any
                setConditionalVisibility(
                    formLayout,
                    it[checkedId][DefinedParams.NAME] as String? ?: "",
                )
                callback?.invoke(resultHashMap, id)
            }
        }
    }

    private fun changeRadioGroupTypeFace(
        radioButtonId: Int,
        rootView: RadioGroup,
    ) {
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

    private fun setOptionVisibility(
        optionVisibility: Any?,
        radioButton: View,
    ) {
        if (optionVisibility is String) {
            setViewVisibility(optionVisibility, radioButton)
        }
    }

    private fun createSingleSelectionView(formLayout: FormLayout) {
        formLayout.apply {
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
                    formLayout,
                    singleSelectionCallback,
                )
                binding.selectionGroup.addView(view)
            }
            if (isMandatory) {
                binding.tvTitle.markMandatory()
            }

            isInfo?.let {
                binding.ivInfo.text = getInfoTitle(translate, getString(R.string.job_aid))
                binding.ivInfo.visibility = getVisibility(it)
                if (formLayout.dosageListItems.isNullOrEmpty()) {
                    // Hide arrow icon
                    binding.ivInfo.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        0,
                        0,
                        0,
                        0,
                    )
                } else {
                    binding.ivInfo.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        0,
                        0,
                        R.drawable.ic_job_aid,
                        0,
                    )
                    binding.ivInfo.setOnClickListener {
                        listener.onInstructionClicked(
                            id,
                            title,
                            dosageListModel = formLayout.dosageListItems,
                        )
                    }
                }
            }

            getFamilyView(family)?.addView(binding.root) ?: run {
                parentLayout.addView(binding.root)
            }
            setViewVisibility(visibility, binding.root)
            setViewEnableDisable(isEnabled, binding.root)
        }
    }

    private var singleSelectionCallback: ((selectedID: Any?, elementId: Pair<String, String?>, formLayout: FormLayout, name: String?) -> Unit)? =
        { selectedId, elementID, formLayout, _ ->
            saveSelectedOptionValue(elementID, selectedId, formLayout)
            if ((selectedId as? String).equals(GENDER_FEMALE, true)) {
                listener.onAgeCheckForPregnancy()
            }
            callback?.invoke(resultHashMap, elementID.first)
            listener.onUpdateInstruction(elementID.first, selectedId)
        }

    fun getResultMap(): HashMap<String, Any> = resultHashMap

    fun getServerData(): List<FormLayout>? = serverData

    fun setServerData(serverData: List<FormLayout>) {
        this.serverData = serverData
    }

    private fun saveSelectedOptionValue(
        id: Pair<String, String?>,
        idValue: Any?,
        formLayout: FormLayout,
    ) {
        idValue?.let {
            resultHashMap[id.first] = it
            setConditionalVisibility(
                formLayout,
                it as? String,
            )
        } ?: run {
            setConditionalVisibility(
                formLayout,
                null,
            )
        }
    }

    private fun createCustomSpinner(formLayout: FormLayout) {
        val binding = CustomSpinnerBinding.inflate(LayoutInflater.from(context))
        formLayout.apply {
            listener.handleMandatoryCondition(formLayout)
            binding.root.tag = id + rootSuffix
            binding.etUserInput.tag = id
            binding.tvTitle.tag = id + titleSuffix
            binding.tvErrorMessage.tag = id + errorSuffix
            binding.tvTitle.text = translateTitle(titleCulture, title, translate)
            (binding.etUserInput.background as? GradientDrawable)?.apply {
                setStroke(resources.getDimensionPixelSize(R.dimen._1sdp), context.getColor(R.color.edittext_stroke))
            }
            val dropDownList = ArrayList<Map<String, Any>>()
            dropDownList.add(
                hashMapOf<String, Any>(
                    DefinedParams.NAME to getString(R.string.please_select),
                    DefinedParams.ID to DEFAULT_ID,
                ),
            )
            if (isMandatory) {
                binding.tvTitle.markMandatory()
            }
            isInfo?.let {
                binding.ivInfo.text = getInfoTitle(translate, getString(R.string.job_aid))
                binding.ivInfo.visibility = getVisibility(it)
            }

            binding.ivInfo.setOnClickListener {
                listener.onInstructionClicked(
                    id,
                    title,
                    dosageListModel = formLayout.dosageListItems,
                )
            }
            if (id == muacCode || id == MUAC) {
                binding.etUserInput.background = ContextCompat.getDrawable(context, R.drawable.edittext_background)
                val adapter = CustomSpinnerAdapterCustomLayout(context, translate)
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
                            itemId: Long,
                        ) {
                            handleSelectedItem(
                                adapter.getData(position = pos),
                                id,
                                dependentID,
                                formLayout,
                            )
                        }

                        override fun onNothingSelected(p0: AdapterView<*>?) {
                            /**
                             * usage of this method is not required
                             */
                        }
                    }
            } else {
                binding.etUserInput.background = null
                val adapter = CustomSpinnerAdapter(context, translate)
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
                            itemId: Long,
                        ) {
                            handleSelectedItem(
                                adapter.getData(position = pos),
                                id,
                                dependentID,
                                formLayout,
                            )
                            callback?.invoke(resultHashMap, id)
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

            getFamilyView(family)?.addView(binding.root) ?: run {
                parentLayout.addView(binding.root)
            }
            setViewVisibility(visibility, binding.root)
            setViewEnableDisable(isEnabled, binding.root)
            disableSpinner?.let {
                if (it) {
                    binding.etUserInput.isEnabled = false
                }
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
        formLayout: FormLayout,
    ) {
        selectedItem?.let {
            val selectedId = it[DefinedParams.ID]
            val selectedName = it[DefinedParams.NAME]
            if (selectedId is String && selectedId == "-1") {
                if (resultHashMap.containsKey(id)) {
                    handleId(id)
                    dependentID?.let { deptId ->
                        resetDependantSpinnerView(deptId)
                    }
                } else {
                    if (editScreen == true) {
                        resultHashMap[id] = ""
                    }
                }
            } else {
                if (formLayout.spinnerAsObject) {
                    resultHashMap[id] = it
                } else {
                    resultHashMap[id] =
                        it[DefinedParams.ID] as Any
                }
                dependentID?.let { deptId ->
                    resetDependantSpinnerView(deptId)
                    listener.loadLocalCache(
                        deptId,
                        deptId,
                        it[DefinedParams.ID] as Long,
                    )
                }
            }

            listener.onUpdateInstruction(id, selectedId)
            selectedIdVisibility(selectedId, formLayout, selectedName)
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
        formLayout: FormLayout,
        selectedName: Any?,
    ) {
        if (selectedId is String) {
            setConditionalVisibility(formLayout, selectedId)
        } else if (selectedId is Long && selectedName is String) {
            setConditionalVisibility(formLayout, selectedName)
        }
    }

    private fun addDropDownList(
        list: ArrayList<Map<String, Any>>,
        dropDownList: ArrayList<Map<String, Any>>,
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
                    DefinedParams.ID to DEFAULT_ID,
                ),
            )
            (view.adapter as CustomSpinnerAdapter).setData(dropDownList)
            view.setSelection(0, true)
        }
    }

    private fun createCheckboxDialogView(formLayout: FormLayout) {
        val binding = CheckboxDialogSpinnerLayoutBinding.inflate(LayoutInflater.from(context))
        formLayout.apply {
            binding.root.tag = id + rootSuffix
            binding.etUserInput.tag = id
            binding.tvTitle.tag = id + titleSuffix
            binding.tvErrorMessage.tag = id + errorSuffix
            binding.tvTitle.text = translateTitle(titleCulture, title, translate)

            hint?.let {
                if (translate) {
                    binding.etUserInput.hint = hintCulture ?: it
                } else {
                    binding.etUserInput.hint = it
                }
            }

            if (isMandatory) {
                binding.tvTitle.markMandatory()
            }

            binding.etUserInput.safeClickListener {
                listener.onCheckBoxDialogueClicked(id, formLayout, resultHashMap[id])
            }
            getFamilyView(family)?.addView(binding.root) ?: run {
                parentLayout.addView(binding.root)
            }
            (binding.etUserInput.background as? GradientDrawable)?.apply {
                setStroke(resources.getDimensionPixelSize(R.dimen._1sdp), context.getColor(R.color.edittext_stroke))
            }
            setViewVisibility(visibility, binding.root)
            setViewEnableDisable(isEnabled, binding.root)
        }
    }

    private fun createInformationLabel(formLayout: FormLayout) {
        val binding = LayoutInformationLabelBinding.inflate(LayoutInflater.from(context))
        formLayout.apply {
            listener.handleMandatoryCondition(formLayout)
            binding.root.tag = id + rootSuffix
            binding.tvValue.tag = id
            binding.tvSubValue.tag = id + infoSuffixText
            binding.tvKey.tag = id + tvKey
            binding.tvValue.text = getString(R.string.hyphen_symbol)
            binding.tvKey.text = updateTitle(title, translate, titleCulture, unitMeasurement)
            backgroundColor?.let { color ->
                if (color.startsWith(getString(R.string.hash_symbol))) {
                    binding.llBase.setBackgroundColor(
                        color.toColorInt(),
                    )
                }
            }
            getFamilyView(family)?.addView(binding.root) ?: run {
                parentLayout.addView(binding.root)
            }
            setViewVisibility(visibility, binding.root)
            setViewEnableDisable(isEnabled, binding.root)
        }
    }

    private fun createInstructionView(formLayout: FormLayout) {
        formLayout.apply {
            var instructionsList = instructions
            if (translate && !instructionsCulture.isNullOrEmpty()) {
                instructionsList = instructionsCulture
            }

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
                        dosageListModel = formLayout.dosageListItems,
                    )
                } ?: run {
                    listener.onInstructionClicked(
                        id,
                        title,
                        dosageListModel = formLayout.dosageListItems,
                    )
                }
            }
            getFamilyView(family)?.addView(binding.root) ?: run {
                parentLayout.addView(binding.root)
            }
            setViewVisibility(visibility, binding.root)
            setViewEnableDisable(isEnabled, binding.root)
        }
    }

    private fun createTextLabel(formLayout: FormLayout) {
        val binding = TextLabelLayoutBinding.inflate(LayoutInflater.from(context))
        formLayout.apply {
            binding.root.tag = id + rootSuffix
            binding.tvTitle.tag = id
            if (translate) {
                binding.tvTitle.text = titleCulture ?: title
            } else {
                binding.tvTitle.text = title
            }
            if (displayAsterisk) {
                binding.tvTitle.markMandatory()
            } else {
                binding.tvTitle.markNonMandatory()
            }
            textLabelColor?.let {
                binding.tvTitle.setTextColor(it.toColorInt())
            }
            textLabelStyle?.let { style ->
                binding.tvTitle.apply {
                    when (style) {
                        BOLD -> setTypeface(null, Typeface.BOLD)
                        ITALIC -> setTypeface(null, Typeface.ITALIC)
                        BOLD_ITALIC -> setTypeface(null, Typeface.BOLD_ITALIC)
                        else -> setTypeface(null, Typeface.NORMAL)
                    }
                }
            }
            getFamilyView(family)?.addView(binding.root) ?: run {
                parentLayout.addView(binding.root)
            }
            setViewVisibility(visibility, binding.root)
            setViewEnableDisable(isEnabled, binding.root)
        }
    }

    private fun createMentalHealthView(formLayout: FormLayout) {
        val binding = MentalHealthLayoutBinding.inflate(LayoutInflater.from(context))
        formLayout.apply {
            binding.root.tag = id + rootSuffix
            binding.tvErrorMessage.tag = id + errorSuffix
            binding.rvMentalHealth.tag = id
            binding.rvMentalHealth.layoutManager = LinearLayoutManager(context)

            localDataCache?.let {
                listener.loadLocalCache(id, it)
            }
            getFamilyView(family)?.addView(binding.root) ?: run {
                parentLayout.addView(binding.root)
            }
            setViewVisibility(visibility, binding.root)
            setViewEnableDisable(isEnabled, binding.root)
        }
    }

    private fun createDatePicker(formLayout: FormLayout) {
        val binding = DatepickerLayoutBinding.inflate(LayoutInflater.from(context))
        formLayout.apply {
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
                val dateInput = if (binding.etUserInput.text
                        .toString()
                        .isNotEmpty()
                ) {
                    DateUtils.getYearMonthAndDate(binding.etUserInput.text.toString())
                } else {
                    null
                }
                showDatePicker(
                    context = context,
                    disableFutureDate = disableFutureDate ?: false,
                    minDate = getMaxDateLimit(menstrualPeriod, minDays),
                    maxDate = maxDate,
                    date = dateInput,
                ) { _, year, month, dayOfMonth ->
                    val stringDate = "$dayOfMonth-$month-$year"
                    val parsedDate = DateUtils.getDatePatternDDMMYYYY().parse(stringDate)
                    parsedDate?.let {
                        binding.etUserInput.text = DateUtils.getDateDDMMYYYY().format(it)
                        resultHashMap[id] = DateUtils.getDateString(
                            parsedDate.time,
                            inputFormat = DateUtils.DATE_FORMAT_yyyyMMdd,
                            outputFormat = DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                        )
                    }
                    callback?.invoke(resultHashMap, id)
                }
            }

            if (isMandatory) {
                binding.tvTitle.markMandatory()
            }

            getFamilyView(family)?.addView(binding.root) ?: run {
                parentLayout.addView(binding.root)
            }
            (binding.etUserInput.background as? GradientDrawable)?.apply {
                setStroke(resources.getDimensionPixelSize(R.dimen._1sdp), context.getColor(R.color.edittext_stroke))
            }
            setViewVisibility(visibility, binding.root)
            setViewEnableDisable(isEnabled, binding.root)
            applyTodayDefaultForVisibleCampDate(this, binding)
        }
    }

    /**
     * BD cataract camp date: when field is shown with [Screening.TODAY] in JSON, pre-fill today
     * only if empty so drafts / [FormAutofill] can still replace with a saved value.
     */
    private fun applyTodayDefaultForVisibleCampDate(
        formLayout: FormLayout,
        binding: DatepickerLayoutBinding,
    ) {
        if (formLayout.id != CAMP_DATE) return
        if (formLayout.visibility != VISIBLE) return
        if (formLayout.defaultValue != TODAY) return
        if (binding.etUserInput.text?.isNotBlank() == true) return
        if (resultHashMap.containsKey(formLayout.id)) return
        val now = Calendar.getInstance().time
        binding.etUserInput.setText(DateUtils.getDateDDMMYYYY().format(now))
        resultHashMap[formLayout.id] =
            DateUtils.getDateString(
                now.time,
                inputFormat = DateUtils.DATE_FORMAT_yyyyMMdd,
                outputFormat = DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
            )
    }

    private lateinit var textWatcher: TextWatcher
    private var isDOBUpdated = false

    private fun createAgeView(formLayout: FormLayout) {
        val binding = AgeDobLayoutBinding.inflate(LayoutInflater.from(context))
        formLayout.apply {
            binding.root.tag = id + rootSuffix
            binding.etDateOfBirth.tag = id
            binding.etYears.inputType = InputType.TYPE_CLASS_NUMBER
            binding.etMonths.inputType = InputType.TYPE_CLASS_NUMBER
            binding.etWeeks.inputType = InputType.TYPE_CLASS_NUMBER
            binding.etYears.tag = id + YEAR_KEY
            binding.etMonths.tag = id + MONTH_KEY
            binding.etWeeks.tag = id + WEEK_KEY
            binding.ageValue.tag = id + VALUE
            binding.tvKey.text = updateTitle(title, translate, titleCulture, unitMeasurement)
            binding.tvErrorMessage.tag = id + errorSuffix
            binding.tvDateOfBirth.tag = id + titleSuffix
            textWatcher = object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int,
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int,
                ) {
                    val years = binding.etYears.text
                        .toString()
                        .toIntOrNull() ?: 0
                    val months = binding.etMonths.text
                        .toString()
                        .toIntOrNull() ?: 0
                    val weeks = binding.etWeeks.text
                        .toString()
                        .toIntOrNull() ?: 0

                    fillDetailsOnEditText(years, months, weeks, id, binding.etDateOfBirth)

                    listener.onAgeCheckForPregnancy()
                    callback?.invoke(resultHashMap, id)
                }

                override fun afterTextChanged(s: Editable?) {
                }
            }

            addWatcher(binding.etYears, binding.etMonths, binding.etWeeks)
            binding.etDateOfBirth.safeClickListener {
                val yearMonthWeek = if (binding.etDateOfBirth.text.isNotEmpty()) {
                    DateUtils.getYearMonthAndDate(binding.etDateOfBirth.text.toString())
                } else {
                    null
                }
                formLayout.run {
                    showDatePicker(
                        context = context,
                        disableFutureDate = disableFutureDate ?: false,
                        minDate = minDate,
                        maxDate = maxDate,
                        date = yearMonthWeek,
                    ) { _, year, month, dayOfMonth ->
                        val stringDate = "$dayOfMonth-$month-$year"
                        val parsedDate = DateUtils.getDatePatternDDMMYYYY().parse(stringDate)
                        parsedDate?.let {
                            binding.etDateOfBirth.text = DateUtils.getDateDDMMYYYY().format(it)
                            fillDetailsOnDatePickerSet(it, true, id)
                            callback?.invoke(resultHashMap, id)
                        }
                    }
                }
            }
            binding.etDateOfBirth.addTextChangedListener { dob ->
                listener.onAgeUpdateListener(
                    if (dob.isNullOrBlank()) 0 else CommonUtils.getAgeInYearsByDOB(dob.toString()),
                    serverData,
                    resultHashMap,
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
            getFamilyView(family)?.addView(binding.root) ?: run {
                parentLayout.addView(binding.root)
            }
            setViewVisibility(visibility, binding.root)
            setViewEnableDisable(isEnabled, binding.root)
        }
    }

    private lateinit var textWatcherYMD: TextWatcher
    private var isDOBUpdatedYMD = false

    private fun createAgeYMDView(formLayout: FormLayout) {
        val binding = AgeYmdDobLayoutBinding.inflate(LayoutInflater.from(context))
        formLayout.apply {
            binding.root.tag = id + rootSuffix
            binding.etDateOfBirth.tag = id
            binding.etYears.inputType = InputType.TYPE_CLASS_NUMBER
            binding.etMonths.inputType = InputType.TYPE_CLASS_NUMBER
            binding.etDays.inputType = InputType.TYPE_CLASS_NUMBER
            // Set max length for days (31)
            binding.etDays.filters = arrayOf(
                InputFilter.LengthFilter(2),
                InputFilter { source, _, _, dest, _, _ ->
                    val input = (dest.toString() + source.toString()).toIntOrNull()
                    if (input != null && input > 31) {
                        ""
                    } else {
                        null
                    }
                },
            )
            binding.etYears.tag = id + YEAR_KEY
            binding.etMonths.tag = id + MONTH_KEY
            binding.etDays.tag = id + DefinedParams.DAYS
            binding.tvErrorMessage.tag = id + errorSuffix
            binding.tvDateOfBirth.tag = id + titleSuffix
            binding.tvTitle.text = updateTitle(title, translate, titleCulture, unitMeasurement)

            // Hide DOB section if hideDob property is true (can be set via JSON)
            val hideDob = formLayout.hideDob ?: false
            if (hideDob) {
                binding.tvDateOfBirth.visibility = View.GONE
                binding.etDateOfBirth.visibility = View.GONE
                // Adjust top constraint for years/months/days to start below title
                val params = binding.tvYear.layoutParams as ConstraintLayout.LayoutParams
                params.topToTop = ConstraintLayout.LayoutParams.UNSET
                params.topToBottom = binding.tvTitle.id
                binding.tvYear.layoutParams = params
                // Also adjust months and days labels
                val paramsMonths = binding.tvMonths.layoutParams as ConstraintLayout.LayoutParams
                paramsMonths.topToTop = ConstraintLayout.LayoutParams.UNSET
                paramsMonths.topToBottom = binding.tvTitle.id
                binding.tvMonths.layoutParams = paramsMonths
                val paramsDays = binding.tvDays.layoutParams as ConstraintLayout.LayoutParams
                paramsDays.topToTop = ConstraintLayout.LayoutParams.UNSET
                paramsDays.topToBottom = binding.tvTitle.id
                binding.tvDays.layoutParams = paramsDays
            }

            textWatcherYMD = object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int,
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int,
                ) {
                    val years = binding.etYears.text
                        .toString()
                        .toIntOrNull() ?: 0
                    val months = binding.etMonths.text
                        .toString()
                        .toIntOrNull() ?: 0
                    val days = binding.etDays.text
                        .toString()
                        .toIntOrNull() ?: 0

                    fillDetailsOnEditTextYMD(years, months, days, id, binding.etDateOfBirth)

                    listener.onAgeCheckForPregnancy()
                    callback?.invoke(resultHashMap, id)
                }

                override fun afterTextChanged(s: Editable?) {
                }
            }

            addWatcherYMD(binding.etYears, binding.etMonths, binding.etDays)

            if (!hideDob) {
                binding.etDateOfBirth.safeClickListener {
                    val yearMonthDay = if (binding.etDateOfBirth.text.isNotEmpty()) {
                        DateUtils.getYearMonthAndDate(binding.etDateOfBirth.text.toString())
                    } else {
                        null
                    }
                    formLayout.run {
                        showDatePicker(
                            context = context,
                            disableFutureDate = disableFutureDate ?: false,
                            minDate = minDate,
                            maxDate = maxDate,
                            date = yearMonthDay,
                        ) { _, year, month, dayOfMonth ->
                            val stringDate = "$dayOfMonth-$month-$year"
                            val parsedDate = DateUtils.getDatePatternDDMMYYYY().parse(stringDate)
                            parsedDate?.let {
                                binding.etDateOfBirth.text = DateUtils.getDateDDMMYYYY().format(it)
                                fillDetailsOnDatePickerSetYMD(it, true, id)
                                callback?.invoke(resultHashMap, id)
                            }
                        }
                    }
                }
                binding.etDateOfBirth.addTextChangedListener { dob ->
                    listener.onAgeUpdateListener(
                        if (dob.isNullOrBlank()) 0 else CommonUtils.getAgeInYearsByDOB(dob.toString()),
                        serverData,
                        resultHashMap,
                    )
                }
            }

            if (isMandatory) {
                binding.tvTitle.markMandatory()
                if (!hideDob) {
                    binding.tvDateOfBirth.markMandatory()
                }
            }
            getFamilyView(family)?.addView(binding.root) ?: run {
                parentLayout.addView(binding.root)
            }
            setViewVisibility(visibility, binding.root)
            setViewEnableDisable(isEnabled, binding.root)
        }
    }

    fun fillDetailsOnDatePickerSetYMD(
        date: Date,
        isEnabled: Boolean,
        id: String,
    ) {
        val yearView = getViewByTag(id + YEAR_KEY)
        val monthView = getViewByTag(id + MONTH_KEY)
        val dayView = getViewByTag(id + DefinedParams.DAYS)
        if (yearView is AppCompatEditText && monthView is AppCompatEditText && dayView is AppCompatEditText) {
            removeWatcherYMD(yearView, monthView, dayView)
        }

        isDOBUpdatedYMD = true

        val dobString = DateUtils.getDateString(
            date.time,
            inputFormat = DateUtils.DATE_FORMAT_yyyyMMdd,
            outputFormat = DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
        )

        addOrUpdateDOB(dobString, id)

        val yearMonthDays = DateUtils.getYearMonthAndDays(dobString)

        if (yearView is AppCompatEditText && monthView is AppCompatEditText && dayView is AppCompatEditText) {
            yearMonthDays.years.let { year ->
                yearView.setText(year.toString())
                yearView.isEnabled = isEnabled
                resultHashMap[YEAR_KEY] = year
            }
            yearMonthDays.months.let { month ->
                monthView.setText(month.toString())
                monthView.isEnabled = isEnabled
                resultHashMap[MONTH_KEY] = month
            }
            yearMonthDays.days.let { day ->
                dayView.setText(day.toString())
                dayView.isEnabled = isEnabled
                resultHashMap[DefinedParams.DAYS] = day
            }
            listener.onAgeCheckForPregnancy()
            addWatcherYMD(yearView, monthView, dayView)
        }
    }

    fun removeWatcherYMD(
        etYears: AppCompatEditText,
        etMonths: AppCompatEditText,
        etDays: AppCompatEditText,
    ) {
        etYears.removeTextChangedListener(textWatcherYMD)
        etMonths.removeTextChangedListener(textWatcherYMD)
        etDays.removeTextChangedListener(textWatcherYMD)
    }

    private fun addWatcherYMD(
        etYears: AppCompatEditText,
        etMonths: AppCompatEditText,
        etDays: AppCompatEditText,
    ) {
        etYears.addTextChangedListener(textWatcherYMD)
        etMonths.addTextChangedListener(textWatcherYMD)
        etDays.addTextChangedListener(textWatcherYMD)
        isDOBUpdatedYMD = false
    }

    private fun fillDetailsOnEditTextYMD(
        years: Int,
        months: Int,
        days: Int,
        id: String,
        etDateOfBirth: AppCompatTextView,
    ) {
        if (years == 0 && months == 0 && days == 0) {
            etDateOfBirth.text = ""
            removeIfContains(id)
            removeIfContains(YEAR_KEY)
            removeIfContains(MONTH_KEY)
            removeIfContains(DefinedParams.DAYS)
        } else {
            resultHashMap[YEAR_KEY] = years
            resultHashMap[MONTH_KEY] = months
            resultHashMap[DefinedParams.DAYS] = days

            val calculatedBirthDate = DateUtils.calculateBirthDateYMD(years, months, days)

            etDateOfBirth.text = convertDateFormat(
                calculatedBirthDate,
                DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                DATE_ddMMyyyy,
            )
            addOrUpdateDOB(calculatedBirthDate, id)
        }
    }

    /**
     * Smaller hint for AgeOrDob [AppCompatEditText] only; entered age keeps [Form_Input_Style] size.
     */
    private fun ageOrDobSmallHintText(
        stringId: Int,
        relativeScale: Float = 0.75f,
    ): CharSequence {
        val text = context.getString(stringId)
        return SpannableString(text).apply {
            setSpan(
                RelativeSizeSpan(relativeScale),
                0,
                text.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
    }

    private fun createAgeOrDobView(formLayout: FormLayout) {
        val binding = AgeOrDobLayoutBinding.inflate(LayoutInflater.from(context))
        var ageListener: TextWatcher?
        formLayout.apply {
            binding.root.tag = id + rootSuffix
            binding.etAge.tag = id + "_age"
            binding.etDob.tag = id
            binding.tvErrorMessage.tag = id + errorSuffix
            binding.tvTitle.text = updateTitle(title, translate, titleCulture, unitMeasurement)
            binding.tvAgeLabel.text = getString(R.string.age)
            binding.tvDobLabel.text = getString(R.string.dob)

            // Configure Age EditText
            binding.etAge.inputType = InputType.TYPE_CLASS_NUMBER
            binding.etAge.filters = arrayOf(InputFilter.LengthFilter(3))
            binding.etAge.hint = ageOrDobSmallHintText(R.string.age)

            // Always set up listeners - they will be disabled in edit mode when value is set
            ageListener = binding.etAge.addTextChangedListener { editable ->
                // Only process if age field is enabled (not in edit mode)
                if (binding.etAge.isEnabled) {
                    val ageText = editable?.toString()?.trim() ?: ""
                    if (ageText.isEmpty()) {
                        clearAgeOrDobFields(
                            id,
                            binding.etAge,
                            binding.tvAgeUnit,
                            binding.etDob,
                            binding.ivClearDob,
                            binding.dobInputHolder,
                        )
                    } else {
                        val age = ageText.toIntOrNull()
                        if (age != null) {
                            val maxAge = maxValue?.toInt() ?: 120
                            if (age > maxAge) {
                                showError(id, "Age cannot exceed $maxAge years")
                            } else {
                                fillDOBFromAge(age, id, binding.etDob, binding.tvAgeUnit)
                            }
                        }
                    }
                    callback?.invoke(resultHashMap, id)
                }
            }

            // DOB DatePicker click listener
            binding.dobInputHolder.safeClickListener {
                // Only process if DOB field is enabled (not in edit mode)
                if (binding.dobInputHolder.isEnabled) {
                    val currentDob = binding.etDob.text.toString()
                    val yearMonthDay = if (currentDob.isNotEmpty()) {
                        DateUtils.getYearMonthAndDate(currentDob)
                    } else {
                        null
                    }
                    formLayout.run {
                        showDatePicker(
                            context = context,
                            disableFutureDate = disableFutureDate ?: false,
                            minDate = minDate,
                            maxDate = maxDate,
                            date = yearMonthDay,
                        ) { _, year, month, dayOfMonth ->
                            // Create date string in dd-MM-yyyy format
                            val stringDate = String.format(Locale.getDefault(), "%02d-%02d-%d", dayOfMonth, month, year)
                            val parsedDate = DateUtils.getDatePatternDDMMYYYY().parse(stringDate)
                            parsedDate?.let {
                                fillAgeFromDOB(
                                    it,
                                    id,
                                    binding.etAge,
                                    binding.tvAgeUnit,
                                    binding.etDob,
                                    ageListener,
                                )
                                callback?.invoke(resultHashMap, id)
                            }
                        }
                    }
                }
            }

            // Clear icon removed - no click listener needed

            if (isMandatory) {
                binding.tvTitle.markMandatory()
            }

            // Initialize in create mode (fields enabled, clear button hidden)
            binding.etAge.isEnabled = true
            binding.etAge.isFocusable = true
            binding.etAge.alpha = 1.0f
            binding.tvAgeUnit.alpha = 1.0f
            binding.dobInputHolder.isEnabled = true
            binding.dobInputHolder.isClickable = true
            binding.dobInputHolder.isFocusable = true
            binding.dobInputHolder.alpha = 1.0f
            binding.ivClearDob.visibility = View.GONE

            getFamilyView(family)?.addView(binding.root) ?: run {
                parentLayout.addView(binding.root)
            }
            setViewVisibility(visibility, binding.root)
            setViewEnableDisable(isEnabled, binding.root)

            // Override: Keep fields enabled in create mode (edit mode handled separately by handleAgeOrDobEditMode)
            // Only re-enable if not in edit mode (no existing value)
            if (binding.etDob.text.isNullOrBlank()) {
                binding.etAge.isEnabled = true
                binding.etAge.alpha = 1.0f
                binding.tvAgeUnit.alpha = 1.0f
                binding.dobInputHolder.isEnabled = true
                binding.dobInputHolder.alpha = 1.0f
            }
        }
    }

    private fun fillDOBFromAge(
        age: Int,
        id: String,
        etDob: AppCompatTextView,
        tvAgeUnit: TextView,
    ) {
        try {
            val dobInUTC = DateUtils.calculateDOBFromAge(age)
            // Parse using DateTimeFormatter which properly handles ISO format with timezone
            val inputFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT_yyyyMMddHHmmssZZZZZ)
            val offsetDateTime = OffsetDateTime.parse(dobInUTC, inputFormatter)
            val localDate = offsetDateTime.toLocalDate()
            // Format to dd/MM/yyyy
            val outputFormatter = DateTimeFormatter.ofPattern(DATE_ddMMyyyy)
            val dobFormatted = localDate.format(outputFormatter)

            etDob.text = dobFormatted
            tvAgeUnit.text = if (age == 1) {
                context.getString(R.string.year)
            } else {
                context.getString(R.string.years)
            }
            addOrUpdateDOB(dobInUTC, id)
            // Keep DOB field enabled (no longer disabling after age input)
            // Clear icon remains hidden
            // Hide any error messages
            hideError(id)
        } catch (_: Exception) {
            showError(id, "Error calculating date of birth")
        }
    }

    private fun fillAgeFromDOB(
        dob: Date,
        id: String,
        etAge: AppCompatEditText,
        tvAgeUnit: TextView,
        etDob: AppCompatTextView,
        ageListener: TextWatcher?,
    ) {
        try {
            // Format the date directly from the Date object to show exact selected date
            val dobFormatted = DateUtils.getDateDDMMYYYY().format(dob)
            etDob.text = dobFormatted

            // Convert to UTC for storage
            val dobInUTC = DateUtils.getDateString(
                dob.time,
                inputFormat = DateUtils.DATE_FORMAT_yyyyMMdd,
                outputFormat = DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
            )
            addOrUpdateDOB(dobInUTC, id)

            val display =
                DateUtils.getAgeOrDobDisplayFromDdMmYyyy(dobFormatted) ?: run {
                    showError(id, "Error calculating age")
                    return
                }
            val birthDate = LocalDate.parse(
                dobFormatted,
                DateTimeFormatter.ofPattern(DATE_ddMMyyyy).withLocale(Locale.ENGLISH),
            )
            val maxAge = serverData?.find { it.id == id }?.maxValue?.toInt() ?: 120
            if (Period.between(birthDate, LocalDate.now()).years > maxAge) {
                showError(id, "Age cannot exceed $maxAge years")
                return
            }
            // Remove listener from age field
            ageListener?.let {
                etAge.removeTextChangedListener(ageListener)
            }
            // Keep age field enabled (no longer disabling after DOB input)
            etAge.setText(display.value.toString())
            applyAgeOrDobUnitToView(tvAgeUnit, display)
            // Add listener once data is set
            ageListener?.let {
                etAge.addTextChangedListener(ageListener)
            }
            // Clear icon remains hidden
            // Hide any error messages
            hideError(id)
        } catch (_: Exception) {
            showError(id, "Error calculating age")
        }
    }

    private fun applyAgeOrDobUnitToView(
        tvAgeUnit: TextView,
        display: AgeOrDobDisplay,
    ) {
        val v = display.value
        tvAgeUnit.text =
            when (display.unit) {
                AgeOrDobDisplay.AgeOrDobUnit.DAY ->
                    if (v == 1) {
                        context.getString(R.string.day)
                    } else {
                        context.getString(R.string.days)
                    }

                AgeOrDobDisplay.AgeOrDobUnit.MONTH ->
                    if (v == 1) {
                        context.getString(R.string.month)
                    } else {
                        context.getString(R.string.months)
                    }

                AgeOrDobDisplay.AgeOrDobUnit.YEAR ->
                    if (v == 1) {
                        context.getString(R.string.year)
                    } else {
                        context.getString(R.string.years)
                    }
            }
    }

    private fun handleAgeOrDobEditMode(
        id: String,
        dobValue: String,
        etDob: AppCompatTextView,
    ) {
        try {
            // Get the root view for this component - find parent until we get the root
            var parent = etDob.parent
            var rootView: ViewGroup? = null
            while (parent != null) {
                if (parent is ViewGroup && parent.tag == id + rootSuffix) {
                    rootView = parent
                    break
                }
                parent = parent.parent
            }
            rootView ?: return

            // Find child views using findViewById (they have resource IDs)
            val etAge = rootView.findViewById<AppCompatEditText>(R.id.etAge) ?: return
            val tvAgeUnit = rootView.findViewById<TextView>(R.id.tvAgeUnit) ?: return
            val ivClearDob = rootView.findViewById<View>(R.id.ivClearDob) ?: return
            val dobInputHolder = rootView.findViewById<View>(R.id.dobInputHolder) ?: return

            // Disable age field FIRST to prevent TextWatcher from triggering
            etAge.isEnabled = false
            etAge.isFocusable = false

            // DOB value is already in dd/MM/yyyy format from setValueForView
            val dobFormatted = dobValue

            val display = DateUtils.getAgeOrDobDisplayFromDdMmYyyy(dobFormatted)
            if (display != null) {
                etAge.setText(display.value.toString())
                applyAgeOrDobUnitToView(tvAgeUnit, display)
            } else {
                etAge.setText("")
                tvAgeUnit.text = context.getString(R.string.years)
            }
            etAge.alpha = 0.6f
            tvAgeUnit.alpha = 0.6f

            // Disable DOB field in edit mode
            dobInputHolder.isEnabled = false
            dobInputHolder.isClickable = false
            dobInputHolder.isFocusable = false
            dobInputHolder.alpha = 0.6f

            // Hide clear button in edit mode
            ivClearDob.visibility = View.GONE

            // Store DOB in UTC format in resultHashMap
            // Check if UTC value already exists (set by setDobValueForAgeOrDob)
            val existingUtcDob = resultHashMap[id] as? String
            if (existingUtcDob != null && (existingUtcDob.contains("T") || existingUtcDob.contains("-"))) {
                // UTC value already stored, no need to convert
                addOrUpdateDOB(existingUtcDob, id)
            } else {
                // Convert dd/MM/yyyy to UTC format
                try {
                    val dobDate = DateUtils.getDatePatternDDMMYYYY().parse(dobFormatted)
                    dobDate?.let {
                        val dobInUTC = DateUtils.getDateString(
                            dobDate.time,
                            inputFormat = DateUtils.DATE_FORMAT_yyyyMMdd,
                            outputFormat = DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                        )
                        addOrUpdateDOB(dobInUTC, id)
                    }
                } catch (_: Exception) {
                    // If conversion fails, try to parse as UTC format directly
                    if (dobValue.contains("T") || dobValue.contains("-")) {
                        addOrUpdateDOB(dobValue, id)
                    }
                }
            }
        } catch (_: Exception) {
            // If handling fails, continue normally
        }
    }

    private fun clearAgeOrDobFields(
        id: String,
        etAge: AppCompatEditText,
        tvAgeUnit: TextView,
        etDob: AppCompatTextView,
        ivClearDob: View,
        dobInputHolder: View,
    ) {
        etAge.text?.clear()
        etAge.hint = ageOrDobSmallHintText(R.string.age)
        etDob.text = ""
        tvAgeUnit.text = context.getString(R.string.years)
        etAge.isEnabled = true
        etAge.alpha = 1.0f
        tvAgeUnit.alpha = 1.0f
        dobInputHolder.isEnabled = true
        dobInputHolder.alpha = 1.0f
        ivClearDob.visibility = View.GONE
        removeIfContains(id)
        hideError(id)
    }

    fun showError(
        id: String,
        message: String,
    ) {
        val errorView = getViewByTag(id + errorSuffix)
        if (errorView is TextView) {
            errorView.text = message
            errorView.visibility = View.VISIBLE
        }
    }

    fun showErrorAndScrollTo(
        id: String,
        message: String,
    ) {
        showError(id, message)
        val focusView =
            getViewByTag(id + titleSuffix)
                ?: getViewByTag(id + rootSuffix)
                ?: getViewByTag(id)
        focusView?.let { view ->
            scrollView?.let { scrollToView(it, view) }
        }
    }

    fun hideError(id: String) {
        val errorView = getViewByTag(id + errorSuffix)
        errorView?.visibility = View.GONE
    }

    fun fillDetailsOnDatePickerSet(
        date: Date,
        isEnabled: Boolean,
        id: String = DATE_OF_BIRTH,
    ) {
        val yearView = getViewByTag(id + YEAR_KEY)
        val monthView = getViewByTag(id + MONTH_KEY)
        val weekView = getViewByTag(id + WEEK_KEY)
        if (yearView is AppCompatEditText && monthView is AppCompatEditText && weekView is AppCompatEditText) {
            removeWatcher(yearView, monthView, weekView)
        }

        isDOBUpdated = true

        val dobString = DateUtils.getDateString(
            date.time,
            inputFormat = DateUtils.DATE_FORMAT_yyyyMMdd,
            outputFormat = DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
        )

        addOrUpdateDOB(dobString, id)

        val yearMonthWeeks = DateUtils.getV2YearMonthAndWeek(dobString)

        if (yearView is AppCompatEditText && monthView is AppCompatEditText && weekView is AppCompatEditText) {
            yearMonthWeeks.years.let { year ->
                yearView.setText(year.toString())
                yearView.isEnabled = isEnabled
                resultHashMap[YEAR_KEY] = year
            }
            yearMonthWeeks.months.let { month ->
                monthView.setText(month.toString())
                monthView.isEnabled = isEnabled
                resultHashMap[MONTH_KEY] = month
            }
            yearMonthWeeks.weeks.let { week ->
                weekView.setText(week.toString())
                weekView.isEnabled = isEnabled
                resultHashMap[WEEK_KEY] = week
            }
            listener.onAgeCheckForPregnancy()
            updateAgeView(id)
            addWatcher(yearView, monthView, weekView)
        }
    }

    fun removeWatcher(
        etYears: AppCompatEditText,
        etMonths: AppCompatEditText,
        etWeeks: AppCompatEditText,
    ) {
        etYears.removeTextChangedListener(textWatcher)
        etMonths.removeTextChangedListener(textWatcher)
        etWeeks.removeTextChangedListener(textWatcher)
    }

    private fun addWatcher(
        etYears: AppCompatEditText,
        etMonths: AppCompatEditText,
        etWeeks: AppCompatEditText,
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
        etDateOfBirth: AppCompatTextView,
    ) {
        if (years == 0 && months == 0 && weeks == 0) {
            etDateOfBirth.text = ""
            removeIfContains(id)
            removeIfContains(YEAR_KEY)
            removeIfContains(MONTH_KEY)
            removeIfContains(WEEK_KEY)
        } else {
            resultHashMap[YEAR_KEY] = years
            resultHashMap[MONTH_KEY] = months
            resultHashMap[WEEK_KEY] = weeks

            val calculatedBirthDate = DateUtils.calculateBirthDate(years, months, weeks)

            etDateOfBirth.text = convertDateFormat(
                calculatedBirthDate,
                DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                DATE_ddMMyyyy,
            )
            addOrUpdateDOB(calculatedBirthDate, id)
        }
        updateAgeView(id)
    }

    private fun updateAgeView(id: String) {
        val dobString = resultHashMap[id] as? String
        val ageView = getViewByTag(id + VALUE)
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

    private fun addOrUpdateDOB(
        dateOfBirth: String,
        id: String,
    ) {
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
            thisDay,
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
        isCheckBox: Boolean = false,
        selectedValues: ArrayList<String>? = null,
    ) {
        val conditionList = model.condition
        if (conditionList.isNullOrEmpty()) {
            return
        }

        var matchingCondition: ConditionalModel? = null

        if (model.viewType == VIEW_TYPE_FORM_MULTI_SELECT_SPINNER) {
            matchingCondition = conditionList.firstOrNull {
                it.eqList?.contains(actualValue) == true || it.eq?.equals(actualValue) == true
            }
        }

        if (matchingCondition != null) {
            validateConditionModel(
                matchingCondition,
                actualValue,
                isCheckBox,
            )
        } else {
            conditionList.forEach { conditionalModel ->
                validateConditionModel(conditionalModel, actualValue, isCheckBox, selectedValues)
            }
        }
    }

    private fun validateConditionModel(
        conditionalModel: ConditionalModel,
        actualValue: String?,
        isCheckBox: Boolean,
        selectedValues: ArrayList<String>? = null,
    ) {
        conditionalModel.apply {
            if (targetId != null && targetOption != null) {
                val targetedView = parentLayout.findViewWithTag<View>(targetOption) ?: return@apply
                if (visibility != null) {
                    checkConditionBasedRendering(
                        conditionalModel,
                        ConditionModelConfig.VISIBILITY,
                        actualValue,
                        targetedView,
                        targetOption,
                        isCheckBox,
                        selectedValues,
                    )
                } else if (enabled != null) {
                    checkConditionBasedRendering(
                        conditionalModel,
                        ConditionModelConfig.ENABLED,
                        actualValue,
                        targetedView,
                        targetOption,
                        isCheckBox,
                        selectedValues,
                    )
                }
            } else if (targetId != null) {
                val targetedView = parentLayout.findViewWithTag<View>(targetId + rootSuffix) ?: return@apply
                visibleEnableConditionRendering(
                    visibility,
                    enabled,
                    conditionalModel,
                    actualValue,
                    targetedView,
                    isCheckBox,
                    selectedValues,
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
        targetedView: View,
        targetOption: String? = null,
        isCheckBox: Boolean,
        selectedValues: ArrayList<String>? = null,
    ) {
        conditionalModel.apply {
            if (!eq.isNullOrBlank()) {
                val conditionMatches = if (isCheckBox && !selectedValues.isNullOrEmpty()) {
                    // For DialogCheckbox: Check if ANY selected value matches eq (OR logic)
                    selectedValues.any { it == eq }
                } else {
                    // For single-value fields: Check if actualValue matches eq
                    eq == actualValue
                }

                if (conditionMatches) {
                    handleConfig(true, config, visibility, enabled, targetedView)
                } else if (!isCheckBox) {
                    handleTargetConfig(serverData, targetId, targetedView, config, targetOption)
                }
            } else if (lengthGreaterThan != null) {
                isLengthGreater(
                    conditionalModel,
                    actualValue,
                    targetOption,
                    targetedView,
                    config,
                    isCheckBox,
                )
            } else if (greaterThanOrEqual != null) {
                isGreaterThanOrEqual(
                    conditionalModel,
                    actualValue,
                    targetOption,
                    targetedView,
                    config,
                    isCheckBox,
                    selectedValues,
                )
            } else if (lessThanOrEqual != null) {
                isLessThanOrEqual(
                    conditionalModel,
                    actualValue,
                    targetOption,
                    targetedView,
                    config,
                    isCheckBox,
                    selectedValues,
                )
            } else if (!eqList.isNullOrEmpty()) {
                val conditionMatches = if (isCheckBox && !selectedValues.isNullOrEmpty()) {
                    // For DialogCheckbox: Check if ANY selected value is in eqList (OR logic)
                    selectedValues.any { selectedValue -> eqList!!.contains(selectedValue) }
                } else {
                    // For single-value fields: Check if actualValue is in eqList
                    eqList!!.contains(actualValue)
                }

                if (conditionMatches) {
                    handleConfig(true, config, visibility, enabled, targetedView)
                } else if (!isCheckBox) {
                    handleTargetConfig(serverData, targetId, targetedView, config, targetOption)
                }
            }
        }
    }

    private fun isLengthGreater(
        conditionalModel: ConditionalModel,
        actualValue: String?,
        targetOption: String?,
        targetedView: View,
        config: ConditionModelConfig,
        isCheckBox: Boolean,
    ) {
        conditionalModel.apply {
            if (actualValue != null && actualValue.length > lengthGreaterThan!!) {
                handleConfig(false, config, visibility, enabled, targetedView)
            } else if (!isCheckBox) {
                handleTargetConfig(serverData, targetId, targetedView, config, targetOption)
            }
        }
    }

    private fun isGreaterThanOrEqual(
        conditionalModel: ConditionalModel,
        actualValue: String?,
        targetOption: String?,
        targetedView: View,
        config: ConditionModelConfig,
        isCheckBox: Boolean,
        selectedValues: ArrayList<String>? = null,
    ) {
        conditionalModel.apply {
            val conditionMatches = if (isCheckBox && !selectedValues.isNullOrEmpty()) {
                // For DialogCheckbox: Check if ANY selected value is >= threshold (OR logic)
                selectedValues.any { selectedValue ->
                    selectedValue.toDoubleOrNull()?.let { it >= greaterThanOrEqual!! } ?: false
                }
            } else {
                // For single-value fields: Check if actualValue is >= threshold
                actualValue?.toDoubleOrNull()?.let { it >= greaterThanOrEqual!! } ?: false
            }

            if (conditionMatches) {
                handleConfig(true, config, visibility, enabled, targetedView)
            } else if (!isCheckBox) {
                handleTargetConfig(serverData, targetId, targetedView, config, targetOption)
            }
        }
    }

    private fun isLessThanOrEqual(
        conditionalModel: ConditionalModel,
        actualValue: String?,
        targetOption: String?,
        targetedView: View,
        config: ConditionModelConfig,
        isCheckBox: Boolean,
        selectedValues: ArrayList<String>? = null,
    ) {
        conditionalModel.apply {
            val conditionMatches = if (isCheckBox && !selectedValues.isNullOrEmpty()) {
                // For DialogCheckbox: Check if ANY selected value is <= threshold (OR logic)
                selectedValues.any { selectedValue ->
                    selectedValue.toDoubleOrNull()?.let { it <= lessThanOrEqual!! } ?: false
                }
            } else {
                // For single-value fields: Check if actualValue is <= threshold
                actualValue?.toDoubleOrNull()?.let { it <= lessThanOrEqual!! } ?: false
            }

            if (conditionMatches) {
                handleConfig(true, config, visibility, enabled, targetedView)
            } else if (!isCheckBox) {
                handleTargetConfig(serverData, targetId, targetedView, config, targetOption)
            }
        }
    }

    private fun handleConfig(
        resetValue: Boolean,
        config: ConditionModelConfig,
        visibility: String?,
        enabled: Boolean?,
        targetedView: View,
    ) {
        if (config == ConditionModelConfig.VISIBILITY) {
            setViewVisibility(visibility, targetedView, resetValue)
        } else {
            setViewEnableDisable(enabled, targetedView, resetValue)
        }
    }

    private fun handleTargetConfig(
        serverData: List<FormLayout>?,
        targetId: String?,
        targetedView: View,
        config: ConditionModelConfig,
        targetOption: String?,
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
        resetValue: Boolean = false,
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
        visibility: String?,
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
        targetOption: String,
    ): Boolean? {
        var enableOrNot = status
        targetModel?.optionsList?.forEach { map ->
            if (map[DefinedParams.NAME] == targetOption) {
                val value = map[DefinedParams.IS_ENABLED]
                if (value is Boolean?) {
                    enableOrNot = value
                }
            }
        }
        return enableOrNot
    }

    private fun removeTargetId(
        targetId: String?,
        visibility: String?,
        targetedView: View,
    ) {
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
        targetedView: View,
        isCheckBox: Boolean,
        selectedValues: ArrayList<String>? = null,
    ) {
        if (visibility != null) {
            checkConditionBasedRendering(
                conditionalModel,
                ConditionModelConfig.VISIBILITY,
                actualValue,
                targetedView,
                isCheckBox = isCheckBox,
                selectedValues = selectedValues,
            )
        } else if (enabled != null) {
            checkConditionBasedRendering(
                conditionalModel,
                ConditionModelConfig.ENABLED,
                actualValue,
                targetedView,
                isCheckBox = isCheckBox,
                selectedValues = selectedValues,
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

    private fun setViewVisibility(
        visibility: String?,
        root: View,
        resetValue: Boolean = false,
    ) {
        val proceedChanges = continueChanges(root)

        if (resetValue && visibility != null && visibility != VISIBLE) {
            resetChildViews(root)
        }

        when (visibility) {
            VISIBLE -> {
                root.visible()
            }

            INVISIBLE -> {
                if (proceedChanges) {
                    root.invisible()
                }
            }

            GONE -> {
                if (proceedChanges) {
                    root.gone()
                }
            }

            else -> {
                root.visible()
            }
        }
    }

    private fun restrictReset(model: FormLayout?): Boolean {
        model?.multipleParents?.let { parents ->
            for ((key, value) in parents) {
                if (value is List<*> && matchValues(value, resultHashMap[key])) {
                    return true
                }
            }
        }
        return false
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
        viewGroupItem: (viewGroup: ViewGroup?) -> Unit,
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
                VIEW_TYPE_FORM_DATEPICKER -> {
                    resetEditTextDatePicker(
                        this,
                        model,
                    )
                    resultHashMap.remove(model.id)
                }

                VIEW_TYPE_FORM_SPINNER,
                VIEW_TYPE_FORM_EDITTEXT, VIEW_TYPE_NO_OF_DAYS,
                -> resetEditTextDatePicker(
                    this,
                    model,
                )

                VIEW_TYPE_FORM_AGE -> resetAgeView(this, model)
                VIEW_TYPE_FORM_AGE_YMD -> resetAgeYMDView(this, model)
                VIEW_TYPE_FORM_AGE_OR_DOB -> resetAgeOrDobView(this, model)
                VIEW_TYPE_DIALOG_CHECKBOX -> resetCheckBoxDialogView(this, model)
                VIEW_INFORMATION_LABEL -> {
                    resetInformationLabel(this, model)
                }

                VIEW_TYPE_FORM_CARD_FAMILY -> resetCardView(model)
                else -> {
                    if (view.tag
                            .toString()
                            .lowercase()
                            .let { it.contains(VIEW_TYPE_TIME) || view.tag in listOf(R.id.etMinute, R.id.etHour) }
                    ) {
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

    private fun resetInformationLabel(
        view: View,
        model: FormLayout,
    ) {
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

    fun disableView(view: View) {
        view.isEnabled = false
        view.setBackgroundColor(ContextCompat.getColor(view.context, R.color.border_gray))
    }

    private fun resetChildFormViewGroupComponents(viewGroup: ViewGroup?) {
        viewGroup?.apply {
            val model = serverData?.find { it.id == tag }
            if (restrictReset(model)) return@apply
            model?.let {
                when (model.viewType) {
                    VIEW_TYPE_FORM_DATEPICKER -> {
                        resetEditTextDatePicker(
                            this,
                            model,
                        )
                        resultHashMap.remove(model.id)
                    }

                    VIEW_TYPE_FORM_EDITTEXT, VIEW_TYPE_NO_OF_DAYS,
                    -> resetEditTextDatePicker(
                        this,
                        model,
                    )

                    VIEW_TYPE_FORM_CARD_FAMILY -> resetCardView(model)
                    VIEW_TYPE_FORM_MULTI_SELECT_SPINNER -> resetMultiSelectSpinner(this)
                    VIEW_TYPE_FORM_SPINNER -> resetSpinner(this)
                    VIEW_TYPE_FORM_RADIOGROUP -> resetRadioGroup(this, model)
                    VIEW_TYPE_SINGLE_SELECTION -> {
                        resultHashMap.remove(model.id)
                        if (this is SingleSelectionCustomView) {
                            resetSingleSelectionChildViews()
                        }
                        model.condition?.forEach { conditionalModel ->
                            if (conditionalModel.targetId != null) {
                                serverData
                                    ?.find { it.id == conditionalModel.targetId }
                                    ?.let { childModel ->
                                        val targetedView =
                                            parentLayout.findViewWithTag<View>(childModel.id + rootSuffix)
                                        resetChildFormViewComponents(targetedView)
                                        setViewVisibility(childModel.visibility, targetedView, true)
                                        setViewEnableDisable(
                                            childModel.isEnabled,
                                            targetedView,
                                            true,
                                        )
                                    }
                            }
                        }
                    }
                    //  VIEW_TYPE_FORM_CHECKBOX -> resetCheckbox(this, model)
                }
            }
        }
    }

    private fun resetMultiSelectSpinner(spinnerView: View) {
        if (spinnerView is Spinner) {
            (spinnerView.adapter as? MultiSelectSpinnerAdapter)?.reset()
            resultHashMap.remove(spinnerView.tag.toString())
        }
    }

    private fun resetCardView(model: FormLayout?) {
        if (model?.id == DefinedParams.BP_LOG) {
            for (i in 0..2) {
                getViewByTag("$i-$diastolicSuffix")?.let {
                    resetEditTextDatePicker(it, model)
                }
                getViewByTag("$i-$systolicSuffix")?.let {
                    resetEditTextDatePicker(it, model)
                }
                getViewByTag("$i-$pulseSuffix")?.let {
                    resetEditTextDatePicker(it, model)
                }
            }
        }
    }

    private fun resetEditTextDatePicker(
        view: View,
        model: FormLayout,
    ) {
        if (view is EditText) {
            model.defaultValue?.let {
                view.setText(it)
            } ?: run {
                view.text.clear()
            }
        } else if (view is TextView) {
            model.defaultValue?.let {
                view.text = it
            } ?: run {
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

    /**
     * Returns the view with specific tag within [parentLayout].
     *
     * Most likely the fields and their child's have some tags.
     */
    fun getViewByTag(tag: Any): View? = parentLayout.findViewWithTag(tag)

    private fun resetRadioGroup(
        rgGroup: View,
        model: FormLayout,
        button: Int? = null,
    ) {
        if (rgGroup is RadioGroup) {
            val default = model.defaultValue
            if (default != null || button != null) {
                val count = rgGroup.childCount
                repeat((0..count).count()) {
                    resetRg(rgGroup, default, button)
                }
            } else {
                rgGroup.clearCheck()
                resultHashMap.remove(model.id)
            }
        }
    }

    private fun resetRg(
        rgGroup: RadioGroup,
        default: String?,
        button: Int?,
    ) {
        rgGroup.forEach {
            if (it is RadioButton) {
                when {
                    default != null && it.tag == default -> it.isChecked = true
                    button != null && it.id == button -> it.isChecked = true
                }
            }
        }
    }

    private fun resetAgeView(
        view: View,
        model: FormLayout,
    ) {
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

    private fun resetAgeYMDView(
        view: View,
        model: FormLayout,
    ) {
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
        getViewByTag(R.id.etDays)?.let {
            resetEditTextDatePicker(it, model)
        }
    }

    private fun resetAgeOrDobView(
        view: View,
        model: FormLayout,
    ) {
        val rootView = view.findViewWithTag<View>(model.id + rootSuffix)
        rootView?.let {
            val etAge = it.findViewWithTag<AppCompatEditText>(model.id + "_age")
            val etDob = it.findViewWithTag<AppCompatTextView>(model.id)
            val tvAgeUnit = it.findViewById<TextView>(R.id.tvAgeUnit)
            val ivClearDob = it.findViewById<View>(R.id.ivClearDob)
            val dobInputHolder = it.findViewById<View>(R.id.dobInputHolder)

            etAge?.let { ageView ->
                ageView.text?.clear()
                ageView.hint = ageOrDobSmallHintText(R.string.age)
                ageView.isEnabled = true
                ageView.alpha = 1.0f
            }
            tvAgeUnit?.text = context.getString(R.string.years)
            tvAgeUnit?.alpha = 1.0f
            etDob?.text = ""
            ivClearDob?.visibility = View.GONE
            dobInputHolder?.let { holder ->
                holder.isEnabled = true
                holder.alpha = 1.0f
            }
            removeIfContains(model.id)
            hideError(model.id)
        }
    }

    private fun resetCheckBoxDialogView(
        view: View,
        model: FormLayout,
    ) {
        (view as TextView).text = model.defaultValue ?: ""
        resultHashMap.remove(model.id)
    }

    private fun resetSpinner(spinnerView: View) {
        if (spinnerView is Spinner) {
            spinnerView.setSelection(0, true)
        }
    }

    fun formSubmitAction(view: View): Boolean =
        if (validateInputs()) {
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

    /**
     * Used to scroll to the given view.
     *
     * @param scrollViewParent Parent ScrollView
     * @param view View to which we need to scroll.
     */
    fun scrollToView(
        scrollViewParent: NestedScrollView,
        view: View,
    ) {
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
        accumulatedOffset: Point,
    ) {
        val parentGroup = parent as ViewGroup
        accumulatedOffset.x += child.left
        accumulatedOffset.y += child.top
        if (parentGroup == mainParent) {
            return
        }
        getDeepChildOffset(mainParent, parentGroup.parent, parentGroup, accumulatedOffset)
    }

    fun validateInputs(fieldId: String? = null): Boolean {
        var isValid = true
        focusNeeded = null
        serverData?.forEach { data ->
            if (fieldId == null || fieldId == data.id) {
                data.apply {
                    if ((isMandatory && !resultHashMap.containsKey(id) && isViewVisible(id) && isViewEnabled(id)) ||
                        (isMandatory && resultHashMap.containsKey(id) && resultHashMap[id] is String && (resultHashMap[id] as String).isEmpty())
                    ) {
                        isValid = false
                        requestFocusView(data)
                    } else if (viewType == VIEW_TYPE_FORM_EDITTEXT && isPhoneNumberField(id)) {
                        val actualValue = if (resultHashMap.containsKey(id)) {
                            resultHashMap[id] as? String
                        } else {
                            null
                        }
                        if (isMandatory && actualValue == null) {
                            isValid = false
                            requestFocusView(data)
                        } else {
                            actualValue?.let {
                                if (naValue == actualValue.toDoubleOrNull()) {
                                    hideValidationField(data)
                                } else if (!startsWith.isNullOrEmpty() &&
                                    !checkPhoneNumberValidOrNot(
                                        it,
                                        startsWith,
                                    )
                                ) {
                                    isValid = false
                                    requestFocusView(
                                        data,
                                        getString(
                                            R.string.start_with_validation,
                                            startsWith?.joinToString(separator = " ${getString(R.string.or)} ")
                                                ?: "",
                                        ),
                                    )
                                } else if (!phoneNumberContainMaxLength(
                                        contentLength ?: maxLength,
                                        it,
                                    )
                                ) {
                                    isValid = false
                                    requestFocusView(data)
                                } else if (!FormFieldValidator.isValidMobileNumber(it)) {
                                    isValid = false
                                    requestFocusView(
                                        data,
                                        getString(
                                            R.string.phone_number_invalid,
                                        ),
                                    )
                                } else {
                                    hideValidationField(data)
                                }
                            }
                        }
                    } else if ((id == DATE_OF_BIRTH || id == DateOfBirth) &&
                        !data.viewType.equals(VIEW_TYPE_FORM_AGE_OR_DOB, true) &&
                        isMandatory &&
                        resultHashMap.containsKey(id)
                    ) {
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
                        if (getViewByTag(AssessmentDefinedParams.BP_LOG + rootSuffix)?.visibility == View.VISIBLE) {
                            val list = resultHashMap[id] as ArrayList<BPModel>
                            val validationBPResultModel = Validator.checkValidBPInput(
                                context,
                                list,
                                data,
                            )
                            if (validationBPResultModel.status) {
                                hideValidationField(data)
                            } else {
                                isValid = false
                                requestFocusView(data, validationBPResultModel.message)
                            }
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

                                    if (((!(minHour != null && maxHour != null)) && (!(minMinute != null && maxMinute != null))) ||
                                        (isValidHour && isValidMinute)
                                    ) {
                                        val res = (resultHashMap[dateKey] as? String)?.let { date ->
                                            if (date.equals(Screening.Today, ignoreCase = true) &&
                                                resultHashMap[timeKey] != null
                                            ) {
                                                DateUtils.isValidTimeForLastMealTime(
                                                    hour,
                                                    minute,
                                                    resultHashMap[timeKey] as String,
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
                                                maxMinute,
                                            ),
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
                    } else if (id == MemberRegistration.ID_GUARDIAN &&
                        resultHashMap.containsKey(id) &&
                        isViewVisible(id) &&
                        isViewEnabled(id)
                    ) {
                        val id = CommonUtils.getLongOrNull(resultHashMap[id]) ?: 0
                        // For guardian if the selected id is less than 0,
                        // that means either the user selected --Select-- or + Add guardian
                        if (id < 0) {
                            isValid = false
                            requestFocusView(data)
                        }
                    } else if (data.viewType.equals(VIEW_TYPE_FORM_QR, true)) {
                        if (isMandatory && !resultHashMap.containsKey(id)) {
                            isValid = false
                            getViewByTag(id + errorSuffix)?.let { view ->
                                view as TextView
                                view.visibility = View.VISIBLE
                                view.text = errorMessage
                                    ?: getString(R.string.linking_qr_code_is_mandatory)
                            }
                        } else {
                            getViewByTag(id + errorSuffix)?.visibility = View.GONE
                        }
                    } else {
                        if (resultHashMap.containsKey(id) &&
                            data.viewType.equals(
                                VIEW_TYPE_FORM_EDITTEXT,
                                true,
                            )
                        ) {
                            val actualValue = resultHashMap[id]
                            if (id == Screening.NO_OF_NEONATES && resultHashMap[id].toString().toIntOrNull() == 0) {
                                isValid = false
                                requestFocusView(data)
                            } else {
                                isValid = validateMinMaxLength(
                                    actualValue,
                                    isValid,
                                    data,
                                )
                                if (isValid && data.onlyAlphabets == true) {
                                    isValid = checkOnlyAlphabets(
                                        actualValue,
                                        isValid,
                                        data,
                                    )
                                } else if (isValid && data.optionType == EditTextOptionType.PERSON_NAME) {
                                    val validationRegex = Regex(PersonNameFilter.VALIDATION_PATTERN)
                                    isValid = validationRegex.matches((resultHashMap[id] as? String) ?: "")
                                    if (isValid) {
                                        hideValidationField(data)
                                    } else {
                                        requestFocusView(data, context.getString(R.string.error_person_name))
                                    }
                                }
                            }
                        } else {
                            when (data.viewType) {
                                VIEW_TYPE_MENTAL_HEALTH -> {
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
                }
            }
        }
        if (!isValid && BuildConfig.DEBUG) {
            Timber.tag(TAG_VALIDATE).w(
                "validateInputs(): submit blocked — one or more fields failed (see \"Invalid field\" lines above; first field is scrolled into view).",
            )
        }
        return isValid
    }

    private fun continueChanges(root: View): Boolean {
        serverData?.firstOrNull { (it.id + rootSuffix) == root.tag }?.multipleParents?.let { parents ->
            for ((key, value) in parents) {
                if (value is List<*> && matchValues(value, resultHashMap[key])) {
                    return false
                }
            }
        }
        return true
    }

    private fun matchValues(
        value: List<*>,
        any: Any?,
    ): Boolean =
        when (any) {
            is String -> {
                value.contains(any)
            }

            is List<*> -> {
                value.any { it in any }
            }

            else -> {
                false
            }
        }

    fun isViewVisible(id: String): Boolean {
        val view = getViewByTag(id + rootSuffix)
        return view != null && view.isShown
    }

    fun isViewEnabled(id: String): Boolean {
        val view = getViewByTag(id)
        return view != null && view.isEnabled
    }

    private fun phoneNumberContainMaxLength(
        maxLength: Int?,
        actualValue: String,
    ): Boolean = (maxLength != null && actualValue.length == maxLength)

    private fun checkPhoneNumberValidOrNot(
        actualValue: String,
        startsWithArray: ArrayList<String>?,
    ): Boolean {
        var valid = false
        startsWithArray?.forEach { value ->
            if (actualValue.startsWith(value, true)) {
                valid = true
            }
        }
        return valid
    }

    private fun requestFocusView(
        formLayout: FormLayout,
        message: String? = null,
    ) {
        logValidationFailure(formLayout, message)
        if (focusNeeded == null) {
            focusNeeded = showValidationMessage(formLayout, message)
        } else {
            showValidationMessage(formLayout, message)
        }
    }

    private fun validateMinMaxLength(
        actualValue: Any?,
        valid: Boolean,
        formLayout: FormLayout,
    ): Boolean {
        var isValid = valid
        formLayout.apply {
            if (minLength != null &&
                viewType == VIEW_TYPE_FORM_EDITTEXT &&
                actualValue != null &&
                actualValue is String &&
                actualValue.length < minLength!!
            ) {
                isValid = false
                requestFocusView(
                    formLayout,
                    getString(
                        R.string.min_char_length_validation,
                        minLength!!.toString(),
                    ),
                )
            } else if (maxValue != null || minValue != null) {
                val value = when (actualValue) {
                    is String -> {
                        actualValue.toDoubleOrNull()
                    }

                    is Number -> {
                        actualValue.toDouble()
                    }

                    else -> {
                        null
                    }
                }
                if (value != null) {
                    var isMinMaxValid = true
                    if (naValue != null && naValue == value) {
                        // If naValue equals value that means user is not able to measure
                        hideValidationField(formLayout)
                    } else if (maxValue != null && minValue != null && (value < minValue!! || value > maxValue!!)) {
                        isValid = false
                        isMinMaxValid = false
                        requestFocusView(
                            formLayout,
                            getString(
                                R.string.general_min_max_validation,
                                CommonUtils.getDecimalFormatted(
                                    minValue!!,
                                ),
                                CommonUtils.getDecimalFormatted(
                                    maxValue!!,
                                ),
                            ),
                        )
                    } else if (minValue != null && value < minValue!!) {
                        isValid = false
                        isMinMaxValid = false
                        requestFocusView(
                            formLayout,
                            getString(
                                R.string.general_min_validation,
                                CommonUtils.getDecimalFormatted(
                                    minValue!!,
                                ),
                            ),
                        )
                    } else if (maxValue != null && value > maxValue!!) {
                        isValid = false
                        isMinMaxValid = false
                        requestFocusView(
                            formLayout,
                            getString(
                                R.string.general_max_validation,
                                CommonUtils.getDecimalFormatted(
                                    maxValue!!,
                                ),
                            ),
                        )
                    } else {
                        hideValidationField(formLayout)
                    }
                    // Custom handling for validating systolic diastolic range
                    if (id == AssessmentDefinedParams.SYSTOLIC && isMinMaxValid) {
                        val diastolicValue = CommonUtils.getDoubleOrNull(resultHashMap[AssessmentDefinedParams.DIASTOLIC])
                        if (diastolicValue != null) {
                            val isSystolicInvalid = if (naValue != null) {
                                naValue != value && value <= diastolicValue
                            } else {
                                value <= diastolicValue
                            }
                            if (isSystolicInvalid) {
                                isValid = false
                                requestFocusView(
                                    formLayout,
                                    getString(R.string.systolic_diastolic_error),
                                )
                            }
                        }
                    }
                } else {
                    hideValidationField(formLayout)
                }
            } else if (contentLength != null) {
                if (actualValue is Number) {
                    val actualValueString =
                        CommonUtils.getDecimalFormatted(actualValue)
                    if (contentLength == actualValueString.length) {
                        hideValidationField(formLayout)
                    } else {
                        isValid = false
                        requestFocusView(formLayout)
                    }
                } else {
                    val actualValueString = actualValue.toString()
                    if (contentLength == actualValueString.length) {
                        hideValidationField(formLayout)
                    } else {
                        isValid = false
                        requestFocusView(formLayout)
                    }
                }
            } else {
                hideValidationField(formLayout)
            }
        }
        return isValid
    }

    private fun showValidationMessage(
        formLayout: FormLayout,
        message: String? = null,
    ): View? {
        formLayout.apply {
            val view = getViewByTag(formLayout.id + errorSuffix)
            if (view is TextView) {
                view.visibility = View.VISIBLE
                if (message != null) {
                    view.text = message
                } else {
                    view.text = getErrorMessageFromJSON(errorMessage, cultureErrorMessage)
                }
                return getViewByTag(formLayout.id + titleSuffix)
            }
        }
        return null
    }

    private fun getErrorMessageFromJSON(
        errorMessage: String?,
        cultureErrorMessage: String?,
    ): String =
        if (translate && !(cultureErrorMessage.isNullOrEmpty() || cultureErrorMessage.isBlank())) {
            cultureErrorMessage
        } else if (!(errorMessage.isNullOrEmpty() || errorMessage.isBlank())) {
            errorMessage
        } else {
            getString(R.string.default_user_input_error)
        }

    private fun hideValidationField(formLayout: FormLayout) {
        formLayout.apply {
            val view = getViewByTag(formLayout.id + errorSuffix)
            if (view != null && view is TextView && view.isVisible) {
                view.visibility = View.GONE
            }
        }
    }

    fun validateCheckboxDialogue(
        id: String,
        formLayout: FormLayout,
        resultMap: ArrayList<HashMap<String, Any>>,
        isListenerEnable: Boolean = true,
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
                if (resultMap.isEmpty()) {
                    view.text = ""
                } else {
                    view.text = resultMap.joinToString {
                        if (translate && it[DefinedParams.CULTURE_VALUE]?.toString().orEmpty().isNotBlank()) {
                            it[DefinedParams.CULTURE_VALUE].toString()
                        } else {
                            it[DefinedParams.NAME].toString()
                        }
                    }
                }
            }
        }

        // Extract selected values for condition evaluation
        // Prioritize 'value' (JSON id) over 'ID' (database id) for condition matching
        val selectedValues = ArrayList<String>()
        resultMap.forEach { item ->
            val selectedId = item[VALUE]?.toString()
                ?: item[DefinedParams.ID]?.toString()
            selectedId?.let { selectedValues.add(it) }
        }

        if (isContainsOther(resultMap)) {
            setConditionalVisibility(
                formLayout,
                DefinedParams.OTHER,
                isCheckBox = true,
                selectedValues = selectedValues,
            )
        } else {
            setConditionalVisibility(
                formLayout,
                null,
                isCheckBox = true,
                selectedValues = selectedValues,
            )
        }
        if (isListenerEnable) {
            callback?.invoke(resultHashMap, id)
        }
    }

    private fun isContainsOther(mapList: ArrayList<*>): Boolean {
        var status = false
        mapList.forEach { map ->
            if (map is HashMap<*, *>) {
                val name = map[DefinedParams.NAME]
                if (name is String && (name.equals(DefinedParams.OTHER, true) || name.equals(OTHER_METHOD_SPECIFY, true))) {
                    status = true
                    return@forEach
                }
            }
        }
        return status
    }

    fun setDobValueForAgeOrDob(
        id: String,
        originalDobUtc: String,
        dobFormatted: String,
        view: View,
    ) {
        // Store original UTC value in resultHashMap for edit mode
        resultHashMap[id] = originalDobUtc
        // Set the formatted value in the view
        setValueForView(dobFormatted, view)
    }

    fun setValueForView(
        value: Any?,
        view: View,
    ) {
        if (view is AppCompatEditText) {
            when (value) {
                is String -> {
                    view.setText(value)
                }

                is Int -> {
                    view.setText(value.toString())
                }

                else -> {
                    view.setText("")
                }
            }
        }

        if (view is AppCompatTextView) {
            when (value) {
                is String -> {
                    view.text = value
                    // Check if this is AgeOrDob component's DOB field
                    val viewTag = view.tag as? String
                    if (viewTag != null && value.isNotBlank()) {
                        val formLayout = serverData?.find { it.id == viewTag && it.viewType == VIEW_TYPE_FORM_AGE_OR_DOB }
                        if (formLayout != null) {
                            // This is AgeOrDob component in edit mode - handle edit mode setup
                            handleAgeOrDobEditMode(viewTag, value, view)
                        }
                    }
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
                if (selectedIndex != -1) {
                    view.setSelection(selectedIndex, true)
                }
            }
            if (adapter != null && adapter is CustomSpinnerAdapter && value is String) {
                val selectedIndex = adapter.getIndexOfItemByName(value)
                if (selectedIndex != -1) {
                    view.setSelection(selectedIndex, true)
                }
            }
        }
    }

    /**
     * Injects data to **Spinner** view with
     * adding default option if [shouldAddDefault] is true && either not mandatory or list > 1
     */
    fun spinnerDataInjection(
        data: LocalSpinnerResponse,
        mapList: ArrayList<Map<String, Any>>,
        shouldAddDefault: Boolean = true,
    ) {
        val spinner = getViewByTag(data.tag) as? AppCompatSpinner ?: return
        val mandatory = serverData?.find { it.id == data.tag }?.isMandatory ?: false
        if (spinner.adapter is CustomSpinnerAdapter) {
            if (shouldAddDefault && (!mandatory || mapList.size != 1)) {
                mapList.add(0, createDefaultMap())
            }
            (spinner.adapter as CustomSpinnerAdapter).setData(mapList)
            spinner.onItemSelectedListener?.onItemSelected(
                spinner,
                spinner.selectedView,
                0,
                spinner.selectedItemId,
            )
        }
    }

    /**
     * Creates a default item for spinner
     */
    fun createDefaultMap(): Map<String, Any> =
        hashMapOf(
            DefinedParams.NAME to getString(R.string.please_select),
            DefinedParams.ID to DEFAULT_ID,
        )

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

    private fun checkOnlyAlphabets(
        actualValue: Any?,
        valid: Boolean,
        formLayout: FormLayout,
    ): Boolean {
        var isValid = valid
        formLayout.apply {
            if (viewType == VIEW_TYPE_FORM_EDITTEXT &&
                actualValue is String &&
                actualValue.isNotBlank() &&
                !CommonUtils.isAlphabetsWithSpace(
                    actualValue,
                )
            ) {
                isValid = false
                requestFocusView(formLayout, getString(R.string.only_alphabets_validation))
            } else {
                hideValidationField(formLayout)
            }
        }
        return isValid
    }

    private fun onlyAlphabet(
        itemId: String,
        input: String,
    ): Boolean {
        val serverItem = serverData?.firstOrNull { it.id == itemId }
        return serverItem?.let { it.onlyAlphabets == true && CommonUtils.isAlphabetsWithSpace(input) }
            ?: true
    }

    fun checkValidMentalHealth(
        formLayout: FormLayout,
        id: String,
    ): Boolean {
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
        if (mentalHealthQuestions == null || (mentalHealthQuestions?.containsKey(id) == false)) {
            listener.loadLocalCache(id, localDataCache = Screening.Fetch_MH_Questions)
        }
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

    fun fetchMHQuestions(
        id: String,
        questions: LocalSpinnerResponse?,
    ) {
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
        isViewOnly: Boolean = false,
    ) {
        val recyclerView = questions?.tag?.let { getViewByTag(it) } ?: return
        if (recyclerView.isVisible) {
            this.mentalHealthEditList = mentalHealthEditList?.let {
                java.util.ArrayList(mentalHealthEditList)
            }
            questions.let { response ->
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
                                translate = translate,
                            ) { id, question, result, isUnselect, isClicked ->
                                processMentalHealthResult(
                                    id,
                                    question,
                                    result,
                                    isUnselect,
                                    isClicked,
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
        isClicked: Boolean,
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
        isClicked: Boolean,
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
        question: String,
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

    private fun saveMentalHealthQuestions(
        id: String,
        questions: java.util.ArrayList<MentalHealthOption>,
    ) {
        if (mentalHealthQuestions == null) {
            mentalHealthQuestions = HashMap()
        }
        mentalHealthQuestions?.put(id, questions)
    }

    fun checkIfNoSymptomsPresent(diabetes: Any?): Boolean {
        var status = false
        if ((diabetes is java.util.ArrayList<*>) && diabetes.isNotEmpty()) {
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

    fun showHideCardFamily(
        status: Boolean,
        family: String,
    ) {
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

    fun showMHView(
        showView: Boolean,
        types: List<String>,
    ) {
        types.forEach { type ->
            val mhType = when (type) {
                AssessmentDefinedParams.PHQ9 -> Pair(
                    AssessmentDefinedParams.PHQ9.lowercase(),
                    AssessmentDefinedParams.PHQ9_Mental_Health,
                )

                AssessmentDefinedParams.GAD7 -> Pair(
                    AssessmentDefinedParams.GAD7.lowercase(),
                    AssessmentDefinedParams.GAD7_Mental_Health,
                )

                else -> Pair(Screening.PHQ4.lowercase(), Screening.MentalHealthDetails)
            }

            getViewByTag(mhType.first + rootSuffix)?.let {
                if (showView) {
                    it.visible()
                } else {
                    parentLayout.removeView(it)
                }
            }
        }
    }

    fun populateEditableViews(
        serverData: List<FormLayout>,
        mentalHealthEditList: java.util.ArrayList<Map<String, Any>>? = null,
    ) {
        this.serverData =
            serverData.filter { it.viewType != VIEW_TYPE_FORM_CARD_FAMILY && it.isEditable }
        this.mentalHealthEditList = mentalHealthEditList?.let {
            java.util.ArrayList(mentalHealthEditList)
        }
        parentLayout.removeAllViews()
        addEditableCards(serverData)
        this.serverData?.forEach { formLayout ->
            when (formLayout.viewType) {
                VIEW_TYPE_FORM_CARD_FAMILY -> createCardViewFamily(formLayout)
                VIEW_TYPE_FORM_EDITTEXT -> createEditText(formLayout)
                VIEW_TYPE_FORM_RADIOGROUP -> createRadioGroup(formLayout)
                VIEW_TYPE_SINGLE_SELECTION -> createSingleSelectionView(formLayout)
                VIEW_TYPE_FORM_SPINNER -> createCustomSpinner(formLayout)
                VIEW_TYPE_FORM_MULTI_SELECT_SPINNER -> createMultiSelectSpinner(formLayout)
                VIEW_TYPE_DIALOG_CHECKBOX -> createCheckboxDialogView(formLayout)
                VIEW_INFORMATION_LABEL -> createInformationLabel(formLayout)
                VIEW_TYPE_INSTRUCTION -> createInstructionView(formLayout)
                VIEW_TYPE_FORM_TEXTLABEL -> createTextLabel(formLayout)
                VIEW_TYPE_MENTAL_HEALTH -> createMentalHealthView(formLayout)
                VIEW_TYPE_FORM_AGE -> createAgeView(formLayout)
                VIEW_TYPE_FORM_AGE_YMD -> createAgeYMDView(formLayout)
                VIEW_TYPE_FORM_AGE_OR_DOB -> createAgeOrDobView(formLayout)
                VIEW_TYPE_NO_OF_DAYS -> createNoOfDaysView(formLayout)
                VIEW_TYPE_FORM_DATEPICKER -> createDatePicker(formLayout)
                VIEW_TYPE_FORM_BP -> createBPView(formLayout)
                VIEW_TYPE_TIME -> createTimeView(formLayout)
                VIEW_TYPE_FORM_QR -> addScannerView(formLayout)
            }
        }
    }

    private fun createMultiSelectSpinner(serverViewModel: FormLayout) {
        val binding = CustomSpinnerBinding.inflate(LayoutInflater.from(context))
        serverViewModel.apply {
            binding.root.tag = id + rootSuffix
            binding.etUserInput.tag = id
            binding.tvTitle.tag = id + titleSuffix
            binding.tvErrorMessage.tag = id + errorSuffix
            binding.tvTitle.text = CommonUtils.getTitle(serverViewModel, translate)
            val dropDownList = ArrayList<Map<String, Any>>()
            if (isMandatory) {
                binding.tvTitle.markMandatory()
            }
            optionsList?.let { list ->
                addDropDownList(list, dropDownList)
            }
            val adapter = MultiSelectSpinnerAdapter(
                context,
                dropDownList,
                resultHashMap[id] as? ArrayList<Map<String, Any>> ?: ArrayList(),
            )
            binding.etUserInput.adapter = adapter
            adapter.setOnItemSelectedListener(object :
                MultiSelectSpinnerAdapter.OnItemSelectedListener {
                override fun onItemSelected(
                    selectedItems: List<Map<String, Any>>,
                    actionItem: Map<String, Any>?,
                    isDeselect: Boolean,
                ) {
                    if (selectedItems.isNotEmpty() && actionItem != null) {
                        resultHashMap[id] = selectedItems.map { it[DefinedParams.ID] }
                        resultHashMap[id + DefinedParams.SPINNER_VALUE] = getSpinners(selectedItems)

                        val exceptActionItem: ArrayList<*>? =
                            (resultHashMap[id] as? ArrayList<*>)?.filter { it != actionItem[DefinedParams.ID] } as? ArrayList<*>

                        val actualValue =
                            actionItem[DefinedParams.ID] ?: DefinedParams.DEFAULT_ID

                        val matchingCondition = serverViewModel.condition?.find {
                            it.eqList?.contains(actualValue) == true || it.eq == actualValue
                        }

                        matchingCondition?.let { matchingCond ->
                            val hasMatch: Boolean =
                                matchingCond.eqList?.any {
                                    it in (exceptActionItem ?: emptyList<Any>())
                                } == true

                            if (!hasMatch) {
                                if (isDeselect) matchingCond.visibility = GONE

                                handleSelectedItem(
                                    actionItem,
                                    id,
                                    dependentID,
                                    serverViewModel,
                                    dependentIDList,
                                )

                                if (isDeselect) matchingCond.visibility = VISIBLE
                            }
                        }
                    } else {
                        handleSelectedItem(
                            hashMapOf<String, Any>().apply {
                                put(DefinedParams.ID, DefinedParams.DEFAULT_ID)
                                put(DefinedParams.NAME, DefinedParams.DEFAULT_ID_LABEL)
                            },
                            id,
                            dependentID,
                            serverViewModel,
                            dependentIDList,
                        )

                        resultHashMap.remove(id)
                        resultHashMap.remove(id + DefinedParams.SPINNER_VALUE)
                    }
                }
            })

            getFamilyView(family)?.addView(binding.root) ?: run {
                parentLayout.addView(binding.root)
            }
            setViewVisibility(visibility, binding.root)
            setViewEnableDisable(isEnabled, binding.root)
        }
    }

    private fun handleSelectedItem(
        selectedItem: Map<String, Any>?,
        id: String,
        dependentID: String?,
        serverViewModel: FormLayout,
        dependentIDList: ArrayList<String>?,
    ) {
        selectedItem?.let {
            val selectedId = it[DefinedParams.ID]
            val selectedName = it[DefinedParams.NAME]
            if ((selectedId is String && selectedId == "-1")) {
                if (resultHashMap.containsKey(id)) {
                    handleId(id)
                    dependentID?.let { deptId ->
                        resetDependantSpinnerView(deptId)
                    } ?: run {
                        dependentIDList?.forEach { deptId ->
                            resetDependantSpinnerView(deptId)
                        }
                    }
                } else {
                    if (editScreen == true) {
                        resultHashMap[id] = ""
                    }
                }
            } else {
                if (serverViewModel.viewType != VIEW_TYPE_FORM_MULTI_SELECT_SPINNER) {
                    resultHashMap[id] = it[DefinedParams.ID] as Any
                    resultHashMap[id + DefinedParams.SPINNER_VALUE] = it[DefinedParams.NAME] as Any
                }
                // API Spinner Value
                val oplData = it[DefinedParams.OPTIONAL_DATA]
                if (oplData is Pair<*, *>) {
                    val key = oplData.first
                    val value = oplData.second
                    if (key is String && value is String) {
                        resultHashMap[key] = value
                    }
                }
                dependentID?.let { deptId ->
                    resetDependantSpinnerView(deptId)
                    listener.loadLocalCache(
                        deptId,
                        deptId,
                        it[DefinedParams.OPTIONAL_DATA]?.toString()?.toLongOrNull()
                            ?: it[DefinedParams.ID] as Long,
                    )
                } ?: run {
                    dependentIDList?.forEach { deptId ->
                        resetDependantSpinnerView(deptId)
                        listener.loadLocalCache(
                            deptId,
                            deptId,
                            it[DefinedParams.OPTIONAL_DATA]?.toString()?.toLongOrNull()
                                ?: it[DefinedParams.ID] as Long,
                        )
                    }
                }
            }
            selectedIdVisibility(selectedId, serverViewModel, selectedName)
        }
    }

    private fun getSpinners(selectedItems: List<Map<String, Any>>): Any =
        if (SecuredPreference.getIsTranslationEnabled()) {
            selectedItems.map { it[DefinedParams.CULTURE_VALUE] ?: it[DefinedParams.NAME] }
        } else {
            selectedItems.map { it[DefinedParams.NAME] }
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

    fun isViewGone(tag: Any): Boolean = getViewByTag(tag)?.visibility == View.GONE

    /**
     * Returns for layout for a given id
     */
    fun getFormLayout(id: String): FormLayout? = getServerData()?.firstOrNull { it.id == id }

    /**
     * Returns result layout for a given id
     */
    fun getResult(id: String): Any? = resultHashMap[id]

    /**
     * Returns if the given form layout id is for phone number
     */
    fun isPhoneNumberField(id: String): Boolean =
        id.contains(Screening.phoneNumber) ||
            id.contains(PHONE_NUMBER) ||
            id.contains(CommunityDetails.EmergencyContactPhu) ||
            id.contains(CommunityDetails.EmergencyTransportContactNo) ||
            id.contains(CommunityDetails.AmbulanceDriverContactNo) ||
            id.contains(RxBuddy.rxBuddyPhoneNumber)

    /**
     * Mark a field as mandatory or non-mandatory
     */
    fun markMandatory(
        id: String,
        mandatory: Boolean,
    ) {
        getFormLayout(id)?.isMandatory = mandatory
        if (mandatory) {
            (getViewByTag(id + titleSuffix) as? TextView)?.markMandatory()
        } else {
            (getViewByTag(id + titleSuffix) as? TextView)?.markNonMandatory()
        }
    }

    /**
     * Updates the national ID field **title** and [EditText] hint to match the selected
     * [MemberRegistration.ID_TYPE] option ([DefinedParams.NAME] and [DefinedParams.CULTURE_VALUE]).
     * Falls back to the static `national_id` [FormLayout] when ID type is empty or [DefinedParams.NA].
     * Does not mutate [FormLayout.titleCulture] on the model.
     */
    fun updateNationalIdLabelForIdType(
        selectedIdType: String?,
        translate: Boolean,
        optionsViewId: String = MemberRegistration.ID_TYPE,
        viewId: String = MemberRegistration.NATIONAL_ID,
    ) {
        val idTypeLayout = getFormLayout(optionsViewId) ?: return
        val nationalIdLayout = getFormLayout(viewId) ?: return
        val titleView = getViewByTag(viewId + titleSuffix) as? TextView
        val nationalIdInput = getViewByTag(viewId) as? EditText

        val options = idTypeLayout.optionsList
        val option =
            if (!selectedIdType.isNullOrBlank() && selectedIdType != DefinedParams.NA) {
                options?.firstOrNull { (it[DefinedParams.ID] as? String) == selectedIdType }
            } else {
                null
            }

        val title: CharSequence =
            if (option != null) {
                val name =
                    (option[DefinedParams.NAME] as? String)?.takeIf { it.isNotEmpty() }
                        ?: nationalIdLayout.title
                val cultureValue =
                    (option[DefinedParams.CULTURE_VALUE] as? String)?.takeIf { it.isNotEmpty() }
                        ?: nationalIdLayout.titleCulture
                updateTitle(name, translate, cultureValue, null)
            } else {
                updateTitle(
                    nationalIdLayout.title,
                    translate,
                    nationalIdLayout.titleCulture,
                    null,
                )
            }
        titleView?.text = title
        nationalIdInput?.hint = title
        if (nationalIdLayout.isMandatory) {
            titleView?.markMandatory()
        } else {
            titleView?.markNonMandatory()
        }
    }

    private fun defaultError(itemId: String): String {
        val error: String? = if (SecuredPreference.getIsTranslationEnabled()) {
            serverData?.firstOrNull { it.id == itemId }?.cultureErrorMessage
        } else {
            serverData?.firstOrNull { it.id == itemId }?.errorMessage
        }
        return if (error.isNullOrBlank()) getString(R.string.default_user_input_error) else error
    }

    private fun upazilaId(): Pair<Long, String> {
        var upazilaId: Long = 0
        val errorMessage: String = defaultError(DefinedParams.UPAZILA)
        getViewByTag(DefinedParams.UPAZILA)?.let { view ->
            if (view is Spinner) {
                val adapter = view.adapter
                if (adapter != null && adapter is CustomSpinnerAdapter) {
                    val pos = view.selectedItemPosition
                    if (pos > 0 && adapter.getData(pos) != null) {
                        adapter.getData(pos)?.let {
                            val id = it[DefinedParams.ID]
                            if (id is Long) {
                                upazilaId = id
                            }
                        }
                    }
                }
            }
        }
        return Pair(upazilaId, errorMessage)
    }

    private fun upazilaIdError(
        upazilaId: Long,
        errorMessage: String,
    ) {
        getViewByTag(DefinedParams.UPAZILA + errorSuffix)?.let { tvError ->
            (tvError as TextView).apply {
                text = errorMessage
                if (upazilaId > 0) {
                    visibility = View.GONE
                } else {
                    visibility = View.VISIBLE
                    focusNeeded = getViewByTag(DefinedParams.UPAZILA + titleSuffix) ?: this
                }
            }
        }
    }

    private fun villageIdError(
        villageId: Long,
        errorMessage: String,
    ) {
        getViewByTag(DefinedParams.VILLAGE + errorSuffix)?.let { tvError ->
            (tvError as TextView).apply {
                text = errorMessage
                if (villageId > 0) {
                    visibility = View.GONE
                } else {
                    visibility = View.VISIBLE
                    focusNeeded = getViewByTag(DefinedParams.VILLAGE + titleSuffix) ?: this
                }
            }
        }
    }

    private fun villageId(): Pair<Long, String> {
        var villageId: Long = 0
        val errorMessage: String = defaultError(DefinedParams.VILLAGE)
        getViewByTag(DefinedParams.VILLAGE)?.let { view ->
            if (view is Spinner) {
                val adapter = view.adapter
                if (adapter != null && adapter is CustomSpinnerAdapter) {
                    val pos = view.selectedItemPosition
                    if (pos > 0 && adapter.getData(pos) != null) {
                        adapter.getData(pos)?.let {
                            val id = it[DefinedParams.ID]
                            if (id is Long) {
                                villageId = id
                            }
                        }
                    }
                }
            }
        }
        return Pair(villageId, errorMessage)
    }

    private fun unionIdError(
        unionId: Long,
        errorMessage: String,
    ) {
        getViewByTag(DefinedParams.UNION + errorSuffix)?.let { tvError ->
            (tvError as TextView).apply {
                text = errorMessage
                if (unionId > 0) {
                    visibility = View.GONE
                } else {
                    visibility = View.VISIBLE
                    focusNeeded = getViewByTag(DefinedParams.UNION + titleSuffix) ?: this
                }
            }
        }
    }

    private fun unionId(): Pair<Long, String> {
        var unionId: Long = 0
        val errorMessage: String = defaultError(DefinedParams.UNION)
        getViewByTag(DefinedParams.UNION)?.let { view ->
            if (view is Spinner) {
                val adapter = view.adapter
                if (adapter != null && adapter is CustomSpinnerAdapter) {
                    val pos = view.selectedItemPosition
                    if (pos > 0 && adapter.getData(pos) != null) {
                        adapter.getData(pos)?.let {
                            val id = it[DefinedParams.ID]
                            if (id is Long) {
                                unionId = id
                            }
                        }
                    }
                }
            }
        }
        return Pair(unionId, errorMessage)
    }

    fun generateUniqueNationalId(
        postValue: Boolean,
        level1SiteId: Long = 0,
    ) {
        val isParacounselorEnabled = false
        var upazilaId: Long = 0
        if (!isParacounselorEnabled) {
            if (level1SiteId > 0) {
                upazilaId = level1SiteId
            } else {
                val upazila = upazilaId()
                upazila.first.let {
                    upazilaId = it
                }
                focusNeeded = null
                if (postValue) {
                    upazilaIdError(upazilaId, upazila.second)
                }
            }
        }

        var unionId: Long
        val union = unionId()
        union.first.let {
            unionId = it
        }
        focusNeeded = null
        if (postValue) {
            unionIdError(unionId, union.second)
        }

        var villageId: Long
        val village = villageId()
        village.first.let {
            villageId = it
        }
        focusNeeded = null
        if (postValue) {
            villageIdError(villageId, village.second)
        }

        val upazilaCodeValidation = upazilaId > 0 || isParacounselorEnabled

        if (upazilaCodeValidation && unionId > 0 && villageId > 0) { // listener.generateNationalID(Triple(upazilaId, unionId, villageId), postValue)
        } else {
            focusNeeded?.let { focusNeeded ->
                scrollView?.let { scrollView ->
                    scrollToView(scrollView, focusNeeded)
                }
            }
        }
    }

    fun showQRScannedText(
        resultString: String,
        tag: String,
    ) {
        getViewByTag(tag)?.let {
            (it as? AppCompatEditText)?.apply {
                setText(resultString)
            }
        }

        getViewByTag(tag + errorSuffix)?.visibility = View.GONE
    }

    /**
     * Edit flow: a community patient already has a QR linked at enrollment, but the patient-details
     * API does not return the QR value. This shows the "linked successfully" state (with the
     * "Scan New QR Code?" option still available) WITHOUT writing a value into [resultHashMap], so
     * submitting preserves the existing link unless the user explicitly re-scans a new code.
     */
    fun markQrAlreadyLinked(tag: String) {
        getViewByTag(tag + titleSuffix)?.let {
            if (it is TextView) {
                it.text = getString(R.string.qr_code_scanned)
            }
        }
        getViewByTag(tag + scanDoneButtonSuffix)?.visibility = View.VISIBLE
        getViewByTag(tag + innerRootSuffix)?.visibility = View.VISIBLE
        getViewByTag(tag + errorSuffix)?.visibility = View.GONE
    }

    fun showErrorQRScanned(
        tag: String,
        message: String? = null,
    ) {
        val errorTextView = getViewByTag(tag + errorSuffix)
        message?.let {
            if (errorTextView is TextView) {
                errorTextView.text = message
            }
        }
        errorTextView?.visibility = View.VISIBLE
        val modelList = serverData?.filter { it.id == tag }
        resultHashMap.remove(tag)
        getViewByTag(tag + titleSuffix)?.let { view ->
            if (view is TextView && !modelList.isNullOrEmpty()) {
                view.text = CommonUtils.getTitle(modelList[0], translate)
                if (modelList[0].isMandatory) {
                    view.markMandatory()
                }
                view.safeClickListener { listener.onQRScanRequested() }
            }
        }

        getViewByTag(tag + scanImageView)?.let { view ->
            view.safeClickListener { listener.onQRScanRequested() }
        }

        getViewByTag(tag + scanDoneButtonSuffix)?.let { view ->
            view.visibility = View.GONE
        }

        getViewByTag(tag + innerRootSuffix)?.let { view ->
            view.visibility = View.GONE
        }
    }
}
