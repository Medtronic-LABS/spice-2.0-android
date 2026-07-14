package org.medtroniclabs.uhis.ui.patient.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.registration.PatientDetailsModel
import org.medtroniclabs.uhis.data.registration.Symptom
import org.medtroniclabs.uhis.data.registration.SymptomsLog
import org.medtroniclabs.uhis.data.registration.UnselectedDiagnosis
import org.medtroniclabs.uhis.databinding.FragmentSymptomsAdherenceBinding
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams
import org.medtroniclabs.uhis.formgeneration.extension.capitalizeFirstChar
import org.medtroniclabs.uhis.formgeneration.extension.markMandatory
import org.medtroniclabs.uhis.formgeneration.model.FormLayout
import org.medtroniclabs.uhis.formgeneration.ui.SingleSelectionCustomView
import org.medtroniclabs.uhis.formgeneration.utility.CustomSpinnerAdapter
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.assessment.fragment.SymptomsChooseDialog
import org.medtroniclabs.uhis.ui.assessment.viewmodel.AssessmentViewModel
import org.medtroniclabs.uhis.ui.patient.viewmodel.MedicalReviewBaseViewModel
import org.medtroniclabs.uhis.ui.patient.viewmodel.NurseMedicalReviewViewModel
import kotlin.getValue

class SymptomsAdherenceFragment : BaseFragment() {
    private val nurseMedicalReviewViewModel: NurseMedicalReviewViewModel by activityViewModels()

    private val assessmentViewModel: AssessmentViewModel by activityViewModels()
    private lateinit var binding: FragmentSymptomsAdherenceBinding
    private val medicalReviewBaseViewModel: MedicalReviewBaseViewModel by activityViewModels()

    companion object {
        const val TAG = "SymptomsAdherenceFragment"

        fun newInstance(): SymptomsAdherenceFragment = SymptomsAdherenceFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentSymptomsAdherenceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        attachObserver()
        spinner()
        binding.symptomsSpinner.setOnClickListener {
            SymptomsChooseDialog
                .newInstance()
                .show(childFragmentManager, SymptomsChooseDialog.TAG)
        }
        nurseMedicalReviewViewModel.nurseMrRequestModel.symptomsLog =
            nurseMedicalReviewViewModel.nurseMrRequestModel.symptomsLog
                ?: SymptomsLog(symptoms = arrayListOf(), compliance = null, medicationTakenDays = null, hasSymptoms = null)
    }

    private fun initView() {
        binding.tvWorseningSymptoms.markMandatory()
        binding.tvTakenMedications.markMandatory()
        binding.tvTakingMedication.markMandatory()
        binding.tvSelectSymptoms.markMandatory()

        addCustomView(
            getData(),
            DefinedParams.WORSENING_SYMPTOMS,
            nurseMedicalReviewViewModel.worseningSymptomsSelections,
            selectionCallBack,
            binding.llWorseningSymptoms,
        )

        binding.etNewOrWorsening.addTextChangedListener {
            nurseMedicalReviewViewModel.nurseMrRequestModel.symptomsLog
                ?.symptoms
                ?.find { otherSymptom ->
                    otherSymptom.name.equals(
                        DefinedParams.ANY_NEW_OR_WORSENING_SYMPTOMS,
                        true,
                    )
                }?.apply {
                    newWorseningSymptoms = if (it.isNullOrBlank()) null else it.toString()
                }
        }
    }

