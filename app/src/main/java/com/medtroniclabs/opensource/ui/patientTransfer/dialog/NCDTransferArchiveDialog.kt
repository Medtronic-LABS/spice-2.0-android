package com.medtroniclabs.opensource.ui.patientTransfer.dialog


import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.hideKeyboard
import com.medtroniclabs.opensource.appextensions.setError
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.databinding.DialogTransferArchiveBinding
import com.medtroniclabs.opensource.formgeneration.extension.markMandatory
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.ncd.data.NCDRegionSiteModel
import com.medtroniclabs.opensource.ncd.data.NCDSiteRoleModel
import com.medtroniclabs.opensource.ncd.data.NCDSiteRoleResponse
import com.medtroniclabs.opensource.ncd.data.NCDTransferCreateRequest
import com.medtroniclabs.opensource.ncd.data.RegionSiteResponse
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.network.utils.ConnectivityManager
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.dialog.GeneralSuccessDialog
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.PatientDetailViewModel
import com.medtroniclabs.opensource.ui.patientTransfer.adapter.NCDRoleUserAutoCompleteAdapter
import com.medtroniclabs.opensource.ui.patientTransfer.adapter.NCDSiteAutoCompleteAdapter
import com.medtroniclabs.opensource.ui.patientTransfer.viewModel.NCDPatientTransferViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NCDTransferArchiveDialog : DialogFragment(), View.OnClickListener {

    private lateinit var binding: DialogTransferArchiveBinding
    private val patientTransferViewModel: NCDPatientTransferViewModel by activityViewModels()
    private val detailsViewModel: PatientDetailViewModel by activityViewModels()

    @Inject
    lateinit var connectivityManager: ConnectivityManager

    private lateinit var myAdapter: NCDSiteAutoCompleteAdapter
    private lateinit var userAdapter: NCDRoleUserAutoCompleteAdapter
    private var canSearch: Boolean = true
    private var userSearch: Boolean = true
    private var transferReason: String? = null
    private var selectedSite: RegionSiteResponse? = null
    private var selectedUser: NCDSiteRoleResponse? = null

    companion object {
        const val TAG = "NCDTransferArchiveDialog"
        fun newInstance(): NCDTransferArchiveDialog {
            return NCDTransferArchiveDialog()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogTransferArchiveBinding.inflate(inflater, container, false)
        val window: Window? = dialog?.window
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        window?.attributes?.apply {
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            gravity = Gravity.CENTER
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window?.setDecorFitsSystemWindows(false)
        }
        binding.root.setOnApplyWindowInsetsListener { _, windowInsets ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val imeHeight = windowInsets.getInsets(WindowInsets.Type.ime()).bottom
                binding.root.setPadding(0, 0, 0, imeHeight)
                windowInsets.getInsets(WindowInsets.Type.ime() or WindowInsets.Type.systemGestures())
            }
            windowInsets
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = false
        initializeView()
        setListeners()
        attachObserver()
    }

    private fun initializeView() {
        detailsViewModel.patientDetailsLiveData.value?.data?.let { details ->
            binding.labelHeader.titleView.text =
                CommonUtils.capitalize("${getString(R.string.transfer)} ${details.firstName} ${details.lastName}${"?"}")
            binding.tvReasonHeader.text = CommonUtils.capitalize(
                "${getString(R.string.reason_to_transfer)} ${details.firstName} ${details.lastName}"
            )
        }
        binding.tvFacility.markMandatory()
        binding.tvPhysician.markMandatory()
        binding.tvReasonHeader.markMandatory()
        myAdapter = NCDSiteAutoCompleteAdapter(requireContext())
        userAdapter = NCDRoleUserAutoCompleteAdapter(requireContext())
    }

    private fun showLoading() {
        binding.loadingProgress.visible()
    }

    private fun hideLoading() {
        binding.loadingProgress.gone()
    }

    private fun setListeners() {
        binding.labelHeader.ivClose.safeClickListener(this)
        binding.btnCancel.safeClickListener(this)
        binding.btnTransfer.safeClickListener(this)

        binding.etFacility.setOnItemClickListener { _, _, index, _ ->
            canSearch = false
            selectedSite = null
            patientTransferViewModel.searchSiteResponse.value?.data?.let { site ->
                site.let { list ->
                    val size = list.size
                    if (size > 0 && index < size) {
                        selectedSite = list[index]
                    }
                    binding.etPhysician.isEnabled = true
                }
            }
        }

        binding.etFacility.threshold = 2
        binding.etFacility.addTextChangedListener(textWatcher)

        binding.etReason.addTextChangedListener {
            if (it.isNullOrBlank()) {
                transferReason = null
            } else {
                transferReason = it.toString()
            }
        }

        binding.etPhysician.setOnItemClickListener { _, _, index, _ ->
            userSearch = false
            selectedUser = null
            patientTransferViewModel.searchRoleUserResponse.value?.data?.let { user ->
                user.let { list ->
                    val size = list.size
                    if (size > 0 && index < size) {
                        selectedUser = list[index]
                    }
                }
            }
        }
        binding.etPhysician.threshold = 2
        binding.etPhysician.addTextChangedListener(roleTextWatcher)
    }

    private fun attachObserver() {
        patientTransferViewModel.searchSiteResponse.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    resourceState.data?.let {
                        loadSearchDropDown(it)
                    }
                }

                ResourceState.ERROR -> {
                    hideLoading()
                    resourceState.message?.let {
                        (activity as BaseActivity).showErrorDialogue(
                            getString(R.string.error),
                            it,
                            isNegativeButtonNeed = false
                        ) {}
                    }
                }
            }
        }

        patientTransferViewModel.searchRoleUserResponse.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    resourceState.data?.let { it ->
                        loadUserSearchDropDown(it)
                    }
                }

                ResourceState.ERROR -> {
                    hideLoading()
                    resourceState.message?.let {
                        (activity as BaseActivity).showErrorDialogue(
                            getString(R.string.error),
                            it,
                            isNegativeButtonNeed = false
                        ) {}
                    }

                }
            }
        }

        patientTransferViewModel.patientTransferResponse.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    patientTransferViewModel.searchRoleUserResponse.setError()
                    patientTransferViewModel.searchSiteResponse.setError()
                    resourceState.data?.let {
                        val fragment = childFragmentManager.findFragmentByTag(GeneralSuccessDialog.TAG)
                        if (fragment == null) {
                            GeneralSuccessDialog.newInstance(
                                title = getString(R.string.transfer),
                                message = it,
                                okayButton = getString(R.string.done)
                            ) {
                                patientTransferViewModel.patientTransferResponse.setError()
                                dismiss()
                            }.show(childFragmentManager, GeneralSuccessDialog.TAG)
                        }
                    }
                }

                ResourceState.ERROR -> {
                    resourceState.message?.let { message ->
                        (activity as? BaseActivity?)?.showErrorDialogue(message = message) {}
                    }
                    hideLoading()
                }
            }
        }
    }

    private fun loadSearchDropDown(data: ArrayList<RegionSiteResponse>) {
        if (data.isEmpty())
            binding.etFacility.dismissDropDown()
        myAdapter.setData(data)
        binding.etFacility.setAdapter(myAdapter)
        if (data.size > 0 && canSearch) {
            binding.etFacility.showDropDown()
        }
        if (binding.etFacility.text.isNullOrEmpty()) {
            binding.etFacility.dismissDropDown()
        }
    }

    private fun loadUserSearchDropDown(data: ArrayList<NCDSiteRoleResponse>) {
        userAdapter.setData(data)
        binding.etPhysician.setAdapter(userAdapter)
        if (data.size > 0 && userSearch) {
            binding.etPhysician.showDropDown()
        }
        if (binding.etPhysician.text.isNullOrEmpty()) {
            binding.etPhysician.dismissDropDown()
        }
    }


    override fun onClick(view: View) {
        when (view.id) {
            binding.labelHeader.ivClose.id, R.id.btnCancel -> {
                patientTransferViewModel.searchSiteResponse.setError()
                patientTransferViewModel.searchRoleUserResponse.setError()
                dismiss()
            }

            R.id.btnTransfer -> {
                binding.btnTransfer.context.hideKeyboard(binding.btnTransfer)
                if (validateInputs()) {
                    submitArchive()
                }
            }
        }
    }

    private fun submitArchive() {
        detailsViewModel.patientDetailsLiveData.value?.data?.let {
            val transferRequest = NCDTransferCreateRequest(
                tenantId = SecuredPreference.getTenantId().toString(),
                transferTo = selectedUser?.id,
                transferSite = selectedSite?.id,
                oldSite = SecuredPreference.getOrganizationId(),
                transferReason = transferReason,
                patientReference = it.patientId,
                memberReference = it.id
            )
            patientTransferViewModel.createPatientTransfer(transferRequest)

        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        if (selectedSite == null) {
            isValid = false
            binding.tvFacilityErrorMessage.visible()
        } else {
            binding.tvFacilityErrorMessage.gone()
        }

        if (selectedUser == null) {
            isValid = false
            binding.tvPhysicianErrorMessage.visible()
        } else {
            binding.tvPhysicianErrorMessage.gone()
        }

        if (transferReason == null) {
            isValid = false
            binding.tvReasonErrorMessage.requestFocus()
            binding.tvReasonErrorMessage.visible()
        } else {
            binding.tvReasonErrorMessage.gone()
        }

        return isValid
    }

    private val textWatcher = object : TextWatcher {
        private var lastLength = 0
        override fun beforeTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {
            s?.let { lastLength = it.length }
        }

        override fun onTextChanged(text: CharSequence?, p1: Int, p2: Int, p3: Int) {
            if (canSearch) {
                text?.toString()?.let {
                    if (it.isNotBlank() && it.length > 1) {
                        val request = NCDRegionSiteModel(
                            searchTerm = it,
                            countryId = SecuredPreference.getCountryId(),
                            tenantId = SecuredPreference.getTenantId()
                        )
                        patientTransferViewModel.searchSite(request)

                    }
                }
            } else
                canSearch = true
            clearSelectedSite()
        }

        override fun afterTextChanged(s: Editable?) {
            s?.let {
                if (lastLength > it.length) {
                    binding.etFacility.text = null
                    selectedSite = null
                    binding.etPhysician.text = null
                    binding.etPhysician.isEnabled = false
                    selectedUser = null
                }
            }
        }
    }

    private fun clearSelectedSite() {
        if (selectedSite != null)
            selectedSite = null
    }

    private val roleTextWatcher = object : TextWatcher {
        private var lastLength = 0
        override fun beforeTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {
            s?.let { lastLength = it.length }
        }

        override fun onTextChanged(text: CharSequence?, p1: Int, p2: Int, p3: Int) {
            if (userSearch) {
                text?.toString()?.let {
                    if (it.isNotBlank() && it.length > 1) {
                        selectedSite?.tenantId?.let { site ->
                            val request = NCDSiteRoleModel(tenantId = site, searchTerm = it)
                            patientTransferViewModel.searchRoleUser(request)
                        }
                    }
                }
            } else
                userSearch = true
            clearSelectedProvider()
        }

        override fun afterTextChanged(s: Editable?) {
            s?.let {
                if (lastLength > it.length) {
                    binding.etPhysician.text = null
                    selectedUser = null
                }
            }
        }
    }

    private fun clearSelectedProvider() {
        if (selectedUser != null)
            selectedUser = null
    }
}