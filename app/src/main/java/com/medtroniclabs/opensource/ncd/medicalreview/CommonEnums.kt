package com.medtroniclabs.opensource.ncd.medicalreview

import com.medtroniclabs.opensource.R

enum class CommonEnums(val title: String, val cultureValue: Int, val value: String) {
    YESTERDAY("Yesterday", R.string.yesterday, "yesterday"),
    TODAY("Today", R.string.today, "today"),
    TOMORROW("Tomorrow", R.string.tomorrow, "tomorrow"),
    REFERRED("Referred", R.string.referred, "referred"),
    ON_TREATMENT("On Treatment", R.string.on_treatment, "on treatment"),
    ENROLLED("Registered", R.string.registered, "ENROLLED"),
    NOT_ENROLLED("Not Registered", R.string.not_registered, "NOT_ENROLLED"),
    HIGH_RISK("High", R.string.high, "high risk"),
    MEDIUM_RISK("Medium", R.string.medium, "medium risk"),
    LOW_RISK("Low", R.string.low, "low risk"),
    RED_RISK("Red Risk", R.string.red_risk, "red risk"),
    WEEK("This Week", R.string.this_week, "week"),
    MONTH("This Month", R.string.this_month, "month"),
    CUSTOMISE("Customize", R.string.customize, "customize"),
    ALIVE_AND_WELL("Alive & Well", R.string.alive_well, "Alive & Well"),
    MATERNAL_DEATH("Maternal Death", R.string.maternal_death, "Maternal Death"),
    STILL_BIRTH("Still Birth", R.string.still_birth, "Still Birth"),
    LIVE_BIRTH("Live Birth", R.string.live_birth, "Live Birth"),
    NEO_NATAL_DEATH("Neo Natal Death", R.string.neo_natal_death, "Neo Natal Death")
}