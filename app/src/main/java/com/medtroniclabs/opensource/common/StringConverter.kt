package com.medtroniclabs.opensource.common

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.ToNumberPolicy
import com.google.gson.reflect.TypeToken
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.data.ErrorResponse
import com.medtroniclabs.opensource.mappingkey.Screening
import okhttp3.ResponseBody
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
        if (errorBody == null)
            return null
        return try {
            val errorResponse =
                Gson().fromJson(errorBody.string(), ErrorResponse::class.java)
            errorResponse?.message
        } catch (e: Exception) {
            null
        }
    }

    fun convertStringToListOfMap(data: String): ArrayList<Map<String, Any>>? {
        return try {
            val gson = Gson()
            val type: Type = object : TypeToken<ArrayList<Map<String, Any>>>() {}.type
            val map: ArrayList<Map<String, Any>>? = gson.fromJson(data, type)
            map
        } catch (e: Exception) {
            null
        }
    }

    fun convertStringToMap(data: String): Map<String, Any>? {
        return try {
            val gson : Gson = GsonBuilder()
                .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
                .create()
            val type: Type = object : TypeToken<Map<String, Any>>() {}.type
            val map: Map<String, Any>? = gson.fromJson(data, type)
            map
        } catch (e: Exception) {
            null
        }
    }

    fun getPHQ4ReadableName(score: Int, context: Context): String {
        return when (score) {
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
    }
    fun getPHQ9ReadableName(score: Int, context: Context): String {
        return when (score) {
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
    }

    fun getGAD7ReadableName(score: Int, context: Context): String {
        return when (score) {
            in 1..2 -> context.getString(R.string.phq4_normal, score)
            in 3..5 -> context.getString(R.string.phq4_mild, score)
            in 6..8 -> context.getString(R.string.phq4_moderate, score)
            in 9..21 -> context.getString(R.string.phq4_severe, score)
            else -> score.toString()
        }
    }

    fun getJsonObject(inputJson: String): JsonObject {
        return Gson().fromJson(inputJson, JsonObject::class.java)
    }

    fun appendTexts(firstText: String, vararg input: String?, separator: String? = null): String {
        val strBuilder = StringBuilder(firstText)
        for (text in input) {
            text?.let {
                if(text.isNotBlank()) {
                    if (separator.isNullOrBlank())
                        strBuilder.append(" $it")
                    else
                        strBuilder.append(" $separator $it")
                }
            }
        }
        return strBuilder.trim().toString()
    }

    fun getDuplicatePatientMap(errorBody: ResponseBody?): HashMap<String, Any>? {
        return try {
            var returnMap: HashMap<String, Any>? = null
            errorBody?.let { err ->
                val errorResponse = Gson().fromJson(err.string(), Map::class.java)
                if (errorResponse.containsKey(Screening.Entity)) {
                    errorResponse[Screening.Entity]?.let { entity ->
                        if (entity is Map<*, *> && entity.contains(Screening.PatientDetails)) {
                            entity[Screening.PatientDetails]?.let { details ->
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
    }
}