package org.medtroniclabs.uhis.common

import kotlin.collections.get

object RoleConstant {
    const val SHASTIYA_KORMI = "SHASTIYA_KORMI"
    val PROVIDER = "PROVIDER"
    val COMMUNITY_HEALTH_WORKER = "CHW"
    val PO = "PO"
    val FO = "FO"
    val CHWs = arrayOf(SHASTIYA_KORMI, COMMUNITY_HEALTH_WORKER, PO, FO)
    val PEER_SUPERVISOR = "PEER_SUPERVISOR"
    val SECHN = "SECHN"
    val MCHA = "MCHA"
    val CHA = "CHA"
    val MID_WIFE = "MID_WIFE"
    val LAB_ASSISTANT = "LAB_ASSISTANT"
    val SRN = "SRN"
    val COMMUNITY_HEALTH_PROVIDER = "CHP"
    val NUTRITIONIST = "NUTRITIONIST"
    val PHYSICIAN_PRESCRIBER = "PHYSICIAN_PRESCRIBER"
    val COUNSELOR = "COUNSELOR"
    val PHARMACIST = "PHARMACIST"
    val NURSE = "NURSE"
    val HRIO = "HRIO"
    val TIBERBU_PROVIDER = "TIBERBU_PROVIDER"
    val LAB_TECHNICIAN = "LAB_TECHNICIAN"
    val HEALTH_SCREENER = "HEALTH_SCREENER"
    val COMMUNITY_HEALTH_PROMOTER = "COMMUNITY_HEALTH_PROMOTER"
    val COMMUNITY_HEALTH_ASSISTANT = "COMMUNITY_HEALTH_ASSISTANT"

    val HEALTH_COACH = "HEALTH_COACH"

    val CARE_COORDINATOR = "CARE_COORDINATOR"
    val COMMUNITY_HEALTH_CARE_PROVIDER = "COMMUNITY_HEALTH_CARE_PROVIDER"
    val PARA_COUNSELLOR = "PARA_COUNSELLOR"
    val PSYCHOLOGIST = "PSYCHOLOGIST"
    val PROGRAM_ORGANIZER = "PROGRAM_ORGANIZER"
    val FIELD_ORGANIZER = "FIELD_ORGANIZER"
    val SHASTHYA_SHEBIKA = "SHASTHYA_SHEBIKA"
    val HEALTH_EDUCATOR = "HEALTH_EDUCATOR"
    val PARAMEDIC = "NON_TECH_TELECOUNSELOR"
    val MEDICAL_DOCTOR = "TECH_TELECOUNSELOR"

    private val DisplayNames = mapOf(
        HEALTH_COACH to "Health Coach",
        HEALTH_SCREENER to "SK",
        HRIO to "HRIO",
        LAB_TECHNICIAN to "Lab Technician",
        NUTRITIONIST to "Nutritionist",
        PHARMACIST to "Pharmacist",
        PROVIDER to "Provider",
        NURSE to "Nurse",
        PHYSICIAN_PRESCRIBER to "Physician Prescriber",
        COMMUNITY_HEALTH_CARE_PROVIDER to "CHCP",
        PARA_COUNSELLOR to "Paracounsellor",
        PROGRAM_ORGANIZER to "PO",
        MEDICAL_DOCTOR to "TC Tech",
        HEALTH_EDUCATOR to "Health Educator",
        PSYCHOLOGIST to "Psychologist",
        PARAMEDIC to "TC Non Tech",
        FIELD_ORGANIZER to "FO",
    )

    fun getRoleInDisplayFormatWithBraces(role: String?): String {
        val value = DisplayNames[role] ?: return ""
        return "($value)"
    }
}
