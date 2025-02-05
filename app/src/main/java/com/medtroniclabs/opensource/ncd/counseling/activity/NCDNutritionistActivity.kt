package com.medtroniclabs.opensource.ncd.counseling.activity

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.setVisible
import com.medtroniclabs.opensource.appextensions.textOrHyphen
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.StringConverter
import com.medtroniclabs.opensource.databinding.ActivityNcdNutritionistBinding
import com.medtroniclabs.opensource.formgeneration.extension.capitalizeFirstChar
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.model.PatientListRespModel
import com.medtroniclabs.opensource.ncd.counseling.adapter.NCDNutritionAdapter
import com.medtroniclabs.opensource.ncd.counseling.adapter.NCDNutritionHistoryAdapter
import com.medtroniclabs.opensource.ncd.data.AssessmentResultModel
import com.medtroniclabs.opensource.ncd.data.NCDCounselingModel
import com.medtroniclabs.opensource.ncd.data.ResultModel
import com.medtroniclabs.opensource.ncd.counseling.utils.ValidationListener
import com.medtroniclabs.opensource.ncd.counseling.viewmodel.CounselingViewModel
import com.medtroniclabs.opensource.ncd.data.LifeStyleRequest
import com.medtroniclabs.opensource.ncd.data.LifeStyleResponse
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil
import com.medtroniclabs.opensource.ncd.medicalreview.viewmodel.NCDMedicalReviewCMRViewModel
import com.medtroniclabs.opensource.ncd.medicalreview.viewmodel.NCDMedicalReviewViewModel
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.MenuConstants
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.PatientDetailViewModel

class NCDNutritionistActivity : BaseActivity(), View.OnClickListener {

    private lateinit var binding: ActivityNcdNutritionistBinding

    private val viewModel: CounselingViewModel by viewModels()
    private val patientViewModel: PatientDetailViewModel by viewModels()
    private val mrViewModel: NCDMedicalReviewViewModel by viewModels()
    private val cmrViewModel: NCDMedicalReviewCMRViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        binding = ActivityNcdNutritionistBinding.inflate(layoutInflater)
        setMainContentView(
            binding.root,
            isToolbarVisible = true,
            homeAndBackVisibility = Pair(true, true),
            callback = {
                backHandelFlow()
            },
            callbackHome = {
                backHandelFlow()
            }
        )
        saveIntentValues()
        attachObservers()
        setClickListener()

