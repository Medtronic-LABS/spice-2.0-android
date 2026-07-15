package org.medtroniclabs.uhis.ui.patient

import android.os.Bundle
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.View
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.common.StringConverter
import org.medtroniclabs.uhis.data.registration.FillMedicineResponse
import org.medtroniclabs.uhis.data.registration.FillPrescriptionRequest
import org.medtroniclabs.uhis.data.registration.PatientDetailsModel
import org.medtroniclabs.uhis.data.registration.PatientPrescriptionModel
import org.medtroniclabs.uhis.databinding.ActivityPrescriptionRefillBinding
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.dialog.GeneralSuccessDialog
import org.medtroniclabs.uhis.ui.patient.fragment.PrescriptionHistoryDialogFragment
import org.medtroniclabs.uhis.ui.patient.fragment.PrescriptionRefillFragment
import org.medtroniclabs.uhis.ui.patient.fragment.QualifyPharmStockAuditResponseDialogue
import org.medtroniclabs.uhis.ui.patient.fragment.QuantityDifferenceDialogue
import org.medtroniclabs.uhis.ui.patient.viewmodel.PatientDetailViewModel
import org.medtroniclabs.uhis.ui.patient.viewmodel.PrescriptionRefillViewModel
import kotlin.getValue

@AndroidEntryPoint
class PrescriptionRefillActivity : BaseActivity(), View.OnClickListener {
    lateinit var binding: ActivityPrescriptionRefillBinding

    private val viewModel: PrescriptionRefillViewModel by viewModels()

