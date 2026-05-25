package org.medtroniclabs.uhis.ui.referralhistory.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.data.history.BirthDetails
import org.medtroniclabs.uhis.databinding.FragmentBirthDetailsBinding
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.referralhistory.adapter.ReferralHistoryAdapter
import org.medtroniclabs.uhis.ui.referralhistory.viewmodel.ReferralHistoryViewModel

@AndroidEntryPoint
class BirthDetailsFragment : BaseFragment(), View.OnClickListener {
    private lateinit var binding: FragmentBirthDetailsBinding
    private lateinit var adapters: ReferralHistoryAdapter
    val viewModel: ReferralHistoryViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentBirthDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        const val TAG = "BirthDetailsFragment"

        fun newInstance(): BirthDetailsFragment = BirthDetailsFragment()

        fun newInstance(
            memberId: String?,
            patientReference: String?,
        ): BirthDetailsFragment {
            val fragment = BirthDetailsFragment()
            val bundle = Bundle()
            bundle.putString(DefinedParams.MEMBER_ID, memberId)
            bundle.putString(DefinedParams.PatientReference, patientReference)
            fragment.arguments = bundle
            return fragment
        }
    }

    private fun getMemberId(): String? = arguments?.getString(DefinedParams.MEMBER_ID, "")

    private fun getPatientReference(): String? = arguments?.getString(DefinedParams.PatientReference, null)

    private fun getInitialReferralTickets() {
        getMemberId()
            ?.takeIf { it.isNotBlank() }
            ?.let { viewModel.getBirthDetails(memberId = it, patientReference = getPatientReference()) }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        attachObservers()
    }

    private fun initView() {
        if (getMemberId().isNullOrBlank()) {
            binding.tvNoHistory.visible()
        } else {
            binding.tvNoHistory.gone()
            adapters = ReferralHistoryAdapter()
            viewModel.medicalTicketId = null
            getInitialReferralTickets()
            setupClickListeners()
            binding.retryButtonBp.safeClickListener(this)
        }
    }

    private fun setupClickListeners() {
    }

    private fun attachObservers() {
        viewModel.birthDetailsLiveData.observe(viewLifecycleOwner) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    resource.data?.let {
                        handleSuccess()
                        setReferralTicket(it)
                    } ?: kotlin.run {
                        binding.clLoaderProgress.gone()
                        binding.tvNoHistory.visible()
                    }
                }

                ResourceState.ERROR -> {
                    handleError()
                }
            }
        }
    }

    override fun showLoading() {
        binding.clLoaderProgress.visible()
        binding.loaderProgress.visible()
        binding.retryButtonBp.gone()
        binding.tvErrorLabel.gone()
        binding.tvNoHistory.gone()
    }

    private fun handleSuccess() {
        binding.clLoaderProgress.gone()
    }

    private fun handleError() {
        binding.clLoaderProgress.visible()
        binding.retryButtonBp.visible()
        binding.tvErrorLabel.visible()
    }

    private fun setReferralTicket(birthDetails: BirthDetails) {
        binding.tvNoHistory.gone()
        adapters.updateList(
            createMedicalReview(birthDetails),
        )
        // adjustGuideline()
        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = adapters
        // / setReferralDates(medicalReviewHistory.history, medicalReviewHistory.id)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.retryButtonBp.id -> {
                handleRetry()
            }
        }
    }

    private fun handleRetry() {
        if (connectivityManager.isNetworkAvailable()) {
            getInitialReferralTickets()
        } else {
            showErrorDialog(getString(R.string.error), getString(R.string.no_internet_error))
        }
    }

    private fun createMedicalReview(birthDetails: BirthDetails): List<Map<String, String?>> {
        val history = listOf(
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.neonate_outcome),
                DefinedParams.Value to (
                    birthDetails.neonateOutcome?.takeIf { it.isNotBlank() }
                        ?: getString(R.string.separator_double_hyphen)
                ),
            ),
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.birth_weight),
                DefinedParams.Value to (
                    birthDetails.birthWeight?.let { "$it " + requireContext().getString(R.string.kg) }
                        ?: getString(R.string.separator_double_hyphen)
                ),
            ),
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.stateOfBaby),
                DefinedParams.Value to (
                    birthDetails.stateOfBaby?.takeIf { it.isNotBlank() }
                        ?: getString(R.string.separator_double_hyphen)
                ),
            ),
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.gestational_period),
                DefinedParams.Value to (
                    birthDetails.gestationalAge?.let { it + " " + requireContext().getString(R.string.weeks) }
                        ?: getString(R.string.separator_double_hyphen)
                ),
            ),
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.apgar_score),
                DefinedParams.Value to
                    (
                        birthDetails.apgarScoreFiveMinuteDTO?.fiveMinuteTotalScore?.let { "$it" + requireContext().getString(R.string.five_minutes) }
                            ?: getString(R.string.separator_double_hyphen)
                    ),
            ),
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.signs_symptoms_observed),
                DefinedParams.Value to (
                    birthDetails.signs?.joinToString(", ")
                        ?: getString(R.string.separator_double_hyphen)
                ),
            ),
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.patient_status),
                DefinedParams.Value to (
                    birthDetails.patientStatus?.takeIf { it.isNotBlank() }
                        ?: getString(R.string.separator_double_hyphen)
                ),
            ),
        )
        return history
    }
}
