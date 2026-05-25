package org.medtroniclabs.uhis.ui.referralhistory.activity

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.appextensions.getLocalDate
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.isFineAndCoarseLocationPermissionGranted
import org.medtroniclabs.uhis.appextensions.isGpsEnabled
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.DefinedParams.ID
import org.medtroniclabs.uhis.databinding.ActivityReferralTicketBinding
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.model.PatientListRespModel
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.home.MedicalReviewToolsActivity
import org.medtroniclabs.uhis.ui.medicalreview.motherneonate.anc.AncVisitCallBack
import org.medtroniclabs.uhis.ui.mypatients.fragment.PatientInfoFragment
import org.medtroniclabs.uhis.ui.mypatients.viewmodel.PatientDetailViewModel
import org.medtroniclabs.uhis.ui.referralhistory.fragment.BirthDetailsFragment
import org.medtroniclabs.uhis.ui.referralhistory.fragment.InvestigationHistoryFragment
import org.medtroniclabs.uhis.ui.referralhistory.fragment.MedicalReviewHistoryFragment
import org.medtroniclabs.uhis.ui.referralhistory.fragment.MotherPncVisitSummaryHistoryFragment
import org.medtroniclabs.uhis.ui.referralhistory.fragment.PrescriptionHistoryFragment
import org.medtroniclabs.uhis.ui.referralhistory.fragment.ReferralTicketFragment
import org.medtroniclabs.uhis.ui.referralhistory.viewmodel.ReferralHistoryViewModel
import java.time.LocalDate

