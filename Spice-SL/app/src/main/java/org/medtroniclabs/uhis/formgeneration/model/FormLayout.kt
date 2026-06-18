package org.medtroniclabs.uhis.formgeneration.model

import org.medtroniclabs.uhis.data.model.RecommendedDosageListModel

/**
 * Data class containing form rendering details for individual fields
 *
 * @param viewType Decides which type of view to be drawn like, CardView, EditText etc.
 * Refer [org.medtroniclabs.uhis.formgeneration.config.ViewType] for more info.
 * @param id Unique id for the field, which is getting mapped to store data
 * and also other conditional handling
 * @param title Title for the particular field
 * @param family On which card family the field to be rendered
 * @param visibility Default visibility of the field
 * @param isMandatory Whether adding some input is mandatory for the field or not
 * @param displayAsterisk Used in view type **TextLabel** to display * at the end of the title
 * @param minLength Validation for minimum length input for view type **EditText**
 * @param maxLength Max length for input field for view types **NoOfDaysView**, **EditText**, **BP**, **EditTextArea**
 * @param inputType Input type for view type **EditText**. Refer [android.text.InputType] for more info.
 * If nothing is passed, it takes the default whatever is there for [android.widget.EditText]
 * @param orientation Orientation for view type **RadioGroup**. For horizontal 0, for vertical 1. If nothing provided default orientation is horizontal.
 * @param errorMessage Error message for a particular field if validation failed except **CardView**, **Instruction**, **InformationLabel**, **TextLabel**.
 * @param cultureErrorMessage Error message for a particular field if validation failed with second language.
 * @param disableFutureDate Flag whether to allow future dates or not for view type **DatePicker**, **Age**.
 * @param minDate Min date for view type **Age**.
 * @param maxDate Max date for view type **DatePicker**, **Age**. If disableFutureDate is provided, then this field is of no use.
 * @param optionsList Options for view types **SingleSelectionView**, **RadioGroup**, **Spinner**, **DialogCheckbox**.
 * @param dayOptionsList Day options for view type **TimeView**.
 * @param timeOptionsList Time options for view type **TimeView**.
 * @param isEnabled Flag whether the field is enabled or not
 * @param defaultValue Default value for a field for view types **EditText**, **EditTextArea**, **Spinner**, **InformationLabel**, **RadioGroup**.
 * @param condition Based on current field value, it will enable or disable other fields which are passed in the conditions
 * @param hint Hint for the particular field
 * @param isNeedAction Used for view type **EditText** & id **identityValue** to generate national id
 * @param instructions Used to display instructions for view type **Instruction**
 * @param instructionsCulture Used to display instructions for view type **Instruction** in second language
 * @param isSummary Flag to decide whether a particular field needs to be displayed in summary screens
 * @param maxLines Maximum number of lines for **EditText** view type.
 * @param minValue Minimum value validation for **EditText** view type, systolic and diastolic validation for **BP** view type.
 * @param minValueForHour Minimum hour validation for **TimeView** view type.
 * @param minValueForMinute Minimum minute validation for **TimeView** view type.
 * @param maxValue Maximum value validation for **EditText** view type, systolic and diastolic validation for **BP** view type.
 * @param maxValueForHour Maximum hour validation for **TimeView** view type.
 * @param maxValueForMinute Maximum minute validation for **TimeView** view type.
 * @param pulseMinValue Minimum value validation for pulse validation for **BP** view type.
 * @param pulseMaxValue Maximum value validation for pulse validation for **BP** view type.
 * @param showPulse When explicitly false, hides the pulse column in the **BP** view type. Defaults to showing pulse.
 * @param totalCount Total number of BP readings to take view type **BP**.
 * @param contentLength Max length for input fields for **EditText** view type. If contentLength is given it precedes [maxLength].
 * @param localDataCache Key for fetching options from local DB for view types **Spinner**, **MentalHealthView**, **RadioGroup**.
 * @param dependentID ID of field which is dependent on current field in case of view type is **Spinner**. E.g, Sub Villages is dependent on Swasthya Sebika.
 * @param unitMeasurement Used to display title with unit measurement like gm, cm etc. for  view types **EditText**, **Age**, **EditTextArea**, **InformationLabel**.
 * For view type **InformationLabel**, it is used to display the unit measurement in the key with title. If view type is **EditText**, it is getting stored in resultmap with key **id+Unit**
 * @param isEditable Used in case of NCD flows mostly. If the field is editable, then the field gets rendered in NCD edit flow.
 * @param isCustomWorkflow TODO
 * @param optionType Used to decide option type for view type **EditText**. Options can be from [org.medtroniclabs.uhis.formgeneration.config.EditTextOptionType]
 * @param mandatoryCount Mandatory number of BP readings to take for view type **BP**.
 * @param titleCulture Title for the particular field in second language
 * @param hintCulture Hint for the particular field in second language
 * @param disableSpinner Whether to disable spinner for view type **Spinner**.
 * @param startsWith Ideally used for phone number fields to check, whether the given phone number starting with the given list or not.
 * @param onlyAlphabets Used for validating the field whether the value is only alphabet or not for view type **EditText**.
 * @param applyDecimalFilter Used for applying decimal filter for view type **EditText**, **EditTextArea**.
 * @param applyTwoDigitPrecision Used for applying decimal filter max up-to 2 digits for view type **EditText**, **EditTextArea**.
 * @param backgroundColor Background color for view type **InformationLabel**
 * @param information Info for view type **NoOfDaysView**.
 * @param titleSummary Used for showing title in summary screen
 * @param noOfDays TODO
 * @param informationVisibility Should be one of [org.medtroniclabs.uhis.formgeneration.config.DefinedParams.VISIBLE], [org.medtroniclabs.uhis.formgeneration.config.DefinedParams.INVISIBLE], [org.medtroniclabs.uhis.formgeneration.config.DefinedParams.GONE] to display info [information] for view type **NoOfDaysView**.
 * @param isInfo Should be one of [org.medtroniclabs.uhis.formgeneration.config.DefinedParams.VISIBLE], [org.medtroniclabs.uhis.formgeneration.config.DefinedParams.INVISIBLE], [org.medtroniclabs.uhis.formgeneration.config.DefinedParams.GONE] to display info [infoTitle] / [infoTitle] for view types **EditText**, **NoOfDaysView**, **SingleSelectionView**, **Spinner**. If nothing provided default is gone.
 * @param dosageListItems TODO
 * @param maxDecimalPlaces Maximum number of decimal filter for view type **EditText**, **EditTextArea**. If [applyDecimalFilter] or [applyTwoDigitPrecision] are given then that precedes.
 * @param menstrualPeriod Needed to calculate minimum date for view type **DatePicker**. If this is true, then the min date for date picker is calculated based today's date - 287 days
 * @param minDays Needed to calculate minimum date for view type **DatePicker**. If > 0, then minimum date for the date picker will be today date - [minDays]. If [menstrualPeriod] is true, then that precedes.
 * @param maxDays Used in [org.medtroniclabs.uhis.ui.medicalreview.investigation.InvestigationGenerator] to calculate maximum date for view type **DatePicker**. If [maxDate] is not given and this is > 0, then the maximum date for the date picker will be today date + [maxDays].
 * @param unitList Used in [org.medtroniclabs.uhis.ui.medicalreview.investigation.InvestigationGenerator] for choosing unit for given value for view type **EditText**.
 * @param maxAge Used to validate date of birth fields. If given and the selected date of birth crosses max age, then displays error message.
 * @param code TODO
 * @param url TODO
 * @param resource TODO
 * @param spinnerAsObject If true then stored result for view type **Spinner** will be whole selected map from [optionsList], otherwise only ID is getting stored in the result.
 * @param enableSingleSelection If given then whether to enable selection for view type **SingleSelectionView**, **TimeView**.
 * @param ranges TODO
 * @param category TODO
 * @param textLabelColor If valid color given, then it applies the text color to the label/title for view type **TextLabel**.
 * @param textLabelStyle If given, then it applies text style to the label/title for view type **TextLabel**. One of [org.medtroniclabs.uhis.common.DefinedParams.BOLD], [org.medtroniclabs.uhis.common.DefinedParams.ITALIC], , [org.medtroniclabs.uhis.common.DefinedParams.BOLD_ITALIC]
 * @param familyOrder A unique number for each of the family (**CardView**)
 * @param ageCondition TODO
 * @param workflowType TODO
 * @param orderId A unique number for each of the field.
 * @param customizedWorkflowId TODO
 * @param infoTitle Info title for view types **EditText**, **NoOfDaysView**, **SingleSelectionView**, **Spinner**. If [isInfo] is not given then this field is of no use.
 * @param infoTitleCulture Info title in second language for view types **EditText**, **NoOfDaysView**, **SingleSelectionView**, **Spinner**. If [isInfo] is not given then this field is of no use.
 * @param hideDob Boolean for whether to hide date picker for view type **AgeYMD**.
 * @param naValue A value the user can enter when the field is not applicable or cannot be measured. If the input matches [naValue], it is treated as a default value and excluded from [minValue] and [maxValue] validation.
 */
