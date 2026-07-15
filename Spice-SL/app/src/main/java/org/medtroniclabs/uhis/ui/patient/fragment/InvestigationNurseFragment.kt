package org.medtroniclabs.uhis.ui.patient.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.medicalreview.ResLabTestRecommendations
import org.medtroniclabs.uhis.data.model.ChipViewItemModel
import org.medtroniclabs.uhis.data.registration.LabTest
import org.medtroniclabs.uhis.data.registration.LabTestListResponse
import org.medtroniclabs.uhis.data.registration.LabTestModel
import org.medtroniclabs.uhis.data.registration.PatientDetailsModel
import org.medtroniclabs.uhis.data.registration.PatientHistoryRequest
import org.medtroniclabs.uhis.databinding.FragmentInvestigationNurseBinding
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.TagListCustomView
import org.medtroniclabs.uhis.ui.dialog.GeneralSuccessDialog
import org.medtroniclabs.uhis.ui.patient.adapter.LabtestHistoryAdapter
import org.medtroniclabs.uhis.ui.patient.viewmodel.LabTestViewModel
import org.medtroniclabs.uhis.ui.patient.viewmodel.MedicalReviewBaseViewModel
import org.medtroniclabs.uhis.ui.patient.viewmodel.MedicalReviewPatientHistoryViewModel
import org.medtroniclabs.uhis.ui.patient.viewmodel.NurseMedicalReviewViewModel
import kotlin.getValue

class InvestigationNurseFragment :
    BaseFragment(),
    View.OnClickListener,
    LabtestHistoryAdapter.LabTestInterface,
    AdapterView.OnItemClickListener {
    private lateinit var binding: FragmentInvestigationNurseBinding
    private lateinit var labTestCreateAdapter: LabtestHistoryAdapter
    private lateinit var addLabTestCreateAdapter: LabtestHistoryAdapter
    private val viewModel: LabTestViewModel by viewModels()
    private lateinit var activityTagListCustomView: TagListCustomView
    private val medicalReviewBaseViewModel: MedicalReviewBaseViewModel by activityViewModels()
    private val medicalReviewPatientHistoryViewModel: MedicalReviewPatientHistoryViewModel by activityViewModels()
    private val nurseViewModel: NurseMedicalReviewViewModel by activityViewModels()

    companion object {
        const val TAG = "NurseMedicalReviewLabTestFragment"

        fun newInstance(): InvestigationNurseFragment = InvestigationNurseFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentInvestigationNurseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initializeView()
        attachObservers()
        loadChipView()
    }

    private fun initializeView() {
        binding.investigationCard.tvValueBy.visibility = View.GONE
        binding.addInvestigationCard.tvValueBy.visibility = View.GONE
        labTestCreateAdapter = LabtestHistoryAdapter(true)
        labTestCreateAdapter.setanInterfaceListener(this)
        addLabTestCreateAdapter = LabtestHistoryAdapter(false, 2)
        addLabTestCreateAdapter.setanInterfaceListener(this)
        // History card
        binding.investigationCard.rvTestList.layoutManager =
            LinearLayoutManager(binding.investigationCard.rvTestList.context ?: requireContext())
        binding.investigationCard.rvTestList.adapter = labTestCreateAdapter
        // Add investigation card
        binding.addInvestigationCard.rvTestList.layoutManager =
            LinearLayoutManager(binding.investigationCard.rvTestList.context ?: requireContext())
        binding.addInvestigationCard.rvTestList.adapter = addLabTestCreateAdapter
        // Chip List api call
        SecuredPreference.getCountryId()?.let {
            medicalReviewPatientHistoryViewModel.getPatientLabTestRecommendation(it)
        }
        nurseViewModel.nurseMrRequestModel.labTest = arrayListOf()
        binding.ivRefresh.visibility = View.GONE
    }

    private fun attachObservers() {
        // History card Lab test api
        medicalReviewPatientHistoryViewModel.labTestListResponse.observe(viewLifecycleOwner) { resourceState ->
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
                        loadLabTestList(it)
                    }
                }
            }
        }
        medicalReviewPatientHistoryViewModel.labTestListContinousResponse.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }
                ResourceState.ERROR -> {
                    hideLoading()
                    resourceState?.message?.let { message ->
                        (activity as BaseActivity).showErrorDialogue(
                            getString(R.string.error),
                            message,
                            isNegativeButtonNeed = false,
                        ) {}
                    }
                }
                ResourceState.SUCCESS -> {
                    hideLoading()
                    resourceState.data?.let {
                        loadLabAddTestList(it)
                    }
                }
            }
        }

        viewModel.referLabTestResponse.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                    resourceState.message?.let { message ->
                        (activity as BaseActivity).showErrorDialogue(
                            getString(R.string.error),
                            message,
                            isNegativeButtonNeed = false,
                        ) {}

                        viewModel.isToRefer.value = true
                    }
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    resourceState.data?.let {
                        requireActivity().finish()
                    }
                }
            }
        }

        // Result for the Lab Test
        viewModel.resultDetailsResponse.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    resourceState.data?.let { data ->
                        loadResultDetails(data)
//                        isAfricaOrNot(data)
                    }
                }
            }
        }

        // on Edit icon click response
        viewModel.labTestResultResponse.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                    resourceState?.message?.let { message ->
                        (activity as BaseActivity).showErrorDialogue(
                            getString(R.string.error),
                            message,
                            isNegativeButtonNeed = false,
                        ) {}
                    }
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    resourceState.data?.let {
                        newLabTestSuccess()
                    }
                }
            }
        }

        viewModel.reviewResultResponse.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    labTestCreateAdapter.updateReviewCommentsView()
                }
            }
        }

        viewModel.removeLabTestResponse.observe(viewLifecycleOwner, ::removeLabTest)
        nurseViewModel.patientDetailsResponse.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
