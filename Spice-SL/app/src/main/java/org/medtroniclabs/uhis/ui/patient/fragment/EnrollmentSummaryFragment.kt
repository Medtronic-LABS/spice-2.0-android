package org.medtroniclabs.uhis.ui.patient.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.addCallback
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.activityViewModels
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.model.PatientDetails
import org.medtroniclabs.uhis.data.offlinesync.model.ProvanceDto
import org.medtroniclabs.uhis.data.registration.InitialDiagnosis
import org.medtroniclabs.uhis.data.registration.PatientCreateResponse
import org.medtroniclabs.uhis.databinding.CardLayoutBinding
import org.medtroniclabs.uhis.databinding.FragmentEnrollmentSummaryBinding
import org.medtroniclabs.uhis.databinding.SummaryLayoutBinding
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.ncd.data.PatientVisitRequest
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.landing.LandingActivity
import org.medtroniclabs.uhis.ui.patient.EnrollmentFormBuilderActivity
import org.medtroniclabs.uhis.ui.patient.IntentConstants
import org.medtroniclabs.uhis.ui.patient.NurseMedicalReviewActivity
import org.medtroniclabs.uhis.ui.patient.UIConstants
import org.medtroniclabs.uhis.ui.patient.viewmodel.EnrollmentFormBuilderViewModel
import kotlin.getValue
import kotlin.toString

