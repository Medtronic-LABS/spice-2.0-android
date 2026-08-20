package org.medtroniclabs.uhis.common

import com.medtroniclabs.microcoaching.CoachingPersona

/**
 * Resolves the coaching SDK persona from the logged-in user's role.
 * First role wins (mirrors [SecuredPreference.getRole]); the SDK stays agnostic
 * of SPICE role vocabulary and only receives the [CoachingPersona].
 */
fun resolveCoachingPersona(): CoachingPersona =
    coachingPersonaForRoleName(
        SecuredPreference
            .getUserDetails()
            ?.roles
            ?.firstOrNull()
            ?.name,
    )

/** Pure role-name → persona mapping. Unknown/blank roles fall back to SK in the SDK. */
fun coachingPersonaForRoleName(roleName: String?): CoachingPersona =
    when {
        roleName == null -> CoachingPersona.UNKNOWN
        roleName == RoleConstant.PO -> CoachingPersona.PO
        roleName in RoleConstant.CHWs -> CoachingPersona.SK
        else -> CoachingPersona.UNKNOWN
    }
