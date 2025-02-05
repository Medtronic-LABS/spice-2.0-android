package com.medtroniclabs.opensource.ui.medicalreview.underfiveyears


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.activityViewModels
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.invisible
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.DefinedParams.DefaultID
import com.medtroniclabs.opensource.common.DefinedParams.DefaultIDLabel
import com.medtroniclabs.opensource.common.DefinedParams.GREEN_MAX_MUAC
import com.medtroniclabs.opensource.common.DefinedParams.ID
import com.medtroniclabs.opensource.common.DefinedParams.NAME
import com.medtroniclabs.opensource.common.DefinedParams.RED_MAX_MUAC
import com.medtroniclabs.opensource.common.DefinedParams.YELLOW_MAX_MUAC
import com.medtroniclabs.opensource.common.DefinedParams.Value
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.data.MedicalReviewMetaItems
import com.medtroniclabs.opensource.databinding.FragmentUnderFiveYearClinicalSummarryBinding
import com.medtroniclabs.opensource.formgeneration.extension.markMandatory
import com.medtroniclabs.opensource.formgeneration.model.FormLayout
import com.medtroniclabs.opensource.formgeneration.ui.SingleSelectionCustomView
import com.medtroniclabs.opensource.formgeneration.utility.CustomSpinnerAdapter
import com.medtroniclabs.opensource.model.medicalreview.WazWhzScoreRequest
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseFragment
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.MotherNeonateUtil.initTextWatcherForString
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.MotherNeonateUtil.isBasicValid
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.MotherNeonateUtil.isValidInput
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewDefinedParams
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewDefinedParams.MOTHER_VITAMIN_TAG

class ClinicalSummaryUnderFiveYearsFragment : BaseFragment() {
    private lateinit var binding: FragmentUnderFiveYearClinicalSummarryBinding
    private val viewModel: UnderFiveYearsClinicalSummaryViewModel by activityViewModels()

    companion object {
        const val TAG = "ClinicalSummaryUnderFiveYearsFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentUnderFiveYearClinicalSummarryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initListeners()
        attachObserver()
    }

