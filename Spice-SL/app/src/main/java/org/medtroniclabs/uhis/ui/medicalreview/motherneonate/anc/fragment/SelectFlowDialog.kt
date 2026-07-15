package org.medtroniclabs.uhis.ui.medicalreview.motherneonate.anc.fragment

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.appextensions.invisible
import org.medtroniclabs.uhis.appextensions.setWidth
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.DefinedParams.EMTCT
import org.medtroniclabs.uhis.common.DefinedParams.HIV
import org.medtroniclabs.uhis.common.DefinedParams.ID
import org.medtroniclabs.uhis.common.DefinedParams.MEMBER_ID
import org.medtroniclabs.uhis.common.DefinedParams.PatientId
import org.medtroniclabs.uhis.common.DefinedParams.villageId
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.databinding.FragmentSelectFlowDialogBinding
import org.medtroniclabs.uhis.formgeneration.model.FormLayout
import org.medtroniclabs.uhis.formgeneration.ui.SingleSelectionCustomView
import org.medtroniclabs.uhis.network.utils.ConnectivityManager
import org.medtroniclabs.uhis.ui.home.ToolsViewModel
import org.medtroniclabs.uhis.ui.household.ConsentFormActivity
import org.medtroniclabs.uhis.ui.medicalreview.hiv.activity.HivImrAndCmrActivity
import org.medtroniclabs.uhis.ui.medicalreview.hiv.activity.HivMedicalReviewBaseActivity
import org.medtroniclabs.uhis.ui.medicalreview.motherneonate.anc.activity.MotherNeonateANCActivity
import org.medtroniclabs.uhis.ui.medicalreview.motherneonate.emtct.activity.MotherNeonateEMTCTActivity
import org.medtroniclabs.uhis.ui.medicalreview.motherneonate.labourdelivery.activity.LabourDeliveryBaseActivity
import org.medtroniclabs.uhis.ui.medicalreview.motherneonate.pnc.activity.MotherNeonatePncActivity
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams
import org.medtroniclabs.uhis.ui.mypatients.viewmodel.PatientDetailViewModel
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@AndroidEntryPoint
class SelectFlowDialog : DialogFragment(), View.OnClickListener {
    private lateinit var binding: FragmentSelectFlowDialogBinding
    private val viewModel: ToolsViewModel by activityViewModels()
    private val patientDetailViewModel: PatientDetailViewModel by activityViewModels()

