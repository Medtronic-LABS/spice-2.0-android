package org.medtroniclabs.uhis.ui.patient

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.core.content.res.ResourcesCompat
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.common.StringConverter
import org.medtroniclabs.uhis.data.registration.NurseCreateResponse
import org.medtroniclabs.uhis.data.registration.SymptomModels
import org.medtroniclabs.uhis.databinding.ActivityNurseMedicalReviewSummaryBinding
import org.medtroniclabs.uhis.databinding.SummaryLayoutBinding
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams
import org.medtroniclabs.uhis.formgeneration.extension.capitalizeFirstChar
import org.medtroniclabs.uhis.formgeneration.extension.customGetSerializable
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.utility.CustomSpinnerAdapter
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.assessment.viewmodel.AssessmentViewModel
import org.medtroniclabs.uhis.ui.landing.LandingActivity
import org.medtroniclabs.uhis.ui.patient.IntentConstants.NURSE_RESPONE
import org.medtroniclabs.uhis.ui.patient.fragment.NurseBioDataFragment
import org.medtroniclabs.uhis.ui.patient.fragment.PatientTypeDialog
import org.medtroniclabs.uhis.ui.patient.util.CommonDialogInterface
import org.medtroniclabs.uhis.ui.patient.viewmodel.MedicalReviewBaseViewModel
import org.medtroniclabs.uhis.ui.patient.viewmodel.NurseMedicalReviewViewModel
import kotlin.getValue

