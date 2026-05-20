package org.medtroniclabs.uhis.ui.household.summary

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.LinearLayoutManager
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.DefinedParams.TB
import org.medtroniclabs.uhis.common.DefinedParams.isMemberRegistration
import org.medtroniclabs.uhis.data.offlinesync.model.HouseholdMemberWithTb
import org.medtroniclabs.uhis.databinding.ActivityHouseholdSummaryBinding
import org.medtroniclabs.uhis.formgeneration.extension.capitalizeFirstChar
import org.medtroniclabs.uhis.formgeneration.extension.safePopupMenuClickListener
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.assessment.AssessmentActivity
import org.medtroniclabs.uhis.ui.dialog.LinkPatientDialogFragment
import org.medtroniclabs.uhis.ui.dialog.SuccessDialogFragment
import org.medtroniclabs.uhis.ui.household.HouseholdActivity
import org.medtroniclabs.uhis.ui.household.HouseholdDefinedParams
import org.medtroniclabs.uhis.ui.household.MemberSelectionListener
import org.medtroniclabs.uhis.ui.household.viewmodel.HouseHoldSummaryViewModel
import org.medtroniclabs.uhis.ui.landing.LandingActivity
import org.medtroniclabs.uhis.ui.landing.OnDialogDismissListener

class HouseholdSummaryActivity : BaseActivity(), MemberSelectionListener, View.OnClickListener, OnDialogDismissListener {
    private lateinit var binding: ActivityHouseholdSummaryBinding

    private val householdSummaryViewModel: HouseHoldSummaryViewModel by viewModels()

