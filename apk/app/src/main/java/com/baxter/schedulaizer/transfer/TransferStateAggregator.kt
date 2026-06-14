package com.baxter.schedulaizer.transfer

import android.content.Context
import com.baxter.schedulaizer.data.repository.AttachmentRepository
import com.baxter.schedulaizer.ui.tryGetMainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

/**
 * Observes the DB for active transfers (STAGED/UPLOADING) and updates UI accordingly.
 */
object TransferStateAggregator {
    private var scope: CoroutineScope? = null
    private var job: Job? = null

    fun start(context: Context, repo: AttachmentRepository) {
        if (scope != null) return
        scope = CoroutineScope(Dispatchers.Default)
        job = scope!!.launch {
            // Collect counts
            launch {
                repo.activeTransferCount.collectLatest { count ->
                    val main = context.tryGetMainActivity()
                    if (count > 0) {
                        main?.runOnUiThread { main.setTransfersUploading(true) }
                    } else {
                        main?.runOnUiThread { main.setTransfersIdle() }
                    }
                }
            }

            // Collect IDs and forward to MainActivity
            launch {
                repo.activeAttachmentIds.collect { ids ->
                    val main = context.tryGetMainActivity() ?: return@collect
                    main.runOnUiThread { main.updateActiveTransferIds(ids) }
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        scope = null
    }
}
