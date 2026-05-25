package org.medtroniclabs.uhis.ui.boarding.repo

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.medtroniclabs.uhis.BuildConfig
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.ConsentFormType
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.RoleConstant.PROVIDER
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.ClinicalWorkflow
import org.medtroniclabs.uhis.data.ConsentFormResponse
import org.medtroniclabs.uhis.data.CulturesEntity
import org.medtroniclabs.uhis.data.FormData
import org.medtroniclabs.uhis.data.FormMetaRequest
import org.medtroniclabs.uhis.data.FormRequest
import org.medtroniclabs.uhis.data.FormResponse
import org.medtroniclabs.uhis.data.HealthFacility
import org.medtroniclabs.uhis.data.MedicalReviewMetaItems
import org.medtroniclabs.uhis.data.Menu
import org.medtroniclabs.uhis.data.MenuDetail
import org.medtroniclabs.uhis.data.MetaDataResponse
import org.medtroniclabs.uhis.data.ModelQuestion
import org.medtroniclabs.uhis.data.ProgramEntity
import org.medtroniclabs.uhis.data.UserProfile
import org.medtroniclabs.uhis.data.model.ShasthyaKormi
import org.medtroniclabs.uhis.data.model.ShasthyaShebika
import org.medtroniclabs.uhis.data.model.SubVillage
import org.medtroniclabs.uhis.data.registration.SiteResponse
import org.medtroniclabs.uhis.db.entity.ClinicalWorkflowConditionEntity
import org.medtroniclabs.uhis.db.entity.ClinicalWorkflowEntity
import org.medtroniclabs.uhis.db.entity.ConsentEntity
import org.medtroniclabs.uhis.db.entity.ConsentForm
import org.medtroniclabs.uhis.db.entity.FormEntity
import org.medtroniclabs.uhis.db.entity.HealthFacilityEntity
import org.medtroniclabs.uhis.db.entity.LinkedVillageEntity
import org.medtroniclabs.uhis.db.entity.MemberAssessmentHistoryEntity
import org.medtroniclabs.uhis.db.entity.MentalHealthEntity
import org.medtroniclabs.uhis.db.entity.MenuEntity
import org.medtroniclabs.uhis.db.entity.NCDAssessmentClinicalWorkflow
import org.medtroniclabs.uhis.db.entity.PregnancyDetail
import org.medtroniclabs.uhis.db.entity.RiskClassificationModel
import org.medtroniclabs.uhis.db.entity.RiskFactorEntity
import org.medtroniclabs.uhis.db.entity.ShasthyaKormiEntity
import org.medtroniclabs.uhis.db.entity.ShasthyaKormiLinkedVillageEntity
import org.medtroniclabs.uhis.db.entity.ShasthyaShebikaEntity
import org.medtroniclabs.uhis.db.entity.ShasthyaShebikaLinkedVillageEntity
import org.medtroniclabs.uhis.db.entity.SignsAndSymptomsEntity
import org.medtroniclabs.uhis.db.entity.SiteEntity
import org.medtroniclabs.uhis.db.entity.SubVillageEntity
import org.medtroniclabs.uhis.db.entity.UserProfileEntity
import org.medtroniclabs.uhis.db.entity.VillageEntity
import org.medtroniclabs.uhis.db.local.RoomHelper
import org.medtroniclabs.uhis.mappingkey.PregnantWomen
import org.medtroniclabs.uhis.mappingkey.Screening
import org.medtroniclabs.uhis.model.CultureLocaleModel
import org.medtroniclabs.uhis.ncd.data.DeviceDetails
import org.medtroniclabs.uhis.ncd.data.NCDPatientTransferNotificationCountRequest
import org.medtroniclabs.uhis.ncd.data.NCDPatientTransferNotificationCountResponse
import org.medtroniclabs.uhis.ncd.data.NCDPatientTransferUpdateRequest
import org.medtroniclabs.uhis.ncd.data.NCDSupportRequest
import org.medtroniclabs.uhis.ncd.data.PatientTransferListResponse
import org.medtroniclabs.uhis.ncd.data.PeerSupervisorNotificationRequest
import org.medtroniclabs.uhis.ncd.data.PeerSupervisorNotificationResponse
import org.medtroniclabs.uhis.ncd.data.TermsAndConditionsModel
import org.medtroniclabs.uhis.network.ApiHelper
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.MenuConstants
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.FamilyPlanningMethods
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH
import org.medtroniclabs.uhis.ui.boarding.ResourceLoadingSyncProgress
import org.medtroniclabs.uhis.ui.medicalreview.motherneonate.anc.MotherNeonateUtil
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewTypeEnums
import org.medtroniclabs.uhis.ui.patient.UIConstants.ENROLLMENT_UNIQUE_ID
import org.medtroniclabs.uhis.ui.patient.UIConstants.SCREENING_UNIQUE_ID
import java.lang.reflect.Type
import java.util.Locale
import javax.inject.Inject

