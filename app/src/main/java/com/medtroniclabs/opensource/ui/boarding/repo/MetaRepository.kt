package com.medtroniclabs.opensource.ui.boarding.repo

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.ConsentFormType
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.RoleConstant.PROVIDER
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.data.ClinicalWorkflow
import com.medtroniclabs.opensource.data.ConsentFormResponse
import com.medtroniclabs.opensource.data.CulturesEntity
import com.medtroniclabs.opensource.data.FormData
import com.medtroniclabs.opensource.data.FormMetaRequest
import com.medtroniclabs.opensource.data.FormRequest
import com.medtroniclabs.opensource.data.FormResponse
import com.medtroniclabs.opensource.data.HealthFacility
import com.medtroniclabs.opensource.data.Menu
import com.medtroniclabs.opensource.data.MenuDetail
import com.medtroniclabs.opensource.data.ModelQuestion
import com.medtroniclabs.opensource.data.ProgramEntity
import com.medtroniclabs.opensource.data.UserProfile
import com.medtroniclabs.opensource.db.entity.ClinicalWorkflowConditionEntity
import com.medtroniclabs.opensource.db.entity.ClinicalWorkflowEntity
import com.medtroniclabs.opensource.db.entity.ConsentEntity
import com.medtroniclabs.opensource.db.entity.ConsentForm
import com.medtroniclabs.opensource.db.entity.FormEntity
import com.medtroniclabs.opensource.db.entity.HealthFacilityEntity
import com.medtroniclabs.opensource.db.entity.MentalHealthEntity
import com.medtroniclabs.opensource.db.entity.MenuEntity
import com.medtroniclabs.opensource.db.entity.NCDAssessmentClinicalWorkflow
import com.medtroniclabs.opensource.db.entity.RiskClassificationModel
import com.medtroniclabs.opensource.db.entity.RiskFactorEntity
import com.medtroniclabs.opensource.db.entity.UserProfileEntity
import com.medtroniclabs.opensource.db.entity.VillageEntity
import com.medtroniclabs.opensource.db.local.RoomHelper
import com.medtroniclabs.opensource.mappingkey.Screening
import com.medtroniclabs.opensource.model.CultureLocaleModel
import com.medtroniclabs.opensource.ncd.data.DeviceDetails
import com.medtroniclabs.opensource.ncd.data.NCDPatientTransferNotificationCountRequest
import com.medtroniclabs.opensource.ncd.data.NCDPatientTransferNotificationCountResponse
import com.medtroniclabs.opensource.ncd.data.NCDPatientTransferUpdateRequest
import com.medtroniclabs.opensource.ncd.data.PatientTransferListResponse
import com.medtroniclabs.opensource.ncd.data.TermsAndConditionsModel
import com.medtroniclabs.opensource.network.ApiHelper
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.MenuConstants
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewTypeEnums
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.lang.reflect.Type
import javax.inject.Inject

