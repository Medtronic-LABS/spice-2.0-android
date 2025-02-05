package com.medtroniclabs.opensource.formgeneration

import android.text.Spannable
import android.text.SpannableString
import android.text.style.ClickableSpan
import com.medtroniclabs.opensource.formgeneration.config.DefinedParams
import java.lang.reflect.Field

object FormSupport {

    fun updateTitle(
        title: String,
        translate: Boolean,
        titleCulture: String?,
        unitMeasurement: String?
    ): CharSequence {
        var titleText: String
        titleText = if (title.isNotEmpty() && !translate) {
            title
        } else if (!titleCulture.isNullOrBlank() && translate) {
            titleCulture
        } else {
            title
        }
        unitMeasurement?.let { measurementType ->
            "$titleText (${measurementType})".also { text -> titleText = text }
        }
        return titleText
    }

    fun getSpannableString(
        clickableSpan: ClickableSpan,
        text: String,
        startIndex: Int = 0
    ): CharSequence {
        val spannableString =
            SpannableString(text)

        spannableString.setSpan(
            clickableSpan,
            startIndex,
            spannableString.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        return spannableString
    }

    fun translateTitle(titleCulture: String?, title: String, translate: Boolean): String {
        return if (translate) (titleCulture ?: title) else title
    }

    fun getResId(resName: String, c: Class<*>): Int {
        return try {
            val idField: Field = c.getDeclaredField(resName)
            idField.getInt(idField)
        } catch (e: Exception) {
            -1
        }
    }

    fun isTranslatedOrNot(map: Map<String, Any>, name: String,translate: Boolean): CharSequence? {
        return if (translate) {
            val translatedName = map[DefinedParams.cultureValue]
            translateName(translatedName, name)
        } else {
            name
        }
    }

    private fun translateName(translatedName: Any?, name: String): CharSequence? {
        return if (translatedName is String?) {
            translatedName ?: name
        } else {
            name
        }
    }




}