    private val patientViewModel: PatientDetailViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrescriptionRefillBinding.inflate(layoutInflater)
        setMainContentView(binding.root, true, homeAndBackVisibility = Pair(true, null))
        initView()
        attachObserver()
        viewModel.patientTrackId?.let { patientTrackerId ->
            val tenantId = SecuredPreference.getTenantId()
            viewModel.tenantId = tenantId
            viewModel.getPatientRefillMedicationList(
                FillPrescriptionRequest(
                    patientTrackerId,
                    tenantId,
                ),
            )
        }
    }

    private fun attachObserver() {
        viewModel.fillPrescriptionUpdateRequest.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> showLoading()
                ResourceState.ERROR -> {
                    hideLoading()
                    resourceState.message?.let {
                        showErrorDialogue(getString(R.string.error), it) {}
                    }
                }
                ResourceState.SUCCESS -> {
                    resourceState.data.let { list ->
                        showDialogue(list)
                    }
                }
            }
        }

        patientViewModel.patientDetailsResponse.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> showLoading()
                ResourceState.ERROR -> hideLoading()
                ResourceState.SUCCESS -> {
                    loadPatientInfo(resourceState.data)
                }
            }
        }

        viewModel.patientRefillMedicationList.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }
                ResourceState.SUCCESS -> {
                    hideLoading()
                    resourceState.data?.let {
                        if (it.size > 0) {
                            binding.btnDone.visibility = View.VISIBLE
                        } else {
                            binding.btnDone.visibility = View.GONE
                        }
                    } ?: kotlin.run {
                        binding.btnDone.visibility = View.GONE
                    }
                }
                ResourceState.ERROR -> {
                    hideLoading()
                }
            }
        }

        viewModel.patientRefillHistoryList.observe(this) { resourceState ->
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
                        PrescriptionHistoryDialogFragment
                            .newInstance(historyList)
                            .show(supportFragmentManager, PrescriptionHistoryDialogFragment.TAG)
                    }
                }
            }
        }
    }

    private fun showDialogue(list: ArrayList<FillMedicineResponse>?) {
        if (!list.isNullOrEmpty()) {
            viewModel.shortageReasonMap = list
            viewModel.shortageReasonMap?.let {
                QualifyPharmStockAuditResponseDialogue
                    .newInstance()
                    .show(
                        supportFragmentManager,
                        QualifyPharmStockAuditResponseDialogue.TAG,
                    )
            }
        } else {
            GeneralSuccessDialog
                .newInstance(
                    title = getString(R.string.prescription),
                    message = getString(R.string.prescription_dispensed_successfully),
                    okayButton = getString(R.string.done),
                ) { }
                .show(supportFragmentManager, GeneralSuccessDialog.TAG)
        }
    }

    private fun loadPatientInfo(data: PatientDetailsModel?) {
        data?.let {
            binding.tvProgramId.text = it.programId?.toString() ?: "-"
            binding.tvNationalId.text = it.nationalId ?: "-"
            data.firstName?.let {
                val text = StringConverter.appendTexts(firstText = it, data.lastName)
                setTitle(
                    StringConverter.appendTexts(
                        firstText = text,
                        data.age?.toInt().toString(),
                        data.gender,
                        separator = "-",
                    ),
                )
            }
            data.prescriberDetails?.let { prescriberDetails ->
                val name = StringBuffer("")
                prescriberDetails.firstName?.let { firstName ->
                    name.append(firstName)
                }
                prescriberDetails.lastName?.let { lastName ->
                    name.append(" $lastName")
                }
                binding.tvPrescriberName.text =
                    name.ifBlank { getString(R.string.separator_hyphen) }
                prescriberDetails.phoneNumber?.let { prescriberNumber ->
                    binding.tvPrescriberNumber.text = prescriberNumber
                }
                prescriberDetails.lastRefilDate?.let { lastRefillDate ->
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
                                viewModel.getPrescriptionRefillHistory(
                                    this,
                                    PatientPrescriptionModel(
                                        patientTrackId = viewModel.patientTrackId,
                                        tenantId = viewModel.tenantId,
                                        lastRefillVisitId = viewModel.lastRefillVisitId,
                                    ),
                                )
                            }
                        }
                }
                prescriberDetails.lastRefillVisitId?.let { lastRefillID ->
                    viewModel.lastRefillVisitId = lastRefillID
                }
            }
        }
    }

    private fun initView() {
        viewModel.patientTrackId = intent.getLongExtra(IntentConstants.INTENT_PATIENT_ID, -1L)
        viewModel.patientVisitId = intent.getLongExtra(IntentConstants.INTENT_VISIT_ID, -1L)
        patientViewModel.patientId = viewModel.patientTrackId
        replaceFragmentInId<PrescriptionRefillFragment>(binding.prescriptionRefillFragment.id)
        binding.btnDone.safeClickListener(this)
        binding.btnCancel.safeClickListener(this)
        patientViewModel.getPatientDetails(this, false, true)
    }

    private inline fun <reified fragment : Fragment> replaceFragmentInId(
        id: Int? = null,
        bundle: Bundle? = null,
        tag: String? = null,
    ) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace<fragment>(id ?: binding.prescriptionRefillFragment.id, args = bundle, tag = tag)
        }
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.btnDone -> {
                validateCountDifference()
            }
            R.id.btnCancel -> {
                finish()
            }
        }
    }

    private fun validateCountDifference() {
        viewModel.patientRefillMedicationList.value?.data?.let { list ->
            val differedQuantityList =
                list.filter { it.remainingPrescriptionDays != it.prescriptionFilledDays }
            if (differedQuantityList.isNotEmpty()) {
                QuantityDifferenceDialogue
                    .newInstance()
                    .show(supportFragmentManager, QuantityDifferenceDialogue.TAG)
            } else {
                val filledValues =
                    list.filter { it.prescriptionFilledDays != null && it.prescriptionFilledDays != 0 }
                val validList = filledValues
                if (validList.isNotEmpty()) {
                    viewModel.patientVisitId?.let { patientVisitId ->
                        viewModel.patientTrackId?.let { patientId ->
                            viewModel.fillPrescriptionUpdate(
                                this,
                                patientId,
                                SecuredPreference.getTenantId(),
                                patientVisitId,
                                viewModel.getFillPrescriptionList(),
                            )
                        }
                    }
                } else {
                    showErrorDialogue(getString(R.string.error), getString(R.string.days_are_required)) {}
                }
            }
        }
    }
}
