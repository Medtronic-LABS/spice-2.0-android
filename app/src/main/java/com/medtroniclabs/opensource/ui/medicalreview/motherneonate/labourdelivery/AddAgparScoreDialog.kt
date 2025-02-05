package com.medtroniclabs.opensource.ui.medicalreview.motherneonate.labourdelivery

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.setWidth
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.databinding.FragmentAddAgparScoreBinding
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.labourdelivery.viewmodel.LabourDeliveryViewModel

class AddAgparScoreDialog : DialogFragment() {

    private var _binding: FragmentAddAgparScoreBinding? = null
    private val viewModel: LabourDeliveryViewModel by activityViewModels()

    val binding: FragmentAddAgparScoreBinding
        get() = _binding!!

    companion object {
        const val TAG = "AgparScoreDialog"
        fun newInstance(): AddAgparScoreDialog {
            return AddAgparScoreDialog()
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddAgparScoreBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        isCancelable = false
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI()
        initListeners()
    }

    override fun onStart() {
        super.onStart()
        handleDialogSize()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        handleDialogSize()
    }

    private fun handleDialogSize() {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val width = if (CommonUtils.checkIsTablet(requireContext())) {
            if (isLandscape) 65 else 90
        } else {
            if (isLandscape) 65 else 90
        }
        setWidth(width)
    }


    private fun initListeners() {

        binding.etAgparScore.doAfterTextChanged { editable ->
            editable?.toString()?.toIntOrNull()?.let { score ->
                binding.apply {
                    if (score <= 2) {
                        btnOkay.isEnabled = true
                        tvAgparErrorLabel.gone()
                    } else {
                        btnOkay.isEnabled = false
                        tvAgparErrorLabel.visible()
                    }
                }
            } ?: kotlin.run {
                binding.btnOkay.isEnabled = false
                binding.tvAgparErrorLabel.gone()
            }
        }


        binding.btnOkay.setOnClickListener {
            viewModel.updateAgparScore(binding.etAgparScore.text.toString())
            viewModel.validateSubmitButtonState()
            dismiss()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.ivClose.setOnClickListener {
            dismiss()
        }
    }


    private fun initUI() {

        viewModel.agparSelectedScore?.let {
            binding.etAgparScore.setText(it)
        }

        val rowName = viewModel.getAgparRowName()
        val columnName = viewModel.getAgparColumnName()

        val rowDislayName = if(rowName != null) getString(rowName) else getString(R.string.separator_double_hyphen)
        val columnDisplayName = if(columnName != null) getString(columnName) else getString(R.string.separator_double_hyphen)

        binding.tvAgparIndicatorLabel.text = "$rowDislayName - $columnDisplayName"
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}