package org.medtroniclabs.uhis.ui.dialog

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams.PHUWALKINSCREENMEMBERLINKSUCCESS
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.invisible
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.databinding.FragmentSuccessDialogBinding
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.ui.household.HouseholdDefinedParams.IS_HOUSEHOLD
import org.medtroniclabs.uhis.ui.household.HouseholdDefinedParams.IS_HOUSEHOLD_MEMBER
import org.medtroniclabs.uhis.ui.household.HouseholdDefinedParams.IS_PHU_WALK_INS_FLOW
import org.medtroniclabs.uhis.ui.household.viewmodel.HouseRegistrationViewModel
import org.medtroniclabs.uhis.ui.landing.OnDialogDismissListener
import org.medtroniclabs.uhis.ui.phuwalkins.activity.PhuWalkInsActivity

class SuccessDialogFragment : DialogFragment(), View.OnClickListener {
    private lateinit var binding: FragmentSuccessDialogBinding
    private var onDismissListener: OnDialogDismissListener? = null
    private val viewModel: HouseRegistrationViewModel by activityViewModels()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        onDismissListener = context as OnDialogDismissListener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentSuccessDialogBinding.inflate(layoutInflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        isCancelable = false
        return binding.root
    }

    companion object {
        const val TAG = "SuccessDialogFragment"

        fun newInstance(
            isHousehold: Boolean = false,
            isMember: Boolean = false,
            isPhuLink: Boolean = false,
            descText: String? = null,
        ): SuccessDialogFragment {
            val bundle = Bundle()
            bundle.putBoolean(IS_HOUSEHOLD, isHousehold)
            bundle.putBoolean(IS_HOUSEHOLD_MEMBER, isMember)
            bundle.putBoolean(IS_PHU_WALK_INS_FLOW, isPhuLink)
            if (!descText.isNullOrBlank()) {
                bundle.putString(DefinedParams.LABEL, descText)
            }
            val fragment = SuccessDialogFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        setListener()
        attachObserver()
    }

    private fun setListener() {
        binding.btnDone.safeClickListener(this)
    }

    private fun attachObserver() {
        binding.householdNo.invisible()
        if (!arguments?.getString(DefinedParams.LABEL).isNullOrBlank()) {
            binding.successMessage.text = arguments?.getString(DefinedParams.LABEL)
        }
        if (arguments?.getBoolean(IS_HOUSEHOLD) == true) {
            viewModel.setUserJourney(AnalyticsDefinedParams.HouseHoldRegistrationSuccess)
            binding.successMessage.text = getString(R.string.household_successfully)
        }

        if (arguments?.getBoolean(IS_HOUSEHOLD_MEMBER) == true) {
            viewModel.setUserJourney(AnalyticsDefinedParams.MemberRegistrationSuccess)
            binding.successMessage.text = getString(R.string.member_registered_successfully)
        }

        if (arguments?.getBoolean(IS_PHU_WALK_INS_FLOW) == true) {
            binding.successMessage.setPadding(50, 0, 50, 0)
            binding.successMessage.text = getString(R.string.member_registered_successfully_linked)
            binding.householdNo.gone()
            viewModel.setUserJourney(PHUWALKINSCREENMEMBERLINKSUCCESS)
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.btnDone.id -> {
                viewModel.setUserJourney(AnalyticsDefinedParams.OKAYBUTTONTRIGGERED)
                if (arguments?.getBoolean(IS_PHU_WALK_INS_FLOW) == true) {
                    val intent = Intent(requireContext(), PhuWalkInsActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    requireActivity().finish()
                } else {
                    onDismissListener?.onDialogDismissListener(
                        arguments?.getBoolean(IS_HOUSEHOLD) == true,
                    )
                    dismiss()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        onDismissListener = null
    }
}
