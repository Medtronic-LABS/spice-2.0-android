package org.medtroniclabs.uhis.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import kotlinx.coroutines.launch
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.databinding.FragmentToolsMenuBinding
import org.medtroniclabs.uhis.db.entity.MenuEntity
import org.medtroniclabs.uhis.ncd.medicalreview.NCDMRUtil.EncounterReference
import org.medtroniclabs.uhis.ncd.medicalreview.NCDMRUtil.MENU_ID
import org.medtroniclabs.uhis.ncd.medicalreview.NCDMedicalReviewActivity
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.MenuConstants
import org.medtroniclabs.uhis.ui.MenuConstants.DIALOG_RESULT
import org.medtroniclabs.uhis.ui.MenuConstants.WORKFLOW_NAME
import org.medtroniclabs.uhis.ui.assessment.AssessmentActivity
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH
import org.medtroniclabs.uhis.ui.cbs.activity.CbsActivity
import org.medtroniclabs.uhis.ui.home.adapter.DashboardMenuItemsAdapter

class ToolsMenuFragment : BaseFragment(), MenuSelectionListener {
    private lateinit var binding: FragmentToolsMenuBinding
    private val viewModel: ToolsViewModel by activityViewModels()

    companion object {
        const val TAG = "ToolsMenuFragment"

        fun newInstance(): ToolsMenuFragment = ToolsMenuFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentToolsMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        childFragmentManager.setFragmentResultListener(
            DIALOG_RESULT,
            viewLifecycleOwner,
        ) { _, bundle ->
            val result = bundle.getString(WORKFLOW_NAME)
            startAssessmentActivity(MenuConstants.RMNCH_MENU_ID, result)
        }
        viewModel.getMenuForClinicalWorkflows(requireArguments().getString(DefinedParams.GENDER))
        attachObservers()
        if (getEncounterReference().isNotBlank()) {
            setTitle(requireContext().getString(R.string.home_title))
        }

        // Deeplink directly goes to Medical review
        if (requireArguments().getBoolean(DefinedParams.IS_DEEP_LINK)) {
            if (getOrigin().equals(MenuConstants.MY_PATIENTS_MENU_ID, true)) {
                val intent =
                    Intent(requireContext(), NCDMedicalReviewActivity::class.java).apply {
                        putExtra(EncounterReference, getEncounterReference())
                        putExtra(MENU_ID, "ncd")
                        putExtra(DefinedParams.FhirId, getMemberReference())
                        putExtra(DefinedParams.PatientId, getPatientReference())
                        putExtra(DefinedParams.ORIGIN, getOrigin())
                    }
                startActivity(intent)
            } else {
                startAssessmentActivity("ncd", null)
            }
        }
    }

    private fun attachObservers() {
        viewModel.menuListLiveData.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    (activity as BaseActivity).showLoading()
                }

                ResourceState.SUCCESS -> {
                    (activity as BaseActivity).hideLoading()
                    resourceState.data?.let {
                        setAdapterViews(it)
                    }
                }

