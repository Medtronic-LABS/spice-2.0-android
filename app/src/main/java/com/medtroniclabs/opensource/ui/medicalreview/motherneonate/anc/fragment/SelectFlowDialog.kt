package com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.fragment

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.setWidth
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.databinding.FragmentSelectFlowDialogBinding
import com.medtroniclabs.opensource.formgeneration.model.FormLayout
import com.medtroniclabs.opensource.formgeneration.ui.SingleSelectionCustomView
import com.medtroniclabs.opensource.network.utils.ConnectivityManager
import com.medtroniclabs.opensource.ui.home.ToolsViewModel
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.labourdelivery.activity.LabourDeliveryBaseActivity
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.activity.MotherNeonateANCActivity
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.pnc.activity.MotherNeonatePncActivity
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewDefinedParams
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.PatientDetailViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@AndroidEntryPoint
class SelectFlowDialog : DialogFragment(), View.OnClickListener {

    private lateinit var binding: FragmentSelectFlowDialogBinding
    private val viewModel: ToolsViewModel by activityViewModels()
    private val patientDetailViewModel :PatientDetailViewModel by activityViewModels()
    @Inject
    lateinit var connectivityManager: ConnectivityManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSelectFlowDialogBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        isCancelable = false
        return binding.root
    }

    companion object {
        const val TAG = "SelectFlowDialog"
        fun newInstance(): SelectFlowDialog {
            return SelectFlowDialog()
        }

        fun newInstance(patientId: String?, id: String?,childPatientId:String?,dateOfDelivery:String?,neonateOutcome:String?): SelectFlowDialog {
            val fragment = SelectFlowDialog()
            val bundle = Bundle()
            bundle.putString(DefinedParams.PatientId, patientId)
            bundle.putString(DefinedParams.ID, id)
            bundle.putString(DefinedParams.ChildPatientId,childPatientId)
            bundle.putString(DefinedParams.DateOfDelivery,dateOfDelivery)
            bundle.putString(DefinedParams.NeonateOutcome,neonateOutcome)
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


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        setListener()
    }

    private var singleSelectionCallback: ((selectedID: Any?, elementId: Pair<String, String?>, serverViewModel: FormLayout, name: String?) -> Unit)? =
        { selectedID, _, _, _ ->
            viewModel.resultANCFlowHashMap[TAG] = selectedID as String
            if (connectivityManager.isNetworkAvailable()) {
                launchActivity()
            }
            dismiss()
        }

    private fun launchActivity() {
        when (viewModel.resultANCFlowHashMap[TAG]) {
            getString(R.string.anc) -> {
                val patientId = arguments?.getString(DefinedParams.PatientId, "")
                val id = arguments?.getString(DefinedParams.ID, "")
                val intent = Intent(requireContext(), MotherNeonateANCActivity::class.java)
                if (patientId?.isNotBlank() == true) {
                    intent.putExtra(DefinedParams.PatientId, patientId)
                    intent.putExtra(DefinedParams.ID, id)
                }
                startActivity(intent)
            }

            getString(R.string.pnc) -> {
                val patientId = arguments?.getString(DefinedParams.PatientId, "")
                val id = arguments?.getString(DefinedParams.ID, "")
                val childId = arguments?.getString(DefinedParams.ChildPatientId, null)
                val  neonateOutcome = arguments?.getString(DefinedParams.NeonateOutcome, null)
                val targetActivity = if (!childId.isNullOrEmpty()) MotherNeonatePncActivity::class.java else{
                    if (neonateOutcome== MedicalReviewDefinedParams.MaceratedStillBirth){
                        MotherNeonatePncActivity::class.java
                    }else {
                        LabourDeliveryBaseActivity::class.java
                    }
                }
                val intent = Intent(requireContext(), targetActivity).apply {
                    if (!patientId.isNullOrBlank()) {
                        putExtra(DefinedParams.PatientId, patientId)
                        putExtra(DefinedParams.ID, id)
                    }
                    if (targetActivity == LabourDeliveryBaseActivity::class.java) {
                        putExtra(DefinedParams.DirectPNCFlow, true)
                    }
                }
                startActivity(intent)
            }

            getString(R.string.child_hood_visit) -> {

            }
            getString(R.string.labour_delivery) -> {
                val patientId = arguments?.getString(DefinedParams.PatientId, "")
                val intent = Intent(requireContext(), LabourDeliveryBaseActivity::class.java)
                if (patientId?.isNotBlank() == true) {
                    intent.putExtra(DefinedParams.PatientId, patientId)
                }
                startActivity(intent)
            }
        }
    }


    private fun initView() {
        viewModel.resultANCFlowHashMap.clear()
        getRMNCHFlowData().let {
            val view = SingleSelectionCustomView(requireContext())
            view.tag = TAG
            view.addViewElements(
                it,
                SecuredPreference.getIsTranslationEnabled(),
                viewModel.resultANCFlowHashMap,
                Pair(TAG, null),
                FormLayout(viewType = "", id = "", title = "", visibility = "", optionsList = null),
                singleSelectionCallback
            )
            binding.selectionGroup.addView(view)
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
                getString(R.string.labour_delivery)
            )
        )
        flowList.add(CommonUtils.getOptionMap(getString(R.string.pnc), getString(R.string.pnc)))
        val id = arguments?.getString(DefinedParams.ChildPatientId, null)
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
    private fun enableLabourBasedOnDate(dateString: String): Boolean {
        if(dateString.isEmpty()){
            return true
        }else {
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
                dismiss()
            }
        }
    }
}