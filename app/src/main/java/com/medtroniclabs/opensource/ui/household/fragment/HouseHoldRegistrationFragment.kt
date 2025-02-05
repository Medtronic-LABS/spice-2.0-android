package com.medtroniclabs.opensource.ui.household.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.app.analytics.model.UserDetail
import com.medtroniclabs.opensource.app.analytics.utils.AnalyticsDefinedParams
import com.medtroniclabs.opensource.app.analytics.utils.AnalyticsDefinedParams.HouseholdCreation
import com.medtroniclabs.opensource.app.analytics.utils.AnalyticsDefinedParams.HouseholdEdit
import com.medtroniclabs.opensource.common.CommonUtils.getBooleanAsString
import com.medtroniclabs.opensource.common.EntityMapper.getResultSpinnerMapList
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.data.model.RecommendedDosageListModel
import com.medtroniclabs.opensource.databinding.FragmentHouseHoldRegistrationBinding
import com.medtroniclabs.opensource.db.entity.HouseholdEntity
import com.medtroniclabs.opensource.formgeneration.FormGenerator
import com.medtroniclabs.opensource.formgeneration.listener.FormEventListener
import com.medtroniclabs.opensource.formgeneration.model.FormLayout
import com.medtroniclabs.opensource.formgeneration.model.FormResponse
import com.medtroniclabs.opensource.mappingkey.HouseHoldRegistration.bedNetCount
import com.medtroniclabs.opensource.mappingkey.HouseHoldRegistration.headPhoneNumber
import com.medtroniclabs.opensource.mappingkey.HouseHoldRegistration.headPhoneNumberCategory
import com.medtroniclabs.opensource.mappingkey.HouseHoldRegistration.householdName
import com.medtroniclabs.opensource.mappingkey.HouseHoldRegistration.isOwnedATreatedBedNet
import com.medtroniclabs.opensource.mappingkey.HouseHoldRegistration.isOwnedAnImprovedLatrine
import com.medtroniclabs.opensource.mappingkey.HouseHoldRegistration.isOwnedHandWashingFacilityWithSoap
import com.medtroniclabs.opensource.mappingkey.HouseHoldRegistration.landmark
import com.medtroniclabs.opensource.mappingkey.HouseHoldRegistration.no
import com.medtroniclabs.opensource.mappingkey.HouseHoldRegistration.noOfPeople
import com.medtroniclabs.opensource.mappingkey.HouseHoldRegistration.villageId
import com.medtroniclabs.opensource.mappingkey.HouseHoldRegistration.yes
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.household.HouseholdDefinedParams.REGISTRATION
import com.medtroniclabs.opensource.ui.household.viewmodel.HouseRegistrationViewModel
import com.medtroniclabs.opensource.ui.landing.OnDialogDismissListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HouseHoldRegistrationFragment : Fragment(), View.OnClickListener, FormEventListener {

    lateinit var binding: FragmentHouseHoldRegistrationBinding
    private lateinit var formGenerator: FormGenerator
    private var onDismissListener: OnDialogDismissListener? = null
    private val householdRegistrationViewModel: HouseRegistrationViewModel by activityViewModels()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        onDismissListener = context as OnDialogDismissListener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHouseHoldRegistrationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        householdRegistrationViewModel.getFormData(REGISTRATION)
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

        householdRegistrationViewModel.villageListResponse.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.SUCCESS -> {
                    resourceState.data?.let { data ->
                        formGenerator.spinnerDataInjection(data, getResultSpinnerMapList(data))
                    }
                }
                else -> {
                    //Invoked if response state is not success
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
        formGenerator.getViewByTag(householdName)?.let { view ->
            formGenerator.setValueForView(details.name, view)
        }
        formGenerator.getViewByTag(villageId)?.let { view ->
            if (details.villageId != 0L) {
                view.isEnabled = false
            }
            formGenerator.setValueForView(details.villageId, view)
        }
        formGenerator.getViewByTag(landmark)?.let { view ->
            formGenerator.setValueForView(details.landmark, view)
        }
        formGenerator.getViewByTag(headPhoneNumber)?.let { view ->
            formGenerator.setValueForView(details.headPhoneNumber, view)
        }

        formGenerator.getViewByTag(headPhoneNumberCategory)?.let { view ->
            formGenerator.setValueForView(details.headPhoneNumberCategory, view)
        }

        formGenerator.getViewByTag(noOfPeople)?.let { view ->
            formGenerator.setValueForView(details.noOfPeople, view)
        }
        details.isOwnedAnImprovedLatrine.let {
            when (getBooleanAsString(it)) {
                yes -> {
                    singleSelectValueOption(
                        yes,
                        isOwnedAnImprovedLatrine
                    )
                }

                no -> {
                    singleSelectValueOption(
                        no,
                        isOwnedAnImprovedLatrine
                    )
                }

                else -> {}
            }
        }
        details.isOwnedHandWashingFacilityWithSoap.let {
            when (getBooleanAsString(it)) {
                yes -> {
                    singleSelectValueOption(
                        yes,
                        isOwnedHandWashingFacilityWithSoap
                    )
                }

                no -> {
                    singleSelectValueOption(
                        no,
                        isOwnedHandWashingFacilityWithSoap
                    )
                }

                else -> {}
            }
        }
        details.isOwnedATreatedBedNet.let {
            when (getBooleanAsString(it)) {
                yes -> {
                    singleSelectValueOption(
                        yes,
                        isOwnedATreatedBedNet
                    )
                    formGenerator.getViewByTag(bedNetCount)?.let { view ->
                        formGenerator.setValueForView(details.bedNetCount, view)
                    }
                }

                no -> {
                    singleSelectValueOption(
                        no,
                        isOwnedATreatedBedNet
                    )
                }

                else -> {}
            }
        }
    }

    private fun singleSelectValueOption(value: String, key: String) {
        formGenerator.getViewByTag("${value}_${key}")
            ?.let { view ->
                if (view is TextView) {
                    view.performClick()
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
            requireContext(), binding.llForm, null, this, binding.scrollView,
            translate = SecuredPreference.getIsTranslationEnabled()
        )
    }


    private fun setListeners() {
        binding.btnNext.setOnClickListener(this)
        binding.btnCancel.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnNext -> {
                formGenerator.formSubmitAction(v)
                /*if (activity is HouseholdActivity) {
                    (activity as HouseholdActivity).loadFragment(2)
                }*/
            }
            R.id.btnCancel -> {
                householdRegistrationViewModel.setAnalyticsData(
                    UserDetail.startDateTime,
                    eventName = if (householdRegistrationViewModel.householdId != -1L) HouseholdEdit else HouseholdCreation,
                    exitReason = AnalyticsDefinedParams.CancelButtonClicked,
                    isCompleted = false
                )
                onDismissListener?.onDialogDismissListener()
            }
        }
    }

    override fun loadLocalCache(id: String, localDataCache: Any, selectedParent: Long?) {
        if (localDataCache is String) {
            householdRegistrationViewModel.loadDataCacheByType(id, localDataCache)
        }
    }

    override fun onPopulate(targetId: String) {
    }

    override fun onCheckBoxDialogueClicked(
        id: String,
        serverViewModel: FormLayout,
        resultMap: Any?
    ) {
    }

    override fun onInstructionClicked(
        id: String,
        title: String,
        informationList: ArrayList<String>?,
        description: String?,
        dosageListModel: ArrayList<RecommendedDosageListModel>?
    ) {
    }

    override fun onFormSubmit(resultMap: HashMap<String, Any>?, serverData: List<FormLayout?>?) {
        resultMap?.let { map ->
            if (householdRegistrationViewModel.householdId != -1L) {
                householdRegistrationViewModel.setAnalyticsData(
                    UserDetail.startDateTime,
                    eventName = HouseholdEdit,
                    isCompleted = true
                )
                householdRegistrationViewModel.updateHousehold(map)
            } else {
                householdRegistrationViewModel.registerHousehold(map)
            }
        }
    }

    override fun onRenderingComplete() {
        if (householdRegistrationViewModel.householdId != -1L) {
            householdRegistrationViewModel.getHouseholdDetailsByID(householdRegistrationViewModel.householdId)
        }
    }

    override fun onUpdateInstruction(id: String, selectedId: Any?) {

    }

    override fun onInformationHandling(
        id: String,
        noOfDays: Int,
        enteredDays: Int?,
        resultMap: HashMap<String, Any>?
    ) {

    }

    override fun onAgeCheckForPregnancy() {
        
    }

    override fun handleMandatoryCondition(serverData: FormLayout?) {

    }

    override fun onAgeUpdateListener(
        age: Int,
        serverData: List<FormLayout?>?,
        resultHashMap: HashMap<String, Any>
    ) {
        /*
       Never used
        */
    }
}