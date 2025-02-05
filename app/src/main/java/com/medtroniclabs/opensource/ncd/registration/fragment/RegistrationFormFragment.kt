package com.medtroniclabs.opensource.ncd.registration.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.EntityMapper
import com.medtroniclabs.opensource.common.FormAutofill
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.common.StringConverter
import com.medtroniclabs.opensource.data.model.RecommendedDosageListModel
import com.medtroniclabs.opensource.databinding.FragmentRegistrationFormBinding
import com.medtroniclabs.opensource.formgeneration.FormGenerator
import com.medtroniclabs.opensource.formgeneration.config.DefinedParams.Days
import com.medtroniclabs.opensource.formgeneration.config.DefinedParams.Month
import com.medtroniclabs.opensource.formgeneration.config.DefinedParams.Week
import com.medtroniclabs.opensource.formgeneration.config.DefinedParams.Year
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.formgeneration.listener.FormEventListener
import com.medtroniclabs.opensource.formgeneration.model.FormLayout
import com.medtroniclabs.opensource.formgeneration.ui.FormResultComposer
import com.medtroniclabs.opensource.mappingkey.Screening
import com.medtroniclabs.opensource.mappingkey.Screening.BioMetrics
import com.medtroniclabs.opensource.ncd.medicalreview.viewmodel.NCDFormViewModel
import com.medtroniclabs.opensource.ncd.registration.ui.RegistrationActivity
import com.medtroniclabs.opensource.ncd.registration.viewmodel.RegistrationFormViewModel
import com.medtroniclabs.opensource.ncd.screening.ui.DuplicationNudgeDialog
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.BaseFragment
import com.medtroniclabs.opensource.ui.MenuConstants
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams
import com.medtroniclabs.opensource.ui.home.AssessmentToolsActivity
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.PatientDetailViewModel

class RegistrationFormFragment : BaseFragment(), View.OnClickListener, FormEventListener {
    private lateinit var binding: FragmentRegistrationFormBinding
    private val viewModel: RegistrationFormViewModel by activityViewModels()
    private val ncdFormViewModel: NCDFormViewModel by activityViewModels()
    private val patientViewModel: PatientDetailViewModel by activityViewModels()
    private lateinit var formGenerator: FormGenerator

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRegistrationFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        loadJson()
        attachObservers()
    }

    private fun initViews() {
        formGenerator = FormGenerator(
            requireContext(), binding.llForm, null, this, binding.scrollView,
            translate = SecuredPreference.getIsTranslationEnabled()
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
                            if (isGenderOrPregnantField(it) && isPregnantFemale())
                                it.copy(enableSingleSelection = false)
                            else
                                it
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
                                        responseMap
                                    ), isFromEnrollment = true
                                ) { doAssessment ->
                                    if (doAssessment)
                                        proceedAssessment(responseMap)
                                    else {
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
                            EntityMapper.getResultSpinnerMapList(data)
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
                            EntityMapper.getResultSpinnerMapList(data)
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
                            EntityMapper.getResultSpinnerMapList(data)
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
                            EntityMapper.getResultSpinnerMapList(data)
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
                            EntityMapper.getResultSpinnerMapList(data)
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
                            isNegativeButtonNeed = false
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
                intent.putExtra(DefinedParams.Gender, "male")
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

    override fun loadLocalCache(id: String, localDataCache: Any, selectedParent: Long?) {
        if (localDataCache is String) {
            viewModel.loadDataCacheByType(id, localDataCache, selectedParent)
        }
    }

    override fun onPopulate(targetId: String) {
    }

    override fun onCheckBoxDialogueClicked(
        id: String,
        serverViewModel: FormLayout,
        resultMap: Any?
    ) {
    }

    override fun onInstructionClicked(
        id: String,
        title: String,
        informationList: ArrayList<String>?,
        description: String?,
        dosageListModel: ArrayList<RecommendedDosageListModel>?
    ) {
    }

    override fun onFormSubmit(resultMap: HashMap<String, Any>?, serverData: List<FormLayout?>?) {
        withNetworkAvailability(online = {
            resultMap?.let {
                CommonUtils.addAncEnableOrNot(it, BioMetrics)

                if (viewModel.isFromProceedEnrollment) {
                    it.apply {
                        viewModel.validatePatientResponseLiveDate.value?.data?.first?.let { validateData ->
                            if (validateData.containsKey(AssessmentDefinedParams.memberReference)) {
                                validateData[AssessmentDefinedParams.memberReference]?.toString()
                                    .let { member ->
                                        if (!member.isNullOrBlank())
                                            put(AssessmentDefinedParams.memberReference, member)
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

    private fun isGenderOrPregnantField(formLayout: FormLayout): Boolean {
        return formLayout.id.equals(DefinedParams.Gender, true) ||
                formLayout.id.equals(Screening.isPregnant, true)
    }

    private fun isPregnantFemale(): Boolean {
        var isPregnantFemale = false
        patientViewModel.patientDetailsLiveData.value?.data?.let {
            isPregnantFemale = it.gender.equals(Screening.Female, true) && it.isPregnant == true
        }
        return isPregnantFemale
    }

    private fun proceedRegistration(
        resultMap: HashMap<String, Any>?,
        serverData: List<FormLayout?>?
    ) {
        resultMap?.remove(Screening.identityType)
        withNetworkAvailability(online = {
            resultMap?.let { map ->
                val unwantedKeys = setOf(Week, Year, Month, Days)
                map.keys.removeAll(unwantedKeys)
                val result = serverData?.let {
                    FormResultComposer().groupValues(
                        context = requireContext(),
                        serverData = it,
                        map
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
                            if (!member.isNullOrBlank())
                                it[AssessmentDefinedParams.id] = member
                        }
                    }

                    viewModel.registerPatient(
                        requireContext(),
                        it,
                        arguments?.getByteArray(Screening.Signature)
                    )
                }
            }
        })
    }

    override fun onRenderingComplete() {
    }

    override fun onUpdateInstruction(id: String, selectedId: Any?) {
    }

    override fun onInformationHandling(
        id: String,
        noOfDays: Int,
        enteredDays: Int?,
        resultMap: HashMap<String, Any>?
    ) {
    }

    override fun onAgeCheckForPregnancy() {
    }

    override fun handleMandatoryCondition(serverData: FormLayout?) {

    }

    override fun onAgeUpdateListener(
        age: Int,
        serverData: List<FormLayout?>?,
        resultHashMap: HashMap<String, Any>
    ) {
        /*
       Never used
        */
    }

    companion object {
        const val TAG = "RegistrationFormFragment"
    }
}