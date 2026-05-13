package org.medtroniclabs.uhis.ui.assessment.viewmodel

import android.content.Context
import android.location.Location
import android.text.SpannableStringBuilder
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.color
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.model.UserDetail
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.appextensions.convertToUtcDateTime
import org.medtroniclabs.uhis.appextensions.getLocalDate
import org.medtroniclabs.uhis.appextensions.postError
import org.medtroniclabs.uhis.appextensions.postLoading
import org.medtroniclabs.uhis.appextensions.postSuccess
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.DefinedParams.CBS
import org.medtroniclabs.uhis.common.DefinedParams.CONTACT_TRACING
import org.medtroniclabs.uhis.common.DefinedParams.CbsNotifiableCondition
import org.medtroniclabs.uhis.common.DefinedParams.IccmDiarrheaNotifiableCondition
import org.medtroniclabs.uhis.common.DefinedParams.IccmFeverNotifiableCondition
import org.medtroniclabs.uhis.common.DefinedParams.NotifiableConditions
import org.medtroniclabs.uhis.common.DefinedParams.OtherNotifiableConditions
import org.medtroniclabs.uhis.common.DefinedParams.OtherNotifiableConditionsForDiarrhoea
import org.medtroniclabs.uhis.common.DefinedParams.OtherNotifiableConditionsForFever
import org.medtroniclabs.uhis.common.DefinedParams.RmnchNotifiableCondition
import org.medtroniclabs.uhis.common.DefinedParams.RxBuddyId
import org.medtroniclabs.uhis.common.DefinedParams.TB
import org.medtroniclabs.uhis.common.DefinedParams.surveillanceDetails
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.common.SpiceLocationManager
import org.medtroniclabs.uhis.common.StringConverter
import org.medtroniclabs.uhis.data.LocalSpinnerResponse
import org.medtroniclabs.uhis.data.UserProfile
import org.medtroniclabs.uhis.data.model.RecommendedDosageListModel
import org.medtroniclabs.uhis.data.model.SymptomModel
import org.medtroniclabs.uhis.data.offlinesync.utils.OfflineConstant
import org.medtroniclabs.uhis.db.entity.AssessmentEntity
import org.medtroniclabs.uhis.db.entity.HealthFacilityEntity
import org.medtroniclabs.uhis.db.entity.MedicalComplianceEntity
import org.medtroniclabs.uhis.db.entity.MemberAssessmentHistoryEntity
import org.medtroniclabs.uhis.db.entity.MemberClinicalEntity
import org.medtroniclabs.uhis.db.entity.PregnancyDetail
import org.medtroniclabs.uhis.db.entity.RiskClassificationModel
import org.medtroniclabs.uhis.db.entity.RxBuddyDetails
import org.medtroniclabs.uhis.db.entity.SignsAndSymptomsEntity
import org.medtroniclabs.uhis.db.entity.TreatmentDetailsEntity
import org.medtroniclabs.uhis.di.IoDispatcher
import org.medtroniclabs.uhis.formgeneration.FormGenerator
import org.medtroniclabs.uhis.formgeneration.config.ViewType
import org.medtroniclabs.uhis.formgeneration.model.BPModel
import org.medtroniclabs.uhis.formgeneration.model.FormLayout
import org.medtroniclabs.uhis.formgeneration.model.FormResponse
import org.medtroniclabs.uhis.mappingkey.HouseHoldRegistration
import org.medtroniclabs.uhis.mappingkey.MemberRegistration
import org.medtroniclabs.uhis.mappingkey.PregnantWomen
import org.medtroniclabs.uhis.mappingkey.RxBuddy.hasCough
import org.medtroniclabs.uhis.mappingkey.RxBuddy.hasProvidedMonitoringSheet
import org.medtroniclabs.uhis.mappingkey.RxBuddy.otherRelationShip
import org.medtroniclabs.uhis.mappingkey.RxBuddy.relationshipToPatient
import org.medtroniclabs.uhis.mappingkey.RxBuddy.rxBuddy
import org.medtroniclabs.uhis.mappingkey.RxBuddy.rxBuddyMonitoringDates
import org.medtroniclabs.uhis.mappingkey.RxBuddy.rxBuddyName
import org.medtroniclabs.uhis.mappingkey.RxBuddy.rxBuddyPhoneNumber
import org.medtroniclabs.uhis.mappingkey.RxBuddy.selectHouseholdMember
import org.medtroniclabs.uhis.mappingkey.RxBuddy.tbScreening
import org.medtroniclabs.uhis.mappingkey.Screening
import org.medtroniclabs.uhis.mappingkey.Screening.BMI_CATEGORY
import org.medtroniclabs.uhis.mappingkey.UnderFiveYearExaminationKeyMapping.DiseaseName.fever
import org.medtroniclabs.uhis.model.assessment.AssessmentMemberDetails
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.network.utils.ConnectivityManager
import org.medtroniclabs.uhis.repo.AssessmentRepository
import org.medtroniclabs.uhis.repo.HouseHoldRepository
import org.medtroniclabs.uhis.repo.HouseholdMemberRepository
import org.medtroniclabs.uhis.repo.RxBuddyRepository
import org.medtroniclabs.uhis.repo.TreatmentDetailsRepository
import org.medtroniclabs.uhis.ui.BaseViewModel
import org.medtroniclabs.uhis.ui.MenuConstants
import org.medtroniclabs.uhis.ui.MenuConstants.CATARACT_MENU_ID
import org.medtroniclabs.uhis.ui.MenuConstants.EYE_CARE_MENU_ID
import org.medtroniclabs.uhis.ui.MenuConstants.ICCM_MENU_ID
import org.medtroniclabs.uhis.ui.MenuConstants.NCD_MENU_ID
import org.medtroniclabs.uhis.ui.MenuConstants.OTHER_SYMPTOMS
import org.medtroniclabs.uhis.ui.MenuConstants.PREGNANCY_OUTCOME
import org.medtroniclabs.uhis.ui.MenuConstants.PREGNANT_WOMEN_PROFILE
import org.medtroniclabs.uhis.ui.MenuConstants.TB_MENU_ID
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.BIO_METRICS
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.BP_LOG
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.BP_LOG_DETAILS
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.EYE_CARE
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.EYE_DISEASE
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.EYE_DISEASE_
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.EYE_TEST_OUTCOME
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.EYE_TEST_OUTCOMES
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.FACILITY_TYPE_COMMUNITY_CLINIC
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.FACILITY_TYPE_UPAZILA
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.FamilyPlanning
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.FamilyPlanningDetails
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.GLUCOSE_LOG
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.HISTORY_OF_OTHER_DISEASES_
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.IS_BEFORE_DIABETES_DIAGNOSIS
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.IS_BEFORE_HTN_DIAGNOSIS
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.IsClinicTaken
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.OPERATION_NAME_
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.REASON_
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.REFERRAL_FACILITY_TYPE
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.REFERRED_SITE
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.STATUS_FACILITY_TYPE_COMMUNITY_CLINIC
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.STATUS_FACILITY_TYPE_UPAZILA
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.TBContactTracing
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.TBRxBuddyFollowUp
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.TBRxBuddyRegister
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.TBScreening
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.ncd
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.otherSymptoms
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.signsAndSymptoms
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.symptoms
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.symptomsDTO
import org.medtroniclabs.uhis.ui.assessment.AssessmentNCDEntity
import org.medtroniclabs.uhis.ui.assessment.referrallogic.ReferralResultGenerator
import org.medtroniclabs.uhis.ui.assessment.referrallogic.model.ReferralDefinedParams.Diarrhoea
import org.medtroniclabs.uhis.ui.assessment.referrallogic.utils.ReferralReasons
import org.medtroniclabs.uhis.ui.assessment.referrallogic.utils.ReferralStatus
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH.ANC
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH.DEATH_OF_MOTHER
import org.medtroniclabs.uhis.ui.assessment.statuslogic.AssessmentStatusGenerator
import org.medtroniclabs.uhis.ui.assessment.utils.AssessmentUtil
import org.medtroniclabs.uhis.ui.boarding.repo.MetaRepository
import java.lang.reflect.Type
import java.time.LocalDate
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlin.Any
import kotlin.collections.HashMap

