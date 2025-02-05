package com.medtroniclabs.opensource.ui.medicalreview.underfiveyears

import com.medtroniclabs.opensource.model.medicalreview.ClinicalSummaryAndSigns
import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.data.model.MedicalReviewEncounter
import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.mappingkey.UnderFiveYearExaminationKeyMapping
import com.medtroniclabs.opensource.mappingkey.UnderFiveYearExaminationKeyMapping.Diarrhoea.bloodyDiarrhoea
import com.medtroniclabs.opensource.mappingkey.UnderFiveYearExaminationKeyMapping.Diarrhoea.signs
import com.medtroniclabs.opensource.model.PatientListRespModel
import com.medtroniclabs.opensource.model.medicalreview.CoughOrDifficultBreathing
import com.medtroniclabs.opensource.model.medicalreview.CreateUnderFiveYearsRequest
import com.medtroniclabs.opensource.model.medicalreview.CreateUnderTwoMonthsResponse
import com.medtroniclabs.opensource.model.medicalreview.EarProblem
import com.medtroniclabs.opensource.model.medicalreview.Fever
import com.medtroniclabs.opensource.model.medicalreview.GeneralDangerSings
import com.medtroniclabs.opensource.model.medicalreview.HivAndAids
import com.medtroniclabs.opensource.model.medicalreview.MalnutritionOrAnaemia
import com.medtroniclabs.opensource.model.medicalreview.UnderFiveDiarrhoea
import com.medtroniclabs.opensource.model.medicalreview.UnderFiveExamination
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.repo.MedicalReviewSummaryRepository
import com.medtroniclabs.opensource.repo.UnderFiveYearsRepository
import com.medtroniclabs.opensource.ui.BaseViewModel
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewTypeEnums
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UnderFiveYearsViewModel @Inject constructor(
    @IoDispatcher override var dispatcherIO: CoroutineDispatcher,
    private var underFiveYearsRepository: UnderFiveYearsRepository,
    private var summaryRepository: MedicalReviewSummaryRepository
) : BaseViewModel(dispatcherIO) {

    val createUnderFiveMedicalReviewLiveData =
        MutableLiveData<Resource<CreateUnderTwoMonthsResponse>>()
    val createUnderFiveYearMedicalReview: LiveData<Resource<CreateUnderTwoMonthsResponse>>
        get() = createUnderFiveMedicalReviewLiveData
    private var lastLocation: Location? = null
    val summaryCreateResponse = MutableLiveData<Resource<HashMap<String, Any>>>()

    val underFiveYearsMetaLiveData = MutableLiveData<Resource<Boolean>>()
    var patientId: String? = null
    var memberId: String? = null
    var isSwipeRefresh: Boolean = false
    var encounterId: String? = null
    var patientReference:String? = null

    fun getStaticMetaData() {
        viewModelScope.launch(dispatcherIO) {
            underFiveYearsMetaLiveData.postLoading()
            underFiveYearsMetaLiveData.postValue(
                underFiveYearsRepository.getStaticMetaData(
                    MedicalReviewTypeEnums.UNDER_FIVE_YEARS.name
                )
            )
        }
    }

    fun createMedicalReviewForUnderFiveYears(
        patientDetail: PatientListRespModel,
        clinicalSummaryAndSigns: ClinicalSummaryAndSigns,
        examinationResultHashMap: HashMap<String, Any>,
        clinicalNotes: String,
        presentingComplaints: String,
        systemicExaminations: List<String?>?,
        systemicExaminationsNotes: String?,
        encounterId: String?
    ) {
        val hhId = patientDetail.houseHoldId
        val memberId = patientDetail.memberId
        val selectedPatientId = patientId

        if (memberId != null && selectedPatientId != null) {
            viewModelScope.launch(dispatcherIO) {
                val examination = getUnderFiveExamination(examinationResultHashMap)
                val currentDateAndTime = DateUtils.getCurrentDateAndTime(DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ)
                val encounter = MedicalReviewEncounter(
                    id = encounterId,
                    startTime = currentDateAndTime,
                    endTime = currentDateAndTime,
                    latitude = lastLocation?.latitude ?: 0.0,
                    longitude = lastLocation?.longitude ?: 0.0,
                    householdId = hhId,
                    patientId = selectedPatientId,
                    memberId = memberId,
                    referred = true,
                    provenance = ProvanceDto()
                )
                val underFiveMedicalReviewRequest = CreateUnderFiveYearsRequest(
                    id = encounterId,
                    clinicalNotes = clinicalNotes,
                    clinicalSummaryAndSigns = clinicalSummaryAndSigns.takeIf { it.isNotEmpty() },
                    examination = examination,
                    presentingComplaints = presentingComplaints.takeIf { it.isNotEmpty() },
                    encounter = encounter,
                    systemicExaminationNotes = systemicExaminationsNotes,
                    systemicExamination = systemicExaminations
                )
                createUnderFiveMedicalReviewLiveData.postLoading()
                createUnderFiveMedicalReviewLiveData.postValue(
                    underFiveYearsRepository.createMedicalReviewForUnderFiveYears(underFiveMedicalReviewRequest)
                )
            }
        }
    }


    private fun getUnderFiveExamination(examinationResultHashMap: HashMap<String, Any>): UnderFiveExamination? {
        val diarrhoea = mapDiarrhoea(examinationResultHashMap)
        val malnutritionOrAnaemia = mapMalnutritionOrAnaemia(examinationResultHashMap)
        val coughOrDifficultBreathing = mapCoughOrDifficultBreathing(examinationResultHashMap)
        val fever = mapFever(examinationResultHashMap)
        val earProblem = mapEarProblem(examinationResultHashMap)
        val hiv = mapHivAndAids(examinationResultHashMap)
        val generalDangerSings = mapGeneralDangerSings(examinationResultHashMap)
        if (diarrhoea == null && fever == null && generalDangerSings == null && malnutritionOrAnaemia == null && coughOrDifficultBreathing == null && earProblem == null && hiv == null) {
            return null
        }
        return UnderFiveExamination(
            diarrhoea = diarrhoea,
            hivRDT = hiv,
            anaemia = malnutritionOrAnaemia,
            cough = coughOrDifficultBreathing,
            fever = fever,
            earProblem = earProblem,
            generalDangerSigns = generalDangerSings
        )
    }


    private fun mapDiarrhoea(examinationResultHashMap: HashMap<String, Any>): UnderFiveDiarrhoea? {
        if (examinationResultHashMap.containsKey(UnderFiveYearExaminationKeyMapping.DiseaseName.diarrhoea)) {
            val diarrhoeaHashMap =
                examinationResultHashMap[UnderFiveYearExaminationKeyMapping.DiseaseName.diarrhoea] as HashMap<String, Any>

            if (diarrhoeaHashMap.isNotEmpty()) {
                var diarrhoea = UnderFiveDiarrhoea()
                if (diarrhoeaHashMap.containsKey(UnderFiveYearExaminationKeyMapping.Diarrhoea.hasDiarrhoea)) {
                    diarrhoea = diarrhoea.copy(
                        hasDiarrhoea = mapStringToBoolean(diarrhoeaHashMap[UnderFiveYearExaminationKeyMapping.Diarrhoea.hasDiarrhoea] as String)
                    )
                }
                if (diarrhoeaHashMap.containsKey(bloodyDiarrhoea)) {
                    diarrhoea = diarrhoea.copy(
                        bloodyDiarrhoea = mapStringToBoolean(diarrhoeaHashMap[bloodyDiarrhoea] as String)
                    )

                }
                if (diarrhoeaHashMap.containsKey(signs)) {
                    diarrhoea = diarrhoea.copy(
                        signs = (diarrhoeaHashMap[signs] as List<HashMap<String, Any>?>?)?.filterNotNull()
                            ?.mapNotNull { (it[DefinedParams.Value] as? String)?.takeIf { name -> name.isNotBlank() } }
                    )
                }
                if (diarrhoeaHashMap.containsKey(UnderFiveYearExaminationKeyMapping.Diarrhoea.timePeriod)) {
                    diarrhoea = diarrhoea.copy(
                        timePeriod = (diarrhoeaHashMap[UnderFiveYearExaminationKeyMapping.Diarrhoea.timePeriod] as Double).toInt()
                    )
                }
                return diarrhoea

            }
        }

        return null
    }


    private fun mapHivAndAids(examinationResultHashMap: HashMap<String, Any>): HivAndAids? {

        if (examinationResultHashMap.containsKey(UnderFiveYearExaminationKeyMapping.DiseaseName.hivRDT)) {
            val hivAndAidsHashMap =
                examinationResultHashMap[UnderFiveYearExaminationKeyMapping.DiseaseName.hivRDT] as HashMap<String, Any>

            if (hivAndAidsHashMap.isNotEmpty()) {
                var hivAndAids = HivAndAids()
                if (hivAndAidsHashMap.containsKey(UnderFiveYearExaminationKeyMapping.HivAndAids.mother)) {
                    hivAndAids = hivAndAids.copy(
                        mother = hivAndAidsHashMap[UnderFiveYearExaminationKeyMapping.HivAndAids.mother] as String
                    )

                }
                if (hivAndAidsHashMap.containsKey(UnderFiveYearExaminationKeyMapping.HivAndAids.child)) {
                    hivAndAids = hivAndAids.copy(
                        child = hivAndAidsHashMap[UnderFiveYearExaminationKeyMapping.HivAndAids.child] as String
                    )
                }

                return hivAndAids
            }
        }
        return null
    }

    private fun mapMalnutritionOrAnaemia(examinationResultHashMap: HashMap<String, Any>): MalnutritionOrAnaemia? {
        if (examinationResultHashMap.containsKey(UnderFiveYearExaminationKeyMapping.DiseaseName.anaemia)) {
            val malnutritionOrAnaemiaHashMap =
                examinationResultHashMap[UnderFiveYearExaminationKeyMapping.DiseaseName.anaemia] as HashMap<String, Any>
            if (malnutritionOrAnaemiaHashMap.isNotEmpty()) {
                var malnutritionOrAnaemia = MalnutritionOrAnaemia()
                if (malnutritionOrAnaemiaHashMap.containsKey(UnderFiveYearExaminationKeyMapping.MalnutritionAndAnaemia.appetiteTest)) {
                    malnutritionOrAnaemia = malnutritionOrAnaemia.copy(
                        appetiteTest = mapStringToBoolean(malnutritionOrAnaemiaHashMap[UnderFiveYearExaminationKeyMapping.MalnutritionAndAnaemia.appetiteTest] as String)
                    )
                }
                if (malnutritionOrAnaemiaHashMap.containsKey(UnderFiveYearExaminationKeyMapping.MalnutritionAndAnaemia.signs)) {
                    malnutritionOrAnaemia = malnutritionOrAnaemia.copy(
                        signs = (malnutritionOrAnaemiaHashMap[UnderFiveYearExaminationKeyMapping.MalnutritionAndAnaemia.signs] as List<HashMap<String, Any>?>?)?.filterNotNull()
                        ?.mapNotNull { (it[DefinedParams.Value] as? String)?.takeIf { name -> name.isNotBlank() } }
                    )
                }
                return malnutritionOrAnaemia
            }
        }
        return null
    }

    private fun mapCoughOrDifficultBreathing(examinationResultHashMap: HashMap<String, Any>): CoughOrDifficultBreathing? {
        if (examinationResultHashMap.containsKey(UnderFiveYearExaminationKeyMapping.DiseaseName.cough)) {
            val coughOrDifficultBreathingHashMap =
                examinationResultHashMap[UnderFiveYearExaminationKeyMapping.DiseaseName.cough] as HashMap<String, Any>
            if (coughOrDifficultBreathingHashMap.isNotEmpty()) {
                var coughOrDifficultBreathing = CoughOrDifficultBreathing()
                if (coughOrDifficultBreathingHashMap.containsKey(UnderFiveYearExaminationKeyMapping.Cough.coughOrDifficultBreathing)) {
                    coughOrDifficultBreathing = coughOrDifficultBreathing.copy(
                        coughOrDIfficultBreathing = mapStringToBoolean(
                            coughOrDifficultBreathingHashMap[UnderFiveYearExaminationKeyMapping.Cough.coughOrDifficultBreathing] as String
                        )
                    )
                }
                if (coughOrDifficultBreathingHashMap.containsKey(UnderFiveYearExaminationKeyMapping.Cough.chestIndrawing)) {
                    coughOrDifficultBreathing = coughOrDifficultBreathing.copy(
                        chestIndrawing = mapStringToBoolean(coughOrDifficultBreathingHashMap[UnderFiveYearExaminationKeyMapping.Cough.chestIndrawing] as String)
                    )
                }
                if (coughOrDifficultBreathingHashMap.containsKey(UnderFiveYearExaminationKeyMapping.Cough.stridor)) {
                    coughOrDifficultBreathing = coughOrDifficultBreathing.copy(
                        stridor = mapStringToBoolean(coughOrDifficultBreathingHashMap[UnderFiveYearExaminationKeyMapping.Cough.stridor] as String)
                    )
                }
                if (coughOrDifficultBreathingHashMap.containsKey(UnderFiveYearExaminationKeyMapping.Cough.noOfDays)) {
                    coughOrDifficultBreathing = coughOrDifficultBreathing.copy(
                        noOfDays = (coughOrDifficultBreathingHashMap[UnderFiveYearExaminationKeyMapping.Cough.noOfDays] as Double).toInt()
                    )
                }

                return coughOrDifficultBreathing
            }
        }
        return null
    }

    private fun mapFever(examinationResultHashMap: HashMap<String, Any>): Fever? {

        if (examinationResultHashMap.containsKey(UnderFiveYearExaminationKeyMapping.DiseaseName.fever)) {
            val feverHashMap =
                examinationResultHashMap[UnderFiveYearExaminationKeyMapping.DiseaseName.fever] as HashMap<String, Any>

            if (feverHashMap.isNotEmpty()) {
                var fever = Fever()
                if (feverHashMap.containsKey(UnderFiveYearExaminationKeyMapping.Fever.hasFever)) {
                    fever = fever.copy(
                        hasFever = mapStringToBoolean(feverHashMap[UnderFiveYearExaminationKeyMapping.Fever.hasFever] as String)
                    )
                }

                if (feverHashMap.containsKey(UnderFiveYearExaminationKeyMapping.Fever.isMotherHasFever)) {
                    fever = fever.copy(
                        isMotherHasFever = feverHashMap[UnderFiveYearExaminationKeyMapping.Fever.isMotherHasFever] as String
                    )
                }

                if (feverHashMap.containsKey(UnderFiveYearExaminationKeyMapping.Fever.microscopyResult)) {
                    fever = fever.copy(
                        microscopyResult = feverHashMap[UnderFiveYearExaminationKeyMapping.Fever.microscopyResult] as String
                    )

                }

                if (feverHashMap.containsKey(UnderFiveYearExaminationKeyMapping.Fever.signs)) {
                    fever = fever.copy(
                        signs = (feverHashMap[UnderFiveYearExaminationKeyMapping.Fever.signs] as List<HashMap<String, Any>?>?)?.filterNotNull()
                            ?.mapNotNull { (it[DefinedParams.Value] as? String)?.takeIf { name -> name.isNotBlank() } }
                    )
                }

                if (feverHashMap.containsKey(UnderFiveYearExaminationKeyMapping.Fever.noOfDays)) {
                    fever = fever.copy(
                        noOfDays = (feverHashMap[UnderFiveYearExaminationKeyMapping.Fever.noOfDays] as Double).toInt()
                    )
                }
                return fever
            }
        }
        return null
    }

    private fun mapEarProblem(examinationResultHashMap: HashMap<String, Any>): EarProblem? {
        if (examinationResultHashMap.containsKey(UnderFiveYearExaminationKeyMapping.DiseaseName.earProblem)) {
            val earProblemHashMap =
                examinationResultHashMap[UnderFiveYearExaminationKeyMapping.DiseaseName.earProblem] as HashMap<String, Any>
            if (earProblemHashMap.isNotEmpty()) {
                var earProblem = EarProblem()
                if (earProblemHashMap.containsKey(UnderFiveYearExaminationKeyMapping.EarProblem.hasEarPain)) {
                    earProblem = earProblem.copy(
                        hasEarPain = mapStringToBoolean(earProblemHashMap[UnderFiveYearExaminationKeyMapping.EarProblem.hasEarPain] as String)
                    )
                }
                if (earProblemHashMap.containsKey(UnderFiveYearExaminationKeyMapping.EarProblem.earDischarge)) {
                    earProblem = earProblem.copy(
                        earDischarge = earProblemHashMap[UnderFiveYearExaminationKeyMapping.EarProblem.earDischarge] as String
                    )
                }
                if (earProblemHashMap.containsKey(UnderFiveYearExaminationKeyMapping.EarProblem.noOfDays)) {
                    earProblem = earProblem.copy(
                        noOfDays = (earProblemHashMap[UnderFiveYearExaminationKeyMapping.EarProblem.noOfDays] as Double).toInt()
                    )
                }
                return earProblem
            }
        }
        return null
    }

    private fun mapGeneralDangerSings(examinationResultHashMap: HashMap<String, Any>): GeneralDangerSings? {
        if (examinationResultHashMap.containsKey(UnderFiveYearExaminationKeyMapping.DiseaseName.generalDangerSigns)) {
            val generalDangerSingsHashMap =
                examinationResultHashMap[UnderFiveYearExaminationKeyMapping.DiseaseName.generalDangerSigns] as HashMap<String, Any>
            if (generalDangerSingsHashMap.isNotEmpty()) {
                var generalDangerSings = GeneralDangerSings()
                if (generalDangerSingsHashMap.containsKey(UnderFiveYearExaminationKeyMapping.GeneralDangerSigns.unableToDrinkOrBreastfeed)) {
                    generalDangerSings = generalDangerSings.copy(
                        unableToDrinkOrBreastfeed = mapStringToBoolean(generalDangerSingsHashMap[UnderFiveYearExaminationKeyMapping.GeneralDangerSigns.unableToDrinkOrBreastfeed] as String)
                    )
                }
                if (generalDangerSingsHashMap.containsKey(UnderFiveYearExaminationKeyMapping.GeneralDangerSigns.historyOfConvulsion)) {
                    generalDangerSings = generalDangerSings.copy(
                        historyOfConvulsion = mapStringToBoolean(generalDangerSingsHashMap[UnderFiveYearExaminationKeyMapping.GeneralDangerSigns.historyOfConvulsion] as String)
                    )
                }
                if (generalDangerSingsHashMap.containsKey(UnderFiveYearExaminationKeyMapping.GeneralDangerSigns.lethargicOrUnconscious)) {
                    generalDangerSings = generalDangerSings.copy(
                        lethargicOrUnconscious = mapStringToBoolean(generalDangerSingsHashMap[UnderFiveYearExaminationKeyMapping.GeneralDangerSigns.lethargicOrUnconscious] as String)
                    )
                }
                if (generalDangerSingsHashMap.containsKey(UnderFiveYearExaminationKeyMapping.GeneralDangerSigns.vomitingEverything)) {
                    generalDangerSings = generalDangerSings.copy(
                        vomitingEverything = mapStringToBoolean(generalDangerSingsHashMap[UnderFiveYearExaminationKeyMapping.GeneralDangerSigns.vomitingEverything] as String)
                    )
                }
                if (generalDangerSingsHashMap.containsKey(UnderFiveYearExaminationKeyMapping.GeneralDangerSigns.convulsingNow)) {
                    generalDangerSings = generalDangerSings.copy(
                        convulsingNow = mapStringToBoolean(generalDangerSingsHashMap[UnderFiveYearExaminationKeyMapping.GeneralDangerSigns.convulsingNow] as String)
                    )
                }
                return generalDangerSings
            }
        }
        return null
    }


    private fun mapStringToBoolean(value: String): Boolean {
        return value == DefinedParams.Yes
    }

    fun underFiveYearsSummaryCreate(
        details: PatientListRespModel,
        submitEncounterId: String,
        nextFollowUpDate: String?,
        selectedPatientStatus: String?,
        patientReferenceId: String
    ) {
        viewModelScope.launch(dispatcherIO) {
            summaryCreateResponse.postLoading()
            val patientId = details.patientId
            val memberId = details.memberId
            val householdId = details.houseHoldId
            val villageId = details.villageId

            if (patientId != null && memberId != null && villageId != null) {
                val convertedNextVisitDate = DateUtils.convertDateTimeToDate(
                    nextFollowUpDate,
                    DateUtils.DATE_ddMMyyyy,
                    DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                    inUTC = true
                )

                val response = summaryRepository.createSummarySubmit(
                    patientId = patientId,
                    patientReference = patientReferenceId,
                    memberId = memberId,
                    id = submitEncounterId,
                    patientStatus = selectedPatientStatus ?: "",
                    nextVisitDate = convertedNextVisitDate,
                    assessmentName = MedicalReviewTypeEnums.UNDER_FIVE_YEARS.name,
                    referralTicketType = MedicalReviewTypeEnums.ICCM.name,
                    householdId = householdId,
                    villageId = villageId
                )
                summaryCreateResponse.postValue(response)
            }
        }
    }
}