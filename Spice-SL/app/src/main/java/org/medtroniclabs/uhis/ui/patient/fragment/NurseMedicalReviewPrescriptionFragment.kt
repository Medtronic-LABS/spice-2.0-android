package org.medtroniclabs.uhis.ui.patient.fragment

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.UpdateMedicationModel
import org.medtroniclabs.uhis.data.model.ChipViewItemModel
import org.medtroniclabs.uhis.data.registration.MedicationSearchReqModel
import org.medtroniclabs.uhis.data.registration.PatientDetailsModel
import org.medtroniclabs.uhis.data.registration.PatientHistoryRequest
import org.medtroniclabs.uhis.data.registration.PatientPrescriptionHistoryResponse
import org.medtroniclabs.uhis.data.registration.Prescription
import org.medtroniclabs.uhis.data.registration.PrescriptionItem
import org.medtroniclabs.uhis.data.registration.PrescriptionModel
import org.medtroniclabs.uhis.data.registration.SessionModel
import org.medtroniclabs.uhis.data.registration.VisitDateModel
import org.medtroniclabs.uhis.databinding.FragmentNurseMedicalReviewPrescriptionBinding
import org.medtroniclabs.uhis.databinding.LayoutMedicationNewBinding
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.utility.CustomSpinnerAdapter
import org.medtroniclabs.uhis.ncd.medicalreview.NCDMRUtil
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.TagListCustomView
import org.medtroniclabs.uhis.ui.patient.adapter.DateListAdapter
import org.medtroniclabs.uhis.ui.patient.adapter.PrescriptionAdapter
import org.medtroniclabs.uhis.ui.patient.util.DateSelectionListener
import org.medtroniclabs.uhis.ui.patient.util.InstructionExpansionDialog
import org.medtroniclabs.uhis.ui.patient.viewmodel.MedicalReviewBaseViewModel
import org.medtroniclabs.uhis.ui.patient.viewmodel.MedicalReviewPatientHistoryViewModel
import org.medtroniclabs.uhis.ui.patient.viewmodel.NurseMedicalReviewViewModel
import org.medtroniclabs.uhis.ui.patient.viewmodel.PatientDetailViewModel
import org.medtroniclabs.uhis.ui.patient.viewmodel.PrescriptionCopyViewModel
import kotlin.getValue

