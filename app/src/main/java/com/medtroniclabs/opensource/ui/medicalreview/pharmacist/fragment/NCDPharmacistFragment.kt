package com.medtroniclabs.opensource.ui.medicalreview.pharmacist.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.data.DispensePrescriptionResponse
import com.medtroniclabs.opensource.databinding.FragmentNcdPharmacistBinding
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseFragment
import com.medtroniclabs.opensource.ui.medicalreview.pharmacist.adapter.NCDPrescriptionRefillAdapter
import com.medtroniclabs.opensource.ui.medicalreview.pharmacist.viewModel.NCDPharmacistViewModel

class NCDPharmacistFragment : BaseFragment() {

    private lateinit var binding: FragmentNcdPharmacistBinding
    private lateinit var prescriptionRefillAdapter: NCDPrescriptionRefillAdapter
    private val ncdPharmacistViewModel: NCDPharmacistViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNcdPharmacistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        attachObserver()
    }


    companion object {
        const val TAG = "NCDPharmacistFragment"
        fun newInstance(): NCDPharmacistFragment {
            return NCDPharmacistFragment()
        }
    }

    private fun initView() {
        binding.rvPrescriptionRefillList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPrescriptionRefillList.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                DividerItemDecoration.VERTICAL
            )
        )

        prescriptionRefillAdapter = NCDPrescriptionRefillAdapter()
        binding.rvPrescriptionRefillList.adapter = prescriptionRefillAdapter
    }


    private fun attachObserver() {
        ncdPharmacistViewModel.prescriptionDispenseLiveData.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.SUCCESS -> {
                    hideProgress()
                    loadPrescriptionRefillList(resourceState.data)
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }
            }
        }
    }

    private fun loadPrescriptionRefillList(data: ArrayList<DispensePrescriptionResponse>?) {
        binding.tvNoRecord.gone()
        data?.let { list ->
            if (list.isEmpty()) {
                binding.rvPrescriptionRefillList.gone()
                binding.tvNoRecord.visible()
            } else {
                binding.rvPrescriptionRefillList.visible()
                binding.rvPrescriptionRefillList.adapter = prescriptionRefillAdapter
                prescriptionRefillAdapter.setData(list)
            }
        }
    }

}