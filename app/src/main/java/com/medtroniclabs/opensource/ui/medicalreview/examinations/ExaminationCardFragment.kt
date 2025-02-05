package com.medtroniclabs.opensource.ui.medicalreview.examinations

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.databinding.FragmentExaminationCardBinding
import com.medtroniclabs.opensource.formgeneration.ExaminationGenerator
import com.medtroniclabs.opensource.formgeneration.ExaminationListener
import com.medtroniclabs.opensource.formgeneration.model.FormLayout
import com.medtroniclabs.opensource.formgeneration.utility.CheckBoxDialog
import com.medtroniclabs.opensource.ui.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ExaminationCardFragment : BaseFragment(), ExaminationListener {

    private val viewModel: ExaminationCardViewModel by activityViewModels()
    private lateinit var binding: FragmentExaminationCardBinding
    private lateinit var examinationGenerator: ExaminationGenerator

    companion object{
        const val TAG = "ExaminationCardFragment"
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentExaminationCardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        attachObservers()
        viewModel.getExaminationQuestionsByWorkFlow(viewModel.workFlowType)
    }

    private fun attachObservers() {
        viewModel.examinationQuestionsLiveData.observe(viewLifecycleOwner) {
            examinationGenerator.populateExaminationView(it)
        }
    }

    private fun initView() {
        examinationGenerator = ExaminationGenerator(binding.root.context, binding.llFamilyRoot,this, translate = SecuredPreference.getIsTranslationEnabled())
    }

    override fun onDialogueCheckboxListener(
        id: String,
        formLayout: FormLayout,
        resultMap: Any?,
        diseaseName: String
    ) {
        CheckBoxDialog.newInstance(id, resultMap) { map ->
            examinationGenerator.validateCheckboxDialogue(id, map,diseaseName)
        }.show(childFragmentManager, CheckBoxDialog.TAG)
    }

    override fun setResultHashMap(resultMap: HashMap<String, Any>) {
        viewModel.examinationResultHashMap = resultMap
    }

}