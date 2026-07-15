package org.medtroniclabs.uhis.ui.household.summary

import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.GravityCompat
import androidx.core.widget.PopupWindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.adjustHeightToView
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.navigateToNext
import org.medtroniclabs.uhis.appextensions.navigateToPrevious
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils.getAgeFromDOB
import org.medtroniclabs.uhis.common.CommonUtils.getGenderText
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.MultiClickListener
import org.medtroniclabs.uhis.databinding.ActivityMemberSummaryBinding
import org.medtroniclabs.uhis.databinding.PopupMemberSummaryDateSelectionBinding
import org.medtroniclabs.uhis.db.entity.HouseholdMemberEntity
import org.medtroniclabs.uhis.db.entity.MemberAssessmentHistoryEntity
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.home.AssessmentToolsActivity
import org.medtroniclabs.uhis.ui.household.adapter.DateSelectionAdapter
import org.medtroniclabs.uhis.ui.household.adapter.MemberAssessmentHistoryAdapter
import org.medtroniclabs.uhis.ui.household.viewmodel.MemberSummaryViewModel

/**
 * Screen to display member details with services they have received
 * Ticket : https://mdtlabs.atlassian.net/browse/UHIS-282
 */
@AndroidEntryPoint
class MemberSummaryActivity : BaseActivity(), View.OnClickListener {
    private lateinit var binding: ActivityMemberSummaryBinding

    private lateinit var pagerHelper: SnapHelper

    private lateinit var layoutManager: LinearLayoutManager

    private lateinit var adapter: MemberAssessmentHistoryAdapter

    private val memberSummaryViewModel: MemberSummaryViewModel by viewModels()

    private var history: List<MemberAssessmentHistoryEntity>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMemberSummaryBinding.inflate(layoutInflater)
        setMainContentView(
            binding.root,
            isToolbarVisible = true,
            homeAndBackVisibility = true to true,
        )
        memberSummaryViewModel.initialize(intent)
        showLoading()
        initializeView()
        setListeners()
        attachObservers()
    }

    override fun onDestroy() {
        binding.rvServiceHistory.removeOnScrollListener(scrollListener)
        super.onDestroy()
    }

    private fun initializeView() {
        supportFragmentManager
            .beginTransaction()
            .add(binding.fragmentContainer.id, MemberDetailsFragment())
            .commit()
        layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        binding.rvServiceHistory.layoutManager = layoutManager
        pagerHelper = PagerSnapHelper()
        pagerHelper.attachToRecyclerView(binding.rvServiceHistory)
    }

    private fun attachObservers() {
        memberSummaryViewModel.memberDetails.observe(this) {
            val history = it?.history
            this.history = history
            if (history.isNullOrEmpty()) {
                binding.rvServiceHistory.gone()
                binding.emptyErrorMessage.visible()
            } else {
                binding.rvServiceHistory.visible()
                binding.emptyErrorMessage.gone()
                adapter = MemberAssessmentHistoryAdapter(history, it.memberPregnancyDetails)
                binding.rvServiceHistory.adapter = adapter
                if (history.size > 1) {
                    binding.ivLeftArrow.visible()
                    binding.ivDate.visible()
                    binding.ivRightArrow.visible()
                    binding.ivLeftArrow.isEnabled = false
                }
                binding.rvServiceHistory.addOnScrollListener(scrollListener)
            }
            it?.member?.let { member ->
                setTitle(getMemberInfoText(member))
            }
            hideLoading()
        }
    }

    private fun getMemberInfoText(item: HouseholdMemberEntity): String =
        SpannableStringBuilder(
            getString(
                R.string.household_summary_member_info,
                item.name,
                getAgeFromDOB(item.dateOfBirth, this),
                getGenderText(item.gender, this),
            ),
        ).toString()

    private fun setListeners() {
        binding.fabServices.setOnClickListener(this)
        binding.ivLeftArrow.setOnClickListener(this)
        binding.ivDate.setOnClickListener(this)
        binding.ivRightArrow.setOnClickListener(this)
        if (memberSummaryViewModel.isNonProdEnv()) {
            getTitleToolbar().setOnClickListener(
                MultiClickListener(5, interval = 1000L) {
                    Toast.makeText(this@MemberSummaryActivity, "Updating date", Toast.LENGTH_SHORT).show()
                    memberSummaryViewModel.decrementAssessmentHistoryDate()
                },
            )
            binding.tvServiceHistoryTitle.setOnClickListener(
                MultiClickListener(5, interval = 1000L) {
                    Toast.makeText(this@MemberSummaryActivity, "Updating date by -14 days", Toast.LENGTH_SHORT).show()
                    memberSummaryViewModel.decrementAssessmentHistoryDate(14)
                },
            )
        }
    }

    /**
     * Updates service history list height after scrolling settles so the snapped card
     * is fully visible and the RecyclerView wraps that card's measured height.
     */
    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(
            recyclerView: RecyclerView,
            newState: Int,
        ) {
            super.onScrollStateChanged(recyclerView, newState)
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                val centerView = pagerHelper.findSnapView(layoutManager)
                if (centerView != null) {
                    binding.rvServiceHistory.adjustHeightToView(centerView)
                }
            }
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.fabServices.id -> {
                val intent = Intent(this, AssessmentToolsActivity::class.java)
                intent.putExtra(DefinedParams.HOUSEHOLD_ID, memberSummaryViewModel.householdId)
                intent.putExtra(DefinedParams.MEMBER_ID, memberSummaryViewModel.memberId)
                intent.putExtra(DefinedParams.DOB, memberSummaryViewModel.dateOfBirth)
                intent.putExtra(DefinedParams.ENTRY_POINT, memberSummaryViewModel.entryPoint)
                startActivity(intent)
            }

            binding.ivLeftArrow.id -> {
                val newPosition = binding.rvServiceHistory.navigateToPrevious()
                binding.ivLeftArrow.isEnabled = newPosition > 0
                binding.ivRightArrow.isEnabled = true
            }

            binding.ivRightArrow.id -> {
                val newPosition = binding.rvServiceHistory.navigateToNext()
                binding.ivLeftArrow.isEnabled = true
                binding.ivRightArrow.isEnabled = newPosition < adapter.itemCount - 1
            }

            binding.ivDate.id -> {
                history?.let { history ->
                    showDateSelectionPopUpWindow(history)
                }
            }
        }
    }

    private fun showDateSelectionPopUpWindow(history: List<MemberAssessmentHistoryEntity>) {
        val dateSelectionBinding = PopupMemberSummaryDateSelectionBinding.inflate(layoutInflater)
        val popUpWindow = PopupWindow(
            dateSelectionBinding.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true,
        )
        popUpWindow.isOutsideTouchable = true
        val adapter = DateSelectionAdapter(
            history,
        ) { position ->
            popUpWindow.dismiss()
            binding.ivLeftArrow.isEnabled = position > 0
            binding.ivRightArrow.isEnabled = position < adapter.itemCount - 1
            binding.rvServiceHistory.smoothScrollToPosition(position)
        }
        dateSelectionBinding.rvDateList.adapter = adapter
        PopupWindowCompat.showAsDropDown(
            popUpWindow,
            binding.ivDate,
            0,
            0,
            GravityCompat.START,
        )
    }
}
