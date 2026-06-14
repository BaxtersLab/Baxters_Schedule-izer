package com.baxter.schedulaizer.ui.transfer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.baxter.schedulaizer.data.db.entity.AttachmentEntity
import com.baxter.schedulaizer.databinding.ItemTransferRowBinding
import java.util.Date

class BackgroundTransfersAdapter(
    private val onOpen: (Long) -> Unit,
    private val onRetry: (Long) -> Unit
) : RecyclerView.Adapter<BackgroundTransfersAdapter.VH>() {

    private val items = mutableListOf<AttachmentEntity>()

    fun submit(list: List<AttachmentEntity>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun indexOfId(id: Long): Int = items.indexOfFirst { it.id == id }

    fun currentList(): List<AttachmentEntity> = items.toList()

    class VH(val binding: ItemTransferRowBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemTransferRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        holder.binding.textFileName.text = item.fileName
        holder.binding.textState.text = "State: ${item.state}"
        holder.binding.textTimestamp.text = Date(item.updatedAt).toString()

        holder.binding.root.setOnClickListener {
            onOpen(item.id)
        }

        holder.binding.buttonRetry.setOnClickListener {
            onRetry(item.id)
        }
    }

    override fun getItemCount() = items.size
}
