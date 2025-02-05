package com.medtroniclabs.opensource.ui.medicalreview.undertwomonths.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.invisible
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.CommonUtils.getOptionMap
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.DefinedParams.DefaultID
import com.medtroniclabs.opensource.common.DefinedParams.DefaultIDLabel
import com.medtroniclabs.opensource.common.DefinedParams.ID
import com.medtroniclabs.opensource.common.DefinedParams.NAME
import com.medtroniclabs.opensource.common.DefinedParams.Yes
import com.medtroniclabs.opensource.common.DefinedParams.Value
import com.medtroniclabs.opensource.data.MedicalReviewMetaItems
import com.medtroniclabs.opensource.databinding.FragmentClinicalSummaryBinding
import com.medtroniclabs.opensource.formgeneration.extension.markMandatory
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.formgeneration.model.FormLayout
import com.medtroniclabs.opensource.formgeneration.ui.SingleSelectionCustomView
import com.medtroniclabs.opensource.formgeneration.utility.CustomSpinnerAdapter
import com.medtroniclabs.opensource.model.medicalreview.WazWhzScoreRequest
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseFragment
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.MotherNeonateUtil
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.MotherNeonateUtil.initTextWatcherForString
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.MotherNeonateUtil.isValidInput
import com.medtroniclabs.opensource.ui.medicalreview.undertwomonths.viewmodel.ClinicalSummaryViewModel
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewDefinedParams.BREAST_FEEDING_TAG
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewDefinedParams.EXCLUSIVE_BREAST_FEED_TAG
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewDefinedParams.MOTHER_VITAMIN_TAG

class ClinicalSummaryFragment : BaseFragment(), View.OnClickListener {
    private lateinit var binding: FragmentClinicalSummaryBinding
    val viewModel: ClinicalSummaryViewModel by activityViewModels()

    companion object {
        const val TAG = "ClinicalSummaryFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            FragmentClinicalSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initListeners()
        clickListener()
        attachObserver()
    }

