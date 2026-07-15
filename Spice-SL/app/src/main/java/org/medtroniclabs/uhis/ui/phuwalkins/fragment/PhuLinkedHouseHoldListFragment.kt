package org.medtroniclabs.uhis.ui.phuwalkins.fragment

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity.RESULT_CANCELED
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams.PHUWALKINSCREENHOUSEHOLDLISTCALLBUTTON
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.setTextChangeListener
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DefinedParams.FhirMemberID
import org.medtroniclabs.uhis.common.DefinedParams.MEMBER_ID
import org.medtroniclabs.uhis.common.DefinedParams.VILLAGE_ID
import org.medtroniclabs.uhis.common.DefinedParams.isCreateHouseholdForPhu
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.offlinesync.model.UnAssignedHouseholdMemberDetail
import org.medtroniclabs.uhis.databinding.FragmentPhuLinkedHosueHoldListBinding
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.household.ConsentFormActivity
import org.medtroniclabs.uhis.ui.household.HouseholdDefinedParams.IS_PHU_WALK_INS_FLOW
import org.medtroniclabs.uhis.ui.phuwalkins.adapter.PhuHouseHoldListAdapter
import org.medtroniclabs.uhis.ui.phuwalkins.listener.PhuLinkCallback
import org.medtroniclabs.uhis.ui.phuwalkins.viewmodel.PhuWalkInsViewModel

class PhuLinkedHouseHoldListFragment(private val patientLinkedDetails: UnAssignedHouseholdMemberDetail) :
    BaseFragment(), View.OnClickListener {
    private lateinit var binding: FragmentPhuLinkedHosueHoldListBinding
    private var dataCallback: PhuLinkCallback? = null
    private val viewModel: PhuWalkInsViewModel by viewModels()

    fun setDataCallback(callback: PhuLinkCallback) {
        dataCallback = callback
    }

    companion object {
        const val TAG = "PhuLinkedHouseHoldListFragment"

        fun newInstance(patientLinkedDetails: UnAssignedHouseholdMemberDetail): PhuLinkedHouseHoldListFragment =
            PhuLinkedHouseHoldListFragment(patientLinkedDetails)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentPhuLinkedHosueHoldListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        setHouseholdLinkedDetails()
        initObserver()
        binding.includedHousehold.linkCallDetailsBtn.setOnClickListener {
            viewModel.memberID = patientLinkedDetails.memberId.toLong()
            val dialIntent = Intent(Intent.ACTION_DIAL)
            dialIntent.data = Uri.parse("tel:${patientLinkedDetails.phoneNumber}")
            dialerLauncher.launch(dialIntent)
            viewModel.setUserJourney(PHUWALKINSCREENHOUSEHOLDLISTCALLBUTTON)
        }
        searchHouseHoldMembers()
    }

    private fun initObserver() {
        viewModel
            .getFilteredHouseholdsLiveData(patientLinkedDetails.villageId)
            .observe(viewLifecycleOwner) {
                val householdList = it

                listVisibility(householdList.isEmpty())
                binding.tvHPatientCount.text =
                    householdList.size
                        .toString()
                        .plus(getString(R.string.households_in))
                        .plus(patientLinkedDetails.villageName)
                // Set adapter
                binding.rcLinkPatientList.layoutManager = LinearLayoutManager(requireContext())
                binding.rcLinkPatientList.adapter =
                    PhuHouseHoldListAdapter(householdList, activity as PhuLinkCallback)
            }
    }

    private fun listVisibility(houseHoldEntityWithMemberCounts: Boolean) {
        if (houseHoldEntityWithMemberCounts) {
            binding.tvPatientNoFound.visible()
            binding.rcLinkPatientList.gone()
            binding.tvHPatientCount.gone()
        } else {
            binding.tvPatientNoFound.gone()
            binding.rcLinkPatientList.visible()
            binding.tvHPatientCount.visible()
        }
    }

    private fun searchHouseHoldMembers() {
        binding.searchView.safeClickListener(this)
        binding.searchView.setTextChangeListener {
            val input = it?.trim().toString()
            searchHouseHoldList(input, patientLinkedDetails.villageId)
        }
        binding.btnAddHousehold.safeClickListener(this)
    }

    private val dialerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK || result.resultCode == RESULT_CANCELED) {
                viewModel.saveCallHistory()
            }
        }

    private fun setHouseholdLinkedDetails() {
        binding.includedHousehold.apply {
            linkPatientBtn.gone()
            callPatientBtn.gone()
            linkCallDetailsBtn.visible()
            val age =
                context?.let { CommonUtils.getAgeFromDOB(patientLinkedDetails.dateOfBirth, it) }
            age?.toIntOrNull()?.let {
                if (it >= 10) binding.bottomNavigation.visible()
            }
            patientNameAgeGender.text =
                formatPatientDemographics(requireContext(), patientLinkedDetails)
            patientVillage.text = patientLinkedDetails.villageName
            val phoneNumberCode = SecuredPreference.getPhoneNumberCode()
            patientMobile.text = "+$phoneNumberCode ${patientLinkedDetails.phoneNumber}"
        }
    }

    private fun formatPatientDemographics(
        context: Context,
        patient: UnAssignedHouseholdMemberDetail,
    ): String = viewModel.getPatientName(context, patient.name, patient.dateOfBirth, patient.gender)

    var isNavigated = false

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.searchView -> {
                searchHouseHoldList(
                    binding.searchView.text
                        .trim()
                        .toString(),
                    patientLinkedDetails.villageId,
                )
            }
            R.id.btnAddHousehold -> {
                withLocationCheck({
                    val intent = Intent(requireContext(), ConsentFormActivity::class.java)
                    intent.putExtra(VILLAGE_ID, patientLinkedDetails.villageId)
                    intent.putExtra(IS_PHU_WALK_INS_FLOW, true)
                    intent.putExtra(isCreateHouseholdForPhu, true)
                    intent.putExtra(MEMBER_ID, patientLinkedDetails.lMemberId.toLongOrNull())
                    intent.putExtra(FhirMemberID, patientLinkedDetails.memberId.toLongOrNull())
                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    startActivity(intent)
                })
            }
        }
    }

    private fun searchHouseHoldList(
        input: String,
        villageId: Long,
    ) {
        viewModel
            .getSearchHouseholdsLiveData(
                if (input.isNotEmpty() && ((input[0].isLetter() && input.length >= 3) || input[0].isDigit())) {
                    input
                } else {
                    ""
                },
                villageId,
            ).observe(viewLifecycleOwner) {
                listVisibility(it.isNullOrEmpty() || it.size == 0)
                binding.tvHPatientCount.text =
                    it.size
                        .toString()
                        .plus(getString(R.string.households_in))
                        .plus(patientLinkedDetails.villageName)
                // Set adapter
                binding.rcLinkPatientList.layoutManager = LinearLayoutManager(requireContext())
                binding.rcLinkPatientList.adapter =
                    PhuHouseHoldListAdapter(it, activity as PhuLinkCallback)
            }
    }
}
