package org.medtroniclabs.uhis.common

import org.medtroniclabs.uhis.common.RoleConstant.COMMUNITY_HEALTH_CARE_PROVIDER
import org.medtroniclabs.uhis.common.RoleConstant.HEALTH_EDUCATOR

object RoleConstant {
    const val SHASTIYA_KORMI = "SHASTIYA_KORMI"
    const val PROVIDER = "PROVIDER"
    const val COMMUNITY_HEALTH_WORKER = "CHW"
    const val PO = "PO"
    const val FO = "FO"
    val CHWs = arrayOf(SHASTIYA_KORMI, COMMUNITY_HEALTH_WORKER, PO, FO)
    const val PEER_SUPERVISOR = "PEER_SUPERVISOR"
    const val SECHN = "SECHN"
    const val MCHA = "MCHA"
    const val CHA = "CHA"
    const val MID_WIFE = "MID_WIFE"
    const val LAB_ASSISTANT = "LAB_ASSISTANT"
    const val SRN = "SRN"
    const val NUTRITIONIST = "NUTRITIONIST"
    const val PHYSICIAN_PRESCRIBER = "PHYSICIAN_PRESCRIBER"
    const val COUNSELOR = "COUNSELOR"
    const val PHARMACIST = "PHARMACIST"
    const val NURSE = "NURSE"
    const val HRIO = "HRIO"
    const val TIBERBU_PROVIDER = "TIBERBU_PROVIDER"
    const val LAB_TECHNICIAN = "LAB_TECHNICIAN"
    const val HEALTH_SCREENER = "HEALTH_SCREENER"
    const val COMMUNITY_HEALTH_PROMOTER = "COMMUNITY_HEALTH_PROMOTER"
    const val COMMUNITY_HEALTH_ASSISTANT = "COMMUNITY_HEALTH_ASSISTANT"

    const val COMMUNITY_HEALTH_CARE_PROVIDER = "COMMUNITY_HEALTH_CARE_PROVIDER"

    /**
     * [COMMUNITY_HEALTH_CARE_PROVIDER]
     */
    const val CHCP = "CHCP"
    const val PARA_COUNSELLOR = "PARA_COUNSELLOR"
    const val PSYCHOLOGIST = "PSYCHOLOGIST"
    const val PROGRAM_ORGANIZER = "PROGRAM_ORGANIZER"
    const val FIELD_ORGANIZER = "FIELD_ORGANIZER"
    const val HEALTH_EDUCATOR = "HEALTH_EDUCATOR"

    /**
     * [HEALTH_EDUCATOR]
     */
    const val HE = "HE"
    const val PARAMEDIC = "NON_TECH_TELECOUNSELOR"
    const val MEDICAL_DOCTOR = "TECH_TELECOUNSELOR"
}
