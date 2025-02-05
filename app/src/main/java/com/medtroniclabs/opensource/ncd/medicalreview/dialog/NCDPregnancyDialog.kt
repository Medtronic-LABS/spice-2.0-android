package com.medtroniclabs.opensource.ncd.medicalreview.dialog

import android.app.DatePickerDialog
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.Group
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.hideKeyboard
import com.medtroniclabs.opensource.appextensions.invisible
import com.medtroniclabs.opensource.appextensions.isVisible
import com.medtroniclabs.opensource.appextensions.loadAsGif
import com.medtroniclabs.opensource.appextensions.postSuccess
import com.medtroniclabs.opensource.appextensions.resetImageView
import com.medtroniclabs.opensource.appextensions.setDialogPercent
import com.medtroniclabs.opensource.appextensions.setVisible
import com.medtroniclabs.opensource.appextensions.takeIfNotNull
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.DateUtils.DATE_FORMAT_ddMMyyyy
import com.medtroniclabs.opensource.common.DateUtils.DATE_FORMAT_yyyyMMdd
import com.medtroniclabs.opensource.common.DateUtils.DATE_ddMMyyyy
import com.medtroniclabs.opensource.common.DateUtils.calculateGestationalAge
import com.medtroniclabs.opensource.common.DateUtils.formatGestationalAge
import com.medtroniclabs.opensource.common.DateUtils.getCurrentYearAsDouble
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.common.ViewUtils
import com.medtroniclabs.opensource.data.PregnancyDetailsModel
import com.medtroniclabs.opensource.data.model.ChipViewItemModel
import com.medtroniclabs.opensource.databinding.DialogNcdPregnancyBinding
import com.medtroniclabs.opensource.db.entity.NCDDiagnosisEntity
import com.medtroniclabs.opensource.formgeneration.extension.markMandatory
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.formgeneration.model.FormLayout
import com.medtroniclabs.opensource.formgeneration.ui.SingleSelectionCustomView
import com.medtroniclabs.opensource.formgeneration.utility.CustomSpinnerAdapter
import com.medtroniclabs.opensource.mappingkey.Screening.Female
import com.medtroniclabs.opensource.mappingkey.Screening.Male
import com.medtroniclabs.opensource.ncd.data.NcdPatientStatus
import com.medtroniclabs.opensource.ncd.medicalreview.CommonEnums
import com.medtroniclabs.opensource.ncd.medicalreview.NCDDialogDismissListener
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil
import com.medtroniclabs.opensource.ncd.medicalreview.dialog.NCDPatientHistoryDialog.Companion.Diabetes
import com.medtroniclabs.opensource.ncd.medicalreview.dialog.NCDPatientHistoryDialog.Companion.Hypertension
import com.medtroniclabs.opensource.ncd.medicalreview.dialog.NCDPatientHistoryDialog.Companion.Known_patient
import com.medtroniclabs.opensource.ncd.medicalreview.viewmodel.NCDMedicalReviewViewModel
import com.medtroniclabs.opensource.ncd.medicalreview.viewmodel.NCDPregnancyViewModel
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.TagListCustomView
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.MotherNeonateUtil
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.MotherNeonateUtil.EstimatedDeliveryDate
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.MotherNeonateUtil.initTextWatcherForDouble
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.MotherNeonateUtil.initTextWatcherForInt
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.PatientDetailViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class NCDPregnancyDialog(private val callback: ((isPositiveResult: Boolean, message: String) -> Unit)) :
    DialogFragment(), View.OnClickListener {
    var listener: NCDDialogDismissListener? = null
    private lateinit var binding: DialogNcdPregnancyBinding
    private val viewModel: NCDPregnancyViewModel by viewModels()
    private val patientDetailViewModel: PatientDetailViewModel by activityViewModels()
    private val medicalReviewViewModel: NCDMedicalReviewViewModel by viewModels()

    private lateinit var neonatalOutcomesView: TagListCustomView
    private lateinit var maternalOutcomesView: TagListCustomView
    private var datePickerDialog: DatePickerDialog? = null

    val adapter by lazy { CustomSpinnerAdapter(requireContext()) }

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
            isTablet && isLandscape -> 70
            else -> 100
        }
        val height = when {
            isTablet && isLandscape -> 95
            else -> 100
        }
        setDialogPercent(width, height)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DialogNcdPregnancyBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        isCancelable = false
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        clickListener()
        attachObserver()
    }

    private fun prefill(responseMap: HashMap<String, Any>) {
        (responseMap[NCDMRUtil.NCDPatientStatus] as? Map<*, *>)?.let { ncdMap ->
            viewModel.id = ncdMap[DefinedParams.ID] as? String
            //Diabetes
            (ncdMap[NCDMRUtil.DiabetesStatus] as? String)?.let { diabetesStatus ->
                with(binding.ncdDiabetesHypertension.llDiabetes) {
                    if (childCount > 0) (getChildAt(0) as? SingleSelectionCustomView)?.singleSelectionAutofill(
                        diabetesStatus + "_${DIABETES}"
                    )
                }
            }
            (ncdMap[NCDMRUtil.DiabetesYearOfDiagnosis] as? String)?.let { diabetesYear ->
                binding.ncdDiabetesHypertension.etYearOfDiagnosis.setText(diabetesYear)
            }
            (ncdMap[NCDMRUtil.DiabetesControlledType] as? String)?.let { diabetesType ->
                (binding.ncdDiabetesHypertension.tvDiabetesControlledSpinner.adapter as? CustomSpinnerAdapter)?.let {
                    it.getIndexOfItemByName(diabetesType).let { pos ->
                        if (pos > 0)
                            binding.ncdDiabetesHypertension.tvDiabetesControlledSpinner.post {
                                binding.ncdDiabetesHypertension.tvDiabetesControlledSpinner.setSelection(
                                    pos,
                                    false
                                )
                            }
                    }
                }
            }


            //Hypertension
            (ncdMap[NCDMRUtil.HypertensionStatus] as? String)?.let { hypertensionStatus ->
                with(binding.ncdDiabetesHypertension.llHypertension) {
                    if (childCount > 0) (getChildAt(0) as? SingleSelectionCustomView)?.singleSelectionAutofill(
                        hypertensionStatus + "_${HYPERTENSION}"
                    )
                }
            }
            (ncdMap[NCDMRUtil.HypertensionYearOfDiagnosis] as? String)?.let { hypertensionYear ->
                binding.ncdDiabetesHypertension.etYearOfDiagnosisHtn.setText(hypertensionYear)
            }
        }
    }

    private fun initView() {
        ncdVisibility()
        viewModel.relatedPersonFhirId = arguments?.getString(PATIENT_ID)
        with(binding) {
            tvPregnant.markMandatory()

            initTextWatcherForInt(etGravida) {
                viewModel.ncdPregnancyCreateModel.gravida = it
            }
            initTextWatcherForInt(etParity) {
                viewModel.ncdPregnancyCreateModel.parity = it
            }
            initTextWatcherForInt(etTemperature) {
                viewModel.ncdPregnancyCreateModel.temperature = it
            }
            tvLastMenstrualPeriodDate.addTextChangedListener {
                binding.tvLastMenstrualPeriodError.gone()
                if (viewModel.ncdPregnancyDetailsResponse.value?.data?.estimatedDeliveryDate.isNullOrBlank()) calculateGestationalAgeAndEstimationDeliveryDate(
                    it.toString()
                )

                viewModel.ncdPregnancyCreateModel.lastMenstrualPeriod =
                    apiFormattedDate(it.toString())
            }
            tvLastMenstrualPeriodDate.safeClickListener(this@NCDPregnancyDialog)
            tvEstimatedDeliveryDate.addTextChangedListener {
                viewModel.ncdPregnancyCreateModel.estimatedDeliveryDate =
                    apiFormattedDate(it.toString())
            }
            tvGestationalText.addTextChangedListener {
                viewModel.ncdPregnancyCreateModel.gestationalAge = removeWeeksStr(it.toString())
            }
            initTextWatcherForInt(etNoFetuses) {
                viewModel.ncdPregnancyCreateModel.noOfFetus = it
            }
            initTextWatcherForDouble(etWeight) {
                viewModel.ncdPregnancyCreateModel.weight = it
            }

            if (viewModel.isPregnancyAncEnabledSite) {
                pregnancyGroup.gone()
                pregnancyAncGroup.visible()

                tvLastMenstrualPeriodLabel.markMandatory()

                neonatalOutcomesView = TagListCustomView(
                    binding.root.context, binding.cgNeonatalOutcomes
                ) { _, _, _ -> }
                neonatalOutcomesView.addChipItemList(neoNatalOutcomes())

                maternalOutcomesView = TagListCustomView(
                    binding.root.context, binding.cgMaternalOutcomes
                ) { _, _, _ -> }
                maternalOutcomesView.addChipItemList(maternalOutcomes())

                tvDeliveryDateLabel.markMandatory()
                tvDeliveryDate.addTextChangedListener {
                    viewModel.ncdPregnancyCreateModel.actualDeliveryDate =
                        if (it.isNullOrBlank()) null else apiFormattedDate(it.toString())
                }
                tvDeliveryDate.safeClickListener(this@NCDPregnancyDialog)

                viewModel.ncdPregnancyCreateModel.isPregnancyAnc = true
            } else {
                pregnancyGroup.visible()
                pregnancyAncGroup.gone()

                tvPregnancyDiagnosisLbl.markMandatory()
                tvPatientTreatmentLbl.markMandatory()

                tvDiagnosesTime.addTextChangedListener {
                    viewModel.ncdPregnancyCreateModel.diagnosisTime =
                        apiFormattedDate(it.toString())
                }
                tvDiagnosesTime.safeClickListener(this@NCDPregnancyDialog)

                viewModel.ncdPregnancyCreateModel.isPregnancyAnc = false
            }

            ncdDiabetesHypertension.apply {
                tvDiabetes.markMandatory()
                tvYearOfDiagnosis.markMandatory()
                tvDiabetesControlledTypeLabel.markMandatory()
                MotherNeonateUtil.initTextWatcherForString(etYearOfDiagnosis) {
                    viewModel.yearForDiabetes = it
                }

                tvHypertension.markMandatory()
                tvYearOfDiagnosisHtn.markMandatory()
                MotherNeonateUtil.initTextWatcherForString(etYearOfDiagnosisHtn) {
                    viewModel.yearForHypertension = it
                }
            }
            viewModel.getSymptoms(Diabetes.lowercase(), getGender(), isPregnant())

            btnCancel.safeClickListener(this@NCDPregnancyDialog)
            btnConfirm.safeClickListener(this@NCDPregnancyDialog)
            ivClose.safeClickListener(this@NCDPregnancyDialog)
        }
        getSingleSelectionOptions().let {
            val view = SingleSelectionCustomView(requireContext())
            view.tag = DIABETES
            view.addViewElements(
                it,
                SecuredPreference.getIsTranslationEnabled(),
                viewModel.resultDiabetesHashMap,
                Pair(DIABETES, null),
                FormLayout(viewType = "", id = "", title = "", visibility = "", optionsList = null),
                singleSelectionCallbackForDiabetes
            )
            binding.ncdDiabetesHypertension.llDiabetes.addView(view)
        }
        getSingleSelectionOptions().let {
            val view = SingleSelectionCustomView(requireContext())
            view.tag = HYPERTENSION
            view.addViewElements(
                it,
                SecuredPreference.getIsTranslationEnabled(),
                viewModel.resultHypertensionHashMap,
                Pair(HYPERTENSION, null),
                FormLayout(viewType = "", id = "", title = "", visibility = "", optionsList = null),
                singleSelectionCallbackForHypertension
            )
            binding.ncdDiabetesHypertension.llHypertension.addView(view)
        }
        getSingleSelectionOptionsForPregnant().let {
            val view = SingleSelectionCustomView(requireContext())
            view.tag = PREGNANT_STATUS
            view.addViewElements(
                it,
                SecuredPreference.getIsTranslationEnabled(),
                viewModel.resultPregnantHashMap,
                Pair(PREGNANT_STATUS, null),
                FormLayout(viewType = "", id = "", title = "", visibility = "", optionsList = null),
                singleSelectionCallbackForPregnant
            )
            binding.llPregnant.addView(view)
        }
        viewModel.relatedPersonFhirId?.let { id ->
            viewModel.ncdPregnancyDetails(id)
        }
        binding.loadingProgress.safeClickListener {

        }
        binding.loadingProgress.bringToFront()
    }

    private fun ncdVisibility() {
        val showNCD = showNCD()
        binding.apply {
            tvNCD.setVisible(showNCD)
            ncdDiabetesHypertension.root.setVisible(showNCD)
        }
    }

    private fun showNCD(): Boolean {
        return arguments?.getBoolean(NCDMRUtil.ShowNCD) ?: false
    }

    private fun getGender(): String {
        return if (arguments?.getBoolean(IS_FEMALE) == true) {
            Female.lowercase()
        } else {
            Male.lowercase()
        }
    }

    private fun isPregnant(): Boolean {
        return arguments?.getBoolean(IS_PREGNANT) ?: false
    }

    private fun removeWeeksStr(weeks: String): Long? {
        return weeks.split(" ")[0].toLongOrNull()
    }

    private fun apiFormattedDate(toString: String): String? {
        return DateUtils.convertDateFormat(
            toString, DATE_ddMMyyyy, DATE_FORMAT_yyyyMMdd
        ).ifBlank { null }
    }

    private var singleSelectionCallbackForDiabetes: ((selectedID: Any?, elementId: Pair<String, String?>, serverViewModel: FormLayout, name: String?) -> Unit)? =
        { selectedID, _, _, _ ->
            viewModel.resultDiabetesHashMap[DIABETES] = selectedID as String
            showViews(
                binding.ncdDiabetesHypertension.groupYearOfDiagnosis,
                selectedID,
                binding.ncdDiabetesHypertension.tvYearOfDiagnosisError,
                binding.ncdDiabetesHypertension.etYearOfDiagnosis
            )
            showSpinnerView(selectedID)
        }

    private var singleSelectionCallbackForHypertension: ((selectedID: Any?, elementId: Pair<String, String?>, serverViewModel: FormLayout, name: String?) -> Unit)? =
        { selectedID, _, _, _ ->
            viewModel.resultHypertensionHashMap[HYPERTENSION] = selectedID as String
            showViews(
                binding.ncdDiabetesHypertension.groupYearOfDiagnosis2,
                selectedID,
                binding.ncdDiabetesHypertension.tvYearOfDiagnosisErrorHtn,
                binding.ncdDiabetesHypertension.etYearOfDiagnosisHtn
            )
        }

    private fun showViews(
        groupYearOfDiagnosis: Group,
        selectedValue: String,
        tvYearOfDiagnosis: AppCompatTextView,
        etYearOfDiagnosis: AppCompatEditText
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
                etYearOfDiagnosis.setText(getString(R.string.empty))
                viewModel.value = null
                tvDiabetesControlledSpinner.post {
                    tvDiabetesControlledSpinner.setSelection(0, false)
                }
            }
            tvDiabetesControlledError.gone()
        }
    }

    private var singleSelectionCallbackForPregnant: ((selectedID: Any?, elementId: Pair<String, String?>, serverViewModel: FormLayout, name: String?) -> Unit)? =
        { selectedID, _, _, _ ->
            (selectedID as? String?)?.let { pregnantStatus ->
                val isPregnant = pregnantStatus.equals(PREGNANT, true)
                val reClick = isPregnant && binding.clPregnant.visibility == View.VISIBLE

                if (!reClick) {
                    viewModel.resultPregnantHashMap[PREGNANT_STATUS] = pregnantStatus

                    clearFields()

                    binding.clPregnant.visibility = if (isPregnant) View.VISIBLE else View.GONE
                    viewModel.ncdPregnancyCreateModel.isPregnant = isPregnant

                    binding.tvPregnantError.gone()
                }
            }
        }

    private fun neoNatalOutcomes(): ArrayList<ChipViewItemModel> {
        val chipItems = ArrayList<ChipViewItemModel>()
        chipItems.add(
            ChipViewItemModel(
                id = 1,
                name = CommonEnums.STILL_BIRTH.title,
                cultureValue = getString(CommonEnums.STILL_BIRTH.cultureValue),
                value = CommonEnums.STILL_BIRTH.value
            )
        )
        chipItems.add(
            ChipViewItemModel(
                id = 2,
                name = CommonEnums.LIVE_BIRTH.title,
                cultureValue = getString(CommonEnums.LIVE_BIRTH.cultureValue),
                value = CommonEnums.LIVE_BIRTH.value
            )
        )
        chipItems.add(
            ChipViewItemModel(
                id = 3,
                name = CommonEnums.NEO_NATAL_DEATH.title,
                cultureValue = getString(CommonEnums.NEO_NATAL_DEATH.cultureValue),
                value = CommonEnums.NEO_NATAL_DEATH.value
            )
        )
        return chipItems
    }

    private fun maternalOutcomes(): ArrayList<ChipViewItemModel> {
        val chipItems = ArrayList<ChipViewItemModel>()
        chipItems.add(
            ChipViewItemModel(
                id = 1,
                name = CommonEnums.ALIVE_AND_WELL.title,
                cultureValue = getString(CommonEnums.ALIVE_AND_WELL.cultureValue),
                value = CommonEnums.ALIVE_AND_WELL.value
            )
        )
        chipItems.add(
            ChipViewItemModel(
                id = 2,
                name = CommonEnums.MATERNAL_DEATH.title,
                cultureValue = getString(CommonEnums.MATERNAL_DEATH.cultureValue),
                value = CommonEnums.MATERNAL_DEATH.value
            )
        )
        return chipItems
    }

    private fun getSingleSelectionOptions(): ArrayList<Map<String, Any>> {
        val yearOfDiagnosis = ArrayList<Map<String, Any>>()
        yearOfDiagnosis.add(
            CommonUtils.getOptionMap(
                N_A,
                N_A,
                getString(R.string.na)
            )
        )
        yearOfDiagnosis.add(
            CommonUtils.getOptionMap(
                NEW_PATIENT,
                NEW_PATIENT,
                getString(R.string.new_patient)
            )
        )
        yearOfDiagnosis.add(
            CommonUtils.getOptionMap(
                KNOWN_PATIENT,
                KNOWN_PATIENT,
                getString(R.string.known_patient)
            )
        )
        return yearOfDiagnosis
    }

    private fun getSingleSelectionOptionsForPregnant(): ArrayList<Map<String, Any>> {
        val pregnantList = ArrayList<Map<String, Any>>()
        pregnantList.add(
            CommonUtils.getOptionMap(
                PREGNANT,
                PREGNANT,
                getString(R.string.pregnant)
            )
        )
        pregnantList.add(
            CommonUtils.getOptionMap(
                NOT_PREGNANT,
                NOT_PREGNANT,
                getString(R.string.not_pregnant)
            )
        )
        return pregnantList
    }

    private fun clickListener() {
        binding.ncdDiabetesHypertension.tvDiabetesControlledSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?, view: View?, pos: Int, itemId: Long
                ) {
                    adapter.getData(pos)?.let {
                        val selectedId = (it[DefinedParams.id] as? Long) ?: -1L
                        val selectedName = it[DefinedParams.NAME] as String?
                        val value = it[DefinedParams.Value] as String?
                        if (selectedId != -1L) {
                            viewModel.value = value
                        } else {
                            viewModel.value = null
                        }
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        binding.mcbNone.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.mcbEclampsia.isChecked = false
                binding.mcbPreEclampsia.isChecked = false
                binding.mcbGestationalDiabetes.isChecked = false
            }
            handleCheckBox()
        }
        binding.mcbEclampsia.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) binding.mcbNone.isChecked = false
            handleCheckBox()
        }
        binding.mcbPreEclampsia.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) binding.mcbNone.isChecked = false
            handleCheckBox()
        }
        binding.mcbGestationalDiabetes.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) binding.mcbNone.isChecked = false
            handleCheckBox()
        }

        binding.rgPatientTreatment.setOnCheckedChangeListener { _, selectedItemId ->
            binding.tvDiagnosesTime.text = getString(R.string.empty)
            binding.tvPatientTreatmentError.gone()
            when (selectedItemId) {
                R.id.rbYes -> {
                    viewModel.ncdPregnancyCreateModel.isOnTreatment = true
                    binding.diagnosesTimeGroup.visible()
                }

                R.id.rbNo -> {
                    viewModel.ncdPregnancyCreateModel.isOnTreatment = false
                    binding.diagnosesTimeGroup.gone()
                }

                else -> {
                    viewModel.ncdPregnancyCreateModel.isOnTreatment = null
                    binding.diagnosesTimeGroup.gone()
                }
            }
        }
        binding.cgNeonatalOutcomes.setOnCheckedStateChangeListener { _, checkedIds ->
            val selected = neonatalOutcomesView.getSelectedTags()
            viewModel.ncdPregnancyCreateModel.neonatalOutcomes = if (selected.isNotEmpty()) selected[0].name.ifBlank { null } else null
            val isNeonatalSelected = checkedIds.size > 0
            binding.actualDeliveryDateGroup.setVisible(isNeonatalSelected)
            setActualDeliveryDate(isNeonatalSelected)
        }
        binding.cgMaternalOutcomes.setOnCheckedStateChangeListener { _, _ ->
            val selected = maternalOutcomesView.getSelectedTags()
            viewModel.ncdPregnancyCreateModel.maternalOutcomes = if (selected.isNotEmpty()) selected[0].name.ifBlank { null } else null
        }
    }

    private fun setActualDeliveryDate(isNeonatalSelected: Boolean) {
        binding.tvDeliveryDate.text =
            if (isNeonatalSelected) DateUtils.getCurrentDateAndTime(DATE_ddMMyyyy) else getString(R.string.empty)
    }

    private fun handleCheckBox() {
        val showPatientTreatment =
            binding.mcbEclampsia.isChecked || binding.mcbPreEclampsia.isChecked || binding.mcbGestationalDiabetes.isChecked
        if (showPatientTreatment) {
            binding.patientTreatmentGroup.visible()
        } else {
            binding.patientTreatmentGroup.gone()
            binding.rgPatientTreatment.clearCheck()
        }
        setPregnancyDiagnoses()
    }

    private fun setPregnancyDiagnoses() {
        val selectedItems = ArrayList<Map<String, Any>>()
        if (binding.mcbNone.isChecked) selectedItems.add(
            CommonUtils.getOptionMap(
                NONE,
                NONE,
                getString(R.string.none)
            )
        )
        if (binding.mcbEclampsia.isChecked) selectedItems.add(
            CommonUtils.getOptionMap(
                ECLAMPSIA,
                ECLAMPSIA,
                getString(R.string.eclampsia)
            )
        )
        if (binding.mcbPreEclampsia.isChecked) selectedItems.add(
            CommonUtils.getOptionMap(
                PRE_ECLAMPSIA,
                PRE_ECLAMPSIA,
                getString(R.string.pre_eclampsia)
            )
        )
        if (binding.mcbGestationalDiabetes.isChecked) selectedItems.add(
            CommonUtils.getOptionMap(
                GESTATIONAL_DIABETES,
                GESTATIONAL_DIABETES,
                getString(R.string.gestational_diabetes)
            )
        )
        viewModel.ncdPregnancyCreateModel.diagnosis = selectedItems
        binding.tvPregnancyDiagnosisError.gone()
    }

    private fun attachObserver() {
        viewModel.ncdPregnancyCreateResponse.observe(viewLifecycleOwner) { resourceState ->
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
        viewModel.ncdPregnancyDetailsResponse.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    resourceState.data?.let {
                        populateFields(it)
                    }
                    hideLoading()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }
            }
        }
        viewModel.getSymptomListByTypeForNCDLiveData.observe(viewLifecycleOwner) {
            loadSiteDetails(ArrayList(it))
        }
        medicalReviewViewModel.ncdPatientDiagnosisStatus.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    resourceState.data?.let { responseMap ->
                        prefill(responseMap)
                    }
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }
            }
        }
    }

    private fun loadSiteDetails(data: ArrayList<NCDDiagnosisEntity>) {
        val list = arrayListOf<Map<String, Any>>(
            hashMapOf(
                DefinedParams.NAME to DefinedParams.DefaultIDLabel,
                DefinedParams.ID to DefinedParams.DefaultSelectID
            )
        )
        data.mapNotNullTo(list) { symptoms ->
            hashMapOf<String, Any>().apply {
                symptoms.id?.let { put(DefinedParams.ID, it) }
                symptoms.name?.let { put(DefinedParams.NAME, it) }
                symptoms.displayValue?.let { put(DefinedParams.cultureValue, it) }
                symptoms.value?.let { put(DefinedParams.Value, it) }
            }.takeIf { it.isNotEmpty() }
        }
        adapter.setData(list)
        binding.ncdDiabetesHypertension.tvDiabetesControlledSpinner.post {
            binding.ncdDiabetesHypertension.tvDiabetesControlledSpinner.setSelection(0, false)
        }
        binding.ncdDiabetesHypertension.tvDiabetesControlledSpinner.adapter = adapter

        getPatientReference()?.let { patientReference ->
            medicalReviewViewModel.ncdPatientDiagnosisStatus(HashMap<String, Any>().apply {
                put(
                    DefinedParams.PatientReference,
                    patientReference
                )
            })
        }
    }

    private fun clearFields() {
        with(binding) {
            requireContext().hideKeyboard(root)

            //Common Fields
            etGravida.text?.clear()
            tvGravidaError.gone()

            etParity.text?.clear()
            tvParityError.gone()

            etTemperature.text?.clear()
            tvTemperatureError.gone()

            tvLastMenstrualPeriodDate.text = getString(R.string.empty)

            tvEstimatedDeliveryDate.text = getString(R.string.empty)

            etNoFetuses.text?.clear()
            tvNoFetusesError.gone()

            etWeight.text?.clear()
            tvWeightError.gone()

            tvGestationalText.text = getString(R.string.hyphen_symbol)

            //Pregnancy Fields
            mcbNone.isChecked = false
            mcbEclampsia.isChecked = false
            mcbPreEclampsia.isChecked = false
            mcbGestationalDiabetes.isChecked = false

            //Pregnancy ANC Fields
            cgNeonatalOutcomes.clearCheck()
            setActualDeliveryDate(false)
            cgMaternalOutcomes.clearCheck()
        }
    }

    companion object {
        const val PATIENT_ID = "PATIENT_ID"
        const val IS_FEMALE = "IS_FEMALE"
        const val IS_PREGNANT = "IS_PREGNANT"

        const val N_A = "N/A"
        const val NEW_PATIENT = "New Patient"
        const val KNOWN_PATIENT = "Known Patient"
        const val DIABETES = "Diabetes"
        const val HYPERTENSION = "Hypertension"

        const val PREGNANT_STATUS = "PregnantStatus"
        const val PREGNANT = "Pregnant"
        const val NOT_PREGNANT = "Not Pregnant"

        const val NONE = "none"
        const val ECLAMPSIA = "eclampsia"
        const val PRE_ECLAMPSIA = "preEclampsia"
        const val GESTATIONAL_DIABETES = "gestationalDiabetes"

        const val TAG = "NCDPregnancyCreateDialog"
        fun newInstance(
            visitId:String?,
            patientReference: String?,
            patientId: String,
            isFemale: Boolean,
            isPregnant: Boolean,
            showNCD: Boolean,
            callback: ((isPositiveResult: Boolean, message: String) -> Unit)
        ): NCDPregnancyDialog {
            return NCDPregnancyDialog(callback).apply {
                arguments = Bundle().apply {
                    putString(NCDMRUtil.VISIT_ID,visitId)
                    putString(NCDMRUtil.PATIENT_REFERENCE, patientReference)
                    putString(PATIENT_ID, patientId)
                    putBoolean(IS_FEMALE, isFemale)
                    putBoolean(IS_PREGNANT, isPregnant)
                    putBoolean(NCDMRUtil.ShowNCD, showNCD)
                }
            }
        }
    }

    private fun getPatientReference():String? {
        return arguments?.getString(NCDMRUtil.PATIENT_REFERENCE)
    }

    private fun getVisitId():String? {
        return arguments?.getString(NCDMRUtil.VISIT_ID)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.btnConfirm.id -> {
                (activity as? BaseActivity?)?.withNetworkAvailability(online = {
                    if (validateInputs()) {
                        viewModel.ncdPregnancyCreateModel.apply {
                            memberReference = viewModel.relatedPersonFhirId
                            patientReference = getPatientReference()
                            ncdPatientStatus = getNcdPatientStatus()
                            patientVisitId = getVisitId()
                        }.also {
                            viewModel.ncdPregnancyCreate(it)
                        }
                    }
                })
            }

            binding.tvLastMenstrualPeriodDate.id -> showDatePickerDialog(binding.tvLastMenstrualPeriodDate)

            binding.tvDiagnosesTime.id -> showDatePickerDialog(binding.tvDiagnosesTime)

            binding.tvDeliveryDate.id -> showDatePickerDialog(binding.tvDeliveryDate)

            binding.ivClose.id, binding.btnCancel.id -> if (patientDetailViewModel.getNCDInitialMedicalReview()) dismiss() else listener?.closePage()
        }
    }

    private fun getNcdPatientStatus(): NcdPatientStatus? {
        val ncdPatientStatus = NcdPatientStatus(
            diabetesStatus = viewModel.resultDiabetesHashMap[Diabetes] as? String,
            hypertensionStatus = viewModel.resultHypertensionHashMap[Hypertension] as? String,
            hypertensionYearOfDiagnosis = viewModel.yearForHypertension.takeIf { !it.isNullOrBlank() },
            diabetesYearOfDiagnosis = viewModel.yearForDiabetes.takeIf { !it.isNullOrBlank() },
            diabetesControlledType = null,
            diabetesDiagnosis = viewModel.value
        )
        return if (ncdPatientStatus.diabetesStatus != null && ncdPatientStatus.hypertensionStatus != null) ncdPatientStatus else null
    }

    private fun validateInputs(): Boolean {
        var ncdPatientStatus : Boolean
        with(viewModel.ncdPregnancyCreateModel) {
            ncdPatientStatus = if (binding.tvNCD.isVisible()) {
                validateNCDPatientStatus()
            } else {
                true
            }

            if (isPregnant == null) {
                binding.tvPregnantError.visible()
                return false
            } else if (isPregnant == true) {
                if (viewModel.isPregnancyAncEnabledSite) {
                    if (lastMenstrualPeriod.isNullOrBlank()) {
                        binding.tvLastMenstrualPeriodError.visible()
                        return false
                    }
                } else {
                    if (diagnosis.isNullOrEmpty()) {
                        binding.tvPregnancyDiagnosisError.visible()
                        return false
                    } else {
                        diagnosis?.let { list ->
                            if (list.isNotEmpty()) {
                                val isNone = list.size == 1 && list[0][DefinedParams.Value] == NONE
                                if (!isNone && isOnTreatment == null) {
                                    binding.tvPatientTreatmentError.visible()
                                    return false
                                }
                            }
                        }
                    }
                }
            }
        }

        return ncdPatientStatus
    }

    private fun showDatePickerDialog(textView: AppCompatTextView) {
        var yearMonthDate: Triple<Int?, Int?, Int?>? = null
        if (!textView.text.isNullOrBlank()) yearMonthDate =
            DateUtils.convertedMMMToddMM(textView.text.toString())
        if (datePickerDialog == null) {
            datePickerDialog = ViewUtils.showDatePicker(context = requireContext(),
                minDate = null,
                date = yearMonthDate,
                isMenstrualPeriod = true,
                disableFutureDate = true,
                cancelCallBack = { datePickerDialog = null }) { _, year, month, dayOfMonth ->
                val stringDate = "$dayOfMonth-$month-$year"
                textView.text = DateUtils.convertDateTimeToDate(
                    stringDate, DATE_FORMAT_ddMMyyyy, DATE_ddMMyyyy
                )
                datePickerDialog = null
            }
        }
    }

    private fun calculateGestationalAgeAndEstimationDeliveryDate(lmpText: String) {
        if (lmpText.isNotEmpty()) {
            val lmpDate = LocalDate.parse(lmpText, DateTimeFormatter.ofPattern(DATE_ddMMyyyy))
            val estimatedDeliveryDate = lmpDate.plusDays(EstimatedDeliveryDate)
            val formattedEstimatedDeliveryDate =
                estimatedDeliveryDate.format(DateTimeFormatter.ofPattern(DATE_ddMMyyyy))
            binding.tvEstimatedDeliveryDate.text = formattedEstimatedDeliveryDate

            val gestationalAgeInWeeks = calculateGestationalAge(lmpDate)
            binding.tvGestationalText.text =
                formatGestationalAge(gestationalAgeInWeeks, requireContext())
        }
    }

    private fun populateFields(model: PregnancyDetailsModel) {
        with(model) {
            binding.apply {
                val pregnantTag = "${PREGNANT}_$PREGNANT_STATUS"
                val notPregnantTag = "${NOT_PREGNANT}_$PREGNANT_STATUS"

                isPregnant?.let { pregnant ->
                    llPregnant.findViewWithTag<TextView>(if (pregnant) pregnantTag else notPregnantTag)
                        ?.performClick()
                }

                etGravida.setText(model.gravida.takeIfNotNull())
                etParity.setText(model.parity.takeIfNotNull())
                etTemperature.setText(model.temperature.takeIfNotNull())
                tvLastMenstrualPeriodDate.text = model.lastMenstrualPeriod?.let {
                    DateUtils.convertDateFormat(it, DATE_FORMAT_yyyyMMdd, DATE_ddMMyyyy)
                }
                tvEstimatedDeliveryDate.text = model.estimatedDeliveryDate?.let {
                    DateUtils.convertDateFormat(it, DATE_FORMAT_yyyyMMdd, DATE_ddMMyyyy)
                }
                tvGestationalText.text = model.gestationalAge?.let {
                    formatGestationalAge(it, requireContext())
                } ?: ""
                etNoFetuses.setText(model.noOfFetus.takeIfNotNull())
                etWeight.setText(model.weight.takeIfNotNull())

                if (viewModel.isPregnancyAncEnabledSite) {
                    neonatalOutcomes?.let {
                        if (it.isNotBlank()) neonatalOutcomesView.populateChipByName(
                            cgNeonatalOutcomes, neoNatalOutcomes(), it
                        ) {
                            tvDeliveryDate.text = model.actualDeliveryDate?.let { deliveryDate ->
                                DateUtils.convertDateFormat(
                                    deliveryDate, DATE_FORMAT_yyyyMMdd, DATE_ddMMyyyy
                                )
                            }
                        }
                    }
                    maternalOutcomes?.let {
                        if (it.isNotBlank()) maternalOutcomesView.populateChipByName(
                            cgMaternalOutcomes, maternalOutcomes(), it
                        ) {}
                    }
                } else {
                    if (!diagnosis.isNullOrEmpty()) {
                        diagnosis?.forEach { map ->
                            when (map[DefinedParams.Value]) {
                                NONE -> mcbNone.isChecked = true
                                ECLAMPSIA -> mcbEclampsia.isChecked = true
                                PRE_ECLAMPSIA -> mcbPreEclampsia.isChecked = true
                                GESTATIONAL_DIABETES -> mcbGestationalDiabetes.isChecked = true
                            }
                        }
                        if (!mcbNone.isChecked) {
                            if (isOnTreatment == true) {
                                rbYes.isChecked = true
                                tvDiagnosesTime.text = model.diagnosisTime?.let {
                                    DateUtils.convertDateFormat(
                                        it, DATE_FORMAT_yyyyMMdd, DATE_ddMMyyyy
                                    )
                                }
                            } else if (isOnTreatment == false) {
                                rbNo.isChecked = true
                            }
                        }
                    }
                }
            }
            viewModel.ncdPregnancyDetailsResponse.postSuccess(null)
        }
    }

    fun showLoading() {
        binding.apply {
            btnConfirm.invisible()
            btnCancel.invisible()
            loadingProgress.visible()
            loaderImage.apply {
                loadAsGif(R.drawable.loader_spice)
            }
        }
    }

    fun hideLoading() {
        binding.apply {
            btnConfirm.visible()
            btnCancel.visible()
            loadingProgress.gone()
            loaderImage.apply {
                resetImageView()
            }
        }
    }

    private fun validateNCDPatientStatus(): Boolean {
        val isDiabetesValid = viewModel.resultDiabetesHashMap.isNotEmpty()
        val isHypertensionValid = viewModel.resultHypertensionHashMap.isNotEmpty()
        val isValueValid = !viewModel.value.isNullOrBlank()

        binding.ncdDiabetesHypertension.tvDiabetesError.setVisible(!isDiabetesValid)
        binding.ncdDiabetesHypertension.tvHypertensionError.setVisible(!isHypertensionValid)

        val isKnownDiabetesPatient =
            (viewModel.resultDiabetesHashMap[Diabetes] as? String)?.equals(
                Known_patient,
                true
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
                true
            ) == true

        val knownPatientValidForDiabetes =
            (!isKnownDiabetesPatient || (isValidDiagnosis() && isValueValid))
        val knownPatientValidForHypertension =
            (!isKnownHypertensionPatient || isValidDiagnosisTwo())
        return isDiabetesValid && isHypertensionValid && knownPatientValidForDiabetes
                && knownPatientValidForHypertension
    }

    private fun isValidDiagnosis(): Boolean {
        return MotherNeonateUtil.isValidInput(
            binding.ncdDiabetesHypertension.etYearOfDiagnosis.text.toString(),
            binding.ncdDiabetesHypertension.etYearOfDiagnosis,
            binding.ncdDiabetesHypertension.tvYearOfDiagnosisError,
            1920.0..getCurrentYearAsDouble(),
            R.string.error_label,
            true,
            requireContext()
        )
    }

    private fun isValidDiagnosisTwo(): Boolean {
        return MotherNeonateUtil.isValidInput(
            binding.ncdDiabetesHypertension.etYearOfDiagnosisHtn.text.toString(),
            binding.ncdDiabetesHypertension.etYearOfDiagnosisHtn,
            binding.ncdDiabetesHypertension.tvYearOfDiagnosisErrorHtn,
            1900.0..getCurrentYearAsDouble(),
            R.string.error_label,
            true,
            requireContext()
        )
    }
}