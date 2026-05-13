package org.medtroniclabs.uhis.ui.boarding

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.net.toUri
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.BuildConfig
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.model.UserDetail
import org.medtroniclabs.uhis.appextensions.getStringForLocale
import org.medtroniclabs.uhis.appextensions.hideKeyboard
import org.medtroniclabs.uhis.appextensions.openPlayStore
import org.medtroniclabs.uhis.common.AppConstants.IS_DIFFERENT_LOGIN
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.EncryptionUtil
import org.medtroniclabs.uhis.common.MultiClickListener
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.databinding.ActivityLoginBinding
import org.medtroniclabs.uhis.formgeneration.extension.markMandatory
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.boarding.viewmodel.LoginViewModel
import org.medtroniclabs.uhis.ui.landing.LandingActivity
import java.util.Locale
import java.util.UUID

@AndroidEntryPoint
class LoginActivity : BaseActivity(), View.OnClickListener {
    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()
    private var unSyncedDataCount = 0
    private val snackBarDuration = 10000
    private var isDifferentUseLogin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setMainContentView(binding.root)
        initView()
        setListeners()
        attachObservers()
        createAndSaveDeviceId()
        checkNotificationPermission()

        // 3. Set app version name
        val packageInfo = applicationContext.packageManager.getPackageInfo(packageName, 0)
        UserDetail.appVersion = packageInfo.versionName ?: ""
    }

    override fun consumeImeInsets(): Boolean = true

    private fun createAndSaveDeviceId() {
        val deviceId = SecuredPreference.getDeviceId()
        if (deviceId == null) {
            val createdId = UUID.randomUUID().toString()
            SecuredPreference.putString(SecuredPreference.EnvironmentKey.DEVICE_ID.name, createdId)
        }
    }

    private fun attachObservers() {
        viewModel.unSyncedDataCountLiveData.observe(this) {
            unSyncedDataCount = it
        }
        viewModel.loginResponseLiveData.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    resourceState?.data?.let {
                        SecuredPreference.putString(
                            SecuredPreference.EnvironmentKey.USERNAME.name,
                            it.username,
                        )
                        SecuredPreference.putString(
                            SecuredPreference.EnvironmentKey.PHONE_NUMBER.name,
                            it.phoneNumber,
                        )
                        triggerResourceLoading()
                    }
                }

                ResourceState.ERROR -> {
                    hideLoading()
                    resourceState.optionalData?.let {
                        resourceState.message?.let { message ->
                            showErrorDialogue(
                                title = getString(R.string.alert),
                                message = message,
                                positiveButtonName = getString(R.string.open_play_store),
                            ) { status ->
                                if (status) {
                                    val opened = openPlayStore()
                                    if (!opened) {
                                        showErrorDialogue(message = getString(R.string.please_check_if_play_store_available)) {}
                                    }
                                }
                            }
                        }
                    } ?: resourceState.message?.let { message ->
                        if (message.equals(getString(R.string.invalid_credentials), true)) {
                            showErrorSnackBar(
                                getString(R.string.invalid_credentials_custom),
                                Snackbar.LENGTH_INDEFINITE,
                                snackBarDuration,
                            )
                        } else if (message.equals(getStringForLocale(R.string.disabled_account, Locale.ENGLISH), true)) {
                            showErrorSnackBar(
                                getString(R.string.disabled_account),
                                Snackbar.LENGTH_INDEFINITE,
                                snackBarDuration,
                            )
                        } else if (message.equals(getStringForLocale(R.string.account_locked_multiple_attempts, Locale.ENGLISH), true)) {
                            showErrorSnackBar(
                                getString(R.string.account_locked_multiple_attempts),
                                Snackbar.LENGTH_INDEFINITE,
                                snackBarDuration,
                            )
                        } else {
                            showErrorSnackBar(message)
                        }
                    }
                }
            }
        }
    }

    private fun triggerResourceLoading() {
        val intent = Intent(this@LoginActivity, ResourceLoadingScreen::class.java)
        intent.putExtra(IS_DIFFERENT_LOGIN, isDifferentUseLogin)
        startAsNewActivity(intent)
    }

    private fun handleOfflineLoginSuccess() {
        SecuredPreference.putBoolean(
            SecuredPreference.EnvironmentKey.ISOFFLINELOGIN.name,
            true,
        )
        startAsNewActivity(
            Intent(
                this@LoginActivity,
                LandingActivity::class.java,
            ),
        )
    }

    private fun setListeners() {
        binding.btnLogin.safeClickListener(this)
        binding.tvForgotPassword.safeClickListener(this)
        if (viewModel.isNonProdEnv()) {
            binding.ivPrevious.setOnClickListener(
                MultiClickListener(
                    targetClicks = 5,
                    interval = 1000L,
                ) {
                    val uri = BuildConfig.API_BASE_URL.toUri()
                    Toast.makeText(this, uri.host?.substringBefore('.') ?: "", Toast.LENGTH_SHORT).show()
                },
            )
        }
    }

    private fun initView() {
        binding.tvUserNameLabel.markMandatory()
        binding.tvPasswordLabel.markMandatory()
        val oldUserName =
            SecuredPreference.getString(SecuredPreference.EnvironmentKey.USERNAME.name)
        if (!oldUserName.isNullOrEmpty() && CommonUtils.isCommunity()) {
            binding.userName.setText(oldUserName)
            binding.userName.isEnabled = false
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.btnLogin -> {
                hideKeyboard(view)
                validateLoginInputs()
            }

            R.id.tvForgotPassword -> {
                val intent = Intent(this, ForgetPasswordActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun validateLoginInputs() {
        val userName = binding.userName.text
            .toString()
            .trim()
        val password = binding.password.text
            .toString()
            .trim()

        // Username blank check
        if (userName.isBlank()) {
            binding.tvUserEmailError.visibility = View.VISIBLE
            binding.tvUserEmailError.text = getString(R.string.email_cannot_be_empty)
            return
        }

        // Password blank check
        if (password.isBlank()) {
            binding.tvUserEmailError.visibility = View.GONE
            binding.tvUserPasswordError.visibility = View.VISIBLE
            binding.tvUserPasswordError.text = getString(R.string.password_cannot_be_empty)
            return
        }

        // Validate the username is phone number or email
//        if (userName.contains(DefinedParams.AT_CHAR)) {
//            if (!Validator.isEmailValid(userName)) {
//                binding.tvUserEmailError.visibility = View.VISIBLE
//                binding.tvUserEmailError.text = getString(R.string.email_phone_invalid)
//                return
//            }
//        } else {
//            if (!(Validator.isValidMobileNumber(userName))) {
//                binding.tvUserEmailError.visibility = View.VISIBLE
//                binding.tvUserEmailError.text = getString(R.string.email_phone_invalid)
//                return
//            }
//        }

        // Check network connection and offline login
        binding.tvUserEmailError.visibility = View.GONE
        binding.tvUserPasswordError.visibility = View.GONE
        if (!connectivityManager.isNetworkAvailable()) {
            val isToShowAlert = (
                (
                    (
                        (
                            userName == SecuredPreference.getString(
                                SecuredPreference.EnvironmentKey.USERNAME.name,
                            )
                        ) ||
                            (
                                userName == SecuredPreference.getString(
                                    SecuredPreference.EnvironmentKey.PHONE_NUMBER.name,
                                )
                            )
                    ) &&
                        (
                            EncryptionUtil.getSecurePassword(
                                password,
                            ) == SecuredPreference.getString(
                                SecuredPreference.EnvironmentKey.PASSWORD.name,
                            )
                        )
                )
            )
            if (isToShowAlert && CommonUtils.offlineUsers()) {
                showErrorDialogue(
                    getString(R.string.alert),
                    message = getString(R.string.offline_login_message),
                    isNegativeButtonNeed = true,
                ) { buttonState ->
                    if (buttonState) {
                        handleOfflineLoginSuccess()
                    }
                }
            } else {
                showErrorSnackBar(getString(R.string.no_internet_error))
            }

            return
        }

        // Since we are not allowing to change username.
        // It is unnecessary to check whether same user getting logged in or different user
        // Hence commenting below code.

//        isDifferentUseLogin = false
//        // Check different account login
//        val oldUserName =
//            SecuredPreference.getString(SecuredPreference.EnvironmentKey.USERNAME.name)
//        val oldPhoneNumber =
//            SecuredPreference.getString(SecuredPreference.EnvironmentKey.PHONE_NUMBER.name)
//        val isNumber = userName.matches(Regex(CONTAINS_NUMBER))
//        if (oldUserName != null &&
//            validateNameOrNumber(
//                isNumber,
//                oldPhoneNumber,
//                oldUserName,
//                userName,
//            )
//        ) {
//            if (unSyncedDataCount > 0) {
//                /*showErrorDialogue(
//                    title = getString(R.string.warning_different_login_title),
//                    message = getString(R.string.warning_different_login_message, oldUserName),
//                    positiveButtonName = getString(R.string.okay),
//                    okayBtnEnable = true,
//                ) {
//
//                }*/
//                isDifferentUseLogin = true
//                val spiceUserId = SecuredPreference.getUserId()
//                SecuredPreference.putLong(SecuredPreference.EnvironmentKey.OLD_USER_ID.name, spiceUserId)
//                viewModel.doLogin(this, userName, password)
//            } else { // Different user login so clear last synced at
//                SecuredPreference.remove(SecuredPreference.EnvironmentKey.SERVER_LAST_SYNCED.name)
//                viewModel.doLogin(this, userName, password)
//            }
//        } else {
        // Same user login in online
        viewModel.doLogin(this, userName, password)
//        }
    }

    private fun validateNameOrNumber(
        isNumber: Boolean,
        oldPhoneNumber: String?,
        oldUserName: String,
        userName: String,
    ): Boolean =
        if (isNumber) {
            oldPhoneNumber != userName
        } else {
            oldUserName != userName
        }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT > 32) {
            if (!shouldShowRequestPermissionRationale("")) {
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.POST_NOTIFICATIONS,
                    ),
                )
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { _ ->
        }
}
