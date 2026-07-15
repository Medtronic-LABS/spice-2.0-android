package org.medtroniclabs.uhis.ui.assessment.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatSpinner
import androidx.fragment.app.activityViewModels
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.model.UserDetail
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams.TBTYPE
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.DefinedParams.DEFAULT_ID
import org.medtroniclabs.uhis.common.DefinedParams.ID
import org.medtroniclabs.uhis.common.DefinedParams.NAME
import org.medtroniclabs.uhis.common.DefinedParams.TB
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.model.RecommendedDosageListModel
import org.medtroniclabs.uhis.databinding.FragmentAssessmentTBBinding
import org.medtroniclabs.uhis.formgeneration.FormGenerator
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.listener.FormEventListener
import org.medtroniclabs.uhis.formgeneration.model.FormLayout
import org.medtroniclabs.uhis.formgeneration.ui.FormResultComposer
import org.medtroniclabs.uhis.formgeneration.utility.CustomSpinnerAdapter
import org.medtroniclabs.uhis.mappingkey.RxBuddy
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.DateOfOnset
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.SleepLocation
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.TBScreening
import org.medtroniclabs.uhis.ui.assessment.referrallogic.ReferralResultGenerator
import org.medtroniclabs.uhis.ui.assessment.viewmodel.AssessmentViewModel