    private var linkHouseholdId: Long = -1
    private var linkMemberId: Long = -1
    private var linkFhirMemberId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHouseholdSummaryBinding.inflate(layoutInflater)
        showLoading()
        setMainContentView(
            binding.root,
            isToolbarVisible = true,
            title = getString(R.string.households),
        )
        initializeView()
        setListener()
        attachObserver()
    }

    override fun onResume() {
        super.onResume()
        if (!householdSummaryViewModel.isPhuWalkInsFlow) {
            householdSummaryViewModel.setUserJourney(AnalyticsDefinedParams.HouseHoldSummary)
        }
    }

    private fun attachObserver() {
        householdSummaryViewModel.householdCardDetailLiveData.observe(this) {
            val householdName = it.firstOrNull()?.name?.capitalizeFirstChar() ?: ""
            setTitle(getString(R.string.household_family, householdName))
            initializePhuLinkFlow()
        }

        householdSummaryViewModel.householdMembersLiveData.observe(this) { data ->
            initializeAdapter(data)
            if (!intent.getBooleanExtra(HouseholdDefinedParams.IS_PHU_WALK_INS_FLOW, false)) {
                if (householdSummaryViewModel.previousCount != 0 && (householdSummaryViewModel.previousCount < data.size)) {
                    data.last().id.let {
                        val existingFragment =
                            supportFragmentManager.findFragmentByTag(SuccessDialogFragment.TAG)
                        if (existingFragment == null) {
                            SuccessDialogFragment
                                .newInstance(isMember = true)
                                .show(supportFragmentManager, SuccessDialogFragment.TAG)
                        }
                    }
                }
                householdSummaryViewModel.previousCount = data.size
            }
        }
    }

    private fun initializeAdapter(data: List<HouseholdMemberWithTb>) {
        val householdListAdapter =
            HouseholdMemberListAdapter(data, this, householdSummaryViewModel.isPhuWalkInsFlow, isTranslationEnabled)
        binding.rvHouseholdList.apply {
            layoutManager = LinearLayoutManager(this@HouseholdSummaryActivity)
            adapter = householdListAdapter
            adapter?.let {
                hideLoading()
            }
        }
    }

    private fun initializeView() {
        initializePhuLinkFlow()
        val householdId = intent.getLongExtra(DefinedParams.householdId, -1)
        linkHouseholdId = householdId
        linkMemberId = intent.getLongExtra(DefinedParams.MEMBER_ID, -1)
        linkFhirMemberId = intent.getLongExtra(DefinedParams.FhirMemberID, -1)
        householdSummaryViewModel.setHouseholdId(householdId)
        householdSummaryViewModel.isFromHouseHoldRegistration = intent.getBooleanExtra(
            HouseholdDefinedParams.IS_FROM_HOUSEHOLD_REGISTRATION,
            false,
        )
        supportFragmentManager
            .beginTransaction()
            .add(binding.fragmentContainer.id, HouseholdDetailsFragment())
            .commit()
        handleBottomNavigation()
    }

    private fun initializePhuLinkFlow() {
        householdSummaryViewModel.isPhuWalkInsFlow = intent.getBooleanExtra(HouseholdDefinedParams.IS_PHU_WALK_INS_FLOW, false)
        if (householdSummaryViewModel.isPhuWalkInsFlow) {
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
            0,
        )
        constraintSet.applyTo(constraintLayout)
    }

    private fun onBackNav() {
        showErrorDialogue(
            getString(R.string.alert),
            getString(R.string.exit_reason),
            isNegativeButtonNeed = true,
        ) { isPositive ->
            if (isPositive) {
                val intent = Intent(this, LandingActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun handleBottomNavigation() {
        if (householdSummaryViewModel.isFromHouseHoldRegistration) {
            binding.bottomNavigationView.visibility = View.VISIBLE
            showHideVerticalIcon(false)
        } else {
            binding.bottomNavigationView.visibility = View.VISIBLE
            binding.btnFinishRegistration.gone()
            showHideVerticalIcon(true)
        }
        if (householdSummaryViewModel.isPhuWalkInsFlow) {
            binding.bottomNavigationViewLinkPatient.visibility = View.VISIBLE
            showHideVerticalIcon(false)
        } else {
            binding.bottomNavigationViewLinkPatient.visibility = View.GONE
            showHideVerticalIcon(false)
        }

        if (!householdSummaryViewModel.isFromHouseHoldRegistration && !householdSummaryViewModel.isPhuWalkInsFlow) {
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

//                    R.id.editHousehold -> {
//                        editHouseholdDetails()
//                    }

                    R.id.editMember -> {
                        MemberEditDialogFragment
                            .newInstance(this@HouseholdSummaryActivity)
                            .show(supportFragmentManager, MemberEditDialogFragment.TAG)
                    }

                    R.id.memberDeceased -> {
                        MemberDeceasedDialogFragment
                            .newInstance()
                            .show(supportFragmentManager, MemberDeceasedDialogFragment.TAG)
                    }
                }
                return true
            }
        })
        popupMenu.setForceShowIcon(true)
        popupMenu.show()
    }

    override fun onMemberSelected(
        item: Long,
        isEdit: Boolean,
        dateOfBirth: String?,
        isContactTrace: Boolean,
        houseHoldId: Long?,
    ) {
        if (isContactTrace) {
            val intent = Intent(this, AssessmentActivity::class.java)
            intent.putExtra(DefinedParams.MEMBER_ID, item)
            intent.putExtra(DefinedParams.DOB, dateOfBirth)
            intent.putExtra(DefinedParams.MENU_ID, TB)
            intent.putExtra(DefinedParams.FhirId, linkFhirMemberId)
            intent.putExtra(DefinedParams.CONTACT_TRACING, true)
            startActivity(intent)
        } else {
            if (isEdit) {
                val intent = Intent(this, HouseholdActivity::class.java)
                intent.putExtra(DefinedParams.MEMBER_ID, item)
                intent.putExtra(DefinedParams.householdId, householdSummaryViewModel.houseHoldId)
                startActivity(intent)
            } else {
                val intent = Intent(this, MemberSummaryActivity::class.java)
                intent.putExtra(DefinedParams.HOUSEHOLD_ID, houseHoldId)
                intent.putExtra(DefinedParams.MEMBER_ID, item)
                intent.putExtra(DefinedParams.DOB, dateOfBirth)
                startActivity(intent)
            }
        }
    }

    private fun setListener() {
        binding.btnAddNewMember.setOnClickListener(this)
        binding.btnFinishRegistration.setOnClickListener(this)
        binding.btnLinkPatient.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnFinishRegistration -> {
                withLocationCheck(::finishRegistrationFlow)
            }

            R.id.btnAddNewMember -> {
                withLocationCheck(::addNewMember)
            }

            R.id.btnLinkPatient -> {
                withLocationCheck(::linkPatient)
            }
        }
    }

    private fun finishRegistrationFlow() {
        householdSummaryViewModel.setUserJourney(AnalyticsDefinedParams.FINISHBUTTONTRIGGERED)
        householdSummaryViewModel.householdCardDetailLiveData.value?.let {
            val existingFragment =
                supportFragmentManager.findFragmentByTag(SuccessDialogFragment.TAG)
            if (existingFragment == null) {
                SuccessDialogFragment
                    .newInstance(isHousehold = true)
                    .show(supportFragmentManager, SuccessDialogFragment.TAG)
            }
        }
    }

    private fun linkPatient() {
        val existingFragment =
            supportFragmentManager.findFragmentByTag(LinkPatientDialogFragment.TAG)
        if (existingFragment == null) {
            LinkPatientDialogFragment
                .newInstance(linkHouseholdId, linkMemberId, linkFhirMemberId)
                .show(supportFragmentManager, LinkPatientDialogFragment.TAG)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        linkHouseholdId = intent.getLongExtra(DefinedParams.householdId, -1)
        linkMemberId = intent.getLongExtra(DefinedParams.MEMBER_ID, -1)
        linkFhirMemberId = intent.getLongExtra(DefinedParams.FhirMemberID, -1)
    }

    private fun addNewMember() {
        if (householdSummaryViewModel.houseHoldId != -1L) {
            val intent =
                Intent(this@HouseholdSummaryActivity, HouseholdActivity::class.java)
            intent.putExtra(isMemberRegistration, true)
            intent.putExtra(DefinedParams.householdId, householdSummaryViewModel.houseHoldId)
            startActivity(intent)
        }
    }

    private fun editHouseholdDetails() {
        if (householdSummaryViewModel.houseHoldId != -1L) {
            val intent =
                Intent(this@HouseholdSummaryActivity, HouseholdActivity::class.java)
            intent.putExtra(isMemberRegistration, false)
            intent.putExtra(DefinedParams.householdId, householdSummaryViewModel.houseHoldId)
            startActivity(intent)
        }
    }

    override fun onDialogDismissListener(isFinish: Boolean) {
        if (isFinish) {
            finish()
        }
    }
}
