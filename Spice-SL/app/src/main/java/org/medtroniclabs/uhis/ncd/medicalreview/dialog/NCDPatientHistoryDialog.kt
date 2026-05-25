package org.medtroniclabs.uhis.ncd.medicalreview.dialog

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.Group
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.loadAsGif
import org.medtroniclabs.uhis.appextensions.resetImageView
import org.medtroniclabs.uhis.appextensions.setDialogPercent
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DateUtils.getCurrentYearAsDouble
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.offlinesync.model.ProvanceDto
import org.medtroniclabs.uhis.databinding.FragmentNcdPatientHistoryDialogBinding
import org.medtroniclabs.uhis.db.entity.NCDDiagnosisEntity
import org.medtroniclabs.uhis.formgeneration.extension.markMandatory
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.model.FormLayout
import org.medtroniclabs.uhis.formgeneration.ui.SingleSelectionCustomView
import org.medtroniclabs.uhis.formgeneration.utility.CustomSpinnerAdapter
import org.medtroniclabs.uhis.mappingkey.Screening.Female
import org.medtroniclabs.uhis.mappingkey.Screening.Male
import org.medtroniclabs.uhis.ncd.data.NCDPatientStatusRequest
import org.medtroniclabs.uhis.ncd.data.NcdPatientStatus
import org.medtroniclabs.uhis.ncd.medicalreview.NCDDialogDismissListener
import org.medtroniclabs.uhis.ncd.medicalreview.NCDMRUtil
import org.medtroniclabs.uhis.ncd.medicalreview.viewmodel.NCDMedicalReviewViewModel
import org.medtroniclabs.uhis.ncd.medicalreview.viewmodel.NCDPatientHistoryViewModel
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.medicalreview.motherneonate.anc.MotherNeonateUtil

@AndroidEntryPoint
class NCDPatientHistoryDialog : DialogFragment(), View.OnClickListener {
    var listener: NCDDialogDismissListener? = null
    private lateinit var binding: FragmentNcdPatientHistoryDialogBinding
    private val viewModel: NCDPatientHistoryViewModel by viewModels()
    private val medicalReviewViewModel: NCDMedicalReviewViewModel by activityViewModels()
    val adapter by lazy { CustomSpinnerAdapter(requireContext(), SecuredPreference.getIsTranslationEnabled()) }

    override fun onStart() {
        super.onStart()
        handleOrientation()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        handleOrientation()
    }

    private fun handleOrientation() {
        val isTablet = CommonUtils.checkIsTablet(requireContext())
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val width = when {
            isTablet && isLandscape -> 50
            else -> 100
        }
        val height = when {
            isTablet && isLandscape -> 70
            else -> 100
        }
        setDialogPercent(width, height)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentNcdPatientHistoryDialogBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        isCancelable = false
        return binding.root
    }

    companion object {
        const val TAG = "NCDPatientHistoryDialog"
        const val Diabetes = "Diabetes"
        const val Hypertension = "Hypertension"
        const val N_A = "N/A"
        const val New_Patient = "New Patient"
        const val Known_patient = "Known Patient"
        const val Newly_Diagnosed = "Newly Diagnosed"

        fun newInstance(
            visitId: String?,
            patientReference: String?,
            memberReference: String?,
            isFemale: Boolean,
            isPregnant: Boolean,
        ): NCDPatientHistoryDialog =
            NCDPatientHistoryDialog().apply {
                arguments = Bundle().apply {
                    putString(NCDMRUtil.VISIT_ID, visitId)
                    putString(NCDMRUtil.PATIENT_REFERENCE, patientReference)
                    putString(NCDMRUtil.MEMBER_REFERENCE, memberReference)
                    putBoolean(NCDMRUtil.IS_FEMALE, isFemale)
                    putBoolean(NCDMRUtil.IsPregnant, isPregnant)
                }
            }
    }

    private fun getPatientReference(): String? = arguments?.getString(NCDMRUtil.PATIENT_REFERENCE)

    private fun getVisitId(): String? = arguments?.getString(NCDMRUtil.VISIT_ID)

    private fun getMemberReference(): String? = arguments?.getString(NCDMRUtil.MEMBER_REFERENCE)

    private fun getGender(): String =
        if (arguments?.getBoolean(NCDMRUtil.IS_FEMALE) == true) {
            Female.lowercase()
        } else {
            Male.lowercase()
        }

