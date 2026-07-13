package org.medtroniclabs.uhis.ui.patient.fragment

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DateUtils.DATE_FORMAT_ddMMMyyyy
import org.medtroniclabs.uhis.common.DateUtils.convertToIsoFormat
import org.medtroniclabs.uhis.data.medicalreview.ReqBPBGLogList
import org.medtroniclabs.uhis.data.registration.BloodGlucose
import org.medtroniclabs.uhis.data.registration.GlucoseLog
import org.medtroniclabs.uhis.databinding.AddBgReadingBinding
import org.medtroniclabs.uhis.databinding.FragmentBloodGlucoseBinding
import org.medtroniclabs.uhis.db.entity.SymptomEntity
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.model.FormLayout
import org.medtroniclabs.uhis.formgeneration.ui.SingleSelectionCustomView
import org.medtroniclabs.uhis.formgeneration.utility.CustomSpinnerAdapter
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.patient.fragment.BloodGlucoseFragment.Companion.GLUCOSE_MAX_VALUE
import org.medtroniclabs.uhis.ui.patient.fragment.BloodGlucoseFragment.Companion.GLUCOSE_MIN_VALUE
import org.medtroniclabs.uhis.ui.patient.fragment.BloodGlucoseFragment.Companion.HB1AC_MAX_VALUE
import org.medtroniclabs.uhis.ui.patient.fragment.BloodGlucoseFragment.Companion.HB1AC_MIN_VALUE
import org.medtroniclabs.uhis.ui.patient.fragment.BloodGlucoseFragment.Companion.OGTT_MAX_VALUE
import org.medtroniclabs.uhis.ui.patient.fragment.BloodGlucoseFragment.Companion.OGTT_MIN_VALUE
import org.medtroniclabs.uhis.ui.patient.util.ViewUtil
import org.medtroniclabs.uhis.ui.patient.viewmodel.MedicalReviewBaseViewModel
import org.medtroniclabs.uhis.ui.patient.viewmodel.NurseMedicalReviewViewModel
import org.medtroniclabs.uhis.ui.patient.viewmodel.PatientDetailViewModel
import timber.log.Timber
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class BloodGlucoseFragment : BaseFragment(), View.OnClickListener {
    private val viewModel: MedicalReviewBaseViewModel by activityViewModels()
    private lateinit var binding: FragmentBloodGlucoseBinding
    private var datePickerDialog: DatePickerDialog? = null
    private val patientDetailViewModel: PatientDetailViewModel by activityViewModels()
    private val nurseViewModel: NurseMedicalReviewViewModel by activityViewModels()

    companion object {
        const val TAG = "BloodGlucoseFragment"

        fun newInstance(): BloodGlucoseFragment = BloodGlucoseFragment()

        private const val OGTT_MIN_VALUE = 0.6
        private const val OGTT_MAX_VALUE = 33.0
        private const val HB1AC_MIN_VALUE = 1.0
        private const val HB1AC_MAX_VALUE = 50.0

        private const val GLUCOSE_MIN_VALUE = 0.6
        private const val GLUCOSE_MAX_VALUE = 33.0
        private const val VISIBLE_ITEM_COUNT = 5
    }

    private var bgReadings: List<Triple<String, String?, Boolean>> = listOf()
    private var isExpanded = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentBloodGlucoseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        nurseViewModel.getSymptomListByType(DefinedParams.GLUCOSE_TYPE)
        attachObserver()
    }

    private fun initView() {
        binding.etInvestigationDate.safeClickListener(this)
        binding.btnAddNewReading.safeClickListener(this)

        binding.tvViewMore.safeClickListener {
            if (!isExpanded) {
                renderItems(bgReadings)
                toggleView(true)
            }
        }
        binding.tvViewLess.safeClickListener {
            if (isExpanded) {
                renderItems(bgReadings.take(VISIBLE_ITEM_COUNT))
                toggleView(false)
            }
        }

        nurseViewModel.glucoseLog =
            nurseViewModel.glucoseLog
                ?: GlucoseLog()
        nurseViewModel.nurseMrRequestModel.glucoseLog =
            (nurseViewModel.nurseMrRequestModel.glucoseLog ?: arrayListOf())
        addCustomView(
            getData(),
            DefinedParams.INVESTIGATION_TYPE,
            nurseViewModel.glucoseType,
            selectionCallBack,
            binding.etInvestigationSelect,
        )

        binding.etReadingValue.addTextChangedListener { validateBloodGlucoseValueOnTextChange() }
        binding.etHbA1cReadingValue.addTextChangedListener { validateHbA1cOnTextChange() }
        binding.etOgttReadingValue.addTextChangedListener { validateOgttOnTextChange() }
    }

    private fun attachObserver() {
        nurseViewModel.patientDetailsResponse.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.SUCCESS -> {
                    resourceState.data?.memberId?.let { id ->
                        val request = ReqBPBGLogList(memberId = id)
                        patientDetailViewModel.getPatientBloodGlucoseList(
                            requireContext(),
                            request,
                        )
                    }
                }

                else -> {
                    // Invoked if response state is not success
                }
            }
        }

        patientDetailViewModel.patientBloodGlucoseListResponse.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    resourceState.data?.glucoseLogList?.takeIf { it.isNotEmpty() }?.let {
                        binding.clBGReadingHistory.visibility = View.VISIBLE
                        renderBGResponse(it)
                    } ?: run {
                        binding.clBGReadingHistory.visibility = View.GONE
                    }
                }
            }
        }
        nurseViewModel.symptomTypeListResponse.observe(viewLifecycleOwner) { list ->
            setInvestigationSpinner(list)
        }
    }

    private fun renderBGResponse(bloodGlucoses: ArrayList<BloodGlucose>) {
        if (bloodGlucoses.isNotEmpty()) {
            binding.clBGReadingHistory.visibility = View.VISIBLE
            binding.gViewMoreLess.visibility = View.VISIBLE
            this.bgReadings = processBloodGlucoseResponse(bloodGlucoses)
            renderItems(bgReadings.take(VISIBLE_ITEM_COUNT))
        } else {
            binding.clBGReadingHistory.visibility = View.GONE
        }
    }

    private fun processBloodGlucoseResponse(glucoseResponses: List<BloodGlucose>): List<Triple<String, String?, Boolean>> =
        glucoseResponses
            .sortedByDescending {
                DateUtils.getLastMenstrualDate(it.glucoseDateTime ?: "").timeInMillis
            }.map { response ->
                val formattedDate = response.glucoseDateTime
                val isHighGlucose = if (response.glucoseType == DefinedParams.FBS_KEY) {
                    (
                        response.glucoseValue?.toFloat()
                            ?: 0f
                    ) > 7.0f ||
                        (
                            response.hba1c?.toFloat()
                                ?: 0f
                        ) > 10.0f ||
                        (response.ogtt?.toFloat() ?: 0f) > 11f
                } else {
                    (
                        response.glucoseValue?.toFloat()
                            ?: 0f
                    ) > 11f ||
                        (
                            response.hba1c?.toFloat()
                                ?: 0f
                        ) > 10.0f ||
                        (response.ogtt?.toFloat() ?: 0f) > 11f
                }
                val result = StringBuilder()

                response.glucoseType?.let { glucoseType ->
                    response.glucoseValue?.let { glucoseValue ->
                        result.append("${glucoseType.uppercase(Locale.ROOT)} - $glucoseValue ${response.glucoseUnit ?: ""}")
                    }
                }

                response.hba1c?.let { hba1c ->
                    if (result.isNotEmpty()) result.append(", ") // Add separator if there are previous values
                    result.append("${getString(R.string.glucose_hba1c)} - $hba1c ${response.hba1cUnit ?: ""}")
                }

                response.ogtt?.let { ogtt ->
                    if (result.isNotEmpty()) result.append(", ") // Add separator if there are previous values
                    result.append("${getString(R.string.ogtt)} - $ogtt ${response.ogttUnit ?: ""}")
                }
                val glucoseInfo = result.toString()
                Triple(glucoseInfo, formattedDate, isHighGlucose)
            }

    private fun renderItems(items: List<Triple<String, String?, Boolean>>) {
        binding.llBgReading.removeAllViews()
        for (item in items) {
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(10, 10, 10, 10)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                )
                setVerticalGravity(Gravity.CENTER_VERTICAL)
            }

            val imageView = ImageView(requireContext()).apply {
                val imageRes =
                    if (item.third) R.drawable.ic_bg_reading_orange else R.drawable.ic_bg_reading_green
                setImageResource(imageRes)
                layoutParams = LinearLayout.LayoutParams(40, 40).apply {
                    marginEnd = 20
                }
            }
            if (item.first != "") {
                row.addView(imageView)

                val textViewBG = TextView(requireContext()).apply {
                    text = item.first
                    textSize = 16f
                    setTextColor(requireContext().getColor(R.color.secondary_black))
                    layoutParams =
                        LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1.5f,
                        )
                    setPadding(10, 10, 10, 10)
                }
                row.addView(textViewBG)
            }

            val separatorTextView = TextView(requireContext()).apply {
                text = " - "
                textSize = 16f
                setTextColor(requireContext().getColor(R.color.secondary_black))
                layoutParams =
                    LinearLayout.LayoutParams(50, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        marginEnd = 20
                    }
                setPadding(10, 10, 10, 10)
            }
            if (item.first != "" && item.second != null) {
                row.addView(separatorTextView)
            }

            if (item.second != null) {
                val textViewDate = TextView(requireContext()).apply {
                    val visitDateMillis = DateUtils.getLastMenstrualDate(item.second ?: "").timeInMillis
                    val displayDate = DateUtils.formatDateToDisplayFormat(visitDateMillis, DATE_FORMAT_ddMMMyyyy) ?: ""
                    text = displayDate
                    textSize = 16f
                    setTextColor(requireContext().getColor(R.color.secondary_black))
                    layoutParams =
                        LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.5f)
                    setPadding(10, 10, 10, 10)
                }
                row.addView(textViewDate)
            }
            binding.llBgReading.addView(row)
        }
    }

    private fun toggleView(expand: Boolean) {
        isExpanded = expand
        binding.tvViewMore.visibility = if (expand) View.GONE else View.VISIBLE
        binding.ivDownArrow.visibility = if (expand) View.GONE else View.VISIBLE
        binding.tvViewLess.visibility = if (expand) View.VISIBLE else View.GONE
        binding.ivUpArrow.visibility = if (expand) View.VISIBLE else View.GONE
    }

    private fun setInvestigationSpinner(list: List<SymptomEntity>) {
        val adapter = CustomSpinnerAdapter(requireContext())
        val dropDownList = ArrayList<Map<String, Any>>()
        dropDownList.add(
            hashMapOf(
                DefinedParams.NAME to DefinedParams.DEFAULT_ID_LABEL,
                DefinedParams.ID to DefinedParams.DEFAULT_ID,
            ),
        )

        list.sortedBy { it.displayOrder }.forEach {
            dropDownList.add(
                hashMapOf(
                    DefinedParams.NAME to it.symptom,
                    DefinedParams.ID to it.symptom,
                ),
            )
        }
        adapter.setData(dropDownList)
    }

    private fun showDatePickerDialog() {
        var yearMonthDate: Triple<Int?, Int?, Int?>? = null
        if (!binding.etInvestigationDate.text.isNullOrBlank()) {
            yearMonthDate =
                DateUtils.convertddMMMToddMM(binding.etInvestigationDate.text.toString())
        }

        if (datePickerDialog == null) {
            datePickerDialog = ViewUtil.showDatePicker(
                context = requireContext(),
                minDate = Calendar.getInstance().apply { add(Calendar.MONTH, -6) }.timeInMillis,
                maxDate = System.currentTimeMillis(),
                date = yearMonthDate,
                cancelCallBack = { datePickerDialog = null },
            ) { _, year, month, dayOfMonth ->
                val stringDate = "$dayOfMonth-$month-$year"
                val todayDate = DateUtils.getCurrentDate()

                val calendar = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                if (todayDate != calendar) {
                    binding.etInvestigationDate.text =
                        DateUtils.convertDateTimeToDate(
                            stringDate,
                            DateUtils.DATE_FORMAT_ddMMyyyy,
                            DateUtils.DATE_DD_MMM_YYYY,
                        )
                    viewModel.nextMedicalReviewDate = binding.etInvestigationDate.text.toString()
                    datePickerDialog = null
                }
            }
        }
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.etInvestigationDate -> {
                showDatePickerDialog()
            }

            R.id.btnAddNewReading -> {
                if (validation()) {
                    loadAddListEdit()
                }
            }
        }
    }

    /**
     * Validates the glucose reading field on text change.
     * Required when an investigation type (FBS/RBS) is selected; otherwise optional.
     * When present, the value must be within [GLUCOSE_MIN_VALUE]..[GLUCOSE_MAX_VALUE].
     */
    private fun validateBloodGlucoseValueOnTextChange() {
        val text = binding.etReadingValue.text
            .toString()
            .trim()
        val isGlucoseTypeSelected = !nurseViewModel.glucoseLog?.glucoseType.isNullOrEmpty()
        if (text.isEmpty()) {
            if (isGlucoseTypeSelected) {
                binding.tvReadingValueErrorMessage.visibility = View.VISIBLE
                binding.tvReadingValueErrorMessage.text = getString(R.string.default_user_input_error)
            } else {
                binding.tvReadingValueErrorMessage.visibility = View.GONE
            }
            return
        }
        val value = text.toDoubleOrNull()
        if (value == null || value !in GLUCOSE_MIN_VALUE..GLUCOSE_MAX_VALUE) {
            binding.tvReadingValueErrorMessage.visibility = View.VISIBLE
            binding.tvReadingValueErrorMessage.text =
                getString(
                    R.string.general_min_max_validation,
                    GLUCOSE_MIN_VALUE.toString(),
                    GLUCOSE_MAX_VALUE.toString(),
                )
        } else {
            binding.tvReadingValueErrorMessage.visibility = View.GONE
        }
    }

    /**
     * Validates the HbA1c field on text change.
     * Empty values are ignored; when present, must be within [HB1AC_MIN_VALUE]..[HB1AC_MAX_VALUE].
     */
    private fun validateHbA1cOnTextChange() {
        val text = binding.etHbA1cReadingValue.text
            .toString()
            .trim()
        if (text.isEmpty()) {
            binding.tvHBA1cReadingValueErrorMessage.visibility = View.GONE
            return
        }
        val value = text.toDoubleOrNull()
        if (value == null || value !in HB1AC_MIN_VALUE..HB1AC_MAX_VALUE) {
            binding.tvHBA1cReadingValueErrorMessage.visibility = View.VISIBLE
            binding.tvHBA1cReadingValueErrorMessage.text =
                getString(
                    R.string.general_min_max_validation,
                    HB1AC_MIN_VALUE.toString(),
                    HB1AC_MAX_VALUE.toString(),
                )
        } else {
            binding.tvHBA1cReadingValueErrorMessage.visibility = View.GONE
        }
    }

    /**
     * Validates the OGTT field on text change.
     * Empty values are ignored; when present, must be within [OGTT_MIN_VALUE]..[OGTT_MAX_VALUE].
     */
    private fun validateOgttOnTextChange() {
        val text = binding.etOgttReadingValue.text
            .toString()
            .trim()
        if (text.isEmpty()) {
            binding.tvOgttReadingValueErrorMessage.visibility = View.GONE
            return
        }
        val value = text.toDoubleOrNull()
        if (value == null || value !in OGTT_MIN_VALUE..OGTT_MAX_VALUE) {
            binding.tvOgttReadingValueErrorMessage.visibility = View.VISIBLE
            binding.tvOgttReadingValueErrorMessage.text =
                getString(
                    R.string.general_min_max_validation,
                    OGTT_MIN_VALUE.toString(),
                    OGTT_MAX_VALUE.toString(),
                )
        } else {
            binding.tvOgttReadingValueErrorMessage.visibility = View.GONE
        }
    }

    /**
     * Validates all glucose form inputs before adding a reading or submitting.
     * Checks investigation type, date, and numeric ranges for glucose, HbA1c, and OGTT.
     * Populates [nurseViewModel.glucoseLog] and calls [loadRequestData] when valid.
     *
     * @param isSubmit when true, defers [loadRequestData] until the first successful submit
     * @return true if all applicable validation rules pass
     */
    fun validation(isSubmit: Boolean = false): Boolean {
        var isValid = true
        val readingValue = binding.etReadingValue.text
            .toString()
            .trim()
            .toDoubleOrNull()
        val hba1cValue = binding.etHbA1cReadingValue.text
            .toString()
            .trim()
            .toDoubleOrNull()
        val ogttValue = binding.etOgttReadingValue.text
            .toString()
            .trim()
            .toDoubleOrNull()

        nurseViewModel.glucoseLog?.apply {
            refId = UUID.randomUUID().toString()
            glucoseDate =
                binding.etInvestigationDate.text
                    .toString()
                    .takeIf { it.isNotEmpty() }
                    ?.let { convertToIsoFormat(it) }
            glucoseValue = readingValue
            hba1c = hba1cValue
            ogtt = ogttValue
        }

        nurseViewModel.glucoseLog?.apply {
            if (!glucoseType.isNullOrEmpty()) {
                glucoseValue?.let {
                    if (it !in GLUCOSE_MIN_VALUE..GLUCOSE_MAX_VALUE) {
                        isValid = false
                        binding.tvReadingValueErrorMessage.visibility = View.VISIBLE
                        binding.tvReadingValueErrorMessage.text =
                            getString(R.string.general_min_max_validation, GLUCOSE_MIN_VALUE.toString(), GLUCOSE_MAX_VALUE.toString())
                    } else {
                        binding.tvReadingValueErrorMessage.visibility = View.GONE
                    }
                } ?: run {
                    isValid = false
                    binding.tvReadingValueErrorMessage.visibility = View.VISIBLE
                    binding.tvReadingValueErrorMessage.text =
                        getString(R.string.default_user_input_error)
                }

                if (glucoseDate == null) {
                    isValid = false
                    binding.tvInvestigationDateErrorMessage.visibility = View.VISIBLE
                    binding.tvInvestigationDateErrorMessage.text =
                        getString(R.string.date_validation)
                } else {
                    binding.tvInvestigationDateErrorMessage.visibility = View.GONE
                }
            }

            // Glucose Value Validation
            if (glucoseValue != null) {
                if (glucoseType.isNullOrEmpty()) {
                    isValid = false
                    binding.tvSelectErrorMessage.visibility = View.VISIBLE
                    binding.tvSelectErrorMessage.text = getString(R.string.error_message_spinner)
                } else {
                    binding.tvSelectErrorMessage.visibility = View.GONE
                }

                if (glucoseDate == null) {
                    isValid = false
                    binding.tvInvestigationDateErrorMessage.visibility = View.VISIBLE
                    binding.tvInvestigationDateErrorMessage.text =
                        getString(R.string.date_validation)
                } else {
                    binding.tvInvestigationDateErrorMessage.visibility = View.GONE
                }
            }

            // HBA1c Validation
            hba1c?.let {
                if (it !in HB1AC_MIN_VALUE..HB1AC_MAX_VALUE) {
                    isValid = false
                    binding.tvHBA1cReadingValueErrorMessage.visibility = View.VISIBLE
                    binding.tvHBA1cReadingValueErrorMessage.text =
                        getString(R.string.general_min_max_validation, HB1AC_MIN_VALUE.toString(), HB1AC_MAX_VALUE.toString())
                } else {
                    binding.tvHBA1cReadingValueErrorMessage.visibility = View.GONE
                }

                if (glucoseDate == null) {
                    isValid = false
                    binding.tvInvestigationDateErrorMessage.visibility = View.VISIBLE
                    binding.tvInvestigationDateErrorMessage.text =
                        getString(R.string.date_validation)
                } else {
                    binding.tvInvestigationDateErrorMessage.visibility = View.GONE
                }
            }

            // OGTT Validation
            ogtt?.let {
                if (it !in OGTT_MIN_VALUE..OGTT_MAX_VALUE) {
                    isValid = false
                    binding.tvOgttReadingValueErrorMessage.visibility = View.VISIBLE
                    binding.tvOgttReadingValueErrorMessage.text =
                        getString(R.string.general_min_max_validation, OGTT_MIN_VALUE.toString(), OGTT_MAX_VALUE.toString())
                } else {
                    binding.tvOgttReadingValueErrorMessage.visibility = View.GONE
                }

                if (glucoseDate == null) {
                    isValid = false
                    binding.tvInvestigationDateErrorMessage.visibility = View.VISIBLE
                    binding.tvInvestigationDateErrorMessage.text =
                        getString(R.string.date_validation)
                } else {
                    binding.tvInvestigationDateErrorMessage.visibility = View.GONE
                }
            }
        }

        if (isValid && (!isSubmit || nurseViewModel.executionCount++ == 0)) {
            loadRequestData()
        }
        return isValid
    }

    private fun loadRequestData() {
        nurseViewModel.glucoseLog?.let {
            if (nurseViewModel.glucoseLogList == null) {
                nurseViewModel.glucoseLogList = arrayListOf()
            }
            // Create a new object before adding
            val newEntry = it.copy() // Assuming GlucoseLog is a data class
            nurseViewModel.glucoseLogList?.add(newEntry)
        }
        nurseViewModel.glucoseLogList = nurseViewModel.glucoseLogList
            ?.filter { log ->
                log.glucoseType != null ||
                    log.hba1c != null ||
                    log.ogtt != null ||
                    log.glucoseValue != null ||
                    log.glucoseDate != null
            }?.toCollection(ArrayList()) // Ensure it's still an ArrayList

        nurseViewModel.nurseMrRequestModel.glucoseLog = ArrayList(nurseViewModel.glucoseLogList)
        // Remove duplicate refId entries from glucoseLogList
        nurseViewModel.nurseMrRequestModel.glucoseLog = nurseViewModel.nurseMrRequestModel.glucoseLog?.apply {
            distinctBy { it.refId }
            toMutableList()
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
                    translate = true,
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

    private fun getData(): ArrayList<Map<String, Any>> {
        val flowList = ArrayList<Map<String, Any>>()
        flowList.add(getOptionMap(DefinedParams.FBS, getString(R.string.glucose_fbs)))
        flowList.add(getOptionMap(DefinedParams.RBS, getString(R.string.glucose_rbs)))
        return flowList
    }

    private fun getOptionMap(
        value: String,
        name: String,
    ): Map<String, Any> {
        val map = HashMap<String, Any>()
        map[DefinedParams.ID] = value
        map[DefinedParams.NAME] = name
        return map
    }

    private val selectionCallBack: (selectedID: Any?, elementId: String, serverViewModel: FormLayout, name: String?) -> Unit =
        { selectedID, _, _, _ ->
            val currentSelection = nurseViewModel.glucoseType[DefinedParams.INVESTIGATION_TYPE]
            if (currentSelection == selectedID) {
                nurseViewModel.glucoseType.remove(DefinedParams.INVESTIGATION_TYPE)
                nurseViewModel.glucoseLog?.glucoseType = null
            } else {
                nurseViewModel.glucoseType[DefinedParams.INVESTIGATION_TYPE] = selectedID as String
                nurseViewModel.glucoseLog?.glucoseType = selectedID
            }
            validateBloodGlucoseValueOnTextChange()
        }

    private fun loadAddListEdit() {
        val medicationEditBinding = AddBgReadingBinding.inflate(layoutInflater)
        if (!nurseViewModel.glucoseLog?.glucoseDate.isNullOrEmpty()) {
            binding.addReadingTitle.visibility = View.VISIBLE
            nurseViewModel.glucoseLog?.let { value ->
                val formattedString = CommonUtils.formatGlucoseData(
                    value.glucoseType,
                    value.glucoseValue,
                    value.hba1c,
                    value.ogtt,
                    context,
                )
                if (formattedString.isNotEmpty()) {
                    medicationEditBinding.etFbsValue.text = formattedString
                    medicationEditBinding.ivDate.text = binding.etInvestigationDate.text
                    binding.llAddNewReadingList.addView(medicationEditBinding.root)
                }
            }

            val existingView =
                binding.etInvestigationSelect.findViewWithTag<View>(DefinedParams.INVESTIGATION_TYPE)
            existingView?.let {
                binding.etInvestigationSelect.removeView(it)
                nurseViewModel.glucoseType.clear()
            }

            binding.etReadingValue.text?.clear()
            binding.etOgttReadingValue.text?.clear()
            binding.etHbA1cReadingValue.text?.clear()
            binding.etInvestigationDate.text = ""
            nurseViewModel.glucoseType = hashMapOf()
            nurseViewModel.glucoseLog?.glucoseType = null

            addCustomView(
                getData(),
                DefinedParams.INVESTIGATION_TYPE,
                nurseViewModel.glucoseType,
                selectionCallBack,
                binding.etInvestigationSelect,
            )
        }
        medicationEditBinding.ivRemoveMedication.setOnClickListener {
            val parentView = it.parent as? View ?: return@setOnClickListener

            binding.llAddNewReadingList.removeView(parentView)

            // Find and remove the correct object manually
            val iterator = nurseViewModel.glucoseLogList?.iterator()
            while (iterator?.hasNext() == true) {
                val log = iterator.next()
                if (log.glucoseDate == nurseViewModel.glucoseLog?.glucoseDate) {
                    iterator.remove()
                    break // Exit after removing the first matching item
                }
            }
            if (nurseViewModel.glucoseLogList.isNullOrEmpty()) {
                binding.addReadingTitle.visibility = View.GONE
            }
            parentView.post {
                nurseViewModel.glucoseLog = GlucoseLog()
                Timber.d("Selected values ${nurseViewModel.glucoseLog}")
            }
        }
    }
}
