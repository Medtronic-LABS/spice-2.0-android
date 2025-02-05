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
import com.medtroniclabs.opensource.databinding.ActivityNcdCounselorBinding
import com.medtroniclabs.opensource.formgeneration.extension.capitalizeFirstChar
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.model.PatientListRespModel
import com.medtroniclabs.opensource.ncd.counseling.adapter.NCDCounselorAdapter
import com.medtroniclabs.opensource.ncd.counseling.adapter.NCDCounselorHistoryAdapter
import com.medtroniclabs.opensource.ncd.data.AssessmentResultModel
import com.medtroniclabs.opensource.ncd.data.NCDCounselingModel
import com.medtroniclabs.opensource.ncd.data.ResultModel
import com.medtroniclabs.opensource.ncd.counseling.utils.ValidationListener
import com.medtroniclabs.opensource.ncd.counseling.viewmodel.CounselingViewModel
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.MenuConstants
import com.medtroniclabs.opensource.ui.dialog.GeneralSuccessDialog
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.PatientDetailViewModel

class NCDCounselorActivity : BaseActivity(), View.OnClickListener {

    private lateinit var binding: ActivityNcdCounselorBinding

    private val viewModel: CounselingViewModel by viewModels()
    private val patientViewModel: PatientDetailViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        binding = ActivityNcdCounselorBinding.inflate(layoutInflater)
        setMainContentView(binding.root,
            isToolbarVisible = true,
            homeAndBackVisibility = Pair(true, true),
            callback = {
                backHandelFlow()
            },
            callbackHome = {
                backHandelFlow()
            })
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
            patientViewModel.getPatients(id = memberId, origin = MenuConstants.PSYCHOLOGICAL)
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
        viewModel.updateAssessmentLiveData.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    resourceState?.data?.message?.let { message ->
                        showSuccessDialogue(
                            title = getString(R.string.psychological_assessment),
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
        viewModel.assessmentListLiveData.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    loadCounselingList()
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
        getCounselingList()
    }

    private fun loadPatientInfo(data: PatientListRespModel) {
        binding.apply {
            tvProgramId.text = data.programId.textOrHyphen()
            tvNationalId.text = data.identityValue.textOrHyphen()
            data.cvdRiskScoreDisplay?.let {
                tvPatientRisk.text = StringConverter.appendTexts(it, "", separator = "-")
                tvPatientRisk.setTextColor(
                    CommonUtils.cvdRiskColorCode(
                        data.cvdRiskScore?.toLong() ?: 0, this@NCDCounselorActivity
                    )
                )
            }
            CommonUtils.getBMIFormattedText(this@NCDCounselorActivity, data.bmi)
                .let { formattedBmi ->
                    tvBMI.text = formattedBmi.first?.toString().textOrHyphen()
                    formattedBmi.second?.let { bmiColor -> tvBMI.setTextColor(bmiColor) }
                }
            data.suicidalIdeation?.let { si ->
                tvSuicidal.text = si.textOrHyphen().capitalizeFirstChar()
                val color = if (si.equals(
                        DefinedParams.yes,
                        true
                    )
                ) R.color.medium_high_risk_color else R.color.black
                tvSuicidal.setTextColor(getColor(color))
            }
            data.cageAid?.toDoubleOrNull()?.toInt()?.let { cageId ->
                if (cageId > 0) {
                    tvCageAid.text = cageId.toString()
                    tvCageAid.setTextColor(getColor(R.color.medium_high_risk_color))
                }
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

        getCounselingList()
    }


    private fun setClickListener() {
        binding.apply {
            btnAdd.safeClickListener(this@NCDCounselorActivity)
            btnDone.safeClickListener(this@NCDCounselorActivity)
            tvViewHistory.safeClickListener(this@NCDCounselorActivity)
        }
    }

    private fun openDialog() {
        val ncdCounselingDialog = NCDCounselingDialog.newInstance(
            viewModel.patientReference,
            viewModel.memberReference,
            viewModel.encounterReference
        ) { response ->
            if (response != null)
                GeneralSuccessDialog.newInstance(
                    title = response.first,
                    message = response.second,
                    okayButton = getString(R.string.done)
                ) { getCounselingList() }.show(supportFragmentManager, GeneralSuccessDialog.TAG)
        }
        ncdCounselingDialog.show(supportFragmentManager, NCDCounselingDialog.TAG)
    }

    private fun loadCounselingList() {
        binding.btnDone.isEnabled = false
        val counselingList = viewModel.assessmentListLiveData.value?.data?.entityList
        if (counselingList.isNullOrEmpty()) {
            counseling(false)
            historyCounseling(false)
        } else {
            val (list, historyList) = counselingList.partition { it.assessedBy.isNullOrBlank() }
            loadList(ArrayList(list))
            loadHistoryList(ArrayList(historyList))
        }
    }

    private fun loadHistoryList(historyList: ArrayList<NCDCounselingModel>) {
        if (historyList.isNotEmpty()) {
            val historyAdapter = NCDCounselorHistoryAdapter()
            binding.rvHistoryPsychological.layoutManager =
                LinearLayoutManager(binding.rvHistoryPsychological.context ?: this)
            binding.rvHistoryPsychological.adapter = historyAdapter
            historyAdapter.submitData(historyList)
            historyCounseling(true)
        } else
            historyCounseling(false)
    }

    private fun loadList(list: ArrayList<NCDCounselingModel>) {
        if (list.isNotEmpty()) {
            val adapter = NCDCounselorAdapter(validation)
            binding.rvCounselorAssessmentList.layoutManager =
                LinearLayoutManager(binding.rvCounselorAssessmentList.context ?: this)
            binding.rvCounselorAssessmentList.adapter = adapter
            adapter.submitData(list)
            counseling(true)
        } else
            counseling(false)
    }

    private val validation = object : ValidationListener {
        override fun validate() {
            binding.btnDone.isEnabled =
                viewModel.assessmentListLiveData.value?.data?.entityList?.firstOrNull { it.assessedBy.isNullOrBlank() && !it.counselorAssessment.isNullOrBlank() } != null
        }
    }

    private fun getCounselingList() {
        val request = NCDCounselingModel(
            patientReference = viewModel.patientReference,
            memberReference = viewModel.memberReference,
            visitId = viewModel.encounterReference
        )
        viewModel.getAssessmentList(request, false)
    }

    private fun counseling(visible: Boolean) {
        binding.rvCounselorAssessmentList.setVisible(visible)
        binding.tvNoRecord.setVisible(!visible)
    }

    private fun historyCounseling(visible: Boolean) {
        binding.rvHistoryPsychological.setVisible(visible)
        binding.tvNoHistoryRecord.setVisible(!visible)
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            binding.btnAdd.id -> openDialog()

            binding.btnDone.id -> updateCounseling()

            binding.tvViewHistory.id -> {
                val show = binding.clPsychologicalHistory.visibility == View.GONE

                if (show) {
                    binding.tvViewHistory.setText(getString(R.string.hide_history))
                    binding.clPsychologicalHistory.visible()
                } else {
                    binding.tvViewHistory.setText(getString(R.string.view_history))
                    binding.clPsychologicalHistory.gone()
                }
            }
        }
    }

    private fun updateCounseling() {
        val counselingList =
            viewModel.assessmentListLiveData.value?.data?.entityList?.filter { it.assessedBy.isNullOrBlank() && !it.counselorAssessment.isNullOrBlank() }
        if (counselingList.isNullOrEmpty())
            return
        else {
            val items = ArrayList<ResultModel>()
            counselingList.forEach {
                items.add(
                    ResultModel(
                        id = it.id,
                        counselorAssessment = it.counselorAssessment
                    )
                )
            }
            val request = AssessmentResultModel(
                counselorAssessments = items,
                patientReference = viewModel.patientReference,
                memberReference = viewModel.memberReference,
                visitId = viewModel.encounterReference,
                patientVisitId = viewModel.encounterReference,
                assessedBy = NCDMRUtil.currentUserId(),
                assessedByDisplay = NCDMRUtil.getUserName(),
                assessedDate = DateUtils.getTodayDateDDMMYYYY()
            )
            viewModel.updateAssessment(request, false)
        }
    }
}