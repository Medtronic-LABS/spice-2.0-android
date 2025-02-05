package com.medtroniclabs.opensource.ui.assessment.fragment

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.app.analytics.utils.AnalyticsDefinedParams
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.StringConverter.stringToMap
import com.medtroniclabs.opensource.common.ViewUtils
import com.medtroniclabs.opensource.databinding.FragmentAssessmentRmnchNeonateSummaryBinding
import com.medtroniclabs.opensource.formgeneration.config.ViewType
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.formgeneration.model.FormResponse
import com.medtroniclabs.opensource.formgeneration.utility.CustomSpinnerAdapter
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseFragment
import com.medtroniclabs.opensource.ui.assessment.AssessmentCommonUtils
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams
import com.medtroniclabs.opensource.ui.assessment.referrallogic.utils.ReferralStatus
import com.medtroniclabs.opensource.ui.assessment.rmnch.RMNCH
import com.medtroniclabs.opensource.ui.assessment.rmnch.RMNCH.getValueFromMap
import com.medtroniclabs.opensource.ui.assessment.viewmodel.AssessmentRMNCHNeonateViewModel
import com.medtroniclabs.opensource.ui.assessment.viewmodel.AssessmentViewModel
import com.medtroniclabs.opensource.ui.household.HouseholdSearchActivity

class AssessmentRMNCHNeonateSummaryFragment : BaseFragment(), View.OnClickListener {

    private lateinit var binding: FragmentAssessmentRmnchNeonateSummaryBinding

    private val assessmentRMNCHNeonateViewModel: AssessmentRMNCHNeonateViewModel by activityViewModels()

    private val viewModel: AssessmentViewModel by activityViewModels()

