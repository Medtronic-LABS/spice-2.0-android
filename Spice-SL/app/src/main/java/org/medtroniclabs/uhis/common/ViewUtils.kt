package org.medtroniclabs.uhis.common

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.DatePicker
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.common.CommonUtils.getMaxDateLimit
import java.util.Calendar

object ViewUtils {
    fun showDatePicker(
        context: Context,
        disableFutureDate: Boolean = false,
        minDate: Long? = null,
        maxDate: Long? = null,
        isMenstrualPeriod: Boolean = false,
        date: Triple<Int?, Int?, Int?>? = null,
        cancelCallBack: (() -> Unit)? = null,
        callBack: (dialog: DatePicker, year: Int, month: Int, dayOfMonth: Int) -> Unit,
    ): DatePickerDialog {
        val calendar = Calendar.getInstance()
        var thisYear = calendar.get(Calendar.YEAR)
        var thisMonth = calendar.get(Calendar.MONTH)
        var thisDay = calendar.get(Calendar.DAY_OF_MONTH)
        date?.let {
            if (it.first != null && it.second != null && it.third != null) {
                thisYear = it.first!!
                thisMonth = it.second!!
                thisDay = it.third!!
            }
        }
        val dateSetListener = DatePickerDialog.OnDateSetListener { datePicker, year, month, dayOfMonth ->
            callBack.invoke(datePicker, year, month + 1, dayOfMonth)
        }
        val dialog = DatePickerDialog(context, dateSetListener, thisYear, thisMonth, thisDay)

        try {
            getMaxDateLimit(isMenstrualPeriod, minDate)?.let { minDateValue ->
                dialog.datePicker.minDate = minDateValue
            }
            maxDate?.let { maxDateValue ->
                dialog.datePicker.maxDate = maxDateValue
            }

            if (disableFutureDate) {
                dialog.datePicker.maxDate = System.currentTimeMillis()
            }
            if (cancelCallBack != null) {
                dialog.setOnCancelListener {
                    cancelCallBack.invoke()
                }
            }
            dialog.setButton(
                DialogInterface.BUTTON_POSITIVE,
                context.getString(R.string.okay),
            ) { dg, _ ->
                dialog.onClick(dg, DialogInterface.BUTTON_POSITIVE)
            }

            dialog.setButton(
                DialogInterface.BUTTON_NEGATIVE,
                context.getString(R.string.cancel),
            ) { dg, _ ->
                dialog.onClick(dg, DialogInterface.BUTTON_NEGATIVE)
            }
            Handler(Looper.getMainLooper()).post {
                dialog.setCancelable(false)
                dialog.show()
            }
        } catch (exception: Exception) {
            exception.printStackTrace()
        }

        return dialog
    }

    fun statusCheck(context: Context) {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
        manager?.let { locationManager ->
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                buildAlertMessageNoGps(context)
            }
        }
    }

    private fun buildAlertMessageNoGps(context: Context) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        builder
            .setMessage(context.getString(R.string.gps_enabled))
            .setCancelable(false)
            .setPositiveButton(
                context.getText(R.string.yes),
            ) { dialog, _ ->
                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                dialog.cancel()
            }.setNegativeButton(
                context.getText(R.string.no),
            ) { dialog, _ ->
                dialog.cancel()
            }
        val alert = builder.create()
        alert.show()
    }
}
