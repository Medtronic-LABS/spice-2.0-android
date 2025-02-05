package com.medtroniclabs.opensource.ui.mypatients.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.medtroniclabs.opensource.databinding.FragmentMedicalReviewPatientExaminationBinding
import com.medtroniclabs.opensource.ui.TagListCustomView
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.MedicalReviewBaseViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MedicalReviewPatientExaminationFragment : Fragment() {

    private lateinit var binding: FragmentMedicalReviewPatientExaminationBinding
    private lateinit var examinationsTagView: TagListCustomView
    private lateinit var complaintsTagView: TagListCustomView
    private val viewModel: MedicalReviewBaseViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentMedicalReviewPatientExaminationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews()
    }

    private fun initializeViews() {
        examinationsTagView = TagListCustomView(binding.root.context, binding.tagPhysicalExamination)
        complaintsTagView =  TagListCustomView(binding.root.context, binding.tagViewPresentingComplaints)
        examinationsTagView.addChipItemList(viewModel.getExamsList())
        complaintsTagView.addChipItemList(viewModel.getComplaintsList())
    }

    fun getSelectedExamsAndComplaints(){
        Timber.tag("Selected Examinations").d(examinationsTagView.getSelectedTags().toString())
        Timber.tag("Selected complaints").d(complaintsTagView.getSelectedTags().toString())
    }
}