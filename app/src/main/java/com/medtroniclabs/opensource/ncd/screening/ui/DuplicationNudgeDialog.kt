package com.medtroniclabs.opensource.ncd.screening.ui

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.app.analytics.utils.AnalyticsDefinedParams
import com.medtroniclabs.opensource.appextensions.setDialogPercent
import com.medtroniclabs.opensource.appextensions.setVisible
import com.medtroniclabs.opensource.appextensions.textOrHyphen
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.common.StringConverter
import com.medtroniclabs.opensource.databinding.DialogDuplicationNudgeBinding
import com.medtroniclabs.opensource.databinding.LayoutDuplicatePatientBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.mappingkey.Screening

class DuplicationNudgeDialog(private val callback: (doAssessment: Boolean) -> Unit) :
    DialogFragment(), View.OnClickListener {

    private lateinit var binding: DialogDuplicationNudgeBinding

    companion object {
        const val TAG = "DuplicationNudgeDialog"
        const val PATIENT_INFO = "PatientInfo"
        const val IS_FROM_ENROLLMENT = "isFromEnrollment"

        fun newInstance(
            patientInfo: String?,
            isFromEnrollment: Boolean,
            callback: (doAssessment: Boolean) -> Unit
        ): DuplicationNudgeDialog {
            val args = Bundle()
            args.putString(PATIENT_INFO, patientInfo)
            args.putBoolean(IS_FROM_ENROLLMENT, isFromEnrollment)
            val fragment = DuplicationNudgeDialog(callback)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DialogDuplicationNudgeBinding.inflate(inflater, container, false)
        val window: Window? = dialog?.window
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = false
        setViews()
    }

    private fun setViews() {
        val isFromEnrollment = requireArguments().getBoolean(IS_FROM_ENROLLMENT)

        requireArguments().getString(PATIENT_INFO)?.let { patientInfo ->
            StringConverter.convertStringToMap(patientInfo)?.let { map ->
                binding.apply {
                    addView(
                        Pair(
                            getString(R.string.first_name),
                            map[Screening.firstName]?.toString().textOrHyphen()
                        ), Pair(
                            getString(R.string.last_name),
                            map[Screening.lastName]?.toString().textOrHyphen()
                        )
                    )
                    addView(
                        Pair(
                            getString(R.string.mobile_number),
                            map[Screening.phoneNumber]?.toString().textOrHyphen()
                        ), Pair(
                            getString(R.string.national_id),
                            map[Screening.identityValue]?.toString().textOrHyphen()
                        )
                    )
                    val patientId = map[DefinedParams.ProgramId]?.toString()
                    addView(
                        Pair(
                            getString(R.string.facility_name),
                            map[Screening.healthFacilityName]?.toString().textOrHyphen()
                        ), if (patientId.isNullOrBlank()) Pair(
                            getString(R.string.empty), getString(R.string.empty)
                        )
                        else Pair(
                            getString(R.string.patient_id),
                            patientId
                        )
                    )

                    val isPatientEnrolled = map[AnalyticsDefinedParams.PatientStatus]?.toString()
                        .equals(DefinedParams.ENROLLED, true)
                    val sameOperatingUnit = map[Screening.CHIEFDOM_ID]?.toString()
                        .equals(SecuredPreference.getChiefdomId().toString())
                    val sameAccount = map[Screening.DISTRICT_ID]?.toString()
                        .equals(SecuredPreference.getDistrictId().toString())
                    if (isFromEnrollment) {
                        if (isPatientEnrolled) doAssessment(sameAccount)
                        else doEnrollment()
                    } else {
                        doAssessment(sameAccount)
                    }

                    btnEdit.safeClickListener(this@DuplicationNudgeDialog)
                    btnPrimary.safeClickListener(this@DuplicationNudgeDialog)
                    ivClose.safeClickListener(this@DuplicationNudgeDialog)
                }
            }
        }
    }

    private fun addView(left: Pair<String, String>, right: Pair<String, String>) {
        val view = LayoutDuplicatePatientBinding.inflate(layoutInflater)
        view.tvKeyLeft.text = left.first
        view.tvValueLeft.text = left.second

        view.tvKeyRight.text = right.first
        view.tvValueRight.text = right.second
        binding.llRoot.addView(view.root)
    }

    private fun doEnrollment() {
        binding.apply {
            applyMessageStyle(
                R.drawable.bg_warning_lite,
                R.drawable.ic_warning,
                R.string.duplicate_patient_not_registered
            )
            primaryGroup.visible()
            btnPrimary.text = getString(R.string.proceed)
        }
    }

    private fun doAssessment(sameAccount: Boolean) {
        binding.apply {
            applyMessageStyle(
                R.drawable.bg_error_lite,
                R.drawable.ic_error,
                if (sameAccount) R.string.duplicate_patient_same_acct else R.string.duplicate_patient_different_acct
            )
            primaryGroup.setVisible(sameAccount)
            btnPrimary.text = getString(R.string.start_assessment)
        }
    }

    private fun applyMessageStyle(bg: Int, img: Int, message: Int) {
        binding.apply {
            val context = requireContext()
            llMessage.background = ContextCompat.getDrawable(context, bg)
            ivInformation.setImageDrawable(ContextCompat.getDrawable(context, img))
            tvMessage.text = getString(message)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        handleConfiguration()

    }

    override fun onStart() {
        super.onStart()
        handleConfiguration()
    }

    private fun handleConfiguration() {
        val isTablet = CommonUtils.checkIsTablet(requireContext())
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        when {
            isTablet && isLandscape -> setDialogPercent(50, 90)
            isTablet -> setDialogPercent(80, 50)
            else -> setDialogPercent(95, 65)
        }
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.btnPrimary -> {
                dismiss()
                callback.invoke(binding.btnPrimary.text.equals(getString(R.string.start_assessment)))
            }

            R.id.btnEdit, R.id.ivClose -> {
                dismiss()
            }
        }
    }
}