    private fun initListeners() {
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
                it, binding.etRepeat.text?.trim().toString()
            )
            showHideRepeatField(it.toDoubleOrNull())
        }
        initTextWatcherForString(binding.etRepeat) {
            viewModel.updateRespiratoryRate(
                binding.etRespirationRate.text?.trim().toString(), it
            )
        }
        initTextWatcherForString(binding.etWAZ) {
            viewModel.updateWaz(it)
        }
        initTextWatcherForString(binding.etWHZ) {
            viewModel.updateWhz(it)
        }
        initTextWatcherForString(binding.etMUACStatus) { input ->
            input.toDoubleOrNull()?.let { value ->
                binding.muacStatusGroup.visible()
                viewModel.updateMuac(value, requireContext())
                setMuacStatus(value)
            } ?: kotlin.run {
                binding.muacStatusGroup.gone()
            }
        }
    }

    private fun initializeImmunisationStatus(list: List<MedicalReviewMetaItems>) {
        val dropDownList = ArrayList<Map<String, Any>>()
        dropDownList.add(
            hashMapOf<String, Any>(
                NAME to DefaultIDLabel, ID to DefaultID
            )
        )
        for (item in list) {
            dropDownList.add(
                hashMapOf<String, Any>(
                    NAME to item.name,
                    DefinedParams.id to item.id.toString(),
                    Value to (item.value ?: item.name)
                )
            )
        }
        setImmunisationStatus(dropDownList)
    }

    private fun setImmunisationStatus(list: ArrayList<Map<String, Any>>) {
        val adapter = CustomSpinnerAdapter(requireContext())
        adapter.setData(list)
        binding.etImmunisation.adapter = adapter
        binding.etImmunisation.setSelection(0, false)
        binding.etImmunisation.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?, view: View?, position: Int, id: Long
                ) {
                    val selectedItem = adapter.getData(position = position)
                    selectedItem?.let {
                        val selectedId = it[ID] as String?
                        val selectedImmunisationStatus = it[DefinedParams.Value] as String?
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

    private fun initView() {
        binding.tvHeightLabel.markMandatory()
        binding.tvWeightLabel.markMandatory()
        getFlowData().let {
            val view = SingleSelectionCustomView(binding.root.context)
            view.tag = MOTHER_VITAMIN_TAG
            view.addViewElements(
                it,
                SecuredPreference.getIsTranslationEnabled(),
                viewModel.resultMotherVitaminHashMap,
                Pair(MOTHER_VITAMIN_TAG, null),
                FormLayout(viewType = "", id = "", title = "", visibility = "", optionsList = null),
                vitaminAMotherSelectionCallBack
            )
            binding.etVitaminAForMother.addView(view)
        }
        getFlowData().let {
            val view = SingleSelectionCustomView(binding.root.context).apply {
                tag = MedicalReviewDefinedParams.Albendazole
                addViewElements(
                    it,
                    false,
                    viewModel.albendazoleHashMap,
                    Pair(MedicalReviewDefinedParams.Albendazole, null),
                    FormLayout(
                        viewType = "", id = "", title = "", visibility = "", optionsList = null
                    ),
                    albendazoleSelectionCallBack
                )
            }
            binding.llAlbendazoleStatus.addView(view)
        }

        viewModel.getImmunisationStatusMetaItems()

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
                        initializeImmunisationStatus(list)

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

    private var vitaminAMotherSelectionCallBack: ((selectedID: Any?, elementId: Pair<String, String?>, serverViewModel: FormLayout, name: String?) -> Unit)? =
        { selectedID, _, _, _ ->
            viewModel.resultMotherVitaminHashMap[MOTHER_VITAMIN_TAG] = selectedID as String
            viewModel.updateVitaminAForMother()
        }

    private var albendazoleSelectionCallBack: ((selectedID: Any?, elementId: Pair<String, String?>, serverViewModel: FormLayout, name: String?) -> Unit)? =
        { selectedID, _, _, _ ->
            viewModel.albendazoleHashMap[MedicalReviewDefinedParams.Albendazole] =
                selectedID as String
            viewModel.updateAlbendazole()
        }

    private fun getFlowData(): ArrayList<Map<String, Any>> {
        val flowList = ArrayList<Map<String, Any>>()
        flowList.add(CommonUtils.getOptionMap(getString(R.string.yes), getString(R.string.yes)))
        flowList.add(CommonUtils.getOptionMap(getString(R.string.no), getString(R.string.no)))
        return flowList
    }

    fun validateEditFields(): Boolean {
        val weight = weightValidate()
        val height = heightValidate()
        val temperature = temperatureValidate()
        val respirationRate = respirationRateValidate()
        val repeat = repeatValidate()
        val muac = muacValidate()
//        val whz = whzValidate()
//        val waz = wazValidate()
        return weight && height && temperature && respirationRate && repeat && muac
    }

    private fun whzValidate(): Boolean {
        return isBasicValid(
            binding.etWHZ.text.toString(),
            binding.tvWHZError,
            0,
            getString(R.string.error_label),
            context = requireContext(),
            isMandatory = true
        )
    }

    private fun muacValidate(): Boolean {
        return isValidInput(
            binding.etMUACStatus.text.toString(),
            binding.etMUACStatus,
            binding.tvMUACError,
            0.0..26.5,
            (R.string.please_enter_muac_between_0_to_26),
            false,
            requireContext()
        )
    }


    private fun wazValidate(): Boolean {
        return isBasicValid(
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

    private fun setMuacStatus(muacValue: Double) {
        val muacText: String
        val muacInput: String

        if (muacValue <= RED_MAX_MUAC) {
            muacText = getString(R.string.muac_red)
            muacInput = getString(R.string.severe_nutrition)
        } else if (muacValue > RED_MAX_MUAC && muacValue <= YELLOW_MAX_MUAC) {
            muacText = getString(R.string.muac_yellow)
            muacInput = getString(R.string.moderate_malnutrition)
        } else if (muacValue > YELLOW_MAX_MUAC && muacValue <= GREEN_MAX_MUAC) {
            muacText = getString(R.string.muac_green)
            muacInput = getString(R.string.normal)
        } else {
            binding.muacStatusGroup.gone()
            muacText = getString(R.string.empty__)
            muacInput = getString(R.string.empty__)
        }
        binding.tvMUACText.text = muacText
        binding.tvMUACInput.text = muacInput
    }

    private fun showHideRepeatField(respiration: Double?) {
        if (respiration != null && respiration > 60.0) {
            binding.repeatGroup.visible()
        } else {
            binding.etRepeat.setText("")
            binding.repeatGroup.invisible()
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