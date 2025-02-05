package com.medtroniclabs.opensource.ncd.medicalreview.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.databinding.ChildViewLifeStyleBinding
import com.medtroniclabs.opensource.databinding.FragmentNcdLifestyleAssessmentBinding
import com.medtroniclabs.opensource.databinding.LayoutLifeStyleBinding
import com.medtroniclabs.opensource.db.entity.LifeStyleAnswer
import com.medtroniclabs.opensource.db.entity.LifeStyleAnswerUIModel
import com.medtroniclabs.opensource.db.entity.LifeStyleUIModel
import com.medtroniclabs.opensource.db.entity.LifestyleEntity
import com.medtroniclabs.opensource.formgeneration.extension.markMandatory
import com.medtroniclabs.opensource.ncd.data.InitialLifeStyle
import com.medtroniclabs.opensource.ncd.medicalreview.LifeStyleCustomView
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil.LIFE_STYLE_ALCOHOL
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil.LIFE_STYLE_NUT
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil.LIFE_STYLE_SMOKE
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil.Yes_Currently
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil.Yes_Past
import com.medtroniclabs.opensource.ncd.medicalreview.viewmodel.NCDLifestyleAssessmentViewModel
import com.medtroniclabs.opensource.ui.BaseFragment

class NCDLifestyleAssessmentFragment : BaseFragment() {

    private lateinit var binding: FragmentNcdLifestyleAssessmentBinding
    private val viewModel: NCDLifestyleAssessmentViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNcdLifestyleAssessmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        const val TAG = "NCDLifestyleAssessmentFragment"
        fun newInstance(): NCDLifestyleAssessmentFragment {
            return NCDLifestyleAssessmentFragment()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        setListener()
        attachObservers()
    }

    private fun attachObservers() {
        viewModel.getLifeStyleLiveData.observe(viewLifecycleOwner) {
            getLifeStyleUIModel(it).let { uiList ->
                viewModel.lifeStyleListUIModel = uiList
                loadLifeStyle(uiList)
            }
        }
    }

    private fun loadLifeStyle(list: List<LifeStyleUIModel>?) {
        binding.llLifeStyleParentHolder.removeAllViews()
        list?.forEach { style ->
            val lifeStyleBinding = LayoutLifeStyleBinding.inflate(LayoutInflater.from(context))
            lifeStyleBinding.tvTitle.text = style.lifestyle
            lifeStyleBinding.tvTitle.markMandatory()
            val selectedItem =
                viewModel.lifestyle?.find { it.id == style._id }
            selectedItem?.let { style.lifestyleAnswer.find { it.name == selectedItem.lifestyleAnswer } }
                ?.apply {
                    isSelected = true
                    comments = selectedItem.comments
                }
            val answerView = getAnswersView(style) { model, answerModel ->
                loadDependant(model, lifeStyleBinding, answerModel)
            }
            lifeStyleBinding.llLifeStyleHolder.addView(answerView)
            binding.llLifeStyleParentHolder.addView(lifeStyleBinding.root)
        }
    }

    private fun loadDependant(
        model: LifeStyleUIModel,
        lifeStyleBinding: LayoutLifeStyleBinding,
        answerModel: LifeStyleAnswerUIModel
    ) {
        if (answerModel.isAnswerDependent) {
            lifeStyleBinding.llDependentHolder.visibility = View.VISIBLE
            lifeStyleBinding.llDependentHolder.removeAllViews()
            lifeStyleBinding.llDependentHolder.addView(getDependView(model, answerModel))
        } else {
            lifeStyleBinding.llDependentHolder.visibility = View.GONE
        }
    }

    private fun getDependView(
        model: LifeStyleUIModel,
        answerModel: LifeStyleAnswerUIModel
    ): View {
        val binding = ChildViewLifeStyleBinding.inflate(LayoutInflater.from(context))
        when (model.lifestyleType) {
            LIFE_STYLE_SMOKE -> {
                when (answerModel.name) {
                    Yes_Currently -> {
                        binding.tvChildTitle.text = getString(R.string.tobacoo_question_child_one)
                    }

                    Yes_Past -> {
                        binding.tvChildTitle.text =
                            getString(R.string.tobacoo_question_child_one_smoke)
                    }

                    else -> {
                        binding.tvChildTitle.text = getString(R.string.tobacoo_question_comment)
                    }
                }
            }

            LIFE_STYLE_ALCOHOL -> {
                when (answerModel.name) {
                    Yes_Currently -> {
                        binding.tvChildTitle.text = getString(R.string.drinks_per_week)
                    }

                    Yes_Past -> {
                        binding.tvChildTitle.text =
                            getString(R.string.tobacoo_question_child_one_smoke)
                    }

                    else -> {
                        binding.tvChildTitle.text = getString(R.string.alcohol_question_comment)
                    }
                }
            }

            LIFE_STYLE_NUT -> {
                binding.tvChildTitle.text = getString(R.string.comments_diet_nutrition)
            }
        }
        binding.tvChildTitle.markMandatory()
        if (!answerModel.comments.isNullOrBlank())
            binding.etChildUserInput.setText(answerModel.comments)
        binding.etChildUserInput.addTextChangedListener { childAnswer ->
            if (childAnswer.isNullOrBlank()) {
                answerModel.comments = null
            } else {
                answerModel.comments = childAnswer.toString()
            }
            addLifestyleAnswer(model, answerModel)
        }
        return binding.root
    }

