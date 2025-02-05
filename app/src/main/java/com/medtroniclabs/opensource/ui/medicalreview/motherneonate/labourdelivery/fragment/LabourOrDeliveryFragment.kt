package com.medtroniclabs.opensource.ui.medicalreview.motherneonate.labourdelivery.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.invisible
import com.medtroniclabs.opensource.appextensions.isVisible
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.DateUtils.getCalendarFromString
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.common.ViewUtils
import com.medtroniclabs.opensource.data.LabourDeliveryMetaEntity
import com.medtroniclabs.opensource.data.model.ChipViewItemModel
import com.medtroniclabs.opensource.databinding.FragmentLabourOrDeliveryBinding
import com.medtroniclabs.opensource.formgeneration.extension.markMandatory
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.formgeneration.model.FormLayout
import com.medtroniclabs.opensource.formgeneration.ui.SingleSelectionCustomView
import com.medtroniclabs.opensource.formgeneration.utility.CustomSpinnerAdapter
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseFragment
import com.medtroniclabs.opensource.ui.TagListCustomView
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.labourdelivery.viewmodel.LabourDeliveryViewModel
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewDefinedParams
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewTypeEnums
import java.util.Calendar

class LabourOrDeliveryFragment : BaseFragment() {

    private lateinit var binding: FragmentLabourOrDeliveryBinding
    private val viewModel: LabourDeliveryViewModel by activityViewModels()
    private lateinit var cgNeonateOutcome: TagListCustomView
    private lateinit var  viewTimeOfDelivery:SingleSelectionCustomView
    private lateinit var  viewTimeOfDeliveryOnset:SingleSelectionCustomView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLabourOrDeliveryBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        const val TAG = "LabourOrDeliveryFragment"

        fun newInstance(): LabourOrDeliveryFragment {
            return LabourOrDeliveryFragment()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mandatoryFields()
        attachObserver()
        initListeners()
        initializeDatePicker()
        initializeTimeOfDelivery()
        initializeTimeOfLabourOnset()
        initDirectPncChipGroup()
    }

    private fun initDirectPncChipGroup() {
        with(binding) {
            if (!viewModel.isDirectPnc) {
                cgNeonateOutcome.gone()
                tvNeonateOutcomeLabel.gone()
                seperatorColon1.gone()
            } else {
                hideAllGroup.gone()
                hideAllErrorGroup.gone()
                cgNeonateOutcome.visible()
                tvNeonateOutcomeLabel.visible()
                seperatorColon1.visible()
                deliveryPlaceOtherGroup.gone()
                tvDeliveryByOthersError.gone()
            }
        }

        cgNeonateOutcome = TagListCustomView(binding.root.context, binding.cgNeonateOutcome) { _, _, _ ->
            cgNeonateOutcome.getSelectedTags().also { selectedTags ->
                clearUiValues()

                val hasSelectedTags = selectedTags.isNotEmpty()
                with(binding) {
                    cgNeonateOutcomeError.showIf(viewModel.neonateOutcome.isNullOrEmpty() )
                }

                if (hasSelectedTags) {
                    binding.cgNeonateOutcomeError.gone()
                    val selectedValue = selectedTags[0].value
                    viewModel.neonateOutcome = selectedValue
                    viewModel.neonateOutComeStateLiveData.value = selectedTags


                    with(binding) {
                        hideAllGroup.gone()
                        hideAllErrorGroup.gone()
                        if (selectedValue == MedicalReviewDefinedParams.Miscarriage || selectedValue.isNullOrEmpty()) {
                            hideAllGroup.gone()
                            hideAllErrorGroup.gone()
                            deliveryPlaceOtherGroup.gone()
                            tvDeliveryByOthersError.gone()
                        } else {
                            hideAllGroup.visible()
                            hideAllErrorGroup.gone()
                            tvDeliveryByOthersError.gone()
                        }
                    }
                } else {
                    viewModel.neonateOutcome = null
                    viewModel.neonateOutComeStateLiveData.value = null
                    with(binding) {
                        hideAllGroup.gone()
                        hideAllErrorGroup.gone()
                        etOtherDeliveryPlace.gone()
                        tvOtherDeliveryAt.gone()
                        tvOtherPlaceError.gone()
                    }
                }

            }

//            viewModel.validateSubmitButtonState()
        }
    }

