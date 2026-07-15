package org.medtroniclabs.uhis.ncd.screening.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.activityViewModels
import io.github.mrherintsoahasina.flextools.FlexRadioGroup
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.isVisible
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.DefinedParams.OTHER
import org.medtroniclabs.uhis.databinding.FragmentGeneralDetailsBinding
import org.medtroniclabs.uhis.db.entity.HealthFacilityEntity
import org.medtroniclabs.uhis.formgeneration.extension.markMandatory
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.utility.CustomSpinnerAdapter
import org.medtroniclabs.uhis.mappingkey.Screening.Camp
import org.medtroniclabs.uhis.mappingkey.Screening.Community
import org.medtroniclabs.uhis.mappingkey.Screening.DoorToDoor
import org.medtroniclabs.uhis.mappingkey.Screening.Facility
import org.medtroniclabs.uhis.mappingkey.Screening.OPDTriage
import org.medtroniclabs.uhis.mappingkey.Screening.Pharmacy
import org.medtroniclabs.uhis.mappingkey.Screening.inpatient
import org.medtroniclabs.uhis.mappingkey.Screening.outpatient
import org.medtroniclabs.uhis.ncd.screening.viewmodel.GeneralDetailsViewModel
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.medicalreview.motherneonate.anc.MotherNeonateUtil

// --- FIX 1: Remove the conflicting listener interfaces from the class declaration ---
class GeneralDetailsFragment : BaseFragment(), View.OnClickListener {
    private lateinit var binding: FragmentGeneralDetailsBinding
    private val viewModel: GeneralDetailsViewModel by activityViewModels()

