package com.medtroniclabs.opensource.ncd.landing.dialog


import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.app.analytics.model.UserDetail
import com.medtroniclabs.opensource.app.analytics.utils.AnalyticsDefinedParams
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.setVisible
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.databinding.FragmentNcdOfflineDataDialogBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.ncd.landing.viewmodel.NCDOfflineDataViewModel
import com.medtroniclabs.opensource.ui.landing.OnDialogDismissListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NCDOfflineDataDialog : DialogFragment(), View.OnClickListener {

    private var onDismissListener: OnDialogDismissListener? = null
    private val viewModel: NCDOfflineDataViewModel by viewModels()
    private lateinit var binding: FragmentNcdOfflineDataDialogBinding

    override fun onAttach(context: Context) {
        super.onAttach(context)
        onDismissListener = context as OnDialogDismissListener
    }

    companion object {
        const val TAG = "NCDOfflineDataDialog"
        fun newInstance(): NCDOfflineDataDialog {
            return NCDOfflineDataDialog()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNcdOfflineDataDialogBinding.inflate(inflater, container, false)
        val window: Window? = dialog?.window
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        attachObservers()
    }

    private fun attachObservers() {
        viewModel.screeningCount.observe(viewLifecycleOwner) {
            handleScreening()
        }
        viewModel.assessmentType.observe(viewLifecycleOwner) {
            handleScreening()
        }
        viewModel.followUpType.observe(viewLifecycleOwner) {
            handleScreening()
        }
    }

    private fun handleScreening() {
        val screening = viewModel.screeningCount.value ?: 0
        val assessment = viewModel.assessmentType.value ?: 0
        val followUpNeed = if (CommonUtils.isNonCommunity() && CommonUtils.isChp()) {
            viewModel.followUpType.value ?: 0
        } else {
            0
        }
        binding.apply {
            val isFollowUpRequired = CommonUtils.isNonCommunity() && CommonUtils.isChp()
            if (isFollowUpRequired) {
                btnUpload.setVisible(screening > 0 || assessment > 0 || followUpNeed > 0)
                btnOkay.setVisible(screening <= 0 && assessment <= 0 && followUpNeed <= 0)
            } else {
                btnUpload.setVisible(screening > 0 || assessment > 0)
                btnOkay.setVisible(screening <= 0 && assessment <= 0)
            }
            screeningTitle.visible()
            assessmentGroup.visible()
            followUpGroup.setVisible(CommonUtils.isNonCommunity() && CommonUtils.isChp())
        }
        offlineDataHandling(screening, assessment, followUpNeed)
    }

    private fun offlineDataHandling(screening: Long, assessment: Long, followUpNeed: Long) {
        if (screening > 0) {
            binding.tvMessage.text =
                if (screening > 1) getString(
                    R.string.screened_patients,
                    screening.toString()
                ) else getString(
                    R.string.screened_patient
                )
        } else
            binding.tvMessage.text = getString(R.string.no_screened_patients)

        if (assessment > 0) {
            binding.tvAssessmentMessage.text =
                if (assessment > 1) getString(
                    R.string.assessed_patients,
                    assessment.toString()
                ) else getString(
                    R.string.assessed_patient
                )
        } else
            binding.tvAssessmentMessage.text = getString(R.string.no_assessed_patients)

        if (followUpNeed > 0) {
            binding.tvFollowUpMessage.text = if (followUpNeed > 1) getString(
                R.string.follow_ups,
                followUpNeed.toString()
            ) else getString(
                R.string.follow_up_one
            )
        } else {
            binding.tvFollowUpMessage.text = getString(R.string.no_follow_up)
        }
    }

    private fun initView() {
        viewModel.getCountOfflineData()
        binding.screeningTitle.gone()
        binding.btnUpload.gone()
        binding.btnOkay.visible()
        binding.assessmentGroup.gone()
        binding.labelHeader.ivClose.safeClickListener(this)
        binding.btnCancel.safeClickListener(this)
        binding.btnOkay.safeClickListener(this)
        binding.btnUpload.safeClickListener(this)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.labelHeader.ivClose.id, binding.btnCancel.id, binding.btnOkay.id -> {
                onDismissListener?.onDialogDismissListener()
                dismiss()
            }

            R.id.btnUpload -> {
                onDismissListener?.onDialogDismissListener(true)
                viewModel.setAnalyticsData(
                    UserDetail.startDateTime,
                    eventName = AnalyticsDefinedParams.NCDUploadOfflineData,
                    isCompleted = true
                )
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        onDismissListener = null
    }
}