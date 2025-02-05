package com.medtroniclabs.opensource.ncd.medicalreview.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.invisible
import com.medtroniclabs.opensource.appextensions.isVisible
import com.medtroniclabs.opensource.appextensions.loadAsGif
import com.medtroniclabs.opensource.appextensions.resetImageView
import com.medtroniclabs.opensource.appextensions.setDialogPercent
import com.medtroniclabs.opensource.appextensions.setVisible
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.DefinedParams.DefaultID
import com.medtroniclabs.opensource.common.DefinedParams.ID
import com.medtroniclabs.opensource.common.DefinedParams.NAME
import com.medtroniclabs.opensource.common.DefinedParams.Value
import com.medtroniclabs.opensource.common.DefinedParams.cultureValue
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.databinding.DialogNcdTreatmentPlanBinding
import com.medtroniclabs.opensource.db.entity.TreatmentPlanEntity
import com.medtroniclabs.opensource.formgeneration.extension.markMandatory
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.formgeneration.utility.CustomSpinnerAdapter
import com.medtroniclabs.opensource.ncd.data.NCDTreatmentPlanModel
import com.medtroniclabs.opensource.ncd.data.NCDTreatmentPlanModelDetails
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil
import com.medtroniclabs.opensource.ncd.medicalreview.viewmodel.NCDTreatmentPlanViewModel
import com.medtroniclabs.opensource.network.resource.ResourceState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NCDTreatmentPlanDialog(private val callback: ((isPositiveResult: Boolean, message: String) -> Unit)) :
    DialogFragment(), View.OnClickListener {
    private lateinit var binding: DialogNcdTreatmentPlanBinding
    private val viewModel: NCDTreatmentPlanViewModel by viewModels()

    private val medicalReviewAdapter by lazy { CustomSpinnerAdapter(requireContext(), SecuredPreference.getIsTranslationEnabled()) }
    private val bPCheckAdapter by lazy { CustomSpinnerAdapter(requireContext(), SecuredPreference.getIsTranslationEnabled()) }
    private val bGCheckAdapter by lazy { CustomSpinnerAdapter(requireContext(), SecuredPreference.getIsTranslationEnabled()) }
    private val hbA1cAdapter by lazy { CustomSpinnerAdapter(requireContext(), SecuredPreference.getIsTranslationEnabled()) }
    private val choAdapter by lazy { CustomSpinnerAdapter(requireContext(), SecuredPreference.getIsTranslationEnabled()) }

    companion object {
        const val TAG = "NCDTreatmentPlanDialog"
        fun newInstance(
            patientId: String?,
            fhirId: String?,
            showCHO: Boolean,
            callback: ((isPositiveResult: Boolean, message: String) -> Unit)
        ): NCDTreatmentPlanDialog {
            val dialog = NCDTreatmentPlanDialog(callback)
            val bundle = Bundle()
            bundle.putString(DefinedParams.PatientId, patientId)
            bundle.putString(DefinedParams.FhirId, fhirId)
            bundle.putBoolean(NCDMRUtil.ShowCHO, showCHO)
            dialog.arguments = bundle
            return dialog
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        attachObserver()
        setListeners()

        viewModel.getFrequencies()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DialogNcdTreatmentPlanBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        isCancelable = false
        return binding.root
    }

    private fun initView() {
        arguments?.let {
            viewModel.apply {
                patientReference = it.getString(DefinedParams.PatientId)
                memberReference = it.getString(DefinedParams.FhirId)
            }
        }
        with(binding) {
            choGroup.setVisible(arguments?.getBoolean(NCDMRUtil.ShowCHO) == true)
            tvMedicalReviewFrequencyLbl.markMandatory()
            tvBPCheckFrequencyLbl.markMandatory()
            tvBGCheckFrequencyLbl.markMandatory()
            tvHbA1cFrequencyLbl.markMandatory()
            tvCHOCheckFrequencyLbl.markMandatory()
            ivClose.safeClickListener(this@NCDTreatmentPlanDialog)
            btnCancel.safeClickListener(this@NCDTreatmentPlanDialog)
            btnSubmit.safeClickListener(this@NCDTreatmentPlanDialog)
            loadingProgress.safeClickListener {}
        }
    }

    private fun attachObserver() {
        viewModel.allFrequencies.observe(viewLifecycleOwner) { data ->
            handleFrequencies(data)
        }
        viewModel.getNCDTreatmentPlanLiveData.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    resourceState.data?.entity?.let {
                        autoPopulateData(it)
                    }
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }
            }
        }
        viewModel.updateNCDTreatmentPlanLiveData.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    callback.invoke(
                        true, resourceState.data?.message ?: ""
                    )
                    dismiss()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                    callback.invoke(
                        false,
                        resourceState.message ?: getString(R.string.something_went_wrong_try_later)
                    )
                    dismiss()
                }
            }
        }
    }

    private fun autoPopulateData(data: NCDTreatmentPlanModelDetails) {
        viewModel.carePlanId = data.carePlanId
        data.medicalReviewFrequency?.let { medicalReview ->
            medicalReviewAdapter.getIndexOfItemByName(medicalReview).let {
                binding.etMedicalReviewFrequency.setSelection(it)
            }
        }
        data.bpCheckFrequency?.let { bpCheck ->
            bPCheckAdapter.getIndexOfItemByName(bpCheck).let {
                binding.etBPCheckFrequency.setSelection(it)
            }
        }
        data.bgCheckFrequency?.let { bgCheck ->
            bGCheckAdapter.getIndexOfItemByName(bgCheck).let {
                binding.etBGCheckFrequency.setSelection(it)
            }
        }
        data.hba1cCheckFrequency?.let { hba1cCheck ->
            hbA1cAdapter.getIndexOfItemByName(hba1cCheck).let {
                binding.etHbA1cFrequency.setSelection(it)
            }
        }
        data.choCheckFrequency?.let { choCheck ->
            choAdapter.getIndexOfItemByName(choCheck).let {
                binding.etCHOCheckFrequency.setSelection(it)
            }
        }
    }

    private fun handleFrequencies(data: List<TreatmentPlanEntity>?) {
        data?.let { list ->
            val dropDownList = ArrayList<Map<String, Any>>()
            dropDownList.add(
                hashMapOf<String, Any>(
                    NAME to getString(R.string.please_select), ID to DefaultID
                )
            )
            for (item in list) {
                dropDownList.add(
                    item.displayValue?.let { culture ->
                        hashMapOf(
                            NAME to item.name, Value to item, cultureValue to culture
                        )
                    } ?: run {
                        hashMapOf(
                            NAME to item.name, Value to item
                        )
                    }
                )
            }

            medicalReviewAdapter.setData(dropDownList)
            bPCheckAdapter.setData(dropDownList)
            bGCheckAdapter.setData(dropDownList)
            hbA1cAdapter.setData(dropDownList)
            choAdapter.setData(dropDownList)

            with(binding) {
                etMedicalReviewFrequency.adapter = medicalReviewAdapter
                etBPCheckFrequency.adapter = bPCheckAdapter
                etBGCheckFrequency.adapter = bGCheckAdapter
                etHbA1cFrequency.adapter = hbA1cAdapter
                etCHOCheckFrequency.adapter = choAdapter
            }

            getTreatmentPlan()
        }
    }

    private fun getTreatmentPlan() {
        with(viewModel) {
            getNCDTreatmentPlan(
                NCDTreatmentPlanModelDetails(
                    patientReference = patientReference,
                    memberReference = memberReference
                )
            )
        }
    }

    private fun setListeners() {
        binding.etMedicalReviewFrequency.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?, view: View?, pos: Int, itemId: Long
                ) {
                    medicalReviewAdapter.getData(pos)?.let { selectedItem ->
                        viewModel.medicalReviewFrequency =
                            selectedItem[Value] as? TreatmentPlanEntity?
                        validateInputs()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        binding.etBPCheckFrequency.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?, view: View?, pos: Int, itemId: Long
                ) {
                    bPCheckAdapter.getData(pos)?.let { selectedItem ->
                        viewModel.bpCheckFrequency = selectedItem[Value] as? TreatmentPlanEntity?
                        validateInputs()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        binding.etBGCheckFrequency.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?, view: View?, pos: Int, itemId: Long
                ) {
                    bGCheckAdapter.getData(pos)?.let { selectedItem ->
                        viewModel.bgCheckFrequency = selectedItem[Value] as? TreatmentPlanEntity?
                        validateInputs()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        binding.etHbA1cFrequency.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?, view: View?, pos: Int, itemId: Long
                ) {
                    hbA1cAdapter.getData(pos)?.let { selectedItem ->
                        viewModel.hba1cCheckFrequency = selectedItem[Value] as? TreatmentPlanEntity?
                        validateInputs()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        binding.etCHOCheckFrequency.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?, view: View?, pos: Int, itemId: Long
                ) {
                    choAdapter.getData(pos)?.let { selectedItem ->
                        viewModel.choCheckFrequency = selectedItem[Value] as? TreatmentPlanEntity?
                        validateInputs()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    private fun validateInputs() {
        val haveValidInputs =
            viewModel.medicalReviewFrequency != null && viewModel.bpCheckFrequency != null && viewModel.bgCheckFrequency != null && viewModel.hba1cCheckFrequency != null
        binding.btnSubmit.isEnabled =
            if (binding.choGroup.isVisible()) haveValidInputs && viewModel.choCheckFrequency != null else haveValidInputs
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.btnCancel.id, binding.ivClose.id -> dismiss()
            binding.btnSubmit.id -> updateTreatmentPlan()
        }
    }

    private fun updateTreatmentPlan() {
        with(viewModel) {
            updateNCDTreatmentPlan(
                NCDTreatmentPlanModel(
                    patientReference = patientReference,
                    memberReference = memberReference,
                    medicalReviewFrequency = medicalReviewFrequency,
                    bpCheckFrequency = bpCheckFrequency,
                    bgCheckFrequency = bgCheckFrequency,
                    hba1cCheckFrequency = hba1cCheckFrequency,
                    choCheckFrequency = choCheckFrequency,
                    carePlanId = carePlanId
                )
            )
        }
    }

    fun showLoading() {
        binding.apply {
            btnSubmit.invisible()
            btnCancel.invisible()
            loadingProgress.visible()
            loaderImage.apply {
                loadAsGif(R.drawable.loader_spice)
            }
        }
    }

    fun hideLoading() {
        binding.apply {
            btnSubmit.visible()
            btnCancel.visible()
            loadingProgress.gone()
            loaderImage.apply {
                resetImageView()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        setDialogPercent(75, 75)
    }
}