class MetaRepository @Inject constructor(
    private var apiHelper: ApiHelper,
    private var roomHelper: RoomHelper
) {

    suspend fun updateDeviceDetails(deviceDetails: DeviceDetails): Resource<DeviceDetails> {
        return try {
            val response = apiHelper.updateDeviceDetails(deviceDetails)
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody?.status == true)
                    Resource(state = ResourceState.SUCCESS)
                else
                    Resource(state = ResourceState.ERROR)
            } else
                Resource(state = ResourceState.ERROR)
        } catch (e: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun getMetaDataInformation(
        workflowNames: MutableList<Long>,
        meta: MutableList<String>,
        changeFacility: Boolean
    ): Resource<Boolean> {
        return try {
            withContext(Dispatchers.IO) {
                val response = async { apiHelper.getMetaDataInformation() }.await()

                if (response.isSuccessful && response.body()?.status == true) {
                    with(roomHelper) {
                        response.body()?.entity?.apply {
                            saveHealthFacilityInDb(
                                nearestHealthFacilities,
                                defaultHealthFacility.id,
                                userHealthFacilities
                            )
                            SecuredPreference.putString(
                                SecuredPreference.EnvironmentKey.DEFAULT_SITE_ID.name,
                                defaultHealthFacility.fhirId
                            )
                            deleteAllVillages()
                            saveVillage(modifiedVillages(villages, userProfile.villages))
                            deleteAllFrequencyList()
                            frequency?.let {
                                saveFrequencyList(it)
                            }
                            saveConsentForm(consentForm)
                            saveFhirId(userProfile.fhirId, defaultHealthFacility,changeFacility)
                            saveUserProfileDetailsInDb(userProfile)
                            appTypes?.forEach {
                                when (it) {
                                    DefinedParams.COMMUNITY -> SecuredPreference.putBoolean(
                                        SecuredPreference.EnvironmentKey.IS_COMMUNITY.name,
                                        true
                                    )
                                    DefinedParams.NON_COMMUNITY -> SecuredPreference.putBoolean(
                                        SecuredPreference.EnvironmentKey.IS_NON_COMMUNITY.name,
                                        true
                                    )
                                }
                            }
                            roomHelper.deleteAllMenus()
                            if (CommonUtils.isRolePresent()) {
                                saveClinicalWorkflowsForProvider(defaultHealthFacility.clinicalWorkflows)
                                if (CommonUtils.isNonCommunity())
                                    handleMeta(menu, meta)
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
                            chiefdoms?.let { chiefdomList ->
                                roomHelper.deleteChiefDoms()
                                roomHelper.saveChiefDoms(chiefdomList)
                            }
                            programs?.let { prgms ->
                                roomHelper.deletePrograms()
                                val list = ArrayList<ProgramEntity>()
                                prgms.forEach { item ->
                                    list.add(
                                        ProgramEntity(
                                            id = item.id,
                                            name = item.name,
                                            healthFacilityIds = fetchIds(item.healthFacilities)
                                        )
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
                                    remAttempts
                                )
                            }
                        }

                        val formsResponse = async {
                            apiHelper.getForms(
                                FormRequest(
                                    nonNcdWorkflowEnabled = CommonUtils.isCommunity(),
                                    workflowNames
                                )
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
                                }
                                if (CommonUtils.isNonCommunity())
                                {
                                    saveNcdFormsInDb(this)
                                    saveNcdModelQuestions(modelQuestions)
                                }
                                clinicalTools?.let {
                                    saveClinicalWorkflowsInDb(it)
                                } ?: run {
                                    return@with Resource(state = ResourceState.ERROR)
                                }
                            }
                        } else {
                            return@with Resource(state = ResourceState.ERROR)
                        }
                        if (meta.isNotEmpty()) {
                            val metadataResponse =
                                async { apiHelper.getFormMetadata(FormMetaRequest(meta)) }.await()
                            if (metadataResponse.isSuccessful && metadataResponse.body()?.status == true) {
                                if (metadataResponse.body()?.entity == null) {
                                    return@with Resource(state = ResourceState.ERROR)
                                }
                                metadataResponse.body()?.entity?.let { res ->
                                    res.symptoms.let {
                                        roomHelper.deleteAllSymptoms()
                                        roomHelper.insertSymptoms(it)
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
                                        roomHelper.deleteNCDDiagnosisList()
                                        roomHelper.saveNCDDiagnosisList(it)
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
                                            baseType
                                        )
                                        roomHelper.run {
                                            deleteRiskFactor()
                                            insertRiskFactor(
                                                RiskFactorEntity(
                                                    nonLabEntity = resultString
                                                )
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
                        if (CommonUtils.isNonCommunity()) {
                            val userTermsAndConditionsMeta = async {
                                apiHelper.getUserTermsAndConditions(
                                    TermsAndConditionsModel(countryId = SecuredPreference.getCountryId())
                                )
                            }.await()
                            if (userTermsAndConditionsMeta.isSuccessful && userTermsAndConditionsMeta.body()?.status == true) {
                                if (userTermsAndConditionsMeta.body()?.entity == null) {
                                    return@with Resource(state = ResourceState.ERROR)
                                }
                                userTermsAndConditionsMeta.body()?.entity?.let { res ->
                                    if (res.id != null && !res.formInput.isNullOrBlank())
                                        roomHelper.saveConsent(
                                            ConsentEntity(
                                                formType = DefinedParams.Landing,
                                                formInput = res.formInput
                                            )
                                        )
                                    else
                                        return@with Resource(state = ResourceState.ERROR)
                                }
                            } else {
                                return@with Resource(state = ResourceState.ERROR)
                            }
                        }
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

    private fun updateCulturesMeta(cultureList: ArrayList<CulturesEntity>) {
        val currentLocaleId = SecuredPreference.getCultureId()
        for (i in 0 until cultureList.size) {
            if (currentLocaleId <= 0) {
                if (cultureList[i].name.contains(DefinedParams.EN_Locale, ignoreCase = true)) {
                    SecuredPreference.setUserPreference(
                        cultureList[i].id,
                        cultureList[i].name,
                        false
                    )
                    break
                }
            } else if (cultureList[i].id == currentLocaleId) {
                val isEnabled = CommonUtils.checkIfTranslationEnabled(cultureList[i].name)
                SecuredPreference.setUserPreference(
                    cultureList[i].id,
                    cultureList[i].name,
                    isEnabled
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
                        true
                    )
                } != null
            )
        }
    }

    private fun modifiedVillages(
        allVillages: List<VillageEntity>,
        userVillages: List<VillageEntity>?
    ): List<VillageEntity> {
        allVillages.forEach { root ->
            root.isUserVillage = userVillages?.firstOrNull { it.id == root.id } != null
        }
        if (allVillages.isNotEmpty()) {
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
        return allVillages
    }

    private fun fetchIds(healthFacilities: ArrayList<HealthFacility>?): ArrayList<Long> {
        val ids = ArrayList<Long>()
        healthFacilities?.forEach {
            ids.add(it.id)
        }
        return ids
    }

    private suspend fun handleMeta(menu: Menu, meta: MutableList<String>) {
        saveMenusInDb(menu.menus, menu.roleName)
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
                    workflowName = it.workflowName
                )
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
                        content = it
                    )
                )
            }
        }
    }

    private suspend fun saveHealthFacilityInDb(
        list: List<HealthFacility>,
        defaultId: Long,
        userHealthFacilities: List<HealthFacility>?
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
                            healthFacility.fhirId
                        )
                        true
                    } else {
                        false
                    },
                    isUserSite = userHealthFacilities?.any { userSiteFacility -> userSiteFacility.id == healthFacility.id } ?: false
                )
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
                    isUserSite = userHealthFacilities?.any { userSiteFacility -> userSiteFacility.id == healthFacility.id } ?: false
                )
            )
        }
    }

    private fun saveFhirId(
        userId: String?,
        healthFacility: HealthFacility,
        changeFacility: Boolean
    ) {
        userId?.let {
            SecuredPreference.putString(SecuredPreference.EnvironmentKey.USER_FHIR_ID.name, it)
        }

        if (!changeFacility) {
            healthFacility.id.let {
                SecuredPreference.putLong(
                    SecuredPreference.EnvironmentKey.ORGANIZATION_ID.name,
                    it
                )
            }

            healthFacility.fhirId?.let {
                SecuredPreference.putString(
                    SecuredPreference.EnvironmentKey.ORGANIZATION_FHIR_ID.name,
                    it
                )
            }

            SecuredPreference.putLong(
                SecuredPreference.EnvironmentKey.TENANT_ID.name,
                healthFacility.tenantId
            )

            healthFacility.district.id.let {
                SecuredPreference.putLong(SecuredPreference.EnvironmentKey.DISTRICT_ID.name,it)
            }

            healthFacility.chiefdom?.id?.let {
                SecuredPreference.putLong(SecuredPreference.EnvironmentKey.CHIEFDOM_ID.name,it)
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
                )
            )
            clinicalWorkFlowConditions.addAll(clinicalWorkflow.conditions?.map { condition ->
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
                    category = condition.category
                )

            }?.toList() ?: listOf())
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
            psychologicalFlow != null
        )
    }

    private suspend fun saveUserProfileDetailsInDb(userProfile: UserProfile) {
        roomHelper.deleteAllUserProfileDetails()
        roomHelper.saveUserProfileDetails(
            UserProfileEntity(
                id = 0,
                profileData = Gson().toJson(userProfile)
            )
        )
    }

    private suspend fun saveMenusInDb(menus: ArrayList<MenuDetail>, roleName: String) {
        menus.forEach { menu ->
            roomHelper.saveMenus(
                MenuEntity(
                    id = 0,
                    roleName = roleName,
                    name = menu.name,
                    displayOrder = menu.order,
                    menuId = menu.workflowName ?: menu.name,
                    displayValue = menu.displayValue
                )
            )
        }
    }

    private suspend fun saveFormsInDb(formData: List<FormData>) {
        roomHelper.deleteAllForms()
        roomHelper.saveForms(formData.map { data ->
            FormEntity(
                id = data.id,
                formInput = data.formInput,
                formType = data.formType,
                workflowName = data.workflowName,
                clinicalWorkflowId = data.clinicalWorkflowId
            )
        })
    }

    private suspend fun saveNcdFormsInDb(formResponse: FormResponse) {
        roomHelper.deleteAllForms()
        roomHelper.deleteConsent()
        formResponse.screening?.let { scr ->
            roomHelper.saveForm(
                FormEntity(
                    id = scr.id,
                    formType = DefinedParams.Screening,
                    formInput = scr.inputForm
                )
            )
            roomHelper.saveConsent(
                ConsentEntity(
                    id = scr.id,
                    formType = DefinedParams.Screening,
                    formInput = scr.consentForm
                )
            )
        }
        formResponse.enrollment?.let { enr ->
            roomHelper.saveForm(
                FormEntity(
                    id = enr.id,
                    formType = DefinedParams.Registration,
                    formInput = enr.inputForm
                )
            )
            roomHelper.saveConsent(
                ConsentEntity(
                    id = enr.id,
                    formType = DefinedParams.Registration,
                    formInput = enr.consentForm
                )
            )
        }
        formResponse.assessment?.let { ass ->
            val gson = Gson()
            val formFieldsType = object :
                TypeToken<com.medtroniclabs.opensource.formgeneration.model.FormResponse>() {}.type
            val formFields: com.medtroniclabs.opensource.formgeneration.model.FormResponse =
                gson.fromJson(ass.inputForm, formFieldsType)
            val categories =
                listOf(AssessmentDefinedParams.ncd, MenuConstants.MATERNAL_HEALTH, MenuConstants.MENTAL_HEALTH)
            categories.forEach { category ->
                val cardIdList = formFields.formLayout
                    .filter {
                        it.viewType.equals(
                            AssessmentDefinedParams.CardView,
                            true
                        ) && (it.category?.contains(category) == true)
                    }
                    .map { it.id }
                if (cardIdList.isNotEmpty()) {
                    val formLayoutList = formFields.formLayout.filter { formLayout ->
                        cardIdList.any { id -> formLayout.family == id || formLayout.id == id }
                    }
                    roomHelper.saveForm(
                        FormEntity(
                            id = ass.id,
                            formType = DefinedParams.Assessment,
                            formInput = gson.toJson(
                                com.medtroniclabs.opensource.formgeneration.model.FormResponse(
                                    formLayout = formLayoutList,
                                    time = formFields.time
                                )
                            ),
                            workflowName = category
                        )
                    )
                }
            }
            roomHelper.saveConsent(
                ConsentEntity(
                    id = ass.id,
                    formType = DefinedParams.Assessment,
                    formInput = ass.consentForm
                )
            )
        }
        formResponse.customizedWorkflow?.let { workflows ->
            if (workflows.isNotEmpty()) {
                val moduleString = Gson().toJson(workflows)
                if (!moduleString.isNullOrBlank())
                    roomHelper.saveForm(
                        FormEntity(
                            id = formResponse.id,
                            formType = DefinedParams.Workflow,
                            formInput = moduleString
                        )
                    )
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
                        formInput = listItem.questions
                    )
                )
            }
            if (mentalHealthList.isNotEmpty()) {
                roomHelper.saveModelQuestions(mentalHealthList)
            }
        }
    }

    private fun saveUserIsLogin() {
        SecuredPreference.putBoolean(
            SecuredPreference.EnvironmentKey.ISLOGGEDIN.name,
            true
        )
        SecuredPreference.putBoolean(
            SecuredPreference.EnvironmentKey.ISMETALOADED.name,
            true
        )
    }


    suspend fun getMenuForClinicalWorkflows(selectedHouseholdMemberID: Long, gender: String?): Resource<List<MenuEntity>> {
        return try {
            if (selectedHouseholdMemberID != -1L) {
                val memberData = roomHelper.getDobAndGenderById(selectedHouseholdMemberID)
                val calenderPeriod = DateUtils.getV2YearMonthAndWeek(memberData.dateOfBirth)
                var months = (calenderPeriod.years * 12) + calenderPeriod.months

                if ((months == 15 || months == 588) && calenderPeriod.weeks == 0 && calenderPeriod.days == 0) {
                    months -= 1
                }

                val list = roomHelper.getClinicalWorkflowId(memberData.gender, months)


                /*val (months, weeks) = DateUtils.dateToMonthsAndWeeks(memberData.dateOfBirth) ?: Pair(0, 0)
                val list = if (months == 15 && weeks == 0) {
                    roomHelper.getClinicalWorkflowId(memberData.gender, months.minus(1))
                } else {
                    roomHelper.getClinicalWorkflowId(memberData.gender, months)
                }*/

                Resource(
                    state = ResourceState.SUCCESS,
                    data = convertorClinicalWorkflowsToMenuEntity(list)
                )
            } else if(!gender.isNullOrBlank()) {
                val list = roomHelper.getAssessmentClinicalWorkflow(gender, DefinedParams.Assessment)

                Resource(
                    state = ResourceState.SUCCESS,
                    data = convertorClinicalWorkflowsToMenuEntity(list)
                )
            } else {
                Resource(state = ResourceState.ERROR)
            }
        } catch (e: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }

    private fun convertorClinicalWorkflowsToMenuEntity(clinicalWorkflows: List<NCDAssessmentClinicalWorkflow>): List<MenuEntity> {
        val (individualWorkflows, groupWorkflows) = clinicalWorkflows.partition { it.category.isNullOrBlank() }
        return (individualWorkflows + groupWorkflows.distinctBy { it.category }).map { workflow ->
            MenuEntity(
                id = workflow.id,
                menuId = workflow.category ?: workflow.workflowName,
                name = workflow.cultureGroupName ?: workflow.groupName ?: workflow.name,
                displayOrder = workflow.displayOrder ?: 0,
                subModule = workflow.subModule
            )
        }
    }

    suspend fun getMenu(): Resource<List<MenuEntity>> {
        return try {
            val data = roomHelper.getMenus()
            Resource(state = ResourceState.SUCCESS, data = data)
        } catch (e: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun getUserProfile(): Resource<UserProfile> {
        return try {
            val data = roomHelper.getUserProfile()
            val userProfile: UserProfile =
                Gson().fromJson(data.profileData, UserProfile::class.java)
            Resource(state = ResourceState.SUCCESS, data = userProfile)
        } catch (e: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun getAllVillagesName(): Resource<List<VillageEntity>> {
        return try {
            val response = roomHelper.getAllVillageEntity()
            Resource(state = ResourceState.SUCCESS, data = response)
        } catch (e: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun getDefaultHealthFacility(): Resource<HealthFacilityEntity> {
        return try {
            val response = roomHelper.getDefaultHealthFacility()
            Resource(state = ResourceState.SUCCESS, data = response)
        } catch (e: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun getAllVillageIds(): List<Long> {
        return roomHelper.getAllVillageIds()
    }

    suspend fun getUserHealthFacility(): Resource<ArrayList<HealthFacilityEntity>> {
        return try {
            val response = roomHelper.getUserHealthFacility(true)
            Resource(state = ResourceState.SUCCESS, data = response)
        } catch (e: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }

    fun getUnSyncedDataCountForNCDScreening() =
        roomHelper.getUnSyncedDataCountForNCDScreening()

    fun getUnSyncedNCDAssessmentCount() =
        roomHelper.getUnSyncedNCDAssessmentCount()

    fun getUnSyncedNCDFollowUpCount() = roomHelper.getUnSyncedNCDFollowUpCount()

    suspend fun getPatientListTransfer(request: NCDPatientTransferNotificationCountRequest): Resource<PatientTransferListResponse> {
        return try {
            val response = apiHelper.getPatientListTransfer(request)
            Resource(state = ResourceState.SUCCESS, data = response.body()?.entity)
        } catch (e: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun patientTransferNotificationCount(request: NCDPatientTransferNotificationCountRequest): Resource<NCDPatientTransferNotificationCountResponse> {
        return try {
            val response = apiHelper.patientTransferNotificationCount(request)
            Resource(state = ResourceState.SUCCESS, data = response.body()?.entity)
        } catch (e: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun patientTransferUpdate(request: NCDPatientTransferUpdateRequest): Resource<String> {
        return try {
            val response = apiHelper.patientTransferUpdate(request)
            Resource(state = ResourceState.SUCCESS, data = response.body()?.message)
        } catch (e: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun getCultures(): Resource<List<CulturesEntity>> {
        return try {
            val response = roomHelper.getCultures()
            Resource(state = ResourceState.SUCCESS, response)
        } catch (_: Exception) {
            Resource(state = ResourceState.ERROR)
        }
    }

    suspend fun cultureLocaleUpdate(localeRequest: CultureLocaleModel) =
        apiHelper.cultureLocaleUpdate(localeRequest)
}