@HiltViewModel
class AssessmentViewModel @Inject constructor(
    @IoDispatcher override var dispatcherIO: CoroutineDispatcher,
    private var memberRegistrationRepository: HouseholdMemberRepository,
    private var assessmentRepository: AssessmentRepository,
    private val metaRepository: MetaRepository,
    private val houseHoldRepository: HouseHoldRepository,
    private val rxBuddyRepository: RxBuddyRepository,
    private val treatmentDetailsRepository: TreatmentDetailsRepository,
) : BaseViewModel(dispatcherIO) {
    var selectedHouseholdMemberId = -1L
    var memberFhirId: String? = null
    var selectedHouseholdId = -1L
    var entryPoint: String? = null
    var followUpId: Long? = null
    val assessmentSaveLiveData = MutableLiveData<Resource<Pair<List<FormLayout>?, AssessmentEntity>>>()
    val assessmentHistoryResultLiveData = MutableLiveData<Resource<MemberAssessmentHistoryEntity>>()
    val assessmentStringLiveData = MutableLiveData<String?>()
    val assessmentUpdateLiveData = MutableLiveData<Resource<String>>()
    val memberDetailsLiveData = MutableLiveData<Resource<AssessmentMemberDetails>>()
    val assessmentTBType = MutableLiveData<String>()
    var menuId: String? = null
    var workflowName: String? = null
    var symptomTypeListResponse = MutableLiveData<List<SignsAndSymptomsEntity>>()
    var symptomListResponse = MutableLiveData<List<SignsAndSymptomsEntity>>()
    var otherAssessmentDetails = HashMap<String, Any>()
    val formLayoutsLiveData = MutableLiveData<Resource<FormResponse>>()
    val nearestFacilityLiveData = MutableLiveData<Resource<ArrayList<Map<String, Any>>>>()
    var referralStatus: String? = null
    var lastLocation: Location? = null
    val memberClinicalLiveData = MutableLiveData<MemberClinicalEntity?>()
    var dosageListModel: ArrayList<RecommendedDosageListModel>? = null
    var instructionId: String? = null
    val treatmentDays = HashMap<String, Int>()
    var referralReason: ArrayList<String>? = null
    var pregnancyDetailForMother: PregnancyDetail? = null
    var selectedMemberDob: String? = null
    var selectedSymptoms = MutableLiveData<List<SymptomModel>>()
    var medicationParentComplianceResponse = MutableLiveData<List<MedicalComplianceEntity>>()
    var selectedMedication = MutableLiveData<MedicalComplianceEntity?>()
    var medicationChildComplianceResponse = MutableLiveData<List<MedicalComplianceEntity>>()
    var complianceMap: ArrayList<HashMap<String, Any>>? = null
    var bioDataMap: HashMap<String, Any>? = null
    var bioMetric: HashMap<String, Any>? = null
    var riskClassificationModels = ArrayList<RiskClassificationModel>()
    private var fbsBloodGlucose: Double? = null
    private var rbsBloodGlucose: Double? = null
    var assessmentType: String? = null
    val assessmentSaveResponse =
        MutableLiveData<Resource<Pair<AssessmentNCDEntity, HashMap<String, Any>?>>>()
    var mentalHealthQuestions = MutableLiveData<Resource<HashMap<String, LocalSpinnerResponse>>>()
    private var phQ4Score: Int? = null
    var isDangerSignFlow: Boolean = false
    var ageInMonth = MutableLiveData<String>()
    var formRenderedLiveData = MutableLiveData<Boolean>()
    var muacColor: String? = null
    var hasDiarrhoea: Boolean = false
    val userProfileLiveData = MutableLiveData<Resource<UserProfile>>()
    val callResultHashMap = HashMap<String, Any>()
    val patientHealthFacility = MutableLiveData<Resource<List<HealthFacilityEntity>>>()
    val callResultSaveLiveData = MutableLiveData<Resource<Pair<List<FormLayout>?, AssessmentEntity>>>()
    val getAssessmentDetails = MutableLiveData<Resource<AssessmentEntity>>()
    var motherID: Long? = null
    val otherHouseholdMemberLiveData = MutableLiveData<Resource<ArrayList<Map<String, Any>>>>()
    val saveRxBuddyDetails = MutableLiveData<Resource<Long>>()
    val treatmentDetailsLiveData = MutableLiveData<TreatmentDetailsEntity>()
    val rxBuddyDetailsLiveData = MutableLiveData<RxBuddyDetails>()
    val saveRxBuddyFollowUpLiveData = MutableLiveData<Resource<Long>>()

    var dangerSingsKey: String? = null

    @Inject
    lateinit var connectivityManager: ConnectivityManager

    var isAssessmentCancelLiveData = MutableLiveData<Boolean>()

    init {
        SecuredPreference.getFollowUpCriteria()?.let { followUpCriteria ->
            treatmentDays[ReferralReasons.Pneumonia.name] = followUpCriteria.pneumonia
            treatmentDays[ReferralReasons.Diarrhoea.name] = followUpCriteria.diarrhea
            treatmentDays[ReferralReasons.MUAC.name] = followUpCriteria.muac
            treatmentDays[ReferralReasons.Malaria.name] = followUpCriteria.malaria
        }
    }

    var nameOfDangerSignClicked: String? = null

    /**
     * Flag related to whether given lmp date
     * during pregnancy registration is too early to access
     */
    var isPregnancyTooEarlyToAccess: Boolean = true

    /**
     * Live data storing pregnancy details
     */
    val pregnancyDetailLiveData = MutableLiveData<PregnancyDetail?>()

    fun getMemberDetailsById() {
        if (selectedHouseholdMemberId == -1L) {
            return
        }
        viewModelScope.launch(dispatcherIO) {
            memberDetailsLiveData.postLoading()
            memberDetailsLiveData.postValue(
                memberRegistrationRepository.getAssessmentMemberDetails(
                    selectedHouseholdMemberId,
                ),
            )
        }
    }

    fun saveTbAssessment(
        serverData: List<FormLayout>,
        assessmentMap: HashMap<String, Any>,
        referralResult: Pair<String?, ArrayList<String>>?,
        tbType: String,
        menuId: String?,
    ) {
        when (tbType) {
            TBScreening -> {
                saveAssessment(serverData, assessmentMap, referralResult, menuId, tbType)
            }

            TBContactTracing -> {
                saveAssessment(serverData, assessmentMap, referralResult, menuId, tbType)
            }

            TBRxBuddyRegister -> {
                saveRxBuddyRegister(assessmentMap)
            }

            TBRxBuddyFollowUp -> {
                insertRxBuddyFollowUp(assessmentMap)
            }
        }
    }

    private fun saveRxBuddyRegister(resultMap: HashMap<String, Any>) {
        viewModelScope.launch(dispatcherIO) {
            val tb = resultMap[TB_MENU_ID.lowercase()] as HashMap<String, Any>
            if (tb.containsKey(TBRxBuddyRegister)) {
                val rxBuddy = tb[TBRxBuddyRegister] as HashMap<String, Any>

                val isSheetProvider = rxBuddy[hasProvidedMonitoringSheet] as Boolean
                val relationShip = rxBuddy[relationshipToPatient] as String
                val otherRelationShip =
                    if (rxBuddy.containsKey(otherRelationShip)) rxBuddy[otherRelationShip] as String else null
                val memberId = rxBuddy[selectHouseholdMember] as Long

                var name: String? = null
                var phoneNumber: String? = null
                if (memberId != 0L) {
                    val hhm = memberRegistrationRepository.getMemberDetails(memberId)
                    rxBuddy[rxBuddyName] = hhm.name
                    rxBuddy[rxBuddyPhoneNumber] = hhm.phoneNumber ?: ""
                } else {
                    name =
                        if (rxBuddy.containsKey(rxBuddyName)) rxBuddy[rxBuddyName] as String else null
                    val phone =
                        if (rxBuddy.containsKey(rxBuddyPhoneNumber)) rxBuddy[rxBuddyPhoneNumber] as String else null
                    phoneNumber = if (phone.isNullOrEmpty()) null else phone
                }

                val patientMemberId = memberDetailsLiveData.value?.data?.memberId!!
                val nextVisitDate = getNextVisitForConfirmedTbPatient()
                nextVisitDateForTBPatientLiveData.postValue(nextVisitDate)

                val rxBuddyId = rxBuddyRepository.insertRxBuddyDetails(
                    0L,
                    patientMemberId,
                    if (memberId == 0L) null else memberId,
                    name,
                    phoneNumber,
                    relationShip,
                    otherRelationShip,
                    isSheetProvider,
                    nextVisitDate,
                    followUpId = followUpId,
                )

                val assessmentDetail = StringConverter.convertGivenMapToString(resultMap) ?: ""
                assessmentStringLiveData.postValue(assessmentDetail)
                saveRxBuddyDetails.postValue(rxBuddyId)
            }
        }
    }

    fun saveAssessment(
        serverData: List<FormLayout>?,
        assessmentMap: HashMap<String, Any>,
        referralResult: Pair<String?, ArrayList<String>>?,
        menuId: String?,
        tbType: String? = null,
    ) {
        viewModelScope.launch(dispatcherIO) {
            memberDetailsLiveData.value?.data?.let { details ->
                referralStatus = referralResult?.first
                val status = AssessmentStatusGenerator.evaluateStatus(assessmentMap, details, referralResult)
                val assessmentDetail =
                    getAssessmentDetails(serverData, assessmentMap as HashMap<Any, Any>)
                assessmentStringLiveData.postValue(assessmentDetail.first)
                referralReason = referralResult?.second
                val otherDetails = calculateOtherDetails(assessmentMap, referralStatus, menuId)
                if (tbType == TBContactTracing) {
                    memberRegistrationRepository.updateContactTracingStatus(details.id, OfflineConstant.CONTACT_TRACING_DONE)
                }

                val assessmentResult = assessmentRepository.saveAssessment(
                    assessmentDetail.second,
                    details,
                    menuId,
                    referralResult,
                    otherDetails,
                    followUpId = followUpId,
                    status = status,
                )

                assessmentResult.data?.let {
                    val history = MemberAssessmentHistoryEntity(
                        memberFhirId = details.memberId,
                        memberId = details.id,
                        visitDate = DateUtils.formatDate(
                            it.createdAt,
                        ),
                        serviceProvided = menuId?.uppercase(Locale.ENGLISH),
                        customStatus = status,
                        latestVisit = true,
                        referralStatus = getReferralStatus(referralStatus, assessmentMap),
                        referralReason = referralReason.toString(),
                        nextFollowUpDate = getNextFollowUpDate(otherDetails),
                    )
                    assessmentHistoryResultLiveData.postValue(assessmentRepository.saveAssessmentHistory(history))
                }

                if (menuId == PREGNANT_WOMEN_PROFILE &&
                    assessmentResult.isSuccess()
                ) {
                    savePregnancyDetails(details, assessmentMap)
                }

                if (menuId == PREGNANCY_OUTCOME &&
                    assessmentResult.isSuccess()
                ) {
                    savePregnancyOutcomeDetails(details, assessmentMap)
                }

                if (
                    menuId == ANC.uppercase(Locale.getDefault()) &&
                    assessmentResult.isSuccess()
                ) {
                    saveAncPregnancyDetails(details, assessmentMap)
                }

                assessmentSaveLiveData.postValue(
                    when (assessmentResult.state) {
                        ResourceState.ERROR -> Resource(state = ResourceState.ERROR)
                        ResourceState.LOADING -> Resource(state = ResourceState.LOADING)
                        ResourceState.SUCCESS -> Resource(state = assessmentResult.state, data = Pair(serverData, assessmentResult.data!!))
                    },
                )
            }
        }
    }

    private fun getNextFollowUpDate(otherDetail: HashMap<String, Any>?): String? {
        if (otherDetail != null && otherDetail.containsKey(AssessmentDefinedParams.NextFollowupDate)) {
            return otherDetail[AssessmentDefinedParams.NextFollowupDate] as String
        }
        return null
    }

    /**
     * Inserts pregnancy details from pregnant women registration
     */
    suspend fun savePregnancyDetails(
        details: AssessmentMemberDetails,
        assessmentMap: HashMap<String, Any>,
    ) {
        val pregnancyProfile = assessmentMap[PREGNANT_WOMEN_PROFILE] as? Map<String, Any?>
        val pregnancy = pregnancyProfile?.get(PregnantWomen.ID_PREGNANCY_DETAILS_AND_HISTORY) as? Map<String, Any?>
        val lmp = pregnancy?.get(PregnantWomen.ID_LMP) as? String
        var edd = ""
        if (!lmp.isNullOrBlank()) {
            edd = DateUtils.getEstDeliveryDateFromLmp(lmp)
        }

        // Create new record
        val pregnancyDetail = PregnancyDetail(
            householdMemberLocalId = details.id,
            patientId = details.patientId,
            householdMemberId = details.memberId,
            lastMenstrualPeriod = lmp,
            estimatedDeliveryDate = edd,
            pregnancyTest = pregnancy?.get(PregnantWomen.ID_PREGNANCY_TEST) as? String,
            gravida = CommonUtils.getDouble(pregnancy?.get(PregnantWomen.ID_GRAVIDA)).toInt(),
            parity = CommonUtils.getDouble(pregnancy?.get(PregnantWomen.ID_PARITY)).toInt(),
            numberOfLivingChildren = CommonUtils.getDouble(pregnancy?.get(PregnantWomen.ID_LIVING_CHILDREN)).toInt(),
            ageOfLastChild = pregnancy?.get(PregnantWomen.ID_AGE_OF_LAST_CHILD) as? String,
        )

        // Ensure pregnancyEpisodeId and timestamps are set
        ensurePregnancyEpisodeIdAndTimestamps(pregnancyDetail)

        memberRegistrationRepository.savePregnancyDetail(pregnancyDetail)
    }

    /**
     * Saves pregnancy outcome details from pregnancy outcome assessment to PregnancyDetail entity.
     * Extracts fields from deliveryOutcomes and abortion sections.
     * Also creates household members for live babies from newborn details.
     */
    suspend fun savePregnancyOutcomeDetails(
        details: AssessmentMemberDetails,
        assessmentMap: HashMap<String, Any>,
    ) {
        val pregnancyOutcomeMap = assessmentMap[PREGNANCY_OUTCOME] as? Map<String, Any?>
            ?: return

        // Check if timeOfDeath has any value and update member status
        val maternalDeath = pregnancyOutcomeMap[AssessmentDefinedParams.MATERNAL_DEATH] as? Map<String, Any?>
        val timeOfDeath = maternalDeath?.get(AssessmentDefinedParams.TIME_OF_DEATH)
        val hasTimeOfDeath = when (timeOfDeath) {
            is String -> timeOfDeath.isNotBlank() && timeOfDeath != DefinedParams.DefaultID
            is Map<*, *> -> {
                val timeOfDeathId = timeOfDeath[DefinedParams.ID]?.toString()
                !timeOfDeathId.isNullOrBlank() && timeOfDeathId != DefinedParams.DefaultID
            }

            else -> false
        }

        // Update member status to inactive if timeOfDeath exists
        if (hasTimeOfDeath) {
            memberRegistrationRepository.updateMemberDeceasedStatus(details.id, false)
        }

        var existingPregnancyDetail = memberRegistrationRepository.getPregnancyDetailByPatientId(details.id)
        // We need to check, date of delivery should be null and type of abortion should be null
        // If any of them is not null, that means we already recorded pregnancy outcome.
        // So, we should create a new record for this pregnancy outcome.
        existingPregnancyDetail = if (existingPregnancyDetail?.dateOfDelivery.isNullOrBlank() && existingPregnancyDetail?.typeOfAbortion.isNullOrBlank()) {
            existingPregnancyDetail
        } else {
            null
        }

        val pregnancyDetail = existingPregnancyDetail ?: PregnancyDetail(
            householdMemberLocalId = details.id,
            patientId = details.patientId,
            householdMemberId = details.memberId,
        )

        // Extract fields from deliveryOutcomes section
        val deliveryOutcomes = pregnancyOutcomeMap[AssessmentDefinedParams.ID_DELIVERY_OUTCOMES] as? Map<String, Any?>
        deliveryOutcomes?.let { delivery ->
            pregnancyDetail.dateOfDelivery = delivery["dateOfDelivery"] as? String
            pregnancyDetail.complicationsDuringDelivery = delivery["complicationsDuringDelivery"] as? String

            // liveBirthNumbers → noOfNeonates (Int)
            val liveBirthNumbers = delivery[AssessmentDefinedParams.ID_LIVE_BIRTH_NUMBERS]
            pregnancyDetail.noOfNeonates = when (liveBirthNumbers) {
                is Number -> liveBirthNumbers.toInt()
                is String -> liveBirthNumbers.toIntOrNull()
                else -> null
            }

            // placeOfDelivery → isDeliveryAtHome (Boolean)
            val placeOfDelivery = delivery["placeOfDelivery"] as? String
            pregnancyDetail.isDeliveryAtHome = placeOfDelivery?.equals("Home", ignoreCase = true)
        }

        // Extract typeOfAbortion from abortion section
        val abortionMap = pregnancyOutcomeMap[AssessmentDefinedParams.ID_ABORTION] as? Map<String, Any?>
        abortionMap?.let { abortion ->
            pregnancyDetail.typeOfAbortion = abortion["typeOfAbortion"] as? String
        }

        // Create household members for live babies
        val dateOfDelivery = deliveryOutcomes?.get("dateOfDelivery") as? String
        val newbornDetailsList = AssessmentUtil.findNewbornDetailsFromMap(pregnancyOutcomeMap)
        var firstBabyMemberId: Long? = null
        val motherMember = memberRegistrationRepository.getMemberDetails(details.id)
        val isExternalMother = details.householdLocalId <= 0L

        if (newbornDetailsList != null && !dateOfDelivery.isNullOrBlank()) {
            newbornDetailsList.forEachIndexed { index, babyData ->
                if (babyData is Map<*, *>) {
                    val isBabyAlive = babyData[AssessmentDefinedParams.IS_BABY_ALIVE]?.toString()
                    // Only create members for live babies
                    if (isBabyAlive.equals(DefinedParams.yes, ignoreCase = true)) {
                        val babyNumber = index + 1
                        val babyMap = HashMap<String, Any>()
                        babyMap[MemberRegistration.NAME] = "Baby $babyNumber of ${details.name}"
                        babyMap[MemberRegistration.DATE_OF_BIRTH] = dateOfDelivery
                        babyMap[MemberRegistration.GENDER] = babyData["sex"]?.toString() ?: ""
                        babyMap[MemberRegistration.IS_HOUSEHOLD_HEAD] = false
                        babyMap[MemberRegistration.ID_GUARDIAN] = details.id

                        // For external mothers, carry location from mother member itself.
                        if (isExternalMother) {
                            motherMember.villageId?.let { babyMap[HouseHoldRegistration.VILLAGE_ID] = it }
                            motherMember.shasthyaShebikaId?.let { babyMap[HouseHoldRegistration.SHASTHYA_SHEBIKA_ID] = it }
                            motherMember.subVillageId?.let { babyMap[HouseHoldRegistration.SUB_VILLAGE_ID] = it }
                        }

                        val memberId = memberRegistrationRepository.registerMember(
                            map = babyMap,
                            householdId = if (isExternalMother) null else details.householdLocalId,
                            parentReferenceId = details.id,
                            location = lastLocation,
                        )

                        // Store first baby's member ID
                        if (firstBabyMemberId == null) {
                            firstBabyMemberId = memberId
                        }
                    }
                }
            }
        }

        // Save first baby's member ID to pregnancy detail
        firstBabyMemberId?.let {
            pregnancyDetail.neonateHouseholdMemberLocalId = it
        }

        // Ensure pregnancyEpisodeId and timestamps are set
        ensurePregnancyEpisodeIdAndTimestamps(pregnancyDetail)

        memberRegistrationRepository.savePregnancyDetail(pregnancyDetail)
    }

    /**
     * Saves ANC assessment fields to PregnancyDetail table.
     * Updates the existing record if one exists, otherwise creates a new one.
     */
    suspend fun saveAncPregnancyDetails(
        details: AssessmentMemberDetails,
        assessmentMap: HashMap<String, Any>,
    ) {
        val ancMap = assessmentMap[ANC] as? Map<*, *>
        if (ancMap != null) {
            // Fetch existing record or create new
            val existingPregnancyDetail = memberRegistrationRepository.getPregnancyDetailByPatientId(details.id)
            val pregnancyDetail = existingPregnancyDetail ?: PregnancyDetail(
                householdMemberLocalId = details.id,
                patientId = details.patientId,
                householdMemberId = details.memberId,
            )

            // Increment visit number on the existing record
            pregnancyDetail.ancVisitNo = getVisitNumber(pregnancyDetail.ancVisitNo)

            // Extract and save previousPregnancyComplications (string)
            val medicalExaminationData = ancMap[AssessmentDefinedParams.GROUP_MEDICAL_HISTORY_PHYSICAL_EXAMINATION] as? Map<String, Any>
            val complications = medicalExaminationData?.get(AssessmentDefinedParams.PREVIOUS_PREGNANCY_COMPLICATIONS)
            pregnancyDetail.previousPregnancyComplications = StringConverter.convertListToString(complications)

            // Extract and save pregnantWomanExistingIllness (list of strings -> JSON)
            val existingIllness = medicalExaminationData?.get(AssessmentDefinedParams.PREGNANT_WOMAN_EXISTING_ILLNESS)
            pregnancyDetail.pregnantWomanExistingIllness = StringConverter.convertListToString(existingIllness)

            // Extract and save pregnantWomanOnTreatment (list of strings -> JSON)
            val onTreatment = medicalExaminationData?.get(AssessmentDefinedParams.PREGNANT_WOMAN_ON_TREATMENT)
            pregnancyDetail.pregnantWomanOnTreatment = StringConverter.convertListToString(onTreatment)

            // Extract and save highRiskPregnantWoman (list of strings -> JSON)
            val summary = ancMap[AssessmentDefinedParams.GROUP_SUMMARY] as? Map<String, Any>
            val highRisk = summary?.get(AssessmentDefinedParams.HIGH_RISK_PREGNANT_WOMAN) as? HashMap<*, *>
            val highRiskString = highRisk?.let { StringConverter.convertGivenMapToString(highRisk) }
            pregnancyDetail.highRiskPregnantWoman = highRiskString

            // Extract and save gapsInAnc (list of strings -> JSON)
            val gapsInAnc = summary?.get(AssessmentDefinedParams.GAPS_IN_ANC)
            pregnancyDetail.gapsInAnc = StringConverter.convertListToString(gapsInAnc)

            // Weight during visit
            val weight = CommonUtils.getDouble(medicalExaminationData?.get(Screening.Weight)).takeIf { it > 0 }
            pregnancyDetail.ancWeight = weight

            // Date of visit
            pregnancyDetail.ancVisitDate = ancMap[AssessmentDefinedParams.ANC_VISIT_DATE] as? String

            // Ensure pregnancyEpisodeId and timestamps are set
            ensurePregnancyEpisodeIdAndTimestamps(pregnancyDetail)

            // Save (updates existing record or inserts new one)
            memberRegistrationRepository.savePregnancyDetail(pregnancyDetail)

            // Fetch the saved record to refresh LiveData
            val savedPregnancyDetail = memberRegistrationRepository.getPregnancyDetailByPatientId(details.id)

            // Update the ViewModel's pregnancyDetail property with the saved record
            this.pregnancyDetailLiveData.postValue(savedPregnancyDetail)

            // Refresh memberClinicalLiveData to update the displayed visit number
            getPatientVisitCountByType(ANC, details.id)
        }
    }

    fun saveCallResult(
        serverData: List<FormLayout>?,
        assessmentEntity: AssessmentEntity,
        assessmentMap: HashMap<String, Any>? = null,
        memberId: Long? = null,
    ) {
        viewModelScope.launch(dispatcherIO) {
            assessmentMap?.let {
                val assessmentDetail =
                    getAssessmentDetails(serverData, it as HashMap<Any, Any>)
                assessmentStringLiveData.postValue(assessmentDetail.first)
            }
            memberId?.let {
                val pregnancyDetail = memberRegistrationRepository.getPregnancyDetailByPatientId(it)
                savePatientClinicalInformation(
                    getUpdatedPregnancyDetail(
                        memberId,
                        pregnancyDetail,
                        true,
                    ),
                )
            }
            val saveResult = assessmentRepository.saveCallResult(assessmentEntity)
            callResultSaveLiveData.postValue(
                when (saveResult.state) {
                    ResourceState.LOADING -> {
                        Resource(state = ResourceState.LOADING)
                    }

                    ResourceState.ERROR -> {
                        Resource(state = ResourceState.ERROR)
                    }

                    ResourceState.SUCCESS -> {
                        Resource(state = ResourceState.SUCCESS, data = Pair(serverData, saveResult.data!!))
                    }
                },
            )
        }
    }

    fun getAssessmentDetailsById(assessmentId: Long) {
        viewModelScope.launch(dispatcherIO) {
            getAssessmentDetails.postValue(assessmentRepository.getAssessmentById(assessmentId))
        }
    }

    private fun calculateOtherDetails(
        assessmentMap: HashMap<String, Any>,
        referralStatus: String?,
        menuId: String?,
    ): HashMap<String, Any>? {
        var otherDetails = HashMap<String, Any>()

        SecuredPreference.getOrganizationId()
        if (menuId == ICCM_MENU_ID) {
            otherDetails = otherAssessmentDetails
        }

        if (referralStatus != null && referralStatus == ReferralStatus.Referred.name) {
            // Add referralFacilityType to otherDetails if it does NOT exist in otherAssessmentDetails
            if (!otherAssessmentDetails.containsKey(REFERRAL_FACILITY_TYPE)) {
                otherDetails[REFERRAL_FACILITY_TYPE] =
                    SecuredPreference.getString(SecuredPreference.EnvironmentKey.DEFAULT_SITE_ID.name)
                        ?: "-1"
            }
            // Remove referredSiteId from otherDetails
            otherDetails.remove(AssessmentDefinedParams.ReferredPHUSiteID)
        } else if (referralStatus != null && referralStatus == ReferralStatus.OnTreatment.name) {
            otherDetails[AssessmentDefinedParams.NextFollowupDate] =
                DateUtils.convertDateTimeToDate(
                    DateUtils.getDateAfterDays(
                        referralReason
                            ?.mapNotNull { treatmentDays[it] }
                            ?.minOrNull() ?: 3,
                    ),
                    DateUtils.DATE_ddMMyyyy,
                    DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                    inUTC = true,
                )
            // Add referralFacilityType to otherDetails if it does NOT exist in otherAssessmentDetails
            if (!otherAssessmentDetails.containsKey(REFERRAL_FACILITY_TYPE)) {
                otherDetails[REFERRAL_FACILITY_TYPE] =
                    otherDetails[AssessmentDefinedParams.ReferredPHUSiteID]
                        ?: "-1"
            }
            // Remove referredSiteId from otherDetails
            otherDetails.remove(AssessmentDefinedParams.ReferredPHUSiteID)
        }

        if (menuId == MenuConstants.NCD_MENU_ID && otherDetails.isNotEmpty()) {
            if (referralStatus == ReferralStatus.Referred.name) {
                val ncdAsst = assessmentMap[ncd] as HashMap<String, Any>
                val facilityType = ncdAsst[REFERRAL_FACILITY_TYPE] as String
                otherDetails[REFERRAL_FACILITY_TYPE] = facilityType

                otherDetails[AssessmentDefinedParams.NextFollowupDate] =
                    DateUtils.convertDateTimeToDate(
                        DateUtils.getDateAfterDays(5),
                        DateUtils.DATE_ddMMyyyy,
                        DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                        inUTC = true,
                    )
            }
        }

        if (menuId == MenuConstants.CATARACT_MENU_ID && otherDetails.isNotEmpty()) {
            if (referralStatus == ReferralStatus.Referred.name) {
                val catAsst = assessmentMap[CATARACT_MENU_ID] as HashMap<String, Any>

                val ncdAsst = catAsst[ncd] as HashMap<String, Any>
                val facilityType = ncdAsst[REFERRAL_FACILITY_TYPE] as String
                otherDetails[REFERRAL_FACILITY_TYPE] = facilityType

                otherDetails[AssessmentDefinedParams.NextFollowupDate] =
                    DateUtils.convertDateTimeToDate(
                        DateUtils.getDateAfterDays(5),
                        DateUtils.DATE_ddMMyyyy,
                        DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                        inUTC = true,
                    )
            }
        }

        if (menuId == EYE_CARE_MENU_ID) {
            otherDetails[REFERRED_SITE] = SecuredPreference.getOrganizationFhirId()
        }

        if (menuId == RMNCH.CHILD_MENU.uppercase(Locale.getDefault())) {
            memberDetailsLiveData.value?.data?.dateOfBirth?.let {
                DateUtils.calculateAgeInMonths(it)?.let { pair ->
                    if (pair.first <= RMNCH.childHoodVisitMaxMonth) {
                        RMNCH
                            .calculateNextChildHoodVisitDate(
                                age = pair.first,
                                birthDate = pair.second,
                            )?.let { visitDate ->
                                otherDetails[AssessmentDefinedParams.NextFollowupDate] =
                                    DateUtils.convertDateTimeToDate(
                                        DateUtils.getDateStringFromDate(
                                            visitDate,
                                            DateUtils.DATE_ddMMyyyy,
                                        ),
                                        DateUtils.DATE_ddMMyyyy,
                                        DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                                    )
                            }
                    }
                }
            }
        }
        return if (otherDetails.isEmpty()) null else otherDetails
    }

    private fun getAssessmentDetails(
        serverData: List<FormLayout>?,
        map: HashMap<Any, Any>,
    ): Pair<String, String> {
        val assessmentDetail = StringConverter.convertGivenMapToString(map) ?: ""
        modifyDialogCheckboxValues(map, serverData)

        // Request modification for syncing ICCM to Backend
        if (map.containsKey(ICCM_MENU_ID)) {
            val iccm = map[ICCM_MENU_ID] as HashMap<*, *>
            if (iccm.containsKey(Diarrhoea)) {
                val diarrhoea = iccm[Diarrhoea] as HashMap<Any, Any>

                if (diarrhoea.containsKey(IccmDiarrheaNotifiableCondition)) {
                    val cbsData = hashMapOf<String, Any>()
                    diarrhoea[IccmDiarrheaNotifiableCondition]?.let {
                        cbsData[NotifiableConditions] = it
                    }
                    if (diarrhoea.containsKey(OtherNotifiableConditionsForDiarrhoea)) {
                        cbsData[OtherNotifiableConditions] =
                            diarrhoea[OtherNotifiableConditionsForDiarrhoea] as String
                    }
                    diarrhoea.remove(IccmDiarrheaNotifiableCondition)
                    diarrhoea.remove(OtherNotifiableConditionsForDiarrhoea)
                    diarrhoea[CBS.lowercase()] = cbsData
                }
            }

            if (iccm.containsKey(fever.lowercase())) {
                val fever = iccm[fever.lowercase()] as HashMap<Any, Any>
                if (fever.containsKey(IccmFeverNotifiableCondition)) {
                    val cbsData = hashMapOf<String, Any>()
                    fever[IccmFeverNotifiableCondition]?.let {
                        cbsData[NotifiableConditions] = it
                    }
                    if (fever.containsKey(OtherNotifiableConditionsForFever)) {
                        cbsData[OtherNotifiableConditions] =
                            fever[OtherNotifiableConditionsForFever] as String
                    }
                    fever.remove(IccmFeverNotifiableCondition)
                    fever.remove(OtherNotifiableConditionsForFever)
                    fever[CBS.lowercase()] = cbsData
                }
            }
        }

        // Request modification for syncing Other Symptoms to Backend
        if (map.containsKey(OTHER_SYMPTOMS)) {
            val otherSymptom = map[OTHER_SYMPTOMS] as HashMap<Any, Any>
            if (otherSymptom.containsKey(signsAndSymptoms)) {
                val signsAndSymptom = otherSymptom[signsAndSymptoms] as HashMap<Any, Any>
                signsAndSymptom[otherSymptoms]?.let {
                    signsAndSymptom[symptoms] = it
                }
                signsAndSymptom.remove(otherSymptoms)
            }
            map.remove(OTHER_SYMPTOMS)
            map[otherSymptoms] = otherSymptom
        }

        // Request modification for syncing NCD Symptoms to Backend
        if (map.containsKey(ncd)) {
            val ncdMap = map[ncd] as HashMap<Any, Any>
            if (ncdMap.containsKey(signsAndSymptoms)) {
                val signsAndSymptom = ncdMap[signsAndSymptoms] as HashMap<Any, Any>
                signsAndSymptom[symptomsDTO]?.let {
                    signsAndSymptom[symptoms] = it
                }
                signsAndSymptom.remove(symptomsDTO)
            }

            if (ncdMap.containsKey(BMI_CATEGORY)) {
                ncdMap.remove(BMI_CATEGORY)
            }

            if (ncdMap.containsKey(BP_LOG)) {
                updateBPLogs(ncdMap[BP_LOG] as HashMap<Any, Any>)
            }
        }

        // Request modification for syncing EyeCare Symptoms to Backend
        if (map.containsKey(EYE_CARE_MENU_ID)) {
            val eyeCareMainMap = map[EYE_CARE_MENU_ID] as HashMap<Any, Any>
            if (eyeCareMainMap.containsKey(EYE_CARE)) {
                val eyeCareMap = eyeCareMainMap[EYE_CARE] as HashMap<Any, Any>
                val eyeTestOutCome = eyeCareMap[EYE_TEST_OUTCOME] as String
                eyeCareMap[EYE_TEST_OUTCOMES] = listOf(eyeTestOutCome)

                eyeCareMap.remove(EYE_TEST_OUTCOME)
                map[EYE_CARE] = eyeCareMap
                map.remove(EYE_CARE_MENU_ID)
            }
        }

        // Request modification for syncing TB to Backend
        if (map.containsKey(TB.lowercase())) {
            val result = map[TB.lowercase()] as? HashMap<Any, Any>
            val contactTracing = result?.get(CONTACT_TRACING) as? HashMap<Any, Any>
            if (contactTracing?.size == 0) {
                result.remove(CONTACT_TRACING)
            }
        }

        // Request modification for CBS Register
        if (map.containsKey(CBS.lowercase())) {
            val result = map[CBS.lowercase()] as? HashMap<Any, Any>
            if (result != null && result.containsKey(surveillanceDetails)) {
                val value = result[surveillanceDetails] as? HashMap<*, *>
                value?.takeIf { it.isNotEmpty() }?.let {
                    val conditions = mutableListOf<String>()
                    (value[CbsNotifiableCondition] as? List<String>)?.let { condition ->
                        conditions.addAll(condition)
                    }
                    (value[RmnchNotifiableCondition] as? List<String>)?.let { condition ->
                        conditions.addAll(condition)
                    }

                    val cbs = it.toMutableMap()
                    if (conditions.contains(DEATH_OF_MOTHER)) {
                        cbs[DEATH_OF_MOTHER] = true
                    }

                    cbs.remove(CbsNotifiableCondition)
                    cbs.remove(RmnchNotifiableCondition)
                    cbs[NotifiableConditions] = conditions

                    map[CBS.lowercase()] = cbs
                }
            }
        }

        // Request modification for Family Planning
        if (map.containsKey(MenuConstants.FP_MENU_ID.lowercase())) {
            val familyPlanning = (map[MenuConstants.FP_MENU_ID] as? Map<String, Any>)
            if (familyPlanning != null && familyPlanning.containsKey(FamilyPlanningDetails)) {
                val result = familyPlanning[FamilyPlanningDetails] as? HashMap<String, Any>
                result?.let {
                    map.remove(MenuConstants.FP_MENU_ID)
                    map[FamilyPlanning] = it
                }
            }
        }

        if (map.containsKey(CATARACT_MENU_ID)) {
            val cataract = map[CATARACT_MENU_ID] as HashMap<Any, Any>

            cataract.remove(HISTORY_OF_OTHER_DISEASES_)
            cataract.remove(OPERATION_NAME_)
            cataract.remove(EYE_DISEASE_)
            cataract.remove(REASON_)

            if (cataract.containsKey(CATARACT_MENU_ID)) {
                val subCataract = cataract[CATARACT_MENU_ID] as HashMap<Any, Any>
                val eyeDiseases = subCataract[EYE_DISEASE] as Any
                subCataract[EYE_TEST_OUTCOMES] = eyeDiseases

                subCataract.remove(EYE_DISEASE)
            }

            val bioMetrics = cataract[BIO_METRICS] as? HashMap<*, *>
            val bmiCategory = cataract[BMI_CATEGORY] as? String
            val glucoseLog = cataract[GLUCOSE_LOG] as? HashMap<Any, Any>
            val bpLog = cataract[BP_LOG] as? HashMap<Any, Any>
            val referralFacilityType = cataract[REFERRAL_FACILITY_TYPE] as? String

            val cvdScore = cataract[DefinedParams.CVD_RISK_SCORE] as? Int
            val cvdLevel = cataract[DefinedParams.CVD_RISK_LEVEL] as? String
            val cvdScoreDisplay = cataract[DefinedParams.CVD_RISK_SCORE_DISPLAY] as? String

            if (bioMetrics != null ||
                bmiCategory != null ||
                glucoseLog != null ||
                bpLog != null ||
                referralFacilityType != null ||
                cvdScore != null ||
                cvdLevel != null ||
                cvdScoreDisplay != null
            ) {
                val ncdMap = mutableMapOf<String, Any>()

                bioMetrics?.let {
                    ncdMap[BIO_METRICS] = it
                }

                bmiCategory?.let {
                    ncdMap[BMI_CATEGORY] = it
                }

                glucoseLog?.let { bg ->
                    (bg[IS_BEFORE_DIABETES_DIAGNOSIS] as? String)?.let {
                        bg[IS_BEFORE_DIABETES_DIAGNOSIS] = getFlagTypeValue(it)
                    }

                    ncdMap[GLUCOSE_LOG] = bg
                }

                bpLog?.let { bp ->
                    updateBPLogs(bp)

                    (bp[IS_BEFORE_HTN_DIAGNOSIS] as? String)?.let {
                        bp[IS_BEFORE_HTN_DIAGNOSIS] = getFlagTypeValue(it)
                    }

                    ncdMap[BP_LOG] = bp
                }

                referralFacilityType?.let {
                    ncdMap[REFERRAL_FACILITY_TYPE] = it
                }

                cvdScore?.let {
                    ncdMap[DefinedParams.CVD_RISK_SCORE] = it
                }

                cvdLevel?.let {
                    ncdMap[DefinedParams.CVD_RISK_LEVEL] = it
                }

                cvdScoreDisplay?.let {
                    ncdMap[DefinedParams.CVD_RISK_SCORE_DISPLAY] = it
                }

                cataract[NCD_MENU_ID] = ncdMap
            }
            cataract.remove(BIO_METRICS)
            cataract.remove(BMI_CATEGORY)
            cataract.remove(GLUCOSE_LOG)
            cataract.remove(BP_LOG)
            cataract.remove(REFERRAL_FACILITY_TYPE)
            cataract.remove(DefinedParams.CVD_RISK_SCORE)
            cataract.remove(DefinedParams.CVD_RISK_LEVEL)
            cataract.remove(DefinedParams.CVD_RISK_SCORE_DISPLAY)
        }

        val assessmentDetailBE = StringConverter.convertGivenMapToString(map) ?: ""
        return Pair(assessmentDetail, assessmentDetailBE)
    }

    private fun getFlagTypeValue(value: Any): Boolean =
        when (value) {
            is String -> value == DefinedParams.Yes
            is Boolean -> value
            else -> false
        }

    fun getFormatedNotifiableCondition(
        map: HashMap<*, *>,
        key: String,
    ): List<String> {
        val conditions = mutableListOf<String>()
        val list = map[key] as List<*>
        list.forEach { condition ->
            if (condition is HashMap<*, *>) {
                conditions.add(condition[DefinedParams.Value] as String)
            }
        }
        return conditions
    }

    fun updateOtherAssessmentDetails() {
        viewModelScope.launch(dispatcherIO) {
            if (otherAssessmentDetails.containsKey(IsClinicTaken)) {
                val isTakenToClinical = otherAssessmentDetails[IsClinicTaken] as String
                otherAssessmentDetails[IsClinicTaken] = (isTakenToClinical == "Yes")
            }
            if (!otherAssessmentDetails.containsKey(REFERRAL_FACILITY_TYPE)) {
                otherAssessmentDetails[REFERRAL_FACILITY_TYPE] =
                    otherAssessmentDetails[AssessmentDefinedParams.ReferredPHUSiteID]
                        ?: "-1"
                otherAssessmentDetails.remove(AssessmentDefinedParams.ReferredPHUSiteID)
            }

            assessmentUpdateLiveData.postValue(
                assessmentRepository.updateOtherAssessmentDetails(
                    assessmentSaveLiveData.value?.data?.second,
                    otherAssessmentDetails,
                    lastLocation,
                ),
            )
            (otherAssessmentDetails[AssessmentDefinedParams.NextFollowupDate] as? String)?.let {
                assessmentRepository.updateAssessmentHistory(
                    assessmentHistoryResultLiveData.value?.data,
                    it,
                )
            }
        }
    }

    private fun updateBPLogs(bpLogMap: HashMap<Any, Any>) {
        val filtered = (bpLogMap[BP_LOG_DETAILS] as? List<*>)
            ?.filterIsInstance<BPModel>()
            ?.filter { it.systolic != null && it.diastolic != null }
            ?: return

        bpLogMap[BP_LOG_DETAILS] = filtered
    }

    fun updateFamilyPlanningAssessmentDetails() {
        // Nothing to update, just update the state to success
        assessmentUpdateLiveData.value = Resource(state = ResourceState.SUCCESS)
    }

    fun updatePregnantWomanAssessmentDetails() {
        // Nothing to update, just update the state to success
        assessmentUpdateLiveData.value = Resource(state = ResourceState.SUCCESS)
    }

    fun getSymptomListByType(
        type: String,
        inputData: List<SignsAndSymptomsEntity>? = null,
    ) {
        viewModelScope.launch(dispatcherIO) {
            if (!inputData.isNullOrEmpty()) {
                symptomTypeListResponse.postValue(inputData)
            } else {
                symptomTypeListResponse.postValue(assessmentRepository.getSymptomListByType(type))
            }
        }
    }

    fun getFormData(
        formType: String,
        tbType: String? = null,
    ) {
        viewModelScope.launch(dispatcherIO) {
            formLayoutsLiveData.postLoading()
            val formData = assessmentRepository.getFormData(formType, tbType)
            applyBdCampFieldVisibility(formData, formType)
            formLayoutsLiveData.postValue(formData)
        }
    }

    fun getFormData(
        formType: String,
        isContactTracking: Boolean?,
        isTbPatient: Boolean?,
        isRxBuddy: Boolean,
    ) {
        viewModelScope.launch(dispatcherIO) {
            formLayoutsLiveData.postLoading()
            val formData = assessmentRepository.getFormData(formType)
            if (isContactTracking == true) {
                updateFieldViewStatus(formData)
            } else if (isTbPatient == true) {
                updateRxBuddyFieldViewStatus(formData, isRxBuddy)
            }
            applyBdCampFieldVisibility(formData, formType)

            formLayoutsLiveData.postValue(formData)
        }
    }

    /**
     * Bangladesh FO/PO flows: show camp date (cataract) or camp type (eye care / NCD for PO only)
     * from assets JSON where those rows default to [gone].
     */
    private fun applyBdCampFieldVisibility(
        formResponse: Resource<FormResponse>,
        formType: String,
    ) {
        if (formResponse.state != ResourceState.SUCCESS) return
        val layout = formResponse.data?.formLayout ?: return
        when (formType) {
            CATARACT_MENU_ID -> {
                if (!CommonUtils.isFoOrPo()) return
                revealBdCampFields(layout, showCampDate = true, showCampType = false)
            }
            EYE_CARE_MENU_ID -> {
                if (!CommonUtils.isFoOrPo()) return
                revealBdCampFields(layout, showCampDate = false, showCampType = true)
            }
            NCD_MENU_ID -> {
                if (!CommonUtils.isPo()) return
                revealBdCampFields(layout, showCampDate = false, showCampType = true)
            }
            else -> return
        }
    }

    private fun revealBdCampFields(
        layout: List<FormLayout>,
        showCampDate: Boolean,
        showCampType: Boolean,
    ) {
        val showGeneralCard = showCampDate || showCampType
        layout.forEach { field ->
            when (field.id) {
                AssessmentDefinedParams.GENERAL_INFORMATION -> {
                    if (showGeneralCard) {
                        field.visibility = "visible"
                    }
                }
                AssessmentDefinedParams.CAMP_DATE -> {
                    if (showCampDate) {
                        field.visibility = "visible"
                    }
                }
                AssessmentDefinedParams.CAMP_TYPE -> {
                    if (showCampType) {
                        field.visibility = "visible"
                    }
                }
            }
        }
    }

    private fun updateFieldViewStatus(formResponse: Resource<FormResponse>) {
        formResponse.data?.formLayout?.forEach { field ->
            if (field.id == CONTACT_TRACING || field.family == CONTACT_TRACING) {
                field.visibility = "visible"
                field.isMandatory = true
            } else {
                field.isMandatory = false
            }
        }
    }

    private fun updateRxBuddyFieldViewStatus(
        formResponse: Resource<FormResponse>,
        isRxBuddy: Boolean,
    ) {
        formResponse.data?.formLayout?.forEach { field ->
            when (field.id) {
                tbScreening -> {
                    field.visibility = "gone"
                    field.isMandatory = false
                }

                hasCough -> {
                    field.visibility = "gone"
                    field.isMandatory = false
                }

                rxBuddyName, rxBuddyPhoneNumber -> {
                    field.visibility = "gone"
                }

                rxBuddy, selectHouseholdMember,
                relationshipToPatient,
                hasProvidedMonitoringSheet,
                -> {
                    field.visibility = if (isRxBuddy) "gone" else "visible"
                }
            }
        }
    }

    fun getNearestHealthFacility() {
        viewModelScope.launch(dispatcherIO) {
            nearestFacilityLiveData.postValue(assessmentRepository.getNearestHealthFacility())
        }
    }

    fun setCurrentLocation(location: Location) {
        this.lastLocation = location
    }

    fun getCurrentLocation(): Location? = this.lastLocation

    fun getPatientVisitCountByType(
        type: String,
        hhmLocalId: Long,
    ) {
        viewModelScope.launch(dispatcherIO) {
            memberClinicalLiveData.postValue(
                memberRegistrationRepository.getPatientVisitCountByType(
                    type,
                    hhmLocalId,
                ),
            )
        }
    }

    /**
     * Gets triggered for updating pregnancy details for PNC and ChildVisit
     */
    fun handlePregnancy(
        details: HashMap<String, Any>,
        workflowName: String,
        memberDetail: AssessmentMemberDetails,
    ) {
        memberDetail.apply {
            if (details.containsKey(workflowName) && details[workflowName] is Map<*, *>) {
                val map = details[workflowName] as HashMap<String, Any>
                val pregnancyDetail = pregnancyDetailLiveData.value
                    ?: PregnancyDetail(householdMemberLocalId = id)
                getClinicalDateAndVisitCount(
                    map,
                    workflowName,
                    pregnancyDetail,
                    memberDetail,
                )
                savePatientClinicalInformation(pregnancyDetail)
            }
        }
    }

    /**
     * Ensures pregnancyEpisodeId exists and manages startAt/endAt timestamps
     * @param pregnancyDetail The pregnancy detail entity to update
     */
    private fun ensurePregnancyEpisodeIdAndTimestamps(pregnancyDetail: PregnancyDetail) {
        // Generate pregnancyEpisodeId if not exists
        if (pregnancyDetail.pregnancyEpisodeId.isNullOrBlank()) {
            pregnancyDetail.pregnancyEpisodeId = UUID.randomUUID().toString()
        }

        // Set startAt when creating new pregnancyEpisodeId (first time)
        if (pregnancyDetail.startAt.isNullOrBlank() && !pregnancyDetail.pregnancyEpisodeId.isNullOrBlank()) {
            pregnancyDetail.startAt = System.currentTimeMillis().convertToUtcDateTime()
        }

        // Always update endAt on every save
        pregnancyDetail.endAt = System.currentTimeMillis().convertToUtcDateTime()
    }

    fun savePatientClinicalInformation(pregnancyDetail: PregnancyDetail) {
        viewModelScope.launch(dispatcherIO) {
            // Get existing record to preserve pregnancyEpisodeId and startAt
            val existingRecord = memberRegistrationRepository.getPregnancyDetailByPatientId(pregnancyDetail.householdMemberLocalId)

            // If existing record found, preserve pregnancyEpisodeId and startAt
            if (existingRecord != null) {
                pregnancyDetail.pregnancyEpisodeId = existingRecord.pregnancyEpisodeId ?: pregnancyDetail.pregnancyEpisodeId
                pregnancyDetail.startAt = existingRecord.startAt ?: pregnancyDetail.startAt
                pregnancyDetail.id = existingRecord.id // Preserve ID for update
            }

            // Ensure pregnancyEpisodeId and timestamps are set
            ensurePregnancyEpisodeIdAndTimestamps(pregnancyDetail)

            memberRegistrationRepository.savePregnancyDetail(pregnancyDetail)
        }
    }

    fun getPregnancyDetailInformation() {
        viewModelScope.launch(dispatcherIO) {
            memberDetailsLiveData.value?.data?.let { detail ->
                pregnancyDetailLiveData.postValue(
                    memberRegistrationRepository.getPregnancyDetailByPatientId(detail.id),
                )
            }
        }
    }

    /**
     * Updates details of pregnancy detail from the map
     */
    private fun getClinicalDateAndVisitCount(
        details: HashMap<String, Any>,
        workflowName: String,
        pregnancyDetail: PregnancyDetail,
        memberDetail: AssessmentMemberDetails,
    ) {
        when (workflowName) {
            RMNCH.PNC -> {
                val visitNo = getVisitNumber(pregnancyDetail.pncVisitNo)
                pregnancyDetail.pncVisitNo = visitNo
                details[RMNCH.visitNo] = pregnancyDetail.pncVisitNo ?: 0L
                val pregnancyHistory = details[RMNCH.ID_PREGNANCY_HISTORY] as? HashMap<String, Any>
                if (pregnancyHistory != null) {
                    if (pregnancyHistory.containsKey(PregnantWomen.ID_PARITY)) {
                        pregnancyDetail.parity = CommonUtils.getDouble(pregnancyHistory[PregnantWomen.ID_PARITY]).toInt()
                    }
                    if (pregnancyHistory.containsKey(PregnantWomen.ID_LIVING_CHILDREN)) {
                        pregnancyDetail.numberOfLivingChildren = CommonUtils.getDouble(pregnancyHistory[PregnantWomen.ID_LIVING_CHILDREN]).toInt()
                    }
                }
                (details[RMNCH.ID_PNC_GAPS] as? List<*>)?.let {
                    pregnancyDetail.gapsInPnc = StringConverter.convertListToString(it)
                }
                (details[RMNCH.ID_MOTHER_RISKS] as? HashMap<*, *>)?.let {
                    pregnancyDetail.highRiskMother = StringConverter.convertGivenMapToString(it)
                }
                (details[RMNCH.ID_PNC_ILLNESS] as? HashMap<*, *>)?.let {
                    pregnancyDetail.pncIllness = StringConverter.convertGivenMapToString(it)
                }
                updatePregnantStatus(memberDetail.id, false)
            }

            else -> {
                pregnancyDetail.childVisitNo =
                    getVisitNumber(pregnancyDetail.childVisitNo)
                (details[AssessmentDefinedParams.ID_CONGENITAL_DEFECT] as? String)?.let { congenitalDefect ->
                    pregnancyDetail.childCongenitalDefect = congenitalDefect
                }
                details[RMNCH.visitNo] = pregnancyDetail.childVisitNo ?: 0L
            }
        }
    }

    private fun getVisitNumber(
        existingCount: Long?,
        visitNo: Long = 1,
    ): Long {
        existingCount?.let { return (it + 1) } ?: return visitNo.let { it }
    }

    fun updateMemberDeceasedStatus(
        id: Long,
        status: Boolean,
        deceasedReason: String? = null,
    ) {
        viewModelScope.launch(dispatcherIO) {
            memberRegistrationRepository.updateMemberDeceasedReason(
                id,
                status,
                deceasedReason,
            )
        }
    }

    fun getSymptomList() {
        viewModelScope.launch(dispatcherIO) {
            symptomListResponse.postValue(assessmentRepository.getSymptomList())
        }
    }

    fun getMedicationParentComplianceList() {
        viewModelScope.launch(dispatcherIO) {
            medicationParentComplianceResponse.postValue(assessmentRepository.getMedicationParentComplianceList())
        }
    }

    fun getMedicationChildComplianceList(parentId: Long) {
        viewModelScope.launch(dispatcherIO) {
            try {
                medicationChildComplianceResponse.postValue(
                    assessmentRepository.getMedicationChildComplianceList(
                        parentId,
                    ),
                )
            } catch (_: Exception) {
                // Exception - Catch block
            }
        }
    }

    fun saveAssessmentInformation(
        request: String,
        uploadStatus: Boolean,
        isRecursion: Boolean,
        onlineSaveResponse: HashMap<String, Any>? = null,
    ) {
        viewModelScope.launch(dispatcherIO) {
            assessmentSaveResponse.postLoading()
            try {
                if (!isRecursion && connectivityManager.isNetworkAvailable()) {
                    val reqMap = StringConverter.convertStringToMap(request)
                    val response = assessmentRepository.createAssessmentNCD(
                        StringConverter.getJsonObject(
                            Gson().toJson(reqMap),
                        ),
                    )
                    val success = response.isSuccessful
                    saveAssessmentInformation(
                        request,
                        uploadStatus = success,
                        isRecursion = true,
                        onlineSaveResponse = if (success) response.body() else null,
                    )
                } else {
                    val assessmentEntity = AssessmentNCDEntity(
                        assessmentDetails = request,
                        uploadStatus = uploadStatus,
                        userId = SecuredPreference.getUserId(),
                    )
                    val rowId = assessmentRepository.saveAssessmentInformation(assessmentEntity)
                    setAnalyticsData(
                        UserDetail.startDateTime,
                        eventName = AnalyticsDefinedParams.NCDAssessmentCreation + " " + menuId,
                        isCompleted = true,
                    )
                    assessmentSaveResponse.postSuccess(Pair(rowId, onlineSaveResponse))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                assessmentSaveResponse.postError()
            }
        }
    }

    fun setFbsBloodGlucose(glucose: Double) {
        fbsBloodGlucose = glucose
    }

    fun setRbsBloodGlucose(glucose: Double) {
        rbsBloodGlucose = glucose
    }

    fun getFbsBloodGlucose(): Double = fbsBloodGlucose ?: 0.0

    fun getRbsBloodGlucose(): Double = rbsBloodGlucose ?: 0.0

    fun fetchMentalHealthQuestions(type: String) {
        viewModelScope.launch(dispatcherIO) {
            var mhResponse = mentalHealthQuestions.value?.data
            mentalHealthQuestions.postLoading()
            try {
                val phq4Questions = assessmentRepository.getMentalQuestion(type = Screening.PHQ4)
                val phq9Questions =
                    assessmentRepository.getMentalQuestion(type = AssessmentDefinedParams.PHQ9)
                val gad7Questions =
                    assessmentRepository.getMentalQuestion(type = AssessmentDefinedParams.GAD7)
                if (mhResponse == null) {
                    mhResponse = HashMap()
                }
                mhResponse[Screening.PHQ4] =
                    LocalSpinnerResponse(
                        tag = Screening.MentalHealthDetails,
                        response = phq4Questions,
                    )
                mhResponse[AssessmentDefinedParams.PHQ9] =
                    LocalSpinnerResponse(
                        tag = AssessmentDefinedParams.PHQ9_Mental_Health,
                        response = phq9Questions,
                    )
                mhResponse[AssessmentDefinedParams.GAD7] =
                    LocalSpinnerResponse(
                        tag = AssessmentDefinedParams.GAD7_Mental_Health,
                        response = gad7Questions,
                    )

                mentalHealthQuestions.postValue(Resource(ResourceState.SUCCESS, mhResponse))
            } catch (_: Exception) {
                mentalHealthQuestions.postValue(Resource(ResourceState.ERROR))
            }
        }
    }

    fun setPhQ4Score(phQ4Score: Int) {
        this.phQ4Score = phQ4Score
    }

    fun getPhQ4Score(): Int? = phQ4Score

    fun savePNCDetails(
        serverData: List<FormLayout>?,
        assessmentMap: HashMap<String, Any>,
        memberDetail: AssessmentMemberDetails,
        followUpId: Long? = null,
    ) {
        viewModelScope.launch(dispatcherIO) {
            // Update Mother member details to NotSynced for PNC Flow
            memberRegistrationRepository.changeMemberDetailsToNotSynced(memberDetail.id)

            val motherReferralResult =
                ReferralResultGenerator().calculateRMNCHReferralResult(assessmentMap)
            val status = AssessmentStatusGenerator.evaluateStatus(assessmentMap, memberDetail)
            val assessmentDetail =
                getAssessmentDetails(serverData, assessmentMap as HashMap<Any, Any>)
            referralStatus = motherReferralResult.first
            referralReason = motherReferralResult.second
            assessmentStringLiveData.postValue(assessmentDetail.first)
            val assessmentResult = assessmentRepository.saveAssessment(
                assessmentDetail.second,
                memberDetail,
                RMNCH.PNC_MOTHER_MENU,
                motherReferralResult,
                null,
                followUpId,
                status = status,
            )
            assessmentResult.data?.let {
                val history = MemberAssessmentHistoryEntity(
                    memberFhirId = memberDetail.memberId,
                    memberId = memberDetail.id,
                    visitDate = DateUtils.formatDate(
                        it.createdAt,
                    ),
                    serviceProvided = RMNCH.PNC_MOTHER_MENU.uppercase(Locale.ENGLISH),
                    customStatus = status,
                    latestVisit = true,
                    referralStatus = referralStatus,
                    referralReason = referralReason.toString(),
                )
                assessmentHistoryResultLiveData.postValue(assessmentRepository.saveAssessmentHistory(history))
            }
            assessmentSaveLiveData.postValue(
                when (assessmentResult.state) {
                    ResourceState.ERROR -> Resource(state = ResourceState.ERROR)
                    ResourceState.LOADING -> Resource(state = ResourceState.LOADING)
                    ResourceState.SUCCESS -> Resource(state = assessmentResult.state, data = Pair(serverData, assessmentResult.data!!))
                },
            )
        }
    }

    private fun getReferralStatus(
        status: String?,
        assessmentMap: HashMap<String, Any>,
    ): String? {
        if (status != ReferralStatus.Referred.name) return status

        var type: String? = null
        if (assessmentMap.containsKey(NCD_MENU_ID)) {
            type = (assessmentMap[NCD_MENU_ID] as? Map<*, *>)
                ?.get(REFERRAL_FACILITY_TYPE) as? String
        } else if (assessmentMap.containsKey(CATARACT_MENU_ID)) {
            val catMap = assessmentMap[CATARACT_MENU_ID] as HashMap<Any, Any>
            type = (catMap[NCD_MENU_ID] as? Map<*, *>)
                ?.get(REFERRAL_FACILITY_TYPE) as? String
        }

        return when (type) {
            FACILITY_TYPE_UPAZILA -> STATUS_FACILITY_TYPE_UPAZILA
            FACILITY_TYPE_COMMUNITY_CLINIC -> STATUS_FACILITY_TYPE_COMMUNITY_CLINIC
            else -> status
        }
    }

    fun fetchCurrentLocation(context: Context) {
        val locationManager = SpiceLocationManager(context)
        locationManager.getCurrentLocation {
            setCurrentLocation(it)
        }
    }

    fun getUserProfile() {
        viewModelScope.launch(dispatcherIO) {
            userProfileLiveData.postLoading()
            userProfileLiveData.postValue(metaRepository.getUserProfile())
        }
    }

    fun getHealthFacilityBasedOnVillageId(villageId: Long) {
        viewModelScope.launch(dispatcherIO) {
            patientHealthFacility.postValue(
                assessmentRepository.getHealthFacilityBasedOnVillageId(
                    villageId = villageId,
                ),
            )
        }
    }

    val triggerGetForm = MutableLiveData<Boolean>()

    fun triggerGetForm() {
        triggerGetForm.value = true
    }

    val formLayoutsCbsLiveData = MutableLiveData<Resource<FormResponse>>()

    fun getFormDataCbs(formType: String) {
        viewModelScope.launch(dispatcherIO) {
            formLayoutsCbsLiveData.postLoading()
            formLayoutsCbsLiveData.postValue(assessmentRepository.getFormData(formType))
        }
    }

    var assessmentMap: HashMap<String, Any> = hashMapOf()
    var referralResult: Pair<String?, ArrayList<String>>? = Pair(null, ArrayList())
    val birthLiveData = MutableLiveData<Resource<Triple<String, Boolean, Boolean>>>()
    var cbsMemberIDAndPregnancyDetail: Pair<Long?, PregnancyDetail?> = Pair(null, null)

    fun setBirth(
        resultValue: HashMap<String, Any>,
        referralResult: Pair<String?, ArrayList<String>>,
        birth: String,
        isDelete: Boolean,
        memberId: Long? = null,
    ) {
        this.assessmentMap = resultValue
        this.referralResult = referralResult
        memberId?.let {
            cbsMemberIDAndPregnancyDetail = Pair(memberId, pregnancyDetailLiveData.value)
        }
        birthLiveData.postValue(Resource(ResourceState.SUCCESS, Triple(birth, isDelete, true)))
    }

    fun getUpdatedPregnancyDetail(
        memberId: Long,
        pregnancyDetail: PregnancyDetail?,
        isResetPregnancy: Boolean = false,
    ): PregnancyDetail {
        if (isResetPregnancy) {
            updatePregnantStatus(memberId, false)
        }
        return (pregnancyDetail ?: PregnancyDetail(householdMemberLocalId = memberId)).apply {
            this.pncVisitNo = 0
            this.dateOfDelivery = null
            this.noOfNeonates = null
            this.neonatePatientId = null
            this.isDeliveryAtHome = null
            this.neonateHouseholdMemberLocalId = null
            this.isNeonateDeathRecordedByPHU = null

            this.ancVisitNo = 0
            this.lastMenstrualPeriod = null
            this.estimatedDeliveryDate = null
        }
    }

    val memberCbsDetailsLiveData = MutableLiveData<Resource<Pair<List<FormLayout>?, AssessmentMemberDetails>>>()

    fun saveMember(
        serverData: List<FormLayout>?,
        memberMap: HashMap<String, Any>,
        householdId: Long,
        motherID: Long,
        location: Location?,
    ) {
        viewModelScope.launch(dispatcherIO) {
            val id = memberRegistrationRepository.registerMember(
                memberMap,
                householdId,
                null,
                motherID,
                location = location,
            )
            // Update Mother member details to NotSynced for PNC Flow
            memberRegistrationRepository.changeMemberDetailsToNotSynced(motherID)
            id.let {
                val result = memberRegistrationRepository.getAssessmentMemberDetails(id)
                memberCbsDetailsLiveData.postValue(
                    when (result.state) {
                        ResourceState.ERROR -> Resource(state = ResourceState.ERROR)
                        ResourceState.SUCCESS -> {
                            Resource(state = ResourceState.SUCCESS, data = Pair(serverData, result.data!!))
                        }

                        ResourceState.LOADING -> {
                            Resource(state = ResourceState.LOADING)
                        }
                    },
                )
            }
        }
    }

    var assessment: AssessmentEntity? = null
    var resultValue: HashMap<String, Any> = hashMapOf()

    fun saveAssessmentCbs(
        data: AssessmentEntity,
        resultValue: HashMap<String, Any>,
        birth: String,
        memberId: Long? = null,
    ) {
        this.assessment = data
        this.resultValue = resultValue
        memberId?.let {
            savePatientClinicalInformation(
                getUpdatedPregnancyDetail(
                    memberId,
                    pregnancyDetailLiveData.value,
                    true,
                ),
            )
        }
        birthLiveData.postValue(Resource(ResourceState.SUCCESS, Triple(birth, false, false)))
    }

    fun updateTBContactTraceStatus(
        hhmId: Long,
        tbContactTracingStatus: Int,
    ) {
        viewModelScope.launch(dispatcherIO) {
            houseHoldRepository.updateHouseholdMemberTbContactTraceStatus(
                hhmId,
                tbContactTracingStatus,
            )
        }
    }

    fun renderBMIValue(
        context: Context,
        formGenerator: FormGenerator,
        resultHashMap: HashMap<String, Any>,
    ) {
        val bmiView = formGenerator.getViewByTag(Screening.BMI) as? AppCompatTextView
        bmiView?.let { view ->
            if (!resultHashMap.containsKey(Screening.Weight) || !resultHashMap.containsKey(Screening.Height)) {
                view.text = context.getString(R.string.hyphen_symbol)
                formGenerator.removeIfContains(Screening.BMI)
            } else {
                if (resultHashMap.containsKey(Screening.Weight) &&
                    resultHashMap.containsKey(
                        Screening.Height,
                    )
                ) {
                    val weight = resultHashMap[Screening.Weight] as? Double
                    val height = resultHashMap[Screening.Height] as? Double

                    if (weight == null || height == null) {
                        view.text = context.getString(R.string.hyphen_symbol)
                    } else {
                        val bmi = CommonUtils.getBMIForNcd(height, weight)
                        CommonUtils
                            .getBMIInformation(context, bmi?.toDoubleOrNull())
                            ?.let { info ->
                                bmi?.toDoubleOrNull()?.let {
                                    resultHashMap[Screening.BMI] = it
                                }
                                resultHashMap[Screening.BMI_CATEGORY] = info.first

                                val bmiWithInfoSpannableStringBuilder = if (bmi == null) {
                                    context.getString(R.string.hyphen_symbol)
                                } else {
                                    SpannableStringBuilder()
                                        .append(bmi)
                                        .color(context.getColor(info.second)) {
                                            append(" (${info.first})")
                                        }
                                }
                                view.text = bmiWithInfoSpannableStringBuilder
                            }
                    }
                }
            }
        }
    }

    fun getPregnancyDetailInformationForMother() {
        viewModelScope.launch(dispatcherIO) {
            motherID?.let { id ->
                pregnancyDetailForMother =
                    memberRegistrationRepository.getPregnancyDetailByPatientId(id)
            }
        }
    }

    fun updatePregnantStatus(
        memberId: Long,
        isPregnant: Boolean,
    ) {
        viewModelScope.launch(dispatcherIO) {
            memberRegistrationRepository.updatePregnantStatus(memberId, isPregnant)
        }
    }

    fun getSymptomListByTypes(types: List<String>) {
        viewModelScope.launch(dispatcherIO) {
            symptomTypeListResponse.postValue(assessmentRepository.getSymptomListByTypes(types))
        }
    }

    fun getOtherHouseholdMemberExcludeTBPatient() {
        val memberId = memberDetailsLiveData.value?.data?.id
        val householdId = memberDetailsLiveData.value?.data?.householdLocalId
        viewModelScope.launch(dispatcherIO) {
            if (memberId != null && householdId != null) {
                otherHouseholdMemberLiveData.postValue(
                    rxBuddyRepository.getOtherHouseholdMembersExcludeTBPatient(
                        householdId,
                        memberId,
                    ),
                )
            }
        }
    }

    fun getTbType(
        memberId: Long,
        isContactTracking: Boolean?,
    ) {
        viewModelScope.launch(dispatcherIO) {
            formLayoutsLiveData.postLoading()
            memberDetailsLiveData.postLoading()
            val assessmentMemberDetails =
                memberRegistrationRepository.getAssessmentMemberDetails(memberId)
            memberDetailsLiveData.postValue(assessmentMemberDetails)

            assessmentMemberDetails.data?.memberId?.let { memberId ->
                // 1. Get Treatment details
                treatmentDetailsRepository.getTreatmentDetails(memberId)?.let { treatmentDetail ->
                    // 1.1. Treatment details not null. Proceed with Rx Buddy
                    treatmentDetailsLiveData.postValue(treatmentDetail)

                    // 2. Get RX Buddy Details
                    rxBuddyRepository.getRxBuddyDetails(memberId)?.let { rxBuddy ->
                        // 2.1.Rx Buddy Details not null. Proceed with Rx Buddy Followup
                        if (rxBuddy.householdMemberId != null) {
                            val member =
                                memberRegistrationRepository.getMemberDetails(rxBuddy.householdMemberId!!)
                            rxBuddy.name = member.name
                            rxBuddy.phoneNumber = member.phoneNumber
                        }
                        rxBuddyDetailsLiveData.postValue(rxBuddy)
                        assessmentTBType.postValue(TBRxBuddyFollowUp)
                    } ?: run {
                        // 2.2.Rx Buddy Details null. Proceed with Rx Buddy Register
                        assessmentTBType.postValue(TBRxBuddyRegister)
                    }
                } ?: run {
                    // 1.2. Treatment details null. Proceed with TB Screening or Contact Tracing
                    if (isContactTracking == true || assessmentMemberDetails.data.contactTracingStatus == 0) {
                        assessmentTBType.postValue(TBContactTracing)
                    } else {
                        assessmentTBType.postValue(TBScreening)
                    }
                }
            } ?: run {
                // If Fhir id not available for member proceed with TB Screening or Contact Tracing
                if (isContactTracking == true || assessmentMemberDetails.data?.contactTracingStatus == 0) {
                    assessmentTBType.postValue(TBContactTracing)
                } else {
                    assessmentTBType.postValue(TBScreening)
                }
            }
        }
    }

    private fun insertRxBuddyFollowUp(map: HashMap<String, Any>) {
        viewModelScope.launch {
            saveRxBuddyFollowUpLiveData.postLoading()
            val tb = map[TB_MENU_ID.lowercase()] as HashMap<String, Any>
            if (tb.containsKey(TBRxBuddyFollowUp)) {
                val rxBuddyFollowUp = tb[TBRxBuddyFollowUp] as HashMap<String, Any>
                val rxBuddyLocalId = rxBuddyDetailsLiveData.value?.id ?: 0
                val rxBuddyId = rxBuddyDetailsLiveData.value?.rxBuddyId

                if (rxBuddyFollowUp.containsKey(rxBuddyMonitoringDates)) {
                    val dates = rxBuddyFollowUp[rxBuddyMonitoringDates] as List<Long>
                    val updatedDates = dates.map { date ->
                        DateUtils.getDateString(
                            date,
                            inputFormat = DateUtils.DATE_FORMAT_yyyyMMdd,
                            outputFormat = DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                        )
                    }

                    rxBuddyFollowUp[rxBuddyMonitoringDates] = updatedDates
                }

                rxBuddyId?.let {
                    rxBuddyFollowUp[RxBuddyId] = it
                }

                val patientMemberId = memberDetailsLiveData.value?.data?.memberId!!
                val nextVisitDate = getNextVisitForConfirmedTbPatient()
                nextVisitDateForTBPatientLiveData.postValue(nextVisitDate)

                saveRxBuddyFollowUpLiveData.postValue(
                    rxBuddyRepository.insertRxBuddyFollowUp(
                        rxBuddyLocalId = rxBuddyLocalId,
                        rxBuddyId = rxBuddyId,
                        patientMemberId = patientMemberId,
                        map = rxBuddyFollowUp,
                        nextVisitDate = nextVisitDate,
                        followUpId = followUpId,
                    ),
                )
            }
            val assessmentDetail = StringConverter.convertGivenMapToString(map) ?: ""
            assessmentStringLiveData.postValue(assessmentDetail)
        }
    }

    fun updateNextVisitDateForRxBuddyRegister(
        nextVisitDate: String,
        id: Long,
    ) {
        viewModelScope.launch(dispatcherIO) {
            assessmentUpdateLiveData.postValue(
                rxBuddyRepository.updateNextVisitDateRxBuddyRegister(
                    nextVisitDate,
                    id,
                ),
            )
        }
    }

    fun updateNextVisitDateForRxBuddyFollowUp(
        nextVisitDate: String,
        id: Long,
    ) {
        viewModelScope.launch(dispatcherIO) {
            assessmentUpdateLiveData.postValue(
                rxBuddyRepository.updateNextVisitDateRxBuddyFollowUp(
                    nextVisitDate,
                    id,
                ),
            )
        }
    }

    val nextVisitDateForTBPatientLiveData = MutableLiveData<LocalDate>()

    private fun getNextVisitForConfirmedTbPatient(): LocalDate {
        val startDate = treatmentDetailsLiveData.value?.tbConfirmationDate
        val visitList = getTBNextVisitSchedule()
        if (startDate != null && startDate.trim().isNotEmpty()) {
            val today = LocalDate.now()
            val dStartDate = startDate.getLocalDate()
            visitList.forEach { visit ->
                val nextVisit = dStartDate.plusMonths(visit.first).plusWeeks(visit.second)
                if (nextVisit.isAfter(today)) {
                    return nextVisit
                }
            }
        }
        return LocalDate.now().plusDays(1)
    }

    private fun getTBNextVisitSchedule(): List<Pair<Long, Long>> =
        listOf(
            Pair(0, 1), // 1st Month, 1st Week
            Pair(0, 3), // 1st Month, 3rd Week
            Pair(1, 1), // 2nd Month, 1st Week
            Pair(1, 3), // 2nd Month, 3rd Week
            Pair(2, 2), // 3rd Month, 2nd Week
            Pair(3, 3), // 4th Month, 3rd Week
            Pair(4, 4), // 5th Month, 4th Week
            Pair(5, 1), // 6th Month, 1st Week
        )

    /**
     * Identifies fields of type DialogCheckbox from the server data and initiates the transformation
     * of their values in the assessment map from a list of maps to a list of strings.
     *
     * @param map The assessment data map to be modified.
     * @param serverData The list of FormLayout defining the field types.
     */
    private fun modifyDialogCheckboxValues(
        map: MutableMap<Any, Any>,
        serverData: List<FormLayout>?,
    ) {
        serverData?.let { layouts ->
            val dialogCheckboxIds =
                layouts.filter { it.viewType == ViewType.VIEW_TYPE_DIALOG_CHECKBOX }.map { it.id }.toSet()
            if (dialogCheckboxIds.isNotEmpty()) {
                processMapForDialogCheckbox(map, dialogCheckboxIds)
            }
        }
    }

    /**
     * Recursively traverses the assessment map to find and transform values for keys that match
     * DialogCheckbox IDs. It converts a List<Map<*, *>> into a List<String> by extracting the "value" field.
     *
     * @param map The map (or nested map) to process.
     * @param dialogCheckboxIds The set of IDs identified as DialogCheckbox fields.
     */
    private fun processMapForDialogCheckbox(
        map: MutableMap<Any, Any>,
        dialogCheckboxIds: Set<String>,
    ) {
        val keys = map.keys.toList()
        keys.forEach { key ->
            val value = map[key]
            if (key is String && dialogCheckboxIds.contains(key)) {
                if (value is List<*>) {
                    val stringList = mutableListOf<String>()
                    value.forEach { item ->
                        if (item is Map<*, *>) {
                            (item[DefinedParams.Value] as? String)?.let { stringList.add(it) }
                        }
                    }
                    map[key] = stringList
                }
            } else if (value is MutableMap<*, *>) {
                processMapForDialogCheckbox(value as MutableMap<Any, Any>, dialogCheckboxIds)
            }
        }
    }

    fun getRiskEntityList() {
        viewModelScope.launch(dispatcherIO) {
            val resultOne = metaRepository.riskFactorListing()
            val baseType: Type = object : TypeToken<ArrayList<RiskClassificationModel>>() {}.type
            if (resultOne.isNotEmpty()) {
                val resultList = Gson().fromJson<ArrayList<RiskClassificationModel>>(
                    resultOne[0].nonLabEntity,
                    baseType,
                )
                riskClassificationModels.clear()
                riskClassificationModels.addAll(resultList)
            }
        }
    }

    suspend fun getLastServiceHistory(type: String): MemberAssessmentHistoryEntity? =
        assessmentRepository.getLastServiceHistory(
            selectedHouseholdMemberId,
            type.lowercase(),
        )
}
