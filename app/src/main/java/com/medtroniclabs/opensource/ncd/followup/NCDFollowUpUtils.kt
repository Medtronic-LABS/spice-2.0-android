package com.medtroniclabs.opensource.ncd.followup

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.GridLayoutManager
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.databinding.FragmentFollowUpSearchBinding
import com.medtroniclabs.opensource.databinding.FragmentNcdFollowUpOfflineSearchBinding
import com.medtroniclabs.opensource.ncd.followup.adapter.NCDFollowUpOfflineListAdapter
import com.medtroniclabs.opensource.ncd.followup.adapter.NCDPatientFollowUPListAdapter
import com.medtroniclabs.opensource.ncd.followup.viewmodel.NCDFollowUpViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

object NCDFollowUpUtils {
    const val SCREENED = "SCREENED"
    const val Assessment_Type = "ASSESSMENT"
    const val Defaulters_Type = "NON_COMMUNITY_MEDICAL_REVIEW"
    const val LTFU_Type = "LOST_TO_FOLLOW_UP"
    const val visited_facility = "Visited facility"
    const val will_visit_facility = "Will visit facility"
    const val wont_visit_facility = "Won’t visit facility"
    const val isInitiated = "isInitiated"
    const val today = "today"
    const val tomorrow = "tomorrow"
    const val customise = "customize"
    const val REASON_CONSTANT = "FOLLOW_UP"
    const val medical_review = "medical review"

    fun getDaysString(it: Long): Int {
        return if (it == 1L) R.string.day_due else R.string.days_due
    }

    fun hasTelephonyFeature(context: Context): Boolean {
        val packageManager = context.packageManager
        return packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
    }

    fun setOrientationAndSpanCount(
        activity: FragmentActivity,
        resources: Resources,
        viewModel: NCDFollowUpViewModel,
        spanCountForTablet: Int = DefinedParams.span_count_3,
        spanCountForPhone: Int = DefinedParams.span_count_1,
        binding: FragmentNcdFollowUpOfflineSearchBinding,
        context: Context,
        followUpAdapter: NCDFollowUpOfflineListAdapter
    ) {
        val isTablet =
            resources.getBoolean(R.bool.isLargeTablet) || resources.getBoolean(R.bool.isTablet)
        if (isTablet) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            viewModel.spanCount = spanCountForTablet
        } else {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            viewModel.spanCount = spanCountForPhone
        }
        binding.rvPatientList.apply {
            layoutManager =
                GridLayoutManager(context, viewModel.spanCount)
            adapter = followUpAdapter
        }
    }

    fun setOrientationAndSpanCountForOffline(
        activity: FragmentActivity,
        resources: Resources,
        viewModel: NCDFollowUpViewModel,
        spanCountForTablet: Int = DefinedParams.span_count_3,
        spanCountForPhone: Int = DefinedParams.span_count_1,
        binding: FragmentFollowUpSearchBinding,
        context: Context,
        followUpAdapter: NCDPatientFollowUPListAdapter
    ) {
        val isTablet =
            resources.getBoolean(R.bool.isLargeTablet) || resources.getBoolean(R.bool.isTablet)
        if (isTablet) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            viewModel.spanCount = spanCountForTablet
        } else {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            viewModel.spanCount = spanCountForPhone
        }
        binding.rvPatientList.apply {
            layoutManager =
                GridLayoutManager(context, viewModel.spanCount)
            adapter = followUpAdapter
        }
    }

    fun <T : PagingDataAdapter<*, *>> setupAdapterLoadStateListener(
        adapter: T,
        context: Context,
        showProgress: () -> Unit,
        hideProgress: () -> Unit,
        showPageProgress: () -> Unit,
        hidePageProgress: () -> Unit,
        showError: (String, String) -> Unit,
        postError: () -> Unit
    ) {
        adapter.addLoadStateListener { loadState ->
            val isLoading = loadState.refresh is LoadState.Loading
            if (isLoading) showProgress() else hideProgress()

            if (loadState.append is LoadState.Loading) {
                showPageProgress()
            } else {
                hidePageProgress()
            }

            // Handle errors in refresh, prepend, or append
            val errorState = when {
                loadState.refresh is LoadState.Error -> loadState.refresh as LoadState.Error
                loadState.append is LoadState.Error -> loadState.append as LoadState.Error
                loadState.prepend is LoadState.Error -> loadState.prepend as LoadState.Error
                else -> null
            }

            errorState?.let {
                showError(
                    context.getString(R.string.alert),
                    context.getString(R.string.something_went_wrong_try_later)
                )
                postError()
            }
        }
    }

    fun <T : Any> submitEmptyList(
        adapter: PagingDataAdapter<T, *>,
        lifecycleOwner: LifecycleOwner
    ) {
        adapter.submitData(lifecycleOwner.lifecycle, PagingData.empty())
    }

    fun <T : Any> collectPagedData(
        lifecycleOwner: LifecycleOwner,
        pagingDataFlow: kotlinx.coroutines.flow.Flow<PagingData<T>>,
        adapter: PagingDataAdapter<T, *>
    ) {
        lifecycleOwner.lifecycleScope.launch {
            pagingDataFlow.collectLatest { pagedData ->
                adapter.submitData(pagedData)
            }
        }
    }
}