        getPatientDetails()
    }

    private val onBackPressedCallback: OnBackPressedCallback =
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                backHandelFlow()
            }
        }

    private fun backHandelFlow() {
        if (binding.btnDone.isEnabled) {
            showErrorDialogue(
                getString(R.string.alert),
                getString(R.string.exit_reason_message),
                isNegativeButtonNeed = true
            ) {
                if (it) finish()
            }
        } else
            finish()
    }

    private fun getPatientDetails() {
        viewModel.memberReference?.let { memberId ->
            patientViewModel.getPatients(id = memberId, origin = MenuConstants.LIFESTYLE)
        }
    }

    private fun saveIntentValues() {
        intent?.extras?.let { bundle ->
            viewModel.apply {
                memberReference = bundle.getString(DefinedParams.FhirId)
                encounterReference = bundle.getString(NCDMRUtil.EncounterReference)
            }
        }
    }

    fun attachObservers() {
        cmrViewModel.lifeStyleResponse.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    resourceState.data?.let {
                        loadLifeStyleData(it)
                    }
                }
            }
        }
        mrViewModel.ncdMedicalReviewStaticLiveData.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    openDialog()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }
            }
        }
        viewModel.assessmentListLiveData.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    loadLifestyleList()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                    resourceState.message?.let { message ->
                        showErrorDialogue(message = message) {}
                    }
                }
            }
        }
        viewModel.updateAssessmentLiveData.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    resourceState?.data?.message?.let { message ->
                        showSuccessDialogue(
                            title = getString(R.string.lifestyle_assessment),
                            message = message
                        ) { refreshPage() }
                    } ?: run { refreshPage() }
                }

                ResourceState.ERROR -> {
                    hideLoading()
                    resourceState.message?.let { message ->
                        showErrorDialogue(message = message) {}
                    }
                }
            }
        }
        patientViewModel.patientDetailsLiveData.observe(this) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    resource.data?.let {
                        loadPatientInfo(it)
                    }
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }
            }
        }
    }

    private fun refreshPage() {
        getLifestyleList()
    }

    private fun loadLifeStyleData(it: java.util.ArrayList<LifeStyleResponse>) {
        it.forEachIndexed { _, lifeStyle ->
            binding.apply {
                when (lifeStyle.lifestyleType) {
                    NCDMRUtil.SMOKING -> tvSmokingStatus.text =
                        lifeStyle.lifestyleAnswer.textOrHyphen()

                    NCDMRUtil.ALCOHOL -> tvAlcoholStatus.text =
                        lifeStyle.lifestyleAnswer.textOrHyphen()

                    NCDMRUtil.DIET_NUTRITION -> tvDietNutrition.text =
                        lifeStyle.lifestyleAnswer.textOrHyphen()


                    NCDMRUtil.PHYSICAL_ACTIVITY -> {
                        val answer = lifeStyle.lifestyleAnswer.textOrHyphen() + " hrs/week"
                        tvPhysicalActivity.text = answer
                    }
                }
            }
        }
    }

    private fun loadPatientInfo(data: PatientListRespModel) {
        binding.apply {
            tvProgramId.text = data.programId.textOrHyphen()
            tvNationalId.text = data.identityValue.textOrHyphen()
            data.cvdRiskScoreDisplay?.let {
                tvPatientRisk.text = StringConverter.appendTexts(it, "", separator = "-")
                tvPatientRisk.setTextColor(
                    CommonUtils.cvdRiskColorCode(
                        data.cvdRiskScore?.toLong() ?: 0, this@NCDNutritionistActivity
                    )
                )
            }
            CommonUtils.getBMIFormattedText(this@NCDNutritionistActivity, data.bmi)
                .let { formattedBmi ->
                    tvBMI.text = formattedBmi.first?.toString().textOrHyphen()
                    formattedBmi.second?.let { bmiColor -> tvBMI.setTextColor(bmiColor) }
                }
        }
        data.firstName?.let {
            val text = StringConverter.appendTexts(firstText = it, data.lastName)
            setTitle(
                StringConverter.appendTexts(
                    firstText = text,
                    data.age.toString(),
                    data.gender?.capitalizeFirstChar(),
                    separator = "-"
                )
            )
        }
        viewModel.patientReference = data.patientId

        getPatientLifestyleData()
        getLifestyleList()
    }

    private fun setClickListener() {
        binding.apply {
            btnAdd.safeClickListener(this@NCDNutritionistActivity)
            btnDone.safeClickListener(this@NCDNutritionistActivity)
            tvViewHistory.safeClickListener(this@NCDNutritionistActivity)
        }
    }

    private fun openDialog() {
        val ncdLifestyleDialog = NCDLifestyleDialog.newInstance(
            viewModel.patientReference,
            viewModel.memberReference,
            viewModel.encounterReference
        ) {
            getLifestyleList()
        }
        ncdLifestyleDialog.show(supportFragmentManager, NCDLifestyleDialog.TAG)
    }

    private fun loadLifestyleList() {
        binding.btnDone.isEnabled = false
        val lifestyleList = viewModel.assessmentListLiveData.value?.data?.entityList
        if (lifestyleList.isNullOrEmpty()) {
            lifestyles(false)
            historyLifestyles(false)
        } else {
            val (list, historyList) = lifestyleList.partition { it.assessedBy.isNullOrBlank() }
            loadList(ArrayList(list))
            loadHistoryList(ArrayList(historyList))
        }
    }

    private fun loadHistoryList(lifestyleList: java.util.ArrayList<NCDCounselingModel>) {
        if (lifestyleList.isNotEmpty()) {
            val adapter = NCDNutritionHistoryAdapter()
            binding.rvHistoryLifestyleList.layoutManager =
                LinearLayoutManager(binding.rvHistoryLifestyleList.context ?: this)
            binding.rvHistoryLifestyleList.adapter = adapter
            adapter.submitData(lifestyleList)
            historyLifestyles(true)
        } else
            historyLifestyles(false)
    }

    private fun loadList(lifestyleHistoryList: java.util.ArrayList<NCDCounselingModel>) {
        if (lifestyleHistoryList.isNotEmpty()) {
            val adapter = NCDNutritionAdapter(validation)
            binding.rvPatientLifestyleList.layoutManager =
                LinearLayoutManager(binding.rvPatientLifestyleList.context ?: this)
            binding.rvPatientLifestyleList.adapter = adapter
            adapter.submitData(lifestyleHistoryList)
            lifestyles(true)
        } else
            lifestyles(false)
    }

    private val validation = object : ValidationListener {
        override fun validate() {
            binding.btnDone.isEnabled =
                viewModel.assessmentListLiveData.value?.data?.entityList?.firstOrNull { it.assessedBy.isNullOrBlank() && !it.lifestyleAssessment.isNullOrBlank() } != null
        }
    }

    private fun getLifestyleList() {
        val request = NCDCounselingModel(
            patientReference = viewModel.patientReference,
            memberReference = viewModel.memberReference,
            visitId = viewModel.encounterReference
        )
        viewModel.getAssessmentList(request, true)
    }

    private fun getPatientLifestyleData() {
        cmrViewModel.getNcdLifeStyleDetails(LifeStyleRequest(patientReference = viewModel.patientReference))
    }

    private fun lifestyles(visible: Boolean) {
        binding.rvPatientLifestyleList.setVisible(visible)
        binding.tvNoRecord.setVisible(!visible)
    }

    private fun historyLifestyles(visible: Boolean) {
        binding.rvHistoryLifestyleList.setVisible(visible)
        binding.tvNoHistoryRecord.setVisible(!visible)
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            binding.btnAdd.id -> {
                if (NCDMRUtil.isNCDMRMetaLoaded())
                    openDialog()
                else {
                    withNetworkAvailability(online = {
                        mrViewModel.getStaticMetaData()
                    })
                }
            }

            binding.btnDone.id -> updateLifestyles()

            binding.tvViewHistory.id -> {
                val show = binding.clLifeStyleHistory.visibility == View.GONE

                if (show) {
                    binding.tvViewHistory.text = getString(R.string.hide_history)
                    binding.clLifeStyleHistory.visible()
                } else {
                    binding.tvViewHistory.text = getString(R.string.view_history)
                    binding.clLifeStyleHistory.gone()
                }
            }
        }
    }

    private fun updateLifestyles() {
        val lifestyles =
            viewModel.assessmentListLiveData.value?.data?.entityList?.filter { it.assessedBy.isNullOrBlank() && !it.lifestyleAssessment.isNullOrBlank() }
        if (lifestyles.isNullOrEmpty())
            return
        else {
            val items = ArrayList<ResultModel>()
            lifestyles.forEach {
                items.add(
                    ResultModel(
                        id = it.id,
                        referredBy = it.referredBy,
                        lifestyleAssessment = it.lifestyleAssessment,
                        otherNote = it.otherNote
                    )
                )
            }
            val request = AssessmentResultModel(
                lifestyles = items, patientReference = viewModel.patientReference,
                memberReference = viewModel.memberReference,
                visitId = viewModel.encounterReference,
                patientVisitId = viewModel.encounterReference,
                assessedBy = NCDMRUtil.currentUserId(),
                assessedByDisplay = NCDMRUtil.getUserName(),
                assessedDate = DateUtils.getTodayDateDDMMYYYY()
            )
            viewModel.updateAssessment(request, true)
        }
    }
}