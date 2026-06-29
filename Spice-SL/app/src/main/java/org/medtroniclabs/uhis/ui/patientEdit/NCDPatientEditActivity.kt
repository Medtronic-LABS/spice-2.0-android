package org.medtroniclabs.uhis.ui.patientEdit

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.databinding.ActivityNcdPatientEditBinding
import org.medtroniclabs.uhis.ncd.medicalreview.NCDMRUtil
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.MenuConstants
import org.medtroniclabs.uhis.ui.mypatients.viewmodel.PatientDetailViewModel
import org.medtroniclabs.uhis.ui.patientEdit.fragment.NCDPatientEditFragment

@AndroidEntryPoint
class NCDPatientEditActivity : BaseActivity() {
    private lateinit var binding: ActivityNcdPatientEditBinding
    private val patientDetailViewModel: PatientDetailViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Shared with the (landscape) NCD flow. The nurse flow is portrait, so keep the edit
        // page in portrait when launched by a nurse instead of forcing the manifest landscape.
        requestedOrientation = if (CommonUtils.isNurse()) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        binding = ActivityNcdPatientEditBinding.inflate(layoutInflater)
        setMainContentView(
            binding.root,
            true,
            getString(R.string.patient_details),
            homeAndBackVisibility = Pair(true, null),
        )
        showVerticalMoreIcon(false)
        getPatientDetails()
        loadFragment()
    }

    private fun getPatientDetails() {
        intent?.let {
            patientDetailViewModel.origin = it.getStringExtra(DefinedParams.ORIGIN)
            // PATIENT_REFERENCE (patient FHIR id) and MEMBER_REFERENCE (member FHIR id) are passed
            // in by the launching flow. Do NOT overwrite PATIENT_REFERENCE here: getPatientId() is
            // still null at this point (details are loaded asynchronously below), so overwriting it
            // would drop the patient FHIR id from the /patient/update payload.
            it.getStringExtra(NCDMRUtil.MEMBER_REFERENCE)?.let { id ->
                patientDetailViewModel.getPatients(
                    id,
                    origin = MenuConstants.MY_PATIENTS_MENU_ID,
                )
            }
        }
    }

    private fun loadFragment() {
        replaceFragmentInId<NCDPatientEditFragment>(
            binding.fragmentContainerAssessment.id,
            tag = NCDPatientEditFragment.TAG,
        )
    }

    private inline fun <reified fragment : Fragment> replaceFragmentInId(
        id: Int? = null,
        bundle: Bundle? = null,
        tag: String? = null,
    ) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace<fragment>(
                id ?: binding.fragmentContainerAssessment.id,
                args = bundle,
                tag = tag,
            )
        }
        hideHomeButton(true)
    }
}
