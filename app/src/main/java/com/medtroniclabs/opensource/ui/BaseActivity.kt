package com.medtroniclabs.opensource.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.google.android.material.snackbar.Snackbar
import com.medtroniclabs.opensource.BuildConfig
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.app.analytics.model.UserDetail
import com.medtroniclabs.opensource.appextensions.loadAsGif
import com.medtroniclabs.opensource.appextensions.resetImageView
import com.medtroniclabs.opensource.appextensions.setVisible
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.DefinedParams.REFRESH_FRAGMENT
import com.medtroniclabs.opensource.common.LocaleHelper
import com.medtroniclabs.opensource.databinding.ActivityBaseBinding
import com.medtroniclabs.opensource.databinding.ErrorLayoutBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.ncd.medicalreview.prescription.dialog.CommentsAlertDialog
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.network.utils.ConnectivityManager
import com.medtroniclabs.opensource.ui.landing.LandingActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.abs

@AndroidEntryPoint
open class BaseActivity : SpiceRootActivity() {
    private lateinit var binding: ActivityBaseBinding

    private var downX: Int = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBaseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        UserDetail.startDateTime =
            com.medtroniclabs.opensource.app.analytics.utils.CommonUtils.getCurrentDateTimeInLocalTime()
        setListener()
    }

    fun setMainContentView(
        view: View,
        isToolbarVisible: Boolean = false,
        title: String? = null,
        //Pair(Home, Back)
        homeAndBackVisibility: Pair<Boolean?, Boolean?> = Pair(false, true),
        callback: (() -> Unit?)? = null,
        callbackHome: (() -> Unit?)? = null,
        homeIcon: Drawable? = null,
        callbackMore: ((view: View) -> Unit?)? = null,
    ) {
        binding.titleToolbar.maxLines = 2
        binding.titleToolbar.ellipsize = TextUtils.TruncateAt.END

        if (isToolbarVisible) {
            binding.toolbar.visibility = View.VISIBLE
        } else {
            binding.toolbar.visibility = View.GONE
        }
        title?.let {
            binding.titleToolbar.text = it
        }

        binding.frameBaseLayout.addView(view)

        homeAndBackVisibility.first?.let {
            binding.ivHome.visibility = checkVisibility(it)
        }

        homeAndBackVisibility.second?.let {
            binding.ivBack.visibility = checkVisibility(it)
        }

        binding.ivBack.safeClickListener {
            if (callback != null)
                callback.invoke()
            else
                finish()
        }
        if (homeIcon != null) {
            binding.ivHome.setImageDrawable(homeIcon)
        } else {
            binding.ivHome.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_home_img))
        }
        binding.ivHome.safeClickListener {
            if (homeIcon == null) {
                if (callbackHome == null) {
                    redirectToHome()
                } else {
                    callbackHome.invoke()
                }
            } else {
                callbackMore?.invoke(binding.ivHome)
            }
        }
    }

    /**
     * Receiver for session expired broadcasts from [Retrofit API].
     */


    private fun checkVisibility(isVisible: Boolean): Int {
        return if (isVisible) View.VISIBLE else View.INVISIBLE
    }

    fun redirectToHome() {
        startAsNewActivity(Intent(this, LandingActivity::class.java))
    }

    fun startAsNewActivity(intent: Intent) {
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    fun startActivityWithoutSplashScreen() {
        val intent = Intent(this, LandingActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        intent.putExtra(REFRESH_FRAGMENT, true)
        startActivity(intent)
        finish()
    }

    fun showVerticalMoreIcon(canVisible: Boolean, callback: ((view: View) -> Unit?)? = null) {
        binding.ivMore.setVisible(canVisible)
        binding.ivMore.safeClickListener {
            callback?.invoke(it)
        }
    }

    fun setTitle(title: String) {
        binding.titleToolbar.text = title
    }

    fun getString(): String {
        return binding.titleToolbar.text.toString()
    }

    fun hideHomeButton(status: Boolean) {
        if (status) {
            binding.ivHome.visibility = View.INVISIBLE
        } else {
            binding.ivHome.visibility = View.VISIBLE
        }
    }

    fun hideBackButton() {
        binding.ivBack.visibility = View.INVISIBLE
    }

    fun showBackButton() {
        binding.ivBack.visibility = View.VISIBLE
    }

    fun showLoading() {
        binding.loadingProgress.visibility = View.VISIBLE
        binding.loaderImage.apply {
            loadAsGif(R.drawable.loader_spice)
        }

    }

    fun hideLoading() {
        binding.loadingProgress.visibility = View.GONE
        binding.loaderImage.apply {
            resetImageView()
        }
    }

    fun showErrorSnackBar(
        text: String,
    ) {
        val rootView = findViewById<View>(android.R.id.content) ?: return
        val snackBar = Snackbar.make(rootView, "", Snackbar.LENGTH_LONG)
        val binding = ErrorLayoutBinding.inflate(layoutInflater)
        binding.tvErrorMessage.text = text
        snackBar.view.setBackgroundColor(Color.TRANSPARENT)
        val snackBarLayout = snackBar.view as Snackbar.SnackbarLayout
        snackBarLayout.setPadding(0, 0, 0, 0)
        snackBarLayout.addView(binding.root)
        snackBar.show()
    }

    inline fun <reified fragment : Fragment> replaceFragmentInId(
        id: Int,
        bundle: Bundle? = null,
        tag: String? = null,
        isAdd: Boolean = false
    ) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            if (isAdd)
                add<fragment>(id, args = bundle, tag = tag)
            else
                replace<fragment>(
                    id,
                    args = bundle,
                    tag = tag
                )
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            downX = event.rawX.toInt()
        }
        if (event.action == MotionEvent.ACTION_UP) {
            val v = currentFocus
            if (v is EditText) {
                val x = event.rawX.toInt()
                val y = event.rawY.toInt()
                //Was it a scroll - If skip all
                if (abs(downX - x) > 5) {
                    return super.dispatchTouchEvent(event)
                }
                val reducePx = 2
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                //Bounding box is to big, reduce it just a little bit
                outRect.inset(reducePx, reducePx)
                touchEvent(v, x, y, reducePx, outRect)
            }
        }
        return super.dispatchTouchEvent(event)
    }


    private fun touchEvent(v: EditText, x: Int, y: Int, reducePx: Int, outRect: Rect) {
        if (!outRect.contains(x, y)) {
            v.clearFocus()
            var touchTargetIsEditText = false
            //Check if another editText has been touched
            for (vi in v.rootView.touchables) {
                if (vi is EditText) {
                    val clickedViewRect = Rect()
                    vi.getGlobalVisibleRect(clickedViewRect)
                    //Bounding box is to big, reduce it just a little bit
                    clickedViewRect.inset(reducePx, reducePx)
                    if (clickedViewRect.contains(x, y)) {
                        touchTargetIsEditText = true
                        break
                    }
                }
            }
            if (!touchTargetIsEditText) {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0)
            }
        }
    }

    fun showTurnOnGPSDialog(isNegativeButtonNeed : Boolean = false) {
        showErrorDialogue(
            title = getString(R.string.gps_disabled_title),
            message = getString(R.string.gps_disabled_message),
            positiveButtonName = getString(R.string.ok),
            isNegativeButtonNeed = isNegativeButtonNeed
        ) {
            if (it) {
                val settingsIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(settingsIntent)
            }
        }
    }

    fun showAllowLocationServiceDialog(isNegativeButtonNeed: Boolean = false) {
        showErrorDialogue(
            title = getString(R.string.gps_disabled_title),
            message = getString(R.string.gps_disabled_message),
            positiveButtonName = getString(R.string.ok),
            isNegativeButtonNeed = isNegativeButtonNeed
        ) {
            if (it) {
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                val uri = Uri.fromParts(
                    "package",
                    BuildConfig.APPLICATION_ID,
                    null
                )
                intent.data = uri
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        connectivityManager.registerConnectionObserver(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        connectivityManager.unregisterConnectionObserver(this)
    }


    private fun setListener() {
        binding.loadingProgress.safeClickListener {

        }
    }

    inline fun <reified F : Fragment> replaceFragmentIfExists(
        id: Int,
        bundle: Bundle? = null,
        tag: String? = null
    ) {
        val existingFragment = supportFragmentManager.findFragmentByTag(tag)

        supportFragmentManager.commit {
            setReorderingAllowed(true)
            if (existingFragment != null) {
                // Fragment exists, replace it
                replace(id, existingFragment, tag)
            } else {
                // Fragment does not exist, create a new instance and replace it
                replace<F>(id, args = bundle, tag = tag)
            }
        }
    }

    inline fun <reified fragment : Fragment> replaceFragmentOrCreateNewFragment(
        id: Int,
        bundle: Bundle? = null,
        tag: String? = null,
        isAdd: Boolean = false
    ) {
        val fragmentManager = supportFragmentManager
        val existingFragment = fragmentManager.findFragmentByTag(tag)

        if (existingFragment == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                if (isAdd)
                    add<fragment>(id, args = bundle, tag = tag)
                else
                    replace<fragment>(
                        id,
                        args = bundle,
                        tag = tag
                    )
            }
        }
    }
    fun getFragmentById(fragmentManager: FragmentManager, fragmentId: Int): Fragment? {
        return fragmentManager.findFragmentById(fragmentId)
    }
    fun withNetworkCheck(
        connectivityManager: ConnectivityManager,
        onNetworkAvailable: () -> Unit,
        onNetworkNotAvailable: (() -> Unit?)? = null

    ) {
        if (connectivityManager.isNetworkAvailable()) {
            onNetworkAvailable()
        } else {
            showErrorDialogue(
                getString(R.string.error),
                getString(R.string.no_internet_error),
                isNegativeButtonNeed = false
            ) {
                if (it && onNetworkNotAvailable != null) {
                    onNetworkNotAvailable()
                }
                hideLoading()
            }
        }
    }

    fun withNetworkAvailability(
        online: () -> Unit,
        offline: () -> Unit = {},
        isErrorShow: Boolean = true
    ) {
        connectivityManager.isNullableNetworkAvailable()?.let { isNetworkAvailable ->
            if (isNetworkAvailable) {
                online()
            } else {
                if (isErrorShow) {
                    showErrorDialogue(
                        getString(R.string.error),
                        getString(R.string.no_internet_error),
                        isNegativeButtonNeed = false
                    ) {}
                }
                offline()
            }
        }
    }

    fun <T> handleResourceState(
        resourceState: Resource<T>,
        onSuccess: () -> Unit,
        onSuccessParam: (T) -> Unit = {},
        onBackPressPopStack: () -> Unit,
        onError: () -> Unit = {
            showErrorDialogue(
                title = getString(R.string.alert),
                message = getString(R.string.something_went_wrong_try_later),
                positiveButtonName = getString(R.string.ok),
            ) {
                if (it) {
                    onBackPressPopStack()
                }
            }
        }
    ) {
        when (resourceState.state) {
            ResourceState.LOADING -> {
                showLoading()
            }

            ResourceState.ERROR -> {
                hideLoading()
                onError()
            }

            ResourceState.SUCCESS -> {
                hideLoading()
                onSuccess()
                resourceState.data?.let(onSuccessParam)
            }
        }
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.mypatient_menu, menu)
        return true    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)
    }

    fun onHomeClick(callbackHome: (() -> Unit?)? = null) {
        binding.ivHome.safeClickListener {
                if (callbackHome == null) {
                    redirectToHome()
                } else {
                    callbackHome.invoke()
                }
            }
        }

    fun hiddenBackButton() {
        binding.ivBack.visibility = checkVisibility(false)
    }

    fun addOrReuseFragment(
        containerId: Int,
        fragmentTag: String,
        fragmentInstance: Fragment,
        bundle: Bundle? = null
    ) {
        val existingFragment = supportFragmentManager.findFragmentByTag(fragmentTag)
        val transaction = supportFragmentManager.beginTransaction()
        if (existingFragment == null) {
            // Add bundle if provided
            if (bundle != null) {
                fragmentInstance.arguments = bundle
            }
            transaction
                .add(containerId, fragmentInstance, fragmentTag)
        }
        transaction.commit()
    }

    fun replaceFragment(
        containerId: Int,
        fragmentTag: String,
        fragment: Fragment
    ) {
        val fragmentManager = supportFragmentManager
        val transaction = fragmentManager.beginTransaction()

        // Check if the fragment with the given tag already exists
        val existingFragment = fragmentManager.findFragmentByTag(fragmentTag)

        // If the fragment exists, remove it
        if (existingFragment != null) {
            transaction.remove(existingFragment)
        }

        // Add a new instance of the fragment
        transaction.replace(containerId, fragment, fragmentTag)

        // Commit the transaction
        transaction.commit()
    }

    fun showAlertDialogWithComments(
        title: String? = null,
        message: String,
        isNegativeButtonNeed: Boolean = false,
        showComment: Boolean = true,
        errorMessage: String = getString(R.string.default_user_input_error),
        buttonName: Pair<String, String> = Pair(
            getString(R.string.ok),
            getString(R.string.cancel)
        ),
        callback: ((isPositiveResult: Boolean, reason: String?) -> Unit)
    ) {
        val dialog = CommentsAlertDialog.newInstance(
            this,
            title,
            isNegativeButtonNeed,
            Pair(buttonName.first, buttonName.second),
            callback = callback,
            showComment = showComment,
            message = Pair(message, errorMessage)
        )
        dialog.show(supportFragmentManager, CommentsAlertDialog.TAG)
    }

    fun setRedRiskPatient(isRedRiskPatient: Boolean?) {
        binding.ivRedAlert.setVisible(isRedRiskPatient == true)
        binding.tvRedAlert.setVisible(isRedRiskPatient == true)
    }

    override fun attachBaseContext(newBase: Context?) {
        try {
            super.attachBaseContext(newBase?.let {
                LocaleHelper.onAttach(it, CommonUtils.parseUserLocale())
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}