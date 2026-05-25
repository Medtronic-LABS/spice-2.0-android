package org.medtroniclabs.uhis.ui.communityprofile.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatSpinner
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.activityViewModels
import com.google.gson.internal.LinkedTreeMap
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.appextensions.startBackgroundOfflineSync
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DateUtils.convertDateToStringWithUTC
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.DefinedParams.COMMUNITY_ID
import org.medtroniclabs.uhis.common.DefinedParams.COMMUNITY_NAME
import org.medtroniclabs.uhis.common.DefinedParams.COMMUNITY_REGISTERED
import org.medtroniclabs.uhis.common.DefinedParams.OTHER
import org.medtroniclabs.uhis.common.DefinedParams.Value
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.common.StringConverter
import org.medtroniclabs.uhis.common.ViewUtils
import org.medtroniclabs.uhis.data.community.CommunityPopulation
import org.medtroniclabs.uhis.data.model.RecommendedDosageListModel
import org.medtroniclabs.uhis.databinding.FragmentEditCommunityBinding
import org.medtroniclabs.uhis.db.entity.CommunityProfile
import org.medtroniclabs.uhis.formgeneration.FormGenerator
import org.medtroniclabs.uhis.formgeneration.config.ViewType.VIEW_TYPE_DIALOG_CHECKBOX
import org.medtroniclabs.uhis.formgeneration.config.ViewType.VIEW_TYPE_FORM_SPINNER
import org.medtroniclabs.uhis.formgeneration.extension.markMandatory
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.listener.FormEventListener
import org.medtroniclabs.uhis.formgeneration.model.FormLayout
import org.medtroniclabs.uhis.formgeneration.ui.FormResultComposer
import org.medtroniclabs.uhis.formgeneration.utility.CheckBoxDialog
import org.medtroniclabs.uhis.formgeneration.utility.CustomSpinnerAdapter
import org.medtroniclabs.uhis.mappingkey.CommunityDetails.AccessRoadToPhu
import org.medtroniclabs.uhis.mappingkey.CommunityDetails.AmbulanceDriverContactNo
import org.medtroniclabs.uhis.mappingkey.CommunityDetails.DescribeLocation
import org.medtroniclabs.uhis.mappingkey.CommunityDetails.EmergencyContactPhu
import org.medtroniclabs.uhis.mappingkey.CommunityDetails.EmergencyManagementPlan
import org.medtroniclabs.uhis.mappingkey.CommunityDetails.EmergencyTransportContactNo
import org.medtroniclabs.uhis.mappingkey.CommunityDetails.False
import org.medtroniclabs.uhis.mappingkey.CommunityDetails.Infrastructure
import org.medtroniclabs.uhis.mappingkey.CommunityDetails.Market
import org.medtroniclabs.uhis.mappingkey.CommunityDetails.MarketDays
import org.medtroniclabs.uhis.mappingkey.CommunityDetails.MobileNetworkCoverage
import org.medtroniclabs.uhis.mappingkey.CommunityDetails.NearestPhu
import org.medtroniclabs.uhis.mappingkey.CommunityDetails.NumberOfHandPumpsNotFunctional
import org.medtroniclabs.uhis.mappingkey.CommunityDetails.NumberOfImprovedToilets
import org.medtroniclabs.uhis.mappingkey.CommunityDetails.NumberOfImprovedWaterSources
import org.medtroniclabs.uhis.mappingkey.CommunityDetails.NumberOfNonImprovedToilets
import org.medtroniclabs.uhis.mappingkey.CommunityDetails.NumberOfNonImprovedWaterSources
import org.medtroniclabs.uhis.mappingkey.CommunityDetails.OtherNetwork
import org.medtroniclabs.uhis.mappingkey.CommunityDetails.SelectedNetwork
import org.medtroniclabs.uhis.mappingkey.CommunityDetails.True
import org.medtroniclabs.uhis.mappingkey.CommunityDetails.WaterAndSanitationFacilities
import org.medtroniclabs.uhis.mappingkey.CommunityDetails.market
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.rootSuffix
import org.medtroniclabs.uhis.ui.communityprofile.adapter.CommunityPopulationAdapter
import org.medtroniclabs.uhis.ui.communityprofile.viewmodel.CommunityProfileViewModel

