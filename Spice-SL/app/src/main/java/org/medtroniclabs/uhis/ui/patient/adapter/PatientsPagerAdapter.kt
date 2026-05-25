package org.medtroniclabs.uhis.ui.patient.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.medtroniclabs.uhis.ui.patient.fragment.PatientListFragment

class PatientsPagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    private val tabs: Array<String>,
    private val origin: String,
) :
    FragmentStateAdapter(fragmentManager, lifecycle) {
    override fun getItemCount(): Int = tabs.size

    override fun createFragment(position: Int): Fragment = PatientListFragment.newInstance(origin, position)
}