    private fun initListeners() {
        binding.etHrsTimeOfDelivery.doAfterTextChanged {
            val timeOfDeliveryInHour = it?.trim().toString()
            if (timeOfDeliveryInHour.isNotEmpty()) {
                viewModel.timeOfDeliveryInHour = it.toString()
                viewModel.validateSubmitButtonState()
            } else {
                viewModel.timeOfDeliveryInHour = null
                viewModel.validateSubmitButtonState()
            }

        }

        binding.etOtherDeliveryPlace.doAfterTextChanged {
            val deliveryPlaceOthers = it?.trim().toString()
            if (deliveryPlaceOthers.isNotEmpty()) {
                viewModel.deliveryPlaceOthers = it.toString()
                viewModel.validateSubmitButtonState()
            } else {
                viewModel.deliveryPlaceOthers = null
                viewModel.validateSubmitButtonState()
            }
        }

        binding.etMinutesTimeOfDelivery.doAfterTextChanged {
            val timeOfDeliveryInMinutes = it?.trim().toString()
            if (timeOfDeliveryInMinutes.isNotEmpty()) {
                viewModel.timeOfDeliveryInMinute = it.toString()
                viewModel.validateSubmitButtonState()
            } else {
                viewModel.timeOfDeliveryInMinute = null
                viewModel.validateSubmitButtonState()
            }
        }

        binding.etHrsTimeOfLabourOnset.doAfterTextChanged {
            val labourOnSetInMinutes = it?.trim().toString()
            if (labourOnSetInMinutes.isNotEmpty()) {
                viewModel.timeOfLabourOnSetInHour = it.toString()
                viewModel.validateSubmitButtonState()
            } else {
                viewModel.timeOfLabourOnSetInHour = null
                viewModel.validateSubmitButtonState()
            }
        }

        binding.etMinutesTimeOfLabourOnset.doAfterTextChanged {
            val labourOnSetInMinutes = it?.trim().toString()
            if (labourOnSetInMinutes.isNotEmpty()) {
                viewModel.timeOfLabourOnSetInMinutes = it.toString()
                viewModel.validateSubmitButtonState()
            } else {
                viewModel.timeOfLabourOnSetInMinutes = null
                viewModel.validateSubmitButtonState()
            }
        }

        binding.etNoOfDeonates.doAfterTextChanged {
            val noOfNenonates = it?.trim().toString()
            if (noOfNenonates.isNotEmpty()) {
                viewModel.noOfNeonates = it.toString()
                viewModel.validateSubmitButtonState()
            } else {
                viewModel.noOfNeonates = null
                viewModel.validateSubmitButtonState()
            }

        }
        binding.etDeliveryByOthers.doAfterTextChanged {
            val deliveryByOthers = it?.trim().toString()
            if (deliveryByOthers.isNotEmpty()) {
                viewModel.deliveryByOthers = it.toString()
                viewModel.validateSubmitButtonState()
            } else {
                viewModel.deliveryByOthers = null
                viewModel.validateSubmitButtonState()
            }

        }
    }

    fun mandatoryFields() {
        binding.tvDateOfDelivery.markMandatory()
        binding.tvDeliveryByOthers.markMandatory()
        binding.tvTimeOfDelivery.markMandatory()
        binding.tvDateOfLabourOnset.markMandatory()
        binding.tvTimeOfLabourOnset.markMandatory()
        binding.tvDeliveryType.markMandatory()
        binding.tvDeliveryBy.markMandatory()
        binding.tvDeliveryAt.markMandatory()
        binding.tvDeliveryStatus.markMandatory()
        binding.tvNoOfDeonates.markMandatory()
        binding.tvNeonateOutcomeLabel.markMandatory()
        binding.tvOtherDeliveryAt.markMandatory()
    }


    private fun initializeTimeOfLabourOnset() {
        getDataFlowData().let {
            viewTimeOfDeliveryOnset = SingleSelectionCustomView(binding.root.context)
            viewTimeOfDeliveryOnset.tag = TAG
            viewTimeOfDeliveryOnset.addViewElements(
                it,
                SecuredPreference.getIsTranslationEnabled(),
                viewModel.timeOfLabourOnsetMap,
                Pair(DefinedParams.TimeOfLabourOnset, null),
                FormLayout(viewType = "", id = "", title = "", visibility = "", optionsList = null),
                timeOfLabourOnsetSingleSelectionCallback
            )
            binding.timeOfLabourOnsetGroup.addView(viewTimeOfDeliveryOnset)
        }
    }