class EnrollmentSummaryFragment : BaseFragment(), View.OnClickListener {
    lateinit var binding: FragmentEnrollmentSummaryBinding
    private val viewModel: EnrollmentFormBuilderViewModel by activityViewModels()
    private val bioDataCardIndex = 1
    private val resultCardIndex = 2
    private val treatmentCardIndex = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val callback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            (activity as BaseActivity).startAsNewActivity(
                Intent(
                    activity,
                    LandingActivity::class.java,
                ),
            )
        }
        callback.isEnabled = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentEnrollmentSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        setTitle(requireContext().getString(R.string.summary))
        hideHomeIcon()
        setListeners()
        addChildViews()
        setObserver()
    }

    private fun setListeners() {
        binding.findHelpButton.visibility = View.GONE
        binding.bottomNavigationView.visibility = View.VISIBLE
        binding.findHelpButton.safeClickListener(this)
        binding.actionButton.safeClickListener(this)
        binding.actionButtonFollowUp.safeClickListener(this)
        binding.bottomNavigationView.visibility = View.VISIBLE
        binding.btnFollowUp.safeClickListener(this)
    }

    private fun addChildViews() {
        binding.llRoot.removeAllViews()
        addCardView(getString(R.string.result), resultCardIndex)
    }

    private fun setObserver() {
        viewModel.patientVisitIDResponse.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                    resourceState?.message?.let { message ->
                        (activity as BaseActivity).showErrorDialogue(
                            getString(R.string.error),
                            message,
                            isNegativeButtonNeed = false,
                        ) {}
                    }
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    resourceState.data?.let { patientDetails ->
                        startNewReviewActivity(patientDetails)
                    }
                }
            }
        }
    }

    private fun startNewReviewActivity(details: PatientDetails) {
        val intent = Intent(
            requireContext(),
            NurseMedicalReviewActivity::class.java,
        )
        intent.putExtra(IntentConstants.INTENT_PATIENT_ID, details.patientID)
        intent.putExtra(IntentConstants.INTENT_PATIENT_ID_STRING, details.patientIdString)
        intent.putExtra(IntentConstants.INTENT_VISIT_ID, details.visitID)
        intent.putExtra(IntentConstants.INTENT_ENCOUNTER_REFERENCE, details.encounterReference)
        intent.putExtra(IntentConstants.INTENT_MEMBER_REFERENCE, details.memberReference)
        intent.putExtra(IntentConstants.INTENT_PATIENT_REFERENCE, details.patientReference)
        intent.putExtra(IntentConstants.INTENT_INITIAL_REVIEW, details.initialReview)
        intent.putExtra(IntentConstants.SHOW_CONTINUOUS_MEDICAL_REVIEW, false)
        intent.putExtra(DefinedParams.ORIGIN, UIConstants.MY_PATIENTS_UNIQUE_ID)
        requireActivity().finish()
        startActivity(intent)
    }

    private fun addCardView(
        cardTitle: String,
        cardCode: Int,
        cardColor: Int? = null,
        textColor: Int? = null,
    ) {
        setCardViewEdit(cardTitle, cardColor, textColor, cardCode)
    }

    private fun setCardViewEdit(
        cardTitle: String,
        cardColor: Int?,
        textColor: Int?,
        cardCode: Int,
    ) {
        val cardBinding = CardLayoutBinding.inflate(layoutInflater)
        cardBinding.cardTitle.text = cardTitle
        cardColor?.let {
            cardBinding.viewCardBG.setBackgroundColor(it)
        }
        textColor?.let {
            cardBinding.cardTitle.setTextColor(it)
        }
        viewModel.enrollPatientLiveData.value?.data?.let {
            inflateCardChild(
                cardCode,
                cardBinding.llFamilyRoot,
                it,
            )
        }
        if (cardBinding.llFamilyRoot.childCount > 0) {
            binding.llRoot.addView(cardBinding.root)
        }
    }

    private fun inflateCardChild(
        cardCode: Int,
        llFamilyRoot: LinearLayout,
        responseModel: PatientCreateResponse,
    ) {
        addBioDataCardDetails(llFamilyRoot, responseModel)
        addResultCardDetails(llFamilyRoot, responseModel)
        addHealthHistoryCard(llFamilyRoot, responseModel)
    }

    private fun addResultCardDetails(
        llFamilyRoot: LinearLayout,
        responseModel: PatientCreateResponse,
    ) {
        llFamilyRoot.let { layout ->

            val diagnosis = mutableListOf<String>()
            if (responseModel.patientDiagnosisStatus?.isDiabetesDiagnosis == true) {
                diagnosis.add(DefinedParams.DIABETES)
            }

            if (responseModel.patientDiagnosisStatus?.isHtnDiagnosis == true) {
                diagnosis.add(DefinedParams.HYPERTENSION)
            }

            if (diagnosis.isNotEmpty()) {
                val diagnosisText = diagnosis.joinToString(separator = ", ")
                layout.addView(inflateChildView(getString(R.string.diagnoses), diagnosisText))
            } else {
                layout.addView(inflateChildView(getString(R.string.diagnoses), getString(R.string.none)))
            }
        }
    }

    private fun addHealthHistoryCard(
        llFamilyRoot: LinearLayout,
        responseModel: PatientCreateResponse,
    ) {
        llFamilyRoot.let { layout ->

            val history = mutableListOf<String>()
            responseModel.patientHealthHistory?.let {
                if (it.heartAttack?.equals(DefinedParams.YES, true) == true) {
                    history.add(DefinedParams.HEART_ATTACK)
                }

                if (it.stroke?.equals(DefinedParams.YES, true) == true) {
                    history.add(DefinedParams.STROKE)
                }

                if (it.copd?.equals(DefinedParams.YES, true) == true) {
                    history.add(DefinedParams.COPD)
                }

                if (it.kidneyDisease?.equals(DefinedParams.YES, true) == true) {
                    history.add(DefinedParams.KIDNEY_DISEASE)
                }
            }

            if (history.isNotEmpty()) {
                val diagnosisText = history.joinToString(separator = ", ")
                layout.addView(inflateChildView(getString(R.string.health_history), diagnosisText, textColor = R.color.attention_color))
            } else {
                layout.addView(inflateChildView(getString(R.string.health_history), getString(R.string.none)))
            }
        }
    }

    private fun addBioDataCardDetails(
        llFamilyRoot: LinearLayout,
        responseModel: PatientCreateResponse,
    ) {
        llFamilyRoot.let { layout ->
            responseModel.apply {
                dateOfEnrollment?.let {
                    layout.addView(
                        inflateChildView(
                            getString(R.string.date_of_registration),
                            DateUtils.convertDateTimeToDate(
                                it,
                                DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                                DateUtils.DATE_DD_MMM_YYYY,
                            ),
                        ),
                    )
                }

                layout.addView(
                    inflateChildView(
                        getString(R.string.program_id),
                        (programId ?: getString(R.string.hyphen_symbol)).toString(),
                    ),
                )

                getIdentityLabel(identityType)?.let {
                    layout.addView(inflateChildView(it, identityValue ?: getString(R.string.hyphen_symbol)))
                }

                layout.addView(inflateChildView(getString(R.string.name), name ?: getString(R.string.hyphen_symbol)))

                gender?.let { gender ->
                    layout.addView(
                        inflateChildView(
                            getString(R.string.gender),
                            gender.replaceFirstChar { it.titlecase() },
                        ),
                    )
                }

                age?.let {
                    layout.addView(inflateChildView(getString(R.string.age), it.toString()))
                }

                phoneNumber?.let { phnNo ->
                    val countryCode = SecuredPreference.getUserDetails()?.countryCode ?: ""
                    layout.addView(
                        inflateChildView(
                            getString(R.string.mobile_number),
                            "+$countryCode $phnNo",
                        ),
                    )
                }
                bmi?.let { bmiValue ->
                    val bmiInfo = CommonUtils.getBMIInformation(requireContext(), bmiValue.toDouble())
                    val formattedBmi = CommonUtils.getDecimalFormatted(bmiValue)
                    val bmiText = bmiInfo?.first?.let { category -> "$formattedBmi ($category)" } ?: formattedBmi
                    layout.addView(
                        inflateChildView(
                            getString(R.string.bmi),
                            bmiText,
                            textColor = bmiInfo?.second,
                        ),
                    )
                }

                facilityName?.let {
                    layout.addView(inflateChildView(getString(R.string.facility_name), it))
                }

//                landmark?.let {
//                    layout.addView(inflateChildView(getString(R.string.village_town_city_name), it))
//                }
            }
        }
    }

    private fun getIdentityLabel(identityType: String?): String? =
        when {
            identityType.isNullOrEmpty() || identityType == DefinedParams.NA -> null
            identityType == DefinedParams.IDENTITY_TYPE_BRN -> requireContext().getString(R.string.brn)
            else -> requireContext().getString(R.string.national_id)
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

    companion object {
        @JvmStatic
        fun newInstance() =
            EnrollmentSummaryFragment().apply {
            }
    }

    override fun onClick(mView: View?) {
        when (mView) {
//            binding.findHelpButton -> {
//               // startActivity(Intent(requireContext(), FindHelpActivity::class.java))
//            }

            binding.actionButton -> {
                (activity as EnrollmentFormBuilderActivity).startAsNewActivity(
                    Intent(
                        requireContext(),
                        LandingActivity::class.java,
                    ),
                )
            }
            binding.actionButtonFollowUp -> {
                (activity as EnrollmentFormBuilderActivity).startAsNewActivity(
                    Intent(
                        requireContext(),
                        LandingActivity::class.java,
                    ),
                )
            }

            binding.btnFollowUp -> {
                val data = viewModel.enrollPatientLiveData.value?.data
                // patientReference / track id (for patientvisit/create) come from the numeric
                // patient id, but the medical-review screen looks the patient up by
                // patientUniqueId (returned by the register API), not the FHIR id.
                val patientTrackIdString = data?.id ?: data?.patientId
                val memberReference = data?.memberId
                val patientTrackId = patientTrackIdString?.toLongOrNull()
                val patientUniqueId = data?.patientUniqueId
                if (patientTrackIdString != null &&
                    memberReference != null &&
                    patientTrackId != null &&
                    patientUniqueId != null
                ) {
                    showLoading()
                    viewModel.createPatientVisit(
                        requireContext(),
                        PatientVisitRequest(
                            patientReference = patientTrackIdString,
                            memberReference = memberReference,
                            provenance = ProvanceDto(),
                        ),
                        patientId = patientTrackId,
                        patientIdString = patientUniqueId,
                    )
                } else {
                    (activity as BaseActivity).showErrorDialogue(
                        getString(R.string.error),
                        getString(R.string.something_went_wrong),
                        isNegativeButtonNeed = false,
                    ) {}
                }
            }
        }
    }

    private fun getDiagnoses(diagnosis: InitialDiagnosis): String {
        val diagnoses = mutableListOf<String>()

        // Check if diabetes diagnosis is true
        if (diagnosis.isDiabetesDiagnosis == true) {
            diagnoses.add(DefinedParams.DIABETES)
        }

        // Check if hypertension diagnosis is true
        if (diagnosis.isHtnDiagnosis == true) {
            diagnoses.add(DefinedParams.HYPERTENSION)
        }

        return if (diagnoses.isEmpty()) getString(R.string.hyphen_symbol) else diagnoses.joinToString(", ")
    }
}
