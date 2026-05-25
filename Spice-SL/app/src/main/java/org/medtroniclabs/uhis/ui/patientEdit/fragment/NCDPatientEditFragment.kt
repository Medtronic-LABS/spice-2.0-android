package org.medtroniclabs.uhis.ui.patientEdit.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.FormAutofill
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.model.RecommendedDosageListModel
import org.medtroniclabs.uhis.data.offlinesync.model.ProvanceDto
import org.medtroniclabs.uhis.databinding.FragmentNcdPatientEditBinding
import org.medtroniclabs.uhis.formgeneration.FormGenerator
import org.medtroniclabs.uhis.formgeneration.config.ViewType
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.listener.FormEventListener
import org.medtroniclabs.uhis.formgeneration.model.FormLayout
import org.medtroniclabs.uhis.formgeneration.utility.CheckBoxDialog
import org.medtroniclabs.uhis.ncd.medicalreview.viewmodel.NCDFormViewModel
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.MenuConstants
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams
import org.medtroniclabs.uhis.ui.dialog.GeneralSuccessDialog
import org.medtroniclabs.uhis.ui.mypatients.viewmodel.PatientDetailViewModel
import org.medtroniclabs.uhis.ui.patientEdit.NCDPatientEditActivity
import org.medtroniclabs.uhis.ui.patientEdit.viewModel.NCDPatientEditViewModel

@AndroidEntryPoint
class NCDPatientEditFragment : BaseFragment(), FormEventListener, View.OnClickListener {
    lateinit var binding: FragmentNcdPatientEditBinding

    private val viewModel: NCDPatientEditViewModel by activityViewModels()
    private val patientViewModel: PatientDetailViewModel by activityViewModels()
    private val ncdFormViewModel: NCDFormViewModel by activityViewModels()
    private lateinit var formGenerator: FormGenerator

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentNcdPatientEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initializeFormBuilder()
        attachObserver()
        setListener()
    }

    private fun setListener() {
        binding.actionButton.safeClickListener(this)
    }

    private fun initializeFormBuilder() {
        formGenerator = FormGenerator(
            requireContext(),
            binding.llForm,
            this,
            binding.scrollView,
            translate = SecuredPreference.getIsTranslationEnabled(),
        )
        ncdFormViewModel.getNCDForm(MenuConstants.REGISTRATION.lowercase())
    }

    private fun attachObserver() {
        ncdFormViewModel.ncdFormResponse.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }

                ResourceState.SUCCESS -> {
                    hideProgress()
                    resourceState.data?.let { list ->
                        val data =
                            list.filter { it.viewType != ViewType.VIEW_TYPE_FORM_CARD_FAMILY && it.isEditable }
                        binding.actionButton.visibility = View.VISIBLE
                        binding.actionButton.isEnabled = data.isNotEmpty()
                        formGenerator.populateEditableViews(list)
                    }
                }
            }
        }

        patientViewModel.patientDetailsLiveData.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }

                ResourceState.SUCCESS -> {
                    hideProgress()
                    resourceState.data?.let {
                        FormAutofill.start(requireContext(), formGenerator, it)
                    }
                }
            }
        }
        viewModel.updatePatientMap.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.ERROR -> {
                    hideProgress()
                    resourceState.message?.let {
                        showErrorDialog(
                            getString(R.string.error),
                            it,
                        )
                    }
                }

                ResourceState.SUCCESS -> {
                    hideProgress()
                    showSuccessDialog()
                }
            }
        }
    }

    private fun showSuccessDialog() {
        GeneralSuccessDialog
            .newInstance(
                getString(R.string.patient_details),
                getString(R.string.patient_detail_updated_successfully),
                okayButton = getString(R.string.done),
                callback = {
                    if (activity is NCDPatientEditActivity) {
                        activity?.apply {
                            setResult(Activity.RESULT_OK, intent)
                            finish()
                        }
                    } else {
                        (activity as BaseActivity).redirectToHome()
                    }
                },
            ).show(childFragmentManager, GeneralSuccessDialog.TAG)
    }

    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                data?.let { resultIntent ->
                }
            }
        }

    override fun onRenderingComplete() {
        /**
         * this method is not used
         */
    }

    override fun onUpdateInstruction(
        id: String,
        selectedId: Any?,
    ) {
        /**
         * this method is not used
         */
    }

    override fun onInformationHandling(
        id: String,
        noOfDays: Int,
        enteredDays: Int?,
        resultMap: HashMap<String, Any>?,
    ) {
        /**
         * this method is not used
         */
    }

    override fun onAgeCheckForPregnancy() {
        /**
         * this method is not used
         */
    }

    override fun handleMandatoryCondition(formLayout: FormLayout?) {
        /**
         * this method is not used
         */
    }

    override fun onAgeUpdateListener(
        age: Int,
        serverData: List<FormLayout>?,
        resultHashMap: HashMap<String, Any>,
    ) {
        /**
         * this method is not used
         */
    }

    override fun onQRScanRequested() {
    }

    override fun onPopulate(targetId: String) {
        /**
         * this method is not used
         */
    }

    override fun loadLocalCache(
        id: String,
        localDataCache: Any,
        selectedParent: Long?,
    ) {
        if (localDataCache is String) {
        }
    }

    override fun onCheckBoxDialogueClicked(
        id: String,
        formLayout: FormLayout,
        resultMap: Any?,
    ) {
        CheckBoxDialog
            .newInstance(id, resultMap) { map ->
                formGenerator.validateCheckboxDialogue(id, formLayout, map)
            }.show(childFragmentManager, CheckBoxDialog.TAG)
    }

    override fun onInstructionClicked(
        id: String,
        title: String,
        informationList: ArrayList<String>?,
        description: String?,
        dosageListModel: ArrayList<RecommendedDosageListModel>?,
    ) {
        /**
         * this method is not used
         */
    }

    override fun onFormSubmit(
        resultMap: HashMap<String, Any>?,
        serverData: List<FormLayout>?,
    ) {
        val map = HashMap<String, Any>()
        if (resultMap != null) {
            map[DefinedParams.BioData] = resultMap as HashMap<String, Any>
        }
        map[DefinedParams.HealthFacilityFhirId] = SecuredPreference.getOrganizationFhirId()
        map[AssessmentDefinedParams.memberReference] =
            patientViewModel.getPatientFHIRId().toString()
        map[AssessmentDefinedParams.patientReference] = patientViewModel.getPatientId().toString()
        map[DefinedParams.Provenance] = ProvanceDto()
        if (connectivityManager.isNetworkAvailable()) {
            viewModel.ncdUpdatePatientDetail(map)
        } else {
            (activity as? BaseActivity)?.showErrorDialogue(
                getString(R.string.error),
                getString(R.string.no_internet_error),
                isNegativeButtonNeed = false,
            ) {}
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.actionButton.id -> {
                formGenerator.formSubmitAction(view)
            }
        }
    }

    companion object {
        const val TAG = "NCDPatientEditFragment"
    }
}
