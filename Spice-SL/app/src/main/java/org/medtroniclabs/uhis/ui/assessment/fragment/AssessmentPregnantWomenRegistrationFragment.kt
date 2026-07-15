package org.medtroniclabs.uhis.ui.assessment.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.children
import androidx.fragment.app.activityViewModels
import org.medtroniclabs.uhis.app.analytics.model.UserDetail
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsUtils
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DateUtils.formatGestationalAge
import org.medtroniclabs.uhis.common.EntityMapper
import org.medtroniclabs.uhis.data.model.RecommendedDosageListModel
import org.medtroniclabs.uhis.databinding.FragmentAssessmentBinding
import org.medtroniclabs.uhis.formgeneration.FormGenerator
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.extension.textSizeSsp
import org.medtroniclabs.uhis.formgeneration.listener.FormEventListener
import org.medtroniclabs.uhis.formgeneration.model.FormLayout
import org.medtroniclabs.uhis.formgeneration.ui.FormResultComposer
import org.medtroniclabs.uhis.formgeneration.utility.CheckBoxDialog
import org.medtroniclabs.uhis.mappingkey.PregnantWomen
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.MenuConstants
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams
import org.medtroniclabs.uhis.ui.assessment.viewmodel.AssessmentViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Fragment responsible for registering pregnant women details
 *
 * Ticket : https://mdtlabs.atlassian.net/browse/UHIS-118
 * UI : https://claude.ai/public/artifacts/b504e171-ab92-4553-8cb4-2ba36bb3a9ed
 */