data class FormLayout(
    override val viewType: String,
    override val id: String,
    override val title: String,
    var family: String? = null,
    var visibility: String?,
    var isMandatory: Boolean = false,
    var displayAsterisk: Boolean = false,
    var maxLength: Int? = null,
    var minLength: Int? = null,
    var inputType: Int? = null,
    var orientation: Int? = null,
    var errorMessage: String? = null,
    var cultureErrorMessage: String? = null,
    var disableFutureDate: Boolean? = null,
    var minDate: Long? = null,
    var maxDate: Long? = null,
    var optionsList: ArrayList<Map<String, Any>>?,
    var dayOptionsList: ArrayList<Map<String, Any>>? = null,
    var timeOptionsList: ArrayList<Map<String, Any>>? = null,
    var isEnabled: Boolean? = null,
    var defaultValue: String? = null,
    var condition: ArrayList<ConditionalModel>? = null,
    var hint: String? = null,
    var isNeedAction: Boolean = false,
    var instructions: ArrayList<String>? = null,
    var instructionsCulture: ArrayList<String>? = null,
    var isSummary: Boolean? = null,
    var maxLines: Int? = null,
    var minValue: Double? = null,
    var minValueForHour: Int? = null,
    var minValueForMinute: Int? = null,
    var maxValue: Double? = null,
    var maxValueForHour: Int? = null,
    var maxValueForMinute: Int? = null,
    var pulseMinValue: Double? = null,
    var pulseMaxValue: Double? = null,
    var showPulse: Boolean? = null,
    var totalCount: Int? = null,
    var contentLength: Int? = null,
    var localDataCache: String? = null,
    var dependentID: String? = null,
    var unitMeasurement: String? = null,
    var isEditable: Boolean = false,
    var isCustomWorkflow: Boolean = false,
    var optionType: String? = null,
    var mandatoryCount: Int? = null,
    var titleCulture: String? = null,
    var hintCulture: String? = null,
    var disableSpinner: Boolean? = null,
    var startsWith: ArrayList<String>? = null,
    var onlyAlphabets: Boolean? = null,
    var applyDecimalFilter: Boolean? = null,
    var applyTwoDigitPrecision: Boolean? = null,
    var backgroundColor: String? = null,
    var information: String? = null,
    var titleSummary: String? = null,
    var titleSummaryCulture: String? = null,
    var noOfDays: Int? = null,
    var informationVisibility: String? = null,
    var isInfo: String? = null,
    var dosageListItems: ArrayList<RecommendedDosageListModel>? = null,
    var maxDecimalPlaces: Int? = null,
    var menstrualPeriod: Boolean = false,
    var minDays: Int? = null,
    var maxDays: Int? = null,
    var unitList: ArrayList<Map<String, Any>>? = null,
    var maxAge: Int? = null,
    var code: String? = null,
    var url: String? = null,
    var resource: String? = null,
    var spinnerAsObject: Boolean = false,
    var dependentIDList: ArrayList<String>? = null,
    var enableSingleSelection: Boolean? = null,
    var ranges: ArrayList<RangeModel>? = null,
    var category: List<String>? = null,
    var textLabelColor: String? = null,
    var textLabelStyle: String? = null,
    var familyOrder: Int? = null,
    var ageCondition: ArrayList<String>? = null,
    var workflowType: ArrayList<String>? = null,
    var orderId: Int? = null,
    var customizedWorkflowId: Double? = null,
    var titles: ArrayList<TitleModel>? = null,
    var infoTitle: String? = null,
    var infoTitleCulture: String? = null,
    var multipleParents: Map<String, Any>? = null,
    var hideDob: Boolean? = null,
    var naValue: Double? = null,
) : BaseViewParams {
    /**
     * Returns info title
     *
     * - If translate is enabled && [infoTitleCulture] is not null then returns [infoTitleCulture]
     * - If translate is enabled && [infoTitleCulture] is null and [infoTitle] is not null then returns [infoTitle]
     * - If translate is not enabled and [infoTitle] is not null then returns [infoTitle]
     * - Otherwise [defaultValue]
     */
    fun getInfoTitle(
        translate: Boolean,
        defaultValue: String?,
    ): String? {
        val title = if (translate) {
            infoTitleCulture ?: infoTitle
        } else {
            infoTitle
        }
        return title ?: defaultValue
    }

    fun getSummaryTitle(translate: Boolean): String? =
        if (!titleSummary.isNullOrBlank()) {
            titleSummary
        } else if (translate && !titleCulture.isNullOrBlank()) {
            titleCulture
        } else {
            title
        }
}

data class RangeModel(
    val unitType: String,
    val gender: String,
    val minRange: Double,
    val maxRange: Double,
    val displayRange: String,
)

data class FormResponse(
    var formLayout: List<FormLayout>, var time: Long,
)

data class BPModel(
    var systolic: Double? = null,
    var diastolic: Double? = null,
    var pulse: Double? = null,
)