    private fun isPregnant(): Boolean = arguments?.getBoolean(NCDMRUtil.IsPregnant) ?: false

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        attachObserver()
        setListeners()
    }

    private fun prefill() {
        medicalReviewViewModel.ncdPatientDiagnosisStatus.value?.data?.let { responseMap ->
            viewModel.patientStatusId = responseMap[DefinedParams.ID] as? String
            (responseMap[NCDMRUtil.NCDPatientStatus] as? Map<*, *>)?.let { ncdMap ->
                viewModel.id = ncdMap[DefinedParams.ID] as? String
                // Diabetes
                (ncdMap[NCDMRUtil.DiabetesStatus] as? String)?.let { diabetesStatus ->
                    with(binding.ncdDiabetesHypertension.llDiabetes) {
                        if (childCount > 0) {
                            (getChildAt(0) as? SingleSelectionCustomView)?.singleSelectionAutofill(
                                diabetesStatus + "_$Diabetes",
                            )
                        }
                    }
                }
                (ncdMap[NCDMRUtil.DiabetesYearOfDiagnosis] as? String)?.let { diabetesYear ->
                    binding.ncdDiabetesHypertension.etYearOfDiagnosis.setText(diabetesYear)
                }
                (ncdMap[NCDMRUtil.DiabetesControlledType] as? String)?.let { diabetesType ->
                    (binding.ncdDiabetesHypertension.tvDiabetesControlledSpinner.adapter as? CustomSpinnerAdapter)?.let {
                        it.getIndexOfItemByName(diabetesType).let { pos ->
                            if (pos > 0) {
                                binding.ncdDiabetesHypertension.tvDiabetesControlledSpinner.post {
                                    binding.ncdDiabetesHypertension.tvDiabetesControlledSpinner.setSelection(
                                        pos,
                                        false,
                                    )
                                }
                            }
                        }
                    }
                }

                // Hypertension
                (ncdMap[NCDMRUtil.HypertensionStatus] as? String)?.let { hypertensionStatus ->
                    with(binding.ncdDiabetesHypertension.llHypertension) {
                        if (childCount > 0) {
                            (getChildAt(0) as? SingleSelectionCustomView)?.singleSelectionAutofill(
                                hypertensionStatus + "_$Hypertension",
                            )
                        }
                    }
                }
                (ncdMap[NCDMRUtil.HypertensionYearOfDiagnosis] as? String)?.let { hypertensionYear ->
                    binding.ncdDiabetesHypertension.etYearOfDiagnosisHtn.setText(hypertensionYear)
                }
            }
        }
    }

    private fun attachObserver() {
        viewModel.getSymptomListByTypeForNCDLiveData.observe(viewLifecycleOwner) {
            medicalReviewViewModel.validationForStatus = it
            loadSiteDetails(ArrayList(it))
        }
        viewModel.createNCDPatientStatus.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    listener?.onDialogDismissed(true)
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }
            }
        }
    }

    private fun setListeners() {
        binding.ncdDiabetesHypertension.tvDiabetesControlledSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    view: View?,
                    pos: Int,
                    itemId: Long,
                ) {
                    adapter.getData(pos)?.let {
                        val selectedId = (it[DefinedParams.ID] as? Long) ?: -1L
                        val value = it[DefinedParams.Value] as String?
                        if (selectedId != -1L) {
                            viewModel.value = value
                        } else {
                            viewModel.value = null
                        }
                        medicalReviewViewModel.statusDiabetesValue = viewModel.value
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    private fun loadSiteDetails(data: ArrayList<NCDDiagnosisEntity>) {
        val list = arrayListOf<Map<String, Any>>(
            hashMapOf(
                DefinedParams.NAME to getString(R.string.please_select),
                DefinedParams.ID to DefinedParams.DEFAULT_SELECT_ID,
            ),
        )

        data.mapNotNullTo(list) { symptoms ->
            hashMapOf<String, Any>()
                .apply {
                    put(DefinedParams.ID, symptoms.id)
                    put(DefinedParams.NAME, symptoms.name)
                    symptoms.displayValue?.let { put(DefinedParams.CULTURE_VALUE, it) }
                    symptoms.value?.let { put(DefinedParams.Value, it) }
                }.takeIf { it.isNotEmpty() }
        }
        adapter.setData(list)
        binding.ncdDiabetesHypertension.tvDiabetesControlledSpinner.post {
            binding.ncdDiabetesHypertension.tvDiabetesControlledSpinner.setSelection(0, false)
        }
        binding.ncdDiabetesHypertension.tvDiabetesControlledSpinner.adapter = adapter

        prefill()
    }

    private fun initView() {
        binding.apply {
            ncdDiabetesHypertension.apply {
                tvDiabetes.markMandatory()
                tvYearOfDiagnosis.markMandatory()
                tvYearOfDiagnosisHtn.markMandatory()
                tvDiabetesControlledTypeLabel.markMandatory()
                tvHypertension.markMandatory()
                MotherNeonateUtil.initTextWatcherForString(etYearOfDiagnosis) {
                    viewModel.yearForDiabetes = it
                }
                MotherNeonateUtil.initTextWatcherForString(etYearOfDiagnosisHtn) {
                    viewModel.yearForHypertension = it
                }
            }
            btnConfirm.text = getString(R.string.save)
            btnConfirm.isEnabled = true
            btnConfirm.safeClickListener(this@NCDPatientHistoryDialog)
            btnCancel.safeClickListener(this@NCDPatientHistoryDialog)
            ivClose.safeClickListener(this@NCDPatientHistoryDialog)
        }
        viewModel.getSymptoms(Diabetes.lowercase(), getGender(), isPregnant())

        getSingleSelectionOptions().let {
            val view = SingleSelectionCustomView(requireContext())
            view.tag = Diabetes
            view.addViewElements(
                it,
                SecuredPreference.getIsTranslationEnabled(),
                viewModel.resultDiabetesHashMap,
                Pair(Diabetes, null),
                FormLayout(viewType = "", id = "", title = "", visibility = "", optionsList = null),
                singleSelectionCallbackForDiabetes,
            )
            binding.ncdDiabetesHypertension.llDiabetes.addView(view)
        }

        getSingleSelectionOptions().let {
            val view = SingleSelectionCustomView(requireContext())
            view.tag = Hypertension
            view.addViewElements(
                it,
                SecuredPreference.getIsTranslationEnabled(),
                viewModel.resultHypertensionHashMap,
                Pair(Hypertension, null),
                FormLayout(viewType = "", id = "", title = "", visibility = "", optionsList = null),
                singleSelectionCallbackForHypertension,
            )
            binding.ncdDiabetesHypertension.llHypertension.addView(view)
        }
    }

    private var singleSelectionCallbackForDiabetes: ((selectedID: Any?, elementId: Pair<String, String?>, formLayout: FormLayout, name: String?) -> Unit)? =
        { selectedID, _, _, _ ->
            viewModel.resultDiabetesHashMap[Diabetes] = selectedID as String
            showViews(
                binding.ncdDiabetesHypertension.groupYearOfDiagnosis,
                selectedID,
                binding.ncdDiabetesHypertension.tvYearOfDiagnosisError,
                binding.ncdDiabetesHypertension.etYearOfDiagnosis,
            )
            showSpinnerView(selectedID)
        }

    private var singleSelectionCallbackForHypertension: ((selectedID: Any?, elementId: Pair<String, String?>, formLayout: FormLayout, name: String?) -> Unit)? =
        { selectedID, _, _, _ ->
            viewModel.resultHypertensionHashMap[Hypertension] =
                selectedID as String
            showViews(
                binding.ncdDiabetesHypertension.groupYearOfDiagnosis2,
                selectedID,
                binding.ncdDiabetesHypertension.tvYearOfDiagnosisErrorHtn,
                binding.ncdDiabetesHypertension.etYearOfDiagnosisHtn,
            )
        }

    private fun showViews(
        groupYearOfDiagnosis: Group,
        selectedValue: String,
        tvYearOfDiagnosis: AppCompatTextView,
        etYearOfDiagnosis: AppCompatEditText,
    ) {
        if (selectedValue.equals(Known_patient, true)) {
            groupYearOfDiagnosis.isVisible = true
            etYearOfDiagnosis.text = null
        } else {
            groupYearOfDiagnosis.isVisible = false
            etYearOfDiagnosis.text = null
        }
        tvYearOfDiagnosis.gone()
    }

    private fun showSpinnerView(selectedValue: String) {
        val isKnownPatient = selectedValue.equals(Known_patient, ignoreCase = true)
        with(binding.ncdDiabetesHypertension) {
            groupDiabetesSpinner.isVisible = isKnownPatient
            if (!isKnownPatient) {
                etYearOfDiagnosis.setText("")
                viewModel.value = null
                tvDiabetesControlledSpinner.post {
                    tvDiabetesControlledSpinner.setSelection(0, false)
                }
            }
            tvDiabetesControlledError.gone()
        }
    }

    private fun getSingleSelectionOptions(): ArrayList<Map<String, Any>> {
        val yearOfDiagnosis = ArrayList<Map<String, Any>>()
        yearOfDiagnosis.add(
            CommonUtils.getOptionMap(
                N_A,
                N_A,
                getString(R.string.na),
            ),
        )
        yearOfDiagnosis.add(
            CommonUtils.getOptionMap(
                Newly_Diagnosed,
                Newly_Diagnosed,
                getString(R.string.newly_Diagnosed),
            ),
        )
        yearOfDiagnosis.add(
            CommonUtils.getOptionMap(
                Known_patient,
                Known_patient,
                getString(R.string.known_patient),
            ),
        )
        return yearOfDiagnosis
    }

    fun validateInput(): Boolean {
        val isDiabetesValid = viewModel.resultDiabetesHashMap.isNotEmpty()
        val isHypertensionValid = viewModel.resultHypertensionHashMap.isNotEmpty()
        val isValueValid = !viewModel.value.isNullOrBlank()

        if (isDiabetesValid) {
            binding.ncdDiabetesHypertension.tvDiabetesError.gone()
        } else {
            binding.ncdDiabetesHypertension.tvDiabetesError.visible()
        }

        if (isHypertensionValid) {
            binding.ncdDiabetesHypertension.tvHypertensionError.gone()
        } else {
            binding.ncdDiabetesHypertension.tvHypertensionError.visible()
        }

        val isKnownDiabetesPatient =
            (viewModel.resultDiabetesHashMap[Diabetes] as? String)?.equals(
                Known_patient,
                true,
            ) == true

        if (isKnownDiabetesPatient && isValueValid) {
            binding.ncdDiabetesHypertension.tvDiabetesControlledError.gone()
        } else {
            if (isKnownDiabetesPatient) {
                binding.ncdDiabetesHypertension.tvDiabetesControlledError.visible()
            }
        }

        val isKnownHypertensionPatient =
            (viewModel.resultHypertensionHashMap[Hypertension] as? String)?.equals(
                Known_patient,
                true,
            ) == true

        val knownPatientValidForDiabetes =
            (!isKnownDiabetesPatient || (isValidDiagnosis() && isValueValid))
        val knownPatientValidForHypertension =
            (!isKnownHypertensionPatient || isValidDiagnosisTwo())
        return isDiabetesValid &&
            isHypertensionValid &&
            knownPatientValidForDiabetes &&
            knownPatientValidForHypertension
    }

    private fun isValidDiagnosis(): Boolean =
        MotherNeonateUtil.isValidInput(
            binding.ncdDiabetesHypertension.etYearOfDiagnosis.text
                .toString(),
            binding.ncdDiabetesHypertension.etYearOfDiagnosis,
            binding.ncdDiabetesHypertension.tvYearOfDiagnosisError,
            1920.0..getCurrentYearAsDouble(),
            R.string.error_label,
            true,
            requireContext(),
        )

    private fun isValidDiagnosisTwo(): Boolean =
        MotherNeonateUtil.isValidInput(
            binding.ncdDiabetesHypertension.etYearOfDiagnosisHtn.text
                .toString(),
            binding.ncdDiabetesHypertension.etYearOfDiagnosisHtn,
            binding.ncdDiabetesHypertension.tvYearOfDiagnosisErrorHtn,
            1900.0..getCurrentYearAsDouble(),
            R.string.error_label,
            true,
            requireContext(),
        )

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.btnConfirm.id -> {
                if (validateInput()) {
                    // Do the API call
                    val request = NCDPatientStatusRequest(
                        id = viewModel.patientStatusId,
                        provenance = ProvanceDto(),
                        patientReference = getPatientReference(),
                        memberReference = getMemberReference(),
                        patientVisitId = getVisitId(),
                        ncdPatientStatus = NcdPatientStatus(
                            id = viewModel.id,
                            diabetesStatus = viewModel.resultDiabetesHashMap[Diabetes] as? String,
                            hypertensionStatus = viewModel.resultHypertensionHashMap[Hypertension] as? String,
                            hypertensionYearOfDiagnosis = viewModel.yearForHypertension.takeIf { !it.isNullOrBlank() },
                            diabetesYearOfDiagnosis = viewModel.yearForDiabetes.takeIf { !it.isNullOrBlank() },
                            diabetesControlledType = null,
                            diabetesDiagnosis = viewModel.value,
                        ),
                    )
                    viewModel.createNCDPatientStatus(request)
                }
            }

            binding.btnCancel.id, binding.ivClose.id -> {
                listener?.closePage()
            }
        }
    }

    private fun showLoading() {
        binding.loadingProgress.visibility = View.VISIBLE
        binding.loaderImage.apply {
            loadAsGif(R.drawable.ic_rotating_uhis_logo)
        }
    }

    private fun hideLoading() {
        binding.loadingProgress.visibility = View.GONE
        binding.loaderImage.apply {
            resetImageView()
        }
    }
}
