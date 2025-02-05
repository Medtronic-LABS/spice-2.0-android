package com.medtroniclabs.opensource.ncd.counseling.activity

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.databinding.ActivityNcdCounselingBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.ncd.counseling.adapter.NCDCounselingAdapter
import com.medtroniclabs.opensource.ncd.data.NCDCounselingModel
import com.medtroniclabs.opensource.ncd.counseling.utils.CounselingInterface
import com.medtroniclabs.opensource.ncd.counseling.viewmodel.CounselingViewModel
import com.medtroniclabs.opensource.ncd.data.BadgeNotificationModel
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil
import com.medtroniclabs.opensource.ncd.medicalreview.viewmodel.NCDMedicalReviewViewModel
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseActivity

class NCDCounselingActivity : BaseActivity(), View.OnClickListener, CounselingInterface {

    private lateinit var binding: ActivityNcdCounselingBinding

    private val viewModel: CounselingViewModel by viewModels()
    private val mrViewModel: NCDMedicalReviewViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        binding = ActivityNcdCounselingBinding.inflate(layoutInflater)
        setMainContentView(
            binding.root,
            isToolbarVisible = true,
            title = getString(R.string.psychological_assessment),
            callback = {
                backHandelFlow()
            }
        )
        saveIntentValues()
        initializeView()
        setListeners()
        attachObserver()
        getCounselingList()
    }

    private val onBackPressedCallback: OnBackPressedCallback =
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                backHandelFlow()
            }
        }

    private fun saveIntentValues() {
        intent?.extras?.let { bundle ->
            viewModel.apply {
                patientReference = bundle.getString(NCDMRUtil.PATIENT_REFERENCE)
                memberReference = bundle.getString(NCDMRUtil.MEMBER_REFERENCE)
                encounterReference = bundle.getString(NCDMRUtil.VISIT_ID)
            }
        }
    }

    private fun initializeView() {
        binding.bottomSheet.btnNext.text = getString(R.string.submit)
        hideRecyclerView()
    }

    private fun setListeners() {
        binding.btnAdd.safeClickListener(this)
        binding.bottomSheet.apply {
            btnNext.safeClickListener(this@NCDCounselingActivity)
            btnBack.safeClickListener(this@NCDCounselingActivity)
        }
        binding.etClinicalNotes.addTextChangedListener {
            binding.btnAdd.isEnabled = !binding.etClinicalNotes.text.isNullOrBlank()
        }
    }

    private fun attachObserver() {
        viewModel.createAssessmentLiveData.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    finish()
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
                    clearBadgeNotification()
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
        viewModel.removeAssessmentLiveData.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    resourceState.data?.entity?.id?.let { removedElementID ->
                        removeCounseling(removedElementID)
                    }
                }

                ResourceState.ERROR -> {
                    hideLoading()
                    resourceState.message?.let { message ->
                        showErrorDialogue(message = message) {}
                    }
                }
            }
        }
    }

    private fun clearBadgeNotification() {
        mrViewModel.updateBadgeNotifications(
            BadgeNotificationModel(
                patientReference = viewModel.patientReference,
                menuName = NCDMRUtil.PsychologicalResults
            )
        )
    }

    private fun removeCounseling(removedID: String) {
        viewModel.assessmentListLiveData.value?.data?.entityList?.let { list ->
            list.removeIf { it.id == removedID }
            loadCounselingList()
        }
    }

    private fun loadCounselingList() {
        val counselingList = viewModel.assessmentListLiveData.value?.data?.entityList

        if (counselingList.isNullOrEmpty()) hideRecyclerView()
        else {
            val adapter = NCDCounselingAdapter(this)
            binding.rvList.layoutManager = LinearLayoutManager(binding.rvList.context ?: this)
            binding.rvList.adapter = adapter
            adapter.submitData(counselingList)
            showRecyclerView()
        }
    }

    private fun backHandelFlow() {
        if (viewModel.clinicianNotes.isNullOrEmpty()) closePage()
        else
            showErrorDialogue(
                getString(R.string.alert),
                getString(R.string.exit_reason_message),
                isNegativeButtonNeed = true
            ) {
                if (it) closePage()
            }
    }

    private fun closePage() {
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun showRecyclerView() {
        binding.rvList.visibility = View.VISIBLE
        binding.tvNoData.visibility = View.GONE
    }

    private fun hideRecyclerView() {
        binding.rvList.visibility = View.GONE
        binding.tvNoData.visibility = View.VISIBLE
    }

    override fun onClick(mView: View?) {
        when (mView?.id) {
            binding.btnAdd.id -> addClinicianNotes()

            binding.bottomSheet.btnNext.id -> {
                if (connectivityManager.isNetworkAvailable()) viewModel.createAssessment(
                    getCreateRequest(),
                    false
                )
                else showErrorDialogue(
                    getString(R.string.error), getString(R.string.no_internet_error), false
                ) {}
            }

            binding.bottomSheet.btnBack.id -> backHandelFlow()
        }
    }

    private fun addClinicianNotes() {
        if (viewModel.clinicianNotes.isNullOrEmpty())
            viewModel.clinicianNotes = arrayListOf()

        binding.etClinicalNotes.text?.let {
            if (it.isNotEmpty())
                viewModel.clinicianNotes?.add(it.toString())
        }

        binding.etClinicalNotes.text?.clear()

        refreshList()
    }

    private fun removeClinicianNotes(model: NCDCounselingModel) {
        viewModel.clinicianNotes?.removeIf { it == model.clinicianNotes?.get(0) }

        refreshList()
    }

    private fun refreshList() {
        viewModel.assessmentListLiveData.value?.data?.entityList?.apply {
            removeIf { it.id == null }

            viewModel.clinicianNotes?.forEach {
                add(
                    0, NCDCounselingModel(
                        clinicianNotes = arrayListOf(it),
                        referredBy = NCDMRUtil.currentUserId(),
                        referredByDisplay = NCDMRUtil.getUserName(),
                        referredDate = DateUtils.getTodayDateDDMMYYYY(),
                    )
                )
            }
        }
        binding.bottomSheet.btnNext.isEnabled = !viewModel.clinicianNotes.isNullOrEmpty()
        loadCounselingList()
    }

    override fun removeElement(model: NCDCounselingModel) {
        if (model.id == null) {
            removeClinicianNotes(model)
        } else {
            viewModel.removeAssessment(NCDCounselingModel(id = model.id), false)
        }
    }

    private fun getCreateRequest(): NCDCounselingModel {
        return with(viewModel) {
            NCDCounselingModel(
                patientReference = patientReference,
                memberReference = memberReference,
                visitId = encounterReference,
                patientVisitId = encounterReference,
                clinicianNotes = clinicianNotes,
                referredBy = NCDMRUtil.currentUserId(),
                referredByDisplay = NCDMRUtil.getUserName(),
                referredDate = DateUtils.getTodayDateDDMMYYYY(),
                isCounselor = counselor
            )
        }
    }

    private fun getCounselingList() {
        val request = NCDCounselingModel(
            patientReference = viewModel.patientReference,
            memberReference = viewModel.memberReference,
            visitId = viewModel.encounterReference,
            patientVisitId = viewModel.encounterReference
        )
        viewModel.getAssessmentList(request, false)
    }
}