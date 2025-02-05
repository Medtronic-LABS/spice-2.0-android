package com.medtroniclabs.opensource.ui.member

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatSpinner
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.app.analytics.model.UserDetail
import com.medtroniclabs.opensource.app.analytics.utils.AnalyticsDefinedParams
import com.medtroniclabs.opensource.app.analytics.utils.AnalyticsDefinedParams.AddNewMember
import com.medtroniclabs.opensource.app.analytics.utils.AnalyticsDefinedParams.EditNewMember
import com.medtroniclabs.opensource.app.analytics.utils.CommonUtils
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.startBackgroundOfflineSync
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.CommonUtils.getBooleanAsString
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ
import com.medtroniclabs.opensource.common.DateUtils.DATE_ddMMyyyy
import com.medtroniclabs.opensource.common.DefinedParams.DOB
import com.medtroniclabs.opensource.common.DefinedParams.HOUSEHOLD_MEMBER_REGISTRATION
import com.medtroniclabs.opensource.common.DefinedParams.HouseholdHead
import com.medtroniclabs.opensource.common.DefinedParams.MemberID
import com.medtroniclabs.opensource.common.DefinedParams.No
import com.medtroniclabs.opensource.common.DefinedParams.Yes
import com.medtroniclabs.opensource.common.DefinedParams.female
import com.medtroniclabs.opensource.common.DefinedParams.male
import com.medtroniclabs.opensource.common.EntityMapper.getResultSpinnerMapList
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.data.model.RecommendedDosageListModel
import com.medtroniclabs.opensource.databinding.FragmentMemberRegistrationBinding
import com.medtroniclabs.opensource.db.entity.HouseholdMemberEntity
import com.medtroniclabs.opensource.db.entity.VillageEntity
import com.medtroniclabs.opensource.formgeneration.FormGenerator
import com.medtroniclabs.opensource.formgeneration.config.DefinedParams
import com.medtroniclabs.opensource.formgeneration.listener.FormEventListener
import com.medtroniclabs.opensource.formgeneration.model.FormLayout
import com.medtroniclabs.opensource.formgeneration.model.FormResponse
import com.medtroniclabs.opensource.formgeneration.utility.CustomSpinnerAdapter
import com.medtroniclabs.opensource.mappingkey.HouseHoldRegistration.headPhoneNumberCategory
import com.medtroniclabs.opensource.mappingkey.HouseHoldRegistration.no
import com.medtroniclabs.opensource.mappingkey.HouseHoldRegistration.villageId
import com.medtroniclabs.opensource.mappingkey.HouseHoldRegistration.yes
import com.medtroniclabs.opensource.mappingkey.MemberRegistration
import com.medtroniclabs.opensource.mappingkey.MemberRegistration.gender
import com.medtroniclabs.opensource.mappingkey.MemberRegistration.householdHeadRelationship
import com.medtroniclabs.opensource.mappingkey.MemberRegistration.isPregnant
import com.medtroniclabs.opensource.mappingkey.MemberRegistration.name
import com.medtroniclabs.opensource.mappingkey.MemberRegistration.otherFamilyMember
import com.medtroniclabs.opensource.mappingkey.MemberRegistration.phoneNumber
import com.medtroniclabs.opensource.mappingkey.MemberRegistration.phoneNumberCategory
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.dialog.SuccessDialogFragment
import com.medtroniclabs.opensource.ui.home.AssessmentToolsActivity
import com.medtroniclabs.opensource.ui.household.HouseholdActivity
import com.medtroniclabs.opensource.ui.household.HouseholdDefinedParams
import com.medtroniclabs.opensource.ui.household.summary.HouseholdSummaryActivity
import com.medtroniclabs.opensource.ui.household.viewmodel.HouseRegistrationViewModel
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewDefinedParams
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MemberRegistrationFragment : Fragment(), FormEventListener, View.OnClickListener {

    private lateinit var binding: FragmentMemberRegistrationBinding
    private lateinit var formGenerator: FormGenerator
    private val memberRegistrationViewModel: MemberRegistrationViewModel by activityViewModels()
    private val householdRegistrationViewModel: HouseRegistrationViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentMemberRegistrationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeView()
        setListener()
        initializeFlow()
        attachObserver()
        handleAddNewMember()
        val eventType =
            if (householdRegistrationViewModel.isMemberRegistration || householdRegistrationViewModel.memberID != -1L)
                EditNewMember
            else
                AnalyticsDefinedParams.MemberRegistration
        householdRegistrationViewModel.eventName = eventType
        memberRegistrationViewModel.setUserJourney(eventType)
        onPhuAddMember()
    }

    private fun initializeFlow() {
        arguments?.let {
            memberRegistrationViewModel.medicalReviewFlow =
                it.getBoolean(MedicalReviewDefinedParams.MEDICAL_REVIEW_ADD_MEMBER)
        } ?: false
        if (memberRegistrationViewModel.medicalReviewFlow) {
            binding.bottomNavigationView.gone()
        } else {
            binding.bottomNavigationView.visible()
        }
        memberRegistrationViewModel.getFormData(HOUSEHOLD_MEMBER_REGISTRATION)
    }


    private fun attachObserver() {
        if (memberRegistrationViewModel.medicalReviewFlow) {
            householdRegistrationViewModel.villageListResponse.observe(viewLifecycleOwner) { resourceState ->
                when (resourceState.state) {
                    ResourceState.SUCCESS -> {
                        resourceState.data?.let { data ->
                            memberRegistrationViewModel.villageDetails =
                                data.response as List<VillageEntity>
                            formGenerator.spinnerDataInjection(data, getResultSpinnerMapList(data))
                        }
                    }

                    ResourceState.LOADING -> {
                        (activity as BaseActivity?)?.showLoading()
                    }

                    ResourceState.ERROR -> {
                        (activity as BaseActivity?)?.hideLoading()
                    }
                }
            }
        }

        memberRegistrationViewModel.memberRegistrationLiveData.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    (activity as BaseActivity?)?.showLoading()
                }

                ResourceState.ERROR -> {
                    (activity as BaseActivity?)?.hideLoading()

                }

                ResourceState.SUCCESS -> {
                    val (title, startDate) = if (memberRegistrationViewModel.addNewMember) {
                        val type =
                            if (householdRegistrationViewModel.isMemberRegistration && householdRegistrationViewModel.memberID != -1L) {
                                EditNewMember
                            } else {
                                AddNewMember
                            }
                        type to (UserDetail.startDateTime ?: "")
                    } else {
                        AnalyticsDefinedParams.HouseholdCreation to (arguments?.getString(
                            AnalyticsDefinedParams.StartDate
                        ) ?: "")
                    }
                    memberRegistrationViewModel.setAnalyticsData(
                        startDate,
                        eventName = title,
                        isCompleted = true
                    )

                    (activity as BaseActivity?)?.hideLoading()
                    resourceState.data?.let {
                        if (arguments?.getBoolean(HouseholdDefinedParams.isPhuWalkInsFlow) == true) {
                            requireActivity().startBackgroundOfflineSync()
                            val existingFragment =
                                childFragmentManager.findFragmentByTag(
                                    SuccessDialogFragment.TAG
                                )
                            if (existingFragment == null) {
                                SuccessDialogFragment.newInstance(isPhuLink = true)
                                    .show(childFragmentManager, SuccessDialogFragment.TAG)
                            }
                        } else {
                            launchSummaryOrAssessmentPage()
                        }

                    }
                }
            }
        }

        memberRegistrationViewModel.formLayoutsLiveData.observe(viewLifecycleOwner) { resources ->
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
                        handleRelationshipSpinner()
                    }
                }

                ResourceState.ERROR -> {
                    (activity as? BaseActivity)?.hideLoading()
                }
            }
        }

        memberRegistrationViewModel.memberDetailsLiveData.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    (activity as BaseActivity?)?.showLoading()
                }

                ResourceState.SUCCESS -> {
                    (activity as BaseActivity?)?.hideLoading()
                    resourceState.data?.let { data ->
                        autoPopulateDetails(data)
                    }
                }

                ResourceState.ERROR -> {
                    (activity as BaseActivity?)?.hideLoading()
                }
            }
        }
        memberRegistrationViewModel.addnewMemberReq.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    (activity as BaseActivity?)?.showLoading()
                }

                ResourceState.SUCCESS -> {
                    (activity as BaseActivity?)?.hideLoading()
                    setFragmentResult(
                        MedicalReviewDefinedParams.MEMBER_REG, bundleOf(
                            MedicalReviewDefinedParams.Notes to true
                        )
                    )
                }

                ResourceState.ERROR -> {
                    (activity as BaseActivity?)?.hideLoading()
                }
            }
        }
    }

    private fun launchSummaryOrAssessmentPage() {
        requireActivity().startBackgroundOfflineSync()
        if (memberRegistrationViewModel.startAssessment != null && memberRegistrationViewModel.startAssessment!!) {
            val intent = Intent(requireActivity(), AssessmentToolsActivity::class.java)
            memberRegistrationViewModel.memberRegistrationLiveData.value?.data.let {
                intent.putExtra(MemberID, it ?: -1)
            }
            intent.putExtra(DOB, memberRegistrationViewModel.memberDob)
            startActivity(intent)
            (activity as HouseholdActivity).finish()
        } else {
            if (!householdRegistrationViewModel.isMemberRegistration) {
                val intent = Intent(
                    requireActivity(), HouseholdSummaryActivity::class.java
                )
                intent.putExtra(
                    HouseholdDefinedParams.ID,
                    memberRegistrationViewModel.selectedHouseholdId
                )
                intent.putExtra(
                    HouseholdDefinedParams.isFromHouseHoldRegistration,
                    memberRegistrationViewModel.memberDetailsLiveData.value?.data?.id == null
                )
                startActivity(intent)
            }
            (activity as HouseholdActivity).finish()
        }
    }

    private fun launchHouseholdSummary() {

        (activity as HouseholdActivity).finish()
    }

    private fun autoPopulateDetails(details: HouseholdMemberEntity) {
        details.householdId?.let { id ->
            householdRegistrationViewModel.householdId = id
        }
        formGenerator.getViewByTag(name)?.let { view ->
            formGenerator.setValueForView(details.name, view)
        }

        val canDisableHHRelation = !(arguments?.getBoolean(HouseholdDefinedParams.isPhuWalkInsFlow) == true || details.householdHeadRelationship.isEmpty())

        if (canDisableHHRelation) {
            formGenerator.getViewByTag(householdHeadRelationship)?.let { view ->
                val relationship =
                    if (details.householdHeadRelationship.contains(getString(R.string.separator_hyphen))) {
                        details.householdHeadRelationship.substringBefore(getString(R.string.separator_hyphen))
                    } else details.householdHeadRelationship
                view.isEnabled = false
                formGenerator.setValueForView(relationship, view)
            }
            formGenerator.getViewByTag(otherFamilyMember)?.let { view ->
                val relationship =
                    if (details.householdHeadRelationship.contains(getString(R.string.separator_hyphen))) {
                        details.householdHeadRelationship.substringAfter(getString(R.string.separator_hyphen))
                    } else details.householdHeadRelationship
                formGenerator.disableView(view, requireContext())
                formGenerator.setValueForView(relationship, view)
            }
        } else {
            val view =
                formGenerator.getViewByTag(DefinedParams.HouseholdHeadRelationship) as AppCompatSpinner
            (view.adapter as CustomSpinnerAdapter).removeItemById(HouseholdHead)
        }

        formGenerator.getViewByTag(phoneNumber)?.let { view ->
            formGenerator.setValueForView(details.phoneNumber, view)
        }
        formGenerator.getViewByTag(phoneNumberCategory)?.let { view ->
            formGenerator.setValueForView(details.phoneNumberCategory, view)
        }
        details.gender.let {
            when (it) {
                male -> {
                    singleSelectValueOption(
                        male,
                        gender
                    )
                }

                female -> {
                    singleSelectValueOption(
                        female,
                        gender
                    )
                }

                else -> {}
            }
            if (details.gender.isNotBlank()) {
                formGenerator.disableSingleSelection(gender)
            }
        }
        details.isPregnant?.let {
            when (getBooleanAsString(it)) {
                yes -> {
                    singleSelectValueOption(
                        Yes.lowercase(),
                        isPregnant
                    )
                }

                no -> {
                    singleSelectValueOption(
                        No.lowercase(),
                        isPregnant
                    )
                }
            }
        }
        details.dateOfBirth.let {
            val dateOfBirth =
                DateUtils.convertDateFormat(it, DATE_FORMAT_yyyyMMddHHmmssZZZZZ, DATE_ddMMyyyy)
            val dateDob = DateUtils.convertStringToDate(it, DATE_FORMAT_yyyyMMddHHmmssZZZZZ)
            formGenerator.getViewByTag(MemberRegistration.dateOfBirth)?.let { view ->
                if (dateOfBirth.isNotBlank()) {
                    formGenerator.disableView(view, requireContext())
                }
                formGenerator.setValueForView(dateOfBirth, view)
            }

            dateDob?.let { dob ->
                formGenerator.fillDetailsOnDatePickerSet(dob, false)
            }

        }
    }


    private fun singleSelectValueOption(value: String, key: String) {
        formGenerator.getViewByTag("${value}_${key}")
            ?.let { view ->
                if (view is TextView) {
                    view.isSelected = true
                    view.performClick()
                }
            }
    }


    private fun setListener() {
        binding.btnSubmit.setOnClickListener(this)
        binding.btnStartAssessment.setOnClickListener(this)
        binding.btnSubmitPhu.setOnClickListener(this)
    }

    private fun initializeView() {
        formGenerator = FormGenerator(
            requireContext(), binding.llForm, null, this, binding.scrollView, translate = SecuredPreference.getIsTranslationEnabled()
        )
    }

    private fun handleRelationshipSpinner() {
        val view =
            formGenerator.getViewByTag(DefinedParams.HouseholdHeadRelationship) as AppCompatSpinner
        householdRegistrationViewModel.householdEntityDetail?.let {
            if (it.id == 0L) {
                val index =
                    (view.adapter as CustomSpinnerAdapter).getIndexOfItemById(HouseholdHead)
                view.setSelection(index, true)
                view.isEnabled = false
                householdRegistrationViewModel.householdEntityDetail?.let { details ->
                    formGenerator.getViewByTag(phoneNumber)?.let { view ->
                        formGenerator.setValueForView(details.headPhoneNumber, view)
                        updateMobileNumberCategoryForHead(details.headPhoneNumberCategory)
                    }
                }
            }
        } ?: kotlin.run {
            if (householdRegistrationViewModel.memberID == -1L) {
                (view.adapter as CustomSpinnerAdapter).removeItemById(HouseholdHead)
            }
        }
    }

    private fun updateMobileNumberCategoryForHead(category: String?) {
        category?.let {
            val view = formGenerator.getViewByTag(headPhoneNumberCategory) as AppCompatSpinner
            val index = (view.adapter as CustomSpinnerAdapter).getIndexOfItemById(it)
            view.setSelection(index, true)
            view.isEnabled = false
        }
    }

    private fun handleAddNewMember() {
        memberRegistrationViewModel.addNewMember =
            arguments?.getBoolean(AddNewMember, false) ?: false
        if (memberRegistrationViewModel.addNewMember) {
            UserDetail.startDateTime = CommonUtils.getCurrentDateTimeInLocalTime()
            UserDetail.eventName = AddNewMember
        } else {
            UserDetail.startDateTime = CommonUtils.getCurrentDateTimeInLocalTime()
            UserDetail.eventName = EditNewMember
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
        id: String, serverViewModel: FormLayout, resultMap: Any?
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
            if (memberRegistrationViewModel.medicalReviewFlow) {
                memberRegistrationViewModel.addNewMember(map, formGenerator)
            } else {
                if (householdRegistrationViewModel.isMemberRegistration || householdRegistrationViewModel.memberID != -1L) {
                    if (memberRegistrationViewModel.isPhuWalkInsFlow == true) {
                        householdRegistrationViewModel.updateMemberAsAssigned(arguments?.getLong(com.medtroniclabs.opensource.common.DefinedParams.FhirMemberID))
                    }
                    memberRegistrationViewModel.registerMember(
                        map,
                        householdRegistrationViewModel.householdId
                    )
                } else {
                    householdRegistrationViewModel.householdEntityDetail?.let { householdEntity ->
                        memberRegistrationViewModel.registerHouseThenMember(
                            householdEntity,
                            map,
                            householdRegistrationViewModel.getCurrentLocation(),
                            householdRegistrationViewModel.initialValue,
                            householdRegistrationViewModel.signatureFilename
                        )
                    }
                }
            }
        }
    }

    override fun onRenderingComplete() {
        val view = formGenerator.getViewByTag(villageId + formGenerator.rootSuffix)
        val relationSipView =
            formGenerator.getViewByTag(MedicalReviewDefinedParams.HH_RELATIONSHIP + formGenerator.rootSuffix)
        if (memberRegistrationViewModel.medicalReviewFlow) {
            view?.visible()
            relationSipView?.gone()
        } else {
            view?.gone()
            relationSipView?.visible()
            if (householdRegistrationViewModel.memberID != -1L) {
                memberRegistrationViewModel.getMemberDetailsByID(householdRegistrationViewModel.memberID)
            }
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
        formGenerator.handlePregnancyCardBasedOnAgeAndWeeks()
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

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnStartAssessment -> {
                memberRegistrationViewModel.startAssessment = true
                formGenerator.formSubmitAction(v)
            }

            R.id.btnSubmit -> {
                memberRegistrationViewModel.startAssessment = false
                formGenerator.formSubmitAction(v)
            }

            R.id.btnSubmitPhu -> {
                memberRegistrationViewModel.startAssessment = false
                formGenerator.formSubmitAction(v)
            }
        }
    }


    // on MR add new member submit
    fun medicalReviewAddMember(v: View) {
        formGenerator.formSubmitAction(v)
    }

    private fun onPhuAddMember() {
        memberRegistrationViewModel.isPhuWalkInsFlow =
            arguments?.getBoolean(HouseholdDefinedParams.isPhuWalkInsFlow, false)
        if (memberRegistrationViewModel.isPhuWalkInsFlow == true) {
            binding.bottomNavigationView.gone()
            binding.bottomNavigationViewPhuSubmit.visible()
        }
        val scrollView = binding.scrollView
        val bottomNavigationView = binding.bottomNavigationView
        val bottomNavigationViewPhuSubmit = binding.bottomNavigationViewPhuSubmit

        bottomNavigationView.viewTreeObserver.addOnGlobalLayoutListener {
            val layoutParams = scrollView.layoutParams as ConstraintLayout.LayoutParams

            if (bottomNavigationView.visibility == View.GONE) {
                layoutParams.bottomToTop = bottomNavigationViewPhuSubmit.id
            } else {
                layoutParams.bottomToTop = bottomNavigationView.id
            }
            scrollView.layoutParams = layoutParams
        }
    }


}