    private fun initializeTimeOfDelivery() {
        getDataFlowData().let {
            viewTimeOfDelivery = SingleSelectionCustomView(binding.root.context)
            viewTimeOfDelivery.tag = TAG
            viewTimeOfDelivery.addViewElements(
                it,
                SecuredPreference.getIsTranslationEnabled(),
                viewModel.timeOfDeliveryMap,
                Pair(DefinedParams.TimeOfDelivery, null),
                FormLayout(
                    viewType = "",
                    id = "",
                    title = "",
                    visibility = "",
                    optionsList = null
                ),
                timeOfDeliverySingleSelectionCallback
            )
            binding.timeOfDeliveryGroup.addView(viewTimeOfDelivery)
        }
    }

    private var timeOfDeliverySingleSelectionCallback: ((selectedID: Any?, elementId: Pair<String, String?>, serverViewModel: FormLayout, name: String?) -> Unit)? =
        { selectedID, _, _, _ ->
            viewModel.timeOfDeliveryMap[DefinedParams.TimeOfDelivery] = selectedID as String
            viewModel.validateSubmitButtonState()
        }

    private var timeOfLabourOnsetSingleSelectionCallback: ((selectedID: Any?, elementId: Pair<String, String?>, serverViewModel: FormLayout, name: String?) -> Unit)? =
        { selectedID, _, _, _ ->
            viewModel.timeOfLabourOnsetMap[DefinedParams.TimeOfLabourOnset] = selectedID as String
            viewModel.validateSubmitButtonState()
        }

    private fun getDataFlowData(): ArrayList<Map<String, Any>> {
        val flowList = ArrayList<Map<String, Any>>()
        flowList.add(CommonUtils.getOptionMap(getString(R.string.am), getString(R.string.am)))
        flowList.add(CommonUtils.getOptionMap(getString(R.string.pm), getString(R.string.pm)))
        return flowList
    }

    private fun initializeDatePicker() {
        binding.etDateOfDelivery.safeClickListener {
            setListenerToDatePicker(binding.etDateOfDelivery, true)
        }
        binding.etDateOfLabourOnset.safeClickListener {
            setListenerToDatePicker(binding.etDateOfLabourOnset)
        }
    }

    private fun setListenerToDatePicker(textView: AppCompatTextView, isDelivery: Boolean = false) {
        var yearMonthDate: Triple<Int?, Int?, Int?>? = null
        if (textView.text.isNotEmpty())
            yearMonthDate = DateUtils.convertedMMMToddMM(textView.text.toString())
        ViewUtils.showDatePicker(
            requireContext(),
            date = yearMonthDate,
            maxDate = if (isDelivery) Calendar.getInstance().timeInMillis else getMaxDateForOnset(),
            cancelCallBack = { }
        ) { _, year, month, dayOfMonth ->
            val stringDate = "$dayOfMonth-$month-$year"
            if (isDelivery) {
                viewModel.dateOfDelivery = Triple(year, month, dayOfMonth)
                viewModel.setDate(year, month, dayOfMonth)
                viewModel.dateOfLabourOnset = null
                binding.etDateOfLabourOnset.text = ""
            } else {
                viewModel.dateOfLabourOnset = Triple(year, month, dayOfMonth)
            }
            textView.text =
                DateUtils.convertDateTimeToDate(
                    stringDate,
                    DateUtils.DATE_FORMAT_ddMMyyyy,
                    DateUtils.DATE_ddMMyyyy
                )
            viewModel.validateSubmitButtonState()
        }
    }

    private fun getMaxDateForOnset(): Long? {
        if (binding.etDateOfDelivery.text.isNotEmpty()) {
            return getCalendarFromString(
                binding.etDateOfDelivery.text.toString(),
                DateUtils.DATE_ddMMyyyy
            )
        } else {
            return Calendar.getInstance().timeInMillis
        }
    }


