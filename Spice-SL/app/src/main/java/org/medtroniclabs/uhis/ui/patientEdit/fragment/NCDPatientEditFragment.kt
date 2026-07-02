package org.medtroniclabs.uhis.ui.patientEdit.fragment

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.text.color
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.FormAutofill
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.common.qrscanner.QRScanContract
import org.medtroniclabs.uhis.common.qrscanner.QRScanResult
import org.medtroniclabs.uhis.common.qrscanner.QRScannerActivity
import org.medtroniclabs.uhis.data.model.RecommendedDosageListModel
import org.medtroniclabs.uhis.data.offlinesync.model.ProvanceDto
import org.medtroniclabs.uhis.databinding.FragmentNcdPatientEditBinding
import org.medtroniclabs.uhis.formgeneration.FormGenerator
import org.medtroniclabs.uhis.formgeneration.config.ViewType
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.listener.FormEventListener
import org.medtroniclabs.uhis.formgeneration.model.FormLayout
import org.medtroniclabs.uhis.formgeneration.utility.CheckBoxDialog
import org.medtroniclabs.uhis.mappingkey.Screening
import org.medtroniclabs.uhis.model.PatientListRespModel
import org.medtroniclabs.uhis.ncd.medicalreview.viewmodel.NCDFormViewModel
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.MenuConstants
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams
import org.medtroniclabs.uhis.ui.dialog.GeneralSuccessDialog
import org.medtroniclabs.uhis.ui.mypatients.viewmodel.PatientDetailViewModel
import org.medtroniclabs.uhis.ui.patient.UIConstants
import org.medtroniclabs.uhis.ui.patientEdit.NCDPatientEditActivity
import org.medtroniclabs.uhis.ui.patientEdit.viewModel.NCDPatientEditViewModel

@AndroidEntryPoint
class NCDPatientEditFragment : BaseFragment(), FormEventListener, View.OnClickListener {
    lateinit var binding: FragmentNcdPatientEditBinding

