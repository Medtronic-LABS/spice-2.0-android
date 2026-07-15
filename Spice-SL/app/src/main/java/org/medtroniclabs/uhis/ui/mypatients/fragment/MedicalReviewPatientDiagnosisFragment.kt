package org.medtroniclabs.uhis.ui.mypatients.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.changePatientStatus
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.invisible
import org.medtroniclabs.uhis.appextensions.setVisible
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils.convertListToString
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.DefinedParams.Lactating
import org.medtroniclabs.uhis.common.DefinedParams.POSTPARTUM
import org.medtroniclabs.uhis.common.DefinedParams.PREGNANT
import org.medtroniclabs.uhis.data.DiseaseCategoryItems
import org.medtroniclabs.uhis.data.model.MotherNeonateAncRequest
import org.medtroniclabs.uhis.databinding.FragmentMedicalReviewPatientDiagnosisBinding
import org.medtroniclabs.uhis.formgeneration.extension.capitalizeFirstChar
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.model.medicalreview.CreateUnderTwoMonthsResponse
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.medicalreview.diagnosis.DiagnosisDialogFragment
import org.medtroniclabs.uhis.ui.medicalreview.diagnosis.viewmodel.DiagnosisViewModel
import org.medtroniclabs.uhis.ui.medicalreview.hiv.fragment.CD4DialogFragment
import org.medtroniclabs.uhis.ui.medicalreview.hiv.fragment.WhoClinicalStageFragment
import org.medtroniclabs.uhis.ui.medicalreview.hiv.viewmodel.WhoClinicalStageViewModel
import org.medtroniclabs.uhis.ui.medicalreview.motherneonate.anc.DialogDismissListener
import org.medtroniclabs.uhis.ui.medicalreview.motherneonate.anc.DialogDismissListenerForTb
import org.medtroniclabs.uhis.ui.medicalreview.motherneonate.anc.MotherNeonateUtil
import org.medtroniclabs.uhis.ui.medicalreview.motherneonate.anc.MotherNeonateUtil.PATIENT_TYPE_HYPHEN
import org.medtroniclabs.uhis.ui.medicalreview.motherneonate.anc.MotherNeonateUtil.calculateBp
import org.medtroniclabs.uhis.ui.medicalreview.motherneonate.anc.fragment.AddBpDialog
import org.medtroniclabs.uhis.ui.medicalreview.tb.fragment.AddBMIDialog
import org.medtroniclabs.uhis.ui.medicalreview.tb.fragment.BMIListDialog
import org.medtroniclabs.uhis.ui.medicalreview.tb.fragment.PatientTypeFragment
import org.medtroniclabs.uhis.ui.medicalreview.tb.fragment.TbConfirmDiagnosisAndSiteOfDiseaseDialog
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewTypeEnums
import org.medtroniclabs.uhis.ui.medicalreview.viewmodel.PatientStatusViewModel
import org.medtroniclabs.uhis.ui.mypatients.viewmodel.MotherNeonateBpWeightViewModel
import org.medtroniclabs.uhis.ui.mypatients.viewmodel.PatientDetailViewModel
import org.medtroniclabs.uhis.ui.mypatients.viewmodel.PregnancyDetailsViewModel

