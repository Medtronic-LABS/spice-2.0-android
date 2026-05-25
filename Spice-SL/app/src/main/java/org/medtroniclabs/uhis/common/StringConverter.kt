package org.medtroniclabs.uhis.common

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.ToNumberPolicy
import com.google.gson.reflect.TypeToken
import okhttp3.ResponseBody
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.data.ErrorResponse
import org.medtroniclabs.uhis.data.registration.DuplicationNudgeModel
import org.medtroniclabs.uhis.data.registration.PatientModel
import org.medtroniclabs.uhis.mappingkey.Screening
import java.lang.reflect.Type

object StringConverter {
    fun convertGivenMapToString(map: HashMap<*, *>?): String? {
        val gson = Gson()
        return gson.toJson(map)
    }

    fun stringToMap(json: String): HashMap<String, Any> {
        val gson = Gson()
        val type = object : TypeToken<HashMap<String, Any>>() {}.type
        return gson.fromJson(json, type)
    }

    fun getErrorMessage(errorBody: ResponseBody?): String? {
        if (errorBody == null) {
            return null
        }
        return try {
            val errorResponse =
                Gson().fromJson(errorBody.string(), ErrorResponse::class.java)
            errorResponse?.message
        } catch (e: Exception) {
            null
        }
    }

    fun convertStringToListOfMap(data: String): ArrayList<Map<String, Any>>? =
        try {
            val gson = Gson()
            val type: Type = object : TypeToken<ArrayList<Map<String, Any>>>() {}.type
            val map: ArrayList<Map<String, Any>>? = gson.fromJson(data, type)
            map
        } catch (e: Exception) {
            null
        }

    fun convertStringToMap(data: String): Map<String, Any>? =
        try {
            val gson: Gson = GsonBuilder()
                .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
                .create()
            val type: Type = object : TypeToken<Map<String, Any>>() {}.type
            val map: Map<String, Any>? = gson.fromJson(data, type)
            map
        } catch (e: Exception) {
            null
        }

    fun getPHQ4ReadableName(
        score: Int,
        context: Context,
    ): String =
        when (score) {
            in 0..3 -> {
                context.getString(R.string.phq4_normal, score)
            }

            in 4..5 -> {
                context.getString(R.string.phq4_mild, score)
            }

            in 6..8 -> {
                context.getString(R.string.phq4_moderate, score)
            }

            in 9..12 -> {
                context.getString(R.string.phq4_severe, score)
            }

            else -> score.toString()
        }

    fun getPHQ9ReadableName(
        score: Int,
        context: Context,
    ): String =
        when (score) {
            in 1..4 -> {
                context.getString(R.string.phq9_minimal, score)
            }

            in 5..9 -> {
                context.getString(R.string.phq4_mild, score)
            }

            in 10..14 -> {
                context.getString(R.string.phq4_moderate, score)
            }

            in 15..19 -> {
                context.getString(R.string.phq9_moderately_severe, score)
            }

            in 20..27 -> {
                context.getString(R.string.phq4_severe, score)
            }

            else -> score.toString()
        }

    fun getGAD7ReadableName(
        score: Int,
        context: Context,
    ): String =
        when (score) {
            in 1..2 -> context.getString(R.string.phq4_normal, score)
            in 3..5 -> context.getString(R.string.phq4_mild, score)
            in 6..8 -> context.getString(R.string.phq4_moderate, score)
            in 9..21 -> context.getString(R.string.phq4_severe, score)
            else -> score.toString()
        }

    fun getJsonObject(inputJson: String): JsonObject = Gson().fromJson(inputJson, JsonObject::class.java)

    fun appendTexts(
        firstText: String,
        vararg input: String?,
        separator: String? = null,
    ): String {
        val strBuilder = StringBuilder(firstText)
        for (text in input) {
            text?.let {
                if (text.isNotBlank()) {
                    if (separator.isNullOrBlank()) {
                        strBuilder.append(" $it")
                    } else {
                        strBuilder.append(" $separator $it")
                    }
                }
            }
        }
        return strBuilder.trim().toString()
    }

    fun getDuplicatePatientMap(errorBody: ResponseBody?): HashMap<String, Any>? =
        try {
            var returnMap: HashMap<String, Any>? = null
            errorBody?.let { err ->
                val errorResponse = Gson().fromJson(err.string(), Map::class.java)
                if (errorResponse.containsKey(Screening.Entity)) {
                    errorResponse[Screening.Entity]?.let { entity ->
                        if (entity is Map<*, *> && entity.contains(Screening.PATIENT_DETAILS)) {
                            entity[Screening.PATIENT_DETAILS]?.let { details ->
                                (details as? Map<String, Any>)?.let { map ->
                                    returnMap = HashMap(map.toMutableMap())
                                }
                            }
                        }
                    }
                }
            }
            returnMap
        } catch (e: Exception) {
            null
        }

    /**
     * Converts a list/array to JSON string for database storage
     */
    fun convertListToString(value: Any?): String? =
        when (value) {
            is List<*> -> {
                val stringList = value.filterIsInstance<String>()
                if (stringList.isNotEmpty()) {
                    Gson().toJson(stringList)
                } else {
                    null
                }
            }

            is Array<*> -> {
                val stringList = value.filterIsInstance<String>()
                if (stringList.isNotEmpty()) {
                    Gson().toJson(stringList.toList())
                } else {
                    null
                }
            }

            else -> null
        }

    /**
     * Parses a JSON string to a List
     */
    fun <T> convertStringToList(jsonString: String): List<T> =
        try {
            val type: Type = object : TypeToken<List<T>>() {}.type
            Gson().fromJson(jsonString, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

    fun getFormattedData(
        context: Context,
        errorBody: ResponseBody?,
        isFromEnrollment: Boolean?,
    ): Pair<String, PatientModel?>? {
        if (errorBody == null) {
            return null
        }
        return try {
            val data = Gson().fromJson(
                errorBody.string(),
                DuplicationNudgeModel::class.java,
            )
            val message = data.message ?: context.getString(R.string.duplicate_record_found)
            val myEntity = data.entity
            val patientModel = if (isFromEnrollment == true) {
                myEntity?.enrollment?.copy(
                    operatingUnitId = myEntity.operatingUnitId,
                    accountId = myEntity.accountId,
                )
            } else {
                myEntity
            }
            Pair(message, patientModel)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
