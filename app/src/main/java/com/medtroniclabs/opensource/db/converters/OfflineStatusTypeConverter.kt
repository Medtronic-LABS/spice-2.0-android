package com.medtroniclabs.opensource.db.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.medtroniclabs.opensource.data.DiseaseConditionItems
import com.medtroniclabs.opensource.data.HealthFacility
import com.medtroniclabs.opensource.data.offlinesync.model.FollowUpCallStatus
import com.medtroniclabs.opensource.data.offlinesync.utils.OfflineSyncStatus
import com.medtroniclabs.opensource.db.entity.LifeStyleAnswer
import com.medtroniclabs.opensource.ui.assessment.referrallogic.utils.ReferralStatus
import java.lang.reflect.Type

class OfflineStatusTypeConverter {

    @TypeConverter
    fun fromOfflineSyncStatus(syncStatus: OfflineSyncStatus): String {
        return syncStatus.name
    }

    @TypeConverter
    fun toOfflineSyncStatus(syncStatusString: String): OfflineSyncStatus {
        return OfflineSyncStatus.valueOf(syncStatusString)
    }

    @TypeConverter
    fun fromReferralStatus(referralStatus: ReferralStatus): String {
        return referralStatus.name
    }

    @TypeConverter
    fun toReferralStatus(referralStatusString: String): ReferralStatus {
        return ReferralStatus.valueOf(referralStatusString)
    }

    @TypeConverter
    fun fromString(value: String?): ArrayList<String?>? {
        val listType: Type = object : TypeToken<ArrayList<String?>?>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromArrayList(list: ArrayList<String?>?): String? {
        val gson = Gson()
        return gson.toJson(list)
    }

    @TypeConverter
    fun fromDiseaseConditionString(value: String?): ArrayList<DiseaseConditionItems?>? {
        val listType: Type = object : TypeToken<ArrayList<DiseaseConditionItems?>?>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromDiseaseConditionArrayList(list: ArrayList<DiseaseConditionItems?>?): String? {
        val gson = Gson()
        return gson.toJson(list)
    }

    @TypeConverter
    fun fromFollowUpCallStatus(callStatus: FollowUpCallStatus): String {
        return callStatus.name
    }

    @TypeConverter
    fun toFollowUpCallStatus(status: String): FollowUpCallStatus {
        return FollowUpCallStatus.valueOf(status)
    }

    @TypeConverter
    fun fromHealthFacilityListToString(list: ArrayList<HealthFacility>?): String {
        return Gson().toJson(list)
    }

    @TypeConverter
    fun fromStringToHealthFacilityList(value: String): ArrayList<HealthFacility> {
        val listType = object : TypeToken<ArrayList<HealthFacility?>?>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromLifestyleAnswersList(list: ArrayList<LifeStyleAnswer>): String {
        val gson = Gson()
        return gson.toJson(list)
    }

    @TypeConverter
    fun toLifestyleAnswersList(value: String): ArrayList<LifeStyleAnswer> {
        val listType = object : TypeToken<ArrayList<LifeStyleAnswer?>?>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun toLongList(value: String): ArrayList<Long> {
        val listType: Type = object : TypeToken<ArrayList<Long>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromLongList(list: ArrayList<Long>): String {
        val gson = Gson()
        return gson.toJson(list)
    }
}