    private var datePickerDialog: DatePickerDialog? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentAssessmentRmnchNeonateSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        setListener()
        viewModel.setUserJourney(AnalyticsDefinedParams.RMNCHNeonateSummaryAssessment)
    }

    private fun initView() {
        viewModel.getNearestHealthFacility()
        assessmentRMNCHNeonateViewModel.assessmentStringSaveLiveData.value?.let {
            val map = stringToMap(it)
            if (map.containsKey(RMNCH.PNC)) {
                addDefaultSummaryView(map)
                showSummaryDetail(
                    map,
                    RMNCH.PNC,
                    binding.motherParentLayout,
                    viewModel.formLayoutsLiveData.value
                )
                showNextFollowUpDate(map)
            }
            if (map.containsKey(RMNCH.PNCNeonatal)) {
                showSummaryDetail(
                    map,
                    RMNCH.PNCNeonatal,
                    binding.neonateParentLayout,
                    assessmentRMNCHNeonateViewModel.formLayoutsLiveData.value
                )
            }
            updateStatusBar()
        }
        viewModel.nearestFacilityLiveData.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.SUCCESS -> {
                    hideProgress()
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }
            }
        }
    }

    private fun loadPhuSitesList(siteList: ArrayList<Map<String, Any>>) {
        val adapter = CustomSpinnerAdapter(requireContext())
        adapter.setData(siteList)
        binding.etPhuChange.adapter = adapter
        binding.etPhuChange.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    view: View?,
                    pos: Int,
                    itemId: Long
                ) {
                    val selectedItem = adapter.getData(position = pos)
                    selectedItem?.let {
                        val selectedId = it[DefinedParams.id] as String?
                        val selectedSiteName = it[DefinedParams.NAME] as String?
                        viewModel.otherAssessmentDetails[AssessmentDefinedParams.ReferredPHUSite] =
                            selectedSiteName ?: ""
                        viewModel.otherAssessmentDetails[AssessmentDefinedParams.ReferredPHUSiteID] =
                            selectedId?.toLong() ?: -1L
                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    /**
                     * this method is not used
                     */
                }
            }
    }

    private fun updateStatusBar() {
        when (assessmentRMNCHNeonateViewModel.referralStatus) {
            ReferralStatus.Referred.name -> {
                viewModel.nearestFacilityLiveData.value?.data?.let { siteList ->
                    loadPhuSitesList(siteList)
                }
                binding.riskResultLayout.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.attention_color)
                binding.riskResultLayout.text = getString(R.string.referred_for_further_assessment)
                binding.labelPhuReferred.visible()
                binding.etPhuChange.visible()
            }

            ReferralStatus.OnTreatment.name -> {
                binding.riskResultLayout.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.red_risk_moderate)
                binding.riskResultLayout.text = getString(R.string.patient_on_treatment)
                binding.labelPhuReferred.gone()
                binding.etPhuChange.gone()
            }

            else -> {
                binding.riskResultLayout.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.green_attention_color)
                binding.riskResultLayout.text = getString(R.string.no_refferral_treatment_required)
                binding.labelPhuReferred.gone()
                binding.etPhuChange.gone()
            }
        }
    }


    private fun setListener() {
        binding.btnDone.safeClickListener(this)
        binding.etNextFollowUpDate.safeClickListener(this)
        binding.etNextFollowUpDate.addTextChangedListener {
            binding.btnDone.isEnabled = !it.isNullOrEmpty()
        }
    }

    private fun showSummaryDetail(
        map: Map<*, *>,
        pnc: String,
        parentLayout: LinearLayout,
        value: Resource<FormResponse>?
    ) {
        value?.data?.formLayout?.filter { it.family == pnc && it.isSummary == true }
            ?.forEach { formlayout ->
                formlayout.apply {
                    parentLayout.addView(
                        AssessmentCommonUtils.addViewSummaryLayout(
                            titleSummary ?: (titleCulture ?: title),
                            getValueFromMap(
                                map as HashMap<String, Any>,
                                id,
                                viewType,
                                pnc,
                                isBooleanAnswer,
                                Triple(
                                    getString(R.string.yes),
                                    getString(R.string.no),
                                    getString(R.string.hyphen_symbol)
                                ),
                                requireContext()
                            ),
                            null,
                            requireContext()
                        )
                    )
                }
            }
    }


    private fun addDefaultSummaryView(map: HashMap<String, Any>) {

        val title: String = when (viewModel.workflowName) {
            RMNCH.ANC -> getString(R.string.anc_visit)
            RMNCH.ChildHoodVisit -> getString(R.string.child_hood_visit)
            RMNCH.PNC -> getString(R.string.pnc_visit)
            else -> getString(R.string.hyphen_symbol)
        }

        binding.motherParentLayout.addView(
            AssessmentCommonUtils.addViewSummaryLayout(
                title,
                getValueFromMap(
                    map,
                    RMNCH.visitNo,
                    ViewType.VIEW_TYPE_FORM_EDITTEXT,
                    viewModel.workflowName,
                    false,
                    Triple(
                        getString(R.string.yes),
                        getString(R.string.no),
                        getString(R.string.hyphen_symbol)
                    ),
                    requireContext()
                ),
                null,
                requireContext()
            )
        )

        binding.motherParentLayout.addView(
            AssessmentCommonUtils.addViewSummaryLayout(
                getString(R.string.date_of_delivery),
                getValueFromMap(
                    map,
                    RMNCH.DateOfDelivery,
                    ViewType.VIEW_TYPE_FORM_DATEPICKER,
                    viewModel.workflowName,
                    false,
                    Triple(
                        getString(R.string.yes),
                        getString(R.string.no),
                        getString(R.string.hyphen_symbol)
                    ),
                    requireContext()
                ), null,
                requireContext()
            )
        )

    }

    override fun onClick(v: View) {
        when (v.id) {
            binding.btnDone.id -> {
                if (binding.etNextFollowUpDate.visibility == View.VISIBLE && binding.etNextFollowUpDate.text.isNotEmpty()) {
                    updateFollowUpDate(binding.etNextFollowUpDate.text.trim().toString())
                }
                if (viewModel.otherAssessmentDetails.isEmpty()) {
                    val intent =  Intent(requireActivity(), HouseholdSearchActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    requireActivity().finish()
                } else {
                    assessmentRMNCHNeonateViewModel.updateOtherAssessmentDetails(
                        viewModel.otherAssessmentDetails,
                        viewModel.getCurrentLocation(),
                        viewModel.assessmentUpdateLiveData
                    )
                }
            }

            binding.etNextFollowUpDate.id -> {
                showDatePickerDialog()
            }
        }
    }

    private fun showDatePickerDialog() {
        var yearMonthDate: Triple<Int?, Int?, Int?>? = null
        if (!binding.etNextFollowUpDate.text.isNullOrBlank())
            yearMonthDate =
                DateUtils.convertedMMMToddMM(binding.etNextFollowUpDate.text.toString())
        if (datePickerDialog == null) {
            datePickerDialog = ViewUtils.showDatePicker(
                context = requireContext(),
                minDate = DateUtils.getTomorrowDate(),
                date = yearMonthDate,
                cancelCallBack = { datePickerDialog = null }
            ) { _, year, month, dayOfMonth ->
                val stringDate = "$dayOfMonth-$month-$year"
                binding.etNextFollowUpDate.text =
                    DateUtils.convertDateTimeToDate(
                        stringDate,
                        DateUtils.DATE_FORMAT_ddMMyyyy,
                        DateUtils.DATE_ddMMyyyy
                    )
                updateFollowUpDate(binding.etNextFollowUpDate.text.toString())
                datePickerDialog = null
            }
        }
    }

    private fun updateFollowUpDate(date: String) {
        if (date.isNotEmpty()) {
            viewModel.otherAssessmentDetails[AssessmentDefinedParams.NextFollowupDate] =
                DateUtils.convertDateTimeToDate(
                    date,
                    DateUtils.DATE_ddMMyyyy,
                    DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                    inUTC = true
                )
        }
    }

    private fun showNextFollowUpDate(map: Map<*, *>?) {
        map?.let {
            if (it.containsKey(RMNCH.PNC)) {
                val map = it[RMNCH.PNC] as Map<*, *>? ?: return
                if (map.containsKey(RMNCH.DateOfDelivery)) {
                    val dateOfDelivery = map[RMNCH.DateOfDelivery] as String
                    DateUtils.convertStringToDate(
                        dateOfDelivery,
                        DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ
                    )?.let { deliveryDate ->
                        RMNCH.calculateNextPNCVisitDate(deliveryDate)?.let { visitDate ->
                            binding.etNextFollowUpDate.text = DateUtils.getDateStringFromDate(
                                visitDate, DateUtils.DATE_ddMMyyyy
                            )
                            updateFollowUpDate(DateUtils.getDateStringFromDate(
                                visitDate, DateUtils.DATE_ddMMyyyy
                            ))
                            if (binding.etNextFollowUpDate.text.isNotEmpty()) {
                                binding.btnDone.isEnabled = true
                            }
                        }
                    }
                }
            }
        }
    }

    fun getCurrentAnsweredStatus(): Boolean {
        return viewModel.otherAssessmentDetails.isNotEmpty()
    }
}