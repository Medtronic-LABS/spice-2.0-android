package org.medtroniclabs.uhis.db.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.medtroniclabs.uhis.data.DiseaseConditionItems
import org.medtroniclabs.uhis.data.HealthFacility
import org.medtroniclabs.uhis.data.offlinesync.model.FollowUpCallStatus
import org.medtroniclabs.uhis.data.offlinesync.utils.OfflineSyncStatus
import org.medtroniclabs.uhis.db.entity.LifeStyleAnswer
import org.medtroniclabs.uhis.db.entity.MemberAssessmentObservations
import org.medtroniclabs.uhis.ui.assessment.referrallogic.utils.ReferralStatus
import java.lang.reflect.Type

class OfflineStatusTypeConverter {
    @TypeConverter
    fun fromOfflineSyncStatus(syncStatus: OfflineSyncStatus): String = syncStatus.name

    @TypeConverter
    fun toOfflineSyncStatus(syncStatusString: String): OfflineSyncStatus = OfflineSyncStatus.valueOf(syncStatusString)

    @TypeConverter
    fun fromReferralStatus(referralStatus: ReferralStatus): String = referralStatus.name

    @TypeConverter
    fun toReferralStatus(referralStatusString: String): ReferralStatus = ReferralStatus.valueOf(referralStatusString)

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
    fun fromFollowUpCallStatus(callStatus: FollowUpCallStatus): String = callStatus.name

    @TypeConverter
    fun toFollowUpCallStatus(status: String): FollowUpCallStatus = FollowUpCallStatus.valueOf(status)

    @TypeConverter
    fun fromHealthFacilityListToString(list: ArrayList<HealthFacility>?): String = Gson().toJson(list)

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

    @TypeConverter
    fun fromMemberAssessmentObservations(observations: MemberAssessmentObservations?): String? =
        observations?.let { Gson().toJson(it) }

    @TypeConverter
    fun toMemberAssessmentObservations(value: String?): MemberAssessmentObservations? =
        value?.let { Gson().fromJson(it, MemberAssessmentObservations::class.java) }
}
