package com.baxter.schedulaizer.ui.transfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.baxter.schedulaizer.data.repository.AttachmentRepository

class BackgroundTransfersVMFactory(
    private val repo: AttachmentRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return BackgroundTransfersViewModel(repo) as T
    }
}
