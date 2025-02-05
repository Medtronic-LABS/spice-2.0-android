package com.medtroniclabs.opensource.ui.medicalreview.undertwomonths.viewmodel

import com.medtroniclabs.opensource.model.medicalreview.BreastfeedingProblem
import com.medtroniclabs.opensource.model.medicalreview.ClinicalSummaryAndSigns
import com.medtroniclabs.opensource.model.medicalreview.CreateUnderTwoMonthsRequest
import com.medtroniclabs.opensource.model.medicalreview.Diarrhoea
import com.medtroniclabs.opensource.model.medicalreview.Examination
import com.medtroniclabs.opensource.model.medicalreview.Hiv
import com.medtroniclabs.opensource.model.medicalreview.Jaundice
import com.medtroniclabs.opensource.model.medicalreview.NonBreastfeedingProblem
import com.medtroniclabs.opensource.model.medicalreview.VerySevereDisease
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
import com.medtroniclabs.opensource.mappingkey.UnderTwoExaminationKeyMapping
import com.medtroniclabs.opensource.model.PatientListRespModel
import com.medtroniclabs.opensource.model.medicalreview.CreateUnderTwoMonthsResponse
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.network.utils.ConnectivityManager
import com.medtroniclabs.opensource.repo.MedicalReviewSummaryRepository
import com.medtroniclabs.opensource.repo.UnderTwoMonthsRepository
import com.medtroniclabs.opensource.ui.BaseViewModel
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewTypeEnums
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UnderTwoMonthViewModel @Inject constructor(
    @IoDispatcher override var dispatcherIO: CoroutineDispatcher,
    private var repository: UnderTwoMonthsRepository,
    private var summaryRepository: MedicalReviewSummaryRepository
) : BaseViewModel(dispatcherIO){
    @Inject
    lateinit var connectivityManager: ConnectivityManager
    var memberId: String? = null
    private val createUnderTwoMonthsMedicalReviewLiveData =
        MutableLiveData<Resource<CreateUnderTwoMonthsResponse>>()
    val createUnderTwoMonthsMedicalReview: LiveData<Resource<CreateUnderTwoMonthsResponse>>
        get() = createUnderTwoMonthsMedicalReviewLiveData
    val underTwoMonthsMetaLiveData = MutableLiveData<Resource<Boolean>>()
    var patientId: String? = null
    private var lastLocation: Location? = null
    val summaryCreateResponse = MutableLiveData<Resource<HashMap<String, Any>>>()
    var isRefresh: Boolean=false
    var encounterId:String?=null
    var patientReference:String?=null
    fun getStaticMetaData() {
        viewModelScope.launch(dispatcherIO) {
            underTwoMonthsMetaLiveData.postLoading()
            underTwoMonthsMetaLiveData.postValue(repository.getStaticMetaData())
        }
    }

    fun createMedicalReviewForUnderTwoMonths(
        details: PatientListRespModel,
        clinicalSummaryAndSigns: ClinicalSummaryAndSigns,
        examinationResultHashMap: HashMap<String, Any>,
        clinicalNotes: String,
        presentingComplaints: String,
        prescriptionEncounterId: String?
    ) {
        details.houseHoldId.let { hhId ->
            details.memberId?.let { memberId ->
                patientId?.let { selectedPatientId ->
                    viewModelScope.launch(dispatcherIO) {
                        val examination = getUnderTwoExamination(examinationResultHashMap)
                        val underTwoMedicalReviewRequest = CreateUnderTwoMonthsRequest(
                            id = prescriptionEncounterId,
                            clinicalNotes = clinicalNotes,
                            clinicalSummaryAndSigns = clinicalSummaryAndSigns.takeIf { it.isNotEmpty() },
                            examination = examination,
                            presentingComplaints = presentingComplaints.takeIf { it.isNotEmpty() },
                            encounter = createUnderTwoMonthsEncounter(hhId, selectedPatientId, memberId,prescriptionEncounterId)
                        )

                        createUnderTwoMonthsMedicalReviewLiveData.postLoading()
                        createUnderTwoMonthsMedicalReviewLiveData.postValue(
                            repository.createMedicalReviewForUnderTwoMonths(underTwoMedicalReviewRequest)
                        )
                    }
                }
            }
        }
    }

    private fun createUnderTwoMonthsEncounter(
        householdId: String?,
        patientId: String,
        memberId: String,
        prescriptionEncounterId: String?
    ): MedicalReviewEncounter {
        val currentTime = DateUtils.getCurrentDateAndTime(DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ)
        return MedicalReviewEncounter(
            id = prescriptionEncounterId,
            startTime = currentTime,
            endTime = currentTime,
            latitude = lastLocation?.latitude ?: 0.0,
            longitude = lastLocation?.longitude ?: 0.0,
            householdId = householdId,
            patientId = patientId,
            memberId = memberId,
            referred = true,
            provenance = ProvanceDto()
        )
    }

    private fun getUnderTwoExamination(examinationResultHashMap: HashMap<String, Any>): Examination? {
        val diarrhoea = mapDiarrhoea(examinationResultHashMap)
        val verySevereDisease = mapVerySevereDisease(examinationResultHashMap)
        val jaundice = mapJaundice(examinationResultHashMap)
        val nonBreastFeeding = mapNonBreastFeeding(examinationResultHashMap)
        val breastFeeding = mapBreastFeeding(examinationResultHashMap)
        val hiv = mapHiv(examinationResultHashMap)
        if (diarrhoea == null && verySevereDisease == null && jaundice == null && nonBreastFeeding == null && breastFeeding == null && hiv == null) {
            return null
        }
        return Examination(
            diarrhoea = diarrhoea,
            verySevereDisease = verySevereDisease,
            jaundice = jaundice,
            nonBreastfeedingProblem = nonBreastFeeding,
            breastfeedingProblem = breastFeeding,
            hivInfection = hiv
        )
    }

    private fun mapHiv(examinationResultHashMap: HashMap<String, Any>): Hiv? {
        if (examinationResultHashMap.containsKey(UnderTwoExaminationKeyMapping.DiseaseName.hiv)) {
            val hivHashMap =
                examinationResultHashMap[UnderTwoExaminationKeyMapping.DiseaseName.hiv] as HashMap<String, Any>
            if (hivHashMap.isNotEmpty()) {
                var hiv = Hiv()
                if (hivHashMap.containsKey(UnderTwoExaminationKeyMapping.Hiv.hasPositiveVirologicalTestForInfant)) {
                    hiv = hiv.copy(
                        hasPositiveVirologicalTestForInfant = mapStringToBoolean(hivHashMap[UnderTwoExaminationKeyMapping.Hiv.hasPositiveVirologicalTestForInfant] as String)
                    )
                }
                if (hivHashMap.containsKey(UnderTwoExaminationKeyMapping.Hiv.isMotherPostiveAndChildNegative)) {
                    hiv = hiv.copy(
                        isMotherPostiveAndChildNegative = mapStringToBoolean(hivHashMap[UnderTwoExaminationKeyMapping.Hiv.isMotherPostiveAndChildNegative] as String)
                    )
                }
                if (hivHashMap.containsKey(UnderTwoExaminationKeyMapping.Hiv.hasPositiveAntibodyTestForInfant)) {
                    hiv = hiv.copy(
                        hasPositiveAntibodyTestForInfant = mapStringToBoolean(hivHashMap[UnderTwoExaminationKeyMapping.Hiv.hasPositiveAntibodyTestForInfant] as String)
                    )
                }
                if (hivHashMap.containsKey(UnderTwoExaminationKeyMapping.Hiv.isMotherPostiveAndInfantNotTested)) {
                    hiv = hiv.copy(
                        isMotherPostiveAndInfantNotTested = mapStringToBoolean(hivHashMap[UnderTwoExaminationKeyMapping.Hiv.isMotherPostiveAndInfantNotTested] as String)
                    )
                }
                if (hivHashMap.containsKey(UnderTwoExaminationKeyMapping.Hiv.hasNegativeForMotherAndChild)) {
                    hiv = hiv.copy(
                        hasNegativeForMotherAndChild = mapStringToBoolean(hivHashMap[UnderTwoExaminationKeyMapping.Hiv.hasNegativeForMotherAndChild] as String)
                    )
                }
                return hiv
            }
        }
        return null
    }

    private fun mapBreastFeeding(examinationResultHashMap: java.util.HashMap<String, Any>): BreastfeedingProblem? {
        if (examinationResultHashMap.containsKey(UnderTwoExaminationKeyMapping.DiseaseName.breastFeeding)) {
            val breastfeedingHashMap =
                examinationResultHashMap[UnderTwoExaminationKeyMapping.DiseaseName.breastFeeding] as HashMap<String, Any>
            if (breastfeedingHashMap.isNotEmpty()) {
                var breastfeedingProblem = BreastfeedingProblem()
                if (breastfeedingHashMap.containsKey(UnderTwoExaminationKeyMapping.BreastFeedingProblem.anyBreastfeedingDifficulty)) {
                    breastfeedingProblem = breastfeedingProblem.copy(
                        anyBreastfeedingDifficulty = mapStringToBoolean(breastfeedingHashMap[UnderTwoExaminationKeyMapping.BreastFeedingProblem.anyBreastfeedingDifficulty] as String)
                    )
                }
                if (breastfeedingHashMap.containsKey(UnderTwoExaminationKeyMapping.BreastFeedingProblem.lessThan8BreastfeedIn24hrs)) {
                    breastfeedingProblem = breastfeedingProblem.copy(
                        lessThan8BreastfeedIn24hrs = mapStringToBoolean(breastfeedingHashMap[UnderTwoExaminationKeyMapping.BreastFeedingProblem.lessThan8BreastfeedIn24hrs] as String)
                    )
                }
                if (breastfeedingHashMap.containsKey(UnderTwoExaminationKeyMapping.BreastFeedingProblem.switchingBreastFrequently)) {
                    breastfeedingProblem = breastfeedingProblem.copy(
                        switchingBreastFrequently = mapStringToBoolean(breastfeedingHashMap[UnderTwoExaminationKeyMapping.BreastFeedingProblem.switchingBreastFrequently] as String)
                    )
                }
                if (breastfeedingHashMap.containsKey(UnderTwoExaminationKeyMapping.BreastFeedingProblem.notIncreasingBFInIllness)) {
                    breastfeedingProblem = breastfeedingProblem.copy(
                        notIncreasingBFInIllness = mapStringToBoolean(breastfeedingHashMap[UnderTwoExaminationKeyMapping.BreastFeedingProblem.notIncreasingBFInIllness] as String)
                    )
                }
                if (breastfeedingHashMap.containsKey(UnderTwoExaminationKeyMapping.BreastFeedingProblem.receivesOtherFoodsOrDrinks)) {
                    breastfeedingProblem = breastfeedingProblem.copy(
                        receivesOtherFoodsOrDrinks = mapStringToBoolean(breastfeedingHashMap[UnderTwoExaminationKeyMapping.BreastFeedingProblem.receivesOtherFoodsOrDrinks] as String)
                    )
                }

                if (breastfeedingHashMap.containsKey(UnderTwoExaminationKeyMapping.BreastFeedingProblem.mouthUlcersOrThrush)) {
                    breastfeedingProblem = breastfeedingProblem.copy(
                        mouthUlcersOrThrush = mapStringToBoolean(breastfeedingHashMap[UnderTwoExaminationKeyMapping.BreastFeedingProblem.mouthUlcersOrThrush] as String)
                    )
                }
                if (breastfeedingHashMap.containsKey(UnderTwoExaminationKeyMapping.BreastFeedingProblem.underweight)) {
                    breastfeedingProblem = breastfeedingProblem.copy(
                        underweight = mapStringToBoolean(breastfeedingHashMap[UnderTwoExaminationKeyMapping.BreastFeedingProblem.underweight] as String)
                    )
                }
                if (breastfeedingHashMap.containsKey(UnderTwoExaminationKeyMapping.BreastFeedingProblem.positioning)) {
                    breastfeedingProblem = breastfeedingProblem.copy(
                        positioning = breastfeedingHashMap[UnderTwoExaminationKeyMapping.BreastFeedingProblem.positioning] as String
                    )
                }
                if (breastfeedingHashMap.containsKey(UnderTwoExaminationKeyMapping.BreastFeedingProblem.attachment)) {
                    breastfeedingProblem = breastfeedingProblem.copy(
                        attachment = breastfeedingHashMap[UnderTwoExaminationKeyMapping.BreastFeedingProblem.attachment] as String
                    )
                }
                if (breastfeedingHashMap.containsKey(UnderTwoExaminationKeyMapping.BreastFeedingProblem.suckling)) {
                    breastfeedingProblem = breastfeedingProblem.copy(
                        suckling = breastfeedingHashMap[UnderTwoExaminationKeyMapping.BreastFeedingProblem.suckling] as String
                    )
                }
                if (breastfeedingHashMap.containsKey(UnderTwoExaminationKeyMapping.BreastFeedingProblem.noFeedingProblem)) {
                    breastfeedingProblem = breastfeedingProblem.copy(
                        noFeedingProblem = breastfeedingHashMap[UnderTwoExaminationKeyMapping.BreastFeedingProblem.noFeedingProblem] as String
                    )
                }
                return breastfeedingProblem
            }
        }
        return null
    }

    private fun mapNonBreastFeeding(examinationResultHashMap: HashMap<String, Any>): NonBreastfeedingProblem? {
        if (examinationResultHashMap.containsKey(UnderTwoExaminationKeyMapping.DiseaseName.nonBreastFeeding)) {
            val nonBreastfeedingHashMap =
                examinationResultHashMap[UnderTwoExaminationKeyMapping.DiseaseName.nonBreastFeeding] as HashMap<String, Any>
            if (nonBreastfeedingHashMap.isNotEmpty()) {
                var nonBreastfeedingProblem = NonBreastfeedingProblem()
                if (nonBreastfeedingHashMap.containsKey(UnderTwoExaminationKeyMapping.NonBreastFeedingProblem.inappropriateReplacementFeeds)) {
                    nonBreastfeedingProblem = nonBreastfeedingProblem.copy(
                        inappropriateReplacementFeeds = mapStringToBoolean(nonBreastfeedingHashMap[UnderTwoExaminationKeyMapping.NonBreastFeedingProblem.inappropriateReplacementFeeds] as String)
                    )
                }
                if (nonBreastfeedingHashMap.containsKey(UnderTwoExaminationKeyMapping.NonBreastFeedingProblem.insufficientReplacementFeeds)) {
                    nonBreastfeedingProblem = nonBreastfeedingProblem.copy(
                        insufficientReplacementFeeds = mapStringToBoolean(nonBreastfeedingHashMap[UnderTwoExaminationKeyMapping.NonBreastFeedingProblem.insufficientReplacementFeeds] as String)
                    )
                }
                if (nonBreastfeedingHashMap.containsKey(UnderTwoExaminationKeyMapping.NonBreastFeedingProblem.incorrectlyPreparedMilk)) {
                    nonBreastfeedingProblem = nonBreastfeedingProblem.copy(
                        incorrectlyPreparedMilk = mapStringToBoolean(nonBreastfeedingHashMap[UnderTwoExaminationKeyMapping.NonBreastFeedingProblem.incorrectlyPreparedMilk] as String)
                    )
                }
                if (nonBreastfeedingHashMap.containsKey(UnderTwoExaminationKeyMapping.NonBreastFeedingProblem.useOfFeedingBottle)) {
                    nonBreastfeedingProblem = nonBreastfeedingProblem.copy(
                        useOfFeedingBottle = mapStringToBoolean(nonBreastfeedingHashMap[UnderTwoExaminationKeyMapping.NonBreastFeedingProblem.useOfFeedingBottle] as String)
                    )
                }
                if (nonBreastfeedingHashMap.containsKey(UnderTwoExaminationKeyMapping.NonBreastFeedingProblem.feedFormHIVPositiveMother)) {
                    nonBreastfeedingProblem = nonBreastfeedingProblem.copy(
                        feedFormHIVPositiveMother = mapStringToBoolean(nonBreastfeedingHashMap[UnderTwoExaminationKeyMapping.NonBreastFeedingProblem.feedFormHIVPositiveMother] as String)
                    )
                }

                if (nonBreastfeedingHashMap.containsKey(UnderTwoExaminationKeyMapping.NonBreastFeedingProblem.bottleFeeding)) {
                    nonBreastfeedingProblem = nonBreastfeedingProblem.copy(
                        bottleFeeding = mapStringToBoolean(nonBreastfeedingHashMap[UnderTwoExaminationKeyMapping.NonBreastFeedingProblem.bottleFeeding] as String)
                    )
                }
                if (nonBreastfeedingHashMap.containsKey(UnderTwoExaminationKeyMapping.NonBreastFeedingProblem.lowWeightForAge)) {
                    nonBreastfeedingProblem = nonBreastfeedingProblem.copy(
                        lowWeightForAge = mapStringToBoolean(nonBreastfeedingHashMap[UnderTwoExaminationKeyMapping.NonBreastFeedingProblem.lowWeightForAge] as String)
                    )
                }
                if (nonBreastfeedingHashMap.containsKey(UnderTwoExaminationKeyMapping.NonBreastFeedingProblem.thrush)) {
                    nonBreastfeedingProblem = nonBreastfeedingProblem.copy(
                        thrush = nonBreastfeedingHashMap[UnderTwoExaminationKeyMapping.NonBreastFeedingProblem.thrush] as String
                    )
                }
                return nonBreastfeedingProblem
            }
        }
        return null
    }

    private fun mapJaundice(examinationResultHashMap: HashMap<String, Any>): Jaundice? {
        if (examinationResultHashMap.containsKey(UnderTwoExaminationKeyMapping.DiseaseName.jaundice)) {
            val verySevereDiseaseHashMap =
                examinationResultHashMap[UnderTwoExaminationKeyMapping.DiseaseName.jaundice] as HashMap<String, Any>
            if (verySevereDiseaseHashMap.isNotEmpty()) {
                var jaundice = Jaundice()
                if (verySevereDiseaseHashMap.containsKey(UnderTwoExaminationKeyMapping.Jaundice.yellowSkinLessThan24hrs)) {
                    jaundice = jaundice.copy(
                        yellowSkinLessThan24hrs = mapStringToBoolean(verySevereDiseaseHashMap[UnderTwoExaminationKeyMapping.Jaundice.yellowSkinLessThan24hrs] as String)
                    )
                }
                if (verySevereDiseaseHashMap.containsKey(UnderTwoExaminationKeyMapping.Jaundice.yellowPalmsAndSoles)) {
                    jaundice = jaundice.copy(
                        yellowPalmsAndSoles = mapStringToBoolean(verySevereDiseaseHashMap[UnderTwoExaminationKeyMapping.Jaundice.yellowPalmsAndSoles] as String)
                    )
                }
                if (verySevereDiseaseHashMap.containsKey(UnderTwoExaminationKeyMapping.Jaundice.jaundiceAppearing)) {
                    jaundice = jaundice.copy(
                        jaundiceAppearing = mapStringToBoolean(verySevereDiseaseHashMap[UnderTwoExaminationKeyMapping.Jaundice.jaundiceAppearing] as String)
                    )
                }
                if (verySevereDiseaseHashMap.containsKey(UnderTwoExaminationKeyMapping.Jaundice.solesNotYellow)) {
                    jaundice = jaundice.copy(
                        solesNotYellow = mapStringToBoolean(verySevereDiseaseHashMap[UnderTwoExaminationKeyMapping.Jaundice.solesNotYellow] as String)
                    )
                }
                if (verySevereDiseaseHashMap.containsKey(UnderTwoExaminationKeyMapping.Jaundice.noJaundice)) {
                    jaundice = jaundice.copy(
                        noJaundice = mapStringToBoolean(verySevereDiseaseHashMap[UnderTwoExaminationKeyMapping.Jaundice.noJaundice] as String)
                    )
                }
                return jaundice
            }
        }
        return null
    }

    private fun mapVerySevereDisease(examinationResultHashMap: HashMap<String, Any>): VerySevereDisease? {
        if (examinationResultHashMap.containsKey(UnderTwoExaminationKeyMapping.DiseaseName.verySevereDisease)) {
            val verySevereDiseaseHashMap =
                examinationResultHashMap[UnderTwoExaminationKeyMapping.DiseaseName.verySevereDisease] as HashMap<String, Any>
            if (verySevereDiseaseHashMap.isNotEmpty()) {
                var verySevereDisease = VerySevereDisease()
                if (verySevereDiseaseHashMap.containsKey(UnderTwoExaminationKeyMapping.VerySevereDisease.stoppedFeeding)) {
                    verySevereDisease = verySevereDisease.copy(
                        stoppedFeeding = mapStringToBoolean(verySevereDiseaseHashMap[UnderTwoExaminationKeyMapping.VerySevereDisease.stoppedFeeding] as String)
                    )
                }
                if (verySevereDiseaseHashMap.containsKey(UnderTwoExaminationKeyMapping.VerySevereDisease.convulsion)) {
                    verySevereDisease = verySevereDisease.copy(
                        convulsions = mapStringToBoolean(verySevereDiseaseHashMap[UnderTwoExaminationKeyMapping.VerySevereDisease.convulsion] as String)
                    )
                }
                if (verySevereDiseaseHashMap.containsKey(UnderTwoExaminationKeyMapping.VerySevereDisease.movementOnStimulation)) {
                    verySevereDisease = verySevereDisease.copy(
                        movementOnStimulation = mapStringToBoolean(verySevereDiseaseHashMap[UnderTwoExaminationKeyMapping.VerySevereDisease.movementOnStimulation] as String)
                    )
                }
                if (verySevereDiseaseHashMap.containsKey(UnderTwoExaminationKeyMapping.VerySevereDisease.severeChestIndrawing)) {
                    verySevereDisease = verySevereDisease.copy(
                        severeChestIndrawing = mapStringToBoolean(verySevereDiseaseHashMap[UnderTwoExaminationKeyMapping.VerySevereDisease.severeChestIndrawing] as String)
                    )
                }
                if (verySevereDiseaseHashMap.containsKey(UnderTwoExaminationKeyMapping.VerySevereDisease.lowBodyTemperature)) {
                    verySevereDisease = verySevereDisease.copy(
                        lowBodyTemperature = mapStringToBoolean(verySevereDiseaseHashMap[UnderTwoExaminationKeyMapping.VerySevereDisease.lowBodyTemperature] as String)
                    )
                }
                if (verySevereDiseaseHashMap.containsKey(UnderTwoExaminationKeyMapping.VerySevereDisease.skinPustules)) {
                    verySevereDisease = verySevereDisease.copy(
                        skinPustules = mapStringToBoolean(verySevereDiseaseHashMap[UnderTwoExaminationKeyMapping.VerySevereDisease.skinPustules] as String)
                    )
                }
                if (verySevereDiseaseHashMap.containsKey(UnderTwoExaminationKeyMapping.VerySevereDisease.umbilicusRedOrDrainingPus)) {
                    verySevereDisease = verySevereDisease.copy(
                        umbilicusRedOrDrainingPus = mapStringToBoolean(verySevereDiseaseHashMap[UnderTwoExaminationKeyMapping.VerySevereDisease.umbilicusRedOrDrainingPus] as String)
                    )
                }
                return verySevereDisease
            }
        }
        return null
    }

    private fun mapDiarrhoea(examinationResultHashMap: HashMap<String, Any>): Diarrhoea? {
        if (examinationResultHashMap.containsKey(UnderTwoExaminationKeyMapping.DiseaseName.diarrhoea)) {
            val diarrhoeaHashMap =
                examinationResultHashMap[UnderTwoExaminationKeyMapping.DiseaseName.diarrhoea] as HashMap<String, Any>
            if (diarrhoeaHashMap.isNotEmpty()) {
                var diarrhoea = Diarrhoea()
                if (diarrhoeaHashMap.containsKey(UnderTwoExaminationKeyMapping.Diarrhoea.timePeriod)) {
                    diarrhoea =
                        diarrhoea.copy(timePeriod = (diarrhoeaHashMap[UnderTwoExaminationKeyMapping.Diarrhoea.timePeriod] as Double).toInt())
                }
                if (diarrhoeaHashMap.containsKey(UnderTwoExaminationKeyMapping.Diarrhoea.bloodInStool)) {
                    diarrhoea =
                        diarrhoea.copy(bloodInStool = mapStringToBoolean(diarrhoeaHashMap[UnderTwoExaminationKeyMapping.Diarrhoea.bloodInStool] as String))
                }
                if (diarrhoeaHashMap.containsKey(UnderTwoExaminationKeyMapping.Diarrhoea.movementOnStimulation)) {
                    diarrhoea =
                        diarrhoea.copy(movementOnStimulation = mapStringToBoolean(diarrhoeaHashMap[UnderTwoExaminationKeyMapping.Diarrhoea.movementOnStimulation] as String))
                }
                if (diarrhoeaHashMap.containsKey(UnderTwoExaminationKeyMapping.Diarrhoea.noMovementOnStimulation)) {
                    diarrhoea =
                        diarrhoea.copy(noMovementOnStimulation = mapStringToBoolean(diarrhoeaHashMap[UnderTwoExaminationKeyMapping.Diarrhoea.noMovementOnStimulation] as String))
                }
                if (diarrhoeaHashMap.containsKey(UnderTwoExaminationKeyMapping.Diarrhoea.restlessOrIrritable)) {
                    diarrhoea =
                        diarrhoea.copy(restlessOrIrritable = (diarrhoeaHashMap[UnderTwoExaminationKeyMapping.Diarrhoea.restlessOrIrritable] as? String)?.let {
                            mapStringToBoolean(
                                it
                            )
                        })
                }
                if (diarrhoeaHashMap.containsKey(UnderTwoExaminationKeyMapping.Diarrhoea.sunkenEyes)) {
                    diarrhoea =
                        diarrhoea.copy(sunkenEyes = mapStringToBoolean(diarrhoeaHashMap[UnderTwoExaminationKeyMapping.Diarrhoea.sunkenEyes] as String))
                }
                if (diarrhoeaHashMap.containsKey(UnderTwoExaminationKeyMapping.Diarrhoea.skinPinch)) {
                    diarrhoea =
                        diarrhoea.copy(skinPinch = diarrhoeaHashMap[UnderTwoExaminationKeyMapping.Diarrhoea.skinPinch] as String)
                }
                return diarrhoea
            }
        }
        return null
    }

    private fun mapStringToBoolean(value: String): Boolean {
        return value == DefinedParams.Yes
    }

    fun underTwoMonthsSummaryCreate(
        details: PatientListRespModel,
        submitCreateId: String,
        nextVisitDate: String?,
        selectedPatientStatus: String?,
        submitCreatePatientReference: String
    ) {
        viewModelScope.launch(dispatcherIO) {
            summaryCreateResponse.postLoading()

            val patientId = details.patientId
            val memberId = details.memberId
            val householdId = details.houseHoldId
            val villageId = details.villageId

            if (patientId != null && memberId != null  && villageId != null) {
                val convertedNextVisitDate = DateUtils.convertDateTimeToDate(
                    nextVisitDate,
                    DateUtils.DATE_ddMMyyyy,
                    DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                    inUTC = true
                )

                val response = summaryRepository.createSummarySubmit(
                    patientId = patientId,
                    patientReference = submitCreatePatientReference,
                    memberId = memberId,
                    id = submitCreateId,
                    patientStatus = selectedPatientStatus ?: "",
                    nextVisitDate = convertedNextVisitDate,
                    referralTicketType = MedicalReviewTypeEnums.ICCM.name,
                    assessmentName = MedicalReviewTypeEnums.UNDER_TWO_MONTHS.name,
                    householdId = householdId,
                    villageId = villageId
                )

                summaryCreateResponse.postValue(response)
            }
        }
    }
}