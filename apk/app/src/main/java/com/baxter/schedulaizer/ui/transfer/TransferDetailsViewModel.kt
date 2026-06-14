package com.baxter.schedulaizer.ui.transfer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baxter.schedulaizer.data.db.entity.AttachmentEntity
import com.baxter.schedulaizer.data.repository.AttachmentRepository
import kotlinx.coroutines.launch

class TransferDetailsViewModel(
    private val repo: AttachmentRepository
) : ViewModel() {

    private val _attachment = MutableLiveData<AttachmentEntity?>()
    val attachment: LiveData<AttachmentEntity?> = _attachment

    fun load(attachmentId: Long) {
        viewModelScope.launch {
            _attachment.value = repo.get(attachmentId)
        }
    }
}
