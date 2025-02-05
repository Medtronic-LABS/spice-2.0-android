package com.medtroniclabs.opensource.ncd.registration.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.activityViewModels
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.data.model.RegistrationResponse
import com.medtroniclabs.opensource.databinding.CardLayoutBinding
import com.medtroniclabs.opensource.databinding.FragmentRegistrationSummaryBinding
import com.medtroniclabs.opensource.databinding.SummaryLayoutBinding
import com.medtroniclabs.opensource.formgeneration.extension.capitalizeFirstChar
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.BaseFragment
import com.medtroniclabs.opensource.ncd.registration.viewmodel.RegistrationFormViewModel

class RegistrationSummaryFragment : BaseFragment(), View.OnClickListener {
    private lateinit var binding: FragmentRegistrationSummaryBinding
    private val viewModel: RegistrationFormViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRegistrationSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        clickListener()
        addChildViews()
    }

    private fun addChildViews() {
        binding.llForm.removeAllViews()
        addCardView(getString(R.string.bio_data))
        inflateTreatmentPlanResponse()
    }

    private fun inflateTreatmentPlanResponse() {
        viewModel.registrationResponseLiveData.value?.data?.treatmentPlanResponse.let { treatmentPlanMap ->
            if (treatmentPlanMap?.containsKey(DefinedParams.TreatmentPlan) == true) {
                treatmentPlanMap[DefinedParams.TreatmentPlan]?.let { list ->
                    if (list is ArrayList<*>)
                        addTPCardView(list)
                }
            }
        }
    }

    private fun addTPCardView(treatmentPlanMap: ArrayList<*>) {
        val cardBinding = CardLayoutBinding.inflate(layoutInflater)
        cardBinding.apply {
            cardTitle.text = getString(R.string.treatment_plan)
            viewCardBG.setBackgroundColor(requireContext().getColor(R.color.cobalt_blue))
            cardTitle.setTextColor(requireContext().getColor(R.color.white))
        }

        cardBinding.llFamilyRoot.let { layout ->
            treatmentPlanMap.forEach {
                if (it is Map<*, *>) {
                    val lbl = it[DefinedParams.label]
                    val value = it[DefinedParams.Value]
                    if (lbl is String && value is String) {
                        layout.addView(
                            inflateChildView(
                                lbl,
                                value
                            )
                        )
                    }
                }
            }
        }

        if (cardBinding.llFamilyRoot.childCount > 0)
            binding.llForm.addView(cardBinding.root)
    }

    private fun addCardView(cardTitle: String) {
        val cardBinding = CardLayoutBinding.inflate(layoutInflater)
        cardBinding.cardTitle.text = cardTitle
        viewModel.registrationResponseLiveData.value?.data?.let {
            addBioDataCardDetails(cardBinding.llFamilyRoot, it)
        }
        if (cardBinding.llFamilyRoot.childCount > 0) binding.llForm.addView(cardBinding.root)
    }

    private fun addBioDataCardDetails(
        llFamilyRoot: LinearLayout,
        responseModel: RegistrationResponse
    ) {
        llFamilyRoot.apply {
            responseModel.let { response ->
                response.dateOfEnrollment?.let {
                    addView(
                        inflateChildView(
                            getString(R.string.date_of_registration), DateUtils.convertDateFormat(
                                it,
                                DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                                DateUtils.DATE_ddMMyyyy
                            )
                        )
                    )
                }
                response.name?.let {
                    addView(
                        inflateChildView(
                            getString(R.string.name), it
                        )
                    )
                }
                response.gender?.let {
                    addView(
                        inflateChildView(
                            getString(R.string.gender), it.capitalizeFirstChar()
                        )
                    )
                }
                response.age?.let {
                    addView(
                        inflateChildView(
                            getString(R.string.age), it
                        )
                    )
                }
                response.programId?.let {
                    addView(
                        inflateChildView(
                            getString(R.string.patient_id), it
                        )
                    )
                }
                response.nationalId?.let {
                    addView(
                        inflateChildView(
                            getString(R.string.national_id), it
                        )
                    )
                }
                response.phoneNumber?.let {
                    addView(
                        inflateChildView(
                            getString(R.string.mobile_number), it
                        )
                    )
                }
                response.facilityName?.let {
                    addView(
                        inflateChildView(
                            getString(R.string.facility_name), it
                        )
                    )
                }
            }
        }
    }

    private fun inflateChildView(
        labelKey: String, value: String, applyBoldStyle: Boolean? = null, textColor: Int? = null
    ): View {
        val summaryBinding = SummaryLayoutBinding.inflate(layoutInflater)
        summaryBinding.tvKey.text = labelKey
        summaryBinding.tvValue.text = value
        summaryBinding.tvRowSeparator.text = getString(R.string.separator_colon)
        applyBoldStyle?.let {
            summaryBinding.tvValue.typeface =
                ResourcesCompat.getFont(requireContext(), R.font.inter_bold)
        }
        textColor?.let {
            summaryBinding.tvValue.setTextColor(it)
        }
        return summaryBinding.root
    }

    private fun clickListener() {
        binding.btnDone.safeClickListener(this)
    }

    companion object {
        const val TAG = "RegistrationSummaryFragment"
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.btnDone -> {
                (activity as? BaseActivity?)?.redirectToHome()
            }
        }
    }
}