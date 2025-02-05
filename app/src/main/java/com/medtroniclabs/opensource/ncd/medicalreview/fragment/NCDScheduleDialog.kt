package com.medtroniclabs.opensource.ncd.medicalreview.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.loadAsGif
import com.medtroniclabs.opensource.appextensions.resetImageView
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.ViewUtils
import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto
import com.medtroniclabs.opensource.databinding.FragmentNcdScheduleDialogBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.ncd.data.NCDMedicalReviewUpdateModel
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil
import com.medtroniclabs.opensource.ncd.medicalreview.viewmodel.HrioViewModel
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.network.utils.ConnectivityManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NCDScheduleDialog : DialogFragment(), View.OnClickListener {
    @Inject
    lateinit var connectivityManager: ConnectivityManager

    private val viewModel: HrioViewModel by activityViewModels()
    private lateinit var binding: FragmentNcdScheduleDialogBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentNcdScheduleDialogBinding.inflate(inflater, container, false)
        val window: Window? = dialog?.window
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        return binding.root
    }

    companion object {
        const val TAG = "NCDScheduleDialog"
        fun newInstance(patientReference: String? = null, memberReference: String? = null, villageId: String? = null) =
            NCDScheduleDialog().apply {
                arguments = Bundle().apply {
                    putString(NCDMRUtil.MEMBER_REFERENCE, memberReference)
                    putString(NCDMRUtil.PATIENT_REFERENCE, patientReference)
                    putString(NCDMRUtil.VillageID, villageId)
                }
            }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.attributes?.windowAnimations = R.style.dialogEnterExitAnimation
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = false
        initView()
        setObservers()
    }

    private fun setObservers() {
        viewModel.nextVisitResponse.observe(viewLifecycleOwner) { resource ->
            when (resource.state) {
                ResourceState.SUCCESS -> {
                    hideLoading()
                    viewModel.toTriggerPatientDetails()
                    dismiss()
                }

                ResourceState.LOADING -> showLoading()
                ResourceState.ERROR -> hideLoading()
            }
        }
    }

    private fun initView() {
        binding.titleCard.titleView.text = getString(R.string.schedule_next_medical_review)
        binding.btnDone.safeClickListener(this)
        binding.titleCard.ivClose.safeClickListener(this)
        binding.tvNextMedicalReviewDate.safeClickListener(this)
    }

    fun showLoading() {
        binding.loadingProgress.visible()
        binding.loaderImage.apply {
            loadAsGif(R.drawable.loader_spice)
        }
    }

    fun hideLoading() {
        binding.loadingProgress.gone()
        binding.loaderImage.apply {
            resetImageView()
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.btnDone.id -> {
                val input = binding.tvNextMedicalReviewDate.text
                if (!input.isNullOrBlank()) {
                    //proceed to save
                    binding.tvErrorMessage.gone()
                    arguments?.getString(NCDMRUtil.MEMBER_REFERENCE)?.let {
                        if (it.isNotBlank()) {
                            val request = NCDMedicalReviewUpdateModel(
                                villageId = arguments?.getString(NCDMRUtil.VillageID),
                                patientReference = arguments?.getString(NCDMRUtil.PATIENT_REFERENCE),
                                memberReference = it,
                                nextMedicalReviewDate = DateUtils.convertDateTimeToDate(
                                    input.toString(),
                                    DateUtils.DATE_FORMAT_ddMMMyyyy,
                                    DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                                    inUTC = true
                                ),
                                provenance = ProvanceDto()
                            )
                            if (connectivityManager.isNetworkAvailable()) {
                                viewModel.ncdUpdateNextVisitDate(request)
                            }
                        }
                    }
                } else {
                    binding.tvErrorMessage.visible()
                }
            }

            binding.titleCard.ivClose.id -> {
                dismiss()
            }

            binding.tvNextMedicalReviewDate.id -> {
                showDatePickerDialog()
            }
        }
    }

    private fun showDatePickerDialog() {
        var yearMonthDate: Triple<Int?, Int?, Int?>? = null
        val dateInput = binding.tvNextMedicalReviewDate.text
        if (!dateInput.isNullOrBlank())
            yearMonthDate =
                DateUtils.convertddMMMToddMM(dateInput.toString())

        ViewUtils.showDatePicker(
            context = requireContext(),
            minDate = DateUtils.getTomorrowDate(),
            date = yearMonthDate,
            cancelCallBack = { }
        ) { _, year, month, dayOfMonth ->
            val stringDate = "$dayOfMonth-$month-$year"
            binding.tvNextMedicalReviewDate.text =
                DateUtils.convertDateTimeToDate(
                    stringDate,
                    DateUtils.DATE_FORMAT_ddMMyyyy,
                    DateUtils.DATE_FORMAT_ddMMMyyyy
                )
            binding.tvErrorMessage.gone()
        }
    }

}