package org.medtroniclabs.uhis.ncd.landing.dialog

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.RadioButton
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.invisible
import org.medtroniclabs.uhis.appextensions.loadAsGif
import org.medtroniclabs.uhis.appextensions.resetImageView
import org.medtroniclabs.uhis.appextensions.setDialogPercent
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.CulturesEntity
import org.medtroniclabs.uhis.databinding.DialogLanguagePreferenceBinding
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.ui.landing.viewmodel.LanguagePreferenceViewModel

@AndroidEntryPoint
class LanguagePreferenceDialog :
    DialogFragment(),
    View.OnClickListener {
    private lateinit var binding: DialogLanguagePreferenceBinding
    private val viewModel: LanguagePreferenceViewModel by activityViewModels()

    companion object {
        const val TAG = "LanguagePreferenceDialog"

        fun newInstance() = LanguagePreferenceDialog()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DialogLanguagePreferenceBinding.inflate(inflater, container, false)
        binding.btnConfirm.safeClickListener(this)
        binding.btnCancel.safeClickListener(this)
        binding.labelHeader.ivClose.safeClickListener(this)
        val window: Window? = dialog?.window
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.getCultures()
        attachObserver()
    }

    private fun initializeRadioGroup(
        langList: ArrayList<CulturesEntity>,
        cultureSelected: Long,
    ) {
        binding.radioGroup.removeAllViews()
        for (i in langList.indices) {
            val radioButton = RadioButton(requireContext())
            radioButton.text = langList[i].name
            radioButton.tag = langList[i].id
            radioButton.textSize = 16f
            radioButton.setPadding(16, 15, 0, 15)
            binding.radioGroup.addView(radioButton)

            if (langList[i].id == cultureSelected) {
                binding.radioGroup.check(radioButton.id)
            }
        }
        binding.radioGroup.setOnCheckedChangeListener { group, checkedId ->
            viewModel.selectedCultureId = group.findViewById<RadioButton>(checkedId).tag as Long
        }
    }

    private fun attachObserver() {
        viewModel.cultureList.observe(viewLifecycleOwner) { resource ->
            resource.data?.let {
                initializeRadioGroup(ArrayList(it), SecuredPreference.getCultureId())
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        handleConfiguration()
    }

    override fun onStart() {
        super.onStart()
        handleConfiguration()
    }

    private fun handleConfiguration() {
        val isTablet = CommonUtils.checkIsTablet(requireContext())
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        when {
            isTablet && isLandscape -> setDialogPercent(50, 90)
            isTablet -> setDialogPercent(80, 50)
            else -> setDialogPercent(95, 65)
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.labelHeader.ivClose.id, R.id.btnCancel -> {
                dismiss()
            }

            R.id.btnConfirm -> {
                val selectedRadioButton =
                    binding.radioGroup.findViewById<RadioButton>(binding.radioGroup.checkedRadioButtonId)

                val cultureId = viewModel.selectedCultureId ?: selectedRadioButton?.tag as? Long
                val culture =
                    viewModel.cultureList.value
                        ?.data
                        ?.firstOrNull { it.id == cultureId }

                culture?.let {
                    viewModel.setSelectedCultureForConfirmation(it)
                    dismiss()
                }
            }
        }
    }

    fun showLoading() {
        binding.apply {
            btnConfirm.invisible()
            btnCancel.invisible()
            loadingProgress.visible()
            loaderImage.apply {
                loadAsGif(R.drawable.ic_rotating_uhis_logo)
            }
        }
    }

    fun hideLoading() {
        binding.apply {
            btnConfirm.visible()
            btnCancel.visible()
            loadingProgress.gone()
            loaderImage.apply {
                resetImageView()
            }
        }
    }
}
