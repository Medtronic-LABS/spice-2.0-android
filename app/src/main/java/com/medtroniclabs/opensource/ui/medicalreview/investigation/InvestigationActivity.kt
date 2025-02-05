package com.medtroniclabs.opensource.ui.medicalreview.investigation

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import androidx.activity.viewModels
import androidx.core.widget.addTextChangedListener
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto
import com.medtroniclabs.opensource.databinding.ActivityInvestigationBinding
import com.medtroniclabs.opensource.model.medicalreview.InvestigationModel
import com.medtroniclabs.opensource.model.medicalreview.SearchLabTestResponse
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.DeleteReasonDialog
import com.medtroniclabs.opensource.ui.medicalreview.investigation.dialog.HBA1CNudgesDialog
import com.medtroniclabs.opensource.ui.medicalreview.investigation.dialog.LipidsNudgesDialog
import com.medtroniclabs.opensource.ui.medicalreview.investigation.dialog.MarkAsReviewedConfirmationDialog
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.PatientDetailViewModel

class InvestigationActivity : BaseActivity(), AdapterView.OnItemClickListener,
    InvestigationListener, View.OnClickListener {

    lateinit var binding: ActivityInvestigationBinding

    private val investigationViewModel: InvestigationViewModel by viewModels()

    private val patientViewModel: PatientDetailViewModel by viewModels()

    private lateinit var investigationGenerator: InvestigationGenerator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInvestigationBinding.inflate(layoutInflater)
        setMainContentView(
            binding.root,
            true,
            title = getString(R.string.investigation),
            callback = {
                setResult(RESULT_OK, intent)
                finish()
            })
        initView()
        setListeners()
        attachObserver()
        showLabTestNudge()
    }

    private fun attachObserver() {
        investigationViewModel.markAsReviewedLiveData.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }

                ResourceState.SUCCESS -> {
//                    patientViewModel.patientDetailsLiveData.value?.data?.let {
//                        if (!it.patientId.isNullOrBlank())
//                            investigationViewModel.getLabTestList(it)
//                    }
                    hideLoading()
                    finish()
                }
            }
        }
        investigationViewModel.labTestPredictionLiveData.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.SUCCESS -> {
                    resourceState.data?.let { showNudgesDialog(it) }
                }

                else -> {
                    //else block
                }
            }
        }
        investigationViewModel.investigationSearchResponseListLiveData.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {

                }

                ResourceState.ERROR -> {

                }

                ResourceState.SUCCESS -> {
                    resourceState.data?.let {
                        loadAdapter(it)
                    }
                }
            }
        }

        investigationViewModel.investigationListLiveData.observe(this) { investigationList ->
            showAdapterList(investigationList)
            enableDisableSubmitButton()
        }

        patientViewModel.patientDetailsLiveData.observe(this) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }

                ResourceState.SUCCESS -> {
                    resource.data?.let { data ->
                        if (data.gender != null){
                            investigationGenerator.setPatientGender(data.gender)
                        }
                        data.id?.let {
                            investigationViewModel.getLabTestList(data)
                        } ?: kotlin.run {
                            hideLoading()
                        }
                    } ?: kotlin.run {
                        hideLoading()
                    }
                }
            }
        }

        investigationViewModel.labTestListLiveData.observe(this) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    resource.data?.let {
                        investigationViewModel.addExistingLabTestListToUI(it)
                    }
                }
            }
        }

        investigationViewModel.createLabTestLiveData.observe(this) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                    showErrorDialogue(
                        title = getString(R.string.error),
                        message = resource.message
                            ?: getString(R.string.something_went_wrong_try_later),
                        positiveButtonName = getString(R.string.ok)
                    ) {}
                }

                ResourceState.SUCCESS -> {
                    resource.data?.let { map ->
                        hideLoading()
                        val intent = Intent()
                        if (map.containsKey(DefinedParams.EncounterId)) {
                            val value = map[DefinedParams.EncounterId]
                            if (value is String) {
                                intent.putExtra(DefinedParams.EncounterId, value)
                            }
                        }
                        setResult(RESULT_OK, intent)
                        finish()
                    } ?: kotlin.run {
                        hideLoading()
                    }
                }
            }
        }

        investigationViewModel.removeLabTestLiveData.observe(this) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    resource.data?.let { map ->
                        if (map.containsKey(DefinedParams.ID)) {
                            val id = map[DefinedParams.ID]
                            if (id is String) {
                                investigationViewModel.removeInvestigationByID(id)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showLabTestNudge() {
        investigationViewModel.getLabTestNudgeList()
    }

    private fun showNudgesDialog(data: HashMap<String, Any>) {
        val hasHbA1c = data.containsKey(NCDMRUtil.HbA1c) && data[NCDMRUtil.HbA1c] != null
        val hasLipidProfile =
            data.containsKey(NCDMRUtil.LipidProfile) && data[NCDMRUtil.LipidProfile] != null
        val hasRenalFunctionTest =
            data.containsKey(NCDMRUtil.RenalFunctionTest) && data[NCDMRUtil.RenalFunctionTest] != null
        if (hasHbA1c)
            showHBA1CDialog()
        else if (hasLipidProfile || hasRenalFunctionTest)
            showLipidsDialog()
    }

    private fun showHBA1CDialog() {
        HBA1CNudgesDialog.newInstance { isClosed ->
            if (isClosed) {
                investigationViewModel.labTestPredictionLiveData.value?.data?.let {data ->
                    (data[NCDMRUtil.RenalFunctionTest] ?: data[NCDMRUtil.LipidProfile])?.let {
                        showLipidsDialog()
                    }
                }
            }
        }.show(supportFragmentManager, HBA1CNudgesDialog.TAG)
    }

    private fun showLipidsDialog() {
        LipidsNudgesDialog.newInstance().show(supportFragmentManager, LipidsNudgesDialog.TAG)
    }

    private fun enableDisableSubmitButton() {
        investigationViewModel.investigationListLiveData.value?.let { investigationList ->
            binding.btnSubmit.isEnabled = investigationList.any { it.id == null || (it.resultHashMap != null && it.resultHashMap!!.size > 0) }
        }
    }

    private fun showAdapterList(investigationList: ArrayList<InvestigationModel>) {
        binding.llInvestigationHolder.removeAllViews()
        if (investigationList.size > 0) {
            binding.llInvestigationHolder.visible()
            binding.tvNoInvestigationDataFound.gone()
            investigationGenerator.populateViews(investigationList, false)
        } else {
            binding.llInvestigationHolder.gone()
            binding.tvNoInvestigationDataFound.visible()
        }

    }

    private fun loadAdapter(data: ArrayList<SearchLabTestResponse>) {
        val adapter = InvestigationSearchAdapter(binding.root.context)
        adapter.setData(data)
        binding.searchView.setAdapter(adapter)
        binding.searchView.showDropDown()
    }

    private fun setListeners() {
        binding.searchView.addTextChangedListener {
            if (it.isNullOrEmpty()) {
                // default showing all medicines
            } else {
                investigationViewModel.searchInvestigationByName(it.toString())
            }
        }
        binding.searchView.onItemClickListener = this
        binding.btnSubmit.setOnClickListener(this)
    }

    private fun initView() {
        investigationViewModel.patientReference = intent.getStringExtra(DefinedParams.PatientReference)
        investigationViewModel.patientId = intent.getStringExtra(DefinedParams.PatientId)
        investigationViewModel.encounterId = intent.getStringExtra(DefinedParams.EncounterId)
        investigationViewModel.visitId = intent.getStringExtra(NCDMRUtil.EncounterReference)
        investigationViewModel.origin = intent.getStringExtra(DefinedParams.ORIGIN)
        investigationGenerator = InvestigationGenerator(
            this@InvestigationActivity,
            binding.llInvestigationHolder,
            binding.nestedScrollView,
            translate = SecuredPreference.getIsTranslationEnabled(),
            this
        )
        investigationViewModel.patientId?.let { patientId ->
            patientViewModel.getPatients(patientId, origin = investigationViewModel.origin)
        }
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        investigationViewModel.investigationSearchResponseListLiveData.value?.let { resourceState ->
            resourceState.data?.let { investigationList ->
                val investigationResponse = investigationList[position]
                investigationViewModel.addInvestigationModelToUI(investigationResponse)
            }
        }
        binding.searchView.setText("")
    }

    override fun removeInvestigation(investigation: InvestigationModel) {
        val dialog = DeleteReasonDialog.newInstance(
            this,
            getString(R.string.confirmation),
            true,
            Pair(getString(R.string.ok), getString(R.string.cancel)),
            showComment = false,
            callback = { isPositiveResult, _ ->
                if (isPositiveResult) {
                    investigationViewModel.removeInvestigationModel(investigation)
                }
            },
            message = Pair(getString(R.string.delete_confirmation), null)
        )
        dialog.show(supportFragmentManager, DeleteReasonDialog.TAG)
    }

    override fun checkValidation() {
        enableDisableSubmitButton()
    }

    override fun markAsReviewed(id: String?, comments: String?) {
        val dialog = supportFragmentManager.findFragmentByTag(MarkAsReviewedConfirmationDialog.TAG)
        if (dialog == null) {
            val markAsReviewedConfirmationDialog =
                MarkAsReviewedConfirmationDialog.newInstance { userConfirmed ->
                    if (userConfirmed) {
                        val requestMap = HashMap<String, Any>()
                        requestMap[DefinedParams.Provenance] = ProvanceDto()
                        id?.let { requestMap[DefinedParams.ID] = it }
                        comments?.let { requestMap[DefinedParams.Comments] = it }
                        investigationViewModel.markAsReviewed(requestMap)
                    }
                }
            markAsReviewedConfirmationDialog.show(
                supportFragmentManager,
                MarkAsReviewedConfirmationDialog.TAG
            )
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.btnSubmit.id -> {
                if (investigationGenerator.onValidateInput(false)) {
                    patientViewModel.patientDetailsLiveData.value?.data?.let { data ->
                        investigationViewModel.createLabTest(
                            geyPayloadForLabTest(investigationGenerator.getResultFromInvestigation()),
                            data
                        )
                    }
                } else {
                    investigationGenerator.getResultFromInvestigation()?.let {
                        binding.llInvestigationHolder.removeAllViews()
                        investigationGenerator.populateViews(ArrayList(it), false)
                    }
                }
            }
        }
    }


    private fun geyPayloadForLabTest(resultFromInvestigation: List<InvestigationModel>?): List<InvestigationModel>? {
        val list = resultFromInvestigation?.filter { it.id == null || (it.resultHashMap != null && it.resultHashMap!!.size > 0) }
        return list
    }

}