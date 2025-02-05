package com.medtroniclabs.opensource.ui.mypatients.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.activityViewModels
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.changePatientStatus
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.invisible
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.CommonUtils.convertListToString
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.DefinedParams.Lactating
import com.medtroniclabs.opensource.common.DefinedParams.Postpartum
import com.medtroniclabs.opensource.common.DefinedParams.Pregnant
import com.medtroniclabs.opensource.data.model.MotherNeonateAncRequest
import com.medtroniclabs.opensource.databinding.FragmentMedicalReviewPatientDiagnosisBinding
import com.medtroniclabs.opensource.formgeneration.extension.capitalizeFirstChar
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.model.medicalreview.CreateUnderTwoMonthsResponse
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.BaseFragment
import com.medtroniclabs.opensource.ui.medicalreview.diagnosis.DiagnosisDialogFragment
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.DialogDismissListener
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.MotherNeonateUtil
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.MotherNeonateUtil.calculateBp
import com.medtroniclabs.opensource.ui.medicalreview.diagnosis.viewmodel.DiagnosisViewModel
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.fragment.AddBpDialog
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.fragment.AddWeightDialog
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.MotherNeonateBpWeightViewModel
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewTypeEnums
import com.medtroniclabs.opensource.ui.medicalreview.viewmodel.PatientStatusViewModel
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.PatientDetailViewModel
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.PregnancyDetailsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MedicalReviewPatientDiagnosisFragment : BaseFragment(), View.OnClickListener,
    DialogDismissListener {

    private lateinit var binding: FragmentMedicalReviewPatientDiagnosisBinding
    private val viewModel: MotherNeonateBpWeightViewModel by activityViewModels()
    private val statusViewModel: PatientStatusViewModel by activityViewModels()
    private val diagnosisViewModel: DiagnosisViewModel by activityViewModels()
    private val patientViewModel: PatientDetailViewModel by activityViewModels()
    private val pregnancyDetailsViewModel: PregnancyDetailsViewModel by activityViewModels()

    companion object {

        const val TAG: String = "MedicalReviewPatientDiagnosisFragment"
        fun newInstance(): MedicalReviewPatientDiagnosisFragment {
            return MedicalReviewPatientDiagnosisFragment()
        }

        fun newInstance(isAnc: Boolean,isPnc:Boolean=false, patientId: String?,memberID: String?, id: String?): MedicalReviewPatientDiagnosisFragment {
            val fragment = MedicalReviewPatientDiagnosisFragment()
            fragment.arguments = Bundle().apply {
                putBoolean(DefinedParams.PregnancyANC, isAnc)
                putBoolean(DefinedParams.PregnancyPNC,isPnc)
                putString(DefinedParams.PatientId, patientId)
                putString(DefinedParams.MemberID, memberID)
                putString(DefinedParams.ID, id)
            }
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentMedicalReviewPatientDiagnosisBinding.inflate(inflater, container, false)
        arguments?.let {
            diagnosisViewModel.diagnosisType = getDiagnosisType()
            statusViewModel.patientId = it.getString(DefinedParams.ID)
        }
        return binding.root
    }

    private fun getDiagnosisType(): String {
        return arguments?.let {
            if (it.getBoolean(DefinedParams.PregnancyANC)){
                MedicalReviewTypeEnums.ANC_REVIEW.name
            }else if (it.getBoolean(DefinedParams.PregnancyPNC)){
                MedicalReviewTypeEnums.PNC_MOTHER_REVIEW.name
            }else {
                it.getString(MedicalReviewTypeEnums.DiagnosisType.name)
            }
        } ?: ""
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews()
        attachObserver()
        handleFlow()
        initializeListeners()
        attachListeners()
        if (patientViewModel.getAncVisit() == 1 && diagnosisViewModel.diagnosisType!=MedicalReviewTypeEnums.PNC_MOTHER_REVIEW.name ) {
            binding.tvWeightValue.text = MotherNeonateUtil.convertWeight(
                pregnancyDetailsViewModel.pregnancyDetailsModel.weight,
                requireContext()
            )
            binding.tvBpValue.text =
                calculateBp(
                    pregnancyDetailsViewModel.pregnancyDetailsModel.systolic,
                    pregnancyDetailsViewModel.pregnancyDetailsModel.diastolic,
                    requireContext()
                )
            binding.tvAddWeight.invisible()
            binding.tvAddBp.invisible()
        }
    }

    private fun attachListeners() {
        viewModel.getWeight.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    handleLoading(binding.pageProgress, binding.tvAddWeight, binding.clWeight)
                }

                ResourceState.SUCCESS -> {
                    handleSuccess(
                        binding.pageProgress,
                        binding.retryButtonWeight,
                        binding.clWeight,
                        binding.tvAddWeight
                    )
                    resourceState.data?.let {
                        binding.tvWeightValue.text =
                            MotherNeonateUtil.convertWeight(
                                it.weight,
                                requireContext()
                            )
                    }
                }

                ResourceState.ERROR -> {
                    handleError(
                        binding.pageProgress,
                        binding.tvAddWeight,
                        binding.clWeight,
                        binding.retryButtonWeight,
                        binding.tvWeightValue
                    )
                }
            }
        }

        patientViewModel.patientDetailsLiveData.observe(viewLifecycleOwner) { resource ->
            when (resource.state) {
                ResourceState.SUCCESS -> {
                    resource.data?.let {
                        statusViewModel.getPatientStatusDetails(
                            it,
                            diagnosisViewModel.diagnosisType
                        )
                    }
                }
                else -> {

                }
            }
        }

        viewModel.getBloodPressure.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    handleLoading(binding.pageProgressBp, binding.tvAddBp, binding.clBp)
                }

                ResourceState.SUCCESS -> {
                    handleSuccess(
                        binding.pageProgressBp,
                        binding.retryButtonBp,
                        binding.clBp,
                        binding.tvAddBp
                    )
                    resourceState.data?.let {
                        binding.tvBpValue.text =
                            calculateBp(it.systolic, it.diastolic, requireContext())
                    }
                }

                ResourceState.ERROR -> {
                    handleError(
                        binding.pageProgressBp,
                        binding.tvAddBp,
                        binding.clBp,
                        binding.retryButtonBp,
                        binding.tvBpValue
                    )
                }
            }
        }
    }

    private fun handleLoading(
        pageProgress: ProgressBar,
        textView: AppCompatTextView,
        clView: ConstraintLayout
    ) {
//        pageProgress.visible()
        textView.isEnabled = false
        clView.setBackgroundResource(R.color.grey_bg)
    }

    private fun handleSuccess(
        pageProgress: ProgressBar,
        retryButton: MaterialButton,
        clView: ConstraintLayout,
        textView: AppCompatTextView
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
        textViewError: AppCompatTextView
    ) {
        pageProgress.gone()
        textView.isEnabled = true
        clView.setBackgroundResource(R.color.white)
        retryButton.visible()
        textViewError.text =
            requireContext().getString(R.string.something_went_wrong)
    }

    private fun handleFlow() {
        with(binding) {
            val isAnc = arguments?.getBoolean(DefinedParams.PregnancyANC, false)
            val isPnc = arguments?.getBoolean(DefinedParams.PregnancyPNC, false)
            if (isAnc == false && isPnc == false) {
                cardAddWeight.gone()
                cardBloodPressure.gone()
            }else {
               ancPncFlow(cardAddWeight,cardBloodPressure)
            }
            tvAddWeight.safeClickListener(this@MedicalReviewPatientDiagnosisFragment)
            tvAddBp.safeClickListener(this@MedicalReviewPatientDiagnosisFragment)
            retryButtonBp.safeClickListener(this@MedicalReviewPatientDiagnosisFragment)
            retryButtonWeight.safeClickListener(this@MedicalReviewPatientDiagnosisFragment)
            cardDiagnosis.visible()
            cardPatientStatus.visible()
        }
    }

    private fun ancPncFlow(cardAddWeight: MaterialCardView, cardBloodPressure: MaterialCardView) {
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
            binding.tvAddWeight.id -> showAddWeightDialog()
            binding.tvAddBp.id -> showAddBpDialog()
            binding.tvDiagnosisConfirm.id -> showDiagnosisDialog()
            binding.retryButtonBp.id -> retryFetchingData(true)
            binding.retryButtonWeight.id -> retryFetchingData(false)
        }
    }

    private fun showAddWeightDialog() {
        showAddBpOrWeightDialog(isBp = false)
    }

    private fun showAddBpDialog() {
        showAddBpOrWeightDialog(isBp = true)
    }

    private fun showAddBpOrWeightDialog(isBp: Boolean) {
        val dialog = if (isBp) {
            AddBpDialog.newInstance(getPatientId()).apply {
                listener = this@MedicalReviewPatientDiagnosisFragment
            }
        } else {
            AddWeightDialog.newInstance(getPatientId()).apply {
                listener = this@MedicalReviewPatientDiagnosisFragment
            }
        }
        dialog.show(childFragmentManager, if (isBp) AddBpDialog.TAG else AddWeightDialog.TAG)
    }


    private fun showDiagnosisDialog() {
        if (connectivityManager.isNetworkAvailable()){
            DiagnosisDialogFragment().show(childFragmentManager, DiagnosisDialogFragment.TAG)
        } else {
            (activity as BaseActivity?)?.showErrorDialogue(
                getString(R.string.error),
                getString(R.string.no_internet_error),
                isNegativeButtonNeed = false
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

    private fun getPatientId(): String {
        return arguments?.getString(DefinedParams.PatientId, "") ?: ""
    }

    private fun attachObserver() {
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
                        patientViewModel.patientDetailsLiveData.value?.data?.let { details ->
                            details.id?.let { id ->
                                diagnosisViewModel.getDiagnosisDetails(
                                    CreateUnderTwoMonthsResponse(
                                        patientReference = id,
                                        type = diagnosisViewModel.diagnosisType
                                    )
                                )
                            }
                        }
                    }
                }

                ResourceState.ERROR -> {
                    hideProgress()
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
                        if (list.isNotEmpty()) {
                            val diagnosisItems = list.map { it.diseaseCategory }.distinct()
                            binding.tvDiagnosis.text =
                                diagnosisViewModel.diagnosisMetaList.value?.data?.let { diagnosisList ->
                                    convertListToString(ArrayList(diagnosisList.filter { it.value in diagnosisItems }
                                        .map { it.name }))
                                } ?: getString(R.string.seperator_hyphen)
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
    }

    private fun getPatientStatus(status: String): String {
        val formattedString = cleanString(status.lowercase())
        return if (diagnosisViewModel.diagnosisType == MedicalReviewTypeEnums.ANC_REVIEW.name) {
            if (formattedString.isEmpty()) {
                Pregnant
            } else if (formattedString.contains(Pregnant, ignoreCase = true)) {
                requireContext().changePatientStatus(formattedString)
            } else {
                "${requireContext().changePatientStatus(formattedString)}, $Pregnant"
            }
        } else {
            requireContext().changePatientStatus(status)
        }
    }


    private fun cleanString(input: String): String {
        val toRemove = listOf(Postpartum.lowercase(), Lactating.lowercase())
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
        statusViewModel.patientId?.let {
            binding.tvPatientStatusValue.text = requireContext().getString(R.string.hyphen_symbol)
            if (it.isNotEmpty()){
                diagnosisViewModel.getDiagnosisDetails(
                    CreateUnderTwoMonthsResponse(
                        patientReference = it,
                        type = diagnosisViewModel.diagnosisType
                    )
                )
            }
        }
    }

    private fun getMemberId(): String {
        return arguments?.getString(DefinedParams.MemberID, "") ?: ""
    }

    override fun onDialogDismissed(isBp: Boolean) {
        if (connectivityManager.isNetworkAvailable()) {
            if (isBp) {
                viewModel.fetchBloodPressure(MotherNeonateAncRequest(memberId = getMemberId()))
            } else {
                viewModel.fetchWeight(MotherNeonateAncRequest(memberId = getMemberId()))
            }
        }
        val dialog =
            childFragmentManager.findFragmentByTag(if (isBp) AddBpDialog.TAG else AddWeightDialog.TAG) as? AddBpDialog
                ?: return
        dialog.listener = null
        dialog.dismiss()
    }

}