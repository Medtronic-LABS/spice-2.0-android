package com.medtroniclabs.opensource.common

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.text.SpannableStringBuilder
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.nullIfEmpty
import com.medtroniclabs.opensource.common.DateUtils.calculateAge
import com.medtroniclabs.opensource.common.RoleConstant.CHA
import com.medtroniclabs.opensource.common.RoleConstant.COMMUNITY_HEALTH_ASSISTANT
import com.medtroniclabs.opensource.common.RoleConstant.COMMUNITY_HEALTH_PROMOTER
import com.medtroniclabs.opensource.common.RoleConstant.COMMUNITY_HEALTH_WORKER
import com.medtroniclabs.opensource.common.RoleConstant.HEALTH_SCREENER
import com.medtroniclabs.opensource.common.RoleConstant.LAB_ASSISTANT
import com.medtroniclabs.opensource.common.RoleConstant.MCHA
import com.medtroniclabs.opensource.common.RoleConstant.MID_WIFE
import com.medtroniclabs.opensource.common.RoleConstant.PHARMACIST
import com.medtroniclabs.opensource.common.RoleConstant.PHYSICIAN_PRESCRIBER
import com.medtroniclabs.opensource.common.RoleConstant.PROVIDER
import com.medtroniclabs.opensource.common.RoleConstant.SECHN
import com.medtroniclabs.opensource.common.RoleConstant.SRN
import com.medtroniclabs.opensource.data.ErrorResponse
import com.medtroniclabs.opensource.data.Prescription
import com.medtroniclabs.opensource.data.history.Investigation
import com.medtroniclabs.opensource.db.entity.RiskClassificationModel
import com.medtroniclabs.opensource.db.entity.RiskFactorModel
import com.medtroniclabs.opensource.db.entity.VillageEntity
import com.medtroniclabs.opensource.formgeneration.config.DefinedParams.DAY
import com.medtroniclabs.opensource.formgeneration.config.DefinedParams.DAYS
import com.medtroniclabs.opensource.formgeneration.config.DefinedParams.GONE
import com.medtroniclabs.opensource.formgeneration.config.DefinedParams.MONTH
import com.medtroniclabs.opensource.formgeneration.config.DefinedParams.MONTHS
import com.medtroniclabs.opensource.formgeneration.config.DefinedParams.VISIBLE
import com.medtroniclabs.opensource.formgeneration.config.DefinedParams.WEEK
import com.medtroniclabs.opensource.formgeneration.config.DefinedParams.WEEKS
import com.medtroniclabs.opensource.formgeneration.config.DefinedParams.YEARS
import com.medtroniclabs.opensource.formgeneration.extension.capitalizeFirstChar
import com.medtroniclabs.opensource.formgeneration.model.BPModel
import com.medtroniclabs.opensource.formgeneration.model.FormLayout
import com.medtroniclabs.opensource.mappingkey.HouseHoldRegistration
import com.medtroniclabs.opensource.mappingkey.Screening
import com.medtroniclabs.opensource.mappingkey.Screening.CAGEAID
import com.medtroniclabs.opensource.mappingkey.Screening.CategoryDisplayName
import com.medtroniclabs.opensource.mappingkey.Screening.CategoryDisplayType
import com.medtroniclabs.opensource.mappingkey.Screening.CategoryType
import com.medtroniclabs.opensource.mappingkey.Screening.Female
import com.medtroniclabs.opensource.mappingkey.Screening.MentalHealthDetails
import com.medtroniclabs.opensource.mappingkey.Screening.PHQ4
import com.medtroniclabs.opensource.mappingkey.Screening.RiskLevel
import com.medtroniclabs.opensource.mappingkey.Screening.SiteName
import com.medtroniclabs.opensource.mappingkey.Screening.Type
import com.medtroniclabs.opensource.mappingkey.Screening.lastMealTime
import com.medtroniclabs.opensource.mappingkey.Screening.lastMealTypeDateSuffix
import com.medtroniclabs.opensource.mappingkey.Screening.lastMealTypeMeridiem
import com.medtroniclabs.opensource.mappingkey.Screening.mentalHealthScore
import com.medtroniclabs.opensource.mappingkey.Screening.otherType
import com.medtroniclabs.opensource.mappingkey.Screening.siteId
import com.medtroniclabs.opensource.mappingkey.Screening.substanceAbuse
import com.medtroniclabs.opensource.mappingkey.Screening.userSiteId
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil
import com.medtroniclabs.opensource.ncd.screening.utils.ReferredReason
import com.medtroniclabs.opensource.ui.MenuConstants
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewTypeEnums
import okhttp3.ResponseBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

object CommonUtils {
    fun checkIsTablet(context: Context): Boolean {
        val res = context.resources?.getBoolean(R.bool.isTablet)
        return res ?: false
    }

    fun getStringFromAssets(fileName: String, assets: AssetManager): String {
        return assets.open(fileName).bufferedReader().use { it.readText() }
    }

    fun getIntegerOrNull(answer: Any?): Int? {
        return if (answer is Int) {
            answer
        } else if (answer is String) {
            val answerNumber = answer.toIntOrNull()
            if (answerNumber is Int) {
                answerNumber
            } else {
                null
            }
        } else {
            null
        }
    }

    fun getLongOrNull(answer: Any?): Long? {
        when (answer) {
            is Long -> return answer
            is String -> {
                val answerNumber = answer.toLongOrNull()
                return if (answerNumber is Long) {
                    answerNumber
                } else {
                    null
                }
            }

            is Double -> {
                return answer.toLong()
            }

            else -> {
                return null
            }
        }
    }

    fun getStringOrEmptyString(answer: Any?): String {
        return if (answer is String) {
            answer
        } else ""
    }

    fun getIsBooleanFromString(answer: Any?): Boolean {
        return (answer is String) && answer.equals(HouseHoldRegistration.yes, true)
    }

    fun displayAge(dobString: String, context: Context): String {
        val yearMonthWeek = DateUtils.getV2YearMonthAndWeek(dobString)
        val years = yearMonthWeek.years
        var months = yearMonthWeek.months
        val weeks = yearMonthWeek.weeks
        val days = yearMonthWeek.days

        val strBuilder = StringBuilder()

        if (months == 0 && years == 0 && weeks == 0) {
            if (days > 1) {
                strBuilder.append("$days $DAYS")
                strBuilder.append(" ")
            } else {
                strBuilder.append("$days $DAY")
                strBuilder.append(" ")
            }
            return strBuilder.toString()
        }


        if (years < 5) {
            months += (years * 12)
        } else {
            strBuilder.append("$years $YEARS ")
            strBuilder.append(" ")
        }

        if (months > 0) {
            if (months == 1) {
                strBuilder.append("$months $MONTH")
                strBuilder.append(" ")
            } else {
                strBuilder.append("$months $MONTHS")
                strBuilder.append(" ")
            }
        }

        if (weeks > 0) {
            if (weeks == 1) {
                strBuilder.append("$weeks $WEEK")
                strBuilder.append(" ")
            } else {
                strBuilder.append("$weeks $WEEKS")
                strBuilder.append(" ")
            }
        }

        return strBuilder.toString()
    }


    fun convertListToString(dispensedList: ArrayList<String>): String {
        return dispensedList.nullIfEmpty()
            ?.joinToString(separator = ", ") { it.capitalizeFirstChar() } ?: "-"
    }

    fun getYearMonthAndWeeks(dateString: String): Triple<String, String, String> {
        val parts = dateString.split("/")
        return Triple(parts[0], parts[1], parts[2])
    }

    fun getDurationInYMD(input: String, context: Context): String {
        val parts = input.split('/')

        return when {
            parts[0] == DefinedParams.ZERO && parts[1] == DefinedParams.ZERO -> context.getString(
                R.string.weeks_w,
                parts[2]
            )

            parts[0] == DefinedParams.ZERO -> context.getString(R.string.months_m, parts[1])
            else -> context.getString(R.string.years_y, parts[0])
        }
    }

    fun isChw(): Boolean {
        val userRole = SecuredPreference.getUserDetails()?.roles?.joinToString { it.name }
        if (userRole != null) {
            return userRole.contains(COMMUNITY_HEALTH_WORKER)
        }
        return false
    }

    fun offlineUsers(): Boolean {
        return isNonCommunity() || (isCommunity() && isChw())
    }

    fun isChwChp(): Boolean {
        val userRole = SecuredPreference.getUserDetails()?.roles?.joinToString { it.name }
        return userRole?.contains(COMMUNITY_HEALTH_WORKER) == true || userRole?.contains(RoleConstant.COMMUNITY_HEALTH_PROVIDER) == true
    }

