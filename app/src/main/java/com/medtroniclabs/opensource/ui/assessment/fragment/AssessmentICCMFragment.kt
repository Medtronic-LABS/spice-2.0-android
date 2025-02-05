package com.medtroniclabs.opensource.ui.assessment.fragment

import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.app.analytics.model.UserDetail
import com.medtroniclabs.opensource.app.analytics.utils.AnalyticsDefinedParams
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.CommonUtils.isMandateOrNot
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ
import com.medtroniclabs.opensource.common.DateUtils.getYearMonthAndWeek
import com.medtroniclabs.opensource.common.DefinedParams.DefaultID
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.data.model.RecommendedDosageListModel
import com.medtroniclabs.opensource.databinding.FragmentAssessmentBinding
import com.medtroniclabs.opensource.formgeneration.FormGenerator
import com.medtroniclabs.opensource.formgeneration.config.DefinedParams.Information
import com.medtroniclabs.opensource.formgeneration.config.DefinedParams.VISIBLE
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.formgeneration.listener.FormEventListener
import com.medtroniclabs.opensource.formgeneration.model.FormLayout
import com.medtroniclabs.opensource.formgeneration.ui.FormResultComposer
import com.medtroniclabs.opensource.formgeneration.utility.CheckBoxDialog
import com.medtroniclabs.opensource.formgeneration.utility.FastBreathingLayoutFragment
import com.medtroniclabs.opensource.formgeneration.utility.InformationLayoutFragment
import com.medtroniclabs.opensource.formgeneration.utility.RecommendedDosageFragment
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseFragment
import com.medtroniclabs.opensource.ui.MenuConstants.ICCM_MENU_ID
import com.medtroniclabs.opensource.ui.assessment.AssessmentCommonUtils.getMuacColorCode
import com.medtroniclabs.opensource.ui.assessment.AssessmentCommonUtils.getNutritionStatus
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.ACT
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.ACTStatus
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.Amoxicillin
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.AmoxicillinStatus
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.BreathPerMinute
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.CoughCondition
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.DiarrheaCondition
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.FB_MAX_BREATHING
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.FB_MAX_MONTH
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.FB_MAX_YEAR
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.FB_MIN_BREATHING
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.FB_MIN_MONTH
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.FB_MIN_YEAR
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.FeverCondition
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.JellyWaterDispensedStatus
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.MUAC
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.MalnutritionCondition
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.ModerateDehydration
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.NoOfDaysDiarrhoea
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.ORSStatus
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.OrsDispensedStatus
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.SevereDehydration
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.SssDispensedStatus
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.ZincDispensedStatus
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.ZincStatus
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.chestInDrawing
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.hasCough
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.hasDiarrhoea
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.hasOedemaOfBothFeet
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.infoSuffixText
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.isBreastfeed
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.isConvulsionPastFewDays
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.isUnusualSleepy
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.isVomiting
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.muacCode
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.muacStatus
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.rdtTest
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.rootSuffix
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.summaryKey
import com.medtroniclabs.opensource.ui.assessment.dialog.DangerSignsDialog
import com.medtroniclabs.opensource.ui.assessment.referrallogic.ReferralResultGenerator
import com.medtroniclabs.opensource.ui.assessment.referrallogic.model.ReferralDefinedParams.IsBloodyDiarrhoea
import com.medtroniclabs.opensource.ui.assessment.referrallogic.model.ReferralDefinedParams.RdtPositive
import com.medtroniclabs.opensource.ui.assessment.referrallogic.model.ReferralDefinedParams.RdtTest
import com.medtroniclabs.opensource.ui.assessment.referrallogic.utils.ReferralReasons
import com.medtroniclabs.opensource.ui.assessment.viewmodel.AssessmentViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AssessmentICCMFragment : BaseFragment(), FormEventListener, View.OnClickListener,
    DangerSignsDialog.DangerSignsClickListener {

    private lateinit var binding: FragmentAssessmentBinding
    private lateinit var formGenerator: FormGenerator
    private val viewModel: AssessmentViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAssessmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        getFormDataForWorkflow()
        setListeners()
        attachObservers()
        viewModel.setUserJourney(AnalyticsDefinedParams.ICCMAssessment)
    }

    private fun setListeners() {
        binding.btnSubmit.safeClickListener(this)
    }

    private fun getFormDataForWorkflow() {
        viewModel.getFormData(ICCM_MENU_ID)
        viewModel.getNearestHealthFacility()
    }

    private fun attachObservers() {
        viewModel.formLayoutsLiveData.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.SUCCESS -> {
                    hideProgress()
                    resourceState.data?.let { data ->
                        formGenerator.populateViews(data.formLayout)
                    }
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }
            }
        }
    }

    private fun renderDosageDetails(dateOfBirth: String) {
        val age = CommonUtils.convertStringDobToMonths(
            dateOfBirth
        )
        age?.let {
            /**
             * ACT Status Condition Rendering
             */
            if (age >= 6) {
                formGenerator.getViewByTag(ACTStatus)?.let {
                    if (it is TextView) {
                        it.text = requireContext().getString(R.string.act_6)
                    }
                }
                formGenerator.getViewByTag(ACTStatus + infoSuffixText)?.let {
                    if (it is TextView) {
                        it.text =
                            getACTSuffixText(viewModel.memberDetailsLiveData.value?.data?.dateOfBirth?.let { dob ->
                                CommonUtils.convertStringDobToMonths(
                                    dob
                                )
                            })
                        it.visibility = View.VISIBLE
                    }
                }
            } else {
                formGenerator.getViewByTag(ACTStatus + rootSuffix)?.apply {
                    visibility = View.GONE
                }
            }

            /**
             * Amoxicillin & Zinc Condition Status rendering
             */
            if (age >= 2) {
                formGenerator.getViewByTag(AmoxicillinStatus)?.let {
                    if (it is TextView) {
                        it.text = requireContext().getString(R.string.amoxicillin_250_mg)
                    }
                }

                formGenerator.getViewByTag(ZincStatus)?.let {
                    if (it is TextView) {
                        it.text = requireContext().getString(R.string.zinc_tablet)
                    }
                }

                formGenerator.getViewByTag(AmoxicillinStatus + infoSuffixText)?.let {
                    if (it is TextView) {
                        it.text =
                            getSuffixText(viewModel.memberDetailsLiveData.value?.data?.dateOfBirth?.let { dob ->
                                CommonUtils.convertStringDobToMonths(
                                    dob
                                )
                            })
                        it.visibility = View.VISIBLE
                    }
                }

                formGenerator.getViewByTag(ZincStatus + infoSuffixText)?.let {
                    if (it is TextView) {
                        it.text =
                            getZincSuffixText(viewModel.memberDetailsLiveData.value?.data?.dateOfBirth?.let { dob ->
                                CommonUtils.convertStringDobToMonths(
                                    dob
                                )
                            })
                        it.visibility = View.VISIBLE
                    }
                }
            }  else {
                formGenerator.getViewByTag(AmoxicillinStatus + rootSuffix)?.apply {
                    visibility = View.GONE
                }

                formGenerator.getViewByTag(ZincStatus + rootSuffix)?.apply {
                    visibility = View.GONE
                }
            }

            formGenerator.getViewByTag(ORSStatus)?.let {
                if (it is TextView) {
                    it.text = requireContext().getString(R.string.ors_packet)
                }
            }
            formGenerator.getViewByTag(ORSStatus + infoSuffixText)?.let {
                if (it is TextView) {
                    it.text = requireContext().getString(R.string.with_one_litre_of_water)
                    it.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun initView() {
        replaceFragmentInId<BioDataFragment>(
            binding.bioDataFragmentContainer.id,
            tag = BioDataFragment.TAG
        )
        formGenerator = FormGenerator(
            requireContext(), binding.llForm, null, this, binding.scrollView,
            translate = SecuredPreference.getIsTranslationEnabled()
        )
    }

    companion object {
        const val TAG = "AssessmentICCMFragment"
    }

    override fun loadLocalCache(id: String, localDataCache: Any, selectedParent: Long?) {
    }

    override fun onPopulate(targetId: String) {
    }

    override fun onCheckBoxDialogueClicked(
        id: String,
        serverViewModel: FormLayout,
        resultMap: Any?
    ) {
        CheckBoxDialog.newInstance(id, resultMap) { map ->
            formGenerator.validateCheckboxDialogue(id, serverViewModel, map)
        }.show(childFragmentManager, CheckBoxDialog.TAG)
    }

    override fun onInstructionClicked(
        id: String,
        title: String,
        informationList: ArrayList<String>?,
        description: String?,
        dosageListModel: ArrayList<RecommendedDosageListModel>?
    ) {
        viewModel.instructionId = id
        viewModel.dosageListModel = dosageListModel
        showInstructionDialog(id)
    }

    private fun showInstructionDialog(id: String) {
        val titleById = getTitleById(id)
        when (id) {
            isUnusualSleepy,isVomiting,isConvulsionPastFewDays,isBreastfeed,muacCode, hasOedemaOfBothFeet, chestInDrawing -> {
                InformationLayoutFragment.newInstance(id, titleById)
                    .show(childFragmentManager, InformationLayoutFragment.TAG)
            }

            Amoxicillin.lowercase(), ZincDispensedStatus, ACT.lowercase(), OrsDispensedStatus, JellyWaterDispensedStatus, SssDispensedStatus -> {
                RecommendedDosageFragment.newInstance(id, titleById)
                    .show(childFragmentManager, RecommendedDosageFragment.TAG)
            }

            BreathPerMinute -> {
                FastBreathingLayoutFragment.newInstance().show(childFragmentManager, FastBreathingLayoutFragment.TAG)
            }
        }
    }

    private fun getTitleById(id: String): String {
        return when (id) {
            muacCode -> getString(R.string.measuring_muac)
            hasOedemaOfBothFeet -> getString(R.string.checking_for_oedema)
            chestInDrawing -> getString(R.string.chest_in_drawing)
            isUnusualSleepy-> getString(R.string.job_aid_unconscious_unusually_sleepy)
            isConvulsionPastFewDays -> getString(R.string.job_aid_convulsions)
            isVomiting -> getString(R.string.job_aid_vomiting)
            isBreastfeed -> getString(R.string.job_aid_not_able_to_breastfeed_drink)
            else -> {id}
        }
    }


    override fun onFormSubmit(resultMap: HashMap<String, Any>?, serverData: List<FormLayout?>?) {
        onSubmitICCM(resultMap,serverData)
    }

    fun onSubmitICCM(resultMap: HashMap<String, Any>?, serverData: List<FormLayout?>?) {
        if (viewModel.isDangerSignFlow){
            resultMap?.keys?.retainAll(listOf(isUnusualSleepy,isConvulsionPastFewDays,isBreastfeed,isVomiting))
        }
        resultMap?.let { details ->
            composeICCMOtherMetrics(details)
            val referralResult = ReferralResultGenerator().calculateIccmReferralResult(details, viewModel.memberDetailsLiveData.value?.data)
            val result = serverData?.let {
                FormResultComposer().groupValues(
                    context = requireContext(),
                    serverData = it,
                    details,
                    ICCM_MENU_ID
                )
            }

            result?.second?.let {
                viewModel.saveAssessment(it, referralResult,viewModel.menuId)
            }
        }
        viewModel.setAnalyticsData(
            UserDetail.startDateTime,
            eventType = AnalyticsDefinedParams.ICCMAssessment,
            eventName = AnalyticsDefinedParams.AssessmentCreation
        )
    }

    private fun composeICCMOtherMetrics(resultMap: HashMap<String, Any>) {
        if (resultMap.containsKey(muacCode) && resultMap[muacCode] is String) {
            getNutritionStatus(resultMap[muacCode] as String, requireContext()).let { value ->
                if (value != "-") {
                    viewModel.otherAssessmentDetails[MalnutritionCondition] = value
                }
            }
        }
        if (resultMap.containsKey(BreathPerMinute) && resultMap[BreathPerMinute] is Int) {
            (resultMap[BreathPerMinute] as Int).let { bpmValue ->
                viewModel.memberDetailsLiveData.value?.data?.let { details ->
                    DateUtils.dateToMonths(details.dateOfBirth).let { month ->
                        month?.let {
                            if ((month in FB_MIN_MONTH..11) && bpmValue >= FB_MAX_BREATHING) {
                                viewModel.otherAssessmentDetails[CoughCondition] =
                                    ReferralReasons.Pneumonia.name
                            } else if (month in FB_MAX_MONTH..60 && bpmValue >= FB_MIN_BREATHING) {
                                viewModel.otherAssessmentDetails[CoughCondition] =
                                    ReferralReasons.Pneumonia.name
                            }
                        }
                    }
                }
            }
        }
        if (resultMap.containsKey(RdtTest) && resultMap[RdtTest] is String) {
            if (resultMap[RdtTest] == RdtPositive) {
                viewModel.otherAssessmentDetails[FeverCondition] =
                    ReferralReasons.Malaria.name
            }
        }
        if (resultMap.containsKey(hasDiarrhoea) && resultMap[hasDiarrhoea] is Boolean && resultMap[hasDiarrhoea] as Boolean) {
            if ((resultMap.containsKey(NoOfDaysDiarrhoea) && resultMap[NoOfDaysDiarrhoea] is Int && ((resultMap[NoOfDaysDiarrhoea] as Int) >= 14)) ||
                (resultMap.containsKey(IsBloodyDiarrhoea) && resultMap[IsBloodyDiarrhoea] is Boolean && resultMap[IsBloodyDiarrhoea] as Boolean)
            ) {
                viewModel.otherAssessmentDetails[DiarrheaCondition] = SevereDehydration
            } else {
                viewModel.otherAssessmentDetails[DiarrheaCondition] = ModerateDehydration
            }
        }
    }


    override fun onRenderingComplete() {

    }

    override fun onUpdateInstruction(id: String, selectedId: Any?) {
        when (id) {
            muacCode -> {
                if (selectedId is String && selectedId != DefaultID)
                {
                    formGenerator.getViewByTag(muacStatus + rootSuffix )?.apply {
                        visibility = View.VISIBLE
                    }
                    formGenerator.getViewByTag(muacStatus + summaryKey)?.let {
                        if (it is TextView) {
                            it.text = requireContext().getString(
                                R.string.firstname_lastname,
                                MUAC.uppercase(),
                                selectedId
                            )
                        }
                    }
                    formGenerator.getViewByTag(muacStatus)?.let {
                        if (it is TextView) {
                            it.text = getNutritionStatus(selectedId, requireContext())
                        }
                    }
                    formGenerator.getViewByTag(muacCode)?.apply {
                        val background = background as? GradientDrawable
                        background?.setStroke(resources.getDimensionPixelSize(R.dimen._4sdp), getMuacColorCode(selectedId as String, requireContext()))
                    }
                } else {
                    formGenerator.getViewByTag(muacStatus + rootSuffix)?.apply {
                        visibility = View.GONE
                    }
                    formGenerator.getViewByTag(muacCode)?.apply {
                        val background = background as? GradientDrawable
                        background?.setStroke(resources.getDimensionPixelSize(R.dimen._1sdp), getMuacColorCode(selectedId as String, requireContext()))
                    }
                }

            }
            hasCough ->{
                viewModel.memberDetailsLiveData.value?.data?.let {
                    renderDosageDetails(it.dateOfBirth)
                }
                handleRecommendedDoseage(hasCough)
            }
            hasDiarrhoea,rdtTest -> {
                viewModel.memberDetailsLiveData.value?.data?.let {
                    renderDosageDetails(it.dateOfBirth)
                }
            }
            isUnusualSleepy,isConvulsionPastFewDays,isVomiting,isBreastfeed->{
                if (selectedId==true) {
                    viewModel.isDangerSignFlow=true
                    val existingFragment =
                        childFragmentManager.findFragmentByTag(
                            DangerSignsDialog.TAG
                        )
                    if (existingFragment == null) {
                        DangerSignsDialog.newInstance(id)
                            .apply { setDangerSignListener(this@AssessmentICCMFragment) }
                            .show(childFragmentManager, DangerSignsDialog.TAG)
                    }
                }
            }

        }
    }

    private fun handleRecommendedDoseage(id: String) {
        viewModel.memberDetailsLiveData.value?.data?.let { data ->
            getYearMonthAndWeek(data.dateOfBirth, DATE_FORMAT_yyyyMMddHHmmssZZZZZ).let { result ->
                val year = result.second.first ?: 0
                val month = result.second.second ?: 0
                if ((year == 0 && month in 0..2)) {
                    displayDaysInformation(id, View.INVISIBLE)
                    formGenerator.getViewByTag((Amoxicillin.lowercase()) + rootSuffix)?.apply {
                        visibility = View.GONE
                    }
                    formGenerator.getViewByTag(AmoxicillinStatus + rootSuffix)?.apply {
                        visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun getZincSuffixText(age: Int?): CharSequence? {
        return when(age){
            in 2..6 -> {
                requireContext().getString(R.string.no_of_tablets_no_of_days_string, "1/2", 10)
            }
            in 7..60 -> {
                requireContext().getString(R.string.no_of_tablets_no_of_days, 1, 10)
            }
            else ->{
                null
            }
        }
    }

    private fun getSuffixText(age: Int?): String? {
        return when(age){
            in 2..12 -> {
               requireContext().getString(R.string.no_of_tablets_no_of_days, 2, 5)
            }
            in 12..36 -> {
                requireContext().getString(R.string.no_of_tablets_no_of_days, 4, 5)
            }
            in 36..60 -> {
                requireContext().getString(R.string.no_of_tablets_no_of_days, 6, 5)
            }
            else ->{
                null
            }
        }
    }

    private fun getACTSuffixText(age: Int?): String {
        return when(age){
            in 6..36 -> {
               requireContext().getString(R.string.no_of_tablets_no_of_days, 2, 3)
            }
            in 37..96 -> {
                requireContext().getString(R.string.no_of_tablets_no_of_days, 4, 3)
            }
            in 97..168 -> {
                requireContext().getString(R.string.no_of_tablets_no_of_days, 6, 3)
            }
            else ->{
                requireContext().getString(R.string.no_of_tablets_no_of_days, 8, 3)
            }
        }
    }

    override fun onInformationHandling(
        id: String,
        noOfDays: Int,
        enteredDays: Int?,
        resultMap: HashMap<String, Any>?
    ) {
        when (id) {
            BreathPerMinute -> {
                enteredDays?.let {
                    viewModel.memberDetailsLiveData.value?.data?.let { data ->
                        getYearMonthAndWeek(data.dateOfBirth, DATE_FORMAT_yyyyMMddHHmmssZZZZZ).let { result ->
                            val year = result.second.first ?: 0
                            val month = result.second.second ?: 0
                            if ((year == 0 && month > FB_MIN_MONTH && month < FB_MAX_MONTH) && enteredDays >= FB_MAX_BREATHING) {
                                displayDaysInformation(id, View.VISIBLE)
                                getAmoxicillinStatus()
                            } else if ((month == FB_MAX_MONTH || year in FB_MIN_YEAR..FB_MAX_YEAR) && enteredDays >= FB_MIN_BREATHING) {
                                displayDaysInformation(id, View.VISIBLE)
                                getAmoxicillinStatus()
                            } else {
                                displayDaysInformation(id, View.INVISIBLE)
                                dismissAmoxicillinStatus(resultMap)
                            }
                        }
                    }
                } ?: kotlin.run {
                    displayDaysInformation(id, View.INVISIBLE)
                }
            }
            else -> {
                if (enteredDays!=null && enteredDays > noOfDays) {
                    updateColorCode(id, ContextCompat.getColor(requireContext(), R.color.medium_high_risk_color))
                    displayDaysInformation(id, View.VISIBLE)
                } else {
                    updateColorCode(id, ContextCompat.getColor(requireContext(), R.color.secondary_black))
                    displayDaysInformation(id, View.INVISIBLE)
                }
            }
        }
    }

    override fun onAgeCheckForPregnancy() {
        
    }

    override fun handleMandatoryCondition(serverData: FormLayout?) {
        if (serverData?.id == muacCode) {
            viewModel.selectedMemberDob?.let { dateOfBirth ->
                val visibility = isMandateOrNot(dateOfBirth)
                serverData.visibility = visibility
                serverData.isMandatory = (visibility == VISIBLE)
                serverData.isSummary = (visibility == VISIBLE)
            }
        }

        if (serverData?.id == muacStatus) {
            viewModel.selectedMemberDob?.let { dateOfBirth ->
                serverData.visibility = isMandateOrNot(dateOfBirth)
            }
        }
    }

    override fun onAgeUpdateListener(
        age: Int,
        serverData: List<FormLayout?>?,
        resultHashMap: HashMap<String, Any>
    ) {
        /*
       Never used
        */
    }

    private fun dismissAmoxicillinStatus(resultMap: HashMap<String, Any>?) {
        formGenerator.getViewByTag((Amoxicillin.lowercase()) + rootSuffix )?.apply {
            visibility = View.GONE
            resultMap?.let {map ->
                if (map.containsKey(Amoxicillin.lowercase())){
                    map.remove(Amoxicillin.lowercase())
                    formGenerator.resetSingleSelection(Amoxicillin.lowercase())
                }
            }
        }
        formGenerator.getViewByTag(AmoxicillinStatus + rootSuffix)?.apply {
            visibility = View.GONE
        }
    }

    private fun getAmoxicillinStatus() {
        formGenerator.getViewByTag((Amoxicillin.lowercase()) + rootSuffix )?.apply {
            visibility = View.VISIBLE
        }
        formGenerator.getViewByTag(AmoxicillinStatus + rootSuffix)?.apply {
            visibility = View.VISIBLE
        }
    }

    private fun updateColorCode(id: String, colorCode: Int) {
        formGenerator.getViewByTag(id + Information)?.let { view ->
            if (view is TextView){
                view.setTextColor(colorCode)
            }
        }
    }

    private fun displayDaysInformation(id: String, viewVisibility: Int) {
        formGenerator.getViewByTag(id + Information)?.apply { visibility = viewVisibility }
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.btnSubmit.id -> {
                formGenerator.formSubmitAction(view)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val dosageDialog = childFragmentManager.findFragmentByTag("RecommendedDosageFragment") as? DialogFragment
        val instructionDialog = childFragmentManager.findFragmentByTag("InformationLayoutFragment") as? DialogFragment
        val fastBreathingDialog = childFragmentManager.findFragmentByTag("FastBreathingLayoutFragment") as? DialogFragment
        if (dosageDialog != null && dosageDialog.showsDialog) {
            dosageDialog.dismiss()
            showDialogBasedOnId()
        }

        if (instructionDialog != null && instructionDialog.showsDialog) {
            instructionDialog.dismiss()
            showDialogBasedOnId()
        }

        if (fastBreathingDialog != null && fastBreathingDialog.showsDialog) {
            fastBreathingDialog.dismiss()
            showDialogBasedOnId()
        }
    }

    private fun showDialogBasedOnId() {
        viewModel.instructionId?.let {
            showInstructionDialog(it)
        }
    }

    fun getCurrentAnsweredStatus(): Boolean {
        return formGenerator.getResultMap().isNotEmpty()
    }

    override fun onDangerSignsClicked() {
        onSubmitICCM(formGenerator.getResultMap(),formGenerator.getServerData())
    }
}