package org.medtroniclabs.uhis.ui.followup.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.medtroniclabs.uhis.ui.followup.fragment.FollowUpPatientListFragment

class FollowUpPatientListAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
    FragmentStateAdapter(fragmentManager, lifecycle) {
    override fun getItemCount(): Int = 2

    override fun createFragment(p0: Int): Fragment = FollowUpPatientListFragment()
}
