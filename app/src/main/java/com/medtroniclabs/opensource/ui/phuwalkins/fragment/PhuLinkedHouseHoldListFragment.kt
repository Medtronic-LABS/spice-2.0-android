package com.medtroniclabs.opensource.ui.phuwalkins.fragment

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
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.setTextChangeListener
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.data.offlinesync.model.UnAssignedHouseholdMemberDetail
import com.medtroniclabs.opensource.databinding.FragmentPhuLinkedHosueHoldListBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.ui.BaseFragment
import com.medtroniclabs.opensource.ui.phuwalkins.adapter.PhuHouseHoldListAdapter
import com.medtroniclabs.opensource.ui.phuwalkins.listener.PhuLinkCallback
import com.medtroniclabs.opensource.ui.phuwalkins.viewmodel.PhuWalkInsViewModel


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
        fun newInstance(patientLinkedDetails: UnAssignedHouseholdMemberDetail): PhuLinkedHouseHoldListFragment {
            return PhuLinkedHouseHoldListFragment(patientLinkedDetails)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPhuLinkedHosueHoldListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHouseholdLinkedDetails()
        initObserver()
        binding.includedHousehold.linkCallDetailsBtn.setOnClickListener {
            viewModel.memberID = patientLinkedDetails.memberId.toLong()
            val dialIntent = Intent(Intent.ACTION_DIAL)
            dialIntent.data = Uri.parse("tel:${patientLinkedDetails.phoneNumber}")
            dialerLauncher.launch(dialIntent)
        }
        searchHouseHoldMembers()
    }

    private fun initObserver() {
        viewModel.getFilteredHouseholdsLiveData(patientLinkedDetails.villageId)
            .observe(viewLifecycleOwner) {
                listVisibility(it.isNullOrEmpty() || it.size == 0)
                binding.tvHPatientCount.text =
                    it.size.toString().plus(getString(R.string.households_in))
                        .plus(patientLinkedDetails.villageName)
                // Set adapter
                binding.rcLinkPatientList.layoutManager = LinearLayoutManager(requireContext())
                binding.rcLinkPatientList.adapter =
                    PhuHouseHoldListAdapter(it, activity as PhuLinkCallback)
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
            searchHouseHoldList(input,patientLinkedDetails.villageId)
        }
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
            patientNameAgeGender.text =
                formatPatientDemographics(requireContext(), patientLinkedDetails)
            patientVillage.text = patientLinkedDetails.villageName
            val phoneNumberCode=SecuredPreference.getPhoneNumberCode()
            patientMobile.text = "+$phoneNumberCode ${patientLinkedDetails.phoneNumber}"
        }
    }

    private fun formatPatientDemographics(
        context: Context,
        patient: UnAssignedHouseholdMemberDetail
    ): String {
        return viewModel.getPatientName(context, patient.name, patient.dateOfBirth, patient.gender)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.searchView -> {
                searchHouseHoldList(
                    binding.searchView.text.trim().toString(),
                    patientLinkedDetails.villageId
                )
            }
        }
    }

    private fun searchHouseHoldList(input: String, villageId: Long) {
        viewModel.getSearchHouseholdsLiveData(
            if (input.isNotEmpty() && ((input[0].isLetter() && input.length >= 3) || input[0].isDigit())) {
                input
            } else {
                ""
            },villageId
        ).observe(viewLifecycleOwner) {
            listVisibility(it.isNullOrEmpty() || it.size == 0)
            binding.tvHPatientCount.text =
                it.size.toString().plus(getString(R.string.households_in))
                    .plus(patientLinkedDetails.villageName)
            // Set adapter
            binding.rcLinkPatientList.layoutManager = LinearLayoutManager(requireContext())
            binding.rcLinkPatientList.adapter =
                PhuHouseHoldListAdapter(it, activity as PhuLinkCallback)
        }
    }

}