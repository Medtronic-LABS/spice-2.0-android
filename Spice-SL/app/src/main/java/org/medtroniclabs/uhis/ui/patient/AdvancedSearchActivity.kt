package org.medtroniclabs.uhis.ui.patient

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.viewpager2.widget.ViewPager2
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.databinding.ActivityAdvancedSearchBinding
import org.medtroniclabs.uhis.databinding.ActivityAdvancedSearchMobileBinding
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.patient.adapter.PatientsPagerAdapter
import org.medtroniclabs.uhis.ui.patient.viewmodel.PatientListViewModel
import kotlin.getValue

open class AdvancedSearchActivity : BaseActivity() {
    private lateinit var binding: Any // Will be ActivityAdvancedSearchBinding or ActivityAdvancedSearchMobileBinding
    private val patientListViewModel: PatientListViewModel by viewModels()

    private val tvTabs = mutableListOf<TextView>()
    private lateinit var viewPager: ViewPager2
    private lateinit var switchTabLayout: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isMobilePortrait = !resources.getBoolean(R.bool.isTablet) &&
            resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT

        if (isMobilePortrait) {
            val mobileBinding = ActivityAdvancedSearchMobileBinding.inflate(layoutInflater)
            binding = mobileBinding
            viewPager = mobileBinding.viewPager
            switchTabLayout = mobileBinding.switchTabLayout.root
            setMainContentView(
                mobileBinding.root,
                title = getString(R.string.search_patient),
                isToolbarVisible = true,
                homeAndBackVisibility = Pair(true, true),
            )
        } else {
            val normalBinding = ActivityAdvancedSearchBinding.inflate(layoutInflater)
            binding = normalBinding
            viewPager = normalBinding.viewPager
            switchTabLayout = normalBinding.switchTabLayout.root
            setMainContentView(
                normalBinding.root,
                title = getString(R.string.search_patient),
                isToolbarVisible = true,
                homeAndBackVisibility = Pair(true, true),
            )
        }

