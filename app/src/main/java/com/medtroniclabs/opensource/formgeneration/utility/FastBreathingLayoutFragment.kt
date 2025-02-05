package com.medtroniclabs.opensource.formgeneration.utility

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.medtroniclabs.opensource.databinding.FragmentFastBreathingLayoutBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener

class FastBreathingLayoutFragment : DialogFragment(), View.OnClickListener {
    lateinit var binding: FragmentFastBreathingLayoutBinding

    companion object {
        const val TAG = "FastBreathingLayoutFragment"
        fun newInstance(): FastBreathingLayoutFragment {
            return FastBreathingLayoutFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFastBreathingLayoutBinding.inflate(inflater, container, false)
        val window: Window? = dialog?.window
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        return  binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = false
        initializeViews()
        setListeners()
    }

    private fun initializeViews() {
        binding.rvInfoList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter  = DosageInstructionAdapter(InformationUtils().getFastBreathingInstructions(requireContext()))
        }
    }

    private fun setListeners() {
        binding.ivClose.safeClickListener(this)
        binding.btnClose.safeClickListener(this)
    }

    override fun onClick(view: View) {
        when(view.id){
            binding.ivClose.id, binding.btnClose.id -> {
                dismiss()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

}