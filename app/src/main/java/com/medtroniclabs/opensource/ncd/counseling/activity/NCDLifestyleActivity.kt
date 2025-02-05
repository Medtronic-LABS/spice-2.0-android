package com.medtroniclabs.opensource.ncd.counseling.activity

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.data.model.ChipViewItemModel
import com.medtroniclabs.opensource.databinding.ActivityNcdLifestyleBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.ncd.counseling.adapter.NCDLifestyleAdapter
import com.medtroniclabs.opensource.ncd.data.NCDCounselingModel
import com.medtroniclabs.opensource.ncd.counseling.utils.CounselingInterface
import com.medtroniclabs.opensource.ncd.counseling.viewmodel.CounselingViewModel
import com.medtroniclabs.opensource.ncd.data.BadgeNotificationModel
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil
import com.medtroniclabs.opensource.ncd.medicalreview.viewmodel.NCDMedicalReviewViewModel
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.TagListCustomView

class NCDLifestyleActivity : BaseActivity(), View.OnClickListener, CounselingInterface {

    private lateinit var binding: ActivityNcdLifestyleBinding

    private val viewModel: CounselingViewModel by viewModels()
    private val mrViewModel: NCDMedicalReviewViewModel by viewModels()
    private lateinit var tagListCustomView: TagListCustomView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        binding = ActivityNcdLifestyleBinding.inflate(layoutInflater)
        setMainContentView(
            binding.root,
            isToolbarVisible = true,
            title = getString(R.string.lifestyle_management),
            callback = {
                backHandelFlow()
            }
        )
        saveIntentValues()
        initializeView()
        setListeners()
        attachObserver()
        getLifestyleList()
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

        tagListCustomView = TagListCustomView(this, binding.chipGroup) { _, isEmpty, _ ->
            viewModel.lifestyles =
                tagListCustomView.getSelectedTags().map { it.name }.ifEmpty { null }
            viewModel.cultureLifestyles =
                tagListCustomView.getSelectedTags().mapNotNull { it.cultureValue }.ifEmpty { null }
            binding.apply {
                etClinicalNotes.isEnabled = !isEmpty
                bottomSheet.btnNext.isEnabled = !isEmpty
            }
            updateView(isEmpty)
        }
        viewModel.getChips()

        hideRecyclerView()
    }

    private fun updateView(isEmpty: Boolean) {
        viewModel.assessmentListLiveData.value?.data?.entityList?.apply {
            removeIf { it.id == null }
            if (!isEmpty) add(0, getCreateRequest())
        }
        loadLifestyleList()
    }

    private fun setListeners() {
        binding.bottomSheet.apply {
            btnNext.safeClickListener(this@NCDLifestyleActivity)
            btnBack.safeClickListener(this@NCDLifestyleActivity)
        }

        binding.etClinicalNotes.addTextChangedListener { notes ->
            viewModel.clinicianNote = if (notes.isNullOrBlank()) null else notes.toString()
        }
    }

    private fun attachObserver() {
        viewModel.getChipItems.observe(this) {
            val complaintList = it.map { item ->
                ChipViewItemModel(
                    id = item.id,
                    name = item.name,
                    cultureValue = item.displayValue,
                    type = item.type,
                    value = item.value
                )
            } as ArrayList<ChipViewItemModel>
            tagListCustomView.addChipItemList(complaintList)
        }
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
        viewModel.removeAssessmentLiveData.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    resourceState.data?.entity?.id?.let { removedElementID ->
                        removeLifestyle(removedElementID)
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
                menuName = NCDMRUtil.LifestyleResults
            )
        )
    }

    private fun removeLifestyle(removedID: String) {
        viewModel.assessmentListLiveData.value?.data?.entityList?.let { list ->
            list.removeIf { it.id == removedID }
            loadLifestyleList()
        }
    }

    private fun loadLifestyleList() {
        val lifestyleList = viewModel.assessmentListLiveData.value?.data?.entityList
        if (lifestyleList.isNullOrEmpty()) hideRecyclerView()
        else {
            val adapter = NCDLifestyleAdapter(this)
            binding.rvList.layoutManager = LinearLayoutManager(binding.rvList.context ?: this)
            binding.rvList.adapter = adapter
            adapter.submitData(lifestyleList)
            showRecyclerView()
        }
    }

    private fun backHandelFlow() {
        if (viewModel.lifestyles.isNullOrEmpty()) closePage()
        else showErrorDialogue(
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
            binding.bottomSheet.btnNext.id -> {
                if (connectivityManager.isNetworkAvailable()) viewModel.createAssessment(
                    getCreateRequest(),
                    true
                )
                else showErrorDialogue(
                    getString(R.string.error), getString(R.string.no_internet_error), false
                ) {}
            }

            binding.bottomSheet.btnBack.id -> backHandelFlow()
        }
    }

    override fun removeElement(model: NCDCounselingModel) {
        if (model.id == null) {
            tagListCustomView.clearSelection()
            binding.etClinicalNotes.text?.clear()
        } else {
            viewModel.removeAssessment(NCDCounselingModel(id = model.id), true)
        }
    }

    private fun getCreateRequest(): NCDCounselingModel {
        return with(viewModel) {
            NCDCounselingModel(
                patientReference = patientReference,
                memberReference = memberReference,
                visitId = encounterReference,
                lifestyles = lifestyles,
                cultureLifestyles = cultureLifestyles,
                clinicianNote = clinicianNote,
                referredBy = NCDMRUtil.currentUserId(),
                referredByDisplay = NCDMRUtil.getUserName(),
                referredDate = DateUtils.getTodayDateDDMMYYYY(),
                isNutritionist = nutritionist
            )
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
}