package org.medtroniclabs.uhis.ui.household.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.model.UserDetail
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams.HouseholdCreation
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams.HouseholdEdit
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.DefinedParams.HOUSEHOLD_REGISTRATION
import org.medtroniclabs.uhis.common.EntityMapper.getResultSpinnerMapList
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.model.RecommendedDosageListModel
import org.medtroniclabs.uhis.databinding.FragmentHouseHoldRegistrationBinding
import org.medtroniclabs.uhis.db.entity.HouseholdEntity
import org.medtroniclabs.uhis.db.entity.SubVillageEntity
import org.medtroniclabs.uhis.formgeneration.FormGenerator
import org.medtroniclabs.uhis.formgeneration.listener.FormEventListener
import org.medtroniclabs.uhis.formgeneration.model.FormLayout
import org.medtroniclabs.uhis.formgeneration.model.FormResponse
import org.medtroniclabs.uhis.mappingkey.HouseHoldRegistration
import org.medtroniclabs.uhis.mappingkey.HouseHoldRegistration.NO_OF_PEOPLE
import org.medtroniclabs.uhis.mappingkey.HouseHoldRegistration.SUB_VILLAGE_ID
import org.medtroniclabs.uhis.mappingkey.HouseHoldRegistration.TOTAL_MEMBERS
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.household.viewmodel.HouseRegistrationViewModel
import org.medtroniclabs.uhis.ui.landing.OnDialogDismissListener

