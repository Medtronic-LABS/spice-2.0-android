package com.medtroniclabs.opensource.formgeneration.utility

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.common.DefinedParams.ID
import com.medtroniclabs.opensource.databinding.FragmentInformationLayoutBinding
import com.medtroniclabs.opensource.formgeneration.config.DefinedParams
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.formgeneration.model.InformationModel
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.MUAC
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.chestInDrawing
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.hasOedemaOfBothFeet
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.isBreastfeed
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.isConvulsionPastFewDays
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.isUnusualSleepy
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.isVomiting
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.muacCode

class InformationLayoutFragment : DialogFragment(), View.OnClickListener {
    lateinit var binding : FragmentInformationLayoutBinding

    companion object {
        const val TAG = "InformationLayoutFragment"
        fun newInstance(id: String, title: String): InformationLayoutFragment {
            val fragment = InformationLayoutFragment()
            fragment.arguments = Bundle().apply {
                putString(DefinedParams.ID, id)
                putString(DefinedParams.Title, title)
            }
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentInformationLayoutBinding.inflate(inflater, container, false)
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
        binding.tvTitle.text = arguments?.getString(DefinedParams.Title) ?: getString(R.string.instructions)
        val informationListByType: ArrayList<InformationModel>? = when(arguments?.getString(ID)){
            muacCode, MUAC-> InformationUtils().getMuacInformationListItem(requireContext())
            hasOedemaOfBothFeet -> InformationUtils().getOedemaInformationList(requireContext())
            chestInDrawing -> InformationUtils().getChestIndrawingInformation(requireContext())
            isUnusualSleepy,isVomiting,isConvulsionPastFewDays,isBreastfeed->{InformationUtils().getDangerSignsInstructions(requireContext(),arguments?.getString(ID))
            }
            else -> null
        }
        binding.rvInfoList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter  = informationListByType?.let { InformationListAdapter(it) }
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