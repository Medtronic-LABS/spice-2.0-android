package org.medtroniclabs.uhis.ui.patient.fragment

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.addCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.text.isDigitsOnly
import androidx.fragment.app.activityViewModels
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.convertToUtcDateTime
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ
import org.medtroniclabs.uhis.common.DateUtils.DATE_ddMMyyyy
import org.medtroniclabs.uhis.common.EntityMapper
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.common.StringConverter
import org.medtroniclabs.uhis.common.qrscanner.QRScanContract
import org.medtroniclabs.uhis.common.qrscanner.QRScanResult
import org.medtroniclabs.uhis.common.qrscanner.QRScannerActivity
import org.medtroniclabs.uhis.data.LocalSpinnerResponse
import org.medtroniclabs.uhis.data.model.RecommendedDosageListModel
import org.medtroniclabs.uhis.data.offlinesync.model.ProvanceDto
import org.medtroniclabs.uhis.data.registration.RequestPatientDetail
import org.medtroniclabs.uhis.databinding.FragmentEnrollmentFormBinding
import org.medtroniclabs.uhis.formgeneration.FormGenerator
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams.CHIEF_DOM
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams.ID
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams.IS_DIABETES_DIAGNOSIS
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams.IS_HTN_DIAGNOSIS
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams.IS_REGULAR_SMOKER
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams.SUB_VILLAGE
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams.VILLAGE
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams.YES
import org.medtroniclabs.uhis.formgeneration.listener.FormEventListener
import org.medtroniclabs.uhis.formgeneration.model.FormLayout
import org.medtroniclabs.uhis.formgeneration.ui.FormResultComposer
import org.medtroniclabs.uhis.formgeneration.utility.CustomSpinnerAdapter
import org.medtroniclabs.uhis.mappingkey.MemberRegistration
import org.medtroniclabs.uhis.mappingkey.Screening
import org.medtroniclabs.uhis.ncd.screening.ui.DuplicationNudgeDialog
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.MenuConstants
import org.medtroniclabs.uhis.ui.common.GeneralInfoDialog
import org.medtroniclabs.uhis.ui.patient.EnrollmentFormBuilderActivity
import org.medtroniclabs.uhis.ui.patient.UIConstants
import org.medtroniclabs.uhis.ui.patient.viewmodel.EnrollmentFormBuilderViewModel
import org.medtroniclabs.uhis.ui.patient.viewmodel.PatientDetailViewModel
import org.medtroniclabs.uhis.ui.patient.viewmodel.ScreeningFormBuilderViewModel

