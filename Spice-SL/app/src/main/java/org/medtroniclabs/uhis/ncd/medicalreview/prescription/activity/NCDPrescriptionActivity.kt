package org.medtroniclabs.uhis.ncd.medicalreview.prescription.activity

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.UnderlineSpan
import android.view.View
import android.widget.AdapterView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.hideKeyboard
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.DefinedParams.SearchLength
import org.medtroniclabs.uhis.data.MedicationResponse
import org.medtroniclabs.uhis.data.MedicationSearchRequest
import org.medtroniclabs.uhis.data.PatientPrescriptionHistoryResponse
import org.medtroniclabs.uhis.data.PatientPrescriptionModel
import org.medtroniclabs.uhis.data.Prescription
import org.medtroniclabs.uhis.data.PrescriptionDetails
import org.medtroniclabs.uhis.data.PrescriptionListRequest
import org.medtroniclabs.uhis.data.UpdatePrescriptionModel
import org.medtroniclabs.uhis.databinding.ActivityNcdPrescriptionBinding
import org.medtroniclabs.uhis.databinding.NcdRowPrescriptionBinding
import org.medtroniclabs.uhis.databinding.NcdRowPrescriptionEditBinding
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.utility.CustomSpinnerAdapter
import org.medtroniclabs.uhis.mappingkey.Screening
import org.medtroniclabs.uhis.ncd.medicalreview.MedicationListener
import org.medtroniclabs.uhis.ncd.medicalreview.prescription.adapter.NCDDiscontinuedMedicationAdapter
import org.medtroniclabs.uhis.ncd.medicalreview.prescription.adapter.NCDPrescriptionAdapter
import org.medtroniclabs.uhis.ncd.medicalreview.prescription.dialog.NCDInstructionExpansionDialog
import org.medtroniclabs.uhis.ncd.medicalreview.prescription.dialog.NCDMedicationHistoryDialog
import org.medtroniclabs.uhis.ncd.medicalreview.prescription.dialog.PrescriptionResultDialogFragment
import org.medtroniclabs.uhis.ncd.medicalreview.prescription.viewmodel.NCDPrescriptionViewModel
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.medicalreview.SignatureDialogFragment
import org.medtroniclabs.uhis.ui.medicalreview.SignatureListener
import org.medtroniclabs.uhis.ui.mypatients.viewmodel.PatientDetailViewModel

