package org.medtroniclabs.uhis.common

import android.content.Context
import org.joda.time.PeriodType
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.data.model.CalendarPeriod
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams
import org.medtroniclabs.uhis.mappingkey.Screening.TODAY
import org.medtroniclabs.uhis.mappingkey.Screening.YESTERDAY
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.Period
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * UI display for age-or-DOB: a single value and unit. Calendar years/months when applicable;
 * below one full month the remainder is shown in **days** (not weeks). From the fifth birthday
 * onward, years only.
 */
data class AgeOrDobDisplay(
    val value: Int,
    val unit: AgeOrDobUnit,
) {
    enum class AgeOrDobUnit { DAY, MONTH, YEAR }
}

object DateUtils {
    const val DATE_ddMMyyyy = "dd/MM/yyyy"
    const val DATE_FORMAT_ddMMyyyy = "dd-MM-yyyy"
    const val DATE_FORMAT_yyyyMMdd = "yyyy-MM-dd"
    const val DATE_FORMAT_yyyyMMddHHmmssZZZZZ = "yyyy-MM-dd'T'HH:mm:ssZZZZZ"
    const val DATE_FORMAT_yyyyMMddHHmmss = "yyyy-MM-dd'T'HH:mm:ss"
    const val DATE_FORMAT_yyyyMMdd_HHmmss = "yyyy-MM-dd HH:mm:ss"
    const val DATE_FORMAT_ddMMMyyyy = "dd MMM, yyyy"
    const val DATE_TIME_DISPLAY_FORMAT = "dd MMM, yyyy - hh:mm a"
    const val DATE_TIME_CALL_DISPLAY_FORMAT = "dd MMM, hh:mm a"
    const val TIME_FORMAT_hhmma = "hh:mm a"
    const val CALENDAR_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX"
    const val GESTATIONALAGE_CALENDAR = "yyyy-MM-dd"
    const val DATE_FORMAT_ddMMyy_GRAPH = "dd-MM-yyyy"
    const val DATE_TIME_EEEMMMddHHmmsszyyyy = "EEE MMM dd HH:mm:ss z yyyy"
    const val DATE_TIME_YYYYMMDDTHHmmssSSSZ = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    const val DATE_FORMAT_DD_MMMM_YYYY = "dd-MMMM-yyyy"
    const val DATE_DD_MMM_YYYY = "dd-MMM-yyyy"

    fun getYearMonthAndWeek(
        inputDate: String,
        inputFormat: String = DATE_FORMAT_ddMMyyyy,
    ): Pair<Int?, Triple<Int?, Int?, Int?>> {
        try {
            val dateFormat = SimpleDateFormat(inputFormat, Locale.getDefault())
            val birthDate = dateFormat.parse(inputDate)

            val currentDate = Calendar.getInstance().time

            val diff = currentDate.time - birthDate.time
            val age = Calendar.getInstance().apply {
                timeInMillis = diff
            }

            val years = age.get(Calendar.YEAR) - 1970
            val months = age.get(Calendar.MONTH)
            val weeks = (age.get(Calendar.DAY_OF_MONTH) - 1) / 7
            val days = age.get(Calendar.DAY_OF_MONTH)

            return Pair(days, Triple(years, months, weeks))
        } catch (_: Exception) {
            return Pair(null, Triple(null, null, null))
        }
    }

    fun getV2YearMonthAndWeek(
        dob: String,
        format: String = DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
    ): CalendarPeriod {
        val dobDate = LocalDate.parse(dob, DateTimeFormatter.ofPattern(format))
        val today = LocalDate.now()

        val period = Period.between(dobDate, today)

        val years = period.years
        val months = period.months

        // Calculate the weeks by finding the number of days and then converting to weeks
        val daysBetween = ChronoUnit.DAYS.between(
            dobDate,
            today.minusYears(years.toLong()).minusMonths(months.toLong()),
        )
        val weeks = (daysBetween / 7).toInt()
        val days = (daysBetween % 7).toInt()

        return CalendarPeriod(years, months, weeks, days)
    }

    /**
     * @param birthDate Date of birth (date-only, same zone as [today]).
     */
    fun getAgeOrDobDisplay(
        birthDate: LocalDate,
        today: LocalDate = LocalDate.now(),
    ): AgeOrDobDisplay {
        if (birthDate.isAfter(today)) {
            return AgeOrDobDisplay(0, AgeOrDobDisplay.AgeOrDobUnit.DAY)
        }
        // Same period breakdown as getV2YearMonthAndWeek / getAgeFromDOB
        val period = Period.between(birthDate, today)
        val years = period.years
        val months = period.months
        val daysInRemainder = ChronoUnit.DAYS.between(
            birthDate,
            today.minusYears(years.toLong()).minusMonths(months.toLong()),
        )
        val daysTotal = daysInRemainder.toInt()
        if (years >= 1) {
            return AgeOrDobDisplay(years, AgeOrDobDisplay.AgeOrDobUnit.YEAR)
        }
        if (months > 0) {
            return AgeOrDobDisplay(months, AgeOrDobDisplay.AgeOrDobUnit.MONTH)
        }
        return AgeOrDobDisplay(daysTotal, AgeOrDobDisplay.AgeOrDobUnit.DAY)
    }

    fun getAgeOrDobDisplayFromDdMmYyyy(
        dobFormatted: String,
        today: LocalDate = LocalDate.now(),
    ): AgeOrDobDisplay? =
        try {
            val birthDate = LocalDate.parse(
                dobFormatted,
                DateTimeFormatter.ofPattern(DATE_ddMMyyyy).withLocale(Locale.ENGLISH),
            )
            getAgeOrDobDisplay(birthDate, today)
        } catch (_: Exception) {
            null
        }

