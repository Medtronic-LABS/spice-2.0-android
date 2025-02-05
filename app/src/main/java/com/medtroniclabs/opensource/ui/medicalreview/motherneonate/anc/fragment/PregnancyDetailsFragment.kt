package com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.fragment

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.activityViewModels
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.CommonUtils.toDoubleOrEmptyString
import com.medtroniclabs.opensource.common.CommonUtils.toIntOrEmptyString
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.DateUtils.DATE_FORMAT_ddMMyyyy
import com.medtroniclabs.opensource.common.DateUtils.DATE_FORMAT_yyyyMMdd
import com.medtroniclabs.opensource.common.DateUtils.DATE_ddMMyyyy
import com.medtroniclabs.opensource.common.DateUtils.calculateGestationalAge
import com.medtroniclabs.opensource.common.DateUtils.formatGestationalAge
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.DefinedParams.LMB
import com.medtroniclabs.opensource.common.ViewUtils
import com.medtroniclabs.opensource.databinding.FragmentPregnancyDetailsBinding
import com.medtroniclabs.opensource.formgeneration.extension.markMandatory
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.formgeneration.utility.CustomSpinnerAdapter
import com.medtroniclabs.opensource.ui.BaseFragment
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.MotherNeonateUtil.EstimatedDeliveryDate
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewTypeEnums
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.MotherNeonateUtil.initTextWatcherForDouble
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.MotherNeonateUtil.initTextWatcherForInt
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.MotherNeonateUtil.isBasicValid
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.MotherNeonateUtil.isValidInput
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.MotherNeonateUtil.isValidMeasurement
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.PregnancyDetailsViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PregnancyDetailsFragment : BaseFragment(), View.OnClickListener {

    private lateinit var binding: FragmentPregnancyDetailsBinding
    private var datePickerDialog: DatePickerDialog? = null
    private val pregnancyDetailsViewModel: PregnancyDetailsViewModel by activityViewModels()
    var adapter: CustomSpinnerAdapter? = null

    companion object {
        const val TAG = "PregnancyDetailsFragment"
        fun newInstance(lmb: String?): PregnancyDetailsFragment {
            val fragment = PregnancyDetailsFragment()
            val bundle = Bundle()
            bundle.putString(LMB, lmb)
            fragment.arguments = bundle
            return fragment
        }
    }

    private fun isHeightValid(): Boolean {
        return isValidInput(
            binding.etHeight.text.toString(),
            binding.etHeight,
            binding.tvHeightError,
            50.0..300.0,
            R.string.height_error,
            true,
            requireContext()
        )
    }

    private fun isWeightValid(): Boolean {
        return isValidInput(
            binding.etWeight.text.toString(),
            binding.etWeight,
            binding.tvKgError,
            10.0..400.0,
            R.string.weight_error,
            true,
            requireContext()
        )
    }

    fun validateInput(): Boolean {
        val isWeightValid = isWeightValid()
        val isHeightValid = isHeightValid()
        val isSystolicValid = isValidMeasurement(
            binding.etSystolic.text.toString(),
            binding.tvSystolicError,
            30,
            300,
            binding.etDiastolic,
            minErrorMessage = getString(R.string.systolic_error_min),
            maxErrorMessage = getString(R.string.systolic_error_max),
            requireContext()
        )
        val isDiastolicValid = isValidMeasurement(
            binding.etDiastolic.text.toString(),
            binding.tvDiastolicError,
            30,
            300,
            binding.etDiastolic,
            getString(R.string.diastolic_error_min),
            maxErrorMessage = getString(R.string.diastolic_error_max),
            requireContext()
        )
        val isPulseValid = isValidMeasurement(
            binding.etPulse.text.toString(),
            binding.tvPulseError,
            35,
            300,
            minErrorMessage = getString(R.string.pulse_error_min),
            maxErrorMessage = getString(R.string.pulse_error_max),
            context = requireContext()
        )
        val isNoFoFetusValid = isBasicValid(
            binding.etNoOfFetus.text.toString(),
            binding.tvNoOfFetusError,
            0,
            getString(R.string.error_label),
            context = requireContext()
        )
        val isGravidaValid = isBasicValid(
            binding.etGravida.text.toString(),
            binding.tvGravidaError,
            0,
            getString(R.string.error_label),
            context = requireContext()
        )
        val isParityFirstValid = isBasicValid(
            binding.etParityFirst.text.toString(),
            binding.tvParityFirstError,
            0,
            getString(R.string.error_label),
            context = requireContext(),
            isParity = true
        )
        val isParitySecondValid = isBasicValid(
            binding.etParitySecond.text.toString(),
            binding.tvParitySecondError,
            0,
            getString(R.string.error_label),
            context = requireContext(),
            isParity = true
        )
        val isLmbValid = binding.tvLastMenstrualPeriodDate.text?.toString()?.trim()?.isBlank() == true
        binding.tvLastMenstrualPeriodError.apply { if (isLmbValid) visible() else gone() }
        findFirstInvalidField(
            isSystolicValid,
            isDiastolicValid,
            isPulseValid,
            isNoFoFetusValid,
            isGravidaValid,
            isParityFirstValid,
            isParitySecondValid,
            isLmbValid
        )
        // Return true only if all validations pass
        return isWeightValid && isHeightValid && isSystolicValid && isDiastolicValid && isPulseValid && isNoFoFetusValid && isGravidaValid && isParityFirstValid && isParitySecondValid && !isLmbValid
    }

    private fun findFirstInvalidField(
        isSystolicValid: Boolean,
        isDiastolicValid: Boolean,
        isPulseValid: Boolean,
        isNoFoFetusValid: Boolean,
        isGravidaValid: Boolean,
        isParityFirstValid: Boolean,
        isParitySecondValid: Boolean,
        isLmbValid: Boolean
    ) {
        // Find and return the first invalid field
        val view = if (!isWeightValid()) {
            binding.etWeight
        } else if (!isHeightValid()) {
            binding.etHeight
        } else if (!isSystolicValid) {
            binding.etSystolic
        } else if (!isDiastolicValid) {
            binding.etDiastolic
        } else if (!isPulseValid) {
            binding.etPulse
        } else if (!isNoFoFetusValid) {
            binding.etNoOfFetus
        } else if (!isGravidaValid) {
            binding.etGravida
        } else if (!isParityFirstValid) {
            binding.etParityFirst
        } else if (!isParitySecondValid) {
            binding.etParitySecond
        }  else if (isLmbValid) {
            binding.tvLastMenstrualPeriodDate
        } else {
            null
        }
        view?.requestFocus()
    }

    fun getParity(): Int? {
        val firstValue = binding.etParityFirst.text.toString().toIntOrNull()
        val secondValue = binding.etParitySecond.text.toString().toIntOrNull()

        return when {
            firstValue != null && secondValue != null -> firstValue + secondValue
            firstValue != null -> firstValue
            secondValue != null -> secondValue
            else -> null
        }
    }

    private fun updateBMI() {
        val height = binding.etHeight.text.toString().toDoubleOrNull()
        val weight = binding.etWeight.text.toString().toDoubleOrNull()
        val bmiValue = if (height != null && weight != null) {
            val value = CommonUtils.getBMI(height, weight, requireContext())
            pregnancyDetailsViewModel.pregnancyDetailsModel.bmi = value.toDoubleOrNull()
            value
        } else {
            pregnancyDetailsViewModel.pregnancyDetailsModel.bmi = null
            getString(R.string.hyphen_symbol)
        }
        binding.tvBMIText.text = bmiValue
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPregnancyDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        attachObservers()
    }

    private fun attachObservers() {
        pregnancyDetailsViewModel.ancMetaLiveDataForBloodGroup.observe(viewLifecycleOwner) {
            val complaintList = ArrayList<Map<String, Any>>()
            for (i in it) {
                complaintList.add(
                    CommonUtils.getOptionMap(i.name, i.name)
                )
            }
            setSpinner(complaintList)
            populateFromModel()
        }
    }

    private fun initView() {
        pregnancyDetailsViewModel.setAncReqToGetMetaForBloodGroup(MedicalReviewTypeEnums.BloodGroup.name)
        adapter = CustomSpinnerAdapter(requireContext())
        with(binding) {
            tvLastMenstrualPeriodLabel.markMandatory()
            tvHeight.markMandatory()
            tvWeight.markMandatory()
            initTextWatcherForDouble(etHeight) {
                updateBMI()
                pregnancyDetailsViewModel.pregnancyDetailsModel.height = it
                pregnancyDetailsViewModel.checkSubmitBtn()
            }
            initTextWatcherForDouble(etWeight) {
                updateBMI()
                pregnancyDetailsViewModel.pregnancyDetailsModel.weight = it
                pregnancyDetailsViewModel.checkSubmitBtn()
            }
            initTextWatcherForDouble(etSystolic) {
                pregnancyDetailsViewModel.pregnancyDetailsModel.systolic = it
                pregnancyDetailsViewModel.checkSubmitBtn()
            }
            initTextWatcherForDouble(etDiastolic) {
                pregnancyDetailsViewModel.pregnancyDetailsModel.diastolic = it
                pregnancyDetailsViewModel.checkSubmitBtn()
            }
            initTextWatcherForDouble(etPulse) {
                pregnancyDetailsViewModel.pregnancyDetailsModel.pulse = it
                pregnancyDetailsViewModel.checkSubmitBtn()
            }
            initTextWatcherForInt(etNoOfFetus) {
                pregnancyDetailsViewModel.pregnancyDetailsModel.noOfFetus = it
                pregnancyDetailsViewModel.checkSubmitBtn()
            }
            initTextWatcherForInt(etParityFirst) {
                pregnancyDetailsViewModel.pregnancyDetailsModel.parity = it
                pregnancyDetailsViewModel.checkSubmitBtn()
            }
            initTextWatcherForInt(etGravida) {
                pregnancyDetailsViewModel.pregnancyDetailsModel.gravida = it
                pregnancyDetailsViewModel.checkSubmitBtn()
            }
            tvLastMenstrualPeriodDate.safeClickListener(this@PregnancyDetailsFragment)
            tvEstimatedDeliveryDate.safeClickListener(this@PregnancyDetailsFragment)
        }
    }

    private fun populateFromModel() {
        pregnancyDetailsViewModel.pregnancyDetailsModel.let { model ->
            with(binding) {
                etHeight.setText(model.height?.toDoubleOrEmptyString())
                etWeight.setText(model.weight?.toDoubleOrEmptyString())
                etSystolic.setText(model.systolic?.toIntOrEmptyString())
                etDiastolic.setText(model.diastolic?.toIntOrEmptyString())
                etPulse.setText(model.pulse?.toIntOrEmptyString())
                val lmb = arguments?.getString(LMB)
                if (!lmb.isNullOrBlank()) {
                    binding.tvLastMenstrualPeriodDate.text =
                        DateUtils.convertDateFormat(
                            lmb,
                            DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ, DATE_ddMMyyyy
                        )
                    calculateGestationalAgeAndEstimationDeliveryDate()
                    binding.tvLastMenstrualPeriodDate.isEnabled = false
                } else {
                    tvLastMenstrualPeriodDate.text = model.lastMenstrualPeriod?.let {
                        DateUtils.convertDateFormat(it, DATE_FORMAT_yyyyMMdd, DATE_ddMMyyyy)
                    }
                }
                tvEstimatedDeliveryDate.text = model.estimatedDeliveryDate?.let {
                    DateUtils.convertDateFormat(it, DATE_FORMAT_yyyyMMdd, DATE_ddMMyyyy)
                }
                etGestationalAge.text = model.gestationalAge?.let {
                    formatGestationalAge(it, requireContext())
                } ?: ""
                etNoOfFetus.setText(model.noOfFetus?.toString())
                etGravida.setText(model.gravida?.toString())
                etParityFirst.setText(model.parity?.toString())
                setSpinnerSelectedItem(model.patientBloodGroup)
            }
        }
    }

    private fun setSpinner(complaintList: ArrayList<Map<String, Any>>) {
        val dropDownList = ArrayList<Map<String, Any>>()
        dropDownList.add(
            hashMapOf<String, Any>(
                DefinedParams.NAME to DefinedParams.DefaultIDLabel,
                DefinedParams.id to DefinedParams.DefaultID
            )
        )
        dropDownList.addAll(complaintList)
        adapter?.setData(dropDownList)
        binding.etPatientBloodGroup.adapter = adapter
        binding.etPatientBloodGroup.setSelection(0, false)
        binding.etPatientBloodGroup.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    view: View?,
                    pos: Int,
                    itemId: Long
                ) {
                    val selectedItem = adapter?.getData(position = pos)
                    selectedItem?.let {
                        val selectedId = it[DefinedParams.id] as String?
                        val selectedBloodGroup = it[DefinedParams.NAME] as String?
                        if (selectedId != DefinedParams.DefaultID) {
                            pregnancyDetailsViewModel.pregnancyDetailsModel.patientBloodGroup =
                                selectedBloodGroup
                        } else {
                            pregnancyDetailsViewModel.pregnancyDetailsModel.patientBloodGroup = null
                        }
                        pregnancyDetailsViewModel.checkSubmitBtn()
                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    /**
                     * this method is not used
                     */
                }
            }
    }

    private fun setSpinnerSelectedItem(selectedBloodGroup: String?) {
        selectedBloodGroup?.let { bloodGroup ->
            adapter?.let {
                val index = adapter?.getIndexOfItemByName(bloodGroup)
                if (index != -1) {
                    if (index != null) {
                        binding.etPatientBloodGroup.setSelection(index)
                    }
                }
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.tvLastMenstrualPeriodDate.id -> {
                showDatePickerDialog(binding.tvLastMenstrualPeriodDate,
                    disableFuture = true,
                    isLmp = true
                )
            }

            binding.tvEstimatedDeliveryDate.id -> {
                showDatePickerDialog(binding.tvEstimatedDeliveryDate)
            }
        }
    }

    private fun showDatePickerDialog(
        textView: AppCompatTextView,
        disableFuture: Boolean = false,
        isLmp: Boolean = false
    ) {
        var yearMonthDate: Triple<Int?, Int?, Int?>? = null
        if (!textView.text.isNullOrBlank())
            yearMonthDate =
                DateUtils.convertedMMMToddMM(textView.text.toString())
        if (datePickerDialog == null) {
            datePickerDialog = ViewUtils.showDatePicker(
                context = requireContext(),
                minDate = null,
                date = yearMonthDate,
                isMenstrualPeriod = true,
                disableFutureDate = disableFuture,
                cancelCallBack = { datePickerDialog = null }
            ) { _, year, month, dayOfMonth ->
                val stringDate = "$dayOfMonth-$month-$year"
                textView.text =
                    DateUtils.convertDateTimeToDate(
                        stringDate,
                        DATE_FORMAT_ddMMyyyy,
                        DATE_ddMMyyyy
                    )
                if (isLmp) {
                    pregnancyDetailsViewModel.pregnancyDetailsModel.lastMenstrualPeriod = DateUtils.convertDateFormat(textView.text.toString(), DATE_ddMMyyyy, DATE_FORMAT_yyyyMMdd)
                    calculateGestationalAgeAndEstimationDeliveryDate()
                } else {
                    pregnancyDetailsViewModel.pregnancyDetailsModel.estimatedDeliveryDate = DateUtils.convertDateFormat(textView.text.toString(), DATE_ddMMyyyy, DATE_FORMAT_yyyyMMdd)
                }
                pregnancyDetailsViewModel.checkSubmitBtn()
                datePickerDialog = null
            }
        }
    }

    private fun calculateGestationalAgeAndEstimationDeliveryDate() {
        val lmpText = binding.tvLastMenstrualPeriodDate.text.toString().trim()

        if (lmpText.isNotEmpty()) {
            val lmpDate = LocalDate.parse(lmpText, DateTimeFormatter.ofPattern(DATE_ddMMyyyy))
            val estimatedDeliveryDate = lmpDate.plusDays(EstimatedDeliveryDate)
            val formattedEstimatedDeliveryDate =
                estimatedDeliveryDate.format(DateTimeFormatter.ofPattern(DATE_ddMMyyyy))
            binding.tvEstimatedDeliveryDate.text = formattedEstimatedDeliveryDate
            pregnancyDetailsViewModel.pregnancyDetailsModel.estimatedDeliveryDate = estimatedDeliveryDate.format(DateTimeFormatter.ofPattern( DATE_FORMAT_yyyyMMdd))
            val gestationalAgeInWeeks = calculateGestationalAge(lmpDate)
            pregnancyDetailsViewModel.pregnancyDetailsModel.gestationalAge =
                gestationalAgeInWeeks
            binding.etGestationalAge.text =
                formatGestationalAge(gestationalAgeInWeeks, requireContext())
        }
    }
}