    private val viewModel: NCDPatientEditViewModel by activityViewModels()
    private val patientViewModel: PatientDetailViewModel by activityViewModels()
    private val ncdFormViewModel: NCDFormViewModel by activityViewModels()
    private lateinit var formGenerator: FormGenerator

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentNcdPatientEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initializeFormBuilder()
        attachObserver()
        setListener()
    }

    private fun setListener() {
        binding.actionButton.safeClickListener(this)
    }

    private fun initializeFormBuilder() {
        formGenerator = FormGenerator(
            requireContext(),
            binding.llForm,
            this,
            binding.scrollView,
            translate = SecuredPreference.getIsTranslationEnabled(),
        ) { map, id ->
            if (id == HEIGHT_FIELD || id == WEIGHT_FIELD) {
                renderBmi(map)
            }
        }
        if (CommonUtils.isCommunity()) {
            ncdFormViewModel.getFormFromAsset(requireContext(), REGISTRATION_FORM_ASSET)
        } else {
            ncdFormViewModel.getNCDForm(MenuConstants.REGISTRATION.lowercase())
        }
    }

    private fun attachObserver() {
        ncdFormViewModel.ncdFormResponse.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }

                ResourceState.SUCCESS -> {
                    hideProgress()
                    resourceState.data?.let { list ->
                        val data =
                            list.filter { it.viewType != ViewType.VIEW_TYPE_FORM_CARD_FAMILY && it.isEditable }
                        binding.actionButton.visibility = View.VISIBLE
                        binding.actionButton.isEnabled = data.isNotEmpty()
                        formGenerator.populateEditableViews(list)
                    }
                }
            }
        }

        patientViewModel.patientDetailsLiveData.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }

                ResourceState.SUCCESS -> {
                    hideProgress()
                    resourceState.data?.let {
                        FormAutofill.start(requireContext(), formGenerator, it)
                        prefillHouseNumber(it)
                        if (CommonUtils.isCommunity()) {
                            prefillCommunityFields(it)
                        }
                    }
                }
            }
        }
        viewModel.updatePatientMap.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.ERROR -> {
                    hideProgress()
                    resourceState.message?.let {
                        showErrorDialog(
                            getString(R.string.error),
                            it,
                        )
                    }
                }

                ResourceState.SUCCESS -> {
                    hideProgress()
                    showSuccessDialog()
                }
            }
        }
    }

    private fun showSuccessDialog() {
        GeneralSuccessDialog
            .newInstance(
                getString(R.string.patient_details),
                getString(R.string.patient_detail_updated_successfully),
                okayButton = getString(R.string.done),
                callback = {
                    if (activity is NCDPatientEditActivity) {
                        activity?.apply {
                            setResult(Activity.RESULT_OK, intent)
                            finish()
                        }
                    } else {
                        (activity as BaseActivity).redirectToHome()
                    }
                },
            ).show(childFragmentManager, GeneralSuccessDialog.TAG)
    }

    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                data?.let { resultIntent ->
                }
            }
        }

    override fun onRenderingComplete() {
        /**
         * this method is not used
         */
    }

    override fun onUpdateInstruction(
        id: String,
        selectedId: Any?,
    ) {
        /**
         * this method is not used
         */
    }

    override fun onInformationHandling(
        id: String,
        noOfDays: Int,
        enteredDays: Int?,
        resultMap: HashMap<String, Any>?,
    ) {
        /**
         * this method is not used
         */
    }

    override fun onAgeCheckForPregnancy() {
        /**
         * this method is not used
         */
    }

    override fun handleMandatoryCondition(formLayout: FormLayout?) {
        /**
         * this method is not used
         */
    }

    override fun onAgeUpdateListener(
        age: Int,
        serverData: List<FormLayout>?,
        resultHashMap: HashMap<String, Any>,
    ) {
        /**
         * this method is not used
         */
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
            // Ignore: scanner could not be launched
        }
    }

    private val cameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startScanning()
            }
        }

    private val qrScanLauncher =
        registerForActivityResult(QRScanContract()) { result ->
            result.resultString?.let { formGenerator.showQRScannedText(it, QR_CODE_FIELD) }
        }

    private fun startScanning() {
        qrScanLauncher.launch(
            Intent(requireContext(), QRScannerActivity::class.java).apply {
                putExtra(QRScanResult.REQUEST_FROM, UIConstants.SCREENING_UNIQUE_ID)
            },
        )
    }

    override fun onPopulate(targetId: String) {
        /**
         * this method is not used
         */
    }

    override fun loadLocalCache(
        id: String,
        localDataCache: Any,
        selectedParent: Long?,
    ) {
        if (localDataCache is String) {
        }
    }

    override fun onCheckBoxDialogueClicked(
        id: String,
        formLayout: FormLayout,
        resultMap: Any?,
    ) {
        CheckBoxDialog
            .newInstance(id, resultMap) { map ->
                formGenerator.validateCheckboxDialogue(id, formLayout, map)
            }.show(childFragmentManager, CheckBoxDialog.TAG)
    }

    override fun onInstructionClicked(
        id: String,
        title: String,
        informationList: ArrayList<String>?,
        description: String?,
        dosageListModel: ArrayList<RecommendedDosageListModel>?,
    ) {
        /**
         * this method is not used
         */
    }

    override fun onFormSubmit(
        resultMap: HashMap<String, Any>?,
        serverData: List<FormLayout>?,
    ) {
        val map = HashMap<String, Any>()
        if (resultMap != null) {
            map[DefinedParams.BioData] = resultMap as HashMap<String, Any>
        }
        map[DefinedParams.HealthFacilityFhirId] = SecuredPreference.getOrganizationFhirId()
        // memberReference = member FHIR id (data.id), patientReference = patient FHIR id
        // (data.patientId). Prefer the loaded patient details (authoritative, matches the NCD
        // medical-review flow); fall back to the intent extras only if the details are missing.
        val memberReference = patientViewModel.getPatientMemberId()
        val patientReference = patientViewModel.getPatientFHIRId()
        memberReference?.let { map[AssessmentDefinedParams.memberReference] = it }
        patientReference?.let { map[AssessmentDefinedParams.patientReference] = it }
        map[DefinedParams.Provenance] = ProvanceDto()
        if (connectivityManager.isNetworkAvailable()) {
            viewModel.ncdUpdatePatientDetail(map)
        } else {
            (activity as? BaseActivity)?.showErrorDialogue(
                getString(R.string.error),
                getString(R.string.no_internet_error),
                isNegativeButtonNeed = false,
            ) {}
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.actionButton.id -> {
                formGenerator.formSubmitAction(view)
            }
        }
    }

    /**
     * Patient details API returns `houseHoldNumber`; the edit form field id is `houseNumber`.
     */
    private fun prefillHouseNumber(model: PatientListRespModel) {
        model.houseHoldNumber?.takeIf { it.isNotBlank() }?.let {
            setEditTextValue(HOUSE_NUMBER_FIELD, it)
        }
    }

    /**
     * Generic [FormAutofill] only handles plain string EditText/Spinner and string single-selections.
     * The community edit form ([REGISTRATION_FORM_ASSET]) also carries field types that need explicit
     * prefill: the Date of Birth ([ViewType.VIEW_TYPE_FORM_AGE_OR_DOB]), numeric height/weight, and the
     * boolean Yes/No single-selections (smoking). This fills those from the patient details.
     */
    private fun prefillCommunityFields(model: PatientListRespModel) {
        model.gender?.let { selectSingleOption(it, GENDER_FIELD) }
        model.isPregnant?.let { selectSingleOption(it.toString(), IS_PREGNANT_FIELD) }
        (model.birthDate ?: model.dateOfBirth)?.let { prefillDateOfBirth(it) }
        numberToInput(model.height)?.let { setEditTextValue(HEIGHT_FIELD, it) }
        numberToInput(model.weight)?.let { setEditTextValue(WEIGHT_FIELD, it) }
        model.isRegularSmoker?.let {
            selectSingleOption(if (it) DefinedParams.YES else DefinedParams.NO, IS_REGULAR_SMOKER_FIELD)
        }
        model.patientHealthHistory?.let { history ->
            history.heartAttack?.let { selectSingleOption(it, HEART_ATTACK_FIELD) }
            history.stroke?.let { selectSingleOption(it, STROKE_FIELD) }
            history.kidneyDisease?.let { selectSingleOption(it, KIDNEY_DISEASE_FIELD) }
            history.copd?.let { selectSingleOption(it, COPD_FIELD) }
        }
        // QR is mandatory at enrollment, so an existing patient already has one linked. The
        // patient-details API doesn't return the value, so show the linked state (re-scan stays
        // available) without seeding a value that would overwrite the link on submit.
        formGenerator.markQrAlreadyLinked(QR_CODE_FIELD)
    }

    private fun renderBmi(resultHashMap: HashMap<String, Any>) {
        val bmiView = formGenerator.getViewByTag(Screening.BMI) as? AppCompatTextView ?: return
        val weight = resultHashMap[Screening.Weight] as? Double
        val height = resultHashMap[Screening.Height] as? Double
        if (weight == null || height == null) {
            bmiView.text = getString(R.string.hyphen_symbol)
            formGenerator.removeIfContains(Screening.BMI)
            return
        }
        val bmi = CommonUtils.getBMIForNcd(height, weight)
        CommonUtils.getBMIInformation(requireContext(), bmi?.toDoubleOrNull())?.let { info ->
            bmi?.toDoubleOrNull()?.let { resultHashMap[Screening.BMI] = it }
            resultHashMap[Screening.BMI_CATEGORY] = info.first
            bmiView.text = if (bmi == null) {
                getString(R.string.hyphen_symbol)
            } else {
                SpannableStringBuilder()
                    .append(bmi)
                    .color(requireContext().getColor(info.second)) {
                        append(" (${info.first})")
                    }
            }
        }
    }

    private fun prefillDateOfBirth(birthDateUtc: String) {
        val dobFormatted = DateUtils.convertDateFormat(
            birthDateUtc,
            DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
            DateUtils.DATE_ddMMyyyy,
        )
        val dobDate = DateUtils.convertStringToDate(birthDateUtc, DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ)
        formGenerator.getViewByTag(DOB_FIELD)?.let { view ->
            formGenerator.setDobValueForAgeOrDob(DOB_FIELD, birthDateUtc, dobFormatted, view)
            dobDate?.let { formGenerator.fillDetailsOnDatePickerSet(it, false, DOB_FIELD) }
            if (dobFormatted.isNotBlank()) {
                formGenerator.disableView(view)
            }
            formGenerator.hideError(DOB_FIELD)
        }
    }

    private fun setEditTextValue(
        fieldId: String,
        value: String,
    ) {
        formGenerator.getViewByTag(fieldId)?.let { formGenerator.setValueForView(value, it) }
    }

    private fun selectSingleOption(
        optionId: String,
        fieldId: String,
    ) {
        val view = formGenerator.getViewByTag("${optionId}_$fieldId")
        if (view is TextView) {
            view.isSelected = true
            view.performClick()
        }
    }

    private fun numberToInput(value: Double?): String? = value?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() }

    companion object {
        const val TAG = "NCDPatientEditFragment"
        private const val REGISTRATION_FORM_ASSET = "registration.json"
        private const val DOB_FIELD = "dateOfBirth"
        private const val GENDER_FIELD = "gender"
        private const val IS_PREGNANT_FIELD = "isPregnant"
        private const val HEIGHT_FIELD = "height"
        private const val WEIGHT_FIELD = "weight"
        private const val IS_REGULAR_SMOKER_FIELD = "isRegularSmoker"
        private const val HEART_ATTACK_FIELD = "heartAttack"
        private const val STROKE_FIELD = "stroke"
        private const val KIDNEY_DISEASE_FIELD = "kidneyDisease"
        private const val COPD_FIELD = "copd"
        private const val QR_CODE_FIELD = "qrCode"
        private const val HOUSE_NUMBER_FIELD = "houseNumber"
    }
}
