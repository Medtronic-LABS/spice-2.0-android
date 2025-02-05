package com.medtroniclabs.opensource.ui.household.summary

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.GridLayoutManager
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.app.analytics.utils.AnalyticsDefinedParams
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.DefinedParams.MemberID
import com.medtroniclabs.opensource.common.DefinedParams.isMemberRegistration
import com.medtroniclabs.opensource.databinding.ActivityHouseholdSummaryBinding
import com.medtroniclabs.opensource.db.entity.HouseholdMemberEntity
import com.medtroniclabs.opensource.formgeneration.extension.capitalizeFirstChar
import com.medtroniclabs.opensource.formgeneration.extension.safePopupMenuClickListener
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.dialog.LinkPatientDialogFragment
import com.medtroniclabs.opensource.ui.dialog.SuccessDialogFragment
import com.medtroniclabs.opensource.ui.home.AssessmentToolsActivity
import com.medtroniclabs.opensource.ui.household.HouseholdActivity
import com.medtroniclabs.opensource.ui.household.HouseholdDefinedParams.ID
import com.medtroniclabs.opensource.ui.household.HouseholdDefinedParams.isFromHouseHoldRegistration
import com.medtroniclabs.opensource.ui.household.HouseholdDefinedParams.isPhuWalkInsFlow
import com.medtroniclabs.opensource.ui.household.MemberSelectionListener
import com.medtroniclabs.opensource.ui.household.viewmodel.HouseHoldSummaryViewModel
import com.medtroniclabs.opensource.ui.landing.LandingActivity
import com.medtroniclabs.opensource.ui.landing.OnDialogDismissListener

class HouseholdSummaryActivity : BaseActivity(), MemberSelectionListener, View.OnClickListener, OnDialogDismissListener {

    private lateinit var binding: ActivityHouseholdSummaryBinding

    private val householdSummaryViewModel: HouseHoldSummaryViewModel by viewModels()

