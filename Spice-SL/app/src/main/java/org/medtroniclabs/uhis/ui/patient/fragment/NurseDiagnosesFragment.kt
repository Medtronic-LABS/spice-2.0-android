package org.medtroniclabs.uhis.ui.patient.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.data.registration.PatientDetailsModel
import org.medtroniclabs.uhis.databinding.FragmentNurseDiagnosisBinding
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams
import org.medtroniclabs.uhis.formgeneration.extension.markMandatory
import org.medtroniclabs.uhis.formgeneration.model.FormLayout
import org.medtroniclabs.uhis.formgeneration.ui.SingleSelectionCustomView
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.patient.viewmodel.MedicalReviewBaseViewModel
import org.medtroniclabs.uhis.ui.patient.viewmodel.NurseMedicalReviewViewModel
import kotlin.getValue

class NurseDiagnosesFragment : BaseFragment() {
    private lateinit var binding: FragmentNurseDiagnosisBinding
    private var patientDetails: PatientDetailsModel? = null
    private val medicalReviewBaseViewModel: MedicalReviewBaseViewModel by activityViewModels()
    private val nurseViewModel: NurseMedicalReviewViewModel by activityViewModels()

    companion object {
        const val TAG = "NurseDiagnosesFragment"

        fun newInstance(): NurseDiagnosesFragment = NurseDiagnosesFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentNurseDiagnosisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        attachObserver()
    }

    private fun initView() {
        binding.tvSelfCareLabel.markMandatory()

        addCustomView(
            getData(),
            DefinedParams.SELF_CARE,
            medicalReviewBaseViewModel.selfCareSelection,
            selfCareSelectionCallBack,
            binding.llSelfCare,
        )
    }

    private fun attachObserver() {
        nurseViewModel.patientDetailsResponse.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.SUCCESS -> {
                    hideLoading()
                    resourceState.data?.let { details ->
                        patientDetails = details
                    }
                }

                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                    resourceState.message?.let {
                        (activity as BaseActivity).showErrorDialogue(
                            getString(R.string.error),
                            it,
                            false,
                        ) {}
                    }
                }
            }
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
                    translate = false,
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

    private val selfCareSelectionCallBack: (selectedID: Any?, elementId: String, serverViewModel: FormLayout, name: String?) -> Unit =
        { selectedID, _, _, _ ->
            val selectedValue = selectedID as? String ?: ""

            medicalReviewBaseViewModel.selfCareSelection[DefinedParams.SELF_CARE] = selectedValue
            nurseViewModel.nurseMrRequestModel.hadCounselling = selectedValue
        }

    private fun getData(): ArrayList<Map<String, Any>> {
        val flowList = ArrayList<Map<String, Any>>()
        flowList.add(getOptionMap(DefinedParams.YES, getString(R.string.yes)))
        flowList.add(getOptionMap(DefinedParams.NO, getString(R.string.no)))
        return flowList
    }

    fun getOptionMap(
        value: String,
        name: String,
    ): Map<String, Any> {
        val map = HashMap<String, Any>()
        map[DefinedParams.ID] = value
        map[DefinedParams.NAME] = name
        return map
    }

    fun validation(): Boolean {
        var isValid = true
        if (nurseViewModel.nurseMrRequestModel.hadCounselling.isNullOrEmpty()) {
            binding.tvSelfCareErrorMessage.visibility = View.VISIBLE
            isValid = false
        } else {
            binding.tvSelfCareErrorMessage.visibility = View.GONE
        }
        return isValid
    }

    fun resetSelection() {
        medicalReviewBaseViewModel.selfCareSelection.clear()
        if (!::binding.isInitialized) return
        addCustomView(
            getData(),
            DefinedParams.SELF_CARE,
            medicalReviewBaseViewModel.selfCareSelection,
            selfCareSelectionCallBack,
            binding.llSelfCare,
        )
    }
}