    fun isProvider(): Boolean {
        val userRole = SecuredPreference.getUserDetails()?.roles?.joinToString { it.name }
        if (userRole != null) {
            return userRole.contains(PROVIDER)
        }
        return false
    }

    fun isRolePresent(): Boolean {
        val roleList =
            listOf(SECHN, MCHA, PROVIDER, CHA, MID_WIFE, LAB_ASSISTANT, SRN).map { it.lowercase() }
        val currentRole = SecuredPreference.getRole()?.lowercase()
        return roleList.contains(currentRole)
    }


    fun getOptionMap(value: String, name: String, culture: String? = null): Map<String, Any> {
        val map = HashMap<String, Any>()
        map[DefinedParams.ID] = value
        map[DefinedParams.NAME] = name
        culture?.let { map[DefinedParams.CULTURE_VALUE] = it }
        return map
    }

    fun getOptions(value: String, name: String): Map<String, Any> {
        val map = HashMap<String, Any>()
        map[DefinedParams.Value] = value
        map[DefinedParams.NAME] = name
        return map
    }

    fun getAgeFromDOB(dateOfBirth: String?, context: Context): String {
        if (dateOfBirth != null) {
            val yearMonthWeek = DateUtils.getV2YearMonthAndWeek(dateOfBirth)

            if (yearMonthWeek.years >= 5) {
                return "${yearMonthWeek.years}"
            }

            val months = (yearMonthWeek.years * 12) + yearMonthWeek.months
            if (months > 0) {
                return if (months == 1) "$months ${context.getString(R.string.month)}" else "$months ${
                    context.getString(
                        R.string.months
                    )
                }"
            }

            if (yearMonthWeek.weeks > 0) {
                return if (yearMonthWeek.weeks == 1) "${yearMonthWeek.weeks} ${context.getString(R.string.week)}" else "${yearMonthWeek.weeks} ${
                    context.getString(
                        R.string.weeks
                    )
                }"
            }

