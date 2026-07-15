package org.medtroniclabs.uhis.ui.patient.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.AdapterView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.registration.FillPrescriptionListResponse
import org.medtroniclabs.uhis.databinding.DialogueQuantityDifferenceLayoutBinding
import org.medtroniclabs.uhis.databinding.LayoutQuantityDifferenceBinding
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.utility.CustomSpinnerAdapter
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.patient.viewmodel.PrescriptionRefillViewModel
import kotlin.getValue

class QuantityDifferenceDialogue : DialogFragment(), View.OnClickListener {
    private lateinit var binding: DialogueQuantityDifferenceLayoutBinding
    private val viewModel: PrescriptionRefillViewModel by activityViewModels()

    companion object {
        const val TAG = "QuantityDifferenceDialogue"

        fun newInstance(): QuantityDifferenceDialogue = QuantityDifferenceDialogue()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DialogueQuantityDifferenceLayoutBinding.inflate(inflater, container, false)
        val window: Window? = dialog?.window
        window?.setBackgroundDrawableResource(android.R.color.transparent)
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
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
        )
    }

    private fun initView() {
        binding.titleCard.ivClose.safeClickListener(this)
        binding.btnDone.safeClickListener(this)
        binding.btnCancel.safeClickListener(this)
        viewModel.getShortageReasonList()
        attachObserver()
    }

    private fun loadMedicationDifferenceDate() {
        binding.rvDifferenceList.removeAllViews()
        val list = viewModel.patientRefillMedicationList.value
            ?.data
            ?.filter { it.remainingPrescriptionDays != it.prescriptionFilledDays }
        list?.forEach { model ->
            val lifeStyleBinding = LayoutQuantityDifferenceBinding.inflate(layoutInflater)
            lifeStyleBinding.tvMedicationName.text = model.medicationName
            lifeStyleBinding.tvPrescribedDays.text = "${model.remainingPrescriptionDays}"
            lifeStyleBinding.tvfilledDays.text = "${model.prescriptionFilledDays ?: 0}"
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

    private fun loadOtherBasedOnReason(
        lifeStyleBinding: LayoutQuantityDifferenceBinding,
        model: FillPrescriptionListResponse,
    ) {
        if (model.reason.isNullOrBlank() || (!model.reason.equals(DefinedParams.OTHER_REASON))) {
            lifeStyleBinding.clOtherHolder.visibility = View.GONE
            lifeStyleBinding.etOther.setText("")
        } else {
            lifeStyleBinding.clOtherHolder.visibility = View.VISIBLE
        }
        lifeStyleBinding.etOther.addTextChangedListener {
            if (it.isNullOrBlank()) {
                model.otherReasonDetail = null
            } else {
                model.otherReasonDetail = it.toString()
            }
        }
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.ivClose -> {
                dialog?.dismiss()
            }
            R.id.btnDone -> {
                val filterList =
                    viewModel.patientRefillMedicationList.value
                        ?.data
                        ?.filter { it.remainingPrescriptionDays != it.prescriptionFilledDays }
                if (filterList != null && filterList.isNotEmpty()) {
                    val listWithoutReason = filterList.filter {
                        it.reason != null && it.reason.equals(DefinedParams.DEFAULT_ID_LABEL)
                    }
                    if (listWithoutReason.isEmpty()) {
                        dialog?.dismiss()
                        viewModel.patientVisitId?.let { patientVisitId ->
                            viewModel.patientTrackId?.let { patientId ->
                                viewModel.fillPrescriptionUpdate(
                                    requireContext(),
                                    patientId,
                                    SecuredPreference.getTenantId(),
                                    patientVisitId,
                                    viewModel.getFillPrescriptionList(),
                                )
                            }
                        }
                    } else {
                        (activity as BaseActivity).showErrorDialogue(getString(R.string.error), getString(R.string.reason_error), false) {}
                    }
                } else {
                    (activity as BaseActivity).showErrorDialogue(getString(R.string.error), getString(R.string.fill_prescription), false) {}
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
                DefinedParams.NAME to DefinedParams.DEFAULT_ID_LABEL,
                DefinedParams.ID to DefinedParams.DEFAULT_ID,
            ),
        )
        viewModel.shortageReasonList.value?.data?.forEach {
            dropDownList.add(
                hashMapOf<String, Any>(
                    DefinedParams.NAME to it.name,
                    DefinedParams.ID to it.id,
                    DefinedParams.CULTURE_VALUE to (it.name),
                ),
            )
        }

        /*dropDownList.add(
            hashMapOf<String, Any>(
                DefinedParams.NAME to MedicalReviewConstant.Stock_Shortage,
                DefinedParams.ID to MedicalReviewConstant.Stock_Shortage
            )
        )
        dropDownList.add(
            hashMapOf<String, Any>(
                DefinedParams.NAME to MedicalReviewConstant.Other_Reason,
                DefinedParams.ID to MedicalReviewConstant.Other_Reason
            )
        )*/
        return dropDownList
    }
}
