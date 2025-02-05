package com.medtroniclabs.opensource.ui.common

import android.os.Bundle
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.databinding.ActivityPatientSearchBinding
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.mypatients.fragment.PatientSearchFragment

class PatientSearchActivity : BaseActivity() {

    private lateinit var binding: ActivityPatientSearchBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPatientSearchBinding.inflate(layoutInflater)
        setMainContentView(
            binding.root, isToolbarVisible = true, title = getString(R.string.search_patient)
        )

        loadSearchFragment()
    }

    private fun loadSearchFragment() {
        replaceFragmentInId<PatientSearchFragment>(
            R.id.fragmentContainerView,
            bundle = intent.extras,
            tag = PatientSearchFragment.TAG
        )
    }
}