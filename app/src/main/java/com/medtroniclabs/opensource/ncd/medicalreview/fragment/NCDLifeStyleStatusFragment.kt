package com.medtroniclabs.opensource.ncd.medicalreview.fragment

import android.content.Context
import android.graphics.Paint
import android.os.Bundle
import android.util.Size
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.databinding.FragmentNcdLifeStyleStatusBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.ncd.data.LifeStyleResponse
import com.medtroniclabs.opensource.ncd.data.LifeStyleRequest
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil
import com.medtroniclabs.opensource.ncd.medicalreview.viewmodel.NCDMedicalReviewCMRViewModel
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseFragment
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.PatientDetailViewModel

class NCDLifeStyleStatusFragment : BaseFragment(), View.OnClickListener {

    private var popupWindow: PopupWindow? = null

    private val viewModel: NCDMedicalReviewCMRViewModel by activityViewModels()

    private val patientViewModel: PatientDetailViewModel by activityViewModels()
    private lateinit var binding: FragmentNcdLifeStyleStatusBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNcdLifeStyleStatusBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    companion object {
        const val TAG = "NCDLifeStyleStatusFragment"
        fun newInstance() =
            NCDLifeStyleStatusFragment()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        attachObserver()
        initView()
    }

    private fun initView() {
        binding.ivRefresh.safeClickListener(this)
        binding.tvDietNutrition.paintFlags = Paint.UNDERLINE_TEXT_FLAG
        binding.tvDietNutrition.safeClickListener(this)
        binding.tvSmoking.paintFlags = Paint.UNDERLINE_TEXT_FLAG
        binding.tvSmoking.safeClickListener(this)
        binding.tvAlcohol.paintFlags = Paint.UNDERLINE_TEXT_FLAG
        binding.tvAlcohol.safeClickListener(this)
        triggerApi()
    }

    private fun triggerApi() {
        loadLifeStyleData(ArrayList())
        patientViewModel.getPatientId()?.let {
            viewModel.getNcdLifeStyleDetails(
                LifeStyleRequest(
                    it
                )
            )
        } ?: kotlin.run {
            binding.ivRefresh.gone()
            binding.CenterProgress.gone()
            binding.clLifeStyle.visible()
            loadLifeStyleData(ArrayList())
        }
    }

    private fun attachObserver() {
        viewModel.lifeStyleResponse.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    binding.CenterProgress.visible()
                    binding.clLifeStyle.gone()
                    binding.ivRefresh.gone()
                }

                ResourceState.ERROR -> {
                    binding.CenterProgress.gone()
                    binding.clLifeStyle.gone()
                    binding.ivRefresh.visible()
                }

                ResourceState.SUCCESS -> {
                    binding.ivRefresh.gone()
                    binding.CenterProgress.gone()
                    binding.clLifeStyle.visible()
                    resourceState.data?.let {
                        loadLifeStyleData(it)
                    }
                }
            }
        }
    }

    private fun loadLifeStyleData(it: ArrayList<LifeStyleResponse>) {
        it.forEachIndexed { _, lifeStyle ->
            val answer: String =
                if (lifeStyle.lifestyleAnswer.isNullOrBlank()) getString(R.string.separator_hyphen) else lifeStyle.lifestyleAnswer
                    ?: ""

            binding.apply {
                when (lifeStyle.lifestyleType) {
                    NCDMRUtil.SMOKING -> {
                        val s = getString(R.string.lifestyle_smoking).replace(
                            getString(R.string.separator_hyphen),
                            answer
                        )
                        tvSmoking.text = s
                    }

                    NCDMRUtil.ALCOHOL -> {
                        val s = getString(R.string.lifestyle_alcohol).replace(
                            getString(R.string.separator_hyphen),
                            answer
                        )
                        tvAlcohol.text = s
                    }

                    NCDMRUtil.DIET_NUTRITION -> {
                        val s = getString(R.string.lifestyle_diet_nutrition).replace(
                            getString(R.string.separator_hyphen),
                            answer
                        )
                        tvDietNutrition.text = s
                    }

                    NCDMRUtil.PHYSICAL_ACTIVITY -> {
                        val s = getString(R.string.lifestyle_physical_activity).replace(
                            getString(R.string.separator_hyphen),
                            answer
                        )
                        tvPhysicalActivity.text = s
                    }
                }
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.ivRefresh -> {
                triggerApi()
            }

            binding.tvDietNutrition.id -> {
                showPopup(
                    binding.tvDietNutrition,
                    NCDMRUtil.DIET_NUTRITION
                )
            }

            binding.tvSmoking.id -> {
                showPopup(binding.tvSmoking, NCDMRUtil.SMOKING)
            }

            binding.tvAlcohol.id -> {
                showPopup(binding.tvAlcohol, NCDMRUtil.ALCOHOL)
            }
        }
    }

    private fun showPopup(popUpView: View, type: String) {
        viewModel.lifeStyleResponse.value?.data?.let { list ->
            if (list.isNotEmpty()) {
                list.firstOrNull { it.lifestyleType == type }?.let {
                    val inflater =
                        requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                    val layout =
                        inflater.inflate(R.layout.layout_patient_lifestyle_details, null)
                    popupWindow = PopupWindow(requireContext())
                    popupWindow?.apply {
                        contentView = layout
                        width = LinearLayout.LayoutParams.WRAP_CONTENT
                        height = LinearLayout.LayoutParams.WRAP_CONTENT
                        isFocusable = true
                        isOutsideTouchable = true
                        setBackgroundDrawable(
                            ContextCompat.getDrawable(
                                requireContext(),
                                R.drawable.bg_popup_window
                            )
                        )
                    }
                    val tvPatientLifeStyleTitle =
                        layout.findViewById<TextView>(R.id.tvPatientLifestyleTitle)
                    val tvPatientLifestyleDesc =
                        layout.findViewById<TextView>(R.id.tvPatientLifestyleDesc)
                    tvPatientLifeStyleTitle.text =
                        getTitleText(type) ?: getString(R.string.separator_hyphen)
                    tvPatientLifestyleDesc.text = it.comments
                    val size = Size(
                        popupWindow?.contentView?.measuredWidth!!,
                        popupWindow?.contentView?.measuredHeight!!
                    )
                    val location = IntArray(2)
                    popUpView.getLocationOnScreen(location)
                    if (!it.comments.isNullOrBlank()) {
                        popupWindow?.showAtLocation(
                            popUpView,
                            Gravity.TOP or Gravity.START,
                            location[0] - -(size.width - popUpView.width) / 2,
                            location[1] - size.height
                        )
                    }
                }
            }
        }
    }

    private fun getTitleText(type: String): String? {
        when (type) {
            NCDMRUtil.DIET_NUTRITION -> return getString(R.string.nutrition_comment_title)

            NCDMRUtil.SMOKING -> return getString(R.string.smoking_comments)

            NCDMRUtil.ALCOHOL -> return getString(R.string.alcohol_comments)
        }

        return null
    }
}