//                    binding.CenterProgress.visibility = View.VISIBLE
                }

                ResourceState.SUCCESS -> {
                    getMedicalReview(resourceState.data)
                }

                ResourceState.ERROR -> {
//                    binding.CenterProgress.visibility = View.GONE
                }
            }
        }

        medicalReviewPatientHistoryViewModel.patientLabTestRecommendation.observe(viewLifecycleOwner) { resourceState ->
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
                        it.forEach { item ->
                            item.testName
                                ?.let { medicationName ->
                                    ChipViewItemModel(
                                        name = medicationName,
                                        value = medicationName,
                                        type = medicationName,
                                    )
                                }?.let { data ->
                                    if (!medicalReviewPatientHistoryViewModel.chipListLT.contains(data)) {
                                        medicalReviewPatientHistoryViewModel.chipListLT.add(data)
                                    }
                                }
                        }
                    } ?: kotlin.run {
                        medicalReviewPatientHistoryViewModel.investigationUIModel = null
                    }

                    updateChipList(medicalReviewPatientHistoryViewModel.chipListLT)
                }
            }
        }
    }

    private fun loadChipView() {
        activityTagListCustomView = TagListCustomView(
            context = binding.root.context,
            chipGroup = binding.prescribeMedicationChipGroup,
        ) { name, _, isChecked ->
            medicalReviewBaseViewModel.selectedChipLabTestMedication =
                ArrayList(activityTagListCustomView.getSelectedTags())
            if (isChecked) {
                onClickedRecommendedMedication()
            } else {
                name?.let {
                    removeCheckList(listOf(it))
                }
            }
        }
        activityTagListCustomView.addChipItemList(
            medicalReviewPatientHistoryViewModel.chipListLT,
            medicalReviewBaseViewModel.selectedChipLabTestMedication,
        )
    }

    private fun removeCheckList(name: List<String>) {
        if (name.isNotEmpty()) {
            val filteredItem =
                medicalReviewPatientHistoryViewModel.patientLabTestRecommendation.value
                    ?.data
                    ?.filter { item ->
                        name.contains(item.testName)
                    }?.toList() ?: emptyList()
            filteredItem.forEach { remove(it) }
        }
    }

    fun remove(model: ResLabTestRecommendations) {
        val iterator =
            medicalReviewPatientHistoryViewModel.investigationUIModel?.iterator()
        if (iterator != null) {
            while (iterator.hasNext()) {
                val item = iterator.next()
                if (item.testName == model.testName) {
                    iterator.remove()
                }
            }
        }
        val iteratorList = viewModel.labTestAddLists.iterator()
        if (iteratorList != null) {
            while (iteratorList.hasNext()) {
                val item = iteratorList.next()
                if (item.labTestName == model.testName && item._id == null) {
                    iteratorList.remove()
                }
            }
        }
        // This for removing in ui
        medicalReviewPatientHistoryViewModel.activityList?.let { list ->
            val iterator = list.iterator()
            while (iterator.hasNext()) {
                val item = iterator.next()
                if (item.value == model.testName) {
                    iterator.remove()
                }
            }
        }
        reformatLabTestResponse()
    }

    private fun onClickedRecommendedMedication() {
        val updatedMedicationList = updateChipList(
            medicalReviewBaseViewModel.selectedChipLabTestMedication,
            medicalReviewPatientHistoryViewModel.activityList,
        )
        val fieldName = updatedMedicationList.map { it.value }.filter { it != "Other" }
        medicalReviewPatientHistoryViewModel.patientLabTestRecommendation.value?.data?.let { data ->
            for (item in data) {
                if (item.testName != null && fieldName.contains(item.testName)) {
                    medicalReviewBaseViewModel.selectedLabTestMedication = item
                    addLabTest()
                    return
                }
            }
        }
    }

    private fun addLabTest() {
        medicalReviewBaseViewModel.selectedLabTestMedication?.let {
            if (medicalReviewPatientHistoryViewModel.investigationUIModel == null) {
                medicalReviewPatientHistoryViewModel.investigationUIModel = ArrayList()
                medicalReviewPatientHistoryViewModel.investigationUIModel!!.add(
                    it,
                )
                medicalReviewBaseViewModel.selectedLabTestMedication = null
            } else {
                medicalReviewPatientHistoryViewModel.investigationUIModel!!.add(
                    it,
                )
                medicalReviewBaseViewModel.selectedLabTestMedication = null
            }
            reformatLabTestResponse()
        }
    }

    private fun reformatLabTestResponse() {
        val labTestListResponse = ArrayList<LabTestModel>()
        labTestListResponse.clear()
        medicalReviewPatientHistoryViewModel.investigationUIModel?.let { list ->
            list.forEach {
                labTestListResponse.add(
                    LabTestModel(
                        _id = null,
                        labTestId = it.id?.toLong(),
                        labTestName = it.testName,
                        patientVisitId = 0L,
                        referredDate = DateUtils.getTodayDateDDMMYYYY(),
                        referredBy = null,
                    ),
                )
            }
            list.clear()
        }

        labTestListResponse.let { list ->
            viewModel.labTestAddLists.addAll((list))
            list.clear()
        }
        if (viewModel.labTestAddLists.isEmpty()) {
            binding.todayInvestigationCard.visibility = View.GONE
        } else {
            binding.todayInvestigationCard.visibility = View.VISIBLE
        }

        addLabTestCreateAdapter.submitData(viewModel.labTestAddLists)
        binding.addInvestigationCard.rvTestList.adapter = addLabTestCreateAdapter
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

    private fun updateChipList(chipItemList: List<ChipViewItemModel>) {
        activityTagListCustomView.addChipItemList(
            chipItemList,
            medicalReviewBaseViewModel.selectedChipLabTestMedication,
        )
    }

    private fun getMedicalReview(data: PatientDetailsModel?) {
        data?.let { patient ->
            medicalReviewPatientHistoryViewModel.patientTrackId = patient._id
            // investigation/list expects the patient FHIR id (_id = the server "id"),
            // not the human-readable patientId.
            medicalReviewPatientHistoryViewModel.patientReference = patient._id.toString()
            // for Current history
            medicalReviewPatientHistoryViewModel.getLabTestList()
            medicalReviewPatientHistoryViewModel.getLabTestContinousList()
        }
    }

    private fun removeLabTest(resourceState: Resource<HashMap<String, Any>>) {
        when (resourceState.state) {
            ResourceState.LOADING -> {
                showLoading()
            }

            ResourceState.ERROR -> {
                hideLoading()
            }

            ResourceState.SUCCESS -> {
                hideLoading()
                resourceState.data?.let { result ->
                    removeLabTestSuccess(result)
                }
            }
        }
    }

    private fun removeLabTestSuccess(result: HashMap<String, Any>) {
        val model = result[DefinedParams.OTHER] as LabTestModel
        val index = viewModel.labTestAddLists.indexOfFirst { model._id == it._id }
        if (index >= 0) {
            viewModel.labTestAddLists.removeAt(index)
        }
        if (viewModel.labTestAddLists.size == 0) {
            binding.todayInvestigationCard.visibility = View.GONE
        } else {
            binding.todayInvestigationCard.visibility = View.VISIBLE
        }
    }

    private fun newLabTestSuccess() {
        AddLabTestResultDialog
            .newInstance(viewModel.editModel!!) { status ->
                if (status) {
                    GeneralSuccessDialog
                        .newInstance(
                            getString(R.string.lab_test),
                            getString(R.string.lab_test_saved_successfully),
                            okayButton = getString(R.string.okay),
                            callback = { refreshLabTest() },
                        ).show(childFragmentManager, GeneralSuccessDialog.TAG)
                }
            }.show(
                childFragmentManager,
                AddLabTestResultDialog.TAG,
            )
    }

    private fun refreshLabTest() {
        nurseViewModel.patientDetailsResponse.value?.data?.let {
            getMedicalReview(it)
        }
    }

    private fun loadResultDetails(data: java.util.HashMap<String, Any>) {
        val comment =
            if (data.containsKey(DefinedParams.COMMENT)) data[DefinedParams.COMMENT] as String? else ""
        if (data.containsKey(DefinedParams.PATIENT_LABTEST_RESULTS)) {
            val resultsData =
                data[DefinedParams.PATIENT_LABTEST_RESULTS] as List<Map<String, Any>>
            labTestCreateAdapter.showResultDetails(resultsData, comment)
        }
    }

    private fun loadLabTestList(it: LabTestListResponse) {
        if (it.patientLabTest?.isNotEmpty() == true) {
            binding.cardInvestigationHistory.visibility = View.VISIBLE
            binding.divider.visibility = View.VISIBLE
            medicalReviewPatientHistoryViewModel.patientVisitId = it.patientLabTest?.get(0)?.patientVisitId ?: -1L
        } else {
            viewModel.labTestLists.clear()
            binding.cardInvestigationHistory.visibility = View.GONE
            binding.divider.visibility = View.GONE
        }
        it.patientLabTest?.let { list ->
            viewModel.labTestLists = ArrayList(list)

            if (viewModel.labTestLists.size > 0) {
                hideLoading()
                labTestCreateAdapter.submitData(viewModel.labTestLists)
                binding.investigationCard.rvTestList.adapter = labTestCreateAdapter
            }
        }
    }

    private fun fetchResults(model: LabTestModel) {
        // Result values are delivered inline by investigation/list (mapped into labResultDetails);
        // the relational result/details endpoint no longer exists on the FHIR backend.
        labTestCreateAdapter.showResultDetails(
            model.labResultDetails ?: emptyList(),
            model.resultComments,
        )
    }

    override fun onClick(mView: View?) {
        when (view?.id) {
            R.id.ivPrevious -> {
                getPreviousItemToCurrent()
            }

            R.id.ivNext -> {
                getNextItemToCurrent()
            }
        }
    }

    private fun onEditClicked(model: LabTestModel?) {
        if (model?._id != null) {
            viewModel.editModel = model
            nurseViewModel.labTestId = model._id
            // Result fields come from the lab test's inline form definition (formInput); the old
            // relational result/list endpoint no longer exists on the FHIR backend.
            viewModel.buildLabTestResultFields(model.labTestName)
        }
    }

    override fun onItemSelected(
        model: LabTestModel,
        isRemove: Boolean,
        loadResult: Boolean,
    ) {
        if (loadResult) {
            fetchResults(model)
        } else if (isRemove) {
            removeLabTest(model)
        } else if (!isRemove) {
            onEditClicked(model)
        }
    }

    private fun removeLabTest(model: LabTestModel) {
        if (model._id != null) {
            viewModel.removeLabTestFhir(model)
        } else {
            model.labTestName?.let {
                activityTagListCustomView.deselectChipByID(
                    chipGroup = binding.prescribeMedicationChipGroup,
                    it,
                )
                removeCheckList(listOf(it))
            }
            val index =
                viewModel.labTestAddLists.indexOfFirst { it.labTestName == model.labTestName && it._id == null }
            if (index >= 0) {
                viewModel.labTestAddLists.removeAt(index)
            }
        }
    }

    override fun setButtonEnabled(isEnabled: Boolean) {
        viewModel.isToRefer.value = isEnabled
    }

    override fun reviewResults(model: LabTestModel) {
        val dialog = childFragmentManager.findFragmentByTag(LabTestConfirmationDialog.TAG)

        if (dialog?.isVisible != true) {
            LabTestConfirmationDialog.newInstance(model).show(
                childFragmentManager,
                LabTestConfirmationDialog.TAG,
            )
        }
    }

    override fun onItemClick(
        parent: AdapterView<*>?,
        view: View?,
        position: Int,
        id: Long,
    ) {
    }

    private fun getNextItemToCurrent() {
        val selectedIndex = checkNextItem()
        medicalReviewPatientHistoryViewModel.patientLabTestHistoryResponse.value?.data?.apply {
            if (selectedIndex != -1) {
                nurseViewModel.patientDetailsResponse.value?.data?.let { patientInfo ->
                    val request = PatientHistoryRequest(
                        false,
                        patientInfo._id,
                        SecuredPreference.getTenantId(),
                        patientVisitId = medicalReviewPatientHistoryViewModel.patientVisitId,
                    )
                    medicalReviewPatientHistoryViewModel.getPatientLabTestHistoryById(request)
                }
            }
        }
    }

    private fun checkNextItem(): Int {
        var selectedIndex = -1
        medicalReviewPatientHistoryViewModel.patientLabTestHistoryResponse.value?.data?.apply {
            patientLabtestDates.forEachIndexed { index, labTestDateModel ->
                if (labTestDateModel._id == medicalReviewPatientHistoryViewModel.selectedPatientId && index + 1 < patientLabtestDates.size) {
                    selectedIndex = index + 1
                }
            }
        }
        return selectedIndex
    }

    private fun getPreviousItemToCurrent() {
        val selectedIndex = checkForPreviousItem()
        medicalReviewPatientHistoryViewModel.patientLabTestHistoryResponse.value?.data?.apply {
            if (selectedIndex != -1) {
                nurseViewModel.patientDetailsResponse.value?.data?.let { patientInfo ->
                    val request = PatientHistoryRequest(
                        false,
                        patientInfo._id,
                        SecuredPreference.getTenantId(),
                        patientVisitId = patientLabtestDates[selectedIndex]._id,
                    )
                    medicalReviewPatientHistoryViewModel.getPatientLabTestHistoryById(request)
                }
            }
        }
    }

    private fun checkForPreviousItem(): Int {
        var selectedIndex = -1
        medicalReviewPatientHistoryViewModel.labTestListResponse.value?.data?.apply {
            patientLabtestDates?.forEachIndexed { index, labTestDateModel ->
                if (labTestDateModel._id == medicalReviewPatientHistoryViewModel.selectedPatientId) {
                    selectedIndex = index - 1
                }
            }
        }
        return selectedIndex
    }

    // Validation functions
    fun validation(isValid: Boolean) {
        if (isValid) {
            viewModel.labTestAddLists.forEach { labTestItem ->
                val labTestList = nurseViewModel.nurseMrRequestModel.labTest
                labTestList?.let { list ->
                    SecuredPreference.getUserDetails().let { user ->
                        if (labTestItem._id == null) {
                            val existingLabTest =
                                list.find { it.labTestId == labTestItem.labTestId }

                            if (existingLabTest != null) {
                                // Update existing lab test
                                existingLabTest.apply {
                                    labTestName = labTestItem.labTestName
                                    resultDate = labTestItem.resultDate
                                    isReviewed = labTestItem.isReviewed
                                    isAbnormal = labTestItem.isAbnormal
                                    comment = labTestItem.comment
                                }
                            } else {
                                // Ensure no duplicates using a Set
                                val uniqueLabTests = list.toMutableSet()
                                val newLabTest = LabTest(
                                    id = labTestItem._id,
                                    labTestId = labTestItem.labTestId,
                                    labTestName = labTestItem.labTestName,
                                    resultDate = labTestItem.resultDate,
                                    referredBy = user?.id,
                                    isReviewed = labTestItem.isReviewed,
                                    isAbnormal = labTestItem.isAbnormal,
                                    comment = labTestItem.comment,
                                )

                                if (!uniqueLabTests.any { it.labTestId == labTestItem.labTestId }) {
                                    uniqueLabTests.add(newLabTest)
                                    list.clear()
                                    list.addAll(uniqueLabTests)
                                }
                            }
                        } else {
                            // Directly add the new lab test if _id is not null
                            list.add(
                                LabTest(
                                    id = labTestItem._id,
                                    labTestId = labTestItem.labTestId,
                                    labTestName = labTestItem.labTestName,
                                    resultDate = labTestItem.resultDate,
                                    referredBy = user?.id,
                                    isReviewed = labTestItem.isReviewed,
                                    isAbnormal = labTestItem.isAbnormal,
                                    comment = labTestItem.comment,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }

    // Previous Lab Test Details
    private fun loadLabAddTestList(it: LabTestListResponse) {
        it.patientLabTest?.let { list ->
            val unsavedLabTests = viewModel.labTestAddLists.filter { it.referredBy == null }
            viewModel.labTestAddLists.clear()
            viewModel.labTestAddLists.addAll(list + unsavedLabTests)

            if (viewModel.labTestAddLists.size == 0) {
                binding.todayInvestigationCard.visibility = View.GONE
                binding.divider.visibility = View.GONE
            } else {
                binding.todayInvestigationCard.visibility = View.VISIBLE
                binding.divider.visibility = View.VISIBLE
            }

            if (viewModel.labTestAddLists.size > 0) {
                addLabTestCreateAdapter = LabtestHistoryAdapter(false)
                addLabTestCreateAdapter.setanInterfaceListener(this)
                addLabTestCreateAdapter.submitData(viewModel.labTestAddLists)
                binding.addInvestigationCard.rvTestList.adapter = addLabTestCreateAdapter
            }
        }
    }
}