class MetaRepository @Inject constructor(
    private var apiHelper: ApiHelper,
    private var roomHelper: RoomHelper,
) {
    suspend fun updateDeviceDetails(deviceDetails: DeviceDetails): Resource<DeviceDetails> =
        try {
            val response = apiHelper.updateDeviceDetails(deviceDetails)
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody?.status == true) {
                    Resource(state = ResourceState.SUCCESS)
                } else {
                    Resource(state = ResourceState.ERROR)
                }
            } else {
                Resource(state = ResourceState.ERROR)
            }
        } catch (_: Exception) {
            Resource(state = ResourceState.ERROR)
        }

    /**
     * Checks whether the installed app version is still allowed by the backend.
     *
     * Result semantics:
     *  - SUCCESS + optionalData == true  -> Update REQUIRED (data carries the server message, if any).
     *  - SUCCESS + optionalData == false -> Up-to-date.
     *  - ERROR                           -> Check failed (network/parse). Callers should fail open.
     */
    suspend fun checkAppVersion(): Resource<String> =
        try {
            val response = apiHelper.checkAppVersion()
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.entity == false) {
                    Resource(
                        state = ResourceState.SUCCESS,
                        data = body.message,
                        optionalData = true,
                    )
                } else {
                    Resource(state = ResourceState.SUCCESS, optionalData = false)
                }
            } else {
                Resource(state = ResourceState.ERROR)
            }
        } catch (_: Exception) {
            Resource(state = ResourceState.ERROR)
        }

    suspend fun getMetaDataInformation(
        workflowNames: MutableList<Long>,
        meta: MutableList<String>,
        changeFacility: Boolean,
        onProgress: ((Int) -> Unit)? = null,
    ): Resource<Boolean> {
        return try {
            withContext(Dispatchers.IO) {
                val response = async { apiHelper.getMetaDataInformation() }.await()
                BuildConfig.SALT
                if (response.isSuccessful && response.body()?.status == true) {
                    with(roomHelper) {
                        response.body()?.entity?.apply {
                            saveHealthFacilityInDb(
                                nearestHealthFacilities,
                                defaultHealthFacility.id,
                                userHealthFacilities,
                            )
                            saveUserLinkedVillages(userHealthFacilities)
                            SecuredPreference.putString(
                                SecuredPreference.EnvironmentKey.DEFAULT_SITE_ID.name,
                                defaultHealthFacility.fhirId,
                            )

                            chiefdoms?.let { chiefdomList ->
                                roomHelper.deleteChiefDoms()
                                roomHelper.saveChiefDoms(chiefdomList)
                            }

                            deleteAllVillages()
                            saveVillage(modifiedVillages(villages, userProfile.villages))
                            if (CommonUtils.isNurse()) {
                                // Save SubVillages which are linked to Nurse
                                saveSubVillages(subVillages)
                            } else {
                                // Save SubVillages which are linked to shasthya shebikas
                                saveSubVillages(shasthyaShebikas?.flatMap { it.subVillages ?: emptyList() })
                            }
                            // Save ShasthyaShebikas (includes saving linked subVillages)
                            saveShasthyaShebikas(shasthyaShebikas)
                            saveShasthyaKormisFromMeta(shasthyaKormis)
                            deleteAllFrequencyList()
                            frequency?.let {
                                saveFrequencyList(it)
                            }
                            saveConsentForm(consentForm)
                            saveFhirId(userProfile.fhirId, defaultHealthFacility, changeFacility)
                            saveUserProfileDetailsInDb(userProfile)
                            appTypes?.forEach {
                                when (it) {
                                    DefinedParams.COMMUNITY -> SecuredPreference.putBoolean(
                                        SecuredPreference.EnvironmentKey.IS_COMMUNITY.name,
                                        true,
                                    )

                                    DefinedParams.NON_COMMUNITY -> SecuredPreference.putBoolean(
                                        SecuredPreference.EnvironmentKey.IS_NON_COMMUNITY.name,
                                        true,
                                    )
                                }
                            }
                            roomHelper.deleteAllMenus()
                            if (CommonUtils.isRolePresent()) {
                                saveClinicalWorkflowsForProvider(defaultHealthFacility.clinicalWorkflows)
                                if (CommonUtils.isNonCommunity()) {
                                    handleMeta(menu, meta)
                                }
                            } else {
                                handleMeta(menu, meta)
                            }
                            workflowNames.addAll(workflowIds)
                            savePregnancyAncStatus(userHealthFacilities?.find { it.id == SecuredPreference.getOrganizationId() })
                            identityTypes?.let { types ->
                                SecuredPreference.putIdentityTypes(types)
                            }
                            districts?.let { districtList ->
                                roomHelper.deleteDistricts()
                                roomHelper.saveDistricts(districtList)
                            }

                            programs?.let {
                                roomHelper.deletePrograms()
                                val list = ArrayList<ProgramEntity>()
                                programs.forEach { item ->
                                    list.add(
                                        ProgramEntity(
                                            id = item.id,
                                            name = item.name,
                                            healthFacilityIds = fetchIds(item.healthFacilities),
                                        ),
                                    )
                                }
                                roomHelper.savePrograms(list)
                            }
                            cultures?.let { cultureList ->
                                roomHelper.deleteCultures()
                                roomHelper.saveCultures(cultureList)
                                updateCulturesMeta(cultureList)
                            }
                            remainingAttemptsCount?.let { remAttempts ->
                                SecuredPreference.putInt(
                                    SecuredPreference.EnvironmentKey.REMAINING_ATTEMPTS_COUNT.name,
                                    remAttempts,
                                )
                            }
                            medicationInstructions?.let { items ->
                                deleteExaminationsComplaints(MedicalReviewTypeEnums.PRESCRIPTION_INSTRUCTION.name)
                                insertExaminationsComplaint(insertPrescriptionInstruction(items))
                            }
                        }
                        onProgress?.invoke(ResourceLoadingSyncProgress.USER_DATA_COMPLETE)

                        val formsResponse = async {
                            apiHelper.getForms(
                                FormRequest(
                                    nonNcdWorkflowEnabled = CommonUtils.isCommunity(),
                                    workflowNames,
                                ),
                            )
                        }.await()
                        if (formsResponse.isSuccessful && formsResponse.body()?.status == true) {
                            if (formsResponse.body()?.entity == null) {
                                return@with Resource(state = ResourceState.ERROR)
                            }
                            formsResponse.body()?.entity?.apply {
                                if (CommonUtils.isCommunity()) {
                                    formData?.let {
                                        saveFormsInDb(it)
                                    } ?: run {
                                        return@with Resource(state = ResourceState.ERROR)
                                    }

                                    enrollment?.let {
                                        roomHelper.insertConsentForm(
                                            ConsentForm(
                                                type = ENROLLMENT_UNIQUE_ID,
                                                content = it.consentForm,
                                            ),
                                        )
                                    }

                                    screening?.let {
                                        roomHelper.insertConsentForm(
                                            ConsentForm(
                                                type = SCREENING_UNIQUE_ID,
                                                content = it.consentForm,
                                            ),
                                        )
                                    }
                                }
                                if (CommonUtils.isNonCommunity()) {
                                    saveNcdFormsInDb(this)
                                    saveNcdModelQuestions(modelQuestions)
                                }
                                clinicalTools?.let {
                                    saveClinicalWorkflowsInDb(it)
                                } ?: run {
                                    return@with Resource(state = ResourceState.ERROR)
                                }
                            }
                            onProgress?.invoke(ResourceLoadingSyncProgress.FORMS_COMPLETE)
                        } else {
                            return@with Resource(state = ResourceState.ERROR)
                        }
                        if (!SecuredPreference.getBoolean(SecuredPreference.EnvironmentKey.IS_TB_LOADED.name)) {
                            val tbResponse = async {
                                apiHelper.getTbStaticData()
                            }.await()
                            if (tbResponse.isSuccessful && tbResponse.body()?.status == true) {
                                if (tbResponse.body()?.entity == null) {
                                    return@with Resource(state = ResourceState.ERROR)
                                }
                                tbResponse.body()?.entity?.let { data ->
                                    roomHelper.apply {
                                        deleteExaminationsComplaintsForAnc(MedicalReviewTypeEnums.TB.name)
                                        insertExaminationsComplaint(
                                            generateChipItemByType(
                                                data.presentingComplaints,
                                                data.systemicExaminations,
                                                data.comorbidities,
                                                data.patientStatus,
                                                data.patientType,
                                                data.treatmentOutcome,
                                            ),
                                        )
                                        roomHelper.deleteDiagnosisList(MedicalReviewTypeEnums.TB.name)
                                        roomHelper.deleteDiagnosisList(MotherNeonateUtil.TB_SITE_OF_DISEASE)
                                        roomHelper.deleteDiagnosisList(MotherNeonateUtil.TB_ORGAN_AFFECTED)
                                        roomHelper.saveDiagnosisList(data.diseaseCategories)
                                        SecuredPreference.putBoolean(
                                            SecuredPreference.EnvironmentKey.IS_TB_LOADED.name,
                                            true,
                                        )
                                    }
                                } ?: run {
                                    return@with Resource(state = ResourceState.ERROR)
                                }
                            } else {
                                return@with Resource(state = ResourceState.ERROR)
                            }
                        }
                        onProgress?.invoke(ResourceLoadingSyncProgress.TB_SEGMENT_COMPLETE)
                        if (meta.isNotEmpty()) {
                            val metadataResponse =
                                async { apiHelper.getFormMetadata(FormMetaRequest(meta)) }.await()
                            if (metadataResponse.isSuccessful && metadataResponse.body()?.status == true) {
                                if (metadataResponse.body()?.entity == null) {
                                    return@with Resource(state = ResourceState.ERROR)
                                }
                                metadataResponse.body()?.entity?.let { res ->
                                    res.symptoms.let { symptoms ->
                                        roomHelper.deleteAllSymptoms()
                                        val maxId = symptoms.maxBy { symptom -> symptom._id }._id
                                        symptoms.filter { symptom -> symptom.type != FamilyPlanningMethods }.let {
                                            roomHelper.insertSymptoms(it)
                                        }
                                        symptoms
                                            .filter { symptom -> symptom.type.equals(FamilyPlanningMethods, true) }
                                            .sortedBy { symptom -> symptom.displayOrder }
                                            .let { list ->
                                                roomHelper.insertSymptoms(
                                                    convertFamilyPlanningMethodToSymptoms(
                                                        list,
                                                        maxId,
                                                    ),
                                                )
                                            }
                                    }
                                    res.medicalCompliances?.let {
                                        roomHelper.deleteMedicalCompliance()
                                        roomHelper.saveMedicalCompliance(it)
                                    }

                                    res.units?.let {
                                        roomHelper.deleteUnitMetric()
                                        roomHelper.saveUnitMetric(it)
                                    }

                                    res.dosageFrequencies?.let {
                                        roomHelper.deleteDosageFrequencyList()
                                        roomHelper.saveDosageFrequencyList(it)
                                    }

                                    res.diagnosis?.let {
                                        roomHelper.deleteDiagnosisList()
                                        roomHelper.saveDiagnosis(it)
                                    }

                                    res.reasons?.let {
                                        roomHelper.deleteNCDShortageReason()
                                        roomHelper.saveNCDShortageReason(it)
                                    }

                                    res.cvdRiskAlgorithms?.nonLab?.let { nonLab ->
                                        val baseType: Type =
                                            object :
                                                TypeToken<ArrayList<RiskClassificationModel>>() {}.type
                                        val resultString = Gson().toJson(
                                            nonLab,
                                            baseType,
                                        )
                                        roomHelper.run {
                                            deleteRiskFactor()
                                            insertRiskFactor(
                                                RiskFactorEntity(
                                                    nonLabEntity = resultString,
                                                ),
                                            )
                                        }
                                    }
                                }
                            } else {
                                return@with Resource(state = ResourceState.ERROR)
                            }
                        } else {
                            roomHelper.deleteAllSymptoms()
                        }
                        onProgress?.invoke(ResourceLoadingSyncProgress.FORM_METADATA_COMPLETE)
                        if (CommonUtils.isNonCommunity()) {
                            val userTermsAndConditionsMeta = async {
                                apiHelper.getUserTermsAndConditions(
                                    TermsAndConditionsModel(countryId = SecuredPreference.getCountryId()),
                                )
                            }.await()
                            if (userTermsAndConditionsMeta.isSuccessful && userTermsAndConditionsMeta.body()?.status == true) {
                                if (userTermsAndConditionsMeta.body()?.entity == null) {
                                    return@with Resource(state = ResourceState.ERROR)
                                }
                                userTermsAndConditionsMeta.body()?.entity?.let { res ->
                                    if (res.id != null && !res.formInput.isNullOrBlank()) {
                                        roomHelper.saveConsent(
                                            ConsentEntity(
                                                formType = DefinedParams.Landing,
                                                formInput = res.formInput,
                                            ),
                                        )
                                    } else {
                                        return@with Resource(state = ResourceState.ERROR)
                                    }
                                }
                            } else {
                                return@with Resource(state = ResourceState.ERROR)
                            }
                        }
                        onProgress?.invoke(ResourceLoadingSyncProgress.METADATA_PHASE_COMPLETE)
                        Resource(state = ResourceState.SUCCESS)
                    }
                } else {
                    Resource(state = ResourceState.ERROR)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Resource(state = ResourceState.ERROR)
        }
    }

    private fun updateSiteResponse(
        fromOs: SiteResponse?,
        siteResponse: SiteResponse,
        list: ArrayList<SiteEntity>,
        name: String,
        responseBody: MetaDataResponse,
    ) {
        if (siteResponse.roleName.isNotEmpty() &&
            (siteResponse.roleDisplayName?.isNotEmpty() == true)
        ) {
            SecuredPreference.getUserId()?.let { userId ->
                list.add(
                    SiteEntity(
                        id = siteResponse._id,
                        name = name,
                        userSite = true,
                        role = siteResponse.roleName[0],
                        roleName = siteResponse.roleDisplayName[0],
                        userId = userId,
                        tenantId = siteResponse.tenantId,
                        countyId = siteResponse.countyId,
                        accountId = siteResponse.accountId,
                        subCountyId = siteResponse.subCountyId,
                        isDefault = siteResponse._id == responseBody.defaultHealthFacility.id,
                        isQualipharmEnabledSite = siteResponse.isQualipharmEnabledSite,
                        siteLevel = siteResponse.siteLevel,
                        bpLog = fromOs?.workflows?.bpLog == true,
                        glucoseLog = fromOs?.workflows?.glucoseLog == true,
                        phq4 = fromOs?.workflows?.phq4 == true,
                        cataract = fromOs?.workflows?.cataract == true,
                        eyeCare = fromOs?.workflows?.eyeCare == true,
                        countyName = siteResponse.countyName,
                        subCountyName = siteResponse.subCountyName,
                        countryName = siteResponse.countryName,
                        code = siteResponse.code,
                    ),
                )
            }
        }
    }

    private fun insertPrescriptionInstruction(items: List<String>): List<MedicalReviewMetaItems> {
        var itemId = 1L
        val resultList = ArrayList<MedicalReviewMetaItems>()
        items.forEach { value ->
            resultList.add(
                MedicalReviewMetaItems(
                    itemId = itemId,
                    id = itemId,
                    name = value,
                    category = MedicalReviewTypeEnums.PRESCRIPTION_INSTRUCTION.name,
                    type = MedicalReviewTypeEnums.PRESCRIPTION_INSTRUCTION.name,
                    displayOrder = itemId.toInt(),
                    value = value,
                ),
            )
            itemId++
        }

        return resultList
    }

    private fun updateCulturesMeta(cultureList: ArrayList<CulturesEntity>) {
        val currentLocaleId = SecuredPreference.getCultureId()
        for (i in 0 until cultureList.size) {
            if (currentLocaleId <= 0) {
                if (cultureList[i].name.contains(DefinedParams.EN_LOCALE, ignoreCase = true)) {
                    SecuredPreference.setUserPreference(
                        cultureList[i].id,
                        cultureList[i].name,
                        false,
                    )
                    break
                }
            } else if (cultureList[i].id == currentLocaleId) {
                val isEnabled = CommonUtils.checkIfTranslationEnabled(cultureList[i].name)
                SecuredPreference.setUserPreference(
                    cultureList[i].id,
                    cultureList[i].name,
                    isEnabled,
                )
                break
            }
        }
    }

    private fun savePregnancyAncStatus(currentHealthFacility: HealthFacility?) {
        currentHealthFacility?.let { facility ->
            SecuredPreference.putBoolean(
                SecuredPreference.EnvironmentKey.PREGNANCY_ANC_ENABLED_SITE.name,
                facility.clinicalWorkflows.find {
                    it.workflowName.equals(
                        DefinedParams.PregnancyANC,
                        true,
                    )
                } != null,
            )
        }
    }

    private fun modifiedVillages(
        allVillages: List<VillageEntity>?,
        userVillages: List<VillageEntity>?,
    ): List<VillageEntity> {
        allVillages?.forEach { root ->
            root.isUserVillage = userVillages?.firstOrNull { it.id == root.id } != null
        }
        if (allVillages != null && allVillages.isNotEmpty()) {
            val storedVillages = SecuredPreference.getLongList(SecuredPreference.EnvironmentKey.LINKED_VILLAGE_IDS.name)
            val newVillages = allVillages.filter { it.isUserVillage }.map { it.id }
            val hasChanges = storedVillages.isEmpty() || storedVillages.toSet() != newVillages.toSet()
            SecuredPreference.putBoolean(SecuredPreference.EnvironmentKey.LINKED_VILLAGE_IDS_ALTER.name, hasChanges)
            if (hasChanges) {
                // Update the stored list only if there are changes
                SecuredPreference.remove(SecuredPreference.EnvironmentKey.NCD_FOLLOW_UP_LAST_SYNCED.name)
                SecuredPreference.saveLongList(SecuredPreference.EnvironmentKey.LINKED_VILLAGE_IDS.name, newVillages)
            }
        }

        if (allVillages != null) {
            return allVillages
        } else {
            return emptyList()
        }
    }

    private suspend fun saveUserLinkedVillages(userHealthFacilities: List<HealthFacility>?) {
        val linkedVillages = mutableListOf<LinkedVillageEntity>()
        userHealthFacilities?.let { facilities ->
            facilities.forEach { facility ->
                val list = facility.linkedVillages
                    .map {
                        LinkedVillageEntity(
                            villageId = it.id,
                            tenantId = facility.tenantId,
                            name = it.name,
                            villagecode = it.code,
                            chiefdomId = it.chiefdomId,
                            countryId = it.countryId,
                            districtId = it.districtId,
                            districtCode = it.districtCode,
                            chiefdomCode = it.chiefdomCode,
                        )
                    }.toList()

                linkedVillages.addAll(list)
            }
        }

        roomHelper.deleteAllLinkedVillages()
        roomHelper.insertLinkedVillages(linkedVillages)
    }

    private fun fetchIds(healthFacilities: ArrayList<HealthFacility>?): ArrayList<Long> {
        val ids = ArrayList<Long>()
        healthFacilities?.forEach {
            ids.add(it.id)
        }
        return ids
    }

    private suspend fun handleMeta(
        menu: Menu,
        meta: MutableList<String>,
    ) {
        menu.menus?.let {
            saveMenusInDb(menu.menus, menu.roleName)
        }
        menu.meta?.let { meta.addAll(it) }
    }

    private suspend fun saveClinicalWorkflowsForProvider(clinicalWorkflows: List<ClinicalWorkflow>) {
        val workflowList = clinicalWorkflows.filter { workflow ->
            workflow.conditions?.any { condition ->
                condition.moduleType == MedicalReviewTypeEnums.medicalReview.name.lowercase()
            } ?: false
        }
        val menuList = ArrayList<MenuDetail>()
        workflowList.forEach {
            menuList.add(
                MenuDetail(
                    name = it.workflowName,
                    order = it.displayOrder,
                    workflowName = it.workflowName,
                ),
            )
        }
        saveMenusInDb(menuList, PROVIDER)
    }

    private suspend fun saveConsentForm(consentForm: ConsentFormResponse?) {
        roomHelper.deleteAllConsentForm()
        consentForm?.let { forms ->
            forms.household?.let {
                roomHelper.insertConsentForm(
                    ConsentForm(
                        type = ConsentFormType.Household,
                        content = it,
                    ),
                )
            }
            forms.householdCulture?.let {
                roomHelper.insertConsentForm(
                    ConsentForm(
                        type = ConsentFormType.HouseHoldCulture,
                        content = it,
                    ),
                )
            }

            forms.EPI?.let {
                roomHelper.insertConsentForm(
                    ConsentForm(
                        type = ConsentFormType.EPI,
                        content = it,
                    ),
                )
            }

            forms.HIV?.let {
                roomHelper.insertConsentForm(
                    ConsentForm(
                        type = ConsentFormType.HIV,
                        content = it,
                    ),
                )
            }
        }
    }

    private suspend fun saveHealthFacilityInDb(
        list: List<HealthFacility>,
        defaultId: Long,
        userHealthFacilities: List<HealthFacility>?,
    ) {
        roomHelper.deleteAllHealthFacility()
        list.forEach { healthFacility ->
            roomHelper.saveHealthFacility(
                HealthFacilityEntity(
                    id = healthFacility.id,
                    name = healthFacility.name,
                    districtId = healthFacility.district.id,
                    chiefdomId = healthFacility.chiefdom?.id ?: healthFacility.chiefdomId,
                    tenantId = healthFacility.tenantId,
                    fhirId = healthFacility.fhirId,
                    isDefault = if (healthFacility.id == defaultId) {
                        SecuredPreference.putString(
                            SecuredPreference.EnvironmentKey.IS_DEFAULT_SITE_ID.name,
                            healthFacility.fhirId,
                        )
                        true
                    } else {
                        false
                    },
                    isUserSite = userHealthFacilities?.any { userSiteFacility -> userSiteFacility.id == healthFacility.id } ?: false,
                    phoneNumber = healthFacility.phuFocalPersonNumber?.toString() ?: "",
                ),
            )
        }
        val otherUserHealthFacility = userHealthFacilities?.filterNot { it in list }
        otherUserHealthFacility?.forEach { healthFacility ->
            roomHelper.saveHealthFacility(
                HealthFacilityEntity(
                    id = healthFacility.id,
                    name = healthFacility.name,
                    districtId = healthFacility.district.id,
                    chiefdomId = healthFacility.chiefdom?.id ?: healthFacility.chiefdomId,
                    tenantId = healthFacility.tenantId,
                    fhirId = healthFacility.fhirId,
                    isDefault = healthFacility.id == defaultId,
                    isUserSite = userHealthFacilities.any { userSiteFacility -> userSiteFacility.id == healthFacility.id },
                    phoneNumber = healthFacility.phuFocalPersonNumber?.toString(),
                ),
            )
        }
    }

    private fun saveFhirId(
        userId: String?,
        healthFacility: HealthFacility,
        changeFacility: Boolean,
    ) {
        userId?.let {
            SecuredPreference.putString(SecuredPreference.EnvironmentKey.USER_FHIR_ID.name, it)
        }

        val spiceUserId = SecuredPreference.getUserId()
        SecuredPreference.putLong(SecuredPreference.EnvironmentKey.OLD_USER_ID.name, spiceUserId)

        if (!changeFacility) {
            healthFacility.id.let {
                SecuredPreference.putLong(
                    SecuredPreference.EnvironmentKey.ORGANIZATION_ID.name,
                    it,
                )
            }

            healthFacility.fhirId?.let {
                SecuredPreference.putString(
                    SecuredPreference.EnvironmentKey.ORGANIZATION_FHIR_ID.name,
                    it,
                )
            }

            SecuredPreference.putLong(
                SecuredPreference.EnvironmentKey.TENANT_ID.name,
                healthFacility.tenantId,
            )

            healthFacility.district.id.let {
                SecuredPreference.putLong(SecuredPreference.EnvironmentKey.DISTRICT_ID.name, it)
            }

            healthFacility.chiefdom?.id?.let {
                SecuredPreference.putLong(SecuredPreference.EnvironmentKey.CHIEFDOM_ID.name, it)
            }
        }
    }

    private suspend fun saveClinicalWorkflowsInDb(clinicalTools: List<ClinicalWorkflow>) {
        val clinicalWorkFlowList = mutableListOf<ClinicalWorkflowEntity>()
        val clinicalWorkFlowConditions = mutableListOf<ClinicalWorkflowConditionEntity>()
        clinicalTools.forEach { clinicalWorkflow ->
            clinicalWorkFlowList.add(
                ClinicalWorkflowEntity(
                    id = clinicalWorkflow.id,
                    name = clinicalWorkflow.name,
                    moduleType = clinicalWorkflow.moduleType,
                    workflowName = clinicalWorkflow.workflowName,
                    countryId = clinicalWorkflow.countryId,
                    displayOrder = clinicalWorkflow.displayOrder,
                ),
            )
            clinicalWorkFlowConditions.addAll(
                clinicalWorkflow.conditions
                    ?.map { condition ->
                        ClinicalWorkflowConditionEntity(
                            id = 0,
                            gender = condition.gender,
                            maxAge = condition.maxAge,
                            minAge = condition.minAge,
                            clinicalWorkflowId = clinicalWorkflow.id,
                            subModule = condition.subModule,
                            moduleType = condition.moduleType,
                            groupName = condition.groupName,
                            cultureGroupName = condition.displayGroupName,
                            category = condition.category,
                        )
                    }?.toList() ?: listOf(),
            )
        }
        roomHelper.deleteAllClinicalWorkflow()
        roomHelper.deleteClinicalWorkflowConditions()
        roomHelper.saveClinicalWorkflows(clinicalWorkFlowList)
        roomHelper.insertClinicalWorkflowConditions(clinicalWorkFlowConditions)
        val psychologicalFlow = clinicalWorkFlowList.firstOrNull {
            it.workflowName?.contains(Screening.PHQ4) == true ||
                it.workflowName?.contains(Screening.suicideScreener) == true ||
                it.workflowName?.contains(Screening.substanceAbuse) == true
        }
        SecuredPreference.putBoolean(
            SecuredPreference.EnvironmentKey.IS_PSYCHOLOGICAL_FLOW_ENABLED.name,
            psychologicalFlow != null,
        )
    }

    private suspend fun saveUserProfileDetailsInDb(userProfile: UserProfile) {
        userProfile.supervisor?.id?.let {
            SecuredPreference.putLong(SecuredPreference.EnvironmentKey.PEER_SUPERVISOR_ID.name, it)
        } ?: run {
            SecuredPreference.remove(SecuredPreference.EnvironmentKey.PEER_SUPERVISOR_ID.name)
        }

        roomHelper.deleteAllUserProfileDetails()
        roomHelper.saveUserProfileDetails(
            UserProfileEntity(
                id = 0,
                profileData = Gson().toJson(userProfile),
            ),
        )
    }

    private suspend fun saveMenusInDb(
        menus: ArrayList<MenuDetail>,
        roleName: String,
    ) {
        menus.forEach { menu ->
            roomHelper.saveMenus(
                MenuEntity(
                    id = 0,
                    roleName = roleName,
                    name = menu.name,
                    displayOrder = menu.order,
                    menuId = menu.workflowName ?: menu.name,
                    displayValue = menu.displayValue,
                ),
            )
        }
    }

    private suspend fun saveFormsInDb(formData: List<FormData>) {
        roomHelper.deleteAllForms()
        roomHelper.saveForms(
            formData.map { data ->
                FormEntity(
                    id = data.id,
                    formInput = data.formInput,
                    formType = data.formType,
                    workflowName = data.workflowName,
                    clinicalWorkflowId = data.clinicalWorkflowId,
                )
            },
        )
    }

    private suspend fun saveNcdFormsInDb(formResponse: FormResponse) {
        roomHelper.deleteAllForms()
        roomHelper.deleteConsent()
        formResponse.screening?.let { scr ->
            roomHelper.saveForm(
                FormEntity(
                    id = scr.id,
                    formType = DefinedParams.Screening,
                    formInput = scr.inputForm,
                ),
            )
            roomHelper.saveConsent(
                ConsentEntity(
                    id = scr.id,
                    formType = DefinedParams.Screening,
                    formInput = scr.consentForm,
                ),
            )
        }
        formResponse.enrollment?.let { enr ->
            roomHelper.saveForm(
                FormEntity(
                    id = enr.id,
                    formType = DefinedParams.Registration,
                    formInput = enr.inputForm,
                ),
            )
            roomHelper.saveConsent(
                ConsentEntity(
                    id = enr.id,
                    formType = DefinedParams.Registration,
                    formInput = enr.consentForm,
                ),
            )
        }
        formResponse.assessment?.let { ass ->
            val gson = Gson()
            val formFieldsType = object :
                TypeToken<org.medtroniclabs.uhis.formgeneration.model.FormResponse>() {}.type
            val formFields: org.medtroniclabs.uhis.formgeneration.model.FormResponse =
                gson.fromJson(ass.inputForm, formFieldsType)
            val categories =
                listOf(AssessmentDefinedParams.ncd, MenuConstants.MATERNAL_HEALTH, MenuConstants.MENTAL_HEALTH)
            categories.forEach { category ->
                val cardIdList = formFields.formLayout
                    .filter {
                        it.viewType.equals(
                            AssessmentDefinedParams.CardView,
                            true,
                        ) &&
                            (it.category?.contains(category) == true)
                    }.map { it.id }
                if (cardIdList.isNotEmpty()) {
                    val formLayoutList = formFields.formLayout.filter { formLayout ->
                        cardIdList.any { id -> formLayout.family == id || formLayout.id == id }
                    }
                    roomHelper.saveForm(
                        FormEntity(
                            id = ass.id,
                            formType = DefinedParams.Assessment,
                            formInput = gson.toJson(
                                org.medtroniclabs.uhis.formgeneration.model.FormResponse(
                                    formLayout = formLayoutList,
                                    time = formFields.time,
                                ),
                            ),
                            workflowName = category,
                        ),
                    )
                }
            }
            roomHelper.saveConsent(
                ConsentEntity(
                    id = ass.id,
                    formType = DefinedParams.Assessment,
                    formInput = ass.consentForm,
                ),
            )
        }
        formResponse.customizedWorkflow?.let { workflows ->
            if (workflows.isNotEmpty()) {
                val moduleString = Gson().toJson(workflows)
                if (!moduleString.isNullOrBlank()) {
                    roomHelper.saveForm(
                        FormEntity(
                            id = formResponse.id,
                            formType = DefinedParams.Workflow,
                            formInput = moduleString,
                        ),
                    )
                }
            }
        }
    }

    private suspend fun saveNcdModelQuestions(modelQuestions: List<ModelQuestion>?) {
        roomHelper.deleteModelQuestions()
        modelQuestions?.let {
            val mentalHealthList = ArrayList<MentalHealthEntity>()
            it.forEach { listItem ->
                mentalHealthList.add(
                    MentalHealthEntity(
                        formType = listItem.type,
                        formInput = listItem.questions,
                    ),
                )
            }
            if (mentalHealthList.isNotEmpty()) {
                roomHelper.saveModelQuestions(mentalHealthList)
            }
        }
    }

    fun getANCPNCStatus(pregnancyDetail: PregnancyDetail?): String? {
        pregnancyDetail?.let { memberPregnancyDetail ->
            val ddDay = getDayCountFromDD(memberPregnancyDetail)
            if (memberPregnancyDetail.typeOfAbortion.isNullOrBlank() && (ddDay == null || ddDay <= 42)) {
                val dd = getDayCountFromDD(memberPregnancyDetail)
                val lmp = getDayCountFromLMP(memberPregnancyDetail)
                if (lmp != null && lmp >= PregnantWomen.LMP_THRESHOLD_DAYS && dd == null) {
                    return RMNCH.ANC
                } else if (dd != null && dd <= 42) {
                    return RMNCH.PNC
                }
            }
        }

        return null
    }

    suspend fun getMenuForClinicalWorkflows(
        selectedHouseholdMemberID: Long,
        gender: String?,
    ): Resource<List<MenuEntity>> =
        try {
            if (selectedHouseholdMemberID != -1L) {
                val memberData = roomHelper.getDobAndGenderById(selectedHouseholdMemberID)
                val calenderPeriod = DateUtils.getV2YearMonthAndWeek(memberData.dateOfBirth)
                var months = (calenderPeriod.years * 12) + calenderPeriod.months

                if ((months == 15 || months == 588) && calenderPeriod.weeks == 0 && calenderPeriod.days == 0) {
                    months -= 1
                }

                val clinicalMenu = roomHelper.getClinicalWorkflowId(memberData.gender, months)

                val menuList = convertorClinicalWorkflowsToMenuEntity(clinicalMenu)

                if (months > 24) {
                    setCustomStatus(menuList, selectedHouseholdMemberID)
                } else {
                    setChildCustomStatus(menuList, selectedHouseholdMemberID)
                }

                Resource(
                    state = ResourceState.SUCCESS,
                    data = menuList.filter { !it.isDisabled },
                )
            } else if (!gender.isNullOrBlank()) {
                val list = roomHelper.getAssessmentClinicalWorkflow(gender, DefinedParams.Assessment)

                Resource(
                    state = ResourceState.SUCCESS,
                    data = convertorClinicalWorkflowsToMenuEntity(list),
                )
            } else {
                Resource(state = ResourceState.ERROR)
            }
        } catch (_: Exception) {
            Resource(state = ResourceState.ERROR)
        }

    private suspend fun setCustomStatus(
        menu: List<MenuEntity>,
        selectedHouseholdMemberID: Long,
    ) {
        var memberPregnancyDetail: PregnancyDetail? = null
        roomHelper.getPregnancyDetailByPatientId(selectedHouseholdMemberID)?.let { pregnancyDetail ->
            val ddDay = getDayCountFromDD(pregnancyDetail)
            if (pregnancyDetail.typeOfAbortion.isNullOrBlank() && (ddDay == null || ddDay <= 42)) {
                memberPregnancyDetail = pregnancyDetail
            }
        }
        menu.forEach { item ->
            when (item.menuId) {
                MenuConstants.FP_MENU_ID -> {
                    item.isDisabled = isFPMenuDisable(memberPregnancyDetail, selectedHouseholdMemberID)
                }

                MenuConstants.PREGNANT_WOMEN_PROFILE -> {
                    item.isDisabled = isPWProfileMenuDisable(memberPregnancyDetail, selectedHouseholdMemberID)
                }

                MenuConstants.PREGNANCY_OUTCOME -> {
                    item.isDisabled = isPOMenuDisable(memberPregnancyDetail, selectedHouseholdMemberID)
                }

                MenuConstants.RMNCH_MENU_ID -> {
                    val rmnchWorkFlow = isRMNCHMenuDisable(memberPregnancyDetail, selectedHouseholdMemberID)
                    item.isDisabled = rmnchWorkFlow.first
                    item.subModule = rmnchWorkFlow.second
                }
            }
        }
    }

    private suspend fun setChildCustomStatus(
        menu: List<MenuEntity>,
        selectedHouseholdMemberID: Long,
    ) {
        menu.forEach { item ->
            when (item.menuId) {
                MenuConstants.RMNCH_MENU_ID -> {
                    val childVisit = isChildVisitMenuDisable(selectedHouseholdMemberID)
                    item.isDisabled = childVisit.first
                    item.subModule = childVisit.second
                }
            }
        }
    }

    fun getDayCountFromLMP(pregnancyDetail: PregnancyDetail): Long? {
        val today = java.util.Date()
        val lmpDate = parseToDate(pregnancyDetail.lastMenstrualPeriod)
        return if (lmpDate != null) {
            DateUtils.daysBetweenDates(lmpDate, today)
        } else {
            null
        }
    }

    fun getDayCountFromDD(pregnancyDetail: PregnancyDetail): Long? {
        val today = java.util.Date()
        val dDate = parseToDate(pregnancyDetail.dateOfDelivery)
        return if (dDate != null) {
            DateUtils.daysBetweenDates(dDate, today)
        } else {
            null
        }
    }

    // Reuse DateUtils: try common formats then daysBetweenDates for day difference
    fun parseToDate(dateStr: String?): java.util.Date? {
        if (dateStr.isNullOrBlank()) return null
        return DateUtils.convertStringToDate(dateStr, DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ)
            ?: DateUtils.convertStringToDate(dateStr, DateUtils.DATE_ddMMyyyy)
            ?: DateUtils.convertStringToDate(dateStr, DateUtils.DATE_FORMAT_yyyyMMdd)
    }

    private suspend fun isFPMenuDisable(
        pregnancyDetail: PregnancyDetail?,
        selectedHouseholdMemberID: Long,
    ): Boolean {
        if (checkIfLastServiceProvidedIsAfter(selectedHouseholdMemberID, MenuConstants.FP_MENU_ID, 15)) {
            return true
        }
        if (pregnancyDetail == null) return false
        val daysFromDelivery = getDayCountFromDD(pregnancyDetail)
        val daysFromLmp = getDayCountFromLMP(pregnancyDetail)
        return (daysFromLmp != null && daysFromLmp >= 42) && (daysFromDelivery == null)
    }

    private suspend fun isPWProfileMenuDisable(
        pregnancyDetail: PregnancyDetail?,
        selectedHouseholdMemberID: Long,
    ): Boolean {
        if (checkIfLastServiceProvidedIsAfter(selectedHouseholdMemberID, MenuConstants.PREGNANT_WOMEN_PROFILE)) {
            return true
        }
        if (pregnancyDetail == null) return false
        val daysFromDelivery = getDayCountFromDD(pregnancyDetail)
        val daysFromLmp = getDayCountFromLMP(pregnancyDetail)
        if (daysFromLmp != null && daysFromLmp >= 42) {
            return true
        } else {
            if (daysFromDelivery != null && daysFromDelivery <= 42) return true
        }
        return false
    }

    private suspend fun isRMNCHMenuDisable(
        pregnancyDetail: PregnancyDetail?,
        selectedHouseholdMemberID: Long,
    ): Pair<Boolean, String?> {
        getANCPNCStatus(pregnancyDetail)?.let { workflow ->
            if (workflow == RMNCH.ANC) {
                return checkIfLastServiceProvidedIsAfter(selectedHouseholdMemberID, MenuConstants.ANC, 15) to workflow
            } else if (workflow == RMNCH.PNC) {
                return checkIfLastServiceProvidedIsAfter(selectedHouseholdMemberID, MenuConstants.PNC_MOTHER) to workflow
            }
        }
        return (pregnancyDetail == null) to null
    }

    private suspend fun isChildVisitMenuDisable(selectedHouseholdMemberID: Long): Pair<Boolean, String> =
        checkIfLastServiceProvidedIsAfter(selectedHouseholdMemberID, MenuConstants.CHILDHOOD_VISIT, 15) to RMNCH.ChildHoodVisit

    private suspend fun isPOMenuDisable(
        pregnancyDetail: PregnancyDetail?,
        selectedHouseholdMemberID: Long,
    ): Boolean {
        if (checkIfLastServiceProvidedIsAfter(selectedHouseholdMemberID, MenuConstants.PREGNANCY_OUTCOME)) {
            return true
        }
        if (pregnancyDetail == null) return false
        val daysFromDelivery = getDayCountFromDD(pregnancyDetail)
        return daysFromDelivery != null && daysFromDelivery <= 42
    }

    private fun convertorClinicalWorkflowsToMenuEntity(clinicalWorkflows: List<NCDAssessmentClinicalWorkflow>): List<MenuEntity> {
        val (individualWorkflows, groupWorkflows) = clinicalWorkflows.partition { it.category.isNullOrBlank() }
        return (individualWorkflows + groupWorkflows.distinctBy { it.category }).map { workflow ->
            MenuEntity(
                id = workflow.id,
                menuId = workflow.category ?: workflow.workflowName,
                name = workflow.cultureGroupName ?: workflow.groupName ?: workflow.name,
                displayOrder = workflow.displayOrder ?: 0,
                subModule = workflow.subModule,
            )
        }
    }

    suspend fun getMenu(): Resource<List<MenuEntity>> =
        try {
            val data = roomHelper.getMenus()
            Resource(state = ResourceState.SUCCESS, data = data)
        } catch (_: Exception) {
            Resource(state = ResourceState.ERROR)
        }

    /**
     * Lowercased [ClinicalWorkflowEntity.workflowName] values from forms sync ([saveClinicalWorkflowsInDb]).
     * Aligns dashboard tiles with configured clinical tools rather than [MenuEntity.menuId].
     */
    suspend fun getClinicalWorkflowWorkflowNamesLower(): Set<String> =
        try {
            roomHelper
                .getMenuForClinicalWorkflows()
                .mapNotNull { wf ->
                    wf.workflowName
                        ?.trim()
                        ?.lowercase()
                        ?.takeIf { it.isNotEmpty() }
                }.toSet()
        } catch (_: Exception) {
            emptySet()
        }

    suspend fun getUserProfile(): Resource<UserProfile> =
        try {
            val data = roomHelper.getUserProfile()
            val userProfile: UserProfile =
                Gson().fromJson(data.profileData, UserProfile::class.java)
            Resource(state = ResourceState.SUCCESS, data = userProfile)
        } catch (_: Exception) {
            Resource(state = ResourceState.ERROR)
        }

    suspend fun getAllSubVillageIds(): List<Long> = roomHelper.getAllSubVillageIds()

    suspend fun checkIfLastServiceProvidedIsAfter(
        memberLocalId: Long,
        serviceTypeFor: String,
        thresholdDays: Long = 1,
    ): Boolean {
        val serviceHistory = getLastServiceHistory(memberLocalId, serviceTypeFor.lowercase(Locale.ENGLISH)) ?: return false
        return DateUtils.isIsoOffsetDateTimeOnLocalCalendarAfter(serviceHistory.visitDate, thresholdDays)
    }

    suspend fun getLastServiceHistory(
        memberLocalId: Long,
        serviceTypeFor: String,
    ): MemberAssessmentHistoryEntity? = roomHelper.getLastServiceHistory(memberLocalId, serviceTypeFor)

    suspend fun getUserHealthFacility(): Resource<ArrayList<HealthFacilityEntity>> =
        try {
            val response = roomHelper.getUserHealthFacility(true)
            Resource(state = ResourceState.SUCCESS, data = response)
        } catch (_: Exception) {
            Resource(state = ResourceState.ERROR)
        }

    fun getUnSyncedDataCountForNCDScreening() = roomHelper.getUnSyncedDataCountForNCDScreening()

    fun getUnSyncedNCDAssessmentCount() = roomHelper.getUnSyncedNCDAssessmentCount()

    fun getUnSyncedNCDFollowUpCount() = roomHelper.getUnSyncedNCDFollowUpCount()

    suspend fun getPatientListTransfer(request: NCDPatientTransferNotificationCountRequest): Resource<PatientTransferListResponse> =
        try {
            val response = apiHelper.getPatientListTransfer(request)
            Resource(state = ResourceState.SUCCESS, data = response.body()?.entity)
        } catch (_: Exception) {
            Resource(state = ResourceState.ERROR)
        }

    suspend fun patientTransferNotificationCount(request: NCDPatientTransferNotificationCountRequest): Resource<NCDPatientTransferNotificationCountResponse> =
        try {
            val response = apiHelper.patientTransferNotificationCount(request)
            Resource(state = ResourceState.SUCCESS, data = response.body()?.entity)
        } catch (_: Exception) {
            Resource(state = ResourceState.ERROR)
        }

    suspend fun patientTransferUpdate(request: NCDPatientTransferUpdateRequest): Resource<String> =
        try {
            val response = apiHelper.patientTransferUpdate(request)
            Resource(state = ResourceState.SUCCESS, data = response.body()?.message)
        } catch (_: Exception) {
            Resource(state = ResourceState.ERROR)
        }

    suspend fun getCultures(): Resource<List<CulturesEntity>> =
        try {
            val response = roomHelper.getCultures()
            Resource(state = ResourceState.SUCCESS, response)
        } catch (_: Exception) {
            Resource(state = ResourceState.ERROR)
        }

    suspend fun cultureLocaleUpdate(localeRequest: CultureLocaleModel) = apiHelper.cultureLocaleUpdate(localeRequest)

    suspend fun createSupportRequest(request: NCDSupportRequest): Resource<String> =
        try {
            val response = apiHelper.createSupportRequest(request)
            Resource(state = ResourceState.SUCCESS, data = response.body()?.message)
        } catch (_: Exception) {
            Resource(state = ResourceState.ERROR)
        }

    private fun convertFamilyPlanningMethodToSymptoms(
        inputList: List<SignsAndSymptomsEntity>,
        maxId: Long,
    ): List<SignsAndSymptomsEntity> {
        val resultList = mutableListOf<SignsAndSymptomsEntity>()
        val categoryMap = mutableMapOf<String, Int>()
        var idCounter = 1
        var categoryId = maxId + 1
        for (item in inputList) {
            if (!categoryMap.containsKey(item.category)) {
                item.category?.let { categoryName ->
                    resultList.add(
                        SignsAndSymptomsEntity(
                            _id = categoryId++,
                            symptom = categoryName,
                            type = item.type,
                            displayValue = categoryName,
                            displayOrder = idCounter++,
                            value = categoryName,
                            isTitle = true,
                        ),
                    )
                    categoryMap[categoryName] = idCounter
                }
            }

            resultList.add(
                SignsAndSymptomsEntity(
                    _id = item._id,
                    symptom = item.symptom,
                    type = item.type,
                    displayValue = item.displayValue,
                    displayOrder = idCounter++,
                    value = item.value,
                    isTitle = false,
                ),
            )
        }
        return resultList
    }

    suspend fun getCBSNotificationDetails(request: PeerSupervisorNotificationRequest): Resource<ArrayList<PeerSupervisorNotificationResponse>> =
        try {
            val response = apiHelper.getCBSNotificationDetails(request)
            if (response.isSuccessful) {
                val res = response.body()
                if (res?.status == true && res.entityList != null) {
                    Resource(state = ResourceState.SUCCESS, data = res.entityList)
                } else {
                    Resource(state = ResourceState.ERROR)
                }
            } else {
                Resource(state = ResourceState.ERROR)
            }
        } catch (_: Exception) {
            Resource(state = ResourceState.ERROR)
        }

    suspend fun updateCBSNotification(request: PeerSupervisorNotificationRequest): Resource<Unit> =
        try {
            val response = apiHelper.updateCBSNotification(request)
            if (response.isSuccessful) {
                Resource(state = ResourceState.SUCCESS, message = response.message())
            } else {
                Resource(state = ResourceState.ERROR, message = response.message())
            }
        } catch (_: Exception) {
            Resource(state = ResourceState.ERROR)
        }

    private fun generateChipItemByType(
        presentingComplaints: List<MedicalReviewMetaItems>,
        systemicExaminations: List<MedicalReviewMetaItems>,
        comorbidities: List<MedicalReviewMetaItems>,
        patientStatus: List<MedicalReviewMetaItems>,
        patientType: List<MedicalReviewMetaItems>,
        treatmentOutcome: List<MedicalReviewMetaItems>,
    ): List<MedicalReviewMetaItems> {
        val chipItemList = mutableListOf<MedicalReviewMetaItems>()
        chipItemList.addAll(
            presentingComplaints.map {
                it.apply {
                    category = MedicalReviewTypeEnums.PresentingComplaints.name
                }
            },
        )
        chipItemList.addAll(
            systemicExaminations.map {
                it.apply {
                    category = MedicalReviewTypeEnums.SystemicExaminations.name
                }
            },
        )
        chipItemList.addAll(
            comorbidities.map {
                it.apply {
                    type = MedicalReviewTypeEnums.TB.name
                    category = MedicalReviewTypeEnums.comorbidities.name
                }
            },
        )
        chipItemList.addAll(
            patientStatus.map {
                it.apply {
                    type = MedicalReviewTypeEnums.TB.name
                    category = MedicalReviewTypeEnums.patient_status.name
                }
            },
        )
        chipItemList.addAll(
            patientType.map {
                it.apply {
                    category = MedicalReviewTypeEnums.patient_type.name
                }
            },
        )
        chipItemList.addAll(
            treatmentOutcome.map {
                it.apply {
                    category = MedicalReviewTypeEnums.treatment_outcome.name
                }
            },
        )
        return chipItemList
    }

    private suspend fun saveSubVillages(subVillages: List<SubVillage>?) {
        subVillages?.let { list ->
            val subVillageEntities = list.map { subVillage ->
                SubVillageEntity(
                    id = subVillage.id,
                    name = subVillage.name,
                    code = subVillage.code,
                    villageId = subVillage.villageId,
                )
            }
            roomHelper.deleteAllSubVillages()
            roomHelper.saveSubVillages(subVillageEntities)
        }
    }

    private suspend fun saveShasthyaKormisFromMeta(shasthyaKormis: List<ShasthyaKormi>?) {
        if (shasthyaKormis == null) return
        roomHelper.deleteAllShasthyaKormiLinkedVillages()
        roomHelper.deleteAllShasthyaKormis()
        if (shasthyaKormis.isEmpty()) return
        val entities =
            shasthyaKormis.map { k ->
                ShasthyaKormiEntity(
                    id = k.id,
                    firstName = k.firstName,
                    lastName = k.lastName,
                    gender = k.gender,
                    phoneNumber = k.phoneNumber,
                    username = k.username,
                    email = k.email,
                )
            }
        roomHelper.saveShasthyaKormis(entities)
        val linked = mutableListOf<ShasthyaKormiLinkedVillageEntity>()
        shasthyaKormis.forEach { k ->
            k.villages?.forEach { v ->
                linked.add(
                    ShasthyaKormiLinkedVillageEntity(
                        shasthyaKormiId = k.id,
                        villageId = v.id,
                    ),
                )
            }
        }
        if (linked.isNotEmpty()) {
            roomHelper.insertShasthyaKormiLinkedVillages(linked)
        }
    }

    private suspend fun saveShasthyaShebikas(shasthyaShebikas: List<ShasthyaShebika>?) {
        shasthyaShebikas?.let { list ->
            // Save ShasthyaShebika entities (without subVillages)
            val shasthyaShebikaEntities = list.map { shebika ->
                ShasthyaShebikaEntity(
                    id = shebika.id,
                    name = shebika.name,
                    phoneNumber = shebika.phoneNumber,
                    ssId = shebika.ssId,
                    shasthyaKormiId = shebika.shasthyaKormiId,
                )
            }
            roomHelper.deleteAllShasthyaShebikas()
            roomHelper.saveShasthyaShebikas(shasthyaShebikaEntities)

            // Save linked subVillages in junction table
            val linkedVillages = mutableListOf<ShasthyaShebikaLinkedVillageEntity>()
            list.forEach { shebika ->
                shebika.subVillages?.forEach { subVillage ->
                    linkedVillages.add(
                        ShasthyaShebikaLinkedVillageEntity(
                            shasthyaShebikaId = shebika.id,
                            subVillageId = subVillage.id,
                        ),
                    )
                }
            }
            roomHelper.deleteAllShasthyaShebikaLinkedVillages()
            if (linkedVillages.isNotEmpty()) {
                roomHelper.insertShasthyaShebikaLinkedVillages(linkedVillages)
            }
        }
    }

    suspend fun getConsentHtmlRawString(
        formType: String,
        userId: Long,
    ) = roomHelper.getConsentFormByType(formType)

    suspend fun riskFactorListing() = roomHelper.getAllRiskFactorEntityList()

    suspend fun deleteDuplicateAssessmentHistory(date: String) = roomHelper.deleteDuplicateAssessmentHistory(date)

    suspend fun getShastyhaShebikas(shasthyaKormiId: Long): List<ShasthyaShebikaEntity> = roomHelper.getShasthyaShebikaByShasthyaKormiId(shasthyaKormiId)

    suspend fun getSubVillagesByShasthyaShebikaIds(ids: List<Long>): List<SubVillageEntity> = roomHelper.getSubVillagesByShasthyaShebikaIds(ids)
}
