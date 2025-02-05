package com.medtroniclabs.opensource.ncd.assessment.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.medtroniclabs.opensource.appextensions.postError
import com.medtroniclabs.opensource.appextensions.setDialogPercent
import com.medtroniclabs.opensource.databinding.DialogGlucoseLogListBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.ncd.assessment.adapter.GlucoseLogListAdapter
import com.medtroniclabs.opensource.ncd.assessment.viewmodel.GlucoseViewModel

class GlucoseLogListDialog(private val addNewReading: () -> Unit) : DialogFragment(),
    View.OnClickListener {
    private lateinit var binding: DialogGlucoseLogListBinding
    private val viewModel: GlucoseViewModel by activityViewModels()

    companion object {
        const val TAG = "GlucoseLogListDialog"
        fun newInstance(addNewReading: () -> Unit): GlucoseLogListDialog {
            return GlucoseLogListDialog(addNewReading)
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
        binding = DialogGlucoseLogListBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        isCancelable = false
        return binding.root
    }

    private fun initView() {
        with(binding) {
            rvBpLogList.layoutManager = LinearLayoutManager(binding.root.context)
            ivClose.safeClickListener(this@GlucoseLogListDialog)
            btnAddNewReading.safeClickListener(this@GlucoseLogListDialog)
        }

        viewModel.glucoseLogListResponseLiveData.value?.data?.glucoseLogList?.let { list ->
            binding.rvBpLogList.adapter = GlucoseLogListAdapter(list)
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.ivClose.id -> dismiss()
            binding.btnAddNewReading.id -> {
                dismiss()
                addNewReading.invoke()
                viewModel.glucoseLogListResponseLiveData.postError()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        setDialogPercent(55,65)
    }
}