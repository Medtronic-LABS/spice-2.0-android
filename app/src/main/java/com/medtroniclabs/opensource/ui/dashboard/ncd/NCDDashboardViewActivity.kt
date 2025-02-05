package com.medtroniclabs.opensource.ui.dashboard.ncd

import android.content.pm.ActivityInfo
import android.os.Bundle
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.isTablet
import com.medtroniclabs.opensource.databinding.ActivityNcdDashboardVeiwBinding
import com.medtroniclabs.opensource.ui.BaseActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NCDDashboardViewActivity : BaseActivity() {
    private lateinit var binding: ActivityNcdDashboardVeiwBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = if (isTablet()) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        binding = ActivityNcdDashboardVeiwBinding.inflate(layoutInflater)
        setMainContentView(
            binding.root,
            isToolbarVisible = true,
            title = getString(R.string.dashboard),
            homeAndBackVisibility = Pair(false, true)
        )
        loadSearchFragment()
    }

    private fun loadSearchFragment() {
        replaceFragmentInId<DashboardFragment>(
            R.id.fragmentContainerView,
            bundle = intent.extras,
            tag = DashboardFragment.TAG
        )
    }
}