                ResourceState.ERROR -> {
                    (activity as BaseActivity).hideLoading()
                }
            }
        }
    }

    private fun setAdapterViews(menus: List<MenuEntity>) {
        if (CommonUtils.checkIsTablet(requireContext())) {
            val layoutManager = FlexboxLayoutManager(context)
            layoutManager.flexDirection = FlexDirection.ROW
            layoutManager.justifyContent = JustifyContent.CENTER
            binding.rvActivitiesList.layoutManager = layoutManager
        } else {
            val layoutManager = GridLayoutManager(context, 2)
            binding.rvActivitiesList.layoutManager = layoutManager
        }
        lifecycleScope.launch {
            val resolvedMenus = getResolvedMenuLabels(menus)
            binding.rvActivitiesList.adapter = DashboardMenuItemsAdapter(resolvedMenus, this@ToolsMenuFragment)
        }
    }

    private fun getResolvedMenuLabels(menus: List<MenuEntity>): List<MenuEntity> =
        menus.map { menu ->
            when (menu.menuId.lowercase()) {
                MenuConstants.RMNCH_MENU_ID.lowercase() -> {
                    // Keep a single RMNCH tile and only update its display label based on active workflow.
                    val rmnchTitle = when (menu.subModule) {
                        RMNCH.ANC -> getString(R.string.anc)
                        RMNCH.PNC -> getString(R.string.pnc)
                        else -> getString(R.string.child_health)
                    }
                    menu.copy(name = rmnchTitle, displayValue = null)
                }

                MenuConstants.PREGNANCY_OUTCOME.lowercase() -> {
                    menu.copy(name = getString(R.string.pregnancy_outcome), displayValue = null)
                }

                MenuConstants.PREGNANT_WOMEN_PROFILE.lowercase() -> {
                    menu.copy(name = getString(R.string.pregnant_women_profile), displayValue = null)
                }

                MenuConstants.FP_MENU_ID.lowercase() -> {
                    menu.copy(name = getString(R.string.family_planning), displayValue = null)
                }

                MenuConstants.NCD_MENU_ID.lowercase() -> {
                    menu.copy(name = getString(R.string.ncd), displayValue = null)
                }

                MenuConstants.EYE_CARE_MENU_ID.lowercase() -> {
                    menu.copy(name = getString(R.string.eye_care), displayValue = null)
                }

                MenuConstants.CATARACT_MENU_ID.lowercase() -> {
                    menu.copy(name = getString(R.string.cataract), displayValue = null)
                }

                else -> {
                    menu
                }
            }
        }

    override fun onMenuSelected(
        menuId: String,
        subModule: String?,
    ) {
        startAssessmentToolsActivity(menuId, subModule)
    }

    private fun startAssessmentToolsActivity(
        menuId: String,
        subModule: String? = null,
    ) {
        when (menuId) {
            MenuConstants.RMNCH_MENU_ID -> {
                subModule?.let {
                    startAssessmentActivity(MenuConstants.RMNCH_MENU_ID, subModule)
                }
//                if (subModule == null) {
//                    lifecycleScope.launch {
//                        viewModel.getANCPNCStatus()?.let { workflowName ->
//                            startAssessmentActivity(MenuConstants.RMNCH_MENU_ID, workflowName)
//                        }
//                    }
//                } else {
//                    startAssessmentActivity(MenuConstants.RMNCH_MENU_ID, RMNCH.ChildHoodVisit)
//                }
            }

            MenuConstants.CBS_MENU_ID.uppercase() -> {
                startCbsActivity(menuId)
            }

            MenuConstants.CBS_MENU_ID -> {
                startCbsActivity(menuId)
            }

            else -> {
                if (getOrigin().equals(MenuConstants.MY_PATIENTS_MENU_ID, true)) {
                    val intent =
                        Intent(requireContext(), NCDMedicalReviewActivity::class.java).apply {
                            putExtra(EncounterReference, getEncounterReference())
                            putExtra(MENU_ID, menuId)
                            putExtra(DefinedParams.FhirId, getMemberReference())
                            putExtra(DefinedParams.PatientId, getPatientReference())
                            putExtra(DefinedParams.ORIGIN, getOrigin())
                        }
                    startActivity(intent)
                } else {
                    startAssessmentActivity(menuId, null)
                }
            }
        }
    }

    private fun startCbsActivity(menuId: String) {
        val intent = Intent(requireContext(), CbsActivity::class.java)
        intent.putExtra(DefinedParams.MEMBER_ID, viewModel.selectedHouseholdMemberID)
        intent.putExtra(DefinedParams.DOB, viewModel.selectedMemberDob)
        intent.putExtra(DefinedParams.FOLLOW_UP_ID, viewModel.followUpId)
        intent.putExtra(DefinedParams.MENU_ID, menuId)
        intent.putExtra(DefinedParams.FhirId, getMemberReference())
        intent.putExtra(DefinedParams.ORIGIN, getOrigin())
        startActivity(intent)
    }

    private fun startAssessmentActivity(
        menuId: String,
        workFlowName: String?,
    ) {
        val intent = Intent(requireContext(), AssessmentActivity::class.java)
        intent.putExtra(DefinedParams.HOUSEHOLD_ID, viewModel.selectedHouseholdId)
        intent.putExtra(DefinedParams.MEMBER_ID, viewModel.selectedHouseholdMemberID)
        intent.putExtra(DefinedParams.DOB, viewModel.selectedMemberDob)
        intent.putExtra(MenuConstants.FOLLOW_UP, isFollowUp())
        intent.putExtra(DefinedParams.FOLLOW_UP_ID, viewModel.followUpId)
        intent.putExtra(DefinedParams.MENU_ID, menuId)
        intent.putExtra(DefinedParams.FhirId, getMemberReference())
        intent.putExtra(DefinedParams.ORIGIN, getOrigin())
        intent.putExtra(DefinedParams.ENTRY_POINT, viewModel.entryPoint)
        workFlowName?.let { name ->
            intent.putExtra(WORKFLOW_NAME, name)
        }
        startActivity(intent)
    }

    private fun getPatientReference(): String? = requireArguments().getString(DefinedParams.PatientId)

    private fun getMemberReference(): String? = requireArguments().getString(DefinedParams.FhirId)

    private fun isFollowUp(): Boolean = requireArguments().getBoolean(MenuConstants.FOLLOW_UP)

    private fun getOrigin(): String = requireArguments().getString(DefinedParams.ORIGIN) ?: ""

    private fun getEncounterReference(): String = requireArguments().getString(EncounterReference) ?: ""
}
