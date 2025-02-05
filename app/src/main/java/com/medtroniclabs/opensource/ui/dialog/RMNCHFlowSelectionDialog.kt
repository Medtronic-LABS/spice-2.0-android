package com.medtroniclabs.opensource.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.common.CommonUtils.getOptionMap
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.databinding.FragmentRmnchSelectionDialogBinding
import com.medtroniclabs.opensource.formgeneration.model.FormLayout
import com.medtroniclabs.opensource.formgeneration.ui.SingleSelectionCustomView
import com.medtroniclabs.opensource.ui.MenuConstants.DialogResult
import com.medtroniclabs.opensource.ui.MenuConstants.WorkFlowName
import com.medtroniclabs.opensource.ui.assessment.rmnch.RMNCH
import com.medtroniclabs.opensource.ui.home.ToolsViewModel

class RMNCHFlowSelectionDialog : DialogFragment(), View.OnClickListener {

    private lateinit var binding: FragmentRmnchSelectionDialogBinding

    private val viewModel: ToolsViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
    }

    companion object {
        const val TAG = "RMNCHFlowSelectionDialog"

        fun newInstance(): RMNCHFlowSelectionDialog {
            return RMNCHFlowSelectionDialog()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRmnchSelectionDialogBinding.inflate(layoutInflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        setListener()
    }

    private fun initView() {

        getRMNCHFlowData().let {
            val view = SingleSelectionCustomView(binding.root.context)
            view.tag = TAG
            view.addViewElements(
                it,
                SecuredPreference.getIsTranslationEnabled(),
                viewModel.resultRMNCHFlowHashMap,
                Pair(TAG,null),
                FormLayout(viewType = "", id = "", title = "", visibility = "", optionsList = null),
                singleSelectionCallback
            )
            binding.selectionGroup.addView(view)
        }

    }

    private fun setListener() {
        binding.ivClose.setOnClickListener(this)
    }

    private var singleSelectionCallback: ((selectedID: Any?, elementId: Pair<String,String?>, serverViewModel: FormLayout, name: String?) -> Unit)? =
        { selectedID, _, _, _ ->
            saveSelectedOptionValue(selectedID)
        }

    private fun saveSelectedOptionValue(
        selectedId: Any?
    ) {
        selectedId?.let {
            setFragmentResult(DialogResult, bundleOf(WorkFlowName to it))
            dismiss()
        }
    }

    private fun getRMNCHFlowData(): ArrayList<Map<String, Any>> {
        val flowList = ArrayList<Map<String, Any>>()
        flowList.add(getOptionMap(RMNCH.ANC,getString(R.string.anc)))
        flowList.add(getOptionMap(RMNCH.PNC,getString(R.string.pnc)))
        return flowList
    }

    override fun onClick(v: View) {
        when (v.id) {
            binding.ivClose.id -> {
                dismiss()
            }
        }
    }

}