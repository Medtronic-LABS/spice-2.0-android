package com.medtroniclabs.opensource.ui.boarding

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.medtroniclabs.opensource.BuildConfig
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.app.analytics.model.UserDetail
import com.medtroniclabs.opensource.appextensions.hideKeyboard
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.EncryptionUtil
import com.medtroniclabs.opensource.common.RegexConstants.Contains_Number
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.common.Validator
import com.medtroniclabs.opensource.databinding.ActivityLoginBinding
import com.medtroniclabs.opensource.formgeneration.extension.markMandatory
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.boarding.viewmodel.LoginViewModel
import com.medtroniclabs.opensource.ui.landing.LandingActivity
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID

@AndroidEntryPoint
class LoginActivity : BaseActivity(), View.OnClickListener {
    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()
    private var unSyncedDataCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setMainContentView(binding.root)
        initView()
        setListeners()
        attachObservers()
        createAndSaveDeviceId()
        checkNotificationPermission()

        //3. Set app version name
        val packageInfo = applicationContext.packageManager.getPackageInfo(packageName, 0)
        UserDetail.appVersion = packageInfo.versionName
    }

    private fun createAndSaveDeviceId() {
        val deviceId = SecuredPreference.getDeviceId()
        if (deviceId == null) {
            val createdId =  UUID.randomUUID().toString()
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
                            SecuredPreference.EnvironmentKey.USERNAME.name, it.username
                        )
                        SecuredPreference.putString(
                            SecuredPreference.EnvironmentKey.PHONE_NUMBER.name, it.phoneNumber
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
                                    try {
                                        startActivity(Intent(Intent.ACTION_VIEW).apply {
                                            data = Uri.parse(
                                                "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID
                                            )
                                            setPackage("com.android.vending")
                                        })
                                    } catch (e: Exception) {
                                        showErrorDialogue(message = getString(R.string.please_check_if_play_store_available)) {}
                                    }
                                }
                            }
                        }
                    } ?: resourceState.message?.let {
                        showErrorSnackBar(it)
                    }
                }
            }
        }

    }

    private fun triggerResourceLoading() {
        startAsNewActivity(
            Intent(
                this@LoginActivity, ResourceLoadingScreen::class.java
            )
        )
    }

    private fun handleOfflineLoginSuccess() {
        SecuredPreference.putBoolean(
            SecuredPreference.EnvironmentKey.ISOFFLINELOGIN.name, true
        )
        startAsNewActivity(
            Intent(
                this@LoginActivity, LandingActivity::class.java
            )
        )
    }

    private fun setListeners() {
        binding.btnLogin.safeClickListener(this)
        binding.tvForgotPassword.safeClickListener(this)

    }

    private fun initView() {
        binding.tvUserNameLabel.markMandatory()
        binding.tvPasswordLabel.markMandatory()
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
        val userName = binding.userName.text.toString().trim()
        val password = binding.password.text.toString().trim()

        //Username blank check
        if (userName.isBlank()) {
            binding.tvUserEmailError.visibility = View.VISIBLE
            binding.tvUserEmailError.text = getString(R.string.email_cannot_be_empty)
            return
        }

        //Password blank check
        if (password.isBlank()) {
            binding.tvUserEmailError.visibility = View.GONE
            binding.tvUserPasswordError.visibility = View.VISIBLE
            binding.tvUserPasswordError.text = getString(R.string.password_cannot_be_empty)
            return
        }

        //Validate the username is phone number or email
        if (userName.contains(DefinedParams.AT_CHAR)) {
            if (!Validator.isEmailValid(userName)) {
                binding.tvUserEmailError.visibility = View.VISIBLE
                binding.tvUserEmailError.text = getString(R.string.email_phone_invalid)
                return
            }
        } else {
            if (!(Validator.isValidMobileNumber(userName))) {
                binding.tvUserEmailError.visibility = View.VISIBLE
                binding.tvUserEmailError.text = getString(R.string.email_phone_invalid)
                return
            }
        }

        // Check network connection and offline login
        binding.tvUserEmailError.visibility = View.GONE
        binding.tvUserPasswordError.visibility = View.GONE
        if (!connectivityManager.isNetworkAvailable()) {
            val isToShowAlert = ((((userName == SecuredPreference.getString(
                SecuredPreference.EnvironmentKey.USERNAME.name
            )) || (userName == SecuredPreference.getString(
                SecuredPreference.EnvironmentKey.PHONE_NUMBER.name
            ))) && (EncryptionUtil.getSecurePassword(
                password
            ) == SecuredPreference.getString(
                SecuredPreference.EnvironmentKey.PASSWORD.name
            ))))
            if (isToShowAlert && CommonUtils.offlineUsers()) {
                showErrorDialogue(
                    getString(R.string.alert),
                    message = getString(R.string.offline_login_message),
                    isNegativeButtonNeed = true
                ) { buttonState ->
                    if (buttonState) {
                        handleOfflineLoginSuccess()
                    }
                }
            } else
                showErrorSnackBar(getString(R.string.no_internet_error))

            return
        }

        // Check different account login
        val oldUserName =
            SecuredPreference.getString(SecuredPreference.EnvironmentKey.USERNAME.name)
        val oldPhoneNumber =
            SecuredPreference.getString(SecuredPreference.EnvironmentKey.PHONE_NUMBER.name)
        val isNumber = userName.matches(Regex(Contains_Number))
        if (oldUserName != null && validateNameOrNumber(
                isNumber, oldPhoneNumber, oldUserName, userName
            )
        ) {
            if (unSyncedDataCount > 0) {
                showErrorDialogue(
                    title = getString(R.string.warning_different_login_title),
                    message = getString(R.string.warning_different_login_message, oldUserName),
                    positiveButtonName = getString(R.string.okay),
                    okayBtnEnable = true,
                ) {

                }
            } else { // Different user login so clear last synced at
                SecuredPreference.remove(SecuredPreference.EnvironmentKey.SERVER_LAST_SYNCED.name)
                viewModel.doLogin(this, userName, password)
            }
        } else {
            // Same user login in online
            viewModel.doLogin(this, userName, password)
        }
    }

    private fun validateNameOrNumber(
        isNumber: Boolean, oldPhoneNumber: String?, oldUserName: String, userName: String
    ): Boolean {
        return if (isNumber) {
            oldPhoneNumber != userName
        } else {
            oldUserName != userName
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT > 32) {
            if (!shouldShowRequestPermissionRationale("")){
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                )
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { _ ->

        }
}