package com.medtroniclabs.opensource.formgeneration

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.medtroniclabs.opensource.databinding.RowMentalHealthBinding
import com.medtroniclabs.opensource.formgeneration.config.DefinedParams
import com.medtroniclabs.opensource.formgeneration.extension.markMandatory
import com.medtroniclabs.opensource.formgeneration.model.MentalHealthOption
import com.medtroniclabs.opensource.mappingkey.Screening


class MentalHealthAdapter(
    val context: Context,
    val list: ArrayList<MentalHealthOption>,
    val baseId: String,
    val editList: ArrayList<Map<String, Any>>? = null,
    val isViewOnly: Boolean = false,
    val translate: Boolean = false,
    val resultMap: HashMap<String, Any>,
    private val callback: ((id: String, question: String, result: HashMap<String, Any>, isUnselect: Boolean, isClicked: Boolean) -> Unit?)? = null
) :
    RecyclerView.Adapter<MentalHealthAdapter.MentalHealthViewHolder>() {
    class MentalHealthViewHolder(val binding: RowMentalHealthBinding) :
        RecyclerView.ViewHolder(binding.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MentalHealthViewHolder {
        return MentalHealthViewHolder(
            RowMentalHealthBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }
    override fun onBindViewHolder(holder: MentalHealthViewHolder, position: Int) {
        val model = list[position]
        inflateAnswerViews(model, holder)
        val question = (model.map[Screening.Questions] as String?) ?: ""
        val questionCulture = (model.map[DefinedParams.cultureValue] as String?)
            ?: (model.map[DefinedParams.displayValue] as String?) ?: ""
        if (translate){
            if (questionCulture.isNotBlank()){
                holder.binding.tvQuestion.text = questionCulture
            } else{
                holder.binding.tvQuestion.text = question
            }
        }else {
            holder.binding.tvQuestion.text = question
        }
        holder.binding.tvQuestion.tag = question
        val isMandatory = (model.map[Screening.Mandatory] as Boolean?) ?: false
        if (isMandatory) {
            holder.binding.tvQuestion.markMandatory()
        }
    }
    private fun inflateAnswerViews(model: MentalHealthOption, holder: MentalHealthViewHolder) {
        val question = (model.map[Screening.Questions] as String?) ?: ""
        val displayOrder = (model.map[Screening.Display_Order] as? Number)?.toLong() ?: -1
        val isUnselect = (model.map[Screening.select] as? Boolean) ?: false
        val mapType = (model.map[Screening.type] as? String)
        if (model.map.containsKey(Screening.ModelAnswers) && model.map[Screening.ModelAnswers] is List<*>) {
            val optionsList =
                model.map[Screening.ModelAnswers] as? ArrayList<Map<String, Any>>
            optionsList?.let {
                optionsList.sortBy { (it[Screening.Display_Order] as? Double) }
                val view = SingleSelectionMHView(context)
                view.tag =
                    view.addViewElements(
                        Pair((model.map[DefinedParams.ID] as? Double)?.toLong() ?: -1L, translate),
                        optionsList,
                        Pair(model.selectedOption?.toLong() ?: -1L, mapType),
                        editList,
                        resultMap,
                        Triple(isViewOnly,isUnselect, question)
                    ) { questionId, answerId, score, answerName, isClicked ->
                        val result = HashMap<String, Any>()
                        result[Screening.Question_Id] = questionId
                        result[Screening.Answer_Id] = answerId
                        result[Screening.Display_Order] = displayOrder
                        result[Screening.mentalHealthScore] = score
                        result[Screening.MHAnswer] = answerName
                        callback?.invoke(
                            baseId,
                            question,
                            result,
                            isUnselect,
                            isClicked
                        )
                    }
                holder.binding.llAnswerRoot.addView(view)
            }
        }
    }
    override fun getItemCount(): Int {
        return list.size
    }
}