    private var linkHouseholdId: Long = -1
    private var linkMemberId: Long = -1
    private var linkFhirMemberId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHouseholdSummaryBinding.inflate(layoutInflater)
        setMainContentView(
            binding.root, isToolbarVisible = true, title = getString(R.string.households)
        )
        initializeView()
        setListener()
        attachObserver()
        householdSummaryViewModel.setUserJourney(AnalyticsDefinedParams.HouseHoldSummary)
    }

    private fun attachObserver() {
        householdSummaryViewModel.householdCardDetailLiveData.observe(this) {
            setTitle(it.name.capitalizeFirstChar() +" "+ getString(R.string.household))
            initializePhuLinkFlow()
        }

        householdSummaryViewModel.householdMembersLiveData.observe(this) { data ->
            initializeAdapter(data)
            if (!intent.getBooleanExtra(isPhuWalkInsFlow, false)) {
                if (householdSummaryViewModel.previousCount != 0 && (householdSummaryViewModel.previousCount < data.size)) {
                    data.last().id?.let {
                        val existingFragment =
                            supportFragmentManager.findFragmentByTag(SuccessDialogFragment.TAG)
                        if (existingFragment == null) {
                            SuccessDialogFragment.newInstance(isMember = true)
                                .show(supportFragmentManager, SuccessDialogFragment.TAG)
                        }
                    }
                }
                householdSummaryViewModel.previousCount = data.size
            }
        }
    }

    private fun initializeAdapter(data: List<HouseholdMemberEntity>) {
        val householdListAdapter = HouseholdMemberListAdapter(data, this,householdSummaryViewModel.isPhuWalkInsFlow)
        binding.rvHouseholdList.apply {
            layoutManager =
                GridLayoutManager(this@HouseholdSummaryActivity, DefinedParams.span_count_1)
            adapter = householdListAdapter
        }
    }

    private fun initializeView() {
        initializePhuLinkFlow()
        val householdId = intent.getLongExtra(ID, -1)
        linkHouseholdId = householdId
        linkMemberId = intent.getLongExtra(MemberID, -1)
        linkFhirMemberId = intent.getLongExtra(DefinedParams.FhirMemberID,-1)
        householdSummaryViewModel.setHouseholdId(householdId)
        householdSummaryViewModel.isFromHouseHoldRegistration = intent.getBooleanExtra(
            isFromHouseHoldRegistration, false
        )
        supportFragmentManager.beginTransaction()
            .add(binding.fragmentContainer.id, HouseholdDetailsFragment()).commit()
        handleBottomNavigation()
    }

    private fun initializePhuLinkFlow(){
        householdSummaryViewModel.isPhuWalkInsFlow=intent.getBooleanExtra(isPhuWalkInsFlow, false)
        if ( householdSummaryViewModel.isPhuWalkInsFlow){
            changeBottomConstraint()
            onHomeClick { onBackNav() }
            hideHomeButton(false)
            setTitle(getString(R.string.link_patient))
        }
    }
    private fun changeBottomConstraint() {
        val constraintLayout = findViewById<ConstraintLayout>(R.id.constraintLayout)
        val constraintSet = ConstraintSet()
        constraintSet.clone(constraintLayout)
                 constraintSet.connect(
                R.id.scrollContainer,
                ConstraintSet.BOTTOM,
                R.id.bottomNavigationViewLinkPatient,
                ConstraintSet.TOP,
                0
            )
        constraintSet.applyTo(constraintLayout)
    }

    private fun onBackNav(){
        showErrorDialogue(
            getString(R.string.alert),
            getString(R.string.exit_reason),
            isNegativeButtonNeed = true
        ) { isPositive ->
            if (isPositive) {
                val intent = Intent(this, LandingActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                startActivity(intent)
                finish()            }
        }
    }


    private fun handleBottomNavigation() {
        if (householdSummaryViewModel.isFromHouseHoldRegistration) {
            binding.bottomNavigationView.visibility = View.VISIBLE
            showHideVerticalIcon(false)
        } else {
            binding.bottomNavigationView.visibility = View.GONE
            showHideVerticalIcon(true)
        }
        if (householdSummaryViewModel.isPhuWalkInsFlow){
            binding.bottomNavigationViewLinkPatient.visibility = View.VISIBLE
            showHideVerticalIcon(false)
        }else{
            binding.bottomNavigationViewLinkPatient.visibility = View.GONE
            showHideVerticalIcon(true)
        }

    }

    private fun showHideVerticalIcon(visibility: Boolean) {
        showVerticalMoreIcon(visibility) {
            onMoreIconClicked(it)
        }
    }


    private fun onMoreIconClicked(view: View) {
        val popupMenu = PopupMenu(this@HouseholdSummaryActivity, view)
        popupMenu.menuInflater.inflate(R.menu.menu_house_hold_detail, popupMenu.menu)
        popupMenu.safePopupMenuClickListener(object :
            android.widget.PopupMenu.OnMenuItemClickListener,
            PopupMenu.OnMenuItemClickListener {
            override fun onMenuItemClick(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.addNewMember -> {
                        addNewMember()
                    }
                    R.id.editHousehold -> {
                        editHouseholdDetails()
                    }
                    R.id.editMember -> {
                        MemberEditDialogFragment.newInstance(this@HouseholdSummaryActivity)
                            .show(supportFragmentManager, MemberEditDialogFragment.TAG)
                    }
                }
                return true
            }
        })
        popupMenu.setForceShowIcon(true)
        popupMenu.show()
    }

    override fun onMemberSelected(item: Long, isEdit: Boolean, dateOfBirth: String?) {
        if (isEdit){
            val intent = Intent(this, HouseholdActivity::class.java)
            intent.putExtra(DefinedParams.MemberID, item)
            startActivity(intent)
        } else {
            val intent = Intent(this, AssessmentToolsActivity::class.java)
            intent.putExtra(DefinedParams.MemberID, item)
            intent.putExtra(DefinedParams.DOB, dateOfBirth)
            startActivity(intent)
        }
    }

    private fun setListener() {
        binding.btnAddNewMember.setOnClickListener(this)
        binding.btnFinishRegistration.setOnClickListener(this)
        binding.btnLinkPatient.setOnClickListener(this)
        /*onBackPressedDispatcher.addCallback(this) {
            onHouseHoldSummaryActivity()
        }*/
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnFinishRegistration -> {
                householdSummaryViewModel.householdCardDetailLiveData.value?.let {
                    val existingFragment =
                        supportFragmentManager.findFragmentByTag(SuccessDialogFragment.TAG)
                    if (existingFragment == null) {
                        SuccessDialogFragment.newInstance(isHousehold = true).show(supportFragmentManager, SuccessDialogFragment.TAG)
                    }
                }
            }

            R.id.btnAddNewMember -> {
                addNewMember()
            }
            R.id.btnLinkPatient->{
                val existingFragment =
                    supportFragmentManager.findFragmentByTag(LinkPatientDialogFragment.TAG)
                if (existingFragment == null) {
                    LinkPatientDialogFragment.newInstance(linkHouseholdId,linkMemberId,linkFhirMemberId).show(supportFragmentManager, LinkPatientDialogFragment.TAG)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        linkHouseholdId = intent.getLongExtra(ID, -1)
        linkMemberId = intent.getLongExtra(MemberID, -1)
        linkFhirMemberId = intent.getLongExtra(DefinedParams.FhirMemberID,-1)
    }

    private fun addNewMember() {
        if (householdSummaryViewModel.houseHoldId != -1L) {
            val intent =
                Intent(this@HouseholdSummaryActivity, HouseholdActivity::class.java)
            intent.putExtra(isMemberRegistration, true)
            intent.putExtra(ID, householdSummaryViewModel.houseHoldId)
            startActivity(intent)
        }
    }
    private fun editHouseholdDetails() {
        if (householdSummaryViewModel.houseHoldId != -1L) {
            val intent =
                Intent(this@HouseholdSummaryActivity, HouseholdActivity::class.java)
            intent.putExtra(isMemberRegistration, false)
            intent.putExtra(ID, householdSummaryViewModel.houseHoldId)
            startActivity(intent)
        }
    }

    override fun onDialogDismissListener(isFinish: Boolean) {
        if (isFinish){
            finish()
        }
    }

}