    @Inject
    lateinit var connectivityManager: ConnectivityManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentSelectFlowDialogBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        isCancelable = false
        return binding.root
    }

    companion object {
        const val TAG = "SelectFlowDialog"

        fun newInstance(): SelectFlowDialog = SelectFlowDialog()

        fun newInstance(
            patientId: String?,
            id: String?,
            childPatientId: String?,
            dateOfDelivery: String?,
            neonateOutcome: String?,
            memberId: String?,
            isEMTCTFlow: Boolean?,
            hivTestedPositive: Boolean?,
            isMenutypeHiv: Boolean?,
        ): SelectFlowDialog {
            val fragment = SelectFlowDialog()
            val bundle = Bundle()
            bundle.putString(DefinedParams.PatientId, patientId)
            bundle.putString(DefinedParams.ID, id)
            bundle.putString(DefinedParams.CHILD_PATIENT_ID, childPatientId)
            bundle.putString(DefinedParams.DateOfDelivery, dateOfDelivery)
            bundle.putString(DefinedParams.NEONATE_OUTCOME, neonateOutcome)
            bundle.putString(MEMBER_ID, memberId)
            bundle.putBoolean(DefinedParams.EMTCT, isEMTCTFlow == true)
            bundle.putBoolean(DefinedParams.HIV_TESTED_POSITIVE, hivTestedPositive == true)
            bundle.putBoolean(DefinedParams.IS_MENU_TYPE_HIV, isMenutypeHiv == true)
            fragment.arguments = bundle
            return fragment
        }

        fun newInstanceHiv(
            patientId: String?,
            id: String?,
            isHIV: Boolean = false,
            isPregnant: Boolean = false,
            memberId: String?,
            village: String,
        ): SelectFlowDialog {
            val fragment = SelectFlowDialog()
            val bundle = Bundle()
            bundle.putString(PatientId, patientId)
            bundle.putString(ID, id)
            bundle.putBoolean(HIV, isHIV)
            bundle.putBoolean(DefinedParams.isPregnant, isPregnant)
            bundle.putString(MEMBER_ID, memberId)
            bundle.putString(DefinedParams.villageId, village)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onStart() {
        super.onStart()
        handleDialogSize()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        handleDialogSize()
    }

    private fun handleDialogSize() {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val width = if (CommonUtils.checkIsTablet(requireContext())) {
            if (isLandscape) 65 else 90
        } else {
            if (isLandscape) 65 else 90
        }
        setWidth(width)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        setListener()
//        selectType()
    }

    private var singleSelectionCallback: ((selectedID: Any?, elementId: Pair<String, String?>, formLayout: FormLayout, name: String?) -> Unit)? =
        { selectedID, _, _, _ ->
            viewModel.resultANCFlowHashMap[TAG] = selectedID as String
            if (connectivityManager.isNetworkAvailable()) {
                launchActivity()
            }
            if (viewModel.resultANCFlowHashMap[TAG] != getString(R.string.anc)) {
                dismiss()
            }
        }

    private fun launchActivity() {
        when (viewModel.resultANCFlowHashMap[TAG]) {
            getString(R.string.anc) -> {
                // if hivtestpositive true -> emtct mr
                // if hivtestpositive false -> isEMtctflow true -> Selection dialog else -> ANC
                if (arguments?.getBoolean(
                        DefinedParams.HIV_TESTED_POSITIVE,
                        false,
                    ) == true &&
                    arguments?.getBoolean(DefinedParams.IS_MENU_TYPE_HIV, false) == true
                ) {
                    val patientId = arguments?.getString(DefinedParams.PatientId, "")
                    val id = arguments?.getString(DefinedParams.ID, "")
                    val memberId = arguments?.getString(MEMBER_ID, "")
                    val intent =
                        Intent(requireContext(), MotherNeonateEMTCTActivity::class.java)
                    if (patientId?.isNotBlank() == true) {
                        intent.putExtra(PatientId, patientId)
                        intent.putExtra(ID, id)
                        intent.putExtra(MEMBER_ID, memberId)
                        intent.putExtra(EMTCT, true)
                        intent.putExtra(PatientId, patientId)
                        intent.putExtra(ID, id)
                        intent.putExtra(EMTCT, true)
                    }
                    startActivity(intent)
                    dismiss()
                } else {
                    binding.tvTitle.text = getString(R.string.select_patient_type)
                    binding.tvSubTitle.visible()
                    if (arguments?.getBoolean(DefinedParams.EMTCT, false) == true && arguments?.getBoolean(DefinedParams.IS_MENU_TYPE_HIV, false) == true) {
                        binding.selectionGroup.removeAllViews()
                        viewModel.isEMTCT = true
                        getPositiveType().let {
                            val view = SingleSelectionCustomView(requireContext())
                            view.tag = TAG
                            view.addViewElements(
                                it,
                                SecuredPreference.getIsTranslationEnabled(),
                                viewModel.resultANCFlowHashMap,
                                Pair(TAG, null),
                                FormLayout(viewType = "", id = "", title = "", visibility = "", optionsList = null),
                                singleSelectionCallback,
                            )

                            binding.selectionGroup.addView(view)
                        }
                    } else {
                        val patientId = arguments?.getString(PatientId, "")
                        val id = arguments?.getString(ID, "")
                        val intent = Intent(requireContext(), MotherNeonateANCActivity::class.java)
                        if (patientId?.isNotBlank() == true) {
                            intent.putExtra(PatientId, patientId)
                            intent.putExtra(ID, id)
                        }
                        startActivity(intent)
                        dismiss()
                    }
                }
            }

            getString(R.string.pnc) -> {
                val patientId = arguments?.getString(PatientId, "")
                val id = arguments?.getString(ID, "")
                val childId = arguments?.getString(DefinedParams.CHILD_PATIENT_ID, null)
                val neonateOutcome = arguments?.getString(DefinedParams.NEONATE_OUTCOME, null)
                val targetActivity = if (!childId.isNullOrEmpty()) {
                    MotherNeonatePncActivity::class.java
                } else {
                    if (neonateOutcome == MedicalReviewDefinedParams.MaceratedStillBirth ||
                        neonateOutcome == MedicalReviewDefinedParams.FreshStillBirth ||
                        neonateOutcome == MedicalReviewDefinedParams.StillBirth ||
                        neonateOutcome == MedicalReviewDefinedParams.Miscarriage
                    ) {
                        MotherNeonatePncActivity::class.java
                    } else {
                        LabourDeliveryBaseActivity::class.java
                    }
                }
                val intent = Intent(requireContext(), targetActivity).apply {
                    if (!patientId.isNullOrBlank()) {
                        putExtra(PatientId, patientId)
                        putExtra(ID, id)
                    }
                    if (targetActivity == LabourDeliveryBaseActivity::class.java) {
                        putExtra(DefinedParams.DIRECT_PNC_FLOW, true)
                    }
                }
                startActivity(intent)
            }

            getString(R.string.child_hood_visit) -> {
            }
            getString(R.string.labour_delivery) -> {
                val patientId = arguments?.getString(PatientId, "")
                val intent = Intent(requireContext(), LabourDeliveryBaseActivity::class.java)
                if (patientId?.isNotBlank() == true) {
                    intent.putExtra(PatientId, patientId)
                }
                startActivity(intent)
            }

            getString(R.string.emtct) -> {
            }
            getString(R.string.yes) -> {
                if (viewModel.isEMTCT) {
                    val patientId = arguments?.getString(DefinedParams.PatientId, "")
                    val id = arguments?.getString(DefinedParams.ID, "")
                    val memberId = arguments?.getString(MEMBER_ID, "")
                    val intent =
                        Intent(requireContext(), MotherNeonateEMTCTActivity::class.java)
                    if (patientId?.isNotBlank() == true) {
                        intent.putExtra(PatientId, patientId)
                        intent.putExtra(ID, id)
                        intent.putExtra(MEMBER_ID, memberId)
                        intent.putExtra(EMTCT, true)
                        intent.putExtra(PatientId, patientId)
                        intent.putExtra(ID, id)
                        intent.putExtra(EMTCT, true)
                    }
                    startActivity(intent)
                } else {
                    val patientId = arguments?.getString(PatientId, "")
                    val id = arguments?.getString(ID, "")
                    val intent =
                        Intent(requireContext(), HivImrAndCmrActivity::class.java)
                    if (patientId?.isNotBlank() == true) {
                        intent.putExtra(PatientId, patientId)
                        intent.putExtra(ID, id)
                    }
                    startActivity(intent)
                }
            }
            getString(R.string.no) -> {
                val isHiv = arguments?.getBoolean(HIV, false)
                if (isHiv == true) {
                    val intent = Intent(requireContext(), ConsentFormActivity::class.java)
                    intent.putExtra(PatientId, arguments?.getString(PatientId))
                    intent.putExtra(HIV, arguments?.getBoolean(HIV, false))
                    intent.putExtra(ID, arguments?.getString(ID))
                    intent.putExtra(villageId, arguments?.getString(villageId))
                    startActivity(intent)
//
                } else if (viewModel.isEMTCT) {
                    val intent = Intent(requireContext(), HivMedicalReviewBaseActivity::class.java).apply {
                        putExtra(PatientId, arguments?.getString(PatientId))
                        putExtra(HIV, arguments?.getBoolean(HIV, false))
                        putExtra(EMTCT, true)
                        putExtra(ID, arguments?.getString(ID))
                        putExtra(MEMBER_ID, arguments?.getString(MEMBER_ID))
                        putExtra(villageId, arguments?.getString(villageId))
                    }
                    startActivity(intent)
                } else {
                }
            }
        }
    }

    private fun initView() {
        viewModel.setUserJourney(AnalyticsDefinedParams.SELECTFLOWDIALOGUE)
        viewModel.resultANCFlowHashMap.clear()
        var isHIV = arguments?.getBoolean(DefinedParams.HIV, false)

        if (isHIV == true) {
            binding.tvTitle.text = getString(R.string.select_patient_type)
            binding.tvSubTitle.visible()
            getPositiveType().let {
                val view = SingleSelectionCustomView(requireContext())
                view.tag = TAG
                view.addViewElements(
                    it,
                    SecuredPreference.getIsTranslationEnabled(),
                    viewModel.resultANCFlowHashMap,
                    Pair(TAG, null),
                    FormLayout(viewType = "", id = "", title = "", visibility = "", optionsList = null),
                    singleSelectionCallback,
                )
                binding.selectionGroup.addView(view)
            }
        } else {
            getRMNCHFlowData().let {
                val view = SingleSelectionCustomView(requireContext())
                view.tag = TAG
                view.addViewElements(
                    it,
                    SecuredPreference.getIsTranslationEnabled(),
                    viewModel.resultANCFlowHashMap,
                    Pair(TAG, null),
                    FormLayout(viewType = "", id = "", title = "", visibility = "", optionsList = null),
                    singleSelectionCallback,
                )
                binding.selectionGroup.addView(view)
            }
        }
    }

    private fun setListener() {
        binding.ivClose.setOnClickListener(this)
    }

    private fun getRMNCHFlowData(): ArrayList<Map<String, Any>> {
        val flowList = ArrayList<Map<String, Any>>()
        flowList.add(CommonUtils.getOptionMap(getString(R.string.anc), getString(R.string.anc)))
        flowList.add(
            CommonUtils.getOptionMap(
                getString(R.string.labour_delivery),
                getString(R.string.labour_delivery),
            ),
        )
        flowList.add(CommonUtils.getOptionMap(getString(R.string.pnc), getString(R.string.pnc)))
        val id = arguments?.getString(DefinedParams.CHILD_PATIENT_ID, null)
        val dateOfDelivery = arguments?.getString(DefinedParams.DateOfDelivery, null)
//        if (!id.isNullOrEmpty()) {
//        }
//        if (dateOfDelivery ==null){

//        }
        // Enable the labour Delivery based on the delivery date
//        else if(enableLabourBasedOnDate(dateOfDelivery)){
//            flowList.add(
//                CommonUtils.getOptionMap(
//                    getString(R.string.labour_delivery),
//                    getString(R.string.labour_delivery)
//                )
//            )
//        }
        return flowList
    }

    private fun getPositiveType(): ArrayList<Map<String, Any>> {
        val flowList = ArrayList<Map<String, Any>>()
        flowList.add(CommonUtils.getOptionMap(getString(R.string.yes), getString(R.string.yes)))
        flowList.add(
            CommonUtils.getOptionMap(
                getString(R.string.no),
                getString(R.string.no),
            ),
        )
        return flowList
    }

    private fun enableLabourBasedOnDate(dateString: String): Boolean {
        if (dateString.isEmpty()) {
            return true
        } else {
            // Define the date format
            val dateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

            // Parse the date string to LocalDateTime
            val dateTime = LocalDateTime.parse(dateString, dateTimeFormatter)

            // Get the current date and time in UTC
            val currentDateTime = LocalDateTime.now(ZoneOffset.UTC)

            // Calculate the date and time 60 days from now
            val dateTimePlus60Days = currentDateTime.plus(60, ChronoUnit.DAYS)

            // Compare the parsed date with the current date + 60 days
            return dateTime.isAfter(dateTimePlus60Days)
        }
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            binding.ivClose.id -> {
                binding.tvSubTitle.invisible()
                dismiss()
            }
        }
    }

    private fun selectType() {
        val isHiv = arguments?.getBoolean(HIV, false)
        if (isHiv == true || viewModel.isEMTCT) {
            binding.tvTitle.text = getString(R.string.select_patient_type)
            binding.tvSubTitle.visible()
        }
    }
}
