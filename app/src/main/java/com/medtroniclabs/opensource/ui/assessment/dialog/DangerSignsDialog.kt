package com.medtroniclabs.opensource.ui.assessment.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.medtroniclabs.opensource.databinding.FragmentDangersignsDialogBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.isBreastfeed
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.isConvulsionPastFewDays
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.isUnusualSleepy
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.isVomiting
import com.medtroniclabs.opensource.R

class DangerSignsDialog : DialogFragment(), View.OnClickListener {

    private lateinit var binding: FragmentDangersignsDialogBinding


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDangersignsDialogBinding.inflate(layoutInflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        isCancelable = false
        return binding.root
    }

    companion object {
        const val TAG = "DangerSignsDialog"

        fun newInstance(id: String): DangerSignsDialog {
            val bundle = Bundle().apply {
              putString("id", id)
            } // Assuming memberID is passed as argument}
            val fragment = DangerSignsDialog()
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setListener()

        arguments?.getString("id")?.let {
            when (it) {
                isUnusualSleepy->{
                    binding.dangerSignType.text = getString(R.string.unconscious_unusually_sleepy)
                }
                isConvulsionPastFewDays->{
                    binding.dangerSignType.text = getString(R.string.convulsions)
                }
                isVomiting->{
                    binding.dangerSignType.text = getString(R.string.vomits_everything)
           }
                isBreastfeed->{
                    binding.dangerSignType.text = getString(R.string.unable_to_drink_or_breastfeed)
              }
            }
        }
    }

    private fun setListener() {
        binding.btnLink.safeClickListener(this)
    }


    override fun onClick(view: View) {
        when (view.id) {
            binding.btnLink.id -> {
                okayButtonClickListener?.onDangerSignsClicked()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
    private var okayButtonClickListener: DangerSignsClickListener? = null

    fun setDangerSignListener(listener: DangerSignsClickListener) {
        this.okayButtonClickListener = listener
    }
    interface DangerSignsClickListener {
        fun onDangerSignsClicked()
    }
}