    private fun attachObserver() {
        nurseMedicalReviewViewModel.latestConfirmDiagnosesList.observe(viewLifecycleOwner) { resourceState ->
            updateDiagnosis(resourceState)
        }
        nurseMedicalReviewViewModel.patientDetailsResponse.observe(viewLifecycleOwner) { resourceState ->
            updateDiagnosis(resourceState)
        }

        nurseMedicalReviewViewModel.selectedSymptomsAndAdherenceLiveData.observe(viewLifecycleOwner) { data ->
            if (data.equals(DefinedParams.NOT_AT_ALL) || data.equals(DefinedParams.SELECT)) {
                binding.tvTakenMedications.visibility = View.GONE
                binding.etTakenMedication.visibility = View.GONE
                binding.etTakenMedication.text?.clear()
                binding.tvTakenMedicationErrorMessage.visibility = View.GONE
            } else {
                binding.tvTakenMedications.visibility = View.GONE // Visible
                binding.etTakenMedication.visibility = View.GONE // Visible
            }
        }

        assessmentViewModel.selectedSymptoms.observe(viewLifecycleOwner) { model ->
            if (model?.size == 1) {
                model.forEach {
                    if (it.symptom == DefinedParams.NO_SYMPTOMS) {
                        binding.symptomsSpinner.text = getString(R.string.no_symptom_selected)
                        binding.gNewOrWorsening.visibility = View.GONE
                    } else if (it.symptom == DefinedParams.ANY_NEW_OR_WORSENING_SYMPTOMS) {
                        binding.symptomsSpinner.text = getString(R.string._1_symptom_selected)
                        binding.gNewOrWorsening.visibility = View.VISIBLE
                    } else {
                        binding.symptomsSpinner.text = getString(R.string._1_symptom_selected)
                        binding.gNewOrWorsening.visibility = View.GONE
                    }
                }
            } else if (model?.size == 0) {
                binding.symptomsSpinner.text = getString(R.string.please_select)
                binding.gNewOrWorsening.visibility = View.GONE
            } else {
                model.forEach {
                    if (it.symptom.equals(DefinedParams.ANY_NEW_OR_WORSENING_SYMPTOMS, true)) {
                        binding.gNewOrWorsening.visibility = View.VISIBLE
                    } else {
                        binding.gNewOrWorsening.visibility = View.GONE
                    }
                    binding.symptomsSpinner.text = "${model.size} ${getString(R.string.symptoms_selected)}"
                }
            }
            model.forEach {
                nurseMedicalReviewViewModel.nurseMrRequestModel.symptomsLog?.symptoms?.add(
                    Symptom(name = it.symptom, type = it.type, id = it.id),
                )
            }
        }
    }

    private fun updateDiagnosis(resourceState: Resource<PatientDetailsModel>) {
        when (resourceState.state) {
            ResourceState.SUCCESS -> {
                hideLoading()
                resourceState.data?.let { details ->
                    updateSpinnerValues(details.unselectedDiagnosis)
                }
            }

            ResourceState.LOADING -> {
                showLoading()
            }

            ResourceState.ERROR -> {
                hideLoading()
                resourceState.message?.let {
                    (activity as BaseActivity).showErrorDialogue(
                        getString(R.string.error),
                        it,
                        false,
                    ) {}
                }
            }
        }
    }

    private fun updateSpinnerValues(unselectedDiagnosis: ArrayList<UnselectedDiagnosis>?) {
        val unSelectedList = ArrayList<String>()
        unselectedDiagnosis?.forEach {
            if (!SecuredPreference.getIsTranslationEnabled()) {
                it.name?.let { it1 -> unSelectedList.add(it1) }
            } else {
                it.cultureValue?.let { it1 -> unSelectedList.add(it1) }
            }
        }

        diagnosisSpinner(unSelectedList, unselectedDiagnosis)
    }

