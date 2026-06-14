package com.baxter.schedulaizer.ui.transfer

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.baxter.schedulaizer.data.db.entity.AttachmentEntity
import com.baxter.schedulaizer.data.repository.AttachmentRepository

class BackgroundTransfersViewModel(
    private val repo: AttachmentRepository
) : ViewModel() {

    val transfers: LiveData<List<AttachmentEntity>> = repo.allAttachments.asLiveData()

}