@AndroidEntryPoint
class ReferralHistoryActivity : BaseActivity(), AncVisitCallBack {
    private lateinit var binding: ActivityReferralTicketBinding
    val viewModel: ReferralHistoryViewModel by viewModels()
    private val patientDetailViewModel: PatientDetailViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReferralTicketBinding.inflate(layoutInflater)
        setMainContentView(
            binding.root,
            true,
            getString(R.string.patient_medical_review),
        )
        initView()
        initializeListener()
        attachObserver()
    }

    private fun attachObserver() {
        patientDetailViewModel.patientDetailsLiveData.observe(this) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    if (binding.refreshLayout.isRefreshing) {
                        binding.refreshLayout.isRefreshing = false
                    }

                    resource.data?.let {
                        showBirthDetailsCard(resource.data)
                    }
                }

                ResourceState.ERROR -> {
                    hideLoading()
                    showErrorDialogue(
                        title = getString(R.string.alert),
                        message = getString(R.string.something_went_wrong_try_later),
                        positiveButtonName = getString(R.string.ok),
                    ) {
                        finish()
                    }
                }
            }
        }

        viewModel.medicalReviewTicketLiveDataPNC.observe(this) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    if (resource.data?.id != null) {
                        val medicalReviewFragmentPNC =
                            MotherPncVisitSummaryHistoryFragment.newInstance(resource.data)
                        addFragmentIfAbsent(
                            R.id.cardMedicalReviewContainer,
                            MotherPncVisitSummaryHistoryFragment.TAG,
                            medicalReviewFragmentPNC,
                        )
                    }
                    val medicalReviewFragment =
                        MedicalReviewHistoryFragment.newInstance(viewModel.patientReference)
                    addFragmentIfAbsent(
                        R.id.cardMotherNeonateContainer,
                        MedicalReviewHistoryFragment.TAG,
                        medicalReviewFragment,
                    )
                }

                ResourceState.ERROR -> {
                }
            }
        }
    }

    private fun showBirthDetailsCard(detail: PatientListRespModel) {
        val sixWeeks = LocalDate.now().minusWeeks(6)
        val birthDate = detail.birthDate?.getLocalDate()
        if (birthDate != null && birthDate.isAfter(sixWeeks)) {
            binding.cardBirthDetailContainer.visible()
            val birthDetailFragment = BirthDetailsFragment.newInstance(detail.memberId, detail.id)
            addFragmentIfAbsent(
                R.id.cardBirthDetailContainer,
                BirthDetailsFragment.TAG,
                birthDetailFragment,
            )
        } else {
            binding.cardBirthDetailContainer.gone()
        }
    }

    private fun initializeListener() {
        binding.refreshLayout.setOnRefreshListener {
            swipeRefresh()
        }
    }

    private fun initView() {
        binding.cardBirthDetailContainer.gone()
        patientDetailViewModel.origin = intent.extras?.getString(DefinedParams.ORIGIN)
        val patientFragment =
            PatientInfoFragment.newInstance(
                intent.getStringExtra(DefinedParams.PatientId),
                isReferredScreen = true,
            )
        patientFragment.setDataCallback(this)
        supportFragmentManager
            .beginTransaction()
            .add(
                R.id.patientDetailsContainer,
                patientFragment,
            ).commit()
    }

    fun ableToGetLocation(): Boolean {
        // Check Location service is enabled
        if (!isGpsEnabled()) {
            showTurnOnGPSDialog()
            return false
        }

        // Check Location permission for limit exceed
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
        ) {
            showAllowLocationServiceDialog()
            return false
        }

        // Check Location permission
        if (!isFineAndCoarseLocationPermissionGranted()) {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
            return false
        }

        return true
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { permissions ->
            val finePermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION]
            val coarsePermission = permissions[Manifest.permission.ACCESS_COARSE_LOCATION]

            if (finePermission == true && coarsePermission == true) {
                launchToolsActivity()
            }
        }

    private fun launchToolsActivity() {
        val intent = Intent(this, MedicalReviewToolsActivity::class.java)
        if (getString().isNotBlank()) {
            intent.putExtra(DefinedParams.MenuTitle, getString())
        }
        intent.putExtra(
            DefinedParams.PatientId,
            this.intent.getStringExtra(DefinedParams.PatientId),
        )
        intent.putExtra(
            ID,
            patientDetailViewModel.patientDetailsId,
        )
        intent.putExtra(DefinedParams.MEMBER_ID, patientDetailViewModel.getPatientMemberId())
        intent.putExtra(
            DefinedParams.GENDER,
            this.intent.getStringExtra(DefinedParams.GENDER),
        )
        intent.putExtra(
            DefinedParams.DOB,
            this.intent.getStringExtra(DefinedParams.DOB),
        )
        intent.putExtra(
            DefinedParams.CHILD_PATIENT_ID,
            patientDetailViewModel.childPatientDetails,
        )
        intent.putExtra(
            DefinedParams.DateOfDelivery,
            patientDetailViewModel.dateOfDelivery,
        )
        intent.putExtra(
            DefinedParams.NEONATE_OUTCOME,
            patientDetailViewModel.neonateOutCome,
        )
        intent.putExtra(
            DefinedParams.householdId,
            patientDetailViewModel.getPatientHouseholdId(),
        )
        intent.putExtra(
            DefinedParams.villageId,
            patientDetailViewModel.getVillageId(),
        )
        intent.putExtra(
            DefinedParams.isPregnant,
            patientDetailViewModel.isPregnant(),
        )
        intent.putExtra(
            DefinedParams.EMTCT,
            patientDetailViewModel.isEmtctFlow,
        )
        intent.putExtra(
            DefinedParams.HIV_TESTED_POSITIVE,
            patientDetailViewModel.hivTestedPositive,
        )
        startActivity(intent)
    }

    override fun onDataLoaded(details: PatientListRespModel) {
        if (connectivityManager.isNetworkAvailable()) {
            val dob = intent.getStringExtra(DefinedParams.DOB)
            dob?.let { DateUtils.calculateAge(dob) } ?: 0
            showLoading()
            viewModel.patientReference = details.id
            viewModel.getMedicalReviewHistoryPNC(patientId = details.id)
            val referralTicketFragment =
                ReferralTicketFragment.newInstance(details.id, details.memberId)
            addFragmentIfAbsent(
                R.id.cardReferralTicket,
                ReferralTicketFragment.TAG,
                referralTicketFragment,
            )

            val prescriptionFragment = PrescriptionHistoryFragment.newInstance(details.id)
            addFragmentIfAbsent(
                R.id.cardPrescriptionContainer,
                PrescriptionHistoryFragment.TAG,
                prescriptionFragment,
            )

            val investigationFragment = InvestigationHistoryFragment.newInstance(details.id)
            addFragmentIfAbsent(
                R.id.cardInvestigationContainer,
                InvestigationHistoryFragment.TAG,
                investigationFragment,
            )

            binding.btnMedicalReview.safeClickListener {
                if (ableToGetLocation()) {
                    launchToolsActivity()
                }
            }
            hideLoading()
        }
    }

    private fun addFragmentIfAbsent(
        containerId: Int,
        fragmentTag: String,
        fragmentInstance: Fragment,
    ) {
        val existingFragment = supportFragmentManager.findFragmentByTag(fragmentTag)
        if (existingFragment == null) {
            supportFragmentManager
                .beginTransaction()
                .add(containerId, fragmentInstance, fragmentTag)
                .commit()
        } else {
            supportFragmentManager
                .beginTransaction()
                .replace(containerId, fragmentInstance, fragmentTag)
                .commit()
        }
    }

    private fun swipeRefresh() {
        patientDetailViewModel.patientDetailsLiveData.value?.data?.let { details ->
            if (CommonUtils.isCommunity()) {
                details.patientId?.let { id ->
                    patientDetailViewModel.getPatients(id)
                }
            } else {
                details.id?.let { id ->
                    patientDetailViewModel.getPatients(id, origin = patientDetailViewModel.origin)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (CommonUtils.isCommunity()) {
            intent.getStringExtra(DefinedParams.PatientId)?.let {
                patientDetailViewModel.getPatients(it)
            }
        } else {
            intent.getStringExtra(DefinedParams.FhirId)?.let {
                patientDetailViewModel.getPatients(it, origin = patientDetailViewModel.origin)
            }
        }
        viewModel.setUserJourney(AnalyticsDefinedParams.ReferralTicket)
    }
}