class EnrollmentFormFragmentBD : BaseFragment(), FormEventListener {
    private lateinit var binding: FragmentEnrollmentFormBinding
    private val viewModel: EnrollmentFormBuilderViewModel by activityViewModels()
    private val screeningViewModel: ScreeningFormBuilderViewModel by activityViewModels()
    private val patientDetailsViewModel: PatientDetailViewModel by activityViewModels()
    private lateinit var formGenerator: FormGenerator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val callback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            (activity as EnrollmentFormBuilderActivity).showOnBackPressedAlert()
        }

        callback.isEnabled = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentEnrollmentFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.prefetchNationalIds()
        initView()
        getFormDataForWorkflow()
        setListeners()
        attachObservers()
    }

    private fun getFormDataForWorkflow() {
        if (viewModel.isConfirmDiagnosis && !viewModel.memberReference.isNullOrBlank()) {
            patientDetailsViewModel.getScreeningDetails(RequestPatientDetail(viewModel.memberReference!!))
        } else {
            viewModel.fetchWorkFlow(MenuConstants.MENU_REGISTRATION)
        }
        viewModel.getRiskEntityList()
    }

    private fun initView() {
        // viewModel.setUserJourney(AnalyticsDefinedParams.NCDASSESSMENT)
        formGenerator = FormGenerator(
            requireContext(),
            binding.llForm,
            this,
            binding.scrollView,
            translate = SecuredPreference.getIsTranslationEnabled(),
        ) { map, id ->
            when (id) {
                Screening.Weight, Screening.Height -> {
                    viewModel.renderBMIValue(requireContext(), formGenerator, map)
                }

                DefinedParams.GENDER -> {
                    addOrRemoveGDMOption(map[id] as String)
                }

                DefinedParams.IDENTITY_TYPE -> {
                    val selectedId = map[id] as? String
                    val nationalIdView = formGenerator.getViewByTag(DefinedParams.IDENTITY_VALUE) as? EditText
                    nationalIdView?.let {
                        // nationalIdView.setText("")
                        if (MemberRegistration.IdType.NATIONAL_ID.value == selectedId) {
                            nationalIdView.inputType = InputType.TYPE_CLASS_NUMBER
                            val filters = nationalIdView.filters.toMutableList()
                            filters.add(InputFilter.LengthFilter(MemberRegistration.MAX_LENGTH_NATIONAL_ID))
                            nationalIdView.filters = filters.toTypedArray()
                        } else {
                            nationalIdView.inputType = InputType.TYPE_CLASS_TEXT
                            val filters = nationalIdView.filters.toMutableList()
                            filters.removeIf {
                                it is InputFilter.LengthFilter
                            }
                            nationalIdView.filters = filters.toTypedArray()
                        }
                    }
                    formGenerator.updateNationalIdLabelForIdType(
                        selectedId,
                        SecuredPreference.getIsTranslationEnabled(),
                        optionsViewId = DefinedParams.IDENTITY_TYPE,
                        viewId = DefinedParams.IDENTITY_VALUE,
                    )
                }

                DefinedParams.IDENTITY_VALUE -> {
                    formGenerator.hideError(id)
                }
            }
        }
    }

    fun getCurrentAnsweredStatus(): Boolean = formGenerator.getResultMap().isNotEmpty()

    private fun setListeners() {
        binding.btnSubmit.setOnClickListener {
            formGenerator.formSubmitAction(binding.btnSubmit)
        }
    }

    private fun attachObservers() {
        patientDetailsViewModel.screeningDetailResponse.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }
                ResourceState.SUCCESS -> {
                    viewModel.fetchWorkFlow(MenuConstants.MENU_REGISTRATION)
                }
                ResourceState.ERROR -> {
                    // hideProgress()
                    viewModel.fetchWorkFlow(MenuConstants.MENU_REGISTRATION)
                }
            }
        }

        viewModel.formResponseLiveData.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }
                ResourceState.SUCCESS -> {
                    hideProgress()
                    resourceState.data?.let { data ->
                        formGenerator.populateViews(data)
                    }
                }
                ResourceState.ERROR -> {
                    hideProgress()
                }
            }
        }

        // Upazila
        viewModel.programListResponse.observe(viewLifecycleOwner, ::handleProgramListResponse)

        // Unions
        viewModel.unionCacheResponse.observe(viewLifecycleOwner, ::unionCacheResponse)

        // Villages
        viewModel.villageCacheResponse.observe(viewLifecycleOwner, ::handleVillageCacheResponse)

        viewModel.qrCodeValidationResult.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.SUCCESS -> {
                    resourceState.data?.let { data ->
                        if (data.status && data.qrCode != null) {
                            formGenerator.showQRScannedText(data.qrCode!!, DefinedParams.QR_CODE)
                        } else {
                            formGenerator.showErrorQRScanned(DefinedParams.QR_CODE)
                        }
                    }
                }
                else -> {
                    resourceState.message?.let { message ->
                        formGenerator.showErrorQRScanned(DefinedParams.QR_CODE, getString(R.string.invalid_qr_message))
                    } ?: kotlin.run {
                        formGenerator.showErrorQRScanned(DefinedParams.QR_CODE)
                    }
                }
            }
        }

        viewModel.duplicationNudgeResponse.observe(viewLifecycleOwner) { resources ->
            if (resources.state == ResourceState.ERROR) {
                binding.btnSubmit.isEnabled = true
                hideLoading()
                if (resources.data is Pair<*, *>) {
                    resources.data.first.let { responseMap ->
                        val dialog =
                            DuplicationNudgeDialog.newInstance(
                                StringConverter.convertGivenMapToString(
                                    responseMap,
                                ),
                                isFromEnrollment = true,
                            ) { doAssessment ->
                                viewModel.isFromProceedEnrollment = true
                                formGenerator.formSubmitAction(binding.btnSubmit)
                            }
                        dialog.show(childFragmentManager, DuplicationNudgeDialog.TAG)
                    }
                } else {
                    (activity as? BaseActivity?)?.showErrorDialogue(
                        title = getString(R.string.error),
                        message = resources.message
                            ?: getString(R.string.something_went_wrong_try_later),
                        positiveButtonName = getString(R.string.ok),
                    ) {}
                }
            }
        }

        viewModel.enrollPatientLiveData.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }
                ResourceState.ERROR -> {
                    hideLoading()
                    hideLoading()
                    binding.btnSubmit.isEnabled = true
                    val message = resourceState.message ?: getString(R.string.something_went_wrong_try_later)
                    (activity as BaseActivity).showErrorDialogue(
                        getString(R.string.error),
                        message,
                        isNegativeButtonNeed = false,
                    ) {}
                }
                ResourceState.SUCCESS -> {
                    hideLoading()
                    (activity as EnrollmentFormBuilderActivity).replaceFragmentInId<EnrollmentSummaryFragment>()
                }
            }
        }
    }

    private fun getCountyByTag(tag: String) {
        when (tag) {
            DefinedParams.COUNTY -> patientDetailsViewModel.getCountySubCountyIds()
            DefinedParams.SUB_COUNTY -> patientDetailsViewModel.getSubCountyById()
        }
    }

    private fun subCountyCacheResponse(resourceState: Resource<LocalSpinnerResponse>) {
        when (resourceState.state) {
            ResourceState.SUCCESS -> {
                resourceState.data?.let {
                    getCountyByTag(it.tag)
                    formGenerator.spinnerDataInjection(
                        it,
                        EntityMapper.getResultSpinnerMapList(it),
                    )
                }
            }
            else -> {
                // Invoked if response state is not success
            }
        }
    }

    private fun unionCacheResponse(resourceState: Resource<LocalSpinnerResponse>) {
        when (resourceState.state) {
            ResourceState.SUCCESS -> {
                resourceState.data?.let {
                    autoPopulateUnion(it)
                }
            }
            else -> {
                // Invoked if response state is not success
            }
        }
    }

    private fun autoPopulateVillage(it: LocalSpinnerResponse) {
        formGenerator.spinnerDataInjection(
            it,
            EntityMapper.getResultSpinnerMapList(it),
        )

        patientDetailsViewModel.screeningDetailResponse.value?.data?.subVillageId?.let {
            formGenerator.getViewByTag(DefinedParams.SUB_VILLAGE)?.let { view ->
                formGenerator.setValueForView(it.toLong(), view)
                formGenerator.disableView(view)
            }
        }
    }

    private fun autoPopulateUnion(it: LocalSpinnerResponse) {
        formGenerator.spinnerDataInjection(
            it,
            EntityMapper.getResultSpinnerMapList(it),
        )

        patientDetailsViewModel.screeningDetailResponse.value?.data?.villageId?.let {
            formGenerator.getViewByTag(DefinedParams.VILLAGE)?.let { view ->
                formGenerator.setValueForView(it.toLong(), view)
                formGenerator.disableView(view)
            }
        }
    }

    private fun autoPopulateUpazila(it: LocalSpinnerResponse) {
        formGenerator.spinnerDataInjection(
            it,
            EntityMapper.getResultSpinnerMapList(it),
        )

        patientDetailsViewModel.screeningDetailResponse.value?.data?.chiefdomId?.let {
            formGenerator.getViewByTag(DefinedParams.CHIEF_DOM)?.let { view ->
                formGenerator.setValueForView(it.toLong(), view)
                formGenerator.disableView(view)
            }
        }
    }

    private fun handleProgramListResponse(resourceState: Resource<LocalSpinnerResponse>) {
        when (resourceState.state) {
            ResourceState.SUCCESS -> {
                resourceState.data?.let {
                    autoPopulateUpazila(it)
                }
            }
            else -> {
                // Invoked if response state is not success
            }
        }
    }

    private fun handleVillageCacheResponse(resourceState: Resource<LocalSpinnerResponse>) {
        when (resourceState.state) {
            ResourceState.SUCCESS -> {
                resourceState.data?.let {
                    autoPopulateVillage(it)
                }
            }
            else -> {
                // Invoked if response state is not success
            }
        }
    }

    override fun loadLocalCache(
        id: String,
        localDataCache: Any,
        selectedParent: Long?,
    ) {
        if (localDataCache is String) {
            viewModel.loadDataCacheByType(id, localDataCache, selectedParent)
        }
    }

    override fun onPopulate(targetId: String) {
        Log.e("TEST", "dddd")
    }

    override fun onCheckBoxDialogueClicked(
        id: String,
        formLayout: FormLayout,
        resultMap: Any?,
    ) {
        Log.e("TEST", "dddd")
    }

    override fun onInstructionClicked(
        id: String,
        title: String,
        informationList: ArrayList<String>?,
        description: String?,
        dosageListModel: ArrayList<RecommendedDosageListModel>?,
    ) {
        informationList?.let {
            GeneralInfoDialog
                .newInstance(
                    title,
                    description,
                    it,
                ).show(childFragmentManager, GeneralInfoDialog.TAG)
        }
    }

    override fun onFormSubmit(
        resultMap: HashMap<String, Any>?,
        serverData: List<FormLayout>?,
    ) {
        resultMap?.let { resultHashMap ->
            val map = HashMap<String, Any>(resultHashMap)

            if (formGenerator.isViewVisible(DefinedParams.IDENTITY_VALUE)) {
                val identityValue = map[DefinedParams.IDENTITY_VALUE] as? String
                val identityType = map[DefinedParams.IDENTITY_TYPE] as? String
                if (MemberRegistration.IdType.NATIONAL_ID.value == identityType &&
                    (
                        identityValue == null ||
                            !identityValue.isDigitsOnly() ||
                            !MemberRegistration.NATIONAL_ID_LENGTH.contains(identityValue.length)
                    )
                ) {
                    formGenerator.showErrorAndScrollTo(DefinedParams.IDENTITY_VALUE, getString(R.string.national_id_validation))
                    return
                }

                val originalIdentityValue =
                    patientDetailsViewModel.screeningDetailResponse.value
                        ?.data
                        ?.identityValue
                if (MemberRegistration.IdType.NATIONAL_ID.value == identityType &&
                    identityValue != originalIdentityValue &&
                    viewModel.nationalIdsSet.contains(identityValue)
                ) {
                    formGenerator.showErrorAndScrollTo(DefinedParams.IDENTITY_VALUE, getString(R.string.national_id_already_exists))
                    return
                }
            }

            val isGeneratedNationalId = map.get(DefinedParams.NATIONAL_ID) as? String

            (map).let { bioData ->
                viewModel.isNationalIdGenerated =
                    if (viewModel.nationalId == isGeneratedNationalId || viewModel.nationalId == (-1).toString()) {
                        viewModel.isNationalIdGenerated
                    } else {
                        false
                    }
            }

            if (viewModel.patientInitial.isNotEmpty()) {
                map[DefinedParams.INITIAL] = viewModel.patientInitial
            }

            patientDetailsViewModel.screeningDetailResponse.value?.data?.memberReference?.let {
                map[ID] = it
            }

            viewModel.patientTrackId?.let {
                if (it != -1L) {
                    map[DefinedParams.PATIENT_ID] = it.toString()
                }
            }

            map[DefinedParams.PROVENANCE] = ProvanceDto(modifiedDate = System.currentTimeMillis().convertToUtcDateTime())

            changeToBoolean(map, IS_REGULAR_SMOKER)
            changeToBoolean(map, IS_HTN_DIAGNOSIS)
            changeToBoolean(map, IS_DIABETES_DIAGNOSIS)

            changeToObject(map, CHIEF_DOM)
            changeToObject(map, VILLAGE)
            changeToObject(map, SUB_VILLAGE)

            map[DefinedParams.DATA_OF_BIRTH_UNDERSCORE]?.let {
                map[DefinedParams.DATE_OF_BIRTH] = it
            }

            val result = serverData?.let {
                FormResultComposer().groupValues(
                    serverData = it,
                    map,
                )
            }
            result?.second?.let {
                viewModel.groupedEnrollmentHashMap = it
            }
            result?.first?.let {
                binding.btnSubmit.isEnabled = false
                viewModel.enrollPatient(
                    requireContext(),
                    it,
                    patientTrackerId = viewModel.patientTrackId,
                )
            }
        }
    }

    private fun changeToObject(
        map: HashMap<String, Any>,
        key: String,
    ) {
        map[key]?.let { value ->
            map[key] = mutableMapOf(
                ID to value,
            )
        }
    }

    private fun changeToBoolean(
        map: HashMap<String, Any>,
        key: String,
    ) {
        map[key]?.let {
            map[key] = getBooleanValue(it)
        }
    }

    private fun getBooleanValue(value: Any): Boolean =
        when (value) {
            is String -> value.equals(YES, ignoreCase = true)
            is Boolean -> value
            else -> false
        }

    override fun onRenderingComplete() {
        patientDetailsViewModel.screeningDetailResponse.value?.data?.let { memberDetail ->
            memberDetail.identityType?.let { idType ->
                formGenerator.getViewByTag(DefinedParams.IDENTITY_TYPE)?.let { view ->
                    formGenerator.setValueForView(idType, view)
                }

                memberDetail.identityValue?.let { idValue ->
                    formGenerator.getViewByTag(DefinedParams.IDENTITY_VALUE)?.let { view ->
                        formGenerator.setValueForView(idValue, view)
                    }
                }

                formGenerator.updateNationalIdLabelForIdType(
                    idType,
                    SecuredPreference.getIsTranslationEnabled(),
                    optionsViewId = DefinedParams.IDENTITY_TYPE,
                    viewId = DefinedParams.IDENTITY_VALUE,
                )
            }

            memberDetail.name?.let {
                formGenerator.getViewByTag(DefinedParams.FULL_NAME)?.let { view ->
                    formGenerator.setValueForView(it, view)
                }
            }

            memberDetail.phoneNumber?.let {
                formGenerator.getViewByTag(DefinedParams.PHONE_NUMBER)?.let { view ->
                    formGenerator.setValueForView(it, view)
                }
            }

            memberDetail.phoneNumberCategory?.let {
                formGenerator.getViewByTag(DefinedParams.PHONE_NUMBER_CATEGORY)?.let { view ->
                    formGenerator.setValueForView(it, view)
                }
            }

            memberDetail.gender?.let {
                addOrRemoveGDMOption(it)
                when (it.lowercase()) {
                    DefinedParams.MALE.lowercase() -> {
                        singleSelectValueOption(
                            DefinedParams.MALE,
                            DefinedParams.GENDER,
                        )
                    }

                    DefinedParams.FEMALE.lowercase() -> {
                        singleSelectValueOption(
                            DefinedParams.FEMALE,
                            DefinedParams.GENDER,
                        )
                    }

                    DefinedParams.GENDER_OTHER.lowercase() -> {
                        singleSelectValueOption(
                            DefinedParams.GENDER_OTHER,
                            DefinedParams.GENDER,
                        )
                    }
                }
                formGenerator.disableSingleSelection(DefinedParams.GENDER)
            }

            memberDetail.birthDate?.let { originalDobUtc ->
                val dateOfBirth =
                    DateUtils.convertDateFormat(originalDobUtc, DATE_FORMAT_yyyyMMddHHmmssZZZZZ, DATE_ddMMyyyy)
                val dateDob = DateUtils.convertStringToDate(originalDobUtc, DATE_FORMAT_yyyyMMddHHmmssZZZZZ)

                formGenerator.getViewByTag(DefinedParams.DATE_OF_BIRTH)?.let { view ->
                    if (dateOfBirth.isNotBlank()) {
                        formGenerator.disableView(view)
                    }
                    // Store original UTC value before setting view value for AgeOrDob edit mode
                    formGenerator.setDobValueForAgeOrDob(DefinedParams.DATE_OF_BIRTH, originalDobUtc, dateOfBirth, view)

                    dateDob?.let { dob ->
                        formGenerator.fillDetailsOnDatePickerSet(
                            dob,
                            false,
                        )
                    }
                    formGenerator.hideError(DefinedParams.DATE_OF_BIRTH)
                }
            }

            memberDetail.height?.let {
                formGenerator.getViewByTag(DefinedParams.HEIGHT)?.let { view ->
                    formGenerator.setValueForView(it, view)
                }
            }

            memberDetail.weight?.let {
                formGenerator.getViewByTag(DefinedParams.WEIGHT)?.let { view ->
                    formGenerator.setValueForView(it, view)
                }
            }

            memberDetail.qrCode?.let { qrCode ->
                formGenerator.showHideCardFamily(false, DefinedParams.QR_CARD)
                formGenerator.getResultMap()[DefinedParams.QR_CODE] = qrCode
            }
        }
    }

    private fun singleSelectValueOption(
        value: String,
        key: String,
    ) {
        formGenerator
            .getViewByTag("${value}_$key")
            ?.let { view ->
                if (view is TextView) {
                    view.isSelected = true
                    view.performClick()
                }
            }
    }

    private fun addOrRemoveGDMOption(gender: String) {
        formGenerator.getViewByTag(DefinedParams.DIABETES_DIAGNOSIS)?.let { view ->
            if (view is Spinner) {
                val adapter = view.adapter as? CustomSpinnerAdapter
                adapter?.let {
                    val list = adapter.itemList
                    list.removeAll { it[DefinedParams.NAME] == DefinedParams.GESTATIONAL_DIABETES }

                    if (gender.lowercase() == DefinedParams.FEMALE.lowercase() && list.none { it[DefinedParams.NAME] == DefinedParams.GESTATIONAL_DIABETES }) {
                        list.add(
                            hashMapOf(
                                DefinedParams.CULTURE_VALUE to getString(R.string.gestational_diabetes_gdm),
                                DefinedParams.NAME to DefinedParams.GESTATIONAL_DIABETES,
                                DefinedParams.ID to DefinedParams.GESTATIONAL_DIABETES_ID,
                            ),
                        )
                    }

                    adapter.itemList = list
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    override fun onUpdateInstruction(
        id: String,
        selectedId: Any?,
    ) {
        Log.e("TEST", "dddd")
    }

    override fun onInformationHandling(
        id: String,
        noOfDays: Int,
        enteredDays: Int?,
        resultMap: HashMap<String, Any>?,
    ) {
        Log.e("TEST", "dddd")
    }

    override fun onAgeCheckForPregnancy() {
        Log.e("TEST", "dddd")
    }

    override fun handleMandatoryCondition(formLayout: FormLayout?) {
        Log.e("TEST", "dddd")
    }

    override fun onAgeUpdateListener(
        age: Int,
        serverData: List<FormLayout>?,
        resultHashMap: HashMap<String, Any>,
    ) {
        Log.e("TEST", "dddd")
    }

    override fun onQRScanRequested() {
        try {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED
            ) {
                cameraPermission.launch(Manifest.permission.CAMERA)
            } else {
                startScanning()
            }
        } catch (e: Exception) {
            // error code block
        }
    }

    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            startScanning()
        } else {
            // Camera permission denied
        }
    }

    private val qrScanLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(QRScanContract()) { result ->
            if (result.resultString != null) {
                viewModel.validateQRCode(requireContext(), result.resultString)
            }
        }

    private fun startScanning() {
        qrScanLauncher.launch(
            Intent(requireContext(), QRScannerActivity::class.java).apply {
                putExtra(QRScanResult.REQUEST_FROM, UIConstants.SCREENING_UNIQUE_ID)
            },
        )
    }
}
