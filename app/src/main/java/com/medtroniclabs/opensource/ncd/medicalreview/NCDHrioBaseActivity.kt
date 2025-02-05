package com.medtroniclabs.opensource.ncd.medicalreview

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.DefinedParams.Other
import com.medtroniclabs.opensource.databinding.ActivityNcdhrioBaseBinding
import com.medtroniclabs.opensource.formgeneration.extension.capitalizeFirstChar
import com.medtroniclabs.opensource.formgeneration.extension.safePopupMenuClickListener
import com.medtroniclabs.opensource.model.PatientListRespModel
import com.medtroniclabs.opensource.ncd.data.NCDPatientRemoveRequest
import com.medtroniclabs.opensource.ncd.medicalreview.fragment.NCDScheduleDialog
import com.medtroniclabs.opensource.ncd.medicalreview.viewmodel.HrioViewModel
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.dialog.GeneralSuccessDialog
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.MotherNeonateUtil.toYesNoOrDefault
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.PatientDetailViewModel
import com.medtroniclabs.opensource.ui.patientDelete.NCDDeleteConfirmationDialog
import com.medtroniclabs.opensource.ui.patientDelete.viewModel.NCDPatientDeleteViewModel
import com.medtroniclabs.opensource.ui.patientEdit.NCDPatientEditActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NCDHrioBaseActivity : BaseActivity() {
    private lateinit var binding: ActivityNcdhrioBaseBinding
    private val patientViewModel: PatientDetailViewModel by viewModels()
    private val patientDeleteViewModel: NCDPatientDeleteViewModel by viewModels()
    private val hrioViewModel: HrioViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNcdhrioBaseBinding.inflate(layoutInflater)
        setMainContentView(
            binding.root,
            true,
            getString(R.string.patient_details),
            homeAndBackVisibility = Pair(false, true),
            homeIcon = ContextCompat.getDrawable(this, R.drawable.ic_more_vertical)
        )
        init()
        attachObservers()
        showHideVerticalIcon(true)
    }

    fun init() {
        withNetworkAvailability(online = {
            getPatientId()?.let { id ->
                patientViewModel.getPatients(
                    id,
                    origin = getMenuOrigin()
                )
            }
        })
    }

    private fun showHideVerticalIcon(visibility: Boolean) {
        showVerticalMoreIcon(visibility) {
            onMoreIconClicked(it)
        }
    }

    private fun onMoreIconClicked(view: View) {
        val popupMenu = PopupMenu(this@NCDHrioBaseActivity, view)
        popupMenu.menuInflater.inflate(R.menu.ncd_menu_patient_edit, popupMenu.menu)
        popupMenu.menu.findItem(R.id.patient_delete).isVisible = true
        popupMenu.menu.findItem(R.id.schedule).isVisible =
            CommonUtils.canShowScheduleMenu()
        popupMenu.menu.findItem(R.id.transfer_patient).isVisible = false
        popupMenu.safePopupMenuClickListener(object :
            android.widget.PopupMenu.OnMenuItemClickListener,
            PopupMenu.OnMenuItemClickListener {
            override fun onMenuItemClick(menuItem: MenuItem): Boolean {
                onPatientEditMenuItemClick(menuItem.itemId)
                return true
            }
        })
        popupMenu.setForceShowIcon(true)
        popupMenu.show()
    }

    private fun onPatientEditMenuItemClick(itemId: Int) {
        when (itemId) {
            R.id.patient_delete -> {
                patientDeleteCreate()
            }

            R.id.patient_edit -> {
                val intent =
                    Intent(this@NCDHrioBaseActivity, NCDPatientEditActivity::class.java)
                intent.putExtra(NCDMRUtil.PATIENT_REFERENCE, patientViewModel.getPatientId())
                intent.putExtra(
                    NCDMRUtil.MEMBER_REFERENCE,
                    patientViewModel.getPatientFHIRId()
                )
                intent.putExtra(DefinedParams.ORIGIN, patientViewModel.origin)
                patientEditLauncher.launch(intent)
            }

            R.id.schedule -> {
                displayScheduleDialog()
            }
        }
    }

    private val patientEditLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK)
                init()
        }

    private fun displayScheduleDialog() {
        NCDScheduleDialog.newInstance(
            patientViewModel.getPatientId(),
            patientViewModel.getPatientFHIRId(),
            patientViewModel.getPatientVillageId()
        ).show(supportFragmentManager, NCDScheduleDialog.TAG)
    }

    private fun patientDeleteCreate() {
        patientViewModel.patientDetailsLiveData.value?.data?.let { model ->
            val deleteConfirmationDialog = NCDDeleteConfirmationDialog.newInstance(
                getString(R.string.alert),
                getString(
                    R.string.patient_delete_confirmation,
                    model.firstName,
                    model.lastName
                ),
                { _, reason, otherReason ->
                    val request = NCDPatientRemoveRequest(
                        patientId = model.patientId.toString(),
                        reason = reason,
                        otherReason = otherReason,
                        memberId = model.id
                    )
                    patientDeleteViewModel.ncdPatientRemove(request)
                },
                this,
                true,
                okayButton = getString(R.string.yes),
                cancelButton = getString(R.string.no)
            )
            deleteConfirmationDialog.show(
                supportFragmentManager,
                NCDDeleteConfirmationDialog.TAG
            )
        }
    }

    fun attachObservers() {
        patientViewModel.patientDetailsLiveData.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    resourceState.data?.let {
                        loadData(it)
                    }
                }

                ResourceState.ERROR -> {
                    hideLoading()
                    showError(true)
                }
            }
        }
        hrioViewModel.toTriggerPatientDetails.observe(this) {
            init()
        }
        patientDeleteViewModel.patientRemoveResponse.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.SUCCESS -> {
                    hideLoading()
                    GeneralSuccessDialog.newInstance(
                        title = getString(R.string.delete),
                        message = getString(R.string.patient_delete_message),
                        okayButton = getString(R.string.done)
                    ) { redirectToHome() }.show(supportFragmentManager, GeneralSuccessDialog.TAG)
                }

                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                    resourceState.message?.let {
                        showErrorDialogue(getString(R.string.error), it, false) {}
                    }
                }
            }
        }
    }

    private fun loadData(patientListRespModel: PatientListRespModel) {

        binding.patientName.tvKey.text = getString(R.string.name)
        binding.patientName.tvValue.text = patientListRespModel.name.takeUnless { it.isNullOrBlank() }?.capitalizeFirstChar() ?: getString(R.string.hyphen_symbol)

        binding.mobileNumber.tvKey.text = getString(R.string.mobile_number)
        binding.mobileNumber.tvValue.text = patientListRespModel.phoneNumber.takeUnless { it.isNullOrBlank() } ?: getString(R.string.hyphen_symbol)

        binding.mobileCategory.tvKey.text = getString(R.string.mobile_category)
        binding.mobileCategory.tvValue.text = patientListRespModel.phoneNumberCategory.takeUnless { it.isNullOrBlank() }?.capitalizeFirstChar() ?: getString(R.string.hyphen_symbol)

        binding.landmark.tvKey.text = getString(R.string.landmark)
        binding.landmark.tvValue.text = patientListRespModel.landmark.takeUnless { it.isNullOrBlank() }?.capitalizeFirstChar() ?: getString(R.string.hyphen_symbol)

        binding.occupation.tvKey.text = getString(R.string.occupation)
        binding.occupation.tvValue.text = patientListRespModel.occupation.takeUnless { it.isNullOrBlank() }?.capitalizeFirstChar() ?: getString(R.string.hyphen_symbol)

        binding.healthStatus.tvKey.text = getString(R.string.health_insurance_status)
        binding.healthStatus.tvValue.text = patientListRespModel.insuranceStatus.toYesNoOrDefault(
            getString(R.string.hyphen_symbol),
            getString(R.string.yes),
            getString(R.string.no)
        )

        binding.healthType.tvKey.text = getString(R.string.health_insurance_type)
        binding.healthType.tvValue.text =  patientListRespModel.insuranceType ?: getString(R.string.hyphen_symbol)
        binding.healthType.tvValue.text = if (patientListRespModel.insuranceType.equals(Other,true)) {
            "$Other${patientListRespModel.otherInsurance?.takeIf { it.isNotBlank() }?.let { " - $it" } ?: ""}"
        } else {
            patientListRespModel.insuranceType ?: getString(R.string.hyphen_symbol)
        }

        binding.healthId.tvKey.text = getString(R.string.health_insurance_Id)
        binding.healthId.tvValue.text = patientListRespModel.insuranceId?.capitalizeFirstChar() ?: getString(R.string.hyphen_symbol)

        binding.nextVisit.tvKey.text = getString(R.string.next_medical_review_date)
        binding.nextVisit.tvValue.text = patientListRespModel.nextMedicalReviewDate?.let {
            DateUtils.convertDateFormat(
                it,
                DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                DateUtils.DATE_FORMAT_ddMMMyyyy
            ).takeIf { it.isNotBlank() } ?: getString(R.string.hyphen_symbol)
        } ?: getString(R.string.hyphen_symbol)
    }

    private fun showError(isActivityClosed: Boolean = false) {
        showErrorDialogue(
            title = getString(R.string.alert),
            message = getString(R.string.something_went_wrong_try_later),
            positiveButtonName = getString(R.string.ok),
        ) { isPositiveResult ->
            if (isPositiveResult && isActivityClosed) {
                onBackPressPopStack()
            }
        }
    }

    private fun onBackPressPopStack() {
        this@NCDHrioBaseActivity.finish()
    }

    private fun getMenuOrigin(): String? {
        return intent.getStringExtra(DefinedParams.ORIGIN)
    }

    private fun getPatientId(): String? {
        return intent.getStringExtra(DefinedParams.FhirId)
    }
}