class AssessmentTBFragment : BaseFragment(), FormEventListener, View.OnClickListener {
    private lateinit var binding: FragmentAssessmentTBBinding
    private lateinit var formGenerator: FormGenerator
    private val viewModel: AssessmentViewModel by activityViewModels()
    private var isContactTracing: Boolean? = null
    private var isTBPatient: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isContactTracing = arguments?.getBoolean(DefinedParams.CONTACT_TRACING, false)
        isTBPatient = arguments?.getBoolean(DefinedParams.isTbPatient, false)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentAssessmentTBBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.getTbType(viewModel.selectedHouseholdMemberId, isContactTracing)
        showBioDataFragment()
        attachObservers()
        setListeners()
        viewModel.getNearestHealthFacility()
        viewModel.setUserJourney(AnalyticsDefinedParams.TBAssessement)
    }

    private fun showBioDataFragment() {
        replaceFragmentInId<BioDataFragment>(
            binding.bioDataFragmentContainer.id,
            tag = BioDataFragment.TAG,
        )

        formGenerator = FormGenerator(
            requireContext(),
            binding.llForm,
            this,
            binding.scrollView,
            translate = SecuredPreference.getIsTranslationEnabled(),
        ) { map, id ->
            when (id) {
                DateOfOnset -> {
                    formGenerator.getViewByTag(id)?.let {
                        val dob = viewModel.memberDetailsLiveData.value
                            ?.data
                            ?.dateOfBirth
                        if (!dob.isNullOrEmpty() && map.containsKey(id)) {
                            val selectedDate = map[id].toString()
                            val isValid = DateUtils.compareDates(dob, selectedDate)
                            formGenerator
                                .getViewByTag(id + AssessmentDefinedParams.errorSuffix)
                                ?.apply {
                                    visibility = if (isValid) View.GONE else View.VISIBLE
                                }.takeIf { it is TextView }
                                ?.let { textView ->
                                    (textView as TextView).text =
                                        getString(R.string.the_day_s_should_be_less_than_age)
                                }
                        }
                    }
                }
            }
        }
    }

    private fun showRxBuddyCard() {
        binding.scrollView.viewTreeObserver.addOnGlobalLayoutListener {
            binding.scrollView.requestLayout()
        }
        replaceFragmentInId<TBRxBuddyFragment>(
            binding.rxBuddyDetailsFragmentContainer.id,
            tag = TBRxBuddyFragment.TAG,
        )
    }

    private fun showTreatmentDetailsCard() {
        binding.scrollView.viewTreeObserver.addOnGlobalLayoutListener {
            binding.scrollView.requestLayout()
        }
        replaceFragmentInId<TBTreatmentFragment>(
            binding.treatmentDetailsFragmentContainer.id,
            tag = TBTreatmentFragment.TAG,
        )
    }

    private fun setListeners() {
        binding.btnSubmit.safeClickListener(this)
    }

    private fun attachObservers() {
        viewModel.assessmentTBType.observe(viewLifecycleOwner) {
            it?.let {
                viewModel.getFormData(TB.lowercase(), it)
            }
        }

        viewModel.formLayoutsLiveData.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.SUCCESS -> {
                    hideProgress()
                    resourceState.data?.let { data ->
                        formGenerator.populateViews(data.formLayout)
                        viewModel.setAnalyticsData(
                            UserDetail.startDateTime,
                            eventType = AnalyticsDefinedParams.TBAssessement,
                            eventName = AnalyticsDefinedParams.AssessmentCreation,
                        )
                    }
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }
            }
        }

        viewModel.otherHouseholdMemberLiveData.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                }
                ResourceState.SUCCESS -> {
                    val members = resourceState.data ?: arrayListOf()
                    members.add(
                        0,
                        hashMapOf<String, Any>(
                            NAME to getString(R.string.please_select),
                            ID to DEFAULT_ID,
                        ),
                    )

                    val view =
                        formGenerator.getViewByTag(RxBuddy.selectHouseholdMember) as? AppCompatSpinner
                    if (view != null) {
                        (view.adapter as CustomSpinnerAdapter).setData(members)
                    }
                }
                ResourceState.ERROR -> {
                }
            }
        }

        viewModel.treatmentDetailsLiveData.observe(viewLifecycleOwner) {
            it?.let {
                showTreatmentDetailsCard()
            }
        }

        viewModel.rxBuddyDetailsLiveData.observe(viewLifecycleOwner) {
            it?.let {
                showRxBuddyCard()
            }
        }
    }

    companion object {
        const val TAG = "AssessmentTBFragment"
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
    }

    override fun onInstructionClicked(
        id: String,
        title: String,
        informationList: ArrayList<String>?,
        description: String?,
        dosageListModel: ArrayList<RecommendedDosageListModel>?,
    ) {}

    override fun onFormSubmit(
        resultMap: HashMap<String, Any>?,
        serverData: List<FormLayout>?,
    ) {
        val dob = viewModel.memberDetailsLiveData.value
            ?.data
            ?.dateOfBirth
        serverData?.forEach { view ->
            if (view?.id == SleepLocation) {
                val option = view.optionsList
                val sleepLocationId = resultMap?.get(SleepLocation)
                option?.forEach { item ->
                    if (item[DefinedParams.ID] == sleepLocationId) {
                        item[DefinedParams.NAME]?.let {
                            resultMap?.put(SleepLocation, it)
                        }
                    }
                }
            }
        }

        resultMap?.let { details ->
            dob?.let { dobDate ->
                val isValid = if (details.containsKey(DateOfOnset)) {
                    val selectedDate = details[DateOfOnset].toString()
                    DateUtils.compareDates(dobDate, selectedDate)
                } else {
                    true
                }
                formGenerator
                    .getViewByTag("${DateOfOnset}${AssessmentDefinedParams.errorSuffix}")
                    ?.apply {
                        visibility = if (isValid) View.GONE else View.VISIBLE
                    }.takeIf { it is TextView }
                    ?.let { textView ->
                        (textView as TextView).text =
                            getString(R.string.the_day_s_should_be_less_than_age)
                    }

                if (isValid) {
                    val referralResult =
                        ReferralResultGenerator().calculateTBReferralResult(details)
                    val result = serverData?.let {
                        FormResultComposer().groupValues(
                            serverData = it,
                            details,
                            TB.lowercase(),
                        )
                    }
                    result?.second?.let {
                      /*  viewModel.memberDetailsLiveData.value?.data?.id?.let { hhmId ->
                            viewModel.updateTBContactTraceStatus(
                                hhmId = hhmId,
                                tbContactTracingStatus = Contact_Trace_Updated
                            )
                        }*/

                        val tbType = viewModel.assessmentTBType.value ?: TBScreening
                        viewModel.setUserJourney("$TBTYPE - ${viewModel.assessmentTBType.value ?: TBScreening}")
                        viewModel.saveTbAssessment(serverData, it, referralResult, tbType, viewModel.menuId)
                        viewModel.setUserJourney(AnalyticsDefinedParams.SUBMITBUTTONTRIGGERED)
                    }
                }
            }
        }
    }

    override fun onRenderingComplete() {
        viewModel.getOtherHouseholdMemberExcludeTBPatient()
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
        /*
       Never used
         */
    }

    override fun onQRScanRequested() {
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.btnSubmit.id -> {
                withLocationCheck({
                    viewModel.fetchCurrentLocation(requireContext())
                    formGenerator.formSubmitAction(view)
                })
            }
        }
    }

    fun getCurrentAnsweredStatus(): Boolean = formGenerator.getResultMap().isNotEmpty()
}
