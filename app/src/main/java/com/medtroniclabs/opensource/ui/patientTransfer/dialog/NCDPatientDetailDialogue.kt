package com.medtroniclabs.opensource.ui.patientTransfer.dialog

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.textOrHyphen
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.ncd.data.NCDPatientTransferNotificationCountRequest
import com.medtroniclabs.opensource.databinding.PatientDetailDialogueBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.landing.viewmodel.LandingViewModel
import com.medtroniclabs.opensource.ncd.data.PatientTransfer
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class NCDPatientDetailDialogue : DialogFragment() {

    private lateinit var binding: PatientDetailDialogueBinding

    private val viewModel: LandingViewModel by viewModels()

    companion object {
        val TAG = "NCDPatientDetailDialogue"
        fun newInstance(patientID: Long): NCDPatientDetailDialogue {
            val bundle = Bundle()
            bundle.putLong(DefinedParams.ID, patientID)
            val fragment = NCDPatientDetailDialogue()
            fragment.arguments = bundle
            return fragment
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = PatientDetailDialogueBinding.inflate(inflater, container, false)
        val window: Window? = dialog?.window
        window?.setGravity(Gravity.CENTER)
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = false
        viewModel.transferPatientViewId = arguments?.getLong(DefinedParams.ID, -1)
        initializeView()
    }

    private fun initializeView() {

        binding.tvGenderAge.text =
            "${getString(R.string.gender)}/${
                getString(R.string.age)
            }"

        binding.ivClose.safeClickListener {
            dismiss()
        }
        viewModel.getPatientListTransfer(
            NCDPatientTransferNotificationCountRequest(
                SecuredPreference.getOrganizationId().toString()
            )
        )
        viewModel.patientListResponse.observe(viewLifecycleOwner) { resourceState ->

            when (resourceState.state) {
                ResourceState.LOADING -> {
                    binding.CenterProgress.visible()
                    binding.cardHolder.gone()
                }

                ResourceState.SUCCESS -> {
                    resourceState.data?.let { data ->
                        loadPatientList(data.incomingPatientList)
                    }
                    binding.CenterProgress.gone()
                    binding.cardHolder.visible()
                }

                ResourceState.ERROR -> {
                    binding.CenterProgress.gone()
                    binding.cardHolder.visible()
                }
            }
        }
    }

    private fun loadPatientList(data: ArrayList<PatientTransfer>) {
        val model = data.filter { it.id == viewModel.transferPatientViewId }
        if (model.isNotEmpty()) {
            val patientModel = model[0]
            patientModel.patient.firstName.let { firstName ->
                var patientName = "$firstName "
                patientModel.patient.lastName?.let {
                        patientName += it
                }
                binding.tvDialogTitle.text = patientName
            }

            patientModel.patient.gender.let { gender ->
                patientModel.patient.age.let { age ->
                    binding.tvGenderAgeValue.text = "${gender}/${age.textOrHyphen()}"
                }
            }
            patientModel.patient.phoneNumber.let {
                binding.tvMobileNumberValue.text = it
            }

            patientModel.patient.identityValue.let {
                binding.tvNationalIdValue.text = it
            }
            patientModel.patient.programId.toString().let {
                binding.tvProgramIdValue.text = it
            }
            patientModel.patient.enrollmentAt.let {
                binding.tvEnrollDateValue.text = DateUtils.convertDateTimeToDate(
                    it,
                    DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                    DateUtils.DATE_ddMMyyyy
                )
            }
            patientModel.oldSite?.apply {
                binding.tvCurrentFacilityValue.text = name ?: getString(R.string.separator_hyphen)
            }

            patientModel.patient.pregnancyDetails?.apply {
                var menstrualDate: Date? = null
                lastMenstrualPeriodDate?.let {
                    menstrualDate = SimpleDateFormat(
                        DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                        Locale.ENGLISH
                    ).parse(it)
                }
                var deliveryDate: Date? = null
                estimatedDeliveryDate?.let {
                    deliveryDate = SimpleDateFormat(
                        DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                        Locale.ENGLISH
                    ).parse(it)
                }
            }

            if (!patientModel.patient.confirmDiagnosis.isNullOrEmpty()) {
                binding.tvDiagnosisValue.text = getListAsText(patientModel.patient.confirmDiagnosis)
            } else if (!patientModel.patient.provisionalDiagnosis.isNullOrEmpty()) {
                binding.tvDiagnosisValue.text =
                    "${getListAsText(patientModel.patient.provisionalDiagnosis)} ${getString(R.string.provisional_text)}"
            } else {
                binding.tvDiagnosisValue.text = getString(R.string.separator_hyphen)
            }

            if (!patientModel.patient.cvdRiskScore.isNullOrEmpty() && !patientModel.patient.cvdRiskLevel.isNullOrEmpty())
                binding.tvCVDRiskValue.text =
                    "${CommonUtils.getDecimalFormatted(patientModel.patient.cvdRiskScore)}% - ${patientModel.patient.cvdRiskLevel}"
            else
                binding.tvCVDRiskValue.text = getString(R.string.separator_hyphen)
        }
    }

    private fun getListAsText(list: ArrayList<String>?): String {
        val resultStringBuilder = StringBuilder()
        list?.let { list ->
            if (list.isNotEmpty()) {
                list.forEachIndexed { index, generalModel ->
                    resultStringBuilder.append(generalModel)
                    if (index != list.size - 1) {
                        resultStringBuilder.append(getString(R.string.comma_symbol))
                    }
                }
            } else {
                resultStringBuilder.append(getString(R.string.separator_hyphen))
            }
        }
        return resultStringBuilder.toString()
    }

}