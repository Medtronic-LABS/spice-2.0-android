package org.medtroniclabs.uhis.ui.patient.util

import android.app.DatePickerDialog
import android.content.Context
import android.widget.DatePicker
import java.util.Calendar

object ViewUtil {
    fun showDatePicker(
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

        if (disableFutureDate) dialog.datePicker.maxDate = System.currentTimeMillis()

        dialog.setCancelable(false)

        dialog.show()

        return dialog
    }
}
