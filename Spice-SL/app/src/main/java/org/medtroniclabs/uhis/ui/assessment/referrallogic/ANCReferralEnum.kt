package org.medtroniclabs.uhis.ui.assessment.referrallogic

import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.BN_CONDITION_ANY_OTHER
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.CONDITION_ANY_OTHER

/**
 * Enum class representing the urgent referral conditions for antenatal care.
 */
enum class ANCUrgentReferrals(val value: String, val cultureValue: String) {
    SUSPECTED_PRE_ECLAMPSIA("Suspected Pre-eclampsia", "সম্ভাব্য প্রি-এক্লাম্পসিয়া"),
    HIGH_FEVER("High Fever", "তীব্র জ্বর"),
    ABNORMAL_FUNDAL_HEIGHT("Abnormal fundal height", "অস্বাভাবিক জরায়ুর উচ্চতা"),
    ABNORMAL_WEIGHT_GAIN("Abnormal weight gain", "অস্বাভাবিক ওজন বৃদ্ধি"),
    ABNORMAL_PULSE("Abnormal Pulse", "অস্বাভাবিক নাড়ির গতি"),
    SEVERE_ANEMIA("Severe Anemia", "মারাত্মক রক্তশূন্যতা"),
    URINARY_BILIRUBIN("Urinary Bilirubin present", "প্রস্রাবে বিলিরুবিন পাওয়া গেছে"),
    CHRONIC_ILLNESS_NOT_ON_TREATMENT("PW not on treatment for existing chronic illnesses", "দীর্ঘমেয়াদী রোগে চিকিৎসাবিহীন গর্ভবতী মা"),
}

/**
 * Enum class representing the nonurgent referral conditions for antenatal care.
 */
enum class ANCNonUrgentReferrals(val value: String, val cultureValue: String) {
    HIGH_RISK_PREGNANCY("High risk PW due to age/birth spacing", "বয়স/স্বল্প জন্মবিরতি কারণে উচ্চ ঝুঁকিপূর্ণ গর্ভবতী মা"),
    MODERATE_ANEMIA("Moderate Anemia", "মাঝারি রক্তসল্পতা"),
    MILD_ANEMIA("Mild Anemia", "মৃদু রক্তসল্পতা"),
    SUSPECTED_DIABETES("Suspected/Existing Case of Diabetes", "সম্ভাব্য/চিহ্নিত ডায়াবেটিস রোগী"),
    LOW_BP("LOW BP", "নিম্ন রক্তচাপ"),
    CHRONIC_ILLNESS_WITH_TREATMENT(
        "PW with existing chronic illnesses with treatment",
        "দীর্ঘমেয়াদী রোগে চিকিৎসাধীন গর্ভবতী মা",
    ),
    MILD_FEVER("Mild Fever", "হালকা জ্বর"),
    PREGNANCY_RELATED_MEDICAL_COMPLICATIONS("H/O Preg related medical complications", "গর্ভকালীন জটিলতার ইতিহাস"),
    OTHER(CONDITION_ANY_OTHER, BN_CONDITION_ANY_OTHER),
}

/**
 * Enum class representing the care gaps identified during antenatal care assessment.
 */
enum class ANCGaps(val value: String, val cultureValue: String) {
    TT_VACCINATION_INCOMPLETE("TT vaccination incomplete", "টিটি টিকার ডোজ সম্পন্ন হয়নি"),
    USG_NOT_DONE("USG not done >36 weeks", "গর্ভাবস্থায় ৩৬ সপ্তাহের মধ্যেও আল্ট্রাসাউন্ড করা হয়নি"),
    ANC_WITH_DOCTOR_NOT_DONE("ANC with Doctor not done >36 weeks", "গর্ভাবস্থায় ৩৬ সপ্তাহের মধ্যেও ডাক্তারের পরামর্শ নেওয়া হয়নি"),
    LESS_THAN_3_ANCS("Less than 3 ANCs completed at end of 36 weeks", "গর্ভাবস্থার ৩৬ সপ্তাহ শেষে ৩টি বা তার কম এএনসি সম্পন্ন হয়েছে"),
    INADEQUATE_IFA("Inadequate /Non consumption IFA", "আয়রনের বড়ি কম খাওয়া বা একদমই না খাওয়া"),
    INADEQUATE_CALCIUM("Inadequate /Non consumption Calcium", "ক্যালসিয়ামের বড়ি কম খাওয়া বা একদমই না খাওয়া"),
    FACILITY_NOT_IDENTIFIED("Facility not identified for institutional delivery", "প্রাতিষ্ঠানিক প্রসবের জন্য স্বাস্থ্যকেন্দ্র চিহ্নিত করা হয়নি"),
    PLANNED_HOME_DELIVERY("Planned for Home Delivery", "ঘরে প্রসবের পরিকল্পনা করা হয়েছে"),
}
