package com.medtroniclabs.opensource.ncd.assessment.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.medtroniclabs.opensource.appextensions.setDialogPercent
import com.medtroniclabs.opensource.databinding.DialogBpLogListBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.ncd.assessment.adapter.BPLogListAdapter
import com.medtroniclabs.opensource.ncd.assessment.viewmodel.BloodPressureViewModel

class BPLogListDialog(private val addNewReading: () -> Unit) : DialogFragment(),
    View.OnClickListener {
    private lateinit var binding: DialogBpLogListBinding
    private val viewModel: BloodPressureViewModel by activityViewModels()

    companion object {
        const val TAG = "BPListDialog"
        fun newInstance(addNewReading: () -> Unit): BPLogListDialog {
            return BPLogListDialog(addNewReading)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogBpLogListBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        isCancelable = false
        return binding.root
    }

    private fun initView() {
        with(binding) {
            rvBpLogList.layoutManager = LinearLayoutManager(binding.root.context)
            ivClose.safeClickListener(this@BPLogListDialog)
            btnAddNewReading.safeClickListener(this@BPLogListDialog)
        }

        viewModel.bpLogListResponseLiveData.value?.data?.bpLogList?.let { list ->
            binding.rvBpLogList.adapter = BPLogListAdapter(list)
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.ivClose.id -> {
                dismiss()
            }
            binding.btnAddNewReading.id -> {
                dismiss()
                addNewReading.invoke()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        setDialogPercent(55,65)
    }
}