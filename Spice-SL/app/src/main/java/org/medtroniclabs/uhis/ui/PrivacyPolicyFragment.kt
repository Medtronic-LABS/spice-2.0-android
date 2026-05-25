package org.medtroniclabs.uhis.ui

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.BuildConfig
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.loadAsGif
import org.medtroniclabs.uhis.appextensions.resetImageView
import org.medtroniclabs.uhis.databinding.FragmentPrivacyPolicyBinding
import org.medtroniclabs.uhis.network.NetworkConstants.PRIVACY_POLICY
import org.medtroniclabs.uhis.ui.landing.viewmodel.LandingViewModel
import timber.log.Timber

@AndroidEntryPoint
class PrivacyPolicyFragment : BaseFragment() {
    private val viewModel: LandingViewModel by activityViewModels()

    lateinit var binding: FragmentPrivacyPolicyBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentPrivacyPolicyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        viewModel.setUserJourney(getString(R.string.privacy_policy))
    }

    private fun initView() {
        val url = "${getBaseURL()}${PRIVACY_POLICY}"
        loadURL(url)

        binding.refreshLayout.setOnRefreshListener {
            loadURL(url)
            binding.refreshLayout.isRefreshing = false
        }

        binding.webView.webViewClient = object : WebViewClient() {
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
            }

            override fun onPageFinished(
                view: WebView,
                url: String,
            ) {
                super.onPageFinished(view, url)
                hideLoading()
            }

            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?,
            ) {
                Timber.e("Error code: $errorCode Description: $description")
            }
        }

        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.setSupportZoom(true)
        binding.webView.settings.domStorageEnabled = true
    }

    private fun getBaseURL(): String = BuildConfig.ADMIN_BASE_URL

    private fun loadURL(url: String) {
        binding.webView.loadUrl(url)
        showLoading()
    }

    fun canGoBack(): Boolean = binding.webView.canGoBack()

    fun goBack() = binding.webView.goBack()

    override fun showLoading() {
        binding.loadingProgress.visibility = View.VISIBLE
        binding.loaderImage.apply {
            loadAsGif(R.drawable.ic_rotating_uhis_logo)
        }
    }

    override fun hideLoading() {
        binding.loadingProgress.visibility = View.GONE
        binding.loaderImage.apply {
            resetImageView()
        }
    }
}