    private fun attachObserver() {
        viewModel.labourDeliveryMetaList.observe(viewLifecycleOwner) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.SUCCESS -> {
                    resource.data?.let { listItems ->
                        initializeDeliveryTypeItem(listItems)
                        initializeDeliveryAtItem(listItems)
                        initializeDeliveryByItem(listItems)
                        initializeDeliveryStatusItem(listItems)
                        initializeNeonateOutcomeItems(listItems)
                    }
                    hideProgress()
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }
            }

        }
    }

    private fun initializeDeliveryStatusItem(listItems: List<LabourDeliveryMetaEntity>) {
        val list = arrayListOf<Map<String, Any>>()
        list.add(
            hashMapOf<String, Any>(
                DefinedParams.NAME to DefinedParams.DefaultIDLabel,
                DefinedParams.ID to DefinedParams.DefaultID
            )
        )
        listItems.filter { it.category == MedicalReviewTypeEnums.DeliveryStatus.name }.forEach {
            list.add(
                hashMapOf<String, Any>(
                    DefinedParams.NAME to it.name,
                    DefinedParams.ID to it.id,
                    DefinedParams.Value to it.value
                )
            )
        }
        val adapter = CustomSpinnerAdapter(requireContext())
        adapter.setData(list)
        binding.etDeliveryStatus.adapter = adapter


        binding.etDeliveryStatus.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                    val selectedItem = adapter.getData(position = pos)
                    selectedItem?.let {
                        val selectedName = it[DefinedParams.Value] as String?
                        if (selectedName != DefinedParams.DefaultIDLabel) {
                            viewModel.deliveryStatus = selectedName
                            viewModel.validateSubmitButtonState()
                        } else {
                            viewModel.deliveryStatus = null
                            viewModel.validateSubmitButtonState()
                        }
                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                }

            }
    }

    private fun initializeDeliveryAtItem(listItems: List<LabourDeliveryMetaEntity>) {
        val list = arrayListOf<Map<String, Any>>()
        list.add(
            hashMapOf<String, Any>(
                DefinedParams.NAME to DefinedParams.DefaultIDLabel,
                DefinedParams.ID to DefinedParams.DefaultID
            )
        )
        listItems.filter { it.category == MedicalReviewTypeEnums.DeliveryAt.name }.forEach {
            list.add(
                hashMapOf<String, Any>(
                    DefinedParams.NAME to it.name,
                    DefinedParams.ID to it.id,
                    DefinedParams.Value to it.value
                )
            )
        }
        val adapter = CustomSpinnerAdapter(requireContext())
        adapter.setData(list)
        binding.etDeliveryAt.adapter = adapter

        binding.etDeliveryAt.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                val selectedItem = adapter.getData(position = pos)
                selectedItem?.let {
                    val selectedName = it[DefinedParams.Value] as String?
                    if (selectedName != DefinedParams.DefaultIDLabel) {
                        viewModel.deliveryAt = selectedName
                        viewModel.validateSubmitButtonState()
                        if (selectedName == DefinedParams.Other.lowercase()){
                            binding.deliveryPlaceOtherGroup.visible()
                        }
                        else{
                            validateOtherDeliveryAt()
                        }
                    } else {
                        viewModel.deliveryAt = null
                        viewModel.validateSubmitButtonState()
                        validateOtherDeliveryAt()
                    }
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
            }

        }


    }

    private fun validateOtherDeliveryAt() {
        if (binding.deliveryPlaceOtherGroup.isVisible() || binding.tvOtherPlaceError.isVisible()){
            binding.deliveryPlaceOtherGroup.gone()
            binding.etOtherDeliveryPlace.text = null
            binding.tvOtherPlaceError.gone()
        }
    }

    private fun initializeDeliveryByItem(listItems: List<LabourDeliveryMetaEntity>) {
        val list = arrayListOf<Map<String, Any>>()
        list.add(
            hashMapOf<String, Any>(
                DefinedParams.NAME to DefinedParams.DefaultIDLabel,
                DefinedParams.ID to DefinedParams.DefaultID,
            )
        )
        listItems.filter { it.category == MedicalReviewTypeEnums.DeliveryBy.name }.forEach {
            list.add(
                hashMapOf<String, Any>(
                    DefinedParams.NAME to it.name,
                    DefinedParams.ID to it.id,
                    DefinedParams.Value to it.value
                )
            )
        }
        val adapter = CustomSpinnerAdapter(requireContext())
        adapter.setData(list)
        binding.etDeliveryBy.adapter = adapter

        binding.etDeliveryBy.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                val selectedItem = adapter.getData(position = pos)
                selectedItem?.let {
                    val selectedName = it[DefinedParams.Value] as String?
                    if (selectedName != DefinedParams.DefaultIDLabel) {
                        viewModel.deliveryBy = selectedName
                        if (viewModel.deliveryBy == DefinedParams.Others_Specify) {
                            binding.deliveryOthersGroup.visible()
                            val newMarginTop = resources.getDimensionPixelSize(R.dimen._20sdp)
                            binding.tvDeliveryStatus.layoutParams =
                                (binding.tvDeliveryStatus.layoutParams as ConstraintLayout.LayoutParams).apply {
                                    topMargin = newMarginTop
                                }
                            binding.tvDeliveryStatus.requestLayout()
                        } else {
                            binding.etDeliveryByOthers.text = null
                            binding.tvDeliveryStatus.layoutParams =
                                (binding.tvDeliveryStatus.layoutParams as ConstraintLayout.LayoutParams).apply {
                                    topMargin = 0
                                }
                            binding.tvDeliveryByOthersError.gone()
                            binding.deliveryOthersGroup.gone()
                        }
                        viewModel.validateSubmitButtonState()
                    } else {
                        binding.tvDeliveryStatus.layoutParams =
                            (binding.tvDeliveryStatus.layoutParams as ConstraintLayout.LayoutParams).apply {
                                topMargin = 0
                            }
                        binding.tvDeliveryByOthersError.gone()
                        binding.deliveryOthersGroup.gone()
                        viewModel.deliveryBy = null
                        viewModel.validateSubmitButtonState()
                    }
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
            }

        }
    }

    private fun initializeDeliveryTypeItem(listItems: List<LabourDeliveryMetaEntity>) {
        val list = arrayListOf<Map<String, Any>>()
        list.add(
            hashMapOf<String, Any>(
                DefinedParams.NAME to DefinedParams.DefaultIDLabel,
                DefinedParams.ID to DefinedParams.DefaultID
            )
        )
        listItems.filter { it.category == MedicalReviewTypeEnums.DeliveryType.name }.forEach {
            list.add(
                hashMapOf<String, Any>(
                    DefinedParams.NAME to it.name,
                    DefinedParams.ID to it.id,
                    DefinedParams.Value to it.value

                )
            )
        }
        val adapter = CustomSpinnerAdapter(requireContext())
        adapter.setData(list)
        binding.etDeliveryType.adapter = adapter

        binding.etDeliveryType.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                    val selectedItem = adapter.getData(position = pos)
                    selectedItem?.let {
                        val selectedName = it[DefinedParams.Value] as String?
                        if (selectedName != DefinedParams.DefaultIDLabel) {
                            viewModel.deliveryType = selectedName
                            viewModel.validateSubmitButtonState()
                        } else {
                            viewModel.deliveryType = null
                            viewModel.validateSubmitButtonState()
                        }
                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                }

            }
    }

    private fun valid12ClockMinuteRange(minute: String?): Boolean {
        if (minute.isNullOrEmpty()) {
            return false

        }
        val minuteInt = minute.toInt()
        return minuteInt in 0..59
    }

    private fun valid12ClockHourRange(hour: String?): Boolean {
        if (hour.isNullOrEmpty()) {
            return false
        }
        val hourInt = hour.toInt()
        return hourInt in 1..12
    }

    fun validate(): Boolean {
        val isValidDateTimeDelivery = labourDeliveryValidation()
        return isValidDateTimeDelivery
    }

    private fun View.showIf(condition: Boolean) {
        this.isVisible = condition
    }

    private fun showLabourDeliveryErrors(status: Boolean) {
        with(binding) {
            tvDateOfLabourOnsetError.showIf(status)
            tvDateOfDeliveryError.showIf(status)
            tvTimeOfDeliveryError.showIf(status)
            tvTimeOfLabourOnsetError.showIf(status)
            tvDeliveryTypeError.showIf(status)
            tvDeliveryByError.showIf(status)
            tvDeliveryAtError.showIf(status)
            tvDeliveryStatusError.showIf(status)
            tvNoOfDeonatesError.showIf(status)
            val deliveryByOthers = viewModel.deliveryByOthers

            if (viewModel.deliveryBy == DefinedParams.Others_Specify) {
                tvDeliveryByOthersError.showIf(deliveryByOthers?.isEmpty() == true)
            }

            if (viewModel.deliveryAt == DefinedParams.Other.lowercase()) {
                tvOtherPlaceError.showIf(viewModel.deliveryPlaceOthers?.isEmpty() == true)
            }

        }
    }

    private fun labourDeliveryValidation(): Boolean {
        if (viewModel.neonateOutcome.isNullOrEmpty()) {
            if (viewModel.isDirectPnc) {
                binding.cgNeonateOutcomeError.showIf(viewModel.neonateOutcome.isNullOrEmpty())
                binding.hideAllErrorGroup.gone()
                binding.hideAllGroup.gone()
                return false
            } else {
                return labourDeliveryFieldValidation()
            }
        }else {
          return  labourDeliveryFieldValidation()
        }
    }

    private fun labourDeliveryFieldValidation():Boolean{
        showLabourDeliveryErrors(false)

        val etHourTimeOfDelivery = binding.etHrsTimeOfDelivery.text?.trim().toString()
        val etMinutesTimeOfDelivery =
            binding.etMinutesTimeOfDelivery.text?.trim().toString()
        val dateOfDelivery = viewModel.dateOfDelivery

        val etHourTimeOfLabourOnset = binding.etHrsTimeOfLabourOnset.text?.trim().toString()
        val etMinutesTimeOfLabourOnSet =
            binding.etMinutesTimeOfLabourOnset.text?.trim().toString()
        val dateOfLabourOnset = viewModel.dateOfLabourOnset
        val noOfNeonate = viewModel.noOfNeonates?.toInt()
        val deliveryByOthers = viewModel.deliveryByOthers
        val deliveryOthers = viewModel.deliveryBy
        val deliveryOther =
            (deliveryOthers == DefinedParams.Others_Specify && deliveryByOthers?.isNotEmpty() == true || deliveryOthers != DefinedParams.Others_Specify)
        val otherDeliveryPlace = viewModel.deliveryPlaceOthers
        val otherDeliveryAt =
            (viewModel.deliveryAt == DefinedParams.Other.lowercase() && otherDeliveryPlace?.isNotEmpty() == true || viewModel.deliveryAt != DefinedParams.Other.lowercase())
        var isValidDeliveryBy = viewModel.deliveryBy != null &&
                viewModel.deliveryAt != null &&
                viewModel.deliveryStatus != null
        var isValidDelivery = etHourTimeOfDelivery.isNotEmpty() &&
                etMinutesTimeOfDelivery.isNotEmpty() &&
                dateOfDelivery != null &&
                deliveryOther &&
                timeValidation(
                    etHourTimeOfLabourOnset,
                    etHourTimeOfDelivery,
                    etMinutesTimeOfLabourOnSet,
                    etMinutesTimeOfDelivery
                ) &&
                viewModel.timeOfDeliveryMap[DefinedParams.TimeOfDelivery] != null &&
                etHourTimeOfLabourOnset.isNotEmpty() &&
                etMinutesTimeOfLabourOnSet.isNotEmpty() &&
                dateOfLabourOnset != null &&
                viewModel.timeOfLabourOnsetMap[DefinedParams.TimeOfLabourOnset] != null &&
                viewModel.deliveryType != null &&
                viewModel.deliveryBy != null &&
                otherDeliveryAt &&
                viewModel.deliveryStatus != null &&
                viewModel.noOfNeonates != null && noOfNeonate != 0



        if (!isValidDeliveryBy) {
            showLabourDeliveryErrors(true)
        }


        with(binding) {
            if (etHourTimeOfDelivery.isEmpty() || etMinutesTimeOfDelivery.isEmpty() || etHourTimeOfLabourOnset.isEmpty() || etMinutesTimeOfLabourOnSet.isEmpty()) {
                tvTimeOfDeliveryError.showIf(true)
                tvTimeOfLabourOnsetError.showIf(true)
            }
            tvDeliveryByError.showIf(viewModel.deliveryBy == null)
            tvDeliveryStatusError.showIf(viewModel.deliveryStatus == null)

            if (etHourTimeOfDelivery.isNotEmpty() && etMinutesTimeOfDelivery.isNotEmpty()) {
                if (etHourTimeOfDelivery.toInt() <= 12 && etMinutesTimeOfDelivery.toInt() <= 59 && viewModel.timeOfDeliveryMap[DefinedParams.TimeOfDelivery] != null) {
                    tvTimeOfDeliveryError.showIf(false)
                } else {
                    tvTimeOfDeliveryError.showIf(true)
                }
            }
            if (etHourTimeOfLabourOnset.isNotEmpty() && etMinutesTimeOfLabourOnSet.isNotEmpty()) {
                if (etHourTimeOfLabourOnset.toInt() <= 12 && etMinutesTimeOfLabourOnSet.toInt() <= 59 && viewModel.timeOfLabourOnsetMap[DefinedParams.TimeOfLabourOnset] != null) {
                    tvTimeOfLabourOnsetError.showIf(false)
                } else {
                    tvTimeOfLabourOnsetError.showIf(true)
                }
            }

            if (viewModel.dateOfDelivery == viewModel.dateOfLabourOnset) {
                if (viewModel.timeOfLabourOnsetMap[DefinedParams.TimeOfLabourOnset] == getString(
                        R.string.am
                    ) && viewModel.timeOfDeliveryMap[DefinedParams.TimeOfDelivery] == getString(
                        R.string.am
                    )
                    || viewModel.timeOfLabourOnsetMap[DefinedParams.TimeOfLabourOnset] == getString(
                        R.string.pm
                    ) && viewModel.timeOfDeliveryMap[DefinedParams.TimeOfDelivery] == getString(
                        R.string.pm
                    )
                    || viewModel.timeOfLabourOnsetMap[DefinedParams.TimeOfLabourOnset] == getString(
                        R.string.pm
                    ) && viewModel.timeOfDeliveryMap[DefinedParams.TimeOfDelivery] == getString(
                        R.string.am
                    )
                ) {
                    if (etHourTimeOfLabourOnset.isEmpty() && etHourTimeOfDelivery.isEmpty() && etMinutesTimeOfLabourOnSet.isEmpty() && etMinutesTimeOfDelivery.isEmpty()) {
                        tvTimeOfLabourOnsetError.showIf(true)
                        tvTimeOfDeliveryError.showIf(true)
                        isValidDelivery=false
                    } else {
                        if (etHourTimeOfDelivery == "" || etMinutesTimeOfDelivery == "") {
                            tvTimeOfDeliveryError.showIf(true)
                            isValidDelivery=false
                        } else if (etHourTimeOfLabourOnset == "" || etMinutesTimeOfLabourOnSet == "") {
                            tvTimeOfLabourOnsetError.showIf(true)
                            isValidDelivery=false
                        } else {
                            if (etHourTimeOfLabourOnset.toInt() > etHourTimeOfDelivery.toInt()) {
                                tvTimeOfLabourOnsetError.showIf(true)
                                tvTimeOfDeliveryError.showIf(true)
                                isValidDelivery=false
                            }
                            if (etHourTimeOfDelivery.toInt() == 12 && etHourTimeOfLabourOnset.toInt() < etHourTimeOfDelivery.toInt()) {
                                tvTimeOfLabourOnsetError.showIf(true)
                                tvTimeOfDeliveryError.showIf(true)
                                isValidDelivery=false
                            }
                            if (etHourTimeOfLabourOnset.toInt() == 12 && etHourTimeOfLabourOnset.toInt() > etHourTimeOfDelivery.toInt()) {
                                tvTimeOfLabourOnsetError.showIf(false)
                                tvTimeOfDeliveryError.showIf(false)
                                isValidDelivery=true
                            }
                            if (etHourTimeOfLabourOnset.toInt() == etHourTimeOfDelivery.toInt() && etMinutesTimeOfLabourOnSet.toInt() > etMinutesTimeOfDelivery.toInt()) {
                                tvTimeOfLabourOnsetError.showIf(true)
                                tvTimeOfDeliveryError.showIf(true)
                                isValidDelivery=false
                            }
                            if (viewModel.timeOfLabourOnsetMap[DefinedParams.TimeOfLabourOnset] == getString(
                                    R.string.pm
                                ) && viewModel.timeOfDeliveryMap[DefinedParams.TimeOfDelivery] == getString(
                                    R.string.am
                                )
                            ) {
                                tvTimeOfLabourOnsetError.showIf(true)
                                tvTimeOfDeliveryError.showIf(true)
                                isValidDelivery=false
                            }

                        }
                    }

                }
            }

            tvDateOfDeliveryError.showIf(dateOfDelivery == null)
            tvDateOfLabourOnsetError.showIf(dateOfLabourOnset == null)
            tvDeliveryTypeError.showIf(viewModel.deliveryType == null)
            tvDeliveryByError.showIf(viewModel.deliveryBy == null)
            tvDeliveryAtError.showIf(viewModel.deliveryAt == null)
            tvDeliveryStatusError.showIf(viewModel.deliveryStatus == null)
            if (viewModel.noOfNeonates == null || noOfNeonate == 0) {
                tvNoOfDeonatesError.showIf(true)
            } else {
                tvNoOfDeonatesError.showIf(false)
            }
            if (deliveryOthers == DefinedParams.Others_Specify) {
                tvDeliveryByOthersError.showIf(deliveryByOthers == null)
            }
            if (viewModel.deliveryAt == DefinedParams.Other.lowercase()) {
                tvOtherPlaceError.showIf(viewModel.deliveryPlaceOthers == null)
            }
            if (viewModel.deliveryAt == null && viewModel.deliveryBy == DefinedParams.Others_Specify && viewModel.deliveryByOthers != null) {
                binding.tvDeliveryByOthersError.invisible()
            }
            if (viewModel.deliveryAt != null && viewModel.deliveryBy == DefinedParams.Others_Specify && viewModel.deliveryByOthers == null) {
                binding.tvDeliveryAtError.invisible()
            }
            if (viewModel.deliveryType != null && viewModel.deliveryBy == null) {
                binding.tvDeliveryTypeError.invisible()
            }
        }
        return isValidDelivery

    }

    private fun timeValidation(
        etHourTimeOfLabourOnset: String,
        etHourTimeOfDelivery: String,
        etMinutesTimeOfLabourOnSet: String,
        etMinutesTimeOfDelivery: String
    ): Boolean {
        val labourOnsetTime = viewModel.timeOfLabourOnsetMap[DefinedParams.TimeOfLabourOnset]
        val deliveryTime = viewModel.timeOfDeliveryMap[DefinedParams.TimeOfDelivery]

        val isSamePeriod = (labourOnsetTime == deliveryTime) ||
                (labourOnsetTime == getString(R.string.pm) && deliveryTime == getString(R.string.am))

        if (viewModel.dateOfDelivery == viewModel.dateOfLabourOnset && isSamePeriod) {
            if (etHourTimeOfLabourOnset.isNotEmpty() && etHourTimeOfDelivery.isNotEmpty() &&
                etMinutesTimeOfLabourOnSet.isNotEmpty() && etMinutesTimeOfDelivery.isNotEmpty()
            ) {

                val labourOnsetHour = etHourTimeOfLabourOnset.toInt()
                val deliveryHour = etHourTimeOfDelivery.toInt()
                val labourOnsetMinute = etMinutesTimeOfLabourOnSet.toInt()
                val deliveryMinute = etMinutesTimeOfDelivery.toInt()

                return when {
                    labourOnsetHour > deliveryHour -> false
                    labourOnsetHour == deliveryHour && labourOnsetMinute > deliveryMinute -> false
                    labourOnsetTime == getString(R.string.pm) && deliveryTime == getString(R.string.am) -> false
                    else -> true
                }
            }
        }
        return true
    }
    private fun initializeNeonateOutcomeItems(listItems: List<LabourDeliveryMetaEntity>) {
        val chipItemList = ArrayList<ChipViewItemModel>()
        listItems.filter { it.category == MedicalReviewTypeEnums.NeonateOutcome.name }.forEach {
            chipItemList.add(
                ChipViewItemModel(
                    id = it.id,
                    name = it.name,
                    type = it.type,
                    value = it.value
                )
            )
        }
        cgNeonateOutcome.addChipItemList(chipItemList)
    }

    private fun clearUiValues() {
        binding.apply {
            etDateOfDelivery.text = ""
            etDateOfLabourOnset.text = ""
            etHrsTimeOfDelivery.setText("")
            etMinutesTimeOfDelivery.setText("")
            etHrsTimeOfLabourOnset.setText("")
            etMinutesTimeOfLabourOnset.setText("")
            etNoOfDeonates.setText("")
            etDeliveryByOthers.setText("")
            etDeliveryType.setSelection(0)
            etDeliveryBy.setSelection(0)
            etDeliveryAt.setSelection(0)
            etDeliveryStatus.setSelection(0)
            viewModel.dateOfDelivery = null
            viewModel.dateOfLabourOnset = null
            viewModel.deliveryType = null
            viewModel.deliveryBy = null
            viewModel.deliveryAt = null
            viewModel.deliveryStatus = null
            viewModel.noOfNeonates = null
            viewModel.deliveryByOthers = null
            viewModel.neonateOutcome = null
            viewTimeOfDeliveryOnset.resetSingleSelectionChildViews()
            viewTimeOfDelivery.resetSingleSelectionChildViews()
        }
    }
}