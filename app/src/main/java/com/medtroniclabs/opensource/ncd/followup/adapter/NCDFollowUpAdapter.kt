package com.medtroniclabs.opensource.ncd.followup.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.ncd.followup.NCDFollowUpUtils.Assessment_Type
import com.medtroniclabs.opensource.ncd.followup.NCDFollowUpUtils.Defaulters_Type
import com.medtroniclabs.opensource.ncd.followup.NCDFollowUpUtils.LTFU_Type
import com.medtroniclabs.opensource.ncd.followup.NCDFollowUpUtils.SCREENED
import com.medtroniclabs.opensource.ncd.followup.fragment.NCDFollowUpAssessmentFragment
import com.medtroniclabs.opensource.ncd.followup.fragment.NCDFollowUpLostFragment
import com.medtroniclabs.opensource.ncd.followup.fragment.NCDFollowUpMRFragment
import com.medtroniclabs.opensource.ncd.followup.fragment.NCDFollowUpOfflineSearchFragment
import com.medtroniclabs.opensource.ncd.followup.fragment.NCDFollowUpScreenedFragment

class NCDFollowUpAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle
) : FragmentStateAdapter(fragmentManager, lifecycle) {
    override fun getItemCount(): Int {
        return 4
    }

    override fun createFragment(position: Int): Fragment {
        val type = when (position) {
            0 -> SCREENED
            1 -> Assessment_Type
            2 -> Defaulters_Type
            3 -> LTFU_Type
            else -> ""
        }
        return if (CommonUtils.isChp()) {
            NCDFollowUpOfflineSearchFragment.newInstance(type)
        } else {
            when (position) {
                0 -> NCDFollowUpScreenedFragment.newInstance(type)
                1 -> NCDFollowUpAssessmentFragment.newInstance(type)
                2 -> NCDFollowUpMRFragment.newInstance(type)
                3 -> NCDFollowUpLostFragment.newInstance(type)
                else -> NCDFollowUpScreenedFragment.newInstance(type)
            }
        }
    }
}