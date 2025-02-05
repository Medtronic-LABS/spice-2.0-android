package com.medtroniclabs.opensource.ui.medicalreview.motherneonate.labourdelivery.viewmodel

import android.content.Context
import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.data.LabourDeliveryMetaEntity
import com.medtroniclabs.opensource.data.MedicalReviewSummarySubmitRequest
import com.medtroniclabs.opensource.data.model.ApgarScoreFiveMinuteDTO
import com.medtroniclabs.opensource.data.model.ApgarScoreOneMinuteDTO
import com.medtroniclabs.opensource.data.model.ApgarScoreTenMinuteDTO
import com.medtroniclabs.opensource.data.model.Child
import com.medtroniclabs.opensource.data.model.ChipViewItemModel
import com.medtroniclabs.opensource.data.model.CreateLabourDeliveryRequest
import com.medtroniclabs.opensource.data.model.CreateLabourDeliveryResponse
import com.medtroniclabs.opensource.data.model.LabourDTO
import com.medtroniclabs.opensource.data.model.MedicalReviewEncounter
import com.medtroniclabs.opensource.data.model.MotherDTO
import com.medtroniclabs.opensource.data.model.NeonateDTO
import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto
import com.medtroniclabs.opensource.data.resource.LabourDeliverySummaryRequest
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.model.PatientListRespModel
import com.medtroniclabs.opensource.model.assessment.AgparScoreFooter
import com.medtroniclabs.opensource.model.assessment.AgparScoreHeader
import com.medtroniclabs.opensource.model.assessment.AgparScoreRow
import com.medtroniclabs.opensource.model.assessment.ApgarScore
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.ui.BaseViewModel
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.labourdelivery.repo.LabourDeliveryRepository
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewDefinedParams
import com.medtroniclabs.opensource.ui.mypatients.enumType.AgparColumnIdentifierType
import com.medtroniclabs.opensource.ui.mypatients.enumType.AgparItemViewType
import com.medtroniclabs.opensource.ui.mypatients.enumType.AgparRowIdentifierType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class LabourDeliveryViewModel @Inject constructor(
    @IoDispatcher override var dispatcherIO: CoroutineDispatcher,
    private var repository: LabourDeliveryRepository
) :  BaseViewModel(dispatcherIO) {


    var context: Context? = null
    var motherPatientStatus: String? = null
    var lastMensurationDate: String? = null
    var timeOfDeliveryMap = HashMap<String, Any>()
    var timeOfLabourOnsetMap = HashMap<String, Any>()
    var perineumStateMap = HashMap<String, Any>()
    var motherSignsAndSymptoms = listOf<ChipViewItemModel>()
    var motherGeneralCondition: String? = null
    var motherRiskFactors = listOf<ChipViewItemModel>()
    var motherTTDosageSoFar: String? = null
    var motherStatus = listOf<ChipViewItemModel>()
    var motherStatusFactors = listOf<ChipViewItemModel>()
    var genderFlow = HashMap<String, Any>()
    var stateOfBaby = HashMap<String, Any>()
    val labourDeliveryMetaLiveData = MutableLiveData<Resource<Boolean>>()
    val labourDeliveryMetaList = MutableLiveData<Resource<List<LabourDeliveryMetaEntity>>>()
    val createLabourDeliveryMedicalReviewResponse =
        MutableLiveData<Resource<CreateLabourDeliveryResponse>>()
    var timeOfDeliveryInHour: String? = null
    var timeOfDeliveryInMinute: String? = null
    var timeOfLabourOnSetInHour: String? = null
    var timeOfLabourOnSetInMinutes: String? = null
    var dateOfDelivery: Triple<Int, Int, Int>? = null
    var dateOfLabourOnset: Triple<Int, Int, Int>? = null
    var deliveryType: String? = null
    var deliveryBy: String? = null
    var deliveryByOthers: String? =null
    var deliveryAt: String? = null
    var deliveryStatus: String? = null
    var noOfNeonates: String? = null
    var neonateOutcome: String? = null
    var neonateBirthWeight: String? = null
    var name:String?=null
    var neonateSignsAndSymptoms = listOf<ChipViewItemModel>()
    var gestationalAge:String?=null
    var encounterID:String? = null
    var deliveryPlaceOthers:String?=null
    private val _apgarScoresLiveData = MutableLiveData<List<ApgarScore>>()
    val apgarScoreLiveData: LiveData<List<ApgarScore>>
        get() = _apgarScoresLiveData

    private val _submitButtonState = MutableLiveData<Boolean>()
    val submitButtonState: LiveData<Boolean>
        get() = _submitButtonState

    var patientId: String? = null
    var lastLocation: Location? = null

    var agparColumnIdentifier: AgparColumnIdentifierType? = null
    var agparRowIdentifier: AgparRowIdentifierType? = null
    var agparSelectedScore: String? = null

    var patientDetailModel: PatientListRespModel? = null
    var isRefresh: Boolean = false
    var nextFollowupDate: String? = null
    val summaryCreateResponse = MutableLiveData<Resource<HashMap<String, Any>>>()

    private val _gestationalDate = MutableLiveData<String>()
    val gestationalDate: LiveData<String> get() = _gestationalDate
    private var summaryCreateRequest: LabourDeliverySummaryRequest? = null

    var isDirectPnc:Boolean=false
    var neonateOutComeStateLiveData= MutableLiveData<List<ChipViewItemModel>>()
    var neonateDataForPncLiveData= MutableLiveData<CreateLabourDeliveryRequest>()


    fun set(context: Context) {
        this.context = context
    }
    fun getAgparScoreData() {
        val apgarScores = mutableListOf<ApgarScore>()
        apgarScores.add(
            ApgarScore(
                viewType = AgparItemViewType.HEADER, AgparScoreHeader(
                    R.string.indicator_header,
                    R.string.one_minute_header,
                    R.string.five_minute_header,
                    R.string.ten_minute_header
                )
            )
        )
        apgarScores.add(
            ApgarScore(
                viewType = AgparItemViewType.ROW, row = AgparScoreRow(
                    indicatorName = R.string.activity_label,
                    indicatorType = AgparRowIdentifierType.ACTIVITY
                )
            )
        )
        apgarScores.add(
            ApgarScore(
                viewType = AgparItemViewType.ROW, row = AgparScoreRow(
                    indicatorName = R.string.pulse_label,
                    indicatorType = AgparRowIdentifierType.PULSE
                )
            )
        )
        apgarScores.add(
            ApgarScore(
                viewType = AgparItemViewType.ROW, row = AgparScoreRow(
                    indicatorName = R.string.grimace_label,
                    indicatorType = AgparRowIdentifierType.GRIMACE
                )
            )
        )
        apgarScores.add(
            ApgarScore(
                viewType = AgparItemViewType.ROW, row = AgparScoreRow(
                    indicatorName = R.string.appearance_label,
                    indicatorType = AgparRowIdentifierType.APPEARANCE
                )
            )
        )
        apgarScores.add(
            ApgarScore(
                viewType = AgparItemViewType.ROW, row = AgparScoreRow(
                    indicatorName = R.string.respiration_label,
                    indicatorType = AgparRowIdentifierType.RESPIRATION
                )
            )
        )

        apgarScores.add(
            ApgarScore(
                viewType = AgparItemViewType.FOOTER, footer = AgparScoreFooter(
                    indicatorName = R.string.total_label
                )
            )
        )

        _apgarScoresLiveData.value = apgarScores
    }

    fun updateAgparScore(
        score: String
    ) {
        _apgarScoresLiveData.value?.toMutableList()?.let { agparScores ->

            val rowPosition =
                agparScores.indexOfFirst { it.row?.indicatorType == agparRowIdentifier }

            when (agparColumnIdentifier) {
                AgparColumnIdentifierType.ONE_MINUTE -> {
                    val newRow = agparScores[rowPosition].row?.copy(oneMinute = score)
                    agparScores[rowPosition] = agparScores[rowPosition].copy(row = newRow)
                }

                AgparColumnIdentifierType.FIVE_MINUTES -> {
                    val newRow = agparScores[rowPosition].row?.copy(fiveMinute = score)
                    agparScores[rowPosition] = agparScores[rowPosition].copy(row = newRow)
                }

                else -> {
                    val newRow = agparScores[rowPosition].row?.copy(tenMinute = score)
                    agparScores[rowPosition] = agparScores[rowPosition].copy(row = newRow)
                }
            }

            var oneMinuteTotal = ""
            var fiveMinuteTotal = ""
            var tenMinuteTotal = ""
            agparScores.filter { it.viewType == AgparItemViewType.ROW }.forEach {
                it.row?.let { row ->
                    oneMinuteTotal += row.oneMinute?.toInt() ?: ""
                    fiveMinuteTotal += row.fiveMinute?.toInt() ?: ""
                    tenMinuteTotal += row.tenMinute?.toInt() ?: ""
                }
            }

            val footerPosition =
                agparScores.indexOfFirst { it.viewType == AgparItemViewType.FOOTER }

            val newFooter = agparScores[footerPosition].footer?.copy(
                oneMinuteTotal = (if (oneMinuteTotal =="") {
                    null
                } else {
                    oneMinuteTotal.map { it.toString().toInt() }.sum().toString()
                }), fiveMinuteTotal =( if (fiveMinuteTotal == "" ) {
                    null
                } else {
                    fiveMinuteTotal.map { it.toString().toInt() }.sum().toString()}),
                tenMinuteTotal = if (tenMinuteTotal == "") {
                    null
                } else {
                    tenMinuteTotal.map { it.toString().toInt() }.sum().toString()
                }
            )
            agparScores[footerPosition] = agparScores[footerPosition].copy(footer = newFooter)

            _apgarScoresLiveData.value = agparScores
        }

    }

    fun getAgparRowName(): Int? {
        return when (agparRowIdentifier) {
            AgparRowIdentifierType.ACTIVITY -> R.string.activity_label
            AgparRowIdentifierType.PULSE -> R.string.pulse_label
            AgparRowIdentifierType.GRIMACE -> R.string.grimace_label
            AgparRowIdentifierType.APPEARANCE -> R.string.appearance_label
            AgparRowIdentifierType.RESPIRATION -> R.string.respiration_label
            else -> null
        }

    }

    fun getAgparColumnName(): Int? {
        return when (agparColumnIdentifier) {
            AgparColumnIdentifierType.ONE_MINUTE -> R.string.one_minute_header
            AgparColumnIdentifierType.FIVE_MINUTES -> R.string.five_minute_header
            AgparColumnIdentifierType.TEN_MINUTES -> R.string.ten_minute_header
            else -> null
        }

    }

    fun getStaticMetaData() {
        viewModelScope.launch(dispatcherIO) {
            labourDeliveryMetaLiveData.postLoading()
            labourDeliveryMetaLiveData.postValue(repository.getStaticMetaData())
        }
    }

    fun createLabourDeliveryRequest(
        prescriptionEncounterId: String?) {
        if (timeOfDeliveryInHour?.toInt()!! <=12 && timeOfDeliveryInMinute?.toInt()!! <=59 && timeOfLabourOnSetInHour?.toInt()!!<=12 &&
            timeOfLabourOnSetInMinutes?.toInt()!!<=59){
            val createLabourMedicalReviewRequest = setLabourDeliveryRequest(prescriptionEncounterId)
            if (isDirectPnc){
                neonateDataForPncLiveData.postValue(createLabourMedicalReviewRequest)
            }else {
                viewModelScope.launch(dispatcherIO) {
                    createLabourDeliveryMedicalReviewResponse.postLoading()
                    createLabourDeliveryMedicalReviewResponse.postValue(
                        repository.createLabourDeliveryMedicalReview(
                            request = createLabourMedicalReviewRequest
                        )
                    )
                }
            }
        }

    }

    fun setLabourDeliveryRequest(prescriptionEncounterId: String?):CreateLabourDeliveryRequest{
        val encounter = MedicalReviewEncounter(
            latitude = lastLocation?.latitude ?: 0.0,
            longitude = lastLocation?.longitude ?: 0.0,
            referred = true,
            startTime = DateUtils.getCurrentDateAndTime(
                DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ
            ),
            endTime = DateUtils.getCurrentDateAndTime(
                DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ
            ),
            householdId = patientDetailModel?.houseHoldId,
            patientId = patientId,
            provenance = ProvanceDto(),
            memberId = patientDetailModel?.memberId.toString()
        )
        val encounterChild = MedicalReviewEncounter(
            latitude = lastLocation?.latitude ?: 0.0,
            longitude = lastLocation?.longitude ?: 0.0,
            referred = true,
            startTime = DateUtils.getCurrentDateAndTime(
                DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ
            ),
            endTime = DateUtils.getCurrentDateAndTime(
                DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ
            ),
            householdId = patientDetailModel?.houseHoldId,
            provenance = ProvanceDto()
        )
        val motherModel = createMotherModel(encounter, prescriptionEncounterId)
        val neonateModel = createNeonateModel(encounterChild)
        val childModel = createChildModel(ProvanceDto())
        val createLabourMedicalReviewRequest = CreateLabourDeliveryRequest(
            motherDTO = motherModel,
            neonateDTO = if (neonateModel.neonateOutcome?.contains(MedicalReviewDefinedParams.MaceratedStillBirth)==true || neonateModel.neonateOutcome?.contains(MedicalReviewDefinedParams.FreshStillBirth)== true|| neonateModel.neonateOutcome?.contains(MedicalReviewDefinedParams.StillBirth)== true|| neonateModel.neonateOutcome?.contains(MedicalReviewDefinedParams.Miscarriage)== true) null else neonateModel,
            child = if (neonateModel.neonateOutcome?.contains(MedicalReviewDefinedParams.MaceratedStillBirth)==true || neonateModel.neonateOutcome?.contains(MedicalReviewDefinedParams.FreshStillBirth)== true|| neonateModel.neonateOutcome?.contains(MedicalReviewDefinedParams.StillBirth)== true|| neonateModel.neonateOutcome?.contains(MedicalReviewDefinedParams.Miscarriage)== true) null else childModel
        )
        return createLabourMedicalReviewRequest
    }

    private fun createChildModel(provanceDto: ProvanceDto): Child {
        val motherName = patientDetailModel?.name
        val childName =
            if (name != null) {
                name
            } else {
                if (motherName != null) {
                    "${DefinedParams.NeonateBabyNamePrefix} ${motherName}"
                } else null
            }
        val village = patientDetailModel?.village.takeIf { it != null }
        val villageId = patientDetailModel?.villageId.takeIf { it != null }

        val genderValue = genderFlow[DefinedParams.Gender]?.toString()?.takeIf { it != null }

        return Child(
            name = childName,
            village = village,
            villageId = villageId?.toInt(),
            motherPatientId = patientId,
            dateOfBirth = getTimeOfDelivery(),
            patientId = null,
            isChild = true,
            gender =  genderValue,
            provenance = provanceDto,
            householdId = patientDetailModel?.houseHoldId,
            phoneNumber = patientDetailModel?.phoneNumber,
            householdHeadRelationship = patientDetailModel?.relationship,
            phoneNumberCategory = patientDetailModel?.phoneNumberCategory
        )
    }

    private fun createNeonateModel(encounter: MedicalReviewEncounter): NeonateDTO {
        val apgarScoreOneMinute = createOneMinuteApgarScore()
        val apgarScoreFiveMinute = createFiveMinuteApgarScore()
        val apgarScoreTenMinute = createTenMinuteApgarScore()
        val genderValue = genderFlow[DefinedParams.Gender]?.toString()?.takeIf { it != "null" }

        return NeonateDTO(neonateOutcome = neonateOutcome.takeIf { it != null },
            gender = genderValue,
            birthWeight = neonateBirthWeight.takeIf { it != null },
            stateOfBaby = stateOfBaby[DefinedParams.StateOfBaby] as? String,
            signs = neonateSignsAndSymptoms.map { it.value.toString() }.takeIf { it.isNotEmpty() },
            encounter = encounter,
            gestationalAge = gestationalAge,
            apgarScoreOneMinuteDTO = apgarScoreOneMinute.takeIf { it != null },
            apgarScoreFiveMinuteDTO = apgarScoreFiveMinute.takeIf { it != null },
            apgarScoreTenMinuteDTO = apgarScoreTenMinute.takeIf { it != null }
        )
    }

    private fun createTenMinuteApgarScore(): ApgarScoreTenMinuteDTO? {
        val apgarScores = _apgarScoresLiveData.value ?: return null
        val tenMinuteScores =
            apgarScores.filter { it.viewType == AgparItemViewType.ROW }.map { it.row?.tenMinute }
        val tenMinuteTotal = apgarScores.filter { it.viewType == AgparItemViewType.FOOTER }
            .map { it.footer?.tenMinuteTotal }

        if (tenMinuteScores.isEmpty() || tenMinuteScores.all { it == null }) {
            return null
        }

        return ApgarScoreTenMinuteDTO(
            activity = tenMinuteScores[0]?.toInt(),
            pulse = tenMinuteScores[1]?.toInt(),
            grimace = tenMinuteScores[2]?.toInt(),
            appearance = tenMinuteScores[3]?.toInt(),
            respiration = tenMinuteScores[4]?.toInt(),
            tenMinuteTotalScore = tenMinuteTotal[0]?.toInt()
        )
    }

    private fun createFiveMinuteApgarScore(): ApgarScoreFiveMinuteDTO? {
        val apgarScores = _apgarScoresLiveData.value ?: return null
        val fiveMinuteScores =
            apgarScores.filter { it.viewType == AgparItemViewType.ROW }.map { it.row?.fiveMinute }
        val fiveMinuteTotal = apgarScores.filter { it.viewType == AgparItemViewType.FOOTER }
            .map { it.footer?.fiveMinuteTotal }

        if (fiveMinuteScores.isEmpty() || fiveMinuteScores.all { it == null }) {
            return null
        }

        return ApgarScoreFiveMinuteDTO(
            activity = fiveMinuteScores[0]?.toInt(),
            pulse = fiveMinuteScores[1]?.toInt(),
            grimace = fiveMinuteScores[2]?.toInt(),
            appearance = fiveMinuteScores[3]?.toInt(),
            respiration = fiveMinuteScores[4]?.toInt(),
            fiveMinuteTotalScore = fiveMinuteTotal[0]?.toInt()
        )
    }

    private fun createOneMinuteApgarScore(): ApgarScoreOneMinuteDTO? {
        val apgarScores = _apgarScoresLiveData.value ?: return null
        val oneMinuteScores =
            apgarScores.filter { it.viewType == AgparItemViewType.ROW }.map { it.row?.oneMinute }
        val oneMinuteTotal = apgarScores.filter { it.viewType == AgparItemViewType.FOOTER }
            .map { it.footer?.oneMinuteTotal }

        if (oneMinuteScores.isEmpty() || oneMinuteScores.all { it == null }) {
            return null
        }

        return ApgarScoreOneMinuteDTO(
            activity = oneMinuteScores[0]?.toInt(),
            pulse = oneMinuteScores[1]?.toInt(),
            grimace = oneMinuteScores[2]?.toInt(),
            appearance = oneMinuteScores[3]?.toInt(),
            respiration = oneMinuteScores[4]?.toInt(),
            oneMinuteTotalScore = oneMinuteTotal[0]?.toInt()
        )
    }

    private fun createMotherModel(
        encounter: MedicalReviewEncounter,
        prescriptionEncounterId: String?
    ): MotherDTO {
        encounterID=prescriptionEncounterId
        encounter.id = prescriptionEncounterId
        val motherTTDosage = motherTTDosageSoFar
        val labourDTO = createLabourDeliveryModel()
        return MotherDTO(id = prescriptionEncounterId,
            signs = motherSignsAndSymptoms.map { it.value.toString() }.takeIf { it.isNotEmpty() },
            generalConditions = motherGeneralCondition.takeIf { it != null },
            riskFactors = motherRiskFactors.map { it.value.toString() }.takeIf { it.isNotEmpty() },
            stateOfPerineum = perineumStateMap[DefinedParams.StateOfPerineum] as? String,
            ttDoseTaken = if (motherTTDosage.isNullOrEmpty()) null else motherTTDosage.toInt(),
            status = motherStatus.map { it.value.toString() }.takeIf { it.isNotEmpty() },
            tear = perineumStateMap[DefinedParams.Tear] as? String,
            encounter = encounter,
            labourDTO = labourDTO,
            neonateOutcome = neonateOutcome.takeIf { it != null }
        )
    }

    private fun createLabourDeliveryModel(): LabourDTO {
        return LabourDTO(
            deliveryAt = deliveryAt,
            deliveryBy = deliveryBy,
            deliveryStatus = deliveryStatus,
            deliveryType = deliveryType,
            deliveryByOther = deliveryByOthers,
            noOfNeoNates = noOfNeonates?.toInt(),
            dateAndTimeOfDelivery = getTimeOfDelivery(),
            dateAndTimeOfLabourOnset = getTimeOfLabourOnset(),
            deliveryAtOther = deliveryPlaceOthers
        )
    }

    private fun getTimeOfDelivery(): String? {
        val dateOfDelivery = this.dateOfDelivery ?: return null
        val timeOfDeliveryInHourInt = timeOfDeliveryInHour?.toInt() ?: 0
        val timeOfDeliveryInMinutesInt = timeOfDeliveryInMinute?.toInt() ?: 0
        val timeOfDeliveryMap = this.timeOfDeliveryMap[DefinedParams.TimeOfDelivery] as String
        val year = dateOfDelivery.first
        val month = dateOfDelivery.second
        val day = dateOfDelivery.third

        val hour = timeOfDeliveryInHourInt.toInt()
        val minute = timeOfDeliveryInMinutesInt.toInt()

        val adjustedHour = when (timeOfDeliveryMap) {
            context?.getString(R.string.pm) -> if (hour == 12) hour else hour + 12
            context?.getString(R.string.am) -> if (hour == 12) 0 else hour
            else -> hour
        }

        val localDate = LocalDate.of(year.toInt(), month.toInt(), day.toInt())
        val localTime = LocalTime.of(adjustedHour, minute)

        val localDateTime = LocalDateTime.of(localDate, localTime)

        val formatter = DateTimeFormatter.ofPattern(DateUtils.DATE_FORMAT_yyyyMMddHHmmss)
        val formattedDateTime = localDateTime.format(formatter)


        return formattedDateTime

    }

    private fun getTimeOfLabourOnset(): String? {
        val dateOfLabourOnset = this.dateOfLabourOnset ?: return null
        val timeOfLabourOnSetInHourInt = timeOfLabourOnSetInHour?.toInt() ?: 0
        val timeOfLabourOnSetInMinuteInt = timeOfLabourOnSetInMinutes?.toInt() ?: 0
        val timeOfLabourOnSetMap =
            this.timeOfLabourOnsetMap[DefinedParams.TimeOfLabourOnset] as String
        val year = dateOfLabourOnset.first
        val month = dateOfLabourOnset.second
        val day = dateOfLabourOnset.third

        // Convert hour and minute to integers
        val hour = timeOfLabourOnSetInHourInt.toInt()
        val minute = timeOfLabourOnSetInMinuteInt.toInt()

        // Adjust hour for AM/PM
        val adjustedHour = when (timeOfLabourOnSetMap) {
            context?.getString(R.string.pm) -> if (hour == 12) hour else hour + 12
            context?.getString(R.string.am) -> if (hour == 12) 0 else hour
            else -> hour
        }

        // Create LocalDate and LocalTime
        val localDate = LocalDate.of(year.toInt(), month.toInt(), day.toInt())
        val localTime = LocalTime.of(adjustedHour, minute)

        // Combine LocalDate and LocalTime into LocalDateTime
        val localDateTime = LocalDateTime.of(localDate, localTime)

        // Format LocalDateTime
        val formatter = DateTimeFormatter.ofPattern(DateUtils.DATE_FORMAT_yyyyMMddHHmmss)
        val formattedDateTime = localDateTime.format(formatter)

        // Output the formatted string
        return formattedDateTime
    }


    fun getLabourDeliveryMetaList() {
        viewModelScope.launch(dispatcherIO) {
            labourDeliveryMetaList.postLoading()
            labourDeliveryMetaList.postValue(repository.getLabourDeliveryList())
        }
    }

    fun validateSubmitButtonState() {
        val apgarScore = _apgarScoresLiveData.value
            ?.filter { it.viewType == AgparItemViewType.ROW }
            ?.any { it.row?.oneMinute != null || it.row?.fiveMinute != null || it.row?.tenMinute != null }
        _submitButtonState.value = (timeOfDeliveryMap.isNotEmpty()
                || timeOfLabourOnsetMap.isNotEmpty()
                || dateOfDelivery != null
                || name!=null
                || dateOfLabourOnset != null
                || timeOfDeliveryInHour != null
                || timeOfDeliveryInMinute != null
                || timeOfLabourOnSetInHour != null
                || timeOfLabourOnSetInMinutes != null
                || deliveryType != null
                || deliveryBy != null
                || deliveryAt != null
                || deliveryStatus != null
                || noOfNeonates != null
                || motherSignsAndSymptoms.isNotEmpty()
                || motherGeneralCondition != null
                || motherRiskFactors.isNotEmpty()
                || perineumStateMap.isNotEmpty()
                || motherTTDosageSoFar != null
                || motherStatus.isNotEmpty()
                || neonateOutcome != null
                || genderFlow.isNotEmpty()
                || neonateBirthWeight != null
                || stateOfBaby.isNotEmpty()
                || neonateSignsAndSymptoms.isNotEmpty()
                || apgarScore == true)

    }

    fun setDate(year: Int, month: Int, dayOfMonth: Int) {
        val localDate = LocalDate.of(year, month, dayOfMonth)
        val zonedDateTime = localDate.atStartOfDay(ZoneOffset.UTC)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
        val formattedDate = zonedDateTime.format(formatter)
        _gestationalDate.value = formattedDate
    }

    fun labourDeliverySummaryCreate(
        request: MedicalReviewSummarySubmitRequest
    ) {
        viewModelScope.launch(dispatcherIO) {
            summaryCreateResponse.postLoading()
            summaryCreateResponse.postValue(repository.labourDeliverySummaryCreate(request))
        }
    }
}