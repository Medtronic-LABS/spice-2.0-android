package org.medtroniclabs.uhis.ui.medicalreview.pharmacist.activity

import android.os.Bundle
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.View
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.numberOrZero
import org.medtroniclabs.uhis.appextensions.textOrHyphen
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.StringConverter
import org.medtroniclabs.uhis.data.DispenseUpdatePrescriptionRequest
import org.medtroniclabs.uhis.data.DispenseUpdateRequest
import org.medtroniclabs.uhis.data.DispenseUpdateResponse
import org.medtroniclabs.uhis.databinding.ActivityNcdPharmacistBinding
import org.medtroniclabs.uhis.formgeneration.extension.capitalizeFirstChar
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.model.PatientListRespModel
import org.medtroniclabs.uhis.ncd.medicalreview.NCDMRUtil
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.MenuConstants
import org.medtroniclabs.uhis.ui.dialog.GeneralSuccessDialog
import org.medtroniclabs.uhis.ui.medicalreview.pharmacist.fragment.NCDPharmacistFragment
import org.medtroniclabs.uhis.ui.medicalreview.pharmacist.fragment.NCDPrescriptionHistoryDialogFragment
import org.medtroniclabs.uhis.ui.medicalreview.pharmacist.fragment.NCDQuantityDifferenceDialogueFragment
import org.medtroniclabs.uhis.ui.medicalreview.pharmacist.viewModel.NCDPharmacistViewModel
import org.medtroniclabs.uhis.ui.mypatients.viewmodel.PatientDetailViewModel

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
                            DispenseUpdateRequest(patientReference = patientDetailViewModel.getPatientFHIRId()),
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
                        NCDPrescriptionHistoryDialogFragment
                            .newInstance(historyList)
                            .show(supportFragmentManager, NCDPrescriptionHistoryDialogFragment.TAG)
                    }
                }
            }
        }
    }

    private fun loadPatientInfo(data: PatientListRespModel?) {
        data?.let {
            binding.tvProgramId.text = it.programId.textOrHyphen()
            binding.tvNationalId.text = (it.identityValue ?: it.nationalID?.toString()).textOrHyphen()
            val displayName = it.name?.takeIf { name -> name.isNotBlank() }
                ?: StringConverter.appendTexts(firstText = it.firstName.orEmpty(), it.lastName)
            if (displayName.isNotBlank()) {
                setTitle(
                    StringConverter.appendTexts(
                        firstText = displayName.capitalizeFirstChar(),
                        it.age?.toString(),
                        it.gender?.capitalizeFirstChar(),
                        separator = "-",
                    ),
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
                    DateUtils
                        .convertDateTimeToDate(
                            lastRefillDate,
                            DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                            DateUtils.DATE_FORMAT_ddMMMyyyy,
                        ).let { formattedDate ->
                            val spannableString = SpannableString(formattedDate)
                            spannableString.setSpan(UnderlineSpan(), 0, formattedDate.length, 0)
                            binding.tvLastRefillDate.text = spannableString
                            binding.tvLastRefillDate.setTextAppearance(R.style.MR_Field_Style)
                            binding.tvLastRefillDate.setTextColor(this.getColor(R.color.cobalt_blue))
                            binding.tvLastRefillDate.safeClickListener {
                                if (!viewModel.lastRefillVisitId.isNullOrBlank() &&
                                    !patientDetailViewModel
                                        .getPatientId()
                                        .isNullOrBlank()
                                ) {
                                    withNetworkAvailability(
                                        online =
                                            {
                                                viewModel.getDispensePrescriptionHistory(
                                                    DispenseUpdateRequest(
                                                        patientVisitId = viewModel.lastRefillVisitId,
                                                        patientReference = patientDetailViewModel.getPatientId(),
                                                        requestFrom = MenuConstants.DISPENSE,
                                                    ),
                                                )
                                            },
                                    )
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
                    origin = patientDetailViewModel.origin?.lowercase(),
                    patientId = it.getStringExtra(DefinedParams.PatientId),
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
                NCDQuantityDifferenceDialogueFragment
                    .newInstance()
                    .show(supportFragmentManager, NCDQuantityDifferenceDialogueFragment.TAG)
            } else {
                val filledValues =
                    list.filter { it.prescriptionFilledDays != null && it.prescriptionFilledDays != 0 }
                if (filledValues.isNotEmpty()) {
                    if ((
                            !viewModel.patientVisitId.isNullOrBlank() &&
                                !viewModel.patientReference.isNullOrBlank()
                        ) &&
                        !viewModel.memberId.isNullOrBlank()
                    ) {
                        withNetworkAvailability(online = {
                            viewModel.updateDispensePrescription(
                                patientVisitId = viewModel.patientVisitId,
                                patientReference = viewModel.patientReference,
                                memberId = viewModel.memberId,
                                request = getReqBody(),
                            )
                        })
                    }
                } else {
                    showErrorDialogue(
                        getString(R.string.error),
                        getString(R.string.days_are_required),
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
                    reason = it.discontinuedReason,
                ),
            )
        }
        return prescriptionList
    }

    private fun showDialogue(data: DispenseUpdateResponse?) {
        if (data != null) {
            GeneralSuccessDialog
                .newInstance(
                    title = getString(R.string.prescription),
                    message = getString(R.string.prescription_dispensed_successfully),
                    okayButton = getString(R.string.done),
                ) { finish() }
                .show(supportFragmentManager, GeneralSuccessDialog.TAG)
        }
    }
}
