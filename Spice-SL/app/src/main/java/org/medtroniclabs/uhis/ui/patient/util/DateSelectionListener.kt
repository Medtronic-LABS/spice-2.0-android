package org.medtroniclabs.uhis.ui.patient.util

import org.medtroniclabs.uhis.data.registration.SessionModel

interface DateSelectionListener {
    fun onDateSelected(_id: Long)

    fun onSessionSelected(model: SessionModel)
}