class EditCommunityProfileFragment : BaseFragment(), FormEventListener, View.OnClickListener {
    private lateinit var binding: FragmentEditCommunityBinding
    private lateinit var formGenerator: FormGenerator
    private val communityProfileViewModel: CommunityProfileViewModel by activityViewModels()
    private lateinit var communityPopulationAdapter: CommunityPopulationAdapter
    private var datePickerDialog: DatePickerDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentEditCommunityBinding.inflate(
            layoutInflater,
            container,
            false,
        )
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        setListener()
        addObservers()
    }

    private fun setListener() {
        binding.btnSubmit.safeClickListener(this)
    }

    private fun addObservers() {
        communityProfileViewModel.saveCommunityDetailsLiveDataLocal.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.SUCCESS -> {
                    hideProgress()
                    val bundle = Bundle().apply {
                        arguments?.getLong(COMMUNITY_ID)?.let {
                            putLong(COMMUNITY_ID, it)
                        }
                        arguments?.getString(COMMUNITY_NAME)?.let {
                            putString(COMMUNITY_NAME, it)
                        }
                    }
                    communityProfileViewModel.updateCurrentFragment(3, bundle)
                    requireActivity().startBackgroundOfflineSync()
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }
            }
        }

        communityProfileViewModel.getCommunityDetailsLiveDataLocal.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.SUCCESS -> {
                    hideProgress()
                    resourceState.data?.let {
                        if (it.villageId == arguments?.getLong(COMMUNITY_ID)) {
                            autoPopulateFormField(it)
                        }
                    }
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }
            }
        }

        communityProfileViewModel.nearestHealthFacilityLiveData.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.SUCCESS -> {
                    hideProgress()
                    resourceState.data?.let { data ->
                        val view =
                            formGenerator.getViewByTag(DefinedParams.NEAREST_PHU) as? AppCompatSpinner
                        if (view != null) {
                            (view.adapter as CustomSpinnerAdapter).setData(data)
                        }
                        prepopulateNearestPhu()
                    }
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }
            }
        }

        communityProfileViewModel.communityStatistics.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.SUCCESS -> {
                    hideProgress()
                    resourceState.data?.let {
                        val list = mutableListOf<CommunityPopulation>()
                        list.add(
                            CommunityPopulation(
                                getString(R.string.total_population),
                                it.populationCount,
                            ),
                        )
                        list.add(
                            CommunityPopulation(
                                getString(R.string.no_of_household_families),
                                it.householdCount,
                            ),
                        )
                        list.add(
                            CommunityPopulation(
                                getString(R.string.no_of_women_of_child_bearing_age),
                                it.childBearingAgeOfWomen,
                            ),
                        )
                        list.add(
                            CommunityPopulation(
                                getString(R.string.no_of_pregnant_women),
                                it.pregnantCount,
                            ),
                        )
                        list.add(
                            CommunityPopulation(
                                getString(R.string.no_of_child_under_one_year),
                                it.belowOneYearCount,
                            ),
                        )
                        list.add(
                            CommunityPopulation(
                                getString(R.string.no_of_children_under_five_years),
                                it.belowFiveYearCount,
                            ),
                        )
                        communityPopulationAdapter.updateList(list)
                    }
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }
            }
        }

        communityProfileViewModel.formLayoutLiveData.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.SUCCESS -> {
                    hideProgress()
                    resourceState.data?.let {
                        formGenerator.populateViews(it.formLayout)
                    }
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }
            }
        }
    }

    private fun prepopulateNearestPhu() {
        formGenerator.getViewByTag(NearestPhu)?.let { view ->
            val nearestPhuName = communityProfileViewModel.nearestPhu.takeIf { it.isNotBlank() }
                ?: communityProfileViewModel.nearestHealthFacilityLiveData.value
                    ?.data
                    ?.firstOrNull { (it[DefinedParams.IS_DEFAULT] as? Boolean) == true }
                    ?.get(DefinedParams.NAME) ?: ""

            formGenerator.setValueForView(nearestPhuName, view)
        }
        formGenerator.getViewByTag(NearestPhu).let {
            if (it is AppCompatSpinner) {
                it.isEnabled = false
            }
        }
    }

    private fun initViews() {
        hideHomeIcon()
        binding.tvNameOfCommunity.markMandatory()
        binding.tvCommunityBoundaryDesc.markMandatory()
        binding.tvRegisteredDate.markMandatory()
        communityProfileViewModel.reinitSaveLiveData()
        arguments?.getLong(COMMUNITY_ID)?.let { villageId ->
            communityProfileViewModel.getPopulationStatistics(villageId = villageId)
        }
        arguments?.getString(DefinedParams.COMMUNITY_NAME).let { villageName ->
            binding.etCommunityName.setText(villageName)
        }
        communityPopulationAdapter = CommunityPopulationAdapter()
        binding.rvCommunitiesStatistics.adapter = communityPopulationAdapter
        formGenerator = FormGenerator(
            requireContext(),
            binding.llForm,
            this,
            binding.nestedScrollView,
            translate = false,
            callback = { map, id ->
                when (id) {
                    Market -> {
                        val isMarket = (map[Market] as? Boolean) ?: false
                        if (!isMarket) {
                            communityProfileViewModel.marketDays.clear()
                        }
                    }
                    MobileNetworkCoverage -> {
                        val isMobileNetwork = (map[MobileNetworkCoverage] as? Boolean) ?: false
                        if (!isMobileNetwork) {
                            communityProfileViewModel.selectedNetworks.clear()
                        }
                    }
                }
            },
        )
        binding.etRegisteredDate.safeClickListener(this)
    }

    companion object {
        const val TAG = "EditCommunityFragment"
    }

    override fun onResume() {
        super.onResume()
        if (arguments?.getBoolean(COMMUNITY_REGISTERED, false) == true) {
            communityProfileViewModel.setUserJourney(AnalyticsDefinedParams.COMMUNITYPROFILEEDITSCREEN)
        } else {
            communityProfileViewModel.setUserJourney(AnalyticsDefinedParams.COMMUNITYPROFILEREGISTERSCREEN)
        }
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.etRegisteredDate -> {
                showDatePickerDialog()
            }

            R.id.btnSubmit -> {
                if (validateCommunityDetails()) {
                    formGenerator.formSubmitAction(view)
                }
            }
        }
    }

    override fun loadLocalCache(
        id: String,
        localDataCache: Any,
        selectedParent: Long?,
    ) {
    }

    override fun onPopulate(targetId: String) {
    }

    override fun onCheckBoxDialogueClicked(
        id: String,
        formLayout: FormLayout,
        resultMap: Any?,
    ) {
        CheckBoxDialog
            .newInstance(
                id,
                resultMap,
                title = getDialogTitle(id),
                autoPopulate = getData(id),
            ) { resultMap ->
                when (id) {
                    MarketDays -> {
                        communityProfileViewModel.marketDays.apply {
                            clear()
                            (resultMap as? List<Map<String, String>>)
                                ?.mapNotNull { it[Value]?.let { value -> value to true } }
                                ?.takeIf { it.isNotEmpty() }
                                ?.let { addAll(it) }
                        }
                    }

                    SelectedNetwork -> {
                        communityProfileViewModel.selectedNetworks.apply {
                            clear()
                            (resultMap as? List<Map<String, String>>)
                                ?.mapNotNull { it[Value]?.let { value -> value to true } }
                                ?.takeIf { it.isNotEmpty() }
                                ?.let { addAll(it) }
                        }
                    }
                }

                formGenerator.validateCheckboxDialogue(id, formLayout, resultMap)
                updateValue(id, resultMap)
            }.show(childFragmentManager, CheckBoxDialog.TAG)
    }

    private fun getDialogTitle(id: String): String? =
        when (id) {
            MarketDays -> getString(R.string.market_days)
            SelectedNetwork -> getString(R.string.networks)
            else -> null
        }

    private fun updateValue(
        id: String,
        resultMap: ArrayList<HashMap<String, Any>>,
    ) {
        when (id) {
            MarketDays -> {
                val marketDaysList = mutableListOf<Pair<String, Boolean>>()
                val list = resultMap as List<*>
                list.forEach { it ->
                    if (it is HashMap<*, *>) {
                        marketDaysList.add(Pair(it[DefinedParams.Value] as String, true))
                    }
                }
                communityProfileViewModel.marketDays = marketDaysList
            }
            SelectedNetwork -> {
                val networkList = mutableListOf<Pair<String, Boolean>>()
                val list = resultMap as List<*>
                list.forEach { it ->
                    if (it is HashMap<*, *>) {
                        networkList.add(Pair(it[DefinedParams.Value] as String, true))
                    }
                }
                communityProfileViewModel.selectedNetworks = networkList
            }
        }
    }

    private fun getData(id: String): List<Pair<String, Boolean>> =
        when (id) {
            MarketDays -> communityProfileViewModel.marketDays
            SelectedNetwork -> communityProfileViewModel.selectedNetworks
            else -> emptyList()
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
        // Calling Local DB
        val regDateUtc = DateUtils
            .convertStringToDate(
                binding.etRegisteredDate.text.toString(),
                DateUtils.DATE_ddMMyyyy,
            )?.let { regDate ->
                convertDateToStringWithUTC(regDate)
            }

        val payload = StringConverter.convertGivenMapToString(getCommunityProfilePayload(resultMap, serverData))
        val villageId = arguments?.getLong(COMMUNITY_ID)
        if (regDateUtc != null && payload != null && villageId != null) {
            communityProfileViewModel.setUserJourney(AnalyticsDefinedParams.SUBMITBUTTONTRIGGERED)
            communityProfileViewModel.insertOrUpdateCommunityProfile(
                villageId = villageId,
                description = binding.etCommunityBoundaryDesc.text.toString(),
                regDate = regDateUtc,
                payload = payload,
            )
        }
    }

    private fun getCommunityProfilePayload(
        resultMap: HashMap<String, Any>?,
        serverData: List<FormLayout?>?,
    ): HashMap<String, Any>? {
        resultMap?.let { details ->
            serverData?.let { form ->
                val result = FormResultComposer().groupValues(
                    serverData = form,
                    resultMap = details,
                )

                val requestMap = result.second
                if (requestMap.containsKey(WaterAndSanitationFacilities)) {
                    val waterAndSanitation = requestMap[WaterAndSanitationFacilities] as HashMap<Any, Any>
                    if (waterAndSanitation.containsKey(NumberOfImprovedWaterSources)) {
                        val numberOfImprovedWaterSources = waterAndSanitation[NumberOfImprovedWaterSources] as String
                        if (numberOfImprovedWaterSources.equals(getString(R.string.two_zero)) ||
                            numberOfImprovedWaterSources.equals(getString(R.string.three_zero))
                        ) {
                            waterAndSanitation[NumberOfImprovedWaterSources] = "0"
                        }
                    }

                    if (waterAndSanitation.containsKey(NumberOfNonImprovedWaterSources)) {
                        val numberOfImprovedWaterSources = waterAndSanitation[NumberOfNonImprovedWaterSources] as String
                        if (numberOfImprovedWaterSources.equals(getString(R.string.two_zero)) ||
                            numberOfImprovedWaterSources.equals(getString(R.string.three_zero))
                        ) {
                            waterAndSanitation[NumberOfNonImprovedWaterSources] = "0"
                        }
                    }

                    if (waterAndSanitation.containsKey(NumberOfImprovedToilets)) {
                        val numberOfImprovedWaterSources = waterAndSanitation[NumberOfImprovedToilets] as String
                        if (numberOfImprovedWaterSources.equals(getString(R.string.two_zero)) ||
                            numberOfImprovedWaterSources.equals(getString(R.string.three_zero))
                        ) {
                            waterAndSanitation[NumberOfImprovedToilets] = "0"
                        }
                    }

                    if (waterAndSanitation.containsKey(NumberOfNonImprovedToilets)) {
                        val numberOfImprovedWaterSources = waterAndSanitation[NumberOfNonImprovedToilets] as String
                        if (numberOfImprovedWaterSources.equals(getString(R.string.two_zero)) ||
                            numberOfImprovedWaterSources.equals(getString(R.string.three_zero))
                        ) {
                            waterAndSanitation[NumberOfNonImprovedToilets] = "0"
                        }
                    }

                    if (waterAndSanitation.containsKey(NumberOfHandPumpsNotFunctional)) {
                        val numberOfImprovedWaterSources = waterAndSanitation[NumberOfHandPumpsNotFunctional] as String
                        if (numberOfImprovedWaterSources.equals(getString(R.string.two_zero)) || numberOfImprovedWaterSources.equals("000")) {
                            waterAndSanitation[NumberOfHandPumpsNotFunctional] = "0"
                        }
                    }
                }
                if (requestMap.containsKey(Infrastructure)) {
                    val infrastructure = requestMap[Infrastructure] as HashMap<Any, Any>
                    if (infrastructure.containsKey(market)) {
                        val isMarket = infrastructure[market] as Boolean
                        val signsList = mutableListOf<String>()
                        if (isMarket && infrastructure.containsKey(MarketDays)) {
                            val list = infrastructure[MarketDays] as List<HashMap<*, *>>
                            list.forEach {
                                (it[DefinedParams.NAME] as? String)?.let { day ->
                                    signsList.add(
                                        day,
                                    )
                                }
                            }
                        } else {
                            communityProfileViewModel.marketDays.clear()
                        }
                        infrastructure[MarketDays] = signsList
                    }

                    if (infrastructure.containsKey(MobileNetworkCoverage)) {
                        val isMobileNetwork = infrastructure[MobileNetworkCoverage] as Boolean
                        val networkList = mutableListOf<String>()
                        if (isMobileNetwork && infrastructure.containsKey(SelectedNetwork)) {
                            val list = infrastructure[SelectedNetwork] as List<HashMap<*, *>>
                            list.forEach {
                                (it[DefinedParams.NAME] as? String)?.let { day ->
                                    networkList.add(
                                        day,
                                    )
                                }
                            }
                        } else {
                            communityProfileViewModel.selectedNetworks.clear()
                        }
                        infrastructure[SelectedNetwork] = networkList
                    }
                }

                if (requestMap.containsKey(EmergencyManagementPlan)) {
                    val emergency = requestMap[EmergencyManagementPlan] as HashMap<Any, Any>
                    if (emergency.containsKey(NearestPhu)) {
                        serverData.forEach { serverData ->
                            when (serverData?.viewType) {
                                VIEW_TYPE_FORM_SPINNER -> {
                                    val nearestPhu = emergency[NearestPhu].toString()
                                    val view = formGenerator.getViewByTag(NearestPhu) as? Spinner
                                    val adapter = view?.adapter as? CustomSpinnerAdapter
                                    val index = adapter?.getIndexOfItemById(nearestPhu)
                                    if (index != -1) {
                                        val name = adapter?.getItem(index ?: 0) as String
                                        emergency[NearestPhu] = name
                                    }
                                }
                            }
                        }
                    }

                    val countryCode = SecuredPreference.getPhoneNumberCode()
                    if (emergency.containsKey(EmergencyContactPhu)) {
                        emergency[EmergencyContactPhu] = "+$countryCode " + emergency[EmergencyContactPhu]
                    }
                    if (emergency.containsKey(EmergencyTransportContactNo)) {
                        emergency[EmergencyTransportContactNo] = "+$countryCode " + emergency[EmergencyTransportContactNo]
                    }
                    if (emergency.containsKey(AmbulanceDriverContactNo)) {
                        emergency[AmbulanceDriverContactNo] = "+$countryCode " + emergency[AmbulanceDriverContactNo]
                    }
                }

                return requestMap
                // return Gson().toJsonTree(requestMap)
            }
        }

        return null
    }

    override fun onRenderingComplete() {
        communityProfileViewModel.marketDays.clear()
        communityProfileViewModel.selectedNetworks.clear()
        communityProfileViewModel.nearestPhu = ""
        communityProfileViewModel.otherNetwork = ""
        communityProfileViewModel.getNearestHealthFacility(arguments?.getLong(COMMUNITY_ID))
        arguments?.getLong(COMMUNITY_ID)?.let { villageId ->
            communityProfileViewModel.getCommunityDetailsLocal(villageId)
        }
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
    }

    override fun onQRScanRequested() {
    }

    fun getCurrentAnswerStatus(): Boolean = formGenerator.getResultMap().isNotEmpty()

    private fun validateCommunityDetails(): Boolean {
        var isValid = true

        binding.apply {
            isValid = checkField(etCommunityName, tvErrorMessage)
            isValid = checkField(etCommunityBoundaryDesc, tvErrorMessageForCommunityDesc) && isValid
        }

        if (binding.etRegisteredDate.text
                .toString()
                .isEmpty()
        ) {
            binding.tvErrorRegisteredDate.visibility = View.VISIBLE
            formGenerator.validateInputs()
            isValid = false
        } else {
            binding.tvErrorRegisteredDate.visibility = View.GONE
        }

        return isValid
    }

    private fun checkField(
        editText: AppCompatEditText,
        errorView: AppCompatTextView,
    ): Boolean =
        if (editText.text.isNullOrEmpty()) {
            errorView.visibility = View.VISIBLE
            formGenerator.validateInputs()
            false
        } else {
            errorView.visibility = View.GONE
            true
        }

    private fun showDatePickerDialog() {
        var yearMonthDate: Triple<Int?, Int?, Int?>? = null
        if (!binding.etRegisteredDate.text.isNullOrBlank()) {
            yearMonthDate =
                DateUtils.convertedMMMToddMM(binding.etRegisteredDate.text.toString())
        }
        if (datePickerDialog == null) {
            datePickerDialog = ViewUtils.showDatePicker(
                context = requireContext(),
                date = yearMonthDate,
                disableFutureDate = true,
                cancelCallBack = { datePickerDialog = null },
            ) { _, year, month, dayOfMonth ->
                val stringDate = "$dayOfMonth-$month-$year"
                binding.etRegisteredDate.setText(
                    DateUtils.convertDateTimeToDate(
                        stringDate,
                        DateUtils.DATE_FORMAT_ddMMyyyy,
                        DateUtils.DATE_ddMMyyyy,
                    ),
                )

                datePickerDialog = null
            }
        }
    }

    private fun autoPopulateFormField(details: CommunityProfile) {
        binding.etCommunityBoundaryDesc.setText(details.communityDescription)
        binding.etRegisteredDate.text = DateUtils.convertUTCString(
            details.registeredDate,
            DateUtils.DATE_ddMMyyyy,
        )
        details.payload?.let {
            val dataMap = StringConverter.stringToMap(it)
            // Water and sanitation
            val waterAndSanitation = dataMap[WaterAndSanitationFacilities] as LinkedTreeMap<*, *>
            waterAndSanitation.forEach { (key, value) ->
                formGenerator.getViewByTag(key)?.let { view ->
                    formGenerator.setValueForView(value, view)
                }
            }

            // Infrastructure
            val infrastructure = dataMap[Infrastructure] as LinkedTreeMap<*, *>
            infrastructure.forEach { (key, value) ->
                if (key.toString() == DescribeLocation || key.toString() == OtherNetwork) {
                    if (key.toString() == OtherNetwork) {
                        communityProfileViewModel.otherNetwork = value.toString()
                    }
                    formGenerator.getViewByTag(key)?.let { view ->
                        formGenerator.setValueForView(value, view)
                    }
                } else {
                    setSingleSelection(value, key.toString())
                }

                if (key.toString() == MarketDays) {
                    formGenerator.getViewByTag(key)?.let { view ->
                        if (view is TextView) {
                            formGenerator.getServerData()?.forEach { serverData ->
                                when (serverData.viewType) {
                                    VIEW_TYPE_DIALOG_CHECKBOX -> {
                                        val list = infrastructure[MarketDays] as List<String>
                                        communityProfileViewModel.marketDays = list.map { data -> Pair(data, true) }.toMutableList()
                                        val daysMap = ArrayList<HashMap<String, Any>>()
                                        list.forEach {
                                            daysMap.add(hashMapOf(DefinedParams.NAME to it))
                                        }
                                        formGenerator.validateCheckboxDialogue(
                                            MarketDays,
                                            serverData,
                                            daysMap,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (key.toString() == SelectedNetwork) {
                    formGenerator.getViewByTag(key)?.let { view ->
                        if (view is TextView) {
                            formGenerator.getServerData()?.forEach { serverData ->
                                when (serverData.viewType) {
                                    VIEW_TYPE_DIALOG_CHECKBOX -> {
                                        val list = infrastructure[SelectedNetwork] as List<String>
                                        communityProfileViewModel.selectedNetworks = list.map { data -> Pair(data, true) }.toMutableList()
                                        val daysMap = ArrayList<HashMap<String, Any>>()
                                        list.forEach {
                                            daysMap.add(hashMapOf(DefinedParams.NAME to it))
                                        }
                                        formGenerator.validateCheckboxDialogue(
                                            SelectedNetwork,
                                            serverData,
                                            daysMap,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // EmergencyManagementPlan
            val emergency = dataMap[EmergencyManagementPlan] as LinkedTreeMap<*, *>
            emergency.forEach { (key, value) ->
                when (key.toString()) {
                    AccessRoadToPhu -> {
                        setSingleSelection(value, key.toString())
                    }
                    NearestPhu -> {
                        communityProfileViewModel.nearestPhu = value.toString()
                    }
                    EmergencyContactPhu, EmergencyTransportContactNo,
                    AmbulanceDriverContactNo,
                    -> {
                        val phoneNumber = value.toString().substringAfter(" ")
                        formGenerator.getViewByTag(key)?.let { view ->
                            formGenerator.setValueForView(phoneNumber, view)
                        }
                    }
                    else -> {
                        formGenerator.getViewByTag(key)?.let { view ->
                            formGenerator.setValueForView(value, view)
                        }
                    }
                }
            }
        }

        if (communityProfileViewModel.selectedNetworks.any {
                it.first.equals(
                    OTHER,
                    true,
                )
            }
        ) {
            formGenerator.getViewByTag(OtherNetwork + rootSuffix)?.visible()
            formGenerator.getViewByTag(OtherNetwork)?.let { view ->
                formGenerator.setValueForView(communityProfileViewModel.otherNetwork, view)
            }
        }
    }

    private fun setSingleSelection(
        value: Any?,
        key: String,
    ) {
        value.let {
            when (value.toString()) {
                True -> {
                    singleSelectValueOption(
                        True,
                        key,
                    )
                }

                False -> {
                    singleSelectValueOption(
                        False,
                        key,
                    )
                }

                else -> {}
            }
        }
    }

    private fun singleSelectValueOption(
        value: String,
        key: String,
    ) {
        formGenerator
            .getViewByTag("${value}_$key")
            ?.let { view ->
                if (view is TextView) {
                    view.performClick()
                }
            }
    }
}