    private fun attachObserver() {
        viewModel.summaryMetaListItems.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.SUCCESS -> {
                    hideProgress()
                    resourceState.data?.let { list ->
                        initializeDeliveryTypeItem(list)
                    }
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }
            }
        }
        viewModel.wazWhzScoreResponseLiveData.observe(viewLifecycleOwner) {
                resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.SUCCESS -> {
                    hideProgress()
                    resourceState.data?.let { it ->
                        it.let { entity ->
                            binding.etWAZ.setText(entity.wfa?.toString() ?: "")
                            binding.etWHZ.setText(entity.wfh?.toString() ?: "")
                        }
                    }
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }
            }
        }
    }

    private fun initListeners() {
        binding.apply {
            tvWeightLabel.markMandatory()
            tvHeightLabel.markMandatory()
        }
        initTextWatcherForString(binding.etWeight) {
            viewModel.updateWeight(it)
        }
        initTextWatcherForString(binding.etHeight) {
            viewModel.updateHeight(it)
        }
        initTextWatcherForString(binding.etTemperature) {
            viewModel.updateTemperature(it)
        }
        initTextWatcherForString(binding.etRespirationRate) {
            viewModel.updateRespiratoryRate(
                it,
                binding.etRepeat.text?.trim().toString()
            )
            showHideRepeatField(it.toDoubleOrNull())
        }
        binding.etRepeat.doAfterTextChanged {
            viewModel.updateRespiratoryRate(
                binding.etRespirationRate.text?.trim().toString(),
                it?.trim().toString()
            )
        }
        initTextWatcherForString(binding.etWAZ) {
            viewModel.updateWaz(it)
        }
        initTextWatcherForString(binding.etWHZ) {
            viewModel.updateWhz(it)
        }
    }

    private fun showHideRepeatField(respiration: Double?) {
        if (respiration != null && respiration > 60.0) {
            binding.repeatGroup.visible()
        } else {
            binding.etRepeat.setText("")
            binding.repeatGroup.invisible()
        }
    }
    private fun clickListener() {
        binding.etVitaminAForMother.safeClickListener(this)
        binding.llExclusiveBreastFeedingStatus.safeClickListener(this)
        binding.llBreastFeedingStatus.safeClickListener(this)
    }

    fun initView() {
        addCustomView(
            getBreastFeedingFlowData(),
            BREAST_FEEDING_TAG,
            viewModel.resultBreastFeedingHashMap,
            breastFeedingSelectionCallback,
            binding.llBreastFeedingStatus
        )

        addCustomView(
            getVitaminAMotherFlowData(),
            MOTHER_VITAMIN_TAG,
            viewModel.resultMotherVitaminHashMap,
            vitaminAMotherSelectionCallBack,
            binding.etVitaminAForMother
        )

        addCustomView(
            getExclusiveBreastFeedingFlowData(),
            EXCLUSIVE_BREAST_FEED_TAG,
            viewModel.resultExclusiveBreastFeedHashMap,
            exclusiveBreastFeedingSelectionCallBack,
            binding.llExclusiveBreastFeedingStatus
        )
        viewModel.getImmunisationStatusMetaItems()
    }

    private fun addCustomView(
        data: ArrayList<Map<String, Any>>,
        tag: String,
        hashMap: HashMap<String, Any>,
        callback: ((selectedID: Any?, elementId: Pair<String, String?>, serverViewModel: FormLayout, name: String?) -> Unit)?,
        container: ViewGroup
    ) {
        SingleSelectionCustomView(binding.root.context).apply {
            this.tag = tag
            addViewElements(
                data,
                false,
                hashMap,
                Pair(tag, null),
                FormLayout(viewType = "", id = "", title = "", visibility = "", optionsList = null),
                callback
            )
            container.addView(this)
        }
    }

    private val breastFeedingSelectionCallback: (selectedID: Any?, elementId: Pair<String, String?>, serverViewModel: FormLayout, name: String?) -> Unit =
        { selectedID, _, _, _ ->
            viewModel.resultBreastFeedingHashMap[BREAST_FEEDING_TAG] = selectedID as String
            resetSelectionViews(EXCLUSIVE_BREAST_FEED_TAG)
            enableExclusiveBreastFeeding(selectedID)
            viewModel.updateBreastFeeding()
        }

    private val vitaminAMotherSelectionCallBack: (selectedID: Any?, elementId: Pair<String, String?>, serverViewModel: FormLayout, name: String?) -> Unit =
        { selectedID, _, _, _ ->
            viewModel.resultMotherVitaminHashMap[MOTHER_VITAMIN_TAG] = selectedID as String
            viewModel.updateVitaminAForMother()
        }

    private val exclusiveBreastFeedingSelectionCallBack: (selectedID: Any?, elementId: Pair<String, String?>, serverViewModel: FormLayout, name: String?) -> Unit =
        { selectedID, _, _, _ ->
            viewModel.resultExclusiveBreastFeedHashMap[EXCLUSIVE_BREAST_FEED_TAG] = selectedID as String
            viewModel.updateExclusiveBreastFeeding()
        }


    private fun enableExclusiveBreastFeeding(selectedID: Any?) {
        if (selectedID == Yes) {
            binding.exclusiveBreastFeedingGroup.visible()
        } else {
            binding.exclusiveBreastFeedingGroup.gone()
            viewModel.resultExclusiveBreastFeedHashMap.clear()
        }
    }

    private fun getBreastFeedingFlowData(): ArrayList<Map<String, Any>> {
        val flowList = ArrayList<Map<String, Any>>()
        flowList.add(getOptionMap(getString(R.string.yes), getString(R.string.yes)))
        flowList.add(getOptionMap(getString(R.string.no), getString(R.string.no)))
        return flowList
    }

    private fun getVitaminAMotherFlowData(): ArrayList<Map<String, Any>> {
        val flowList = ArrayList<Map<String, Any>>()
        flowList.add(getOptionMap(getString(R.string.yes), getString(R.string.yes)))
        flowList.add(getOptionMap(getString(R.string.no), getString(R.string.no)))
        return flowList
    }

    private fun getExclusiveBreastFeedingFlowData(): ArrayList<Map<String, Any>> {
        val flowList = ArrayList<Map<String, Any>>()
        flowList.add(getOptionMap(getString(R.string.yes), getString(R.string.yes)))
        flowList.add(getOptionMap(getString(R.string.no), getString(R.string.no)))
        return flowList
    }

    private fun initializeDeliveryTypeItem(status: List<MedicalReviewMetaItems>) {
        val dropDownList = ArrayList<Map<String, Any>>()
        dropDownList.add(
            hashMapOf<String, Any>(
                NAME to DefaultIDLabel,
                ID to DefaultID
            )
        )
        for (item in status) {
            dropDownList.add(
                hashMapOf<String, Any>(
                    NAME to item.name,
                    DefinedParams.id to item.id.toString(),
                    Value to (item.value ?: item.name)
                )
            )
        }
        setListenerToDeliveryStatus(dropDownList)
    }

    private fun setListenerToDeliveryStatus(list: ArrayList<Map<String, Any>>) {
        val adapter = CustomSpinnerAdapter(requireContext())
        adapter.setData(list)
        binding.etImmunisationStatus.adapter = adapter
        binding.etImmunisationStatus.setSelection(0, false)
        binding.etImmunisationStatus.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedItem = adapter.getData(position = position)
                    selectedItem?.let {
                        val selectedId = it[ID] as String?
                        val selectedImmunisationStatus = it[Value] as String?
                        if (selectedId != DefaultID) {
                            selectedImmunisationStatus?.let {
                                viewModel.selectedImmunisationStatus = it
                                viewModel.updateImmunisationStatus()
                            }
                        }
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    /**
                     * this method is not used
                     */
                }
            }
    }

    override fun onClick(v: View?) {

    }

    fun isAnyEditTextFilled(): Boolean {
        return binding.etWeight.text?.isNotBlank() == true &&
                binding.etHeight.text?.isNotBlank() == true &&
                binding.etTemperature.text?.isNotBlank() == true &&
                binding.etRepeat.text?.isNotBlank() == true &&
                binding.etRespirationRate.text?.isNotBlank() == true &&
                viewModel.selectedImmunisationStatus != null &&
                viewModel.resultMotherVitaminHashMap[MOTHER_VITAMIN_TAG] != null &&
                viewModel.resultBreastFeedingHashMap[BREAST_FEEDING_TAG] != null ||
                viewModel.resultExclusiveBreastFeedHashMap[EXCLUSIVE_BREAST_FEED_TAG] != null
    }

    fun validateEditFields(): Boolean {
        val weight = weightValidate()
        val height = heightValidate()
        val temperature = temperatureValidate()
        val respirationRate = respirationRateValidate()
        val repeat = repeatValidate()
//        val whz=whzValidate()
//        val waz=wazValidate()
        return weight && height && temperature && respirationRate && repeat
    }
    private fun whzValidate(): Boolean {
        return MotherNeonateUtil.isBasicValid(
            binding.etWHZ.text.toString(),
            binding.tvWHZError,
            0,
            getString(R.string.error_label),
            context = requireContext(),
            isMandatory = true
        )
    }

    private fun wazValidate(): Boolean {
        return MotherNeonateUtil.isBasicValid(
            binding.etWAZ.text.toString(),
            binding.tvWAZError,
            0,
            getString(R.string.error_label),
            context = requireContext(),
            isMandatory = true
        )
    }

    private fun heightValidate(): Boolean {
        return isValidInput(
            binding.etHeight.text.toString(),
            binding.etHeight,
            binding.tvHeightError,
            45.0..120.0,
            R.string.please_enter_valid_value_between_45_to_120,
            true,
            requireContext()
        )
    }

    private fun respirationRateValidate(): Boolean {
        return isValidInput(
            binding.etRespirationRate.text.toString(),
            binding.etRespirationRate,
            binding.tvRespirationRateError,
            1.0..99.9,
            (R.string.respiratory_error),
            false,
            requireContext()
        )
    }

    private fun repeatValidate(): Boolean {
        return isValidInput(
            binding.etRepeat.text.toString(),
            binding.etRepeat,
            binding.tvRepeatError,
            1.0..99.9,
            (R.string.please_enter_repeat_between_0_to_100),
            false,
            requireContext()
        )
    }

    private fun temperatureValidate(): Boolean {
        return isValidInput(
            binding.etTemperature.text.toString(),
            binding.etTemperature,
            binding.tvTemperatureLabelError,
            10.0..200.0,
            (R.string.please_enter_temperature_between_10_to_200),
            false,
            requireContext()
        )
    }


    private fun weightValidate(): Boolean {
        return isValidInput(
            binding.etWeight.text.toString(),
            binding.etWeight,
            binding.tvWeightError,
            0.1..400.0,
            R.string.weight_error_0_400,
            true,
            requireContext()
        )
    }

    private fun resetSelectionViews(vararg viewTags: String) {
        viewTags.forEach { tag ->
            val view = binding.root.findViewWithTag<SingleSelectionCustomView>(tag)
            view?.resetSingleSelectionChildViews()
        }
    }
    private fun wazPopulateScore() {
        binding.apply {
            etWAZ.isEnabled = false
            etWHZ.isEnabled = false
            if (arguments?.getInt(DefinedParams.Age)?.toString()?.toInt() in 0..60) {
                val onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) {
                        val gender = arguments?.getString(DefinedParams.Gender)
                        val age = arguments?.getInt(DefinedParams.Age)?.toString()
                        val weight = etWeight.text?.toString()
                        val height = etHeight.text?.toString()

                        val request = WazWhzScoreRequest(
                            gender = gender,
                            ageInMonths = age,
                            weight = if (weight.isNullOrEmpty()) null else weight,
                            height = if (height.isNullOrEmpty()) null else height
                        )
                        val heightText = etHeight.text.toString()
                        val heightInt = heightText.toIntOrNull()

                        val isHeightValid = heightInt == null || (heightInt in 45..120)

                        if (weight.isNullOrEmpty() || weight=="0") {
                            etWAZ.setText("")
                            etWHZ.setText("")
                            tvHeightError.visibility = if (isHeightValid) View.GONE else View.VISIBLE
                        } else {
                            tvHeightError.visibility = if (isHeightValid) View.GONE else View.VISIBLE
                            if (isHeightValid) {
                                viewModel.getWazWhzScore(request)
                            } else {
                                etWHZ.setText("")
                            }
                        }

                    }
                }
                etWeight.onFocusChangeListener = onFocusChangeListener
                etHeight.onFocusChangeListener = onFocusChangeListener

            } else {
                etWAZ.setText("0")
                etWHZ.setText("0")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        wazPopulateScore()
    }


}