        getIntentValues()
        initViews()
        setPager()
    }

    private fun getIntentValues() {
        patientListViewModel.origin = intent.getStringExtra(DefinedParams.ORIGIN) ?: ""
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun initViews() {
        tvTabs.clear()
        val tabIds = buildList {
            add(R.id.tvTabStart)
            add(R.id.tvTabSecond)
            add(R.id.tvTabMiddle)
            add(R.id.tvTabFourth)
        }

        tabIds.forEachIndexed { index, id ->
            val tv = switchTabLayout.findViewById<TextView>(id)
            tv.safeClickListener {
                patientListViewModel.selectedNavTab = index
                changeTab(index)
                scrollToTab(tv, offset = 50 * index)
            }
            tvTabs.add(tv)
        }

        equalizeTabWidths()
        handleOrientation()
        if ((CommonUtils.isHealthScreener() || CommonUtils.isCHCP()) && patientListViewModel.origin == UIConstants.FOLLOW_UP) {
            viewPager.isUserInputEnabled = false // Restrict scroll
        } else if (patientListViewModel.origin != UIConstants.FOLLOW_UP) {
            viewPager.isUserInputEnabled = false // Restrict scroll
        } else {
            viewPager.registerOnPageChangeCallback(pagerListener)
            viewPager.isUserInputEnabled = true // Allow scroll for others
        }
    }

    private fun scrollToTab(
        view: View,
        offset: Int,
    ) {
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            view.post {
                (switchTabLayout.parent as? HorizontalScrollView)?.smoothScrollTo(
                    view.left - offset,
                    0,
                )
            }
        }
    }

    private fun equalizeTabWidths() {
        switchTabLayout.post {
            val maxWidth = tvTabs.maxOf { it.measuredWidth }
            tvTabs.forEach {
                it.layoutParams = it.layoutParams.apply {
                    width = maxWidth
                }
            }
        }
    }

    private val pagerListener = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int,
        ) {
            super.onPageScrolled(position, positionOffset, positionOffsetPixels)
            updateSelection(position)

            when (position) {
                0 -> {
                    if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                        (switchTabLayout as? HorizontalScrollView)?.smoothScrollTo(0, 0)
                    }
                }

                1 -> {
                    findViewById<View>(R.id.tvTabSecond)?.post {
                        (switchTabLayout as? HorizontalScrollView)?.smoothScrollTo(
                            findViewById<View>(R.id.tvTabSecond)?.left?.minus(0) ?: 0,
                            0,
                        )
                    }
                }

                2 -> {
                    findViewById<View>(R.id.tvTabMiddle)?.post {
                        (switchTabLayout as? HorizontalScrollView)?.smoothScrollTo(
                            findViewById<View>(R.id.tvTabMiddle)?.left?.minus(60) ?: 0,
                            0,
                        )
                    }
                }

                3 -> {
                    findViewById<View>(R.id.tvTabFourth)?.post {
                        (switchTabLayout as? HorizontalScrollView)?.smoothScrollTo(
                            findViewById<View>(R.id.tvTabFourth)?.left?.minus(150) ?: 0,
                            0,
                        )
                    }
                }
            }
        }

        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            patientListViewModel.selectedNavTab = position
            if (position == 0) {
                patientListViewModel.filter?.patientType = null
            }
        }
    }

    private fun changeTab(position: Int) {
        viewPager.setCurrentItem(position, false)
    }

    private fun updateSelection(selectedIndex: Int) {
        tvTabs.forEachIndexed { index, textView ->
            textView.isSelected = index == selectedIndex
        }
    }

    private fun setPager() {
        switchTabLayout.visibility = if (patientListViewModel.origin != UIConstants.FOLLOW_UP) {
            View.GONE
        } else {
            if (CommonUtils.isHealthScreener() || CommonUtils.isCHCP()) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }

        val tabs = tabs()
        if (tabs.isNotEmpty()) {
            applyBg(tabs)
            if ((CommonUtils.isHealthScreener() || CommonUtils.isCHCP()) && patientListViewModel.origin == UIConstants.FOLLOW_UP) {
                viewPager.isUserInputEnabled = false // Restrict scroll
            } else if (patientListViewModel.origin != UIConstants.FOLLOW_UP) {
                viewPager.isUserInputEnabled = false // Restrict scroll
            } else {
                viewPager.isUserInputEnabled = tabs.size > 1 // Allow scroll for others
            }

            val adapter = PatientsPagerAdapter(
                supportFragmentManager,
                lifecycle,
                tabs,
                patientListViewModel.origin,
            )
            viewPager.adapter = adapter
            if (patientListViewModel.selectedNavTab == null) {
                patientListViewModel.selectedNavTab = 0
            }
        }
    }

    private fun tabs(): Array<String> =
        buildList {
            add(getString(R.string.screening))
            addAll(
                listOf(
                    getString(R.string.overdue),
                    getString(R.string.missed_visit),
                    getString(R.string.ltf),
                ),
            )
        }.toTypedArray()

    private fun applyBg(tabs: Array<String>) {
        if (tabs.size == tvTabs.size) {
            tvTabs.forEachIndexed { i, tv ->
                tv.visibility = View.VISIBLE
                tv.text = tabs[i]
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewPager.unregisterOnPageChangeCallback(pagerListener)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        handleOrientation()
    }

    private fun handleOrientation() {
        val tabletSize =
            resources.getBoolean(R.bool.isLargeTablet) || resources.getBoolean(R.bool.isTablet)

        patientListViewModel.spanCount = when {
            tabletSize &&
                resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT &&
                patientListViewModel.origin == UIConstants.FOLLOW_UP -> DefinedParams.SPAN_COUNT_2
            tabletSize -> DefinedParams.SPAN_COUNT_THREE
            else -> DefinedParams.span_count_1
        }

        viewPager.adapter = viewPager.adapter
    }
}
