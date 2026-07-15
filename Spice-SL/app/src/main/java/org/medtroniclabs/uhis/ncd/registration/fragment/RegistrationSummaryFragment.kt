package org.medtroniclabs.uhis.ncd.registration.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.activityViewModels
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.data.model.RegistrationResponse
import org.medtroniclabs.uhis.databinding.CardLayoutBinding
import org.medtroniclabs.uhis.databinding.FragmentRegistrationSummaryBinding
import org.medtroniclabs.uhis.databinding.SummaryLayoutBinding
import org.medtroniclabs.uhis.formgeneration.extension.capitalizeFirstChar
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.ncd.registration.viewmodel.RegistrationFormViewModel
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.BaseFragment

class RegistrationSummaryFragment : BaseFragment(), View.OnClickListener {
    private lateinit var binding: FragmentRegistrationSummaryBinding
    private val viewModel: RegistrationFormViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentRegistrationSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
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
            if (treatmentPlanMap?.containsKey(DefinedParams.TREATMENT_PLAN) == true) {
                treatmentPlanMap[DefinedParams.TREATMENT_PLAN]?.let { list ->
                    if (list is ArrayList<*>) {
                        addTPCardView(list)
                    }
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
                    val lbl = it[DefinedParams.LABEL]
                    val value = it[DefinedParams.Value]
                    if (lbl is String && value is String) {
                        layout.addView(
                            inflateChildView(
                                lbl,
                                value,
                            ),
                        )
                    }
                }
            }
        }

        if (cardBinding.llFamilyRoot.childCount > 0) {
            binding.llForm.addView(cardBinding.root)
        }
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
        responseModel: RegistrationResponse,
    ) {
        llFamilyRoot.apply {
            responseModel.let { response ->
                response.dateOfEnrollment?.let {
                    addView(
                        inflateChildView(
                            getString(R.string.date_of_registration),
                            DateUtils.convertDateFormat(
                                it,
                                DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                                DateUtils.DATE_ddMMyyyy,
                            ),
                        ),
                    )
                }
                response.name?.let {
                    addView(
                        inflateChildView(
                            getString(R.string.name),
                            it,
                        ),
                    )
                }
                response.gender?.let {
                    addView(
                        inflateChildView(
                            getString(R.string.gender),
                            it.capitalizeFirstChar(),
                        ),
                    )
                }
                response.age?.let {
                    addView(
                        inflateChildView(
                            getString(R.string.age),
                            it,
                        ),
                    )
                }
                response.programId?.let {
                    addView(
                        inflateChildView(
                            getString(R.string.patient_id),
                            it,
                        ),
                    )
                }
                response.nationalId?.let {
                    addView(
                        inflateChildView(
                            getString(R.string.national_id),
                            it,
                        ),
                    )
                }
                response.phoneNumber?.let {
                    addView(
                        inflateChildView(
                            getString(R.string.mobile_number),
                            it,
                        ),
                    )
                }
                response.facilityName?.let {
                    addView(
                        inflateChildView(
                            getString(R.string.facility_name),
                            it,
                        ),
                    )
                }
            }
        }
    }

    private fun inflateChildView(
        labelKey: String,
        value: String,
        applyBoldStyle: Boolean? = null,
        textColor: Int? = null,
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