@AndroidEntryPoint
class NCDPrescriptionActivity :
    BaseActivity(),
    View.OnClickListener,
    SignatureListener,
    MedicationListener {
    private lateinit var binding: ActivityNcdPrescriptionBinding
    private val prescriptionViewModel: NCDPrescriptionViewModel by viewModels()
    private val patientViewModel: PatientDetailViewModel by viewModels()
    private lateinit var prescriptionAdapter: NCDPrescriptionAdapter
    private var canSearch: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNcdPrescriptionBinding.inflate(layoutInflater)
        setMainContentView(
            binding.root,
            true,
            getString(R.string.prescription),
            homeAndBackVisibility = Pair(true, true),
            callback = {
                setResult(RESULT_OK, intent)
                finish()
            },
        )
        getPatients()
        clickListener()
        attachObserver()
        initializeView()
        showMedicationNudge()
    }

    fun getPatients() {
        // patientReference must be the patient FHIR id (passed in via DefinedParams.ID =
        // getPatientFHIRId()), not the human-readable patientId.
        prescriptionViewModel.patientReference = intent.getStringExtra(DefinedParams.ID)
        prescriptionViewModel.memberReference = intent.getStringExtra(DefinedParams.ID)
        prescriptionViewModel.patient_visit_id = intent.getStringExtra(DefinedParams.PATIENT_VISIT_ID)
        prescriptionViewModel.enrollmentType = intent.getStringExtra(DefinedParams.EnrollmentType)
        prescriptionViewModel.identityValue = intent.getStringExtra(Screening.identityValue)
        fetchPrescriptionList()
    }

    private fun showMedicationNudge() {
        prescriptionViewModel.getPrescriptionPrediction()
    }

    private fun initializeView() {
        prescriptionAdapter = NCDPrescriptionAdapter(this)
        binding.etPrescriptionSearch.apply {
            setOnItemClickListener { _, _, position, _ ->
                canSearch = false
                hideKeyboard(this)
                prescriptionViewModel.selectedMedication = null
                prescriptionViewModel.medicationListLiveData.value?.data?.let {
                    if (it.size > 0) {
                        prescriptionViewModel.selectedMedication = it[position]
                        val search = getSearchString(it[position])
                        binding.etPrescriptionSearch.setText(search)
                        binding.etPrescriptionSearch.setSelection(search.length)
                        binding.btnAddMedicine.isEnabled = true
                    }
                }
            }
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    p0: CharSequence?,
                    p1: Int,
                    p2: Int,
                    p3: Int,
                ) {
                    /**
                     * this method is not used
                     */
                }

                override fun onTextChanged(
                    p0: CharSequence?,
                    p1: Int,
                    p2: Int,
                    p3: Int,
                ) {
                    /**
                     * this method is not used
                     */
                }

                override fun afterTextChanged(p0: Editable?) {
                    text?.toString()?.let {
                        if (it.isEmpty()) {
                            // default showing all medicines
                            binding.btnAddMedicine.isEnabled = false
                            prescriptionViewModel.selectedMedication = null
                        } else {
                            if (it.isNotEmpty() && it.length > SearchLength) {
                                val medicationSearchRequest =
                                    MedicationSearchRequest(searchTerm = it.trim())
                                prescriptionViewModel.searchMedication(medicationSearchRequest)
                            }
                        }
                    }
                }
            })
        }
        prescriptionViewModel.getDosageFrequencyList()
        prescriptionViewModel.getPrescribedDays()
    }

    private fun clickListener() {
        binding.btnAddMedicine.safeClickListener(this)
        binding.btnPrescribe.safeClickListener(this)
        binding.tvDiscontinuedMedication.safeClickListener(this)
        binding.btnBack.safeClickListener(this)
        binding.btnRenewAll.safeClickListener(this)
    }

    fun attachObserver() {
        prescriptionViewModel.prescriptionPredictionResponseLiveDate.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }

                ResourceState.SUCCESS -> {
                    resourceState.data?.let {
                        PrescriptionResultDialogFragment().show(
                            supportFragmentManager,
                            PrescriptionResultDialogFragment.TAG,
                        )
                    }
                }
            }
        }

        reloadPrescriptionInstruction()
        prescriptionViewModel.createPrescriptionLiveData.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.SUCCESS -> {
                    resourceState.data?.let { map ->
                        val intent = Intent()
                        if (map.containsKey(DefinedParams.ENCOUNTER_ID)) {
                            val value = map[DefinedParams.ENCOUNTER_ID]
                            if (value is String) {
                                intent.putExtra(DefinedParams.ENCOUNTER_ID, value)
                            }
                        }
                        setResult(RESULT_OK, intent)
                        finish()
                    }
                }

                ResourceState.ERROR -> hideLoading()
                ResourceState.LOADING -> hideLoading()
            }
        }

        prescriptionViewModel.medicationListLiveData.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.SUCCESS -> {
                    resourceState.data?.let {
                        generateSuggestions(it)
                    }
                }

                else -> {
                    // Invoked if response state is not success
                }
            }
        }

        prescriptionViewModel.frequencyList.observe(this) {
            prescriptionViewModel.getDosageUnitList()
        }

        prescriptionViewModel.discontinuedPrescriptionListLiveData.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.ERROR -> hideLoading()
                ResourceState.LOADING -> showLoading()
                ResourceState.SUCCESS -> {
                    resourceState.data?.let {
                        disContinuedResponse(it)
                    } ?: kotlin.run {
                        binding.llDiscontinuedMedication.visibility = View.GONE
                    }
                    hideLoading()
                }
            }
        }

        prescriptionViewModel.prescriptionListLiveData.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    resourceState.data?.let { prescriptionListData ->
                        // Prescription changes regarding deleting the current medication
                        prescriptionViewModel.prescriptionUIModel?.removeAll { it.prescriptionId == prescriptionViewModel.prescriptionId }
                        prescriptionViewModel.prescriptionUIModel = prescriptionViewModel.prescriptionUIModel ?: ArrayList()

                        val prescriptionUIModel = ArrayList<MedicationResponse>()
                        prescriptionListData.forEach { prescription ->
                            prescriptionUIModel.add(
                                MedicationResponse(
                                    prescriptionId = prescription.prescriptionId,
                                    prescribedDays = prescription.prescribedDays,
                                    name = prescription.medicationName,
                                    id = prescription.medicationId.toLong(),
                                    dosageFormName = prescription.dosageFormName ?: "",
                                    dosageFrequencyName = prescription.dosageFrequencyName ?: "",
                                    brandName = prescription.brandName ?: "",
                                    classificationName = prescription.classificationName ?: "",
                                    dosageUnitName = prescription.dosageUnitName,
                                    dosageUnitValue = prescription.dosageUnitValue,
                                    instructionNote = prescription.instructionNote,
                                    codeDetails = prescription.codeDetails,
                                    dosageUnitId = null,
                                    selectedMap = null,
                                    quantity = 0L,
                                    isEditable = false,
                                    prescribedSince = prescription.prescribedSince,
                                    showErrorMessage = false,
                                    prescriptionRemainingDays = prescription.prescriptionRemainingDays,
                                ),
                            )
                        }
                        val existingIds = prescriptionViewModel.prescriptionUIModel?.map { it.prescriptionId }?.toSet() ?: emptySet()

                        val newItems = prescriptionUIModel.filter { it.prescriptionId !in existingIds }

                        prescriptionViewModel.prescriptionUIModel?.addAll(newItems)
                    } ?: kotlin.run {
                        prescriptionViewModel.prescriptionUIModel = null
                    }
                    loadPrescriptionListData()
                }
            }
        }
        prescriptionViewModel.removePrescriptionLiveData.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                    resourceState?.message?.let { message ->
                        showErrorDialogue(getString(R.string.error), message, false) {}
                    }
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    binding.llDiscontinuedMedication.visibility = View.GONE
                    binding.tvDiscontinuedMedication.text =
                        getString(R.string.view_discontinued_medication)
                    fetchPrescriptionList()
                }
            }
        }

        prescriptionViewModel.medicationHistoryLiveData.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    resourceState.data?.let { list ->
                        val patientPrescriptionHistoryResponse = ArrayList<PatientPrescriptionHistoryResponse>()
                        list.forEach { data ->
                            patientPrescriptionHistoryResponse.add(
                                PatientPrescriptionHistoryResponse(
                                    medicationName = data.medicationName,
                                    prescribedDays = data.prescribedDays,
                                    instructionNote = data.instructionNote,
                                    dosageFrequencyName = data.dosageFrequencyName,
                                    dosageFormName = data.dosageFormName,
                                    dosageUnitName = data.dosageUnitName,
                                    createdAt = data.prescribedSince,
                                    dosageUnitValue = data.dosageUnitValue,
                                ),
                            )
                        }
                        NCDMedicationHistoryDialog
                            .newInstance(patientPrescriptionHistoryResponse)
                            .show(supportFragmentManager, NCDMedicationHistoryDialog.TAG)
                    }
                }
            }
        }
    }

    private fun reloadPrescriptionInstruction() {
        prescriptionViewModel.reloadInstruction.observe(this) {
            if (it) {
                loadPrescriptionListData()
            }
        }
    }

    private fun generateSuggestions(searchResultList: ArrayList<MedicationResponse>) {
        val searchResults = ArrayList<Pair<String, String>>()
        if (searchResultList.isNotEmpty()) {
            searchResultList.forEach {
                val search = "${it.name}, ${it.brandName}, ${it.dosageFormName}"
                if (isValidSuggestion(search)) {
                    searchResults.add(
                        Pair(search, it.classificationName ?: ""),
                    )
                }
            }
        }
        prescriptionAdapter.setData(searchResults)
        binding.etPrescriptionSearch.setAdapter(prescriptionAdapter)
        if (searchResults.size > 0) binding.etPrescriptionSearch.showDropDown()
    }

    private fun isValidSuggestion(search: String): Boolean {
        val searchStr = search.replace(", ", "", true)
        return searchStr.isNotEmpty()
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.btnAddMedicine -> {
                if (connectivityManager.isNetworkAvailable()) {
                    addMedicine()
                } else {
                    showErrorDialogue(
                        getString(R.string.title_no_network),
                        getString(R.string.message_no_network),
                        isNegativeButtonNeed = false,
                    ) { _ -> }
                }
            }

            R.id.btnPrescribe -> {
                if (connectivityManager.isNetworkAvailable()) {
                    createOrUpdatePrescription()
                } else {
                    showErrorDialogue(
                        getString(R.string.title_no_network),
                        getString(R.string.message_no_network),
                        isNegativeButtonNeed = false,
                    ) { _ -> }
                }
            }

            R.id.tvDiscontinuedMedication -> {
                if (connectivityManager.isNetworkAvailable()) {
                    discontinuedMedication()
                } else {
                    showErrorDialogue(
                        getString(R.string.title_no_network),
                        getString(R.string.message_no_network),
                        isNegativeButtonNeed = false,
                    ) { _ -> }
                }
            }

            binding.btnBack.id -> {
                finish()
            }

            R.id.btnRenewAll -> {
                changeAllToEditMode()
            }
        }
    }

    private fun discontinuedMedication() {
        if (binding.tvDiscontinuedMedication.text.toString() == getString(R.string.hide_discontinued_medication)) {
            binding.tvDiscontinuedMedication.text =
                getText(R.string.view_discontinued_medication)
            binding.llDiscontinuedMedication.gone()
        } else {
            fetchPrescriptionList(true)
        }
    }

    private fun addMedicine() {
        prescriptionViewModel.selectedMedication?.let {
            if (prescriptionViewModel.prescriptionUIModel == null) {
                prescriptionViewModel.prescriptionUIModel = ArrayList()
                val model = MedicationResponse(
                    id = it.id,
                    name = it.name,
                    quantity = it.quantity,
                    prescribedDays = it.prescribedDays,
                    dosageDurationName = it.dosageDurationName,
                    dosageFormName = it.dosageFormName,
                    classificationName = it.classificationName,
                    brandName = it.brandName,
                    prescribedSince = it.prescribedSince,
                    selectedMap = it.selectedMap,
                    isEditable = it.isEditable,
                    showErrorMessage = it.showErrorMessage,
                    codeDetails = it.codeDetails,
                )
                model.dosageFormNameEntered = it.dosageFormName
                prescriptionViewModel.prescriptionUIModel!!.add(model)
            } else {
                val model = MedicationResponse(
                    id = it.id,
                    name = it.name,
                    quantity = it.quantity,
                    prescribedDays = it.prescribedDays,
                    dosageDurationName = it.dosageDurationName,
                    dosageFormName = it.dosageFormName,
                    classificationName = it.classificationName,
                    brandName = it.brandName,
                    prescribedSince = it.prescribedSince,
                    selectedMap = it.selectedMap,
                    isEditable = it.isEditable,
                    showErrorMessage = it.showErrorMessage,
                    codeDetails = it.codeDetails,
                )
                model.dosageFormNameEntered = it.dosageFormName
                prescriptionViewModel.prescriptionUIModel!!.add(model)
            }
            loadPrescriptionListData()
        }
        binding.etPrescriptionSearch.text.clear()
    }

    private fun disContinuedResponse(it: ArrayList<Prescription>) {
        if (it.size > 0) {
            val discontinuedMedicationAdapter = NCDDiscontinuedMedicationAdapter(it, this)
            binding.rvDiscontinuedMedicationList.layoutManager =
                LinearLayoutManager(this)
            binding.rvDiscontinuedMedicationList.adapter =
                discontinuedMedicationAdapter
            showDMRecyclerView()
        } else {
            hideDMRecyclerView()
        }
    }

    private fun loadPrescriptionListData() {
        prescriptionViewModel.prescriptionUIModel?.let {
            if (it.size > 0) {
                showRecyclerView()
                loadPrescriptionData(it)
            } else {
                hideRecyclerView()
            }
        }
        changeButtonVisibility()
    }

    private fun showRecyclerView() {
        binding.apply {
            btnPrescribe.isEnabled = true
            llMedicationList.visibility = View.VISIBLE
            tvNoData.visibility = View.GONE
        }
    }

    private fun hideRecyclerView() {
        binding.apply {
            btnPrescribe.isEnabled = false
            llMedicationList.visibility = View.GONE
            tvNoData.visibility = View.VISIBLE
        }
    }

    private fun changeButtonVisibility() {
        if (prescriptionViewModel.prescriptionUIModel != null && prescriptionViewModel.prescriptionUIModel!!.size > 0) {
            val data = prescriptionViewModel.prescriptionUIModel?.firstOrNull {
                (it.id ?: 0) > 0 && !it.isEdit
            }
            binding.btnRenewAll.visibility = if (data == null) View.GONE else View.VISIBLE
        } else {
            binding.btnRenewAll.visibility = View.GONE
        }
    }

    private fun loadPrescriptionData(data: ArrayList<MedicationResponse>) {
        binding.llMedicationList.removeAllViews()
        data.forEachIndexed { index, model ->
            if (model.prescribedSince.isNullOrBlank() || model.isEdit) {
                if (index == data.size - 1) {
                    loadMedicationEdit(model, true)
                } else {
                    loadMedicationEdit(model, false)
                }
            } else {
                if (index == data.size - 1) {
                    loadMedicationView(model, true)
                } else {
                    loadMedicationView(model, false)
                }
            }
        }
    }

    private fun loadMedicationEdit(
        model: MedicationResponse,
        dividerStatus: Boolean,
    ) {
        val medicationEditBinding = NcdRowPrescriptionEditBinding.inflate(layoutInflater)
        medicationEditBinding.tvMedicationName.text =
            if (model.name.isNullOrBlank()) getString(R.string.separator_hyphen) else model.name
        if (model.dosageFormNameEntered.isNullOrBlank() || (model.id ?: 0) <= 0) {
            otherMedicationEdit(medicationEditBinding, model)
        } else {
            medicationEditBinding.tvForm.visibility = View.VISIBLE
            medicationEditBinding.spinnerForm.visibility = View.GONE
            medicationEditBinding.tvForm.text = model.dosageFormNameEntered
        }

        model.filledPrescriptionDays?.let {
            medicationEditBinding.etPrescribedDays.setText(it.toString())
        } ?: kotlin.run {
            medicationEditBinding.etPrescribedDays.setText("")
        }

        medicationEditBinding.tvMedicineErrorMessage.tag =
            "${model.datetime}${model.name}"

        medicationEditBinding.divider.visibility = dividerVisibility(dividerStatus)

        medicationEditBinding.etInstruction.text = model.instructionEntered ?: ""

        // Dosage Unit Value
        medicationEditBinding.etDosage.visibility = View.VISIBLE
        medicationEditBinding.etDosage.setText(model.enteredDosageUnitValue ?: "")

        medicationEditBinding.tvDosage.visibility = View.GONE
        medicationEditBinding.tvDosage.text = ""

        // Dosage Unit Name
        medicationEditBinding.tvUnitVal.visibility = View.GONE
        medicationEditBinding.tvUnitVal.text = ""

        medicationEditBinding.tvUnit.visibility = View.VISIBLE
        val dosageAdapter = CustomSpinnerAdapter(this)
        dosageAdapter.setData(getDosageUnit())
        medicationEditBinding.tvUnit.adapter = dosageAdapter
        medicationEditBinding.tvUnit.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    p0: AdapterView<*>?,
                    p1: View?,
                    p2: Int,
                    p3: Long,
                ) {
                    val selectedItem = dosageAdapter.getData(position = p2)
                    selectedItem?.let {
                        model.dosageUnitSelected = it[DefinedParams.ID].toString().toLong()
                        model.dosageUnitNameEntered = it[DefinedParams.NAME] as String
                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    /**
                     * this method is not used
                     */
                }
            }
        model.dosageUnitNameEntered?.let {
            medicationEditBinding.tvUnit.setSelection(
                getSpinnerPosition(dosageAdapter, it),
                true,
            )
        } ?: kotlin.run {
            medicationEditBinding.tvUnit.setSelection(0, true)
        }
        if (CommonUtils.isTiberbuUser()) {
            medicationEditBinding.spinnerPrescribedDays.visible()
            medicationEditBinding.etPrescribedDays.gone()
            val prescribedDaysAdapter = CustomSpinnerAdapter(this)
            prescribedDaysAdapter.setData(getPrescribedDays())
            medicationEditBinding.spinnerPrescribedDays.adapter = prescribedDaysAdapter
            medicationEditBinding.spinnerPrescribedDays.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        p0: AdapterView<*>?,
                        p1: View?,
                        p2: Int,
                        p3: Long,
                    ) {
                        val selectedItem = prescribedDaysAdapter.getData(position = p2)
                        selectedItem?.let {
                            model.filledPrescriptionDays = it[DefinedParams.QUANTITY].toString().toLongOrNull()
                            model.dosage_duration_name = it[DefinedParams.NAME].toString()
                        }
                    }

                    override fun onNothingSelected(p0: AdapterView<*>?) {
                        /**
                         * this method is not used
                         */
                    }
                }
            model.dosage_duration_name?.let {
                medicationEditBinding.spinnerPrescribedDays.setSelection(
                    getSpinnerPosition(prescribedDaysAdapter, it),
                    true,
                )
            } ?: kotlin.run {
                medicationEditBinding.tvUnit.setSelection(0, true)
            }
        } else {
            medicationEditBinding.spinnerPrescribedDays.gone()
            medicationEditBinding.etPrescribedDays.visible()
        }
        val adapter = CustomSpinnerAdapter(this)
        adapter.setData(getFrequencyList())
        medicationEditBinding.spinnerFrequency.adapter = adapter
        medicationEditBinding.spinnerFrequency.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    p0: AdapterView<*>?,
                    p1: View?,
                    p2: Int,
                    p3: Long,
                ) {
                    val selectedItem = adapter.getData(position = p2)
                    selectedItem?.let {
                        editSpinnerFrequency(selectedItem, medicationEditBinding, model)
                        model.dosageFrequencyNameEntered = it[DefinedParams.NAME] as String
                        model.dosageFrequencyEntered = it[DefinedParams.ID].toString().toLong()
                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    /**
                     * this method is not used
                     */
                }
            }
        model.dosageFrequencyNameEntered?.let {
            medicationEditBinding.spinnerFrequency.setSelection(
                getSpinnerPosition(adapter, it),
                true,
            )
        } ?: kotlin.run {
            medicationEditBinding.spinnerFrequency.setSelection(0, true)
        }

        ivResetRemove(model.isEdit, medicationEditBinding)

        medicationEditBinding.etDosage.addTextChangedListener {
            checkValue(it)?.let { value ->
                model.enteredDosageUnitValue = value.toString()
            } ?: kotlin.run {
                model.enteredDosageUnitValue = null
            }
        }
        medicationEditBinding.etPrescribedDays.addTextChangedListener {
            checkValue(it)?.let { value ->
                model.filledPrescriptionDays = value.toString().toLongOrNull()
            } ?: kotlin.run {
                model.filledPrescriptionDays = null
            }
        }
        medicationEditBinding.etInstruction.addTextChangedListener {
            it?.let {
                model.instructionEntered = it.toString()
            } ?: kotlin.run {
                model.instructionEntered = null
            }
        }
        medicationEditBinding.ivRemoveMedication.safeClickListener {
            prescriptionViewModel.prescriptionUIModel?.remove(model)
            loadPrescriptionListData()
        }
        medicationEditBinding.ivResetMedication.safeClickListener {
            model.isEdit = false
            model.isEdited = false
            model.filledPrescriptionDays = model.prescribedDays
            model.enteredDosageUnitValue = model.dosageUnitValue
            model.dosageFormNameEntered = model.dosageFormName
            model.dosageFrequencyNameEntered = model.dosageFrequencyName
            model.dosageFrequencyEntered = model.dosageFrequencyEntered
            model.dosageUnitSelected = model.dosageUnitId
            model.dosageUnitNameEntered = model.dosageUnitName
            model.instructionEntered = model.instructionNote
            loadPrescriptionListData()
        }
        medicationEditBinding.etInstruction.safeClickListener {
            NCDInstructionExpansionDialog
                .newInstance(model)
                .show(supportFragmentManager, NCDInstructionExpansionDialog.TAG)
        }
        binding.llMedicationList.addView(medicationEditBinding.root)
    }

    private fun loadMedicationView(
        model: MedicationResponse,
        dividerStatus: Boolean,
    ) {
        val medicationBinding = NcdRowPrescriptionBinding.inflate(layoutInflater)
        medicationBinding.tvMedicationName.text =
            if (model.name.isNullOrBlank()) getString(R.string.separator_hyphen) else model.name
        medicationBinding.tvDosage.text =
            if (model.dosageUnitValue.isNullOrBlank()) getString(R.string.separator_hyphen) else model.dosageUnitValue
        medicationBinding.tvUnit.text =
            if (model.dosageUnitName.isNullOrBlank()) getString(R.string.separator_hyphen) else model.dosageUnitName
        medicationBinding.tvForm.text =
            if (model.dosageFormName.isNullOrBlank()) getString(R.string.separator_hyphen) else model.dosageFormName
        medicationBinding.tvFrequency.text =
            if (model.dosageFrequencyName.isNullOrBlank()) getString(R.string.separator_hyphen) else model.dosageFrequencyName

        var prescribedDays = ""
        model.prescribedDays?.let {
            prescribedDays += it.toString()
        }
        val remainingDays = model.prescriptionRemainingDays ?: 0
        val daysLeft = if (remainingDays > 1) " Days Left" else " Day Left"
        val completed = "Completed"
        val resultedString: String
        if (remainingDays > 0) {
            resultedString = "$prescribedDays - $remainingDays$daysLeft"
            medicationBinding.tvPrescribedDays.setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.black,
                ),
            )
            medicationBinding.ivPrescriptionCompleted.visibility = View.GONE
        } else {
            resultedString = "$prescribedDays - $completed"
            medicationBinding.tvPrescribedDays.setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.medium_high_risk_color,
                ),
            )
            medicationBinding.ivPrescriptionCompleted.visibility = View.VISIBLE
        }
        if (dividerStatus) {
            medicationBinding.divider.visibility = View.GONE
        } else {
            medicationBinding.divider.visibility = View.VISIBLE
        }
        medicationBinding.tvPrescribedDays.text = resultedString

        medicationBinding.tvInformation.text =
            model.instructionNote ?: ""

        var strPrescribedSince = getString(R.string.separator_hyphen)
        model.prescribedSince?.let { prescribedSince ->
            strPrescribedSince = DateUtils.convertDateTimeToDate(
                prescribedSince,
                DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                DateUtils.DATE_FORMAT_ddMMMyyyy,
            )
            medicationBinding.tvPrescribedSince.safeClickListener {
                if (connectivityManager.isNetworkAvailable()) {
                    model.prescriptionId?.let { prescriptionID ->
                        prescriptionViewModel.getMedicationHistory(prescriptionID)
                    }
                } else {
                    showErrorDialogue(
                        getString(R.string.title_no_network),
                        getString(R.string.message_no_network),
                        isNegativeButtonNeed = false,
                    ) { _ -> }
                }
            }
        } ?: kotlin.run {
            medicationBinding.tvPrescribedSince.text = getString(R.string.separator_hyphen)
        }
        val spannableString = SpannableString(strPrescribedSince)
        spannableString.setSpan(UnderlineSpan(), 0, strPrescribedSince.length, 0)
        medicationBinding.tvPrescribedSince.text = spannableString

        medicationBinding.ivEditMedication.safeClickListener {
            model.isEdit = true
            model.isEdited = true
            model.filledPrescriptionDays = null
            model.enteredDosageUnitValue = null
            model.dosageFormNameEntered = model.dosageFormName
            model.dosageUnitNameEntered = null
            model.instructionEntered = null
            model.dosageUnitSelected = null
            model.dosageFrequencyEntered = null
            model.dosageFrequencyNameEntered = null
            loadPrescriptionListData()
        }

        medicationBinding.ivDeleteMedication.safeClickListener {
            if (connectivityManager.isNetworkAvailable()) {
                showAlertDialogWithComments(
                    getString(R.string.confirmation),
                    message = getString(R.string.delete_confirmation),
                    true,
                    buttonName = Pair(getString(R.string.ok), getString(R.string.cancel)),
                ) { isPositiveResult, discontinuedReason ->
                    if (isPositiveResult) {
                        model.prescriptionId.let {
                            prescriptionViewModel.removePrescription(
                                it.toString(),
                                discontinuedReason,
                            )
                        }
                    }
                }
            } else {
                showErrorDialogue(
                    getString(R.string.title_no_network),
                    getString(R.string.message_no_network),
                    isNegativeButtonNeed = false,
                ) { _ -> }
            }
        }
        binding.llMedicationList.addView(medicationBinding.root)
    }

    private fun getSearchString(medicationResponse: MedicationResponse): String =
        medicationResponse.let {
            val name = it.name ?: ""
            val brandName = it.brandName ?: ""
            val dosageFormName = it.dosageFormName ?: ""
            "$name, $brandName , $dosageFormName"
        }

    override fun applySignature(signature: Bitmap) {
        updatePrescriptions(signature)
    }

    private fun updatePrescriptions(signatureBitmap: Bitmap) {
        signatureBitmap.let { signature ->
            prescriptionViewModel.createOrUpdatePrescription(
                signature,
                CommonUtils.getFilePath(
                    prescriptionViewModel.patient_visit_id!!.toString(),
                    context = this,
                ),
                getReqBody(),
            )
        }
    }

    private fun getReqBody(): PatientPrescriptionModel {
        val prescriptionList = ArrayList<PrescriptionDetails>()
        prescriptionViewModel.savePrescriptionList?.forEach {
            prescriptionList.add(
                PrescriptionDetails(
                    prescriptionId = it.prescriptionId,
                    prescribedDays = it.prescribedDays,
                    dosageDurationName = it.dosageDurationName,
                    medicationName = it.medicationName ?: "",
                    medicationId = it.medicationId.toString(),
                    dosageFormName = it.dosageFormName ?: "",
                    dosageFrequencyName = it.dosageFrequencyName ?: "",
                    brandName = it.brandName ?: "",
                    classificationName = it.classificationName ?: "",
                    dosageUnitName = it.dosageUnitName,
                    dosageUnitValue = it.dosageUnitValue,
                    instructionNote = it.instructionNote,
                    codeDetails = it.codeDetailsObject,
                ),
            )
        }

        return PatientPrescriptionModel(
            prescriptions = prescriptionList,
            enrollmentType = prescriptionViewModel.enrollmentType,
            identityValue = prescriptionViewModel.identityValue,
            patientVisitId = prescriptionViewModel.patient_visit_id?.toLong(),
        )
    }

    private fun dividerVisibility(dividerStatus: Boolean): Int =
        if (dividerStatus) {
            View.GONE
        } else {
            View.VISIBLE
        }

    private fun getPrescribedDays(): ArrayList<Map<String, Any>> {
        val dropDownList = ArrayList<Map<String, Any>>()
        dropDownList.add(
            hashMapOf<String, Any>(
                DefinedParams.NAME to getString(R.string.please_select),
                DefinedParams.ID to "-1",
                DefinedParams.QUANTITY to "",
            ),
        )
        prescriptionViewModel.prescribedDaysList.value?.forEach {
            dropDownList.add(
                hashMapOf<String, Any>(
                    DefinedParams.NAME to it.name,
                    DefinedParams.ID to it.id,
                    DefinedParams.QUANTITY to it.quantity,
                ),
            )
        }
        return dropDownList
    }

    private fun getDosageUnit(): ArrayList<Map<String, Any>> {
        val dropDownList = ArrayList<Map<String, Any>>()
        dropDownList.add(
            hashMapOf<String, Any>(
                DefinedParams.NAME to getString(R.string.please_select),
                DefinedParams.ID to "-1",
                DefinedParams.DESCRIPTION to "",
            ),
        )
        prescriptionViewModel.unitList.value?.forEach {
            dropDownList.add(
                hashMapOf<String, Any>(
                    DefinedParams.NAME to it.unit,
                    DefinedParams.ID to it.id,
                ),
            )
        }
        return dropDownList
    }

    private fun getSpinnerPosition(
        dosageAdapter: CustomSpinnerAdapter,
        it: String,
    ): Int =
        if (it != getString(R.string.please_select) && dosageAdapter.getIndexOfItemByName(it) != -1) {
            dosageAdapter.getIndexOfItemByName(it)
        } else {
            0
        }

    private fun checkValue(it: Editable?): Editable? =
        if (it.isNullOrBlank()) {
            null
        } else {
            it
        }

    private fun editSpinnerFrequency(
        selectedItem: Map<String, Any>,
        medicationEditBinding: NcdRowPrescriptionEditBinding,
        model: MedicationResponse,
    ) {
        selectedItem.let {
            if (!(
                    model.dosageFrequencyNameEntered != null &&
                        model.dosageFrequencyNameEntered!!.isNotEmpty() &&
                        model.dosageFrequencyNameEntered == (it[DefinedParams.NAME] as String)
                )
            ) {
                if (it.containsKey(DefinedParams.DESCRIPTION)) {
                    medicationEditBinding.etInstruction.text =
                        it[DefinedParams.DESCRIPTION] as String
                }
            } else {
                if (!model.instructionEntered.isNullOrBlank()) {
                    medicationEditBinding.etInstruction.text = model.instructionEntered
                } else {
                    medicationEditBinding.etInstruction.text =
                        it[DefinedParams.DESCRIPTION] as String
                }
            }
        }
    }

    private fun getFrequencyList(): ArrayList<Map<String, Any>> {
        val dropDownList = ArrayList<Map<String, Any>>()
        dropDownList.add(
            hashMapOf<String, Any>(
                DefinedParams.NAME to getString(R.string.please_select),
                DefinedParams.ID to "-1",
                DefinedParams.DESCRIPTION to "",
            ),
        )
        prescriptionViewModel.frequencyList.value?.forEach {
            dropDownList.add(
                hashMapOf<String, Any>(
                    DefinedParams.NAME to it.name,
                    DefinedParams.ID to it.id,
                    DefinedParams.DESCRIPTION to (it.description ?: ""),
                ),
            )
        }
        return dropDownList
    }

    private fun otherMedicationEdit(
        medicationEditBinding: NcdRowPrescriptionEditBinding,
        model: MedicationResponse,
    ) {
        medicationEditBinding.tvForm.visibility = View.GONE
        medicationEditBinding.spinnerForm.visibility = View.VISIBLE
        val spinnerFormAdapter = CustomSpinnerAdapter(this)
        spinnerFormAdapter.setData(getFormName())
        medicationEditBinding.spinnerForm.adapter = spinnerFormAdapter
        medicationEditBinding.spinnerForm.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    p0: AdapterView<*>?,
                    p1: View?,
                    p2: Int,
                    p3: Long,
                ) {
                    val selectedItem = spinnerFormAdapter.getData(position = p2)
                    selectedItem?.let {
                        model.dosageFormNameEntered = it[DefinedParams.ID] as String
                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    /**
                     * this method is not used
                     */
                }
            }
        model.dosageFormNameEntered?.let {
            medicationEditBinding.spinnerFrequency.setSelection(
                getSpinnerPosition(spinnerFormAdapter, it),
                true,
            )
        } ?: kotlin.run {
            medicationEditBinding.spinnerForm.setSelection(0, true)
        }
    }

    private fun ivResetRemove(
        edit: Boolean,
        medicationEditBinding: NcdRowPrescriptionEditBinding,
    ) {
        if (edit) {
            medicationEditBinding.ivResetMedication.visibility = View.VISIBLE
            medicationEditBinding.ivRemoveMedication.visibility = View.GONE
        } else {
            medicationEditBinding.ivResetMedication.visibility = View.GONE
            medicationEditBinding.ivRemoveMedication.visibility = View.VISIBLE
        }
    }

    private fun getFormName(): ArrayList<Map<String, Any>> {
        val dropDownList = ArrayList<Map<String, Any>>()
        dropDownList.add(
            hashMapOf<String, Any>(
                DefinedParams.NAME to DefinedParams.DEFAULT_ID_LABEL,
                DefinedParams.ID to DefinedParams.DEFAULT_ID,
            ),
        )
        dropDownList.add(
            hashMapOf<String, Any>(
                DefinedParams.NAME to DefinedParams.TABLET,
                DefinedParams.ID to DefinedParams.TABLET,
            ),
        )
        dropDownList.add(
            hashMapOf<String, Any>(
                DefinedParams.NAME to DefinedParams.LIQUID_ORAL,
                DefinedParams.ID to DefinedParams.LIQUID_ORAL,
            ),
        )
        dropDownList.add(
            hashMapOf<String, Any>(
                DefinedParams.NAME to DefinedParams.INJECTION_INJECTABLE_SOLUTION,
                DefinedParams.ID to DefinedParams.INJECTION_INJECTABLE_SOLUTION,
            ),
        )
        dropDownList.add(
            hashMapOf<String, Any>(
                DefinedParams.NAME to DefinedParams.CAPSULE,
                DefinedParams.ID to DefinedParams.CAPSULE,
            ),
        )

        return dropDownList
    }

    private fun createOrUpdatePrescription() {
        val list =
            prescriptionViewModel.prescriptionUIModel?.filter { it.isEdited || it.prescribedSince.isNullOrBlank() }
        list?.let { _ ->
            if (list.isNotEmpty()) {
                val errorList = ArrayList<String>()
                prescriptionViewModel.savePrescriptionList = ArrayList()
                list.forEachIndexed { _, it ->
                    var isValid: Boolean
                    val invalidList = ArrayList<String>()

                    prescriptionValidation(it).let {
                        isValid = it.first
                        invalidList.addAll(it.second)
                    }

                    errorList.addAll(invalidList)
                    if (isValid) {
                        invalidList.clear()
                        validateErrorMessage(it)
                        val finalModel = UpdatePrescriptionModel(
                            medicationId = it.id,
                            medicationName = it.name,
                            prescriptionId = it.prescriptionId,
                            dosageUnitName = it.dosageUnitNameEntered,
                            dosageUnitValue = it.enteredDosageUnitValue,
                            dosageFormName = it.dosageFormNameEntered,
                            dosageFrequencyName = it.dosageFrequencyNameEntered,
                            dosageFrequencyId = it.dosageFrequencyEntered,
                            prescribedDays = it.filledPrescriptionDays,
                            dosageDurationName = it.dosage_duration_name,
                            instructionNote = it.instructionEntered,
                            isDeleted = false,
                            dosageUnitId = it.dosageUnitSelected,
                            classificationName = it.classificationName,
                            brandName = it.brandName,
                            codeDetailsObject = it.codeDetails,
                        )
                        prescriptionViewModel.savePrescriptionList?.add(finalModel)
                    } else {
                        invalidPrescription(it, invalidList)
                        return@forEachIndexed
                    }
                }
                isSignatureViewOrNot(errorList)
            } else {
                showErrorDialogue(
                    getString(R.string.alert),
                    getString(R.string.no_new_medicines_prescribed),
                    false,
                ) {}
            }
        } ?: kotlin.run {
            showErrorDialogue(
                getString(R.string.alert),
                getString(R.string.no_new_medicines_prescribed),
                false,
            ) {}
        }
    }

    private fun prescriptionValidation(prescriptionModel: MedicationResponse): Pair<Boolean, ArrayList<String>> {
        prescriptionModel.let { prescription ->
            var isValid = true
            val invalidList = ArrayList<String>()
            if (prescription.enteredDosageUnitValue.isNullOrBlank()) {
                isValid = false
                invalidList.add(getString(R.string.dosage))
            }
            prescription.dosageUnitNameEntered?.let {
                if (validateSpinnerValue(it)) {
                    isValid = false
                    invalidList.add(getString(R.string.unit))
                }
            } ?: kotlin.run {
                isValid = false
                invalidList.add(getString(R.string.unit))
            }
            prescription.dosageFormNameEntered?.let {
                if (it.isEmpty() || it == DefinedParams.DEFAULT_ID) {
                    isValid = false
                    invalidList.add(getString(R.string.form))
                }
            } ?: kotlin.run {
                isValid = false
                invalidList.add(getString(R.string.form))
            }
            prescription.dosageFrequencyNameEntered?.let {
                if (validateSpinnerValue(it)) {
                    isValid = false
                    invalidList.add(getString(R.string.frequency))
                }
            } ?: kotlin.run {
                isValid = false
                invalidList.add(getString(R.string.frequency))
            }
            if ((
                    prescription.filledPrescriptionDays
                        ?: 0
                ) <= 0 ||
                prescription.filledPrescriptionDays
                    ?.toString()
                    .isNullOrBlank()
            ) {
                isValid = false
                invalidList.add(getString(R.string.prescribed_days))
            }
            return Pair(isValid, invalidList)
        }
    }

    private fun validateSpinnerValue(it: String): Boolean = it.isEmpty() || it == getString(R.string.please_select)

    private fun validateErrorMessage(data: MedicationResponse) {
        showHideMedicineErrorMessage(
            data.datetime,
            data.name,
            ArrayList(),
            View.GONE,
        )
    }

    private fun showHideMedicineErrorMessage(
        index: Long?,
        medicationName: String?,
        invalidList: ArrayList<String>,
        visiblity: Int,
    ) {
        index?.let {
            getViewByTag("${index}$medicationName")?.let { view ->
                if (view is TextView) {
                    view.visibility = visiblity
                    if (invalidList.isNotEmpty()) {
                        view.text = "${getString(R.string.please_enter_details)} ${
                            invalidList.joinToString(separator = ", ")
                        }"
                    }
                }
            }
        }
    }

    private fun getViewByTag(tag: Any): View? = binding.root.findViewWithTag(tag)

    private fun invalidPrescription(
        it: MedicationResponse,
        invalidList: java.util.ArrayList<String>,
    ) {
        if (prescriptionViewModel.prescriptionUIModel != null && prescriptionViewModel.prescriptionUIModel!!.isNotEmpty()) {
            showHideMedicineErrorMessage(
                it.datetime,
                it.name,
                invalidList,
                View.VISIBLE,
            )
        }
        prescriptionViewModel.savePrescriptionList?.clear()
    }

    private fun isSignatureViewOrNot(errorList: ArrayList<String>) {
        if (errorList.size <= 0) {
            SignatureDialogFragment
                .newInstance(this)
                .show(supportFragmentManager, SignatureDialogFragment.TAG)
        }
    }

    private fun changeAllToEditMode() {
        val list = prescriptionViewModel.prescriptionUIModel?.filter { !it.isEdit }
        if (!list.isNullOrEmpty()) {
            list.forEach {
                it.isEdit = true
                it.isEdited = true
                it.filledPrescriptionDays = null
                it.dosage_duration_name = null
                it.enteredDosageUnitValue = null
                it.dosageFormNameEntered = it.dosageFormName
                it.dosageFrequencyEntered = null
                it.dosageFrequencyNameEntered = null
                it.dosageUnitNameEntered = null
                it.instructionEntered = null
                it.dosageUnitSelected = null
            }
        }
        loadPrescriptionListData()
    }

    private fun showDMRecyclerView() {
        binding.apply {
            llDiscontinuedMedication.visibility = View.VISIBLE
            rvDiscontinuedMedicationList.visibility = View.VISIBLE
            tvDMNoData.visibility = View.GONE
            tvDiscontinuedMedication.text = getString(R.string.hide_discontinued_medication)
        }
    }

    private fun hideDMRecyclerView() {
        binding.apply {
            rvDiscontinuedMedicationList.visibility = View.GONE
            llDiscontinuedMedication.visibility = View.VISIBLE
            tvDMNoData.visibility = View.VISIBLE
            tvDiscontinuedMedication.text = getString(R.string.hide_discontinued_medication)
        }
    }

    private fun fetchPrescriptionList(isFromDiscontinue: Boolean = false) {
        prescriptionViewModel.let {
            prescriptionViewModel.getPrescriptionList(
                PrescriptionListRequest(
                    patientReference = it.patientReference,
                    memberReference = it.memberReference,
                    isActive = !isFromDiscontinue,
                    requestFrom = DefinedParams.AFRICA,
                ),
            )
        }
    }

    override fun openMedicalHistory(prescriptionId: Long?) {
        prescriptionViewModel.getMedicationHistory(prescriptionId.toString())
    }

    override fun updateView(isEmpty: Boolean) {
        /**
         * this method is not used
         */
    }

    override fun deleteMedication(
        pos: Int,
        prescriptionId: Long?,
    ) {
        /**
         * this method is not used
         */
    }
}