@AndroidEntryPoint
class MedicalReviewPatientDiagnosisFragment :
    BaseFragment(),
    View.OnClickListener,
    DialogDismissListener,
    DialogDismissListenerForTb {
    private lateinit var binding: FragmentMedicalReviewPatientDiagnosisBinding
    private val viewModel: MotherNeonateBpWeightViewModel by activityViewModels()
    private val statusViewModel: PatientStatusViewModel by activityViewModels()
    private val diagnosisViewModel: DiagnosisViewModel by activityViewModels()
    private val patientViewModel: PatientDetailViewModel by activityViewModels()
    private val whoStageViewModel: WhoClinicalStageViewModel by activityViewModels()
    private val pregnancyDetailsViewModel: PregnancyDetailsViewModel by activityViewModels()

    companion object {
        const val TAG: String = "MedicalReviewPatientDiagnosisFragment"

        fun newInstance(): MedicalReviewPatientDiagnosisFragment = MedicalReviewPatientDiagnosisFragment()

        fun newInstance(
            isAnc: Boolean,
            isPnc: Boolean = false,
            patientId: String?,
            memberID: String?,
            id: String?,
            isTB: Boolean = false,
            isHivImrCmr: Boolean = false,
            isFp: Boolean = false,
        ): MedicalReviewPatientDiagnosisFragment {
            val fragment = MedicalReviewPatientDiagnosisFragment()
            fragment.arguments = Bundle().apply {
                putBoolean(DefinedParams.PregnancyANC, isAnc)
                putBoolean(DefinedParams.PREGNANCY_PNC, isPnc)
                putBoolean(DefinedParams.TB, isTB)
                putBoolean(DefinedParams.HIV_IMR_CMR, isHivImrCmr)
                putString(DefinedParams.PatientId, patientId)
                putString(DefinedParams.MEMBER_ID, memberID)
                putString(DefinedParams.ID, id)
                putBoolean(DefinedParams.FP, isFp)
            }
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentMedicalReviewPatientDiagnosisBinding.inflate(inflater, container, false)
        arguments?.let {
            diagnosisViewModel.diagnosisType = getDiagnosisType()
            statusViewModel.patientId = it.getString(DefinedParams.ID)
        }
        return binding.root
    }

    private fun getDiagnosisType(): String =
        arguments?.let {
            if (it.getBoolean(DefinedParams.PregnancyANC)) {
                MedicalReviewTypeEnums.ANC_REVIEW.name
            } else if (it.getBoolean(DefinedParams.PREGNANCY_PNC)) {
                MedicalReviewTypeEnums.PNC_MOTHER_REVIEW.name
            } else if (isTb()) {
                MedicalReviewTypeEnums.TB.name
            } else if (isFp()) {
                MedicalReviewTypeEnums.FP.name
            } else if (isHivImrCmr()) {
                MedicalReviewTypeEnums.HIV_REVIEW.name
            } else {
                it.getString(MedicalReviewTypeEnums.DiagnosisType.name)
            }
        } ?: ""

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews()
        attachObserver()
        handleFlow()
        initializeListeners()
        attachListeners()
        if (patientViewModel.getAncVisit() == 1 && diagnosisViewModel.diagnosisType != MedicalReviewTypeEnums.PNC_MOTHER_REVIEW.name) {
            binding.tvWeightValue.text = MotherNeonateUtil.convertWeight(
                pregnancyDetailsViewModel.pregnancyDetailsModel.weight,
                requireContext(),
            )
            binding.tvBpValue.text =
                calculateBp(
                    pregnancyDetailsViewModel.pregnancyDetailsModel.systolic,
                    pregnancyDetailsViewModel.pregnancyDetailsModel.diastolic,
                    requireContext(),
                )
            binding.tvAddWeight.invisible()
            binding.tvAddBp.invisible()
        }
        if (isHivImrCmr()) {
            binding.tvAddWeight.visible()
        }
    }

    private fun attachListeners() {
        val isTb = isTb()
        val isFp = isFp()
        val progressBar = if (isTb) {
            binding.tbWeightPageProgress
        } else if (isFp) {
            binding.fpWeightPageProgress
        } else {
            binding.pageProgress
        }
        val addWeightText = if (isTb) {
            binding.tvTbAddWeight
        } else if (isFp) {
            binding.tvFpAddWeight
        } else {
            binding.tvAddWeight
        }
        val weightContainer = if (isTb) {
            binding.clTbWeight
        } else if (isFp) {
            binding.clFpWeight
        } else {
            binding.clWeight
        }
        val retryButton =
            if (isTb) {
                binding.retryButtonTbWeight
            } else if (isFp) {
                binding.retryButtonFpWeight
            } else {
                binding.retryButtonWeight
            }
        val weightTextView = if (isTb) {
            binding.tvTbWeight
        } else if (isFp) {
            binding.tvFpWeight
        } else {
            binding.tvWeightValue
        }

        viewModel.getWeight.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> handleLoading(progressBar, addWeightText, weightContainer)

                ResourceState.SUCCESS -> {
                    handleSuccess(progressBar, retryButton, weightContainer, addWeightText)
                    resourceState.data?.let {
                        weightTextView.text =
                            MotherNeonateUtil.convertWeight(it.weight, requireContext())
                    }
                }

                ResourceState.ERROR -> handleError(
                    progressBar,
                    addWeightText,
                    weightContainer,
                    retryButton,
                    weightTextView,
                )
            }
        }

        patientViewModel.patientDetailsLiveData.observe(viewLifecycleOwner) { resource ->
            when (resource.state) {
                ResourceState.SUCCESS -> {
                    resource.data?.let {
                        statusViewModel.getPatientStatusDetails(
                            it,
                            diagnosisViewModel.diagnosisType,
                        )
                    }
                }
                else -> {
                }
            }
        }

        viewModel.getBloodPressure.observe(viewLifecycleOwner) { resourceState ->
            val isTb = isTb()
            val isFp = isFp()
            val progressBar = if (isTb) {
                binding.tbBpPageProgress
            } else if (isFp) {
                binding.fpBpPageProgress
            } else {
                binding.pageProgressBp
            }
            val addBPText = if (isTb) {
                binding.tvTbAddBp
            } else if (isFp) {
                binding.tvFpAddBp
            } else {
                binding.tvAddBp
            }
            val bpContainer = if (isTb) {
                binding.clTbBp
            } else if (isFp) {
                binding.clFpBp
            } else {
                binding.clBp
            }
            val retryButton =
                if (isTb) {
                    binding.retryButtonTbBp
                } else if (isFp) {
                    binding.retryButtonFpBp
                } else {
                    binding.retryButtonBp
                }
            val bpTextView = if (isTb) {
                binding.tvTbBp
            } else if (isFp) {
                binding.tvFpBp
            } else {
                binding.tvBpValue
            }

            when (resourceState.state) {
                ResourceState.LOADING -> {
                    handleLoading(progressBar, addBPText, bpContainer)
                }

                ResourceState.SUCCESS -> {
                    handleSuccess(
                        progressBar,
                        retryButton,
                        bpContainer,
                        addBPText,
                    )
                    resourceState.data?.let {
                        bpTextView.text =
                            calculateBp(it.systolic, it.diastolic, requireContext())
                    }
                }

                ResourceState.ERROR -> {
                    handleError(
                        progressBar,
                        addBPText,
                        bpContainer,
                        retryButton,
                        bpTextView,
                    )
                }
            }
        }

        viewModel.getHeight.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    if (isFp()) {
                        handleLoading(
                            binding.fpHeightPageProgress,
                            binding.tvAddHeightFP,
                            binding.clHeightFP,
                        )
                    } else {
                        handleLoading(
                            binding.tbHeightPageProgress,
                            binding.tvAddHeight,
                            binding.clHeight,
                        )
                    }
                }

                ResourceState.SUCCESS -> {
                    if (isFp()) {
                        handleSuccess(
                            binding.fpHeightPageProgress,
                            binding.retryButtonFPHeightConfirm,
                            binding.clHeightFP,
                            binding.tvAddHeightFP,
                        )
                        resourceState.data?.let {
                            binding.tvHeightLblFP.text = getString(R.string.height)
                            binding.tvHeightFP.text =
                                MotherNeonateUtil.convertHeight(
                                    it.height,
                                    requireContext(),
                                )
                        }
                    } else {
                        handleSuccess(
                            binding.tbHeightPageProgress,
                            binding.retryButtonTbHeightConfirm,
                            binding.clHeight,
                            binding.tvAddHeight,
                        )
                        resourceState.data?.let {
                            binding.tvHeightLbl.text = getString(R.string.height)
                            binding.tvHeight.text =
                                MotherNeonateUtil.convertHeight(
                                    it.height,
                                    requireContext(),
                                )
                        }
                    }
                }

                ResourceState.ERROR -> {
                    if (isFp()) {
                        handleError(
                            binding.fpHeightPageProgress,
                            binding.tvAddHeightFP,
                            binding.clHeightFP,
                            binding.retryButtonFPHeightConfirm,
                            binding.tvHeightFP,
                        )
                    } else {
                        handleError(
                            binding.tbHeightPageProgress,
                            binding.tvAddHeight,
                            binding.clHeight,
                            binding.retryButtonTbHeightConfirm,
                            binding.tvHeight,
                        )
                    }
                }
            }
        }

        viewModel.getBmi.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    if (isFp()) {
                        handleLoading(
                            binding.fpBMIPageProgress,
                            binding.tvFpBmiHistory,
                            binding.clFpBmi,
                        )
                    } else {
                        handleLoading(
                            binding.tbBMIPageProgress,
                            binding.tvBmiHistory,
                            binding.clBmi,
                        )
                    }
                }

                ResourceState.SUCCESS -> {
                    if (isFp()) {
                        handleSuccess(
                            binding.fpBMIPageProgress,
                            binding.retryButtonFpBMI,
                            binding.clFpBmi,
                            binding.tvFpBmiHistory,
                        )
                        resourceState.data?.let {
                            binding.tvFpBmiLbl.text = getString(R.string.bmi)
                            binding.tvFpBmi.text =
                                MotherNeonateUtil.convertBmi(
                                    it.bmi,
                                    requireContext(),
                                )
                        }
                    } else {
                        handleSuccess(
                            binding.tbBMIPageProgress,
                            binding.retryButtonTbBMI,
                            binding.clBmi,
                            binding.tvBmiHistory,
                        )
                        resourceState.data?.let {
                            binding.tvBmiLbl.text = getString(R.string.bmi)
                            binding.tvBmi.text =
                                MotherNeonateUtil.convertBmi(
                                    it.bmi,
                                    requireContext(),
                                )
                        }
                    }
                }

                ResourceState.ERROR -> {
                    if (isFp()) {
                        handleError(
                            binding.fpBMIPageProgress,
                            binding.tvFpBmiHistory,
                            binding.clFpBmi,
                            binding.retryButtonFpBMI,
                            binding.tvFpBmi,
                        )
                    } else {
                        handleError(
                            binding.tbBMIPageProgress,
                            binding.tvBmiHistory,
                            binding.clBmi,
                            binding.retryButtonTbBMI,
                            binding.tvBmi,
                        )
                    }
                }
            }
        }
    }

    private fun handleLoading(
        pageProgress: ProgressBar,
        textView: AppCompatTextView,
        clView: ConstraintLayout,
    ) {
//        pageProgress.visible()
        textView.isEnabled = false
        clView.setBackgroundResource(R.color.grey_bg)
    }

    private fun handleSuccess(
        pageProgress: ProgressBar,
        retryButton: MaterialButton,
        clView: ConstraintLayout,
        textView: AppCompatTextView,
    ) {
        pageProgress.gone()
        retryButton.gone()
        clView.setBackgroundResource(R.color.white)
        textView.isEnabled = true
    }

    private fun handleError(
        pageProgress: ProgressBar,
        textView: AppCompatTextView,
        clView: ConstraintLayout,
        retryButton: MaterialButton,
        textViewError: AppCompatTextView,
    ) {
        pageProgress.gone()
        textView.isEnabled = true
        clView.setBackgroundResource(R.color.white)
        retryButton.visible()
        textViewError.text =
            requireContext().getString(R.string.something_went_wrong)
    }

    private fun isTb(): Boolean = arguments?.getBoolean(DefinedParams.TB, false) ?: false

    private fun isFp(): Boolean = arguments?.getBoolean(DefinedParams.FP, false) ?: false

    private fun isHivImrCmr(): Boolean = arguments?.getBoolean(DefinedParams.HIV_IMR_CMR, false) ?: false

    private fun handleFlow() {
        with(binding) {
            val isAnc = arguments?.getBoolean(DefinedParams.PregnancyANC, false)
            val isPnc = arguments?.getBoolean(DefinedParams.PREGNANCY_PNC, false)
            if (isAnc == false && isPnc == false) {
                cardAddWeight.gone()
                cardBloodPressure.gone()
            } else {
                ancPncFlow(cardAddWeight, cardBloodPressure)
            }
            tvAddWeight.safeClickListener(this@MedicalReviewPatientDiagnosisFragment)
            tvFpAddWeight.safeClickListener(this@MedicalReviewPatientDiagnosisFragment)
            tvAddBp.safeClickListener(this@MedicalReviewPatientDiagnosisFragment)
            tvFpAddBp.safeClickListener(this@MedicalReviewPatientDiagnosisFragment)
            retryButtonBp.safeClickListener(this@MedicalReviewPatientDiagnosisFragment)
            retryButtonFpBp.safeClickListener(this@MedicalReviewPatientDiagnosisFragment)
            retryButtonWeight.safeClickListener(this@MedicalReviewPatientDiagnosisFragment)
            retryButtonFpWeight.safeClickListener(this@MedicalReviewPatientDiagnosisFragment)
            cardDiagnosis.visible()
            cardPatientStatus.visible()
            val isTb = isTb()
            cardSiteDisease.setVisible(isTb && patientViewModel.getTbMedicalReviewStatus())
            cardPatientType.setVisible(isTb && patientViewModel.getTbMedicalReviewStatus())
            if (isTb) {
                binding.tvHeightLbl.text = getString(R.string.height)
                tvTbAddBp.safeClickListener(this@MedicalReviewPatientDiagnosisFragment)
                tvTbAddWeight.safeClickListener(this@MedicalReviewPatientDiagnosisFragment)
                tvAddSiteDisease.safeClickListener(this@MedicalReviewPatientDiagnosisFragment)
                tvAddPatientType.safeClickListener(this@MedicalReviewPatientDiagnosisFragment)
                commonLl.setVisible(patientViewModel.getTbMedicalReviewStatus())
                viewModel.fetchWeight(MotherNeonateAncRequest(memberId = getMemberId()))
                viewModel.fetchBloodPressure(MotherNeonateAncRequest(memberId = getMemberId()))
                viewModel.fetchHeight(MotherNeonateAncRequest(memberId = getMemberId()))
                viewModel.fetchBmi(MotherNeonateAncRequest(memberId = getMemberId()))
                fetchPatientType()
                retryButtonTbBp.safeClickListener(this@MedicalReviewPatientDiagnosisFragment)
                retryButtonTbWeight.safeClickListener(this@MedicalReviewPatientDiagnosisFragment)
                retryButtonTbHeightConfirm.safeClickListener(this@MedicalReviewPatientDiagnosisFragment)
                retryButtonTbBMI.safeClickListener(this@MedicalReviewPatientDiagnosisFragment)
                tvAddHeight.safeClickListener(this@MedicalReviewPatientDiagnosisFragment)
                tvAddPatientStatus.gone()
                tvBmiHistory.safeClickListener(this@MedicalReviewPatientDiagnosisFragment)
            }
            tbLl.setVisible(isTb)
            if (isFp()) {
                commonLl.gone()
                tbLl.gone()
                familyPlanningLl.visible()
                binding.tvHeightLblFP.text = getString(R.string.height)
                tvFpAddBp.safeClickListener(this@MedicalReviewPatientDiagnosisFragment)
                tvFpAddWeight.safeClickListener(this@MedicalReviewPatientDiagnosisFragment)
                viewModel.fetchWeight(MotherNeonateAncRequest(memberId = getMemberId()))
                viewModel.fetchBloodPressure(MotherNeonateAncRequest(memberId = getMemberId()))
                viewModel.fetchHeight(MotherNeonateAncRequest(memberId = getMemberId()))
                viewModel.fetchBmi(MotherNeonateAncRequest(memberId = getMemberId()))
                retryButtonFpBp.safeClickListener(this@MedicalReviewPatientDiagnosisFragment)
                retryButtonFpWeight.safeClickListener(this@MedicalReviewPatientDiagnosisFragment)
                retryButtonFPHeightConfirm.safeClickListener(this@MedicalReviewPatientDiagnosisFragment)
                retryButtonFpBMI.safeClickListener(this@MedicalReviewPatientDiagnosisFragment)
                tvAddHeightFP.safeClickListener(this@MedicalReviewPatientDiagnosisFragment)
                tvFpBmiHistory.safeClickListener(this@MedicalReviewPatientDiagnosisFragment)
            }
            if (isHivImrCmr()) {
                viewModel.fetchWeight(MotherNeonateAncRequest(memberId = getMemberId()))
                patientViewModel.patientDetailsLiveData.value?.data?.let { details ->
                    details.id?.let { id ->
                        diagnosisViewModel.getHivVitalsDetails(
                            patientReference = id,
                            memberId = getMemberId(),
                        )
                    }
                }
                cardPatientStatus.gone()
                cardBloodPressure.gone()
                cardAddWeight.visible()
                cardWhoClinicalStage.visible()
                cardCd4.visible()
                cardCd4Percent.visible()
                tvCd4.safeClickListener(this@MedicalReviewPatientDiagnosisFragment)
                tvCd4Percent.safeClickListener(this@MedicalReviewPatientDiagnosisFragment)
                tvWho.safeClickListener(this@MedicalReviewPatientDiagnosisFragment)
            } else {
                cardWhoClinicalStage.gone()
                cardCd4.gone()
                cardCd4Percent.gone()
            }
        }
    }

    private fun ancPncFlow(
        cardAddWeight: MaterialCardView,
        cardBloodPressure: MaterialCardView,
    ) {
        cardAddWeight.visible()
        cardBloodPressure.visible()
        if (connectivityManager.isNetworkAvailable()) {
            if (patientViewModel.getAncVisit() > 1 || getDiagnosisType() == MedicalReviewTypeEnums.PNC_MOTHER_REVIEW.name) {
                viewModel.fetchWeight(MotherNeonateAncRequest(memberId = getMemberId()))
                viewModel.fetchBloodPressure(MotherNeonateAncRequest(memberId = getMemberId()))
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.tvAddWeight.id, binding.tvTbAddWeight.id, binding.tvFpAddWeight.id -> showAddWeightDialog()
            binding.tvAddBp.id, binding.tvTbAddBp.id, binding.tvFpAddBp.id -> showAddBpDialog()
            binding.tvDiagnosisConfirm.id -> showDiagnosisDialog()
            binding.tvAddSiteDisease.id -> showDiagnosisDialog()
            binding.retryButtonBp.id, binding.retryButtonTbBp.id, binding.retryButtonFpBp.id -> retryFetchingData(true)
            binding.retryButtonWeight.id, binding.retryButtonTbWeight.id, binding.retryButtonFpWeight.id -> retryFetchingData(false)
            binding.tvAddHeight.id, binding.tvAddHeightFP.id -> showAddHeightDialog()
            binding.retryButtonTbHeightConfirm.id, binding.retryButtonFPHeightConfirm.id -> retryFetchingDataForHeight()
            binding.tvBmiHistory.id, binding.tvFpBmiHistory.id -> showBmiDialog()
            binding.retryButtonTbBMI.id, binding.retryButtonFpBMI.id -> retryFetchingDataForBMI()
            binding.tvAddPatientType.id -> showPatientType()
            binding.tvWho.id -> showWhoStage()
            binding.tvCd4.id -> showCD4Dialog(true, false)
            binding.tvCd4Percent.id -> showCD4Dialog(false, true)
        }
    }

    private fun showCD4Dialog(
        isCd4: Boolean,
        isCd4Percentage: Boolean,
    ) {
        withNetworkAvailability(online = {
            showDialogIfNotPresent(CD4DialogFragment.TAG) {
                CD4DialogFragment.newInstance(
                    isCD4 = isCd4,
                    isCD4Percentage = isCd4Percentage,
                )
            }
        })
    }

    private fun showWhoStage() {
        withNetworkAvailability(online = {
            showDialogIfNotPresent(WhoClinicalStageFragment.TAG) {
                WhoClinicalStageFragment.newInstance().apply {
                    listener = this@MedicalReviewPatientDiagnosisFragment
                }
            }
        })
    }

    private fun fetchPatientType() {
        viewModel.getPatientType(MotherNeonateAncRequest(memberId = patientViewModel.getPatientMemberId()))
    }

    private fun showPatientType() {
        withNetworkAvailability(online = {
            showDialogIfNotPresent(PatientTypeFragment.TAG) {
                PatientTypeFragment.newInstance().apply {
                    listener = this@MedicalReviewPatientDiagnosisFragment
                }
            }
        })
    }

    private fun retryFetchingDataForHeight() {
        if (connectivityManager.isNetworkAvailable()) {
            viewModel.fetchHeight(MotherNeonateAncRequest(memberId = getMemberId()))
        }
    }

    private fun retryFetchingDataForBMI() {
        if (connectivityManager.isNetworkAvailable()) {
            viewModel.fetchBmi(MotherNeonateAncRequest(memberId = getMemberId()))
        }
    }

    private fun showAddWeightDialog() {
        withNetworkAvailability(online = {
            showAddBpOrWeightDialog(isBp = false)
        })
    }

    private fun showAddBpDialog() {
        withNetworkAvailability(online = {
            showAddBpOrWeightDialog(isBp = true)
        })
    }

    private fun showAddHeightDialog() {
        withNetworkAvailability(online = {
            showDialogIfNotPresent(AddBMIDialog.TAG) {
                AddBMIDialog
                    .newInstance(
                        patientId = getPatientId(),
                        villageId = patientViewModel.getVillageId(),
                        householdId = patientViewModel.getPatientHouseholdId(),
                        memberId = getMemberId(),
                        isTb = isTb(),
                    ).apply {
                        listener = this@MedicalReviewPatientDiagnosisFragment
                    }
            }
        })
    }

    private fun showAddBpOrWeightDialog(isBp: Boolean) {
        val dialog = if (isBp) {
            AddBpDialog
                .newInstance(
                    getPatientId(),
                    villageId = patientViewModel.getVillageId(),
                    householdId = patientViewModel.getPatientHouseholdId(),
                ).apply {
                    listener = this@MedicalReviewPatientDiagnosisFragment
                }
        } else {
            AddBMIDialog
                .newInstance(
                    patientId = getPatientId(),
                    villageId = patientViewModel.getVillageId(),
                    householdId = patientViewModel.getPatientHouseholdId(),
                    memberId = patientViewModel.getPatientMemberId(),
                    isTb = isTb(),
                ).apply {
                    listener = this@MedicalReviewPatientDiagnosisFragment
                }
        }
        showDialogIfNotPresent(if (isBp) AddBpDialog.TAG else AddBMIDialog.TAG) {
            dialog
        }
    }

    private fun showBmiDialog() {
        if (connectivityManager.isNetworkAvailable()) {
            showDialogIfNotPresent(BMIListDialog.TAG) {
                BMIListDialog.newInstance(getMemberId())
            }
        } else {
            (activity as BaseActivity?)?.showErrorDialogue(
                getString(R.string.error),
                getString(R.string.no_internet_error),
                isNegativeButtonNeed = false,
            ) {
            }
        }
    }

    private fun showDiagnosisDialog() {
        if (connectivityManager.isNetworkAvailable()) {
            if (isTb()) {
                TbConfirmDiagnosisAndSiteOfDiseaseDialog()
                    .apply {
                        this.listener = this@MedicalReviewPatientDiagnosisFragment
                    }.show(childFragmentManager, TbConfirmDiagnosisAndSiteOfDiseaseDialog.TAG)
            } else {
                DiagnosisDialogFragment().show(childFragmentManager, DiagnosisDialogFragment.TAG)
            }
        } else {
            (activity as BaseActivity?)?.showErrorDialogue(
                getString(R.string.error),
                getString(R.string.no_internet_error),
                isNegativeButtonNeed = false,
            ) {
            }
        }
    }

    private fun retryFetchingData(isBp: Boolean) {
        if (connectivityManager.isNetworkAvailable()) {
            if (isBp) {
                viewModel.fetchBloodPressure(MotherNeonateAncRequest(memberId = getMemberId()))
            } else {
                viewModel.fetchWeight(MotherNeonateAncRequest(memberId = getMemberId()))
            }
        }
    }

    private fun initializeListeners() {
        binding.tvDiagnosisConfirm.safeClickListener(this)
    }

    private fun getPatientId(): String = arguments?.getString(DefinedParams.PatientId, "") ?: ""

    private fun attachObserver() {
      /*  whoStageViewModel.getWhoStageLiveData.observe(viewLifecycleOwner){
            diagnosisViewModel.hivVitalsDetailLiveData.value?.data?.let {list ->
                binding.tvWhoValue.text = getClinicalStageText(list.whoClinicalStage)
            }
        }*/

        viewModel.getPatientTypeLiveData.observe(viewLifecycleOwner) {
            val type = viewModel.getPatientType.value
                ?.data
                ?.get(PATIENT_TYPE_HYPHEN) as? String
            if (!type.isNullOrBlank()) {
                val patientType = viewModel.getPatientTypeLiveData.value
                    ?.find { it.value == type }
                    ?.name
                    ?.capitalizeFirstChar()
                    ?: getString(R.string.hyphen_symbol)
                binding.tvPatientType.text = patientType
                binding.tvAddPatientType.text = getString(R.string.edit_type)
            }
        }
        viewModel.getPatientType.observe(viewLifecycleOwner) { result ->
            val type = result.data?.get(PATIENT_TYPE_HYPHEN) as? String
            if (result.state == ResourceState.SUCCESS && !type.isNullOrBlank()) {
                binding.tvAddPatientType.text = getString(R.string.edit_type)
                viewModel.setPatientType(MotherNeonateUtil.PATIENT_TYPE)
            } else {
                binding.tvPatientType.text = getString(R.string.hyphen_symbol)
                binding.tvAddPatientType.text = getString(R.string.add_type)
            }
        }
        statusViewModel.patientStatusLiveData.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.SUCCESS -> {
                    hideProgress()
                    resourceState.data?.let { patientStatus ->
                        if (patientStatus.status.isNullOrEmpty()) {
                            binding.tvPatientStatusValue.text = getString(R.string.seperator_hyphen)
                        } else {
                            //  binding.tvPatientStatusValue.text = getPatientStatus(patientStatus.status)
                            binding.tvPatientStatusValue.text = patientStatus.status
                        }
                        patientViewModel.patientCurrentStatus.postValue(patientStatus.status)
                        patientViewModel.patientDetailsLiveData.value?.data?.let { details ->
                            details.id?.let { id ->
                                diagnosisViewModel.getDiagnosisDetails(
                                    CreateUnderTwoMonthsResponse(
                                        patientReference = id,
                                        type = diagnosisViewModel.diagnosisType,
                                    ),
                                )
                            }
                        }
                    }
                }

                ResourceState.ERROR -> {
                    hideProgress()
                    patientViewModel.patientCurrentStatus.postValue("")
                }
            }
        }

        diagnosisViewModel.diagnosisDetailsList.observe(viewLifecycleOwner) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.SUCCESS -> {
                    hideProgress()
                    resource.data?.let { list ->
                        val diagnosisItems = list.map { it.diseaseCategory }.distinct()
                        if (isTb()) {
                            updateDiagnosisUI(
                                diagnosisItems,
                                list.any { (it.type.equals("TB", true) || it.type.isNullOrBlank()) },
                                binding.tvDiagnosis,
                                binding.tvDiagnosisConfirm,
                                diagnosisViewModel.diagnosisMetaList,
                            )
                            updateDiagnosisUI(
                                diagnosisItems,
                                list.any { it.type.equals("siteOfDisease", true) },
                                binding.tvSiteDisease,
                                binding.tvAddSiteDisease,
                                diagnosisViewModel.siteOfDiseaseMetaList,
                                true,
                            )
                        } else {
                            updateDiagnosisUI(
                                diagnosisItems,
                                list.isNotEmpty(),
                                binding.tvDiagnosis,
                                binding.tvDiagnosisConfirm,
                                diagnosisViewModel.diagnosisMetaList,
                            )
                        }
                    }
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }
            }
        }

        diagnosisViewModel.diagnosisSaveUpdateResponse.observe(viewLifecycleOwner) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.SUCCESS -> {
                    hideProgress()
                    resource.data?.let { list ->
                        if (list.isNotEmpty()) {
                            val diagnosisItems = list.map { it.diseaseCategory }.distinct()
                            binding.tvDiagnosis.text =
                                convertListToString(ArrayList(diagnosisItems))
                            binding.tvDiagnosisConfirm.text = getString(R.string.edit_diagnoses)
                        } else {
                            binding.tvDiagnosis.text =
                                requireContext().getString(R.string.hyphen_symbol)
                            binding.tvDiagnosisConfirm.text = getString(R.string.add_diagnosis)
                        }
                    }
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }
            }
        }

        diagnosisViewModel.hivVitalsDetailLiveData.observe(viewLifecycleOwner) { resources ->
            when (resources.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }
                ResourceState.SUCCESS -> {
                    hideProgress()
                    resources.data?.let { list ->
                        binding.tvCd4Value.text = list.cd4 ?: getString(R.string.seperator_hyphen)
                        binding.tvCd4PercentValue.text = list.cd4Percentage ?: getString(R.string.seperator_hyphen)
                        binding.tvWhoValue.text = list.whoClinicalStage ?: getString(R.string.seperator_hyphen)
                        // getClinicalStageText(list.whoClinicalStage)
                        binding.tvWho.text = if (list.whoClinicalStage != null) {
                            getString(R.string.edit_who_clinical_stage)
                        } else {
                            getString(R.string.add_who_clinical_stage)
                        }
                    }
                }
                ResourceState.ERROR -> {
                    hideProgress()
                }
            }
        }

        whoStageViewModel.whoStageCreateLiveData.observe(viewLifecycleOwner) { resourcesState ->
            when (resourcesState.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.SUCCESS -> {
                    hideProgress()
                    patientViewModel.patientDetailsLiveData.value?.data?.let { details ->
                        diagnosisViewModel.getHivVitalsDetails(
                            patientReference = details.id,
                            memberId = details.memberId,
                        )
                    }
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }
            }
        }
    }

    private fun getClinicalStageText(whoClinicalStage: String?): String {
        whoStageViewModel.getWhoStageLiveData.value?.let { whoStageList ->
            return if (whoClinicalStage.isNullOrEmpty()) {
                getString(R.string.seperator_hyphen)
            } else {
                whoStageList.find { it.value == whoClinicalStage }?.name?.capitalizeFirstChar()
                    ?: getString(R.string.seperator_hyphen)
            }
        } ?: run {
            return getString(R.string.seperator_hyphen)
        }
    }

    private fun updateDiagnosisUI(
        diagnosisItems: List<String>,
        condition: Boolean,
        textView: TextView,
        buttonView: TextView,
        metaList: MutableLiveData<Resource<List<DiseaseCategoryItems>>>,
        isSite: Boolean = false,
    ) {
        textView.text = if (condition) {
            metaList.value?.data?.let { diagnosisList ->
                convertListToString(
                    ArrayList(
                        diagnosisList
                            .filter { it.value in diagnosisItems }
                            .map { it.name },
                    ),
                )
            } ?: getString(R.string.seperator_hyphen)
        } else {
            getString(R.string.hyphen_symbol)
        }

        if (!isSite) {
            buttonView.text = if (condition) getString(R.string.edit_diagnoses) else getString(R.string.add_diagnosis)
        } else {
            buttonView.text = if (condition) getString(R.string.edit_disease) else getString(R.string.add_disease)
        }
    }

    private fun getPatientStatus(status: String): String {
        val formattedString = cleanString(status.lowercase())
        return if (diagnosisViewModel.diagnosisType == MedicalReviewTypeEnums.ANC_REVIEW.name) {
            if (formattedString.isEmpty()) {
                PREGNANT
            } else if (formattedString.contains(PREGNANT, ignoreCase = true)) {
                requireContext().changePatientStatus(formattedString)
            } else {
                "${requireContext().changePatientStatus(formattedString)}, $PREGNANT"
            }
        } else {
            requireContext().changePatientStatus(status)
        }
    }

    private fun cleanString(input: String): String {
        val toRemove = listOf(POSTPARTUM.lowercase(), Lactating.lowercase())
        if (toRemove.any { input.contains(it) }) {
            var cleanedString = input
            for (str in toRemove) {
                cleanedString = cleanedString.replace(str, "")
            }
            cleanedString = cleanedString.replace(",", "")
            return cleanedString.trim().capitalizeFirstChar()
        } else {
            return input.capitalizeFirstChar()
        }
    }

    private fun initializeViews() {
        diagnosisViewModel.getDiagnosisMetaList(diagnosisViewModel.diagnosisType)
        if (isTb() && patientViewModel.getTbMedicalReviewStatus()) {
            diagnosisViewModel.getSiteOfDiseaseMetaList(MotherNeonateUtil.TB_SITE_OF_DISEASE)
        }
        statusViewModel.patientId?.let {
            binding.tvPatientStatusValue.text = requireContext().getString(R.string.hyphen_symbol)
            if (it.isNotEmpty()) {
                diagnosisViewModel.getDiagnosisDetails(
                    CreateUnderTwoMonthsResponse(
                        patientReference = it,
                        type = diagnosisViewModel.diagnosisType,
                    ),
                )
            }
        }
    }

    private fun getMemberId(): String = arguments?.getString(DefinedParams.MEMBER_ID, "") ?: ""

    override fun onDialogDismissed(
        isBp: Boolean,
        isHeight: Boolean,
    ) {
        if (connectivityManager.isNetworkAvailable()) {
            if (isBp && !isHeight) {
                viewModel.fetchBloodPressure(MotherNeonateAncRequest(memberId = getMemberId()))
            }
            if ((!isBp && !isHeight) && (isTb() || isFp())) {
                viewModel.fetchHeight(MotherNeonateAncRequest(memberId = getMemberId()))
                viewModel.fetchWeight(MotherNeonateAncRequest(memberId = getMemberId()))
                viewModel.fetchBmi(MotherNeonateAncRequest(memberId = getMemberId()))
            }
            val isAnc = arguments?.getBoolean(DefinedParams.PregnancyANC, false) ?: false
            if ((isAnc || isHivImrCmr()) && (!isBp && !isHeight)) {
                viewModel.fetchWeight(MotherNeonateAncRequest(memberId = getMemberId()))
                viewModel.fetchHeight(MotherNeonateAncRequest(memberId = getMemberId()))
            }
        }
        val dialog =
            childFragmentManager.findFragmentByTag(if (isBp) AddBpDialog.TAG else AddBMIDialog.TAG)
                ?: return

        when (dialog) {
            is AddBpDialog -> {
                dialog.listener = null
                dialog.dismiss()
            }

            is AddBMIDialog -> {
                dialog.listener = null
                dialog.dismiss()
            }

            else -> {
                return
            }
        }
    }

    override fun onDialogDismissedForTb(isPatientType: Boolean) {
        if (isPatientType) {
            fetchPatientType()
        } else {
            patientViewModel.patientDetailsLiveData.value?.data?.let { details ->
                details.id?.let { id ->
                    diagnosisViewModel.getDiagnosisDetails(
                        CreateUnderTwoMonthsResponse(
                            patientReference = id,
                            type = diagnosisViewModel.diagnosisType,
                        ),
                    )
                }
            }
        }
    }
}
