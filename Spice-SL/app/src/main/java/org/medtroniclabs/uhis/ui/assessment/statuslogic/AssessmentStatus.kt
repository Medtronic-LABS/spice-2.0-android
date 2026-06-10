package org.medtroniclabs.uhis.ui.assessment.statuslogic

/**
 * Different status for different assessment.
 */
enum class AssessmentStatus {
    /**
     * High risk PW : Pregnancy Registration, ANC
     */
    HIGH_RISK_PW,

    /**
     * Normal Pregnancy : Pregnancy Registration, ANC
     */
    NORMAL_PREGNANCY,

    /**
     * Gaps IN ANC : ANC
     */
    GAPS_IN_ANC,

    /**
     * C-Section : Pregnancy Outcome
     */
    C_SECTION,

    /**
     * Assisted Delivery : Pregnancy Outcome
     */
    ASSISTED_DELIVERY,

    /**
     * Normal Delivery : Pregnancy Outcome
     */
    NORMAL_DELIVERY,

    /**
     * Neonatal Death : Pregnancy Outcome
     */
    NEONATAL_DEATH,

    /**
     * Abortion : Pregnancy Outcome
     */
    ABORTION,

    /**
     * Still Birth : Pregnancy Outcome
     */
    STILL_BIRTH,

    /**
     * Live Birth : Pregnancy Outcome
     */
    LIVE_BIRTH,

    /**
     * High Risk PNC : PNC
     */
    HIGH_RISK_PNC,

    /**
     * Normal PNC : PNC
     */
    NORMAL_PNC,

    /**
     * Gaps IN PNC : PNC
     */
    GAPS_IN_PNC,

    /**
     * Using modern family planning methods : family planning
     */
    USING_MODERN_FP,

    /**
     * Not using modern family planning methods : family planning
     */
    NOT_USING_MODERN_FP,

    CONTROLLED_BP,

    CONTROLLED_BG,

    UNCONTROLLED_BP,

    UNCONTROLLED_BG,

    GLASSES_SOLD,

    /** NCD vitals captured during a cataract camp visit (form: ncdServiceProvided = Yes). */
    NCD_SERVICE_IN_CATARACT_CAMP,

    /** Cataract form: patientReferredForOperation = Yes. */
    REFERRED_FOR_OPERATION,

    /** Eye problem: cataracts. */
    CATARACTS,

    /** Eye problem: lacrimal (tear duct) problem. */
    LECRIMAL_TEAR_DUCT_PROBLEM,

    /** Eye problem: pterygium. */
    PTERYGIUM,

    /** Eye problem: glaucoma. */
    GLAUCOMA,

    /** Eye problem: myopia. */
    MYOPIA,

    /** Eye problem: presbyopia. */
    PRESBYOPIA,

    /** Eye problem: other problem. */
    OTHER_EYE_PROBLEM,

    /** Eye problem: no problem. */
    NO_EYE_PROBLEM,

    DEFAULT,
}
