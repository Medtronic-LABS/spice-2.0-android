package com.medtroniclabs.opensource.ncd.landing.dialog

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.RadioButton
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.invisible
import com.medtroniclabs.opensource.appextensions.loadAsGif
import com.medtroniclabs.opensource.appextensions.resetImageView
import com.medtroniclabs.opensource.appextensions.setDialogPercent
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.data.CulturesEntity
import com.medtroniclabs.opensource.databinding.DialogLanguagePreferenceBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.model.CultureLocaleModel
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.landing.OnDialogDismissListener
import com.medtroniclabs.opensource.ui.landing.viewmodel.LanguagePreferenceViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LanguagePreferenceDialog(private val listener: OnDialogDismissListener) : DialogFragment(),
    View.OnClickListener {
    private lateinit var binding: DialogLanguagePreferenceBinding
    private val viewModel: LanguagePreferenceViewModel by viewModels()

    companion object {
        const val TAG = "LanguagePreferenceDialog"
        fun newInstance(listener: OnDialogDismissListener): LanguagePreferenceDialog {
            val fragment = LanguagePreferenceDialog(listener)
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DialogLanguagePreferenceBinding.inflate(inflater, container, false)
        binding.btnConfirm.safeClickListener(this)
        binding.btnCancel.safeClickListener(this)
        binding.labelHeader.ivClose.safeClickListener(this)
        val window: Window? = dialog?.window
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.getCultures()
        attachObserver()
    }

    private fun initializeRadioGroup(langList: ArrayList<CulturesEntity>, cultureSelected: Long) {
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
        viewModel.cultureUpdateResponse.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> showLoading()
                ResourceState.SUCCESS -> {
                    hideLoading()
                    listener.onDialogDismissListener(true)
                    dismiss()
                }

                ResourceState.ERROR -> hideLoading()
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
                (activity as? BaseActivity)?.withNetworkAvailability(online = {
                    updateUserLocale()
                })
            }
        }
    }

    private fun updateUserLocale() {
        viewModel.cultureList.value?.data?.firstOrNull { it.id == viewModel.selectedCultureId }?.let {
            viewModel.cultureLocaleUpdate(
                CultureLocaleModel(
                    SecuredPreference.getUserId(), it
                )
            )
        }
    }

    fun showLoading() {
        binding.apply {
            btnConfirm.invisible()
            btnCancel.invisible()
            loadingProgress.visible()
            loaderImage.apply {
                loadAsGif(R.drawable.loader_spice)
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