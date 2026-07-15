package org.medtroniclabs.uhis.ui.patient

import android.os.Bundle
import android.view.WindowManager
import androidx.fragment.app.add
import androidx.fragment.app.commit
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.databinding.ActivityNurseDashboardBinding
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.patient.fragment.NurseDashboardFragment

class NurseDashboardActivity : BaseActivity() {
    private lateinit var binding: ActivityNurseDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNurseDashboardBinding.inflate(layoutInflater)
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)

        setMainContentView(
            binding.root,
            title = getString(R.string.dashboard),
            isToolbarVisible = true,
            homeAndBackVisibility = Pair(true, true),
        )
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add<NurseDashboardFragment>(binding.fragmentContainerView.id)
            }
        }
    }
}
