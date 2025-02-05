package com.medtroniclabs.opensource.common

import android.content.Context
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.common.DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ
import com.medtroniclabs.opensource.common.DateUtils.DATE_ddMMyyyy
import com.medtroniclabs.opensource.common.DateUtils.convertDateFormat
import com.medtroniclabs.opensource.formgeneration.FormGenerator
import com.medtroniclabs.opensource.formgeneration.MentalHealthAdapter
import com.medtroniclabs.opensource.formgeneration.SingleSelectionMHView
import com.medtroniclabs.opensource.formgeneration.ui.SingleSelectionCustomView
import com.medtroniclabs.opensource.formgeneration.utility.CustomSpinnerAdapter
import com.medtroniclabs.opensource.mappingkey.Screening

object FormAutofill {
    fun start(context: Context, formGenerator: FormGenerator, values: Any) {
        val resultMap = objectToMap(values)
        resultMap.forEach { map ->
            (map.key as? String?)?.let { key ->
                formGenerator.getViewByTag(key)?.let { view ->
                    when (view) {
                        is EditText -> {
                            setEditText(view, map.value)
                        }

                        is Spinner -> {
                            setSpinner(view, map.value)
                        }

                        is SingleSelectionCustomView -> {
                            setSingleSelectionCustomView(view, key, map.value)
                        }

                        is TextView -> {
                            setTextView(view, key, map.value, formGenerator)
                        }

                        is RecyclerView -> {
                            if (view.adapter is MentalHealthAdapter)
                                (map.value as? ArrayList<Map<String, Any>>)?.let { list ->
                                    prefillRecyclerView(context, view, list, key)
                                }
                        }
                    }
                }
            }
        }
    }

    private fun prefillRecyclerView(
        context: Context,
        view: RecyclerView,
        list: ArrayList<Map<String, Any>>,
        key: String
    ) {
        for (i in 0 until list.size) {
            //Getting one by one listItem
            (view.findViewHolderForAdapterPosition(i) as? MentalHealthAdapter.MentalHealthViewHolder)?.let { viewHolder ->
                val questionView = viewHolder.binding.tvQuestion
                val inflatedView = viewHolder.binding.llAnswerRoot

                val map = list.firstOrNull {
                    (it[Screening.MHQuestion] as? String)?.equals(
                        questionView.tag?.toString(),
                        true
                    ) == true
                }

                if (!map.isNullOrEmpty())
                    for (j in 0 until inflatedView.childCount) {
                        (inflatedView.getChildAt(j) as? SingleSelectionMHView)?.let { singleSelectionView ->
                            (getTranslatedString(context, map[Screening.MHAnswer] as? String))?.let {
                                singleSelectionView.prefillSingleSelection(it)
                            }
                        }
                    }
            }
        }
    }

    private fun getTranslatedString(context: Context, value: String?): String? {
        return if (SecuredPreference.getIsTranslationEnabled()) {
            when (value) {
                Screening.NotAtAll -> context.getString(R.string.not_at_all)
                Screening.SeveralDays -> context.getString(R.string.several_days)
                Screening.MoreThanHalfTheDays -> context.getString(R.string.more_than_half_day)
                Screening.NearlyEveryDay -> context.getString(R.string.nearly_every_day)
                else -> value
            }
        } else
            value
    }

    private fun setTextView(
        view: TextView, key: String, value: Any?, formGenerator: FormGenerator
    ) {
        if (value is String) {
            if (key.equals(Screening.DateOfBirth, true)) {
                val dobString =
                    convertDateFormat(value, DATE_FORMAT_yyyyMMddHHmmssZZZZZ, DATE_ddMMyyyy)
                val dobDate = DateUtils.convertStringToDate(value, DATE_FORMAT_yyyyMMddHHmmssZZZZZ)

                if (dobString.isNotBlank()) view.text = dobString

                if (dobDate != null) formGenerator.fillDetailsOnDatePickerSet(
                    dobDate,
                    false,
                    id = key
                )
            } else view.text = value
        }
    }

    private fun setSingleSelectionCustomView(
        view: SingleSelectionCustomView, key: String, value: Any?
    ) {
        val id = "${value}_$key"
        view.singleSelectionAutofill(id)
    }

    private fun setSpinner(view: Spinner, value: Any?) {
        val adapter = view.adapter
        if (adapter is CustomSpinnerAdapter) {
            when (value) {
                is String -> {
                    val index = adapter.getIndexOfItemById(value)
                    if (index > 0)
                        view.setSelection(index)
                }
            }
        }
    }

    private fun setEditText(view: EditText, value: Any?) {
        if (value is String) view.setText(value)
    }


    private fun objectToMap(obj: Any): Map<*, *> {
        val gson = Gson()
        return gson.fromJson(gson.toJson(obj), Map::class.java) as Map<*, *>
    }
}