@AndroidEntryPoint
class HouseHoldRegistrationFragment : BaseFragment(), View.OnClickListener, FormEventListener {
    lateinit var binding: FragmentHouseHoldRegistrationBinding
    private lateinit var formGenerator: FormGenerator
    private var onDismissListener: OnDialogDismissListener? = null
    private val householdRegistrationViewModel: HouseRegistrationViewModel by activityViewModels()
    private var pendingSubVillageId: Long? = null
    private var lastSubVillageList: List<SubVillageEntity> = emptyList()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        onDismissListener = context as OnDialogDismissListener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentHouseHoldRegistrationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        householdRegistrationViewModel.getFormData(HOUSEHOLD_REGISTRATION)
        initializeFormGenerator()
        setListeners()
        attachObservers()
    }

    private fun attachObservers() {
        householdRegistrationViewModel.formLayoutsLiveData.observe(viewLifecycleOwner) { resources ->
            when (resources.state) {
                ResourceState.LOADING -> {
                    (activity as? BaseActivity)?.showLoading()
                }

                ResourceState.SUCCESS -> {
                    (activity as? BaseActivity)?.hideLoading()
                    resources.data?.let { data ->
                        val formFieldsType = object : TypeToken<FormResponse>() {}.type
                        val formFields: FormResponse = Gson().fromJson(data, formFieldsType)
                        formGenerator.populateViews(formFields.formLayout)
                    }
                }

                ResourceState.ERROR -> {
                    (activity as? BaseActivity)?.hideLoading()
                }
            }
        }

        // Observe generated household number and populate the field
        householdRegistrationViewModel.generatedHouseholdNumberLiveData.observe(viewLifecycleOwner) { householdNumber ->
            if (householdRegistrationViewModel.householdId == -1L) {
                // Only populate for new household registration
                formGenerator.getViewByTag(HouseHoldRegistration.HOUSEHOLD_NUMBER)?.let { view ->
                    formGenerator.setValueForView(householdNumber.toString(), view)
                }
            }
        }

        householdRegistrationViewModel.villageListResponse.observe(viewLifecycleOwner) { resourceState ->
            fetchFormDetails()
            when (resourceState.state) {
                ResourceState.SUCCESS -> {
                    resourceState.data?.let { data ->
                        formGenerator.spinnerDataInjection(data, getResultSpinnerMapList(data))

                        // Auto-select if single village
                        if (data.response is List<*> && data.response.size == 1) {
                            val singleItem = data.response[0]
                            if (singleItem is Map<*, *>) {
                                val id = singleItem[DefinedParams.ID]
                                formGenerator.getViewByTag(HouseHoldRegistration.VILLAGE_ID)?.let { view ->
                                    formGenerator.setValueForView(id, view)
                                }
                            }
                        }

                        arguments?.getLong(DefinedParams.VILLAGE_ID)?.let {
                            if (it != 0L) {
                                formGenerator.getViewByTag(HouseHoldRegistration.VILLAGE_ID)?.let { view ->
                                    view.isEnabled = false
                                    formGenerator.setValueForView(it, view)
                                }
                            }
                        }
                    }
                }
                else -> {
                    // Invoked if response state is not success
                }
            }
        }

        householdRegistrationViewModel.shasthyaShebikaListResponse.observe(viewLifecycleOwner) { resourceState ->
            fetchFormDetails()
            when (resourceState.state) {
                ResourceState.SUCCESS -> {
                    resourceState.data?.let { data ->
                        formGenerator.spinnerDataInjection(data, getResultSpinnerMapList(data))

                        // Auto-select if single shasthya shebika
                        if (data.response is List<*> && data.response.size == 1) {
                            val singleItem = data.response[0]
                            if (singleItem is Map<*, *>) {
                                val id = singleItem[DefinedParams.ID]
                                formGenerator.getViewByTag(HouseHoldRegistration.SHASTHYA_SHEBIKA_ID)?.let { view ->
                                    formGenerator.setValueForView(id, view)
                                    // Trigger sub-village loading
                                    val shasthyaShebikaIdLong = CommonUtils
                                        .getLongOrNull(id) ?: 0L
                                    if (shasthyaShebikaIdLong != 0L) {
                                        householdRegistrationViewModel.loadSubVillageDataCacheByType(
                                            shasthyaShebikaIdLong,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    // Invoked if response state is not success
                }
            }
        }

        householdRegistrationViewModel.subVillageListResponse.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.SUCCESS -> {
                    resourceState.data?.let { data ->
                        lastSubVillageList =
                            (data.response as? List<*>)
                                ?.filterIsInstance<SubVillageEntity>()
                                .orEmpty()
                        formGenerator.spinnerDataInjection(data, getResultSpinnerMapList(data))

                        // Auto-select if single sub village
                        if (data.response is List<*> && data.response.size == 1) {
                            val only = data.response[0]
                            val id: Any? =
                                when (only) {
                                    is SubVillageEntity -> only.id
                                    is Map<*, *> -> only[DefinedParams.ID]
                                    else -> null
                                }
                            if (id != null) {
                                formGenerator.getViewByTag(SUB_VILLAGE_ID)?.let { view ->
                                    formGenerator.setValueForView(id, view)
                                }
                            }
                        }

                        // Set pending sub village ID if available (for edit mode)
                        pendingSubVillageId?.let { subVillageId ->
                            if (subVillageId != 0L) {
                                formGenerator.getViewByTag(SUB_VILLAGE_ID)?.let { view ->
                                    formGenerator.setValueForView(subVillageId, view)
                                    // Disable Village field in edit mode
                                    view.isEnabled = false
                                }
                            }
                            pendingSubVillageId = null // Clear after setting
                        }
                    }
                }
                else -> {
                    // Invoked if response state is not success
                }
            }
        }

        householdRegistrationViewModel.houseHoldDetailLiveData.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    (activity as BaseActivity?)?.showLoading()
                }

                ResourceState.SUCCESS -> {
                    (activity as BaseActivity?)?.hideLoading()
                    resourceState.data?.let { houseHoldDetail ->
                        autoPopulateFormFields(houseHoldDetail)
                    }
                }

                ResourceState.ERROR -> {
                    (activity as BaseActivity?)?.hideLoading()
                }
            }
        }
    }

    private fun autoPopulateFormFields(details: HouseholdEntity) {
        formGenerator.getViewByTag(HouseHoldRegistration.VILLAGE_ID)?.let { view ->
            if (details.villageId != 0L) {
                view.isEnabled = false
                formGenerator.setValueForView(details.villageId, view)
            }
        }
        formGenerator.getViewByTag(NO_OF_PEOPLE)?.let { view ->
            formGenerator.setValueForView(details.noOfPeople, view)
        }
        formGenerator.getViewByTag(TOTAL_MEMBERS)?.let { view ->
            formGenerator.setValueForView(details.noOfPeople, view)
        }
        formGenerator.getViewByTag(HouseHoldRegistration.SHASTHYA_SHEBIKA_ID)?.let { view ->
            val shasthyaShebikaId = details.shasthyaShebikaId
            if (shasthyaShebikaId != null && shasthyaShebikaId != 0L) {
                // Store sub village ID to set after list is loaded
                pendingSubVillageId = details.subVillageId
                formGenerator.setValueForView(shasthyaShebikaId, view)
                // Disable SS field in edit mode
                view.isEnabled = false
            }
        }
        formGenerator.getViewByTag(HouseHoldRegistration.HOUSEHOLD_TYPE)?.let { view ->
            details.householdType?.let { type ->
                formGenerator.setValueForView(type, view)
            }
        }
        formGenerator.getViewByTag(HouseHoldRegistration.MONTHLY_INCOME_RANGE)?.let { view ->
            val range = if (details.monthlyIncomeRange.isNullOrBlank()) {
                details.monthlyIncome?.let { HouseHoldRegistration.rangeFromExactValue(it) }
            } else {
                details.monthlyIncomeRange
            }
            range?.let { incomeRange ->
                formGenerator.setValueForView(incomeRange, view)
            }
        }
        formGenerator.getViewByTag(HouseHoldRegistration.HOUSEHOLD_NUMBER)?.let { view ->
            details.householdNo?.let { householdNo ->
                formGenerator.setValueForView(householdNo, view)
            }
        }
        formGenerator.getViewByTag(HouseHoldRegistration.ID_DISABILITY_PERSONS_COUNT)?.let { view ->
            details.disabilityPersonsCount?.let { disabilityPersonsCount ->
                formGenerator.setValueForView(disabilityPersonsCount, view)
            }
        }
        formGenerator.getViewByTag(HouseHoldRegistration.HOUSEHOLD_HEAD_OCCUPATION)?.let { view ->
            details.householdHeadOccupation?.let { occupation ->
                formGenerator.setValueForView(occupation, view)
            }
        }
        formGenerator.getViewByTag(HouseHoldRegistration.OTHER_OCCUPATION)?.let { view ->
            details.otherOccupation?.let { otherOccupation ->
                formGenerator.setValueForView(otherOccupation, view)
            }
        }
    }

    private fun initializeFormGenerator() {
        if (householdRegistrationViewModel.householdId != -1L) {
            binding.btnNext.text = getString(R.string.submit)
            householdRegistrationViewModel.eventName = HouseholdEdit
            householdRegistrationViewModel.setUserJourney(HouseholdEdit)
        } else {
            householdRegistrationViewModel.eventName = HouseholdCreation
            householdRegistrationViewModel.setUserJourney(HouseholdCreation)
        }
        formGenerator = FormGenerator(
            requireContext(),
            binding.llForm,
            this,
            binding.scrollView,
            translate = SecuredPreference.getIsTranslationEnabled(),
            callback = { resultMap, id ->
                when (id) {
                    HouseHoldRegistration.SUB_VILLAGE_ID -> {
                        val selectedId = CommonUtils.getLongOrNull(resultMap[id]) ?: return@FormGenerator
                        householdRegistrationViewModel.selectedSubVillageId = selectedId
                        if (householdRegistrationViewModel.householdId == -1L &&
                            selectedId != -1L
                        ) {
                            householdRegistrationViewModel.generateHouseholdNumber()
                        }
                    }

                    TOTAL_MEMBERS -> {
                        val totalMembers = CommonUtils.getIntegerOrNull(resultMap[TOTAL_MEMBERS]) ?: 0
                        // Disability persons count shouldn't cross total members count
                        formGenerator.getFormLayout(HouseHoldRegistration.ID_DISABILITY_PERSONS_COUNT)?.maxValue = totalMembers.toDouble()
                    }
                }
            },
        )
    }

    /**
     * This function checks if both village and ss are fetched
     * then trigger fetch form details if it is for edit
     */
    private fun fetchFormDetails() {
        if (householdRegistrationViewModel.householdId != -1L) {
            val villageFetchStatus = householdRegistrationViewModel.villageListResponse.value
            val ssFetchStatus = householdRegistrationViewModel.shasthyaShebikaListResponse.value
            if (villageFetchStatus?.state != ResourceState.LOADING && ssFetchStatus?.state != ResourceState.LOADING) {
                householdRegistrationViewModel.getHouseholdDetailsByID(
                    householdRegistrationViewModel.householdId,
                )
            }
        }
    }

    private fun setListeners() {
        binding.btnNext.setOnClickListener(this)
        binding.btnCancel.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnNext -> {
                withLocationCheck({
                    formGenerator.formSubmitAction(v)
                })
                /*if (activity is HouseholdActivity) {
                    (activity as HouseholdActivity).loadFragment(2)
                }*/
            }
            R.id.btnCancel -> {
                householdRegistrationViewModel.setUserJourney(AnalyticsDefinedParams.CANCELBUTTONTRIGGERED)
                householdRegistrationViewModel.setAnalyticsData(
                    UserDetail.startDateTime,
                    eventName = if (householdRegistrationViewModel.householdId != -1L) HouseholdEdit else HouseholdCreation,
                    exitReason = AnalyticsDefinedParams.CancelButtonClicked,
                    isCompleted = false,
                )
                onDismissListener?.onDialogDismissListener()
            }
        }
    }

    override fun loadLocalCache(
        id: String,
        localDataCache: Any,
        selectedParent: Long?,
    ) {
        if (localDataCache is String) {
            when (id) {
                HouseHoldRegistration.VILLAGE_ID -> {
                    householdRegistrationViewModel.loadDataCacheByType(id, localDataCache)
                }
                HouseHoldRegistration.SHASTHYA_SHEBIKA_ID -> {
                    householdRegistrationViewModel.loadShasthyaShebikaDataCacheByType()
                }
                HouseHoldRegistration.SUB_VILLAGE_ID -> {
                    // This will be triggered when shasthya shebika is selected (via dependentID)
                    // selectedParent should contain the shasthya shebika id
                    val shasthyaShebikaIdLong = selectedParent ?: 0L
                    if (shasthyaShebikaIdLong != 0L) {
                        // Clear sub village when parent changes
                        formGenerator.getViewByTag(HouseHoldRegistration.SUB_VILLAGE_ID)?.let { view ->
                            formGenerator.setValueForView("", view)
                        }
                        householdRegistrationViewModel.loadSubVillageDataCacheByType(shasthyaShebikaIdLong)
                    }
                }
                else -> {
                    householdRegistrationViewModel.loadDataCacheByType(id, localDataCache)
                }
            }
        }
    }

    override fun onPopulate(targetId: String) {
    }

    override fun onCheckBoxDialogueClicked(
        id: String,
        formLayout: FormLayout,
        resultMap: Any?,
    ) {
    }

    override fun onInstructionClicked(
        id: String,
        title: String,
        informationList: ArrayList<String>?,
        description: String?,
        dosageListModel: ArrayList<RecommendedDosageListModel>?,
    ) {
    }

    override fun onFormSubmit(
        resultMap: HashMap<String, Any>?,
        serverData: List<FormLayout>?,
    ) {
        resultMap?.let { map ->
            if (householdRegistrationViewModel.householdId != -1L) {
                householdRegistrationViewModel.setAnalyticsData(
                    UserDetail.startDateTime,
                    eventName = HouseholdEdit,
                    isCompleted = true,
                )
                householdRegistrationViewModel.setUserJourney(AnalyticsDefinedParams.SUBMITBUTTONTRIGGERED)
                householdRegistrationViewModel.updateHousehold(map)
            } else {
                householdRegistrationViewModel.setUserJourney(AnalyticsDefinedParams.NEXTBUTTONTRIGGERED)
                householdRegistrationViewModel.registerHousehold(map)
            }
        }
    }

    override fun onRenderingComplete() {
        // SS list is tied to logged-in Kormi user, not Union — same as [ExternalMemberRegistrationFragment]
        householdRegistrationViewModel.loadShasthyaShebikaDataCacheByType()
    }

    override fun onUpdateInstruction(
        id: String,
        selectedId: Any?,
    ) {
        if (id == SUB_VILLAGE_ID) {
            applyVillageIdFromSubVillageSelection(selectedId)
        }
    }

    override fun onInformationHandling(
        id: String,
        noOfDays: Int,
        enteredDays: Int?,
        resultMap: HashMap<String, Any>?,
    ) {
    }

    override fun onAgeCheckForPregnancy() {
    }

    override fun handleMandatoryCondition(formLayout: FormLayout?) {
    }

    override fun onAgeUpdateListener(
        age: Int,
        serverData: List<FormLayout>?,
        resultHashMap: HashMap<String, Any>,
    ) {
        /*
       Never used
         */
    }

    /**
     * [village_id] (Union) stays hidden; when user picks a sub-village, set the parent [VillageEntity] id.
     */
    private fun applyVillageIdFromSubVillageSelection(selectedId: Any?) {
        val subVillageId = CommonUtils.getLongOrNull(selectedId) ?: return
        if (subVillageId == 0L) return
        val parentVillageId = lastSubVillageList.find { it.id == subVillageId }?.villageId ?: return
        formGenerator.getViewByTag(HouseHoldRegistration.VILLAGE_ID)?.let { view ->
            formGenerator.setValueForView(parentVillageId, view)
        }
    }

    override fun onQRScanRequested() {
    }

    fun getHouseHoldEnteredInputs(): Boolean = formGenerator.getResultMap().isNotEmpty()
}
