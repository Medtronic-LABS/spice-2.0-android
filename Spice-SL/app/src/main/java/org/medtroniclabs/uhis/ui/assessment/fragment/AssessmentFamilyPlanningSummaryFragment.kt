package org.medtroniclabs.uhis.ui.assessment.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.StringConverter
import org.medtroniclabs.uhis.databinding.FragmentAssessmentFamilyPlanningSummaryBinding
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.MenuConstants
import org.medtroniclabs.uhis.ui.assessment.AssessmentCommonUtils
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams
import org.medtroniclabs.uhis.ui.assessment.FamilyPlanning
import org.medtroniclabs.uhis.ui.assessment.viewmodel.AssessmentViewModel

class AssessmentFamilyPlanningSummaryFragment : BaseFragment(), View.OnClickListener {
    private val viewModel: AssessmentViewModel by activityViewModels()
    lateinit var binding: FragmentAssessmentFamilyPlanningSummaryBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentAssessmentFamilyPlanningSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        attachObservers()
    }

    private fun attachObservers() {
        viewModel.assessmentStringLiveData.value?.let {
            renderSummaryData(it)
        }
    }

    private fun initView() {
        viewModel.setUserJourney(AnalyticsDefinedParams.FAMILYPLANNINGSUMMARY)
        binding.btnDone.safeClickListener(this)
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.btnDone -> {
                viewModel.setUserJourney(AnalyticsDefinedParams.DONEBUTTONTRIGGERED)
                viewModel.updateFamilyPlanningAssessmentDetails()
            }
        }
    }

    /**
     * Renders summary data
     */
    private fun renderSummaryData(data: String) {
        val convertedMap = StringConverter.stringToMap(data)
        if (convertedMap.isEmpty()) {
            showErrorInSummary()
        } else {
            binding.emptyErrorMessage.gone()
            val fpMap = convertedMap[MenuConstants.FP_MENU_ID.lowercase()] as Map<*, *>
            val assessmentMap = fpMap[AssessmentDefinedParams.FamilyPlanningDetails] as Map<*, *>
            val noOfChildren = CommonUtils.getInteger(assessmentMap[AssessmentDefinedParams.NUMBER_OF_LIVING_CHILDREN])
            val desireForChildren = assessmentMap[AssessmentDefinedParams.DESIRE_FOR_CHILDREN_IN_FUTURE] as? String
            val familyPlanningMethod = assessmentMap[AssessmentDefinedParams.FamilyPlanningMethods] as? List<*>
            // No of children
            val noOfChildrenForm =
                viewModel.formLayoutsLiveData.value
                    ?.data
                    ?.formLayout
                    ?.firstOrNull { it.id.equals(AssessmentDefinedParams.NUMBER_OF_LIVING_CHILDREN, true) }
            noOfChildrenForm?.let {
                bindSummaryView(
                    noOfChildrenForm.getSummaryTitle(isTranslationEnabled),
                    noOfChildren.toString(),
                )
            }

            // Desire for children
            val desireForChildrenForm =
                viewModel.formLayoutsLiveData.value
                    ?.data
                    ?.formLayout
                    ?.firstOrNull { it.id.equals(AssessmentDefinedParams.DESIRE_FOR_CHILDREN_IN_FUTURE, true) }
            desireForChildrenForm?.let {
                val option = desireForChildrenForm.optionsList?.firstOrNull { it[DefinedParams.ID] == desireForChildren }
                bindSummaryView(
                    desireForChildrenForm.getSummaryTitle(isTranslationEnabled),
                    (if (isTranslationEnabled) option?.get(DefinedParams.CULTURE_VALUE) else option?.get(DefinedParams.NAME)) as? String,
                )
            }

            // Current use of family planning
            val familyPlanningMethodForm =
                viewModel.formLayoutsLiveData.value
                    ?.data
                    ?.formLayout
                    ?.firstOrNull { it.id.equals(AssessmentDefinedParams.FamilyPlanningMethods, true) }
            familyPlanningMethodForm?.let {
                val option = familyPlanningMethodForm.optionsList?.firstOrNull { it[DefinedParams.ID] == familyPlanningMethod?.firstOrNull() }
                bindSummaryView(
                    familyPlanningMethodForm.getSummaryTitle(isTranslationEnabled),
                    (if (isTranslationEnabled) option?.get(DefinedParams.CULTURE_VALUE) else option?.get(DefinedParams.NAME)) as? String,
                )
            }

            FamilyPlanning
                .getRecommendedFamilyPlanningMethod(
                    requireContext(),
                    desireForChildren,
                    noOfChildren,
                    familyPlanningMethod?.firstOrNull()?.toString(),
                )?.let {
                    bindSummaryView(getString(R.string.recommended_method), it)
                }
        }
    }

    private fun showErrorInSummary() {
        binding.emptyErrorMessage.visibility = View.VISIBLE
        binding.parentLayout.visibility = View.GONE
    }

    private fun bindSummaryView(
        title: String?,
        value: String?,
        valueTextColor: Int? = null,
    ) {
        binding.parentLayout.addView(
            AssessmentCommonUtils.addViewSummaryLayout(
                title,
                value,
                valueTextColor,
                requireContext(),
            ),
        )
    }

    companion object {
        const val TAG = "AssessmentFamilyPlanningSummaryFragment"

        fun newInstance(): AssessmentFamilyPlanningSummaryFragment = AssessmentFamilyPlanningSummaryFragment()
    }

    fun getCurrentAnsweredStatus(): Boolean = viewModel.otherAssessmentDetails.isNotEmpty()
}