@AndroidEntryPoint
class NurseMedicalReviewSummaryActivity : BaseActivity(), View.OnClickListener {
    lateinit var binding: ActivityNurseMedicalReviewSummaryBinding
    private val viewModel: MedicalReviewBaseViewModel by viewModels()
    private val assessmentViewModel: AssessmentViewModel by viewModels()
    private val nurseViewModel: NurseMedicalReviewViewModel by viewModels()
    private val resultCardIndex = 2
    private var adapter: CustomSpinnerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNurseMedicalReviewSummaryBinding.inflate(layoutInflater)
        setMainContentView(
            binding.root,
            true,
            homeAndBackVisibility = Pair(false, true),
            callback = {
                redirectToHome()
            },
            callbackHome = {
                redirectToHome()
            },
        )
        initView()
        onObserve()
        assessmentViewModel.getSymptomList()
    }

    override fun consumeImeInsets() = true

    private fun initView() {
        binding.btnSubmit.safeClickListener(this)
        intent.extras?.let { bundle ->
            nurseViewModel.patientIdString = bundle.getString(IntentConstants.INTENT_PATIENT_ID_STRING)
            nurseViewModel.patientId = bundle.getLong(IntentConstants.INTENT_PATIENT_ID, -1L)
            nurseViewModel.patientVisitId = bundle.getLong(IntentConstants.INTENT_VISIT_ID, -1L)
        }

        nurseViewModel.patientIdString?.let {
            nurseViewModel.getPatientDetails(this, it)
        }
    }

    fun onObserve() {
        nurseViewModel.ouSitesLiveData.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    val sites = resourceState.data
                    if (!sites.isNullOrEmpty()) {
                        (intent.extras?.customGetSerializable(NURSE_RESPONE) as Any?)?.let { mrResponse ->
                            if (mrResponse is NurseCreateResponse) {
                                val hashMap = HashMap<String, Any>()
                                mrResponse.id.toString().toLongOrNull()?.let { respId ->
                                    hashMap[DefinedParams.ID] = respId
                                }
                                mrResponse.patientTrackId.toString().toLongOrNull()?.let { respPatientTrackId ->
                                    hashMap[DefinedParams.PATIENT_TRACK_ID] = respPatientTrackId
                                }
                                mrResponse.tenantId.toString().toLongOrNull()?.let { respTenantId ->
                                    hashMap[DefinedParams.TENANT_ID] = respTenantId
                                }
                                PatientTypeDialog
                                    .newInstance(hashMap, sites, commonDialogInterface)
                                    .show(supportFragmentManager, PatientTypeDialog.TAG)
                            }
                        }
                    }
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }
            }
        }
        nurseViewModel.patientDetailsResponse.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.SUCCESS -> {
                    hideLoading()
                    resourceState.data?.let { data ->
                        // patient/details may not carry the initial-review flag (defaults to false),
                        // so fall back to the value forwarded from the medical review screen.
                        val resolvedInitialReview =
                            data.initialReview ||
                                intent.getBooleanExtra(IntentConstants.INTENT_INITIAL_REVIEW, false)
                        viewModel.initialReview = resolvedInitialReview
                        nurseViewModel.initialReview = resolvedInitialReview
                        nurseViewModel.unselectedDiagnosis = data.unselectedDiagnosis
                        nurseViewModel.patientDetailsValue = data
                        loadFragment()
                        nurseViewModel.patientTrackId = data._id
                        nurseViewModel.nurseMrRequestModel.patientTrackId = data._id
                        nurseViewModel.nurseMrRequestModel.patientVisitId =
                            intent.getLongExtra(IntentConstants.INTENT_VISIT_ID, -1L)
                        nurseViewModel.nurseMrRequestModel.tenantId = data.tenantId
                        if (!data.ncdStatus.isNullOrBlank() && data.ncdStatus.equals(DefinedParams.CONTROLLED, true)) {
                            nurseViewModel.fetchOUSiteList()
                        }

                        data.firstName?.let { firstName ->
                            val text = StringConverter.appendTexts(firstText = firstName, data.lastName)
                            setTitle(
                                StringConverter.appendTexts(
                                    firstText = CommonUtils.capitalize(text),
                                    data.age?.toInt().toString(),
                                    data.gender,
                                    separator = "-",
                                ),
                            )
                        }
                        intent.extras?.let { bundle ->
                            nurseViewModel.patientId = bundle.getLong(IntentConstants.INTENT_PATIENT_ID, -1L)
                            nurseViewModel.patientVisitId = bundle.getLong(IntentConstants.INTENT_VISIT_ID, -1L)
                            (bundle.customGetSerializable(NURSE_RESPONE) as Any?)?.let { details ->
                                if (details is NurseCreateResponse) {
                                    inflateCardChild(
                                        resultCardIndex,
                                        binding.llFamilyRoot,
                                        details,
                                    )
                                }
                            }
                        }
                    }
                }

                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                    resourceState.message?.let {
                        showErrorDialogue(
                            getString(R.string.error),
                            it,
                            false,
                        ) {}
                    }
                }
            }
        }
    }

    fun loadFragment() {
        replaceFragment(
            binding.bioDataFragment.id,
            NurseBioDataFragment.TAG,
            NurseBioDataFragment.newInstance(true),
        )
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnSubmit -> {
                showReviewStatus()
            }
        }
    }

    private fun inflateCardChild(
        cardCode: Int,
        llFamilyRoot: LinearLayout,
        responseModel: NurseCreateResponse,
    ) {
        when (cardCode) {
            resultCardIndex -> {
                addResultCardDetails(llFamilyRoot, responseModel)
            }
        }
    }

    private val commonDialogInterface = object : CommonDialogInterface {
        override fun onSuccess(
            confirmedDiagnoses: ArrayList<String>?,
            diagnosisNotes: String?,
        ) {
            showReviewStatus()
        }
    }

    private fun showReviewStatus() {
        startAsNewActivity(
            Intent(
                this@NurseMedicalReviewSummaryActivity,
                LandingActivity::class.java,
            ),
        )
    }

    private fun addResultCardDetails(
        llFamilyRoot: LinearLayout,
        responseModel: NurseCreateResponse,
    ) {
        llFamilyRoot.let { layout ->

            if (nurseViewModel.initialReview) {
                responseModel.symptoms?.let {
                    layout.addView(
                        inflateChildView(
                            getString(R.string.symptoms),
                            getSelectedSymptomsText(it),
                            textColor = getColor(R.color.attention_color),
                        ),
                    )
                } ?: layout.addView(
                    inflateChildView(
                        getString(R.string.symptoms),
                        getString(R.string.hyphen_symbol),
                        textColor = getColor(R.color.attention_color),
                    ),
                )

                responseModel.compliance?.let {
                    layout.addView(
                        inflateChildView(
                            getString(R.string.medical_adherence),
                            it ?: getString(R.string.hyphen_symbol),
                        ),
                    )
                } ?: layout.addView(
                    inflateChildView(
                        getString(R.string.medical_adherence),
                        getString(R.string.hyphen_symbol),
                        textColor = getColor(R.color.attention_color),
                    ),
                )
            }
            responseModel.avgSystolic?.let {
                layout.addView(
                    inflateChildView(
                        getString(R.string.average_bp_text),
                        getString(
                            R.string.average_mmhg_string,
                            CommonUtils.getDecimalFormatted(it),
                            CommonUtils.getDecimalFormatted(responseModel.avgDiastolic),
                        ),
                    ),
                )
            } ?: layout.addView(
                inflateChildView(
                    getString(R.string.average_mmhg_string),
                    getString(R.string.hyphen_symbol),
                    textColor = getColor(R.color.attention_color),
                ),
            )

            responseModel.glucoseLog?.let { list ->
                var formattedString = StringBuilder()
                list.forEachIndexed { index, it ->
                    formattedString.append(
                        "${index + 1}.${
                            CommonUtils.formatGlucoseData(
                                it.glucoseType,
                                it.glucoseValue,
                                it.hba1c,
                                it.ogtt,
                                this.baseContext,
                            )
                        } - ${it.glucoseDate?.let { it1 -> DateUtils.formatDateStringLegacy(it1) }} \n",
                    )
                }
                formattedString?.let {
                    layout.addView(
                        inflateChildView(
                            getString(R.string.blood_glucose),
                            it.toString(),
                            isSpace = true,
                        ),
                    )
                }
            } ?: layout.addView(
                inflateChildView(
                    getString(R.string.blood_glucose),
                    getString(R.string.hyphen_symbol),
                ),
            )

            responseModel.prescriptions?.takeIf { it.isNotEmpty() }?.let { list ->
                val mdp = StringBuilder().apply {
                    list.forEachIndexed { index, it ->
                        append("${index + 1}.${it.medicationName} / ${it.dosageUnitValue}(${it.dosageUnitName}) / ${it.dosageFrequencyName} \n")
                    }
                }
                if (mdp.isNotEmpty()) {
                    layout.addView(
                        inflateChildView(
                            getString(R.string.medication_prescribed_),
                            mdp.toString(),
                        ),
                    )
                }
            } ?: run {
                layout.addView(
                    inflateChildView(
                        getString(R.string.medication_prescribed_),
                        getString(R.string.hyphen_symbol),
                        textColor = getColor(R.color.attention_color),
                    ),
                )
            }
            responseModel.investigations?.takeIf { it.isNotEmpty() }?.let { list ->
                val investigation = StringBuilder()
                list.forEachIndexed { index, it ->
                    investigation.append("${index.plus(1)}.${it.labTestName} \n")
                }
                investigation.let {
                    layout.addView(
                        inflateChildView(
                            getString(R.string.investigation),
                            it.toString(),
                        ),
                    )
                }
            } ?: run {
                layout.addView(
                    inflateChildView(
                        getString(R.string.investigation),
                        getString(R.string.hyphen_symbol),
                        textColor = getColor(R.color.attention_color),
                    ),
                )
            }

            responseModel.nextMedicalReviewDate?.let {
                layout.addView(
                    inflateChildView(
                        getString(R.string.next_follow_up_date),
                        DateUtils.formatDateStringLegacy(it),
                    ),
                )
            } ?: layout.addView(
                inflateChildView(
                    getString(R.string.next_follow_up_date),
                    getString(R.string.hyphen_symbol),
                    textColor = getColor(R.color.attention_color),
                ),
            )
        }
    }

    private fun inflateChildView(
        labelKey: String,
        value: String,
        applyBoldStyle: Boolean? = null,
        textColor: Int? = null,
        isSpace: Boolean = false,
    ): View {
        val summaryBinding = SummaryLayoutBinding.inflate(layoutInflater)
        summaryBinding.tvKey.text = labelKey
        summaryBinding.tvValue.text = value
        summaryBinding.tvRowSeparator.text = ":"
        applyBoldStyle?.let {
            summaryBinding.tvValue.typeface =
                ResourcesCompat.getFont(this, R.font.inter_bold)
        }
        textColor?.let {
            summaryBinding.tvValue.setTextColor(it)
        }
        if (isSpace) {
            summaryBinding.tvValue.setLineSpacing(4f, 1.2f)
        }
        return summaryBinding.root
    }

    private fun getSelectedSymptomsText(list: ArrayList<SymptomModels>): String {
        val resultString = StringBuilder()
        val isTranslationEnabled = SecuredPreference.getIsTranslationEnabled()
        list.forEachIndexed { index, symptomResponse ->

            translateOrNot(symptomResponse, resultString, isTranslationEnabled)
            if (!symptomResponse.otherSymptom.isNullOrBlank()) {
                resultString.append(getString(R.string.empty_space))
                resultString.append(getString(R.string.separator_hyphen))
                resultString.append(getString(R.string.empty_space))
                resultString.append(symptomResponse.otherSymptom.trim().capitalizeFirstChar())
            } else if (!symptomResponse.newWorseningSymptoms.isNullOrBlank()) {
                resultString.append(getString(R.string.empty_space))
                resultString.append(getString(R.string.separator_hyphen))
                resultString.append(getString(R.string.empty_space))
                resultString.append(symptomResponse.newWorseningSymptoms.trim().capitalizeFirstChar())
            } else if (symptomResponse.name.startsWith(
                    DefinedParams.NO_SYMPTOMS,
                    true,
                ) &&
                !symptomResponse.type.isNullOrBlank()
            ) {
                resultString.append(getString(R.string.empty_space))
                resultString.append(getString(R.string.separator_hyphen))
                resultString.append(getString(R.string.empty_space))
                if (isTranslationEnabled) {
                    when (symptomResponse.type.lowercase()) {
                        DefinedParams.HYPERTENSION -> getString(R.string.hypertension)
                        DefinedParams.DIABETES -> getString(R.string.diabetes)
                    }
                } else {
                    resultString.append(symptomResponse.type.trim().capitalizeFirstChar())
                }
            }
            if (index != list.size - 1) {
                resultString.append(getString(R.string.comma_symbol))
            }
            resultString.append(getString(R.string.empty_space))
        }
        return resultString.toString()
    }

    private fun translateOrNot(
        symptomResponse: SymptomModels,
        resultString: StringBuilder,
        translationEnabled: Boolean,
    ) {
        if (translationEnabled) {
            resultString.append(symptomResponse.name?.let { getTranslatedSymptomName(it) })
        } else {
            resultString.append(symptomResponse.name)
        }
    }

    private fun getTranslatedSymptomName(name: String): String {
        val model = assessmentViewModel.symptomListResponse.value?.find { it.symptom == name }
        return name
//        return if (model?.cultureValue != null&& model.cultureValue.isNotEmpty()){
//            model.cultureValue
//        }else{
//            name
//        }
    }
}
