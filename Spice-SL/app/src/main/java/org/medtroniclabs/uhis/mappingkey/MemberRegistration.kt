package org.medtroniclabs.uhis.mappingkey

import android.content.Context
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.common.DateUtils
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter

object MemberRegistration {
    const val NAME = "name"
    const val PHONE_NUMBER = "phone_number"
    const val PHONE_NUMBER_CATEGORY = "phone_number_category"
    const val VILLAGE_ID = "village_id"
    const val DATE_OF_BIRTH = "date_of_birth"
    const val GENDER = "gender"

    /**
     * Spinner : ID Type
     */
    const val ID_TYPE = "id_type"
    const val NATIONAL_ID = "national_id"
    const val IS_HOUSEHOLD_HEAD = "is_house_hold_head"
    const val HOUSEHOLD_FHIR_ID = "household_fhir_id"

    private const val WIFE_OR_HUSBAND = "Wife / Husband"
    private const val SON_OR_DAUGHTER = "Son / Daughter"
    private const val FATHER_OR_MOTHER = "Father / Mother"
    private const val BROTHER_OR_SISTER = "Brother / Sister"
    private const val GRAND_CHILD = "Grandchild"
    private const val GRAND_PARENT = "Grandparent"
    const val OTHER_RELATION = "Other Family Member (specify)"

    /**
     * SingleSelectionView : Marital Status
     */
    const val ID_MARITAL_STATUS = "maritalStatus"

    /**
     * SingleSelectionView : Disability
     */
    const val ID_DISABILITY = "disability"

    /**
     * Spinner : Guardian Name
     */
    const val ID_GUARDIAN = "guardian"

    /**
     * Spinner option id for add guardian field
     */
    const val ADD_GUARDIAN_ID = -2L

    /**
     * Maximum age till which guardian is mandatory
     */
    const val MAX_AGE_GUARDIAN = 2L

    /**
     * Minimum age for marital status capture
     */
    const val MIN_AGE_MARITAL_STATUS = 14L

    /**
     * Minimum age for household head
     */
    const val MIN_AGE_HH_HEAD = 14

    /**
     * Minimum age for pregnancy
     */
    const val MIN_AGE_PREGNANCY = 14

    /**
     * Maximum age for pregnancy
     */
    const val MAX_AGE_PREGNANCY = 50

    const val MAX_LENGTH_NATIONAL_ID = 17

    const val QR_CODE = "qrCode"

    val NATIONAL_ID_LENGTH = listOf(10, 13, 17)

    enum class MaritalStatus(val value: String) {
        MARRIED("married"),
    }

    enum class IdType(val value: String) {
        NATIONAL_ID("nid"),
    }

    enum class PhoneNumberCategory(val value: String) {
        PERSONAL("personal"),
        HOUSEHOLD_HEAD("household_head"),
        FAMILY("family"),
        FRIEND("friend"),
    }

    fun isValidRelationAge(
        ctx: Context,
        dob: String,
        relation: String,
        headDob: String?,
    ): String? {
        // No Age Validation for Brother or Sister and Other relation
        if (relation.equals(BROTHER_OR_SISTER, true) ||
            relation.equals(OTHER_RELATION, true)
        ) {
            return null
        }

        val memberDOB =
            LocalDate.parse(dob, DateTimeFormatter.ofPattern(DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ))
        // Min Age validation for Wife or Husband
        if (relation.equals(WIFE_OR_HUSBAND, true)) {
            return if (isValidMinAge(memberDOB)) null else ctx.getString(R.string.age_validation_wife_husband)
        }

        val headDOB = LocalDate.parse(headDob, DateTimeFormatter.ofPattern(DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ))
        val ageDifference = Period.between(headDOB, memberDOB).years

        return when (relation) {
            // Minimum +10 Age Difference for Son or Daughter
            SON_OR_DAUGHTER -> if (ageDifference >= 10) null else ctx.getString(R.string.age_validation_son_daughter)

            // Minimum -10 Age Difference for Father or Mother
            FATHER_OR_MOTHER -> if (ageDifference <= -10) null else ctx.getString(R.string.age_validation_father_mother)

            // Minimum +30 Age Difference for Grandchild
            GRAND_CHILD -> if (ageDifference >= 30) null else ctx.getString(R.string.age_validation_grandchild)

            // Minimum -30 Age Difference for Grandparent
            GRAND_PARENT -> if (ageDifference <= -30) null else ctx.getString(R.string.age_validation_grandparent)
            else -> null
        }
    }

    fun isValidMinAge(dateOfBirth: LocalDate): Boolean {
        val today = LocalDate.now()
        val age = Period.between(dateOfBirth, today).years
        return age >= MIN_AGE_HH_HEAD
    }

    fun isValidMinAgeForCbsMemberAdd(dob: String): Boolean {
        val memberDOB = LocalDate.parse(
            dob,
            DateTimeFormatter.ofPattern(DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ),
        )
        val ageDifference = Period.between(memberDOB, LocalDate.now())
        return ageDifference.years == 0
    }
}
