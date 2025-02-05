package com.medtroniclabs.opensource.offlinesync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.medtroniclabs.opensource.db.local.RoomHelper
import com.medtroniclabs.opensource.repo.OfflineSyncRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class PostDataWorker  @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted userParameter: WorkerParameters,
    val roomHelper: RoomHelper,
    val offlineSyncRepository: OfflineSyncRepository
) : CoroutineWorker(context, userParameter) {

    override suspend fun doWork(): Result {
        println("Worker Starts : ${id.toString()}")
        for (i in 0..25) {
            if (isStopped) {
                println("Failed... Interrupt by Other worker")
                return Result.failure()
            }

            println("Index $i")
            Thread.sleep(2000)
        }
        println("Worker Ends")

        return Result.success()
    }
}