    private fun addLifestyleAnswer(
        questionModel: LifeStyleUIModel,
        answerModel: LifeStyleAnswerUIModel
    ) {
        val model =
            viewModel.lifestyle?.find { it.id == questionModel._id }
        if (model == null) {
            val listItem = InitialLifeStyle(
                questionModel.lifestyle,
                answerModel.name,
                questionModel._id,
                isAnswerDependent = answerModel.isAnswerDependent,
                comments = answerModel.comments
            )
            viewModel.lifestyle?.add(listItem)
        } else {
            model.comments = answerModel.comments
        }
    }

    private fun getAnswersView(
        style: LifeStyleUIModel,
        callback: ((model: LifeStyleUIModel, answerModel: LifeStyleAnswerUIModel) -> Unit?)? = null
    ): View {
        val view = LifeStyleCustomView(requireContext())
        view.tag = style._id
        view.addViewElements(style, callback)
        return view
    }


    private fun getLifeStyleUIModel(list: List<LifestyleEntity>?): List<LifeStyleUIModel> {
        val lifeStyle = ArrayList<LifeStyleUIModel>()
        list?.forEach { model ->
            lifeStyle.add(
                LifeStyleUIModel(
                    model.id,
                    model.displayOrder,
                    model.displayValue ?: "",
                    getAnswerLifeStyle(model.answers),
                    model.type,
                    null,
                    model.value
                )
            )
        }
        return lifeStyle
    }

    private fun getAnswerLifeStyle(lifestyleAnswer: ArrayList<LifeStyleAnswer>): ArrayList<LifeStyleAnswerUIModel> {
        val lifeStyleUIAnswer = ArrayList<LifeStyleAnswerUIModel>()
        lifestyleAnswer.forEach {
            lifeStyleUIAnswer.add(
                LifeStyleAnswerUIModel(
                    question = it.name,
                    name = it.name,
                    cultureAnswerValue = it.displayValue,
                    isAnswerDependent = it.isAnswerDependent,
                    value = it.value
                )
            )
        }
        return lifeStyleUIAnswer
    }

    private fun setListener() {
        /* never used  */
    }

    private fun initView() {
        viewModel.getLifestyleAssessment(true)
    }

    private fun getSelectedLifeStyleList(list: List<LifeStyleUIModel>): ArrayList<InitialLifeStyle> {
        val resultList = ArrayList<InitialLifeStyle>()
        list.forEach { model ->
            val selectedModel = model.lifestyleAnswer.filter { it.isSelected }
            if (selectedModel.isNotEmpty()) {
                resultList.add(
                    InitialLifeStyle(
                        model.lifestyle,
                        selectedModel[0].name,
                        model._id,
                        isAnswerDependent = selectedModel[0].isAnswerDependent,
                        comments = selectedModel[0].comments,
                        questionValue = model.value,
                        answerValue = selectedModel[0].value
                    )
                )
            } else {
                resultList.add(InitialLifeStyle(id = model._id))
            }
        }
        return resultList
    }

    private fun showErrorMessage(message: String, view: TextView) {
        view.visible()
        view.text = message
    }

    private fun hideErrorMessage(view: TextView) {
        view.gone()
    }

    fun validateInput(): Pair<Boolean, AppCompatTextView> {
        viewModel.lifeStyleListUIModel?.let {
            viewModel.lifestyle = getSelectedLifeStyleList(it)
        }
        var isValid = true
        if (viewModel.lifestyle != null) {
            viewModel.lifestyle?.let {
                val unAnsweredList = it.filter { it.lifestyleAnswer == null }
                if (unAnsweredList.isNotEmpty()) {
                    isValid = false
                    showErrorMessage(
                        getString(R.string.validation_message_lifestyle),
                        binding.tvErrorLifeStyle
                    )
                } else {
                    val unCommentedList =
                        it.filter { it.isAnswerDependent && it.comments == null }
                    if (unCommentedList.isNotEmpty()) {
                        isValid = false
                        showErrorMessage(
                            getString(R.string.validation_message_lifestyle),
                            binding.tvErrorLifeStyle
                        )
                    } else
                        hideErrorMessage(binding.tvErrorLifeStyle)
                }
            }
        } else {
            isValid = false
            showErrorMessage(
                getString(R.string.validation_message_lifestyle),
                binding.tvErrorLifeStyle
            )
        }
        return Pair(isValid, binding.tvErrorLifeStyle)
    }
}