class NurseMedicalReviewPrescriptionFragment :
    BaseFragment(),
    DateSelectionListener,
    View.OnClickListener,
    AdapterView.OnItemClickListener {
    private val viewModel: MedicalReviewBaseViewModel by activityViewModels()
    private lateinit var binding: FragmentNurseMedicalReviewPrescriptionBinding
    private lateinit var activityTagListCustomView: TagListCustomView
    private lateinit var prescriptionAdapter: PrescriptionAdapter
    private val patientViewModel: PatientDetailViewModel by activityViewModels()
    private val prescriptionViewModel: PrescriptionCopyViewModel by activityViewModels()
    private val medicalReviewPatientHistoryViewModel: MedicalReviewPatientHistoryViewModel by activityViewModels()
    private val nurseViewModel: NurseMedicalReviewViewModel by activityViewModels()

    private var listPopupWindow: PopupWindow? = null
    private var canSearch: Boolean = true

    companion object {
        const val TAG = "NurseMedicalReviewPrescriptionFragment"

        fun newInstance(): NurseMedicalReviewPrescriptionFragment = NurseMedicalReviewPrescriptionFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentNurseMedicalReviewPrescriptionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        attachObserver()
        loadChipView()
        getAllMedicationsList(true)
        searchView()
        prescriptionViewModel.patientTrackId = nurseViewModel.patientTrackId
        prescriptionViewModel.tenantId = SecuredPreference.getTenantId()
        // prescription-request/list & investigation/list expect the patient FHIR id
        // (patientTrackId = the server "id"), not the human-readable patientIdString.
        prescriptionViewModel.patientReference = nurseViewModel.patientTrackId?.toString()
        medicalReviewPatientHistoryViewModel.patientReference = nurseViewModel.patientTrackId?.toString()
    }

    private fun initView() {
        binding.tvRepeatPrescription.isEnabled = true
        binding.btnAddMedicine.safeClickListener(this)
        binding.tvRepeatPrescription.safeClickListener(this)
        binding.llPresActions.ivNext.safeClickListener(this)
        binding.llPresActions.ivPrevious.safeClickListener(this)
        binding.llPresActions.ivReload.safeClickListener(this)
        addTableRow(
            binding.tableLayout,
            listOf(
                getString(R.string.name).uppercase(),
                getString(R.string.dosage).uppercase(),
                getString(R.string.frequency).uppercase(),
                getString(R.string.instructions_caps),
                getString(R.string.duration_caps),
            ),
            isHeader = true,
        )
        nurseViewModel.nurseMrRequestModel.prescription =
            nurseViewModel.nurseMrRequestModel.prescription ?: Prescription(arrayListOf())
    }

    private fun getAllMedicationsList(
        isDefault: Boolean,
        search: String? = null,
    ) {
        if (!binding.searchView.isPopupShowing) {
            val countryId = SecuredPreference.getCountryId()
            val request = MedicationSearchReqModel(
                searchTerm = search?.trim(),
                countryId = countryId,
            )
            if (isDefault) {
                patientViewModel.getRecommendation(requireContext(), request)
            } else {
                patientViewModel.searchMedication(requireContext(), request)
            }
        }
    }

    private fun attachObserver() {
        patientViewModel.recommendationResponse.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.SUCCESS -> {
                    resourceState.data?.let { searchSuggestions ->
                        if (searchSuggestions.isNotEmpty()) {
                            val recommendations = mutableListOf<ChipViewItemModel>()
                            searchSuggestions
                                .sortedBy { item -> item.displayOrder }
                                .forEach { item ->
                                    item.medicationName?.let { medicationName ->
                                        val activityModel = ChipViewItemModel(
                                            name = medicationName,
                                            value = medicationName,
                                            type = medicationName,
                                        )
                                        recommendations.add(activityModel)
                                    }
                                }

                            recommendations.forEach { data ->
                                if (!medicalReviewPatientHistoryViewModel.chipList.contains(data)) {
                                    medicalReviewPatientHistoryViewModel.chipList.add(data)
                                }
                            }
                            val other = medicalReviewPatientHistoryViewModel.chipList.firstOrNull {
                                it.type.equals(
                                    DefinedParams.OTHER,
                                    true,
                                )
                            }
                            if (other != null) {
                                val lastIndex =
                                    medicalReviewPatientHistoryViewModel.chipList.size - 1
                                medicalReviewPatientHistoryViewModel.chipList.remove(other)
                                medicalReviewPatientHistoryViewModel.chipList.add(
                                    if (lastIndex > 0) lastIndex else 0,
                                    other,
                                )
                            }
                            updateChipList(medicalReviewPatientHistoryViewModel.chipList)
                        }
                    }
                }

                else -> {
                    // Invoked if response state is not success
                }
            }
        }

        patientViewModel.medicationSearchResponse.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.SUCCESS -> {
                    resourceState.data?.let { searchSuggestions ->
                        generateSuggestions(searchSuggestions)
                    }
                }

                else -> {
                    // Invoked if response state is not success
                }
            }
        }
        prescriptionViewModel.prescriptionListLiveDate.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    resourceState.data?.let {
                        prescriptionViewModel.prescriptionUIModel = ArrayList()
                        prescriptionViewModel.prescriptionUIModel!!.addAll(it)
                    } ?: kotlin.run {
                        prescriptionViewModel.prescriptionUIModel = null
                    }
                    loadPrescriptionListData()
                }
            }
        }

        prescriptionViewModel.frequencyList.observe(viewLifecycleOwner) {
            prescriptionViewModel.getDosageUnitList()
        }

        prescriptionViewModel.unitList.observe(viewLifecycleOwner) {
            prescriptionViewModel.getPrescriptionList(
                false,
            )
        }

        medicalReviewPatientHistoryViewModel.patientPrescriptionHistoryResponse.observe(
            viewLifecycleOwner,
        ) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    binding.ivRefresh.visibility = View.GONE
                    binding.CenterProgress.visibility = View.VISIBLE
                    binding.cardPrescriptionHistory.visibility = View.GONE
                }

                ResourceState.SUCCESS -> {
                    binding.ivRefresh.visibility = View.GONE
                    binding.CenterProgress.visibility = View.GONE
                    loadPatientPrescription(resourceState.data, true)
                }

                ResourceState.ERROR -> {
                    binding.ivRefresh.visibility = View.VISIBLE
                    binding.CenterProgress.visibility = View.GONE
                    binding.cardPrescriptionHistory.visibility = View.GONE
                }
            }
        }

        medicalReviewPatientHistoryViewModel.patientPrescriptionHistoryResponseBYID.observe(
            viewLifecycleOwner,
        ) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    binding.ivRefresh.visibility = View.GONE
                    binding.CenterProgress.visibility = View.VISIBLE
                    binding.cardPrescriptionHistory.visibility = View.GONE
                }

                ResourceState.SUCCESS -> {
                    binding.ivRefresh.visibility = View.GONE
                    binding.CenterProgress.visibility = View.GONE
                    binding.tvRepeatPrescription.isEnabled = true
                    loadPatientPrescription(resourceState.data, false)
                }

                ResourceState.ERROR -> {
                    binding.ivRefresh.visibility = View.VISIBLE
                    binding.CenterProgress.visibility = View.GONE
                    binding.cardPrescriptionHistory.visibility = View.GONE
                }
            }
        }

        nurseViewModel.patientDetailsResponse.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    binding.CenterProgress.visibility = View.VISIBLE
                }

                ResourceState.SUCCESS -> {
                    getPrescriptionHistory(resourceState.data)
                }

                ResourceState.ERROR -> {
                    binding.CenterProgress.visibility = View.GONE
                }
            }
        }
        reloadPrescriptionInstruction()
    }

    private fun updateChipList(chipItemList: List<ChipViewItemModel>) {
        activityTagListCustomView.addChipItemList(
            chipItemList,
            viewModel.selectedChipMedications,
        )
    }

    private fun loadChipView() {
        activityTagListCustomView = TagListCustomView(
            context = binding.root.context,
            chipGroup = binding.prescribeMedicationChipGroup,
        ) { name, _, isChecked ->
            viewModel.selectedChipMedications =
                ArrayList(activityTagListCustomView.getSelectedTags())
            if (isChecked) {
                setPrescribeMedicationView()
            } else {
                removeCheckList(listOf(name) as List<String>)
            }
        }
        activityTagListCustomView.addChipItemList(
            medicalReviewPatientHistoryViewModel.chipList,
            viewModel.selectedChipMedications,
        )
    }

    private fun removeCheckList(name: List<String>) {
        if (name.contains("other")) binding.clSearch.visibility = View.GONE
        if (name.isNotEmpty()) {
            val filteredItem =
                patientViewModel.recommendationResponse.value
                    ?.data
                    ?.filter { item ->
                        name.contains(item.medicationName)
                    }?.toList() ?: emptyList()
            filteredItem.forEach { removeChipView(it) }
        }
    }

    private fun removeChipView(model: PrescriptionModel) {
        for (i in 0 until binding.llMedicationList.childCount) {
            val currentView = binding.llMedicationList.getChildAt(i)
            if (currentView != null) {
                val textview = currentView.findViewById<TextView>(R.id.tvMedicationName)
                if (textview != null && textview.text.toString().equals(model.medicationName, true)
                ) {
                    binding.llMedicationList.removeView(currentView)
                    val iterator = prescriptionViewModel.prescriptionUIModel?.iterator()
                    if (iterator != null) {
                        while (iterator.hasNext()) {
                            val item = iterator.next()
                            if ((item.id == null && item.medicationName == model.medicationName) || item.id == model.id) {
                                iterator.remove()
                            }
                        }
                    }
                    // This for removing in ui
                    medicalReviewPatientHistoryViewModel.activityList?.let { list ->
                        val iterator = list.iterator()
                        while (iterator.hasNext()) {
                            val item = iterator.next()
                            if (item.value == model.medicationName) {
                                iterator.remove()
                            }
                        }
                    }
                    loadPrescriptionListData()
                }
            }
        }
    }

    private fun loadPrescriptionListData() {
        prescriptionViewModel.prescriptionUIModel?.let {
            val list = it.filter { model -> model.prescribedSince.isNullOrBlank() || model.isEdit }
            if (list.isNotEmpty()) {
                showRecyclerView()
                loadPrescriptionData(list)
            } else {
                hideRecyclerView()
            }
        }
    }

    private fun hideRecyclerView() {
        binding.apply {
            llMedicationList.visibility = View.GONE
            tvNoData.visibility = View.VISIBLE
            addCardPrescriptionLayout.visibility = View.GONE
        }
    }

    private fun showRecyclerView() {
        binding.apply {
            llMedicationList.visibility = View.VISIBLE
            tvNoData.visibility = View.GONE
            addCardPrescriptionLayout.visibility = View.VISIBLE
        }
    }

    private fun loadPrescriptionData(data: List<PrescriptionModel>) {
        binding.llMedicationList.removeAllViews()
        binding.addCardPrescriptionLayout.visibility = View.VISIBLE
        data.forEachIndexed { index, model ->
            if (model.prescribedSince.isNullOrBlank() || model.isEdit) {
                if (index == data.size - 1) {
                    loadMedicationEdit(model, true)
                } else {
                    loadMedicationEdit(model, false)
                }
            }
        }
    }

    private fun loadMedicationEdit(
        model: PrescriptionModel,
        dividerStatus: Boolean,
    ): PrescriptionModel {
        val medicationEditBinding = LayoutMedicationNewBinding.inflate(layoutInflater)
        medicationEditBinding.tvMedicationName.text =
            if (model.medicationName.isNullOrBlank()) getString(R.string.separator_hyphen) else model.medicationName
        if (model.dosageFormNameEntered.isNullOrBlank() ||
            (
                model.medicationId
                    ?: 0
            ) <= 0
        ) {
            otherMedicationEdit(medicationEditBinding, model)
        } else {
            medicationEditBinding.tvForm.visibility = View.GONE
            medicationEditBinding.spinnerForm.visibility = View.GONE
            medicationEditBinding.tvForm.text = model.dosageFormNameEntered
        }

        model.filledPrescriptionDays?.let {
            medicationEditBinding.etPrescribedDays.setText(it.toString())
        } ?: kotlin.run {
            medicationEditBinding.etPrescribedDays.setText(getString(R.string.empty_space))
        }

        medicationEditBinding.tvMedicineErrorMessage.tag =
            "${model.datetime}${model.medicationName}"

        medicationEditBinding.divider.visibility = dividerVisibility(dividerStatus)

        medicationEditBinding.etInstruction.text =
            model.instructionEntered ?: getString(R.string.empty_space)

        validateMedication(model)

        // Dosage Unit Value
        medicationEditBinding.etDosage.visibility = View.VISIBLE
        NCDMRUtil.applyDosageInputRestrictions(medicationEditBinding.etDosage)
        medicationEditBinding.etDosage.setText(
            model.enteredDosageUnitValue ?: getString(R.string.empty_space),
        )

        medicationEditBinding.tvDosage.visibility = View.GONE
        medicationEditBinding.tvDosage.text = getString(R.string.empty_space)

        // Dosage Unit Name
        medicationEditBinding.tvUnitVal.visibility = View.GONE
        medicationEditBinding.tvUnitVal.text = getString(R.string.empty_space)

        medicationEditBinding.tvUnit.visibility = View.VISIBLE
        val dosageAdapter = CustomSpinnerAdapter(requireContext())
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
        val adapter = CustomSpinnerAdapter(requireContext())
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
                        model.dosageFrequencyEntered =
                            it[DefinedParams.ID].toString().toLong()
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
                model.enteredDosageUnitValue = value.trim().toString()
            } ?: kotlin.run {
                model.enteredDosageUnitValue = null
            }
        }
        medicationEditBinding.etPrescribedDays.addTextChangedListener {
            checkValue(it)?.let { value ->
                model.filledPrescriptionDays = value.trim().toString().toIntOrNull()
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
            if (model.id == null) {
                model.medicationName?.let { checkedMedication ->
                    if (!checkedMedication.equals(DefinedParams.OTHER, true)) {
                        activityTagListCustomView.populateChipByName(
                            chipGroup = binding.prescribeMedicationChipGroup,
                            medicalReviewPatientHistoryViewModel.chipList,
                            checkedMedication,
                        ) {}
                    }
                }
            }
            prescriptionViewModel.prescriptionUIModel?.remove(model)
            loadPrescriptionListData()
        }
        medicationEditBinding.etInstruction.safeClickListener {
            InstructionExpansionDialog
                .newInstance(model)
                .show(childFragmentManager, InstructionExpansionDialog.TAG)
        }
        medicationEditBinding.tvMedicationName.tag = if (model.id == null) "chip" else "history"
        binding.llMedicationList.addView(medicationEditBinding.root)
        return model
    }

    private fun otherMedicationEdit(
        medicationEditBinding: LayoutMedicationNewBinding,
        model: PrescriptionModel,
    ) {
        medicationEditBinding.llForm.visibility = View.VISIBLE
        val spinnerFormAdapter = CustomSpinnerAdapter(requireContext())
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
            if (it != getString(R.string.please_select) &&
                spinnerFormAdapter.getIndexOfItemByName(
                    it,
                ) != -1
            ) {
                medicationEditBinding.spinnerForm.setSelection(
                    spinnerFormAdapter.getIndexOfItemByName(
                        it,
                    ),
                    true,
                )
            }
        } ?: kotlin.run {
            medicationEditBinding.spinnerForm.setSelection(0, true)
        }
    }

    private fun dividerVisibility(dividerStatus: Boolean): Int =
        if (dividerStatus) {
            View.GONE
        } else {
            View.VISIBLE
        }

    private fun generateSuggestions(searchResultList: ArrayList<PrescriptionModel>) {
        val searchResults = ArrayList<Pair<String, String>>()
        if (searchResultList.isNotEmpty()) {
            searchResultList.forEach {
                val unitValue = it.dosageUnitValue
                val unitName = it.dosageUnitName
                val dosage =
                    if (!unitValue.isNullOrBlank() && !unitName.isNullOrBlank()) "$unitValue $unitName, " else ""
                val search = "${it.medicationName}, ${it.brandName}, $dosage${it.dosageFormName}"
                if (isValidSuggestion(search)) {
                    searchResults.add(
                        Pair(
                            search,
                            it.classificationName ?: "",
                        ),
                    )
                }
            }
        }
        prescriptionAdapter.setData(searchResults)
        binding.searchView.setAdapter(prescriptionAdapter)
        if (searchResults.size > 0 && canSearch) binding.searchView.showDropDown()
    }

    private fun isValidSuggestion(search: String): Boolean {
        val searchStr = search.replace(", ", "", true)
        return searchStr.isNotEmpty()
    }

    private fun searchView() {
        prescriptionAdapter = PrescriptionAdapter(requireContext())
        binding.searchView.apply {
            setOnItemClickListener { _, _, position, _ ->
                hideKeyboard(this)
                viewModel.selectedMedication = null
                patientViewModel.medicationSearchResponse.value?.data?.let {
                    if (it.size > 0) {
                        viewModel.selectedMedication = it[position]
                        val search = getSearchString()
                        binding.searchView.setText(search)
                        binding.searchView.setSelection(search.length)
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
                    binding.btnAddMedicine.isEnabled = !binding.searchView.text.isNullOrBlank()
                    text?.toString()?.let {
                        if (it.isNotEmpty() && it.length > 1) {
                            getAllMedicationsList(false, it)
                        }
                    }
                }
            })
        }
        prescriptionViewModel.getFrequencyList()
    }

    private fun getSearchString(): String {
        var str = ""
        viewModel.selectedMedication?.let {
            val medicationName = it.medicationName ?: getString(R.string.empty_space)
            val brandName = it.brandName ?: getString(R.string.empty_space)
            val dosageName = it.dosageFormName ?: getString(R.string.empty_space)
            val unitValue = it.dosageUnitValue
            val unitName = it.dosageUnitName
            str =
                if (!unitValue.isNullOrBlank() &&
                    !unitName.isNullOrBlank()
                ) {
                    "$medicationName, $brandName, $unitValue $unitName, $dosageName"
                } else {
                    "$medicationName, $brandName, $dosageName"
                }
        }
        return str
    }

    private fun setPrescribeMedicationView() {
        val selectedItem = activityTagListCustomView.getSelectedTags() as ArrayList<ChipViewItemModel>
        val containsOther = selectedItem.any { it.name == "Other" }
        if (containsOther) {
            binding.clSearch.visibility =
                View.VISIBLE
        } else {
            binding.clSearch.visibility = View.GONE
        }

        val updatedMedicationList = updateChipList(
            viewModel.selectedChipMedications,
            medicalReviewPatientHistoryViewModel.activityList,
        )
        val fieldName = updatedMedicationList.map { it.value }.filter { it != "Other" }
        patientViewModel.recommendationResponse.value?.data?.let { data ->
            for (item in data) {
                if (item.medicationName != null && fieldName.contains(item.medicationName)) {
                    viewModel.selectedMedication = item
                    addMedicine()
                    return
                }
            }
        }
    }

    private fun updateChipList(
        newItems: List<ChipViewItemModel>,
        existingList: MutableList<ChipViewItemModel>?,
    ): List<ChipViewItemModel> {
        if (existingList == null) {
            medicalReviewPatientHistoryViewModel.activityList = newItems.toMutableList()
            return newItems
        }
        val newlyAddedItems = newItems.filter { newItem ->
            existingList.none { existingItem -> existingItem.value == newItem.value }
        }
        existingList.clear()
        existingList.addAll(newItems)
        return newlyAddedItems
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.btnAddMedicine -> {
                addMedicine()
            }

            binding.llPresActions.ivReload.id -> {
                listPopupWindow?.isOutsideTouchable = true
                listPopupWindow?.isFocusable = true
                listPopupWindow?.showAsDropDown(binding.llPresActions.ivReload)
            }

            binding.llPresActions.ivPrevious.id -> {
                getPreviousItemToCurrent()
            }

            binding.llPresActions.ivNext.id -> {
                getNextItemToCurrent()
            }

            binding.ivRefresh.id -> {
                nurseViewModel.patientDetailsResponse.value?.data?.let { patient ->
                    SecuredPreference.getTenantId().let {
                        if (medicalReviewPatientHistoryViewModel.selectedPatientPrescription == null) {
                            val request = PatientHistoryRequest(true, patient._id, it)
                            medicalReviewPatientHistoryViewModel.getPatientPrescriptionHistory(
                                request,
                            )
                        } else {
                            val request = PatientHistoryRequest(
                                false,
                                patient._id,
                                it,
                                patientVisitId = medicalReviewPatientHistoryViewModel.selectedPatientPrescription,
                            )
                            medicalReviewPatientHistoryViewModel.getPatientPrescriptionHistoryById(
                                request,
                            )
                        }
                    }
                }
            }

            binding.tvRepeatPrescription.id -> {
                editMedicine()
            }
        }
    }

    private fun addMedicine() {
        binding.btnAddMedicine.isEnabled = false
        val prescription = binding.searchView.text
        binding.searchView.setText(getString(R.string.empty_space))
        viewModel.selectedMedication?.let {
            if (prescriptionViewModel.prescriptionUIModel == null) {
                prescriptionViewModel.prescriptionUIModel = ArrayList()
                val model = PrescriptionModel(
                    isDeleted = false,
                    medicationId = it.id,
                    dosageFrequencyId = it.dosageFrequencyId,
                    prescribedDays = it.prescribedDays,
                    medicationName = it.medicationName,
                    dosageUnitValue = it.dosageUnitValue,
                    dosageUnitName = it.dosageUnitName,
                    dosageUnitId = it.dosageUnitId,
                    dosageFrequencyName = it.dosageFrequencyName,
                    instructionNote = it.instructionNote,
                    dosageFormName = it.dosageFormName,
                    dosageForm = it.dosageForm,
                    classificationName = it.classificationName,
                    brandName = it.brandName,
                )
                model.dosageFormNameEntered = it.dosageFormName
                prescriptionViewModel.prescriptionUIModel!!.add(
                    model,
                )
                viewModel.selectedMedication = null
            } else {
                val model = PrescriptionModel(
                    isDeleted = false,
                    medicationId = it.id,
                    dosageFrequencyId = it.dosageFrequencyId,
                    prescribedDays = it.prescribedDays,
                    medicationName = it.medicationName,
                    dosageUnitValue = it.dosageUnitValue,
                    dosageUnitName = it.dosageUnitName,
                    dosageUnitId = it.dosageUnitId,
                    dosageFrequencyName = it.dosageFrequencyName,
                    instructionNote = it.instructionNote,
                    dosageFormName = it.dosageFormName,
                    dosageForm = it.dosageForm,
                    classificationName = it.classificationName,
                    brandName = it.brandName,
                )
                model.dosageFormNameEntered = it.dosageFormName
                prescriptionViewModel.prescriptionUIModel!!.add(
                    model,
                )
                viewModel.selectedMedication = null
            }
            loadPrescriptionListData()
        } ?: kotlin.run {
            if (!prescription.isNullOrBlank()) {
                if (prescriptionViewModel.prescriptionUIModel == null) {
                    prescriptionViewModel.prescriptionUIModel = ArrayList()
                    prescriptionViewModel.prescriptionUIModel!!.add(
                        PrescriptionModel(
                            isDeleted = false,
                            medicationId = -1L,
                            dosageFrequencyId = null,
                            prescribedDays = null,
                            medicationName = prescription.toString(),
                            dosageUnitValue = null,
                            dosageUnitName = null,
                            dosageFrequencyName = null,
                            instructionNote = null,
                            dosageFormName = null,
                            dosageForm = null,
                        ),
                    )
                } else {
                    prescriptionViewModel.prescriptionUIModel!!.add(
                        PrescriptionModel(
                            isDeleted = false,
                            medicationId = -1L,
                            dosageFrequencyId = null,
                            prescribedDays = null,
                            medicationName = prescription.toString(),
                            dosageUnitValue = null,
                            dosageUnitName = null,
                            dosageFrequencyName = null,
                            instructionNote = null,
                            dosageFormName = null,
                            dosageForm = null,
                        ),
                    )
                }
                loadPrescriptionListData()
            }
        }
    }

    private fun validateMedication(model: PrescriptionModel) {
        if (model.enteredDosageUnitValue.isNullOrBlank()) {
            model.enteredDosageUnitValue =
                model.dosageUnitValue // API
        }
        if (model.dosageUnitSelected == null || model.dosageUnitSelected!! > 0) {
            model.dosageUnitSelected =
                model.dosageUnitId // API
        }
        if (model.dosageUnitNameEntered.isNullOrBlank()) {
            model.dosageUnitNameEntered =
                model.dosageUnitName // API
        }
        if (model.enteredDosageUnitValue.isNullOrBlank()) {
            model.enteredDosageUnitValue =
                model.dosageUnitValue
        }
        if (model.dosageFrequencyNameEntered.isNullOrBlank()) {
            model.dosageFrequencyNameEntered =
                model.dosageFrequencyName
        }
        if (model.dosageFrequencyEntered == null || model.dosageFrequencyEntered!! > 0) {
            model.dosageFrequencyEntered =
                model.dosageFrequencyId
        }

        if (model.medicationId == null) {
            model.medicationId = model.id
            model.id = null
        }

        if (model.dosageFormNameEntered == null && model.dosageFormName != null) {
            model.dosageFormNameEntered = model.dosageFormName
        }
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

    private fun getSpinnerPosition(
        dosageAdapter: CustomSpinnerAdapter,
        it: String,
    ): Int =
        if (it != getString(R.string.please_select) &&
            dosageAdapter.getIndexOfItemByName(
                it,
            ) != -1
        ) {
            dosageAdapter.getIndexOfItemByName(
                it,
            )
        } else {
            0
        }

    private fun editSpinnerFrequency(
        selectedItem: Map<String, Any>,
        medicationEditBinding: LayoutMedicationNewBinding,
        model: PrescriptionModel,
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

    private fun ivResetRemove(
        edit: Boolean,
        medicationEditBinding: LayoutMedicationNewBinding,
    ) {
        if (edit) {
            medicationEditBinding.ivResetMedication.visibility = View.VISIBLE
            medicationEditBinding.ivRemoveMedication.visibility = View.GONE
        } else {
            medicationEditBinding.ivResetMedication.visibility = View.GONE
            medicationEditBinding.ivRemoveMedication.visibility = View.VISIBLE
        }
    }

    private fun checkValue(it: Editable?): Editable? {
        if (it.isNullOrBlank()) return null
        return if (NCDMRUtil.isValidDosageValue(it.toString())) it else null
    }

    override fun onItemClick(
        p0: AdapterView<*>?,
        p1: View?,
        p2: Int,
        p3: Long,
    ) {
        //
    }

    private fun loadPatientPrescription(
        data: PatientPrescriptionHistoryResponse?,
        updateDate: Boolean,
    ) {
        data?.let { prescription ->
            if (prescription.patientPrescription.isNotEmpty()) {
                binding.cardPrescriptionHistory.visibility = View.VISIBLE
                clearTableRowsExceptHeader(binding.tableLayout)
                prescription.patientPrescription.forEachIndexed { index, prescriptionItem ->
                    val daysText = if ((
                            prescriptionItem.prescribedDays
                                ?: 0
                        ) < 10
                    ) {
                        getString(R.string.day)
                    } else {
                        getString(R.string.days)
                    }

                    addTableRow(
                        binding.tableLayout,
                        listOf(
                            prescriptionItem.medicationName,
                            "${prescriptionItem.dosageUnitValue} ${prescriptionItem.dosageUnitName}",
                            prescriptionItem.dosageFrequencyName,
                            prescriptionItem.instructionNote,
                            "${prescriptionItem.prescribedDays} $daysText",
                        ),
                        indexColumn = index == prescription.patientPrescription.lastIndex,
                    )
                }

                val date = DateUtils.convertDateTimeToDate(
                    prescription.patientPrescription[0].createdAt,
                    DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                    DateUtils.DATE_DD_MMM_YYYY,
                )
                binding.tvDate.text =
                    date.takeIf { date.isNotEmpty() } ?: getString(R.string.hyphen_symbol)
                medicalReviewPatientHistoryViewModel.selectedPatientPrescription =
                    prescription.patientPrescription[0].patientVisitId
                binding.llPresActions.ivPrevious.isEnabled = checkForPreviousItem() != -1
                binding.llPresActions.ivNext.isEnabled = checkNextItem() != -1
                if (updateDate) {
                    loadDatesMenu(prescription.prescriptionHistoryDates)
                } else {
                    val view =
                        listPopupWindow?.contentView?.findViewById<RecyclerView>(R.id.rvDateList)
                    if (view != null && view.adapter is DateListAdapter) {
                        medicalReviewPatientHistoryViewModel.patientPrescriptionHistoryResponse.value?.data?.let {
                            view.adapter = DateListAdapter(
                                it.prescriptionHistoryDates,
                                medicalReviewPatientHistoryViewModel.selectedPatientPrescription,
                                this@NurseMedicalReviewPrescriptionFragment,
                            )
                        }
                    }
                }
            } else {
                binding.cardPrescriptionHistory.visibility = View.GONE
            }
        } ?: kotlin.run {
            binding.cardPrescriptionHistory.visibility = View.GONE
        }
    }

    private fun getPreviousItemToCurrent() {
        val selectedIndex = checkForPreviousItem()
        medicalReviewPatientHistoryViewModel.patientPrescriptionHistoryResponse.value?.data?.apply {
            if (selectedIndex != -1) {
                nurseViewModel.patientDetailsResponse.value?.data?.let { patientInfo ->
                    val request = PatientHistoryRequest(
                        false,
                        patientInfo._id,
                        SecuredPreference.getTenantId(),
                        patientVisitId = prescriptionHistoryDates[selectedIndex]._id,
                    )
                    medicalReviewPatientHistoryViewModel.getPatientPrescriptionHistoryById(
                        request,
                    )
                }
            }
        }
    }

    private fun checkForPreviousItem(): Int {
        var selectedIndex = -1
        medicalReviewPatientHistoryViewModel.patientPrescriptionHistoryResponse.value?.data?.apply {
            prescriptionHistoryDates.forEachIndexed { index, labTestDateModel ->
                if (labTestDateModel._id == medicalReviewPatientHistoryViewModel.selectedPatientPrescription) {
                    selectedIndex = index - 1
                }
            }
        }
        return selectedIndex
    }

    private fun checkNextItem(): Int {
        var selectedIndex = -1
        medicalReviewPatientHistoryViewModel.patientPrescriptionHistoryResponse.value?.data?.apply {
            prescriptionHistoryDates.forEachIndexed { index, labTestDateModel ->
                if (labTestDateModel._id == medicalReviewPatientHistoryViewModel.selectedPatientPrescription && index + 1 < prescriptionHistoryDates.size) {
                    selectedIndex = index + 1
                }
            }
        }
        return selectedIndex
    }

    private fun loadDatesMenu(prescriptionHistoryDates: ArrayList<VisitDateModel>) {
        val inflater =
            requireActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.layout_popup_window, null)
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvDateList)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.addItemDecoration(
            DividerItemDecoration(
                recyclerView.context,
                DividerItemDecoration.VERTICAL,
            ),
        )
        val adapter = DateListAdapter(
            prescriptionHistoryDates,
            medicalReviewPatientHistoryViewModel.selectedPatientPrescription,
            this,
        )
        recyclerView.adapter = adapter
        listPopupWindow = PopupWindow(
            view,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )
    }

    override fun onDateSelected(_id: Long) {
        SecuredPreference.getTenantId().let { tenantId ->
            nurseViewModel.patientDetailsResponse.value?.data?.let { patientInfo ->
                val request = PatientHistoryRequest(
                    false,
                    patientInfo._id,
                    tenantId,
                    patientVisitId = _id,
                )
                medicalReviewPatientHistoryViewModel.getPatientPrescriptionHistoryById(request)
            }
        }
        listPopupWindow?.dismiss()
    }

    private fun getPrescriptionHistory(data: PatientDetailsModel?) {
        data?.let { patient ->
            SecuredPreference.getTenantId().let {
                val request = PatientHistoryRequest(true, patient._id, it)
                medicalReviewPatientHistoryViewModel.getPatientPrescriptionHistory(request)
            }
        }
    }

    private fun getNextItemToCurrent() {
        val selectedIndex = checkNextItem()
        medicalReviewPatientHistoryViewModel.patientPrescriptionHistoryResponse.value?.data?.apply {
            if (selectedIndex != -1) {
                SecuredPreference.getTenantId().let { tenantId ->
                    nurseViewModel.patientDetailsResponse.value?.data?.let { patientInfo ->
                        val request = PatientHistoryRequest(
                            false,
                            patientInfo._id,
                            tenantId,
                            patientVisitId = prescriptionHistoryDates[selectedIndex]._id,
                        )
                        medicalReviewPatientHistoryViewModel.getPatientPrescriptionHistoryById(
                            request,
                        )
                    }
                }
            }
        }
    }

    override fun onSessionSelected(model: SessionModel) {
        //
    }

    private fun addTableRow(
        tableLayout: TableLayout,
        rowData: List<String?>,
        isHeader: Boolean = false,
        indexColumn: Boolean = false,
    ) {
        if (tableLayout.background == null) {
            tableLayout.background = GradientDrawable().apply {
                setColor(Color.WHITE)
            }
        }

        val tableRow = TableRow(requireContext())
        tableRow.layoutParams = TableRow.LayoutParams(
            TableRow.LayoutParams.MATCH_PARENT,
            TableRow.LayoutParams.WRAP_CONTENT,
        )

        if (isHeader) {
            tableRow.background =
                ContextCompat.getDrawable(requireContext(), R.drawable.gradient_color)
        } else {
            tableRow.setBackgroundColor(Color.WHITE)
        }

        // Loop through each data item in the row
        for ((index, cellData) in rowData.withIndex()) {
            val textView = TextView(requireContext()).apply {
                layoutParams = TableRow.LayoutParams(
                    0,
                    TableRow.LayoutParams.MATCH_PARENT,
                    1f,
                )
                text = cellData ?: ""
                gravity = Gravity.CENTER
                setPadding(8, 16, 8, 16)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_black))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                if (isHeader) {
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.night_rider))

                    if (index == 0) {
                        val borderDrawable = GradientDrawable().apply {
                            setStroke(1, Color.GRAY) // Set the border color and width
                            cornerRadii = floatArrayOf(
                                15f,
                                15f, // Top-left corner (sharp)
                                0f,
                                0f, // Top-right corner (curved)
                                0f,
                                0f, // Bottom-left corner (sharp)
                                0f,
                                0f, // Bottom-right corner (curved)
                            )
                        }
                        background = borderDrawable
                    } else if (rowData.lastIndex == index) {
                        val borderDrawable = GradientDrawable().apply {
                            setStroke(1, Color.GRAY) // Set the border color and width
                            cornerRadii = floatArrayOf(
                                0f,
                                0f,
                                15f,
                                15f,
                                0f,
                                0f,
                                0f,
                                0f,
                            )
                        }
                        background = borderDrawable
                    } else {
                        // Create a border drawable with a stroke for header cells
                        val borderDrawable = GradientDrawable().apply {
                            setStroke(1, Color.GRAY)
                        }
                        background = borderDrawable
                    }
                } else {
                    if (indexColumn) {
                        if (index == 0) {
                            val borderDrawable = GradientDrawable().apply {
                                setStroke(1, Color.GRAY) // Set the border color and width
                                cornerRadii = floatArrayOf(
                                    0f,
                                    0f,
                                    0f,
                                    0f,
                                    0f,
                                    0f,
                                    15f,
                                    15f,
                                )
                            }
                            background = borderDrawable
                        } else if (rowData.lastIndex == index) {
                            val borderDrawable = GradientDrawable().apply {
                                setStroke(1, Color.GRAY) // Set the border color and width
                                cornerRadii = floatArrayOf(
                                    0f,
                                    0f,
                                    0f,
                                    0f,
                                    15f,
                                    15f,
                                    0f,
                                    0f, // Bottom-right corner (curved)
                                )
                            }
                            background = borderDrawable
                        } else {
                            // Non-header rows - set border with a different background (white color)
                            val borderDrawable = GradientDrawable().apply {
                                setStroke(1, Color.GRAY)
                                setColor(Color.WHITE)
                            }
                            background = borderDrawable
                        }
                    } else {
                        val borderDrawable = GradientDrawable().apply {
                            setStroke(1, Color.GRAY) // Set the border color and width
                        }
                        background = borderDrawable
                    }
                }
            }
            tableRow.addView(textView)
        }

        // Add the table row to the TableLayout
        tableLayout.addView(
            tableRow,
            TableLayout
                .LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    TableLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    setMargins(0, 0, 0, 0)
                },
        )
    }

    private fun clearTableRowsExceptHeader(tableLayout: TableLayout) {
        while (tableLayout.childCount > 1) {
            tableLayout.removeViewAt(1)
        }
    }

    fun createOrUpdatePrescription(): Boolean {
        var isMedication = true
        val list = prescriptionViewModel.prescriptionUIModel?.filter { it.isEdited || it.prescribedSince.isNullOrBlank() }
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
                        val finalModel = UpdateMedicationModel(
                            id = it.id,
                            medicationId = it.medicationId,
                            prescriptionId = it.prescriptionId,
                            medicationName = it.medicationName,
                            dosageUnitName = it.dosageUnitNameEntered,
                            dosageUnitValue = it.enteredDosageUnitValue,
                            dosageFormName = it.dosageFormNameEntered,
                            dosageFrequencyName = it.dosageFrequencyNameEntered,
                            dosageFrequencyId = it.dosageFrequencyEntered,
                            prescribedDays = it.filledPrescriptionDays,
                            instructionNote = it.instructionEntered,
                            isDeleted = false,
                            dosageUnitId = it.dosageUnitSelected,
                            classificationName = it.classificationName,
                            brandName = it.brandName,
                        )
                        prescriptionViewModel.savePrescriptionList?.add(finalModel)
                        changeAssignPrescriptionModel(prescriptionViewModel.savePrescriptionList)
                    } else {
                        isMedication = invalidPrescription(it, invalidList)
                        return@forEachIndexed
                    }
                }
            }
        }
        return isMedication
    }

    private fun changeAssignPrescriptionModel(finalModel: ArrayList<UpdateMedicationModel>?) {
        val prescriptionItems = finalModel?.map { prescriptionModel ->
            PrescriptionItem(
                id = prescriptionModel.prescriptionId,
                medicationId = prescriptionModel.medicationId,
                dosageFrequencyId = prescriptionModel.dosageFrequencyId,
                dosageUnitId = prescriptionModel.dosageUnitId,
                endDate = prescriptionModel.endDate,
                prescribedDays = prescriptionModel.prescribedDays,
                classificationName = prescriptionModel.classificationName,
                brandName = prescriptionModel.brandName,
                medicationName = prescriptionModel.medicationName,
                dosageUnitValue = prescriptionModel.dosageUnitValue?.takeIf { it.isNotEmpty() }, // Safe conversion
                dosageUnitName = prescriptionModel.dosageUnitName,
                dosageFrequencyName = prescriptionModel.dosageFrequencyName,
                instructionNote = prescriptionModel.instructionNote,
                dosageFormName = prescriptionModel.dosageFormName,
                prescribedSince = prescriptionModel.prescribedSince,
                prescriptionRemainingDays = prescriptionModel.prescriptionRemainingDays,
                discontinuedOn = prescriptionModel.discontinuedOn,
            )
        } as ArrayList<PrescriptionItem>
        nurseViewModel.nurseMrRequestModel.prescription?.prescriptionList = prescriptionItems
    }

    private fun prescriptionValidation(prescriptionModel: PrescriptionModel): Pair<Boolean, ArrayList<String>> {
        prescriptionModel.let { prescription ->
            var isValid = true
            val invalidList = ArrayList<String>()
            if (!NCDMRUtil.isValidDosageValue(prescription.enteredDosageUnitValue)) {
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
                prescription.filledPrescriptionDays?.toString().isNullOrBlank()
            ) {
                isValid = false
                invalidList.add(getString(R.string.prescribed_days))
            }
            return Pair(isValid, invalidList)
        }
    }

    private fun validateSpinnerValue(it: String): Boolean = it.isEmpty() || it == getString(R.string.please_select)

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
                        view.text = "${getString(R.string.please_enter_details_prefix)} ${
                            invalidList.joinToString(separator = ", ")
                        }"
                    }
                }
            }
        }
    }

    private fun validateErrorMessage(data: PrescriptionModel) {
        showHideMedicineErrorMessage(
            data.datetime,
            data.medicationName,
            ArrayList(),
            View.GONE,
        )
    }

    private fun invalidPrescription(
        it: PrescriptionModel,
        invalidList: java.util.ArrayList<String>,
    ): Boolean {
        if (prescriptionViewModel.prescriptionUIModel != null && prescriptionViewModel.prescriptionUIModel!!.isNotEmpty()) {
            showHideMedicineErrorMessage(
                it.datetime,
                it.medicationName,
                invalidList,
                View.VISIBLE,
            )
        }
        prescriptionViewModel.savePrescriptionList?.clear()
        return false
    }

    private fun getViewByTag(tag: Any): View? = binding.root.findViewWithTag(tag)

    private fun reloadPrescriptionInstruction() {
        prescriptionViewModel.reloadInstruction.observe(viewLifecycleOwner) {
            if (it) {
                loadPrescriptionListData()
            }
        }
    }

    private fun editMedicine() {
        binding.tvRepeatPrescription.isEnabled = false
        showRecyclerView()
        showHistory()
        removePrevHistory()
        medicalReviewPatientHistoryViewModel.prescriptionList?.forEach {
            it.dosageFormNameEntered = it.dosageFormName
            it.filledPrescriptionDays = it.prescribedDays
            loadMedicationEdit(model = it, false)
        }
        medicalReviewPatientHistoryViewModel.prescriptionList = emptyList()
        // (activity as? NurseMedicalReviewActivity)?.goToPrescriptionList()
    }

    private fun removePrevHistory() {
        val views = ArrayList<View>()
        for (i in 0 until binding.llMedicationList.childCount) {
            val currentView = binding.llMedicationList.getChildAt(i)
            if (currentView != null) {
                val textview = currentView.findViewById<TextView>(R.id.tvMedicationName)
                if (textview.tag?.toString().equals("chip", true)) {
                    views.add(currentView)
                }
            }
        }
        binding.llMedicationList.removeAllViews()
        views.forEach { binding.llMedicationList.addView(it) }
    }

    private fun showHistory() {
        val patientPrescriptionHistoryById = medicalReviewPatientHistoryViewModel.patientPrescriptionHistoryResponseBYID.value
            ?.data
            ?.patientPrescription
        val patientPrescriptionHistory = medicalReviewPatientHistoryViewModel.patientPrescriptionHistoryResponse.value
            ?.data
            ?.patientPrescription

        val prescriptionHistory = patientPrescriptionHistoryById ?: patientPrescriptionHistory

        medicalReviewPatientHistoryViewModel.prescriptionList = prescriptionHistory.orEmpty().map { prescription ->
            PrescriptionModel(
                id = prescription.id,
                medicationId = prescription.medicationId,
                prescriptionId = prescription.prescriptionId,
                medicationName = prescription.medicationName,
                dosageUnitValue = prescription.dosageUnitValue,
                dosageUnitName = prescription.dosageUnitName,
                dosageFrequencyName = prescription.dosageFrequencyName,
                prescribedDays = prescription.prescribedDays,
                prescribedSince = prescription.prescribedSince,
                dosageFormName = prescription.dosageFormName,
                instructionNote = prescription.instructionNote,
                classificationName = prescription.classificationName,
                brandName = prescription.brandName,
            )
        }

        prescriptionViewModel.prescriptionUIModel = (prescriptionViewModel.prescriptionUIModel ?: arrayListOf()).apply {
            removeAll { it.medicationId != null }
            medicalReviewPatientHistoryViewModel.prescriptionList?.forEach {
                it.dosageFormNameEntered = it.dosageForm
                it.filledPrescriptionDays = it.prescribedDays
            }
            addAll(medicalReviewPatientHistoryViewModel.prescriptionList ?: emptyList())
        }
    }

    fun repeatedPrescriptionView(): View? = binding.addCardPrescriptionLayout
}
