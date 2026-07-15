package org.medtroniclabs.uhis.ncd.registration.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.EntityMapper
import org.medtroniclabs.uhis.common.FormAutofill
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.common.StringConverter
import org.medtroniclabs.uhis.data.model.RecommendedDosageListModel
import org.medtroniclabs.uhis.databinding.FragmentRegistrationFormBinding
import org.medtroniclabs.uhis.formgeneration.FormGenerator
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams.DAYS_KEY
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams.MONTH_KEY
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams.WEEK_KEY
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams.YEAR_KEY
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.listener.FormEventListener
import org.medtroniclabs.uhis.formgeneration.model.FormLayout
import org.medtroniclabs.uhis.formgeneration.ui.FormResultComposer
import org.medtroniclabs.uhis.mappingkey.Screening
import org.medtroniclabs.uhis.mappingkey.Screening.BioMetrics
import org.medtroniclabs.uhis.ncd.medicalreview.viewmodel.NCDFormViewModel
import org.medtroniclabs.uhis.ncd.registration.ui.RegistrationActivity
import org.medtroniclabs.uhis.ncd.registration.viewmodel.RegistrationFormViewModel
import org.medtroniclabs.uhis.ncd.screening.ui.DuplicationNudgeDialog
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.MenuConstants
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams
import org.medtroniclabs.uhis.ui.home.AssessmentToolsActivity
import org.medtroniclabs.uhis.ui.mypatients.viewmodel.PatientDetailViewModel