    fun getYearMonthAndDate(
        dateString: String,
        defaultFormat: SimpleDateFormat = getDateDDMMYYYY(),
    ): Triple<Int?, Int?, Int?> {
        try {
            val date = formatStringToDate(dateString, defaultFormat)
            date?.let {
                val cal = Calendar.getInstance()
                cal.time = date
                return Triple(
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DATE),
                )
            }
        } catch (_: Exception) {
            return Triple(null, null, null)
        }
        return Triple(null, null, null)
    }

    fun getTimeInMilliFromDate(
        dateString: String,
        defaultFormat: SimpleDateFormat = getDateDDMMYYYY(),
    ): Long? {
        try {
            val date = formatStringToDate(dateString, defaultFormat)
            date?.let {
                val cal = Calendar.getInstance()
                cal.time = date
                return cal.timeInMillis
            }
        } catch (_: Exception) {
            return null
        }
        return null
    }

    fun formatStringToDate(
        dateString: String,
        format: SimpleDateFormat,
    ): Date? = format.parse(dateString)

    fun getDatePatternDDMMYYYY() = SimpleDateFormat(DATE_FORMAT_ddMMyyyy, Locale.ENGLISH)

    fun getDateDDMMYYYY(): SimpleDateFormat = SimpleDateFormat(DATE_ddMMyyyy, Locale.ENGLISH)

    fun getDateString(
        time: Long,
        inputFormat: String? = null,
        outputFormat: String? = null,
    ): String {
        val date = Date(time)
        val format = SimpleDateFormat(
            inputFormat,
            Locale.ENGLISH,
        )
        return format.format(date).let {
            if (outputFormat == DATE_FORMAT_yyyyMMddHHmmssZZZZZ) {
                "${format.format(date)}${DefinedParams.DOB_STRING}"
            } else {
                format.format(date)
            }
        }
    }

    fun calculateBirthDate(
        years: Int,
        months: Int,
        weeks: Int,
    ): String {
        var localDate = OffsetDateTime.now()

        val days = weeks * 7
        localDate = localDate
            .minusYears(years.toLong())
            .minusMonths(months.toLong())
            .minusDays(days.toLong())
            .withHour(0)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)

        val resul = localDate.format(DateTimeFormatter.ofPattern(DATE_FORMAT_yyyyMMddHHmmss).withLocale(Locale.ENGLISH))
        return "$resul+00:00"
    }

    /**
     * Calculates birth date from years, months, and days
     * @param years Number of years
     * @param months Number of months
     * @param days Number of days
     * @return Birth date in UTC format (yyyy-MM-dd'T'HH:mm:ss+00:00)
     */
    fun calculateBirthDateYMD(
        years: Int,
        months: Int,
        days: Int,
    ): String {
        var localDate = OffsetDateTime.now()

        localDate = localDate
            .minusYears(years.toLong())
            .minusMonths(months.toLong())
            .minusDays(days.toLong())
            .withHour(0)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)

        val result = localDate.format(DateTimeFormatter.ofPattern(DATE_FORMAT_yyyyMMddHHmmss).withLocale(Locale.ENGLISH))
        return "$result+00:00"
    }

    /**
     * Calculates DOB from age (years)
     * Returns DOB as 01/01/{calculatedYear} in UTC format
     * @param age Age in years (integer)
     * @return DOB string in UTC format (yyyy-MM-dd'T'HH:mm:ss+00:00)
     */
    fun calculateDOBFromAge(age: Int): String {
        var localDate = OffsetDateTime.now()

        localDate = localDate
            .minusYears(age.toLong())
            .withMonth(1)
            .withDayOfMonth(1)
            .withHour(0)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)

        val resul = localDate.format(DateTimeFormatter.ofPattern(DATE_FORMAT_yyyyMMddHHmmss).withLocale(Locale.ENGLISH))
        return "$resul+00:00"
    }

    /**
     * Calculates years, months, and days from a date of birth
     * @param dob Date of birth in format yyyy-MM-dd'T'HH:mm:ssZZZZZ
     * @param format Date format (default: DATE_FORMAT_yyyyMMddHHmmssZZZZZ)
     * @return CalendarPeriod with years, months, weeks=0, and days
     */
    fun getYearMonthAndDays(
        dob: String,
        format: String = DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
    ): CalendarPeriod {
        val dobDate = LocalDate.parse(dob, DateTimeFormatter.ofPattern(format))
        val today = LocalDate.now()

        val period = Period.between(dobDate, today)

        val years = period.years
        val months = period.months

        // Calculate the days by finding the number of days remaining after years and months
        val daysBetween = ChronoUnit.DAYS.between(
            dobDate,
            today.minusYears(years.toLong()).minusMonths(months.toLong()),
        )
        val days = daysBetween.toInt()

        return CalendarPeriod(years, months, 0, days)
    }

    fun calculateAge(birthYear: Int): Int {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        return currentYear - birthYear
    }

    fun calculateAgeInDays(birthDateStr: String?): Int {
        val birthDate = parseDate(birthDateStr)
        return birthDate?.let {
            ChronoUnit.DAYS.between(it, LocalDate.now()).toInt()
        } ?: 0
    }

    fun calculateAgeInMonths(birthDateString: String): Pair<Int, Date>? {
        val startDate = formatStringToDate(
            birthDateString,
            SimpleDateFormat(
                DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                Locale.ENGLISH,
            ),
        )
        startDate?.let {
            return Pair(calculateAgeInMonths(it), it)
        }
        return null
    }

    fun calculateAgeInMonths(startDate: Date): Int {
        val endDate = Calendar.getInstance().time
        val period = org.joda.time.Period(startDate.time, endDate.time, PeriodType.months())
        return period.months
    }

    fun getTomorrowDate(): Long {
        val chosenDate = Calendar.getInstance()
        chosenDate.timeInMillis = System.currentTimeMillis()
        chosenDate.add(Calendar.DAY_OF_MONTH, 1)
        return chosenDate.timeInMillis
    }

    fun convertDateToLong(
        date: String?,
        format: String? = DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
    ): Long? {
        try {
            val testedDate = date?.let { dateStr ->
                SimpleDateFormat(
                    format,
                    Locale.ENGLISH,
                ).parse(dateStr)?.time
            }
            return testedDate ?: 0L
        } catch (_: Exception) {
            return null
        }
    }

    fun convertDateTimeToDate(
        inputText: String?,
        inputFormat: String,
        outputFormat: String,
        inUserTimeZone: Boolean? = false,
        inUTC: Boolean? = null,
    ): String {
        try {
            inputText?.let {
                if (it.isNotBlank()) {
                    var userTimeZone: TimeZone? = null
                    val isTimeZoneFormat = inputFormat == DATE_FORMAT_yyyyMMddHHmmssZZZZZ
                    if (isTimeZoneFormat || inUserTimeZone == true) {
                        getTimeZoneInput(inputText, isTimeZoneFormat)?.let { timeZone ->
                            userTimeZone = timeZone
                        }
                    } else if (inUTC == true) {
                        userTimeZone = getUTCFormat()
                    }
                    val sdfInput = SimpleDateFormat(inputFormat, Locale.ENGLISH)
                    userTimeZone?.let { timeZone ->
                        sdfInput.timeZone = timeZone
                    }
                    val date = sdfInput.parse(it)
                    date?.let {
                        val sdfOutput = SimpleDateFormat(outputFormat, Locale.ENGLISH)
                        userTimeZone?.let { timeZone ->
                            sdfOutput.timeZone = timeZone
                        }
                        return sdfOutput.format(date)
                    }
                }
            }
        } catch (_: Exception) {
            return ""
        }
        return ""
    }

    private fun getTimeZoneInput(
        inputText: String,
        timeZoneFormat: Boolean,
    ): TimeZone? {
        var timeZoneInput = "GMT+00:00" // Take from secured preference in future
        if (timeZoneInput.isBlank() && timeZoneFormat) {
            timeZoneInput = "GMT${
                if (inputText.contains("+")) {
                    inputText.substring(inputText.indexOf("+"))
                } else {
                    inputText.substring(inputText.indexOf("-"))
                }
            }"
        } else if (timeZoneInput.isNotBlank()) {
            timeZoneInput = "GMT$timeZoneInput"
        }
        return TimeZone.getTimeZone(timeZoneInput)
    }

    private fun getUTCFormat(): TimeZone? = TimeZone.getTimeZone("GMT+00:00")

    fun convertedMMMToddMM(inputDate: String): Triple<Int?, Int?, Int?> =
        try {
            val inputFormat = SimpleDateFormat(DATE_ddMMyyyy, Locale.ENGLISH)
            val outputFormat = SimpleDateFormat(DATE_ddMMyyyy, Locale.ENGLISH)
            val date = inputFormat.parse(inputDate)
            getYearMonthAndDate(outputFormat.format(date))
        } catch (_: Exception) {
            Triple(null, null, null)
        }

    fun convertDateFormat(
        inputDateString: String,
        inputDateFormat: String,
        outputDateFormat: String,
    ): String {
        val inputFormat = SimpleDateFormat(inputDateFormat, Locale.ENGLISH)

        return try {
            val date = inputFormat.parse(inputDateString)
            val outputFormat = SimpleDateFormat(outputDateFormat, Locale.ENGLISH)
            date?.let { outputFormat.format(it) } ?: " "
        } catch (_: Exception) {
            " "
        }
    }

    fun dateToMonths(
        dateString: String,
        givenFormat: String = DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
    ): Int? =
        try {
            val formatter = DateTimeFormatter.ofPattern(givenFormat)
            val givenDateTime = LocalDateTime.parse(dateString, formatter)
            val currentDateTime = LocalDateTime.now()
            val months =
                givenDateTime.toLocalDate().until(currentDateTime.toLocalDate()).toTotalMonths()
            months.toInt()
        } catch (_: Exception) {
            null
        }

    fun calculateAge(
        birthDateString: String,
        givenFormat: String = DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
    ): Int =
        try {
            val yearConvert = 1000L * 60 * 60 * 24 * 365.25
            val dateFormat = SimpleDateFormat(givenFormat, Locale.getDefault())
            val birthDate = dateFormat.parse(birthDateString)
            val today = Calendar.getInstance().time
            val diff = today.time - birthDate.time
            val age = diff / yearConvert
            age.toInt()
        } catch (_: Exception) {
            0
        }

    /**
     * Calculate age from given to date instead of today date
     */
    fun calculateAgeToDate(
        birthDateString: String,
        toDate: String,
        givenFormat: String = DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
    ): Int =
        try {
            val yearConvert = 1000L * 60 * 60 * 24 * 365.25
            val dateFormat = SimpleDateFormat(givenFormat, Locale.getDefault())
            val birthDate = dateFormat.parse(birthDateString)
            val toDate = dateFormat.parse(toDate)
            val diff = toDate.time - birthDate.time
            val age = diff / yearConvert
            age.toInt()
        } catch (_: Exception) {
            0
        }

    fun getDateAfterDays(days: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, days)

        val dateFormat = SimpleDateFormat(DATE_ddMMyyyy, Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    fun calculateGestationalAge(lastMenstrualDate: Calendar): Pair<Long, Long> {
        val currentDate = Calendar.getInstance()
        val diffInMillis = currentDate.timeInMillis - lastMenstrualDate.timeInMillis
        val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)
        val weeks = diffInDays / 7
        val days = diffInDays % 7
        return Pair(weeks, days)
    }

    fun getDateFormat(): SimpleDateFormat = SimpleDateFormat(DATE_ddMMyyyy, Locale.getDefault())

    fun getLastMenstrualDate(clinicalDate: String): Calendar {
        // Define the format of the input date string
        val lastMenstrualDateString = convertDateFormat(
            clinicalDate,
            DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
            DATE_ddMMyyyy,
        )
        return Calendar.getInstance().apply {
            time = getDateFormat().parse(lastMenstrualDateString)
        }
    }

    fun getEstDeliveryDateFromLmp(lmp: String): String {
        val serverDf = SimpleDateFormat(DATE_FORMAT_yyyyMMddHHmmssZZZZZ)
        val lmpCalendar = Calendar.getInstance()
        lmpCalendar.time = serverDf.parse(lmp)

        val estDeliveryDate = calculateEstimatedDeliveryDate(lmpCalendar)
        return serverDf.format(estDeliveryDate.time)
    }

    fun calculateEstimatedDeliveryDate(lastMenstrualDate: Calendar): Calendar {
        val estimatedDeliveryDate = lastMenstrualDate.clone() as Calendar
        estimatedDeliveryDate.add(Calendar.DAY_OF_YEAR, 280)
        return estimatedDeliveryDate
    }

    fun getCurrentDateAndTime(format: String): String {
        val currentDateTime = ZonedDateTime.now(ZoneId.systemDefault())
        val formatter = DateTimeFormatter.ofPattern(format)
        return currentDateTime.format(formatter)
    }

    fun addDaysToDate(
        date: Date,
        days: Int,
    ): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.add(Calendar.DAY_OF_MONTH, days)
        return calendar.time
    }

    fun addMonthsToDate(
        date: Date,
        months: Int,
    ): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.add(Calendar.MONTH, months)
        return calendar.time
    }

    fun convertStringToDate(
        dateString: String,
        format: String,
    ): Date? {
        val dateFormat = SimpleDateFormat(format, Locale.getDefault())
        return dateFormat.parse(dateString)
    }

    fun getDateStringFromDate(
        date: Date,
        format: String,
    ): String {
        val dateFormat = SimpleDateFormat(format, Locale.getDefault())
        return dateFormat.format(date)
    }

    fun convertDateToStringWithUTC(date: Date): String {
        val instant = date.toInstant()
        var offsetDateTime = OffsetDateTime.ofInstant(instant, ZoneOffset.systemDefault())

        offsetDateTime = offsetDateTime
            .withHour(0)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)

        val result = offsetDateTime.format(DateTimeFormatter.ofPattern(DATE_FORMAT_yyyyMMddHHmmss))
        return "$result+00:00"
    }

    fun daysBetweenDates(
        startDate: Date,
        endDate: Date,
    ): Long {
        val diffInMillis = abs(endDate.time - startDate.time)
        return TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS)
    }

    fun getTodayDateDDMMYYYY(format: String = DATE_FORMAT_yyyyMMddHHmmssZZZZZ): String {
        val calendar = Calendar.getInstance()
        return getDateString(calendar.time.time, format)
    }

    fun calculateGestationalAge(lmpDate: LocalDate): Long {
        val currentDate = LocalDate.now()
        return ChronoUnit.WEEKS.between(lmpDate, currentDate)
    }

    fun calculateGestationalAgeWeeks(
        startDateString: String,
        endDateString: String,
    ): Long {
        val formatter = DateTimeFormatter.ISO_DATE_TIME
        val startDate = ZonedDateTime.parse(startDateString, formatter)
        val endDate = ZonedDateTime.parse(endDateString, formatter)
        val daysBetween = ChronoUnit.DAYS.between(startDate, endDate)
        val weeksBetween = daysBetween / 7
        return weeksBetween
    }

    fun formatGestationalAge(
        gestationalAgeInWeeks: Long,
        context: Context,
    ): String =
        when (gestationalAgeInWeeks) {
            0L -> "0 ${context.getString(R.string.week)}"
            1L -> "1 ${context.getString(R.string.week)}"
            else -> "$gestationalAgeInWeeks ${context.getString(R.string.weeks).lowercase()}"
        }

    /**
     * Returns age in 2 weeks 2 days when time is 2,2
     *
     * @param time : Combination of number of weeks and number of days
     * @param context : Android context
     */
    fun formatGestationalAge(
        time: Pair<Long, Long>,
        context: Context,
    ): String =
        "${time.first} ${context.getString(R.string.weeks).lowercase()}" +
            " " +
            "${time.second} ${context.getString(R.string.days).lowercase()} "

    fun parseDate(
        dateStr: String?,
        givenFormat: String = DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
    ): LocalDate? =
        try {
            dateStr?.let {
                LocalDate.parse(it, DateTimeFormatter.ofPattern(givenFormat))
            }
        } catch (_: Exception) {
            null
        }

    fun calculateAge(birthDateStr: String?): Int {
        val birthDate = parseDate(birthDateStr)
        return birthDate?.let {
            ChronoUnit.YEARS.between(it, LocalDate.now()).toInt()
        } ?: 0
    }

    private fun dateToWeeks(birthDateStr: String?): Int {
        val birthDate = parseDate(birthDateStr)
        return birthDate?.let {
            ChronoUnit.WEEKS.between(it, LocalDate.now()).toInt()
        } ?: 0
    }

    private fun dateToDays(birthDateStr: String?): Int {
        val birthDate = parseDate(birthDateStr)
        val currentDate = LocalDate.now()
        return birthDate?.let {
            val daysBetween = ChronoUnit.DAYS.between(it, currentDate).toInt()
            if (daysBetween < 0) 0 else daysBetween
        } ?: 0
    }

    fun getAgeDescription(
        birthDate: String,
        context: Context,
    ): String {
        val ageInYears = calculateAge(birthDateStr = birthDate)
        return when {
            ageInYears >= 5 -> {
                "$ageInYears ${context.getString(R.string.years).lowercase()}"
            }

            ageInYears in 1..4 -> {
                val ageInMonths = dateToMonths(birthDate) ?: 0
                if (ageInMonths == 1) {
                    "$ageInMonths ${context.getString(R.string.month)}"
                } else {
                    "$ageInMonths ${
                        context.getString(
                            R.string.months,
                        ).lowercase()
                    }"
                }
            }

            else -> {
                val ageInMonths = dateToMonths(birthDate) ?: 0
                if (ageInMonths < 1) {
                    val ageInWeeks = dateToWeeks(birthDate)
                    if (ageInWeeks < 1) {
                        val ageInDays = dateToDays(birthDate)
                        if (ageInDays == 1 || ageInDays == 0) {
                            "$ageInDays ${context.getString(R.string.day)}"
                        } else {
                            "$ageInDays ${
                                context.getString(
                                    R.string.days,
                                ).lowercase()
                            }"
                        }
                    } else {
                        if (ageInWeeks == 1) {
                            "$ageInWeeks ${context.getString(R.string.week)}"
                        } else {
                            "$ageInWeeks ${
                                context.getString(
                                    R.string.weeks,
                                ).lowercase()
                            }"
                        }
                    }
                } else {
                    if (ageInMonths == 1) {
                        "$ageInMonths ${
                            context.getString(R.string.month).lowercase()
                        }"
                    } else {
                        "$ageInMonths ${context.getString(R.string.months).lowercase()}"
                    }
                }
            }
        }
    }

    fun calculateGestationPastMonths(
        currentDateTimeInMillis: Long,
        days: Int,
    ): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentDateTimeInMillis
        calendar.add(Calendar.DAY_OF_MONTH, -days)
        return calendar.timeInMillis
    }

    fun getCalendarFromString(
        dateString: String,
        format: String = "yyyy-MM-dd HH:mm:ss",
    ): Long? {
        val dateFormat = SimpleDateFormat(format, Locale.getDefault())
        try {
            val date = dateFormat.parse(dateString)
            val timeInMillis = Calendar
                .getInstance()
                .apply {
                    time = date
                }.timeInMillis
            return timeInMillis
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun getCurrentDateTimeInUserTimeZone(inputFormat: String): String {
        val calendar = Calendar.getInstance()
        return parseDateWithTimeZone(inputFormat)?.format(calendar.time) ?: ""
    }

    fun parseDateWithTimeZone(inputFormat: String): SimpleDateFormat? {
        try {
            val sdf = SimpleDateFormat(inputFormat, Locale.ENGLISH)
            SecuredPreference.getTimeZoneId()?.let {
                sdf.timeZone = TimeZone.getTimeZone("GMT$it")
            }
            return sdf
        } catch (_: Exception) {
            return null
        }
    }

    fun getFormattedDateTimeForLastMeal(
        dayIndicator: String, // "YESTERDAY" or "TODAY"
        amPmIndicator: String, // "AM" or "PM"
        time: Pair<Int, Int>, // Pair of Hour and Minute
    ): String? {
        // Get the current date
        try {
            val currentDate = LocalDate.now()
            val date = when (dayIndicator.uppercase()) {
                YESTERDAY -> currentDate.minusDays(1)
                TODAY -> currentDate
                else -> currentDate
            }

            // Adjust hour based on AM/PM
            var hour = time.first
            if (amPmIndicator.uppercase() == "PM" && hour < 12) {
                hour += 12
            } else if (amPmIndicator.uppercase() == "AM" && hour == 12) {
                hour = 0 // Midnight case for AM
            }

            // Create LocalTime
            val localTime = LocalTime.of(hour, time.second)

            // Combine date and time to form LocalDateTime
            val localDateTime = LocalDateTime.of(date, localTime)

            // Define a formatter to match the required output format
            val formatter = DateTimeFormatter.ofPattern(CALENDAR_FORMAT)

            // Convert LocalDateTime to a ZonedDateTime using the system's default time zone
            val zonedDateTime = localDateTime.atZone(ZoneId.systemDefault())

            // Format the ZonedDateTime to the desired string format
            return zonedDateTime.format(formatter)
        } catch (_: Exception) {
            return null
        }
    }

    fun convertDateTimeToMillisUsingLocal(dateTimeString: String): Long =
        try {
            // Parse using ZonedDateTime to handle both UTC (Z) and offset-based timestamps
            val zonedDateTime = ZonedDateTime.parse(dateTimeString, DateTimeFormatter.ISO_DATE_TIME)
            zonedDateTime.toInstant().toEpochMilli() // Convert to UTC millis
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid date format: $dateTimeString", e)
        }

    fun getStartDate(): Long {
        val todayDate = Calendar.getInstance()
        todayDate.set(Calendar.HOUR, todayDate.getActualMinimum(Calendar.HOUR))
        todayDate.set(Calendar.HOUR_OF_DAY, todayDate.getActualMinimum(Calendar.HOUR_OF_DAY))
        todayDate.set(Calendar.MINUTE, todayDate.getActualMinimum(Calendar.MINUTE))
        todayDate.set(Calendar.SECOND, todayDate.getActualMinimum(Calendar.SECOND))
        todayDate.set(Calendar.MILLISECOND, todayDate.getActualMinimum(Calendar.MILLISECOND))
        return todayDate.timeInMillis
    }

    fun getEndDate(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR, calendar.getActualMaximum(Calendar.HOUR))
        calendar.set(Calendar.HOUR_OF_DAY, calendar.getActualMaximum(Calendar.HOUR_OF_DAY))
        calendar.set(Calendar.MINUTE, calendar.getActualMaximum(Calendar.MINUTE))
        calendar.set(Calendar.SECOND, calendar.getActualMaximum(Calendar.SECOND))
        calendar.set(Calendar.MILLISECOND, calendar.getActualMaximum(Calendar.MILLISECOND))
        return calendar.timeInMillis
    }

    fun isValidTimeForLastMealTime(
        hour: Int,
        minute: Int,
        amOrPm: String,
    ): Boolean {
        val adjustedHour = when (amOrPm) {
            "PM" -> if (hour == 12) hour else hour + 12
            "AM" -> if (hour == 12) 0 else hour
            else -> hour
        }
        val inputTime = LocalTime.of(adjustedHour, minute)
        val currentTime = LocalTime.now()
        return inputTime.isBefore(currentTime) || inputTime.equals(currentTime)
    }

    fun getTodayDateInMilliseconds(): Long {
        val calendar = Calendar.getInstance()

        calendar.set(Calendar.HOUR, 0)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun getCurrentYearAsDouble(): Double = Calendar.getInstance().get(Calendar.YEAR).toDouble()

    fun getEndDate(
        endDate: Date?,
        opFormat: String,
        inUTC: Boolean? = null,
    ): String? {
        endDate?.let { date ->
            val sdf = SimpleDateFormat(opFormat, Locale.ENGLISH)
            val calendar = Calendar.getInstance()
            calendar.time = date
            if (inUTC == true) {
                getUTCFormat()?.let {
                    calendar.timeZone = it
                    sdf.timeZone = it
                }
            }
            calendar.set(Calendar.HOUR, calendar.getActualMaximum(Calendar.HOUR))
            calendar.set(Calendar.HOUR_OF_DAY, calendar.getActualMaximum(Calendar.HOUR_OF_DAY))
            calendar.set(Calendar.MINUTE, calendar.getActualMaximum(Calendar.MINUTE))
            calendar.set(Calendar.SECOND, calendar.getActualMaximum(Calendar.SECOND))
            return sdf.format(calendar.time)
        }

        return null
    }

    fun validateForSameDate(
        year: Int,
        month: Int,
        dayOfMonth: Int,
    ): Boolean {
        val today = getTodayDate()
        val chosenDate = getTodayDate()
        chosenDate.set(Calendar.YEAR, year)
        chosenDate.set(Calendar.MONTH, month - 1)
        chosenDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)
        return !chosenDate.before(today) && !chosenDate.after(today)
    }

    private fun getTodayDate(): Calendar {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR, 0)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar
    }

    fun getDateStringInFormat(
        input: String?,
        ipFormat: String,
        opFormat: String,
    ): String {
        try {
            input?.let {
                if (it.isNotBlank()) {
                    val selectedCalendar = Calendar.getInstance()
                    val currentCalendar = Calendar.getInstance()
                    val inputFormat = SimpleDateFormat(ipFormat, Locale.ENGLISH)
                    val date = inputFormat.parse(it)
                    date?.let {
                        selectedCalendar.time = date
                        currentCalendar.set(Calendar.DAY_OF_MONTH, selectedCalendar.get(Calendar.DAY_OF_MONTH))
                        currentCalendar.set(Calendar.MONTH, selectedCalendar.get(Calendar.MONTH))
                        currentCalendar.set(Calendar.YEAR, selectedCalendar.get(Calendar.YEAR))
                        val outputFormat =
                            SimpleDateFormat(opFormat, Locale.ENGLISH)
                        return outputFormat.format(currentCalendar.time)
                    }
                }
            }
        } catch (_: Exception) {
            return ""
        }
        return ""
    }

    fun convertddMMMToddMM(inputDate: String): Triple<Int?, Int?, Int?> =
        try {
            val inputFormat = SimpleDateFormat(DATE_FORMAT_ddMMMyyyy, Locale.ENGLISH)
            val outputFormat = SimpleDateFormat(DATE_ddMMyyyy, Locale.ENGLISH)
            val date = inputFormat.parse(inputDate)
            getYearMonthAndDate(outputFormat.format(date))
        } catch (_: Exception) {
            Triple(null, null, null)
        }

    fun changeFormat(dateString: String): Date? = SimpleDateFormat(DATE_FORMAT_ddMMMyyyy, Locale.ENGLISH).parse(dateString)

    fun convertToTimestamp(dateString: String?): Long? =
        dateString?.let {
            val zonedDateTime = ZonedDateTime.parse(it, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            zonedDateTime.toInstant().toEpochMilli()
        }

    fun convertToTimestamp(
        dateString: String?,
        defaultValue: Long,
    ): Long =
        dateString?.let {
            val zonedDateTime = ZonedDateTime.parse(it, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            zonedDateTime.toInstant().toEpochMilli()
        } ?: defaultValue

    fun convertToTimestampWithoutZone(
        dateString: String?,
        isStartOfDay: Boolean,
    ): Long? =
        dateString?.let {
            // Define a custom formatter matching the input format
            val date = convertDateFormat(it, DATE_FORMAT_yyyyMMddHHmmssZZZZZ, GESTATIONALAGE_CALENDAR)
            val formatter = DateTimeFormatter.ofPattern(DATE_FORMAT_yyyyMMdd_HHmmss)

            // Parse the input string into LocalDateTime
            val localDateTime = LocalDateTime.parse("$date 00:00:00", formatter)

            // Adjust for start or end of the day
            val adjustedDateTime = if (isStartOfDay) {
                localDateTime
                    .withHour(0)
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0)
            } else {
                localDateTime
                    .withHour(23)
                    .withMinute(59)
                    .withSecond(59)
                    .withNano(999_999_999)
            }

            // Convert to timestamp in milliseconds
            adjustedDateTime.toEpochSecond(ZoneOffset.UTC) * 1000
        }

    fun getDaysDifference(due: Long): Int? =
        try {
            val diffInMillis = abs(System.currentTimeMillis() - due)
            TimeUnit.MILLISECONDS.toDays(diffInMillis).toInt()
        } catch (_: Exception) {
            null
        }

    fun getDaysDifference(dateString: String?): Long {
        if (dateString.isNullOrEmpty()) return -1

        return try {
            val sdf = SimpleDateFormat(DATE_FORMAT_yyyyMMddHHmmssZZZZZ, Locale.ENGLISH)
            val givenDate = sdf.parse(dateString) ?: return 0

            // Today date
            val today = Date()

            val diffMillis = today.time - givenDate.time
            diffMillis / (1000 * 60 * 60 * 24)
        } catch (ex: Exception) {
            -1
        }
    }

    fun getCurrentDayMonthYear(): Triple<Int, Int, Int> {
        val currentDate = LocalDate.now()
        return Triple(currentDate.dayOfMonth, currentDate.monthValue, currentDate.year)
    }

    fun dateToLong(dateString: String = getTodayDateDDMMYYYY()): Long? = convertToTimestampWithoutZone(dateString, false)

    fun calculateGestationalWeeks(dateGiven: String): Int? {
        try {
            val dateFormat = SimpleDateFormat(DATE_FORMAT_yyyyMMddHHmmssZZZZZ, Locale.ENGLISH)
            val date = dateFormat.parse(dateGiven)

            val calendar = Calendar.getInstance()
            val currentTime = calendar.timeInMillis
            calendar.time = date
            val targetTime = calendar.timeInMillis

            val diffInMillis = targetTime - currentTime
            val diffInWeeks = diffInMillis / (1000 * 60 * 60 * 24 * 7)

            return abs(diffInWeeks.toInt())
        } catch (_: Exception) {
            return null
        }
    }

    fun convertUTCString(
        utcDate: String?,
        output: String,
    ): String {
        val inputFormat = SimpleDateFormat(DATE_FORMAT_yyyyMMddHHmmss, Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC") // Parse as UTC

        val outputFormat = SimpleDateFormat(output, Locale.getDefault())
        outputFormat.timeZone = TimeZone.getDefault() // Convert to local time zone

        val date = utcDate?.let {
            inputFormat.parse(it) // Convert UTC string to Date object
        }
        return outputFormat.format(date) // Format to required output
    }

    fun compareDates(
        dobDate: String,
        selectedDate: String,
    ): Boolean {
        val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

        val dobZonedDateTime = ZonedDateTime.parse(dobDate, formatter)
        val selectedZonedDateTime = ZonedDateTime.parse(selectedDate, formatter)

        return selectedZonedDateTime >= dobZonedDateTime
    }

    fun getFormattedDate(daysToAdd: Int): String? =
        try {
            val calendar = Calendar.getInstance().apply {
                if (daysToAdd > 0) add(Calendar.DAY_OF_YEAR, daysToAdd)
            }
            val dateFormat = SimpleDateFormat(DATE_ddMMyyyy, Locale.getDefault())
            dateFormat.format(calendar.time)
        } catch (_: Exception) {
            null // Return null in case of an error
        }

    fun getFormattedDateAfterMonths(
        monthsToAdd: Long = 1,
        fromDate: LocalDate = LocalDate.now(),
        format: String = DATE_ddMMyyyy,
    ): String {
        val formatter = DateTimeFormatter.ofPattern(format)
        val newDate = fromDate.plusMonths(monthsToAdd)
        return newDate.format(formatter)
    }

    fun isDateFormat(
        inputFormat: String,
        value: String,
    ): Boolean {
        val dateFormat = SimpleDateFormat(inputFormat, Locale.ENGLISH)
        dateFormat.isLenient = false
        return try {
            dateFormat.parse(value)
            true
        } catch (_: ParseException) {
            false
        }
    }

    fun convertToRequiredFormat(dateStr: String): String {
        try {
            val inputFormatter = DateTimeFormatter.ofPattern(DATE_ddMMyyyy)
            val date = LocalDate.parse(dateStr, inputFormatter)
            val localDateTime = date.atStartOfDay()
            val outputFormatter = DateTimeFormatter.ofPattern(DATE_TIME_YYYYMMDDTHHmmssSSSZ)
            val formattedDate = localDateTime.format(outputFormatter)
            return formattedDate
        } catch (e: Exception) {
            e.printStackTrace()
            return "" // return empty or error message if parsing fails
        }
    }

    fun convertToMillis(
        dateStr: String,
        pattern: String,
    ): Long {
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        return sdf.parse(dateStr)?.time ?: 0L
    }

    fun formatDateToDDMMYYYY(dateString: String?): String {
        if (dateString.isNullOrBlank() || dateString == "--") {
            return "--" // fallback display string
        }

        return try {
            val parsedDate = OffsetDateTime.parse(dateString)
            val formatter = DateTimeFormatter.ofPattern(DATE_ddMMyyyy, Locale.getDefault())
            parsedDate.format(formatter)
        } catch (_: DateTimeParseException) {
            "--"
        }
    }

    fun getFormattedDateAfterDays(
        daysToAdd: Long = 14,
        fromDate: LocalDate = LocalDate.now(),
        format: String = DATE_ddMMyyyy,
    ): String {
        val formatter = DateTimeFormatter.ofPattern(format)
        val newDate = fromDate.plusDays(daysToAdd)
        return newDate.format(formatter)
    }

    fun formatDateToDisplayFormat(date: Long): String? {
        return try {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = date
            return getDateFormat().format(calendar.time)
        } catch (_: Exception) {
            null
        }
    }

    fun formatDate(date: Long): String? {
        return try {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = date
            return SimpleDateFormat(
                DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                Locale.ENGLISH,
            ).apply {
                getUTCFormat()?.let {
                    timeZone = it
                }
            }.format(calendar.time)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * True when [isoDateTimeString] is ISO-8601 with offset (e.g. `2025-12-23T18:30:00+00:00`)
     * and compares whether the [isoDateTimeString] is after threshold days
     */
    fun isIsoOffsetDateTimeOnLocalCalendarAfter(
        isoDateTimeString: String?,
        thresholdDays: Long = 1,
    ): Boolean {
        if (isoDateTimeString.isNullOrBlank()) return false
        return try {
            val visitLocalDate =
                OffsetDateTime
                    .parse(isoDateTimeString)
                    .atZoneSameInstant(ZoneId.systemDefault())
                    .toLocalDate()
            visitLocalDate > LocalDate.now(ZoneId.systemDefault()).minusDays(thresholdDays)
        } catch (_: DateTimeParseException) {
            false
        }
    }

    fun getCurrentDate(): Calendar {
        val calendar = Calendar.getInstance()

        calendar.set(Calendar.HOUR, 0)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar
    }

    private fun convertBengaliToEnglishNumbers(input: String): String {
        val bengaliDigits = "০১২৩৪৫৬৭৮৯"
        val englishDigits = "0123456789"
        return input
            .map { char ->
                val index = bengaliDigits.indexOf(char)
                if (index != -1) englishDigits[index] else char
            }.joinToString("")
    }

    private fun convertBengaliMonthToEnglish(input: String): String {
        val monthMap = mapOf(
            "জানুয়ারি" to "Jan",
            "জানু." to "Jan",
            "ফেব্রুয়ারি" to "Feb",
            "ফেব." to "Feb",
            "মার্চ" to "Mar",
            "মা." to "Mar",
            "এপ্রিল" to "Apr",
            "এপ্রি." to "Apr",
            "মে" to "May",
            "জুন" to "Jun",
            "জুলাই" to "Jul",
            "জুল." to "Jul",
            "আগস্ট" to "Aug",
            "আগ." to "Aug",
            "সেপ্টেম্বর" to "Sep",
            "সেপ." to "Sep",
            "অক্টোবর" to "Oct",
            "অক্টো." to "Oct",
            "নভেম্বর" to "Nov",
            "নভে." to "Nov",
            "ডিসেম্বর" to "Dec",
            "ডিসে." to "Dec",
        )

        var updatedDate = input
        monthMap.forEach { (bengali, english) ->
            updatedDate = updatedDate.replace(bengali, english)
        }
        return updatedDate
    }

    fun convertToIsoFormat(inputDate: String): String {
        return try {
            val convertedDate = convertBengaliToEnglishNumbers(inputDate)
            val englishMonthDate = convertBengaliMonthToEnglish(convertedDate)

            val inputFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH)
            val outputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH)

            // Parse the date
            val date = inputFormat.parse(englishMonthDate) ?: return ""

            // Get today's date
            val calendar = Calendar.getInstance()
            val today = Calendar.getInstance().apply {
                time = date
            }

            // Check if the parsed date is today's date
            if (calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
            ) {
                // If the date is today, set the current time
                today.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY))
                today.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE))
                today.set(Calendar.SECOND, calendar.get(Calendar.SECOND))
                today.set(Calendar.MILLISECOND, calendar.get(Calendar.MILLISECOND))
            }

            outputFormat.format(today.time)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun getCurrentDateTime(inputFormat: String): String {
        val calendar = Calendar.getInstance()
        return SimpleDateFormat(inputFormat, Locale.ENGLISH).format(calendar.time)
    }

    fun getTwoYearsLater(): Long {
        val chosenDate = Calendar.getInstance().apply {
            add(Calendar.YEAR, 2)
        }
        return chosenDate.timeInMillis
    }

    fun convertddMMMToddMM(
        inputDate: String,
        outputDateFormat: String? = null,
    ): Triple<Int?, Int?, Int?> =
        try {
            val inputFormat = SimpleDateFormat(DATE_FORMAT_ddMMMyyyy, Locale.ENGLISH)
            val outputFormat = SimpleDateFormat(outputDateFormat ?: DATE_ddMMyyyy, Locale.ENGLISH)
            val date = inputFormat.parse(inputDate)
            getYearMonthAndDate(outputFormat.format(date))
        } catch (exception: Exception) {
            Triple(null, null, null)
        }

    fun convertDateTimeToDateWithDay(
        inputText: String?,
        inputFormat: String,
        outputFormat: String,
        inUserTimeZone: Boolean? = false,
        inUTC: Boolean? = null,
    ): Pair<String, String> {
        try {
            inputText?.let {
                if (it.isNotBlank()) {
                    var userTimeZone: TimeZone? = null
                    val isTimeZoneFormat = inputFormat == DATE_FORMAT_yyyyMMddHHmmssZZZZZ
                    if (isTimeZoneFormat || inUserTimeZone == true) {
                        getTimeZoneInput(inputText, isTimeZoneFormat)?.let { timeZone ->
                            userTimeZone = timeZone
                        }
                    } else if (inUTC == true) {
                        userTimeZone = getUTCFormat()
                    }

                    val sdfInput = SimpleDateFormat(inputFormat, Locale.ENGLISH)
                    userTimeZone?.let {
                        sdfInput.timeZone = userTimeZone
                    }
                    val date = sdfInput.parse(it)
                    date?.let {
                        val sdfOutput = SimpleDateFormat(outputFormat, Locale.ENGLISH)
                        val sdfDay = SimpleDateFormat("EEEE", Locale.ENGLISH)
                        userTimeZone?.let {
                            sdfOutput.timeZone = userTimeZone
                            sdfDay.timeZone = userTimeZone
                        }

                        val formattedDate = sdfOutput.format(date)
                        val dayOfWeek = sdfDay.format(date)
                        return Pair(formattedDate, dayOfWeek)
                    }
                }
            }
        } catch (e: Exception) {
            return Pair("", "")
        }
        return Pair("", "")
    }

    fun calculateDaysToCurrentDateAndDay(
        days: Int,
        format: String,
    ): Pair<String, String> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, days)
        val date = calendar.time
        val sdfDate = SimpleDateFormat(format, Locale.ENGLISH).format(date)
        val sdfDay = SimpleDateFormat("EEEE", Locale.ENGLISH).format(date) // Full day name
        return Pair(sdfDate, sdfDay)
    }

    fun formatDateStringLegacy(input: String): String {
        // Parse the input date string
        val isoFormat = SimpleDateFormat(DATE_FORMAT_yyyyMMddHHmmssZZZZZ, Locale.getDefault())
        val date = isoFormat.parse(input)

        // Define the desired output format
        val outputFormat = SimpleDateFormat(DATE_DD_MMM_YYYY, Locale.getDefault())

        // Format the date into the desired format
        return outputFormat.format(date)
    }

    fun getAge(dateString: String?): String {
        return try {
            val birthDate = dateString
                ?.let { OffsetDateTime.parse(it).toLocalDate() }
                ?: return "-"

            val currentDate = LocalDate.now(ZoneId.systemDefault())
            Period.between(birthDate, currentDate).years.toString()
        } catch (e: Exception) {
            "-"
        }
    }

    fun getDateDDMMMYYYY(): SimpleDateFormat = SimpleDateFormat(DATE_FORMAT_ddMMMyyyy, Locale.ENGLISH)
}
