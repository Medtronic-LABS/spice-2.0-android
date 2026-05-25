package org.medtroniclabs.uhis.ui.patient.fragment

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.addCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isNotEmpty
import androidx.fragment.app.activityViewModels
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.convertToUtcDateTime
import org.medtroniclabs.uhis.common.EntityMapper
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.common.qrscanner.QRScanContract
import org.medtroniclabs.uhis.common.qrscanner.QRScanResult
import org.medtroniclabs.uhis.common.qrscanner.QRScannerActivity
import org.medtroniclabs.uhis.data.LocalSpinnerResponse
import org.medtroniclabs.uhis.data.model.RecommendedDosageListModel
import org.medtroniclabs.uhis.data.offlinesync.model.ProvanceDto
import org.medtroniclabs.uhis.data.registration.RequestPatientDetail
import org.medtroniclabs.uhis.data.registration.ResponsePatientDetail
import org.medtroniclabs.uhis.databinding.CardLayoutBinding
import org.medtroniclabs.uhis.databinding.FragmentEnrollmentFormBinding
import org.medtroniclabs.uhis.databinding.SummaryLayoutBinding
import org.medtroniclabs.uhis.formgeneration.FormGenerator
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams.ID
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams.IS_DIABETES_DIAGNOSIS
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams.IS_HTN_DIAGNOSIS
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams.IS_REGULAR_SMOKER
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams.SUB_VILLAGE
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams.UPAZILA
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams.VILLAGE
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams.YES
import org.medtroniclabs.uhis.formgeneration.listener.FormEventListener
import org.medtroniclabs.uhis.formgeneration.model.FormLayout
import org.medtroniclabs.uhis.formgeneration.ui.FormResultComposer
import org.medtroniclabs.uhis.mappingkey.Screening
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
        initView()
        getFormDataForWorkflow()
        setListeners()
        attachObservers()
    }

    private fun addChildViews(response: ResponsePatientDetail) {
        binding.bioDataContainer.removeAllViews()
        addCardView(getString(R.string.bio_data), response)
    }

    private fun addCardView(
        cardTitle: String,
        response: ResponsePatientDetail,
        cardColor: Int? = null,
        textColor: Int? = null,
    ) {
        setCardViewEdit(cardTitle, response, cardColor, textColor)
    }

    private fun setCardViewEdit(
        cardTitle: String,
        response: ResponsePatientDetail,
        cardColor: Int?,
        textColor: Int?,
    ) {
        val cardBinding = CardLayoutBinding.inflate(layoutInflater)
        cardBinding.cardTitle.text = cardTitle
        cardColor?.let {
            cardBinding.viewCardBG.setBackgroundColor(it)
        }
        textColor?.let {
            cardBinding.cardTitle.setTextColor(it)
        }
        inflateCardChild(cardBinding.llFamilyRoot, response)
        if (cardBinding.llFamilyRoot.isNotEmpty()) {
            binding.bioDataContainer.addView(cardBinding.root)
        }
    }

    private fun inflateCardChild(
        llFamilyRoot: LinearLayout,
        response: ResponsePatientDetail,
    ) {
        addBioDataCardDetails(llFamilyRoot, response)
    }

    private fun addBioDataCardDetails(
        llFamilyRoot: LinearLayout,
        response: ResponsePatientDetail,
    ) {
        llFamilyRoot.let { layout ->
            val patientId = response.patientId ?: getString(R.string.hyphen_symbol)
            val name = response.name ?: getString(R.string.hyphen_symbol)
            val phoneNumber = response.phoneNumber ?: getString(R.string.hyphen_symbol)
            val age = response.age?.toString() ?: getString(R.string.hyphen_symbol)
            val gender = response.gender ?: getString(R.string.hyphen_symbol)
            val village = response.villageId ?: getString(R.string.hyphen_symbol)
            val subVillage = response.subVillage ?: getString(R.string.hyphen_symbol)

            layout.addView(inflateChildView(getString(R.string.national_id), patientId))
            layout.addView(inflateChildView(getString(R.string.name), name))
            layout.addView(inflateChildView(getString(R.string.phone_no), phoneNumber))
            layout.addView(inflateChildView(getString(R.string.age), age))
            layout.addView(inflateChildView(getString(R.string.gender), gender))
            layout.addView(inflateChildView(getString(R.string.union), village))
            layout.addView(inflateChildView(getString(R.string.village), subVillage))
        }
    }

    private fun inflateChildView(
        labelKey: String,
        value: String,
        applyBoldStyle: Boolean? = null,
        textColor: Int? = null,
    ): View {
        val summaryBinding = SummaryLayoutBinding.inflate(layoutInflater)
        summaryBinding.tvKey.text = labelKey
        summaryBinding.tvValue.text = value
        summaryBinding.tvRowSeparator.text = ":"
        applyBoldStyle?.let {
            summaryBinding.tvValue.typeface =
                ResourcesCompat.getFont(requireContext(), R.font.inter_bold)
        }
        textColor?.let {
            summaryBinding.tvValue.setTextColor(requireContext().getColor(it))
        }
        return summaryBinding.root
    }

    private fun getFormDataForWorkflow() {
        if (viewModel.isConfirmDiagnosis && viewModel.patientTrackId != -1L) {
            viewModel.patientTrackId?.let {
                val request = RequestPatientDetail(it.toString())
                patientDetailsViewModel.getScreeningDetails(request)
            }
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
                    resourceState.data?.let {
                        addChildViews(it)
                    }
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

        viewModel.localDataCacheResponse.observe(viewLifecycleOwner) { resourceState ->
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

        viewModel.countyCacheResponse.observe(viewLifecycleOwner) { resourceState ->
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

        viewModel.subCountyCacheResponse.observe(viewLifecycleOwner, ::subCountyCacheResponse)
        viewModel.programListResponse.observe(viewLifecycleOwner, ::handleProgramListResponse)
        viewModel.unionCacheResponse.observe(viewLifecycleOwner, ::unionCacheResponse)
        viewModel.villageCacheResponse.observe(viewLifecycleOwner, ::handleVillageCacheResponse)

        viewModel.qrCodeValidationResult.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.SUCCESS -> {
                    resourceState.data?.qrCode?.let { qrCode ->
                        formGenerator.showQRScannedText(qrCode, DefinedParams.QR_CODE)
                    }
                }
                else -> {
                    resourceState.message?.let { message ->
                        formGenerator.showErrorQRScanned(DefinedParams.QR_CODE, message)
                    } ?: kotlin.run {
                        formGenerator.showErrorQRScanned(DefinedParams.QR_CODE)
                    }
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
//        patientDetailsViewModel.screeningDetailResponse.value?.data?.let { screeningLog ->
//            val village = screeningLog[DefinedParams.VILLAGE_NAME]
//            if (village is String?) {
//                formGenerator.spinnerDataInjection(
//                    it,
//                    EntityMapper.getResultSpinnerMapList(it),
//                )
//            }
//        } ?: kotlin.run {
//            formGenerator.spinnerDataInjection(
//                it,
//                EntityMapper.getResultSpinnerMapList(it),
//            )
//        }
    }

    private fun autoPopulateUnion(it: LocalSpinnerResponse) {
        formGenerator.spinnerDataInjection(
            it,
            EntityMapper.getResultSpinnerMapList(it),
        )
//        patientDetailsViewModel.screeningDetailResponse.value?.data?.let { screeningLog ->
//            val unionName = screeningLog[DefinedParams.UNION_NAME]
//            if (unionName is String?) {
//                formGenerator.spinnerDataInjection(
//                    it,
//                    EntityMapper.getResultSpinnerMapList(it),
//                )
//            }
//        } ?: kotlin.run {
//            formGenerator.spinnerDataInjection(
//                it,
//                EntityMapper.getResultSpinnerMapList(it),
//            )
//        }
    }

    private fun autoPopulateUpazila(it: LocalSpinnerResponse) {
        formGenerator.spinnerDataInjection(
            it,
            EntityMapper.getResultSpinnerMapList(it),
        )
//        patientDetailsViewModel.screeningDetailResponse.value?.data?.let { screeningLog ->
//            val unionName = screeningLog["siteId"]
//            if (unionName is Double?) {
//                formGenerator.spinnerDataInjection(
//                    it,
//                    EntityMapper.getResultSpinnerMapList(it),
//                )
//            }
//        } ?: kotlin.run {
//            formGenerator.spinnerDataInjection(
//                it,
//                EntityMapper.getResultSpinnerMapList(it),
//            )
//        }
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
                map[DefinedParams.PATIENT_ID] = it.toString()
            }

            map[DefinedParams.PROVENANCE] = ProvanceDto(modifiedDate = System.currentTimeMillis().convertToUtcDateTime())

            changeToBoolean(map, IS_REGULAR_SMOKER)
            changeToBoolean(map, IS_HTN_DIAGNOSIS)
            changeToBoolean(map, IS_DIABETES_DIAGNOSIS)

            changeToObject(map, UPAZILA)
            changeToObject(map, VILLAGE)
            changeToObject(map, SUB_VILLAGE)

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
                viewModel.enrollPatient(requireContext(), it)
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
        Log.e("TEST", "dddd")
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
            // error block
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
