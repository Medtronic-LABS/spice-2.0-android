package org.medtroniclabs.uhis.ui.household

interface MemberSelectionListener {
    fun onMemberSelected(
        item: Long,
        isEdit: Boolean,
        dateOfBirth: String?,
        isContactTrace: Boolean = false,
        houseHoldId: Long? = null,
        fhirId: String? = null,
    )
}
