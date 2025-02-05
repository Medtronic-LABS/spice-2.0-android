package com.medtroniclabs.opensource.ui.mypatients.fragment

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.setWidth
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.data.ReferPatientHealthFacilityItem
import com.medtroniclabs.opensource.data.ReferPatientNameNumber
import com.medtroniclabs.opensource.databinding.FragmentReferPatientBinding
import com.medtroniclabs.opensource.formgeneration.extension.markMandatory
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.formgeneration.utility.CustomSpinnerAdapter
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.BaseDialogFragment
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewTypeEnums
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.PatientDetailViewModel
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.ReferPatientViewModel

class ReferPatientFragment : BaseDialogFragment(), View.OnClickListener {

    private lateinit var binding: FragmentReferPatientBinding
    private val viewModel: ReferPatientViewModel by activityViewModels()
    private val patientViewModel: PatientDetailViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentReferPatientBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        isCancelable = false
        return binding.root
    }

    companion object {
        const val TAG = "ReferPatientFragment"
        fun newInstance(name: String, patientReference: String?, encounterId: String?): ReferPatientFragment {
            val fragment = ReferPatientFragment()
            val bundle = Bundle()
            bundle.putString(DefinedParams.NAME, name)
            bundle.putString(DefinedParams.PatientReference, patientReference)
            bundle.putString(DefinedParams.EncounterId, encounterId)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        setListener()
        attachObserver()
    }

    private fun initViews() {
        binding.tvNameNumberLabel.markMandatory()
        binding.tvReferToLabel.markMandatory()
        binding.tvReferredReasonLabel.markMandatory()
        initializeNameNumberAdapter(null)
        withNetworkAvailability(online = {
            setHealthFacilityDistrictId(SecuredPreference.getDistrictId())
            viewModel.getHealthFacilityMetaData(SecuredPreference.getDistrictId().toString())
        }, ::finishFragment)

    }

    private fun attachObserver() {
       /* viewModel.defaultHealthFacilityLiveData.observe(viewLifecycleOwner) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    showLoadingProgress()
                }

                ResourceState.SUCCESS -> {
                    hideLoadingProgress()
                    resource.data?.let {
                        setHealthFacilityDistrictId(it)
                    }
                }

                ResourceState.ERROR -> {
                    hideLoadingProgress()
                }
            }
        }*/
        viewModel.healthFacilityLiveData.observe(viewLifecycleOwner) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    showLoadingProgress()
                }

                ResourceState.SUCCESS -> {
                    hideLoadingProgress()
                    resource.data?.let { listItems ->
                        initializeReferTo(listItems)
                    }
                }

                ResourceState.ERROR -> {
                    hideLoadingProgress()
                }
            }
        }
        viewModel.nameNumberListLiveData.observe(viewLifecycleOwner) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    showLoadingProgress()
                }

                ResourceState.SUCCESS -> {
                    hideLoadingProgress()
                    resource.data?.let { listItems ->
                        initializeNameNumberAdapter(listItems)
                    } ?: kotlin.run {
                    }
                }

                ResourceState.ERROR -> {
                    hideLoadingProgress()
                }
            }
        }

        viewModel.referPatientResultLiveData.observe(this) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    showLoadingProgress()
                }

                ResourceState.SUCCESS -> {
                    hideLoadingProgress()
                }

                ResourceState.ERROR -> {
                    hideLoadingProgress()
                }
            }
        }
    }

    private fun setHealthFacilityDistrictId(districtId: Long) {
        viewModel.getHealthFacilityMetaData(districtId.toString())
    }

    private fun initializeNameNumberAdapter(listItems: List<ReferPatientNameNumber>?) {
        val list = arrayListOf<Map<String, Any>>()
        list.add(
            hashMapOf<String, Any>(
                DefinedParams.NAME to DefinedParams.DefaultIDLabel,
                DefinedParams.ID to DefinedParams.DefaultID
            )
        )
        listItems?.forEach {
            it.roles.forEach { roles ->
                list.add(
                    hashMapOf<String, Any>(
                        DefinedParams.NAME to requireContext().getString(R.string.refer_patient_name_num_display, it.firstName, roles.displayName, SecuredPreference.getPhoneNumberCode(), it.phoneNumber),
                        DefinedParams.ID to it.id,
                        DefinedParams.FhirId to it.fhirId
                    )
                )
            }
        }

        val nameAdapter = CustomSpinnerAdapter(requireContext())
        nameAdapter.setData(list)
        binding.etNameNumber.adapter = nameAdapter
        binding.etNameNumber.setSelection(0,false)
        binding.etNameNumber.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedValue = nameAdapter.getData(position)
                selectedValue?.let {
                    viewModel.clinicalSelectedId = it[DefinedParams.FhirId] as? String
                    isEnableRefer()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // method not in use
            }
        }
    }

    private fun initializeReferTo(listItems: List<ReferPatientHealthFacilityItem>) {
        val list = arrayListOf<Map<String, Any>>()
        list.add(
            hashMapOf<String, Any>(
                DefinedParams.NAME to DefinedParams.DefaultIDLabel,
                DefinedParams.ID to DefinedParams.DefaultID
            )
        )
        listItems.forEach {
            list.add(
                hashMapOf<String, Any>(
                    DefinedParams.NAME to it.name,
                    DefinedParams.TenantId to it.tenantId,
                    DefinedParams.ID to it.id,
                    DefinedParams.FhirId to it.fhirId
                )
            )
        }
        referToListListener(list)
    }
    private fun referToListListener(list: ArrayList<Map<String, Any>>) {
        val referToAdapter = CustomSpinnerAdapter(requireContext())
        referToAdapter.setData(list)
        binding.etReferTo.adapter = referToAdapter
        binding.etReferTo.setSelection(0,false)
        binding.etReferTo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedValue = referToAdapter.getData(position)
                selectedValue?.let {
                    val selectedTenantId = it[DefinedParams.TenantId] as? String
                    val selectedFHIRId = it[DefinedParams.FhirId] as? String
                    val selectedId = it[DefinedParams.ID] as? String
                    if (selectedId != DefinedParams.DefaultID) {
                        selectedTenantId?.let { tenantId ->
                            viewModel.referToSelectedId = selectedFHIRId
                            isEnableRefer()
                            viewModel.getNameNumberFieldList(tenantId)
                        }
                    } else {
                        initializeNameNumberAdapter(null)
                        viewModel.referToSelectedId = null
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // method not in use
            }
        }
    }


    private fun setListener() {
        binding.btnCancel.safeClickListener(this)
        binding.ivClose.safeClickListener(this)
        binding.btnRefer.safeClickListener(this)
        binding.etRefferedReason.doAfterTextChanged { input ->
            input?.let {
                viewModel.enteredReferredReason =
                    if (it.trim().isNotEmpty()) it.trim().toString() else null
            }
            isEnableRefer()
        }
    }

    override fun onStart() {
        super.onStart()
        handleDialogSize()
    }

    private fun handleDialogSize() {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val width = if (CommonUtils.checkIsTablet(requireContext())) {
            if (isLandscape) 50 else 90
        } else {
            if (isLandscape) 50 else 90
        }
        setWidth(width)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.btnCancel.id, binding.ivClose.id -> dismiss()
            binding.btnRefer.id -> {
                postResultInput()
            }
        }
    }
    private fun postResultInput() {
        val assessmentName: String? = arguments?.getString(DefinedParams.NAME, "")
        val patientReference: String? = arguments?.getString(DefinedParams.PatientReference, "")
        val encounterId: String? = arguments?.getString(DefinedParams.EncounterId, "")
        val referralTicketType: String = when (assessmentName) {
            MedicalReviewTypeEnums.ABOVE_FIVE_YEARS.name, MedicalReviewTypeEnums.UNDER_FIVE_YEARS.name, MedicalReviewTypeEnums.UNDER_TWO_MONTHS.name -> DefinedParams.ICCM
            MedicalReviewTypeEnums.ANC_REVIEW.name, MedicalReviewTypeEnums.PNC_MOTHER_REVIEW.name, MedicalReviewTypeEnums.MOTHER_DELIVERY_REVIEW.name -> DefinedParams.RMNCH
            else -> {
                ""
            }
        }
        if (connectivityManager.isNetworkAvailable()){
            patientViewModel.patientDetailsLiveData.value?.data?.let { patientDetails ->
                viewModel.createReferPatientResult(
                    patientReference,
                    encounterId,
                    Pair(assessmentName, referralTicketType),
                    patientDetails.patientId,
                    patientDetails.houseHoldId,
                    patientDetails.villageId,
                    patientDetails.memberId
                )
            }
        } else {
            (activity as BaseActivity?)?.showErrorDialogue(
                getString(R.string.error),
                getString(R.string.no_internet_error),
                isNegativeButtonNeed = false
            ) {

            }
        }
    }
    private fun isEnableRefer() {
        binding.btnRefer.isEnabled = viewModel.referToSelectedId != null && viewModel.clinicalSelectedId != null && viewModel.enteredReferredReason!=null
    }

    private fun showLoadingProgress() {
        binding.loadingProgress.visible()
    }

    private fun hideLoadingProgress() {
       binding.loadingProgress.gone()
    }
}