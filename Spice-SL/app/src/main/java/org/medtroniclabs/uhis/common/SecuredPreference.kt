package org.medtroniclabs.uhis.common

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.medtroniclabs.uhis.data.IdentityType
import org.medtroniclabs.uhis.data.LoginResponse
import org.medtroniclabs.uhis.data.offlinesync.model.FollowUpCriteria
import org.medtroniclabs.uhis.mappingkey.Screening
import org.medtroniclabs.uhis.model.CulturePreference
import java.lang.reflect.Type

object SecuredPreference {
    enum class EnvironmentKey {
        TOKEN,
        USERNAME,
        PHONE_NUMBER,
        PASSWORD,
        ISLOGGEDIN,
        ISOFFLINELOGIN,
        ISMETALOADED,
        USER_RESPONSE,
        IS_ABOVE_FIVE_YEARS_LOADED,
        USER_ID,
        DEVICE_ID,
        USER_FHIR_ID,
        ORGANIZATION_ID,
        ORGANIZATION_FHIR_ID,
        IS_MOTHER_NEONATE_LOADEDANC,
        IS_UNDER_TWO_MONTHS_LOADED,
        IS_LABOUR_DELIVERY_LOADED,
        OFFLINE_SYNC_REQUEST_ID,
        FOLLOW_UP_CRITERIA,
        DEFAULT_SITE_ID,
        IS_UNDER_FIVE_YEARS_LOADED,
        VILLAGE_IDS,
        SERVER_LAST_SYNCED,
        IS_MOTHER_LOADED_PNC,
        IS_NEONATE_LOADED_PNC,
        TENANT_ID,
        DISTRICT_ID,
        CHIEFDOM_ID,
        IDENTITY_TYPES,
        IS_NON_NCD_WORKFLOW_ENABLED,
        REMAINING_ATTEMPTS_COUNT,
        CURRENT_LATITUDE,
        CURRENT_LONGITUDE,
        MEASUREMENT_TYPE_KEY,
        IS_NCD_MEDICAL_REVIEW_LOADED,
        IS_PSYCHOLOGICAL_FLOW_ENABLED,
        IS_NON_COMMUNITY,
        IS_COMMUNITY,
        IS_TRANSLATION_ENABLED,
        APPLICATION,
        PREGNANCY_ANC_ENABLED_SITE,
        IS_TERMS_AND_CONDITIONS_APPROVED,
        IS_DEFAULT_SITE_ID,
        LINKED_VILLAGE_IDS,
        LINKED_VILLAGE_IDS_ALTER,
        NCD_FOLLOW_UP_LAST_SYNCED,
        NCD_FOLLOW_UP_ATTEMPTS,
        NCD_FOLLOW_UP_SCREENING_REMAINING_DAYS,
        NCD_FOLLOW_UP_ASSESSMENT_REMAINING_DAYS,
        NCD_FOLLOW_UP_MEDICAL_REVIEW_REMAINING_DAYS,
        NCD_FOLLOW_UP_LOST_REMAINING_DAYS,
        INITIAL_CALL,
        OFFLINE_FOLLOW_UP_SYNC_REQUEST_ID,
        USER_CULTURE,
        OLD_USER_ID,
        IS_TB_LOADED,
        PEER_SUPERVISOR_ID,
        BACKGROUNDTIMESTAMP,
        PEER_SUPERVISOR_NOTIFICATION_TOKEN,
        IS_FAMILY_PLANNING_LOADED,
        IS_HIV_DATA_LOADED,

        /**
         * Flag to store whether duplicate assessment history are deleted
         */
        DELETED_DUPLICATE_ASSESSMENT_HISTORY,
    }

    private const val DEFAULT_SUFFIX = "_preferences"

    private var mPrefs: SharedPreferences? = null

    var notificationIds: List<Int>? = null

    fun build(
        mKey: String,
        context: Context,
    ) {
        initPrefs(context, mKey + DEFAULT_SUFFIX)
    }

