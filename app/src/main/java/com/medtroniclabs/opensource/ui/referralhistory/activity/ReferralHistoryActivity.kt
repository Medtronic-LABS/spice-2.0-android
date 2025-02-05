package com.medtroniclabs.opensource.ui.referralhistory.activity

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.app.analytics.utils.AnalyticsDefinedParams
import com.medtroniclabs.opensource.appextensions.isFineAndCoarseLocationPermissionGranted
import com.medtroniclabs.opensource.appextensions.isGpsEnabled
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.DefinedParams.ID
import com.medtroniclabs.opensource.databinding.ActivityReferralTicketBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.model.PatientListRespModel
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.home.MedicalReviewToolsActivity
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.AncVisitCallBack
import com.medtroniclabs.opensource.ui.mypatients.fragment.PatientInfoFragment
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.PatientDetailViewModel
import com.medtroniclabs.opensource.ui.referralhistory.fragment.InvestigationHistoryFragment
import com.medtroniclabs.opensource.ui.referralhistory.fragment.MedicalReviewHistoryFragment
import com.medtroniclabs.opensource.ui.referralhistory.fragment.MotherPncVisitSummaryHistoryFragment
import com.medtroniclabs.opensource.ui.referralhistory.fragment.PrescriptionHistoryFragment
import com.medtroniclabs.opensource.ui.referralhistory.fragment.ReferralTicketFragment
import com.medtroniclabs.opensource.ui.referralhistory.viewmodel.ReferralHistoryViewModel
import dagger.hilt.android.AndroidEntryPoint

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
            getString(R.string.patient_medical_review)
        )
        initView()
        viewModel.setUserJourney(AnalyticsDefinedParams.ReferralTicket)
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
                            val medicalReviewFragmentPNC = MotherPncVisitSummaryHistoryFragment.newInstance(resource.data)
                            addFragmentIfAbsent(
                                R.id.cardMedicalReviewContainer,
                                MotherPncVisitSummaryHistoryFragment.TAG,
                                medicalReviewFragmentPNC
                            )
                        }
                            val medicalReviewFragment = MedicalReviewHistoryFragment.newInstance(viewModel.patientReference)
                            addFragmentIfAbsent(
                                R.id.cardMotherNeonateContainer,
                                MedicalReviewHistoryFragment.TAG,
                                medicalReviewFragment
                            )

                    }
                    ResourceState.ERROR -> {
                    }
                }
            }
    }

    private fun initializeListener() {
        binding.refreshLayout.setOnRefreshListener {
            swipeRefresh()
        }
    }


    private fun initView() {
        patientDetailViewModel.origin = intent.extras?.getString(DefinedParams.ORIGIN)
        val patientFragment =
            PatientInfoFragment.newInstance(
                intent.getStringExtra(DefinedParams.PatientId),
                isReferredScreen = true
            )
        patientFragment.setDataCallback(this)
        supportFragmentManager.beginTransaction()
            .add(
                R.id.patientDetailsContainer,
                patientFragment
            ).commit()
    }

    private fun ableToGetLocation(): Boolean {
        //Check Location service is enabled
        if (!isGpsEnabled()) {
            showTurnOnGPSDialog()
            return false
        }

        //Check Location permission for limit exceed
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            showAllowLocationServiceDialog()
            return false
        }

        //Check Location permission
        if (!isFineAndCoarseLocationPermissionGranted()) {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return false
        }

        return true
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
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
            this.intent.getStringExtra(DefinedParams.PatientId)
        )
        intent.putExtra(
            ID,
            patientDetailViewModel.patientDetailsId
        )
        intent.putExtra(
            DefinedParams.Gender,
            this.intent.getStringExtra(DefinedParams.Gender)
        )
        intent.putExtra(
            DefinedParams.DOB,
            this.intent.getStringExtra(DefinedParams.DOB)
        )
        intent.putExtra(
            DefinedParams.ChildPatientId,
            patientDetailViewModel.childPatientDetails
        )
        intent.putExtra(
            DefinedParams.DateOfDelivery,
            patientDetailViewModel.dateOfDelivery
        )
        intent.putExtra(
            DefinedParams.NeonateOutcome,
           patientDetailViewModel.neonateOutCome
        )

        startActivity(intent)
    }

    override fun onDataLoaded(details: PatientListRespModel) {
        if (connectivityManager.isNetworkAvailable()) {
            val dob = intent.getStringExtra(DefinedParams.DOB)
            dob?.let { DateUtils.calculateAge(dob) } ?: 0
            showLoading()
            viewModel.patientReference=details.id
            viewModel.getMedicalReviewHistoryPNC(patientId = details.id)
            val referralTicketFragment = ReferralTicketFragment.newInstance(details.id)
            addFragmentIfAbsent(
                R.id.cardReferralTicket,
                ReferralTicketFragment.TAG,
                referralTicketFragment
            )

            val prescriptionFragment = PrescriptionHistoryFragment.newInstance(details.id)
            addFragmentIfAbsent(
                R.id.cardPrescriptionContainer,
                PrescriptionHistoryFragment.TAG,
                prescriptionFragment
            )

            val investigationFragment = InvestigationHistoryFragment.newInstance(details.id)
            addFragmentIfAbsent(
                R.id.cardInvestigationContainer,
                InvestigationHistoryFragment.TAG,
                investigationFragment
            )

            binding.btnMedicalReview.safeClickListener {
                if (ableToGetLocation())
                    launchToolsActivity()
            }
            hideLoading()
        }
    }

    private fun addFragmentIfAbsent(
        containerId: Int,
        fragmentTag: String,
        fragmentInstance: Fragment
    ) {
        val existingFragment = supportFragmentManager.findFragmentByTag(fragmentTag)
        if (existingFragment == null) {
            supportFragmentManager.beginTransaction()
                .add(containerId, fragmentInstance, fragmentTag)
                .commit()
        } else {
            supportFragmentManager.beginTransaction()
                .replace(containerId, fragmentInstance, fragmentTag)
                .commit()
        }
    }

    private fun swipeRefresh() {
        patientDetailViewModel.patientDetailsLiveData.value?.data?.let { details ->
            if(CommonUtils.isCommunity()) {
                details.patientId?.let { id ->
                    patientDetailViewModel.getPatients(id, origin = null)
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
                patientDetailViewModel.getPatients(it, origin = null)
            }
        } else {
            intent.getStringExtra(DefinedParams.FhirId)?.let {
                patientDetailViewModel.getPatients(it, origin = patientDetailViewModel.origin)
            }
        }
    }
}