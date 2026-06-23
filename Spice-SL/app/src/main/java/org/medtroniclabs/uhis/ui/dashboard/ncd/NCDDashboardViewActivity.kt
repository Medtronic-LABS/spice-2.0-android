package org.medtroniclabs.uhis.ui.dashboard.ncd

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams.ONMOREBUTTONTRIGGERED
import org.medtroniclabs.uhis.databinding.ActivityNcdDashboardVeiwBinding
import org.medtroniclabs.uhis.formgeneration.extension.safePopupMenuClickListener
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.dashboard.ncd.viewmodel.NCDDashBoardViewModel
import kotlin.getValue

@AndroidEntryPoint
class NCDDashboardViewActivity : BaseActivity() {
    private lateinit var binding: ActivityNcdDashboardVeiwBinding

    private val viewModel: NCDDashBoardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNcdDashboardVeiwBinding.inflate(layoutInflater)
        setMainContentView(
            binding.root,
            isToolbarVisible = true,
            title = getString(R.string.dashboard),
            homeAndBackVisibility = Pair(false, true),
        )
        loadDashboardFragment()
        showVerticalMoreIcon(true) {
            onMoreIconClicked(it)
        }
    }

    private fun onMoreIconClicked(view: View) {
        viewModel.setUserJourney(ONMOREBUTTONTRIGGERED)
        val popupMenu = PopupMenu(this, view)
        popupMenu.menuInflater.inflate(R.menu.dashboard_menu, popupMenu.menu)
        popupMenu.safePopupMenuClickListener(object :
            android.widget.PopupMenu.OnMenuItemClickListener,
            PopupMenu.OnMenuItemClickListener {
            override fun onMenuItemClick(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.menu_item_share -> {
                        viewModel.triggerShare()
                    }
                }
                return true
            }
        })
        popupMenu.setForceShowIcon(true)
        popupMenu.show()
    }

    override fun consumeImeInsets() = true

    private fun loadDashboardFragment() {
        replaceFragmentInId<DashboardFragment>(
            R.id.fragmentContainerView,
            bundle = intent.extras,
            tag = DashboardFragment.TAG,
        )
    }
}
