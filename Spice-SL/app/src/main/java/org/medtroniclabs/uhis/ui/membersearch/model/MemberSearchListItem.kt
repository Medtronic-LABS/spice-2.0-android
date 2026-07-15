package org.medtroniclabs.uhis.ui.membersearch.model

import org.medtroniclabs.uhis.data.model.PatientListResModel
import org.medtroniclabs.uhis.data.offlinesync.model.HouseholdMemberWithTb

/**
 * Row model for the hybrid member search list.
 *
 * Local items are clickable and backed by Room; remote items fetch member details on tap.
 */
sealed class MemberSearchListItem {
    /** Member from the local database. */
    data class Local(
        val member: HouseholdMemberWithTb,
    ) : MemberSearchListItem()

    /** Patient from the remote search API; navigable after fetching member details. */
    data class Remote(
        val patient: PatientListResModel,
    ) : MemberSearchListItem()
}
