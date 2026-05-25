package org.medtroniclabs.uhis.ui.household

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.DefinedParams.FhirMemberID
import org.medtroniclabs.uhis.common.DefinedParams.HIV
import org.medtroniclabs.uhis.common.DefinedParams.ID
import org.medtroniclabs.uhis.common.DefinedParams.IS_HOUSE_HOLD
import org.medtroniclabs.uhis.common.DefinedParams.IS_MEMBER_REGISTRATION
import org.medtroniclabs.uhis.common.DefinedParams.MEMBER_ID
import org.medtroniclabs.uhis.common.DefinedParams.PatientId
import org.medtroniclabs.uhis.common.DefinedParams.VILLAGE_ID
import org.medtroniclabs.uhis.common.DefinedParams.isCreateHouseholdForPhu
import org.medtroniclabs.uhis.common.DefinedParams.villageId
import org.medtroniclabs.uhis.databinding.ActivityConsentFormBinding
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.household.HouseholdDefinedParams.IS_PHU_WALK_INS_FLOW
import org.medtroniclabs.uhis.ui.household.fragment.ConsentSignatureDialogFragment
import org.medtroniclabs.uhis.ui.household.viewmodel.ConsentFormViewModel
import org.medtroniclabs.uhis.ui.medicalreview.hiv.activity.HivMedicalReviewBaseActivity

@AndroidEntryPoint
class ConsentFormActivity : BaseActivity(), View.OnClickListener {
    private lateinit var binding: ActivityConsentFormBinding
    private val viewModel: ConsentFormViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.isHivFlow = intent.getBooleanExtra(HIV, false)
        viewModel.isHouseHoldFlow = intent.getBooleanExtra(IS_HOUSE_HOLD, false)

        binding = ActivityConsentFormBinding.inflate(layoutInflater)
        setMainContentView(
            binding.root,
            isToolbarVisible = true,
            title = if (viewModel.isHivFlow) {
                getString(R.string.consent_form)
            } else {
                getString(R.string.terms_and_condition)
            },
            homeAndBackVisibility = Pair(true, true),
        )

        showLoading()
        initView()
    }

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            finish()
        }
    }

    private fun initView() {
        if (viewModel.isHivFlow) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            binding.btnSignature.gone()
            binding.btnAccept.visible()
        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            if (viewModel.isHouseHoldFlow) {
                binding.btnAccept.visible()
                binding.btnSignature.gone()
            } else {
                binding.btnAccept.gone()
                binding.btnSignature.visible()
            }
        }
        viewModel.setUserJourney(getString(R.string.terms_and_condition))
        binding.btnSignature.setOnClickListener {
            val dialog = ConsentSignatureDialogFragment().apply {
                arguments = Bundle().apply {
                    putLong(VILLAGE_ID, intent.getLongExtra(VILLAGE_ID, -1L))
                    putBoolean(IS_PHU_WALK_INS_FLOW, intent.getBooleanExtra(IS_PHU_WALK_INS_FLOW, false))
                    putBoolean(isCreateHouseholdForPhu, intent.getBooleanExtra(isCreateHouseholdForPhu, false))
                    putBoolean(IS_HOUSE_HOLD, intent.getBooleanExtra(IS_HOUSE_HOLD, false))
                    putLong(MEMBER_ID, intent.getLongExtra(MEMBER_ID, -1L))
                    putLong(FhirMemberID, intent.getLongExtra(FhirMemberID, -1L))
                    putBoolean(
                        IS_MEMBER_REGISTRATION,
                        intent.getBooleanExtra(IS_MEMBER_REGISTRATION, false),
                    )
                }
            }
            dialog.show(supportFragmentManager, ConsentSignatureDialogFragment.TAG)
        }

        binding.wvTermAndCondition.webViewClient = webViewClientCallBack
        binding.wvTermAndCondition.settings.javaScriptEnabled = true
        binding.wvTermAndCondition.settings.apply {
            builtInZoomControls = false
            setSupportZoom(false)
        }

        viewModel.termsAndConditionStringLiveData.observe(this) {
            binding.wvTermAndCondition.loadDataWithBaseURL(
                null,
                it,
                "text/html",
                "UTF-8",
                null,
            )

            binding.btnAccept.safeClickListener(this)
        }
    }

    private val webViewClientCallBack = object : WebViewClient() {
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

    override fun onClick(view: View?) {
        when (view) {
            binding.btnAccept -> {
                if (viewModel.isHouseHoldFlow) {
                    val intent = Intent(this, HouseholdActivity::class.java)
                    intent.putExtra(VILLAGE_ID, intent.getLongExtra(VILLAGE_ID, -1L))
                    intent.putExtra(FhirMemberID, intent.getLongExtra(FhirMemberID, -1L))
                    intent.putExtra(IS_PHU_WALK_INS_FLOW, intent.getBooleanExtra(IS_PHU_WALK_INS_FLOW, false))
                    intent.putExtra(isCreateHouseholdForPhu, intent.getBooleanExtra(isCreateHouseholdForPhu, false))
                    intent.putExtra(MEMBER_ID, intent.getLongExtra(MEMBER_ID, -1L))
                    finish()
                    startActivity(intent)
                } else {
                    val intent = Intent(this, HivMedicalReviewBaseActivity::class.java).apply {
                        putExtra(PatientId, intent.getStringExtra(PatientId))
                        putExtra(HIV, viewModel.isHivFlow)
                        putExtra(ID, intent.getStringExtra(ID))
                        putExtra(MEMBER_ID, intent.getStringExtra(MEMBER_ID))
                        putExtra(villageId, intent.getStringExtra(villageId))
                    }
                    launcher.launch(intent)
                }
            }
        }
    }
}
