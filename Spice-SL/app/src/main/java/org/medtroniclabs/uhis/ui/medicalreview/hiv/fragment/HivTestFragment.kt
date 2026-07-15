package org.medtroniclabs.uhis.ui.medicalreview.hiv.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.isVisible
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils.getOptionMap
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.DefinedParams.OTHER
import org.medtroniclabs.uhis.data.MedicalReviewMetaItems
import org.medtroniclabs.uhis.databinding.FragmentHivTestBinding
import org.medtroniclabs.uhis.formgeneration.extension.markMandatorys
import org.medtroniclabs.uhis.formgeneration.extension.markNonMandatory
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.model.FormLayout
import org.medtroniclabs.uhis.formgeneration.ui.SingleSelectionCustomView
import org.medtroniclabs.uhis.formgeneration.utility.CustomSpinnerAdapter
import org.medtroniclabs.uhis.formgeneration.utility.InformationLayoutFragment
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams
import org.medtroniclabs.uhis.ui.medicalreview.hiv.viewmodel.HivViewModel
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.A1_TEST_RESULT
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.A2_TEST_RESULT
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.A3_TEST_RESULT
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.HBsAg
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams.HIV_SYPHILIS_DUO_TEST
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewTypeEnums

class HivTestFragment : BaseFragment(), View.OnClickListener {
    private lateinit var binding: FragmentHivTestBinding
    private val hivViewModel: HivViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentHivTestBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        addObservers()
        testResultVisibility("")
    }

    private fun initViews() {
        hivViewModel.getHistoryListMetaItems()
        binding.tvPreTestCounselling.safeClickListener(this)
        binding.tvTestForHiv.safeClickListener(this)
        binding.tvPostTestCounselling.safeClickListener(this)
        binding.tvSyphilisTestResult.text = getString(R.string.hiv_syphilis_duo_test)
        binding.tvHBsAgTestResult.text = getString(R.string.hbsag_test)
        binding.tvA1TestResult.text = getString(R.string.a_test_result, 1)
        binding.tvA2TestResult.text = getString(R.string.a_test_result, 2)
        binding.tvA3TestResult.text = getString(R.string.a_test_result, 3)

        addCustomView(
            getSyphilisData(),
            HIV_SYPHILIS_DUO_TEST,
            hivViewModel.resultHashMap,
            SyphilisTestCallBack,
            binding.llSyphilisTestResultRoot,
        )
        addCustomView(
            getData(),
            HBsAg,
            hivViewModel.resultHashMap,
            HBsAgTestCallBack,
            binding.llHBsAgTestResultRoot,
        )
        addCustomView(
            getData(),
            A1_TEST_RESULT,
            hivViewModel.resultHashMap,
            A1TestCallBack,
            binding.llA1TestResultRoot,
        )
        addCustomView(
            getData(),
            A2_TEST_RESULT,
            hivViewModel.resultHashMap,
            A2TestCallBack,
            binding.llA2TestResultRoot,
        )
        addCustomView(
            getData(),
            A3_TEST_RESULT,
            hivViewModel.resultHashMap,
            A3TestCallBack,
            binding.llA3TestResultRoot,
        )

        val isEmtct = arguments?.getBoolean(DefinedParams.EMTCT, false)
        if (isEmtct == true) {
            binding.forEmtct.visible()
            binding.forA3.gone()
            binding.tvHBsAgTestResult.markMandatorys()
        } else {
            binding.tvA1TestResult.markMandatorys()
        }

        binding.etOtherEntryPoint.doAfterTextChanged { input ->
            input?.let {
                hivViewModel.otherEntryPoint =
                    if (it.trim().isNotEmpty()) it.trim().toString() else null
            }
        }
    }

    private fun addObservers() {
        hivViewModel.hivMetaListItems.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }

                ResourceState.SUCCESS -> {
                    hideProgress()
                    val isEmtct = arguments?.getBoolean(DefinedParams.EMTCT, false)
                    resourceState.data?.let { list ->
                        initializeEntryPointOptions(
                            list
                                .filter {
                                    it.category ==
                                        if (isEmtct == true) MedicalReviewTypeEnums.emtctEntryPoint.name else MedicalReviewTypeEnums.entry_point.name
                                }.sortedBy { it.displayOrder },
                        )
                    }
                }
            }
        }
    }

    private fun initializeEntryPointOptions(entryList: List<MedicalReviewMetaItems>) {
        val entryPoint = ArrayList<Map<String, Any>>()
        entryPoint.add(
            hashMapOf<String, Any>(
                DefinedParams.NAME to DefinedParams.DEFAULT_ID_LABEL,
                DefinedParams.ID to DefinedParams.DEFAULT_ID,
                DefinedParams.Value to DefinedParams.DEFAULT_ID_LABEL,
            ),
        )

        entryList.forEach {
            entryPoint.add(
                hashMapOf<String, Any>(
                    DefinedParams.NAME to it.name,
                    DefinedParams.ID to it.id.toString(),
                    DefinedParams.Value to (it.value ?: it.name),
                ),
            )
        }

        val adapter = CustomSpinnerAdapter(requireContext())
        adapter.setData(entryPoint)
        binding.tvEntryPointSpinner.adapter = adapter
        binding.tvEntryPointSpinner.setSelection(0, false)
        binding.tvEntryPointSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    view: View?,
                    pos: Int,
                    itemId: Long,
                ) {
                    val selectedItem = adapter.getData(position = pos)
                    selectedItem?.let {
                        val selectedName = it[DefinedParams.NAME] as String?
                        if (selectedName != DefinedParams.DEFAULT_ID_LABEL) {
                            hivViewModel.selectedEntryPoint = it[DefinedParams.Value] as String
                            if (hivViewModel.selectedEntryPoint.equals(OTHER, true)) {
                                binding.etOtherEntryPoint.visible()
                            } else if (binding.etOtherEntryPoint.isVisible()) {
                                binding.etOtherEntryPoint.gone()
                            }
                            resultMapChanged()
                        } else {
                            hivViewModel.selectedEntryPoint = null
                        }
                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    /**
                     * this method is not used
                     */
                }
            }
    }

    private fun addCustomView(
        data: ArrayList<Map<String, Any>>,
        tag: String,
        hashMap: HashMap<String, Any>,
        callback: ((selectedID: Any?, elementId: Pair<String, String?>, formLayout: FormLayout, name: String?) -> Unit)?,
        container: ViewGroup?,
    ) {
        SingleSelectionCustomView(binding.root.context).apply {
            this.tag = tag
            addViewElements(
                data,
                false,
                hashMap,
                Pair(tag, null),
                FormLayout(viewType = "", id = "", title = "", visibility = "", optionsList = null),
                callback,
            )
            container?.addView(this)
        }
    }

    private fun getData(): ArrayList<Map<String, Any>> {
        val flowList = ArrayList<Map<String, Any>>()
        flowList.add(getOptionMap(getString(R.string.reactive), getString(R.string.reactive)))
        flowList.add(
            getOptionMap(
                getString(R.string.non_reactive),
                getString(R.string.non_reactive),
            ),
        )
        flowList.add(
            getOptionMap(
                getString(R.string.inconclusive),
                getString(R.string.inconclusive),
            ),
        )
        return flowList
    }

    private fun getSyphilisData(): ArrayList<Map<String, Any>> {
        val flowList = ArrayList<Map<String, Any>>()
        flowList.add(getOptionMap(getString(R.string.hiv_ve_syphilis_ve), getString(R.string.hiv_ve_syphilis_ve)))
        flowList.add(getOptionMap(getString(R.string.hiv_ve_syphilis_ve_), getString(R.string.hiv_ve_syphilis_ve_)))
        flowList.add(getOptionMap(getString(R.string.hiv_ve_syphilis_ve__), getString(R.string.hiv_ve_syphilis_ve__)))
        flowList.add(getOptionMap(getString(R.string.hiv_ve__syphilis_ve), getString(R.string.hiv_ve__syphilis_ve)))

        return flowList
    }

    private var SyphilisTestCallBack: (selectedID: Any?, elementId: Pair<String, String?>, formLayout: FormLayout, name: String?) -> Unit =
        { selectedID, _, _, _ ->
            hivViewModel.resultHashMap[HIV_SYPHILIS_DUO_TEST] = selectedID as String
            resultMapChanged()
        }

    private var HBsAgTestCallBack: (selectedID: Any?, elementId: Pair<String, String?>, formLayout: FormLayout, name: String?) -> Unit =
        { selectedID, _, _, _ ->
            hivViewModel.resultHashMap[HBsAg] = selectedID as String
            testResultVisibility(HBsAg)
            resultMapChanged()
        }
    private var A1TestCallBack: (selectedID: Any?, elementId: Pair<String, String?>, formLayout: FormLayout, name: String?) -> Unit =
        { selectedID, _, _, _ ->
            if (!hivViewModel.resultHashMap[HBsAg].toString().isNullOrEmpty() || (binding.forEmtct.visibility != View.VISIBLE)) {
                hivViewModel.resultHashMap[A1_TEST_RESULT] = selectedID as String
                testResultVisibility(A1_TEST_RESULT)
                resultMapChanged()
                if (!hivViewModel.resultHashMap[A1_TEST_RESULT]
                        ?.toString()
                        .equals(getString(R.string.reactive), true)
                ) {
                    hivViewModel.resultHashMap.remove(A2_TEST_RESULT)
                    hivViewModel.resultHashMap.remove(A3_TEST_RESULT)
                    resetSelectionViews(A2_TEST_RESULT)
                    resetSelectionViews(A3_TEST_RESULT)
                    binding.tvA3TestResult.gone()
                    binding.llA3TestResultRoot.gone()
                }
            }
        }
    private var A2TestCallBack: (selectedID: Any?, elementId: Pair<String, String?>, formLayout: FormLayout, name: String?) -> Unit =
        { selectedID, _, _, _ ->
            if (hivViewModel.resultHashMap[A1_TEST_RESULT]?.toString().equals(getString(R.string.reactive), true)) {
                hivViewModel.resultHashMap[A2_TEST_RESULT] = selectedID as String
                testResultVisibility(A2_TEST_RESULT)
                resultMapChanged()
                if (!hivViewModel.resultHashMap[A2_TEST_RESULT]
                        ?.toString()
                        .equals(getString(R.string.reactive), true) ||
                    !hivViewModel.resultHashMap[A1_TEST_RESULT]
                        ?.toString()
                        .equals(getString(R.string.reactive), true)
                ) {
                    hivViewModel.resultHashMap.remove(A3_TEST_RESULT)
                    resetSelectionViews(A3_TEST_RESULT)
                }
            }
        }
    private var A3TestCallBack: (selectedID: Any?, elementId: Pair<String, String?>, formLayout: FormLayout, name: String?) -> Unit =
        { selectedID, _, _, _ ->
            if (hivViewModel.resultHashMap[A2_TEST_RESULT]?.toString().equals(getString(R.string.reactive), true)) {
                hivViewModel.resultHashMap[A3_TEST_RESULT] = selectedID as String
                resultMapChanged()
            }
        }

    private fun resultMapChanged() {
        setFragmentResult(
            MedicalReviewDefinedParams.HIV_TEST_ITEM,
            bundleOf(
                MedicalReviewDefinedParams.HIV_TEST_VALUES to true,
            ),
        )
    }

    companion object {
        const val TAG = "HivTestFragment"
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.tvPreTestCounselling -> {
                InformationLayoutFragment
                    .newInstance(
                        AssessmentDefinedParams.PreTestCounselling,
                        getString(R.string.pre_test_counselling),
                    ).show(childFragmentManager, InformationLayoutFragment.TAG)
            }

            R.id.tvTestForHiv -> {
                InformationLayoutFragment
                    .newInstance(
                        AssessmentDefinedParams.TestForHiv,
                        getString(R.string.test_for_hiv),
                    ).show(childFragmentManager, InformationLayoutFragment.TAG)
            }

            R.id.tvPostTestCounselling -> {
                InformationLayoutFragment
                    .newInstance(
                        AssessmentDefinedParams.PostTestCounselling,
                        getString(R.string.post_test_counselling),
                    ).show(childFragmentManager, InformationLayoutFragment.TAG)
            }
        }
    }

    fun validation(): Boolean {
        var isValid = true
        val a1TestResult = hivViewModel.resultHashMap[A1_TEST_RESULT] as? String
        val a2TestResult = hivViewModel.resultHashMap[A2_TEST_RESULT] as? String
        val a3TestResult = hivViewModel.resultHashMap[A3_TEST_RESULT] as? String
        val hbsgTestResult = hivViewModel.resultHashMap[HBsAg] as? String
        val isEmtct = arguments?.getBoolean(DefinedParams.EMTCT, false)

        val testCases = mutableListOf<Triple<Float, String?, View>>().apply {
            add(Triple(binding.tvA1TestResult.alpha, a1TestResult, binding.tvA1TestResultError))
            add(Triple(binding.tvA2TestResult.alpha, a2TestResult, binding.tvA2TestResultError))
            if (isEmtct == true) {
                add(Triple(binding.tvHBsAgTestResult.alpha, hbsgTestResult, binding.tvHBsAgTestResultError))
            } else if (binding.tvA3TestResult.alpha == 1.0f && binding.tvA3TestResult.visibility == View.VISIBLE) {
                add(Triple(binding.tvA3TestResult.alpha, a3TestResult, binding.tvA3TestResultError))
            }
        }

        for ((alpha, result, errorView) in testCases) {
            if (alpha == 1.0f && result.isNullOrEmpty()) {
                errorView.visible()
                isValid = false
            }
        }
        if (isValid && binding.etOtherEntryPoint.isVisible() && hivViewModel.otherEntryPoint == null) {
            binding.tvEntryPointError.visible()
            isValid = false
        } else {
            binding.tvEntryPointError.gone()
        }

        return isValid
    }

    private fun testResultVisibility(type: String) {
        val resultMap = hivViewModel.resultHashMap

        val hbsAgValue = resultMap[HBsAg]?.toString()
        val a1Value = resultMap[A1_TEST_RESULT]?.toString()
        val a2Value = resultMap[A2_TEST_RESULT]?.toString()

        val isHBsAgSet = !hbsAgValue.isNullOrEmpty()
        val isA1Set = !a1Value.isNullOrEmpty()
        val isA2Set = !a2Value.isNullOrEmpty()

        val isHBsAgReactive = !hbsAgValue.isNullOrEmpty()
        val isA1Reactive = a1Value.equals(getString(R.string.reactive), ignoreCase = true)
        val isA2Reactive = a2Value.equals(getString(R.string.reactive), ignoreCase = true)

        fun View.updateState(
            clickable: Boolean = false,
            enabled: Boolean = false,
            alpha: Float,
            visible: Boolean? = null,
        ) {
            visible?.let { visibility = if (it) View.VISIBLE else View.GONE }
            isClickable = clickable
            isEnabled = enabled
            this.alpha = alpha
        }

        val setAlpha = { view: View, condition: Boolean -> view.alpha = if (condition) 1.0f else 0.5f }

        if (binding.forEmtct.visibility != View.VISIBLE) {
            when (type) {
                A1_TEST_RESULT -> {
                    val isEnabled = !isA1Reactive
                    val isA2Reactives = isA2Reactive
                    binding.llA2TestResultRoot.updateState(isEnabled, isEnabled, if (isEnabled) 0.5f else 1.0f, true)
                    binding.tvA2TestResult.updateState(false, false, if (isA1Reactive) 1.0f else 0.5f, true)
                    binding.llA3TestResultRoot.updateState(alpha = if (isA1Reactive && isA2Reactives)1.0f else 0.5f, visible = isA1Reactive)
                    binding.tvA3TestResult.updateState(alpha = if (isA1Reactive && isA2Reactives)1.0f else 0.5f, visible = isA1Reactive)
                    if (isEnabled) {
                        binding.tvA2TestResult.markNonMandatory()
                        binding.tvA3TestResult.markNonMandatory()
                    } else {
                        binding.tvA2TestResult.markMandatorys()
                    }
                }

                A2_TEST_RESULT -> {
                    val show = isA1Reactive
                    val isEnabled = isA2Reactive
                    binding.llA3TestResultRoot.updateState(isEnabled, isEnabled, if (isEnabled) 1.0f else 0.5f, show)
                    binding.tvA3TestResult.updateState(false, false, if (isEnabled) 1.0f else 0.5f, show)

                    if (!isEnabled) {
                        binding.tvA3TestResult.markNonMandatory()
                    } else {
                        binding.tvA3TestResult.markMandatorys()
                    }
                }

                else -> {
                    binding.llA1TestResultRoot.updateState(true, true, 1.0f, true)
                    setAlpha(binding.tvA1TestResult, true)

                    binding.llA2TestResultRoot.updateState(true, true, 0.5f, true)
                    setAlpha(binding.tvA2TestResult, false)

                    binding.llA3TestResultRoot.visibility = View.GONE
                    binding.tvA3TestResult.visibility = View.GONE
                }
            }
        } else {
            when (type) {
                HBsAg -> {
                    binding.llA1TestResultRoot.updateState(isHBsAgReactive, isHBsAgReactive, if (isHBsAgReactive) 1.0f else 0.5f)
                    setAlpha(binding.tvA1TestResult, isHBsAgReactive)
                    binding.llA2TestResultRoot.updateState(alpha = if (isHBsAgReactive && isA1Reactive) 1.0f else 0.5f, visible = true)
                    binding.tvA2TestResult.updateState(alpha = if (isHBsAgReactive && isA1Reactive) 1.0f else 0.5f, visible = true)
                    if (isHBsAgReactive) {
                        binding.tvA1TestResult.markMandatorys()
                    } else {
                        binding.tvA1TestResult.markNonMandatory()
                    }
                    binding.llA2TestResultRoot.visible()
                    binding.llA2TestResultRoot.visible()
                }

                A1_TEST_RESULT -> {
                    val isEnabled = !isA1Reactive
                    binding.llA2TestResultRoot.updateState(isEnabled, isEnabled, if (isA1Reactive) 1.0f else 0.5f, isHBsAgReactive)
                    binding.tvA2TestResult.updateState(false, false, if (isA1Reactive) 1.0f else 0.5f, isHBsAgReactive)
                    if (isA1Reactive) {
                        binding.tvA2TestResult.markMandatorys()
                    } else {
                        binding.tvA2TestResult.markNonMandatory()
                    }
                }

                A2_TEST_RESULT -> {
                }

                else -> {
                    binding.llA1TestResultRoot.updateState(isHBsAgReactive, isHBsAgReactive, if (isHBsAgReactive) 1.0f else 0.5f)
                    setAlpha(binding.tvA1TestResult, isHBsAgReactive)

                    val a2Enabled = !isA1Reactive
                    binding.llA2TestResultRoot.updateState(a2Enabled, a2Enabled, if (isA1Reactive) 1.0f else 0.5f, isHBsAgReactive)
                    binding.tvA2TestResult.updateState(false, false, if (isA1Reactive) 1.0f else 0.5f, isHBsAgReactive)

                    val showA3 = isA1Set && isA1Reactive
                    val a3Enabled = isA2Set && isA2Reactive
                    binding.llA3TestResultRoot.updateState(a3Enabled, a3Enabled, if (isA2Reactive) 1.0f else 0.5f, showA3)
                    binding.tvA3TestResult.updateState(false, false, if (showA3) 1.0f else 0.5f, showA3)
                }
            }
        }
    }

    private fun resetSelectionViews(vararg viewTags: String) {
        viewTags.forEach { tag ->
            val view = binding.root.findViewWithTag<SingleSelectionCustomView>(tag)
            view?.resetSingleSelectionChildViews()
        }
    }
}