class AssessmentPregnantWomenRegistrationFragment :
    BaseFragment(),
    FormEventListener,
    View.OnClickListener {
    private lateinit var binding: FragmentAssessmentBinding

    private val viewModel: AssessmentViewModel by activityViewModels()

    private lateinit var formGenerator: FormGenerator

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentAssessmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        attachObservers()
        setListener()
        UserDetail.startDateTime = AnalyticsUtils.getCurrentDateTimeInLocalTime()
        viewModel.setUserJourney(AnalyticsDefinedParams.PREGNANT_WOMEN_PROFILE)
    }

    private fun attachObservers() {
        viewModel.formLayoutsLiveData.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.SUCCESS -> {
                    hideProgress()
                    resourceState.data?.let { data ->
                        formGenerator.populateViews(data.formLayout)
                    }
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }

                ResourceState.LOADING -> {
                    showProgress()
                }
            }
        }
    }

    private fun setListener() {
        binding.btnSubmit.safeClickListener(this)
    }

    fun initView() {
        replaceFragmentInId<BioDataFragment>(
            binding.bioDataFragmentContainer.id,
            tag = BioDataFragment.TAG,
        )
        childFragmentManager.executePendingTransactions() // Ensures transaction is complete
        formGenerator = FormGenerator(
            requireContext(),
            binding.llForm,
            this,
            binding.scrollView,
            translate = isTranslationEnabled,
            callback = { resultHashMap, id ->
                when (id.lowercase(Locale.ENGLISH)) {
                    PregnantWomen.ID_LMP -> {
                        val lastMenstrualDateString =
                            resultHashMap[id] as? String ?: return@FormGenerator
                        val lastMenstrualDate =
                            DateUtils.getLastMenstrualDate(lastMenstrualDateString)
                        val daysDifference =
                            DateUtils.getDaysDifference(lastMenstrualDate.timeInMillis) ?: 0
                        if (daysDifference < PregnantWomen.LMP_THRESHOLD_DAYS) {
                            viewModel.isPregnancyTooEarlyToAccess = true
                            // Hide health risk & reset
                            formGenerator
                                .getViewByTag(
                                    PregnantWomen.ID_HEALTH_RISK_SCREENING + formGenerator.rootSuffix,
                                )?.gone()
                            (
                                formGenerator.getViewByTag(
                                    PregnantWomen.ID_HEALTH_RISK_SCREENING,
                                ) as? ViewGroup
                            )?.run {
                                children.forEach { healthRiskFields ->
                                    formGenerator.resetChildViews(healthRiskFields)
                                    healthRiskFields.gone()
                                }
                            }
                            // Hide rest of the form fields other than lmp and too early messages
                            (
                                formGenerator.getViewByTag(
                                    PregnantWomen.ID_PREGNANCY_DETAILS_AND_HISTORY,
                                ) as? ViewGroup
                            )?.run {
                                children.forEach { pregnancyField ->
                                    if (pregnancyField.tag !in listOf(
                                            PregnantWomen.ID_LMP + formGenerator.rootSuffix,
                                            PregnantWomen.ID_TOO_EARLY_TITLE + formGenerator.rootSuffix,
                                            PregnantWomen.ID_TOO_EARLY_DESC1 + formGenerator.rootSuffix,
                                            PregnantWomen.ID_TOO_EARLY_DESC2 + formGenerator.rootSuffix,
                                        )
                                    ) {
                                        formGenerator.resetChildViews(pregnancyField)
                                        pregnancyField.gone()
                                    }
                                }
                            }
                            // Show too early details
                            formGenerator
                                .getViewByTag(
                                    PregnantWomen.ID_TOO_EARLY_TITLE + formGenerator.rootSuffix,
                                )?.visible()
                            (
                                formGenerator.getViewByTag(
                                    PregnantWomen.ID_TOO_EARLY_TITLE,
                                ) as? TextView
                            )?.textSizeSsp = PregnantWomen.SSP_18
                            formGenerator
                                .getViewByTag(
                                    PregnantWomen.ID_TOO_EARLY_DESC1 + AssessmentDefinedParams.rootSuffix,
                                )?.visible()
                            formGenerator
                                .getViewByTag(
                                    PregnantWomen.ID_TOO_EARLY_DESC2 + AssessmentDefinedParams.rootSuffix,
                                )?.visible()
                        } else {
                            viewModel.isPregnancyTooEarlyToAccess = false
                            // Hide too early messages
                            listOf(
                                PregnantWomen.ID_TOO_EARLY_TITLE + formGenerator.rootSuffix,
                                PregnantWomen.ID_TOO_EARLY_DESC1 + formGenerator.rootSuffix,
                                PregnantWomen.ID_TOO_EARLY_DESC2 + formGenerator.rootSuffix,
                            ).forEach {
                                formGenerator.getViewByTag(it)?.gone()
                            }

                            // EDD
                            updateEDD(lastMenstrualDate)

                            // Gestational Week
                            updateGestationalWeek(lastMenstrualDate)

                            // Gravida
                            formGenerator
                                .getViewByTag(
                                    PregnantWomen.ID_GRAVIDA + formGenerator.rootSuffix,
                                )?.visible()

                            // Show health risk
                            formGenerator.showHideCardFamily(
                                true,
                                PregnantWomen.ID_HEALTH_RISK_SCREENING,
                            )

                            // Show medical conditions
                            formGenerator
                                .getViewByTag(
                                    PregnantWomen.ID_CURRENT_MEDICAL_CONDITIONS + formGenerator.rootSuffix,
                                )?.visible()
                        }
                    }

                    PregnantWomen.ID_GRAVIDA -> {
                        fixObstetricComplications()
                    }

                    PregnantWomen.ID_PARITY -> {
                        fixObstetricComplications()
                    }
                }
            },
        )
        viewModel.getFormData(MenuConstants.PREGNANT_WOMEN_PROFILE)
    }

    /**
     * Calculates gestational week and updates UI
     */
    private fun updateGestationalWeek(lastMenstrualDate: Calendar) {
        val gestationalAge = DateUtils.calculateGestationalAge(lastMenstrualDate)
        val gestationalAgeWeekString = formatGestationalAge(
            gestationalAge,
            requireContext(),
        )
        formGenerator
            .getViewByTag(
                PregnantWomen.ID_GESTATIONAL_WEEK_TITLE + formGenerator.rootSuffix,
            )?.visible()
        formGenerator
            .getViewByTag(
                PregnantWomen.ID_GESTATIONAL_WEEK + formGenerator.rootSuffix,
            )?.visible()
        (
            formGenerator.getViewByTag(
                PregnantWomen.ID_GESTATIONAL_WEEK,
            ) as? TextView
        )?.text = gestationalAgeWeekString
        updatePregnancyTestVisibility(gestationalAge)
    }

    /**
     * Calculates EDD and updates the map and UI
     */
    private fun updateEDD(lastMenstrualDate: Calendar) {
        val estimatedDeliveryDate =
            DateUtils.calculateEstimatedDeliveryDate(lastMenstrualDate)
        val eddFormat = SimpleDateFormat(
            DateUtils.DATE_FORMAT_DD_MMMM_YYYY,
            Locale.getDefault(),
        )
        val formattedEstimatedDeliveryDate = eddFormat.format(estimatedDeliveryDate.time)
        formGenerator
            .getViewByTag(
                PregnantWomen.ID_EDD_TITLE + formGenerator.rootSuffix,
            )?.visible()
        formGenerator
            .getViewByTag(
                PregnantWomen.ID_EDD + formGenerator.rootSuffix,
            )?.visible()
        (
            formGenerator.getViewByTag(
                PregnantWomen.ID_EDD,
            ) as? TextView
        )?.text = formattedEstimatedDeliveryDate
    }

    /**
     * Returns difference between gravida and parity
     */
    private fun getGravidaParityDiff(): Double {
        val resultHashMap = formGenerator.getResultMap()
        val gravida = CommonUtils.getDouble(resultHashMap[PregnantWomen.ID_GRAVIDA])
        val parity = CommonUtils.getDouble(resultHashMap[PregnantWomen.ID_PARITY])
        return gravida - parity
    }

    /**
     * If selected obstetric complications contains complications to ignore
     * when gravida parity difference is <= 1,
     * then it removes those selected ignored complications from the result and updates the UI
     */
    private fun fixObstetricComplications() {
        val id = PregnantWomen.ID_OBSTETRIC_COMPLICATIONS
        val formLayout = formGenerator.getFormLayout(id) ?: return

        @Suppress("UNCHECKED_CAST")
        val resultMap = formGenerator.getResult(id) as? ArrayList<HashMap<String, Any>> ?: return
        val gravidaParityDiff = getGravidaParityDiff()
        if (gravidaParityDiff <= PregnantWomen.GRAVIDA_PARITY_IGNORE_DIFF) {
            resultMap.removeIf {
                PregnantWomen.OBSTETRIC_COMPLICATIONS_TO_IGNORE.contains(it[DefinedParams.VALUE])
            }
        }
        formGenerator.validateCheckboxDialogue(id, formLayout, resultMap, false)
    }

    override fun loadLocalCache(
        id: String,
        localDataCache: Any,
        selectedParent: Long?,
    ) {
    }

    override fun onPopulate(targetId: String) {
    }

    override fun onCheckBoxDialogueClicked(
        id: String,
        formLayout: FormLayout,
        resultMap: Any?,
    ) {
        val title = if (isTranslationEnabled && !formLayout.hintCulture.isNullOrBlank()) {
            formLayout.hintCulture
        } else {
            formLayout.hint
        }

        val inputData = EntityMapper.mapToSignsAndSymptomsEntity(formLayout.optionsList)
        if (id == PregnantWomen.ID_OBSTETRIC_COMPLICATIONS) {
            val gravidaParityDiff = getGravidaParityDiff()
            if (gravidaParityDiff < PregnantWomen.GRAVIDA_PARITY_IGNORE_DIFF) {
                inputData.removeIf { PregnantWomen.OBSTETRIC_COMPLICATIONS_TO_IGNORE.contains(it.value) }
            }
        }
        CheckBoxDialog
            .newInstance(
                id,
                resultMap,
                title = title,
                inputData = inputData,
            ) { map ->
                formGenerator.validateCheckboxDialogue(id, formLayout, map)
            }.show(childFragmentManager, CheckBoxDialog.TAG)
    }

    override fun onInstructionClicked(
        id: String,
        title: String,
        informationList: ArrayList<String>?,
        description: String?,
        dosageListModel: ArrayList<RecommendedDosageListModel>?,
    ) {
    }

    override fun onFormSubmit(
        resultMap: HashMap<String, Any>?,
        serverData: List<FormLayout>?,
    ) {
        if (viewModel.isPregnancyTooEarlyToAccess) {
            viewModel.updatePregnantWomanAssessmentDetails()
        } else {
            resultMap?.let {
                val result = serverData?.let {
                    FormResultComposer().groupValues(
                        serverData = it,
                        resultMap,
                        MenuConstants.PREGNANT_WOMEN_PROFILE,
                    )
                }
                result?.second?.let {
                    viewModel.saveAssessment(serverData, it, null, viewModel.menuId)
                }
            }
        }
        viewModel.setAnalyticsData(
            UserDetail.startDateTime,
            eventType = AnalyticsDefinedParams.PREGNANT_WOMEN_PROFILE,
            eventName = AnalyticsDefinedParams.AssessmentCreation,
        )
    }

    override fun onRenderingComplete() {
    }

    override fun onUpdateInstruction(
        id: String,
        selectedId: Any?,
    ) {
    }

    override fun onInformationHandling(
        id: String,
        noOfDays: Int,
        enteredDays: Int?,
        resultMap: HashMap<String, Any>?,
    ) {
    }

    override fun onAgeCheckForPregnancy() {
    }

    override fun handleMandatoryCondition(formLayout: FormLayout?) {
    }

    override fun onAgeUpdateListener(
        age: Int,
        serverData: List<FormLayout>?,
        resultHashMap: HashMap<String, Any>,
    ) {
    }

    override fun onQRScanRequested() {
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.btnSubmit.id -> {
                withLocationCheck({
                    viewModel.setUserJourney(AnalyticsDefinedParams.SUBMITBUTTONTRIGGERED)
                    viewModel.fetchCurrentLocation(requireContext())
                    formGenerator.formSubmitAction(view)
                })
            }
        }
    }

    /**
     * Updates the visibility of the Pregnancy Test field based on gestational age.
     * Uses total gestational days (weeks × 7 + days) so 16 weeks + 1 day exceeds the limit.
     */
    private fun updatePregnancyTestVisibility(gestationalAge: Pair<Long, Long>) {
        val totalGestationalDays = gestationalAge.first * 7 + gestationalAge.second
        val pregnancyTestView = formGenerator.getViewByTag(
            PregnantWomen.ID_PREGNANCY_TEST + formGenerator.rootSuffix,
        )
        if (totalGestationalDays <= PregnantWomen.PREGNANCY_TEST_MAX_GESTATIONAL_DAYS) {
            pregnancyTestView?.visible()
        } else {
            pregnancyTestView?.let { view ->
                formGenerator.resetChildViews(view)
                view.gone()
            }
        }
    }

    /**
     * Returns whether user has filled any details already or not.
     *
     * Based on this check, when user pressing back, we show alert for confirmation
     */
    fun getCurrentAnsweredStatus(): Boolean = formGenerator.getResultMap().isNotEmpty()

    companion object {
        const val TAG = "AssessmentPregnantWomenRegistrationFragment"
    }
}
