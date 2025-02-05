package com.medtroniclabs.opensource.common

import android.content.Context
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.data.model.CalendarPeriod
import com.medtroniclabs.opensource.formgeneration.config.DefinedParams
import com.medtroniclabs.opensource.mappingkey.Screening.TODAY
import com.medtroniclabs.opensource.mappingkey.Screening.YESTERDAY
import org.joda.time.PeriodType
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.Period
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.math.abs

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
    const val TIME_FORMAT_hhmm = "hh:mm"
    const val TIME_FORMAT_hhmma = "hh:mm a"
    const val CALENDAR_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX"
    const val GESTATIONALAGE_CALENDAR = "yyyy-MM-dd"
    const val DATE_FORMAT_ddMMyy_GRAPH = "dd-MM-yyyy"
    const val DATE_TIME_yyyyMMddTHHmmssSSSXXX = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"


    fun getYearMonthAndWeek(
        inputDate: String,
        inputFormat: String = DATE_FORMAT_ddMMyyyy
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
        } catch (exception: Exception) {
            return Pair(null, Triple(null, null, null))
        }
    }

    fun getV2YearMonthAndWeek(
        dob: String,
        format: String = DATE_FORMAT_yyyyMMddHHmmssZZZZZ
    ): CalendarPeriod {
        val dobDate = LocalDate.parse(dob, DateTimeFormatter.ofPattern(format))
        val today = LocalDate.now()

        val period = Period.between(dobDate, today)

        val years = period.years
        val months = period.months

        // Calculate the weeks by finding the number of days and then converting to weeks
        val daysBetween = ChronoUnit.DAYS.between(
            dobDate.plusYears(years.toLong()).plusMonths(months.toLong()),
            today
        )
        val weeks = (daysBetween / 7).toInt()
        val days = (daysBetween % 7).toInt()

        return CalendarPeriod(years, months, weeks, days)
    }

    fun getYearMonthAndDate(
        dateString: String,
        defaultFormat: SimpleDateFormat = getDateDDMMYYYY()
    ): Triple<Int?, Int?, Int?> {
        try {
            val date = formatStringToDate(dateString, defaultFormat)
            date?.let {
                val cal = Calendar.getInstance()
                cal.time = date
                return Triple(
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DATE)
                )
            }
        } catch (exception: Exception) {
            return Triple(null, null, null)
        }
        return Triple(null, null, null)
    }

    fun getTimeInMilliFromDate(
        dateString: String,
        defaultFormat: SimpleDateFormat = getDateDDMMYYYY()
    ): Long? {
        try {
            val date = formatStringToDate(dateString, defaultFormat)
            date?.let {
                val cal = Calendar.getInstance()
                cal.time = date
                return cal.timeInMillis
            }
        } catch (exception: Exception) {
            return null
        }
        return null
    }

    fun formatStringToDate(dateString: String, format: SimpleDateFormat): Date? {
        return format.parse(dateString)
    }

    fun getDatePatternDDMMYYYY() = SimpleDateFormat(DATE_FORMAT_ddMMyyyy, Locale.ENGLISH)

    fun getDateDDMMYYYY(): SimpleDateFormat {
        return SimpleDateFormat(DATE_ddMMyyyy, Locale.ENGLISH)
    }

    fun getDateString(
        time: Long,
        inputFormat: String? = null,
        outputFormat: String? = null
    ): String {
        val date = Date(time)
        val format = SimpleDateFormat(
            inputFormat,
            Locale.ENGLISH
        )
        return format.format(date).let {
            if (outputFormat == DATE_FORMAT_yyyyMMddHHmmssZZZZZ) {
                "${format.format(date)}${DefinedParams.DOBString}"
            } else
                format.format(date)
        }
    }

    fun calculateBirthDate(years: Int, months: Int, weeks: Int): String {
        var localDate = OffsetDateTime.now()

        val days = weeks * 7
        localDate = localDate.minusYears(years.toLong())
            .minusMonths(months.toLong())
            .minusDays(days.toLong())
            .withHour(0)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)

        val resul = localDate.format(DateTimeFormatter.ofPattern(DATE_FORMAT_yyyyMMddHHmmss))
        return "$resul+00:00"
    }

    fun calculateAge(birthYear: Int): Int {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        return currentYear - birthYear
    }

    fun calculateAgeInMonths(birthDateString: String): Pair<Int, Date>? {
        val startDate = formatStringToDate(
            birthDateString, SimpleDateFormat(
                DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                Locale.ENGLISH
            )
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

    fun getMinDateBasedOnDeliveryDate(currentDateTimeInMillis: Triple<Int, Int, Int>?): Long {
        if (currentDateTimeInMillis == null) return 0L
        val (year, month, day) = currentDateTimeInMillis
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, day)
        calendar.add(Calendar.MONTH, 0)
        return calendar.timeInMillis
    }

    fun convertDateToLong(date: String?, format: String? = DATE_FORMAT_yyyyMMddHHmmssZZZZZ): Long? {
        try {
            val testedDate = date?.let { dateStr ->
                SimpleDateFormat(
                    format,
                    Locale.ENGLISH
                ).parse(dateStr)?.time
            }
            return testedDate ?: 0L
        } catch (e: Exception) {
            return null
        }
    }

    fun convertDateTimeToDate(
        inputText: String?,
        inputFormat: String,
        outputFormat: String,
        inUserTimeZone: Boolean? = false,
        inUTC: Boolean? = null
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
        } catch (e: Exception) {
            return ""
        }
        return ""
    }

    private fun getTimeZoneInput(inputText: String, timeZoneFormat: Boolean): TimeZone? {
        var timeZoneInput = "GMT+00:00" // Take from secured preference in future
        if (timeZoneInput.isBlank() && timeZoneFormat) {
            timeZoneInput = "GMT${
                if (inputText.contains("+")) inputText.substring(inputText.indexOf("+"))
                else inputText.substring(inputText.indexOf("-"))
            }"
        } else if (!timeZoneInput.isNullOrBlank())
            timeZoneInput = "GMT$timeZoneInput"
        return TimeZone.getTimeZone(timeZoneInput)
    }

    private fun getUTCFormat(): TimeZone? {
        return TimeZone.getTimeZone("GMT+00:00")
    }

    fun convertedMMMToddMM(inputDate: String): Triple<Int?, Int?, Int?> {
        return try {
            val inputFormat = SimpleDateFormat(DATE_ddMMyyyy, Locale.ENGLISH)
            val outputFormat = SimpleDateFormat(DATE_ddMMyyyy, Locale.ENGLISH)
            val date = inputFormat.parse(inputDate)
            getYearMonthAndDate(outputFormat.format(date))
        } catch (exception: Exception) {
            Triple(null, null, null)
        }
    }

    fun convertDateFormat(
        inputDateString: String,
        inputDateFormat: String,
        outputDateFormat: String
    ): String {
        val inputFormat = SimpleDateFormat(inputDateFormat, Locale.getDefault())

        return try {
            val date = inputFormat.parse(inputDateString)
            val outputFormat = SimpleDateFormat(outputDateFormat, Locale.getDefault())
            date?.let { outputFormat.format(it) } ?: " "
        } catch (e: Exception) {
            " "
        }
    }

    fun dateToMonths(
        dateString: String,
        givenFormat: String = DATE_FORMAT_yyyyMMddHHmmssZZZZZ
    ): Int? {
        return try {
            val formatter = DateTimeFormatter.ofPattern(givenFormat)
            val givenDateTime = LocalDateTime.parse(dateString, formatter)
            val currentDateTime = LocalDateTime.now()
            val months =
                givenDateTime.toLocalDate().until(currentDateTime.toLocalDate()).toTotalMonths()
            months.toInt()
        } catch (e: Exception) {
            null
        }
    }

    fun dateToMonthsAndWeeks(
        dateString: String,
        givenFormat: String = DATE_FORMAT_yyyyMMddHHmmssZZZZZ
    ): Pair<Int, Int>? {
        return try {
            val formatter = DateTimeFormatter.ofPattern(givenFormat)
            val givenDateTime = LocalDateTime.parse(dateString, formatter)
            val currentDateTime = LocalDateTime.now()

            val totalMonths = ChronoUnit.MONTHS.between(givenDateTime, currentDateTime).toInt()

            val adjustedGivenDate = givenDateTime.plusMonths(totalMonths.toLong())
            val weeks = ChronoUnit.WEEKS.between(adjustedGivenDate, currentDateTime).toInt()

            Pair(totalMonths, weeks)
        } catch (e: Exception) {
            Pair(0, 0)
        }
    }

    fun calculateAge(
        birthDateString: String,
        givenFormat: String = DATE_FORMAT_yyyyMMddHHmmssZZZZZ
    ): Int {
        return try {
            val yearConvert = 1000L * 60 * 60 * 24 * 365.25
            val dateFormat = SimpleDateFormat(givenFormat, Locale.getDefault())
            val birthDate = dateFormat.parse(birthDateString)
            val today = Calendar.getInstance().time
            val diff = today.time - birthDate.time
            val age = diff / yearConvert
            age.toInt()
        } catch (e: Exception) {
            0
        }
    }

    fun calculateAgeAndWeek(
        birthDateString: String,
        givenFormat: String = DATE_FORMAT_yyyyMMddHHmmssZZZZZ
    ): Pair<Int, Int> {
        return try {
            val yearInMillis = 1000L * 60 * 60 * 24 * 365.25
            val weekInMillis = 1000L * 60 * 60 * 24 * 7

            val dateFormat = SimpleDateFormat(givenFormat, Locale.getDefault())
            val birthDate = dateFormat.parse(birthDateString)
            val today = Calendar.getInstance().time

            val diff = today.time - (birthDate?.time ?: 0)

            val ageYears = (diff / yearInMillis).toInt()
            val remainingMillisAfterYears = diff % yearInMillis
            val ageWeeks = (remainingMillisAfterYears / weekInMillis).toInt()
            Pair(ageYears, ageWeeks)
        } catch (e: Exception) {
            Pair(0, 0)
        }
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

    fun getDateFormat(): SimpleDateFormat {
        return SimpleDateFormat(DATE_ddMMyyyy, Locale.getDefault())
    }

    fun getLastMenstrualDate(clinicalDate: String): Calendar {
        // Define the format of the input date string
        val lastMenstrualDateString = convertDateFormat(
            clinicalDate, DATE_FORMAT_yyyyMMddHHmmssZZZZZ, DATE_ddMMyyyy
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

    fun addDaysToDate(date: Date, days: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.add(Calendar.DAY_OF_MONTH, days)
        return calendar.time
    }

    fun addMonthsToDate(date: Date, months: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.add(Calendar.MONTH, months)
        return calendar.time
    }

    fun convertStringToDate(dateString: String, format: String): Date? {
        val dateFormat = SimpleDateFormat(format, Locale.getDefault())
        return dateFormat.parse(dateString)
    }


    fun getDateStringFromDate(date: Date, format: String): String {
        val dateFormat = SimpleDateFormat(format, Locale.getDefault())
        return dateFormat.format(date)
    }

    fun convertDateToStringWithUTC(date: Date, format: String): String {
        val instant = date.toInstant()
        var offsetDateTime = OffsetDateTime.ofInstant(instant, ZoneOffset.systemDefault())

        offsetDateTime = offsetDateTime.withHour(0)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)

        val result = offsetDateTime.format(DateTimeFormatter.ofPattern(DATE_FORMAT_yyyyMMddHHmmss))
        return "$result+00:00"
    }

    fun daysBetweenDates(startDate: Date, endDate: Date): Long {
        val diffInMillis = abs(endDate.time - startDate.time)
        return TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS)
    }

    fun getTodayDateDDMMYYYY(): String {
        val calendar = Calendar.getInstance()
        return getDateString(calendar.time.time, DATE_FORMAT_yyyyMMddHHmmssZZZZZ)
    }

    fun getCurrentDateTimeInMillis(): Long = Instant.now().toEpochMilli()

    fun calculateGestationalAge(lmpDate: LocalDate): Long {
        val currentDate = LocalDate.now()
        return ChronoUnit.WEEKS.between(lmpDate, currentDate)
    }

    fun calculateGestationalAgeWeeks(startDateString: String, endDateString: String): Long {
        val formatter = DateTimeFormatter.ISO_DATE_TIME
        val startDate = ZonedDateTime.parse(startDateString, formatter)
        val endDate = ZonedDateTime.parse(endDateString, formatter)
        val daysBetween = ChronoUnit.DAYS.between(startDate, endDate)
        val weeksBetween = daysBetween / 7
        return weeksBetween
    }

    fun formatGestationalAge(gestationalAgeInWeeks: Long, context: Context): String {
        return when (gestationalAgeInWeeks) {
            0L -> "0 ${context.getString(R.string.week)}"
            1L -> "1 ${context.getString(R.string.week)}"
            else -> "$gestationalAgeInWeeks ${context.getString(R.string.weeks).lowercase()}"
        }
    }

    fun parseDate(
        dateStr: String?,
        givenFormat: String = DATE_FORMAT_yyyyMMddHHmmssZZZZZ
    ): LocalDate? {
        return try {
            dateStr?.let {
                LocalDate.parse(it, DateTimeFormatter.ofPattern(givenFormat))
            }
        } catch (e: Exception) {
            null
        }
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

    fun getAgeDescription(birthDate: String, context: Context): String {
        val ageInYears = calculateAge(birthDateStr = birthDate)
        return when {
            ageInYears >= 5 -> {
                "$ageInYears ${context.getString(R.string.years).lowercase()}"
            }

            ageInYears in 1..4 -> {
                val ageInMonths = dateToMonths(birthDate) ?: 0
                if (ageInMonths == 1) "$ageInMonths ${context.getString(R.string.month)}" else "$ageInMonths ${
                    context.getString(
                        R.string.months
                    ).lowercase()
                }"
            }

            else -> {
                val ageInMonths = dateToMonths(birthDate) ?: 0
                if (ageInMonths < 1) {
                    val ageInWeeks = dateToWeeks(birthDate)
                    if (ageInWeeks < 1) {
                        val ageInDays = dateToDays(birthDate)
                        if (ageInDays == 1 || ageInDays == 0) "$ageInDays ${context.getString(R.string.day)}" else "$ageInDays ${
                            context.getString(
                                R.string.days
                            ).lowercase()
                        }"
                    } else {
                        if (ageInWeeks == 1) "$ageInWeeks ${context.getString(R.string.week)}" else "$ageInWeeks ${
                            context.getString(
                                R.string.weeks
                            ).lowercase()
                        }"
                    }
                } else {
                    if (ageInMonths == 1) "$ageInMonths ${
                        context.getString(R.string.month).lowercase()
                    }" else "$ageInMonths ${context.getString(R.string.months).lowercase()}"
                }
            }
        }
    }

    fun calculateGestationPastMonths(currentDateTimeInMillis: Long, days: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentDateTimeInMillis
        calendar.add(Calendar.DAY_OF_MONTH, -days)
        return calendar.timeInMillis
    }

    fun getCalendarFromString(dateString: String, format: String = "yyyy-MM-dd HH:mm:ss"): Long? {
        val dateFormat = SimpleDateFormat(format, Locale.getDefault())
        try {
            val date = dateFormat.parse(dateString)
            val timeInMillis = Calendar.getInstance().apply {
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
    fun parseDateWithTimeZone(inputFormat : String): SimpleDateFormat? {
        try {
            val sdf = SimpleDateFormat(inputFormat, Locale.ENGLISH)
            SecuredPreference.getTimeZoneId()?.let {
                sdf.timeZone = TimeZone.getTimeZone("GMT$it")
            }
            return sdf
        }catch (_:Exception){
            return null
        }
    }

    fun getFormattedDateTimeForLastMeal(
        dayIndicator: String, // "YESTERDAY" or "TODAY"
        amPmIndicator: String, // "AM" or "PM"
        time: Pair<Int, Int> // Pair of Hour and Minute
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
        } catch (e: Exception) {
            return  null
        }

    }

    fun convertDateTimeToMillisUsingLocal(dateTimeString: String): Long {
        val formatter = DateTimeFormatter.ofPattern(CALENDAR_FORMAT)
        val dateTime = LocalDateTime.parse(dateTimeString, formatter)
        val offset = ZoneOffset.of(dateTimeString.takeLast(6))
        val instant = dateTime.toInstant(offset)
        return instant.toEpochMilli()
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

    fun isValidTimeForLastMealTime(hour: Int, minute: Int, amOrPm: String): Boolean {
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

    fun getCurrentYearAsDouble(): Double {
        return Calendar.getInstance().get(Calendar.YEAR).toDouble()
    }

    fun getEndDate(endDate: Date?, opFormat: String, inUTC: Boolean? = null): String? {
        endDate?.let { date ->
            val sdf = SimpleDateFormat(opFormat, Locale.ENGLISH)
            val calendar = Calendar.getInstance()
            calendar.time = date
            if (inUTC == true)
                getUTCFormat()?.let {
                    calendar.timeZone = it
                    sdf.timeZone = it
                }
            calendar.set(Calendar.HOUR, calendar.getActualMaximum(Calendar.HOUR))
            calendar.set(Calendar.HOUR_OF_DAY, calendar.getActualMaximum(Calendar.HOUR_OF_DAY))
            calendar.set(Calendar.MINUTE, calendar.getActualMaximum(Calendar.MINUTE))
            calendar.set(Calendar.SECOND, calendar.getActualMaximum(Calendar.SECOND))
            return sdf.format(calendar.time)
        }

        return null
    }

    fun validateForSameDate(year: Int, month: Int, dayOfMonth: Int): Boolean {
        val today = getTodayDate()
        val chosenDate = getTodayDate()
        chosenDate.set(Calendar.YEAR, year)
        chosenDate.set(Calendar.MONTH, month - 1)
        chosenDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)
        if (!chosenDate.before(today) && !chosenDate.after(today))
            return true
        return false
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

    fun getDateStringInFormat(input: String?, ipFormat: String, opFormat: String): String {
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
        } catch (e: Exception) {
            return ""
        }
        return ""
    }

    fun convertddMMMToddMM(inputDate: String): Triple<Int?, Int?, Int?> {
        return try{
            val inputFormat = SimpleDateFormat(DATE_FORMAT_ddMMMyyyy, Locale.ENGLISH)
            val outputFormat = SimpleDateFormat(DATE_ddMMyyyy, Locale.ENGLISH)
            val date = inputFormat.parse(inputDate)
            getYearMonthAndDate(outputFormat.format(date))
        }catch (exception:Exception){
            Triple(null, null, null)
        }
    }
    fun changeFormat(dateString: String): Date? =
        SimpleDateFormat(DATE_FORMAT_ddMMMyyyy, Locale.ENGLISH).parse(dateString)

    fun convertToTimestamp(dateString: String?): Long? {
        return dateString?.let {
            val zonedDateTime = ZonedDateTime.parse(it, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            zonedDateTime.toInstant().toEpochMilli()
        }
    }

    fun convertToTimestampWithoutZone(dateString: String?, isStartOfDay: Boolean): Long? {
        return dateString?.let {
            // Define a custom formatter matching the input format
            val date = convertDateFormat(it, DATE_FORMAT_yyyyMMddHHmmssZZZZZ, GESTATIONALAGE_CALENDAR)
            val formatter = DateTimeFormatter.ofPattern(DATE_FORMAT_yyyyMMdd_HHmmss)

            // Parse the input string into LocalDateTime
            val localDateTime = LocalDateTime.parse("$date 00:00:00", formatter)

            // Adjust for start or end of the day
            val adjustedDateTime = if (isStartOfDay) {
                localDateTime.withHour(0).withMinute(0).withSecond(0).withNano(0)
            } else {
                localDateTime.withHour(23).withMinute(59).withSecond(59).withNano(999_999_999)
            }

            // Convert to timestamp in milliseconds
            adjustedDateTime.toEpochSecond(ZoneOffset.UTC) * 1000
        }
    }

    fun getDaysDifference(due: Long): Int? {
        return try {
            val diffInMillis = abs(System.currentTimeMillis() - due)
            TimeUnit.MILLISECONDS.toDays(diffInMillis).toInt()
        } catch (e: Exception) {
            null
        }
    }

    fun getCurrentDayMonthYear(): Triple<Int, Int, Int> {
        val currentDate = LocalDate.now()
        return Triple(currentDate.dayOfMonth, currentDate.monthValue, currentDate.year)
    }
    fun dateToLong(dateString: String = getTodayDateDDMMYYYY()): Long? {
        return convertToTimestampWithoutZone(dateString, false)
    }

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
        } catch (e: Exception) {
            return null
        }
    }
}