    private fun spinner() {
        val adapter = CustomSpinnerAdapter(requireContext(), translate = true)
        val dropDownList = ArrayList<Map<String, Any>>()
        dropDownList.add(
            hashMapOf(
                DefinedParams.NAME to DefinedParams.SELECT,
                DefinedParams.ID to 0,
            ),
        )
        dropDownList.add(
            hashMapOf(
                DefinedParams.NAME to DefinedParams.YES.capitalizeFirstChar(),
                DefinedParams.ID to 1,
                DefinedParams.CULTURE_VALUE to getString(R.string.yes),
            ),
        )
        dropDownList.add(
            hashMapOf<String, Any>(
                DefinedParams.NAME to DefinedParams.NO.capitalizeFirstChar(),
                DefinedParams.ID to 2,
                DefinedParams.CULTURE_VALUE to getString(R.string.no),
            ),
        )

        adapter.setData(dropDownList)
        binding.spinnerMedication.adapter = adapter
        binding.spinnerMedication.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    view: View?,
                    pos: Int,
                    itemId: Long,
                ) {
                    val selectedItem = adapter.getData(position = pos)
                    val selectedId = selectedItem?.get(DefinedParams.NAME) as? String ?: ""
                    if (selectedItem != null && selectedItem.containsKey(DefinedParams.ID)) {
                        if (selectedItem[DefinedParams.ID] as Int != 0) {
                            nurseMedicalReviewViewModel.selectedSymptomsAndAdherenceLiveData.value = selectedId
                            nurseMedicalReviewViewModel.selectedSymptomsAndAdherence = selectedId
                            binding.tvTakenMedicationErrorMessage.visibility = View.GONE
                        } else if (selectedItem[DefinedParams.ID] as Int == 0) {
                            nurseMedicalReviewViewModel.selectedSymptomsAndAdherenceLiveData.value = selectedId
                            nurseMedicalReviewViewModel.selectedSymptomsAndAdherence = null
                        } else {
                            binding.tvTakenMedications.visibility = View.GONE // Visible
                            binding.etTakenMedication.visibility = View.GONE // Visible
                            binding.etTakenMedication.text?.clear()
                            binding.tvTakenMedicationErrorMessage.visibility = View.GONE
                            nurseMedicalReviewViewModel.selectedSymptomsAndAdherence = null
                        }
                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    binding.tvTakenMedicationErrorMessage.visibility = View.VISIBLE
                    nurseMedicalReviewViewModel.selectedSymptomsAndAdherence = null
                }
            }
    }

    private fun addCustomView(
        data: ArrayList<Map<String, Any>>,
        tag: String,
        hashMap: HashMap<String, Any>,
        callback: ((selectedID: Any?, elementId: String, serverViewModel: FormLayout, name: String?) -> Unit)?,
        container: ViewGroup,
    ) {
        binding.root.context?.let {
            SingleSelectionCustomView(it).apply {
                this.tag = tag
                addViewElements(
                    optionList = data,
                    translate = false,
                    resultMap = hashMap,
                    elementID = tag,
                    serverViewModel = FormLayout(
                        viewType = "",
                        id = "",
                        title = "",
                        visibility = "",
                        optionsList = null,
                    ),
                    callback,
                )
                container.addView(this)
            }
        }
    }

    private val selectionCallBack: (selectedID: Any?, elementId: String, serverViewModel: FormLayout, name: String?) -> Unit =
        { selectedID, _, _, _ ->
            nurseMedicalReviewViewModel.worseningSymptomsSelections[DefinedParams.WORSENING_SYMPTOMS] = selectedID as String
            val hasYes = nurseMedicalReviewViewModel.worseningSymptomsSelections.values.any { it == getString(R.string.yes) }
            if (hasYes) {
                binding.symptomsSpinner.visibility = View.VISIBLE
                binding.tvSelectSymptoms.visibility = View.VISIBLE
            } else {
                assessmentViewModel.selectedSymptoms.value = emptyList()
                binding.symptomsSpinner.visibility = View.GONE
                binding.tvSelectSymptoms.visibility = View.GONE
            }
        }

    private fun getData(): ArrayList<Map<String, Any>> {
        val flowList = ArrayList<Map<String, Any>>()
        flowList.add(getOptionMap(getString(R.string.yes), getString(R.string.yes)))
        flowList.add(getOptionMap(getString(R.string.no), getString(R.string.no)))
        return flowList
    }

    fun getOptionMap(
        value: String,
        name: String,
    ): Map<String, Any> {
        val map = HashMap<String, Any>()
        map[DefinedParams.ID] = value
        map[DefinedParams.NAME] = name
        return map
    }

    fun validation(): Boolean {
        var isValid = true
        val selectedWorseningSymptom = nurseMedicalReviewViewModel.worseningSymptomsSelections[DefinedParams.WORSENING_SYMPTOMS] as? String

        if (selectedWorseningSymptom.isNullOrEmpty()) {
            isValid = false
            binding.tvWorseningSymptomsErrorMessage.visibility = View.VISIBLE
        } else {
            nurseMedicalReviewViewModel.nurseMrRequestModel.symptomsLog?.hasSymptoms =
                if (selectedWorseningSymptom.equals(getString(R.string.yes), true))true else false
            binding.tvWorseningSymptomsErrorMessage.visibility = View.GONE
        }
        if (binding.symptomsSpinner.visibility == View.VISIBLE && assessmentViewModel.selectedSymptoms.value.isNullOrEmpty()) {
            isValid = false
            binding.tvErrorMessage.visibility = View.VISIBLE
        } else {
            binding.tvErrorMessage.visibility = View.GONE
        }
        if (binding.etNewOrWorsening.visibility == View.VISIBLE && binding.etNewOrWorsening.text.isNullOrEmpty()) {
            isValid = false
            binding.tvNewOrWorseningErrorMessage.requestFocus()
            binding.tvNewOrWorseningErrorMessage.visibility = View.VISIBLE
        } else {
            binding.tvNewOrWorseningErrorMessage.visibility = View.GONE
        }
        if (nurseMedicalReviewViewModel.selectedSymptomsAndAdherence.isNullOrEmpty()) {
            isValid = false
            binding.tvMedicationErrorMessage.visibility = View.VISIBLE
        } else {
            nurseMedicalReviewViewModel.nurseMrRequestModel.symptomsLog?.compliance = nurseMedicalReviewViewModel.selectedSymptomsAndAdherence
            binding.tvMedicationErrorMessage.visibility = View.GONE
        }
        val validDays = 30
        val takenMedicationDays = binding.etTakenMedication.text
            .toString()
            .trim()
        val enteredDays = takenMedicationDays.toIntOrNull() ?: -1
        if (binding.etTakenMedication.visibility == View.VISIBLE) {
            if (binding.etTakenMedication.text.isNullOrEmpty()) {
                isValid = false
                binding.tvTakenMedicationErrorMessage.visibility = View.VISIBLE
            } else if (enteredDays > validDays) {
                isValid = false
                binding.tvTakenMedicationErrorMessage.visibility = View.VISIBLE
            } else {
                nurseMedicalReviewViewModel.nurseMrRequestModel.symptomsLog?.medicationTakenDays =
                    binding.etTakenMedication.text
                        .toString()
                        .trim()
                        .toDoubleOrNull()
                binding.tvTakenMedicationErrorMessage.visibility = View.GONE
            }
        } else {
            binding.tvTakenMedicationErrorMessage.visibility = View.GONE
        }

        return isValid
    }

    private fun diagnosisSpinner(
        list: ArrayList<String>?,
        completeList: ArrayList<UnselectedDiagnosis>?,
    ) {
        val adapter = CustomSpinnerAdapter(requireContext())
        val dropDownList = ArrayList<Map<String, Any>>()
        dropDownList.add(
            hashMapOf<String, Any>(
                DefinedParams.NAME to DefinedParams.DEFAULT_ID_LABEL,
                DefinedParams.ID to DefinedParams.DEFAULT_SELECT_ID,
            ),
        )
        list
            ?.filter { it != DefinedParams.OTHER.capitalizeFirstChar() }
            ?.forEach {
                dropDownList.add(
                    hashMapOf(
                        DefinedParams.NAME to it,
                        DefinedParams.ID to it as String,
                    ),
                )
            }

        adapter.setData(dropDownList)
        binding.etNewNcd.adapter = adapter
        binding.etNewNcd.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    view: View?,
                    pos: Int,
                    itemId: Long,
                ) {
                    val selectedItem =
                        adapter.getData(position = binding.etNewNcd.selectedItemPosition)
                    medicalReviewBaseViewModel.selectedDiagnoses =
                        selectedItem as HashMap<String, Any>
                    // Get the Bengali diagnosis value
                    val bengaliDiagnosis = selectedItem[DefinedParams.NAME]?.toString()

// Find the corresponding English name from the list
                    val englishDiagnosis =
                        completeList?.find { it.cultureValue == bengaliDiagnosis }?.name

                    if (selectedItem[DefinedParams.NAME] != DefinedParams.DEFAULT_ID_LABEL && !englishDiagnosis.isNullOrBlank()) {
                        // A "None" selection (or the default) means no new NCD developed,
                        // so send an empty list instead of ["none"].
                        nurseMedicalReviewViewModel.nurseMrRequestModel.confirmDiagnosis =
                            if (englishDiagnosis.equals(DefinedParams.NONE, ignoreCase = true)) {
                                arrayListOf()
                            } else {
                                arrayListOf(englishDiagnosis)
                            }
                    } else {
                        nurseMedicalReviewViewModel.nurseMrRequestModel.confirmDiagnosis =
                            arrayListOf()
                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    // No action required
                }
            }
    }

    fun resetSelection() {
        nurseMedicalReviewViewModel.worseningSymptomsSelections.clear()
        if (!::binding.isInitialized) return
        addCustomView(
            getData(),
            DefinedParams.WORSENING_SYMPTOMS,
            nurseMedicalReviewViewModel.worseningSymptomsSelections,
            selectionCallBack,
            binding.llWorseningSymptoms,
        )
    }
}
