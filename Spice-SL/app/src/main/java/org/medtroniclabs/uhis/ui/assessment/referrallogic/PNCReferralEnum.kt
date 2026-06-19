package org.medtroniclabs.uhis.ui.assessment.referrallogic

import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.BN_CONDITION_ANY_OTHER
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.CONDITION_ANY_OTHER

/**
 * Enum class representing the danger signs that can occur after childbirth.
 * These signs are used for assessment during postnatal care (PNC).
 */
enum class PostpartumDangerSigns(val value: String) {
    HEAVY_BLEEDING("heavyBleeding"),
    FOUL_SMELLING_DISCHARGE("foulSmellingDischarge"),
    SEVERE_ABDOMINAL_PAIN("severeAbdominalPain"),
    SEVERE_HEADACHE_VISION_CONVULSIONS("severeHeadacheVisionConvulsions"),
    PERINEAL_WOUND_DISCHARGE("perinealWoundDischarge"),
    BREAST_PAIN_SWELLING_FEVER("breastPainSwellingFever"),
    OTHER("other"),
}

/**
 * Enum class representing the urgent referral conditions for postnatal care.
 */
enum class PNCUrgentReferrals(val value: String, val cultureValue: String) {
    HEAVY_BLEEDING("Heavy bleeding", "অতিরিক্ত রক্তপাত"),
    SEVERE_ABDOMINAL_PAIN("Severe abdominal pain", "তীব্র পেটে ব্যথা"),
    SEVERE_HEADACHE_VISUAL_ISSUES_CONVULSIONS("Severe headache/visual issues/convulsions", "তীব্র মাথাব্যথা/দৃষ্টি সমস্যা/খিঁচুনি"),
    FOUL_SMELLING_DISCHARGE("Foul-smelling discharge", "দুর্গন্ধযুক্ত স্রাব"),
    PERINEUM_TEAR_DISCHARGE("Perineum tear / Discharge from wound area", "পেরিনিয়াম ছিঁড়ে যাওয়া / ক্ষতস্থান থেকে  পুঁজ বা পানি পড়া"),
    HIGH_BP("High BP", "উচ্চ রক্তচাপ"),
    HTN_ECLAMPSIA_NOT_ON_TREATMENT(
        "Not on treatment for HTN or Pre-eclampsia /Eclampsia",
        "উচ্চ রক্তচাপ বা প্রি-এক্লাম্পসিয়া/এক্লাম্পসিয়ার চিকিৎসা নিচ্ছেন না",
    ),
    EDEMA_PLUS_RISK("Edema", "ইডিমা"),
    URINE_ALBUMIN_PLUS_RISK("Urine Albumin", "প্রস্রাবে অ্যালবুমিন"),
    HIGH_FEVER("High Fever", "তীব্র জ্বর"),
    ABNORMAL_PULSE("Abnormal Pulse", "অস্বাভাবিক নাড়ির গতি"),
    SEVERE_ANEMIA("Severe Anemia", "মারাত্মক রক্তসল্পতা"),
    HIGH_BLOOD_SUGAR("High Blood sugar", "রক্তে চিনির মাত্রা বেশি"),
    DM_GDM_NOT_ON_TREATMENT("Known DM/GDM patient not on treatment", "ডায়াবেটিস / গর্ভকালীন ডায়াবেটিস আছে কিন্তু চিকিৎসাধীন নন"),
    SUSPECTED_JAUNDICE("Suspected Jaundice", "সম্ভাব্য জন্ডিস"),
}

/**
 * Enum class representing the non-urgent referral conditions for postnatal care.
 */
enum class PNCNonUrgentReferrals(val value: String, val cultureValue: String) {
    MODERATE_ANEMIA("Moderate Anemia", "মাঝারি রক্তসল্পতা"),
    MILD_ANEMIA("Mild Anemia", "মৃদু রক্তসল্পতা"),
    BREAST_ISSUES("Cracked nipples / painful / swollen breasts with or without fever", "নিপল ফেটে যাওয়া / স্তনে ব্যথা বা ফোলা এবং সাথে জ্বর থাকা বা না থাকা"),
    FEVER("Fever", "জ্বর"),
    HTN_ECLAMPSIA_ON_TREATMENT("On treatment for HTN or Pre-eclampsia / Eclampsia", "উচ্চ রক্তচাপ বা প্রি-এক্লাম্পসিয়া/এক্লাম্পসিয়ার চিকিৎসা নিচ্ছেন"),
    DM_GDM_ON_TREATMENT("On treatment for DM/GDM", "ডায়াবেটিস বা গর্ভকালীন ডায়াবেটিসের চিকিৎসাধীন আছেন"),
    OTHER(CONDITION_ANY_OTHER, BN_CONDITION_ANY_OTHER),
}

/**
 * Enum class representing the care gaps identified during postnatal care assessment.
 */
enum class PNCGaps(val value: String, val cultureValue: String) {
    SUPPLEMENTATION("Supplementation", "পরিপূরক"),
    CONTRACEPTION_GAP("Not using postpartum contraception", "প্রসব-পরবর্তী পরিবার পরিকল্পনা নিচ্ছেন না"),
}

/**
 * Enum class representing the specific supplements for postnatal care.
 */
enum class PNCSupplementation(val value: String, val cultureValue: String) {
    VITAMIN_A("Vitamin A", "ভিটামিন এ"),
    IFA("IFA", "আয়রন"),
    CALCIUM("Calcium", "ক্যালসিয়াম"),
}

/**
 * Anemia level
 */
enum class AnemiaLevel {
    Moderate, // if Hb<10
    Severe, // if Hb <8
    Mild, // if Hb <11
    None,
}
