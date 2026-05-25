package org.medtroniclabs.uhis.formgeneration.utility

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
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.common.DefinedParams.ID
import org.medtroniclabs.uhis.databinding.FragmentInformationLayoutBinding
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.model.InformationModel
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.Contraceptive
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.MUAC
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.PostTestCounselling
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.PreTestCounselling
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.TestForHiv
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.chestInDrawing
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.hasOedemaOfBothFeet
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.isBreastfeed
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.isConvulsionPastFewDays
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.isUnusualSleepy
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.isVomiting
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.muacCode
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.rdtTest
import org.medtroniclabs.uhis.ui.assessment.viewmodel.AssessmentViewModel

class InformationLayoutFragment : DialogFragment(), View.OnClickListener {
    lateinit var binding: FragmentInformationLayoutBinding

    private val viewModel: AssessmentViewModel by activityViewModels()

    companion object {
        const val TAG = "InformationLayoutFragment"
        private const val KEY_CUSTOM_INFORMATION = "customInformation"
        private const val KEY_CUSTOM_INFORMATION_LIST = "customInformationList"

        fun newInstance(
            id: String,
            title: String,
            customInformation: String? = null,
            customInformationList: ArrayList<String>? = null,
        ): InformationLayoutFragment {
            val fragment = InformationLayoutFragment()
            fragment.arguments = Bundle().apply {
                putString(DefinedParams.ID, id)
                putString(DefinedParams.TITLE, title)
                customInformation?.let { putString(KEY_CUSTOM_INFORMATION, it) }
                customInformationList?.let { putStringArrayList(KEY_CUSTOM_INFORMATION_LIST, it) }
            }
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentInformationLayoutBinding.inflate(inflater, container, false)
        val window: Window? = dialog?.window
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = false
        initializeViews()
        setListeners()
    }

    private fun initializeViews() {
        binding.tvTitle.text = arguments?.getString(DefinedParams.TITLE) ?: getString(R.string.instructions)
        arguments?.getString(ID)?.let { informationType ->
            viewModel.setUserJourney("$informationType  ${AnalyticsDefinedParams.INSTRUCTIONDIALOGUE}")
        }

        // Check for custom information first
        val customInformation = arguments?.getString(KEY_CUSTOM_INFORMATION)
        val customInformationList = arguments?.getStringArrayList(KEY_CUSTOM_INFORMATION_LIST)

        val informationListByType: ArrayList<InformationModel>? = when {
            // Use custom information if provided
            !customInformation.isNullOrBlank() -> {
                val model = InformationModel(
                    inputText = customInformation,
                )
                arrayListOf(model)
            }
            customInformationList != null && customInformationList.isNotEmpty() -> {
                customInformationList
                    .map { info ->
                        InformationModel(
                            inputText = info,
                        )
                    }.let { ArrayList(it) }
            }
            // Otherwise use predefined information based on ID
            else -> when (arguments?.getString(ID)) {
                muacCode, MUAC -> InformationUtils().getMuacInformationListItem(requireContext())
                hasOedemaOfBothFeet -> InformationUtils().getOedemaInformationList(requireContext())
                chestInDrawing -> InformationUtils().getChestIndrawingInformation(requireContext())
                isUnusualSleepy, isVomiting, isConvulsionPastFewDays, isBreastfeed -> {
                    InformationUtils().getDangerSignsInstructions(requireContext(), arguments?.getString(ID))
                }
                rdtTest -> {
                    InformationUtils().getRdtTest(requireContext())
                }
                Contraceptive -> {
                    InformationUtils().getContraceptiveInformation(requireContext())
                }
                PreTestCounselling, PostTestCounselling, TestForHiv -> {
                    InformationUtils().getHIVTestInformation(requireContext(), arguments?.getString(ID))
                }
                else -> null
            }
        }
        binding.rvInfoList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = informationListByType?.let { InformationListAdapter(it) }
        }
    }

    private fun setListeners() {
        binding.ivClose.safeClickListener(this)
        binding.btnClose.safeClickListener(this)
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.ivClose.id, binding.btnClose.id -> {
                dismiss()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
        )
    }
}