class RegistrationFormFragment : BaseFragment(), View.OnClickListener, FormEventListener {
    private lateinit var binding: FragmentRegistrationFormBinding
    private val viewModel: RegistrationFormViewModel by activityViewModels()
    private val ncdFormViewModel: NCDFormViewModel by activityViewModels()
    private val patientViewModel: PatientDetailViewModel by activityViewModels()
    private lateinit var formGenerator: FormGenerator

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentRegistrationFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        loadJson()
        attachObservers()
    }

    private fun initViews() {
        formGenerator = FormGenerator(
            requireContext(),
            binding.llForm,
            this,
            binding.scrollView,
            translate = SecuredPreference.getIsTranslationEnabled(),
        )

        binding.btnSubmit.safeClickListener(this)
    }

    private fun loadJson() {
        ncdFormViewModel.getNCDForm(MenuConstants.REGISTRATION.lowercase())
    }

    private fun attachObservers() {
        ncdFormViewModel.ncdFormResponse.observe(viewLifecycleOwner) { resources ->
            when (resources.state) {
                ResourceState.LOADING -> {
                    (activity as? BaseActivity)?.showLoading()
                }

                ResourceState.SUCCESS -> {
                    (activity as? BaseActivity)?.hideLoading()
                    resources.data?.let { resData ->
                        val updatedResData = resData.map {
                            if (isGenderOrPregnantField(it) && isPregnantFemale()) {
                                it.copy(enableSingleSelection = false)
                            } else {
                                it
                            }
                        }
                        formGenerator.populateViews(updatedResData)
                        prePopulate()
                    }
                }

                ResourceState.ERROR -> {
                    (activity as? BaseActivity)?.hideLoading()
                }
            }
        }
        viewModel.validatePatientResponseLiveDate.observe(viewLifecycleOwner) { resources ->
            when (resources.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.SUCCESS -> {
                    hideProgress()
                    resources.data?.let { data ->
                        proceedRegistration(data.first, data.second)
                    }
                }

                ResourceState.ERROR -> {
                    hideProgress()
                    if (resources.data is Pair<*, *>) {
                        resources.data.first.let { responseMap ->
                            val dialog =
                                DuplicationNudgeDialog.newInstance(
                                    StringConverter.convertGivenMapToString(
                                        responseMap,
                                    ),
                                    isFromEnrollment = true,
                                ) { doAssessment ->
                                    if (doAssessment) {
                                        proceedAssessment(responseMap)
                                    } else {
                                        viewModel.isFromProceedEnrollment = true
                                        formGenerator.formSubmitAction(binding.btnSubmit)
                                    }
                                }
                            dialog.show(childFragmentManager, DuplicationNudgeDialog.TAG)
                        }
                    } else {
                        (activity as? BaseActivity?)?.showErrorDialogue(
                            title = getString(R.string.error),
                            message = resources.message
                                ?: getString(R.string.something_went_wrong_try_later),
                            positiveButtonName = getString(R.string.ok),
                        ) {}
                    }
                }
            }
        }
        viewModel.countrySpinnerLiveData.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.SUCCESS -> {
                    hideProgress()
                    resourceState.data?.let { data ->
                        formGenerator.spinnerDataInjection(
                            data,
                            EntityMapper.getResultSpinnerMapList(data),
                        )
                    }
                }

                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }
            }
        }
        viewModel.districtSpinnerLiveData.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.SUCCESS -> {
                    hideProgress()
                    resourceState.data?.let { data ->
                        formGenerator.spinnerDataInjection(
                            data,
                            EntityMapper.getResultSpinnerMapList(data),
                        )
                    }
                }

                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }
            }
        }
        viewModel.chiefdomSpinnerLiveData.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.SUCCESS -> {
                    hideProgress()
                    resourceState.data?.let { data ->
                        formGenerator.spinnerDataInjection(
                            data,
                            EntityMapper.getResultSpinnerMapList(data),
                        )
                    }
                }

                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }
            }
        }
        viewModel.villageSpinnerLiveData.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.SUCCESS -> {
                    hideProgress()
                    resourceState.data?.let { data ->
                        formGenerator.spinnerDataInjection(
                            data,
                            EntityMapper.getResultSpinnerMapList(data),
                        )
                    }
                }

                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }
            }
        }
        viewModel.programsSpinnerLiveData.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.SUCCESS -> {
                    hideProgress()
                    resourceState.data?.let { data ->
                        formGenerator.spinnerDataInjection(
                            data,
                            EntityMapper.getResultSpinnerMapList(data),
                        )
                    }
                }

                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }
            }
        }
        viewModel.registrationResponseLiveData.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.SUCCESS -> {
                    hideProgress()
                    resourceState?.data?.let {
                        (activity as RegistrationActivity?)?.loadRegistrationSummaryFragment()
                    }
                }

                ResourceState.ERROR -> {
                    hideProgress()
                    resourceState.message?.let {
                        (activity as? BaseActivity)?.showErrorDialogue(
                            getString(R.string.error),
                            it,
                            isNegativeButtonNeed = false,
                        ) {}
                    }
                }
            }
        }
    }

    private fun proceedAssessment(data: HashMap<String, Any>?) {
        data?.let { map ->
            map[AssessmentDefinedParams.memberReference]?.toString().let { fhirId ->
                val intent = Intent(requireContext(), AssessmentToolsActivity::class.java)
                intent.putExtra(DefinedParams.FhirId, fhirId)
                intent.putExtra(DefinedParams.ORIGIN, MenuConstants.ASSESSMENT)
                intent.putExtra(DefinedParams.GENDER, "male")
                startActivity(intent)
                activity?.finish()
            }
        }
    }

    private fun prePopulate() {
        patientViewModel.patientDetailsLiveData.value?.data?.let {
            FormAutofill.start(requireContext(), formGenerator, it)
        }
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            binding.btnSubmit.id -> {
                viewModel.isFromProceedEnrollment = false
                formGenerator.formSubmitAction(view)
            }
        }
    }

    override fun loadLocalCache(
        id: String,
        localDataCache: Any,
        selectedParent: Long?,
    ) {
        if (localDataCache is String) {
            viewModel.loadDataCacheByType(id, localDataCache, selectedParent)
        }
    }

    override fun onPopulate(targetId: String) {
    }

    override fun onCheckBoxDialogueClicked(
        id: String,
        formLayout: FormLayout,
        resultMap: Any?,
    ) {
    }

    override fun onInstructionClicked(
        id: String,
        title: String,
        informationList: ArrayList<String>?,
        description: String?,
        dosageListModel: ArrayList<RecommendedDosageListModel>?,
    ) {
    }

    override fun onFormSubmit(
        resultMap: HashMap<String, Any>?,
        serverData: List<FormLayout>?,
    ) {
        withNetworkAvailability(online = {
            resultMap?.let {
                CommonUtils.addAncEnableOrNot(it, BioMetrics)

                if (viewModel.isFromProceedEnrollment) {
                    it.apply {
                        viewModel.validatePatientResponseLiveDate.value?.data?.first?.let { validateData ->
                            if (validateData.containsKey(AssessmentDefinedParams.memberReference)) {
                                validateData[AssessmentDefinedParams.memberReference]
                                    ?.toString()
                                    .let { member ->
                                        if (!member.isNullOrBlank()) {
                                            put(AssessmentDefinedParams.memberReference, member)
                                        }
                                    }
                            }
                            if (validateData.containsKey(Screening.Patient_Id)) {
                                validateData[Screening.Patient_Id]?.let { patientId ->
                                    put(Screening.Patient_Id, patientId.toString())
                                } ?: run {
                                    remove(Screening.Patient_Id)
                                }
                            } else {
                                remove(Screening.Patient_Id)
                            }
                        }
                    }
                    proceedRegistration(it, serverData)
                } else {
                    it.apply {
                        patientViewModel.patientDetailsLiveData.value?.data?.let { patientData ->
                            patientData.id?.let { memberId ->
                                put(AssessmentDefinedParams.memberReference, memberId)
                            }
                            patientData.patientId?.let { patientId ->
                                put(Screening.Patient_Id, patientId)
                            }
                        }
                    }
                    viewModel.validatePatient(it, serverData)
                }
            }
        })
    }

    private fun isGenderOrPregnantField(formLayout: FormLayout): Boolean =
        formLayout.id.equals(DefinedParams.GENDER, true) ||
            formLayout.id.equals(Screening.isPregnant, true)

    private fun isPregnantFemale(): Boolean {
        var isPregnantFemale = false
        patientViewModel.patientDetailsLiveData.value?.data?.let {
            isPregnantFemale = it.gender.equals(Screening.Female, true) && it.isPregnant == true
        }
        return isPregnantFemale
    }

    private fun proceedRegistration(
        resultMap: HashMap<String, Any>?,
        serverData: List<FormLayout>?,
    ) {
        resultMap?.remove(Screening.identityType)
        withNetworkAvailability(online = {
            resultMap?.let { map ->
                val unwantedKeys = setOf(WEEK_KEY, YEAR_KEY, MONTH_KEY, DAYS_KEY)
                map.keys.removeAll(unwantedKeys)
                val result = serverData?.let {
                    FormResultComposer().groupValues(
                        serverData = it,
                        map,
                    )
                }
                result?.second?.let {
                    val bioDataMap = it[Screening.bioData] as HashMap<String, Any>
                    bioDataMap[Screening.identityType] = Screening.nationalId
                    arguments?.getString(Screening.Initial)?.let { initial ->
                        bioDataMap[Screening.Initial] = initial
                    }

                    if (it.containsKey(AssessmentDefinedParams.memberReference)) {
                        it[AssessmentDefinedParams.memberReference]?.toString().let { member ->
                            if (!member.isNullOrBlank()) {
                                it[AssessmentDefinedParams.id] = member
                            }
                        }
                    }

                    viewModel.registerPatient(
                        requireContext(),
                        it,
                        arguments?.getByteArray(Screening.Signature),
                    )
                }
            }
        })
    }

    override fun onRenderingComplete() {
    }

    override fun onUpdateInstruction(
        id: String,
        selectedId: Any?,
    ) {
    }

    override fun onInformationHandling(
        id: String,
        noOfDays: Int,
        enteredDays: Int?,
        resultMap: HashMap<String, Any>?,
    ) {
    }

    override fun onAgeCheckForPregnancy() {
    }

    override fun handleMandatoryCondition(formLayout: FormLayout?) {
    }

    override fun onAgeUpdateListener(
        age: Int,
        serverData: List<FormLayout>?,
        resultHashMap: HashMap<String, Any>,
    ) {
        /*
       Never used
         */
    }

    override fun onQRScanRequested() {
    }

    companion object {
        const val TAG = "RegistrationFormFragment"
    }
}
