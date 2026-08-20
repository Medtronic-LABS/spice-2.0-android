package org.medtroniclabs.uhis.ui.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.medtroniclabs.microcoaching.Language
import com.medtroniclabs.microcoaching.MicroCoachingSDK
import com.medtroniclabs.microcoaching.ui.chat.CoachingChatBottomSheet
import com.medtroniclabs.microcoaching.ui.components.ChatFab
import com.medtroniclabs.microcoaching.ui.flow.CoachingFlowActivity
import com.medtroniclabs.microcoaching.ui.theme.MicroCoachingTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.databinding.FragmentHomeScreenBinding
import org.medtroniclabs.uhis.db.dao.FollowUpDao
import org.medtroniclabs.uhis.db.entity.MenuEntity
import org.medtroniclabs.uhis.microcoaching.toTodaysVisit
import org.medtroniclabs.uhis.ncd.followup.activity.NCDFollowUpActivity
import org.medtroniclabs.uhis.ncd.screening.ui.ScreeningActivity
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.MenuConstants
import org.medtroniclabs.uhis.ui.common.PatientSearchActivity
import org.medtroniclabs.uhis.ui.communityprofile.CommunityProfileActivity
import org.medtroniclabs.uhis.ui.dashboard.ncd.NCDDashboardViewActivity
import org.medtroniclabs.uhis.ui.followup.FollowUpMyPatientActivity
import org.medtroniclabs.uhis.ui.home.adapter.DashboardMenuItemsAdapter
import org.medtroniclabs.uhis.ui.household.HouseholdSearchActivity
import org.medtroniclabs.uhis.ui.landing.viewmodel.LandingViewModel
import org.medtroniclabs.uhis.ui.membersearch.MemberSearchActivity
import org.medtroniclabs.uhis.ui.patient.AdvancedSearchActivity
import org.medtroniclabs.uhis.ui.patient.NurseDashboardActivity
import org.medtroniclabs.uhis.ui.patient.UIConstants
import org.medtroniclabs.uhis.ui.peersupervisor.PerformanceMonitoringActivity
import org.medtroniclabs.uhis.ui.services.ServicesActivity
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class HomeScreenFragment : BaseFragment(), MenuSelectionListener {
    private lateinit var binding: FragmentHomeScreenBinding

    private val viewModel: LandingViewModel by activityViewModels()

    @Inject
    lateinit var followUpDao: FollowUpDao

    private val chwId: String
        get() = runCatching { SecuredPreference.getUserId().toString() }.getOrDefault("")

    companion object {
        const val TAG = "HomeScreenFragment"

        fun newInstance(): HomeScreenFragment = HomeScreenFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentHomeScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        attachObservers()
        viewModel.getMenus()
        setupCoachingSurfaces()
    }

    /**
     * Wire up the MicroCoaching SDK surfaces on the home screen: feed the SDK stores
     * that back the Coaching grid tile (badge + Practice Zone refreshers), and mount
     * the CHW AI chat FAB at bottom-right (opens the chat bottom sheet).
     */
    private fun setupCoachingSurfaces() {
        if (!MicroCoachingSDK.isInitialized()) return
        val sdk = MicroCoachingSDK.getInstance()

        sdk.onHomeScreenShown(chwId)
        pushTodaysVisits(sdk)

        // The Coaching grid tile + its skipped-refresher badge are rendered by the
        // SDK's CoachingGridTile (see DashboardMenuItemsAdapter) — no host wiring needed.

        // ── Chat FAB (bottom-right) ────────────────────────────────────────
        binding.chatFab.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MicroCoachingTheme {
                    ChatFab(
                        onClick = { launchCoachingChatSheet() },
                    )
                }
            }
        }
    }

    /**
     * Push the CHW's patient visits due today into the coaching SDK so it can surface
     * visit-relevant refreshers at cold-start. Only the clinical-type signal is sent —
     * no patient identifiers (see [org.medtroniclabs.uhis.microcoaching.TodaysVisitRow]).
     * Best-effort, off the main thread; failures are non-fatal.
     */
    private fun pushTodaysVisits(sdk: MicroCoachingSDK) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                val today = LocalDate.now().toString() // yyyy-MM-dd, device-local
                val visits = followUpDao.getVisitsDueOn(today).map { it.toTodaysVisit() }
                sdk.onTodaysVisitsUpdated(visits)
                Log.d(TAG, "MicroCoaching: pushed ${visits.size} visit(s) due today")
            }.onFailure { Log.w(TAG, "MicroCoaching: failed to push today's visits", it) }
        }
    }

    /**
     * Open [CoachingChatBottomSheet]. All download UX (AI model, voice/TTS packs)
     * now lives on the SDK's coaching setup screen inside the sheet, so the host
     * no longer prompts to download the model — it just opens the sheet.
     */
    private fun launchCoachingChatSheet() {
        if (!MicroCoachingSDK.isInitialized()) return
        CoachingChatBottomSheet.show(parentFragmentManager)
    }

    override fun onResume() {
        super.onResume()
        viewModel.setUserJourney(getString(R.string.home))
        isDeeplink(arguments?.getBoolean(DefinedParams.IS_DEEP_LINK, false))
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

    private fun setAdapterViews(menuEntity: List<MenuEntity>) {
        if (CommonUtils.checkIsTablet(requireContext())) {
            val layoutManager = FlexboxLayoutManager(context)
            layoutManager.flexDirection = FlexDirection.ROW
            layoutManager.justifyContent = JustifyContent.CENTER
            binding.rvActivitiesList.layoutManager = layoutManager
        } else {
            val layoutManager = GridLayoutManager(context, 2)
            binding.rvActivitiesList.layoutManager = layoutManager
        }
        val items = if (MicroCoachingSDK.isInitialized() &&
            menuEntity.none { it.menuId.equals(MenuConstants.COACHING_MENU_ID, ignoreCase = true) }
        ) {
            val isBangla = MicroCoachingSDK.getInstance().config.language == Language.BANGLA
            menuEntity + MenuEntity(
                id = -1L,
                menuId = MenuConstants.COACHING_MENU_ID,
                name = "Coaching",
                displayValue = if (isBangla) "কোচিং" else null,
                displayOrder = menuEntity.size,
            )
        } else {
            menuEntity
        }
        binding.rvActivitiesList.adapter = DashboardMenuItemsAdapter(items, this)
    }

    override fun onMenuSelected(
        menuId: String,
        subModule: String?,
    ) {
        when (menuId) {
            MenuConstants.HOUSEHOLD_MENU_ID -> {
                startActivity(Intent(requireContext(), HouseholdSearchActivity::class.java))
            }

            MenuConstants.REGISTRATION, MenuConstants.CONFIRM_DIAGNOSIS -> {
                withNetworkAvailability(online = {
                    val bundle = Bundle().apply {
                        putString(DefinedParams.ORIGIN, MenuConstants.ENROLLMENT.lowercase())
                    }
                    val intent = Intent(requireContext(), AdvancedSearchActivity::class.java)
                    intent.putExtras(bundle)
                    startActivity(intent)
                })
            }

            MenuConstants.DISPENSE -> {
                val bundle = Bundle().apply {
                    putString(DefinedParams.ORIGIN, MenuConstants.DISPENSE.lowercase())
                }
                val intent = Intent(requireContext(), AdvancedSearchActivity::class.java)
                intent.putExtras(bundle)
                startActivity(intent)
            }

            MenuConstants.MY_PATIENTS_MENU_ID -> {
                if (CommonUtils.isFoOrPo() || CommonUtils.isCHCP()) {
                    startActivity(Intent(requireContext(), MemberSearchActivity::class.java))
                } else if (CommonUtils.isNurse()) {
                    val bundle = Bundle().apply {
                        putString(DefinedParams.ORIGIN, MenuConstants.MY_PATIENTS_MENU_ID.lowercase())
                    }
                    val intent = Intent(requireContext(), AdvancedSearchActivity::class.java)
                    intent.putExtras(bundle)
                    withNetworkAvailability(online = {
                        startActivity(intent)
                    })
                } else {
                    val bundle = Bundle().apply {
                        putString(DefinedParams.ORIGIN, MenuConstants.MY_PATIENTS_MENU_ID)
                    }
                    val intent = if (CommonUtils.isCommunity()) {
                        Intent(
                            requireContext(),
                            FollowUpMyPatientActivity::class.java,
                        )
                    } else {
                        Intent(requireContext(), PatientSearchActivity::class.java)
                    }

                    intent.putExtras(bundle)
                    if (CommonUtils.isCommunity()) {
                        startActivity(intent)
                    } else {
                        withNetworkAvailability(online = {
                            startActivity(intent)
                        })
                    }
                }
            }

            MenuConstants.COMMUNITY_PROFILE -> {
                startActivity(Intent(requireContext(), CommunityProfileActivity::class.java))
            }

            MenuConstants.PERFORMANCE_MONITORING_ID -> {
                if (connectivityManager.isNetworkAvailable()) {
                    val intent = Intent(requireContext(), PerformanceMonitoringActivity::class.java)
                    startActivity(intent)
                } else {
                    (activity as BaseActivity?)?.showErrorDialogue(
                        getString(R.string.title_no_network),
                        getString(R.string.message_no_network),
                        isNegativeButtonNeed = false,
                    ) { _ -> }
                }
            }

            // NCD WorkFlow
            MenuConstants.SCREENING -> {
                startActivity(Intent(requireContext(), ScreeningActivity::class.java))
            }

            MenuConstants.ASSESSMENT -> {
                val bundle = Bundle().apply {
                    putString(DefinedParams.ORIGIN, MenuConstants.ASSESSMENT.lowercase())
                }
                val intent = Intent(requireContext(), PatientSearchActivity::class.java)
                intent.putExtras(bundle)
                startActivity(intent)
            }

            MenuConstants.DASHBOARD -> {
                if (CommonUtils.isNURSE()) {
                    if (connectivityManager.isNetworkAvailable()) {
                        val intent = Intent(requireContext(), NurseDashboardActivity::class.java)
                        startActivity(intent)
                    } else {
                        (activity as BaseActivity?)?.showErrorDialogue(
                            getString(R.string.title_no_network),
                            getString(R.string.message_no_network),
                            isNegativeButtonNeed = false,
                        ) { _ -> }
                    }
                } else {
                    val intent = Intent(requireContext(), NCDDashboardViewActivity::class.java)
                    startActivity(intent)
                }
            }

            MenuConstants.LIFESTYLE -> {
                val bundle = Bundle().apply {
                    putString(DefinedParams.ORIGIN, MenuConstants.LIFESTYLE.lowercase())
                }
                val intent = Intent(requireContext(), PatientSearchActivity::class.java)
                intent.putExtras(bundle)
                startActivity(intent)
            }

            MenuConstants.PSYCHOLOGICAL -> {
                val bundle = Bundle().apply {
                    putString(DefinedParams.ORIGIN, MenuConstants.PSYCHOLOGICAL.lowercase())
                }
                val intent = Intent(requireContext(), PatientSearchActivity::class.java)
                intent.putExtras(bundle)
                startActivity(intent)
            }

            MenuConstants.INVESTIGATION -> {
                val bundle = Bundle().apply {
                    putString(DefinedParams.ORIGIN, MenuConstants.INVESTIGATION.lowercase())
                }
                val intent = Intent(requireContext(), PatientSearchActivity::class.java)
                intent.putExtras(bundle)
                startActivity(intent)
            }
            MenuConstants.FOLLOW_UP -> {
                if ((CommonUtils.isChp())) {
                    val intent = Intent(requireContext(), FollowUpMyPatientActivity::class.java)
                    startActivity(intent)
                } else {
                    val intent = Intent(requireContext(), NCDFollowUpActivity::class.java)
                    startActivity(intent)
                }
            }

            MenuConstants.SERVICE_RECIPIENT -> {
                startActivity(Intent(requireContext(), ServicesActivity::class.java))
            }

            MenuConstants.TELE_SUPPORT -> {
                withNetworkAvailability(online = {
                    val bundle = Bundle().apply {
                        putString(DefinedParams.ORIGIN, UIConstants.FOLLOW_UP)
                    }
                    val intent = Intent(requireContext(), AdvancedSearchActivity::class.java)
                    intent.putExtras(bundle)
                    startActivity(intent)
                })
            }

            MenuConstants.COACHING_MENU_ID -> {
                if (MicroCoachingSDK.isInitialized()) {
                    CoachingFlowActivity.launchLearn(requireContext(), chwId)
                }
            }
        }
    }

    // Deeplink  redirecting to Search Patient
    private fun isDeeplink(isDeepLink: Boolean?) {
        if (isDeepLink == true) {
            val bundle = Bundle().apply {
                putString(DefinedParams.ORIGIN, MenuConstants.MY_PATIENTS_MENU_ID)
            }
            val intent = if (CommonUtils.isCommunity()) {
                Intent(
                    requireContext(),
                    FollowUpMyPatientActivity::class.java,
                )
            } else {
                Intent(requireContext(), PatientSearchActivity::class.java)
            }

            intent.putExtras(bundle)
            if (CommonUtils.isCommunity()) {
                startActivity(intent)
            } else {
                startActivity(intent)
            }
        }
    }
}
