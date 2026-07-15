package org.medtroniclabs.uhis.ui.cbs.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.common.DefinedParams.CbsNotifiableCondition
import org.medtroniclabs.uhis.common.DefinedParams.ICCM_DIARRHEA_NOTIFIABLE_CONDITION
import org.medtroniclabs.uhis.common.DefinedParams.ICCM_FEVER_NOTIFIABLE_CONDITION
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.databinding.CheckboxDialogLayoutBinding
import org.medtroniclabs.uhis.db.entity.SignsAndSymptomsEntity
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.utility.CheckboxDialogAdapter
import org.medtroniclabs.uhis.ui.assessment.viewmodel.AssessmentViewModel

class CbsCheckBoxDialog() : DialogFragment(), View.OnClickListener {
    private var callback: ((result: ArrayList<HashMap<String, Any>>) -> Unit)? = null
    var resultMap: Any? = null
    var title: String? = null
    var autoPopulate: List<Pair<String, Boolean>> = emptyList()

    constructor(
        callback: (result: ArrayList<HashMap<String, Any>>) -> Unit,
        resultMap: Any?,
        title: String?,
        autoPopulate: List<Pair<String, Boolean>> = emptyList(),
    ) : this() {
        this.callback = callback
        this.resultMap = resultMap
        this.title = title
        this.autoPopulate = autoPopulate
    }

    lateinit var binding: CheckboxDialogLayoutBinding
    private val viewModel: AssessmentViewModel by activityViewModels()

    companion object {
        const val TAG = "CbsCheckBoxDialog"
        private const val KEY_TYPE = "KEY_TYPE"

        fun newInstance(
            key: String,
            resultMap: Any?,
            title: String? = null,
            autoPopulate: List<Pair<String, Boolean>> = emptyList(),
            callback: (result: ArrayList<HashMap<String, Any>>) -> Unit,
        ): CbsCheckBoxDialog {
            val args = Bundle()
            args.putString(KEY_TYPE, key)
            val fragment = CbsCheckBoxDialog(callback, resultMap, title, autoPopulate)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = CheckboxDialogLayoutBinding.inflate(inflater, container, false)
        val window: Window? = dialog?.window
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initializeView()
        getRespectiveList(requireArguments().getString(KEY_TYPE))
        attachObserver()
    }

    private fun attachObserver() {
        viewModel.symptomTypeListResponse.observe(viewLifecycleOwner) { list ->
            binding.labelHeader.text =
                title.takeIf { !it.isNullOrEmpty() } ?: getString(R.string.symptoms)

            if (list.isEmpty()) {
                binding.rvItems.adapter = null
                return@observe
            }

            val shouldAutoPopulate = autoPopulate.isNotEmpty()

            if (shouldAutoPopulate) {
                val autoPopulateMap = autoPopulate.toMap()
                list.forEach { symptom ->
                    autoPopulateMap[symptom.value]?.let { isEnabled ->
                        symptom.isSelected = true
                        symptom.isEnabled = isEnabled
                    }
                }
            }

            val adapterList = if (resultMap != null && resultMap is ArrayList<*>) {
                getSelectedSymptomList(list, resultMap as ArrayList<*>)
            } else {
                list
            }

            binding.rvItems.adapter = CheckboxDialogAdapter(
                adapterList,
                translate = SecuredPreference.getIsTranslationEnabled(),
            )
        }
    }

    private fun getSelectedSymptomList(
        list: List<SignsAndSymptomsEntity>,
        resultMap: ArrayList<*>,
    ): List<SignsAndSymptomsEntity> {
        var value = emptyList<SignsAndSymptomsEntity>()
        if (resultMap.isNotEmpty()) {
            resultMap.forEach { resultSymptom ->
                if (resultSymptom is Map<*, *>) {
                    val map = list.find { it._id == resultSymptom[DefinedParams.ID] }
                    map?.isSelected = true
                }
            }
            value = list
        } else {
            value = list
        }
        if (autoPopulate.isNotEmpty()) {
            val autoPopulateMap = autoPopulate.toMap()
            value.forEach { symptom ->
                autoPopulateMap[symptom.value]?.let { isEnabled ->
                    symptom.isSelected = true
                    symptom.isEnabled = isEnabled
                }
            }
        }
        return value
    }

    private fun getRespectiveList(key: String?) {
        key?.let {
            viewModel.getSymptomListByTypes(
                listOf(ICCM_DIARRHEA_NOTIFIABLE_CONDITION.lowercase(), ICCM_FEVER_NOTIFIABLE_CONDITION.lowercase(), CbsNotifiableCondition.lowercase()),
            )
        }
    }

    private fun initializeView() {
        binding.rvItems.layoutManager = LinearLayoutManager(binding.root.context)
        binding.btnCancel.safeClickListener(this)
        binding.btnOkay.safeClickListener(this)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
        )
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.btnCancel -> {
                dismiss()
            }

            R.id.btnOkay -> {
                val adapter = binding.rvItems.adapter
                if (adapter != null && adapter is CheckboxDialogAdapter) {
                    callback?.invoke(
                        adapter.getSelectedItems(),
                    )
                    dismiss()
                }
            }
        }
    }
}
