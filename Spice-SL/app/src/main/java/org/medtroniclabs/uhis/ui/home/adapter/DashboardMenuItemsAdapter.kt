package org.medtroniclabs.uhis.ui.home.adapter

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.medtroniclabs.microcoaching.ui.components.CoachingGridTile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.databinding.RowActivitiesBinding
import org.medtroniclabs.uhis.databinding.RowCoachingTileBinding
import org.medtroniclabs.uhis.db.entity.MenuEntity
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.ui.MenuConstants
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH
import org.medtroniclabs.uhis.ui.home.MenuSelectionListener

class DashboardMenuItemsAdapter(
    private val roleBasedActivitiesList: List<MenuEntity>,
    private val listener: MenuSelectionListener,
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    /** Default menu tile (icon + title), inflated from `row_activities`. */
    class ActivitiesViewHolder(val binding: RowActivitiesBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val context: Context = binding.root.context
    }

    /**
     * Coaching tile — the whole tile (and its skipped-refresher badge) is owned
     * by the MicroCoaching SDK's [CoachingGridTile] Composable, so the host just
     * forwards the click.
     */
    class CoachingViewHolder(val binding: RowCoachingTileBinding) :
        RecyclerView.ViewHolder(binding.root)

    /**
     * Parent composition context for the coaching tile's [ComposeView].
     *
     * On tablets this RecyclerView uses FlexboxLayoutManager, which measures item
     * views inside `calculateFlexLines` BEFORE attaching them to the window.
     * `AbstractComposeView.onMeasure` unconditionally creates its composition, and
     * with no explicit parent context it resolves one from the window recomposer —
     * which doesn't exist while the view is detached, so it throws
     * "Cannot locate windowRecomposer ... not attached to a window" (crash seen on
     * tablets; phones use GridLayoutManager, which attaches before measuring, so
     * they never hit it).
     *
     * Supplying an explicit, lifecycle-scoped [Recomposer] via
     * `setParentCompositionContext` short-circuits that window lookup, so the tile
     * composes safely even when measured detached. Created when the adapter is
     * attached to the RecyclerView and cancelled when it detaches.
     */
    private var recomposer: Recomposer? = null
    private var recomposeScope: CoroutineScope? = null

    override fun getItemViewType(position: Int): Int =
        if (roleBasedActivitiesList[position]
                .menuId
                .equals(MenuConstants.COACHING_MENU_ID, ignoreCase = true)
        ) {
            VIEW_TYPE_COACHING
        } else {
            VIEW_TYPE_DEFAULT
        }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        // AndroidUiDispatcher.CurrentThread supplies a Choreographer-backed frame
        // clock; the Recomposer drives the coaching tile's composition off it.
        // Created on the main thread (RecyclerView callbacks are main-thread).
        val dispatcher = AndroidUiDispatcher.CurrentThread
        val scope = CoroutineScope(dispatcher)
        val newRecomposer = Recomposer(dispatcher)
        recomposeScope = scope
        recomposer = newRecomposer
        scope.launch { newRecomposer.runRecomposeAndApplyChanges() }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        recomposer?.cancel()
        recomposer = null
        recomposeScope = null
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_COACHING) {
            CoachingViewHolder(RowCoachingTileBinding.inflate(inflater, parent, false)).also {
                it.binding.coachingTileComposeView.apply {
                    // Explicit parent context — see [recomposer]. Must be set before
                    // the first onMeasure (which is why it's here, not at bind).
                    setParentCompositionContext(recomposer)
                    setViewCompositionStrategy(
                        ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool,
                    )
                }
            }
        } else {
            ActivitiesViewHolder(RowActivitiesBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        val model = roleBasedActivitiesList[position]
        when (holder) {
            is CoachingViewHolder -> holder.binding.coachingTileComposeView.setContent {
                CoachingGridTile(
                    onClick = { listener.onMenuSelected(model.menuId, model.subModule) },
                )
            }
            is ActivitiesViewHolder -> bindActivityTile(holder, model)
        }
    }

    private fun bindActivityTile(
        holder: ActivitiesViewHolder,
        model: MenuEntity,
    ) {
        holder.binding.tvTitle.text = if (CommonUtils.parseUserLocale() == DefinedParams.EN) {
            model.name
        } else {
            model.displayValue ?: model.name
        }

        val imageModel = getResourceActivityId(
            model.menuId,
            holder.context,
            model.subModule,
        )
        val backgroundColorResId = if (model.isDisabled) R.color.border_gray else R.color.white
        holder.binding.root.apply {
            isEnabled = !model.isDisabled
            setCardBackgroundColor(ContextCompat.getColor(holder.context, backgroundColorResId))
        }
        if (imageModel == null) {
            holder.binding.ivActivity.visibility = View.INVISIBLE
        } else {
            holder.binding.ivActivity.visibility = View.VISIBLE
            holder.binding.ivActivity.setImageDrawable(imageModel)
        }

        holder.binding.root.safeClickListener {
            if (model.menuId == MenuConstants.ICCM_MENU_ID &&
                model.name.equals(
                    MenuConstants.OTHER_SYMPTOMS,
                    true,
                )
            ) {
                listener.onMenuSelected(model.name, model.subModule)
            } else {
                listener.onMenuSelected(model.menuId, model.subModule)
            }
        }
    }

    private fun getResourceActivityId(
        menuID: String,
        context: Context,
        subModule: String?,
    ): Drawable? =
        when (menuID) {
            MenuConstants.CBS_MENU_ID.lowercase() -> ContextCompat.getDrawable(
                context,
                R.drawable.ic_cbs,
            )

            MenuConstants.CBS_MENU_ID.uppercase() -> ContextCompat.getDrawable(
                context,
                R.drawable.ic_cbs,
            )

            MenuConstants.HOUSEHOLD_MENU_ID -> ContextCompat.getDrawable(
                context,
                R.drawable.ic_household,
            )

            MenuConstants.MY_PATIENTS_MENU_ID, MenuConstants.SERVICE_RECIPIENT -> ContextCompat.getDrawable(
                context,
                R.drawable.ic_my_patient,
            )

            MenuConstants.SCREENER_MENU_ID -> ContextCompat.getDrawable(
                context,
                R.drawable.ic_general_screener_tool,
            )

            MenuConstants.ICCM_MENU_ID -> {
                ContextCompat.getDrawable(
                    context,
                    R
                        .drawable.ic_general,
                )
            }

            MenuConstants.TB_MENU_ID.lowercase() -> ContextCompat.getDrawable(
                context,
                R.drawable.ic_tb_tool,
            )

            MenuConstants.TB_MENU_ID -> ContextCompat.getDrawable(
                context,
                R.drawable.ic_tb_tool,
            )
            MenuConstants.NCD_MENU_ID -> ContextCompat.getDrawable(
                context,
                R.drawable.ic_ncd_tool,
            )

            MenuConstants.EYE_CARE_MENU_ID -> ContextCompat.getDrawable(
                context,
                R.drawable.ic_eye_care,
            )

            MenuConstants.CATARACT_MENU_ID -> ContextCompat.getDrawable(
                context,
                R.drawable.ic_cataract,
            )

            MenuConstants.PREGNANCY_OUTCOME -> ContextCompat.getDrawable(
                context,
                R.drawable.ic_po_pnc,
            )

            MenuConstants.MATERNAL_HEALTH, MenuConstants.PREGNANT_WOMEN_PROFILE ->
                ContextCompat
                    .getDrawable(
                        context,
                        R.drawable.ic_rmnch_tool,
                    )

            MenuConstants.RMNCH_MENU_ID -> {
                var drawableRes = -1
                when (subModule) {
                    RMNCH.ANC -> {
                        drawableRes = R.drawable.ic_rmnch_tool
                    }

                    RMNCH.PNC -> {
                        drawableRes = R.drawable.ic_po_pnc
                    }

                    RMNCH.ChildHoodVisit -> {
                        drawableRes = R.drawable.ic_child_health
                    }
                }
                if (drawableRes == -1) {
                    ContextCompat
                        .getDrawable(
                            context,
                            R.drawable.ic_rmnch_tool,
                        )
                } else {
                    ContextCompat
                        .getDrawable(
                            context,
                            drawableRes,
                        )
                }
            }

            MenuConstants.OTHER_SYMPTOMS -> ContextCompat.getDrawable(
                context,
                R.drawable.ic_general,
            )

            MenuConstants.GENERAL_ID -> ContextCompat.getDrawable(
                context,
                R.drawable.ic_general,
            )

            MenuConstants.MOTHER_AND_NEONATE_ID -> ContextCompat.getDrawable(
                context,
                R.drawable.ic_mother_neonate,
            )

            MenuConstants.UNDER_AGE_FIVE_TO_TWO_MONTHS_ID -> ContextCompat.getDrawable(
                context,
                R.drawable.ic_child_under_5,
            )

            MenuConstants.UNDER_AGE_ABOVE_FIVE_YEAR_ID -> ContextCompat.getDrawable(
                context,
                R.drawable.ic_child_above_5,
            )

            MenuConstants.EPI_ID -> ContextCompat.getDrawable(
                context,
                R.drawable.ic_epi,
            )
            MenuConstants.PERFORMANCE_MONITORING_ID -> ContextCompat.getDrawable(
                context,
                R.drawable.ic_performance_monitoring,
            )

            MenuConstants.SCREENING -> ContextCompat.getDrawable(
                context,
                R.drawable.ic_screening,
            )

            MenuConstants.REGISTRATION -> ContextCompat.getDrawable(
                context,
                R.drawable.ic_registration,
            )

            MenuConstants.ASSESSMENT -> ContextCompat.getDrawable(
                context,
                R.drawable.ic_assessment,
            )

            MenuConstants.DASHBOARD -> ContextCompat.getDrawable(
                context,
                R.drawable.ic_dashboard,
            )

            MenuConstants.LIFESTYLE -> ContextCompat.getDrawable(
                context,
                R.drawable.ic_lifestyle,
            )

            MenuConstants.PSYCHOLOGICAL -> ContextCompat.getDrawable(
                context,
                R.drawable.ic_psycological_menu,
            )

            MenuConstants.MENTAL_HEALTH -> ContextCompat.getDrawable(
                context,
                R.drawable.ic_mental_health,
            )

            MenuConstants.DISPENSE -> ContextCompat.getDrawable(
                context,
                R.drawable.ic_dispense,
            )

            MenuConstants.FOLLOW_UP -> ContextCompat.getDrawable(
                context,
                R.drawable.ic_follow_up,
            )

            MenuConstants.INVESTIGATION -> ContextCompat.getDrawable(
                context,
                R.drawable.ic_investigation,
            )

            MenuConstants.COMMUNITY_PROFILE -> ContextCompat.getDrawable(
                context,
                R.drawable.ic_community_profile,
            )

            MenuConstants.FP_MENU_ID -> ContextCompat.getDrawable(
                context,
                R.drawable.ic_family_planning,
            )

            MenuConstants.FP_MENU_MR -> ContextCompat.getDrawable(
                context,
                R.drawable.ic_family_planning,
            )

            MenuConstants.HIV -> ContextCompat.getDrawable(
                context,
                R.drawable.ic_hiv,
            )

            // COACHING_MENU_ID is rendered by the SDK CoachingGridTile (own
            // view type) — no host drawable needed.

            else -> null
        }

    override fun getItemCount(): Int = roleBasedActivitiesList.size

    companion object {
        private const val VIEW_TYPE_DEFAULT = 0
        private const val VIEW_TYPE_COACHING = 1
    }
}
