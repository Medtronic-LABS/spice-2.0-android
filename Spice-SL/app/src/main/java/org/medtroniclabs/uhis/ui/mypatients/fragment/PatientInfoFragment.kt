package org.medtroniclabs.uhis.ui.mypatients.fragment

import android.content.res.Configuration
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.getLocalDate
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.CommonUtils.combineText
import org.medtroniclabs.uhis.common.CommonUtils.getContactNumber
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DateUtils.DATE_FORMAT_ddMMMyyyy
import org.medtroniclabs.uhis.common.DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ
import org.medtroniclabs.uhis.common.DateUtils.DATE_ddMMyyyy
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.DefinedParams.DEFAULT_ID
import org.medtroniclabs.uhis.common.DefinedParams.EMTCT
import org.medtroniclabs.uhis.common.DefinedParams.EMTCTMR
import org.medtroniclabs.uhis.common.DefinedParams.EMTCT_SUMMARY
import org.medtroniclabs.uhis.common.DefinedParams.HIV_IMR_CMR
import org.medtroniclabs.uhis.common.DefinedParams.IS_FAMILY_PLAN_SUMMARY
import org.medtroniclabs.uhis.common.DefinedParams.IsReferredScreen
import org.medtroniclabs.uhis.common.DefinedParams.MARITAL_STATUS
import org.medtroniclabs.uhis.common.DefinedParams.OCCUPATION
import org.medtroniclabs.uhis.common.DefinedParams.OTHER_NOTES
import org.medtroniclabs.uhis.common.DefinedParams.TB
import org.medtroniclabs.uhis.common.StringConverter
import org.medtroniclabs.uhis.data.offlinesync.model.ProvanceDto
import org.medtroniclabs.uhis.databinding.FragmentPatientInfoBinding
import org.medtroniclabs.uhis.formgeneration.extension.capitalizeFirstChar
import org.medtroniclabs.uhis.mappingkey.Screening
import org.medtroniclabs.uhis.model.PatientListRespModel
import org.medtroniclabs.uhis.ncd.data.NCDPregnancyRiskUpdate
import org.medtroniclabs.uhis.ncd.medicalreview.NCDMedicalReviewActivity
import org.medtroniclabs.uhis.ncd.medicalreview.dialog.NCDMentalHealthQuestionDialog
import org.medtroniclabs.uhis.ncd.medicalreview.dialog.NCDPatientHistoryDialog
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.MenuConstants
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH.ANC
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH.PNC
import org.medtroniclabs.uhis.ui.common.GeneralInfoDialog
import org.medtroniclabs.uhis.ui.dialog.GeneralSuccessDialog
import org.medtroniclabs.uhis.ui.medicalreview.motherneonate.anc.AncVisitCallBack
import org.medtroniclabs.uhis.ui.mypatients.viewmodel.PatientDetailViewModel
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class PatientInfoFragment : BaseFragment() {
    private lateinit var binding: FragmentPatientInfoBinding
    val viewModel: PatientDetailViewModel by activityViewModels()
    private var dataCallback: AncVisitCallBack? = null

    fun setDataCallback(callback: AncVisitCallBack) {
        dataCallback = callback
    }

    companion object {
        const val TAG = "PatientInfoFragment"

        fun newInstance(): PatientInfoFragment = PatientInfoFragment()

        fun newInstance(
            patientId: String?,
            isAnc: Boolean = false,
            isPnc: Boolean = false,
            isReferredScreen: Boolean = false,
            isTb: Boolean = false,
            isFamilyPlan: Boolean = false,
            isFPSummary: Boolean = false,
            isHivImrCmr: Boolean = false,
        ): PatientInfoFragment {
            val fragment = PatientInfoFragment()
            val bundle = Bundle()
            bundle.putString(DefinedParams.PatientId, patientId)
            bundle.putBoolean(ANC, isAnc)
            bundle.putBoolean(PNC, isPnc)
            bundle.putBoolean(TB, isTb)
            bundle.putBoolean(HIV_IMR_CMR, isHivImrCmr)
            bundle.putBoolean(IsReferredScreen, isReferredScreen)
            bundle.putBoolean(MenuConstants.FP_MENU_ID, isFamilyPlan)
            bundle.putBoolean(IS_FAMILY_PLAN_SUMMARY, isFPSummary)
            fragment.arguments = bundle
            return fragment
        }

        fun newInstanceForNCD(
            patientId: String?,
            origin: String,
        ): PatientInfoFragment {
            val fragment = PatientInfoFragment()
            val bundle = Bundle()
            bundle.putString(DefinedParams.PatientId, patientId)
            bundle.putString(DefinedParams.ORIGIN, origin)
            fragment.arguments = bundle
            return fragment
        }

        // EMTCT Patient Info
        fun newInstanceForEMTCT(
            patientId: String?,
            isEMTCT: Boolean = false,
            isEMTCTMR: Boolean = false,
            isEMTCTSummary: Boolean = false,
        ): PatientInfoFragment {
            val fragment = PatientInfoFragment()
            val bundle = Bundle()
            bundle.putString(DefinedParams.PatientId, patientId)
            bundle.putBoolean(DefinedParams.EMTCT, isEMTCT)
            bundle.putBoolean(EMTCTMR, isEMTCTMR)
            bundle.putBoolean(DefinedParams.EMTCT_SUMMARY, isEMTCTSummary)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentPatientInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.getMenuForClinicalWorkflows()
        attachObservers()
    }

    fun initView() {
        val patientId = arguments?.getString(DefinedParams.PatientId, "")
        if (patientId?.isNotBlank() == true) {
            if (CommonUtils.isCommunity()) {
                viewModel.getPatients(patientId, if (isAnc() == true) ANC.uppercase() else null)
            } else {
                val origin = arguments?.getString(DefinedParams.ORIGIN, "")
                viewModel.getPatients(
                    patientId,
                    origin = origin,
                )
            }
        }
    }

    private fun attachObservers() {
        viewModel.clinicalWorkflowsMenusLiveData.observe(viewLifecycleOwner) { data ->
            initView()
        }
        viewModel.patientDetailsLiveData.observe(viewLifecycleOwner) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.SUCCESS -> {
                    resource.data?.let {
                        if (CommonUtils.isCommunity()) {
                            setDataInInfo(it)
                        } else {
                            setNCDData(it)
                        }
                        dataCallback?.onDataLoaded(it)
                    }
                }

                ResourceState.ERROR -> {
//                    hideProgress()
                }
            }
        }

        viewModel.ncdInstructionModelResponse.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.SUCCESS -> {
                    resourceState.data?.instructions?.let { instructions ->
                        val title = instructions.removeAt(0)
                        GeneralInfoDialog
                            .newInstance(
                                title,
                                null,
                                instructions,
                            ).show(childFragmentManager, GeneralInfoDialog.TAG)
                    }
                }

                else -> {
                    // Nothing to invoke
                }
            }
        }

        viewModel.updatePregnancyRisk.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }

                ResourceState.SUCCESS -> {
                    hideProgress()
                    initView()
                }
            }
        }
    }

    private fun isAnc(): Boolean? = arguments?.getBoolean(ANC, false)

    private fun isHivImrCmr(): Boolean = arguments?.getBoolean(HIV_IMR_CMR, false) ?: false

    private fun isPnc(): Boolean? = arguments?.getBoolean(PNC, false)

    private fun isTB(): Boolean? = arguments?.getBoolean(TB, false)

    private fun isReferredScreen(): Boolean? = arguments?.getBoolean(IsReferredScreen, false)

    private fun isFamilyPlan(): Boolean? = arguments?.getBoolean(MenuConstants.FP_MENU_ID, false)

    private fun isFamilyPlanSummary(): Boolean? = arguments?.getBoolean(IS_FAMILY_PLAN_SUMMARY, false)

    private fun isEMTCT(): Boolean? = arguments?.getBoolean(EMTCT, false)

    private fun isEMTCTMR(): Boolean? = arguments?.getBoolean(EMTCTMR, false)

    private fun isEMTCTSummary(): Boolean? = arguments?.getBoolean(EMTCT_SUMMARY, false)

    private fun setDataInInfo(patientListRespModel: PatientListRespModel) {
        showProgress()
        viewModel.patientDetailsId = patientListRespModel.id
        viewModel.childPatientDetails = patientListRespModel.pregnancyDetails?.neonatePatientId
        viewModel.dateOfDelivery = patientListRespModel.pregnancyDetails?.dateOfDelivery
        viewModel.neonateOutCome = patientListRespModel.pregnancyDetails?.neonatalOutcomes
        viewModel.chwName = patientListRespModel.chwName
        viewModel.isEmtctFlow = patientListRespModel.isEmtctFlow == true
        viewModel.hivTestedPositive = patientListRespModel.hivTestedPositive == true
        val isAnc = isAnc()
        val isPnc = isPnc()
        val name =
            patientListRespModel.name ?: requireContext().getString(R.string.separator_hyphen)
        val gender =
            patientListRespModel.gender ?: requireContext().getString(R.string.separator_hyphen)
        val age = patientListRespModel.birthDate?.let {
            DateUtils.getAgeDescription(patientListRespModel.birthDate, requireContext())
        } ?: (patientListRespModel.age ?: requireContext().getString(R.string.separator_hyphen))
        setTitle(requireContext().getString(R.string.household_summary_member_info, name.trim(), age, CommonUtils.translatedGender(requireContext(), gender)))
        val chwPhoneNumber = patientListRespModel.chwPhoneNumber
            ?.takeIf { it.isNotBlank() }
            ?.let { getContactNumber(it) }
            .orEmpty()

        val chwName = patientListRespModel.chwName?.takeIf { it.isNotBlank() }?.trim()

        val chwValue = listOfNotNull(
            chwName,
            chwPhoneNumber.takeIf { it.isNotBlank() }?.let { "($it)" },
        ).joinToString(" ")
        with(binding) {
            val lastMenstrualDate =
                patientListRespModel.pregnancyDetails?.lastMenstrualPeriod.takeIf { it?.isNotBlank() == true }?.let {
                    DateUtils.convertDateFormat(it, DATE_FORMAT_yyyyMMddHHmmssZZZZZ, DATE_ddMMyyyy)
                }
            val dateOfDelivery =
                patientListRespModel.pregnancyDetails?.dateOfDelivery.takeIf { it?.isNotBlank() == true }?.let {
                    DateUtils.convertDateFormat(it, DATE_FORMAT_yyyyMMddHHmmssZZZZZ, DATE_ddMMyyyy) ?: requireContext().getString(R.string.hyphen_symbol)
                }

            val dataList = mutableListOf(
                mapOf(
                    DefinedParams.LABEL to requireContext().getString(R.string.patient_id),
                    DefinedParams.Value to (
                        patientListRespModel.patientId
                            ?: requireContext().getString(R.string.hyphen_symbol)
                    ).toString().trim(),
                ),
                mapOf(
                    DefinedParams.LABEL to requireContext().getString(R.string.contact_number),
                    DefinedParams.Value to (
                        getContactNumber(
                            patientListRespModel.phoneNumber
                                .takeIf { it?.isNotBlank() == true }
                                ?.trim(),
                        )
                            ?: requireContext().getString(R.string.hyphen_symbol)
                    ),
                ),
                mapOf(
                    DefinedParams.LABEL to requireContext().getString(R.string.hh_id),
                    DefinedParams.Value to (
                        patientListRespModel.houseHoldNumber
                            ?: requireContext().getString(R.string.hyphen_symbol)
                    ).toString(),
                ),
                mapOf(
                    DefinedParams.LABEL to requireContext().getString(R.string.landmark),
                    DefinedParams.Value to (
                        patientListRespModel.landmark
                            .takeIf { it?.isNotBlank() == true }
                            ?.trim()
                            ?: requireContext().getString(R.string.hyphen_symbol)
                    ),
                ),
                mapOf(
                    DefinedParams.LABEL to requireContext().getString(R.string.household_location),
                    DefinedParams.Value to (
                        patientListRespModel.village
                            .takeIf { it?.isNotBlank() == true }
                            ?.trim()
                            ?: requireContext().getString(R.string.hyphen_symbol)
                    ),
                ),
                mapOf(
                    DefinedParams.LABEL to requireContext().getString(R.string.dateofbirth),
                    DefinedParams.Value to (
                        patientListRespModel.birthDate
                            ?.getLocalDate()
                            ?.format(DateTimeFormatter.ofPattern(DATE_ddMMyyyy))
                            ?: requireContext().getString(R.string.hyphen_symbol)
                    ),
                ),
            )
            if (isAnc == true && lastMenstrualDate != null) {
                dataList.add(
                    mapOf(
                        DefinedParams.LABEL to requireContext().getString(R.string.last_menstrual_period),
                        DefinedParams.Value to lastMenstrualDate,
                    ),
                )
            }
            if (isPnc == true && dateOfDelivery != null) {
                dataList.add(
                    mapOf(
                        DefinedParams.LABEL to requireContext().getString(R.string.date_of_delivery),
                        DefinedParams.Value to dateOfDelivery,
                    ),
                )
            }
            if (isAnc == true && !(viewModel.isSummary)) {
                dataList.add(
                    mapOf(
                        DefinedParams.LABEL to requireContext().getString(R.string.anc_visit),
                        DefinedParams.Value to (
                            patientListRespModel.pregnancyDetails
                                ?.ancVisitMedicalReview
                                ?.takeIf { true }
                                ?.plus(1)
                                ?.toString()
                                ?: "1"
                        ),
                    ),
                )
            }

            if (isTB() == true) {
                val tbNo = viewModel.presumptiveTbNo ?: ""
                dataList.add(
                    mapOf(
                        DefinedParams.LABEL to requireContext().getString(R.string.presumptive_tb_no),
                        DefinedParams.Value to (patientListRespModel.presumptiveTbNo?.takeIf { it.isNotBlank() } ?: tbNo),
                        DefinedParams.IS_SUMMARY to viewModel.isSummary.toString(),
                    ),
                )
            }
            if (isReferredScreen() == true) {
                dataList.add(
                    mapOf(
                        DefinedParams.LABEL to if (CommonUtils.isCommunity()) {
                            requireContext().getString(
                                R.string.diagnosis_tb,
                            )
                        } else {
                            requireContext().getString(R.string.diagnosis)
                        },
                        DefinedParams.Value to combineText(
                            patientListRespModel.diagnosis
                                ?.filter { it.diseaseCategory?.lowercase() != OTHER_NOTES.lowercase() }
                                ?.map { it.diseaseCategory }
                                ?.distinct(),
                            "",
                            getString(R.string.hyphen_symbol),
                        ),
                    ),
                )
            }
            if (isFamilyPlan() == true || isFamilyPlanSummary() == true) {
                dataList.removeAt(5)
                dataList.removeAt(3)

                if (isFamilyPlanSummary() == true) {
                    dataList.add(
                        mapOf(
                            DefinedParams.LABEL to requireContext().getString(R.string.marital_status_summary),
                            DefinedParams.Value to stringOrHyphen(patientListRespModel.maritalStatus),
                        ),
                    )
                    dataList.add(
                        mapOf(
                            DefinedParams.LABEL to requireContext().getString(R.string.date_of_delivery),
                            DefinedParams.Value to (dateOfDelivery ?: requireContext().getString(R.string.hyphen_symbol)).toString().trim(),
                        ),
                    )
                } else {
                    dataList.add(
                        mapOf(
                            DefinedParams.LABEL to requireContext().getString(R.string.marital_status),
                            DefinedParams.Value to (viewModel.maritalStatus ?: requireContext().getString(R.string.hyphen_symbol)).toString().trim(),
                        ),
                    )
                    dataList.add(
                        mapOf(
                            DefinedParams.LABEL to requireContext().getString(R.string.occupation),
                            DefinedParams.Value to (viewModel.occupation ?: requireContext().getString(R.string.hyphen_symbol)).toString().trim(),
                        ),
                    )
                    dataList.add(
                        mapOf(
                            DefinedParams.LABEL to requireContext().getString(R.string.date_of_delivery),
                            DefinedParams.Value to (dateOfDelivery ?: requireContext().getString(R.string.hyphen_symbol)).toString().trim(),
                        ),
                    )
                }
            }
            if (isEMTCT() == true || isEMTCTSummary() == true) {
               /* dataList.add(
                    mapOf(
                        DefinedParams.label to requireContext().getString(R.string.chw),
                        DefinedParams.Value to stringOrHyphen(chwValue)
                    )
                )
                dataList.add(
                    mapOf(
                        DefinedParams.label to requireContext().getString(R.string.occupation_summary),
                        DefinedParams.Value to stringOrHyphen(patientListRespModel.occupation)
                    )
                )*/
            }
            if (isEMTCTSummary() == true) {
                dataList.add(
                    mapOf(
                        DefinedParams.LABEL to requireContext().getString(R.string.anc_visit),
                        DefinedParams.Value to (
                            patientListRespModel.pregnancyDetails
                                ?.ancVisitMedicalReview
                                ?.takeIf { true }
                                ?.toString()
                                ?: "1"
                        ),
                    ),
                )
            }

            if (isHivImrCmr()) {
                prepareHivImrCmrData(
                    dataList = dataList,
                    patient = patientListRespModel,
                )
            }

            if (isEMTCTMR() == true) {
                prepareEMTCTMRData(
                    dataList = dataList,
                    patient = patientListRespModel,
                )
            }
            commonAdapter(dataList as MutableList<Map<String, Any>>)
        }
    }

    private fun stringOrHyphen(value: String?) = value?.takeIf { it.isNotBlank() }?.trim() ?: requireContext().getString(R.string.hyphen_symbol)

    private fun prepareHivImrCmrData(
        dataList: MutableList<Map<String, String>>,
        patient: PatientListRespModel,
    ): MutableList<Map<String, String>> {
        fun removeItem(
            labelResId: Int,
            value: String?,
        ) {
            dataList.remove(
                mapOf(
                    DefinedParams.LABEL to requireContext().getString(labelResId),
                    DefinedParams.Value to stringOrHyphen(value),
                ),
            )
        }
        removeItem(R.string.household_location, patient.village)
        val chwPhoneNumber = patient.chwPhoneNumber
            ?.takeIf { it.isNotBlank() }
            ?.let { getContactNumber(it) }
            .orEmpty()

        val chwName = patient.chwName?.takeIf { it.isNotBlank() }?.trim()

        val chwValue = listOfNotNull(
            chwName,
            chwPhoneNumber.takeIf { it.isNotBlank() }?.let { "($it)" },
        ).joinToString(" ")
        // Add updated items
        dataList.addAll(
            listOf(
                /*mapOf(
                    DefinedParams.label to requireContext().getString(R.string.occupation),
                    DefinedParams.Value to stringOrHyphen(patient.occupation)
                ),*/
                mapOf(
                    DefinedParams.LABEL to requireContext().getString(R.string.household_location),
                    DefinedParams.Value to stringOrHyphen(patient.village),
                ),
                /*mapOf(
                    DefinedParams.label to requireContext().getString(R.string.chw),
                    DefinedParams.Value to stringOrHyphen(chwValue)
                ),*/
                /*mapOf(
                    DefinedParams.label to requireContext().getString(R.string.marital_status),
                    DefinedParams.Value to stringOrHyphen(patient.maritalStatus)
                ),*/
                mapOf(
                    DefinedParams.LABEL to requireContext().getString(R.string.population_type),
                    DefinedParams.Value to combineText(
                        patient.populationTypes,
                        "",
                        getString(R.string.hyphen_symbol),
                    ),
                ),
                mapOf(
                    DefinedParams.LABEL to requireContext().getString(R.string.art_code),
                    DefinedParams.Value to ((patient.artCode ?: viewModel.artCode) ?: ""),
                    DefinedParams.IS_SUMMARY to viewModel.isSummary.toString(),
                ),
            ),
        )
        return dataList
    }

    private fun prepareEMTCTMRData(
        dataList: MutableList<Map<String, String>>,
        patient: PatientListRespModel,
    ): MutableList<Map<String, String>> {
        val hyphen = requireContext().getString(R.string.hyphen_symbol)
        val formatter = DateTimeFormatter.ofPattern(DATE_ddMMyyyy)

        fun stringOrHyphen(value: String?) = value?.takeIf { it.isNotBlank() }?.trim() ?: hyphen
        val chwPhoneNumber = patient.chwPhoneNumber
            ?.takeIf { it.isNotBlank() }
            ?.let { getContactNumber(it) }
            .orEmpty()

        val chwName = patient.chwName?.takeIf { it.isNotBlank() }?.trim()

        val chwValue = listOfNotNull(
            chwName,
            chwPhoneNumber.takeIf { it.isNotBlank() }?.let { "($it)" },
        ).joinToString(" ")

        fun removeItem(
            labelResId: Int,
            value: String?,
        ) {
            dataList.remove(
                mapOf(
                    DefinedParams.LABEL to requireContext().getString(labelResId),
                    DefinedParams.Value to stringOrHyphen(value),
                ),
            )
        }
        // Remove specific items if present
        removeItem(R.string.dateofbirth, patient.birthDate?.getLocalDate()?.format(formatter))
        removeItem(R.string.landmark, patient.landmark)
        removeItem(R.string.household_location, patient.village)
        // Add updated items
        var entryPoint = listOfNotNull(patient.entryPoint)

        dataList.addAll(
            listOf(
                /*mapOf(
                    DefinedParams.label to requireContext().getString(R.string.occupation_summary),
                    DefinedParams.Value to stringOrHyphen(patient.occupation)
                ),*/
                mapOf(
                    DefinedParams.LABEL to requireContext().getString(R.string.household_location),
                    DefinedParams.Value to stringOrHyphen(patient.village),
                ),
                /*mapOf(
                    DefinedParams.label to requireContext().getString(R.string.chw),
                    DefinedParams.Value to stringOrHyphen(chwValue)
                ),*/
                /*mapOf(
                    DefinedParams.label to requireContext().getString(R.string.marital_status_summary),
                    DefinedParams.Value to stringOrHyphen(patient.maritalStatus)
                ),*/
                mapOf(
                    DefinedParams.LABEL to requireContext().getString(R.string.emtct_enrolment_date),
                    DefinedParams.Value to (
                        patient.emtctEnrollDate
                            ?.getLocalDate()
                            ?.format(DateTimeFormatter.ofPattern(DATE_ddMMyyyy))
                            ?: requireContext().getString(R.string.empty__)
                    ),
                ),
                mapOf(
                    DefinedParams.LABEL to requireContext().getString(R.string.entry_point),
                    DefinedParams.Value to combineText(
                        entryPoint,
                        patient.otherEntryPoint,
                        getString(R.string.hyphen_symbol),
                    ),
                ),
                mapOf(
                    DefinedParams.LABEL to requireContext().getString(R.string.art_code),
                    DefinedParams.Value to ((patient.artCode ?: viewModel.artCode) ?: ""),
                    DefinedParams.IS_SUMMARY to viewModel.isSummary.toString(),
                ),
            ),
        )
        return dataList
    }

    private fun commonAdapter(dataList: MutableList<Map<String, Any>>) {
        val enteredAdapterValues = HashMap<String, Any>()
        enteredAdapterValues[OCCUPATION] = viewModel.occupation ?: ""
        enteredAdapterValues[MARITAL_STATUS] = viewModel.maritalStatus ?: ""
        val adapter =
            PatientInfoAdapter(
                dataList,
                R.color.fragment_bg,
                (requireActivity() as BaseActivity),
                mentalHealthAssessment = { mhPair ->
                    mhPair.first?.let { type ->
                        if (type.isNotBlank()) {
                            viewModel.patientDetailsLiveData.value?.data?.let {
                                withNetworkAvailability(online = {
                                    showMentalHealthDialog(
                                        CommonUtils.getAssessmentType(
                                            requireContext(),
                                            type,
                                        ),
                                        mhPair.second,
                                    )
                                })
                            }
                        }
                    }
                },
                onItemPregnantDialog = {
                    withNetworkAvailability(online = {
                        viewModel.ncdGetInstructions()
                    })
                },
                onItemToggle = {
                    withNetworkAvailability(online = {
                        val request = NCDPregnancyRiskUpdate(
                            memberReference = viewModel.getPatientFHIRId(),
                            patientReference = viewModel.getPatientId(),
                            isPregnancyRisk = it,
                            provenance = ProvanceDto(),
                        )
                        viewModel.ncdUpdatePregnancyRisk(request)
                    })
                },
                occupation = {
                    viewModel.occupation = it
                },
                maritalStatus = {
                    if (!it.equals(DEFAULT_ID)) {
                        viewModel.maritalStatus = it
                    }
                },
                resultValues = enteredAdapterValues,
                presumptiveTbNo = {
                    showPresumptiveTbNo(it)
                },
                artCode = {
                    showArtCode(it)
                },
                isHiv = isHivImrCmr(),
                isFamilyPlanningSummary = viewModel.isFamilyPlanning,
            )
        val isLandscape =
            resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        val spanCount = if (isLandscape) 2 else 1
        if (CommonUtils.checkIsTablet(requireContext())) {
            binding.rvPatientInfo.layoutManager = GridLayoutManager(requireContext(), spanCount)
        } else {
            binding.rvPatientInfo.layoutManager = GridLayoutManager(requireContext(), 1)
        }
        binding.rvPatientInfo.adapter = adapter
        hideProgress()
    }

    private fun showInputDialog(
        tag: String,
        text: String?,
        titleRes: Int,
        hintRes: Int,
        length: Int = 20,
        inputType: Int = InputType.TYPE_CLASS_NUMBER,
        onEntered: (String) -> Unit,
    ) {
        showDialogIfNotPresent(tag) {
            AddPresumptiveTBNoDialog
                .newInstance(
                    text = text,
                    title = getString(titleRes),
                    hint = getString(hintRes),
                    length = length,
                    inputType = inputType,
                ).apply {
                    this.listener = object : OnPresumptiveTBEnteredListener {
                        override fun onPresumptiveTBEntered(tbNumber: String) {
                            onEntered(tbNumber)
                        }
                    }
                }
        }
    }

    private fun showPresumptiveTbNo(text: String?) {
        showInputDialog(
            tag = AddPresumptiveTBNoDialog.TAG,
            text = text,
            titleRes = R.string.presumptive_tb_no,
            hintRes = R.string.enter_number,
        ) { tbNumber ->
            viewModel.presumptiveTbNo = tbNumber
            viewModel.patientDetailsLiveData.value
                ?.data
                ?.apply {
                    presumptiveTbNo = tbNumber
                }?.let {
                    setDataInInfo(it)
                }
        }
    }

    private fun showArtCode(text: String?) {
        showInputDialog(
            tag = AddPresumptiveTBNoDialog.TAG,
            text = text,
            titleRes = R.string.art_code,
            hintRes = R.string.enter_number,
        ) { artCode ->
            viewModel.artCode = artCode
            viewModel.patientDetailsLiveData.value
                ?.data
                ?.apply {
                    this.artCode = artCode
                }?.let {
                    setDataInInfo(it)
                }
        }
    }

    private fun showMentalHealthDialog(
        type: String,
        isEditAssessment: Boolean,
    ) {
        val dialog = childFragmentManager.findFragmentByTag(NCDMentalHealthQuestionDialog.TAG)
        if (dialog == null) {
            NCDMentalHealthQuestionDialog
                .newInstance(
                    type,
                    viewModel.getPatientId(),
                    viewModel.getPatientFHIRId(),
                    isEditAssessment,
                ) { response, errorResponse ->
                    if (errorResponse.isNullOrBlank() && response != null) {
                        val fragment = childFragmentManager.findFragmentByTag(GeneralSuccessDialog.TAG)
                        if (fragment == null) {
                            GeneralSuccessDialog
                                .newInstance(
                                    title = response.first,
                                    message = response.second,
                                    okayButton = getString(R.string.done),
                                ) {
                                    (requireActivity() as? NCDMedicalReviewActivity)?.swipeRefresh()
                                }.show(childFragmentManager, GeneralSuccessDialog.TAG)
                        }
                    } else {
                        (activity as BaseActivity?)?.showErrorDialogue(
                            title = getString(R.string.error),
                            message = errorResponse
                                ?: getString(R.string.something_went_wrong_try_later),
                            positiveButtonName = getString(R.string.ok),
                        ) {}
                    }
                }.show(childFragmentManager, NCDPatientHistoryDialog.TAG)
        }
    }

    private fun setNCDData(data: PatientListRespModel) {
        showProgress()
        data.name?.let { name ->
            setTitle(
                setTitleBasedOnRole(data, name),
            )
        }
        val cvdRiskLevel = data.cvdRiskScore?.toLong()?.takeIf { it > 0 }?.let {
            Pair(
                StringConverter.appendTexts(
                    "$it%",
                    data.cvdRiskLevel,
                    separator = "-",
                ),
                CommonUtils.cvdRiskColorCode(it, requireContext()),
            )
        }

        val bmiPair = CommonUtils.getBMIFormattedText(
            requireContext(),
            data.bmi?.takeIf { it > 0.0 },
        )
        val dataList = mutableListOf(
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.registration_date),
                DefinedParams.Value to (
                    data.enrollmentAt?.let {
                        DateUtils
                            .convertDateFormat(
                                it,
                                DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                                DATE_FORMAT_ddMMMyyyy,
                            ).takeIf { it.isNotBlank() } ?: requireContext().getString(R.string.hyphen_symbol)
                    } ?: requireContext().getString(R.string.pending_registration)
                ).toString().trim(),
            ),
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.cvd_risk),
                DefinedParams.Value to (
                    cvdRiskLevel?.first
                        ?: requireContext().getString(R.string.hyphen_symbol)
                ).toString().trim(),
                DefinedParams.color to cvdRiskLevel?.second,
            ),
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.patient_id),
                DefinedParams.Value to (
                    data.programId
                        ?: requireContext().getString(R.string.hyphen_symbol)
                ).toString().trim(),
            ),
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.bmi),
                DefinedParams.Value to (
                    bmiPair.first
                        ?: requireContext().getString(R.string.hyphen_symbol)
                ).toString().trim(),
                DefinedParams.color to bmiPair.second,
            ),
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.national_id),
                DefinedParams.Value to (
                    data.identityValue
                        ?: requireContext().getString(R.string.hyphen_symbol)
                ).toString().trim(),
            ),
        )
        dataList.add(
            mapOf(
                DefinedParams.LABEL to requireContext().getString(R.string.contact_number),
                DefinedParams.Value to (
                    getContactNumber(
                        data.phoneNumber
                            .takeIf { it?.isNotBlank() == true }
                            ?.trim(),
                    )
                        ?: requireContext().getString(R.string.hyphen_symbol)
                ),
            ),
        )
        val isMentalHealth = viewModel.mrMenuId.equals("mentalHealth", ignoreCase = true)
        if (isMentalHealth) {
            val phq4Score = data.phq4score ?: requireContext().getString(R.string.hyphen_symbol)
            val phq4AssessmentType = if (data.phq4score != null) {
                requireContext().getString(R.string.edit_assessment)
            } else {
                requireContext().getString(
                    R.string.start_assessment,
                )
            }
            dataList.add(
                mapOf(
                    DefinedParams.LABEL to requireContext().getString(R.string.phq4_score),
                    DefinedParams.Value to phq4Score,
                    Screening.type to phq4AssessmentType,
                    DefinedParams.color to requireContext().getColor(R.color.medium_high_risk_color),
                ),
            )
            val showSuicidcalIdeation =
                viewModel.clinicalWorkflowsMenusLiveData.value?.firstOrNull {
                    it.workflowName.equals(
                        Screening.suicideScreener,
                        true,
                    )
                } != null
            if (showSuicidcalIdeation) {
                val suicidcalIdeation = data.suicidalIdeation ?: requireContext().getString(
                    R.string.hyphen_symbol,
                )
                val type =
                    if (data.suicidalIdeation != null) {
                        requireContext().getString(R.string.edit_assessment)
                    } else {
                        requireContext().getString(
                            R.string.start_assessment,
                        )
                    }
                dataList.add(
                    mapOf(
                        DefinedParams.LABEL to requireContext().getString(R.string.suicidal_ideation),
                        DefinedParams.Value to suicidcalIdeation.capitalizeFirstChar(),
                        Screening.type to type,
                        DefinedParams.color to requireContext().getColor(R.color.medium_high_risk_color),
                    ),
                )
            }
            val showCageAid =
                viewModel.clinicalWorkflowsMenusLiveData.value?.firstOrNull {
                    it.workflowName.equals(
                        Screening.substanceAbuse,
                        true,
                    )
                } != null
            if (showCageAid) {
                val cageAid = data.cageAid
                    ?.toDoubleOrNull()
                    ?.toInt()
                    ?.toString()
                    ?: requireContext().getString(R.string.hyphen_symbol)
                val assessmentType =
                    if (data.cageAid != null) {
                        requireContext().getString(R.string.edit_assessment)
                    } else {
                        requireContext().getString(
                            R.string.start_assessment,
                        )
                    }
                dataList.add(
                    mapOf(
                        DefinedParams.LABEL to requireContext().getString(R.string.cage_aid),
                        DefinedParams.Value to cageAid,
                        Screening.type to assessmentType,
                        DefinedParams.color to requireContext().getColor(R.color.medium_high_risk_color),
                    ),
                )
            }
        } else {
            data.cageAid?.let { aid ->
                if ((aid.toDoubleOrNull()?.toInt() ?: 0) > 0) {
                    dataList.add(
                        mapOf(
                            DefinedParams.LABEL to requireContext().getString(R.string.cage_aid),
                            DefinedParams.Value to aid.toDoubleOrNull()?.toInt()?.toString(),
                            DefinedParams.color to requireContext().getColor(R.color.medium_high_risk_color),
                        ),
                    )
                }
            }
        }

        val isPregnancyANC =
            viewModel.mrMenuId.equals(DefinedParams.PregnancyANC, ignoreCase = true)
        if (isPregnancyANC && CommonUtils.canShowToggle(viewModel.getGender(), data.pregnancyDetails?.isPregnancyRisk)) {
            dataList.add(
                mapOf(
                    DefinedParams.LABEL to requireContext().getString(R.string.high_risk),
                    DefinedParams.Value to (data.pregnancyDetails?.isPregnancyRisk ?: false),
                    DefinedParams.GENDER to (data.gender),
                ),
            )
        }
        if (viewModel.isCmr) {
            dataList.add(
                mapOf(
                    DefinedParams.LABEL to requireContext().getString(R.string.diagnosis),
                    DefinedParams.Value to combineText(
                        data.confirmDiagnosis?.diagnosis?.mapNotNull { it.name },
                        data.confirmDiagnosis?.diagnosisNotes.takeIf { it?.isNotBlank() == true },
                        getString(R.string.hyphen_symbol),
                    ),
                ),
            )
        }
        (activity as BaseActivity).setRedRiskPatient(data.isRedRiskPatient)
        commonAdapter(dataList as MutableList<Map<String, Any>>)
    }

    private fun setTitleBasedOnRole(
        response: PatientListRespModel,
        text: String,
    ): String =
        StringConverter
            .appendTexts(
                firstText = text,
                response.age.toString(),
                CommonUtils.translatedGender(requireContext(), response.gender),
                separator = getString(R.string.hyphen_symbol),
            ).capitalizeFirstChar()
}
