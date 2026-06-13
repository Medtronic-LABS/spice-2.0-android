package org.medtroniclabs.uhis.model.services

/**
 * Static filters list
 */
enum class ServiceStaticFilter(val value: String, val culturalValue: String) {
    /**
     * All members with dynamic filters if applied
     */
    ALL_MEMBERS("All member list", "সব সদস্য তালিকা"),

    /**
     * Women of reproductive age, who are currently married
     */
    FAMILY_PLANNING_COUNSELLING_ELIGIBLE("Family planning counseling list", "পরিবার পরিকল্পনা কাউন্সেলিং তালিকা"),

    /**
     * Women currently expecting
     */
    PREGNANT_WOMEN("Pregnant Women List", "গর্ভবতী মায়ের তালিকা"),

    /**
     * High risk cases during pregnancy
     */
    HIGH_RISK_PREGNANT_WOMEN("High risk PW women list", "উচ্চ ঝুঁকিপূর্ণ গর্ভবতী মায়ের তালিকা"),

    /**
     * Mothers in the postnatal period
     */
    POSTNATAL_CARE_MOTHERS("Postnatal Mothers List", "প্রসবোত্তর মায়ের তালিকা"),

    /**
     * Toddlers and infants
     */
    CHILDREN_UNDER_TWO_YEARS("Child List (under-2)", "শিশু তালিকা (২ বছরের নিচের)"),

    /**
     * Members registered from outside the catchment area
     */
    EXTERNAL_MEMBERS("External member list", "বহিরাগত সদস্য তালিকা"),

    /**
     * External expectant mothers
     */
    EXTERNAL_PREGNANT_WOMEN("External PW list", "বহিরাগত গর্ভবতী মায়ের তালিকা"),

    /**
     * Upcoming delivery dates
     */
    EXPECTED_DELIVERIES("Expected delivery list", "সম্ভাব্য ডেলিভারি তালিকা"),

    /**
     * Deliveries that are past due or pending update
     */
    PENDING_DELIVERIES("Pending Delivery List", "পেন্ডিং ডেলিভারি লিস্ট"),

    /**
     * Members with NCD-related service history (FO/PO service list)
     */

    NCD_SERVICES("NCD Services", "এনসিডি সেবাসমূহ"),

    /**
     * Members with cataract screening service history
     */

    CATARACT_SCREENING("Cataract Screening", "ক্যাটারাক্ট স্ক্রিনিং"),

    /**
     * Members with eye screening (eye care) service history
     */
    EYE_SCREENING("Eye Screening", "চক্ষু পরীক্ষা স্ক্রিনিং"),
}
