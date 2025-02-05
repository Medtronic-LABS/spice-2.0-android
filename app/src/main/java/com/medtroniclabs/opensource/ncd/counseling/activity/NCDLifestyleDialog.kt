package com.medtroniclabs.opensource.ncd.counseling.activity

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.loadAsGif
import com.medtroniclabs.opensource.appextensions.resetImageView
import com.medtroniclabs.opensource.appextensions.setDialogPercent
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.data.model.ChipViewItemModel
import com.medtroniclabs.opensource.databinding.DialogNcdLifestyleBinding
import com.medtroniclabs.opensource.formgeneration.extension.hideKeyboard
import com.medtroniclabs.opensource.formgeneration.extension.markMandatory
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.ncd.data.NCDCounselingModel
import com.medtroniclabs.opensource.ncd.counseling.viewmodel.CounselingViewModel
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.network.utils.ConnectivityManager
import com.medtroniclabs.opensource.ui.TagListCustomView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NCDLifestyleDialog(private val callback: (isPositiveResult: Boolean) -> Unit) :
    DialogFragment(), View.OnClickListener {

    private lateinit var binding: DialogNcdLifestyleBinding

    private val viewModel: CounselingViewModel by viewModels()
    private lateinit var tagListCustomView: TagListCustomView

    @Inject
    lateinit var connectivityManager: ConnectivityManager

    companion object {
        const val TAG = "NCDLifestyleDialog"

        const val PatientReference = "patientReference"
        const val MemberReference = "memberReference"
        const val EncounterReference = "encounterReference"

        fun newInstance(
            patientReference: String?,
            memberReference: String?,
            encounterReference: String?,
            callback: (isPositiveResult: Boolean) -> Unit
        ): NCDLifestyleDialog {
            val dialog = NCDLifestyleDialog(callback)
            val bundle = Bundle()
            bundle.putString(PatientReference, patientReference)
            bundle.putString(MemberReference, memberReference)
            bundle.putString(EncounterReference, encounterReference)
            dialog.arguments = bundle
            return dialog
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        attachObserver()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DialogNcdLifestyleBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        isCancelable = false
        return binding.root
    }

    private fun initView() {
        arguments?.let {
            viewModel.apply {
                patientReference = it.getString(PatientReference)
                memberReference = it.getString(MemberReference)
                encounterReference = it.getString(EncounterReference)
            }
        }

        binding.apply {
            tvLifestyleAssessmentLbl.markMandatory()
            tvOtherNotesLbl.markMandatory()
            btnSave.safeClickListener(this@NCDLifestyleDialog)
            ivClose.safeClickListener(this@NCDLifestyleDialog)
            btnCancel.safeClickListener(this@NCDLifestyleDialog)
            loadingProgress.safeClickListener {}

            etLifestyleAssessment.addTextChangedListener {
                viewModel.lifestyleAssessment = it?.toString()
                updateView()
            }

            etOtherNotes.addTextChangedListener {
                viewModel.otherNote = it?.toString()
                updateView()
            }
        }

        tagListCustomView =
            TagListCustomView(requireContext(), binding.chipGroup) { _, _, _ ->
                viewModel.lifestyles =
                    tagListCustomView.getSelectedTags().map { it.name }.ifEmpty { null }
                viewModel.cultureLifestyles =
                    tagListCustomView.getSelectedTags().mapNotNull { it.cultureValue }.ifEmpty { null }
                updateView()
            }
        viewModel.getChips()
    }

    private fun updateView() {
        with(viewModel) {
            binding.btnSave.isEnabled =
                !(lifestyles.isNullOrEmpty() || lifestyleAssessment.isNullOrBlank() || otherNote.isNullOrBlank())
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
                    dismiss()
                    callback.invoke(true)
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.ivClose.id, binding.btnCancel.id -> {
                dismiss()
            }

            binding.btnSave.id -> {
                requireContext().hideKeyboard(v)
                if (connectivityManager.isNetworkAvailable())
                    viewModel.createAssessment(getCreateRequest(), true)
            }
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
                lifestyleAssessment = lifestyleAssessment,
                otherNote = otherNote,
                referredBy = NCDMRUtil.currentUserId(),
                referredByDisplay = NCDMRUtil.getUserName(),
                referredDate = DateUtils.getTodayDateDDMMYYYY(),
                assessedBy = NCDMRUtil.currentUserId(),
                assessedByDisplay = NCDMRUtil.getUserName(),
                assessedDate = DateUtils.getTodayDateDDMMYYYY(),
                isNutritionist = nutritionist
            )
        }
    }

    override fun onStart() {
        super.onStart()
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        if (isLandscape)
            setDialogPercent(60, 90)
        else
            setDialogPercent(90, 60)
    }

    private fun showLoading() {
        binding.apply {
            loadingProgress.visibility = View.VISIBLE
            loaderImage.apply {
                loadAsGif(R.drawable.loader_spice)
            }
            btnSave.visibility = View.INVISIBLE
            btnCancel.visibility = View.INVISIBLE
        }
    }

    private fun hideLoading() {
        binding.apply {
            loadingProgress.visibility = View.GONE
            loaderImage.apply {
                resetImageView()
            }
            btnSave.visibility = View.VISIBLE
            btnCancel.visibility = View.VISIBLE
        }
    }
}