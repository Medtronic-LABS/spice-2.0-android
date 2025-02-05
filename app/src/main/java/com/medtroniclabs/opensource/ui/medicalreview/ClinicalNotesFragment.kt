package com.medtroniclabs.opensource.ui.medicalreview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.databinding.FragmentClinicalNotesBinding
import com.medtroniclabs.opensource.formgeneration.extension.markMandatory
import com.medtroniclabs.opensource.ui.BaseFragment
import com.medtroniclabs.opensource.ui.medicalreview.abovefiveyears.ClinicalNotesViewModel
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewDefinedParams
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ClinicalNotesFragment : BaseFragment() {

    private lateinit var binding: FragmentClinicalNotesBinding
    private val viewModel : ClinicalNotesViewModel by activityViewModels()

    companion object{
        const val TAG = "ClinicalNotesFragment"
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentClinicalNotesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews()
    }

    private fun initializeViews() {
        binding.tvClinicalNotesTitle.markMandatory()
        binding.etClinicalNotes.addTextChangedListener {
            it?.let {
                viewModel.enteredClinicalNotes = it.toString()
                viewModel.handleSubmitButtonState()
                setFragmentResult(
                    MedicalReviewDefinedParams.CLINICAL_NOTES, bundleOf(
                        MedicalReviewDefinedParams.Notes to true)
                )
            }
        }
        if (viewModel.isMotherPnc){
            binding.etClinicalNotes.setText(viewModel.enteredClinicalNotes)
        }

    }

    fun validateInput():Boolean {
        if (binding.etClinicalNotes.text?.trim().toString().isBlank()) {
            binding.tvClinicalNoteErrorMessage.visible()
            binding.etClinicalNotes.requestFocus()
            return false
        }
        binding.tvClinicalNoteErrorMessage.gone()
        return true
    }
    fun refreshFragment() {
        binding.etClinicalNotes.text?.clear()
        setFragmentResult(
            MedicalReviewDefinedParams.CLINICAL_NOTES, bundleOf(
                MedicalReviewDefinedParams.Notes to false)
        )
    }
    fun reloadFragment(value: String) {
        binding.etClinicalNotes.setText(value)
    }
}