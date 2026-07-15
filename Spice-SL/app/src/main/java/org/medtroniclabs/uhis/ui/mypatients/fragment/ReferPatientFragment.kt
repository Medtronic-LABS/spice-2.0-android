package org.medtroniclabs.uhis.ui.mypatients.fragment

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.setWidth
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.ReferPatientHealthFacilityItem
import org.medtroniclabs.uhis.data.ReferPatientNameNumber
import org.medtroniclabs.uhis.databinding.FragmentReferPatientBinding
import org.medtroniclabs.uhis.formgeneration.extension.markMandatory
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.utility.CustomSpinnerAdapter
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.BaseDialogFragment
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewTypeEnums
import org.medtroniclabs.uhis.ui.mypatients.viewmodel.PatientDetailViewModel
import org.medtroniclabs.uhis.ui.mypatients.viewmodel.ReferPatientViewModel

class ReferPatientFragment : BaseDialogFragment(), View.OnClickListener {
    private lateinit var binding: FragmentReferPatientBinding
    private val viewModel: ReferPatientViewModel by activityViewModels()
    private val patientViewModel: PatientDetailViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentReferPatientBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        isCancelable = false
        return binding.root
    }

    companion object {
        const val TAG = "ReferPatientFragment"

        fun newInstance(
            name: String,
            patientReference: String?,
            encounterId: String?,
        ): ReferPatientFragment {
            val fragment = ReferPatientFragment()
            val bundle = Bundle()
            bundle.putString(DefinedParams.NAME, name)
            bundle.putString(DefinedParams.PatientReference, patientReference)
            bundle.putString(DefinedParams.ENCOUNTER_ID, encounterId)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        setListener()
        attachObserver()
    }

    private fun initViews() {
        patientViewModel.setUserJourney(AnalyticsDefinedParams.REFERPATIENTDIALOGUE)
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
                DefinedParams.NAME to DefinedParams.DEFAULT_ID_LABEL,
                DefinedParams.ID to DefinedParams.DEFAULT_ID,
            ),
        )
        listItems?.forEach {
            it.roles.forEach { roles ->
                list.add(
                    hashMapOf<String, Any>(
                        DefinedParams.NAME to
                            requireContext().getString(
                                R.string.refer_patient_name_num_display,
                                it.firstName,
                                roles.displayName,
                                SecuredPreference.getPhoneNumberCode(),
                                it.phoneNumber,
                            ),
                        DefinedParams.ID to it.id,
                        DefinedParams.FhirId to it.fhirId,
                    ),
                )
            }
        }

        val nameAdapter = CustomSpinnerAdapter(requireContext())
        nameAdapter.setData(list)
        binding.etNameNumber.adapter = nameAdapter
        binding.etNameNumber.setSelection(0, false)
        binding.etNameNumber.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long,
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
                DefinedParams.NAME to DefinedParams.DEFAULT_ID_LABEL,
                DefinedParams.ID to DefinedParams.DEFAULT_ID,
            ),
        )
        listItems.forEach {
            list.add(
                hashMapOf<String, Any>(
                    DefinedParams.NAME to it.name,
                    DefinedParams.TENANT_ID to it.tenantId,
                    DefinedParams.ID to it.id,
                    DefinedParams.FhirId to it.fhirId,
                ),
            )
        }
        referToListListener(list)
    }

    private fun referToListListener(list: ArrayList<Map<String, Any>>) {
        val referToAdapter = CustomSpinnerAdapter(requireContext())
        referToAdapter.setData(list)
        binding.etReferTo.adapter = referToAdapter
        binding.etReferTo.setSelection(0, false)
        binding.etReferTo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long,
            ) {
                val selectedValue = referToAdapter.getData(position)
                selectedValue?.let {
                    val selectedTenantId = it[DefinedParams.TENANT_ID] as? String
                    val selectedFHIRId = it[DefinedParams.FhirId] as? String
                    val selectedId = it[DefinedParams.ID] as? String
                    if (selectedId != DefinedParams.DEFAULT_ID) {
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
        val encounterId: String? = arguments?.getString(DefinedParams.ENCOUNTER_ID, "")
        val referralTicketType: String = when (assessmentName) {
            MedicalReviewTypeEnums.ABOVE_FIVE_YEARS.name, MedicalReviewTypeEnums.UNDER_FIVE_YEARS.name, MedicalReviewTypeEnums.UNDER_TWO_MONTHS.name -> DefinedParams.ICCM
            MedicalReviewTypeEnums.ANC_REVIEW.name, MedicalReviewTypeEnums.PNC_MOTHER_REVIEW.name, MedicalReviewTypeEnums.MOTHER_DELIVERY_REVIEW.name -> DefinedParams.RMNCH
            MedicalReviewTypeEnums.TB.name -> {
                MedicalReviewTypeEnums.TB.name
            }
            MedicalReviewTypeEnums.FAMILY_PLANNING_REVIEW.name -> {
                MedicalReviewTypeEnums.FAMILY_PLANNING.name
            }
            MedicalReviewTypeEnums.HIV.name -> {
                MedicalReviewTypeEnums.HIV.name
            }
            else -> {
                ""
            }
        }
        if (connectivityManager.isNetworkAvailable()) {
            patientViewModel.patientDetailsLiveData.value?.data?.let { patientDetails ->
                viewModel.createReferPatientResult(
                    patientReference,
                    encounterId,
                    Pair(assessmentName, referralTicketType),
                    patientDetails.patientId,
                    patientDetails.houseHoldId,
                    patientDetails.villageId,
                    patientDetails.memberId,
                    tbIMRCompleted = patientViewModel.getTbMedicalReviewStatus(),
                )
            }
        } else {
            (activity as BaseActivity?)?.showErrorDialogue(
                getString(R.string.error),
                getString(R.string.no_internet_error),
                isNegativeButtonNeed = false,
            ) {
            }
        }
    }

    private fun isEnableRefer() {
        binding.btnRefer.isEnabled = viewModel.referToSelectedId != null && viewModel.clinicalSelectedId != null && viewModel.enteredReferredReason != null
    }

    private fun showLoadingProgress() {
        binding.loadingProgress.visible()
    }

    private fun hideLoadingProgress() {
        binding.loadingProgress.gone()
    }
}
