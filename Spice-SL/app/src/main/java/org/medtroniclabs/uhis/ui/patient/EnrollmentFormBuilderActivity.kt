package org.medtroniclabs.uhis.ui.patient

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.databinding.ActivityEnrollmentFormBuilderBinding
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.landing.LandingActivity
import org.medtroniclabs.uhis.ui.patient.fragment.EnrollmentFormFragmentBD
import org.medtroniclabs.uhis.ui.patient.fragment.EnrollmentSummaryFragment
import org.medtroniclabs.uhis.ui.patient.viewmodel.EnrollmentFormBuilderViewModel
import org.medtroniclabs.uhis.ui.patient.viewmodel.PatientDetailViewModel
import kotlin.getValue

@AndroidEntryPoint
class EnrollmentFormBuilderActivity : BaseActivity() {
    lateinit var binding: ActivityEnrollmentFormBuilderBinding
    private val viewModel: EnrollmentFormBuilderViewModel by viewModels()
    private val patientDetailsViewModel: PatientDetailViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEnrollmentFormBuilderBinding.inflate(layoutInflater)
        intent?.getStringExtra(IntentConstants.INTENT_PATIENT_INITIAL)?.let {
            viewModel.patientInitial = it
        }
        viewModel.screeningId = intent.getLongExtra(DefinedParams.SCREENING_ID, -1L)
        viewModel.isConfirmDiagnosis =
            intent.getBooleanExtra(DefinedParams.IS_CONFIRM_DIAGNOSIS, false)
        setMainContentView(
            binding.root,
            true,
            title = getString(R.string.confirm_diagnoses),
            homeAndBackVisibility = Pair(true, true),
            callbackHome = {
                showOnBackPressedAlert()
            },
            callback = {
                showOnBackPressedAlert()
            },
        )
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add<EnrollmentFormFragmentBD>(binding.fragmentContainerView.id)
            }
        }
        getIntentValues()
    }

    private fun getIntentValues() {
        patientDetailsViewModel.patientId = intent.getLongExtra(DefinedParams.PATIENT_ID, -1L)
        viewModel.patientTrackId = intent.getLongExtra(DefinedParams.PATIENT_ID, -1L)
        viewModel.isFromDirectEnrollment =
            intent.getBooleanExtra(IntentConstants.IS_FROM_DIRECT_ENROLLMENT, false)
    }

    inline fun <reified fragment : Fragment> replaceFragmentInId(id: Int? = null) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace<fragment>(id ?: binding.fragmentContainerView.id)
        }
    }

    fun showOnBackPressedAlert(fragment: Fragment? = null) {
        val fragmentValue = supportFragmentManager.findFragmentById(binding.fragmentContainerView.id)
        if (fragmentValue != null && fragmentValue.isVisible && fragmentValue is EnrollmentSummaryFragment) {
            startAsNewActivity(
                Intent(
                    this@EnrollmentFormBuilderActivity,
                    LandingActivity::class.java,
                ),
            )
        } else {
            showErrorDialogue(getString(R.string.alert), getString(R.string.exit_reason), isNegativeButtonNeed = true) {
                if (it) {
                    startAsNewActivity(
                        Intent(
                            this@EnrollmentFormBuilderActivity,
                            LandingActivity::class.java,
                        ),
                    )
                }
            }
        }
    }
}