    private val adapter by lazy { CustomSpinnerAdapter(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentGeneralDetailsBinding.inflate(
            inflater,
            container,
            false,
        )
        return binding.root
    }

    companion object {
        const val TAG = "GeneralDetailsFragment"

        fun newInstance() = GeneralDetailsFragment()
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        attachObservers()
        setListeners()
        restorePreviousSelections()
    }

    private fun restorePreviousSelections() {
        // Check if a category was selected before
        when (viewModel.siteDetail.category) {
            Facility -> {
                binding.rbFacility.isChecked = true
                showFacilityOptions()
                restoreTypeSelection()
            }

            Community -> {
                binding.rbCommunity.isChecked = true
                showCommunityOptions()
                restoreTypeSelection()
            }
            else -> binding.rbFacility.isChecked = true
        }
    }

    // Restores the type selection based on what was previously stored in the ViewModel
    private fun restoreTypeSelection() {
        when (viewModel.siteDetail.categoryType) {
            OPDTriage -> binding.rbTypeBtn1.isChecked = true
            outpatient -> binding.rbTypeBtn2.isChecked = true
            inpatient -> binding.rbTypeBtn3.isChecked = true
            Pharmacy -> binding.rbTypeBtn4.isChecked = true
            OTHER -> {
                binding.rbTypeBtn5.isChecked = true
                binding.etOthers.visible()
                binding.etOthers.setText(viewModel.siteDetail.otherType)
            }
            DoorToDoor -> binding.rbTypeBtn6.isChecked = true
            Camp -> binding.rbTypeBtn7.isChecked = true
            else -> {
                if (viewModel.siteDetail.category == Community) {
                    binding.etOthers.text = null
                    binding.etOthers.gone()
                    viewModel.siteDetail.otherType = null
                    binding.rbTypeBtn6.isChecked = true
                }
            }
        }
    }

    private fun attachObservers() {
        viewModel.getSitesLiveData.observe(viewLifecycleOwner) {
            loadSiteDetails(ArrayList(it))
        }
    }

    private fun loadSiteDetails(data: ArrayList<HealthFacilityEntity>?) {
        val defaultUserSiteId: Long = data?.first { it.isDefault }?.fhirId?.toLongOrNull() ?: -1L
        viewModel.siteDetail.apply {
            userSiteId = defaultUserSiteId
        }
        val list = arrayListOf<Map<String, Any>>(
            hashMapOf(
                DefinedParams.NAME to DefinedParams.DEFAULT_ID_LABEL,
                DefinedParams.ID to DefinedParams.DEFAULT_SELECT_ID,
            ),
        )
        var defaultPosition = 0
        data
            ?.mapIndexed { index, site ->
                hashMapOf(
                    DefinedParams.ID to site.id,
                    DefinedParams.NAME to site.name,
                    DefinedParams.TENANT_ID to site.tenantId,
                    DefinedParams.FhirId to (site.fhirId ?: 0),
                ).also {
                    if (viewModel.siteDetail.siteId == site.id) {
                        defaultPosition = index + 1
                    } else if (site.isDefault) {
                        defaultPosition = index + 1
                    }
                }
            }?.let { list.addAll(it) }
        adapter.setData(list)
        binding.etSiteChange.post {
            binding.etSiteChange.setSelection(defaultPosition, false)
        }
        binding.etSiteChange.adapter = adapter
    }

    private fun setListeners() {
        binding.etSiteChange.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                adapterView: AdapterView<*>?,
                view: View?,
                pos: Int,
                itemId: Long,
            ) {
                adapter.getData(pos)?.let {
                    processSiteSelection(it)
                    validateToEnableNext()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun processSiteSelection(map: Map<String, Any>) {
        viewModel.siteDetail.apply {
            siteName = map[DefinedParams.NAME] as? String ?: ""
            siteId = map[DefinedParams.FhirId]?.toString()?.toLongOrNull() ?: -1L
            tenantId = map[DefinedParams.TENANT_ID]?.toString()?.toLongOrNull() ?: -1L
        }
    }

    private fun initView() {
        binding.apply {
            tvTitleCategory.markMandatory()
            tvTitleType.markMandatory()
            btnNext.safeClickListener(this@GeneralDetailsFragment)

            // --- FIX 3: Set listeners using modern lambda expressions ---
            rgCategoryRow.setOnCheckedChangeListener { group, checkedId ->
                onCategoryGroupChanged(group, checkedId)
            }
            rgType.setOnCheckedChangeListener { group, checkedId ->
                onFlexBoxGroupChanged(group, checkedId)
            }
        }
        viewModel.getSites(true)
        MotherNeonateUtil.initTextWatcherForString(binding.etOthers) {
            viewModel.siteDetail.categoryDisplayType = it.ifBlank { null }
            validateToEnableNext()
        }
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.btnNext -> {
                if (binding.etOthers.isVisible()) {
                    viewModel.siteDetail.otherType = binding.etOthers
                        .takeIf { it.isVisible() }
                        ?.text
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                        ?.toString()
                }
                replaceFragmentIfExists<StatsFragment>(
                    R.id.screeningParentLayout,
                    bundle = null,
                    tag = StatsFragment.TAG,
                )
            }
        }
    }

    // --- FIX 2: Renamed method for the standard RadioGroup, remove 'override' ---
    private fun onCategoryGroupChanged(
        radioGroup: android.widget.RadioGroup,
        radioButton: Int,
    ) {
        when (radioGroup.id) {
            R.id.rgCategoryRow -> {
                when (radioButton) {
                    R.id.rbFacility -> {
                        viewModel.siteDetail.category = Facility
                        viewModel.siteDetail.categoryDisplayName = getString(R.string.clinic)
                        showFacilityOptions()
                    }
                    R.id.rbCommunity -> {
                        viewModel.siteDetail.category = Community
                        viewModel.siteDetail.categoryDisplayName = getString(R.string.community)
                        showCommunityOptions()
                    }
                }
            }
        }
    }

    private fun showFacilityOptions() {
        binding.apply {
            rgType.clearCheck()
            rbTypeBtn1.setText(R.string.opd_triage)
            rbTypeBtn2.setText(R.string.outpatient)
            rbTypeBtn3.setText(R.string.inpatient)
            rbTypeBtn4.setText(R.string.pharmacy)
            rbTypeBtn5.setText(R.string.Other)
            rbTypeBtn1.visible()
            rbTypeBtn2.visible()
            rbTypeBtn3.visible()
            rbTypeBtn4.visible()
            rbTypeBtn5.visible()
            rbTypeBtn6.gone()
            rbTypeBtn7.gone()
            etOthers.gone()
        }
    }

    private fun showCommunityOptions() {
        binding.apply {
            rgType.clearCheck()
            rbTypeBtn6.setText(R.string.door_to_door)
            rbTypeBtn7.setText(R.string.camp)
            rbTypeBtn6.visible()
            rbTypeBtn7.visible()
            rbTypeBtn1.gone()
            rbTypeBtn2.gone()
            rbTypeBtn3.gone()
            rbTypeBtn4.gone()
            rbTypeBtn5.gone()
            etOthers.gone()
        }
    }

    // --- FIX 2: Renamed method for the FlexBoxRadioGroup, remove 'override' ---
    private fun onFlexBoxGroupChanged(
        radioGroup: FlexRadioGroup,
        radioButton: Int,
    ) {
        when (radioGroup.checkedRadioButtonId) {
            R.id.rbTypeBtn1 -> configureCategoryType(R.string.opd_triage, OPDTriage)
            R.id.rbTypeBtn2 -> configureCategoryType(R.string.outpatient, outpatient)
            R.id.rbTypeBtn3 -> configureCategoryType(R.string.inpatient, inpatient)
            R.id.rbTypeBtn4 -> configureCategoryType(R.string.pharmacy, Pharmacy)
            R.id.rbTypeBtn5 -> {
                binding.etOthers.visible()
                viewModel.siteDetail.categoryDisplayType = getString(R.string.Other)
                viewModel.siteDetail.categoryType = OTHER
            }
            R.id.rbTypeBtn6 -> configureCategoryType(R.string.door_to_door, DoorToDoor)
            R.id.rbTypeBtn7 -> configureCategoryType(R.string.camp, Camp)
            else -> {
                val facilityList = listOf(OTHER, OPDTriage, outpatient, inpatient, Pharmacy)
                val communityList = listOf(DoorToDoor, Camp)
                if (viewModel.siteDetail.category == Community &&
                    (
                        viewModel.siteDetail.categoryType == null ||
                            facilityList.contains(
                                viewModel.siteDetail.categoryType,
                            )
                    )
                ) {
                    binding.rbTypeBtn6.isChecked = true
                } else if (viewModel.siteDetail.category == Facility &&
                    communityList.contains(
                        viewModel.siteDetail.categoryType,
                    )
                ) {
                    viewModel.siteDetail.categoryType = null
                }
            }
        }
        validateToEnableNext()
    }

    private fun configureCategoryType(
        displayResId: Int,
        type: String,
    ) {
        binding.etOthers.gone()
        binding.etOthers.text = null
        viewModel.siteDetail.apply {
            otherType = null
            categoryDisplayType = getString(displayResId)
            categoryType = type
        }
    }

    private fun validateToEnableNext() {
        val categorySelected = binding.rgCategoryRow.checkedRadioButtonId != -1
        var typeSelected = binding.rgType.checkedRadioButtonId != -1
        if (viewModel.siteDetail.categoryType.equals(OTHER, true)) {
            typeSelected = !binding.etOthers.text
                ?.trim()
                .isNullOrBlank()
        }
        binding.btnNext.isEnabled =
            categorySelected &&
            typeSelected &&
            viewModel.siteDetail.siteId != -1L
    }
}
