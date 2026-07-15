package org.medtroniclabs.uhis.mappingkey

object HouseHoldRegistration {
    const val HOUSEHOLD_NAME = "householdName"
    const val NO_OF_PEOPLE = "no_of_people"
    const val VILLAGE_ID = "village_id"
    const val CHIEFDOM_ID = "chiefdom_id"
    const val SHASTHYA_KORMI_ID = "shasthya_kormi_id"
    const val SHASTHYA_SHEBIKA_ID = "shasthya_shebika_id"
    const val SUB_VILLAGE_ID = "sub_village_id"
    const val HOUSEHOLD_TYPE = "household_type"
    const val MONTHLY_INCOME_RANGE = "monthlyIncomeRange"
    const val HOUSEHOLD_NUMBER = "household_number"
    const val TOTAL_MEMBERS = "total_members"
    const val YES = "yes"
    const val NO = "no"

    /**
     * EditText : Disability persons count
     */
    const val ID_DISABILITY_PERSONS_COUNT = "disability_persons_count"

    const val HOUSEHOLD_HEAD_OCCUPATION = "householdHeadOccupation"
    const val OTHER_OCCUPATION = "otherOccupation"

    fun rangeFromExactValue(exactValue: Double): String =
        when {
            exactValue <= 5000 -> "<5000"
            exactValue > 5000 && exactValue <= 10000 -> "5001–10000"
            exactValue > 10000 && exactValue <= 15000 -> "10001–15000"
            exactValue > 15000 && exactValue <= 20000 -> "15001–20000"
            exactValue > 20000 && exactValue <= 30000 -> "20001–30000"
            exactValue > 30000 && exactValue <= 40000 -> "30001–40000"
            exactValue > 40000 && exactValue <= 70000 -> "40001–70000"
            exactValue > 70000 -> ">70000"
            else -> {
                ""
            }
        }
}