            if (yearMonthWeek.days >= 0) {
                return if (yearMonthWeek.days < 2) "${yearMonthWeek.days} ${context.getString(R.string.day)}" else "${yearMonthWeek.days} ${
                    context.getString(
                        R.string.days
                    )
                }"
            }
        }
        return ""
    }

    fun getAgeFromDob(dateOfBirth: String?, month: String): String {
        if (dateOfBirth != null) {
            val age = calculateAge(dateOfBirth)
            return if (age != null && age > 5) {
                "$age"
            } else {
                val startDate = DateUtils.formatStringToDate(
                    dateOfBirth, SimpleDateFormat(
                        DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                        Locale.ENGLISH
                    )
                )
                startDate?.let { date ->
                    "${DateUtils.calculateAgeInMonths(date)} $month"
                } ?: kotlin.run {
                    return ""
                }
            }
        } else {
            return ""
        }
    }


    private fun calculateAge(dateOfBirth: String): Int? {
        val ageTriplet = DateUtils.getYearMonthAndDate(
            dateOfBirth, SimpleDateFormat(
                DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                Locale.ENGLISH
            )
        )
        val year = ageTriplet.first
        return year?.let { calculateAge(it) }
    }

    fun getGenderText(gender: String?, context: Context): String {
        return if (gender.equals(DefinedParams.male, true)) {
            context.getString(R.string.male_prefix)
        } else {
            context.getString(R.string.female_prefix)
        }
    }

    fun translatedGender(context: Context, gender: String?): String {
        return if (gender.equals(DefinedParams.male, true)) {
            context.getString(R.string.male)
        } else {
            context.getString(R.string.female)
        }
    }

    fun getBooleanAsString(value: Boolean): String {
        return if (value) HouseHoldRegistration.yes else HouseHoldRegistration.no
    }


    fun getBMI(heightInCM: Double, weight: Double, context: Context): String {
        val heightInMeter = heightInCM / 100
        val bmi = weight / (heightInMeter * heightInMeter)
        if (bmi.isInfinite() || bmi.isNaN()) {
            return context.getString(R.string.hyphen_symbol)
        }
        return getDecimalFormatted(bmi)
    }

    fun convertStringDobToMonths(dateOfBirth: String): Int? {
        val startDate = DateUtils.formatStringToDate(
            dateOfBirth, SimpleDateFormat(
                DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                Locale.ENGLISH
            )
        )
        return startDate?.let { date ->
            DateUtils.calculateAgeInMonths(date)
        }
    }

    fun isAlphabetsWithSpace(input: String): Boolean {
        return input.matches(Regex(RegexConstants.Contains_Alphabets_Space))
    }

    fun getDaysValue(enteredDays: String, maxDays: Int, context: Context): String {
        return try {
            val days = enteredDays.toDouble().toInt()
            if (days > maxDays) {
                context.getString(R.string.days_summary, days, maxDays + 1)
            } else {
                days.toString()
            }
        } catch (e: NumberFormatException) {
            enteredDays
        }
    }

    fun convertStringToIntString(value: String): String {
        return try {
            return value.toDouble().toInt().toString()
        } catch (e: NumberFormatException) {
            value
        }
    }

    fun getDecimalFormatted(value: Any?, pattern: String = "###.##"): String {
        var formattedValue = ""
        try {
            value?.let {
                val actualValue = if (it is String) it.toDoubleOrNull() ?: "" else it
                val df = DecimalFormat(pattern, DecimalFormatSymbols(Locale.ENGLISH))
                df.roundingMode = RoundingMode.FLOOR
                if (actualValue is String) {
                    if (actualValue.isNotBlank())
                        formattedValue = df.format(actualValue)
                } else
                    formattedValue = df.format(actualValue)
            }
        } catch (_: Exception) {
            //Exception - Catch block
        }
        return formattedValue
    }

    fun Double?.toIntOrEmptyString(): String {
        return this?.toInt().toString()
    }

    fun Double?.toDoubleOrEmptyString(): String {
        return if (this != null) {
            if (this == this.toInt().toDouble()) {
                this.toInt().toString()
            } else {
                this.toString()
            }
        } else {
            ""
        }
    }

    fun getFilePath(id: String, context: Context): File {
        val cw = ContextWrapper(context)
        val directory = cw.getDir(id, Context.MODE_PRIVATE)
        return File(directory, DefinedParams.SIGN_DIR)
    }

    fun getTicketType(menuType: String): String? {
        return when (menuType) {
            MedicalReviewTypeEnums.ABOVE_FIVE_YEARS.name, MedicalReviewTypeEnums.UNDER_TWO_MONTHS.name, MedicalReviewTypeEnums.UNDER_FIVE_YEARS.name -> {
                MedicalReviewTypeEnums.ICCM.name
            }

            MedicalReviewTypeEnums.ANC_REVIEW.name, MedicalReviewTypeEnums.PNC_MOTHER_REVIEW.name, MedicalReviewTypeEnums.MOTHER_DELIVERY_REVIEW.name -> {
                MedicalReviewTypeEnums.RMNCH.name
            }

            else -> {
                null
            }
        }
    }

    fun createPrescription(prescriptions: List<Prescription>?, context: Context): String? {
        return prescriptions?.takeIf { it.isNotEmpty() }?.mapIndexed { index, prescription ->
            "${index + 1}. ${prescription.medicationName} / ${prescription.frequencyName} / ${
                dayPeriod(
                    prescription.prescribedDays,
                    context
                )
            }"
        }?.joinToString("\n")
    }

    fun createInvestigation(investigation: List<Investigation>?, context: Context): String? {
        return investigation?.takeIf { it.isNotEmpty() }?.mapIndexed { index, investigation ->
            "${index + 1}. ${investigation.testName} "
        }?.joinToString("\n")
    }

    fun createMotherNeonateExamination(
        prescriptions: List<HashMap<String, Pair<String?, Any?>>>,
        context: Context,
        type: Boolean
    ): String? {
        val maxKeyLength = prescriptions.flatMap { it.keys }.maxOfOrNull { it.length} ?: 0
        var formattedFirst=""
        return prescriptions.takeIf { it.isNotEmpty() }?.mapIndexed { index, prescription ->
            prescription.entries.joinToString("\n") { (key, pair) ->
                formattedFirst = if (key.length==maxKeyLength && type){
                    key.plus(":").padEnd(maxKeyLength)
                }else if(!type){
                    key.plus(": ").padEnd(maxKeyLength)
                } else {
                    key.plus(":").padEnd((maxKeyLength+6).toInt())
                }
                val formattedPair = if (pair.second == null) {
                    "${index + 1}. $formattedFirst ${pair.first}"
                } else {
                    "${index + 1}. $formattedFirst ${pair.first}- (${pair.second})"
                }
                formattedPair
            }
        }?.joinToString("\n")
    }



    private fun dayPeriod(prescribedDays: Long?, context: Context): String {
        return if (prescribedDays == 1L) {
            "$prescribedDays ${context.getString(R.string.day)}"
        } else {
            "$prescribedDays ${context.getString(R.string.days)}"
        }
    }

    fun getContactNumber(phNumber: String?): String? {
        return SecuredPreference.getPhoneNumberCode()?.let { code ->
            val formattedCode = if (!code.startsWith("+")) "+$code" else code
            "$formattedCode $phNumber"
        }
    }


    fun getMaxDateLimit(menstrualPeriod: Boolean, minDays: Int?): Long? {
        return if (menstrualPeriod) {
            DateUtils.calculateGestationPastMonths(System.currentTimeMillis(), 287)
        } else {
            if (minDays != null ) {
                if (minDays > 0){
                    val calendar = Calendar.getInstance()
                    calendar.add(Calendar.DAY_OF_MONTH, -minDays)
                    return calendar.timeInMillis
                }else {
                    return null
                }
            } else {
                return minDays
            }
        }
    }

    fun getMaxDateLimit(maxDays: Int?): Long? {
        if (maxDays != null) {
            if (maxDays > 0) {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_MONTH, maxDays)
                return calendar.timeInMillis
            } else {
                return null
            }
        } else {
            return maxDays
        }
    }

    fun getMaxDateLimit(menstrualPeriod: Boolean, minDays: Long?): Long? {
        return if (menstrualPeriod) {
            DateUtils.calculateGestationPastMonths(System.currentTimeMillis(), 287)
        } else {
            if (minDays !=null && minDays > 0){
                return minDays
            }else{
                return null
            }
        }
    }

    fun convertAnyToString(value: Any?, context: Context): String {
        return when (value) {
            is String -> value
            is List<*> -> {
                if (value.all { it is String }) {
                    (value as List<String>).joinToString(", ")
                } else {
                    context.getString(R.string.separator_double_hyphen)
                }
            }
            is Boolean -> booleanToYesNo(value,context)
            null -> context.getString(R.string.separator_double_hyphen)
            else -> context.getString(R.string.separator_double_hyphen)
        }
    }

    private fun booleanToYesNo(value: Boolean, context: Context): String {
        return if (value) context.getString(R.string.yes) else context.getString(R.string.no)
    }

    fun convertAnyToListOfString(value: Any?): List<String?> {
        return when (value) {
            is String -> listOf(value)
            is List<*> -> value.filterIsInstance<String?>()
            else -> emptyList()
        }
    }

    fun combineText(
        items: List<String?>?,
        notes: String?,
        nullHandleString: String
    ): String {
        val combinedText = StringBuilder()
        items?.filterNotNull()?.takeIf { it.isNotEmpty() }?.joinToString(separator = ", ")?.let {
            combinedText.append(it)
        }
        if (!notes.isNullOrEmpty()) {
            if (combinedText.isNotEmpty()) {
                combinedText.append(" - ")
            }
            combinedText.append(notes)
        }
        return if (combinedText.isNotEmpty()) combinedText.toString() else nullHandleString
    }


    fun composeLabelName(name: String, status: String?, context: Context): String {
        return if (!(status.isNullOrEmpty())) {
            context.getString(R.string.patient_status_append, name, status.trim())
        } else {
            name
        }
    }

    fun extractNumber(input: String): Int {
        return input.split(" ").getOrNull(0)?.toIntOrNull() ?: 0
    }
    fun birthWeight(kg: Double, context: Context): String{
        val grams = (kg * 1000).toInt()
        return when {
            grams < 1000 -> context.getString(R.string.elbw)
            grams < 1500 -> context.getString(R.string.vlbw)
            grams < 2500 -> context.getString(R.string.lbw)
            grams in 2500..4000 ->  context.getString(R.string.nbw)
            grams > 4000 ->  context.getString(R.string.hbw)
            else -> ""
        }
    }

    fun isMandateOrNot(dateOfBirth: String): String {
        val yearMonthWeek = DateUtils.getV2YearMonthAndWeek(dateOfBirth)
        val months = (yearMonthWeek.years * 12) + yearMonthWeek.months

        if (months >= 6)
            return VISIBLE
        else
            return GONE
    }

    fun formatConsent(consent: String): String {
        return consent.replace("\\\"", "\"").replace("contenteditable=\"true\"", "")
    }

    fun calculateBMI(map: HashMap<String, Any>): Double? {
        if (map.containsKey(Screening.Weight) &&
            map.containsKey(Screening.Height)
        ) {
            var height: Double? = null
            val weight = fetchValue(map, Screening.Weight)
            if (map[Screening.Height] is Map<*, *>) {
                val heightMap = map[Screening.Height] as Map<*, *>
                if (heightMap.containsKey(Screening.Feet) && heightMap.containsKey(Screening.Inches)) {
                    val feet = heightMap[Screening.Feet] as Double
                    val inches = heightMap[Screening.Inches] as Double
                    height = (feet * 12) + inches
                }
            } else {
                height = fetchValue(map, Screening.Height)
            }
            height?.let {
                val bmiValue = getBMIForNcd(it, weight)?.toDoubleOrNull()
                val formattedValue = String.format(Locale.US, "%.2f", bmiValue).toDouble()
                map[Screening.BMI] = formattedValue
                return formattedValue
            }
        } else {
            map.remove(Screening.BMI)
        }
        return null
    }

    private fun fetchValue(map: HashMap<String, Any>, params: String): Double {
        return if (map[params] is String)
            (map[params] as String).toDouble()
        else
            map[params] as Double
    }

    fun calculateAverageBloodPressure(
        resultMap: HashMap<String, Any>,
        addDateTime: Boolean = false
    ) {
        if (resultMap.containsKey(Screening.BPLog_Details)) {
            val actualMapList = resultMap[Screening.BPLog_Details]
            if (actualMapList is ArrayList<*>) {
                var systolic = 0.0
                var diastolic = 0.0
                var enteredBGCount = 0
                actualMapList.forEach { map ->
                    val sys = getSystolicValue(map)
                    val dia = getDiastolicValue(map)
                    if (sys > 0 && dia > 0) {
                        enteredBGCount++
                        systolic += sys
                        diastolic += dia
                    }
                }
                val finalSys = (systolic / enteredBGCount).roundToInt()
                val finalDia = (diastolic / enteredBGCount).roundToInt()
                resultMap[Screening.Avg_Systolic] = finalSys
                resultMap[Screening.Avg_Diastolic] = finalDia
                if (addDateTime)
                    resultMap[Screening.BPTakenOn] = DateUtils.getTodayDateDDMMYYYY()
                resultMap[Screening.Avg_Blood_pressure] = ("$finalSys/$finalDia")
                resultMap[Screening.BPLog_Details] = parsedList(actualMapList)
            }
        }
    }

    private fun parsedList(actualList: ArrayList<*>): ArrayList<*> {
        try {
            val modifiedList = ArrayList<HashMap<String, String>>()
            actualList.forEachIndexed { _, any ->
                (any as? BPModel?)?.let { record ->
                    val data = HashMap<String, String>()
                    record.systolic?.let { sys ->
                        data[Screening.Systolic] = parseDouble(sys)
                    }
                    record.diastolic?.let { dia ->
                        data[Screening.Diastolic] = parseDouble(dia)
                    }
                    record.pulse?.let { pul ->
                        data[Screening.Pulse] = parseDouble(pul)
                    }
                    if (data.isNotEmpty())
                        modifiedList.add(data)
                }
            }
            return modifiedList.ifEmpty { actualList }
        } catch (_: Exception) {
            return actualList
        }
    }

    fun parseDouble(dValue: Double): String {
        return dValue.toString().replace(".0", "")
    }

    private fun getSystolicValue(map: Any?): Double {
        var returnValue = 0.0
        if (map is Map<*, *> && map.containsKey(Screening.Systolic))
            returnValue = map[Screening.Systolic] as Double
        else if (map is BPModel)
            map.systolic?.let {
                returnValue = it
            }
        return returnValue
    }

    private fun getDiastolicValue(map: Any?): Double {
        var returnValue = 0.0
        if (map is Map<*, *> && map.containsKey(Screening.Diastolic))
            returnValue = map[Screening.Diastolic] as Double
        else if (map is BPModel)
            map.diastolic?.let {
                returnValue = it
            }
        return returnValue
    }

    fun mentalHealthKey(type: String): String {
        var key = Screening.MentalHealthDetails
        if (type.equals(AssessmentDefinedParams.PHQ9, true))
            key = AssessmentDefinedParams.PHQ9_Mental_Health
        else if (type.equals(AssessmentDefinedParams.GAD7,true))
            key = AssessmentDefinedParams.GAD7_Mental_Health
        return key
    }

    fun calculatePHQScore(
        map: HashMap<String, Any>,
        type: String = PHQ4
    ): Int {
        val key = mentalHealthKey(type)
        if (map.containsKey(key)) {
            val phqMap = ArrayList<HashMap<String, Any>>()
            var phqScore = 0
            val mentalHealthResultMap = map[key]
            if (mentalHealthResultMap is Map<*, *>) {
                mentalHealthResultMap.keys.forEach { mapKey ->
                    val optionsMap = HashMap<String, Any>()
                    val actualValue = mentalHealthResultMap[mapKey]
                    optionsMap[Screening.MHQuestion] = mapKey as String
                    if (actualValue is HashMap<*, *>) {
                        (actualValue[mentalHealthScore] as? Double)?.toInt()?.let {
                            phqScore += it
                            optionsMap[mentalHealthScore] = it
                        }
                        optionsMap[Screening.Question_Id] =
                            actualValue[Screening.Question_Id] as Long
                        optionsMap[Screening.Answer_Id] =
                            actualValue[Screening.Answer_Id] as Long
                        optionsMap[Screening.MHAnswer] =
                            actualValue[Screening.MHAnswer] as String
                        optionsMap[Screening.Display_Order] =
                            actualValue[Screening.Display_Order] as Long
                        phqMap.add(optionsMap)
                    }
                }
            }
            applyScores(type, map, phqMap, phqScore)
            return phqScore
        }
        return 0
    }

    private fun applyScores(
        type: String,
        map: HashMap<String, Any>,
        phqMap: ArrayList<HashMap<String, Any>>,
        phqScore: Int
    ) {
        when (type) {
            PHQ4 -> {
                map[Screening.PHQ4_Score] = phqScore
                map[Screening.RiskLevel] = getPhQ4RiskLevel(phqScore)
                map[Screening.MentalHealthDetails] = phqMap
            }
            AssessmentDefinedParams.PHQ9 -> {
                map[AssessmentDefinedParams.PHQ9_Score] = phqScore
                map[AssessmentDefinedParams.PHQ9_Risk_Level] = getPhQ4RiskLevel(phqScore)
                map[AssessmentDefinedParams.PHQ9_Mental_Health] = phqMap
                if (map.containsKey(Screening.MentalHealthDetails))
                    map.remove(Screening.MentalHealthDetails)
            }

            AssessmentDefinedParams.GAD7 -> {
                map[AssessmentDefinedParams.GAD7_Score] = phqScore
                map[AssessmentDefinedParams.GAD7_Risk_Level] = getPhQ4RiskLevel(phqScore)
                map[AssessmentDefinedParams.GAD7_Mental_Health] = phqMap
                if (map.containsKey(Screening.MentalHealthDetails))
                    map.remove(Screening.MentalHealthDetails)
            }

        }
    }

    private fun getPhQ4RiskLevel(phq4Score: Int): String {
        return when (phq4Score) {
            4, 5 -> Screening.Mild
            6, 7, 8 -> Screening.Moderate
            0, 1, 2, 3 -> Screening.Normal
            else -> Screening.Severe
        }
    }

    fun calculateSuicidalIdeation(map: HashMap<String, Any>) {
        if (map.containsKey(Screening.SuicidalIdeationQuestion)) {
            val actual = map[Screening.SuicidalIdeationQuestion]
            if (actual is String) {
                map[Screening.SuicidalIdeation] = actual
            }
        }
    }

    fun calculateCAGEAIDSCore(map: HashMap<String, Any>, serverData: List<FormLayout?>?) {
        var cageAid = 0
        if (serverData?.any { it?.id == substanceAbuse || it?.family == substanceAbuse} != true) {
            return
        }
        serverData?.let { dataList ->
            val substanceAbuseList = dataList.filter { it != null && it.family == substanceAbuse }
            substanceAbuseList.forEach { formData ->
                formData?.let { data ->
                    if (map.containsKey(data.id)) {
                        val actualValue = map[data.id]
                        if (actualValue != null && actualValue is String && actualValue.equals(
                                DefinedParams.Yes,
                                true
                            )
                        ) {
                            cageAid += 1
                        }
                    }
                }
            }
        }

        if (cageAid > 0) {
            cageAid -= 1
            map[CAGEAID] = cageAid
        } else {
            map[CAGEAID] = 0
        }
    }

    fun getMeasurementTypeValues(map: HashMap<String, Any>): String {
        val unitType = map[Screening.BloodGlucoseID + Screening.unitMeasurement_KEY]
        if (unitType is String) {
            return unitType
        }
        return Screening.mmoll
    }

    fun checkAssessmentCondition(
        systolicAverage: Int? = null,
        diastolicAverage: Int? = null,
        phQ4Score: Int? = null,
        glucoseValuePair: Pair<Double, Double>,
        unitGenericType: String,
        pregnantSymptoms: Int? = null,
        resultMapPair: Pair<Boolean?, HashMap<String, Any>>,
        serverData: List<FormLayout?>?
    ): Pair<Boolean, java.util.ArrayList<String>> {
        var status = false
        var cageAId = 0
        val referredReasonList = ArrayList<String>()
        if (resultMapPair.second.containsKey(CAGEAID)) {
            cageAId = (resultMapPair.second[CAGEAID] as? Int?) ?: 0
        }
        if ((systolicAverage ?: 0) > Screening.UpperLimitSystolic || (diastolicAverage
                ?: 0) > Screening.UpperLimitDiastolic
        ) {
            referredReasonList.add(ReferredReason.bloodPressure)
            status = true
        }
        if ((phQ4Score ?: 0) > 4) {
            referredReasonList.add(ReferredReason.PHQ4)
            status = true
        }
        if (unitGenericType == Screening.mgdl && (glucoseValuePair.first > Screening.FBSMaximumMGDlValue || glucoseValuePair.second >= Screening.RBSMaximumMGDlValue)) {
            referredReasonList.add(ReferredReason.bloodGlucose)
            status = true
        }

        if (glucoseValuePair.first > Screening.FBSMaximumValue || glucoseValuePair.second >= Screening.RBSMaximumValue) {
            referredReasonList.add(ReferredReason.bloodGlucose)
            status = true
        }
        if (pregnantSymptoms != null && pregnantSymptoms >= 1) {
            referredReasonList.add(ReferredReason.pregnancySymptoms)
            status = true
        }
        if (resultMapPair.second.containsKey(Screening.SuicidalIdeation) && (resultMapPair.second[Screening.SuicidalIdeation] as String).equals(DefinedParams.Yes, true)) {
            referredReasonList.add(ReferredReason.SuicidalIdeation)
            status = true
        }
        if (cageAId >= 2) {
            referredReasonList.add(ReferredReason.CAGEAID)
            status = true
        }

        val hivQuestionViews = serverData?.filter {
            it?.ageCondition?.isNotEmpty() == true && it.workflowType?.contains(ReferredReason.HIV) == true
        }?.map { it?.id }
        hivQuestionViews?.let { hivQuestionList ->
            val matchedItem = hivQuestionList.firstOrNull { item ->
                resultMapPair.second.containsKey(item) &&
                        resultMapPair.second[item] is String &&
                        (resultMapPair.second[item] as String).equals(DefinedParams.yes, true)
            }
            if (matchedItem != null) {
                status = true
                referredReasonList.add(ReferredReason.HIV)
            }
        }

        return Pair(status, referredReasonList)
    }


    fun calculateBloodGlucose(
        map: HashMap<String, Any>,
        removeUnwantedKeys: Boolean = false,
        addDateTime: Boolean = false,
        getRbsFbs: (Pair<Double?,Double?>) -> Unit
    ) {
        if (map.containsKey(Screening.BloodGlucoseID)) {
            val bloodGlucoseString = map[Screening.BloodGlucoseID]
            val bloodGlucoseValue: Double? = parseGlucoseValue(bloodGlucoseString)
            if (bloodGlucoseValue != null) {
                map[Screening.Glucose_Value] = bloodGlucoseValue
                if (removeUnwantedKeys)
                    map.keys.remove(Screening.BloodGlucoseID)
                formatLastMealTime(map)
                if (map.containsKey(lastMealTime) && map[lastMealTime] is String) {
                    setFBSAndRBSValues(map, bloodGlucoseValue, 1) {
                        getRbsFbs.invoke(it)
                    }
                } else {
                    setFBSAndRBSValues(map, bloodGlucoseValue, 2) {
                        getRbsFbs.invoke(it)
                    }
                }
                val dateTime = DateUtils.getTodayDateDDMMYYYY()
                map[Screening.Glucose_Date_Time] = dateTime
                if (addDateTime)
                    map[Screening.BGTakenOn] = dateTime
            }
        } else {
            formatLastMealTime(map)
        }
    }

    private fun formatLastMealTime(map: HashMap<String, Any>) {
        if (map["${lastMealTime}${lastMealTypeMeridiem}"] is String &&
            map["${lastMealTime}${lastMealTypeDateSuffix}"] is String &&
            map[lastMealTime] is Map<*, *>
        ) {
            val hourMinMap = map[lastMealTime] as? Map<*, *> ?: emptyMap<Any, Any>()
            val hour = (hourMinMap[Screening.Hour] as? String)?.toIntOrNull() ?: -1
            val minute = (hourMinMap[Screening.Minute] as? String)?.toIntOrNull() ?: -1

            val hourMinPair = Pair(hour, minute)
            val formattedDateTime = DateUtils.getFormattedDateTimeForLastMeal(
                map["${lastMealTime}${lastMealTypeDateSuffix}"] as String,
                map["${lastMealTime}${lastMealTypeMeridiem}"] as String,
                hourMinPair
            )
            formattedDateTime?.let {
                map.apply {
                    remove("${lastMealTime}${lastMealTypeMeridiem}")
                    remove("${lastMealTime}${lastMealTypeDateSuffix}")
                    remove(lastMealTime)
                    this[lastMealTime] = formattedDateTime
                }
            }
        }
    }
    private fun setFBSAndRBSValues(
        map: HashMap<String, Any>,
        bloodGlucoseValue: Double,
        code: Int,
        getRbsFbs: (Pair<Double?,Double?>) -> Unit
    ) {
        when (code) {
            1 -> {
                if (checkFBS(map)) {
                    map[Screening.Glucose_Type] = Screening.fbs
                    getRbsFbs.invoke(Pair(bloodGlucoseValue, null))
                } else {
                    map[Screening.Glucose_Type] = Screening.rbs
                    getRbsFbs.invoke(Pair(null, bloodGlucoseValue))
                }
            }

            2 -> {
                if (map.containsKey(Screening.Glucose_Type)) {
                    when ((map[Screening.Glucose_Type] as String).lowercase()) {
                        Screening.rbs -> {
                            getRbsFbs.invoke(Pair(bloodGlucoseValue, null))
                        }

                        Screening.fbs -> {
                            getRbsFbs.invoke(Pair(null, bloodGlucoseValue))
                        }
                    }

                }
            }
        }
    }
    private fun checkFBS(map: HashMap<String, Any>): Boolean {
        if (map.containsKey(lastMealTime) && map[lastMealTime] is String) {
            val lastMealTimeInMillis =
                DateUtils.convertDateTimeToMillisUsingLocal(map[lastMealTime] as String)
            val calendar = Calendar.getInstance()
            var different: Long = calendar.timeInMillis - lastMealTimeInMillis
            val secondsInMilli: Long = 1000
            val minutesInMilli = secondsInMilli * 60
            val hoursInMilli = minutesInMilli * 60
            val elapsedHours: Long = different / hoursInMilli
            different %= hoursInMilli
            if (elapsedHours >= 8) {
                return true
            }
        }
        return false
    }

    private fun parseGlucoseValue(bloodGlucoseString: Any?): Double? {
        return when (bloodGlucoseString) {
            is String -> {
                bloodGlucoseString.toDoubleOrNull()
            }

            is Double -> {
                bloodGlucoseString
            }

            else -> null
        }
    }

    fun calculatePregnancySymptomCount(list: ArrayList<Map<*, *>>): Int {
        return when {
            list.size > 1 -> list.size
            list.size == 1 && !((list).first().containsValue(Screening.NoSymptoms)) -> 1
            else -> 0
        }
    }

    fun getBMIInformation(context: Context, bmi: Double?): Pair<String, Int>? {
        bmi?.let {
            if (bmi <= 18.49) {
                return Pair(
                    first = context.getString(R.string.under_weight),
                    second = R.color.bmi_under_weight
                )
            } else if (bmi in 18.50..24.99) {
                return Pair(
                    first = context.getString(R.string.normal_weight),
                    second = R.color.bmi_normal_weight
                )
            } else if (bmi in 25.00..29.99) {
                return Pair(
                    first = context.getString(R.string.over_weight),
                    second = R.color.bmi_over_weight
                )
            } else if (bmi in 30.00..34.99) {
                return Pair(
                    first = context.getString(R.string.obese),
                    second = R.color.bmi_obese
                )
            } else if (bmi >= 35.00) {
                return Pair(
                    first = context.getString(R.string.extremely_obese),
                    second = R.color.bmi_extremely_obese
                )
            } else {
                return null
            }
        }
        return null
    }

    fun getIdentityDisplayName(identityType: String?): String {
        if (identityType == null) {
            return ""
        }
        val identityTypes = SecuredPreference.getIdentityTypes()
        return identityTypes?.find { identityType == it.value }?.name ?: ""
    }

    fun getGlucoseUnit(glucoseUnit: String?, withoutBracket: Boolean): String {
        return if (glucoseUnit.isNullOrBlank()) {
            ""
        } else if (withoutBracket) {
            "$glucoseUnit"
        } else {
            "(${glucoseUnit})"
        }
    }

    fun parseRequest(
        generalDetails: String,
        screeningDetails: String,
        userId: String?
    ): HashMap<String, Any>? {
        val generalData: Map<String, Any>? =
            StringConverter.convertStringToMap(generalDetails)
        (StringConverter.convertStringToMap(screeningDetails))?.let { map ->
            HashMap(map).let {
                setType(it, generalData)
                handleBPLogs(it)
                handlePregnancyAnc(it)
                updateGlucoseData(it)
                it[Screening.UserId] = userId
                return it
            }
        }

        return null
    }

    private fun handlePregnancyAnc(givenMap: HashMap<String, Any>) {
        if (givenMap.containsKey(Screening.pregnancyAnc) && givenMap[Screening.pregnancyAnc] is Map<*, *>) {
            (givenMap[Screening.pregnancyAnc] as Map<*, *>?)?.let { ancMap ->
                if (ancMap.containsKey(Screening.PregnancySymptoms) && ancMap[Screening.PregnancySymptoms] is List<*>) {
                    (ancMap[Screening.PregnancySymptoms] as? ArrayList<*>)?.forEach {
                        if (it is LinkedTreeMap<*, *> && it.contains(DefinedParams.cultureValue))
                            it.remove(DefinedParams.cultureValue)
                    }
                }
            }
        }
    }

    private fun setType(hashMap: HashMap<String, Any>, generalData: Map<String, Any>?) {
        generalData?.forEach { (key, value) ->
            when (key) {
                CategoryDisplayName, CategoryDisplayType, SiteName, userSiteId -> Unit // Skip these keys
                CategoryType -> hashMap[Type] = value // Change key from "categoryType" to "type"
                otherType -> hashMap[key] = value
                siteId -> hashMap[siteId] = value
                else -> hashMap[key] = value
            }
        }
    }

    private fun handleBPLogs(map: HashMap<String, Any>) {
        try {
                val bpLog = map[Screening.bp_log] as? MutableMap<String, Any>?
                bpLog?.let { log ->
                    val logDetail =
                        log.entries.first { (key, _) -> key == Screening.BPLog_Details }.value as? java.util.ArrayList<*>
                    val newList = ArrayList<HashMap<String, String>>()
                    logDetail?.forEachIndexed { _, any ->
                        (any as? Map<*, *>?)?.let { record ->
                            val data = HashMap<String, String>()
                            (record[Screening.Systolic] as? Double?)?.let { sys ->
                                data[Screening.Systolic] = CommonUtils.parseDouble(sys)
                            }
                            (record[Screening.Diastolic] as? Double?)?.let { dia ->
                                data[Screening.Diastolic] = CommonUtils.parseDouble(dia)
                            }
                            (record[Screening.Pulse] as? Double?)?.let { pul ->
                                data[Screening.Pulse] = CommonUtils.parseDouble(pul)
                            }
                            if (data.isNotEmpty())
                                newList.add(data)
                        }
                    }
                    if (newList.isNotEmpty())
                        log[Screening.BPLog_Details] = newList
                }
        } catch (_: Exception) {
            //Catch Block
        }
    }

    private fun updateGlucoseData(givenMap: HashMap<String, Any>) {
        if (givenMap.containsKey(Screening.GlucoseLog) && givenMap[Screening.GlucoseLog] is Map<*, *>) {
            (givenMap[Screening.GlucoseLog] as Map<*, *>?)?.let { map ->
                updateGlucoseLog(givenMap, map)
            }
        }
    }

    private fun updateGlucoseLog(hashMap: HashMap<String, Any>, map: Map<*, *>) {
        var isChanged = false
        val subMap = HashMap(map)
        if (!subMap.containsKey(Screening.Glucose_Value) && subMap.containsKey(
                lastMealTime
            )
        ) {
            isChanged = true
            subMap.remove(key = lastMealTime)
        }
        if (subMap.containsKey(Screening.Glucose_Value) &&
            !subMap.containsKey(Screening.BloodGlucoseID + Screening.unitMeasurement_KEY)
        ) {
            isChanged = true
            subMap[Screening.BloodGlucoseID + Screening.unitMeasurement_KEY] =
                Screening.mmoll
        }
        if (subMap.containsKey(Screening.diabetes) && subMap[Screening.diabetes] is List<*>) {
            (subMap[Screening.diabetes] as? ArrayList<*>)?.forEach {
                if (it is LinkedTreeMap<*, *> && it.contains(DefinedParams.cultureValue))
                    it.remove(DefinedParams.cultureValue)
            }
        }
        if (isChanged) {
            hashMap[Screening.GlucoseLog] = subMap
        }
    }

    fun getBMIForNcd(heightInCM: Double, weight: Double, context: Context? = null): String? {
        val heightInMeter = heightInCM / 100
        val bmi = weight / (heightInMeter * heightInMeter)
        if (bmi.isInfinite() || bmi.isNaN()) {
            return context?.getString(R.string.hyphen_symbol)
        }
        return String.format(Locale.US, "%.2f", bmi)
    }

    fun calculateCVDRiskFactor(
        map: HashMap<String, Any>,
        list: ArrayList<RiskClassificationModel>,
        avgSystolic: Int?
    ) {
        if (map.containsKey(Screening.BMI) && list.isNotEmpty()) {
            val riskFactor = calculateRiskFactor(
                map,
                list,
                (map[Screening.BMI] as Double),
                avgSystolic
            )

            riskFactor?.let { riskFactorMap ->
                map[Screening.CVD_Risk_Score] =
                    riskFactorMap[Screening.CVD_Risk_Score] as Int
                map[Screening.CVD_Risk_Level] =
                    riskFactorMap[Screening.CVD_Risk_Level] as String
                map[Screening.CVD_Risk_Score_Display] =
                    riskFactorMap[Screening.CVD_Risk_Score_Display] as String
            }
        }
    }

    private fun calculateRiskFactor(
        map: Map<String, Any>,
        list: ArrayList<RiskClassificationModel>,
        bmiValue: Double?,
        systolicAverage: Int?
    ): Map<String, Any>? {

        val age: Double? = if (map.containsKey(Screening.DateOfBirth)) {
            when {
                map.containsKey(Screening.DateOfBirth) -> {
                    (map[Screening.DateOfBirth] as String).let {
                        DateUtils.getV2YearMonthAndWeek(it).years.toDouble()
                    }
                }

                else -> {
                    null
                }
            }
        } else {
            null
        }

        val gender: String? = if (map.containsKey(DefinedParams.Gender)) {
            val value = map[DefinedParams.Gender] as String
            if (value.equals(Screening.Female, ignoreCase = true))
                value
            else
                Screening.Male
        } else {
            null
        }

        val smoker: Boolean? = if (map.containsKey(Screening.is_regular_smoker)) {
            val tobaccoUsage = map[Screening.is_regular_smoker] as Boolean
            tobaccoUsage
        } else {
            null
        }

        val resultModel = list.filter {
            it.isSmoker == smoker && it.gender.equals(gender, true) && isAgeInLimit(
                age,
                it.age
            )
        }

        if (resultModel.isNotEmpty()) {
            return getRiskBasedOnParams(resultModel[0].riskFactors, bmiValue, systolicAverage)
        }
        return null
    }

    private fun isAgeInLimit(age: Double?, limit: String): Boolean {
        var status = false
        val limitArray = limit.split("-")
        if (age != null && limitArray.size == 2) {
            val minValue = limitArray[0].toIntOrNull()
            val maxValue = limitArray[1].toIntOrNull()
            if (minValue != null && maxValue != null) {
                status = age >= minValue && age <= maxValue
            }
        }
        return status
    }

    private fun getRiskBasedOnParams(
        riskFactors: ArrayList<RiskFactorModel>,
        bmiValue: Double?,
        systolicAverage: Int?
    ): Map<String, Any>? {
        if (bmiValue == null || systolicAverage == null)
            return null
        riskFactors.forEach { riskFactorModel ->
            if (checkBMIValue(riskFactorModel.bmi, bmiValue) && checkSystolicBPValue(
                    riskFactorModel.sbp,
                    systolicAverage
                )
            ) {
                val resultMap = HashMap<String, Any>()
                resultMap[Screening.CVD_Risk_Score] = riskFactorModel.riskScore
                resultMap[Screening.CVD_Risk_Score_Display] =
                    "${riskFactorModel.riskScore}% - ${riskFactorModel.riskLevel}"
                resultMap[Screening.CVD_Risk_Level] = riskFactorModel.riskLevel
                return resultMap
            }
        }

        return null
    }

    private fun checkBMIValue(bmi: String, bmiValue: Double): Boolean {
        when {
            bmi.contains(">=") -> {
                getCheckList(bmi, ">=")?.let {
                    val maxvalue = it[1].trim().toDouble()
                    return bmiValue >= maxvalue
                }
            }

            bmi.contains("<=") -> {
                getCheckList(bmi, "<=")?.let {
                    val maxvalue = it[1].trim().toDouble()
                    return bmiValue <= maxvalue
                }
            }

            bmi.startsWith("<") -> {
                getCheckList(bmi, "<")?.let {
                    val maxvalue = it[1].trim().toDouble()
                    return bmiValue < maxvalue
                }
            }

            bmi.contains("-") -> {
                getCheckList(bmi, "-")?.let {
                    val minValue = it[0].trim().toDouble()
                    val maxValue = it[1].trim().toDouble()
                    return bmiValue in minValue..maxValue
                }
            }

            bmi.contains(">") -> {
                getCheckList(bmi, ">")?.let {
                    val minValue = it[1].trim().toDouble()
                    return bmiValue > minValue
                }
            }
        }
        return false
    }

    private fun getCheckList(s: String, character: String): List<String>? {
        s.split(character).let {
            if (it.size > 1)
                return it
        }
        return null
    }

    private fun checkSystolicBPValue(sbp: String, systolicAverage: Int): Boolean {
        when {
            sbp.startsWith("<") -> {
                getCheckList(sbp, "<")?.let {
                    val maxvalue = it[1].trim().toInt()
                    return systolicAverage < maxvalue
                }
            }

            sbp.contains("-") -> {
                getCheckList(sbp, "-")?.let {
                    val minValue = it[0].trim().toInt()
                    val maxValue = it[1].trim().toInt()
                    return systolicAverage in minValue..maxValue
                }
            }

            sbp.contains(">=") -> {
                getCheckList(sbp, ">=")?.let {
                    val minValue = it[1].trim().toInt()
                    return systolicAverage >= minValue
                }
            }
        }
        return false
    }

    fun cvdRiskColorCode(score: Long, context: Context): Int {
        return when {
            score < Screening.very_low_risk_limit -> context.getColor(R.color.very_low_risk_color)
            score < Screening.low_risk_limit -> context.getColor(R.color.low_risk_color)
            score < Screening.medium_risk_limit -> context.getColor(R.color.medium_risk_color)
            score < Screening.medium_high_risk_limit -> context.getColor(R.color.medium_high_risk_color)
            else -> context.getColor(R.color.high_risk_color)
        }
    }

    fun isPatientListRequired(origin: String?): Boolean {
        return when (origin) {
            null,
            MenuConstants.LIFESTYLE.lowercase(),
            MenuConstants.PSYCHOLOGICAL.lowercase(),
            MenuConstants.DISPENSE.lowercase(),
            MenuConstants.INVESTIGATION.lowercase(),
            MenuConstants.MY_PATIENTS_MENU_ID.lowercase() -> true
            else -> false
        }
    }

    fun canShowToggle(gender: String?, pregnancyRisk: Boolean?): Boolean {
        val role = SecuredPreference.getRole()
        return (role == PROVIDER || role == PHYSICIAN_PRESCRIBER) &&
                gender.equals(Female, true) &&
                (pregnancyRisk != null &&
                        ((!pregnancyRisk && SecuredPreference.isAncEnabled()) || pregnancyRisk))
    }

    fun requestFrom(): String {
        return when {
            isCommunity() -> SPICE.SIERRA_LEONE.name
            else -> SPICE.AFRICA.name
        }
    }

    fun calculateProvisionalDiagnosis(
        map: HashMap<String, Any>,
        isConfirmDiagnosis: Boolean? = null,
        avgSystolic: Int? = null,
        avgDiastolic: Int? = null,
        fbsValue: Double = 0.0,
        rbsValue: Double = 0.0,
        unitType: String
    ) {
        if (isConfirmDiagnosis == false) {
            val diagnosisMap = ArrayList<String>()

            checkAvg(avgSystolic, avgDiastolic)?.let {
                diagnosisMap.add(it)
            }

            when (unitType) {
                Screening.mgdl -> {
                    if (fbsValue > Screening.FBSMaximumMGDlValue || rbsValue >= Screening.RBSMaximumMGDlValue) {
                        diagnosisMap.add(AssessmentDefinedParams.DM_Diagnosis)
                    }
                }

                else -> {
                    if (fbsValue > Screening.FBSMaximumValue || rbsValue >= Screening.RBSMaximumValue) {
                        diagnosisMap.add(AssessmentDefinedParams.DM_Diagnosis)
                    }
                }
            }
            if (diagnosisMap.size > 0)
                map[AssessmentDefinedParams.Provisional_Diagnosis] = diagnosisMap
        }
    }

    private fun checkAvg(avgSystolic: Int?, avgDiastolic: Int?): String? {
        if ((avgSystolic ?: 0) > AssessmentDefinedParams.UpperLimitSystolic || (avgDiastolic
                ?: 0) > AssessmentDefinedParams.UpperLimitDiastolic
        )
            return AssessmentDefinedParams.HTN_Diagnosis
        return null
    }

    fun formatListToString(list: List<String?>?, default: String): String {
        return if (list.isNullOrEmpty()) {
            default
        } else {
            list.filter { !it.isNullOrBlank() }
                .mapIndexed { index, item -> "${index + 1}. $item" }
                .joinToString("\n")
        }
    }

    fun getDialogValue(value: Any?, otherSymptoms: String? = null): String {
        val result = (value as? List<*>)?.mapNotNull { getListActual(it) }?.joinToString(", ") ?: ""
        return if (result.isNotEmpty()) {
            result + otherSymptoms?.let { " - $it" }.orEmpty()
        } else ""
    }

    fun getListActual(map: Any?): String? {
        return (map as? Map<*, *>)?.get(DefinedParams.cultureValue) as? String
            ?: (map as? Map<*, *>)?.get(DefinedParams.NAME) as? String
    }

    fun getBMIFormattedText(context: Context, bmi: Double?): Pair<CharSequence?, Int?> {
        if (bmi == null) return Pair(null,null)
        val bioCategoryInfo = getBMIInformation(context, bmi)
        val bmiInfo = getDecimalFormatted(bmi)

        return if (bioCategoryInfo == null) {
            Pair(bmiInfo, null)
        } else {
            Pair(
                SpannableStringBuilder()
                    .append(bmiInfo).append(" (${bioCategoryInfo.first})"),
                context.getColor(bioCategoryInfo.second)
            )
        }
    }

    fun isNurse(): Boolean {
        val userRole = SecuredPreference.getUserDetails()?.roles?.joinToString { it.name }
        if (userRole != null) {
            return userRole.contains(RoleConstant.NURSE)
        }
        return false
    }

    fun isLabTechnician(): Boolean {
        val userRole = SecuredPreference.getUserDetails()?.roles?.joinToString { it.name }
        if (userRole != null) {
            return userRole.contains(RoleConstant.LAB_TECHNICIAN)
        }
        return false
    }

    fun isNCDProvider(): Boolean {
        val userRole = SecuredPreference.getUserDetails()?.roles?.joinToString { it.name }
        if (userRole != null) {
            return userRole.contains(PROVIDER)
        }
        return false
    }

    fun isPsychologicalFlowEnabled(): Boolean {
        return SecuredPreference.getBoolean(SecuredPreference.EnvironmentKey.IS_PSYCHOLOGICAL_FLOW_ENABLED.name)
    }

    fun convertBitmapToByteArray(bitmap: Bitmap): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 20, byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }

    fun convertByteArrayToBitmap(byteArray: ByteArray?) : Bitmap?{
        return byteArray?.let { BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size) }
    }

    fun getIdentityValue(requestJson: Map<String, Any>?): String {
        requestJson?.let { req ->
            if (req.containsKey(Screening.bioData)) {
                (req[Screening.bioData] as? Map<*, *>)?.let { bio ->
                    if (bio.containsKey(Screening.identityValue)) {
                        return bio[Screening.identityValue] as? String ?: ""
                    }
                }
            }
        }
        return ""
    }

    fun addAncEnableOrNot(map: HashMap<String, Any>, key: String, isPregnant: Boolean = false) {
        if (SecuredPreference.isAncEnabled()) {
            (map[key] as? MutableMap<Any, Any>)?.let { pregnancyAnc ->
                if (((pregnancyAnc[Screening.isPregnant] as? Boolean) == true) || isPregnant) {
                    if (isPregnant) {
                        pregnancyAnc[Screening.isPregnant] = true
                    }
                    pregnancyAnc[Screening.isPregnancyAnc] = true
                }

                (map[Screening.bioMetrics] as? HashMap<*, *>)?.let { bioMetricMap ->
                    val isMale =
                        bioMetricMap[DefinedParams.Gender]?.toString().equals(Screening.Male, true)
                    if (isMale)
                        map.remove(Screening.pregnancyAnc)
                }
            }
        }
    }

    fun isNURSE(): Boolean {
        val userRole = SecuredPreference.getUserDetails()?.roles?.joinToString { it.name }
        if (userRole != null) {
            return userRole.contains(RoleConstant.NURSE)
        }
        return false
    }

    fun isHRIO(): Boolean {
        val userRole = SecuredPreference.getUserDetails()?.roles?.joinToString { it.name }
        if (userRole != null) {
            return userRole.contains(RoleConstant.HRIO)
        }
        return false
    }

    fun getErrorMessage(errorBody: ResponseBody?): String? {
        if (errorBody == null)
            return null
        return try {
            val errorResponse = Gson().fromJson(errorBody.string(), ErrorResponse::class.java)
            return errorResponse.message
        } catch (e: Exception) {
            null
        }
    }

    fun validationRequest(requestMap: HashMap<String, Any>): HashMap<String, Any> {
        requestMap[Screening.identityType] = Screening.nationalId
        if (!requestMap.containsKey(DefinedParams.Country)) {
            requestMap[DefinedParams.Country] = getCountryMap()
        }
        val map = requestMap.filterKeys {
            it == Screening.firstName ||
                    it == Screening.lastName ||
                    it == Screening.phoneNumber ||
                    it == Screening.identityType ||
                    it == Screening.identityValue ||
                    it == DefinedParams.Country ||
                    it == AssessmentDefinedParams.memberReference
        }
        return HashMap(map)
    }

    fun getCountryMap(): HashMap<String, Any> {
        val countryId = HashMap<String, Any>()
        SecuredPreference.getCountryId()?.let { cId ->
            countryId[DefinedParams.ID] = cId
        }
        return countryId
    }

    fun canShowScheduleMenu(): Boolean {
        return isNonCommunity() && isHRIO()
    }

    fun capitalize(str: String): String {
        val words = str.lowercase().split(" ")
        val sb = StringBuilder()
        words.forEach {
            if (it != "") {
                sb.append(it[0].uppercase()).append(it.substring(1))
            }
            sb.append(" ")
        }
        return sb.toString().trim { it <= ' ' }
    }

    fun canShowSort(origin: String?): Boolean {
        if(isTiberbuUser())
            return false

        val isFromMyPatients: Boolean =
            origin?.equals(MenuConstants.MY_PATIENTS_MENU_ID, true) ?: false

        val userRole = SecuredPreference.getUserDetails()?.roles?.joinToString { it.name }

        if (userRole != null) {
            return isFromMyPatients &&
                    (userRole.contains(COMMUNITY_HEALTH_WORKER) || userRole.contains(PROVIDER) || userRole.contains(
                        PHYSICIAN_PRESCRIBER
                    ))
        }
        return false
    }

    fun canShowFilter(origin: String?): Boolean {
        if (isTiberbuUser())
            return false

        val isFromInvestigation: Boolean =
            origin?.equals(MenuConstants.INVESTIGATION, true) ?: false
        val isFromDispense: Boolean = origin?.equals(MenuConstants.DISPENSE, true) ?: false

        val userRole = SecuredPreference.getUserDetails()?.roles?.joinToString { it.name }

        if (userRole != null) {
            return canShowSort(origin) || isFromDispense || isFromInvestigation
        }
        return false
    }

    fun isCommunity(): Boolean {
        return SecuredPreference.getBoolean(SecuredPreference.EnvironmentKey.IS_COMMUNITY.name)
    }

    //Africa
    fun isNonCommunity(): Boolean {
        return SecuredPreference.getBoolean(SecuredPreference.EnvironmentKey.IS_NON_COMMUNITY.name)
    }

    fun isPharmacist(): Boolean {
        val userRole = SecuredPreference.getUserDetails()?.roles?.joinToString { it.name }
        return userRole != null && userRole.contains(PHARMACIST)
    }

    fun isDispenseOrInvestigation(origin: String?): Boolean {
        return when (origin?.lowercase()) {
            MenuConstants.DISPENSE.lowercase(), MenuConstants.INVESTIGATION.lowercase() -> true
            else -> false
        }
    }
    fun isPhysicianPrescriber(): Boolean {
        val userRole = SecuredPreference.getUserDetails()?.roles?.joinToString { it.name }
        if (userRole != null) {
            return userRole.contains(PHYSICIAN_PRESCRIBER)
        }
        return false
    }

    fun getPhysicalExaminationTitle(
        context: Context,
        workflow: String,
        isFemalePregnant: Boolean
    ): String {
        if (workflow.equals(NCDMRUtil.MENTAL_HEALTH, true))
            return context.getString(R.string.systemic_examinations)

        if (workflow.equals(
                DefinedParams.PregnancyANC,
                true
            ) && SecuredPreference.isAncEnabled() && isFemalePregnant
        )
            return context.getString(R.string.obstetric_examination)

        return context.getString(R.string.physical_examinations)
    }

    fun getAssessmentType(context: Context, type: String): String {
        return when (type) {
            context.getString(R.string.phq4_score) -> AssessmentDefinedParams.phq4
            context.getString(R.string.phq9_score) -> AssessmentDefinedParams.phq9
            context.getString(R.string.gad7_score) -> AssessmentDefinedParams.gad7
            context.getString(R.string.suicidal_ideation) -> AssessmentDefinedParams.suicidalIdeation
            context.getString(R.string.cage_aid) -> AssessmentDefinedParams.cageAid
            else -> ""
        }
    }

    fun mentalHealths(
        questionarieId: String?,
        observationId: String?,
        result: Pair<String?, HashMap<String, Any>>
    ) {
        result.second.let { mentalHealthMap ->
            (mentalHealthMap[AssessmentDefinedParams.phq4] as? HashMap<String, Any>)?.let { phq4Map ->
                if (phq4Map.isEmpty())
                    mentalHealthMap.remove(AssessmentDefinedParams.phq4)
                else {
                    questionarieId?.let { qId ->
                        phq4Map[NCDMRUtil.questionnaireId] = qId
                    }
                }
            }
            (mentalHealthMap[AssessmentDefinedParams.phq9] as? HashMap<String, Any>)?.let { phq9Map ->
                if (phq9Map.isEmpty())
                    mentalHealthMap.remove(AssessmentDefinedParams.phq9)
                else {
                    (phq9Map[AssessmentDefinedParams.PHQ9_Score] as? Int)?.let { phq9Score ->
                        phq9Map[mentalHealthScore] = phq9Score
                        phq9Map.remove(AssessmentDefinedParams.PHQ9_Score)
                    }
                    (phq9Map[AssessmentDefinedParams.PHQ9_Risk_Level] as? String)?.let { phq9RiskLevel ->
                        phq9Map[RiskLevel] = phq9RiskLevel
                        phq9Map.remove(AssessmentDefinedParams.PHQ9_Risk_Level)
                    }
                    (phq9Map[AssessmentDefinedParams.PHQ9_Mental_Health] as? ArrayList<*>)?.let { phq9MentalHealth ->
                        phq9Map[MentalHealthDetails] = phq9MentalHealth
                        phq9Map.remove(AssessmentDefinedParams.PHQ9_Mental_Health)
                    }
                    questionarieId?.let { qId ->
                        phq9Map[NCDMRUtil.questionnaireId] = qId
                    }
                }
            }
            (mentalHealthMap[AssessmentDefinedParams.gad7] as? HashMap<String, Any>)?.let { gad7Map ->
                if (gad7Map.isEmpty())
                    mentalHealthMap.remove(AssessmentDefinedParams.gad7)
                else {
                    (gad7Map[AssessmentDefinedParams.GAD7_Score] as? Int)?.let { gad7Score ->
                        gad7Map[mentalHealthScore] = gad7Score
                        gad7Map.remove(AssessmentDefinedParams.GAD7_Score)
                    }
                    (gad7Map[AssessmentDefinedParams.GAD7_Risk_Level] as? String)?.let { gad7RiskLevel ->
                        gad7Map[RiskLevel] = gad7RiskLevel
                        gad7Map.remove(AssessmentDefinedParams.GAD7_Risk_Level)
                    }
                    (gad7Map[AssessmentDefinedParams.GAD7_Mental_Health] as? ArrayList<*>)?.let { gad7MentalHealth ->
                        gad7Map[MentalHealthDetails] = gad7MentalHealth
                        gad7Map.remove(AssessmentDefinedParams.GAD7_Mental_Health)
                    }
                    questionarieId?.let { qId ->
                        gad7Map[NCDMRUtil.questionnaireId] = qId
                    }
                }
            }
            (mentalHealthMap[AssessmentDefinedParams.suicidalIdeation] as? HashMap<String, Any>)?.let { suicideScreener ->
                if (suicideScreener.isEmpty())
                    mentalHealthMap.remove(AssessmentDefinedParams.suicidalIdeation)
                else {
                    observationId?.let { oId ->
                        suicideScreener[AssessmentDefinedParams.ObservationID] = oId
                    }
                }
            }
            (mentalHealthMap[AssessmentDefinedParams.cageAid] as? HashMap<String, Any>)?.let { substanceAbuse ->
                if (substanceAbuse.isEmpty())
                    mentalHealthMap.remove(AssessmentDefinedParams.cageAid)
                else {
                    observationId?.let { oId ->
                        substanceAbuse[AssessmentDefinedParams.ObservationID] = oId
                    }
                }
            }
        }
    }

    fun getModifiedResponse(response: List<VillageEntity>): List<VillageEntity> {
        val (villages, otherVillage) = response.partition { it.chiefdomId != null }
        return ArrayList<VillageEntity>().apply {
            addAll(villages)
            addAll(otherVillage)
        }
    }

    fun isHealthScreener(): Boolean {
        val userRole = SecuredPreference.getUserDetails()?.roles?.joinToString { it.name }
        if (userRole != null) {
            return userRole.contains(HEALTH_SCREENER)
        }
        return false
    }

    fun isChp(): Boolean {
        val userRole = SecuredPreference.getUserDetails()?.roles?.joinToString { it.name }
        if (userRole != null) {
            return userRole.contains(COMMUNITY_HEALTH_PROMOTER)
        }
        return false
    }

    fun isCommunityOrNot(): String {
        return if (isCommunity()) {
            DefinedParams.COMMUNITY
        } else {
            DefinedParams.NON_COMMUNITY
        }
    }

    fun isNutritionist(): Boolean {
        val userRole = SecuredPreference.getUserDetails()?.roles?.joinToString { it.name }
        if (userRole != null) {
            return userRole.contains(RoleConstant.NUTRITIONIST)
        }
        return false
    }

    fun isCha(): Boolean {
        val userRole = SecuredPreference.getUserDetails()?.roles?.joinToString { it.name }
        if (userRole != null) {
            return userRole.contains(COMMUNITY_HEALTH_ASSISTANT)
        }
        return false
    }

    fun gestationalWeekLimitCheck(date: String?): Boolean {
        date?.let {
            DateUtils.calculateGestationalWeeks(it)?.let { weeks ->
                if (weeks in 4..40) {
                    return true
                }
            }
        }
        return false
    }

    fun isTiberbuUser(): Boolean {
        val userRole = SecuredPreference.getUserDetails()?.roles?.joinToString { it.name }
        if (userRole != null) {
            return userRole.contains(RoleConstant.TIBERBU_PROVIDER)
        }
        return false
    }

    fun parseUserLocale(): String {
        val preference = SecuredPreference.getCultureName()
        return when {
            preference.contains(DefinedParams.EN_Locale, ignoreCase = true) ->
                DefinedParams.EN

            preference.contains(DefinedParams.SW_Locale, ignoreCase = true) ->
                DefinedParams.SW

            else -> DefinedParams.EN
        }
    }

    fun checkIfTranslationEnabled(name: String): Boolean {
        return name.contains(DefinedParams.SW_Locale, ignoreCase = true)
    }

    fun getAgeInYearsByDOB(dob: String) : Int {
        val formatter = DateTimeFormatter.ofPattern(DateUtils.DATE_ddMMyyyy)
        val birthDate = LocalDate.parse(dob, formatter)
        val currentDate = LocalDate.now()

        return Period.between(birthDate, currentDate).years
    }
}