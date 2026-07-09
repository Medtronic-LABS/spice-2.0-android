package org.medtroniclabs.uhis.ui.landing

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.appextensions.WORKER_UNIQUE_NAME
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.offlinesync.utils.OfflineConstant.KEY_REQUESTS_ID
import org.medtroniclabs.uhis.databinding.FragmentOfflineSyncBinding
import org.medtroniclabs.uhis.offlinesync.GetSyncStatusWorker
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.landing.adapter.OfflineSyncEntitiesAdapter
import org.medtroniclabs.uhis.ui.landing.viewmodel.OfflineSyncViewModel
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class OfflineSyncActivity : BaseActivity() {
    private val viewModel: OfflineSyncViewModel by viewModels()
    private lateinit var binding: FragmentOfflineSyncBinding
    private lateinit var unSyncedCountAdapter: OfflineSyncEntitiesAdapter
    private val getStatusStartTimer = 30L // Seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentOfflineSyncBinding.inflate(layoutInflater)
        setContentView(binding.root)

        overrideBackPress()

        setListener()
        initObserver()
        viewModel.setUserJourney(getString(R.string.offline_sync))
    }

    private fun setListener() {
        binding.rvUnSyncedDetail.layoutManager = LinearLayoutManager(this)
        unSyncedCountAdapter = OfflineSyncEntitiesAdapter()
        binding.rvUnSyncedDetail.adapter = unSyncedCountAdapter

        binding.btnStart.setOnClickListener {
            viewModel.setUserJourney(AnalyticsDefinedParams.STARTBUTTONTRIGGERED)
            if (isManualTriggerBlocked()) {
                showAlreadyRunningPopup()
            } else {
                initiateUpload()
            }
        }

        binding.btnCancel.setOnClickListener {
            viewModel.setUserJourney(AnalyticsDefinedParams.CANCELBUTTONTRIGGERED)
            finish()
        }

        binding.btnOkay.setOnClickListener {
            if (binding.btnOkay.text.toString() == getString(R.string.retry)) {
                viewModel.setUserJourney(AnalyticsDefinedParams.RETRYBUTTONTRIGGERED)
                val requestIds =
                    SecuredPreference.getStringArray(SecuredPreference.EnvironmentKey.OFFLINE_SYNC_REQUEST_ID.name)
                requestIds?.let {
                    initiateGetStatus(it)
                }
            } else {
                viewModel.setUserJourney(AnalyticsDefinedParams.OKAYBUTTONTRIGGERED)
                finish()
            }
        }
    }

    private fun showAlreadyRunningPopup() {
        showErrorDialogue(
            getString(R.string.alert),
            getString(R.string.background_sync_in_progress),
            isNegativeButtonNeed = false,
        ) {
            finish()
        }
    }

    /**
     * Returns true if any of the worker for Post is already running
     */
    private fun isSyncWorkerAlreadyRunning(): Boolean {
        val workManager = WorkManager.getInstance(this)
        val workInfos = workManager.getWorkInfosForUniqueWork(WORKER_UNIQUE_NAME).get()
        return workInfos.any {
            it.state == WorkInfo.State.RUNNING
        }
    }

    private fun isManualTriggerBlocked() = isSyncWorkerAlreadyRunning() || viewModel.isPostOfflineSyncAlreadyRunning()

    private fun initiateUpload() {
        if (viewModel.connectivityManager.isNetworkAvailable()) {
            if (isManualTriggerBlocked()) {
                showAlreadyRunningPopup()
                return
            }
            showProgressView()
            viewModel.startUploadingData()
        } else {
            showErrorDialogue(
                getString(R.string.title_no_network),
                getString(R.string.message_no_network),
                isNegativeButtonNeed = false,
            ) { _ -> }
        }
    }

    private fun initObserver() {
        viewModel.unSyncedCountLiveData.observe(this) {
            unSyncedCountAdapter.updateList(it)
        }

        viewModel.lastSyncedAtLiveData.observe(this) {
            binding.tvLastSyncedAt.text = it
        }

        viewModel.oldRequestIdsLiveData.observe(this) {
            it?.let {
                if (viewModel.connectivityManager.isNetworkAvailable()) {
                    initiateGetStatus(it)
                } else {
                    showErrorDialogue(
                        getString(R.string.title_no_network),
                        getString(R.string.message_no_network),
                        isNegativeButtonNeed = false,
                    ) { _ ->
                        finish()
                    }
                }
            }
        }

        viewModel.postRequestIdsLiveData.observe(this) { requestIds ->
            if (viewModel.connectivityManager.isNetworkAvailable()) {
                if (requestIds.isNotEmpty()) {
                    startGetSyncStatusWorkManager(requestIds.toTypedArray(), getStatusStartTimer)
                } else {
                    startGetSyncStatusWorkManager(arrayOf(), 0)
                }
            } else {
                showErrorDialogue(
                    getString(R.string.title_no_network),
                    getString(R.string.message_no_network),
                    isNegativeButtonNeed = false,
                ) { _ ->
                    finish()
                }
            }
        }

        viewModel.progressLiveData.observe(this) {
            binding.progressBar.progress = it
            binding.tvOfflineSyncProgress.text = getString(R.string.sync_progress_percent, it)
        }

        viewModel.statusLiveData.observe(this) {
            showCompletionView(it.first, it.second)
        }
    }

    private fun initiateGetStatus(arr: Array<String>) {
        showProgressView()
        viewModel.startProgress(getStatusStartTimer)
        startGetSyncStatusWorkManager(arr, 0)
    }

    private fun startGetSyncStatusWorkManager(
        requestIds: Array<String>,
        duration: Long,
    ) {
        val workManager = WorkManager.getInstance(this)

        val data = Data
            .Builder()
            .putStringArray(KEY_REQUESTS_ID, requestIds)
            .build()

        val constrain = Constraints
            .Builder()
            // .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val getSyncStatusWorker = OneTimeWorkRequestBuilder<GetSyncStatusWorker>()
            .setInitialDelay(duration, TimeUnit.SECONDS)
            .setInputData(data)
            .setConstraints(constrain)
            .build()

        workManager.enqueue(getSyncStatusWorker)

        workManager.getWorkInfoByIdLiveData(getSyncStatusWorker.id).observe(this) { workerInfo ->
            if (workerInfo.state == WorkInfo.State.SUCCEEDED) {
                binding.btnOkay.text = getString(R.string.okay)
                viewModel.syncCompleted(true)
            } else if (workerInfo.state == WorkInfo.State.FAILED) {
                val errorData = workerInfo.outputData
                val errorMessage = errorData.getString("failureReason")
                if (errorMessage != null) {
                    binding.btnOkay.text = getString(R.string.retry)
                    viewModel.syncCompleted(message = getString(R.string.message_no_network))
                } else {
                    viewModel.syncCompleted()
                }
            }
        }
    }

    private fun showProgressView() {
        binding.clBeforeSync.gone()
        binding.clAfterSync.gone()
        binding.clSyncInProgress.visible()
    }

    private fun showCompletionView(
        isSuccess: Boolean = false,
        message: String? = null,
    ) {
        binding.clAfterSync.gone()
        binding.clSyncInProgress.gone()
        binding.clAfterSync.visible()
        if (isSuccess) {
            binding.statusImage.setImageDrawable(getDrawable(R.drawable.success_icon))
            binding.tvOfflineSyncCompleted.text = getString(R.string.offline_data_completion)
        } else {
            binding.statusImage.setImageDrawable(getDrawable(R.drawable.ic_failed))
            binding.tvOfflineSyncCompleted.text = message ?: getString(R.string.offline_data_failed)
        }
    }

    private fun overrideBackPress() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Handle the back button event
                // For example, finish the activity
                // finish()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }
}
