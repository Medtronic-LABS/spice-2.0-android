package com.medtroniclabs.opensource.appextensions

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Rect
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.ui.BaseActivity
import kotlin.math.sqrt

fun Context.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}

fun EditText.setTextChangeListener(
    onChanged: (text: String?) -> Unit
) {
    this.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(
            s: CharSequence?,
            start: Int,
            count: Int,
            after: Int
        ) {
            // Not used, implementation can be left empty
        }

        override fun onTextChanged(
            s: CharSequence?,
            start: Int,
            before: Int,
            count: Int
        ) {
            // Not used, implementation can be left empty
        }

        override fun afterTextChanged(s: Editable?) {
            onChanged(s?.toString())
        }
    })
}

fun View.visible() {
    this.visibility = View.VISIBLE
}

fun View.gone() {
    this.visibility = View.GONE
}

fun View.invisible() {
    this.visibility = View.INVISIBLE
}

fun View.isVisible(): Boolean {
    return this.visibility == View.VISIBLE
}

fun View.isGone(): Boolean {
    return this.visibility == View.GONE
}

fun View.isInvisible(): Boolean {
    return this.visibility == View.INVISIBLE
}

fun <T> ArrayList<T>.nullIfEmpty(): ArrayList<T>? {
    return if (this.isEmpty()) {
        null
    } else {
        this
    }
}


fun DialogFragment.setWidth(percentage: Int) {
    val percent = percentage.toFloat() / 100
    val dm = Resources.getSystem().displayMetrics
    val rect = dm.run { Rect(0, 0, widthPixels, heightPixels) }
    val percentWidth = rect.width() * percent
    dialog?.window?.setLayout(percentWidth.toInt(), LinearLayout.LayoutParams.WRAP_CONTENT)
}


fun ConstraintLayout.setPercentWidth(viewId: Int, percentage: Float) {
    val constraintSet = ConstraintSet()
    constraintSet.clone(this)
    constraintSet.constrainPercentWidth(viewId, percentage)
    constraintSet.applyTo(this)
}

fun Context.isNotTabletAndPortrait(): Boolean {
    val orientation = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    return !CommonUtils.checkIsTablet(this) && orientation
}

fun DialogFragment.setDialogWidthAndHeightAsWrapPercent(widthPercent: Int) {
    val widthPercentage = widthPercent.toFloat() / 100
    val dm = Resources.getSystem().displayMetrics
    val rect = dm.run { Rect(0, 0, widthPixels, heightPixels) }
    val percentWidth = rect.width() * widthPercentage
    dialog?.window?.setLayout(percentWidth.toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
}

fun DialogFragment.setDialogPercent(width: Int, height: Int = 80) {
    val widthPercentage = width.toFloat() / 100
    val heightPercentage = height.toFloat() / 100
    val dm = Resources.getSystem().displayMetrics
    val rect = dm.run { Rect(0, 0, widthPixels, heightPixels) }
    val percentWidth = rect.width() * widthPercentage
    val percentHeight = rect.height() * heightPercentage
    dialog?.window?.setLayout(percentWidth.toInt(), percentHeight.toInt())
}

fun TextView.setExpandableText(
    fullText: String,
    maxLength: Int = 100,
    moreText: String = "…more",
    moreColorResId: Int = R.color.purple_700,
    title: String,
    activity: BaseActivity? = null
) {
    if (fullText.length <= maxLength) {
        this.text = fullText
        return
    }

    val truncatedText = fullText.substring(0, maxLength).trim() + " "
    val spannableString = SpannableString(truncatedText + moreText)

    val moreColor = ContextCompat.getColor(context, moreColorResId)

    val clickableSpan = object : ClickableSpan() {
        override fun onClick(widget: View) {
            activity?.showErrorDialogue(
                title = title,
                message = fullText,
                isNegativeButtonNeed = false,
                positiveButtonName = context.getString(R.string.ok)
            ) {
            }
        }

        override fun updateDrawState(ds: android.text.TextPaint) {
            super.updateDrawState(ds)
            ds.isUnderlineText = false // Remove underline
            ds.color = moreColor // Set the text color to the specified color
        }
    }

    spannableString.setSpan(
        clickableSpan,
        truncatedText.length,
        spannableString.length,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    spannableString.setSpan(
        ForegroundColorSpan(moreColor),
        truncatedText.length,
        spannableString.length,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
    )

    this.text = spannableString
    this.movementMethod = LinkMovementMethod.getInstance()
}

fun ViewGroup.showView() {
    this.apply {
        alpha = 0f
        visibility = View.VISIBLE
        animate()
            .alpha(1f)
            .setDuration(300)
            .setListener(null)
    }
}

fun ViewGroup.hideView() {
    this.apply {
        animate()
            .alpha(0f)
            .setDuration(300)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    visibility = View.GONE
                }
            })
    }
}

fun ImageView.loadAsGif(drawable: Int) {
    Glide.with(this)
        .asGif()
        .placeholder(R.drawable.ic_spice_logo)
        .load(drawable)
        .diskCacheStrategy(DiskCacheStrategy.ALL)  // Cache all versions of the image
        .into(this)
}

fun ImageView.resetImageView() {
    this.setImageDrawable(null)
}

fun Int?.takeIfNotNull(default: String = ""): String {
    return this.takeIf { it != null }?.toString() ?: default
}

fun Double?.takeIfNotNull(default: String = ""): String {
    return this.takeIf { it != null }?.toString() ?: default
}

fun Float?.takeIfNotNull(default: String = ""): String {
    return this.takeIf { it != null }?.toString() ?: default
}

fun String?.takeIfNotNull(default: String = ""): String {
    return this.takeIf { it != null }?.toString() ?: default
}
fun View.setVisible(isVisible: Boolean) {
    if (isVisible) {
        this.visible() // Assuming `visible()` shows the view
    } else {
        this.gone() // Assuming `gone()` hides the view
    }
}

fun DialogFragment.setDialogPercentForWidth(percentage: Int) {
    val percent = percentage.toFloat() / 100
    val dm = Resources.getSystem().displayMetrics
    val rect = dm.run { Rect(0, 0, widthPixels, heightPixels) }
    val percentWidth = rect.width() * percent
    dialog?.window?.setLayout(percentWidth.toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
}

fun getScreenSizeInInches(context: Context): Double {
    val displayMetrics = context.resources.displayMetrics
    val widthInPixels = displayMetrics.widthPixels.toDouble()
    val heightInPixels = displayMetrics.heightPixels.toDouble()
    val densityDpi = displayMetrics.densityDpi.toDouble()

    // Calculate width and height in inches
    val widthInInches = widthInPixels / densityDpi
    val heightInInches = heightInPixels / densityDpi

    // Calculate the diagonal screen size in inches
    return sqrt(widthInInches * widthInInches + heightInInches * heightInInches)
}



fun Context.isTablet(): Boolean {
    return (resources.configuration.screenLayout and
            Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE
}