package org.medtroniclabs.uhis.ui.medicalreview.pharmacist.fragment

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.numberOrZero
import org.medtroniclabs.uhis.appextensions.setWidth
import org.medtroniclabs.uhis.appextensions.textOrHyphen
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.DispensePrescriptionResponse
import org.medtroniclabs.uhis.data.DispenseUpdatePrescriptionRequest
import org.medtroniclabs.uhis.databinding.FragmentNcdQuantityDifferenceDialogueBinding
import org.medtroniclabs.uhis.databinding.LayoutQuantityDifferenceBinding
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.utility.CustomSpinnerAdapter
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.network.utils.ConnectivityManager
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.MenuConstants
import org.medtroniclabs.uhis.ui.medicalreview.pharmacist.viewModel.NCDPharmacistViewModel
import javax.inject.Inject

@AndroidEntryPoint
class NCDQuantityDifferenceDialogueFragment : DialogFragment(), View.OnClickListener {
    private lateinit var binding: FragmentNcdQuantityDifferenceDialogueBinding
    private val viewModel: NCDPharmacistViewModel by activityViewModels()

    @Inject
    lateinit var connectivityManager: ConnectivityManager

    companion object {
        const val TAG = "NCDQuantityDifferenceDialogueFragment"

        fun newInstance(): NCDQuantityDifferenceDialogueFragment = NCDQuantityDifferenceDialogueFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentNcdQuantityDifferenceDialogueBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        isCancelable = false
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    override fun onStart() {
        super.onStart()
        handleDialogSize()
    }

    private fun handleDialogSize() {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val width = if (CommonUtils.checkIsTablet(requireContext())) {
            if (isLandscape) 90 else 90
        } else {
            if (isLandscape) 50 else 60
        }
        setWidth(width)
    }

    private fun initView() {
        binding.ivClose.safeClickListener(this)
        binding.bottomView.btnDone.safeClickListener(this)
        binding.bottomView.btnCancel.safeClickListener(this)
        viewModel.getShortageReasonList()
        attachObserver()
    }

    private fun attachObserver() {
        viewModel.shortageReasonList.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    // Invoked if the response is in loading state
                }

                ResourceState.SUCCESS -> {
                    loadMedicationDifferenceDate()
                }

                ResourceState.ERROR -> {
                    loadMedicationDifferenceDate()
                }
            }
        }
    }

    private fun loadMedicationDifferenceDate() {
        binding.rvDifferenceList.removeAllViews()
        val list =
            viewModel.prescriptionDispenseLiveData.value
                ?.data
                ?.filter { it.prescriptionFilledDays.numberOrZero() < it.dispenseRemainingDays.numberOrZero() }
        list?.forEach { model ->
            val lifeStyleBinding = LayoutQuantityDifferenceBinding.inflate(layoutInflater)
            lifeStyleBinding.tvMedicationName.text = model.medicationName.textOrHyphen()
            lifeStyleBinding.tvPrescribedDays.text = model.dispenseRemainingDays.numberOrZero().toString()
            lifeStyleBinding.tvfilledDays.text = model.prescriptionFilledDays.numberOrZero().toString()
            val adapter = CustomSpinnerAdapter(requireContext(), SecuredPreference.getIsTranslationEnabled())
            adapter.setData(getDropDownList())
            lifeStyleBinding.etReasonSpinner.adapter = adapter
            lifeStyleBinding.etReasonSpinner.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        p0: AdapterView<*>?,
                        p1: View?,
                        p2: Int,
                        p3: Long,
                    ) {
                        adapter.getData(p2)?.let {
                            model.reason = it[DefinedParams.NAME] as String
                            loadOtherBasedOnReason(lifeStyleBinding, model)
                        }
                    }

                    override fun onNothingSelected(p0: AdapterView<*>?) {
                        /**
                         * this method is not used
                         */
                    }
                }
            binding.rvDifferenceList.addView(lifeStyleBinding.root)
        }
    }

    private fun loadOtherBasedOnReason(
        lifeStyleBinding: LayoutQuantityDifferenceBinding,
        model: DispensePrescriptionResponse,
    ) {
        if (model.reason.isNullOrBlank() || (!model.reason.equals(MenuConstants.OTHER_REASON))) {
            lifeStyleBinding.clOtherHolder.gone()
            lifeStyleBinding.etOther.setText("")
        } else {
            lifeStyleBinding.clOtherHolder.visible()
        }
        lifeStyleBinding.etOther.addTextChangedListener {
            if (it.isNullOrBlank()) {
                model.otherReasonDetail = null
            } else {
                model.otherReasonDetail = it.toString()
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
                    reason = it.reason,
                ),
            )
        }
        return prescriptionList
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.ivClose -> {
                dialog?.dismiss()
            }

            R.id.btnDone -> {
                val filterList =
                    viewModel.prescriptionDispenseLiveData.value?.data?.filter {
                        it.prescriptionFilledDays.numberOrZero() <
                            it.dispenseRemainingDays.numberOrZero()
                    }
                if (!filterList.isNullOrEmpty()) {
                    val listWithoutReason = filterList.filter {
                        it.reason != null && it.reason.equals(DefinedParams.DEFAULT_ID_LABEL)
                    }
                    if (listWithoutReason.isEmpty()) {
                        dialog?.dismiss()
                        if ((
                                !viewModel.patientVisitId.isNullOrBlank() &&
                                    !viewModel.patientReference.isNullOrBlank()
                            ) &&
                            !viewModel.memberId.isNullOrBlank()
                        ) {
                            if (connectivityManager.isNetworkAvailable()) {
                                viewModel.updateDispensePrescription(
                                    patientVisitId = viewModel.patientVisitId,
                                    patientReference = viewModel.patientReference,
                                    memberId = viewModel.memberId,
                                    request = getReqBody(),
                                )
                            } else {
                                (activity as BaseActivity).showErrorDialogue(
                                    getString(R.string.error),
                                    getString(R.string.reason_error),
                                    false,
                                ) {}
                            }
                        }
                    } else {
                        (activity as BaseActivity).showErrorDialogue(
                            getString(R.string.error),
                            getString(R.string.reason_error),
                            false,
                        ) {}
                    }
                } else {
                    (activity as BaseActivity).showErrorDialogue(
                        getString(R.string.error),
                        getString(R.string.fill_prescription),
                        false,
                    ) {}
                }
            }

            R.id.btnCancel -> {
                dialog?.dismiss()
            }
        }
    }

    private fun getDropDownList(): ArrayList<Map<String, Any>> {
        val dropDownList = ArrayList<Map<String, Any>>()
        dropDownList.add(
            hashMapOf<String, Any>(
                DefinedParams.NAME to getString(R.string.please_select),
                DefinedParams.ID to DefinedParams.DEFAULT_ID,
            ),
        )

        viewModel.shortageReasonList.value?.data?.forEach {
            dropDownList.add(
                hashMapOf<String, Any>(
                    DefinedParams.NAME to it.name,
                    DefinedParams.ID to it.id,
                    DefinedParams.CULTURE_VALUE to it.displayValue,
                ),
            )
        }
        return dropDownList
    }
}
