package org.medtroniclabs.uhis.ui.assessment.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.common.DefinedParams.DEFAULT_ID
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.model.SymptomModel
import org.medtroniclabs.uhis.databinding.LayoutChooseSymptomsBinding
import org.medtroniclabs.uhis.db.entity.SignsAndSymptomsEntity
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams
import org.medtroniclabs.uhis.ui.assessment.SymptomAdapter
import org.medtroniclabs.uhis.ui.assessment.viewmodel.AssessmentViewModel

class SymptomsChooseDialog : DialogFragment(), View.OnClickListener {
    lateinit var binding: LayoutChooseSymptomsBinding

    private val viewModel: AssessmentViewModel by activityViewModels()

    companion object {
        const val TAG = "SymptomsChooseDialog"

        fun newInstance(): SymptomsChooseDialog = SymptomsChooseDialog()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = LayoutChooseSymptomsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initializeView(view)
        viewModel.getSymptomList()
        attachObserver()
    }

    private fun attachObserver() {
        viewModel.symptomListResponse.observe(viewLifecycleOwner) {
            binding.rvSymptom.adapter = SymptomAdapter(getSymptomModelList(it), SecuredPreference.getIsTranslationEnabled())
        }
    }

    private fun getSymptomModelList(symptomList: List<SignsAndSymptomsEntity>?): ArrayList<SymptomModel> {
        val selectedSymptoms = viewModel.selectedSymptoms.value
        val tempList = ArrayList<SymptomModel>()
        symptomList?.forEach { symptom ->
            if (selectedSymptoms != null && selectedSymptoms.isNotEmpty()) {
                val model = selectedSymptoms.find { it.id == symptom._id }
                if (model != null) {
                    tempList.add(SymptomModel(symptom._id, symptom.symptom, true, symptom.type, cultureValue = symptom.displayValue, value = symptom.value))
                } else {
                    tempList.add(SymptomModel(symptom._id, symptom.symptom, type = symptom.type, cultureValue = symptom.displayValue, value = symptom.value))
                }
            } else {
                tempList.add(SymptomModel(symptom._id, symptom.symptom, type = symptom.type, cultureValue = symptom.displayValue, value = symptom.value))
            }
        }

        val list = ArrayList<SymptomModel>()
        list.add(
            SymptomModel(
                DEFAULT_ID.toLong(),
                "",
                false,
                getString(R.string.hypertension),
                viewType = 1,
            ),
        )
        list.addAll(
            tempList.filter {
                it.type.equals(
                    AssessmentDefinedParams.Compliance_Type_Hypertension,
                    true,
                )
            },
        )
        list.add(
            SymptomModel(
                DEFAULT_ID.toLong(),
                "",
                false,
                getString(R.string.diabetes),
                viewType = 1,
            ),
        )
        list.addAll(
            tempList.filter {
                it.type.equals(
                    AssessmentDefinedParams.Compliance_Type_Diabetes,
                    true,
                )
            },
        )
        return list
    }

    private fun initializeView(view: View) {
        binding.rvSymptom.layoutManager = LinearLayoutManager(view.context)
        binding.btnCancel.safeClickListener(this)
        binding.btnOkay.safeClickListener(this)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
        )
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.btnCancel -> {
                dismiss()
            }

            R.id.btnOkay -> {
                val adapter = binding.rvSymptom.adapter
                if (adapter != null && adapter is SymptomAdapter) {
                    viewModel.selectedSymptoms.value = adapter.getSelectedSymptomList()
                }
                dismiss()
            }
        }
    }
}
