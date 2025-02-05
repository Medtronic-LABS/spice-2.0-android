package com.medtroniclabs.opensource.ui.landing

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.databinding.FragmentLoadingDialogBinding

class LoadingDialogFragment : DialogFragment() {

    private lateinit var binding: FragmentLoadingDialogBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLoadingDialogBinding.inflate(layoutInflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(R.color.transparent)
        isCancelable = false
        return binding.root
    }

    companion object {
        const val TAG = "LoadingDialogFragment"
        fun newInstance() =
            LoadingDialogFragment()
    }

    override fun onStart() {
        super.onStart()
        handleDialogHeight()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        handleDialogHeight()
    }

    private fun handleDialogHeight() {
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }
}