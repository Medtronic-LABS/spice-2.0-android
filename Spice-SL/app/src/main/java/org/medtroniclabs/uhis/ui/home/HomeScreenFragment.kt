package org.medtroniclabs.uhis.ui.home

import android.content.Intent
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.medtroniclabs.microcoaching.Language
import com.medtroniclabs.microcoaching.MicroCoachingSDK
import com.medtroniclabs.microcoaching.ui.chat.CoachingChatBottomSheet
import com.medtroniclabs.microcoaching.ui.components.ChatFab
import com.medtroniclabs.microcoaching.ui.components.MorningCard
import com.medtroniclabs.microcoaching.ui.flow.CoachingFlowActivity
import com.medtroniclabs.microcoaching.ui.learn.modules.QuickLearnViewModel
import com.medtroniclabs.microcoaching.ui.learn.modules.bottomsheet.RefresherBottomSheet
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
import org.medtroniclabs.uhis.ui.peersupervisor.PerformanceMonitoringActivity
import org.medtroniclabs.uhis.ui.services.ServicesActivity
import java.time.LocalDate
import javax.inject.Inject
import android.net.ConnectivityManager as AndroidConnectivityManager

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

        /**
         * How long the pull-to-refresh spinner lingers after [MicroCoachingSDK.refreshRefreshers]
         * (which is fire-and-forget; the MorningCard updates reactively). Purely cosmetic feedback.
         */
        private const val COACHING_REFRESH_SPINNER_MS = 1200L

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
     * Wire up the MicroCoaching SDK surfaces on the home screen:
     *   1. MorningCard banner pinned above the menu grid — shows the gap-prioritised
     *      morning module with Start / Skip actions.
     *   2. CHW AI chat FAB at bottom-right (opens chat in a bottom sheet).
     *
     * The banner collapses to zero height when `morningModules` is empty.
     * Tapping Start opens the v3 Learn flow; tapping Skip dismisses the card
     * for the current session.
     */
    private fun setupCoachingSurfaces() {
        if (!MicroCoachingSDK.isInitialized()) return
        val sdk = MicroCoachingSDK.getInstance()

        sdk.onHomeScreenShown(chwId)
        pushTodaysVisits(sdk)

        // Pull-to-refresh on the home coaching surface → re-fetch the morning
        // refreshers (backend re-runs its gap algorithm + on-device re-evaluation).
        // refreshRefreshers() is fire-and-forget on the SDK scope and the MorningCard
        // updates reactively, so stop the spinner after a short delay for feedback.
        // Capture the view (not `binding`) so the delayed stop is safe across teardown.
        binding.coachingSwipeRefresh.setOnRefreshListener {
            sdk.refreshRefreshers()
            val swipeRefresh = binding.coachingSwipeRefresh
            swipeRefresh.postDelayed({ swipeRefresh.isRefreshing = false }, COACHING_REFRESH_SPINNER_MS)
        }
        // The Coaching grid tile + its skipped-refresher badge are rendered by the
        // SDK's CoachingGridTile (see DashboardMenuItemsAdapter) — no host badge
        // wiring needed; the tile observes the count internally.

        // ── MorningCard banner (above grid) ───────────────────────────────
        // Uses MorningCard (W6) for ALL refreshers — including quiz-only modules
        // (cardCount == 0), where its eyebrow renders "Quiz". Start always launches
        // the cards-first flow; RefresherContent skips straight to the quiz when the
        // module ships no lesson cards, so a card-less module is handled gracefully.
        binding.coachingCardBanner.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MicroCoachingTheme {
                    // The featured refresher comes from the shared store (the SAME
                    // pick the modules screen shows). Collected as state so the card
                    // advances live when the CHW skips or finishes a refresher, and
                    // hides only when every refresher is skipped (top == null).
                    val top by sdk.selectedMorningModule.collectAsState()

                    // QuickLearnViewModel provides the wrong-question count for the label.
                    val morningVm: QuickLearnViewModel = viewModel(
                        factory = QuickLearnViewModel.factory(
                            androidx.compose.ui.platform.LocalContext.current.applicationContext,
                            chwId,
                        ),
                    )
                    val wrongCount by morningVm.wrongQuestionCount.collectAsState()

                    // Recompute the label whenever the featured module changes.
                    LaunchedEffect(top?.moduleId) {
                        morningVm.computeWrongQuestionCount()
                    }

                    val current = top
                    if (current != null) {
                        val title = if (sdk.config.language == Language.ENGLISH) {
                            current.titleEn ?: current.titleBn
                        } else {
                            current.titleBn
                        }
                        // Effective question count: wrong answers if any; total otherwise.
                        val effectiveQuestionCount = if (wrongCount > 0) wrongCount else current.questionCount

                        val onSkip: () -> Unit = {
                            // Skip = advance: mark this refresher skipped → the store
                            // promotes the next pending refresher into
                            // selectedMorningModule (the card re-renders with it), or
                            // hides when none remain. Also feeds the Coaching tile badge.
                            sdk.markRefresherSkipped(current.moduleFamilyId)
                        }
                        val onStart: () -> Unit = {
                            RefresherBottomSheet.show(
                                parentFragmentManager,
                                chwId,
                                fromHomeScreen = true,
                                // Cards-first: lesson cards → quiz. The home card
                                // never chains ("Next refresher" is suppressed by
                                // fromHomeScreen), so it ends on the quiz → Done.
                                entryMode = RefresherBottomSheet.EntryMode.CARDS_FIRST,
                                // Drill the SAME featured module the card shows.
                                targetModuleFamilyId = current.moduleFamilyId,
                            )
                        }

                        MorningCard(
                            moduleTitle = title,
                            cardCount = current.cardCount,
                            questionCount = effectiveQuestionCount,
                            estimatedMinutes = current.estimatedMinutes,
                            onStart = onStart,
                            onSkip = onSkip,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }

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
     * Push the CHW's patient visits due today into the coaching SDK so it can
     * surface visit-relevant refreshers at cold-start (no behavioural gaps and no
     * backend morning cards). Only the clinical-type signal is sent — no patient
     * identifiers (see [org.medtroniclabs.uhis.microcoaching.TodaysVisitRow]).
     * Best-effort and off the main thread; failures are non-fatal. Applies from the
     * next on-device morning recompute.
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
     * Open [CoachingChatBottomSheet] if the on-device LLM model is staged.
     * Otherwise, prompt the CHW to download it (~600 MB) — same dialog flow as
     * Phase 1.2's drawer entry, surfaced from a different entry point.
     */
    private fun launchCoachingChatSheet() {
        if (!MicroCoachingSDK.isInitialized()) return
        val sdk = MicroCoachingSDK.getInstance()
        // Low-end devices (< 3 GB RAM) run the chat in retrieval-only mode —
        // no AI model is required, so skip the download prompt entirely.
        if (sdk.isLowEndDevice || sdk.modelManager.isModelPresent()) {
            CoachingChatBottomSheet.show(parentFragmentManager)
        } else {
            showCoachingModelDownloadPrompt()
        }
    }

    /**
     * Single-dialog model-download confirmation. The previous two-dialog flow
     * (general prompt → metered warning → trigger) was the failure surface in
     * a QA report — the second dialog's positive callback was being dropped on
     * some devices, leaving the user on the home screen with no feedback. The
     * metered-network hint is now baked into the message string so the user
     * gives a single explicit yes.
     */
    private fun showCoachingModelDownloadPrompt() {
        val activity = (activity as? BaseActivity) ?: return
        val metered = isOnMeteredNetwork()
        val messageRes = if (metered) {
            R.string.coaching_model_download_message_metered
        } else {
            R.string.coaching_model_download_message
        }
        // Dynamic download size from the SDK's configured model variant, formatted
        // locale-aware — tracks the selected model instead of a hard-coded "~600 MB".
        val sizeLabel = runCatching {
            android.text.format.Formatter.formatShortFileSize(
                requireContext(),
                MicroCoachingSDK.getInstance().selectedModelVariant().sizeInBytes,
            )
        }.getOrDefault("")
        activity.showErrorDialogue(
            title = getString(R.string.coaching_model_download_title),
            message = getString(messageRes, sizeLabel),
            isNegativeButtonNeed = true,
            positiveButtonName = getString(R.string.yes),
            cancelBtnName = getString(R.string.no),
        ) { isPositive ->
            Log.i(TAG, "ModelDownloadPrompt dismissed — positive=$isPositive metered=$metered")
            if (isPositive) triggerCoachingModelDownload()
        }
    }

    private fun triggerCoachingModelDownload() {
        Log.i(TAG, "triggerCoachingModelDownload — calling modelManager.triggerDownload()")
        runCatching { MicroCoachingSDK.getInstance().modelManager.triggerDownload() }
            .onFailure { Log.e(TAG, "modelManager.triggerDownload threw", it) }
        // Always-on user feedback — even if the chat sheet fails to open
        // (rare illegal-state edge case), the toast confirms the trigger ran.
        Toast
            .makeText(
                requireContext(),
                getString(R.string.coaching_download_started),
                Toast.LENGTH_LONG,
            ).show()
        // Open the chat sheet immediately so the CHW lands on a screen that
        // shows live download progress — see ChatViewModel.currentModelNotReadyState
        // for the race-fix that keeps the freshly-opened sheet in sync with the
        // in-flight ModelState.
        runCatching { CoachingChatBottomSheet.show(parentFragmentManager) }
            .onFailure { Log.e(TAG, "CoachingChatBottomSheet.show threw", it) }
    }

    /**
     * Default to `true` (assume metered) when the connectivity manager or the
     * active network is null — a transient null read shouldn't bypass the
     * user's consent step on the rare race where we check right at network
     * handoff.
     */
    private fun isOnMeteredNetwork(): Boolean {
        val cm = requireContext().getSystemService(AndroidConnectivityManager::class.java) ?: return true
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return true
        return !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    override fun onResume() {
        super.onResume()
        viewModel.setUserJourney(getString(R.string.home))
        isDeeplink(arguments?.getBoolean(DefinedParams.IsDeepLink, false))
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

            MenuConstants.MY_PATIENTS_MENU_ID -> {
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

            MenuConstants.REGISTRATION -> {
                withNetworkAvailability(online = {
                    val bundle = Bundle().apply {
                        putString(DefinedParams.ORIGIN, MenuConstants.REGISTRATION.lowercase())
                    }
                    val intent = Intent(requireContext(), PatientSearchActivity::class.java)
                    intent.putExtras(bundle)
                    startActivity(intent)
                })
            }

            MenuConstants.ASSESSMENT -> {
                val bundle = Bundle().apply {
                    putString(DefinedParams.ORIGIN, MenuConstants.ASSESSMENT.lowercase())
                }
                val intent = Intent(requireContext(), PatientSearchActivity::class.java)
                intent.putExtras(bundle)
                startActivity(intent)
            }

            MenuConstants.DISPENSE -> {
                val bundle = Bundle().apply {
                    putString(DefinedParams.ORIGIN, MenuConstants.DISPENSE.lowercase())
                }
                val intent = Intent(requireContext(), PatientSearchActivity::class.java)
                intent.putExtras(bundle)
                startActivity(intent)
            }

            MenuConstants.DASHBOARD -> {
                val intent = Intent(requireContext(), NCDDashboardViewActivity::class.java)
                startActivity(intent)
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