    private fun initPrefs(
        context: Context,
        prefsName: String,
    ) {
        val mainKeyAlias = MasterKey
            .Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        mPrefs = EncryptedSharedPreferences.create(
            context,
            prefsName,
            mainKeyAlias,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    /**
     * Returns the underlying SharedPreference instance
     *
     * @return an instance of the SharedPreference
     * @throws RuntimeException if SharedPreference instance has not been instantiated yet.
     */
    private val preferences: SharedPreferences
        get() {
            if (mPrefs != null) {
                return mPrefs!!
            }
            throw RuntimeException(
                "Prefs class not correctly instantiated. Please call Builder.setContext().build() in the Application class onCreate.",
            )
        }

    /**
     * @return Returns a map containing a list of pairs key/value representing
     * the preferences.
     * @see android.content.SharedPreferences.getAll
     */
    val all: Map<String, *>
        get() = preferences.all

    /**
     * Retrieves a stored int value.
     *
     * @param key      The name of the preference to retrieve.
     * @param defValue Value to return if this preference does not exist.
     * @return Returns the preference value if it exists, or defValue.
     * @throws ClassCastException if there is a preference with this name that is not
     * an int.
     * @see android.content.SharedPreferences.getInt
     */
    fun getInt(
        key: String,
        defValue: Int,
    ): Int = preferences.getInt(key, defValue)

    /**
     * Retrieves a stored int value, or 0 if the preference does not exist.
     *
     * @param key      The name of the preference to retrieve.
     * @return Returns the preference value if it exists, or 0.
     * @throws ClassCastException if there is a preference with this name that is not
     * an int.
     * @see android.content.SharedPreferences.getInt
     */
    fun getInt(key: String): Int = preferences.getInt(key, 0)

    /**
     * Retrieves a stored boolean value.
     *
     * @param key      The name of the preference to retrieve.
     * @param defValue Value to return if this preference does not exist.
     * @return Returns the preference value if it exists, or defValue.
     * @throws ClassCastException if there is a preference with this name that is not a boolean.
     * @see android.content.SharedPreferences.getBoolean
     */
    fun getBoolean(
        key: String,
        defValue: Boolean,
    ): Boolean = preferences.getBoolean(key, defValue)

    /**
     * Retrieves a stored boolean value, or false if the preference does not exist.
     *
     * @param key      The name of the preference to retrieve.
     * @return Returns the preference value if it exists, or false.
     * @throws ClassCastException if there is a preference with this name that is not a boolean.
     * @see android.content.SharedPreferences.getBoolean
     */
    fun getBoolean(key: String): Boolean = preferences.getBoolean(key, false)

    fun getBoolean(key: EnvironmentKey): Boolean = getBoolean(key.name)

    /**
     * Retrieves a stored long value.
     *
     * @param key      The name of the preference to retrieve.
     * @param defValue Value to return if this preference does not exist.
     * @return Returns the preference value if it exists, or defValue.
     * @throws ClassCastException if there is a preference with this name that is not a long.
     * @see android.content.SharedPreferences.getLong
     */
    fun getLong(
        key: String,
        defValue: Long,
    ): Long = preferences.getLong(key, defValue)

    /**
     * Retrieves a stored long value, or 0 if the preference does not exist.
     *
     * @param key      The name of the preference to retrieve.
     * @return Returns the preference value if it exists, or 0.
     * @throws ClassCastException if there is a preference with this name that is not a long.
     * @see android.content.SharedPreferences.getLong
     */
    fun getLong(key: String): Long = preferences.getLong(key, 0L)

    /**
     * Returns the double that has been saved as a long raw bits value in the long preferences.
     *
     * @param key      The name of the preference to retrieve.
     * @param defValue the double Value to return if this preference does not exist.
     * @return Returns the preference value if it exists, or defValue.
     * @throws ClassCastException if there is a preference with this name that is not a long.
     * @see android.content.SharedPreferences.getLong
     */
    fun getDouble(
        key: String,
        defValue: Double,
    ): Double =
        java.lang.Double.longBitsToDouble(
            preferences.getLong(
                key,
                java.lang.Double.doubleToLongBits(defValue),
            ),
        )

    /**
     * Returns the double that has been saved as a long raw bits value in the long preferences.
     * Returns 0 if the preference does not exist.
     *
     * @param key      The name of the preference to retrieve.
     * @return Returns the preference value if it exists, or 0.
     * @throws ClassCastException if there is a preference with this name that is not a long.
     * @see android.content.SharedPreferences.getLong
     */
    fun getDouble(key: String): Double =
        java.lang.Double.longBitsToDouble(
            preferences.getLong(
                key,
                java.lang.Double.doubleToLongBits(0.0),
            ),
        )

    /**
     * Retrieves a stored float value.
     *
     * @param key      The name of the preference to retrieve.
     * @param defValue Value to return if this preference does not exist.
     * @return Returns the preference value if it exists, or defValue.
     * @throws ClassCastException if there is a preference with this name that is not a float.
     * @see android.content.SharedPreferences.getFloat
     */
    fun getFloat(
        key: String,
        defValue: Float,
    ): Float = preferences.getFloat(key, defValue)

    /**
     * Retrieves a stored float value, or 0 if the preference does not exist.
     *
     * @param key      The name of the preference to retrieve.
     * @return Returns the preference value if it exists, or 0.
     * @throws ClassCastException if there is a preference with this name that is not a float.
     * @see android.content.SharedPreferences.getFloat
     */
    fun getFloat(key: String): Float = preferences.getFloat(key, 0.0f)

    /**
     * Retrieves a stored String value.
     *
     * @param key      The name of the preference to retrieve.
     * @param defValue Value to return if this preference does not exist.
     * @return Returns the preference value if it exists, or defValue.
     * @throws ClassCastException if there is a preference with this name that is not a String.
     * @see android.content.SharedPreferences.getString
     */
    fun getString(
        key: String,
        defValue: String?,
    ): String? = preferences.getString(key, defValue)

    /**
     * Retrieves a stored String value, or an empty string if the preference does not exist.
     *
     * @param key      The name of the preference to retrieve.
     * @return Returns the preference value if it exists, or "".
     * @throws ClassCastException if there is a preference with this name that is not a String.
     * @see android.content.SharedPreferences.getString
     */
    fun getString(key: String): String? = preferences.getString(key, null)

    /**
     * Stores a long value.
     *
     * @param key   The name of the preference to modify.
     * @param value The new value for the preference.
     * @see android.content.SharedPreferences.Editor.putLong
     */
    fun putLong(
        key: String,
        value: Long,
    ) {
        val editor = preferences.edit()
        editor.putLong(key, value)
        editor.apply()
    }

    /**
     * Stores an integer value.
     *
     * @param key   The name of the preference to modify.
     * @param value The new value for the preference.
     * @see android.content.SharedPreferences.Editor.putInt
     */
    fun putInt(
        key: String,
        value: Int,
    ) {
        val editor = preferences.edit()
        editor.putInt(key, value)
        editor.apply()
    }

    /**
     * Stores a double value as a long raw bits value.
     *
     * @param key   The name of the preference to modify.
     * @param value The double value to be save in the preferences.
     * @see android.content.SharedPreferences.Editor.putLong
     */
    fun putDouble(
        key: String,
        value: Double,
    ) {
        val editor = preferences.edit()
        editor.putLong(key, java.lang.Double.doubleToRawLongBits(value))
        editor.apply()
    }

    /**
     * Stores a float value.
     *
     * @param key   The name of the preference to modify.
     * @param value The new value for the preference.
     * @see android.content.SharedPreferences.Editor.putFloat
     */
    fun putFloat(
        key: String,
        value: Float,
    ) {
        val editor = preferences.edit()
        editor.putFloat(key, value)
        editor.apply()
    }

    /**
     * Stores a boolean value.
     *
     * @param key   The name of the preference to modify.
     * @param value The new value for the preference.
     * @see android.content.SharedPreferences.Editor.putBoolean
     */
    fun putBoolean(
        key: String,
        value: Boolean,
    ) {
        val editor = preferences.edit()
        editor.putBoolean(key, value)
        editor.apply()
    }

    fun putBoolean(
        key: EnvironmentKey,
        value: Boolean,
    ) {
        putBoolean(key.name, value)
    }

    /**
     * Stores a String value.
     *
     * @param key   The name of the preference to modify.
     * @param value The new value for the preference.
     * @see android.content.SharedPreferences.Editor.putString
     */
    fun putString(
        key: String,
        value: String?,
    ) {
        val editor = preferences.edit()
        editor.putString(key, value)
        editor.apply()
    }

    /**
     * Removes a preference value.
     *
     * @param key The name of the preference to remove.
     * @see android.content.SharedPreferences.Editor.remove
     */
    fun remove(key: String) {
        val prefs = preferences
        prefs.edit {
            remove(key)
        }
    }

    /**
     * Removes a preference value.
     *
     * @param key The name of the preference to remove.
     * @see android.content.SharedPreferences.Editor.remove
     */
    fun remove(key: EnvironmentKey) {
        remove(key.name)
    }

    /**
     * Checks if a value is stored for the given key.
     *
     * @param key The name of the preference to check.
     * @return `true` if the storage contains this key value, `false` otherwise.
     * @see android.content.SharedPreferences.contains
     */
    operator fun contains(key: String): Boolean = preferences.contains(key)

    fun saveStringArray(
        key: String,
        array: Array<String>,
    ) {
        val editor = preferences.edit()
        val set = HashSet<String>()
        set.addAll(array)
        editor.putStringSet(key, set)
        editor.apply()
    }

    fun getStringArray(key: String): Array<String>? {
        val set = preferences.getStringSet(key, null)
        return set?.toTypedArray()
    }

    fun saveLongList(
        key: String,
        list: List<Long>,
    ) {
        val editor = preferences.edit()
        val jsonString = Gson().toJson(list)
        editor.putString(key, jsonString)
        editor.apply()
    }

    fun getLongList(key: String): List<Long> {
        val jsonString = preferences.getString(key, null)
        return if (jsonString != null) {
            val type = object : TypeToken<List<Long>>() {}.type
            Gson().fromJson(jsonString, type)
        } else {
            emptyList()
        }
    }

    fun clear(context: Context) {
        val username = getString(EnvironmentKey.USERNAME.name)
        val phoneNumber = getString(EnvironmentKey.PHONE_NUMBER.name)
        val password = getString(EnvironmentKey.PASSWORD.name)
        val requestIds =
            getStringArray(EnvironmentKey.OFFLINE_SYNC_REQUEST_ID.name)
        val followUpCriteria = getString(EnvironmentKey.FOLLOW_UP_CRITERIA.name)
        val villageIds = getString(EnvironmentKey.VILLAGE_IDS.name)
        val serverLastSyncedTime = getString(EnvironmentKey.SERVER_LAST_SYNCED.name)
        val deviceId = getString(EnvironmentKey.DEVICE_ID.name)
        val community = getBoolean(EnvironmentKey.IS_COMMUNITY.name)
        val nonCommunity = getBoolean(EnvironmentKey.IS_NON_COMMUNITY.name)
        val isTermsAndConditionsApproved = getBoolean(EnvironmentKey.IS_TERMS_AND_CONDITIONS_APPROVED.name)
        val userFhirId = getString(EnvironmentKey.USER_FHIR_ID.name)
        val organizationFhirId = getString(EnvironmentKey.ORGANIZATION_FHIR_ID.name)
        val spiceUserId = getLong(EnvironmentKey.USER_ID.name)
        try {
            preferences.edit().clear().apply()
        } catch (e: Exception) {
            val sharedPreferences =
                context.getSharedPreferences("SecuredPreference", Context.MODE_PRIVATE)
            sharedPreferences.edit().clear().apply()
        } finally {
            putString(EnvironmentKey.USERNAME.name, username)
            putString(EnvironmentKey.PHONE_NUMBER.name, phoneNumber)
            putString(EnvironmentKey.PASSWORD.name, password)
            putString(EnvironmentKey.USER_FHIR_ID.name, userFhirId)
            putString(EnvironmentKey.ORGANIZATION_FHIR_ID.name, organizationFhirId)
            putLong(EnvironmentKey.USER_ID.name, spiceUserId)
            putString(EnvironmentKey.FOLLOW_UP_CRITERIA.name, followUpCriteria)
            requestIds?.let { saveStringArray(EnvironmentKey.OFFLINE_SYNC_REQUEST_ID.name, it) }
            putString(EnvironmentKey.VILLAGE_IDS.name, villageIds)
            putString(EnvironmentKey.SERVER_LAST_SYNCED.name, serverLastSyncedTime)
            putString(EnvironmentKey.DEVICE_ID.name, deviceId)
            putBoolean(EnvironmentKey.IS_COMMUNITY.name, community)
            putBoolean(EnvironmentKey.IS_NON_COMMUNITY.name, nonCommunity)
            putBoolean(EnvironmentKey.IS_TERMS_AND_CONDITIONS_APPROVED.name, isTermsAndConditionsApproved)
        }
    }

    fun putUserDetails(loginResponse: LoginResponse) {
        val loginResponseString = Gson().toJson(loginResponse)
        putLong(EnvironmentKey.USER_ID.name, loginResponse.id)
        putString(EnvironmentKey.USER_RESPONSE.name, loginResponseString)
    }

    fun getUserDetails(): LoginResponse? {
        val userResponseString = getString(EnvironmentKey.USER_RESPONSE.name)
        val type: Type = object : TypeToken<LoginResponse>() {}.type
        return try {
            Gson().fromJson(userResponseString, type)
        } catch (e: Exception) {
            null
        }
    }

    fun putFollowUpCriteria(followUpCriteria: FollowUpCriteria) {
        val followUpCriteriaString = Gson().toJson(followUpCriteria)
        putString(EnvironmentKey.FOLLOW_UP_CRITERIA.name, followUpCriteriaString)
    }

    fun getFollowUpCriteria(): FollowUpCriteria? {
        val followUpCriteriaString = getString(EnvironmentKey.FOLLOW_UP_CRITERIA.name)
        val type: Type = object : TypeToken<FollowUpCriteria>() {}.type
        return Gson().fromJson(followUpCriteriaString, type)
    }

    fun getUserId(): Long = getLong(EnvironmentKey.USER_ID.name)

    fun getOldUserId(): Long = getLong(EnvironmentKey.OLD_USER_ID.name)

    fun getDeviceId(): String? = getString(EnvironmentKey.DEVICE_ID.name)

    fun getPhoneNumberCode(): String? = getUserDetails()?.country?.phoneNumberCode

    fun getRole(): String = getUserDetails()?.roles?.first()?.name ?: ""

    fun getTermsAndConditionsStatus(): Boolean = getBoolean(EnvironmentKey.IS_TERMS_AND_CONDITIONS_APPROVED.name)

    fun putIdentityTypes(types: List<IdentityType>) {
        val typesString = Gson().toJson(types)
        putString(EnvironmentKey.IDENTITY_TYPES.name, typesString)
    }

    fun getIdentityTypes(): List<IdentityType>? {
        val identityTypesString = getString(EnvironmentKey.IDENTITY_TYPES.name)
        val type: Type = object : TypeToken<List<IdentityType>>() {}.type
        return Gson().fromJson(identityTypesString, type)
    }

    fun logout(isNetworkAvailable: Boolean): Boolean {
        if (isNetworkAvailable) { // If network available usual logout
            return logout()
        } else { // If network not available the clear logged in flag
            remove(EnvironmentKey.ISLOGGEDIN.name)
            remove(EnvironmentKey.ISOFFLINELOGIN.name)
        }
        return true
    }

    fun logout(): Boolean {
        remove(EnvironmentKey.ISLOGGEDIN.name)
        remove(EnvironmentKey.TOKEN.name)
        remove(EnvironmentKey.ISOFFLINELOGIN.name)
//        remove(EnvironmentKey.USER_RESPONSE.name)
        remove(EnvironmentKey.IS_ABOVE_FIVE_YEARS_LOADED.name)
        remove(EnvironmentKey.IS_MOTHER_NEONATE_LOADEDANC.name)
        remove(EnvironmentKey.IS_TB_LOADED.name)
        remove(EnvironmentKey.IS_HIV_DATA_LOADED.name)
        remove(EnvironmentKey.IS_UNDER_TWO_MONTHS_LOADED.name)
        remove(EnvironmentKey.IS_LABOUR_DELIVERY_LOADED.name)
        remove(EnvironmentKey.IS_UNDER_FIVE_YEARS_LOADED.name)
        remove(EnvironmentKey.IS_NCD_MEDICAL_REVIEW_LOADED.name)
        remove(EnvironmentKey.IS_FAMILY_PLANNING_LOADED.name)
        return true
    }

    fun getUserFhirId(): String = getString(EnvironmentKey.USER_FHIR_ID.name) ?: ""

    fun getOrganizationId(): Long = getLong(EnvironmentKey.ORGANIZATION_ID.name)

    fun getOrganizationFhirId(): String = getString(EnvironmentKey.ORGANIZATION_FHIR_ID.name) ?: ""

    fun getTenantId(): Long = getLong(EnvironmentKey.TENANT_ID.name)

    fun getDistrictId(): Long = getLong(EnvironmentKey.DISTRICT_ID.name)

    fun getChiefdomId(): Long = getLong(EnvironmentKey.CHIEFDOM_ID.name)

    fun getUnitMeasurementType(): String {
        getString(
            EnvironmentKey.MEASUREMENT_TYPE_KEY.name,
            Screening.Unit_Measurement_Metric_Type,
        )?.let {
            return it
        } ?: kotlin.run {
            return Screening.Unit_Measurement_Metric_Type
        }
    }

    fun getTimeZoneId(): String? {
        getString(EnvironmentKey.USER_RESPONSE.name)?.let { userResponseString ->
            val type: Type = object : TypeToken<LoginResponse>() {}.type
            val usersResponse = Gson().fromJson<LoginResponse>(userResponseString, type)
            return usersResponse.timezone?.offset
        }
        return null
    }

    fun getDeviceID(): Long? = getUserDetails()?.deviceInfoId

    fun getCountryId(): Long? = getUserDetails()?.country?.id

    fun isAncEnabled(): Boolean = getBoolean(EnvironmentKey.PREGNANCY_ANC_ENABLED_SITE.name)

    fun setUserPreference(
        locale: Long,
        name: String,
        enabled: Boolean,
    ) {
        val culture = getCulturePreference()
        if (culture == null) {
            saveCulturePreference(CulturePreference(locale, name, enabled))
        } else {
            culture.cultureId = locale
            culture.name = name
            culture.isTranslationEnabled = enabled
            saveCulturePreference(culture)
        }
    }

    fun getIsTranslationEnabled() = getCulturePreference()?.isTranslationEnabled ?: false

    fun getCultureName() = getCulturePreference()?.name ?: DefinedParams.EN_Locale

    private fun saveCulturePreference(model: CulturePreference) {
        val culture = Gson().toJson(model)
        putString(EnvironmentKey.USER_CULTURE.name, culture)
    }

    /**
     * Saves culture preference synchronously (commit) so it is visible immediately
     * before navigating to login screen after language change.
     */
    fun setUserPreferenceSync(
        locale: Long,
        name: String,
        enabled: Boolean,
    ) {
        val culture = getCulturePreference()
        val toSave = if (culture == null) {
            CulturePreference(locale, name, enabled)
        } else {
            culture.cultureId = locale
            culture.name = name
            culture.isTranslationEnabled = enabled
            culture
        }
        val cultureJson = Gson().toJson(toSave)
        preferences.edit().putString(EnvironmentKey.USER_CULTURE.name, cultureJson).commit()
    }

    private fun getCulturePreference(): CulturePreference? {
        val userResponseString = getString(EnvironmentKey.USER_CULTURE.name)
        val type: Type = object : TypeToken<CulturePreference>() {}.type
        return Gson().fromJson(userResponseString, type)
    }

    fun getCultureId() = getCulturePreference()?.cultureId ?: 0

    fun removePeerSupervisorToken() {
        remove(EnvironmentKey.PEER_SUPERVISOR_NOTIFICATION_TOKEN.name)
    }
}
