package org.medtroniclabs.uhis.ncd.medicalreview.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.invisible
import org.medtroniclabs.uhis.appextensions.isVisible
import org.medtroniclabs.uhis.appextensions.loadAsGif
import org.medtroniclabs.uhis.appextensions.resetImageView
import org.medtroniclabs.uhis.appextensions.setDialogPercent
import org.medtroniclabs.uhis.appextensions.setVisible
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.DefinedParams.CULTURE_VALUE
import org.medtroniclabs.uhis.common.DefinedParams.DEFAULT_ID
import org.medtroniclabs.uhis.common.DefinedParams.ID
import org.medtroniclabs.uhis.common.DefinedParams.NAME
import org.medtroniclabs.uhis.common.DefinedParams.Value
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.databinding.DialogNcdTreatmentPlanBinding
import org.medtroniclabs.uhis.db.entity.TreatmentPlanEntity
import org.medtroniclabs.uhis.formgeneration.extension.markMandatory
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.utility.CustomSpinnerAdapter
import org.medtroniclabs.uhis.ncd.data.NCDTreatmentPlanModel
import org.medtroniclabs.uhis.ncd.data.NCDTreatmentPlanModelDetails
import org.medtroniclabs.uhis.ncd.medicalreview.NCDMRUtil
import org.medtroniclabs.uhis.ncd.medicalreview.viewmodel.NCDTreatmentPlanViewModel
import org.medtroniclabs.uhis.network.resource.ResourceState

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
            callback: ((isPositiveResult: Boolean, message: String) -> Unit),
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

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        attachObserver()
        setListeners()

        viewModel.getFrequencies()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
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
                        true,
                        resourceState.data?.message ?: "",
                    )
                    dismiss()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                    callback.invoke(
                        false,
                        resourceState.message ?: getString(R.string.something_went_wrong_try_later),
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
                    NAME to getString(R.string.please_select),
                    ID to DEFAULT_ID,
                ),
            )
            for (item in list) {
                dropDownList.add(
                    item.displayValue?.let { culture ->
                        hashMapOf(
                            NAME to item.name,
                            Value to item,
                            CULTURE_VALUE to culture,
                        )
                    } ?: run {
                        hashMapOf(
                            NAME to item.name,
                            Value to item,
                        )
                    },
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
                    memberReference = memberReference,
                ),
            )
        }
    }

    private fun setListeners() {
        binding.etMedicalReviewFrequency.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    view: View?,
                    pos: Int,
                    itemId: Long,
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
                    adapterView: AdapterView<*>?,
                    view: View?,
                    pos: Int,
                    itemId: Long,
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
                    adapterView: AdapterView<*>?,
                    view: View?,
                    pos: Int,
                    itemId: Long,
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
                    adapterView: AdapterView<*>?,
                    view: View?,
                    pos: Int,
                    itemId: Long,
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
                    adapterView: AdapterView<*>?,
                    view: View?,
                    pos: Int,
                    itemId: Long,
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
            viewModel.medicalReviewFrequency != null &&
                viewModel.bpCheckFrequency != null &&
                viewModel.bgCheckFrequency != null &&
                viewModel.hba1cCheckFrequency != null
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
                    carePlanId = carePlanId,
                ),
            )
        }
    }

    fun showLoading() {
        binding.apply {
            btnSubmit.invisible()
            btnCancel.invisible()
            loadingProgress.visible()
            loaderImage.apply {
                loadAsGif(R.drawable.ic_rotating_uhis_logo)
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
