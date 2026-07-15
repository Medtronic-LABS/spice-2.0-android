package org.medtroniclabs.uhis.ui.patient.fragment

import android.content.Intent
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
import org.medtroniclabs.uhis.databinding.QualipharmDialogueLayoutBinding
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.landing.LandingActivity
import org.medtroniclabs.uhis.ui.patient.adapter.QualiPharmAdapter
import org.medtroniclabs.uhis.ui.patient.viewmodel.PrescriptionRefillViewModel
import kotlin.getValue

class QualifyPharmStockAuditResponseDialogue : DialogFragment(), View.OnClickListener {
    private lateinit var binding: QualipharmDialogueLayoutBinding

    private val viewModel: PrescriptionRefillViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = QualipharmDialogueLayoutBinding.inflate(inflater, container, false)
        val window: Window? = dialog?.window
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = false
        viewModel.shortageReasonMap?.let { map ->
            binding.rvMedicationReason.layoutManager = LinearLayoutManager(view.context)
            binding.rvMedicationReason.adapter = QualiPharmAdapter(map)
        }
        binding.titleCard.ivClose.safeClickListener(this)
        binding.btnDone.safeClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.ivClose, R.id.btnDone -> {
                dialog?.dismiss()
                if (requireActivity() is BaseActivity) {
                    (requireActivity() as BaseActivity).startAsNewActivity(
                        Intent(
                            requireContext(),
                            LandingActivity::class.java,
                        ),
                    )
                }
            }
        }
    }

    companion object {
        const val TAG = "QualifyPharmStockAuditResponseDialogue"

        fun newInstance(): QualifyPharmStockAuditResponseDialogue = QualifyPharmStockAuditResponseDialogue()
    }
}
