package org.medtroniclabs.uhis.ui.patient

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Bundle
import android.view.View
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.widget.addTextChangedListener
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.hideKeyboard
import org.medtroniclabs.uhis.common.ViewUtils.statusCheck
import org.medtroniclabs.uhis.databinding.ActivityTermsAndConditionBinding
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams
import org.medtroniclabs.uhis.formgeneration.extension.markMandatory
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.landing.LandingActivity
import org.medtroniclabs.uhis.ui.patient.viewmodel.TermsAndConditionViewModel
import kotlin.getValue
import kotlin.text.trim

@AndroidEntryPoint
class TermsAndConditionActivity : BaseActivity(), View.OnClickListener {
    lateinit var binding: ActivityTermsAndConditionBinding

    private val viewModel: TermsAndConditionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        binding = ActivityTermsAndConditionBinding.inflate(layoutInflater)
        setMainContentView(binding.root, true, getString(R.string.terms_condition), homeAndBackVisibility = Pair(true, null), callbackHome = {
            if (viewModel.isFromScreening) {
                startAsNewActivity(Intent(this, LandingActivity::class.java))
            } else {
                onHomeIconClicked()
            }
        }, callback = {
            backHandelFlow()
        })
        initializeView()
        attachObserver()
    }

    private fun attachObserver() {
        binding.etUserInitial.addTextChangedListener { patientInitial ->
            if (patientInitial.isNullOrBlank()) {
                viewModel.patientInitial.value = null
            } else {
                viewModel.patientInitial.value = patientInitial.trim().toString()
            }
        }
        viewModel.consentDoneLiveDate.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }
                ResourceState.SUCCESS -> {
                    hideLoading()
                    resourceState.data?.let { url ->
                        loadRespectiveWebpage(url)
                    }
                }
                ResourceState.ERROR -> {
                    hideLoading()
                }
            }
        }

        viewModel.patientInitial.observe(this) { initial ->
            if (viewModel.enrollmentConsent) {
                binding.btnAccept.isEnabled = !initial.isNullOrBlank()
            }
        }
    }

    private fun loadRespectiveWebpage(url: String) {
        binding.termsConditionWebView.loadDataWithBaseURL(null, url, "text/html", "utf-8", null)
    }

    private fun initializeView() {
        viewModel.isFromScreening =
            intent.getBooleanExtra(IntentConstants.INTENT_SCREENING, false)
        viewModel.isEyeFromScreening =
            intent.getBooleanExtra(IntentConstants.INTENT_EYE_SCREENING, false)
        viewModel.isCataractScreening =
            intent.getBooleanExtra(IntentConstants.INTENT_CATARACT, false)
        viewModel.enrollmentConsent =
            intent.getBooleanExtra(IntentConstants.INTENT_ENROLLMENT, false)
        viewModel.isFromSummaryPage =
            intent.getBooleanExtra(IntentConstants.IS_FROM_SUMMARY_PAGE, false)
        viewModel.isFromDirectEnrollment =
            intent.getBooleanExtra(IntentConstants.IS_FROM_DIRECT_ENROLLMENT, false)
        binding.tvTitle.markMandatory()

        if (viewModel.enrollmentConsent) {
            binding.tvTitle.visibility = View.VISIBLE
            binding.btnAccept.isEnabled = false
            binding.etUserInitial.visibility = View.VISIBLE
            binding.tvTermsAndConditionInfo.text =
                getString(R.string.terms_condition_info_enrollment)
        } else {
            binding.tvTitle.visibility = View.GONE
            binding.btnAccept.isEnabled = true
            binding.etUserInitial.visibility = View.GONE
            binding.tvTermsAndConditionInfo.text =
                getString(R.string.terms_condition_info_screening)
        }
        binding.termsConditionWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?,
            ): Boolean {
                request?.url?.let { emailUri ->
                    if (emailUri.toString().startsWith("mailto")) {
                        try {
                            startActivity(Intent(Intent.ACTION_VIEW, emailUri))
                            return true
                        } catch (e: Exception) {
                            return false
                        }
                    } else {
                        showLoading()
                        view?.loadUrl(emailUri.toString())
                    }
                }
                return false
            }

            override fun onPageStarted(
                view: WebView?,
                url: String?,
                favicon: Bitmap?,
            ) {
                super.onPageStarted(view, url, favicon)
                showLoading()
            }

            override fun onPageFinished(
                view: WebView?,
                url: String?,
            ) {
                super.onPageFinished(view, url)
                hideLoading()
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?,
            ) {
                super.onReceivedError(view, request, error)
                hideLoading()
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?,
            ) {
                hideLoading()
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?,
            ) {
                super.onReceivedSslError(view, handler, error)
                hideLoading()
            }
        }
        binding.termsConditionWebView.settings.javaScriptEnabled = true
        viewModel.fetchConsentRawHTML()
        binding.btnAccept.safeClickListener(this)
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.btnAccept -> {
                binding.btnAccept.isEnabled = false
                hideKeyboard(view)
                if (viewModel.enrollmentConsent) {
                    startActivity(
                        Intent(
                            this@TermsAndConditionActivity,
                            EnrollmentFormBuilderActivity::class.java,
                        ).apply {
                            putExtra(
                                DefinedParams.PATIENT_ID,
                                intent.getLongExtra(DefinedParams.PATIENT_ID, -1L),
                            )
                            putExtra(
                                IntentConstants.INTENT_PATIENT_INITIAL,
                                binding.etUserInitial.text?.toString(),
                            )
                            putExtra(
                                IntentConstants.IS_FROM_DIRECT_ENROLLMENT,
                                viewModel.isFromDirectEnrollment,
                            )
                            putExtra(
                                DefinedParams.SCREENING_ID,
                                intent.getLongExtra(DefinedParams.SCREENING_ID, -1L),
                            )
                            putExtra(
                                DefinedParams.PATIENT_DIAGNOSIS,
                                intent.getBooleanExtra(DefinedParams.PATIENT_DIAGNOSIS, false),
                            )
                            putExtra(
                                DefinedParams.IS_CONFIRM_DIAGNOSIS,
                                intent.getBooleanExtra(DefinedParams.IS_CONFIRM_DIAGNOSIS, false),
                            )
                        },
                    )
                }
                binding.btnAccept.isEnabled = true
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.isFromScreening) {
            statusCheck(this)
        }
    }

    private val onBackPressedCallback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            backHandelFlow()
        }
    }

    private fun backHandelFlow() {
        if (viewModel.isFromSummaryPage) {
            startAsNewActivity(Intent(this@TermsAndConditionActivity, LandingActivity::class.java))
        } else {
            finish()
        }
    }

    fun onHomeIconClicked() {
        showErrorDialogue(
            getString(R.string.alert),
            getString(R.string.exit_reason),
            isNegativeButtonNeed = true,
        ) {
            if (it) {
                startAsNewActivity(Intent(this, LandingActivity::class.java))
            }
        }
    }

    private fun isWebViewAvailable(context: Context): Boolean =
        try {
            WebView(context)
            true
        } catch (e: Exception) {
            false
        }
}
