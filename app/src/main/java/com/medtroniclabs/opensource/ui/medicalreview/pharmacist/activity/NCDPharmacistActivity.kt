package com.medtroniclabs.opensource.ui.medicalreview.pharmacist.activity

import android.os.Bundle
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.View
import androidx.activity.viewModels
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.numberOrZero
import com.medtroniclabs.opensource.appextensions.textOrHyphen
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.StringConverter
import com.medtroniclabs.opensource.data.DispenseUpdatePrescriptionRequest
import com.medtroniclabs.opensource.data.DispenseUpdateRequest
import com.medtroniclabs.opensource.data.DispenseUpdateResponse
import com.medtroniclabs.opensource.databinding.ActivityNcdPharmacistBinding
import com.medtroniclabs.opensource.formgeneration.extension.capitalizeFirstChar
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.model.PatientListRespModel
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.MenuConstants
import com.medtroniclabs.opensource.ui.dialog.GeneralSuccessDialog
import com.medtroniclabs.opensource.ui.medicalreview.pharmacist.fragment.NCDPharmacistFragment
import com.medtroniclabs.opensource.ui.medicalreview.pharmacist.fragment.NCDPrescriptionHistoryDialogFragment
import com.medtroniclabs.opensource.ui.medicalreview.pharmacist.fragment.NCDQuantityDifferenceDialogueFragment
import com.medtroniclabs.opensource.ui.medicalreview.pharmacist.viewModel.NCDPharmacistViewModel
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.PatientDetailViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NCDPharmacistActivity : BaseActivity(), View.OnClickListener {

    private lateinit var binding: ActivityNcdPharmacistBinding
    private val patientDetailViewModel: PatientDetailViewModel by viewModels()
    private val viewModel: NCDPharmacistViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNcdPharmacistBinding.inflate(layoutInflater)
        setMainContentView(
            binding.root,
            true,
            getString(R.string.pharmacist_title),
            homeAndBackVisibility = Pair(false, true),
        )
        initView()
        getPatientDetails()
        attachObserver()
    }


    private fun initView() {
        viewModel.patientVisitId = intent.getStringExtra(NCDMRUtil.EncounterReference)
        binding.bottomView.btnDone.safeClickListener(this)
        binding.bottomView.btnCancel.safeClickListener(this)
        replaceFragmentInId<NCDPharmacistFragment>(binding.prescriptionRefillFragment.id)
    }


    private fun attachObserver() {

        viewModel.updatePrescriptionLiveData.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> showLoading()
                ResourceState.ERROR -> {
                    hideLoading()
                    resourceState.message?.let {
                        showErrorDialogue(getString(R.string.error), it) {}
                    }
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    resourceState.data.let { data ->
                        showDialogue(data)
                    }
                }
            }
        }

        patientDetailViewModel.patientDetailsLiveData.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> showLoading()
                ResourceState.ERROR -> hideLoading()
                ResourceState.SUCCESS -> {
                    hideLoading()
                    loadPatientInfo(resourceState.data)
                    withNetworkAvailability(online = {
                        viewModel.getPrescriptionDispenseList(
                            DispenseUpdateRequest(patientReference = patientDetailViewModel.getPatientId())
                        )
                    })
                    viewModel.patientReference = patientDetailViewModel.getPatientId()
                    viewModel.memberId = patientDetailViewModel.getPatientFHIRId()
                    viewModel.lastRefillVisitId = patientDetailViewModel.getLastRefillVisitId()
                }
            }
        }

        viewModel.prescriptionDispenseLiveData.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    resourceState.data?.let {
                        if (it.size > 0) {
                            binding.bottomView.btnDone.visible()
                        } else {
                            binding.bottomView.btnDone.visible()
                        }
                    } ?: kotlin.run {
                        binding.bottomView.btnDone.gone()
                    }
                }

                ResourceState.ERROR -> {
                    hideLoading()

                }
            }
        }

        viewModel.prescriptionDispenseHistoryLiveData.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                    resourceState?.message?.let { message ->
                        showErrorDialogue(getString(R.string.error), message) {}
                    }
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    resourceState.data?.let { historyList ->
                        NCDPrescriptionHistoryDialogFragment.newInstance(historyList)
                            .show(supportFragmentManager, NCDPrescriptionHistoryDialogFragment.TAG)
                    }
                }
            }
        }

    }

    private fun loadPatientInfo(data: PatientListRespModel?) {
        data?.let {
            binding.tvProgramId.text = it.programId.textOrHyphen()
            binding.tvNationalId.text = it.identityValue.textOrHyphen()
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
            data.prescribedDetails?.let { prescriberDetails ->
                val name = StringBuffer()
                prescriberDetails.firstName?.let { firstName ->
                    name.append(firstName)
                }
                prescriberDetails.lastName?.let { lastName ->
                    name.append(" $lastName")
                }
                binding.tvPrescriberName.text =
                    name.ifBlank { getString(R.string.separator_hyphen) }
                binding.tvPrescriberNumber.text = prescriberDetails.phoneNumber.textOrHyphen()

                prescriberDetails.lastRefillDate?.let { lastRefillDate ->
                    DateUtils.convertDateTimeToDate(
                        lastRefillDate,
                        DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                        DateUtils.DATE_FORMAT_ddMMMyyyy
                    ).let { formattedDate ->
                        val spannableString = SpannableString(formattedDate)
                        spannableString.setSpan(UnderlineSpan(), 0, formattedDate.length, 0)
                        binding.tvLastRefillDate.text = spannableString
                        binding.tvLastRefillDate.setTextAppearance(R.style.MR_Field_Style)
                        binding.tvLastRefillDate.setTextColor(this.getColor(R.color.cobalt_blue))
                        binding.tvLastRefillDate.safeClickListener {
                            if (!viewModel.lastRefillVisitId.isNullOrBlank() && !patientDetailViewModel.getPatientId()
                                    .isNullOrBlank()
                            ) {
                                withNetworkAvailability(online =
                                {
                                    viewModel.getDispensePrescriptionHistory(
                                        DispenseUpdateRequest(
                                            patientVisitId = viewModel.lastRefillVisitId,
                                            patientReference = patientDetailViewModel.getPatientId(),
                                            requestFrom = MenuConstants.DISPENSE
                                        )
                                    )
                                })
                            }
                        }
                    }
                }
            }
        }
    }


    private fun getPatientDetails() {
        intent?.let {
            patientDetailViewModel.origin = it.getStringExtra(DefinedParams.ORIGIN)
            it.getStringExtra(DefinedParams.FhirId)?.let { id ->
                patientDetailViewModel.getPatients(
                    id,
                    origin = patientDetailViewModel.origin?.lowercase()
                )
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.bottomView.btnDone.id -> {
                validateCountDifference()
            }

            binding.bottomView.btnCancel.id -> {
                finish()
            }
        }
    }

    private fun validateCountDifference() {
        viewModel.prescriptionDispenseLiveData.value?.data?.let { list ->
            val differedQuantityList =
                list.filter { it.prescriptionFilledDays.numberOrZero() < it.dispenseRemainingDays.numberOrZero() }
            if (differedQuantityList.isNotEmpty()) {
                NCDQuantityDifferenceDialogueFragment.newInstance()
                    .show(supportFragmentManager, NCDQuantityDifferenceDialogueFragment.TAG)
            } else {
                val filledValues =
                    list.filter { it.prescriptionFilledDays != null && it.prescriptionFilledDays != 0 }
                if (filledValues.isNotEmpty()) {
                    if ((!viewModel.patientVisitId.isNullOrBlank()
                                && !viewModel.patientReference.isNullOrBlank())
                        && !viewModel.memberId.isNullOrBlank()
                    ) {
                        withNetworkAvailability(online = {
                            viewModel.updateDispensePrescription(
                                patientVisitId = viewModel.patientVisitId,
                                patientReference = viewModel.patientReference,
                                memberId = viewModel.memberId,
                                request = getReqBody()
                            )
                        })
                    }
                } else {
                    showErrorDialogue(
                        getString(R.string.error),
                        getString(R.string.days_are_required)
                    ) {}
                }
            }
        }
    }

    private fun getReqBody(): List<DispenseUpdatePrescriptionRequest> {
        val prescriptionList = ArrayList<DispenseUpdatePrescriptionRequest>()
        viewModel.prescriptionDispenseLiveData.value?.data?.forEach {
            prescriptionList.add(
                DispenseUpdatePrescriptionRequest(
                    medicationName = it.medicationName,
                    dosageFrequencyName = it.dosageFrequencyName,
                    prescriptionId = it.prescriptionId,
                    instructionNote = it.instructionNote,
                    prescriptionFilledDays = it.prescriptionFilledDays,
                    reason = it.discontinuedReason
                )
            )
        }
        return prescriptionList
    }

    private fun showDialogue(data: DispenseUpdateResponse?) {
        if (data != null) {
            GeneralSuccessDialog.newInstance(
                title = getString(R.string.prescription),
                message = getString(R.string.prescription_dispensed_successfully),
                okayButton = getString(R.string.done)
            ) { finish() }.show(supportFragmentManager, GeneralSuccessDialog.TAG)
        }
    }
}